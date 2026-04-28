package com.glorpaddons.mixin;

import com.glorpaddons.misc.PeekChat;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    /**
     * Override isChatFocused() so getWidth/getHeight/getVisibleLineCount
     * use focused (full-size) dimensions while peeking.
     */
    @Inject(method = "isChatFocused", at = @At("RETURN"), cancellable = true)
    private void glorpaddons$peekChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && PeekChat.getPeeking()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Override the boolean (focused) parameter of render() so that
     * forEachVisibleLine uses full opacity instead of age-based fade.
     */
    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean glorpaddons$peekChatRenderFocused(boolean focused) {
        if (PeekChat.getPeeking()) return true;
        return focused;
    }
}
