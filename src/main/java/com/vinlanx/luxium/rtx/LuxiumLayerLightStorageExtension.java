/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.lighting.LightEngine
 */
package com.vinlanx.luxium.rtx;

import net.minecraft.world.level.lighting.LightEngine;

public interface LuxiumLayerLightStorageExtension {
    public int luxium$getStoredLevel(long var1);

    public void luxium$setStoredLevel(long var1, int var3);

    public void luxium$markNewInconsistencies(LightEngine<?, ?> var1);

    public void luxium$swapSectionMap();
}

