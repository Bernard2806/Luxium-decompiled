package com.vinlanx.luxium.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Testing.TestFlashLight;
import com.vinlanx.luxium.platform.RenderFrame;
import com.vinlanx.luxium.rtx.TorchRtxState;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.ReflectionSystem;
import net.minecraft.core.BlockPos;

public final class LightAtlas {
    public static final int SIZE_X = 48;
    public static final int SIZE_Y = 32;
    public static final int SIZE_Z = 48;
    private static final int ATLAS_W = 2304;
    private static final int ATLAS_H = 32;
    private static final int ANCHOR_SNAP = 8;
    private static final int ANCHOR_MARGIN = 10;
    private static final long UPLOAD_INTERVAL_MS = 120L;
    private static int



    textureId = -1;
    private static ByteBuffer buffer;
    private static int litVoxels = 0;
    private static final int[] litVoxelIndices = new int[73728];
    private static final byte[]
 rtxLightCache = new byte[73728];
    private static long cachedRtxSnapshotVersion = Long.MIN_VALUE;
    private static int cachedRtxAnchorX = Integer.MIN_VALUE;
    private static int cachedRtxAnchorY = Integer.MIN_VALUE;
    private static int cachedRtxAnchorZ = Integer.MIN_VALUE;
    private static long lastUploadMs = 0L;
    private static long lastFlashlightTick = Long.MIN_VALUE;
    private static final Vector3f
    volumeMin = new Vector3f();
    private static final Vector3f volumeSize = new Vector3f(48.0F, 32.0F, 48.0F);
    private static int anchorX = Integer.MIN_VALUE;
    private static int anchorY = Integer.MIN_VALUE;
    private static int anchorZ = Integer.MIN_VALUE;
    private static final MutableBlockPos cursor = new MutableBlockPos();

    private LightAtlas() { }

    public static int getTextureId() {
        return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeTextureId() : textureId;
    }

    public static boolean isReady() { return NeoGpuVanilla.isVolumeReady() || textureId != -1 && litVoxels > 0;
    }

    public static Vector3f getMin() { return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeMin(volumeMin) : volumeMin;
    }

    public static Vector3f getSize() { return NeoGpuVanilla.isVolumeReady() ? NeoGpuVanilla.getVolumeSize(volumeSize) : volumeSize;
    }

    public static boolean sampleRandomLitPosition(RandomSource random, Vector3f output) {
        if (NeoGpuVanilla.isVolumeReady()) {
            return NeoGpuVanilla.sampleRandomLitPosition(random, output);
        }
        if (litVoxels <= 0 || anchorX == Integer.MIN_VALUE) { return false;
        }
        int packedIndex = litVoxelIndices[random.nextInt(litVoxels)];
        int localX = packedIndex % 48;
        int yz = packedIndex / 48;
        int localZ = yz % 48;
        int localY = yz / 48;

        output.set((float)(anchorX + localX) + random.nextFloat
                (), (float)(anchorY + localY) + random.nextFloat
                (), (float)(anchorZ + localZ) + random.nextFloat
                ());

        return true;
    }

    public static float sampleLightFactor(double worldX, double worldY, double worldZ) {
        if (NeoGpuVanilla.isVolumeReady()) {
            return NeoGpuVanilla.sampleLightFactor(worldX, worldY, worldZ);
        }
        if (buffer == null || litVoxels <= 0 || anchorX == Integer.MIN_VALUE) { return 0.0f;
        }
        int localX = Mth.floor((double)worldX) - anchorX;
        int localY = Mth.floor((double)worldY) - anchorY;
        int localZ = Mth.floor((double)worldZ) - anchorZ;
        if (localX < 0 || localY < 0 || localZ < 0 || localX >= 48 || localY >= 32 || localZ >= 48) {

            return 0.0f;
        }

        int index = localY * 2304 + localZ * 48 + localX;
        return (float)(buffer.get(index) & 0xFF) / 255.0f;
    }

    public static void onRenderLevel(RenderFrame event) {
        if (!(event.getStage() == RenderFrame.Stage.AFTER_PARTICLES)) { return;
 } if (!(!ReflectionSystem.isRenderingWorldPass() && !NeoShadowsEngine.isAnyShadowCapturePass())) { return;
 }
                clearVolumeData();
    }

    private static void clearVolumeData() {
 litVoxels = 0;
        lastFlashlightTick = Long.MIN_VALUE;
    }

    private static void ensureTexture() {
        if (textureId != -1) { return;
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

        if (flashlight && mc.level != null) {
            long tick = mc.level.getGameTime();
            if (tick == lastFlashlightTick) { return; }
            lastFlashlightTick = tick;
        } else {
            lastFlashlightTick = Long.MIN_VALUE;
            long now = System.currentTimeMillis();
            if (now - lastUploadMs < 120L) { return; }
            lastUploadMs = now;
        }

        if (buffer == null) { return;
        }
        TorchRtxState rtx = TorchRtxState.get();
        boolean rtxOn = rtx.isEnabled();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        LightAtlas.updateAnchor(cam.x, cam.y, cam.z);
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
            cachedRtxAnchorX = cachedRtxAnchorY = cachedRtxAnchorZ = Integer.MIN_VALUE;
        }

        int lit = 0;
        for (int y = 0; y < 32; ++y) {
            int wy = anchorY + y;
            int row = y * 2304;
            for (int z = 0; z < 48; ++z) {
                int wz = anchorZ + z;
                int slice = row + z * 48;
                for (int x = 0; x < 48; ++x) {
                    cursor.set(anchorX + x, wy, wz);
                    int packedIndex = slice + x;
                    int level = rtxOn ? rtxLightCache[packedIndex] & 0xFF : 0; int l;
                    if (flashlight) { l = TestFlashLight.sampleLight((BlockPos)cursor); if (l > level) { level = l; } }
                    level = Mth.clamp((int)level, (int)0, (int)15);
                    if (level > 0) {
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
        int hx = 24; int hy = 16; int hz = 24;
        int dx = LightAtlas.snapToGrid(Mth.floor((double)cx) - hx, 8);
        int dy = LightAtlas.snapToGrid(Mth.floor((double)cy) - hy, 8);
        int dz = LightAtlas.snapToGrid(Mth.floor((double)cz) - hz, 8);
        if (anchorX == Integer.MIN_VALUE) { anchorX = dx; anchorY = dy; anchorZ = dz; return; }
        int bx = Mth.floor((double)cx); int by = Mth.floor((double)cy); int bz = Mth.floor((double)cz);
        if (bx < anchorX + 10 || bx > anchorX + 48 - 10) { anchorX = dx; }
        if (by < anchorY + 10 || by > anchorY + 32 - 10) { anchorY = dy; }
        if (bz < anchorZ + 10 || bz > anchorZ + 48 - 10) { anchorZ = dz; }
    } private static int snapToGrid(int v, int g) {

 return Mth.floorDiv(v, g) * g;
    }
    }
