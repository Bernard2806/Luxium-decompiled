/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsVisibility {
    private static final int SPARSE_TOUCHED_SECTION_THRESHOLD = 24;
    private static final ThreadLocal<NeoShadowsTypes.ContributorCollector> CONTRIBUTOR_COLLECTOR_TL = ThreadLocal.withInitial(NeoShadowsTypes.ContributorCollector::new);
    private static final ThreadLocal<NeoShadowsTypes.FaceLightingScratch> FACE_LIGHTING_SCRATCH_TL = ThreadLocal.withInitial(NeoShadowsTypes.FaceLightingScratch::new);
    private static final ThreadLocal<TouchedSectionScratch> TOUCHED_SECTION_SCRATCH_TL = ThreadLocal.withInitial(TouchedSectionScratch::new);
    private final NeoShadowsEngine engine;
    static final long LIT_SAMPLE_CACHE_EMPTY = Long.MAX_VALUE;

    NeoShadowsVisibility(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    List<NeoShadowsTypes.FaceCandidate> buildVisibleFaceCandidates(ClientLevel level, BlockPos center, TorchRtxState torchState, ExposedFaceService exposedFaceService, NeoShadowsTypes.GeometryBuildState state) {
        double maxLightReach = Math.min(LightRtMath.getMaxDistance(15), 30.0);
        double sourceSearchRadiusSq = (36.0 + maxLightReach) * (36.0 + maxLightReach);
        Vec3 centerVec = Vec3.m_82512_((Vec3i)center);
        long[] sourcesInRange = torchState.getClosestSources(centerVec, sourceSearchRadiusSq, 256);
        state.sourceCount = sourcesInRange.length;
        if (sourcesInRange.length == 0) {
            return Collections.emptyList();
        }
        double renderRadiusSq = 1296.0;
        long[] frontierPositions = torchState.getFrontierPositions(center, sourceSearchRadiusSq);
        if (frontierPositions.length == 0) {
            return Collections.emptyList();
        }
        NeoShadowsTypes.VisibleCandidateBlockSnapshot snapshot = this.collectVisibleCandidateBlockSnapshot(level, center, exposedFaceService, frontierPositions, renderRadiusSq);
        if (snapshot.candidates.length == 0) {
            return Collections.emptyList();
        }
        return snapshot.asList();
    }

    NeoShadowsTypes.VisibleCandidateBlockSnapshot collectVisibleCandidateBlockSnapshot(ClientLevel level, BlockPos center, ExposedFaceService exposedFaceService, long[] frontierPositions, double renderRadiusSq) {
        NeoShadowsTypes.VisibleCandidateBlockSnapshot snapshot;
        long frontierSignature = NeoShadowsVisibility.hashFrontierPositions(frontierPositions);
        NeoShadowsTypes.VisibleCandidateBlockSnapshot cached = this.engine.visibleCandidateSnapshot;
        if (cached != null && cached.matches(center, frontierSignature, frontierPositions)) {
            return cached;
        }
        int centerX = center.m_123341_();
        int centerY = center.m_123342_();
        int centerZ = center.m_123343_();
        int limit = 12288;
        Long2ObjectOpenHashMap<TouchedSectionData> touchedSections = this.collectTouchedReceiverSections(level, exposedFaceService, frontierPositions, centerX, centerY, centerZ, renderRadiusSq);
        if (touchedSections.isEmpty()) {
            NeoShadowsTypes.VisibleCandidateBlockSnapshot empty;
            this.engine.visibleCandidateSnapshot = empty = NeoShadowsTypes.VisibleCandidateBlockSnapshot.empty(center, frontierSignature, frontierPositions);
            return empty;
        }
        NearestFaceCandidateCollector candidates = new NearestFaceCandidateCollector(limit);
        for (Long2ObjectMap.Entry entry : touchedSections.long2ObjectEntrySet()) {
            this.appendSectionReceiverCandidates(candidates, level, exposedFaceService, entry.getLongKey(), (TouchedSectionData)entry.getValue(), center);
        }
        NeoShadowsTypes.FaceCandidate[] sortedCandidates = candidates.toSortedArray();
        if (sortedCandidates.length == 0) {
            NeoShadowsTypes.VisibleCandidateBlockSnapshot empty;
            this.engine.visibleCandidateSnapshot = empty = NeoShadowsTypes.VisibleCandidateBlockSnapshot.empty(center, frontierSignature, frontierPositions);
            return empty;
        }
        this.engine.visibleCandidateSnapshot = snapshot = new NeoShadowsTypes.VisibleCandidateBlockSnapshot(center.m_7949_(), frontierSignature, Arrays.copyOf(frontierPositions, frontierPositions.length), sortedCandidates);
        return snapshot;
    }

    Long2ObjectOpenHashMap<TouchedSectionData> collectTouchedReceiverSections(ClientLevel level, ExposedFaceService exposedFaceService, long[] frontierPositions, int centerX, int centerY, int centerZ, double renderRadiusSq) {
        TouchedSectionScratch scratch = TOUCHED_SECTION_SCRATCH_TL.get();
        scratch.reset();
        Long2ObjectOpenHashMap<TouchedSectionData> touchedSections = scratch.touchedSections;
        for (long frontierKey : frontierPositions) {
            int frontierX = BlockPos.m_121983_((long)frontierKey);
            int frontierY = BlockPos.m_122008_((long)frontierKey);
            int frontierZ = BlockPos.m_122015_((long)frontierKey);
            long lastSectionKey = Long.MIN_VALUE;
            TouchedSectionData lastTouchedSection = null;
            ExposedFaceService.SectionFaceSnapshot lastSectionSnapshot = ExposedFaceService.SectionFaceSnapshot.empty();
            boolean lastSectionHasSnapshot = false;
            boolean lastSectionRejectAll = false;
            for (int[] offset : NeoShadowsEngine.RECEIVER_OFFSETS) {
                int bx = frontierX + offset[0];
                double dx = bx - centerX;
                int by = frontierY + offset[1];
                double dy = by - centerY;
                int bz = frontierZ + offset[2];
                double dz = bz - centerZ;
                if (dx * dx + dy * dy + dz * dz > renderRadiusSq) continue;
                long sectionKey = SectionPos.m_123209_((int)SectionPos.m_123171_((int)bx), (int)SectionPos.m_123171_((int)by), (int)SectionPos.m_123171_((int)bz));
                if (sectionKey != lastSectionKey) {
                    lastSectionHasSnapshot = exposedFaceService.isSectionFullyCaptured(sectionKey) && exposedFaceService.hasSectionFaceSnapshot(sectionKey);
                    lastSectionSnapshot = lastSectionHasSnapshot ? exposedFaceService.snapshotSectionFaces(level, sectionKey) : ExposedFaceService.SectionFaceSnapshot.empty();
                    lastSectionRejectAll = lastSectionHasSnapshot && !lastSectionSnapshot.hasCandidateReceivers();
                    lastSectionKey = sectionKey;
                    lastTouchedSection = null;
                }
                if (lastSectionRejectAll) continue;
                int localIndex = NeoShadowsVisibility.localSectionBlockIndex(bx & 0xF, by & 0xF, bz & 0xF);
                if (lastSectionHasSnapshot && !lastSectionSnapshot.isRenderableSolid(localIndex)) continue;
                TouchedSectionData touchedSection = lastTouchedSection;
                if (touchedSection == null) {
                    touchedSection = (TouchedSectionData)touchedSections.get(sectionKey);
                    if (touchedSection == null) {
                        touchedSection = scratch.rentTouchedSection();
                        touchedSections.put(sectionKey, (Object)touchedSection);
                    }
                    lastTouchedSection = touchedSection;
                }
                touchedSection.add(localIndex, scratch);
            }
        }
        return touchedSections;
    }

    void appendSectionReceiverCandidates(NearestFaceCandidateCollector candidates, ClientLevel level, ExposedFaceService exposedFaceService, long sectionKey, TouchedSectionData touchedSection, BlockPos center) {
        int sectionBaseX = SectionPos.m_123213_((long)sectionKey) << 4;
        int sectionBaseY = SectionPos.m_123225_((long)sectionKey) << 4;
        int sectionBaseZ = SectionPos.m_123230_((long)sectionKey) << 4;
        this.appendTouchedFaceMaskFallback(candidates, level, exposedFaceService, sectionBaseX, sectionBaseY, sectionBaseZ, touchedSection, center);
    }

    void appendTouchedFaceMaskFallback(NearestFaceCandidateCollector candidates, ClientLevel level, ExposedFaceService exposedFaceService, int sectionBaseX, int sectionBaseY, int sectionBaseZ, TouchedSectionData touchedSection, BlockPos center) {
        if (touchedSection.isDense()) {
            DenseTouchedBits denseBits = touchedSection.denseBits();
            for (int dirtyIndex = 0; dirtyIndex < denseBits.dirtyWordCount; ++dirtyIndex) {
                int wordIndex = denseBits.dirtyWordIndices[dirtyIndex];
                for (long remainingBits = denseBits.words[wordIndex]; remainingBits != 0L; remainingBits &= remainingBits - 1L) {
                    int bitIndex = Long.numberOfTrailingZeros(remainingBits);
                    int localIndex = (wordIndex << 6) + bitIndex;
                    int localY = localIndex >>> 8;
                    int localZ = localIndex >>> 4 & 0xF;
                    int localX = localIndex & 0xF;
                    long blockKey = BlockPos.m_121882_((int)(sectionBaseX + localX), (int)(sectionBaseY + localY), (int)(sectionBaseZ + localZ));
                    this.appendReceiverSurfaceCandidates(candidates, exposedFaceService, level, blockKey, center);
                }
            }
            return;
        }
        for (int index = 0; index < touchedSection.sparseCount(); ++index) {
            int localIndex = touchedSection.sparseIndexAt(index);
            int localY = localIndex >>> 8;
            int localZ = localIndex >>> 4 & 0xF;
            int localX = localIndex & 0xF;
            long blockKey = BlockPos.m_121882_((int)(sectionBaseX + localX), (int)(sectionBaseY + localY), (int)(sectionBaseZ + localZ));
            this.appendReceiverSurfaceCandidates(candidates, exposedFaceService, level, blockKey, center);
        }
    }

    void appendReceiverSurfaceCandidates(NearestFaceCandidateCollector candidates, ExposedFaceService exposedFaceService, ClientLevel level, long blockKey, BlockPos center) {
        double dz;
        double dy;
        BlockPos pos = BlockPos.m_122022_((long)blockKey);
        double dx = pos.m_123341_() - center.m_123341_();
        double distanceSq = dx * dx + (dy = (double)(pos.m_123342_() - center.m_123342_())) * dy + (dz = (double)(pos.m_123343_() - center.m_123343_())) * dz;
        if (candidates.canRejectBlock(distanceSq)) {
            return;
        }
        exposedFaceService.forEachCompleteReceiverSurface(level, blockKey, (face, planeCoordinate, minU, maxU, minV, maxV, surfaceKey) -> {
            double worldPlaneCoordinate = switch (face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> (double)pos.m_123342_() + planeCoordinate;
                case Direction.NORTH, Direction.SOUTH -> (double)pos.m_123343_() + planeCoordinate;
                case Direction.WEST, Direction.EAST -> (double)pos.m_123341_() + planeCoordinate;
            };
            candidates.offer(pos, face, distanceSq, worldPlaneCoordinate, minU, maxU, minV, maxV, surfaceKey);
        });
    }

    void appendReceiverSurfaceCandidates(List<NeoShadowsTypes.FaceCandidate> candidates, ExposedFaceService exposedFaceService, ClientLevel level, long blockKey, BlockPos center) {
        BlockPos pos = BlockPos.m_122022_((long)blockKey);
        double dx = pos.m_123341_() - center.m_123341_();
        double dy = pos.m_123342_() - center.m_123342_();
        double dz = pos.m_123343_() - center.m_123343_();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        exposedFaceService.forEachCompleteReceiverSurface(level, blockKey, (face, planeCoordinate, minU, maxU, minV, maxV, surfaceKey) -> {
            double worldPlaneCoordinate = switch (face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> (double)pos.m_123342_() + planeCoordinate;
                case Direction.NORTH, Direction.SOUTH -> (double)pos.m_123343_() + planeCoordinate;
                case Direction.WEST, Direction.EAST -> (double)pos.m_123341_() + planeCoordinate;
            };
            candidates.add(new NeoShadowsTypes.FaceCandidate(pos, face, distanceSq, worldPlaneCoordinate, new NeoShadowsTypes.PlaneRect(minU, maxU, minV, maxV), surfaceKey));
        });
    }

    void appendLocalSectionFallbackCandidates(List<NeoShadowsTypes.FaceCandidate> candidates, ClientLevel level, ExposedFaceService exposedFaceService, long sectionKey, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockPos center) {
        int sectionBaseX = SectionPos.m_123213_((long)sectionKey) << 4;
        int sectionBaseY = SectionPos.m_123225_((long)sectionKey) << 4;
        int sectionBaseZ = SectionPos.m_123230_((long)sectionKey) << 4;
        if (exposedFaceService.isSectionFullyCaptured(sectionKey) && exposedFaceService.hasSectionFaceSnapshot(sectionKey)) {
            this.appendLocalSectionSnapshotCandidates(candidates, level, exposedFaceService, sectionKey, sectionBaseX, sectionBaseY, sectionBaseZ, minX, maxX, minY, maxY, minZ, maxZ, center);
            return;
        }
        int sectionMinX = Math.max(minX, sectionBaseX);
        int sectionMaxX = Math.min(maxX, sectionBaseX + 15);
        int sectionMinY = Math.max(minY, sectionBaseY);
        int sectionMaxY = Math.min(maxY, sectionBaseY + 15);
        int sectionMinZ = Math.max(minZ, sectionBaseZ);
        int sectionMaxZ = Math.min(maxZ, sectionBaseZ + 15);
        for (int y = sectionMinY; y <= sectionMaxY; ++y) {
            for (int z = sectionMinZ; z <= sectionMaxZ; ++z) {
                for (int x = sectionMinX; x <= sectionMaxX; ++x) {
                    long blockKey = BlockPos.m_121882_((int)x, (int)y, (int)z);
                    this.appendReceiverSurfaceCandidates(candidates, exposedFaceService, level, blockKey, center);
                }
            }
        }
    }

    void appendLocalSectionSnapshotCandidates(List<NeoShadowsTypes.FaceCandidate> candidates, ClientLevel level, ExposedFaceService exposedFaceService, long sectionKey, int sectionBaseX, int sectionBaseY, int sectionBaseZ, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockPos center) {
        ExposedFaceService.SectionFaceSnapshot snapshot = exposedFaceService.snapshotSectionFaces(level, sectionKey);
        if (!snapshot.hasCandidateReceivers()) {
            return;
        }
        int sectionMinX = Math.max(minX, sectionBaseX);
        int sectionMaxX = Math.min(maxX, sectionBaseX + 15);
        int sectionMinY = Math.max(minY, sectionBaseY);
        int sectionMaxY = Math.min(maxY, sectionBaseY + 15);
        int sectionMinZ = Math.max(minZ, sectionBaseZ);
        int sectionMaxZ = Math.min(maxZ, sectionBaseZ + 15);
        if (sectionMinX > sectionMaxX || sectionMinY > sectionMaxY || sectionMinZ > sectionMaxZ) {
            return;
        }
        for (int wordIndex = 0; wordIndex < snapshot.wordCount(); ++wordIndex) {
            for (long remainingBits = snapshot.word(wordIndex); remainingBits != 0L; remainingBits &= remainingBits - 1L) {
                int localX;
                int worldX;
                int localZ;
                int worldZ;
                int bitIndex = Long.numberOfTrailingZeros(remainingBits);
                int localIndex = (wordIndex << 6) + bitIndex;
                int localY = localIndex >>> 8;
                int worldY = sectionBaseY + localY;
                if (worldY < sectionMinY || worldY > sectionMaxY || (worldZ = sectionBaseZ + (localZ = localIndex >>> 4 & 0xF)) < sectionMinZ || worldZ > sectionMaxZ || (worldX = sectionBaseX + (localX = localIndex & 0xF)) < sectionMinX || worldX > sectionMaxX) continue;
                this.appendReceiverSurfaceCandidates(candidates, exposedFaceService, level, BlockPos.m_121882_((int)worldX, (int)worldY, (int)worldZ), center);
            }
        }
    }

    List<NeoShadowsTypes.FaceCandidate> buildLocalFaceCandidates(ClientLevel level, BlockPos center, int localRadius, ExposedFaceService exposedFaceService) {
        ArrayList<NeoShadowsTypes.FaceCandidate> candidates = new ArrayList<NeoShadowsTypes.FaceCandidate>();
        int minX = center.m_123341_() - localRadius;
        int maxX = center.m_123341_() + localRadius;
        int minY = center.m_123342_() - localRadius;
        int maxY = center.m_123342_() + localRadius;
        int minZ = center.m_123343_() - localRadius;
        int maxZ = center.m_123343_() + localRadius;
        int minSectionX = SectionPos.m_123171_((int)minX);
        int maxSectionX = SectionPos.m_123171_((int)maxX);
        int minSectionY = SectionPos.m_123171_((int)minY);
        int maxSectionY = SectionPos.m_123171_((int)maxY);
        int minSectionZ = SectionPos.m_123171_((int)minZ);
        int maxSectionZ = SectionPos.m_123171_((int)maxZ);
        for (int sectionY = minSectionY; sectionY <= maxSectionY; ++sectionY) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; ++sectionZ) {
                for (int sectionX = minSectionX; sectionX <= maxSectionX; ++sectionX) {
                    long sectionKey = SectionPos.m_123209_((int)sectionX, (int)sectionY, (int)sectionZ);
                    this.appendLocalSectionFallbackCandidates(candidates, level, exposedFaceService, sectionKey, minX, maxX, minY, maxY, minZ, maxZ, center);
                }
            }
        }
        return candidates;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    NeoShadowsTypes.FaceLightingInfo resolveFaceLightingCached(TorchRtxState torchState, @Nullable NeoShadowsTypes.GeometryBuildState state, NeoShadowsTypes.FaceCandidate candidate) {
        Long2ObjectOpenHashMap faceCache;
        if (state == null) {
            return this.resolveFaceLighting(torchState, candidate, null);
        }
        EnumMap<Direction, Long2ObjectOpenHashMap<NeoShadowsTypes.FaceLightingInfo>> enumMap = state.faceLightingCache;
        synchronized (enumMap) {
            faceCache = state.faceLightingCache.get(candidate.face);
            if (faceCache == null) {
                faceCache = new Long2ObjectOpenHashMap();
                state.faceLightingCache.put(candidate.face, (Long2ObjectOpenHashMap<NeoShadowsTypes.FaceLightingInfo>)faceCache);
            }
        }
        long receiverKey = candidate.receiverKey;
        Long2ObjectOpenHashMap long2ObjectOpenHashMap = faceCache;
        synchronized (long2ObjectOpenHashMap) {
            NeoShadowsTypes.FaceLightingInfo cached = (NeoShadowsTypes.FaceLightingInfo)faceCache.get(receiverKey);
            if (cached != null) {
                return cached;
            }
        }
        NeoShadowsTypes.FaceLightingInfo resolved = this.resolveFaceLighting(torchState, candidate, state);
        Long2ObjectOpenHashMap long2ObjectOpenHashMap2 = faceCache;
        synchronized (long2ObjectOpenHashMap2) {
            NeoShadowsTypes.FaceLightingInfo existing = (NeoShadowsTypes.FaceLightingInfo)faceCache.get(receiverKey);
            if (existing != null) {
                return existing;
            }
            faceCache.put(receiverKey, (Object)resolved);
        }
        return resolved;
    }

    NeoShadowsTypes.FaceLightingInfo resolveFaceLighting(TorchRtxState torchState, NeoShadowsTypes.FaceCandidate candidate, @Nullable NeoShadowsTypes.GeometryBuildState state) {
        long sourceKey;
        int index;
        double maxLightReach;
        double sampleZ;
        double sampleY;
        BlockPos pos = candidate.pos;
        Direction face = candidate.face;
        NeoShadowsTypes.PlaneRect receiverBounds = candidate.localBounds;
        NeoShadowsTypes.FaceLightingScratch lightingScratch = FACE_LIGHTING_SCRATCH_TL.get();
        Long2ObjectOpenHashMap<NeoShadowsTypes.SourceScore> scoresBySource = lightingScratch.scores;
        scoresBySource.clear();
        long ownPosKey = pos.m_121878_();
        NeoShadowsTypes.UvPoint[] probes = NeoShadowsVisibility.buildLightingFaceProbes(face);
        NeoShadowsTypes.PlaneRect[] probeRects = NeoShadowsVisibility.buildLightingProbeRects(face);
        NeoShadowsTypes.ContributorScoringVisitor visitor = lightingScratch.visitor;
        visitor.bind(scoresBySource, ownPosKey);
        for (int probeIndex = 0; probeIndex < probes.length; ++probeIndex) {
            double sampleZ2;
            double sampleY2;
            double sampleX;
            long lightSamplePosKey;
            NeoShadowsTypes.UvPoint probe = probes[probeIndex];
            NeoShadowsTypes.PlaneRect probeRect = probeRects[probeIndex];
            if (!NeoShadowsProjection.isProbeInsideRect(probe, receiverBounds) || (lightSamplePosKey = this.resolveLightSamplePosKey(torchState, pos, face, sampleX = NeoShadowsProjection.facePointX(pos, face, candidate.planeCoordinate, (float)probe.u), sampleY2 = NeoShadowsProjection.facePointY(pos, face, candidate.planeCoordinate, (float)probe.v), sampleZ2 = NeoShadowsProjection.facePointZ(pos, face, candidate.planeCoordinate, (float)probe.u, (float)probe.v), state)) == Long.MIN_VALUE) continue;
            visitor.setProbe(sampleX, sampleY2, sampleZ2, lightSamplePosKey, probeRect);
            this.forEachContributorCached(torchState, lightSamplePosKey, state, visitor);
        }
        NeoShadowsTypes.SourceScore bestScore = null;
        double totalScore = 0.0;
        for (NeoShadowsTypes.SourceScore score : scoresBySource.values()) {
            totalScore += score.totalScore;
            if (!(bestScore == null || score.totalScore > bestScore.totalScore || score.totalScore == bestScore.totalScore && score.sampleCount > bestScore.sampleCount) && (score.totalScore != bestScore.totalScore || score.sampleCount != bestScore.sampleCount || !(score.bestDistanceSq < bestScore.bestDistanceSq))) continue;
            bestScore = score;
        }
        if (bestScore != null) {
            List<NeoShadowsTypes.SourceContribution> contributions;
            if (scoresBySource.size() == 1 || totalScore <= 0.0) {
                contributions = Collections.singletonList(new NeoShadowsTypes.SourceContribution(bestScore.sourceKey, bestScore.representativeSamplePosKey, bestScore.bestEmission, 1.0f, receiverBounds));
            } else {
                contributions = new ArrayList(scoresBySource.size());
                double keptTotal = 0.0;
                for (NeoShadowsTypes.SourceScore score : scoresBySource.values()) {
                    double weight = score.totalScore / totalScore;
                    if (weight < 0.03) continue;
                    keptTotal += weight;
                }
                if (keptTotal <= 0.0) {
                    contributions = Collections.singletonList(new NeoShadowsTypes.SourceContribution(bestScore.sourceKey, bestScore.representativeSamplePosKey, bestScore.bestEmission, 1.0f, receiverBounds));
                } else {
                    double invKeptTotal = 1.0 / keptTotal;
                    for (NeoShadowsTypes.SourceScore score : scoresBySource.values()) {
                        double weight = score.totalScore / totalScore;
                        if (weight < 0.03) continue;
                        contributions.add(new NeoShadowsTypes.SourceContribution(score.sourceKey, score.representativeSamplePosKey, score.bestEmission, (float)(weight * invKeptTotal), NeoShadowsVisibility.intersectPlaneRects(receiverBounds, score.contributionBounds())));
                    }
                }
            }
            return new NeoShadowsTypes.FaceLightingInfo(bestScore.representativeSamplePosKey, bestScore.sourceKey, bestScore.bestEmission, contributions);
        }
        float centerU = (float)((receiverBounds.minU + receiverBounds.maxU) * 0.5);
        float centerV = (float)((receiverBounds.minV + receiverBounds.maxV) * 0.5);
        double sampleX = NeoShadowsProjection.facePointX(pos, face, candidate.planeCoordinate, centerU);
        long[] closestSources = torchState.getClosestSources(new Vec3(sampleX, sampleY = NeoShadowsProjection.facePointY(pos, face, candidate.planeCoordinate, centerV), sampleZ = NeoShadowsProjection.facePointZ(pos, face, candidate.planeCoordinate, centerU, centerV)), (maxLightReach = Math.min(LightRtMath.getMaxDistance(15), 30.0)) * maxLightReach, 4);
        if (closestSources.length == 0) {
            return NeoShadowsTypes.FaceLightingInfo.empty();
        }
        long fakeSampleKey = candidate.receiverKey;
        ArrayList<NeoShadowsTypes.SourceContribution> fallbackContribs = new ArrayList<NeoShadowsTypes.SourceContribution>(closestSources.length);
        double[] fallbackScores = new double[closestSources.length];
        double totalFallbackScore = 0.0;
        long bestFallbackSourceKey = Long.MIN_VALUE;
        int bestFallbackEmission = 0;
        double bestFallbackScore = Double.NEGATIVE_INFINITY;
        for (index = 0; index < closestSources.length; ++index) {
            double score;
            sourceKey = closestSources[index];
            int sourceEmission = torchState.getSourceEmission(sourceKey);
            if (sourceEmission <= 0) continue;
            double dx = (double)BlockPos.m_121983_((long)sourceKey) + 0.5 - sampleX;
            double dy = (double)BlockPos.m_122008_((long)sourceKey) + 0.5 - sampleY;
            double dz = (double)BlockPos.m_122015_((long)sourceKey) + 0.5 - sampleZ;
            fallbackScores[index] = score = 1.0 / Math.max(0.25, dx * dx + dy * dy + dz * dz);
            totalFallbackScore += score;
            if (!(score > bestFallbackScore)) continue;
            bestFallbackScore = score;
            bestFallbackSourceKey = sourceKey;
            bestFallbackEmission = sourceEmission;
        }
        if (totalFallbackScore <= 0.0 || bestFallbackSourceKey == Long.MIN_VALUE) {
            return NeoShadowsTypes.FaceLightingInfo.empty();
        }
        for (index = 0; index < closestSources.length; ++index) {
            float weight;
            int sourceEmission;
            sourceKey = closestSources[index];
            double score = fallbackScores[index];
            if (score <= 0.0 || (sourceEmission = torchState.getSourceEmission(sourceKey)) <= 0 || (weight = (float)(score / totalFallbackScore)) < 0.03f) continue;
            fallbackContribs.add(new NeoShadowsTypes.SourceContribution(sourceKey, fakeSampleKey, sourceEmission, weight, receiverBounds));
        }
        if (fallbackContribs.isEmpty()) {
            return NeoShadowsTypes.FaceLightingInfo.empty();
        }
        return new NeoShadowsTypes.FaceLightingInfo(fakeSampleKey, bestFallbackSourceKey, bestFallbackEmission, fallbackContribs);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    long resolveLightSamplePosKey(TorchRtxState torchState, BlockPos pos, Direction face, double sampleX, double sampleY, double sampleZ, @Nullable NeoShadowsTypes.GeometryBuildState state) {
        long basePosKey = NeoShadowsProjection.sampleRtxPosKey(pos, face, sampleX, sampleY, sampleZ);
        Long2LongOpenHashMap cache = null;
        if (state != null) {
            int ord = face.ordinal();
            cache = state.litSampleCache[ord];
            if (cache == null) {
                Long2LongOpenHashMap[] long2LongOpenHashMapArray = state.litSampleCache;
                // MONITORENTER : state.litSampleCache
                cache = state.litSampleCache[ord];
                if (cache == null) {
                    cache = new Long2LongOpenHashMap();
                    cache.defaultReturnValue(Long.MAX_VALUE);
                    state.litSampleCache[ord] = cache;
                }
                // MONITOREXIT : long2LongOpenHashMapArray
            }
            Long2LongOpenHashMap long2LongOpenHashMap = cache;
            // MONITORENTER : long2LongOpenHashMap
            long cached = cache.get(basePosKey);
            // MONITOREXIT : long2LongOpenHashMap
            if (cached != Long.MAX_VALUE) {
                return cached;
            }
        }
        Direction axisU = NeoShadowsProjection.firstFaceTangent(face);
        Direction axisV = NeoShadowsProjection.secondFaceTangent(face);
        int ux = axisU.m_122429_();
        int uy = axisU.m_122430_();
        int uz = axisU.m_122431_();
        int vx = axisV.m_122429_();
        int vy = axisV.m_122430_();
        int vz = axisV.m_122431_();
        int searchRadius = NeoShadowsProjection.isHorizontalReceiverFace(face) ? 2 : 1;
        long result = torchState.findNearestLitSample(basePosKey, ux, uy, uz, vx, vy, vz, searchRadius, sampleX, sampleY, sampleZ);
        if (cache == null) return result;
        Long2LongOpenHashMap long2LongOpenHashMap = cache;
        // MONITORENTER : long2LongOpenHashMap
        if (cache.get(basePosKey) == Long.MAX_VALUE) {
            cache.put(basePosKey, result);
        }
        // MONITOREXIT : long2LongOpenHashMap
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void forEachContributorCached(TorchRtxState torchState, long posKey, @Nullable NeoShadowsTypes.GeometryBuildState state, NeoFloodEngine.ContributorVisitor visitor) {
        int n;
        NeoShadowsTypes.ContributorList list;
        if (state == null) {
            torchState.forEachContributor(posKey, visitor);
            return;
        }
        Long2ObjectOpenHashMap<NeoShadowsTypes.ContributorList> long2ObjectOpenHashMap = state.contributorCache;
        synchronized (long2ObjectOpenHashMap) {
            list = (NeoShadowsTypes.ContributorList)state.contributorCache.get(posKey);
        }
        if (list == null) {
            NeoShadowsTypes.ContributorCollector collector = CONTRIBUTOR_COLLECTOR_TL.get();
            collector.reset();
            torchState.forEachContributor(posKey, collector);
            list = collector.toList();
            Long2ObjectOpenHashMap<NeoShadowsTypes.ContributorList> long2ObjectOpenHashMap2 = state.contributorCache;
            synchronized (long2ObjectOpenHashMap2) {
                NeoShadowsTypes.ContributorList existing = (NeoShadowsTypes.ContributorList)state.contributorCache.get(posKey);
                if (existing != null) {
                    list = existing;
                } else {
                    state.contributorCache.put(posKey, (Object)list);
                }
            }
        }
        if ((n = list.count) == 0) {
            return;
        }
        long[] keys = list.sourceKeys;
        int[] pack = list.packed;
        for (int index = 0; index < n; ++index) {
            int p = pack[index];
            int light = p & 0xFF;
            int coverage = p >>> 8 & 0xFF;
            int emission = p >>> 16 & 0xFF;
            visitor.visit(keys[index], light, coverage, emission);
        }
    }

    static long hashFrontierPositions(long[] frontierPositions) {
        if (frontierPositions.length == 0) {
            return 0L;
        }
        long sum = -7046029254386353131L;
        long xor = -4417276706812531889L;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long frontierKey : frontierPositions) {
            long mixed = NeoShadowsVisibility.mixFrontierKey(frontierKey);
            sum += mixed;
            xor ^= mixed;
            if (frontierKey < min) {
                min = frontierKey;
            }
            if (frontierKey <= max) continue;
            max = frontierKey;
        }
        return NeoShadowsVisibility.mixFrontierKey(sum) ^ Long.rotateLeft(NeoShadowsVisibility.mixFrontierKey(xor), 17) ^ NeoShadowsVisibility.mixFrontierKey(min) ^ Long.rotateLeft(NeoShadowsVisibility.mixFrontierKey(max), 33) ^ (long)frontierPositions.length;
    }

    static long mixFrontierKey(long value) {
        value ^= value >>> 33;
        value *= -49064778989728563L;
        value ^= value >>> 33;
        value *= -4265267296055464877L;
        value ^= value >>> 33;
        return value;
    }

    static int localSectionBlockIndex(int localX, int localY, int localZ) {
        return localY << 8 | localZ << 4 | localX;
    }

    static NeoShadowsTypes.UvPoint[] buildLightingFaceProbes(Direction face) {
        return NeoShadowsProjection.isHorizontalReceiverFace(face) ? NeoShadowsEngine.HORIZONTAL_LIGHTING_FACE_PROBES : NeoShadowsEngine.LIGHTING_FACE_PROBES;
    }

    static NeoShadowsTypes.PlaneRect[] buildLightingProbeRects(Direction face) {
        return NeoShadowsProjection.isHorizontalReceiverFace(face) ? NeoShadowsEngine.HORIZONTAL_LIGHTING_FACE_PROBE_RECTS : NeoShadowsEngine.LIGHTING_FACE_PROBE_RECTS;
    }

    static NeoShadowsTypes.PlaneRect intersectPlaneRects(NeoShadowsTypes.PlaneRect first, NeoShadowsTypes.PlaneRect second) {
        double minU = Math.max(first.minU, second.minU);
        double maxU = Math.min(first.maxU, second.maxU);
        double minV = Math.max(first.minV, second.minV);
        double maxV = Math.min(first.maxV, second.maxV);
        if (maxU - minU <= 1.0E-6 || maxV - minV <= 1.0E-6) {
            return first;
        }
        return new NeoShadowsTypes.PlaneRect(minU, maxU, minV, maxV);
    }

    private static final class NearestFaceCandidateCollector {
        private final NeoShadowsTypes.FaceCandidate[] heap;
        private int size;

        NearestFaceCandidateCollector(int limit) {
            this.heap = new NeoShadowsTypes.FaceCandidate[Math.max(1, limit)];
        }

        boolean canRejectBlock(double distanceSq) {
            return this.size >= this.heap.length && distanceSq > this.heap[0].distanceSq;
        }

        void offer(BlockPos pos, Direction face, double distanceSq, double planeCoordinate, double minU, double maxU, double minV, double maxV, long surfaceKey) {
            NeoShadowsTypes.FaceCandidate candidate = new NeoShadowsTypes.FaceCandidate(pos, face, distanceSq, planeCoordinate, new NeoShadowsTypes.PlaneRect(minU, maxU, minV, maxV), surfaceKey);
            if (this.size < this.heap.length) {
                this.heap[this.size] = candidate;
                this.siftUp(this.size);
                ++this.size;
                return;
            }
            if (distanceSq >= this.heap[0].distanceSq) {
                return;
            }
            this.heap[0] = candidate;
            this.siftDown(0);
        }

        NeoShadowsTypes.FaceCandidate[] toSortedArray() {
            NeoShadowsTypes.FaceCandidate[] result = Arrays.copyOf(this.heap, this.size);
            Arrays.sort(result, Comparator.comparingDouble(value -> value.distanceSq));
            return result;
        }

        private void siftUp(int index) {
            while (index > 0) {
                int parentIndex = index - 1 >>> 1;
                if (this.heap[parentIndex].distanceSq >= this.heap[index].distanceSq) {
                    return;
                }
                NeoShadowsTypes.FaceCandidate temp = this.heap[parentIndex];
                this.heap[parentIndex] = this.heap[index];
                this.heap[index] = temp;
                index = parentIndex;
            }
        }

        private void siftDown(int index) {
            int leftIndex;
            while ((leftIndex = (index << 1) + 1) < this.size) {
                int rightIndex = leftIndex + 1;
                int largestIndex = leftIndex;
                if (rightIndex < this.size && this.heap[rightIndex].distanceSq > this.heap[leftIndex].distanceSq) {
                    largestIndex = rightIndex;
                }
                if (this.heap[index].distanceSq >= this.heap[largestIndex].distanceSq) {
                    return;
                }
                NeoShadowsTypes.FaceCandidate temp = this.heap[index];
                this.heap[index] = this.heap[largestIndex];
                this.heap[largestIndex] = temp;
                index = largestIndex;
            }
            return;
        }
    }

    private static final class TouchedSectionData {
        private final short[] sparseIndices = new short[24];
        private int sparseCount;
        @Nullable
        private DenseTouchedBits denseBits;

        private TouchedSectionData() {
        }

        void reset() {
            this.sparseCount = 0;
            this.denseBits = null;
        }

        void add(int localIndex, TouchedSectionScratch scratch) {
            if (this.denseBits != null) {
                this.denseBits.set(localIndex);
                return;
            }
            short packed = (short)localIndex;
            for (int index = 0; index < this.sparseCount; ++index) {
                if (this.sparseIndices[index] != packed) continue;
                return;
            }
            if (this.sparseCount < this.sparseIndices.length) {
                this.sparseIndices[this.sparseCount++] = packed;
                return;
            }
            DenseTouchedBits promoted = scratch.rentDenseBits();
            for (int index = 0; index < this.sparseCount; ++index) {
                promoted.set(this.sparseIndices[index] & 0xFFFF);
            }
            promoted.set(localIndex);
            this.denseBits = promoted;
            this.sparseCount = 0;
        }

        boolean isDense() {
            return this.denseBits != null;
        }

        DenseTouchedBits denseBits() {
            return this.denseBits;
        }

        int sparseCount() {
            return this.sparseCount;
        }

        int sparseIndexAt(int index) {
            return this.sparseIndices[index] & 0xFFFF;
        }
    }

    private static final class TouchedSectionScratch {
        final Long2ObjectOpenHashMap<TouchedSectionData> touchedSections = new Long2ObjectOpenHashMap();
        final List<TouchedSectionData> sectionPool = new ArrayList<TouchedSectionData>();
        final List<DenseTouchedBits> denseBitsetPool = new ArrayList<DenseTouchedBits>();
        int usedSections;
        int usedDenseBitsets;

        private TouchedSectionScratch() {
        }

        TouchedSectionData rentTouchedSection() {
            TouchedSectionData touchedSection;
            if (this.usedSections < this.sectionPool.size()) {
                touchedSection = this.sectionPool.get(this.usedSections);
                touchedSection.reset();
            } else {
                touchedSection = new TouchedSectionData();
                this.sectionPool.add(touchedSection);
            }
            ++this.usedSections;
            return touchedSection;
        }

        DenseTouchedBits rentDenseBits() {
            DenseTouchedBits denseBits;
            if (this.usedDenseBitsets < this.denseBitsetPool.size()) {
                denseBits = this.denseBitsetPool.get(this.usedDenseBitsets);
                denseBits.reset();
            } else {
                denseBits = new DenseTouchedBits();
                this.denseBitsetPool.add(denseBits);
            }
            ++this.usedDenseBitsets;
            return denseBits;
        }

        void reset() {
            this.touchedSections.clear();
            this.usedSections = 0;
            this.usedDenseBitsets = 0;
        }
    }

    private static final class DenseTouchedBits {
        private final long[] words = new long[64];
        private final int[] dirtyWordIndices = new int[64];
        private int dirtyWordCount;

        private DenseTouchedBits() {
        }

        void reset() {
            for (int index = 0; index < this.dirtyWordCount; ++index) {
                this.words[this.dirtyWordIndices[index]] = 0L;
            }
            this.dirtyWordCount = 0;
        }

        void set(int localIndex) {
            int wordIndex = localIndex >>> 6;
            long existing = this.words[wordIndex];
            long mask = 1L << (localIndex & 0x3F);
            if ((existing & mask) != 0L) {
                return;
            }
            if (existing == 0L) {
                this.dirtyWordIndices[this.dirtyWordCount++] = wordIndex;
            }
            this.words[wordIndex] = existing | mask;
        }
    }
}
