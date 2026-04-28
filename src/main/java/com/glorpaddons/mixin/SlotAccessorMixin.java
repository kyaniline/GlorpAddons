package com.glorpaddons.mixin;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Slot.x as writable so GlorpInventoryScreen can temporarily move
 * the offhand slot right to make room for the equipment column.
 */
@Mixin(Slot.class)
public interface SlotAccessorMixin {
    @Mutable
    @Accessor("x")
    void setX(int x);
}
