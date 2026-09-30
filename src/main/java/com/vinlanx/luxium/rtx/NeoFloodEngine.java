/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.rtx.FloodRtSettings;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodRtEngine;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class NeoFloodEngine {
    private static final Direction[] FACES = Direction.values();
    private static final int SNAPSHOT_SHARDS = 64;
    private static final int SNAPSHOT_SHARD_MASK = 63;
    private static final int DIRTY_BLOCK_RETRACE_DELAY_TICKS = 2;
    private static final int MAX_SOURCE_REACH_BLOCKS = (int)Math.ceil(LightRtMath.getMaxDistance(15));
    private static final int SOURCE_CHUNK_RADIUS = MAX_SOURCE_REACH_BLOCKS + 15 >> 4;
    private static final int MAX_WORLD_TRACE_BATCH = 16;
    private static final long BACKGROUND_BATCH_INTERVAL_NANOS = 50000000L;
    private final Object stateLock = new Object();
    private final Object dirtyBlocksLock = new Object();
    private final ConcurrentHashMap<Long, SourceState> sources = new ConcurrentHashMap();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesByChunk = new Long2ObjectOpenHashMap();
    private final LongOpenHashSet worldDirtyQueue = new LongOpenHashSet();
    private final LongOpenHashSet pendingDirtyBlocks = new LongOpenHashSet();
    private final LongOpenHashSet selectionCandidates = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesBySection = new Long2ObjectOpenHashMap();
    private final ExecutorService playerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NeoFlood-Player");
        t.setDaemon(true);
        t.setPriority(6);
        return t;
    });
    private final ExecutorService worldExecutor = Executors.newFixedThreadPool(Math.max(1, Config.MAX_RTX_WORLD_TRACING_WORKERS), r -> {
        Thread t = new Thread(r, "NeoFlood-World");
        t.setDaemon(true);
        t.setPriority(3);
        return t;
    });
    private final ExecutorService dispatchExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NeoFlood-Dispatch");
        t.setDaemon(true);
        t.setPriority(3);
        return t;
    });
    private final ExecutorService publishExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NeoFlood-Publish");
        t.setDaemon(true);
        t.setPriority(4);
        return t;
    });
    private final AtomicBoolean dispatching = new AtomicBoolean();
    private final AtomicBoolean backgroundBatchInFlight = new AtomicBoolean();
    private final AtomicInteger epoch = new AtomicInteger();
    private final AtomicLong visibilityRevision = new AtomicLong();
    private final AtomicLong snapshotVersion = new AtomicLong();
    private volatile Snapshot snapshot = Snapshot.empty();
    private volatile SourceState[] activeSourceSnapshot = new SourceState[0];
    private volatile ClientLevel currentLevel;
    private volatile Vec3 playerPos = Vec3.f_82478_;
    private volatile int worldTracingWorkers = 1;
    private volatile long nextBackgroundBatchNanos;
    private volatile boolean ready;
    private boolean activeSourceSnapshotDirty;
    private int dirtyBlockDelay = -1;
    private final SectionBuildScratch sectionBuildScratch = new SectionBuildScratch();
    private volatile Consumer<LongOpenHashSet> onSectionsChanged;
    private volatile Consumer<LongOpenHashSet> onImmediateSectionsChanged;
    private volatile Consumer<LightChangeBatch> onLightChanges;
    private volatile Runnable onHighPriorityTraceDone;

    public void setLevel(ClientLevel level) {
        this.currentLevel = level;
    }

    public void setPlayerPos(Vec3 pos) {
        this.playerPos = pos == null ? Vec3.f_82478_ : pos;
    }

    public void setOnSectionsChanged(Consumer<LongOpenHashSet> callback) {
        this.onSectionsChanged = callback;
    }

    public void setOnImmediateSectionsChanged(Consumer<LongOpenHashSet> callback) {
        this.onImmediateSectionsChanged = callback;
    }

    public void setOnLightChanges(Consumer<LightChangeBatch> callback) {
        this.onLightChanges = callback;
    }

    public void setOnHighPriorityTraceDone(Runnable callback) {
        this.onHighPriorityTraceDone = callback;
    }

    public void setWorldTracingWorkers(int count) {
        this.worldTracingWorkers = NeoFloodEngine.clamp(count, 1, Math.max(1, Config.MAX_RTX_WORLD_TRACING_WORKERS));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addOrUpdateSource(long key, int emission, boolean playerPlaced) {
        ClientLevel level = this.currentLevel;
        if (level == null || emission <= 0) {
            return;
        }
        Object object = this.stateLock;
        synchronized (object) {
            SourceState state = this.sources.get(key);
            boolean membershipChanged = false;
            if (state == null) {
                state = new SourceState(key);
                this.sources.put(key, state);
                this.indexSourceLocked(state);
                membershipChanged = true;
            } else if (!state.active) {
                membershipChanged = true;
            }
            state.active = true;
            state.emission = emission;
            state.maxReachBlocks = NeoFloodEngine.sourceReach(emission);
            ++state.generation;
            if (membershipChanged) {
                this.activeSourceSnapshotDirty = true;
            }
            if (!playerPlaced) {
                this.worldDirtyQueue.add(key);
            }
        }
        if (playerPlaced) {
            this.submitTrace(key, level, true);
        } else {
            this.scheduleWorldDispatch();
        }
    }

    public void ensureSourceScheduled(long key, int emission, boolean highPriority) {
        this.addOrUpdateSource(key, emission, highPriority);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addOrUpdateWorldSources(Long2ByteOpenHashMap sourceBatch) {
        if (sourceBatch == null || sourceBatch.isEmpty()) {
            return;
        }
        Object object = this.stateLock;
        synchronized (object) {
            boolean membershipChanged = false;
            ObjectIterator it = sourceBatch.long2ByteEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ByteMap.Entry entry = (Long2ByteMap.Entry)it.next();
                int emission = entry.getByteValue() & 0xFF;
                if (emission <= 0) continue;
                long key = entry.getLongKey();
                SourceState state = this.sources.get(key);
                if (state == null) {
                    state = new SourceState(key);
                    this.sources.put(key, state);
                    this.indexSourceLocked(state);
                    membershipChanged = true;
                } else if (!state.active) {
                    membershipChanged = true;
                }
                if (state.active && state.emission == emission && state.result != null) continue;
                state.active = true;
                state.emission = emission;
                state.maxReachBlocks = NeoFloodEngine.sourceReach(emission);
                ++state.generation;
                this.worldDirtyQueue.add(key);
            }
            if (membershipChanged) {
                this.activeSourceSnapshotDirty = true;
            }
        }
        this.scheduleWorldDispatch();
    }

    public void removeSource(long key) {
        this.removeSource(key, false);
    }

    public void removeSource(long key, boolean playerAction) {
        LongOpenHashSet keys = new LongOpenHashSet();
        keys.add(key);
        this.removeSourcesInternal(keys, playerAction);
    }

    public void removeSources(LongOpenHashSet keys) {
        this.removeSourcesInternal(keys, false);
    }

    public void removeSourcesAsync(LongOpenHashSet keys) {
        this.removeSourcesInternal(keys, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void removeSourcesInternal(LongOpenHashSet keys, boolean highPriority) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        ArrayList<SourceState> removed = new ArrayList<SourceState>(keys.size());
        Object object = this.stateLock;
        synchronized (object) {
            LongIterator it = keys.iterator();
            while (it.hasNext()) {
                long key = it.nextLong();
                SourceState state = this.sources.get(key);
                if (state == null || !state.active) continue;
                state.active = false;
                ++state.generation;
                this.worldDirtyQueue.remove(key);
                this.unindexSourceLocked(state);
                this.sources.remove(key, state);
                state.removedResult = state.result;
                removed.add(state);
            }
            if (!removed.isEmpty()) {
                this.activeSourceSnapshotDirty = true;
            }
        }
        if (!removed.isEmpty()) {
            int removalEpoch = this.epoch.get();
            this.publishExecutor.execute(() -> this.publishRemovedSources(removed, highPriority, removalEpoch));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void markDirty(LongOpenHashSet keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Object object = this.stateLock;
        synchronized (object) {
            LongIterator it = keys.iterator();
            while (it.hasNext()) {
                long key = it.nextLong();
                SourceState state = this.sources.get(key);
                if (state == null || !state.active) continue;
                state.maxReachBlocks = NeoFloodEngine.sourceReach(state.emission);
                ++state.generation;
                this.worldDirtyQueue.add(key);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clear() {
        Consumer<LongOpenHashSet> callback;
        this.epoch.incrementAndGet();
        ArrayList<SourceState> oldStates = new ArrayList<SourceState>();
        LongOpenHashSet oldSections = new LongOpenHashSet();
        Object object = this.stateLock;
        synchronized (object) {
            for (SourceState state : this.sources.values()) {
                state.active = false;
                ++state.generation;
                oldStates.add(state);
            }
            Snapshot old = this.snapshot;
            old.collectSectionKeys(oldSections);
            this.sources.clear();
            this.sourcesByChunk.clear();
            this.worldDirtyQueue.clear();
            this.activeSourceSnapshot = new SourceState[0];
            this.activeSourceSnapshotDirty = false;
            this.ready = false;
            this.snapshot = Snapshot.empty();
            this.snapshotVersion.incrementAndGet();
        }
        object = this.dirtyBlocksLock;
        synchronized (object) {
            this.pendingDirtyBlocks.clear();
            this.dirtyBlockDelay = -1;
        }
        this.publishExecutor.execute(() -> {
            this.sourcesBySection.clear();
            Iterator iterator = oldStates.iterator();
            while (iterator.hasNext()) {
                PackedSourceData old;
                SourceState state;
                SourceState sourceState = state = (SourceState)iterator.next();
                synchronized (sourceState) {
                    old = state.result;
                    state.result = null;
                    state.visibilityRevision = 0L;
                }
                if (old == null) continue;
                NeoFloodRtEngine.recycleVisibilityVolume(old.visibilityVolume);
            }
        });
        if (!oldSections.isEmpty() && (callback = this.onSectionsChanged) != null) {
            callback.accept(oldSections);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onBlockChanged(BlockPos pos, ClientLevel level, boolean playerAction) {
        if (pos == null || level == null) {
            return;
        }
        if (playerAction) {
            LongOpenHashSet blocks = new LongOpenHashSet(1);
            blocks.add(pos.m_121878_());
            this.dispatchExecutor.execute(() -> this.processDirtyBlocks(blocks));
            return;
        }
        Object object = this.dirtyBlocksLock;
        synchronized (object) {
            this.pendingDirtyBlocks.add(pos.m_121878_());
            this.dirtyBlockDelay = 2;
        }
    }

    public void tick() {
        this.tickPendingDirtyBlocks();
        this.scheduleWorldDispatch();
    }

    public void boostNearestWorldSources(int limit) {
        if (limit <= 0) {
            return;
        }
        this.scheduleWorldDispatch();
    }

    public void traceNearestSourcesSync(int maxCount, ClientLevel level, Vec3 origin) {
        if (maxCount <= 0 || level == null) {
            return;
        }
        if (origin != null) {
            this.setPlayerPos(origin);
        }
        this.scheduleWorldDispatch();
    }

    public int getLight(BlockPos pos) {
        return pos == null ? 0 : this.getLight(pos.m_121878_());
    }

    public int getLight(long key) {
        return this.snapshot.getLight(key);
    }

    public boolean hasAnyLight(long key) {
        return this.getLight(key) > 0;
    }

    public int getCoverage(BlockPos pos) {
        return pos == null ? 0 : this.getCoverage(pos.m_121878_());
    }

    public int getCoverage(long key) {
        return this.snapshot.getCoverage(key);
    }

    public boolean anySourceNear(long posKey, int extraRadius) {
        int x = BlockPos.m_121983_((long)posKey);
        int y = BlockPos.m_122008_((long)posKey);
        int z = BlockPos.m_122015_((long)posKey);
        for (SourceState source : this.activeSourceSnapshot) {
            int reach = source.maxReachBlocks + Math.max(0, extraRadius);
            if (Math.abs(x - source.x) > reach || Math.abs(y - source.y) > reach || Math.abs(z - source.z) > reach) continue;
            return true;
        }
        return false;
    }

    public long findNearestLitSample(long basePosKey, int axisUX, int axisUY, int axisUZ, int axisVX, int axisVY, int axisVZ, int searchRadius, double sampleX, double sampleY, double sampleZ) {
        if (this.getLight(basePosKey) > 0) {
            return basePosKey;
        }
        int baseX = BlockPos.m_121983_((long)basePosKey);
        int baseY = BlockPos.m_122008_((long)basePosKey);
        int baseZ = BlockPos.m_122015_((long)basePosKey);
        long best = Long.MIN_VALUE;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int dv = -searchRadius; dv <= searchRadius; ++dv) {
            for (int du = -searchRadius; du <= searchRadius; ++du) {
                double dz;
                double dy;
                double dx;
                double distance;
                int z;
                int y;
                int x;
                long key;
                if (du == 0 && dv == 0 || this.getLight(key = BlockPos.m_121882_((int)(x = baseX + axisUX * du + axisVX * dv), (int)(y = baseY + axisUY * du + axisVY * dv), (int)(z = baseZ + axisUZ * du + axisVZ * dv))) <= 0 || !((distance = (dx = (double)x + 0.5 - sampleX) * dx + (dy = (double)y + 0.5 - sampleY) * dy + (dz = (double)z + 0.5 - sampleZ) * dz) < bestDistance)) continue;
                bestDistance = distance;
                best = key;
            }
        }
        return best;
    }

    public long getDominantSource(BlockPos pos) {
        return pos == null ? Long.MIN_VALUE : this.getDominantSource(pos.m_121878_());
    }

    public long getDominantSource(long posKey) {
        SectionLightData section = this.snapshot.getSection(NeoFloodEngine.sectionKeyForBlock(posKey));
        if (section == null || section.sourceKeys.length == 0) {
            return Long.MIN_VALUE;
        }
        long bestKey = Long.MIN_VALUE;
        int bestLight = 0;
        int bestCoverage = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (long sourceKey : section.sourceKeys) {
            int light;
            PackedSourceData data;
            SourceState state = this.sources.get(sourceKey);
            PackedSourceData packedSourceData = data = state == null || !state.active ? null : state.result;
            if (data == null || (light = data.getLight(posKey)) <= 0) continue;
            int coverage = data.getCoverage(posKey);
            double distance = NeoFloodEngine.distanceSq(sourceKey, posKey);
            if (!(light > bestLight || light == bestLight && coverage > bestCoverage || light == bestLight && coverage == bestCoverage && distance < bestDistance) && (light != bestLight || coverage != bestCoverage || distance != bestDistance || sourceKey >= bestKey)) continue;
            bestKey = sourceKey;
            bestLight = light;
            bestCoverage = coverage;
            bestDistance = distance;
        }
        return bestKey;
    }

    public void forEachContributor(long posKey, ContributorVisitor visitor) {
        if (visitor == null) {
            return;
        }
        SectionLightData section = this.snapshot.getSection(NeoFloodEngine.sectionKeyForBlock(posKey));
        if (section == null) {
            return;
        }
        for (long sourceKey : section.sourceKeys) {
            this.visitContributor(sourceKey, posKey, visitor);
        }
    }

    public void visitContributor(long sourceKey, long posKey, ContributorVisitor visitor) {
        PackedSourceData data;
        if (visitor == null) {
            return;
        }
        SourceState state = this.sources.get(sourceKey);
        PackedSourceData packedSourceData = data = state == null || !state.active ? null : state.result;
        if (data == null) {
            return;
        }
        int light = data.getLight(posKey);
        if (light <= 0) {
            return;
        }
        visitor.visit(sourceKey, light, data.getCoverage(posKey), state.emission);
    }

    public boolean sourceContributesTo(long sourceKey, long posKey) {
        SourceState state = this.sources.get(sourceKey);
        PackedSourceData data = state == null || !state.active ? null : state.result;
        return data != null && data.getLight(posKey) > 0 && data.getCoverage(posKey) > 0;
    }

    public long getSourceVisibilityRevision(long sourceKey) {
        SourceState state = this.sources.get(sourceKey);
        return state == null || !state.active ? 0L : state.visibilityRevision;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long copySourceVisibilityVolume(long sourceKey, int radius, byte[] output) {
        if (output == null || radius < 0) {
            return 0L;
        }
        int side = radius * 2 + 1;
        int required = side * side * side;
        if (output.length < required) {
            throw new IllegalArgumentException("Output buffer is smaller than requested source volume");
        }
        Arrays.fill(output, 0, required, (byte)0);
        SourceState state = this.sources.get(sourceKey);
        if (state == null || !state.active) {
            return 0L;
        }
        SourceState sourceState = state;
        synchronized (sourceState) {
            PackedSourceData data = state.result;
            long revision = state.visibilityRevision;
            if (data == null || revision <= 0L) {
                return 0L;
            }
            data.copyVisibility(radius, output);
            return revision;
        }
    }

    public boolean isDirectlyLit(long key, int coverageThreshold) {
        return this.getLight(key) > 0 && this.getCoverage(key) >= coverageThreshold;
    }

    public boolean isFrontier(long key, int coverageThreshold) {
        if (!this.isDirectlyLit(key, coverageThreshold)) {
            return false;
        }
        int x = BlockPos.m_121983_((long)key);
        int y = BlockPos.m_122008_((long)key);
        int z = BlockPos.m_122015_((long)key);
        for (Direction face : FACES) {
            long neighbour = BlockPos.m_121882_((int)(x + face.m_122429_()), (int)(y + face.m_122430_()), (int)(z + face.m_122431_()));
            if (this.getLight(neighbour) > 0 && this.getCoverage(neighbour) >= coverageThreshold) continue;
            return true;
        }
        return false;
    }

    public long getLightSnapshotVersion() {
        return this.snapshotVersion.get();
    }

    public void fillLightVolume(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, byte[] output) {
        if (output == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return;
        }
        int required = sizeX * sizeY * sizeZ;
        if (output.length < required) {
            throw new IllegalArgumentException("Output buffer is smaller than requested light volume");
        }
        Snapshot read = this.snapshot;
        int index = 0;
        for (int y = 0; y < sizeY; ++y) {
            int worldY = minY + y;
            for (int z = 0; z < sizeZ; ++z) {
                int worldZ = minZ + z;
                for (int x = 0; x < sizeX; ++x) {
                    output[index++] = (byte)read.getLight(BlockPos.m_121882_((int)(minX + x), (int)worldY, (int)worldZ));
                }
            }
        }
    }

    public boolean isReady() {
        return this.ready;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void tickPendingDirtyBlocks() {
        LongOpenHashSet blocks = new LongOpenHashSet();
        Object object = this.dirtyBlocksLock;
        synchronized (object) {
            if (this.dirtyBlockDelay < 0) {
                return;
            }
            if (this.dirtyBlockDelay-- > 0) {
                return;
            }
            blocks.addAll((LongCollection)this.pendingDirtyBlocks);
            this.pendingDirtyBlocks.clear();
            this.dirtyBlockDelay = -1;
        }
        if (blocks.isEmpty()) {
            return;
        }
        this.dispatchExecutor.execute(() -> this.processDirtyBlocks(blocks));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processDirtyBlocks(LongOpenHashSet blocks) {
        Object object = this.stateLock;
        synchronized (object) {
            LongOpenHashSet affected = new LongOpenHashSet();
            LongIterator blockIt = blocks.iterator();
            while (blockIt.hasNext()) {
                long blockKey = blockIt.nextLong();
                this.collectCandidateSourcesNearBlockLocked(BlockPos.m_122022_((long)blockKey), affected);
                affected.remove(blockKey);
            }
            LongIterator sourceIt = affected.iterator();
            while (sourceIt.hasNext()) {
                long key = sourceIt.nextLong();
                SourceState state = this.sources.get(key);
                if (state == null || !state.active) continue;
                state.maxReachBlocks = NeoFloodEngine.sourceReach(state.emission);
                ++state.generation;
                this.worldDirtyQueue.add(key);
            }
        }
        this.scheduleWorldDispatch();
    }

    private void scheduleWorldDispatch() {
        if (this.backgroundBatchInFlight.get()) {
            return;
        }
        if (System.nanoTime() < this.nextBackgroundBatchNanos) {
            return;
        }
        if (!this.hasQueuedWorldSources()) {
            return;
        }
        if (!this.dispatching.compareAndSet(false, true)) {
            return;
        }
        this.dispatchExecutor.execute(() -> {
            try {
                this.dispatchWorldSources();
            }
            finally {
                this.dispatching.set(false);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean hasQueuedWorldSources() {
        Object object = this.stateLock;
        synchronized (object) {
            return !this.worldDirtyQueue.isEmpty();
        }
    }

    private void dispatchWorldSources() {
        ClientLevel level = this.currentLevel;
        if (level == null) {
            return;
        }
        long now = System.nanoTime();
        if (now < this.nextBackgroundBatchNanos) {
            return;
        }
        if (!this.backgroundBatchInFlight.compareAndSet(false, true)) {
            return;
        }
        int configuredBatch = Math.min(Math.max(1, FloodRtSettings.updateBudgetPerTick()), Math.max(1, this.worldTracingWorkers));
        ArrayList<TraceCapture> captures = this.selectTraceBatch(Math.min(16, configuredBatch));
        if (captures.isEmpty()) {
            this.backgroundBatchInFlight.set(false);
            return;
        }
        this.nextBackgroundBatchNanos = now + 50000000L;
        this.submitTraceBatch(captures, level, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ArrayList<TraceCapture> selectTraceBatch(int limit) {
        ArrayList<TraceCapture> captures = new ArrayList<TraceCapture>(Math.max(1, limit));
        Object object = this.stateLock;
        synchronized (object) {
            long[] nearest;
            if (this.worldDirtyQueue.isEmpty()) {
                return captures;
            }
            for (long key : nearest = this.selectNearestLocked(this.worldDirtyQueue, this.playerPos, limit)) {
                SourceState state = this.sources.get(key);
                if (state == null || !state.active || state.emission <= 0) {
                    this.worldDirtyQueue.remove(key);
                    continue;
                }
                if (state.submittedGeneration >= state.generation) {
                    this.worldDirtyQueue.remove(key);
                    continue;
                }
                state.submittedGeneration = state.generation;
                captures.add(new TraceCapture(this.epoch.get(), state, state.generation, state.emission));
                this.worldDirtyQueue.remove(key);
            }
        }
        return captures;
    }

    private void submitTraceBatch(ArrayList<TraceCapture> captures, ClientLevel level, boolean highPriority) {
        if (captures.isEmpty()) {
            if (!highPriority) {
                this.backgroundBatchInFlight.set(false);
            }
            return;
        }
        ExecutorService executor = highPriority ? this.playerExecutor : this.worldExecutor;
        int lanes = highPriority ? 1 : Math.min(Math.max(1, this.worldTracingWorkers), captures.size());
        CompletedTrace[] completed = new CompletedTrace[captures.size()];
        AtomicInteger nextIndex = new AtomicInteger();
        AtomicInteger remainingLanes = new AtomicInteger(lanes);
        for (int lane = 0; lane < lanes; ++lane) {
            executor.execute(() -> {
                block8: {
                    block5: while (true) {
                        int index;
                        while ((index = nextIndex.getAndIncrement()) < captures.size()) {
                            TraceCapture capture = (TraceCapture)captures.get(index);
                            try {
                                NeoFloodRtEngine.Result solved = NeoFloodRtEngine.solve(capture.state.key, capture.emission, level);
                                completed[index] = new CompletedTrace(capture, PackedSourceData.from(capture.state.key, solved));
                                continue block5;
                            }
                            catch (Throwable error) {
                                this.restoreFailedTrace(capture);
                            }
                        }
                        break block8;
                        {
                            continue block5;
                            break;
                        }
                        break;
                    }
                    finally {
                        if (remainingLanes.decrementAndGet() == 0) {
                            this.publishExecutor.execute(() -> {
                                try {
                                    this.publishTraceBatch(completed, highPriority);
                                }
                                finally {
                                    if (!highPriority) {
                                        this.backgroundBatchInFlight.set(false);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void restoreFailedTrace(TraceCapture capture) {
        Object object = this.stateLock;
        synchronized (object) {
            SourceState state = capture.state;
            if (this.isCaptureCurrent(capture)) {
                state.submittedGeneration = capture.generation - 1;
                this.worldDirtyQueue.add(state.key);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean submitTrace(long key, ClientLevel level, boolean highPriority) {
        if (!highPriority) {
            Object object = this.stateLock;
            synchronized (object) {
                SourceState state = this.sources.get(key);
                if (state == null || !state.active) {
                    return false;
                }
                this.worldDirtyQueue.add(key);
            }
            this.scheduleWorldDispatch();
            return true;
        }
        TraceCapture capture = this.captureTrace(key);
        if (capture == null) {
            return false;
        }
        ArrayList<TraceCapture> captures = new ArrayList<TraceCapture>(1);
        captures.add(capture);
        this.submitTraceBatch(captures, level, true);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private TraceCapture captureTrace(long key) {
        Object object = this.stateLock;
        synchronized (object) {
            SourceState state = this.sources.get(key);
            if (state == null || !state.active || state.emission <= 0) {
                return null;
            }
            if (state.submittedGeneration >= state.generation) {
                return null;
            }
            state.submittedGeneration = state.generation;
            return new TraceCapture(this.epoch.get(), state, state.generation, state.emission);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void publishTraceBatch(CompletedTrace[] completed, boolean highPriority) {
        ArrayList<CompletedTrace> pending = new ArrayList<CompletedTrace>(completed.length);
        for (CompletedTrace trace : completed) {
            if (trace == null) continue;
            pending.add(trace);
        }
        if (pending.isEmpty()) {
            return;
        }
        while (!pending.isEmpty()) {
            Runnable runnable;
            Long2ObjectOpenHashMap replacements = new Long2ObjectOpenHashMap(pending.size());
            Long2ObjectOpenHashMap oldDataBySource = new Long2ObjectOpenHashMap(pending.size());
            LongOpenHashSet affected = new LongOpenHashSet();
            ArrayList<CompletedTrace> current = new ArrayList<CompletedTrace>(pending.size());
            Object object = this.stateLock;
            synchronized (object) {
                for (CompletedTrace completedTrace : pending) {
                    if (!this.isCaptureCurrent(completedTrace.capture)) {
                        NeoFloodRtEngine.recycleVisibilityVolume(completedTrace.data.visibilityVolume);
                        continue;
                    }
                    long l = completedTrace.capture.state.key;
                    PackedSourceData oldData = completedTrace.capture.state.result;
                    current.add(completedTrace);
                    replacements.put(l, (Object)completedTrace.data);
                    if (oldData != null) {
                        oldDataBySource.put(l, (Object)oldData);
                        affected.addAll((LongCollection)oldData.sectionBits.keySet());
                    }
                    affected.addAll((LongCollection)completedTrace.data.sectionBits.keySet());
                }
            }
            pending.clear();
            if (current.isEmpty()) {
                return;
            }
            PublishResult rebuilt = this.rebuildSectionsBatch(affected, (Long2ObjectOpenHashMap<PackedSourceData>)replacements, null);
            boolean allCurrent = true;
            Object object2 = this.stateLock;
            synchronized (object2) {
                for (CompletedTrace completedTrace : current) {
                    if (this.isCaptureCurrent(completedTrace.capture)) continue;
                    allCurrent = false;
                    break;
                }
                if (allCurrent) {
                    for (CompletedTrace completedTrace : current) {
                        SourceState state = completedTrace.capture.state;
                        PackedSourceData oldData = (PackedSourceData)oldDataBySource.get(state.key);
                        SourceState sourceState = state;
                        synchronized (sourceState) {
                            state.result = completedTrace.data;
                            state.visibilityRevision = this.visibilityRevision.incrementAndGet();
                        }
                        this.updateReverseIndex(state.key, oldData, completedTrace.data);
                    }
                    if (this.activeSourceSnapshotDirty) {
                        this.rebuildActiveSourceSnapshotLocked();
                    }
                    this.applyRebuiltSnapshot(rebuilt);
                    this.ready = true;
                }
            }
            if (!allCurrent) {
                for (CompletedTrace completedTrace : current) {
                    if (this.isCaptureCurrent(completedTrace.capture)) {
                        pending.add(completedTrace);
                        continue;
                    }
                    NeoFloodRtEngine.recycleVisibilityVolume(completedTrace.data.visibilityVolume);
                }
                continue;
            }
            for (CompletedTrace completedTrace : current) {
                PackedSourceData packedSourceData = (PackedSourceData)oldDataBySource.get(completedTrace.capture.state.key);
                if (packedSourceData == null || packedSourceData.visibilityVolume == completedTrace.data.visibilityVolume) continue;
                NeoFloodRtEngine.recycleVisibilityVolume(packedSourceData.visibilityVolume);
            }
            this.notifyRebuiltSections(rebuilt, highPriority);
            if (highPriority && (runnable = this.onHighPriorityTraceDone) != null) {
                runnable.run();
            }
            return;
        }
    }

    private boolean isCaptureCurrent(TraceCapture capture) {
        SourceState state = capture.state;
        return capture.epoch == this.epoch.get() && this.sources.get(state.key) == state && state.active && state.generation == capture.generation && state.emission == capture.emission;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void publishRemovedSources(List<SourceState> removed, boolean highPriority, int removalEpoch) {
        Runnable callback;
        boolean publish;
        Object old;
        LongOpenHashSet removedKeys = new LongOpenHashSet(removed.size());
        LongOpenHashSet affected = new LongOpenHashSet();
        for (SourceState state : removed) {
            removedKeys.add(state.key);
            old = state.result;
            if (old == null) continue;
            affected.addAll((LongCollection)((PackedSourceData)old).sectionBits.keySet());
        }
        PublishResult rebuilt = this.rebuildSectionsBatch(affected, null, removedKeys);
        old = this.stateLock;
        synchronized (old) {
            boolean bl = publish = removalEpoch == this.epoch.get();
            if (publish) {
                for (SourceState state : removed) {
                    this.updateReverseIndex(state.key, state.result, null);
                }
                if (this.activeSourceSnapshotDirty) {
                    this.rebuildActiveSourceSnapshotLocked();
                }
                this.applyRebuiltSnapshot(rebuilt);
            }
            Iterator<SourceState> iterator = removed.iterator();
            while (iterator.hasNext()) {
                SourceState state;
                SourceState sourceState = state = iterator.next();
                synchronized (sourceState) {
                    state.result = null;
                    state.visibilityRevision = 0L;
                }
                this.sources.remove(state.key, state);
            }
        }
        for (SourceState state : removed) {
            PackedSourceData old2 = state.removedResult;
            state.removedResult = null;
            if (old2 == null) continue;
            NeoFloodRtEngine.recycleVisibilityVolume(old2.visibilityVolume);
        }
        if (publish) {
            this.notifyRebuiltSections(rebuilt, highPriority);
        }
        if (publish && highPriority && (callback = this.onHighPriorityTraceDone) != null) {
            callback.run();
        }
    }

    private PublishResult rebuildSectionsBatch(LongOpenHashSet affected, Long2ObjectOpenHashMap<PackedSourceData> replacements, LongOpenHashSet removedKeys) {
        Snapshot oldSnapshot = this.snapshot;
        Long2ObjectOpenHashMap rebuilt = new Long2ObjectOpenHashMap();
        LongOpenHashSet emptySections = new LongOpenHashSet();
        ChangedCells changed = new ChangedCells();
        LongOpenHashSet changedSections = new LongOpenHashSet();
        LongOpenHashSet candidates = new LongOpenHashSet();
        LongIterator sectionIt = affected.iterator();
        while (sectionIt.hasNext()) {
            long sectionKey = sectionIt.nextLong();
            candidates.clear();
            LongOpenHashSet indexed = (LongOpenHashSet)this.sourcesBySection.get(sectionKey);
            if (indexed != null) {
                candidates.addAll((LongCollection)indexed);
            }
            if (removedKeys != null) {
                candidates.removeAll((LongCollection)removedKeys);
            }
            if (replacements != null) {
                ObjectIterator replacementIt = replacements.long2ObjectEntrySet().fastIterator();
                while (replacementIt.hasNext()) {
                    Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)replacementIt.next();
                    if (((PackedSourceData)entry.getValue()).sectionBits.containsKey(sectionKey)) {
                        candidates.add(entry.getLongKey());
                        continue;
                    }
                    candidates.remove(entry.getLongKey());
                }
            }
            SectionLightData previous = oldSnapshot.getSection(sectionKey);
            SectionBuildOutput output = this.buildSection(sectionKey, candidates, replacements, previous);
            if (output.dirtyWords != null) {
                changed.sections.put(sectionKey, (Object)output.dirtyWords);
                changedSections.add(sectionKey);
            }
            if (output.next == null) {
                emptySections.add(sectionKey);
                continue;
            }
            rebuilt.put(sectionKey, (Object)output.next);
        }
        return new PublishResult(affected, (Long2ObjectOpenHashMap<SectionLightData>)rebuilt, emptySections, changed, changedSections);
    }

    private SectionBuildOutput buildSection(long sectionKey, LongOpenHashSet candidates, Long2ObjectOpenHashMap<PackedSourceData> replacements, SectionLightData previous) {
        boolean sameSources;
        SectionBuildScratch scratch = this.sectionBuildScratch;
        scratch.reset(candidates.size());
        int baseX = SectionPos.m_123223_((int)SectionPos.m_123213_((long)sectionKey));
        int baseY = SectionPos.m_123223_((int)SectionPos.m_123225_((long)sectionKey));
        int baseZ = SectionPos.m_123223_((int)SectionPos.m_123230_((long)sectionKey));
        LongIterator sourceIt = candidates.iterator();
        while (sourceIt.hasNext()) {
            long[] bitsForSection;
            PackedSourceData data;
            long sourceKey = sourceIt.nextLong();
            if (replacements != null && replacements.containsKey(sourceKey)) {
                data = (PackedSourceData)replacements.get(sourceKey);
            } else {
                SourceState state = this.sources.get(sourceKey);
                PackedSourceData packedSourceData = data = state == null || !state.active ? null : state.result;
            }
            if (data == null || (bitsForSection = (long[])data.sectionBits.get(sectionKey)) == null) continue;
            scratch.sourceKeys[scratch.sourceCount++] = sourceKey;
            for (int word = 0; word < 64; ++word) {
                for (long bits = bitsForSection[word]; bits != 0L; bits &= bits - 1L) {
                    int bit = Long.numberOfTrailingZeros(bits);
                    int localIndex = (word << 6) + bit;
                    int worldX = baseX + (localIndex & 0xF);
                    int worldZ = baseZ + (localIndex >>> 4 & 0xF);
                    int worldY = baseY + (localIndex >>> 8 & 0xF);
                    int candidateLight = data.getLight(worldX, worldY, worldZ);
                    int candidateCoverage = data.getCoverage(worldX, worldY, worldZ);
                    int currentLight = scratch.light[localIndex] & 0xFF;
                    int currentCoverage = scratch.coverage[localIndex] & 0xFF;
                    if (candidateLight <= currentLight && (candidateLight != currentLight || candidateCoverage <= currentCoverage)) continue;
                    scratch.light[localIndex] = (byte)candidateLight;
                    scratch.coverage[localIndex] = (byte)candidateCoverage;
                }
            }
        }
        Arrays.sort(scratch.sourceKeys, 0, scratch.sourceCount);
        long[] dirtyWords = NeoFloodEngine.diffSection(previous, scratch.light, scratch.coverage);
        boolean bl = sameSources = previous != null && Arrays.equals(previous.sourceKeys, 0, previous.sourceKeys.length, scratch.sourceKeys, 0, scratch.sourceCount);
        if (scratch.sourceCount == 0) {
            return new SectionBuildOutput(null, dirtyWords);
        }
        if (dirtyWords == null && sameSources) {
            return new SectionBuildOutput(previous, null);
        }
        byte[] nextLight = dirtyWords == null && previous != null ? previous.light : Arrays.copyOf(scratch.light, scratch.light.length);
        byte[] nextCoverage = dirtyWords == null && previous != null ? previous.coverage : Arrays.copyOf(scratch.coverage, scratch.coverage.length);
        long[] nextSources = sameSources ? previous.sourceKeys : Arrays.copyOf(scratch.sourceKeys, scratch.sourceCount);
        return new SectionBuildOutput(new SectionLightData(nextLight, nextCoverage, nextSources), dirtyWords);
    }

    private static long[] diffSection(SectionLightData previous, byte[] light, byte[] coverage) {
        long[] words = null;
        for (int i = 0; i < 4096; ++i) {
            int oldCoverage;
            int oldLight = previous == null ? 0 : previous.light[i] & 0xFF;
            int n = oldCoverage = previous == null ? 0 : previous.coverage[i] & 0xFF;
            if (oldLight == (light[i] & 0xFF) && oldCoverage == (coverage[i] & 0xFF)) continue;
            if (words == null) {
                words = new long[64];
            }
            int n2 = i >>> 6;
            words[n2] = words[n2] | 1L << (i & 0x3F);
        }
        return words;
    }

    private void applyRebuiltSnapshot(PublishResult rebuilt) {
        if (rebuilt.affected.isEmpty()) {
            return;
        }
        Snapshot old = this.snapshot;
        Long2ObjectOpenHashMap[] nextShards = (Long2ObjectOpenHashMap[])old.shards.clone();
        boolean[] copied = new boolean[64];
        LongIterator it = rebuilt.affected.iterator();
        while (it.hasNext()) {
            long sectionKey = it.nextLong();
            int shardIndex = NeoFloodEngine.snapshotShard(sectionKey);
            if (!copied[shardIndex]) {
                nextShards[shardIndex] = new Long2ObjectOpenHashMap((Long2ObjectMap)nextShards[shardIndex]);
                copied[shardIndex] = true;
            }
            if (rebuilt.emptySections.contains(sectionKey)) {
                nextShards[shardIndex].remove(sectionKey);
                continue;
            }
            SectionLightData section = (SectionLightData)rebuilt.rebuilt.get(sectionKey);
            if (section == null) continue;
            nextShards[shardIndex].put(sectionKey, (Object)section);
        }
        this.snapshot = new Snapshot(nextShards);
        this.snapshotVersion.incrementAndGet();
    }

    private void notifyRebuiltSections(PublishResult rebuilt, boolean highPriority) {
        Consumer<LightChangeBatch> lightCallback;
        if (!rebuilt.changed.isEmpty() && (lightCallback = this.onLightChanges) != null) {
            lightCallback.accept(new LightChangeBatch(rebuilt.changed.sections));
        }
        if (!rebuilt.changedSections.isEmpty()) {
            Consumer<LongOpenHashSet> sectionCallback;
            Consumer<LongOpenHashSet> consumer = sectionCallback = highPriority ? this.onImmediateSectionsChanged : this.onSectionsChanged;
            if (sectionCallback != null) {
                sectionCallback.accept(rebuilt.changedSections);
            }
        }
    }

    private void updateReverseIndex(long sourceKey, PackedSourceData oldData, PackedSourceData newData) {
        LongOpenHashSet bucket;
        long sectionKey;
        LongIterator it;
        if (oldData != null) {
            it = oldData.sectionBits.keySet().iterator();
            while (it.hasNext()) {
                sectionKey = it.nextLong();
                if (newData != null && newData.sectionBits.containsKey(sectionKey) || (bucket = (LongOpenHashSet)this.sourcesBySection.get(sectionKey)) == null) continue;
                bucket.remove(sourceKey);
                if (!bucket.isEmpty()) continue;
                this.sourcesBySection.remove(sectionKey);
            }
        }
        if (newData != null) {
            it = newData.sectionBits.keySet().iterator();
            while (it.hasNext()) {
                sectionKey = it.nextLong();
                bucket = (LongOpenHashSet)this.sourcesBySection.get(sectionKey);
                if (bucket == null) {
                    bucket = new LongOpenHashSet();
                    this.sourcesBySection.put(sectionKey, (Object)bucket);
                }
                bucket.add(sourceKey);
            }
        }
    }

    private void collectCandidateSourcesNearBlockLocked(BlockPos pos, LongOpenHashSet output) {
        int chunkX = pos.m_123341_() >> 4;
        int chunkZ = pos.m_123343_() >> 4;
        int blockX = pos.m_123341_();
        int blockY = pos.m_123342_();
        int blockZ = pos.m_123343_();
        for (int dz = -SOURCE_CHUNK_RADIUS; dz <= SOURCE_CHUNK_RADIUS; ++dz) {
            for (int dx = -SOURCE_CHUNK_RADIUS; dx <= SOURCE_CHUNK_RADIUS; ++dx) {
                LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(ChunkPos.m_45589_((int)(chunkX + dx), (int)(chunkZ + dz)));
                if (bucket == null) continue;
                LongIterator it = bucket.iterator();
                while (it.hasNext()) {
                    int reach;
                    int sz;
                    int sy;
                    int sx;
                    long key = it.nextLong();
                    SourceState source = this.sources.get(key);
                    if (source == null || !source.active || (sx = blockX - source.x) * sx + (sy = blockY - source.y) * sy + (sz = blockZ - source.z) * sz > (reach = source.maxReachBlocks) * reach) continue;
                    output.add(key);
                }
            }
        }
    }

    private long[] selectNearestLocked(LongOpenHashSet keys, Vec3 origin, int limit) {
        int capacity = Math.min(Math.max(0, limit), keys.size());
        if (capacity == 0) {
            return new long[0];
        }
        LongOpenHashSet selection = keys;
        if (keys == this.worldDirtyQueue && keys.size() > capacity * 2) {
            this.selectionCandidates.clear();
            int centerChunkX = Mth.m_14107_((double)origin.f_82479_) >> 4;
            int centerChunkZ = Mth.m_14107_((double)origin.f_82481_) >> 4;
            int target = Math.min(keys.size(), Math.max(capacity * 4, capacity));
            int maxRing = 48;
            for (int ring = 0; ring <= 48 && this.selectionCandidates.size() < target; ++ring) {
                this.collectDirtySourcesOnChunkRingLocked(centerChunkX, centerChunkZ, ring, keys, this.selectionCandidates);
            }
            if (this.selectionCandidates.size() >= capacity) {
                selection = this.selectionCandidates;
            }
        }
        long[] bestKeys = new long[capacity];
        double[] bestDistances = new double[capacity];
        Arrays.fill(bestDistances, Double.POSITIVE_INFINITY);
        int count = 0;
        LongIterator it = selection.iterator();
        while (it.hasNext()) {
            int insert;
            long key = it.nextLong();
            SourceState state = this.sources.get(key);
            if (state == null || !state.active) continue;
            double dx = (double)state.x + 0.5 - origin.f_82479_;
            double dy = (double)state.y + 0.5 - origin.f_82480_;
            double dz = (double)state.z + 0.5 - origin.f_82481_;
            double distance = dx * dx + dy * dy + dz * dz;
            for (insert = Math.min(count, capacity - 1); insert > 0 && distance < bestDistances[insert - 1]; --insert) {
                bestDistances[insert] = bestDistances[insert - 1];
                bestKeys[insert] = bestKeys[insert - 1];
            }
            if (!(distance < bestDistances[insert])) continue;
            bestDistances[insert] = distance;
            bestKeys[insert] = key;
            if (count >= capacity) continue;
            ++count;
        }
        return count == capacity ? bestKeys : Arrays.copyOf(bestKeys, count);
    }

    private void collectDirtySourcesOnChunkRingLocked(int centerChunkX, int centerChunkZ, int ring, LongOpenHashSet dirty, LongOpenHashSet output) {
        if (ring == 0) {
            this.collectDirtySourcesFromChunkLocked(centerChunkX, centerChunkZ, dirty, output);
            return;
        }
        int minX = centerChunkX - ring;
        int maxX = centerChunkX + ring;
        int minZ = centerChunkZ - ring;
        int maxZ = centerChunkZ + ring;
        for (int x = minX; x <= maxX; ++x) {
            this.collectDirtySourcesFromChunkLocked(x, minZ, dirty, output);
            this.collectDirtySourcesFromChunkLocked(x, maxZ, dirty, output);
        }
        for (int z = minZ + 1; z < maxZ; ++z) {
            this.collectDirtySourcesFromChunkLocked(minX, z, dirty, output);
            this.collectDirtySourcesFromChunkLocked(maxX, z, dirty, output);
        }
    }

    private void collectDirtySourcesFromChunkLocked(int chunkX, int chunkZ, LongOpenHashSet dirty, LongOpenHashSet output) {
        LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(ChunkPos.m_45589_((int)chunkX, (int)chunkZ));
        if (bucket == null) {
            return;
        }
        LongIterator it = bucket.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            if (!dirty.contains(key)) continue;
            output.add(key);
        }
    }

    private void indexSourceLocked(SourceState state) {
        long chunkKey = ChunkPos.m_45589_((int)(state.x >> 4), (int)(state.z >> 4));
        LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(chunkKey);
        if (bucket == null) {
            bucket = new LongOpenHashSet();
            this.sourcesByChunk.put(chunkKey, (Object)bucket);
        }
        bucket.add(state.key);
    }

    private void unindexSourceLocked(SourceState state) {
        long chunkKey = ChunkPos.m_45589_((int)(state.x >> 4), (int)(state.z >> 4));
        LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(chunkKey);
        if (bucket == null) {
            return;
        }
        bucket.remove(state.key);
        if (bucket.isEmpty()) {
            this.sourcesByChunk.remove(chunkKey);
        }
    }

    private void rebuildActiveSourceSnapshotLocked() {
        SourceState[] buffer = new SourceState[this.sources.size()];
        int count = 0;
        for (SourceState state : this.sources.values()) {
            if (!state.active) continue;
            buffer[count++] = state;
        }
        this.activeSourceSnapshot = count == buffer.length ? buffer : Arrays.copyOf(buffer, count);
        this.activeSourceSnapshotDirty = false;
    }

    private static int sourceReach(int emission) {
        return Math.min(36, Math.min(FloodRtSettings.radiusCap(), (int)Math.ceil(LightRtMath.getMaxDistance(emission))));
    }

    private static long sectionKeyForBlock(long posKey) {
        return SectionPos.m_123209_((int)(BlockPos.m_121983_((long)posKey) >> 4), (int)(BlockPos.m_122008_((long)posKey) >> 4), (int)(BlockPos.m_122015_((long)posKey) >> 4));
    }

    private static int localSectionIndex(long posKey) {
        return (BlockPos.m_122008_((long)posKey) & 0xF) << 8 | (BlockPos.m_122015_((long)posKey) & 0xF) << 4 | BlockPos.m_121983_((long)posKey) & 0xF;
    }

    private static int snapshotShard(long sectionKey) {
        long value = sectionKey;
        value ^= value >>> 33;
        value *= -49064778989728563L;
        value ^= value >>> 33;
        value *= -4265267296055464877L;
        value ^= value >>> 33;
        return (int)value & 0x3F;
    }

    private static double distanceSq(long a, long b) {
        double dx = BlockPos.m_121983_((long)a) - BlockPos.m_121983_((long)b);
        double dy = BlockPos.m_122008_((long)a) - BlockPos.m_122008_((long)b);
        double dz = BlockPos.m_122015_((long)a) - BlockPos.m_122015_((long)b);
        return dx * dx + dy * dy + dz * dz;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    private static final class Snapshot {
        final Long2ObjectOpenHashMap<SectionLightData>[] shards;

        Snapshot(Long2ObjectOpenHashMap<SectionLightData>[] shards) {
            this.shards = shards;
        }

        static Snapshot empty() {
            Long2ObjectOpenHashMap[] shards = new Long2ObjectOpenHashMap[64];
            for (int i = 0; i < shards.length; ++i) {
                shards[i] = new Long2ObjectOpenHashMap();
            }
            return new Snapshot(shards);
        }

        SectionLightData getSection(long sectionKey) {
            return (SectionLightData)this.shards[NeoFloodEngine.snapshotShard(sectionKey)].get(sectionKey);
        }

        int getLight(long posKey) {
            SectionLightData section = this.getSection(NeoFloodEngine.sectionKeyForBlock(posKey));
            return section == null ? 0 : section.light[NeoFloodEngine.localSectionIndex(posKey)] & 0xFF;
        }

        int getCoverage(long posKey) {
            SectionLightData section = this.getSection(NeoFloodEngine.sectionKeyForBlock(posKey));
            return section == null ? 0 : section.coverage[NeoFloodEngine.localSectionIndex(posKey)] & 0xFF;
        }

        void collectSectionKeys(LongOpenHashSet output) {
            for (Long2ObjectOpenHashMap<SectionLightData> shard : this.shards) {
                output.addAll((LongCollection)shard.keySet());
            }
        }
    }

    private static final class SourceState {
        final long key;
        final int x;
        final int y;
        final int z;
        volatile boolean active;
        volatile int emission;
        volatile int maxReachBlocks;
        volatile int generation;
        volatile int submittedGeneration = -1;
        volatile PackedSourceData result;
        volatile PackedSourceData removedResult;
        volatile long visibilityRevision;

        SourceState(long key) {
            this.key = key;
            this.x = BlockPos.m_121983_((long)key);
            this.y = BlockPos.m_122008_((long)key);
            this.z = BlockPos.m_122015_((long)key);
        }
    }

    private static final class SectionBuildScratch {
        final byte[] light = new byte[4096];
        final byte[] coverage = new byte[4096];
        long[] sourceKeys = new long[16];
        int sourceCount;

        private SectionBuildScratch() {
        }

        void reset(int sourceCapacity) {
            Arrays.fill(this.light, (byte)0);
            Arrays.fill(this.coverage, (byte)0);
            this.sourceCount = 0;
            if (sourceCapacity > this.sourceKeys.length) {
                int capacity;
                for (capacity = this.sourceKeys.length; capacity < sourceCapacity; capacity <<= 1) {
                }
                this.sourceKeys = new long[capacity];
            }
        }
    }

    private static final class PackedSourceData {
        final int originX;
        final int originY;
        final int originZ;
        final byte[] packedLight;
        final byte[] visibilityVolume;
        final Long2ObjectOpenHashMap<long[]> sectionBits;

        PackedSourceData(int originX, int originY, int originZ, byte[] packedLight, byte[] visibilityVolume, Long2ObjectOpenHashMap<long[]> sectionBits) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.packedLight = packedLight;
            this.visibilityVolume = visibilityVolume;
            this.sectionBits = sectionBits;
        }

        static PackedSourceData from(long sourceKey, NeoFloodRtEngine.Result result) {
            int originX = BlockPos.m_121983_((long)sourceKey);
            int originY = BlockPos.m_122008_((long)sourceKey);
            int originZ = BlockPos.m_122015_((long)sourceKey);
            Long2ObjectOpenHashMap sectionBits = new Long2ObjectOpenHashMap();
            for (int wordIndex = 0; wordIndex < result.touchedBits.length; ++wordIndex) {
                int volumeIndex;
                for (long bits = result.touchedBits[wordIndex]; bits != 0L && (volumeIndex = (wordIndex << 6) + Long.numberOfTrailingZeros(bits)) < 389017; bits &= bits - 1L) {
                    int xIndex = volumeIndex % 73;
                    int worldX = originX + xIndex - 36;
                    int yz = volumeIndex / 73;
                    int yIndex = yz / 73;
                    int worldY = originY + yIndex - 36;
                    int zIndex = yz % 73;
                    int worldZ = originZ + zIndex - 36;
                    long posKey = BlockPos.m_121882_((int)worldX, (int)worldY, (int)worldZ);
                    long sectionKey = NeoFloodEngine.sectionKeyForBlock(posKey);
                    long[] localBits = (long[])sectionBits.get(sectionKey);
                    if (localBits == null) {
                        localBits = new long[64];
                        sectionBits.put(sectionKey, (Object)localBits);
                    }
                    int localIndex = NeoFloodEngine.localSectionIndex(posKey);
                    int n = localIndex >>> 6;
                    localBits[n] = localBits[n] | 1L << (localIndex & 0x3F);
                }
            }
            return new PackedSourceData(originX, originY, originZ, result.packedLight, result.visibilityVolume, (Long2ObjectOpenHashMap<long[]>)sectionBits);
        }

        int getLight(long posKey) {
            return this.getLight(BlockPos.m_121983_((long)posKey), BlockPos.m_122008_((long)posKey), BlockPos.m_122015_((long)posKey));
        }

        int getLight(int worldX, int worldY, int worldZ) {
            int index = this.indexFor(worldX, worldY, worldZ);
            if (index < 0) {
                return 0;
            }
            int packed = this.packedLight[index >>> 1] & 0xFF;
            return (index & 1) == 0 ? packed & 0xF : packed >>> 4 & 0xF;
        }

        int getCoverage(long posKey) {
            return this.getCoverage(BlockPos.m_121983_((long)posKey), BlockPos.m_122008_((long)posKey), BlockPos.m_122015_((long)posKey));
        }

        int getCoverage(int worldX, int worldY, int worldZ) {
            int index = this.indexFor(worldX, worldY, worldZ);
            return index < 0 ? 0 : this.visibilityVolume[index] & 0xFF;
        }

        int indexFor(int worldX, int worldY, int worldZ) {
            int dx = worldX - this.originX;
            int dy = worldY - this.originY;
            int dz = worldZ - this.originZ;
            if (dx < -36 || dx > 36 || dy < -36 || dy > 36 || dz < -36 || dz > 36) {
                return -1;
            }
            return (dy + 36) * 5329 + (dz + 36) * 73 + (dx + 36);
        }

        void copyVisibility(int radius, byte[] output) {
            if (radius == 36) {
                System.arraycopy(this.visibilityVolume, 0, output, 0, 389017);
                return;
            }
            int side = radius * 2 + 1;
            int copyRadius = Math.min(radius, 36);
            int targetPlane = side * side;
            for (int y = -copyRadius; y <= copyRadius; ++y) {
                int sourceY = y + 36;
                int targetY = y + radius;
                for (int z = -copyRadius; z <= copyRadius; ++z) {
                    int sourceZ = z + 36;
                    int targetZ = z + radius;
                    int sourceOffset = sourceY * 5329 + sourceZ * 73 + (36 - copyRadius);
                    int targetOffset = targetY * targetPlane + targetZ * side + (radius - copyRadius);
                    System.arraycopy(this.visibilityVolume, sourceOffset, output, targetOffset, copyRadius * 2 + 1);
                }
            }
        }
    }

    private static final class SectionLightData {
        final byte[] light;
        final byte[] coverage;
        final long[] sourceKeys;

        SectionLightData(byte[] light, byte[] coverage, long[] sourceKeys) {
            this.light = light;
            this.coverage = coverage;
            this.sourceKeys = sourceKeys;
        }
    }

    @FunctionalInterface
    public static interface ContributorVisitor {
        public void visit(long var1, int var3, int var4, int var5);
    }

    private static final class TraceCapture {
        final int epoch;
        final SourceState state;
        final int generation;
        final int emission;

        TraceCapture(int epoch, SourceState state, int generation, int emission) {
            this.epoch = epoch;
            this.state = state;
            this.generation = generation;
            this.emission = emission;
        }
    }

    private static final class CompletedTrace {
        final TraceCapture capture;
        final PackedSourceData data;

        CompletedTrace(TraceCapture capture, PackedSourceData data) {
            this.capture = capture;
            this.data = data;
        }
    }

    private static final class PublishResult {
        final LongOpenHashSet affected;
        final Long2ObjectOpenHashMap<SectionLightData> rebuilt;
        final LongOpenHashSet emptySections;
        final ChangedCells changed;
        final LongOpenHashSet changedSections;

        PublishResult(LongOpenHashSet affected, Long2ObjectOpenHashMap<SectionLightData> rebuilt, LongOpenHashSet emptySections, ChangedCells changed, LongOpenHashSet changedSections) {
            this.affected = affected;
            this.rebuilt = rebuilt;
            this.emptySections = emptySections;
            this.changed = changed;
            this.changedSections = changedSections;
        }
    }

    private static final class ChangedCells {
        final Long2ObjectOpenHashMap<long[]> sections = new Long2ObjectOpenHashMap();

        private ChangedCells() {
        }

        long[] wordsFor(long sectionKey) {
            long[] words = (long[])this.sections.get(sectionKey);
            if (words == null) {
                words = new long[64];
                this.sections.put(sectionKey, (Object)words);
            }
            return words;
        }

        boolean isEmpty() {
            return this.sections.isEmpty();
        }
    }

    private static final class SectionBuildOutput {
        final SectionLightData next;
        final long[] dirtyWords;

        SectionBuildOutput(SectionLightData next, long[] dirtyWords) {
            this.next = next;
            this.dirtyWords = dirtyWords;
        }
    }

    public static final class LightChangeBatch {
        private final Long2ObjectOpenHashMap<long[]> sections;

        private LightChangeBatch(Long2ObjectOpenHashMap<long[]> sections) {
            this.sections = sections;
        }

        public int sectionCount() {
            return this.sections.size();
        }

        public void forEachSection(SectionChangeVisitor visitor) {
            if (visitor == null) {
                return;
            }
            ObjectIterator it = this.sections.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)it.next();
                visitor.visit(entry.getLongKey(), (long[])entry.getValue());
            }
        }
    }

    @FunctionalInterface
    public static interface SectionChangeVisitor {
        public void visit(long var1, long[] var3);
    }
}

