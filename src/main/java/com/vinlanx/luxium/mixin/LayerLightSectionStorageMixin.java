/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.lighting.LayerLightSectionStorage
 *  net.minecraft.world.level.lighting.LightEngine
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.LuxiumLayerLightStorageExtension;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={LayerLightSectionStorage.class})
public abstract class LayerLightSectionStorageMixin
implements LuxiumLayerLightStorageExtension {
    @Shadow
    protected abstract int m_75795_(long var1);

    @Shadow
    protected abstract void m_75772_(long var1, int var3);

    @Shadow
    protected abstract void m_284283_(LightEngine<?, ?> var1);

    @Shadow
    protected abstract void m_75790_();

    @Override
    public int luxium$getStoredLevel(long posKey) {
        return this.m_75795_(posKey);
    }

    @Override
    public void luxium$setStoredLevel(long posKey, int value) {
        this.m_75772_(posKey, value);
    }

    @Override
    public void luxium$markNewInconsistencies(LightEngine<?, ?> engine) {
        this.m_284283_(engine);
    }

    @Override
    public void luxium$swapSectionMap() {
        this.m_75790_();
    }
}

