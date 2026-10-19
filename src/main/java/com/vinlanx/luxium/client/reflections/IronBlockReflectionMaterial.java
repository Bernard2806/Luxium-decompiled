package com.vinlanx.luxium.client.reflections;

import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public final class IronBlockReflectionMaterial
implements ReflectionMaterial {
    @Override
    public boolean supports(BlockState state) {
        return state.m_60713_(Blocks.f_50075_);
    }

    @Override
    public float getBaseAlpha() {
        return 0.85f;
    }
}
