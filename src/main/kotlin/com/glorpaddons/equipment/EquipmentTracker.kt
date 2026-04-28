package com.glorpaddons.equipment

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack

/**
 * Reads equipment items from the Hypixel /equipment GUI whenever it is open.
 * Hypixel places the four equipment items in column 1 (slot % 9 == 1) of rows
 * 1–4 (slot / 9 in 1..4), i.e. handler slots 10, 19, 28, 37.
 */
object EquipmentTracker {

    val equipment: Array<ItemStack> = Array(4) { ItemStack.EMPTY }

    fun tick(client: MinecraftClient) {
        val screen = client.currentScreen as? GenericContainerScreen ?: return
        if (!screen.title.string.lowercase().contains("equipment")) return

        val handler = screen.screenHandler
        for (i in 0..3) {
            val slotIdx = 10 + i * 9
            if (slotIdx >= handler.slots.size) break
            val stack = handler.slots[slotIdx].stack
            val name = stack.name.string.trim().lowercase()
            equipment[i] = if (stack.isEmpty || name.startsWith("empty")) ItemStack.EMPTY else stack
        }
    }

    /** Returns a snapshot copy so the screen can display a stable view. */
    fun snapshot(): Array<ItemStack> = equipment.copyOf()
}
