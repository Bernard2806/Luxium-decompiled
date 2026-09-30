/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.shaders.Uniform
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package com.vinlanx.luxium.Testing;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class SphereWarpEffect {
    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 40;
    private static final int MAX_DURATION_SECONDS = 600;
    private static final float MIN_RADIUS = 0.1f;
    private final Supplier<ShaderInstance> shaderSupplier;
    private final boolean depthTestEnabled;
    private final List<Instance> instances = new ArrayList<Instance>();
    private final Matrix4f viewProj = new Matrix4f();
    private final Matrix4f inverseViewProj = new Matrix4f();
    @Nullable
    private RenderTarget distortionTarget;
    private int lastWidth = -1;
    private int lastHeight = -1;

    SphereWarpEffect(Supplier<ShaderInstance> shaderSupplier, boolean depthTestEnabled) {
        this.shaderSupplier = shaderSupplier;
        this.depthTestEnabled = depthTestEnabled;
    }

    void spawnAtPlayer(int radiusBlocks) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            return;
        }
        this.start(mc.f_91074_.m_20182_(), 600, radiusBlocks);
    }

    void start(Vec3 center, int durationSeconds, float radiusBlocks) {
        int clampedDuration = Mth.m_14045_((int)durationSeconds, (int)1, (int)600);
        float radius = Math.max(0.1f, radiusBlocks);
        this.instances.add(Instance.staticSphere(center, radius, clampedDuration * 20));
    }

    void startExpandingWave(Vec3 center, float startRadiusBlocks, float endRadiusBlocks, int totalTicks, float fadeStartProgress) {
        float startRadius = Math.max(0.1f, startRadiusBlocks);
        float endRadius = Math.max(startRadius, endRadiusBlocks);
        int clampedTicks = Math.max(1, totalTicks);
        float clampedFadeStart = Mth.m_14036_((float)fadeStartProgress, (float)0.0f, (float)0.98f);
        this.instances.add(Instance.expandingWave(center, startRadius, endRadius, clampedTicks, clampedFadeStart));
    }

    void onClientTick() {
        if (this.instances.isEmpty()) {
            return;
        }
        Iterator<Instance> iterator = this.instances.iterator();
        while (iterator.hasNext()) {
            Instance instance = iterator.next();
            ++instance.ageTicks;
            if (instance.ageTicks < instance.totalTicks) continue;
            iterator.remove();
        }
    }

    void clear() {
        this.instances.clear();
    }

    void onRenderLevel(Matrix4f viewMatrix) {
        if (this.instances.isEmpty()) {
            return;
        }
        ShaderInstance shader = this.shaderSupplier.get();
        if (shader == null) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        RenderTarget mainTarget = mc.m_91385_();
        if (mainTarget == null) {
            return;
        }
        this.ensureTarget(mainTarget.f_83915_, mainTarget.f_83916_);
        if (this.distortionTarget == null) {
            return;
        }
        this.viewProj.set((Matrix4fc)RenderSystem.getProjectionMatrix());
        this.viewProj.mul((Matrix4fc)viewMatrix);
        this.inverseViewProj.set((Matrix4fc)this.viewProj).invert();
        if (this.depthTestEnabled) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int width = mainTarget.f_83915_;
        int height = mainTarget.f_83916_;
        int depthTextureId = mainTarget.m_83980_();
        Vec3 cameraPos = mc.f_91063_.m_109153_().m_90583_();
        float partialTick = mc.m_91296_();
        for (Instance instance : this.instances) {
            float intensity = instance.getIntensity(partialTick);
            if (intensity <= 0.001f) continue;
            this.distortionTarget.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)this.distortionTarget.f_83915_, (int)this.distortionTarget.f_83916_);
            this.renderFullscreen(shader, mainTarget.m_83975_(), depthTextureId, width, height, instance, intensity, cameraPos, partialTick);
            mainTarget.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)width, (int)height);
            this.renderFullscreen(shader, this.distortionTarget.m_83975_(), depthTextureId, width, height, instance, 0.0f, cameraPos, partialTick);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableDepthTest();
    }

    private void ensureTarget(int width, int height) {
        if (this.distortionTarget == null || width != this.lastWidth || height != this.lastHeight) {
            if (this.distortionTarget != null) {
                this.distortionTarget.m_83930_();
            }
            this.distortionTarget = new TextureTarget(width, height, false, Minecraft.f_91002_);
            this.lastWidth = width;
            this.lastHeight = height;
        }
    }

    private void renderFullscreen(ShaderInstance shader, int textureId, int depthTextureId, int width, int height, Instance instance, float intensity, Vec3 cameraPos, float partialTick) {
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture((int)0, (int)textureId);
        shader.m_173350_("DiffuseSampler", (Object)textureId);
        shader.m_173350_("DepthSampler", (Object)depthTextureId);
        float time = ((float)instance.ageTicks + partialTick) / 20.0f;
        SphereWarpEffect.setUniform1f(shader, "Time", time);
        SphereWarpEffect.setUniform1f(shader, "Intensity", Mth.m_14036_((float)intensity, (float)0.0f, (float)1.0f));
        SphereWarpEffect.setUniform3f(shader, "SphereCenter", (float)instance.center.f_82479_, (float)instance.center.f_82480_, (float)instance.center.f_82481_);
        SphereWarpEffect.setUniform1f(shader, "SphereRadius", instance.getRadius(partialTick));
        SphereWarpEffect.setUniformMatrix(shader, "ViewProj", this.viewProj);
        SphereWarpEffect.setUniformMatrix(shader, "InverseViewProj", this.inverseViewProj);
        SphereWarpEffect.setUniform3f(shader, "CameraPos", (float)cameraPos.f_82479_, (float)cameraPos.f_82480_, (float)cameraPos.f_82481_);
        SphereWarpEffect.setUniform2f(shader, "InSize", width, height);
        SphereWarpEffect.setUniform2f(shader, "OutSize", width, height);
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        builder.m_5483_(-1.0, -1.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
        builder.m_5483_(1.0, -1.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
        builder.m_5483_(1.0, 1.0, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
        builder.m_5483_(-1.0, 1.0, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
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

    private static void setUniform3f(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private static void setUniformMatrix(ShaderInstance shader, String name, Matrix4f matrix) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5679_(matrix);
        }
    }

    private static final class Instance {
        private final Vec3 center;
        private final float startRadius;
        private final float endRadius;
        private final int totalTicks;
        private final float fadeStartProgress;
        private final boolean expandingWave;
        private int ageTicks;

        private Instance(Vec3 center, float startRadius, float endRadius, int totalTicks, float fadeStartProgress, boolean expandingWave) {
            this.center = center;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.totalTicks = totalTicks;
            this.fadeStartProgress = fadeStartProgress;
            this.expandingWave = expandingWave;
        }

        private static Instance staticSphere(Vec3 center, float radius, int totalTicks) {
            return new Instance(center, radius, radius, totalTicks, 1.0f, false);
        }

        private static Instance expandingWave(Vec3 center, float startRadius, float endRadius, int totalTicks, float fadeStartProgress) {
            return new Instance(center, startRadius, endRadius, totalTicks, fadeStartProgress, true);
        }

        private float getRadius(float partialTick) {
            if (!this.expandingWave) {
                return this.endRadius;
            }
            return Mth.m_14179_((float)this.getSmoothProgress(partialTick), (float)this.startRadius, (float)this.endRadius);
        }

        private float getIntensity(float partialTick) {
            if (!this.expandingWave) {
                float fadeIn = Mth.m_14036_((float)(((float)this.ageTicks + partialTick) / 40.0f), (float)0.0f, (float)1.0f);
                float fadeOut = Mth.m_14036_((float)(((float)(this.totalTicks - this.ageTicks) - partialTick) / 40.0f), (float)0.0f, (float)1.0f);
                return Math.min(fadeIn, fadeOut);
            }
            float progress = this.getLinearProgress(partialTick);
            float fadeIn = this.smoothStep(0.0f, 0.03f, progress);
            float fadeOut = 1.0f - Mth.m_14036_((float)((progress - this.fadeStartProgress) / (1.0f - this.fadeStartProgress)), (float)0.0f, (float)1.0f);
            return Mth.m_14036_((float)(fadeIn * fadeOut), (float)0.0f, (float)1.0f);
        }

        private float getLinearProgress(float partialTick) {
            return Mth.m_14036_((float)(((float)this.ageTicks + partialTick) / (float)this.totalTicks), (float)0.0f, (float)1.0f);
        }

        private float getSmoothProgress(float partialTick) {
            float progress = this.getLinearProgress(partialTick);
            return progress * progress * (3.0f - 2.0f * progress);
        }

        private float smoothStep(float start, float end, float value) {
            if (end <= start) {
                return value >= end ? 1.0f : 0.0f;
            }
            float normalized = Mth.m_14036_((float)((value - start) / (end - start)), (float)0.0f, (float)1.0f);
            return normalized * normalized * (3.0f - 2.0f * normalized);
        }
    }
}

