/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.vinlanx.luxium.client.ConfigScreen;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ConfigScreen.ConfigOption;
import com.vinlanx.luxium.client.Sky;
import com.vinlanx.luxium.client.kawase.KawaseBloomRenderer;
import com.vinlanx.luxium.client.posteffects.fog;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigScreenModel {
    private ConfigScreenModel() {
    }

    public static List<Category> create() {
        Config.Client c = Config.CLIENT;
        ArrayList<Category> categories = new ArrayList<Category>();
        BooleanSupplier floodActive = () -> (Boolean)c.rtxEnabled.get() != false && (Boolean)c.neoGpuVanillaEnabled.get() == false;
        ArrayList<Row> generalRows = new ArrayList<Row>();
        generalRows.add(Row.option(ConfigScreenModel.b("rtx_enabled", "option.luxium.rtx_enabled", "tooltip.luxium.rtx_enabled", c.rtxEnabled).enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false)));
        generalRows.add(Row.option(ConfigScreenModel.b("rtx_lava_tracing_enabled", "option.luxium.rtx_lava_tracing_enabled", "tooltip.luxium.rtx_lava_tracing_enabled", c.rtxLavaTracingEnabled).enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false)));
        generalRows.add(Row.option(ConfigScreenModel.i("rtx_world_tracing_workers", "option.luxium.rtx_world_tracing_workers", "tooltip.luxium.rtx_world_tracing_workers", c.rtxWorldTracingWorkers, 1, Config.MAX_RTX_WORLD_TRACING_WORKERS, "").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false)));
        generalRows.add(Row.header("header.luxium.flood_rt"));
        generalRows.add(Row.option(ConfigScreenModel.i("flood_radius_cap", "option.luxium.flood_radius_cap", "tooltip.luxium.flood_radius_cap", c.floodRadiusCap, 16, 36, " blocks").enabledWhen(floodActive)));
        generalRows.add(Row.option(ConfigScreenModel.d("flood_penumbra_softness", "option.luxium.flood_penumbra_softness", "tooltip.luxium.flood_penumbra_softness", c.floodPenumbraSoftness, 0.0, 1.0, 0.01, "").enabledWhen(floodActive)));
        generalRows.add(Row.option(ConfigScreenModel.d("flood_corner_seal", "option.luxium.flood_corner_seal", "tooltip.luxium.flood_corner_seal", c.floodCornerSeal, 0.0, 4.0, 0.05, "x").enabledWhen(floodActive)));
        generalRows.add(Row.option(ConfigScreenModel.d("flood_directional_bias", "option.luxium.flood_directional_bias", "tooltip.luxium.flood_directional_bias", c.floodDirectionalBias, 0.25, 4.0, 0.05, "x").enabledWhen(floodActive)));
        generalRows.add(Row.option(ConfigScreenModel.i("flood_update_budget", "option.luxium.flood_update_budget", "tooltip.luxium.flood_update_budget", c.floodUpdateBudget, 1, 64, " /50ms").enabledWhen(floodActive)));
        categories.add(new Category("general", (Component)Component.m_237115_((String)"category.luxium.general"), List.copyOf(generalRows)));
        categories.add(ConfigScreenModel.category("block_light_test", "category.luxium.block_light_test", ConfigScreenModel.b("block_light_test_enabled", "option.luxium.block_light_test_enabled", "tooltip.luxium.block_light_test_enabled", c.blockLightTestEnabled)));
        categories.add(ConfigScreenModel.category("reflection", "category.luxium.reflection", ConfigScreenModel.b("reflection_enabled", "option.luxium.reflection_enabled", "tooltip.luxium.reflection_enabled", c.reflectionEnabled), ConfigScreenModel.b("reflection_bilinear_filtering", "option.luxium.reflection_bilinear_filtering", "tooltip.luxium.reflection_bilinear_filtering", c.reflectionBilinearFiltering), ConfigScreenModel.d("reflection_render_scale", "option.luxium.reflection_render_scale", "tooltip.luxium.reflection_render_scale", c.reflectionRenderScale, 0.1, 1.0, 0.01, "x"), ConfigScreenModel.i("reflection_distance", "option.luxium.reflection_distance", "tooltip.luxium.reflection_distance", c.reflectionDistance, 5, 200, " blocks"), ConfigScreenModel.i("reflection_scan_interval", "option.luxium.reflection_scan_interval", "tooltip.luxium.reflection_scan_interval", c.reflectionScanIntervalMs, 5, 5000, " ms")));
        BooleanSupplier vanillaGpuActive = () -> ((ForgeConfigSpec.BooleanValue)c.neoGpuVanillaEnabled).get();
        categories.add(ConfigScreenModel.category("vanilla_gpu", "category.luxium.vanilla_gpu", ConfigScreenModel.b("neo_gpu_vanilla_enabled", "option.luxium.neo_gpu_vanilla_enabled", "tooltip.luxium.neo_gpu_vanilla_enabled", c.neoGpuVanillaEnabled).onChanged(enabled -> NeoGpuVanilla.onConfigChanged()), ConfigScreenModel.i("neo_gpu_vanilla_capture_resolution", "option.luxium.neo_gpu_vanilla_capture_resolution", "tooltip.luxium.neo_gpu_vanilla_capture_resolution", c.neoGpuVanillaCaptureResolution, 1, 512, 1, " px/block").enabledWhen(vanillaGpuActive).onChanged(ignored -> NeoGpuVanilla.requestRebuild()), ConfigScreenModel.i("neo_gpu_vanilla_distance", "option.luxium.neo_gpu_vanilla_distance", "tooltip.luxium.neo_gpu_vanilla_distance", c.neoGpuVanillaDistance, 16, 64, " blocks").enabledWhen(vanillaGpuActive).onChanged(ignored -> NeoGpuVanilla.requestRebake()), ConfigScreenModel.i("neo_gpu_vanilla_capture_budget", "option.luxium.neo_gpu_vanilla_capture_budget", "tooltip.luxium.neo_gpu_vanilla_capture_budget", c.neoGpuVanillaCaptureBudget, 1, 300, " faces/frame").enabledWhen(vanillaGpuActive), ConfigScreenModel.i("neo_gpu_vanilla_max_sources", "option.luxium.neo_gpu_vanilla_max_sources", "tooltip.luxium.neo_gpu_vanilla_max_sources", c.neoGpuVanillaMaxSources, 4, 40, "").enabledWhen(vanillaGpuActive).onChanged(ignored -> NeoGpuVanilla.requestRebake()), ConfigScreenModel.b("neo_gpu_vanilla_cutout_enabled", "option.luxium.neo_gpu_vanilla_cutout_enabled", "tooltip.luxium.neo_gpu_vanilla_cutout_enabled", c.neoGpuVanillaCutoutEnabled).enabledWhen(vanillaGpuActive), ConfigScreenModel.b("neo_gpu_vanilla_fast_shadows_enabled", "option.luxium.neo_gpu_vanilla_fast_shadows_enabled", "tooltip.luxium.neo_gpu_vanilla_fast_shadows_enabled", c.neoGpuVanillaFastShadowsEnabled).enabledWhen(vanillaGpuActive).onChanged(ignored -> NeoGpuVanilla.requestRebake()), ConfigScreenModel.d("neo_gpu_vanilla_fast_shadow_strength", "option.luxium.neo_gpu_vanilla_fast_shadow_strength", "tooltip.luxium.neo_gpu_vanilla_fast_shadow_strength", c.neoGpuVanillaFastShadowStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> vanillaGpuActive.getAsBoolean() && (Boolean)c.neoGpuVanillaFastShadowsEnabled.get() != false).onChanged(ignored -> NeoGpuVanilla.requestRebake()), ConfigScreenModel.d("neo_gpu_vanilla_fast_diffuse_wrap", "option.luxium.neo_gpu_vanilla_fast_diffuse_wrap", "tooltip.luxium.neo_gpu_vanilla_fast_diffuse_wrap", c.neoGpuVanillaFastDiffuseWrap, 0.0, 0.5, 0.01, "").enabledWhen(() -> vanillaGpuActive.getAsBoolean() && (Boolean)c.neoGpuVanillaFastShadowsEnabled.get() != false).onChanged(ignored -> NeoGpuVanilla.requestRebake()), ConfigScreenModel.b("neo_gpu_vanilla_geometry_updates", "option.luxium.neo_gpu_vanilla_geometry_updates", "tooltip.luxium.neo_gpu_vanilla_geometry_updates", c.neoGpuVanillaGeometryUpdates).enabledWhen(vanillaGpuActive), ConfigScreenModel.b("neo_gpu_vanilla_debug", "option.luxium.neo_gpu_vanilla_debug", "tooltip.luxium.neo_gpu_vanilla_debug", c.neoGpuVanillaDebug).enabledWhen(vanillaGpuActive)));
        ConfigOption<Config.GpuLocalLightingMode> gpuLightingMode = ConfigOption.enumeration("gpu_local_lighting_mode", "option.luxium.gpu_local_lighting_mode", "tooltip.luxium.gpu_local_lighting_mode", c.gpuLocalLightingMode, List.of(Config.GpuLocalLightingMode.GPU_ONLY, Config.GpuLocalLightingMode.HYBRID, Config.GpuLocalLightingMode.NEOFLOOD_ONLY), value -> Component.m_237115_((String)("value.luxium.gpu_local_lighting_mode." + value.name().toLowerCase(Locale.ROOT))), ignored -> {});
        if (c.gpuLocalShadowMode.get() == Config.GpuLocalShadowMode.FAST_SPREAD) {
            c.gpuLocalShadowMode.set((Object)Config.GpuLocalShadowMode.CUBEMAP_HARD);
            c.gpuLocalShadowMode.save();
        }
        if (((Boolean)c.gpuHardShadowBakeEnabled.get()).booleanValue()) {
            if (c.gpuLocalShadowMode.get() != Config.GpuLocalShadowMode.CUBEMAP_HARD) {
                c.gpuLocalShadowMode.set((Object)Config.GpuLocalShadowMode.CUBEMAP_HARD);
                c.gpuLocalShadowMode.save();
            }
            if (((Boolean)c.entityShadowsEnabled.get()).booleanValue()) {
                c.entityShadowsEnabled.set((Object)false);
                c.entityShadowsEnabled.save();
            }
        }
        ConfigOption<Config.GpuLocalShadowMode> gpuShadowMode = ConfigOption.enumeration("gpu_local_shadow_mode", "option.luxium.gpu_local_shadow_mode", "tooltip.luxium.gpu_local_shadow_mode", c.gpuLocalShadowMode, List.of(Config.GpuLocalShadowMode.CUBEMAP_HARD, Config.GpuLocalShadowMode.CUBEMAP_SOFT), value -> Component.m_237115_((String)("value.luxium.gpu_local_shadow_mode." + value.name().toLowerCase(Locale.ROOT))), ignored -> {});
        categories.add(ConfigScreenModel.category("realistic_shadows", "category.luxium.realistic_shadows", ConfigScreenModel.b("gpu_shadows_enabled", "option.luxium.gpu_shadows_enabled", "tooltip.luxium.gpu_shadows_enabled", c.gpuShadowsEnabled).enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false), gpuLightingMode.enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false), gpuShadowMode.enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false && c.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY && (Boolean)c.gpuHardShadowBakeEnabled.get() == false), ConfigScreenModel.b("gpu_hard_shadow_bake_enabled", "option.luxium.gpu_hard_shadow_bake_enabled", "tooltip.luxium.gpu_hard_shadow_bake_enabled", c.gpuHardShadowBakeEnabled).enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false && c.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY).onChanged(enabled -> {
            if (enabled.booleanValue()) {
                c.gpuLocalShadowMode.set((Object)Config.GpuLocalShadowMode.CUBEMAP_HARD);
                c.gpuLocalShadowMode.save();
                c.entityShadowsEnabled.set((Object)false);
                c.entityShadowsEnabled.save();
            }
            GpuNeoShadows.onConfigChanged();
        }), ConfigScreenModel.i("gpu_hard_shadow_capture_budget", "option.luxium.gpu_hard_shadow_capture_budget", "tooltip.luxium.gpu_hard_shadow_capture_budget", c.gpuHardShadowCaptureBudget, 1, 300, " faces/frame").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false && c.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY && (Boolean)c.gpuHardShadowBakeEnabled.get() != false), ConfigScreenModel.i("gpu_fast_spread_max_lights", "option.luxium.gpu_fast_spread_max_lights", "tooltip.luxium.gpu_fast_spread_max_lights", c.gpuFastSpreadMaxLights, 4, 32, "").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false && c.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY), ConfigScreenModel.i("gpu_local_light_distance", "option.luxium.gpu_local_light_distance", "tooltip.luxium.gpu_local_light_distance", c.gpuLocalLightDistance, 4, 64, " blocks").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuShadowsEnabled.get() != false && (Boolean)c.neoCpuShadowsEnabled.get() == false && c.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY), ConfigScreenModel.b("entity_shadows_enabled", "option.luxium.entity_shadows_enabled", "tooltip.luxium.entity_shadows_enabled", c.entityShadowsEnabled).enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuHardShadowBakeEnabled.get() == false), ConfigScreenModel.i("entity_shadow_update_fps", "option.luxium.entity_shadow_update_fps", "tooltip.luxium.entity_shadow_update_fps", c.entityShadowUpdateFpsLimit, 10, 120, " FPS").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && (Boolean)c.gpuHardShadowBakeEnabled.get() == false && (Boolean)c.entityShadowsEnabled.get() != false && ((Boolean)c.realisticShadowsEnabled.get() != false || (Boolean)c.neoCpuShadowsEnabled.get() != false || (Boolean)c.gpuShadowsEnabled.get() != false)), ConfigScreenModel.d("realistic_shadows_render_scale", "option.luxium.realistic_shadows_render_scale", "tooltip.luxium.realistic_shadows_render_scale", c.realisticShadowsRenderScale, 0.01, 1.0, 0.01, "x").enabledWhen(() -> (Boolean)c.neoGpuVanillaEnabled.get() == false && ((Boolean)c.neoCpuShadowsEnabled.get() != false || (Boolean)c.realisticShadowsEnabled.get() != false && (Boolean)c.gpuShadowsEnabled.get() == false))));
        ArrayList<Row> celestialRows = new ArrayList<Row>();
        celestialRows.add(Row.header("header.luxium.celestial_light"));
        celestialRows.add(Row.option(ConfigScreenModel.b("sky_light_enabled", "option.luxium.sky_light_enabled", "tooltip.luxium.sky_light_enabled", c.skyLightEnabled)));
        celestialRows.add(Row.option(ConfigScreenModel.b("sky_light_colors_enabled", "option.luxium.sky_light_colors_enabled", "tooltip.luxium.sky_light_colors_enabled", c.skyLightColorsEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_sun_light_strength", "option.luxium.sky_sun_light_strength", "tooltip.luxium.sky_sun_light_strength", c.skyLightSunStrength, 0.0, 200.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_moon_light_strength", "option.luxium.sky_moon_light_strength", "tooltip.luxium.sky_moon_light_strength", c.skyLightMoonStrength, 0.0, 200.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigOption.color("sky_light_sun_zenith_color", "option.luxium.sky_light_sun_zenith_color", "tooltip.luxium.sky_light_sun_zenith_color", c.skyLightSunZenithColor).enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false)));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_light_sun_zenith_strength", "option.luxium.sky_light_sun_zenith_strength", "tooltip.luxium.sky_light_sun_zenith_strength", c.skyLightSunZenithStrength, 0.0, 200.0, 0.1, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigOption.color("sky_light_sunset_color", "option.luxium.sky_light_sunset_color", "tooltip.luxium.sky_light_sunset_color", c.skyLightSunsetColor).enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false)));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_light_sunset_strength", "option.luxium.sky_light_sunset_strength", "tooltip.luxium.sky_light_sunset_strength", c.skyLightSunsetStrength, 0.0, 200.0, 0.1, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigOption.color("sky_light_moon_color", "option.luxium.sky_light_moon_color", "tooltip.luxium.sky_light_moon_color", c.skyLightMoonColor).enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false)));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_light_moon_base_strength", "option.luxium.sky_light_moon_base_strength", "tooltip.luxium.sky_light_moon_base_strength", c.skyLightMoonBaseStrength, 0.0, 200.0, 0.1, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_ambient_light_strength", "option.luxium.sky_ambient_light_strength", "tooltip.luxium.sky_ambient_light_strength", c.skyLightAmbientStrength, 0.0, 200.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.b("realistic_shadow_temperature", "option.luxium.realistic_shadow_temperature", "tooltip.luxium.realistic_shadow_temperature", c.realisticShadowTemperature).enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false)));
        celestialRows.add(Row.option(ConfigScreenModel.d("shadow_temperature_strength", "option.luxium.shadow_temperature_strength", "tooltip.luxium.shadow_temperature_strength", c.shadowTemperatureStrength, 0.0, 1.0, 0.05, "").enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false && (Boolean)c.realisticShadowTemperature.get() != false)));
        celestialRows.add(Row.option(ConfigScreenModel.i("shadow_temperature_bias", "option.luxium.shadow_temperature_bias", "tooltip.luxium.shadow_temperature_bias", c.shadowTemperatureBias, -100, 100, "").enabledWhen(() -> (Boolean)c.skyLightEnabled.get() != false && (Boolean)c.skyLightColorsEnabled.get() != false && (Boolean)c.realisticShadowTemperature.get() != false)));
        celestialRows.add(Row.header("header.luxium.celestial_occlusion"));
        celestialRows.add(Row.option(ConfigScreenModel.d("vanilla_block_light_in_sun_shadows", "option.luxium.vanilla_block_light_in_sun_shadows", "tooltip.luxium.vanilla_block_light_in_sun_shadows", c.vanillaBlockLightInSunShadows, 0.0, 100.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_ray_length", "option.luxium.sky_shadow_ray_length", "tooltip.luxium.sky_shadow_ray_length", c.skyShadowRayLength, 32, 512, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_near_resolution", "option.luxium.sky_shadow_near_resolution", "tooltip.luxium.sky_shadow_near_resolution", c.skyShadowNearResolution, 256, 4096, 256, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_far_resolution", "option.luxium.sky_shadow_far_resolution", "tooltip.luxium.sky_shadow_far_resolution", c.skyShadowFarResolution, 256, 4096, 256, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_near_radius", "option.luxium.sky_shadow_near_radius", "tooltip.luxium.sky_shadow_near_radius", c.skyShadowNearRadius, 16, 128, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.d("sky_shadow_near_update_interval", "option.luxium.sky_shadow_near_update_interval", "tooltip.luxium.sky_shadow_near_update_interval", c.skyShadowNearUpdateIntervalSeconds, 0.01, 5.0, 0.01, " s").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_far_radius", "option.luxium.sky_shadow_far_radius", "tooltip.luxium.sky_shadow_far_radius", c.skyShadowFarRadius, 64, 384, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_far_update_ms", "option.luxium.sky_shadow_far_update_ms", "tooltip.luxium.sky_shadow_far_update_ms", c.skyShadowFarUpdateMs, 50, 3000, " ms").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_shadow_filter_samples", "option.luxium.sky_shadow_filter_samples", "tooltip.luxium.sky_shadow_filter_samples", c.skyShadowFilterSamples, 1, 4, 3, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.b("sky_shadow_soft_shadows_enabled", "option.luxium.sky_shadow_soft_shadows_enabled", "tooltip.luxium.sky_shadow_soft_shadows_enabled", c.skyShadowSoftShadowsEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.b("sky_cloud_shadows_enabled", "option.luxium.sky_cloud_shadows_enabled", "tooltip.luxium.sky_cloud_shadows_enabled", c.skyCloudShadowsEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyLightEnabled).get())));
        celestialRows.add(Row.header("header.luxium.celestial_entity_shadows"));
        celestialRows.add(Row.option(ConfigScreenModel.b("sky_entity_shadows_enabled", "option.luxium.sky_entity_shadows_enabled", "tooltip.luxium.sky_entity_shadows_enabled", c.skyEntityShadowsEnabled)));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_entity_shadow_resolution", "option.luxium.sky_entity_shadow_resolution", "tooltip.luxium.sky_entity_shadow_resolution", c.skyEntityShadowResolution, 512, 2048, 256, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEntityShadowsEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_entity_shadow_radius", "option.luxium.sky_entity_shadow_radius", "tooltip.luxium.sky_entity_shadow_radius", c.skyEntityShadowRadius, 16, 128, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEntityShadowsEnabled).get())));
        celestialRows.add(Row.option(ConfigScreenModel.i("sky_entity_shadow_update_fps", "option.luxium.sky_entity_shadow_update_fps", "tooltip.luxium.sky_entity_shadow_update_fps", c.skyEntityShadowUpdateFps, 10, 61, " FPS").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEntityShadowsEnabled).get())));
        categories.add(new Category("neoskycelestia", (Component)Component.m_237115_((String)"category.luxium.neoskycelestia"), List.copyOf(celestialRows)));
        ConfigOption<Config.SkyBakeTileSize> skyBakeTileSize = ConfigOption.enumeration("sky_bake_tile_size", "option.luxium.sky_bake_tile_size", "tooltip.luxium.sky_bake_tile_size", c.skyBakeTileSize, List.of(Config.SkyBakeTileSize.SIZE_512, Config.SkyBakeTileSize.SIZE_256, Config.SkyBakeTileSize.SIZE_128), value -> Component.m_237113_((String)(value.pixels() + " px")), ignored -> Sky.onConfigChanged());
        ConfigOption[] configOptionArray = new ConfigOption[7];
        configOptionArray[0] = ConfigScreenModel.b("sky_enabled", "option.luxium.sky_enabled", "tooltip.luxium.sky_enabled", c.skyEnabled).onChanged(ignored -> Sky.onConfigChanged());
        configOptionArray[1] = ConfigScreenModel.d("sky_bake_update_interval", "option.luxium.sky_bake_update_interval", "tooltip.luxium.sky_bake_update_interval", c.skyBakeUpdateIntervalSeconds, 0.1, 2.0, 0.1, " s").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEnabled).get()).onChanged(ignored -> Sky.onConfigChanged());
        configOptionArray[2] = ConfigScreenModel.b("sky_bake_smooth_enabled", "option.luxium.sky_bake_smooth_enabled", "tooltip.luxium.sky_bake_smooth_enabled", c.skyBakeSmoothEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEnabled).get()).onChanged(ignored -> Sky.onConfigChanged());
        configOptionArray[3] = ConfigScreenModel.i("sky_bake_work_per_frame", "option.luxium.sky_bake_work_per_frame", "tooltip.luxium.sky_bake_work_per_frame", c.skyBakeWorkPerFrame, 1, 6, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyEnabled).get()).onChanged(ignored -> Sky.onConfigChanged());
        configOptionArray[4] = skyBakeTileSize.enabledWhen(() -> (Boolean)c.skyEnabled.get() != false && (Boolean)c.skyBakeSmoothEnabled.get() != false);
        configOptionArray[5] = ConfigScreenModel.d("sky_sun_size", "option.luxium.sky_sun_size", "tooltip.luxium.sky_sun_size", c.skySunSize, 0.1, 3.0, 0.01, "x");
        configOptionArray[6] = ConfigScreenModel.d("sky_moon_size", "option.luxium.sky_moon_size", "tooltip.luxium.sky_moon_size", c.skyMoonSize, 0.1, 3.0, 0.01, "x");
        categories.add(ConfigScreenModel.category("sky", "category.luxium.sky", configOptionArray));
        ArrayList<Row> cloudRows = new ArrayList<Row>();
        BooleanSupplier customClouds = () -> ((ForgeConfigSpec.BooleanValue)c.cloudsEnabled).get();
        BooleanSupplier cloudLayer1 = () -> (Boolean)c.cloudsEnabled.get() != false && (Boolean)c.cloudLayer1Enabled.get() != false;
        BooleanSupplier cloudLayer2 = () -> (Boolean)c.cloudsEnabled.get() != false && (Boolean)c.cloudLayer2Enabled.get() != false;
        BooleanSupplier cloudLayer3 = () -> (Boolean)c.cloudsEnabled.get() != false && (Boolean)c.cloudLayer3Enabled.get() != false;
        cloudRows.add(Row.header("header.luxium.clouds_general"));
        cloudRows.add(Row.option(ConfigScreenModel.b("clouds_enabled", "option.luxium.clouds_enabled", "tooltip.luxium.clouds_enabled", c.cloudsEnabled)));
        cloudRows.add(Row.header("header.luxium.cloud_layer_1"));
        cloudRows.add(Row.option(ConfigScreenModel.b("cloud_layer_1_enabled", "option.luxium.cloud_layer_enabled", "tooltip.luxium.cloud_layer_enabled", c.cloudLayer1Enabled).enabledWhen(customClouds)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_height", "option.luxium.cloud_height", "tooltip.luxium.cloud_height", c.cloudLayer1Height, 64.0, 384.0, 1.0, " blocks").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_speed", "option.luxium.cloud_speed", "tooltip.luxium.cloud_speed", c.cloudLayer1Speed, 0.0, 3.0, 0.01, "x").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_direction", "option.luxium.cloud_direction", "tooltip.luxium.cloud_direction", c.cloudLayer1Direction, 0.0, 360.0, 1.0, "\u00b0").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_scale", "option.luxium.cloud_scale", "tooltip.luxium.cloud_scale", c.cloudLayer1Scale, 0.25, 4.0, 0.01, "x").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_opacity", "option.luxium.cloud_opacity", "tooltip.luxium.cloud_opacity", c.cloudLayer1Opacity, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_thickness", "option.luxium.cloud_thickness", "tooltip.luxium.cloud_thickness", c.cloudLayer1Thickness, 0.5, 16.0, 0.1, " blocks").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_brightness", "option.luxium.cloud_brightness", "tooltip.luxium.cloud_brightness", c.cloudLayer1Brightness, 0.25, 1.5, 0.01, "x").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_coverage", "option.luxium.cloud_coverage", "tooltip.luxium.cloud_coverage", c.cloudLayer1Coverage, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_edge_softness", "option.luxium.cloud_edge_softness", "tooltip.luxium.cloud_edge_softness", c.cloudLayer1EdgeSoftness, 0.0, 4.0, 0.1, " px").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_variation", "option.luxium.cloud_variation", "tooltip.luxium.cloud_variation", c.cloudLayer1Variation, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_weather", "option.luxium.cloud_weather_influence", "tooltip.luxium.cloud_weather_influence", c.cloudLayer1WeatherInfluence, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_1_storm_darkening", "option.luxium.cloud_storm_darkening", "tooltip.luxium.cloud_storm_darkening", c.cloudLayer1StormDarkening, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.option(ConfigScreenModel.i("cloud_layer_1_render_distance", "option.luxium.cloud_render_distance", "tooltip.luxium.cloud_render_distance", c.cloudLayer1RenderDistance, 128, 1024, 16, " blocks").enabledWhen(cloudLayer1)));
        cloudRows.add(Row.header("header.luxium.cloud_layer_2"));
        cloudRows.add(Row.option(ConfigScreenModel.b("cloud_layer_2_enabled", "option.luxium.cloud_layer_enabled", "tooltip.luxium.cloud_layer_enabled", c.cloudLayer2Enabled).enabledWhen(customClouds)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_height", "option.luxium.cloud_height", "tooltip.luxium.cloud_height", c.cloudLayer2Height, 64.0, 384.0, 1.0, " blocks").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_speed", "option.luxium.cloud_speed", "tooltip.luxium.cloud_speed", c.cloudLayer2Speed, 0.0, 3.0, 0.01, "x").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_direction", "option.luxium.cloud_direction", "tooltip.luxium.cloud_direction", c.cloudLayer2Direction, 0.0, 360.0, 1.0, "\u00b0").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_scale", "option.luxium.cloud_scale", "tooltip.luxium.cloud_scale", c.cloudLayer2Scale, 0.25, 4.0, 0.01, "x").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_opacity", "option.luxium.cloud_opacity", "tooltip.luxium.cloud_opacity", c.cloudLayer2Opacity, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_thickness", "option.luxium.cloud_thickness", "tooltip.luxium.cloud_thickness", c.cloudLayer2Thickness, 0.5, 16.0, 0.1, " blocks").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_brightness", "option.luxium.cloud_brightness", "tooltip.luxium.cloud_brightness", c.cloudLayer2Brightness, 0.25, 1.5, 0.01, "x").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_coverage", "option.luxium.cloud_coverage", "tooltip.luxium.cloud_coverage", c.cloudLayer2Coverage, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_edge_softness", "option.luxium.cloud_edge_softness", "tooltip.luxium.cloud_edge_softness", c.cloudLayer2EdgeSoftness, 0.0, 4.0, 0.1, " px").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_variation", "option.luxium.cloud_variation", "tooltip.luxium.cloud_variation", c.cloudLayer2Variation, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_weather", "option.luxium.cloud_weather_influence", "tooltip.luxium.cloud_weather_influence", c.cloudLayer2WeatherInfluence, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_2_storm_darkening", "option.luxium.cloud_storm_darkening", "tooltip.luxium.cloud_storm_darkening", c.cloudLayer2StormDarkening, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.option(ConfigScreenModel.i("cloud_layer_2_render_distance", "option.luxium.cloud_render_distance", "tooltip.luxium.cloud_render_distance", c.cloudLayer2RenderDistance, 128, 1024, 16, " blocks").enabledWhen(cloudLayer2)));
        cloudRows.add(Row.header("header.luxium.cloud_layer_3"));
        cloudRows.add(Row.option(ConfigScreenModel.b("cloud_layer_3_enabled", "option.luxium.cloud_layer_enabled", "tooltip.luxium.cloud_layer_enabled", c.cloudLayer3Enabled).enabledWhen(customClouds)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_height", "option.luxium.cloud_height", "tooltip.luxium.cloud_height", c.cloudLayer3Height, 64.0, 384.0, 1.0, " blocks").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_speed", "option.luxium.cloud_speed", "tooltip.luxium.cloud_speed", c.cloudLayer3Speed, 0.0, 3.0, 0.01, "x").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_direction", "option.luxium.cloud_direction", "tooltip.luxium.cloud_direction", c.cloudLayer3Direction, 0.0, 360.0, 1.0, "\u00b0").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_scale", "option.luxium.cloud_scale", "tooltip.luxium.cloud_scale", c.cloudLayer3Scale, 0.25, 4.0, 0.01, "x").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_opacity", "option.luxium.cloud_opacity", "tooltip.luxium.cloud_opacity", c.cloudLayer3Opacity, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_thickness", "option.luxium.cloud_thickness", "tooltip.luxium.cloud_thickness", c.cloudLayer3Thickness, 0.5, 16.0, 0.1, " blocks").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_brightness", "option.luxium.cloud_brightness", "tooltip.luxium.cloud_brightness", c.cloudLayer3Brightness, 0.25, 1.5, 0.01, "x").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_coverage", "option.luxium.cloud_coverage", "tooltip.luxium.cloud_coverage", c.cloudLayer3Coverage, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_edge_softness", "option.luxium.cloud_edge_softness", "tooltip.luxium.cloud_edge_softness", c.cloudLayer3EdgeSoftness, 0.0, 4.0, 0.1, " px").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_variation", "option.luxium.cloud_variation", "tooltip.luxium.cloud_variation", c.cloudLayer3Variation, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_weather", "option.luxium.cloud_weather_influence", "tooltip.luxium.cloud_weather_influence", c.cloudLayer3WeatherInfluence, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.d("cloud_layer_3_storm_darkening", "option.luxium.cloud_storm_darkening", "tooltip.luxium.cloud_storm_darkening", c.cloudLayer3StormDarkening, 0.0, 100.0, 1.0, "%").enabledWhen(cloudLayer3)));
        cloudRows.add(Row.option(ConfigScreenModel.i("cloud_layer_3_render_distance", "option.luxium.cloud_render_distance", "tooltip.luxium.cloud_render_distance", c.cloudLayer3RenderDistance, 128, 1024, 16, " blocks").enabledWhen(cloudLayer3)));
        categories.add(new Category("clouds", (Component)Component.m_237115_((String)"category.luxium.clouds"), (Component)Component.m_237115_((String)"description.luxium.clouds"), List.copyOf(cloudRows)));
        BooleanSupplier lsrActive = () -> ((ForgeConfigSpec.BooleanValue)c.lsrEnabled).get();
        categories.add(new Category("lsr", (Component)Component.m_237115_((String)"category.luxium.lsr"), (Component)Component.m_237115_((String)"category.luxium.lsr.description"), List.of(Row.option(ConfigScreenModel.b("lsr_enabled", "option.luxium.lsr_enabled", "tooltip.luxium.lsr_enabled", c.lsrEnabled).onChanged(ignored -> LsrSystem.onConfigChanged())), Row.option(ConfigScreenModel.d("lsr_render_scale", "option.luxium.lsr_render_scale", "tooltip.luxium.lsr_render_scale", c.lsrRenderScale, 0.5, 0.85, 0.01, "x").enabledWhen(lsrActive).onChanged(ignored -> LsrSystem.onConfigChanged())), Row.option(ConfigScreenModel.d("lsr_sharpness", "option.luxium.lsr_sharpness", "tooltip.luxium.lsr_sharpness", c.lsrSharpness, 0.0, 1.0, 0.01, "").enabledWhen(lsrActive).onChanged(ignored -> LsrSystem.onConfigChanged())))));
        BooleanSupplier tfrActive = () -> ((ForgeConfigSpec.BooleanValue)c.tfrEnabled).get();
        categories.add(new Category("tfr", (Component)Component.m_237115_((String)"category.luxium.tfr"), (Component)Component.m_237115_((String)"category.luxium.tfr.description"), List.of(Row.option(ConfigScreenModel.b("tfr_enabled", "option.luxium.tfr_enabled", "tooltip.luxium.tfr_enabled", c.tfrEnabled).onChanged(ignored -> TemporalFrameSystem.onConfigChanged())), Row.option(ConfigScreenModel.d("tfr_reprojection_strength", "option.luxium.tfr_reprojection_strength", "tooltip.luxium.tfr_reprojection_strength", c.tfrReprojectionStrength, 0.0, 1.0, 0.01, "x").enabledWhen(tfrActive)), Row.option(ConfigScreenModel.d("tfr_history_stability", "option.luxium.tfr_history_stability", "tooltip.luxium.tfr_history_stability", c.tfrHistoryStability, 0.0, 0.35, 0.01, "x").enabledWhen(tfrActive)), Row.option(ConfigScreenModel.d("tfr_camera_cut_distance", "option.luxium.tfr_camera_cut_distance", "tooltip.luxium.tfr_camera_cut_distance", c.tfrCameraCutDistance, 0.25, 16.0, 0.25, " blocks").enabledWhen(tfrActive)), Row.option(ConfigScreenModel.d("tfr_camera_cut_angle", "option.luxium.tfr_camera_cut_angle", "tooltip.luxium.tfr_camera_cut_angle", c.tfrCameraCutAngle, 5.0, 90.0, 1.0, "\u00b0").enabledWhen(tfrActive)))));
        ConfigOption[] configOptionArray2 = new ConfigOption[9];
        configOptionArray2[0] = ConfigScreenModel.b("tonemap_enabled", "option.luxium.tonemap_enabled", "tooltip.luxium.tonemap_enabled", c.tonemapEnabled);
        configOptionArray2[1] = ConfigScreenModel.d("tonemap_exposure", "option.luxium.tonemap_exposure", "tooltip.luxium.tonemap_exposure", c.tonemapExposure, -2.0, 2.0, 0.01, " EV").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[2] = ConfigScreenModel.d("tonemap_contrast", "option.luxium.tonemap_contrast", "tooltip.luxium.tonemap_contrast", c.tonemapContrast, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[3] = ConfigScreenModel.d("tonemap_highlight_compression", "option.luxium.tonemap_highlight_compression", "tooltip.luxium.tonemap_highlight_compression", c.tonemapHighlightCompression, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[4] = ConfigScreenModel.d("tonemap_shadow_depth", "option.luxium.tonemap_shadow_depth", "tooltip.luxium.tonemap_shadow_depth", c.tonemapShadowDepth, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[5] = ConfigScreenModel.d("tonemap_saturation", "option.luxium.tonemap_saturation", "tooltip.luxium.tonemap_saturation", c.tonemapSaturation, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[6] = ConfigScreenModel.d("tonemap_vibrance", "option.luxium.tonemap_vibrance", "tooltip.luxium.tonemap_vibrance", c.tonemapVibrance, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[7] = ConfigScreenModel.d("tonemap_gamma", "option.luxium.tonemap_gamma", "tooltip.luxium.tonemap_gamma", c.tonemapGamma, 1.6, 2.8, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        configOptionArray2[8] = ConfigScreenModel.d("tonemap_strength", "option.luxium.tonemap_strength", "tooltip.luxium.tonemap_strength", c.tonemapStrength, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.tonemapEnabled).get());
        categories.add(ConfigScreenModel.category("tonemap", "category.luxium.tonemap", configOptionArray2));
        categories.add(ConfigScreenModel.category("posteffects", "category.luxium.posteffects_quality", ConfigScreenModel.d("posteffects_render_scale", "option.luxium.posteffects_render_scale", "tooltip.luxium.posteffects_render_scale", c.postEffectsRenderScale, 0.1, 1.0, 0.01, "x")));
        ConfigOption[] configOptionArray3 = new ConfigOption[8];
        configOptionArray3[0] = ConfigScreenModel.b("kawase_bloom_enabled", "option.luxium.kawase_bloom_enabled", "tooltip.luxium.kawase_bloom_enabled", c.kawaseBloomEnabled).onChanged(enabled -> {
            if (!enabled.booleanValue()) {
                KawaseBloomRenderer.releaseResources();
            }
        });
        configOptionArray3[1] = ConfigScreenModel.d("kawase_bloom_intensity", "option.luxium.kawase_bloom_intensity", "tooltip.luxium.kawase_bloom_intensity", c.kawaseBloomIntensity, 0.0, 3.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[2] = ConfigScreenModel.d("kawase_bloom_radius", "option.luxium.kawase_bloom_radius", "tooltip.luxium.kawase_bloom_radius", c.kawaseBloomRadius, 0.5, 4.0, 0.05, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[3] = ConfigScreenModel.i("kawase_bloom_levels", "option.luxium.kawase_bloom_levels", "tooltip.luxium.kawase_bloom_levels", c.kawaseBloomLevels, 2, 7, " levels").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[4] = ConfigScreenModel.d("kawase_bloom_threshold", "option.luxium.kawase_bloom_threshold", "tooltip.luxium.kawase_bloom_threshold", c.kawaseBloomThreshold, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[5] = ConfigScreenModel.i("kawase_bloom_source_distance", "option.luxium.kawase_bloom_source_distance", "tooltip.luxium.kawase_bloom_source_distance", c.kawaseBloomSourceDistance, 8, 32, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[6] = ConfigScreenModel.b("kawase_bloom_depth_occlusion", "option.luxium.kawase_bloom_depth_occlusion", "tooltip.luxium.kawase_bloom_depth_occlusion", c.kawaseBloomDepthOcclusion).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.kawaseBloomEnabled).get());
        configOptionArray3[7] = ConfigScreenModel.d("kawase_bloom_depth_tolerance", "option.luxium.kawase_bloom_depth_tolerance", "tooltip.luxium.kawase_bloom_depth_tolerance", c.kawaseBloomDepthTolerance, 0.01, 2.0, 0.01, " blocks").enabledWhen(() -> (Boolean)c.kawaseBloomEnabled.get() != false && (Boolean)c.kawaseBloomDepthOcclusion.get() != false);
        categories.add(ConfigScreenModel.category("kawase_bloom", "category.luxium.kawase_bloom", configOptionArray3));
        ConfigOption<Double> ray = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_ray_intensity", "option.luxium.sky_godrays_ray_intensity", "tooltip.luxium.sky_godrays_ray_intensity", c.skyGodRaysRayIntensity, 0.0, 2.5, 0.01, "x"));
        ConfigOption<Double> haloIntensity = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_halo_intensity", "option.luxium.sky_godrays_halo_intensity", "tooltip.luxium.sky_godrays_halo_intensity", c.skyGodRaysHaloIntensity, 0.0, 2.5, 0.01, "x"));
        ConfigOption<Double> discIntensity = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_disc_intensity", "option.luxium.sky_godrays_disc_intensity", "tooltip.luxium.sky_godrays_disc_intensity", c.skyGodRaysDiscIntensity, 0.0, 2.0, 0.01, "x"));
        ConfigOption<Double> haloSize = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_halo_size", "option.luxium.sky_godrays_halo_size", "tooltip.luxium.sky_godrays_halo_size", c.skyGodRaysHaloSize, 0.5, 2.5, 0.01, "x"));
        ConfigOption<Double> discSize = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_disc_size", "option.luxium.sky_godrays_disc_size", "tooltip.luxium.sky_godrays_disc_size", c.skyGodRaysDiscSize, 0.5, 1.8, 0.01, "x"));
        ConfigOption<Double> weather = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_weather_influence", "option.luxium.sky_godrays_weather_influence", "tooltip.luxium.sky_godrays_weather_influence", c.skyGodRaysWeatherInfluence, 0.0, 2.0, 0.01, "x"));
        ConfigOption<Double> center = ConfigScreenModel.custom(ConfigScreenModel.d("sky_godrays_center_suppression", "option.luxium.sky_godrays_center_suppression", "tooltip.luxium.sky_godrays_center_suppression", c.skyGodRaysCenterSuppression, 0.0, 2.0, 0.01, "x"));
        ConfigOption<Config.SkyGodRaysVisualPreset> preset = ConfigOption.enumeration("sky_godrays_visual_preset", "option.luxium.sky_godrays_visual_preset", "tooltip.luxium.sky_godrays_visual_preset", c.skyGodRaysVisualPreset, List.of(Config.SkyGodRaysVisualPreset.DEFAULT, Config.SkyGodRaysVisualPreset.CINEMATIC, Config.SkyGodRaysVisualPreset.CUSTOM), value -> Component.m_237115_((String)("value.luxium.sky_godrays_visual_preset." + value.name().toLowerCase(Locale.ROOT))), ConfigScreenModel::applyPreset);
        categories.add(ConfigScreenModel.category("godrays", "category.luxium.godrays", ConfigScreenModel.b("sky_godrays_enabled", "option.luxium.sky_godrays_enabled", "tooltip.luxium.sky_godrays_enabled", c.skyGodRaysEnabled), ConfigScreenModel.d("sky_godrays_sun_size", "option.luxium.sky_godrays_sun_size", "tooltip.luxium.sky_godrays_sun_size", c.skyGodRaysSunSize, 0.5, 2.0, 0.01, "x"), ConfigScreenModel.d("sky_godrays_moon_size", "option.luxium.sky_godrays_moon_size", "tooltip.luxium.sky_godrays_moon_size", c.skyGodRaysMoonSize, 0.5, 2.0, 0.01, "x"), ConfigScreenModel.b("sky_godrays_celestial_disc", "option.luxium.sky_godrays_celestial_disc", "tooltip.luxium.sky_godrays_celestial_disc", c.skyGodRaysCelestialDiscEnabled), ConfigScreenModel.b("sky_godrays_halo", "option.luxium.sky_godrays_halo", "tooltip.luxium.sky_godrays_halo", c.skyGodRaysHaloEnabled), preset, ray, haloIntensity, discIntensity, haloSize, discSize, ConfigOption.color("sky_godrays_sun_day_color", "option.luxium.sky_godrays_sun_day_color", "tooltip.luxium.sky_godrays_sun_day_color", c.skyGodRaysSunDayColor), ConfigOption.color("sky_godrays_sunset_color", "option.luxium.sky_godrays_sunset_color", "tooltip.luxium.sky_godrays_sunset_color", c.skyGodRaysSunsetColor), ConfigOption.color("sky_godrays_moon_base_color", "option.luxium.sky_godrays_moon_base_color", "tooltip.luxium.sky_godrays_moon_base_color", c.skyGodRaysMoonBaseColor), ConfigOption.color("sky_godrays_moon_halo_color", "option.luxium.sky_godrays_moon_halo_color", "tooltip.luxium.sky_godrays_moon_halo_color", c.skyGodRaysMoonHaloColor), weather, center));
        ConfigOption[] configOptionArray4 = new ConfigOption[13];
        configOptionArray4[0] = ConfigScreenModel.b("sky_volumetric_god_rays_enabled", "option.luxium.sky_volumetric_god_rays_enabled", "tooltip.luxium.sky_volumetric_god_rays_enabled", c.skyVolumetricGodRaysEnabled);
        configOptionArray4[1] = ConfigScreenModel.d("sky_volumetric_god_rays_intensity", "option.luxium.sky_volumetric_god_rays_intensity", "tooltip.luxium.sky_volumetric_god_rays_intensity", c.skyVolumetricGodRaysIntensity, 0.0, 3.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[2] = ConfigScreenModel.d("sky_volumetric_god_rays_density", "option.luxium.sky_volumetric_god_rays_density", "tooltip.luxium.sky_volumetric_god_rays_density", c.skyVolumetricGodRaysDensity, 0.0, 3.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[3] = ConfigScreenModel.i("sky_volumetric_god_rays_samples", "option.luxium.sky_volumetric_god_rays_samples", "tooltip.luxium.sky_volumetric_god_rays_samples", c.skyVolumetricGodRaysSamples, 8, 32, " samples").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[4] = ConfigScreenModel.d("sky_volumetric_god_rays_uniformity", "option.luxium.sky_volumetric_god_rays_uniformity", "tooltip.luxium.sky_volumetric_god_rays_uniformity", c.skyVolumetricGodRaysUniformity, 0.0, 100.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[5] = ConfigScreenModel.d("sky_volumetric_god_rays_side_visibility", "option.luxium.sky_volumetric_god_rays_side_visibility", "tooltip.luxium.sky_volumetric_god_rays_side_visibility", c.skyVolumetricGodRaysSideVisibility, 0.0, 100.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[6] = ConfigScreenModel.d("sky_volumetric_god_rays_haze_suppression", "option.luxium.sky_volumetric_god_rays_haze_suppression", "tooltip.luxium.sky_volumetric_god_rays_haze_suppression", c.skyVolumetricGodRaysHazeSuppression, 0.0, 100.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[7] = ConfigScreenModel.b("sky_volumetric_god_rays_entity_occlusion", "option.luxium.sky_volumetric_god_rays_entity_occlusion", "tooltip.luxium.sky_volumetric_god_rays_entity_occlusion", c.skyVolumetricGodRaysEntityOcclusion).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[8] = ConfigScreenModel.d("sky_volumetric_god_rays_max_distance", "option.luxium.sky_volumetric_god_rays_max_distance", "tooltip.luxium.sky_volumetric_god_rays_max_distance", c.skyVolumetricGodRaysMaxDistance, 16.0, 384.0, 1.0, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[9] = ConfigScreenModel.d("sky_volumetric_god_rays_anisotropy", "option.luxium.sky_volumetric_god_rays_anisotropy", "tooltip.luxium.sky_volumetric_god_rays_anisotropy", c.skyVolumetricGodRaysAnisotropy, 0.0, 0.95, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[10] = ConfigScreenModel.d("sky_volumetric_god_rays_celestial_color_influence", "option.luxium.sky_volumetric_god_rays_celestial_color_influence", "tooltip.luxium.sky_volumetric_god_rays_celestial_color_influence", c.skyVolumetricGodRaysCelestialColorInfluence, 0.0, 100.0, 1.0, "%").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[11] = ConfigScreenModel.b("sky_volumetric_god_rays_auto_adaptation", "option.luxium.sky_volumetric_god_rays_auto_adaptation", "tooltip.luxium.sky_volumetric_god_rays_auto_adaptation", c.skyVolumetricGodRaysAutoAdaptation).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.skyVolumetricGodRaysEnabled).get());
        configOptionArray4[12] = ConfigScreenModel.i("sky_volumetric_god_rays_full_adaptation_skylight", "option.luxium.sky_volumetric_god_rays_full_adaptation_skylight", "tooltip.luxium.sky_volumetric_god_rays_full_adaptation_skylight", c.skyVolumetricGodRaysFullAdaptationSkylight, 0, 15, "").reversedSlider().enabledWhen(() -> (Boolean)c.skyVolumetricGodRaysEnabled.get() != false && (Boolean)c.skyVolumetricGodRaysAutoAdaptation.get() != false);
        categories.add(ConfigScreenModel.category("volumetric_rays", "category.luxium.volumetric_rays", configOptionArray4));
        ConfigOption[] configOptionArray5 = new ConfigOption[16];
        configOptionArray5[0] = ConfigScreenModel.fogConfig(ConfigScreenModel.b("fog_enabled", "option.luxium.fog_enabled", "tooltip.luxium.fog_enabled", c.fogEnabled));
        configOptionArray5[1] = ConfigScreenModel.fogConfig(ConfigScreenModel.b("fog_celestial_scattering_enabled", "option.luxium.fog_celestial_scattering_enabled", "tooltip.luxium.fog_celestial_scattering_enabled", c.fogCelestialScatteringEnabled));
        configOptionArray5[2] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_density", "option.luxium.fog_density", "tooltip.luxium.fog_density", c.fogDensity, 0.0, 20.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[3] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_scattering_brightness", "option.luxium.fog_scattering_brightness", "tooltip.luxium.fog_scattering_brightness", c.fogScatteringBrightness, 0.25, 1.2, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[4] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_max_brightness", "option.luxium.fog_max_brightness", "tooltip.luxium.fog_max_brightness", c.fogMaxBrightness, 0.15, 1.2, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[5] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_near_density_boost", "option.luxium.fog_near_density_boost", "tooltip.luxium.fog_near_density_boost", c.fogNearDensityBoost, 0.0, 5.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[6] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_near_boost_range", "option.luxium.fog_near_boost_range", "tooltip.luxium.fog_near_boost_range", c.fogNearBoostRange, 4.0, 128.0, 1.0, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[7] = ConfigScreenModel.fogConfig(ConfigScreenModel.b("fog_dynamic_celestial_color", "option.luxium.fog_dynamic_celestial_color", "tooltip.luxium.fog_dynamic_celestial_color", c.fogDynamicCelestialColor).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[8] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_celestial_color_blend", "option.luxium.fog_celestial_color_blend", "tooltip.luxium.fog_celestial_color_blend", c.fogCelestialColorBlend, 0.0, 1.0, 0.01, "").enabledWhen(() -> (Boolean)c.fogEnabled.get() != false && (Boolean)c.fogDynamicCelestialColor.get() != false));
        configOptionArray5[9] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_start_distance", "option.luxium.fog_start_distance", "tooltip.luxium.fog_start_distance", c.fogStartDistance, 0.0, 160.0, 1.0, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[10] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_near_fade", "option.luxium.fog_near_fade", "tooltip.luxium.fog_near_fade", c.fogNearFade, 0.0, 64.0, 0.1, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[11] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_max_opacity", "option.luxium.fog_max_opacity", "tooltip.luxium.fog_max_opacity", c.fogMaxOpacity, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[12] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_sky_tint", "option.luxium.fog_sky_tint", "tooltip.luxium.fog_sky_tint", c.fogSkyTint, 0.0, 1.0, 0.01, "").enabledWhen(() -> (Boolean)c.fogEnabled.get() != false && (Boolean)c.fogDynamicCelestialColor.get() == false));
        configOptionArray5[13] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_scattering_strength", "option.luxium.fog_scattering_strength", "tooltip.luxium.fog_scattering_strength", c.fogScatteringStrength, 0.0, 5.0, 0.01, "x").enabledWhen(() -> (Boolean)c.fogEnabled.get() != false || (Boolean)c.fogCelestialScatteringEnabled.get() != false));
        configOptionArray5[14] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_distance_curve", "option.luxium.fog_distance_curve", "tooltip.luxium.fog_distance_curve", c.fogDistanceCurve, 0.35, 3.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        configOptionArray5[15] = ConfigScreenModel.fogConfig(ConfigScreenModel.d("fog_height", "option.luxium.fog_height", "tooltip.luxium.fog_height", c.fogHeight, 50.0, 384.0, 1.0, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.fogEnabled).get()));
        categories.add(ConfigScreenModel.category("fog", "category.luxium.fog", configOptionArray5));
        categories.add(ConfigScreenModel.category("camera", "category.luxium.camera", ConfigScreenModel.b("lens_flare_enabled", "option.luxium.lens_flare_enabled", "tooltip.luxium.lens_flare_enabled", c.lensFlareEnabled), ConfigScreenModel.d("lens_flare_intensity", "option.luxium.lens_flare_intensity", "tooltip.luxium.lens_flare_intensity", c.lensFlareIntensity, 0.0, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_streak_intensity", "option.luxium.lens_flare_streak_intensity", "tooltip.luxium.lens_flare_streak_intensity", c.lensFlareStreakIntensity, 0.0, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_streak_length", "option.luxium.lens_flare_streak_length", "tooltip.luxium.lens_flare_streak_length", c.lensFlareStreakLength, 0.25, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_streak_width", "option.luxium.lens_flare_streak_width", "tooltip.luxium.lens_flare_streak_width", c.lensFlareStreakWidth, 0.25, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_chromatic_spread", "option.luxium.lens_flare_chromatic_spread", "tooltip.luxium.lens_flare_chromatic_spread", c.lensFlareChromaticSpread, 0.0, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_ghost_intensity", "option.luxium.lens_flare_ghost_intensity", "tooltip.luxium.lens_flare_ghost_intensity", c.lensFlareGhostIntensity, 0.0, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_ghost_size", "option.luxium.lens_flare_ghost_size", "tooltip.luxium.lens_flare_ghost_size", c.lensFlareGhostSize, 0.35, 3.0, 0.01, "x"), ConfigScreenModel.d("lens_flare_spread", "option.luxium.lens_flare_spread", "tooltip.luxium.lens_flare_spread", c.lensFlareSpread, 0.35, 3.0, 0.01, "x")));
        ArrayList<Row> waterRows = new ArrayList<Row>();
        waterRows.add(Row.header("header.luxium.water_surface"));
        waterRows.add(Row.option(ConfigScreenModel.b("water_enabled", "option.luxium.water_enabled", "tooltip.luxium.water_enabled", c.waterEnabled)));
        waterRows.add(Row.option(ConfigScreenModel.b("water_depth_aware_refraction", "option.luxium.water_depth_aware_refraction", "tooltip.luxium.water_depth_aware_refraction", c.waterDepthAwareRefraction).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.b("water_detail_waves", "option.luxium.water_detail_waves", "tooltip.luxium.water_detail_waves", c.waterDetailWaves).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_render_scale", "option.luxium.water_render_scale", "tooltip.luxium.water_render_scale", c.waterRenderScale, 0.25, 1.0, 0.05, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_wave_strength", "option.luxium.water_wave_strength", "tooltip.luxium.water_wave_strength", c.waterWaveStrength, 0.0, 5.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_wave_scale", "option.luxium.water_wave_scale", "tooltip.luxium.water_wave_scale", c.waterWaveScale, 0.25, 10.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_wave_speed", "option.luxium.water_wave_speed", "tooltip.luxium.water_wave_speed", c.waterWaveSpeed, 0.0, 6.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_detail_strength", "option.luxium.water_detail_strength", "tooltip.luxium.water_detail_strength", c.waterDetailStrength, 0.0, 1.0, 0.01, "x").enabledWhen(() -> (Boolean)c.waterEnabled.get() != false && (Boolean)c.waterDetailWaves.get() != false)));
        waterRows.add(Row.option(ConfigScreenModel.d("water_refraction_strength", "option.luxium.water_refraction_strength", "tooltip.luxium.water_refraction_strength", c.waterRefractionStrength, 0.0, 4.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_absorption_strength", "option.luxium.water_absorption_strength", "tooltip.luxium.water_absorption_strength", c.waterAbsorptionStrength, 0.0, 3.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_visibility_depth", "option.luxium.water_visibility_depth", "tooltip.luxium.water_visibility_depth", c.waterVisibilityDepth, 2.0, 48.0, 0.5, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_fresnel_strength", "option.luxium.water_fresnel_strength", "tooltip.luxium.water_fresnel_strength", c.waterFresnelStrength, 0.0, 1.5, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_fresnel_power", "option.luxium.water_fresnel_power", "tooltip.luxium.water_fresnel_power", c.waterFresnelPower, 1.0, 10.0, 0.1, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_surface_opacity", "option.luxium.water_surface_opacity", "tooltip.luxium.water_surface_opacity", c.waterSurfaceOpacity, 0.0, 0.6, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_specular_strength", "option.luxium.water_specular_strength", "tooltip.luxium.water_specular_strength", c.waterSpecularStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_specular_sharpness", "option.luxium.water_specular_sharpness", "tooltip.luxium.water_specular_sharpness", c.waterSpecularSharpness, 16.0, 512.0, 1.0, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.header("header.luxium.water_ssr"));
        waterRows.add(Row.option(ConfigScreenModel.b("water_ssr_enabled", "option.luxium.water_ssr_enabled", "tooltip.luxium.water_ssr_enabled", c.waterSsrEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.waterEnabled).get())));
        waterRows.add(Row.option(ConfigScreenModel.d("water_ssr_strength", "option.luxium.water_ssr_strength", "tooltip.luxium.water_ssr_strength", c.waterSsrStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> (Boolean)c.waterEnabled.get() != false && (Boolean)c.waterSsrEnabled.get() != false)));
        waterRows.add(Row.option(ConfigScreenModel.d("water_ssr_max_distance", "option.luxium.water_ssr_max_distance", "tooltip.luxium.water_ssr_max_distance", c.waterSsrMaxDistance, 4.0, 128.0, 1.0, " blocks").enabledWhen(() -> (Boolean)c.waterEnabled.get() != false && (Boolean)c.waterSsrEnabled.get() != false)));
        waterRows.add(Row.option(ConfigScreenModel.d("water_ssr_thickness", "option.luxium.water_ssr_thickness", "tooltip.luxium.water_ssr_thickness", c.waterSsrThickness, 0.02, 2.0, 0.01, " blocks").enabledWhen(() -> (Boolean)c.waterEnabled.get() != false && (Boolean)c.waterSsrEnabled.get() != false)));
        waterRows.add(Row.option(ConfigScreenModel.d("water_ssr_edge_fade", "option.luxium.water_ssr_edge_fade", "tooltip.luxium.water_ssr_edge_fade", c.waterSsrEdgeFade, 0.01, 0.4, 0.01, "").enabledWhen(() -> (Boolean)c.waterEnabled.get() != false && (Boolean)c.waterSsrEnabled.get() != false)));
        categories.add(new Category("water", (Component)Component.m_237115_((String)"category.luxium.water"), (Component)Component.m_237115_((String)"category.luxium.water.description"), List.copyOf(waterRows)));
        categories.add(ConfigScreenModel.category("ssr", "category.luxium.ssr", ConfigScreenModel.d("ssr_quality", "option.luxium.ssr_quality", "tooltip.luxium.ssr_quality", c.ssrQuality, 0.25, 1.0, 0.05, "x")));
        ArrayList<Row> puddleRows = new ArrayList<Row>();
        puddleRows.add(Row.header("header.luxium.rain_puddles_surface"));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_max_depth", "option.luxium.puddle_max_depth", "tooltip.luxium.puddle_max_depth", c.puddleMaxDepth, 0.005, 0.2, 0.005, " blocks")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_depth_curve", "option.luxium.puddle_depth_curve", "tooltip.luxium.puddle_depth_curve", c.puddleDepthCurve, 0.35, 3.0, 0.05, "")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_wet_darkening", "option.luxium.puddle_wet_darkening", "tooltip.luxium.puddle_wet_darkening", c.puddleWetDarkening, 0.0, 0.6, 0.01, "")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_surface_opacity", "option.luxium.puddle_surface_opacity", "tooltip.luxium.puddle_surface_opacity", c.puddleSurfaceOpacity, 0.1, 1.0, 0.01, "")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_edge_softness", "option.luxium.puddle_edge_softness", "tooltip.luxium.puddle_edge_softness", c.puddleEdgeSoftness, 0.0, 1.0, 0.01, "")));
        puddleRows.add(Row.header("header.luxium.rain_puddles_motion"));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_wave_strength", "option.luxium.puddle_wave_strength", "tooltip.luxium.puddle_wave_strength", c.puddleWaveStrength, 0.0, 2.0, 0.01, "x")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_wave_scale", "option.luxium.puddle_wave_scale", "tooltip.luxium.puddle_wave_scale", c.puddleWaveScale, 0.25, 10.0, 0.05, "x")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_wave_speed", "option.luxium.puddle_wave_speed", "tooltip.luxium.puddle_wave_speed", c.puddleWaveSpeed, 0.0, 4.0, 0.01, "x")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_ripple_strength", "option.luxium.puddle_ripple_strength", "tooltip.luxium.puddle_ripple_strength", c.puddleRippleStrength, 0.0, 2.0, 0.01, "x")));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_refraction_strength", "option.luxium.puddle_refraction_strength", "tooltip.luxium.puddle_refraction_strength", c.puddleRefractionStrength, 0.0, 3.0, 0.01, "x")));
        puddleRows.add(Row.header("header.luxium.rain_puddles_ssr"));
        puddleRows.add(Row.option(ConfigScreenModel.b("puddle_ssr_enabled", "option.luxium.puddle_ssr_enabled", "tooltip.luxium.puddle_ssr_enabled", c.puddleSsrEnabled)));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_ssr_strength", "option.luxium.puddle_ssr_strength", "tooltip.luxium.puddle_ssr_strength", c.puddleSsrStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.puddleSsrEnabled).get())));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_ssr_max_distance", "option.luxium.puddle_ssr_max_distance", "tooltip.luxium.puddle_ssr_max_distance", c.puddleSsrMaxDistance, 4.0, 96.0, 1.0, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.puddleSsrEnabled).get())));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_ssr_thickness", "option.luxium.puddle_ssr_thickness", "tooltip.luxium.puddle_ssr_thickness", c.puddleSsrThickness, 0.02, 2.0, 0.01, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.puddleSsrEnabled).get())));
        puddleRows.add(Row.option(ConfigScreenModel.d("puddle_ssr_edge_fade", "option.luxium.puddle_ssr_edge_fade", "tooltip.luxium.puddle_ssr_edge_fade", c.puddleSsrEdgeFade, 0.01, 0.4, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.puddleSsrEnabled).get())));
        categories.add(new Category("rain_puddles", (Component)Component.m_237115_((String)"category.luxium.rain_puddles"), (Component)Component.m_237115_((String)"description.luxium.rain_puddles"), List.copyOf(puddleRows)));
        ArrayList<Row> wetRows = new ArrayList<Row>();
        wetRows.add(Row.header("header.luxium.wet_surface"));
        wetRows.add(Row.option(ConfigScreenModel.b("wet_enabled", "option.luxium.wet_enabled", "tooltip.luxium.wet_enabled", c.wetEnabled)));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_max_depth", "option.luxium.wet_max_depth", "tooltip.luxium.wet_max_depth", c.wetMaxDepth, 0.005, 0.2, 0.005, " blocks").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_depth_curve", "option.luxium.wet_depth_curve", "tooltip.luxium.wet_depth_curve", c.wetDepthCurve, 0.35, 3.0, 0.05, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_darkening", "option.luxium.wet_darkening", "tooltip.luxium.wet_darkening", c.wetDarkening, 0.0, 0.6, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_surface_opacity", "option.luxium.wet_surface_opacity", "tooltip.luxium.wet_surface_opacity", c.wetSurfaceOpacity, 0.1, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_edge_softness", "option.luxium.wet_edge_softness", "tooltip.luxium.wet_edge_softness", c.wetEdgeSoftness, 0.0, 1.0, 0.01, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.header("header.luxium.wet_motion"));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_wave_strength", "option.luxium.wet_wave_strength", "tooltip.luxium.wet_wave_strength", c.wetWaveStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_wave_scale", "option.luxium.wet_wave_scale", "tooltip.luxium.wet_wave_scale", c.wetWaveScale, 0.25, 10.0, 0.05, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_wave_speed", "option.luxium.wet_wave_speed", "tooltip.luxium.wet_wave_speed", c.wetWaveSpeed, 0.0, 4.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ripple_strength", "option.luxium.wet_ripple_strength", "tooltip.luxium.wet_ripple_strength", c.wetRippleStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_refraction_strength", "option.luxium.wet_refraction_strength", "tooltip.luxium.wet_refraction_strength", c.wetRefractionStrength, 0.0, 3.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.header("header.luxium.wet_lighting"));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_environment_reflection_strength", "option.luxium.wet_environment_reflection_strength", "tooltip.luxium.wet_environment_reflection_strength", c.wetEnvironmentReflectionStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_celestial_specular_strength", "option.luxium.wet_celestial_specular_strength", "tooltip.luxium.wet_celestial_specular_strength", c.wetCelestialSpecularStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_sheen_floor", "option.luxium.wet_sheen_floor", "tooltip.luxium.wet_sheen_floor", c.wetSheenFloor, 0.0, 0.25, 0.005, "").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ripple_highlight_strength", "option.luxium.wet_ripple_highlight_strength", "tooltip.luxium.wet_ripple_highlight_strength", c.wetRippleHighlightStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.header("header.luxium.wet_ssr"));
        wetRows.add(Row.option(ConfigScreenModel.b("wet_ssr_enabled", "option.luxium.wet_ssr_enabled", "tooltip.luxium.wet_ssr_enabled", c.wetSsrEnabled).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.wetEnabled).get())));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ssr_strength", "option.luxium.wet_ssr_strength", "tooltip.luxium.wet_ssr_strength", c.wetSsrStrength, 0.0, 2.0, 0.01, "x").enabledWhen(() -> (Boolean)c.wetEnabled.get() != false && (Boolean)c.wetSsrEnabled.get() != false)));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ssr_max_distance", "option.luxium.wet_ssr_max_distance", "tooltip.luxium.wet_ssr_max_distance", c.wetSsrMaxDistance, 4.0, 96.0, 1.0, " blocks").enabledWhen(() -> (Boolean)c.wetEnabled.get() != false && (Boolean)c.wetSsrEnabled.get() != false)));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ssr_thickness", "option.luxium.wet_ssr_thickness", "tooltip.luxium.wet_ssr_thickness", c.wetSsrThickness, 0.02, 2.0, 0.01, " blocks").enabledWhen(() -> (Boolean)c.wetEnabled.get() != false && (Boolean)c.wetSsrEnabled.get() != false)));
        wetRows.add(Row.option(ConfigScreenModel.d("wet_ssr_edge_fade", "option.luxium.wet_ssr_edge_fade", "tooltip.luxium.wet_ssr_edge_fade", c.wetSsrEdgeFade, 0.01, 0.4, 0.01, "").enabledWhen(() -> (Boolean)c.wetEnabled.get() != false && (Boolean)c.wetSsrEnabled.get() != false)));
        categories.add(new Category("wet", (Component)Component.m_237115_((String)"category.luxium.wet"), (Component)Component.m_237115_((String)"description.luxium.wet"), List.copyOf(wetRows)));
        BooleanSupplier plantsWaveActive = () -> ((ForgeConfigSpec.BooleanValue)c.plantsWaveEnabled).get();
        categories.add(new Category("grass", (Component)Component.m_237115_((String)"category.luxium.grass"), (Component)Component.m_237115_((String)"description.luxium.grass"), List.of(Row.option(ConfigScreenModel.b("plants_wave_enabled", "option.luxium.plants_wave_enabled", "tooltip.luxium.plants_wave_enabled", c.plantsWaveEnabled)), Row.option(ConfigScreenModel.d("plants_wave_strength", "option.luxium.plants_wave_strength", "tooltip.luxium.plants_wave_strength", c.plantsWaveStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_speed", "option.luxium.plants_wave_speed", "tooltip.luxium.plants_wave_speed", c.plantsWaveSpeed, 0.0, 3.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_gust_strength", "option.luxium.plants_wave_gust_strength", "tooltip.luxium.plants_wave_gust_strength", c.plantsWaveGustStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.i("plants_wave_distance", "option.luxium.plants_wave_distance", "tooltip.luxium.plants_wave_distance", c.plantsWaveDistance, 16, 192, 4, " blocks").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_grass_strength", "option.luxium.plants_wave_grass_strength", "tooltip.luxium.plants_wave_grass_strength", c.plantsWaveGrassStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_leaves_strength", "option.luxium.plants_wave_leaves_strength", "tooltip.luxium.plants_wave_leaves_strength", c.plantsWaveLeavesStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_aquatic_strength", "option.luxium.plants_wave_aquatic_strength", "tooltip.luxium.plants_wave_aquatic_strength", c.plantsWaveAquaticStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)), Row.option(ConfigScreenModel.d("plants_wave_bend_strength", "option.luxium.plants_wave_bend_strength", "tooltip.luxium.plants_wave_bend_strength", c.plantsWaveBendStrength, 0.0, 2.0, 0.01, "x").enabledWhen(plantsWaveActive)))));
        categories.add(new Category("opaque_ice", (Component)Component.m_237115_((String)"category.luxium.opaque_ice"), (Component)Component.m_237115_((String)"description.luxium.opaque_ice"), List.of(Row.option(ConfigScreenModel.b("opaque_ice_enabled", "option.luxium.opaque_ice_enabled", "tooltip.luxium.opaque_ice_enabled", c.opaqueIceEnabled)))));
        ConfigOption[] configOptionArray6 = new ConfigOption[9];
        configOptionArray6[0] = ConfigScreenModel.b("debug_hud_enabled", "option.luxium.debug_hud_enabled", "tooltip.luxium.debug_hud_enabled", c.debugHudEnabled);
        configOptionArray6[1] = ConfigScreenModel.b("debug_hud_fps", "option.luxium.debug_hud_fps", "tooltip.luxium.debug_hud_fps", c.debugHudFps).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[2] = ConfigScreenModel.b("debug_hud_memory", "option.luxium.debug_hud_memory", "tooltip.luxium.debug_hud_memory", c.debugHudMemory).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[3] = ConfigScreenModel.b("debug_hud_allocated", "option.luxium.debug_hud_allocated", "tooltip.luxium.debug_hud_allocated", c.debugHudAllocated).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[4] = ConfigOption.enumeration("debug_hud_position", "option.luxium.debug_hud_position", "tooltip.luxium.debug_hud_position", c.debugHudPosition, List.of(Config.DebugHudPosition.values()), position -> Component.m_237115_((String)("value.luxium.debug_hud_position." + position.name().toLowerCase(Locale.ROOT))), ignored -> {}).enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[5] = ConfigScreenModel.i("debug_hud_offset_x", "option.luxium.debug_hud_offset_x", "tooltip.luxium.debug_hud_offset_x", c.debugHudOffsetX, 0, 200, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[6] = ConfigScreenModel.i("debug_hud_offset_y", "option.luxium.debug_hud_offset_y", "tooltip.luxium.debug_hud_offset_y", c.debugHudOffsetY, 0, 200, " px").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[7] = ConfigScreenModel.d("debug_hud_text_scale", "option.luxium.debug_hud_text_scale", "tooltip.luxium.debug_hud_text_scale", c.debugHudTextScale, 0.1, 5.0, 0.1, "x").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        configOptionArray6[8] = ConfigScreenModel.d("debug_hud_update_seconds", "option.luxium.debug_hud_update_seconds", "tooltip.luxium.debug_hud_update_seconds", c.debugHudUpdateSeconds, 0.1, 2.0, 0.1, " s").enabledWhen(() -> ((ForgeConfigSpec.BooleanValue)c.debugHudEnabled).get());
        categories.add(ConfigScreenModel.category("debug", "category.luxium.debug", configOptionArray6));
        return List.copyOf(categories);
    }

    public static List<Folder> createFolders() {
        ArrayList ready = new ArrayList();
        ArrayList<Category> development = new ArrayList<Category>();
        Set<String> developmentIds = Set.of("general", "reflection", "vanilla_gpu", "realistic_shadows", "sky", "clouds", "ssr", "rain_puddles", "wet", "grass", "kawase_bloom", "testing", "block_light_test");
        for (Category category : ConfigScreenModel.create()) {
            (developmentIds.contains(category.id()) ? development : ready).add(category);
        }
        return List.of(new Folder("main", (Component)Component.m_237119_(), (Component)Component.m_237119_(), List.copyOf(ready)), new Folder("not_ready_to_play_dev", (Component)Component.m_237115_((String)"folder.luxium.not_ready_to_play_dev.title"), (Component)Component.m_237115_((String)"folder.luxium.not_ready_to_play_dev.description"), List.copyOf(development)));
    }

    public static void reset(Category category) {
        for (Row row : category.rows()) {
            if (row.isHeader()) continue;
            row.option().reset();
        }
    }

    public static void resetAll(List<Category> categories) {
        for (Category category : categories) {
            ConfigScreenModel.reset(category);
        }
    }

    private static Category category(String id, String titleKey, ConfigOption<?> ... options) {
        return new Category(id, (Component)Component.m_237115_((String)titleKey), Arrays.stream(options).map(Row::option).toList());
    }

    private static ConfigOption<Boolean> b(String id, String label, String tooltip, ForgeConfigSpec.BooleanValue value) {
        return ConfigOption.bool(id, label, tooltip, value);
    }

    private static ConfigOption<Integer> i(String id, String label, String tooltip, ForgeConfigSpec.IntValue value, int min, int max, String suffix) {
        return ConfigOption.integer(id, label, tooltip, value, min, max, 1, suffix);
    }

    private static ConfigOption<Integer> i(String id, String label, String tooltip, ForgeConfigSpec.IntValue value, int min, int max, int step, String suffix) {
        return ConfigOption.integer(id, label, tooltip, value, min, max, step, suffix);
    }

    private static ConfigOption<Double> d(String id, String label, String tooltip, ForgeConfigSpec.DoubleValue value, double min, double max, double step, String suffix) {
        return ConfigOption.decimal(id, label, tooltip, value, min, max, step, suffix);
    }

    private static <T> ConfigOption<T> fogConfig(ConfigOption<T> option) {
        return option.onChanged(ignored -> fog.onConfigChanged());
    }

    private static ConfigOption<Double> custom(ConfigOption<Double> option) {
        return option.onChanged(ignored -> {
            Config.CLIENT.skyGodRaysVisualPreset.set((Object)Config.SkyGodRaysVisualPreset.CUSTOM);
            Config.CLIENT.skyGodRaysVisualPreset.save();
        });
    }

    private static void applyPreset(Config.SkyGodRaysVisualPreset preset) {
        double[] dArray;
        if (preset == Config.SkyGodRaysVisualPreset.CUSTOM) {
            return;
        }
        if (preset == Config.SkyGodRaysVisualPreset.CINEMATIC) {
            double[] dArray2 = new double[7];
            dArray2[0] = 1.65;
            dArray2[1] = 1.85;
            dArray2[2] = 1.15;
            dArray2[3] = 1.45;
            dArray2[4] = 1.1;
            dArray2[5] = 1.35;
            dArray = dArray2;
            dArray2[6] = 1.45;
        } else {
            double[] dArray3 = new double[7];
            dArray3[0] = 1.0;
            dArray3[1] = 1.0;
            dArray3[2] = 1.0;
            dArray3[3] = 1.0;
            dArray3[4] = 1.0;
            dArray3[5] = 1.0;
            dArray = dArray3;
            dArray3[6] = 1.0;
        }
        double[] values = dArray;
        Config.Client c = Config.CLIENT;
        c.skyGodRaysRayIntensity.set((Object)values[0]);
        c.skyGodRaysHaloIntensity.set((Object)values[1]);
        c.skyGodRaysDiscIntensity.set((Object)values[2]);
        c.skyGodRaysHaloSize.set((Object)values[3]);
        c.skyGodRaysDiscSize.set((Object)values[4]);
        c.skyGodRaysWeatherInfluence.set((Object)values[5]);
        c.skyGodRaysCenterSuppression.set((Object)values[6]);
        c.skyGodRaysRayIntensity.save();
        c.skyGodRaysHaloIntensity.save();
        c.skyGodRaysDiscIntensity.save();
        c.skyGodRaysHaloSize.save();
        c.skyGodRaysDiscSize.save();
        c.skyGodRaysWeatherInfluence.save();
        c.skyGodRaysCenterSuppression.save();
    }

    public record Row(Component header, ConfigOption<?> option) {
        public static Row option(ConfigOption<?> option) {
            return new Row(null, option);
        }

        public static Row header(String key) {
            return new Row((Component)Component.m_237115_((String)key), null);
        }

        public boolean isHeader() {
            return this.header != null;
        }
    }

    public record Category(String id, Component title, Component description, List<Row> rows) {
        public Category(String id, Component title, List<Row> rows) {
            this(id, title, (Component)Component.m_237119_(), rows);
        }
    }

    public record Folder(String id, Component title, Component description, List<Category> categories) {
    }
}

