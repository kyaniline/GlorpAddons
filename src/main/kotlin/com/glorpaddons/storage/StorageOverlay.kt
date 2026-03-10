package com.glorpaddons.storage

import com.glorpaddons.util.RenderUtils
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen as MCScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

object StorageOverlay {

    // ── Layout ────────────────────────────────────────────────────────────
    private const val COLS           = 3
    private const val PANEL_PAD      = 10
    private const val BOX_GAP        = 8
    private const val BOX_PAD        = 4
    private const val TITLE_H        = 14
    private const val SLOT_SIZE      = 18
    private const val ITEM_ROWS      = 5      // rows 1-5 of the chest (skip row 0 nav buttons)
    private const val BOX_CONTENT_W  = 9 * SLOT_SIZE                                // 162
    private const val BOX_CONTENT_H  = ITEM_ROWS * SLOT_SIZE                        // 90
    private const val BOX_W          = BOX_CONTENT_W + BOX_PAD * 2                  // 170
    private const val BOX_H          = TITLE_H + BOX_CONTENT_H + BOX_PAD * 2 + 4   // 116

    private const val SEARCH_H       = 14    // input area height inside the search row
    private const val SEARCH_ROW     = SEARCH_H + 10  // total reserved at panel bottom

    private const val INV_PAD        = 6
    private const val INV_HOTBAR_GAP = 4
    private const val INV_GAP        = 8   // gap between panel bottom and inventory
    private val INV_W = 9 * SLOT_SIZE + INV_PAD * 2          // 174
    private val INV_H = 3 * SLOT_SIZE + INV_HOTBAR_GAP + SLOT_SIZE + INV_PAD * 2  // 88

    // ── Colors ────────────────────────────────────────────────────────────
    private val COLOR_BG         = 0xE0101820.toInt()
    private val COLOR_BORDER     = 0x50888888
    private val COLOR_DIVIDER    = 0x40FFFFFF
    private val COLOR_BOX_BG     = 0x80182030.toInt()
    private val COLOR_BOX_BORDER = 0x40888888
    private val COLOR_SLOT_BG    = 0x60101010
    private val COLOR_TEXT       = 0xFFCCCCCC.toInt()
    private val COLOR_ACTIVE_T   = 0xFFFFFFFF.toInt()
    private val COLOR_ACTIVE_BOX = 0x50888888
    private val COLOR_HOVER_BOX  = 0x28FFFFFF
    private val COLOR_SCROLL     = 0x60AAAAAA
    private val COLOR_SCROLL_T   = 0x30FFFFFF
    private val COLOR_DIM        = 0xA0000000.toInt()
    private val COLOR_SEARCH_BG  = 0x60182840
    private val COLOR_SEARCH_FOC = 0x80203050.toInt()

    // ── State ─────────────────────────────────────────────────────────────
    private var scrollTarget  = 0f   // target row (may be fractional mid-scroll)
    private var scrollCurrent = 0f   // visually rendered row (animates toward target)
    private var lastRenderNs  = 0L   // for framerate-independent lerp
    private var searchText    = ""
    private var searchFocused = false
    private var hoveredStack: net.minecraft.item.ItemStack? = null

    /** Set to true during storage navigation to prevent cursor centering. */
    @JvmField var suppressCursorReset = false

    // ── Panel geometry (recomputed each frame) ────────────────────────────
    private var panelX   = 0
    private var panelY   = 0
    private var panelW   = 0
    private var panelH   = 0
    private var visRows  = 0
    private var maxScrollRows = 0
    private var invX     = 0
    private var invY     = 0

    // Top-left of the item grid inside the active (current) page box.
    // Updated each render frame; used by click handler for slot interaction.
    private var activeItemGridX = -1
    private var activeItemGridY = -1

    // ── Registration ─────────────────────────────────────────────────────

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (!StorageConfigManager.config.enabled) return@register
            if (screen !is GenericContainerScreen) {
                if (!StorageCache.wasInOverview) {
                    StorageCache.exitStorage()
                    searchText = ""
                    searchFocused = false
                }
                return@register
            }

            val title = StorageCache.stripFormatting(screen.title.string)

