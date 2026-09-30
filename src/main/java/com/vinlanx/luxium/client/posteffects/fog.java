/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.shaders.Uniform
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 */
package com.vinlanx.luxium.client.posteffects;

import com.mojang.blaze3d.shaders.Uniform;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaLighting;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class fog {
    private static final float FOG_HEIGHT = 72.0f;
    private static final Matrix4f INVERSE_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_VIEW = new Matrix4f();
    private static final Vector4f VIEW_ORIGIN = new Vector4f();
    private static final FogConfigState CONFIG_STATE = new FogConfigState();
    private static final FogFrameState FRAME_STATE = new FogFrameState();
    private static boolean configDirty = true;
    private static long frameSerial = Long.MIN_VALUE;
    private static FogUniforms uniformSlot0;
    private static FogUniforms uniformSlot1;

    private fog() {
    }

    public static void onConfigChanged() {
        configDirty = true;
        frameSerial = Long.MIN_VALUE;
    }

    static boolean isEnabled() {
        FogConfigState config = fog.config();
        return config.renderFog || config.celestialScatteringEnabled;
    }

    @Nullable
    static ShaderInstance getShader() {
        return ShaderManager.getFogShader();
    }

    static void configure(ShaderInstance shader, RenderLevelStageEvent event, Minecraft mc, int depthTexture) {
        shader.m_173350_("DepthSampler", (Object)depthTexture);
        FogUniforms uniforms = fog.uniforms(shader);
        FogFrameState state = fog.frameState(event, mc);
        fog.setMatrix(uniforms.inverseProjMat, INVERSE_PROJECTION);
        fog.setMatrix(uniforms.inverseViewMat, INVERSE_VIEW);
        fog.set3f(uniforms.fogSunDir, state.sunX, state.sunY, state.sunZ);
        fog.set1i(uniforms.fogEnabled, state.renderFog ? 1 : 0);
        fog.set1i(uniforms.celestialScatteringEnabled, state.celestialScatteringEnabled ? 1 : 0);
        fog.set1f(uniforms.fogDensityAtOrigin, state.densityAtOrigin);
        fog.set1f(uniforms.fogExtinction, state.densitySetting);
        fog.set1f(uniforms.fogScatteringBrightness, state.scatteringBrightness);
        fog.set1f(uniforms.fogMaxBrightness, state.maxBrightness);
        fog.set1f(uniforms.fogNearDensityBoost, state.nearDensityBoost);
        fog.set1f(uniforms.fogNearBoostRange, state.nearBoostRange);
        fog.set1i(uniforms.fogDynamicCelestialColor, state.dynamicCelestialColor ? 1 : 0);
        fog.set1f(uniforms.fogCelestialColorBlend, state.celestialColorBlend);
        fog.set3f(uniforms.fogCelestialColor, state.celestialFogR, state.celestialFogG, state.celestialFogB);
        fog.set1f(uniforms.fogStartDistance, state.startDistance);
        fog.set1f(uniforms.fogNearFade, state.nearFade);
        fog.set1f(uniforms.fogMaxOpacity, state.maxOpacity);
        fog.set1f(uniforms.fogSkyTint, state.skyTint);
        fog.set1f(uniforms.fogScatteringStrength, state.scatteringStrength);
        fog.set1f(uniforms.fogDistanceCurve, state.distanceCurve);
        fog.set1f(uniforms.fogHeightFalloff, state.heightFalloff);
        fog.set3f(uniforms.fogAmbientColor, state.ambientR, state.ambientG, state.ambientB);
        fog.set3f(uniforms.fogSkyColor, state.skyR, state.skyG, state.skyB);
        fog.set3f(uniforms.fogSunColor, state.sunR, state.sunG, state.sunB);
        fog.set3f(uniforms.fogMoonColor, state.moonR, state.moonG, state.moonB);
        fog.set4f(uniforms.fogMieParams, 0.86f, 0.94f, 0.82f, 0.91f);
        fog.set1f(uniforms.fogSunVisibility, state.sunFogVisibility);
        fog.set1f(uniforms.fogMoonVisibility, state.moonFogVisibility);
    }

    private static FogConfigState config() {
        if (configDirty) {
            CONFIG_STATE.refresh();
            configDirty = false;
        }
        return CONFIG_STATE;
    }

    private static FogFrameState frameState(RenderLevelStageEvent event, Minecraft mc) {
        long currentFrame = NeoSkyFrameCache.frameSerial();
        if (frameSerial != currentFrame) {
            FRAME_STATE.update(event, mc, fog.config());
            frameSerial = currentFrame;
        }
        return FRAME_STATE;
    }

    private static FogUniforms uniforms(ShaderInstance shader) {
        if (uniformSlot0 != null && fog.uniformSlot0.shader == shader) {
            return uniformSlot0;
        }
        if (uniformSlot1 != null && fog.uniformSlot1.shader == shader) {
            return uniformSlot1;
        }
        FogUniforms resolved = new FogUniforms(shader);
        if (uniformSlot0 == null) {
            uniformSlot0 = resolved;
        } else if (uniformSlot1 == null) {
            uniformSlot1 = resolved;
        } else {
            uniformSlot0 = uniformSlot1;
            uniformSlot1 = resolved;
        }
        return resolved;
    }

    private static float lerp(float a, float b, float t) {
        return Mth.m_14179_((float)Mth.m_14036_((float)t, (float)0.0f, (float)1.0f), (float)a, (float)b);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float denominator = edge1 - edge0;
        if (Math.abs(denominator) < 1.0E-5f) {
            denominator = Math.copySign(1.0E-5f, denominator == 0.0f ? 1.0f : denominator);
        }
        float t = Mth.m_14036_((float)((value - edge0) / denominator), (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static void set1i(Uniform uniform, int value) {
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    private static void set1f(Uniform uniform, float value) {
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void set3f(Uniform uniform, float x, float y, float z) {
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private static void set4f(Uniform uniform, float x, float y, float z, float w) {
        if (uniform != null) {
            uniform.m_5805_(x, y, z, w);
        }
    }

    private static void setMatrix(Uniform uniform, Matrix4f matrix) {
        if (uniform != null) {
            uniform.m_5679_(matrix);
        }
    }

    private static final class FogConfigState {
        private boolean renderFog;
        private boolean celestialScatteringEnabled;
        private boolean dynamicCelestialColor;
        private float densitySetting;
        private float scatteringBrightness;
        private float maxBrightness;
        private float nearDensityBoost;
        private float nearBoostRange;
        private float celestialColorBlend;
        private float startDistance;
        private float nearFade;
        private float maxOpacity;
        private float skyTint;
        private float scatteringStrength;
        private float distanceCurve;
        private float heightFalloff;

        private FogConfigState() {
        }

        private void refresh() {
            this.renderFog = Config.isFeatureEnabled(Config.CLIENT.fogEnabled);
            this.celestialScatteringEnabled = Config.isFeatureEnabled(Config.CLIENT.fogCelestialScatteringEnabled);
            this.dynamicCelestialColor = Config.isFeatureEnabled(Config.CLIENT.fogDynamicCelestialColor);
            this.densitySetting = Mth.m_14036_((float)((Double)Config.CLIENT.fogDensity.get()).floatValue(), (float)0.0f, (float)20.0f);
            this.scatteringBrightness = Mth.m_14036_((float)((Double)Config.CLIENT.fogScatteringBrightness.get()).floatValue(), (float)0.25f, (float)1.2f);
            this.maxBrightness = Mth.m_14036_((float)((Double)Config.CLIENT.fogMaxBrightness.get()).floatValue(), (float)0.15f, (float)1.2f);
            this.nearDensityBoost = Mth.m_14036_((float)((Double)Config.CLIENT.fogNearDensityBoost.get()).floatValue(), (float)0.0f, (float)5.0f);
            this.nearBoostRange = Mth.m_14036_((float)((Double)Config.CLIENT.fogNearBoostRange.get()).floatValue(), (float)4.0f, (float)128.0f);
            this.celestialColorBlend = Mth.m_14036_((float)((Double)Config.CLIENT.fogCelestialColorBlend.get()).floatValue(), (float)0.0f, (float)1.0f);
            this.startDistance = Mth.m_14036_((float)((Double)Config.CLIENT.fogStartDistance.get()).floatValue(), (float)0.0f, (float)160.0f);
            this.nearFade = Mth.m_14036_((float)((Double)Config.CLIENT.fogNearFade.get()).floatValue(), (float)0.0f, (float)64.0f);
            this.maxOpacity = Mth.m_14036_((float)((Double)Config.CLIENT.fogMaxOpacity.get()).floatValue(), (float)0.0f, (float)1.0f);
            this.skyTint = Mth.m_14036_((float)((Double)Config.CLIENT.fogSkyTint.get()).floatValue(), (float)0.0f, (float)1.0f);
            this.scatteringStrength = Mth.m_14036_((float)((Double)Config.CLIENT.fogScatteringStrength.get()).floatValue(), (float)0.0f, (float)5.0f);
            this.distanceCurve = Mth.m_14036_((float)((Double)Config.CLIENT.fogDistanceCurve.get()).floatValue(), (float)0.35f, (float)3.0f);
            float fogHeight = Mth.m_14036_((float)((Double)Config.CLIENT.fogHeight.get()).floatValue(), (float)50.0f, (float)384.0f);
            this.heightFalloff = 4.0f / fogHeight;
        }
    }

    private static final class FogUniforms {
        private final ShaderInstance shader;
        private final Uniform inverseProjMat;
        private final Uniform inverseViewMat;
        private final Uniform fogSunDir;
        private final Uniform fogEnabled;
        private final Uniform celestialScatteringEnabled;
        private final Uniform fogDensityAtOrigin;
        private final Uniform fogExtinction;
        private final Uniform fogScatteringBrightness;
        private final Uniform fogMaxBrightness;
        private final Uniform fogNearDensityBoost;
        private final Uniform fogNearBoostRange;
        private final Uniform fogDynamicCelestialColor;
        private final Uniform fogCelestialColorBlend;
        private final Uniform fogCelestialColor;
        private final Uniform fogStartDistance;
        private final Uniform fogNearFade;
        private final Uniform fogMaxOpacity;
        private final Uniform fogSkyTint;
        private final Uniform fogScatteringStrength;
        private final Uniform fogDistanceCurve;
        private final Uniform fogHeightFalloff;
        private final Uniform fogAmbientColor;
        private final Uniform fogSkyColor;
        private final Uniform fogSunColor;
        private final Uniform fogMoonColor;
        private final Uniform fogMieParams;
        private final Uniform fogSunVisibility;
        private final Uniform fogMoonVisibility;

        private FogUniforms(ShaderInstance shader) {
            this.shader = shader;
            this.inverseProjMat = shader.m_173348_("InverseProjMat");
            this.inverseViewMat = shader.m_173348_("InverseViewMat");
            this.fogSunDir = shader.m_173348_("FogSunDir");
            this.fogEnabled = shader.m_173348_("FogEnabled");
            this.celestialScatteringEnabled = shader.m_173348_("CelestialScatteringEnabled");
            this.fogDensityAtOrigin = shader.m_173348_("FogDensityAtOrigin");
            this.fogExtinction = shader.m_173348_("FogExtinction");
            this.fogScatteringBrightness = shader.m_173348_("FogScatteringBrightness");
            this.fogMaxBrightness = shader.m_173348_("FogMaxBrightness");
            this.fogNearDensityBoost = shader.m_173348_("FogNearDensityBoost");
            this.fogNearBoostRange = shader.m_173348_("FogNearBoostRange");
            this.fogDynamicCelestialColor = shader.m_173348_("FogDynamicCelestialColor");
            this.fogCelestialColorBlend = shader.m_173348_("FogCelestialColorBlend");
            this.fogCelestialColor = shader.m_173348_("FogCelestialColor");
            this.fogStartDistance = shader.m_173348_("FogStartDistance");
            this.fogNearFade = shader.m_173348_("FogNearFade");
            this.fogMaxOpacity = shader.m_173348_("FogMaxOpacity");
            this.fogSkyTint = shader.m_173348_("FogSkyTint");
            this.fogScatteringStrength = shader.m_173348_("FogScatteringStrength");
            this.fogDistanceCurve = shader.m_173348_("FogDistanceCurve");
            this.fogHeightFalloff = shader.m_173348_("FogHeightFalloff");
            this.fogAmbientColor = shader.m_173348_("FogAmbientColor");
            this.fogSkyColor = shader.m_173348_("FogSkyColor");
            this.fogSunColor = shader.m_173348_("FogSunColor");
            this.fogMoonColor = shader.m_173348_("FogMoonColor");
            this.fogMieParams = shader.m_173348_("FogMieParams");
            this.fogSunVisibility = shader.m_173348_("FogSunVisibility");
            this.fogMoonVisibility = shader.m_173348_("FogMoonVisibility");
        }
    }

    private static final class FogFrameState {
        private boolean renderFog;
        private boolean celestialScatteringEnabled;
        private boolean dynamicCelestialColor;
        private float sunX;
        private float sunY;
        private float sunZ;
        private float densityAtOrigin;
        private float densitySetting;
        private float scatteringBrightness;
        private float maxBrightness;
        private float nearDensityBoost;
        private float nearBoostRange;
        private float celestialColorBlend;
        private float celestialFogR;
        private float celestialFogG;
        private float celestialFogB;
        private float startDistance;
        private float nearFade;
        private float maxOpacity;
        private float skyTint;
        private float scatteringStrength;
        private float distanceCurve;
        private float heightFalloff;
        private float ambientR;
        private float ambientG;
        private float ambientB;
        private float skyR;
        private float skyG;
        private float skyB;
        private float sunR;
        private float sunG;
        private float sunB;
        private float moonR;
        private float moonG;
        private float moonB;
        private float sunFogVisibility;
        private float moonFogVisibility;

        private FogFrameState() {
        }

        private void update(RenderLevelStageEvent event, Minecraft mc, FogConfigState config) {
            NeoSkyFrameCache.copyInverseProjection(event.getProjectionMatrix(), INVERSE_PROJECTION);
            NeoSkyFrameCache.copyInverseView(event.getPoseStack().m_85850_().m_252922_(), INVERSE_VIEW);
            Vec3 cameraPos = event.getCamera().m_90583_();
            VIEW_ORIGIN.set(0.0f, 0.0f, 0.0f, 1.0f).mul((Matrix4fc)INVERSE_VIEW);
            float rayOriginY = (float)cameraPos.f_82480_ + fog.VIEW_ORIGIN.y;
            CelestialPath.State celestialPath = NeoSkyFrameCache.celestial(mc.f_91073_, event.getPartialTick());
            Vec3 sunDirection = celestialPath.sunDirection();
            this.sunX = (float)sunDirection.f_82479_;
            this.sunY = (float)sunDirection.f_82480_;
            this.sunZ = (float)sunDirection.f_82481_;
            float rain = mc.f_91073_ != null ? mc.f_91073_.m_46722_(event.getPartialTick()) : 0.0f;
            float thunder = mc.f_91073_ != null ? mc.f_91073_.m_46661_(event.getPartialTick()) : 0.0f;
            float weather = Mth.m_14036_((float)(rain * 0.7f + thunder * 0.55f), (float)0.0f, (float)1.0f);
            float sunsetPeak = fog.smoothstep(-0.08f, 0.06f, this.sunY) * (1.0f - fog.smoothstep(0.08f, 0.3f, this.sunY));
            float night = 1.0f - fog.smoothstep(-0.16f, 0.02f, this.sunY);
            float timeDensityMultiplier = 1.0f + sunsetPeak * 0.28f - night * 0.18f;
            float weatherDensityMultiplier = 1.0f + weather * 1.15f;
            float altitudeGate = fog.smoothstep(28.0f, 58.0f, rayOriginY);
            float baseDensity = config.renderFog ? 0.0058f * timeDensityMultiplier * weatherDensityMultiplier * altitudeGate : 0.0f;
            this.densityAtOrigin = baseDensity * (float)Math.exp(-config.heightFalloff * (rayOriginY - 72.0f));
            this.renderFog = config.renderFog;
            this.celestialScatteringEnabled = config.celestialScatteringEnabled;
            this.dynamicCelestialColor = config.dynamicCelestialColor;
            this.densitySetting = config.densitySetting;
            this.scatteringBrightness = config.scatteringBrightness;
            this.maxBrightness = config.maxBrightness;
            this.nearDensityBoost = config.nearDensityBoost;
            this.nearBoostRange = config.nearBoostRange;
            this.celestialColorBlend = config.celestialColorBlend;
            this.startDistance = config.startDistance;
            this.nearFade = config.nearFade;
            this.maxOpacity = config.maxOpacity;
            this.skyTint = config.skyTint;
            this.scatteringStrength = config.scatteringStrength;
            this.distanceCurve = config.distanceCurve;
            this.heightFalloff = config.heightFalloff;
            NeoSkyCelestiaLighting.State lighting = NeoSkyFrameCache.lighting(mc.f_91073_, event.getPartialTick());
            this.celestialFogR = Mth.m_14179_((float)0.42f, (float)lighting.skyAmbientColor().x, (float)lighting.directColor().x);
            this.celestialFogG = Mth.m_14179_((float)0.42f, (float)lighting.skyAmbientColor().y, (float)lighting.directColor().y);
            this.celestialFogB = Mth.m_14179_((float)0.42f, (float)lighting.skyAmbientColor().z, (float)lighting.directColor().z);
            float nightToTwilight = fog.smoothstep(-0.18f, 0.04f, this.sunY);
            float twilightToDay = fog.smoothstep(0.02f, 0.32f, this.sunY);
            this.ambientR = fog.lerp(0.018f, 0.22f, nightToTwilight);
            this.ambientG = fog.lerp(0.03f, 0.2f, nightToTwilight);
            this.ambientB = fog.lerp(0.06f, 0.24f, nightToTwilight);
            this.ambientR = fog.lerp(this.ambientR, 0.245f, twilightToDay);
            this.ambientG = fog.lerp(this.ambientG, 0.335f, twilightToDay);
            this.ambientB = fog.lerp(this.ambientB, 0.455f, twilightToDay);
            float ambientWeather = Mth.m_14036_((float)(weather * 0.78f), (float)0.0f, (float)1.0f);
            this.ambientR = fog.lerp(this.ambientR, 0.2f, ambientWeather);
            this.ambientG = fog.lerp(this.ambientG, 0.25f, ambientWeather);
            this.ambientB = fog.lerp(this.ambientB, 0.31f, ambientWeather);
            this.skyR = fog.lerp(0.028f, 0.42f, nightToTwilight);
            this.skyG = fog.lerp(0.055f, 0.31f, nightToTwilight);
            this.skyB = fog.lerp(0.115f, 0.34f, nightToTwilight);
            this.skyR = fog.lerp(this.skyR, 0.36f, twilightToDay);
            this.skyG = fog.lerp(this.skyG, 0.55f, twilightToDay);
            this.skyB = fog.lerp(this.skyB, 0.78f, twilightToDay);
            float skyWeather = Mth.m_14036_((float)(weather * 0.82f), (float)0.0f, (float)1.0f);
            this.skyR = fog.lerp(this.skyR, 0.25f, skyWeather);
            this.skyG = fog.lerp(this.skyG, 0.32f, skyWeather);
            this.skyB = fog.lerp(this.skyB, 0.4f, skyWeather);
            float sunsetFactor = 1.0f - fog.smoothstep(0.02f, 0.3f, this.sunY);
            this.sunFogVisibility = fog.smoothstep(-0.06f, 0.1f, this.sunY);
            float sunMix = Mth.m_14036_((float)(sunsetFactor * (1.0f - night)), (float)0.0f, (float)1.0f);
            float sunScale = this.sunFogVisibility * (1.0f - weather * 0.42f);
            this.sunR = fog.lerp(1.1f, 1.55f, sunMix) * sunScale;
            this.sunG = fog.lerp(0.94f, 0.62f, sunMix) * sunScale;
            this.sunB = fog.lerp(0.7f, 0.2f, sunMix) * sunScale;
            this.moonFogVisibility = fog.smoothstep(0.05f, 0.2f, -this.sunY);
            float moonScale = this.moonFogVisibility * (1.0f - weather * 0.35f);
            this.moonR = 0.34f * moonScale;
            this.moonG = 0.44f * moonScale;
            this.moonB = 0.68f * moonScale;
        }
    }
}

