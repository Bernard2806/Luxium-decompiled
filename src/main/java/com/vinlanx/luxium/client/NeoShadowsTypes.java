/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsOcclusionPipeline;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsTypes {
    private NeoShadowsTypes() {
    }

    static final class DynamicResult {
        final long id;
        final long epoch;
        final long staticResultId;
        final BlockPos center;
        final List<ShadowPolygon> polygons;

        DynamicResult(long id, long epoch, long staticResultId, BlockPos center, List<ShadowPolygon> polygons) {
            this.id = id;
            this.epoch = epoch;
            this.staticResultId = staticResultId;
            this.center = center;
            this.polygons = polygons;
        }
    }

    static final class DynamicRequest {
        final long id;
        final long epoch;
        final long staticResultId;
        final ClientLevel level;
        final BlockPos center;
        final RealtimeStaticShadowOutput staticOutput;
        final DynamicShadowMeshCapture.Snapshot dynamicSnapshot;

        DynamicRequest(long id, long epoch, long staticResultId, ClientLevel level, BlockPos center, RealtimeStaticShadowOutput staticOutput, DynamicShadowMeshCapture.Snapshot dynamicSnapshot) {
            this.id = id;
            this.epoch = epoch;
            this.staticResultId = staticResultId;
            this.level = level;
            this.center = center;
            this.staticOutput = staticOutput;
            this.dynamicSnapshot = dynamicSnapshot;
        }
    }

    static final class DynamicCaptureRequest {
        final long id;
        final long epoch;
        final ClientLevel level;
        final BlockPos center;
        final float partialTick;
        final long fingerprint;

        DynamicCaptureRequest(long id, long epoch, ClientLevel level, BlockPos center, float partialTick, long fingerprint) {
            this.id = id;
            this.epoch = epoch;
            this.level = level;
            this.center = center;
            this.partialTick = partialTick;
            this.fingerprint = fingerprint;
        }
    }

    static final class StaticHotPatchResult {
        final long id;
        final long epoch;
        final long baseStaticResultId;
        final StaticHotPatchOutput output;

        StaticHotPatchResult(long id, long epoch, long baseStaticResultId, StaticHotPatchOutput output) {
            this.id = id;
            this.epoch = epoch;
            this.baseStaticResultId = baseStaticResultId;
            this.output = output;
        }
    }

    static final class StaticHotPatchRequest {
        final long id;
        final long epoch;
        final long baseStaticResultId;
        final ClientLevel level;
        final LongOpenHashSet changedBlocks;
        final int localRadius;

        StaticHotPatchRequest(long id, long epoch, long baseStaticResultId, ClientLevel level, LongOpenHashSet changedBlocks, int localRadius) {
            this.id = id;
            this.epoch = epoch;
            this.baseStaticResultId = baseStaticResultId;
            this.level = level;
            this.changedBlocks = changedBlocks;
            this.localRadius = localRadius;
        }
    }

    static final class StaticResult {
        final long id;
        final long epoch;
        final RealtimeStaticShadowOutput output;

        StaticResult(long id, long epoch, RealtimeStaticShadowOutput output) {
            this.id = id;
            this.epoch = epoch;
            this.output = output;
        }
    }

    static final class StaticRequest {
        final long id;
        final long epoch;
        final ClientLevel level;
        final BlockPos center;

        StaticRequest(long id, long epoch, ClientLevel level, BlockPos center) {
            this.id = id;
            this.epoch = epoch;
            this.level = level;
            this.center = center;
        }
    }

    static final class Box {
        final double minX;
        final double minY;
        final double minZ;
        final double maxX;
        final double maxY;
        final double maxZ;

        Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        static Box fromAabb(AABB aabb) {
            return new Box(aabb.f_82288_, aabb.f_82289_, aabb.f_82290_, aabb.f_82291_, aabb.f_82292_, aabb.f_82293_);
        }

        Vec3[] corners() {
            return this.cornersWithOffset(0, 0, 0);
        }

        boolean contains(double x, double y, double z) {
            return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
        }

        double distanceToPointSq(double x, double y, double z) {
            double dx = 0.0;
            if (x < this.minX) {
                dx = this.minX - x;
            } else if (x > this.maxX) {
                dx = x - this.maxX;
            }
            double dy = 0.0;
            if (y < this.minY) {
                dy = this.minY - y;
            } else if (y > this.maxY) {
                dy = y - this.maxY;
            }
            double dz = 0.0;
            if (z < this.minZ) {
                dz = this.minZ - z;
            } else if (z > this.maxZ) {
                dz = z - this.maxZ;
            }
            return dx * dx + dy * dy + dz * dz;
        }

        Vec3[] cornersWithOffset(int ox, int oy, int oz) {
            double ax = this.minX + (double)ox;
            double bx = this.maxX + (double)ox;
            double ay = this.minY + (double)oy;
            double by = this.maxY + (double)oy;
            double az = this.minZ + (double)oz;
            double bz = this.maxZ + (double)oz;
            return new Vec3[]{new Vec3(ax, ay, az), new Vec3(ax, ay, bz), new Vec3(ax, by, az), new Vec3(ax, by, bz), new Vec3(bx, ay, az), new Vec3(bx, ay, bz), new Vec3(bx, by, az), new Vec3(bx, by, bz)};
        }

        boolean intersectsSegment(double startX, double startY, double startZ, double endX, double endY, double endZ) {
            return this.intersectsSegmentWithOffset(startX, startY, startZ, endX, endY, endZ, 0, 0, 0);
        }

        boolean intersectsSegmentWithOffset(double startX, double startY, double startZ, double endX, double endY, double endZ, int offsetX, int offsetY, int offsetZ) {
            double oMinX = this.minX + (double)offsetX;
            double oMaxX = this.maxX + (double)offsetX;
            double oMinY = this.minY + (double)offsetY;
            double oMaxY = this.maxY + (double)offsetY;
            double oMinZ = this.minZ + (double)offsetZ;
            double oMaxZ = this.maxZ + (double)offsetZ;
            double dirX = endX - startX;
            double dirY = endY - startY;
            double dirZ = endZ - startZ;
            double tMin = 0.0;
            double tMax = 1.0;
            if (Double.isNaN(tMin = Box.clipAxis(startX, dirX, oMinX, oMaxX, tMin, true))) {
                return false;
            }
            if (Double.isNaN(tMax = Box.clipAxis(startX, dirX, oMinX, oMaxX, tMax, false)) || tMax < tMin) {
                return false;
            }
            if (Double.isNaN(tMin = Box.clipAxis(startY, dirY, oMinY, oMaxY, tMin, true))) {
                return false;
            }
            if (Double.isNaN(tMax = Box.clipAxis(startY, dirY, oMinY, oMaxY, tMax, false)) || tMax < tMin) {
                return false;
            }
            if (Double.isNaN(tMin = Box.clipAxis(startZ, dirZ, oMinZ, oMaxZ, tMin, true))) {
                return false;
            }
            return !Double.isNaN(tMax = Box.clipAxis(startZ, dirZ, oMinZ, oMaxZ, tMax, false)) && tMax >= tMin;
        }

        private static double clipAxis(double start, double dir, double min, double max, double current, boolean minBound) {
            if (Math.abs(dir) <= 1.0E-6) {
                if (start < min || start > max) {
                    return Double.NaN;
                }
                return current;
            }
            double inverse = 1.0 / dir;
            double t1 = (min - start) * inverse;
            double t2 = (max - start) * inverse;
            double lower = Math.min(t1, t2);
            double upper = Math.max(t1, t2);
            return minBound ? Math.max(current, lower) : Math.min(current, upper);
        }
    }

    static final class TexturedQuad {
        final Vec3[] worldVertices;
        final AtlasUv[] atlasUvs;
        final Vec3 normal;

        TexturedQuad(Vec3[] worldVertices, AtlasUv[] atlasUvs, Vec3 normal) {
            this.worldVertices = worldVertices;
            this.atlasUvs = atlasUvs;
            this.normal = normal;
        }

        @Nullable
        static TexturedQuad from(BakedQuad quad, BlockPos blockPos) {
            int[] vertices = quad.m_111303_();
            if (vertices.length < 32) {
                return null;
            }
            Vec3[] worldVertices = new Vec3[4];
            AtlasUv[] atlasUvs = new AtlasUv[4];
            for (int vertexIndex = 0; vertexIndex < 4; ++vertexIndex) {
                int baseIndex = vertexIndex * 8;
                double x = Float.intBitsToFloat(vertices[baseIndex]) + (float)blockPos.m_123341_();
                double y = Float.intBitsToFloat(vertices[baseIndex + 1]) + (float)blockPos.m_123342_();
                double z = Float.intBitsToFloat(vertices[baseIndex + 2]) + (float)blockPos.m_123343_();
                double u = Float.intBitsToFloat(vertices[baseIndex + 4]);
                double v = Float.intBitsToFloat(vertices[baseIndex + 5]);
                worldVertices[vertexIndex] = new Vec3(x, y, z);
                atlasUvs[vertexIndex] = new AtlasUv(u, v);
            }
            Vec3 normal = worldVertices[1].m_82546_(worldVertices[0]).m_82537_(worldVertices[2].m_82546_(worldVertices[0]));
            if (normal.m_82556_() <= 1.0E-12) {
                return null;
            }
            return new TexturedQuad(worldVertices, atlasUvs, normal);
        }

        boolean facesSource(Vec3 source) {
            Vec3 toSource = source.m_82546_(this.worldVertices[0]);
            return this.normal.m_82526_(toSource) > 1.0E-6;
        }
    }

    static final class SpritePixelBounds {
        final double minX;
        final double maxX;
        final double minY;
        final double maxY;

        SpritePixelBounds(double minX, double maxX, double minY, double maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    static final class SpriteOpaqueMask {
        static final SpriteOpaqueMask FULLY_OPAQUE = new SpriteOpaqueMask(new SpriteRect[0], true, false);
        static final SpriteOpaqueMask FULLY_TRANSPARENT = new SpriteOpaqueMask(new SpriteRect[0], false, true);
        final SpriteRect[] rectangles;
        final boolean fullyOpaque;
        final boolean fullyTransparent;

        SpriteOpaqueMask(SpriteRect[] rectangles, boolean fullyOpaque, boolean fullyTransparent) {
            this.rectangles = rectangles;
            this.fullyOpaque = fullyOpaque;
            this.fullyTransparent = fullyTransparent;
        }

        static SpriteOpaqueMask fullyOpaque() {
            return FULLY_OPAQUE;
        }

        static SpriteOpaqueMask fullyTransparent() {
            return FULLY_TRANSPARENT;
        }
    }

    static final class SpriteRect {
        final int minX;
        final int maxX;
        final int minY;
        final int maxY;

        SpriteRect(int minX, int maxX, int minY, int maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    static final class AtlasUv {
        final double u;
        final double v;

        AtlasUv(double u, double v) {
            this.u = u;
            this.v = v;
        }
    }

    static final class UvPoint {
        final double u;
        final double v;

        UvPoint(double u, double v) {
            this.u = u;
            this.v = v;
        }

        boolean equalsApprox(UvPoint other) {
            return Math.abs(this.u - other.u) <= 1.0E-6 && Math.abs(this.v - other.v) <= 1.0E-6;
        }
    }

    static final class ProjectionPoint {
        final double denominator;
        final double t;
        @Nullable
        final UvPoint uv;

        ProjectionPoint(double denominator, double t, @Nullable UvPoint uv) {
            this.denominator = denominator;
            this.t = t;
            this.uv = uv;
        }
    }

    static final class GlobalReceiverPlane {
        final Direction face;
        final Vec3 origin;
        final Vec3 axisU;
        final Vec3 axisV;
        final Vec3 normal;

        GlobalReceiverPlane(Direction face, double planeCoordinate) {
            this.face = face;
            double epsilon = 0.0015f;
            switch (face) {
                case UP: {
                    this.origin = new Vec3(0.0, planeCoordinate + epsilon, 0.0);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 0.0, 1.0);
                    this.normal = new Vec3(0.0, 1.0, 0.0);
                    break;
                }
                case DOWN: {
                    this.origin = new Vec3(0.0, planeCoordinate - epsilon, 0.0);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 0.0, 1.0);
                    this.normal = new Vec3(0.0, -1.0, 0.0);
                    break;
                }
                case NORTH: {
                    this.origin = new Vec3(0.0, 0.0, planeCoordinate - epsilon);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(0.0, 0.0, -1.0);
                    break;
                }
                case SOUTH: {
                    this.origin = new Vec3(0.0, 0.0, planeCoordinate + epsilon);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(0.0, 0.0, 1.0);
                    break;
                }
                case WEST: {
                    this.origin = new Vec3(planeCoordinate - epsilon, 0.0, 0.0);
                    this.axisU = new Vec3(0.0, 0.0, 1.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(-1.0, 0.0, 0.0);
                    break;
                }
                default: {
                    this.origin = new Vec3(planeCoordinate + epsilon, 0.0, 0.0);
                    this.axisU = new Vec3(0.0, 0.0, 1.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(1.0, 0.0, 0.0);
                }
            }
        }

        boolean isFacingSource(Vec3 source) {
            double dx = source.f_82479_ - this.origin.f_82479_;
            double dy = source.f_82480_ - this.origin.f_82480_;
            double dz = source.f_82481_ - this.origin.f_82481_;
            return dx * this.normal.f_82479_ + dy * this.normal.f_82480_ + dz * this.normal.f_82481_ > 1.0E-6;
        }

        double sampleX(double u, double v) {
            return this.origin.f_82479_ + this.axisU.f_82479_ * u + this.axisV.f_82479_ * v;
        }

        double sampleY(double u, double v) {
            return this.origin.f_82480_ + this.axisU.f_82480_ * u + this.axisV.f_82480_ * v;
        }

        double sampleZ(double u, double v) {
            return this.origin.f_82481_ + this.axisU.f_82481_ * u + this.axisV.f_82481_ * v;
        }

        @Nullable
        UvPoint projectFinite(Vec3 source, Vec3 point) {
            ProjectionPoint projection = this.projectPoint(source, point);
            return projection.uv;
        }

        ProjectionPoint projectPoint(Vec3 source, Vec3 point) {
            double minProjectionT;
            double dirX = point.f_82479_ - source.f_82479_;
            double dirY = point.f_82480_ - source.f_82480_;
            double dirZ = point.f_82481_ - source.f_82481_;
            double denominator = dirX * this.normal.f_82479_ + dirY * this.normal.f_82480_ + dirZ * this.normal.f_82481_;
            if (Math.abs(denominator) <= 1.0E-9) {
                return new ProjectionPoint(denominator, Double.NaN, null);
            }
            double odX = this.origin.f_82479_ - source.f_82479_;
            double odY = this.origin.f_82480_ - source.f_82480_;
            double odZ = this.origin.f_82481_ - source.f_82481_;
            double t = (odX * this.normal.f_82479_ + odY * this.normal.f_82480_ + odZ * this.normal.f_82481_) / denominator;
            double d = minProjectionT = GlobalReceiverPlane.isHorizontalFace(this.face) ? 1.0E-6 : 1.000001;
            if (!Double.isFinite(t) || t <= minProjectionT) {
                return new ProjectionPoint(denominator, t, null);
            }
            double px = source.f_82479_ + dirX * t;
            double py = source.f_82480_ + dirY * t;
            double pz = source.f_82481_ + dirZ * t;
            if (!(Double.isFinite(px) && Double.isFinite(py) && Double.isFinite(pz))) {
                return new ProjectionPoint(denominator, t, null);
            }
            double lx = px - this.origin.f_82479_;
            double ly = py - this.origin.f_82480_;
            double lz = pz - this.origin.f_82481_;
            return new ProjectionPoint(denominator, t, new UvPoint(lx * this.axisU.f_82479_ + ly * this.axisU.f_82480_ + lz * this.axisU.f_82481_, lx * this.axisV.f_82479_ + ly * this.axisV.f_82480_ + lz * this.axisV.f_82481_));
        }

        private static boolean isHorizontalFace(Direction face) {
            return face == Direction.UP || face == Direction.DOWN;
        }
    }

    static final class ReceiverPlane {
        final Vec3 origin;
        final Vec3 axisU;
        final Vec3 axisV;
        final Vec3 normal;

        ReceiverPlane(BlockPos pos, Direction face, double planeCoordinate) {
            double minX = pos.m_123341_();
            double maxX = minX + 1.0;
            double minY = pos.m_123342_();
            double minZ = pos.m_123343_();
            double epsilon = 0.0015f;
            switch (face) {
                case UP: {
                    this.origin = new Vec3(minX, planeCoordinate + epsilon, minZ);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 0.0, 1.0);
                    this.normal = new Vec3(0.0, 1.0, 0.0);
                    break;
                }
                case DOWN: {
                    this.origin = new Vec3(minX, planeCoordinate - epsilon, minZ + 1.0);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 0.0, -1.0);
                    this.normal = new Vec3(0.0, -1.0, 0.0);
                    break;
                }
                case NORTH: {
                    this.origin = new Vec3(maxX, minY, planeCoordinate - epsilon);
                    this.axisU = new Vec3(-1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(0.0, 0.0, -1.0);
                    break;
                }
                case SOUTH: {
                    this.origin = new Vec3(minX, minY, planeCoordinate + epsilon);
                    this.axisU = new Vec3(1.0, 0.0, 0.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(0.0, 0.0, 1.0);
                    break;
                }
                case WEST: {
                    this.origin = new Vec3(planeCoordinate - epsilon, minY, minZ + 1.0);
                    this.axisU = new Vec3(0.0, 0.0, -1.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(-1.0, 0.0, 0.0);
                    break;
                }
                default: {
                    this.origin = new Vec3(planeCoordinate + epsilon, minY, minZ);
                    this.axisU = new Vec3(0.0, 0.0, 1.0);
                    this.axisV = new Vec3(0.0, 1.0, 0.0);
                    this.normal = new Vec3(1.0, 0.0, 0.0);
                }
            }
        }

        double sampleX(double u, double v) {
            return this.origin.f_82479_ + this.axisU.f_82479_ * u + this.axisV.f_82479_ * v;
        }

        double sampleY(double u, double v) {
            return this.origin.f_82480_ + this.axisU.f_82480_ * u + this.axisV.f_82480_ * v;
        }

        double sampleZ(double u, double v) {
            return this.origin.f_82481_ + this.axisU.f_82481_ * u + this.axisV.f_82481_ * v;
        }
    }

    static final class PlaneRect {
        double minU;
        double maxU;
        double minV;
        double maxV;

        PlaneRect(double minU, double maxU, double minV, double maxV) {
            this.minU = minU;
            this.maxU = maxU;
            this.minV = minV;
            this.maxV = maxV;
        }

        void include(PlaneRect other) {
            this.minU = Math.min(this.minU, other.minU);
            this.maxU = Math.max(this.maxU, other.maxU);
            this.minV = Math.min(this.minV, other.minV);
            this.maxV = Math.max(this.maxV, other.maxV);
        }
    }

    static final class ReceiverContributionRect {
        final BlockPos anchorPos;
        final PlaneRect planeRect;
        final float strength;

        ReceiverContributionRect(BlockPos anchorPos, PlaneRect planeRect, float strength) {
            this.anchorPos = anchorPos;
            this.planeRect = planeRect;
            this.strength = strength;
        }
    }

    static final class ReceiverPlaneGroup {
        final ReceiverPlaneKey key;
        final GlobalReceiverPlane plane;
        final Vec3 source;
        final int sourceEmission;
        final List<FaceCandidate> faces = new ArrayList<FaceCandidate>();
        final Long2ObjectOpenHashMap<FaceCandidate> receiverFacesByKey = new Long2ObjectOpenHashMap();
        final Long2FloatOpenHashMap faceWeights = new Long2FloatOpenHashMap();
        final Long2ObjectOpenHashMap<PlaneRect> faceContributionBounds = new Long2ObjectOpenHashMap();
        double[] receiverSamples = new double[0];
        double nearestReceiverDistanceSq = Double.POSITIVE_INFINITY;
        int minReceiverCellU = Integer.MAX_VALUE;
        int maxReceiverCellU = Integer.MIN_VALUE;
        int minReceiverCellV = Integer.MAX_VALUE;
        int maxReceiverCellV = Integer.MIN_VALUE;
        @Nullable
        List<ReceiverContributionRect> mergedReceiverRects;

        ReceiverPlaneGroup(ReceiverPlaneKey key, Vec3 source, int sourceEmission) {
            this.key = key;
            this.plane = new GlobalReceiverPlane(key.face, key.planeCoordinate);
            this.source = source;
            this.sourceEmission = sourceEmission;
            this.faceWeights.defaultReturnValue(0.0f);
        }

        void addFace(FaceCandidate candidate, float weight, PlaneRect localFaceBounds) {
            PlaneRect existingBounds;
            this.faces.add(candidate);
            this.mergedReceiverRects = null;
            long receiverKey = candidate.receiverKey;
            this.receiverFacesByKey.putIfAbsent(receiverKey, (Object)candidate);
            float existing = this.faceWeights.get(receiverKey);
            if (weight > existing) {
                this.faceWeights.put(receiverKey, weight);
            }
            if ((existingBounds = (PlaneRect)this.faceContributionBounds.get(receiverKey)) == null) {
                this.faceContributionBounds.put(receiverKey, (Object)new PlaneRect(localFaceBounds.minU, localFaceBounds.maxU, localFaceBounds.minV, localFaceBounds.maxV));
            } else {
                existingBounds.include(localFaceBounds);
            }
            this.nearestReceiverDistanceSq = Math.min(this.nearestReceiverDistanceSq, candidate.distanceSq);
            int cellU = NeoShadowsProjection.blockPosToPlaneCellU(this.key.face, candidate.pos);
            int cellV = NeoShadowsProjection.blockPosToPlaneCellV(this.key.face, candidate.pos);
            this.minReceiverCellU = Math.min(this.minReceiverCellU, cellU);
            this.maxReceiverCellU = Math.max(this.maxReceiverCellU, cellU);
            this.minReceiverCellV = Math.min(this.minReceiverCellV, cellV);
            this.maxReceiverCellV = Math.max(this.maxReceiverCellV, cellV);
        }

        void addFace(FaceCandidate candidate, SourceContribution contribution) {
            this.addFace(candidate, contribution.weight, contribution.localFaceBounds);
        }

        void addFace(FaceCandidate candidate) {
            this.addFace(candidate, 0.0f, candidate.localBounds);
        }

        float weightForReceiverPos(BlockPos pos) {
            return this.faceWeights.get(pos.m_121878_());
        }

        boolean containsReceiverFace(BlockPos pos) {
            return this.receiverFacesByKey.containsKey(pos.m_121878_());
        }

        PlaneRect localContributionBoundsForReceiverPos(BlockPos pos) {
            PlaneRect bounds = (PlaneRect)this.faceContributionBounds.get(pos.m_121878_());
            return bounds != null ? bounds : NeoShadowsProjection.fullFaceRect();
        }

        PlaneRect localContributionBoundsForReceiverPosKey(long posKey) {
            PlaneRect bounds = (PlaneRect)this.faceContributionBounds.get(posKey);
            return bounds != null ? bounds : NeoShadowsProjection.fullFaceRect();
        }

        boolean mayOverlapProjectedBounds(Vec3 source, DynamicCaster caster) {
            double minU = Double.POSITIVE_INFINITY;
            double maxU = Double.NEGATIVE_INFINITY;
            double minV = Double.POSITIVE_INFINITY;
            double maxV = Double.NEGATIVE_INFINITY;
            int validCount = 0;
            for (Vec3 corner : caster.boxCorners) {
                ProjectionPoint projection = this.plane.projectPoint(source, corner);
                if (projection.uv == null) continue;
                ++validCount;
                minU = Math.min(minU, projection.uv.u);
                maxU = Math.max(maxU, projection.uv.u);
                minV = Math.min(minV, projection.uv.v);
                maxV = Math.max(maxV, projection.uv.v);
            }
            if (validCount == 0) {
                return true;
            }
            return maxU > (double)this.minReceiverCellU && minU < (double)this.maxReceiverCellU + 1.0 && maxV > (double)this.minReceiverCellV && minV < (double)this.maxReceiverCellV + 1.0;
        }

        List<ReceiverContributionRect> receiverRects() {
            if (this.mergedReceiverRects == null) {
                this.mergedReceiverRects = this.buildReceiverRects();
            }
            return this.mergedReceiverRects;
        }

        private List<ReceiverContributionRect> buildReceiverRects() {
            float strength;
            if (this.receiverFacesByKey.isEmpty()) {
                return Collections.emptyList();
            }
            Long2LongOpenHashMap fullFacePosByCell = new Long2LongOpenHashMap(this.receiverFacesByKey.size());
            LongOpenHashSet visitedCells = new LongOpenHashSet(this.receiverFacesByKey.size());
            ArrayList<ReceiverContributionRect> rects = new ArrayList<ReceiverContributionRect>(this.receiverFacesByKey.size());
            fullFacePosByCell.defaultReturnValue(Long.MIN_VALUE);
            for (Long2ObjectMap.Entry entry : this.receiverFacesByKey.long2ObjectEntrySet()) {
                long receiverKey = entry.getLongKey();
                FaceCandidate candidate = (FaceCandidate)entry.getValue();
                PlaneRect localBounds = this.localContributionBoundsForReceiverPosKey(receiverKey);
                strength = this.faceWeights.get(receiverKey);
                if (strength <= 0.0f) continue;
                BlockPos pos = candidate.pos;
                int cellU = NeoShadowsProjection.blockPosToPlaneCellU(this.key.face, pos);
                int cellV = NeoShadowsProjection.blockPosToPlaneCellV(this.key.face, pos);
                long cellKey = NeoShadowsProjection.receiverCellKey(cellU, cellV);
                if (!NeoShadowsProjection.isMergeableReceiverBounds(localBounds) || !NeoShadowsProjection.isMergeablePlaneCoordinate(this.key.planeCoordinate)) {
                    rects.add(new ReceiverContributionRect(pos, NeoShadowsProjection.faceContributionPlaneRect(this.key.face, pos, localBounds), strength));
                    visitedCells.add(cellKey);
                    continue;
                }
                fullFacePosByCell.put(cellKey, receiverKey);
            }
            for (Long2ObjectMap.Entry entry : fullFacePosByCell.long2LongEntrySet()) {
                long nextCellKey;
                long nextPosKey;
                long startCellKey = entry.getLongKey();
                if (!visitedCells.add(startCellKey)) continue;
                long startPosKey = entry.getLongValue();
                strength = this.faceWeights.get(startPosKey);
                int startCellU = NeoShadowsProjection.unpackReceiverCellU(startCellKey);
                int startCellV = NeoShadowsProjection.unpackReceiverCellV(startCellKey);
                int width = 1;
                while ((nextPosKey = fullFacePosByCell.get(nextCellKey = NeoShadowsProjection.receiverCellKey(startCellU + width, startCellV))) != Long.MIN_VALUE && !visitedCells.contains(nextCellKey) && NeoShadowsProjection.sameReceiverStrength(strength, this.faceWeights.get(nextPosKey))) {
                    ++width;
                }
                int height = 1;
                while (true) {
                    int nextV = startCellV + height;
                    boolean rowMatches = true;
                    for (int offsetU = 0; offsetU < width; ++offsetU) {
                        long nextCellKey2 = NeoShadowsProjection.receiverCellKey(startCellU + offsetU, nextV);
                        long nextPosKey2 = fullFacePosByCell.get(nextCellKey2);
                        if (nextPosKey2 != Long.MIN_VALUE && !visitedCells.contains(nextCellKey2) && NeoShadowsProjection.sameReceiverStrength(strength, this.faceWeights.get(nextPosKey2))) continue;
                        rowMatches = false;
                        break;
                    }
                    if (!rowMatches) break;
                    ++height;
                }
                for (int offsetV = 0; offsetV < height; ++offsetV) {
                    for (int offsetU = 0; offsetU < width; ++offsetU) {
                        visitedCells.add(NeoShadowsProjection.receiverCellKey(startCellU + offsetU, startCellV + offsetV));
                    }
                }
                BlockPos anchorPos = NeoShadowsProjection.planeCellToBlockPos(this.key, startCellU, startCellV);
                rects.add(new ReceiverContributionRect(anchorPos, new PlaneRect(startCellU, startCellU + width, startCellV, startCellV + height), strength));
            }
            return rects;
        }

        void finalizeReceiverSamples() {
            double maxDistance = Math.min(LightRtMath.getMaxDistance(this.sourceEmission), 30.0);
            double maxDistanceSq = maxDistance * maxDistance;
            int faceCount = this.faces.size();
            if (faceCount == 0) {
                this.receiverSamples = NeoShadowsProjection.EMPTY_DOUBLE_ARRAY;
                return;
            }
            double[] scratch = NeoShadowsProjection.SCRATCH_RECEIVER_SAMPLES.get();
            int writeIndex = 0;
            double sx0 = this.source.f_82479_;
            double sy0 = this.source.f_82480_;
            double sz0 = this.source.f_82481_;
            int emission = this.sourceEmission;
            for (FaceCandidate candidate : this.faces) {
                ReceiverPlane facePlane = new ReceiverPlane(candidate.pos, candidate.face, candidate.planeCoordinate);
                PlaneRect localBounds = this.localContributionBoundsForReceiverPosKey(candidate.receiverKey);
                UvPoint[] probes = NeoShadowsProjection.buildOcclusionFaceProbes(candidate.face);
                int needed = writeIndex + probes.length * 3;
                if (needed > scratch.length) {
                    int newLength = Math.max(needed, scratch.length * 2);
                    scratch = Arrays.copyOf(scratch, newLength);
                    NeoShadowsProjection.SCRATCH_RECEIVER_SAMPLES.set(scratch);
                }
                for (UvPoint probe : probes) {
                    double distance;
                    double sz;
                    double dz;
                    double sy;
                    double dy;
                    double sx;
                    double dx;
                    double distanceSq;
                    if (!NeoShadowsProjection.isProbeInsideRect(probe, localBounds) || (distanceSq = (dx = (sx = facePlane.sampleX(probe.u, probe.v)) - sx0) * dx + (dy = (sy = facePlane.sampleY(probe.u, probe.v)) - sy0) * dy + (dz = (sz = facePlane.sampleZ(probe.u, probe.v)) - sz0) * dz) > maxDistanceSq || LightRtMath.getFalloff(emission, distance = Math.sqrt(distanceSq)) <= 0) continue;
                    scratch[writeIndex++] = sx;
                    scratch[writeIndex++] = sy;
                    scratch[writeIndex++] = sz;
                }
            }
            if (writeIndex == 0) {
                this.receiverSamples = NeoShadowsProjection.EMPTY_DOUBLE_ARRAY;
                return;
            }
            double[] result = new double[writeIndex];
            System.arraycopy(scratch, 0, result, 0, writeIndex);
            this.receiverSamples = result;
        }
    }

    static final class ReceiverPlaneKey {
        final Direction face;
        final double planeCoordinate;
        final long sourceKey;

        private ReceiverPlaneKey(Direction face, double planeCoordinate, long sourceKey) {
            this.face = face;
            this.planeCoordinate = planeCoordinate;
            this.sourceKey = sourceKey;
        }

        static ReceiverPlaneKey of(FaceCandidate candidate, long sourceKey) {
            return new ReceiverPlaneKey(candidate.face, candidate.planeCoordinate, sourceKey);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ReceiverPlaneKey)) {
                return false;
            }
            ReceiverPlaneKey key = (ReceiverPlaneKey)other;
            return this.face == key.face && Double.compare(this.planeCoordinate, key.planeCoordinate) == 0 && this.sourceKey == key.sourceKey;
        }

        public int hashCode() {
            int result = this.face.hashCode();
            result = 31 * result + Double.hashCode(this.planeCoordinate);
            result = 31 * result + Long.hashCode(this.sourceKey);
            return result;
        }
    }

    static final class ContributorScoringVisitor
    implements NeoFloodEngine.ContributorVisitor {
        private Long2ObjectOpenHashMap<SourceScore> scoresBySource;
        private long ownPosKey;
        private double sampleX;
        private double sampleY;
        private double sampleZ;
        private long lightSamplePosKey;
        private PlaneRect probeRect;

        ContributorScoringVisitor() {
        }

        void bind(Long2ObjectOpenHashMap<SourceScore> scoresBySource, long ownPosKey) {
            this.scoresBySource = scoresBySource;
            this.ownPosKey = ownPosKey;
        }

        void setProbe(double sampleX, double sampleY, double sampleZ, long lightSamplePosKey, PlaneRect probeRect) {
            this.sampleX = sampleX;
            this.sampleY = sampleY;
            this.sampleZ = sampleZ;
            this.lightSamplePosKey = lightSamplePosKey;
            this.probeRect = probeRect;
        }

        @Override
        public void visit(long sourceKey, int light, int coverage, int sourceEmission) {
            double denom;
            if (sourceKey == this.ownPosKey || sourceEmission <= 0 || light <= 0) {
                return;
            }
            double dx = (double)BlockPos.m_121983_((long)sourceKey) + 0.5 - this.sampleX;
            double dy = (double)BlockPos.m_122008_((long)sourceKey) + 0.5 - this.sampleY;
            double dz = (double)BlockPos.m_122015_((long)sourceKey) + 0.5 - this.sampleZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            SourceScore score = (SourceScore)this.scoresBySource.get(sourceKey);
            if (score == null) {
                score = new SourceScore(sourceKey, this.lightSamplePosKey, sourceEmission);
                this.scoresBySource.put(sourceKey, (Object)score);
            }
            ++score.sampleCount;
            if (sourceEmission > score.bestEmission) {
                score.bestEmission = sourceEmission;
            }
            if (distanceSq < score.bestDistanceSq) {
                score.bestDistanceSq = distanceSq;
                score.representativeSamplePosKey = this.lightSamplePosKey;
            }
            score.includeProbeBounds(this.probeRect);
            double coverageFactor = (double)coverage / 255.0;
            if (coverageFactor < 0.15) {
                coverageFactor = 0.15;
            }
            if ((denom = distanceSq + 0.25) < 0.25) {
                denom = 0.25;
            }
            score.totalScore += (double)light * coverageFactor / denom;
        }
    }

    static final class SourceScore {
        final long sourceKey;
        long representativeSamplePosKey;
        int bestEmission;
        int sampleCount;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        double totalScore;
        double minProbeU = Double.POSITIVE_INFINITY;
        double maxProbeU = Double.NEGATIVE_INFINITY;
        double minProbeV = Double.POSITIVE_INFINITY;
        double maxProbeV = Double.NEGATIVE_INFINITY;

        SourceScore(long sourceKey, long representativeSamplePosKey, int bestEmission) {
            this.sourceKey = sourceKey;
            this.representativeSamplePosKey = representativeSamplePosKey;
            this.bestEmission = bestEmission;
        }

        void includeProbeBounds(PlaneRect rect) {
            this.minProbeU = Math.min(this.minProbeU, rect.minU);
            this.maxProbeU = Math.max(this.maxProbeU, rect.maxU);
            this.minProbeV = Math.min(this.minProbeV, rect.minV);
            this.maxProbeV = Math.max(this.maxProbeV, rect.maxV);
        }

        PlaneRect contributionBounds() {
            if (!(Double.isFinite(this.minProbeU) && Double.isFinite(this.minProbeV) && Double.isFinite(this.maxProbeU) && Double.isFinite(this.maxProbeV))) {
                return NeoShadowsProjection.fullFaceRect();
            }
            return new PlaneRect(this.minProbeU, this.maxProbeU, this.minProbeV, this.maxProbeV);
        }
    }

    static final class SourceContribution {
        final long sourceKey;
        final long lightSamplePosKey;
        final int sourceEmission;
        final float weight;
        final PlaneRect localFaceBounds;

        SourceContribution(long sourceKey, long lightSamplePosKey, int sourceEmission, float weight, PlaneRect localFaceBounds) {
            this.sourceKey = sourceKey;
            this.lightSamplePosKey = lightSamplePosKey;
            this.sourceEmission = sourceEmission;
            this.weight = weight;
            this.localFaceBounds = localFaceBounds;
        }
    }

    static final class FaceLightingInfo {
        final long lightSamplePosKey;
        final long sourceKey;
        final int sourceEmission;
        final List<SourceContribution> contributions;

        FaceLightingInfo(long lightSamplePosKey, long sourceKey, int sourceEmission, List<SourceContribution> contributions) {
            this.lightSamplePosKey = lightSamplePosKey;
            this.sourceKey = sourceKey;
            this.sourceEmission = sourceEmission;
            this.contributions = contributions;
        }

        static FaceLightingInfo empty() {
            return new FaceLightingInfo(Long.MIN_VALUE, Long.MIN_VALUE, 0, Collections.emptyList());
        }
    }

    static final class FaceCandidate {
        final BlockPos pos;
        final Direction face;
        final double distanceSq;
        final double planeCoordinate;
        final PlaneRect localBounds;
        final long receiverKey;

        FaceCandidate(BlockPos pos, Direction face, double distanceSq, double planeCoordinate, PlaneRect localBounds, long receiverKey) {
            this.pos = pos;
            this.face = face;
            this.distanceSq = distanceSq;
            this.planeCoordinate = planeCoordinate;
            this.localBounds = localBounds;
            this.receiverKey = receiverKey;
        }
    }

    static final class VisibleCandidateBlockSnapshot {
        final BlockPos center;
        final long frontierSignature;
        final long[] frontierPositions;
        final FaceCandidate[] candidates;
        final List<FaceCandidate> candidateView;

        VisibleCandidateBlockSnapshot(BlockPos center, long frontierSignature, long[] frontierPositions, FaceCandidate[] candidates) {
            this.center = center;
            this.frontierSignature = frontierSignature;
            this.frontierPositions = frontierPositions;
            this.candidates = candidates;
            this.candidateView = candidates.length == 0 ? Collections.emptyList() : Arrays.asList(candidates);
        }

        boolean matches(BlockPos otherCenter, long otherFrontierSignature, long[] otherFrontierPositions) {
            if (this.center.m_123341_() != otherCenter.m_123341_() || this.center.m_123342_() != otherCenter.m_123342_() || this.center.m_123343_() != otherCenter.m_123343_()) {
                return false;
            }
            if (this.frontierPositions.length != otherFrontierPositions.length) {
                return false;
            }
            if (this.frontierSignature == otherFrontierSignature) {
                return true;
            }
            boolean exactMatch = true;
            for (int index = 0; index < this.frontierPositions.length; ++index) {
                if (this.frontierPositions[index] == otherFrontierPositions[index]) continue;
                exactMatch = false;
                break;
            }
            if (exactMatch) {
                return true;
            }
            LongOpenHashSet frontierSet = new LongOpenHashSet(this.frontierPositions.length);
            for (long frontierPosition : this.frontierPositions) {
                frontierSet.add(frontierPosition);
            }
            for (long frontierPosition : otherFrontierPositions) {
                if (frontierSet.contains(frontierPosition)) continue;
                return false;
            }
            return true;
        }

        List<FaceCandidate> asList() {
            return this.candidateView;
        }

        static VisibleCandidateBlockSnapshot empty(BlockPos center, long frontierSignature, long[] frontierPositions) {
            return new VisibleCandidateBlockSnapshot(center.m_7949_(), frontierSignature, Arrays.copyOf(frontierPositions, frontierPositions.length), new FaceCandidate[0]);
        }
    }

    static final class StaticHotPatchOverlay {
        static final StaticHotPatchOverlay EMPTY = new StaticHotPatchOverlay(new Long2ByteOpenHashMap(0), (Long2ObjectOpenHashMap<List<ShadowPolygon>[]>)new Long2ObjectOpenHashMap(0), Collections.emptyList());
        final Long2ByteOpenHashMap replacedFacesByBlock;
        final Long2ObjectOpenHashMap<List<ShadowPolygon>[]> polygonsByBlock;
        final List<ShadowPolygon> polygons;

        StaticHotPatchOverlay(Long2ByteOpenHashMap replacedFacesByBlock, Long2ObjectOpenHashMap<List<ShadowPolygon>[]> polygonsByBlock, List<ShadowPolygon> polygons) {
            replacedFacesByBlock.defaultReturnValue((byte)0);
            this.replacedFacesByBlock = replacedFacesByBlock;
            this.polygonsByBlock = polygonsByBlock;
            this.polygons = polygons;
        }

        boolean isEmpty() {
            return this.replacedFacesByBlock.isEmpty() && this.polygons.isEmpty();
        }

        boolean replaces(ShadowPolygon polygon) {
            byte mask = this.replacedFacesByBlock.get(polygon.blockPos.m_121878_());
            return (mask & StaticHotPatchOverlay.faceMask(polygon.face)) != 0;
        }

        StaticHotPatchOverlay merged(StaticHotPatchOutput output) {
            long blockKey;
            if (output == null || output.affectedFacesByBlock.isEmpty()) {
                return this;
            }
            Long2ByteOpenHashMap mergedMasks = new Long2ByteOpenHashMap(Math.max(16, this.replacedFacesByBlock.size() + output.affectedFacesByBlock.size()));
            mergedMasks.defaultReturnValue((byte)0);
            mergedMasks.putAll((Map)this.replacedFacesByBlock);
            Long2ObjectOpenHashMap mergedByBlock = new Long2ObjectOpenHashMap(Math.max(16, this.polygonsByBlock.size() + output.affectedFacesByBlock.size()));
            for (Long2ObjectMap.Entry entry : this.polygonsByBlock.long2ObjectEntrySet()) {
                mergedByBlock.put(entry.getLongKey(), StaticHotPatchOverlay.copyFaceBuckets((List[])entry.getValue()));
            }
            for (Long2ObjectMap.Entry entry : output.affectedFacesByBlock.long2ByteEntrySet()) {
                blockKey = entry.getLongKey();
                byte faceMask = entry.getByteValue();
                if (faceMask == 0) continue;
                mergedMasks.put(blockKey, (byte)(mergedMasks.get(blockKey) | faceMask));
                List[] buckets = (List[])mergedByBlock.get(blockKey);
                if (buckets == null) {
                    buckets = StaticHotPatchOverlay.newFaceBuckets();
                }
                StaticHotPatchOverlay.clearFaceBuckets(buckets, faceMask);
                if (StaticHotPatchOverlay.hasAnyPolygons(buckets)) {
                    mergedByBlock.put(blockKey, (Object)buckets);
                    continue;
                }
                mergedByBlock.remove(blockKey);
            }
            for (ShadowPolygon polygon : output.shadowPolygons) {
                int faceIndex;
                ArrayList<ShadowPolygon> facePolygons;
                blockKey = polygon.blockPos.m_121878_();
                List[] buckets = (List[])mergedByBlock.get(blockKey);
                if (buckets == null) {
                    buckets = StaticHotPatchOverlay.newFaceBuckets();
                }
                if ((facePolygons = buckets[faceIndex = polygon.face.ordinal()]) == null) {
                    buckets[faceIndex] = facePolygons = new ArrayList<ShadowPolygon>();
                }
                facePolygons.add(polygon);
                mergedByBlock.put(blockKey, (Object)buckets);
            }
            return new StaticHotPatchOverlay(mergedMasks, (Long2ObjectOpenHashMap<List<ShadowPolygon>[]>)mergedByBlock, StaticHotPatchOverlay.flattenPolygons((Long2ObjectOpenHashMap<List<ShadowPolygon>[]>)mergedByBlock));
        }

        private static byte faceMask(Direction face) {
            return (byte)(1 << face.ordinal());
        }

        private static List<ShadowPolygon>[] newFaceBuckets() {
            return new List[NeoShadowsEngine.FACES.length];
        }

        private static List<ShadowPolygon>[] copyFaceBuckets(List<ShadowPolygon>[] source) {
            List<ShadowPolygon>[] copy = StaticHotPatchOverlay.newFaceBuckets();
            System.arraycopy(source, 0, copy, 0, source.length);
            return copy;
        }

        private static void clearFaceBuckets(List<ShadowPolygon>[] buckets, byte faceMask) {
            for (int faceIndex = 0; faceIndex < buckets.length; ++faceIndex) {
                if ((faceMask & 1 << faceIndex) == 0) continue;
                buckets[faceIndex] = null;
            }
        }

        private static boolean hasAnyPolygons(List<ShadowPolygon>[] buckets) {
            for (List<ShadowPolygon> bucket : buckets) {
                if (bucket == null || bucket.isEmpty()) continue;
                return true;
            }
            return false;
        }

        private static List<ShadowPolygon> flattenPolygons(Long2ObjectOpenHashMap<List<ShadowPolygon>[]> polygonsByBlock) {
            ArrayList flattened = new ArrayList();
            ObjectIterator objectIterator = polygonsByBlock.values().iterator();
            while (objectIterator.hasNext()) {
                List[] buckets;
                for (List bucket : buckets = (List[])objectIterator.next()) {
                    if (bucket == null || bucket.isEmpty()) continue;
                    flattened.addAll(bucket);
                }
            }
            return flattened.isEmpty() ? Collections.emptyList() : List.copyOf(flattened);
        }
    }

    static final class StaticHotPatchOutput {
        static final StaticHotPatchOutput EMPTY = new StaticHotPatchOutput(new Long2ByteOpenHashMap(0), Collections.emptyList());
        final Long2ByteOpenHashMap affectedFacesByBlock;
        final List<ShadowPolygon> shadowPolygons;

        StaticHotPatchOutput(Long2ByteOpenHashMap affectedFacesByBlock, List<ShadowPolygon> shadowPolygons) {
            affectedFacesByBlock.defaultReturnValue((byte)0);
            this.affectedFacesByBlock = affectedFacesByBlock;
            this.shadowPolygons = shadowPolygons;
        }

        boolean isEmpty() {
            return this.affectedFacesByBlock.isEmpty() && this.shadowPolygons.isEmpty();
        }
    }

    static final class RealtimeStaticShadowOutput {
        final BlockPos center;
        final List<ShadowPolygon> shadowPolygons;
        final List<ReceiverPlaneGroup> receiverPlaneGroups;
        final int receiverFaceCount;
        final int sourceCount;
        final int occluderHitCount;
        final int shadowPolygonCount;

        RealtimeStaticShadowOutput(BlockPos center, List<ShadowPolygon> shadowPolygons, List<ReceiverPlaneGroup> receiverPlaneGroups, int receiverFaceCount, int sourceCount, int occluderHitCount, int shadowPolygonCount) {
            this.center = center;
            this.shadowPolygons = shadowPolygons;
            this.receiverPlaneGroups = receiverPlaneGroups;
            this.receiverFaceCount = receiverFaceCount;
            this.sourceCount = sourceCount;
            this.occluderHitCount = occluderHitCount;
            this.shadowPolygonCount = shadowPolygonCount;
        }
    }

    static final class ShadowPolygon {
        final BlockPos blockPos;
        final Direction face;
        final double[] vertices;
        final double[] triangleVertices;
        final AABB bounds;
        final double faceCenterX;
        final double faceCenterY;
        final double faceCenterZ;
        final int faceNormalX;
        final int faceNormalY;
        final int faceNormalZ;
        final float strength;

        ShadowPolygon(BlockPos blockPos, Direction face, double[] vertices, float strength) {
            this.blockPos = blockPos;
            this.face = face;
            this.vertices = vertices;
            this.triangleVertices = ShadowPolygon.buildTriangleVertices(vertices);
            this.bounds = ShadowPolygon.buildBounds(vertices);
            this.faceCenterX = (double)blockPos.m_123341_() + 0.5 + (double)face.m_122429_() * 0.5;
            this.faceCenterY = (double)blockPos.m_123342_() + 0.5 + (double)face.m_122430_() * 0.5;
            this.faceCenterZ = (double)blockPos.m_123343_() + 0.5 + (double)face.m_122431_() * 0.5;
            this.faceNormalX = face.m_122429_();
            this.faceNormalY = face.m_122430_();
            this.faceNormalZ = face.m_122431_();
            this.strength = Mth.m_14036_((float)strength, (float)0.0f, (float)1.0f);
        }

        private static AABB buildBounds(double[] vertices) {
            if (vertices.length < 3) {
                return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }
            double minX = vertices[0];
            double minY = vertices[1];
            double minZ = vertices[2];
            double maxX = minX;
            double maxY = minY;
            double maxZ = minZ;
            for (int index = 3; index < vertices.length; index += 3) {
                double x = vertices[index];
                double y = vertices[index + 1];
                double z = vertices[index + 2];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ).m_82400_((double)0.0015f);
        }

        private static double[] buildTriangleVertices(double[] vertices) {
            int vertexCount = vertices.length / 3;
            if (vertexCount < 3) {
                return NeoShadowsProjection.EMPTY_DOUBLE_ARRAY;
            }
            if (vertexCount == 3) {
                return vertices;
            }
            double[] triangulated = new double[(vertexCount - 2) * 9];
            int writeIndex = 0;
            double x0 = vertices[0];
            double y0 = vertices[1];
            double z0 = vertices[2];
            for (int index = 1; index < vertexCount - 1; ++index) {
                int offsetA = index * 3;
                int offsetB = (index + 1) * 3;
                triangulated[writeIndex++] = x0;
                triangulated[writeIndex++] = y0;
                triangulated[writeIndex++] = z0;
                triangulated[writeIndex++] = vertices[offsetA];
                triangulated[writeIndex++] = vertices[offsetA + 1];
                triangulated[writeIndex++] = vertices[offsetA + 2];
                triangulated[writeIndex++] = vertices[offsetB];
                triangulated[writeIndex++] = vertices[offsetB + 1];
                triangulated[writeIndex++] = vertices[offsetB + 2];
            }
            return triangulated;
        }
    }

    static final class DynamicCaster {
        final long id;
        final Box box;
        final Vec3[] boxCorners;
        final List<Vec3[]> quads;

        DynamicCaster(long id, Box box, List<Vec3[]> quads) {
            this.id = id;
            this.box = box;
            this.boxCorners = box.corners();
            this.quads = quads;
        }

        boolean canAffect(Vec3 source, BlockPos center, double maxDist) {
            if (this.box.contains(source.f_82479_, source.f_82480_, source.f_82481_)) {
                return false;
            }
            double sourceDistanceSq = this.box.distanceToPointSq(source.f_82479_, source.f_82480_, source.f_82481_);
            if (sourceDistanceSq > (maxDist + 2.0) * (maxDist + 2.0)) {
                return false;
            }
            double centerDistanceSq = this.box.distanceToPointSq((double)center.m_123341_() + 0.5, (double)center.m_123342_() + 0.5, (double)center.m_123343_() + 0.5);
            return centerDistanceSq <= 1521.0;
        }

        boolean intersectsSegment(double startX, double startY, double startZ, double endX, double endY, double endZ) {
            if (!this.box.intersectsSegment(startX, startY, startZ, endX, endY, endZ)) {
                return false;
            }
            if (this.quads.isEmpty() || this.hasThinMesh()) {
                return true;
            }
            for (Vec3[] quad : this.quads) {
                if (!DynamicCaster.segmentIntersectsQuad(startX, startY, startZ, endX, endY, endZ, quad)) continue;
                return true;
            }
            return false;
        }

        boolean hasThinMesh() {
            return !this.quads.isEmpty() && (this.box.maxX - this.box.minX <= 0.1875 || this.box.maxY - this.box.minY <= 0.1875 || this.box.maxZ - this.box.minZ <= 0.1875);
        }

        private static boolean segmentIntersectsQuad(double startX, double startY, double startZ, double endX, double endY, double endZ, Vec3[] quad) {
            return DynamicCaster.segmentIntersectsTriangle(startX, startY, startZ, endX, endY, endZ, quad[0], quad[1], quad[2]) || DynamicCaster.segmentIntersectsTriangle(startX, startY, startZ, endX, endY, endZ, quad[0], quad[2], quad[3]);
        }

        private static boolean segmentIntersectsTriangle(double startX, double startY, double startZ, double endX, double endY, double endZ, Vec3 a, Vec3 b, Vec3 c) {
            double edge1X = b.f_82479_ - a.f_82479_;
            double dirY = endY - startY;
            double edge2Z = c.f_82481_ - a.f_82481_;
            double dirZ = endZ - startZ;
            double edge2Y = c.f_82480_ - a.f_82480_;
            double pvecX = dirY * edge2Z - dirZ * edge2Y;
            double edge1Y = b.f_82480_ - a.f_82480_;
            double edge2X = c.f_82479_ - a.f_82479_;
            double dirX = endX - startX;
            double pvecY = dirZ * edge2X - dirX * edge2Z;
            double edge1Z = b.f_82481_ - a.f_82481_;
            double pvecZ = dirX * edge2Y - dirY * edge2X;
            double determinant = edge1X * pvecX + edge1Y * pvecY + edge1Z * pvecZ;
            if (Math.abs(determinant) <= 1.0E-9) {
                return false;
            }
            double tvecX = startX - a.f_82479_;
            double tvecY = startY - a.f_82480_;
            double tvecZ = startZ - a.f_82481_;
            double invDet = 1.0 / determinant;
            double u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet;
            if (u < -1.0E-6 || u > 1.000001) {
                return false;
            }
            double qvecX = tvecY * edge1Z - tvecZ * edge1Y;
            double qvecY = tvecZ * edge1X - tvecX * edge1Z;
            double qvecZ = tvecX * edge1Y - tvecY * edge1X;
            double v = (dirX * qvecX + dirY * qvecY + dirZ * qvecZ) * invDet;
            if (v < -1.0E-6 || u + v > 1.000001) {
                return false;
            }
            double t = (edge2X * qvecX + edge2Y * qvecY + edge2Z * qvecZ) * invDet;
            return t >= -1.0E-6 && t <= 1.000001;
        }
    }

    static final class BuildScratch {
        final List<ShadowPolygon> shadowPolygons = new ArrayList<ShadowPolygon>();
        final Long2ObjectLinkedOpenHashMap<NeoShadowsOcclusionPipeline.OccluderHit> reusableGroupHits = new Long2ObjectLinkedOpenHashMap();
        final List<UvPoint> projectedPointBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> clipBufA = new ArrayList<UvPoint>(32);
        final List<UvPoint> clipBufB = new ArrayList<UvPoint>(32);
        final List<UvPoint> hullSortBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> hullUniqueBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> hullLowerBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> hullUpperBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> hullOutputBuf = new ArrayList<UvPoint>(64);
        final List<UvPoint> polyLocalBuf = new ArrayList<UvPoint>(32);
        final List<UvPoint> polyDilateBuf = new ArrayList<UvPoint>(32);
        final List<UvPoint> polySnapBuf = new ArrayList<UvPoint>(32);
        int occluderHitCount;
        int shadowPolygonCount;

        BuildScratch() {
        }

        void reset() {
            this.shadowPolygons.clear();
            this.reusableGroupHits.clear();
            this.projectedPointBuf.clear();
            this.clipBufA.clear();
            this.clipBufB.clear();
            this.hullSortBuf.clear();
            this.hullUniqueBuf.clear();
            this.hullLowerBuf.clear();
            this.hullUpperBuf.clear();
            this.hullOutputBuf.clear();
            this.polyLocalBuf.clear();
            this.polyDilateBuf.clear();
            this.polySnapBuf.clear();
            this.occluderHitCount = 0;
            this.shadowPolygonCount = 0;
        }
    }

    static final class GeometryBuildState {
        final ClientLevel level;
        final BlockPos center;
        final ExposedFaceService exposedFaceService;
        final boolean includeDynamicCasters;
        final List<DynamicCaster> dynamicCasters;
        final EnumMap<Direction, Long2ObjectOpenHashMap<FaceLightingInfo>> faceLightingCache = new EnumMap(Direction.class);
        final List<ShadowPolygon> shadowPolygons = new ArrayList<ShadowPolygon>();
        final Long2LongOpenHashMap[] litSampleCache = new Long2LongOpenHashMap[6];
        final Long2ObjectOpenHashMap<ContributorList> contributorCache = new Long2ObjectOpenHashMap();
        final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<NeoShadowsOcclusionPipeline.ProbeFirstHitCache>> firstHitScalarCache = new Long2ObjectOpenHashMap();
        int receiverFaceCount;
        int sourceCount;
        int occluderHitCount;
        int shadowPolygonCount;

        GeometryBuildState(NeoShadowsEngine engine, ClientLevel level, BlockPos center, ExposedFaceService exposedFaceService, boolean includeDynamicCasters, DynamicShadowMeshCapture.Snapshot dynamicSnapshot) {
            this.level = level;
            this.center = center;
            this.exposedFaceService = exposedFaceService;
            this.includeDynamicCasters = includeDynamicCasters;
            this.dynamicCasters = includeDynamicCasters ? engine.dynamicPipeline.collectDynamicCasters(level, center, dynamicSnapshot) : Collections.emptyList();
            this.contributorCache.defaultReturnValue(null);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        NeoShadowsOcclusionPipeline.ProbeFirstHitCache firstHitScalarCache(long sourceKey, long receiverKey, int probeCount) {
            Long2ObjectOpenHashMap inner;
            Long2ObjectOpenHashMap long2ObjectOpenHashMap = this.firstHitScalarCache;
            synchronized (long2ObjectOpenHashMap) {
                inner = (Long2ObjectOpenHashMap)this.firstHitScalarCache.get(sourceKey);
                if (inner == null) {
                    inner = new Long2ObjectOpenHashMap();
                    this.firstHitScalarCache.put(sourceKey, (Object)inner);
                }
            }
            long2ObjectOpenHashMap = inner;
            synchronized (long2ObjectOpenHashMap) {
                NeoShadowsOcclusionPipeline.ProbeFirstHitCache cache = (NeoShadowsOcclusionPipeline.ProbeFirstHitCache)inner.get(receiverKey);
                if (cache == null) {
                    cache = new NeoShadowsOcclusionPipeline.ProbeFirstHitCache(probeCount);
                    inner.put(receiverKey, (Object)cache);
                }
                return cache;
            }
        }
    }

    static final class FaceLightingScratch {
        final Long2ObjectOpenHashMap<SourceScore> scores = new Long2ObjectOpenHashMap();
        final ContributorScoringVisitor visitor = new ContributorScoringVisitor();

        FaceLightingScratch() {
        }
    }

    static final class ContributorCollector
    implements NeoFloodEngine.ContributorVisitor {
        long[] sourceKeys = new long[16];
        int[] packed = new int[16];
        int count;

        ContributorCollector() {
        }

        void reset() {
            this.count = 0;
        }

        @Override
        public void visit(long sourceKey, int light, int coverage, int sourceEmission) {
            if (this.count == this.sourceKeys.length) {
                int newCap = this.sourceKeys.length * 2;
                long[] nextKeys = new long[newCap];
                int[] nextPacked = new int[newCap];
                System.arraycopy(this.sourceKeys, 0, nextKeys, 0, this.count);
                System.arraycopy(this.packed, 0, nextPacked, 0, this.count);
                this.sourceKeys = nextKeys;
                this.packed = nextPacked;
            }
            this.sourceKeys[this.count] = sourceKey;
            this.packed[this.count] = light & 0xFF | (coverage & 0xFF) << 8 | (sourceEmission & 0xFF) << 16;
            ++this.count;
        }

        ContributorList toList() {
            if (this.count == 0) {
                return ContributorList.EMPTY;
            }
            long[] keys = new long[this.count];
            int[] pack = new int[this.count];
            System.arraycopy(this.sourceKeys, 0, keys, 0, this.count);
            System.arraycopy(this.packed, 0, pack, 0, this.count);
            return new ContributorList(keys, pack, this.count);
        }
    }

    static final class ContributorList {
        static final ContributorList EMPTY = new ContributorList(new long[0], new int[0], 0);
        final long[] sourceKeys;
        final int[] packed;
        final int count;

        ContributorList(long[] sourceKeys, int[] packed, int count) {
            this.sourceKeys = sourceKeys;
            this.packed = packed;
            this.count = count;
        }
    }
}

