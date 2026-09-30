/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.shaders.Uniform
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.tfrpluslsr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.tfrpluslsr.LsrSystem;
import com.vinlanx.luxium.mixin.MinecraftAccessor;
import java.nio.ByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class TemporalFrameSystem {
    private static final float MOTION_FIELD_SCALE = 0.5f;
    private static final double CAMERA_MOTION_EPSILON_SQ = 1.0E-8;
    private static final float CAMERA_ANGLE_EPSILON = 0.01f;
    @Nullable
    private static TextureTarget sceneTarget;
    @Nullable
    private static TextureTarget preparedHistoryTarget;
    @Nullable
    private static TextureTarget motionTarget;
    @Nullable
    private static RenderTarget nativeMainTarget;
    @Nullable
    private static Object historyLevel;
    private static final Matrix4f previousViewProjection;
    private static final Matrix4f previousInverseViewProjection;
    private static final Matrix4f currentViewProjection;
    private static final Matrix4f currentViewMatrix;
    private static final Matrix4f geometryReprojection;
    private static final Matrix4f skyReprojection;
    private static final Matrix4f cameraTranslation;
    private static double previousCameraX;
    private static double previousCameraY;
    private static double previousCameraZ;
    private static float previousYaw;
    private static float previousPitch;
    private static boolean historyValid;
    private static boolean preparedHistoryValid;
    private static int syntheticFramesSinceReal;
    private static boolean syntheticThisFrame;
    private static boolean worldPassActive;
    private static boolean firstPersonOverlayPass;

    private TemporalFrameSystem() {
    }

    public static void onConfigChanged() {
        TemporalFrameSystem.invalidateHistory();
        if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled)) {
            TemporalFrameSystem.releaseTargets();
        }
    }

    public static boolean isSyntheticThisFrame() {
        return syntheticThisFrame;
    }

    public static boolean isWorldPassActive() {
        return worldPassActive;
    }

    public static boolean beginRealFrame(Minecraft mc) {
        syntheticThisFrame = false;
        if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled) || mc.f_91073_ == null) {
            if (historyValid || historyLevel != mc.f_91073_) {
                TemporalFrameSystem.invalidateHistory();
            }
            if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled)) {
                TemporalFrameSystem.releaseTargets();
            }
            return false;
        }
        RenderTarget nativeTarget = mc.m_91385_();
        if (nativeTarget == null || nativeTarget.f_83915_ <= 0 || nativeTarget.f_83916_ <= 0) {
            TemporalFrameSystem.invalidateHistory();
            return false;
        }
        int sourceWidth = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) ? LsrSystem.expectedWidth(nativeTarget.f_83915_) : nativeTarget.f_83915_;
        int sourceHeight = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) ? LsrSystem.expectedHeight(nativeTarget.f_83916_) : nativeTarget.f_83916_;
        TemporalFrameSystem.ensureSceneTarget(sourceWidth, sourceHeight);
        TextureTarget scene = sceneTarget;
        if (scene == null) {
            TemporalFrameSystem.invalidateHistory();
            return false;
        }
        nativeMainTarget = nativeTarget;
        worldPassActive = true;
        ((MinecraftAccessor)mc).luxium$setMainRenderTargetField((RenderTarget)scene);
        scene.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        scene.m_83954_(Minecraft.f_91002_);
        scene.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)scene.f_83917_, (int)scene.f_83918_);
        return true;
    }

    public static void rebindWorldTarget() {
        if (!worldPassActive || sceneTarget == null) {
            return;
        }
        sceneTarget.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)TemporalFrameSystem.sceneTarget.f_83917_, (int)TemporalFrameSystem.sceneTarget.f_83918_);
    }

    public static void finishRealFrame(Minecraft mc, Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        boolean presented;
        TextureTarget source = sceneTarget;
        RenderTarget destination = nativeMainTarget;
        if (!worldPassActive || source == null || destination == null) {
            TemporalFrameSystem.abortRealFrame(mc);
            return;
        }
        ((MinecraftAccessor)mc).luxium$setMainRenderTargetField(destination);
        worldPassActive = false;
        destination.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        preparedHistoryValid = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) ? TemporalFrameSystem.prepareLsrHistory() : false;
        previousViewProjection.set((Matrix4fc)projectionMatrix).mul((Matrix4fc)viewMatrix);
        previousInverseViewProjection.set((Matrix4fc)previousViewProjection).invert();
        Vec3 cameraPos = camera.m_90583_();
        previousCameraX = cameraPos.f_82479_;
        previousCameraY = cameraPos.f_82480_;
        previousCameraZ = cameraPos.f_82481_;
        previousYaw = camera.m_90590_();
        previousPitch = camera.m_90589_();
        historyLevel = mc.f_91073_;
        historyValid = true;
        syntheticFramesSinceReal = 0;
        ShaderInstance finalShader = ShaderManager.getTemporalReprojectionShader();
        boolean bl = presented = finalShader != null && TemporalFrameSystem.renderFinalComposite(finalShader, destination, false);
        if (!presented) {
            TemporalFrameSystem.blitSceneFallback((RenderTarget)source, destination);
        }
        nativeMainTarget = null;
        destination.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
    }

    public static void abortRealFrame(Minecraft mc) {
        RenderTarget destination = nativeMainTarget;
        if (destination != null) {
            ((MinecraftAccessor)mc).luxium$setMainRenderTargetField(destination);
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        }
        worldPassActive = false;
        nativeMainTarget = null;
        TemporalFrameSystem.invalidateHistory();
    }

    public static boolean tryRenderSynthetic(Minecraft mc, Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        int expectedHeight;
        syntheticThisFrame = false;
        currentViewMatrix.set((Matrix4fc)viewMatrix);
        if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled) || mc.f_91073_ == null) {
            if (historyValid || historyLevel != mc.f_91073_) {
                TemporalFrameSystem.invalidateHistory();
            }
            if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled)) {
                TemporalFrameSystem.releaseTargets();
            }
            return false;
        }
        RenderTarget main = mc.m_91385_();
        TextureTarget scene = sceneTarget;
        ShaderInstance finalShader = ShaderManager.getTemporalReprojectionShader();
        if (main == null || scene == null || finalShader == null || main.f_83915_ <= 0 || main.f_83916_ <= 0) {
            return false;
        }
        if (!historyValid || historyLevel != mc.f_91073_) {
            return false;
        }
        if (Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) && !preparedHistoryValid) {
            return false;
        }
        int expectedWidth = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) ? LsrSystem.expectedWidth(main.f_83915_) : main.f_83915_;
        int n = expectedHeight = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) ? LsrSystem.expectedHeight(main.f_83916_) : main.f_83916_;
        if (scene.f_83915_ != expectedWidth || scene.f_83916_ != expectedHeight) {
            TemporalFrameSystem.invalidateHistory();
            return false;
        }
        if (syntheticFramesSinceReal >= 1 || TemporalFrameSystem.isCameraCut(camera)) {
            return false;
        }
        currentViewProjection.set((Matrix4fc)projectionMatrix).mul((Matrix4fc)viewMatrix);
        boolean cameraMoved = TemporalFrameSystem.hasMeaningfulCameraMotion(camera);
        if (cameraMoved) {
            ShaderInstance motionShader = ShaderManager.getTemporalMotionShader();
            if (motionShader == null) {
                return false;
            }
            int motionWidth = Math.max(1, Math.round((float)scene.f_83915_ * 0.5f));
            int motionHeight = Math.max(1, Math.round((float)scene.f_83916_ * 0.5f));
            TemporalFrameSystem.ensureMotionTarget(motionWidth, motionHeight);
            if (motionTarget == null) {
                return false;
            }
            TemporalFrameSystem.buildReprojectionMatrices(camera);
            if (!TemporalFrameSystem.renderMotionField(motionShader)) {
                return false;
            }
        }
        if (!TemporalFrameSystem.renderFinalComposite(finalShader, main, cameraMoved)) {
            return false;
        }
        ++syntheticFramesSinceReal;
        syntheticThisFrame = true;
        return true;
    }

    public static void beginFirstPersonOverlay(Minecraft mc) {
        firstPersonOverlayPass = false;
        if (!Config.isFeatureEnabled(Config.CLIENT.tfrEnabled)) {
            return;
        }
        RenderTarget main = mc.m_91385_();
        if (main == null) {
            return;
        }
        firstPersonOverlayPass = true;
        main.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_);
        RenderSystem.activeTexture((int)33984);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        GlStateManager._enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (mc.f_91073_ != null) {
            Matrix4f lightingView = new Matrix4f((Matrix4fc)currentViewMatrix);
            if (mc.f_91073_.m_104583_().m_108885_()) {
                Lighting.m_252995_((Matrix4f)lightingView);
            } else {
                Lighting.m_252756_((Matrix4f)lightingView);
            }
        }
    }

    public static void endFirstPersonOverlay() {
        firstPersonOverlayPass = false;
    }

    public static boolean isFirstPersonOverlayPass() {
        return firstPersonOverlayPass;
    }

    public static void invalidate() {
        TemporalFrameSystem.invalidateHistory();
        worldPassActive = false;
        nativeMainTarget = null;
    }

    private static void invalidateHistory() {
        historyValid = false;
        preparedHistoryValid = false;
        historyLevel = null;
        syntheticFramesSinceReal = 0;
        syntheticThisFrame = false;
        firstPersonOverlayPass = false;
    }

    private static void releaseTargets() {
        if (sceneTarget != null) {
            sceneTarget.m_83930_();
            sceneTarget = null;
        }
        if (preparedHistoryTarget != null) {
            preparedHistoryTarget.m_83930_();
            preparedHistoryTarget = null;
        }
        preparedHistoryValid = false;
        if (motionTarget != null) {
            motionTarget.m_83930_();
            motionTarget = null;
        }
    }

    private static void ensureSceneTarget(int width, int height) {
        if (sceneTarget == null) {
            sceneTarget = new TextureTarget(width, height, true, Minecraft.f_91002_);
            sceneTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            sceneTarget.m_83936_(9729);
            TemporalFrameSystem.invalidateHistory();
            return;
        }
        if (TemporalFrameSystem.sceneTarget.f_83915_ != width || TemporalFrameSystem.sceneTarget.f_83916_ != height) {
            sceneTarget.m_83941_(width, height, Minecraft.f_91002_);
            sceneTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            sceneTarget.m_83936_(9729);
            TemporalFrameSystem.invalidateHistory();
        }
    }

    private static void ensurePreparedHistoryTarget(int width, int height) {
        if (preparedHistoryTarget == null) {
            preparedHistoryTarget = new TextureTarget(width, height, false, Minecraft.f_91002_);
            preparedHistoryTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            preparedHistoryTarget.m_83936_(9729);
            preparedHistoryValid = false;
            return;
        }
        if (TemporalFrameSystem.preparedHistoryTarget.f_83915_ != width || TemporalFrameSystem.preparedHistoryTarget.f_83916_ != height) {
            preparedHistoryTarget.m_83941_(width, height, Minecraft.f_91002_);
            preparedHistoryTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            preparedHistoryTarget.m_83936_(9729);
            preparedHistoryValid = false;
        }
    }

    private static void ensureMotionTarget(int width, int height) {
        if (motionTarget == null) {
            motionTarget = new TextureTarget(width, height, false, Minecraft.f_91002_);
            TemporalFrameSystem.configureMotionTexture(motionTarget);
            return;
        }
        if (TemporalFrameSystem.motionTarget.f_83915_ != width || TemporalFrameSystem.motionTarget.f_83916_ != height) {
            motionTarget.m_83941_(width, height, Minecraft.f_91002_);
            TemporalFrameSystem.configureMotionTexture(motionTarget);
        }
    }

    private static void configureMotionTexture(TextureTarget target) {
        int previousBinding = GL11.glGetInteger((int)32873);
        GL11.glBindTexture((int)3553, (int)target.m_83975_());
        GL11.glTexImage2D((int)3553, (int)0, (int)34842, (int)target.f_83915_, (int)target.f_83916_, (int)0, (int)6408, (int)5131, (ByteBuffer)null);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glBindTexture((int)3553, (int)previousBinding);
        target.m_83936_(9729);
    }

    private static boolean isCameraCut(Camera camera) {
        float pitchDelta;
        Vec3 pos = camera.m_90583_();
        double dx = pos.f_82479_ - previousCameraX;
        double dy = pos.f_82480_ - previousCameraY;
        double dz = pos.f_82481_ - previousCameraZ;
        double maxDistance = (Double)Config.CLIENT.tfrCameraCutDistance.get();
        if (dx * dx + dy * dy + dz * dz > maxDistance * maxDistance) {
            return true;
        }
        float yawDelta = TemporalFrameSystem.wrappedDegrees(camera.m_90590_() - previousYaw);
        double angularDelta = Math.sqrt(yawDelta * yawDelta + (pitchDelta = TemporalFrameSystem.wrappedDegrees(camera.m_90589_() - previousPitch)) * pitchDelta);
        return angularDelta > (Double)Config.CLIENT.tfrCameraCutAngle.get();
    }

    private static boolean hasMeaningfulCameraMotion(Camera camera) {
        Vec3 pos = camera.m_90583_();
        double dx = pos.f_82479_ - previousCameraX;
        double dy = pos.f_82480_ - previousCameraY;
        double dz = pos.f_82481_ - previousCameraZ;
        if (dx * dx + dy * dy + dz * dz > 1.0E-8) {
            return true;
        }
        return Math.abs(TemporalFrameSystem.wrappedDegrees(camera.m_90590_() - previousYaw)) > 0.01f || Math.abs(TemporalFrameSystem.wrappedDegrees(camera.m_90589_() - previousPitch)) > 0.01f;
    }

    private static float wrappedDegrees(float value) {
        if ((value %= 360.0f) >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    private static void buildReprojectionMatrices(Camera camera) {
        Vec3 currentCamera = camera.m_90583_();
        float dx = (float)(previousCameraX - currentCamera.f_82479_);
        float dy = (float)(previousCameraY - currentCamera.f_82480_);
        float dz = (float)(previousCameraZ - currentCamera.f_82481_);
        cameraTranslation.identity().translation(dx, dy, dz);
        geometryReprojection.set((Matrix4fc)currentViewProjection).mul((Matrix4fc)cameraTranslation).mul((Matrix4fc)previousInverseViewProjection);
        skyReprojection.set((Matrix4fc)currentViewProjection).mul((Matrix4fc)previousInverseViewProjection);
    }

    private static boolean prepareLsrHistory() {
        TextureTarget scene = sceneTarget;
        ShaderInstance shader = ShaderManager.getTemporalPrepareShader();
        if (scene == null || shader == null) {
            return false;
        }
        TemporalFrameSystem.ensurePreparedHistoryTarget(scene.f_83915_, scene.f_83916_);
        TextureTarget prepared = preparedHistoryTarget;
        if (prepared == null) {
            return false;
        }
        shader.m_173350_("SceneSampler", (Object)scene.m_83975_());
        shader.m_173350_("SceneDepth", (Object)scene.m_83980_());
        TemporalFrameSystem.setUniform2f(shader, "SourceTexelSize", 1.0f / (float)Math.max(1, scene.f_83915_), 1.0f / (float)Math.max(1, scene.f_83916_));
        TemporalFrameSystem.setUniform1f(shader, "LsrSharpness", LsrSystem.sharpness());
        TemporalFrameSystem.drawFullscreen(shader, (RenderTarget)prepared);
        return true;
    }

    private static boolean renderMotionField(ShaderInstance shader) {
        TextureTarget scene = sceneTarget;
        TextureTarget motion = motionTarget;
        if (scene == null || motion == null) {
            return false;
        }
        shader.m_173350_("HistoryDepth", (Object)scene.m_83980_());
        TemporalFrameSystem.setUniformMatrix(shader, "ReprojectionMat", geometryReprojection);
        TemporalFrameSystem.setUniformMatrix(shader, "SkyReprojectionMat", skyReprojection);
        TemporalFrameSystem.drawFullscreen(shader, (RenderTarget)motion);
        return true;
    }

    private static boolean renderFinalComposite(ShaderInstance shader, RenderTarget destination, boolean syntheticFrame) {
        TextureTarget scene = sceneTarget;
        if (scene == null) {
            return false;
        }
        boolean usePrepared = Config.isFeatureEnabled(Config.CLIENT.lsrEnabled) && preparedHistoryValid && preparedHistoryTarget != null;
        TextureTarget colorSource = usePrepared ? preparedHistoryTarget : scene;
        TextureTarget motion = motionTarget;
        shader.m_173350_("HistoryColor", (Object)colorSource.m_83975_());
        shader.m_173350_("MotionSampler", (Object)(syntheticFrame && motion != null ? motion.m_83975_() : colorSource.m_83975_()));
        TemporalFrameSystem.setUniform2f(shader, "SourceTexelSize", 1.0f / (float)Math.max(1, colorSource.f_83915_), 1.0f / (float)Math.max(1, colorSource.f_83916_));
        TemporalFrameSystem.setUniform1f(shader, "ReprojectionStrength", ((Double)Config.CLIENT.tfrReprojectionStrength.get()).floatValue());
        TemporalFrameSystem.setUniform1i(shader, "SyntheticFrame", syntheticFrame ? 1 : 0);
        TemporalFrameSystem.setUniform1i(shader, "HistoryHasEdgeMetadata", usePrepared ? 1 : 0);
        TemporalFrameSystem.drawFullscreen(shader, destination);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void drawFullscreen(ShaderInstance shader, RenderTarget destination) {
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        int savedTexture0 = RenderSystem.getShaderTexture((int)0);
        int savedTexture1 = RenderSystem.getShaderTexture((int)1);
        int savedTexture2 = RenderSystem.getShaderTexture((int)2);
        float[] savedColor = RenderSystem.getShaderColor();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)destination.f_83917_, (float)destination.f_83918_, 0.0f, 1000.0f, 3000.0f);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
            RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(ortho);
            }
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            GlStateManager._blendFuncSeparate((int)1, (int)0, (int)1, (int)0);
            shader.m_173363_();
            Tesselator tessellator = RenderSystem.renderThreadTesselator();
            BufferBuilder buffer = tessellator.m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_(0.0, (double)destination.f_83918_, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)destination.f_83917_, (double)destination.f_83918_, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)destination.f_83917_, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.depthFunc((int)515);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor((float)savedColor[0], (float)savedColor[1], (float)savedColor[2], (float)savedColor[3]);
            RenderSystem.setShaderTexture((int)0, (int)savedTexture0);
            RenderSystem.setShaderTexture((int)1, (int)savedTexture1);
            RenderSystem.setShaderTexture((int)2, (int)savedTexture2);
            RenderSystem.activeTexture((int)33984);
            destination.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)destination.f_83917_, (int)destination.f_83918_);
        }
    }

    private static void blitSceneFallback(RenderTarget source, RenderTarget destination) {
        GL30.glBindFramebuffer((int)36008, (int)source.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)destination.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)source.f_83915_, (int)source.f_83916_, (int)0, (int)0, (int)destination.f_83915_, (int)destination.f_83916_, (int)16384, (int)(source.f_83915_ == destination.f_83915_ && source.f_83916_ == destination.f_83916_ ? 9728 : 9729));
        destination.m_83947_(true);
    }

    private static void setUniformMatrix(ShaderInstance shader, String name, Matrix4f value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5679_(value);
        }
    }

    private static void setUniform1i(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_142617_(value);
        }
    }

    private static void setUniform1f(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_5985_(value);
        }
    }

    private static void setUniform2f(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.m_173348_(name);
        if (uniform != null) {
            uniform.m_7971_(x, y);
        }
    }

    static {
        previousViewProjection = new Matrix4f();
        previousInverseViewProjection = new Matrix4f();
        currentViewProjection = new Matrix4f();
        currentViewMatrix = new Matrix4f();
        geometryReprojection = new Matrix4f();
        skyReprojection = new Matrix4f();
        cameraTranslation = new Matrix4f();
    }
}

