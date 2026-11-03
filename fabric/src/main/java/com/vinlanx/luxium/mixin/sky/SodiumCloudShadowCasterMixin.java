package com.vinlanx.luxium.mixin.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import me.jellysquid.mods.sodium.client.render.immediate.CloudRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {CloudRenderer.class}, remap = false ) public abstract

 class SodiumCloudShadowCasterMixin {
    @Redirect(
        method = {"render"},
        remap = false, require = 2, expect = 2, allow = 2, at = @At(value = "INVOKE", remap = true, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V" ) ) private void luxium$preserveCloudShadowTarget(RenderTarget target, boolean setViewport) { if (!NeoSkyCelestia.get


().isRenderingCloudShadowPass()) {
            target.bindWrite(setViewport);
        }
    }

    @Redirect(
        method = {"render"},
        remap = false,
        require = 1,
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            remap = true,
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V" ) ) private void luxium$drawNativeCloudShadow(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, ShaderInstance nativeShader) {
        if (!NeoSkyCelestia.get().isRenderingCloudShadowPass()) {
            buffer.drawWithShader(modelView, projection, nativeShader);
        } else {
            ShaderInstance caster = GameRenderer.getPositionColorShader();
            caster.MODEL_VIEW_MATRIX.set(modelView);
            caster.PROJECTION_MATRIX.set(projection);
            caster.COLOR_MODULATOR.set(1.0F, 1.0F, 1.0F, 1.0F);
            caster.apply();

            try {
                luxium$configureCloudShadowDepthState();
                RenderSystem.disableCull();
                buffer.draw();
            } finally {
                caster.clear();
            }
        } } @Redirect(method = {"render"}, remap = false, require = 1, at = @At(value = "INVOKE", ordinal = 1, remap = true, target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V" ) ) private void luxium$skipCloudShadowColorDraw(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, ShaderInstance nativeShader) { if (NeoSkyCelestia.get





().isRenderingCloudShadowPass()) {
            luxium$configureCloudShadowDepthState();
        } else {
            buffer.drawWithShader(modelView, projection, nativeShader);
        }
    }

    @Inject(method = {"render"}, at = {@At("RETURN")}, remap = false, require = 1 ) private void luxium$restoreCloudShadowDepthState(CallbackInfo ci) { if (NeoSkyCelestia.get

().isRenderingCloudShadowPass()) {
            luxium$configureCloudShadowDepthState();
        }
    }

    @Unique private static void luxium$configureCloudShadowDepthState() {
        RenderSystem.disableBlend
        ();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(513);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
    }
    }
