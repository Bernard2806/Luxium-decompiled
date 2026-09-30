/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.reflections;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface ReflectionMaterial {
    public boolean supports(BlockState var1);

    default public boolean shouldReflectFace(Minecraft mc, BlockPos pos, BlockState state, Direction face) {
        return true;
    }

    default public float getBaseAlpha() {
        return 0.65f;
    }

    default public float getMaxDistance() {
        return 40.0f;
    }

    default public float computeAlpha(BlockPos pos, Direction face, Vec3 cameraPos, double distanceSq) {
        float maxDistance = this.getMaxDistance();
        double maxDistSq = maxDistance * maxDistance;
        if (distanceSq >= maxDistSq) {
            return 0.0f;
        }
        float fade = 1.0f - (float)Math.sqrt(distanceSq) / maxDistance;
        return Math.max(0.0f, this.getBaseAlpha() * fade);
    }

    default public float adjustVertexAlpha(float alpha, float projectedU, float projectedV, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        return alpha;
    }

    default public boolean useRaycastVisibility() {
        return true;
    }

    default public boolean useCornerVisibilitySamples() {
        return true;
    }

    default public int getFaceSubdivision() {
        return 1;
    }

    default public float warpU(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        return u;
    }

    default public float warpV(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        return v;
    }

    public static float clampUv(float uv) {
        return Math.max(0.0f, Math.min(1.0f, uv));
    }

    public static float hashToUnit(int x, int y, int z, int w) {
        int h = x * 73428767 ^ y * 912931 ^ z * 19349663 ^ w * 83492791;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        return (float)(h & Integer.MAX_VALUE) / 2.1474836E9f;
    }
}

