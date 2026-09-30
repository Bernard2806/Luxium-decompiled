/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.Testing.SphereWarpEffect;
import com.vinlanx.luxium.client.ShaderManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class AirDistortion {
    private static final SphereWarpEffect EFFECT = new SphereWarpEffect(ShaderManager::getAirDistortionShader, false);

    private AirDistortion() {
    }

    public static void spawnAtPlayer(int radiusBlocks) {
        EFFECT.spawnAtPlayer(radiusBlocks);
    }

    public static void clear() {
        EFFECT.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EFFECT.onClientTick();
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        EFFECT.onRenderLevel(event.getPoseStack().m_85850_().m_252922_());
    }
}

