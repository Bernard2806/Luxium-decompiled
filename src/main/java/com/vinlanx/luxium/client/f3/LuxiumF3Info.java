package com.vinlanx.luxium.client.f3;



import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
public final class LuxiumF3Info {
    private static final List<String> LUXIUM_INFO = List.of("", ChatFormatting.RED + "Luxium v" + ((ModContainer)ModList.get

            ().getModContainerById
            ("luxium").orElseThrow
            (() -> new IllegalStateException("Luxium mod metadata is unavailable"))).getModInfo
            ().getVersion
            ());


    private LuxiumF3Info() {
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!Minecraft.m_91087_().f_91066_.f_92063_) { return;
        }
        List<String> right = event.getRight();
        int targetIndex = 0;
        int blankLines = 0;

        while (targetIndex < right.size()) {
            String line = (String)right.get(targetIndex);
            if (line == null || line.isEmpty()) {++blankLines; }
            if (blankLines == 3) break;
            ++targetIndex;
        }

        right.addAll(targetIndex, LUXIUM_INFO);
    }
}
