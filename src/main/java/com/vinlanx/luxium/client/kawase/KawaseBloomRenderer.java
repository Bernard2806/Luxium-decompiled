/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL21
 *  org.lwjgl.system.MemoryUtil
 */
package com.vinlanx.luxium.client.kawase;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.client.SharedPostResources;
import com.vinlanx.luxium.client.kawase.KawaseSourceRegistry;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyFrameCache;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class KawaseBloomRenderer {
    private static final int MAX_LEVELS = 7;
    private static final int MAX_SOURCE_DISTANCE = 32;
    private static final TextureTarget[] DOWN_TARGETS = new TextureTarget[7];
    private static final TextureTarget[] UP_TARGETS = new TextureTarget[7];
    private static TextureTarget sourceTarget;
    private static int sourceAtlas;
    private static ByteBuffer sourcePixels;
    private static int volumeSize;
    private static int atlasSize;
    private static int atlasTilesPerRow;
    private static int volumeMinX;
    private static int volumeMinY;
    private static int volumeMinZ;
    private static int anchorX;
    private static int anchorY;
    private static int anchorZ;
    private static int uploadedRadius;
    private static long uploadedVersion;
    private static int previousSourceAtlasBinding;
    private static boolean sourceAtlasBound;
    private static final Matrix4f VIEW_ROTATION;
    private static final Matrix4f INVERSE_VIEW_PROJECTION;
    private static final Matrix4f INVERSE_PROJECTION;

    private KawaseBloomRenderer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!Config.isFeatureEnabled(Config.CLIENT.kawaseBloomEnabled)) {
            KawaseBloomRenderer.releaseResources();
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        RenderTarget main = mc.m_91385_();
        int depthTexture = SharedPostResources.getDepthTextureId();
        if (mc.f_91073_ == null || mc.f_91074_ == null || main == null || main.f_83917_ <= 0 || main.f_83918_ <= 0 || main.m_83975_() <= 0 || depthTexture <= 0) {
            return;
        }
        ShaderInstance sourceShader = ShaderManager.getKawaseSourceShader();
        ShaderInstance downShader = ShaderManager.getKawaseDownShader();
        ShaderInstance upShader = ShaderManager.getKawaseUpShader();
        ShaderInstance compositeShader = ShaderManager.getKawaseCompositeShader();
        if (sourceShader == null || downShader == null || upShader == null || compositeShader == null) {
            return;
        }
        int levels = Mth.m_14045_((int)((Integer)Config.CLIENT.kawaseBloomLevels.get()), (int)2, (int)7);
        KawaseBloomRenderer.ensureTargets(main.f_83917_, main.f_83918_, levels);
        KawaseBloomRenderer.updateSourceVolume(event.getCamera().m_90583_());
        if (sourceAtlas <= 0 || sourceTarget == null) {
            return;
        }
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        float[] previousColor = RenderSystem.getShaderColor();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.m_85836_();
        modelViewStack.m_166856_();
        modelViewStack.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            TextureTarget output;
            int level;
            Matrix4f viewRotation = VIEW_ROTATION.set((Matrix4fc)event.getPoseStack().m_85850_().m_252922_());
            Matrix4f inverseViewProjection = NeoSkyFrameCache.copyInverseViewProjection(event.getProjectionMatrix(), viewRotation, INVERSE_VIEW_PROJECTION);
            Matrix4f inverseProjection = NeoSkyFrameCache.copyInverseProjection(event.getProjectionMatrix(), INVERSE_PROJECTION);
            Vec3 camera = event.getCamera().m_90583_();
            sourceShader.m_173350_("SceneSampler", (Object)main.m_83975_());
            sourceShader.m_173350_("DepthSampler", (Object)depthTexture);
            KawaseBloomRenderer.setMatrix(sourceShader, "InverseViewProjection", inverseViewProjection);
            KawaseBloomRenderer.setMatrix(sourceShader, "InverseProjection", inverseProjection);
            KawaseBloomRenderer.set3f(sourceShader, "VolumeMinRelative", (float)((double)volumeMinX - camera.f_82479_), (float)((double)volumeMinY - camera.f_82480_), (float)((double)volumeMinZ - camera.f_82481_));
            KawaseBloomRenderer.set1f(sourceShader, "VolumeSize", volumeSize);
            KawaseBloomRenderer.set1f(sourceShader, "SourceAtlasSize", atlasSize);
            KawaseBloomRenderer.set1f(sourceShader, "SourceTilesPerRow", atlasTilesPerRow);
            KawaseBloomRenderer.set1f(sourceShader, "Threshold", ((Double)Config.CLIENT.kawaseBloomThreshold.get()).floatValue());
            sourceTarget.m_83947_(true);
            KawaseBloomRenderer.draw(sourceShader, KawaseBloomRenderer.sourceTarget.f_83917_, KawaseBloomRenderer.sourceTarget.f_83918_, false, () -> KawaseBloomRenderer.bindSourceVolume(sourceShader));
            TextureTarget input = sourceTarget;
            float radius = ((Double)Config.CLIENT.kawaseBloomRadius.get()).floatValue();
            for (level = 0; level < levels; ++level) {
                output = DOWN_TARGETS[level];
                downShader.m_173350_("InputSampler", (Object)input.m_83975_());
                KawaseBloomRenderer.set2f(downShader, "InputTexelSize", 1.0f / (float)input.f_83917_, 1.0f / (float)input.f_83918_);
                KawaseBloomRenderer.set1f(downShader, "Radius", radius);
                output.m_83947_(true);
                KawaseBloomRenderer.draw(downShader, output.f_83917_, output.f_83918_, false, null);
                input = output;
            }
            for (level = levels - 2; level >= 0; --level) {
                output = UP_TARGETS[level];
                TextureTarget high = DOWN_TARGETS[level];
                upShader.m_173350_("LowSampler", (Object)input.m_83975_());
                upShader.m_173350_("HighSampler", (Object)high.m_83975_());
                KawaseBloomRenderer.set2f(upShader, "LowTexelSize", 1.0f / (float)input.f_83917_, 1.0f / (float)input.f_83918_);
                KawaseBloomRenderer.set1f(upShader, "Radius", radius);
                output.m_83947_(true);
                KawaseBloomRenderer.draw(upShader, output.f_83917_, output.f_83918_, false, null);
                input = output;
            }
            compositeShader.m_173350_("GlowSampler", (Object)input.m_83975_());
            compositeShader.m_173350_("DepthSampler", (Object)depthTexture);
            KawaseBloomRenderer.setMatrix(compositeShader, "InverseProjection", inverseProjection);
            KawaseBloomRenderer.set2f(compositeShader, "GlowSize", input.f_83917_, input.f_83918_);
            KawaseBloomRenderer.set1f(compositeShader, "Intensity", ((Double)Config.CLIENT.kawaseBloomIntensity.get()).floatValue());
            KawaseBloomRenderer.set1f(compositeShader, "DepthTolerance", ((Double)Config.CLIENT.kawaseBloomDepthTolerance.get()).floatValue());
            KawaseBloomRenderer.set1i(compositeShader, "DepthOcclusion", (Boolean)Config.CLIENT.kawaseBloomDepthOcclusion.get() != false ? 1 : 0);
            main.m_83947_(true);
            KawaseBloomRenderer.draw(compositeShader, main.f_83917_, main.f_83918_, true, null);
        }
        finally {
            KawaseBloomRenderer.restoreSourceAtlasBinding();
            modelViewStack.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            RenderSystem.setShaderColor((float)previousColor[0], (float)previousColor[1], (float)previousColor[2], (float)previousColor[3]);
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            main.m_83947_(true);
            RenderSystem.viewport((int)0, (int)0, (int)main.f_83917_, (int)main.f_83918_);
        }
    }

    public static void releaseResources() {
        KawaseBloomRenderer.destroyTarget(sourceTarget);
        sourceTarget = null;
        for (int i = 0; i < 7; ++i) {
            KawaseBloomRenderer.destroyTarget(DOWN_TARGETS[i]);
            KawaseBloomRenderer.destroyTarget(UP_TARGETS[i]);
            KawaseBloomRenderer.DOWN_TARGETS[i] = null;
            KawaseBloomRenderer.UP_TARGETS[i] = null;
        }
        if (sourceAtlas > 0) {
            TextureUtil.releaseTextureId((int)sourceAtlas);
            sourceAtlas = -1;
        }
        sourcePixels = null;
        volumeSize = 0;
        atlasSize = 0;
        atlasTilesPerRow = 0;
        uploadedRadius = -1;
        uploadedVersion = Long.MIN_VALUE;
        anchorZ = Integer.MIN_VALUE;
        anchorY = Integer.MIN_VALUE;
        anchorX = Integer.MIN_VALUE;
    }

    private static void ensureTargets(int width, int height, int levels) {
        int i;
        sourceTarget = KawaseBloomRenderer.ensureTarget(sourceTarget, width, height);
        int levelWidth = width;
        int levelHeight = height;
        for (i = 0; i < levels; ++i) {
            levelWidth = Math.max(1, levelWidth / 2);
            levelHeight = Math.max(1, levelHeight / 2);
            KawaseBloomRenderer.DOWN_TARGETS[i] = KawaseBloomRenderer.ensureTarget(DOWN_TARGETS[i], levelWidth, levelHeight);
            if (i >= levels - 1) continue;
            KawaseBloomRenderer.UP_TARGETS[i] = KawaseBloomRenderer.ensureTarget(UP_TARGETS[i], levelWidth, levelHeight);
        }
        for (i = levels; i < 7; ++i) {
            KawaseBloomRenderer.destroyTarget(DOWN_TARGETS[i]);
            KawaseBloomRenderer.DOWN_TARGETS[i] = null;
        }
        for (i = Math.max(0, levels - 1); i < 7; ++i) {
            KawaseBloomRenderer.destroyTarget(UP_TARGETS[i]);
            KawaseBloomRenderer.UP_TARGETS[i] = null;
        }
    }

    private static TextureTarget ensureTarget(TextureTarget target, int width, int height) {
        if (target == null) {
            target = new TextureTarget(width, height, false, Minecraft.f_91002_);
            KawaseBloomRenderer.configureHdrTarget(target);
        } else if (target.f_83915_ != width || target.f_83916_ != height) {
            target.m_83941_(width, height, Minecraft.f_91002_);
            KawaseBloomRenderer.configureHdrTarget(target);
        }
        return target;
    }

    private static void configureHdrTarget(TextureTarget target) {
        int previousActiveTexture = GL11.glGetInteger((int)34016);
        GL13.glActiveTexture((int)33984);
        int previousTexture = GL11.glGetInteger((int)32873);
        GL11.glBindTexture((int)3553, (int)target.m_83975_());
        GL11.glTexImage2D((int)3553, (int)0, (int)34842, (int)target.f_83915_, (int)target.f_83916_, (int)0, (int)6408, (int)5131, (ByteBuffer)null);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glBindTexture((int)3553, (int)previousTexture);
        GL13.glActiveTexture((int)previousActiveTexture);
        target.m_83936_(9729);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void updateSourceVolume(Vec3 camera) {
        boolean moved;
        KawaseSourceRegistry.Snapshot snapshot = KawaseSourceRegistry.get().snapshot();
        int radius = Mth.m_14045_((int)((Integer)Config.CLIENT.kawaseBloomSourceDistance.get()), (int)8, (int)32);
        int cameraX = Mth.m_14107_((double)camera.f_82479_);
        int cameraY = Mth.m_14107_((double)camera.f_82480_);
        int cameraZ = Mth.m_14107_((double)camera.f_82481_);
        boolean bl = moved = anchorX == Integer.MIN_VALUE || Math.abs(cameraX - anchorX) >= 8 || Math.abs(cameraY - anchorY) >= 8 || Math.abs(cameraZ - anchorZ) >= 8;
        if (!moved && uploadedRadius == radius && uploadedVersion == snapshot.version()) {
            return;
        }
        if (moved || uploadedRadius != radius) {
            anchorX = cameraX;
            anchorY = cameraY;
            anchorZ = cameraZ;
        }
        int size = radius * 2 + 3;
        volumeMinX = anchorX - radius - 1;
        volumeMinY = anchorY - radius - 1;
        volumeMinZ = anchorZ - radius - 1;
        KawaseBloomRenderer.ensureSourceVolume(size);
        MemoryUtil.memSet((long)MemoryUtil.memAddress((ByteBuffer)sourcePixels), (int)0, (long)sourcePixels.capacity());
        long[] positions = snapshot.positions();
        byte[] emissions = snapshot.emissions();
        for (int i = 0; i < positions.length; ++i) {
            int x = BlockPos.m_121983_((long)positions[i]) - volumeMinX;
            int y = BlockPos.m_122008_((long)positions[i]) - volumeMinY;
            int z = BlockPos.m_122015_((long)positions[i]) - volumeMinZ;
            if (x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size) continue;
            int tileX = z % atlasTilesPerRow;
            int tileY = z / atlasTilesPerRow;
            int index = (tileY * size + y) * atlasSize + tileX * size + x;
            sourcePixels.put(index, (byte)((emissions[i] & 0xFF) * 17));
        }
        sourcePixels.position(0).limit(sourcePixels.capacity());
        int previousActiveTexture = GL11.glGetInteger((int)34016);
        GL13.glActiveTexture((int)33984);
        int previousTexture = GL11.glGetInteger((int)32873);
        int previousAlignment = GL11.glGetInteger((int)3317);
        int previousRowLength = GL11.glGetInteger((int)3314);
        int previousSkipRows = GL11.glGetInteger((int)3315);
        int previousSkipPixels = GL11.glGetInteger((int)3316);
        int previousUnpackBuffer = GL11.glGetInteger((int)35055);
        try {
            GL21.glBindBuffer((int)35052, (int)0);
            GL11.glBindTexture((int)3553, (int)sourceAtlas);
            GlStateManager._pixelStore((int)3317, (int)1);
            GlStateManager._pixelStore((int)3314, (int)0);
            GlStateManager._pixelStore((int)3315, (int)0);
            GlStateManager._pixelStore((int)3316, (int)0);
            GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)atlasSize, (int)atlasSize, (int)6403, (int)5121, (ByteBuffer)sourcePixels);
        }
        finally {
            GlStateManager._pixelStore((int)3317, (int)previousAlignment);
            GlStateManager._pixelStore((int)3314, (int)previousRowLength);
            GlStateManager._pixelStore((int)3315, (int)previousSkipRows);
            GlStateManager._pixelStore((int)3316, (int)previousSkipPixels);
            GL11.glBindTexture((int)3553, (int)previousTexture);
            GL21.glBindBuffer((int)35052, (int)previousUnpackBuffer);
            GL13.glActiveTexture((int)previousActiveTexture);
        }
        uploadedRadius = radius;
        uploadedVersion = snapshot.version();
    }

    private static void ensureSourceVolume(int size) {
        if (sourceAtlas > 0 && volumeSize == size && sourcePixels != null) {
            return;
        }
        if (sourceAtlas > 0) {
            TextureUtil.releaseTextureId((int)sourceAtlas);
        }
        sourceAtlas = TextureUtil.generateTextureId();
        volumeSize = size;
        atlasTilesPerRow = Mth.m_14167_((float)((float)Math.sqrt(size)));
        atlasSize = atlasTilesPerRow * size;
        sourcePixels = BufferUtils.createByteBuffer((int)(atlasSize * atlasSize));
        int previousActiveTexture = GL11.glGetInteger((int)34016);
        GL13.glActiveTexture((int)33984);
        int previousTexture = GL11.glGetInteger((int)32873);
        GL11.glBindTexture((int)3553, (int)sourceAtlas);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)33321, (int)atlasSize, (int)atlasSize, (int)0, (int)6403, (int)5121, (ByteBuffer)null);
        GL11.glBindTexture((int)3553, (int)previousTexture);
        GL13.glActiveTexture((int)previousActiveTexture);
    }

    private static void bindSourceVolume(ShaderInstance shader) {
        int previousActiveTexture = GL11.glGetInteger((int)34016);
        GL13.glActiveTexture((int)33991);
        previousSourceAtlasBinding = GL11.glGetInteger((int)32873);
        GL11.glBindTexture((int)3553, (int)sourceAtlas);
        sourceAtlasBound = true;
        int location = GL20.glGetUniformLocation((int)shader.m_108943_(), (CharSequence)"SourceAtlas");
        if (location >= 0) {
            GL20.glUniform1i((int)location, (int)7);
        }
        GL13.glActiveTexture((int)previousActiveTexture);
    }

    private static void restoreSourceAtlasBinding() {
        if (!sourceAtlasBound) {
            return;
        }
        int previousActiveTexture = GL11.glGetInteger((int)34016);
        GL13.glActiveTexture((int)33991);
        GL11.glBindTexture((int)3553, (int)previousSourceAtlasBinding);
        GL13.glActiveTexture((int)previousActiveTexture);
        sourceAtlasBound = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void draw(ShaderInstance shader, int width, int height, boolean additive, Runnable afterApply) {
        Matrix4f projection = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix((Matrix4f)projection, (VertexSorting)VertexSorting.f_276633_);
        RenderSystem.viewport((int)0, (int)0, (int)width, (int)height);
        if (shader.f_173308_ != null) {
            shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
        }
        if (shader.f_173309_ != null) {
            shader.f_173309_.m_5679_(projection);
        }
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask((boolean)false);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        shader.m_173363_();
        try {
            if (afterApply != null) {
                afterApply.run();
            }
            if (additive) {
                RenderSystem.enableBlend();
                GlStateManager._blendFuncSeparate((int)1, (int)1, (int)0, (int)1);
            } else {
                RenderSystem.disableBlend();
            }
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
        }
    }

    private static void set1f(ShaderInstance shader, String name, float value) {
        if (shader.m_173348_(name) != null) {
            shader.m_173348_(name).m_5985_(value);
        }
    }

    private static void set1i(ShaderInstance shader, String name, int value) {
        if (shader.m_173348_(name) != null) {
            shader.m_173348_(name).m_142617_(value);
        }
    }

    private static void set2f(ShaderInstance shader, String name, float x, float y) {
        if (shader.m_173348_(name) != null) {
            shader.m_173348_(name).m_7971_(x, y);
        }
    }

    private static void set3f(ShaderInstance shader, String name, float x, float y, float z) {
        if (shader.m_173348_(name) != null) {
            shader.m_173348_(name).m_5889_(x, y, z);
        }
    }

    private static void setMatrix(ShaderInstance shader, String name, Matrix4f matrix) {
        if (shader.m_173348_(name) != null) {
            shader.m_173348_(name).m_5679_(matrix);
        }
    }

    private static void destroyTarget(TextureTarget target) {
        if (target != null) {
            target.m_83930_();
        }
    }

    static {
        sourceAtlas = -1;
        anchorX = Integer.MIN_VALUE;
        anchorY = Integer.MIN_VALUE;
        anchorZ = Integer.MIN_VALUE;
        uploadedRadius = -1;
        uploadedVersion = Long.MIN_VALUE;
        VIEW_ROTATION = new Matrix4f();
        INVERSE_VIEW_PROJECTION = new Matrix4f();
        INVERSE_PROJECTION = new Matrix4f();
    }
}

