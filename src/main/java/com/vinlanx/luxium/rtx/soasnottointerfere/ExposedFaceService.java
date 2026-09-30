/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.rtx.soasnottointerfere;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.rtx.TorchRtxState;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ExposedFaceService {
    private static final double SURFACE_EPSILON = 1.0E-6;
    private static final int REQUESTED_SECTION_CAPTURES_PER_TICK = 2;
    private static final int MAX_DIRTY_BLOCKS_PER_TICK = 2048;
    private static final int MAX_ASYNC_COMMITS_PER_TICK = 64;
    private static final ExposedFaceService INSTANCE = new ExposedFaceService();
    private static final Direction[] FACES = Direction.values();
    private final ConcurrentHashMap<Long, SectionFaceData> sectionFaces = new ConcurrentHashMap();
    private final ConcurrentHashMap<Long, SectionFaceSnapshot> sectionFaceSnapshots = new ConcurrentHashMap();
    private final ConcurrentHashMap<Long, SectionReceiverSnapshot> sectionReceivers = new ConcurrentHashMap();
    private final ConcurrentHashMap<Long, SectionCompleteReceiverSurfaceCache> sectionCompleteReceiverSurfaces = new ConcurrentHashMap();
    private final ConcurrentHashMap<Long, Boolean> fullyCapturedSections = new ConcurrentHashMap();
    private final ConcurrentHashMap<BlockState, ReceiverShapeTemplate> receiverShapeCache = new ConcurrentHashMap();
    private final ConcurrentHashMap.KeySetView<Long, Boolean> pendingDirtyBlocks = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Double> pendingSectionCaptures = new ConcurrentHashMap();
    private final ThreadLocal<SectionCapture> activeCapture = new ThreadLocal();
    private final ExecutorService receiverBuildExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Luxium-ReceiverBuild");
        t.setDaemon(true);
        t.setPriority(3);
        return t;
    });
    private final ConcurrentLinkedQueue<BuiltSectionCommit> completedSectionBuilds = new ConcurrentLinkedQueue();
    private final ConcurrentHashMap<Long, Long> latestSectionBuildRevision = new ConcurrentHashMap();
    private final AtomicLong sectionBuildRevision = new AtomicLong();
    private static final ReceiverSurface[] EMPTY_RECEIVER_SURFACES = new ReceiverSurface[0];

    private ExposedFaceService() {
    }

    public static ExposedFaceService get() {
        return INSTANCE;
    }

    public boolean hasReceiverGeometryChanged(@Nullable ClientLevel level, BlockPos pos, @Nullable BlockState oldState, @Nullable BlockState newState) {
        ReceiverShapeTemplate newTemplate;
        if (level == null || pos == null || oldState == null || newState == null) {
            return true;
        }
        if (oldState == newState || oldState.equals(newState)) {
            return false;
        }
        if (oldState.m_60795_() != newState.m_60795_() || oldState.m_60815_() != newState.m_60815_() || oldState.m_60799_() != newState.m_60799_()) {
            return true;
        }
        ReceiverShapeTemplate oldTemplate = this.resolveReceiverShapeTemplate(oldState, (BlockGetter)level, pos);
        return !oldTemplate.sameGeometry(newTemplate = this.resolveReceiverShapeTemplate(newState, (BlockGetter)level, pos));
    }

    public void clear() {
        this.activeCapture.remove();
        this.sectionFaces.clear();
        this.sectionFaceSnapshots.clear();
        this.sectionReceivers.clear();
        this.sectionCompleteReceiverSurfaces.clear();
        this.fullyCapturedSections.clear();
        this.pendingDirtyBlocks.clear();
        this.pendingSectionCaptures.clear();
        this.completedSectionBuilds.clear();
        this.latestSectionBuildRevision.clear();
    }

    public void beginSectionCapture(@Nullable BlockPos sectionOrigin) {
        if (!ExposedFaceService.isCacheActive()) {
            this.activeCapture.remove();
            return;
        }
        if (sectionOrigin == null) {
            this.activeCapture.remove();
            return;
        }
        this.activeCapture.set(new SectionCapture(ExposedFaceService.sectionKeyFromOrigin(sectionOrigin)));
    }

    public void finishSectionCapture() {
        SectionCapture capture = this.activeCapture.get();
        this.activeCapture.remove();
        if (capture == null) {
            return;
        }
        this.scheduleSectionBuild(capture.sectionKey, capture.detachFaceData(), true);
    }

    public void discardSectionCapture() {
        this.activeCapture.remove();
    }

    public void recordFace(BlockPos pos, Direction face, BlockGetter level, BlockState state) {
        SectionCapture capture = this.activeCapture.get();
        if (capture == null || pos == null || face == null) {
            return;
        }
        if (!this.hasReceiverGeometry(state, level, pos)) {
            return;
        }
        capture.recordFace(pos.m_121878_(), face);
    }

    public List<ReceiverSurface> getCompleteReceiverSurfaces(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive() || level == null) {
            return Collections.emptyList();
        }
        BlockPos pos = BlockPos.m_122022_((long)blockKey);
        BlockState state = level.m_8055_(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        if (template.isEmpty()) {
            return Collections.emptyList();
        }
        byte visibleBoundaryMask = template.hasBoundaryFaces() ? this.getCompleteExposedFaceMask(level, blockKey, state, pos, template) : (byte)-1;
        return template.instantiate(blockKey, visibleBoundaryMask);
    }

    public void forEachCompleteReceiverSurface(@Nullable ClientLevel level, long blockKey, ReceiverSurfaceVisitor visitor) {
        if (!ExposedFaceService.isCacheActive() || level == null || visitor == null) {
            return;
        }
        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        if (this.fullyCapturedSections.containsKey(sectionKey)) {
            SectionFaceSnapshot snapshot = this.sectionFaceSnapshots.get(sectionKey);
            if (snapshot == null || !snapshot.hasCandidateReceivers()) {
                return;
            }
            int localIndex = ExposedFaceService.sectionLocalIndex(BlockPos.m_121983_((long)blockKey) & 0xF, BlockPos.m_122008_((long)blockKey) & 0xF, BlockPos.m_122015_((long)blockKey) & 0xF);
            byte visibleBoundaryMask = snapshot.faceMask(localIndex);
            if (visibleBoundaryMask == 0) {
                return;
            }
            SectionCompleteReceiverSurfaceCache sectionCache = this.sectionCompleteReceiverSurfaces.computeIfAbsent(sectionKey, ignored -> new SectionCompleteReceiverSurfaceCache());
            ReceiverSurface[] cached = sectionCache.surfacesByBlock.get(blockKey);
            if (cached == null) {
                BlockPos pos = BlockPos.m_122022_((long)blockKey);
                BlockState state = level.m_8055_(pos);
                ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
                if (template.isEmpty()) {
                    return;
                }
                cached = template.instantiateArray(blockKey, visibleBoundaryMask);
                ReceiverSurface[] existing = sectionCache.surfacesByBlock.putIfAbsent(blockKey, cached);
                if (existing != null) {
                    cached = existing;
                }
            }
            for (ReceiverSurface surface : cached) {
                visitor.visit(surface.face(), surface.planeCoordinate(), surface.minU(), surface.maxU(), surface.minV(), surface.maxV(), surface.surfaceKey());
            }
            return;
        }
        BlockPos pos = BlockPos.m_122022_((long)blockKey);
        BlockState state = level.m_8055_(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        if (template.isEmpty()) {
            return;
        }
        byte visibleBoundaryMask = template.hasBoundaryFaces() ? (byte)this.getCompleteExposedFaceMask(level, blockKey, state, pos, template) : (byte)-1;
        template.forEachSurface(blockKey, visibleBoundaryMask, visitor);
    }

    public byte getExposedFaceMask(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive()) {
            return 0;
        }
        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        SectionFaceData sectionData = this.sectionFaces.get(sectionKey);
        boolean fullyCaptured = this.fullyCapturedSections.containsKey(sectionKey);
        if (sectionData == null) {
            return fullyCaptured || level == null ? (byte)0 : this.computeExposedFaceMask(level, BlockPos.m_122022_((long)blockKey));
        }
        byte cachedMask = sectionData.faceMask(blockKey);
        if (cachedMask != 0 || fullyCaptured || level == null) {
            return cachedMask;
        }
        return this.computeExposedFaceMask(level, BlockPos.m_122022_((long)blockKey));
    }

    public byte getCompleteExposedFaceMask(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive()) {
            return 0;
        }
        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        if (level == null || this.fullyCapturedSections.containsKey(sectionKey)) {
            return this.getExposedFaceMask(level, blockKey);
        }
        BlockPos pos = BlockPos.m_122022_((long)blockKey);
        BlockState state = level.m_8055_(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        return template.computeFaceMask(state, (BlockGetter)level, pos);
    }

    public void requestSectionCapture(long sectionKey, double priorityDistanceSq) {
        if (!ExposedFaceService.isCacheActive() || this.fullyCapturedSections.containsKey(sectionKey)) {
            return;
        }
        double priority = Double.isFinite(priorityDistanceSq) ? Math.max(0.0, priorityDistanceSq) : Double.MAX_VALUE;
        this.pendingSectionCaptures.merge(sectionKey, priority, Math::min);
    }

    public void refreshAround(ClientLevel level, BlockPos center) {
        if (!ExposedFaceService.isCacheActive() || level == null || center == null) {
            return;
        }
        this.pendingDirtyBlocks.add(center.m_121878_());
    }

    public void refreshAroundNow(ClientLevel level, BlockPos center) {
        if (!ExposedFaceService.isCacheActive() || level == null || center == null) {
            return;
        }
        Long2ObjectOpenHashMap updatedSections = new Long2ObjectOpenHashMap();
        LongOpenHashSet changedBlocksForNotify = new LongOpenHashSet();
        LongOpenHashSet affectedSections = new LongOpenHashSet();
        changedBlocksForNotify.add(center.m_121878_());
        affectedSections.add(ExposedFaceService.sectionKeyForBlock(center.m_121878_()));
        for (Direction face : FACES) {
            affectedSections.add(ExposedFaceService.sectionKeyForBlock(center.m_121945_(face).m_121878_()));
        }
        LongIterator sectionIt = affectedSections.iterator();
        while (sectionIt.hasNext()) {
            long sectionKey = sectionIt.nextLong();
            this.requestSectionCapture(sectionKey, 0.0);
        }
        this.updateLocalBlock(level, center, (Long2ObjectOpenHashMap<Long2ByteOpenHashMap>)updatedSections);
        for (Direction face : FACES) {
            this.updateLocalBlock(level, center.m_121945_(face), (Long2ObjectOpenHashMap<Long2ByteOpenHashMap>)updatedSections);
        }
        boolean changed = false;
        for (Long2ObjectMap.Entry entry : updatedSections.long2ObjectEntrySet()) {
            changed |= this.applySection(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue()));
        }
        if (changed) {
            ExposedFaceService.notifySystemsChanged(changedBlocksForNotify);
        }
    }

    public void bootstrapAround(ClientLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        if (!ExposedFaceService.isCacheActive() || level == null || center == null || horizontalRadius < 0 || verticalRadius < 0) {
            return;
        }
        Long2ObjectOpenHashMap updatedSections = new Long2ObjectOpenHashMap();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radiusSq = horizontalRadius * horizontalRadius;
        for (int y = center.m_123342_() - verticalRadius; y <= center.m_123342_() + verticalRadius; ++y) {
            for (int z = center.m_123343_() - horizontalRadius; z <= center.m_123343_() + horizontalRadius; ++z) {
                int dz = z - center.m_123343_();
                for (int x = center.m_123341_() - horizontalRadius; x <= center.m_123341_() + horizontalRadius; ++x) {
                    int dx = x - center.m_123341_();
                    if (dx * dx + dz * dz > radiusSq) continue;
                    pos.m_122178_(x, y, z);
                    this.updateLocalBlock(level, (BlockPos)pos, (Long2ObjectOpenHashMap<Long2ByteOpenHashMap>)updatedSections);
                }
            }
        }
        boolean changed = false;
        for (Long2ObjectMap.Entry entry : updatedSections.long2ObjectEntrySet()) {
            changed |= this.applySection(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue()));
        }
        if (changed) {
            ExposedFaceService.notifySystemsChanged();
        }
    }

    public void refreshChangedBlocks(ClientLevel level, LongOpenHashSet changedBlocks, int maxBlocks) {
        if (!ExposedFaceService.isCacheActive() || level == null || changedBlocks == null || changedBlocks.isEmpty()) {
            return;
        }
        LongIterator iterator = changedBlocks.iterator();
        while (iterator.hasNext()) {
            this.pendingDirtyBlocks.add(iterator.nextLong());
        }
    }

    public void tick(ClientLevel level) {
        if (!ExposedFaceService.isCacheActive() || level == null) {
            return;
        }
        this.drainCompletedSectionBuilds();
        this.drainRequestedSectionCaptures(level);
        if (this.pendingDirtyBlocks.isEmpty()) {
            return;
        }
        int maxProcess = 2048;
        LongOpenHashSet batch = new LongOpenHashSet();
        Iterator<Long> it = this.pendingDirtyBlocks.iterator();
        while (it.hasNext() && batch.size() < maxProcess) {
            batch.add(it.next());
            it.remove();
        }
        if (batch.isEmpty()) {
            return;
        }
        Long2ObjectOpenHashMap updatedSections = new Long2ObjectOpenHashMap();
        LongOpenHashSet changedBlocksForNotify = new LongOpenHashSet();
        LongIterator batchIt = batch.iterator();
        while (batchIt.hasNext()) {
            long blockKey = batchIt.nextLong();
            BlockPos center = BlockPos.m_122022_((long)blockKey);
            changedBlocksForNotify.add(blockKey);
            this.updateLocalBlock(level, center, (Long2ObjectOpenHashMap<Long2ByteOpenHashMap>)updatedSections);
            for (Direction face : FACES) {
                this.updateLocalBlock(level, center.m_121945_(face), (Long2ObjectOpenHashMap<Long2ByteOpenHashMap>)updatedSections);
            }
        }
        if (updatedSections.isEmpty()) {
            return;
        }
        for (Long2ObjectMap.Entry entry : updatedSections.long2ObjectEntrySet()) {
            this.fullyCapturedSections.remove(entry.getLongKey());
            this.scheduleSectionBuild(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue(), true), false);
        }
        ExposedFaceService.notifySystemsChanged(changedBlocksForNotify);
    }

    private void drainRequestedSectionCaptures(ClientLevel level) {
        for (int processed = 0; processed < 2; ++processed) {
            long selectedKey = Long.MIN_VALUE;
            double selectedPriority = Double.MAX_VALUE;
            for (Map.Entry<Long, Double> entry : this.pendingSectionCaptures.entrySet()) {
                double priority = entry.getValue();
                if (!(priority < selectedPriority)) continue;
                selectedPriority = priority;
                selectedKey = entry.getKey();
            }
            if (selectedKey == Long.MIN_VALUE) {
                return;
            }
            if (!this.pendingSectionCaptures.remove(selectedKey, selectedPriority)) {
                --processed;
                continue;
            }
            if (this.ensureSectionCapturedNow(level, selectedKey)) continue;
            this.pendingSectionCaptures.merge(selectedKey, selectedPriority, Math::min);
            return;
        }
    }

    public void removeChunk(int chunkX, int chunkZ) {
        boolean changed = this.removeChunkEntries(chunkX, chunkZ);
        if (changed) {
            ExposedFaceService.notifySystemsChanged();
        }
    }

    public Map<Long, long[]> snapshotExposedBlocksByChunk() {
        if (!ExposedFaceService.isCacheActive() || this.sectionFaces.isEmpty()) {
            return Collections.emptyMap();
        }
        Long2ObjectOpenHashMap blocksByChunk = new Long2ObjectOpenHashMap();
        for (SectionFaceData sectionData : this.sectionFaces.values()) {
            if (sectionData == null || sectionData.isEmpty()) continue;
            for (Long2ByteMap.Entry entry : sectionData.facesByBlock.long2ByteEntrySet()) {
                if (entry.getByteValue() == 0) continue;
                long blockKey = entry.getLongKey();
                long chunkKey = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)blockKey) >> 4), (int)(BlockPos.m_122015_((long)blockKey) >> 4));
                ((LongOpenHashSet)blocksByChunk.computeIfAbsent(chunkKey, ignored -> new LongOpenHashSet())).add(blockKey);
            }
        }
        if (blocksByChunk.isEmpty()) {
            return Collections.emptyMap();
        }
        HashMap<Long, long[]> snapshot = new HashMap<Long, long[]>(blocksByChunk.size());
        LongIterator longIterator = blocksByChunk.keySet().iterator();
        while (longIterator.hasNext()) {
            long chunkKey = (Long)longIterator.next();
            LongOpenHashSet bucket = (LongOpenHashSet)blocksByChunk.get(chunkKey);
            if (bucket == null || bucket.isEmpty()) continue;
            snapshot.put(chunkKey, bucket.toLongArray());
        }
        return snapshot;
    }

    public SectionFaceSnapshot snapshotSectionFaces(@Nullable ClientLevel level, long sectionKey) {
        if (!ExposedFaceService.isCacheActive()) {
            return SectionFaceSnapshot.empty();
        }
        SectionFaceSnapshot snapshot = this.sectionFaceSnapshots.get(sectionKey);
        return snapshot != null ? snapshot : SectionFaceSnapshot.empty();
    }

    public boolean hasSectionFaceSnapshot(long sectionKey) {
        return this.sectionFaceSnapshots.containsKey(sectionKey);
    }

    public boolean isSectionFullyCaptured(long sectionKey) {
        return this.fullyCapturedSections.containsKey(sectionKey);
    }

    public SectionReceiverSnapshot snapshotSectionReceivers(long sectionKey) {
        SectionReceiverSnapshot snapshot = this.sectionReceivers.get(sectionKey);
        return snapshot != null ? snapshot : SectionReceiverSnapshot.empty();
    }

    private boolean removeChunkEntries(int chunkX, int chunkZ) {
        boolean changed = false;
        for (Long sectionKey : this.sectionFaces.keySet()) {
            if (SectionPos.m_123213_((long)sectionKey) != chunkX || SectionPos.m_123230_((long)sectionKey) != chunkZ) continue;
            changed |= this.sectionFaces.remove(sectionKey) != null;
            this.sectionFaceSnapshots.remove(sectionKey);
            this.sectionReceivers.remove(sectionKey);
            this.sectionCompleteReceiverSurfaces.remove(sectionKey);
        }
        for (Long sectionKey : this.fullyCapturedSections.keySet()) {
            if (SectionPos.m_123213_((long)sectionKey) != chunkX || SectionPos.m_123230_((long)sectionKey) != chunkZ) continue;
            changed |= this.fullyCapturedSections.remove(sectionKey) != null;
        }
        for (Long sectionKey : this.pendingSectionCaptures.keySet()) {
            if (SectionPos.m_123213_((long)sectionKey) != chunkX || SectionPos.m_123230_((long)sectionKey) != chunkZ) continue;
            this.pendingSectionCaptures.remove(sectionKey);
        }
        for (Long sectionKey : this.latestSectionBuildRevision.keySet()) {
            if (SectionPos.m_123213_((long)sectionKey) != chunkX || SectionPos.m_123230_((long)sectionKey) != chunkZ) continue;
            this.latestSectionBuildRevision.remove(sectionKey);
        }
        return changed;
    }

    private boolean ensureSectionCapturedNow(ClientLevel level, long sectionKey) {
        if (this.fullyCapturedSections.containsKey(sectionKey)) {
            return true;
        }
        int sectionX = SectionPos.m_123213_((long)sectionKey);
        int sectionY = SectionPos.m_123225_((long)sectionKey);
        int sectionZ = SectionPos.m_123230_((long)sectionKey);
        LevelChunk chunk = level.m_7726_().m_7131_(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int baseX = SectionPos.m_123223_((int)sectionX);
        int baseY = SectionPos.m_123223_((int)sectionY);
        int baseZ = SectionPos.m_123223_((int)sectionZ);
        int sectionIndex = baseY - level.m_141937_() >> 4;
        LevelChunkSection[] sections = chunk.m_7103_();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            this.scheduleSectionBuild(sectionKey, new SectionFaceData(new Long2ByteOpenHashMap(), true), true);
            return true;
        }
        LevelChunkSection section = sections[sectionIndex];
        if (section == null || section.m_188008_()) {
            this.scheduleSectionBuild(sectionKey, new SectionFaceData(new Long2ByteOpenHashMap(), true), true);
            return true;
        }
        Long2ByteOpenHashMap faces = new Long2ByteOpenHashMap();
        faces.defaultReturnValue((byte)0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int localY = 0; localY < 16; ++localY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    BlockState state = section.m_62982_(localX, localY, localZ);
                    if (state.m_60795_() || state.m_60799_() == RenderShape.INVISIBLE) continue;
                    pos.m_122178_(baseX + localX, baseY + localY, baseZ + localZ);
                    ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, (BlockPos)pos);
                    byte mask = template.computeFaceMask(state, (BlockGetter)level, (BlockPos)pos);
                    if (mask == 0) continue;
                    faces.put(pos.m_121878_(), mask);
                }
            }
        }
        this.scheduleSectionBuild(sectionKey, new SectionFaceData(faces, true), true);
        return true;
    }

    private void updateLocalBlock(ClientLevel level, BlockPos pos, Long2ObjectOpenHashMap<Long2ByteOpenHashMap> updatedSections) {
        long sectionKey = ExposedFaceService.sectionKeyForBlock(pos.m_121878_());
        Long2ByteOpenHashMap sectionMap = (Long2ByteOpenHashMap)updatedSections.get(sectionKey);
        if (sectionMap == null) {
            SectionFaceData current = this.sectionFaces.get(sectionKey);
            sectionMap = current != null ? current.copyFaces() : new Long2ByteOpenHashMap();
            sectionMap.defaultReturnValue((byte)0);
            updatedSections.put(sectionKey, (Object)sectionMap);
        }
        long blockKey = pos.m_121878_();
        byte faceMask = this.computeExposedFaceMask(level, pos);
        if (faceMask == 0) {
            sectionMap.remove(blockKey);
        } else {
            sectionMap.put(blockKey, faceMask);
        }
    }

    private byte computeExposedFaceMask(ClientLevel level, BlockPos pos) {
        BlockState state = level.m_8055_(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        return template.computeFaceMask(state, (BlockGetter)level, pos);
    }

    private byte getCompleteExposedFaceMask(ClientLevel level, long blockKey, BlockState state, BlockPos pos, ReceiverShapeTemplate template) {
        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        if (this.fullyCapturedSections.containsKey(sectionKey)) {
            return this.getExposedFaceMask(level, blockKey);
        }
        return template.computeFaceMask(state, (BlockGetter)level, pos);
    }

    private boolean hasReceiverGeometry(BlockState state, BlockGetter level, BlockPos pos) {
        return !this.resolveReceiverShapeTemplate(state, level, pos).isEmpty();
    }

    private ReceiverShapeTemplate resolveReceiverShapeTemplate(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.m_60795_() || state.m_60799_() == RenderShape.INVISIBLE) {
            return ReceiverShapeTemplate.EMPTY;
        }
        ReceiverShapeTemplate cached = this.receiverShapeCache.get(state);
        if (cached != null) {
            return cached;
        }
        ReceiverShapeTemplate built = ExposedFaceService.buildReceiverShapeTemplate(state, level, pos);
        ReceiverShapeTemplate existing = this.receiverShapeCache.putIfAbsent(state, built);
        return existing != null ? existing : built;
    }

    private static ReceiverShapeTemplate buildReceiverShapeTemplate(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = state.m_60808_(level, pos);
        if (shape.m_83281_()) {
            return ReceiverShapeTemplate.EMPTY;
        }
        List boxes = shape.m_83299_();
        if (boxes.isEmpty()) {
            return ReceiverShapeTemplate.EMPTY;
        }
        ArrayList<ReceiverSurfaceTemplate> surfaces = new ArrayList<ReceiverSurfaceTemplate>();
        for (int boxIndex = 0; boxIndex < boxes.size(); ++boxIndex) {
            AABB box = (AABB)boxes.get(boxIndex);
            for (Direction face : FACES) {
                SurfaceRect initialRect = ExposedFaceService.canonicalFaceRect(face, box);
                if (initialRect == null) continue;
                double planeCoordinate = ExposedFaceService.facePlaneCoordinate(face, box);
                List<SurfaceRect> fragments = new ArrayList<SurfaceRect>();
                fragments.add(initialRect);
                for (int otherIndex = 0; otherIndex < boxes.size() && !fragments.isEmpty(); ++otherIndex) {
                    AABB otherBox;
                    if (otherIndex == boxIndex || !ExposedFaceService.coversFacePlane(face, planeCoordinate, otherBox = (AABB)boxes.get(otherIndex))) continue;
                    fragments = ExposedFaceService.subtractSurfaceRectList(fragments, ExposedFaceService.canonicalFaceRect(face, otherBox));
                }
                boolean boundaryFace = ExposedFaceService.isBoundaryFace(face, planeCoordinate);
                for (SurfaceRect fragment : fragments) {
                    ReceiverSurfaceBounds bounds;
                    if (fragment == null || fragment.width() <= 1.0E-6 || fragment.height() <= 1.0E-6 || (bounds = ExposedFaceService.toLocalFaceBounds(face, fragment)).width() <= 1.0E-6 || bounds.height() <= 1.0E-6) continue;
                    surfaces.add(new ReceiverSurfaceTemplate(face, planeCoordinate, bounds, boundaryFace));
                }
            }
        }
        return surfaces.isEmpty() ? ReceiverShapeTemplate.EMPTY : new ReceiverShapeTemplate(surfaces);
    }

    private static List<SurfaceRect> subtractSurfaceRectList(List<SurfaceRect> sourceRects, @Nullable SurfaceRect coverRect) {
        if (coverRect == null || sourceRects.isEmpty()) {
            return sourceRects;
        }
        ArrayList<SurfaceRect> result = new ArrayList<SurfaceRect>(sourceRects.size());
        for (SurfaceRect rect : sourceRects) {
            ExposedFaceService.subtractSurfaceRect(rect, coverRect, result);
        }
        return result;
    }

    private static void subtractSurfaceRect(SurfaceRect sourceRect, SurfaceRect coverRect, List<SurfaceRect> out) {
        double overlapMinU = Math.max(sourceRect.minU, coverRect.minU);
        double overlapMaxU = Math.min(sourceRect.maxU, coverRect.maxU);
        double overlapMinV = Math.max(sourceRect.minV, coverRect.minV);
        double overlapMaxV = Math.min(sourceRect.maxV, coverRect.maxV);
        if (overlapMaxU - overlapMinU <= 1.0E-6 || overlapMaxV - overlapMinV <= 1.0E-6) {
            out.add(sourceRect);
            return;
        }
        if (overlapMinV - sourceRect.minV > 1.0E-6) {
            out.add(new SurfaceRect(sourceRect.minU, sourceRect.maxU, sourceRect.minV, overlapMinV));
        }
        if (sourceRect.maxV - overlapMaxV > 1.0E-6) {
            out.add(new SurfaceRect(sourceRect.minU, sourceRect.maxU, overlapMaxV, sourceRect.maxV));
        }
        if (overlapMinU - sourceRect.minU > 1.0E-6) {
            out.add(new SurfaceRect(sourceRect.minU, overlapMinU, overlapMinV, overlapMaxV));
        }
        if (sourceRect.maxU - overlapMaxU > 1.0E-6) {
            out.add(new SurfaceRect(overlapMaxU, sourceRect.maxU, overlapMinV, overlapMaxV));
        }
    }

    private static boolean coversFacePlane(Direction face, double planeCoordinate, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> {
                if (Math.abs(box.f_82289_ - planeCoordinate) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.DOWN -> {
                if (Math.abs(box.f_82292_ - planeCoordinate) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.NORTH -> {
                if (Math.abs(box.f_82293_ - planeCoordinate) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.SOUTH -> {
                if (Math.abs(box.f_82290_ - planeCoordinate) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.WEST -> {
                if (Math.abs(box.f_82291_ - planeCoordinate) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.EAST -> Math.abs(box.f_82288_ - planeCoordinate) <= 1.0E-6;
        };
    }

    private static double facePlaneCoordinate(Direction face, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> box.f_82292_;
            case Direction.DOWN -> box.f_82289_;
            case Direction.NORTH -> box.f_82290_;
            case Direction.SOUTH -> box.f_82293_;
            case Direction.WEST -> box.f_82288_;
            case Direction.EAST -> box.f_82291_;
        };
    }

    private static boolean isBoundaryFace(Direction face, double planeCoordinate) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.SOUTH, Direction.EAST -> {
                if (Math.abs(planeCoordinate - 1.0) <= 1.0E-6) {
                    yield true;
                }
                yield false;
            }
            case Direction.DOWN, Direction.NORTH, Direction.WEST -> Math.abs(planeCoordinate) <= 1.0E-6;
        };
    }

    @Nullable
    private static SurfaceRect canonicalFaceRect(Direction face, AABB box) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> ExposedFaceService.surfaceRect(box.f_82288_, box.f_82291_, box.f_82290_, box.f_82293_);
            case Direction.NORTH, Direction.SOUTH -> ExposedFaceService.surfaceRect(box.f_82288_, box.f_82291_, box.f_82289_, box.f_82292_);
            case Direction.WEST, Direction.EAST -> ExposedFaceService.surfaceRect(box.f_82290_, box.f_82293_, box.f_82289_, box.f_82292_);
        };
    }

    @Nullable
    private static SurfaceRect surfaceRect(double minU, double maxU, double minV, double maxV) {
        return maxU - minU <= 1.0E-6 || maxV - minV <= 1.0E-6 ? null : new SurfaceRect(minU, maxU, minV, maxV);
    }

    private static ReceiverSurfaceBounds toLocalFaceBounds(Direction face, SurfaceRect rect) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.SOUTH, Direction.EAST -> new ReceiverSurfaceBounds(rect.minU, rect.maxU, rect.minV, rect.maxV);
            case Direction.DOWN -> new ReceiverSurfaceBounds(rect.minU, rect.maxU, 1.0 - rect.maxV, 1.0 - rect.minV);
            case Direction.NORTH, Direction.WEST -> new ReceiverSurfaceBounds(1.0 - rect.maxU, 1.0 - rect.minU, rect.minV, rect.maxV);
        };
    }

    private void scheduleSectionBuild(long sectionKey, SectionFaceData nextData, boolean fullyCaptured) {
        long revision = this.sectionBuildRevision.incrementAndGet();
        this.latestSectionBuildRevision.put(sectionKey, revision);
        this.receiverBuildExecutor.execute(() -> {
            try {
                SectionFaceSnapshot faceSnapshot = this.buildFaceSnapshot(nextData);
                SectionReceiverSnapshot receiverSnapshot = this.buildReceiverSnapshot(nextData);
                this.completedSectionBuilds.add(new BuiltSectionCommit(sectionKey, revision, nextData, faceSnapshot, receiverSnapshot, fullyCaptured));
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
    }

    private void drainCompletedSectionBuilds() {
        BuiltSectionCommit built;
        boolean changed = false;
        for (int i = 0; i < 64 && (built = this.completedSectionBuilds.poll()) != null; ++i) {
            Long latest = this.latestSectionBuildRevision.get(built.sectionKey);
            if (latest == null || latest != built.revision) continue;
            this.latestSectionBuildRevision.remove(built.sectionKey, built.revision);
            SectionFaceData previous = this.sectionFaces.get(built.sectionKey);
            boolean contentChanged = previous == null ? !built.data.isEmpty() : !previous.sameContent(built.data);
            this.sectionCompleteReceiverSurfaces.remove(built.sectionKey);
            if (built.data.isEmpty()) {
                this.sectionFaces.remove(built.sectionKey);
                this.sectionFaceSnapshots.remove(built.sectionKey);
                this.sectionReceivers.remove(built.sectionKey);
            } else {
                this.sectionFaces.put(built.sectionKey, built.data);
                this.sectionFaceSnapshots.put(built.sectionKey, built.faceSnapshot);
                this.sectionReceivers.put(built.sectionKey, built.receiverSnapshot);
            }
            boolean firstComplete = built.fullyCaptured && this.fullyCapturedSections.put(built.sectionKey, Boolean.TRUE) == null;
            changed |= contentChanged || firstComplete;
        }
        if (changed) {
            ExposedFaceService.notifySystemsChanged();
        }
    }

    private boolean applySection(long sectionKey, SectionFaceData nextData) {
        SectionFaceData previous = this.sectionFaces.get(sectionKey);
        if (previous != null && previous.sameContent(nextData)) {
            return false;
        }
        this.scheduleSectionBuild(sectionKey, nextData, false);
        return true;
    }

    private SectionFaceSnapshot buildFaceSnapshot(SectionFaceData sectionData) {
        if (sectionData == null || sectionData.isEmpty()) {
            return SectionFaceSnapshot.empty();
        }
        long[] renderableSolidBits = new long[64];
        byte[] faceMasks = new byte[4096];
        boolean anyCandidateReceivers = false;
        for (Long2ByteMap.Entry entry : sectionData.facesByBlock.long2ByteEntrySet()) {
            byte faceMask = entry.getByteValue();
            if (faceMask == 0) continue;
            long blockKey = entry.getLongKey();
            int localIndex = ExposedFaceService.sectionLocalIndex(BlockPos.m_121983_((long)blockKey) & 0xF, BlockPos.m_122008_((long)blockKey) & 0xF, BlockPos.m_122015_((long)blockKey) & 0xF);
            int n = localIndex >>> 6;
            renderableSolidBits[n] = renderableSolidBits[n] | 1L << (localIndex & 0x3F);
            faceMasks[localIndex] = faceMask;
            anyCandidateReceivers = true;
        }
        return anyCandidateReceivers ? new SectionFaceSnapshot(renderableSolidBits, faceMasks, true) : SectionFaceSnapshot.empty();
    }

    private SectionReceiverSnapshot buildReceiverSnapshot(SectionFaceData sectionData) {
        if (sectionData == null || sectionData.isEmpty()) {
            return SectionReceiverSnapshot.empty();
        }
        HashMap<Long, Long> faceCells = new HashMap<Long, Long>();
        ArrayList<ReceiverRect> rects = new ArrayList<ReceiverRect>();
        for (Long2ByteMap.Entry entry : sectionData.facesByBlock.long2ByteEntrySet()) {
            long blockKey = entry.getLongKey();
            byte faceMask = entry.getByteValue();
            if (faceMask == 0) continue;
            int localX = BlockPos.m_121983_((long)blockKey) & 0xF;
            int localY = BlockPos.m_122008_((long)blockKey) & 0xF;
            int localZ = BlockPos.m_122015_((long)blockKey) & 0xF;
            for (Direction face : FACES) {
                if ((faceMask & 1 << face.ordinal()) == 0) continue;
                int cellU = ExposedFaceService.localCellU(face, localX, localZ);
                int cellV = ExposedFaceService.localCellV(face, localY, localZ);
                int planeCoordinate = ExposedFaceService.planeCoordinateForFace(face, localX, localY, localZ);
                faceCells.put(ExposedFaceService.receiverCellKey(face.ordinal(), planeCoordinate, cellU, cellV), blockKey);
            }
        }
        HashSet<Long> visited = new HashSet<Long>(faceCells.size());
        for (Map.Entry entry : faceCells.entrySet()) {
            long packedCell = (Long)entry.getKey();
            if (!visited.add(packedCell)) continue;
            int faceOrdinal = ExposedFaceService.unpackFaceOrdinal(packedCell);
            int planeCoordinate = ExposedFaceService.unpackPlaneCoordinate(packedCell);
            int startCellU = ExposedFaceService.unpackCellU(packedCell);
            int startCellV = ExposedFaceService.unpackCellV(packedCell);
            int width = 1;
            while (faceCells.containsKey(ExposedFaceService.receiverCellKey(faceOrdinal, planeCoordinate, startCellU + width, startCellV)) && !visited.contains(ExposedFaceService.receiverCellKey(faceOrdinal, planeCoordinate, startCellU + width, startCellV))) {
                ++width;
            }
            int height = 1;
            while (true) {
                int nextV = startCellV + height;
                boolean rowMatches = true;
                for (int offsetU = 0; offsetU < width; ++offsetU) {
                    long nextKey = ExposedFaceService.receiverCellKey(faceOrdinal, planeCoordinate, startCellU + offsetU, nextV);
                    if (faceCells.containsKey(nextKey) && !visited.contains(nextKey)) continue;
                    rowMatches = false;
                    break;
                }
                if (!rowMatches) break;
                ++height;
            }
            for (int offsetV = 0; offsetV < height; ++offsetV) {
                for (int offsetU = 0; offsetU < width; ++offsetU) {
                    visited.add(ExposedFaceService.receiverCellKey(faceOrdinal, planeCoordinate, startCellU + offsetU, startCellV + offsetV));
                }
            }
            rects.add(new ReceiverRect(Direction.m_122376_((int)faceOrdinal), planeCoordinate, startCellU, startCellU + width, startCellV, startCellV + height, (Long)entry.getValue()));
        }
        return rects.isEmpty() ? SectionReceiverSnapshot.empty() : new SectionReceiverSnapshot(rects);
    }

    private static long receiverCellKey(int faceOrdinal, int planeCoordinate, int cellU, int cellV) {
        long value = ((long)faceOrdinal & 7L) << 61;
        value |= ((long)planeCoordinate & 0x1FFFFFL) << 40;
        value |= ((long)cellU & 0xFFFFFL) << 20;
        return value |= (long)cellV & 0xFFFFFL;
    }

    private static int unpackFaceOrdinal(long key) {
        return (int)(key >>> 61 & 7L);
    }

    private static int unpackPlaneCoordinate(long key) {
        int value = (int)(key >>> 40 & 0x1FFFFFL);
        if ((value & 0x100000) != 0) {
            value |= 0xFFE00000;
        }
        return value;
    }

    private static int unpackCellU(long key) {
        int value = (int)(key >>> 20 & 0xFFFFFL);
        if ((value & 0x80000) != 0) {
            value |= 0xFFF00000;
        }
        return value;
    }

    private static int unpackCellV(long key) {
        int value = (int)(key & 0xFFFFFL);
        if ((value & 0x80000) != 0) {
            value |= 0xFFF00000;
        }
        return value;
    }

    private static int planeCoordinateForFace(Direction face, int localX, int localY, int localZ) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.EAST -> localX + 1;
            case Direction.WEST -> localX;
            case Direction.UP -> localY + 1;
            case Direction.DOWN -> localY;
            case Direction.SOUTH -> localZ + 1;
            case Direction.NORTH -> localZ;
        };
    }

    private static int localCellU(Direction face, int localX, int localZ) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> localX;
            case Direction.WEST, Direction.EAST -> localZ;
        };
    }

    private static int localCellV(Direction face, int localY, int localZ) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> localZ;
            case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> localY;
        };
    }

    private static long sectionKeyFromOrigin(BlockPos sectionOrigin) {
        return SectionPos.m_123209_((int)SectionPos.m_123171_((int)sectionOrigin.m_123341_()), (int)SectionPos.m_123171_((int)sectionOrigin.m_123342_()), (int)SectionPos.m_123171_((int)sectionOrigin.m_123343_()));
    }

    private static long sectionKeyForBlock(long blockKey) {
        return SectionPos.m_123209_((int)SectionPos.m_123171_((int)BlockPos.m_121983_((long)blockKey)), (int)SectionPos.m_123171_((int)BlockPos.m_122008_((long)blockKey)), (int)SectionPos.m_123171_((int)BlockPos.m_122015_((long)blockKey)));
    }

    private static void notifySystemsChanged() {
        ExposedFaceService.notifySystemsChanged(null);
    }

    private static void notifySystemsChanged(@Nullable LongOpenHashSet changedBlocks) {
        ReflectionSystem.get().markWorldDirty();
        if (changedBlocks == null || changedBlocks.isEmpty()) {
            NeoShadowsEngine.markWorldDirty();
        } else {
            NeoShadowsEngine.markWorldDirty(changedBlocks);
        }
        Minecraft.m_91087_().execute(TorchRtxState.get()::requestReflectiveSnapshotRefresh);
    }

    private static int sectionLocalIndex(int localX, int localY, int localZ) {
        return localY << 8 | localZ << 4 | localX;
    }

    private static boolean isCacheActive() {
        return Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled);
    }

    private static long computeSurfaceKey(long blockKey, Direction face, double planeCoordinate, ReceiverSurfaceBounds bounds) {
        long hash = 1469598103934665603L;
        hash = ExposedFaceService.mixSurfaceKey(hash, blockKey);
        hash = ExposedFaceService.mixSurfaceKey(hash, face.ordinal());
        hash = ExposedFaceService.mixSurfaceKey(hash, Double.doubleToLongBits(planeCoordinate));
        hash = ExposedFaceService.mixSurfaceKey(hash, Double.doubleToLongBits(bounds.minU));
        hash = ExposedFaceService.mixSurfaceKey(hash, Double.doubleToLongBits(bounds.maxU));
        hash = ExposedFaceService.mixSurfaceKey(hash, Double.doubleToLongBits(bounds.minV));
        hash = ExposedFaceService.mixSurfaceKey(hash, Double.doubleToLongBits(bounds.maxV));
        return hash;
    }

    private static long mixSurfaceKey(long hash, long value) {
        return (hash ^ value) * 1099511628211L;
    }

    private static final class ReceiverShapeTemplate {
        private static final ReceiverShapeTemplate EMPTY = new ReceiverShapeTemplate(new ReceiverSurfaceTemplate[0]);
        private final ReceiverSurfaceTemplate[] surfaces;
        private final boolean hasBoundaryFaces;

        private ReceiverShapeTemplate(List<ReceiverSurfaceTemplate> surfaces) {
            this(surfaces.toArray(new ReceiverSurfaceTemplate[0]));
        }

        private ReceiverShapeTemplate(ReceiverSurfaceTemplate[] surfaces) {
            this.surfaces = surfaces;
            boolean anyBoundaryFaces = false;
            for (ReceiverSurfaceTemplate surface : surfaces) {
                if (!surface.boundaryFace) continue;
                anyBoundaryFaces = true;
                break;
            }
            this.hasBoundaryFaces = anyBoundaryFaces;
        }

        private boolean isEmpty() {
            return this.surfaces.length == 0;
        }

        private boolean hasBoundaryFaces() {
            return this.hasBoundaryFaces;
        }

        private boolean sameGeometry(ReceiverShapeTemplate other) {
            if (other == null || this.surfaces.length != other.surfaces.length) {
                return false;
            }
            for (int i = 0; i < this.surfaces.length; ++i) {
                ReceiverSurfaceTemplate a = this.surfaces[i];
                ReceiverSurfaceTemplate b = other.surfaces[i];
                if (a.face == b.face && a.boundaryFace == b.boundaryFace && Double.doubleToLongBits(a.planeCoordinate) == Double.doubleToLongBits(b.planeCoordinate) && Double.doubleToLongBits(a.bounds.minU) == Double.doubleToLongBits(b.bounds.minU) && Double.doubleToLongBits(a.bounds.maxU) == Double.doubleToLongBits(b.bounds.maxU) && Double.doubleToLongBits(a.bounds.minV) == Double.doubleToLongBits(b.bounds.minV) && Double.doubleToLongBits(a.bounds.maxV) == Double.doubleToLongBits(b.bounds.maxV)) continue;
                return false;
            }
            return true;
        }

        private byte computeFaceMask(BlockState state, BlockGetter level, BlockPos pos) {
            if (this.surfaces.length == 0) {
                return 0;
            }
            byte mask = 0;
            BlockPos.MutableBlockPos neighbour = null;
            for (ReceiverSurfaceTemplate surface : this.surfaces) {
                if (surface.boundaryFace) {
                    if (neighbour == null) {
                        neighbour = new BlockPos.MutableBlockPos();
                    }
                    neighbour.m_122159_((Vec3i)pos, surface.face);
                    if (!Block.m_152444_((BlockState)state, (BlockGetter)level, (BlockPos)pos, (Direction)surface.face, (BlockPos)neighbour)) continue;
                }
                mask = (byte)(mask | (byte)surface.faceMaskBit);
            }
            return mask;
        }

        private void forEachSurface(long blockKey, byte visibleBoundaryMask, ReceiverSurfaceVisitor visitor) {
            for (ReceiverSurfaceTemplate surface : this.surfaces) {
                if (surface.boundaryFace && (visibleBoundaryMask & surface.faceMaskBit) == 0) continue;
                visitor.visit(surface.face, surface.planeCoordinate, surface.bounds.minU, surface.bounds.maxU, surface.bounds.minV, surface.bounds.maxV, ExposedFaceService.computeSurfaceKey(blockKey, surface.face, surface.planeCoordinate, surface.bounds));
            }
        }

        private ReceiverSurface[] instantiateArray(long blockKey, byte visibleBoundaryMask) {
            if (this.surfaces.length == 0) {
                return EMPTY_RECEIVER_SURFACES;
            }
            ReceiverSurface[] resolved = new ReceiverSurface[this.surfaces.length];
            int count = 0;
            for (ReceiverSurfaceTemplate surface : this.surfaces) {
                if (surface.boundaryFace && (visibleBoundaryMask & surface.faceMaskBit) == 0) continue;
                resolved[count++] = surface.instantiate(blockKey);
            }
            if (count == 0) {
                return EMPTY_RECEIVER_SURFACES;
            }
            if (count == resolved.length) {
                return resolved;
            }
            ReceiverSurface[] compact = new ReceiverSurface[count];
            System.arraycopy(resolved, 0, compact, 0, count);
            return compact;
        }

        private List<ReceiverSurface> instantiate(long blockKey, byte visibleBoundaryMask) {
            if (this.surfaces.length == 0) {
                return Collections.emptyList();
            }
            ArrayList<ReceiverSurface> resolved = null;
            for (ReceiverSurfaceTemplate surface : this.surfaces) {
                if (surface.boundaryFace && (visibleBoundaryMask & surface.faceMaskBit) == 0) continue;
                if (resolved == null) {
                    resolved = new ArrayList<ReceiverSurface>(this.surfaces.length);
                }
                resolved.add(surface.instantiate(blockKey));
            }
            return resolved == null || resolved.isEmpty() ? Collections.emptyList() : resolved;
        }
    }

    private static final class SectionCapture {
        private final long sectionKey;
        private final Long2ByteOpenHashMap facesByBlock = new Long2ByteOpenHashMap();

        private SectionCapture(long sectionKey) {
            this.sectionKey = sectionKey;
            this.facesByBlock.defaultReturnValue((byte)0);
        }

        private void recordFace(long blockKey, Direction face) {
            this.facesByBlock.put(blockKey, (byte)(this.facesByBlock.get(blockKey) | 1 << face.ordinal()));
        }

        private SectionFaceData detachFaceData() {
            return new SectionFaceData(this.facesByBlock, true);
        }
    }

    private static final class SectionFaceData {
        private final Long2ByteOpenHashMap facesByBlock;

        private SectionFaceData(Long2ByteOpenHashMap source) {
            this(source, false);
        }

        private SectionFaceData(Long2ByteOpenHashMap source, boolean takeOwnership) {
            this.facesByBlock = takeOwnership ? source : new Long2ByteOpenHashMap((Long2ByteMap)source);
            this.facesByBlock.defaultReturnValue((byte)0);
        }

        private byte faceMask(long blockKey) {
            return this.facesByBlock.get(blockKey);
        }

        private boolean isEmpty() {
            return this.facesByBlock.isEmpty();
        }

        private Long2ByteOpenHashMap copyFaces() {
            return new Long2ByteOpenHashMap((Long2ByteMap)this.facesByBlock);
        }

        private boolean sameContent(SectionFaceData other) {
            if (this.facesByBlock.size() != other.facesByBlock.size()) {
                return false;
            }
            for (Long2ByteMap.Entry entry : this.facesByBlock.long2ByteEntrySet()) {
                if (other.facesByBlock.get(entry.getLongKey()) == entry.getByteValue()) continue;
                return false;
            }
            return true;
        }
    }

    public static final class SectionFaceSnapshot {
        private static final SectionFaceSnapshot EMPTY = new SectionFaceSnapshot(new long[64], new byte[4096], false);
        private final long[] renderableSolidBits;
        private final byte[] faceMasks;
        private final boolean anyCandidateReceivers;

        private SectionFaceSnapshot(long[] renderableSolidBits, byte[] faceMasks, boolean anyCandidateReceivers) {
            this.renderableSolidBits = renderableSolidBits;
            this.faceMasks = faceMasks;
            this.anyCandidateReceivers = anyCandidateReceivers;
        }

        public boolean hasCandidateReceivers() {
            return this.anyCandidateReceivers;
        }

        public boolean isRenderableSolid(int localIndex) {
            return (this.renderableSolidBits[localIndex >>> 6] & 1L << (localIndex & 0x3F)) != 0L;
        }

        public long word(int wordIndex) {
            return this.renderableSolidBits[wordIndex];
        }

        public int wordCount() {
            return this.renderableSolidBits.length;
        }

        public byte faceMask(int localIndex) {
            return this.faceMasks[localIndex];
        }

        public static SectionFaceSnapshot empty() {
            return EMPTY;
        }
    }

    private static final class SectionCompleteReceiverSurfaceCache {
        final ConcurrentHashMap<Long, ReceiverSurface[]> surfacesByBlock = new ConcurrentHashMap();

        private SectionCompleteReceiverSurfaceCache() {
        }
    }

    public static final class ReceiverSurface {
        private final long blockKey;
        private final Direction face;
        private final double planeCoordinate;
        private final double minU;
        private final double maxU;
        private final double minV;
        private final double maxV;
        private final long surfaceKey;

        private ReceiverSurface(long blockKey, Direction face, double planeCoordinate, double minU, double maxU, double minV, double maxV, long surfaceKey) {
            this.blockKey = blockKey;
            this.face = face;
            this.planeCoordinate = planeCoordinate;
            this.minU = minU;
            this.maxU = maxU;
            this.minV = minV;
            this.maxV = maxV;
            this.surfaceKey = surfaceKey;
        }

        public long blockKey() {
            return this.blockKey;
        }

        public Direction face() {
            return this.face;
        }

        public double planeCoordinate() {
            return this.planeCoordinate;
        }

        public double minU() {
            return this.minU;
        }

        public double maxU() {
            return this.maxU;
        }

        public double minV() {
            return this.minV;
        }

        public double maxV() {
            return this.maxV;
        }

        public long surfaceKey() {
            return this.surfaceKey;
        }
    }

    @FunctionalInterface
    public static interface ReceiverSurfaceVisitor {
        public void visit(Direction var1, double var2, double var4, double var6, double var8, double var10, long var12);
    }

    public static final class SectionReceiverSnapshot {
        private static final SectionReceiverSnapshot EMPTY = new SectionReceiverSnapshot(Collections.emptyList());
        private final List<ReceiverRect> rects;

        private SectionReceiverSnapshot(List<ReceiverRect> rects) {
            this.rects = List.copyOf(rects);
        }

        public List<ReceiverRect> rects() {
            return this.rects;
        }

        public boolean isEmpty() {
            return this.rects.isEmpty();
        }

        public static SectionReceiverSnapshot empty() {
            return EMPTY;
        }
    }

    private record SurfaceRect(double minU, double maxU, double minV, double maxV) {
        double width() {
            return this.maxU - this.minU;
        }

        double height() {
            return this.maxV - this.minV;
        }
    }

    private record ReceiverSurfaceBounds(double minU, double maxU, double minV, double maxV) {
        double width() {
            return this.maxU - this.minU;
        }

        double height() {
            return this.maxV - this.minV;
        }
    }

    private static final class ReceiverSurfaceTemplate {
        private final Direction face;
        private final int faceMaskBit;
        private final double planeCoordinate;
        private final ReceiverSurfaceBounds bounds;
        private final boolean boundaryFace;

        private ReceiverSurfaceTemplate(Direction face, double planeCoordinate, ReceiverSurfaceBounds bounds, boolean boundaryFace) {
            this.face = face;
            this.faceMaskBit = 1 << face.ordinal();
            this.planeCoordinate = planeCoordinate;
            this.bounds = bounds;
            this.boundaryFace = boundaryFace;
        }

        private ReceiverSurface instantiate(long blockKey) {
            return new ReceiverSurface(blockKey, this.face, this.planeCoordinate, this.bounds.minU, this.bounds.maxU, this.bounds.minV, this.bounds.maxV, ExposedFaceService.computeSurfaceKey(blockKey, this.face, this.planeCoordinate, this.bounds));
        }
    }

    private static final class BuiltSectionCommit {
        final long sectionKey;
        final long revision;
        final SectionFaceData data;
        final SectionFaceSnapshot faceSnapshot;
        final SectionReceiverSnapshot receiverSnapshot;
        final boolean fullyCaptured;

        BuiltSectionCommit(long sectionKey, long revision, SectionFaceData data, SectionFaceSnapshot faceSnapshot, SectionReceiverSnapshot receiverSnapshot, boolean fullyCaptured) {
            this.sectionKey = sectionKey;
            this.revision = revision;
            this.data = data;
            this.faceSnapshot = faceSnapshot;
            this.receiverSnapshot = receiverSnapshot;
            this.fullyCaptured = fullyCaptured;
        }
    }

    public static final class ReceiverRect {
        private final Direction face;
        private final int planeCoordinate;
        private final int minCellU;
        private final int maxCellU;
        private final int minCellV;
        private final int maxCellV;
        private final long anchorBlockKey;

        private ReceiverRect(Direction face, int planeCoordinate, int minCellU, int maxCellU, int minCellV, int maxCellV, long anchorBlockKey) {
            this.face = face;
            this.planeCoordinate = planeCoordinate;
            this.minCellU = minCellU;
            this.maxCellU = maxCellU;
            this.minCellV = minCellV;
            this.maxCellV = maxCellV;
            this.anchorBlockKey = anchorBlockKey;
        }

        public Direction face() {
            return this.face;
        }

        public int planeCoordinate() {
            return this.planeCoordinate;
        }

        public int minCellU() {
            return this.minCellU;
        }

        public int maxCellU() {
            return this.maxCellU;
        }

        public int minCellV() {
            return this.minCellV;
        }

        public int maxCellV() {
            return this.maxCellV;
        }

        public long anchorBlockKey() {
            return this.anchorBlockKey;
        }
    }
}

