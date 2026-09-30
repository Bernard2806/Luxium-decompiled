/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.ssr;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

final class SsrHitBuffer
implements AutoCloseable {
    private int textureId;
    private int width;
    private int height;
    private int attachedFramebuffer;
    private boolean captureActive;
    private boolean ready;

    SsrHitBuffer() {
    }

    boolean beginCapture(int framebufferId, int requestedWidth, int requestedHeight) {
        RenderSystem.assertOnRenderThread();
        if (framebufferId <= 0 || requestedWidth <= 0 || requestedHeight <= 0) {
            this.invalidate();
            return false;
        }
        this.ensure(requestedWidth, requestedHeight);
        if (this.textureId == 0) {
            this.invalidate();
            return false;
        }
        GlStateManager._glBindFramebuffer((int)36160, (int)framebufferId);
        GL30.glFramebufferTexture2D((int)36160, (int)36065, (int)3553, (int)this.textureId, (int)0);
        GL20.glDrawBuffers((int[])new int[]{36065});
        GlStateManager._clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GlStateManager._clear((int)16384, (boolean)Minecraft.f_91002_);
        GL20.glDrawBuffers((int[])new int[]{36064, 36065});
        int status = GL30.glCheckFramebufferStatus((int)36160);
        if (status != 36053) {
            GL20.glDrawBuffers((int[])new int[]{36064});
            GL30.glFramebufferTexture2D((int)36160, (int)36065, (int)3553, (int)0, (int)0);
            this.invalidate();
            return false;
        }
        this.attachedFramebuffer = framebufferId;
        this.captureActive = true;
        this.ready = false;
        return true;
    }

    void endCapture(boolean successful) {
        RenderSystem.assertOnRenderThread();
        if (!this.captureActive) {
            this.ready = false;
            return;
        }
        if (this.attachedFramebuffer > 0) {
            GlStateManager._glBindFramebuffer((int)36160, (int)this.attachedFramebuffer);
            GL20.glDrawBuffers((int[])new int[]{36064});
            GL30.glFramebufferTexture2D((int)36160, (int)36065, (int)3553, (int)0, (int)0);
        }
        this.ready = successful && this.textureId != 0 && this.width > 0 && this.height > 0;
        this.captureActive = false;
        this.attachedFramebuffer = 0;
    }

    boolean captureActive() {
        return this.captureActive;
    }

    boolean ready() {
        return this.ready && this.textureId != 0 && this.width > 0 && this.height > 0;
    }

    int textureId() {
        return this.ready() ? this.textureId : -1;
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    void invalidate() {
        this.ready = false;
        this.captureActive = false;
        this.attachedFramebuffer = 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void ensure(int requestedWidth, int requestedHeight) {
        int newWidth = Math.max(1, requestedWidth);
        int newHeight = Math.max(1, requestedHeight);
        if (this.textureId != 0 && this.width == newWidth && this.height == newHeight) {
            return;
        }
        this.destroyTexture();
        this.width = newWidth;
        this.height = newHeight;
        this.textureId = GlStateManager._genTexture();
        int previousTexture2D = GL11.glGetInteger((int)32873);
        try {
            GlStateManager._bindTexture((int)this.textureId);
            GL11.glTexImage2D((int)3553, (int)0, (int)34842, (int)this.width, (int)this.height, (int)0, (int)6408, (int)5126, (long)0L);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        }
        finally {
            GlStateManager._bindTexture((int)previousTexture2D);
        }
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        this.destroyTexture();
    }

    private void destroyTexture() {
        if (this.textureId != 0) {
            GlStateManager._deleteTexture((int)this.textureId);
            this.textureId = 0;
        }
        this.width = 0;
        this.height = 0;
        this.ready = false;
        this.captureActive = false;
        this.attachedFramebuffer = 0;
    }
}

