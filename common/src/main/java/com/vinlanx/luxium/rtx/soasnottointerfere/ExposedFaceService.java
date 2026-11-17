package com.vinlanx.luxium.rtx.soasnottointerfere;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.rtx.TorchRtxState;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
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
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
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
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.Vec3i;

public final class ExposedFaceService {
    private static final double SURFACE_EPSILON = 1.0E-6; private static final int REQUESTED_SECTION_CAPTURES_PER_TICK = 2; private static final int MAX_DIRTY_BLOCKS_PER_TICK = 2048; private static final int MAX_ASYNC_COMMITS_PER_TICK = 64; private static final ExposedFaceService INSTANCE = new ExposedFaceService();
    private static final Direction[] FACES = Direction.values();

    private final ConcurrentHashMap<Long, ExposedFaceService.SectionFaceData> sectionFaces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExposedFaceService.SectionFaceSnapshot> sectionFaceSnapshots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExposedFaceService.SectionReceiverSnapshot> sectionReceivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExposedFaceService.SectionCompleteReceiverSurfaceCache> sectionCompleteReceiverSurfaces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> fullyCapturedSections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockState, ExposedFaceService.ReceiverShapeTemplate> receiverShapeCache = new ConcurrentHashMap<>();
    private final KeySetView<Long, Boolean> pendingDirtyBlocks = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<Long, Double> pendingSectionCaptures = new ConcurrentHashMap<>();
    private final ThreadLocal<ExposedFaceService.SectionCapture> activeCapture = new ThreadLocal<>();

