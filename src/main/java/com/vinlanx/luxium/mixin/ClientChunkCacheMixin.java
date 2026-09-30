/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientChunkCache
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityTagOutput
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.kawase.KawaseSourceRegistry;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.VanillaLavaLightEngine;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientChunkCache.class})
public abstract class ClientChunkCacheMixin {
    @Shadow
    ClientLevel f_104411_;

    @Inject(method={"replaceWithPacketData"}, at={@At(value="RETURN")})
    private void luxium$onChunkLoad(int x, int z, FriendlyByteBuf buf, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk chunk = (LevelChunk)cir.getReturnValue();
        if (chunk == null) {
            return;
        }
        ClientLevel capturedLevel = this.f_104411_;
        BlockLightTest.onChunk(capturedLevel, chunk);
        ExposedFaceService.get().removeChunk(x, z);
        VanillaLavaLightEngine.get().onChunkLoaded(capturedLevel, x, z);
        TorchRtxState.get().addChunkLightSources(capturedLevel, chunk);
        KawaseSourceRegistry.get().addChunk(capturedLevel, chunk);
        NeoSkyCelestia.get().markChunkDirty(x, z);
    }

    @Inject(method={"drop"}, at={@At(value="HEAD")})
    private void luxium$onChunkUnload(int x, int z, CallbackInfo ci) {
        BlockLightTest.removeChunk(x, z);
        TorchRtxState.get().removeChunkLightSources(x, z);
        KawaseSourceRegistry.get().removeChunk(x, z);
        NeoSkyCelestia.get().markChunkDirty(x, z);
        ExposedFaceService.get().removeChunk(x, z);
    }

    @Inject(method={"updateViewRadius"}, at={@At(value="RETURN")})
    private void luxium$reconcileKawaseSources(int radius, CallbackInfo ci) {
        KawaseSourceRegistry.get().reconcileLoadedChunks(this.f_104411_);
    }
}

