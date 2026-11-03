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
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.platform.RenderFrame;
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
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.client.neocpu.NeoCpuEntityShadows;
import com.vinlanx.luxium.client.neocpu.NeoCpuFloodShadowBuilder;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4fc;

public final class NeoCpuShadowsEngine {
    private static final double SOURCE_SEARCH_RADIUS = 66.0; private static final int MAX_SOURCES = 256; private static final long NO_SOURCE_CLEAR_DELAY_MS = 250L; private static final int STATIC_FINGERPRINT_CAMERA_GRID = 4; private static final RenderType SHADOW_MASK_RENDER_TYPE = buildShadowMaskRenderType ();
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
    private static volatile NeoCpuShadowsEngine.BuildRequest latestRequest;
    private static volatile NeoCpuShadowsEngine.BuildResult completedResult;
    private static volatile NeoCpuShadowsEngine.DynamicBuildRequest latestDynamicRequest;
    private static volatile NeoCpuShadowsEngine.DynamicBuildResult completedDynamicResult;
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activePolygons = Collections.emptyList();
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activeBlockPolygons = Collections.emptyList();
    private static volatile List<NeoCpuShadowTypes.ShadowPolygon> activeEntityPolygons = Collections.emptyList();
    private static volatile NeoCpuShadowTypes.StaticBuildOutput activeStaticOutput = NeoCpuShadowTypes.StaticBuildOutput.EMPTY;
    private static volatile long activeResultId;
    private static volatile long activeDynamicResultId;
    private static volatile long lastStaticFingerprint;
    private static volatile long lastEntityFingerprint;
    private static volatile boolean dirty = true;
    private static long nextRequestId = 1L;
    private static long noSourceSinceMs;
    @Nullable private static volatile ClientLevel trackedLevel;
    @Nullable private static TextureTarget shadowMaskTarget; @Nullable private static RenderTarget sceneCopyTarget; private NeoCpuShadowsEngine()


 {
    }

    public static boolean isActive() {
        return !NeoGpuVanilla.isConfiguredEnabled() && Config.isFeatureEnabled(Config.CLIENT.neoCpuShadowsEnabled);
    }

    public static long syncSharedLevel(@Nullable ClientLevel level) {
        return syncLevel(level);
    }

    public static boolean hasActivePolygons() {
        return isActive() && !activePolygons.isEmpty();
    }

    public static void onLightDataChanged() {
        if (!NeoCpuShadowsEngine.isActive()) {
            return;
        }
        dirty = true;
    }

    public static void prepareForShadowlessCapture(Minecraft mc, Camera camera) {
        NeoCpuShadowsEngine.syncLevel(mc.level);
        if (!NeoCpuShadowsEngine.isActive() || mc.level == null || mc.player == null || !TorchRtxState.get().isEnabled()) {
            if (!activePolygons.isEmpty()) {
                activePolygons = Collections.emptyList();
                NeoShadowsEngine.markShadowlessCaptureDirty();
            }
            return;
        }
        NeoCpuShadowsEngine.applyCompletedResult();
        NeoCpuShadowsEngine.scheduleIfNeeded(mc, camera);
    }

    public static void clear() {
        synchronized (NeoCpuShadowsEngine.class) {
            trackedLevel = null;
            resetWorldState();
        }
    }

