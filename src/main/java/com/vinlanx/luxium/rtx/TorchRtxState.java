/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2LongMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 *  net.minecraft.world.level.lighting.LightEngine
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.reflections.RainPuddleManager;
import com.vinlanx.luxium.client.reflections.ReflectionMaterialRegistry;
import com.vinlanx.luxium.client.shadows.GpuShadowCache;
import com.vinlanx.luxium.mixin.LightTextureAccessor;
import com.vinlanx.luxium.rtx.EntityShadowManager;
import com.vinlanx.luxium.rtx.FloodRtSettings;
import com.vinlanx.luxium.rtx.LightRtMath;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class TorchRtxState {
    private static final TorchRtxState INSTANCE = new TorchRtxState();
    private static final double ENTITY_UPDATE_RADIUS = 48.0;
    private static final int ENTITY_UPDATE_INTERVAL = 1;
    private static final int CLOSEST_SOURCE_CACHE_LIMIT = 20000;
    private static final int DIRECT_LIGHT_COVERAGE_THRESHOLD = 72;
    private static final int RTX_FRONTIER_BAND_RADIUS = 2;
    private static final int MAX_PENDING_LIGHT_CHANGES_PER_TICK = 192;
    private static final int MAX_PENDING_LIGHT_SECTIONS_PER_TICK = 48;
    private static final long LIGHT_CHANGE_BUDGET_NANOS = 1000000L;
    private static final int MAX_PENDING_SECTIONS_PER_TICK = 16;
    private static final Direction[] FACES = Direction.values();
    private static final long CHUNK_LIFECYCLE_BUDGET_NANOS = 1250000L;
    private static final int MAX_POPULATED_CHUNK_SECTIONS_PER_TICK = 2;
    private static final int MAX_CHUNK_UNLOADS_PER_TICK = 8;
    private static final int CHUNK_INVALIDATION_DEBOUNCE_TICKS = 4;
    private static final int CHUNK_INVALIDATION_MAX_WAIT_TICKS = 12;
    private static final int CHUNK_DISCOVERY_BUDGET_PER_TICK = 24;
    private final Long2ByteOpenHashMap lightSources = new Long2ByteOpenHashMap();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesByChunk = new Long2ObjectOpenHashMap();
    private final LongOpenHashSet dirtySourceChunks = new LongOpenHashSet();
    private final LongOpenHashSet scannedLightChunks = new LongOpenHashSet();
    private final Object chunkLifecycleLock = new Object();
    private final Long2ObjectOpenHashMap<PendingChunkScan> pendingChunkScans = new Long2ObjectOpenHashMap();
    private final LongArrayFIFOQueue pendingChunkScanOrder = new LongArrayFIFOQueue();
    private final LongOpenHashSet pendingChunkUnloads = new LongOpenHashSet();
    private final LongArrayFIFOQueue pendingChunkUnloadOrder = new LongArrayFIFOQueue();
    private final LongOpenHashSet pendingLoadedChunkInvalidations = new LongOpenHashSet();
    private final AtomicBoolean loadedChunkInvalidationRunning = new AtomicBoolean(false);
    private int loadedChunkInvalidationDelay = -1;
    private int loadedChunkInvalidationAge = 0;
    private int discoveryCenterChunkX = Integer.MIN_VALUE;
    private int discoveryCenterChunkZ = Integer.MIN_VALUE;
    private int discoveryRadius = -1;
    private int discoveryCursor = 0;
    private int discoveryRestartCooldown = 0;
    private int[] discoveryOffsets = new int[0];
    private static final float GPU_OWNERSHIP_ACQUIRE_WEIGHT = 0.995f;
    private static final float GPU_OWNERSHIP_RELEASE_WEIGHT = 0.9f;
    private volatile LongOpenHashSet gpuDirectOwnedSourceSnapshot = new LongOpenHashSet();
    private volatile boolean gpuDirectHybridApplied;
    private final LongOpenHashSet reflectiveSources = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> reflectiveByChunk = new Long2ObjectOpenHashMap();
    private final LongOpenHashSet dirtyReflectiveChunks = new LongOpenHashSet();
    private volatile Map<Long, long[]> reflectiveSnapshot = Collections.emptyMap();
    private final NeoFloodEngine engine = new NeoFloodEngine();
    private final AtomicBoolean sourceRefreshQueued = new AtomicBoolean(false);
    private final AtomicBoolean reflectiveRefreshQueued = new AtomicBoolean(false);
    private final Map<Long, BlockPos> closestTorchCache = new ConcurrentHashMap<Long, BlockPos>();
    private volatile Map<Long, long[]> sourceIndexSnapshot = Collections.emptyMap();
    private final Object pendingLightChangesLock = new Object();
    private final Long2ObjectOpenHashMap<long[]> pendingLightChangeBitsBySection = new Long2ObjectOpenHashMap();
    private int pendingLightChangeCount = 0;
    private final Object pendingRendererSectionsLock = new Object();
    private final Long2LongOpenHashMap pendingRendererSections = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap rendererSectionCooldownUntil = new Long2LongOpenHashMap();
    private static final long SECTION_REBUILD_COOLDOWN_MS = 100L;
    private static final long SECTION_UPDATE_DEBOUNCE_FAR_MS = 250L;
    private static final long SECTION_UPDATE_DEBOUNCE_MID_MS = 100L;
    private static final long SECTION_UPDATE_DEBOUNCE_NEAR_MS = 40L;
    private static final double SECTION_UPDATE_NEAR_DISTANCE_SQ = 1024.0;
    private static final double SECTION_UPDATE_MID_DISTANCE_SQ = 4096.0;
    private static final double SECTION_UPDATE_FAR_DISTANCE_SQ = 12544.0;
    private static final ExecutorService FRONTIER_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Luxium-Frontier");
        thread.setDaemon(true);
        return thread;
    });
    private static final int MAX_FRONTIER_CHECKS_PER_BATCH = 4096;
    private final Object frontierQueueLock = new Object();
    private final LongOpenHashSet pendingFrontierChecks = new LongOpenHashSet();
    private final AtomicBoolean frontierWorkerRunning = new AtomicBoolean(false);
    private final AtomicInteger frontierEpoch = new AtomicInteger(1);
    private volatile LongOpenHashSet frontierBlockSnapshot = new LongOpenHashSet();
    private volatile Map<Long, long[]> frontierSnapshot = Collections.emptyMap();
    private volatile boolean enabled = false;
    private int appliedWorldTracingWorkers = -1;
    private int appliedFloodRevision = -1;
    private boolean lastScanLight = false;
    private boolean lastScanReflective = false;
    private boolean lastLavaTracingEnabled = false;
    private boolean lastNeoGpuVanillaConfigured = false;
    private int entityUpdateCounter = 0;
    private ClientLevel lastLevel = null;
    private int warmupTicks = 0;

    private TorchRtxState() {
        this.lightSources.defaultReturnValue((byte)0);
        this.engine.setOnSectionsChanged(this::enqueueSectionsDirty);
        this.engine.setOnImmediateSectionsChanged(this::enqueueSectionsDirtyImmediate);
        this.engine.setOnLightChanges(this::enqueueLightChanges);
        this.engine.setOnHighPriorityTraceDone(() -> NeoShadowsEngine.get().notifyHighPriorityTraceDone());
    }

    public static TorchRtxState get() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isFloodEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        this.clearSourceCaches();
        this.clearFrontierCache();
        if (!enabled) {
            this.appliedWorldTracingWorkers = -1;
            this.appliedFloodRevision = -1;
            this.engine.clear();
        } else {
            this.warmupTicks = 40;
            this.syncFloodRtSettings();
            this.applyConfiguredWorldTracingWorkers((Integer)Config.CLIENT.rtxWorldTracingWorkers.get());
            LongIterator it = this.lightSources.keySet().iterator();
            while (it.hasNext()) {
                long k = it.nextLong();
                if (!this.shouldTraceSource(k)) continue;
                this.engine.addOrUpdateSource(k, this.lightSources.get(k) & 0xFF, false);
            }
        }
        this.triggerGlobalUpdate();
    }

    public void tick(@Nullable ClientLevel level) {
        boolean levelChanged;
        if (level == null) {
            this.lastLevel = null;
            return;
        }
        boolean bl = levelChanged = level != this.lastLevel;
        if (levelChanged) {
            this.lastLevel = level;
            this.warmupTicks = 40;
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                this.clearLightRegistry();
                this.lastScanLight = false;
            }
        }
        boolean gpuLightingEnabled = Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled);
        Config.GpuLocalLightingMode gpuMode = (Config.GpuLocalLightingMode)((Object)Config.CLIENT.gpuLocalLightingMode.get());
        boolean standaloneNeoFlood = Config.isFeatureEnabled(Config.CLIENT.rtxEnabled) && !gpuLightingEnabled;
        boolean hybridNeoFlood = gpuLightingEnabled && gpuMode == Config.GpuLocalLightingMode.HYBRID && Config.isFeatureEnabled(Config.CLIENT.rtxEnabled);
        boolean gpuNeoFloodOnly = gpuLightingEnabled && gpuMode == Config.GpuLocalLightingMode.NEOFLOOD_ONLY && Config.isFeatureEnabled(Config.CLIENT.rtxEnabled);
        boolean gpuFastSpreadVisibility = false;
        boolean realisticNeoFlood = Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) && (!gpuLightingEnabled || gpuMode == Config.GpuLocalLightingMode.NEOFLOOD_ONLY);
        boolean cfgEnabled = !NeoGpuVanilla.isConfiguredEnabled() && (standaloneNeoFlood || hybridNeoFlood || gpuNeoFloodOnly || gpuFastSpreadVisibility || realisticNeoFlood || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled));
        boolean cfgScanLight = TorchRtxState.shouldScanLightSources();
        boolean cfgReflection = Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled);
        boolean cfgLavaTracing = TorchRtxState.shouldTraceLava(level);
        int cfgWorldTracingWorkers = (Integer)Config.CLIENT.rtxWorldTracingWorkers.get();
        boolean neoGpuVanillaConfigured = NeoGpuVanilla.isConfiguredEnabled();
        if (this.lastNeoGpuVanillaConfigured != neoGpuVanillaConfigured) {
            this.lastNeoGpuVanillaConfigured = neoGpuVanillaConfigured;
            if (neoGpuVanillaConfigured) {
                this.forceNeoGpuVanillaSourceBootstrap(level);
            }
        } else if (levelChanged && neoGpuVanillaConfigured) {
            this.forceNeoGpuVanillaSourceBootstrap(level);
        }
        if (this.lastScanLight != cfgScanLight || this.lastScanReflective != cfgReflection) {
            boolean prevScanLight = this.lastScanLight;
            boolean prevScanReflective = this.lastScanReflective;
            this.lastScanLight = cfgScanLight;
            this.lastScanReflective = cfgReflection;
            this.handleScanModeChanged(level, prevScanLight, cfgScanLight, prevScanReflective, cfgReflection);
        }
        if (this.lastLavaTracingEnabled != cfgLavaTracing) {
            this.lastLavaTracingEnabled = cfgLavaTracing;
            if (cfgScanLight) {
                this.rescanLoadedChunks(level, true, cfgReflection);
            }
            this.triggerGlobalUpdate();
        }
        if (this.enabled != cfgEnabled) {
            this.setEnabled(cfgEnabled);
        }
        if (this.enabled && this.appliedWorldTracingWorkers != cfgWorldTracingWorkers) {
            this.applyConfiguredWorldTracingWorkers(cfgWorldTracingWorkers);
        }
        if (this.enabled) {
            this.syncFloodRtSettings();
        }
        Minecraft mc = Minecraft.m_91087_();
        if (this.enabled) {
            this.engine.setLevel(level);
            Vec3 camPos = null;
            if (mc.f_91074_ != null) {
                camPos = mc.f_91074_.m_20182_();
            } else if (mc.f_91075_ != null) {
                camPos = mc.f_91075_.m_20182_();
            } else if (mc.f_91063_ != null && mc.f_91063_.m_109153_() != null) {
                camPos = mc.f_91063_.m_109153_().m_90583_();
            }
            if (camPos != null) {
                this.engine.setPlayerPos(camPos);
            }
            if (this.warmupTicks > 0) {
                --this.warmupTicks;
                this.engine.boostNearestWorldSources(16);
            }
            this.discoverLoadedChunkSources(level, camPos);
            this.drainChunkLifecycle(level);
            this.tickLoadedChunkInvalidations();
            this.engine.tick();
        } else {
            Vec3 registryCenter = null;
            if (mc.f_91074_ != null) {
                registryCenter = mc.f_91074_.m_20182_();
            } else if (mc.f_91075_ != null) {
                registryCenter = mc.f_91075_.m_20182_();
            } else if (mc.f_91063_ != null && mc.f_91063_.m_109153_() != null) {
                registryCenter = mc.f_91063_.m_109153_().m_90583_();
            }
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                this.discoverLoadedChunkSources(level, registryCenter);
            }
            this.drainChunkLifecycle(level);
        }
        this.drainPendingSectionNotifications();
        this.drainPendingLightChanges();
        if (!this.enabled) {
            return;
        }
        if (this.usesEntityOcclusionShadows() && ++this.entityUpdateCounter >= 1) {
            this.entityUpdateCounter = 0;
            if (mc.f_91075_ != null) {
                EntityShadowManager.get().updateAsync(level, mc.f_91075_.m_20182_(), 48.0);
            }
        }
    }

    public boolean isImmediateStateChange(BlockPos pos) {
        double dz;
        double dy;
        Minecraft mc = Minecraft.m_91087_();
        Vec3 observer = null;
        if (mc.f_91074_ != null) {
            observer = mc.f_91074_.m_20182_();
        } else if (mc.f_91075_ != null) {
            observer = mc.f_91075_.m_20182_();
        } else if (mc.f_91063_ != null && mc.f_91063_.m_109153_() != null) {
            observer = mc.f_91063_.m_109153_().m_90583_();
        }
        if (observer == null) {
            return false;
        }
        double dx = (double)pos.m_123341_() + 0.5 - observer.f_82479_;
        return dx * dx + (dy = (double)pos.m_123342_() + 0.5 - observer.f_82480_) * dy + (dz = (double)pos.m_123343_() + 0.5 - observer.f_82481_) * dz <= 4096.0;
    }

    public void trackBlockTransition(ClientLevel level, BlockPos pos, BlockState oldState, BlockState newState, boolean immediate, boolean geometryChanged) {
        if (level == null || pos == null || newState == null) {
            return;
        }
        boolean scanLight = TorchRtxState.shouldScanLightSources();
        boolean scanReflective = TorchRtxState.shouldScanReflectiveSources();
        int oldEmission = oldState == null ? 0 : oldState.getLightEmission((BlockGetter)level, pos);
        int emission = newState.getLightEmission((BlockGetter)level, pos);
        boolean oldRtxLightSource = oldState != null && oldEmission > 0 && TorchRtxState.shouldRegisterRtxSource(level, oldState);
        boolean rtxLightSource = emission > 0 && TorchRtxState.shouldRegisterRtxSource(level, newState);
        long key = pos.m_121878_();
        boolean registeredBefore = this.lightSources.containsKey(key);
        boolean vanillaEmissionChanged = oldEmission != emission;
        boolean vanillaLightPropertiesChanged = geometryChanged && NeoGpuVanilla.isConfiguredEnabled() && LightEngine.m_284387_((BlockGetter)level, (BlockPos)pos, (BlockState)oldState, (BlockState)newState);
        RainPuddleManager.get().removeIfUnsupported(level, pos, newState);
        boolean reflChanged = false;
        boolean reflNow = scanReflective && ReflectionMaterialRegistry.find(newState) != null;
        boolean reflWas = this.reflectiveSources.contains(key);
        if (reflNow && !reflWas) {
            this.reflectiveSources.add(key);
            this.indexReflectiveSource(key);
            reflChanged = true;
        } else if (!reflNow && reflWas) {
            this.reflectiveSources.remove(key);
            this.unindexReflectiveSource(key);
            reflChanged = true;
        }
        boolean posChanged = false;
        boolean emissionChanged = false;
        if (scanLight && rtxLightSource) {
            byte current = this.lightSources.get(key);
            if (current != (byte)emission) {
                this.lightSources.put(key, (byte)emission);
                if (current == this.lightSources.defaultReturnValue()) {
                    this.indexSource(key);
                    posChanged = true;
                }
                emissionChanged = true;
            }
        } else if (scanLight && this.lightSources.containsKey(key)) {
            this.lightSources.remove(key);
            this.unindexSource(key);
            posChanged = true;
            emissionChanged = true;
        }
        if (posChanged) {
            this.clearSourceCaches();
            this.refreshSourceSnapshot();
        }
        if (reflChanged) {
            this.refreshReflectiveSnapshot();
        }
        if (posChanged || emissionChanged || reflChanged) {
            ReflectionSystem.get().markWorldDirty();
        }
        if (posChanged || emissionChanged) {
            NeoShadowsEngine.markLightingDirty();
        }
        if (geometryChanged) {
            NeoShadowsEngine.markGeometryDirty();
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                NeoGpuVanilla.onGeometryChanged(pos, vanillaLightPropertiesChanged);
            } else {
                GpuShadowCache.get().onGeometryChanged(pos);
            }
            if (this.enabled) {
                this.engine.onBlockChanged(pos, level, immediate);
            }
        }
        if (NeoGpuVanilla.isConfiguredEnabled() && vanillaEmissionChanged) {
            if (oldRtxLightSource) {
                GpuShadowCache.get().onSourceRemoved(key);
            }
            NeoGpuVanilla.onSourceRegistryChanged();
        }
        if (this.enabled) {
            if (rtxLightSource && this.shouldTraceSource(key)) {
                if (vanillaEmissionChanged || emissionChanged || !registeredBefore || geometryChanged) {
                    this.engine.ensureSourceScheduled(key, emission, immediate);
                }
            } else if (oldRtxLightSource || registeredBefore || emissionChanged) {
                GpuShadowCache.get().onSourceRemoved(key);
                this.engine.removeSource(key, immediate);
            }
        }
    }

    public void trackBlock(BlockPos pos, BlockState state, boolean immediate) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level != null) {
            this.trackBlockTransition(level, pos, null, state, immediate, true);
        }
    }

    public void addChunkLightSources(ClientLevel level, LevelChunk chunk) {
        if (level == null || chunk == null || !TorchRtxState.shouldScanAnySources()) {
            return;
        }
        if (NeoGpuVanilla.isConfiguredEnabled() && TorchRtxState.shouldScanLightSources()) {
            this.registerVanillaChunkLightSourcesNow(level, chunk);
            if (TorchRtxState.shouldScanReflectiveSources()) {
                this.queueChunkScan(level, chunk, true, false, true);
            }
            return;
        }
        this.queueChunkScan(level, chunk, true);
    }

    private void registerVanillaChunkLightSourcesNow(ClientLevel level, LevelChunk chunk) {
        LongIterator it;
        boolean changed;
        if (level == null || chunk == null || !NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
        long chunkKey = ChunkPos.m_45589_((int)chunk.m_7697_().f_45578_, (int)chunk.m_7697_().f_45579_);
        Long2ByteOpenHashMap discovered = new Long2ByteOpenHashMap();
        discovered.defaultReturnValue((byte)0);
        chunk.m_284254_((pos, state) -> {
            int emission = state.getLightEmission((BlockGetter)level, pos);
            if (emission > 0 && TorchRtxState.shouldRegisterRtxSource(level, state)) {
                discovered.put(pos.m_121878_(), (byte)emission);
            }
        });
        LongOpenHashSet previous = (LongOpenHashSet)this.sourcesByChunk.get(chunkKey);
        LongOpenHashSet next = new LongOpenHashSet((LongCollection)discovered.keySet());
        LongOpenHashSet removed = previous == null ? new LongOpenHashSet() : new LongOpenHashSet((LongCollection)previous);
        removed.removeAll((LongCollection)next);
        boolean bl = changed = !removed.isEmpty();
        if (!removed.isEmpty()) {
            it = removed.iterator();
            while (it.hasNext()) {
                this.lightSources.remove(it.nextLong());
            }
            GpuShadowCache.get().onSourcesRemoved(removed);
        }
        ObjectIterator<Long2ByteMap.Entry> entries = discovered.long2ByteEntrySet().fastIterator();
        while (entries.hasNext()) {
            int oldEmission;
            Long2ByteMap.Entry entry = entries.next();
            long key = entry.getLongKey();
            byte emission = entry.getByteValue();
            int n = oldEmission = this.lightSources.containsKey(key) ? this.lightSources.get(key) & 0xFF : -1;
            if (oldEmission != (emission & 0xFF) || previous == null || !previous.contains(key)) {
                changed = true;
            }
            this.lightSources.put(key, emission);
        }
        if (next.isEmpty()) {
            this.sourcesByChunk.remove(chunkKey);
        } else {
            this.sourcesByChunk.put(chunkKey, (Object)next);
        }
        this.scannedLightChunks.add(chunkKey);
        if (!changed) {
            return;
        }
        this.dirtySourceChunks.add(chunkKey);
        this.clearSourceCaches();
        this.refreshSourceSnapshot();
        NeoShadowsEngine.markLightingDirty();
        ReflectionSystem.get().markWorldDirty();
        NeoGpuVanilla.onChunkSourceRegistryChanged();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeChunkLightSources(int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.m_45589_((int)chunkX, (int)chunkZ);
        Object object = this.chunkLifecycleLock;
        synchronized (object) {
            this.pendingChunkScans.remove(chunkKey);
            this.scannedLightChunks.remove(chunkKey);
            if (this.pendingChunkUnloads.add(chunkKey)) {
                this.pendingChunkUnloadOrder.enqueue(chunkKey);
            }
        }
    }

    public int sampleLight(BlockPos pos) {
        return this.sampleLight(pos.m_121878_());
    }

    public int sampleLight(long posKey) {
        if (!this.enabled) {
            return -1;
        }
        if (!this.engine.isReady()) {
            return -1;
        }
        return this.engine.getLight(posKey);
    }

    public boolean fillLightVolume(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, byte[] output) {
        if (output == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return false;
        }
        int required = sizeX * sizeY * sizeZ;
        if (output.length < required) {
            throw new IllegalArgumentException("Output buffer is smaller than requested light volume");
        }
        if (!this.enabled || !this.engine.isReady()) {
            Arrays.fill(output, 0, required, (byte)0);
            return false;
        }
        this.engine.fillLightVolume(minX, minY, minZ, sizeX, sizeY, sizeZ, output);
        return true;
    }

    public long getLightSnapshotVersion() {
        return this.engine.getLightSnapshotVersion();
    }

    public boolean hasAnyLight(long posKey) {
        if (!this.enabled || !this.engine.isReady()) {
            return false;
        }
        return this.engine.hasAnyLight(posKey);
    }

    public boolean anySourceNear(long posKey, int extraRadius) {
        if (!this.enabled || !this.engine.isReady()) {
            return false;
        }
        return this.engine.anySourceNear(posKey, extraRadius);
    }

    public long findNearestLitSample(long basePosKey, int axisUX, int axisUY, int axisUZ, int axisVX, int axisVY, int axisVZ, int searchRadius, double sampleX, double sampleY, double sampleZ) {
        if (!this.enabled || !this.engine.isReady()) {
            return Long.MIN_VALUE;
        }
        return this.engine.findNearestLitSample(basePosKey, axisUX, axisUY, axisUZ, axisVX, axisVY, axisVZ, searchRadius, sampleX, sampleY, sampleZ);
    }

    public int sampleCoverage(BlockPos pos) {
        return this.sampleCoverage(pos.m_121878_());
    }

    public int sampleCoverage(long posKey) {
        if (!this.enabled) {
            return -1;
        }
        if (!this.engine.isReady()) {
            return -1;
        }
        return this.engine.getCoverage(posKey);
    }

    public int getRegisteredLightSourceCount() {
        return this.lightSources.size();
    }

    public void warmUpNearestSources(ClientLevel level, BlockPos center) {
        if (!this.enabled || level == null) {
            return;
        }
        this.engine.traceNearestSourcesSync(16, level, Vec3.m_82512_((Vec3i)center));
    }

    public long getDominantSource(BlockPos pos) {
        if (!this.enabled) {
            return Long.MIN_VALUE;
        }
        if (!this.engine.isReady()) {
            return Long.MIN_VALUE;
        }
        return this.engine.getDominantSource(pos);
    }

    public long getDominantSource(long posKey) {
        if (!this.enabled) {
            return Long.MIN_VALUE;
        }
        if (!this.engine.isReady()) {
            return Long.MIN_VALUE;
        }
        return this.engine.getDominantSource(posKey);
    }

    public void forEachContributor(long posKey, NeoFloodEngine.ContributorVisitor visitor) {
        if (!this.enabled || !this.engine.isReady() || visitor == null) {
            return;
        }
        this.engine.forEachContributor(posKey, visitor);
    }

    public boolean sourceContributesTo(long sourceKey, long posKey) {
        if (!this.enabled || !this.engine.isReady()) {
            return false;
        }
        return this.engine.sourceContributesTo(sourceKey, posKey);
    }

    public boolean isOccludedByEntity(long targetKey) {
        if (!this.usesEntityOcclusionShadows()) {
            return false;
        }
        return EntityShadowManager.get().isBlockInShadow(targetKey);
    }

    public int sampleFloodLight(BlockPos pos) {
        return this.sampleLight(pos);
    }

    public boolean isDirectlyLit(BlockPos pos) {
        return this.isDirectlyLit(pos.m_121878_());
    }

    public boolean isDirectlyLit(long posKey) {
        return this.enabled && this.engine.isReady() && this.engine.isDirectlyLit(posKey, 72);
    }

    public boolean isRtxFrontier(BlockPos pos) {
        return this.enabled && this.engine.isReady() && this.frontierBlockSnapshot.contains(pos.m_121878_());
    }

    public boolean isRtxFrontier(long posKey) {
        return this.enabled && this.engine.isReady() && this.frontierBlockSnapshot.contains(posKey);
    }

    public boolean isNearRtxFrontier(BlockPos pos) {
        if (!this.enabled || !this.engine.isReady() || this.frontierBlockSnapshot.isEmpty()) {
            return false;
        }
        if (this.frontierBlockSnapshot.contains(pos.m_121878_())) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; ++dy) {
            for (int dz = -2; dz <= 2; ++dz) {
                for (int dx = -2; dx <= 2; ++dx) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2) continue;
                    cursor.m_122178_(pos.m_123341_() + dx, pos.m_123342_() + dy, pos.m_123343_() + dz);
                    if (!this.frontierBlockSnapshot.contains(cursor.m_121878_())) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isNearRtxFrontier(long posKey) {
        if (!this.enabled || !this.engine.isReady() || this.frontierBlockSnapshot.isEmpty()) {
            return false;
        }
        if (this.frontierBlockSnapshot.contains(posKey)) {
            return true;
        }
        int baseX = BlockPos.m_121983_((long)posKey);
        int baseY = BlockPos.m_122008_((long)posKey);
        int baseZ = BlockPos.m_122015_((long)posKey);
        for (int dy = -2; dy <= 2; ++dy) {
            for (int dz = -2; dz <= 2; ++dz) {
                for (int dx = -2; dx <= 2; ++dx) {
                    long nearbyKey;
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2 || !this.frontierBlockSnapshot.contains(nearbyKey = BlockPos.m_121882_((int)(baseX + dx), (int)(baseY + dy), (int)(baseZ + dz)))) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public long[] getFrontierPositions(BlockPos origin, double maxDistanceSq) {
        Map<Long, long[]> snap = this.frontierSnapshot;
        if (!this.enabled || !this.engine.isReady() || snap.isEmpty()) {
            return new long[0];
        }
        int centerChunkX = SectionPos.m_123171_((int)origin.m_123341_());
        int centerChunkZ = SectionPos.m_123171_((int)origin.m_123343_());
        int chunkRadius = Math.max(1, (int)Math.ceil(Math.sqrt(maxDistanceSq) / 16.0));
        double ox = (double)origin.m_123341_() + 0.5;
        double oy = (double)origin.m_123342_() + 0.5;
        double oz = (double)origin.m_123343_() + 0.5;
        LongArrayList matches = new LongArrayList();
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                long[] bucket = snap.get(ChunkPos.m_45589_((int)(centerChunkX + dx), (int)(centerChunkZ + dz)));
                if (bucket == null || bucket.length == 0) continue;
                for (long key : bucket) {
                    double ddz;
                    double ddy;
                    double ddx = (double)BlockPos.m_121983_((long)key) + 0.5 - ox;
                    if (!(ddx * ddx + (ddy = (double)BlockPos.m_122008_((long)key) + 0.5 - oy) * ddy + (ddz = (double)BlockPos.m_122015_((long)key) + 0.5 - oz) * ddz <= maxDistanceSq)) continue;
                    matches.add(key);
                }
            }
        }
        return matches.toLongArray();
    }

    public boolean usesEntityOcclusionShadows() {
        return this.enabled && TorchRtxState.shouldUseEntityOcclusionShadows();
    }

    public void handleBlockRemoval(BlockPos pos) {
        long key = pos.m_121878_();
        if (this.lightSources.containsKey(key)) {
            this.lightSources.remove(key);
            this.unindexSource(key);
            this.clearSourceCaches();
            this.refreshSourceSnapshot();
            NeoShadowsEngine.markLightingDirty();
        }
        GpuShadowCache.get().onSourceRemoved(key);
        if (this.enabled) {
            this.engine.removeSource(key);
        }
    }

    @Nullable
    public BlockPos getClosestSource(BlockPos origin, double maxDistanceSq) {
        long cacheKey = origin.m_121878_();
        BlockPos cached = this.closestTorchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<Long, long[]> snap = this.sourceIndexSnapshot;
        if (snap.isEmpty()) {
            return null;
        }
        BlockPos closest = null;
        double best = maxDistanceSq;
        int ccX = SectionPos.m_123171_((int)origin.m_123341_());
        int ccZ = SectionPos.m_123171_((int)origin.m_123343_());
        int cr = Math.max(1, (int)Math.ceil(Math.sqrt(maxDistanceSq) / 16.0));
        double ox = origin.m_123341_();
        double oy = origin.m_123342_();
        double oz = origin.m_123343_();
        block0: for (int dz = -cr; dz <= cr; ++dz) {
            for (int dx = -cr; dx <= cr; ++dx) {
                long[] b = snap.get(ChunkPos.m_45589_((int)(ccX + dx), (int)(ccZ + dz)));
                if (b == null) continue;
                for (long sk : b) {
                    double sz;
                    double sy;
                    double sx = (double)BlockPos.m_121983_((long)sk) - ox;
                    double d = sx * sx + (sy = (double)BlockPos.m_122008_((long)sk) - oy) * sy + (sz = (double)BlockPos.m_122015_((long)sk) - oz) * sz;
                    if (!(d < best)) continue;
                    best = d;
                    closest = BlockPos.m_122022_((long)sk);
                    if (best < 1.0) break block0;
                }
            }
        }
        if (closest != null && this.closestTorchCache.size() < 20000) {
            this.closestTorchCache.put(cacheKey, closest);
        }
        return closest;
    }

    public LongOpenHashSet getTorchPositionsView() {
        LongOpenHashSet s = new LongOpenHashSet();
        s.addAll((LongCollection)this.lightSources.keySet());
        return s;
    }

    public Map<Long, long[]> getLightSourceSnapshotView() {
        return this.sourceIndexSnapshot;
    }

    public synchronized void setGpuDirectSources(long[] sourceKeys, float[] weights, int count) {
        long sourceKey;
        LongOpenHashSet previous = this.gpuDirectOwnedSourceSnapshot;
        boolean hybridTracing = this.enabled && Config.isFeatureEnabled(Config.CLIENT.rtxEnabled) && Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) && Config.CLIENT.gpuLocalLightingMode.get() == Config.GpuLocalLightingMode.HYBRID;
        LongOpenHashSet next = new LongOpenHashSet();
        if (hybridTracing && sourceKeys != null && weights != null) {
            int limit = Math.min(Math.max(count, 0), Math.min(sourceKeys.length, weights.length));
            for (int i = 0; i < limit; ++i) {
                float threshold;
                sourceKey = sourceKeys[i];
                float weight = Mth.m_14036_((float)weights[i], (float)0.0f, (float)1.0f);
                float f = threshold = previous.contains(sourceKey) ? 0.9f : 0.995f;
                if (!(weight >= threshold)) continue;
                next.add(sourceKey);
            }
        }
        if (previous.equals((Object)next) && this.gpuDirectHybridApplied == hybridTracing) {
            return;
        }
        if (this.enabled) {
            LongIterator oldIterator = previous.iterator();
            while (oldIterator.hasNext()) {
                long sourceKey2 = oldIterator.nextLong();
                if (next.contains(sourceKey2) || !this.lightSources.containsKey(sourceKey2)) continue;
                this.engine.addOrUpdateSource(sourceKey2, this.lightSources.get(sourceKey2) & 0xFF, false);
            }
            LongIterator newIterator = next.iterator();
            while (newIterator.hasNext()) {
                sourceKey = newIterator.nextLong();
                if (previous.contains(sourceKey)) continue;
                this.engine.removeSource(sourceKey);
            }
        }
        this.gpuDirectOwnedSourceSnapshot = next;
        this.gpuDirectHybridApplied = hybridTracing;
    }

    public void clearGpuDirectSources() {
        this.setGpuDirectSources(null, null, 0);
    }

    public int sampleLightForRender(BlockPos pos) {
        return this.sampleLight(pos);
    }

    public int sampleLightForRender(long posKey) {
        return this.sampleLight(posKey);
    }

    public int getSourceEmission(long sourceKey) {
        return this.lightSources.get(sourceKey) & 0xFF;
    }

    public long[] getClosestSources(Vec3 origin, double maxDistanceSq, int limit) {
        if (limit <= 0) {
            return new long[0];
        }
        if (NeoGpuVanilla.isConfiguredEnabled()) {
            return this.getClosestSourcesLive(origin, maxDistanceSq, limit);
        }
        Map<Long, long[]> snap = this.sourceIndexSnapshot;
        if (snap.isEmpty()) {
            return new long[0];
        }
        long[] bestKeys = new long[limit];
        double[] bestDistances = new double[limit];
        int size = 0;
        int centerChunkX = SectionPos.m_123171_((int)Mth.m_14107_((double)origin.f_82479_));
        int centerChunkZ = SectionPos.m_123171_((int)Mth.m_14107_((double)origin.f_82481_));
        int chunkRadius = Math.max(1, (int)Math.ceil(Math.sqrt(maxDistanceSq) / 16.0));
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                long[] bucket = snap.get(ChunkPos.m_45589_((int)(centerChunkX + dx), (int)(centerChunkZ + dz)));
                if (bucket == null) continue;
                for (long sourceKey : bucket) {
                    double sz;
                    double sy;
                    double sx = (double)BlockPos.m_121983_((long)sourceKey) + 0.5 - origin.f_82479_;
                    double distanceSq = sx * sx + (sy = (double)BlockPos.m_122008_((long)sourceKey) + 0.5 - origin.f_82480_) * sy + (sz = (double)BlockPos.m_122015_((long)sourceKey) + 0.5 - origin.f_82481_) * sz;
                    if (distanceSq > maxDistanceSq) continue;
                    if (size < limit) {
                        bestKeys[size] = sourceKey;
                        bestDistances[size] = distanceSq;
                        ++size;
                        continue;
                    }
                    int worstIndex = 0;
                    double worstDistance = bestDistances[0];
                    long worstKey = bestKeys[0];
                    for (int index = 1; index < size; ++index) {
                        if (!(bestDistances[index] > worstDistance) && (Double.compare(bestDistances[index], worstDistance) != 0 || bestKeys[index] <= worstKey)) continue;
                        worstDistance = bestDistances[index];
                        worstKey = bestKeys[index];
                        worstIndex = index;
                    }
                    if (!(distanceSq < worstDistance) && (Double.compare(distanceSq, worstDistance) != 0 || sourceKey >= worstKey)) continue;
                    bestKeys[worstIndex] = sourceKey;
                    bestDistances[worstIndex] = distanceSq;
                }
            }
        }
        for (int i = 0; i < size - 1; ++i) {
            for (int j = i + 1; j < size; ++j) {
                if (!(bestDistances[j] < bestDistances[i]) && (Double.compare(bestDistances[j], bestDistances[i]) != 0 || bestKeys[j] >= bestKeys[i])) continue;
                double tempDistance = bestDistances[i];
                bestDistances[i] = bestDistances[j];
                bestDistances[j] = tempDistance;
                long tempKey = bestKeys[i];
                bestKeys[i] = bestKeys[j];
                bestKeys[j] = tempKey;
            }
        }
        long[] result = new long[size];
        System.arraycopy(bestKeys, 0, result, 0, size);
        return result;
    }

    private long[] getClosestSourcesLive(Vec3 origin, double maxDistanceSq, int limit) {
        if (this.sourcesByChunk.isEmpty()) {
            return new long[0];
        }
        long[] bestKeys = new long[limit];
        double[] bestDistances = new double[limit];
        int size = 0;
        int centerChunkX = SectionPos.m_123171_((int)Mth.m_14107_((double)origin.f_82479_));
        int centerChunkZ = SectionPos.m_123171_((int)Mth.m_14107_((double)origin.f_82481_));
        int chunkRadius = Math.max(1, (int)Math.ceil(Math.sqrt(maxDistanceSq) / 16.0));
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(ChunkPos.m_45589_((int)(centerChunkX + dx), (int)(centerChunkZ + dz)));
                if (bucket == null || bucket.isEmpty()) continue;
                LongIterator it = bucket.iterator();
                while (it.hasNext()) {
                    double sz;
                    double sy;
                    long sourceKey = it.nextLong();
                    double sx = (double)BlockPos.m_121983_((long)sourceKey) + 0.5 - origin.f_82479_;
                    double distanceSq = sx * sx + (sy = (double)BlockPos.m_122008_((long)sourceKey) + 0.5 - origin.f_82480_) * sy + (sz = (double)BlockPos.m_122015_((long)sourceKey) + 0.5 - origin.f_82481_) * sz;
                    if (distanceSq > maxDistanceSq) continue;
                    if (size < limit) {
                        bestKeys[size] = sourceKey;
                        bestDistances[size] = distanceSq;
                        ++size;
                        continue;
                    }
                    int worstIndex = 0;
                    double worstDistance = bestDistances[0];
                    long worstKey = bestKeys[0];
                    for (int index = 1; index < size; ++index) {
                        if (!(bestDistances[index] > worstDistance) && (Double.compare(bestDistances[index], worstDistance) != 0 || bestKeys[index] <= worstKey)) continue;
                        worstDistance = bestDistances[index];
                        worstKey = bestKeys[index];
                        worstIndex = index;
                    }
                    if (!(distanceSq < worstDistance) && (Double.compare(distanceSq, worstDistance) != 0 || sourceKey >= worstKey)) continue;
                    bestKeys[worstIndex] = sourceKey;
                    bestDistances[worstIndex] = distanceSq;
                }
            }
        }
        for (int i = 0; i < size - 1; ++i) {
            for (int j = i + 1; j < size; ++j) {
                if (!(bestDistances[j] < bestDistances[i]) && (Double.compare(bestDistances[j], bestDistances[i]) != 0 || bestKeys[j] >= bestKeys[i])) continue;
                double td = bestDistances[i];
                bestDistances[i] = bestDistances[j];
                bestDistances[j] = td;
                long tk = bestKeys[i];
                bestKeys[i] = bestKeys[j];
                bestKeys[j] = tk;
            }
        }
        return Arrays.copyOf(bestKeys, size);
    }

    public NeoFloodEngine getEngine() {
        return this.engine;
    }

    public Map<Long, long[]> getReflectiveSnapshotView() {
        return this.reflectiveSnapshot;
    }

    private void applySectionsDirty(LongOpenHashSet sections) {
        if (sections.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91060_ == null) {
            return;
        }
        LongIterator si = sections.iterator();
        while (si.hasNext()) {
            long sec = si.nextLong();
            int bx = SectionPos.m_123223_((int)SectionPos.m_123213_((long)sec));
            int by = SectionPos.m_123223_((int)SectionPos.m_123225_((long)sec));
            int bz = SectionPos.m_123223_((int)SectionPos.m_123230_((long)sec));
            mc.f_91060_.m_109494_(bx, by, bz, bx + 15, by + 15, bz + 15);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void enqueueSectionsDirty(LongOpenHashSet sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.m_91087_();
        Vec3 viewer = mc.f_91063_ != null && mc.f_91063_.m_109153_() != null ? mc.f_91063_.m_109153_().m_90583_() : (mc.f_91074_ != null ? mc.f_91074_.m_20182_() : Vec3.f_82478_);
        Object object = this.pendingRendererSectionsLock;
        synchronized (object) {
            LongIterator it = sections.iterator();
            while (it.hasNext()) {
                long previous;
                long eligibleAt;
                long section = it.nextLong();
                long cooldown = this.rendererSectionCooldownUntil.getOrDefault(section, 0L);
                if (cooldown <= now) {
                    this.rendererSectionCooldownUntil.remove(section);
                    cooldown = 0L;
                }
                if ((eligibleAt = Math.max(now + TorchRtxState.rendererDelayForSection(section, viewer), cooldown)) >= (previous = this.pendingRendererSections.getOrDefault(section, Long.MAX_VALUE))) continue;
                this.pendingRendererSections.put(section, eligibleAt);
            }
        }
    }

    private void enqueueSectionsDirtyImmediate(LongOpenHashSet sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LongOpenHashSet atomicSections = new LongOpenHashSet((LongCollection)sections);
        mc.execute(() -> {
            long now = System.currentTimeMillis();
            Object object = this.pendingRendererSectionsLock;
            synchronized (object) {
                LongIterator it = atomicSections.iterator();
                while (it.hasNext()) {
                    long section = it.nextLong();
                    this.pendingRendererSections.remove(section);
                    this.rendererSectionCooldownUntil.put(section, now + 100L);
                }
            }
            this.applySectionsDirty(atomicSections);
            if (mc.f_91063_ != null && mc.f_91063_.m_109154_() != null) {
                ((LightTextureAccessor)mc.f_91063_.m_109154_()).luxium$setUpdateLightTexture(true);
            }
        });
    }

    private static long rendererDelayForSection(long section, Vec3 viewer) {
        double distanceSq = TorchRtxState.sectionDistanceSq(section, viewer);
        if (distanceSq <= 1024.0) {
            return 0L;
        }
        if (distanceSq <= 4096.0) {
            return 40L;
        }
        if (distanceSq <= 12544.0) {
            return 100L;
        }
        return 250L;
    }

    private static double sectionDistanceSq(long section, Vec3 viewer) {
        double cx = (double)SectionPos.m_123223_((int)SectionPos.m_123213_((long)section)) + 8.0 - viewer.f_82479_;
        double cy = (double)SectionPos.m_123223_((int)SectionPos.m_123225_((long)section)) + 8.0 - viewer.f_82480_;
        double cz = (double)SectionPos.m_123223_((int)SectionPos.m_123230_((long)section)) + 8.0 - viewer.f_82481_;
        return cx * cx + cy * cy + cz * cz;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void enqueueLightChanges(NeoFloodEngine.LightChangeBatch changes) {
        if (changes == null || changes.sectionCount() == 0) {
            return;
        }
        Object object = this.pendingLightChangesLock;
        synchronized (object) {
            changes.forEachSection((sectionKey, incomingWords) -> {
                long[] target = (long[])this.pendingLightChangeBitsBySection.get(sectionKey);
                if (target == null) {
                    target = new long[64];
                    this.pendingLightChangeBitsBySection.put(sectionKey, (Object)target);
                }
                for (int i = 0; i < 64; ++i) {
                    long added = incomingWords[i] & (target[i] ^ 0xFFFFFFFFFFFFFFFFL);
                    if (added == 0L) continue;
                    int n = i;
                    target[n] = target[n] | added;
                    this.pendingLightChangeCount += Long.bitCount(added);
                }
            });
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drainPendingSectionNotifications() {
        long now = System.currentTimeMillis();
        LongOpenHashSet batch = new LongOpenHashSet(16);
        Object object = this.pendingRendererSectionsLock;
        synchronized (object) {
            if (this.pendingRendererSections.isEmpty()) {
                return;
            }
            ObjectIterator it = this.pendingRendererSections.long2LongEntrySet().fastIterator();
            while (it.hasNext() && batch.size() < 16) {
                Long2LongMap.Entry entry = (Long2LongMap.Entry)it.next();
                if (now < entry.getLongValue()) continue;
                batch.add(entry.getLongKey());
                this.rendererSectionCooldownUntil.put(entry.getLongKey(), now + 100L);
                it.remove();
            }
        }
        if (!batch.isEmpty()) {
            this.applySectionsDirty(batch);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drainPendingLightChanges() {
        long started = System.nanoTime();
        long extractionDeadline = started + Math.min(250000L, 250000L);
        long deadline = started + 1000000L;
        LongArrayList changed = new LongArrayList(192);
        Object object = this.pendingLightChangesLock;
        synchronized (object) {
            if (this.pendingLightChangeCount <= 0 || this.pendingLightChangeBitsBySection.isEmpty()) {
                return;
            }
            ObjectIterator sectionIt = this.pendingLightChangeBitsBySection.long2ObjectEntrySet().fastIterator();
            for (int sectionsProcessed = 0; sectionIt.hasNext() && sectionsProcessed < 48 && changed.size() < 192 && System.nanoTime() < extractionDeadline; ++sectionsProcessed) {
                Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)sectionIt.next();
                long sectionKey = entry.getLongKey();
                long[] words = (long[])entry.getValue();
                int baseX = SectionPos.m_123223_((int)SectionPos.m_123213_((long)sectionKey));
                int baseY = SectionPos.m_123223_((int)SectionPos.m_123225_((long)sectionKey));
                int baseZ = SectionPos.m_123223_((int)SectionPos.m_123230_((long)sectionKey));
                for (int wordIndex = 0; wordIndex < 64 && changed.size() < 192 && System.nanoTime() < extractionDeadline; ++wordIndex) {
                    long bits = words[wordIndex];
                    while (bits != 0L && changed.size() < 192 && System.nanoTime() < extractionDeadline) {
                        int bit = Long.numberOfTrailingZeros(bits);
                        int localIndex = (wordIndex << 6) + bit;
                        changed.add(BlockPos.m_121882_((int)(baseX + (localIndex & 0xF)), (int)(baseY + (localIndex >>> 8 & 0xF)), (int)(baseZ + (localIndex >>> 4 & 0xF))));
                        int n = wordIndex;
                        words[n] = words[n] & (1L << bit ^ 0xFFFFFFFFFFFFFFFFL);
                        bits &= bits - 1L;
                        --this.pendingLightChangeCount;
                    }
                }
                boolean empty = true;
                for (long word : words) {
                    if (word == 0L) continue;
                    empty = false;
                    break;
                }
                if (!empty) continue;
                sectionIt.remove();
            }
            if (this.pendingLightChangeCount < 0) {
                this.pendingLightChangeCount = 0;
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        this.applyLightChangesBudgeted(changed, deadline);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void applyLightChangesBudgeted(LongArrayList changedBlocks, long deadline) {
        int processed;
        if (!this.enabled || !this.engine.isReady()) {
            this.clearFrontierCache();
            NeoShadowsEngine.get().onLightingChanged();
            return;
        }
        LongOpenHashSet affected = new LongOpenHashSet(changedBlocks.size() * 7);
        for (processed = 0; processed < changedBlocks.size() && (processed <= 0 || System.nanoTime() < deadline); ++processed) {
            long key = changedBlocks.getLong(processed);
            int x = BlockPos.m_121983_((long)key);
            int y = BlockPos.m_122008_((long)key);
            int z = BlockPos.m_122015_((long)key);
            affected.add(key);
            for (Direction dir : FACES) {
                affected.add(BlockPos.m_121882_((int)(x + dir.m_122429_()), (int)(y + dir.m_122430_()), (int)(z + dir.m_122431_())));
            }
        }
        if (processed < changedBlocks.size()) {
            Object object = this.pendingLightChangesLock;
            synchronized (object) {
                for (int i = processed; i < changedBlocks.size(); ++i) {
                    this.requeueLightChangeLocked(changedBlocks.getLong(i));
                }
            }
        }
        if (!affected.isEmpty()) {
            this.enqueueFrontierChecks(affected);
            NeoShadowsEngine.get().onLightingChanged(affected, false);
            NeoCpuShadowsEngine.onLightDataChanged();
        }
    }

    private void requeueLightChangeLocked(long posKey) {
        long mask;
        int localIndex;
        int wordIndex;
        long sectionKey = SectionPos.m_123209_((int)(BlockPos.m_121983_((long)posKey) >> 4), (int)(BlockPos.m_122008_((long)posKey) >> 4), (int)(BlockPos.m_122015_((long)posKey) >> 4));
        long[] words = (long[])this.pendingLightChangeBitsBySection.get(sectionKey);
        if (words == null) {
            words = new long[64];
            this.pendingLightChangeBitsBySection.put(sectionKey, (Object)words);
        }
        if ((words[wordIndex = (localIndex = (BlockPos.m_122008_((long)posKey) & 0xF) << 8 | (BlockPos.m_122015_((long)posKey) & 0xF) << 4 | BlockPos.m_121983_((long)posKey) & 0xF) >>> 6] & (mask = 1L << (localIndex & 0x3F))) == 0L) {
            int n = wordIndex;
            words[n] = words[n] | mask;
            ++this.pendingLightChangeCount;
        }
    }

    public void scanChunkLightSources(ClientLevel level, LevelChunk chunk) {
        this.queueChunkScan(level, chunk, true);
    }

    private void queueChunkScan(ClientLevel level, LevelChunk chunk, boolean force) {
        this.queueChunkScan(level, chunk, force, TorchRtxState.shouldScanLightSources(), TorchRtxState.shouldScanReflectiveSources());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void queueChunkScan(ClientLevel level, LevelChunk chunk, boolean force, boolean scanLight, boolean scanReflective) {
        if (level == null || chunk == null || !scanLight && !scanReflective) {
            return;
        }
        long chunkKey = ChunkPos.m_45589_((int)chunk.m_7697_().f_45578_, (int)chunk.m_7697_().f_45579_);
        Object object = this.chunkLifecycleLock;
        synchronized (object) {
            if (!force && (scanLight && this.scannedLightChunks.contains(chunkKey) || this.pendingChunkScans.containsKey(chunkKey))) {
                return;
            }
            boolean alreadyQueued = this.pendingChunkScans.containsKey(chunkKey);
            this.pendingChunkScans.put(chunkKey, (Object)new PendingChunkScan(level, chunk, scanLight, scanReflective));
            if (scanLight) {
                this.scannedLightChunks.remove(chunkKey);
            }
            if (!alreadyQueued) {
                this.pendingChunkScanOrder.enqueue(chunkKey);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drainChunkLifecycle(ClientLevel level) {
        long deadline = System.nanoTime() + 1250000L;
        ChunkLifecycleBatch batch = new ChunkLifecycleBatch();
        int unloads = 0;
        while (unloads < 8 && System.nanoTime() < deadline) {
            long chunkKey;
            Object object = this.chunkLifecycleLock;
            synchronized (object) {
                if (this.pendingChunkUnloadOrder.isEmpty()) {
                    break;
                }
                chunkKey = this.pendingChunkUnloadOrder.dequeueLong();
                if (!this.pendingChunkUnloads.remove(chunkKey)) {
                    continue;
                }
            }
            this.applyQueuedChunkUnload(chunkKey, batch);
            ++unloads;
        }
        int populatedSections = 0;
        int queueVisits = 0;
        while (populatedSections < 2 && System.nanoTime() < deadline) {
            PendingChunkScan task;
            long chunkKey;
            Object object = this.chunkLifecycleLock;
            synchronized (object) {
                if (this.pendingChunkScanOrder.isEmpty()) {
                    break;
                }
                chunkKey = this.pendingChunkScanOrder.dequeueLong();
                task = (PendingChunkScan)this.pendingChunkScans.get(chunkKey);
            }
            if (task == null) continue;
            if (task.level != level || level.m_7726_().m_7131_(task.chunkX, task.chunkZ) != task.chunk) {
                object = this.chunkLifecycleLock;
                synchronized (object) {
                    if (this.pendingChunkScans.get(chunkKey) == task) {
                        this.pendingChunkScans.remove(chunkKey);
                    }
                    continue;
                }
            }
            int scannedPopulated = this.scanNextChunkSection(task);
            populatedSections += scannedPopulated;
            ++queueVisits;
            if (task.sectionIndex >= task.chunk.m_7103_().length) {
                synchronized (this.chunkLifecycleLock) {
                    if (this.pendingChunkScans.get(chunkKey) == task) {
                        this.pendingChunkScans.remove(chunkKey);
                    }
                }
                this.finishChunkScan(task, batch);
            } else {
                synchronized (this.chunkLifecycleLock) {
                    if (this.pendingChunkScans.get(chunkKey) == task) {
                        this.pendingChunkScanOrder.enqueue(chunkKey);
                    }
                }
            }
            if (queueVisits < 64) continue;
            break;
        }
        this.finishChunkLifecycleBatch(batch);
    }

    private int scanNextChunkSection(PendingChunkScan task) {
        boolean maybeReflective;
        int sectionIndex;
        LevelChunkSection section;
        LevelChunkSection[] sections = task.chunk.m_7103_();
        if (task.sectionIndex >= sections.length) {
            return 0;
        }
        if ((section = sections[sectionIndex = task.sectionIndex++]) == null || section.m_188008_()) {
            return 0;
        }
        boolean maybeLight = task.scanLight && section.m_63002_(state -> state.getLightEmission((BlockGetter)task.level, BlockPos.f_121853_) != 0);
        boolean bl = maybeReflective = task.scanReflective && TorchRtxState.shouldScanReflectiveSources() && section.m_63002_(state -> ReflectionMaterialRegistry.find(state) != null);
        if (!maybeLight && !maybeReflective) {
            return 0;
        }
        int baseX = task.chunk.m_7697_().m_45604_();
        int baseY = SectionPos.m_123223_((int)task.chunk.m_151568_(sectionIndex));
        int baseZ = task.chunk.m_7697_().m_45605_();
        BlockPos.MutableBlockPos pos = task.mutablePos;
        for (int y = 0; y < 16; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    int emission;
                    BlockState state2 = section.m_62982_(x, y, z);
                    pos.m_122178_(baseX + x, baseY + y, baseZ + z);
                    if (maybeLight && (emission = state2.getLightEmission((BlockGetter)task.level, (BlockPos)pos)) > 0 && TorchRtxState.shouldRegisterRtxSource(task.level, state2)) {
                        task.sources.put(pos.m_121878_(), (byte)emission);
                    }
                    if (!maybeReflective || ReflectionMaterialRegistry.find(state2) == null) continue;
                    task.reflections.add(pos.m_121878_());
                }
            }
        }
        return 1;
    }

    private void applyQueuedChunkUnload(long chunkKey, ChunkLifecycleBatch batch) {
        LongOpenHashSet reflectionBucket;
        this.scannedLightChunks.remove(chunkKey);
        LongOpenHashSet sourceBucket = (LongOpenHashSet)this.sourcesByChunk.remove(chunkKey);
        if (sourceBucket != null && !sourceBucket.isEmpty()) {
            this.dirtySourceChunks.add(chunkKey);
            LongIterator it = sourceBucket.iterator();
            while (it.hasNext()) {
                long sourceKey = it.nextLong();
                this.lightSources.remove(sourceKey);
                batch.removedSources.add(sourceKey);
            }
            batch.sourceRegistryChanged = true;
        }
        if ((reflectionBucket = (LongOpenHashSet)this.reflectiveByChunk.remove(chunkKey)) != null && !reflectionBucket.isEmpty()) {
            this.dirtyReflectiveChunks.add(chunkKey);
            LongIterator it = reflectionBucket.iterator();
            while (it.hasNext()) {
                this.reflectiveSources.remove(it.nextLong());
            }
            batch.reflectionRegistryChanged = true;
        }
    }

    private void finishChunkScan(PendingChunkScan task, ChunkLifecycleBatch batch) {
        long chunkKey = task.chunkKey;
        if (task.scanLight) {
            long key;
            LongOpenHashSet oldSources = (LongOpenHashSet)this.sourcesByChunk.get(chunkKey);
            LongOpenHashSet newSources = new LongOpenHashSet();
            newSources.addAll((LongCollection)task.sources.keySet());
            boolean sourceChunkChanged = false;
            if (oldSources != null && !oldSources.isEmpty()) {
                LongOpenHashSet removed = new LongOpenHashSet((LongCollection)oldSources);
                removed.removeAll((LongCollection)newSources);
                if (!removed.isEmpty()) {
                    LongIterator it = removed.iterator();
                    while (it.hasNext()) {
                        key = it.nextLong();
                        this.lightSources.remove(key);
                        batch.removedSources.add(key);
                    }
                    sourceChunkChanged = true;
                }
            }
            ObjectIterator sourceIt = task.sources.long2ByteEntrySet().fastIterator();
            while (sourceIt.hasNext()) {
                Long2ByteMap.Entry entry = (Long2ByteMap.Entry)sourceIt.next();
                key = entry.getLongKey();
                byte emission = entry.getByteValue();
                int previous = this.lightSources.containsKey(key) ? this.lightSources.get(key) & 0xFF : -1;
                this.lightSources.put(key, emission);
                if (oldSources != null && oldSources.contains(key) && previous == (emission & 0xFF)) continue;
                if (this.shouldTraceSource(key)) {
                    batch.addedOrChangedSources.put(key, emission);
                }
                ExposedFaceService.get().requestSectionCapture(SectionPos.m_123209_((int)(BlockPos.m_121983_((long)key) >> 4), (int)(BlockPos.m_122008_((long)key) >> 4), (int)(BlockPos.m_122015_((long)key) >> 4)), 0.0);
                sourceChunkChanged = true;
            }
            if (newSources.isEmpty()) {
                this.sourcesByChunk.remove(chunkKey);
            } else {
                this.sourcesByChunk.put(chunkKey, (Object)newSources);
            }
            if (sourceChunkChanged) {
                this.dirtySourceChunks.add(chunkKey);
                batch.sourceRegistryChanged = true;
            }
            this.scannedLightChunks.add(chunkKey);
        }
        if (task.scanReflective) {
            LongOpenHashSet oldReflections = (LongOpenHashSet)this.reflectiveByChunk.get(chunkKey);
            boolean reflectionChunkChanged = oldReflections == null ? !task.reflections.isEmpty() : !oldReflections.equals((Object)task.reflections);
            if (reflectionChunkChanged) {
                if (oldReflections != null) {
                    LongIterator it = oldReflections.iterator();
                    while (it.hasNext()) {
                        this.reflectiveSources.remove(it.nextLong());
                    }
                }
                if (task.reflections.isEmpty()) {
                    this.reflectiveByChunk.remove(chunkKey);
                } else {
                    this.reflectiveByChunk.put(chunkKey, (Object)new LongOpenHashSet((LongCollection)task.reflections));
                    LongIterator it = task.reflections.iterator();
                    while (it.hasNext()) {
                        this.reflectiveSources.add(it.nextLong());
                    }
                }
                this.dirtyReflectiveChunks.add(chunkKey);
                batch.reflectionRegistryChanged = true;
            }
        }
        if (this.enabled) {
            batch.loadedChunks.add(chunkKey);
        }
    }

    private void finishChunkLifecycleBatch(ChunkLifecycleBatch batch) {
        if (batch.isEmpty()) {
            return;
        }
        LongOpenHashSet replaced = new LongOpenHashSet((LongCollection)batch.removedSources);
        replaced.retainAll((LongCollection)batch.addedOrChangedSources.keySet());
        if (!replaced.isEmpty()) {
            batch.removedSources.removeAll((LongCollection)replaced);
        }
        if (!batch.removedSources.isEmpty()) {
            GpuShadowCache.get().onSourcesRemoved(batch.removedSources);
            if (this.enabled) {
                this.engine.removeSourcesAsync(batch.removedSources);
            }
        }
        if (this.enabled && !batch.addedOrChangedSources.isEmpty()) {
            this.engine.addOrUpdateWorldSources(batch.addedOrChangedSources);
        }
        if (this.enabled && !replaced.isEmpty()) {
            this.engine.markDirty(replaced);
        }
        if (batch.sourceRegistryChanged) {
            this.clearSourceCaches();
            this.refreshSourceSnapshot();
            NeoShadowsEngine.markLightingDirty();
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                NeoGpuVanilla.onChunkSourceRegistryChanged();
            }
        }
        if (batch.reflectionRegistryChanged) {
            this.requestReflectiveSnapshotRefresh();
            ReflectionSystem.get().markWorldDirty();
        }
        if (!batch.loadedChunks.isEmpty()) {
            this.pendingLoadedChunkInvalidations.addAll((LongCollection)batch.loadedChunks);
            if (this.loadedChunkInvalidationDelay < 0) {
                this.loadedChunkInvalidationAge = 0;
            }
            this.loadedChunkInvalidationDelay = 4;
        }
    }

    private void tickLoadedChunkInvalidations() {
        if (this.pendingLoadedChunkInvalidations.isEmpty()) {
            return;
        }
        ++this.loadedChunkInvalidationAge;
        if (this.loadedChunkInvalidationDelay > 0) {
            --this.loadedChunkInvalidationDelay;
        }
        if (this.loadedChunkInvalidationDelay > 0 && this.loadedChunkInvalidationAge < 12) {
            return;
        }
        if (!this.loadedChunkInvalidationRunning.compareAndSet(false, true)) {
            return;
        }
        LongOpenHashSet loadedChunks = new LongOpenHashSet((LongCollection)this.pendingLoadedChunkInvalidations);
        this.pendingLoadedChunkInvalidations.clear();
        this.loadedChunkInvalidationDelay = -1;
        this.loadedChunkInvalidationAge = 0;
        Map<Long, long[]> sourceSnapshot = this.sourceIndexSnapshot;
        CompletableFuture.runAsync(() -> {
            try {
                LongOpenHashSet nearby = TorchRtxState.collectSourcesNearLoadedChunks(loadedChunks, sourceSnapshot);
                if (!nearby.isEmpty()) {
                    this.engine.markDirty(nearby);
                }
            }
            finally {
                this.loadedChunkInvalidationRunning.set(false);
            }
        });
    }

    private static LongOpenHashSet collectSourcesNearLoadedChunks(LongOpenHashSet loadedChunks, Map<Long, long[]> sourceSnapshot) {
        LongOpenHashSet nearby = new LongOpenHashSet();
        if (loadedChunks.isEmpty() || sourceSnapshot.isEmpty()) {
            return nearby;
        }
        double maxDist = LightRtMath.getMaxDistance(15);
        double maxSq = maxDist * maxDist;
        int chunkRadius = Math.max(1, (int)Math.ceil((maxDist + 16.0) / 16.0));
        LongIterator chunkIt = loadedChunks.iterator();
        while (chunkIt.hasNext()) {
            long loadedKey = chunkIt.nextLong();
            int chunkX = ChunkPos.m_45592_((long)loadedKey);
            int chunkZ = ChunkPos.m_45602_((long)loadedKey);
            double minX = chunkX << 4;
            double maxX = minX + 16.0;
            double minZ = chunkZ << 4;
            double maxZ = minZ + 16.0;
            for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
                for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                    long[] bucket = sourceSnapshot.get(ChunkPos.m_45589_((int)(chunkX + dx), (int)(chunkZ + dz)));
                    if (bucket == null) continue;
                    for (long sourceKey : bucket) {
                        long sourceChunkKey = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)sourceKey) >> 4), (int)(BlockPos.m_122015_((long)sourceKey) >> 4));
                        if (loadedChunks.contains(sourceChunkKey)) continue;
                        double sourceX = (double)BlockPos.m_121983_((long)sourceKey) + 0.5;
                        double sourceZ = (double)BlockPos.m_122015_((long)sourceKey) + 0.5;
                        double distX = sourceX < minX ? minX - sourceX : sourceX > maxX ? sourceX - maxX : 0.0;
                        double distZ = sourceZ < minZ ? minZ - sourceZ : sourceZ > maxZ ? sourceZ - maxZ : 0.0;
                        if (!(distX * distX + distZ * distZ <= maxSq)) continue;
                        nearby.add(sourceKey);
                    }
                }
            }
        }
        return nearby;
    }

    private void discoverLoadedChunkSources(ClientLevel level, Vec3 center) {
        if (!TorchRtxState.shouldScanLightSources() || center == null) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        int centerX = SectionPos.m_123171_((int)Mth.m_14107_((double)center.f_82479_));
        int centerZ = SectionPos.m_123171_((int)Mth.m_14107_((double)center.f_82481_));
        int radius = Math.max(1, mc.f_91066_.m_193772_());
        if (radius != this.discoveryRadius) {
            this.discoveryRadius = radius;
            this.discoveryOffsets = TorchRtxState.buildNearestChunkOffsets(radius);
            this.discoveryCursor = 0;
            this.discoveryRestartCooldown = 0;
        }
        if (centerX != this.discoveryCenterChunkX || centerZ != this.discoveryCenterChunkZ) {
            this.discoveryCenterChunkX = centerX;
            this.discoveryCenterChunkZ = centerZ;
            this.discoveryCursor = 0;
            this.discoveryRestartCooldown = 0;
        }
        if (this.discoveryOffsets.length == 0) {
            return;
        }
        if (this.discoveryCursor >= this.discoveryOffsets.length) {
            if (this.discoveryRestartCooldown++ < 20) {
                return;
            }
            this.discoveryRestartCooldown = 0;
            this.discoveryCursor = 0;
        }
        for (int attempts = 0; attempts < 24 && this.discoveryCursor < this.discoveryOffsets.length; ++attempts) {
            LevelChunk chunk;
            int packed = this.discoveryOffsets[this.discoveryCursor++];
            short dx = (short)(packed >>> 16);
            int chunkX = centerX + dx;
            short dz = (short)packed;
            int chunkZ = centerZ + dz;
            long key = ChunkPos.m_45589_((int)chunkX, (int)chunkZ);
            if (this.scannedLightChunks.contains(key) || (chunk = level.m_7726_().m_7131_(chunkX, chunkZ)) == null) continue;
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                this.registerVanillaChunkLightSourcesNow(level, chunk);
                if (!TorchRtxState.shouldScanReflectiveSources()) continue;
                this.queueChunkScan(level, chunk, false, false, true);
                continue;
            }
            this.queueChunkScan(level, chunk, false);
        }
    }

    private static int[] buildNearestChunkOffsets(int radius) {
        ArrayList<int[]> offsets = new ArrayList<int[]>((radius * 2 + 1) * (radius * 2 + 1));
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                if (dx * dx + dz * dz > radius * radius) continue;
                offsets.add(new int[]{dx, dz});
            }
        }
        offsets.sort(Comparator.comparingInt(v -> v[0] * v[0] + v[1] * v[1]));
        int[] packed = new int[offsets.size()];
        for (int i = 0; i < offsets.size(); ++i) {
            int[] v2 = (int[])offsets.get(i);
            packed[i] = (v2[0] & 0xFFFF) << 16 | v2[1] & 0xFFFF;
        }
        return packed;
    }

    private boolean shouldTraceSource(long sourceKey) {
        return !Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) || Config.CLIENT.gpuLocalLightingMode.get() != Config.GpuLocalLightingMode.HYBRID || !this.gpuDirectOwnedSourceSnapshot.contains(sourceKey);
    }

    private static boolean shouldScanLightSources() {
        return NeoGpuVanilla.isConfiguredEnabled() || Config.isFeatureEnabled(Config.CLIENT.rtxEnabled) || Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled) || Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled);
    }

    private static boolean shouldTraceLava(ClientLevel level) {
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.rtxLavaTracingEnabled) && level.m_46472_() != Level.f_46429_;
    }

    private static boolean shouldRegisterRtxSource(ClientLevel level, BlockState state) {
        if (NeoGpuVanilla.isConfiguredEnabled() && state.m_60819_().m_205070_(FluidTags.f_13132_)) {
            return false;
        }
        return !state.m_60819_().m_205070_(FluidTags.f_13132_) || TorchRtxState.shouldTraceLava(level);
    }

    private static boolean shouldScanReflectiveSources() {
        return Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled);
    }

    private static boolean shouldScanAnySources() {
        return TorchRtxState.shouldScanLightSources() || TorchRtxState.shouldScanReflectiveSources();
    }

    private static boolean shouldUseEntityOcclusionShadows() {
        return Config.isFeatureEnabled(Config.CLIENT.rtxEnabled) && Config.isFeatureEnabled(Config.CLIENT.entityShadowsEnabled) && !Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled) && !Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled) && !Config.isFeatureEnabled(Config.CLIENT.gpuShadowsEnabled);
    }

    private void handleScanModeChanged(@Nullable ClientLevel level, boolean wasScanLight, boolean scanLight, boolean wasScanReflective, boolean scanReflective) {
        if (!scanLight) {
            this.clearLightRegistry();
        }
        if (!scanReflective) {
            this.clearReflectiveRegistry();
        }
        if (scanLight && !wasScanLight || scanReflective && !wasScanReflective) {
            this.rescanLoadedChunks(level, scanLight, scanReflective);
        }
        ReflectionSystem.get().markWorldDirty();
        NeoShadowsEngine.markWorldDirty();
    }

    private void syncFloodRtSettings() {
        boolean changed = FloodRtSettings.apply((Integer)Config.CLIENT.floodRadiusCap.get(), (Double)Config.CLIENT.floodPenumbraSoftness.get(), (Double)Config.CLIENT.floodCornerSeal.get(), (Double)Config.CLIENT.floodDirectionalBias.get(), (Integer)Config.CLIENT.floodUpdateBudget.get());
        int revision = FloodRtSettings.revision();
        if (!changed && this.appliedFloodRevision == revision) {
            return;
        }
        this.appliedFloodRevision = revision;
        this.clearSourceCaches();
        this.clearFrontierCache();
        if (this.lightSources.isEmpty()) {
            return;
        }
        this.engine.markDirty(new LongOpenHashSet((LongCollection)this.lightSources.keySet()));
    }

    private void applyConfiguredWorldTracingWorkers(int workerCount) {
        if (this.appliedWorldTracingWorkers == workerCount) {
            return;
        }
        this.engine.setWorldTracingWorkers(workerCount);
        this.appliedWorldTracingWorkers = workerCount;
    }

    private void forceNeoGpuVanillaSourceBootstrap(@Nullable ClientLevel level) {
        if (level == null || !NeoGpuVanilla.isConfiguredEnabled()) {
            return;
        }
        this.scannedLightChunks.clear();
        this.discoveryRadius = -1;
        this.discoveryCursor = 0;
        this.discoveryRestartCooldown = 0;
        this.discoveryOffsets = new int[0];
        GpuShadowCache.get().markAllDirty();
        NeoGpuVanilla.onChunkSourceRegistryChanged();
        this.rescanLoadedChunks(level, true, TorchRtxState.shouldScanReflectiveSources());
    }

    private void rescanLoadedChunks(@Nullable ClientLevel level, boolean scanLight, boolean scanReflective) {
        int[] offsets;
        if (level == null || !scanLight && !scanReflective) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        Vec3 scanCenter = mc.f_91074_ != null ? mc.f_91074_.m_20182_() : mc.f_91063_.m_109153_().m_90583_();
        int centerChunkX = SectionPos.m_123171_((int)Mth.m_14107_((double)scanCenter.f_82479_));
        int centerChunkZ = SectionPos.m_123171_((int)Mth.m_14107_((double)scanCenter.f_82481_));
        double blockRadius = 0.0;
        if (scanLight) {
            blockRadius = Math.max(blockRadius, LightRtMath.getMaxDistance(15) + 16.0);
            if (NeoGpuVanilla.isConfiguredEnabled()) {
                blockRadius = Math.max(blockRadius, (double)((Integer)Config.CLIENT.neoGpuVanillaDistance.get()).intValue() + 16.0);
            }
            if (TorchRtxState.shouldTraceLava(level)) {
                blockRadius = Math.max(blockRadius, (double)mc.f_91066_.m_193772_() * 16.0);
            }
        }
        if (scanReflective) {
            blockRadius = Math.max(blockRadius, (double)mc.f_91066_.m_193772_() * 16.0);
        }
        int chunkRadius = Math.max(1, (int)Math.ceil(blockRadius / 16.0));
        boolean rescannedAny = false;
        for (int packedOffset : offsets = TorchRtxState.buildNearestChunkOffsets(chunkRadius)) {
            short dx = (short)(packedOffset >>> 16);
            short dz = (short)packedOffset;
            LevelChunk chunk = level.m_7726_().m_7131_(centerChunkX + dx, centerChunkZ + dz);
            if (chunk == null) continue;
            if (scanLight && NeoGpuVanilla.isConfiguredEnabled()) {
                this.registerVanillaChunkLightSourcesNow(level, chunk);
                if (scanReflective) {
                    this.queueChunkScan(level, chunk, true, false, true);
                }
            } else {
                this.queueChunkScan(level, chunk, true, scanLight, scanReflective);
            }
            rescannedAny = true;
        }
        if (scanReflective) {
            ReflectionSystem.get().forceImmediateUpdate();
        }
        if (scanLight && rescannedAny && !NeoGpuVanilla.isConfiguredEnabled()) {
            this.engine.boostNearestWorldSources(2);
        }
        if (rescannedAny) {
            this.triggerGlobalUpdate();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void clearLightRegistry() {
        this.lightSources.clear();
        this.sourcesByChunk.clear();
        this.scannedLightChunks.clear();
        this.dirtySourceChunks.clear();
        this.discoveryCursor = 0;
        this.discoveryRestartCooldown = 0;
        this.discoveryOffsets = new int[0];
        Object object = this.chunkLifecycleLock;
        synchronized (object) {
            this.pendingChunkScans.clear();
            this.pendingChunkScanOrder.clear();
            this.pendingChunkUnloads.clear();
            this.pendingChunkUnloadOrder.clear();
        }
        this.pendingLoadedChunkInvalidations.clear();
        this.loadedChunkInvalidationDelay = -1;
        this.loadedChunkInvalidationAge = 0;
        this.sourceIndexSnapshot = Collections.emptyMap();
        object = this.pendingLightChangesLock;
        synchronized (object) {
            this.pendingLightChangeBitsBySection.clear();
            this.pendingLightChangeCount = 0;
        }
        object = this.pendingRendererSectionsLock;
        synchronized (object) {
            this.pendingRendererSections.clear();
            this.rendererSectionCooldownUntil.clear();
        }
        this.clearSourceCaches();
        this.clearFrontierCache();
        this.engine.clear();
    }

    private void clearSourceCaches() {
        this.closestTorchCache.clear();
    }

    private void clearReflectiveRegistry() {
        this.reflectiveSources.clear();
        this.reflectiveByChunk.clear();
        this.reflectiveSnapshot = Collections.emptyMap();
        this.dirtyReflectiveChunks.clear();
    }

    private void indexSource(long k) {
        long chunkKey = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)k) >> 4), (int)(BlockPos.m_122015_((long)k) >> 4));
        ((LongOpenHashSet)this.sourcesByChunk.computeIfAbsent(chunkKey, x -> new LongOpenHashSet())).add(k);
        this.dirtySourceChunks.add(chunkKey);
    }

    private void unindexSource(long k) {
        long ck = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)k) >> 4), (int)(BlockPos.m_122015_((long)k) >> 4));
        LongOpenHashSet b = (LongOpenHashSet)this.sourcesByChunk.get(ck);
        if (b == null) {
            return;
        }
        b.remove(k);
        if (b.isEmpty()) {
            this.sourcesByChunk.remove(ck);
        }
        this.dirtySourceChunks.add(ck);
    }

    private void indexReflectiveSource(long k) {
        long ck = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)k) >> 4), (int)(BlockPos.m_122015_((long)k) >> 4));
        ((LongOpenHashSet)this.reflectiveByChunk.computeIfAbsent(ck, x -> new LongOpenHashSet())).add(k);
        this.dirtyReflectiveChunks.add(ck);
    }

    private void unindexReflectiveSource(long k) {
        long ck = ChunkPos.m_45589_((int)(BlockPos.m_121983_((long)k) >> 4), (int)(BlockPos.m_122015_((long)k) >> 4));
        LongOpenHashSet b = (LongOpenHashSet)this.reflectiveByChunk.get(ck);
        if (b == null) {
            return;
        }
        b.remove(k);
        if (b.isEmpty()) {
            this.reflectiveByChunk.remove(ck);
        }
        this.dirtyReflectiveChunks.add(ck);
    }

    private void refreshSourceSnapshot() {
        if (this.dirtySourceChunks.isEmpty()) {
            return;
        }
        if (!this.sourceRefreshQueued.compareAndSet(false, true)) {
            return;
        }
        int dirtyCount = this.dirtySourceChunks.size();
        long[] updatedChunkKeys = new long[dirtyCount];
        long[][] updatedBucketArrays = new long[dirtyCount][];
        int count = 0;
        LongIterator ci = this.dirtySourceChunks.iterator();
        while (ci.hasNext()) {
            long chunkKey = ci.nextLong();
            LongOpenHashSet bucket = (LongOpenHashSet)this.sourcesByChunk.get(chunkKey);
            updatedChunkKeys[count] = chunkKey;
            if (bucket == null || bucket.isEmpty()) {
                updatedBucketArrays[count] = null;
            } else {
                long[] values = new long[bucket.size()];
                int index = 0;
                LongIterator bi = bucket.iterator();
                while (bi.hasNext()) {
                    values[index++] = bi.nextLong();
                }
                updatedBucketArrays[count] = values;
            }
            ++count;
        }
        this.dirtySourceChunks.clear();
        Map<Long, long[]> previousSnapshot = this.sourceIndexSnapshot;
        int finalCount = count;
        CompletableFuture.runAsync(() -> {
            HashMap<Long, long[]> nextSnapshot = new HashMap<Long, long[]>(previousSnapshot.size() + finalCount);
            nextSnapshot.putAll(previousSnapshot);
            for (int i = 0; i < finalCount; ++i) {
                long[] values = updatedBucketArrays[i];
                if (values == null) {
                    nextSnapshot.remove(updatedChunkKeys[i]);
                    continue;
                }
                nextSnapshot.put(updatedChunkKeys[i], values);
            }
            this.sourceIndexSnapshot = nextSnapshot.isEmpty() ? Collections.emptyMap() : nextSnapshot;
        }).whenComplete((unused, error) -> Minecraft.m_91087_().execute(() -> {
            this.sourceRefreshQueued.set(false);
            if (!this.dirtySourceChunks.isEmpty()) {
                this.refreshSourceSnapshot();
            }
        }));
    }

    private void refreshReflectiveSnapshot() {
        if (this.dirtyReflectiveChunks.isEmpty()) {
            return;
        }
        int dirtyCount = this.dirtyReflectiveChunks.size();
        long[] updatedChunkKeys = new long[dirtyCount];
        long[][] updatedBucketArrays = new long[dirtyCount][];
        int count = 0;
        LongIterator ci = this.dirtyReflectiveChunks.iterator();
        while (ci.hasNext()) {
            long chunkKey = ci.nextLong();
            LongOpenHashSet bucket = (LongOpenHashSet)this.reflectiveByChunk.get(chunkKey);
            updatedChunkKeys[count] = chunkKey;
            if (bucket == null || bucket.isEmpty()) {
                updatedBucketArrays[count] = null;
            } else {
                long[] values = new long[bucket.size()];
                int index = 0;
                LongIterator bi = bucket.iterator();
                while (bi.hasNext()) {
                    values[index++] = bi.nextLong();
                }
                updatedBucketArrays[count] = values;
            }
            ++count;
        }
        this.dirtyReflectiveChunks.clear();
        Map<Long, long[]> previousSnapshot = this.reflectiveSnapshot;
        int finalCount = count;
        CompletableFuture.runAsync(() -> {
            HashMap<Long, long[]> nextSnapshot = new HashMap<Long, long[]>(previousSnapshot.size() + finalCount);
            nextSnapshot.putAll(previousSnapshot);
            for (int i = 0; i < finalCount; ++i) {
                long[] values = updatedBucketArrays[i];
                if (values == null) {
                    nextSnapshot.remove(updatedChunkKeys[i]);
                    continue;
                }
                nextSnapshot.put(updatedChunkKeys[i], values);
            }
            this.reflectiveSnapshot = nextSnapshot.isEmpty() ? Collections.emptyMap() : nextSnapshot;
        });
    }

    public void requestReflectiveSnapshotRefresh() {
        if (!TorchRtxState.shouldScanReflectiveSources()) {
            this.clearReflectiveRegistry();
            return;
        }
        if (!this.reflectiveRefreshQueued.compareAndSet(false, true)) {
            return;
        }
        Minecraft.m_91087_().execute(() -> {
            this.reflectiveRefreshQueued.set(false);
            this.refreshReflectiveSnapshot();
        });
    }

    private void triggerGlobalUpdate() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91060_ != null) {
            mc.f_91060_.m_109818_();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void enqueueFrontierChecks(LongOpenHashSet positions) {
        if (positions == null || positions.isEmpty() || !this.enabled) {
            return;
        }
        Object object = this.frontierQueueLock;
        synchronized (object) {
            this.pendingFrontierChecks.addAll((LongCollection)positions);
        }
        this.scheduleFrontierWorker();
    }

    private void scheduleFrontierWorker() {
        if (!this.enabled || !this.frontierWorkerRunning.compareAndSet(false, true)) {
            return;
        }
        int epoch = this.frontierEpoch.get();
        FRONTIER_EXECUTOR.execute(() -> this.drainFrontierChecks(epoch));
    }

    /*
     * Exception decompiling
     */
    private void drainFrontierChecks(int epoch) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [3[TRYBLOCK]], but top level block is 25[FORLOOP]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void clearFrontierCache() {
        this.frontierEpoch.incrementAndGet();
        Object object = this.frontierQueueLock;
        synchronized (object) {
            this.pendingFrontierChecks.clear();
        }
        this.frontierBlockSnapshot = new LongOpenHashSet();
        this.frontierSnapshot = Collections.emptyMap();
    }

    private /* synthetic */ void lambda$drainFrontierChecks$17(int epoch, LongOpenHashSet notification) {
        if (this.enabled && this.frontierEpoch.get() == epoch) {
            NeoShadowsEngine.get().onLightingChanged(notification, true);
            NeoCpuShadowsEngine.onLightDataChanged();
        }
    }

    private static final class PendingChunkScan {
        final ClientLevel level;
        final LevelChunk chunk;
        final long chunkKey;
        final int chunkX;
        final int chunkZ;
        final boolean scanLight;
        final boolean scanReflective;
        final Long2ByteOpenHashMap sources = new Long2ByteOpenHashMap();
        final LongOpenHashSet reflections = new LongOpenHashSet();
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int sectionIndex;

        PendingChunkScan(ClientLevel level, LevelChunk chunk, boolean scanLight, boolean scanReflective) {
            this.level = level;
            this.chunk = chunk;
            this.chunkX = chunk.m_7697_().f_45578_;
            this.chunkZ = chunk.m_7697_().f_45579_;
            this.chunkKey = ChunkPos.m_45589_((int)this.chunkX, (int)this.chunkZ);
            this.scanLight = scanLight;
            this.scanReflective = scanReflective;
            this.sources.defaultReturnValue((byte)0);
        }
    }

    private static final class ChunkLifecycleBatch {
        final Long2ByteOpenHashMap addedOrChangedSources = new Long2ByteOpenHashMap();
        final LongOpenHashSet removedSources = new LongOpenHashSet();
        final LongOpenHashSet loadedChunks = new LongOpenHashSet();
        boolean sourceRegistryChanged;
        boolean reflectionRegistryChanged;

        ChunkLifecycleBatch() {
            this.addedOrChangedSources.defaultReturnValue((byte)0);
        }

        boolean isEmpty() {
            return this.addedOrChangedSources.isEmpty() && this.removedSources.isEmpty() && this.loadedChunks.isEmpty() && !this.sourceRegistryChanged && !this.reflectionRegistryChanged;
        }
    }
}
