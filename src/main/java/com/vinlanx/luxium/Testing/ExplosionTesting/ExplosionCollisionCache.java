/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ExplosionCollisionCache {
    private static final int SIZE_X = 72;
    private static final int SIZE_Y = 48;
    private static final int SIZE_Z = 72;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<Snapshot>(new Snapshot(0, 0, 0, new boolean[248832]));

    ExplosionCollisionCache() {
    }

    void rebuild(ClientLevel level, Vec3 center) {
        int minX = Mth.m_14107_((double)center.f_82479_) - 36;
        int minY = Math.max(level.m_141937_(), Mth.m_14107_((double)center.f_82480_) - 8);
        int minZ = Mth.m_14107_((double)center.f_82481_) - 36;
        boolean[] solid = new boolean[248832];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 48; ++y) {
            for (int z = 0; z < 72; ++z) {
                for (int x = 0; x < 72; ++x) {
                    cursor.m_122178_(minX + x, minY + y, minZ + z);
                    solid[ExplosionCollisionCache.index((int)x, (int)y, (int)z)] = level.m_8055_((BlockPos)cursor).m_60815_();
                }
            }
        }
        this.snapshot.set(new Snapshot(minX, minY, minZ, solid));
    }

    boolean isSolid(double worldX, double worldY, double worldZ) {
        Snapshot current = this.snapshot.get();
        int x = Mth.m_14107_((double)worldX) - current.minX;
        int y = Mth.m_14107_((double)worldY) - current.minY;
        int z = Mth.m_14107_((double)worldZ) - current.minZ;
        if (x < 0 || y < 0 || z < 0 || x >= 72 || y >= 48 || z >= 72) {
            return false;
        }
        return current.solid[ExplosionCollisionCache.index(x, y, z)];
    }

    private static int index(int x, int y, int z) {
        return (y * 72 + z) * 72 + x;
    }

    private record Snapshot(int minX, int minY, int minZ, boolean[] solid) {
    }
}

