/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL12
 *  org.lwjgl.opengl.GL21
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.shadows;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.client.ShaderManager;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

final class GpuBakedHardShadowVolume {
    static final int SIZE_X = 64;
    static final int SIZE_Y = 48;
    static final int SIZE_Z = 64;
    private static final int SURFACE_SAMPLES_PER_BLOCK = 8;
    private static final int DIRECTIONS = 6;
    private static final int SNAP_XZ = 8;
    private static final int SNAP_Y = 4;
    private static final int META_TEXELS_PER_SOURCE = 9;
    private static final int TOTAL_LAYERS = GpuBakedHardShadowVolume.depthForDirection(0) + GpuBakedHardShadowVolume.depthForDirection(1) + GpuBakedHardShadowVolume.depthForDirection(2) + GpuBakedHardShadowVolume.depthForDirection(3) + GpuBakedHardShadowVolume.depthForDirection(4) + GpuBakedHardShadowVolume.depthForDirection(5);
    private final int[] textures = new int[]{-1, -1, -1, -1, -1, -1};
    private boolean ready;
    private int buildTexture = -1;
    private final FloatBuffer metadataPixels = ByteBuffer.allocateDirect(4608).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int fbo = -1;
    private int metadataTexture = -1;
    private int buildDirection;
    private int buildLayerCursor;
    private boolean building;
    private long buildingSignature = Long.MIN_VALUE;
    private long activeSignature = Long.MIN_VALUE;
    private int activeAnchorX = Integer.MIN_VALUE;
    private int activeAnchorY = Integer.MIN_VALUE;
    private int activeAnchorZ = Integer.MIN_VALUE;
    private int buildAnchorX;
    private int buildAnchorY;
    private int buildAnchorZ;

    GpuBakedHardShadowVolume() {
    }

    boolean isReady() {
        return this.ready;
    }

    int textureId(int direction) {
        if (!this.isReady() || direction < 0 || direction >= 6) {
            return -1;
        }
        return this.textures[direction];
    }

    float minX() {
        return this.activeAnchorX;
    }

    float minY() {
        return this.activeAnchorY;
    }

    float minZ() {
        return this.activeAnchorZ;
    }

    float sizeX() {
        return 64.0f;
    }

    float sizeY() {
        return 48.0f;
    }

    float sizeZ() {
        return 64.0f;
    }

