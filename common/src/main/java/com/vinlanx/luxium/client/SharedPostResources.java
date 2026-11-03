package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.posteffects.Hdr;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import com.vinlanx.luxium.platform.RenderFrame;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;

public final class SharedPostResources {
    private static TextureTarget sharedDepthTarget;
    private static TextureTarget sharedColorTarget;
    private static TextureTarget waterRenderTarget;

    private SharedPostResources()


    { }

    public static void captureSharedBuffers(RenderFrame event) {
        if (!(event.getStage() == RenderFrame.Stage.AFTER_PARTICLES)) { return;
 } if (!(!ReflectionSystem.isRenderingWorldPass() && !NeoShadowsEngine.isAnyShadowCapturePass())) { return;
 }
                Minecraft mc = Minecraft.getInstance();
                RenderTarget main = mc.getMainRenderTarget();
                boolean needsDepth = PostEffectPipeline.needsSharedDepthSnapshot
 ()
                    || Config.isFeatureEnabled(Config.CLIENT.kawaseBloomEnabled)
                    || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled)
                    || Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled);
                if (needsDepth) {
                    captureDepth(main);
                }

                if (needsDepth) {
                    main.bindWrite(true);
                }
    }

    private static void ensureDepthTarget(RenderTarget main) {
        if (sharedDepthTarget == null) {
            sharedDepthTarget = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            sharedDepthTarget.setFilterMode(9728);
        } else if (sharedDepthTarget.width != main.width || sharedDepthTarget.height != main.height) {
            sharedDepthTarget.resize(main.width, main.height, Minecraft.ON_OSX);
            sharedDepthTarget.setFilterMode(9728);
        }
    }

    private static void ensureColorTarget(RenderTarget main) {
        if (sharedColorTarget == null) {
            sharedColorTarget = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            sharedColorTarget.setFilterMode(9729);
        } else if (sharedColorTarget.width != main.width || sharedColorTarget.height != main.height) {
            sharedColorTarget.resize(main.width, main.height, Minecraft.ON_OSX);
            sharedColorTarget.setFilterMode(9729);
        }
    }

    public static void prepareWaterRenderTarget(RenderTarget main) {
        ensureWaterRenderTarget(main);
        Hdr.configureColorTarget(waterRenderTarget, Hdr.isEnabled(), true);
        waterRenderTarget.bindWrite(true);
        GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager._clear(16640, Minecraft.ON_OSX);
        waterRenderTarget.bindWrite(true);
    }

    private static void ensureWaterRenderTarget(RenderTarget main) {
        double scale = Math.max(0.25, Math.min(1.0, Config.CLIENT.waterRenderScale.get()));
        int width = Math.max(1, (int)Math.ceil((double)main.width * scale));
        int height = Math.max(1, (int)Math.ceil((double)main.height * scale));
        if (waterRenderTarget == null) {
            waterRenderTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            waterRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            waterRenderTarget.setFilterMode(9729);
        } else if (waterRenderTarget.width != width || waterRenderTarget.height != height) {
            waterRenderTarget.resize(width, height, Minecraft.ON_OSX);
            waterRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            waterRenderTarget.setFilterMode(9729);
        }
    }

    public static void captureColor(RenderTarget main) {
        ensureColorTarget(main);
        Hdr.configureColorTarget(sharedColorTarget, Hdr.isEnabled());
        GL30.glBindFramebuffer(36008, main.frameBufferId);
        GL30.glBindFramebuffer(36009, sharedColorTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, sharedColorTarget.width, sharedColorTarget.height, 16384, 9728);


        main.bindWrite(true);
    }

    public static void captureDepth(RenderTarget main) {
        ensureDepthTarget(main);
        sharedDepthTarget.copyDepthFrom(main);
        main.bindWrite(true);
    }

    public static int getDepthTextureId() {
        return sharedDepthTarget != null ? sharedDepthTarget.getDepthTextureId() : -1;
    }

    public static int getColorTextureId() {
        return sharedColorTarget != null ? sharedColorTarget.getColorTextureId() : -1;
    }

    public static void bindWaterRenderTarget() {
        if (waterRenderTarget != null) {
            waterRenderTarget.bindWrite(true);
        }
    }

    public static int getWaterRenderColorTextureId() {
        return waterRenderTarget != null ? waterRenderTarget.getColorTextureId() : -1;
    }

    public static int getWaterRenderFramebufferId() {
        return waterRenderTarget != null ? waterRenderTarget.frameBufferId : -1;
    }

    public static int getWaterRenderWidth() {
        return waterRenderTarget != null ? waterRenderTarget.width : 0;
    }

    public static int getWaterRenderHeight() {
        return waterRenderTarget != null ? waterRenderTarget.height : 0;
    }
    }
