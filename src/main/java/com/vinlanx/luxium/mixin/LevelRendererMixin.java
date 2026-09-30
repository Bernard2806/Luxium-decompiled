/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  me.jellysquid.mods.sodium.client.gl.device.RenderDevice
 *  me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  me.jellysquid.mods.sodium.client.world.WorldRendererExtended
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Constant
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.ModifyConstant
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.Sky;
import com.vinlanx.luxium.client.clouds.LuxiumCloudRenderer;
import com.vinlanx.luxium.client.posteffects.skygodrays;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import com.vinlanx.luxium.client.water.WaterSurfaceRenderer;
import com.vinlanx.luxium.client.water.WaterSurfaceState;
import com.vinlanx.luxium.mixin.sky.SodiumWorldRendererAccessor;
import com.vinlanx.luxium.rtx.EntityShadowManager;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.VanillaLavaLightEngine;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaCutoutPrepass;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={LevelRenderer.class}, priority=1100)
public abstract class LevelRendererMixin {
    @Shadow
    private int f_109477_;
    @Unique
    private boolean luxium$neoCutoutPrepassArmed;

    @Inject(method={"getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$overrideLight(BlockAndTintGetter getter, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int flashlightLight;
        TorchRtxState rtx = TorchRtxState.get();
        if (!rtx.isEnabled()) {
            return;
        }
        int blockLight = rtx.sampleLightForRender(pos);
        if (blockLight < 0) {
            if (!VanillaLavaLightEngine.get().isActive()) {
                return;
            }
            blockLight = 0;
        }
        if (VanillaLavaLightEngine.get().isActive()) {
            blockLight = Math.max(blockLight, VanillaLavaLightEngine.get().sampleLight(pos));
        }
        if ((flashlightLight = TestFlashLight.sampleLight(pos)) > blockLight) {
            blockLight = flashlightLight;
        }
        if (blockLight > 0 && rtx.usesEntityOcclusionShadows() && EntityShadowManager.get().isBlockInShadow(pos.m_121878_())) {
            blockLight = Math.max(0, blockLight - 10);
        }
        int emission = state.getLightEmission((BlockGetter)getter, pos);
        blockLight = Math.max(blockLight, emission);
        blockLight = Math.max(0, Math.min(15, blockLight));
        int sky = getter.m_45517_(LightLayer.SKY, pos);
        cir.setReturnValue((Object)(sky << 20 | blockLight << 4));
    }

    @ModifyVariable(method={"Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"}, at=@At(value="HEAD"), require=0, argsOnly=true, ordinal=1)
    private boolean luxium$shadowPassSpectator(boolean spectator) {
        return NeoShadowsEngine.isShadowCapturePass() || spectator;
    }

    @ModifyArgs(method={"setupRender"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ViewArea;repositionCamera(DD)V"), require=0)
    private void luxium$moveViewAreaToRenderCamera(Args args) {
        Vec3 cameraPos = ReflectionSystem.getActiveRenderCameraPos();
        if (cameraPos == null) {
            return;
        }
        args.set(0, (Object)cameraPos.f_82479_);
        args.set(1, (Object)cameraPos.f_82481_);
    }

    @ModifyArgs(method={"setupRender"}, at=@At(value="INVOKE", target="Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"), require=0)
    private void luxium$useRenderCameraForTerrainOrigin(Args args) {
        Vec3 cameraPos = ReflectionSystem.getActiveRenderCameraPos();
        if (cameraPos == null) {
            return;
        }
        args.set(0, (Object)cameraPos.f_82479_);
        args.set(1, (Object)cameraPos.f_82480_);
        args.set(2, (Object)cameraPos.f_82481_);
    }

    @Inject(method={"renderClouds"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void luxium$renderThreeLayerClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!Config.isFeatureEnabled(Config.CLIENT.cloudsEnabled)) {
            return;
        }
        boolean cloudShadowPass = NeoSkyCelestia.get().isRenderingCloudShadowPass();
        if (LuxiumCloudRenderer.get().render(poseStack, projectionMatrix, partialTick, cameraX, cameraY, cameraZ, this.f_109477_, cloudShadowPass)) {
            ci.cancel();
        }
    }

    @Inject(method={"allChanged"}, at={@At(value="RETURN")}, require=0)
    private void luxium$reloadCloudResources(CallbackInfo ci) {
        LuxiumCloudRenderer.get().invalidateResources();
    }

    @Inject(method={"setLevel"}, at={@At(value="RETURN")}, require=0)
    private void luxium$resetCloudWorldCache(ClientLevel level, CallbackInfo ci) {
        LuxiumCloudRenderer.get().invalidateGeometry();
    }

    @Inject(method={"renderClouds"}, at={@At(value="HEAD")}, require=0)
    private void luxium$prepareCloudShadowDepthPass(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!NeoSkyCelestia.get().isRenderingCloudShadowPass()) {
            return;
        }
        ShaderInstance shader = ShaderManager.getCloudShadowCasterShader();
        if (shader != null) {
            RenderSystem.setShader(() -> shader);
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
    }

    @Inject(method={"renderClouds"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", shift=At.Shift.BEFORE)}, require=0)
    private void luxium$forceCloudShadowDepthWrite(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!NeoSkyCelestia.get().isRenderingCloudShadowPass()) {
            return;
        }
        ShaderInstance shader = ShaderManager.getCloudShadowCasterShader();
        if (shader != null) {
            RenderSystem.setShader(() -> shader);
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
    }

    @Inject(method={"renderClouds"}, at={@At(value="RETURN")}, require=0)
    private void luxium$preserveCloudShadowDepthState(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!NeoSkyCelestia.get().isRenderingCloudShadowPass()) {
            return;
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
    }

    @Inject(method={"renderSky"}, at={@At(value="HEAD")}, cancellable=true)
    private void luxium$customSkyRendering(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (Config.isFeatureEnabled(Config.CLIENT.skyEnabled)) {
            Sky.render(poseStack, projectionMatrix, partialTick);
            ci.cancel();
        }
    }

    @ModifyConstant(method={"renderSky"}, constant={@Constant(floatValue=30.0f)}, require=1)
    private float luxium$scaleVanillaSunSize(float vanillaSize) {
        return Config.isEnabled() ? vanillaSize * Mth.m_14036_((float)((Double)Config.CLIENT.skySunSize.get()).floatValue(), (float)0.1f, (float)3.0f) : vanillaSize;
    }

    @ModifyConstant(method={"renderSky"}, constant={@Constant(floatValue=20.0f)}, require=1)
    private float luxium$scaleVanillaMoonSize(float vanillaSize) {
        return Config.isEnabled() ? vanillaSize * Mth.m_14036_((float)((Double)Config.CLIENT.skyMoonSize.get()).floatValue(), (float)0.1f, (float)3.0f) : vanillaSize;
    }

    @Inject(method={"renderSky"}, at={@At(value="HEAD")}, require=0)
    private void luxium$beginSkyGodRayCapture(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        skygodrays.beginSkyCapture();
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal=1, shift=At.Shift.AFTER)}, require=1)
    private void luxium$applyVanillaCelestialPath(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        CelestialPath.applyOrbitTilt(poseStack);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal=0)}, require=0)
    private void luxium$captureSunSkyGodRayAnchor(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        skygodrays.captureCelestial(new Matrix4f((Matrix4fc)poseStack.m_85850_().m_252922_()), new Matrix4f((Matrix4fc)projectionMatrix), false, partialTick);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal=1)}, require=0)
    private void luxium$captureMoonSkyGodRayAnchor(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        skygodrays.captureCelestial(new Matrix4f((Matrix4fc)poseStack.m_85850_().m_252922_()), new Matrix4f((Matrix4fc)projectionMatrix), true, partialTick);
    }

    @Inject(method={"renderEntity"}, at={@At(value="HEAD")}, cancellable=true)
    private void luxium$skipShadowCaptureEntities(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (NeoShadowsEngine.isShadowCapturePass() && !NeoShadowsEngine.shouldRenderDynamicCastersInShadowPass()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal=0, shift=At.Shift.BEFORE)}, require=0)
    private void luxium$beginNeoSkyCelestiaInlineTerrain(PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        NeoSkyCelestia.get().beginInlineTerrainPass();
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal=2, shift=At.Shift.AFTER)}, require=0)
    private void luxium$finishNeoSkyCelestiaInlineTerrain(PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        NeoSkyCelestia.get().finishInlineTerrainPass();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal=0, shift=At.Shift.BEFORE)}, require=0)
    private void luxium$neoGpuCutoutDepthPrepass(PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.luxium$neoCutoutPrepassArmed = false;
        if (!NeoGpuVanillaCutoutPrepass.shouldRun()) {
            NeoGpuVanillaCutoutPrepass.finish();
            return;
        }
        SodiumWorldRenderer sodium = ((WorldRendererExtended)this).sodium$getWorldRenderer();
        if (sodium == null) {
            NeoGpuVanillaCutoutPrepass.finish();
            return;
        }
        RenderSectionManager manager = ((SodiumWorldRendererAccessor)sodium).luxium$getRenderSectionManager();
        if (manager == null) {
            NeoGpuVanillaCutoutPrepass.finish();
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        ChunkRenderMatrices matrices = new ChunkRenderMatrices((Matrix4fc)projectionMatrix, (Matrix4fc)new Matrix4f((Matrix4fc)poseStack.m_85850_().m_252922_()));
        NeoGpuVanillaCutoutPrepass.beginDepthPrepass();
        RenderDevice.enterManagedCode();
        boolean completed = false;
        try {
            manager.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_);
            completed = true;
        }
        finally {
            RenderDevice.exitManagedCode();
            if (!completed) {
                NeoGpuVanillaCutoutPrepass.finish();
            }
        }
        if (completed) {
            NeoGpuVanillaCutoutPrepass.armColorPass();
            this.luxium$neoCutoutPrepassArmed = true;
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal=0, shift=At.Shift.AFTER)}, require=0)
    private void luxium$finishNeoGpuCutoutColorPass(PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.luxium$neoCutoutPrepassArmed) {
            NeoGpuVanillaCutoutPrepass.finish();
            this.luxium$neoCutoutPrepassArmed = false;
        }
    }

    @Inject(method={"renderChunkLayer"}, at={@At(value="HEAD")}, require=0)
    private void luxium$captureWaterBackground(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (renderType != RenderType.m_110466_()) {
            return;
        }
        if (!WaterSurfaceState.canRenderMainPass()) {
            return;
        }
        SodiumWorldRenderer sodium = ((WorldRendererExtended)this).sodium$getWorldRenderer();
        if (sodium == null) {
            return;
        }
        RenderSectionManager manager = ((SodiumWorldRendererAccessor)sodium).luxium$getRenderSectionManager();
        if (manager == null) {
            return;
        }
        ChunkRenderMatrices matrices = new ChunkRenderMatrices((Matrix4fc)projectionMatrix, (Matrix4fc)new Matrix4f((Matrix4fc)poseStack.m_85850_().m_252922_()));
        WaterSurfaceRenderer.renderWater(manager, matrices, camX, camY, camZ);
    }

    @Inject(method={"renderChunkLayer"}, at={@At(value="HEAD")}, cancellable=true)
    private void luxium$skipShadowCaptureTranslucency(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (NeoShadowsEngine.isShadowCapturePass() && (renderType == RenderType.m_110466_() || renderType == RenderType.m_110503_())) {
            ci.cancel();
        }
    }
}

