package com.glorpaddons.commissions

import com.glorpaddons.util.RenderUtils
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier

object CommissionHud {

    private val SAMPLE_COMMISSIONS = listOf(
        Commission("Royal Mines Mithril", 72, 100),
        Commission("Lucky Raffle", 100, 100),
        Commission("Goblin Raid Slayer", 30, 100),
        Commission("Treasure Hoarder Puncher", 0, 100)
    )

    // Colors (ARGB)
    private val COLOR_BACKGROUND = 0xC8101820.toInt()
    private val COLOR_TITLE      = 0xFFFFD700.toInt()
    private val COLOR_NAME       = 0xFFFFFFFF.toInt()
    private val COLOR_PROGRESS   = 0xFFAAAAAA.toInt()
    private val COLOR_DONE_TEXT  = 0xFF55FF55.toInt()
    private val COLOR_BAR_BG     = 0xFF2A2A40.toInt()
    private val COLOR_BAR_FILL   = 0xFF2196F3.toInt()
    private val COLOR_BAR_DONE   = 0xFF00C853.toInt()

    private const val PADDING    = 8f
    private const val RADIUS     = 6
    private const val BAR_HEIGHT = 4f

    fun register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.of("glorpaddons", "commission_hud")
        ) { context, _ ->
            if (ConfigManager.config.enabled && CommissionTracker.isInAllowedArea) {
                render(context, CommissionTracker.commissions)
            }
        }
    }

    /** Renders the HUD with sample data for use in the config screen. */
    fun renderPreview(context: DrawContext) {
        render(context, SAMPLE_COMMISSIONS)
    }

    fun render(context: DrawContext, commissions: List<Commission>) {
        val cfg = ConfigManager.config
        val client = MinecraftClient.getInstance()
        val tr = client.textRenderer

        val x = cfg.x
        val y = cfg.y
        val w = cfg.width
        val h = cfg.height
        val innerW = w - PADDING * 2

        // Background
        RenderUtils.drawRoundedRect(context, x, y, w, h, RADIUS, COLOR_BACKGROUND)

        // Title
        context.drawText(tr, "⛏ Commissions", (x + PADDING).toInt(), (y + PADDING).toInt(), COLOR_TITLE, true)

        var offsetY = y + PADDING + 14f

        if (commissions.isEmpty()) {
            context.drawText(tr, "No commissions found", (x + PADDING).toInt(), offsetY.toInt(), COLOR_PROGRESS, false)
            return
        }

        for (commission in commissions) {
            if (offsetY + 22f > y + h - PADDING) break

            // Commission name (truncated) and progress text on the same line
            val progressText = if (commission.isDone) "DONE" else "${commission.current}%"
            val progressWidth = tr.getWidth(progressText)
            val nameMaxWidth = (innerW - progressWidth - 6).toInt()
            val name = truncate(tr, commission.name, nameMaxWidth)

            context.drawText(tr, name, (x + PADDING).toInt(), offsetY.toInt(), COLOR_NAME, false)

            val progressColor = if (commission.isDone) COLOR_DONE_TEXT else COLOR_PROGRESS
            context.drawText(tr, progressText, (x + w - PADDING - progressWidth).toInt(), offsetY.toInt(), progressColor, false)

            offsetY += 12f

            // Progress bar
            RenderUtils.drawRoundedRect(context, x + PADDING, offsetY, innerW, BAR_HEIGHT, 2, COLOR_BAR_BG)

            val fillWidth = (innerW * commission.progress).coerceAtLeast(0f).coerceAtMost(innerW)
            if (fillWidth > 0f) {
                val barColor = if (commission.isDone) COLOR_BAR_DONE else COLOR_BAR_FILL
                RenderUtils.drawRoundedRect(context, x + PADDING, offsetY, fillWidth, BAR_HEIGHT, 2, barColor)
            }

            offsetY += BAR_HEIGHT + 8f
        }
    }

    private fun truncate(tr: net.minecraft.client.font.TextRenderer, text: String, maxWidth: Int): String {
        if (tr.getWidth(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && tr.getWidth("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }
}