    boolean matches(Vec3 cameraPos, long signature) {
        if (!this.isReady() || this.building || cameraPos == null) {
            return false;
        }
        int targetAnchorX = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82479_) - 32, 8);
        int targetAnchorY = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82480_) - 24, 4);
        int targetAnchorZ = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82481_) - 32, 8);
        return this.activeSignature == signature && this.activeAnchorX == targetAnchorX && this.activeAnchorY == targetAnchorY && this.activeAnchorZ == targetAnchorZ;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void update(Vec3 cameraPos, int sourceCount, float[] lightPosRadius, float[] sourceGeometry, float[] sourceColors, float[] atlasRects, int shadowAtlasTexture, long signature, int layerBudget) {
        if (sourceCount <= 0 || shadowAtlasTexture <= 0) {
            return;
        }
        ShaderInstance shader = ShaderManager.getGpuHardShadowBakeShader();
        if (shader == null) {
            return;
        }
        int targetAnchorX = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82479_) - 32, 8);
        int targetAnchorY = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82480_) - 24, 4);
        int targetAnchorZ = GpuBakedHardShadowVolume.snap(Mth.m_14107_((double)cameraPos.f_82481_) - 32, 8);
        if (this.isReady() && !this.building && this.activeSignature == signature && this.activeAnchorX == targetAnchorX && this.activeAnchorY == targetAnchorY && this.activeAnchorZ == targetAnchorZ) {
            return;
        }
        if (!this.building || this.buildingSignature != signature || this.buildAnchorX != targetAnchorX || this.buildAnchorY != targetAnchorY || this.buildAnchorZ != targetAnchorZ) {
            boolean sameAnchor;
            this.ensureResources();
            this.buildAnchorX = targetAnchorX;
            this.buildAnchorY = targetAnchorY;
            this.buildAnchorZ = targetAnchorZ;
            this.buildingSignature = signature;
            this.buildDirection = 0;
            this.buildLayerCursor = 0;
            this.building = true;
            boolean bl = sameAnchor = this.ready && this.activeAnchorX == this.buildAnchorX && this.activeAnchorY == this.buildAnchorY && this.activeAnchorZ == this.buildAnchorZ;
            if (!sameAnchor) {
                this.ready = false;
            }
            if (this.buildTexture != -1) {
                TextureUtil.releaseTextureId((int)this.buildTexture);
                this.buildTexture = -1;
            }
            this.uploadMetadata(cameraPos, sourceCount, lightPosRadius, sourceGeometry, sourceColors, atlasRects);
        }
        int oldFramebuffer = GL11.glGetInteger((int)36006);
        IntBuffer oldViewport = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();
        GL11.glGetIntegerv((int)2978, (IntBuffer)oldViewport);
        int oldViewportX = oldViewport.get(0);
        int oldViewportY = oldViewport.get(1);
        int oldViewportW = oldViewport.get(2);
        int oldViewportH = oldViewport.get(3);
        boolean oldDepth = GL11.glIsEnabled((int)2929);
        boolean oldCull = GL11.glIsEnabled((int)2884);
        boolean oldBlend = GL11.glIsEnabled((int)3042);
        boolean oldScissor = GL11.glIsEnabled((int)3089);
        Matrix4f oldProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        try {
            GL11.glDisable((int)2929);
            GL11.glDisable((int)2884);
            GL11.glDisable((int)3042);
            GL11.glDisable((int)3089);
            shader.m_173350_("ShadowAtlas", (Object)shadowAtlasTexture);
            shader.m_173350_("SourceMetadata", (Object)this.metadataTexture);
            shader.f_173308_.m_5679_(new Matrix4f());
            if (shader.m_173348_("SourceCount") != null) {
                shader.m_173348_("SourceCount").m_142617_(sourceCount);
            }
            int budget = Math.max(1, Math.min(TOTAL_LAYERS, layerBudget));
            while (budget > 0 && this.buildDirection < 6) {
                int direction = this.buildDirection;
                if (this.buildTexture == -1) {
                    this.buildTexture = GpuBakedHardShadowVolume.createRgba16fTexture3D(GpuBakedHardShadowVolume.widthForDirection(direction), GpuBakedHardShadowVolume.heightForDirection(direction), GpuBakedHardShadowVolume.depthForDirection(direction));
                }
                int width = GpuBakedHardShadowVolume.widthForDirection(direction);
                int height = GpuBakedHardShadowVolume.heightForDirection(direction);
                int depth = GpuBakedHardShadowVolume.depthForDirection(direction);
                int remaining = depth - this.buildLayerCursor;
                int layersThisFrame = Math.min(remaining, budget);
                int endLayer = this.buildLayerCursor + layersThisFrame;
                GL11.glViewport((int)0, (int)0, (int)width, (int)height);
                Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float)width, (float)height, 0.0f, -1.0f, 1.0f);
                RenderSystem.setProjectionMatrix((Matrix4f)ortho, (VertexSorting)VertexSorting.f_276633_);
                shader.f_173309_.m_5679_(ortho);
                if (shader.m_173348_("DirectionIndex") != null) {
                    shader.m_173348_("DirectionIndex").m_142617_(direction);
                }
                if (shader.m_173348_("ReceiverScaleX") != null) {
                    shader.m_173348_("ReceiverScaleX").m_142617_(GpuBakedHardShadowVolume.scaleXForDirection(direction));
                }
                if (shader.m_173348_("ReceiverScaleY") != null) {
                    shader.m_173348_("ReceiverScaleY").m_142617_(GpuBakedHardShadowVolume.scaleYForDirection(direction));
                }
                if (shader.m_173348_("ReceiverScaleZ") != null) {
                    shader.m_173348_("ReceiverScaleZ").m_142617_(GpuBakedHardShadowVolume.scaleZForDirection(direction));
                }
                while (this.buildLayerCursor < endLayer) {
                    this.attachLayer(this.buildTexture, this.buildLayerCursor);
                    if (shader.m_173348_("SliceZ") != null) {
                        shader.m_173348_("SliceZ").m_142617_(this.buildLayerCursor);
                    }
                    shader.m_173363_();
                    GpuBakedHardShadowVolume.drawFullscreen(width, height);
                    shader.m_173362_();
                    ++this.buildLayerCursor;
                }
                budget -= layersThisFrame;
                if (this.buildLayerCursor < depth) continue;
                if (this.textures[direction] != -1) {
                    TextureUtil.releaseTextureId((int)this.textures[direction]);
                }
                this.textures[direction] = this.buildTexture;
                this.buildTexture = -1;
                ++this.buildDirection;
                this.buildLayerCursor = 0;
            }
            if (this.buildDirection >= 6) {
                this.ready = true;
                this.activeSignature = this.buildingSignature;
                this.activeAnchorX = this.buildAnchorX;
                this.activeAnchorY = this.buildAnchorY;
                this.activeAnchorZ = this.buildAnchorZ;
                this.building = false;
            }
        }
        finally {
            GL30.glBindFramebuffer((int)36160, (int)oldFramebuffer);
            GL11.glViewport((int)oldViewportX, (int)oldViewportY, (int)oldViewportW, (int)oldViewportH);
            if (oldDepth) {
                GL11.glEnable((int)2929);
            } else {
                GL11.glDisable((int)2929);
            }
            if (oldCull) {
                GL11.glEnable((int)2884);
            } else {
                GL11.glDisable((int)2884);
            }
            if (oldBlend) {
                GL11.glEnable((int)3042);
            } else {
                GL11.glDisable((int)3042);
            }
            if (oldScissor) {
                GL11.glEnable((int)3089);
            } else {
                GL11.glDisable((int)3089);
            }
            RenderSystem.setProjectionMatrix((Matrix4f)oldProjection, (VertexSorting)oldSorting);
            GlStateManager._activeTexture((int)33984);
        }
    }

    void release() {
        if (this.fbo != -1) {
            GL30.glDeleteFramebuffers((int)this.fbo);
            this.fbo = -1;
        }
        if (this.metadataTexture != -1) {
            TextureUtil.releaseTextureId((int)this.metadataTexture);
            this.metadataTexture = -1;
        }
        for (int d = 0; d < 6; ++d) {
            if (this.textures[d] == -1) continue;
            TextureUtil.releaseTextureId((int)this.textures[d]);
            this.textures[d] = -1;
        }
        if (this.buildTexture != -1) {
            TextureUtil.releaseTextureId((int)this.buildTexture);
            this.buildTexture = -1;
        }
        this.ready = false;
        this.buildDirection = 0;
        this.buildLayerCursor = 0;
        this.building = false;
        this.buildingSignature = Long.MIN_VALUE;
        this.activeSignature = Long.MIN_VALUE;
        this.activeAnchorZ = Integer.MIN_VALUE;
        this.activeAnchorY = Integer.MIN_VALUE;
        this.activeAnchorX = Integer.MIN_VALUE;
    }

    private void ensureResources() {
        if (this.fbo == -1) {
            this.fbo = GL30.glGenFramebuffers();
        }
        if (this.metadataTexture == -1) {
            this.metadataTexture = TextureUtil.generateTextureId();
            GL11.glBindTexture((int)3553, (int)this.metadataTexture);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
            GL11.glTexImage2D((int)3553, (int)0, (int)34836, (int)288, (int)1, (int)0, (int)6408, (int)5126, (ByteBuffer)null);
            GL11.glBindTexture((int)3553, (int)0);
        }
    }

    private void uploadMetadata(Vec3 cameraPos, int sourceCount, float[] lightPosRadius, float[] sourceGeometry, float[] sourceColors, float[] atlasRects) {
        this.metadataPixels.clear();
        for (int i = 0; i < 32; ++i) {
            if (i < sourceCount) {
                int base = i * 4;
                int rectBase = i * 24;
                this.metadataPixels.put((float)(cameraPos.f_82479_ + (double)lightPosRadius[base] - (double)this.buildAnchorX));
                this.metadataPixels.put((float)(cameraPos.f_82480_ + (double)lightPosRadius[base + 1] - (double)this.buildAnchorY));
                this.metadataPixels.put((float)(cameraPos.f_82481_ + (double)lightPosRadius[base + 2] - (double)this.buildAnchorZ));
                this.metadataPixels.put(lightPosRadius[base + 3]);
                this.metadataPixels.put((float)(cameraPos.f_82479_ + (double)sourceGeometry[base] - (double)this.buildAnchorX));
                this.metadataPixels.put((float)(cameraPos.f_82480_ + (double)sourceGeometry[base + 1] - (double)this.buildAnchorY));
                this.metadataPixels.put((float)(cameraPos.f_82481_ + (double)sourceGeometry[base + 2] - (double)this.buildAnchorZ));
                this.metadataPixels.put(sourceGeometry[base + 3]);
                this.metadataPixels.put(sourceColors[base]);
                this.metadataPixels.put(sourceColors[base + 1]);
                this.metadataPixels.put(sourceColors[base + 2]);
                this.metadataPixels.put(atlasRects[rectBase + 7]);
                this.metadataPixels.put(atlasRects, rectBase, 24);
                continue;
            }
            for (int j = 0; j < 36; ++j) {
                this.metadataPixels.put(0.0f);
            }
        }
        this.metadataPixels.flip();
        GL21.glBindBuffer((int)35052, (int)0);
        GL11.glBindTexture((int)3553, (int)this.metadataTexture);
        GlStateManager._pixelStore((int)3317, (int)1);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)288, (int)1, (int)6408, (int)5126, (FloatBuffer)this.metadataPixels);
        GL11.glBindTexture((int)3553, (int)0);
    }

    private void attachLayer(int texture, int z) {
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL30.glFramebufferTextureLayer((int)36160, (int)36064, (int)texture, (int)0, (int)z);
        GL11.glDrawBuffer((int)36064);
    }

    private static int createRgba16fTexture3D(int width, int height, int depth) {
        int texture = TextureUtil.generateTextureId();
        GL11.glBindTexture((int)32879, (int)texture);
        GL11.glTexParameteri((int)32879, (int)10241, (int)9729);
        GL11.glTexParameteri((int)32879, (int)10240, (int)9729);
        GL11.glTexParameteri((int)32879, (int)10242, (int)33071);
        GL11.glTexParameteri((int)32879, (int)10243, (int)33071);
        GL11.glTexParameteri((int)32879, (int)32882, (int)33071);
        GL12.glTexImage3D((int)32879, (int)0, (int)34842, (int)width, (int)height, (int)depth, (int)0, (int)6408, (int)5131, (ByteBuffer)null);
        GL11.glBindTexture((int)32879, (int)0);
        return texture;
    }

    private static void drawFullscreen(int width, int height) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder builder = tessellator.m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        builder.m_5483_(0.0, (double)height, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
        builder.m_5483_((double)width, (double)height, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
        builder.m_5483_((double)width, 0.0, 0.0).m_7421_(1.0f, 1.0f).m_5752_();
        builder.m_5483_(0.0, 0.0, 0.0).m_7421_(0.0f, 1.0f).m_5752_();
        BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)builder.m_231175_());
    }

    private static int widthForDirection(int direction) {
        return switch (direction) {
            case 0, 1 -> 64;
            case 2, 3, 4, 5 -> 512;
            default -> 64;
        };
    }

    private static int heightForDirection(int direction) {
        return switch (direction) {
            case 0, 1, 4, 5 -> 384;
            case 2, 3 -> 48;
            default -> 48;
        };
    }

    private static int depthForDirection(int direction) {
        return switch (direction) {
            case 0, 1, 2, 3 -> 512;
            case 4, 5 -> 64;
            default -> 64;
        };
    }

    private static int scaleXForDirection(int direction) {
        return direction == 0 || direction == 1 ? 1 : 8;
    }

    private static int scaleYForDirection(int direction) {
        return direction == 2 || direction == 3 ? 1 : 8;
    }

    private static int scaleZForDirection(int direction) {
        return direction == 4 || direction == 5 ? 1 : 8;
    }

    private static int snap(int value, int grid) {
        return Mth.m_14042_((int)value, (int)grid) * grid;
    }
}

