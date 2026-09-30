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
 *  org.joml.Vector3f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 */
package com.vinlanx.luxium.client.wet;

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
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaLighting;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.ssr.ScreenSpaceReflectionSystem;
import com.vinlanx.luxium.client.ssr.SsrConsumer;
import com.vinlanx.luxium.client.ssr.SsrSettings;
import java.nio.IntBuffer;
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
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class WetSurfaceRenderer {
    private static final ResourceLocation MICRO_NORMAL = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"textures/effects/water_micro_normal.png");
    private static TextureTarget terrainDepthTarget;
    private static boolean terrainDepthReady;
    private static final IntBuffer SAVED_VIEWPORT;
    private static final IntBuffer SAVED_SCISSOR;
    private static final Matrix4f VIEW_PROJ;
    private static final Matrix4f INV_VIEW_PROJ;

    private WetSurfaceRenderer() {
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void captureSolidTerrainDepth(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        if (!Config.isFeatureEnabled(Config.CLIENT.wetEnabled)) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        terrainDepthReady = false;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null) {
            return;
        }
        RenderTarget main = mc.m_91385_();
        if (main == null || main.f_83915_ <= 0 || main.f_83916_ <= 0 || main.m_83980_() <= 0) {
            return;
        }
        WetSurfaceRenderer.ensureTerrainDepthTarget(main);
        terrainDepthTarget.m_83945_(main);
        terrainDepthReady = terrainDepthTarget.m_83980_() > 0;
        main.m_83947_(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void renderWetSurfaces(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!Config.isFeatureEnabled(Config.CLIENT.wetEnabled) || !terrainDepthReady) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        ShaderInstance shader = ShaderManager.getWetSurfaceShader();
        if (shader == null || terrainDepthTarget == null) {
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
        int terrainDepth = terrainDepthTarget.m_83980_();
        if (sceneColor <= 0 || sceneDepth <= 0 || terrainDepth <= 0) {
            main.m_83947_(true);
            return;
        }
        mc.m_91097_().m_174784_(MICRO_NORMAL);
        AbstractTexture microTexture = mc.m_91097_().m_118506_(MICRO_NORMAL);
        int microNormal = microTexture.m_117963_();
        if (microNormal <= 0) {
            main.m_83947_(true);
            return;
        }
        Vec3 camera = event.getCamera().m_90583_();
        Matrix4f viewRotation = event.getPoseStack().m_85850_().m_252922_();
        Matrix4f viewProj = NeoSkyFrameCache.copyViewProjection(event.getProjectionMatrix(), viewRotation, VIEW_PROJ);
        Matrix4f invViewProj = NeoSkyFrameCache.copyInverseViewProjection(event.getProjectionMatrix(), viewRotation, INV_VIEW_PROJ);
        SsrSettings ssr = ScreenSpaceReflectionSystem.settings(SsrConsumer.WET);
        NeoSkyCelestiaLighting.State lighting = NeoSkyFrameCache.lighting(mc.f_91073_, event.getPartialTick());
        boolean hasSkyEnvironment = mc.f_91073_.m_6042_().f_223549_();
        float time = ((float)mc.f_91073_.m_46467_() + event.getPartialTick()) / 20.0f % 4096.0f;
        WetSurfaceRenderer.renderFullscreen(shader, main, sceneColor, sceneDepth, terrainDepth, microNormal, viewProj, invViewProj, camera, time, ssr, lighting, hasSkyEnvironment);
    }

    private static void ensureTerrainDepthTarget(RenderTarget main) {
        if (terrainDepthTarget == null) {
            terrainDepthTarget = new TextureTarget(main.f_83915_, main.f_83916_, true, Minecraft.f_91002_);
            terrainDepthTarget.m_83936_(9728);
        } else if (WetSurfaceRenderer.terrainDepthTarget.f_83915_ != main.f_83915_ || WetSurfaceRenderer.terrainDepthTarget.f_83916_ != main.f_83916_) {
            terrainDepthTarget.m_83941_(main.f_83915_, main.f_83916_, Minecraft.f_91002_);
            terrainDepthTarget.m_83936_(9728);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void renderFullscreen(ShaderInstance shader, RenderTarget main, int sceneColor, int sceneDepth, int terrainDepth, int microNormal, Matrix4f viewProj, Matrix4f invViewProj, Vec3 camera, float time, SsrSettings ssr, NeoSkyCelestiaLighting.State lighting, boolean hasSkyEnvironment) {
        float[] savedShaderColor = RenderSystem.getShaderColor();
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        SAVED_VIEWPORT.clear();
        SAVED_SCISSOR.clear();
        GL11.glGetIntegerv((int)2978, (IntBuffer)SAVED_VIEWPORT);
        GL11.glGetIntegerv((int)3088, (IntBuffer)SAVED_SCISSOR);
        boolean previousScissorEnabled = GL11.glIsEnabled((int)3089);
        int passWidth = Math.max(1, main.f_83915_);
        int passHeight = Math.max(1, main.f_83916_);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)passWidth, (float)passHeight, 0.0f, 1000.0f, 3000.0f);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            main.m_83947_(true);
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            RenderSystem.disableScissor();
            RenderSystem.viewport((int)0, (int)0, (int)passWidth, (int)passHeight);
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(ortho);
            }
            shader.m_173350_("SceneSampler", (Object)sceneColor);
            shader.m_173350_("DepthSampler", (Object)sceneDepth);
            shader.m_173350_("TerrainDepthSampler", (Object)terrainDepth);
            shader.m_173350_("MicroNormalSampler", (Object)microNormal);
            WetSurfaceRenderer.setMat4(shader, "ViewProj", viewProj);
            WetSurfaceRenderer.setMat4(shader, "InvViewProj", invViewProj);
            WetSurfaceRenderer.set3f(shader, "CameraPos", (float)camera.f_82479_, (float)camera.f_82480_, (float)camera.f_82481_);
            WetSurfaceRenderer.set2f(shader, "ScreenSize", main.f_83915_, main.f_83916_);
            WetSurfaceRenderer.set1f(shader, "Time", time);
            WetSurfaceRenderer.set1f(shader, "MaxVirtualDepth", ((Double)Config.CLIENT.wetMaxDepth.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "DepthCurve", ((Double)Config.CLIENT.wetDepthCurve.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "WetDarkening", ((Double)Config.CLIENT.wetDarkening.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "SurfaceOpacity", ((Double)Config.CLIENT.wetSurfaceOpacity.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "EdgeSoftness", ((Double)Config.CLIENT.wetEdgeSoftness.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "WaveStrength", ((Double)Config.CLIENT.wetWaveStrength.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "WaveScale", ((Double)Config.CLIENT.wetWaveScale.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "WaveSpeed", ((Double)Config.CLIENT.wetWaveSpeed.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "RippleStrength", ((Double)Config.CLIENT.wetRippleStrength.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "RefractionStrength", ((Double)Config.CLIENT.wetRefractionStrength.get()).floatValue());
            Vector3f lightDirection = lighting.direction();
            Vector3f lightColor = lighting.directColor();
            Vector3f skyColor = lighting.skyAmbientColor();
            Vector3f groundColor = lighting.groundAmbientColor();
            WetSurfaceRenderer.set3f(shader, "CelestialDirection", lightDirection.x, lightDirection.y, lightDirection.z);
            WetSurfaceRenderer.set3f(shader, "CelestialColor", lightColor.x, lightColor.y, lightColor.z);
            WetSurfaceRenderer.set3f(shader, "SkyAmbientColor", skyColor.x, skyColor.y, skyColor.z);
            WetSurfaceRenderer.set3f(shader, "GroundAmbientColor", groundColor.x, groundColor.y, groundColor.z);
            WetSurfaceRenderer.set1f(shader, "CelestialStrength", hasSkyEnvironment ? lighting.directStrength() : 0.0f);
            WetSurfaceRenderer.set1f(shader, "AmbientStrength", hasSkyEnvironment ? lighting.ambientStrength() : 0.0f);
            WetSurfaceRenderer.set1f(shader, "EnvironmentReflectionStrength", hasSkyEnvironment ? ((Double)Config.CLIENT.wetEnvironmentReflectionStrength.get()).floatValue() : 0.0f);
            WetSurfaceRenderer.set1f(shader, "CelestialSpecularStrength", ((Double)Config.CLIENT.wetCelestialSpecularStrength.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "SheenFloor", ((Double)Config.CLIENT.wetSheenFloor.get()).floatValue());
            WetSurfaceRenderer.set1f(shader, "RippleHighlightStrength", ((Double)Config.CLIENT.wetRippleHighlightStrength.get()).floatValue());
            WetSurfaceRenderer.set1i(shader, "SsrEnabled", ssr.enabled() ? 1 : 0);
            WetSurfaceRenderer.set1i(shader, "SsrSteps", ssr.coarseSteps());
            WetSurfaceRenderer.set1i(shader, "SsrRefinementSteps", ssr.refinementSteps());
            WetSurfaceRenderer.set1f(shader, "SsrMaxDistance", ssr.maxDistance());
            WetSurfaceRenderer.set1f(shader, "SsrThickness", ssr.thickness());
            WetSurfaceRenderer.set1f(shader, "SsrEdgeFade", ssr.edgeFade());
            WetSurfaceRenderer.set1f(shader, "SsrStrength", ssr.strength());
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            shader.m_173363_();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_(0.0, (double)passHeight, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)passWidth, (double)passHeight, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)passWidth, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            RenderSystem.viewport((int)SAVED_VIEWPORT.get(0), (int)SAVED_VIEWPORT.get(1), (int)SAVED_VIEWPORT.get(2), (int)SAVED_VIEWPORT.get(3));
            if (previousScissorEnabled) {
                GL11.glEnable((int)3089);
                GL11.glScissor((int)SAVED_SCISSOR.get(0), (int)SAVED_SCISSOR.get(1), (int)SAVED_SCISSOR.get(2), (int)SAVED_SCISSOR.get(3));
            } else {
                GL11.glDisable((int)3089);
            }
            RenderSystem.setShaderColor((float)savedShaderColor[0], (float)savedShaderColor[1], (float)savedShaderColor[2], (float)savedShaderColor[3]);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void set1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void set1i(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    private static void set2f(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }

    private static void set3f(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private static void setMat4(ShaderInstance shader, String name, Matrix4f value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5679_(value);
        }
    }

    static {
        SAVED_VIEWPORT = BufferUtils.createIntBuffer((int)4);
        SAVED_SCISSOR = BufferUtils.createIntBuffer((int)4);
        VIEW_PROJ = new Matrix4f();
        INV_VIEW_PROJ = new Matrix4f();
    }
}

