/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  javax.annotation.Nullable
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 *  net.minecraft.world.level.chunk.LightChunkGetter
 *  net.minecraft.world.level.lighting.BlockLightEngine
 *  net.minecraft.world.level.lighting.LightEngine
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.rtx.LuxiumBlockLightEngineExtension;
import com.vinlanx.luxium.rtx.LuxiumLightEngineExtension;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;

public final class VanillaLavaLightEngine {
    private static final VanillaLavaLightEngine INSTANCE = new VanillaLavaLightEngine();
    private static final int MAX_COMPLETED_SECTIONS_PER_TICK = 3;
    private static final long SECTION_SCAN_BUDGET_NANOS = 1250000L;
    private static final int SCAN_DEADLINE_CHECK_MASK = 63;
    private static final int MAX_SOURCE_CHECKS_PER_TICK = 1024;
    private static final long SOURCE_CHECK_BUDGET_NANOS = 500000L;
    private static final int MAX_LIGHT_OPERATIONS_PER_TICK = 8000;
    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<PendingSectionScan> pendingSectionScans = new Long2ObjectOpenHashMap();
    private final LongArrayFIFOQueue pendingSectionOrder = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue pendingSourceChecks = new LongArrayFIFOQueue();
    private final LongOpenHashSet pendingSourceCheckSet = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> lavaSourcesBySection = new Long2ObjectOpenHashMap();
    private final BlockPos.MutableBlockPos scanCursor = new BlockPos.MutableBlockPos();
    private ClientLevel currentLevel;
    private volatile BlockLightEngine engine;
    private volatile boolean active;

    private VanillaLavaLightEngine() {
    }

    public static VanillaLavaLightEngine get() {
        return INSTANCE;
    }

    public void tick(ClientLevel level, boolean rtxEnabled) {
        boolean shouldBeActive;
        if (level != this.currentLevel) {
            this.reset(level);
        }
        boolean bl = shouldBeActive = !NeoGpuVanilla.isConfiguredEnabled() && rtxEnabled && (!Config.isFeatureEnabled(Config.CLIENT.rtxLavaTracingEnabled) || level.m_46472_() == Level.f_46429_);
        if (shouldBeActive != this.active) {
            this.setActive(shouldBeActive);
        }
        if (!this.active) {
            return;
        }
        this.scanPendingSections();
        this.drainSourceChecks();
        BlockLightEngine current = this.engine;
        if (current != null && current.m_75808_()) {
            ((LuxiumLightEngineExtension)(Object)current).luxium$runLightUpdatesBudgeted(8000);
        }
    }

    public void reset(@Nullable ClientLevel level) {
        this.active = false;
        this.engine = null;
        this.currentLevel = level;
        this.loadedChunks.clear();
        this.pendingSectionScans.clear();
        this.pendingSectionOrder.clear();
        this.pendingSourceChecks.clear();
        this.pendingSourceCheckSet.clear();
        this.lavaSourcesBySection.clear();
    }

    public void onChunkLoaded(ClientLevel level, int chunkX, int chunkZ) {
        if (level != this.currentLevel) {
            return;
        }
        long chunkKey = ChunkPos.m_45589_((int)chunkX, (int)chunkZ);
        this.loadedChunks.add(chunkKey);
        if (!this.active) {
            return;
        }
        this.initializeChunk(chunkX, chunkZ);
        this.queueLoadedNeighborSections(chunkX, chunkZ);
    }

    public void onChunkUnloaded(ClientLevel level, int chunkX, int chunkZ) {
        int sectionY;
        if (level != this.currentLevel) {
            return;
        }
        long chunkKey = ChunkPos.m_45589_((int)chunkX, (int)chunkZ);
        this.loadedChunks.remove(chunkKey);
        BlockLightEngine current = this.engine;
        boolean canQueueChecks = this.active && current != null;
        for (sectionY = level.m_151560_(); sectionY < level.m_151561_(); ++sectionY) {
            long sectionKey = SectionPos.m_123209_((int)chunkX, (int)sectionY, (int)chunkZ);
            this.pendingSectionScans.remove(sectionKey);
            LongOpenHashSet knownSources = (LongOpenHashSet)this.lavaSourcesBySection.remove(sectionKey);
            if (!canQueueChecks || knownSources == null) continue;
            LongIterator iterator = knownSources.iterator();
            while (iterator.hasNext()) {
                this.enqueueSourceCheck(iterator.nextLong());
            }
        }
        if (!canQueueChecks) {
            return;
        }
        current.m_9335_(new ChunkPos(chunkX, chunkZ), false);
        for (sectionY = level.m_151560_(); sectionY < level.m_151561_(); ++sectionY) {
            current.m_6191_(SectionPos.m_123173_((int)chunkX, (int)sectionY, (int)chunkZ), true);
        }
    }

