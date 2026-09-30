/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.client.sunmoonapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CelestialPath {
    public static final float ORBIT_TILT_DEGREES = -40.0f;
    private static final float ORBIT_TILT_RADIANS = -0.6981317f;
    private static final double TILT_COS = Math.cos(-0.6981316804885864);
    private static final double TILT_SIN = Math.sin(-0.6981316804885864);

    private CelestialPath() {
    }

    public static State sample(ClientLevel level, float partialTick) {
        float sunAngle = level.m_46490_(partialTick);
        Vec3 sunDirection = CelestialPath.sunDirection(sunAngle);
        Vec3 moonDirection = sunDirection.m_82490_(-1.0);
        boolean usingMoon = sunDirection.f_82480_ < 0.0;
        return new State(sunAngle, sunDirection, moonDirection, usingMoon ? moonDirection : sunDirection, usingMoon);
    }

    public static Vec3 sunDirection(float sunAngle) {
        double cosine = Mth.m_14089_((float)sunAngle);
        return new Vec3((double)(-Mth.m_14031_((float)sunAngle)), cosine * TILT_COS, -cosine * TILT_SIN);
    }

    public static void applyOrbitTilt(PoseStack poseStack) {
        poseStack.m_252781_(Axis.f_252529_.m_252977_(40.0f));
    }

    public static void applyOrbit(PoseStack poseStack, float sunAngle) {
        CelestialPath.applyOrbitTilt(poseStack);
        poseStack.m_252781_(Axis.f_252436_.m_252977_(-90.0f));
        poseStack.m_252781_(Axis.f_252529_.m_252961_(sunAngle));
    }

    public record State(float sunAngle, Vec3 sunDirection, Vec3 moonDirection, Vec3 activeDirection, boolean usingMoon) {
        public Vec3 direction(boolean moon) {
            return moon ? this.moonDirection : this.sunDirection;
        }

        public double elevation(boolean moon) {
            return this.direction((boolean)moon).f_82480_;
        }
    }
}

