/*
 * Decompiled with CFR 0.152.
 */
package com.vinlanx.luxium.client.plantswave;

public enum PlantWaveProfile {
    NONE(0, Anchor.NONE),
    ROOTED(1, Anchor.ROOTED),
    FREE(1, Anchor.FREE),
    HANGING(1, Anchor.HANGING),
    LEAVES(2, Anchor.FREE),
    AQUATIC_ROOTED(3, Anchor.ROOTED),
    AQUATIC_FREE(3, Anchor.FREE);

    private final int materialCode;
    private final Anchor anchor;

    private PlantWaveProfile(int materialCode, Anchor anchor) {
        this.materialCode = materialCode;
        this.anchor = anchor;
    }

    public int materialCode() {
        return this.materialCode;
    }

    public int vertexWeight(float y) {
        if (this == AQUATIC_ROOTED) {
            return PlantWaveProfile.quantize(PlantWaveProfile.smooth(PlantWaveProfile.clamp(y * 1.08f)));
        }
        if (this == AQUATIC_FREE) {
            return 15;
        }
        return switch (this.anchor) {
            default -> throw new IncompatibleClassChangeError();
            case Anchor.NONE -> 0;
            case Anchor.FREE -> 15;
            case Anchor.ROOTED -> PlantWaveProfile.quantize(PlantWaveProfile.clamp(y));
            case Anchor.HANGING -> PlantWaveProfile.quantize(PlantWaveProfile.clamp(1.0f - y));
        };
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private static int quantize(float value) {
        return Math.max(0, Math.min(15, Math.round(value * 15.0f)));
    }

    private static enum Anchor {
        NONE,
        ROOTED,
        FREE,
        HANGING;

    }
}

