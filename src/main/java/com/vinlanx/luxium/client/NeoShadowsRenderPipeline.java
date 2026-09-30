/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.mixin.LightTextureAccessor;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;

final class NeoShadowsRenderPipeline {
    private final NeoShadowsEngine engine;

    NeoShadowsRenderPipeline(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void captureSkyLightSceneInternal(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, long finishNano, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix) {
        LightTextureAccessor lightTextureAccessor;
        boolean hasShadowPolygons;
        Minecraft mc = Minecraft.m_91087_();
        boolean neoSelected = this.engine.isSelected();
        boolean cpuSelected = NeoCpuShadowsEngine.isActive();
        this.engine.setEnabled(neoSelected);
        if (cpuSelected) {
            NeoCpuShadowsEngine.prepareForShadowlessCapture(mc, camera);
        }
        if (!this.engine.enabled && !cpuSelected || mc.f_91073_ == null || mc.f_91074_ == null || ReflectionSystem.isRenderingWorldPass() || this.engine.skyLightCapturePass) {
            this.engine.skySceneReady = false;
            return;
        }
        if (this.engine.enabled && !this.engine.framePrepared) {
            this.engine.runtime.prepareFrameInternal(mc, camera, partialTick);
        }
        boolean bl = hasShadowPolygons = this.engine.enabled && this.engine.hasAnyShadowPolygons() || NeoCpuShadowsEngine.hasActivePolygons();
        if (!hasShadowPolygons) {
            this.engine.skySceneReady = false;
            return;
        }
        RenderTarget originalTarget = mc.m_91385_();
        this.engine.ensureTargets(originalTarget.f_83917_, originalTarget.f_83918_);
        long nowMs = System.currentTimeMillis();
        if (!this.shouldRefreshSkyCapture(camera, projectionMatrix, this.engine.skyCaptureTarget.f_83915_, this.engine.skyCaptureTarget.f_83916_, nowMs)) {
            return;
        }
        NeoShadowsEngine.flushSharedRenderBuffers(mc);
        PoseStack skyPose = new PoseStack();
        skyPose.m_85850_().m_252922_().set((Matrix4fc)poseStack.m_85850_().m_252922_());
        skyPose.m_85850_().m_252943_().set((Matrix3fc)poseStack.m_85850_().m_252943_());
        MinecraftAccessor accessor = (MinecraftAccessor)mc;
        RenderTarget oldMainTargetField = accessor.luxium$getMainRenderTargetField();
        float savedFogStart = RenderSystem.getShaderFogStart();
        float savedFogEnd = RenderSystem.getShaderFogEnd();
        float[] savedFogColor = RenderSystem.getShaderFogColor();
        float[] savedShaderColor = RenderSystem.getShaderColor();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        int savedShaderTex3 = RenderSystem.getShaderTexture((int)3);
        this.engine.skyCaptureTarget.m_83954_(Minecraft.f_91002_);
        this.engine.skyCaptureTarget.m_83947_(true);
        accessor.luxium$setMainRenderTargetField((RenderTarget)this.engine.skyCaptureTarget);
        this.engine.skyLightCapturePass = true;
        this.engine.skySceneReady = false;
        try {
            lightTextureAccessor = (LightTextureAccessor)lightTexture;
            lightTextureAccessor.luxium$setUpdateLightTexture(true);
            lightTexture.m_109881_(partialTick);
            levelRenderer.m_109599_(skyPose, partialTick, finishNano, false, camera, gameRenderer, lightTexture, projectionMatrix);
            NeoShadowsEngine.flushSharedRenderBuffers(mc);
            this.rememberSkyCapture(camera, projectionMatrix, this.engine.skyCaptureTarget.f_83915_, this.engine.skyCaptureTarget.f_83916_, nowMs);
        }
        finally {
            this.engine.skyLightCapturePass = false;
            accessor.luxium$setMainRenderTargetField(oldMainTargetField);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderFogStart((float)savedFogStart);
            RenderSystem.setShaderFogEnd((float)savedFogEnd);
            RenderSystem.setShaderFogColor((float)savedFogColor[0], (float)savedFogColor[1], (float)savedFogColor[2], (float)savedFogColor[3]);
            RenderSystem.setShaderColor((float)savedShaderColor[0], (float)savedShaderColor[1], (float)savedShaderColor[2], (float)savedShaderColor[3]);
            RenderSystem.setShaderTexture((int)0, (int)savedShaderTex0);
            RenderSystem.setShaderTexture((int)1, (int)savedShaderTex1);
            RenderSystem.setShaderTexture((int)2, (int)savedShaderTex2);
            RenderSystem.setShaderTexture((int)3, (int)savedShaderTex3);
            lightTextureAccessor = (LightTextureAccessor)lightTexture;
            lightTextureAccessor.luxium$setUpdateLightTexture(true);
            lightTexture.m_109881_(partialTick);
            originalTarget.m_83947_(true);
            PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);
        }
    }

