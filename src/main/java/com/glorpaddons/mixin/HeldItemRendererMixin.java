package com.glorpaddons.mixin;

import com.glorpaddons.misc.MiscConfigManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    /**
     * When the held item's components change but the item type stays the same
     * (e.g. Hypixel updating lore mid-game), Minecraft normally plays the hand
     * swap animation. Returning true from shouldSkipHandAnimationOnSwap suppresses
     * that animation.
     */
    @Inject(method = "shouldSkipHandAnimationOnSwap", at = @At("RETURN"), cancellable = true)
    private void glorpaddons$cancelComponentUpdateAnimation(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (!MiscConfigManager.INSTANCE.getConfig().getCancelComponentUpdateAnimations()) return;
        if (from.getItem() == to.getItem()) {
            cir.setReturnValue(true);
        }
    }
}
