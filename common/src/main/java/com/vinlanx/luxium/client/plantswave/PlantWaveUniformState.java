package com.vinlanx.luxium.client.plantswave;

import com.vinlanx.luxium.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public record PlantWaveUniformState(
    int enabled,
    float time,
    float cameraX,
    float cameraY,
    float cameraZ,
    float strength,
    float speed,
    float gustStrength,
    float distance,
    float grassStrength,
    float leavesStrength,
    float aquaticStrength,
    float bendStrength ) {
    public static PlantWaveUniformState capture() {
        Minecraft minecraft = Minecraft.getInstance
 ();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        float time = minecraft.level == null ?
 0.0F : ((float)minecraft.level.getGameTime
() + minecraft.getFrameTime()) / 20.0F % 4096.0F;

        return new PlantWaveUniformState(
            Config.isFeatureEnabled(Config.CLIENT.plantsWaveEnabled) ? 1 : 0,
            time,
            (float)camera.x,
            (float)camera.y,
            (float)camera.z,
            Config.CLIENT.plantsWaveStrength.get().floatValue(),
            Config.CLIENT.plantsWaveSpeed.get().floatValue(),
            Config.CLIENT.plantsWaveGustStrength.get().floatValue(),
            Config.CLIENT.plantsWaveDistance.get().floatValue(),
            Config.CLIENT.plantsWaveGrassStrength.get().floatValue(),
            Config.CLIENT.plantsWaveLeavesStrength.get().floatValue(),
            Config.CLIENT.plantsWaveAquaticStrength.get().floatValue(),
            Config.CLIENT.plantsWaveBendStrength.get().floatValue() );
    }
}
