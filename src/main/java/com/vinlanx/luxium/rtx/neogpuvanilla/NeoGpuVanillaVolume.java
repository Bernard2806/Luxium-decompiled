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
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.LightLayer
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL12
 *  org.lwjgl.opengl.GL21
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.rtx.neogpuvanilla;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.ShaderManager;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaGpuDebug;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

final class NeoGpuVanillaVolume {
    static final int SIZE_X = 48;
    static final int SIZE_Y = 32;
    static final int SIZE_Z = 48;
    static final int ATLAS_W = 2304;
    static final int ATLAS_H = 32;
    private static final int VOXEL_COUNT = 73728;
    private static final int VOLUME_CHANNELS = 4;
    private static final int ANCHOR_SNAP = 8;
    private static final int ANCHOR_MARGIN = 10;
    private final int[] candidateTextures = new int[]{-1, -1};
    private final int[] receiverTextures = new int[]{-1, -1};
    private final int[] receiverScales = new int[]{0, 0};
    private final int[][] fastLightTextures = new int[][]{{-1, -1, -1, -1, -1, -1}, {-1, -1, -1, -1, -1, -1}};
    private final boolean[] fastLightReady = new boolean[]{false, false};
    private int fastActiveIndex;
    private int activeIndex;
    private int vanillaTexture = -1;
    private int metadataTexture = -1;
    private int fbo = -1;
    private final ByteBuffer vanillaPixels = ByteBuffer.allocateDirect(73728).order(ByteOrder.nativeOrder());
    private final ByteBuffer cpuMirror = ByteBuffer.allocateDirect(294912).order(ByteOrder.nativeOrder());
    private final ByteBuffer buildMirror = ByteBuffer.allocateDirect(294912).order(ByteOrder.nativeOrder());
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final FloatBuffer metadataPixels = ByteBuffer.allocateDirect(5760).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final IntBuffer savedViewport = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();
    private int activeAnchorX = Integer.MIN_VALUE;
    private int activeAnchorY = Integer.MIN_VALUE;
    private int activeAnchorZ = Integer.MIN_VALUE;
    private int fastAnchorX = Integer.MIN_VALUE;
    private int fastAnchorY = Integer.MIN_VALUE;
    private int fastAnchorZ = Integer.MIN_VALUE;
    private int buildAnchorX = Integer.MIN_VALUE;
    private int buildAnchorY = Integer.MIN_VALUE;
    private int buildAnchorZ = Integer.MIN_VALUE;
    private boolean ready;
    private int bakedSourceCount;
    private long revision;

    NeoGpuVanillaVolume() {
    }

    int textureId() {
        return this.ready ? this.candidateTextures[this.activeIndex] : -1;
    }

    int receiverTextureId() {
        return this.ready ? this.receiverTextures[this.activeIndex] : -1;
    }

    int receiverScale() {
        return this.ready ? Math.max(1, this.receiverScales[this.activeIndex]) : 1;
    }

    boolean fastShadowsReady() {
        return this.ready && this.fastLightReady[this.fastActiveIndex] && this.fastAnchorX == this.activeAnchorX && this.fastAnchorY == this.activeAnchorY && this.fastAnchorZ == this.activeAnchorZ;
    }

    int fastLightTextureId(int direction) {
        if (!this.fastShadowsReady() || direction < 0 || direction >= 6) {
            return -1;
        }
        return this.fastLightTextures[this.fastActiveIndex][direction];
    }

    int metadataTextureId() {
        return this.metadataTexture;
    }

    int bakedSourceCount() {
        return this.bakedSourceCount;
    }

    boolean isReady() {
        return this.ready && this.candidateTextures[this.activeIndex] > 0 && this.receiverTextures[this.activeIndex] > 0;
    }

