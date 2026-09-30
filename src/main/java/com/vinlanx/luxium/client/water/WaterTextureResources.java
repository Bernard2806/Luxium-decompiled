/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.SimpleTexture
 *  net.minecraft.resources.ResourceLocation
 */
package com.vinlanx.luxium.client.water;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;

public final class WaterTextureResources {
    private static final ResourceLocation MICRO_NORMAL = new ResourceLocation("luxium", "textures/effects/water_micro_normal.png");
    private static SimpleTexture microNormalTexture;

    private WaterTextureResources() {
    }

    public static int microNormalTextureId() {
        if (microNormalTexture == null) {
            microNormalTexture = new SimpleTexture(MICRO_NORMAL);
            Minecraft.m_91087_().m_91097_().m_118495_(MICRO_NORMAL, (AbstractTexture)microNormalTexture);
        }
        return microNormalTexture.m_117963_();
    }
}