    private final ExecutorService receiverBuildExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Luxium-ReceiverBuild");
        t.setDaemon(true);
        t.setPriority(3);
        return t;
    });
    private final ConcurrentLinkedQueue<ExposedFaceService.BuiltSectionCommit> completedSectionBuilds = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Long> latestSectionBuildRevision = new ConcurrentHashMap<>();
    private final AtomicLong sectionBuildRevision = new AtomicLong();
    private ExposedFaceService()
    {
    } public static ExposedFaceService get() {


 return INSTANCE; } public boolean hasReceiverGeometryChanged(@Nullable ClientLevel level, BlockPos pos, @Nullable BlockState oldState, @Nullable BlockState newState) {






        if (level == null || pos == null || oldState == null || newState == null) {
            return true;
        }
        if (oldState == newState || oldState.equals(newState)) {
            return false;
        }
        if (oldState.isAir() != newState.isAir() || oldState.canOcclude
                () != newState.canOcclude() || oldState.getRenderShape
                () != newState.getRenderShape()) {
            return true;
        }

        ReceiverShapeTemplate oldTemplate = this.resolveReceiverShapeTemplate(oldState, (BlockGetter)level, pos);
        ReceiverShapeTemplate newTemplate = this.resolveReceiverShapeTemplate(newState, (BlockGetter)level, pos);
        return !oldTemplate.sameGeometry(newTemplate);
    } public void clear() {


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
} public void beginSectionCapture(@Nullable BlockPos sectionOrigin) {
        if (!ExposedFaceService.isCacheActive

()) {
            this.activeCapture.remove();
            return;
        }

        if (sectionOrigin == null) {
            this.activeCapture.remove();
            return;
        }

        this.activeCapture.set(new SectionCapture(ExposedFaceService.sectionKeyFromOrigin(sectionOrigin)));
    } public void finishSectionCapture() {
        SectionCapture

        capture = this.activeCapture.get();
        this.activeCapture.remove();
        if (capture == null) {
            return;
        }

        this.scheduleSectionBuild(capture.sectionKey, capture.detachFaceData(), true);
    } public void discardSectionCapture() {


 this.activeCapture.remove();
} public void recordFace(BlockPos pos, Direction face, BlockGetter level, BlockState state) {
        SectionCapture

        capture = this.activeCapture.get();
        if (capture == null || pos == null || face == null) {
            return;
        }
        if (!this.hasReceiverGeometry(state, level, pos)) {
            return;
        }

        capture.recordFace(pos.asLong(), face);
    } public List<ReceiverSurface> getCompleteReceiverSurfaces(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive

() || level == null) {
            return Collections.emptyList();
        }

        BlockPos pos = BlockPos.of((long)blockKey);
        BlockState state = level.getBlockState(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        if (template.isEmpty()) {
            return Collections.emptyList();
        }

        byte visibleBoundaryMask = template.hasBoundaryFaces() ? this.getCompleteExposedFaceMask
                (level, blockKey, state, pos, template) :
                (byte)-1;
        return template.instantiate(blockKey, visibleBoundaryMask);
    } public void forEachCompleteReceiverSurface(@Nullable ClientLevel level, long blockKey, ExposedFaceService.ReceiverSurfaceVisitor visitor) { if (!(isCacheActive




() && level != null && visitor != null)) {
 return;
 }
 long
 sectionKey = sectionKeyForBlock(blockKey);
if (this.fullyCapturedSections.containsKey(sectionKey)) { ExposedFaceService.SectionFaceSnapshot
 snapshot = this.sectionFaceSnapshots.get(sectionKey);
if (!(snapshot != null && snapshot.hasCandidateReceivers())) {
return;
}
 int
 localIndex = sectionLocalIndex(BlockPos.getX(blockKey) & 15, BlockPos.getY(blockKey) & 15, BlockPos.getZ
 (blockKey) & 15); byte
 visibleBoundaryMask = snapshot.faceMask(localIndex);
if (!(visibleBoundaryMask != 0)) {
 return;
 }
 ExposedFaceService.SectionCompleteReceiverSurfaceCache
 sectionCache = this.sectionCompleteReceiverSurfaces .computeIfAbsent(
 sectionKey, ignored ->
 new ExposedFaceService.SectionCompleteReceiverSurfaceCache()); ExposedFaceService.ReceiverSurface[]
cached = sectionCache.surfacesByBlock.get(blockKey);
if (cached == null) { BlockPos
 pos = BlockPos.of(blockKey); BlockState
state = level.getBlockState(pos); ExposedFaceService.ReceiverShapeTemplate
 template = this.resolveReceiverShapeTemplate(state, level, pos);
 if (template.isEmpty()) {
return; }


cached = template.instantiateArray(blockKey, visibleBoundaryMask); ExposedFaceService.ReceiverSurface[]
existing = sectionCache.surfacesByBlock.putIfAbsent(blockKey, cached);
if (existing != null) {
 cached = existing; } } for (ExposedFaceService.ReceiverSurface surface :



cached) {
visitor.visit(surface.face
(), surface.planeCoordinate
 (), surface.minU
 (), surface.maxU
 (), surface.minV
 (), surface.maxV
 (), surface.surfaceKey
 () ); }

 return; } BlockPos


 pos = BlockPos.of(blockKey); BlockState
state = level.getBlockState(pos); ExposedFaceService.ReceiverShapeTemplate
 template = this.resolveReceiverShapeTemplate(state, level, pos);
 if (template.isEmpty()) {
return; } byte visibleBoundaryMask = template.hasBoundaryFaces


 () ? this.getCompleteExposedFaceMask
 (level, blockKey, state, pos, template) :
 -1;
 template.forEachSurface(blockKey, visibleBoundaryMask, visitor);
 } public byte getExposedFaceMask(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive

()) {
            return 0;
        }

        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        SectionFaceData sectionData = this.sectionFaces.get(sectionKey);
        boolean fullyCaptured = this.fullyCapturedSections.containsKey(sectionKey);
        if (sectionData == null) {
            return fullyCaptured || level == null ? (byte)0 : this.computeExposedFaceMask(level, BlockPos.of((long)blockKey));
        }

        byte cachedMask = sectionData.faceMask(blockKey);
        if (cachedMask != 0 || fullyCaptured || level == null) {
            return cachedMask;
        }

        return this.computeExposedFaceMask(level, BlockPos.of((long)blockKey));
    } public byte getCompleteExposedFaceMask(@Nullable ClientLevel level, long blockKey) {
        if (!ExposedFaceService.isCacheActive

()) {
            return 0;
        }

        long sectionKey = ExposedFaceService.sectionKeyForBlock(blockKey);
        if (level == null || this.fullyCapturedSections.containsKey(sectionKey)) {
            return this.getExposedFaceMask(level, blockKey);
        }

        BlockPos pos = BlockPos.of((long)blockKey);
        BlockState state = level.getBlockState(pos);
        ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate(state, (BlockGetter)level, pos);
        return template.computeFaceMask(state, (BlockGetter)level, pos);
    } public void requestSectionCapture(long sectionKey, double priorityDistanceSq) {
        if (!ExposedFaceService.isCacheActive

() || this.fullyCapturedSections.containsKey(sectionKey)) {
            return;
        }
        double priority = Double.isFinite(priorityDistanceSq) ? Math.max
                (0.0, priorityDistanceSq) : Double
                .MAX_VALUE;
        this.pendingSectionCaptures.merge(sectionKey, priority, Math::min);
    } public void refreshAround(ClientLevel level, BlockPos center) {
        if (!ExposedFaceService.isCacheActive

() || level == null || center == null) {
            return;
        }
        this.pendingDirtyBlocks.add(center.asLong());
    } public void refreshAroundNow(ClientLevel level, BlockPos center) {
        if (!ExposedFaceService.isCacheActive

() || level == null || center == null) {
            return;
        }

        Long2ObjectOpenHashMap<Long2ByteOpenHashMap> updatedSections = new Long2ObjectOpenHashMap<>();

        LongOpenHashSet changedBlocksForNotify = new LongOpenHashSet();
        LongOpenHashSet affectedSections = new LongOpenHashSet();
        changedBlocksForNotify.add(center.asLong());
        affectedSections.add(ExposedFaceService.sectionKeyForBlock(center.asLong()));
        for (Direction face : FACES) {
            affectedSections.add(ExposedFaceService.sectionKeyForBlock(center.relative(face).asLong()));
        }
        LongIterator sectionIt = affectedSections.iterator();
        while (sectionIt.hasNext()) {
            long sectionKey = sectionIt.nextLong();
            this.requestSectionCapture(sectionKey, 0.0);
        }

        this.updateLocalBlock(level, center, updatedSections);
        for (Direction face : FACES) {
            this.updateLocalBlock(level, center.relative(face), updatedSections);
        }

        boolean changed = false;

        for (Long2ObjectMap.Entry<Long2ByteOpenHashMap> entry : updatedSections.long2ObjectEntrySet()) {
            changed |= this.applySection(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue()));
        }
        if (changed) {
            ExposedFaceService.notifySystemsChanged(changedBlocksForNotify);
        }
    } public void bootstrapAround(ClientLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        if (!ExposedFaceService.isCacheActive

() || level == null || center == null || horizontalRadius < 0 || verticalRadius < 0) {

            return;
        }

        Long2ObjectOpenHashMap<Long2ByteOpenHashMap> updatedSections = new Long2ObjectOpenHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radiusSq = horizontalRadius * horizontalRadius;

        for (int y = center.getY() - verticalRadius; y <= center.getY() + verticalRadius; ++y) {
            for (int z = center.getZ() - horizontalRadius; z <= center.getZ() + horizontalRadius; ++z) {
                int dz = z - center.getZ();
                for (int x = center.getX() - horizontalRadius; x <= center.getX() + horizontalRadius; ++x) {
                    int dx = x - center.getX();
                    if (dx * dx + dz * dz > radiusSq)
                            continue;

                    pos.set(x, y, z);
                    this.updateLocalBlock(level, (BlockPos)pos, updatedSections);
                }
            }
        }

        boolean changed = false;

        for (Long2ObjectMap.Entry<Long2ByteOpenHashMap> entry : updatedSections.long2ObjectEntrySet()) {
            changed |= this.applySection(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue()));
        }
        if (changed) {
            ExposedFaceService.notifySystemsChanged();
        }
    } public void refreshChangedBlocks(ClientLevel level, LongOpenHashSet changedBlocks, int maxBlocks) {
        if (!ExposedFaceService.isCacheActive

() || level == null || changedBlocks == null || changedBlocks.isEmpty()) {
            return;
        }
        LongIterator iterator = changedBlocks.iterator();
        while (iterator.hasNext()) {
            this.pendingDirtyBlocks.add(iterator.nextLong());
        }
    } public void tick(ClientLevel level) {
        if (!ExposedFaceService.isCacheActive

() || level == null) {
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

        Long2ObjectOpenHashMap<Long2ByteOpenHashMap> updatedSections = new Long2ObjectOpenHashMap<>();

        LongOpenHashSet changedBlocksForNotify = new LongOpenHashSet();

        LongIterator batchIt = batch.iterator();
        while (batchIt.hasNext()) {
            long blockKey = batchIt.nextLong();
            BlockPos center = BlockPos.of((long)blockKey);
            changedBlocksForNotify.add(blockKey);

            this.updateLocalBlock(level, center, updatedSections);
            for (Direction face : FACES) {
                this.updateLocalBlock(level, center.relative(face), updatedSections);
            }
        }

        if (updatedSections.isEmpty()) {
            return;
        }

        for (Long2ObjectMap.Entry<Long2ByteOpenHashMap> entry : updatedSections.long2ObjectEntrySet()) {
            this.fullyCapturedSections.remove(entry.getLongKey());
            this.scheduleSectionBuild(entry.getLongKey(), new SectionFaceData((Long2ByteOpenHashMap)entry.getValue(), true), false);
        }

        ExposedFaceService.notifySystemsChanged(changedBlocksForNotify);
    } private void drainRequestedSectionCaptures(ClientLevel level) { for (int


 processed = 0; processed < 2; processed++) { long
 selectedKey = Long.MIN_VALUE; double
selectedPriority = Double.MAX_VALUE; for (java.util.Map.Entry<Long, Double> entry :
 this.pendingSectionCaptures.entrySet()) { double
 priority = entry.getValue();
if (priority < selectedPriority) {
 selectedPriority = priority;
 selectedKey = entry.getKey(); }

}
if (selectedKey == Long.MIN_VALUE) {
return; }

if (!this.pendingSectionCaptures.remove(selectedKey, selectedPriority)) {
 processed--;
 continue; }

 if (!this.ensureSectionCapturedNow(level, selectedKey)) {

 this.pendingSectionCaptures.merge(selectedKey, selectedPriority, Math::min);
 return; } }


 } public void removeChunk(int chunkX, int chunkZ) { boolean


 changed = this.removeChunkEntries(chunkX, chunkZ);

if (changed) { notifySystemsChanged
 (); }

 } public Map<Long, long[]> snapshotExposedBlocksByChunk() {
        if (!ExposedFaceService.isCacheActive

() || this.sectionFaces.isEmpty()) {
            return Collections.emptyMap();
        }

        Long2ObjectOpenHashMap<LongOpenHashSet> blocksByChunk = new Long2ObjectOpenHashMap<LongOpenHashSet>();
        for (SectionFaceData sectionData : this.sectionFaces.values()) {
            if (sectionData == null || sectionData.isEmpty())
                    continue;


            for (Long2ByteMap.Entry entry : sectionData.facesByBlock.long2ByteEntrySet()) {
                if (entry.getByteValue() == 0)
                        continue;


                long blockKey = entry.getLongKey();
                long chunkKey = ChunkPos.asLong((int)(BlockPos.getX((long)blockKey) >> 4), (int)(BlockPos.getZ((long)blockKey) >> 4));
                blocksByChunk.computeIfAbsent(chunkKey, ignored -> new LongOpenHashSet()).add(blockKey);
            }
        }

        if (blocksByChunk.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, long[]> snapshot = new HashMap<Long, long[]>(blocksByChunk.size());
        for (long chunkKey : blocksByChunk.keySet()) {
            LongOpenHashSet bucket = (LongOpenHashSet)blocksByChunk.get(chunkKey);
            if (bucket != null && !bucket.isEmpty()) {
                snapshot.put(chunkKey, bucket.toLongArray());
            }
        }
        return snapshot;
    } public ExposedFaceService.SectionFaceSnapshot snapshotSectionFaces(@Nullable ClientLevel level, long sectionKey) { if (!isCacheActive


()) {
return ExposedFaceService.SectionFaceSnapshot.empty(); } ExposedFaceService.SectionFaceSnapshot

 snapshot = this.sectionFaceSnapshots.get(sectionKey);
return snapshot != null ? snapshot : ExposedFaceService.SectionFaceSnapshot.empty();  } public boolean hasSectionFaceSnapshot(long sectionKey) {



 return this.sectionFaceSnapshots.containsKey(sectionKey); } public boolean isSectionFullyCaptured(long sectionKey) {



 return this.fullyCapturedSections.containsKey(sectionKey); } public ExposedFaceService.SectionReceiverSnapshot snapshotSectionReceivers(long sectionKey) { ExposedFaceService.SectionReceiverSnapshot



 snapshot = this.sectionReceivers.get(sectionKey);
return snapshot != null ? snapshot : ExposedFaceService.SectionReceiverSnapshot.empty(); } private boolean removeChunkEntries(int chunkX, int chunkZ) { boolean



 changed = false; for (Long sectionKey :
this.sectionFaces.keySet()) {
if (SectionPos.x(sectionKey) == chunkX && SectionPos.z(sectionKey) == chunkZ) {
 changed |= this.sectionFaces.remove(sectionKey) != null;
 this.sectionFaceSnapshots.remove(sectionKey);
this.sectionReceivers.remove(sectionKey);
this.sectionCompleteReceiverSurfaces.remove(sectionKey); }

} for (Long sectionKey :
this.fullyCapturedSections.keySet()) {
if (SectionPos.x(sectionKey) == chunkX && SectionPos.z(sectionKey) == chunkZ) {
 changed |= this.fullyCapturedSections.remove(sectionKey) != null; }

 } for (Long sectionKey :
this.pendingSectionCaptures.keySet()) {
if (SectionPos.x(sectionKey) == chunkX && SectionPos.z(sectionKey) == chunkZ) {
 this.pendingSectionCaptures.remove(sectionKey); }

} for (Long sectionKey :

this.latestSectionBuildRevision.keySet()) {
if (SectionPos.x(sectionKey) == chunkX && SectionPos.z(sectionKey) == chunkZ) {
 this.latestSectionBuildRevision.remove(sectionKey); }

}
return changed; } private boolean ensureSectionCapturedNow(ClientLevel level, long sectionKey) {



        if (this.fullyCapturedSections.containsKey(sectionKey)) {
            return true;
        }

        int sectionX = SectionPos.x((long)sectionKey);
        int sectionY = SectionPos.y((long)sectionKey);
        int sectionZ = SectionPos.z((long)sectionKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }

        int baseX = SectionPos.sectionToBlockCoord((int)sectionX);
        int baseY = SectionPos.sectionToBlockCoord((int)sectionY);
        int baseZ = SectionPos.sectionToBlockCoord((int)sectionZ);
        int sectionIndex = baseY - level.getMinBuildHeight() >> 4;
        LevelChunkSection[] sections = chunk.getSections();

        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            this.scheduleSectionBuild(sectionKey, new SectionFaceData(new Long2ByteOpenHashMap(), true), true);
            return true;
        }

        LevelChunkSection section = sections[sectionIndex];
        if (section == null || section.hasOnlyAir()) {
            this.scheduleSectionBuild(sectionKey, new SectionFaceData(new Long2ByteOpenHashMap(), true), true);
            return true;
        }

        Long2ByteOpenHashMap faces = new Long2ByteOpenHashMap();
        faces.defaultReturnValue((byte)0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int localY = 0; localY < 16; ++localY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    BlockState state = section.getBlockState(localX, localY, localZ);
                    if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE)
                            continue;

                    pos.set(baseX + localX, baseY + localY, baseZ + localZ);
                    ReceiverShapeTemplate template = this.resolveReceiverShapeTemplate
                            (state, (BlockGetter)level, (BlockPos)pos);
                    byte mask = template.computeFaceMask(state, (BlockGetter)level, (BlockPos)pos);
                    if (mask != 0) {
                        faces.put(pos.asLong(), mask);
                    }
                }
            }
        }

        this.scheduleSectionBuild(sectionKey, new SectionFaceData(faces, true), true);
        return true;
    } private void updateLocalBlock(ClientLevel level, BlockPos pos, Long2ObjectOpenHashMap<Long2ByteOpenHashMap> updatedSections) { long




 sectionKey = sectionKeyForBlock(pos.asLong()); Long2ByteOpenHashMap
sectionMap = updatedSections.get(sectionKey);
if (sectionMap == null) { ExposedFaceService.SectionFaceData
 current = this.sectionFaces.get(sectionKey); sectionMap = current
 != null ? current.copyFaces() : new Long2ByteOpenHashMap();
 sectionMap.defaultReturnValue((byte)0);
updatedSections.put(sectionKey, sectionMap); } long


 blockKey = pos.asLong(); byte
faceMask = this.computeExposedFaceMask(level, pos);
if (faceMask == 0) {
 sectionMap.remove(blockKey); } else {

 sectionMap.put(blockKey, faceMask); }

 } private byte computeExposedFaceMask(ClientLevel level, BlockPos pos) { BlockState


 state = level.getBlockState(pos); ExposedFaceService.ReceiverShapeTemplate
 template = this.resolveReceiverShapeTemplate(state, level, pos);
 return template.computeFaceMask(state, level, pos); } private byte getCompleteExposedFaceMask(ClientLevel level, long blockKey, BlockState state, BlockPos pos, ExposedFaceService.ReceiverShapeTemplate template) { long







 sectionKey = sectionKeyForBlock(blockKey);
if (this.fullyCapturedSections.containsKey(sectionKey)) {
return this.getExposedFaceMask(level, blockKey); }

 return template.computeFaceMask(state, level, pos); } private boolean hasReceiverGeometry(BlockState state, BlockGetter level, BlockPos pos) {



 return !this.resolveReceiverShapeTemplate(state, level, pos).isEmpty(); } private ExposedFaceService.ReceiverShapeTemplate resolveReceiverShapeTemplate(BlockState state, BlockGetter level, BlockPos pos) {



 if (!(!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE)) {
return ExposedFaceService.ReceiverShapeTemplate.EMPTY; } ExposedFaceService.ReceiverShapeTemplate


 cached = this.receiverShapeCache.get(state);
if (cached != null) {
 return cached; } ExposedFaceService.ReceiverShapeTemplate


 built = buildReceiverShapeTemplate(state, level, pos); ExposedFaceService.ReceiverShapeTemplate
 existing = this.receiverShapeCache.putIfAbsent(state, built);
return existing != null ? existing : built;  } private static ExposedFaceService.ReceiverShapeTemplate buildReceiverShapeTemplate(BlockState state, BlockGetter level, BlockPos pos) { VoxelShape



 shape = state.getShape(level, pos);
if (shape.isEmpty()) {
return ExposedFaceService.ReceiverShapeTemplate.EMPTY; } List<AABB>


boxes = shape.toAabbs();
if (boxes.isEmpty()) {
return ExposedFaceService.ReceiverShapeTemplate.EMPTY; } List<ExposedFaceService.ReceiverSurfaceTemplate>


surfaces = new ArrayList<>(); for (int
 boxIndex = 0; boxIndex < boxes.size(); boxIndex++) { AABB
 box = boxes.get(boxIndex); for (Direction face :
FACES) { ExposedFaceService.SurfaceRect
 initialRect = canonicalFaceRect(face, box);
if (!(initialRect != null)) {
 continue;
 }
 double
 planeCoordinate = facePlaneCoordinate(face, box); List<ExposedFaceService.SurfaceRect>
fragments = new ArrayList<>();
 fragments.add(initialRect); for (int
 otherIndex = 0; otherIndex < boxes.size() && !fragments.isEmpty(); otherIndex++) {
if (!(otherIndex != boxIndex)) {
 continue;
 }
 AABB
 otherBox = boxes.get(otherIndex);
if (!(coversFacePlane(face, planeCoordinate, otherBox))) {
 continue;
 }

 fragments = subtractSurfaceRectList(fragments, canonicalFaceRect(face, otherBox)); } boolean


 boundaryFace = isBoundaryFace(face, planeCoordinate); for (ExposedFaceService.SurfaceRect fragment :
fragments) {
if (!(fragment != null && !(fragment.width() <= 1.0E-6) && !(fragment.height() <= 1.0E-6))) {
 continue;
 }
 ExposedFaceService.ReceiverSurfaceBounds
 bounds = toLocalFaceBounds(face, fragment);
if (!(!(bounds.width() <= 1.0E-6) && !(bounds.height() <= 1.0E-6))) {
 continue;
 }

 surfaces.add(new ExposedFaceService.ReceiverSurfaceTemplate(face, planeCoordinate, bounds, boundaryFace));
 } } }



 return surfaces.isEmpty() ? ExposedFaceService.ReceiverShapeTemplate.EMPTY : new ExposedFaceService.ReceiverShapeTemplate(surfaces);   } private static List<ExposedFaceService.SurfaceRect> subtractSurfaceRectList(List<ExposedFaceService.SurfaceRect> sourceRects, @Nullable ExposedFaceService.SurfaceRect coverRect ) {



 if (!(coverRect != null && !sourceRects.isEmpty())) {
return sourceRects; } List<ExposedFaceService.SurfaceRect>


result = new ArrayList<>(sourceRects.size()); for (ExposedFaceService.SurfaceRect rect :
sourceRects) {
subtractSurfaceRect(rect, coverRect, result);
 }
 return result; } private static void subtractSurfaceRect(ExposedFaceService.SurfaceRect sourceRect, ExposedFaceService.SurfaceRect coverRect, List<ExposedFaceService.SurfaceRect> out ) { double



 overlapMinU = Math.max(sourceRect.minU, coverRect.minU); double
overlapMaxU = Math.min(sourceRect.maxU, coverRect.maxU); double
overlapMinV = Math.max(sourceRect.minV, coverRect.minV); double
overlapMaxV = Math.min(sourceRect.maxV, coverRect.maxV);
 if (!(!(overlapMaxU - overlapMinU <= 1.0E-6) && !(overlapMaxV - overlapMinV <= 1.0E-6))) {
 out.add(sourceRect);
return; }


if (overlapMinV - sourceRect.minV > 1.0E-6) {
 out.add(new ExposedFaceService.SurfaceRect(sourceRect.minU, sourceRect.maxU, sourceRect.minV, overlapMinV)); }

 if (sourceRect.maxV - overlapMaxV > 1.0E-6) {
 out.add(new ExposedFaceService.SurfaceRect(sourceRect.minU, sourceRect.maxU, overlapMaxV, sourceRect.maxV)); }

if (overlapMinU - sourceRect.minU > 1.0E-6) {
 out.add(new ExposedFaceService.SurfaceRect(sourceRect.minU, overlapMinU, overlapMinV, overlapMaxV)); }

 if (sourceRect.maxU - overlapMaxU > 1.0E-6) {
 out.add(new ExposedFaceService.SurfaceRect(overlapMaxU, sourceRect.maxU, overlapMinV, overlapMaxV)); }

 } private static boolean coversFacePlane(Direction face, double planeCoordinate, AABB box) {


 return switch (face) { case UP ->
 Math.abs(box.minY - planeCoordinate) <= 1.0E-6; case DOWN ->
 Math.abs(box.maxY - planeCoordinate) <= 1.0E-6; case NORTH ->
 Math.abs(box.maxZ - planeCoordinate) <= 1.0E-6; case SOUTH ->
 Math.abs(box.minZ - planeCoordinate) <= 1.0E-6; case WEST ->
 Math.abs(box.maxX - planeCoordinate) <= 1.0E-6; case EAST ->
 Math.abs(box.minX - planeCoordinate) <= 1.0E-6; }; } private static double facePlaneCoordinate(Direction face, AABB box) {




 return switch (face) { case UP ->
 box.maxY; case DOWN ->
 box.minY; case NORTH ->
 box.minZ; case SOUTH ->
 box.maxZ; case WEST ->
 box.minX; case EAST ->
 box.maxX; }; } private static boolean isBoundaryFace(Direction face, double planeCoordinate) {




 return switch (face) { case UP, SOUTH, EAST ->
 Math.abs(planeCoordinate - 1.0) <= 1.0E-6; case DOWN, NORTH, WEST ->
 Math.abs(planeCoordinate) <= 1.0E-6; }; } @Nullable private static ExposedFaceService.SurfaceRect canonicalFaceRect(Direction face, AABB box) {




 return switch (face) { case UP, DOWN ->
 surfaceRect(box.minX, box.maxX, box.minZ, box.maxZ); case NORTH, SOUTH ->
 surfaceRect(box.minX, box.maxX, box.minY, box.maxY); case WEST, EAST ->
 surfaceRect(box.minZ, box.maxZ, box.minY, box.maxY); }; } @Nullable private static ExposedFaceService.SurfaceRect surfaceRect(double minU, double maxU, double minV, double maxV) {




 return !(!(maxU - minU <= 1.0E-6) && !(maxV - minV <= 1.0E-6)) ?
 null :
 new ExposedFaceService.SurfaceRect(minU, maxU, minV, maxV); } private static ExposedFaceService.ReceiverSurfaceBounds toLocalFaceBounds(Direction face, ExposedFaceService.SurfaceRect rect) {



 return switch (face) { case UP, SOUTH, EAST ->
 new ExposedFaceService.ReceiverSurfaceBounds(rect.minU, rect.maxU, rect.minV, rect.maxV); case DOWN ->
 new ExposedFaceService.ReceiverSurfaceBounds(rect.minU, rect.maxU, 1.0 - rect.maxV, 1.0 - rect.minV); case NORTH, WEST ->
 new ExposedFaceService.ReceiverSurfaceBounds(1.0 - rect.maxU, 1.0 - rect.minU, rect.minV, rect.maxV); }; } private void scheduleSectionBuild(long sectionKey, ExposedFaceService.SectionFaceData nextData, boolean fullyCaptured) { long




 revision = this.sectionBuildRevision.incrementAndGet();
this.latestSectionBuildRevision.put(sectionKey, revision);
this.receiverBuildExecutor .execute(() -> { try { ExposedFaceService.SectionFaceSnapshot

 faceSnapshot = this.buildFaceSnapshot(nextData); ExposedFaceService.SectionReceiverSnapshot
 receiverSnapshot = this.buildReceiverSnapshot(nextData);
this.completedSectionBuilds .add(new ExposedFaceService.BuiltSectionCommit(sectionKey, revision, nextData, faceSnapshot, receiverSnapshot, fullyCaptured)); }

 catch (Throwable var9) {

 }
 } );
 } private void drainCompletedSectionBuilds() {
        boolean

        changed = false;
        for (int i = 0; i < 64; ++i) {
            BuiltSectionCommit built = this.completedSectionBuilds.poll();
            if (built == null) break;
            Long latest = this.latestSectionBuildRevision.get(built.sectionKey);
            if (latest == null || latest != built.revision) continue;
            this.latestSectionBuildRevision.remove(built.sectionKey, built.revision);

            SectionFaceData previous = this.sectionFaces.get(built.sectionKey);
            boolean contentChanged = previous == null ?
                    !built.data.isEmpty() :
                    !previous.sameContent(built.data);
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
            boolean firstComplete = built.fullyCaptured && this.fullyCapturedSections.put
                    (built.sectionKey, Boolean.TRUE) == null;
            changed |= contentChanged || firstComplete;
        }
        if (changed) { ExposedFaceService.notifySystemsChanged(); }
    } private boolean applySection(long sectionKey, ExposedFaceService.SectionFaceData nextData) { ExposedFaceService.SectionFaceData


 previous = this.sectionFaces.get(sectionKey);
if (previous != null && previous.sameContent(nextData)) { return false; } else {
 this.scheduleSectionBuild(sectionKey, nextData, false);
 return true; } } private ExposedFaceService.SectionFaceSnapshot buildFaceSnapshot(ExposedFaceService.SectionFaceData sectionData) {



 if (!(sectionData != null && !sectionData.isEmpty())) {
return ExposedFaceService.SectionFaceSnapshot.empty(); } long[]


 renderableSolidBits = new long[64]; byte[]
 faceMasks = new byte[4096]; boolean
anyCandidateReceivers = false; for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry :

sectionData.facesByBlock.long2ByteEntrySet()) { byte
 faceMask = entry.getByteValue();
if (!(faceMask != 0)) {
 continue;
 }
 long
 blockKey = entry.getLongKey(); int
localIndex = sectionLocalIndex(BlockPos.getX(blockKey) & 15, BlockPos.getY(blockKey) & 15, BlockPos.getZ(blockKey) & 15);
 renderableSolidBits[localIndex >>> 6] |= 1L << (localIndex & 63);
 faceMasks[localIndex] = faceMask;
 anyCandidateReceivers = true;
 }

 return anyCandidateReceivers ? new ExposedFaceService.SectionFaceSnapshot(renderableSolidBits, faceMasks, true) : ExposedFaceService.SectionFaceSnapshot.empty(); } private ExposedFaceService.SectionReceiverSnapshot buildReceiverSnapshot(ExposedFaceService.SectionFaceData sectionData) {



 if (!(sectionData != null && !sectionData.isEmpty())) {
return ExposedFaceService.SectionReceiverSnapshot.empty(); } HashMap<Long, Long>


faceCells = new HashMap<>(); ArrayList<ExposedFaceService.ReceiverRect>
rects = new ArrayList<>(); for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry :

sectionData.facesByBlock.long2ByteEntrySet()) { long
 blockKey = entry.getLongKey(); byte
faceMask = entry.getByteValue();
if (!(faceMask != 0)) {
 continue;
 }
 int
 localX = BlockPos.getX(blockKey) & 15; int
 localY = BlockPos.getY(blockKey) & 15; int
 localZ = BlockPos.getZ(blockKey) & 15; for (Direction face :
FACES) {
if (!((faceMask & 1 << face.ordinal()) != 0)) {
 continue;
 }
 int cellU = localCellU(face, localX, localZ); int
 cellV = localCellV(face, localY, localZ); int
 planeCoordinate = planeCoordinateForFace(face, localX, localY, localZ);
 faceCells.put(receiverCellKey(face.ordinal(), planeCoordinate, cellU, cellV), blockKey); }

 } HashSet<Long>

