/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.vinlanx.luxium.Testing.ExplosionTesting;

import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionCollisionCache;
import com.vinlanx.luxium.Testing.ExplosionTesting.ExplosionProfile;
import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ExplosionFluidGrid {
    static final int NX = 22;
    static final int NY = 19;
    static final int NZ = 22;
    private static final int COUNT = 9196;
    private final float[] vx = new float[9196];
    private final float[] vy = new float[9196];
    private final float[] vz = new float[9196];
    private final float[] temperature = new float[9196];
    private final float[] smoke = new float[9196];
    private final float[] fuel = new float[9196];
    private final float[] expansion = new float[9196];
    private final float[] pressure = new float[9196];
    private final boolean[] solid = new boolean[9196];
    private final float[] scratch = new float[9196];
    private final float[] advectVx = new float[9196];
    private final float[] advectVy = new float[9196];
    private final float[] advectVz = new float[9196];
    private final float[] curlX = new float[9196];
    private final float[] curlY = new float[9196];
    private final float[] curlZ = new float[9196];
    private final float[] curlMagnitude = new float[9196];
    private final float[] nextPressure = new float[9196];
    private double minX;
    private double minY;
    private double minZ;
    private boolean initialized;

    ExplosionFluidGrid() {
    }

    void reset() {
        Arrays.fill(this.vx, 0.0f);
        Arrays.fill(this.vy, 0.0f);
        Arrays.fill(this.vz, 0.0f);
        Arrays.fill(this.temperature, 0.0f);
        Arrays.fill(this.smoke, 0.0f);
        Arrays.fill(this.fuel, 0.0f);
        Arrays.fill(this.expansion, 0.0f);
        Arrays.fill(this.pressure, 0.0f);
        this.initialized = false;
    }

    void centerOn(ExplosionCollisionCache collision, Vec3 center) {
        this.minX = center.f_82479_ - 11.0;
        this.minY = center.f_82480_ - 1.0;
        this.minZ = center.f_82481_ - 11.0;
        this.rebuildSolids(collision);
        this.initialized = true;
    }

    void inject(ExplosionCollisionCache collision, Vec3 center, ExplosionProfile profile) {
        if (!this.initialized || center.m_82531_(this.minX + 11.0, this.minY + 9.5, this.minZ + 11.0) > 64.0) {
            this.reset();
            this.centerOn(collision, center);
        }
        float radius = profile.coreRadius();
        float impulse = profile.blastStrength();
        boolean ground = center.f_82480_ < Math.max(1.5, (double)radius * 0.42);
        for (int y = 1; y < 18; ++y) {
            for (int z = 1; z < 21; ++z) {
                for (int x = 1; x < 21; ++x) {
                    int i = ExplosionFluidGrid.index(x, y, z);
                    if (this.solid[i]) continue;
                    double px = this.minX + (double)x + 0.5;
                    double py = this.minY + (double)y + 0.5;
                    double pz = this.minZ + (double)z + 0.5;
                    float ox = (float)(px - center.f_82479_);
                    float oy = (float)(py - center.f_82480_);
                    float oz = (float)(pz - center.f_82481_);
                    float distance = (float)Math.sqrt(ox * ox + oy * oy + oz * oz);
                    float inv = 1.0f / Math.max(distance, 0.08f);
                    ox *= inv;
                    oy *= inv;
                    oz *= inv;
                    if (ground) {
                        oy = Math.abs(oy) * 0.78f + 0.12f;
                        float norm = Mth.m_264536_((float)(ox * ox + oy * oy + oz * oz));
                        ox *= norm;
                        oy *= norm;
                        oz *= norm;
                    }
                    float q = distance / radius;
                    float weight = (float)Math.exp(-(q * q) * 1.65f);
                    int n = i;
                    this.vx[n] = this.vx[n] + ox * weight * impulse;
                    int n2 = i;
                    this.vy[n2] = this.vy[n2] + (oy * weight * impulse + weight * impulse * 0.24f);
                    int n3 = i;
                    this.vz[n3] = this.vz[n3] + oz * weight * impulse;
                    int n4 = i;
                    this.temperature[n4] = this.temperature[n4] + weight * 2.8f * profile.heatMultiplier();
                    int n5 = i;
                    this.fuel[n5] = this.fuel[n5] + weight * 1.35f * profile.heatMultiplier();
                    int n6 = i;
                    this.smoke[n6] = this.smoke[n6] + weight * (0.16f + 0.025f * profile.scale());
                    int n7 = i;
                    this.expansion[n7] = this.expansion[n7] + weight * 3.0f * (float)Math.pow(profile.scale(), 0.24);
                }
            }
        }
        this.applySolidBoundary();
    }

    void injectAfterburn(Vec3 center, ExplosionProfile profile, float age) {
        if (!this.initialized) {
            return;
        }
        float radius = profile.coreRadius() * 0.56f + age * (0.28f + 0.1f * profile.scale());
        float pulse = (float)Math.exp(-age * (3.0f / profile.durationScale()));
        float strength = profile.blastStrength();
        for (int y = 1; y < 18; ++y) {
            for (int z = 1; z < 21; ++z) {
                for (int x = 1; x < 21; ++x) {
                    int i = ExplosionFluidGrid.index(x, y, z);
                    if (this.solid[i]) continue;
                    float ox = (float)(this.minX + (double)x + 0.5 - center.f_82479_);
                    float oy = (float)(this.minY + (double)y + 0.5 - center.f_82480_);
                    float oz = (float)(this.minZ + (double)z + 0.5 - center.f_82481_);
                    float distance = (float)Math.sqrt(ox * ox + oy * oy + oz * oz);
                    float q = distance / radius;
                    float weight = (float)Math.exp(-(q * q) * 2.1f);
                    int n = i;
                    this.temperature[n] = this.temperature[n] + weight * pulse * 0.13f * strength * profile.heatMultiplier();
                    int n2 = i;
                    this.fuel[n2] = this.fuel[n2] + weight * pulse * 0.045f * strength * profile.heatMultiplier();
                    int n3 = i;
                    this.expansion[n3] = this.expansion[n3] + weight * pulse * 0.06f * strength * (float)Math.pow(profile.scale(), 0.22);
                    int n4 = i;
                    this.vy[n4] = this.vy[n4] + weight * pulse * 0.12f * strength;
                }
            }
        }
    }

    void step(float dt) {
        if (!this.initialized) {
            return;
        }
        if ((dt = Mth.m_14036_((float)dt, (float)0.0f, (float)0.04f)) <= 0.0f) {
            return;
        }
        System.arraycopy(this.vx, 0, this.advectVx, 0, 9196);
        System.arraycopy(this.vy, 0, this.advectVy, 0, 9196);
        System.arraycopy(this.vz, 0, this.advectVz, 0, 9196);
        this.advect(this.vx, dt);
        this.advect(this.vy, dt);
        this.advect(this.vz, dt);
        this.advect(this.temperature, dt);
        this.advect(this.smoke, dt);
        this.advect(this.fuel, dt);
        this.advect(this.expansion, dt);
        for (int i = 0; i < 9196; ++i) {
            if (this.solid[i]) continue;
            float ignition = Mth.m_14036_((float)((this.temperature[i] - 0.32f) * 2.4f), (float)0.0f, (float)1.0f);
            float burn = Math.min(this.fuel[i], this.fuel[i] * ignition * dt * 3.4f);
            int n = i;
            this.fuel[n] = this.fuel[n] - burn;
            int n2 = i;
            this.temperature[n2] = this.temperature[n2] + burn * 2.5f;
            int n3 = i;
            this.smoke[n3] = this.smoke[n3] + burn * 0.82f;
            int n4 = i;
            this.expansion[n4] = this.expansion[n4] + burn * 2.2f;
            int n5 = i;
            this.vy[n5] = this.vy[n5] + dt * (this.temperature[i] * 3.0f - this.smoke[i] * 0.24f);
        }
        this.vorticity(dt, 2.3f);
        this.projectPressure();
        float velocityDecay = (float)Math.exp(-dt * 0.18f);
        float heatDecay = (float)Math.exp(-dt * 0.78f);
        float smokeDecay = (float)Math.exp(-dt * 0.075f);
        float fuelDecay = (float)Math.exp(-dt * 0.12f);
        float expansionDecay = (float)Math.exp(-dt * 4.2f);
        int i = 0;
        while (i < 9196) {
            int n = i;
            this.vx[n] = this.vx[n] * velocityDecay;
            int n6 = i;
            this.vy[n6] = this.vy[n6] * velocityDecay;
            int n7 = i;
            this.vz[n7] = this.vz[n7] * velocityDecay;
            this.temperature[i] = Mth.m_14036_((float)(this.temperature[i] * heatDecay), (float)0.0f, (float)5.0f);
            this.smoke[i] = Mth.m_14036_((float)(this.smoke[i] * smokeDecay), (float)0.0f, (float)4.0f);
            this.fuel[i] = Mth.m_14036_((float)(this.fuel[i] * fuelDecay), (float)0.0f, (float)3.0f);
            int n8 = i++;
            this.expansion[n8] = this.expansion[n8] * expansionDecay;
        }
        this.applySolidBoundary();
    }

    Vec3 sampleVelocity(double x, double y, double z) {
        return new Vec3((double)this.sample(this.vx, x, y, z), (double)this.sample(this.vy, x, y, z), (double)this.sample(this.vz, x, y, z));
    }

    float sampleTemperature(double x, double y, double z) {
        return this.sample(this.temperature, x, y, z);
    }

    private void rebuildSolids(ExplosionCollisionCache collision) {
        for (int y = 0; y < 19; ++y) {
            for (int z = 0; z < 22; ++z) {
                for (int x = 0; x < 22; ++x) {
                    int i = ExplosionFluidGrid.index(x, y, z);
                    boolean boundary = x == 0 || x == 21 || y == 0 || y == 18 || z == 0 || z == 21;
                    this.solid[i] = boundary || collision.isSolid(this.minX + (double)x + 0.5, this.minY + (double)y + 0.5, this.minZ + (double)z + 0.5);
                }
            }
        }
    }

    private void advect(float[] field, float dt) {
        for (int y = 0; y < 19; ++y) {
            for (int z = 0; z < 22; ++z) {
                for (int x = 0; x < 22; ++x) {
                    int i = ExplosionFluidGrid.index(x, y, z);
                    double px = this.minX + (double)x + 0.5 - (double)(this.advectVx[i] * dt);
                    double py = this.minY + (double)y + 0.5 - (double)(this.advectVy[i] * dt);
                    double pz = this.minZ + (double)z + 0.5 - (double)(this.advectVz[i] * dt);
                    this.scratch[i] = this.sample(field, px, py, pz);
                }
            }
        }
        System.arraycopy(this.scratch, 0, field, 0, 9196);
    }

    private void vorticity(float dt, float strength) {
        int i;
        int x;
        int z;
        int y;
        Arrays.fill(this.curlX, 0.0f);
        Arrays.fill(this.curlY, 0.0f);
        Arrays.fill(this.curlZ, 0.0f);
        Arrays.fill(this.curlMagnitude, 0.0f);
        for (y = 1; y < 18; ++y) {
            for (z = 1; z < 21; ++z) {
                for (x = 1; x < 21; ++x) {
                    i = ExplosionFluidGrid.index(x, y, z);
                    this.curlX[i] = (this.vz[ExplosionFluidGrid.index(x, y + 1, z)] - this.vz[ExplosionFluidGrid.index(x, y - 1, z)]) * 0.5f - (this.vy[ExplosionFluidGrid.index(x, y, z + 1)] - this.vy[ExplosionFluidGrid.index(x, y, z - 1)]) * 0.5f;
                    this.curlY[i] = (this.vx[ExplosionFluidGrid.index(x, y, z + 1)] - this.vx[ExplosionFluidGrid.index(x, y, z - 1)]) * 0.5f - (this.vz[ExplosionFluidGrid.index(x + 1, y, z)] - this.vz[ExplosionFluidGrid.index(x - 1, y, z)]) * 0.5f;
                    this.curlZ[i] = (this.vy[ExplosionFluidGrid.index(x + 1, y, z)] - this.vy[ExplosionFluidGrid.index(x - 1, y, z)]) * 0.5f - (this.vx[ExplosionFluidGrid.index(x, y + 1, z)] - this.vx[ExplosionFluidGrid.index(x, y - 1, z)]) * 0.5f;
                    this.curlMagnitude[i] = (float)Math.sqrt(this.curlX[i] * this.curlX[i] + this.curlY[i] * this.curlY[i] + this.curlZ[i] * this.curlZ[i]);
                }
            }
        }
        for (y = 2; y < 17; ++y) {
            for (z = 2; z < 20; ++z) {
                for (x = 2; x < 20; ++x) {
                    i = ExplosionFluidGrid.index(x, y, z);
                    float nx = (this.curlMagnitude[ExplosionFluidGrid.index(x + 1, y, z)] - this.curlMagnitude[ExplosionFluidGrid.index(x - 1, y, z)]) * 0.5f;
                    float ny = (this.curlMagnitude[ExplosionFluidGrid.index(x, y + 1, z)] - this.curlMagnitude[ExplosionFluidGrid.index(x, y - 1, z)]) * 0.5f;
                    float nz = (this.curlMagnitude[ExplosionFluidGrid.index(x, y, z + 1)] - this.curlMagnitude[ExplosionFluidGrid.index(x, y, z - 1)]) * 0.5f;
                    float inv = Mth.m_264536_((float)(nx * nx + ny * ny + nz * nz + 1.0E-5f));
                    int n = i;
                    this.vx[n] = this.vx[n] + ((ny *= inv) * this.curlZ[i] - (nz *= inv) * this.curlY[i]) * dt * strength;
                    int n2 = i;
                    this.vy[n2] = this.vy[n2] + (nz * this.curlX[i] - (nx *= inv) * this.curlZ[i]) * dt * strength;
                    int n3 = i;
                    this.vz[n3] = this.vz[n3] + (nx * this.curlY[i] - ny * this.curlX[i]) * dt * strength;
                }
            }
        }
    }

    private void projectPressure() {
        int i;
        int x;
        int z;
        int y;
        Arrays.fill(this.scratch, 0.0f);
        for (y = 1; y < 18; ++y) {
            for (z = 1; z < 21; ++z) {
                for (x = 1; x < 21; ++x) {
                    i = ExplosionFluidGrid.index(x, y, z);
                    this.scratch[i] = (this.vx[ExplosionFluidGrid.index(x + 1, y, z)] - this.vx[ExplosionFluidGrid.index(x - 1, y, z)] + this.vy[ExplosionFluidGrid.index(x, y + 1, z)] - this.vy[ExplosionFluidGrid.index(x, y - 1, z)] + this.vz[ExplosionFluidGrid.index(x, y, z + 1)] - this.vz[ExplosionFluidGrid.index(x, y, z - 1)]) * 0.5f - this.expansion[i];
                }
            }
        }
        Arrays.fill(this.pressure, 0.0f);
        for (int iteration = 0; iteration < 7; ++iteration) {
            Arrays.fill(this.nextPressure, 0.0f);
            for (int y2 = 1; y2 < 18; ++y2) {
                for (int z2 = 1; z2 < 21; ++z2) {
                    for (int x2 = 1; x2 < 21; ++x2) {
                        int i2 = ExplosionFluidGrid.index(x2, y2, z2);
                        if (this.solid[i2]) continue;
                        this.nextPressure[i2] = (this.pressure[ExplosionFluidGrid.index(x2 + 1, y2, z2)] + this.pressure[ExplosionFluidGrid.index(x2 - 1, y2, z2)] + this.pressure[ExplosionFluidGrid.index(x2, y2 + 1, z2)] + this.pressure[ExplosionFluidGrid.index(x2, y2 - 1, z2)] + this.pressure[ExplosionFluidGrid.index(x2, y2, z2 + 1)] + this.pressure[ExplosionFluidGrid.index(x2, y2, z2 - 1)] - this.scratch[i2]) / 6.0f;
                    }
                }
            }
            System.arraycopy(this.nextPressure, 0, this.pressure, 0, 9196);
        }
        for (y = 1; y < 18; ++y) {
            for (z = 1; z < 21; ++z) {
                for (x = 1; x < 21; ++x) {
                    int n = i = ExplosionFluidGrid.index(x, y, z);
                    this.vx[n] = this.vx[n] - (this.pressure[ExplosionFluidGrid.index(x + 1, y, z)] - this.pressure[ExplosionFluidGrid.index(x - 1, y, z)]) * 0.5f;
                    int n2 = i;
                    this.vy[n2] = this.vy[n2] - (this.pressure[ExplosionFluidGrid.index(x, y + 1, z)] - this.pressure[ExplosionFluidGrid.index(x, y - 1, z)]) * 0.5f;
                    int n3 = i;
                    this.vz[n3] = this.vz[n3] - (this.pressure[ExplosionFluidGrid.index(x, y, z + 1)] - this.pressure[ExplosionFluidGrid.index(x, y, z - 1)]) * 0.5f;
                }
            }
        }
    }

    private float sample(float[] field, double worldX, double worldY, double worldZ) {
        float gx = Mth.m_14036_((float)((float)(worldX - this.minX - 0.5)), (float)0.0f, (float)20.999f);
        float gy = Mth.m_14036_((float)((float)(worldY - this.minY - 0.5)), (float)0.0f, (float)17.999f);
        float gz = Mth.m_14036_((float)((float)(worldZ - this.minZ - 0.5)), (float)0.0f, (float)20.999f);
        int x0 = Mth.m_14143_((float)gx);
        int y0 = Mth.m_14143_((float)gy);
        int z0 = Mth.m_14143_((float)gz);
        int x1 = Math.min(x0 + 1, 21);
        int y1 = Math.min(y0 + 1, 18);
        int z1 = Math.min(z0 + 1, 21);
        float fx = gx - (float)x0;
        float fy = gy - (float)y0;
        float fz = gz - (float)z0;
        float c00 = Mth.m_14179_((float)fx, (float)field[ExplosionFluidGrid.index(x0, y0, z0)], (float)field[ExplosionFluidGrid.index(x1, y0, z0)]);
        float c10 = Mth.m_14179_((float)fx, (float)field[ExplosionFluidGrid.index(x0, y1, z0)], (float)field[ExplosionFluidGrid.index(x1, y1, z0)]);
        float c01 = Mth.m_14179_((float)fx, (float)field[ExplosionFluidGrid.index(x0, y0, z1)], (float)field[ExplosionFluidGrid.index(x1, y0, z1)]);
        float c11 = Mth.m_14179_((float)fx, (float)field[ExplosionFluidGrid.index(x0, y1, z1)], (float)field[ExplosionFluidGrid.index(x1, y1, z1)]);
        return Mth.m_14179_((float)fz, (float)Mth.m_14179_((float)fy, (float)c00, (float)c10), (float)Mth.m_14179_((float)fy, (float)c01, (float)c11));
    }

    private void applySolidBoundary() {
        for (int i = 0; i < 9196; ++i) {
            if (!this.solid[i]) continue;
            this.expansion[i] = 0.0f;
            this.fuel[i] = 0.0f;
            this.smoke[i] = 0.0f;
            this.temperature[i] = 0.0f;
            this.vz[i] = 0.0f;
            this.vy[i] = 0.0f;
            this.vx[i] = 0.0f;
        }
    }

    private static int index(int x, int y, int z) {
        return (y * 22 + z) * 22 + x;
    }
}

