/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.system.MemoryStack
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class NeoSkyDepthFramebuffer
implements AutoCloseable {
    private static final int GL_CLAMP_TO_BORDER = 33069;
    private static final int GL_TEXTURE_COMPARE_MODE = 34892;
    private static final int GL_TEXTURE_COMPARE_FUNC = 34893;
    private static final int GL_COMPARE_REF_TO_TEXTURE = 34894;
    private final boolean comparisonSampling;
    private boolean linearFiltering;
    private int framebufferId;
    private int depthTextureId;
    private int size;

    public NeoSkyDepthFramebuffer() {
        this(false);
    }

    public NeoSkyDepthFramebuffer(boolean comparisonSampling) {
        this.comparisonSampling = comparisonSampling;
    }

    public void resize(int requestedSize) {
        RenderSystem.assertOnRenderThread();
        int newSize = Math.max(16, requestedSize);
        if (this.framebufferId != 0 && this.size == newSize) {
            return;
        }
        int previousFramebuffer = GL11.glGetInteger((int)36006);
        int previousTexture = GL11.glGetInteger((int)32873);
        try (MemoryStack stack = MemoryStack.stackPush();){
            IntBuffer previousViewport = stack.mallocInt(4);
            GL11.glGetIntegerv((int)2978, (IntBuffer)previousViewport);
            int viewportX = previousViewport.get(0);
            int viewportY = previousViewport.get(1);
            int viewportWidth = previousViewport.get(2);
            int viewportHeight = previousViewport.get(3);
            this.allocate(newSize);
            GlStateManager._bindTexture((int)previousTexture);
            GL30.glBindFramebuffer((int)36160, (int)previousFramebuffer);
            GL11.glViewport((int)viewportX, (int)viewportY, (int)viewportWidth, (int)viewportHeight);
        }
    }

    private void allocate(int newSize) {
        this.destroy();
        this.size = newSize;
        this.framebufferId = GL30.glGenFramebuffers();
        this.depthTextureId = GlStateManager._genTexture();
        GL30.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GlStateManager._bindTexture((int)this.depthTextureId);
        GL11.glTexImage2D((int)3553, (int)0, (int)33190, (int)newSize, (int)newSize, (int)0, (int)6402, (int)5126, (long)0L);
        int textureFilter = this.linearFiltering ? 9729 : 9728;
        GL11.glTexParameteri((int)3553, (int)10241, (int)textureFilter);
        GL11.glTexParameteri((int)3553, (int)10240, (int)textureFilter);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33069);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33069);
        try (MemoryStack stack = MemoryStack.stackPush();){
            FloatBuffer border = stack.floats(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glTexParameterfv((int)3553, (int)4100, (FloatBuffer)border);
        }
        GL11.glTexParameteri((int)3553, (int)34892, (int)(this.comparisonSampling ? 34894 : 0));
        if (this.comparisonSampling) {
            GL11.glTexParameteri((int)3553, (int)34893, (int)515);
        }
        GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)this.depthTextureId, (int)0);
        GL11.glDrawBuffer((int)0);
        GL11.glReadBuffer((int)0);
        GL11.glClearDepth((double)1.0);
        int status = GL30.glCheckFramebufferStatus((int)36160);
        if (status != 36053) {
            this.destroy();
            throw new IllegalStateException("Luxium depth framebuffer is incomplete: 0x" + Integer.toHexString(status));
        }
        GlStateManager._bindTexture((int)0);
    }

    public void bindAndClear() {
        RenderSystem.assertOnRenderThread();
        if (this.framebufferId == 0) {
            throw new IllegalStateException("Depth framebuffer is not allocated");
        }
        this.bind();
        GL11.glClear((int)256);
    }

    public void bind() {
        RenderSystem.assertOnRenderThread();
        if (this.framebufferId == 0) {
            throw new IllegalStateException("Depth framebuffer is not allocated");
        }
        GL30.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GL11.glViewport((int)0, (int)0, (int)this.size, (int)this.size);
    }

    public void setLinearFiltering(boolean linearFiltering) {
        RenderSystem.assertOnRenderThread();
        if (this.linearFiltering == linearFiltering) {
            return;
        }
        this.linearFiltering = linearFiltering;
        if (this.depthTextureId == 0) {
            return;
        }
        int previousTexture = GL11.glGetInteger((int)32873);
        GlStateManager._bindTexture((int)this.depthTextureId);
        int textureFilter = this.linearFiltering ? 9729 : 9728;
        GL11.glTexParameteri((int)3553, (int)10241, (int)textureFilter);
        GL11.glTexParameteri((int)3553, (int)10240, (int)textureFilter);
        GlStateManager._bindTexture((int)previousTexture);
    }

    public int depthTextureId() {
        return this.depthTextureId;
    }

    public int size() {
        return this.size;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        this.destroy();
    }

    private void destroy() {
        if (this.depthTextureId != 0) {
            GlStateManager._deleteTexture((int)this.depthTextureId);
            this.depthTextureId = 0;
        }
        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers((int)this.framebufferId);
            this.framebufferId = 0;
        }
        this.size = 0;
    }
}

