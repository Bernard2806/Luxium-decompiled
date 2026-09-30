/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.rtx.LightRtMath;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TestFlashLight {
    private static final int MAX_POWER = 15;
    private static final int MIN_POWER = 1;
    private static final int MIN_CONE_ANGLE = 1;
    private static final int MAX_CONE_ANGLE = 180;
    private static final int MAX_RAYS_AT_180 = 2000;
    private static final int MIN_RAYS = 1;
    private static final double STEP = 0.5;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double RAY_ANGLE_EXPONENT = 1.365;
    private static volatile boolean enabled = false;
    private static volatile int power = 15;
    private static volatile int coneAngle = 180;
    private static Long2ByteOpenHashMap lightMap = TestFlashLight.createEmptyMap();
    private static int lastTick = Integer.MIN_VALUE;
    private static Vec3 previousEyePos;

    private TestFlashLight() {
    }

    public static void enable(int newPower, int newConeAngle) {
        power = Mth.m_14045_((int)newPower, (int)1, (int)15);
        coneAngle = Mth.m_14045_((int)newConeAngle, (int)1, (int)180);
        enabled = true;
        lastTick = Integer.MIN_VALUE;
        lightMap = TestFlashLight.createEmptyMap();
    }

    public static void disable() {
        enabled = false;
        lastTick = Integer.MIN_VALUE;
        previousEyePos = null;
        lightMap = TestFlashLight.createEmptyMap();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getPower() {
        return power;
    }

    public static int getConeAngle() {
        return coneAngle;
    }

    public static int getCurrentRayCount() {
        return TestFlashLight.computeRayCount(coneAngle);
    }

    public static int sampleLight(BlockPos pos) {
        if (!enabled) {
            return -1;
        }
        return lightMap.get(pos.m_121878_()) & 0xFF;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled) {
            return;
        }
        TestFlashLight.updateIfNeeded();
    }

    private static void updateIfNeeded() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            lightMap = TestFlashLight.createEmptyMap();
            return;
        }
        int currentTick = mc.f_91074_.f_19797_;
        if (currentTick == lastTick) {
            return;
        }
        lastTick = currentTick;
        ClientLevel level = mc.f_91073_;
        Vec3 start = mc.f_91074_.m_146892_();
        Vec3 forward = mc.f_91074_.m_20154_().m_82541_();
        Vec3 right = TestFlashLight.computeRight(forward);
        Vec3 up = right.m_82537_(forward).m_82541_();
        int rays = TestFlashLight.computeRayCount(coneAngle);
        double halfAngleRad = Math.toRadians((double)coneAngle * 0.5);
        double cosHalfAngle = Math.cos(halfAngleRad);
        double maxDistance = LightRtMath.getMaxDistance(power);
        Long2ByteOpenHashMap newMap = TestFlashLight.createEmptyMap();
        newMap.put(BlockPos.m_274446_((Position)start).m_121878_(), (byte)power);
        for (int i = 0; i < rays; ++i) {
            int brightness;
            Vec3 dir = TestFlashLight.computeDirectionInCone(forward, right, up, i, rays, cosHalfAngle);
            Vec3 end = start.m_82549_(dir.m_82490_(maxDistance));
            BlockHitResult hit = level.m_45547_(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)mc.f_91074_));
            double rayLength = maxDistance;
            if (hit.m_6662_() != HitResult.Type.MISS) {
                rayLength = start.m_82554_(hit.m_82450_());
            }
            for (double traveled = 0.0; traveled <= rayLength && (brightness = LightRtMath.getFalloff(power, traveled)) > 0; traveled += 0.5) {
                Vec3 point = start.m_82549_(dir.m_82490_(traveled));
                BlockPos blockPos = BlockPos.m_274446_((Position)point);
                long key = blockPos.m_121878_();
                byte current = newMap.get(key);
                if ((current & 0xFF) >= brightness) continue;
                newMap.put(key, (byte)brightness);
            }
        }
        TestFlashLight.markDirtyVolume(mc, start, maxDistance);
        if (previousEyePos != null) {
            TestFlashLight.markDirtyVolume(mc, previousEyePos, maxDistance);
        }
        previousEyePos = start;
        lightMap = newMap;
    }

    private static Vec3 computeRight(Vec3 forward) {
        Vec3 up = Math.abs(forward.f_82480_) > 0.999 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        return forward.m_82537_(up).m_82541_();
    }

    private static Vec3 computeDirectionInCone(Vec3 forward, Vec3 right, Vec3 up, int index, int total, double cosHalfAngle) {
        if (total <= 1) {
            return forward;
        }
        if (index == 0) {
            return forward;
        }
        int spreadCount = total - 1;
        int spreadIndex = index - 1;
        double u = ((double)spreadIndex + 0.5) / (double)spreadCount;
        double cosTheta = 1.0 - u * (1.0 - cosHalfAngle);
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double phi = (double)spreadIndex * GOLDEN_ANGLE;
        double rx = Math.cos(phi) * sinTheta;
        double ux = Math.sin(phi) * sinTheta;
        return forward.m_82490_(cosTheta).m_82549_(right.m_82490_(rx)).m_82549_(up.m_82490_(ux)).m_82541_();
    }

    private static int computeRayCount(int angleDeg) {
        double t = (double)Mth.m_14045_((int)angleDeg, (int)1, (int)180) / 180.0;
        int rays = (int)Math.round(2000.0 * Math.pow(t, 1.365));
        return Math.max(1, Math.min(2000, rays));
    }

    private static Long2ByteOpenHashMap createEmptyMap() {
        Long2ByteOpenHashMap map = new Long2ByteOpenHashMap();
        map.defaultReturnValue((byte)0);
        return map;
    }

    private static void markDirtyVolume(Minecraft mc, Vec3 eyePos, double radius) {
        if (mc.f_91060_ == null) {
            return;
        }
        int minX = Mth.m_14107_((double)(eyePos.f_82479_ - radius)) - 1;
        int minY = Mth.m_14107_((double)(eyePos.f_82480_ - radius)) - 1;
        int minZ = Mth.m_14107_((double)(eyePos.f_82481_ - radius)) - 1;
        int maxX = Mth.m_14107_((double)(eyePos.f_82479_ + radius)) + 1;
        int maxY = Mth.m_14107_((double)(eyePos.f_82480_ + radius)) + 1;
        int maxZ = Mth.m_14107_((double)(eyePos.f_82481_ + radius)) + 1;
        mc.f_91060_.m_109494_(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

