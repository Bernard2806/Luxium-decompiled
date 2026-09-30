/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 */
package com.vinlanx.luxium.client.shadows;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class GpuShadowLightGrid {
    static final int TILE_SIZE = 16;
    private static final int LIGHTS_PER_TEXEL = 4;
    private static final int TEXELS_PER_TILE = 8;
    private static final int UPLOAD_GUARD_BYTES = 128;
    private int textureId = -1;
    private int textureWidth;
    private int textureHeight;
    private ByteBuffer pixels;
    private int[] tileCounts = new int[0];
    private final Matrix4f viewProjection = new Matrix4f();
    private final Vector4f viewPosition = new Vector4f();
    private final Vector4f clipPosition = new Vector4f();

    GpuShadowLightGrid() {
    }

    int getTextureId() {
        return this.textureId;
    }

    void release() {
        if (this.textureId != -1) {
            TextureUtil.releaseTextureId((int)this.textureId);
            this.textureId = -1;
        }
        this.textureWidth = 0;
        this.textureHeight = 0;
        this.pixels = null;
        this.tileCounts = new int[0];
    }

    void update(int screenWidth, int screenHeight, int lightCount, float[] lights, Matrix4f projection, Matrix4f view) {
        int tilesX = (screenWidth + 16 - 1) / 16;
        int tilesY = (screenHeight + 16 - 1) / 16;
        this.ensureTexture(tilesX * 8, tilesY);
        int byteCount = this.textureWidth * this.textureHeight * 4;
        if (this.pixels == null || this.pixels.capacity() < byteCount + 128) {
            this.pixels = BufferUtils.createByteBuffer((int)(byteCount + 128));
        }
        this.pixels.clear();
        for (int i = 0; i < byteCount; ++i) {
            this.pixels.put((byte)0);
        }
        projection.mul((Matrix4fc)view, this.viewProjection);
        int tileCount = tilesX * tilesY;
        if (this.tileCounts.length < tileCount) {
            this.tileCounts = new int[tileCount];
        }
        Arrays.fill(this.tileCounts, 0, tileCount, 0);
        for (int light = 0; light < lightCount; ++light) {
            int base = light * 4;
            double dx = lights[base];
            double dy = lights[base + 1];
            double dz = lights[base + 2];
            float radius = lights[base + 3];
            this.viewPosition.set((float)dx, (float)dy, (float)dz, 1.0f).mul((Matrix4fc)view);
            float viewDepth = -this.viewPosition.z;
            if (viewDepth <= -radius) continue;
            int minTileX = 0;
            int maxTileX = tilesX - 1;
            int minTileY = 0;
            int maxTileY = tilesY - 1;
            if (viewDepth > radius + 0.1f) {
                float minNdcX = Float.POSITIVE_INFINITY;
                float maxNdcX = Float.NEGATIVE_INFINITY;
                float minNdcY = Float.POSITIVE_INFINITY;
                float maxNdcY = Float.NEGATIVE_INFINITY;
                for (int corner = 0; corner < 8; ++corner) {
                    double wx = dx + (double)((corner & 1) == 0 ? -radius : radius);
                    double wy = dy + (double)((corner & 2) == 0 ? -radius : radius);
                    double wz = dz + (double)((corner & 4) == 0 ? -radius : radius);
                    this.clipPosition.set((float)wx, (float)wy, (float)wz, 1.0f).mul((Matrix4fc)this.viewProjection);
                    if (this.clipPosition.w <= 0.0f) continue;
                    float invW = 1.0f / this.clipPosition.w;
                    minNdcX = Math.min(minNdcX, this.clipPosition.x * invW);
                    maxNdcX = Math.max(maxNdcX, this.clipPosition.x * invW);
                    minNdcY = Math.min(minNdcY, this.clipPosition.y * invW);
                    maxNdcY = Math.max(maxNdcY, this.clipPosition.y * invW);
                }
                if (!Float.isFinite(minNdcX) || maxNdcX < -1.0f || minNdcX > 1.0f || maxNdcY < -1.0f || minNdcY > 1.0f) continue;
                minTileX = GpuShadowLightGrid.clampTile((int)Math.floor((minNdcX * 0.5f + 0.5f) * (float)screenWidth / 16.0f) - 1, tilesX);
                maxTileX = GpuShadowLightGrid.clampTile((int)Math.floor((maxNdcX * 0.5f + 0.5f) * (float)screenWidth / 16.0f) + 1, tilesX);
                minTileY = GpuShadowLightGrid.clampTile((int)Math.floor((minNdcY * 0.5f + 0.5f) * (float)screenHeight / 16.0f) - 1, tilesY);
                maxTileY = GpuShadowLightGrid.clampTile((int)Math.floor((maxNdcY * 0.5f + 0.5f) * (float)screenHeight / 16.0f) + 1, tilesY);
            }
            for (int tileY = minTileY; tileY <= maxTileY; ++tileY) {
                for (int tileX = minTileX; tileX <= maxTileX; ++tileX) {
                    int slot;
                    int tile;
                    int n = tile = tileY * tilesX + tileX;
                    this.tileCounts[n] = this.tileCounts[n] + 1;
                    if (slot >= 32) continue;
                    int texelX = tileX * 8 + slot / 4;
                    int channel = slot % 4;
                    int offset = (tileY * this.textureWidth + texelX) * 4 + channel;
                    this.pixels.put(offset, (byte)(light + 1));
                }
            }
        }
        this.pixels.position(0).limit(byteCount);
        RenderSystem.activeTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glPixelStorei((int)3317, (int)1);
        GL11.glPixelStorei((int)3314, (int)0);
        GL11.glPixelStorei((int)3315, (int)0);
        GL11.glPixelStorei((int)3316, (int)0);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)this.textureWidth, (int)this.textureHeight, (int)6408, (int)5121, (ByteBuffer)this.pixels);
    }

    private void ensureTexture(int width, int height) {
        if (this.textureId == -1) {
            this.textureId = TextureUtil.generateTextureId();
        }
        if (this.textureWidth == width && this.textureHeight == height) {
            return;
        }
        this.textureWidth = width;
        this.textureHeight = height;
        RenderSystem.activeTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)width, (int)height, (int)0, (int)6408, (int)5121, (ByteBuffer)null);
    }

    private static int clampTile(int value, int count) {
        return Math.max(0, Math.min(count - 1, value));
    }
}

