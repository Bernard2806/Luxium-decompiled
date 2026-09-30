/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.kawase.KawaseSourceRegistry;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.VanillaLavaLightEngine;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientLevel.class})
public class ClientLevelMixin {
    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void luxium$tick(BooleanSupplier runTask, CallbackInfo ci) {
        ClientLevel level = (ClientLevel)this;
        BlockLightTest.tick(level);
        TorchRtxState.get().tick(level);
        VanillaLavaLightEngine.get().tick(level, TorchRtxState.get().isEnabled());
        ExposedFaceService.get().tick(level);
    }

    @Inject(method={"unload"}, at={@At(value="HEAD")})
    private void luxium$unloadLavaLightChunk(LevelChunk chunk, CallbackInfo ci) {
        ClientLevel level = (ClientLevel)this;
        BlockLightTest.removeChunk(chunk.m_7697_().f_45578_, chunk.m_7697_().f_45579_);
        KawaseSourceRegistry.get().removeChunk(chunk.m_7697_().f_45578_, chunk.m_7697_().f_45579_);
        VanillaLavaLightEngine.get().onChunkUnloaded(level, chunk.m_7697_().f_45578_, chunk.m_7697_().f_45579_);
    }
}

