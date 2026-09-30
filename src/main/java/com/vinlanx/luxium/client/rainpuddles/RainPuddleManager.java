/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.rainpuddles;

import com.vinlanx.luxium.client.rainpuddles.RainPuddleInstance;
import com.vinlanx.luxium.client.rainpuddles.RainPuddlePatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class RainPuddleManager {
    private static final RainPuddleManager INSTANCE = new RainPuddleManager();
    private static final String RESOURCE_ROOT = "rain";
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 10;
    private static final int MAX_PLACEMENTS_PER_DIMENSION = 48;
    private static final double SURFACE_EPSILON = 0.0025;
    private final Map<ResourceKey<Level>, List<RainPuddleInstance>> byDimension = new HashMap<ResourceKey<Level>, List<RainPuddleInstance>>();
    private ClientLevel activeLevel;

    private RainPuddleManager() {
    }

    public static RainPuddleManager get() {
        return INSTANCE;
    }

    @Nullable
    public synchronized RainPuddleInstance placeRandom(Minecraft mc, BlockPos center, int requestedSize) {
        ClientLevel level = mc.f_91073_;
        if (level == null) {
            return null;
        }
        this.ensureLevel(level);
        List<ResourceLocation> maps = this.findDepthMaps(mc);
        if (maps.isEmpty()) {
            return null;
        }
        int size = Math.max(1, Math.min(10, requestedSize));
        int minX = center.m_123341_() - (size - 1) / 2;
        int minZ = center.m_123343_() - (size - 1) / 2;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        List<RainPuddlePatch> patches = this.buildPatches(level, center.m_123342_(), minX, minZ, size);
        if (patches.isEmpty()) {
            return null;
        }
        ResourceLocation selected = maps.get(ThreadLocalRandom.current().nextInt(maps.size()));
        RainPuddleInstance instance = new RainPuddleInstance(selected, size, minX, minZ, maxX, maxZ, patches);
        List placements = this.byDimension.computeIfAbsent((ResourceKey<Level>)level.m_46472_(), ignored -> new ArrayList());
        placements.removeIf(existing -> existing.intersects(minX, minZ, maxX, maxZ));
        placements.add(instance);
        while (placements.size() > 48) {
            placements.remove(0);
        }
        return instance;
    }

    public synchronized List<RainPuddleInstance> snapshot(@Nullable ClientLevel level) {
        if (level == null) {
            return List.of();
        }
        this.ensureLevel(level);
        List<RainPuddleInstance> placements = this.byDimension.get(level.m_46472_());
        return placements == null || placements.isEmpty() ? List.of() : List.copyOf(placements);
    }

    public synchronized boolean hasPuddles(@Nullable ClientLevel level) {
        if (level == null) {
            return false;
        }
        this.ensureLevel(level);
        List<RainPuddleInstance> placements = this.byDimension.get(level.m_46472_());
        return placements != null && !placements.isEmpty();
    }

    private void ensureLevel(ClientLevel level) {
        if (this.activeLevel != level) {
            this.byDimension.clear();
            this.activeLevel = level;
        }
    }

    private List<ResourceLocation> findDepthMaps(Minecraft mc) {
        Map resources = mc.m_91098_().m_214159_(RESOURCE_ROOT, id -> id.m_135827_().equals("luxium") && id.m_135815_().toLowerCase(Locale.ROOT).endsWith(".png"));
        if (resources.isEmpty()) {
            return List.of();
        }
        ArrayList<ResourceLocation> ids = new ArrayList<ResourceLocation>(resources.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::m_135815_));
        return ids;
    }

    private List<RainPuddlePatch> buildPatches(ClientLevel level, int blockY, int minX, int minZ, int size) {
        ArrayList<RainPuddlePatch> patches = new ArrayList<RainPuddlePatch>();
        CollisionContext collision = CollisionContext.m_82749_();
        for (int dz = 0; dz < size; ++dz) {
            for (int dx = 0; dx < size; ++dx) {
                VoxelShape shape;
                BlockPos pos = new BlockPos(minX + dx, blockY, minZ + dz);
                BlockState state = level.m_8055_(pos);
                if (state.m_60795_() || (shape = state.m_60651_((BlockGetter)level, pos, collision)).m_83281_()) continue;
                float blockU0 = (float)dx / (float)size;
                float blockV0 = (float)dz / (float)size;
                float blockUScale = 1.0f / (float)size;
                float blockVScale = 1.0f / (float)size;
                for (AABB box : shape.m_83299_()) {
                    if (box.m_82362_() <= 1.0E-4 || box.m_82385_() <= 1.0E-4 || box.f_82292_ <= 1.0E-4) continue;
                    double worldMinX = (double)pos.m_123341_() + box.f_82288_;
                    double worldMaxX = (double)pos.m_123341_() + box.f_82291_;
                    double worldMinZ = (double)pos.m_123343_() + box.f_82290_;
                    double worldMaxZ = (double)pos.m_123343_() + box.f_82293_;
                    double worldY = (double)pos.m_123342_() + box.f_82292_ + 0.0025;
                    float u0 = blockU0 + (float)box.f_82288_ * blockUScale;
                    float u1 = blockU0 + (float)box.f_82291_ * blockUScale;
                    float v0 = blockV0 + (float)box.f_82290_ * blockVScale;
                    float v1 = blockV0 + (float)box.f_82293_ * blockVScale;
                    patches.add(new RainPuddlePatch(worldMinX, worldMaxX, worldY, worldMinZ, worldMaxZ, u0, v0, u1, v1));
                }
            }
        }
        return patches;
    }
}

