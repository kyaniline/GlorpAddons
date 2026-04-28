package com.glorpaddons.farming

import com.glorpaddons.util.RenderUtils
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.component.DataComponentTypes
import net.minecraft.text.Text

/**
 * Displays the items required by the current garden visitor, read from
 * the visitor's accept-offer button lore (slot 29).
 *
 * The panel appears at the top-left of the screen whenever a "Visitor"
 * container is open.  Lore parsing:
 *   "Items Required" line is found, then each following non-empty line
 *   is a "<name> x<count>" requirement.
 */
object VisitorHelper {

    data class Requirement(val name: String, val amount: Int)

    private var currentRequirements: List<Requirement> = emptyList()
    private var visitorName: String = ""

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is GenericContainerScreen) return@register
            if (!screen.title.string.lowercase().contains("visitor")) return@register

            ScreenEvents.afterTick(screen).register { _ ->
                if (FarmingConfigManager.config.visitorHelperEnabled) {
                    readVisitorData(screen)
                }
            }

            ScreenEvents.afterRender(screen).register { _, context, mouseX, mouseY, _ ->
                if (FarmingConfigManager.config.visitorHelperEnabled) {
                    render(context)
                }
            }

            ScreenEvents.remove(screen).register { _ ->
                currentRequirements = emptyList()
                visitorName = ""
            }
        }
    }

    private fun readVisitorData(screen: GenericContainerScreen) {
        val handler = screen.screenHandler
        if (handler.slots.size <= 29) return

        // Slot 13: visitor head (name)
        val headStack = handler.slots[13].stack
        if (!headStack.isEmpty) {
            visitorName = headStack.name.string.trim()
        }

        // Slot 29: accept button with required items in lore
        val acceptStack = handler.slots[29].stack
        if (acceptStack.isEmpty) {
            currentRequirements = emptyList()
            return
        }

        val lore = acceptStack.get(DataComponentTypes.LORE) ?: run {
            currentRequirements = emptyList()
            return
        }

        val lines = lore.lines().map { it.string.replace(Regex("\u00A7."), "").trim() }
        val reqIndex = lines.indexOfFirst { it.contains("Items Required") }
        if (reqIndex < 0) {
            currentRequirements = emptyList()
            return
        }

        val parsed = mutableListOf<Requirement>()
        for (i in (reqIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) break
            // Format: "<item name> x<count>" or just "<item name>"
            val xIdx = line.lastIndexOf(" x")
            if (xIdx >= 0) {
                val name = line.substring(0, xIdx).trim()
                val amount = line.substring(xIdx + 2).replace(",", "").toIntOrNull() ?: 1
                parsed += Requirement(name, amount)
            } else {
                parsed += Requirement(line, 1)
            }
        }
        currentRequirements = parsed
    }

    private fun render(context: DrawContext) {
        if (currentRequirements.isEmpty() && visitorName.isEmpty()) return

        val client = MinecraftClient.getInstance()
        val tr = client.textRenderer

        val title = if (visitorName.isNotEmpty()) "Visitor: $visitorName" else "Visitor"
        val reqLines = currentRequirements.map { r ->
            if (r.amount > 1) "${r.name} ×${"%,d".format(r.amount)}" else r.name
        }
        val allLines = listOf(title) + reqLines

        val lineH = tr.fontHeight + 2
        val w = allLines.maxOf { tr.getWidth(it) } + 16f
        val h = allLines.size * lineH + 10f
        val x = 4
        val y = 4

        RenderUtils.drawRoundedRect(context, x.toFloat(), y.toFloat(), w, h, 4, 0xD0101820.toInt())
        context.fill(x + 4, y + lineH + 4, (x + w - 5).toInt(), y + lineH + 5, 0x40FFFFFF)

        for ((i, line) in allLines.withIndex()) {
            val color = if (i == 0) 0xFFFFD700.toInt() else 0xFFCCCCCC.toInt()
            context.drawText(tr, Text.literal(line), x + 8, y + 5 + i * lineH, color, true)
        }
    }
}
