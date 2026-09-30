/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.shaders.Uniform
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.posteffects;

import com.mojang.blaze3d.shaders.Uniform;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.posteffects.skygodrays;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class lensflare {
    private lensflare() {
    }

    static boolean isEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.lensFlareEnabled);
    }

    @Nullable
    static ShaderInstance getShader() {
        return ShaderManager.getLensFlareShader();
    }

    static void configure(ShaderInstance shader, int width, int height, int depthTexture) {
        lensflare.configure(shader, width, height, depthTexture, 0);
    }

    static void configure(ShaderInstance shader, int width, int height, int depthTexture, int cloudOcclusionTexture) {
        skygodrays.configureCelestialUniforms(shader, width, height, depthTexture, cloudOcclusionTexture);
        lensflare.configureEffectUniforms(shader);
    }

    static void configureEffectUniforms(ShaderInstance shader) {
        lensflare.setUniform1f(shader, "FlareIntensity", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareIntensity.get()).floatValue(), (float)0.0f, (float)3.0f));
        lensflare.setUniform1f(shader, "StreakIntensity", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareStreakIntensity.get()).floatValue(), (float)0.0f, (float)3.0f));
        lensflare.setUniform1f(shader, "StreakLength", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareStreakLength.get()).floatValue(), (float)0.25f, (float)3.0f));
        lensflare.setUniform1f(shader, "StreakWidth", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareStreakWidth.get()).floatValue(), (float)0.25f, (float)3.0f));
        lensflare.setUniform1f(shader, "ChromaticSpread", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareChromaticSpread.get()).floatValue(), (float)0.0f, (float)3.0f));
        lensflare.setUniform1f(shader, "GhostIntensity", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareGhostIntensity.get()).floatValue(), (float)0.0f, (float)3.0f));
        lensflare.setUniform1f(shader, "GhostSize", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareGhostSize.get()).floatValue(), (float)0.35f, (float)3.0f));
        lensflare.setUniform1f(shader, "FlareSpread", Mth.m_14036_((float)((Double)Config.CLIENT.lensFlareSpread.get()).floatValue(), (float)0.35f, (float)3.0f));
    }

    private static void setUniform1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }
}

