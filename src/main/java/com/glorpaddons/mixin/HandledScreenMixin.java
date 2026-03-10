package com.glorpaddons.mixin;

import com.glorpaddons.storage.StorageCache;
import com.glorpaddons.storage.StorageConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses vanilla rendering for the custom storage overlay:
 *  - Cancels drawSlot so vanilla items never appear on screen.
 *  - Cancels drawForeground so "Inventory" / container title text doesn't appear.
 *  - Overrides isClickOutsideBounds so the screen doesn't close on clicks
 *    inside our overlay area.
 * Note: slot.x / slot.y are ACC_FINAL in Java 21 and cannot be written outside
 * <init>, so we suppress slot rendering at the drawSlot call site instead.
 */
@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    private boolean overlayActive() {
        if (!((Object) this instanceof GenericContainerScreen)) return false;
        if (!StorageConfigManager.INSTANCE.getConfig().getEnabled()) return false;
        return StorageCache.INSTANCE.getWasInOverview()
            || StorageCache.INSTANCE.getCurrentPage() != null;
    }

    /**
     * Cancel vanilla slot rendering (items + slot backgrounds) when our overlay is active.
     * This replaces the old slot-repositioning approach which hit IllegalAccessError on Java 21.
     */
    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!overlayActive()) return;
        ci.cancel();
    }

    /**
     * Cancel vanilla's label rendering ("Inventory", container title).
     */
    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelLabels(
            DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!overlayActive()) return;
        ci.cancel();
    }

    /**
     * Cancel vanilla slot highlight rendering (back layer) when our overlay is active.
     */
    @Inject(method = "drawSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelSlotHighlightBack(DrawContext context, CallbackInfo ci) {
        if (!overlayActive()) return;
        ci.cancel();
    }

    /**
     * Cancel vanilla slot highlight rendering (front layer) when our overlay is active.
     */
    @Inject(method = "drawSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelSlotHighlightFront(DrawContext context, CallbackInfo ci) {
        if (!overlayActive()) return;
        ci.cancel();
    }

    /**
     * Cancel vanilla mouseover tooltip (drawn after super.render() in GenericContainerScreen.render)
     * so it doesn't appear behind or over our overlay.
     */
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$cancelMouseoverTooltip(
            DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!overlayActive()) return;
        ci.cancel();
    }

    /**
     * Prevent the screen from closing when clicking inside our overlay panels.
     */
    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$isClickOutsideBounds(
            double mouseX, double mouseY, int left, int top,
            CallbackInfoReturnable<Boolean> cir) {
        if (!overlayActive()) return;
        cir.setReturnValue(false);
    }
}
