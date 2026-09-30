/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.ResourceProvider
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.ssr.ScreenSpaceReflectionSystem;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.mixin.LightTextureAccessor;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GameRenderer.class})
public abstract class GameRendererMixin {
    @Unique
    private static final Set<ResourceLocation> LUXIUM$ENTITY_RECEIVER_DEFINITIONS = Set.of(ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_entity_solid.json"), ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_entity_cutout.json"), ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_entity_cutout_no_cull.json"), ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_entity_translucent.json"), ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_entity_translucent_cull.json"), ResourceLocation.fromNamespaceAndPath((String)"minecraft", (String)"shaders/core/rendertype_armor_cutout_no_cull.json"));

    @ModifyVariable(method={"reloadShaders"}, at=@At(value="HEAD"), argsOnly=true)
    private ResourceProvider luxium$selectEntityReceiverShaders(ResourceProvider resourceProvider) {
        LuxiumGpuShaderFeatures.refreshNow();
        ResourceManager resourceManager = Minecraft.m_91087_().m_91098_();
        if (BlockLightTest.enabled() && !LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled()) {
            return resourceLocation -> {
                if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                    return resourceProvider.m_213713_(resourceLocation);
                }
                Resource definition = (Resource)resourceProvider.m_213713_(resourceLocation).orElseThrow(() -> new IllegalStateException("Missing entity shader " + resourceLocation));
                return Optional.of(new Resource(definition.m_247173_(), () -> {
                    try (InputStream stream = definition.m_215507_();){
                        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\"vertex\": \"luxium_entity_shadow\"", "\"vertex\": \"luxium_entity_blocklighttest\"").replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_blocklighttest\"");
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                        return byteArrayInputStream;
                    }
                }));
            };
        }
        if (LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled()) {
            return resourceLocation -> {
                if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                    return resourceProvider.m_213713_(resourceLocation);
                }
                Resource receiverDefinition = (Resource)resourceProvider.m_213713_(resourceLocation).orElseThrow(() -> new IllegalStateException("Missing Luxium shader resource " + resourceLocation));
                return Optional.of(new Resource(receiverDefinition.m_247173_(), () -> {
                    try (InputStream stream = receiverDefinition.m_215507_();){
                        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_neogpuvanilla\"");
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                        return byteArrayInputStream;
                    }
                }));
            };
        }
        if (LuxiumGpuShaderFeatures.localReceiverEnabled()) {
            return resourceProvider;
        }
        return resourceLocation -> {
            if (!LUXIUM$ENTITY_RECEIVER_DEFINITIONS.contains(resourceLocation)) {
                return resourceProvider.m_213713_(resourceLocation);
            }
            if (!LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled()) {
                Resource vanilla = resourceManager.m_213829_(resourceLocation).stream().filter(resource -> "vanilla".equals(resource.m_215506_())).findFirst().orElseThrow(() -> new IllegalStateException("Missing vanilla shader resource " + resourceLocation));
                return Optional.of(vanilla);
            }
            Resource receiverDefinition = (Resource)resourceProvider.m_213713_(resourceLocation).orElseThrow(() -> new IllegalStateException("Missing Luxium shader resource " + resourceLocation));
            return Optional.of(new Resource(receiverDefinition.m_247173_(), () -> {
                try (InputStream stream = receiverDefinition.m_215507_();){
                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\"vertex\": \"luxium_entity_shadow\"", "\"vertex\": \"luxium_entity_neoskycelestia\"").replace("\"fragment\": \"luxium_entity_shadow\"", "\"fragment\": \"luxium_entity_neoskycelestia\"");
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                    return byteArrayInputStream;
                }
            }));
        };
    }

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void luxium$beginColoredWorldRender(float partialTick, long finishNano, PoseStack poseStack, CallbackInfo ci) {
        Coloredlight.beginWorldRender();
    }

    @Inject(method={"renderLevel"}, at={@At(value="RETURN")})
    private void luxium$endColoredWorldRender(float partialTick, long finishNano, PoseStack poseStack, CallbackInfo ci) {
        TemporalFrameSystem.endFirstPersonOverlay();
        Coloredlight.endWorldRender();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"))
    private void luxium$redirectRenderLevel(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix) {
        boolean tfrRealActive;
        boolean needsCubemapCapture;
        Minecraft minecraft = Minecraft.m_91087_();
        Matrix4f viewMatrix = new Matrix4f((Matrix4fc)poseStack.m_85850_().m_252922_());
        LuxiumGpuShaderFeatures.beginFrame();
        if (TemporalFrameSystem.tryRenderSynthetic(minecraft, camera, projectionMatrix, viewMatrix)) {
            return;
        }
        NeoSkyFrameCache.beginFrame(minecraft, partialTick, projectionMatrix, viewMatrix);
        ReflectionSystem.get().onMainRenderLevel(levelRenderer, poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
        NeoShadowsEngine.prepareFrame(Minecraft.m_91087_(), camera, partialTick);
        NeoShadowsEngine.captureSkyLightScene(levelRenderer, poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
        NeoGpuVanilla.prepareShadowAtlas(minecraft, camera);
        GpuNeoShadows.prepareShadowAtlas(minecraft, camera);
        boolean bl = needsCubemapCapture = NeoGpuVanilla.hasStagedLights() || GpuNeoShadows.hasStagedLights() && GpuNeoShadows.usesCubemapShadows();
        if (needsCubemapCapture) {
            GpuShadowCache.get().buildPendingShadows(levelRenderer, partialTick, finishNano, gameRenderer, lightTexture);
        }
        boolean lsrActive = !(tfrRealActive = TemporalFrameSystem.beginRealFrame(minecraft)) && LsrSystem.beginWorldRender(minecraft);
        try {
            NeoGpuVanilla.prepareMainWorldRender(minecraft, camera);
            GpuNeoShadows.prepareMainWorldRender(minecraft, camera, projectionMatrix, viewMatrix);
            NeoSkyCelestia skyShadows = NeoSkyCelestia.get();
            skyShadows.prepareForMainWorldRender(Minecraft.m_91087_(), camera, partialTick, projectionMatrix, viewMatrix);
            if (tfrRealActive) {
                TemporalFrameSystem.rebindWorldTarget();
            } else if (lsrActive) {
                LsrSystem.rebindWorldTarget();
            }
            NeoGpuVanilla.beginMainWorldRender();
            GpuNeoShadows.beginMainWorldRender();
            try {
                levelRenderer.m_109599_(poseStack, partialTick, finishNano, renderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
            }
            finally {
                GpuNeoShadows.finishMainWorldRender();
                NeoGpuVanilla.finishMainWorldRender();
                skyShadows.finishMainWorldRender();
            }
            ScreenSpaceReflectionSystem.resolveLateFrame();
            if (tfrRealActive) {
                TemporalFrameSystem.finishRealFrame(minecraft, camera, projectionMatrix, viewMatrix);
            } else if (lsrActive) {
                LsrSystem.finishRealFrame(minecraft);
            }
        }
        finally {
            if (tfrRealActive && TemporalFrameSystem.isWorldPassActive()) {
                TemporalFrameSystem.abortRealFrame(minecraft);
            }
            if (lsrActive && LsrSystem.isWorldPassActive()) {
                LsrSystem.restoreNativeTarget(minecraft);
            }
        }
    }

    @Inject(method={"renderItemInHand"}, at={@At(value="HEAD")}, require=0)
    private void luxium$beginTemporalFirstPersonPass(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) {
        TemporalFrameSystem.beginFirstPersonOverlay(Minecraft.m_91087_());
    }

    @Inject(method={"renderItemInHand"}, at={@At(value="RETURN")}, require=0)
    private void luxium$endTemporalFirstPersonPass(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) {
        TemporalFrameSystem.endFirstPersonOverlay();
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V", shift=At.Shift.AFTER)})
    private void luxium$restoreVanillaLightmapForGui(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        if (!renderWorld || !Coloredlight.isEnabled()) {
            return;
        }
        GameRenderer self = (GameRenderer)this;
        LightTexture lightTexture = self.m_109154_();
        LightTextureAccessor accessor = (LightTextureAccessor)lightTexture;
        accessor.luxium$setUpdateLightTexture(true);
        lightTexture.m_109881_(partialTick);
        accessor.luxium$setUpdateLightTexture(true);
    }

    @Inject(method={"render"}, at={@At(value="HEAD")}, require=0)
    private void luxium$captureCanonicalMainTarget(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        Minecraft mc = Minecraft.m_91087_();
        PostProcessStateGuard.captureCanonicalMainTarget(mc);
        PostProcessStateGuard.restoreCanonicalMainTargetBinding(mc);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V", shift=At.Shift.BEFORE)}, require=0)
    private void luxium$restoreCanonicalMainTargetBeforeGui(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState();
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift=At.Shift.BEFORE)}, require=0)
    private void luxium$restoreCanonicalMainTargetBeforeScreen(float partialTick, long finishNano, boolean renderWorld, CallbackInfo ci) {
        PostProcessStateGuard.restoreGuiCompositeState();
    }
}

