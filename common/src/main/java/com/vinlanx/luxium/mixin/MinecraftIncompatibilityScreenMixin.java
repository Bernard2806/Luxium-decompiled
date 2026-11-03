package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.guiscreen.IncompatibleModScreen;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class}) public abstract
class MinecraftIncompatibilityScreenMixin {
    @Shadow public Screen screen;
    @Shadow public abstract void setScreen(Screen var1); @Inject(method = {"setInitialScreen"}, at = {@At("HEAD")}, cancellable = true ) private void luxium$blockInitialScreen(CallbackInfo ci) { IncompatibleModScreen blocker = LuxiumPlatform.incompatibilityScreen



 ();
        if (blocker != null) {
            this.setScreen(blocker);
            ci.cancel();
        }
    }

    @ModifyVariable(method = {"setScreen"}, at = @At("HEAD"), argsOnly = true ) private Screen luxium$prioritizeIncompatibilityScreen(Screen requested) { IncompatibleModScreen blocker = LuxiumPlatform.incompatibilityScreen

 ();
        return (Screen)(blocker != null ? blocker : requested);
    }

    @Inject(method = {"setScreen"}, at = {@At("HEAD")}, cancellable = true ) private void luxium$keepIncompatibilityScreen(Screen requested, CallbackInfo ci) { IncompatibleModScreen blocker = LuxiumPlatform.incompatibilityScreen

 ();
        if (blocker != null && this.screen == blocker) {
            ci.cancel();
        }
    }
    }
