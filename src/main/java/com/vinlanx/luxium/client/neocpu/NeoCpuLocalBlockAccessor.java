/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShape;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShapeCache;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class NeoCpuLocalBlockAccessor {
    private final ClientLevel level;
    private final NeoCpuShadowShapeCache shapeCache;
    private final Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap();
    private final Long2ObjectOpenHashMap<NeoCpuShadowShape> shapes = new Long2ObjectOpenHashMap();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final int minBuildHeight;
    private final int maxBuildHeight;

    NeoCpuLocalBlockAccessor(ClientLevel level, NeoCpuShadowShapeCache shapeCache) {
        this.level = level;
        this.shapeCache = shapeCache;
        this.minBuildHeight = level.m_141937_();
        this.maxBuildHeight = level.m_151558_();
        this.states.defaultReturnValue(null);
        this.shapes.defaultReturnValue(null);
    }

    boolean isLoaded(int x, int z) {
        this.cursor.m_122178_(x, this.minBuildHeight, z);
        return this.level.m_46805_((BlockPos)this.cursor);
    }

    boolean isInsideBuildHeight(int y) {
        return y >= this.minBuildHeight && y < this.maxBuildHeight;
    }

    BlockState stateAt(int x, int y, int z) {
        long key = BlockPos.m_121882_((int)x, (int)y, (int)z);
        BlockState cached = (BlockState)this.states.get(key);
        if (cached != null) {
            return cached;
        }
        this.cursor.m_122178_(x, y, z);
        BlockState state = this.level.m_46805_((BlockPos)this.cursor) ? this.level.m_8055_((BlockPos)this.cursor) : Blocks.f_50016_.m_49966_();
        this.states.put(key, (Object)state);
        return state;
    }

    NeoCpuShadowShape shapeAt(int x, int y, int z) {
        long key = BlockPos.m_121882_((int)x, (int)y, (int)z);
        NeoCpuShadowShape cached = (NeoCpuShadowShape)this.shapes.get(key);
        if (cached != null) {
            return cached;
        }
        BlockState state = this.stateAt(x, y, z);
        this.cursor.m_122178_(x, y, z);
        NeoCpuShadowShape shape = state.m_60795_() ? NeoCpuShadowShape.EMPTY : this.shapeCache.get(this.level, (BlockPos)this.cursor, state);
        this.shapes.put(key, (Object)shape);
        return shape;
    }

    NeoCpuShadowShape shapeAt(long key) {
        return this.shapeAt(BlockPos.m_121983_((long)key), BlockPos.m_122008_((long)key), BlockPos.m_122015_((long)key));
    }

    BlockState stateAt(long key) {
        return this.stateAt(BlockPos.m_121983_((long)key), BlockPos.m_122008_((long)key), BlockPos.m_122015_((long)key));
    }

    boolean blocksFlood(long key, BlockState state) {
        if (state.m_60795_() || !state.m_60815_()) {
            return false;
        }
        this.cursor.m_122178_(BlockPos.m_121983_((long)key), BlockPos.m_122008_((long)key), BlockPos.m_122015_((long)key));
        return state.m_60838_((BlockGetter)this.level, (BlockPos)this.cursor);
    }
}

