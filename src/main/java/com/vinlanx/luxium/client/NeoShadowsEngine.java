/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.RenderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$CullStateShard
 *  net.minecraft.client.renderer.RenderStateShard$DepthTestStateShard
 *  net.minecraft.client.renderer.RenderStateShard$ShaderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TransparencyStateShard
 *  net.minecraft.client.renderer.RenderStateShard$WriteMaskStateShard
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.core.Direction
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.DynamicShadowMeshCapture;
import com.vinlanx.luxium.client.NeoShadowsDynamicPipeline;
import com.vinlanx.luxium.client.NeoShadowsOcclusionPipeline;
import com.vinlanx.luxium.client.NeoShadowsProjection;
import com.vinlanx.luxium.client.NeoShadowsRenderPipeline;
import com.vinlanx.luxium.client.NeoShadowsRuntime;
import com.vinlanx.luxium.client.NeoShadowsStaticPipeline;
import com.vinlanx.luxium.client.NeoShadowsTypes;
import com.vinlanx.luxium.client.NeoShadowsVisibility;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowsEngine;
import com.vinlanx.luxium.client.postprocess.PostProcessStateGuard;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class NeoShadowsEngine {
    private static final NeoShadowsEngine INSTANCE = new NeoShadowsEngine();
    static final double RENDER_RADIUS = 36.0;
    static final double MAX_RECEIVER_LIGHT_DISTANCE = 30.0;
    static final int MAX_VISIBLE_SOURCES = 256;
    static final int MAX_RECEIVER_BLOCKS = 16384;
    static final int MAX_RECEIVER_FACES = 12288;
    static final int MAX_DYNAMIC_CASTERS = 160;
    static final int FRONTIER_RECEIVER_EXPANSION_RADIUS = 3;
    static final double COMPATIBLE_BUILD_DISTANCE_SQ = 256.0;
    static final float FACE_EPSILON = 0.0015f;
    static final float FACE_PROBE_INSET = 0.18f;
    static final float HORIZONTAL_FACE_PROBE_INSET = 0.18f;
    static final int LIGHTING_FACE_PROBE_GRID = 1;
    static final int OCCLUSION_FACE_PROBE_GRID = 1;
    static final int HORIZONTAL_LIGHTING_FACE_PROBE_GRID = 3;
    static final int HORIZONTAL_OCCLUSION_FACE_PROBE_GRID = 3;
    static final double PROJECT_EPSILON = 1.0E-6;
    static final double PROJECTION_DENOMINATOR_EPSILON = 1.0E-9;
    static final double POLYGON_AREA_EPSILON = 1.0E-5;
    static final double EXTENDED_PROJECTION_DELTA = 0.001;
    static final double EXTENDED_PROJECTION_FAR = 8.0;
    static final double SHADOW_POLYGON_DILATION = 0.016;
    static final double HORIZONTAL_SHADOW_POLYGON_DILATION = 0.024;
    static final double SHADOW_EDGE_SNAP_EPSILON = 0.022;
    static final double HORIZONTAL_SHADOW_EDGE_SNAP_EPSILON = 0.03;
    static final double FULL_RECT_SHADOW_AREA_THRESHOLD = 0.988;
    static final int SPRITE_ALPHA_THRESHOLD = 16;
    static final long SKY_CAPTURE_MIN_UPDATE_INTERVAL_MS = 100L;
    static final float FACE_CAMERA_FACING_EPSILON = 0.02f;
    static final double MIN_CONTRIBUTION_WEIGHT = 0.03;
    static final long REBUILD_DEBOUNCE_MS = 2L;
    static final long REBUILD_MAX_DEFERRAL_MS = 1500L;
    static final int SHADOW_WORKER_COUNT = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
    static volatile ExecutorService shadowWorkerPool;
    static final Direction[] FACES;
    static final int[][] QUAD_EDGE_VERTEX_INDICES;
    static final NeoShadowsTypes.UvPoint[] LIGHTING_FACE_PROBES;
    static final NeoShadowsTypes.UvPoint[] OCCLUSION_FACE_PROBES;
    static final NeoShadowsTypes.UvPoint[] HORIZONTAL_LIGHTING_FACE_PROBES;
    static final NeoShadowsTypes.UvPoint[] HORIZONTAL_OCCLUSION_FACE_PROBES;
    static final NeoShadowsTypes.PlaneRect[] LIGHTING_FACE_PROBE_RECTS;
    static final NeoShadowsTypes.PlaneRect[] HORIZONTAL_LIGHTING_FACE_PROBE_RECTS;
    static final int[][] RECEIVER_OFFSETS;
    static final RenderType MASK_PASS;
    static final ThreadLocal<long[]> CANDIDATE_KEYS_TL;
    static final ThreadLocal<double[]> CANDIDATE_DISTANCES_TL;
    static final ThreadLocal<byte[]> CANDIDATE_MASKS_TL;
    final ConcurrentHashMap<TextureAtlasSprite, NeoShadowsTypes.SpriteOpaqueMask> spriteOpaqueMaskCache = new ConcurrentHashMap();
    final AtomicBoolean staticTaskQueued = new AtomicBoolean(false);
    final AtomicBoolean staticHotPatchTaskQueued = new AtomicBoolean(false);
    final AtomicBoolean dynamicTaskQueued = new AtomicBoolean(false);
    final AtomicBoolean dynamicCaptureQueued = new AtomicBoolean(false);
    final ExecutorService staticExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NeoShadows-Static");
        thread.setDaemon(true);
        thread.setPriority(7);
        return thread;
    });
    final ExecutorService hotPatchExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NeoShadows-HotPatch");
        thread.setDaemon(true);
        thread.setPriority(7);
        return thread;
    });
    final ExecutorService dynamicExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NeoShadows-Dynamic");
        thread.setDaemon(true);
        thread.setPriority(5);
        return thread;
    });
    volatile boolean enabled;
    volatile boolean framePrepared;
    volatile boolean worldDirty = true;
    volatile boolean geometryDirty = true;
    volatile boolean skyCaptureDirty = true;
    volatile boolean skyLightCapturePass;
    volatile boolean shadowCapturePass;
    volatile boolean gpuShadowAtlasCapturePass;
    volatile boolean gpuShadowDynamicCastersAllowed;
    volatile int gpuShadowCaptureFace = -1;
    volatile double gpuShadowCaptureSourceX;
    volatile double gpuShadowCaptureSourceY;
    volatile double gpuShadowCaptureSourceZ;
    volatile float gpuShadowCaptureSourceRadius;
    volatile boolean maskReady;
    volatile boolean skySceneReady;
    volatile boolean dynamicDirty = true;
    volatile boolean pendingRebuild;
    volatile long pendingRebuildSinceMs;
    volatile long pendingRebuildFirstRequestMs;
    volatile boolean highPriorityTracePending;
    volatile long buildEpoch = 1L;
    volatile long nextStaticRequestId = 1L;
    volatile long nextStaticHotPatchRequestId = 1L;
    volatile long nextDynamicCaptureRequestId = 1L;
    volatile long nextDynamicRequestId = 1L;
    volatile long lastDynamicScheduleMs;
    volatile long lastDynamicStateScanMs;
    volatile long lastDynamicMeshCaptureMs;
    volatile long dynamicCasterFingerprint;
    volatile long dynamicSnapshotFingerprint;
    @Nullable
    volatile NeoShadowsTypes.StaticRequest latestStaticRequest;
    @Nullable
    volatile NeoShadowsTypes.StaticResult completedStaticResult;
    @Nullable
    volatile NeoShadowsTypes.StaticHotPatchRequest latestStaticHotPatchRequest;
    @Nullable
    volatile NeoShadowsTypes.StaticHotPatchResult completedStaticHotPatchResult;
    @Nullable
    volatile NeoShadowsTypes.DynamicCaptureRequest latestDynamicCaptureRequest;
    @Nullable
    volatile NeoShadowsTypes.DynamicRequest latestDynamicRequest;
    @Nullable
    volatile NeoShadowsTypes.DynamicResult completedDynamicResult;
    @Nullable
    volatile NeoShadowsTypes.RealtimeStaticShadowOutput activeStaticOutput;
    @Nullable
    volatile NeoShadowsTypes.VisibleCandidateBlockSnapshot visibleCandidateSnapshot;
    volatile NeoShadowsTypes.StaticHotPatchOverlay activeStaticHotPatch = NeoShadowsTypes.StaticHotPatchOverlay.EMPTY;
    volatile List<NeoShadowsTypes.ShadowPolygon> activeDynamicPolygons = Collections.emptyList();
    volatile List<NeoShadowsTypes.ShadowPolygon> inFlightStaticPolygons = Collections.emptyList();
    volatile long inFlightStaticEpoch;
    volatile long activeStaticResultId;
    volatile long appliedStaticResultId;
    volatile long appliedStaticHotPatchResultId;
    volatile long activeDynamicResultId;
    volatile long activeDynamicStaticResultId;
    volatile DynamicShadowMeshCapture.Snapshot dynamicCasterSnapshot = DynamicShadowMeshCapture.Snapshot.empty();
    TextureTarget shadowMaskTarget;
    @Nullable
    RenderTarget sceneCopyTarget;
    TextureTarget skyCaptureTarget;
    long lastSkyCaptureRenderMs;
    int lastSkyCaptureWidth = -1;
    int lastSkyCaptureHeight = -1;
    double lastSkyCaptureCamX = Double.NaN;
    double lastSkyCaptureCamY = Double.NaN;
    double lastSkyCaptureCamZ = Double.NaN;
    float lastSkyCaptureYaw = Float.NaN;
    float lastSkyCapturePitch = Float.NaN;
    final Matrix4f lastSkyCaptureProjection = new Matrix4f();
    boolean lastSkyCaptureProjectionValid;
    final NeoShadowsRuntime runtime;
    final NeoShadowsRenderPipeline renderPipeline;
    final NeoShadowsDynamicPipeline dynamicPipeline = new NeoShadowsDynamicPipeline(this);
    final NeoShadowsVisibility visibility;
    final NeoShadowsOcclusionPipeline occlusionPipeline = new NeoShadowsOcclusionPipeline(this);
    final NeoShadowsStaticPipeline staticPipeline;

    private NeoShadowsEngine() {
        this.visibility = new NeoShadowsVisibility(this);
        this.staticPipeline = new NeoShadowsStaticPipeline(this);
        this.runtime = new NeoShadowsRuntime(this);
        this.renderPipeline = new NeoShadowsRenderPipeline(this);
    }

    public static NeoShadowsEngine get() {
        return INSTANCE;
    }

    public static boolean isAnyShadowCapturePass() {
        return NeoShadowsEngine.INSTANCE.skyLightCapturePass || NeoShadowsEngine.INSTANCE.shadowCapturePass;
    }

    public static boolean isShadowCapturePass() {
        return NeoShadowsEngine.INSTANCE.shadowCapturePass;
    }

    public static boolean isGpuShadowAtlasCapturePass() {
        return NeoShadowsEngine.INSTANCE.gpuShadowAtlasCapturePass;
    }

    public static boolean shouldRenderDynamicCastersInShadowPass() {
        return NeoShadowsEngine.INSTANCE.gpuShadowAtlasCapturePass && NeoShadowsEngine.INSTANCE.gpuShadowDynamicCastersAllowed;
    }

    public static void beginGpuShadowAtlasCapture(double sourceX, double sourceY, double sourceZ, float radius, int face, boolean allowDynamicCasters) {
        NeoShadowsEngine.INSTANCE.shadowCapturePass = true;
        NeoShadowsEngine.INSTANCE.gpuShadowAtlasCapturePass = true;
        NeoShadowsEngine.INSTANCE.gpuShadowDynamicCastersAllowed = allowDynamicCasters;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureFace = face;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceX = sourceX;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceY = sourceY;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceZ = sourceZ;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceRadius = radius;
    }

    public static void endGpuShadowAtlasCapture() {
        NeoShadowsEngine.INSTANCE.gpuShadowDynamicCastersAllowed = false;
        NeoShadowsEngine.INSTANCE.gpuShadowAtlasCapturePass = false;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureFace = -1;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceX = 0.0;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceY = 0.0;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceZ = 0.0;
        NeoShadowsEngine.INSTANCE.gpuShadowCaptureSourceRadius = 0.0f;
        NeoShadowsEngine.INSTANCE.shadowCapturePass = false;
    }

    public static void setShadowCapturePass(boolean val) {
        NeoShadowsEngine.INSTANCE.shadowCapturePass = val;
    }

    public static boolean isShadowlessSceneReady() {
        return NeoShadowsEngine.INSTANCE.skySceneReady && NeoShadowsEngine.INSTANCE.skyCaptureTarget != null;
    }

    public static int getShadowlessSceneColorTextureId() {
        return NeoShadowsEngine.INSTANCE.skyCaptureTarget != null ? NeoShadowsEngine.INSTANCE.skyCaptureTarget.m_83975_() : -1;
    }

    public static int getShadowlessSceneDepthTextureId() {
        return NeoShadowsEngine.INSTANCE.skyCaptureTarget != null ? NeoShadowsEngine.INSTANCE.skyCaptureTarget.m_83980_() : -1;
    }

    public static void markShadowlessCaptureDirty() {
        NeoShadowsEngine.INSTANCE.skyCaptureDirty = true;
    }

    public static boolean isRenderingSkyLightPass() {
        return NeoShadowsEngine.INSTANCE.skyLightCapturePass;
    }

    public static void markWorldDirty() {
        NeoShadowsEngine.INSTANCE.runtime.onWorldDirty(null);
    }

    public static void markWorldDirty(@Nullable LongOpenHashSet changedBlocks) {
        NeoShadowsEngine.INSTANCE.runtime.onWorldDirty(changedBlocks);
    }

    public static void markLightingDirty() {
        NeoShadowsEngine.INSTANCE.runtime.onLightingDirty(null, false);
    }

    public static void markLightingDirty(@Nullable LongOpenHashSet changedBlocks, boolean geometryChanged) {
        NeoShadowsEngine.INSTANCE.runtime.onLightingDirty(changedBlocks, geometryChanged);
    }

    public static void markGeometryDirty() {
        NeoShadowsEngine.INSTANCE.geometryDirty = true;
    }

    public void notifyHighPriorityTraceDone() {
        this.highPriorityTracePending = true;
    }

    public void onLightingChanged() {
        this.runtime.onLightingDirty(null, true);
    }

    public void onLightingChanged(@Nullable LongOpenHashSet changedBlocks) {
        this.runtime.onLightingDirty(changedBlocks, true);
    }

    public void onLightingChanged(@Nullable LongOpenHashSet changedBlocks, boolean geometryChanged) {
        this.runtime.onLightingDirty(changedBlocks, geometryChanged);
    }

    public static void prepareFrame(Minecraft mc, Camera camera) {
        NeoShadowsEngine.INSTANCE.runtime.prepareFrameInternal(mc, camera, mc.getPartialTick());
    }

    public static void prepareFrame(Minecraft mc, Camera camera, float partialTick) {
        NeoShadowsEngine.INSTANCE.runtime.prepareFrameInternal(mc, camera, partialTick);
    }

    public static void captureSkyLightScene(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix) {
        NeoShadowsEngine.INSTANCE.renderPipeline.captureSkyLightSceneInternal(levelRenderer, poseStack, partialTick, finishNano, camera, gameRenderer, lightTexture, projectionMatrix);
    }

    public static void forceDisable() {
        INSTANCE.forceDisableInternal();
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        INSTANCE.handleRenderLevelStage(event);
    }

    private void handleRenderLevelStage(RenderLevelStageEvent event) {
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isRenderingSkyLightPass()) {
            return;
        }
        if (!this.isSelected()) {
            if (NeoCpuShadowsEngine.isActive() || GpuNeoShadows.isActive()) {
                return;
            }
            this.forceDisableInternal();
            PostProcessStateGuard.restoreCanonicalMainTargetBinding(Minecraft.m_91087_());
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            this.renderPipeline.renderAfterParticles(mc, event);
            this.runtime.drainDynamicCaptureQueue();
        }
    }

    boolean isSelected() {
        if (NeoCpuShadowsEngine.isActive() || GpuNeoShadows.isActive()) {
            return false;
        }
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.realisticShadowsEnabled);
    }

    void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        ++this.buildEpoch;
        this.clearRuntimeState();
        if (enabled) {
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91060_ != null) {
                mc.f_91060_.m_109818_();
            }
        }
    }

    void forceDisableInternal() {
        if (this.enabled) {
            this.setEnabled(false);
        } else if (this.framePrepared || this.skyLightCapturePass || this.maskReady || this.skySceneReady) {
            this.clearRuntimeState();
        }
    }

    void clearRuntimeState() {
        this.framePrepared = false;
        this.worldDirty = true;
        this.skyCaptureDirty = true;
        this.skyLightCapturePass = false;
        this.shadowCapturePass = false;
        this.gpuShadowAtlasCapturePass = false;
        this.gpuShadowDynamicCastersAllowed = false;
        this.gpuShadowCaptureFace = -1;
        this.gpuShadowCaptureSourceX = 0.0;
        this.gpuShadowCaptureSourceY = 0.0;
        this.gpuShadowCaptureSourceZ = 0.0;
        this.gpuShadowCaptureSourceRadius = 0.0f;
        this.maskReady = false;
        this.skySceneReady = false;
        this.dynamicDirty = true;
        this.pendingRebuild = false;
        this.pendingRebuildSinceMs = 0L;
        this.pendingRebuildFirstRequestMs = 0L;
        this.activeStaticOutput = null;
        this.visibleCandidateSnapshot = null;
        this.activeStaticHotPatch = NeoShadowsTypes.StaticHotPatchOverlay.EMPTY;
        this.activeDynamicPolygons = Collections.emptyList();
        this.inFlightStaticPolygons = Collections.emptyList();
        this.inFlightStaticEpoch = 0L;
        this.latestStaticRequest = null;
        this.completedStaticResult = null;
        this.latestStaticHotPatchRequest = null;
        this.completedStaticHotPatchResult = null;
        this.latestDynamicCaptureRequest = null;
        this.latestDynamicRequest = null;
        this.completedDynamicResult = null;
        this.dynamicCaptureQueued.set(false);
        this.activeStaticResultId = 0L;
        this.appliedStaticResultId = 0L;
        this.appliedStaticHotPatchResultId = 0L;
        this.activeDynamicResultId = 0L;
        this.activeDynamicStaticResultId = 0L;
        this.lastDynamicScheduleMs = 0L;
        this.lastDynamicStateScanMs = 0L;
        this.lastDynamicMeshCaptureMs = 0L;
        this.dynamicCasterFingerprint = 0L;
        this.dynamicSnapshotFingerprint = 0L;
        this.dynamicCasterSnapshot = DynamicShadowMeshCapture.Snapshot.empty();
        this.lastSkyCaptureRenderMs = 0L;
        this.lastSkyCaptureWidth = -1;
        this.lastSkyCaptureHeight = -1;
        this.lastSkyCaptureCamX = Double.NaN;
        this.lastSkyCaptureCamY = Double.NaN;
        this.lastSkyCaptureCamZ = Double.NaN;
        this.lastSkyCaptureYaw = Float.NaN;
        this.lastSkyCapturePitch = Float.NaN;
        this.lastSkyCaptureProjectionValid = false;
        this.occlusionPipeline.clearOcclusionCache();
    }

    boolean hasAnyShadowPolygons() {
        return this.activeStaticOutput != null && !this.activeStaticOutput.shadowPolygons.isEmpty() || !this.activeStaticHotPatch.polygons.isEmpty() || !this.inFlightStaticPolygons.isEmpty() || !this.activeDynamicPolygons.isEmpty();
    }

    void ensureTargets(int width, int height) {
        if (this.shadowMaskTarget == null) {
            this.shadowMaskTarget = new TextureTarget(width, height, true, Minecraft.f_91002_);
            this.shadowMaskTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        } else if (this.shadowMaskTarget.f_83915_ != width || this.shadowMaskTarget.f_83916_ != height) {
            this.shadowMaskTarget.m_83941_(width, height, Minecraft.f_91002_);
            this.shadowMaskTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        }
        if (this.sceneCopyTarget == null) {
            this.sceneCopyTarget = new RenderTarget(false){};
            this.sceneCopyTarget.m_83950_(width, height, Minecraft.f_91002_);
            this.sceneCopyTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        } else if (this.sceneCopyTarget.f_83917_ != width || this.sceneCopyTarget.f_83918_ != height) {
            this.sceneCopyTarget.m_83941_(width, height, Minecraft.f_91002_);
        }
        double skyCaptureScaleValue = (Double)Config.CLIENT.realisticShadowsRenderScale.get();
        float skyCaptureScale = (float)Math.max(0.01, Math.min(1.0, skyCaptureScaleValue));
        int skyWidth = Math.max(1, Math.round((float)width * skyCaptureScale));
        int skyHeight = Math.max(1, Math.round((float)height * skyCaptureScale));
        if (this.skyCaptureTarget == null) {
            this.skyCaptureTarget = new TextureTarget(skyWidth, skyHeight, true, Minecraft.f_91002_);
            this.skyCaptureTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            this.skyCaptureTarget.m_83936_(9729);
        } else if (this.skyCaptureTarget.f_83915_ != skyWidth || this.skyCaptureTarget.f_83916_ != skyHeight) {
            this.skyCaptureTarget.m_83941_(skyWidth, skyHeight, Minecraft.f_91002_);
            this.skyCaptureTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            this.skyCaptureTarget.m_83936_(9729);
        }
    }

    static void flushSharedRenderBuffers(Minecraft mc) {
        mc.m_91269_().m_110104_().m_109911_();
        mc.m_91269_().m_110108_().m_109911_();
        mc.m_91269_().m_110109_().m_109928_();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static ExecutorService shadowWorkerPool() {
        ExecutorService pool = shadowWorkerPool;
        if (pool != null) {
            return pool;
        }
        Class<NeoShadowsEngine> clazz = NeoShadowsEngine.class;
        synchronized (NeoShadowsEngine.class) {
            if (shadowWorkerPool == null) {
                shadowWorkerPool = Executors.newFixedThreadPool(SHADOW_WORKER_COUNT, runnable -> {
                    Thread thread = new Thread(runnable, "NeoShadows-Worker");
                    thread.setDaemon(true);
                    thread.setPriority(6);
                    return thread;
                });
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return shadowWorkerPool;
        }
    }

    static void mergeScratch(NeoShadowsTypes.GeometryBuildState state, NeoShadowsTypes.BuildScratch scratch) {
        if (!scratch.shadowPolygons.isEmpty()) {
            state.shadowPolygons.addAll(scratch.shadowPolygons);
        }
        state.occluderHitCount += scratch.occluderHitCount;
        state.shadowPolygonCount += scratch.shadowPolygonCount;
    }

    boolean shouldCullShadowPolygon(NeoShadowsTypes.ShadowPolygon polygon, double cameraX, double cameraY, double cameraZ, @Nullable Frustum frustum) {
        if (!NeoShadowsEngine.isFaceFacingCamera(polygon, cameraX, cameraY, cameraZ)) {
            return true;
        }
        return frustum != null && !frustum.m_113029_(polygon.bounds);
    }

    static boolean isFaceFacingCamera(NeoShadowsTypes.ShadowPolygon polygon, double cameraX, double cameraY, double cameraZ) {
        double toCameraX = cameraX - polygon.faceCenterX;
        double toCameraY = cameraY - polygon.faceCenterY;
        double toCameraZ = cameraZ - polygon.faceCenterZ;
        return toCameraX * (double)polygon.faceNormalX + toCameraY * (double)polygon.faceNormalY + toCameraZ * (double)polygon.faceNormalZ > (double)0.02f;
    }

    void emitPolygon(Matrix4f pose, VertexConsumer consumer, Vec3 cameraPos, NeoShadowsTypes.ShadowPolygon polygon) {
        double[] vertices = polygon.triangleVertices;
        if (vertices.length < 9 || polygon.strength <= 0.0f) {
            return;
        }
        float value = polygon.strength;
        for (int offset = 0; offset < vertices.length; offset += 3) {
            consumer.m_252986_(pose, (float)(vertices[offset] - cameraPos.f_82479_), (float)(vertices[offset + 1] - cameraPos.f_82480_), (float)(vertices[offset + 2] - cameraPos.f_82481_)).m_85950_(value, value, value, 1.0f).m_5752_();
        }
    }

    private static RenderType buildMaskPass() {
        RenderType.CompositeState state = RenderType.CompositeState.m_110628_().m_173292_(new RenderStateShard.ShaderStateShard(GameRenderer::m_172811_)).m_110685_(RenderHelper.ADDITIVE_TRANSPARENCY_STATE).m_110663_(RenderHelper.LEQUAL).m_110661_(RenderHelper.NO_CULL).m_110687_(RenderHelper.COLOR_ONLY).m_110691_(false);
        return RenderType.m_173215_((String)"luxium_neo_shadow_mask", (VertexFormat)DefaultVertexFormat.f_85815_, (VertexFormat.Mode)VertexFormat.Mode.TRIANGLES, (int)262144, (boolean)false, (boolean)true, (RenderType.CompositeState)state);
    }

    static {
        FACES = Direction.values();
        QUAD_EDGE_VERTEX_INDICES = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        LIGHTING_FACE_PROBES = NeoShadowsOcclusionPipeline.createFaceProbes(1, true, 0.18f);
        OCCLUSION_FACE_PROBES = NeoShadowsOcclusionPipeline.createFaceProbes(1, true, 0.18f);
        HORIZONTAL_LIGHTING_FACE_PROBES = NeoShadowsOcclusionPipeline.createFaceProbes(3, true, 0.18f);
        HORIZONTAL_OCCLUSION_FACE_PROBES = NeoShadowsOcclusionPipeline.createFaceProbes(3, true, 0.18f);
        LIGHTING_FACE_PROBE_RECTS = NeoShadowsOcclusionPipeline.createProbeSupportRects(LIGHTING_FACE_PROBES);
        HORIZONTAL_LIGHTING_FACE_PROBE_RECTS = NeoShadowsOcclusionPipeline.createProbeSupportRects(HORIZONTAL_LIGHTING_FACE_PROBES);
        RECEIVER_OFFSETS = NeoShadowsProjection.buildReceiverOffsets();
        MASK_PASS = NeoShadowsEngine.buildMaskPass();
        CANDIDATE_KEYS_TL = ThreadLocal.withInitial(() -> new long[2048]);
        CANDIDATE_DISTANCES_TL = ThreadLocal.withInitial(() -> new double[2048]);
        CANDIDATE_MASKS_TL = ThreadLocal.withInitial(() -> new byte[2048]);
    }

    static final class RenderHelper
    extends RenderStateShard {
        static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY_STATE = f_110134_;
        static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY_STATE = f_110135_;
        static final RenderStateShard.DepthTestStateShard LEQUAL = f_110113_;
        static final RenderStateShard.CullStateShard NO_CULL = RenderStateShard.f_110110_;
        static final RenderStateShard.WriteMaskStateShard COLOR_ONLY = RenderStateShard.f_110115_;

        private RenderHelper() {
            super("luxium_neo_shadow_helper", () -> {}, () -> {});
        }
    }
}

