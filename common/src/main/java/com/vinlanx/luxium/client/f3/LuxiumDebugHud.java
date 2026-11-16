package com.vinlanx.luxium.client.f3;

import com.vinlanx.luxium.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class LuxiumDebugHud {
    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 10;
    private static final String[]
 lines = new String[3];
    private static long lastUpdate;
    private static int lastMetrics = -1;
    private static int lineCount;
    private static int textWidth;

    private LuxiumDebugHud() { }

    public static void render(Minecraft minecraft, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Config.Client
        config = Config.CLIENT;
        if (!(config.debugHudEnabled.get() && !minecraft.options.hideGui)) {
            lastUpdate = 0L;
 return;
        }
            long now = System.nanoTime
 ();
            int metrics = (config.debugHudFps.get() ? 1 : 0) | (config.debugHudMemory.get
() ? 2 : 0) | (config.debugHudAllocated.get
() ? 4 : 0);
            if (lastUpdate == 0L || metrics != lastMetrics || now - lastUpdate >= (long)(config.debugHudUpdateSeconds.get
() * 1.0E9)) {
                lastUpdate = now;
                lastMetrics = metrics;
                lineCount = 0;
                textWidth = 0;
                if (config.debugHudFps.get()) {
                    addLine(minecraft, "FPS: " + minecraft.getFps());
                }
                if (config.debugHudMemory.get() || config.debugHudAllocated.get()) {
                    Runtime runtime = Runtime.getRuntime();
                    long total = runtime.totalMemory();
                    long max = runtime.maxMemory();
                    if (config.debugHudMemory.get()) {
                        addLine(minecraft, "RAM: " + (total - runtime.freeMemory()) / 1048576L + "/" + max / 1048576L + " MB");
                    }
                    if (config.debugHudAllocated.get()) {
                        addLine(minecraft, "Allocated: " + total / 1048576L + "/" + max / 1048576L + " MB");
                    }
                }
            }

            if (!(lineCount != 0)) { return;
 }
                float scale = config.debugHudTextScale.get().floatValue();
                Config.DebugHudPosition position = config.debugHudPosition.get();
                boolean right = position == Config.DebugHudPosition.TOP_RIGHT || position == Config.DebugHudPosition.BOTTOM_RIGHT;
                boolean bottom = position == Config.DebugHudPosition.BOTTOM_LEFT || position == Config.DebugHudPosition.BOTTOM_RIGHT;
                float width = (float)textWidth * scale;
                float height = (float)(lineCount * 10) * scale;
                float x = right ? (float)(screenWidth - 4 - config.debugHudOffsetX.get()) - width : (float)(4 + config.debugHudOffsetX.get());
                float y = bottom ? (float)(screenHeight - 4 - config.debugHudOffsetY.get()) - height : (float)(4 + config.debugHudOffsetY.get());

                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0.0F);
                graphics.pose().scale(scale, scale, 1.0F);
                for (int index = 0; index < lineCount; index++) {
                    graphics.drawString(minecraft.font, lines[index], 0, index * 10, -1, true);
                }
                graphics.pose().popPose();
    }

    private static void addLine(Minecraft minecraft, String text) {
 lines[lineCount++] = text;
        textWidth = Math.max(textWidth, minecraft.font.width(text));
    }
    }
