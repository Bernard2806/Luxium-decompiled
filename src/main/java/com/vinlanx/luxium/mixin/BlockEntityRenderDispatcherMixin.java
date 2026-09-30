/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockEntityRenderDispatcher.class})
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private <E extends BlockEntity> void luxium$skipShadowCaptureBlockEntities(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (NeoShadowsEngine.isShadowCapturePass() && !NeoShadowsEngine.shouldRenderDynamicCastersInShadowPass()) {
            ci.cancel();
        }
    }
}

