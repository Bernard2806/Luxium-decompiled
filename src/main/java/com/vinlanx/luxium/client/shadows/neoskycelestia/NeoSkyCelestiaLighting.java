/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class NeoSkyCelestiaLighting {
    private static final Vector3f DAY_SKY = new Vector3f(0.94f, 0.97f, 1.0f);
    private static final Vector3f SUNSET_SKY = new Vector3f(0.86f, 0.66f, 0.54f);
    private static final Vector3f NIGHT_SKY = new Vector3f(0.48f, 0.56f, 0.7f);
    private static final Vector3f DAY_GROUND = new Vector3f(0.72f, 0.7f, 0.68f);
    private static final Vector3f SUNSET_GROUND = new Vector3f(0.55f, 0.42f, 0.36f);
    private static final Vector3f NIGHT_GROUND = new Vector3f(0.3f, 0.34f, 0.44f);
    private static final float NIGHT_TO_TWILIGHT_START = -0.18f;
    private static final float NIGHT_TO_TWILIGHT_END = 0.04f;
    private static final float TWILIGHT_TO_DAY_START = 0.02f;
    private static final float TWILIGHT_TO_DAY_END = 0.32f;
    private static final float DIRECT_HORIZON_FADE_END = 0.14f;
    private static final float DIRECT_COLOR_BLEND_START = -0.08f;
    private static final float DIRECT_COLOR_BLEND_END = 0.08f;

    private NeoSkyCelestiaLighting() {
    }

    public static State calculate(ClientLevel level, float partialTick, CelestialPath.State celestialPath) {
        float globalDirectScale;
        float directBaseStrength;
        if (level == null || !level.m_6042_().f_223549_()) {
            return State.disabled();
        }
        boolean moon = celestialPath.usingMoon();
        Vec3 sunDirection = celestialPath.sunDirection();
        Vec3 activeDirection = celestialPath.activeDirection();
        float sunElevation = (float)sunDirection.f_82480_;
        float activeElevation = Math.max(0.0f, (float)activeDirection.f_82480_);
        float nightToTwilight = NeoSkyCelestiaLighting.smoothstep(-0.18f, 0.04f, sunElevation);
        float twilightToDay = NeoSkyCelestiaLighting.smoothstep(0.02f, 0.32f, sunElevation);
        float directColorBlend = NeoSkyCelestiaLighting.smoothstep(-0.08f, 0.08f, sunElevation);
        float directVisibility = NeoSkyCelestiaLighting.smoothstep(0.0f, 0.14f, activeElevation);
        float sunInfluence = NeoSkyCelestiaLighting.smoothstep(0.0f, 0.14f, sunElevation);
        float highAngle = NeoSkyCelestiaLighting.smoothstep(0.25f, 0.85f, Math.max(0.0f, sunElevation));
        float weather = Mth.m_14036_((float)(level.m_46722_(partialTick) * 0.65f + level.m_46661_(partialTick) * 0.35f), (float)0.0f, (float)1.0f);
        float weatherDirect = Mth.m_14179_((float)weather, (float)1.0f, (float)0.3f);
        float weatherAmbient = Mth.m_14179_((float)weather, (float)1.0f, (float)0.62f);
        Vector3f sunZenithColor = NeoSkyCelestiaLighting.colorFromConfig((Integer)Config.CLIENT.skyLightSunZenithColor.get());
        Vector3f sunsetColor = NeoSkyCelestiaLighting.colorFromConfig((Integer)Config.CLIENT.skyLightSunsetColor.get());
        Vector3f moonColor = NeoSkyCelestiaLighting.colorFromConfig((Integer)Config.CLIENT.skyLightMoonColor.get());
        Vector3f sunDirectColor = NeoSkyCelestiaLighting.mix(sunsetColor, sunZenithColor, highAngle);
        Vector3f directColor = NeoSkyCelestiaLighting.mix(moonColor, sunDirectColor, directColorBlend);
        Vector3f skyAmbient = NeoSkyCelestiaLighting.mix(NeoSkyCelestiaLighting.mix(NIGHT_SKY, SUNSET_SKY, nightToTwilight), DAY_SKY, twilightToDay);
        Vector3f groundAmbient = NeoSkyCelestiaLighting.mix(NeoSkyCelestiaLighting.mix(NIGHT_GROUND, SUNSET_GROUND, nightToTwilight), DAY_GROUND, twilightToDay);
        if (moon) {
            directBaseStrength = ((Double)Config.CLIENT.skyLightMoonBaseStrength.get()).floatValue() * 0.01f;
            globalDirectScale = ((Double)Config.CLIENT.skyLightMoonStrength.get()).floatValue() * 0.01f;
        } else {
            float sunsetBaseStrength = ((Double)Config.CLIENT.skyLightSunsetStrength.get()).floatValue() * 0.01f;
            float zenithBaseStrength = ((Double)Config.CLIENT.skyLightSunZenithStrength.get()).floatValue() * 0.01f;
            directBaseStrength = Mth.m_14179_((float)highAngle, (float)sunsetBaseStrength, (float)zenithBaseStrength);
            globalDirectScale = ((Double)Config.CLIENT.skyLightSunStrength.get()).floatValue() * 0.01f;
        }
        float directStrength = directBaseStrength * directVisibility * weatherDirect * globalDirectScale;
        float twilightAmbientStrength = Mth.m_14179_((float)nightToTwilight, (float)0.72f, (float)0.82f);
        float dayAmbientStrength = Mth.m_14179_((float)highAngle, (float)0.82f, (float)0.98f);
        float ambientStrength = Mth.m_14179_((float)twilightToDay, (float)twilightAmbientStrength, (float)dayAmbientStrength) * weatherAmbient * ((Double)Config.CLIENT.skyLightAmbientStrength.get()).floatValue() * 0.01f;
        boolean colorsEnabled = Config.isFeatureEnabled(Config.CLIENT.skyLightColorsEnabled);
        if (!colorsEnabled) {
            directColor.set(1.0f);
            skyAmbient.set(1.0f);
            groundAmbient.set(1.0f);
        }
        Vector3f shadowSkyAmbient = new Vector3f((Vector3fc)skyAmbient);
        Vector3f shadowGroundAmbient = new Vector3f((Vector3fc)groundAmbient);
        if (colorsEnabled && Config.isFeatureEnabled(Config.CLIENT.realisticShadowTemperature)) {
            float temperatureStrength = ((Double)Config.CLIENT.shadowTemperatureStrength.get()).floatValue();
            int temperatureBias = (Integer)Config.CLIENT.shadowTemperatureBias.get();
            shadowSkyAmbient = NeoSkyCelestiaLighting.applyShadowTemperature(shadowSkyAmbient, directColor, temperatureStrength, temperatureBias);
            shadowGroundAmbient = NeoSkyCelestiaLighting.applyShadowTemperature(shadowGroundAmbient, directColor, temperatureStrength, temperatureBias);
        }
        return new State(Config.isFeatureEnabled(Config.CLIENT.skyLightEnabled), moon, new Vector3f((float)activeDirection.f_82479_, (float)activeDirection.f_82480_, (float)activeDirection.f_82481_).normalize(), directColor, skyAmbient, groundAmbient, shadowSkyAmbient, shadowGroundAmbient, directStrength, ambientStrength, directVisibility, sunInfluence);
    }

    private static Vector3f applyShadowTemperature(Vector3f ambient, Vector3f directColor, float strength, int bias) {
        float amount = Mth.m_14036_((float)strength, (float)0.0f, (float)1.0f);
        float manualShift = Mth.m_14036_((float)((float)bias / 100.0f), (float)-1.0f, (float)1.0f) * 0.55f;
        if (amount <= 0.0f && Math.abs(manualShift) <= 1.0E-5f) {
            return new Vector3f((Vector3fc)ambient);
        }
        float warmth = (float)(Math.log((directColor.x + 0.05f) / (directColor.z + 0.05f)) / Math.log(5.0));
        warmth = Mth.m_14036_((float)warmth, (float)-1.0f, (float)1.0f);
        float shift = warmth * amount * 0.38f + manualShift;
        float redScale = (float)Math.exp(-shift);
        float blueScale = (float)Math.exp(shift);
        Vector3f shifted = new Vector3f(ambient.x * redScale, ambient.y, ambient.z * blueScale);
        float sourceLuminance = NeoSkyCelestiaLighting.luminance(ambient);
        float shiftedLuminance = NeoSkyCelestiaLighting.luminance(shifted);
        if (shiftedLuminance > 1.0E-5f) {
            shifted.mul(sourceLuminance / shiftedLuminance);
        }
        shifted.x = Mth.m_14036_((float)shifted.x, (float)0.0f, (float)1.25f);
        shifted.y = Mth.m_14036_((float)shifted.y, (float)0.0f, (float)1.25f);
        shifted.z = Mth.m_14036_((float)shifted.z, (float)0.0f, (float)1.25f);
        return shifted;
    }

    private static float luminance(Vector3f color) {
        return Math.max(color.x, 0.0f) * 0.2126f + Math.max(color.y, 0.0f) * 0.7152f + Math.max(color.z, 0.0f) * 0.0722f;
    }

    private static Vector3f mix(Vector3f a, Vector3f b, float value) {
        float t = Mth.m_14036_((float)value, (float)0.0f, (float)1.0f);
        return new Vector3f(Mth.m_14179_((float)t, (float)a.x, (float)b.x), Mth.m_14179_((float)t, (float)a.y, (float)b.y), Mth.m_14179_((float)t, (float)a.z, (float)b.z));
    }

    private static Vector3f colorFromConfig(int rgb) {
        return new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0f, (float)(rgb >> 8 & 0xFF) / 255.0f, (float)(rgb & 0xFF) / 255.0f);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.m_14036_((float)((value - edge0) / Math.max(edge1 - edge0, 1.0E-5f)), (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public record State(boolean enabled, boolean moon, Vector3f direction, Vector3f directColor, Vector3f skyAmbientColor, Vector3f groundAmbientColor, Vector3f shadowSkyAmbientColor, Vector3f shadowGroundAmbientColor, float directStrength, float ambientStrength, float visibility, float sunInfluence) {
        public static State disabled() {
            return new State(false, false, new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(1.0f), new Vector3f(1.0f), new Vector3f(1.0f), new Vector3f(1.0f), new Vector3f(1.0f), 0.0f, 1.0f, 0.0f, 0.0f);
        }
    }
}

