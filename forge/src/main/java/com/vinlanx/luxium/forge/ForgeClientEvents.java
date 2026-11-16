package com.vinlanx.luxium.forge;

import com.vinlanx.luxium.client.f3.LuxiumF3Info;
import com.vinlanx.luxium.platform.ClientHooks;
import com.vinlanx.luxium.platform.RenderFrame;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent.DebugText;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class ForgeClientEvents { private ForgeClientEvents() { }

    @SubscribeEvent public static void tick(ClientTickEvent event) {

        if (event.phase == Phase.END) { ClientHooks.tick();
        } }

    @SubscribeEvent(priority = EventPriority.HIGHEST ) public static void debugText(DebugText event) {

 LuxiumF3Info.onDebugText(event.getRight());
    }

    private static RenderFrame frame(RenderLevelStageEvent event) {
        Stage nativeStage = event.getStage();
        RenderFrame.Stage stage;
        if (nativeStage == Stage.AFTER_SKY) { stage = RenderFrame.Stage.AFTER_SKY;
        } else if (nativeStage == Stage.AFTER_SOLID_BLOCKS) { stage = RenderFrame.Stage.AFTER_SOLID_BLOCKS;
        } else if (nativeStage == Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) { stage = RenderFrame.Stage.AFTER_CUTOUT_MIPPED_BLOCKS;
        } else if (nativeStage == Stage.AFTER_CUTOUT_BLOCKS) { stage = RenderFrame.Stage.AFTER_CUTOUT_BLOCKS;
        } else if (nativeStage == Stage.AFTER_ENTITIES) { stage = RenderFrame.Stage.AFTER_ENTITIES;
        } else if (nativeStage == Stage.AFTER_BLOCK_ENTITIES) { stage = RenderFrame.Stage.AFTER_BLOCK_ENTITIES;
        } else if (nativeStage == Stage.AFTER_TRANSLUCENT_BLOCKS) { stage = RenderFrame.Stage.AFTER_TRANSLUCENT_BLOCKS;
        } else if (nativeStage == Stage.AFTER_TRIPWIRE_BLOCKS) { stage = RenderFrame.Stage.AFTER_TRIPWIRE_BLOCKS; } else
if (nativeStage == Stage.AFTER_PARTICLES) { stage = RenderFrame.Stage.AFTER_PARTICLES; } else
if (nativeStage == Stage.AFTER_WEATHER) { stage = RenderFrame.Stage.AFTER_WEATHER; } else
if (nativeStage == Stage.AFTER_LEVEL) { stage = RenderFrame.Stage.AFTER_LEVEL; } else {
 return null; }
 return new RenderFrame(stage, event.getPoseStack(), event.getProjectionMatrix(), event.getRenderTick
 (), event.getPartialTick(), event.getCamera(), event.getFrustum() ); } @SubscribeEvent(priority = EventPriority.HIGHEST ) public static void renderHighest(RenderLevelStageEvent event) { RenderFrame




 frame = frame(event);
        if (frame != null) { ClientHooks.renderHighest(frame);
        } }

    @SubscribeEvent(priority = EventPriority.HIGH ) public static void renderHigh(RenderLevelStageEvent event) { RenderFrame

 frame = frame(event);
        if (frame != null) { ClientHooks.renderHigh(frame);
        } }

    @SubscribeEvent(priority = EventPriority.NORMAL ) public static void renderNormal(RenderLevelStageEvent event) { RenderFrame

 frame = frame(event);
        if (frame != null) { ClientHooks.renderNormal(frame);
        } }

    @SubscribeEvent(priority = EventPriority.LOW ) public static void renderLow(RenderLevelStageEvent event) { RenderFrame

 frame = frame(event);
        if (frame != null) { ClientHooks.renderLow(frame);
        } }

    @SubscribeEvent(priority = EventPriority.LOWEST ) public static void renderLowest(RenderLevelStageEvent event) { RenderFrame

 frame = frame(event);
        if (frame != null) { ClientHooks.renderLowest(frame);
        } }
        }