    public static void onRenderLevel(RenderFrame event) {
        if (!(event.getStage() == RenderFrame.Stage.AFTER_PARTICLES)) {
return;
}

            if (!(!ReflectionSystem.isRenderingWorldPass() && !NeoShadowsEngine.isAnyShadowCapturePass())) {
return;
}

                Minecraft mc = Minecraft.getInstance();
                syncLevel(mc.level);
                if (!isActive()) {
                    if (!activePolygons.isEmpty()) {
                        activePolygons = Collections.emptyList();
                    }
                } else if (mc.level != null && mc.player != null && TorchRtxState.get().isEnabled()) {
                    applyCompletedResult();
                    scheduleIfNeeded(mc, event.getCamera());
                    renderActivePolygons(mc, event.getPoseStack(), event.getCamera(), event.getFrustum());
                } else {
                    activePolygons = Collections.emptyList();
                } } private static void scheduleIfNeeded(Minecraft mc, Camera camera) { Vec3 cameraPos = camera.getPosition();
        TorchRtxState rtx = TorchRtxState.get();
        long[] sources = rtx.getClosestSources(cameraPos, 4356.0, 256);
        if (sources.length == 0) {
            if (noSourceSinceMs == 0L) { noSourceSinceMs = System.currentTimeMillis(); }
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
        long staticFingerprint = NeoCpuShadowsEngine.staticFingerprint(camera.getBlockPosition(), sources, emissions);
        if (dirty || staticFingerprint != lastStaticFingerprint) {
            lastStaticFingerprint = staticFingerprint;
            lastEntityFingerprint = 0L;
            dirty = false;
            latestRequest = new BuildRequest(nextRequestId++, epoch, staticFingerprint, mc.level, sources, emissions);
            if (BUILD_QUEUED.compareAndSet(false, true)) {
                BUILDER_EXECUTOR.submit(NeoCpuShadowsEngine::runStaticBuildTask);
            }
            if (activeStaticOutput.isEmpty()) { return; }
        }
        if (activeStaticOutput.isEmpty() || !NeoCpuEntityShadows.isActive()) { return; }
        NeoCpuEntityShadows.CaptureState entityState = NeoCpuEntityShadows.updateState(mc.level, camera.getBlockPosition(), mc.getFrameTime());
        long entityFingerprint = entityState.fingerprint();
        if (entityFingerprint == lastEntityFingerprint) { return; }
        lastEntityFingerprint = entityFingerprint;
        latestDynamicRequest = new DynamicBuildRequest(nextRequestId++, epoch, entityFingerprint, mc.level, activeStaticOutput, entityState);
        if (DYNAMIC_BUILD_QUEUED.compareAndSet(false, true)) {
            DYNAMIC_EXECUTOR.submit(NeoCpuShadowsEngine::runDynamicBuildTask);
        }
    }

    private static void runStaticBuildTask() {
        BuildRequest request = latestRequest;
        try {
            if (request == null || request.epoch != WORLD_EPOCH.get() || request.level == null) { return; }
            NeoCpuFloodShadowBuilder builder = new NeoCpuFloodShadowBuilder(request.level);
            NeoCpuShadowTypes.StaticBuildOutput staticOutput = builder.buildStatic(request.sources, request.emissions);
            if (request.epoch == WORLD_EPOCH.get()) {
                ArrayList<NeoCpuShadowTypes.ShadowPolygon> blockPolygons = new ArrayList<NeoCpuShadowTypes.ShadowPolygon>();
                for (NeoCpuShadowTypes.PerSourceStatic ps : staticOutput.sources()) {
                    blockPolygons.addAll(ps.blockPolygons());
                }
                completedResult = new BuildResult(request.id, request.epoch, request.fingerprint, staticOutput, blockPolygons.isEmpty() ? List.of() : List.copyOf(blockPolygons)); }
        }
        catch (Throwable ignored) {
            dirty = true; } finally {

            BUILD_QUEUED.set(false);
            BuildRequest newest = latestRequest;
            if (newest != null && request != null && newest.id > request.id && BUILD_QUEUED.compareAndSet(false, true)) {
                BUILDER_EXECUTOR.submit(NeoCpuShadowsEngine::runStaticBuildTask);
            }
        }
    }
    private static void runDynamicBuildTask() {
        DynamicBuildRequest
        request = latestDynamicRequest;
        try {
            if (request == null || request.epoch != WORLD_EPOCH.get() || request.level == null) { return; }
            NeoCpuFloodShadowBuilder builder = new NeoCpuFloodShadowBuilder(request.level);
            List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons = builder.buildDynamicOverlay(request.staticOutput, request.entityState);
            if (request.epoch == WORLD_EPOCH.get()) {
                completedDynamicResult = new DynamicBuildResult(request.id, request.epoch, request.entityFingerprint, entityPolygons); }
        }
        catch (Throwable ignored) {
            lastEntityFingerprint = 0L; } finally {

            DYNAMIC_BUILD_QUEUED.set(false);
            DynamicBuildRequest newest = latestDynamicRequest;
            if (newest != null && request != null && newest.id > request.id && DYNAMIC_BUILD_QUEUED.compareAndSet(false, true)) {
                DYNAMIC_EXECUTOR.submit(NeoCpuShadowsEngine::runDynamicBuildTask);
            }
        }
    } private static void applyCompletedResult() { NeoCpuShadowsEngine.BuildResult


 staticResult = completedResult;
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
                mergeAndApplyPolygons(activeBlockPolygons, activeEntityPolygons);
            }
        }
        NeoCpuShadowsEngine.DynamicBuildResult dynamicResult = completedDynamicResult;
        if (dynamicResult != null && dynamicResult.id > activeDynamicResultId) {
            if (dynamicResult.epoch != WORLD_EPOCH.get()) {
                completedDynamicResult = null;
            } else {
                activeEntityPolygons = dynamicResult.entityPolygons;
                activeDynamicResultId = dynamicResult.id;
                completedDynamicResult = null;
                mergeAndApplyPolygons(activeBlockPolygons, activeEntityPolygons);
            }
        }
    }

    private static void mergeAndApplyPolygons(List<NeoCpuShadowTypes.ShadowPolygon> blockPolygons, List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons) {
        List<NeoCpuShadowTypes.ShadowPolygon> merged;
        if (blockPolygons.isEmpty() && entityPolygons.isEmpty()) { merged = Collections.emptyList();
        } else if (entityPolygons.isEmpty()) { merged = blockPolygons;
        } else if (blockPolygons.isEmpty()) { merged = entityPolygons;
        } else {
            ArrayList<NeoCpuShadowTypes.ShadowPolygon> combined = new ArrayList<>(blockPolygons.size() + entityPolygons.size());
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
        if (polygons.isEmpty() || !NeoShadowsEngine.isShadowlessSceneReady()) { return; }
        Vec3 cameraPos = camera.getPosition();
        RenderTarget main = mc.getMainRenderTarget();
        NeoCpuShadowsEngine.ensureTargets(main.viewWidth, main.viewHeight);
        if (shadowMaskTarget == null) { return; }
        shadowMaskTarget.clear(Minecraft.ON_OSX);
        shadowMaskTarget.copyDepthFrom(main);
        shadowMaskTarget.bindWrite(false);
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(SHADOW_MASK_RENDER_TYPE);
        Matrix4f pose = poseStack.last().pose();
        for (NeoCpuShadowTypes.ShadowPolygon polygon : polygons) {
            if (!polygon.facesCamera(cameraPos.x, cameraPos.y, cameraPos.z)) continue;
            if ((frustum != null && !frustum.isVisible(polygon.bounds))) continue;
            NeoCpuShadowsEngine.emitPolygon(pose, consumer, cameraPos, polygon);
        }
        bufferSource.endBatch(SHADOW_MASK_RENDER_TYPE);
        main.bindWrite(false);
        NeoCpuShadowsEngine.composeShadowMask(mc);
    } private static void emitPolygon(Matrix4f pose, VertexConsumer consumer, Vec3 cameraPos, NeoCpuShadowTypes.ShadowPolygon polygon) { double[]


 vertices = polygon.vertices;
        for (int offset = 0; offset < vertices.length; offset += 3) {
            consumer.vertex(pose, (float)(vertices[offset] - cameraPos.x), (float)(vertices[offset + 1] - cameraPos.y), (float)(vertices[offset + 2] - cameraPos.z) ) .color(1.0F, 1.0F, 1.0F, 1.0F) .endVertex();
        }
        }

    private static void ensureTargets(int width, int height) {
        if (shadowMaskTarget == null) {
            shadowMaskTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            shadowMaskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        } else if (shadowMaskTarget.width != width || shadowMaskTarget.height != height) {
            shadowMaskTarget.resize(width, height, Minecraft.ON_OSX);
            shadowMaskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        if (sceneCopyTarget == null) {
            sceneCopyTarget = new RenderTarget(false) {
            }; sceneCopyTarget.createBuffers(width, height, Minecraft.ON_OSX);
            sceneCopyTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        } else if (sceneCopyTarget.viewWidth != width || sceneCopyTarget.viewHeight != height) {
            sceneCopyTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private static void composeShadowMask(Minecraft mc) {
        if (shadowMaskTarget == null || sceneCopyTarget == null || !NeoShadowsEngine.isShadowlessSceneReady()) { return; }
        int sceneDepthTexture = SharedPostResources.getDepthTextureId();
        int shadowlessTexture = NeoShadowsEngine.getShadowlessSceneColorTextureId();
        int shadowlessDepthTexture = NeoShadowsEngine.getShadowlessSceneDepthTextureId();
        if (sceneDepthTexture < 0 || shadowlessTexture < 0 || shadowlessDepthTexture < 0) { return; }
        ShaderInstance shader = ShaderManager.getFloodShadowCompositeShader();
        if (shader == null) { return; }
        RenderTarget main = mc.getMainRenderTarget();
        NeoCpuShadowsEngine.ensureTargets(main.viewWidth, main.viewHeight);
        sceneCopyTarget.clear(Minecraft.ON_OSX);
        GL30.glBindFramebuffer((int)36008, (int)main.frameBufferId);
        GL30.glBindFramebuffer((int)36009, (int)NeoCpuShadowsEngine.sceneCopyTarget.frameBufferId);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.viewWidth, (int)main.viewHeight, (int)0, (int)0, (int)main.viewWidth, (int)main.viewHeight, (int)16384, (int)9728);
        GL30.glBindFramebuffer((int)36160, (int)0);
        main.bindWrite(false);
        int width = main.viewWidth;
        int height = main.viewHeight;
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        int savedShaderTex3 = RenderSystem.getShaderTexture((int)3);
        int savedShaderTex4 = RenderSystem.getShaderTexture((int)4);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.ORTHOGRAPHIC_Z);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            shader.setSampler("SceneSampler", sceneCopyTarget.getColorTextureId());
            shader.setSampler("ShadowlessSceneSampler", shadowlessTexture);
            shader.setSampler("ShadowlessDepthSampler", shadowlessDepthTexture);
            shader.setSampler("ShadowMaskSampler", shadowMaskTarget.getColorTextureId());
            shader.setSampler("DepthSampler", sceneDepthTexture);
            shader.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            shader.PROJECTION_MATRIX.set(ortho);
            shader.apply();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(0.0, (double)height, 0.0).uv(0.0f, 0.0f).endVertex();
            buffer.vertex((double)width, (double)height, 0.0).uv(1.0f, 0.0f).endVertex();
            buffer.vertex((double)width, 0.0, 0.0).uv(1.0f, 1.0f).endVertex();
            buffer.vertex(0.0, 0.0, 0.0).uv(0.0f, 1.0f).endVertex();
            BufferUploader.draw((BufferBuilder.RenderedBuffer)buffer.end()); } finally {

            shader.clear();
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
            main.bindWrite(true);
        }
    } private static long staticFingerprint(BlockPos cameraBlock, long[] sources, int[] emissions) { long


 hash = -7046029254386353131L;
        hash = mix(hash, BlockPos.asLong(cameraBlock.getX
() >> 4, cameraBlock.getY
 () >> 4, cameraBlock.getZ
 () >> 4));
        for (int i = 0; i < sources.length; i++) {
            long sourceHash = -4417276706812531889L;
            sourceHash = mix(sourceHash, sources[i]);
            sourceHash = mix(sourceHash, (long)emissions[i]);
            hash += sourceHash;
            hash ^= Long.rotateLeft(sourceHash, (i & 15) + 1);
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        hash ^= value + -7046029254386353131L + (hash << 6) + (hash >>> 2);
        return hash;
    }

    private static long syncLevel(@Nullable ClientLevel level) {
        ClientLevel current = trackedLevel;
        if (current == level) { return WORLD_EPOCH.get();
        } else { synchronized (NeoCpuShadowsEngine.class) {
                if (trackedLevel != level) {
                    trackedLevel = level;
                    resetWorldState();
                }
                return WORLD_EPOCH.get();
            }
        }
    }

    private static void resetWorldState() { WORLD_EPOCH.incrementAndGet();
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
        CompositeState state = CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
            .setTransparencyState(NeoCpuShadowsEngine.RenderHelper.ADDITIVE_TRANSPARENCY_STATE)
            .setDepthTestState(NeoCpuShadowsEngine.RenderHelper.LEQUAL)
            .setCullState(NeoCpuShadowsEngine.RenderHelper.NO_CULL_STATE)
            .setWriteMaskState(NeoCpuShadowsEngine.RenderHelper.COLOR_ONLY)
            .createCompositeState(false);
        return RenderType.create("luxium_neocpu_shadow_mask", DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLES, 262144, false, true, state);
    }

    private static record BuildRequest(long id, long epoch, long fingerprint, ClientLevel level, long[] sources, int[] emissions) { }


    private static record BuildResult(
        long id, long epoch, long fingerprint, NeoCpuShadowTypes.StaticBuildOutput staticOutput, List<NeoCpuShadowTypes.ShadowPolygon> blockPolygons ) { }

private static record DynamicBuildRequest(
        long id, long epoch, long entityFingerprint, ClientLevel level, NeoCpuShadowTypes.StaticBuildOutput staticOutput, NeoCpuEntityShadows.CaptureState entityState ) { }

private static record DynamicBuildResult(long id, long epoch, long entityFingerprint, List<NeoCpuShadowTypes.ShadowPolygon> entityPolygons) { }

    static final class RenderHelper extends RenderStateShard {
        private RenderHelper() {

 super("luxium_neocpu_shadow_helper", () -> { }, () -> { });
 }
        static final RenderStateShard.TransparencyStateShard
 ADDITIVE_TRANSPARENCY_STATE = ADDITIVE_TRANSPARENCY;
        static final RenderStateShard.DepthTestStateShard LEQUAL = LEQUAL_DEPTH_TEST;
        static final RenderStateShard.CullStateShard NO_CULL_STATE = NO_CULL; static final RenderStateShard.WriteMaskStateShard
 COLOR_ONLY = COLOR_WRITE;
    }
    }
