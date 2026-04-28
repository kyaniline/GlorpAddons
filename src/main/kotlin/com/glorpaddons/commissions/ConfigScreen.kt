package com.glorpaddons.commissions

import com.glorpaddons.equipment.EquipmentConfigManager
import com.glorpaddons.farming.FarmingConfigManager
import com.glorpaddons.itemrarity.ItemRarityConfigManager
import com.glorpaddons.misc.MiscConfigManager
import com.glorpaddons.mobesp.MobEspConfigManager
import com.glorpaddons.storage.StorageConfigManager
import com.glorpaddons.util.RenderUtils
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/** Simple hit-test rectangle, shared across config screens. */
internal data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun contains(mx: Int, my: Int) = mx in x..(x + w) && my in y..(y + h)
}

/**
 * Main /ga config panel.
 * Square, rounded, semi-transparent.  Left column = section nav, right = content.
 * Add new sections by appending to [sections].
 */
class ConfigScreen : Screen(Text.literal("GlorpAddons")) {

    // ── Layout constants ────────────────────────────────────────────────────
    private val panelSize = 280
    private val colW      = 86
    private val pad       = 12

    // ── Sections ────────────────────────────────────────────────────────────
    private val sections  = listOf("⛏  Mining", "Inventory", "Mob ESP", "Misc", "Farming")
    private var selected  = 0

    // ── Hit rects (computed in init) ────────────────────────────────────────
    private var px = 0; private var py = 0
    private val sectionRects = mutableListOf<Rect>()

    // Mining
    private var toggleRect        = Rect(0, 0, 0, 0)
    private var editRect          = Rect(0, 0, 0, 0)

    // Inventory
    private var storageToggleRect   = Rect(0, 0, 0, 0)
    private var scrollDecRect       = Rect(0, 0, 0, 0)
    private var scrollIncRect       = Rect(0, 0, 0, 0)
    private var rarityBgToggleRect  = Rect(0, 0, 0, 0)
    private var equipmentToggleRect = Rect(0, 0, 0, 0)

    // Mob ESP
    private var batEspToggleRect   = Rect(0, 0, 0, 0)

    // Misc
    private var cancelAnimToggleRect = Rect(0, 0, 0, 0)

    // Farming
    private var mouseLockToggleRect        = Rect(0, 0, 0, 0)
    private var mouseLockGroundToggleRect  = Rect(0, 0, 0, 0)
    private var pestHighlightToggleRect    = Rect(0, 0, 0, 0)
    private var farmingHudToggleRect       = Rect(0, 0, 0, 0)
    private var visitorHelperToggleRect    = Rect(0, 0, 0, 0)
    private var gardenPlotsToggleRect      = Rect(0, 0, 0, 0)

