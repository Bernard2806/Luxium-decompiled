/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.server.packs.resources.ResourceProvider
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.Testing.LuxiumMasterKeyMapping;
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
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class LuxiumMasterKeyHandler {
    private LuxiumMasterKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (LuxiumMasterKeyMapping.TOGGLE.m_90859_()) {
            Config.CLIENT.luxiumEnabled.set((Object)(!Config.isEnabled() ? 1 : 0));
            Config.CLIENT.luxiumEnabled.save();
            LuxiumMasterKeyHandler.refresh();
        }
    }

    private static void refresh() {
        Minecraft minecraft = Minecraft.m_91087_();
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
        if (minecraft.f_91073_ != null) {
            minecraft.f_91060_.m_109818_();
        }
        ((GameRendererAccessor)minecraft.f_91063_).luxium$reloadShaders((ResourceProvider)minecraft.m_91098_());
    }
}

