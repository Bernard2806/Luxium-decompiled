/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.neocpu;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NeoCpuShadowTypes {
    static final double EPSILON = 1.0E-6;
    static final Direction[] FACES = Direction.values();

    private NeoCpuShadowTypes() {
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record StaticBuildOutput(List<PerSourceStatic> sources) {
        public static final StaticBuildOutput EMPTY = new StaticBuildOutput(List.of());

        public boolean isEmpty() {
            return this.sources.isEmpty();
        }
    }

    public record ReceiverWithStrength(ReceiverSurface receiver, float strength) {
    }

    public record ReceiverGroupEntry(List<ReceiverWithStrength> receivers) {
        public boolean isEmpty() {
            return this.receivers.isEmpty();
        }

        public ReceiverSurface referenceReceiver() {
            return this.receivers.isEmpty() ? null : this.receivers.get(0).receiver();
        }
    }

    public record PerSourceStatic(long sourceKey, int emission, Vec3 source, List<ReceiverGroupEntry> groups, List<ShadowPolygon> blockPolygons) {
    }

    public static final class ShadowPolygon {
        public final double[] vertices;
        public final AABB bounds;
        public final double centerX;
        public final double centerY;
        public final double centerZ;
        public final double normalX;
        public final double normalY;
        public final double normalZ;
        public final float strength;

        public ShadowPolygon(double[] vertices, double normalX, double normalY, double normalZ, float strength) {
            this.vertices = vertices;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.strength = strength;
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            double sumX = 0.0;
            double sumY = 0.0;
            double sumZ = 0.0;
            int count = 0;
            for (int i = 0; i < vertices.length; i += 3) {
                double x = vertices[i];
                double y = vertices[i + 1];
                double z = vertices[i + 2];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
                sumX += x;
                sumY += y;
                sumZ += z;
                ++count;
            }
            this.centerX = count == 0 ? 0.0 : sumX / (double)count;
            this.centerY = count == 0 ? 0.0 : sumY / (double)count;
            this.centerZ = count == 0 ? 0.0 : sumZ / (double)count;
            this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).m_82400_(0.02);
        }

        public boolean facesCamera(double cameraX, double cameraY, double cameraZ) {
            double dx = cameraX - this.centerX;
            double dy = cameraY - this.centerY;
            double dz = cameraZ - this.centerZ;
            return dx * this.normalX + dy * this.normalY + dz * this.normalZ > -0.01;
        }
    }

    public record Uv(double u, double v) {
        public boolean closeTo(Uv other) {
            return Math.abs(this.u - other.u) <= 1.0E-5 && Math.abs(this.v - other.v) <= 1.0E-5;
        }
    }

    public record SectionGeometry(long sectionKey, long chunkKey, ReceiverSurface[] receivers, Occluder[][] occluderGrid) {
        static final SectionGeometry EMPTY = new SectionGeometry(0L, 0L, new ReceiverSurface[0], null);

        public boolean isEmpty() {
            return this.receivers.length == 0 && this.occluderGrid == null;
        }

        public Occluder[] occludersAt(int localX, int localY, int localZ) {
            if (this.occluderGrid == null) {
                return null;
            }
            return this.occluderGrid[localY << 8 | localZ << 4 | localX];
        }
    }

    public record ReceiverSurface(long blockKey, Direction face, double plane, double minU, double maxU, double minV, double maxV, double centerX, double centerY, double centerZ, double normalX, double normalY, double normalZ) {
        public double width() {
            return this.maxU - this.minU;
        }

        public double height() {
            return this.maxV - this.minV;
        }

        public double distanceToSourceSq(double sourceX, double sourceY, double sourceZ) {
            double closestY;
            double closestX;
            double closestZ = switch (this.face) {
                case Direction.UP, Direction.DOWN -> {
                    closestX = NeoCpuShadowTypes.clamp(sourceX, this.minU, this.maxU);
                    closestY = this.plane;
                    yield NeoCpuShadowTypes.clamp(sourceZ, this.minV, this.maxV);
                }
                case Direction.NORTH, Direction.SOUTH -> {
                    closestX = NeoCpuShadowTypes.clamp(sourceX, this.minU, this.maxU);
                    closestY = NeoCpuShadowTypes.clamp(sourceY, this.minV, this.maxV);
                    yield this.plane;
                }
                case Direction.WEST, Direction.EAST -> {
                    closestX = this.plane;
                    closestY = NeoCpuShadowTypes.clamp(sourceY, this.minV, this.maxV);
                    yield NeoCpuShadowTypes.clamp(sourceZ, this.minU, this.maxU);
                }
                default -> throw new IllegalStateException("Unexpected face: " + this.face);
            };
            double dx = closestX - sourceX;
            double dy = closestY - sourceY;
            double dz = closestZ - sourceZ;
            return dx * dx + dy * dy + dz * dz;
        }

        public boolean facesSource(double sourceX, double sourceY, double sourceZ) {
            double dx = sourceX - this.centerX;
            double dy = sourceY - this.centerY;
            double dz = sourceZ - this.centerZ;
            return dx * this.normalX + dy * this.normalY + dz * this.normalZ > 1.0E-6;
        }

        public double sampleX(double u, double v) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> u;
                case Direction.WEST, Direction.EAST -> this.plane;
            };
        }

        public double sampleY(double u, double v) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> this.plane;
                case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> v;
            };
        }

        public double sampleZ(double u, double v) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> v;
                case Direction.NORTH, Direction.SOUTH -> this.plane;
                case Direction.WEST, Direction.EAST -> u;
            };
        }

        public Uv worldToUv(double x, double y, double z) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> new Uv(x, z);
                case Direction.NORTH, Direction.SOUTH -> new Uv(x, y);
                case Direction.WEST, Direction.EAST -> new Uv(z, y);
            };
        }
    }

    public static final class QuadOccluder
    implements Occluder {
        private final Vec3[] vertices;
        private final AABB bounds;
        private final long blockKey;
        private final float opacity;

        public QuadOccluder(Vec3[] vertices, long blockKey, float opacity) {
            this.vertices = (Vec3[])vertices.clone();
            this.blockKey = blockKey;
            this.opacity = Mth.m_14036_((float)opacity, (float)0.0f, (float)1.0f);
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (Vec3 vertex : this.vertices) {
                minX = Math.min(minX, vertex.f_82479_);
                minY = Math.min(minY, vertex.f_82480_);
                minZ = Math.min(minZ, vertex.f_82481_);
                maxX = Math.max(maxX, vertex.f_82479_);
                maxY = Math.max(maxY, vertex.f_82480_);
                maxZ = Math.max(maxZ, vertex.f_82481_);
            }
            this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).m_82400_(0.002);
        }

        public QuadOccluder(Vec3[] ownedVertices, AABB precomputedBounds, long blockKey, float opacity) {
            this.vertices = ownedVertices;
            this.bounds = precomputedBounds;
            this.blockKey = blockKey;
            this.opacity = Mth.m_14036_((float)opacity, (float)0.0f, (float)1.0f);
        }

        public Vec3[] vertices() {
            return this.vertices;
        }

        public AABB bounds() {
            return this.bounds;
        }

        @Override
        public long blockKey() {
            return this.blockKey;
        }

        @Override
        public float opacity() {
            return this.opacity;
        }
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, long blockKey, float opacity) implements Occluder
    {
        public double centerX() {
            return (this.minX + this.maxX) * 0.5;
        }

        public double centerY() {
            return (this.minY + this.maxY) * 0.5;
        }

        public double centerZ() {
            return (this.minZ + this.maxZ) * 0.5;
        }
    }

    /*
     * Uses 'sealed' constructs - enablewith --sealed true
     */
    public static interface Occluder {
        public long blockKey();

        public float opacity();
    }
}

