/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ShockWaveTest {
    private static final float START_RADIUS_BLOCKS = 0.25f;
    private static final float VISUAL_RADIUS_SCALE = 0.92f;
    private static final float FADE_START_PROGRESS = 0.7f;
    private static final int MIN_SPEED_BLOCKS_PER_SECOND = 1;
    private static final int MAX_SPEED_BLOCKS_PER_SECOND = 300;
    private static final SphereWarpEffect EFFECT = new SphereWarpEffect(ShaderManager::getAirDistortionShader, false);

    private ShockWaveTest() {
    }

    public static void spawnExplosion(Vec3 center, int power, int speed) {
        int clampedPower = Mth.m_14045_((int)power, (int)1, (int)40);
        int clampedSpeed = Mth.m_14045_((int)speed, (int)1, (int)300);
        float endRadius = (float)clampedPower * 0.92f;
        float travelSeconds = endRadius / (float)clampedSpeed;
        int totalTicks = Math.max(8, Mth.m_14167_((float)(travelSeconds * 20.0f)));
        EFFECT.startExpandingWave(center, 0.25f, endRadius, totalTicks, 0.7f);
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

