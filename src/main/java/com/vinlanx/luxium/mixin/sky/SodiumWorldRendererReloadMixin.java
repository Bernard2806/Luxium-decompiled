/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin.sky;

import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SodiumWorldRenderer.class}, remap=false)
public abstract class SodiumWorldRendererReloadMixin {
    @Inject(method={"reload"}, at={@At(value="RETURN")})
    private void luxium$invalidateSkyShadowsAfterReload(CallbackInfo ci) {
        GpuShadowCache.get().markAllDirty();
        if (NeoGpuVanilla.isConfiguredEnabled()) {
            NeoGpuVanilla.requestRebake();
        }
        NeoSkyCelestia.get().markAllDirty();
    }
}

