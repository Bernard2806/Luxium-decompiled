/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.ReflectionSystem;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Camera.class})
public abstract class CameraMixin {
    @Inject(method={"isDetached"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void luxium$renderPlayerInReflection(CallbackInfoReturnable<Boolean> cir) {
        if (ReflectionSystem.isRenderingWorldPass()) {
            cir.setReturnValue((Object)true);
        }
    }
}

