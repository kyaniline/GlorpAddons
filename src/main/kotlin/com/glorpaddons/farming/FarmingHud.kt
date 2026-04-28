package com.glorpaddons.farming

import com.glorpaddons.util.RenderUtils
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.text.NumberFormat
import java.text.ParseException
import java.util.ArrayDeque
import java.util.Locale
import java.util.regex.Pattern

object FarmingHud {

    private val XP_PATTERN: Pattern = Pattern.compile(
        "\\+([\\d,]+(?:\\.\\d+)?) Farming \\(([\\d,]+(?:\\.\\d+)?%|[\\d,]+/[\\d,]+)\\)"
    )
    private val NUMBER_FORMAT: NumberFormat = NumberFormat.getInstance(Locale.US)

    /** Each entry: Pair(xpGained, timestampMillis). Window: 5 seconds. */
    private val xpEvents: ArrayDeque<Pair<Float, Long>> = ArrayDeque()
    private const val WINDOW_MS = 5_000L

    fun register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            if (overlay && FarmingConfigManager.config.farmingHudEnabled) {
                handleMessage(message)
            }
            true
        }

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.of("glorpaddons", "farming_hud")
        ) { context, _ ->
            if (FarmingConfigManager.config.farmingHudEnabled) {
                render(context)
            }
        }
    }

    private fun handleMessage(message: Text) {
        val plain = message.string.replace(Regex("\u00A7."), "") // strip color codes
        val matcher = XP_PATTERN.matcher(plain)
        if (!matcher.find()) return
        val xp = try {
            NUMBER_FORMAT.parse(matcher.group(1))?.toFloat() ?: return
        } catch (_: ParseException) { return }

        val now = System.currentTimeMillis()
        xpEvents.addLast(Pair(xp, now))
    }

    private fun evict() {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        while (xpEvents.isNotEmpty() && xpEvents.peekFirst().second < cutoff) {
            xpEvents.removeFirst()
        }
    }

    private fun render(context: net.minecraft.client.gui.DrawContext) {
        evict()

        val client = MinecraftClient.getInstance()
        // Only show when holding a farming tool
        val player = client.player ?: return
        val heldId = LowerSensitivity.getSkyblockIdPublic(player.mainHandStack) ?: ""
        val holdingTool = heldId.isNotEmpty() && LowerSensitivity.isFarmingTool(heldId)
        if (!holdingTool && xpEvents.isEmpty()) return

        val blocksPerSec: Double
        val xpPerHour: Double

        if (xpEvents.size < 2) {
            blocksPerSec = 0.0
            xpPerHour = 0.0
        } else {
            val first = xpEvents.peekFirst()
            val last = xpEvents.peekLast()
            val spanSec = (last.second - first.second) / 1000.0
            blocksPerSec = if (spanSec > 0) (xpEvents.size - 1) / spanSec else 0.0
            val totalXp = xpEvents.sumOf { it.first.toDouble() }
            xpPerHour = if (spanSec > 0) totalXp / spanSec * 3600 else 0.0
        }

        val tr = client.textRenderer
        val x = 4
        val y = 4
        val lines = listOf(
            "§e§lFarming",
            "§7Blocks/s: §f%.1f".format(blocksPerSec),
            "§7XP/hr:   §f%,.0f".format(xpPerHour)
        )
        val lineH = tr.fontHeight + 2
        val w = lines.maxOf { tr.getWidth(it.replace(Regex("\u00A7."), "")) } + 12f
        val h = lines.size * lineH + 8f

        RenderUtils.drawRoundedRect(context, x.toFloat(), y.toFloat(), w, h, 4, 0xC0101820.toInt())

        for ((i, line) in lines.withIndex()) {
            context.drawText(tr, Text.literal(line), x + 6, y + 4 + i * lineH, 0xFFFFFFFF.toInt(), true)
        }
    }
}
