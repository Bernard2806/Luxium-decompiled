/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.rtx.FloodRtSettings;
import com.vinlanx.luxium.rtx.LightRtMath;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

final class NeoFloodRtEngine {
    static final int VOLUME_RADIUS = 36;
    static final int VOLUME_SIDE = 73;
    static final int VOLUME_PLANE = 5329;
    static final int VOLUME_SIZE = 389017;
    private static final int MAX_POOLED_VISIBILITY_VOLUMES = 32;
    private static final ConcurrentLinkedQueue<byte[]> VISIBILITY_VOLUME_POOL = new ConcurrentLinkedQueue();
    private static final AtomicInteger VISIBILITY_VOLUME_POOL_SIZE = new AtomicInteger();
    private static final double HALF_BLOCK_DIAGONAL = 0.8660254037844386;
    private static final int MAX_DISTANCE_SQ = 3889;
    private static final int OPACITY_SOLID = 255;
    private static final float MIN_VISIBILITY = 0.007843138f;
    private static final int PROJECTION_TABLE_SIDE = 37;
    private static final int PROJECTION_TABLE_SIZE = 1369;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private NeoFloodRtEngine() {
    }

    static Result solve(long sourceKey, int lightLevel, ClientLevel level) {
        Scratch scratch = SCRATCH.get();
        scratch.reset();
        scratch.refreshTables(lightLevel);
        int originX = BlockPos.m_121983_((long)sourceKey);
        int originY = BlockPos.m_122008_((long)sourceKey);
        int originZ = BlockPos.m_122015_((long)sourceKey);
        double maxDistance = LightRtMath.getMaxDistance(lightLevel);
        int radius = Math.min(36, Math.min(FloodRtSettings.radiusCap(), (int)Math.ceil(maxDistance)));
        byte[] visibilityVolume = NeoFloodRtEngine.borrowVisibilityVolume();
        if (radius <= 0) {
            return NeoFloodRtEngine.finish(scratch, visibilityVolume, false);
        }
        boolean complete = NeoFloodRtEngine.fillOpacity(level, originX, originY, originZ, radius, scratch);
        NeoFloodRtEngine.propagate(scratch, visibilityVolume, radius);
        return NeoFloodRtEngine.finish(scratch, visibilityVolume, !complete);
    }

    static byte[] borrowVisibilityVolume() {
        byte[] volume = VISIBILITY_VOLUME_POOL.poll();
        if (volume != null) {
            VISIBILITY_VOLUME_POOL_SIZE.decrementAndGet();
            Arrays.fill(volume, (byte)0);
            return volume;
        }
        return new byte[389017];
    }

    static void recycleVisibilityVolume(byte[] volume) {
        int size;
        if (volume == null || volume.length != 389017) {
            return;
        }
        do {
            if ((size = VISIBILITY_VOLUME_POOL_SIZE.get()) < 32) continue;
            return;
        } while (!VISIBILITY_VOLUME_POOL_SIZE.compareAndSet(size, size + 1));
        VISIBILITY_VOLUME_POOL.offer(volume);
    }

