/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$CullStateShard
 *  net.minecraft.client.renderer.RenderStateShard$DepthTestStateShard
 *  net.minecraft.client.renderer.RenderStateShard$ShaderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TransparencyStateShard
 *  net.minecraft.client.renderer.RenderStateShard$WriteMaskStateShard
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.neocpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.neocpu.NeoCpuEntityShadows;
import com.vinlanx.luxium.client.neocpu.NeoCpuFloodShadowBuilder;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class NeoCpuShadowsEngine {
    private static final double SOURCE_SEARCH_RADIUS = 66.0;
    private static final int MAX_SOURCES = 256;
    private static final long NO_SOURCE_CLEAR_DELAY_MS = 250L;
    private static final int STATIC_FINGERPRINT_CAMERA_GRID = 4;
    private static final RenderType SHADOW_MASK_RENDER_TYPE = NeoCpuShadowsEngine.buildShadowMaskRenderType();
    private static final ExecutorService BUILDER_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NeoCPUUUShadows-Builder");
        thread.setDaemon(true);
        thread.setPriority(5);
        return thread;
    });
    private static final ExecutorService DYNAMIC_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NeoCPUUUShadows-Dynamic");
        thread.setDaemon(true);
        thread.setPriority(4);
        return thread;
    });
    private static final AtomicBoolean BUILD_QUEUED = new AtomicBoolean(false);
    private static final AtomicBoolean DYNAMIC_BUILD_QUEUED = new AtomicBoolean(false);
    private static final AtomicLong WORLD_EPOCH = new AtomicLong(1L);
    private static volatile BuildRequest latestRequest;
    private static volatile BuildResult completedResult;
    private static volatile DynamicBuildRequest latestDynamicRequest;
    private static volatile DynamicBuildResult completedDynamicResult;
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activePolygons;
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activeBlockPolygons;
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activeEntityPolygons;
    private static volatile NeoCpuShadowTypes.StaticBuildOutput activeStaticOutput;
    private static volatile long activeResultId;
    private static volatile long activeDynamicResultId;
    private static volatile long lastStaticFingerprint;
    private static volatile long lastEntityFingerprint;
    private static volatile boolean dirty;
    private static long nextRequestId;
    private static long noSourceSinceMs;
    @Nullable
    private static volatile ClientLevel trackedLevel;
    @Nullable
    private static TextureTarget shadowMaskTarget;
    @Nullable
    private static RenderTarget sceneCopyTarget;

    private NeoCpuShadowsEngine() {
    }

    public static boolean isActive() {
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled);
    }

    public static long syncSharedLevel(@Nullable ClientLevel level) {
        return NeoCpuShadowsEngine.syncLevel(level);
    }

    public static boolean hasActivePolygons() {
        return NeoCpuShadowsEngine.isActive() && !activePolygons.isEmpty();
    }

    public static void onLightDataChanged() {
        if (!NeoCpuShadowsEngine.isActive()) {
            return;
        }
        dirty = true;
    }

    public static void prepareForShadowlessCapture(Minecraft mc, Camera camera) {
        NeoCpuShadowsEngine.syncLevel(mc.f_91073_);
        if (!NeoCpuShadowsEngine.isActive() || mc.f_91073_ == null || mc.f_91074_ == null || !TorchRtxState.get().isEnabled()) {
            if (!activePolygons.isEmpty()) {
                activePolygons = Collections.emptyList();
                NeoShadowsEngine.markShadowlessCaptureDirty();
            }
            return;
        }
        NeoCpuShadowsEngine.applyCompletedResult();
        NeoCpuShadowsEngine.scheduleIfNeeded(mc, camera);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear() {
        Class<NeoCpuShadowsEngine> clazz = NeoCpuShadowsEngine.class;
        synchronized (NeoCpuShadowsEngine.class) {
            trackedLevel = null;
            NeoCpuShadowsEngine.resetWorldState();
            // ** MonitorExit[var0] (shouldn't be in output)
            return;
        }
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        NeoCpuShadowsEngine.syncLevel(mc.f_91073_);
        if (!NeoCpuShadowsEngine.isActive()) {
            if (!activePolygons.isEmpty()) {
                activePolygons = Collections.emptyList();
            }
            return;
        }
        if (mc.f_91073_ == null || mc.f_91074_ == null || !TorchRtxState.get().isEnabled()) {
            activePolygons = Collections.emptyList();
            return;
        }
        NeoCpuShadowsEngine.applyCompletedResult();
        NeoCpuShadowsEngine.scheduleIfNeeded(mc, event.getCamera());
        NeoCpuShadowsEngine.renderActivePolygons(mc, event.getPoseStack(), event.getCamera(), event.getFrustum());
    }

    private static void scheduleIfNeeded(Minecraft mc, Camera camera) {
        Vec3 cameraPos = camera.m_90583_();
        TorchRtxState rtx = TorchRtxState.get();
        long[] sources = rtx.getClosestSources(cameraPos, 4356.0, 256);
        if (sources.length == 0) {
            if (noSourceSinceMs == 0L) {
                noSourceSinceMs = System.currentTimeMillis();
            }
            completedResult = null;
            completedDynamicResult = null;
            latestRequest = null;
            latestDynamicRequest = null;
            activeResultId = 0L;
            activeDynamicResultId = 0L;
            lastStaticFingerprint = 0L;
            lastEntityFingerprint = 0L;
            activeStaticOutput = NeoCpuShadowTypes.StaticBuildOutput.EMPTY;
            dirty = true;
            if (!activePolygons.isEmpty() && System.currentTimeMillis() - noSourceSinceMs >= 250L) {
                activePolygons = Collections.emptyList();
                NeoShadowsEngine.markShadowlessCaptureDirty();
            }
            return;
        }
        noSourceSinceMs = 0L;
        int[] emissions = new int[sources.length];
        for (int i = 0; i < sources.length; ++i) {
            emissions[i] = rtx.getSourceEmission(sources[i]);
        }
        long epoch = WORLD_EPOCH.get();
        long staticFingerprint = NeoCpuShadowsEngine.staticFingerprint(camera.m_90588_(), sources, emissions);
        if (dirty || staticFingerprint != lastStaticFingerprint) {
            lastStaticFingerprint = staticFingerprint;
            lastEntityFingerprint = 0L;
            dirty = false;
            latestRequest = new BuildRequest(nextRequestId++, epoch, staticFingerprint, mc.f_91073_, sources, emissions);
            if (BUILD_QUEUED.compareAndSet(false, true)) {
                BUILDER_EXECUTOR.submit(NeoCpuShadowsEngine::runStaticBuildTask);
            }
            if (activeStaticOutput.isEmpty()) {
                return;
            }
        }
        if (activeStaticOutput.isEmpty() || !NeoCpuEntityShadows.isActive()) {
            return;
        }
        NeoCpuEntityShadows.CaptureState entityState = NeoCpuEntityShadows.updateState(mc.f_91073_, camera.m_90588_(), mc.m_91296_());
        long entityFingerprint = entityState.fingerprint();
        if (entityFingerprint == lastEntityFingerprint) {
            return;
        }
        lastEntityFingerprint = entityFingerprint;
        latestDynamicRequest = new DynamicBuildRequest(nextRequestId++, epoch, entityFingerprint, mc.f_91073_, activeStaticOutput, entityState);
        if (DYNAMIC_BUILD_QUEUED.compareAndSet(false, true)) {
            DYNAMIC_EXECUTOR.submit(NeoCpuShadowsEngine::runDynamicBuildTask);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void runStaticBuildTask() {
        BuildRequest request = latestRequest;
        try {
            if (request == null || request.epoch != WORLD_EPOCH.get() || request.level == null) {
                return;
            }
            NeoCpuFloodShadowBuilder builder = new NeoCpuFloodShadowBuilder(request.level);
            NeoCpuShadowTypes.StaticBuildOutput staticOutput = builder.buildStatic(request.sources, request.emissions);
            if (request.epoch == WORLD_EPOCH.get()) {
                ArrayList<NeoCpuShadowTypes.ShadowPolygon> blockPolygons = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>();
                for (NeoCpuShadowTypes.PerSourceStatic ps : staticOutput.sources()) {
                    blockPolygons.addAll(ps.blockPolygons());
                }
                completedResult = new BuildResult(request.id, request.epoch, request.fingerprint, staticOutput, blockPolygons.isEmpty() ? List.of() : List.copyOf(blockPolygons));
            }
        }
        catch (Throwable ignored) {
            dirty = true;
        }
        finally {
            BUILD_QUEUED.set(false);
            BuildRequest newest = latestRequest;
            if (newest != null && request != null && newest.id > request.id && BUILD_QUEUED.compareAndSet(false, true)) {
                BUILDER_EXECUTOR.submit(NeoCpuShadowsEngine::runStaticBuildTask);
            }
        }
    }

    private static void runDynamicBuildTask() {
        DynamicBuildRequest request = latestDynamicRequest;
        try {
            if (request == null || request.epoch != WORLD_EPOCH.get() || request.level == null) {
                return;
            }
            NeoCpuFloodShadowBuilder builder = new NeoCpuFloodShadowBuilder(request.level);
            List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons = builder.buildDynamicOverlay(request.staticOutput, request.entityState);
            if (request.epoch == WORLD_EPOCH.get()) {
                completedDynamicResult = new DynamicBuildResult(request.id, request.epoch, request.entityFingerprint, entityPolygons);
            }
        }
        catch (Throwable ignored) {
            lastEntityFingerprint = 0L;
        }
        finally {
            DYNAMIC_BUILD_QUEUED.set(false);
            DynamicBuildRequest newest = latestDynamicRequest;
            if (newest != null && request != null && newest.id > request.id && DYNAMIC_BUILD_QUEUED.compareAndSet(false, true)) {
                DYNAMIC_EXECUTOR.submit(NeoCpuShadowsEngine::runDynamicBuildTask);
            }
        }
    }

    private static void applyCompletedResult() {
        DynamicBuildResult dynamicResult;
        BuildResult staticResult = completedResult;
        if (staticResult != null && staticResult.id > activeResultId) {
            if (staticResult.epoch != WORLD_EPOCH.get()) {
                completedResult = null;
            } else {
                activeStaticOutput = staticResult.staticOutput;
                activeBlockPolygons = staticResult.blockPolygons;
                activeEntityPolygons = Collections.emptyList();
                activeDynamicResultId = 0L;
                activeResultId = staticResult.id;
                completedResult = null;
                NeoCpuShadowsEngine.mergeAndApplyPolygons(activeBlockPolygons, activeEntityPolygons);
            }
        }
        if ((dynamicResult = completedDynamicResult) != null && dynamicResult.id > activeDynamicResultId) {
            if (dynamicResult.epoch != WORLD_EPOCH.get()) {
                completedDynamicResult = null;
            } else {
                activeEntityPolygons = dynamicResult.entityPolygons;
                activeDynamicResultId = dynamicResult.id;
                completedDynamicResult = null;
                NeoCpuShadowsEngine.mergeAndApplyPolygons(activeBlockPolygons, activeEntityPolygons);
            }
        }
    }

    private static void mergeAndApplyPolygons(List<NeoCpuShadowTypes.ShadowPolygon> blockPolygons, List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons) {
        List<NeoCpuShadowTypes.ShadowPolygon> merged;
        if (blockPolygons.isEmpty() && entityPolygons.isEmpty()) {
            merged = Collections.emptyList();
        } else if (entityPolygons.isEmpty()) {
            merged = blockPolygons;
        } else if (blockPolygons.isEmpty()) {
            merged = entityPolygons;
        } else {
            ArrayList<NeoCpuShadowTypes.ShadowPolygon> combined = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>(blockPolygons.size() + entityPolygons.size());
            combined.addAll(blockPolygons);
            combined.addAll(entityPolygons);
            merged = List.copyOf(combined);
        }
        if (merged != activePolygons) {
            activePolygons = merged;
            NeoShadowsEngine.markShadowlessCaptureDirty();
        }
    }

    private static void renderActivePolygons(Minecraft mc, PoseStack poseStack, Camera camera, @Nullable Frustum frustum) {
        List<NeoCpuShadowTypes.ShadowPolygon> polygons = activePolygons;
        if (polygons.isEmpty() || !NeoShadowsEngine.isShadowlessSceneReady()) {
            return;
        }
        Vec3 cameraPos = camera.m_90583_();
        RenderTarget main = mc.m_91385_();
        NeoCpuShadowsEngine.ensureTargets(main.f_83917_, main.f_83918_);
        if (shadowMaskTarget == null) {
            return;
        }
        shadowMaskTarget.m_83954_(Minecraft.f_91002_);
        shadowMaskTarget.m_83945_(main);
        shadowMaskTarget.m_83947_(false);
        MultiBufferSource.BufferSource bufferSource = mc.m_91269_().m_110104_();
        VertexConsumer consumer = bufferSource.m_6299_(SHADOW_MASK_RENDER_TYPE);
        Matrix4f pose = poseStack.m_85850_().m_252922_();
        for (NeoCpuShadowTypes.ShadowPolygon polygon : polygons) {
            if (!polygon.facesCamera(cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_) || frustum != null && !frustum.m_113029_(polygon.bounds)) continue;
            NeoCpuShadowsEngine.emitPolygon(pose, consumer, cameraPos, polygon);
        }
        bufferSource.m_109912_(SHADOW_MASK_RENDER_TYPE);
        main.m_83947_(false);
        NeoCpuShadowsEngine.composeShadowMask(mc);
    }

    private static void emitPolygon(Matrix4f pose, VertexConsumer consumer, Vec3 cameraPos, NeoCpuShadowTypes.ShadowPolygon polygon) {
        double[] vertices = polygon.vertices;
        for (int offset = 0; offset < vertices.length; offset += 3) {
            consumer.m_252986_(pose, (float)(vertices[offset] - cameraPos.f_82479_), (float)(vertices[offset + 1] - cameraPos.f_82480_), (float)(vertices[offset + 2] - cameraPos.f_82481_)).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        }
    }

    private static void ensureTargets(int width, int height) {
        if (shadowMaskTarget == null) {
            shadowMaskTarget = new TextureTarget(width, height, true, Minecraft.f_91002_);
            shadowMaskTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        } else if (NeoCpuShadowsEngine.shadowMaskTarget.f_83915_ != width || NeoCpuShadowsEngine.shadowMaskTarget.f_83916_ != height) {
            shadowMaskTarget.m_83941_(width, height, Minecraft.f_91002_);
            shadowMaskTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        }
        if (sceneCopyTarget == null) {
            sceneCopyTarget = new RenderTarget(false){};
            sceneCopyTarget.m_83950_(width, height, Minecraft.f_91002_);
            sceneCopyTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        } else if (NeoCpuShadowsEngine.sceneCopyTarget.f_83917_ != width || NeoCpuShadowsEngine.sceneCopyTarget.f_83918_ != height) {
            sceneCopyTarget.m_83941_(width, height, Minecraft.f_91002_);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void composeShadowMask(Minecraft mc) {
        if (shadowMaskTarget == null || sceneCopyTarget == null || !NeoShadowsEngine.isShadowlessSceneReady()) {
            return;
        }
        int sceneDepthTexture = SharedPostResources.getDepthTextureId();
        int shadowlessTexture = NeoShadowsEngine.getShadowlessSceneColorTextureId();
        int shadowlessDepthTexture = NeoShadowsEngine.getShadowlessSceneDepthTextureId();
        if (sceneDepthTexture < 0 || shadowlessTexture < 0 || shadowlessDepthTexture < 0) {
            return;
        }
        ShaderInstance shader = ShaderManager.getFloodShadowCompositeShader();
        if (shader == null) {
            return;
        }
        RenderTarget main = mc.m_91385_();
        NeoCpuShadowsEngine.ensureTargets(main.f_83917_, main.f_83918_);
        sceneCopyTarget.m_83954_(Minecraft.f_91002_);
        GL30.glBindFramebuffer((int)36008, (int)main.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)NeoCpuShadowsEngine.sceneCopyTarget.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_, (int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_, (int)16384, (int)9728);
        GL30.glBindFramebuffer((int)36160, (int)0);
        main.m_83947_(false);
        int width = main.f_83917_;
        int height = main.f_83918_;
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        int savedShaderTex3 = RenderSystem.getShaderTexture((int)3);
        int savedShaderTex4 = RenderSystem.getShaderTexture((int)4);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            shader.m_173350_("SceneSampler", (Object)sceneCopyTarget.m_83975_());
            shader.m_173350_("ShadowlessSceneSampler", (Object)shadowlessTexture);
            shader.m_173350_("ShadowlessDepthSampler", (Object)shadowlessDepthTexture);
            shader.m_173350_("ShadowMaskSampler", (Object)shadowMaskTarget.m_83975_());
            shader.m_173350_("DepthSampler", (Object)sceneDepthTexture);
            shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            shader.f_173309_.m_5679_(ortho);
            shader.m_173363_();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_(0.0, (double)height, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)width, (double)height, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)width, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.setShaderTexture((int)0, (int)savedShaderTex0);
            RenderSystem.setShaderTexture((int)1, (int)savedShaderTex1);
            RenderSystem.setShaderTexture((int)2, (int)savedShaderTex2);
            RenderSystem.setShaderTexture((int)3, (int)savedShaderTex3);
            RenderSystem.setShaderTexture((int)4, (int)savedShaderTex4);
            RenderSystem.activeTexture((int)33984);
            main.m_83947_(true);
        }
    }

    private static long staticFingerprint(BlockPos cameraBlock, long[] sources, int[] emissions) {
        long hash = -7046029254386353131L;
        hash = NeoCpuShadowsEngine.mix(hash, BlockPos.m_121882_((int)(cameraBlock.m_123341_() >> 4), (int)(cameraBlock.m_123342_() >> 4), (int)(cameraBlock.m_123343_() >> 4)));
        for (int i = 0; i < sources.length; ++i) {
            long sourceHash = -4417276706812531889L;
            sourceHash = NeoCpuShadowsEngine.mix(sourceHash, sources[i]);
            sourceHash = NeoCpuShadowsEngine.mix(sourceHash, emissions[i]);
            hash += sourceHash;
            hash ^= Long.rotateLeft(sourceHash, (i & 0xF) + 1);
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        hash ^= value + -7046029254386353131L + (hash << 6) + (hash >>> 2);
        return hash;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static long syncLevel(@Nullable ClientLevel level) {
        ClientLevel current = trackedLevel;
        if (current == level) {
            return WORLD_EPOCH.get();
        }
        Class<NeoCpuShadowsEngine> clazz = NeoCpuShadowsEngine.class;
        synchronized (NeoCpuShadowsEngine.class) {
            if (trackedLevel != level) {
                trackedLevel = level;
                NeoCpuShadowsEngine.resetWorldState();
            }
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return WORLD_EPOCH.get();
        }
    }

    private static void resetWorldState() {
        WORLD_EPOCH.incrementAndGet();
        latestRequest = null;
        latestDynamicRequest = null;
        completedResult = null;
        completedDynamicResult = null;
        activePolygons = Collections.emptyList();
        activeBlockPolygons = Collections.emptyList();
        activeEntityPolygons = Collections.emptyList();
        activeStaticOutput = NeoCpuShadowTypes.StaticBuildOutput.EMPTY;
        activeResultId = 0L;
        activeDynamicResultId = 0L;
        lastStaticFingerprint = 0L;
        lastEntityFingerprint = 0L;
        dirty = true;
        nextRequestId = 1L;
        noSourceSinceMs = 0L;
        NeoCpuEntityShadows.clear();
        NeoShadowsEngine.markShadowlessCaptureDirty();
    }

    private static RenderType buildShadowMaskRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.m_110628_().m_173292_(new RenderStateShard.ShaderStateShard(GameRenderer::m_172811_)).m_110685_(RenderHelper.ADDITIVE_TRANSPARENCY_STATE).m_110663_(RenderHelper.LEQUAL).m_110661_(RenderHelper.NO_CULL_STATE).m_110687_(RenderHelper.COLOR_ONLY).m_110691_(false);
        return RenderType.m_173215_((String)"luxium_neocpu_shadow_mask", (VertexFormat)DefaultVertexFormat.f_85815_, (VertexFormat.Mode)VertexFormat.Mode.TRIANGLES, (int)262144, (boolean)false, (boolean)true, (RenderType.CompositeState)state);
    }

    static {
        activePolygons = Collections.emptyList();
        activeBlockPolygons = Collections.emptyList();
        activeEntityPolygons = Collections.emptyList();
        activeStaticOutput = NeoCpuShadowTypes.StaticBuildOutput.EMPTY;
        dirty = true;
        nextRequestId = 1L;
    }

    private record BuildResult(long id, long epoch, long fingerprint, NeoCpuShadowTypes.StaticBuildOutput staticOutput, List<NeoCpuShadowTypes.ShadowPolygon> blockPolygons) {
    }

    private record DynamicBuildResult(long id, long epoch, long entityFingerprint, List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons) {
    }

    private record BuildRequest(long id, long epoch, long fingerprint, ClientLevel level, long[] sources, int[] emissions) {
    }

    private record DynamicBuildRequest(long id, long epoch, long entityFingerprint, ClientLevel level, NeoCpuShadowTypes.StaticBuildOutput staticOutput, NeoCpuEntityShadows.CaptureState entityState) {
    }

    static final class RenderHelper
    extends RenderStateShard {
        static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY_STATE = f_110135_;
        static final RenderStateShard.DepthTestStateShard LEQUAL = f_110113_;
        static final RenderStateShard.CullStateShard NO_CULL_STATE = f_110110_;
        static final RenderStateShard.WriteMaskStateShard COLOR_ONLY = f_110115_;

        private RenderHelper() {
            super("luxium_neocpu_shadow_helper", () -> {}, () -> {});
        }
    }
}
