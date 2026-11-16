package com.vinlanx.luxium.client.guiscreen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public abstract class IncompatibleModScreen extends Screen {
    private static final ResourceLocation NO_TEXTURE = new ResourceLocation("luxium", "abunezabuvaloatlas/no.png");
    private static final int TEXTURE_SIZE = 144;
    private static final int FRAME_SIZE = 124;
    private static final int FRAME_COLUMNS = 6;
    private static final int FRAME_ROWS = 2;
    private static final long FRAME_DURATION_NANOS = 150000000L;
    private final long animationStartNanos = System.nanoTime();
    private final Component removeMessage;
    private final Component instructions;

    protected IncompatibleModScreen(String modName, String removeKey) {
        super(Component.translatable("screen.luxium.incompatible.title", modName));
        this.removeMessage = Component.translatable(removeKey).withStyle(ChatFormatting.BOLD);
        this.instructions = Component.translatable("screen.luxium.incompatible.instructions", modName);
    }

    @Override protected void init() {
        int
        buttonWidth = Math.min(200, Math.max(1, this.width - 24));
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button -> this.minecraft.stop())
                .bounds((this.width - buttonWidth) / 2, this.height - 32, buttonWidth, 20)
                .build() );
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        graphics.fill(0, 0, this.width, this.height, -15396840);
        int titleY = Math.max(8, this.height / 8);
        this.drawWrapped(graphics, this.title, titleY, -1);
        float
        scale = Math.min(
            3.0F, Math.min((float)Math.max(1, this.width - 24) / (float)Math.max(1, this.font.width(this.removeMessage)), (float)Math.max
(1, this.height / 8) / (float)this.font.lineHeight) );
        int titleBottom = titleY + this.font.split(this.title, Math.max(1, this.width - 24)).size() * (this
 .font.lineHeight + 2);
        int instructionsHeight = this.font.split(this.instructions, Math.max(1, this.width - 24)).size() * (this
 .font.lineHeight + 2);
        int messageHeight = (int)Math.ceil((double)((float)this.font.lineHeight * scale));
        int textureSize = Math.max(0, Math.min(144, Math.min
 (this.width - 24, this.height - 40 - instructionsHeight - titleBottom - messageHeight - 24)));
        float messageCenterY = Math.max((float)this.height / 2.0F, (float)(titleBottom + textureSize + 16) + (float)messageHeight / 2.0F);
        int
        messageTop = (int)(messageCenterY - (float)messageHeight / 2.0F);
        if (textureSize > 0) {
            int frame = (int)((System.nanoTime() - this.animationStartNanos) / 150000000L % 12L);
            RenderSystem.enableBlend
            ();
            RenderSystem.defaultBlendFunc();
            graphics.blit(
                NO_TEXTURE,
                (this.width - textureSize) / 2,
                titleBottom + (messageTop - titleBottom - textureSize) / 2,
                textureSize,
                textureSize, (float)(frame % 6 * 124), (float)(frame / 6 * 124), 124, 124, 744, 248 ); RenderSystem.disableBlend();
        }
        graphics.pose().pushPose();
        graphics.pose().translate((float)this.width / 2.0F, messageCenterY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, this.removeMessage, -this.font.width(this.removeMessage) / 2, -this
 .font.lineHeight / 2, -43691, true);
        graphics.pose().popPose();

        this.drawWrapped(graphics, this.instructions, Math.min(this.height - 40 - instructionsHeight, Math.max
 (this.height * 3 / 4, messageTop + messageHeight + 8)),
            -2236963 );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int y, int color) { for (FormattedCharSequence line :
this.font.split(text, Math.max(1, this.width - 24))) {
            graphics.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, color, false);
            y += this.font.lineHeight + 2;
        }
    }

    @Override public boolean shouldCloseOnEsc() {

        return false;
    }

    @Override public void onClose() {

    }
    }
