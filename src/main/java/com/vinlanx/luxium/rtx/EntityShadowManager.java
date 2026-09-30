/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import com.vinlanx.luxium.rtx.TorchRtxState;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityShadowManager {
    private static final EntityShadowManager INSTANCE = new EntityShadowManager();
    private static final double INV_DIR_EPSILON = 1.0E-6;
    private final AtomicReference<LongOpenHashSet> shadowedBlocks = new AtomicReference<LongOpenHashSet>(new LongOpenHashSet());
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private final ExecutorService shadowExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NeoFlood-EntityShadows");
        t.setDaemon(true);
        t.setPriority(4);
        return t;
    });

    private EntityShadowManager() {
    }

    public static EntityShadowManager get() {
        return INSTANCE;
    }

    public boolean isBlockInShadow(long posKey) {
        return this.shadowedBlocks.get().contains(posKey);
    }

    public void updateAsync(ClientLevel level, Vec3 playerPos, double radius) {
        if (!this.isCalculating.compareAndSet(false, true)) {
            return;
        }
        double radiusSq = radius * radius;
        NeoFloodEngine engine = TorchRtxState.get().getEngine();
        CompletableFuture.runAsync(() -> {
            try {
                LongOpenHashSet newShadows = new LongOpenHashSet();
                for (Entity entity : level.m_104735_()) {
                    Vec3 entityCenter;
                    int emission;
                    AABB box;
                    if (entity.m_20238_(playerPos) > radiusSq || (box = entity.m_20191_()) == null) continue;
                    long dominantSourceKey = engine.getDominantSource(entity.m_20183_());
                    if (dominantSourceKey == Long.MIN_VALUE) {
                        long[] closest = TorchRtxState.get().getClosestSources(entity.m_20182_(), 256.0, 1);
                        if (closest.length <= 0) continue;
                        dominantSourceKey = closest[0];
                    }
                    if ((emission = TorchRtxState.get().getSourceEmission(dominantSourceKey)) <= 0) {
                        emission = 15;
                    }
                    double maxLightDist = LightRtMath.getMaxDistance(emission);
                    Vec3 sourcePos = Vec3.m_82512_((Vec3i)BlockPos.m_122022_((long)dominantSourceKey));
                    double distToEntity = sourcePos.m_82554_(entityCenter = entity.m_20182_().m_82520_(0.0, (double)entity.m_20206_() / 2.0, 0.0));
                    if (distToEntity >= maxLightDist) continue;
                    double remainingDist = maxLightDist - distToEntity;
                    this.castShadowVolume(sourcePos, box, remainingDist, newShadows);
                }
                this.applyShadowDiffAndNotify(newShadows);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.isCalculating.set(false);
            }
        }, this.shadowExecutor);
    }

    private void castShadowVolume(Vec3 lightPos, AABB entityBox, double castDistance, LongOpenHashSet outShadows) {
        Vec3 entityCenter = entityBox.m_82399_();
        Vec3 dir = entityCenter.m_82546_(lightPos).m_82541_();
        Vec3 shadowEnd = entityCenter.m_82549_(dir.m_82490_(castDistance));
        AABB shadowBounds = entityBox.m_82367_(new AABB(shadowEnd, shadowEnd)).m_82400_(0.5);
        int minX = Mth.m_14107_((double)shadowBounds.f_82288_);
        int maxX = Mth.m_14165_((double)shadowBounds.f_82291_);
        int minY = Mth.m_14107_((double)shadowBounds.f_82289_);
        int maxY = Mth.m_14165_((double)shadowBounds.f_82292_);
        int minZ = Mth.m_14107_((double)shadowBounds.f_82290_);
        int maxZ = Mth.m_14165_((double)shadowBounds.f_82293_);
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    double bx = (double)x + 0.5;
                    double by = (double)y + 0.5;
                    double bz = (double)z + 0.5;
                    if (!EntityShadowManager.intersect(bx, by, bz, lightPos.f_82479_, lightPos.f_82480_, lightPos.f_82481_, entityBox)) continue;
                    outShadows.add(BlockPos.m_121882_((int)x, (int)y, (int)z));
                }
            }
        }
    }

    private void applyShadowDiffAndNotify(LongOpenHashSet newShadows) {
        LongOpenHashSet oldShadows = this.shadowedBlocks.getAndSet(newShadows);
        LongOpenHashSet blocksToUpdate = new LongOpenHashSet();
        LongIterator oldIt = oldShadows.iterator();
        while (oldIt.hasNext()) {
            long k = oldIt.nextLong();
            if (newShadows.contains(k)) continue;
            blocksToUpdate.add(k);
        }
        LongIterator newIt = newShadows.iterator();
        while (newIt.hasNext()) {
            long k = newIt.nextLong();
            if (oldShadows.contains(k)) continue;
            blocksToUpdate.add(k);
        }
        if (!blocksToUpdate.isEmpty()) {
            Minecraft.m_91087_().execute(() -> {
                if (Minecraft.m_91087_().f_91060_ != null) {
                    LongIterator updateIt = blocksToUpdate.iterator();
                    while (updateIt.hasNext()) {
                        long k = updateIt.nextLong();
                        int x = BlockPos.m_121983_((long)k);
                        int y = BlockPos.m_122008_((long)k);
                        int z = BlockPos.m_122015_((long)k);
                        Minecraft.m_91087_().f_91060_.m_109494_(x, y, z, x, y, z);
                    }
                }
            });
        }
    }

    private static boolean intersect(double ox, double oy, double oz, double lx, double ly, double lz, AABB box) {
        double dirX = lx - ox;
        double dirY = ly - oy;
        double dirZ = lz - oz;
        if (dirX * dirX + dirY * dirY + dirZ * dirZ < 1.0E-4) {
            return false;
        }
        double invDirX = 1.0 / (Math.abs(dirX) < 1.0E-6 ? (dirX < 0.0 ? -1.0E-6 : 1.0E-6) : dirX);
        double invDirY = 1.0 / (Math.abs(dirY) < 1.0E-6 ? (dirY < 0.0 ? -1.0E-6 : 1.0E-6) : dirY);
        double invDirZ = 1.0 / (Math.abs(dirZ) < 1.0E-6 ? (dirZ < 0.0 ? -1.0E-6 : 1.0E-6) : dirZ);
        double t1 = (box.f_82288_ - ox) * invDirX;
        double t2 = (box.f_82291_ - ox) * invDirX;
        double t3 = (box.f_82289_ - oy) * invDirY;
        double t4 = (box.f_82292_ - oy) * invDirY;
        double t5 = (box.f_82290_ - oz) * invDirZ;
        double t6 = (box.f_82293_ - oz) * invDirZ;
        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        return tmax >= 0.0 && tmin <= tmax && tmin <= 1.0;
    }

    public void clear() {
        this.shadowedBlocks.set(new LongOpenHashSet());
    }
}

