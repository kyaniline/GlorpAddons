package com.glorpaddons.storage

import net.minecraft.item.ItemStack

data class StoragePage(
    val pageSlot: StoragePageSlot,
    val icon: ItemStack,
    val name: String
)
