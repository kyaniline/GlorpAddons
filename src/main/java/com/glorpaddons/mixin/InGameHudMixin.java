package com.glorpaddons.mixin;

import com.glorpaddons.itemrarity.ItemRarityBg;
import com.glorpaddons.itemrarity.ItemRarityConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderHotbarItem", at = @At("HEAD"))
    private void glorpaddons$drawHotbarRarityBg(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (!ItemRarityConfigManager.INSTANCE.getConfig().getEnabled()) return;
        if (stack.isEmpty()) return;

        Integer color = ItemRarityBg.INSTANCE.getRarityColor(stack);
        if (color == null) return;

        ItemRarityBg.INSTANCE.drawBackground(context, x, y, color);
    }
}
