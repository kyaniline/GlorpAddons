package com.glorpaddons.mixin;

import com.glorpaddons.itemrarity.ItemRarityBg;
import com.glorpaddons.itemrarity.ItemRarityConfigManager;
import com.glorpaddons.storage.StorageCache;
import com.glorpaddons.storage.StorageConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a rarity-colored gradient background behind each item slot before the
 * vanilla item icon is rendered.
 *
 * HandledScreen.render() pushes translate(this.x, this.y, 0) onto the matrix
 * stack before calling drawSlot, so slot.x / slot.y are already in screen space
 * from DrawContext's perspective — do NOT add this.x / this.y again.
 */
@Mixin(HandledScreen.class)
public abstract class ItemRarityMixin {

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void glorpaddons$drawRarityBg(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!ItemRarityConfigManager.INSTANCE.getConfig().getEnabled()) return;

        // Don't draw behind the storage overlay
        if (StorageConfigManager.INSTANCE.getConfig().getEnabled() &&
            (StorageCache.INSTANCE.getWasInOverview() ||
             StorageCache.INSTANCE.getCurrentPage() != null)) {
            return;
        }

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        Integer color = ItemRarityBg.INSTANCE.getRarityColor(stack);
        if (color == null) return;

        ItemRarityBg.INSTANCE.drawBackground(context, slot.x, slot.y, color);
    }
}
