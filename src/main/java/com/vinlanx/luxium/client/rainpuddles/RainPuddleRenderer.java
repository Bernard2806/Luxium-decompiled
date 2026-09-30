/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
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
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package com.vinlanx.luxium.client.rainpuddles;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleInstance;
import com.vinlanx.luxium.client.rainpuddles.RainPuddleManager;
import com.vinlanx.luxium.client.rainpuddles.RainPuddlePatch;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.ssr.ScreenSpaceReflectionSystem;
import com.vinlanx.luxium.client.ssr.SsrConsumer;
import com.vinlanx.luxium.client.ssr.SsrSettings;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class RainPuddleRenderer {
    private static final ResourceLocation MICRO_NORMAL = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"textures/effects/water_micro_normal.png");
    private static final Matrix4f VIEW_ROTATION = new Matrix4f();
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();

    private RainPuddleRenderer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent(priority=EventPriority.NORMAL)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!Config.isEnabled()) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        List<RainPuddleInstance> puddles = RainPuddleManager.get().snapshot(mc.f_91073_);
        if (puddles.isEmpty()) {
            return;
        }
        ShaderInstance shader = ShaderManager.getRainPuddleShader();
        if (shader == null) {
            return;
        }
        RenderTarget main = mc.m_91385_();
        if (main == null || main.f_83915_ <= 0 || main.f_83916_ <= 0 || main.m_83975_() <= 0 || main.m_83980_() <= 0) {
            return;
        }
        SharedPostResources.captureColor(main);
        SharedPostResources.captureDepth(main);
        int sceneColor = SharedPostResources.getColorTextureId();
        int sceneDepth = SharedPostResources.getDepthTextureId();
        if (sceneColor <= 0 || sceneDepth <= 0) {
            main.m_83947_(true);
            return;
        }
        main.m_83947_(true);
        Vec3 camera = event.getCamera().m_90583_();
        Matrix4f viewRotation = VIEW_ROTATION.set((Matrix4fc)event.getPoseStack().m_85850_().m_252922_());
        Matrix4f viewProj = NeoSkyFrameCache.copyViewProjection(event.getProjectionMatrix(), viewRotation, VIEW_PROJ);
        Matrix4f invViewProj = NeoSkyFrameCache.copyInverseViewProjection(event.getProjectionMatrix(), viewRotation, INV_VIEW_PROJ);
        mc.m_91097_().m_174784_(MICRO_NORMAL);
        AbstractTexture microTexture = mc.m_91097_().m_118506_(MICRO_NORMAL);
        int microNormal = microTexture.m_117963_();
        SsrSettings ssr = ScreenSpaceReflectionSystem.settings(SsrConsumer.PUDDLE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.setShader(() -> shader);
        float time = ((float)mc.f_91073_.m_46467_() + event.getPartialTick()) / 20.0f % 4096.0f;
        RainPuddleRenderer.setCommonUniforms(shader, viewProj, invViewProj, camera, main.f_83915_, main.f_83916_, time, ssr);
        shader.m_173350_("SceneSampler", (Object)sceneColor);
        shader.m_173350_("DepthSampler", (Object)sceneDepth);
        shader.m_173350_("MicroNormalSampler", (Object)microNormal);
        try {
            for (RainPuddleInstance puddle : puddles) {
                mc.m_91097_().m_174784_(puddle.depthMap());
                AbstractTexture depthMap = mc.m_91097_().m_118506_(puddle.depthMap());
                int depthMapTexture = depthMap.m_117963_();
                if (depthMapTexture <= 0) continue;
                shader.m_173350_("PuddleDepthSampler", (Object)depthMapTexture);
                RainPuddleRenderer.drawPuddle(puddle, camera);
            }
        }
        finally {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            main.m_83947_(true);
        }
    }

    private static void drawPuddle(RainPuddleInstance puddle, Vec3 camera) {
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        for (RainPuddlePatch p : puddle.patches()) {
            float x0 = (float)(p.minX() - camera.f_82479_);
            float x1 = (float)(p.maxX() - camera.f_82479_);
            float y = (float)(p.y() - camera.f_82480_);
            float z0 = (float)(p.minZ() - camera.f_82481_);
            float z1 = (float)(p.maxZ() - camera.f_82481_);
            builder.m_5483_((double)x0, (double)y, (double)z0).m_7421_(p.u0(), p.v0()).m_5752_();
            builder.m_5483_((double)x0, (double)y, (double)z1).m_7421_(p.u0(), p.v1()).m_5752_();
            builder.m_5483_((double)x1, (double)y, (double)z1).m_7421_(p.u1(), p.v1()).m_5752_();
            builder.m_5483_((double)x1, (double)y, (double)z0).m_7421_(p.u1(), p.v0()).m_5752_();
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
    }

    private static void setCommonUniforms(ShaderInstance shader, Matrix4f viewProj, Matrix4f invViewProj, Vec3 camera, int width, int height, float time, SsrSettings ssr) {
        RainPuddleRenderer.setMat4(shader, "ViewProj", viewProj);
        RainPuddleRenderer.setMat4(shader, "InvViewProj", invViewProj);
        RainPuddleRenderer.set3f(shader, "CameraPos", (float)camera.f_82479_, (float)camera.f_82480_, (float)camera.f_82481_);
        RainPuddleRenderer.set2f(shader, "ScreenSize", width, height);
        RainPuddleRenderer.set1f(shader, "Time", time);
        RainPuddleRenderer.set1f(shader, "MaxVirtualDepth", ((Double)Config.CLIENT.puddleMaxDepth.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "DepthCurve", ((Double)Config.CLIENT.puddleDepthCurve.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "WetDarkening", ((Double)Config.CLIENT.puddleWetDarkening.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "SurfaceOpacity", ((Double)Config.CLIENT.puddleSurfaceOpacity.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "EdgeSoftness", ((Double)Config.CLIENT.puddleEdgeSoftness.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "WaveStrength", ((Double)Config.CLIENT.puddleWaveStrength.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "WaveScale", ((Double)Config.CLIENT.puddleWaveScale.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "WaveSpeed", ((Double)Config.CLIENT.puddleWaveSpeed.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "RippleStrength", ((Double)Config.CLIENT.puddleRippleStrength.get()).floatValue());
        RainPuddleRenderer.set1f(shader, "RefractionStrength", ((Double)Config.CLIENT.puddleRefractionStrength.get()).floatValue());
        RainPuddleRenderer.set1i(shader, "SsrEnabled", ssr.enabled() ? 1 : 0);
        RainPuddleRenderer.set1i(shader, "SsrSteps", ssr.coarseSteps());
        RainPuddleRenderer.set1i(shader, "SsrRefinementSteps", ssr.refinementSteps());
        RainPuddleRenderer.set1f(shader, "SsrMaxDistance", ssr.maxDistance());
        RainPuddleRenderer.set1f(shader, "SsrThickness", ssr.thickness());
        RainPuddleRenderer.set1f(shader, "SsrEdgeFade", ssr.edgeFade());
        RainPuddleRenderer.set1f(shader, "SsrStrength", ssr.strength());
    }

    private static void set1f(ShaderInstance shader, String name, float value) {
        Uniform u = shader.m_173348_(name);
        if (u != null) {
            u.m_5985_(value);
        }
    }

    private static void set1i(ShaderInstance shader, String name, int value) {
        Uniform u = shader.m_173348_(name);
        if (u != null) {
            u.m_142617_(value);
        }
    }

    private static void set2f(ShaderInstance shader, String name, float x, float y) {
        Uniform u = shader.m_173348_(name);
        if (u != null) {
            u.m_7971_(x, y);
        }
    }

    private static void set3f(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform u = shader.m_173348_(name);
        if (u != null) {
            u.m_5889_(x, y, z);
        }
    }

    private static void setMat4(ShaderInstance shader, String name, Matrix4f value) {
        Uniform u = shader.m_173348_(name);
        if (u != null) {
            u.m_5679_(value);
        }
    }
}

