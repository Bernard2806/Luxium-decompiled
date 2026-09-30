/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
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
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL30
 */
package com.vinlanx.luxium.client.ConfigScreen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vinlanx.luxium.client.ShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;

final class GlassPanelRenderer
implements AutoCloseable {
    private TextureTarget sceneCopy;

    GlassPanelRenderer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean render(int x, int y, int width, int height, int screenWidth, int screenHeight, float blurAmount, float dimmingAmount) {
        Minecraft minecraft = Minecraft.m_91087_();
        RenderTarget main = minecraft.m_91385_();
        ShaderInstance shader = ShaderManager.getConfigGlassShader();
        if (shader == null || main.f_83917_ <= 0 || main.f_83918_ <= 0) {
            return false;
        }
        this.ensureTarget(main);
        GL30.glBindFramebuffer((int)36008, (int)main.f_83920_);
        GL30.glBindFramebuffer((int)36009, (int)this.sceneCopy.f_83920_);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.f_83915_, (int)main.f_83916_, (int)0, (int)0, (int)this.sceneCopy.f_83915_, (int)this.sceneCopy.f_83916_, (int)16384, (int)9728);
        main.m_83947_(true);
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix4f projection = new Matrix4f().setOrtho(0.0f, (float)screenWidth, (float)screenHeight, 0.0f, 1000.0f, 3000.0f);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.m_85836_();
        modelView.m_166856_();
        modelView.m_252880_(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            RenderSystem.setProjectionMatrix((Matrix4f)projection, (VertexSorting)VertexSorting.f_276633_);
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(new Matrix4f().translation(0.0f, 0.0f, -2000.0f));
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(projection);
            }
            shader.m_173350_("SceneSampler", (Object)this.sceneCopy.m_83975_());
            if (shader.m_173348_("ScreenSize") != null) {
                shader.m_173348_("ScreenSize").m_7971_((float)main.f_83915_, (float)main.f_83916_);
            }
            if (shader.m_173348_("BlurEnabled") != null) {
                shader.m_173348_("BlurEnabled").m_5985_(blurAmount);
            }
            if (shader.m_173348_("DimmingEnabled") != null) {
                shader.m_173348_("DimmingEnabled").m_5985_(dimmingAmount);
            }
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            shader.m_173363_();
            BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_((double)x, (double)(y + height), 0.0).m_7421_((float)x / (float)screenWidth, 1.0f - (float)(y + height) / (float)screenHeight).m_5752_();
            buffer.m_5483_((double)(x + width), (double)(y + height), 0.0).m_7421_((float)(x + width) / (float)screenWidth, 1.0f - (float)(y + height) / (float)screenHeight).m_5752_();
            buffer.m_5483_((double)(x + width), (double)y, 0.0).m_7421_((float)(x + width) / (float)screenWidth, 1.0f - (float)y / (float)screenHeight).m_5752_();
            buffer.m_5483_((double)x, (double)y, 0.0).m_7421_((float)x / (float)screenWidth, 1.0f - (float)y / (float)screenHeight).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            shader.m_173362_();
            modelView.m_85849_();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            RenderSystem.viewport((int)0, (int)0, (int)minecraft.m_91268_().m_85441_(), (int)minecraft.m_91268_().m_85442_());
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.defaultBlendFunc();
            main.m_83947_(true);
        }
        return true;
    }

    private void ensureTarget(RenderTarget main) {
        if (this.sceneCopy == null) {
            this.sceneCopy = new TextureTarget(main.f_83915_, main.f_83916_, false, Minecraft.f_91002_);
            this.sceneCopy.m_83936_(9729);
        } else if (this.sceneCopy.f_83915_ != main.f_83915_ || this.sceneCopy.f_83916_ != main.f_83916_) {
            this.sceneCopy.m_83941_(main.f_83915_, main.f_83916_, Minecraft.f_91002_);
        }
    }

    @Override
    public void close() {
        if (this.sceneCopy != null) {
            this.sceneCopy.m_83930_();
            this.sceneCopy = null;
        }
    }
}

