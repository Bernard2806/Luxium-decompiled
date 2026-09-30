/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.shaders.Uniform
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.tfrpluslsr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import com.vinlanx.luxium.client.posteffects.tonemap;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;

public final class LsrSystem {
    @Nullable
    private static TextureTarget lowResWorldTarget;
    @Nullable
    private static RenderTarget nativeMainTarget;
    private static boolean worldPassActive;

    private LsrSystem() {
    }

    public static boolean isEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.lsrEnabled);
    }

    public static boolean isWorldPassActive() {
        return worldPassActive;
    }

    public static boolean shouldDeferTonemap() {
        return worldPassActive && !Config.isFeatureEnabled(Config.CLIENT.tfrEnabled);
    }

    public static void onConfigChanged() {
        TemporalFrameSystem.onConfigChanged();
        PostEffectPipeline.clearDeferredTonemap();
        if (!LsrSystem.isEnabled() && !worldPassActive && lowResWorldTarget != null) {
            lowResWorldTarget.m_83930_();
            lowResWorldTarget = null;
        }
    }

    public static boolean beginWorldRender(Minecraft mc) {
        worldPassActive = false;
        nativeMainTarget = null;
        PostEffectPipeline.clearDeferredTonemap();
        if (!LsrSystem.isEnabled() || mc.f_91073_ == null) {
            if (!LsrSystem.isEnabled() && lowResWorldTarget != null) {
                lowResWorldTarget.m_83930_();
                lowResWorldTarget = null;
            }
            return false;
        }
        RenderTarget nativeTarget = mc.m_91385_();
        if (nativeTarget == null || nativeTarget.f_83915_ <= 0 || nativeTarget.f_83916_ <= 0) {
            return false;
        }
        int width = LsrSystem.expectedWidth(nativeTarget.f_83915_);
        int height = LsrSystem.expectedHeight(nativeTarget.f_83916_);
        LsrSystem.ensureLowResTarget(width, height);
        TextureTarget lowRes = lowResWorldTarget;
        if (lowRes == null) {
            return false;
        }
        nativeMainTarget = nativeTarget;
        worldPassActive = true;
        MinecraftAccessor accessor = (MinecraftAccessor)mc;
        accessor.luxium$setMainRenderTargetField((RenderTarget)lowRes);
        lowRes.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        lowRes.m_83954_(Minecraft.f_91002_);
        lowRes.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)lowRes.f_83917_, (int)lowRes.f_83918_);
        return true;
    }

    public static void rebindWorldTarget() {
        if (!worldPassActive || lowResWorldTarget == null) {
            return;
        }
        lowResWorldTarget.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)LsrSystem.lowResWorldTarget.f_83917_, (int)LsrSystem.lowResWorldTarget.f_83918_);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void finishRealFrame(Minecraft mc) {
        if (!worldPassActive) {
            return;
        }
        TextureTarget source = lowResWorldTarget;
        RenderTarget destination = nativeMainTarget;
        if (source == null || destination == null) {
            LsrSystem.restoreNativeTarget(mc);
            return;
        }
        MinecraftAccessor accessor = (MinecraftAccessor)mc;
        accessor.luxium$setMainRenderTargetField(destination);
        destination.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        int previousFilter = source.f_83922_;
        if (previousFilter != 9729) {
            source.m_83936_(9729);
        }
        try {
            ShaderInstance shader;
            boolean rendered = false;
            if (PostEffectPipeline.hasDeferredTonemap() && (shader = tonemap.getShader()) != null) {
                int effectsTexture = PostEffectPipeline.getDeferredEffectsTextureId();
                tonemap.configure(shader, source.m_83975_(), effectsTexture, PostEffectPipeline.deferredTonemapCompositesEffects(), true, source.f_83915_, source.f_83916_);
                LsrSystem.drawToNative(shader, destination);
                rendered = true;
            }
            if (!rendered) {
                rendered = LsrSystem.renderSpatialUpscale((RenderTarget)source, destination);
            }
            if (!rendered) {
                LsrSystem.blitFallback((RenderTarget)source, destination);
            }
        }
        finally {
            if (source.f_83922_ != previousFilter) {
                source.m_83936_(previousFilter);
            }
            PostEffectPipeline.clearDeferredTonemap();
            worldPassActive = false;
            nativeMainTarget = null;
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        }
    }

    public static void restoreNativeTarget(Minecraft mc) {
        RenderTarget destination = nativeMainTarget;
        if (destination != null) {
            ((MinecraftAccessor)mc).luxium$setMainRenderTargetField(destination);
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        }
        PostEffectPipeline.clearDeferredTonemap();
        worldPassActive = false;
        nativeMainTarget = null;
    }

    public static int expectedWidth(int nativeWidth) {
        return Math.max(1, Math.round((float)nativeWidth * LsrSystem.renderScale()));
    }

    public static int expectedHeight(int nativeHeight) {
        return Math.max(1, Math.round((float)nativeHeight * LsrSystem.renderScale()));
    }

    public static float sharpness() {
        return Mth.m_14036_((float)((Double)Config.CLIENT.lsrSharpness.get()).floatValue(), (float)0.0f, (float)1.0f);
    }

    private static float renderScale() {
        return Mth.m_14036_((float)((Double)Config.CLIENT.lsrRenderScale.get()).floatValue(), (float)0.5f, (float)0.85f);
    }

    private static boolean renderSpatialUpscale(RenderTarget source, RenderTarget destination) {
        ShaderInstance shader = ShaderManager.getLsrUpscaleShader();
        if (shader == null) {
            return false;
        }
        shader.m_173350_("SceneSampler", (Object)source.m_83975_());
        LsrSystem.setUniform2f(shader, "SourceTexelSize", 1.0f / (float)Math.max(1, source.f_83915_), 1.0f / (float)Math.max(1, source.f_83916_));
        LsrSystem.setUniform1f(shader, "LsrSharpness", LsrSystem.sharpness());
        LsrSystem.drawToNative(shader, destination);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void drawToNative(ShaderInstance shader, RenderTarget destination) {
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedTexture0 = RenderSystem.getShaderTexture((int)0);
        int savedTexture1 = RenderSystem.getShaderTexture((int)1);
        int savedTexture2 = RenderSystem.getShaderTexture((int)2);
        float[] savedColor = RenderSystem.getShaderColor();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)destination.f_83917_, (float)destination.f_83918_, 0.0f, 1000.0f, 3000.0f);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(ortho);
            }
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            GlStateManager._blendFuncSeparate((int)1, (int)0, (int)1, (int)0);
            shader.m_173363_();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_(0.0, (double)destination.f_83918_, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)destination.f_83917_, (double)destination.f_83918_, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)destination.f_83917_, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.depthFunc((int)515);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor((float)savedColor[0], (float)savedColor[1], (float)savedColor[2], (float)savedColor[3]);
            RenderSystem.setShaderTexture((int)0, (int)savedTexture0);
            RenderSystem.setShaderTexture((int)1, (int)savedTexture1);
            RenderSystem.setShaderTexture((int)2, (int)savedTexture2);
            RenderSystem.activeTexture((int)33984);
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        }
    }

    private static void blitFallback(RenderTarget source, RenderTarget destination) {
        GL30.glBindFramebuffer((int)36008, (int)source.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)destination.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)source.f_83915_, (int)source.f_83916_, (int)0, (int)0, (int)destination.f_83915_, (int)destination.f_83916_, (int)16384, (int)9729);
        destination.m_83947_(true);
    }

    private static void ensureLowResTarget(int width, int height) {
        if (lowResWorldTarget == null) {
            lowResWorldTarget = new TextureTarget(width, height, true, Minecraft.f_91002_);
            lowResWorldTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            lowResWorldTarget.m_83936_(9729);
            return;
        }
        if (LsrSystem.lowResWorldTarget.f_83915_ != width || LsrSystem.lowResWorldTarget.f_83916_ != height) {
            lowResWorldTarget.m_83941_(width, height, Minecraft.f_91002_);
            lowResWorldTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            lowResWorldTarget.m_83936_(9729);
        }
    }

    private static void setUniform1i(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    private static void setUniform1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void setUniform2f(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }
}

