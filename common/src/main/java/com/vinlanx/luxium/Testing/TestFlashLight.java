package com.vinlanx.luxium.Testing;

import com.vinlanx.luxium.rtx.LightRtMath;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public final class TestFlashLight {
    private static final int MAX_POWER = 15;
    private static final int MIN_POWER = 1;
    private static final int MIN_CONE_ANGLE = 1;
    private static final int MAX_CONE_ANGLE = 180;
    private static final int MAX_RAYS_AT_180 = 2000; private static final int MIN_RAYS = 1; private static final double STEP = 0.5; private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double RAY_ANGLE_EXPONENT = 1.365;
    private static volatile boolean
    enabled = false;
    private static volatile int power = 15;
    private static volatile int coneAngle = 180;
    private static Long2ByteOpenHashMap lightMap = createEmptyMap
 ();
    private static int lastTick = Integer.MIN_VALUE;
    private static Vec3 previousEyePos;

    private TestFlashLight() {
    }

    public static void enable(int newPower, int newConeAngle) {
        power = Mth.clamp(newPower, 1, 15);
        coneAngle = Mth.clamp(newConeAngle, 1, 180);
        enabled = true;
        lastTick = Integer.MIN_VALUE;
        lightMap = createEmptyMap();
    }

    public static void disable() {
        enabled = false;
        lastTick = Integer.MIN_VALUE;
        previousEyePos = null;
        lightMap = createEmptyMap();
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
        return computeRayCount(coneAngle);
    }

    public static int sampleLight(BlockPos pos) {
        if (!enabled) {
            return -1;
        }
        return lightMap.get(pos.asLong()) & 0xFF;
    }

    public static void onClientTick() {
        if (!(enabled)) {
return;
}
            updateIfNeeded();
    }

    private static void updateIfNeeded() {
        Minecraft mc = Minecraft.getInstance ();
        if (mc.level == null || mc.player == null) {
            lightMap = TestFlashLight.createEmptyMap();
            return;
        }

        int currentTick = mc.player.tickCount;
        if (currentTick == lastTick) {
            return;
        }
        lastTick = currentTick;

        ClientLevel level = mc.level;
        Vec3 start = mc.player.getEyePosition();
        Vec3 forward = mc.player.getLookAngle().normalize();
        Vec3 right = TestFlashLight.computeRight(forward);
        Vec3 up = right.cross(forward).normalize();

        int rays = TestFlashLight.computeRayCount(coneAngle);
        double halfAngleRad = Math.toRadians((double)coneAngle * 0.5);
        double cosHalfAngle = Math.cos(halfAngleRad);
        double maxDistance = LightRtMath.getMaxDistance(power);

        Long2ByteOpenHashMap newMap = TestFlashLight.createEmptyMap();
        newMap.put(BlockPos.containing((Position)start).asLong(), (byte)power);

        for (int i = 0; i < rays; ++i) {
            Vec3 dir = TestFlashLight.computeDirectionInCone(forward, right, up, i, rays, cosHalfAngle);
            Vec3 end = start.add(dir.scale(maxDistance));
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)mc.player));
            double rayLength = maxDistance;
            if (hit.getType() != HitResult.Type.MISS) {
                rayLength = start.distanceTo(hit.getLocation());
            }

            for (double traveled = 0.0; traveled <= rayLength; traveled += 0.5) {
                int brightness = LightRtMath.getFalloff(power, traveled);
                if (brightness <= 0)
                        break;


                Vec3 point = start.add(dir.scale(traveled));
                BlockPos blockPos = BlockPos.containing((Position)point);
                long key = blockPos.asLong();
                byte current = newMap.get(key);
                if ((current & 0xFF) < brightness) {
                    newMap.put(key, (byte)brightness);
                }
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
        Vec3 up = Math.abs (forward.y) > 0.999 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        return forward.cross(up).normalize();
    }

    private static Vec3 computeDirectionInCone(Vec3 forward, Vec3 right, Vec3 up, int index, int total, double cosHalfAngle) {
        if (total <= 1) {
            return forward;
        } else

        if (index == 0) {

            return forward;
        }
            int
            spreadCount = total - 1;
            int spreadIndex = index - 1;
            double u = ((double)spreadIndex + 0.5) / (double)spreadCount;
            double cosTheta = 1.0 - u * (1.0 - cosHalfAngle);
            double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
            double phi = (double)spreadIndex * GOLDEN_ANGLE;
            double
            rx = Math.cos(phi) * sinTheta;
            double ux = Math.sin(phi) * sinTheta;

            return forward.scale(cosTheta).add
(right.scale(rx)).add
(up.scale(ux)).normalize
();
        
    }

    private static int computeRayCount(int angleDeg) { double t = (double)Mth.clamp(angleDeg, 1, 180) / 180.0;
        int rays = (int)Math.round(2000.0 * Math.pow(t, 1.365));
        return Math.max(1, Math.min(2000, rays));
    }

    private static Long2ByteOpenHashMap createEmptyMap() {
        Long2ByteOpenHashMap map = new Long2ByteOpenHashMap();
        map.defaultReturnValue((byte)0);
        return map;
    }

    private static void markDirtyVolume(Minecraft mc, Vec3 eyePos, double radius) {
        if (mc.levelRenderer == null) {
            return;
        }

        int minX = Mth.floor((double)(eyePos.x - radius)) - 1;
        int minY = Mth.floor((double)(eyePos.y - radius)) - 1;
        int minZ = Mth.floor((double)(eyePos.z - radius)) - 1;
        int maxX = Mth.floor((double)(eyePos.x + radius)) + 1;
        int maxY = Mth.floor((double)(eyePos.y + radius)) + 1;
        int maxZ = Mth.floor((double)(eyePos.z + radius)) + 1;
        mc.levelRenderer.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
    }
    }
