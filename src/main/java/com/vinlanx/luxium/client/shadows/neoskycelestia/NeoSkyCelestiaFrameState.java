/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public record NeoSkyCelestiaFrameState(boolean enabled, boolean skyLightEnabled, boolean moonLight, int blockOnlyLightTexture, int celestialLightLutTexture, int nearDepthTexture, int farDepthTexture, boolean entityShadowEnabled, int entityDepthTexture, Matrix4f nearLightFromView, Matrix4f farLightFromView, Matrix4f entityLightFromView, float nearRadius, float farRadius, float entityRadius, float nearTexelSize, float farTexelSize, float entityTexelSize, int filterSamples, float baseBiasNear, float baseBiasFar, float entityBaseBias, float slopeBias, float cascadeBlendStart, Vector3f lightDirection, Vector3f directColor, Vector3f skyAmbientColor, Vector3f groundAmbientColor, float directStrength, float ambientStrength, long nearGeneration, long farGeneration, long entityGeneration) {
    public static NeoSkyCelestiaFrameState disabled(int celestialLightLutTexture, int fallbackDepthTexture) {
        return new NeoSkyCelestiaFrameState(false, false, false, -1, celestialLightLutTexture, fallbackDepthTexture, fallbackDepthTexture, false, fallbackDepthTexture, new Matrix4f(), new Matrix4f(), new Matrix4f(), 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4, 0.001f, 0.001f, 0.001f, 1.0f, 0.94f, new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(1.0f), new Vector3f(1.0f), new Vector3f(1.0f), 0.0f, 1.0f, 0L, 0L, 0L);
    }
}

