/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.joml.Vector3f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL21
 */
package com.vinlanx.luxium.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

@Mod.EventBusSubscriber(modid="luxium", value={Dist.CLIENT})
public final class LightAtlas {
    public static final int SIZE_X = 48;
    public static final int SIZE_Y = 32;
    public static final int SIZE_Z = 48;
    private static final int ATLAS_W = 2304;
    private static final int ATLAS_H = 32;
    private static final int ANCHOR_SNAP = 8;
    private static final int ANCHOR_MARGIN = 10;
    private static final long UPLOAD_INTERVAL_MS = 120L;
    private static int textureId = -1;
    private static ByteBuffer buffer;
    private static int litVoxels;
    private static final int[] litVoxelIndices;
    private static final byte[] rtxLightCache;
    private static long cachedRtxSnapshotVersion;
    private static int cachedRtxAnchorX;
    private static int cachedRtxAnchorY;
    private static int cachedRtxAnchorZ;
    private static long lastUploadMs;
    private static long lastFlashlightTick;
    private static final Vector3f volumeMin;
    private static final Vector3f volumeSize;
    private static int anchorX;
    private static int anchorY;
    private static int anchorZ;
    private static final BlockPos.MutableBlockPos cursor;

    private LightAtlas() {
    }

    public static int getTextureId() {
        return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeTextureId() : textureId;
    }

    public static boolean isReady() {
        return NeoGpuVanilla.isVolumeReady() || textureId != -1 && litVoxels > 0;
    }

    public static Vector3f getMin() {
        return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeMin(volumeMin) : volumeMin;
    }

    public static Vector3f getSize() {
        return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeSize(volumeSize) : volumeSize;
    }

    public static boolean sampleRandomLitPosition(RandomSource random, Vector3f output) {
        if (NeoGpuVanilla.isVolumeReady()) {
            return NeoGpuVanilla.sampleRandomLitPosition(random, output);
        }
        if (litVoxels <= 0 || anchorX == Integer.MIN_VALUE) {
            return false;
        }
        int packedIndex = litVoxelIndices[random.m_188503_(litVoxels)];
        int localX = packedIndex % 48;
        int yz = packedIndex / 48;
        int localZ = yz % 48;
        int localY = yz / 48;
        output.set((float)(anchorX + localX) + random.m_188501_(), (float)(anchorY + localY) + random.m_188501_(), (float)(anchorZ + localZ) + random.m_188501_());
        return true;
    }

    public static float sampleLightFactor(double worldX, double worldY, double worldZ) {
        if (NeoGpuVanilla.isVolumeReady()) {
            return NeoGpuVanilla.sampleLightFactor(worldX, worldY, worldZ);
        }
        if (buffer == null || litVoxels <= 0 || anchorX == Integer.MIN_VALUE) {
            return 0.0f;
        }
        int localX = Mth.m_14107_((double)worldX) - anchorX;
        int localY = Mth.m_14107_((double)worldY) - anchorY;
        int localZ = Mth.m_14107_((double)worldZ) - anchorZ;
        if (localX < 0 || localY < 0 || localZ < 0 || localX >= 48 || localY >= 32 || localZ >= 48) {
            return 0.0f;
        }
        int index = localY * 2304 + localZ * 48 + localX;
        return (float)(buffer.get(index) & 0xFF) / 255.0f;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (ReflectionSystem.isRenderingWorldPass() || NeoShadowsEngine.isAnyShadowCapturePass()) {
            return;
        }
        LightAtlas.clearVolumeData();
    }

    private static void clearVolumeData() {
        litVoxels = 0;
        lastFlashlightTick = Long.MIN_VALUE;
    }

