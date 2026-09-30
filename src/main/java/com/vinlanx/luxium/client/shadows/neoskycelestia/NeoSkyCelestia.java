/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.CloudStatus
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.fml.ModList
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.system.MemoryStack
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.clouds.LuxiumCloudRenderer;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCascade;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaFrameState;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaLighting;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaMath;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyEntityShadowMap;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyShadowRenderLists;
import com.vinlanx.luxium.client.shadows.neoskycelestia.embeddium.NeoSkyEmbeddiumShadowBridge;
import com.vinlanx.luxium.client.shadows.neoskycelestia.lightmap.NeoSkyCelestiaLightLut;
import com.vinlanx.luxium.client.shadows.neoskycelestia.lightmap.NeoSkyLightTextureExtension;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import java.nio.FloatBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public final class NeoSkyCelestia {
    private static final NeoSkyCelestia INSTANCE = new NeoSkyCelestia();
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");
    private final NeoSkyCascade near = new NeoSkyCascade("near");
    private final NeoSkyCascade far = new NeoSkyCascade("far");
    private final NeoSkyEntityShadowMap entityShadows = new NeoSkyEntityShadowMap();
    private final NeoSkyEmbeddiumShadowBridge embeddium = new NeoSkyEmbeddiumShadowBridge();
    private final NeoSkyCelestiaLightLut celestialLightLut = new NeoSkyCelestiaLightLut();
    private NeoSkyCelestiaFrameState frameState;
    private ClientLevel boundLevel;
    private int fallbackDepthTexture;
    private boolean mainPassActive;
    private boolean rebuilding;
    private boolean terrainShadowPass;
    private boolean cloudShadowPass;
    private boolean entityShadowPass;
    private float cloudPartialTick;
    private boolean inlineTerrainPass;
    private final Matrix4f nearLightFromWorldRelative = new Matrix4f();
    private final Matrix4f farLightFromWorldRelative = new Matrix4f();
    private final Matrix4f entityLightFromWorldRelative = new Matrix4f();
    private final Matrix4f inverseMainViewRotation = new Matrix4f();

    private NeoSkyCelestia() {
    }

    public static NeoSkyCelestia get() {
        return INSTANCE;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void prepareForMainWorldRender(Minecraft mc, Camera camera, float partialTick, Matrix4f mainProjection, Matrix4f mainViewRotation) {
        boolean buildNear;
        boolean wantsShadows;
        RenderSystem.assertOnRenderThread();
        this.mainPassActive = false;
        this.inlineTerrainPass = false;
        this.cloudPartialTick = partialTick;
        this.ensureFallbackDepthTexture();
        if (!this.canRun(mc)) {
            this.publishDisabledState(mc);
            return;
        }
        if (this.boundLevel != mc.f_91073_) {
            this.onWorldChanged(mc.f_91073_);
        }
        this.configureCascades();
        CelestialPath.State celestialPath = NeoSkyFrameCache.celestial(mc.f_91073_, partialTick);
        Vec3 toLight = celestialPath.activeDirection();
        NeoSkyCelestiaLighting.State lighting = NeoSkyFrameCache.lighting(mc.f_91073_, partialTick);
        float horizonVisibility = NeoSkyCelestiaMath.horizonFade(toLight);
        boolean wantsLighting = this.isGpuModeSelected() && lighting.enabled();
        boolean bl = wantsShadows = wantsLighting && this.isGpuModeSelected() && horizonVisibility > 0.005f;
        if (!wantsShadows && !wantsLighting) {
            this.publishDisabledState(mc);
            return;
        }
        Vec3 cameraPosition = camera.m_90583_();
        long now = System.nanoTime();
        boolean wantsEntityShadows = wantsShadows && Config.isFeatureEnabled(Config.CLIENT.skyEntityShadowsEnabled);
        double nearMovementThreshold = Math.max(1.5, (double)this.near.radius() * 16.0 / (double)this.near.resolution());
        long nearUpdateIntervalNanos = Math.max(10000000L, (long)((Double)Config.CLIENT.skyShadowNearUpdateIntervalSeconds.get() * 1.0E9));
        boolean bl2 = buildNear = wantsShadows && this.near.shouldBuild(cameraPosition, toLight, nearMovementThreshold, 0.0, nearUpdateIntervalNanos, now);
        boolean buildFar = wantsShadows && this.far.shouldBuild(cameraPosition, toLight, 4.0, Config.isFeatureEnabled(Config.CLIENT.skyCloudShadowsEnabled) ? 0.0 : 0.75, (long)((Integer)Config.CLIENT.skyShadowFarUpdateMs.get()).intValue() * 1000000L, now);
        long nearGenerationBefore = this.near.generation();
        long farGenerationBefore = this.far.generation();
        if (buildNear || buildFar) {
            this.rebuilding = true;
            try {
                NeoSkyEmbeddiumShadowBridge.PreparedRenderLists prepared;
                Matrix4f sharedLightRotation = NeoSkyCelestiaMath.buildLightViewRotation(toLight);
                if (buildNear) {
                    this.near.prepareReceiver(cameraPosition, toLight, sharedLightRotation);
                }
                if (buildFar) {
                    this.far.prepareReceiver(cameraPosition, toLight, sharedLightRotation);
                }
                if ((prepared = this.embeddium.prepareRenderLists(this.near, buildNear, this.far, buildFar)) == null) {
                    if (buildNear) {
                        this.near.markDirty();
                    }
                    if (buildFar) {
                        this.far.markDirty();
                    }
                } else {
                    if (buildNear) {
                        this.near.finalizeCasterVolume(prepared.nearCasterFront(), NeoSkyCelestia.computeAuxiliaryCasterFront(mc, this.near));
                        this.rebuildPreparedCascade(this.near, toLight, now, prepared.renderer(), prepared.near(), prepared.nearIncomplete());
                    }
                    if (buildFar) {
                        this.far.finalizeCasterVolume(prepared.farCasterFront(), NeoSkyCelestia.computeAuxiliaryCasterFront(mc, this.far));
                        this.rebuildPreparedCascade(this.far, toLight, now, prepared.renderer(), prepared.far(), prepared.farIncomplete());
                    }
                }
            }
            finally {
                this.rebuilding = false;
                NeoSkyCelestia.restoreKnownMainRenderState(mc);
            }
        }
        this.entityShadows.configure((Integer)Config.CLIENT.skyEntityShadowResolution.get(), ((Integer)Config.CLIENT.skyEntityShadowRadius.get()).intValue());
        if (wantsEntityShadows && this.entityShadows.shouldUpdate(now)) {
            this.rebuilding = true;
            try {
                this.entityShadows.render(mc, cameraPosition, toLight, partialTick, now);
            }
            finally {
                this.rebuilding = false;
                NeoSkyCelestia.restoreKnownMainRenderState(mc);
            }
        }
        NeoSkyFrameCache.copyInverseView(mainViewRotation, this.inverseMainViewRotation);
        this.near.updateReceiverMatrix(cameraPosition, this.inverseMainViewRotation);
        this.far.updateReceiverMatrix(cameraPosition, this.inverseMainViewRotation);
        this.entityShadows.updateReceiverMatrix(cameraPosition, this.inverseMainViewRotation);
        this.nearLightFromWorldRelative.set((Matrix4fc)this.near.lightFromMainView()).mul((Matrix4fc)mainViewRotation);
        this.farLightFromWorldRelative.set((Matrix4fc)this.far.lightFromMainView()).mul((Matrix4fc)mainViewRotation);
        this.entityLightFromWorldRelative.set((Matrix4fc)this.entityShadows.lightFromMainView()).mul((Matrix4fc)mainViewRotation);
        int blockOnlyTexture = NeoSkyCelestia.getBlockOnlyTexture(mc.f_91063_.m_109154_());
        boolean cascadesReady = wantsShadows && this.near.ready() && this.far.ready() && blockOnlyTexture > 0;
        boolean shadowMapUpdated = this.near.generation() != nearGenerationBefore || this.far.generation() != farGenerationBefore;
        float sunShadowBlockVisibility = ((Double)Config.CLIENT.vanillaBlockLightInSunShadows.get()).floatValue() * 0.01f * lighting.sunInfluence();
        int celestialLightLutTexture = this.celestialLightLut.textureId();
        if (wantsLighting && (shadowMapUpdated || celestialLightLutTexture <= 0)) {
            celestialLightLutTexture = this.celestialLightLut.update(mc.f_91063_.m_109154_(), lighting, cascadesReady, sunShadowBlockVisibility);
        }
        boolean shadowsReady = cascadesReady && celestialLightLutTexture > 0;
        boolean lightReady = wantsLighting && celestialLightLutTexture > 0;
        this.frameState = new NeoSkyCelestiaFrameState(shadowsReady, lightReady, lighting.moon(), blockOnlyTexture, celestialLightLutTexture, this.near.ready() ? this.near.target().depthTextureId() : this.fallbackDepthTexture, this.far.ready() ? this.far.target().depthTextureId() : this.fallbackDepthTexture, wantsEntityShadows && this.entityShadows.ready(), this.entityShadows.ready() ? this.entityShadows.depthTextureId() : this.fallbackDepthTexture, this.near.lightFromMainView(), this.far.lightFromMainView(), this.entityShadows.lightFromMainView(), this.near.radius(), this.far.radius(), this.entityShadows.radius(), 1.0f / (float)this.near.resolution(), 1.0f / (float)this.far.resolution(), this.entityShadows.texelSize(), (Integer)Config.CLIENT.skyShadowFilterSamples.get() >= 4 ? 4 : 1, NeoSkyCelestia.computeBaseBias(this.near), NeoSkyCelestia.computeBaseBias(this.far), this.entityShadows.baseBias(), 1.0f, 0.94f, new Vector3f((Vector3fc)lighting.direction()), new Vector3f((Vector3fc)lighting.directColor()), new Vector3f((Vector3fc)lighting.shadowSkyAmbientColor()), new Vector3f((Vector3fc)lighting.shadowGroundAmbientColor()), lighting.directStrength(), lighting.ambientStrength(), this.near.generation(), this.far.generation(), this.entityShadows.generation());
        this.mainPassActive = shadowsReady || lightReady;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void rebuildPreparedCascade(NeoSkyCascade cascade, Vec3 toLight, long now, ChunkRenderer renderer, NeoSkyShadowRenderLists lists, boolean incompleteCoverage) {
        boolean rendered;
        cascade.target().bindAndClear();
        RenderSystem.polygonOffset((float)1.5f, (float)1.0f);
        RenderSystem.enablePolygonOffset();
        this.terrainShadowPass = true;
        try {
            RenderSystem.disableCull();
            rendered = this.embeddium.renderCascade(cascade, renderer, lists);
        }
        finally {
            this.terrainShadowPass = false;
            RenderSystem.polygonOffset((float)0.0f, (float)0.0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
            GL11.glCullFace((int)1029);
        }
        if (rendered) {
            this.renderCloudCasters(cascade);
        }
        if (rendered) {
            cascade.commitBuild(toLight, now);
            if (incompleteCoverage) {
                cascade.markDirty();
            }
        } else {
            cascade.markDirty();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderCloudCasters(NeoSkyCascade cascade) {
        Minecraft mc = Minecraft.m_91087_();
        if (!Config.isFeatureEnabled(Config.CLIENT.skyCloudShadowsEnabled) || mc.f_91060_ == null || mc.f_91066_.m_92174_() == CloudStatus.OFF) {
            return;
        }
        Vec3 center = cascade.center();
        Vec3 eye = cascade.eye();
        PoseStack poseStack = new PoseStack();
        poseStack.m_252931_(new Matrix4f((Matrix4fc)cascade.lightViewRotation()).translate((float)(center.f_82479_ - eye.f_82479_), (float)(center.f_82480_ - eye.f_82480_), (float)(center.f_82481_ - eye.f_82481_)));
        this.cloudShadowPass = true;
        try {
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)513);
            RenderSystem.disableBlend();
            boolean handledCustomClouds = LuxiumCloudRenderer.get().renderCustomShadowCasters(poseStack, cascade.lightProjection(), cascade.lightViewRotation(), cascade.projectionRadius(), cascade.toLight(), this.cloudPartialTick, center.f_82479_, center.f_82480_, center.f_82481_, mc.f_91060_.getTicks());
            if (!handledCustomClouds) {
                mc.f_91060_.m_253054_(poseStack, cascade.lightProjection(), this.cloudPartialTick, center.f_82479_, center.f_82480_, center.f_82481_);
            }
        }
        finally {
            this.cloudShadowPass = false;
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask((boolean)true);
            RenderSystem.depthFunc((int)513);
            RenderSystem.disableBlend();
        }
    }

    private static void restoreKnownMainRenderState(Minecraft mc) {
        NeoSkyCelestia.resetSkyGpuShaderBindings();
        if (mc.m_91385_() != null) {
            mc.m_91385_().m_83947_(true);
        }
        GlStateManager._activeTexture((int)33984);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        RenderSystem.polygonOffset((float)0.0f, (float)0.0f);
        RenderSystem.disablePolygonOffset();
        GlStateManager._enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        GL11.glCullFace((int)1029);
    }

    public void beginInlineTerrainPass() {
        if (!this.isMainPassActive() || this.inlineTerrainPass) {
            return;
        }
        this.inlineTerrainPass = true;
    }

    public void finishInlineTerrainPass() {
        this.inlineTerrainPass = false;
    }

    public boolean isInlineTerrainPassActive() {
        return this.inlineTerrainPass;
    }

    public Matrix4f nearLightFromWorldRelative() {
        return this.nearLightFromWorldRelative;
    }

    public Matrix4f farLightFromWorldRelative() {
        return this.farLightFromWorldRelative;
    }

    public Matrix4f entityLightFromWorldRelative() {
        return this.entityLightFromWorldRelative;
    }

    public void finishMainWorldRender() {
        this.inlineTerrainPass = false;
        boolean wasActive = this.mainPassActive;
        this.mainPassActive = false;
        if (wasActive) {
            NeoSkyCelestia.resetSkyGpuShaderBindings();
        }
    }

    private static void resetSkyGpuShaderBindings() {
        ShaderInstance trackedShader = RenderSystem.getShader();
        if (trackedShader != null) {
            trackedShader.m_173362_();
        } else {
            GlStateManager._glUseProgram((int)0);
        }
        for (int unit = 2; unit <= 6; ++unit) {
            GlStateManager._activeTexture((int)(33984 + unit));
            GlStateManager._bindTexture((int)0);
            GL11.glBindTexture((int)32879, (int)0);
            RenderSystem.setShaderTexture((int)unit, (int)0);
        }
        GlStateManager._activeTexture((int)33984);
    }

    public boolean isMainPassActive() {
        return this.mainPassActive && !this.rebuilding;
    }

    public boolean isRebuildingShadowMap() {
        return this.rebuilding;
    }

    public boolean isRenderingTerrainShadowPass() {
        return this.terrainShadowPass;
    }

    public boolean isRenderingCloudShadowPass() {
        return this.cloudShadowPass;
    }

    public boolean isRenderingEntityShadowPass() {
        return this.entityShadowPass;
    }

    public void setEntityShadowPass(boolean active) {
        this.entityShadowPass = active;
    }

    public void bindEntityShadowTarget() {
        if (this.entityShadowPass) {
            this.entityShadows.bindTarget();
        }
    }

    public boolean isConfiguredAndAvailable() {
        Minecraft mc = Minecraft.m_91087_();
        return this.isGpuModeSelected() && this.embeddium.isAvailable();
    }

    public boolean isGpuModeSelected() {
        Minecraft mc = Minecraft.m_91087_();
        return LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled() && !LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled() && !LuxiumGpuShaderFeatures.localReceiverEnabled() && !OCULUS_LOADED && mc.f_91073_ != null && mc.f_91073_.m_6042_().f_223549_();
    }

    public NeoSkyCelestiaFrameState frameState() {
        if (this.frameState == null) {
            this.ensureFallbackDepthTexture();
            this.frameState = NeoSkyCelestiaFrameState.disabled(this.celestialLightLut.textureId(), this.fallbackDepthTexture);
        }
        return this.frameState;
    }

    public void markBlockDirty(int x, int y, int z) {
        boolean nearDirty = this.near.intersectsBlock(x, y, z);
        boolean farDirty = this.far.intersectsBlock(x, y, z);
        if (!nearDirty && !farDirty) {
            return;
        }
        if (nearDirty) {
            this.near.markDirty();
        }
        if (farDirty) {
            this.far.markDirty();
        }
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        ClientLevel level = this.boundLevel;
        if (level == null) {
            this.markAllDirty();
            return;
        }
        int minY = level.m_141937_();
        int maxY = level.m_151558_();
        boolean nearDirty = this.near.intersectsChunk(chunkX, chunkZ, minY, maxY);
        boolean farDirty = this.far.intersectsChunk(chunkX, chunkZ, minY, maxY);
        if (!nearDirty && !farDirty) {
            return;
        }
        if (nearDirty) {
            this.near.markDirty();
        }
        if (farDirty) {
            this.far.markDirty();
        }
    }

    public void markAllDirty() {
        this.near.markDirty();
        this.far.markDirty();
    }

    public void onWorldChanged(ClientLevel level) {
        this.boundLevel = level;
        this.near.invalidate();
        this.far.invalidate();
        this.entityShadows.invalidate();
        this.embeddium.clearCaches();
        this.publishDisabledState(Minecraft.m_91087_());
    }

    public void close() {
        RenderSystem.assertOnRenderThread();
        this.near.close();
        this.far.close();
        this.entityShadows.close();
        this.celestialLightLut.close();
        this.embeddium.clearCaches();
        if (this.fallbackDepthTexture != 0) {
            GlStateManager._deleteTexture((int)this.fallbackDepthTexture);
            this.fallbackDepthTexture = 0;
        }
        this.frameState = null;
        this.boundLevel = null;
        this.mainPassActive = false;
        this.rebuilding = false;
        this.terrainShadowPass = false;
        this.cloudShadowPass = false;
        this.entityShadowPass = false;
        this.inlineTerrainPass = false;
    }

    private boolean canRun(Minecraft mc) {
        return this.isGpuModeSelected() && this.embeddium.isAvailable() && mc.f_91074_ != null && !NeoShadowsEngine.isAnyShadowCapturePass() && !ReflectionSystem.isRenderingWorldPass();
    }

    private void configureCascades() {
        float nearRadius = ((Integer)Config.CLIENT.skyShadowNearRadius.get()).intValue();
        float farRadius = Math.max((float)((Integer)Config.CLIENT.skyShadowFarRadius.get()).intValue(), nearRadius + 16.0f);
        float minimumCasterReach = ((Integer)Config.CLIENT.skyShadowRayLength.get()).intValue();
        this.near.configure((Integer)Config.CLIENT.skyShadowNearResolution.get(), nearRadius, minimumCasterReach);
        this.far.configure((Integer)Config.CLIENT.skyShadowFarResolution.get(), farRadius, minimumCasterReach);
        boolean softShadows = (Boolean)Config.CLIENT.skyShadowSoftShadowsEnabled.get();
        this.near.target().setLinearFiltering(softShadows);
        this.far.target().setLinearFiltering(softShadows);
    }

    private static float computeAuxiliaryCasterFront(Minecraft mc, NeoSkyCascade cascade) {
        if (mc == null || mc.f_91073_ == null || !Config.isFeatureEnabled(Config.CLIENT.skyCloudShadowsEnabled) || mc.f_91066_.m_92174_() == CloudStatus.OFF) {
            return Float.NaN;
        }
        float cloudHeight = mc.f_91073_.m_104583_().m_108871_();
        if (!Float.isFinite(cloudHeight)) {
            return Float.NaN;
        }
        if (Config.isFeatureEnabled(Config.CLIENT.cloudsEnabled)) {
            return LuxiumCloudRenderer.get().computeCustomShadowCasterFront(cascade.center(), cascade.toLight(), cascade.lightViewRotation(), cascade.projectionRadius(), mc.f_91066_.m_92174_());
        }
        Vec3 center = cascade.center();
        Vec3 toLight = cascade.toLight();
        if (toLight.f_82480_ <= 0.0) {
            return Float.NaN;
        }
        double cloudCasterReach = ((double)cloudHeight - center.f_82480_) / toLight.f_82480_;
        double receiverFront = (double)cascade.projectionRadius() * (Math.abs(toLight.f_82479_) + Math.abs(toLight.f_82481_));
        return (float)(cloudCasterReach + receiverFront + 16.0);
    }

    private void publishDisabledState(Minecraft mc) {
        this.ensureFallbackDepthTexture();
        this.frameState = NeoSkyCelestiaFrameState.disabled(this.celestialLightLut.textureId(), this.fallbackDepthTexture);
        this.mainPassActive = false;
    }

    private static int getBlockOnlyTexture(LightTexture lightTexture) {
        return ((NeoSkyLightTextureExtension)lightTexture).neosky$getBlockOnlyLightTextureId();
    }

    private static float computeBaseBias(NeoSkyCascade cascade) {
        float worldTexel = cascade.projectionRadius() * 2.0f / (float)Math.max(1, cascade.resolution());
        float worldBias = Math.max(0.008f, Math.min(0.04f, worldTexel * 0.18f));
        float depthRange = Math.max(1.0f, cascade.depthRange());
        return worldBias / depthRange;
    }

    private void ensureFallbackDepthTexture() {
        if (this.fallbackDepthTexture != 0) {
            return;
        }
        this.fallbackDepthTexture = GlStateManager._genTexture();
        GlStateManager._bindTexture((int)this.fallbackDepthTexture);
        try (MemoryStack stack = MemoryStack.stackPush();){
            GL11.glTexImage2D((int)3553, (int)0, (int)33190, (int)1, (int)1, (int)0, (int)6402, (int)5126, (FloatBuffer)stack.floats(1.0f));
        }
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
    }
}

