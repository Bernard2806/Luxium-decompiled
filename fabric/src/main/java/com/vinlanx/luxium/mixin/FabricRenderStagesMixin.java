package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.platform.ClientHooks;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import com.vinlanx.luxium.platform.RenderFrame;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({LevelRenderer.class}) public abstract
class FabricRenderStagesMixin {
    @Shadow private int ticks; @Shadow private Frustum cullingFrustum; @Shadow private Frustum capturedFrustum;


 @Unique private final Deque<Frustum> luxium$frameFrustums = new ArrayDeque<>();

    @Unique private void luxium$dispatch(RenderFrame.Stage stage, PoseStack pose, Camera camera, Matrix4f projection) {

 ClientHooks.render(new RenderFrame(stage, pose, projection, this.ticks, LuxiumPlatform.get().partialTick(Minecraft.getInstance()), camera, this.luxium$frameFrustums.element
() ) );
}

    @Inject(method = {"renderLevel"}, at = {@At("HEAD")} ) private void luxium$beginFrame(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$frameFrustums.push(this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = Shift.AFTER )} ) private void luxium$afterSky(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_SKY, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 0 )} ) private void luxium$afterSolid(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_SOLID_BLOCKS, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 1 )} ) private void luxium$afterCutoutMipped(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_CUTOUT_MIPPED_BLOCKS, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 2 )} ) private void luxium$afterCutout(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_CUTOUT_BLOCKS, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"},
        at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 3 ), @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 5 )} ) private void luxium$afterTranslucent(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {




 this.luxium$dispatch(RenderFrame.Stage.AFTER_TRANSLUCENT_BLOCKS, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"},
        at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 4 ), @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", shift = Shift.AFTER, ordinal = 6 )} ) private void luxium$afterTripwire(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {




 this.luxium$dispatch(RenderFrame.Stage.AFTER_TRIPWIRE_BLOCKS, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", shift = Shift.AFTER )} ) private void luxium$afterParticles(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_PARTICLES, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift = Shift.AFTER )} ) private void luxium$afterWeather(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_WEATHER, pose, camera, projection);
    }

    @Inject(method = {"renderLevel"}, at = {@At(value = "CONSTANT", args = {"stringValue=blockentities"} )} ) private void luxium$afterEntities(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_ENTITIES, pose, camera, projection);
    }

    @Inject(method = {"renderLevel"}, at = {@At(value = "CONSTANT", args = {"stringValue=destroyProgress"} )} ) private void luxium$afterBlockEntities(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) {



 this.luxium$dispatch(RenderFrame.Stage.AFTER_BLOCK_ENTITIES, pose, camera, projection);
    }

    @Inject(
        method = {"renderLevel"},
        at = {@At("RETURN")} ) private void luxium$endFrame(PoseStack pose, float partialTick, long finishTime, boolean blockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci ) { try {


 this.luxium$dispatch(RenderFrame.Stage.AFTER_LEVEL, pose, camera, projection);
        } finally {
            this.luxium$frameFrustums.pop();
        }
    }
    }
