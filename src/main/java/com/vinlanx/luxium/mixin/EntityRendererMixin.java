/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.VanillaLavaLightEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EntityRenderer.class})
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void luxium$skipEntityExtrasInCelestialShadow(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        if (NeoSkyCelestia.get().isRenderingEntityShadowPass() || NeoShadowsEngine.isGpuShadowAtlasCapturePass()) {
            ci.cancel();
        }
    }

    @Inject(method={"getPackedLightCoords"}, at={@At(value="RETURN")}, cancellable=true)
    private void luxium$rtxEntityLight(T entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        int best;
        if (NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        TorchRtxState rtx = TorchRtxState.get();
        if (!rtx.isEnabled()) {
            return;
        }
        BlockPos pos = entity.m_20183_();
        int rtxLight = rtx.sampleLightForRender(pos);
        VanillaLavaLightEngine lava = VanillaLavaLightEngine.get();
        if (rtxLight < 0 && !lava.isActive()) {
            return;
        }
        rtxLight = Math.max(0, rtxLight);
        int vanilla = (Integer)cir.getReturnValue();
        int vanillaBlock = vanilla >> 4 & 0xF;
        int vanillaSky = vanilla >> 20 & 0xF;
        boolean hybridLocalLighting = Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) && !Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) && Config.CLIENT.gpuLocalLightingMode.get() == Config.GpuLocalLightingMode.HYBRID;
        int n = best = hybridLocalLighting ? rtxLight : Math.max(vanillaBlock, rtxLight);
        if (hybridLocalLighting && lava.isActive()) {
            best = Math.max(best, lava.sampleLight(pos));
        }
        best = Math.max(best, TestFlashLight.sampleLight(pos));
        cir.setReturnValue((Object)LightTexture.m_109885_((int)Math.max(0, Math.min(15, best)), (int)vanillaSky));
    }
}

