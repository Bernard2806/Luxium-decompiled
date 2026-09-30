/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.shaders.Uniform
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.util.Mth
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.vinlanx.luxium.client.posteffects;

import com.mojang.blaze3d.shaders.Uniform;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaFrameState;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class skyvolumetrigodrays {
    private static final float ACTIVE_EPSILON = 1.0E-4f;
    private static final long SKYLIGHT_SAMPLE_TICKS = 2L;
    private static final float ADAPTATION_SETTLE_SECONDS = 0.5f;
    private static final Matrix4f INVERSE_PROJECTION = new Matrix4f();
    private static final Vector3f VIEW_LIGHT_DIRECTION = new Vector3f();
    private static final Vector3f RAY_COLOR = new Vector3f();
    private static Object adaptationLevel;
    private static long lastSkylightSampleGameTime;
    private static long lastAdaptationUpdateNs;
    private static float adaptationTarget;
    private static float adaptationSmoothed;
    private static boolean autoAdaptationWasEnabled;

    private skyvolumetrigodrays() {
    }

    static boolean isEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.skyVolumetricGodRaysEnabled);
    }

    static boolean shouldRender() {
        NeoSkyCelestiaFrameState state;
        if (!skyvolumetrigodrays.isEnabled()) {
            return false;
        }
        NeoSkyCelestia system = NeoSkyCelestia.get();
        if (!skyvolumetrigodrays.hasShadowData(system, state = system.frameState())) {
            return false;
        }
        return state.directStrength() > 1.0E-4f && ((Double)Config.CLIENT.skyVolumetricGodRaysIntensity.get()).floatValue() > 1.0E-4f && ((Double)Config.CLIENT.skyVolumetricGodRaysDensity.get()).floatValue() > 1.0E-4f;
    }

    private static boolean hasShadowData(NeoSkyCelestia system, NeoSkyCelestiaFrameState state) {
        return system.isMainPassActive() && state.enabled() && state.nearDepthTexture() > 0 && state.farDepthTexture() > 0;
    }

    static void configure(ShaderInstance shader, RenderLevelStageEvent event, boolean inverseProjectionAlreadyConfigured) {
        NeoSkyCelestiaFrameState state = NeoSkyCelestia.get().frameState();
        shader.m_173350_("VolumetricShadowMap0", (Object)state.nearDepthTexture());
        shader.m_173350_("VolumetricShadowMap1", (Object)state.farDepthTexture());
        if (!inverseProjectionAlreadyConfigured) {
            NeoSkyFrameCache.copyInverseProjection(event.getProjectionMatrix(), INVERSE_PROJECTION);
            skyvolumetrigodrays.setUniformMatrix(shader, "InverseProjMat", INVERSE_PROJECTION);
        }
        skyvolumetrigodrays.setUniformMatrix(shader, "VolumetricNearLightFromView", state.nearLightFromView());
        skyvolumetrigodrays.setUniformMatrix(shader, "VolumetricFarLightFromView", state.farLightFromView());
        skyvolumetrigodrays.setUniform4f(shader, "VolumetricCascadeData", state.nearRadius(), state.farRadius(), state.baseBiasNear(), state.baseBiasFar());
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricCascadeBlendStart", Mth.m_14036_((float)state.cascadeBlendStart(), (float)0.0f, (float)1.0f));
        VIEW_LIGHT_DIRECTION.set((Vector3fc)state.lightDirection());
        event.getPoseStack().m_85850_().m_252922_().transformDirection(VIEW_LIGHT_DIRECTION);
        if (VIEW_LIGHT_DIRECTION.lengthSquared() > 1.0E-8f) {
            VIEW_LIGHT_DIRECTION.normalize();
        } else {
            VIEW_LIGHT_DIRECTION.set(0.0f, 1.0f, 0.0f);
        }
        skyvolumetrigodrays.setUniform3f(shader, "VolumetricLightDirectionView", skyvolumetrigodrays.VIEW_LIGHT_DIRECTION.x, skyvolumetrigodrays.VIEW_LIGHT_DIRECTION.y, skyvolumetrigodrays.VIEW_LIGHT_DIRECTION.z);
        float celestialInfluence = Mth.m_14036_((float)(((Double)Config.CLIENT.skyVolumetricGodRaysCelestialColorInfluence.get()).floatValue() * 0.01f), (float)0.0f, (float)1.0f);
        RAY_COLOR.set(1.0f, 1.0f, 1.0f).lerp((Vector3fc)state.directColor(), celestialInfluence);
        skyvolumetrigodrays.setUniform3f(shader, "VolumetricLightColor", skyvolumetrigodrays.RAY_COLOR.x, skyvolumetrigodrays.RAY_COLOR.y, skyvolumetrigodrays.RAY_COLOR.z);
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricLightStrength", Mth.m_14036_((float)state.directStrength(), (float)0.0f, (float)2.0f));
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricIntensity", Mth.m_14036_((float)((Double)Config.CLIENT.skyVolumetricGodRaysIntensity.get()).floatValue(), (float)0.0f, (float)3.0f));
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricDensity", Mth.m_14036_((float)((Double)Config.CLIENT.skyVolumetricGodRaysDensity.get()).floatValue(), (float)0.0f, (float)3.0f));
        skyvolumetrigodrays.setUniform1i(shader, "VolumetricSamples", Mth.m_14045_((int)((Integer)Config.CLIENT.skyVolumetricGodRaysSamples.get()), (int)8, (int)32));
        float baseUniformity = Mth.m_14036_((float)(((Double)Config.CLIENT.skyVolumetricGodRaysUniformity.get()).floatValue() * 0.01f), (float)0.0f, (float)1.0f);
        float baseSideVisibility = Mth.m_14036_((float)(((Double)Config.CLIENT.skyVolumetricGodRaysSideVisibility.get()).floatValue() * 0.01f), (float)0.0f, (float)1.0f);
        float adaptation = skyvolumetrigodrays.updateAutoAdaptation();
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricUniformity", Mth.m_14179_((float)adaptation, (float)baseUniformity, (float)1.0f));
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricSideVisibility", Mth.m_14179_((float)adaptation, (float)baseSideVisibility, (float)1.0f));
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricHazeSuppression", Mth.m_14036_((float)(((Double)Config.CLIENT.skyVolumetricGodRaysHazeSuppression.get()).floatValue() * 0.01f), (float)0.0f, (float)1.0f));
        boolean entityOcclusion = Config.isFeatureEnabled(Config.CLIENT.skyVolumetricGodRaysEntityOcclusion) && state.entityShadowEnabled() && state.entityDepthTexture() > 0;
        skyvolumetrigodrays.setUniform1i(shader, "VolumetricEntityOcclusionEnabled", entityOcclusion ? 1 : 0);
        if (entityOcclusion) {
            shader.m_173350_("VolumetricEntityShadowMap", (Object)state.entityDepthTexture());
            skyvolumetrigodrays.setUniformMatrix(shader, "VolumetricEntityLightFromView", state.entityLightFromView());
            skyvolumetrigodrays.setUniform1f(shader, "VolumetricEntityShadowBias", Math.max(state.entityBaseBias(), 1.0E-6f));
        }
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricMaxDistance", Mth.m_14036_((float)((Double)Config.CLIENT.skyVolumetricGodRaysMaxDistance.get()).floatValue(), (float)16.0f, (float)384.0f));
        skyvolumetrigodrays.setUniform1f(shader, "VolumetricAnisotropy", Mth.m_14036_((float)((Double)Config.CLIENT.skyVolumetricGodRaysAnisotropy.get()).floatValue(), (float)0.0f, (float)0.95f));
    }

    public static int sampleCameraSkylight() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return -1;
        }
        BlockPos cameraPos = BlockPos.m_274446_((Position)minecraft.f_91063_.m_109153_().m_90583_());
        minecraft.f_91073_.m_46465_();
        return Mth.m_14045_((int)minecraft.f_91073_.m_46803_(cameraPos), (int)0, (int)15);
    }

    private static float updateAutoAdaptation() {
        float alpha;
        boolean levelChanged;
        boolean enabled = Config.isFeatureEnabled(Config.CLIENT.skyVolumetricGodRaysAutoAdaptation);
        Minecraft minecraft = Minecraft.m_91087_();
        if (!enabled || minecraft.f_91073_ == null) {
            skyvolumetrigodrays.resetAutoAdaptation();
            return 0.0f;
        }
        long nowNs = System.nanoTime();
        boolean bl = levelChanged = adaptationLevel != minecraft.f_91073_;
        if (levelChanged) {
            adaptationLevel = minecraft.f_91073_;
            lastSkylightSampleGameTime = Long.MIN_VALUE;
            adaptationTarget = 0.0f;
            adaptationSmoothed = 0.0f;
            lastAdaptationUpdateNs = nowNs;
        }
        if (!autoAdaptationWasEnabled) {
            adaptationSmoothed = 0.0f;
            lastSkylightSampleGameTime = Long.MIN_VALUE;
            lastAdaptationUpdateNs = nowNs;
            autoAdaptationWasEnabled = true;
        }
        long gameTime = minecraft.f_91073_.m_46467_();
        if (lastSkylightSampleGameTime == Long.MIN_VALUE || gameTime < lastSkylightSampleGameTime || gameTime - lastSkylightSampleGameTime >= 2L) {
            int skyLight = skyvolumetrigodrays.sampleCameraSkylight();
            int fullAdaptationSkylight = Mth.m_14045_((int)((Integer)Config.CLIENT.skyVolumetricGodRaysFullAdaptationSkylight.get()), (int)0, (int)15);
            adaptationTarget = fullAdaptationSkylight >= 15 ? 1.0f : Mth.m_14036_((float)((15.0f - (float)skyLight) / (15.0f - (float)fullAdaptationSkylight)), (float)0.0f, (float)1.0f);
            lastSkylightSampleGameTime = gameTime;
        }
        if (lastAdaptationUpdateNs == 0L) {
            lastAdaptationUpdateNs = nowNs;
            return adaptationSmoothed;
        }
        float dt = (float)((double)(nowNs - lastAdaptationUpdateNs) * 1.0E-9);
        lastAdaptationUpdateNs = nowNs;
        if ((dt = Mth.m_14036_((float)dt, (float)0.0f, (float)0.25f)) > 0.0f && Math.abs(adaptationTarget - (adaptationSmoothed += (adaptationTarget - adaptationSmoothed) * (alpha = 1.0f - (float)Math.exp(-6.0f * dt / 0.5f)))) < 1.0E-4f) {
            adaptationSmoothed = adaptationTarget;
        }
        return Mth.m_14036_((float)adaptationSmoothed, (float)0.0f, (float)1.0f);
    }

    private static void resetAutoAdaptation() {
        adaptationLevel = null;
        lastSkylightSampleGameTime = Long.MIN_VALUE;
        lastAdaptationUpdateNs = 0L;
        adaptationTarget = 0.0f;
        adaptationSmoothed = 0.0f;
        autoAdaptationWasEnabled = false;
    }

    private static void setUniform1i(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    private static void setUniform1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void setUniform3f(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private static void setUniform4f(ShaderInstance shader, String name, float x, float y, float z, float w) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5805_(x, y, z, w);
        }
    }

    private static void setUniformMatrix(ShaderInstance shader, String name, Matrix4f matrix) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5679_(matrix);
        }
    }

    static {
        lastSkylightSampleGameTime = Long.MIN_VALUE;
    }
}

