package com.vinlanx.luxium.mixin.sky;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.water.EmbeddiumWaterShaderCompileContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = {ShaderLoader.class}, remap = false ) public abstract

 class SodiumShaderLoaderMixin {
    @Inject(
        method = {"getShaderSource"},
        at = {@At("HEAD")},
        cancellable = true ) private static void luxium$replaceTerrainShaders(ResourceLocation name, CallbackInfoReturnable<String> cir) { if (!("sodium".equals(name.getNamespace()))) {
return;
}
            boolean vertex = "blocks/block_layer_opaque.vsh".equals(name.getPath());
            if (!(vertex || "blocks/block_layer_opaque.fsh".equals(name.getPath()))) {
return;
}

                boolean waterPass = Config.isFeatureEnabled(Config.CLIENT.waterEnabled) && EmbeddiumWaterShaderCompileContext.isWaterPass
 ();
                boolean blockLightTest = BlockLightTest.enabled();
                boolean receiver = LuxiumGpuShaderFeatures.receiverShadersEnabled();
                boolean neoGpuVanilla = receiver && LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled();
                boolean localReceiver = receiver && LuxiumGpuShaderFeatures.localReceiverEnabled();
                boolean neoSky = LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled() && !neoGpuVanilla && !localReceiver && !waterPass;
                boolean
                neoSkyForward = neoSky && EmbeddiumWaterShaderCompileContext.isTranslucentPass();
                boolean standaloneBlockLightTest = blockLightTest && !LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled
() && !waterPass;
                String variant;

                if (standaloneBlockLightTest) {
                    variant = "block_layer_blocklighttest";
                } else if (waterPass) {
                    variant = "block_layer_water";
                } else if (neoSkyForward) {
                    variant = "block_layer_neoskycelestia_forward";
                } else if (neoSky) {
                    variant = "block_layer_neoskycelestia";
                } else if (receiver) {
                    variant = !vertex && neoGpuVanilla ? "block_layer_neogpuvanilla" : "block_layer_opaque";
                } else {
                    variant = "block_layer_plantswave";
                }

                ResourceLocation replacement = new ResourceLocation("luxium", "shaders/sodium/" + variant + (
 vertex ? ".vsh" : ".fsh"));
                try { try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow
(replacement).open()) {
                        String source = IOUtils.toString(stream, StandardCharsets.UTF_8);
                        if (!standaloneBlockLightTest && !waterPass) {
                            if (receiver || neoSkyForward) {
                                source = source.replace(
                                    "#define LUXIUM_ENTITY_SHADOWS_ENABLED 0",
                                    "#define LUXIUM_ENTITY_SHADOWS_ENABLED " + (Config.isFeatureEnabled(Config.CLIENT.skyEntityShadowsEnabled) ? "1" : "0") );
                            }
                            if (receiver && (neoGpuVanilla || localReceiver)) {
                                source = source.replace(
                                    "#define LUXIUM_NGV_CUTOUT_ENABLED 0",
                                    "#define LUXIUM_NGV_CUTOUT_ENABLED " + (neoGpuVanilla && !Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaCutoutEnabled) ? "0" : "1") );
                            }
                            if (blockLightTest && neoSky && vertex) {
                                source = source.replace("#version 330 core", "#version 330 core\n#define LUXIUM_BLOCKLIGHT_TEST 1");
                            }
                        }

                        cir.setReturnValue(source);
                    } } catch (IOException var18) {
                    throw new IllegalStateException("Failed to load Luxium Sodium shader " + replacement, var18);
                }
    }
    }
