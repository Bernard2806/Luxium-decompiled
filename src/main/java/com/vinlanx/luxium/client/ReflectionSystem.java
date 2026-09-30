/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.shaders.FogShape
 *  com.mojang.blaze3d.shaders.Uniform
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexBuffer
 *  com.mojang.blaze3d.vertex.VertexBuffer$Usage
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.CloudStatus
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.FogRenderer
 *  net.minecraft.client.renderer.FogRenderer$FogMode
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$CullStateShard
 *  net.minecraft.client.renderer.RenderStateShard$DepthTestStateShard
 *  net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard
 *  net.minecraft.client.renderer.RenderStateShard$LightmapStateShard
 *  net.minecraft.client.renderer.RenderStateShard$ShaderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TextureStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TransparencyStateShard
 *  net.minecraft.client.renderer.RenderStateShard$WriteMaskStateShard
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.reflections.RainPuddleManager;
import com.vinlanx.luxium.client.reflections.RainPuddleSurface;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import com.vinlanx.luxium.client.reflections.ReflectionMaterialRegistry;
import com.vinlanx.luxium.mixin.CameraAccessor;
import com.vinlanx.luxium.mixin.LevelRendererAccessor;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.soasnottointerfere.ExposedFaceService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class ReflectionSystem {
    private static final ReflectionSystem INSTANCE = new ReflectionSystem();
    private static final int MAX_RENDERED_PLANES = 4;
    private static final float FACE_EPSILON = 0.0015f;
    private static final int BOOTSTRAP_SCAN_RADIUS = 24;
    private static final int BOOTSTRAP_VERTICAL_RADIUS = 24;
    private static final int PROBE_FACE = 64;
    private static final long PROBE_UPDATE_MS = 1000L;
    private volatile boolean enabled = false;
    private boolean initialized = false;
    private boolean bilinearFilteringEnabled = true;
    private int recursionDepth = 0;
    @Nullable
    private Vec3 activeRenderCameraPos;
    private final List<PlaneRenderCache> planeRenderCaches = new ArrayList<PlaneRenderCache>(4);
    private DynamicTexture probeTexture;
    private ResourceLocation probeLoc;
    private RenderType probeRenderType;
    private long lastProbeMs;
    private BlockPos probeCenter;
    private final Camera reflectionCamera = new Camera();
    private final Matrix4f frameViewMatrix = new Matrix4f();
    private final Matrix4f frameProjectionMatrix = new Matrix4f();
    private float framePartialTick;
    private long frameFinishNano;
    private GameRenderer frameGameRenderer;
    private LightTexture frameLightTexture;
    private boolean frameContextReady = false;
    private long lastPlaneScanMs = 0L;
    private int lastPlaneScanBlockX = Integer.MIN_VALUE;
    private int lastPlaneScanBlockY = Integer.MIN_VALUE;
    private int lastPlaneScanBlockZ = Integer.MIN_VALUE;
    private int lastPlaneScanDistance = Integer.MIN_VALUE;
    private final List<MirrorPlane> activePlanes = new ArrayList<MirrorPlane>();
    private int activePassPlaneIndex = -1;
    private int activePassViewportWidth;
    private int activePassViewportHeight;
    private boolean[][] feedDemand = new boolean[4][4];
    private double lastCamX;
    private double lastCamY;
    private double lastCamZ;
    private float lastCamYaw;
    private float lastCamPitch;
    private boolean cameraMovedThisFrame = true;
    private volatile boolean worldDirty = false;
    private long lastWorldDirtyMs = 0L;
    private static final long WORLD_DIRTY_DEBOUNCE_MS = 50L;
    private final Matrix4f cachedOriginalProjection = new Matrix4f();
    private final Matrix3f cachedOriginalInverseView = new Matrix3f();
    private static final int SLOT_EXACT = 0;
    private static final int SLOT_FEED = 1;
    private static final float FEED_FAR_PLANE = 1536.0f;
    private static final float FEED_WORLD_HALF_EXTENT = 24.0f;
    private final Matrix4f feedScratchMirrorLinear = new Matrix4f();
    private final Matrix4f feedScratchProj = new Matrix4f();
    private final Vec3[] feedScratchVirtCam = new Vec3[1];

    private ReflectionSystem() {
    }

    public static ReflectionSystem get() {
        return INSTANCE;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (NeoShadowsEngine.isShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        ReflectionSystem reflect = ReflectionSystem.get();
        reflect.setEnabled(Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled));
        if (reflect.recursionDepth == 0) {
            reflect.updateProbe(mc);
        }
        if (reflect.isEnabled()) {
            reflect.renderPlanar(mc, event.getPoseStack(), (MultiBufferSource)mc.m_91269_().m_110104_());
        }
    }

    public static boolean isRenderingWorldPass() {
        return ReflectionSystem.INSTANCE.recursionDepth > 0;
    }

    @Nullable
    public static Vec3 getActiveRenderCameraPos() {
        return ReflectionSystem.INSTANCE.activeRenderCameraPos;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            this.forceImmediateUpdate();
            this.bootstrapNearbyFaces(Minecraft.m_91087_());
        } else {
            this.activePlanes.clear();
            for (PlaneRenderCache cache : this.planeRenderCaches) {
                cache.invalidate();
                cache.closeMesh();
                cache.lastPlaneIdentity = Long.MIN_VALUE;
            }
            this.frameContextReady = false;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91060_ != null) {
            mc.f_91060_.m_109818_();
        }
    }

    public void markWorldDirty() {
        long now = System.currentTimeMillis();
        if (now - this.lastWorldDirtyMs < 50L) {
            return;
        }
        this.lastWorldDirtyMs = now;
        this.worldDirty = true;
    }

    public void forceImmediateUpdate() {
        this.lastPlaneScanMs = 0L;
        this.lastPlaneScanBlockX = Integer.MIN_VALUE;
        this.lastPlaneScanBlockY = Integer.MIN_VALUE;
        this.lastPlaneScanBlockZ = Integer.MIN_VALUE;
        this.lastPlaneScanDistance = Integer.MIN_VALUE;
        this.worldDirty = true;
    }

    private void bootstrapNearbyFaces(Minecraft mc) {
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        int horizontalRadius = Math.min(24, (int)Math.ceil(ReflectionSystem.getMirrorScanRadius()));
        int verticalRadius = Math.min(24, horizontalRadius);
        ExposedFaceService.get().bootstrapAround(mc.f_91073_, mc.f_91074_.m_20183_(), horizontalRadius, verticalRadius);
        TorchRtxState.get().requestReflectiveSnapshotRefresh();
        this.worldDirty = true;
    }

    public void onMainRenderLevel(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, long finishNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.m_91087_();
        boolean configuredEnabled = Config.isFeatureEnabled(Config.CLIENT.reflectionEnabled);
        if (this.enabled != configuredEnabled) {
            this.setEnabled(configuredEnabled);
        }
        if (!this.enabled || mc.f_91073_ == null || mc.f_91074_ == null || this.recursionDepth > 0) {
            return;
        }
        this.ensureInit(mc);
        this.frameViewMatrix.set((Matrix4fc)poseStack.m_85850_().m_252922_());
        this.frameProjectionMatrix.set((Matrix4fc)projectionMatrix);
        this.framePartialTick = partialTick;
        this.frameFinishNano = finishNano;
        this.frameGameRenderer = gameRenderer;
        this.frameLightTexture = lightTexture;
        this.frameContextReady = true;
        Vec3 mainCameraPos = camera.m_90583_();
        double cx = mainCameraPos.f_82479_;
        double cy = mainCameraPos.f_82480_;
        double cz = mainCameraPos.f_82481_;
        float yaw = camera.m_90590_();
        float pitch = camera.m_90589_();
        this.cameraMovedThisFrame = cx != this.lastCamX || cy != this.lastCamY || cz != this.lastCamZ || yaw != this.lastCamYaw || pitch != this.lastCamPitch;
        this.lastCamX = cx;
        this.lastCamY = cy;
        this.lastCamZ = cz;
        this.lastCamYaw = yaw;
        this.lastCamPitch = pitch;
        this.updateMirrorPlanes(mc, mainCameraPos);
        this.updatePlaneRenderCaches(mc, mainCameraPos);
    }

    public void updateProbe(Minecraft mc) {
        if (!this.enabled || mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        this.ensureInit(mc);
        long now = System.currentTimeMillis();
        if (now - this.lastProbeMs < 1000L) {
            return;
        }
        this.lastProbeMs = now;
        this.probeCenter = mc.f_91074_.m_20183_();
        this.fillProbeCross(mc);
    }

    public void renderPlanar(Minecraft mc, PoseStack mainPoseStack, MultiBufferSource buffers) {
        if (!this.enabled || !this.initialized || !this.frameContextReady || this.activePlanes.isEmpty()) {
            return;
        }
        Vec3 cameraPos = this.recursionDepth > 0 && this.activeRenderCameraPos != null ? this.activeRenderCameraPos : mc.f_91063_.m_109153_().m_90583_();
        Frustum currentFrustum = new Frustum(mainPoseStack.m_85850_().m_252922_(), RenderSystem.getProjectionMatrix());
        currentFrustum.m_113002_(cameraPos.f_82479_, cameraPos.f_82480_, cameraPos.f_82481_);
        this.cachedOriginalProjection.set((Matrix4fc)RenderSystem.getProjectionMatrix());
        this.cachedOriginalInverseView.set((Matrix3fc)mainPoseStack.m_85850_().m_252943_()).invert();
        RenderTarget originalTarget = mc.m_91385_();
        int planeCount = Math.min(this.activePlanes.size(), this.planeRenderCaches.size());
        for (int planeIndex = 0; planeIndex < planeCount; ++planeIndex) {
            int slotIndex;
            Vec3 slotVirtualCamPos;
            Matrix4f slotVirtualProj;
            Matrix4f slotMirrorLinear;
            boolean nested;
            MirrorPlane plane = this.activePlanes.get(planeIndex);
            if (!plane.isFacingCamera(cameraPos) || !currentFrustum.m_113029_(plane.getCullingBounds())) continue;
            PlaneRenderCache cache = this.planeRenderCaches.get(planeIndex);
            boolean bl = nested = this.recursionDepth > 0;
            if (nested) {
                if (!cache.feedValid || cache.feedVirtualCamPos == null) continue;
                slotMirrorLinear = cache.feedMirrorLinear;
                slotVirtualProj = cache.feedVirtualProj;
                slotVirtualCamPos = cache.feedVirtualCamPos;
                slotIndex = 1;
                cache.activeUvScaleX = cache.feedUvScaleX;
                cache.activeUvScaleY = cache.feedUvScaleY;
            } else {
                if (!cache.exactValid || cache.exactVirtualCamPos == null) continue;
                slotMirrorLinear = cache.exactMirrorLinear;
                slotVirtualProj = cache.exactVirtualProj;
                slotVirtualCamPos = cache.exactVirtualCamPos;
                slotIndex = 0;
                cache.activeUvScaleX = cache.exactUvScaleX;
                cache.activeUvScaleY = cache.exactUvScaleY;
            }
            cache.textureRef.setGlId(cache.targets[slotIndex].m_83975_());
            RenderSystem.setProjectionMatrix((Matrix4f)this.cachedOriginalProjection, (VertexSorting)VertexSorting.f_276450_);
            RenderSystem.setInverseViewRotationMatrix((Matrix3f)this.cachedOriginalInverseView);
            originalTarget.m_83947_(false);
            RenderSystem.viewport((int)0, (int)0, (int)(this.recursionDepth > 0 ? this.activePassViewportWidth : originalTarget.f_83917_), (int)(this.recursionDepth > 0 ? this.activePassViewportHeight : originalTarget.f_83918_));
            this.drawPlane(mainPoseStack, plane, cache, slotMirrorLinear, slotVirtualProj, slotVirtualCamPos, cameraPos);
        }
        RenderSystem.setProjectionMatrix((Matrix4f)this.cachedOriginalProjection, (VertexSorting)VertexSorting.f_276450_);
        RenderSystem.setInverseViewRotationMatrix((Matrix3f)this.cachedOriginalInverseView);
        originalTarget.m_83947_(false);
        RenderSystem.viewport((int)0, (int)0, (int)(this.recursionDepth > 0 ? this.activePassViewportWidth : originalTarget.f_83917_), (int)(this.recursionDepth > 0 ? this.activePassViewportHeight : originalTarget.f_83918_));
    }

    private void updatePlaneRenderCaches(Minecraft mc, Vec3 mainCameraPos) {
        int consumer;
        int i;
        if (this.activePlanes.isEmpty()) {
            return;
        }
        int planeCount = Math.min(this.activePlanes.size(), this.planeRenderCaches.size());
        for (int i2 = 0; i2 < planeCount; ++i2) {
            MirrorPlane plane = this.activePlanes.get(i2);
            PlaneRenderCache cache = this.planeRenderCaches.get(i2);
            long planeIdentity = ReflectionSystem.computePlaneIdentity(plane);
            if (cache.lastPlaneIdentity != planeIdentity) {
                cache.exactValid = false;
                cache.feedValid = false;
                cache.feedConsumerIdentity = Long.MIN_VALUE;
                cache.lastPlaneIdentity = planeIdentity;
            }
            cache.ensureMesh(plane, ReflectionSystem.computePlaneSignature(plane));
        }
        Frustum playerFrustum = new Frustum(this.frameViewMatrix, this.frameProjectionMatrix);
        playerFrustum.m_113002_(mainCameraPos.f_82479_, mainCameraPos.f_82480_, mainCameraPos.f_82481_);
        RenderTarget originalTarget = mc.m_91385_();
        Vec3[] exactCamOf = new Vec3[planeCount];
        boolean[] playerSeesArr = new boolean[planeCount];
        for (i = 0; i < planeCount; ++i) {
            MirrorPlane plane = this.activePlanes.get(i);
            double signedDist = mainCameraPos.m_82546_(plane.planePoint).m_82526_(plane.normal);
            if (!(signedDist > 0.05)) continue;
            exactCamOf[i] = ReflectionSystem.reflectPoint(mainCameraPos, plane.planePoint, plane.normal);
            playerSeesArr[i] = playerFrustum.m_113029_(plane.getCullingBounds());
        }
        for (i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                this.feedDemand[i][j] = false;
            }
        }
        for (consumer = 0; consumer < planeCount; ++consumer) {
            if (!playerSeesArr[consumer] || exactCamOf[consumer] == null) continue;
            Matrix4f consumerView = new Matrix4f((Matrix4fc)this.frameViewMatrix).mul((Matrix4fc)ReflectionSystem.axisReflectionMatrix(this.activePlanes.get((int)consumer).face.m_122434_()));
            Frustum consumerFrustum = new Frustum(consumerView, this.frameProjectionMatrix);
            Vec3 consumerCamera = exactCamOf[consumer];
            consumerFrustum.m_113002_(consumerCamera.f_82479_, consumerCamera.f_82480_, consumerCamera.f_82481_);
            for (int producer = 0; producer < planeCount; ++producer) {
                if (producer == consumer) continue;
                MirrorPlane producerPlane = this.activePlanes.get(producer);
                this.feedDemand[consumer][producer] = producerPlane.isFacingCamera(consumerCamera) && consumerFrustum.m_113029_(producerPlane.getCullingBounds());
            }
        }
        for (consumer = 0; consumer < planeCount; ++consumer) {
            for (int producer = 0; producer < planeCount; ++producer) {
                if (!this.feedDemand[consumer][producer]) continue;
                this.feedDemand[producer][consumer] = true;
            }
        }
        if (planeCount > 1) {
            for (i = 0; i < planeCount; ++i) {
                int consumer2;
                MirrorPlane plane = this.activePlanes.get(i);
                PlaneRenderCache cache = this.planeRenderCaches.get(i);
                Vec3 feedViewer = null;
                int feedConsumer = -1;
                for (consumer2 = 0; consumer2 < planeCount; ++consumer2) {
                    if (ReflectionSystem.computePlaneIdentity(this.activePlanes.get(consumer2)) != cache.feedConsumerIdentity || !this.feedDemand[consumer2][i] || exactCamOf[consumer2] == null) continue;
                    feedViewer = exactCamOf[consumer2];
                    feedConsumer = consumer2;
                    break;
                }
                for (consumer2 = 0; consumer2 < planeCount && feedViewer == null; ++consumer2) {
                    if (!this.feedDemand[consumer2][i] || exactCamOf[consumer2] == null) continue;
                    feedViewer = exactCamOf[consumer2];
                    feedConsumer = consumer2;
                }
                if (feedViewer == null || !this.computeFeedMatrices(plane, feedViewer, mainCameraPos, this.feedScratchMirrorLinear, this.feedScratchProj, this.feedScratchVirtCam)) {
                    cache.feedValid = false;
                    continue;
                }
                cache.textureRef.setGlId(cache.targets[1].m_83975_());
                this.activePassPlaneIndex = feedConsumer;
                this.renderPlanePassEx(mc, cache.targets[1], this.feedScratchVirtCam[0], this.feedScratchMirrorLinear, this.feedScratchProj, this.feedScratchProj, cache.targets[1].f_83917_, cache.targets[1].f_83918_);
                cache.feedMirrorLinear.set((Matrix4fc)this.feedScratchMirrorLinear);
                cache.feedVirtualProj.set((Matrix4fc)this.feedScratchProj);
                cache.feedVirtualCamPos = this.feedScratchVirtCam[0];
                cache.feedUvScaleX = 1.0f;
                cache.feedUvScaleY = 1.0f;
                cache.feedValid = true;
                cache.feedConsumerIdentity = ReflectionSystem.computePlaneIdentity(this.activePlanes.get(feedConsumer));
            }
        } else {
            for (i = 0; i < planeCount; ++i) {
                this.planeRenderCaches.get((int)i).feedValid = false;
            }
        }
        for (i = 0; i < planeCount; ++i) {
            MirrorPlane plane = this.activePlanes.get(i);
            PlaneRenderCache cache = this.planeRenderCaches.get(i);
            if (!playerSeesArr[i]) {
                cache.exactValid = false;
                continue;
            }
            Vec3 virtualCamPos = exactCamOf[i];
            Matrix4f mirrorLinear = new Matrix4f((Matrix4fc)this.frameViewMatrix).mul((Matrix4fc)ReflectionSystem.axisReflectionMatrix(plane.face.m_122434_()));
            Vector4f clipPlaneVS = ReflectionSystem.worldPlaneToViewSpace(mirrorLinear, virtualCamPos, plane.normal, plane.planePoint);
            Matrix4f virtualProj = ReflectionSystem.applyObliqueNearClip(new Matrix4f((Matrix4fc)this.frameProjectionMatrix), clipPlaneVS);
            CropRect crop = ReflectionSystem.computeCropRect(plane, mainCameraPos, this.frameViewMatrix, this.frameProjectionMatrix, cache.targets[0].f_83917_, cache.targets[0].f_83918_);
            if (crop == null) {
                cache.exactValid = false;
                continue;
            }
            Matrix4f croppedProj = ReflectionSystem.applyProjectionCrop(virtualProj, crop);
            cache.textureRef.setGlId(cache.targets[0].m_83975_());
            this.activePassPlaneIndex = i;
            this.renderPlanePassEx(mc, cache.targets[0], virtualCamPos, mirrorLinear, croppedProj, croppedProj, crop.width, crop.height);
            cache.exactMirrorLinear.set((Matrix4fc)mirrorLinear);
            cache.exactVirtualProj.set((Matrix4fc)croppedProj);
            cache.exactVirtualCamPos = virtualCamPos;
            cache.exactUvScaleX = (float)crop.width / (float)cache.targets[0].f_83917_;
            cache.exactUvScaleY = (float)crop.height / (float)cache.targets[0].f_83918_;
            cache.exactValid = true;
        }
        this.activePassPlaneIndex = -1;
        originalTarget.m_83947_(true);
        this.worldDirty = false;
    }

    @Nullable
    private static CropRect computeCropRect(MirrorPlane plane, Vec3 cameraPos, Matrix4f view, Matrix4f projection, int targetWidth, int targetHeight) {
        if (plane.face.m_122434_() == Direction.Axis.Y) {
            return new CropRect(0, 0, targetWidth, targetHeight, targetWidth, targetHeight);
        }
        double planeDistance = Math.abs(cameraPos.m_82546_(plane.planePoint).m_82526_(plane.normal));
        if (planeDistance < 2.5) {
            return new CropRect(0, 0, targetWidth, targetHeight, targetWidth, targetHeight);
        }
        Matrix4f viewProjection = new Matrix4f((Matrix4fc)projection).mul((Matrix4fc)view);
        Vector3f corner = new Vector3f();
        Vector4f clip = new Vector4f();
        float minX = 1.0f;
        float maxX = -1.0f;
        float minY = 1.0f;
        float maxY = -1.0f;
        boolean projected = false;
        boolean crossesNearPlane = false;
        for (int i = 0; i < 4; ++i) {
            plane.getRectCorner(i, corner);
            clip.set(corner.x - (float)cameraPos.f_82479_, corner.y - (float)cameraPos.f_82480_, corner.z - (float)cameraPos.f_82481_, 1.0f);
            viewProjection.transform(clip);
            if (clip.w <= 1.0E-4f) {
                crossesNearPlane = true;
                continue;
            }
            float ndcX = Mth.m_14036_((float)(clip.x / clip.w), (float)-1.0f, (float)1.0f);
            float ndcY = Mth.m_14036_((float)(clip.y / clip.w), (float)-1.0f, (float)1.0f);
            minX = Math.min(minX, ndcX);
            maxX = Math.max(maxX, ndcX);
            minY = Math.min(minY, ndcY);
            maxY = Math.max(maxY, ndcY);
            projected = true;
        }
        AABB bounds = plane.getBounds();
        for (int xi = 0; xi < 2; ++xi) {
            for (int yi = 0; yi < 2; ++yi) {
                for (int zi = 0; zi < 2; ++zi) {
                    clip.set((float)(xi == 0 ? bounds.f_82288_ : bounds.f_82291_) - (float)cameraPos.f_82479_, (float)(yi == 0 ? bounds.f_82289_ : bounds.f_82292_) - (float)cameraPos.f_82480_, (float)(zi == 0 ? bounds.f_82290_ : bounds.f_82293_) - (float)cameraPos.f_82481_, 1.0f);
                    viewProjection.transform(clip);
                    if (clip.w <= 1.0E-4f) {
                        crossesNearPlane = true;
                        continue;
                    }
                    float ndcX = Mth.m_14036_((float)(clip.x / clip.w), (float)-1.0f, (float)1.0f);
                    float ndcY = Mth.m_14036_((float)(clip.y / clip.w), (float)-1.0f, (float)1.0f);
                    minX = Math.min(minX, ndcX);
                    maxX = Math.max(maxX, ndcX);
                    minY = Math.min(minY, ndcY);
                    maxY = Math.max(maxY, ndcY);
                    projected = true;
                }
            }
        }
        if (!projected || maxX <= minX || maxY <= minY) {
            return null;
        }
        if (crossesNearPlane) {
            return new CropRect(0, 0, targetWidth, targetHeight, targetWidth, targetHeight);
        }
        float marginX = Math.max(0.04f, (maxX - minX) * 0.12f);
        float marginY = Math.max(0.04f, (maxY - minY) * 0.12f);
        minX = Math.max(-1.0f, minX - marginX);
        maxX = Math.min(1.0f, maxX + marginX);
        minY = Math.max(-1.0f, minY - marginY);
        maxY = Math.min(1.0f, maxY + marginY);
        int x = Math.max(0, Mth.m_14143_((float)((minX * 0.5f + 0.5f) * (float)targetWidth)) - 8);
        int y = Math.max(0, Mth.m_14143_((float)((minY * 0.5f + 0.5f) * (float)targetHeight)) - 8);
        int right = Math.min(targetWidth, Mth.m_14167_((float)((maxX * 0.5f + 0.5f) * (float)targetWidth)) + 8);
        int top = Math.min(targetHeight, Mth.m_14167_((float)((maxY * 0.5f + 0.5f) * (float)targetHeight)) + 8);
        return right > x && top > y ? new CropRect(x, y, right - x, top - y, targetWidth, targetHeight) : null;
    }

    private static Matrix4f applyProjectionCrop(Matrix4f projection, CropRect crop) {
        float left = (float)crop.x / (float)crop.targetWidth * 2.0f - 1.0f;
        float right = (float)(crop.x + crop.width) / (float)crop.targetWidth * 2.0f - 1.0f;
        float bottom = (float)crop.y / (float)crop.targetHeight * 2.0f - 1.0f;
        float top = (float)(crop.y + crop.height) / (float)crop.targetHeight * 2.0f - 1.0f;
        float scaleX = 2.0f / (right - left);
        float scaleY = 2.0f / (top - bottom);
        float offsetX = -(right + left) / (right - left);
        float offsetY = -(top + bottom) / (top - bottom);
        Matrix4f cropMatrix = new Matrix4f().identity();
        cropMatrix.m00(scaleX);
        cropMatrix.m11(scaleY);
        cropMatrix.m30(offsetX);
        cropMatrix.m31(offsetY);
        return cropMatrix.mul((Matrix4fc)new Matrix4f((Matrix4fc)projection));
    }

    private boolean computeFeedMatrices(MirrorPlane plane, Vec3 viewerPos, Vec3 mainCameraPos, Matrix4f outMirrorLinear, Matrix4f outProj, Vec3[] outVirtCam) {
        double signedDist = viewerPos.m_82546_(plane.planePoint).m_82526_(plane.normal);
        if (signedDist < 0.02) {
            return false;
        }
        Vec3 virtualCamPos = ReflectionSystem.reflectPoint(viewerPos, plane.planePoint, plane.normal);
        Vector3f dir = new Vector3f((float)(-plane.normal.f_82479_), (float)(-plane.normal.f_82480_), (float)(-plane.normal.f_82481_));
        Vector3f up = plane.face.m_122434_() == Direction.Axis.Y ? new Vector3f(0.0f, 0.0f, 1.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Matrix4f lookRot = new Matrix4f().setLookAlong((Vector3fc)dir, (Vector3fc)up);
        outMirrorLinear.set((Matrix4fc)lookRot).mul((Matrix4fc)ReflectionSystem.axisReflectionMatrix(plane.face.m_122434_()));
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        Vector3f corner = new Vector3f();
        for (int k = 0; k < 4; ++k) {
            plane.getFeedRectCorner(k, mainCameraPos, 24.0f, corner);
            corner.sub((float)virtualCamPos.f_82479_, (float)virtualCamPos.f_82480_, (float)virtualCamPos.f_82481_);
            outMirrorLinear.transformPosition(corner);
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minY = Math.min(minY, corner.y);
            maxY = Math.max(maxY, corner.y);
        }
        float near = (float)signedDist;
        float margin = 0.03f * Math.max(maxX - minX, maxY - minY) + 0.05f;
        minY -= margin;
        maxY += margin;
        if ((maxX += margin) - (minX -= margin) < 1.0E-4f || maxY - minY < 1.0E-4f) {
            return false;
        }
        outProj.setFrustum(minX, maxX, minY, maxY, near, 1536.0f);
        outVirtCam[0] = virtualCamPos;
        return true;
    }

    public float sampleProbeGloss(BlockPos pos, Vec3 normal) {
        if (!this.enabled || this.probeCenter == null) {
            return 0.0f;
        }
        if (pos.m_123331_((Vec3i)this.probeCenter) > 1024.0) {
            return 0.0f;
        }
        Vec3 view = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Vec3 dir = new Vec3((double)pos.m_123341_() + 0.5 - view.f_82479_, (double)pos.m_123342_() + 0.5 - view.f_82480_, (double)pos.m_123343_() + 0.5 - view.f_82481_).m_82541_();
        double ndv = Math.max(0.0, normal.m_82541_().m_82526_(dir));
        return (float)(Math.pow(1.0 - ndv, 4.0) * 0.35);
    }

    public void renderGlossOverlay(Minecraft mc, PoseStack ps, MultiBufferSource buffers, Iterable<BlockPos> glossyBlocks) {
        if (!this.enabled || !this.initialized || this.probeRenderType == null) {
            return;
        }
        VertexConsumer vc = buffers.m_6299_(this.probeRenderType);
        Vec3 cam = mc.f_91063_.m_109153_().m_90583_();
        for (BlockPos bp : glossyBlocks) {
            float a = this.sampleProbeGloss(bp, new Vec3(0.0, 1.0, 0.0));
            if (a <= 0.01f) continue;
            float bx = (float)((double)bp.m_123341_() - cam.f_82479_);
            float by = (float)((double)bp.m_123342_() + 1.001 - cam.f_82480_);
            float bz = (float)((double)bp.m_123343_() - cam.f_82481_);
            ReflectionSystem.reflectVertex(ps, vc, bx, by, bz, 0.0f, 0.0f, a);
            ReflectionSystem.reflectVertex(ps, vc, bx + 1.0f, by, bz, 1.0f, 0.0f, a);
            ReflectionSystem.reflectVertex(ps, vc, bx + 1.0f, by, bz + 1.0f, 1.0f, 1.0f, a);
            ReflectionSystem.reflectVertex(ps, vc, bx, by, bz + 1.0f, 0.0f, 1.0f, a);
        }
    }

    private void ensureInit(Minecraft mc) {
        int targetWidth = ReflectionSystem.getReflectionTargetSize(mc.m_91268_().m_85441_());
        int targetHeight = ReflectionSystem.getReflectionTargetSize(mc.m_91268_().m_85442_());
        boolean bilinearFiltering = (Boolean)Config.CLIENT.reflectionBilinearFiltering.get();
        if (this.initialized) {
            for (PlaneRenderCache cache : this.planeRenderCaches) {
                boolean sizeChanged;
                boolean bl = sizeChanged = cache.targets[0].f_83915_ != targetWidth || cache.targets[0].f_83916_ != targetHeight;
                if (sizeChanged) {
                    cache.targets[0].m_83941_(targetWidth, targetHeight, Minecraft.f_91002_);
                    cache.targets[1].m_83941_(targetWidth, targetHeight, Minecraft.f_91002_);
                    cache.invalidate();
                    cache.textureRef.setGlId(cache.targets[0].m_83975_());
                }
                if (!sizeChanged && this.bilinearFilteringEnabled == bilinearFiltering) continue;
                ReflectionSystem.configureReflectionTexture(cache.targets[0], bilinearFiltering);
                ReflectionSystem.configureReflectionTexture(cache.targets[1], bilinearFiltering);
            }
            this.bilinearFilteringEnabled = bilinearFiltering;
            return;
        }
        this.initialized = true;
        this.bilinearFilteringEnabled = bilinearFiltering;
        for (int i = 0; i < 4; ++i) {
            RenderTarget[] targets = new RenderTarget[2];
            for (int d = 0; d < targets.length; ++d) {
                targets[d] = new RenderTarget(true){};
                targets[d].m_83950_(targetWidth, targetHeight, Minecraft.f_91002_);
                targets[d].m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
                targets[d].m_83954_(Minecraft.f_91002_);
                ReflectionSystem.configureReflectionTexture(targets[d], bilinearFiltering);
            }
            GlTextureRef textureRef = new GlTextureRef(targets[0].m_83975_());
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)("dynamic/planar_reflect_" + i));
            mc.m_91097_().m_118495_(textureLocation, (AbstractTexture)textureRef);
            this.planeRenderCaches.add(new PlaneRenderCache(targets, textureRef, ReflectionSystem.buildReflectType(textureLocation), ReflectionSystem.buildPolishedAndesiteReflectType(textureLocation), ReflectionSystem.buildMetalReflectType(textureLocation)));
        }
        this.probeTexture = new DynamicTexture(192, 128, true);
        this.probeLoc = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"dynamic/probe");
        mc.m_91097_().m_118495_(this.probeLoc, (AbstractTexture)this.probeTexture);
        this.probeRenderType = ReflectionSystem.buildReflectType(this.probeLoc);
    }

    private static int getReflectionTargetSize(int fullSize) {
        double scale = Mth.m_14008_((double)((Double)Config.CLIENT.reflectionRenderScale.get()), (double)0.1, (double)1.0);
        return Math.max(1, Mth.m_14167_((float)((float)((double)fullSize * scale))));
    }

    private static void configureReflectionTexture(RenderTarget target, boolean bilinearFiltering) {
        RenderSystem.bindTexture((int)target.m_83975_());
        int filter = bilinearFiltering ? 9729 : 9728;
        GlStateManager._texParameter((int)3553, (int)10241, (int)filter);
        GlStateManager._texParameter((int)3553, (int)10240, (int)filter);
        GlStateManager._texParameter((int)3553, (int)10242, (int)33071);
        GlStateManager._texParameter((int)3553, (int)10243, (int)33071);
        RenderSystem.bindTexture((int)0);
    }

    private void updateMirrorPlanes(Minecraft mc, Vec3 cameraPos) {
        boolean activeAreaMoved;
        long now = System.currentTimeMillis();
        long scanIntervalMs = ReflectionSystem.getPlaneScanUpdateMs();
        BlockPos base = BlockPos.m_274446_((Position)cameraPos);
        int configuredDistance = (Integer)Config.CLIENT.reflectionDistance.get();
        boolean bl = activeAreaMoved = base.m_123341_() != this.lastPlaneScanBlockX || base.m_123342_() != this.lastPlaneScanBlockY || base.m_123343_() != this.lastPlaneScanBlockZ || configuredDistance != this.lastPlaneScanDistance;
        if (!activeAreaMoved && now - this.lastPlaneScanMs < scanIntervalMs) {
            return;
        }
        this.lastPlaneScanMs = now;
        this.lastPlaneScanBlockX = base.m_123341_();
        this.lastPlaneScanBlockY = base.m_123342_();
        this.lastPlaneScanBlockZ = base.m_123343_();
        this.lastPlaneScanDistance = configuredDistance;
        HashMap<PlaneKey, MirrorPlane> planes = new HashMap<PlaneKey, MirrorPlane>();
        Map<Long, long[]> snapshot = TorchRtxState.get().getReflectiveSnapshotView();
        ExposedFaceService exposedFaceService = ExposedFaceService.get();
        RainPuddleManager rainPuddleManager = RainPuddleManager.get();
        if (snapshot.isEmpty()) {
            this.activePlanes.clear();
            return;
        }
        int centerChunkX = base.m_123341_() >> 4;
        int centerChunkZ = base.m_123343_() >> 4;
        double mirrorScanRadius = ReflectionSystem.getMirrorScanRadius();
        double faceInclusionRadius = mirrorScanRadius + 0.875;
        double faceInclusionRadiusSq = faceInclusionRadius * faceInclusionRadius;
        int chunkRadius = Math.max(1, (int)Math.ceil(mirrorScanRadius / 16.0)) + 1;
        for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                long[] bucket = snapshot.get(ChunkPos.m_45589_((int)(centerChunkX + dx), (int)(centerChunkZ + dz)));
                if (bucket == null) continue;
                for (long blockKey : bucket) {
                    byte exposedMask;
                    BlockPos pos = BlockPos.m_122022_((long)blockKey);
                    BlockState state = mc.f_91073_.m_8055_(pos);
                    ReflectionMaterial material = ReflectionMaterialRegistry.find(state);
                    if (material == null || (exposedMask = exposedFaceService.getExposedFaceMask(mc.f_91073_, blockKey)) == 0) continue;
                    for (Direction face : Direction.values()) {
                        if ((exposedMask & 1 << face.ordinal()) == 0 || ReflectionSystem.faceCenter(pos, face).m_82557_(cameraPos) > faceInclusionRadiusSq || !material.shouldReflectFace(mc, pos, state, face)) continue;
                        PlaneKey key = PlaneKey.of(pos, face);
                        MirrorPlane plane = planes.computeIfAbsent(key, k -> new MirrorPlane(face, ReflectionSystem.planePointForFace(pos, face)));
                        boolean usePolishedShader = state.m_60713_(Blocks.f_50387_);
                        boolean useMetalShader = state.m_60713_(Blocks.f_50075_);
                        RainPuddleSurface rainPuddleSurface = face == Direction.UP ? rainPuddleManager.getActiveSurface(mc.f_91073_, pos) : null;
                        plane.addFace(pos, face, material, usePolishedShader, useMetalShader, cameraPos, rainPuddleSurface);
                    }
                }
            }
        }
        this.activePlanes.clear();
        if (planes.isEmpty()) {
            return;
        }
        ArrayList<Object> sorted = new ArrayList(planes.values());
        sorted.sort((a, b) -> {
            int byFaceCount = Integer.compare(b.faces.size(), a.faces.size());
            if (byFaceCount != 0) {
                return byFaceCount;
            }
            int byDistance = Double.compare(a.minDistanceSq, b.minDistanceSq);
            if (byDistance != 0) {
                return byDistance;
            }
            int byFace = Integer.compare(a.face.m_122411_(), b.face.m_122411_());
            if (byFace != 0) {
                return byFace;
            }
            return Double.compare(ReflectionSystem.planeCoordinateValue(a), ReflectionSystem.planeCoordinateValue(b));
        });
        if (sorted.size() > 4) {
            sorted = new ArrayList(sorted.subList(0, 4));
        }
        sorted.sort((a, b) -> Long.compare(ReflectionSystem.computePlaneIdentity(a), ReflectionSystem.computePlaneIdentity(b)));
        this.activePlanes.addAll(sorted);
    }

    private static double getMirrorScanRadius() {
        return Mth.m_14045_((int)((Integer)Config.CLIENT.reflectionDistance.get()), (int)5, (int)200);
    }

    private static double computeScaledDistanceMul() {
        double distanceScale = 32.0 / ReflectionSystem.getMirrorScanRadius();
        return distanceScale * distanceScale;
    }

    private static long getPlaneScanUpdateMs() {
        int value = (Integer)Config.CLIENT.reflectionScanIntervalMs.get();
        if (value < 5) {
            value = 5;
        }
        if (value > 5000) {
            value = 5000;
        }
        return value;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderPlanePassEx(Minecraft mc, RenderTarget targetToBind, Vec3 virtualCamPos, Matrix4f mirrorLinear, Matrix4f virtualProj, Matrix4f cullProj, int viewportWidth, int viewportHeight) {
        PoseStack mirrorPose = new PoseStack();
        mirrorPose.m_85850_().m_252922_().set((Matrix4fc)mirrorLinear);
        mirrorPose.m_85850_().m_252943_().set((Matrix3fc)new Matrix3f((Matrix4fc)mirrorLinear));
        this.configureReflectionCamera(mc, virtualCamPos);
        ReflectionSystem.flushSharedRenderBuffers(mc);
        targetToBind.m_83947_(false);
        RenderSystem.viewport((int)0, (int)0, (int)viewportWidth, (int)viewportHeight);
        RenderSystem.enableScissor((int)0, (int)0, (int)viewportWidth, (int)viewportHeight);
        GL30.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL30.glClear((int)16640);
        MinecraftAccessor mcAccessor = (MinecraftAccessor)mc;
        RenderTarget oldMainTargetField = mcAccessor.luxium$getMainRenderTargetField();
        mcAccessor.luxium$setMainRenderTargetField(targetToBind);
        float savedFogStart = RenderSystem.getShaderFogStart();
        float savedFogEnd = RenderSystem.getShaderFogEnd();
        float[] savedFogColorArr = RenderSystem.getShaderFogColor();
        Vector4f savedFogColor = new Vector4f(savedFogColorArr[0], savedFogColorArr[1], savedFogColorArr[2], savedFogColorArr[3]);
        FogShape savedFogShape = RenderSystem.getShaderFogShape();
        float[] savedShaderColorArr = RenderSystem.getShaderColor();
        Vector4f savedShaderColor = new Vector4f(savedShaderColorArr[0], savedShaderColorArr[1], savedShaderColorArr[2], savedShaderColorArr[3]);
        int savedShaderTex0 = RenderSystem.getShaderTexture((int)0);
        int savedShaderTex1 = RenderSystem.getShaderTexture((int)1);
        int savedShaderTex2 = RenderSystem.getShaderTexture((int)2);
        Vec3 oldActiveCamera = this.activeRenderCameraPos;
        int oldRecursionDepth = this.recursionDepth;
        int oldViewportWidth = this.activePassViewportWidth;
        int oldViewportHeight = this.activePassViewportHeight;
        this.activeRenderCameraPos = virtualCamPos;
        this.recursionDepth = 1;
        this.activePassViewportWidth = viewportWidth;
        this.activePassViewportHeight = viewportHeight;
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)virtualProj, (VertexSorting)VertexSorting.f_276450_);
            RenderSystem.setInverseViewRotationMatrix((Matrix3f)new Matrix3f((Matrix3fc)mirrorPose.m_85850_().m_252943_()).invert());
            RenderSystem.enableCull();
            GL30.glCullFace((int)1028);
            mc.f_91060_.m_253210_(mirrorPose, virtualCamPos, new Matrix4f((Matrix4fc)cullProj));
            this.renderReflectionWorld(mc, mirrorPose, virtualProj);
            ReflectionSystem.flushSharedRenderBuffers(mc);
            ReflectionSystem.flushSharedRenderBuffers(mc);
            mc.m_91307_().m_7238_();
        }
        finally {
            RenderSystem.disableScissor();
            mcAccessor.luxium$setMainRenderTargetField(oldMainTargetField);
            GL30.glCullFace((int)1029);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthFunc((int)515);
            RenderSystem.setShaderFogStart((float)savedFogStart);
            RenderSystem.setShaderFogEnd((float)savedFogEnd);
            RenderSystem.setShaderFogColor((float)savedFogColor.x, (float)savedFogColor.y, (float)savedFogColor.z, (float)savedFogColor.w);
            RenderSystem.setShaderFogShape((FogShape)savedFogShape);
            RenderSystem.setShaderColor((float)savedShaderColor.x, (float)savedShaderColor.y, (float)savedShaderColor.z, (float)savedShaderColor.w);
            RenderSystem.setShaderTexture((int)0, (int)savedShaderTex0);
            RenderSystem.setShaderTexture((int)1, (int)savedShaderTex1);
            RenderSystem.setShaderTexture((int)2, (int)savedShaderTex2);
            RenderSystem.setProjectionMatrix((Matrix4f)new Matrix4f((Matrix4fc)this.frameProjectionMatrix), (VertexSorting)VertexSorting.f_276450_);
            RenderSystem.setInverseViewRotationMatrix((Matrix3f)new Matrix3f((Matrix4fc)this.frameViewMatrix).invert());
            PoseStack restored = new PoseStack();
            restored.m_85850_().m_252922_().set((Matrix4fc)this.frameViewMatrix);
            mc.f_91060_.m_253210_(restored, mc.f_91063_.m_109153_().m_90583_(), this.frameProjectionMatrix);
            this.recursionDepth = oldRecursionDepth;
            this.activeRenderCameraPos = oldActiveCamera;
            this.activePassViewportWidth = oldViewportWidth;
            this.activePassViewportHeight = oldViewportHeight;
        }
    }

    private void renderReflectionWorld(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix) {
        LevelRendererAccessor levelRenderer = (LevelRendererAccessor)mc.f_91060_;
        Frustum frustum = levelRenderer.luxium$getCullingFrustum();
        levelRenderer.luxium$setupRender(this.reflectionCamera, frustum, false, true);
        levelRenderer.luxium$compileChunks(this.reflectionCamera);
        Vec3 cameraPos = this.reflectionCamera.m_90583_();
        double cameraX = cameraPos.f_82479_;
        double cameraY = cameraPos.f_82480_;
        double cameraZ = cameraPos.f_82481_;
        float renderDistance = this.frameGameRenderer.m_109152_();
        boolean foggy = mc.f_91073_.m_104583_().m_5781_(Mth.m_14107_((double)cameraX), Mth.m_14107_((double)cameraZ)) || mc.f_91065_.m_93090_().m_93715_();
        FogRenderer.m_109018_((Camera)this.reflectionCamera, (float)this.framePartialTick, (ClientLevel)mc.f_91073_, (int)mc.f_91066_.m_193772_(), (float)this.frameGameRenderer.m_109131_(this.framePartialTick));
        FogRenderer.m_109036_();
        FogRenderer.m_234172_((Camera)this.reflectionCamera, (FogRenderer.FogMode)FogRenderer.FogMode.FOG_SKY, (float)renderDistance, (boolean)foggy, (float)this.framePartialTick);
        RenderSystem.setShader(GameRenderer::m_172808_);
        mc.f_91060_.m_202423_(poseStack, projectionMatrix, this.framePartialTick, this.reflectionCamera, foggy, () -> FogRenderer.m_234172_((Camera)this.reflectionCamera, (FogRenderer.FogMode)FogRenderer.FogMode.FOG_SKY, (float)renderDistance, (boolean)foggy, (float)this.framePartialTick));
        FogRenderer.m_234172_((Camera)this.reflectionCamera, (FogRenderer.FogMode)FogRenderer.FogMode.FOG_TERRAIN, (float)Math.max(renderDistance, 32.0f), (boolean)foggy, (float)this.framePartialTick);
        levelRenderer.luxium$renderChunkLayer(RenderType.m_110451_(), poseStack, cameraX, cameraY, cameraZ, projectionMatrix);
        levelRenderer.luxium$renderChunkLayer(RenderType.m_110457_(), poseStack, cameraX, cameraY, cameraZ, projectionMatrix);
        levelRenderer.luxium$renderChunkLayer(RenderType.m_110463_(), poseStack, cameraX, cameraY, cameraZ, projectionMatrix);
        mc.m_91290_().m_114408_((Level)mc.f_91073_, this.reflectionCamera, mc.f_91076_);
        MultiBufferSource.BufferSource buffers = mc.m_91269_().m_110104_();
        for (Entity entity : mc.f_91073_.m_104735_()) {
            if (!mc.m_91290_().m_114397_(entity, frustum, cameraX, cameraY, cameraZ) || entity == this.reflectionCamera.m_90592_() && !this.reflectionCamera.m_90594_()) continue;
            double x = Mth.m_14139_((double)this.framePartialTick, (double)entity.f_19790_, (double)entity.m_20185_()) - cameraX;
            double y = Mth.m_14139_((double)this.framePartialTick, (double)entity.f_19791_, (double)entity.m_20186_()) - cameraY;
            double z = Mth.m_14139_((double)this.framePartialTick, (double)entity.f_19792_, (double)entity.m_20189_()) - cameraZ;
            float yaw = Mth.m_14179_((float)this.framePartialTick, (float)entity.f_19859_, (float)entity.m_146908_());
            mc.m_91290_().m_114384_(entity, x, y, z, yaw, this.framePartialTick, poseStack, (MultiBufferSource)buffers, mc.m_91290_().m_114394_(entity, this.framePartialTick));
        }
        buffers.m_109911_();
        levelRenderer.luxium$renderChunkLayer(RenderType.m_110466_(), poseStack, cameraX, cameraY, cameraZ, projectionMatrix);
        levelRenderer.luxium$renderChunkLayer(RenderType.m_110503_(), poseStack, cameraX, cameraY, cameraZ, projectionMatrix);
        mc.f_91061_.render(poseStack, buffers, this.frameLightTexture, this.reflectionCamera, this.framePartialTick, frustum);
        buffers.m_109911_();
        this.renderPlanar(mc, poseStack, (MultiBufferSource)buffers);
        if (mc.f_91066_.m_92174_() != CloudStatus.OFF) {
            mc.f_91060_.m_253054_(poseStack, projectionMatrix, this.framePartialTick, cameraX, cameraY, cameraZ);
        }
    }

    private static void flushSharedRenderBuffers(Minecraft mc) {
        mc.m_91269_().m_110104_().m_109911_();
        mc.m_91269_().m_110108_().m_109911_();
        mc.m_91269_().m_110109_().m_109928_();
    }

    private void configureReflectionCamera(Minecraft mc, Vec3 reflectedCamPos) {
        Camera mainCamera = mc.f_91063_.m_109153_();
        this.reflectionCamera.m_90575_((BlockGetter)mc.f_91073_, (Entity)(mc.f_91075_ == null ? mc.f_91074_ : mc.f_91075_), !mc.f_91066_.m_92176_().m_90612_(), mc.f_91066_.m_92176_().m_90613_(), this.framePartialTick);
        CameraAccessor accessor = (CameraAccessor)this.reflectionCamera;
        accessor.luxium$setPosition(reflectedCamPos.f_82479_, reflectedCamPos.f_82480_, reflectedCamPos.f_82481_);
        accessor.luxium$setRotation(mainCamera.m_90590_(), mainCamera.m_90589_());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawPlane(PoseStack mainPoseStack, MirrorPlane plane, PlaneRenderCache cache, Matrix4f mirrorLinear, Matrix4f virtualProj, Vec3 virtualCamPos, Vec3 cameraPos) {
        CachedPlaneMesh mesh = cache.mesh;
        if (mesh == null) {
            return;
        }
        Vec3 origin = plane.meshOrigin;
        Matrix4f modelView = new Matrix4f((Matrix4fc)mainPoseStack.m_85850_().m_252922_()).translate((float)(origin.f_82479_ - cameraPos.f_82479_), (float)(origin.f_82480_ - cameraPos.f_82480_), (float)(origin.f_82481_ - cameraPos.f_82481_));
        Matrix4f virtualVP = new Matrix4f((Matrix4fc)virtualProj).mul((Matrix4fc)mirrorLinear).translate((float)(origin.f_82479_ - virtualCamPos.f_82479_), (float)(origin.f_82480_ - virtualCamPos.f_82480_), (float)(origin.f_82481_ - virtualCamPos.f_82481_));
        Vec3 playerCameraPos = new Vec3(this.lastCamX, this.lastCamY, this.lastCamZ);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        GlStateManager._depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture((int)0, (int)cache.textureRef.m_117963_());
        RenderSystem.polygonOffset((float)-1.0f, (float)-1.0f);
        RenderSystem.enablePolygonOffset();
        try {
            this.drawCachedMesh(mesh.defaultBuffer, ShaderManager.getMetalReflectionShader(), modelView, this.cachedOriginalProjection, virtualVP, plane, cache, cameraPos, playerCameraPos);
            this.drawCachedMesh(mesh.metalBuffer, ShaderManager.getMetalReflectionShader(), modelView, this.cachedOriginalProjection, virtualVP, plane, cache, cameraPos, playerCameraPos);
            this.drawCachedMesh(mesh.polishedBuffer, ShaderManager.getPolishedAndesiteShader(), modelView, this.cachedOriginalProjection, virtualVP, plane, cache, cameraPos, playerCameraPos);
        }
        finally {
            VertexBuffer.m_85931_();
            RenderSystem.polygonOffset((float)0.0f, (float)0.0f);
            RenderSystem.disablePolygonOffset();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void drawCachedMesh(@Nullable VertexBuffer buffer, @Nullable ShaderInstance shader, Matrix4f modelView, Matrix4f projection, Matrix4f virtualVP, MirrorPlane plane, PlaneRenderCache cache, Vec3 cameraPos, Vec3 playerCameraPos) {
        if (buffer == null || shader == null || buffer.m_231230_()) {
            return;
        }
        ReflectionSystem.setMatrixUniform(shader, "VirtualVPMat", virtualVP);
        ReflectionSystem.setVector3Uniform(shader, "CameraOffset", (float)(plane.meshOrigin.f_82479_ - cameraPos.f_82479_), (float)(plane.meshOrigin.f_82480_ - cameraPos.f_82480_), (float)(plane.meshOrigin.f_82481_ - cameraPos.f_82481_));
        ReflectionSystem.setVector3Uniform(shader, "PlayerOffset", (float)(plane.meshOrigin.f_82479_ - playerCameraPos.f_82479_), (float)(plane.meshOrigin.f_82480_ - playerCameraPos.f_82480_), (float)(plane.meshOrigin.f_82481_ - playerCameraPos.f_82481_));
        ReflectionSystem.setVector3Uniform(shader, "FaceNormal", (float)plane.normal.f_82479_, (float)plane.normal.f_82480_, (float)plane.normal.f_82481_);
        ReflectionSystem.setFloatUniform(shader, "DistanceScale", (float)Math.sqrt(ReflectionSystem.computeScaledDistanceMul()));
        ReflectionSystem.setFloatUniform(shader, "ReflectionDistance", (float)ReflectionSystem.getMirrorScanRadius());
        ReflectionSystem.setFloatUniform(shader, "NestedPass", this.recursionDepth > 0 ? 1.0f : 0.0f);
        ReflectionSystem.setVector2Uniform(shader, "TextureUvScale", cache.activeUvScaleX, cache.activeUvScaleY);
        buffer.m_85921_();
        buffer.m_253207_(modelView, projection, shader);
    }

    private static void setMatrixUniform(ShaderInstance shader, String name, Matrix4f value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5679_(value);
        }
    }

    private static void setVector3Uniform(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5889_(x, y, z);
        }
    }

    private static void setVector2Uniform(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }

    private static void setFloatUniform(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static long computePlaneSignature(MirrorPlane plane) {
        if (plane.cachedSignature != Long.MIN_VALUE) {
            return plane.cachedSignature;
        }
        long h = 31L * (long)plane.face.m_122411_() + Double.doubleToLongBits(ReflectionSystem.planeCoordinateValue(plane));
        for (MirrorFace face : plane.faces) {
            h = h * 31L + face.blockPos.m_121878_();
            h = h * 31L + (long)face.material.getClass().getName().hashCode();
            for (int i = 0; i < 4; ++i) {
                h = h * 31L + (long)Float.floatToIntBits(face.warpedU[i]);
                h = h * 31L + (long)Float.floatToIntBits(face.warpedV[i]);
            }
            if (face.rainPuddleSurface == null) continue;
            h = h * 31L + (long)Float.floatToIntBits(face.rainPuddleSurface.sample(face.blockPos, 0.0f, 0.0f));
            h = h * 31L + (long)Float.floatToIntBits(face.rainPuddleSurface.sample(face.blockPos, 1.0f, 0.0f));
            h = h * 31L + (long)Float.floatToIntBits(face.rainPuddleSurface.sample(face.blockPos, 1.0f, 1.0f));
            h = h * 31L + (long)Float.floatToIntBits(face.rainPuddleSurface.sample(face.blockPos, 0.0f, 1.0f));
        }
        plane.cachedSignature = h;
        return plane.cachedSignature;
    }

    private static long computePlaneIdentity(MirrorPlane plane) {
        long coordinate = Double.doubleToLongBits(ReflectionSystem.planeCoordinateValue(plane));
        return coordinate ^ (long)plane.face.m_122411_() << 56;
    }

    private static double planeCoordinateValue(MirrorPlane plane) {
        return switch (plane.face.m_122434_()) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.Axis.X -> plane.planePoint.f_82479_;
            case Direction.Axis.Y -> plane.planePoint.f_82480_;
            case Direction.Axis.Z -> plane.planePoint.f_82481_;
        };
    }

    private static float sampleMaskedAlpha(float baseAlpha, @Nullable RainPuddleSurface rainPuddleSurface, BlockPos pos, float faceU, float faceV) {
        if (rainPuddleSurface == null) {
            return baseAlpha;
        }
        return baseAlpha * rainPuddleSurface.sample(pos, faceU, faceV);
    }

    private static double facePointX(BlockPos pos, Direction face, float s) {
        double minX = pos.m_123341_();
        double maxX = minX + 1.0;
        double e = 0.0015f;
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP, Direction.DOWN, Direction.SOUTH -> minX + (double)s;
            case Direction.NORTH -> maxX - (double)s;
            case Direction.WEST -> minX - e;
            case Direction.EAST -> maxX + e;
        };
    }

    private static double facePointY(BlockPos pos, Direction face, float t) {
        double minY = pos.m_123342_();
        double maxY = minY + 1.0;
        double e = 0.0015f;
        return switch (face) {
            case Direction.UP -> maxY + e;
            case Direction.DOWN -> minY - e;
            default -> minY + (double)t;
        };
    }

    private static double facePointZ(BlockPos pos, Direction face, float s, float t) {
        double minZ = pos.m_123343_();
        double maxZ = minZ + 1.0;
        double e = 0.0015f;
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> minZ + (double)t;
            case Direction.DOWN -> maxZ - (double)t;
            case Direction.NORTH -> minZ - e;
            case Direction.SOUTH -> maxZ + e;
            case Direction.WEST -> maxZ - (double)s;
            case Direction.EAST -> minZ + (double)s;
        };
    }

    private static Vector4f worldPlaneToViewSpace(Matrix4f mirrorLinear, Vec3 virtualCamPos, Vec3 planeNormal, Vec3 planePoint) {
        Vector3f nv = new Vector3f((float)planeNormal.f_82479_, (float)planeNormal.f_82480_, (float)planeNormal.f_82481_);
        mirrorLinear.transformDirection(nv);
        float dx = (float)(planePoint.f_82479_ - virtualCamPos.f_82479_);
        float dy = (float)(planePoint.f_82480_ - virtualCamPos.f_82480_);
        float dz = (float)(planePoint.f_82481_ - virtualCamPos.f_82481_);
        Vector3f vp = new Vector3f(dx, dy, dz);
        mirrorLinear.transformPosition(vp);
        float d = -(nv.x * vp.x + nv.y * vp.y + nv.z * vp.z);
        return new Vector4f(nv.x, nv.y, nv.z, d);
    }

    private static Matrix4f applyObliqueNearClip(Matrix4f proj, Vector4f c) {
        float qw;
        float qz;
        float qy;
        float qx = (Math.signum(c.x) + proj.m20()) / proj.m00();
        float dot = c.x * qx + c.y * (qy = (Math.signum(c.y) + proj.m21()) / proj.m11()) + c.z * (qz = -1.0f) + c.w * (qw = (1.0f + proj.m22()) / proj.m32());
        if (Math.abs(dot) < 1.0E-4f) {
            return proj;
        }
        float d = 2.0f / dot;
        Matrix4f result = new Matrix4f((Matrix4fc)proj);
        result.m02(c.x * d);
        result.m12(c.y * d);
        result.m22(c.z * d + 1.0f);
        result.m32(c.w * d);
        if (!(Float.isFinite(result.m02()) && Float.isFinite(result.m12()) && Float.isFinite(result.m22()) && Float.isFinite(result.m32()) && !(Math.abs(result.m22()) > 50.0f))) {
            return proj;
        }
        return result;
    }

    private static Matrix4f axisReflectionMatrix(Direction.Axis axis) {
        return switch (axis) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.Axis.X -> new Matrix4f().scaling(-1.0f, 1.0f, 1.0f);
            case Direction.Axis.Y -> new Matrix4f().scaling(1.0f, -1.0f, 1.0f);
            case Direction.Axis.Z -> new Matrix4f().scaling(1.0f, 1.0f, -1.0f);
        };
    }

    private static Vec3 reflectPoint(Vec3 point, Vec3 planePoint, Vec3 planeNormal) {
        double dist = point.m_82546_(planePoint).m_82526_(planeNormal);
        return point.m_82546_(planeNormal.m_82490_(dist * 2.0));
    }

    private static Vec3 planePointForFace(BlockPos pos, Direction face) {
        return switch (face) {
            default -> throw new IncompatibleClassChangeError();
            case Direction.UP -> new Vec3((double)pos.m_123341_(), (double)pos.m_123342_() + 1.0, (double)pos.m_123343_());
            case Direction.DOWN -> new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
            case Direction.NORTH -> new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
            case Direction.SOUTH -> new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_() + 1.0);
            case Direction.WEST -> new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
            case Direction.EAST -> new Vec3((double)pos.m_123341_() + 1.0, (double)pos.m_123342_(), (double)pos.m_123343_());
        };
    }

    private static Vec3 faceCenter(BlockPos pos, Direction face) {
        return Vec3.m_82528_((Vec3i)pos).m_82520_(0.5 + (double)face.m_122429_() * 0.5, 0.5 + (double)face.m_122430_() * 0.5, 0.5 + (double)face.m_122431_() * 0.5);
    }

    private void fillProbeCross(Minecraft mc) {
        NativeImage img = this.probeTexture.m_117991_();
        if (img == null) {
            return;
        }
        Vec3 skyVec = mc.f_91073_.m_171660_(mc.f_91063_.m_109153_().m_90583_(), 1.0f);
        float skyR = (float)skyVec.f_82479_;
        float skyG = (float)skyVec.f_82480_;
        float skyB = (float)skyVec.f_82481_;
        float fogR = skyR * 0.78f;
        float fogG = skyG * 0.8f;
        float fogB = Math.min(1.0f, skyB * 0.88f);
        float gndR = 0.17f;
        float gndG = 0.13f;
        float gndB = 0.07f;
        float[][] faces = new float[][]{{0.0f, 0.0f, fogR, fogG, fogB}, {1.0f, 0.0f, skyR, skyG, skyB}, {2.0f, 0.0f, fogR, fogG, fogB}, {0.0f, 1.0f, fogR, fogG, fogB}, {1.0f, 1.0f, gndR, gndG, gndB}, {2.0f, 1.0f, fogR, fogG, fogB}};
        int f = 64;
        for (float[] face : faces) {
            int col = (int)face[0];
            int row = (int)face[1];
            float fr = face[2];
            float fg = face[3];
            float fb = face[4];
            for (int py = 0; py < f; ++py) {
                for (int px = 0; px < f; ++px) {
                    float dx = ((float)px - (float)f * 0.5f) / (float)f;
                    float dy = ((float)py - (float)f * 0.5f) / (float)f;
                    float vig = Math.max(0.75f, 1.0f - 0.9f * (dx * dx + dy * dy) * 4.0f);
                    img.m_84988_(col * f + px, row * f + py, ReflectionSystem.packARGB(ReflectionSystem.clamp(fr * vig), ReflectionSystem.clamp(fg * vig), ReflectionSystem.clamp(fb * vig), 220));
                }
            }
        }
        ReflectionSystem.boxBlur(img, 3);
        this.probeTexture.m_117985_();
    }

    private static RenderType buildReflectType(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.m_110628_().m_173292_(new RenderStateShard.ShaderStateShard(ShaderManager::getMetalReflectionShader)).m_173290_((RenderStateShard.EmptyTextureStateShard)new RenderStateShard.TextureStateShard(texture, false, false)).m_110685_(RenderHelper.TRANSLUCENT).m_110663_(RenderHelper.LEQUAL).m_110661_(RenderHelper.NO_CULL).m_110671_(RenderHelper.LIGHTMAP_ON).m_110687_(RenderHelper.COLOR_WRITE).m_110691_(false);
        return RenderType.m_173215_((String)"luxium_reflect", (VertexFormat)DefaultVertexFormat.f_85820_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)256, (boolean)false, (boolean)true, (RenderType.CompositeState)state);
    }

    private static RenderType buildPolishedAndesiteReflectType(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.m_110628_().m_173292_(new RenderStateShard.ShaderStateShard(ShaderManager::getPolishedAndesiteShader)).m_173290_((RenderStateShard.EmptyTextureStateShard)new RenderStateShard.TextureStateShard(texture, false, false)).m_110685_(RenderHelper.TRANSLUCENT).m_110663_(RenderHelper.LEQUAL).m_110661_(RenderHelper.NO_CULL).m_110671_(RenderHelper.LIGHTMAP_ON).m_110687_(RenderHelper.COLOR_WRITE).m_110691_(false);
        return RenderType.m_173215_((String)"luxium_reflect_polished_andesite", (VertexFormat)DefaultVertexFormat.f_85820_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)256, (boolean)false, (boolean)true, (RenderType.CompositeState)state);
    }

    private static RenderType buildMetalReflectType(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.m_110628_().m_173292_(new RenderStateShard.ShaderStateShard(ShaderManager::getMetalReflectionShader)).m_173290_((RenderStateShard.EmptyTextureStateShard)new RenderStateShard.TextureStateShard(texture, false, false)).m_110685_(RenderHelper.TRANSLUCENT).m_110663_(RenderHelper.LEQUAL).m_110661_(RenderHelper.NO_CULL).m_110671_(RenderHelper.LIGHTMAP_ON).m_110687_(RenderHelper.COLOR_WRITE).m_110691_(false);
        return RenderType.m_173215_((String)"luxium_reflect_metal", (VertexFormat)DefaultVertexFormat.f_85820_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)256, (boolean)false, (boolean)true, (RenderType.CompositeState)state);
    }

    private static void reflectVertex(PoseStack ps, VertexConsumer vc, float x, float y, float z, float u, float v, float alpha) {
        vc.m_252986_(ps.m_85850_().m_252922_(), x, y, z).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_7421_(u, v).m_85969_(0xF000F0).m_5752_();
    }

    private static int packARGB(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int)(v * 255.0f)));
    }

    private static void boxBlur(NativeImage img, int radius) {
        int px;
        long n;
        long aa;
        long ba;
        long ga;
        long ra;
        int x;
        int y;
        int w = img.m_84982_();
        int h = img.m_85084_();
        int[] src = new int[w * h];
        for (int y2 = 0; y2 < h; ++y2) {
            for (int x2 = 0; x2 < w; ++x2) {
                src[y2 * w + x2] = img.m_84985_(x2, y2);
            }
        }
        int[] horizontal = new int[w * h];
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                ra = 0L;
                ga = 0L;
                ba = 0L;
                aa = 0L;
                n = 0L;
                for (int dx = -radius; dx <= radius; ++dx) {
                    int xx = x + dx;
                    if (xx < 0 || xx >= w) continue;
                    px = src[y * w + xx];
                    aa += (long)(px >>> 24 & 0xFF);
                    ra += (long)(px >>> 16 & 0xFF);
                    ga += (long)(px >>> 8 & 0xFF);
                    ba += (long)(px & 0xFF);
                    ++n;
                }
                horizontal[y * w + x] = (int)(aa / n) << 24 | (int)(ra / n) << 16 | (int)(ga / n) << 8 | (int)(ba / n);
            }
        }
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                ra = 0L;
                ga = 0L;
                ba = 0L;
                aa = 0L;
                n = 0L;
                for (int dy = -radius; dy <= radius; ++dy) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) continue;
                    px = horizontal[yy * w + x];
                    aa += (long)(px >>> 24 & 0xFF);
                    ra += (long)(px >>> 16 & 0xFF);
                    ga += (long)(px >>> 8 & 0xFF);
                    ba += (long)(px & 0xFF);
                    ++n;
                }
                img.m_84988_(x, y, (int)(aa / n) << 24 | (int)(ra / n) << 16 | (int)(ga / n) << 8 | (int)(ba / n));
            }
        }
    }

    private static final class PlaneRenderCache {
        final RenderTarget[] targets;
        final GlTextureRef textureRef;
        final RenderType reflectRenderType;
        final RenderType polishedAndesiteReflectRenderType;
        final RenderType metalReflectRenderType;
        final Matrix4f exactMirrorLinear = new Matrix4f();
        final Matrix4f exactVirtualProj = new Matrix4f();
        @Nullable
        Vec3 exactVirtualCamPos;
        boolean exactValid = false;
        final Matrix4f feedMirrorLinear = new Matrix4f();
        final Matrix4f feedVirtualProj = new Matrix4f();
        @Nullable
        Vec3 feedVirtualCamPos;
        boolean feedValid = false;
        long feedConsumerIdentity = Long.MIN_VALUE;
        float exactUvScaleX = 1.0f;
        float exactUvScaleY = 1.0f;
        float feedUvScaleX = 1.0f;
        float feedUvScaleY = 1.0f;
        float activeUvScaleX = 1.0f;
        float activeUvScaleY = 1.0f;
        @Nullable
        CachedPlaneMesh mesh;
        long meshSignature = Long.MIN_VALUE;
        long lastPlaneIdentity = Long.MIN_VALUE;

        PlaneRenderCache(RenderTarget[] targets, GlTextureRef textureRef, RenderType reflectRenderType, RenderType polishedAndesiteReflectRenderType, RenderType metalReflectRenderType) {
            this.targets = targets;
            this.textureRef = textureRef;
            this.reflectRenderType = reflectRenderType;
            this.polishedAndesiteReflectRenderType = polishedAndesiteReflectRenderType;
            this.metalReflectRenderType = metalReflectRenderType;
        }

        void invalidate() {
            this.exactValid = false;
            this.exactVirtualCamPos = null;
            this.feedValid = false;
            this.feedVirtualCamPos = null;
        }

        void ensureMesh(MirrorPlane plane, long signature) {
            if (this.mesh != null && this.meshSignature == signature) {
                return;
            }
            if (this.mesh != null) {
                this.mesh.close();
            }
            this.mesh = new CachedPlaneMesh(plane);
            this.meshSignature = signature;
        }

        void closeMesh() {
            if (this.mesh != null) {
                this.mesh.close();
                this.mesh = null;
            }
            this.meshSignature = Long.MIN_VALUE;
        }
    }

    private static final class MirrorPlane {
        final Direction face;
        final Vec3 normal;
        final Vec3 planePoint;
        final List<MirrorFace> faces = new ArrayList<MirrorFace>();
        final Vec3 meshOrigin;
        double minDistanceSq = Double.MAX_VALUE;
        int minAlongU = Integer.MAX_VALUE;
        int minAlongV = Integer.MAX_VALUE;
        int maxAlongU = Integer.MIN_VALUE;
        int maxAlongV = Integer.MIN_VALUE;
        long cachedSignature = Long.MIN_VALUE;

        MirrorPlane(Direction face, Vec3 planePoint) {
            this.face = face;
            this.normal = Vec3.m_82528_((Vec3i)face.m_122436_());
            this.planePoint = planePoint;
            this.meshOrigin = planePoint;
        }

        void addFace(BlockPos pos, Direction face, ReflectionMaterial material, boolean usePolishedShader, boolean useMetalShader, Vec3 cameraPos, @Nullable RainPuddleSurface rainPuddleSurface) {
            this.faces.add(new MirrorFace(pos, face, material, usePolishedShader, useMetalShader, rainPuddleSurface, cameraPos));
            this.minDistanceSq = Math.min(this.minDistanceSq, ReflectionSystem.faceCenter(pos, face).m_82557_(cameraPos));
            int alongU = this.coordinateAlongU(pos);
            int alongV = this.coordinateAlongV(pos);
            this.minAlongU = Math.min(this.minAlongU, alongU);
            this.minAlongV = Math.min(this.minAlongV, alongV);
            this.maxAlongU = Math.max(this.maxAlongU, alongU + 1);
            this.maxAlongV = Math.max(this.maxAlongV, alongV + 1);
        }

        boolean isFacingCamera(Vec3 cameraPos) {
            return cameraPos.m_82546_(this.planePoint).m_82526_(this.normal) > 0.05;
        }

        AABB getBounds() {
            double minU = this.minAlongU;
            double maxU = this.maxAlongU;
            double minV = this.minAlongV;
            double maxV = this.maxAlongV;
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> new AABB(minU, this.planePoint.f_82480_ - 0.1, minV, maxU, this.planePoint.f_82480_ + 0.1, maxV);
                case Direction.SOUTH, Direction.NORTH -> new AABB(minU, minV, this.planePoint.f_82481_ - 0.1, maxU, maxV, this.planePoint.f_82481_ + 0.1);
                case Direction.WEST, Direction.EAST -> new AABB(this.planePoint.f_82479_ - 0.1, minV, minU, this.planePoint.f_82479_ + 0.1, maxV, maxU);
            };
        }

        AABB getCullingBounds() {
            return this.getBounds().m_82400_(0.5);
        }

        void getRectCorner(int k, Vector3f out) {
            float u = k == 0 || k == 3 ? (float)this.minAlongU : (float)this.maxAlongU;
            float v = k <= 1 ? (float)this.minAlongV : (float)this.maxAlongV;
            switch (this.face) {
                case UP: 
                case DOWN: {
                    out.set(u, (float)this.planePoint.f_82480_, v);
                    break;
                }
                case SOUTH: 
                case NORTH: {
                    out.set(u, v, (float)this.planePoint.f_82481_);
                    break;
                }
                case WEST: 
                case EAST: {
                    out.set((float)this.planePoint.f_82479_, v, u);
                }
            }
        }

        void getFeedRectCorner(int k, Vec3 cameraPos, float halfExtent, Vector3f out) {
            float signU = k == 0 || k == 3 ? -1.0f : 1.0f;
            float signV = k <= 1 ? -1.0f : 1.0f;
            switch (this.face) {
                case UP: 
                case DOWN: {
                    float centerU = (float)Mth.m_14008_((double)cameraPos.f_82479_, (double)this.minAlongU, (double)this.maxAlongU);
                    float centerV = (float)Mth.m_14008_((double)cameraPos.f_82481_, (double)this.minAlongV, (double)this.maxAlongV);
                    out.set(centerU + signU * halfExtent, (float)this.planePoint.f_82480_, centerV + signV * halfExtent);
                    break;
                }
                case SOUTH: 
                case NORTH: {
                    float centerU = (float)Mth.m_14008_((double)cameraPos.f_82479_, (double)this.minAlongU, (double)this.maxAlongU);
                    float centerV = (float)Mth.m_14008_((double)cameraPos.f_82480_, (double)this.minAlongV, (double)this.maxAlongV);
                    out.set(centerU + signU * halfExtent, centerV + signV * halfExtent, (float)this.planePoint.f_82481_);
                    break;
                }
                case WEST: 
                case EAST: {
                    float centerU = (float)Mth.m_14008_((double)cameraPos.f_82481_, (double)this.minAlongU, (double)this.maxAlongU);
                    float centerV = (float)Mth.m_14008_((double)cameraPos.f_82480_, (double)this.minAlongV, (double)this.maxAlongV);
                    out.set((float)this.planePoint.f_82479_, centerV + signV * halfExtent, centerU + signU * halfExtent);
                }
            }
        }

        int coordinateAlongU(BlockPos pos) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN, Direction.SOUTH -> pos.m_123341_();
                case Direction.NORTH -> pos.m_123341_();
                case Direction.WEST, Direction.EAST -> pos.m_123343_();
            };
        }

        int coordinateAlongV(BlockPos pos) {
            return switch (this.face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.UP, Direction.DOWN -> pos.m_123343_();
                case Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST -> pos.m_123342_();
            };
        }
    }

    static final class GlTextureRef
    extends AbstractTexture {
        GlTextureRef(int glId) {
            this.f_117950_ = glId;
        }

        void setGlId(int glId) {
            this.f_117950_ = glId;
        }

        public void m_6704_(ResourceManager manager) throws IOException {
        }
    }

    private record CropRect(int x, int y, int width, int height, int targetWidth, int targetHeight) {
    }

    private static final class PlaneKey {
        final Direction face;
        final int coordinate;

        private PlaneKey(Direction face, int coordinate) {
            this.face = face;
            this.coordinate = coordinate;
        }

        static PlaneKey of(BlockPos pos, Direction face) {
            int coord = switch (face) {
                default -> throw new IncompatibleClassChangeError();
                case Direction.EAST -> pos.m_123341_() + 1;
                case Direction.WEST -> pos.m_123341_();
                case Direction.UP -> pos.m_123342_() + 1;
                case Direction.DOWN -> pos.m_123342_();
                case Direction.SOUTH -> pos.m_123343_() + 1;
                case Direction.NORTH -> pos.m_123343_();
            };
            return new PlaneKey(face, coord);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PlaneKey)) {
                return false;
            }
            PlaneKey k = (PlaneKey)o;
            return this.face == k.face && this.coordinate == k.coordinate;
        }

        public int hashCode() {
            return 31 * this.face.hashCode() + this.coordinate;
        }
    }

    private static final class CachedPlaneMesh
    implements AutoCloseable {
        @Nullable
        final VertexBuffer defaultBuffer;
        @Nullable
        final VertexBuffer polishedBuffer;
        @Nullable
        final VertexBuffer metalBuffer;

        CachedPlaneMesh(MirrorPlane plane) {
            this.defaultBuffer = CachedPlaneMesh.buildBuffer(plane, 0);
            this.polishedBuffer = CachedPlaneMesh.buildBuffer(plane, 1);
            this.metalBuffer = CachedPlaneMesh.buildBuffer(plane, 2);
        }

        @Nullable
        private static VertexBuffer buildBuffer(MirrorPlane plane, int category) {
            int count = 0;
            for (MirrorFace mirrorFace : plane.faces) {
                if (CachedPlaneMesh.categoryOf(mirrorFace) != category) continue;
                ++count;
            }
            if (count == 0) {
                return null;
            }
            BufferBuilder builder = new BufferBuilder(Math.max(256, count * 128));
            builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85820_);
            for (MirrorFace face : plane.faces) {
                if (CachedPlaneMesh.categoryOf(face) != category) continue;
                CachedPlaneMesh.emitFace(builder, plane, face);
            }
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vertexBuffer.m_85921_();
            vertexBuffer.m_231221_(builder.m_231175_());
            VertexBuffer.m_85931_();
            return vertexBuffer;
        }

        private static void emitFace(BufferBuilder builder, MirrorPlane plane, MirrorFace face) {
            for (int vertex = 0; vertex < 4; ++vertex) {
                float u = vertex == 0 || vertex == 3 ? 0.0f : 1.0f;
                float v = vertex <= 1 ? 0.0f : 1.0f;
                float mask = ReflectionSystem.sampleMaskedAlpha(1.0f, face.rainPuddleSurface, face.blockPos, u, v);
                CachedPlaneMesh.emitMeshVertex(builder, plane, face, ReflectionSystem.facePointX(face.blockPos, face.face, u), ReflectionSystem.facePointY(face.blockPos, face.face, v), ReflectionSystem.facePointZ(face.blockPos, face.face, u, v), face.warpedU[vertex], face.warpedV[vertex], mask);
            }
        }

        private static void emitMeshVertex(BufferBuilder builder, MirrorPlane plane, MirrorFace face, double x, double y, double z, float u, float v, float mask) {
            Vec3 origin = plane.meshOrigin;
            builder.m_5483_(x - origin.f_82479_, y - origin.f_82480_, z - origin.f_82481_).m_85950_(face.material.getBaseAlpha(), face.material.getMaxDistance() / 64.0f, mask, CachedPlaneMesh.categoryOf(face) == 0 ? 0.5f : 1.0f).m_7421_(u, v).m_85969_(0xF000F0).m_5752_();
        }

        private static int categoryOf(MirrorFace face) {
            if (face.usePolishedShader) {
                return 1;
            }
            if (face.useMetalShader) {
                return 2;
            }
            return 0;
        }

        @Override
        public void close() {
            CachedPlaneMesh.closeBuffer(this.defaultBuffer);
            CachedPlaneMesh.closeBuffer(this.polishedBuffer);
            CachedPlaneMesh.closeBuffer(this.metalBuffer);
        }

        private static void closeBuffer(@Nullable VertexBuffer buffer) {
            if (buffer != null) {
                buffer.close();
            }
        }
    }

    private static final class MirrorFace {
        final BlockPos blockPos;
        final Direction face;
        final ReflectionMaterial material;
        final boolean usePolishedShader;
        final boolean useMetalShader;
        @Nullable
        final RainPuddleSurface rainPuddleSurface;
        final float[] warpedU = new float[4];
        final float[] warpedV = new float[4];

        MirrorFace(BlockPos p, Direction f, ReflectionMaterial material, boolean usePolishedShader, boolean useMetalShader, @Nullable RainPuddleSurface rainPuddleSurface, Vec3 cameraPos) {
            this.blockPos = p.m_7949_();
            this.face = f;
            this.material = material;
            this.usePolishedShader = usePolishedShader;
            this.useMetalShader = useMetalShader;
            this.rainPuddleSurface = rainPuddleSurface;
            for (int vertex = 0; vertex < 4; ++vertex) {
                float u = vertex == 0 || vertex == 3 ? 0.0f : 1.0f;
                float v = vertex <= 1 ? 0.0f : 1.0f;
                float worldX = (float)ReflectionSystem.facePointX(p, f, u);
                float worldY = (float)ReflectionSystem.facePointY(p, f, v);
                float worldZ = (float)ReflectionSystem.facePointZ(p, f, u, v);
                this.warpedU[vertex] = material.warpU(u, v, worldX, worldY, worldZ, p, f, vertex, cameraPos);
                this.warpedV[vertex] = material.warpV(u, v, worldX, worldY, worldZ, p, f, vertex, cameraPos);
            }
        }
    }

    private static final class RenderHelper
    extends RenderStateShard {
        static final RenderStateShard.TransparencyStateShard TRANSLUCENT = f_110139_;
        static final RenderStateShard.DepthTestStateShard LEQUAL = f_110113_;
        static final RenderStateShard.CullStateShard NO_CULL = RenderStateShard.f_110110_;
        static final RenderStateShard.LightmapStateShard LIGHTMAP_ON = f_110152_;
        static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = RenderStateShard.f_110115_;

        private RenderHelper() {
            super("loh_helper", () -> {}, () -> {});
        }
    }
}

