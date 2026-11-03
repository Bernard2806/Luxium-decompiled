package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.platform.RenderFrame;

public final class AirDistortion {
    private static final SphereWarpEffect
    EFFECT = new SphereWarpEffect(ShaderManager::getAirDistortionShader, false);

    private AirDistortion() { }

    public static void spawnAtPlayer(int radiusBlocks) {
        EFFECT.spawnAtPlayer(radiusBlocks);
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
