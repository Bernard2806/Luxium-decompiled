package com.vinlanx.luxium.client.guiscreen;

import com.vinlanx.luxium.Config;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import com.vinlanx.luxium.client.guiscreen.LuxiumMenuBackgroundRenderer;
import java.util.Objects;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public class AlphaWarningScreen extends Screen {
    private static final int CONTINUE_LOCK_TICKS = 200;
    private static final int MAX_TEXT_WIDTH = 480; private static final int LOGO_SIZE = 84; private static final int SCREEN_MARGIN = 8; private static final float MIN_CONTENT_SCALE = 0.4F; private static final ResourceLocation LOGO = new ResourceLocation("luxium", "textures/logow.png");


    private int ticksOpen = 0;
    private final LuxiumMenuBackgroundRenderer backgroundRenderer = new LuxiumMenuBackgroundRenderer();
    private Button continueButton;
    private Button youtubeButton;
    private int controlsTop;

    public AlphaWarningScreen() {

        super(Component.translatable("screen.luxium.alpha_warning.title"));
    }

    public static boolean shouldShow() {
        return !Config.CLIENT.alphaWarningShown.get();
    }

    @Override protected void init() {

        this.continueButton = Button.builder(Component.translatable
("screen.luxium.alpha_warning.continue"), btn ->
this.acknowledge())
            .bounds(0, 0, 136, 20)
            .build();
        this.continueButton.active = false;
        this.addRenderableWidget(this.continueButton);

        this.youtubeButton = Button.builder(Component.translatable
("screen.luxium.alpha_warning.youtube"), btn ->
this.openYoutube())
            .bounds(0, 0, 62, 20)
            .build();
        this.addRenderableWidget(this.youtubeButton);

        this.layoutControls();
    }

    @Override public void tick() {

        this.ticksOpen++;
        this.backgroundRenderer.tick();
        if (this.ticksOpen >= 200) {
            this.continueButton.active = true;
            this.continueButton.setMessage(Component.translatable("screen.luxium.alpha_warning.continue"));
        } else {
            int remaining = (200 - this.ticksOpen + 19) / 20;
            this.continueButton.setMessage(Component.translatable("screen.luxium.alpha_warning.continue_wait", remaining));
        }
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int
        h = this.height;

        this.backgroundRenderer.render(gfx, this.width, h, mouseX, mouseY, partialTick);

        float contentScale = this.calculateContentScale();
        int virtualWidth = Math.max(1, Mth.floor((float)((float)this.width / contentScale)));
        int cx = virtualWidth / 2;
        int textWidth = Math.min(480, Math.max
                (120, Mth.floor((float)((float)(this.width - 16) / contentScale))));
        int textY = Mth.ceil((float)(8.0f / contentScale));

        gfx.pose().pushPose();
        gfx.pose().scale(contentScale, contentScale, 1.0f);

        gfx.blit(LOGO, cx - 42, textY, 0, 0.0f, 0.0f, 84, 84, 84, 84);

        textY += 92;

        Component titleComp = Component.translatable((String)"screen.luxium.alpha_warning.mod_title");
        int titleW = this.font.width((FormattedText)titleComp);
        gfx.pose().pushPose();
        gfx.pose().scale(2.0f, 2.0f, 1.0f);
        gfx.drawString(this.font, (Component)titleComp, cx / 2 - titleW / 2, textY / 2, -462632, true);
        gfx.pose().popPose();
        textY += this.font.lineHeight * 2 + 14;

        int dividerHalfWidth = Math.min(160, textWidth / 2);
        gfx.fill(cx - dividerHalfWidth, textY, cx + dividerHalfWidth, textY + 1, 1442829419);
        textY += 9;

        for (Component line : AlphaWarningScreen.bodyText()) {
            List<FormattedCharSequence> wrapped = this.font.split
                    ((FormattedText)line, textWidth);
            for (FormattedCharSequence seq : wrapped) {
                int lineW = this.font.width(seq);
                gfx.drawString(this.font, seq, cx - lineW / 2, textY, -1317421, false);
                textY += this.font.lineHeight + 2;
            }
            textY += 2;
        }

        Component ytHint = Component.translatable((String)"screen.luxium.alpha_warning.youtube_hint");
        for (FormattedCharSequence seq : this.font.split((FormattedText)ytHint, textWidth)) {
            int lineW = this.font.width(seq);
            gfx.drawString(this.font, seq, cx - lineW / 2, textY, -6303489, false);
            textY += this.font.lineHeight + 1;
        }

        gfx.pose().popPose();

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void layoutControls() {
        int buttonHeight = 20;
        int gap = 6;
        if (this.width >= 220) {
            int youtubeWidth = Mth.clamp((int)(this.width / 8), (int)62, (int)76);
            int continueWidth = Mth.clamp((int)(this.width / 5), (int)112, (int)150);
            int totalWidth = continueWidth + gap + youtubeWidth;
            int startX = (this.width - totalWidth) / 2;
            this.controlsTop = Math.max(8, this.height - buttonHeight - 8);
            this.continueButton.setX(startX);
            this.continueButton.setY(this.controlsTop);
            this.continueButton.setWidth(continueWidth);
            this.youtubeButton.setX(startX + continueWidth + gap);
            this.youtubeButton.setY(this.controlsTop);
            this.youtubeButton.setWidth(youtubeWidth);
            return;
        }

        int buttonWidth = Math.max(80, this.width - 16);
        this.controlsTop = Math.max(8, this.height - buttonHeight * 2 - gap - 8);
        this.continueButton.setX((this.width - buttonWidth) / 2);
        this.continueButton.setY(this.controlsTop);
        this.continueButton.setWidth(buttonWidth);
        this.youtubeButton.setX((this.width - buttonWidth) / 2);
        this.youtubeButton.setY(this.controlsTop + buttonHeight + gap);
        this.youtubeButton.setWidth(buttonWidth);
    }

    private float calculateContentScale() {
        float availableHeight = Math.max(1.0F, (float)this.controlsTop - 16.0F);
        float scale = Mth.clamp(Math.min((float)this.width / 560.0F, availableHeight / 300.0F), 0.4F, 1.0F);

        for (int
        i = 0; i < 2; i++) {
            int textWidth = Math.min(480, Math.max
 (120, Mth.floor((float)(this.width - 16) / scale)));
            float requiredHeight = this.contentHeight(textWidth, scale);
            if (requiredHeight * scale <= availableHeight) {
                break;
            }
            scale = Math.max(0.4F, scale * availableHeight / (requiredHeight * scale));
        }
        return scale;
    }

    private float contentHeight(int textWidth, float scale) {
        float height = 8.0f / scale + 84.0f + 8.0f + (float)(this.font.lineHeight * 2) + 23.0f;
        for (Component line : AlphaWarningScreen.bodyText()) {
            height += (float)(this.font.split((FormattedText)line, textWidth).size() * (this.font.lineHeight + 2) + 2);
        }
        Component hint = Component.translatable((String)"screen.luxium.alpha_warning.youtube_hint");
        height += (float)(this.font.split((FormattedText)hint, textWidth).size() * (this.font.lineHeight + 1));
        return height;
    }

    private static List<Component> bodyText() {
 return List.of(
            Component.translatable("screen.luxium.alpha_warning.line1"),
            Component.translatable("screen.luxium.alpha_warning.line2"),
            Component.translatable("screen.luxium.alpha_warning.line3"),
            Component.translatable("screen.luxium.alpha_warning.line4"),
            Component.translatable("screen.luxium.alpha_warning.line5") );
    }

    private void acknowledge() {

        Config.CLIENT.alphaWarningShown.set(Boolean.valueOf(true));
        Config.CLIENT.alphaWarningShown.save();
        this.onClose();
    }

    private void openYoutube() {
        Minecraft.getInstance().setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri("https://www.youtube.com/@Vinlanx");
            }

            Minecraft.getInstance().setScreen(this);
        }, "https://www.youtube.com/@Vinlanx", false));
    }

    @Override public void onClose() {
        Minecraft.getInstance
        ().setScreen(null);
    }

    @Override public boolean isPauseScreen() {

        return false;
    }
    }
