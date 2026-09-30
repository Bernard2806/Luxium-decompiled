/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.ConfirmLinkScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 */
package com.vinlanx.luxium.client.guiscreen;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.guiscreen.LuxiumMenuBackgroundRenderer;
import java.util.List;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class AlphaWarningScreen
extends Screen {
    private static final int CONTINUE_LOCK_TICKS = 200;
    private static final int MAX_TEXT_WIDTH = 480;
    private static final int LOGO_SIZE = 84;
    private static final int SCREEN_MARGIN = 8;
    private static final float MIN_CONTENT_SCALE = 0.4f;
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath((String)"luxium", (String)"textures/logow.png");
    private int ticksOpen = 0;
    private final LuxiumMenuBackgroundRenderer backgroundRenderer = new LuxiumMenuBackgroundRenderer();
    private Button continueButton;
    private Button youtubeButton;
    private int controlsTop;

    public AlphaWarningScreen() {
        super((Component)Component.m_237115_((String)"screen.luxium.alpha_warning.title"));
    }

    public static boolean shouldShow() {
        return (Boolean)Config.CLIENT.alphaWarningShown.get() == false;
    }

    protected void m_7856_() {
        this.continueButton = Button.m_253074_((Component)Component.m_237115_((String)"screen.luxium.alpha_warning.continue"), btn -> this.acknowledge()).m_252987_(0, 0, 136, 20).m_253136_();
        this.continueButton.f_93623_ = false;
        this.m_142416_((GuiEventListener)this.continueButton);
        this.youtubeButton = Button.m_253074_((Component)Component.m_237115_((String)"screen.luxium.alpha_warning.youtube"), btn -> this.openYoutube()).m_252987_(0, 0, 62, 20).m_253136_();
        this.m_142416_((GuiEventListener)this.youtubeButton);
        this.layoutControls();
    }

    public void m_86600_() {
        ++this.ticksOpen;
        this.backgroundRenderer.tick();
        if (this.ticksOpen >= 200) {
            this.continueButton.f_93623_ = true;
            this.continueButton.m_93666_((Component)Component.m_237115_((String)"screen.luxium.alpha_warning.continue"));
        } else {
            int remaining = (200 - this.ticksOpen + 19) / 20;
            this.continueButton.m_93666_((Component)Component.m_237110_((String)"screen.luxium.alpha_warning.continue_wait", (Object[])new Object[]{remaining}));
        }
    }

    public void m_88315_(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int h = this.f_96544_;
        this.backgroundRenderer.render(gfx, this.f_96543_, h, mouseX, mouseY, partialTick);
        float contentScale = this.calculateContentScale();
        int virtualWidth = Math.max(1, Mth.m_14143_((float)((float)this.f_96543_ / contentScale)));
        int cx = virtualWidth / 2;
        int textWidth = Math.min(480, Math.max(120, Mth.m_14143_((float)((float)(this.f_96543_ - 16) / contentScale))));
        int textY = Mth.m_14167_((float)(8.0f / contentScale));
        gfx.m_280168_().m_85836_();
        gfx.m_280168_().m_85841_(contentScale, contentScale, 1.0f);
        gfx.m_280398_(LOGO, cx - 42, textY, 0, 0.0f, 0.0f, 84, 84, 84, 84);
        MutableComponent titleComp = Component.m_237115_((String)"screen.luxium.alpha_warning.mod_title");
        int titleW = this.f_96547_.m_92852_((FormattedText)titleComp);
        gfx.m_280168_().m_85836_();
        gfx.m_280168_().m_85841_(2.0f, 2.0f, 1.0f);
        gfx.m_280614_(this.f_96547_, (Component)titleComp, cx / 2 - titleW / 2, (textY += 92) / 2, -462632, true);
        gfx.m_280168_().m_85849_();
        Objects.requireNonNull(this.f_96547_);
        int dividerHalfWidth = Math.min(160, textWidth / 2);
        gfx.m_280509_(cx - dividerHalfWidth, textY += 9 * 2 + 14, cx + dividerHalfWidth, textY + 1, 1442829419);
        textY += 9;
        for (Component line : AlphaWarningScreen.bodyText()) {
            List wrapped = this.f_96547_.m_92923_((FormattedText)line, textWidth);
            for (FormattedCharSequence seq : wrapped) {
                int lineW = this.f_96547_.m_92724_(seq);
                gfx.m_280649_(this.f_96547_, seq, cx - lineW / 2, textY, -1317421, false);
                Objects.requireNonNull(this.f_96547_);
                textY += 9 + 2;
            }
            textY += 2;
        }
        MutableComponent ytHint = Component.m_237115_((String)"screen.luxium.alpha_warning.youtube_hint");
        for (FormattedCharSequence seq : this.f_96547_.m_92923_((FormattedText)ytHint, textWidth)) {
            int lineW = this.f_96547_.m_92724_(seq);
            gfx.m_280649_(this.f_96547_, seq, cx - lineW / 2, textY, -6303489, false);
            Objects.requireNonNull(this.f_96547_);
            textY += 9 + 1;
        }
        gfx.m_280168_().m_85849_();
        super.m_88315_(gfx, mouseX, mouseY, partialTick);
    }

    private void layoutControls() {
        int buttonHeight = 20;
        int gap = 6;
        if (this.f_96543_ >= 220) {
            int youtubeWidth = Mth.m_14045_((int)(this.f_96543_ / 8), (int)62, (int)76);
            int continueWidth = Mth.m_14045_((int)(this.f_96543_ / 5), (int)112, (int)150);
            int totalWidth = continueWidth + gap + youtubeWidth;
            int startX = (this.f_96543_ - totalWidth) / 2;
            this.controlsTop = Math.max(8, this.f_96544_ - buttonHeight - 8);
            this.continueButton.m_252865_(startX);
            this.continueButton.m_253211_(this.controlsTop);
            this.continueButton.m_93674_(continueWidth);
            this.youtubeButton.m_252865_(startX + continueWidth + gap);
            this.youtubeButton.m_253211_(this.controlsTop);
            this.youtubeButton.m_93674_(youtubeWidth);
            return;
        }
        int buttonWidth = Math.max(80, this.f_96543_ - 16);
        this.controlsTop = Math.max(8, this.f_96544_ - buttonHeight * 2 - gap - 8);
        this.continueButton.m_252865_((this.f_96543_ - buttonWidth) / 2);
        this.continueButton.m_253211_(this.controlsTop);
        this.continueButton.m_93674_(buttonWidth);
        this.youtubeButton.m_252865_((this.f_96543_ - buttonWidth) / 2);
        this.youtubeButton.m_253211_(this.controlsTop + buttonHeight + gap);
        this.youtubeButton.m_93674_(buttonWidth);
    }

    private float calculateContentScale() {
        int textWidth;
        float requiredHeight;
        float availableHeight = Math.max(1.0f, (float)this.controlsTop - 16.0f);
        float scale = Mth.m_14036_((float)Math.min((float)this.f_96543_ / 560.0f, availableHeight / 300.0f), (float)0.4f, (float)1.0f);
        for (int i = 0; i < 2 && !((requiredHeight = this.contentHeight(textWidth = Math.min(480, Math.max(120, Mth.m_14143_((float)((float)(this.f_96543_ - 16) / scale)))), scale)) * scale <= availableHeight); ++i) {
            scale = Math.max(0.4f, scale * availableHeight / (requiredHeight * scale));
        }
        return scale;
    }

    private float contentHeight(int textWidth, float scale) {
        float f = 8.0f / scale + 84.0f + 8.0f;
        Objects.requireNonNull(this.f_96547_);
        float height = f + (float)(9 * 2) + 23.0f;
        for (Component line : AlphaWarningScreen.bodyText()) {
            int n = this.f_96547_.m_92923_((FormattedText)line, textWidth).size();
            Objects.requireNonNull(this.f_96547_);
            height += (float)(n * (9 + 2) + 2);
        }
        MutableComponent hint = Component.m_237115_((String)"screen.luxium.alpha_warning.youtube_hint");
        int n = this.f_96547_.m_92923_((FormattedText)hint, textWidth).size();
        Objects.requireNonNull(this.f_96547_);
        return height += (float)(n * (9 + 1));
    }

    private static List<Component> bodyText() {
        return List.of(Component.m_237115_((String)"screen.luxium.alpha_warning.line1"), Component.m_237115_((String)"screen.luxium.alpha_warning.line2"), Component.m_237115_((String)"screen.luxium.alpha_warning.line3"), Component.m_237115_((String)"screen.luxium.alpha_warning.line4"), Component.m_237115_((String)"screen.luxium.alpha_warning.line5"));
    }

    private void acknowledge() {
        Config.CLIENT.alphaWarningShown.set((Object)true);
        Config.CLIENT.alphaWarningShown.save();
        this.m_7379_();
    }

    private void openYoutube() {
        Minecraft.m_91087_().m_91152_((Screen)new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.m_137581_().m_137646_("https://www.youtube.com/@Vinlanx");
            }
            Minecraft.m_91087_().m_91152_((Screen)this);
        }, "https://www.youtube.com/@Vinlanx", false));
    }

    public void m_7379_() {
        Minecraft.m_91087_().m_91152_(null);
    }

    public boolean m_7043_() {
        return false;
    }
}

