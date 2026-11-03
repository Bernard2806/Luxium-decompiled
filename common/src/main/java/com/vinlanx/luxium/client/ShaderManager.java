package com.vinlanx.luxium.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.vinlanx.luxium.platform.ShaderRegistrar;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ShaderManager {
    private static final Logger
    LOGGER = LogManager.getLogger();
    @Nullable private static ShaderInstance polishedAndesiteShader;
    @Nullable private static ShaderInstance metalReflectionShader;
    @Nullable private static ShaderInstance godRaysPostShader;
    @Nullable private static ShaderInstance skyGodRaysShader; @Nullable private static ShaderInstance lensFlareShader; @Nullable private static ShaderInstance postEffectsCombinedShader; @Nullable private static ShaderInstance skyShader; @Nullable private static ShaderInstance skyBakeShader; @Nullable private static ShaderInstance grassShader; @Nullable private static ShaderInstance entityShadowSolidShader; @Nullable private static ShaderInstance entityShadowCutoutShader; @Nullable private static ShaderInstance floodShadowCompositeShader; @Nullable private static ShaderInstance fogShader; @Nullable private static ShaderInstance tonemapShader; @Nullable private static ShaderInstance postEffectCompositeShader; @Nullable private static ShaderInstance airDistortionShader; @Nullable private static ShaderInstance blackHoleShader; @Nullable private static ShaderInstance cloudShadowCasterShader; @Nullable private static ShaderInstance depthToDistanceShader; @Nullable private static ShaderInstance neoGpuVanillaBakeShader; @Nullable private static ShaderInstance neoGpuVanillaResolveShader; @Nullable private static ShaderInstance neoGpuVanillaFastResolveShader; @Nullable private static ShaderInstance gpuHardShadowBakeShader; @Nullable private static ShaderInstance configGlassShader; @Nullable private static ShaderInstance youShader; @Nullable private static ShaderInstance waterSurfaceShader; @Nullable private static ShaderInstance rainPuddleShader; @Nullable private static ShaderInstance wetSurfaceShader; @Nullable private static ShaderInstance lsrUpscaleShader; @Nullable private static ShaderInstance temporalReprojectionShader; @Nullable private static ShaderInstance temporalMotionShader; @Nullable private static ShaderInstance temporalPrepareShader; @Nullable private static ShaderInstance kawaseSourceShader; @Nullable private static ShaderInstance kawaseDownShader; @Nullable private static ShaderInstance kawaseUpShader; @Nullable private static ShaderInstance kawaseCompositeShader; private ShaderManager()



































 { }

    public static void onRegisterShaders(ShaderRegistrar event) {

        entityShadowSolidShader = null;
        entityShadowCutoutShader = null;

        try { event.registerShader(
                new ResourceLocation("luxium", "entity_shadow_solid"), DefaultVertexFormat.NEW_ENTITY, shader -> entityShadowSolidShader = shader );
 event.registerShader(
                new ResourceLocation("luxium", "entity_shadow_cutout"), DefaultVertexFormat.NEW_ENTITY, shader -> entityShadowCutoutShader = shader ); }
 catch (IOException var32) {
            throw new IllegalStateException("Failed to load entity shadow caster shaders", var32);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "polished_andesite_reflection"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, shader -> polishedAndesiteShader = shader ); }
 catch (IOException var31) {
            LOGGER.error("Failed to load polished-andesite reflection shader", var31);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "metal_reflection"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, shader -> metalReflectionShader = shader ); }
 catch (IOException var30) {
            LOGGER.error("Failed to load metal reflection shader", var30);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "godrays_post"), DefaultVertexFormat.POSITION_TEX, shader -> godRaysPostShader = shader);
        } catch (IOException var29) {
            LOGGER.error("Failed to load god rays post shader", var29);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "skygodrays"), DefaultVertexFormat.POSITION_TEX, shader -> skyGodRaysShader = shader);
        } catch (IOException var28) {
            LOGGER.error("Failed to load sky god rays shader", var28);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "lensflare"), DefaultVertexFormat.POSITION_TEX, shader -> lensFlareShader = shader);
        } catch (IOException var27) {
            LOGGER.error("Failed to load lens flare shader", var27);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "posteffects_combined"), DefaultVertexFormat.POSITION_TEX, shader -> postEffectsCombinedShader = shader ); }
 catch (IOException var26) {
            LOGGER.error("Failed to load fused post-effects shader", var26);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "sky"), DefaultVertexFormat.POSITION, shader -> skyShader = shader);
        } catch (IOException var25) {
            LOGGER.error("Failed to load sky shader", var25);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "sky_bake"), DefaultVertexFormat.POSITION, shader -> skyBakeShader = shader);
        } catch (IOException var24) {
            LOGGER.error("Failed to load sky bake shader", var24);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "fog"), DefaultVertexFormat.POSITION_TEX, shader -> fogShader = shader);
        } catch (IOException var23) {
            LOGGER.error("Failed to load fog shader", var23);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "tonemap"), DefaultVertexFormat.POSITION_TEX, shader -> tonemapShader = shader);
        } catch (IOException var22) {
            LOGGER.error("Failed to load tonemap shader", var22);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "posteffect_composite"), DefaultVertexFormat.POSITION_TEX, shader -> postEffectCompositeShader = shader ); }
 catch (IOException var21) {
            LOGGER.error("Failed to load post-effect composite shader", var21);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "grass"), DefaultVertexFormat.NEW_ENTITY, shader -> grassShader = shader);
        } catch (IOException var20) {
            LOGGER.error("Failed to load grass shader", var20);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "flood_shadow_composite"), DefaultVertexFormat.POSITION_TEX, shader -> floodShadowCompositeShader = shader ); }
 catch (IOException var19) {
            LOGGER.error("Failed to load flood shadow composite shader", var19);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "air_distortion"), DefaultVertexFormat.POSITION_TEX, shader -> airDistortionShader = shader);
        } catch (IOException var18) {
            LOGGER.error("Failed to load air distortion shader", var18);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "blackhole"), DefaultVertexFormat.POSITION_TEX, shader -> blackHoleShader = shader);
        } catch (IOException var17) {
            LOGGER.error("Failed to load black hole shader", var17);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "cloud_shadow_caster"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, shader -> cloudShadowCasterShader = shader ); }
 catch (IOException var16) {
            LOGGER.error("Failed to load cloud shadow caster shader", var16);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "depth_to_distance"), DefaultVertexFormat.POSITION_TEX, shader -> depthToDistanceShader = shader ); }
 catch (IOException var15) {
            LOGGER.error("Failed to load depth_to_distance shader", var15);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "neogpuvanilla_bake"), DefaultVertexFormat.POSITION_TEX, shader -> neoGpuVanillaBakeShader = shader ); }
 catch (IOException var14) {
            LOGGER.error("Failed to load NeoGpuVanilla bake shader", var14);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "neogpuvanilla_resolve"), DefaultVertexFormat.POSITION_TEX, shader -> neoGpuVanillaResolveShader = shader ); }
 catch (IOException var13) {
            LOGGER.error("Failed to load NeoGpuVanilla resolve shader", var13);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "neogpuvanilla_fast_resolve"), DefaultVertexFormat.POSITION_TEX, shader -> neoGpuVanillaFastResolveShader = shader ); }
 catch (IOException var12) {
            LOGGER.error("Failed to load NeoGpuVanilla Fast GPU Shadows resolve shader", var12);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "gpu_hard_shadow_bake"), DefaultVertexFormat.POSITION_TEX, shader -> gpuHardShadowBakeShader = shader ); }
 catch (IOException var11) {
            LOGGER.error("Failed to load baked Cubemap Hard receiver shader", var11);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "config_glass"), DefaultVertexFormat.POSITION_TEX, shader -> configGlassShader = shader);
        } catch (IOException var10) {
            LOGGER.error("Failed to load config glass shader", var10);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "water_surface"), DefaultVertexFormat.POSITION_TEX, shader -> waterSurfaceShader = shader);
        } catch (IOException var9) {
            LOGGER.error("Failed to load water surface shader", var9);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "rain_puddle"), DefaultVertexFormat.POSITION_TEX, shader -> rainPuddleShader = shader);
        } catch (IOException var8) {
            LOGGER.error("Failed to load new rain puddle shader", var8);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "wet_surface"), DefaultVertexFormat.POSITION_TEX, shader -> wetSurfaceShader = shader);
        } catch (IOException var7) {
            LOGGER.error("Failed to load wet surface shader", var7);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "lsr_upscale"), DefaultVertexFormat.POSITION_TEX, shader -> lsrUpscaleShader = shader);
        } catch (IOException var6) {
            LOGGER.error("Failed to load LSR upscale shader", var6);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "temporal_reprojection"), DefaultVertexFormat.POSITION_TEX, shader -> temporalReprojectionShader = shader ); }
 catch (IOException var5) {
            LOGGER.error("Failed to load temporal reprojection shader", var5);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "temporal_motion"), DefaultVertexFormat.POSITION_TEX, shader -> temporalMotionShader = shader);
        } catch (IOException var4) {
            LOGGER.error("Failed to load temporal motion shader", var4);
        }

        try {
            event.registerShader(new ResourceLocation("luxium", "temporal_prepare"), DefaultVertexFormat.POSITION_TEX, shader -> temporalPrepareShader = shader);
        } catch (IOException var3) {
            LOGGER.error("Failed to load temporal prepare shader", var3);
        }

        registerFullscreen(event, "kawase_source", shader -> kawaseSourceShader = shader);
        registerFullscreen(event, "kawase_down", shader -> kawaseDownShader = shader);
        registerFullscreen(event, "kawase_up", shader -> kawaseUpShader = shader);
        registerFullscreen(event, "kawase_composite", shader -> kawaseCompositeShader = shader);

        try {

            event.registerShader(new ResourceLocation("luxium", "youshader"), DefaultVertexFormat.POSITION_TEX, shader -> youShader = shader);
        } catch (IOException var2) {
            LOGGER.error("Failed to load youshader shader", var2);
        }

    }

    @Nullable public static ShaderInstance getPolishedAndesiteShader() {
 return polishedAndesiteShader;
    }

    public static ShaderInstance getEntityShadowCasterShader(boolean solid) {
        return Objects.requireNonNull(solid ? entityShadowSolidShader : entityShadowCutoutShader, "Entity shadow caster shaders have not been registered");
    }

    @Nullable public static ShaderInstance getMetalReflectionShader() {

        return metalReflectionShader;
    }

    @Nullable public static ShaderInstance getGodRaysPostShader() {
 return godRaysPostShader;
    }

    @Nullable public static ShaderInstance getSkyGodRaysShader() {
 return skyGodRaysShader;
    }

    @Nullable public static ShaderInstance getLensFlareShader() {
 return lensFlareShader;
    }

    @Nullable public static ShaderInstance getPostEffectsCombinedShader() {
 return postEffectsCombinedShader;
    }

    @Nullable public static ShaderInstance getSkyShader() {
 return skyShader;
    }

    @Nullable public static ShaderInstance getSkyBakeShader() {
 return skyBakeShader;
    }

    @Nullable public static ShaderInstance getFogShader() {
 return fogShader;
    }

    @Nullable public static ShaderInstance getTonemapShader() {
 return tonemapShader;
    }

    @Nullable public static ShaderInstance getPostEffectCompositeShader() {
 return postEffectCompositeShader;
    }

    @Nullable public static ShaderInstance getGrassShader() {
 return grassShader;
    }

    @Nullable public static ShaderInstance getFloodShadowCompositeShader() {
 return floodShadowCompositeShader;
    }

    @Nullable public static ShaderInstance getAirDistortionShader() {
 return airDistortionShader;
    }

    @Nullable public static ShaderInstance getBlackHoleShader() {
 return blackHoleShader;
    }

    @Nullable public static ShaderInstance getCloudShadowCasterShader() {
 return cloudShadowCasterShader;
    }

    @Nullable public static ShaderInstance getDepthToDistanceShader() {
 return depthToDistanceShader;
    }

    @Nullable public static ShaderInstance getNeoGpuVanillaBakeShader() {
 return neoGpuVanillaBakeShader;
    }

    @Nullable public static ShaderInstance getNeoGpuVanillaResolveShader() {
 return neoGpuVanillaResolveShader;
    }

    @Nullable public static ShaderInstance getNeoGpuVanillaFastResolveShader() {
 return neoGpuVanillaFastResolveShader;
    }

    @Nullable public static ShaderInstance getGpuHardShadowBakeShader() {
 return gpuHardShadowBakeShader;
    }

    @Nullable public static ShaderInstance getConfigGlassShader() {
 return configGlassShader;
    }

    @Nullable public static ShaderInstance getYouShader() {
 return youShader;
    }

    @Nullable public static ShaderInstance getWaterSurfaceShader() {
 return waterSurfaceShader;
    }

    @Nullable public static ShaderInstance getRainPuddleShader() {
 return rainPuddleShader;
    }

    @Nullable public static ShaderInstance getWetSurfaceShader() {
 return wetSurfaceShader;
    }

    @Nullable public static ShaderInstance getLsrUpscaleShader() {
 return lsrUpscaleShader;
    }

    @Nullable public static ShaderInstance getTemporalReprojectionShader() {
 return temporalReprojectionShader;
    }

    @Nullable public static ShaderInstance getTemporalMotionShader() {
 return temporalMotionShader;
    }

    @Nullable public static ShaderInstance getTemporalPrepareShader() {
 return temporalPrepareShader;
    }

    @Nullable public static ShaderInstance getKawaseSourceShader() {
 return kawaseSourceShader;
    }

    @Nullable public static ShaderInstance getKawaseDownShader() {
 return kawaseDownShader;
    }

    @Nullable public static ShaderInstance getKawaseUpShader() {
 return kawaseUpShader;
    }

    @Nullable public static ShaderInstance getKawaseCompositeShader() {
 return kawaseCompositeShader;
    }

    private static void registerFullscreen(ShaderRegistrar event, String name, Consumer<ShaderInstance> setter) {
        try {

            event.registerShader(new ResourceLocation("luxium", name), DefaultVertexFormat.POSITION_TEX, setter);
        } catch (IOException var4) {
            LOGGER.error("Failed to load {} shader", name, var4);
            setter.accept(null);
        }
    }
    }
