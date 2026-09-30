/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class NeoCpuShadowShape {
    private static final NeoCpuShadowTypes.Box[] EMPTY_BOXES = new NeoCpuShadowTypes.Box[0];
    private static final LocalQuadTemplate[] EMPTY_QUADS = new LocalQuadTemplate[0];
    private static final ReceiverTemplate[] EMPTY_RECEIVERS = new ReceiverTemplate[0];
    static final NeoCpuShadowShape EMPTY = new NeoCpuShadowShape(EMPTY_BOXES, EMPTY_QUADS, EMPTY_RECEIVERS, false);
    private final NeoCpuShadowTypes.Box[] localBoxes;
    private final LocalQuadTemplate[] localQuads;
    private final ReceiverTemplate[] receivers;
    private final boolean floodBlocking;

    NeoCpuShadowShape(NeoCpuShadowTypes.Box[] localBoxes, LocalQuadTemplate[] localQuads, ReceiverTemplate[] receivers, boolean floodBlocking) {
        this.localBoxes = localBoxes;
        this.localQuads = localQuads;
        this.receivers = receivers;
        this.floodBlocking = floodBlocking;
    }

    boolean isEmpty() {
        return this.localBoxes.length == 0 && this.localQuads.length == 0 && this.receivers.length == 0;
    }

    boolean hasOccluders() {
        return this.localBoxes.length > 0 || this.localQuads.length > 0;
    }

    boolean blocksFlood() {
        return this.floodBlocking;
    }

    ReceiverTemplate[] receivers() {
        return this.receivers;
    }

    void appendReceiversForFace(long blockKey, int blockX, int blockY, int blockZ, Direction face, ArrayList<NeoCpuShadowTypes.ReceiverSurface> out) {
        for (ReceiverTemplate receiver : this.receivers) {
            if (receiver.face != face) continue;
            out.add(receiver.instantiate(blockKey, blockX, blockY, blockZ));
        }
    }

    NeoCpuShadowTypes.Occluder[] instantiateOccluders(long blockKey, int blockX, int blockY, int blockZ) {
        if (!this.hasOccluders()) {
            return null;
        }
        if (this.localQuads.length > 0) {
            NeoCpuShadowTypes.Occluder[] result = new NeoCpuShadowTypes.Occluder[this.localQuads.length];
            for (int i = 0; i < this.localQuads.length; ++i) {
                result[i] = this.localQuads[i].instantiate(blockKey, blockX, blockY, blockZ);
            }
            return result;
        }
        NeoCpuShadowTypes.Occluder[] result = new NeoCpuShadowTypes.Occluder[this.localBoxes.length];
        for (int i = 0; i < this.localBoxes.length; ++i) {
            NeoCpuShadowTypes.Box box = this.localBoxes[i];
            result[i] = new NeoCpuShadowTypes.Box((double)blockX + box.minX(), (double)blockY + box.minY(), (double)blockZ + box.minZ(), (double)blockX + box.maxX(), (double)blockY + box.maxY(), (double)blockZ + box.maxZ(), blockKey, box.opacity());
        }
        return result;
    }

    record LocalQuadTemplate(Vec3[] vertices, float opacity) {
        NeoCpuShadowTypes.QuadOccluder instantiate(long blockKey, int blockX, int blockY, int blockZ) {
            Vec3[] worldVertices = new Vec3[this.vertices.length];
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < this.vertices.length; ++i) {
                Vec3 vertex = this.vertices[i];
                double wx = vertex.f_82479_ + (double)blockX;
                double wy = vertex.f_82480_ + (double)blockY;
                double wz = vertex.f_82481_ + (double)blockZ;
                worldVertices[i] = new Vec3(wx, wy, wz);
                if (wx < minX) {
                    minX = wx;
                }
                if (wx > maxX) {
                    maxX = wx;
                }
                if (wy < minY) {
                    minY = wy;
                }
                if (wy > maxY) {
                    maxY = wy;
                }
                if (wz < minZ) {
                    minZ = wz;
                }
                if (!(wz > maxZ)) continue;
                maxZ = wz;
            }
            AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).m_82400_(0.002);
            return new NeoCpuShadowTypes.QuadOccluder(worldVertices, bounds, blockKey, this.opacity);
        }
    }

    record ReceiverTemplate(Direction face, double plane, double minU, double maxU, double minV, double maxV) {
        NeoCpuShadowTypes.ReceiverSurface instantiate(long blockKey, int blockX, int blockY, int blockZ) {
            double worldPlane = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> (double)blockY + this.plane;
                case Direction.NORTH, Direction.SOUTH -> (double)blockZ + this.plane;
                case Direction.WEST, Direction.EAST -> (double)blockX + this.plane;
            };
            double worldMinU = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> (double)blockX + this.minU;
                case Direction.WEST, Direction.EAST -> (double)blockZ + this.minU;
            };
            double worldMaxU = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> (double)blockX + this.maxU;
                case Direction.WEST, Direction.EAST -> (double)blockZ + this.maxU;
            };
            double worldMinV = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> (double)blockZ + this.minV;
                case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> (double)blockY + this.minV;
            };
            double worldMaxV = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> (double)blockZ + this.maxV;
                case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> (double)blockY + this.maxV;
            };
            double centerU = (worldMinU + worldMaxU) * 0.5;
            double centerV = (worldMinV + worldMaxV) * 0.5;
            double centerX = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> centerU;
                case Direction.WEST, Direction.EAST -> worldPlane;
            };
            double centerY = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> worldPlane;
                case Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> centerV;
            };
            double centerZ = switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> centerV;
                case Direction.NORTH, Direction.SOUTH -> worldPlane;
                case Direction.WEST, Direction.EAST -> centerU;
            };
            return new NeoCpuShadowTypes.ReceiverSurface(blockKey, this.face, worldPlane, worldMinU, worldMaxU, worldMinV, worldMaxV, centerX, centerY, centerZ, this.face.m_122429_(), this.face.m_122430_(), this.face.m_122431_());
        }
    }
}