    public void onBlockChanged(ClientLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean newLava;
        boolean oldLava;
        BlockLightEngine current = this.engine;
        if (!this.active || current == null || level != this.currentLevel) {
            return;
        }
        LevelChunk chunk = level.m_7726_().m_7131_(pos.m_123341_() >> 4, pos.m_123343_() >> 4);
        if (chunk != null) {
            int sectionIndex = level.m_151564_(pos.m_123342_());
            LevelChunkSection[] sections = chunk.m_7103_();
            if (sectionIndex >= 0 && sectionIndex < sections.length) {
                current.m_6191_(SectionPos.m_123199_((BlockPos)pos), sections[sectionIndex].m_188008_());
            }
        }
        if ((oldLava = oldState.m_60819_().m_205070_(FluidTags.f_13132_)) != (newLava = newState.m_60819_().m_205070_(FluidTags.f_13132_))) {
            long sectionKey = SectionPos.m_175568_((BlockPos)pos);
            LongOpenHashSet bucket = (LongOpenHashSet)this.lavaSourcesBySection.get(sectionKey);
            if (newLava) {
                if (bucket == null) {
                    bucket = new LongOpenHashSet();
                    this.lavaSourcesBySection.put(sectionKey, (Object)bucket);
                }
                bucket.add(pos.m_121878_());
            } else if (bucket != null) {
                bucket.remove(pos.m_121878_());
                if (bucket.isEmpty()) {
                    this.lavaSourcesBySection.remove(sectionKey);
                }
            }
        }
        if (oldLava != newLava || LightEngine.m_284387_((BlockGetter)level, (BlockPos)pos, (BlockState)oldState, (BlockState)newState)) {
            current.m_7174_(pos);
        }
    }

    public int sampleLight(BlockPos pos) {
        BlockLightEngine current = this.engine;
        return this.active && current != null ? current.m_7768_(pos) : 0;
    }

    public boolean isActive() {
        return this.active && this.engine != null;
    }

