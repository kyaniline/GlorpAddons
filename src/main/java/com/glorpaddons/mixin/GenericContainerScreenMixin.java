package com.glorpaddons.mixin;

import com.glorpaddons.storage.StorageCache;
import com.glorpaddons.storage.StorageConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class GenericContainerScreenMixin {

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelBlur(DrawContext context, CallbackInfo ci) {
        if ((Object) this instanceof GenericContainerScreen &&
            StorageConfigManager.INSTANCE.getConfig().getEnabled() &&
            (StorageCache.INSTANCE.getWasInOverview() || StorageCache.INSTANCE.getCurrentPage() != null)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof GenericContainerScreen &&
            StorageConfigManager.INSTANCE.getConfig().getEnabled() &&
            (StorageCache.INSTANCE.getWasInOverview() || StorageCache.INSTANCE.getCurrentPage() != null)) {
            ci.cancel();
        }
    }
}
