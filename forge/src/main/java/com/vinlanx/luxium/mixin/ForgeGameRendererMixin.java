package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({GameRenderer.class}) public
class ForgeGameRendererMixin {
    @Inject(
        method = {"render(FJZ)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = Shift.BEFORE,
            remap = false )} ) private void luxium$restoreCanonicalMainTargetBeforeScreen(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState


        ();
    }
    }
