/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 */
package com.vinlanx.luxium.client.shadows;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shadows.GpuBakedHardShadowVolume;
import com.vinlanx.luxium.client.shadows.GpuLocalLightResolver;
import com.vinlanx.luxium.client.shadows.GpuNeoFloodVisibilityAtlas;
import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.client.shadows.GpuShadowFrameState;
import com.vinlanx.luxium.client.shadows.GpuShadowLightGrid;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class GpuNeoShadows {
    public static final int MAX_LIGHTS = 32;
    private static final int MIN_LIGHT_DISTANCE = 4;
    private static final int MAX_LIGHT_DISTANCE = 64;
    private static final float LOD_BLEND_START = 0.8f;
    private static final GpuShadowLightGrid LIGHT_GRID = new GpuShadowLightGrid();
    private static final GpuNeoFloodVisibilityAtlas NEO_FLOOD_VISIBILITY_ATLAS = new GpuNeoFloodVisibilityAtlas();
    private static final GpuBakedHardShadowVolume BAKED_HARD_VOLUME = new GpuBakedHardShadowVolume();
    private static final float[] LIGHT_POS_RADIUS_BUF = new float[128];
    private static final float[] LIGHT_COLOR_INTENSITY_BUF = new float[128];
    private static final float[] LIGHT_GEOMETRY_BUF = new float[128];
    private static final float[] LIGHT_ATLAS_RECT_BUF = new float[768];
    private static final long[] VISIBLE_SOURCE_KEYS_BUF = new long[32];
    private static final int[] VISIBLE_EMISSIONS_BUF = new int[32];
    private static final float[] VISIBLE_GPU_WEIGHTS_BUF = new float[32];
    private static final int[] VISIBLE_VISIBILITY_SLOTS_BUF = new int[32];
    @Nullable
    private static volatile ClientLevel trackedLevel;
    @Nullable
    private static volatile Config.GpuLocalShadowMode trackedShadowMode;
    private static volatile boolean trackedHardBakeEnabled;
    private static long[] stagedSources;
    private static int[] stagedEmissions;
    private static volatile boolean mainPassActive;
    private static volatile boolean resourcesAllocated;
    private static volatile long frameVersion;
    private static volatile float bakedHardRenderMinX;
    private static volatile float bakedHardRenderMinY;
    private static volatile float bakedHardRenderMinZ;
    private static volatile GpuShadowFrameState frameState;

    private GpuNeoShadows() {
    }

    public static boolean isActive() {
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) && !Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) && Config.CLIENT.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.NEOFLOOD_ONLY;
    }

    public static boolean isHardShadowBakeEnabled() {
        return GpuNeoShadows.isActive() && Config.isFeatureEnabled(Config.CLIENT.gpuHardShadowBakeEnabled);
    }

    public static boolean entityShadowCastersEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.entityShadowsEnabled) && !GpuNeoShadows.isHardShadowBakeEnabled();
    }

    public static boolean isBakedHardReceiverReady() {
        return GpuNeoShadows.isHardShadowBakeEnabled() && BAKED_HARD_VOLUME.isReady();
    }

    public static int bakedHardTexture(int direction) {
        return BAKED_HARD_VOLUME.textureId(direction);
    }

    public static float bakedHardMinX() {
        return bakedHardRenderMinX;
    }

    public static float bakedHardMinY() {
        return bakedHardRenderMinY;
    }

    public static float bakedHardMinZ() {
        return bakedHardRenderMinZ;
    }

    public static float bakedHardSizeX() {
        return BAKED_HARD_VOLUME.sizeX();
    }

    public static float bakedHardSizeY() {
        return BAKED_HARD_VOLUME.sizeY();
    }

    public static float bakedHardSizeZ() {
        return BAKED_HARD_VOLUME.sizeZ();
    }

    public static void onConfigChanged() {
        trackedShadowMode = null;
        trackedHardBakeEnabled = !Config.isFeatureEnabled(Config.CLIENT.gpuHardShadowBakeEnabled);
    }

    public static Config.GpuLocalShadowMode getShadowMode() {
        if (GpuNeoShadows.isHardShadowBakeEnabled()) {
            return Config.GpuLocalShadowMode.CUBEMAP_HARD;
        }
        Config.GpuLocalShadowMode configured = (Config.GpuLocalShadowMode)((Object)Config.CLIENT.gpuLocalShadowMode.get());
        return configured == Config.GpuLocalShadowMode.FAST_SPREAD ? Config.GpuLocalShadowMode.CUBEMAP_HARD : configured;
    }

    public static boolean usesCubemapShadows() {
        Config.GpuLocalShadowMode mode = GpuNeoShadows.getShadowMode();
        return mode == Config.GpuLocalShadowMode.CUBEMAP_HARD || mode == Config.GpuLocalShadowMode.CUBEMAP_SOFT;
    }

    public static double getLightSearchRadius() {
        return Math.max(4, Math.min(64, (Integer)Config.CLIENT.gpuLocalLightDistance.get()));
    }

    public static int getActiveLightLimit() {
        return Math.max(4, Math.min(32, (Integer)Config.CLIENT.gpuFastSpreadMaxLights.get()));
    }

    public static void prepareShadowAtlas(Minecraft mc, Camera camera) {
        mainPassActive = false;
        GpuNeoShadows.syncLevel(mc.f_91073_);
        GpuNeoShadows.syncShadowMode();
        if (!GpuNeoShadows.isUsable(mc)) {
            TorchRtxState.get().clearGpuDirectSources();
            if (!GpuNeoShadows.isActive() || mc.f_91073_ == null) {
                GpuNeoShadows.releaseGpuResources();
            }
            GpuNeoShadows.clearFrame(false);
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        TorchRtxState sourceRegistry = TorchRtxState.get();
        double searchRadius = GpuNeoShadows.getLightSearchRadius();
        int lightLimit = GpuNeoShadows.getActiveLightLimit();
        stagedSources = sourceRegistry.getClosestSources(cameraPos, searchRadius * searchRadius, lightLimit);
        stagedEmissions = GpuNeoShadows.collectEmissions(sourceRegistry, stagedSources);
        if (GpuNeoShadows.usesCubemapShadows()) {
            GpuShadowCache cache = GpuShadowCache.get();
            if (GpuNeoShadows.entityShadowCastersEnabled()) {
                cache.refreshDynamicCasters(mc.f_91073_, camera.m_90588_(), cameraPos);
            }
            cache.stageVisibleEntries(mc.f_91073_, stagedSources, stagedEmissions, lightLimit);
        }
        if (stagedSources.length > 0) {
            resourcesAllocated = true;
        }
    }

    public static void prepareMainWorldRender(Minecraft mc, Camera camera, Matrix4f projection, Matrix4f view) {
        int hardBakeDirtyFaces;
        mainPassActive = false;
        if (!GpuNeoShadows.isUsable(mc) || stagedSources.length == 0) {
            TorchRtxState.get().clearGpuDirectSources();
            GpuNeoShadows.clearFrame(false);
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        GpuShadowCache cache = GpuShadowCache.get();
        int lightLimit = GpuNeoShadows.getActiveLightLimit();
        boolean hardBake = GpuNeoShadows.isHardShadowBakeEnabled();
        long hardBakeInputSignature = hardBake ? GpuNeoShadows.hardBakeSignature(cache, stagedSources, stagedEmissions, lightLimit) : Long.MIN_VALUE;
        int n = hardBakeDirtyFaces = hardBake ? cache.countDirtyFacesForSources(stagedSources, lightLimit) : 0;
        if (hardBake && BAKED_HARD_VOLUME.isReady() && (hardBakeDirtyFaces > 0 || BAKED_HARD_VOLUME.matches(cameraPos, hardBakeInputSignature))) {
            GpuNeoShadows.publishBakedHardOnly(cameraPos);
            return;
        }
        Arrays.fill(LIGHT_POS_RADIUS_BUF, 0.0f);
        Arrays.fill(LIGHT_COLOR_INTENSITY_BUF, 0.0f);
        Arrays.fill(LIGHT_GEOMETRY_BUF, 0.0f);
        Arrays.fill(LIGHT_ATLAS_RECT_BUF, 0.0f);
        Arrays.fill(VISIBLE_SOURCE_KEYS_BUF, 0L);
        Arrays.fill(VISIBLE_EMISSIONS_BUF, 0);
        Arrays.fill(VISIBLE_GPU_WEIGHTS_BUF, 0.0f);
        Arrays.fill(VISIBLE_VISIBILITY_SLOTS_BUF, -1);
        int lightCount = GpuNeoShadows.usesCubemapShadows() ? cache.collectVisibleEntries(stagedSources, stagedEmissions, lightLimit, cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_, LIGHT_POS_RADIUS_BUF, LIGHT_ATLAS_RECT_BUF, VISIBLE_SOURCE_KEYS_BUF, VISIBLE_EMISSIONS_BUF, LIGHT_GEOMETRY_BUF) : GpuNeoShadows.collectSpreadEntries(stagedSources, stagedEmissions, lightLimit, cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_, LIGHT_POS_RADIUS_BUF, VISIBLE_SOURCE_KEYS_BUF, VISIBLE_EMISSIONS_BUF);
        if (lightCount <= 0) {
            TorchRtxState.get().clearGpuDirectSources();
            GpuNeoShadows.clearFrame(false);
            return;
        }
        Config.GpuLocalShadowMode mode = GpuNeoShadows.getShadowMode();
        if (mode == Config.GpuLocalShadowMode.FAST_SPREAD) {
            NEO_FLOOD_VISIBILITY_ATLAS.update(TorchRtxState.get().getEngine(), VISIBLE_SOURCE_KEYS_BUF, lightCount, lightLimit, VISIBLE_VISIBILITY_SLOTS_BUF);
        }
        float searchRadius = (float)GpuNeoShadows.getLightSearchRadius();
        float blendStart = searchRadius * 0.8f;
        for (int i = 0; i < lightCount; ++i) {
            float gpuWeight;
            int lightBase = i * 4;
            float x = LIGHT_POS_RADIUS_BUF[lightBase];
            float y = LIGHT_POS_RADIUS_BUF[lightBase + 1];
            float z = LIGHT_POS_RADIUS_BUF[lightBase + 2];
            float cameraDistance = (float)Math.sqrt(x * x + y * y + z * z);
            GpuNeoShadows.VISIBLE_GPU_WEIGHTS_BUF[i] = gpuWeight = 1.0f - GpuNeoShadows.smoothstep(blendStart, searchRadius, cameraDistance);
            long sourceKey = VISIBLE_SOURCE_KEYS_BUF[i];
            GpuLocalLightResolver.resolve(mc.f_91073_, sourceKey, VISIBLE_EMISSIONS_BUF[i], LIGHT_COLOR_INTENSITY_BUF, lightBase);
            GpuLocalLightResolver.SourceGeometry geometry = GpuLocalLightResolver.resolveGeometry(mc.f_91073_, sourceKey);
            int rectBase = i * 24;
            GpuNeoShadows.LIGHT_ATLAS_RECT_BUF[rectBase + 2] = LIGHT_COLOR_INTENSITY_BUF[lightBase];
            GpuNeoShadows.LIGHT_ATLAS_RECT_BUF[rectBase + 3] = LIGHT_COLOR_INTENSITY_BUF[lightBase + 1];
            GpuNeoShadows.LIGHT_ATLAS_RECT_BUF[rectBase + 6] = LIGHT_COLOR_INTENSITY_BUF[lightBase + 2];
            GpuNeoShadows.LIGHT_ATLAS_RECT_BUF[rectBase + 7] = gpuWeight;
            GpuLocalLightResolver.writeGeometryMetadata(geometry, LIGHT_ATLAS_RECT_BUF, rectBase);
            if (mode != Config.GpuLocalShadowMode.FAST_SPREAD) continue;
            int visibilitySlot = VISIBLE_VISIBILITY_SLOTS_BUF[i];
            GpuNeoShadows.LIGHT_ATLAS_RECT_BUF[rectBase + 19] = visibilitySlot >= 0 ? (float)visibilitySlot + 1.0f : 0.0f;
        }
        if (hardBake) {
            if (hardBakeDirtyFaces == 0 && lightCount > 0) {
                int faceBudget = Math.max(1, (Integer)Config.CLIENT.gpuHardShadowCaptureBudget.get());
                int layerBudget = Math.max(24, Math.min(768, faceBudget * 4));
                BAKED_HARD_VOLUME.update(cameraPos, lightCount, LIGHT_POS_RADIUS_BUF, LIGHT_GEOMETRY_BUF, LIGHT_COLOR_INTENSITY_BUF, LIGHT_ATLAS_RECT_BUF, cache.getAtlasTextureId(), hardBakeInputSignature, layerBudget);
            }
            if (BAKED_HARD_VOLUME.isReady()) {
                GpuNeoShadows.publishBakedHardOnly(cameraPos);
                return;
            }
        }
        int width = mc.m_91385_().f_83917_;
        int height = mc.m_91385_().f_83918_;
        if (width <= 0 || height <= 0) {
            TorchRtxState.get().clearGpuDirectSources();
            GpuNeoShadows.clearFrame(false);
            return;
        }
        LIGHT_GRID.update(width, height, lightCount, LIGHT_POS_RADIUS_BUF, projection, view);
        TorchRtxState.get().setGpuDirectSources(VISIBLE_SOURCE_KEYS_BUF, VISIBLE_GPU_WEIGHTS_BUF, lightCount);
        resourcesAllocated = true;
        int shaderMode = switch (mode) {
            default -> throw new IncompatibleClassChangeError();
            case Config.GpuLocalShadowMode.FAST_SPREAD -> 0;
            case Config.GpuLocalShadowMode.CUBEMAP_HARD -> 1;
            case Config.GpuLocalShadowMode.CUBEMAP_SOFT -> 2;
        };
        boolean softShadows = mode == Config.GpuLocalShadowMode.CUBEMAP_SOFT;
        float spreadStrength = (float)Math.max(0.0, Math.min(1.0, (Double)Config.CLIENT.gpuSpreadOcclusionStrength.get()));
        long version = ++frameVersion;
        frameState = new GpuShadowFrameState(true, softShadows, true, shaderMode, spreadStrength, lightCount, GpuNeoShadows.usesCubemapShadows() ? cache.getAtlasTextureId() : NEO_FLOOD_VISIBILITY_ATLAS.getTextureId(), LIGHT_GRID.getTextureId(), version, Arrays.copyOf(LIGHT_POS_RADIUS_BUF, LIGHT_POS_RADIUS_BUF.length), Arrays.copyOf(LIGHT_ATLAS_RECT_BUF, LIGHT_ATLAS_RECT_BUF.length));
    }

    public static boolean hasStagedLights() {
        return GpuNeoShadows.isActive() && stagedSources.length > 0;
    }

    public static void beginMainWorldRender() {
        mainPassActive = frameState.enabled();
    }

    public static void finishMainWorldRender() {
        mainPassActive = false;
    }

    public static boolean isMainPassActive() {
        return mainPassActive;
    }

    public static GpuShadowFrameState frameState() {
        return frameState;
    }

    public static void releaseResources() {
        trackedLevel = null;
        trackedShadowMode = null;
        trackedHardBakeEnabled = false;
        stagedSources = new long[0];
        stagedEmissions = new int[0];
        mainPassActive = false;
        TorchRtxState.get().clearGpuDirectSources();
        frameState = GpuShadowFrameState.disabled(++frameVersion);
        bakedHardRenderMinZ = 0.0f;
        bakedHardRenderMinY = 0.0f;
        bakedHardRenderMinX = 0.0f;
        GpuNeoShadows.releaseGpuResources();
    }

    private static boolean isUsable(Minecraft mc) {
        return GpuNeoShadows.isActive() && mc.f_91073_ != null && mc.f_91074_ != null;
    }

    private static int[] collectEmissions(TorchRtxState sourceRegistry, long[] sources) {
        int[] emissions = new int[Math.min(sources.length, 32)];
        for (int i = 0; i < emissions.length; ++i) {
            emissions[i] = sourceRegistry.getSourceEmission(sources[i]);
        }
        return emissions;
    }

    private static int collectSpreadEntries(long[] sourceKeys, int[] emissions, int limit, double cameraX, double cameraY, double cameraZ, float[] lightPosRadiusOut, long[] visibleSourceKeysOut, int[] visibleEmissionsOut) {
        if (sourceKeys == null || emissions == null || limit <= 0) {
            return 0;
        }
        int count = 0;
        int max = Math.min(limit, Math.min(sourceKeys.length, emissions.length));
        for (int i = 0; i < max; ++i) {
            int emission = emissions[i];
            if (emission <= 0) continue;
            long sourceKey = sourceKeys[i];
            GpuLocalLightResolver.SourceGeometry geometry = GpuLocalLightResolver.resolveGeometry(trackedLevel, sourceKey);
            int posBase = count * 4;
            lightPosRadiusOut[posBase] = (float)(geometry.sourceX - cameraX);
            lightPosRadiusOut[posBase + 1] = (float)(geometry.sourceY - cameraY);
            lightPosRadiusOut[posBase + 2] = (float)(geometry.sourceZ - cameraZ);
            lightPosRadiusOut[posBase + 3] = (float)LightRtMath.getBlockShadowRadius(emission);
            visibleSourceKeysOut[count] = sourceKey;
            visibleEmissionsOut[count] = emission;
            ++count;
        }
        return count;
    }

    private static void clearFrame(boolean clearStaged) {
        if (clearStaged) {
            stagedSources = new long[0];
            stagedEmissions = new int[0];
        }
        mainPassActive = false;
        frameState = GpuShadowFrameState.disabled(++frameVersion);
    }

    private static void releaseGpuResources() {
        if (!resourcesAllocated) {
            return;
        }
        LIGHT_GRID.release();
        NEO_FLOOD_VISIBILITY_ATLAS.release();
        BAKED_HARD_VOLUME.release();
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            GpuShadowCache.get().release();
        }
        resourcesAllocated = false;
    }

    private static void syncLevel(@Nullable ClientLevel level) {
        if (trackedLevel != null && trackedLevel != level) {
            TorchRtxState.get().clearGpuDirectSources();
            GpuNeoShadows.releaseGpuResources();
            GpuNeoShadows.clearFrame(true);
        }
        trackedLevel = level;
    }

    private static void syncShadowMode() {
        Config.GpuLocalShadowMode mode = GpuNeoShadows.getShadowMode();
        boolean hardBake = Config.isFeatureEnabled(Config.CLIENT.gpuHardShadowBakeEnabled);
        Config.GpuLocalShadowMode previous = trackedShadowMode;
        if (previous != null && previous != mode || trackedHardBakeEnabled != hardBake) {
            TorchRtxState.get().clearGpuDirectSources();
            if (!NeoGpuVanilla.isConfiguredEnabled()) {
                GpuShadowCache.get().release();
            }
            NEO_FLOOD_VISIBILITY_ATLAS.release();
            BAKED_HARD_VOLUME.release();
            stagedSources = new long[0];
            stagedEmissions = new int[0];
            frameState = GpuShadowFrameState.disabled(++frameVersion);
        }
        trackedShadowMode = mode;
        trackedHardBakeEnabled = hardBake;
    }

    private static long hardBakeSignature(GpuShadowCache cache, long[] sourceKeys, int[] emissions, int limit) {
        long h = -3750763034362895579L;
        int count = Math.min(Math.max(limit, 0), Math.min(sourceKeys.length, emissions.length));
        h = GpuNeoShadows.mix(h, cache.getNeoGpuVanillaContentGeneration(sourceKeys, count));
        h = GpuNeoShadows.mix(h, count);
        h = GpuNeoShadows.mix(h, ((Integer)Config.CLIENT.gpuFastSpreadMaxLights.get()).intValue());
        h = GpuNeoShadows.mix(h, ((Integer)Config.CLIENT.gpuLocalLightDistance.get()).intValue());
        for (int i = 0; i < count; ++i) {
            h = GpuNeoShadows.mix(h, sourceKeys[i]);
            h = GpuNeoShadows.mix(h, emissions[i]);
        }
        return h;
    }

    private static void publishBakedHardOnly(Vec3 cameraPos) {
        bakedHardRenderMinX = (float)((double)BAKED_HARD_VOLUME.minX() - cameraPos.f_82479_);
        bakedHardRenderMinY = (float)((double)BAKED_HARD_VOLUME.minY() - cameraPos.f_82480_);
        bakedHardRenderMinZ = (float)((double)BAKED_HARD_VOLUME.minZ() - cameraPos.f_82481_);
        GpuNeoShadows.publishBakedOwnership(cameraPos);
        LIGHT_GRID.release();
        resourcesAllocated = true;
        if (!frameState.enabled() || frameState.lightCount() != 0 || frameState.atlasTexture() != -1 || frameState.lightGridTexture() != -1 || frameState.shadowMode() != 1) {
            frameState = GpuShadowFrameState.bakedHardOnly(++frameVersion);
        }
    }

    private static void publishBakedOwnership(Vec3 cameraPos) {
        if (cameraPos == null) {
            return;
        }
        int count = Math.min(GpuNeoShadows.getActiveLightLimit(), stagedSources.length);
        float searchRadius = (float)GpuNeoShadows.getLightSearchRadius();
        float blendStart = searchRadius * 0.8f;
        for (int i = 0; i < count; ++i) {
            long key = stagedSources[i];
            double x = (double)BlockPos.m_121983_((long)key) + 0.5 - cameraPos.f_82479_;
            double y = (double)BlockPos.m_122008_((long)key) + 0.5 - cameraPos.f_82480_;
            double z = (double)BlockPos.m_122015_((long)key) + 0.5 - cameraPos.f_82481_;
            float distance = (float)Math.sqrt(x * x + y * y + z * z);
            GpuNeoShadows.VISIBLE_GPU_WEIGHTS_BUF[i] = 1.0f - GpuNeoShadows.smoothstep(blendStart, searchRadius, distance);
        }
        TorchRtxState.get().setGpuDirectSources(stagedSources, VISIBLE_GPU_WEIGHTS_BUF, count);
    }

    private static long mix(long h, long v) {
        return (h ^= v) * 1099511628211L;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) {
            return value < edge0 ? 0.0f : 1.0f;
        }
        float t = Math.max(0.0f, Math.min(1.0f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    static {
        stagedSources = new long[0];
        stagedEmissions = new int[0];
        frameState = GpuShadowFrameState.disabled(0L);
    }
}

