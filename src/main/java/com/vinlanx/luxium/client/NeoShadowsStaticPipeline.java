/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsOcclusionPipeline;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsStaticPipeline {
    private final NeoShadowsEngine engine;
    private static final ThreadLocal<RandomSource> RANDOM_SOURCE_TL = ThreadLocal.withInitial(RandomSource::m_216327_);

    NeoShadowsStaticPipeline(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    NeoShadowsTypes.RealtimeStaticShadowOutput buildRealtimeStaticShadowOutput(ClientLevel level, BlockPos center) {
        return this.buildRealtimeStaticShadowOutput(level, center, null);
    }

    NeoShadowsTypes.RealtimeStaticShadowOutput buildRealtimeStaticShadowOutput(ClientLevel level, BlockPos center, @Nullable Consumer<List<NeoShadowsTypes.ShadowPolygon>> progressPublisher) {
        ExposedFaceService exposedFaceService = ExposedFaceService.get();
        NeoShadowsTypes.GeometryBuildState state = new NeoShadowsTypes.GeometryBuildState(this.engine, level, center, exposedFaceService, false, DynamicShadowMeshCapture.Snapshot.empty());
        TorchRtxState torchState = TorchRtxState.get();
        List<NeoShadowsTypes.FaceCandidate> candidates = this.engine.visibility.buildVisibleFaceCandidates(level, center, torchState, exposedFaceService, state);
        state.receiverFaceCount = candidates.size();
        List<NeoShadowsTypes.ReceiverPlaneGroup> planeGroups = candidates.isEmpty() ? Collections.emptyList() : this.buildReceiverPlaneGroups(torchState, candidates, state);
        NeoShadowsStaticPipeline.sortPlaneGroupsByPriority(planeGroups, center);
        int groupCount = planeGroups.size();
        if (groupCount > 0) {
            ExecutorService pool;
            int workerCount = Math.max(1, Math.min(NeoShadowsEngine.SHADOW_WORKER_COUNT, groupCount));
            NeoShadowsTypes.BuildScratch[] scratches = new NeoShadowsTypes.BuildScratch[workerCount];
            for (int i = 0; i < workerCount; ++i) {
                scratches[i] = new NeoShadowsTypes.BuildScratch();
            }
            ExecutorService executorService = pool = workerCount > 1 ? NeoShadowsEngine.shadowWorkerPool() : null;
            if (pool == null) {
                NeoShadowsTypes.BuildScratch scratch = scratches[0];
                scratch.reset();
                for (int i = 0; i < groupCount && state.shadowPolygons.size() < 12288; ++i) {
                    this.processReceiverPlaneGroup(level, planeGroups.get(i), state, scratch);
                }
                NeoShadowsEngine.mergeScratch(state, scratch);
                if (progressPublisher != null) {
                    progressPublisher.accept(state.shadowPolygons);
                }
            } else {
                int stripeSize = Math.max(workerCount, Math.max(1, groupCount / 4));
                for (int stripeStart = 0; stripeStart < groupCount && state.shadowPolygons.size() < 12288; stripeStart += stripeSize) {
                    int w;
                    int stripeEnd = Math.min(stripeStart + stripeSize, groupCount);
                    int activeWorkers = Math.min(workerCount, stripeEnd - stripeStart);
                    AtomicInteger nextIdx = new AtomicInteger(stripeStart);
                    CountDownLatch stripeLatch = new CountDownLatch(activeWorkers);
                    for (w = 0; w < activeWorkers; ++w) {
                        scratches[w].reset();
                    }
                    for (w = 0; w < activeWorkers; ++w) {
                        NeoShadowsTypes.BuildScratch scratch = scratches[w];
                        pool.execute(() -> {
                            try {
                                int idx;
                                while ((idx = nextIdx.getAndIncrement()) < stripeEnd) {
                                    if (state.shadowPolygons.size() >= 12288) {
                                        break;
                                    }
                                    this.processReceiverPlaneGroup(level, (NeoShadowsTypes.ReceiverPlaneGroup)planeGroups.get(idx), state, scratch);
                                }
                            }
                            catch (Throwable throwable) {
                            }
                            finally {
                                stripeLatch.countDown();
                            }
                        });
                    }
                    try {
                        stripeLatch.await();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    for (w = 0; w < activeWorkers; ++w) {
                        NeoShadowsEngine.mergeScratch(state, scratches[w]);
                    }
                    if (progressPublisher == null) continue;
                    progressPublisher.accept(state.shadowPolygons);
                }
            }
        }
        return new NeoShadowsTypes.RealtimeStaticShadowOutput(center.m_7949_(), List.copyOf(state.shadowPolygons), List.copyOf(planeGroups), state.receiverFaceCount, state.sourceCount, state.occluderHitCount, state.shadowPolygonCount);
    }

    List<NeoShadowsTypes.ShadowPolygon> buildRealtimeChangedBlockShadowPolygons(ClientLevel level, LongOpenHashSet changedBlocks, int maxBlocks, int localRadius) {
        return this.buildRealtimeChangedBlockShadowOutput((ClientLevel)level, (LongOpenHashSet)changedBlocks, (int)maxBlocks, (int)localRadius).shadowPolygons;
    }

    NeoShadowsTypes.StaticHotPatchOutput buildRealtimeChangedBlockShadowOutput(ClientLevel level, LongOpenHashSet changedBlocks, int maxBlocks, int localRadius) {
        if (level == null || changedBlocks == null || changedBlocks.isEmpty() || maxBlocks <= 0 || localRadius < 0) {
            return NeoShadowsTypes.StaticHotPatchOutput.EMPTY;
        }
        ExposedFaceService exposedFaceService = ExposedFaceService.get();
        TorchRtxState torchState = TorchRtxState.get();
        ArrayList<NeoShadowsTypes.ShadowPolygon> output = new ArrayList<NeoShadowsTypes.ShadowPolygon>();
        Long2ByteOpenHashMap affectedFacesByBlock = new Long2ByteOpenHashMap();
        affectedFacesByBlock.defaultReturnValue((byte)0);
        LongIterator iterator = changedBlocks.iterator();
        NeoShadowsTypes.BuildScratch scratch = new NeoShadowsTypes.BuildScratch();
        block0: for (int processed = 0; iterator.hasNext() && processed < maxBlocks && output.size() < 12288; ++processed) {
            BlockPos center = BlockPos.m_122022_((long)iterator.nextLong());
            NeoShadowsStaticPipeline.markImmediateHotPatchFaces(affectedFacesByBlock, center);
            NeoShadowsTypes.GeometryBuildState state = new NeoShadowsTypes.GeometryBuildState(this.engine, level, center, exposedFaceService, false, DynamicShadowMeshCapture.Snapshot.empty());
            List<NeoShadowsTypes.FaceCandidate> candidates = this.engine.visibility.buildLocalFaceCandidates(level, center, localRadius, exposedFaceService);
            state.receiverFaceCount = candidates.size();
            NeoShadowsStaticPipeline.markAffectedReceiverFaces(affectedFacesByBlock, candidates);
            if (candidates.isEmpty()) continue;
            List<NeoShadowsTypes.ReceiverPlaneGroup> planeGroups = this.buildReceiverPlaneGroups(torchState, candidates, state);
            for (NeoShadowsTypes.ReceiverPlaneGroup group : planeGroups) {
                if (output.size() >= 12288) continue block0;
                scratch.reset();
                this.processReceiverPlaneGroup(level, group, state, scratch);
                if (scratch.shadowPolygons.isEmpty()) continue;
                output.addAll(scratch.shadowPolygons);
            }
        }
        if (affectedFacesByBlock.isEmpty()) {
            return NeoShadowsTypes.StaticHotPatchOutput.EMPTY;
        }
        return new NeoShadowsTypes.StaticHotPatchOutput(affectedFacesByBlock, output.isEmpty() ? Collections.emptyList() : List.copyOf(output));
    }

    private static void markImmediateHotPatchFaces(Long2ByteOpenHashMap affectedFacesByBlock, BlockPos center) {
        byte allFacesMask = (byte)((1 << NeoShadowsEngine.FACES.length) - 1);
        NeoShadowsStaticPipeline.mergeAffectedFaceMask(affectedFacesByBlock, center.m_121878_(), allFacesMask);
        for (Direction face : NeoShadowsEngine.FACES) {
            NeoShadowsStaticPipeline.mergeAffectedFaceMask(affectedFacesByBlock, center.m_121945_(face).m_121878_(), allFacesMask);
        }
    }

    private static void markAffectedReceiverFaces(Long2ByteOpenHashMap affectedFacesByBlock, List<NeoShadowsTypes.FaceCandidate> candidates) {
        for (NeoShadowsTypes.FaceCandidate candidate : candidates) {
            NeoShadowsStaticPipeline.mergeAffectedFaceMask(affectedFacesByBlock, candidate.pos.m_121878_(), (byte)(1 << candidate.face.ordinal()));
        }
    }

    private static void mergeAffectedFaceMask(Long2ByteOpenHashMap affectedFacesByBlock, long blockKey, byte mask) {
        affectedFacesByBlock.put(blockKey, (byte)(affectedFacesByBlock.get(blockKey) | mask));
    }

    List<NeoShadowsTypes.ReceiverPlaneGroup> buildReceiverPlaneGroups(TorchRtxState torchState, List<NeoShadowsTypes.FaceCandidate> candidates, NeoShadowsTypes.GeometryBuildState state) {
        NeoShadowsTypes.FaceCandidate candidate;
        int initialCapacity = Math.max(16, Math.min(candidates.size() * 2, 4096));
        LinkedHashMap<NeoShadowsTypes.ReceiverPlaneKey, NeoShadowsTypes.ReceiverPlaneGroup> groups = new LinkedHashMap<NeoShadowsTypes.ReceiverPlaneKey, NeoShadowsTypes.ReceiverPlaneGroup>(initialCapacity);
        int n = candidates.size();
        NeoShadowsTypes.FaceLightingInfo[] lightingByIndex = new NeoShadowsTypes.FaceLightingInfo[n];
        int workerCount = Math.max(1, Math.min(NeoShadowsEngine.SHADOW_WORKER_COUNT, n));
        if (workerCount > 1 && n >= 64) {
            ExecutorService pool = NeoShadowsEngine.shadowWorkerPool();
            CountDownLatch latch = new CountDownLatch(workerCount);
            int w = 0;
            while (w < workerCount) {
                int shard = w++;
                pool.execute(() -> {
                    try {
                        for (int i = shard; i < n; i += workerCount) {
                            NeoShadowsTypes.FaceCandidate shardCandidate = candidates.get(i);
                            lightingByIndex[i] = this.engine.visibility.resolveFaceLightingCached(torchState, state, shardCandidate);
                        }
                    }
                    catch (Throwable throwable) {
                    }
                    finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            for (int i = 0; i < n; ++i) {
                candidate = candidates.get(i);
                lightingByIndex[i] = this.engine.visibility.resolveFaceLightingCached(torchState, state, candidate);
            }
        }
        for (int i = 0; i < n; ++i) {
            candidate = candidates.get(i);
            NeoShadowsTypes.FaceLightingInfo lighting = lightingByIndex[i];
            if (lighting == null) {
                lighting = this.engine.visibility.resolveFaceLightingCached(torchState, state, candidate);
            }
            if (lighting.contributions.isEmpty()) continue;
            long candidatePosKey = candidate.pos.m_121878_();
            for (NeoShadowsTypes.SourceContribution contribution : lighting.contributions) {
                if (contribution.sourceKey == Long.MIN_VALUE || contribution.sourceEmission <= 0 || contribution.sourceKey == candidatePosKey) continue;
                NeoShadowsTypes.ReceiverPlaneKey key = NeoShadowsTypes.ReceiverPlaneKey.of(candidate, contribution.sourceKey);
                NeoShadowsTypes.ReceiverPlaneGroup group = (NeoShadowsTypes.ReceiverPlaneGroup)groups.get(key);
                if (group == null) {
                    Vec3 source = new Vec3((double)BlockPos.m_121983_((long)contribution.sourceKey) + 0.5, (double)BlockPos.m_122008_((long)contribution.sourceKey) + 0.5, (double)BlockPos.m_122015_((long)contribution.sourceKey) + 0.5);
                    group = new NeoShadowsTypes.ReceiverPlaneGroup(key, source, contribution.sourceEmission);
                    groups.put(key, group);
                }
                group.addFace(candidate, contribution.weight, contribution.localFaceBounds);
            }
        }
        ArrayList<NeoShadowsTypes.ReceiverPlaneGroup> result = new ArrayList<NeoShadowsTypes.ReceiverPlaneGroup>(groups.values());
        int rgCount = result.size();
        if (rgCount > 1) {
            ExecutorService finalizePool = NeoShadowsEngine.shadowWorkerPool();
            CountDownLatch finalizeLatch = new CountDownLatch(rgCount);
            for (NeoShadowsTypes.ReceiverPlaneGroup group : result) {
                finalizePool.execute(() -> {
                    try {
                        group.finalizeReceiverSamples();
                    }
                    catch (Throwable throwable) {
                    }
                    finally {
                        finalizeLatch.countDown();
                    }
                });
            }
            try {
                finalizeLatch.await();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (rgCount == 1) {
            ((NeoShadowsTypes.ReceiverPlaneGroup)result.get(0)).finalizeReceiverSamples();
        }
        return result;
    }

    static void sortPlaneGroupsByPriority(List<NeoShadowsTypes.ReceiverPlaneGroup> planeGroups, BlockPos center) {
        if (planeGroups.size() <= 1) {
            return;
        }
        Vec3 centerVec = Vec3.m_82512_((Vec3i)center);
        planeGroups.sort((left, right) -> {
            int receiverCompare = Double.compare(left.nearestReceiverDistanceSq, right.nearestReceiverDistanceSq);
            if (receiverCompare != 0) {
                return receiverCompare;
            }
            double da = left.source.m_82557_(centerVec);
            double db = right.source.m_82557_(centerVec);
            return Double.compare(da, db);
        });
    }

    void processReceiverPlaneGroup(ClientLevel level, NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.GeometryBuildState state, NeoShadowsTypes.BuildScratch scratch) {
        if (group.faces.isEmpty() || scratch.shadowPolygons.size() >= 12288) {
            return;
        }
        if (!group.plane.isFacingSource(group.source)) {
            this.addBackFacingReceiverShadows(group, scratch);
            return;
        }
        Long2ObjectLinkedOpenHashMap<NeoShadowsOcclusionPipeline.OccluderHit> hitsByBlock = scratch.reusableGroupHits;
        hitsByBlock.clear();
        for (NeoShadowsTypes.FaceCandidate candidate : group.faces) {
            NeoShadowsTypes.ReceiverPlane facePlane = new NeoShadowsTypes.ReceiverPlane(candidate.pos, candidate.face, candidate.planeCoordinate);
            this.engine.occlusionPipeline.findFaceOccludersInto(level, facePlane, candidate, group, state, group.source, group.key.sourceKey, group.sourceEmission, hitsByBlock);
        }
        if (hitsByBlock.isEmpty() && !state.includeDynamicCasters) {
            return;
        }
        scratch.occluderHitCount += hitsByBlock.size();
        LongOpenHashSet mergedFullBlockHits = new LongOpenHashSet();
        this.appendMergedFullBlockOccluderShadows(level, group, hitsByBlock, mergedFullBlockHits, scratch);
        for (NeoShadowsOcclusionPipeline.OccluderHit hit : hitsByBlock.values()) {
            if (mergedFullBlockHits.contains(hit.blockPos.m_121878_())) continue;
            if (this.addModelShadowPolygons(level, group, hit, state, scratch)) {
                if (scratch.shadowPolygons.size() < 12288) continue;
                return;
            }
            for (NeoShadowsTypes.Box bounds : hit.boxes) {
                List<NeoShadowsTypes.UvPoint> projected = NeoShadowsProjection.collectProjectedBoxPoints(group.plane, group.source, bounds, hit.blockPos);
                List<NeoShadowsTypes.UvPoint> polygon = NeoShadowsProjection.buildProjectedShadowHullBuffered(projected, scratch);
                if (polygon == null) continue;
                this.addPlaneShadowPolygons(group, polygon, scratch);
                if (scratch.shadowPolygons.size() < 12288) continue;
                return;
            }
        }
        if (state.includeDynamicCasters && scratch.shadowPolygons.size() < 12288) {
            this.engine.dynamicPipeline.addDynamicCasterShadows(group, state, scratch);
        }
    }

    void appendMergedFullBlockOccluderShadows(ClientLevel level, NeoShadowsTypes.ReceiverPlaneGroup group, Long2ObjectLinkedOpenHashMap<NeoShadowsOcclusionPipeline.OccluderHit> hitsByBlock, LongOpenHashSet consumedHits, NeoShadowsTypes.BuildScratch scratch) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        ArrayList<BlockPos> cluster = new ArrayList<BlockPos>();
        LongOpenHashSet clusterKeys = new LongOpenHashSet();
        for (NeoShadowsOcclusionPipeline.OccluderHit seedHit : hitsByBlock.values()) {
            NeoShadowsTypes.Box mergedWorldBox;
            List<NeoShadowsTypes.UvPoint> projected;
            List<NeoShadowsTypes.UvPoint> polygon;
            int minZ;
            int minY;
            int minX;
            if (scratch.shadowPolygons.size() >= 12288) {
                return;
            }
            long seedKey = seedHit.blockPos.m_121878_();
            if (consumedHits.contains(seedKey) || !NeoShadowsStaticPipeline.isMergeableFullBlockOccluder(level, seedHit)) continue;
            queue.clear();
            cluster.clear();
            clusterKeys.clear();
            queue.add(seedHit.blockPos);
            clusterKeys.add(seedKey);
            int maxX = minX = seedHit.blockPos.m_123341_();
            int maxY = minY = seedHit.blockPos.m_123342_();
            int maxZ = minZ = seedHit.blockPos.m_123343_();
            while (!queue.isEmpty()) {
                BlockPos current = (BlockPos)queue.removeFirst();
                cluster.add(current);
                minX = Math.min(minX, current.m_123341_());
                maxX = Math.max(maxX, current.m_123341_());
                minY = Math.min(minY, current.m_123342_());
                maxY = Math.max(maxY, current.m_123342_());
                minZ = Math.min(minZ, current.m_123343_());
                maxZ = Math.max(maxZ, current.m_123343_());
                for (Direction direction : NeoShadowsEngine.FACES) {
                    NeoShadowsOcclusionPipeline.OccluderHit neighbourHit;
                    BlockPos neighbour = current.m_121945_(direction);
                    long neighbourKey = neighbour.m_121878_();
                    if (clusterKeys.contains(neighbourKey) || consumedHits.contains(neighbourKey) || (neighbourHit = (NeoShadowsOcclusionPipeline.OccluderHit)hitsByBlock.get(neighbourKey)) == null || !NeoShadowsStaticPipeline.isMergeableFullBlockOccluder(level, neighbourHit)) continue;
                    clusterKeys.add(neighbourKey);
                    queue.addLast(neighbour);
                }
            }
            int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            if (cluster.size() <= 1 || volume != cluster.size() || (polygon = NeoShadowsProjection.buildProjectedShadowHullBuffered(projected = NeoShadowsProjection.collectProjectedBoxPoints(group.plane, group.source, mergedWorldBox = new NeoShadowsTypes.Box(minX, minY, minZ, (double)maxX + 1.0, (double)maxY + 1.0, (double)maxZ + 1.0)), scratch)) == null) continue;
            this.addPlaneShadowPolygons(group, polygon, scratch);
            consumedHits.addAll((LongCollection)clusterKeys);
        }
    }

    static boolean isMergeableFullBlockOccluder(ClientLevel level, NeoShadowsOcclusionPipeline.OccluderHit hit) {
        if (hit == null || !NeoShadowsStaticPipeline.isFullBlockOccluder(hit.boxes)) {
            return false;
        }
        BlockState blockState = level.m_8055_(hit.blockPos);
        return !blockState.m_60795_() && blockState.m_60799_() == RenderShape.MODEL && blockState.m_60804_((BlockGetter)level, hit.blockPos);
    }

    boolean addModelShadowPolygons(ClientLevel level, NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsOcclusionPipeline.OccluderHit hit, NeoShadowsTypes.GeometryBuildState state, NeoShadowsTypes.BuildScratch scratch) {
        BlockState blockState = level.m_8055_(hit.blockPos);
        if (blockState.m_60799_() != RenderShape.MODEL) {
            return false;
        }
        BakedModel model = Minecraft.m_91087_().m_91289_().m_110910_(blockState);
        if (model == null) {
            return false;
        }
        List<BakedQuad> quads = NeoShadowsStaticPipeline.collectModelQuads(model, blockState);
        if (quads.isEmpty()) {
            return false;
        }
        boolean fullBlockOccluder = NeoShadowsStaticPipeline.isFullBlockOccluder(hit.boxes);
        boolean allSpritesOpaque = true;
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.m_173410_();
            if (sprite == null) continue;
            NeoShadowsTypes.SpriteOpaqueMask mask = this.getSpriteOpaqueMask(sprite);
            if (mask.fullyOpaque) continue;
            allSpritesOpaque = false;
            break;
        }
        if (fullBlockOccluder && allSpritesOpaque) {
            return false;
        }
        int polygonsBefore = scratch.shadowPolygons.size();
        block1: for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite;
            if (scratch.shadowPolygons.size() >= 12288) break;
            NeoShadowsTypes.TexturedQuad texturedQuad = NeoShadowsTypes.TexturedQuad.from(quad, hit.blockPos);
            if (texturedQuad == null || !texturedQuad.facesSource(group.source) || (sprite = quad.m_173410_()) == null) continue;
            NeoShadowsTypes.SpriteOpaqueMask mask = this.getSpriteOpaqueMask(sprite);
            if (mask.fullyTransparent) continue;
            if (mask.fullyOpaque) {
                this.addProjectedQuadShadow(group, texturedQuad.worldVertices, scratch);
                continue;
            }
            NeoShadowsTypes.SpritePixelBounds usedPixels = NeoShadowsStaticPipeline.computeQuadPixelBounds(texturedQuad, sprite);
            if (usedPixels == null) continue;
            for (NeoShadowsTypes.SpriteRect rect : mask.rectangles) {
                Vec3[] opaqueRectVertices;
                if (scratch.shadowPolygons.size() >= 12288) continue block1;
                double minPixelX = Math.max((double)rect.minX, usedPixels.minX);
                double maxPixelX = Math.min((double)rect.maxX, usedPixels.maxX);
                double minPixelY = Math.max((double)rect.minY, usedPixels.minY);
                double maxPixelY = Math.min((double)rect.maxY, usedPixels.maxY);
                if (maxPixelX - minPixelX <= 1.0E-6 || maxPixelY - minPixelY <= 1.0E-6 || (opaqueRectVertices = NeoShadowsStaticPipeline.buildOpaqueRectVertices(texturedQuad, sprite, minPixelX, maxPixelX, minPixelY, maxPixelY)) == null) continue;
                this.addProjectedQuadShadow(group, opaqueRectVertices, scratch);
            }
        }
        return scratch.shadowPolygons.size() > polygonsBefore;
    }

    static List<BakedQuad> collectModelQuads(BakedModel model, BlockState state) {
        ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
        RandomSource random = RANDOM_SOURCE_TL.get();
        for (Direction face : NeoShadowsEngine.FACES) {
            random.m_188584_(42L);
            quads.addAll(model.m_213637_(state, face, random));
        }
        random.m_188584_(42L);
        quads.addAll(model.m_213637_(state, null, random));
        return quads;
    }

    boolean addProjectedQuadShadow(NeoShadowsTypes.ReceiverPlaneGroup group, Vec3[] quadVertices, NeoShadowsTypes.BuildScratch scratch) {
        List<NeoShadowsTypes.UvPoint> projected = NeoShadowsStaticPipeline.collectProjectedQuadPoints(group.plane, group.source, quadVertices);
        List<NeoShadowsTypes.UvPoint> polygon = NeoShadowsProjection.buildProjectedShadowHullBuffered(projected, scratch);
        if (polygon == null) {
            return false;
        }
        int polygonsBefore = scratch.shadowPolygons.size();
        this.addPlaneShadowPolygons(group, polygon, scratch);
        return scratch.shadowPolygons.size() > polygonsBefore;
    }

    static boolean facesSource(Vec3[] quadVertices, Vec3 source) {
        if (quadVertices.length < 3) {
            return false;
        }
        Vec3 normal = quadVertices[1].m_82546_(quadVertices[0]).m_82537_(quadVertices[2].m_82546_(quadVertices[0]));
        if (normal.m_82556_() <= 1.0E-12) {
            return false;
        }
        return normal.m_82526_(source.m_82546_(quadVertices[0])) > 1.0E-6;
    }

    NeoShadowsTypes.SpriteOpaqueMask getSpriteOpaqueMask(TextureAtlasSprite sprite) {
        return this.engine.spriteOpaqueMaskCache.computeIfAbsent(sprite, this::buildSpriteOpaqueMask);
    }

    NeoShadowsTypes.SpriteOpaqueMask buildSpriteOpaqueMask(TextureAtlasSprite sprite) {
        int width = Math.max(1, sprite.m_245424_().m_246492_());
        int height = Math.max(1, sprite.m_245424_().m_245330_());
        boolean[] opaquePixels = new boolean[width * height];
        boolean anyOpaque = false;
        boolean anyTransparent = false;
        for (int pixelY = 0; pixelY < height; ++pixelY) {
            int rowOffset = pixelY * width;
            for (int pixelX = 0; pixelX < width; ++pixelX) {
                boolean opaque;
                int alpha = FastColor.ARGB32.m_13655_((int)sprite.getPixelRGBA(0, pixelX, pixelY));
                opaquePixels[rowOffset + pixelX] = opaque = alpha >= 16;
                anyOpaque |= opaque;
                anyTransparent |= !opaque;
            }
        }
        if (!anyOpaque) {
            return NeoShadowsTypes.SpriteOpaqueMask.fullyTransparent();
        }
        if (!anyTransparent) {
            return NeoShadowsTypes.SpriteOpaqueMask.fullyOpaque();
        }
        boolean[] consumed = new boolean[opaquePixels.length];
        ArrayList<NeoShadowsTypes.SpriteRect> rectangles = new ArrayList<NeoShadowsTypes.SpriteRect>();
        for (int pixelY = 0; pixelY < height; ++pixelY) {
            for (int pixelX = 0; pixelX < width; ++pixelX) {
                int nextIndex;
                int index = pixelY * width + pixelX;
                if (!opaquePixels[index] || consumed[index]) continue;
                int rectWidth = 1;
                while (pixelX + rectWidth < width && opaquePixels[nextIndex = pixelY * width + pixelX + rectWidth] && !consumed[nextIndex]) {
                    ++rectWidth;
                }
                int rectHeight = 1;
                boolean canGrow = true;
                while (pixelY + rectHeight < height && canGrow) {
                    int nextRowOffset = (pixelY + rectHeight) * width;
                    for (int scanX = 0; scanX < rectWidth; ++scanX) {
                        int nextIndex2 = nextRowOffset + pixelX + scanX;
                        if (opaquePixels[nextIndex2] && !consumed[nextIndex2]) continue;
                        canGrow = false;
                        break;
                    }
                    if (!canGrow) continue;
                    ++rectHeight;
                }
                for (int fillY = 0; fillY < rectHeight; ++fillY) {
                    int fillRowOffset = (pixelY + fillY) * width;
                    for (int fillX = 0; fillX < rectWidth; ++fillX) {
                        consumed[fillRowOffset + pixelX + fillX] = true;
                    }
                }
                rectangles.add(new NeoShadowsTypes.SpriteRect(pixelX, pixelX + rectWidth, pixelY, pixelY + rectHeight));
            }
        }
        return new NeoShadowsTypes.SpriteOpaqueMask(rectangles.toArray(new NeoShadowsTypes.SpriteRect[0]), false, false);
    }

    @Nullable
    static NeoShadowsTypes.SpritePixelBounds computeQuadPixelBounds(NeoShadowsTypes.TexturedQuad quad, TextureAtlasSprite sprite) {
        int spriteWidth = Math.max(1, sprite.m_245424_().m_246492_());
        int spriteHeight = Math.max(1, sprite.m_245424_().m_245330_());
        double minPixelX = Double.POSITIVE_INFINITY;
        double maxPixelX = Double.NEGATIVE_INFINITY;
        double minPixelY = Double.POSITIVE_INFINITY;
        double maxPixelY = Double.NEGATIVE_INFINITY;
        for (NeoShadowsTypes.AtlasUv uv : quad.atlasUvs) {
            double pixelX = (double)(sprite.m_174727_((float)uv.u) * (float)spriteWidth) / 16.0;
            double pixelY = (double)(sprite.m_174741_((float)uv.v) * (float)spriteHeight) / 16.0;
            minPixelX = Math.min(minPixelX, pixelX);
            maxPixelX = Math.max(maxPixelX, pixelX);
            minPixelY = Math.min(minPixelY, pixelY);
            maxPixelY = Math.max(maxPixelY, pixelY);
        }
        if (!(Double.isFinite(minPixelX) && Double.isFinite(minPixelY) && Double.isFinite(maxPixelX) && Double.isFinite(maxPixelY))) {
            return null;
        }
        minPixelX = Mth.m_14008_((double)minPixelX, (double)0.0, (double)spriteWidth);
        maxPixelX = Mth.m_14008_((double)maxPixelX, (double)0.0, (double)spriteWidth);
        minPixelY = Mth.m_14008_((double)minPixelY, (double)0.0, (double)spriteHeight);
        maxPixelY = Mth.m_14008_((double)maxPixelY, (double)0.0, (double)spriteHeight);
        if (maxPixelX - minPixelX <= 1.0E-6 || maxPixelY - minPixelY <= 1.0E-6) {
            return null;
        }
        return new NeoShadowsTypes.SpritePixelBounds(minPixelX, maxPixelX, minPixelY, maxPixelY);
    }

    @Nullable
    static Vec3[] buildOpaqueRectVertices(NeoShadowsTypes.TexturedQuad quad, TextureAtlasSprite sprite, double minPixelX, double maxPixelX, double minPixelY, double maxPixelY) {
        int spriteWidth = Math.max(1, sprite.m_245424_().m_246492_());
        int spriteHeight = Math.max(1, sprite.m_245424_().m_245330_());
        double minAtlasU = sprite.m_118367_(minPixelX * 16.0 / (double)spriteWidth);
        double maxAtlasU = sprite.m_118367_(maxPixelX * 16.0 / (double)spriteWidth);
        double minAtlasV = sprite.m_118393_(minPixelY * 16.0 / (double)spriteHeight);
        double maxAtlasV = sprite.m_118393_(maxPixelY * 16.0 / (double)spriteHeight);
        Vec3 topLeft = NeoShadowsStaticPipeline.mapAtlasUvToWorld(quad, minAtlasU, minAtlasV);
        Vec3 topRight = NeoShadowsStaticPipeline.mapAtlasUvToWorld(quad, maxAtlasU, minAtlasV);
        Vec3 bottomRight = NeoShadowsStaticPipeline.mapAtlasUvToWorld(quad, maxAtlasU, maxAtlasV);
        Vec3 bottomLeft = NeoShadowsStaticPipeline.mapAtlasUvToWorld(quad, minAtlasU, maxAtlasV);
        if (topLeft == null || topRight == null || bottomRight == null || bottomLeft == null) {
            return null;
        }
        return new Vec3[]{topLeft, topRight, bottomRight, bottomLeft};
    }

    @Nullable
    static Vec3 mapAtlasUvToWorld(NeoShadowsTypes.TexturedQuad quad, double atlasU, double atlasV) {
        Vec3 mapped = NeoShadowsStaticPipeline.mapAtlasUvToWorldTriangle(quad, 0, 1, 2, atlasU, atlasV);
        if (mapped != null) {
            return mapped;
        }
        return NeoShadowsStaticPipeline.mapAtlasUvToWorldTriangle(quad, 0, 2, 3, atlasU, atlasV);
    }

    @Nullable
    static Vec3 mapAtlasUvToWorldTriangle(NeoShadowsTypes.TexturedQuad quad, int indexA, int indexB, int indexC, double atlasU, double atlasV) {
        NeoShadowsTypes.AtlasUv uvA = quad.atlasUvs[indexA];
        NeoShadowsTypes.AtlasUv uvB = quad.atlasUvs[indexB];
        NeoShadowsTypes.AtlasUv uvC = quad.atlasUvs[indexC];
        double denominator = (uvB.v - uvC.v) * (uvA.u - uvC.u) + (uvC.u - uvB.u) * (uvA.v - uvC.v);
        if (Math.abs(denominator) <= 1.0E-6) {
            return null;
        }
        double weightA = ((uvB.v - uvC.v) * (atlasU - uvC.u) + (uvC.u - uvB.u) * (atlasV - uvC.v)) / denominator;
        double weightB = ((uvC.v - uvA.v) * (atlasU - uvC.u) + (uvA.u - uvC.u) * (atlasV - uvC.v)) / denominator;
        double weightC = 1.0 - weightA - weightB;
        double epsilon = 1.0E-4;
        if (weightA < -epsilon || weightB < -epsilon || weightC < -epsilon) {
            return null;
        }
        Vec3 vertexA = quad.worldVertices[indexA];
        Vec3 vertexB = quad.worldVertices[indexB];
        Vec3 vertexC = quad.worldVertices[indexC];
        return new Vec3(vertexA.f_82479_ * weightA + vertexB.f_82479_ * weightB + vertexC.f_82479_ * weightC, vertexA.f_82480_ * weightA + vertexB.f_82480_ * weightB + vertexC.f_82480_ * weightC, vertexA.f_82481_ * weightA + vertexB.f_82481_ * weightB + vertexC.f_82481_ * weightC);
    }

    static boolean isFullBlockOccluder(NeoShadowsTypes.Box[] boxes) {
        if (boxes.length != 1) {
            return false;
        }
        NeoShadowsTypes.Box box = boxes[0];
        return box.minX <= 1.0E-6 && box.minY <= 1.0E-6 && box.minZ <= 1.0E-6 && box.maxX >= 0.999999 && box.maxY >= 0.999999 && box.maxZ >= 0.999999;
    }

    static List<NeoShadowsTypes.UvPoint> collectProjectedQuadPoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, Vec3[] corners) {
        NeoShadowsTypes.ProjectionPoint[] projectedCorners = new NeoShadowsTypes.ProjectionPoint[corners.length];
        ArrayList<NeoShadowsTypes.UvPoint> points = new ArrayList<NeoShadowsTypes.UvPoint>(corners.length + NeoShadowsEngine.QUAD_EDGE_VERTEX_INDICES.length * 2);
        for (int index = 0; index < corners.length; ++index) {
            NeoShadowsTypes.ProjectionPoint projection;
            projectedCorners[index] = projection = plane.projectPoint(source, corners[index]);
            if (projection.uv == null) continue;
            points.add(projection.uv);
        }
        for (int[] edge : NeoShadowsEngine.QUAD_EDGE_VERTEX_INDICES) {
            NeoShadowsStaticPipeline.appendExtendedQuadEdgePoints(plane, source, corners[edge[0]], corners[edge[1]], projectedCorners[edge[0]], projectedCorners[edge[1]], points);
        }
        return points;
    }

    private static void appendExtendedQuadEdgePoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, Vec3 start, Vec3 end, NeoShadowsTypes.ProjectionPoint startProjection, NeoShadowsTypes.ProjectionPoint endProjection, List<NeoShadowsTypes.UvPoint> points) {
        Vec3 edgeDirection;
        boolean endValid;
        boolean startValid = startProjection.uv != null;
        boolean bl = endValid = endProjection.uv != null;
        if (startValid == endValid) {
            return;
        }
        double denominatorDelta = endProjection.denominator - startProjection.denominator;
        if (Math.abs(denominatorDelta) <= 1.0E-6) {
            return;
        }
        double singularParam = -startProjection.denominator / denominatorDelta;
        if (singularParam < -1.0E-6 || singularParam > 1.000001) {
            return;
        }
        singularParam = Math.max(0.0, Math.min(1.0, singularParam));
        boolean validFromStart = startValid;
        double outerOffset = validFromStart ? -0.001 : 0.001;
        double innerOffset = validFromStart ? -2.5E-4 : 2.5E-4;
        double outerParam = NeoShadowsProjection.clampUnit(singularParam + outerOffset);
        double innerParam = NeoShadowsProjection.clampUnit(singularParam + innerOffset);
        if (Math.abs(innerParam - outerParam) <= 1.0E-6) {
            return;
        }
        NeoShadowsTypes.UvPoint outerUv = plane.projectFinite(source, NeoShadowsProjection.lerp(start, end, outerParam));
        NeoShadowsTypes.UvPoint innerUv = plane.projectFinite(source, NeoShadowsProjection.lerp(start, end, innerParam));
        if (outerUv == null || innerUv == null) {
            return;
        }
        points.add(outerUv);
        points.add(innerUv);
        double directionU = innerUv.u - outerUv.u;
        double directionV = innerUv.v - outerUv.v;
        double directionLengthSq = directionU * directionU + directionV * directionV;
        if (directionLengthSq <= 1.0E-6 && (directionLengthSq = (directionU = (edgeDirection = end.m_82546_(start)).m_82526_(plane.axisU)) * directionU + (directionV = edgeDirection.m_82526_(plane.axisV)) * directionV) <= 1.0E-6) {
            return;
        }
        double directionLength = Math.sqrt(directionLengthSq);
        points.add(new NeoShadowsTypes.UvPoint(innerUv.u + directionU / directionLength * 8.0, innerUv.v + directionV / directionLength * 8.0));
    }

    void addBackFacingReceiverShadows(NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.BuildScratch scratch) {
        for (NeoShadowsTypes.ReceiverContributionRect rect : group.receiverRects()) {
            if (scratch.shadowPolygons.size() >= 12288) {
                return;
            }
            float strength = rect.strength;
            if (strength <= 0.0f) continue;
            NeoShadowsTypes.PlaneRect planeRect = rect.planeRect;
            if (planeRect.maxU - planeRect.minU <= 1.0E-6 || planeRect.maxV - planeRect.minV <= 1.0E-6) continue;
            List<NeoShadowsTypes.UvPoint> contributionRect = NeoShadowsProjection.rectShadowPolygon(planeRect);
            double[] vertices = new double[contributionRect.size() * 3];
            for (int index = 0; index < contributionRect.size(); ++index) {
                NeoShadowsTypes.UvPoint uv = contributionRect.get(index);
                int offset = index * 3;
                vertices[offset] = group.plane.sampleX(uv.u, uv.v);
                vertices[offset + 1] = group.plane.sampleY(uv.u, uv.v);
                vertices[offset + 2] = group.plane.sampleZ(uv.u, uv.v);
            }
            scratch.shadowPolygons.add(new NeoShadowsTypes.ShadowPolygon(rect.anchorPos, group.key.face, vertices, strength));
            ++scratch.shadowPolygonCount;
        }
    }

    void addPlaneShadowPolygons(NeoShadowsTypes.ReceiverPlaneGroup group, List<NeoShadowsTypes.UvPoint> planePolygon, NeoShadowsTypes.BuildScratch scratch) {
        double minU = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (NeoShadowsTypes.UvPoint point : planePolygon) {
            minU = Math.min(minU, point.u);
            maxU = Math.max(maxU, point.u);
            minV = Math.min(minV, point.v);
            maxV = Math.max(maxV, point.v);
        }
        for (NeoShadowsTypes.ReceiverContributionRect receiverRect : group.receiverRects()) {
            if (scratch.shadowPolygons.size() >= 12288) {
                return;
            }
            NeoShadowsTypes.PlaneRect rect = receiverRect.planeRect;
            if (rect.maxU - rect.minU <= 1.0E-6 || rect.maxV - rect.minV <= 1.0E-6 || rect.maxU <= minU || rect.minU >= maxU || rect.maxV <= minV || rect.minV >= maxV) continue;
            if (rect.maxU - rect.minU > 1.000001 || rect.maxV - rect.minV > 1.000001) {
                int minCellU = Mth.m_14107_((double)(rect.minU + 1.0E-6));
                int maxCellU = Mth.m_14165_((double)(rect.maxU - 1.0E-6)) - 1;
                int minCellV = Mth.m_14107_((double)(rect.minV + 1.0E-6));
                int maxCellV = Mth.m_14165_((double)(rect.maxV - 1.0E-6)) - 1;
                for (int cellV = minCellV; cellV <= maxCellV; ++cellV) {
                    for (int cellU = minCellU; cellU <= maxCellU; ++cellU) {
                        if (scratch.shadowPolygons.size() >= 12288) {
                            return;
                        }
                        NeoShadowsTypes.PlaneRect cellRect = new NeoShadowsTypes.PlaneRect(cellU, (double)cellU + 1.0, cellV, (double)cellV + 1.0);
                        BlockPos anchorPos = NeoShadowsProjection.planeCellToBlockPos(group.key, cellU, cellV);
                        this.appendPlaneShadowPolygon(group, planePolygon, minU, maxU, minV, maxV, cellRect, anchorPos, receiverRect.strength, scratch);
                    }
                }
                continue;
            }
            this.appendPlaneShadowPolygon(group, planePolygon, minU, maxU, minV, maxV, rect, receiverRect.anchorPos, receiverRect.strength, scratch);
        }
    }

    private void appendRectShadowPolygon(NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.PlaneRect rect, BlockPos anchorPos, float strength, NeoShadowsTypes.BuildScratch scratch) {
        List<NeoShadowsTypes.UvPoint> clipped = NeoShadowsProjection.rectShadowPolygon(rect);
        double[] vertices = new double[clipped.size() * 3];
        for (int index = 0; index < clipped.size(); ++index) {
            NeoShadowsTypes.UvPoint uv = clipped.get(index);
            int offset = index * 3;
            vertices[offset] = group.plane.sampleX(uv.u, uv.v);
            vertices[offset + 1] = group.plane.sampleY(uv.u, uv.v);
            vertices[offset + 2] = group.plane.sampleZ(uv.u, uv.v);
        }
        scratch.shadowPolygons.add(new NeoShadowsTypes.ShadowPolygon(anchorPos, group.key.face, vertices, strength));
        ++scratch.shadowPolygonCount;
    }

    void appendPlaneShadowPolygon(NeoShadowsTypes.ReceiverPlaneGroup group, List<NeoShadowsTypes.UvPoint> planePolygon, double minU, double maxU, double minV, double maxV, NeoShadowsTypes.PlaneRect rect, BlockPos anchorPos, float strength, NeoShadowsTypes.BuildScratch scratch) {
        if (strength <= 0.0f) {
            return;
        }
        if (rect.maxU <= minU || rect.minU >= maxU || rect.maxV <= minV || rect.minV >= maxV) {
            return;
        }
        if (minU <= rect.minU && maxU >= rect.maxU && minV <= rect.minV && maxV >= rect.maxV && NeoShadowsProjection.rectFullyInsideConvexPolygon(planePolygon, rect)) {
            this.appendRectShadowPolygon(group, rect, anchorPos, strength, scratch);
            return;
        }
        List<NeoShadowsTypes.UvPoint> clipped = NeoShadowsProjection.clipToRectangleBuffered(planePolygon, rect.minU, rect.maxU, rect.minV, rect.maxV, scratch);
        if (clipped.size() < 3 || NeoShadowsProjection.polygonArea(clipped) <= 1.0E-5) {
            return;
        }
        if ((clipped = NeoShadowsProjection.buildConvexHull(clipped)).size() < 3 || NeoShadowsProjection.polygonArea(clipped) <= 1.0E-5) {
            return;
        }
        if ((clipped = NeoShadowsProjection.normalizeShadowPolygonToRect(clipped, rect, group.key.face, scratch)) == null || clipped.size() < 3 || NeoShadowsProjection.polygonArea(clipped) <= 1.0E-5) {
            return;
        }
        double[] vertices = new double[clipped.size() * 3];
        for (int index = 0; index < clipped.size(); ++index) {
            NeoShadowsTypes.UvPoint uv = clipped.get(index);
            int offset = index * 3;
            vertices[offset] = group.plane.sampleX(uv.u, uv.v);
            vertices[offset + 1] = group.plane.sampleY(uv.u, uv.v);
            vertices[offset + 2] = group.plane.sampleZ(uv.u, uv.v);
        }
        scratch.shadowPolygons.add(new NeoShadowsTypes.ShadowPolygon(anchorPos, group.key.face, vertices, strength));
        ++scratch.shadowPolygonCount;
    }
}
