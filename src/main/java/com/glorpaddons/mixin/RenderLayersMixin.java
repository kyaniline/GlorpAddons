package com.glorpaddons.mixin;

import com.glorpaddons.misc.MiscConfigManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {

    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private static void glorpaddons$coalToGlassLayer(BlockState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        if (MiscConfigManager.INSTANCE.getConfig().getFuckCoalEnabled()
                && state.isOf(Blocks.COAL_BLOCK)) {
            cir.setReturnValue(BlockRenderLayer.CUTOUT);
        }
    }
}
