package com.glorpaddons.mixin;

import com.glorpaddons.farming.FarmingConfigManager;
import com.glorpaddons.farming.PestHighlighter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    /**
     * Makes pest ArmorStand entities glow client-side.
     * Called by MinecraftClient.hasOutline() before the outline color is set.
     */
    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void glorpaddons$pestGlow(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && FarmingConfigManager.INSTANCE.getConfig().getPestHighlightEnabled()
                && PestHighlighter.isPest((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Sets the outline color to dark red for pest entities.
     * Called by EntityRenderer.updateRenderState() when hasOutline() is true.
     */
    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    private void glorpaddons$pestGlowColor(CallbackInfoReturnable<Integer> cir) {
        if (FarmingConfigManager.INSTANCE.getConfig().getPestHighlightEnabled()
                && PestHighlighter.isPest((Entity) (Object) this)) {
            cir.setReturnValue(PestHighlighter.GLOW_COLOR);
        }
    }
}
