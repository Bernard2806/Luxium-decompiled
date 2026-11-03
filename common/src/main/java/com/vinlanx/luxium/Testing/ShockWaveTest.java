package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.platform.RenderFrame;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShockWaveTest {
    private static final float START_RADIUS_BLOCKS = 0.25F;
    private static final float VISUAL_RADIUS_SCALE = 0.92F;
    private static final float FADE_START_PROGRESS = 0.7F;
    private static final int MIN_SPEED_BLOCKS_PER_SECOND = 1;
    private static final int MAX_SPEED_BLOCKS_PER_SECOND = 300;
    private static final SphereWarpEffect

    EFFECT = new SphereWarpEffect(ShaderManager::getAirDistortionShader, false);

    private ShockWaveTest() { }

    public static void spawnExplosion(Vec3 center, int power, int speed) {
        int clampedPower = Mth.clamp(power, 1, 40);
        int clampedSpeed = Mth.clamp(speed, 1, 300);
        float endRadius = (float)clampedPower * 0.92F;
        float travelSeconds = endRadius / (float)clampedSpeed;
        int totalTicks = Math.max(8, Mth.ceil(travelSeconds * 20.0F));
        EFFECT.startExpandingWave(center, 0.25F, endRadius, totalTicks, 0.7F);
    }

    public static void clear() {
        EFFECT.clear();
    }

    public static void onClientTick() {
        EFFECT.onClientTick();
    }

    public static void onRenderLevel(RenderFrame event) {
        if (!(event.getStage() == RenderFrame.Stage.AFTER_PARTICLES)) {
return;
}
            EFFECT.onRenderLevel(event.getPoseStack().last().pose());
    }
    }
