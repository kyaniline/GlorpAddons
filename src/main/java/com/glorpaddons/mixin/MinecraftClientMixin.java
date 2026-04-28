package com.glorpaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.glorpaddons.equipment.EquipmentConfigManager;
import com.glorpaddons.screen.GlorpInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /**
     * Replaces the vanilla inventory screen with our custom one when
     * "Show Equipment in Inventory" is enabled.
     */
    @WrapOperation(
        method = "handleInputEvents",
        at = @At(value = "NEW", target = "net/minecraft/client/gui/screen/ingame/InventoryScreen")
    )
    private InventoryScreen glorpaddons$replaceInventoryScreen(PlayerEntity player, Operation<InventoryScreen> original) {
        if (EquipmentConfigManager.INSTANCE.getConfig().getShowEquipmentInInventory()) {
            return new GlorpInventoryScreen(player);
        }
        return original.call(player);
    }
}
