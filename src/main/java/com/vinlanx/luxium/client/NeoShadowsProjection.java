/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.NeoShadowsOcclusionPipeline;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class NeoShadowsProjection {
    private static final NeoShadowsTypes.PlaneRect FULL_FACE_RECT = new NeoShadowsTypes.PlaneRect(0.0, 1.0, 0.0, 1.0);
    static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    static final ThreadLocal<double[]> SCRATCH_RECEIVER_SAMPLES = ThreadLocal.withInitial(() -> new double[768]);
    private static final int[][] BOX_EDGE_VERTEX_INDICES = new int[][]{{0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}};

    private NeoShadowsProjection() {
    }

    static List<NeoShadowsTypes.UvPoint> collectProjectedBoxPoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, NeoShadowsTypes.Box worldBox) {
        Vec3[] corners = worldBox.corners();
        NeoShadowsTypes.ProjectionPoint[] projectedCorners = new NeoShadowsTypes.ProjectionPoint[corners.length];
        ArrayList<NeoShadowsTypes.UvPoint> points = new ArrayList<NeoShadowsTypes.UvPoint>(corners.length + BOX_EDGE_VERTEX_INDICES.length * 2);
        for (int index = 0; index < corners.length; ++index) {
            NeoShadowsTypes.ProjectionPoint projection;
            projectedCorners[index] = projection = plane.projectPoint(source, corners[index]);
            if (projection.uv == null) continue;
            points.add(projection.uv);
        }
        for (int[] edge : BOX_EDGE_VERTEX_INDICES) {
            NeoShadowsProjection.appendExtendedEdgePoints(plane, source, corners[edge[0]], corners[edge[1]], projectedCorners[edge[0]], projectedCorners[edge[1]], points);
        }
        return points;
    }

    static List<NeoShadowsTypes.UvPoint> collectProjectedBoxPoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, Vec3[] corners, List<NeoShadowsTypes.UvPoint> points) {
        points.clear();
        NeoShadowsTypes.ProjectionPoint[] projectedCorners = new NeoShadowsTypes.ProjectionPoint[corners.length];
        for (int index = 0; index < corners.length; ++index) {
            NeoShadowsTypes.ProjectionPoint projection;
            projectedCorners[index] = projection = plane.projectPoint(source, corners[index]);
            if (projection.uv == null) continue;
            points.add(projection.uv);
        }
        for (int[] edge : BOX_EDGE_VERTEX_INDICES) {
            NeoShadowsProjection.appendExtendedEdgePoints(plane, source, corners[edge[0]], corners[edge[1]], projectedCorners[edge[0]], projectedCorners[edge[1]], points);
        }
        return points;
    }

    static List<NeoShadowsTypes.UvPoint> collectProjectedBoxPoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, NeoShadowsTypes.Box localBox, BlockPos blockPos) {
        Vec3[] corners = localBox.cornersWithOffset(blockPos.m_123341_(), blockPos.m_123342_(), blockPos.m_123343_());
        NeoShadowsTypes.ProjectionPoint[] projectedCorners = new NeoShadowsTypes.ProjectionPoint[corners.length];
        ArrayList<NeoShadowsTypes.UvPoint> points = new ArrayList<NeoShadowsTypes.UvPoint>(corners.length + BOX_EDGE_VERTEX_INDICES.length * 2);
        for (int index = 0; index < corners.length; ++index) {
            NeoShadowsTypes.ProjectionPoint projection;
            projectedCorners[index] = projection = plane.projectPoint(source, corners[index]);
            if (projection.uv == null) continue;
            points.add(projection.uv);
        }
        for (int[] edge : BOX_EDGE_VERTEX_INDICES) {
            NeoShadowsProjection.appendExtendedEdgePoints(plane, source, corners[edge[0]], corners[edge[1]], projectedCorners[edge[0]], projectedCorners[edge[1]], points);
        }
        return points;
    }

    private static void appendExtendedEdgePoints(NeoShadowsTypes.GlobalReceiverPlane plane, Vec3 source, Vec3 start, Vec3 end, NeoShadowsTypes.ProjectionPoint startProjection, NeoShadowsTypes.ProjectionPoint endProjection, List<NeoShadowsTypes.UvPoint> points) {
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

    static double clampUnit(double value) {
        return Math.max(1.0E-6, Math.min(0.999999, value));
    }

    static Vec3 lerp(Vec3 start, Vec3 end, double delta) {
        return new Vec3(start.f_82479_ + (end.f_82479_ - start.f_82479_) * delta, start.f_82480_ + (end.f_82480_ - start.f_82480_) * delta, start.f_82481_ + (end.f_82481_ - start.f_82481_) * delta);
    }

    @Nullable
    static List<NeoShadowsTypes.UvPoint> buildProjectedShadowHull(List<NeoShadowsTypes.UvPoint> projected) {
        if (projected.size() < 3) {
            return null;
        }
        List<NeoShadowsTypes.UvPoint> hull = NeoShadowsProjection.buildConvexHull(projected);
        if (hull.size() < 3 || NeoShadowsProjection.polygonArea(hull) <= 1.0E-5) {
            return null;
        }
        return hull;
    }

    @Nullable
    static List<NeoShadowsTypes.UvPoint> buildProjectedShadowHullBuffered(List<NeoShadowsTypes.UvPoint> projected, NeoShadowsTypes.BuildScratch scratch) {
        if (projected.size() < 3) {
            return null;
        }
        List<NeoShadowsTypes.UvPoint> hull = NeoShadowsProjection.buildConvexHullBuffered(projected, scratch);
        if (hull.size() < 3 || NeoShadowsProjection.polygonArea(hull) <= 1.0E-5) {
            return null;
        }
        return hull;
    }

    static List<NeoShadowsTypes.UvPoint> fullBlockShadowPolygon() {
        return List.of(new NeoShadowsTypes.UvPoint(0.0, 0.0), new NeoShadowsTypes.UvPoint(1.0, 0.0), new NeoShadowsTypes.UvPoint(1.0, 1.0), new NeoShadowsTypes.UvPoint(0.0, 1.0));
    }

    static long sampleRtxPosKey(BlockPos pos, Direction face, double sampleX, double sampleY, double sampleZ) {
        int sampleBlockZ;
        int sampleBlockY;
        double outwardX = (double)face.m_122429_() * 0.05;
        double outwardY = (double)face.m_122430_() * 0.05;
        double outwardZ = (double)face.m_122431_() * 0.05;
        int sampleBlockX = Mth.m_14107_((double)(sampleX + outwardX));
        long sampleKey = BlockPos.m_121882_((int)sampleBlockX, (int)(sampleBlockY = Mth.m_14107_((double)(sampleY + outwardY))), (int)(sampleBlockZ = Mth.m_14107_((double)(sampleZ + outwardZ))));
        if (sampleKey == pos.m_121878_()) {
            return BlockPos.m_121882_((int)(pos.m_123341_() + face.m_122429_()), (int)(pos.m_123342_() + face.m_122430_()), (int)(pos.m_123343_() + face.m_122431_()));
        }
        return sampleKey;
    }

    static Direction firstFaceTangent(Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> Direction.EAST;
            case Direction.WEST, Direction.EAST -> Direction.SOUTH;
        };
    }

    static Direction secondFaceTangent(Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> Direction.SOUTH;
            case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> Direction.UP;
        };
    }

    static double facePointX(BlockPos pos, Direction face, double planeCoordinate, float s) {
        double minX = pos.m_123341_();
        double maxX = minX + 1.0;
        double epsilon = 0.0015f;
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.SOUTH -> minX + (double)s;
            case Direction.NORTH -> maxX - (double)s;
            case Direction.WEST -> planeCoordinate - epsilon;
            case Direction.EAST -> planeCoordinate + epsilon;
        };
    }

    static double facePointY(BlockPos pos, Direction face, double planeCoordinate, float t) {
        double minY = pos.m_123342_();
        double epsilon = 0.0015f;
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> planeCoordinate + epsilon;
            case Direction.DOWN -> planeCoordinate - epsilon;
            case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> minY + (double)t;
        };
    }

    static double facePointZ(BlockPos pos, Direction face, double planeCoordinate, float s, float t) {
        double minZ = pos.m_123343_();
        double epsilon = 0.0015f;
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> minZ + (double)t;
            case Direction.DOWN -> minZ + 1.0 - (double)t;
            case Direction.NORTH -> planeCoordinate - epsilon;
            case Direction.SOUTH -> planeCoordinate + epsilon;
            case Direction.WEST -> minZ + 1.0 - (double)s;
            case Direction.EAST -> minZ + (double)s;
        };
    }

    static int[][] buildReceiverOffsets() {
        ArrayList<int[]> offsets = new ArrayList<int[]>();
        for (int dy = -3; dy <= 3; ++dy) {
            for (int dz = -3; dz <= 3; ++dz) {
                for (int dx = -3; dx <= 3; ++dx) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 3) continue;
                    offsets.add(new int[]{dx, dy, dz});
                }
            }
        }
        return (int[][])offsets.toArray((T[])new int[0][]);
    }

    static List<NeoShadowsTypes.UvPoint> buildConvexHull(List<NeoShadowsTypes.UvPoint> points) {
        ArrayList<NeoShadowsTypes.UvPoint> sorted = new ArrayList<NeoShadowsTypes.UvPoint>(points);
        sorted.sort((left, right) -> {
            int cmpU = Double.compare(left.u, right.u);
            if (cmpU != 0) {
                return cmpU;
            }
            return Double.compare(left.v, right.v);
        });
        ArrayList<NeoShadowsTypes.UvPoint> unique = new ArrayList<NeoShadowsTypes.UvPoint>(sorted.size());
        for (NeoShadowsTypes.UvPoint uvPoint : sorted) {
            if (!unique.isEmpty() && uvPoint.equalsApprox((NeoShadowsTypes.UvPoint)unique.get(unique.size() - 1))) continue;
            unique.add(uvPoint);
        }
        if (unique.size() < 3) {
            return unique;
        }
        ArrayList<NeoShadowsTypes.UvPoint> lower = new ArrayList<NeoShadowsTypes.UvPoint>();
        for (NeoShadowsTypes.UvPoint point : unique) {
            while (lower.size() >= 2 && NeoShadowsProjection.cross((NeoShadowsTypes.UvPoint)lower.get(lower.size() - 2), (NeoShadowsTypes.UvPoint)lower.get(lower.size() - 1), point) <= 1.0E-6) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }
        ArrayList<NeoShadowsTypes.UvPoint> arrayList = new ArrayList<NeoShadowsTypes.UvPoint>();
        for (int index = unique.size() - 1; index >= 0; --index) {
            NeoShadowsTypes.UvPoint point = (NeoShadowsTypes.UvPoint)unique.get(index);
            while (arrayList.size() >= 2 && NeoShadowsProjection.cross((NeoShadowsTypes.UvPoint)arrayList.get(arrayList.size() - 2), (NeoShadowsTypes.UvPoint)arrayList.get(arrayList.size() - 1), point) <= 1.0E-6) {
                arrayList.remove(arrayList.size() - 1);
            }
            arrayList.add(point);
        }
        lower.remove(lower.size() - 1);
        arrayList.remove(arrayList.size() - 1);
        lower.addAll(arrayList);
        return lower;
    }

    static List<NeoShadowsTypes.UvPoint> buildConvexHullBuffered(List<NeoShadowsTypes.UvPoint> points, NeoShadowsTypes.BuildScratch scratch) {
        int index;
        List<NeoShadowsTypes.UvPoint> sorted = scratch.hullSortBuf;
        sorted.clear();
        sorted.addAll(points);
        sorted.sort((left, right) -> {
            int cmpU = Double.compare(left.u, right.u);
            if (cmpU != 0) {
                return cmpU;
            }
            return Double.compare(left.v, right.v);
        });
        List<NeoShadowsTypes.UvPoint> unique = scratch.hullUniqueBuf;
        unique.clear();
        for (NeoShadowsTypes.UvPoint uvPoint : sorted) {
            if (!unique.isEmpty() && uvPoint.equalsApprox(unique.get(unique.size() - 1))) continue;
            unique.add(uvPoint);
        }
        if (unique.size() < 3) {
            return unique;
        }
        List<NeoShadowsTypes.UvPoint> lower = scratch.hullLowerBuf;
        lower.clear();
        for (NeoShadowsTypes.UvPoint point : unique) {
            while (lower.size() >= 2 && NeoShadowsProjection.cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), point) <= 1.0E-6) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }
        List<NeoShadowsTypes.UvPoint> list = scratch.hullUpperBuf;
        list.clear();
        for (int index2 = unique.size() - 1; index2 >= 0; --index2) {
            NeoShadowsTypes.UvPoint point = unique.get(index2);
            while (list.size() >= 2 && NeoShadowsProjection.cross(list.get(list.size() - 2), list.get(list.size() - 1), point) <= 1.0E-6) {
                list.remove(list.size() - 1);
            }
            list.add(point);
        }
        List<NeoShadowsTypes.UvPoint> output = scratch.hullOutputBuf;
        output.clear();
        for (index = 0; index < lower.size() - 1; ++index) {
            output.add(lower.get(index));
        }
        for (index = 0; index < list.size() - 1; ++index) {
            output.add(list.get(index));
        }
        return output;
    }

    static double cross(NeoShadowsTypes.UvPoint a, NeoShadowsTypes.UvPoint b, NeoShadowsTypes.UvPoint c) {
        return (b.u - a.u) * (c.v - a.v) - (b.v - a.v) * (c.u - a.u);
    }

    static List<NeoShadowsTypes.UvPoint> clipToRectangleBuffered(List<NeoShadowsTypes.UvPoint> polygon, double minU, double maxU, double minV, double maxV, NeoShadowsTypes.BuildScratch scratch) {
        List<NeoShadowsTypes.UvPoint> src = polygon;
        List<NeoShadowsTypes.UvPoint> dst = scratch.clipBufA;
        dst.clear();
        NeoShadowsProjection.clipAgainstEdgeInto(src, dst, p -> p.u >= minU, (a, b) -> NeoShadowsProjection.interpolateAtU(a, b, minU));
        if (dst.isEmpty()) {
            return dst;
        }
        src = dst;
        dst = scratch.clipBufB;
        dst.clear();
        NeoShadowsProjection.clipAgainstEdgeInto(src, dst, p -> p.u <= maxU, (a, b) -> NeoShadowsProjection.interpolateAtU(a, b, maxU));
        if (dst.isEmpty()) {
            return dst;
        }
        src = dst;
        dst = scratch.clipBufA;
        dst.clear();
        NeoShadowsProjection.clipAgainstEdgeInto(src, dst, p -> p.v >= minV, (a, b) -> NeoShadowsProjection.interpolateAtV(a, b, minV));
        if (dst.isEmpty()) {
            return dst;
        }
        src = dst;
        dst = scratch.clipBufB;
        dst.clear();
        NeoShadowsProjection.clipAgainstEdgeInto(src, dst, p -> p.v <= maxV, (a, b) -> NeoShadowsProjection.interpolateAtV(a, b, maxV));
        return dst;
    }

    static void clipAgainstEdgeInto(List<NeoShadowsTypes.UvPoint> polygon, List<NeoShadowsTypes.UvPoint> output, Predicate<NeoShadowsTypes.UvPoint> inside, IntersectionFactory intersectionFactory) {
        if (polygon.isEmpty()) {
            return;
        }
        NeoShadowsTypes.UvPoint previous = polygon.get(polygon.size() - 1);
        boolean previousInside = inside.test(previous);
        for (NeoShadowsTypes.UvPoint current : polygon) {
            boolean currentInside = inside.test(current);
            if (currentInside) {
                if (!previousInside) {
                    output.add(intersectionFactory.create(previous, current));
                }
                output.add(current);
            } else if (previousInside) {
                output.add(intersectionFactory.create(previous, current));
            }
            previous = current;
            previousInside = currentInside;
        }
    }

    static NeoShadowsTypes.UvPoint interpolateAtU(NeoShadowsTypes.UvPoint a, NeoShadowsTypes.UvPoint b, double u) {
        double delta = b.u - a.u;
        double t = Math.abs(delta) <= 1.0E-6 ? 0.0 : (u - a.u) / delta;
        return new NeoShadowsTypes.UvPoint(u, a.v + (b.v - a.v) * t);
    }

    static NeoShadowsTypes.UvPoint interpolateAtV(NeoShadowsTypes.UvPoint a, NeoShadowsTypes.UvPoint b, double v) {
        double delta = b.v - a.v;
        double t = Math.abs(delta) <= 1.0E-6 ? 0.0 : (v - a.v) / delta;
        return new NeoShadowsTypes.UvPoint(a.u + (b.u - a.u) * t, v);
    }

    static double polygonArea(List<NeoShadowsTypes.UvPoint> polygon) {
        double area = 0.0;
        for (int i = 0; i < polygon.size(); ++i) {
            NeoShadowsTypes.UvPoint a = polygon.get(i);
            NeoShadowsTypes.UvPoint b = polygon.get((i + 1) % polygon.size());
            area += a.u * b.v - b.u * a.v;
        }
        return Math.abs(area) * 0.5;
    }

    static boolean rectFullyInsideConvexPolygon(List<NeoShadowsTypes.UvPoint> polygon, NeoShadowsTypes.PlaneRect rect) {
        if (polygon.size() < 3) {
            return false;
        }
        return NeoShadowsProjection.convexPolygonContainsPoint(polygon, rect.minU, rect.minV) && NeoShadowsProjection.convexPolygonContainsPoint(polygon, rect.maxU, rect.minV) && NeoShadowsProjection.convexPolygonContainsPoint(polygon, rect.maxU, rect.maxV) && NeoShadowsProjection.convexPolygonContainsPoint(polygon, rect.minU, rect.maxV);
    }

    static boolean convexPolygonContainsPoint(List<NeoShadowsTypes.UvPoint> polygon, double u, double v) {
        double sign = 0.0;
        int size = polygon.size();
        for (int index = 0; index < size; ++index) {
            NeoShadowsTypes.UvPoint current = polygon.get(index);
            NeoShadowsTypes.UvPoint next = polygon.get((index + 1) % size);
            double cross = (next.u - current.u) * (v - current.v) - (next.v - current.v) * (u - current.u);
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

    @Nullable
    static List<NeoShadowsTypes.UvPoint> normalizeShadowPolygonToRect(List<NeoShadowsTypes.UvPoint> polygon, NeoShadowsTypes.PlaneRect rect, Direction face, NeoShadowsTypes.BuildScratch scratch) {
        if (polygon.size() < 3) {
            return null;
        }
        double rectWidth = rect.maxU - rect.minU;
        double rectHeight = rect.maxV - rect.minV;
        if (rectWidth <= 1.0E-6 || rectHeight <= 1.0E-6) {
            return polygon;
        }
        double rectArea = rectWidth * rectHeight;
        if (rectArea <= 1.0E-5) {
            return polygon;
        }
        if (NeoShadowsProjection.polygonArea(polygon) / rectArea >= 0.988) {
            return NeoShadowsProjection.rectShadowPolygon(rect);
        }
        List<NeoShadowsTypes.UvPoint> localPolygon = scratch.polyLocalBuf;
        localPolygon.clear();
        for (NeoShadowsTypes.UvPoint point : polygon) {
            localPolygon.add(new NeoShadowsTypes.UvPoint((point.u - rect.minU) / rectWidth, (point.v - rect.minV) / rectHeight));
        }
        List<NeoShadowsTypes.UvPoint> dilated = NeoShadowsProjection.dilateUvPolygonInto(localPolygon, NeoShadowsProjection.getShadowPolygonDilation(face), scratch.polyDilateBuf);
        List<NeoShadowsTypes.UvPoint> snapped = NeoShadowsProjection.snapPolygonToEdgesInto(dilated, NeoShadowsProjection.getShadowEdgeSnapEpsilon(face), scratch.polySnapBuf);
        List<NeoShadowsTypes.UvPoint> clippedLocal = NeoShadowsProjection.clipToRectangleBuffered(snapped, 0.0, 1.0, 0.0, 1.0, scratch);
        if (clippedLocal.size() < 3) {
            return polygon;
        }
        List<NeoShadowsTypes.UvPoint> normalizedLocal = NeoShadowsProjection.buildConvexHull(clippedLocal);
        if (normalizedLocal.size() < 3) {
            return polygon;
        }
        if (NeoShadowsProjection.polygonArea(normalizedLocal) >= 0.988) {
            return NeoShadowsProjection.rectShadowPolygon(rect);
        }
        ArrayList<NeoShadowsTypes.UvPoint> normalized = new ArrayList<NeoShadowsTypes.UvPoint>(normalizedLocal.size());
        for (NeoShadowsTypes.UvPoint point : normalizedLocal) {
            normalized.add(new NeoShadowsTypes.UvPoint(rect.minU + point.u * rectWidth, rect.minV + point.v * rectHeight));
        }
        return normalized;
    }

    static List<NeoShadowsTypes.UvPoint> dilateUvPolygonInto(List<NeoShadowsTypes.UvPoint> polygon, double amount, List<NeoShadowsTypes.UvPoint> dilated) {
        if (amount <= 1.0E-6 || polygon.isEmpty()) {
            return polygon;
        }
        double centerU = 0.0;
        double centerV = 0.0;
        for (NeoShadowsTypes.UvPoint point : polygon) {
            centerU += point.u;
            centerV += point.v;
        }
        centerU /= (double)polygon.size();
        centerV /= (double)polygon.size();
        dilated.clear();
        for (NeoShadowsTypes.UvPoint point : polygon) {
            double dirU = point.u - centerU;
            double dirV = point.v - centerV;
            double length = Math.sqrt(dirU * dirU + dirV * dirV);
            if (length <= 1.0E-6) {
                dilated.add(point);
                continue;
            }
            dilated.add(new NeoShadowsTypes.UvPoint(point.u + dirU / length * amount, point.v + dirV / length * amount));
        }
        return dilated;
    }

    static List<NeoShadowsTypes.UvPoint> snapPolygonToEdgesInto(List<NeoShadowsTypes.UvPoint> polygon, double epsilon, List<NeoShadowsTypes.UvPoint> snapped) {
        if (polygon.isEmpty() || epsilon <= 1.0E-6) {
            return polygon;
        }
        snapped.clear();
        for (NeoShadowsTypes.UvPoint point : polygon) {
            snapped.add(new NeoShadowsTypes.UvPoint(NeoShadowsProjection.snapUvCoordinate(point.u, epsilon), NeoShadowsProjection.snapUvCoordinate(point.v, epsilon)));
        }
        return snapped;
    }

    static double snapUvCoordinate(double value, double epsilon) {
        if (value <= epsilon) {
            return 0.0;
        }
        if (value >= 1.0 - epsilon) {
            return 1.0;
        }
        return value;
    }

    static double getShadowPolygonDilation(Direction face) {
        return NeoShadowsProjection.isHorizontalReceiverFace(face) ? 0.024 : 0.016;
    }

    static double getShadowEdgeSnapEpsilon(Direction face) {
        return NeoShadowsProjection.isHorizontalReceiverFace(face) ? 0.03 : 0.022;
    }

    static List<NeoShadowsTypes.UvPoint> rectShadowPolygon(NeoShadowsTypes.PlaneRect rect) {
        return List.of(new NeoShadowsTypes.UvPoint(rect.minU, rect.minV), new NeoShadowsTypes.UvPoint(rect.maxU, rect.minV), new NeoShadowsTypes.UvPoint(rect.maxU, rect.maxV), new NeoShadowsTypes.UvPoint(rect.minU, rect.maxV));
    }

    static int planeCoordinateForFace(BlockPos pos, Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> pos.m_123342_() + 1;
            case Direction.DOWN -> pos.m_123342_();
            case Direction.NORTH -> pos.m_123343_();
            case Direction.SOUTH -> pos.m_123343_() + 1;
            case Direction.WEST -> pos.m_123341_();
            case Direction.EAST -> pos.m_123341_() + 1;
        };
    }

    static boolean isMergeableReceiverBounds(NeoShadowsTypes.PlaneRect rect) {
        return rect != null && Math.abs(rect.minU) <= 1.0E-6 && Math.abs(rect.minV) <= 1.0E-6 && Math.abs(rect.maxU - 1.0) <= 1.0E-6 && Math.abs(rect.maxV - 1.0) <= 1.0E-6;
    }

    static boolean isMergeablePlaneCoordinate(double planeCoordinate) {
        return Math.abs(planeCoordinate - Math.rint(planeCoordinate)) <= 1.0E-6;
    }

    static boolean sameReceiverStrength(float left, float right) {
        return Math.abs(left - right) <= 0.01f;
    }

    static long receiverCellKey(int cellU, int cellV) {
        return (long)cellU << 32 ^ (long)cellV & 0xFFFFFFFFL;
    }

    static int unpackReceiverCellU(long cellKey) {
        return (int)(cellKey >> 32);
    }

    static int unpackReceiverCellV(long cellKey) {
        return (int)cellKey;
    }

    static boolean isProbeInsideRect(NeoShadowsTypes.UvPoint probe, NeoShadowsTypes.PlaneRect rect) {
        return probe.u >= rect.minU - 1.0E-6 && probe.u <= rect.maxU + 1.0E-6 && probe.v >= rect.minV - 1.0E-6 && probe.v <= rect.maxV + 1.0E-6;
    }

    static NeoShadowsTypes.UvPoint[] buildOcclusionFaceProbes(Direction face) {
        return NeoShadowsOcclusionPipeline.buildOcclusionFaceProbes(face);
    }

    static NeoShadowsTypes.PlaneRect fullFaceRect() {
        return FULL_FACE_RECT;
    }

    static boolean isHorizontalReceiverFace(Direction face) {
        return face == Direction.UP || face == Direction.DOWN;
    }

    static int blockPosToPlaneCellU(Direction face, BlockPos pos) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> pos.m_123341_();
            case Direction.WEST, Direction.EAST -> pos.m_123343_();
        };
    }

    static int blockPosToPlaneCellV(Direction face, BlockPos pos) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN -> pos.m_123343_();
            case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> pos.m_123342_();
        };
    }

    static BlockPos planeCellToBlockPos(NeoShadowsTypes.ReceiverPlaneKey key, int cellU, int cellV) {
        return switch (key.face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> new BlockPos(cellU, Mth.m_14107_((double)key.planeCoordinate) - 1, cellV);
            case Direction.DOWN -> new BlockPos(cellU, Mth.m_14107_((double)key.planeCoordinate), cellV);
            case Direction.NORTH -> new BlockPos(cellU, cellV, Mth.m_14107_((double)key.planeCoordinate));
            case Direction.SOUTH -> new BlockPos(cellU, cellV, Mth.m_14107_((double)key.planeCoordinate) - 1);
            case Direction.WEST -> new BlockPos(Mth.m_14107_((double)key.planeCoordinate), cellV, cellU);
            case Direction.EAST -> new BlockPos(Mth.m_14107_((double)key.planeCoordinate) - 1, cellV, cellU);
        };
    }

    static NeoShadowsTypes.PlaneRect faceContributionPlaneRect(Direction face, BlockPos pos, NeoShadowsTypes.PlaneRect localRect) {
        int cellU = NeoShadowsProjection.blockPosToPlaneCellU(face, pos);
        int cellV = NeoShadowsProjection.blockPosToPlaneCellV(face, pos);
        return new NeoShadowsTypes.PlaneRect((double)cellU + localRect.minU, (double)cellU + localRect.maxU, (double)cellV + localRect.minV, (double)cellV + localRect.maxV);
    }

    static long sectionRectCellToBlockKey(int sectionBaseX, int sectionBaseY, int sectionBaseZ, Direction face, int localU, int localV, int planeCoordinate) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> BlockPos.m_121882_((int)(sectionBaseX + localU), (int)(planeCoordinate - 1), (int)(sectionBaseZ + localV));
            case Direction.DOWN -> BlockPos.m_121882_((int)(sectionBaseX + localU), (int)planeCoordinate, (int)(sectionBaseZ + localV));
            case Direction.NORTH -> BlockPos.m_121882_((int)(sectionBaseX + localU), (int)(sectionBaseY + localV), (int)planeCoordinate);
            case Direction.SOUTH -> BlockPos.m_121882_((int)(sectionBaseX + localU), (int)(sectionBaseY + localV), (int)(planeCoordinate - 1));
            case Direction.WEST -> BlockPos.m_121882_((int)planeCoordinate, (int)(sectionBaseY + localV), (int)(sectionBaseZ + localU));
            case Direction.EAST -> BlockPos.m_121882_((int)(planeCoordinate - 1), (int)(sectionBaseY + localV), (int)(sectionBaseZ + localU));
        };
    }

    static long[] scratchKeys(int needed) {
        long[] current = NeoShadowsEngine.CANDIDATE_KEYS_TL.get();
        if (current.length >= needed) {
            return current;
        }
        int size = Integer.highestOneBit(Math.max(needed, 16) - 1) << 1;
        long[] grown = new long[size];
        NeoShadowsEngine.CANDIDATE_KEYS_TL.set(grown);
        return grown;
    }

    static double[] scratchDistances(int needed) {
        double[] current = NeoShadowsEngine.CANDIDATE_DISTANCES_TL.get();
        if (current.length >= needed) {
            return current;
        }
        int size = Integer.highestOneBit(Math.max(needed, 16) - 1) << 1;
        double[] grown = new double[size];
        NeoShadowsEngine.CANDIDATE_DISTANCES_TL.set(grown);
        return grown;
    }

    static byte[] scratchMasks(int needed) {
        byte[] current = NeoShadowsEngine.CANDIDATE_MASKS_TL.get();
        if (current.length >= needed) {
            return current;
        }
        int size = Integer.highestOneBit(Math.max(needed, 16) - 1) << 1;
        byte[] grown = new byte[size];
        NeoShadowsEngine.CANDIDATE_MASKS_TL.set(grown);
        return grown;
    }

    @FunctionalInterface
    static interface IntersectionFactory {
        public NeoShadowsTypes.UvPoint create(NeoShadowsTypes.UvPoint var1, NeoShadowsTypes.UvPoint var2);
    }
}

