/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.plantswave;

import com.vinlanx.luxium.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public record PlantWaveUniformState(int enabled, float time, float cameraX, float cameraY, float cameraZ, float strength, float speed, float gustStrength, float distance, float grassStrength, float leavesStrength, float aquaticStrength, float bendStrength) {
    public static PlantWaveUniformState capture() {
        Minecraft minecraft = Minecraft.m_91087_();
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        float time = minecraft.f_91073_ == null ? 0.0f : ((float)minecraft.f_91073_.m_46467_() + minecraft.m_91296_()) / 20.0f % 4096.0f;
        return new PlantWaveUniformState(Config.isFeatureEnabled(Config.CLIENT.plantsWaveEnabled) ? 1 : 0, time, (float)camera.f_82479_, (float)camera.f_82480_, (float)camera.f_82481_, ((Double)Config.CLIENT.plantsWaveStrength.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveSpeed.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveGustStrength.get()).floatValue(), ((Integer)Config.CLIENT.plantsWaveDistance.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveGrassStrength.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveLeavesStrength.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveAquaticStrength.get()).floatValue(), ((Double)Config.CLIENT.plantsWaveBendStrength.get()).floatValue());
    }
}

