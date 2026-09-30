/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.lighting.BlockLightEngine
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.LuxiumBlockLightEngineExtension;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BlockLightEngine.class})
public abstract class BlockLightEngineMixin
implements LuxiumBlockLightEngineExtension {
    @Unique
    private boolean luxium$lavaOnly;

    @Override
    public void luxium$setLavaOnly(boolean lavaOnly) {
        this.luxium$lavaOnly = lavaOnly;
    }

    @Override
    public boolean luxium$isLavaOnly() {
        return this.luxium$lavaOnly;
    }

    @Inject(method={"getEmission"}, at={@At(value="RETURN")}, cancellable=true)
    private void luxium$filterLavaOnlyEmission(long posKey, BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (this.luxium$lavaOnly && !state.m_60819_().m_205070_(FluidTags.f_13132_)) {
            cir.setReturnValue((Object)0);
        }
    }
}

