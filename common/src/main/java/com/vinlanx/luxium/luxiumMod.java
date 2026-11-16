package com.vinlanx.luxium;

import com.vinlanx.luxium.client.guiscreen.AlphaWarningScreen;
import com.vinlanx.luxium.platform.LuxiumPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public final class luxiumMod {
    public static final String MOD_ID = "luxium";
    private static boolean



    warningScheduled = false;

    private luxiumMod() { }

    public static void onClientTick() {
        if (!(LuxiumPlatform.incompatibilityScreen() == null)) { return;
 } if (!(!warningScheduled)) { return; }
                if (!(AlphaWarningScreen.shouldShow())) { return; }
                    Minecraft mc = Minecraft.getInstance
 ();
                    if (!(mc.screen instanceof TitleScreen)) { return; }

 warningScheduled = true;
 mc.execute(() -> mc.setScreen(new AlphaWarningScreen()));
    }
    }
