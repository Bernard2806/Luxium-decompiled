package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.vinlanx.luxium.client.posteffects.HdrColorTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderTarget.class}) public abstract

class HdrRenderTargetMixin implements HdrColorTarget {
    @Unique private int luxium$colorFormat = 32856;

    @Override public int luxium$getColorFormat() {

 return this.luxium$colorFormat;
    }

    @Override public void luxium$setColorFormat(int format) {

        this.luxium$colorFormat = format;
    }

    @Inject(method = {"createBuffers"}, at = {@At("TAIL")} ) private void luxium$resetHdrFormat(int width, int height, boolean onOsx, CallbackInfo ci) {

 this.luxium$colorFormat = 32856;
    }

    @Inject(method = {"destroyBuffers"}, at = {@At("TAIL")} ) private void luxium$releaseHdrFormat(CallbackInfo ci) {

 this.luxium$colorFormat = 32856;
    }
    }
