/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  net.minecraft.client.renderer.RenderType
 */
package com.vinlanx.luxium.client.water;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.renderer.RenderType;

public final class WaterTerrainPass {
    private static TerrainRenderPass water;

    private WaterTerrainPass() {
    }

    public static void bootstrap() {
        if (water == null) {
            water = new TerrainRenderPass(RenderType.m_110466_(), false, false);
        }
    }

    public static TerrainRenderPass get() {
        WaterTerrainPass.bootstrap();
        return water;
    }

    public static boolean is(TerrainRenderPass pass) {
        return pass != null && pass == water;
    }
}

