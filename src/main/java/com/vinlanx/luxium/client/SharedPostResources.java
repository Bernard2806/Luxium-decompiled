/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  net.minecraft.client.Minecraft
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class SharedPostResources {
    private static TextureTarget sharedDepthTarget;
    private static TextureTarget sharedColorTarget;
    private static TextureTarget waterRenderTarget;

    private SharedPostResources() {
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void captureSharedBuffers(RenderLevelStageEvent event) {
        boolean needsDepth;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        RenderTarget main = mc.m_91385_();
        boolean bl = needsDepth = PostEffectPipeline.needsSharedDepthSnapshot() || Config.isFeatureEnabled(Config.CLIENT.kawaseBloomEnabled) || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled);
        if (needsDepth) {
            SharedPostResources.captureDepth(main);
        }
        if (needsDepth) {
            main.m_83947_(true);
        }
    }

    private static void ensureDepthTarget(RenderTarget main) {
        if (sharedDepthTarget == null) {
            sharedDepthTarget = new TextureTarget(main.f_83915_, main.f_83916_, true, Minecraft.f_91002_);
            sharedDepthTarget.m_83936_(9728);
        } else if (SharedPostResources.sharedDepthTarget.f_83915_ != main.f_83915_ || SharedPostResources.sharedDepthTarget.f_83916_ != main.f_83916_) {
            sharedDepthTarget.m_83941_(main.f_83915_, main.f_83916_, Minecraft.f_91002_);
            sharedDepthTarget.m_83936_(9728);
        }
    }

    private static void ensureColorTarget(RenderTarget main) {
        if (sharedColorTarget == null) {
            sharedColorTarget = new TextureTarget(main.f_83915_, main.f_83916_, false, Minecraft.f_91002_);
            sharedColorTarget.m_83936_(9729);
        } else if (SharedPostResources.sharedColorTarget.f_83915_ != main.f_83915_ || SharedPostResources.sharedColorTarget.f_83916_ != main.f_83916_) {
            sharedColorTarget.m_83941_(main.f_83915_, main.f_83916_, Minecraft.f_91002_);
            sharedColorTarget.m_83936_(9729);
        }
    }

    public static void prepareWaterRenderTarget(RenderTarget main) {
        SharedPostResources.ensureWaterRenderTarget(main);
        waterRenderTarget.m_83947_(true);
        GlStateManager._clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GlStateManager._clear((int)16640, (boolean)Minecraft.f_91002_);
        waterRenderTarget.m_83947_(true);
    }

    private static void ensureWaterRenderTarget(RenderTarget main) {
        double scale = Math.max(0.25, Math.min(1.0, (Double)Config.CLIENT.waterRenderScale.get()));
        int width = Math.max(1, (int)Math.ceil((double)main.f_83915_ * scale));
        int height = Math.max(1, (int)Math.ceil((double)main.f_83916_ * scale));
        if (waterRenderTarget == null) {
            waterRenderTarget = new TextureTarget(width, height, true, Minecraft.f_91002_);
            waterRenderTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            waterRenderTarget.m_83936_(9729);
        } else if (SharedPostResources.waterRenderTarget.f_83915_ != width || SharedPostResources.waterRenderTarget.f_83916_ != height) {
            waterRenderTarget.m_83941_(width, height, Minecraft.f_91002_);
            waterRenderTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            waterRenderTarget.m_83936_(9729);
        }
    }

    public static void captureColor(RenderTarget main) {
        SharedPostResources.ensureColorTarget(main);
        GL30.glBindFramebuffer((int)36008, (int)main.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)SharedPostResources.sharedColorTarget.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.f_83915_, (int)main.f_83916_, (int)0, (int)0, (int)SharedPostResources.sharedColorTarget.f_83915_, (int)SharedPostResources.sharedColorTarget.f_83916_, (int)16384, (int)9728);
        main.m_83947_(true);
    }

    public static void captureDepth(RenderTarget main) {
        SharedPostResources.ensureDepthTarget(main);
        sharedDepthTarget.m_83945_(main);
        main.m_83947_(true);
    }

    public static int getDepthTextureId() {
        return sharedDepthTarget != null ? sharedDepthTarget.m_83980_() : -1;
    }

    public static int getColorTextureId() {
        return sharedColorTarget != null ? sharedColorTarget.m_83975_() : -1;
    }

    public static void bindWaterRenderTarget() {
        if (waterRenderTarget != null) {
            waterRenderTarget.m_83947_(true);
        }
    }

    public static int getWaterRenderColorTextureId() {
        return waterRenderTarget != null ? waterRenderTarget.m_83975_() : -1;
    }

    public static int getWaterRenderFramebufferId() {
        return waterRenderTarget != null ? SharedPostResources.waterRenderTarget.f_83920_ : -1;
    }

    public static int getWaterRenderWidth() {
        return waterRenderTarget != null ? SharedPostResources.waterRenderTarget.f_83915_ : 0;
    }

    public static int getWaterRenderHeight() {
        return waterRenderTarget != null ? SharedPostResources.waterRenderTarget.f_83916_ : 0;
    }
}

