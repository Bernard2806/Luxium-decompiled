/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.ItemBlockRenderTypes
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.client.ChunkRenderTypeSet
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.Config;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ItemBlockRenderTypes.class})
public abstract class ItemBlockRenderTypesMixin {
    @Unique
    private static final ChunkRenderTypeSet LUXIUM$OPAQUE_ICE_LAYERS = ChunkRenderTypeSet.of((RenderType[])new RenderType[]{RenderType.m_110451_()});

    @Unique
    private static boolean luxium$useOpaqueIce(BlockState state) {
        return state.m_60734_() == Blocks.f_50126_ && Config.isFeatureEnabled(Config.CLIENT.opaqueIceEnabled);
    }

    @Inject(method={"getRenderLayers"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private static void luxium$overrideIceRenderLayers(BlockState state, CallbackInfoReturnable<ChunkRenderTypeSet> cir) {
        if (ItemBlockRenderTypesMixin.luxium$useOpaqueIce(state)) {
            cir.setReturnValue((Object)LUXIUM$OPAQUE_ICE_LAYERS);
        }
    }

    @Inject(method={"getChunkRenderType"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$overrideLegacyIceChunkLayer(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        if (ItemBlockRenderTypesMixin.luxium$useOpaqueIce(state)) {
            cir.setReturnValue((Object)RenderType.m_110451_());
        }
    }

    @Inject(method={"getMovingBlockRenderType"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$overrideMovingIceChunkLayer(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        if (ItemBlockRenderTypesMixin.luxium$useOpaqueIce(state)) {
            cir.setReturnValue((Object)RenderType.m_110451_());
        }
    }
}

