package com.vinlanx.luxium.mixin.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.water.WaterSurfaceState;
import com.vinlanx.luxium.client.water.WaterTerrainPass;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = {LevelRenderer.class}, priority = 900 ) public abstract

 class FabricReflectionWaterMixin {
    @Inject(
        method = {"renderChunkLayer"},
        at = {@At("HEAD")},
        require = 1 ) private void luxium$renderReflectionWater(
        RenderType renderType, PoseStack poseStack, double cameraX, double cameraY, double cameraZ, Matrix4f projectionMatrix, CallbackInfo ci ) { if (!(renderType == RenderType.translucent() && ReflectionSystem.isRenderingWorldPass
 () && WaterSurfaceState.enabled())) {
return;
}
            SodiumWorldRenderer renderer = ((WorldRendererExtended)(Object)this).sodium$getWorldRenderer();
            if (!(renderer != null)) {
 return;
 }
                RenderSectionManager manager = ((SodiumWorldRendererAccessor)renderer).luxium$getRenderSectionManager();
                if (!(manager != null)) {
 return;
 }

                    ChunkRenderMatrices matrices = new ChunkRenderMatrices(projectionMatrix, new Matrix4f(poseStack.last
().pose()));
                    RenderDevice.enterManagedCode();

                    try { manager.renderLayer(matrices, WaterTerrainPass.get(), cameraX, cameraY, cameraZ);
                    } finally {
                        RenderDevice.exitManagedCode();
                    }
    }
    }
