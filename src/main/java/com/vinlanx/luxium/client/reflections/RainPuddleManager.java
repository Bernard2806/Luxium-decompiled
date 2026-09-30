/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.NativeImage
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.client.reflections;

import com.mojang.blaze3d.platform.NativeImage;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.reflections.RainHeightmap;
import com.vinlanx.luxium.client.reflections.RainPuddleSurface;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import com.vinlanx.luxium.client.reflections.ReflectionMaterialRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class RainPuddleManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final RainPuddleManager INSTANCE = new RainPuddleManager();
    private static final String RAIN_RESOURCE_ROOT = "rain";
    private static final String PNG_SUFFIX = ".png";
    private static final Path DEV_RAIN_DIR = Path.of("src", "main", "resources", "assets", "luxium", "rain");
    private static final int MIN_PUDDLE_SIZE = 1;
    private static final int MAX_PUDDLE_SIZE = 10;
    private final Map<ResourceLocation, RainHeightmap> heightmapsById = new LinkedHashMap<ResourceLocation, RainHeightmap>();
    private List<RainHeightmap> orderedHeightmaps = List.of();
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<RainPuddleSurface>> puddlesByDimension = new HashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<RainPuddleSurface>>();

    private RainPuddleManager() {
    }

    public static RainPuddleManager get() {
        return INSTANCE;
    }

    public synchronized int reloadHeightmaps(ResourceManager resourceManager) {
        LinkedHashMap<ResourceLocation, RainHeightmap> loaded = new LinkedHashMap<ResourceLocation, RainHeightmap>();
        this.loadFromResourceManager(resourceManager, loaded);
        this.loadFromDevDirectory(loaded);
        this.heightmapsById.clear();
        this.heightmapsById.putAll(loaded);
        this.orderedHeightmaps = List.copyOf(this.heightmapsById.values());
        return this.orderedHeightmaps.size();
    }

    @Nullable
    public synchronized RainPuddleSurface placeRandomPuddle(Minecraft mc, BlockPos pos, int sizeBlocks) {
        ClientLevel level = mc.f_91073_;
        if (level == null) {
            return null;
        }
        this.reloadHeightmaps(mc.m_91098_());
        if (this.orderedHeightmaps.isEmpty()) {
            return null;
        }
        int clampedSize = Math.max(1, Math.min(10, sizeBlocks));
        RainHeightmap selected = this.orderedHeightmaps.get(ThreadLocalRandom.current().nextInt(this.orderedHeightmaps.size()));
        RainPuddleSurface surface = this.buildSurface(mc, level, pos, selected, clampedSize);
        if (surface == null) {
            return null;
        }
        Long2ObjectOpenHashMap<RainPuddleSurface> puddles = this.puddlesFor(level);
        for (long coveredKey : surface.getCoveredBlockKeys()) {
            this.removePlacementAt(puddles, coveredKey);
        }
        for (long coveredKey : surface.getCoveredBlockKeys()) {
            puddles.put(coveredKey, (Object)surface);
        }
        ReflectionSystem.get().forceImmediateUpdate();
        return surface;
    }

    @Nullable
    public synchronized RainPuddleSurface getActiveSurface(@Nullable ClientLevel level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        Long2ObjectOpenHashMap<RainPuddleSurface> puddles = this.puddlesByDimension.get(level.m_46472_());
        if (puddles == null) {
            return null;
        }
        return (RainPuddleSurface)puddles.get(pos.m_121878_());
    }

    public synchronized void removeIfUnsupported(@Nullable ClientLevel level, BlockPos pos, BlockState state) {
        boolean supportsTopFace;
        if (level == null) {
            return;
        }
        ReflectionMaterial material = ReflectionMaterialRegistry.find(state);
        boolean bl = supportsTopFace = material != null && material.shouldReflectFace(Minecraft.m_91087_(), pos, state, Direction.UP);
        if (supportsTopFace) {
            return;
        }
        Long2ObjectOpenHashMap<RainPuddleSurface> puddles = this.puddlesByDimension.get(level.m_46472_());
        if (puddles == null) {
            return;
        }
        this.removePlacementAt(puddles, pos.m_121878_());
        if (puddles.isEmpty()) {
            this.puddlesByDimension.remove(level.m_46472_());
        }
    }

    public synchronized int getLoadedHeightmapCount() {
        return this.orderedHeightmaps.size();
    }

    private Long2ObjectOpenHashMap<RainPuddleSurface> puddlesFor(ClientLevel level) {
        return this.puddlesByDimension.computeIfAbsent((ResourceKey<Level>)level.m_46472_(), key -> new Long2ObjectOpenHashMap());
    }

    @Nullable
    private RainPuddleSurface buildSurface(Minecraft mc, ClientLevel level, BlockPos center, RainHeightmap heightmap, int sizeBlocks) {
        int minX = center.m_123341_() - (sizeBlocks - 1) / 2;
        int minZ = center.m_123343_() - (sizeBlocks - 1) / 2;
        LongArrayList covered = new LongArrayList(sizeBlocks * sizeBlocks);
        for (int dz = 0; dz < sizeBlocks; ++dz) {
            for (int dx = 0; dx < sizeBlocks; ++dx) {
                BlockPos candidate = new BlockPos(minX + dx, center.m_123342_(), minZ + dz);
                BlockState candidateState = level.m_8055_(candidate);
                ReflectionMaterial material = ReflectionMaterialRegistry.find(candidateState);
                if (material == null || !material.shouldReflectFace(mc, candidate, candidateState, Direction.UP)) continue;
                covered.add(candidate.m_121878_());
            }
        }
        if (covered.isEmpty()) {
            return null;
        }
        return new RainPuddleSurface(heightmap, sizeBlocks, minX, minZ, covered.toLongArray());
    }

    private void removePlacementAt(Long2ObjectOpenHashMap<RainPuddleSurface> puddles, long blockKey) {
        RainPuddleSurface existing = (RainPuddleSurface)puddles.get(blockKey);
        if (existing == null) {
            return;
        }
        for (long coveredKey : existing.getCoveredBlockKeys()) {
            puddles.remove(coveredKey);
        }
    }

    private void loadFromResourceManager(ResourceManager resourceManager, Map<ResourceLocation, RainHeightmap> loaded) {
        Map<ResourceLocation, Resource> resources = resourceManager.m_214159_(RAIN_RESOURCE_ROOT, location -> location.m_135827_().equals("luxium") && location.m_135815_().toLowerCase(Locale.ROOT).endsWith(PNG_SUFFIX));
        ArrayList<ResourceLocation> ids = new ArrayList<>(resources.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::m_135815_));
        for (ResourceLocation id : ids) {
            Resource resource = (Resource)resources.get(id);
            if (resource == null) continue;
            try {
                InputStream input = resource.m_215507_();
                try {
                    RainHeightmap heightmap = this.loadHeightmap(id, input);
                    if (heightmap == null) continue;
                    loaded.put(id, heightmap);
                }
                finally {
                    if (input == null) continue;
                    input.close();
                }
            }
            catch (IOException e) {
                LOGGER.error("Failed to load rain heightmap {}", (Object)id, (Object)e);
            }
        }
    }

    private void loadFromDevDirectory(Map<ResourceLocation, RainHeightmap> loaded) {
        if (!Files.isDirectory(DEV_RAIN_DIR, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.walk(DEV_RAIN_DIR, new FileVisitOption[0]);){
            List<Path> files = stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(PNG_SUFFIX)).sorted(Comparator.comparing(path -> DEV_RAIN_DIR.relativize((Path)path).toString())).toList();
            for (Path file : files) {
                String relativePath = DEV_RAIN_DIR.relativize(file).toString().replace('\\', '/');
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)("rain/" + relativePath));
                try {
                    InputStream input = Files.newInputStream(file, new OpenOption[0]);
                    try {
                        RainHeightmap heightmap = this.loadHeightmap(id, input);
                        if (heightmap == null) continue;
                        loaded.put(id, heightmap);
                    }
                    finally {
                        if (input == null) continue;
                        input.close();
                    }
                }
                catch (IOException e) {
                    LOGGER.error("Failed to load dev rain heightmap {}", (Object)file, (Object)e);
                }
            }
        }
        catch (IOException e) {
            LOGGER.error("Failed to scan {} for rain heightmaps", (Object)DEV_RAIN_DIR, (Object)e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private RainHeightmap loadHeightmap(ResourceLocation id, InputStream input) {
        try (NativeImage image = NativeImage.m_85058_((InputStream)input);){
            int width = image.m_84982_();
            int height = image.m_85084_();
            if (width <= 0 || height <= 0) {
                RainHeightmap rainHeightmap = null;
                return rainHeightmap;
            }
            float[] opacity = new float[width * height];
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int pixel = image.m_84985_(x, y);
                    int alpha = pixel >>> 24 & 0xFF;
                    int luminance = pixel & 0xFF;
                    opacity[y * width + x] = (float)alpha / 255.0f * ((float)luminance / 255.0f);
                }
            }
            RainHeightmap heightmap = new RainHeightmap(id, width, height, opacity);
            if (!heightmap.hasVisiblePixels()) {
                LOGGER.warn("Skipping rain heightmap {} because it has no visible puddle data", (Object)id);
                RainHeightmap rainHeightmap = null;
                return rainHeightmap;
            }
            RainHeightmap rainHeightmap = heightmap;
            return rainHeightmap;
        }
        catch (IOException e) {
            LOGGER.error("Failed to decode rain heightmap {}", (Object)id, (Object)e);
            return null;
        }
    }
}
