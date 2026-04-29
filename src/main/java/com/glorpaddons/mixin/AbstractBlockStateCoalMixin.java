package com.glorpaddons.mixin;

import com.glorpaddons.misc.MiscConfigManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class AbstractBlockStateCoalMixin {

    @Inject(method = "getCullingFace", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$emptyCoalCullingFace(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState self = (BlockState) (Object) this;
        if (MiscConfigManager.INSTANCE.getConfig().getFuckCoalEnabled() && self.isOf(Blocks.COAL_BLOCK)) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }

    @Inject(method = "getCullingShape", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$emptyCoalCullingShape(CallbackInfoReturnable<VoxelShape> cir) {
        BlockState self = (BlockState) (Object) this;
        if (MiscConfigManager.INSTANCE.getConfig().getFuckCoalEnabled() && self.isOf(Blocks.COAL_BLOCK)) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }

    @Inject(method = "isOpaqueFullCube", at = @At("HEAD"), cancellable = true)
    private void glorpaddons$coalNotOpaqueFullCube(CallbackInfoReturnable<Boolean> cir) {
        BlockState self = (BlockState) (Object) this;
        if (MiscConfigManager.INSTANCE.getConfig().getFuckCoalEnabled() && self.isOf(Blocks.COAL_BLOCK)) {
            cir.setReturnValue(false);
        }
    }
}
