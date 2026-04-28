package com.glorpaddons.farming

import net.minecraft.client.MinecraftClient
import net.minecraft.component.DataComponentTypes

object LowerSensitivity {

    /** Skyblock item IDs of all farming tools that trigger mouse lock. */
    private val FARMING_TOOL_IDS: Set<String> = setOf(
        "THEORETICAL_HOE_WHEAT_1", "THEORETICAL_HOE_WHEAT_2", "THEORETICAL_HOE_WHEAT_3",
        "THEORETICAL_HOE_CARROT_1", "THEORETICAL_HOE_CARROT_2", "THEORETICAL_HOE_CARROT_3",
        "THEORETICAL_HOE_POTATO_1", "THEORETICAL_HOE_POTATO_2", "THEORETICAL_HOE_POTATO_3",
        "THEORETICAL_HOE_CANE_1", "THEORETICAL_HOE_CANE_2", "THEORETICAL_HOE_CANE_3",
        "THEORETICAL_HOE_SUNFLOWER_1", "THEORETICAL_HOE_SUNFLOWER_2", "THEORETICAL_HOE_SUNFLOWER_3",
        "THEORETICAL_HOE_WILD_ROSE_1", "THEORETICAL_HOE_WILD_ROSE_2", "THEORETICAL_HOE_WILD_ROSE_3",
        "THEORETICAL_HOE_WARTS_1", "THEORETICAL_HOE_WARTS_2", "THEORETICAL_HOE_WARTS_3",
        "FUNGI_CUTTER", "FUNGI_CUTTER_2", "FUNGI_CUTTER_3",
        "CACTUS_KNIFE", "CACTUS_KNIFE_2", "CACTUS_KNIFE_3",
        "MELON_DICER", "MELON_DICER_2", "MELON_DICER_3",
        "PUMPKIN_DICER", "PUMPKIN_DICER_2", "PUMPKIN_DICER_3",
        "COCO_CHOPPER", "COCO_CHOPPER_2", "COCO_CHOPPER_3",
        "BASIC_GARDENING_HOE", "ADVANCED_GARDENING_HOE", "BINGHOE"
    )

    @Volatile
    private var sensitivityLowered = false

    @JvmStatic
    fun isSensitivityLowered(): Boolean = sensitivityLowered

    fun tick(client: MinecraftClient) {
        val cfg = FarmingConfigManager.config
        if (!cfg.mouseLockEnabled || client.player == null) {
            if (sensitivityLowered) sensitivityLowered = false
            return
        }

        val stack = client.player!!.mainHandStack
        val skyblockId = getSkyblockId(stack)
        val holdingTool = skyblockId != null && skyblockId in FARMING_TOOL_IDS
        val groundCheck = !cfg.mouseLockGroundOnly || client.player!!.isOnGround

        val shouldLock = holdingTool && groundCheck
        if (shouldLock != sensitivityLowered) {
            sensitivityLowered = shouldLock
        }
    }

    fun isFarmingTool(skyblockId: String): Boolean = skyblockId in FARMING_TOOL_IDS

    /** Reads the Hypixel Skyblock item ID from the item's ExtraAttributes NBT compound. */
    fun getSkyblockIdPublic(stack: net.minecraft.item.ItemStack): String? = getSkyblockId(stack)

    private fun getSkyblockId(stack: net.minecraft.item.ItemStack): String? {
        if (stack.isEmpty) return null
        val nbt = stack.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt() ?: return null
        val extras = nbt.getCompound("ExtraAttributes").orElse(null) ?: return null
        val id = extras.getString("id").orElse("") ?: return null
        return if (id.isEmpty()) null else id
    }
}
