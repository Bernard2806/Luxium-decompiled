/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.ssr;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ssr.SsrConsumer;
import com.vinlanx.luxium.client.ssr.SsrHitBuffer;
import com.vinlanx.luxium.client.ssr.SsrSettings;
import com.vinlanx.luxium.client.water.WaterSurfaceRenderer;
import org.jetbrains.annotations.Nullable;

public final class ScreenSpaceReflectionSystem {
    private static final SsrHitBuffer WATER_HITS = new SsrHitBuffer();

    private ScreenSpaceReflectionSystem() {
    }

    public static SsrSettings settings(SsrConsumer consumer) {
        return switch (consumer) {
            default -> throw new IncompatibleClassChangeError();
            case SsrConsumer.WATER -> ScreenSpaceReflectionSystem.waterSettings();
            case SsrConsumer.PUDDLE -> ScreenSpaceReflectionSystem.puddleSettings();
            case SsrConsumer.WET -> ScreenSpaceReflectionSystem.wetSettings();
        };
    }

    public static void resolveLateFrame() {
        WaterSurfaceRenderer.renderLateReflections();
    }

    public static boolean beginHitTrace(SsrConsumer consumer, int framebufferId, int width, int height) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        return buffer != null && buffer.beginCapture(framebufferId, width, height);
    }

    public static void endHitTrace(SsrConsumer consumer, boolean successful) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        if (buffer != null) {
            buffer.endCapture(successful);
        }
    }

    public static boolean isHitTraceActive(SsrConsumer consumer) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        return buffer != null && buffer.captureActive();
    }

    public static boolean isHitReady(SsrConsumer consumer) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        return buffer != null && buffer.ready();
    }

    public static int hitTexture(SsrConsumer consumer) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        return buffer != null ? buffer.textureId() : -1;
    }

    public static void invalidateHit(SsrConsumer consumer) {
        SsrHitBuffer buffer = ScreenSpaceReflectionSystem.hitBuffer(consumer);
        if (buffer != null) {
            buffer.invalidate();
        }
    }

    @Nullable
    private static SsrHitBuffer hitBuffer(SsrConsumer consumer) {
        return consumer == SsrConsumer.WATER ? WATER_HITS : null;
    }

    private static SsrSettings waterSettings() {
        if (!Config.isFeatureEnabled(Config.CLIENT.waterEnabled) || !Config.isFeatureEnabled(Config.CLIENT.waterSsrEnabled)) {
            return SsrSettings.disabled();
        }
        int[] budget = ScreenSpaceReflectionSystem.qualityBudget();
        return new SsrSettings(true, budget[0], budget[1], ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.waterSsrMaxDistance.get()).floatValue(), 4.0f, 128.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.waterSsrThickness.get()).floatValue(), 0.02f, 2.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.waterSsrEdgeFade.get()).floatValue(), 0.01f, 0.4f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.waterSsrStrength.get()).floatValue(), 0.0f, 2.0f));
    }

    private static SsrSettings puddleSettings() {
        if (!Config.isFeatureEnabled(Config.CLIENT.puddleSsrEnabled)) {
            return SsrSettings.disabled();
        }
        int[] budget = ScreenSpaceReflectionSystem.qualityBudget();
        return new SsrSettings(true, budget[0], budget[1], ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.puddleSsrMaxDistance.get()).floatValue(), 4.0f, 96.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.puddleSsrThickness.get()).floatValue(), 0.02f, 2.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.puddleSsrEdgeFade.get()).floatValue(), 0.01f, 0.4f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.puddleSsrStrength.get()).floatValue(), 0.0f, 2.0f));
    }

    private static SsrSettings wetSettings() {
        if (!Config.isFeatureEnabled(Config.CLIENT.wetEnabled) || !Config.isFeatureEnabled(Config.CLIENT.wetSsrEnabled)) {
            return SsrSettings.disabled();
        }
        int[] budget = ScreenSpaceReflectionSystem.qualityBudget();
        return new SsrSettings(true, budget[0], budget[1], ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.wetSsrMaxDistance.get()).floatValue(), 4.0f, 96.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.wetSsrThickness.get()).floatValue(), 0.02f, 2.0f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.wetSsrEdgeFade.get()).floatValue(), 0.01f, 0.4f), ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.wetSsrStrength.get()).floatValue(), 0.0f, 2.0f));
    }

    private static int[] qualityBudget() {
        float quality = ScreenSpaceReflectionSystem.clamp(((Double)Config.CLIENT.ssrQuality.get()).floatValue(), 0.25f, 1.0f);
        int coarseSteps = 5 + Math.round(quality * 11.0f);
        int refinementSteps = quality < 0.6f ? 1 : (quality < 0.9f ? 2 : 3);
        return new int[]{coarseSteps, refinementSteps};
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

