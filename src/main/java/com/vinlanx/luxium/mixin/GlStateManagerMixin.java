/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.opengl.GL11
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GlStateManager.class})
public class GlStateManagerMixin {
    @Inject(method={"_enableCull"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$preventCullInShadowPass(CallbackInfo ci) {
        if (NeoShadowsEngine.isShadowCapturePass() || NeoSkyCelestia.get().isRenderingTerrainShadowPass()) {
            ci.cancel();
        }
    }

    @Inject(method={"_viewport"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$clampViewportToRenderTarget(int x, int y, int w, int h, CallbackInfo ci) {
        Minecraft mc;
        RenderTarget target;
        if ((NeoShadowsEngine.isAnyShadowCapturePass() || ReflectionSystem.isRenderingWorldPass() || TemporalFrameSystem.isWorldPassActive() || LsrSystem.isWorldPassActive()) && (target = (mc = Minecraft.m_91087_()).m_91385_()) != null && (w > target.f_83917_ || h > target.f_83918_)) {
            GL11.glViewport((int)x, (int)y, (int)target.f_83917_, (int)target.f_83918_);
            ci.cancel();
        }
    }
}