    boolean hasResources() {
        boolean fast = false;
        for (int i = 0; i < 2 && !fast; ++i) {
            for (int d = 0; d < 6; ++d) {
                fast |= this.fastLightTextures[i][d] > 0;
            }
        }
        return this.candidateTextures[0] > 0 || this.candidateTextures[1] > 0 || this.receiverTextures[0] > 0 || this.receiverTextures[1] > 0 || fast || this.vanillaTexture > 0 || this.metadataTexture > 0 || this.fbo > 0;
    }

    long revision() {
        return this.revision;
    }

    int anchorX() {
        return this.ready ? this.activeAnchorX : this.buildAnchorX;
    }

    int anchorY() {
        return this.ready ? this.activeAnchorY : this.buildAnchorY;
    }

    int anchorZ() {
        return this.ready ? this.activeAnchorZ : this.buildAnchorZ;
    }

    int buildAnchorX() {
        return this.buildAnchorX;
    }

    int buildAnchorY() {
        return this.buildAnchorY;
    }

    int buildAnchorZ() {
        return this.buildAnchorZ;
    }

    boolean updateAnchor(double cameraX, double cameraY, double cameraZ) {
        boolean outside;
        int desiredX = NeoGpuVanillaVolume.snapToGrid(Mth.m_14107_((double)cameraX) - 24, 8);
        int desiredY = NeoGpuVanillaVolume.snapToGrid(Mth.m_14107_((double)cameraY) - 16, 8);
        int desiredZ = NeoGpuVanillaVolume.snapToGrid(Mth.m_14107_((double)cameraZ) - 24, 8);
        if (this.buildAnchorX == Integer.MIN_VALUE) {
            this.buildAnchorX = desiredX;
            this.buildAnchorY = desiredY;
            this.buildAnchorZ = desiredZ;
            return true;
        }
        int referenceX = this.ready ? this.activeAnchorX : this.buildAnchorX;
        int referenceY = this.ready ? this.activeAnchorY : this.buildAnchorY;
        int referenceZ = this.ready ? this.activeAnchorZ : this.buildAnchorZ;
        int bx = Mth.m_14107_((double)cameraX);
        int by = Mth.m_14107_((double)cameraY);
        int bz = Mth.m_14107_((double)cameraZ);
        boolean bl = outside = bx < referenceX + 10 || bx > referenceX + 48 - 10 || by < referenceY + 10 || by > referenceY + 32 - 10 || bz < referenceZ + 10 || bz > referenceZ + 48 - 10;
        if (!outside) {
            return false;
        }
        if (desiredX == this.buildAnchorX && desiredY == this.buildAnchorY && desiredZ == this.buildAnchorZ) {
            return false;
        }
        this.buildAnchorX = desiredX;
        this.buildAnchorY = desiredY;
        this.buildAnchorZ = desiredZ;
        return true;
    }

