/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.vinlanx.luxium.rtx.neogpuvanilla;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shadows.GpuLocalLightResolver;
import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaFrameState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaGpuDebug;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaVolume;
import java.util.Arrays;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class NeoGpuVanilla {
    public static final int MAX_SOURCES = 40;
    private static final NeoGpuVanillaVolume VOLUME = new NeoGpuVanillaVolume();
    private static final float[] SOURCE_DATA = new float[160];
    private static final float[] SOURCE_GEOMETRY = new float[160];
    private static final float[] SOURCE_COLORS = new float[160];
    private static final float[] ATLAS_RECTS = new float[960];
    private static final long[] VISIBLE_KEYS = new long[40];
    private static final int[] VISIBLE_EMISSIONS = new int[40];
    private static ClientLevel trackedLevel;
    private static long[] stagedSources;
    private static int[] stagedEmissions;
    private static long stagedSignature;
    private static long stagedSourceSignature;
    private static long bakedSignature;
    private static long observedContentGeneration;
    private static long requestedVanillaGeneration;
    private static long bakedVanillaGeneration;
    private static boolean mainPassActive;
    private static int pendingVanillaSettleFrames;
    private static boolean sourceSetChangePending;
    private static int sourceSetQuietFrames;
    private static int sourceSetChangeAgeFrames;
    private static long frameVersion;
    private static NeoGpuVanillaFrameState frameState;

    private NeoGpuVanilla() {
    }

    public static boolean isConfiguredEnabled() {
        return Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaEnabled);
    }

    public static boolean isActive() {
        Minecraft mc = Minecraft.m_91087_();
        return NeoGpuVanilla.isConfiguredEnabled() && mc.f_91073_ != null && mc.f_91074_ != null;
    }

    public static boolean isMainPassActive() {
        return mainPassActive;
    }

    public static NeoGpuVanillaFrameState frameState() {
        return frameState;
    }

    public static void prepareShadowAtlas(Minecraft mc, Camera camera) {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            if (trackedLevel != null || VOLUME.hasResources()) {
                NeoGpuVanilla.releaseResources();
            }
            NeoGpuVanilla.clearFrame();
            return;
        }
        if (!NeoGpuVanilla.isActive() || mc.f_91073_ == null || camera == null) {
            NeoGpuVanilla.clearFrame();
            return;
        }
        if (trackedLevel != mc.f_91073_) {
            trackedLevel = mc.f_91073_;
            stagedSources = new long[0];
            stagedEmissions = new int[0];
            stagedSignature = 0L;
            stagedSourceSignature = 0L;
            bakedSignature = Long.MIN_VALUE;
            observedContentGeneration = Long.MIN_VALUE;
            requestedVanillaGeneration = 0L;
            bakedVanillaGeneration = Long.MIN_VALUE;
            sourceSetChangePending = false;
            sourceSetQuietFrames = 0;
            sourceSetChangeAgeFrames = 0;
            VOLUME.release();
            GpuShadowCache.get().release();
        }
        Vec3 cameraPos = camera.m_90583_();
        if (VOLUME.updateAnchor(cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_)) {
            bakedSignature = Long.MIN_VALUE;
        }
        GpuShadowCache cache = GpuShadowCache.get();
        int configuredLimit = Math.max(1, Math.min(40, (Integer)Config.CLIENT.neoGpuVanillaMaxSources.get()));
        int limit = Math.min(configuredLimit, cache.getSourceCapacity());
        double distance = ((Integer)Config.CLIENT.neoGpuVanillaDistance.get()).intValue();
        TorchRtxState registry = TorchRtxState.get();
        Vec3 selectionOrigin = new Vec3((double)VOLUME.buildAnchorX() + 24.0, (double)VOLUME.buildAnchorY() + 16.0, (double)VOLUME.buildAnchorZ() + 24.0);
        stagedSources = registry.getClosestSources(selectionOrigin, distance * distance, limit);
        stagedEmissions = new int[stagedSources.length];
        for (int i = 0; i < stagedSources.length; ++i) {
            NeoGpuVanilla.stagedEmissions[i] = registry.getSourceEmission(stagedSources[i]);
        }
        long nextSourceSignature = NeoGpuVanilla.signature(stagedSources, stagedEmissions, 0, 0, 0, (Integer)Config.CLIENT.neoGpuVanillaCaptureResolution.get());
        if (nextSourceSignature != stagedSourceSignature) {
            stagedSourceSignature = nextSourceSignature;
            if (!sourceSetChangePending) {
                sourceSetChangeAgeFrames = 0;
            }
            sourceSetChangePending = false;
            sourceSetQuietFrames = 0;
            sourceSetChangeAgeFrames = 0;
        }
        stagedSignature = NeoGpuVanilla.signature(stagedSources, stagedEmissions, VOLUME.buildAnchorX(), VOLUME.buildAnchorY(), VOLUME.buildAnchorZ(), (Integer)Config.CLIENT.neoGpuVanillaCaptureResolution.get());
        cache.stageVisibleEntries(mc.f_91073_, stagedSources, stagedEmissions, limit);
    }

    public static boolean hasStagedLights() {
        return NeoGpuVanilla.isActive() && stagedSources.length > 0;
    }

    public static void prepareMainWorldRender(Minecraft mc, Camera camera) {
        boolean wantsBake;
        mainPassActive = false;
        if (!NeoGpuVanilla.isActive() || mc.f_91073_ == null || camera == null) {
            NeoGpuVanilla.publishCurrentFrame(camera);
            return;
        }
        GpuShadowCache cache = GpuShadowCache.get();
        pendingVanillaSettleFrames = 0;
        sourceSetChangePending = false;
        sourceSetQuietFrames = 0;
        sourceSetChangeAgeFrames = 0;
        Arrays.fill(SOURCE_DATA, 0.0f);
        Arrays.fill(SOURCE_GEOMETRY, 0.0f);
        Arrays.fill(SOURCE_COLORS, 0.0f);
        Arrays.fill(ATLAS_RECTS, 0.0f);
        Arrays.fill(VISIBLE_KEYS, 0L);
        Arrays.fill(VISIBLE_EMISSIONS, 0);
        int count = cache.collectNeoGpuVanillaEntries(stagedSources, stagedEmissions, stagedSources.length, VOLUME.buildAnchorX(), VOLUME.buildAnchorY(), VOLUME.buildAnchorZ(), SOURCE_DATA, ATLAS_RECTS, VISIBLE_KEYS, VISIBLE_EMISSIONS, SOURCE_GEOMETRY);
        boolean capturePending = false;
        for (int i = 0; i < count; ++i) {
            NeoGpuVanilla.SOURCE_DATA[i * 4 + 3] = VISIBLE_EMISSIONS[i];
            GpuLocalLightResolver.resolve(mc.f_91073_, VISIBLE_KEYS[i], VISIBLE_EMISSIONS[i], SOURCE_COLORS, i * 4);
            int flags = Math.round(SOURCE_GEOMETRY[i * 4 + 3]);
            if ((flags & 0x80) == 0) continue;
            capturePending = true;
        }
        long contentGeneration = cache.getNeoGpuVanillaContentGeneration(stagedSources, stagedSources.length);
        boolean bl = wantsBake = stagedSignature != bakedSignature || contentGeneration != observedContentGeneration || requestedVanillaGeneration != bakedVanillaGeneration || !VOLUME.isReady();
        if (wantsBake) {
            int atlasTexture;
            int n = atlasTexture = count > 0 ? cache.getAtlasTextureId() : 0;
            if (VOLUME.bake(mc.f_91073_, atlasTexture, count, SOURCE_DATA, SOURCE_GEOMETRY, SOURCE_COLORS, ATLAS_RECTS)) {
                bakedSignature = stagedSignature;
                observedContentGeneration = contentGeneration;
                bakedVanillaGeneration = requestedVanillaGeneration;
            }
        }
        NeoGpuVanillaGpuDebug.tick(stagedSources.length, cache.countPendingNeoGpuVanillaFaces(stagedSources, stagedSources.length), (Integer)Config.CLIENT.neoGpuVanillaCaptureResolution.get());
        NeoGpuVanilla.publishCurrentFrame(camera);
    }

    public static void beginMainWorldRender() {
        mainPassActive = frameState.enabled();
    }

    public static void finishMainWorldRender() {
        mainPassActive = false;
    }

    public static void onGeometryChanged(BlockPos pos) {
        NeoGpuVanilla.onGeometryChanged(pos, true);
    }

    public static void onGeometryChanged(BlockPos pos, boolean vanillaLightMayChange) {
        if (!NeoGpuVanilla.isConfiguredEnabled() || !Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaGeometryUpdates) || pos == null) {
            return;
        }
        GpuShadowCache cache = GpuShadowCache.get();
        if (cache.usesDirectEmbeddiumCapture()) {
            cache.expectRenderedSectionChange(pos);
            SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
            if (worldRenderer != null) {
                worldRenderer.scheduleRebuildForChunk(pos.m_123341_() >> 4, pos.m_123342_() >> 4, pos.m_123343_() >> 4, true);
            }
        } else {
            cache.onGeometryChanged(pos);
        }
        if (NeoGpuVanilla.isInsideBuildVolume(pos)) {
            ++requestedVanillaGeneration;
            pendingVanillaSettleFrames = 0;
        }
    }

    public static void onRenderedSectionChanged(int originX, int originY, int originZ) {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
        GpuShadowCache.get().onRenderedSectionChanged(originX, originY, originZ);
    }

    public static void onChunkSourceRegistryChanged() {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
    }

    public static void onSourceRegistryChanged() {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
        ++requestedVanillaGeneration;
        pendingVanillaSettleFrames = 0;
    }

    public static void requestRebake() {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
        stagedSignature = Long.MIN_VALUE;
    }

    public static void requestRebuild() {
        if (!NeoGpuVanilla.isConfiguredEnabled()) {
            NeoGpuVanilla.releaseResources();
            return;
        }
        bakedSignature = Long.MIN_VALUE;
        pendingVanillaSettleFrames = 0;
    }

    public static void onConfigChanged() {
        if (NeoGpuVanilla.isConfiguredEnabled()) {
            NeoGpuVanilla.requestRebuild();
        } else {
            NeoGpuVanilla.releaseResources();
        }
    }

    public static void releaseResources() {
        trackedLevel = null;
        stagedSources = new long[0];
        stagedEmissions = new int[0];
        mainPassActive = false;
        VOLUME.release();
        GpuShadowCache.get().release();
        bakedSignature = Long.MIN_VALUE;
        observedContentGeneration = Long.MIN_VALUE;
        requestedVanillaGeneration = 0L;
        bakedVanillaGeneration = Long.MIN_VALUE;
        stagedSourceSignature = 0L;
        pendingVanillaSettleFrames = 0;
        sourceSetChangePending = false;
        sourceSetQuietFrames = 0;
        sourceSetChangeAgeFrames = 0;
        NeoGpuVanillaGpuDebug.reset();
        frameState = NeoGpuVanillaFrameState.disabled(++frameVersion);
    }

    public static int getVolumeTextureId() {
        return VOLUME.textureId();
    }

    public static int getReceiverTextureId() {
        return VOLUME.receiverTextureId();
    }

    public static boolean isVolumeReady() {
        return VOLUME.isReady();
    }

    public static Vector3f getVolumeMin(Vector3f out) {
        return out.set((float)VOLUME.anchorX(), (float)VOLUME.anchorY(), (float)VOLUME.anchorZ());
    }

    public static Vector3f getVolumeSize(Vector3f out) {
        return out.set(48.0f, 32.0f, 48.0f);
    }

    public static float sampleLightFactor(double worldX, double worldY, double worldZ) {
        return VOLUME.sampleLightFactor(worldX, worldY, worldZ);
    }

    public static boolean sampleRandomLitPosition(RandomSource random, Vector3f output) {
        return VOLUME.sampleRandomLitPosition(random, output);
    }

    private static boolean isInsideBuildVolume(BlockPos pos) {
        int minX = VOLUME.buildAnchorX();
        int minY = VOLUME.buildAnchorY();
        int minZ = VOLUME.buildAnchorZ();
        if (minX == Integer.MIN_VALUE || minY == Integer.MIN_VALUE || minZ == Integer.MIN_VALUE) {
            return false;
        }
        return pos.m_123341_() >= minX && pos.m_123341_() < minX + 48 && pos.m_123342_() >= minY && pos.m_123342_() < minY + 32 && pos.m_123343_() >= minZ && pos.m_123343_() < minZ + 48;
    }

    private static void publishCurrentFrame(Camera camera) {
        if (camera == null || !VOLUME.isReady()) {
            frameState = NeoGpuVanillaFrameState.disabled(++frameVersion);
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        boolean fast = Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaFastShadowsEnabled) && VOLUME.fastShadowsReady();
        frameState = new NeoGpuVanillaFrameState(true, VOLUME.receiverTextureId(), VOLUME.receiverScale(), fast, VOLUME.fastLightTextureId(0), VOLUME.fastLightTextureId(1), VOLUME.fastLightTextureId(2), VOLUME.fastLightTextureId(3), VOLUME.fastLightTextureId(4), VOLUME.fastLightTextureId(5), (float)((double)VOLUME.anchorX() - cameraPos.f_82479_), (float)((double)VOLUME.anchorY() - cameraPos.f_82480_), (float)((double)VOLUME.anchorZ() - cameraPos.f_82481_), 48.0f, 32.0f, 48.0f, ++frameVersion);
    }

    private static void clearFrame() {
        mainPassActive = false;
        frameState = NeoGpuVanillaFrameState.disabled(++frameVersion);
    }

    private static long signature(long[] keys, int[] emissions, int anchorX, int anchorY, int anchorZ, int captureResolution) {
        long h = -7046029254386353131L;
        h = NeoGpuVanilla.mix(h, anchorX);
        h = NeoGpuVanilla.mix(h, anchorY);
        h = NeoGpuVanilla.mix(h, anchorZ);
        h = NeoGpuVanilla.mix(h, captureResolution);
        h = NeoGpuVanilla.mix(h, Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaFastShadowsEnabled) ? 1L : 0L);
        h = NeoGpuVanilla.mix(h, Double.doubleToLongBits((Double)Config.CLIENT.neoGpuVanillaFastShadowStrength.get()));
        h = NeoGpuVanilla.mix(h, Double.doubleToLongBits((Double)Config.CLIENT.neoGpuVanillaFastDiffuseWrap.get()));
        int count = Math.min(keys.length, emissions.length);
        long xor = 0L;
        long sum = 0L;
        for (int i = 0; i < count; ++i) {
            long element = keys[i] ^ -7046029254386353131L * ((long)emissions[i] + 1L);
            element ^= element >>> 30;
            element *= -4658895280553007687L;
            element ^= element >>> 27;
            element *= -7723592293110705685L;
            element ^= element >>> 31;
            xor ^= element;
            sum += Long.rotateLeft(element, emissions[i] & 0x3F);
        }
        h = NeoGpuVanilla.mix(h, count);
        h = NeoGpuVanilla.mix(h, xor);
        h = NeoGpuVanilla.mix(h, sum);
        return h;
    }

    private static long mix(long hash, long value) {
        long v = value * -4417276706812531889L;
        v ^= v >>> 29;
        hash ^= v;
        return (hash *= 1609587929392839161L) ^ hash >>> 32;
    }

    static {
        stagedSources = new long[0];
        stagedEmissions = new int[0];
        bakedSignature = Long.MIN_VALUE;
        observedContentGeneration = Long.MIN_VALUE;
        bakedVanillaGeneration = Long.MIN_VALUE;
        frameState = NeoGpuVanillaFrameState.disabled(0L);
    }
}

