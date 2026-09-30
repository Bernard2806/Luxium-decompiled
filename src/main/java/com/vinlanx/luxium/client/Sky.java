/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.posteffects.skygodrays;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import com.vinlanx.luxium.client.sunmoonapi.CelestialPath;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class Sky {
    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
    private static final int CACHE_FACE_SIZE = 1024;
    private static final int FACE_COUNT = 6;
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer((int)4);
    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer((int)4);
    private static final Matrix4f PROJ_INVERSE = new Matrix4f();
    private static final Matrix4f VIEW_INVERSE = new Matrix4f();
    private static int cacheTextureId;
    private static int stagingCacheTextureId;
    private static int cacheFramebufferId;
    private static long nextCacheUpdateNanos;
    private static ClientLevel cachedLevel;
    private static boolean bakeInProgress;
    private static int bakeWorkIndex;
    private static boolean bakeSmooth;
    private static int bakeTileSize;
    private static int bakeWorkPerFrame;
    private static float bakeSunX;
    private static float bakeSunY;
    private static float bakeSunZ;
    private static ClientLevel bakeLevel;
    private static int cachedSamplerProgramId;
    private static int cachedSamplerLocation;

    private Sky() {
    }

    public static int getReflectionCacheTextureId() {
        Minecraft mc = Minecraft.m_91087_();
        return cacheTextureId != 0 && cachedLevel == mc.f_91073_ ? cacheTextureId : 0;
    }

    public static void onConfigChanged() {
        bakeInProgress = false;
        bakeWorkIndex = 0;
        bakeLevel = null;
        nextCacheUpdateNanos = 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null) {
            return;
        }
        ShaderInstance displayShader = ShaderManager.getSkyShader();
        ShaderInstance bakeShader = ShaderManager.getSkyBakeShader();
        if (displayShader == null || bakeShader == null) {
            return;
        }
        CelestialPath.State celestialPath = NeoSkyFrameCache.celestial(mc.f_91073_, partialTick);
        Sky.updateCache(bakeShader, celestialPath.sunDirection(), mc.f_91073_);
        if (cacheTextureId == 0 || cachedLevel != mc.f_91073_) {
            return;
        }
        Matrix4f projInverse = NeoSkyFrameCache.copyInverseProjection(projectionMatrix, PROJ_INVERSE);
        Matrix4f viewInverse = NeoSkyFrameCache.copyInverseView(poseStack.m_85850_().m_252922_(), VIEW_INVERSE);
        if (displayShader.m_173348_("ProjInverseMat") != null) {
            displayShader.m_173348_("ProjInverseMat").m_5679_(projInverse);
        }
        if (displayShader.m_173348_("ModelViewInverseMat") != null) {
            displayShader.m_173348_("ModelViewInverseMat").m_5679_(viewInverse);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask((boolean)false);
        GlStateManager._disableDepthTest();
        RenderSystem.disableCull();
        displayShader.m_173363_();
        try {
            Sky.bindCacheTexture(displayShader);
            Sky.drawFullscreenQuad();
        }
        finally {
            displayShader.m_173362_();
        }
        Sky.renderVanillaCelestials(mc, poseStack, projectionMatrix, partialTick);
        GlStateManager._enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void updateCache(ShaderInstance bakeShader, Vec3 sunDirection, ClientLevel level) {
        boolean displayCacheValidForLevel;
        long now = System.nanoTime();
        if (bakeInProgress && bakeLevel != level) {
            bakeInProgress = false;
            bakeWorkIndex = 0;
            bakeLevel = null;
            nextCacheUpdateNanos = 0L;
        }
        boolean bl = displayCacheValidForLevel = cacheTextureId != 0 && cachedLevel == level;
        if (!bakeInProgress) {
            if (displayCacheValidForLevel && now < nextCacheUpdateNanos) {
                return;
            }
            Sky.beginBake(level, sunDirection);
        }
        Sky.renderBakeWork(bakeShader);
    }

    private static void beginBake(ClientLevel level, Vec3 sunDirection) {
        Sky.ensureCacheStorage();
        bakeInProgress = true;
        bakeWorkIndex = 0;
        bakeLevel = level;
        bakeSunX = (float)sunDirection.f_82479_;
        bakeSunY = (float)sunDirection.f_82480_;
        bakeSunZ = (float)sunDirection.f_82481_;
        bakeSmooth = (Boolean)Config.CLIENT.skyBakeSmoothEnabled.get();
        bakeWorkPerFrame = Mth.m_14045_((int)((Integer)Config.CLIENT.skyBakeWorkPerFrame.get()), (int)1, (int)6);
        bakeTileSize = ((Config.SkyBakeTileSize)((Object)Config.CLIENT.skyBakeTileSize.get())).pixels();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void renderBakeWork(ShaderInstance bakeShader) {
        int totalWork = Sky.getTotalBakeWork();
        if (bakeWorkIndex >= totalWork) {
            Sky.finishBake();
            return;
        }
        int previousFramebuffer = GL11.glGetInteger((int)36006);
        int previousActiveTexture = GlStateManager._getActiveTexture();
        int previousCubeTexture = GL11.glGetInteger((int)34068);
        boolean previousScissorEnabled = GL11.glIsEnabled((int)3089);
        boolean previousDepthTestEnabled = GL11.glIsEnabled((int)2929);
        boolean previousDepthMask = GL11.glGetBoolean((int)2930);
        boolean previousBlendEnabled = GL11.glIsEnabled((int)3042);
        boolean previousCullEnabled = GL11.glIsEnabled((int)2884);
        VIEWPORT.clear();
        GL11.glGetIntegerv((int)2978, (IntBuffer)VIEWPORT);
        int previousViewportX = VIEWPORT.get(0);
        int previousViewportY = VIEWPORT.get(1);
        int previousViewportWidth = VIEWPORT.get(2);
        int previousViewportHeight = VIEWPORT.get(3);
        SCISSOR_BOX.clear();
        GL11.glGetIntegerv((int)3088, (IntBuffer)SCISSOR_BOX);
        int previousScissorX = SCISSOR_BOX.get(0);
        int previousScissorY = SCISSOR_BOX.get(1);
        int previousScissorWidth = SCISSOR_BOX.get(2);
        int previousScissorHeight = SCISSOR_BOX.get(3);
        try {
            GlStateManager._glBindFramebuffer((int)36160, (int)cacheFramebufferId);
            GlStateManager._viewport((int)0, (int)0, (int)1024, (int)1024);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            if (bakeShader.m_173348_("SunDir") != null) {
                bakeShader.m_173348_("SunDir").m_5889_(bakeSunX, bakeSunY, bakeSunZ);
            }
            if (bakeShader.m_173348_("CachePass") != null) {
                bakeShader.m_173348_("CachePass").m_142617_(1);
            }
            if (bakeShader.m_173348_("CacheFaceSize") != null) {
                bakeShader.m_173348_("CacheFaceSize").m_142617_(1024);
            }
            int workThisFrame = Math.min(bakeWorkPerFrame, totalWork - bakeWorkIndex);
            int i = 0;
            while (i < workThisFrame) {
                Sky.renderBakeUnit(bakeShader, bakeWorkIndex);
                ++i;
                ++bakeWorkIndex;
            }
        }
        finally {
            if (previousScissorEnabled) {
                GL11.glEnable((int)3089);
                GL11.glScissor((int)previousScissorX, (int)previousScissorY, (int)previousScissorWidth, (int)previousScissorHeight);
            } else {
                GL11.glDisable((int)3089);
            }
            if (previousDepthTestEnabled) {
                GlStateManager._enableDepthTest();
            } else {
                GlStateManager._disableDepthTest();
            }
            GlStateManager._depthMask((boolean)previousDepthMask);
            if (previousBlendEnabled) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (previousCullEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            GL11.glBindTexture((int)34067, (int)previousCubeTexture);
            GlStateManager._glBindFramebuffer((int)36160, (int)previousFramebuffer);
            GlStateManager._viewport((int)previousViewportX, (int)previousViewportY, (int)previousViewportWidth, (int)previousViewportHeight);
            GlStateManager._activeTexture((int)previousActiveTexture);
        }
        if (bakeWorkIndex >= totalWork) {
            Sky.finishBake();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void renderBakeUnit(ShaderInstance bakeShader, int workIndex) {
        int face;
        if (bakeSmooth) {
            int tilesPerAxis = 1024 / bakeTileSize;
            int tilesPerFace = tilesPerAxis * tilesPerAxis;
            face = workIndex / tilesPerFace;
            int tileIndex = workIndex % tilesPerFace;
            int tileX = tileIndex % tilesPerAxis;
            int tileY = tileIndex / tilesPerAxis;
            GL11.glEnable((int)3089);
            GL11.glScissor((int)(tileX * bakeTileSize), (int)(tileY * bakeTileSize), (int)bakeTileSize, (int)bakeTileSize);
        } else {
            face = workIndex;
            GL11.glDisable((int)3089);
        }
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)(34069 + face), (int)stagingCacheTextureId, (int)0);
        if (bakeShader.m_173348_("CubeFace") != null) {
            bakeShader.m_173348_("CubeFace").m_142617_(face);
        }
        bakeShader.m_173363_();
        try {
            Sky.drawFullscreenQuad();
        }
        finally {
            bakeShader.m_173362_();
        }
    }

    private static int getTotalBakeWork() {
        if (!bakeSmooth) {
            return 6;
        }
        int tilesPerAxis = 1024 / bakeTileSize;
        return 6 * tilesPerAxis * tilesPerAxis;
    }

    private static void finishBake() {
        int completedTexture = stagingCacheTextureId;
        stagingCacheTextureId = cacheTextureId;
        cacheTextureId = completedTexture;
        cachedLevel = bakeLevel;
        bakeLevel = null;
        bakeInProgress = false;
        bakeWorkIndex = 0;
        nextCacheUpdateNanos = System.nanoTime() + Sky.getCacheUpdateIntervalNanos();
    }

    private static long getCacheUpdateIntervalNanos() {
        double seconds = Mth.m_14008_((double)((Double)Config.CLIENT.skyBakeUpdateIntervalSeconds.get()), (double)0.1, (double)2.0);
        return Math.max(1L, (long)(seconds * 1.0E9));
    }

    private static void ensureCacheStorage() {
        if (cacheTextureId != 0 && stagingCacheTextureId != 0) {
            return;
        }
        int previousFramebuffer = GL11.glGetInteger((int)36006);
        int previousActiveTexture = GlStateManager._getActiveTexture();
        int previousCubeTexture = GL11.glGetInteger((int)34068);
        cacheTextureId = GlStateManager._genTexture();
        stagingCacheTextureId = GlStateManager._genTexture();
        cacheFramebufferId = GlStateManager.glGenFramebuffers();
        Sky.configureCubemapTexture(cacheTextureId);
        Sky.configureCubemapTexture(stagingCacheTextureId);
        GlStateManager._glBindFramebuffer((int)36160, (int)cacheFramebufferId);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)34069, (int)stagingCacheTextureId, (int)0);
        if (GL30.glCheckFramebufferStatus((int)36160) != 36053) {
            throw new IllegalStateException("Luxium sky cubemap framebuffer is incomplete");
        }
        GL11.glBindTexture((int)34067, (int)previousCubeTexture);
        GlStateManager._glBindFramebuffer((int)36160, (int)previousFramebuffer);
        GlStateManager._activeTexture((int)previousActiveTexture);
    }

    private static void configureCubemapTexture(int textureId) {
        GL11.glBindTexture((int)34067, (int)textureId);
        GL11.glTexParameteri((int)34067, (int)10241, (int)9729);
        GL11.glTexParameteri((int)34067, (int)10240, (int)9729);
        GL11.glTexParameteri((int)34067, (int)10242, (int)33071);
        GL11.glTexParameteri((int)34067, (int)10243, (int)33071);
        GL11.glTexParameteri((int)34067, (int)32882, (int)33071);
        for (int face = 0; face < 6; ++face) {
            GL11.glTexImage2D((int)(34069 + face), (int)0, (int)34842, (int)1024, (int)1024, (int)0, (int)6408, (int)5131, (long)0L);
        }
    }

    private static void bindCacheTexture(ShaderInstance shader) {
        int programId = shader.m_108943_();
        if (cachedSamplerProgramId != programId) {
            cachedSamplerProgramId = programId;
            cachedSamplerLocation = GL20.glGetUniformLocation((int)programId, (CharSequence)"SkyCache");
        }
        if (cachedSamplerLocation < 0) {
            return;
        }
        GlStateManager._activeTexture((int)33984);
        GL20.glUniform1i((int)cachedSamplerLocation, (int)0);
        GL11.glBindTexture((int)34067, (int)cacheTextureId);
    }

    private static void drawFullscreenQuad() {
        Tesselator tesselator = Tesselator.m_85913_();
        BufferBuilder bufferBuilder = tesselator.m_85915_();
        bufferBuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85814_);
        bufferBuilder.m_5483_(-1.0, -1.0, 0.0).m_5752_();
        bufferBuilder.m_5483_(1.0, -1.0, 0.0).m_5752_();
        bufferBuilder.m_5483_(1.0, 1.0, 0.0).m_5752_();
        bufferBuilder.m_5483_(-1.0, 1.0, 0.0).m_5752_();
        BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)bufferBuilder.m_231175_());
    }

    private static void renderVanillaCelestials(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
        skygodrays.beginSkyCapture();
        Tesselator tesselator = Tesselator.m_85913_();
        BufferBuilder bufferBuilder = tesselator.m_85915_();
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        poseStack.m_85836_();
        float rainFade = 1.0f - mc.f_91073_.m_46722_(partialTick);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)rainFade);
        CelestialPath.applyOrbit(poseStack, mc.f_91073_.m_46490_(partialTick));
        Matrix4f celestialMatrix = poseStack.m_85850_().m_252922_();
        RenderSystem.setShader(GameRenderer::m_172817_);
        skygodrays.captureCelestial(new Matrix4f((Matrix4fc)celestialMatrix), new Matrix4f((Matrix4fc)projectionMatrix), false, partialTick);
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)SUN_LOCATION);
        float sunSize = 30.0f * Mth.m_14036_((float)((Double)Config.CLIENT.skySunSize.get()).floatValue(), (float)0.1f, (float)3.0f);
        bufferBuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        bufferBuilder.m_252986_(celestialMatrix, -sunSize, 100.0f, -sunSize).m_7421_(0.0f, 0.0f).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, sunSize, 100.0f, -sunSize).m_7421_(1.0f, 0.0f).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, sunSize, 100.0f, sunSize).m_7421_(1.0f, 1.0f).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, -sunSize, 100.0f, sunSize).m_7421_(0.0f, 1.0f).m_5752_();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)bufferBuilder.m_231175_());
        skygodrays.captureCelestial(new Matrix4f((Matrix4fc)celestialMatrix), new Matrix4f((Matrix4fc)projectionMatrix), true, partialTick);
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)MOON_LOCATION);
        float moonSize = 20.0f * Mth.m_14036_((float)((Double)Config.CLIENT.skyMoonSize.get()).floatValue(), (float)0.1f, (float)3.0f);
        int moonPhase = mc.f_91073_.m_46941_();
        int phaseX = moonPhase % 4;
        int phaseY = moonPhase / 4 % 2;
        float minU = (float)phaseX / 4.0f;
        float minV = (float)phaseY / 2.0f;
        float maxU = (float)(phaseX + 1) / 4.0f;
        float maxV = (float)(phaseY + 1) / 2.0f;
        bufferBuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        bufferBuilder.m_252986_(celestialMatrix, -moonSize, -100.0f, moonSize).m_7421_(maxU, maxV).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, moonSize, -100.0f, moonSize).m_7421_(minU, maxV).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, moonSize, -100.0f, -moonSize).m_7421_(minU, minV).m_5752_();
        bufferBuilder.m_252986_(celestialMatrix, -moonSize, -100.0f, -moonSize).m_7421_(maxU, minV).m_5752_();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)bufferBuilder.m_231175_());
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.defaultBlendFunc();
        poseStack.m_85849_();
    }

    static {
        bakeTileSize = 512;
        bakeWorkPerFrame = 2;
        bakeSunY = 1.0f;
        cachedSamplerProgramId = -1;
        cachedSamplerLocation = -1;
    }
}

