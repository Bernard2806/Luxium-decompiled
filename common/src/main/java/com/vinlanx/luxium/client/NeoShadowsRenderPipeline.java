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
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.mixin.LightTextureAccessor;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import com.vinlanx.luxium.platform.RenderFrame;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc; final class NeoShadowsRenderPipeline { private final NeoShadowsEngine engine; NeoShadowsRenderPipeline(NeoShadowsEngine engine) {

        this.engine = engine;
    }

    void captureSkyLightSceneInternal(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, long finishNano, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance






 ();
        boolean neoSelected = this.engine.isSelected();
        boolean cpuSelected = NeoCpuShadowsEngine.isActive();
        this.engine.setEnabled(neoSelected);
        if (cpuSelected) {
            NeoCpuShadowsEngine.prepareForShadowlessCapture(mc, camera);
        }
        if (!this.engine.enabled && !cpuSelected || mc.level == null || mc.player == null || ReflectionSystem.isRenderingWorldPass
                () || this.engine.skyLightCapturePass) {
            this.engine.skySceneReady = false;
            return;
        }
        if (this.engine.enabled && !this.engine.framePrepared) {
            this.engine.runtime.prepareFrameInternal(mc, camera, partialTick);
        }
        boolean hasShadowPolygons = this.engine.enabled && this.engine.hasAnyShadowPolygons() || NeoCpuShadowsEngine.hasActivePolygons
                ();
        if (!hasShadowPolygons) {
            this.engine.skySceneReady = false;
            return;
        }

        RenderTarget originalTarget = mc.getMainRenderTarget();
        this.engine.ensureTargets(originalTarget.viewWidth, originalTarget.viewHeight);
        long nowMs = System.currentTimeMillis();
        if (!this.shouldRefreshSkyCapture(camera, projectionMatrix, this.engine.skyCaptureTarget.width, this.engine.skyCaptureTarget.height, nowMs)) {
            return;
        }

        NeoShadowsEngine.flushSharedRenderBuffers(mc);

        PoseStack skyPose = new PoseStack();
        skyPose.last().pose().set((Matrix4fc)poseStack.last().pose());
        skyPose.last().normal().set((Matrix3fc)poseStack.last().normal());

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

        this.engine.skyCaptureTarget.clear(Minecraft.ON_OSX);
        this.engine.skyCaptureTarget.bindWrite(true);
        accessor.luxium$setMainRenderTargetField((RenderTarget)this.engine.skyCaptureTarget);
        this.engine.skyLightCapturePass = true;
        this.engine.skySceneReady = false;

        try {
            LightTextureAccessor lightTextureAccessor = (LightTextureAccessor)lightTexture;
            lightTextureAccessor.luxium$setUpdateLightTexture(true);
            lightTexture.updateLightTexture(partialTick);

            levelRenderer.renderLevel(skyPose, partialTick, finishNano, false, camera, gameRenderer, lightTexture, projectionMatrix);









            NeoShadowsEngine.flushSharedRenderBuffers(mc);
            this.rememberSkyCapture(camera, projectionMatrix, this.engine.skyCaptureTarget.width, this.engine.skyCaptureTarget.height, nowMs); } finally {

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
            LightTextureAccessor lightTextureAccessor = (LightTextureAccessor)lightTexture;
            lightTextureAccessor.luxium$setUpdateLightTexture(true);
            lightTexture.updateLightTexture(partialTick);
            originalTarget.bindWrite(true);
            PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);
        }
    }

    void renderAfterParticles(Minecraft mc, RenderFrame event) {
        this.renderShadowMask(mc, event.getPoseStack(), event.getCamera(), event.getFrustum());
        this.composeShadowMask(mc);
    }

    void renderShadowMask(Minecraft mc, PoseStack poseStack, Camera camera, @Nullable Frustum frustum) {
        if (!this.engine.framePrepared) {
            this.engine.runtime.prepareFrameInternal(mc, camera, LuxiumPlatform.get().partialTick(mc));
        }

        if (!this.engine.hasAnyShadowPolygons()) {
            this.engine.maskReady = false;
            this.engine.framePrepared = false;
        return; }
            Vec3

            cameraPos = camera.getPosition();
            double cameraX = cameraPos.x;
            double cameraY = cameraPos.y;
            double cameraZ = cameraPos.z;
            RenderTarget main = mc.getMainRenderTarget();
            this.engine.ensureTargets(main.viewWidth, main.viewHeight);
            this.engine.shadowMaskTarget.clear(Minecraft.ON_OSX);
            this.engine.shadowMaskTarget.copyDepthFrom(main);
            this.engine.shadowMaskTarget.bindWrite(false);
            BufferSource
            bufferSource = mc.renderBuffers().bufferSource();
            VertexConsumer consumer = bufferSource.getBuffer(NeoShadowsEngine.MASK_PASS);
            Matrix4f pose = poseStack.last().pose();
            NeoShadowsTypes.StaticHotPatchOverlay hotPatch = this.engine.activeStaticHotPatch;
            boolean hasHotPatch = !hotPatch.isEmpty();

            if (this.engine.activeStaticOutput != null) {
                for (NeoShadowsTypes.ShadowPolygon polygon : this.engine.activeStaticOutput.shadowPolygons) {
                    if (!(!hasHotPatch || !hotPatch.replaces(polygon))) {
continue; }

if (!this.engine.shouldCullShadowPolygon(polygon, cameraX, cameraY, cameraZ, frustum)) {
                        this.engine.emitPolygon(pose, consumer, cameraPos, polygon); }

                }
            }

            List<NeoShadowsTypes.ShadowPolygon> staging = this.engine.inFlightStaticPolygons;
            if (this.engine.activeStaticOutput == null && !staging.isEmpty() && this.engine.inFlightStaticEpoch == this.engine.buildEpoch) {
                for (NeoShadowsTypes.ShadowPolygon polygonx : staging) {
                    if (!this.engine.shouldCullShadowPolygon(polygonx, cameraX, cameraY, cameraZ, frustum)) {
                        this.engine.emitPolygon(pose, consumer, cameraPos, polygonx);
                    }
                }
            }

            if (hasHotPatch) {
                for (NeoShadowsTypes.ShadowPolygon polygonxx : hotPatch.polygons) {
                    if (!this.engine.shouldCullShadowPolygon(polygonxx, cameraX, cameraY, cameraZ, frustum)) {
                        this.engine.emitPolygon(pose, consumer, cameraPos, polygonxx);
                    }
                }
            }

            for (NeoShadowsTypes.ShadowPolygon polygonxxx : this.engine.activeDynamicPolygons) {
                if (!this.engine.shouldCullShadowPolygon(polygonxxx, cameraX, cameraY, cameraZ, frustum)) {
                    this.engine.emitPolygon(pose, consumer, cameraPos, polygonxxx);
                }
            }

            bufferSource.endBatch(NeoShadowsEngine.MASK_PASS);
            main.bindWrite(false);
            this.engine.maskReady = true;
            this.engine.framePrepared = false;
    }

    void composeShadowMask(Minecraft mc) {
        if (!this.engine.maskReady || this.engine.shadowMaskTarget == null || !this.engine.skySceneReady || this.engine.skyCaptureTarget == null) {
            return;
        }

        ShaderInstance shader = ShaderManager.getFloodShadowCompositeShader();
        int sceneDepthTexture = SharedPostResources.getDepthTextureId();
        if (shader == null || sceneDepthTexture < 0) {
            return;
        }

        RenderTarget main = mc.getMainRenderTarget();
        this.engine.ensureTargets(main.viewWidth, main.viewHeight);

        this.engine.sceneCopyTarget.clear(Minecraft.ON_OSX);
        GL30.glBindFramebuffer((int)36008, (int)main.frameBufferId);
        GL30.glBindFramebuffer((int)36009, (int)this.engine.sceneCopyTarget.frameBufferId);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.viewWidth, (int)main.viewHeight, (int)0, (int)0, (int)main.viewWidth, (int)main.viewHeight, (int)16384, (int)9728);





        GL30.glBindFramebuffer((int)36160, (int)0);
        main.bindWrite(false);

        int width = main.viewWidth;
        int height = main.viewHeight;
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        int savedShaderTex3 = RenderSystem.getShaderTexture((int)3);
        int savedShaderTex4 = RenderSystem.getShaderTexture((int)4);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);

        try {
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.ORTHOGRAPHIC_Z);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();

            shader.setSampler("SceneSampler", this.engine.sceneCopyTarget.getColorTextureId());
            shader.setSampler("ShadowlessSceneSampler", this.engine.skyCaptureTarget.getColorTextureId());
            shader.setSampler("ShadowlessDepthSampler", this.engine.skyCaptureTarget.getDepthTextureId());
            shader.setSampler("ShadowMaskSampler", this.engine.shadowMaskTarget.getColorTextureId());
            shader.setSampler("DepthSampler", sceneDepthTexture);

            shader.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            shader.PROJECTION_MATRIX.set(ortho);
            shader.apply();

            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(0.0, (double)height, 0.0).uv(0.0f, 0.0f).endVertex();
            buffer.vertex((double)width, (double)height, 0.0).uv(1.0f, 0.0f).endVertex();
            buffer.vertex((double)width, 0.0, 0.0).uv(1.0f, 1.0f).endVertex();
            buffer.vertex(0.0, 0.0, 0.0).uv(0.0f, 1.0f).endVertex();
            BufferUploader.draw((BufferBuilder.RenderedBuffer)buffer.end()); } finally {

            shader.clear();
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
            main.bindWrite(true);
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
        Vec3 cameraPos = camera.getPosition();
        double dx = cameraPos.x - this.engine.lastSkyCaptureCamX;
        double dy = cameraPos.y - this.engine.lastSkyCaptureCamY;
        double dz = cameraPos.z - this.engine.lastSkyCaptureCamZ;
        if (dx * dx + dy * dy + dz * dz > 1.0E-6) {
            return true;
        }
        if (Math.abs(camera.getYRot() - this.engine.lastSkyCaptureYaw) > 0.05f || Math.abs
                (camera.getXRot() - this.engine.lastSkyCapturePitch) > 0.05f) {
            return true;
        }
        return !projectionMatrix.equals(this.engine.lastSkyCaptureProjection);
    }

    void rememberSkyCapture(Camera camera, Matrix4f projectionMatrix, int captureWidth, int captureHeight, long nowMs) {
        Vec3



        cameraPos = camera.getPosition();
        this.engine.skyCaptureDirty = false;
        this.engine.skySceneReady = true;
        this.engine.lastSkyCaptureRenderMs = nowMs;
        this.engine.lastSkyCaptureWidth = captureWidth;
        this.engine.lastSkyCaptureHeight = captureHeight;
        this.engine.lastSkyCaptureCamX = cameraPos.x;
        this.engine.lastSkyCaptureCamY = cameraPos.y;
        this.engine.lastSkyCaptureCamZ = cameraPos.z;
        this.engine.lastSkyCaptureYaw = camera.getYRot();
        this.engine.lastSkyCapturePitch = camera.getXRot();
        this.engine.lastSkyCaptureProjection.set(projectionMatrix);
        this.engine.lastSkyCaptureProjectionValid = true;
    }

    static boolean isCompatibleBuildCenter(BlockPos buildCenter, BlockPos currentCenter) {
        double dx = (double)(buildCenter.getX() - currentCenter.getX());
        double dy = (double)(buildCenter.getY() - currentCenter.getY());
        double dz = (double)(buildCenter.getZ() - currentCenter.getZ());
        return dx * dx + dy * dy + dz * dz <= 256.0;
    }
    }
