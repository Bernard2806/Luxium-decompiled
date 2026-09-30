/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.world.level.lighting.LayerLightSectionStorage
 *  net.minecraft.world.level.lighting.LightEngine
 *  net.minecraft.world.level.lighting.LightEngine$QueueEntry
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.LuxiumLayerLightStorageExtension;
import com.vinlanx.luxium.rtx.LuxiumLightEngineExtension;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={LightEngine.class})
public abstract class LightEngineMixin
implements LuxiumLightEngineExtension {
    @Shadow
    @Final
    private LongOpenHashSet f_283863_;
    @Shadow
    @Final
    private LongArrayFIFOQueue f_283823_;
    @Shadow
    @Final
    private LongArrayFIFOQueue f_283934_;
    @Shadow
    @Final
    protected LayerLightSectionStorage<?> f_283849_;

    @Shadow
    protected abstract void m_75858_(long var1);

    @Shadow
    protected abstract void m_284316_(long var1, long var3, int var5);

    @Shadow
    protected abstract void m_284321_(long var1, long var3);

    @Invoker(value="clearChunkCache")
    protected abstract void luxium$invokeClearChunkCache();

    @Override
    public int luxium$runLightUpdatesBudgeted(int maxOperations) {
        long posKey;
        int processed;
        int budget = Math.max(1, maxOperations);
        LongIterator nodes = this.f_283863_.iterator();
        for (processed = 0; processed < budget && nodes.hasNext(); ++processed) {
            posKey = nodes.nextLong();
            nodes.remove();
            this.m_75858_(posKey);
        }
        if (this.f_283863_.isEmpty()) {
            this.f_283863_.trim(512);
        }
        while (processed < budget && !this.f_283823_.isEmpty()) {
            posKey = this.f_283823_.dequeueLong();
            long entry = this.f_283823_.dequeueLong();
            this.m_284321_(posKey, entry);
            ++processed;
        }
        LuxiumLayerLightStorageExtension storageAccess = (LuxiumLayerLightStorageExtension)this.f_283849_;
        while (processed < budget && this.f_283823_.isEmpty() && !this.f_283934_.isEmpty()) {
            long posKey2 = this.f_283934_.dequeueLong();
            long entry = this.f_283934_.dequeueLong();
            int stored = storageAccess.luxium$getStoredLevel(posKey2);
            int fromLevel = LightEngine.QueueEntry.m_284170_((long)entry);
            if (LightEngine.QueueEntry.m_284312_((long)entry) && stored < fromLevel) {
                storageAccess.luxium$setStoredLevel(posKey2, fromLevel);
                stored = fromLevel;
            }
            if (stored == fromLevel) {
                this.m_284316_(posKey2, entry, stored);
            }
            ++processed;
        }
        this.luxium$invokeClearChunkCache();
        if (this.f_283863_.isEmpty() && this.f_283823_.isEmpty() && this.f_283934_.isEmpty()) {
            storageAccess.luxium$markNewInconsistencies((LightEngine)this);
        }
        storageAccess.luxium$swapSectionMap();
        return processed;
    }
}

