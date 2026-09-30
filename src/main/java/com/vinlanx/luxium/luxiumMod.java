/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.ModLoadingContext
 *  net.minecraftforge.fml.common.Mod
 */
package com.vinlanx.luxium;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ConfigScreen.LuxiumConfigScreen;
import com.vinlanx.luxium.client.f3.LuxiumF3Info;
import com.vinlanx.luxium.client.guiscreen.AlphaWarningScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(value="luxium")
public final class luxiumMod {
    public static final String MOD_ID = "luxium";
    private static boolean warningScheduled = false;

    public luxiumMod() {
        Config.register();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new LuxiumConfigScreen((Screen)parent)));
        MinecraftForge.EVENT_BUS.register(luxiumMod.class);
        MinecraftForge.EVENT_BUS.register(LuxiumF3Info.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (warningScheduled) {
            return;
        }
        if (!AlphaWarningScreen.shouldShow()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (!(mc.f_91080_ instanceof TitleScreen)) {
            return;
        }
        warningScheduled = true;
        mc.execute(() -> mc.m_91152_((Screen)new AlphaWarningScreen()));
    }
}