    override fun init() {
        px = (width  - panelSize) / 2
        py = (height - panelSize) / 2

        sectionRects.clear()
        sections.forEachIndexed { i, _ ->
            sectionRects += Rect(px + 6, py + 30 + i * 28, colW - 12, 20)
        }

        val cx       = px + colW + pad
        val cy       = py + 46
        val contentW = panelSize - colW - pad * 2
        val toggleX  = px + panelSize - pad - 36

        toggleRect        = Rect(toggleX, cy, 36, 14)
        editRect          = Rect(cx, cy + 22, contentW, 20)
        storageToggleRect  = Rect(toggleX, cy,      36, 14)
        rarityBgToggleRect  = Rect(toggleX, cy + 56, 36, 14)
        equipmentToggleRect = Rect(toggleX, cy + 84, 36, 14)
        batEspToggleRect    = Rect(toggleX, cy,      36, 14)
        cancelAnimToggleRect  = Rect(toggleX, cy,      36, 14)
        mouseLockToggleRect       = Rect(toggleX, cy,       36, 14)
        mouseLockGroundToggleRect = Rect(toggleX, cy + 28,  36, 14)
        pestHighlightToggleRect   = Rect(toggleX, cy + 56,  36, 14)
        farmingHudToggleRect      = Rect(toggleX, cy + 84,  36, 14)
        visitorHelperToggleRect   = Rect(toggleX, cy + 112, 36, 14)
        gardenPlotsToggleRect     = Rect(toggleX, cy + 140, 36, 14)

        val arrowW = 16; val arrowH = 14
        val arrowRightX = px + panelSize - pad - arrowW
        val arrowLeftX  = arrowRightX - arrowW - 22
        val speedY = cy + 28
        scrollDecRect = Rect(arrowLeftX,  speedY, arrowW, arrowH)
        scrollIncRect = Rect(arrowRightX, speedY, arrowW, arrowH)
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val ps = panelSize.toFloat()

        // Panel border + background
        RenderUtils.drawRoundedRect(context, px - 1f, py - 1f, ps + 2f, ps + 2f, 9, 0x50888888)
        RenderUtils.drawRoundedRect(context, px.toFloat(), py.toFloat(), ps, ps, 8, 0xD0101820.toInt())

        // ── Left column ──────────────────────────────────────────────────────
        drawText(context, "GlorpAddons", px + colW / 2, py + 10, 0xFFFFFFFF.toInt(), centered = true)
        context.fill(px + 6, py + 22, px + colW - 6, py + 23, 0x40888888)

        // Section buttons
        sections.forEachIndexed { i, name ->
            val r        = sectionRects[i]
            val isActive = selected == i
            val isHover  = r.contains(mouseX, mouseY)

            val bg = when {
                isActive -> 0x70888888
                isHover  -> 0x28FFFFFF
                else     -> 0
            }

            if (bg != 0) RenderUtils.drawRoundedRect(context, r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(), 4, bg)
            drawText(context, name, r.x + r.w / 2, r.y + 6, 0xFFFFFFFF.toInt(), centered = true)
        }

        // Column divider
        context.fill(px + colW, py + 10, px + colW + 1, py + panelSize - 10, 0x30FFFFFF)

        // ── Right content ────────────────────────────────────────────────────
        when (selected) {
            0 -> renderMining(context, mouseX, mouseY)
            1 -> renderInventory(context, mouseX, mouseY)
            2 -> renderMobEsp(context, mouseX, mouseY)
            3 -> renderMisc(context, mouseX, mouseY)
            4 -> renderFarming(context, mouseX, mouseY)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderMining(context: DrawContext, mouseX: Int, mouseY: Int) {
        val cx = px + colW + pad
        val cy = py + pad + 4

        drawText(context, "Mining", cx, cy, 0xFFFFFFFF.toInt())
        context.fill(cx, cy + 12, px + panelSize - pad, cy + 13, 0x28FFFFFF)

        drawText(context, "Commission HUD", cx, cy + 28, 0xFFCCCCCC.toInt())
        drawToggle(context, toggleRect.x, toggleRect.y, ConfigManager.config.enabled)

        val hovered = editRect.contains(mouseX, mouseY)
        RenderUtils.drawRoundedRect(
            context,
            (editRect.x - 1).toFloat(), (editRect.y - 1).toFloat(),
            (editRect.w + 2).toFloat(), (editRect.h + 2).toFloat(),
            5, if (hovered) 0x50888888 else 0x28FFFFFF
        )
        RenderUtils.drawRoundedRect(
            context,
            editRect.x.toFloat(), editRect.y.toFloat(),
            editRect.w.toFloat(), editRect.h.toFloat(),
            4, if (hovered) 0x40888888 else 0x20FFFFFF
        )
        drawText(context, "Edit Position & Size", editRect.x + editRect.w / 2, editRect.y + 6, 0xFFDDDDDD.toInt(), centered = true)
    }

    private fun renderInventory(context: DrawContext, mouseX: Int, mouseY: Int) {
        val cx = px + colW + pad
        val cy = py + pad + 4

        drawText(context, "Inventory", cx, cy, 0xFFFFFFFF.toInt())
        context.fill(cx, cy + 12, px + panelSize - pad, cy + 13, 0x28FFFFFF)

        drawText(context, "Storage Overlay", cx, cy + 28, 0xFFCCCCCC.toInt())
        drawToggle(context, storageToggleRect.x, storageToggleRect.y, StorageConfigManager.config.enabled)

        drawText(context, "Scroll Speed", cx, cy + 56, 0xFFCCCCCC.toInt())
        drawArrowButton(context, scrollDecRect, "◀", mouseX, mouseY)
        val speed = StorageConfigManager.config.scrollSpeed
        val labelX = scrollDecRect.x + scrollDecRect.w + 11
        drawText(context, speed.toString(), labelX, cy + 57, 0xFFFFFFFF.toInt(), centered = true)
        drawArrowButton(context, scrollIncRect, "▶", mouseX, mouseY)

        drawText(context, "Rarity Backgrounds", cx, cy + 84, 0xFFCCCCCC.toInt())
        drawToggle(context, rarityBgToggleRect.x, rarityBgToggleRect.y, ItemRarityConfigManager.config.enabled)

        drawText(context, "Show Equipment", cx, cy + 112, 0xFFCCCCCC.toInt())
        drawToggle(context, equipmentToggleRect.x, equipmentToggleRect.y, EquipmentConfigManager.config.showEquipmentInInventory)
    }

    private fun renderMisc(context: DrawContext, mouseX: Int, mouseY: Int) {
        val cx = px + colW + pad
        val cy = py + pad + 4

        drawText(context, "Misc", cx, cy, 0xFFFFFFFF.toInt())
        context.fill(cx, cy + 12, px + panelSize - pad, cy + 13, 0x28FFFFFF)

        drawText(context, "Cancel Anim. Updates", cx, cy + 28, 0xFFCCCCCC.toInt())
        drawToggle(context, cancelAnimToggleRect.x, cancelAnimToggleRect.y, MiscConfigManager.config.cancelComponentUpdateAnimations)
    }

    private fun renderMobEsp(context: DrawContext, mouseX: Int, mouseY: Int) {
        val cx = px + colW + pad
        val cy = py + pad + 4

        drawText(context, "Mob ESP", cx, cy, 0xFFFFFFFF.toInt())
        context.fill(cx, cy + 12, px + panelSize - pad, cy + 13, 0x28FFFFFF)

        drawText(context, "Bat ESP", cx, cy + 28, 0xFFCCCCCC.toInt())
        drawToggle(context, batEspToggleRect.x, batEspToggleRect.y, MobEspConfigManager.config.batEspEnabled)
    }

    private fun renderFarming(context: DrawContext, mouseX: Int, mouseY: Int) {
        val cx = px + colW + pad
        val cy = py + pad + 4

        drawText(context, "Farming", cx, cy, 0xFFFFFFFF.toInt())
        context.fill(cx, cy + 12, px + panelSize - pad, cy + 13, 0x28FFFFFF)

        drawText(context, "Mouse Lock", cx, cy + 28, 0xFFCCCCCC.toInt())
        drawToggle(context, mouseLockToggleRect.x, mouseLockToggleRect.y, FarmingConfigManager.config.mouseLockEnabled)

        drawText(context, "Ground Only", cx, cy + 56, 0xFFCCCCCC.toInt())
        drawToggle(context, mouseLockGroundToggleRect.x, mouseLockGroundToggleRect.y, FarmingConfigManager.config.mouseLockGroundOnly)

        drawText(context, "Pest Highlight", cx, cy + 84, 0xFFCCCCCC.toInt())
        drawToggle(context, pestHighlightToggleRect.x, pestHighlightToggleRect.y, FarmingConfigManager.config.pestHighlightEnabled)

        drawText(context, "Farming HUD", cx, cy + 112, 0xFFCCCCCC.toInt())
        drawToggle(context, farmingHudToggleRect.x, farmingHudToggleRect.y, FarmingConfigManager.config.farmingHudEnabled)

        drawText(context, "Visitor Helper", cx, cy + 140, 0xFFCCCCCC.toInt())
        drawToggle(context, visitorHelperToggleRect.x, visitorHelperToggleRect.y, FarmingConfigManager.config.visitorHelperEnabled)

        drawText(context, "Garden Plots", cx, cy + 168, 0xFFCCCCCC.toInt())
        drawToggle(context, gardenPlotsToggleRect.x, gardenPlotsToggleRect.y, FarmingConfigManager.config.gardenPlotsEnabled)
    }

    private fun drawArrowButton(context: DrawContext, r: Rect, label: String, mx: Int, my: Int) {
        val hovered = r.contains(mx, my)
        RenderUtils.drawRoundedRect(context,
            r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(),
            3, if (hovered) 0x50888888 else 0x28FFFFFF)
        drawText(context, label, r.x + r.w / 2, r.y + 3, 0xFFDDDDDD.toInt(), centered = true)
    }

    // ── UI helpers ───────────────────────────────────────────────────────────

    private fun drawToggle(context: DrawContext, x: Int, y: Int, on: Boolean) {
        val w = 36; val h = 14
        RenderUtils.drawRoundedRect(context, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), h / 2,
            if (on) 0xFF00C853.toInt() else 0xFF333344.toInt())
        val knobX = if (on) x + w - h + 2 else x + 2
        RenderUtils.drawRoundedRect(context, knobX.toFloat(), (y + 2).toFloat(), (h - 4).toFloat(), (h - 4).toFloat(), (h - 4) / 2, 0xFFFFFFFF.toInt())
    }

    internal fun drawText(context: DrawContext, str: String, x: Int, y: Int, color: Int, centered: Boolean = false) {
        val t = Text.literal(str)
        if (centered) context.drawCenteredTextWithShadow(textRenderer, t, x, y, color)
        else          context.drawText(textRenderer, t, x, y, color, true)
    }

    // ── Input ────────────────────────────────────────────────────────────────

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x.toInt(); val my = click.y.toInt()

        sectionRects.forEachIndexed { i, r ->
            if (r.contains(mx, my)) { selected = i; return true }
        }

        when (selected) {
            0 -> {
                if (toggleRect.contains(mx, my)) {
                    ConfigManager.config.enabled = !ConfigManager.config.enabled
                    ConfigManager.save()
                    return true
                }
                if (editRect.contains(mx, my)) {
                    client?.setScreen(MiningConfigScreen(this))
                    return true
                }
            }
            1 -> {
                if (storageToggleRect.contains(mx, my)) {
                    StorageConfigManager.config.enabled = !StorageConfigManager.config.enabled
                    StorageConfigManager.save()
                    return true
                }
                if (scrollDecRect.contains(mx, my)) {
                    StorageConfigManager.config.scrollSpeed =
                        (StorageConfigManager.config.scrollSpeed - 1).coerceAtLeast(1)
                    StorageConfigManager.save()
                    return true
                }
                if (scrollIncRect.contains(mx, my)) {
                    StorageConfigManager.config.scrollSpeed =
                        (StorageConfigManager.config.scrollSpeed + 1).coerceAtMost(10)
                    StorageConfigManager.save()
                    return true
                }
                if (rarityBgToggleRect.contains(mx, my)) {
                    ItemRarityConfigManager.config.enabled = !ItemRarityConfigManager.config.enabled
                    ItemRarityConfigManager.save()
                    return true
                }
                if (equipmentToggleRect.contains(mx, my)) {
                    EquipmentConfigManager.config.showEquipmentInInventory = !EquipmentConfigManager.config.showEquipmentInInventory
                    EquipmentConfigManager.save()
                    return true
                }
            }
            2 -> {
                if (batEspToggleRect.contains(mx, my)) {
                    MobEspConfigManager.config.batEspEnabled = !MobEspConfigManager.config.batEspEnabled
                    MobEspConfigManager.save()
                    return true
                }
            }
            3 -> {
                if (cancelAnimToggleRect.contains(mx, my)) {
                    MiscConfigManager.config.cancelComponentUpdateAnimations = !MiscConfigManager.config.cancelComponentUpdateAnimations
                    MiscConfigManager.save()
                    return true
                }
            }
            4 -> {
                if (mouseLockToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.mouseLockEnabled = !FarmingConfigManager.config.mouseLockEnabled
                    FarmingConfigManager.save(); return true
                }
                if (mouseLockGroundToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.mouseLockGroundOnly = !FarmingConfigManager.config.mouseLockGroundOnly
                    FarmingConfigManager.save(); return true
                }
                if (pestHighlightToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.pestHighlightEnabled = !FarmingConfigManager.config.pestHighlightEnabled
                    FarmingConfigManager.save(); return true
                }
                if (farmingHudToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.farmingHudEnabled = !FarmingConfigManager.config.farmingHudEnabled
                    FarmingConfigManager.save(); return true
                }
                if (visitorHelperToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.visitorHelperEnabled = !FarmingConfigManager.config.visitorHelperEnabled
                    FarmingConfigManager.save(); return true
                }
                if (gardenPlotsToggleRect.contains(mx, my)) {
                    FarmingConfigManager.config.gardenPlotsEnabled = !FarmingConfigManager.config.gardenPlotsEnabled
                    FarmingConfigManager.save(); return true
                }
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun close() { ConfigManager.save(); super.close() }
    override fun shouldPause() = false
    override fun applyBlur(context: DrawContext) { }
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) { }
}
