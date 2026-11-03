package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.posteffects.Hdr;
import com.vinlanx.luxium.client.posteffects.PostEffectPipeline;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.ssr.ScreenSpaceReflectionSystem;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.rtx.Coloredlight;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.vinlanx.luxium.mixin.LightTextureAccessor;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable; @Mixin({GameRenderer.class}) public abstract class GameRendererMixin { @Inject(method = {"getRendertypeEntitySolidShader"}, at = {@At("HEAD")}, cancellable = true ) private static void luxium$entitySolidCaster(CallbackInfoReturnable<ShaderInstance> cir) { if (NeoSkyCelestia.get ().isRenderingEntityShadowPass()) {
cir.setReturnValue(ShaderManager.getEntityShadowCasterShader(true)); }

} @Inject(method = {"getRendertypeEntityCutoutShader", "getRendertypeEntityCutoutNoCullShader", "getRendertypeEntityTranslucentShader", "getRendertypeEntityTranslucentCullShader", "getRendertypeArmorCutoutNoCullShader"}, at = {@At("HEAD")}, cancellable = true ) private static void luxium$entityCutoutCaster(CallbackInfoReturnable<ShaderInstance> cir) { if (NeoSkyCelestia.get





().isRenderingEntityShadowPass()) {
cir.setReturnValue(ShaderManager.getEntityShadowCasterShader(false)); }

} @Unique private static final Set<ResourceLocation>


LUXIUM$ENTITY_RECEIVER_DEFINITIONS = Set.of(new ResourceLocation("minecraft", "shaders/core/rendertype_entity_solid.json"), new ResourceLocation("minecraft", "shaders/core/rendertype_entity_cutout.json"), new ResourceLocation("minecraft", "shaders/core/rendertype_entity_cutout_no_cull.json"), new ResourceLocation("minecraft", "shaders/core/rendertype_entity_translucent.json"), new ResourceLocation("minecraft", "shaders/core/rendertype_entity_translucent_cull.json"), new ResourceLocation("minecraft", "shaders/core/rendertype_armor_cutout_no_cull.json") );

    @ModifyVariable(
        method = {"reloadShaders"},
        at = @At("HEAD"),
        argsOnly = true ) private ResourceProvider luxium$selectEntityReceiverShaders(ResourceProvider resourceProvider) {
        LuxiumGpuShaderFeatures.refreshNow();
        LuxiumGpuShaderFeatures.recordEntityShaderSelection();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (BlockLightTest.enabled() && !LuxiumGpuShaderFeatures.neoSkyEntityReceiverEnabled() && !LuxiumGpuShaderFeatures.receiverShadersEnabled()) {
            return resourceLocation -> {






 if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                    return resourceProvider.getResource(resourceLocation);
                } else { Resource definition = luxium$entityReceiverDefinition(resourceProvider, resourceLocation);
                    return Optional.of(new Resource(definition.source(),
                            () -> {
                                ByteArrayInputStream var3;
                                try (InputStream stream = definition.open()) {
                                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                        .replace("\"vertex\": \"luxium_entity_shadow\"", "\"vertex\": \"luxium_entity_blocklighttest\"")
                                        .replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_blocklighttest\"");
                                    var3 = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                }

                                return var3;
                            } ) );
                }
            };
        } else if (LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled()) { return resourceLocation -> {

 if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                    return resourceProvider.getResource(resourceLocation);
                } else {
                    Resource receiverDefinition = luxium$entityReceiverDefinition(resourceProvider, resourceLocation);
                    return Optional.of(new Resource(receiverDefinition.source(),
                            () -> {
                                ByteArrayInputStream var3;
                                try (InputStream stream = receiverDefinition.open()) {
                                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                        .replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_neogpuvanilla\"");
                                    var3 = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                }

                                return var3;
                            } ) ); } }; } else { return LuxiumGpuShaderFeatures.localReceiverEnabled() ? resourceLocation ->

 LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)
                        ? Optional.of(luxium$entityReceiverDefinition(resourceProvider, resourceLocation)) : resourceProvider.getResource
 (resourceLocation) : resourceLocation -> {



                    if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                        return resourceProvider.getResource(resourceLocation);
                    } else if (!LuxiumGpuShaderFeatures.neoSkyEntityReceiverEnabled

()) {
                        Resource vanilla = resourceManager.getResourceStack(resourceLocation) .stream()
                            .filter(resource -> "vanilla".equals(resource.sourcePackId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Missing vanilla shader resource " + resourceLocation));

                        return Optional.of(vanilla);
                    } else {
                        Resource
                        receiverDefinition = luxium$entityReceiverDefinition(resourceProvider, resourceLocation);
                        return Optional.of(new Resource(receiverDefinition.source(),
                                () -> {
                                    ByteArrayInputStream var3x;
                                    try (InputStream stream = receiverDefinition.open()) {
                                        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                            .replace("\"vertex\": \"luxium_entity_shadow\"", "\"vertex\": \"luxium_entity_neoskycelestia\"")
                                            .replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_neoskycelestia\"");
                                        var3x = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                                    }

                                    return var3x;
                                } ) );
                    } }; } } @Unique private static Resource luxium$entityReceiverDefinition(ResourceProvider provider, ResourceLocation location) { String


 path = location.getPath();
        ResourceLocation definition = new ResourceLocation("luxium", "shaders/core/entity_receivers/" + path.substring
 ("shaders/core/".length()));
        return provider.getResource(definition).orElseThrow
(() -> new IllegalStateException("Missing Luxium entity shader " + definition));
    }

    @Inject(method = {"renderLevel"}, at = {@At("HEAD")} ) private void luxium$beginColoredWorldRender(float partialTick, long finishNano, PoseStack poseStack, CallbackInfo ci) { Coloredlight.beginWorldRender

 ();
    }

    @Inject(
        method = {"renderLevel"}, at = {@At("RETURN")} ) private void luxium$endColoredWorldRender(float partialTick, long finishNano, PoseStack poseStack, CallbackInfo ci) { TemporalFrameSystem.endFirstPersonOverlay

 ();
        Coloredlight.endWorldRender();
    }

    @Redirect(
        method = {"renderLevel"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" ) ) private void luxium$redirectRenderLevel(
        LevelRenderer levelRenderer,
        PoseStack poseStack,
        float partialTick,
        long finishNano,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix ) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix4f viewMatrix = new Matrix4f(poseStack.last().pose());

        Hdr.beginFrame(minecraft);
        LuxiumGpuShaderFeatures.beginFrame();

        if (!(!TemporalFrameSystem.tryRenderSynthetic(minecraft, camera, projectionMatrix, viewMatrix))) {

 return;
 }

            NeoSkyFrameCache.beginFrame(minecraft, partialTick, projectionMatrix, viewMatrix);
            ReflectionSystem.get
            () .onMainRenderLevel(levelRenderer, poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
            NeoShadowsEngine.prepareFrame



            (Minecraft.getInstance
(), camera, partialTick);




            NeoShadowsEngine.captureSkyLightScene(
                levelRenderer, poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix );










            NeoGpuVanilla.prepareShadowAtlas(minecraft, camera);
            GpuNeoShadows.prepareShadowAtlas(minecraft, camera);
            boolean needsCubemapCapture = NeoGpuVanilla.hasStagedLights() || GpuNeoShadows.hasStagedLights
 () && GpuNeoShadows.usesCubemapShadows();
            if (needsCubemapCapture) {
                GpuShadowCache.get().buildPendingShadows(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture);
            }

            boolean
            tfrRealActive = TemporalFrameSystem.beginRealFrame(minecraft);
            boolean lsrActive = !tfrRealActive && LsrSystem.beginWorldRender(minecraft);

            try { if (Hdr.configureColorTarget(minecraft.getMainRenderTarget(), Hdr.isEnabled())) {
                    minecraft.getMainRenderTarget().clear(Minecraft.ON_OSX);
                    minecraft.getMainRenderTarget().bindWrite(true);
                }
                NeoGpuVanilla.prepareMainWorldRender(minecraft, camera);
                GpuNeoShadows.prepareMainWorldRender(minecraft, camera, projectionMatrix, viewMatrix);
                NeoSkyCelestia skyShadows = NeoSkyCelestia.get

 ();
                skyShadows.prepareForMainWorldRender(Minecraft.getInstance
(), camera, partialTick, projectionMatrix, viewMatrix);

                if (tfrRealActive) {
                    TemporalFrameSystem.rebindWorldTarget();
                } else if (lsrActive) {
                    LsrSystem.rebindWorldTarget();
                }
                NeoGpuVanilla.beginMainWorldRender();
                GpuNeoShadows.beginMainWorldRender();

                try { levelRenderer.renderLevel(poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
                } finally {


                    GpuNeoShadows.finishMainWorldRender();
                    NeoGpuVanilla.finishMainWorldRender();
                    skyShadows.finishMainWorldRender();
                }

                ScreenSpaceReflectionSystem.resolveLateFrame();

                if (!lsrActive || !PostEffectPipeline.hasDeferredTonemap()) {
                    PostEffectPipeline.flushDeferredTonemapForSceneConsumer();
                }

                if (tfrRealActive) {
                    TemporalFrameSystem.finishRealFrame(minecraft, camera, projectionMatrix, viewMatrix);
                } else
                if (lsrActive) {
                    LsrSystem.finishRealFrame(minecraft);
                }
            } finally {
                PostEffectPipeline.clearDeferredTonemap();
                if (tfrRealActive && TemporalFrameSystem.isWorldPassActive()) {
                    TemporalFrameSystem.abortRealFrame(minecraft);
                }
                if (lsrActive && LsrSystem.isWorldPassActive()) {
                    LsrSystem.restoreNativeTarget(minecraft);
                }
            }
    }

    @Inject(
        method = {"renderItemInHand"},
        at = {@At("HEAD")},
        require = 0 ) private void luxium$beginTemporalFirstPersonPass(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) { TemporalFrameSystem.beginFirstPersonOverlay (Minecraft.getInstance());
    }

    @Inject(
        method = {"renderItemInHand"}, at = {@At("RETURN")}, require = 0 ) private void luxium$endTemporalFirstPersonPass(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) { TemporalFrameSystem.endFirstPersonOverlay

 ();
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V", shift=At.Shift.AFTER)}) private void luxium$restoreVanillaLightmapForGui(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {








        if (!renderWorld || !Coloredlight.isEnabled()) {
            return;
        }

        GameRenderer self = (GameRenderer)(Object)this;
        LightTexture lightTexture = self.lightTexture();
        LightTextureAccessor accessor = (LightTextureAccessor)lightTexture;
        accessor.luxium$setUpdateLightTexture(true);
        lightTexture.updateLightTexture(partialTick);
        accessor.luxium$setUpdateLightTexture(true);
    }

    @Inject(
        method = {"render"},
        at = {@At("HEAD")}, require = 0 ) private void luxium$captureCanonicalMainTarget(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) { Minecraft mc = Minecraft.getInstance ();
        PostProcessStateGuard.captureCanonicalMainTarget(mc);
        PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);
    }

    @Inject(
        method = {"render"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            shift = Shift.BEFORE )},
        require = 0 ) private void luxium$restoreCanonicalMainTargetBeforeGui(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState


        ();
    }
    }
