package com.vinlanx.luxium.client.ConfigScreen;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance.IntRange;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

final class ResolutionControls {
    private ResolutionControls() { }

    static AbstractWidget createSlider(Minecraft minecraft, int width) {
        Window window = minecraft.getWindow();
        Monitor monitor = window.findBestMonitor();
        int selected = monitor == null ? -1 : window.getPreferredFullscreenVideoMode
 ().map(monitor::getVideoModeIndex).orElse(-1);
        OptionInstance<Integer> resolution = new OptionInstance<>(
            "options.fullscreen.resolution", OptionInstance.noTooltip(),
            (caption, index) -> (Component)(monitor == null
                    ? Component.translatable("options.fullscreen.unavailable") : Options.genericValueLabel(
                        caption, index == -1 ? Component.translatable("options.fullscreen.current") : Component.literal(monitor.getMode(index).toString()) )),
            new IntRange(-1, monitor


== null ? -1 : monitor.getModeCount() - 1),
            selected,
            index -> { if (monitor != null) {
                    window.setPreferredFullscreenVideoMode(index == -1 ? Optional.empty
 () : Optional.of(monitor.getMode(index)));
                }
            } ); AbstractWidget
            slider = resolution.createButton(minecraft.options, 0, 0, width);
        slider.active = monitor != null && monitor.getModeCount() > 0;
        return slider;
    }
    }
