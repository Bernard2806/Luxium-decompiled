/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.client.water.WaterTerrainPass;
import java.util.Arrays;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DefaultTerrainRenderPasses.class}, remap=false)
public abstract class EmbeddiumTerrainPassesMixin {
    @Shadow
    @Final
    @Mutable
    public static TerrainRenderPass[] ALL;

    @Inject(method={"<clinit>"}, at={@At(value="TAIL")})
    private static void luxium$appendWaterTerrainPass(CallbackInfo ci) {
        WaterTerrainPass.bootstrap();
        TerrainRenderPass water = WaterTerrainPass.get();
        for (TerrainRenderPass pass : ALL) {
            if (pass != water) continue;
            return;
        }
        TerrainRenderPass[] extended = Arrays.copyOf(ALL, ALL.length + 1);
        extended[EmbeddiumTerrainPassesMixin.ALL.length] = water;
        ALL = extended;
    }
}