visited = new HashSet<>(faceCells.size()); for (java.util.Map.Entry<Long, Long> entry :
 faceCells.entrySet()) { long
 packedCell = entry.getKey();
if (!(visited.add(packedCell))) {
continue;
}
 int
 faceOrdinal = unpackFaceOrdinal(packedCell); int
planeCoordinate = unpackPlaneCoordinate(packedCell); int
startCellU = unpackCellU(packedCell); int
startCellV = unpackCellV(packedCell); int

width = 1; while
 (faceCells.containsKey(receiverCellKey(faceOrdinal, planeCoordinate, startCellU + width, startCellV)) && !visited.contains
(receiverCellKey(faceOrdinal, planeCoordinate, startCellU + width, startCellV)) ) {
 width++; } int


 height = 1; while (true) { int

 nextV = startCellV + height; boolean
 rowMatches = true; for (int
 offsetU = 0; offsetU < width; offsetU++) { long
 nextKey = receiverCellKey(faceOrdinal, planeCoordinate, startCellU + offsetU, nextV);
 if (!faceCells.containsKey(nextKey) || visited.contains(nextKey)) {
rowMatches = false;
 break; } }


 if (!rowMatches) {
break; }

height++;
} for (int

 offsetV = 0; offsetV < height; offsetV++) { for (int
 offsetU = 0; offsetU < width; offsetU++) {
visited.add(receiverCellKey(faceOrdinal, planeCoordinate, startCellU + offsetU, startCellV + offsetV)); } }



 rects.add(new ExposedFaceService.ReceiverRect(Direction.from3DDataValue
(faceOrdinal), planeCoordinate, startCellU, startCellU + width, startCellV, startCellV + height, entry.getValue





 () ) );

 }

 return rects.isEmpty() ? ExposedFaceService.SectionReceiverSnapshot.empty() : new ExposedFaceService.SectionReceiverSnapshot(rects); } private static long receiverCellKey(int faceOrdinal, int planeCoordinate, int cellU, int cellV) {
        long


        value = ((long)faceOrdinal & 7L) << 61;
        value |= ((long)planeCoordinate & 0x1FFFFFL) << 40;
        value |= ((long)cellU & 0xFFFFFL) << 20;
        value |= (long)cellV & 0xFFFFFL;
        return value;
    } private static int unpackFaceOrdinal(long key) {


 return (int)(key >>> 61 & 7L); } private static int unpackPlaneCoordinate(long key) { int



 value = (int)(key >>> 40 & 2097151L);
 if ((value & 1048576) != 0) {
 value |= -2097152; }

 return value; } private static int unpackCellU(long key) { int



 value = (int)(key >>> 20 & 1048575L);
 if ((value & 524288) != 0) {
 value |= -1048576; }

 return value; } private static int unpackCellV(long key) { int



 value = (int)(key & 1048575L);
 if ((value & 524288) != 0) {
 value |= -1048576; }

 return value; } private static int planeCoordinateForFace(Direction face, int localX, int localY, int localZ) {



        return switch (face) { default -> throw new IncompatibleClassChangeError();
            case EAST -> localX + 1;
            case WEST -> localX;
            case UP -> localY + 1;
            case DOWN -> localY;
            case SOUTH -> localZ + 1;
            case NORTH -> localZ;
        };
    } private static int localCellU(Direction face, int localX, int localZ) {


 return switch (face) { case UP, DOWN, NORTH, SOUTH ->
 localX; case WEST, EAST ->
 localZ; }; } private static int localCellV(Direction face, int localY, int localZ) {




 return switch (face) { case UP, DOWN ->
 localZ; case NORTH, SOUTH, WEST, EAST ->
 localY; }; } private static long sectionKeyFromOrigin(BlockPos sectionOrigin) {




 return SectionPos.asLong(SectionPos.blockToSectionCoord
(sectionOrigin.getX()), SectionPos.blockToSectionCoord
 (sectionOrigin.getY()), SectionPos.blockToSectionCoord
 (sectionOrigin.getZ()) ); } private static long sectionKeyForBlock(long blockKey) {




 return SectionPos.asLong(SectionPos.blockToSectionCoord
(BlockPos.getX(blockKey)), SectionPos.blockToSectionCoord
 (BlockPos.getY(blockKey)), SectionPos.blockToSectionCoord
 (BlockPos.getZ(blockKey)) ); } private static void notifySystemsChanged() {




 notifySystemsChanged(null);
} private static void notifySystemsChanged(@Nullable LongOpenHashSet changedBlocks) { ReflectionSystem.get


 ().markWorldDirty();
if (!(changedBlocks != null && !changedBlocks.isEmpty())) { NeoShadowsEngine.markWorldDirty
 (); } else {

 NeoShadowsEngine.markWorldDirty(changedBlocks); } Minecraft.getInstance

 ().execute(TorchRtxState.get()::requestReflectiveSnapshotRefresh);
} private static int sectionLocalIndex(int localX, int localY, int localZ) {


 return localY << 8 | localZ << 4 | localX; } private static boolean isCacheActive() {




 return Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) || Config.isFeatureEnabled
 (Config.CLIENT.neoCpuShadowsEnabled) || Config.isFeatureEnabled
 (Config.CLIENT.gpuShadowsEnabled) || Config.isFeatureEnabled
 (Config.CLIENT.reflectionEnabled); } private static final class BuiltSectionCommit { final long sectionKey; final long revision; final ExposedFaceService.SectionFaceData data; final ExposedFaceService.SectionFaceSnapshot faceSnapshot; final ExposedFaceService.SectionReceiverSnapshot receiverSnapshot; final boolean fullyCaptured; BuiltSectionCommit(long sectionKey, long revision, ExposedFaceService.SectionFaceData data, ExposedFaceService.SectionFaceSnapshot faceSnapshot, ExposedFaceService.SectionReceiverSnapshot receiverSnapshot, boolean fullyCaptured )













 {
 this.sectionKey = sectionKey;
 this.revision = revision;
 this.data = data;
 this.faceSnapshot = faceSnapshot;
 this.receiverSnapshot = receiverSnapshot;
 this.fullyCaptured = fullyCaptured;
 } } private static final class SectionCapture { private final long sectionKey;




 private final Long2ByteOpenHashMap facesByBlock = new Long2ByteOpenHashMap(); private SectionCapture(long sectionKey)

{
this.sectionKey = sectionKey;
 this.facesByBlock.defaultReturnValue((byte)0);
} private void recordFace(long blockKey, Direction face) {


 this.facesByBlock.put(blockKey, (byte)(this.facesByBlock.get(blockKey) | 1 << face.ordinal()));
} private ExposedFaceService.SectionFaceData detachFaceData() {


 return new ExposedFaceService.SectionFaceData(this.facesByBlock, true); } } private static final class SectionFaceData { private final Long2ByteOpenHashMap facesByBlock; private SectionFaceData(Long2ByteOpenHashMap source) {







 this(source, false);
} private SectionFaceData(Long2ByteOpenHashMap source, boolean takeOwnership)

 {
 this.facesByBlock = takeOwnership ? source : new Long2ByteOpenHashMap(source);
 this.facesByBlock.defaultReturnValue((byte)0);
} private byte faceMask(long blockKey) {


 return this.facesByBlock.get(blockKey); } private boolean isEmpty() {



 return this.facesByBlock.isEmpty(); } private Long2ByteOpenHashMap copyFaces() {



 return new Long2ByteOpenHashMap(this.facesByBlock); } private boolean sameContent(ExposedFaceService.SectionFaceData other) {



 if (this.facesByBlock.size() != other.facesByBlock.size()) {
return false; } else { for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry :


this.facesByBlock.long2ByteEntrySet()) {
if (other.facesByBlock.get(entry.getLongKey()) != entry.getByteValue()) {
return false; }

 }

 return true; } } } public static final class SectionFaceSnapshot { private static final ExposedFaceService.SectionFaceSnapshot




 EMPTY = new ExposedFaceService.SectionFaceSnapshot(new long[64], new byte[4096], false); private final long[] renderableSolidBits; private final byte[] faceMasks; private final boolean anyCandidateReceivers; private SectionFaceSnapshot(long[] renderableSolidBits, byte[] faceMasks, boolean anyCandidateReceivers)





 {
 this.renderableSolidBits = renderableSolidBits;
 this.faceMasks = faceMasks;
 this.anyCandidateReceivers = anyCandidateReceivers;
 } public boolean hasCandidateReceivers() {


 return this.anyCandidateReceivers; } public boolean isRenderableSolid(int localIndex) {



 return (this.renderableSolidBits[localIndex >>> 6] & 1L << (localIndex & 63)) != 0L; } public long word(int wordIndex) {



 return this.renderableSolidBits[wordIndex]; } public int wordCount() {



 return this.renderableSolidBits.length; } public byte faceMask(int localIndex) {



 return this.faceMasks[localIndex]; } public static ExposedFaceService.SectionFaceSnapshot empty() {



 return EMPTY; } } public static final class SectionReceiverSnapshot { private static final ExposedFaceService.SectionReceiverSnapshot




 EMPTY = new ExposedFaceService.SectionReceiverSnapshot(Collections.emptyList()); private final List<ExposedFaceService.ReceiverRect> rects; private SectionReceiverSnapshot(List<ExposedFaceService.ReceiverRect> rects)



{
this.rects = List.copyOf(rects);
} public List<ExposedFaceService.ReceiverRect> rects() {


 return this.rects; } public boolean isEmpty() {



 return this.rects.isEmpty(); } public static ExposedFaceService.SectionReceiverSnapshot empty() {



 return EMPTY; } } public static final class ReceiverRect { private final Direction face; private final int planeCoordinate; private final int minCellU; private final int maxCellU; private final int minCellV; private final int maxCellV; private final long anchorBlockKey; private ReceiverRect(Direction face, int planeCoordinate, int minCellU, int maxCellU, int minCellV, int maxCellV, long anchorBlockKey)


















 {
 this.face = face;
 this.planeCoordinate = planeCoordinate;
 this.minCellU = minCellU;
 this.maxCellU = maxCellU;
 this.minCellV = minCellV;
 this.maxCellV = maxCellV;
 this.anchorBlockKey = anchorBlockKey;
 } public Direction face() {


 return this.face; } public int planeCoordinate() {



 return this.planeCoordinate; } public int minCellU() {



 return this.minCellU; } public int maxCellU() {



 return this.maxCellU; } public int minCellV() {



 return this.minCellV; } public int maxCellV() {



 return this.maxCellV; } public long anchorBlockKey() {



 return this.anchorBlockKey; } } public static final class ReceiverSurface { private final long blockKey; private final Direction face; private final double planeCoordinate; private final double minU; private final double maxU; private final double minV; private final double maxV; private final long surfaceKey; private ReceiverSurface(long blockKey, Direction face, double planeCoordinate, double minU, double maxU, double minV, double maxV, long surfaceKey)




















 {
 this.blockKey = blockKey;
 this.face = face;
 this.planeCoordinate = planeCoordinate;
 this.minU = minU;
 this.maxU = maxU;
 this.minV = minV;
 this.maxV = maxV;
 this.surfaceKey = surfaceKey;
 } public long blockKey() {


 return this.blockKey; } public Direction face() {



 return this.face; } public double planeCoordinate() {



 return this.planeCoordinate; } public double minU() {



 return this.minU; } public double maxU() {



 return this.maxU; } public double minV() {



 return this.minV; } public double maxV() {



 return this.maxV; } public long surfaceKey() {



 return this.surfaceKey; } } private static long computeSurfaceKey(long blockKey, Direction face, double planeCoordinate, ExposedFaceService.ReceiverSurfaceBounds bounds) { long


















 hash = 1469598103934665603L;
 hash = mixSurfaceKey(hash, blockKey);
hash = mixSurfaceKey(hash, (long)face.ordinal());
hash = mixSurfaceKey(hash, Double.doubleToLongBits(planeCoordinate));
hash = mixSurfaceKey(hash, Double.doubleToLongBits(bounds.minU));
hash = mixSurfaceKey(hash, Double.doubleToLongBits(bounds.maxU));
hash = mixSurfaceKey(hash, Double.doubleToLongBits(bounds.minV));
hash = mixSurfaceKey(hash, Double.doubleToLongBits(bounds.maxV));
 return hash; } private static long mixSurfaceKey(long hash, long value) {



 return (hash ^ value) * 1099511628211L; }


 @FunctionalInterface public interface ReceiverSurfaceVisitor { void visit(Direction var1, double var2, double var4, double var6, double var8, double var10, long var12); } private static record SurfaceRect(double minU, double maxU, double minV, double maxV) { double width() {

 return this.maxU - this.minU; } double height() {



 return this.maxV - this.minV;
        } }


        private static record ReceiverSurfaceBounds(double minU, double maxU, double minV, double maxV) { double width() {

 return this.maxU - this.minU; } double height() {



 return this.maxV - this.minV; } } private static final class ReceiverSurfaceTemplate { private final Direction face; private final int faceMaskBit; private final double planeCoordinate; private final ExposedFaceService.ReceiverSurfaceBounds bounds; private final boolean boundaryFace; private ReceiverSurfaceTemplate(Direction face, double planeCoordinate, ExposedFaceService.ReceiverSurfaceBounds bounds, boolean boundaryFace)













 {
 this.face = face;
 this.faceMaskBit = 1 << face.ordinal();
this.planeCoordinate = planeCoordinate;
 this.bounds = bounds;
 this.boundaryFace = boundaryFace;
 } private ExposedFaceService.ReceiverSurface instantiate(long blockKey) {


 return new ExposedFaceService.ReceiverSurface(blockKey, this.face, this.planeCoordinate, this.bounds.minU, this.bounds.maxU, this.bounds.minV, this.bounds.maxV, ExposedFaceService.computeSurfaceKey







 (blockKey, this.face, this.planeCoordinate, this.bounds) ); } } private static final class ReceiverShapeTemplate { private static final ExposedFaceService.ReceiverShapeTemplate





 EMPTY = new ExposedFaceService.ReceiverShapeTemplate(new ExposedFaceService.ReceiverSurfaceTemplate[0] ); private final ExposedFaceService.ReceiverSurfaceTemplate[] surfaces; private final boolean hasBoundaryFaces; private ReceiverShapeTemplate(List<ExposedFaceService.ReceiverSurfaceTemplate> surfaces) {





 this(surfaces.toArray(new ExposedFaceService.ReceiverSurfaceTemplate[0]));
} private ReceiverShapeTemplate(ExposedFaceService.ReceiverSurfaceTemplate[] surfaces)

{
this.surfaces = surfaces; boolean
 anyBoundaryFaces = false; for (ExposedFaceService.ReceiverSurfaceTemplate surface :
surfaces) {
if (surface.boundaryFace) {
anyBoundaryFaces = true;
 break; } }


 this.hasBoundaryFaces = anyBoundaryFaces;
 } private boolean isEmpty() {


 return this.surfaces.length == 0; } private boolean hasBoundaryFaces() {



 return this.hasBoundaryFaces; } private boolean sameGeometry(ExposedFaceService.ReceiverShapeTemplate other) {



 if (!(other != null && this.surfaces.length == other.surfaces.length)) {
return false; } for (int

 i = 0; i < this.surfaces.length; i++) { ExposedFaceService.ReceiverSurfaceTemplate
 a = this.surfaces[i]; ExposedFaceService.ReceiverSurfaceTemplate
 b = other.surfaces[i];
if (a.face != b.face || a.boundaryFace != b.boundaryFace || Double.doubleToLongBits

 (a.planeCoordinate) != Double.doubleToLongBits
 (b.planeCoordinate) || Double.doubleToLongBits
 (a.bounds.minU) != Double.doubleToLongBits
 (b.bounds.minU) || Double.doubleToLongBits
 (a.bounds.maxU) != Double.doubleToLongBits
 (b.bounds.maxU) || Double.doubleToLongBits
 (a.bounds.minV) != Double.doubleToLongBits
 (b.bounds.minV) || Double.doubleToLongBits
 (a.bounds.maxV) != Double.doubleToLongBits
 (b.bounds.maxV)) {
return false; } }


 return true; } private byte computeFaceMask(BlockState state, BlockGetter level, BlockPos pos) {



 if (this.surfaces.length == 0) {
 return 0; } byte


 mask = 0; MutableBlockPos
 neighbour = null; for (ExposedFaceService.ReceiverSurfaceTemplate surface :
this.surfaces) {
if (surface.boundaryFace) {
if (neighbour == null) {
 neighbour = new MutableBlockPos(); }

 neighbour.setWithOffset(pos, surface.face);
 if (!Block.shouldRenderFace(state, level, pos, surface.face, neighbour)) {
 continue; } }


 mask |= (byte)surface.faceMaskBit; }

return mask;  } private void forEachSurface(long blockKey, byte visibleBoundaryMask, ExposedFaceService.ReceiverSurfaceVisitor visitor) { for (ExposedFaceService.ReceiverSurfaceTemplate surface :



this.surfaces) {
if (!(!surface.boundaryFace || (visibleBoundaryMask & surface.faceMaskBit) != 0)) {
 continue;
 }

 visitor.visit(surface.face, surface.planeCoordinate, surface.bounds.minU, surface.bounds.maxU, surface.bounds.minV, surface.bounds.maxV, ExposedFaceService.computeSurfaceKey






 (blockKey, surface.face, surface.planeCoordinate, surface.bounds) ); }


} private ExposedFaceService.ReceiverSurface[] instantiateArray(long blockKey, byte visibleBoundaryMask) {


 if (this.surfaces.length == 0) {
 return ExposedFaceService.EMPTY_RECEIVER_SURFACES; } ExposedFaceService.ReceiverSurface[]


resolved = new ExposedFaceService.ReceiverSurface[this.surfaces.length]; int
count = 0; for (ExposedFaceService.ReceiverSurfaceTemplate surface :
this.surfaces) {
if (!(!surface.boundaryFace || (visibleBoundaryMask & surface.faceMaskBit) != 0)) {
 continue;
 }
 resolved[count++] = surface.instantiate(blockKey); }


if (count == 0) {
 return ExposedFaceService.EMPTY_RECEIVER_SURFACES; } else

if (count == resolved.length) {
return resolved; } ExposedFaceService.ReceiverSurface[]


compact = new ExposedFaceService.ReceiverSurface[count];
System.arraycopy(resolved, 0, compact, 0, count);
 return compact;   } private List<ReceiverSurface> instantiate(long blockKey, byte visibleBoundaryMask) {



            if (this.surfaces.length == 0) {
                return Collections.emptyList();
            }

            ArrayList<ReceiverSurface> resolved = null;
            for (ReceiverSurfaceTemplate surface : this.surfaces) {
                if (surface.boundaryFace && (visibleBoundaryMask & surface.faceMaskBit) == 0)
                        continue;


                if (resolved == null) {
                    resolved = new ArrayList<ReceiverSurface>(this.surfaces.length);
                }
                resolved.add(surface.instantiate(blockKey));
            }

            return resolved == null || resolved.isEmpty() ? Collections.emptyList() : resolved;
        } } private static final ExposedFaceService.ReceiverSurface[]


EMPTY_RECEIVER_SURFACES = new ExposedFaceService.ReceiverSurface[0]; private static final

class SectionCompleteReceiverSurfaceCache {
final ConcurrentHashMap<Long, ExposedFaceService.ReceiverSurface[]> surfacesByBlock = new ConcurrentHashMap<>(); }
    }
