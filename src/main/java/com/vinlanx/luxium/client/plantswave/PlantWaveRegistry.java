/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.DoublePlantBlock
 *  net.minecraft.world.level.block.MangrovePropaguleBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.vinlanx.luxium.client.plantswave;

import com.vinlanx.luxium.client.plantswave.PlantWaveProfile;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

public final class PlantWaveRegistry {
    private static final Set<Block> LEAVES = Set.of(Blocks.f_50050_, Blocks.f_50051_, Blocks.f_50052_, Blocks.f_50053_, Blocks.f_50054_, Blocks.f_271115_, Blocks.f_50055_, Blocks.f_220838_, Blocks.f_152470_, Blocks.f_152471_);
    private static final Set<Block> ROOTED = Set.of(Blocks.f_50746_, Blocks.f_50747_, Blocks.f_50748_, Blocks.f_50749_, Blocks.f_50750_, Blocks.f_271334_, Blocks.f_50751_, Blocks.f_50034_, Blocks.f_50035_, Blocks.f_50036_, Blocks.f_50111_, Blocks.f_271329_, Blocks.f_50112_, Blocks.f_50113_, Blocks.f_50114_, Blocks.f_50115_, Blocks.f_50116_, Blocks.f_50117_, Blocks.f_50118_, Blocks.f_50119_, Blocks.f_50120_, Blocks.f_50121_, Blocks.f_50070_, Blocks.f_50071_, Blocks.f_50072_, Blocks.f_50073_, Blocks.f_50092_, Blocks.f_50249_, Blocks.f_50250_, Blocks.f_50444_, Blocks.f_50200_, Blocks.f_50189_, Blocks.f_50190_, Blocks.f_50187_, Blocks.f_50188_, Blocks.f_50359_, Blocks.f_50360_, Blocks.f_50355_, Blocks.f_50356_, Blocks.f_50357_, Blocks.f_50358_, Blocks.f_50570_, Blocks.f_50685_, Blocks.f_50691_, Blocks.f_50700_, Blocks.f_50693_, Blocks.f_50654_, Blocks.f_50694_, Blocks.f_152541_, Blocks.f_152542_, Blocks.f_152547_, Blocks.f_276665_, Blocks.f_276668_, Blocks.f_271410_);
    private static final Set<Block> AQUATIC = Set.of(Blocks.f_50037_, Blocks.f_50038_, Blocks.f_50575_, Blocks.f_50576_, Blocks.f_50196_);

    private PlantWaveRegistry() {
    }

    public static PlantWaveProfile resolve(BlockGetter level, BlockPos pos, BlockState state) {
        Block block = state.m_60734_();
        if (LEAVES.contains(block)) {
            return PlantWaveProfile.LEAVES;
        }
        if (block == Blocks.f_220831_) {
            return (Boolean)state.m_61143_((Property)MangrovePropaguleBlock.f_221443_) != false ? PlantWaveProfile.HANGING : PlantWaveProfile.ROOTED;
        }
        if (block == Blocks.f_152548_ || block == Blocks.f_152540_) {
            return PlantWaveProfile.HANGING;
        }
        if (PlantWaveRegistry.isHangingChain(block)) {
            Block above = level.m_8055_(pos.m_7494_()).m_60734_();
            return PlantWaveRegistry.sameHangingFamily(block, above) ? PlantWaveProfile.FREE : PlantWaveProfile.HANGING;
        }
        if (PlantWaveRegistry.isUpwardChain(block)) {
            Block below = level.m_8055_(pos.m_7495_()).m_60734_();
            return PlantWaveRegistry.sameUpwardFamily(block, below) ? PlantWaveProfile.FREE : PlantWaveProfile.ROOTED;
        }
        if (AQUATIC.contains(block)) {
            if (block == Blocks.f_50575_ || block == Blocks.f_50576_) {
                Block below = level.m_8055_(pos.m_7495_()).m_60734_();
                return PlantWaveRegistry.isKelp(below) ? PlantWaveProfile.AQUATIC_FREE : PlantWaveProfile.AQUATIC_ROOTED;
            }
            if (state.m_61138_((Property)DoublePlantBlock.f_52858_) && state.m_61143_((Property)DoublePlantBlock.f_52858_) == DoubleBlockHalf.UPPER) {
                return PlantWaveProfile.AQUATIC_FREE;
            }
            if (block == Blocks.f_50196_) {
                return PlantWaveProfile.AQUATIC_FREE;
            }
            return PlantWaveProfile.AQUATIC_ROOTED;
        }
        if (state.m_61138_((Property)DoublePlantBlock.f_52858_)) {
            return state.m_61143_((Property)DoublePlantBlock.f_52858_) == DoubleBlockHalf.UPPER ? PlantWaveProfile.FREE : PlantWaveProfile.ROOTED;
        }
        if (ROOTED.contains(block)) {
            return PlantWaveProfile.ROOTED;
        }
        return PlantWaveProfile.NONE;
    }

    private static boolean isUpwardChain(Block block) {
        return block == Blocks.f_50130_ || block == Blocks.f_50571_ || block == Blocks.f_50704_ || block == Blocks.f_50653_ || block == Blocks.f_152545_ || block == Blocks.f_152546_;
    }

    private static boolean sameUpwardFamily(Block first, Block second) {
        if (first == Blocks.f_50130_) {
            return second == Blocks.f_50130_;
        }
        if (first == Blocks.f_50571_) {
            return second == Blocks.f_50571_ || second == Blocks.f_50570_;
        }
        if (first == Blocks.f_50704_ || first == Blocks.f_50653_) {
            return second == Blocks.f_50704_ || second == Blocks.f_50653_;
        }
        if (first == Blocks.f_152545_ || first == Blocks.f_152546_) {
            return second == Blocks.f_152545_ || second == Blocks.f_152546_;
        }
        return false;
    }

    private static boolean isHangingChain(Block block) {
        return block == Blocks.f_50191_ || block == Blocks.f_50702_ || block == Blocks.f_50703_ || block == Blocks.f_152538_ || block == Blocks.f_152539_;
    }

    private static boolean sameHangingFamily(Block first, Block second) {
        if (first == Blocks.f_50191_) {
            return second == Blocks.f_50191_;
        }
        if (first == Blocks.f_50702_ || first == Blocks.f_50703_) {
            return second == Blocks.f_50702_ || second == Blocks.f_50703_;
        }
        if (first == Blocks.f_152538_ || first == Blocks.f_152539_) {
            return second == Blocks.f_152538_ || second == Blocks.f_152539_;
        }
        return false;
    }

    private static boolean isKelp(Block block) {
        return block == Blocks.f_50575_ || block == Blocks.f_50576_;
    }
}

