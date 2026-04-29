package com.glorpaddons.mixin;

import com.glorpaddons.misc.MiscConfigManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockCoalCullingMixin {

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void glorpaddons$coalNonOccluding(BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (MiscConfigManager.INSTANCE.getConfig().getFuckCoalEnabled()
                && otherState.isOf(Blocks.COAL_BLOCK)
                && !state.isOf(Blocks.COAL_BLOCK)) {
            cir.setReturnValue(true);
        }
    }
}
