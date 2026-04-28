package com.glorpaddons.mixin;

import com.glorpaddons.misc.PeekChat;
import com.glorpaddons.storage.StorageOverlay;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow private double x;
    @Shadow private double y;
    @Shadow @Final private MinecraftClient client;
    private static final double SCROLL_SPEED_MULTIPLIER = 3.0;

    private double glorpaddons$savedX;
    private double glorpaddons$savedY;

    /**
     * Intercept scroll events to redirect to chat when peek chat is active.
     */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$peekChatScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == this.client.getWindow().getHandle() && PeekChat.getPeeking()) {
            boolean discrete = this.client.options.getDiscreteMouseScroll().getValue();
            double sensitivity = this.client.options.getMouseWheelSensitivity().getValue();
            int amount = (int) ((discrete ? Math.signum(vertical) : vertical) * sensitivity * SCROLL_SPEED_MULTIPLIER);
            if (amount != 0) {
                this.client.inGameHud.getChatHud().scroll(amount);
            }
            ci.cancel();
        }
    }

    /**
     * Save the current GLFW cursor position before lockCursor can move it.
     */
    @Inject(method = "lockCursor", at = @At("HEAD"))
    private void glorpaddons$saveCursorOnLock(CallbackInfo ci) {
        if (StorageOverlay.INSTANCE.suppressCursorReset) {
            long handle = this.client.getWindow().getHandle();
            double[] xBuf = new double[1];
            double[] yBuf = new double[1];
            GLFW.glfwGetCursorPos(handle, xBuf, yBuf);
            glorpaddons$savedX = xBuf[0];
            glorpaddons$savedY = yBuf[0];
        }
    }

    /**
     * Redirect the setCursorParameters call inside lockCursor to use saved
     * position instead of screen center when suppressing cursor reset.
     */
    @Redirect(method = "lockCursor",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(Lnet/minecraft/client/util/Window;IDD)V"))
    private void glorpaddons$redirectLockCursor(Window window, int code, double centerX, double centerY) {
        if (StorageOverlay.INSTANCE.suppressCursorReset) {
            InputUtil.setCursorParameters(window, code, glorpaddons$savedX, glorpaddons$savedY);
            this.x = glorpaddons$savedX;
            this.y = glorpaddons$savedY;
        } else {
            InputUtil.setCursorParameters(window, code, centerX, centerY);
        }
    }

    /**
     * Redirect the setCursorParameters call inside unlockCursor to use saved
     * position instead of screen center when suppressing cursor reset.
     */
    @Redirect(method = "unlockCursor",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(Lnet/minecraft/client/util/Window;IDD)V"))
    private void glorpaddons$redirectUnlockCursor(Window window, int code, double centerX, double centerY) {
        if (StorageOverlay.INSTANCE.suppressCursorReset) {
            InputUtil.setCursorParameters(window, code, glorpaddons$savedX, glorpaddons$savedY);
            this.x = glorpaddons$savedX;
            this.y = glorpaddons$savedY;
            StorageOverlay.INSTANCE.suppressCursorReset = false;
        } else {
            InputUtil.setCursorParameters(window, code, centerX, centerY);
        }
    }
}
