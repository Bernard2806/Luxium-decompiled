/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL11C
 *  org.lwjgl.opengl.GL21
 */
package com.vinlanx.luxium.client.shadows;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.rtx.NeoFloodEngine;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL21;

final class GpuNeoFloodVisibilityAtlas {
    static final int VOLUME_RADIUS = 36;
    static final int VOLUME_SIDE = 73;
    static final int SLICES_PER_ROW = 9;
    static final int SOURCE_REGION_SIZE = 657;
    static final int SLOT_COLUMNS = 6;
    private static final int VOLUME_PLANE = 5329;
    private static final int VOLUME_BYTES = 389017;
    private static final int REGION_BYTES = 431649;
    private static final long EMPTY_KEY = Long.MIN_VALUE;
    private int textureId = -1;
    private int textureWidth;
    private int textureHeight;
    private int slotCapacity;
    private int frameSerial;
    private final long[] slotSourceKeys = new long[32];
    private final long[] slotRevisions = new long[32];
    private final int[] slotLastUsed = new int[32];
    private final boolean[] slotUsedThisFrame = new boolean[32];
    private final byte[] rawVolume = new byte[389017];
    private final byte[] expandedVolume = new byte[389017];
    private final byte[] packedRegion = new byte[431649];
    private ByteBuffer uploadBuffer;

    GpuNeoFloodVisibilityAtlas() {
        Arrays.fill(this.slotSourceKeys, Long.MIN_VALUE);
    }

    int getTextureId() {
        return this.textureId;
    }

    void update(NeoFloodEngine engine, long[] sourceKeys, int lightCount, int requestedCapacity, int[] slotsOut) {
        int capacity = Math.max(1, Math.min(32, requestedCapacity));
        this.ensureTexture(capacity);
        Arrays.fill(slotsOut, -1);
        Arrays.fill(this.slotUsedThisFrame, false);
        this.frameSerial = this.frameSerial == Integer.MAX_VALUE ? 1 : this.frameSerial + 1;
        int count = Math.min(lightCount, Math.min(sourceKeys.length, slotsOut.length));
        for (int light = 0; light < count; ++light) {
            long sourceKey = sourceKeys[light];
            int slot = this.findExistingSlot(sourceKey, capacity);
            if (slot < 0) {
                slot = this.acquireSlot(capacity);
            }
            if (slot < 0) continue;
            this.slotUsedThisFrame[slot] = true;
            this.slotLastUsed[slot] = this.frameSerial;
            boolean reassigned = this.slotSourceKeys[slot] != sourceKey;
            long revision = engine.getSourceVisibilityRevision(sourceKey);
            if (revision <= 0L) {
                if (reassigned || this.slotRevisions[slot] != 0L) {
                    this.clearSlot(slot);
                }
                this.slotSourceKeys[slot] = sourceKey;
                this.slotRevisions[slot] = 0L;
                slotsOut[light] = slot;
                continue;
            }
            if (reassigned || this.slotRevisions[slot] != revision) {
                long copiedRevision = engine.copySourceVisibilityVolume(sourceKey, 36, this.rawVolume);
                if (copiedRevision <= 0L) {
                    this.clearSlot(slot);
                    this.slotSourceKeys[slot] = sourceKey;
                    this.slotRevisions[slot] = 0L;
                    slotsOut[light] = slot;
                    continue;
                }
                System.arraycopy(this.rawVolume, 0, this.expandedVolume, 0, 389017);
                this.packSlices();
                this.uploadSlot(slot);
                this.slotSourceKeys[slot] = sourceKey;
                this.slotRevisions[slot] = copiedRevision;
            }
            slotsOut[light] = slot;
        }
    }

    void release() {
        if (this.textureId != -1) {
            TextureUtil.releaseTextureId((int)this.textureId);
            this.textureId = -1;
        }
        this.textureWidth = 0;
        this.textureHeight = 0;
        this.slotCapacity = 0;
        this.uploadBuffer = null;
        Arrays.fill(this.slotSourceKeys, Long.MIN_VALUE);
        Arrays.fill(this.slotRevisions, 0L);
        Arrays.fill(this.slotLastUsed, 0);
        Arrays.fill(this.slotUsedThisFrame, false);
    }

