/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.client.LightAtlas;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class GodRaySystem {
    private static final boolean ENABLED = false;
    private static final float RENDER_SCALE = 0.8f;
    private static final Vector3f GOD_RAY_TINT = new Vector3f(0.82f, 0.92f, 1.0f);
    private static TextureTarget lowResGodRayTarget;
    private static final Matrix4f INVERSE_PROJECTION;

    private GodRaySystem() {
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void renderPostPass(Minecraft mc, RenderLevelStageEvent event, ShaderInstance shader) {
        int renderHeight;
        RenderTarget outputTarget = mc.m_91385_();
        int outputWidth = outputTarget.f_83917_;
        int outputHeight = outputTarget.f_83918_;
        float renderScale = 0.8f;
        boolean useLowResTarget = renderScale < 0.999f;
        int renderWidth = useLowResTarget ? Math.max(1, Math.round((float)outputWidth * renderScale)) : outputWidth;
        int n = renderHeight = useLowResTarget ? Math.max(1, Math.round((float)outputHeight * renderScale)) : outputHeight;
        if (useLowResTarget) {
            GodRaySystem.ensureLowResTarget(renderWidth, renderHeight);
        }
        Vec3 camPos = event.getCamera().m_90583_();
        Matrix4f inverseProj = NeoSkyFrameCache.copyInverseProjection(event.getProjectionMatrix(), INVERSE_PROJECTION);
        Vector3f look = event.getCamera().m_253058_();
        Vector3f up = event.getCamera().m_253028_();
        Vector3f left = event.getCamera().m_252775_();
        Vector3f right = new Vector3f((Vector3fc)left).mul(-1.0f);
        Vector3f vMin = LightAtlas.getMin();
        Vector3f vSize = LightAtlas.getSize();
        int depthTextureId = SharedPostResources.getDepthTextureId();
        int lightVolumeTextureId = LightAtlas.getTextureId();
        if (depthTextureId <= 0 || lightVolumeTextureId <= 0) {
            return;
        }
        shader.m_173350_("DepthSampler", (Object)depthTextureId);
        shader.m_173350_("LightVolumeSampler", (Object)lightVolumeTextureId);
        if (shader.m_173348_("InverseProj") != null) {
            shader.m_173348_("InverseProj").m_5679_(inverseProj);
        }
        if (shader.m_173348_("CamForward") != null) {
            shader.m_173348_("CamForward").m_5889_(look.x(), look.y(), look.z());
        }
        if (shader.m_173348_("CamUp") != null) {
            shader.m_173348_("CamUp").m_5889_(up.x(), up.y(), up.z());
        }
        if (shader.m_173348_("CamRight") != null) {
            shader.m_173348_("CamRight").m_5889_(right.x(), right.y(), right.z());
        }
        if (shader.m_173348_("CameraPos") != null) {
            shader.m_173348_("CameraPos").m_5889_((float)camPos.f_82479_, (float)camPos.f_82480_, (float)camPos.f_82481_);
        }
        if (shader.m_173348_("VolumeMin") != null) {
            shader.m_173348_("VolumeMin").m_5889_(vMin.x, vMin.y, vMin.z);
        }
        if (shader.m_173348_("VolumeSize") != null) {
            shader.m_173348_("VolumeSize").m_5889_(vSize.x, vSize.y, vSize.z);
        }
        if (shader.m_173348_("VolumeDim") != null) {
            shader.m_173348_("VolumeDim").m_5889_(48.0f, 32.0f, 48.0f);
        }
        if (shader.m_173348_("RayColor") != null) {
            shader.m_173348_("RayColor").m_142276_(GOD_RAY_TINT);
        }
        if (shader.m_173348_("GameTime") != null) {
            shader.m_173348_("GameTime").m_5985_((float)(mc.f_91073_.m_46467_() % 100000L));
        }
        if (shader.m_173348_("EffectStrength") != null) {
            shader.m_173348_("EffectStrength").m_5985_(1.0f);
        }
        if (shader.m_173348_("ScreenSize") != null) {
            shader.m_173348_("ScreenSize").m_7971_((float)renderWidth, (float)renderHeight);
        }
        Matrix4f prevProj = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting prevVS = RenderSystem.getVertexSorting();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        float[] savedShaderColor = RenderSystem.getShaderColor();
        Matrix4f orthoOutput = new Matrix4f().setOrtho(0.0f, (float)renderWidth, (float)renderHeight, 0.0f, 1000.0f, 3000.0f);
        try {
            if (useLowResTarget) {
                lowResGodRayTarget.m_83954_(Minecraft.f_91002_);
                lowResGodRayTarget.m_83947_(true);
            } else {
                outputTarget.m_83947_(true);
            }
            RenderSystem.setProjectionMatrix((Matrix4f)orthoOutput, (VertexSorting)VertexSorting.f_276633_);
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(orthoOutput);
            }
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            if (useLowResTarget) {
                GlStateManager._disableBlend();
            } else {
                GlStateManager._enableBlend();
                GlStateManager._blendFunc((int)1, (int)1);
            }
            shader.m_173363_();
            GodRaySystem.renderFullscreenQuad(renderWidth, renderHeight);
            shader.m_173362_();
            if (useLowResTarget) {
                outputTarget.m_83947_(true);
                GlStateManager._enableBlend();
                GlStateManager._blendFunc((int)1, (int)1);
                lowResGodRayTarget.m_83957_(outputWidth, outputHeight, false);
            }
        }
        finally {
            shader.m_173362_();
            RenderSystem.setProjectionMatrix((Matrix4f)prevProj, (VertexSorting)prevVS);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._disableBlend();
            RenderSystem.defaultBlendFunc();
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderColor((float)savedShaderColor[0], (float)savedShaderColor[1], (float)savedShaderColor[2], (float)savedShaderColor[3]);
            RenderSystem.setShaderTexture((int)0, (int)savedShaderTex0);
            RenderSystem.setShaderTexture((int)1, (int)savedShaderTex1);
            RenderSystem.setShaderTexture((int)2, (int)savedShaderTex2);
            RenderSystem.activeTexture((int)33984);
            outputTarget.m_83947_(true);
        }
    }

    private static void ensureLowResTarget(int renderWidth, int renderHeight) {
        if (lowResGodRayTarget == null) {
            lowResGodRayTarget = new TextureTarget(renderWidth, renderHeight, false, Minecraft.f_91002_);
            lowResGodRayTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            lowResGodRayTarget.m_83936_(9729);
        } else if (GodRaySystem.lowResGodRayTarget.f_83915_ != renderWidth || GodRaySystem.lowResGodRayTarget.f_83916_ != renderHeight) {
            lowResGodRayTarget.m_83941_(renderWidth, renderHeight, Minecraft.f_91002_);
            lowResGodRayTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            lowResGodRayTarget.m_83936_(9729);
        }
    }

    private static void renderFullscreenQuad(int width, int height) {
        Tesselator tess = RenderSystem.renderThreadTesselator();
        BufferBuilder buf = tess.m_85915_();
        buf.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        buf.m_5483_(0.0, (double)height, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
        buf.m_5483_((double)width, (double)height, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
        buf.m_5483_((double)width, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
        buf.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
        BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buf.m_231175_());
    }

    static {
        INVERSE_PROJECTION = new Matrix4f();
    }
}

