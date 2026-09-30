/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionCollisionCache;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionFluidGrid;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionParticleSystem;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionProfile;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionRenderSnapshot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public final class ExplosionWorld {
    private static final ExplosionWorld INSTANCE = new ExplosionWorld();
    private final ExplosionFluidGrid fluid = new ExplosionFluidGrid();
    private final ExplosionParticleSystem particles = new ExplosionParticleSystem();
    private final ExplosionCollisionCache collision = new ExplosionCollisionCache();
    private final List<Source> sources = new ArrayList<Source>();
    private final ConcurrentLinkedQueue<SpawnRequest> spawnQueue = new ConcurrentLinkedQueue();
    private final ExecutorService simulationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Luxium-ExplosionSimulation");
        thread.setDaemon(true);
        thread.setPriority(5);
        return thread;
    });
    private final AtomicBoolean simulationQueued = new AtomicBoolean();
    private final AtomicLong pendingStepNanos = new AtomicLong();
    private final AtomicReference<ExplosionRenderSnapshot> publishedSnapshot = new AtomicReference<ExplosionRenderSnapshot>(new ExplosionRenderSnapshot());
    private ExplosionRenderSnapshot writeSnapshot = new ExplosionRenderSnapshot();
    private ClientLevel level;
    private float simulationTime;
    private float fluidAccumulator;

    private ExplosionWorld() {
    }

    public static ExplosionWorld get() {
        return INSTANCE;
    }

    public void trigger(Vec3 center, int power) {
        ClientLevel currentLevel = Minecraft.m_91087_().f_91073_;
        if (currentLevel == null) {
            return;
        }
        ExplosionProfile profile = ExplosionProfile.fromPower(power);
        Vec3 actual = new Vec3(center.f_82479_, Math.max(center.f_82480_, (double)currentLevel.m_141937_() + 0.72), center.f_82481_);
        this.collision.rebuild(currentLevel, actual);
        this.spawnQueue.add(new SpawnRequest(currentLevel, actual, profile));
        this.requestStep(0.0f);
    }

    private void clearInternal() {
        this.fluid.reset();
        this.particles.clear();
        this.sources.clear();
        this.simulationTime = 0.0f;
        this.fluidAccumulator = 0.0f;
    }

    public void requestStep(float dt) {
        float safeDt = Math.max(0.0f, Math.min(dt, 0.05f));
        this.pendingStepNanos.addAndGet((long)(safeDt * 1.0E9f));
        this.scheduleSimulation();
    }

    ExplosionRenderSnapshot snapshot() {
        return this.publishedSnapshot.get();
    }

    private void stepInternal(float dt) {
        SpawnRequest request;
        while ((request = this.spawnQueue.poll()) != null) {
            int limit;
            if (this.level != request.level) {
                this.clearInternal();
                this.level = request.level;
            }
            this.fluid.inject(this.collision, request.center, request.profile);
            this.particles.trigger(request.center, request.profile, true);
            this.sources.add(new Source(request.center, request.profile));
            int n = limit = request.profile.tntKg() >= 100.0 ? 2 : 4;
            while (this.sources.size() > limit) {
                this.sources.remove(0);
            }
        }
        if (this.level == null || dt <= 0.0f) {
            return;
        }
        this.simulationTime += dt;
        Iterator<Source> iterator = this.sources.iterator();
        while (iterator.hasNext()) {
            Source source = iterator.next();
            source.age += dt;
            float duration = source.profile.durationScale();
            if (source.age < 1.05f * duration) {
                this.fluid.injectAfterburn(source.center, source.profile, source.age);
                double decay = Math.exp(-source.age * (1.7f / duration));
                source.emissionAccumulator += (double)(dt * 1450.0f * source.profile.afterburnMultiplier()) * decay;
                int spawn = (int)source.emissionAccumulator;
                if (spawn > 0) {
                    source.emissionAccumulator -= (double)spawn;
                    this.particles.spawnAfterburn(source.center, source.profile, spawn);
                }
            }
            if (!(source.age >= 1.2f * duration)) continue;
            iterator.remove();
        }
        this.fluidAccumulator += dt;
        for (int iterations = 0; this.fluidAccumulator >= 0.033333335f && iterations < 2; ++iterations) {
            this.fluid.step(0.033333335f);
            this.fluidAccumulator -= 0.033333335f;
        }
        this.particles.step(dt, this.fluid, this.collision, this.simulationTime);
    }

    private void scheduleSimulation() {
        if (!this.simulationQueued.compareAndSet(false, true)) {
            return;
        }
        this.simulationExecutor.execute(() -> {
            try {
                do {
                    long nanos = this.pendingStepNanos.getAndSet(0L);
                    this.stepInternal(Math.min((float)nanos / 1.0E9f, 0.05f));
                    this.publishSnapshot();
                } while (this.pendingStepNanos.get() > 0L || !this.spawnQueue.isEmpty());
            }
            finally {
                this.simulationQueued.set(false);
                if (this.pendingStepNanos.get() > 0L || !this.spawnQueue.isEmpty()) {
                    this.scheduleSimulation();
                }
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void publishSnapshot() {
        ExplosionRenderSnapshot explosionRenderSnapshot = this.writeSnapshot;
        synchronized (explosionRenderSnapshot) {
            ExplosionRenderSnapshot previousRead;
            this.particles.writeSnapshot(this.writeSnapshot);
            this.writeSnapshot = previousRead = this.publishedSnapshot.getAndSet(this.writeSnapshot);
        }
    }

    private record SpawnRequest(ClientLevel level, Vec3 center, ExplosionProfile profile) {
    }

    private static final class Source {
        final Vec3 center;
        final ExplosionProfile profile;
        float age;
        double emissionAccumulator;

        Source(Vec3 center, ExplosionProfile profile) {
            this.center = center;
            this.profile = profile;
        }
    }
}

