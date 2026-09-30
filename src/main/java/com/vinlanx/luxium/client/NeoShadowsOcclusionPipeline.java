/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.minecraft.client.multiplayer.ClientChunkCache
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.SectionSkip;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsOcclusionPipeline {
    private static final double DDA_TIE_EPSILON = 1.0E-9;
    private static final ThreadLocal<FaceOcclusionScratch> FACE_OCCLUSION_SCRATCH_TL = ThreadLocal.withInitial(FaceOcclusionScratch::new);
    private static final ThreadLocal<OccluderHit> SCRATCH_HIT = ThreadLocal.withInitial(() -> new OccluderHit(BlockPos.f_121853_, new NeoShadowsTypes.Box[0]));
    private final NeoShadowsEngine engine;
    private final ConcurrentHashMap<OcclusionCacheKey, CachedFaceOcclusion> occlusionCache = new ConcurrentHashMap();

    NeoShadowsOcclusionPipeline(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    void clearOcclusionCache() {
        this.occlusionCache.clear();
    }

    void findFaceOccludersInto(ClientLevel level, NeoShadowsTypes.ReceiverPlane plane, NeoShadowsTypes.FaceCandidate candidate, NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.GeometryBuildState state, Vec3 source, long sourceKey, int sourceEmission, Long2ObjectLinkedOpenHashMap<OccluderHit> hitsByBlock) {
        double maxDist = Math.min(LightRtMath.getMaxDistance(sourceEmission), 30.0);
        NeoShadowsTypes.PlaneRect localBounds = group.localContributionBoundsForReceiverPosKey(candidate.receiverKey);
        long candidateKey = candidate.receiverKey;
        OcclusionCacheKey cacheKey = new OcclusionCacheKey(candidateKey, candidate.face.ordinal(), sourceKey);
        CachedFaceOcclusion cached = this.occlusionCache.get(cacheKey);
        if (cached != null) {
            int n = cached.hitKeys.length;
            for (int i = 0; i < n; ++i) {
                NeoShadowsTypes.UvPoint[] probes;
                int n2;
                NeoShadowsTypes.UvPoint[] probes2;
                long hitKey = cached.hitKeys[i];
                OccluderHit mergedHit = (OccluderHit)hitsByBlock.get(hitKey);
                if (mergedHit == null) {
                    OccluderHit persisted = new OccluderHit(BlockPos.m_122022_((long)hitKey), cached.hitBoxes[i]);
                    NeoShadowsTypes.UvPoint[] uvPointArray = probes2 = cached.probesPerHit[i];
                    n2 = uvPointArray.length;
                    for (int j = 0; j < n2; ++j) {
                        NeoShadowsTypes.UvPoint probe = uvPointArray[j];
                        persisted.addReceiverSample(probe);
                    }
                    hitsByBlock.put(hitKey, (Object)persisted);
                    continue;
                }
                probes2 = probes = cached.probesPerHit[i];
                int n3 = probes2.length;
                for (n2 = 0; n2 < n3; ++n2) {
                    NeoShadowsTypes.UvPoint probe = probes2[n2];
                    mergedHit.addReceiverSample(probe);
                }
            }
            return;
        }
        FaceOcclusionScratch faceScratch = FACE_OCCLUSION_SCRATCH_TL.get();
        faceScratch.reset();
        NeoShadowsTypes.UvPoint[] occlusionProbes = NeoShadowsOcclusionPipeline.buildOcclusionFaceProbes(candidate.face);
        ProbeFirstHitCache firstHitCache = state.firstHitScalarCache(sourceKey, candidateKey, occlusionProbes.length);
        for (int probeIndex = 0; probeIndex < occlusionProbes.length; ++probeIndex) {
            double sz;
            double ddz;
            double sy;
            double ddy;
            double sx;
            double ddx;
            double distance;
            NeoShadowsTypes.UvPoint probe = occlusionProbes[probeIndex];
            if (!NeoShadowsProjection.isProbeInsideRect(probe, localBounds) || (distance = Math.sqrt((ddx = (sx = plane.sampleX(probe.u, probe.v)) - source.f_82479_) * ddx + (ddy = (sy = plane.sampleY(probe.u, probe.v)) - source.f_82480_) * ddy + (ddz = (sz = plane.sampleZ(probe.u, probe.v)) - source.f_82481_) * ddz)) > maxDist || LightRtMath.getFalloff(sourceEmission, distance) <= 0) continue;
            CachedFirstScalarHit cachedHit = firstHitCache.get(probeIndex);
            if (cachedHit == null) {
                cachedHit = firstHitCache.findReusableHit(source.f_82479_, source.f_82480_, source.f_82481_, sx, sy, sz);
            }
            if (cachedHit == null) {
                OccluderHit hit = SCRATCH_HIT.get();
                hit.reset();
                cachedHit = SolidOccluderCache.firstHitScalar(level, source, sx, sy, sz, sourceKey, candidateKey, hit) ? CachedFirstScalarHit.hit(hit.blockKey, hit.boxes) : CachedFirstScalarHit.MISS;
                cachedHit = firstHitCache.storeIfAbsent(probeIndex, cachedHit);
            }
            if (!cachedHit.hit) continue;
            firstHitCache.remember(cachedHit);
            long hitKey = cachedHit.blockKey;
            OccluderHit mergedHit = (OccluderHit)hitsByBlock.get(hitKey);
            if (mergedHit == null) {
                OccluderHit persisted = new OccluderHit(BlockPos.m_122022_((long)hitKey), cachedHit.boxes);
                persisted.addReceiverSample(probe);
                hitsByBlock.put(hitKey, (Object)persisted);
            } else {
                mergedHit.addReceiverSample(probe);
            }
            List<NeoShadowsTypes.UvPoint> probesForHit = (List<NeoShadowsTypes.UvPoint>)faceScratch.probesByHit.get(hitKey);
            if (probesForHit == null) {
                probesForHit = faceScratch.rentList();
                faceScratch.probesByHit.put(hitKey, probesForHit);
                faceScratch.boxesByHit.put(hitKey, (Object)cachedHit.boxes);
            }
            probesForHit.add(probe);
        }
        int n = faceScratch.probesByHit.size();
        if (n == 0) {
            this.occlusionCache.put(cacheKey, CachedFaceOcclusion.EMPTY);
        } else {
            long[] hitKeys = new long[n];
            NeoShadowsTypes.Box[][] hitBoxes = new NeoShadowsTypes.Box[n][];
            NeoShadowsTypes.UvPoint[][] probesPerHit = new NeoShadowsTypes.UvPoint[n][];
            int i = 0;
            for (Long2ObjectMap.Entry entry : faceScratch.probesByHit.long2ObjectEntrySet()) {
                long key;
                hitKeys[i] = key = entry.getLongKey();
                hitBoxes[i] = (NeoShadowsTypes.Box[])faceScratch.boxesByHit.get(key);
                List probes = (List)entry.getValue();
                probesPerHit[i] = probes.toArray(new NeoShadowsTypes.UvPoint[0]);
                ++i;
            }
            this.occlusionCache.put(cacheKey, new CachedFaceOcclusion(hitKeys, hitBoxes, probesPerHit));
        }
    }

    static NeoShadowsTypes.UvPoint[] createFaceProbes(int gridSize, boolean includeCenterFirst, double inset) {
        double min = inset;
        double max = 1.0 - inset;
        double step = gridSize <= 1 ? 0.0 : (max - min) / (double)(gridSize - 1);
        ArrayList<NeoShadowsTypes.UvPoint> probes = new ArrayList<NeoShadowsTypes.UvPoint>(gridSize * gridSize + 1);
        if (includeCenterFirst) {
            probes.add(new NeoShadowsTypes.UvPoint(0.5, 0.5));
        }
        for (int vIndex = 0; vIndex < gridSize; ++vIndex) {
            double v = gridSize <= 1 ? 0.5 : min + step * (double)vIndex;
            for (int uIndex = 0; uIndex < gridSize; ++uIndex) {
                double u;
                double d = u = gridSize <= 1 ? 0.5 : min + step * (double)uIndex;
                if (includeCenterFirst && Math.abs(u - 0.5) <= 1.0E-6 && Math.abs(v - 0.5) <= 1.0E-6) continue;
                probes.add(new NeoShadowsTypes.UvPoint(u, v));
            }
        }
        return probes.toArray(new NeoShadowsTypes.UvPoint[0]);
    }

    static NeoShadowsTypes.PlaneRect[] createProbeSupportRects(NeoShadowsTypes.UvPoint[] probes) {
        List<Double> uniqueU = NeoShadowsOcclusionPipeline.collectUniqueProbeCoordinates(probes, true);
        List<Double> uniqueV = NeoShadowsOcclusionPipeline.collectUniqueProbeCoordinates(probes, false);
        NeoShadowsTypes.PlaneRect[] rects = new NeoShadowsTypes.PlaneRect[probes.length];
        for (int index = 0; index < probes.length; ++index) {
            NeoShadowsTypes.UvPoint probe = probes[index];
            int uIndex = NeoShadowsOcclusionPipeline.findProbeCoordinateIndex(uniqueU, probe.u);
            int vIndex = NeoShadowsOcclusionPipeline.findProbeCoordinateIndex(uniqueV, probe.v);
            double minU = uIndex <= 0 ? 0.0 : (uniqueU.get(uIndex - 1) + probe.u) * 0.5;
            double maxU = uIndex >= uniqueU.size() - 1 ? 1.0 : (probe.u + uniqueU.get(uIndex + 1)) * 0.5;
            double minV = vIndex <= 0 ? 0.0 : (uniqueV.get(vIndex - 1) + probe.v) * 0.5;
            double maxV = vIndex >= uniqueV.size() - 1 ? 1.0 : (probe.v + uniqueV.get(vIndex + 1)) * 0.5;
            rects[index] = new NeoShadowsTypes.PlaneRect(minU, maxU, minV, maxV);
        }
        return rects;
    }

    static List<Double> collectUniqueProbeCoordinates(NeoShadowsTypes.UvPoint[] probes, boolean useU) {
        ArrayList<Double> values = new ArrayList<Double>(probes.length);
        for (NeoShadowsTypes.UvPoint probe : probes) {
            double value = useU ? probe.u : probe.v;
            boolean exists = false;
            Iterator iterator = values.iterator();
            while (iterator.hasNext()) {
                double existing = (Double)iterator.next();
                if (!(Math.abs(existing - value) <= 1.0E-6)) continue;
                exists = true;
                break;
            }
            if (exists) continue;
            values.add(value);
        }
        values.sort(Double::compare);
        return values;
    }

    static int findProbeCoordinateIndex(List<Double> coordinates, double value) {
        for (int index = 0; index < coordinates.size(); ++index) {
            if (!(Math.abs(coordinates.get(index) - value) <= 1.0E-6)) continue;
            return index;
        }
        return -1;
    }

    static NeoShadowsTypes.UvPoint[] buildOcclusionFaceProbes(Direction face) {
        return NeoShadowsProjection.isHorizontalReceiverFace(face) ? NeoShadowsEngine.HORIZONTAL_OCCLUSION_FACE_PROBES : NeoShadowsEngine.OCCLUSION_FACE_PROBES;
    }

    private record OcclusionCacheKey(long receiverPosKey, int faceOrdinal, long sourceKey) {
    }

    private static final class CachedFaceOcclusion {
        static final CachedFaceOcclusion EMPTY = new CachedFaceOcclusion(new long[0], new NeoShadowsTypes.Box[0][], new NeoShadowsTypes.UvPoint[0][]);
        final long[] hitKeys;
        final NeoShadowsTypes.Box[][] hitBoxes;
        final NeoShadowsTypes.UvPoint[][] probesPerHit;

        CachedFaceOcclusion(long[] hitKeys, NeoShadowsTypes.Box[][] hitBoxes, NeoShadowsTypes.UvPoint[][] probesPerHit) {
            this.hitKeys = hitKeys;
            this.hitBoxes = hitBoxes;
            this.probesPerHit = probesPerHit;
        }
    }

    static final class OccluderHit {
        final BlockPos blockPos;
        long blockKey;
        NeoShadowsTypes.Box[] boxes;
        final List<NeoShadowsTypes.UvPoint> receiverSamples = new ArrayList<NeoShadowsTypes.UvPoint>(4);

        OccluderHit(BlockPos blockPos, NeoShadowsTypes.Box[] boxes) {
            this.blockPos = blockPos;
            this.blockKey = blockPos.m_121878_();
            this.boxes = boxes;
        }

        void reset() {
            this.blockKey = 0L;
            this.boxes = null;
            this.receiverSamples.clear();
        }

        void addReceiverSample(NeoShadowsTypes.UvPoint sample) {
            this.receiverSamples.add(sample);
        }
    }

    private static final class FaceOcclusionScratch {
        final Long2ObjectLinkedOpenHashMap<List<NeoShadowsTypes.UvPoint>> probesByHit = new Long2ObjectLinkedOpenHashMap(8);
        final Long2ObjectOpenHashMap<NeoShadowsTypes.Box[]> boxesByHit = new Long2ObjectOpenHashMap(8);
        final List<List<NeoShadowsTypes.UvPoint>> listPool = new ArrayList<List<NeoShadowsTypes.UvPoint>>();
        int listPoolUsed;

        private FaceOcclusionScratch() {
        }

        List<NeoShadowsTypes.UvPoint> rentList() {
            List<Object> list;
            if (this.listPoolUsed < this.listPool.size()) {
                list = this.listPool.get(this.listPoolUsed);
                list.clear();
            } else {
                list = new ArrayList(4);
                this.listPool.add(list);
            }
            ++this.listPoolUsed;
            return list;
        }

        void reset() {
            this.probesByHit.clear();
            this.boxesByHit.clear();
            this.listPoolUsed = 0;
        }
    }

    static final class ProbeFirstHitCache {
        private final AtomicReferenceArray<CachedFirstScalarHit> cachedHits;
        @Nullable
        private volatile CachedFirstScalarHit lastResolvedHit;

        ProbeFirstHitCache(int probeCount) {
            this.cachedHits = new AtomicReferenceArray(probeCount);
        }

        CachedFirstScalarHit get(int probeIndex) {
            return this.cachedHits.get(probeIndex);
        }

        CachedFirstScalarHit storeIfAbsent(int probeIndex, CachedFirstScalarHit hit) {
            if (this.cachedHits.compareAndSet(probeIndex, null, hit)) {
                return hit;
            }
            CachedFirstScalarHit existing = this.cachedHits.get(probeIndex);
            return existing != null ? existing : hit;
        }

        @Nullable
        CachedFirstScalarHit findReusableHit(double startX, double startY, double startZ, double endX, double endY, double endZ) {
            CachedFirstScalarHit lastHit = this.lastResolvedHit;
            if (lastHit == null || !lastHit.hit) {
                return null;
            }
            return lastHit.intersectsSegment(startX, startY, startZ, endX, endY, endZ) ? lastHit : null;
        }

        void remember(CachedFirstScalarHit hit) {
            if (hit.hit) {
                this.lastResolvedHit = hit;
            }
        }
    }

    static final class CachedFirstScalarHit {
        static final CachedFirstScalarHit MISS = new CachedFirstScalarHit(false, Long.MIN_VALUE, null);
        final boolean hit;
        final long blockKey;
        final int blockX;
        final int blockY;
        final int blockZ;
        final NeoShadowsTypes.Box[] boxes;

        private CachedFirstScalarHit(boolean hit, long blockKey, @Nullable NeoShadowsTypes.Box[] boxes) {
            this.hit = hit;
            this.blockKey = blockKey;
            this.blockX = hit ? BlockPos.m_121983_((long)blockKey) : 0;
            this.blockY = hit ? BlockPos.m_122008_((long)blockKey) : 0;
            this.blockZ = hit ? BlockPos.m_122015_((long)blockKey) : 0;
            this.boxes = boxes;
        }

        static CachedFirstScalarHit hit(long blockKey, NeoShadowsTypes.Box[] boxes) {
            return new CachedFirstScalarHit(true, blockKey, boxes);
        }

        boolean intersectsSegment(double startX, double startY, double startZ, double endX, double endY, double endZ) {
            if (!this.hit || this.boxes == null || this.boxes.length == 0) {
                return false;
            }
            if (this.boxes.length == 1) {
                return this.boxes[0].intersectsSegmentWithOffset(startX, startY, startZ, endX, endY, endZ, this.blockX, this.blockY, this.blockZ);
            }
            for (NeoShadowsTypes.Box box : this.boxes) {
                if (!box.intersectsSegmentWithOffset(startX, startY, startZ, endX, endY, endZ, this.blockX, this.blockY, this.blockZ)) continue;
                return true;
            }
            return false;
        }
    }

    static final class SolidOccluderCache {
        private static final ConcurrentHashMap<BlockState, NeoShadowsTypes.Box[]> BOX_CACHE = new ConcurrentHashMap();
        private static final NeoShadowsTypes.Box[] NO_BOXES = new NeoShadowsTypes.Box[0];
        private static final NeoShadowsTypes.Box[] FULL_BLOCK_BOXES = new NeoShadowsTypes.Box[]{new NeoShadowsTypes.Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)};
        private static final ThreadLocal<BlockPos.MutableBlockPos> CURSOR_TL = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

        private SolidOccluderCache() {
        }

        static boolean firstHitScalar(ClientLevel level, Vec3 start, double endX, double endY, double endZ, long ignoredSource, long ignoredReceiver, OccluderHit out) {
            double deltaX = endX - start.f_82479_;
            double deltaY = endY - start.f_82480_;
            double deltaZ = endZ - start.f_82481_;
            double maxDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (maxDistance <= 1.0E-6) {
                return false;
            }
            double dx = deltaX / maxDistance;
            double dy = deltaY / maxDistance;
            double dz = deltaZ / maxDistance;
            SectionSkip.TraversalState traversal = SectionSkip.rentState().reset(start.f_82479_, start.f_82480_, start.f_82481_, dx, dy, dz, Mth.m_14107_((double)start.f_82479_), Mth.m_14107_((double)start.f_82480_), Mth.m_14107_((double)start.f_82481_));
            int minBuildHeight = level.m_141937_();
            ClientChunkCache chunkSource = level.m_7726_();
            int cachedChunkX = Integer.MIN_VALUE;
            int cachedChunkZ = Integer.MIN_VALUE;
            int cachedSectionIndex = Integer.MIN_VALUE;
            LevelChunk cachedChunk = null;
            LevelChunkSection[] cachedSections = null;
            LevelChunkSection cachedSection = null;
            boolean sectionIsAir = false;
            BlockPos.MutableBlockPos cursor = CURSOR_TL.get();
            while (traversal.traveled <= maxDistance) {
                double posX = traversal.currentX();
                double posY = traversal.currentY();
                double posZ = traversal.currentZ();
                long currentBlockKey = BlockPos.m_121882_((int)traversal.blockX, (int)traversal.blockY, (int)traversal.blockZ);
                if (currentBlockKey != ignoredSource && currentBlockKey != ignoredReceiver) {
                    int chunkX = traversal.blockX >> 4;
                    int chunkZ = traversal.blockZ >> 4;
                    int sectionIndex = traversal.blockY - minBuildHeight >> 4;
                    if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
                        cachedChunk = chunkSource.m_7131_(chunkX, chunkZ);
                        cachedSections = cachedChunk != null ? cachedChunk.m_7103_() : null;
                        cachedChunkX = chunkX;
                        cachedChunkZ = chunkZ;
                        cachedSectionIndex = Integer.MIN_VALUE;
                    }
                    if (cachedChunk != null) {
                        BlockState state;
                        if (sectionIndex != cachedSectionIndex) {
                            if (cachedSections == null || sectionIndex < 0 || sectionIndex >= cachedSections.length) {
                                cachedSection = null;
                                sectionIsAir = true;
                            } else {
                                cachedSection = cachedSections[sectionIndex];
                                sectionIsAir = cachedSection == null || cachedSection.m_188008_();
                            }
                            cachedSectionIndex = sectionIndex;
                        }
                        if (sectionIsAir) {
                            double nextTraveled = SectionSkip.skipEmptySection(traversal, chunkX, chunkZ, sectionIndex, minBuildHeight, maxDistance, 0.01);
                            if (!(nextTraveled >= maxDistance)) continue;
                            break;
                        }
                        if (!sectionIsAir && cachedSection != null && !(state = cachedSection.m_62982_(traversal.blockX & 0xF, traversal.blockY & 0xF, traversal.blockZ & 0xF)).m_60795_()) {
                            NeoShadowsTypes.Box[] boxes = BOX_CACHE.get(state);
                            if (boxes == null) {
                                cursor.m_122178_(traversal.blockX, traversal.blockY, traversal.blockZ);
                                NeoShadowsTypes.Box[] built = SolidOccluderCache.buildBoxes(state, level, (BlockPos)cursor);
                                NeoShadowsTypes.Box[] existing = BOX_CACHE.putIfAbsent(state, built);
                                NeoShadowsTypes.Box[] boxArray = boxes = existing != null ? existing : built;
                            }
                            if (boxes == FULL_BLOCK_BOXES) {
                                out.blockKey = BlockPos.m_121882_((int)traversal.blockX, (int)traversal.blockY, (int)traversal.blockZ);
                                out.boxes = boxes;
                                return true;
                            }
                            if (boxes.length == 1) {
                                if (boxes[0].intersectsSegmentWithOffset(start.f_82479_, start.f_82480_, start.f_82481_, endX, endY, endZ, traversal.blockX, traversal.blockY, traversal.blockZ)) {
                                    out.blockKey = BlockPos.m_121882_((int)traversal.blockX, (int)traversal.blockY, (int)traversal.blockZ);
                                    out.boxes = boxes;
                                    return true;
                                }
                            } else if (boxes.length > 1) {
                                for (NeoShadowsTypes.Box box : boxes) {
                                    if (!box.intersectsSegmentWithOffset(start.f_82479_, start.f_82480_, start.f_82481_, endX, endY, endZ, traversal.blockX, traversal.blockY, traversal.blockZ)) continue;
                                    out.blockKey = BlockPos.m_121882_((int)traversal.blockX, (int)traversal.blockY, (int)traversal.blockZ);
                                    out.boxes = boxes;
                                    return true;
                                }
                            }
                        }
                    }
                }
                double nextTraveled = SectionSkip.nextBoundaryDistance(traversal);
                SectionSkip.advanceTo(traversal, nextTraveled, 1.0E-9);
            }
            return false;
        }

        private static NeoShadowsTypes.Box[] buildBoxes(BlockState state, ClientLevel level, BlockPos pos) {
            VoxelShape shape = state.m_60812_((BlockGetter)level, pos);
            if (shape.m_83281_()) {
                return NO_BOXES;
            }
            if (state.m_60838_((BlockGetter)level, pos)) {
                return FULL_BLOCK_BOXES;
            }
            ArrayList boxes = new ArrayList();
            shape.m_83286_((minX, minY, minZ, maxX, maxY, maxZ) -> {
                if (maxX - minX > 1.0E-6 && maxY - minY > 1.0E-6 && maxZ - minZ > 1.0E-6) {
                    boxes.add(new NeoShadowsTypes.Box(minX, minY, minZ, maxX, maxY, maxZ));
                }
            });
            return boxes.isEmpty() ? NO_BOXES : boxes.toArray(new NeoShadowsTypes.Box[0]);
        }
    }
}

