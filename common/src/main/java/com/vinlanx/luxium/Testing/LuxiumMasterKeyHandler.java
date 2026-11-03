package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.Sky;
import com.vinlanx.luxium.client.posteffects.fog;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.mixin.GameRendererAccessor;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import net.minecraft.client.Minecraft;

public final class LuxiumMasterKeyHandler {
    private LuxiumMasterKeyHandler()
    {
    }

    public static void onClientTick() {
        while
        (LuxiumMasterKeyMapping.TOGGLE.consumeClick()) {
            Config.CLIENT.luxiumEnabled.set(Boolean.valueOf(!Config.isEnabled()));
            Config.CLIENT.luxiumEnabled.save();
            refresh();
        }
    }

    private static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LuxiumGpuShaderFeatures.refreshNow();
        fog.onConfigChanged();
        NeoGpuVanilla.onConfigChanged();
        GpuNeoShadows.onConfigChanged();
        if (!Config.isEnabled()) {
            GpuNeoShadows.releaseResources();
        }
        Sky.onConfigChanged();
        LsrSystem.onConfigChanged();
        ReflectionSystem.get().setEnabled(Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled));
        NeoSkyCelestia.get().markAllDirty();

        if (minecraft.level != null) {
            minecraft.levelRenderer.allChanged();
        }
        ((GameRendererAccessor)minecraft.gameRenderer).luxium$reloadShaders
(minecraft.getResourceManager());
    }
    }