            when {
                isOverview(title) -> {
                    StorageCache.ensureLoaded()
                    scrollTarget = 0f
                    scrollCurrent = 0f

                    ScreenEvents.afterRender(screen).register { s, ctx, mx, my, _ ->
                        val gs = s as GenericContainerScreen
                        StorageCache.updateFromOverview(gs.screenHandler.slots)
                        computeGeometry(gs)
                        renderPanel(ctx, mx, my)
                    }
                    ScreenMouseEvents.afterMouseScroll(screen).register { _, mx, my, _, vert, _ ->
                        handleScroll(mx, my, vert); false
                    }
                    ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
                        handleClick(click.x, click.y, click.button(), null)
                    }
                    ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyInput ->
                        handleKeyPress(keyInput)
                    }
                    ScreenEvents.remove(screen).register { _ -> StorageCache.exitStorage() }
                }

                else -> {
                    val pageSlot = StoragePageSlot.fromTitle(title) ?: return@register
                    StorageCache.ensureLoaded()
                    StorageCache.enterSubPage(pageSlot)

                    ScreenEvents.afterRender(screen).register { s, ctx, mx, my, _ ->
                        val gs = s as GenericContainerScreen
                        StorageCache.cachePageItems(pageSlot, gs.screenHandler.slots.toList(), gs.screenHandler.rows)
                        computeGeometry(gs)
                        renderPanel(ctx, mx, my)
                    }
                    ScreenMouseEvents.afterMouseScroll(screen).register { _, mx, my, _, vert, _ ->
                        handleScroll(mx, my, vert); false
                    }
                    ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
                        handleClick(click.x, click.y, click.button(), screen.screenHandler)
                    }
                    ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyInput ->
                        handleKeyPress(keyInput)
                    }
                    ScreenEvents.remove(screen).register { _ -> StorageCache.exitStorage() }
                }
            }
        }
    }

    // ── Title detection ───────────────────────────────────────────────────

    private fun isOverview(title: String) =
        title.equals("Storage", ignoreCase = true)

    // ── Geometry ─────────────────────────────────────────────────────────

    private fun computeGeometry(screen: GenericContainerScreen) {
        val pages     = StorageCache.pages
        val totalRows = (pages.size + COLS - 1) / COLS

        panelW = COLS * BOX_W + (COLS - 1) * BOX_GAP + PANEL_PAD * 2
        val neededH  = totalRows * (BOX_H + BOX_GAP) - BOX_GAP + PANEL_PAD * 2 + 22 + SEARCH_ROW
        val maxPanelH = screen.height - INV_H - INV_GAP - 10
        panelH = neededH.coerceIn(80, maxPanelH.coerceAtLeast(80))

        val totalH = panelH + INV_GAP + INV_H
        panelX = (screen.width - panelW) / 2
        panelY = (screen.height - totalH) / 2

        panelX = panelX.coerceIn(2, (screen.width  - panelW).coerceAtLeast(2))
        panelY = panelY.coerceIn(2, (screen.height - totalH).coerceAtLeast(2))

        invX = (screen.width - INV_W) / 2
        invY = panelY + panelH + INV_GAP

        visRows = ((panelH - PANEL_PAD * 2 - 22 - SEARCH_ROW) / (BOX_H + BOX_GAP)).coerceAtLeast(1)
        maxScrollRows = (totalRows - visRows).coerceAtLeast(0)
        scrollTarget = scrollTarget.coerceIn(0f, maxScrollRows.toFloat())
    }

    // ── Smooth scroll ─────────────────────────────────────────────────────

    private fun advanceScroll() {
        val nowNs = System.nanoTime()
        val dtSec = if (lastRenderNs == 0L) 0f
                    else ((nowNs - lastRenderNs) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
        lastRenderNs = nowNs
        // Framerate-independent lerp: k=14 → ~86% closure per 0.1 s
        val factor = 1f - Math.exp((-14.0 * dtSec)).toFloat()
        scrollCurrent += (scrollTarget - scrollCurrent) * factor
        // Snap once close enough to avoid perpetual sub-pixel drift
        if (Math.abs(scrollTarget - scrollCurrent) < 0.001f) scrollCurrent = scrollTarget
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    private fun renderPanel(ctx: DrawContext, mx: Int, my: Int) {
        advanceScroll()
        hoveredStack = null
        val tr    = MinecraftClient.getInstance().textRenderer
        val pages = StorageCache.pages

        // Panel border + background
        RenderUtils.drawRoundedRect(ctx,
            (panelX - 1).toFloat(), (panelY - 1).toFloat(),
            (panelW + 2).toFloat(), (panelH + 2).toFloat(),
            8, COLOR_BORDER)
        RenderUtils.drawRoundedRect(ctx,
            panelX.toFloat(), panelY.toFloat(),
            panelW.toFloat(), panelH.toFloat(),
            7, COLOR_BG)

        // Title bar
        ctx.drawCenteredTextWithShadow(tr, Text.literal("Storage"),
            panelX + panelW / 2, panelY + 6, COLOR_ACTIVE_T)
        ctx.fill(panelX + PANEL_PAD, panelY + 18,
                 panelX + panelW - PANEL_PAD, panelY + 19, COLOR_DIVIDER)

        // ── Search bar (pinned to panel bottom) ───────────────────────────
        val searchDivY = panelY + panelH - PANEL_PAD - SEARCH_ROW
        ctx.fill(panelX + PANEL_PAD, searchDivY,
                 panelX + panelW - PANEL_PAD, searchDivY + 1, COLOR_DIVIDER)

        val sbX = panelX + PANEL_PAD
        val sbY = searchDivY + (SEARCH_ROW - SEARCH_H) / 2
        val sbW = panelW - PANEL_PAD * 2

        val searchBg = if (searchFocused) COLOR_SEARCH_FOC else COLOR_SEARCH_BG
        ctx.fill(sbX, sbY, sbX + sbW, sbY + SEARCH_H, searchBg)

        if (searchFocused) {
            ctx.fill(sbX, sbY,                sbX + sbW, sbY + 1,      COLOR_SCROLL)
            ctx.fill(sbX, sbY + SEARCH_H - 1, sbX + sbW, sbY + SEARCH_H, COLOR_SCROLL)
            ctx.fill(sbX, sbY, sbX + 1,       sbY + SEARCH_H,          COLOR_SCROLL)
            ctx.fill(sbX + sbW - 1, sbY, sbX + sbW, sbY + SEARCH_H,   COLOR_SCROLL)
        }

        val cursorVisible = System.currentTimeMillis() / 530 % 2 == 0L
        val cursor = if (searchFocused && cursorVisible) "|" else ""
        val displayText: String
        val displayColor: Int
        if (searchText.isEmpty() && !searchFocused) {
            displayText  = "Search..."
            displayColor = 0xFF666688.toInt()
        } else {
            displayText  = searchText + cursor
            displayColor = COLOR_ACTIVE_T
        }
        ctx.drawText(tr, Text.literal(displayText), sbX + 4, sbY + 3, displayColor, false)

        if (pages.isEmpty()) {
            ctx.drawText(tr, Text.literal("Open Storage to cache pages"),
                panelX + PANEL_PAD, panelY + 26, COLOR_TEXT, false)
            drawPlayerInventory(ctx, tr, mx, my)
            return
        }

        val contentTop    = panelY + PANEL_PAD + 22
        val contentBottom = searchDivY

        ctx.enableScissor(panelX, contentTop, panelX + panelW, contentBottom)

        val scrollPx = (scrollCurrent * (BOX_H + BOX_GAP)).toInt()

        for (i in pages.indices) {
            val col = i % COLS
            val row = i / COLS

            val page = pages[i]
            val bx   = panelX + PANEL_PAD + col * (BOX_W + BOX_GAP)
            val by   = contentTop + row * (BOX_H + BOX_GAP) - scrollPx

            // Skip if fully outside the visible clip region
            if (by + BOX_H < contentTop || by > contentBottom) continue

            val isActive    = StorageCache.currentPage?.index == page.pageSlot.index
            val isHover     = mx in bx..(bx + BOX_W) && my in by..(by + BOX_H)
            val pageMatches = pageMatchesSearch(page.pageSlot)

            val boxColor = when {
                isActive -> COLOR_ACTIVE_BOX
                isHover  -> COLOR_HOVER_BOX
                else     -> COLOR_BOX_BG
            }
            RenderUtils.drawRoundedRect(ctx,
                (bx - 1).toFloat(), (by - 1).toFloat(),
                (BOX_W + 2).toFloat(), (BOX_H + 2).toFloat(),
                4, COLOR_BOX_BORDER)
            RenderUtils.drawRoundedRect(ctx,
                bx.toFloat(), by.toFloat(),
                BOX_W.toFloat(), BOX_H.toFloat(),
                3, boxColor)

            // Page icon + name
            ctx.drawItem(page.icon, bx + BOX_PAD, by + BOX_PAD - 1)
            val nameColor = if (isActive) COLOR_ACTIVE_T else COLOR_TEXT
            val maxNameW  = BOX_W - BOX_PAD * 2 - 18
            ctx.drawText(tr,
                Text.literal(truncate(tr, page.name, maxNameW)),
                bx + BOX_PAD + 18, by + BOX_PAD + 4, nameColor, false)

            // Item grid (cached contents, skipping first row)
            val items = StorageCache.getPageItems(page.pageSlot)
            val gridX = bx + BOX_PAD
            val gridY = by + BOX_PAD + TITLE_H + 2

            if (isActive) {
                activeItemGridX = gridX
                activeItemGridY = gridY
            }

            val onSubPage = StorageCache.currentPage != null
            for (r in 0 until ITEM_ROWS) {
                for (c in 0 until 9) {
                    val slot = r * 9 + c
                    val sx   = gridX + c * SLOT_SIZE
                    val sy   = gridY + r * SLOT_SIZE
                    ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_SLOT_BG)
                    val stack = items?.getOrNull(slot)
                    if (stack != null && !stack.isEmpty) {
                        ctx.drawItem(stack, sx + 1, sy + 1)
                        ctx.drawStackOverlay(tr, stack, sx + 1, sy + 1)
                        if (pageMatches && !matchesSearch(stack)) {
                            ctx.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_DIM)
                        }
                    }
                    // Hover highlight + tooltip tracking
                    if (mx in sx until sx + SLOT_SIZE
                        && my in sy until sy + SLOT_SIZE
                        && my > contentTop && my < contentBottom) {
                        ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x80FFFFFF.toInt())
                        if (stack != null && !stack.isEmpty) hoveredStack = stack
                    }
                }
            }

            // Dim entire page if nothing in it matches
            if (!pageMatches) {
                ctx.fill(bx, by, bx + BOX_W, by + BOX_H, COLOR_DIM)
            }
        }

        ctx.disableScissor()

        // Scrollbar
        val totalRows = (pages.size + COLS - 1) / COLS
        if (maxScrollRows > 0) {
            val trackH   = contentBottom - contentTop
            val thumbH   = (trackH * visRows / totalRows).coerceAtLeast(12)
            val progress = (scrollCurrent / maxScrollRows).coerceIn(0f, 1f)
            val thumbY   = contentTop + ((trackH - thumbH) * progress).toInt()
            val barX     = panelX + panelW - 5
            ctx.fill(barX, contentTop, barX + 3, contentBottom, COLOR_SCROLL_T)
            ctx.fill(barX, thumbY,     barX + 3, thumbY + thumbH, COLOR_SCROLL)
        }

        drawPlayerInventory(ctx, tr, mx, my)

        // Draw tooltip on top of everything else.
        // DrawContext.drawItemTooltip only stores a deferred Runnable in tooltipDrawer;
        // it never draws visibly when called from afterRender because drawDeferredElements()
        // is called by renderWithTooltip() AFTER render() returns, and the deferred slot
        // may already have been claimed. drawTooltipImmediately() bypasses deferral entirely.
        val hovered = hoveredStack
        if (hovered != null && !hovered.isEmpty) {
            val mc = MinecraftClient.getInstance()
            val tooltipTexts = MCScreen.getTooltipFromItem(mc, hovered)
            val tooltipComponents = tooltipTexts.map { TooltipComponent.of(it.asOrderedText()) }
            ctx.drawTooltipImmediately(tr, tooltipComponents, mx, my, HoveredTooltipPositioner.INSTANCE, null)
        }

        // Re-draw cursor (held/dragged) item on top of the overlay so it isn't buried underneath
        val cursorStack = MinecraftClient.getInstance().player?.currentScreenHandler?.cursorStack
        if (cursorStack != null && !cursorStack.isEmpty) {
            ctx.drawItem(cursorStack, mx - 8, my - 8)
            ctx.drawStackOverlay(tr, cursorStack, mx - 8, my - 8)
        }
    }

    private fun drawPlayerInventory(
            ctx: DrawContext, tr: net.minecraft.client.font.TextRenderer, mx: Int, my: Int) {
        val player = MinecraftClient.getInstance().player ?: return
        val inv = player.inventory

        // Background
        RenderUtils.drawRoundedRect(ctx,
            (invX - 1).toFloat(), (invY - 1).toFloat(),
            (INV_W + 2).toFloat(), (INV_H + 2).toFloat(),
            6, COLOR_BORDER)
        RenderUtils.drawRoundedRect(ctx,
            invX.toFloat(), invY.toFloat(),
            INV_W.toFloat(), INV_H.toFloat(),
            5, COLOR_BG)

        // Main inventory: 3 rows (slots 9-35 via Inventory.getStack)
        for (row in 0..2) {
            for (col in 0..8) {
                val slotIdx = 9 + row * 9 + col
                val sx = invX + INV_PAD + col * SLOT_SIZE
                val sy = invY + INV_PAD + row * SLOT_SIZE
                val isHover = mx in sx until sx + SLOT_SIZE && my in sy until sy + SLOT_SIZE
                ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_SLOT_BG)
                val stack = inv.getStack(slotIdx)
                if (!stack.isEmpty) {
                    ctx.drawItem(stack, sx + 1, sy + 1)
                    ctx.drawStackOverlay(tr, stack, sx + 1, sy + 1)
                }
                if (isHover) {
                    ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x80FFFFFF.toInt())
                    if (!stack.isEmpty) hoveredStack = stack
                }
            }
        }

        // Divider between main inventory and hotbar
        val hotbarY = invY + INV_PAD + 3 * SLOT_SIZE + INV_HOTBAR_GAP
        ctx.fill(invX + INV_PAD, hotbarY - 2,
                 invX + INV_W - INV_PAD, hotbarY - 1, COLOR_DIVIDER)

        // Hotbar: slots 0-8
        for (col in 0..8) {
            val sx = invX + INV_PAD + col * SLOT_SIZE
            val sy = hotbarY
            val isHover = mx in sx until sx + SLOT_SIZE && my in sy until sy + SLOT_SIZE
            ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_SLOT_BG)
            val stack = inv.getStack(col)
            if (!stack.isEmpty) {
                ctx.drawItem(stack, sx + 1, sy + 1)
                ctx.drawStackOverlay(tr, stack, sx + 1, sy + 1)
            }
            if (isHover) {
                ctx.fill(sx, sy, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x80FFFFFF.toInt())
                if (!stack.isEmpty) hoveredStack = stack
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private fun handleScroll(mx: Double, my: Double, vert: Double): Boolean {
        if (mx < panelX || mx > panelX + panelW) return false
        if (my < panelY || my > panelY + panelH)  return false
        val delta = StorageConfigManager.config.scrollSpeed * (if (vert < 0) 1f else -1f)
        scrollTarget = (scrollTarget + delta).coerceIn(0f, maxScrollRows.toFloat())
        return true
    }

    private fun handleClick(mx: Double, my: Double, button: Int, handler: ScreenHandler?): Boolean {
        val onSubPage = StorageCache.currentPage != null
        val client = MinecraftClient.getInstance()
        val player = client.player

        // ── Player inventory panel: send clickSlot manually ───────────────────
        if (handler != null && player != null &&
            mx >= invX && mx <= invX + INV_W && my >= invY && my <= invY + INV_H) {
            val col = ((mx - invX - INV_PAD) / SLOT_SIZE).toInt()
            if (col in 0..8) {
                val chestSlots = handler.slots.size - 36
                val hotbarAY = invY + INV_PAD + 3 * SLOT_SIZE + INV_HOTBAR_GAP
                val slotIndex: Int? = when {
                    my >= hotbarAY && my < hotbarAY + SLOT_SIZE ->
                        chestSlots + 27 + col
                    my >= invY + INV_PAD && my < invY + INV_PAD + 3 * SLOT_SIZE -> {
                        val row = ((my - invY - INV_PAD) / SLOT_SIZE).toInt()
                        if (row in 0..2) chestSlots + row * 9 + col else null
                    }
                    else -> null
                }
                if (slotIndex != null) {
                    val action = if (isShiftDown()) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP
                    client.interactionManager?.clickSlot(handler.syncId, slotIndex, button, action, player)
                }
            }
            return false
        }

        // ── Active chest item grid: send clickSlot manually ───────────────────
        if (handler != null && player != null && onSubPage && activeItemGridX >= 0) {
            val relX = mx - activeItemGridX
            val relY = my - activeItemGridY
            if (relX >= 0 && relX < BOX_CONTENT_W && relY >= 0 && relY < BOX_CONTENT_H) {
                val col = (relX / SLOT_SIZE).toInt()
                val row = (relY / SLOT_SIZE).toInt()
                if (col in 0..8 && row in 0 until ITEM_ROWS) {
                    // Slot 0-8 = nav row (skipped); slots 9+ = item rows
                    val slotIndex = 9 + row * 9 + col
                    val action = if (isShiftDown()) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP
                    client.interactionManager?.clickSlot(handler.syncId, slotIndex, button, action, player)
                }
                return false
            }
        }

        // ── Only left-click navigates the overview panel ──────────────────────
        if (button != 0) return true

        if (mx < panelX || mx > panelX + panelW) {
            searchFocused = false
            return true
        }
        if (my < panelY || my > panelY + panelH) {
            searchFocused = false
            return true
        }

        // Search bar click
        val searchDivY = panelY + panelH - PANEL_PAD - SEARCH_ROW
        val sbY = searchDivY + (SEARCH_ROW - SEARCH_H) / 2
        if (my >= sbY && my <= sbY + SEARCH_H) {
            searchFocused = true
            return false
        }

        val contentTop = panelY + PANEL_PAD + 22
        if (my < contentTop) return false

        // Page box click (overview navigation)
        val col = ((mx - panelX - PANEL_PAD) / (BOX_W + BOX_GAP)).toInt()
        val row = ((my - contentTop + (scrollCurrent * (BOX_H + BOX_GAP)).toInt()) / (BOX_H + BOX_GAP)).toInt()
        val idx = row * COLS + col

        val page = StorageCache.pages.getOrNull(idx) ?: return false
        if (StorageCache.currentPage?.index == page.pageSlot.index) return false

        suppressCursorReset = true
        page.pageSlot.navigateTo()
        return false
    }

    private fun handleKeyPress(keyInput: net.minecraft.client.input.KeyInput): Boolean {
        if (!searchFocused) return true
        val key = keyInput.key()
        return when (key) {
            GLFW.GLFW_KEY_ESCAPE -> {
                if (searchText.isNotEmpty()) searchText = "" else searchFocused = false
                false
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (searchText.isNotEmpty()) searchText = searchText.dropLast(1)
                false
            }
            GLFW.GLFW_KEY_SPACE -> {
                searchText += ' '
                false
            }
            else -> {
                val name = GLFW.glfwGetKeyName(key, keyInput.scancode())
                if (name != null && name.length == 1) {
                    searchText += name[0]
                    false
                } else true
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    private fun matchesSearch(stack: net.minecraft.item.ItemStack): Boolean {
        if (searchText.isEmpty()) return true
        if (stack.isEmpty) return false
        return stack.name.string.contains(searchText, ignoreCase = true)
    }

    private fun pageMatchesSearch(pageSlot: StoragePageSlot): Boolean {
        if (searchText.isEmpty()) return true
        val items = StorageCache.getPageItems(pageSlot) ?: return true  // unknown = don't dim
        return items.any { matchesSearch(it) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun isShiftDown(): Boolean {
        val handle = MinecraftClient.getInstance().window.handle
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
    }

    private fun truncate(tr: net.minecraft.client.font.TextRenderer,
                         text: String, maxPx: Int): String {
        if (tr.getWidth(text) <= maxPx) return text
        var t = text
        while (t.isNotEmpty() && tr.getWidth("$t…") > maxPx) t = t.dropLast(1)
        return "$t…"
    }
}
