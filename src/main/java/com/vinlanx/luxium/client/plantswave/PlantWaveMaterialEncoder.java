/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material
 *  net.minecraft.core.Direction
 */
package com.vinlanx.luxium.client.plantswave;

import com.vinlanx.luxium.client.plantswave.PlantWaveProfile;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.core.Direction;

public final class PlantWaveMaterialEncoder {
    private static final int NORMAL_SHIFT = 3;
    private static final int NORMAL_MASK = 56;
    private static final int WAVE_SHIFT = 6;
    private static final int WAVE_MASK = 192;
    private static final Material[][] SOLID = PlantWaveMaterialEncoder.wrap(DefaultMaterials.SOLID);
    private static final Material[][] CUTOUT = PlantWaveMaterialEncoder.wrap(DefaultMaterials.CUTOUT);
    private static final Material[][] CUTOUT_MIPPED = PlantWaveMaterialEncoder.wrap(DefaultMaterials.CUTOUT_MIPPED);
    private static final Material[][] TRANSLUCENT = PlantWaveMaterialEncoder.wrap(DefaultMaterials.TRANSLUCENT);

    private PlantWaveMaterialEncoder() {
    }

    public static Material encode(Material material, Direction direction, boolean axisAligned, PlantWaveProfile profile) {
        int normalCode = PlantWaveMaterialEncoder.normalCode(direction, axisAligned);
        int waveCode = profile.materialCode();
        if (normalCode == 0 && waveCode == 0) {
            return material;
        }
        if (material == DefaultMaterials.SOLID) {
            return SOLID[normalCode][waveCode];
        }
        if (material == DefaultMaterials.CUTOUT) {
            return CUTOUT[normalCode][waveCode];
        }
        if (material == DefaultMaterials.CUTOUT_MIPPED) {
            return CUTOUT_MIPPED[normalCode][waveCode];
        }
        if (material == DefaultMaterials.TRANSLUCENT) {
            return TRANSLUCENT[normalCode][waveCode];
        }
        return material;
    }

    private static int normalCode(Direction direction, boolean axisAligned) {
        if (!axisAligned || direction == null) {
            return 0;
        }
        return switch (direction) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.DOWN -> 1;
            case Direction.UP -> 2;
            case Direction.NORTH -> 3;
            case Direction.SOUTH -> 4;
            case Direction.WEST -> 5;
            case Direction.EAST -> 6;
        };
    }

    private static Material[][] wrap(Material base) {
        Material[][] variants = new Material[7][4];
        for (int normalCode = 0; normalCode < 7; ++normalCode) {
            for (int waveCode = 0; waveCode < 4; ++waveCode) {
                variants[normalCode][waveCode] = normalCode == 0 && waveCode == 0 ? base : new EncodedMaterial(base, normalCode, waveCode);
            }
        }
        return variants;
    }

    private static final class EncodedMaterial
    extends Material {
        private final int encodedBits;

        private EncodedMaterial(Material base, int normalCode, int waveCode) {
            super(base.pass, base.alphaCutoff, base.mipped);
            this.encodedBits = base.bits() & 0xFFFFFF07 | (normalCode & 7) << 3 | (waveCode & 3) << 6;
        }

        public int bits() {
            return this.encodedBits;
        }
    }
}