    void renderAfterParticles(Minecraft mc, RenderLevelStageEvent event) {
        this.renderShadowMask(mc, event.getPoseStack(), event.getCamera(), event.getFrustum());
        this.composeShadowMask(mc);
    }

    void renderShadowMask(Minecraft mc, PoseStack poseStack, Camera camera, @Nullable Frustum frustum) {
        boolean hasHotPatch;
        if (!this.engine.framePrepared) {
            this.engine.runtime.prepareFrameInternal(mc, camera, mc.getPartialTick());
        }
        if (!this.engine.hasAnyShadowPolygons()) {
            this.engine.maskReady = false;
            this.engine.framePrepared = false;
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        double cameraX = cameraPos.f_82479_;
        double cameraY = cameraPos.f_82480_;
        double cameraZ = cameraPos.f_82481_;
        RenderTarget main = mc.m_91385_();
        this.engine.ensureTargets(main.f_83917_, main.f_83918_);
        this.engine.shadowMaskTarget.m_83954_(Minecraft.f_91002_);
        this.engine.shadowMaskTarget.m_83945_(main);
        this.engine.shadowMaskTarget.m_83947_(false);
        MultiBufferSource.BufferSource bufferSource = mc.m_91269_().m_110104_();
        VertexConsumer consumer = bufferSource.m_6299_(NeoShadowsEngine.MASK_PASS);
        Matrix4f pose = poseStack.m_85850_().m_252922_();
        NeoShadowsTypes.StaticHotPatchOverlay hotPatch = this.engine.activeStaticHotPatch;
        boolean bl = hasHotPatch = !hotPatch.isEmpty();
        if (this.engine.activeStaticOutput != null) {
            for (NeoShadowsTypes.ShadowPolygon polygon : this.engine.activeStaticOutput.shadowPolygons) {
                if (hasHotPatch && hotPatch.replaces(polygon) || this.engine.shouldCullShadowPolygon(polygon, cameraX, cameraY, cameraZ, frustum)) continue;
                this.engine.emitPolygon(pose, consumer, cameraPos, polygon);
            }
        }
        List<NeoShadowsTypes.ShadowPolygon> staging = this.engine.inFlightStaticPolygons;
        if (this.engine.activeStaticOutput == null && !staging.isEmpty() && this.engine.inFlightStaticEpoch == this.engine.buildEpoch) {
            for (NeoShadowsTypes.ShadowPolygon polygon : staging) {
                if (this.engine.shouldCullShadowPolygon(polygon, cameraX, cameraY, cameraZ, frustum)) continue;
                this.engine.emitPolygon(pose, consumer, cameraPos, polygon);
            }
        }
        if (hasHotPatch) {
            for (NeoShadowsTypes.ShadowPolygon polygon : hotPatch.polygons) {
                if (this.engine.shouldCullShadowPolygon(polygon, cameraX, cameraY, cameraZ, frustum)) continue;
                this.engine.emitPolygon(pose, consumer, cameraPos, polygon);
            }
        }
        for (NeoShadowsTypes.ShadowPolygon polygon : this.engine.activeDynamicPolygons) {
            if (this.engine.shouldCullShadowPolygon(polygon, cameraX, cameraY, cameraZ, frustum)) continue;
            this.engine.emitPolygon(pose, consumer, cameraPos, polygon);
        }
        bufferSource.m_109912_(NeoShadowsEngine.MASK_PASS);
        main.m_83947_(false);
        this.engine.maskReady = true;
        this.engine.framePrepared = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void composeShadowMask(Minecraft mc) {
        if (!this.engine.maskReady || this.engine.shadowMaskTarget == null || !this.engine.skySceneReady || this.engine.skyCaptureTarget == null) {
            return;
        }
        ShaderInstance shader = ShaderManager.getFloodShadowCompositeShader();
        int sceneDepthTexture = SharedPostResources.getDepthTextureId();
        if (shader == null || sceneDepthTexture < 0) {
            return;
        }
        RenderTarget main = mc.m_91385_();
        this.engine.ensureTargets(main.f_83917_, main.f_83918_);
        this.engine.sceneCopyTarget.m_83954_(Minecraft.f_91002_);
        GL30.glBindFramebuffer((int)36008, (int)main.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)this.engine.sceneCopyTarget.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_, (int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_, (int)16384, (int)9728);
        GL30.glBindFramebuffer((int)36160, (int)0);
        main.m_83947_(false);
        int width = main.f_83917_;
        int height = main.f_83918_;
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        int savedShaderTex3 = RenderSystem.getShaderTexture((int)3);
        int savedShaderTex4 = RenderSystem.getShaderTexture((int)4);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            shader.m_173350_("SceneSampler", (Object)this.engine.sceneCopyTarget.m_83975_());
            shader.m_173350_("ShadowlessSceneSampler", (Object)this.engine.skyCaptureTarget.m_83975_());
            shader.m_173350_("ShadowlessDepthSampler", (Object)this.engine.skyCaptureTarget.m_83980_());
            shader.m_173350_("ShadowMaskSampler", (Object)this.engine.shadowMaskTarget.m_83975_());
            shader.m_173350_("DepthSampler", (Object)sceneDepthTexture);
            shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            shader.f_173309_.m_5679_(ortho);
            shader.m_173363_();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_(0.0, (double)height, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)width, (double)height, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)width, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.setShaderTexture((int)0, (int)savedShaderTex0);
            RenderSystem.setShaderTexture((int)1, (int)savedShaderTex1);
            RenderSystem.setShaderTexture((int)2, (int)savedShaderTex2);
            RenderSystem.setShaderTexture((int)3, (int)savedShaderTex3);
            RenderSystem.setShaderTexture((int)4, (int)savedShaderTex4);
            RenderSystem.activeTexture((int)33984);
            main.m_83947_(true);
            this.engine.maskReady = false;
        }
    }

