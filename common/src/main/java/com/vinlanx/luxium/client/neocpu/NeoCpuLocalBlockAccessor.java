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
        this.minBuildHeight = level.getMinBuildHeight();
        this.maxBuildHeight = level.getMaxBuildHeight();
        this.states.defaultReturnValue(null);
        this.shapes.defaultReturnValue(null);
    }

    boolean isLoaded(int x, int z) {
        this.cursor.set(x, this.minBuildHeight, z);
        return this.level.hasChunkAt((BlockPos)this.cursor);
    }

    boolean isInsideBuildHeight(int y) {
        return y >= this.minBuildHeight && y < this.maxBuildHeight;
    }

    BlockState stateAt(int x, int y, int z) {
        long key = BlockPos.asLong((int)x, (int)y, (int)z);
        BlockState cached = (BlockState)this.states.get(key);
        if (cached != null) {
            return cached;
        }
        this.cursor.set(x, y, z);
        BlockState state = this.level.hasChunkAt((BlockPos)this.cursor) ? this.level.getBlockState((BlockPos)this.cursor) : Blocks.AIR.defaultBlockState();
        this.states.put(key, state);
        return state;
    }

    NeoCpuShadowShape shapeAt(int x, int y, int z) {
        long key = BlockPos.asLong((int)x, (int)y, (int)z);
        NeoCpuShadowShape cached = (NeoCpuShadowShape)this.shapes.get(key);
        if (cached != null) {
            return cached;
        }
        BlockState state = this.stateAt(x, y, z);
        this.cursor.set(x, y, z);
        NeoCpuShadowShape shape = state.isAir() ? NeoCpuShadowShape.EMPTY : this.shapeCache.get(this.level, (BlockPos)this.cursor, state);
        this.shapes.put(key, shape);
        return shape;
    }

    NeoCpuShadowShape shapeAt(long key) {
        return this.shapeAt(BlockPos.getX((long)key), BlockPos.getY((long)key), BlockPos.getZ((long)key));
    }

    BlockState stateAt(long key) {
        return this.stateAt(BlockPos.getX((long)key), BlockPos.getY((long)key), BlockPos.getZ((long)key));
    }

    boolean blocksFlood(long key, BlockState state) {
        if (state.isAir() || !state.canOcclude()) {
            return false;
        }
        this.cursor.set(BlockPos.getX((long)key), BlockPos.getY((long)key), BlockPos.getZ((long)key));
        return state.isCollisionShapeFullBlock((BlockGetter)this.level, (BlockPos)this.cursor);
    }
}
