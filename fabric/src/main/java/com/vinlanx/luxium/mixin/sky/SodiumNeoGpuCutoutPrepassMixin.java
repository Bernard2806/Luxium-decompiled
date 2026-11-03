package com.vinlanx.luxium.mixin.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.water.WaterSurfaceState;
import com.vinlanx.luxium.client.water.WaterTerrainPass;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaCutoutPrepass;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = {ShaderChunkRenderer.class}, remap = false ) public abstract

 class SodiumNeoGpuCutoutPrepassMixin {
    @Unique private boolean luxium$preserveWaterCaptureTarget;
    @Unique private int luxium$waterCaptureDrawFramebuffer; @Unique private int luxium$waterCaptureReadFramebuffer; @Inject(method = {"begin"}, at = {@At("HEAD")}, remap = false, require = 1 ) private void luxium$saveWaterCaptureTarget(TerrainRenderPass pass, CallbackInfo ci) {




 this.luxium$preserveWaterCaptureTarget = WaterTerrainPass.is(pass) && WaterSurfaceState.renderMode
 () == 0 && ReflectionSystem.isRenderingWorldPass();
        if (this.luxium$preserveWaterCaptureTarget) {
            this.luxium$waterCaptureDrawFramebuffer = GL11.glGetInteger(36006);
            this.luxium$waterCaptureReadFramebuffer = GL11.glGetInteger(36010);
        }
    }

    @Inject(
        method = {"begin"},
        remap = false,
        require = 1,
        at = {@At(
            value = "INVOKE",
            remap = false,
            desc = @Desc(
                owner = TerrainRenderPass.class,
                value = "startDrawing" ),
            shift = Shift.AFTER )} ) private void luxium$configureNeoGpuCutoutDepthState(TerrainRenderPass pass, CallbackInfo ci) {
        if (WaterTerrainPass.is(pass)) {
            switch (WaterSurfaceState.renderMode()) {
                case 0:
                    if (this.luxium$preserveWaterCaptureTarget) {
                        this.luxium$restoreWaterCaptureTarget();
                    }
                    break;
                case 1:
                case 3:
                    SharedPostResources.bindWaterRenderTarget();
                    break;
                case 2:
                case 4:
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
                    break;
                default:
                    throw new IllegalStateException("Unknown water render mode");
            }
        } else if (pass == DefaultTerrainRenderPasses.CUTOUT) {
            if (NeoGpuVanillaCutoutPrepass.isDepthPrepass()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(false, false, false, false);
                RenderSystem.disableBlend();
            } else if (NeoGpuVanillaCutoutPrepass.isColorPass()) {
                RenderSystem.enableDepthTest(); RenderSystem.depthFunc(514); RenderSystem.depthMask(false); RenderSystem.colorMask(true, true, true, true); } } } @Inject(method = {"end"}, at = {@At("RETURN")}, remap = false, require = 1 ) private void luxium$restoreNeoGpuCutoutDepthState(TerrainRenderPass pass, CallbackInfo ci) { if (WaterTerrainPass.is(pass) && this.luxium$preserveWaterCaptureTarget) {
this.luxium$restoreWaterCaptureTarget();
this.luxium$preserveWaterCaptureTarget = false; }

 if (!(pass == DefaultTerrainRenderPasses.CUTOUT)) {
return;
}
 if (!(NeoGpuVanillaCutoutPrepass.isDepthPrepass() || NeoGpuVanillaCutoutPrepass.isColorPass
 ())) {
 return;
 }
 RenderSystem.colorMask(true, true, true, true);
 RenderSystem.depthMask(true);
RenderSystem.depthFunc(515);
} @Unique private void luxium$restoreWaterCaptureTarget() {



 GlStateManager._glBindFramebuffer(36009, this.luxium$waterCaptureDrawFramebuffer);
        GlStateManager._glBindFramebuffer(36008, this.luxium$waterCaptureReadFramebuffer);
    }
    }
