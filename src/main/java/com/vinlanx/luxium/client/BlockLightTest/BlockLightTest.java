/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.ChunkStatus
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.Vec3
 *  org.lwjgl.opengl.GL20
 */
package com.vinlanx.luxium.client.BlockLightTest;

import com.vinlanx.luxium.Config;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20;

public final class BlockLightTest {
    private static final int LIMIT = 12;
    private static final Map<Long, Emitter> SOURCES = new HashMap<Long, Emitter>();
    private static final Map<Long, Set<Long>> CHUNKS = new HashMap<Long, Set<Long>>();
    private static final Set<Long> SCANNED_CHUNKS = new HashSet<Long>();
    private static final Map<Integer, int[]> PROGRAMS = new HashMap<Integer, int[]>();
    private static final Emitter[] NEAR = new Emitter[12];
    private static final float[] DISTANCES = new float[12];
    private static final float[] LOCATIONS = new float[48];
    private static final float[] COLORS = new float[48];
    private static ClientLevel level;
    private static int size;
    private static boolean previouslyEnabled;

    private BlockLightTest() {
    }

    public static boolean enabled() {
        return Config.isFeatureEnabled(Config.CLIENT.blockLightTestEnabled);
    }

    public static void clearProgramCache() {
        PROGRAMS.clear();
    }

    public static void onChunk(ClientLevel current, LevelChunk chunk) {
        if (!BlockLightTest.enabled()) {
            return;
        }
        BlockLightTest.setLevel(current);
        int x = chunk.m_7697_().f_45578_;
        int z = chunk.m_7697_().f_45579_;
        BlockLightTest.removeChunk(x, z);
        long chunkKey = ChunkPos.m_45589_((int)x, (int)z);
        SCANNED_CHUNKS.add(chunkKey);
        HashSet members = new HashSet();
        chunk.m_284254_((pos, state) -> {
            int strength = state.getLightEmission((BlockGetter)current, pos);
            if (strength <= 0) {
                return;
            }
            long key = pos.m_121878_();
            SOURCES.put(key, BlockLightTest.create(pos, state, strength));
            members.add(key);
        });
        CHUNKS.put(chunkKey, members);
    }

    public static void removeChunk(int x, int z) {
        SCANNED_CHUNKS.remove(ChunkPos.m_45589_((int)x, (int)z));
        Set<Long> members = CHUNKS.remove(ChunkPos.m_45589_((int)x, (int)z));
        if (members != null) {
            for (long key : members) {
                SOURCES.remove(key);
            }
        }
    }

    public static void changed(ClientLevel current, BlockPos pos, BlockState state) {
        if (!BlockLightTest.enabled()) {
            return;
        }
        BlockLightTest.setLevel(current);
        long key = pos.m_121878_();
        int strength = state.getLightEmission((BlockGetter)current, pos);
        Set members = CHUNKS.computeIfAbsent(ChunkPos.m_151388_((BlockPos)pos), ignored -> new HashSet());
        if (strength <= 0) {
            SOURCES.remove(key);
            members.remove(key);
        } else {
            SOURCES.put(key, BlockLightTest.create(pos, state, strength));
            members.add(key);
        }
    }