    void invalidate() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean bake(ClientLevel level, int shadowAtlasTexture, int sourceCount, float[] sourceData, float[] sourceGeometry, float[] sourceColors, float[] atlasRects) {
        ShaderInstance fastShader;
        if (level == null || sourceCount < 0 || sourceCount > 40 || this.buildAnchorX == Integer.MIN_VALUE) {
            return false;
        }
        if (sourceCount > 0 && shadowAtlasTexture <= 0) {
            return false;
        }
        ShaderInstance coarseShader = ShaderManager.getNeoGpuVanillaBakeShader();
        ShaderInstance resolveShader = ShaderManager.getNeoGpuVanillaResolveShader();
        boolean fastEnabled = Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaFastShadowsEnabled);
        Object object = fastShader = fastEnabled ? ShaderManager.getNeoGpuVanillaFastResolveShader() : null;
        if (coarseShader == null || resolveShader == null || fastEnabled && fastShader == null) {
            return false;
        }
        this.ensureResources();
        this.uploadVanillaLight(level);
        this.uploadSourceMetadata(sourceCount, sourceData, sourceGeometry, sourceColors, atlasRects);
        boolean fastSnapshotComplete = true;
        if (fastEnabled && sourceGeometry != null) {
            int max = Math.min(sourceCount, sourceGeometry.length / 4);
            for (int i = 0; i < max; ++i) {
                int flags = Math.round(sourceGeometry[i * 4 + 3]);
                if ((flags & 0x80) == 0) continue;
                fastSnapshotComplete = false;
                break;
            }
        }
        int buildIndex = this.ready ? 1 - this.activeIndex : this.activeIndex;
        int fineScale = NeoGpuVanillaVolume.receiverScaleForPixelsPerBlock((Integer)Config.CLIENT.neoGpuVanillaCaptureResolution.get());
        this.ensureReceiverTexture(buildIndex, fineScale);
        this.ensureFastLightTextures(buildIndex, fastEnabled && fastSnapshotComplete);
        int oldFramebuffer = GL11.glGetInteger((int)36006);
        boolean oldDepth = GL11.glIsEnabled((int)2929);
        boolean oldCull = GL11.glIsEnabled((int)2884);
        boolean oldBlend = GL11.glIsEnabled((int)3042);
        boolean oldScissor = GL11.glIsEnabled((int)3089);
        boolean oldDither = GL11.glIsEnabled((int)3024);
        boolean oldFramebufferSrgb = GL11.glIsEnabled((int)36281);
        Matrix4f oldProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        this.savedViewport.clear();
        GL11.glGetIntegerv((int)2978, (IntBuffer)this.savedViewport);
        int oldViewportX = this.savedViewport.get(0);
        int oldViewportY = this.savedViewport.get(1);
        int oldViewportW = this.savedViewport.get(2);
        int oldViewportH = this.savedViewport.get(3);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)2884);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)3089);
        GL11.glDisable((int)3024);
        GL11.glDisable((int)36281);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        try {
            this.attachColor(this.candidateTextures[buildIndex]);
            GL11.glViewport((int)0, (int)0, (int)2304, (int)32);
            Matrix4f coarseOrtho = new Matrix4f().setOrtho(0.0f, 2304.0f, 32.0f, 0.0f, -1.0f, 1.0f);
            RenderSystem.setProjectionMatrix((Matrix4f)coarseOrtho, (VertexSorting)VertexSorting.f_276633_);
            coarseShader.m_173350_("VanillaLightVolume", (Object)this.vanillaTexture);
            coarseShader.m_173350_("ShadowAtlas", (Object)Math.max(shadowAtlasTexture, 0));
            coarseShader.m_173350_("SourceMetadata", (Object)this.metadataTexture);
            coarseShader.f_173308_.m_5679_(new Matrix4f());
            coarseShader.f_173309_.m_5679_(coarseOrtho);
            if (coarseShader.m_173348_("SourceCount") != null) {
                coarseShader.m_173348_("SourceCount").m_142617_(sourceCount);
            }
            if (coarseShader.m_173348_("VolumeMin") != null) {
                coarseShader.m_173348_("VolumeMin").m_5889_((float)this.buildAnchorX, (float)this.buildAnchorY, (float)this.buildAnchorZ);
            }
            if (coarseShader.m_173348_("VolumeSize") != null) {
                coarseShader.m_173348_("VolumeSize").m_5889_(48.0f, 32.0f, 48.0f);
            }
            coarseShader.m_173363_();
            NeoGpuVanillaVolume.drawFullscreen(2304, 32);
            coarseShader.m_173362_();
            this.buildMirror.clear();
            GlStateManager._pixelStore((int)3333, (int)1);
            GL11.glReadPixels((int)0, (int)0, (int)2304, (int)32, (int)6408, (int)5121, (ByteBuffer)this.buildMirror);
            this.buildMirror.position(0).limit(this.buildMirror.capacity());
            int fineX = 48 * fineScale;
            int fineY = 32 * fineScale;
            int fineZ = 48 * fineScale;
            int slicesPerRow = NeoGpuVanillaVolume.slicesPerRow(fineX, fineY, fineZ);
            int rows = (fineZ + slicesPerRow - 1) / slicesPerRow;
            int fineAtlasW = fineX * slicesPerRow;
            int fineAtlasH = fineY * rows;
            this.attachColor(this.receiverTextures[buildIndex]);
            GL11.glViewport((int)0, (int)0, (int)fineAtlasW, (int)fineAtlasH);
            Matrix4f fineOrtho = new Matrix4f().setOrtho(0.0f, (float)fineAtlasW, (float)fineAtlasH, 0.0f, -1.0f, 1.0f);
            RenderSystem.setProjectionMatrix((Matrix4f)fineOrtho, (VertexSorting)VertexSorting.f_276633_);
            resolveShader.m_173350_("CandidateVolume", (Object)this.candidateTextures[buildIndex]);
            resolveShader.m_173350_("VanillaLightVolume", (Object)this.vanillaTexture);
            resolveShader.m_173350_("ShadowAtlas", (Object)Math.max(shadowAtlasTexture, 0));
            resolveShader.m_173350_("SourceMetadata", (Object)this.metadataTexture);
            resolveShader.f_173308_.m_5679_(new Matrix4f());
            resolveShader.f_173309_.m_5679_(fineOrtho);
            if (resolveShader.m_173348_("SourceCount") != null) {
                resolveShader.m_173348_("SourceCount").m_142617_(sourceCount);
            }
            if (resolveShader.m_173348_("FineScale") != null) {
                resolveShader.m_173348_("FineScale").m_142617_(fineScale);
            }
            if (resolveShader.m_173348_("FineSlicesPerRow") != null) {
                resolveShader.m_173348_("FineSlicesPerRow").m_142617_(slicesPerRow);
            }
            NeoGpuVanillaGpuDebug.beginFineBake();
            try {
                resolveShader.m_173363_();
                NeoGpuVanillaVolume.drawFullscreen(fineAtlasW, fineAtlasH);
            }
            finally {
                resolveShader.m_173362_();
                NeoGpuVanillaGpuDebug.endFineBake();
            }
            if (fastEnabled && fastSnapshotComplete) {
                this.fastLightReady[buildIndex] = false;
                Matrix4f fastOrtho = new Matrix4f().setOrtho(0.0f, 48.0f, 32.0f, 0.0f, -1.0f, 1.0f);
                RenderSystem.setProjectionMatrix((Matrix4f)fastOrtho, (VertexSorting)VertexSorting.f_276633_);
                GL11.glViewport((int)0, (int)0, (int)48, (int)32);
                fastShader.m_173350_("ShadowAtlas", (Object)Math.max(shadowAtlasTexture, 0));
                fastShader.m_173350_("SourceMetadata", (Object)this.metadataTexture);
                fastShader.f_173308_.m_5679_(new Matrix4f());
                fastShader.f_173309_.m_5679_(fastOrtho);
                if (fastShader.m_173348_("SourceCount") != null) {
                    fastShader.m_173348_("SourceCount").m_142617_(sourceCount);
                }
                if (fastShader.m_173348_("FastStrength") != null) {
                    fastShader.m_173348_("FastStrength").m_5985_(((Double)Config.CLIENT.neoGpuVanillaFastShadowStrength.get()).floatValue());
                }
                if (fastShader.m_173348_("DiffuseWrap") != null) {
                    fastShader.m_173348_("DiffuseWrap").m_5985_(((Double)Config.CLIENT.neoGpuVanillaFastDiffuseWrap.get()).floatValue());
                }
                for (int z = 0; z < 48; ++z) {
                    if (fastShader.m_173348_("SliceZ") != null) {
                        fastShader.m_173348_("SliceZ").m_142617_(z);
                    }
                    for (int direction = 0; direction < 6; ++direction) {
                        this.attachFastLightLayer(buildIndex, z, direction);
                        if (fastShader.m_173348_("DirectionIndex") != null) {
                            fastShader.m_173348_("DirectionIndex").m_142617_(direction);
                        }
                        fastShader.m_173363_();
                        NeoGpuVanillaVolume.drawFullscreen(48, 32);
                        fastShader.m_173362_();
                    }
                }
                this.fastLightReady[buildIndex] = true;
                this.fastActiveIndex = buildIndex;
                this.fastAnchorX = this.buildAnchorX;
                this.fastAnchorY = this.buildAnchorY;
                this.fastAnchorZ = this.buildAnchorZ;
            }
            this.cpuMirror.clear();
            this.buildMirror.position(0);
            this.cpuMirror.put(this.buildMirror);
            this.cpuMirror.position(0).limit(this.cpuMirror.capacity());
            this.activeIndex = buildIndex;
            this.activeAnchorX = this.buildAnchorX;
            this.activeAnchorY = this.buildAnchorY;
            this.activeAnchorZ = this.buildAnchorZ;
            this.bakedSourceCount = sourceCount;
            this.ready = true;
            ++this.revision;
            boolean bl = true;
            return bl;
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
            if (oldDither) {
                GL11.glEnable((int)3024);
            } else {
                GL11.glDisable((int)3024);
            }
            if (oldFramebufferSrgb) {
                GL11.glEnable((int)36281);
            } else {
                GL11.glDisable((int)36281);
            }
            RenderSystem.setProjectionMatrix((Matrix4f)oldProjection, (VertexSorting)oldSorting);
            GlStateManager._activeTexture((int)33984);
        }
    }

    float sampleLightFactor(double worldX, double worldY, double worldZ) {
        if (!this.isReady()) {
            return 0.0f;
        }
        int x = Mth.m_14107_((double)worldX) - this.activeAnchorX;
        int y = Mth.m_14107_((double)worldY) - this.activeAnchorY;
        int z = Mth.m_14107_((double)worldZ) - this.activeAnchorZ;
        if (x < 0 || y < 0 || z < 0 || x >= 48 || y >= 32 || z >= 48) {
            return 0.0f;
        }
        int index = y * 2304 + z * 48 + x;
        return (float)(this.cpuMirror.get(index * 4) & 0xFF) / 255.0f;
    }

    boolean sampleRandomLitPosition(RandomSource random, Vector3f output) {
        if (!this.isReady()) {
            return false;
        }
        int start = random.m_188503_(73728);
        for (int i = 0; i < 96; ++i) {
            int index = (start + i * 977) % 73728;
            if ((this.cpuMirror.get(index * 4) & 0xFF) == 0) continue;
            int x = index % 48;
            int yz = index / 48;
            int z = yz % 48;
            int y = yz / 48;
            output.set((float)(this.activeAnchorX + x) + random.m_188501_(), (float)(this.activeAnchorY + y) + random.m_188501_(), (float)(this.activeAnchorZ + z) + random.m_188501_());
            return true;
        }
        return false;
    }

    void release() {
        if (this.fbo != -1) {
            GL30.glDeleteFramebuffers((int)this.fbo);
            this.fbo = -1;
        }
        for (int i = 0; i < 2; ++i) {
            if (this.candidateTextures[i] != -1) {
                TextureUtil.releaseTextureId((int)this.candidateTextures[i]);
                this.candidateTextures[i] = -1;
            }
            if (this.receiverTextures[i] != -1) {
                TextureUtil.releaseTextureId((int)this.receiverTextures[i]);
                this.receiverTextures[i] = -1;
            }
            for (int d = 0; d < 6; ++d) {
                if (this.fastLightTextures[i][d] == -1) continue;
                TextureUtil.releaseTextureId((int)this.fastLightTextures[i][d]);
                this.fastLightTextures[i][d] = -1;
            }
            this.fastLightReady[i] = false;
            this.receiverScales[i] = 0;
        }
        if (this.vanillaTexture != -1) {
            TextureUtil.releaseTextureId((int)this.vanillaTexture);
            this.vanillaTexture = -1;
        }
        if (this.metadataTexture != -1) {
            TextureUtil.releaseTextureId((int)this.metadataTexture);
            this.metadataTexture = -1;
        }
        this.ready = false;
        this.bakedSourceCount = 0;
        this.activeIndex = 0;
        this.fastActiveIndex = 0;
        this.activeAnchorZ = Integer.MIN_VALUE;
        this.activeAnchorY = Integer.MIN_VALUE;
        this.activeAnchorX = Integer.MIN_VALUE;
        this.fastAnchorZ = Integer.MIN_VALUE;
        this.fastAnchorY = Integer.MIN_VALUE;
        this.fastAnchorX = Integer.MIN_VALUE;
        this.buildAnchorZ = Integer.MIN_VALUE;
        this.buildAnchorY = Integer.MIN_VALUE;
        this.buildAnchorX = Integer.MIN_VALUE;
        ++this.revision;
    }

    private void ensureResources() {
        if (this.fbo == -1) {
            this.fbo = GL30.glGenFramebuffers();
        }
        for (int i = 0; i < 2; ++i) {
            if (this.candidateTextures[i] != -1) continue;
            this.candidateTextures[i] = NeoGpuVanillaVolume.createRgba8Texture(2304, 32);
        }
        if (this.vanillaTexture == -1) {
            this.vanillaTexture = NeoGpuVanillaVolume.createR8Texture(2304, 32);
        }
        if (this.metadataTexture == -1) {
            this.metadataTexture = NeoGpuVanillaVolume.createMetadataTexture();
        }
    }

    private void ensureReceiverTexture(int index, int scale) {
        if (this.receiverTextures[index] != -1 && this.receiverScales[index] == scale) {
            return;
        }
        if (this.receiverTextures[index] != -1) {
            TextureUtil.releaseTextureId((int)this.receiverTextures[index]);
        }
        int fineX = 48 * scale;
        int fineY = 32 * scale;
        int fineZ = 48 * scale;
        int slicesPerRow = NeoGpuVanillaVolume.slicesPerRow(fineX, fineY, fineZ);
        int rows = (fineZ + slicesPerRow - 1) / slicesPerRow;
        this.receiverTextures[index] = NeoGpuVanillaVolume.createR8Texture(fineX * slicesPerRow, fineY * rows);
        this.receiverScales[index] = scale;
    }

    private void ensureFastLightTextures(int index, boolean enabled) {
        if (!enabled) {
            this.fastLightReady[index] = false;
            return;
        }
        for (int d = 0; d < 6; ++d) {
            if (this.fastLightTextures[index][d] != -1) continue;
            this.fastLightTextures[index][d] = NeoGpuVanillaVolume.createRgba16f3dTexture(48, 32, 48);
        }
    }

    private void attachColor(int texture) {
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)texture, (int)0);
        for (int attachment = 1; attachment < 4; ++attachment) {
            GL30.glFramebufferTexture2D((int)36160, (int)(36064 + attachment), (int)3553, (int)0, (int)0);
        }
        GL11.glDrawBuffer((int)36064);
    }

    private void attachFastLightLayer(int index, int z, int direction) {
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL30.glFramebufferTextureLayer((int)36160, (int)36064, (int)this.fastLightTextures[index][direction], (int)0, (int)z);
        for (int attachment = 1; attachment < 4; ++attachment) {
            GL30.glFramebufferTexture2D((int)36160, (int)(36064 + attachment), (int)3553, (int)0, (int)0);
        }
        GL11.glDrawBuffer((int)36064);
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

    private static int createRgba8Texture(int width, int height) {
        int texture = TextureUtil.generateTextureId();
        GL11.glBindTexture((int)3553, (int)texture);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)width, (int)height, (int)0, (int)6408, (int)5121, (ByteBuffer)null);
        GL11.glBindTexture((int)3553, (int)0);
        return texture;
    }

    private static int createRgba16f3dTexture(int width, int height, int depth) {
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

    private static int createR8Texture(int width, int height) {
        int texture = TextureUtil.generateTextureId();
        GL11.glBindTexture((int)3553, (int)texture);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)33321, (int)width, (int)height, (int)0, (int)6403, (int)5121, (ByteBuffer)null);
        GL11.glBindTexture((int)3553, (int)0);
        return texture;
    }

    private static int createMetadataTexture() {
        int texture = TextureUtil.generateTextureId();
        GL11.glBindTexture((int)3553, (int)texture);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)34836, (int)360, (int)1, (int)0, (int)6408, (int)5126, (ByteBuffer)null);
        GL11.glBindTexture((int)3553, (int)0);
        return texture;
    }

    private void uploadSourceMetadata(int sourceCount, float[] sourceData, float[] sourceGeometry, float[] sourceColors, float[] atlasRects) {
        this.metadataPixels.clear();
        for (int i = 0; i < 40; ++i) {
            if (i < sourceCount) {
                this.metadataPixels.put(sourceData, i * 4, 4);
                this.metadataPixels.put(sourceGeometry, i * 4, 4);
                this.metadataPixels.put(sourceColors, i * 4, 4);
                this.metadataPixels.put(atlasRects, i * 24, 24);
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
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)360, (int)1, (int)6408, (int)5126, (FloatBuffer)this.metadataPixels);
        GL11.glBindTexture((int)3553, (int)0);
    }

    private void uploadVanillaLight(ClientLevel level) {
        this.vanillaPixels.clear();
        for (int y = 0; y < 32; ++y) {
            int wy = this.buildAnchorY + y;
            int row = y * 2304;
            for (int z = 0; z < 48; ++z) {
                int wz = this.buildAnchorZ + z;
                int slice = row + z * 48;
                for (int x = 0; x < 48; ++x) {
                    this.cursor.m_122178_(this.buildAnchorX + x, wy, wz);
                    int block = Mth.m_14045_((int)level.m_45517_(LightLayer.BLOCK, (BlockPos)this.cursor), (int)0, (int)15);
                    this.vanillaPixels.put(slice + x, (byte)(block * 255 / 15));
                }
            }
        }
        this.vanillaPixels.position(0).limit(this.vanillaPixels.capacity());
        GL21.glBindBuffer((int)35052, (int)0);
        GL11.glBindTexture((int)3553, (int)this.vanillaTexture);
        GlStateManager._pixelStore((int)3314, (int)0);
        GlStateManager._pixelStore((int)3316, (int)0);
        GlStateManager._pixelStore((int)3315, (int)0);
        GlStateManager._pixelStore((int)3317, (int)1);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)2304, (int)32, (int)6403, (int)5121, (ByteBuffer)this.vanillaPixels);
        GL11.glBindTexture((int)3553, (int)0);
    }

    private static int slicesPerRow(int fineX, int fineY, int fineZ) {
        return Math.max(1, (int)Math.ceil(Math.sqrt((double)fineZ * (double)fineY / (double)Math.max(1, fineX))));
    }

    private static int receiverScaleForPixelsPerBlock(int pixelsPerBlock) {
        int p = Math.max(1, pixelsPerBlock);
        if (p >= 512) {
            return 10;
        }
        if (p >= 256) {
            return 9;
        }
        if (p >= 128) {
            return 8;
        }
        if (p >= 64) {
            return 7;
        }
        if (p >= 32) {
            return 6;
        }
        if (p >= 16) {
            return 5;
        }
        if (p >= 8) {
            return 4;
        }
        if (p >= 4) {
            return 3;
        }
        if (p >= 2) {
            return 2;
        }
        return 1;
    }

    private static int snapToGrid(int value, int grid) {
        return Mth.m_14042_((int)value, (int)grid) * grid;
    }
}

