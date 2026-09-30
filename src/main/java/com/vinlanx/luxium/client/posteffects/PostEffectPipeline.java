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
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package com.vinlanx.luxium.client.posteffects;

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
import com.vinlanx.luxium.client.clouds.LuxiumCloudRenderer;
import com.vinlanx.luxium.client.posteffects.fog;
import com.vinlanx.luxium.client.posteffects.lensflare;
import com.vinlanx.luxium.client.posteffects.skygodrays;
import com.vinlanx.luxium.client.posteffects.skyvolumetrigodrays;
import com.vinlanx.luxium.client.posteffects.tonemap;
import com.vinlanx.luxium.client.postprocess.PostProcessSwapChain;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class PostEffectPipeline {
    private static final Matrix4f PREVIOUS_PROJECTION = new Matrix4f();
    private static TextureTarget effectsTarget;
    private static boolean deferredTonemap;
    private static boolean deferredTonemapCompositeEffects;
    private static int deferredEffectsTextureId;

    private PostEffectPipeline() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        block35: {
            int effectsHeight;
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                return;
            }
            if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
                return;
            }
            PostEffectPipeline.clearDeferredTonemap();
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91073_ == null || mc.f_91074_ == null) {
                return;
            }
            boolean renderFog = fog.isEnabled();
            boolean renderRays = skygodrays.isEnabled() && skygodrays.hasVisibleCelestial();
            boolean renderVolumetricRays = skyvolumetrigodrays.shouldRender();
            boolean renderFlare = lensflare.isEnabled() && skygodrays.hasVisibleCelestial();
            boolean renderEffects = renderFog || renderRays || renderVolumetricRays || renderFlare;
            boolean renderTonemap = tonemap.isEnabled();
            if (!renderEffects && !renderTonemap) {
                return;
            }
            RenderTarget main = mc.m_91385_();
            int width = main.f_83917_;
            int height = main.f_83918_;
            if (width <= 0 || height <= 0) {
                return;
            }
            float renderScale = Mth.m_14036_((float)((Double)Config.CLIENT.postEffectsRenderScale.get()).floatValue(), (float)0.1f, (float)1.0f);
            boolean useEffectsTarget = renderEffects && (renderTonemap || renderScale < 0.999f);
            int effectsWidth = useEffectsTarget ? Math.max(1, Math.round((float)width * renderScale)) : width;
            int n = effectsHeight = useEffectsTarget ? Math.max(1, Math.round((float)height * renderScale)) : height;
            if (useEffectsTarget) {
                PostEffectPipeline.ensureEffectsTarget(effectsWidth, effectsHeight);
            }
            int effectsDepthTexture = useEffectsTarget ? main.m_83980_() : SharedPostResources.getDepthTextureId();
            PREVIOUS_PROJECTION.set((Matrix4fc)RenderSystem.getProjectionMatrix());
            VertexSorting previousVertexSorting = RenderSystem.getVertexSorting();
            int[] savedTextures = PostEffectPipeline.saveShaderTextures();
            float[] savedShaderColor = RenderSystem.getShaderColor();
            int cloudOcclusionTexture = 0;
            if (renderRays || renderFlare) {
                Vec3 cameraPos = event.getCamera().m_90583_();
                cloudOcclusionTexture = LuxiumCloudRenderer.get().renderOcclusionMask(event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick(), cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_, event.getRenderTick(), effectsWidth, effectsHeight);
            }
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.m_85836_();
            modelViewStack.m_166856_();
            modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
            RenderSystem.applyModelViewMatrix();
            try {
                ShaderInstance shader;
                if (renderEffects) {
                    boolean useCombinedEffects;
                    RenderTarget destination = main;
                    int activeEffectCount = (renderFog ? 1 : 0) + (renderRays ? 1 : 0) + (renderVolumetricRays ? 1 : 0) + (renderFlare ? 1 : 0);
                    ShaderInstance combinedEffects = renderVolumetricRays || activeEffectCount >= 2 ? ShaderManager.getPostEffectsCombinedShader() : null;
                    boolean bl = useCombinedEffects = combinedEffects != null;
                    if (useEffectsTarget) {
                        boolean firstPassOverwrites;
                        TextureTarget target = effectsTarget;
                        if (target == null) {
                            return;
                        }
                        target.m_83947_(true);
                        boolean bl2 = firstPassOverwrites = useCombinedEffects || renderFog && fog.getShader() != null;
                        if (!firstPassOverwrites) {
                            target.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
                            target.m_83954_(Minecraft.f_91002_);
                            target.m_83947_(true);
                        }
                        destination = target;
                    }
                    if (useCombinedEffects) {
                        combinedEffects.m_173350_("DepthSampler", (Object)effectsDepthTexture);
                        PostEffectPipeline.setUniform1i(combinedEffects, "RenderFogEffect", renderFog ? 1 : 0);
                        PostEffectPipeline.setUniform1i(combinedEffects, "RenderSkyRays", renderRays ? 1 : 0);
                        PostEffectPipeline.setUniform1i(combinedEffects, "RenderSkyVolumetricRays", renderVolumetricRays ? 1 : 0);
                        PostEffectPipeline.setUniform1i(combinedEffects, "RenderLensFlare", renderFlare ? 1 : 0);
                        PostEffectPipeline.setUniform1i(combinedEffects, "RenderCelestialVisibility", renderRays || renderFlare ? 1 : 0);
                        if (renderFog) {
                            fog.configure(combinedEffects, event, mc, effectsDepthTexture);
                        }
                        if (renderRays) {
                            skygodrays.configure(combinedEffects, effectsWidth, effectsHeight, effectsDepthTexture, cloudOcclusionTexture);
                        }
                        if (renderVolumetricRays) {
                            skyvolumetrigodrays.configure(combinedEffects, event, renderFog);
                        }
                        if (renderFlare) {
                            if (renderRays) {
                                lensflare.configureEffectUniforms(combinedEffects);
                            } else {
                                lensflare.configure(combinedEffects, effectsWidth, effectsHeight, effectsDepthTexture, cloudOcclusionTexture);
                            }
                        }
                        destination.m_83947_(true);
                        PostEffectPipeline.draw(combinedEffects, effectsWidth, effectsHeight, useEffectsTarget ? BlendMode.OVERWRITE : (renderFog ? BlendMode.PREMULTIPLIED : BlendMode.ADDITIVE));
                    } else {
                        ShaderInstance shader2;
                        if (renderFog) {
                            destination.m_83947_(true);
                            shader2 = fog.getShader();
                            if (shader2 != null) {
                                fog.configure(shader2, event, mc, effectsDepthTexture);
                                PostEffectPipeline.draw(shader2, effectsWidth, effectsHeight, useEffectsTarget ? BlendMode.OVERWRITE : BlendMode.PREMULTIPLIED);
                            }
                        }
                        if (renderRays && (shader2 = skygodrays.getShader()) != null) {
                            skygodrays.configure(shader2, effectsWidth, effectsHeight, effectsDepthTexture, cloudOcclusionTexture);
                            destination.m_83947_(true);
                            PostEffectPipeline.draw(shader2, effectsWidth, effectsHeight, BlendMode.ADDITIVE);
                        }
                        if (renderFlare && (shader2 = lensflare.getShader()) != null) {
                            lensflare.configure(shader2, effectsWidth, effectsHeight, effectsDepthTexture, cloudOcclusionTexture);
                            destination.m_83947_(true);
                            PostEffectPipeline.draw(shader2, effectsWidth, effectsHeight, BlendMode.ADDITIVE);
                        }
                    }
                }
                if (renderTonemap) {
                    int effectsTexture;
                    shader = tonemap.getShader();
                    if (shader == null) break block35;
                    int sceneTexture = main.m_83975_();
                    int n2 = effectsTexture = useEffectsTarget && effectsTarget != null ? effectsTarget.m_83975_() : 0;
                    if (LsrSystem.shouldDeferTonemap()) {
                        deferredTonemap = true;
                        deferredTonemapCompositeEffects = useEffectsTarget;
                        deferredEffectsTextureId = effectsTexture;
                        break block35;
                    }
                    tonemap.configure(shader, sceneTexture, effectsTexture, useEffectsTarget);
                    int previousSceneFilter = main.f_83922_;
                    if (previousSceneFilter != 9729) {
                        main.m_83936_(9729);
                    }
                    try {
                        RenderTarget postTarget = PostProcessSwapChain.begin(main);
                        postTarget.m_83947_(true);
                        PostEffectPipeline.draw(shader, width, height, BlendMode.OVERWRITE);
                    }
                    finally {
                        if (main.f_83922_ != previousSceneFilter) {
                            main.m_83936_(previousSceneFilter);
                        }
                    }
                    PostProcessSwapChain.commit(main);
                    break block35;
                }
                if (useEffectsTarget && effectsTarget != null && (shader = ShaderManager.getPostEffectCompositeShader()) != null) {
                    shader.m_173350_("EffectsSampler", (Object)effectsTarget.m_83975_());
                    main.m_83947_(true);
                    PostEffectPipeline.draw(shader, width, height, BlendMode.PREMULTIPLIED);
                }
            }
            finally {
                PostEffectPipeline.restoreRenderState(modelViewStack, previousVertexSorting, savedTextures, savedShaderColor, main);
            }
        }
    }

    public static boolean hasDeferredTonemap() {
        return deferredTonemap;
    }

    public static boolean deferredTonemapCompositesEffects() {
        return deferredTonemapCompositeEffects;
    }

    public static int getDeferredEffectsTextureId() {
        return deferredEffectsTextureId;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean flushDeferredTonemapForSceneConsumer() {
        if (!deferredTonemap) {
            return false;
        }
        Minecraft mc = Minecraft.m_91087_();
        RenderTarget main = mc.m_91385_();
        ShaderInstance shader = tonemap.getShader();
        if (main == null || shader == null || main.f_83917_ <= 0 || main.f_83918_ <= 0) {
            return false;
        }
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousVertexSorting = RenderSystem.getVertexSorting();
        int[] savedTextures = PostEffectPipeline.saveShaderTextures();
        float[] savedShaderColor = RenderSystem.getShaderColor();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        int previousSceneFilter = main.f_83922_;
        try {
            tonemap.configure(shader, main.m_83975_(), deferredEffectsTextureId, deferredTonemapCompositeEffects, false, main.f_83915_, main.f_83916_);
            if (previousSceneFilter != 9729) {
                main.m_83936_(9729);
            }
            RenderTarget postTarget = PostProcessSwapChain.begin(main);
            postTarget.m_83947_(true);
            PostEffectPipeline.draw(shader, main.f_83917_, main.f_83918_, BlendMode.OVERWRITE);
            PostProcessSwapChain.commit(main);
            PostEffectPipeline.clearDeferredTonemap();
            boolean bl = true;
            return bl;
        }
        finally {
            if (main.f_83922_ != previousSceneFilter) {
                main.m_83936_(previousSceneFilter);
            }
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousVertexSorting);
            GlStateManager._disableBlend();
            RenderSystem.defaultBlendFunc();
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderColor((float)savedShaderColor[0], (float)savedShaderColor[1], (float)savedShaderColor[2], (float)savedShaderColor[3]);
            for (int i = 0; i < savedTextures.length; ++i) {
                RenderSystem.setShaderTexture((int)i, (int)savedTextures[i]);
            }
            RenderSystem.activeTexture((int)33984);
            main.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_);
        }
    }

    public static void clearDeferredTonemap() {
        deferredTonemap = false;
        deferredTonemapCompositeEffects = false;
        deferredEffectsTextureId = 0;
    }

    public static boolean needsSharedDepthSnapshot() {
        boolean renderEffects;
        boolean renderFog = fog.isEnabled();
        boolean celestialVisible = skygodrays.hasVisibleCelestial();
        boolean renderRays = skygodrays.isEnabled() && celestialVisible;
        boolean renderVolumetricRays = skyvolumetrigodrays.shouldRender();
        boolean renderFlare = lensflare.isEnabled() && celestialVisible;
        boolean bl = renderEffects = renderFog || renderRays || renderVolumetricRays || renderFlare;
        if (!renderEffects) {
            return false;
        }
        float renderScale = Mth.m_14036_((float)((Double)Config.CLIENT.postEffectsRenderScale.get()).floatValue(), (float)0.1f, (float)1.0f);
        boolean useEffectsTarget = tonemap.isEnabled() || renderScale < 0.999f;
        return !useEffectsTarget;
    }

    private static void setUniform1i(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void draw(ShaderInstance shader, int width, int height, BlendMode blendMode) {
        Matrix4f projection = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix((Matrix4f)projection, (VertexSorting)VertexSorting.f_276633_);
        RenderSystem.viewport((int)0, (int)0, (int)width, (int)height);
        if (shader.f_173308_ != null) {
            shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
        }
        if (shader.f_173309_ != null) {
            shader.f_173309_.m_5679_(projection);
        }
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask((boolean)false);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        shader.m_173363_();
        try {
            PostEffectPipeline.applyBlendMode(blendMode);
            PostEffectPipeline.renderFullscreenQuad(width, height);
        }
        finally {
            shader.m_173362_();
        }
    }

    private static void applyBlendMode(BlendMode blendMode) {
        if (blendMode == BlendMode.OVERWRITE) {
            RenderSystem.disableBlend();
        } else if (blendMode == BlendMode.PREMULTIPLIED) {
            RenderSystem.enableBlend();
            GlStateManager._blendFuncSeparate((int)1, (int)771, (int)1, (int)771);
        } else {
            RenderSystem.enableBlend();
            GlStateManager._blendFuncSeparate((int)1, (int)1, (int)0, (int)1);
        }
    }

    private static void renderFullscreenQuad(int width, int height) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder buffer = tessellator.m_85915_();
        buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        buffer.m_5483_(0.0, (double)height, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
        buffer.m_5483_((double)width, (double)height, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
        buffer.m_5483_((double)width, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
        buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
        BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
    }

    private static int[] saveShaderTextures() {
        int[] textures = new int[5];
        for (int i = 0; i < textures.length; ++i) {
            textures[i] = RenderSystem.getShaderTexture((int)i);
        }
        return textures;
    }

    private static void restoreRenderState(PoseStack modelViewStack, VertexSorting previousVertexSorting, int[] savedTextures, float[] savedShaderColor, RenderTarget main) {
        modelViewStack.m_85849_();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix((Matrix4f)PREVIOUS_PROJECTION, (VertexSorting)previousVertexSorting);
        GlStateManager._disableBlend();
        RenderSystem.defaultBlendFunc();
        GlStateManager._depthMask((boolean)true);
        GlStateManager._enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.setShaderColor((float)savedShaderColor[0], (float)savedShaderColor[1], (float)savedShaderColor[2], (float)savedShaderColor[3]);
        for (int i = 0; i < savedTextures.length; ++i) {
            RenderSystem.setShaderTexture((int)i, (int)savedTextures[i]);
        }
        RenderSystem.activeTexture((int)33984);
        main.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_);
    }

    private static void ensureEffectsTarget(int width, int height) {
        if (effectsTarget == null) {
            effectsTarget = new TextureTarget(width, height, false, Minecraft.f_91002_);
            effectsTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            effectsTarget.m_83936_(9729);
        } else if (PostEffectPipeline.effectsTarget.f_83915_ != width || PostEffectPipeline.effectsTarget.f_83916_ != height) {
            effectsTarget.m_83941_(width, height, Minecraft.f_91002_);
            effectsTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            effectsTarget.m_83936_(9729);
        }
    }

    private static enum BlendMode {
        OVERWRITE,
        PREMULTIPLIED,
        ADDITIVE;

    }
}

