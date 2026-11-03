package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.f3.LuxiumF3Info;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DebugScreenOverlay.class}) public abstract
class FabricDebugScreenOverlayMixin {
    @Inject(method = {"getSystemInformation"}, at = {@At("RETURN")}, cancellable = true ) private void luxium$debugText(CallbackInfoReturnable<List<String>> cir) { List<String>

lines = new ArrayList<>(cir.getReturnValue());
        LuxiumF3Info.onDebugText(lines);
        cir.setReturnValue(lines);
    }
    }
