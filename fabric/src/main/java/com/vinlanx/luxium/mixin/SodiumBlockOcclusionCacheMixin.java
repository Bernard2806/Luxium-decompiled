package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = {BlockOcclusionCache.class}, remap = false ) public abstract

 class SodiumBlockOcclusionCacheMixin {
    @Inject(
        method = {"shouldDrawSide"}, at = {@At("RETURN")}, remap = false, require = 1 ) private void luxium$captureSodiumFaces(BlockState state, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {

 if (cir.getReturnValue()) {
            ExposedFaceService.get().recordFace(pos, facing, view, state);
        }
    }
    }
