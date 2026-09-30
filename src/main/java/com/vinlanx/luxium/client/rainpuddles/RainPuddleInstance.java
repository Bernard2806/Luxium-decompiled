/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package com.vinlanx.luxium.client.rainpuddles;

import com.vinlanx.luxium.client.rainpuddles.RainPuddlePatch;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record RainPuddleInstance(ResourceLocation depthMap, int sizeBlocks, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ, List<RainPuddlePatch> patches) {
    public RainPuddleInstance {
        patches = List.copyOf(patches);
    }

    public int patchCount() {
        return this.patches.size();
    }

    public boolean intersects(int minX, int minZ, int maxX, int maxZ) {
        return this.minBlockX <= maxX && this.maxBlockX >= minX && this.minBlockZ <= maxZ && this.maxBlockZ >= minZ;
    }
}