    private static void ensureTexture() {
        if (textureId != -1) {
            return;
        }
        textureId = TextureUtil.generateTextureId();
        RenderSystem.activeTexture((int)33986);
        GL11.glBindTexture((int)3553, (int)textureId);
        GL21.glBindBuffer((int)35052, (int)0);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)33321, (int)2304, (int)32, (int)0, (int)6403, (int)5121, (ByteBuffer)null);
        GL11.glBindTexture((int)3553, (int)0);
        RenderSystem.activeTexture((int)33984);
        buffer = ByteBuffer.allocateDirect(73728);
    }

    private static void upload(Minecraft mc) {
        boolean flashlight = TestFlashLight.isEnabled();
        if (flashlight && mc.f_91073_ != null) {
            long tick = mc.f_91073_.m_46467_();
            if (tick == lastFlashlightTick) {
                return;
            }
            lastFlashlightTick = tick;
        } else {
            lastFlashlightTick = Long.MIN_VALUE;
            long now = System.currentTimeMillis();
            if (now - lastUploadMs < 120L) {
                return;
            }
            lastUploadMs = now;
        }
        if (buffer == null) {
            return;
        }
        TorchRtxState rtx = TorchRtxState.get();
        boolean rtxOn = rtx.isEnabled();
        Vec3 cam = mc.f_91063_.m_109153_().m_90583_();
        LightAtlas.updateAnchor(cam.f_82479_, cam.f_82480_, cam.f_82481_);
        volumeMin.set((float)anchorX, (float)anchorY, (float)anchorZ);
        if (rtxOn) {
            long snapshotVersion = rtx.getLightSnapshotVersion();
            if (snapshotVersion != cachedRtxSnapshotVersion || anchorX != cachedRtxAnchorX || anchorY != cachedRtxAnchorY || anchorZ != cachedRtxAnchorZ) {
                rtx.fillLightVolume(anchorX, anchorY, anchorZ, 48, 32, 48, rtxLightCache);
                cachedRtxSnapshotVersion = snapshotVersion;
                cachedRtxAnchorX = anchorX;
                cachedRtxAnchorY = anchorY;
                cachedRtxAnchorZ = anchorZ;
            }
        } else if (cachedRtxSnapshotVersion != Long.MIN_VALUE) {
            Arrays.fill(rtxLightCache, (byte)0);
            cachedRtxSnapshotVersion = Long.MIN_VALUE;
            cachedRtxAnchorZ = Integer.MIN_VALUE;
            cachedRtxAnchorY = Integer.MIN_VALUE;
            cachedRtxAnchorX = Integer.MIN_VALUE;
        }
        int lit = 0;
        for (int y = 0; y < 32; ++y) {
            int wy = anchorY + y;
            int row = y * 2304;
            for (int z = 0; z < 48; ++z) {
                int wz = anchorZ + z;
                int slice = row + z * 48;
                for (int x = 0; x < 48; ++x) {
                    int l;
                    int level;
                    cursor.m_122178_(anchorX + x, wy, wz);
                    int packedIndex = slice + x;
                    int n = level = rtxOn ? rtxLightCache[packedIndex] & 0xFF : 0;
                    if (flashlight && (l = TestFlashLight.sampleLight((BlockPos)cursor)) > level) {
                        level = l;
                    }
                    if ((level = Mth.m_14045_((int)level, (int)0, (int)15)) > 0) {
                        LightAtlas.litVoxelIndices[lit] = (y * 48 + z) * 48 + x;
                        ++lit;
                    }
                    buffer.put(slice + x, (byte)(level * 255 / 15 & 0xFF));
                }
            }
        }
        litVoxels = lit;
        buffer.position(0).limit(buffer.capacity());
        RenderSystem.activeTexture((int)33986);
        GL11.glBindTexture((int)3553, (int)textureId);
        GL21.glBindBuffer((int)35052, (int)0);
        GlStateManager._pixelStore((int)3314, (int)0);
        GlStateManager._pixelStore((int)3316, (int)0);
        GlStateManager._pixelStore((int)3315, (int)0);
        GlStateManager._pixelStore((int)3317, (int)1);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)2304, (int)32, (int)6403, (int)5121, (ByteBuffer)buffer);
        GL11.glBindTexture((int)3553, (int)0);
        RenderSystem.activeTexture((int)33984);
    }

    private static void updateAnchor(double cx, double cy, double cz) {
        int hx = 24;
        int hy = 16;
        int hz = 24;
        int dx = LightAtlas.snapToGrid(Mth.m_14107_((double)cx) - hx, 8);
        int dy = LightAtlas.snapToGrid(Mth.m_14107_((double)cy) - hy, 8);
        int dz = LightAtlas.snapToGrid(Mth.m_14107_((double)cz) - hz, 8);
        if (anchorX == Integer.MIN_VALUE) {
            anchorX = dx;
            anchorY = dy;
            anchorZ = dz;
            return;
        }
        int bx = Mth.m_14107_((double)cx);
        int by = Mth.m_14107_((double)cy);
        int bz = Mth.m_14107_((double)cz);
        if (bx < anchorX + 10 || bx > anchorX + 48 - 10) {
            anchorX = dx;
        }
        if (by < anchorY + 10 || by > anchorY + 32 - 10) {
            anchorY = dy;
        }
        if (bz < anchorZ + 10 || bz > anchorZ + 48 - 10) {
            anchorZ = dz;
        }
    }

    private static int snapToGrid(int v, int g) {
        return Mth.m_14042_((int)v, (int)g) * g;
    }

    static {
        litVoxels = 0;
        litVoxelIndices = new int[73728];
        rtxLightCache = new byte[73728];
        cachedRtxSnapshotVersion = Long.MIN_VALUE;
        cachedRtxAnchorX = Integer.MIN_VALUE;
        cachedRtxAnchorY = Integer.MIN_VALUE;
        cachedRtxAnchorZ = Integer.MIN_VALUE;
        lastUploadMs = 0L;
        lastFlashlightTick = Long.MIN_VALUE;
        volumeMin = new Vector3f();
        volumeSize = new Vector3f(48.0f, 32.0f, 48.0f);
        anchorX = Integer.MIN_VALUE;
        anchorY = Integer.MIN_VALUE;
        anchorZ = Integer.MIN_VALUE;
        cursor = new BlockPos.MutableBlockPos();
    }
}

