/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuFloodBoundaryFace;
import com.vinlanx.luxium.client.neocpu.NeoCpuLocalBlockAccessor;
import com.vinlanx.luxium.client.neocpu.NeoCpuReceiverFaceSet;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShape;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import com.vinlanx.luxium.rtx.LightRtMath;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

final class NeoCpuFloodLightVolume {
    private static final int MIN_VISITED_CELLS = 16384;
    private static final int MAX_VISITED_CELLS = 262144;
    private static final Direction[] ALL_FACES = Direction.values();
    private final LongOpenHashSet visitedCells;
    private final List<NeoCpuFloodBoundaryFace> boundaryFaces;

    private NeoCpuFloodLightVolume(LongOpenHashSet visitedCells, List<NeoCpuFloodBoundaryFace> boundaryFaces) {
        this.visitedCells = visitedCells;
        this.boundaryFaces = boundaryFaces;
    }

    static NeoCpuFloodLightVolume build(NeoCpuLocalBlockAccessor accessor, long sourceKey, int emission) {
        double radius = LightRtMath.getBlockShadowRadius(emission);
        double radiusSq = radius * radius;
        int maxVisitedCells = NeoCpuFloodLightVolume.getVisitedCellLimit(radius);
        LongOpenHashSet visited = new LongOpenHashSet();
        NeoCpuReceiverFaceSet receiverFaces = new NeoCpuReceiverFaceSet();
        ArrayDeque<Long> queue = new ArrayDeque<Long>();
        ArrayList<NeoCpuShadowTypes.ReceiverSurface> scratchReceivers = new ArrayList<NeoCpuShadowTypes.ReceiverSurface>(8);
        visited.add(sourceKey);
        queue.add(sourceKey);
        double sourceX = (double)BlockPos.m_121983_((long)sourceKey) + 0.5;
        double sourceY = (double)BlockPos.m_122008_((long)sourceKey) + 0.5;
        double sourceZ = (double)BlockPos.m_122015_((long)sourceKey) + 0.5;
        while (!queue.isEmpty() && visited.size() < maxVisitedCells) {
            long cellKey = (Long)queue.removeFirst();
            int x = BlockPos.m_121983_((long)cellKey);
            int y = BlockPos.m_122008_((long)cellKey);
            int z = BlockPos.m_122015_((long)cellKey);
            for (Direction direction : ALL_FACES) {
                boolean passable;
                int nx = x + direction.m_122429_();
                int ny = y + direction.m_122430_();
                int nz = z + direction.m_122431_();
                long neighborKey = BlockPos.m_121882_((int)nx, (int)ny, (int)nz);
                double dx = (double)nx + 0.5 - sourceX;
                double dy = (double)ny + 0.5 - sourceY;
                double dz = (double)nz + 0.5 - sourceZ;
                if (dx * dx + dy * dy + dz * dz > radiusSq || !accessor.isInsideBuildHeight(ny) || !accessor.isLoaded(nx, nz)) continue;
                NeoCpuShadowShape shape = accessor.shapeAt(neighborKey);
                boolean bl = passable = neighborKey == sourceKey || shape.isEmpty() || !shape.blocksFlood();
                if (!shape.isEmpty()) {
                    int blockX = BlockPos.m_121983_((long)neighborKey);
                    int blockY = BlockPos.m_122008_((long)neighborKey);
                    int blockZ = BlockPos.m_122015_((long)neighborKey);
                    scratchReceivers.clear();
                    shape.appendReceiversForFace(neighborKey, blockX, blockY, blockZ, direction.m_122424_(), scratchReceivers);
                    for (NeoCpuShadowTypes.ReceiverSurface receiver : scratchReceivers) {
                        receiverFaces.add(receiver, receiver.distanceToSourceSq(sourceX, sourceY, sourceZ));
                    }
                }
                if (!passable || !visited.add(neighborKey)) continue;
                queue.addLast(neighborKey);
            }
        }
        return new NeoCpuFloodLightVolume(visited, receiverFaces.faces());
    }

    private static int getVisitedCellLimit(double radius) {
        double sphereVolume = 4.1887902047863905 * radius * radius * radius;
        int estimatedCells = (int)Math.ceil(sphereVolume * 1.1);
        return Math.max(16384, Math.min(262144, estimatedCells));
    }

    LongOpenHashSet visitedCells() {
        return this.visitedCells;
    }

    List<NeoCpuFloodBoundaryFace> boundaryFaces() {
        return this.boundaryFaces;
    }
}

