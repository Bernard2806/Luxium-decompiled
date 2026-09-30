/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.shaders.Uniform
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package com.vinlanx.luxium.client.posteffects;

import com.mojang.blaze3d.shaders.Uniform;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.posteffects.lensflare;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class skygodrays {
    private static final float SUN_QUAD_CENTER_Y = 100.0f;
    private static final float MOON_QUAD_CENTER_Y = -100.0f;
    @Nullable
    private static CelestialScreenPos capturedCelestial;

    private skygodrays() {
    }

    public static void beginSkyCapture() {
        capturedCelestial = null;
    }

    public static void captureCelestial(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, boolean useMoon, float partialTick) {
        if (!skygodrays.isEnabled() && !lensflare.isEnabled()) {
            return;
        }
        CelestialScreenPos celestial = skygodrays.projectCelestialToScreen(modelViewMatrix, projectionMatrix, useMoon, partialTick);
        if (celestial != null) {
            capturedCelestial = celestial;
        }
    }

    static boolean isEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.skyGodRaysEnabled);
    }

    static boolean hasVisibleCelestial() {
        return capturedCelestial != null;
    }

    @Nullable
    static ShaderInstance getShader() {
        return ShaderManager.getSkyGodRaysShader();
    }

    static void configure(ShaderInstance shader, int width, int height, int depthTexture) {
        skygodrays.configure(shader, width, height, depthTexture, 0);
    }

    static void configure(ShaderInstance shader, int width, int height, int depthTexture, int cloudOcclusionTexture) {
        CelestialScreenPos celestial = capturedCelestial;
        if (celestial == null) {
            return;
        }
        skygodrays.configureCelestialSamplers(shader, depthTexture, cloudOcclusionTexture);
        skygodrays.setUniform2f(shader, "SunScreenPos", celestial.screenX, celestial.screenY);
        skygodrays.setUniform3f(shader, "LightColor", celestial.color.x, celestial.color.y, celestial.color.z);
        skygodrays.setUniform1f(shader, "ScreenFade", celestial.fade);
        skygodrays.setUniform1f(shader, "UseMoonStyle", celestial.useMoonStyle ? 1.0f : 0.0f);
        skygodrays.setUniform2f(shader, "ScreenSize", width, height);
        skygodrays.setUniform1f(shader, "RayStrength", celestial.rayStrength);
        skygodrays.setUniform1f(shader, "HaloStrength", celestial.haloStrength);
        skygodrays.setUniform1f(shader, "DiscStrength", celestial.discStrength);
        skygodrays.setUniform1f(shader, "HaloRadius", celestial.haloRadius);
        skygodrays.setUniform1f(shader, "DiscRadius", celestial.discRadius);
        skygodrays.setUniform1f(shader, "CenterSuppression", celestial.centerSuppression);
    }

    static void configureCelestialUniforms(ShaderInstance shader, int width, int height, int depthTexture) {
        skygodrays.configureCelestialUniforms(shader, width, height, depthTexture, 0);
    }

    static void configureCelestialUniforms(ShaderInstance shader, int width, int height, int depthTexture, int cloudOcclusionTexture) {
        CelestialScreenPos celestial = capturedCelestial;
        if (celestial == null) {
            return;
        }
        skygodrays.configureCelestialSamplers(shader, depthTexture, cloudOcclusionTexture);
        skygodrays.setUniform2f(shader, "SunScreenPos", celestial.screenX, celestial.screenY);
        skygodrays.setUniform3f(shader, "LightColor", celestial.color.x, celestial.color.y, celestial.color.z);
        skygodrays.setUniform1f(shader, "ScreenFade", celestial.fade);
        skygodrays.setUniform1f(shader, "UseMoonStyle", celestial.useMoonStyle ? 1.0f : 0.0f);
        skygodrays.setUniform2f(shader, "ScreenSize", width, height);
    }

    private static void configureCelestialSamplers(ShaderInstance shader, int depthTexture, int cloudOcclusionTexture) {
        shader.m_173350_("DepthSampler", (Object)depthTexture);
        shader.m_173350_("CloudOcclusionSampler", (Object)(cloudOcclusionTexture != 0 ? cloudOcclusionTexture : depthTexture));
        skygodrays.setUniform1f(shader, "UseCloudOcclusion", cloudOcclusionTexture != 0 ? 1.0f : 0.0f);
    }

    @Nullable
    private static CelestialScreenPos projectCelestialToScreen(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, boolean useMoon, float partialTick) {
        Vector4f clip = new Vector4f(0.0f, useMoon ? -100.0f : 100.0f, 0.0f, 1.0f);
        clip.mul((Matrix4fc)modelViewMatrix);
        clip.mul((Matrix4fc)projectionMatrix);
        if (clip.w <= 0.0f) {
            return null;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float screenX = (ndcX + 1.0f) * 0.5f;
        float screenY = (ndcY + 1.0f) * 0.5f;
        if (screenX < -0.25f || screenX > 1.25f || screenY < -0.25f || screenY > 1.25f) {
            return null;
        }
        float distFromCenter = (float)Math.sqrt(ndcX * ndcX + ndcY * ndcY);
        float fade = 1.0f - Mth.m_14036_((float)((distFromCenter - 0.8f) / 0.5f), (float)0.0f, (float)1.0f);
        CelestialLook look = skygodrays.buildCelestialLook(useMoon, partialTick);
        if ((fade *= look.horizonFade) <= 0.0f) {
            return null;
        }
        return new CelestialScreenPos(screenX, screenY, fade, look.color, look.rayStrength, look.haloStrength, look.discStrength, look.haloRadius, look.discRadius, look.centerSuppression, useMoon);
    }

    private static CelestialLook buildCelestialLook(boolean useMoon, float partialTick) {
        Minecraft mc = Minecraft.m_91087_();
        CelestialPath.State celestialPath = NeoSkyFrameCache.celestial(mc.f_91073_, partialTick);
        float elevation = (float)celestialPath.elevation(useMoon);
        float visible = skygodrays.smoothstep(0.0f, 0.12f, elevation);
        float lowAngle = (1.0f - skygodrays.smoothstep(0.1f, 0.55f, elevation)) * visible;
        float golden = (1.0f - Mth.m_14036_((float)(Math.abs(elevation - 0.08f) / 0.32f), (float)0.0f, (float)1.0f)) * visible;
        float high = skygodrays.smoothstep(0.25f, 0.85f, elevation);
        float rain = mc.f_91073_ != null ? mc.f_91073_.m_46722_(partialTick) : 0.0f;
        float thunder = mc.f_91073_ != null ? mc.f_91073_.m_46661_(partialTick) : 0.0f;
        float rayIntensity = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysRayIntensity.get()).floatValue(), (float)0.0f, (float)2.5f);
        float haloIntensity = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysHaloIntensity.get()).floatValue(), (float)0.0f, (float)2.5f);
        float discIntensity = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysDiscIntensity.get()).floatValue(), (float)0.0f, (float)2.0f);
        float haloSize = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysHaloSize.get()).floatValue(), (float)0.5f, (float)2.5f);
        float discSize = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysDiscSize.get()).floatValue(), (float)0.5f, (float)1.8f);
        float sunSizeSlider = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysSunSize.get()).floatValue(), (float)0.5f, (float)2.0f);
        float moonSizeSlider = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysMoonSize.get()).floatValue(), (float)0.5f, (float)2.0f);
        float sunSize = sunSizeSlider * sunSizeSlider * sunSizeSlider;
        float moonSize = moonSizeSlider * moonSizeSlider * moonSizeSlider;
        float weatherInfluence = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysWeatherInfluence.get()).floatValue(), (float)0.0f, (float)2.0f);
        float centerSuppression = Mth.m_14036_((float)((Double)Config.CLIENT.skyGodRaysCenterSuppression.get()).floatValue(), (float)0.0f, (float)2.0f);
        float weatherOcclusion = Mth.m_14036_((float)(1.0f - (rain * 0.55f + thunder * 0.25f) * weatherInfluence), (float)0.2f, (float)1.0f);
        float haze = Mth.m_14036_((float)((rain * 0.65f + thunder * 0.35f) * weatherInfluence), (float)0.0f, (float)1.0f);
        boolean discEnabled = Config.isFeatureEnabled(Config.CLIENT.skyGodRaysCelestialDiscEnabled);
        boolean haloEnabled = Config.isFeatureEnabled(Config.CLIENT.skyGodRaysHaloEnabled);
        Vector3f sunDayColor = skygodrays.colorFromConfig((Integer)Config.CLIENT.skyGodRaysSunDayColor.get());
        Vector3f sunHorizonColor = skygodrays.colorFromConfig((Integer)Config.CLIENT.skyGodRaysSunsetColor.get());
        Vector3f moonBaseColor = skygodrays.colorFromConfig((Integer)Config.CLIENT.skyGodRaysMoonBaseColor.get());
        Vector3f moonHaloColor = skygodrays.colorFromConfig((Integer)Config.CLIENT.skyGodRaysMoonHaloColor.get());
        if (useMoon) {
            Vector3f color = skygodrays.mix(moonBaseColor, moonHaloColor, 0.25f * lowAngle);
            float rayStrength = (0.05f + 0.11f * lowAngle + 0.06f * haze) * weatherOcclusion * visible * rayIntensity;
            float haloStrength = haloEnabled ? (0.08f + 0.1f * lowAngle + 0.05f * haze) * visible * haloIntensity : 0.0f;
            float discStrength = discEnabled ? 0.035f * visible * discIntensity : 0.0f;
            float haloRadius = (0.033f + 0.017f * lowAngle + 0.01f * haze) * haloSize * moonSize;
            return new CelestialLook(color, visible, rayStrength, haloStrength, discStrength, haloRadius, 0.0067f * discSize * moonSize, centerSuppression);
        }
        Vector3f color = skygodrays.mix(sunHorizonColor, sunDayColor, high);
        color = skygodrays.mix(color, sunHorizonColor, golden * 0.55f);
        float rayStrength = (0.55f + 1.15f * golden + 0.34f * haze) * weatherOcclusion * visible * rayIntensity;
        float haloStrength = haloEnabled ? (0.34f + 0.56f * lowAngle + 0.22f * haze) * weatherOcclusion * visible * haloIntensity : 0.0f;
        float discStrength = discEnabled ? (0.12f + 0.06f * high) * visible * discIntensity : 0.0f;
        float haloRadius = (0.047f + 0.04f * lowAngle + 0.017f * haze) * haloSize * sunSize;
        return new CelestialLook(color, visible, rayStrength, haloStrength, discStrength, haloRadius, 0.0133f * discSize * sunSize, centerSuppression);
    }

    private static Vector3f colorFromConfig(int rgb) {
        return new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0f, (float)(rgb >> 8 & 0xFF) / 255.0f, (float)(rgb & 0xFF) / 255.0f);
    }

    private static Vector3f mix(Vector3f a, Vector3f b, float t) {
        float f = Mth.m_14036_((float)t, (float)0.0f, (float)1.0f);
        return new Vector3f(Mth.m_14179_((float)f, (float)a.x, (float)b.x), Mth.m_14179_((float)f, (float)a.y, (float)b.y), Mth.m_14179_((float)f, (float)a.z, (float)b.z));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float denominator = edge1 - edge0;
        if (Math.abs(denominator) < 1.0E-5f) {
            denominator = Math.copySign(1.0E-5f, denominator == 0.0f ? 1.0f : denominator);
        }
        float t = Mth.m_14036_((float)((value - edge0) / denominator), (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static void setUniform1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void setUniform2f(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }

    private static void setUniform3f(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private record CelestialScreenPos(float screenX, float screenY, float fade, Vector3f color, float rayStrength, float haloStrength, float discStrength, float haloRadius, float discRadius, float centerSuppression, boolean useMoonStyle) {
    }

    private record CelestialLook(Vector3f color, float horizonFade, float rayStrength, float haloStrength, float discStrength, float haloRadius, float discRadius, float centerSuppression) {
    }
}

