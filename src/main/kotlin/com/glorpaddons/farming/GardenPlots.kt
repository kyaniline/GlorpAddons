package com.glorpaddons.farming

import com.glorpaddons.util.RenderUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * Parses the Hypixel tab list for pest-infested plot numbers and displays
 * them as a small HUD panel when any plots are infested.
 *
 * The tab list entry from Hypixel looks like:
 *   "Plots: 1, 3, 7"
 * where the numbers are infested plot IDs.
 */
object GardenPlots {

    private var infestedPlots: List<Int> = emptyList()
    private var lastUpdateTick = 0L

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (FarmingConfigManager.config.gardenPlotsEnabled) {
                val tick = System.currentTimeMillis()
                if (tick - lastUpdateTick > 2000) {
                    lastUpdateTick = tick
                    updateFromTabList()
                }
            }
        }

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.of("glorpaddons", "garden_plots")
        ) { context, _ ->
            if (FarmingConfigManager.config.gardenPlotsEnabled && infestedPlots.isNotEmpty()) {
                render(context)
            }
        }
    }

    private fun updateFromTabList() {
        val client = MinecraftClient.getInstance()
        val handler = client.networkHandler ?: return

        val found = mutableListOf<Int>()
        for (entry in handler.playerList) {
            val text = (entry.displayName ?: Text.literal(entry.profile.name)).string
                .replace(Regex("\u00A7."), "")
                .trim()

            if (text.startsWith("Plots:")) {
                val part = text.substringAfter("Plots:").trim()
                for (token in part.split(",")) {
                    val num = token.trim().toIntOrNull()
                    if (num != null) found += num
                }
                break
            }
        }
        infestedPlots = found.sorted()
    }

    private fun render(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val tr = client.textRenderer

        val plotText = infestedPlots.joinToString(", ")
        val title = "§c⚠ Infested Plots"
        val body  = "§fPlots: §e$plotText"

        val lineH = tr.fontHeight + 2
        val w = maxOf(
            tr.getWidth(title.replace(Regex("\u00A7."), "")),
            tr.getWidth(body.replace(Regex("\u00A7."), ""))
        ) + 16f
        val h = 2 * lineH + 10f

        // Bottom-left corner, above chat
        val screenH = client.window.scaledHeight
        val x = 4
        val y = screenH - h.toInt() - 60

        RenderUtils.drawRoundedRect(context, x.toFloat(), y.toFloat(), w, h, 4, 0xD0101820.toInt())
        context.drawText(tr, Text.literal(title), x + 8, y + 4, 0xFFFF5555.toInt(), true)
        context.drawText(tr, Text.literal(body),  x + 8, y + 4 + lineH, 0xFFFFFFFF.toInt(), true)
    }
}
