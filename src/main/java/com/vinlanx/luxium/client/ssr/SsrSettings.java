/*
 * Decompiled with CFR 0.152.
 */
package com.vinlanx.luxium.client.ssr;

public record SsrSettings(boolean enabled, int coarseSteps, int refinementSteps, float maxDistance, float thickness, float edgeFade, float strength) {
    public static SsrSettings disabled() {
        return new SsrSettings(false, 0, 0, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}

