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
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.client.NeoShadowsStaticPipeline;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
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

final class NeoShadowsDynamicPipeline {
    private final NeoShadowsEngine engine;

    NeoShadowsDynamicPipeline(NeoShadowsEngine engine) {
        this.engine = engine;
    }

    DynamicShadowMeshCapture.Snapshot captureDynamicCasterSnapshot(ClientLevel level, BlockPos center, float partialTick) {
        if (!GpuNeoShadows.entityShadowCastersEnabled()) {
            return DynamicShadowMeshCapture.Snapshot.empty();
        }
        double radius = 70.0;
        return DynamicShadowMeshCapture.capture(level, center, radius, 160, partialTick);
    }

    long scanDynamicSceneFingerprint(ClientLevel level, BlockPos center) {
        Vec3 centerPos = Vec3.m_82512_((Vec3i)center);
        double radius = 70.0;
        double radiusSq = radius * radius;
        long hash = 1469598103934665603L;
        int casterCount = 0;
        for (Entity entity : level.m_104735_()) {
            AABB box;
            if (casterCount >= 160) {
                return this.mixFingerprint(hash, casterCount);
            }
            if (entity == null || entity.m_213877_() || entity.m_20145_() || (box = entity.m_20191_()) == null || NeoShadowsDynamicPipeline.distanceToAabbSq(centerPos, box) > radiusSq) continue;
            hash = this.mixFingerprint(hash, entity.m_19879_());
            hash = this.mixFingerprint(hash, entity.f_19797_);
            hash = this.mixFingerprint(hash, Float.floatToIntBits(entity.m_146908_()));
            hash = this.mixFingerprint(hash, Float.floatToIntBits(entity.m_146909_()));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82288_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82289_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82290_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82291_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82292_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82293_));
            ++casterCount;
        }
        BlockEntityRenderDispatcher dispatcher = Minecraft.m_91087_().m_167982_();
        int chunkRadius = Math.max(1, (int)Math.ceil(radius / 16.0));
        int centerChunkX = center.m_123341_() >> 4;
        int centerChunkZ = center.m_123343_() >> 4;
        Vec3 cameraPos = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                if (casterCount >= 160) {
                    return this.mixFingerprint(hash, casterCount);
                }
                LevelChunk chunk = level.m_7726_().m_7131_(centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                    BlockEntityRenderer renderer;
                    if (casterCount >= 160) {
                        return this.mixFingerprint(hash, casterCount);
                    }
                    if (blockEntity == null || blockEntity.m_58901_() || (renderer = dispatcher.m_112265_(blockEntity)) == null || !renderer.m_142756_(blockEntity, cameraPos)) continue;
                    AABB box = blockEntity.getRenderBoundingBox();
                    if (box == null) {
                        box = new AABB(blockEntity.m_58899_());
                    }
                    if (NeoShadowsDynamicPipeline.distanceToAabbSq(centerPos, box) > radiusSq) continue;
                    hash = this.mixFingerprint(hash, blockEntity.m_58899_().m_121878_());
                    hash = this.mixFingerprint(hash, blockEntity.m_58903_().hashCode());
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82288_));
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82289_));
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82290_));
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82291_));
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82292_));
                    hash = this.mixFingerprint(hash, Double.doubleToLongBits(box.f_82293_));
                    ++casterCount;
                }
            }
        }
        return this.mixFingerprint(hash, casterCount);
    }

    long fingerprintDynamicSnapshot(DynamicShadowMeshCapture.Snapshot snapshot) {
        long hash = 1469598103934665603L;
        for (DynamicShadowMeshCapture.CapturedCaster caster : snapshot.casters) {
            hash = this.mixFingerprint(hash, caster.id);
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82288_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82289_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82290_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82291_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82292_));
            hash = this.mixFingerprint(hash, Double.doubleToLongBits(caster.bounds.f_82293_));
            hash = this.mixFingerprint(hash, caster.quads.size());
            for (Vec3[] quad : caster.quads) {
                if (quad == null || quad.length == 0) continue;
                Vec3 first = quad[0];
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(first.f_82479_));
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(first.f_82480_));
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(first.f_82481_));
                if (quad.length <= 2) continue;
                Vec3 opposite = quad[2];
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82479_));
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82480_));
                hash = this.mixFingerprint(hash, Double.doubleToLongBits(opposite.f_82481_));
            }
        }
        return hash;
    }

    long mixFingerprint(long hash, long value) {
        hash ^= value;
        return hash *= 1099511628211L;
    }

    List<NeoShadowsTypes.ShadowPolygon> buildRealtimeDynamicShadowPolygons(ClientLevel level, BlockPos center, @Nullable NeoShadowsTypes.RealtimeStaticShadowOutput output, DynamicShadowMeshCapture.Snapshot snapshot) {
        if (output == null || output.receiverPlaneGroups.isEmpty()) {
            return Collections.emptyList();
        }
        NeoShadowsTypes.GeometryBuildState state = new NeoShadowsTypes.GeometryBuildState(this.engine, level, center, ExposedFaceService.get(), true, snapshot);
        NeoShadowsTypes.BuildScratch scratch = new NeoShadowsTypes.BuildScratch();
        for (NeoShadowsTypes.ReceiverPlaneGroup group : output.receiverPlaneGroups) {
            if (scratch.shadowPolygons.size() >= 12288) break;
            this.addDynamicCasterShadows(group, state, scratch);
        }
        return scratch.shadowPolygons.isEmpty() ? Collections.emptyList() : List.copyOf(scratch.shadowPolygons);
    }

    void addDynamicCasterShadows(NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.GeometryBuildState state, NeoShadowsTypes.BuildScratch scratch) {
        double maxDist = Math.min(LightRtMath.getMaxDistance(group.sourceEmission), 30.0);
        for (NeoShadowsTypes.DynamicCaster caster : state.dynamicCasters) {
            List<NeoShadowsTypes.UvPoint> projected;
            List<NeoShadowsTypes.UvPoint> polygon;
            if (scratch.shadowPolygons.size() >= 12288) {
                return;
            }
            if (!caster.canAffect(group.source, state.center, maxDist) || !group.mayOverlapProjectedBounds(group.source, caster) || !this.blocksAnyReceiverSample(group, caster)) continue;
            if (!caster.quads.isEmpty()) {
                boolean added = false;
                for (Vec3[] quad : caster.quads) {
                    if (scratch.shadowPolygons.size() >= 12288) {
                        return;
                    }
                    if (!NeoShadowsStaticPipeline.facesSource(quad, group.source)) continue;
                    added |= this.engine.staticPipeline.addProjectedQuadShadow(group, quad, scratch);
                }
                if (added) {
                    ++scratch.occluderHitCount;
                    continue;
                }
            }
            if ((polygon = NeoShadowsProjection.buildProjectedShadowHullBuffered(projected = NeoShadowsProjection.collectProjectedBoxPoints(group.plane, group.source, caster.boxCorners, scratch.projectedPointBuf), scratch)) == null) continue;
            this.engine.staticPipeline.addPlaneShadowPolygons(group, polygon, scratch);
            ++scratch.occluderHitCount;
        }
    }

    boolean blocksAnyReceiverSample(NeoShadowsTypes.ReceiverPlaneGroup group, NeoShadowsTypes.DynamicCaster caster) {
        double[] samples = group.receiverSamples;
        for (int index = 0; index < samples.length; index += 3) {
            if (!caster.intersectsSegment(group.source.f_82479_, group.source.f_82480_, group.source.f_82481_, samples[index], samples[index + 1], samples[index + 2])) continue;
            return true;
        }
        return false;
    }

    List<NeoShadowsTypes.DynamicCaster> collectDynamicCasters(ClientLevel level, BlockPos center, DynamicShadowMeshCapture.Snapshot snapshot) {
        if (!GpuNeoShadows.entityShadowCastersEnabled()) {
            return Collections.emptyList();
        }
        if (snapshot.level == level && !snapshot.casters.isEmpty()) {
            ArrayList<NeoShadowsTypes.DynamicCaster> casters = new ArrayList<NeoShadowsTypes.DynamicCaster>(Math.min(snapshot.casters.size(), 160));
            for (DynamicShadowMeshCapture.CapturedCaster captured : snapshot.casters) {
                casters.add(new NeoShadowsTypes.DynamicCaster(captured.id, NeoShadowsTypes.Box.fromAabb(captured.bounds), captured.quads));
                if (casters.size() < 160) continue;
                break;
            }
            if (!casters.isEmpty()) {
                return casters;
            }
        }
        Vec3 centerPos = Vec3.m_82512_((Vec3i)center);
        double radius = 70.0;
        double radiusSq = radius * radius;
        ArrayList<NeoShadowsTypes.DynamicCaster> fallbackCasters = new ArrayList<NeoShadowsTypes.DynamicCaster>(160);
        long nextId = 1L;
        for (Entity entity : level.m_104735_()) {
            AABB box;
            if (fallbackCasters.size() >= 160) break;
            if (entity == null || entity.m_213877_() || entity.m_20145_() || (box = entity.m_20191_()) == null || NeoShadowsDynamicPipeline.distanceToAabbSq(centerPos, box) > radiusSq) continue;
            fallbackCasters.add(new NeoShadowsTypes.DynamicCaster(nextId++, NeoShadowsTypes.Box.fromAabb(box.m_82400_(0.03)), Collections.emptyList()));
        }
        return fallbackCasters;
    }

    static double distanceToAabbSq(Vec3 point, AABB box) {
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
}

