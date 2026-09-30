/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package com.vinlanx.luxium.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={Camera.class})
public interface CameraAccessor {
    @Invoker(value="setPosition")
    public void luxium$setPosition(double var1, double var3, double var5);

    @Invoker(value="setRotation")
    public void luxium$setRotation(float var1, float var2);
}