    boolean shouldRefreshSkyCapture(Camera camera, Matrix4f projectionMatrix, int captureWidth, int captureHeight, long nowMs) {
        if (!this.engine.skySceneReady || this.engine.skyCaptureDirty || !this.engine.lastSkyCaptureProjectionValid) {
            return true;
        }
        if (captureWidth != this.engine.lastSkyCaptureWidth || captureHeight != this.engine.lastSkyCaptureHeight) {
            return true;
        }
        if (this.hasSkyCaptureViewChanged(camera, projectionMatrix)) {
            return true;
        }
        if (nowMs - this.engine.lastSkyCaptureRenderMs < 100L) {
            return false;
        }
        return nowMs - this.engine.lastSkyCaptureRenderMs > 2000L;
    }

    boolean hasSkyCaptureViewChanged(Camera camera, Matrix4f projectionMatrix) {
        Vec3 cameraPos = camera.m_90583_();
        double dx = cameraPos.f_82479_ - this.engine.lastSkyCaptureCamX;
        double dy = cameraPos.f_82480_ - this.engine.lastSkyCaptureCamY;
        double dz = cameraPos.f_82481_ - this.engine.lastSkyCaptureCamZ;
        if (dx * dx + dy * dy + dz * dz > 1.0E-6) {
            return true;
        }
        if (Math.abs(camera.m_90590_() - this.engine.lastSkyCaptureYaw) > 0.05f || Math.abs(camera.m_90589_() - this.engine.lastSkyCapturePitch) > 0.05f) {
            return true;
        }
        return !projectionMatrix.equals((Object)this.engine.lastSkyCaptureProjection);
    }

    void rememberSkyCapture(Camera camera, Matrix4f projectionMatrix, int captureWidth, int captureHeight, long nowMs) {
        Vec3 cameraPos = camera.m_90583_();
        this.engine.skyCaptureDirty = false;
        this.engine.skySceneReady = true;
        this.engine.lastSkyCaptureRenderMs = nowMs;
        this.engine.lastSkyCaptureWidth = captureWidth;
        this.engine.lastSkyCaptureHeight = captureHeight;
        this.engine.lastSkyCaptureCamX = cameraPos.f_82479_;
        this.engine.lastSkyCaptureCamY = cameraPos.f_82480_;
        this.engine.lastSkyCaptureCamZ = cameraPos.f_82481_;
        this.engine.lastSkyCaptureYaw = camera.m_90590_();
        this.engine.lastSkyCapturePitch = camera.m_90589_();
        this.engine.lastSkyCaptureProjection.set((Matrix4fc)projectionMatrix);
        this.engine.lastSkyCaptureProjectionValid = true;
    }

    static boolean isCompatibleBuildCenter(BlockPos buildCenter, BlockPos currentCenter) {
        double dz;
        double dy;
        double dx = buildCenter.m_123341_() - currentCenter.m_123341_();
        return dx * dx + (dy = (double)(buildCenter.m_123342_() - currentCenter.m_123342_())) * dy + (dz = (double)(buildCenter.m_123343_() - currentCenter.m_123343_())) * dz <= 256.0;
    }
}

