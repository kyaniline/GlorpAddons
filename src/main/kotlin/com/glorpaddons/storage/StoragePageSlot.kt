package com.glorpaddons.storage

import net.minecraft.client.MinecraftClient

/**
 * Identifies a storage page by index (0-8 = Ender Chest, 9-26 = Backpack).
 * Mirrors Hypixel's storage layout.
 */
data class StoragePageSlot(val index: Int) : Comparable<StoragePageSlot> {

    val isEnderChest get() = index < 9
    val isBackpack get() = index >= 9

    val displayName: String get() = when {
        isEnderChest -> "Ender Chest ${index + 1}"
        else -> "Backpack ${index - 9 + 1}"
    }

    /** Navigate to this page via Hypixel commands. Works from any screen. */
    fun navigateTo() {
        val client = MinecraftClient.getInstance()
        val cmd = if (isEnderChest) "enderchest ${index + 1}" else "backpack ${index - 9 + 1}"
        client.networkHandler?.sendChatCommand(cmd)
    }

    override fun compareTo(other: StoragePageSlot) = index.compareTo(other.index)

    companion object {
        /**
         * Maps an overview chest slot index to a StoragePageSlot.
         * Slots 9-17 = Ender Chest pages, slots 27-44 = Backpack pages.
         */
        fun fromOverviewSlot(slotIndex: Int): StoragePageSlot? = when (slotIndex) {
            in 9..17  -> StoragePageSlot(slotIndex - 9)
            in 27..44 -> StoragePageSlot(slotIndex - 27 + 9)
            else      -> null
        }

        private val EC_REGEX = Regex("^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$")
        private val BP_REGEX = Regex("^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$")

        /** Parses the screen title of an open storage sub-page to identify the page. */
        fun fromTitle(title: String): StoragePageSlot? {
            EC_REGEX.find(title)?.let {
                return StoragePageSlot(it.groupValues[1].toInt() - 1)
            }
            BP_REGEX.find(title)?.let {
                return StoragePageSlot(it.groupValues[1].toInt() - 1 + 9)
            }
            return null
        }
    }
}
