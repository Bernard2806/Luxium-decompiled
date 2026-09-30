/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  org.apache.commons.io.IOUtils
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.vinlanx.luxium.mixin.sky;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.water.EmbeddiumWaterShaderCompileContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ShaderLoader.class}, remap=false)
public abstract class EmbeddiumShaderLoaderMixin {
    @Inject(method={"getShaderSource"}, at={@At(value="HEAD")}, cancellable=true)
    private static void luxium$replaceTerrainShaders(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        ResourceLocation replacement;
        boolean neoSkyInline;
        boolean waterPassNow;
        if (!"sodium".equals(name.m_135827_())) {
            return;
        }
        boolean bl = waterPassNow = Config.isFeatureEnabled(Config.CLIENT.waterEnabled) && EmbeddiumWaterShaderCompileContext.isWaterPass();
        if (BlockLightTest.enabled() && !LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled() && !waterPassNow && ("blocks/block_layer_opaque.vsh".equals(name.m_135815_()) || "blocks/block_layer_opaque.fsh".equals(name.m_135815_()))) {
            ResourceLocation source = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)("shaders/embeddium/block_layer_blocklighttest." + (name.m_135815_().endsWith(".vsh") ? "vsh" : "fsh")));
            try (InputStream stream = Minecraft.m_91087_().m_91098_().m_215593_(source).m_215507_();){
                cir.setReturnValue((Object)IOUtils.toString((InputStream)stream, (Charset)StandardCharsets.UTF_8));
            }
            catch (IOException exception) {
                throw new IllegalStateException("Failed to load BlockLightTest shader " + source, exception);
            }
            return;
        }
        boolean receiverShaders = LuxiumGpuShaderFeatures.receiverShadersEnabled();
        boolean waterPass = waterPassNow;
        boolean neoGpuVanilla = receiverShaders && LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled();
        boolean localReceiver = receiverShaders && LuxiumGpuShaderFeatures.localReceiverEnabled();
        boolean neoSky = LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled() && !neoGpuVanilla && !localReceiver && !waterPass;
        boolean neoSkyForward = neoSky && EmbeddiumWaterShaderCompileContext.isTranslucentPass();
        boolean bl2 = neoSkyInline = neoSky && !neoSkyForward;
        if (!(receiverShaders || waterPass || neoSky || "blocks/block_layer_opaque.vsh".equals(name.m_135815_()))) {
            return;
        }
        if ("blocks/block_layer_opaque.vsh".equals(name.m_135815_())) {
            replacement = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)(waterPass ? "shaders/embeddium/block_layer_water.vsh" : (neoSkyForward ? "shaders/embeddium/block_layer_neoskycelestia_forward.vsh" : (neoSkyInline ? "shaders/embeddium/block_layer_neoskycelestia.vsh" : (receiverShaders ? "shaders/embeddium/block_layer_opaque.vsh" : "shaders/embeddium/block_layer_plantswave.vsh")))));
        } else if ("blocks/block_layer_opaque.fsh".equals(name.m_135815_())) {
            replacement = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)(waterPass ? "shaders/embeddium/block_layer_water.fsh" : (neoGpuVanilla ? "shaders/embeddium/block_layer_neogpuvanilla.fsh" : (neoSkyForward ? "shaders/embeddium/block_layer_neoskycelestia_forward.fsh" : (neoSkyInline ? "shaders/embeddium/block_layer_neoskycelestia.fsh" : "shaders/embeddium/block_layer_opaque.fsh")))));
        } else {
            return;
        }
        try {
            Resource resource = Minecraft.m_91087_().m_91098_().m_215593_(replacement);
            try (InputStream stream = resource.m_215507_();){
                String source = IOUtils.toString((InputStream)stream, (Charset)StandardCharsets.UTF_8);
                if ((receiverShaders || neoSkyForward) && !waterPass) {
                    source = source.replace("#define LUXIUM_ENTITY_SHADOWS_ENABLED 0", "#define LUXIUM_ENTITY_SHADOWS_ENABLED " + (Config.isFeatureEnabled(Config.CLIENT.skyEntityShadowsEnabled) ? "1" : "0"));
                }
                if (receiverShaders && !waterPass && (neoGpuVanilla || localReceiver)) {
                    source = source.replace("#define LUXIUM_NGV_CUTOUT_ENABLED 0", "#define LUXIUM_NGV_CUTOUT_ENABLED " + (neoGpuVanilla && !Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaCutoutEnabled) ? "0" : "1"));
                }
                if (BlockLightTest.enabled() && neoSky && name.m_135815_().endsWith(".vsh")) {
                    source = source.replaceFirst("#version 330 core", "#version 330 core\n#define LUXIUM_BLOCKLIGHT_TEST 1");
                }
                cir.setReturnValue((Object)source);
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to load Luxium Embeddium shader " + replacement, exception);
        }
    }
}