    public static void tick(ClientLevel current) {
        BlockItem item;
        BlockState held;
        int light;
        if (!BlockLightTest.enabled()) {
            if (previouslyEnabled) {
                BlockLightTest.setLevel(null);
                PROGRAMS.clear();
            }
            previouslyEnabled = false;
            return;
        }
        boolean firstTick = !previouslyEnabled || level != current;
        previouslyEnabled = true;
        BlockLightTest.setLevel(current);
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ == null) {
            return;
        }
        if (firstTick) {
            PROGRAMS.clear();
        }
        int centerX = minecraft.f_91074_.m_20183_().m_123341_() >> 4;
        int centerZ = minecraft.f_91074_.m_20183_().m_123343_() >> 4;
        for (int radius = 0; radius <= 3; ++radius) {
            boolean scanned = false;
            block1: for (int dz = -radius; dz <= radius && !scanned; ++dz) {
                for (int dx = -radius; dx <= radius; ++dx) {
                    LevelChunk chunk;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (SCANNED_CHUNKS.contains(ChunkPos.m_45589_((int)x, (int)z)) || (chunk = current.m_7726_().m_7587_(x, z, ChunkStatus.f_62326_, false)) == null) continue;
                    BlockLightTest.onChunk(current, chunk);
                    scanned = true;
                    continue block1;
                }
            }
            if (scanned) break;
        }
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        Arrays.fill(NEAR, null);
        Arrays.fill(DISTANCES, Float.POSITIVE_INFINITY);
        size = 0;
        for (Emitter source : SOURCES.values()) {
            BlockLightTest.addNearby(source, camera);
        }
        Item source = minecraft.f_91074_.m_21205_().m_41720_();
        if (source instanceof BlockItem && (light = (held = (item = (BlockItem)source).m_40614_().m_49966_()).getLightEmission((BlockGetter)current, minecraft.f_91074_.m_20183_())) > 0) {
            BlockLightTest.addNearby(new Emitter(camera.f_82479_, camera.f_82480_ - 0.15, camera.f_82481_, light, BlockLightTest.tint(item.m_40614_().m_7705_()), true), camera);
        }
    }

    public static void bind(int program, boolean entityProgram) {
        if (program <= 0) {
            return;
        }
        int[] slots = PROGRAMS.computeIfAbsent(program, id -> new int[]{GL20.glGetUniformLocation((int)id, (CharSequence)(entityProgram ? "BtCount" : "u_BtCount")), GL20.glGetUniformLocation((int)id, (CharSequence)(entityProgram ? "BtEmitters[0]" : "u_BtEmitters[0]")), GL20.glGetUniformLocation((int)id, (CharSequence)(entityProgram ? "BtColors[0]" : "u_BtColors[0]")), entityProgram ? GL20.glGetUniformLocation((int)id, (CharSequence)"BtActive") : -1});
        if (slots[3] >= 0) {
            GL20.glUniform1i((int)slots[3], (int)(BlockLightTest.enabled() ? 1 : 0));
        }
        if (!BlockLightTest.enabled()) {
            return;
        }
        if (slots[0] < 0) {
            return;
        }
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Arrays.fill(LOCATIONS, 0.0f);
        Arrays.fill(COLORS, 0.0f);
        for (int i = 0; i < size; ++i) {
            Emitter light = NEAR[i];
            int offset = i * 4;
            BlockLightTest.LOCATIONS[offset] = (float)(light.x - camera.f_82479_);
            BlockLightTest.LOCATIONS[offset + 1] = (float)(light.y - camera.f_82480_);
            BlockLightTest.LOCATIONS[offset + 2] = (float)(light.z - camera.f_82481_);
            BlockLightTest.LOCATIONS[offset + 3] = (light.held ? -1.0f : 1.0f) * (float)light.level / 15.0f;
            BlockLightTest.COLORS[offset] = light.rgb[0];
            BlockLightTest.COLORS[offset + 1] = light.rgb[1];
            BlockLightTest.COLORS[offset + 2] = light.rgb[2];
        }
        GL20.glUniform1i((int)slots[0], (int)size);
        if (slots[1] >= 0) {
            GL20.glUniform4fv((int)slots[1], (float[])LOCATIONS);
        }
        if (slots[2] >= 0) {
            GL20.glUniform4fv((int)slots[2], (float[])COLORS);
        }
    }

    private static void addNearby(Emitter source, Vec3 camera) {
        int end;
        int insert;
        float dx = (float)(source.x - camera.f_82479_);
        float dy = (float)(source.y - camera.f_82480_);
        float dz = (float)(source.z - camera.f_82481_);
        float distance = dx * dx + dy * dy + dz * dz;
        if (distance > 1296.0f) {
            return;
        }
        for (insert = 0; insert < size && distance >= DISTANCES[insert]; ++insert) {
        }
        if (insert >= 12) {
            return;
        }
        for (int index = end = Math.min(size, 11); index > insert; --index) {
            BlockLightTest.NEAR[index] = NEAR[index - 1];
            BlockLightTest.DISTANCES[index] = DISTANCES[index - 1];
        }
        BlockLightTest.NEAR[insert] = source;
        BlockLightTest.DISTANCES[insert] = distance;
        size = Math.min(size + 1, 12);
    }

    private static void setLevel(ClientLevel next) {
        if (level == next) {
            return;
        }
        level = next;
        SOURCES.clear();
        CHUNKS.clear();
        SCANNED_CHUNKS.clear();
        size = 0;
    }

    private static Emitter create(BlockPos pos, BlockState state, int strength) {
        ResourceLocation id = BuiltInRegistries.f_256975_.m_7981_((Object)state.m_60734_());
        return new Emitter((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, Math.min(strength, 15), BlockLightTest.tint(id == null ? "" : id.m_135815_()), false);
    }

    private static float[] tint(String name) {
        if (name.contains("soul")) {
            return new float[]{0.28f, 0.78f, 1.0f};
        }
        if (name.contains("redstone")) {
            return new float[]{1.0f, 0.2f, 0.08f};
        }
        if (name.contains("sea_lantern") || name.contains("conduit")) {
            return new float[]{0.58f, 0.95f, 1.0f};
        }
        if (name.contains("end_rod") || name.contains("pearlescent_froglight")) {
            return new float[]{1.0f, 0.72f, 0.95f};
        }
        if (name.contains("verdant_froglight")) {
            return new float[]{0.68f, 1.0f, 0.66f};
        }
        if (name.contains("glowstone") || name.contains("ochre_froglight") || name.contains("shroomlight")) {
            return new float[]{1.0f, 0.82f, 0.48f};
        }
        if (name.contains("sculk") || name.contains("respawn_anchor")) {
            return new float[]{0.2f, 0.78f, 0.92f};
        }
        if (name.contains("amethyst")) {
            return new float[]{0.78f, 0.48f, 1.0f};
        }
        if (name.contains("glow_lichen") || name.contains("glow_berries")) {
            return new float[]{0.72f, 1.0f, 0.58f};
        }
        if (name.contains("lava") || name.contains("torch") || name.contains("lantern") || name.contains("fire") || name.contains("candle")) {
            return new float[]{1.0f, 0.58f, 0.26f};
        }
        return new float[]{1.0f, 0.76f, 0.48f};
    }

    private record Emitter(double x, double y, double z, int level, float[] rgb, boolean held) {
    }
}

