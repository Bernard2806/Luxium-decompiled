/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.water.WaterSurfaceState;
import com.vinlanx.luxium.client.water.WaterTerrainPass;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaCutoutPrepass;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ShaderChunkRenderer.class}, remap=false)
public abstract class EmbeddiumNeoGpuCutoutPrepassMixin {
    @Inject(method={"begin"}, at={@At(value="INVOKE", target="Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;startDrawing()V", shift=At.Shift.AFTER)}, require=0)
    private void luxium$configureNeoGpuCutoutDepthState(TerrainRenderPass pass, CallbackInfo ci) {
        if (WaterTerrainPass.is(pass) && WaterSurfaceState.isScaledPassActive()) {
            SharedPostResources.bindWaterRenderTarget();
            return;
        }
        if (pass != DefaultTerrainRenderPasses.CUTOUT) {
            return;
        }
        if (NeoGpuVanillaCutoutPrepass.isDepthPrepass()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc((int)515);
            RenderSystem.depthMask((boolean)true);
            RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            RenderSystem.disableBlend();
        } else if (NeoGpuVanillaCutoutPrepass.isColorPass()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc((int)514);
            RenderSystem.depthMask((boolean)false);
            RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        }
    }

    @Inject(method={"end"}, at={@At(value="RETURN")}, require=0)
    private void luxium$restoreNeoGpuCutoutDepthState(TerrainRenderPass pass, CallbackInfo ci) {
        if (pass != DefaultTerrainRenderPasses.CUTOUT) {
            return;
        }
        if (!NeoGpuVanillaCutoutPrepass.isDepthPrepass() && !NeoGpuVanillaCutoutPrepass.isColorPass()) {
            return;
        }
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.depthFunc((int)515);
    }
}

