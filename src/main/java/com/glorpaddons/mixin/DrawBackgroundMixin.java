package com.glorpaddons.mixin;

import com.glorpaddons.storage.StorageCache;
import com.glorpaddons.storage.StorageConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels GenericContainerScreen.drawBackground (the chest texture draw)
 * when our storage overlay is active. The existing GenericContainerScreenMixin
 * cancels Screen.renderBackground (the dark overlay), but HandledScreen still
 * calls drawBackground directly after that super call — this mixin plugs that gap.
 */
@Mixin(GenericContainerScreen.class)
public class DrawBackgroundMixin {

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelDrawBackground(
            DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (StorageConfigManager.INSTANCE.getConfig().getEnabled() &&
            (StorageCache.INSTANCE.getWasInOverview() ||
             StorageCache.INSTANCE.getCurrentPage() != null)) {
            ci.cancel();
        }
    }
}