    private int findExistingSlot(long sourceKey, int capacity) {
        for (int slot = 0; slot < capacity; ++slot) {
            if (this.slotSourceKeys[slot] != sourceKey) continue;
            return slot;
        }
        return -1;
    }

    private int acquireSlot(int capacity) {
        for (int slot = 0; slot < capacity; ++slot) {
            if (this.slotUsedThisFrame[slot] || this.slotSourceKeys[slot] != Long.MIN_VALUE) continue;
            return slot;
        }
        int bestSlot = -1;
        int oldestFrame = Integer.MAX_VALUE;
        for (int slot = 0; slot < capacity; ++slot) {
            if (this.slotUsedThisFrame[slot] || this.slotLastUsed[slot] >= oldestFrame) continue;
            oldestFrame = this.slotLastUsed[slot];
            bestSlot = slot;
        }
        return bestSlot;
    }

    private void ensureTexture(int capacity) {
        RenderSystem.assertOnRenderThread();
        int rows = (capacity + 6 - 1) / 6;
        int width = 3942;
        int height = rows * 657;
        if (this.textureId != -1 && this.textureWidth == width && this.textureHeight == height && this.slotCapacity == capacity) {
            return;
        }
        if (this.textureId != -1) {
            TextureUtil.releaseTextureId((int)this.textureId);
        }
        this.textureId = TextureUtil.generateTextureId();
        this.textureWidth = width;
        this.textureHeight = height;
        this.slotCapacity = capacity;
        Arrays.fill(this.slotSourceKeys, Long.MIN_VALUE);
        Arrays.fill(this.slotRevisions, 0L);
        Arrays.fill(this.slotLastUsed, 0);
        RenderSystem.activeTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL21.glBindBuffer((int)35052, (int)0);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexParameteri((int)3553, (int)33085, (int)0);
        GL11.glPixelStorei((int)3317, (int)1);
        GL11.glPixelStorei((int)3314, (int)0);
        GL11.glPixelStorei((int)3315, (int)0);
        GL11.glPixelStorei((int)3316, (int)0);
        GL11C.glTexImage2D((int)3553, (int)0, (int)33321, (int)this.textureWidth, (int)this.textureHeight, (int)0, (int)6403, (int)5121, (long)0L);
        Arrays.fill(this.packedRegion, (byte)0);
        for (int slot = 0; slot < this.slotCapacity; ++slot) {
            this.uploadSlot(slot);
        }
    }

    private void packSlices() {
        Arrays.fill(this.packedRegion, (byte)0);
        for (int slice = 0; slice < 73; ++slice) {
            int tileX = slice % 9 * 73;
            int tileY = slice / 9 * 73;
            for (int y = 0; y < 73; ++y) {
                int sourceOffset = y * 5329 + slice * 73;
                int targetOffset = (tileY + y) * 657 + tileX;
                System.arraycopy(this.expandedVolume, sourceOffset, this.packedRegion, targetOffset, 73);
            }
        }
    }

    private void clearSlot(int slot) {
        Arrays.fill(this.packedRegion, (byte)0);
        this.uploadSlot(slot);
    }

    private void uploadSlot(int slot) {
        if (this.uploadBuffer == null || this.uploadBuffer.capacity() < 431649) {
            this.uploadBuffer = BufferUtils.createByteBuffer((int)431649);
        }
        this.uploadBuffer.clear();
        this.uploadBuffer.put(this.packedRegion);
        this.uploadBuffer.flip();
        int originX = slot % 6 * 657;
        int originY = slot / 6 * 657;
        RenderSystem.activeTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL21.glBindBuffer((int)35052, (int)0);
        GL11.glPixelStorei((int)3317, (int)1);
        GL11.glPixelStorei((int)3314, (int)0);
        GL11.glPixelStorei((int)3315, (int)0);
        GL11.glPixelStorei((int)3316, (int)0);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)originX, (int)originY, (int)657, (int)657, (int)6403, (int)5121, (ByteBuffer)this.uploadBuffer);
    }
}

