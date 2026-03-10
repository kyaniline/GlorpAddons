package com.glorpaddons.commissions

import com.glorpaddons.util.RenderUtils
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class MiningConfigScreen(private val parent: Screen) : Screen(Text.literal("Mining Config")) {

    private val handleSize = 12

    private var dragging      = false
    private var resizing      = false
    private var dragOffsetX   = 0f
    private var dragOffsetY   = 0f

    // Bottom bar buttons
    private var backRect  = Rect(0, 0, 0, 0)
    private var resetRect = Rect(0, 0, 0, 0)

    override fun init() {
        backRect  = Rect(width / 2 - 170, height - 30, 76, 20)
        resetRect = Rect(width / 2 + 94,  height - 30, 106, 20)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val cfg = ConfigManager.config

        // HUD border (gold = enabled, grey = disabled)
        val borderColor = if (cfg.enabled) 0xFF888888.toInt() else 0xFF555566.toInt()
        RenderUtils.drawRoundedRect(context, cfg.x - 1f, cfg.y - 1f, cfg.width + 2f, cfg.height + 2f, 7, borderColor)

        // HUD preview
        CommissionHud.renderPreview(context)

        // Dim tint when disabled
        if (!cfg.enabled) {
            context.fill(cfg.x.toInt(), cfg.y.toInt(), (cfg.x + cfg.width).toInt(), (cfg.y + cfg.height).toInt(), 0x66000000)
        }

        // Resize handle
        val hx = (cfg.x + cfg.width  - handleSize / 2f).toInt()
        val hy = (cfg.y + cfg.height - handleSize / 2f).toInt()
        context.fill(hx, hy, hx + handleSize, hy + handleSize, 0xFF888888.toInt())
        context.drawText(textRenderer, "↘", hx + 1, hy, 0xFF000000.toInt(), false)

        // Title
        drawText(context, "⛏ Mining Config", width / 2, 10, 0xFFFFFFFF.toInt(), centered = true)

        // Bottom hint
        drawText(context, "Drag to move  |  Drag ↘ to resize  |  ESC to save & back",
            width / 2, height - 46, 0xFF888899.toInt(), centered = true)

        // Bottom buttons
        drawBtn(context, backRect,  "◀  Back",          mouseX, mouseY)
        drawBtn(context, resetRect, "Reset Position",    mouseX, mouseY)

        super.render(context, mouseX, mouseY, delta)
    }

    private fun drawBtn(context: DrawContext, r: Rect, label: String, mouseX: Int, mouseY: Int) {
        val hover = r.contains(mouseX, mouseY)
        // Border
        RenderUtils.drawRoundedRect(context, (r.x - 1).toFloat(), (r.y - 1).toFloat(), (r.w + 2).toFloat(), (r.h + 2).toFloat(),
            5, if (hover) 0x70888888 else 0x35FFFFFF)
        // Fill
        RenderUtils.drawRoundedRect(context, r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(),
            4, if (hover) 0x35888888 else 0x20FFFFFF)
        // Label
        drawText(context, label, r.x + r.w / 2, r.y + 6, 0xFFFFFFFF.toInt(), centered = true)
    }

    private fun drawText(context: DrawContext, str: String, x: Int, y: Int, color: Int, centered: Boolean = false) {
        val t = Text.literal(str)
        if (centered) context.drawCenteredTextWithShadow(textRenderer, t, x, y, color)
        else          context.drawText(textRenderer, t, x, y, color, true)
    }

    // ── Input ────────────────────────────────────────────────────────────────

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() != 0) return super.mouseClicked(click, doubled)
        val mx = click.x.toInt(); val my = click.y.toInt()
        val mxf = click.x.toFloat(); val myf = click.y.toFloat()
        val cfg = ConfigManager.config

        if (backRect.contains(mx, my))  { client?.setScreen(parent); return true }
        if (resetRect.contains(mx, my)) {
            cfg.apply { x = 10f; y = 10f; width = 210f; height = 140f }
            return true
        }

        // Resize handle
        val hx = cfg.x + cfg.width  - handleSize / 2f
        val hy = cfg.y + cfg.height - handleSize / 2f
        if (mxf >= hx && mxf <= hx + handleSize && myf >= hy && myf <= hy + handleSize) {
            resizing = true; return true
        }

        // Drag HUD body
        if (mxf >= cfg.x && mxf <= cfg.x + cfg.width && myf >= cfg.y && myf <= cfg.y + cfg.height) {
            dragging = true
            dragOffsetX = mxf - cfg.x
            dragOffsetY = myf - cfg.y
            return true
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        val cfg = ConfigManager.config
        val mx = click.x.toFloat(); val my = click.y.toFloat()

        if (resizing) {
            cfg.width  = (mx - cfg.x + handleSize / 2f).coerceIn(100f, width.toFloat()  - cfg.x)
            cfg.height = (my - cfg.y + handleSize / 2f).coerceIn(80f,  height.toFloat() - cfg.y)
            return true
        }
        if (dragging) {
            cfg.x = (mx - dragOffsetX).coerceIn(0f, width.toFloat()  - cfg.width)
            cfg.y = (my - dragOffsetY).coerceIn(0f, height.toFloat() - cfg.height)
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: Click): Boolean {
        dragging = false; resizing = false
        return super.mouseReleased(click)
    }

    override fun close() { ConfigManager.save(); client?.setScreen(parent) }
    override fun shouldPause() = false
    override fun applyBlur(context: DrawContext) { }
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) { }
}