    private void setActive(boolean enabled) {
        if (!enabled) {
            ClientLevel level = this.currentLevel;
            this.active = false;
            this.engine = null;
            this.pendingSectionScans.clear();
            this.pendingSectionOrder.clear();
            this.pendingSourceChecks.clear();
            this.pendingSourceCheckSet.clear();
            this.lavaSourcesBySection.clear();
            if (level != null) {
                this.markLoadedSectionsDirty(level);
            }
            return;
        }
        ClientLevel level = this.currentLevel;
        if (level == null) {
            return;
        }
        BlockLightEngine created = new BlockLightEngine((LightChunkGetter)level.m_7726_());
        ((LuxiumBlockLightEngineExtension)(Object)created).luxium$setLavaOnly(true);
        this.engine = created;
        this.active = true;
        LongIterator iterator = this.loadedChunks.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.nextLong();
            int chunkX = ChunkPos.m_45592_((long)chunkKey);
            int chunkZ = ChunkPos.m_45602_((long)chunkKey);
            this.initializeChunk(chunkX, chunkZ);
            this.queueChunkSections(chunkX, chunkZ);
        }
    }

    private void queueLoadedNeighborSections(int chunkX, int chunkZ) {
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                int nearbyX = chunkX + dx;
                int nearbyZ = chunkZ + dz;
                if (!this.loadedChunks.contains(ChunkPos.m_45589_((int)nearbyX, (int)nearbyZ))) continue;
                this.queueChunkSections(nearbyX, nearbyZ);
            }
        }
    }

    private void queueChunkSections(int chunkX, int chunkZ) {
        ClientLevel level = this.currentLevel;
        if (level == null) {
            return;
        }
        for (int sectionY = level.m_151560_(); sectionY < level.m_151561_(); ++sectionY) {
            this.queueSection(SectionPos.m_123209_((int)chunkX, (int)sectionY, (int)chunkZ));
        }
    }

    private void queueSection(long sectionKey) {
        if (this.pendingSectionScans.containsKey(sectionKey)) {
            return;
        }
        this.pendingSectionScans.put(sectionKey, (Object)new PendingSectionScan(sectionKey));
        this.pendingSectionOrder.enqueue(sectionKey);
    }

    private void initializeChunk(int chunkX, int chunkZ) {
        ClientLevel level = this.currentLevel;
        BlockLightEngine current = this.engine;
        if (level == null || current == null) {
            return;
        }
        LevelChunk chunk = level.m_7726_().m_7131_(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        current.m_9335_(new ChunkPos(chunkX, chunkZ), true);
        LevelChunkSection[] sections = chunk.m_7103_();
        for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
            int sectionY = level.m_151568_(sectionIndex);
            current.m_6191_(SectionPos.m_123173_((int)chunkX, (int)sectionY, (int)chunkZ), sections[sectionIndex].m_188008_());
        }
    }

    private void scanPendingSections() {
        long deadline = System.nanoTime() + 1250000L;
        int completed = 0;
        while (completed < 3 && !this.pendingSectionOrder.isEmpty() && System.nanoTime() < deadline) {
            long sectionKey = this.pendingSectionOrder.dequeueLong();
            PendingSectionScan scan = (PendingSectionScan)this.pendingSectionScans.get(sectionKey);
            if (scan == null) continue;
            if (this.scanSectionSlice(scan, deadline)) {
                this.pendingSectionScans.remove(sectionKey);
                this.publishCompletedSectionScan(scan);
                ++completed;
                continue;
            }
            this.pendingSectionOrder.enqueue(sectionKey);
            break;
        }
    }

    private boolean scanSectionSlice(PendingSectionScan scan, long deadline) {
        ClientLevel level = this.currentLevel;
        if (level == null || !this.loadedChunks.contains(scan.chunkKey)) {
            return true;
        }
        LevelChunk chunk = level.m_7726_().m_7131_(scan.chunkX, scan.chunkZ);
        if (chunk == null) {
            return true;
        }
        int sectionIndex = scan.sectionY - level.m_151560_();
        LevelChunkSection[] sections = chunk.m_7103_();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return true;
        }
        LevelChunkSection section = sections[sectionIndex];
        if (!scan.paletteChecked) {
            scan.paletteChecked = true;
            if (section.m_188008_() || !section.m_63002_(state -> state.m_60819_().m_205070_(FluidTags.f_13132_))) {
                scan.noLavaPossible = true;
                return true;
            }
        }
        if (scan.noLavaPossible) {
            return true;
        }
        int baseX = scan.chunkX << 4;
        int baseY = scan.sectionY << 4;
        int baseZ = scan.chunkZ << 4;
        while (scan.cursor < 4096) {
            int index = scan.cursor++;
            int localX = index & 0xF;
            int localY = index >>> 8 & 0xF;
            int localZ = index >>> 4 & 0xF;
            BlockState state2 = section.m_62982_(localX, localY, localZ);
            if (state2.m_60819_().m_205070_(FluidTags.f_13132_)) {
                scan.foundSources.add(BlockPos.m_121882_((int)(baseX + localX), (int)(baseY + localY), (int)(baseZ + localZ)));
            }
            if ((index & 0x3F) != 63 || System.nanoTime() < deadline) continue;
            return false;
        }
        return true;
    }

    private void publishCompletedSectionScan(PendingSectionScan scan) {
        LongOpenHashSet previous = (LongOpenHashSet)this.lavaSourcesBySection.remove(scan.sectionKey);
        if (scan.noLavaPossible || scan.foundSources.isEmpty()) {
            if (previous != null) {
                this.enqueueAll(previous);
            }
            return;
        }
        LongOpenHashSet immutableWorkingSet = new LongOpenHashSet((LongCollection)scan.foundSources);
        this.lavaSourcesBySection.put(scan.sectionKey, (Object)immutableWorkingSet);
        this.enqueueAll(immutableWorkingSet);
        if (previous != null) {
            LongIterator iterator = previous.iterator();
            while (iterator.hasNext()) {
                long key = iterator.nextLong();
                if (immutableWorkingSet.contains(key)) continue;
                this.enqueueSourceCheck(key);
            }
        }
    }

    private void enqueueAll(LongOpenHashSet values) {
        LongIterator iterator = values.iterator();
        while (iterator.hasNext()) {
            this.enqueueSourceCheck(iterator.nextLong());
        }
    }

    private void enqueueSourceCheck(long key) {
        if (this.pendingSourceCheckSet.add(key)) {
            this.pendingSourceChecks.enqueue(key);
        }
    }

    private void drainSourceChecks() {
        BlockLightEngine current = this.engine;
        if (current == null || this.pendingSourceChecks.isEmpty()) {
            return;
        }
        long deadline = System.nanoTime() + 500000L;
        for (int checked = 0; checked < 1024 && !this.pendingSourceChecks.isEmpty() && System.nanoTime() < deadline; ++checked) {
            long key = this.pendingSourceChecks.dequeueLong();
            this.pendingSourceCheckSet.remove(key);
            this.scanCursor.m_122178_(BlockPos.m_121983_((long)key), BlockPos.m_122008_((long)key), BlockPos.m_122015_((long)key));
            current.m_7174_((BlockPos)this.scanCursor);
        }
    }

    private void markLoadedSectionsDirty(ClientLevel level) {
        LongIterator iterator = this.loadedChunks.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.nextLong();
            int chunkX = ChunkPos.m_45592_((long)chunkKey);
            int chunkZ = ChunkPos.m_45602_((long)chunkKey);
            for (int sectionY = level.m_151560_(); sectionY < level.m_151561_(); ++sectionY) {
                level.m_7726_().m_6506_(LightLayer.BLOCK, SectionPos.m_123173_((int)chunkX, (int)sectionY, (int)chunkZ));
            }
        }
    }

    private static final class PendingSectionScan {
        final long sectionKey;
        final int chunkX;
        final int chunkZ;
        final int sectionY;
        final long chunkKey;
        final LongOpenHashSet foundSources = new LongOpenHashSet();
        int cursor;
        boolean paletteChecked;
        boolean noLavaPossible;

        PendingSectionScan(long sectionKey) {
            this.sectionKey = sectionKey;
            this.chunkX = SectionPos.m_123213_((long)sectionKey);
            this.chunkZ = SectionPos.m_123230_((long)sectionKey);
            this.sectionY = SectionPos.m_123225_((long)sectionKey);
            this.chunkKey = ChunkPos.m_45589_((int)this.chunkX, (int)this.chunkZ);
        }
    }
}
