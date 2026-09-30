/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import net.minecraft.util.Mth;

public record ExplosionProfile(int power, double tntKg, float scale, float blastStrength, float coreRadius, float durationScale, float particleMultiplier, float particleSizeScale, float heatMultiplier, float dustMultiplier, float afterburnMultiplier, float flashMultiplier) {
    public static ExplosionProfile fromPower(int requestedPower) {
        int power = Mth.m_14045_((int)requestedPower, (int)1, (int)100);
        double t = ((double)power - 1.0) / 99.0;
        double tntKg = 0.18 * Math.pow(2222.222222222222, t);
        float scale = (float)Math.cbrt(tntKg / 5.0);
        float particleMultiplier = ExplosionProfile.clamp((float)Math.pow(scale, 1.35), 0.22f, 4.3f);
        return new ExplosionProfile(power, tntKg, scale, 7.2f * (float)Math.pow(scale, 0.42), ExplosionProfile.clamp(1.35f * (float)Math.pow(scale, 0.88), 0.48f, 4.9f), ExplosionProfile.clamp((float)Math.pow(scale, 0.62), 0.5f, 2.55f), particleMultiplier, ExplosionProfile.clamp((float)Math.pow(scale, 0.38), 0.68f, 1.75f), ExplosionProfile.clamp(0.94f + 0.09f * ExplosionProfile.log2(Math.max(scale, 0.25f)), 0.78f, 1.22f), ExplosionProfile.clamp(0.72f + 0.24f * scale, 0.62f, 1.95f), ExplosionProfile.clamp((float)Math.pow(particleMultiplier, 0.68), 0.38f, 2.75f), ExplosionProfile.clamp((float)Math.pow(scale, 0.75), 0.34f, 3.0f));
    }

    private static float log2(float value) {
        return (float)(Math.log(value) / Math.log(2.0));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

