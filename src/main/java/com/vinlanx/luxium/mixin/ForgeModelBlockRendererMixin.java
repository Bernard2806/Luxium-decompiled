/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.client.model.lighting.ForgeModelBlockRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.lighting.ForgeModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ForgeModelBlockRenderer.class})
public abstract class ForgeModelBlockRendererMixin {
    @Redirect(method={"render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/lighting/QuadLighter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Z"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"), require=0)
    private static boolean luxium$captureForgeFaces(BlockState state, BlockGetter level, BlockPos pos, Direction face, BlockPos neighbourPos) {
        boolean shouldRender = Block.m_152444_((BlockState)state, (BlockGetter)level, (BlockPos)pos, (Direction)face, (BlockPos)neighbourPos);
        if (shouldRender) {
            ExposedFaceService.get().recordFace(pos, face, level, state);
        }
        return shouldRender;
    }
}

