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
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class tonemap {
    private tonemap() {
    }

    static boolean isEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.tonemapEnabled);
    }

    @Nullable
    public static ShaderInstance getShader() {
        return ShaderManager.getTonemapShader();
    }

    public static void configure(ShaderInstance shader, int sceneTexture, int effectsTexture, boolean compositeEffects) {
        tonemap.configure(shader, sceneTexture, effectsTexture, compositeEffects, false, 1, 1);
    }

    public static void configure(ShaderInstance shader, int sceneTexture, int effectsTexture, boolean compositeEffects, boolean lsrEnabled, int sourceWidth, int sourceHeight) {
        shader.m_173350_("SceneSampler", (Object)sceneTexture);
        shader.m_173350_("EffectsSampler", (Object)effectsTexture);
        tonemap.setUniform1i(shader, "CompositeEffects", compositeEffects ? 1 : 0);
        tonemap.setUniform1i(shader, "LsrEnabled", lsrEnabled ? 1 : 0);
        tonemap.setUniform2f(shader, "SceneTexelSize", 1.0f / (float)Math.max(1, sourceWidth), 1.0f / (float)Math.max(1, sourceHeight));
        tonemap.setUniform1f(shader, "LsrSharpness", lsrEnabled ? LsrSystem.sharpness() : 0.0f);
        tonemap.setUniform1f(shader, "TonemapExposure", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapExposure.get()).floatValue(), (float)-2.0f, (float)2.0f));
        tonemap.setUniform1f(shader, "TonemapContrast", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapContrast.get()).floatValue(), (float)0.0f, (float)1.0f));
        tonemap.setUniform1f(shader, "TonemapHighlightCompression", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapHighlightCompression.get()).floatValue(), (float)0.0f, (float)1.0f));
        tonemap.setUniform1f(shader, "TonemapShadowDepth", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapShadowDepth.get()).floatValue(), (float)0.0f, (float)1.0f));
        tonemap.setUniform1f(shader, "TonemapSaturation", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapSaturation.get()).floatValue(), (float)0.0f, (float)2.0f));
        tonemap.setUniform1f(shader, "TonemapVibrance", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapVibrance.get()).floatValue(), (float)0.0f, (float)1.0f));
        tonemap.setUniform1f(shader, "TonemapGamma", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapGamma.get()).floatValue(), (float)1.6f, (float)2.8f));
        tonemap.setUniform1f(shader, "TonemapStrength", Mth.m_14036_((float)((Double)Config.CLIENT.tonemapStrength.get()).floatValue(), (float)0.0f, (float)1.0f));
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

    private static void setUniform2f(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }
}

