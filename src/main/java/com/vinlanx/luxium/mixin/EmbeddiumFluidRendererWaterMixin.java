/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.level.material.FluidState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.vinlanx.luxium.mixin;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.water.WaterTerrainPass;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={FluidRenderer.class}, remap=false)
public abstract class EmbeddiumFluidRendererWaterMixin {
    @Unique
    private static final Material luxium$waterMaterial = new Material(WaterTerrainPass.get(), AlphaCutoffParameter.ONE, true);

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forFluidState(Lnet/minecraft/world/level/material/FluidState;)Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;"))
    private Material luxium$tagWaterMaterial(FluidState state) {
        Material original = DefaultMaterials.forFluidState((FluidState)state);
        if (Config.isFeatureEnabled(Config.CLIENT.waterEnabled) && state.m_205070_(FluidTags.f_13131_) && original.pass == DefaultTerrainRenderPasses.TRANSLUCENT) {
            return luxium$waterMaterial;
        }
        return original;
    }
}

