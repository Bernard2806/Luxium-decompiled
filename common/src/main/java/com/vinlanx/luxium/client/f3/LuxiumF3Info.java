package com.vinlanx.luxium.client.f3;

import com.vinlanx.luxium.platform.LuxiumPlatform;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public final class LuxiumF3Info {
    private static final List<String>


LUXIUM_INFO = List.of("", ChatFormatting.RED + "Luxium v" + LuxiumPlatform.get

 ().version());

    private LuxiumF3Info()
    {
    }

    public static void onDebugText(List<String> right) {
        if (!(Minecraft.getInstance().options.renderDebug)) { return;
 }
            int targetIndex = 0;
            for (int blankLines = 0; targetIndex

 < right.size();



targetIndex++) { String line = right.get(targetIndex); if (line == null || line.isEmpty()) { blankLines++; } if (blankLines == 3) { break;
                }
            }
            right.addAll(targetIndex, LUXIUM_INFO);
    }
    }
