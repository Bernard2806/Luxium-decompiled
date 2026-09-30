/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.ChunkStatus
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.kawase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

public final class KawaseSourceRegistry {
    private static final KawaseSourceRegistry INSTANCE = new KawaseSourceRegistry();
    private final Map<Long, Byte> sources = new HashMap<Long, Byte>();
    private final Map<Long, Set<Long>> sourcesByChunk = new HashMap<Long, Set<Long>>();
    @Nullable
    private ClientLevel level;
    private long version;
    private Snapshot cachedSnapshot = new Snapshot(0L, new long[0], new byte[0]);

    private KawaseSourceRegistry() {
    }

    public static KawaseSourceRegistry get() {
        return INSTANCE;
    }

    public synchronized void setLevel(@Nullable ClientLevel level) {
        if (this.level == level) {
            return;
        }
        this.level = level;
        this.sources.clear();
        this.sourcesByChunk.clear();
        ++this.version;
    }

    public synchronized void addChunk(ClientLevel level, LevelChunk chunk) {
        if (this.level != level) {
            this.setLevel(level);
        }
        int chunkX = chunk.m_7697_().f_45578_;
        int chunkZ = chunk.m_7697_().f_45579_;
        this.removeChunkInternal(chunkX, chunkZ);
        long chunkKey = ChunkPos.m_45589_((int)chunkX, (int)chunkZ);
        HashSet chunkSources = new HashSet();
        chunk.m_284254_((pos, state) -> {
            int emission = state.getLightEmission((BlockGetter)level, pos);
            if (emission <= 0) {
                return;
            }
            long sourceKey = pos.m_121878_();
            this.sources.put(sourceKey, (byte)Math.min(emission, 15));
            chunkSources.add(sourceKey);
        });
        if (!chunkSources.isEmpty()) {
            this.sourcesByChunk.put(chunkKey, chunkSources);
        }
        ++this.version;
    }

    public synchronized void removeChunk(int chunkX, int chunkZ) {
        if (this.removeChunkInternal(chunkX, chunkZ)) {
            ++this.version;
        }
    }

    public synchronized void updateBlock(ClientLevel level, BlockPos pos, BlockState newState) {
        if (this.level != level) {
            this.setLevel(level);
        }
        long sourceKey = pos.m_121878_();
        long chunkKey = ChunkPos.m_151388_((BlockPos)pos);
        int emission = newState.getLightEmission((BlockGetter)level, pos);
        Byte previous = this.sources.get(sourceKey);
        if (emission > 0) {
            byte value = (byte)Math.min(emission, 15);
            if (previous != null && previous == value) {
                return;
            }
            this.sources.put(sourceKey, value);
            this.sourcesByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet()).add(sourceKey);
        } else {
            if (previous == null) {
                return;
            }
            this.sources.remove(sourceKey);
            Set<Long> chunkSources = this.sourcesByChunk.get(chunkKey);
            if (chunkSources != null) {
                chunkSources.remove(sourceKey);
                if (chunkSources.isEmpty()) {
                    this.sourcesByChunk.remove(chunkKey);
                }
            }
        }
        ++this.version;
    }

    public synchronized Snapshot snapshot() {
        if (this.cachedSnapshot.version() == this.version) {
            return this.cachedSnapshot;
        }
        long[] positions = new long[this.sources.size()];
        byte[] emissions = new byte[this.sources.size()];
        int index = 0;
        for (Map.Entry<Long, Byte> entry : this.sources.entrySet()) {
            positions[index] = entry.getKey();
            emissions[index] = entry.getValue();
            ++index;
        }
        this.cachedSnapshot = new Snapshot(this.version, positions, emissions);
        return this.cachedSnapshot;
    }

    public synchronized void reconcileLoadedChunks(ClientLevel level) {
        if (this.level != level) {
            this.setLevel(level);
            return;
        }
        boolean changed = false;
        Iterator<Map.Entry<Long, Set<Long>>> iterator = this.sourcesByChunk.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Set<Long>> entry = iterator.next();
            long chunkKey = entry.getKey();
            if (level.m_7726_().m_7587_(ChunkPos.m_45592_((long)chunkKey), ChunkPos.m_45602_((long)chunkKey), ChunkStatus.f_62326_, false) != null) continue;
            for (long sourceKey : entry.getValue()) {
                this.sources.remove(sourceKey);
            }
            iterator.remove();
            changed = true;
        }
        if (changed) {
            ++this.version;
        }
    }

    private boolean removeChunkInternal(int chunkX, int chunkZ) {
        Set<Long> removed = this.sourcesByChunk.remove(ChunkPos.m_45589_((int)chunkX, (int)chunkZ));
        if (removed == null || removed.isEmpty()) {
            return false;
        }
        for (long sourceKey : removed) {
            this.sources.remove(sourceKey);
        }
        return true;
    }

    public record Snapshot(long version, long[] positions, byte[] emissions) {
    }
}