    private static boolean fillOpacity(ClientLevel level, int originX, int originY, int originZ, int radius, Scratch scratch) {
        byte[] opacity = scratch.opacity;
        Arrays.fill(opacity, (byte)0);
        int minX = originX - radius;
        int maxX = originX + radius;
        int minY = originY - radius;
        int maxY = originY + radius;
        int minZ = originZ - radius;
        int maxZ = originZ + radius;
        int minSectionX = SectionPos.m_123171_((int)minX);
        int maxSectionX = SectionPos.m_123171_((int)maxX);
        int minSectionY = SectionPos.m_123171_((int)minY);
        int maxSectionY = SectionPos.m_123171_((int)maxY);
        int minSectionZ = SectionPos.m_123171_((int)minZ);
        int maxSectionZ = SectionPos.m_123171_((int)maxZ);
        int minBuildHeight = level.m_141937_();
        boolean complete = true;
        for (int sectionX = minSectionX; sectionX <= maxSectionX; ++sectionX) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; ++sectionZ) {
                LevelChunk chunk = level.m_7726_().m_7131_(sectionX, sectionZ);
                LevelChunkSection[] sections = chunk != null ? chunk.m_7103_() : null;
                int chunkMinX = Math.max(minX, SectionPos.m_123223_((int)sectionX));
                int chunkMaxX = Math.min(maxX, SectionPos.m_123223_((int)sectionX) + 15);
                int chunkMinZ = Math.max(minZ, SectionPos.m_123223_((int)sectionZ));
                int chunkMaxZ = Math.min(maxZ, SectionPos.m_123223_((int)sectionZ) + 15);
                for (int sectionY = minSectionY; sectionY <= maxSectionY; ++sectionY) {
                    boolean hasOccludersOrEmitters;
                    int chunkMaxY;
                    int sectionBaseY = SectionPos.m_123223_((int)sectionY);
                    int chunkMinY = Math.max(minY, sectionBaseY);
                    if (chunkMinY > (chunkMaxY = Math.min(maxY, sectionBaseY + 15))) continue;
                    LevelChunkSection section = null;
                    if (sections != null) {
                        int sectionIndex = sectionBaseY - minBuildHeight >> 4;
                        if (sectionIndex < 0 || sectionIndex >= sections.length) continue;
                        section = sections[sectionIndex];
                    }
                    if (chunk == null) {
                        complete = false;
                        NeoFloodRtEngine.fillSolidRange(opacity, originX, originY, originZ, radius, chunkMinX, chunkMaxX, chunkMinY, chunkMaxY, chunkMinZ, chunkMaxZ);
                        continue;
                    }
                    if (section == null || section.m_188008_() || !(hasOccludersOrEmitters = section.m_63019_().m_63109_(state -> state.m_60815_() || state.m_60791_() > 0))) continue;
                    NeoFloodRtEngine.fillSectionRange(opacity, section, originX, originY, originZ, radius, chunkMinX, chunkMaxX, chunkMinY, chunkMaxY, chunkMinZ, chunkMaxZ, scratch);
                }
            }
        }
        opacity[NeoFloodRtEngine.volumeIndex((int)0, (int)0, (int)0)] = 0;
        return complete;
    }

    private static void fillSolidRange(byte[] opacity, int originX, int originY, int originZ, int radius, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int radiusSq = radius * radius;
        for (int y = minY; y <= maxY; ++y) {
            int dy = y - originY;
            int dy2 = dy * dy;
            if (dy2 > radiusSq) continue;
            int baseY = (dy + 36) * 5329;
            for (int z = minZ; z <= maxZ; ++z) {
                int dz = z - originZ;
                int dyz2 = dy2 + dz * dz;
                if (dyz2 > radiusSq) continue;
                int baseYZ = baseY + (dz + 36) * 73;
                for (int x = minX; x <= maxX; ++x) {
                    int dx = x - originX;
                    if (dyz2 + dx * dx > radiusSq) continue;
                    opacity[baseYZ + (dx + 36)] = -1;
                }
            }
        }
    }

    private static void fillSectionRange(byte[] opacity, LevelChunkSection section, int originX, int originY, int originZ, int radius, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Scratch scratch) {
        int radiusSq = radius * radius;
        BlockState cachedState = scratch.cachedState;
        int cachedOpacity = scratch.cachedOpacity;
        for (int y = minY; y <= maxY; ++y) {
            int dy = y - originY;
            int dy2 = dy * dy;
            if (dy2 > radiusSq) continue;
            int localY = y & 0xF;
            int baseY = (dy + 36) * 5329;
            for (int z = minZ; z <= maxZ; ++z) {
                int dz = z - originZ;
                int dyz2 = dy2 + dz * dz;
                if (dyz2 > radiusSq) continue;
                int localZ = z & 0xF;
                int baseYZ = baseY + (dz + 36) * 73;
                for (int x = minX; x <= maxX; ++x) {
                    int dx = x - originX;
                    if (dyz2 + dx * dx > radiusSq) continue;
                    BlockState state = section.m_62982_(x & 0xF, localY, localZ);
                    if (state != cachedState) {
                        cachedState = state;
                        cachedOpacity = NeoFloodRtEngine.computeOpacity(state);
                    }
                    if (cachedOpacity == 0) continue;
                    opacity[baseYZ + (dx + 36)] = (byte)cachedOpacity;
                }
            }
        }
        scratch.cachedState = cachedState;
        scratch.cachedOpacity = cachedOpacity;
    }

    private static int computeOpacity(BlockState state) {
        if (state.m_60795_()) {
            return 0;
        }
        if (state.m_60791_() > 0) {
            return 0;
        }
        return state.m_60815_() ? 255 : 0;
    }

    private static void propagate(Scratch scratch, byte[] visibilityVolume, int radius) {
        float[] throughput = scratch.throughput;
        byte[] opacity = scratch.opacity;
        int[] brightnessLut = scratch.brightnessLut;
        float softness = FloodRtSettings.penumbraSoftness();
        float edgeSeal = Math.min(1.0f, FloodRtSettings.cornerSeal() * 0.25f);
        int radiusSq = radius * radius;
        int centerIndex = NeoFloodRtEngine.volumeIndex(0, 0, 0);
        throughput[centerIndex] = 1.0f;
        NeoFloodRtEngine.emit(scratch, visibilityVolume, centerIndex, 0, 1.0f, brightnessLut);
        for (int octant = 0; octant < 8; ++octant) {
            int ay2;
            int signX = (octant & 1) == 0 ? 1 : -1;
            int signY = (octant & 2) == 0 ? 1 : -1;
            int signZ = (octant & 4) == 0 ? 1 : -1;
            for (int ay = 0; ay <= radius && (ay2 = ay * ay) <= radiusSq; ++ay) {
                int ayz2;
                int dy = ay * signY;
                int baseY = (dy + 36) * 5329;
                for (int az = 0; az <= radius && (ayz2 = ay2 + az * az) <= radiusSq; ++az) {
                    int distanceSq;
                    int dz = az * signZ;
                    int baseYZ = baseY + (dz + 36) * 73;
                    for (int ax = 0; ax <= radius && (distanceSq = ayz2 + ax * ax) <= radiusSq; ++ax) {
                        int cellOpacity;
                        if (distanceSq == 0) continue;
                        int dx = ax * signX;
                        int index = baseYZ + (dx + 36);
                        float incident = NeoFloodRtEngine.sampleBackProjected(scratch, ax, ay, az, signX, signY, signZ, softness, edgeSeal);
                        if (incident <= 0.007843138f) {
                            throughput[index] = 0.0f;
                            continue;
                        }
                        if (incident > 1.0f) {
                            incident = 1.0f;
                        }
                        throughput[index] = (cellOpacity = opacity[index] & 0xFF) >= 255 ? 0.0f : (cellOpacity == 0 ? incident : incident * (1.0f - (float)cellOpacity * 0.003921569f));
                        NeoFloodRtEngine.emit(scratch, visibilityVolume, index, distanceSq, incident, brightnessLut);
                    }
                }
            }
        }
    }

    private static float sampleBackProjected(Scratch scratch, int ax, int ay, int az, int signX, int signY, int signZ, float softness, float edgeSeal) {
        float nearest;
        float smooth;
        float v000;
        int shell = Math.max(ax, Math.max(ay, az));
        int row = shell * 37;
        int lowX = scratch.projectionLow[row + ax] & 0xFF;
        int lowY = scratch.projectionLow[row + ay] & 0xFF;
        int lowZ = scratch.projectionLow[row + az] & 0xFF;
        float fracX = scratch.projectionFraction[row + ax];
        float fracY = scratch.projectionFraction[row + ay];
        float fracZ = scratch.projectionFraction[row + az];
        boolean nearHighX = (scratch.projectionNearest[row + ax] & 0xFF) != lowX;
        boolean nearHighY = (scratch.projectionNearest[row + ay] & 0xFF) != lowY;
        boolean nearHighZ = (scratch.projectionNearest[row + az] & 0xFF) != lowZ;
        int stepX = signX;
        int stepY = signY * 5329;
        int stepZ = signZ * 73;
        int base = NeoFloodRtEngine.volumeIndex(signX * lowX, signY * lowY, signZ * lowZ);
        float[] throughput = scratch.throughput;
        float minimum = v000 = throughput[base];
        float maximum = v000;
        if (fracX == 0.0f) {
            if (fracY == 0.0f) {
                if (fracZ == 0.0f) {
                    smooth = v000;
                    nearest = v000;
                } else {
                    v001 = throughput[base + stepZ];
                    minimum = Math.min(minimum, v001);
                    maximum = Math.max(maximum, v001);
                    smooth = NeoFloodRtEngine.lerp(v000, v001, fracZ);
                    nearest = nearHighZ ? v001 : v000;
                }
            } else if (fracZ == 0.0f) {
                float v010 = throughput[base + stepY];
                minimum = Math.min(minimum, v010);
                maximum = Math.max(maximum, v010);
                smooth = NeoFloodRtEngine.lerp(v000, v010, fracY);
                nearest = nearHighY ? v010 : v000;
            } else {
                v001 = throughput[base + stepZ];
                float v010 = throughput[base + stepY];
                float v011 = throughput[base + stepY + stepZ];
                minimum = Math.min(Math.min(v000, v001), Math.min(v010, v011));
                maximum = Math.max(Math.max(v000, v001), Math.max(v010, v011));
                smooth = NeoFloodRtEngine.bilerp(v000, v010, v001, v011, fracY, fracZ);
                nearest = nearHighY ? (nearHighZ ? v011 : v010) : (nearHighZ ? v001 : v000);
            }
        } else if (fracY == 0.0f) {
            if (fracZ == 0.0f) {
                v100 = throughput[base + stepX];
                minimum = Math.min(minimum, v100);
                maximum = Math.max(maximum, v100);
                smooth = NeoFloodRtEngine.lerp(v000, v100, fracX);
                nearest = nearHighX ? v100 : v000;
            } else {
                v100 = throughput[base + stepX];
                float v001 = throughput[base + stepZ];
                float v101 = throughput[base + stepX + stepZ];
                minimum = Math.min(Math.min(v000, v100), Math.min(v001, v101));
                maximum = Math.max(Math.max(v000, v100), Math.max(v001, v101));
                smooth = NeoFloodRtEngine.bilerp(v000, v100, v001, v101, fracX, fracZ);
                nearest = nearHighX ? (nearHighZ ? v101 : v100) : (nearHighZ ? v001 : v000);
            }
        } else {
            v100 = throughput[base + stepX];
            float v010 = throughput[base + stepY];
            float v110 = throughput[base + stepX + stepY];
            minimum = Math.min(Math.min(v000, v100), Math.min(v010, v110));
            maximum = Math.max(Math.max(v000, v100), Math.max(v010, v110));
            smooth = NeoFloodRtEngine.bilerp(v000, v100, v010, v110, fracX, fracY);
            nearest = nearHighX ? (nearHighY ? v110 : v100) : (nearHighY ? v010 : v000);
        }
        float filtered = smooth;
        if (edgeSeal > 0.0f && minimum <= 0.007843138f && maximum > 0.007843138f) {
            filtered = NeoFloodRtEngine.lerp(smooth, nearest, edgeSeal);
        }
        return NeoFloodRtEngine.lerp(nearest, filtered, softness);
    }

    private static float bilerp(float v00, float v10, float v01, float v11, float fx, float fy) {
        return NeoFloodRtEngine.lerp(NeoFloodRtEngine.lerp(v00, v10, fx), NeoFloodRtEngine.lerp(v01, v11, fx), fy);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void emit(Scratch scratch, byte[] visibilityVolume, int index, int distanceSq, float incident, int[] brightnessLut) {
        int coverage;
        int brightness = brightnessLut[distanceSq];
        if (brightness <= 0) {
            return;
        }
        int light = (int)((float)brightness * incident + 0.5f);
        if (light <= 0) {
            return;
        }
        if (light > 15) {
            light = 15;
        }
        if ((coverage = (int)(incident * 255.0f + 0.5f)) < 1) {
            coverage = 1;
        } else if (coverage > 255) {
            coverage = 255;
        }
        if (scratch.light[index] == 0) {
            int n = index >>> 6;
            scratch.touchedBits[n] = scratch.touchedBits[n] | 1L << (index & 0x3F);
            ++scratch.touchedCount;
        }
        if (light > (scratch.light[index] & 0xFF)) {
            scratch.light[index] = (byte)light;
        }
        if (coverage > (visibilityVolume[index] & 0xFF)) {
            visibilityVolume[index] = (byte)coverage;
        }
    }

    private static Result finish(Scratch scratch, byte[] visibilityVolume, boolean dataIncomplete) {
        int touchedCount = scratch.touchedCount;
        byte[] packedLight = new byte[194509];
        long[] touchedBits = Arrays.copyOf(scratch.touchedBits, scratch.touchedBits.length);
        for (int wordIndex = 0; wordIndex < touchedBits.length; ++wordIndex) {
            int index;
            for (long bits = touchedBits[wordIndex]; bits != 0L && (index = (wordIndex << 6) + Long.numberOfTrailingZeros(bits)) < 389017; bits &= bits - 1L) {
                int light = scratch.light[index] & 0xF;
                if (light == 0) continue;
                int packedIndex = index >>> 1;
                int current = packedLight[packedIndex] & 0xFF;
                packedLight[packedIndex] = (byte)((index & 1) == 0 ? current & 0xF0 | light : current & 0xF | light << 4);
            }
        }
        return new Result(packedLight, visibilityVolume, touchedBits, touchedCount, dataIncomplete);
    }

    private static int volumeIndex(int dx, int dy, int dz) {
        return (dy + 36) * 5329 + (dz + 36) * 73 + (dx + 36);
    }

    private static final class Scratch {
        final float[] throughput = new float[389017];
        final byte[] opacity = new byte[389017];
        final byte[] light = new byte[389017];
        final long[] touchedBits = new long[6079];
        int touchedCount;
        final byte[] projectionLow = new byte[1369];
        final byte[] projectionNearest = new byte[1369];
        final float[] projectionFraction = new float[1369];
        final int[] brightnessLut = new int[3889];
        int projectionRevision = -1;
        int brightnessLightLevel = -1;
        BlockState cachedState;
        int cachedOpacity;

        private Scratch() {
        }

        void reset() {
            for (int wordIndex = 0; wordIndex < this.touchedBits.length; ++wordIndex) {
                int index;
                for (long bits = this.touchedBits[wordIndex]; bits != 0L && (index = (wordIndex << 6) + Long.numberOfTrailingZeros(bits)) < 389017; bits &= bits - 1L) {
                    this.light[index] = 0;
                }
            }
            Arrays.fill(this.touchedBits, 0L);
            this.touchedCount = 0;
            this.cachedState = null;
            this.cachedOpacity = 0;
        }

        void refreshTables(int lightLevel) {
            int revision = FloodRtSettings.revision();
            if (revision == this.projectionRevision && lightLevel == this.brightnessLightLevel) {
                return;
            }
            if (revision != this.projectionRevision) {
                float sharpness = FloodRtSettings.directionalBias();
                for (int shell = 0; shell <= 36; ++shell) {
                    int row = shell * 37;
                    for (int axis = 0; axis <= 36; ++axis) {
                        int tableIndex = row + axis;
                        if (shell == 0 || axis == 0) {
                            this.projectionLow[tableIndex] = 0;
                            this.projectionNearest[tableIndex] = 0;
                            this.projectionFraction[tableIndex] = 0.0f;
                            continue;
                        }
                        if (axis >= shell) {
                            this.projectionLow[tableIndex] = (byte)(shell - 1);
                            this.projectionNearest[tableIndex] = (byte)(shell - 1);
                            this.projectionFraction[tableIndex] = 0.0f;
                            continue;
                        }
                        double projected = (double)axis * (((double)shell - 1.0) / (double)shell);
                        int low = (int)Math.floor(projected);
                        double rawFraction = projected - (double)low;
                        this.projectionLow[tableIndex] = (byte)low;
                        this.projectionNearest[tableIndex] = (byte)Math.floor(projected + 0.5);
                        this.projectionFraction[tableIndex] = Scratch.warpFraction(rawFraction, sharpness);
                    }
                }
                this.projectionRevision = revision;
            }
            if (lightLevel != this.brightnessLightLevel) {
                for (int distanceSq = 0; distanceSq < 3889; ++distanceSq) {
                    double distance = Math.sqrt(distanceSq);
                    double nearDistance = Math.max(0.0, distance - 0.8660254037844386);
                    this.brightnessLut[distanceSq] = LightRtMath.getFalloff(lightLevel, nearDistance);
                }
                this.brightnessLightLevel = lightLevel;
            }
        }

        private static float warpFraction(double fraction, float sharpness) {
            if (fraction <= 0.0) {
                return 0.0f;
            }
            if (fraction >= 1.0) {
                return 1.0f;
            }
            if (Math.abs(sharpness - 1.0f) <= 1.0E-4f) {
                return (float)fraction;
            }
            double left = Math.pow(fraction, sharpness);
            double right = Math.pow(1.0 - fraction, sharpness);
            return (float)(left / (left + right));
        }
    }

    static final class Result {
        final byte[] packedLight;
        final byte[] visibilityVolume;
        final long[] touchedBits;
        final int solvedCellCount;
        final boolean dataIncomplete;

        Result(byte[] packedLight, byte[] visibilityVolume, long[] touchedBits, int solvedCellCount, boolean dataIncomplete) {
            this.packedLight = packedLight;
            this.visibilityVolume = visibilityVolume;
            this.touchedBits = touchedBits;
            this.solvedCellCount = solvedCellCount;
            this.dataIncomplete = dataIncomplete;
        }
    }
}

