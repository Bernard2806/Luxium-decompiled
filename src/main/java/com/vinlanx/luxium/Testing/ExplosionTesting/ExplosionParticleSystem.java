/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionCollisionCache;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionFluidGrid;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionProfile;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionRenderSnapshot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ExplosionParticleSystem {
    static final int MAX_PARTICLES = 70000;
    static final byte FIRE = 0;
    static final byte SMOKE = 1;
    static final byte SPARK = 2;
    static final byte DUST = 3;
    private static final float AMBIENT_K = 293.15f;
    private final Random random = new Random(7L);
    private final ArrayList<Particle> particles = new ArrayList(70000);

    ExplosionParticleSystem() {
    }

    void clear() {
        this.particles.clear();
    }

    int count() {
        return this.particles.size();
    }

    void writeSnapshot(ExplosionRenderSnapshot snapshot) {
        int smoke = 0;
        for (Particle particle : this.particles) {
            if (particle.emissive()) continue;
            ++smoke;
        }
        int smokeIndex = 0;
        int hotIndex = smoke;
        for (Particle particle : this.particles) {
            int index = particle.emissive() ? hotIndex++ : smokeIndex++;
            snapshot.x[index] = particle.x;
            snapshot.y[index] = particle.y;
            snapshot.z[index] = particle.z;
            snapshot.r[index] = particle.r;
            snapshot.g[index] = particle.g;
            snapshot.b[index] = particle.b;
            snapshot.alpha[index] = particle.alpha;
            snapshot.size[index] = particle.size;
            snapshot.frame[index] = (byte)particle.frame;
        }
        snapshot.smokeCount = smoke;
        snapshot.totalCount = this.particles.size();
    }

    void trigger(Vec3 center, ExplosionProfile profile, boolean nearGround) {
        int available;
        int requested = (int)(12400.0f * profile.particleMultiplier());
        if (70000 - this.particles.size() < Math.max(2000, requested)) {
            this.compact(true);
        }
        if ((available = 70000 - this.particles.size()) <= 0) {
            return;
        }
        double multiplier = profile.particleMultiplier();
        double[] dArray = new double[]{4300.0 * multiplier, 5900.0 * multiplier, 720.0 * Math.pow(multiplier, 0.84), nearGround ? 1450.0 * multiplier * (double)profile.dustMultiplier() : 0.0};
        double[] target = dArray;
        double total = target[0] + target[1] + target[2] + target[3];
        if (total > (double)available) {
            int i = 0;
            while (i < target.length) {
                int n = i++;
                target[n] = target[n] * ((double)available / total);
            }
        }
        this.spawnKind(center, profile, (byte)0, (int)target[0], nearGround);
        this.spawnKind(center, profile, (byte)1, (int)target[1], nearGround);
        this.spawnKind(center, profile, (byte)2, (int)target[2], nearGround);
        this.spawnKind(center, profile, (byte)3, (int)target[3], true);
    }

    void spawnAfterburn(Vec3 center, ExplosionProfile profile, int requested) {
        int n = Math.min(requested, 70000 - this.particles.size());
        for (int i = 0; i < n; ++i) {
            byte kind = this.random.nextFloat() < 0.55f ? (byte)0 : 1;
            Vec3 direction = this.randomDirection(true);
            double radius = this.randomRange(0.08f, 0.48f) * profile.coreRadius() / 1.35f;
            Vec3 position = center.m_82549_(direction.m_82490_(radius));
            double speed = this.randomRange(0.35f, 1.7f) * profile.blastStrength() * 0.22f;
            Vec3 velocity = direction.m_82490_(speed).m_82520_(0.0, (double)this.randomRange(0.2f, 1.1f) * Math.pow(profile.scale(), 0.14), 0.0);
            float temp = (kind == 0 ? this.randomRange(1900.0f, 4300.0f) : this.randomRange(700.0f, 1900.0f)) * profile.heatMultiplier();
            float life = (kind == 0 ? this.randomRange(0.8f, 1.8f) : this.randomRange(3.6f, 7.0f)) * profile.durationScale();
            this.add(position, velocity, temp, this.randomRange(0.38f, 1.05f), life, this.randomRange(0.3f, 0.9f) * profile.particleSizeScale(), kind);
        }
    }

    private void spawnKind(Vec3 center, ExplosionProfile profile, byte kind, int count, boolean nearGround) {
        float spatialScale = profile.coreRadius() / 1.35f;
        for (int i = 0; i < count && this.particles.size() < 70000; ++i) {
            float size;
            float life;
            float density;
            float temperature;
            float speed;
            float radius;
            Vec3 direction;
            boolean upward = nearGround || kind == 2 || kind == 3;
            Vec3 vec3 = direction = kind == 3 ? this.dustDirection() : this.randomDirection(upward);
            if (kind == 0) {
                radius = (float)Math.cbrt(this.random.nextFloat()) * 0.58f * spatialScale;
                speed = profile.blastStrength() * this.randomRange(0.62f, 1.28f);
                temperature = this.randomRange(2600.0f, 6100.0f) * profile.heatMultiplier();
                density = this.randomRange(0.25f, 0.92f);
                life = this.randomRange(1.15f, 2.65f) * profile.durationScale();
                size = this.randomRange(0.36f, 1.08f) * profile.particleSizeScale();
            } else if (kind == 1) {
                radius = (float)Math.cbrt(this.random.nextFloat()) * 0.78f * spatialScale;
                speed = profile.blastStrength() * this.randomRange(0.28f, 0.72f);
                temperature = this.randomRange(850.0f, 2900.0f) * profile.heatMultiplier();
                density = this.randomRange(0.46f, 1.25f);
                life = this.randomRange(5.0f, 10.5f) * profile.durationScale();
                size = this.randomRange(0.54f, 1.44f) * profile.particleSizeScale();
            } else if (kind == 2) {
                radius = this.randomRange(0.1f, 0.5f) * spatialScale;
                speed = profile.blastStrength() * this.randomRange(1.15f, 2.4f);
                temperature = this.randomRange(1800.0f, 4300.0f) * profile.heatMultiplier();
                density = this.randomRange(0.5f, 1.0f);
                life = this.randomRange(0.8f, 2.1f) * (float)Math.pow(profile.durationScale(), 0.72);
                size = this.randomRange(0.15f, 0.36f) * profile.particleSizeScale();
            } else {
                radius = this.randomRange(0.15f, 0.75f) * spatialScale;
                speed = profile.blastStrength() * this.randomRange(0.55f, 1.15f);
                temperature = this.randomRange(450.0f, 1350.0f) * profile.heatMultiplier();
                density = this.randomRange(0.55f, 1.4f);
                life = this.randomRange(3.0f, 7.2f) * profile.durationScale();
                size = this.randomRange(0.36f, 1.02f) * profile.particleSizeScale();
            }
            Vec3 position = center.m_82549_(direction.m_82490_((double)radius));
            if (kind == 3) {
                position = new Vec3(position.f_82479_, Math.max(position.f_82480_, center.f_82480_ + 0.02), position.f_82481_);
            }
            Vec3 velocity = direction.m_82490_((double)speed);
            if (kind == 0 || kind == 1) {
                velocity = velocity.m_82520_(0.0, (double)this.randomRange(0.25f, 1.8f) * Math.pow(profile.scale(), 0.17), 0.0);
            }
            this.add(position, velocity, temperature, density, life, size, kind);
        }
    }

    void step(float dt, ExplosionFluidGrid fluid, ExplosionCollisionCache collision, float simulationTime) {
        dt = Mth.m_14036_((float)dt, (float)0.0f, (float)0.04f);
        for (Particle p : this.particles) {
            p.age += dt;
            Vec3 gridVelocity = fluid.sampleVelocity(p.x, p.y, p.z);
            float coupling = p.kind == 1 ? 2.7f : (p.kind == 0 ? 1.85f : (p.kind == 3 ? 1.3f : 0.28f));
            float blend = 1.0f - (float)Math.exp(-coupling * dt);
            p.vx = (float)((double)p.vx + (gridVelocity.f_82479_ - (double)p.vx) * (double)blend);
            p.vy = (float)((double)p.vy + (gridVelocity.f_82480_ - (double)p.vy) * (double)blend);
            p.vz = (float)((double)p.vz + (gridVelocity.f_82481_ - (double)p.vz) * (double)blend);
            float qx = p.x * 0.72f + p.seed * 0.017f;
            float qy = p.y * 0.93f + p.seed * 0.011f;
            float qz = p.z * 0.72f + p.seed * 0.019f;
            float curlX = Mth.m_14031_((float)(qy * 1.7f + simulationTime * 1.1f)) - Mth.m_14089_((float)(qz * 1.25f - simulationTime * 0.8f));
            float curlY = Mth.m_14031_((float)(qz * 1.55f + simulationTime * 0.7f)) - Mth.m_14089_((float)(qx * 1.45f + simulationTime * 0.9f));
            float curlZ = Mth.m_14031_((float)(qx * 1.35f - simulationTime)) - Mth.m_14089_((float)(qy * 1.65f - simulationTime * 0.6f));
            float turbulenceWeight = p.kind == 2 ? 0.12f : 0.62f;
            p.vx += curlX * dt * turbulenceWeight;
            p.vy += curlY * dt * turbulenceWeight;
            p.vz += curlZ * dt * turbulenceWeight;
            float hotness = Mth.m_14036_((float)((p.temperature - 293.15f) / 2600.0f), (float)0.0f, (float)2.0f);
            p.vy = p.vy + (hotness * 4.1f - p.density * (p.kind == 3 ? 0.72f : 0.16f)) * dt;
            p.vy = p.vy - (p.kind == 2 ? 9.81f : (p.kind == 3 ? 2.2f : 0.08f)) * dt;
            float targetTemperature = 293.15f + fluid.sampleTemperature(p.x, p.y, p.z) * 1350.0f;
            p.temperature += Math.max(targetTemperature - p.temperature, 0.0f) * (1.0f - (float)Math.exp(-dt * 0.8f));
            float cooling = 0.46f * (p.kind == 2 ? 1.65f : (p.kind == 0 ? 0.82f : 1.0f));
            p.temperature += (293.15f - p.temperature) * (1.0f - (float)Math.exp(-cooling * dt));
            p.density = p.density * (float)Math.exp(-dt * (p.kind == 1 ? 0.055f : (p.kind == 3 ? 0.1f : 0.18f)));
            if (p.kind == 0 && (p.temperature < 1150.0f || p.age > p.life * 0.68f)) {
                p.kind = 1;
                p.density = Math.max(p.density, 0.42f);
                p.life = Math.max(p.life, p.age + this.randomRange(2.5f, 5.0f));
            }
            float drag = p.kind == 2 ? 0.22f : (p.kind == 3 ? 0.55f : 0.34f);
            float dragFactor = (float)Math.exp(-drag * dt);
            p.vx *= dragFactor;
            p.vy *= dragFactor;
            p.vz *= dragFactor;
            ExplosionParticleSystem.moveWithCollision(p, dt, collision);
            this.updateVisuals(p, hotness);
        }
        this.compact(false);
    }

    private static void moveWithCollision(Particle p, float dt, ExplosionCollisionCache collision) {
        float oldX = p.x;
        float nextX = oldX + p.vx * dt;
        float oldY = p.y;
        float oldZ = p.z;
        if (!collision.isSolid(nextX, oldY, oldZ)) {
            p.x = nextX;
        } else {
            p.vx *= -0.16f;
        }
        float nextY = oldY + p.vy * dt;
        if (!collision.isSolid(p.x, nextY, oldZ)) {
            p.y = nextY;
        } else {
            p.vy = p.vy * (p.kind == 2 ? -0.42f : -0.08f);
        }
        float nextZ = oldZ + p.vz * dt;
        if (!collision.isSolid(p.x, p.y, nextZ)) {
            p.z = nextZ;
        } else {
            p.vz *= -0.16f;
        }
    }

    private void updateVisuals(Particle p, float hotness) {
        float fadeIn = Mth.m_14036_((float)(p.age / 0.08f), (float)0.0f, (float)1.0f);
        float fadeOut = Mth.m_14036_((float)((p.life - p.age) / Math.max(p.life * 0.24f, 0.15f)), (float)0.0f, (float)1.0f);
        float glow = Mth.m_14036_((float)((p.temperature - 620.0f) / 1750.0f), (float)0.0f, (float)1.0f) * (p.kind == 1 ? 0.72f : 1.0f);
        float[] blackbody = ExplosionParticleSystem.blackbody(p.temperature);
        float soot = Mth.m_14036_((float)(0.24f - p.density * 0.13f + hotness * 0.05f), (float)0.025f, (float)0.31f);
        float sr = soot * (p.kind == 3 ? 1.55f : 1.02f);
        float sg = soot * (p.kind == 3 ? 1.23f : 0.95f);
        float sb = soot * (p.kind == 3 ? 0.82f : 0.88f);
        p.r = Mth.m_14036_((float)(Mth.m_14179_((float)glow, (float)sr, (float)blackbody[0]) + blackbody[0] * glow * glow * 0.32f), (float)0.0f, (float)2.4f);
        p.g = Mth.m_14036_((float)(Mth.m_14179_((float)glow, (float)sg, (float)blackbody[1]) + blackbody[1] * glow * glow * 0.32f), (float)0.0f, (float)2.4f);
        p.b = Mth.m_14036_((float)(Mth.m_14179_((float)glow, (float)sb, (float)blackbody[2]) + blackbody[2] * glow * glow * 0.32f), (float)0.0f, (float)2.4f);
        float kindAlpha = p.kind == 0 ? 0.68f : (p.kind == 2 ? 0.96f : (p.kind == 3 ? 0.42f : 0.31f));
        p.alpha = Mth.m_14036_((float)(p.density * fadeIn * fadeOut * kindAlpha), (float)0.0f, (float)0.98f);
        float growth = p.kind == 1 ? 1.0f + p.age * 0.43f : (p.kind == 3 ? 1.0f + p.age * 0.22f : 1.0f + p.age * 0.12f);
        p.size = p.baseSize * growth * Mth.m_14036_((float)(0.75f + p.density * 0.48f), (float)0.55f, (float)2.0f);
        float score = p.density * 3.3f + Mth.m_14036_((float)(p.age / Math.max(p.life, 1.0E-4f)), (float)0.0f, (float)1.0f) * 3.4f;
        p.frame = Mth.m_14045_((int)Mth.m_14143_((float)(score += p.kind == 1 ? 1.0f : (p.kind == 3 ? 0.6f : 0.0f))), (int)0, (int)7);
    }

    private void compact(boolean forceDrop) {
        Iterator<Particle> it = this.particles.iterator();
        while (it.hasNext()) {
            boolean alive;
            Particle p = it.next();
            boolean bl = alive = p.age < p.life && p.alpha > 0.003f && (double)p.y > -64.0;
            if (forceDrop && (double)this.particles.size() > 50400.0 && p.kind != 0 && p.kind != 2 && p.age >= p.life * 0.72f) {
                alive = false;
            }
            if (alive) continue;
            it.remove();
        }
    }

    private void add(Vec3 position, Vec3 velocity, float temperature, float density, float life, float size, byte kind) {
        Particle p = new Particle();
        p.x = (float)position.f_82479_;
        p.y = (float)position.f_82480_;
        p.z = (float)position.f_82481_;
        p.vx = (float)velocity.f_82479_;
        p.vy = (float)velocity.f_82480_;
        p.vz = (float)velocity.f_82481_;
        p.temperature = temperature;
        p.density = density;
        p.life = life;
        p.baseSize = size;
        p.size = size;
        p.kind = kind;
        p.seed = this.random.nextFloat() * 100.0f;
        p.alpha = 1.0f;
        this.particles.add(p);
    }

    private Vec3 randomDirection(boolean upward) {
        double x = this.random.nextGaussian();
        double y = this.random.nextGaussian();
        double z = this.random.nextGaussian();
        if (upward) {
            y = Math.abs(y) * 0.82 + 0.08;
        }
        return new Vec3(x, y, z).m_82541_();
    }

    private Vec3 dustDirection() {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        return new Vec3(Math.cos(angle), (double)this.randomRange(0.03f, 0.24f), Math.sin(angle)).m_82541_();
    }

    private float randomRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private static float[] blackbody(float kelvin) {
        float g;
        float t = Mth.m_14036_((float)kelvin, (float)1000.0f, (float)40000.0f) / 100.0f;
        float safeHot = Math.max(t - 60.0f, 0.001f);
        float r = t <= 66.0f ? 255.0f : 329.69873f * (float)Math.pow(safeHot, -0.1332047592);
        float f = g = t <= 66.0f ? 99.4708f * (float)Math.log(Math.max(t, 1.0E-4f)) - 161.11957f : 288.12216f * (float)Math.pow(safeHot, -0.0755148492);
        float b = t >= 66.0f ? 255.0f : (t <= 19.0f ? 0.0f : 138.51773f * (float)Math.log(Math.max(t - 10.0f, 1.0E-4f)) - 305.0448f);
        return new float[]{Mth.m_14036_((float)(r / 255.0f), (float)0.0f, (float)1.0f), Mth.m_14036_((float)(g / 255.0f), (float)0.0f, (float)1.0f), Mth.m_14036_((float)(b / 255.0f), (float)0.0f, (float)1.0f)};
    }

    static final class Particle {
        float x;
        float y;
        float z;
        float vx;
        float vy;
        float vz;
        float temperature;
        float density;
        float age;
        float life;
        float baseSize;
        float size;
        float seed;
        float r;
        float g;
        float b;
        float alpha;
        int frame;
        byte kind;

        Particle() {
        }

        boolean emissive() {
            return this.kind == 0 || this.kind == 2 || this.temperature > 1450.0f;
        }

        double distanceToSqr(Vec3 camera) {
            double dx = (double)this.x - camera.f_82479_;
            double dy = (double)this.y - camera.f_82480_;
            double dz = (double)this.z - camera.f_82481_;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}

