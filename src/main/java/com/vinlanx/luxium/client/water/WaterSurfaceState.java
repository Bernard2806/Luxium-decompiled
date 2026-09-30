/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.vinlanx.luxium.client.water;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class WaterSurfaceState {
    private static boolean materialPassActive;
    private static boolean resolvePassActive;
    private static boolean lateReflectionPassActive;
    private static boolean lateResolvePassActive;
    private static boolean scaledFrameReady;
    private static boolean lateReflectionReady;
    private static long lastFrameNanos;

    private WaterSurfaceState() {
    }

    public static boolean enabled() {
        return Config.isFeatureEnabled(Config.CLIENT.waterEnabled);
    }

    public static boolean canRenderMainPass() {
        if (!WaterSurfaceState.enabled() || ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return false;
        }
        Minecraft mc = Minecraft.m_91087_();
        return mc.f_91073_ != null && mc.m_91385_().m_83975_() > 0 && mc.m_91385_().m_83980_() > 0;
    }

    public static void beginFrame() {
        materialPassActive = false;
        resolvePassActive = false;
        lateReflectionPassActive = false;
        lateResolvePassActive = false;
        scaledFrameReady = false;
        lateReflectionReady = false;
        lastFrameNanos = System.nanoTime();
    }

    static void beginMaterialPass() {
        materialPassActive = true;
        resolvePassActive = false;
        scaledFrameReady = false;
    }

    static void finishMaterialPass(boolean ready) {
        materialPassActive = false;
        scaledFrameReady = ready;
    }

    static void beginResolvePass() {
        resolvePassActive = true;
    }

    static void finishResolvePass() {
        resolvePassActive = false;
        scaledFrameReady = false;
    }

    static void beginLateReflectionPass() {
        materialPassActive = false;
        resolvePassActive = false;
        lateResolvePassActive = false;
        lateReflectionPassActive = true;
        lateReflectionReady = false;
    }

    static void finishLateReflectionPass(boolean ready) {
        lateReflectionPassActive = false;
        lateReflectionReady = ready;
    }

    static void beginLateResolvePass() {
        lateResolvePassActive = lateReflectionReady;
    }

    static void finishLateResolvePass() {
        lateResolvePassActive = false;
        lateReflectionReady = false;
    }

    public static int renderMode() {
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return 0;
        }
        if (materialPassActive) {
            return 1;
        }
        if (resolvePassActive && scaledFrameReady) {
            return 2;
        }
        if (lateReflectionPassActive) {
            return 3;
        }
        if (lateResolvePassActive && lateReflectionReady) {
            return 4;
        }
        return 0;
    }

    public static boolean isScaledPassActive() {
        return materialPassActive;
    }

    public static boolean isResolvePassActive() {
        return resolvePassActive;
    }

    public static boolean isLateReflectionPassActive() {
        return lateReflectionPassActive;
    }

    public static boolean isLateResolvePassActive() {
        return lateResolvePassActive;
    }

    public static boolean sceneReady() {
        Minecraft mc = Minecraft.m_91087_();
        return WaterSurfaceState.canRenderMainPass() && mc.m_91385_().m_83975_() > 0 && mc.m_91385_().m_83980_() > 0;
    }

    public static boolean resolvedReady() {
        return scaledFrameReady && SharedPostResources.getWaterRenderColorTextureId() > 0;
    }

    public static boolean lateReflectionResolvedReady() {
        return lateReflectionReady && SharedPostResources.getWaterRenderColorTextureId() > 0;
    }

    public static float timeSeconds() {
        long nanos = lastFrameNanos != 0L ? lastFrameNanos : System.nanoTime();
        return (float)((double)nanos * 1.0E-9 % 4096.0);
    }

    public static Vec3 cameraPosition() {
        return Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
    }

    public static Light light(float partialTick) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null || !level.m_6042_().f_223549_()) {
            return new Light(new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f), 0.0f);
        }
        CelestialPath.State path = NeoSkyFrameCache.celestial(level, partialTick);
        Vec3 direction = path.activeDirection();
        float rain = level.m_46722_(partialTick);
        float thunder = level.m_46661_(partialTick);
        float weather = Math.max(0.18f, 1.0f - rain * 0.62f - thunder * 0.25f);
        float elevation = Math.max(0.0f, (float)direction.f_82480_);
        float horizonVisibility = WaterSurfaceState.smoothstep(0.0f, 0.14f, elevation);
        float sunElevation = (float)path.sunDirection().f_82480_;
        float sunColorBlend = WaterSurfaceState.smoothstep(-0.08f, 0.08f, sunElevation);
        float sunLow = 1.0f - Math.min(1.0f, Math.max(0.0f, sunElevation) * 2.2f);
        Vector3f moonColor = new Vector3f(0.48f, 0.58f, 0.82f);
        Vector3f sunColor = new Vector3f(1.0f, 0.94f - 0.26f * sunLow, 0.86f - 0.43f * sunLow);
        Vector3f color = WaterSurfaceState.mix(moonColor, sunColor, sunColorBlend);
        float strength = path.usingMoon() ? 0.24f * weather * horizonVisibility : weather * (0.48f + 0.52f * elevation) * horizonVisibility;
        return new Light(new Vector3f((float)direction.f_82479_, (float)direction.f_82480_, (float)direction.f_82481_).normalize(), color, strength);
    }

    private static Vector3f mix(Vector3f a, Vector3f b, float value) {
        float t = Math.max(0.0f, Math.min(1.0f, value));
        return new Vector3f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float denominator = Math.max(edge1 - edge0, 1.0E-5f);
        float t = Math.max(0.0f, Math.min(1.0f, (value - edge0) / denominator));
        return t * t * (3.0f - 2.0f * t);
    }

    public static long lastFrameNanos() {
        return lastFrameNanos;
    }

    public record Light(Vector3f direction, Vector3f color, float strength) {
    }
}

