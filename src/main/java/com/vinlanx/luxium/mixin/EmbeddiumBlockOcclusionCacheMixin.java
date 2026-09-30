/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets={"me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache"})
public abstract class EmbeddiumBlockOcclusionCacheMixin {
    @Inject(method={"shouldDrawSide"}, at={@At(value="RETURN")}, cancellable=true, remap=false, require=1)
    private void luxium$captureEmbeddiumFaces(BlockState selfState, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (((Boolean)cir.getReturnValue()).booleanValue()) {
            ExposedFaceService.get().recordFace(pos, facing, view, selfState);
        }
    }
}

