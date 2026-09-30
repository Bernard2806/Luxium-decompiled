/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.shaders.FogShape
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexBuffer
 *  com.mojang.blaze3d.vertex.VertexBuffer$Usage
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.CloudStatus
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.FogRenderer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.util.FastColor$ABGR32
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.slf4j.Logger
 */
package com.vinlanx.luxium.client.clouds;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class LuxiumCloudRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LuxiumCloudRenderer INSTANCE = new LuxiumCloudRenderer();
    private static final ResourceLocation VANILLA_CLOUDS = ResourceLocation.withDefaultNamespace((String)"textures/environment/clouds.png");
    private static final int TEXTURE_SIZE = 256;
    private static final float UV_SCALE = 0.00390625f;
    private static final float EPSILON = 9.765625E-4f;
    private static final int TILE_SIZE = 8;
    private static final int MAX_RADIUS_TEXELS = 128;
    private static final float SHADOW_FOOTPRINT_PADDING = 1.0f;
    private static final float CLOUD_PROJECTION_MARGIN = 48.0f;
    private static final int[] PATTERN_OFFSET_X = new int[]{0, 73, 181};
    private static final int[] PATTERN_OFFSET_Z = new int[]{0, 151, 47};
    private final LayerState[] layers = new LayerState[]{new LayerState(0), new LayerState(1), new LayerState(2)};
    private final LayerState vanillaMaskLayer = new LayerState(-1);
    private boolean sourceLoaded;
    private boolean sourceLoadFailed;
    private boolean[] sourceOpaque;
    private int[] sourcePixels;
    private int[] distanceToOpaque;
    private int[] distanceToClear;
    private int[] nearestOpaquePixel;
    private TextureTarget occlusionTarget;

    private LuxiumCloudRenderer() {
    }

    public static LuxiumCloudRenderer get() {
        return INSTANCE;
    }

    public void invalidateResources() {
        this.sourceLoaded = false;
        this.sourceLoadFailed = false;
        this.sourceOpaque = null;
        this.sourcePixels = null;
        this.distanceToOpaque = null;
        this.distanceToClear = null;
        this.nearestOpaquePixel = null;
        for (LayerState state : this.layers) {
            state.textureKey = Long.MIN_VALUE;
            state.geometryKey = Long.MIN_VALUE;
            state.maskGeometryKey = Long.MIN_VALUE;
        }
        this.vanillaMaskLayer.maskGeometryKey = Long.MIN_VALUE;
    }

    public void invalidateGeometry() {
        for (LayerState state : this.layers) {
            state.geometryKey = Long.MIN_VALUE;
            state.maskGeometryKey = Long.MIN_VALUE;
        }
        this.vanillaMaskLayer.maskGeometryKey = Long.MIN_VALUE;
    }

    public boolean render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, boolean cloudShadowPass) {
        Minecraft mc = Minecraft.m_91087_();
        ClientLevel level = mc.f_91073_;
        if (level == null || mc.f_91074_ == null) {
            return false;
        }
        float dimensionCloudHeight = level.m_104583_().m_108871_();
        if (Float.isNaN(dimensionCloudHeight)) {
            return true;
        }
        CloudStatus cloudStatus = mc.f_91066_.m_92174_();
        if (cloudStatus == CloudStatus.OFF) {
            return true;
        }
        LayerConfig[] config = LuxiumCloudRenderer.readConfig();
        boolean anyEnabled = false;
        for (LayerConfig layer : config) {
            anyEnabled |= layer.enabled && layer.opacity > 0.0;
        }
        if (!anyEnabled) {
            return true;
        }
        float weather = Mth.m_14036_((float)Math.max(level.m_46722_(partialTick), level.m_46661_(partialTick)), (float)0.0f, (float)1.0f);
        int weatherStep = Mth.m_14045_((int)Math.round(weather * 20.0f), (int)0, (int)20);
        float textureWeather = (float)weatherStep / 20.0f;
        this.ensureSourceTexture();
        if (cloudShadowPass) {
            this.renderShadowPass(poseStack, projectionMatrix, partialTick, cameraX, cameraY, cameraZ, ticks, cloudStatus, textureWeather, config);
        } else {
            this.renderColorPass(level, poseStack, projectionMatrix, partialTick, cameraX, cameraY, cameraZ, ticks, cloudStatus, weather, textureWeather, config);
        }
        return true;
    }

    public boolean renderCustomShadowCasters(PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f lightViewRotation, float projectionRadius, Vec3 toLight, float partialTick, double centerX, double centerY, double centerZ, int ticks) {
        if (!Config.isFeatureEnabled(Config.CLIENT.cloudsEnabled)) {
            return false;
        }
        Minecraft mc = Minecraft.m_91087_();
        ClientLevel level = mc.f_91073_;
        if (level == null || mc.f_91074_ == null || mc.f_91066_.m_92174_() == CloudStatus.OFF) {
            return true;
        }
        float dimensionCloudHeight = level.m_104583_().m_108871_();
        if (Float.isNaN(dimensionCloudHeight)) {
            return true;
        }
        CloudStatus cloudStatus = mc.f_91066_.m_92174_();
        LayerConfig[] config = LuxiumCloudRenderer.readConfig();
        float weather = Mth.m_14036_((float)Math.max(level.m_46722_(partialTick), level.m_46661_(partialTick)), (float)0.0f, (float)1.0f);
        int weatherStep = Mth.m_14045_((int)Math.round(weather * 20.0f), (int)0, (int)20);
        float textureWeather = (float)weatherStep / 20.0f;
        this.ensureSourceTexture();
        ShaderInstance shadowShader = ShaderManager.getCloudShadowCasterShader();
        if (shadowShader != null) {
            RenderSystem.setShader(() -> shadowShader);
        } else {
            RenderSystem.setShader(GameRenderer::m_172838_);
        }
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        Vec3 center = new Vec3(centerX, centerY, centerZ);
        for (int i = 0; i < this.layers.length; ++i) {
            double casterY;
            ShadowPlane plane;
            LayerConfig layer = config[i];
            if (!layer.enabled || layer.opacity <= 0.0 || (plane = LuxiumCloudRenderer.buildShadowPlane(center, toLight, lightViewRotation, projectionRadius, casterY = LuxiumCloudRenderer.shadowCasterHeight(layer, cloudStatus))) == null || plane.maxFront <= 0.0) continue;
            ResourceLocation texture = this.textureForLayer(i, layer, textureWeather);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)texture);
            this.uploadShadowPlane(this.layers[i], plane, i, layer, partialTick, centerX, centerZ, ticks);
            LayerState state = this.layers[i];
            if (state.shadowVertexBuffer == null) continue;
            state.shadowVertexBuffer.m_85921_();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                state.shadowVertexBuffer.m_253207_(poseStack.m_85850_().m_252922_(), projectionMatrix, shader);
            }
            VertexBuffer.m_85931_();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        RenderSystem.enableCull();
        return true;
    }

    public float computeCustomShadowCasterFront(Vec3 center, Vec3 toLight, Matrix4f lightViewRotation, float projectionRadius, CloudStatus cloudStatus) {
        if (!Config.isFeatureEnabled(Config.CLIENT.cloudsEnabled) || cloudStatus == CloudStatus.OFF || toLight == null || toLight.f_82480_ <= 1.0E-6) {
            return Float.NaN;
        }
        LayerConfig[] config = LuxiumCloudRenderer.readConfig();
        double maxFront = Double.NEGATIVE_INFINITY;
        for (LayerConfig layer : config) {
            ShadowPlane plane;
            if (!layer.enabled || layer.opacity <= 0.0 || (plane = LuxiumCloudRenderer.buildShadowPlane(center, toLight, lightViewRotation, projectionRadius, LuxiumCloudRenderer.shadowCasterHeight(layer, cloudStatus))) == null || !Double.isFinite(plane.maxFront)) continue;
            maxFront = Math.max(maxFront, plane.maxFront);
        }
        return Double.isFinite(maxFront) && maxFront > 0.0 ? (float)maxFront : Float.NaN;
    }

    public int renderOcclusionMask(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, int width, int height) {
        if (width <= 0 || height <= 0) {
            return 0;
        }
        Minecraft mc = Minecraft.m_91087_();
        ClientLevel level = mc.f_91073_;
        if (level == null || mc.f_91074_ == null) {
            return 0;
        }
        float dimensionCloudHeight = level.m_104583_().m_108871_();
        CloudStatus cloudStatus = mc.f_91066_.m_92174_();
        if (Float.isNaN(dimensionCloudHeight) || cloudStatus == CloudStatus.OFF) {
            return 0;
        }
        boolean customClouds = Config.isFeatureEnabled(Config.CLIENT.cloudsEnabled);
        LayerConfig[] config = null;
        float textureWeather = 0.0f;
        if (customClouds) {
            config = LuxiumCloudRenderer.readConfig();
            boolean anyEnabled = false;
            for (LayerConfig layer : config) {
                anyEnabled |= layer.enabled && layer.opacity > 0.0;
            }
            if (!anyEnabled) {
                return 0;
            }
            float weather = Mth.m_14036_((float)Math.max(level.m_46722_(partialTick), level.m_46661_(partialTick)), (float)0.0f, (float)1.0f);
            int weatherStep = Mth.m_14045_((int)Math.round(weather * 20.0f), (int)0, (int)20);
            textureWeather = (float)weatherStep / 20.0f;
            this.ensureSourceTexture();
        }
        this.ensureOcclusionTarget(width, height);
        if (this.occlusionTarget == null) {
            return 0;
        }
        this.occlusionTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        this.occlusionTarget.m_83954_(Minecraft.f_91002_);
        this.occlusionTarget.m_83947_(true);
        RenderSystem.viewport((int)0, (int)0, (int)width, (int)height);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::m_172838_);
        if (customClouds) {
            this.renderCustomOcclusionMask(poseStack, projectionMatrix, partialTick, cameraX, cameraY, cameraZ, ticks, cloudStatus, textureWeather, config);
        } else {
            this.renderVanillaOcclusionMask(poseStack, projectionMatrix, partialTick, cameraX, cameraY, cameraZ, ticks, cloudStatus, dimensionCloudHeight);
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        return this.occlusionTarget.m_83975_();
    }

    private void renderCustomOcclusionMask(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, CloudStatus cloudStatus, float textureWeather, LayerConfig[] config) {
        for (int i = 0; i < this.layers.length; ++i) {
            MaskLayerFrame frame;
            LayerConfig layer = config[i];
            if (!layer.enabled || layer.opacity <= 0.0 || (frame = this.prepareMaskLayer(i, layer, cloudStatus, partialTick, cameraX, cameraY, cameraZ, ticks, textureWeather)) == null) continue;
            Matrix4f cloudProjection = LuxiumCloudRenderer.cloudProjectionMatrix(projectionMatrix, layer, cloudStatus, cameraY);
            LuxiumCloudRenderer.drawOcclusionMaskLayer(poseStack, cloudProjection, this.layers[i], frame, (float)(layer.opacity / 100.0));
        }
    }

    private void renderVanillaOcclusionMask(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, CloudStatus cloudStatus, float cloudHeight) {
        MaskLayerFrame frame = this.prepareVanillaMaskLayer(cloudStatus, partialTick, cameraX, cameraY, cameraZ, ticks, cloudHeight);
        if (frame != null) {
            LuxiumCloudRenderer.drawOcclusionMaskLayer(poseStack, projectionMatrix, this.vanillaMaskLayer, frame, 0.8f);
        }
    }

    private static void drawOcclusionMaskLayer(PoseStack poseStack, Matrix4f projectionMatrix, LayerState state, MaskLayerFrame frame, float opacity) {
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)frame.texture);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)opacity);
        poseStack.m_85836_();
        poseStack.m_85841_(frame.blockScale, 1.0f, frame.blockScale);
        poseStack.m_252880_(-frame.fractionX, frame.relativeY, -frame.fractionZ);
        if (state.maskVertexBuffer != null) {
            state.maskVertexBuffer.m_85921_();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                state.maskVertexBuffer.m_253207_(poseStack.m_85850_().m_252922_(), projectionMatrix, shader);
            }
            VertexBuffer.m_85931_();
        }
        poseStack.m_85849_();
    }

    private void renderColorPass(ClientLevel level, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, CloudStatus cloudStatus, float weather, float textureWeather, LayerConfig[] config) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask((boolean)true);
        FogRenderer.m_109036_();
        float savedFogStart = RenderSystem.getShaderFogStart();
        float savedFogEnd = RenderSystem.getShaderFogEnd();
        FogShape savedFogShape = RenderSystem.getShaderFogShape();
        boolean ordinaryDistanceFog = LuxiumCloudRenderer.isOrdinaryTerrainDistanceFog(savedFogEnd, savedFogShape);
        Vec3 cloudColor = level.m_104808_(partialTick);
        for (int i = 0; i < this.layers.length; ++i) {
            LayerFrame frame;
            LayerConfig layer = config[i];
            if (!layer.enabled || layer.opacity <= 0.0 || (frame = this.prepareLayer(i, layer, cloudStatus, partialTick, cameraX, cameraY, cameraZ, ticks, textureWeather)) == null) continue;
            Matrix4f cloudProjection = LuxiumCloudRenderer.cloudProjectionMatrix(projectionMatrix, layer, cloudStatus, cameraY);
            if (ordinaryDistanceFog) {
                LuxiumCloudRenderer.applyLayerDistanceFog(layer, cloudStatus, cameraY);
            }
            float stormFactor = 1.0f - weather * (float)(layer.stormDarkening / 100.0) * 0.65f;
            float brightness = (float)layer.brightness * Math.max(0.05f, stormFactor);
            float red = (float)cloudColor.f_82479_ * brightness;
            float green = (float)cloudColor.f_82480_ * brightness;
            float blue = (float)cloudColor.f_82481_ * brightness;
            float alpha = (float)(layer.opacity / 100.0);
            RenderSystem.setShader(GameRenderer::m_172838_);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)frame.texture);
            RenderSystem.setShaderColor((float)red, (float)green, (float)blue, (float)alpha);
            poseStack.m_85836_();
            poseStack.m_85841_(frame.blockScale, 1.0f, frame.blockScale);
            poseStack.m_252880_(-frame.fractionX, frame.fractionY, -frame.fractionZ);
            LayerState state = this.layers[i];
            if (state.vertexBuffer != null) {
                ShaderInstance shader;
                state.vertexBuffer.m_85921_();
                if (cloudStatus == CloudStatus.FANCY) {
                    RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
                    shader = RenderSystem.getShader();
                    if (shader != null) {
                        state.vertexBuffer.m_253207_(poseStack.m_85850_().m_252922_(), cloudProjection, shader);
                    }
                }
                RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
                shader = RenderSystem.getShader();
                if (shader != null) {
                    state.vertexBuffer.m_253207_(poseStack.m_85850_().m_252922_(), cloudProjection, shader);
                }
                VertexBuffer.m_85931_();
            }
            poseStack.m_85849_();
        }
        RenderSystem.setShaderFogStart((float)savedFogStart);
        RenderSystem.setShaderFogEnd((float)savedFogEnd);
        RenderSystem.setShaderFogShape((FogShape)savedFogShape);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void renderShadowPass(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, CloudStatus cloudStatus, float textureWeather, LayerConfig[] config) {
        ShaderInstance shadowShader = ShaderManager.getCloudShadowCasterShader();
        if (shadowShader != null) {
            RenderSystem.setShader(() -> shadowShader);
        } else {
            RenderSystem.setShader(GameRenderer::m_172838_);
        }
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        for (int i = 0; i < this.layers.length; ++i) {
            LayerFrame frame;
            LayerConfig layer = config[i];
            if (!layer.enabled || layer.opacity <= 0.0 || (frame = this.prepareLayer(i, layer, cloudStatus, partialTick, cameraX, cameraY, cameraZ, ticks, textureWeather)) == null) continue;
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)frame.texture);
            poseStack.m_85836_();
            poseStack.m_85841_(frame.blockScale, 1.0f, frame.blockScale);
            poseStack.m_252880_(-frame.fractionX, frame.fractionY, -frame.fractionZ);
            LayerState state = this.layers[i];
            if (state.vertexBuffer != null) {
                state.vertexBuffer.m_85921_();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    state.vertexBuffer.m_253207_(poseStack.m_85850_().m_252922_(), projectionMatrix, shader);
                }
                VertexBuffer.m_85931_();
            }
            poseStack.m_85849_();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        RenderSystem.enableCull();
    }

    private LayerFrame prepareLayer(int index, LayerConfig layer, CloudStatus cloudStatus, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, float weather) {
        float blockScale = (float)(12.0 * layer.scale);
        if (blockScale <= 0.0f) {
            return null;
        }
        double time = ((double)ticks + (double)partialTick) * 0.03 * layer.speed;
        double radians = Math.toRadians(layer.direction);
        double windX = Math.cos(radians) * time;
        double windZ = Math.sin(radians) * time;
        double cloudX = (cameraX + windX) / (double)blockScale + (double)PATTERN_OFFSET_X[index];
        double cloudZ = (cameraZ + windZ) / (double)blockScale + (double)PATTERN_OFFSET_Z[index] + 0.33;
        int absoluteCellX = Mth.m_14107_((double)cloudX);
        int absoluteCellZ = Mth.m_14107_((double)cloudZ);
        int textureCellX = Math.floorMod(absoluteCellX, 256);
        int textureCellZ = Math.floorMod(absoluteCellZ, 256);
        float fractionX = (float)(cloudX - Math.floor(cloudX));
        float fractionZ = (float)(cloudZ - Math.floor(cloudZ));
        double relativeY = layer.height - cameraY + 0.33;
        double thickness = Math.max(0.5, layer.thickness);
        int yCell = Mth.m_14107_((double)(relativeY / thickness));
        float yBase = (float)((double)yCell * thickness);
        float fractionY = (float)(relativeY - (double)yBase);
        int radiusTexels = LuxiumCloudRenderer.layerRadiusTexels(layer);
        long geometryKey = LuxiumCloudRenderer.geometryKey(textureCellX, textureCellZ, yCell, radiusTexels, cloudStatus, layer.thickness);
        LayerState state = this.layers[index];
        if (state.geometryKey != geometryKey || state.vertexBuffer == null) {
            this.rebuildGeometry(state, textureCellX, textureCellZ, yBase, (float)thickness, radiusTexels, cloudStatus);
            state.geometryKey = geometryKey;
        }
        ResourceLocation texture = this.textureForLayer(index, layer, weather);
        return new LayerFrame(blockScale, fractionX, fractionY, fractionZ, texture);
    }

    private MaskLayerFrame prepareMaskLayer(int index, LayerConfig layer, CloudStatus cloudStatus, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, float weather) {
        double bottomY;
        float blockScale = (float)(12.0 * layer.scale);
        if (blockScale <= 0.0f) {
            return null;
        }
        double time = ((double)ticks + (double)partialTick) * 0.03 * layer.speed;
        double radians = Math.toRadians(layer.direction);
        double windX = Math.cos(radians) * time;
        double windZ = Math.sin(radians) * time;
        double cloudX = (cameraX + windX) / (double)blockScale + (double)PATTERN_OFFSET_X[index];
        double cloudZ = (cameraZ + windZ) / (double)blockScale + (double)PATTERN_OFFSET_Z[index] + 0.33;
        int absoluteCellX = Mth.m_14107_((double)cloudX);
        int absoluteCellZ = Mth.m_14107_((double)cloudZ);
        int textureCellX = Math.floorMod(absoluteCellX, 256);
        int textureCellZ = Math.floorMod(absoluteCellZ, 256);
        float fractionX = (float)(cloudX - Math.floor(cloudX));
        float fractionZ = (float)(cloudZ - Math.floor(cloudZ));
        double thickness = Math.max(0.5, layer.thickness);
        double maskY = bottomY = layer.height + 0.33;
        if (cloudStatus == CloudStatus.FANCY) {
            double topY = bottomY + thickness;
            if (cameraY >= topY) {
                maskY = topY;
            } else if (cameraY > bottomY) {
                maskY = cameraY - bottomY <= topY - cameraY ? bottomY : topY;
            }
        }
        float relativeY = (float)(maskY - cameraY);
        int radiusTexels = LuxiumCloudRenderer.layerRadiusTexels(layer);
        long maskKey = LuxiumCloudRenderer.geometryKey(textureCellX, textureCellZ, 0, radiusTexels, cloudStatus, 1.0);
        LayerState state = this.layers[index];
        if (state.maskGeometryKey != maskKey || state.maskVertexBuffer == null) {
            this.rebuildMaskGeometry(state, textureCellX, textureCellZ, radiusTexels, cloudStatus);
            state.maskGeometryKey = maskKey;
        }
        ResourceLocation texture = this.textureForLayer(index, layer, weather);
        return new MaskLayerFrame(blockScale, fractionX, relativeY, fractionZ, texture);
    }

    private MaskLayerFrame prepareVanillaMaskLayer(CloudStatus cloudStatus, float partialTick, double cameraX, double cameraY, double cameraZ, int ticks, float cloudHeight) {
        double bottomY;
        float blockScale = 12.0f;
        float thickness = 4.0f;
        int radiusTexels = 32;
        double time = ((double)ticks + (double)partialTick) * 0.03;
        double cloudX = (cameraX + time) / 12.0;
        double cloudZ = cameraZ / 12.0 + 0.33;
        int absoluteCellX = Mth.m_14107_((double)cloudX);
        int absoluteCellZ = Mth.m_14107_((double)cloudZ);
        int textureCellX = Math.floorMod(absoluteCellX, 256);
        int textureCellZ = Math.floorMod(absoluteCellZ, 256);
        float fractionX = (float)(cloudX - Math.floor(cloudX));
        float fractionZ = (float)(cloudZ - Math.floor(cloudZ));
        double maskY = bottomY = (double)cloudHeight + 0.33;
        if (cloudStatus == CloudStatus.FANCY) {
            double topY = bottomY + 4.0 - 9.765625E-4;
            if (cameraY >= topY) {
                maskY = topY;
            } else if (cameraY > bottomY) {
                maskY = cameraY - bottomY <= topY - cameraY ? bottomY : topY;
            }
        }
        float relativeY = (float)(maskY - cameraY);
        long maskKey = LuxiumCloudRenderer.geometryKey(textureCellX, textureCellZ, 0, 32, cloudStatus, 1.0);
        if (this.vanillaMaskLayer.maskGeometryKey != maskKey || this.vanillaMaskLayer.maskVertexBuffer == null) {
            this.rebuildMaskGeometry(this.vanillaMaskLayer, textureCellX, textureCellZ, 32, cloudStatus);
            this.vanillaMaskLayer.maskGeometryKey = maskKey;
        }
        return new MaskLayerFrame(12.0f, fractionX, relativeY, fractionZ, VANILLA_CLOUDS);
    }

    private static double shadowCasterHeight(LayerConfig layer, CloudStatus cloudStatus) {
        double casterY = layer.height + 0.33;
        if (cloudStatus == CloudStatus.FANCY) {
            casterY += Math.max(0.5, layer.thickness) - 9.765625E-4;
        }
        return casterY;
    }

    private static ShadowPlane buildShadowPlane(Vec3 center, Vec3 toLight, Matrix4f lightViewRotation, float projectionRadius, double cloudY) {
        if (center == null || toLight == null || lightViewRotation == null || !Double.isFinite(cloudY) || toLight.f_82480_ <= 1.0E-6) {
            return null;
        }
        float radius = Math.max(1.0f, projectionRadius) + 1.0f;
        Matrix4f inverseLightRotation = new Matrix4f((Matrix4fc)lightViewRotation).invert();
        Vector3f[] corners = new Vector3f[4];
        float[] lightX = new float[]{-radius, radius, radius, -radius};
        float[] lightY = new float[]{-radius, -radius, radius, radius};
        double maxFront = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < corners.length; ++i) {
            Vector3f base = new Vector3f(lightX[i], lightY[i], 0.0f);
            inverseLightRotation.transformDirection(base);
            double front = (cloudY - center.f_82480_ - (double)base.y) / toLight.f_82480_;
            maxFront = Math.max(maxFront, front);
            base.add((float)(toLight.f_82479_ * front), (float)(toLight.f_82480_ * front), (float)(toLight.f_82481_ * front));
            corners[i] = base;
        }
        return new ShadowPlane(corners[0], corners[1], corners[2], corners[3], maxFront);
    }

    private void uploadShadowPlane(LayerState state, ShadowPlane plane, int layerIndex, LayerConfig layer, float partialTick, double centerX, double centerZ, int ticks) {
        float blockScale = (float)(12.0 * layer.scale);
        if (blockScale <= 0.0f) {
            return;
        }
        double time = ((double)ticks + (double)partialTick) * 0.03 * layer.speed;
        double radians = Math.toRadians(layer.direction);
        double windX = Math.cos(radians) * time;
        double windZ = Math.sin(radians) * time;
        double centerCloudX = (centerX + windX) / (double)blockScale + (double)PATTERN_OFFSET_X[layerIndex];
        double centerCloudZ = (centerZ + windZ) / (double)blockScale + (double)PATTERN_OFFSET_Z[layerIndex] + 0.33;
        double uPeriodBase = Math.floor(centerCloudX / 256.0) * 256.0;
        double vPeriodBase = Math.floor(centerCloudZ / 256.0) * 256.0;
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85822_);
        LuxiumCloudRenderer.shadowVertex(builder, plane.p0, centerX, centerZ, windX, windZ, blockScale, layerIndex, uPeriodBase, vPeriodBase);
        LuxiumCloudRenderer.shadowVertex(builder, plane.p1, centerX, centerZ, windX, windZ, blockScale, layerIndex, uPeriodBase, vPeriodBase);
        LuxiumCloudRenderer.shadowVertex(builder, plane.p2, centerX, centerZ, windX, windZ, blockScale, layerIndex, uPeriodBase, vPeriodBase);
        LuxiumCloudRenderer.shadowVertex(builder, plane.p3, centerX, centerZ, windX, windZ, blockScale, layerIndex, uPeriodBase, vPeriodBase);
        BufferBuilder.RenderedBuffer rendered = builder.m_231175_();
        if (state.shadowVertexBuffer == null) {
            state.shadowVertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        state.shadowVertexBuffer.m_85921_();
        state.shadowVertexBuffer.m_231221_(rendered);
        VertexBuffer.m_85931_();
    }

    private static void shadowVertex(BufferBuilder builder, Vector3f point, double centerX, double centerZ, double windX, double windZ, float blockScale, int layerIndex, double uPeriodBase, double vPeriodBase) {
        double worldX = centerX + (double)point.x;
        double worldZ = centerZ + (double)point.z;
        float u = (float)(((worldX + windX) / (double)blockScale + (double)PATTERN_OFFSET_X[layerIndex] - uPeriodBase) * 0.00390625);
        float v = (float)(((worldZ + windZ) / (double)blockScale + (double)PATTERN_OFFSET_Z[layerIndex] + 0.33 - vPeriodBase) * 0.00390625);
        builder.m_5483_((double)point.x, (double)point.y, (double)point.z).m_7421_(u, v).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
    }

    private void rebuildMaskGeometry(LayerState state, int centerCellX, int centerCellZ, int radiusTexels, CloudStatus cloudStatus) {
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        RenderSystem.setShader(GameRenderer::m_172838_);
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85822_);
        if (cloudStatus == CloudStatus.FANCY) {
            LuxiumCloudRenderer.buildFancyOcclusionPlane(builder, centerCellX, centerCellZ, radiusTexels);
        } else {
            LuxiumCloudRenderer.buildFast(builder, centerCellX, centerCellZ, 0.0f, radiusTexels);
        }
        BufferBuilder.RenderedBuffer rendered = builder.m_231175_();
        if (state.maskVertexBuffer == null) {
            state.maskVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        state.maskVertexBuffer.m_85921_();
        state.maskVertexBuffer.m_231221_(rendered);
        VertexBuffer.m_85931_();
    }

    private void rebuildGeometry(LayerState state, int centerCellX, int centerCellZ, float yBase, float thickness, int radiusTexels, CloudStatus cloudStatus) {
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        RenderSystem.setShader(GameRenderer::m_172838_);
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85822_);
        if (cloudStatus == CloudStatus.FANCY) {
            LuxiumCloudRenderer.buildFancy(builder, centerCellX, centerCellZ, yBase, thickness, radiusTexels);
        } else {
            LuxiumCloudRenderer.buildFast(builder, centerCellX, centerCellZ, yBase, radiusTexels);
        }
        BufferBuilder.RenderedBuffer rendered = builder.m_231175_();
        if (state.vertexBuffer == null) {
            state.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        state.vertexBuffer.m_85921_();
        state.vertexBuffer.m_231221_(rendered);
        VertexBuffer.m_85931_();
    }

    private static void buildFancyOcclusionPlane(BufferBuilder builder, int centerCellX, int centerCellZ, int radiusTexels) {
        int start = -radiusTexels + 8;
        int end = radiusTexels;
        for (int x0i = start; x0i <= end; x0i += 8) {
            for (int z0i = start; z0i <= end; z0i += 8) {
                float x0 = x0i;
                float x1 = x0 + 8.0f;
                float z0 = z0i;
                float z1 = z0 + 8.0f;
                float u0 = ((float)centerCellX + x0) * 0.00390625f;
                float u1 = ((float)centerCellX + x1) * 0.00390625f;
                float v0 = ((float)centerCellZ + z0) * 0.00390625f;
                float v1 = ((float)centerCellZ + z1) * 0.00390625f;
                LuxiumCloudRenderer.quad(builder, x0, 0.0f, z1, u0, v1, x1, 0.0f, z1, u1, v1, x1, 0.0f, z0, u1, v0, x0, 0.0f, z0, u0, v0, 1.0f, 0.0f, 1.0f, 0.0f);
            }
        }
    }

    private static void buildFancy(BufferBuilder builder, int centerCellX, int centerCellZ, float yBase, float thickness, int radiusTexels) {
        boolean drawBottom = yBase > -(thickness + 1.0f);
        boolean drawTop = yBase <= thickness + 1.0f;
        int start = -radiusTexels + 8;
        int end = radiusTexels;
        for (int x0i = start; x0i <= end; x0i += 8) {
            for (int z0i = start; z0i <= end; z0i += 8) {
                float v;
                float z;
                float u;
                float x;
                float x0 = x0i;
                float x1 = x0 + 8.0f;
                float z0 = z0i;
                float z1 = z0 + 8.0f;
                float u0 = ((float)centerCellX + x0) * 0.00390625f;
                float u1 = ((float)centerCellX + x1) * 0.00390625f;
                float v0 = ((float)centerCellZ + z0) * 0.00390625f;
                float v1 = ((float)centerCellZ + z1) * 0.00390625f;
                if (drawBottom) {
                    LuxiumCloudRenderer.quad(builder, x0, yBase, z1, u0, v1, x1, yBase, z1, u1, v1, x1, yBase, z0, u1, v0, x0, yBase, z0, u0, v0, 0.7f, 0.0f, -1.0f, 0.0f);
                }
                if (drawTop) {
                    float yTop = yBase + thickness - 9.765625E-4f;
                    LuxiumCloudRenderer.quad(builder, x0, yTop, z1, u0, v1, x1, yTop, z1, u1, v1, x1, yTop, z0, u1, v0, x0, yTop, z0, u0, v0, 1.0f, 0.0f, 1.0f, 0.0f);
                }
                if (x0i >= 0) {
                    for (int px = 0; px < 8; ++px) {
                        x = x0 + (float)px;
                        u = ((float)centerCellX + x + 0.5f) * 0.00390625f;
                        LuxiumCloudRenderer.quad(builder, x, yBase, z1, u, v1, x, yBase + thickness, z1, u, v1, x, yBase + thickness, z0, u, v0, x, yBase, z0, u, v0, 0.9f, -1.0f, 0.0f, 0.0f);
                    }
                }
                if (x0i <= 8) {
                    for (int px = 0; px < 8; ++px) {
                        x = x0 + (float)px + 1.0f - 9.765625E-4f;
                        u = ((float)centerCellX + x0 + (float)px + 0.5f) * 0.00390625f;
                        LuxiumCloudRenderer.quad(builder, x, yBase, z1, u, v1, x, yBase + thickness, z1, u, v1, x, yBase + thickness, z0, u, v0, x, yBase, z0, u, v0, 0.9f, 1.0f, 0.0f, 0.0f);
                    }
                }
                if (z0i >= 0) {
                    for (int pz = 0; pz < 8; ++pz) {
                        z = z0 + (float)pz;
                        v = ((float)centerCellZ + z + 0.5f) * 0.00390625f;
                        LuxiumCloudRenderer.quad(builder, x0, yBase + thickness, z, u0, v, x1, yBase + thickness, z, u1, v, x1, yBase, z, u1, v, x0, yBase, z, u0, v, 0.8f, 0.0f, 0.0f, -1.0f);
                    }
                }
                if (z0i > 8) continue;
                for (int pz = 0; pz < 8; ++pz) {
                    z = z0 + (float)pz + 1.0f - 9.765625E-4f;
                    v = ((float)centerCellZ + z0 + (float)pz + 0.5f) * 0.00390625f;
                    LuxiumCloudRenderer.quad(builder, x0, yBase + thickness, z, u0, v, x1, yBase + thickness, z, u1, v, x1, yBase, z, u1, v, x0, yBase, z, u0, v, 0.8f, 0.0f, 0.0f, 1.0f);
                }
            }
        }
    }

    private static void buildFast(BufferBuilder builder, int centerCellX, int centerCellZ, float yBase, int radiusTexels) {
        int tile = 32;
        int radius = Math.max(32, LuxiumCloudRenderer.roundUp(radiusTexels, 32));
        for (int x0i = -radius; x0i < radius; x0i += 32) {
            for (int z0i = -radius; z0i < radius; z0i += 32) {
                float x0 = x0i;
                float x1 = x0 + 32.0f;
                float z0 = z0i;
                float z1 = z0 + 32.0f;
                float u0 = ((float)centerCellX + x0) * 0.00390625f;
                float u1 = ((float)centerCellX + x1) * 0.00390625f;
                float v0 = ((float)centerCellZ + z0) * 0.00390625f;
                float v1 = ((float)centerCellZ + z1) * 0.00390625f;
                LuxiumCloudRenderer.quad(builder, x0, yBase, z1, u0, v1, x1, yBase, z1, u1, v1, x1, yBase, z0, u1, v0, x0, yBase, z0, u0, v0, 1.0f, 0.0f, -1.0f, 0.0f);
            }
        }
    }

    private static void quad(BufferBuilder builder, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3, float shade, float nx, float ny, float nz) {
        builder.m_5483_((double)x0, (double)y0, (double)z0).m_7421_(u0, v0).m_85950_(shade, shade, shade, 1.0f).m_5601_(nx, ny, nz).m_5752_();
        builder.m_5483_((double)x1, (double)y1, (double)z1).m_7421_(u1, v1).m_85950_(shade, shade, shade, 1.0f).m_5601_(nx, ny, nz).m_5752_();
        builder.m_5483_((double)x2, (double)y2, (double)z2).m_7421_(u2, v2).m_85950_(shade, shade, shade, 1.0f).m_5601_(nx, ny, nz).m_5752_();
        builder.m_5483_((double)x3, (double)y3, (double)z3).m_7421_(u3, v3).m_85950_(shade, shade, shade, 1.0f).m_5601_(nx, ny, nz).m_5752_();
    }

    private ResourceLocation textureForLayer(int index, LayerConfig config, float weather) {
        if (!this.sourceLoaded || this.sourceOpaque == null) {
            return VANILLA_CLOUDS;
        }
        double effectiveCoverage = config.coverage + (100.0 - config.coverage) * (config.weatherInfluence / 100.0) * (double)weather;
        effectiveCoverage = Mth.m_14008_((double)effectiveCoverage, (double)0.0, (double)100.0);
        long key = 17L;
        key = LuxiumCloudRenderer.mix(key, effectiveCoverage);
        key = LuxiumCloudRenderer.mix(key, config.edgeSoftness);
        key = LuxiumCloudRenderer.mix(key, config.variation);
        key = 31L * key + (long)index;
        LayerState state = this.layers[index];
        if (state.textureKey != key || state.textureLocation == null || state.dynamicTexture == null) {
            NativeImage image = this.buildCoverageTexture(index, effectiveCoverage, config.edgeSoftness, config.variation);
            Minecraft mc = Minecraft.m_91087_();
            if (state.dynamicTexture == null) {
                state.dynamicTexture = new DynamicTexture(image);
                state.textureLocation = mc.m_91097_().m_118490_("luxium_cloud_layer_" + (index + 1), state.dynamicTexture);
            } else {
                state.dynamicTexture.m_117988_(image);
                state.dynamicTexture.m_117985_();
            }
            state.textureKey = key;
        }
        return state.textureLocation;
    }

    private NativeImage buildCoverageTexture(int layerIndex, double coverage, double softness, double variation) {
        NativeImage output = new NativeImage(256, 256, false);
        double centered = (coverage - 50.0) / 50.0;
        double coverageShift = centered < 0.0 ? centered * 2.5 : centered * 5.5;
        double variationAmplitude = variation / 100.0 * 2.0;
        double feather = Math.max(0.0, softness);
        int seedX = PATTERN_OFFSET_X[layerIndex];
        int seedZ = PATTERN_OFFSET_Z[layerIndex];
        for (int y = 0; y < 256; ++y) {
            for (int x = 0; x < 256; ++x) {
                int basePixel;
                int alpha;
                int index = y * 256 + x;
                double signedDistance = this.sourceOpaque[index] ? -((double)this.distanceToClear[index] - 0.5) : (double)this.distanceToOpaque[index] - 0.5;
                double periodic = 0.5 * Math.sin((double)(x + seedX) * 0.09817477042468103) + 0.3 * Math.cos((double)(y + seedZ) * 0.19634954084936207) + 0.2 * Math.sin((double)(x + y + seedX - seedZ) * 0.39269908169872414);
                double dither = (LuxiumCloudRenderer.hash01(x, y, layerIndex) - 0.5) * 0.8;
                double adjustedDistance = signedDistance + periodic * variationAmplitude + dither;
                double edge = coverageShift - adjustedDistance;
                if (feather <= 1.0E-5) {
                    alpha = edge >= 0.0 ? 255 : 0;
                } else {
                    double t = Mth.m_14008_((double)(edge / (feather * 2.0) + 0.5), (double)0.0, (double)1.0);
                    t = t * t * (3.0 - 2.0 * t);
                    alpha = Mth.m_14045_((int)((int)Math.round(t * 255.0)), (int)0, (int)255);
                }
                if (this.sourcePixels == null) {
                    basePixel = FastColor.ABGR32.m_266248_((int)255, (int)255, (int)255, (int)255);
                } else if (this.sourceOpaque[index]) {
                    basePixel = this.sourcePixels[index];
                } else {
                    int nearest = this.nearestOpaquePixel != null ? this.nearestOpaquePixel[index] : -1;
                    basePixel = nearest >= 0 ? this.sourcePixels[nearest] : FastColor.ABGR32.m_266248_((int)255, (int)255, (int)255, (int)255);
                }
                output.m_84988_(x, y, FastColor.ABGR32.m_266498_((int)alpha, (int)basePixel));
            }
        }
        return output;
    }

    private void ensureSourceTexture() {
        if (this.sourceLoaded || this.sourceLoadFailed) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        Optional resource = mc.m_91098_().m_213713_(VANILLA_CLOUDS);
        if (resource.isEmpty()) {
            LOGGER.warn("Luxium custom clouds could not find {}. Falling back to the vanilla texture without coverage shaping.", (Object)VANILLA_CLOUDS);
            this.sourceLoadFailed = true;
            return;
        }
        try (InputStream stream = ((Resource)resource.get()).m_215507_();
             NativeImage image = NativeImage.m_85058_((InputStream)stream);){
            if (image.m_84982_() != 256 || image.m_85084_() != 256) {
                LOGGER.warn("Luxium custom clouds expected a 256x256 cloud texture but resource pack supplied {}x{}. Coverage shaping is disabled for this pack.", (Object)image.m_84982_(), (Object)image.m_85084_());
                this.sourceLoadFailed = true;
                return;
            }
            this.sourceOpaque = new boolean[65536];
            this.sourcePixels = new int[65536];
            for (int y = 0; y < 256; ++y) {
                for (int x = 0; x < 256; ++x) {
                    int pixel;
                    int index = y * 256 + x;
                    this.sourcePixels[index] = pixel = image.m_84985_(x, y);
                    this.sourceOpaque[index] = FastColor.ABGR32.m_266503_((int)pixel) >= 128;
                }
            }
            this.distanceToOpaque = LuxiumCloudRenderer.distanceTransform(this.sourceOpaque, true);
            this.distanceToClear = LuxiumCloudRenderer.distanceTransform(this.sourceOpaque, false);
            this.nearestOpaquePixel = LuxiumCloudRenderer.nearestOpaqueSource(this.sourceOpaque);
            this.sourceLoaded = true;
        }
        catch (IOException | RuntimeException e) {
            LOGGER.warn("Luxium custom clouds failed to read the active vanilla cloud texture; using the original texture without coverage shaping.", (Throwable)e);
            this.sourceLoadFailed = true;
        }
    }

    private static int[] nearestOpaqueSource(boolean[] opaque) {
        int count = 65536;
        int[] nearest = new int[count];
        Arrays.fill(nearest, -1);
        int[] queue = new int[count];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < count; ++i) {
            if (!opaque[i]) continue;
            nearest[i] = i;
            queue[tail++] = i;
        }
        while (head < tail) {
            int index = queue[head++];
            int x = index & 0xFF;
            int y = index >>> 8;
            int source = nearest[index];
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    int nx;
                    int ny;
                    int next;
                    if (dx == 0 && dy == 0 || nearest[next = (ny = y + dy & 0xFF) << 8 | (nx = x + dx & 0xFF)] != -1) continue;
                    nearest[next] = source;
                    queue[tail++] = next;
                }
            }
        }
        return nearest;
    }

    private static int[] distanceTransform(boolean[] opaque, boolean seedOpaque) {
        int count = 65536;
        int[] distance = new int[count];
        Arrays.fill(distance, Integer.MAX_VALUE);
        int[] queue = new int[count];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < count; ++i) {
            if (opaque[i] != seedOpaque) continue;
            distance[i] = 0;
            queue[tail++] = i;
        }
        while (head < tail) {
            int index = queue[head++];
            int x = index & 0xFF;
            int y = index >>> 8;
            int nextDistance = distance[index] + 1;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    int nx;
                    int ny;
                    int next;
                    if (dx == 0 && dy == 0 || distance[next = (ny = y + dy & 0xFF) << 8 | (nx = x + dx & 0xFF)] != Integer.MAX_VALUE) continue;
                    distance[next] = nextDistance;
                    queue[tail++] = next;
                }
            }
        }
        return distance;
    }

    private static double hash01(int x, int y, int layer) {
        int h = x * 0x1F1F1F1F ^ y * 73244475 ^ (layer + 1) * 668265261;
        h ^= h >>> 16;
        h *= 2146121005;
        h ^= h >>> 15;
        h *= -2073254261;
        h ^= h >>> 16;
        return (double)(h & Integer.MAX_VALUE) / 2.147483647E9;
    }

    private static int layerRadiusTexels(LayerConfig layer) {
        float blockScale = (float)(12.0 * layer.scale);
        if (blockScale <= 0.0f) {
            return 16;
        }
        int radiusTexels = Mth.m_14167_((float)((float)layer.renderDistance / blockScale));
        return Math.max(16, Math.min(128, LuxiumCloudRenderer.roundUp(radiusTexels, 8)));
    }

    private static float layerGeometryRadiusBlocks(LayerConfig layer, CloudStatus cloudStatus) {
        int meshRadiusTexels;
        float blockScale = (float)(12.0 * layer.scale);
        int radiusTexels = LuxiumCloudRenderer.layerRadiusTexels(layer);
        if (cloudStatus == CloudStatus.FANCY) {
            meshRadiusTexels = radiusTexels + 8;
        } else {
            int fastTile = 32;
            meshRadiusTexels = Math.max(32, LuxiumCloudRenderer.roundUp(radiusTexels, 32));
        }
        return Math.max(1.0f, (float)meshRadiusTexels * blockScale);
    }

    private static float layerVerticalReach(LayerConfig layer, CloudStatus cloudStatus, double cameraY) {
        double bottom;
        double top = bottom = layer.height + 0.33 - cameraY;
        if (cloudStatus == CloudStatus.FANCY) {
            top += Math.max(0.5, layer.thickness);
        }
        return (float)Math.max(Math.abs(bottom), Math.abs(top));
    }

    private static float layerRequiredFar(LayerConfig layer, CloudStatus cloudStatus, double cameraY) {
        double horizontal = LuxiumCloudRenderer.layerGeometryRadiusBlocks(layer, cloudStatus);
        double vertical = LuxiumCloudRenderer.layerVerticalReach(layer, cloudStatus, cameraY);
        double cornerDistance = Math.sqrt(horizontal * horizontal * 2.0 + vertical * vertical);
        return (float)Math.min(16384.0, cornerDistance + 48.0);
    }

    private static Matrix4f cloudProjectionMatrix(Matrix4f projectionMatrix, LayerConfig layer, CloudStatus cloudStatus, double cameraY) {
        return LuxiumCloudRenderer.extendPerspectiveFar(projectionMatrix, LuxiumCloudRenderer.layerRequiredFar(layer, cloudStatus, cameraY));
    }

    private static Matrix4f extendPerspectiveFar(Matrix4f source, float requestedFar) {
        if (source == null || !Float.isFinite(requestedFar) || requestedFar <= 0.0f || Math.abs(source.m23() + 1.0f) > 0.001f || Math.abs(source.m33()) > 0.001f) {
            return source;
        }
        float m22 = source.m22();
        float m32 = source.m32();
        float nearDenominator = m22 - 1.0f;
        float farDenominator = m22 + 1.0f;
        if (Math.abs(nearDenominator) < 1.0E-6f || Math.abs(farDenominator) < 1.0E-6f) {
            return source;
        }
        float near = m32 / nearDenominator;
        float currentFar = m32 / farDenominator;
        if (!Float.isFinite(near) || !Float.isFinite(currentFar) || near <= 0.0f || currentFar <= near || requestedFar <= currentFar + 1.0f) {
            return source;
        }
        Matrix4f extended = new Matrix4f((Matrix4fc)source);
        float invDepth = 1.0f / (requestedFar - near);
        extended.m22(-(requestedFar + near) * invDepth);
        extended.m32(-(2.0f * requestedFar * near) * invDepth);
        return extended;
    }

    private static boolean isOrdinaryTerrainDistanceFog(float fogEnd, FogShape fogShape) {
        if (fogShape != FogShape.CYLINDER) {
            return false;
        }
        Minecraft mc = Minecraft.m_91087_();
        float vanillaTerrainFogEnd = Math.max(mc.f_91063_.m_109152_(), 32.0f);
        return Float.isFinite(fogEnd) && Math.abs(fogEnd - vanillaTerrainFogEnd) <= 0.75f;
    }

    private static void applyLayerDistanceFog(LayerConfig layer, CloudStatus cloudStatus, double cameraY) {
        float horizontalRadius = LuxiumCloudRenderer.layerGeometryRadiusBlocks(layer, cloudStatus);
        float vertical = LuxiumCloudRenderer.layerVerticalReach(layer, cloudStatus, cameraY);
        float fadeWidthHorizontal = Mth.m_14036_((float)(horizontalRadius * 0.125f), (float)24.0f, (float)96.0f);
        float startHorizontal = Math.max(0.0f, horizontalRadius - fadeWidthHorizontal);
        float fogStart = (float)Math.sqrt((double)startHorizontal * (double)startHorizontal + (double)vertical * (double)vertical);
        float fogEnd = (float)Math.sqrt((double)horizontalRadius * (double)horizontalRadius + (double)vertical * (double)vertical);
        if (fogEnd <= fogStart + 1.0f) {
            fogEnd = fogStart + 1.0f;
        }
        RenderSystem.setShaderFogStart((float)fogStart);
        RenderSystem.setShaderFogEnd((float)fogEnd);
        RenderSystem.setShaderFogShape((FogShape)FogShape.SPHERE);
    }

    private static int roundUp(int value, int multiple) {
        return (Math.max(1, value) + multiple - 1) / multiple * multiple;
    }

    private static long geometryKey(int x, int z, int y, int radius, CloudStatus status, double thickness) {
        long key = 1469598103934665603L;
        key = (key ^ (long)x) * 1099511628211L;
        key = (key ^ (long)z) * 1099511628211L;
        key = (key ^ (long)y) * 1099511628211L;
        key = (key ^ (long)radius) * 1099511628211L;
        key = (key ^ (long)status.ordinal()) * 1099511628211L;
        key = (key ^ Double.doubleToLongBits(thickness)) * 1099511628211L;
        return key;
    }

    private static long mix(long key, double value) {
        return 31L * key + Double.doubleToLongBits(value);
    }

    private void ensureOcclusionTarget(int width, int height) {
        if (this.occlusionTarget == null) {
            this.occlusionTarget = new TextureTarget(width, height, false, Minecraft.f_91002_);
            this.occlusionTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            this.occlusionTarget.m_83936_(9729);
        } else if (this.occlusionTarget.f_83915_ != width || this.occlusionTarget.f_83916_ != height) {
            this.occlusionTarget.m_83941_(width, height, Minecraft.f_91002_);
            this.occlusionTarget.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            this.occlusionTarget.m_83936_(9729);
        }
    }

    private static LayerConfig[] readConfig() {
        Config.Client c = Config.CLIENT;
        return new LayerConfig[]{new LayerConfig(Config.isFeatureEnabled(c.cloudLayer1Enabled), (Double)c.cloudLayer1Height.get(), (Double)c.cloudLayer1Speed.get(), (Double)c.cloudLayer1Direction.get(), (Double)c.cloudLayer1Scale.get(), (Double)c.cloudLayer1Opacity.get(), (Double)c.cloudLayer1Thickness.get(), (Double)c.cloudLayer1Brightness.get(), (Double)c.cloudLayer1Coverage.get(), (Double)c.cloudLayer1EdgeSoftness.get(), (Double)c.cloudLayer1Variation.get(), (Double)c.cloudLayer1WeatherInfluence.get(), (Double)c.cloudLayer1StormDarkening.get(), (Integer)c.cloudLayer1RenderDistance.get()), new LayerConfig(Config.isFeatureEnabled(c.cloudLayer2Enabled), (Double)c.cloudLayer2Height.get(), (Double)c.cloudLayer2Speed.get(), (Double)c.cloudLayer2Direction.get(), (Double)c.cloudLayer2Scale.get(), (Double)c.cloudLayer2Opacity.get(), (Double)c.cloudLayer2Thickness.get(), (Double)c.cloudLayer2Brightness.get(), (Double)c.cloudLayer2Coverage.get(), (Double)c.cloudLayer2EdgeSoftness.get(), (Double)c.cloudLayer2Variation.get(), (Double)c.cloudLayer2WeatherInfluence.get(), (Double)c.cloudLayer2StormDarkening.get(), (Integer)c.cloudLayer2RenderDistance.get()), new LayerConfig(Config.isFeatureEnabled(c.cloudLayer3Enabled), (Double)c.cloudLayer3Height.get(), (Double)c.cloudLayer3Speed.get(), (Double)c.cloudLayer3Direction.get(), (Double)c.cloudLayer3Scale.get(), (Double)c.cloudLayer3Opacity.get(), (Double)c.cloudLayer3Thickness.get(), (Double)c.cloudLayer3Brightness.get(), (Double)c.cloudLayer3Coverage.get(), (Double)c.cloudLayer3EdgeSoftness.get(), (Double)c.cloudLayer3Variation.get(), (Double)c.cloudLayer3WeatherInfluence.get(), (Double)c.cloudLayer3StormDarkening.get(), (Integer)c.cloudLayer3RenderDistance.get())};
    }

    private static final class LayerState {
        final int index;
        VertexBuffer vertexBuffer;
        long geometryKey = Long.MIN_VALUE;
        VertexBuffer maskVertexBuffer;
        long maskGeometryKey = Long.MIN_VALUE;
        VertexBuffer shadowVertexBuffer;
        DynamicTexture dynamicTexture;
        ResourceLocation textureLocation;
        long textureKey = Long.MIN_VALUE;

        LayerState(int index) {
            this.index = index;
        }
    }

    private record LayerConfig(boolean enabled, double height, double speed, double direction, double scale, double opacity, double thickness, double brightness, double coverage, double edgeSoftness, double variation, double weatherInfluence, double stormDarkening, int renderDistance) {
    }

    private record ShadowPlane(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, double maxFront) {
    }

    private record MaskLayerFrame(float blockScale, float fractionX, float relativeY, float fractionZ, ResourceLocation texture) {
    }

    private record LayerFrame(float blockScale, float fractionX, float fractionY, float fractionZ, ResourceLocation texture) {
    }
}

