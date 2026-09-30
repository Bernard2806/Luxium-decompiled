/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuEntityShadows;
import com.vinlanx.luxium.client.neocpu.NeoCpuFloodBoundaryFace;
import com.vinlanx.luxium.client.neocpu.NeoCpuFloodLightVolume;
import com.vinlanx.luxium.client.neocpu.NeoCpuLocalBlockAccessor;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShape;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShapeCache;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import com.vinlanx.luxium.rtx.TorchRtxState;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class NeoCpuFloodShadowBuilder {
    private static final int MAX_POLYGONS = 8192;
    private static final int MAX_HITS_PER_RECEIVER = 9;
    private static final int MAX_HITS_PER_PLANE = 1024;
    private static final int RECEIVER_CONTRIBUTION_KEY_COUNT = 9;
    private static final double SHADOW_EPSILON = 1.0E-6;
    private static final double FACE_EPSILON = 0.0015;
    private static final double SURFACE_OFFSET = 0.0015;
    private static final double DDA_TIE_EPSILON = 1.0E-9;
    private static final double MIN_SOURCE_HIT_DISTANCE = 0.035;
    private static final double RECEIVER_HIT_EPSILON = 1.0E-4;
    private static final double SELF_SURFACE_EPSILON = 1.0E-4;
    private static final double MIN_RECEIVER_FACING_DOT = 1.0E-4;
    private static final double ADAPTIVE_GRAZING_ALIGNMENT_THRESHOLD = 0.26;
    private static final double ADAPTIVE_LARGE_RECEIVER_SIZE = 0.72;
    private static final double ADAPTIVE_NEAR_SOURCE_FRACTION = 0.18;
    private static final double ADAPTIVE_NEAR_RECEIVER_FRACTION = 0.82;
    private static final double EDGE_PROJECT_EPSILON = 1.0E-6;
    private static final double PROJECTION_DENOMINATOR_EPSILON = 1.0E-9;
    private static final double EXTENDED_PROJECTION_DELTA = 0.001;
    private static final double EXTENDED_PROJECTION_FAR = 8.0;
    private static final double POLYGON_AREA_EPSILON = 1.0E-4;
    private static final double FULL_RECT_SHADOW_AREA_THRESHOLD = 0.988;
    private static final double SHADOW_POLYGON_DILATION = 0.016;
    private static final double HORIZONTAL_SHADOW_POLYGON_DILATION = 0.024;
    private static final double SHADOW_EDGE_SNAP_EPSILON = 0.022;
    private static final double HORIZONTAL_SHADOW_EDGE_SNAP_EPSILON = 0.03;
    private static final int[][] BOX_EDGE_VERTEX_INDICES = new int[][]{{0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}};
    private static final double[] CROSS_PROBE_LOCAL_U = new double[]{0.18, 0.5, 0.82, 0.5};
    private static final double[] CROSS_PROBE_LOCAL_V = new double[]{0.5, 0.18, 0.5, 0.82};
    private static final double[] CORNER_PROBE_LOCAL_U = new double[]{0.18, 0.82, 0.18, 0.82};
    private static final double[] CORNER_PROBE_LOCAL_V = new double[]{0.18, 0.18, 0.82, 0.82};
    private final ClientLevel level;
    private final NeoCpuShadowShapeCache shapeCache = new NeoCpuShadowShapeCache();
    private final ArrayList<NeoCpuShadowTypes.Uv> projected = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> hull = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> polygonBuf = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> clipA = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> clipB = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> sortBuf = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> uniqueBuf = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> lowerBuf = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> upperBuf = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> localPolygon = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> dilatedPolygon = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> snappedPolygon = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> normalizedPolygon = new ArrayList(32);
    private final ArrayList<NeoCpuShadowTypes.Uv> rectPolygon = new ArrayList(4);
    private final double[][] boxCornerScratch = new double[8][3];
    private final CpuProjection[] boxCornerProjScratch = new CpuProjection[8];
    private final CpuProjection[] quadCornerProjScratch = new CpuProjection[4];
    private final Long2ObjectOpenHashMap<NeoCpuShadowTypes.Occluder[]> occluderCache = new Long2ObjectOpenHashMap();
    private final Long2ObjectOpenHashMap<PlaneGroup> planeGroupsByKey = new Long2ObjectOpenHashMap();
    private final ArrayList<PlaneGroup> planeGroups = new ArrayList(64);
    private final Long2ObjectOpenHashMap<Long2ByteOpenHashMap> sourceContributionCacheBySource = new Long2ObjectOpenHashMap();
    private final Long2ObjectOpenHashMap<Long> dominantSourceCache = new Long2ObjectOpenHashMap();
    private final Long2ByteOpenHashMap sampleCoverageCache = new Long2ByteOpenHashMap();
    private final long[] receiverContributionKeys = new long[9];
    private final Long2ObjectOpenHashMap<ContributionData> contributionDataCache = new Long2ObjectOpenHashMap();
    private final ContributionCollector contributionCollector = new ContributionCollector();
    private final SourceDebugStats debugStats = new SourceDebugStats();

    public NeoCpuFloodShadowBuilder(ClientLevel level) {
        this.level = level;
        this.sampleCoverageCache.defaultReturnValue((byte)0);
    }

    public List<NeoCpuShadowTypes.ShadowPolygon> build(long[] sourceKeys, int[] sourceEmissions) {
        return this.build(sourceKeys, sourceEmissions, NeoCpuEntityShadows.CaptureState.empty());
    }

    public List<NeoCpuShadowTypes.ShadowPolygon> build(long[] sourceKeys, int[] sourceEmissions, @Nullable NeoCpuEntityShadows.CaptureState entityShadowState) {
        NeoCpuLocalBlockAccessor accessor = new NeoCpuLocalBlockAccessor(this.level, this.shapeCache);
        ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>(1024);
        for (int sourceIndex = 0; sourceIndex < sourceKeys.length && polygons.size() < 8192; ++sourceIndex) {
            long sourceKey = sourceKeys[sourceIndex];
            int emission = sourceEmissions[sourceIndex];
            if (sourceKey == Long.MIN_VALUE || emission <= 0) continue;
            this.buildSource(accessor, sourceKey, emission, entityShadowState, polygons);
        }
        return polygons.isEmpty() ? List.of() : List.copyOf(polygons);
    }

    public NeoCpuShadowTypes.StaticBuildOutput buildStatic(long[] sourceKeys, int[] sourceEmissions) {
        NeoCpuLocalBlockAccessor accessor = new NeoCpuLocalBlockAccessor(this.level, this.shapeCache);
        ArrayList<NeoCpuShadowTypes.PerSourceStatic> sources = new ArrayList<NeoCpuShadowTypes.PerSourceStatic>(sourceKeys.length);
        ArrayList<NeoCpuShadowTypes.ShadowPolygon> allBlockPolygons = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>(1024);
        for (int sourceIndex = 0; sourceIndex < sourceKeys.length; ++sourceIndex) {
            long sourceKey = sourceKeys[sourceIndex];
            int emission = sourceEmissions[sourceIndex];
            if (sourceKey == Long.MIN_VALUE || emission <= 0) continue;
            NeoCpuShadowTypes.PerSourceStatic perSource = this.buildStaticSource(accessor, sourceKey, emission, allBlockPolygons);
            if (perSource != null) {
                sources.add(perSource);
            }
            if (allBlockPolygons.size() >= 8192) break;
        }
        return sources.isEmpty() ? NeoCpuShadowTypes.StaticBuildOutput.EMPTY : new NeoCpuShadowTypes.StaticBuildOutput(List.copyOf(sources));
    }

    public List<NeoCpuShadowTypes.ShadowPolygon> buildDynamicOverlay(NeoCpuShadowTypes.StaticBuildOutput staticOutput, NeoCpuEntityShadows.CaptureState entityState) {
        if (staticOutput.isEmpty() || entityState == null || entityState.isEmpty()) {
            return List.of();
        }
        ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>(256);
        for (NeoCpuShadowTypes.PerSourceStatic perSource : staticOutput.sources()) {
            if (polygons.size() >= 8192) break;
            if (perSource.groups().isEmpty()) continue;
            NeoCpuEntityShadows.appendEntityShadows(this, perSource.source(), perSource.emission(), entityState, perSource.groups(), polygons);
        }
        return polygons.isEmpty() ? List.of() : List.copyOf(polygons);
    }

    @Nullable
    private NeoCpuShadowTypes.PerSourceStatic buildStaticSource(NeoCpuLocalBlockAccessor accessor, long sourceKey, int emission, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        this.occluderCache.clear();
        double sourceX = (double)BlockPos.m_121983_((long)sourceKey) + 0.5;
        double sourceY = (double)BlockPos.m_122008_((long)sourceKey) + 0.5;
        double sourceZ = (double)BlockPos.m_122015_((long)sourceKey) + 0.5;
        Vec3 source = new Vec3(sourceX, sourceY, sourceZ);
        double radius = LightRtMath.getBlockShadowRadius(emission);
        TorchRtxState torchState = TorchRtxState.get();
        NeoCpuFloodLightVolume volume = NeoCpuFloodLightVolume.build(accessor, sourceKey, emission);
        if (volume.boundaryFaces().isEmpty()) {
            return null;
        }
        this.debugStats.reset(sourceKey, emission, volume.boundaryFaces().size());
        this.sourceContributionCacheBySource.clear();
        this.dominantSourceCache.clear();
        this.sampleCoverageCache.clear();
        this.contributionDataCache.clear();
        this.planeGroupsByKey.clear();
        this.planeGroups.clear();
        for (NeoCpuFloodBoundaryFace boundaryFace : volume.boundaryFaces()) {
            NeoCpuShadowTypes.ReceiverSurface receiver = boundaryFace.receiver();
            ContributionData contribution = this.resolveReceiverContribution(torchState, receiver, sourceKey);
            if (contribution == null || !contribution.accepted()) {
                ++this.debugStats.rejectedReceivers;
                continue;
            }
            ++this.debugStats.acceptedReceivers;
            float strength = this.computeReceiverStrength(sourceX, sourceY, sourceZ, emission, receiver, contribution.weightScale());
            if (strength <= 0.0f) continue;
            this.addReceiverToPlaneGroup(receiver, strength);
        }
        ArrayList<NeoCpuShadowTypes.ReceiverGroupEntry> groups = new ArrayList<NeoCpuShadowTypes.ReceiverGroupEntry>(this.planeGroups.size());
        int polygonsBefore = polygons.size();
        for (PlaneGroup group : this.planeGroups) {
            if (polygons.size() >= 8192) {
                this.debugStats.hitPolygonCap = true;
                break;
            }
            ArrayList<NeoCpuShadowTypes.ReceiverWithStrength> rwsList = new ArrayList<NeoCpuShadowTypes.ReceiverWithStrength>(group.receivers.size());
            for (ReceiverEntry entry : group.receivers) {
                rwsList.add(new NeoCpuShadowTypes.ReceiverWithStrength(entry.receiver(), entry.strength()));
            }
            groups.add(new NeoCpuShadowTypes.ReceiverGroupEntry(List.copyOf(rwsList)));
            this.buildPlaneGroup(accessor, sourceKey, source, radius, group, polygons);
        }
        List<NeoCpuShadowTypes.ShadowPolygon> blockPolygons = polygons.size() > polygonsBefore ? List.copyOf(polygons.subList(polygonsBefore, polygons.size())) : List.of();
        return groups.isEmpty() ? null : new NeoCpuShadowTypes.PerSourceStatic(sourceKey, emission, source, List.copyOf(groups), blockPolygons);
    }

    private void buildSource(NeoCpuLocalBlockAccessor accessor, long sourceKey, int emission, @Nullable NeoCpuEntityShadows.CaptureState entityShadowState, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        this.occluderCache.clear();
        double sourceX = (double)BlockPos.m_121983_((long)sourceKey) + 0.5;
        double sourceY = (double)BlockPos.m_122008_((long)sourceKey) + 0.5;
        double sourceZ = (double)BlockPos.m_122015_((long)sourceKey) + 0.5;
        Vec3 source = new Vec3(sourceX, sourceY, sourceZ);
        double radius = LightRtMath.getBlockShadowRadius(emission);
        TorchRtxState torchState = TorchRtxState.get();
        int polygonsBefore = polygons.size();
        NeoCpuFloodLightVolume volume = NeoCpuFloodLightVolume.build(accessor, sourceKey, emission);
        if (volume.boundaryFaces().isEmpty()) {
            return;
        }
        this.debugStats.reset(sourceKey, emission, volume.boundaryFaces().size());
        this.sourceContributionCacheBySource.clear();
        this.dominantSourceCache.clear();
        this.sampleCoverageCache.clear();
        this.contributionDataCache.clear();
        this.planeGroupsByKey.clear();
        this.planeGroups.clear();
        for (NeoCpuFloodBoundaryFace boundaryFace : volume.boundaryFaces()) {
            NeoCpuShadowTypes.ReceiverSurface receiver = boundaryFace.receiver();
            ContributionData contribution = this.resolveReceiverContribution(torchState, receiver, sourceKey);
            if (contribution == null || !contribution.accepted()) {
                ++this.debugStats.rejectedReceivers;
                continue;
            }
            ++this.debugStats.acceptedReceivers;
            float strength = this.computeReceiverStrength(sourceX, sourceY, sourceZ, emission, receiver, contribution.weightScale());
            if (strength <= 0.0f) continue;
            this.addReceiverToPlaneGroup(receiver, strength);
        }
        for (PlaneGroup group : this.planeGroups) {
            if (polygons.size() >= 8192) {
                this.debugStats.hitPolygonCap = true;
                return;
            }
            this.buildPlaneGroup(accessor, sourceKey, source, radius, group, polygons);
        }
        if (entityShadowState != null && !entityShadowState.isEmpty() && !this.planeGroups.isEmpty() && polygons.size() < 8192) {
            ArrayList<NeoCpuShadowTypes.ReceiverGroupEntry> receiverGroups = new ArrayList<NeoCpuShadowTypes.ReceiverGroupEntry>(this.planeGroups.size());
            for (PlaneGroup group : this.planeGroups) {
                if (group.receivers.isEmpty()) continue;
                ArrayList<NeoCpuShadowTypes.ReceiverWithStrength> receivers = new ArrayList<NeoCpuShadowTypes.ReceiverWithStrength>(group.receivers.size());
                for (ReceiverEntry entry : group.receivers) {
                    receivers.add(new NeoCpuShadowTypes.ReceiverWithStrength(entry.receiver(), entry.strength()));
                }
                receiverGroups.add(new NeoCpuShadowTypes.ReceiverGroupEntry(List.copyOf(receivers)));
            }
            if (!receiverGroups.isEmpty()) {
                NeoCpuEntityShadows.appendEntityShadows(this, source, emission, entityShadowState, receiverGroups, polygons);
            }
        }
    }

    private float computeReceiverStrength(double sourceX, double sourceY, double sourceZ, int emission, NeoCpuShadowTypes.ReceiverSurface receiver, float weightScale) {
        double receiverDistance = Math.sqrt(receiver.distanceToSourceSq(sourceX, sourceY, sourceZ));
        int lightAtReceiver = LightRtMath.getFalloff(emission, receiverDistance);
        if (lightAtReceiver <= 0) {
            return 0.0f;
        }
        return Mth.m_14036_((float)((0.28f + 0.52f * ((float)lightAtReceiver / Math.max(1.0f, (float)emission))) * weightScale), (float)0.18f, (float)0.78f);
    }

    private void addReceiverToPlaneGroup(NeoCpuShadowTypes.ReceiverSurface receiver, float strength) {
        long key = NeoCpuFloodShadowBuilder.planeGroupKey(receiver.face(), receiver.plane());
        PlaneGroup group = (PlaneGroup)this.planeGroupsByKey.get(key);
        if (group == null) {
            group = new PlaneGroup();
            this.planeGroupsByKey.put(key, (Object)group);
            this.planeGroups.add(group);
        }
        group.receivers.add(new ReceiverEntry(receiver, strength));
    }

    private ContributionData resolveReceiverContribution(TorchRtxState torchState, NeoCpuShadowTypes.ReceiverSurface receiver, long sourceKey) {
        double sampleZ;
        double sampleY;
        if (receiver.blockKey() == sourceKey) {
            return ContributionData.REJECTED;
        }
        ContributionData cached = (ContributionData)this.contributionDataCache.get(receiver.blockKey() ^ (long)receiver.face().ordinal() << 58 ^ NeoCpuFloodShadowBuilder.planeGroupKey(receiver.face(), receiver.plane()));
        if (cached != null) {
            return cached;
        }
        double sampleX = receiver.centerX();
        long basePosKey = NeoCpuFloodShadowBuilder.sampleRtxPosKey(receiver, sampleX, sampleY = receiver.centerY(), sampleZ = receiver.centerZ());
        if (basePosKey == Long.MIN_VALUE) {
            ++this.debugStats.noSampleRejects;
            return this.cacheContribution(receiver, ContributionData.REJECTED);
        }
        int keyCount = this.fillReceiverContributionKeys(receiver, basePosKey, this.receiverContributionKeys);
        ContributionData data = this.buildContributionData(torchState, receiver, sourceKey, this.receiverContributionKeys, keyCount, sampleX, sampleY, sampleZ);
        return this.cacheContribution(receiver, data);
    }

    private ContributionData cacheContribution(NeoCpuShadowTypes.ReceiverSurface receiver, ContributionData data) {
        this.contributionDataCache.put(receiver.blockKey() ^ (long)receiver.face().ordinal() << 58 ^ NeoCpuFloodShadowBuilder.planeGroupKey(receiver.face(), receiver.plane()), (Object)data);
        return data;
    }

    private ContributionData buildContributionData(TorchRtxState torchState, NeoCpuShadowTypes.ReceiverSurface receiver, long sourceKey, long[] candidateKeys, int candidateKeyCount, double sampleX, double sampleY, double sampleZ) {
        for (int index = 0; index < candidateKeyCount; ++index) {
            boolean hasCoverage;
            long sampleKey = candidateKeys[index];
            if (sampleKey == Long.MIN_VALUE || !(hasCoverage = this.hasPositiveCoverageAt(torchState, sampleKey))) continue;
            if (this.dominantSourceAt(torchState, sampleKey) == sourceKey) {
                ++this.debugStats.dominantBaseAccepts;
                return new ContributionData(true, 1.0f);
            }
            if (!this.sourceContributesCached(torchState, sourceKey, sampleKey)) continue;
            this.contributionCollector.reset(sourceKey, receiver, sampleX, sampleY, sampleZ);
            torchState.forEachContributor(sampleKey, this.contributionCollector);
            if (this.contributionCollector.accepted()) {
                ++this.debugStats.contributorAccepts;
                return new ContributionData(true, this.contributionCollector.weightScale());
            }
            ++this.debugStats.contributorAccepts;
            return new ContributionData(true, 0.85f);
        }
        ++this.debugStats.contributorRejects;
        return ContributionData.REJECTED;
    }

    private int fillReceiverContributionKeys(NeoCpuShadowTypes.ReceiverSurface receiver, long basePosKey, long[] out) {
        out[0] = basePosKey;
        Direction axisU = NeoCpuFloodShadowBuilder.firstFaceTangent(receiver.face());
        Direction axisV = NeoCpuFloodShadowBuilder.secondFaceTangent(receiver.face());
        out[1] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, axisU.m_122429_(), axisU.m_122430_(), axisU.m_122431_());
        out[2] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, -axisU.m_122429_(), -axisU.m_122430_(), -axisU.m_122431_());
        out[3] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, axisV.m_122429_(), axisV.m_122430_(), axisV.m_122431_());
        out[4] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, -axisV.m_122429_(), -axisV.m_122430_(), -axisV.m_122431_());
        out[5] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, axisU.m_122429_() + axisV.m_122429_(), axisU.m_122430_() + axisV.m_122430_(), axisU.m_122431_() + axisV.m_122431_());
        out[6] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, axisU.m_122429_() - axisV.m_122429_(), axisU.m_122430_() - axisV.m_122430_(), axisU.m_122431_() - axisV.m_122431_());
        out[7] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, -axisU.m_122429_() + axisV.m_122429_(), -axisU.m_122430_() + axisV.m_122430_(), -axisU.m_122431_() + axisV.m_122431_());
        out[8] = NeoCpuFloodShadowBuilder.offsetBlockKey(basePosKey, -axisU.m_122429_() - axisV.m_122429_(), -axisU.m_122430_() - axisV.m_122430_(), -axisU.m_122431_() - axisV.m_122431_());
        return 9;
    }

    private static long offsetBlockKey(long basePosKey, int dx, int dy, int dz) {
        return BlockPos.m_121882_((int)(BlockPos.m_121983_((long)basePosKey) + dx), (int)(BlockPos.m_122008_((long)basePosKey) + dy), (int)(BlockPos.m_122015_((long)basePosKey) + dz));
    }

    private boolean sourceContributesCached(TorchRtxState torchState, long sourceKey, long sampleKey) {
        Long2ByteOpenHashMap cache = (Long2ByteOpenHashMap)this.sourceContributionCacheBySource.get(sourceKey);
        if (cache == null) {
            cache = new Long2ByteOpenHashMap();
            cache.defaultReturnValue((byte)0);
            this.sourceContributionCacheBySource.put(sourceKey, (Object)cache);
        } else {
            byte cached = cache.get(sampleKey);
            if (cached != 0) {
                return cached > 0;
            }
        }
        boolean contributes = torchState.sourceContributesTo(sourceKey, sampleKey);
        cache.put(sampleKey, contributes ? (byte)1 : -1);
        return contributes;
    }

    private long dominantSourceAt(TorchRtxState torchState, long sampleKey) {
        if (this.dominantSourceCache.containsKey(sampleKey)) {
            return (Long)this.dominantSourceCache.get(sampleKey);
        }
        long dominant = torchState.getDominantSource(sampleKey);
        this.dominantSourceCache.put(Long.valueOf(sampleKey), Long.valueOf(dominant));
        return dominant;
    }

    private boolean hasPositiveCoverageAt(TorchRtxState torchState, long sampleKey) {
        byte cached = this.sampleCoverageCache.get(sampleKey);
        if (cached != 0) {
            return cached > 0;
        }
        boolean positive = torchState.sampleCoverage(sampleKey) > 0;
        this.sampleCoverageCache.put(sampleKey, positive ? (byte)1 : -1);
        return positive;
    }

    private void buildPlaneGroup(NeoCpuLocalBlockAccessor accessor, long sourceKey, Vec3 source, double radius, PlaneGroup group, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        if (group.receivers.isEmpty()) {
            return;
        }
        group.hitsByBlock.clear();
        for (ReceiverEntry entry : group.receivers) {
            if (group.hitsByBlock.size() >= 1024) break;
            this.sampleReceiver(accessor, sourceKey, source, entry.receiver(), radius, group.hitsByBlock);
        }
        for (Hit hit : group.hitsByBlock.values()) {
            ++this.debugStats.hitBlocks;
            NeoCpuShadowTypes.Occluder[] occluders = this.occludersAt(accessor, hit.occluder().blockKey());
            if (occluders == null || occluders.length == 0) {
                this.appendProjectedOccluderShadow(source, group, hit.occluder(), polygons);
                if (polygons.size() < 8192) continue;
                return;
            }
            for (NeoCpuShadowTypes.Occluder occluder : occluders) {
                this.appendProjectedOccluderShadow(source, group, occluder, polygons);
                if (polygons.size() < 8192) continue;
                return;
            }
        }
    }

    private void sampleReceiver(NeoCpuLocalBlockAccessor accessor, long sourceKey, Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, double radius, Long2ObjectOpenHashMap<Hit> hitsByBlock) {
        int index;
        double minU = receiver.minU();
        double maxU = receiver.maxU();
        double minV = receiver.minV();
        double maxV = receiver.maxV();
        ArrayList<Hit> localHits = new ArrayList<Hit>(9);
        ProbeStats probeStats = new ProbeStats();
        ProbeSample center = this.sampleReceiverPoint(accessor, sourceKey, source, receiver, (minU + maxU) * 0.5, (minV + maxV) * 0.5, radius, localHits);
        probeStats.recordCenter(center);
        if (!NeoCpuFloodShadowBuilder.shouldContinueReceiverSampling(probeStats, false)) {
            NeoCpuFloodShadowBuilder.mergeReceiverHits(hitsByBlock, localHits);
            return;
        }
        for (index = 0; index < CROSS_PROBE_LOCAL_U.length && localHits.size() < 9; ++index) {
            ProbeSample cross = this.sampleReceiverPoint(accessor, sourceKey, source, receiver, minU + (maxU - minU) * CROSS_PROBE_LOCAL_U[index], minV + (maxV - minV) * CROSS_PROBE_LOCAL_V[index], radius, localHits);
            probeStats.recordCross(cross);
            if (NeoCpuFloodShadowBuilder.shouldContinueReceiverSampling(probeStats, false)) continue;
            NeoCpuFloodShadowBuilder.mergeReceiverHits(hitsByBlock, localHits);
            return;
        }
        if (!NeoCpuFloodShadowBuilder.shouldContinueReceiverSampling(probeStats, true)) {
            NeoCpuFloodShadowBuilder.mergeReceiverHits(hitsByBlock, localHits);
            return;
        }
        for (index = 0; index < CORNER_PROBE_LOCAL_U.length && localHits.size() < 9; ++index) {
            ProbeSample corner = this.sampleReceiverPoint(accessor, sourceKey, source, receiver, minU + (maxU - minU) * CORNER_PROBE_LOCAL_U[index], minV + (maxV - minV) * CORNER_PROBE_LOCAL_V[index], radius, localHits);
            probeStats.recordCorner(corner);
            if (!NeoCpuFloodShadowBuilder.shouldContinueReceiverSampling(probeStats, true)) break;
        }
        NeoCpuFloodShadowBuilder.mergeReceiverHits(hitsByBlock, localHits);
    }

    private static void mergeReceiverHits(Long2ObjectOpenHashMap<Hit> hitsByBlock, ArrayList<Hit> localHits) {
        for (Hit hit : localHits) {
            if (hitsByBlock.size() >= 1024) {
                return;
            }
            long key = hit.occluder().blockKey();
            if (hitsByBlock.containsKey(key)) continue;
            hitsByBlock.put(key, (Object)hit);
        }
    }

    private ProbeSample sampleReceiverPoint(NeoCpuLocalBlockAccessor accessor, long sourceKey, Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, double u, double v, double radius, ArrayList<Hit> hits) {
        double targetZ;
        double dz;
        double targetY;
        double dy;
        double targetX = receiver.sampleX(u, v) + receiver.normalX() * 0.0015;
        double dx = targetX - source.f_82479_;
        double distanceSq = dx * dx + (dy = (targetY = receiver.sampleY(u, v) + receiver.normalY() * 0.0015) - source.f_82480_) * dy + (dz = (targetZ = receiver.sampleZ(u, v) + receiver.normalZ() * 0.0015) - source.f_82481_) * dz;
        if (distanceSq <= 1.0E-4 || distanceSq > radius * radius) {
            return ProbeSample.NONE;
        }
        double distance = Math.sqrt(distanceSq);
        Hit hit = this.traceFirstHit(accessor, source.f_82479_, source.f_82480_, source.f_82481_, targetX, targetY, targetZ, sourceKey, receiver);
        if (hit != null) {
            NeoCpuFloodShadowBuilder.addUniqueHit(hits, hit);
        }
        return new ProbeSample(hit, NeoCpuFloodShadowBuilder.isSuspiciousProbe(receiver, dx, dy, dz, distance, hit));
    }

    private static void addUniqueHit(ArrayList<Hit> hits, Hit hit) {
        for (Hit existing : hits) {
            if (existing.occluder() != hit.occluder()) continue;
            return;
        }
        if (hits.size() < 9) {
            hits.add(hit);
        }
    }

    private Hit traceFirstHit(NeoCpuLocalBlockAccessor accessor, double sourceX, double sourceY, double sourceZ, double targetX, double targetY, double targetZ, long sourceKey, NeoCpuShadowTypes.ReceiverSurface receiver) {
        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double dz = targetZ - sourceZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-6) {
            return null;
        }
        double dirX = dx / distance;
        double dirY = dy / distance;
        double dirZ = dz / distance;
        double invDirX = Math.abs(dirX) <= 1.0E-6 ? Double.POSITIVE_INFINITY : 1.0 / dirX;
        double invDirY = Math.abs(dirY) <= 1.0E-6 ? Double.POSITIVE_INFINITY : 1.0 / dirY;
        double invDirZ = Math.abs(dirZ) <= 1.0E-6 ? Double.POSITIVE_INFINITY : 1.0 / dirZ;
        int blockX = Mth.m_14107_((double)sourceX);
        int blockY = Mth.m_14107_((double)sourceY);
        int blockZ = Mth.m_14107_((double)sourceZ);
        int stepX = dirX > 0.0 ? 1 : dirX < 0.0 ? -1 : 0;
        int stepY = dirY > 0.0 ? 1 : dirY < 0.0 ? -1 : 0;
        int stepZ = dirZ > 0.0 ? 1 : dirZ < 0.0 ? -1 : 0;
        double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? (double)blockX + 1.0 : (double)blockX) - sourceX) / dirX;
        double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? (double)blockY + 1.0 : (double)blockY) - sourceY) / dirY;
        double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? (double)blockZ + 1.0 : (double)blockZ) - sourceZ) / dirZ;
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(invDirX);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(invDirY);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(invDirZ);
        double traveled = 0.0;
        double bestT = distance;
        while (traveled < bestT - 1.0E-4) {
            boolean advanceZ;
            double next;
            NeoCpuShadowTypes.Occluder[] occluders;
            long blockKey = BlockPos.m_121882_((int)blockX, (int)blockY, (int)blockZ);
            if ((blockKey != sourceKey || receiver.blockKey() == sourceKey) && (occluders = this.occludersAt(accessor, blockKey)) != null) {
                for (NeoCpuShadowTypes.Occluder occluder : occluders) {
                    double hitT = NeoCpuFloodShadowBuilder.intersectOccluder(occluder, sourceX, sourceY, sourceZ, dirX, dirY, dirZ, invDirX, invDirY, invDirZ, bestT);
                    if (blockKey == receiver.blockKey() && NeoCpuFloodShadowBuilder.isReceiverSurfaceSelfHit(receiver, sourceX, sourceY, sourceZ, dirX, dirY, dirZ, hitT) || !(occluder.opacity() > 0.0f) || !(hitT > 0.035) || !(hitT < bestT - 1.0E-4)) continue;
                    return new Hit(occluder, hitT);
                }
            }
            if ((next = Math.min(bestT, Math.min(tMaxX, Math.min(tMaxY, tMaxZ)))) <= traveled + 1.0E-7) break;
            traveled = next;
            boolean advanceX = tMaxX <= next + 1.0E-9;
            boolean advanceY = tMaxY <= next + 1.0E-9;
            boolean bl = advanceZ = tMaxZ <= next + 1.0E-9;
            if (advanceX) {
                blockX += stepX;
                tMaxX += tDeltaX;
            }
            if (advanceY) {
                blockY += stepY;
                tMaxY += tDeltaY;
            }
            if (!advanceZ) continue;
            blockZ += stepZ;
            tMaxZ += tDeltaZ;
        }
        return null;
    }

    private NeoCpuShadowTypes.Occluder[] occludersAt(NeoCpuLocalBlockAccessor accessor, long blockKey) {
        if (this.occluderCache.containsKey(blockKey)) {
            return (NeoCpuShadowTypes.Occluder[])this.occluderCache.get(blockKey);
        }
        NeoCpuShadowShape shape = accessor.shapeAt(blockKey);
        NeoCpuShadowTypes.Occluder[] occluders = null;
        if (shape.hasOccluders()) {
            occluders = shape.instantiateOccluders(blockKey, BlockPos.m_121983_((long)blockKey), BlockPos.m_122008_((long)blockKey), BlockPos.m_122015_((long)blockKey));
        }
        this.occluderCache.put(blockKey, occluders);
        return occluders;
    }

    void appendProjectedOccluderShadow(Vec3 source, List<ReceiverEntry> receivers, NeoCpuShadowTypes.Occluder occluder, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        if (receivers.isEmpty()) {
            return;
        }
        this.projected.clear();
        NeoCpuShadowTypes.ReceiverSurface reference = receivers.get(0).receiver();
        if (occluder instanceof NeoCpuShadowTypes.Box) {
            NeoCpuShadowTypes.Box box = (NeoCpuShadowTypes.Box)occluder;
            this.appendProjectedBoxShadow(source, reference, box);
        } else if (occluder instanceof NeoCpuShadowTypes.QuadOccluder) {
            NeoCpuShadowTypes.QuadOccluder quad = (NeoCpuShadowTypes.QuadOccluder)occluder;
            this.appendProjectedQuadShadow(source, reference, quad.vertices());
        } else {
            return;
        }
        if (this.projected.size() < 3) {
            return;
        }
        this.buildHull(this.projected, this.hull);
        if (this.hull.size() < 3) {
            return;
        }
        double minU = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (NeoCpuShadowTypes.Uv point : this.hull) {
            minU = Math.min(minU, point.u());
            maxU = Math.max(maxU, point.u());
            minV = Math.min(minV, point.v());
            maxV = Math.max(maxV, point.v());
        }
        float strength = occluder.opacity();
        for (ReceiverEntry entry : receivers) {
            if (polygons.size() >= 8192) {
                return;
            }
            this.appendProjectedShadowToReceiver(entry.receiver(), this.hull, minU, maxU, minV, maxV, entry.strength() * strength, polygons);
        }
    }

    void appendProjectedOccluderShadowFromStaticGroup(Vec3 source, List<NeoCpuShadowTypes.ReceiverWithStrength> receivers, NeoCpuShadowTypes.Occluder occluder, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        if (receivers.isEmpty()) {
            return;
        }
        this.projected.clear();
        NeoCpuShadowTypes.ReceiverSurface reference = receivers.get(0).receiver();
        if (occluder instanceof NeoCpuShadowTypes.Box) {
            NeoCpuShadowTypes.Box box = (NeoCpuShadowTypes.Box)occluder;
            this.appendProjectedBoxShadow(source, reference, box);
        } else if (occluder instanceof NeoCpuShadowTypes.QuadOccluder) {
            NeoCpuShadowTypes.QuadOccluder quad = (NeoCpuShadowTypes.QuadOccluder)occluder;
            this.appendProjectedQuadShadow(source, reference, quad.vertices());
        } else {
            return;
        }
        if (this.projected.size() < 3) {
            return;
        }
        this.buildHull(this.projected, this.hull);
        if (this.hull.size() < 3) {
            return;
        }
        double minU = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (NeoCpuShadowTypes.Uv point : this.hull) {
            minU = Math.min(minU, point.u());
            maxU = Math.max(maxU, point.u());
            minV = Math.min(minV, point.v());
            maxV = Math.max(maxV, point.v());
        }
        float strength = occluder.opacity();
        for (NeoCpuShadowTypes.ReceiverWithStrength entry : receivers) {
            if (polygons.size() >= 8192) {
                return;
            }
            this.appendProjectedShadowToReceiver(entry.receiver(), this.hull, minU, maxU, minV, maxV, entry.strength() * strength, polygons);
        }
    }

    private void appendProjectedOccluderShadow(Vec3 source, PlaneGroup group, NeoCpuShadowTypes.Occluder occluder, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        this.appendProjectedOccluderShadow(source, group.receivers, occluder, polygons);
    }

    private void appendProjectedShadowToReceiver(NeoCpuShadowTypes.ReceiverSurface receiver, List<NeoCpuShadowTypes.Uv> planePolygon, double minU, double maxU, double minV, double maxV, float strength, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        if (strength <= 0.0f) {
            return;
        }
        if (receiver.maxU() <= minU || receiver.minU() >= maxU || receiver.maxV() <= minV || receiver.minV() >= maxV) {
            return;
        }
        if (minU <= receiver.minU() && maxU >= receiver.maxU() && minV <= receiver.minV() && maxV >= receiver.maxV() && NeoCpuFloodShadowBuilder.rectFullyInsideConvexPolygon(planePolygon, receiver)) {
            double[] vertices = this.triangulate(receiver, this.rectPolygon(receiver));
            if (vertices.length >= 9) {
                polygons.add(new NeoCpuShadowTypes.ShadowPolygon(vertices, receiver.normalX(), receiver.normalY(), receiver.normalZ(), strength));
            }
            return;
        }
        this.polygonBuf.clear();
        this.polygonBuf.addAll(planePolygon);
        List<NeoCpuShadowTypes.Uv> clipped = this.clipPolygonToRect(receiver.minU(), receiver.maxU(), receiver.minV(), receiver.maxV());
        if (clipped.size() < 3 || NeoCpuFloodShadowBuilder.polygonArea(clipped) <= 1.0E-4) {
            return;
        }
        this.buildHull(clipped, this.polygonBuf);
        if (this.polygonBuf.size() < 3 || NeoCpuFloodShadowBuilder.polygonArea(this.polygonBuf) <= 1.0E-4) {
            return;
        }
        List<NeoCpuShadowTypes.Uv> normalized = this.normalizeShadowPolygonToReceiver(this.polygonBuf, receiver);
        if (normalized.size() < 3 || NeoCpuFloodShadowBuilder.polygonArea(normalized) <= 1.0E-4) {
            return;
        }
        double[] triangleVertices = this.triangulate(receiver, normalized);
        if (triangleVertices.length < 9) {
            return;
        }
        polygons.add(new NeoCpuShadowTypes.ShadowPolygon(triangleVertices, receiver.normalX(), receiver.normalY(), receiver.normalZ(), strength));
    }

    private void appendProjectedBoxShadow(Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, NeoCpuShadowTypes.Box box) {
        this.boxCornerScratch[0][0] = box.minX();
        this.boxCornerScratch[0][1] = box.minY();
        this.boxCornerScratch[0][2] = box.minZ();
        this.boxCornerScratch[1][0] = box.maxX();
        this.boxCornerScratch[1][1] = box.minY();
        this.boxCornerScratch[1][2] = box.minZ();
        this.boxCornerScratch[2][0] = box.minX();
        this.boxCornerScratch[2][1] = box.maxY();
        this.boxCornerScratch[2][2] = box.minZ();
        this.boxCornerScratch[3][0] = box.maxX();
        this.boxCornerScratch[3][1] = box.maxY();
        this.boxCornerScratch[3][2] = box.minZ();
        this.boxCornerScratch[4][0] = box.minX();
        this.boxCornerScratch[4][1] = box.minY();
        this.boxCornerScratch[4][2] = box.maxZ();
        this.boxCornerScratch[5][0] = box.maxX();
        this.boxCornerScratch[5][1] = box.minY();
        this.boxCornerScratch[5][2] = box.maxZ();
        this.boxCornerScratch[6][0] = box.minX();
        this.boxCornerScratch[6][1] = box.maxY();
        this.boxCornerScratch[6][2] = box.maxZ();
        this.boxCornerScratch[7][0] = box.maxX();
        this.boxCornerScratch[7][1] = box.maxY();
        this.boxCornerScratch[7][2] = box.maxZ();
        for (int i = 0; i < 8; ++i) {
            double[] corner = this.boxCornerScratch[i];
            this.boxCornerProjScratch[i] = NeoCpuFloodShadowBuilder.projectWorldPoint(source, receiver, corner[0], corner[1], corner[2]);
            NeoCpuShadowTypes.Uv uv = this.boxCornerProjScratch[i].uv();
            if (uv == null) continue;
            this.projected.add(uv);
        }
        for (int[] edge : BOX_EDGE_VERTEX_INDICES) {
            this.appendExtendedEdgeUv(source, receiver, this.boxCornerScratch[edge[0]], this.boxCornerScratch[edge[1]], this.boxCornerProjScratch[edge[0]], this.boxCornerProjScratch[edge[1]]);
        }
    }

    private void appendProjectedQuadShadow(Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, Vec3[] vertices) {
        int i;
        if (vertices.length < 4) {
            return;
        }
        for (i = 0; i < 4; ++i) {
            Vec3 vertex = vertices[i];
            this.quadCornerProjScratch[i] = NeoCpuFloodShadowBuilder.projectWorldPoint(source, receiver, vertex.f_82479_, vertex.f_82480_, vertex.f_82481_);
            NeoCpuShadowTypes.Uv uv = this.quadCornerProjScratch[i].uv();
            if (uv == null) continue;
            this.projected.add(uv);
        }
        for (i = 0; i < 4; ++i) {
            int next = i + 1 & 3;
            Vec3 start = vertices[i];
            Vec3 end = vertices[next];
            this.appendExtendedEdgeUv(source, receiver, new double[]{start.f_82479_, start.f_82480_, start.f_82481_}, new double[]{end.f_82479_, end.f_82480_, end.f_82481_}, this.quadCornerProjScratch[i], this.quadCornerProjScratch[next]);
        }
    }

    private void appendExtendedEdgeUv(Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, double[] start, double[] end, CpuProjection startProjection, CpuProjection endProjection) {
        boolean endValid;
        boolean startValid = startProjection.uv() != null;
        boolean bl = endValid = endProjection.uv() != null;
        if (startValid == endValid) {
            return;
        }
        double denominatorDelta = endProjection.denominator() - startProjection.denominator();
        if (Math.abs(denominatorDelta) <= 1.0E-6) {
            return;
        }
        double singularParam = -startProjection.denominator() / denominatorDelta;
        if (singularParam < -1.0E-6 || singularParam > 1.000001) {
            return;
        }
        singularParam = Math.max(0.0, Math.min(1.0, singularParam));
        boolean validFromStart = startValid;
        double outerOffset = validFromStart ? -0.001 : 0.001;
        double innerOffset = validFromStart ? -2.5E-4 : 2.5E-4;
        double outerParam = NeoCpuFloodShadowBuilder.clampUnitEdge(singularParam + outerOffset);
        double innerParam = NeoCpuFloodShadowBuilder.clampUnitEdge(singularParam + innerOffset);
        if (Math.abs(innerParam - outerParam) <= 1.0E-6) {
            return;
        }
        double ox = start[0] + (end[0] - start[0]) * outerParam;
        double oy = start[1] + (end[1] - start[1]) * outerParam;
        double oz = start[2] + (end[2] - start[2]) * outerParam;
        double ix = start[0] + (end[0] - start[0]) * innerParam;
        double iy = start[1] + (end[1] - start[1]) * innerParam;
        double iz = start[2] + (end[2] - start[2]) * innerParam;
        CpuProjection outerP = NeoCpuFloodShadowBuilder.projectWorldPoint(source, receiver, ox, oy, oz);
        CpuProjection innerP = NeoCpuFloodShadowBuilder.projectWorldPoint(source, receiver, ix, iy, iz);
        NeoCpuShadowTypes.Uv outerUv = outerP.uv();
        NeoCpuShadowTypes.Uv innerUv = innerP.uv();
        if (outerUv == null || innerUv == null) {
            return;
        }
        this.projected.add(outerUv);
        this.projected.add(innerUv);
        double directionU = innerUv.u() - outerUv.u();
        double directionV = innerUv.v() - outerUv.v();
        double directionLengthSq = directionU * directionU + directionV * directionV;
        if (directionLengthSq <= 1.0E-6) {
            return;
        }
        double directionLength = Math.sqrt(directionLengthSq);
        this.projected.add(new NeoCpuShadowTypes.Uv(innerUv.u() + directionU / directionLength * 8.0, innerUv.v() + directionV / directionLength * 8.0));
    }

    private static CpuProjection projectWorldPoint(Vec3 source, NeoCpuShadowTypes.ReceiverSurface receiver, double x, double y, double z) {
        double denominator = switch (receiver.face()) {
            case UP, DOWN -> y - source.f_82480_;
            case NORTH, SOUTH -> z - source.f_82481_;
            case WEST, EAST -> x - source.f_82479_;
        };
        double planeCoordinate = receiver.plane();
        if (Math.abs(denominator) <= 1.0E-9) {
            return new CpuProjection(denominator, Double.NaN, null);
        }
        double t = (planeCoordinate - switch (receiver.face()) {
            case UP, DOWN -> source.f_82480_;
            case NORTH, SOUTH -> source.f_82481_;
            case WEST, EAST -> source.f_82479_;
        }) / denominator;
        if (t <= 0.0) {
            return new CpuProjection(denominator, t, null);
        }
        double projectedX = source.f_82479_ + (x - source.f_82479_) * t;
        double projectedY = source.f_82480_ + (y - source.f_82480_) * t;
        double projectedZ = source.f_82481_ + (z - source.f_82481_) * t;
        return new CpuProjection(denominator, t, receiver.worldToUv(projectedX, projectedY, projectedZ));
    }

    private void buildHull(List<NeoCpuShadowTypes.Uv> input, ArrayList<NeoCpuShadowTypes.Uv> output) {
        int i;
        output.clear();
        this.sortBuf.clear();
        this.sortBuf.addAll(input);
        this.sortBuf.sort(Comparator.comparingDouble(NeoCpuShadowTypes.Uv::u).thenComparingDouble(NeoCpuShadowTypes.Uv::v));
        this.uniqueBuf.clear();
        for (NeoCpuShadowTypes.Uv point : this.sortBuf) {
            if (!this.uniqueBuf.isEmpty() && point.closeTo(this.uniqueBuf.get(this.uniqueBuf.size() - 1))) continue;
            this.uniqueBuf.add(point);
        }
        if (this.uniqueBuf.size() < 3) {
            output.addAll(this.uniqueBuf);
            return;
        }
        this.lowerBuf.clear();
        for (NeoCpuShadowTypes.Uv point : this.uniqueBuf) {
            while (this.lowerBuf.size() >= 2 && NeoCpuFloodShadowBuilder.cross(this.lowerBuf.get(this.lowerBuf.size() - 2), this.lowerBuf.get(this.lowerBuf.size() - 1), point) <= 1.0E-6) {
                this.lowerBuf.remove(this.lowerBuf.size() - 1);
            }
            this.lowerBuf.add(point);
        }
        this.upperBuf.clear();
        for (i = this.uniqueBuf.size() - 1; i >= 0; --i) {
            NeoCpuShadowTypes.Uv point;
            point = this.uniqueBuf.get(i);
            while (this.upperBuf.size() >= 2 && NeoCpuFloodShadowBuilder.cross(this.upperBuf.get(this.upperBuf.size() - 2), this.upperBuf.get(this.upperBuf.size() - 1), point) <= 1.0E-6) {
                this.upperBuf.remove(this.upperBuf.size() - 1);
            }
            this.upperBuf.add(point);
        }
        for (i = 0; i < this.lowerBuf.size() - 1; ++i) {
            output.add(this.lowerBuf.get(i));
        }
        for (i = 0; i < this.upperBuf.size() - 1; ++i) {
            output.add(this.upperBuf.get(i));
        }
    }

    private List<NeoCpuShadowTypes.Uv> dilate(List<NeoCpuShadowTypes.Uv> polygon, double amount, ArrayList<NeoCpuShadowTypes.Uv> out) {
        if (amount <= 1.0E-6 || polygon.isEmpty()) {
            return polygon;
        }
        double centerU = 0.0;
        double centerV = 0.0;
        for (NeoCpuShadowTypes.Uv point : polygon) {
            centerU += point.u();
            centerV += point.v();
        }
        centerU /= (double)polygon.size();
        centerV /= (double)polygon.size();
        out.clear();
        for (NeoCpuShadowTypes.Uv point : polygon) {
            double dv;
            double du = point.u() - centerU;
            double length = Math.sqrt(du * du + (dv = point.v() - centerV) * dv);
            if (length <= 1.0E-6) {
                out.add(point);
                continue;
            }
            out.add(new NeoCpuShadowTypes.Uv(point.u() + du / length * amount, point.v() + dv / length * amount));
        }
        return out;
    }

    private List<NeoCpuShadowTypes.Uv> normalizeShadowPolygonToReceiver(List<NeoCpuShadowTypes.Uv> polygon, NeoCpuShadowTypes.ReceiverSurface receiver) {
        double rectWidth = receiver.width();
        double rectHeight = receiver.height();
        if (rectWidth <= 1.0E-6 || rectHeight <= 1.0E-6) {
            return polygon;
        }
        double rectArea = rectWidth * rectHeight;
        if (rectArea <= 1.0E-4) {
            return polygon;
        }
        if (NeoCpuFloodShadowBuilder.polygonArea(polygon) / rectArea >= 0.988) {
            return this.rectPolygon(receiver);
        }
        this.localPolygon.clear();
        for (NeoCpuShadowTypes.Uv point : polygon) {
            this.localPolygon.add(new NeoCpuShadowTypes.Uv((point.u() - receiver.minU()) / rectWidth, (point.v() - receiver.minV()) / rectHeight));
        }
        List<NeoCpuShadowTypes.Uv> dilated = this.dilate(this.localPolygon, NeoCpuFloodShadowBuilder.shadowPolygonDilation(receiver.face()), this.dilatedPolygon);
        List<NeoCpuShadowTypes.Uv> snapped = this.snapPolygonToEdges(dilated, NeoCpuFloodShadowBuilder.shadowEdgeSnapEpsilon(receiver.face()), this.snappedPolygon);
        this.polygonBuf.clear();
        this.polygonBuf.addAll(snapped);
        List<NeoCpuShadowTypes.Uv> clippedLocal = this.clipPolygonToRect(0.0, 1.0, 0.0, 1.0);
        if (clippedLocal.size() < 3) {
            return polygon;
        }
        this.buildHull(clippedLocal, this.normalizedPolygon);
        if (this.normalizedPolygon.size() < 3) {
            return polygon;
        }
        if (NeoCpuFloodShadowBuilder.polygonArea(this.normalizedPolygon) >= 0.988) {
            return this.rectPolygon(receiver);
        }
        for (int i = 0; i < this.normalizedPolygon.size(); ++i) {
            NeoCpuShadowTypes.Uv point = this.normalizedPolygon.get(i);
            this.normalizedPolygon.set(i, new NeoCpuShadowTypes.Uv(receiver.minU() + point.u() * rectWidth, receiver.minV() + point.v() * rectHeight));
        }
        return this.normalizedPolygon;
    }

    private List<NeoCpuShadowTypes.Uv> snapPolygonToEdges(List<NeoCpuShadowTypes.Uv> polygon, double epsilon, ArrayList<NeoCpuShadowTypes.Uv> out) {
        if (polygon.isEmpty() || epsilon <= 1.0E-6) {
            return polygon;
        }
        out.clear();
        for (NeoCpuShadowTypes.Uv point : polygon) {
            out.add(new NeoCpuShadowTypes.Uv(NeoCpuFloodShadowBuilder.snapUvCoordinate(point.u(), epsilon), NeoCpuFloodShadowBuilder.snapUvCoordinate(point.v(), epsilon)));
        }
        return out;
    }

    private List<NeoCpuShadowTypes.Uv> clipPolygonToRect(double minU, double maxU, double minV, double maxV) {
        double t;
        double delta;
        boolean curIn;
        this.clipA.clear();
        ArrayList<NeoCpuShadowTypes.Uv> input = this.polygonBuf;
        if (input.isEmpty()) {
            return this.clipA;
        }
        NeoCpuShadowTypes.Uv prev = (NeoCpuShadowTypes.Uv)input.get(input.size() - 1);
        boolean prevIn = prev.u() >= minU;
        for (NeoCpuShadowTypes.Uv cur : input) {
            boolean bl = curIn = cur.u() >= minU;
            if (curIn) {
                if (!prevIn) {
                    delta = cur.u() - prev.u();
                    t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (minU - prev.u()) / delta;
                    this.clipA.add(new NeoCpuShadowTypes.Uv(minU, prev.v() + (cur.v() - prev.v()) * t));
                }
                this.clipA.add(cur);
            } else if (prevIn) {
                delta = cur.u() - prev.u();
                t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (minU - prev.u()) / delta;
                this.clipA.add(new NeoCpuShadowTypes.Uv(minU, prev.v() + (cur.v() - prev.v()) * t));
            }
            prev = cur;
            prevIn = curIn;
        }
        if (this.clipA.isEmpty()) {
            return this.clipA;
        }
        this.clipB.clear();
        input = this.clipA;
        prev = (NeoCpuShadowTypes.Uv)input.get(input.size() - 1);
        prevIn = prev.u() <= maxU;
        for (NeoCpuShadowTypes.Uv cur : input) {
            boolean bl = curIn = cur.u() <= maxU;
            if (curIn) {
                if (!prevIn) {
                    delta = cur.u() - prev.u();
                    t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (maxU - prev.u()) / delta;
                    this.clipB.add(new NeoCpuShadowTypes.Uv(maxU, prev.v() + (cur.v() - prev.v()) * t));
                }
                this.clipB.add(cur);
            } else if (prevIn) {
                delta = cur.u() - prev.u();
                t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (maxU - prev.u()) / delta;
                this.clipB.add(new NeoCpuShadowTypes.Uv(maxU, prev.v() + (cur.v() - prev.v()) * t));
            }
            prev = cur;
            prevIn = curIn;
        }
        if (this.clipB.isEmpty()) {
            return this.clipB;
        }
        this.clipA.clear();
        input = this.clipB;
        prev = (NeoCpuShadowTypes.Uv)input.get(input.size() - 1);
        prevIn = prev.v() >= minV;
        for (NeoCpuShadowTypes.Uv cur : input) {
            boolean bl = curIn = cur.v() >= minV;
            if (curIn) {
                if (!prevIn) {
                    delta = cur.v() - prev.v();
                    t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (minV - prev.v()) / delta;
                    this.clipA.add(new NeoCpuShadowTypes.Uv(prev.u() + (cur.u() - prev.u()) * t, minV));
                }
                this.clipA.add(cur);
            } else if (prevIn) {
                delta = cur.v() - prev.v();
                t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (minV - prev.v()) / delta;
                this.clipA.add(new NeoCpuShadowTypes.Uv(prev.u() + (cur.u() - prev.u()) * t, minV));
            }
            prev = cur;
            prevIn = curIn;
        }
        if (this.clipA.isEmpty()) {
            return this.clipA;
        }
        this.clipB.clear();
        input = this.clipA;
        prev = (NeoCpuShadowTypes.Uv)input.get(input.size() - 1);
        prevIn = prev.v() <= maxV;
        for (NeoCpuShadowTypes.Uv cur : input) {
            boolean bl = curIn = cur.v() <= maxV;
            if (curIn) {
                if (!prevIn) {
                    delta = cur.v() - prev.v();
                    t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (maxV - prev.v()) / delta;
                    this.clipB.add(new NeoCpuShadowTypes.Uv(prev.u() + (cur.u() - prev.u()) * t, maxV));
                }
                this.clipB.add(cur);
            } else if (prevIn) {
                delta = cur.v() - prev.v();
                t = Math.abs(delta) <= 1.0E-9 ? 0.0 : (maxV - prev.v()) / delta;
                this.clipB.add(new NeoCpuShadowTypes.Uv(prev.u() + (cur.u() - prev.u()) * t, maxV));
            }
            prev = cur;
            prevIn = curIn;
        }
        return this.clipB;
    }

    private double[] triangulate(NeoCpuShadowTypes.ReceiverSurface receiver, List<NeoCpuShadowTypes.Uv> polygon) {
        int triangleCount = polygon.size() - 2;
        double[] vertices = new double[triangleCount * 9];
        NeoCpuShadowTypes.Uv first = polygon.get(0);
        int offset = 0;
        for (int i = 1; i < polygon.size() - 1; ++i) {
            offset = NeoCpuFloodShadowBuilder.writeVertex(receiver, first, vertices, offset);
            offset = NeoCpuFloodShadowBuilder.writeVertex(receiver, polygon.get(i), vertices, offset);
            offset = NeoCpuFloodShadowBuilder.writeVertex(receiver, polygon.get(i + 1), vertices, offset);
        }
        return vertices;
    }

    private static int writeVertex(NeoCpuShadowTypes.ReceiverSurface receiver, NeoCpuShadowTypes.Uv point, double[] vertices, int offset) {
        vertices[offset] = receiver.sampleX(point.u(), point.v()) + receiver.normalX() * 0.0015;
        vertices[offset + 1] = receiver.sampleY(point.u(), point.v()) + receiver.normalY() * 0.0015;
        vertices[offset + 2] = receiver.sampleZ(point.u(), point.v()) + receiver.normalZ() * 0.0015;
        return offset + 3;
    }

    private static boolean rectFullyInsideConvexPolygon(List<NeoCpuShadowTypes.Uv> polygon, NeoCpuShadowTypes.ReceiverSurface receiver) {
        if (polygon.size() < 3) {
            return false;
        }
        return NeoCpuFloodShadowBuilder.convexPolygonContainsPoint(polygon, receiver.minU(), receiver.minV()) && NeoCpuFloodShadowBuilder.convexPolygonContainsPoint(polygon, receiver.maxU(), receiver.minV()) && NeoCpuFloodShadowBuilder.convexPolygonContainsPoint(polygon, receiver.maxU(), receiver.maxV()) && NeoCpuFloodShadowBuilder.convexPolygonContainsPoint(polygon, receiver.minU(), receiver.maxV());
    }

    private static boolean convexPolygonContainsPoint(List<NeoCpuShadowTypes.Uv> polygon, double u, double v) {
        double sign = 0.0;
        int size = polygon.size();
        for (int index = 0; index < size; ++index) {
            NeoCpuShadowTypes.Uv current = polygon.get(index);
            NeoCpuShadowTypes.Uv next = polygon.get((index + 1) % size);
            double cross = (next.u() - current.u()) * (v - current.v()) - (next.v() - current.v()) * (u - current.u());
            if (Math.abs(cross) <= 1.0E-6) continue;
            if (sign == 0.0) {
                sign = Math.copySign(1.0, cross);
                continue;
            }
            if (!(cross * sign < -1.0E-6)) continue;
            return false;
        }
        return true;
    }

    private List<NeoCpuShadowTypes.Uv> rectPolygon(NeoCpuShadowTypes.ReceiverSurface receiver) {
        this.rectPolygon.clear();
        this.rectPolygon.add(new NeoCpuShadowTypes.Uv(receiver.minU(), receiver.minV()));
        this.rectPolygon.add(new NeoCpuShadowTypes.Uv(receiver.maxU(), receiver.minV()));
        this.rectPolygon.add(new NeoCpuShadowTypes.Uv(receiver.maxU(), receiver.maxV()));
        this.rectPolygon.add(new NeoCpuShadowTypes.Uv(receiver.minU(), receiver.maxV()));
        return this.rectPolygon;
    }

    private static double polygonArea(List<NeoCpuShadowTypes.Uv> polygon) {
        double area = 0.0;
        for (int i = 0; i < polygon.size(); ++i) {
            NeoCpuShadowTypes.Uv a = polygon.get(i);
            NeoCpuShadowTypes.Uv b = polygon.get((i + 1) % polygon.size());
            area += a.u() * b.v() - b.u() * a.v();
        }
        return Math.abs(area) * 0.5;
    }

    private static double cross(NeoCpuShadowTypes.Uv a, NeoCpuShadowTypes.Uv b, NeoCpuShadowTypes.Uv c) {
        return (b.u() - a.u()) * (c.v() - a.v()) - (b.v() - a.v()) * (c.u() - a.u());
    }

    private static double shadowPolygonDilation(Direction face) {
        return NeoCpuFloodShadowBuilder.isHorizontalFace(face) ? 0.024 : 0.016;
    }

    private static double shadowEdgeSnapEpsilon(Direction face) {
        return NeoCpuFloodShadowBuilder.isHorizontalFace(face) ? 0.03 : 0.022;
    }

    private static boolean isHorizontalFace(Direction face) {
        return face == Direction.UP || face == Direction.DOWN;
    }

    private static double snapUvCoordinate(double value, double epsilon) {
        if (value <= epsilon) {
            return 0.0;
        }
        if (value >= 1.0 - epsilon) {
            return 1.0;
        }
        return value;
    }

    private static double clampUnitEdge(double value) {
        return Mth.m_14008_((double)value, (double)1.0E-6, (double)0.999999);
    }

    private static long planeGroupKey(Direction face, double plane) {
        long quantizedPlane = Math.round(plane * 1000000.0);
        return (long)face.ordinal() << 56 ^ quantizedPlane & 0xFFFFFFFFFFFFFFL;
    }

    private static long sampleRtxPosKey(NeoCpuShadowTypes.ReceiverSurface receiver, double sampleX, double sampleY, double sampleZ) {
        int sampleBlockZ;
        int sampleBlockY;
        double outwardX = receiver.normalX() * 0.05;
        double outwardY = receiver.normalY() * 0.05;
        double outwardZ = receiver.normalZ() * 0.05;
        int sampleBlockX = Mth.m_14107_((double)(sampleX + outwardX));
        long sampleKey = BlockPos.m_121882_((int)sampleBlockX, (int)(sampleBlockY = Mth.m_14107_((double)(sampleY + outwardY))), (int)(sampleBlockZ = Mth.m_14107_((double)(sampleZ + outwardZ))));
        if (sampleKey == receiver.blockKey()) {
            return BlockPos.m_121882_((int)(BlockPos.m_121983_((long)receiver.blockKey()) + (int)receiver.normalX()), (int)(BlockPos.m_122008_((long)receiver.blockKey()) + (int)receiver.normalY()), (int)(BlockPos.m_122015_((long)receiver.blockKey()) + (int)receiver.normalZ()));
        }
        return sampleKey;
    }

    private static Direction firstFaceTangent(Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> Direction.EAST;
            case Direction.WEST, Direction.EAST -> Direction.SOUTH;
        };
    }

    private static Direction secondFaceTangent(Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> Direction.SOUTH;
            case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> Direction.UP;
        };
    }

    private static boolean isReceiverSurfaceSelfHit(NeoCpuShadowTypes.ReceiverSurface receiver, double sourceX, double sourceY, double sourceZ, double dirX, double dirY, double dirZ, double hitT) {
        if (!Double.isFinite(hitT)) {
            return false;
        }
        double hitX = sourceX + dirX * hitT;
        double hitY = sourceY + dirY * hitT;
        double hitZ = sourceZ + dirZ * hitT;
        double hitPlane = switch (receiver.face()) {
            case UP, DOWN -> hitY;
            case NORTH, SOUTH -> hitZ;
            case WEST, EAST -> hitX;
        };
        if (Math.abs(hitPlane - receiver.plane()) > 1.0E-4) {
            return false;
        }
        NeoCpuShadowTypes.Uv hitUv = receiver.worldToUv(hitX, hitY, hitZ);
        NeoCpuShadowTypes.Uv sourceUv = receiver.worldToUv(sourceX, sourceY, sourceZ);
        double du = hitUv.u() - sourceUv.u();
        double dv = hitUv.v() - sourceUv.v();
        return du * du + dv * dv <= 1.0E-8;
    }

    private static boolean isSuspiciousProbe(NeoCpuShadowTypes.ReceiverSurface receiver, double dx, double dy, double dz, double distance, Hit hit) {
        boolean largeReceiver;
        if (distance <= 1.0E-6) {
            return false;
        }
        double alignment = Math.abs((dx * receiver.normalX() + dy * receiver.normalY() + dz * receiver.normalZ()) / distance);
        boolean bl = largeReceiver = Math.max(receiver.width(), receiver.height()) >= 0.72;
        if (hit == null) {
            return largeReceiver && alignment <= 0.26;
        }
        if (!(hit.occluder() instanceof NeoCpuShadowTypes.Box)) {
            return true;
        }
        double travelFraction = hit.distance() / distance;
        return alignment <= 0.26 || travelFraction <= 0.18 || travelFraction >= 0.82;
    }

    private static boolean shouldContinueReceiverSampling(ProbeStats probeStats, boolean cornersStarted) {
        int stableThreshold;
        ProbeSample center = probeStats.center();
        if (center == null) {
            return false;
        }
        if (center.hit() == null) {
            int cleanThreshold;
            int n = cleanThreshold = center.suspicious() ? 4 : 3;
            if (probeStats.hitCount() == 0 && probeStats.totalProbes() >= cleanThreshold) {
                return false;
            }
            if (!cornersStarted) {
                if (probeStats.crossProbes() < CROSS_PROBE_LOCAL_U.length) {
                    return true;
                }
                return probeStats.hitCount() > 0;
            }
            return probeStats.cornerProbes() < CORNER_PROBE_LOCAL_U.length;
        }
        if (!center.suspicious() && center.hit().occluder() instanceof NeoCpuShadowTypes.Box && probeStats.missCount() == 0 && probeStats.differentHitCount() == 0 && probeStats.totalProbes() >= 2) {
            return false;
        }
        int n = stableThreshold = center.suspicious() ? 4 : 3;
        if (probeStats.sameAsCenterHitCount() >= stableThreshold && probeStats.missCount() == 0 && probeStats.differentHitCount() == 0) {
            return false;
        }
        if (!cornersStarted) {
            if (probeStats.crossProbes() < CROSS_PROBE_LOCAL_U.length) {
                return true;
            }
            return probeStats.missCount() > 0 || probeStats.differentHitCount() > 0;
        }
        return probeStats.cornerProbes() < CORNER_PROBE_LOCAL_U.length;
    }

    private static double intersectOccluder(NeoCpuShadowTypes.Occluder occluder, double sourceX, double sourceY, double sourceZ, double dirX, double dirY, double dirZ, double invDirX, double invDirY, double invDirZ, double maxDistance) {
        if (occluder instanceof NeoCpuShadowTypes.Box) {
            NeoCpuShadowTypes.Box box = (NeoCpuShadowTypes.Box)occluder;
            return NeoCpuFloodShadowBuilder.intersectBox(box, sourceX, sourceY, sourceZ, invDirX, invDirY, invDirZ, maxDistance);
        }
        if (occluder instanceof NeoCpuShadowTypes.QuadOccluder) {
            NeoCpuShadowTypes.QuadOccluder quad = (NeoCpuShadowTypes.QuadOccluder)occluder;
            return NeoCpuFloodShadowBuilder.intersectQuad(quad, sourceX, sourceY, sourceZ, dirX, dirY, dirZ, maxDistance);
        }
        return Double.POSITIVE_INFINITY;
    }

    private static double intersectBox(NeoCpuShadowTypes.Box box, double sourceX, double sourceY, double sourceZ, double invDirX, double invDirY, double invDirZ, double maxDistance) {
        double t1 = (box.minX() - sourceX) * invDirX;
        double t2 = (box.maxX() - sourceX) * invDirX;
        double t3 = (box.minY() - sourceY) * invDirY;
        double t4 = (box.maxY() - sourceY) * invDirY;
        double t5 = (box.minZ() - sourceZ) * invDirZ;
        double t6 = (box.maxZ() - sourceZ) * invDirZ;
        double tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        if (tMax < 0.0 || tMin > tMax || tMin >= maxDistance) {
            return Double.POSITIVE_INFINITY;
        }
        return tMin >= 0.0 ? tMin : tMax;
    }

    private static double intersectQuad(NeoCpuShadowTypes.QuadOccluder quad, double sourceX, double sourceY, double sourceZ, double dirX, double dirY, double dirZ, double maxDistance) {
        Vec3[] vertices = quad.vertices();
        if (vertices.length < 4) {
            return Double.POSITIVE_INFINITY;
        }
        double hitA = NeoCpuFloodShadowBuilder.intersectTriangle(vertices[0], vertices[1], vertices[2], sourceX, sourceY, sourceZ, dirX, dirY, dirZ, maxDistance);
        double hitB = NeoCpuFloodShadowBuilder.intersectTriangle(vertices[0], vertices[2], vertices[3], sourceX, sourceY, sourceZ, dirX, dirY, dirZ, maxDistance);
        return Math.min(hitA, hitB);
    }

    private static double intersectTriangle(Vec3 a, Vec3 b, Vec3 c, double sourceX, double sourceY, double sourceZ, double dirX, double dirY, double dirZ, double maxDistance) {
        double edge1X = b.f_82479_ - a.f_82479_;
        double edge2Z = c.f_82481_ - a.f_82481_;
        double edge2Y = c.f_82480_ - a.f_82480_;
        double pvecX = dirY * edge2Z - dirZ * edge2Y;
        double edge1Y = b.f_82480_ - a.f_82480_;
        double edge2X = c.f_82479_ - a.f_82479_;
        double pvecY = dirZ * edge2X - dirX * edge2Z;
        double edge1Z = b.f_82481_ - a.f_82481_;
        double pvecZ = dirX * edge2Y - dirY * edge2X;
        double det = edge1X * pvecX + edge1Y * pvecY + edge1Z * pvecZ;
        if (Math.abs(det) <= 1.0E-6) {
            return Double.POSITIVE_INFINITY;
        }
        double tvecX = sourceX - a.f_82479_;
        double tvecY = sourceY - a.f_82480_;
        double tvecZ = sourceZ - a.f_82481_;
        double invDet = 1.0 / det;
        double u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet;
        if (u < -1.0E-6 || u > 1.000001) {
            return Double.POSITIVE_INFINITY;
        }
        double qvecX = tvecY * edge1Z - tvecZ * edge1Y;
        double qvecY = tvecZ * edge1X - tvecX * edge1Z;
        double qvecZ = tvecX * edge1Y - tvecY * edge1X;
        double v = (dirX * qvecX + dirY * qvecY + dirZ * qvecZ) * invDet;
        if (v < -1.0E-6 || u + v > 1.000001) {
            return Double.POSITIVE_INFINITY;
        }
        double t = (edge2X * qvecX + edge2Y * qvecY + edge2Z * qvecZ) * invDet;
        if (t <= 1.0E-6 || t >= maxDistance) {
            return Double.POSITIVE_INFINITY;
        }
        return t;
    }

    private record CpuProjection(double denominator, double t, NeoCpuShadowTypes.Uv uv) {
    }

    private static final class ContributionCollector
    implements NeoFloodEngine.ContributorVisitor {
        private long sourceKey;
        private NeoCpuShadowTypes.ReceiverSurface receiver;
        private double sampleX;
        private double sampleY;
        private double sampleZ;
        private double totalScore;
        private double sourceScore;

        private ContributionCollector() {
        }

        void reset(long sourceKey, NeoCpuShadowTypes.ReceiverSurface receiver, double sampleX, double sampleY, double sampleZ) {
            this.sourceKey = sourceKey;
            this.receiver = receiver;
            this.sampleX = sampleX;
            this.sampleY = sampleY;
            this.sampleZ = sampleZ;
            this.totalScore = 0.0;
            this.sourceScore = 0.0;
        }

        @Override
        public void visit(long candidateSourceKey, int light, int coverage, int sourceEmission) {
            if (sourceEmission <= 0 || light <= 0 || coverage <= 0) {
                return;
            }
            double dx = (double)BlockPos.m_121983_((long)candidateSourceKey) + 0.5 - this.sampleX;
            double dy = (double)BlockPos.m_122008_((long)candidateSourceKey) + 0.5 - this.sampleY;
            double dz = (double)BlockPos.m_122015_((long)candidateSourceKey) + 0.5 - this.sampleZ;
            double facing = dx * this.receiver.normalX() + dy * this.receiver.normalY() + dz * this.receiver.normalZ();
            if (facing <= 1.0E-4) {
                return;
            }
            double distanceSq = dx * dx + dy * dy + dz * dz;
            double coverageFactor = Math.max(0.15, (double)coverage / 255.0);
            double score = (double)light * coverageFactor / Math.max(0.25, distanceSq + 0.25);
            this.totalScore += score;
            if (candidateSourceKey == this.sourceKey) {
                this.sourceScore += score;
            }
        }

        boolean accepted() {
            return this.sourceScore > 0.0 && (this.totalScore <= 0.0 || this.sourceScore / this.totalScore >= 0.2);
        }

        float weightScale() {
            if (this.totalScore <= 0.0) {
                return 1.0f;
            }
            return Mth.m_14036_((float)((float)Math.sqrt(this.sourceScore / this.totalScore)), (float)0.35f, (float)1.0f);
        }
    }

    private static final class SourceDebugStats {
        private long sourceKey;
        private int emission;
        private int boundaryFaces;
        private int acceptedReceivers;
        private int rejectedReceivers;
        private int dominantBaseAccepts;
        private int dominantSampleAccepts;
        private int contributorAccepts;
        private int noSampleRejects;
        private int contributorRejects;
        private int hitBlocks;
        private boolean hitPolygonCap;

        private SourceDebugStats() {
        }

        void reset(long sourceKey, int emission, int boundaryFaces) {
            this.sourceKey = sourceKey;
            this.emission = emission;
            this.boundaryFaces = boundaryFaces;
            this.acceptedReceivers = 0;
            this.rejectedReceivers = 0;
            this.dominantBaseAccepts = 0;
            this.dominantSampleAccepts = 0;
            this.contributorAccepts = 0;
            this.noSampleRejects = 0;
            this.contributorRejects = 0;
            this.hitBlocks = 0;
            this.hitPolygonCap = false;
        }
    }

    private record ContributionData(boolean accepted, float weightScale) {
        private static final ContributionData REJECTED = new ContributionData(false, 0.0f);
    }

    private static final class PlaneGroup {
        private final ArrayList<ReceiverEntry> receivers = new ArrayList(64);
        private final Long2ObjectOpenHashMap<Hit> hitsByBlock = new Long2ObjectOpenHashMap(32);

        private PlaneGroup() {
        }
    }

    record ReceiverEntry(NeoCpuShadowTypes.ReceiverSurface receiver, float strength) {
    }

    private record Hit(NeoCpuShadowTypes.Occluder occluder, double distance) {
    }

    private static final class ProbeStats {
        private ProbeSample center;
        private Hit centerHit;
        private int totalProbes;
        private int crossProbes;
        private int cornerProbes;
        private int hitCount;
        private int missCount;
        private int sameAsCenterHitCount;
        private int differentHitCount;

        private ProbeStats() {
        }

        void recordCenter(ProbeSample sample) {
            this.center = sample;
            this.centerHit = sample.hit();
            this.record(sample, true);
        }

        void recordCross(ProbeSample sample) {
            ++this.crossProbes;
            this.record(sample, false);
        }

        void recordCorner(ProbeSample sample) {
            ++this.cornerProbes;
            this.record(sample, false);
        }

        private void record(ProbeSample sample, boolean isCenter) {
            ++this.totalProbes;
            if (sample.hit() == null) {
                ++this.missCount;
                return;
            }
            ++this.hitCount;
            if (!isCenter) {
                if (this.centerHit != null && this.centerHit.occluder() == sample.hit().occluder()) {
                    ++this.sameAsCenterHitCount;
                } else {
                    ++this.differentHitCount;
                }
            }
        }

        ProbeSample center() {
            return this.center;
        }

        int totalProbes() {
            return this.totalProbes;
        }

        int crossProbes() {
            return this.crossProbes;
        }

        int cornerProbes() {
            return this.cornerProbes;
        }

        int hitCount() {
            return this.hitCount;
        }

        int missCount() {
            return this.missCount;
        }

        int sameAsCenterHitCount() {
            return this.sameAsCenterHitCount;
        }

        int differentHitCount() {
            return this.differentHitCount;
        }
    }

    private record ProbeSample(Hit hit, boolean suspicious) {
        private static final ProbeSample NONE = new ProbeSample(null, false);
    }
}
