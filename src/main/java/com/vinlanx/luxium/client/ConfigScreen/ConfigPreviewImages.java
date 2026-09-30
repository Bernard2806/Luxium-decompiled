/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package com.vinlanx.luxium.client.ConfigScreen;

import com.vinlanx.luxium.client.ConfigScreen.ConfigOption;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class ConfigPreviewImages {
    private static final String NAMESPACE = "luxium";
    private static final String DIRECTORY = "configpic/";
    private static final Map<String, ImagePair> IMAGES = Map.ofEntries(ConfigPreviewImages.entry("lens_flare_enabled", "cameralensoff.jpg", "cameralenson.jpg"), ConfigPreviewImages.entry("entity_shadows_enabled", "enitirealisticshadowsoff.jpg", "enitirealisticshadowson.jpg"), ConfigPreviewImages.entry("sky_volumetric_god_rays_entity_occlusion", "entityperekrivaeoff.jpg", "entityperekrivaeon.jpg"), ConfigPreviewImages.entry("sky_entity_shadows_enabled", "entitytinioff.jpg", "entitytinion.jpg"), ConfigPreviewImages.entry("fog_enabled", "fogoff.jpg", "fogon.jpg"), ConfigPreviewImages.entry("sky_cloud_shadows_enabled", "hmaritinioff.jpg", "hmaritinion.jpg"), ConfigPreviewImages.entry("gpu_shadows_enabled", "realisticshadowsoff.jpg", "realisticshadowson.jpg"), ConfigPreviewImages.entry("sky_godrays_enabled", "skygodraysoff.jpg", "skygodrayson.jpg"), ConfigPreviewImages.entry("sky_light_colors_enabled", "svitloskyoff.jpg", "svitloskyon.jpg"), ConfigPreviewImages.entry("sky_light_enabled", "tiniskyoff.jpg", "tiniskyon.jpg"), ConfigPreviewImages.entry("tonemap_enabled", "tonemapoff.jpg", "tonemapon.jpg"), ConfigPreviewImages.entry("sky_volumetric_god_rays_enabled", "volumetricskyoff.jpg", "volumetricskyon.jpg"), ConfigPreviewImages.entry("water_enabled", "wateroff.jpg", "wateron.jpg"), ConfigPreviewImages.entry("water_ssr_enabled", "waterssroff.jpg", "waterssron.jpg"));

    private ConfigPreviewImages() {
    }

    static boolean hasPreview(ConfigOption<?> option) {
        return IMAGES.containsKey(option.id());
    }

    static ResourceLocation image(ConfigOption<?> option, boolean after) {
        ImagePair images = IMAGES.get(option.id());
        if (images == null) {
            throw new IllegalArgumentException("No preview images registered for option: " + option.id());
        }
        return after ? images.on() : images.off();
    }

    private static Map.Entry<String, ImagePair> entry(String optionId, String offFile, String onFile) {
        return Map.entry(optionId, new ImagePair(ConfigPreviewImages.resource(offFile), ConfigPreviewImages.resource(onFile)));
    }

    private static ResourceLocation resource(String fileName) {
        return ResourceLocation.fromNamespaceAndPath((String)NAMESPACE, (String)(DIRECTORY + fileName));
    }

    private record ImagePair(ResourceLocation off, ResourceLocation on) {
    }
}

