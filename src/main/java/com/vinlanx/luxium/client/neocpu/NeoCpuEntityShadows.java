/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.neocpu.NeoCpuFloodShadowBuilder;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class NeoCpuEntityShadows {
    private static final int MAX_DYNAMIC_CASTERS = 160;
    private static final int MAX_TOTAL_POLYGONS = 8192;
    private static final double ENTITY_SNAPSHOT_MOVEMENT_PADDING = 30.0;
    private static final double SNAPSHOT_RADIUS_PADDING = 4.0;
    private static final double MAX_CASTER_AXIS = 12.0;
    private static final double MAX_CASTER_VOLUME = 512.0;
    private static final long FNV_OFFSET_BASIS = 1469598103934665603L;
    private static final long FNV_PRIME = 1099511628211L;
    private static volatile CaptureState currentState = CaptureState.empty();
    @Nullable
    private static volatile ClientLevel trackedLevel;
    @Nullable
    private static volatile BlockPos lastCenter;
    private static volatile long lastStateScanMs;
    private static volatile long lastCaptureMs;
    private static volatile long lastSceneFingerprint;

    private NeoCpuEntityShadows() {
    }

    public static boolean isActive() {
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) && Config.isFeatureEnabled(Config.CLIENT.entityShadowsEnabled);
    }

    public static void clear() {
        trackedLevel = null;
        lastCenter = null;
        lastStateScanMs = 0L;
        lastCaptureMs = 0L;
        lastSceneFingerprint = 0L;
        currentState = CaptureState.empty();
    }

    public static CaptureState updateState(@Nullable ClientLevel level, BlockPos center, float partialTick) {
        boolean sceneChanged;
        boolean captureElapsed;
        if (!NeoCpuEntityShadows.isActive() || level == null) {
            currentState = CaptureState.empty();
            trackedLevel = level;
            lastCenter = center.m_7949_();
            lastStateScanMs = 0L;
            lastCaptureMs = 0L;
            lastSceneFingerprint = 0L;
            return currentState;
        }
        long nowMs = System.currentTimeMillis();
        long intervalMs = NeoCpuEntityShadows.getUpdateIntervalMs();
        boolean levelChanged = trackedLevel != level;
        boolean centerChanged = NeoCpuEntityShadows.shouldForceRefresh(center);
        boolean scanElapsed = nowMs - lastStateScanMs >= intervalMs;
        boolean bl = captureElapsed = nowMs - lastCaptureMs >= intervalMs;
        if (!(levelChanged || centerChanged || scanElapsed || captureElapsed)) {
            return currentState;
        }
        long sceneFingerprint = lastSceneFingerprint;
        boolean bl2 = sceneChanged = levelChanged || centerChanged;
        if (sceneChanged || scanElapsed) {
            long nextSceneFingerprint = NeoCpuEntityShadows.scanDynamicSceneFingerprint(level, center);
            lastStateScanMs = nowMs;
            sceneChanged |= sceneFingerprint != nextSceneFingerprint;
            sceneFingerprint = nextSceneFingerprint;
        }
        lastSceneFingerprint = sceneFingerprint;
        if (!sceneChanged && !captureElapsed) {
            trackedLevel = level;
            lastCenter = center.m_7949_();
            return currentState;
        }
        double radius = LightRtMath.getEntityShadowRadius(15) + 30.0 + 4.0;
        DynamicShadowMeshCapture.Snapshot snapshot = DynamicShadowMeshCapture.capture(level, center, radius, 160, partialTick);
        currentState = NeoCpuEntityShadows.buildState(snapshot);
        trackedLevel = level;
        lastCenter = center.m_7949_();
        lastCaptureMs = nowMs;
        return currentState;
    }

    private static long scanDynamicSceneFingerprint(ClientLevel level, BlockPos center) {
        Vec3 centerPos = Vec3.m_82512_((Vec3i)center);
        double radius = LightRtMath.getEntityShadowRadius(15) + 30.0 + 4.0;
        double radiusSq = radius * radius;
        long hash = 1469598103934665603L;
        int casterCount = 0;
        for (Entity entity : level.m_104735_()) {
            AABB box;
            if (casterCount >= 160) {
                return NeoCpuEntityShadows.mixFingerprint(hash, casterCount);
            }
            if (entity == null || entity.m_213877_() || entity.m_20145_() || (box = entity.m_20191_()) == null || NeoCpuEntityShadows.distanceToAabbSq(centerPos, box) > radiusSq) continue;
            hash = NeoCpuEntityShadows.mixFingerprint(hash, entity.m_19879_());
            hash = NeoCpuEntityShadows.mixFingerprint(hash, entity.f_19797_);
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Float.floatToIntBits(entity.m_146908_()));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Float.floatToIntBits(entity.m_146909_()));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82288_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82289_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82290_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82291_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82292_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82293_));
            ++casterCount;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        BlockEntityRenderDispatcher dispatcher = minecraft.m_167982_();
        Vec3 cameraPos = minecraft.f_91063_.m_109153_().m_90583_();
        int chunkRadius = Math.max(1, (int)Math.ceil(radius / 16.0));
        int centerChunkX = center.m_123341_() >> 4;
        int centerChunkZ = center.m_123343_() >> 4;
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                if (casterCount >= 160) {
                    return NeoCpuEntityShadows.mixFingerprint(hash, casterCount);
                }
                LevelChunk chunk = level.m_7726_().m_7131_(centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                    BlockEntityRenderer renderer;
                    if (casterCount >= 160) {
                        return NeoCpuEntityShadows.mixFingerprint(hash, casterCount);
                    }
                    if (blockEntity == null || blockEntity.m_58901_() || (renderer = dispatcher.m_112265_(blockEntity)) == null || !renderer.m_142756_(blockEntity, cameraPos)) continue;
                    AABB box = blockEntity.getRenderBoundingBox();
                    if (box == null) {
                        box = new AABB(blockEntity.m_58899_());
                    }
                    if (NeoCpuEntityShadows.distanceToAabbSq(centerPos, box) > radiusSq) continue;
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, blockEntity.m_58899_().m_121878_());
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, blockEntity.m_58903_().hashCode());
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82288_));
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82289_));
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82290_));
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82291_));
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82292_));
                    hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(box.f_82293_));
                    ++casterCount;
                }
            }
        }
        return NeoCpuEntityShadows.mixFingerprint(hash, casterCount);
    }

    static void appendEntityShadows(NeoCpuFloodShadowBuilder builder, Vec3 source, int emission, CaptureState state, List<NeoCpuShadowTypes.ReceiverGroupEntry> receiverGroups, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        if (state.isEmpty() || receiverGroups.isEmpty() || polygons.size() >= 8192) {
            return;
        }
        double maxCasterDistance = LightRtMath.getEntityShadowRadius(emission);
        for (NeoCpuShadowTypes.ReceiverGroupEntry group : receiverGroups) {
            if (group.isEmpty() || polygons.size() >= 8192) {
                return;
            }
            NeoCpuShadowTypes.ReceiverSurface reference = group.referenceReceiver();
            if (!reference.facesSource(source.f_82479_, source.f_82480_, source.f_82481_)) continue;
            for (Caster caster : state.casters()) {
                if (polygons.size() >= 8192) {
                    return;
                }
                if (!caster.canAffect(source, maxCasterDistance)) continue;
                boolean addedQuadShadow = false;
                for (Vec3[] quad : caster.quads()) {
                    if (quad == null || quad.length < 4 || !NeoCpuEntityShadows.quadFacesSource(quad, source)) continue;
                    NeoCpuEntityShadows.appendProjectedOccluderShadow(builder, source, group, new NeoCpuShadowTypes.QuadOccluder(quad, caster.id(), 1.0f), polygons);
                    addedQuadShadow = true;
                    if (polygons.size() < 8192) continue;
                    return;
                }
                if (addedQuadShadow) continue;
                builder.appendProjectedOccluderShadowFromStaticGroup(source, group.receivers(), caster.box(), polygons);
            }
        }
    }

    private static void appendProjectedOccluderShadow(NeoCpuFloodShadowBuilder builder, Vec3 source, NeoCpuShadowTypes.ReceiverGroupEntry group, NeoCpuShadowTypes.Occluder occluder, ArrayList<NeoCpuShadowTypes.ShadowPolygon> polygons) {
        builder.appendProjectedOccluderShadowFromStaticGroup(source, group.receivers(), occluder, polygons);
    }

    private static CaptureState buildState(DynamicShadowMeshCapture.Snapshot snapshot) {
        if (snapshot.level == null || snapshot.casters.isEmpty()) {
            return CaptureState.empty();
        }
        ArrayList<Caster> casters = new ArrayList<Caster>(Math.min(snapshot.casters.size(), 160));
        long hash = 1469598103934665603L;
        for (DynamicShadowMeshCapture.CapturedCaster captured : snapshot.casters) {
            if (captured == null || captured.bounds == null || NeoCpuEntityShadows.isOversizedCaster(captured.bounds)) continue;
            List<Vec3[]> quads = captured.quads == null || captured.quads.isEmpty() ? Collections.emptyList() : List.copyOf(captured.quads);
            NeoCpuShadowTypes.Box box = new NeoCpuShadowTypes.Box(captured.bounds.f_82288_, captured.bounds.f_82289_, captured.bounds.f_82290_, captured.bounds.f_82291_, captured.bounds.f_82292_, captured.bounds.f_82293_, captured.id, 1.0f);
            casters.add(new Caster(captured.id, captured.bounds, box, quads));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, captured.id);
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82288_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82289_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82290_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82291_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82292_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(captured.bounds.f_82293_));
            hash = NeoCpuEntityShadows.mixFingerprint(hash, quads.size());
            for (Vec3[] quad : quads) {
                if (quad == null || quad.length == 0) continue;
                Vec3 first = quad[0];
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(first.f_82479_));
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(first.f_82480_));
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(first.f_82481_));
                if (quad.length <= 2) continue;
                Vec3 opposite = quad[2];
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82479_));
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82480_));
                hash = NeoCpuEntityShadows.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82481_));
            }
            if (casters.size() < 160) continue;
            break;
        }
        return casters.isEmpty() ? CaptureState.empty() : new CaptureState(hash, List.copyOf(casters));
    }

    private static boolean isOversizedCaster(AABB bounds) {
        double dx = bounds.m_82362_();
        double dy = bounds.m_82376_();
        double dz = bounds.m_82385_();
        if (dx > 12.0 || dy > 12.0 || dz > 12.0) {
            return true;
        }
        return dx * dy * dz > 512.0;
    }

    private static boolean shouldForceRefresh(BlockPos center) {
        BlockPos previousCenter = lastCenter;
        if (previousCenter == null) {
            return true;
        }
        return Math.abs(center.m_123341_() - previousCenter.m_123341_()) > 4 || Math.abs(center.m_123342_() - previousCenter.m_123342_()) > 4 || Math.abs(center.m_123343_() - previousCenter.m_123343_()) > 4;
    }

    private static long getUpdateIntervalMs() {
        int fps = Math.max(10, Math.min(120, (Integer)Config.CLIENT.entityShadowUpdateFpsLimit.get()));
        return Math.max(1L, 1000L / (long)fps);
    }

    private static long mixFingerprint(long hash, long value) {
        hash ^= value;
        return hash *= 1099511628211L;
    }

    private static boolean quadFacesSource(Vec3[] quad, Vec3 source) {
        Vec3 edgeB;
        Vec3 edgeA = quad[1].m_82546_(quad[0]);
        Vec3 normal = edgeA.m_82537_(edgeB = quad[3].m_82546_(quad[0]));
        if (normal.m_82556_() <= 1.0E-8) {
            return false;
        }
        Vec3 toSource = source.m_82546_(quad[0]);
        return normal.m_82526_(toSource) > 1.0E-6;
    }

    private static double distanceToAabbSq(Vec3 point, AABB box) {
        double dx = 0.0;
        if (point.f_82479_ < box.f_82288_) {
            dx = box.f_82288_ - point.f_82479_;
        } else if (point.f_82479_ > box.f_82291_) {
            dx = point.f_82479_ - box.f_82291_;
        }
        double dy = 0.0;
        if (point.f_82480_ < box.f_82289_) {
            dy = box.f_82289_ - point.f_82480_;
        } else if (point.f_82480_ > box.f_82292_) {
            dy = point.f_82480_ - box.f_82292_;
        }
        double dz = 0.0;
        if (point.f_82481_ < box.f_82290_) {
            dz = box.f_82290_ - point.f_82481_;
        } else if (point.f_82481_ > box.f_82293_) {
            dz = point.f_82481_ - box.f_82293_;
        }
        return dx * dx + dy * dy + dz * dz;
    }

    public record CaptureState(long fingerprint, List<Caster> casters) {
        private static final CaptureState EMPTY = new CaptureState(0L, Collections.emptyList());

        public static CaptureState empty() {
            return EMPTY;
        }

        public boolean isEmpty() {
            return this.casters.isEmpty();
        }
    }

    public record Caster(long id, AABB bounds, NeoCpuShadowTypes.Box box, List<Vec3[]> quads) {
        boolean canAffect(Vec3 source, double maxDistance) {
            double expanded = maxDistance + 2.0;
            return NeoCpuEntityShadows.distanceToAabbSq(source, this.bounds) <= expanded * expanded;
        }
    }
}

