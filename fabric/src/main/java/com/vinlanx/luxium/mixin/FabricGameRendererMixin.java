package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.fabric.FabricShaderRegistration;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({GameRenderer.class}) public
class FabricGameRendererMixin {
    @Inject(method = {"reloadShaders"}, at = {@At("HEAD")} ) private void luxium$beginShaderReload(ResourceProvider resourceProvider, CallbackInfo ci) { FabricShaderRegistration.beginReload

 ();
    }

    @Inject(
        method = {"render(FJZ)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = Shift.BEFORE )} ) private void luxium$restoreCanonicalMainTargetBeforeScreen(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState


        ();
    }
    }
