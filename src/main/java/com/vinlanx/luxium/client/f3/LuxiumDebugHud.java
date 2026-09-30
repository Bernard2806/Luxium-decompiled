/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterGuiOverlaysEvent
 *  net.minecraftforge.client.gui.overlay.ForgeGui
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.client.f3;

import com.vinlanx.luxium.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public final class LuxiumDebugHud {
    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 10;
    private static final String[] lines = new String[3];
    private static long lastUpdate;
    private static int lastMetrics;
    private static int lineCount;
    private static int textWidth;

    private LuxiumDebugHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("debug_hud", LuxiumDebugHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Config.Client config = Config.CLIENT;
        Minecraft minecraft = gui.getMinecraft();
        if (!((Boolean)config.debugHudEnabled.get()).booleanValue() || minecraft.f_91066_.f_92062_) {
            lastUpdate = 0L;
            return;
        }
        long now = System.nanoTime();
        int metrics = ((Boolean)config.debugHudFps.get() != false ? 1 : 0) | ((Boolean)config.debugHudMemory.get() != false ? 2 : 0) | ((Boolean)config.debugHudAllocated.get() != false ? 4 : 0);
        if (lastUpdate == 0L || metrics != lastMetrics || now - lastUpdate >= (long)((Double)config.debugHudUpdateSeconds.get() * 1.0E9)) {
            lastUpdate = now;
            lastMetrics = metrics;
            lineCount = 0;
            textWidth = 0;
            if (((Boolean)config.debugHudFps.get()).booleanValue()) {
                LuxiumDebugHud.addLine(minecraft, "FPS: " + minecraft.m_260875_());
            }
            if (((Boolean)config.debugHudMemory.get()).booleanValue() || ((Boolean)config.debugHudAllocated.get()).booleanValue()) {
                Runtime runtime = Runtime.getRuntime();
                long total = runtime.totalMemory();
                long max = runtime.maxMemory();
                if (((Boolean)config.debugHudMemory.get()).booleanValue()) {
                    LuxiumDebugHud.addLine(minecraft, "RAM: " + (total - runtime.freeMemory()) / 0x100000L + "/" + max / 0x100000L + " MB");
                }
                if (((Boolean)config.debugHudAllocated.get()).booleanValue()) {
                    LuxiumDebugHud.addLine(minecraft, "Allocated: " + total / 0x100000L + "/" + max / 0x100000L + " MB");
                }
            }
        }
        if (lineCount == 0) {
            return;
        }
        float scale = ((Double)config.debugHudTextScale.get()).floatValue();
        Config.DebugHudPosition position = (Config.DebugHudPosition)((Object)config.debugHudPosition.get());
        boolean right = position == Config.DebugHudPosition.TOP_RIGHT || position == Config.DebugHudPosition.BOTTOM_RIGHT;
        boolean bottom = position == Config.DebugHudPosition.BOTTOM_LEFT || position == Config.DebugHudPosition.BOTTOM_RIGHT;
        float width = (float)textWidth * scale;
        float height = (float)(lineCount * 10) * scale;
        float x = right ? (float)(screenWidth - 4 - (Integer)config.debugHudOffsetX.get()) - width : (float)(4 + (Integer)config.debugHudOffsetX.get());
        float y = bottom ? (float)(screenHeight - 4 - (Integer)config.debugHudOffsetY.get()) - height : (float)(4 + (Integer)config.debugHudOffsetY.get());
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(x, y, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        for (int index = 0; index < lineCount; ++index) {
            graphics.m_280056_(minecraft.f_91062_, lines[index], 0, index * 10, -1, true);
        }
        graphics.m_280168_().m_85849_();
    }

    private static void addLine(Minecraft minecraft, String text) {
        LuxiumDebugHud.lines[LuxiumDebugHud.lineCount++] = text;
        textWidth = Math.max(textWidth, minecraft.f_91062_.m_92895_(text));
    }

    static {
        lastMetrics = -1;
    }
}

