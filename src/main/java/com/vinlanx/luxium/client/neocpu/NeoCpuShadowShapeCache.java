package com.vinlanx.luxium.client.neocpu; import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShape; import com.vinlanx.luxium.client.neocpu.NeoCpuShadowShapeBaker; import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
final class NeoCpuShadowShapeCache {
    private final ConcurrentHashMap<BlockState, NeoCpuShadowShape> cache;
    private final NeoCpuShadowShapeBaker baker;
    NeoCpuShadowShapeCache() {
        this.cache = new ConcurrentHashMap();
        this.baker = new NeoCpuShadowShapeBaker(); }

    NeoCpuShadowShape get(ClientLevel level, BlockPos pos, BlockState state) {
        NeoCpuShadowShape shape = this.cache.get(state);
        if (shape != null) {
            return shape;
        }
        NeoCpuShadowShape baked = this.baker.bake(state, level, pos);
        NeoCpuShadowShape existing = this.cache.putIfAbsent(state, baked);
        return existing == null ? baked : existing;
    }

    void clear() {
        this.cache.clear();
    }
}
