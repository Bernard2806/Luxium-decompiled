/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.reflections;

import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class PolishedAndesiteReflectionMaterial
implements ReflectionMaterial {
    @Override
    public boolean supports(BlockState state) {
        return state.m_60713_(Blocks.f_50387_);
    }

    @Override
    public float getBaseAlpha() {
        return 0.65f;
    }

    @Override
    public float warpU(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        float wave = (float)Math.sin(worldX * 1.7f + worldZ * 1.1f + (float)face.m_122411_() * 0.9f) * 0.004f;
        return ReflectionMaterial.clampUv(u + wave);
    }

    @Override
    public float warpV(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        float wave = (float)Math.cos(worldY * 2.0f + worldX * 0.8f + (float)face.m_122411_() * 1.2f) * 0.004f;
        return ReflectionMaterial.clampUv(v + wave);
    }
}

