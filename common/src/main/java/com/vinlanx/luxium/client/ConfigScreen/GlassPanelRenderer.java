package com.vinlanx.luxium.client.ConfigScreen; import com.mojang.blaze3d.pipeline.RenderTarget; import com.mojang.blaze3d.pipeline.TextureTarget;
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
    GlassPanelRenderer() { }




    boolean render(int x, int y, int width, int height, int screenWidth, int screenHeight, float blurAmount, float dimmingAmount) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        ShaderInstance shader = ShaderManager.getConfigGlassShader();
        if (shader == null || main.viewWidth <= 0 || main.viewHeight <= 0) { return false;
        }
        this.ensureTarget(main);
        GL30.glBindFramebuffer((int)36008, (int)main.frameBufferId);
        GL30.glBindFramebuffer((int)36009, (int)this.sceneCopy.frameBufferId);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)main.width, (int)main.height, (int)0, (int)0, (int)this.sceneCopy.width, (int)this.sceneCopy.height, (int)16384, (int)9728);

        main.bindWrite(true);

        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix4f projection = new Matrix4f().setOrtho(0.0f, (float)screenWidth, (float)screenHeight, 0.0f, 1000.0f, 3000.0f);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();

        try {
            RenderSystem.setProjectionMatrix((Matrix4f)projection, (VertexSorting)VertexSorting.ORTHOGRAPHIC_Z);
            if (shader.MODEL_VIEW_MATRIX != null) { shader.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0f, 0.0f, -2000.0f)); }
            if (shader.PROJECTION_MATRIX != null) { shader.PROJECTION_MATRIX.set(projection); }
            shader.setSampler("SceneSampler", this.sceneCopy.getColorTextureId());
            if (shader.getUniform("ScreenSize") != null) { shader.getUniform("ScreenSize").set((float)main.width, (float)main.height); }
            if (shader.getUniform("BlurEnabled") != null) { shader.getUniform("BlurEnabled").set(blurAmount); }
            if (shader.getUniform("DimmingEnabled") != null) { shader.getUniform("DimmingEnabled").set(dimmingAmount);
            }
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            shader.apply();

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex((double)x, (double)(y + height), 0.0).uv((float)x / (float)screenWidth, 1.0f - (float)(y + height) / (float)screenHeight).endVertex();
            buffer.vertex((double)(x + width), (double)(y + height), 0.0).uv((float)(x + width) / (float)screenWidth, 1.0f - (float)(y + height) / (float)screenHeight).endVertex();
            buffer.vertex((double)(x + width), (double)y, 0.0).uv((float)(x + width) / (float)screenWidth, 1.0f - (float)y / (float)screenHeight).endVertex();
            buffer.vertex((double)x, (double)y, 0.0).uv((float)x / (float)screenWidth, 1.0f - (float)y / (float)screenHeight).endVertex();
            BufferUploader.draw((BufferBuilder.RenderedBuffer)buffer.end()); }
        finally {
            shader.clear();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
            RenderSystem.viewport((int)0, (int)0, (int)minecraft.getWindow().getWidth(), (int)minecraft.getWindow().getHeight());
            GlStateManager._depthMask((boolean)true);
            GlStateManager._enableDepthTest();
            RenderSystem.defaultBlendFunc();
            main.bindWrite(true);
        }
        return true;
    }

    private void ensureTarget(RenderTarget main) {
        if (this.sceneCopy == null) {
            this.sceneCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            this.sceneCopy.setFilterMode(9729);
        } else if (this.sceneCopy.width != main.width || this.sceneCopy.height != main.height) {
            this.sceneCopy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
    }

    @Override
    public void close() {
        if (this.sceneCopy != null) {
            this.sceneCopy.destroyBuffers();
            this.sceneCopy = null;
        }
    }
}
