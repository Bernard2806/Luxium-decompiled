/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.MultiPlayerGameMode
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.phys.BlockHitResult
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.rtx.PlayerActionContext;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={MultiPlayerGameMode.class})
public class MultiPlayerGameModeMixin {
    @Inject(method={"startDestroyBlock"}, at={@At(value="HEAD")})
    private void luxium$beginStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.begin();
    }

    @Inject(method={"startDestroyBlock"}, at={@At(value="RETURN")})
    private void luxium$endStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.end();
    }

    @Inject(method={"continueDestroyBlock"}, at={@At(value="HEAD")})
    private void luxium$beginContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.begin();
    }

    @Inject(method={"continueDestroyBlock"}, at={@At(value="RETURN")})
    private void luxium$endContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.end();
    }

    @Inject(method={"destroyBlock"}, at={@At(value="HEAD")})
    private void luxium$beginDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.begin();
    }

    @Inject(method={"destroyBlock"}, at={@At(value="RETURN")})
    private void luxium$endDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerActionContext.end();
    }

    @Inject(method={"useItemOn"}, at={@At(value="HEAD")})
    private void luxium$beginUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerActionContext.begin();
    }

    @Inject(method={"useItemOn"}, at={@At(value="RETURN")})
    private void luxium$endUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerActionContext.end();
    }
}

