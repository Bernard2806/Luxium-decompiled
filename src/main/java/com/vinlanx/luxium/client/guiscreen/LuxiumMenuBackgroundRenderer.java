/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 */
package com.vinlanx.luxium.client.guiscreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class LuxiumMenuBackgroundRenderer {
    private static final ResourceLocation[] BACKGROUNDS = new ResourceLocation[]{LuxiumMenuBackgroundRenderer.image("1.png"), LuxiumMenuBackgroundRenderer.image("2.png"), LuxiumMenuBackgroundRenderer.image("3.png"), LuxiumMenuBackgroundRenderer.image("4.png"), LuxiumMenuBackgroundRenderer.image("5.png"), LuxiumMenuBackgroundRenderer.image("6.png")};
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;
    private static final float SLIDE_DURATION_TICKS = 100.0f;
    private static final float CROSSFADE_TICKS = 20.0f;
    private static final float SLIDE_LIFETIME_TICKS = 120.0f;
    private static final float START_ZOOM = 1.05f;
    private static final float END_ZOOM = 1.07f;
    private static final float PARALLAX_X = 26.0f;
    private static final float PARALLAX_Y = 18.0f;
    private int ticks;
    private float parallaxX;
    private float parallaxY;

    public void tick() {
        ++this.ticks;
    }

    public void render(GuiGraphics graphics, int width, int height, int mouseX, int mouseY, float partialTick) {
        this.updateParallax(width, height, mouseX, mouseY);
        float time = (float)this.ticks + partialTick;
        int slideIndex = Mth.m_14143_((float)(time / 100.0f));
        float localTicks = time - (float)slideIndex * 100.0f;
        float fadeStart = 80.0f;
        ResourceLocation current = BACKGROUNDS[Math.floorMod(slideIndex, BACKGROUNDS.length)];
        ResourceLocation next = BACKGROUNDS[Math.floorMod(slideIndex + 1, BACKGROUNDS.length)];
        float currentAgeTicks = slideIndex == 0 ? localTicks : localTicks + 20.0f;
        float currentLifetime = slideIndex == 0 ? 100.0f : 120.0f;
        float currentProgress = Mth.m_14036_((float)(currentAgeTicks / currentLifetime), (float)0.0f, (float)1.0f);
        graphics.m_280509_(0, 0, width, height, -16381942);
        if (localTicks < fadeStart) {
            this.drawSlide(current, 1.0f, currentProgress, width, height);
        } else {
            float fadeTicks = localTicks - fadeStart;
            float blend = LuxiumMenuBackgroundRenderer.smootherStep(fadeTicks / 20.0f);
            float nextProgress = Mth.m_14036_((float)(fadeTicks / 120.0f), (float)0.0f, (float)1.0f);
            this.drawSlide(current, 1.0f - blend, currentProgress, width, height);
            this.drawSlide(next, blend, nextProgress, width, height);
        }
        RenderSystem.disableBlend();
        graphics.m_280024_(0, 0, width, height, 1543833354, 2114587664);
        graphics.m_280509_(0, 0, width, height, 0x29000000);
        graphics.m_280024_(0, height - 120, width, height, 0, 0x7E000000);
    }

    private void updateParallax(int width, int height, int mouseX, int mouseY) {
        float targetX = ((float)mouseX / (float)Math.max(1, width) - 0.5f) * 26.0f;
        float targetY = ((float)mouseY / (float)Math.max(1, height) - 0.5f) * 18.0f;
        this.parallaxX = Mth.m_14179_((float)0.08f, (float)this.parallaxX, (float)targetX);
        this.parallaxY = Mth.m_14179_((float)0.08f, (float)this.parallaxY, (float)targetY);
    }

    private void drawSlide(ResourceLocation texture, float alpha, float progress, int width, int height) {
        if (alpha <= 0.001f) {
            return;
        }
        float zoom = Mth.m_14179_((float)Mth.m_14036_((float)progress, (float)0.0f, (float)1.0f), (float)1.05f, (float)1.07f);
        float coverScale = Math.max((float)width / 1920.0f, (float)height / 1080.0f);
        float drawWidth = 1920.0f * coverScale * zoom;
        float drawHeight = 1080.0f * coverScale * zoom;
        float drawX = ((float)width - drawWidth) * 0.5f - this.parallaxX;
        float drawY = ((float)height - drawHeight) * 0.5f - this.parallaxY;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::m_172817_);
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)texture);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)alpha);
        BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
        buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        buffer.m_5483_((double)drawX, (double)(drawY + drawHeight), 0.0).m_7421_(0.0f, 1.0f).m_5752_();
        buffer.m_5483_((double)(drawX + drawWidth), (double)(drawY + drawHeight), 0.0).m_7421_(1.0f, 1.0f).m_5752_();
        buffer.m_5483_((double)(drawX + drawWidth), (double)drawY, 0.0).m_7421_(1.0f, 0.0f).m_5752_();
        buffer.m_5483_((double)drawX, (double)drawY, 0.0).m_7421_(0.0f, 0.0f).m_5752_();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static float smootherStep(float value) {
        float t = Mth.m_14036_((float)value, (float)0.0f, (float)1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    private static ResourceLocation image(String name) {
        return ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)("ekrannn/" + name));
    }
}

