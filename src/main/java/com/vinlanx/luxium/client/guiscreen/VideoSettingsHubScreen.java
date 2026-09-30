/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.Util
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.layouts.FrameLayout
 *  net.minecraft.client.gui.layouts.GridLayout
 *  net.minecraft.client.gui.layouts.GridLayout$RowHelper
 *  net.minecraft.client.gui.layouts.LayoutElement
 *  net.minecraft.client.gui.narration.NarrationElementOutput
 *  net.minecraft.client.gui.screens.ConfirmLinkScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 */
package com.vinlanx.luxium.client.guiscreen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vinlanx.luxium.client.ConfigScreen.LuxiumConfigScreen;
import com.vinlanx.luxium.client.ShaderManager;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class VideoSettingsHubScreen
extends Screen {
    private static final Component TITLE = Component.m_237115_((String)"options.videoTitle");
    private static final Component VIDEO_SETTINGS = Component.m_237115_((String)"options.video");
    private static final Component LUXIUM = Component.m_237113_((String)"Luxium");
    private static final String YOUTUBE_URL = "https://www.youtube.com/@Vinlanx";
    private final Screen parent;
    private final Supplier<Screen> videoScreenFactory;
    private YouTubeButton youTubeButton;

    public VideoSettingsHubScreen(Screen parent, Supplier<Screen> videoScreenFactory) {
        super(TITLE);
        this.parent = parent;
        this.videoScreenFactory = videoScreenFactory;
    }

    protected void m_7856_() {
        GridLayout layout = new GridLayout();
        layout.m_264211_().m_264215_(5).m_264154_(4).m_264356_();
        GridLayout.RowHelper rowHelper = layout.m_264606_(2);
        rowHelper.m_264139_((LayoutElement)Button.m_253074_((Component)VIDEO_SETTINGS, button -> this.f_96541_.m_91152_(this.videoScreenFactory.get())).m_252780_(150).m_253136_());
        rowHelper.m_264139_((LayoutElement)Button.m_253074_((Component)LUXIUM, button -> this.f_96541_.m_91152_((Screen)new LuxiumConfigScreen(this))).m_252780_(150).m_253136_());
        rowHelper.m_264276_((LayoutElement)Button.m_253074_((Component)CommonComponents.f_130655_, button -> this.m_7379_()).m_252780_(200).m_253136_(), 2, rowHelper.m_264551_().m_264311_(6));
        layout.m_264036_();
        FrameLayout.m_264460_((LayoutElement)layout, (int)0, (int)(this.f_96544_ / 6), (int)this.f_96543_, (int)this.f_96544_, (float)0.5f, (float)0.0f);
        layout.m_264134_(x$0 -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.m_142416_((GuiEventListener)x$0);
        });
        this.youTubeButton = (YouTubeButton)this.m_142416_((GuiEventListener)new YouTubeButton(8, 8, this::openYouTube));
    }

    public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(guiGraphics);
        guiGraphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 20, 0xFFFFFF);
        super.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void m_7379_() {
        this.f_96541_.m_91152_(this.parent);
    }

    private void openYouTube() {
        this.f_96541_.m_91152_((Screen)new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.m_137581_().m_137646_(YOUTUBE_URL);
            }
            this.f_96541_.m_91152_((Screen)this);
        }, YOUTUBE_URL, false));
    }

    private static final class YouTubeButton
    extends AbstractWidget {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"textures/youtube.png");
        private static final int WIDTH = 64;
        private static final float TEXTURE_ASPECT = 0.9609375f;
        private final Runnable action;
        private float hover;

        YouTubeButton(int x, int y, Runnable action) {
            super(x, y, 64, Math.round(61.5f), (Component)Component.m_237115_((String)"gui.luxium.youtube"));
            this.action = action;
        }

        public void m_5716_(double mouseX, double mouseY) {
            this.action.run();
        }

        protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.hover = YouTubeButton.animate(this.hover, this.m_274382_() ? 1.0f : 0.0f, 0.18f);
            ShaderInstance shader = ShaderManager.getYouShader();
            if (shader == null) {
                graphics.m_280163_(TEXTURE, this.m_252754_(), this.m_252907_(), 0.0f, 0.0f, this.m_5711_(), this.m_93694_(), this.m_5711_(), this.m_93694_());
                return;
            }
            float scale = 1.0f + this.hover * 0.08f;
            float centerX = (float)this.m_252754_() + (float)this.m_5711_() / 2.0f;
            float centerY = (float)this.m_252907_() + (float)this.m_93694_() / 2.0f;
            float glowTime = (float)(System.nanoTime() % 4000000000L) / 1.0E9f;
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)TEXTURE);
            shader.m_173350_("Sampler0", (Object)RenderSystem.getShaderTexture((int)0));
            if (shader.f_173308_ != null) {
                shader.f_173308_.m_5679_(RenderSystem.getModelViewMatrix());
            }
            if (shader.f_173309_ != null) {
                shader.f_173309_.m_5679_(RenderSystem.getProjectionMatrix());
            }
            if (shader.m_173348_("Center") != null) {
                shader.m_173348_("Center").m_7971_(centerX, centerY);
            }
            if (shader.m_173348_("ScaleAmount") != null) {
                shader.m_173348_("ScaleAmount").m_5985_(scale);
            }
            if (shader.m_173348_("HoverAmount") != null) {
                shader.m_173348_("HoverAmount").m_5985_(this.hover);
            }
            if (shader.m_173348_("GlowTime") != null) {
                shader.m_173348_("GlowTime").m_5985_(glowTime);
            }
            GlStateManager._disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            shader.m_173363_();
            BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            buffer.m_5483_((double)this.m_252754_(), (double)(this.m_252907_() + this.m_93694_()), 0.0).m_7421_(0.0f, 1.0f).m_5752_();
            buffer.m_5483_((double)(this.m_252754_() + this.m_5711_()), (double)(this.m_252907_() + this.m_93694_()), 0.0).m_7421_(1.0f, 1.0f).m_5752_();
            buffer.m_5483_((double)(this.m_252754_() + this.m_5711_()), (double)this.m_252907_(), 0.0).m_7421_(1.0f, 0.0f).m_5752_();
            buffer.m_5483_((double)this.m_252754_(), (double)this.m_252907_(), 0.0).m_7421_(0.0f, 0.0f).m_5752_();
            BufferUploader.m_231209_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
            shader.m_173362_();
            GlStateManager._enableDepthTest();
        }

        protected void m_168797_(NarrationElementOutput output) {
            this.m_168802_(output);
        }

        private static float animate(float current, float target, float speed) {
            float next = current + (target - current) * speed;
            return Math.abs(next - target) < 0.001f ? target : next;
        }
    }
}

