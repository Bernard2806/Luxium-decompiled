/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterShadersEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ShaderManager {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private static ShaderInstance polishedAndesiteShader;
    @Nullable
    private static ShaderInstance metalReflectionShader;
    @Nullable
    private static ShaderInstance godRaysPostShader;
    @Nullable
    private static ShaderInstance skyGodRaysShader;
    @Nullable
    private static ShaderInstance lensFlareShader;
    @Nullable
    private static ShaderInstance postEffectsCombinedShader;
    @Nullable
    private static ShaderInstance skyShader;
    @Nullable
    private static ShaderInstance skyBakeShader;
    @Nullable
    private static ShaderInstance grassShader;
    @Nullable
    private static ShaderInstance floodShadowCompositeShader;
    @Nullable
    private static ShaderInstance fogShader;
    @Nullable
    private static ShaderInstance tonemapShader;
    @Nullable
    private static ShaderInstance postEffectCompositeShader;
    @Nullable
    private static ShaderInstance airDistortionShader;
    @Nullable
    private static ShaderInstance blackHoleShader;
    @Nullable
    private static ShaderInstance cloudShadowCasterShader;
    @Nullable
    private static ShaderInstance depthToDistanceShader;
    @Nullable
    private static ShaderInstance neoGpuVanillaBakeShader;
    @Nullable
    private static ShaderInstance neoGpuVanillaResolveShader;
    @Nullable
    private static ShaderInstance neoGpuVanillaFastResolveShader;
    @Nullable
    private static ShaderInstance gpuHardShadowBakeShader;
    @Nullable
    private static ShaderInstance configGlassShader;
    @Nullable
    private static ShaderInstance youShader;
    @Nullable
    private static ShaderInstance waterSurfaceShader;
    @Nullable
    private static ShaderInstance rainPuddleShader;
    @Nullable
    private static ShaderInstance wetSurfaceShader;
    @Nullable
    private static ShaderInstance lsrUpscaleShader;
    @Nullable
    private static ShaderInstance temporalReprojectionShader;
    @Nullable
    private static ShaderInstance temporalMotionShader;
    @Nullable
    private static ShaderInstance temporalPrepareShader;
    @Nullable
    private static ShaderInstance kawaseSourceShader;
    @Nullable
    private static ShaderInstance kawaseDownShader;
    @Nullable
    private static ShaderInstance kawaseUpShader;
    @Nullable
    private static ShaderInstance kawaseCompositeShader;

    private ShaderManager() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        ShaderInstance instance;
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"polished_andesite_reflection"), DefaultVertexFormat.f_85820_);
            event.registerShader(instance, shader -> {
                polishedAndesiteShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load polished-andesite reflection shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"metal_reflection"), DefaultVertexFormat.f_85820_);
            event.registerShader(instance, shader -> {
                metalReflectionShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load metal reflection shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"godrays_post"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                godRaysPostShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load god rays post shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"skygodrays"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                skyGodRaysShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load sky god rays shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"lensflare"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                lensFlareShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load lens flare shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"posteffects_combined"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                postEffectsCombinedShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load fused post-effects shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"sky"), DefaultVertexFormat.f_85814_);
            event.registerShader(instance, shader -> {
                skyShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load sky shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"sky_bake"), DefaultVertexFormat.f_85814_);
            event.registerShader(instance, shader -> {
                skyBakeShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load sky bake shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"fog"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                fogShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load fog shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"tonemap"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                tonemapShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load tonemap shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"posteffect_composite"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                postEffectCompositeShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load post-effect composite shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"grass"), DefaultVertexFormat.f_85812_);
            event.registerShader(instance, shader -> {
                grassShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load grass shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"flood_shadow_composite"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                floodShadowCompositeShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load flood shadow composite shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"air_distortion"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                airDistortionShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load air distortion shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"blackhole"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                blackHoleShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load black hole shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"cloud_shadow_caster"), DefaultVertexFormat.f_85822_);
            event.registerShader(instance, shader -> {
                cloudShadowCasterShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load cloud shadow caster shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"depth_to_distance"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                depthToDistanceShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load depth_to_distance shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"neogpuvanilla_bake"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                neoGpuVanillaBakeShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load NeoGpuVanilla bake shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"neogpuvanilla_resolve"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                neoGpuVanillaResolveShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load NeoGpuVanilla resolve shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"neogpuvanilla_fast_resolve"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                neoGpuVanillaFastResolveShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load NeoGpuVanilla Fast GPU Shadows resolve shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"gpu_hard_shadow_bake"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                gpuHardShadowBakeShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load baked Cubemap Hard receiver shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"config_glass"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                configGlassShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load config glass shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"water_surface"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                waterSurfaceShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load water surface shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"rain_puddle"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                rainPuddleShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load new rain puddle shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"wet_surface"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                wetSurfaceShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load wet surface shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"lsr_upscale"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                lsrUpscaleShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load LSR upscale shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"temporal_reprojection"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                temporalReprojectionShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load temporal reprojection shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"temporal_motion"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                temporalMotionShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load temporal motion shader", (Throwable)e);
        }
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"temporal_prepare"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                temporalPrepareShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load temporal prepare shader", (Throwable)e);
        }
        ShaderManager.registerFullscreen(event, "kawase_source", shader -> {
            kawaseSourceShader = shader;
        });
        ShaderManager.registerFullscreen(event, "kawase_down", shader -> {
            kawaseDownShader = shader;
        });
        ShaderManager.registerFullscreen(event, "kawase_up", shader -> {
            kawaseUpShader = shader;
        });
        ShaderManager.registerFullscreen(event, "kawase_composite", shader -> {
            kawaseCompositeShader = shader;
        });
        try {
            instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"youshader"), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, shader -> {
                youShader = shader;
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to load youshader shader", (Throwable)e);
        }
    }

    @Nullable
    public static ShaderInstance getPolishedAndesiteShader() {
        return polishedAndesiteShader;
    }

    @Nullable
    public static ShaderInstance getMetalReflectionShader() {
        return metalReflectionShader;
    }

    @Nullable
    public static ShaderInstance getGodRaysPostShader() {
        return godRaysPostShader;
    }

    @Nullable
    public static ShaderInstance getSkyGodRaysShader() {
        return skyGodRaysShader;
    }

    @Nullable
    public static ShaderInstance getLensFlareShader() {
        return lensFlareShader;
    }

    @Nullable
    public static ShaderInstance getPostEffectsCombinedShader() {
        return postEffectsCombinedShader;
    }

    @Nullable
    public static ShaderInstance getSkyShader() {
        return skyShader;
    }

    @Nullable
    public static ShaderInstance getSkyBakeShader() {
        return skyBakeShader;
    }

    @Nullable
    public static ShaderInstance getFogShader() {
        return fogShader;
    }

    @Nullable
    public static ShaderInstance getTonemapShader() {
        return tonemapShader;
    }

    @Nullable
    public static ShaderInstance getPostEffectCompositeShader() {
        return postEffectCompositeShader;
    }

    @Nullable
    public static ShaderInstance getGrassShader() {
        return grassShader;
    }

    @Nullable
    public static ShaderInstance getFloodShadowCompositeShader() {
        return floodShadowCompositeShader;
    }

    @Nullable
    public static ShaderInstance getAirDistortionShader() {
        return airDistortionShader;
    }

    @Nullable
    public static ShaderInstance getBlackHoleShader() {
        return blackHoleShader;
    }

    @Nullable
    public static ShaderInstance getCloudShadowCasterShader() {
        return cloudShadowCasterShader;
    }

    @Nullable
    public static ShaderInstance getDepthToDistanceShader() {
        return depthToDistanceShader;
    }

    @Nullable
    public static ShaderInstance getNeoGpuVanillaBakeShader() {
        return neoGpuVanillaBakeShader;
    }

    @Nullable
    public static ShaderInstance getNeoGpuVanillaResolveShader() {
        return neoGpuVanillaResolveShader;
    }

    @Nullable
    public static ShaderInstance getNeoGpuVanillaFastResolveShader() {
        return neoGpuVanillaFastResolveShader;
    }

    @Nullable
    public static ShaderInstance getGpuHardShadowBakeShader() {
        return gpuHardShadowBakeShader;
    }

    @Nullable
    public static ShaderInstance getConfigGlassShader() {
        return configGlassShader;
    }

    @Nullable
    public static ShaderInstance getYouShader() {
        return youShader;
    }

    @Nullable
    public static ShaderInstance getWaterSurfaceShader() {
        return waterSurfaceShader;
    }

    @Nullable
    public static ShaderInstance getRainPuddleShader() {
        return rainPuddleShader;
    }

    @Nullable
    public static ShaderInstance getWetSurfaceShader() {
        return wetSurfaceShader;
    }

    @Nullable
    public static ShaderInstance getLsrUpscaleShader() {
        return lsrUpscaleShader;
    }

    @Nullable
    public static ShaderInstance getTemporalReprojectionShader() {
        return temporalReprojectionShader;
    }

    @Nullable
    public static ShaderInstance getTemporalMotionShader() {
        return temporalMotionShader;
    }

    @Nullable
    public static ShaderInstance getTemporalPrepareShader() {
        return temporalPrepareShader;
    }

    @Nullable
    public static ShaderInstance getKawaseSourceShader() {
        return kawaseSourceShader;
    }

    @Nullable
    public static ShaderInstance getKawaseDownShader() {
        return kawaseDownShader;
    }

    @Nullable
    public static ShaderInstance getKawaseUpShader() {
        return kawaseUpShader;
    }

    @Nullable
    public static ShaderInstance getKawaseCompositeShader() {
        return kawaseCompositeShader;
    }

    private static void registerFullscreen(RegisterShadersEvent event, String name, Consumer<ShaderInstance> setter) {
        try {
            ShaderInstance instance = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)name), DefaultVertexFormat.f_85817_);
            event.registerShader(instance, setter);
        }
        catch (IOException e) {
            LOGGER.error("Failed to load {} shader", (Object)name, (Object)e);
            setter.accept(null);
        }
    }
}

