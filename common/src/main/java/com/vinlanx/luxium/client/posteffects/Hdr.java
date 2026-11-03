package com.vinlanx.luxium.client.posteffects;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.postprocess.PostProcessSwapChain;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class Hdr {
    private static final boolean EXTERNAL_SHADERS_LOADED = LuxiumPlatform.get


 ().externalShadersLoaded();
    private static boolean initialized;
    private static boolean enabledLastFrame;
    private static float cachedHighlightCompression = Float.NaN;
    private static float cachedHighlightScale = 1.0F;
    private static float cachedExposure = Float.NaN;
    private static float cachedExposureScale = 1.0F;
    private static final Set<RenderTarget> HDR_TARGETS = Collections.newSetFromMap(new WeakHashMap<>());

    private Hdr() { }

    public static boolean isEnabled() {
        return !EXTERNAL_SHADERS_LOADED
            && Config.isFeatureEnabled(Config.CLIENT.hdrEnabled)
            && Config.isFeatureEnabled(Config.CLIENT.tonemapEnabled)
            && ShaderManager.getTonemapShader() != null;
    }

    public static void onConfigChanged() {
        initialized = false;
        TemporalFrameSystem.onConfigChanged();
        PostEffectPipeline.clearDeferredTonemap();
    }

    public static float highlightScale(float compression) {
        if (Float.compare(compression, cachedHighlightCompression) != 0) {
            cachedHighlightCompression = compression;
            float shoulder = 1.5F - 0.75F * compression;
            cachedHighlightScale = (float)(Math.pow(2.0, (double)shoulder) - 1.0);
        }
        return cachedHighlightScale;
    }

    public static float exposureScale(float exposure) {
        if (Float.compare(exposure, cachedExposure) != 0) {
            cachedExposure = exposure;
            cachedExposureScale = (float)Math.pow(2.0, (double)exposure);
        }
        return cachedExposureScale;
    }

    public static void beginFrame(Minecraft mc) {
        RenderSystem.assertOnRenderThread();
        boolean enabled = isEnabled();
        RenderTarget main = mc.getMainRenderTarget();
        boolean restoredMain = !enabled && hasHdrColor(main);
        if (!initialized || enabled != enabledLastFrame) {
            TemporalFrameSystem.onConfigChanged();
            PostEffectPipeline.clearDeferredTonemap();
            ReflectionSystem.get().onColorFormatChanged();
            if (!enabled) { restoreColorTargets();
            } enabledLastFrame = enabled;
            initialized = true;
        }
        boolean separateScene = Config.isFeatureEnabled(Config.CLIENT.tfrEnabled) || Config.isFeatureEnabled
 (Config.CLIENT.lsrEnabled);
        if (configureColorTarget(main, enabled && !separateScene) || restoredMain) {
            main.clear(Minecraft.ON_OSX);
            main.bindWrite(true);
        }
    }

    public static boolean hasHdrColor(RenderTarget target) {
        if (target instanceof HdrColorTarget state && state.luxium$getColorFormat() != 32856) {
            return true;
        }

        return false;
    } public static void releaseResources() { RenderSystem.assertOnRenderThread();
        PostProcessSwapChain.releasePendingScene();
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        boolean restoredMain = hasHdrColor(main);
        restoreColorTargets();
        if (restoredMain) {
            main.clear(Minecraft.ON_OSX);
            main.bindWrite(true);
        }
        initialized = false;
        TemporalFrameSystem.onConfigChanged();
        PostEffectPipeline.clearDeferredTonemap();
    }

    private static void restoreColorTargets() {
        PostProcessSwapChain.releasePendingScene();
        for (RenderTarget target : new ArrayList<>(HDR_TARGETS)) {
            configureColorTarget(target, false);
        }
        HDR_TARGETS.clear();
    }

    public static boolean configureColorTarget(RenderTarget target, boolean enabled) {
        return configureColorTarget(target, enabled, false);
    }

    public static boolean configureColorTarget(RenderTarget target, boolean enabled, boolean preserveAlpha) {
        return configureColorFormat(target, enabled ? (
 preserveAlpha ? '蠚' : '谺') :
 '聘');
    }

    public static boolean matchColorTarget(RenderTarget target, RenderTarget source) {
        int format = source instanceof HdrColorTarget state ? state.luxium$getColorFormat
 () : '聘';
        return configureColorFormat(target, format);
    }

    public static void onColorTexturesSwapped(RenderTarget first, RenderTarget second) {
        if (first instanceof HdrColorTarget a && second instanceof HdrColorTarget b) {
            int format = a.luxium$getColorFormat();
            a.luxium$setColorFormat(b.luxium$getColorFormat());
            b.luxium$setColorFormat(format);
            if (hasHdrColor(first)) {
                HDR_TARGETS.add(first);
            } else {
                HDR_TARGETS.remove(first);
            }

            if (hasHdrColor(second)) {
                HDR_TARGETS.add(second);
            } else {
                HDR_TARGETS.remove(second);
            }

            return;
        }
    }

    private static boolean configureColorFormat(RenderTarget target, int format) {
        RenderSystem.assertOnRenderThread();
        boolean restored = format != 32856 && target != null && PostProcessSwapChain.restoreHdrScene(target);
        if (target != null
            && target.width > 0
            && target.height > 0
            && target.getColorTextureId() > 0
            && target instanceof HdrColorTarget state
            && state.luxium$getColorFormat() != format) {
            int previousTexture = GL11.glGetInteger(32873);
            int previousUnpackBuffer = GL11.glGetInteger(35055);
            int previousUnpackAlignment = GL11.glGetInteger(3317);

            try {
                GL15.glBindBuffer(35052, 0);
                GL11.glPixelStorei(3317, 1);
                GlStateManager._bindTexture(target.getColorTextureId());
                GL11.glTexImage2D(3553, 0, format, target.width, target.height, 0, format == 35898 ? 6407 : 6408, format == 32856 ? 5121 : 5131, 0L);
                state.luxium$setColorFormat(format);
                if (format != 32856) {
                    HDR_TARGETS.add(target);
                } else {
                    HDR_TARGETS.remove(target);
                }
            } finally {
                GlStateManager._bindTexture(previousTexture);
                GL11.glPixelStorei(3317, previousUnpackAlignment);
                GL15.glBindBuffer(35052, previousUnpackBuffer);
            }

            return true;
        }

        return restored;
    }
    }
