/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.ConfirmLinkScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.ResourceProvider
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 */
package com.vinlanx.luxium.client.ConfigScreen;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.ConfigScreen.ConfigOption;
import com.vinlanx.luxium.client.ConfigScreen.ConfigPreviewImages;
import com.vinlanx.luxium.client.ConfigScreen.ConfigScreenModel;
import com.vinlanx.luxium.client.ConfigScreen.ConfigWidgets;
import com.vinlanx.luxium.client.ConfigScreen.GlassPanelRenderer;
import com.vinlanx.luxium.client.guiscreen.LuxiumMenuBackgroundRenderer;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.mixin.GameRendererAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class LuxiumConfigScreen
extends Screen {
    private static final int PANEL = -1509225201;
    private static final int HOVER = 0x14FFFFFF;
    private static final int BORDER = 0x33FFFFFF;
    private static final int ACCENT = -11157;
    private static final int TEXT = -461080;
    private static final int MUTED = -6249040;
    private static final int ROW_HEIGHT = 32;
    private static final int HEADER_HEIGHT = 38;
    private static final int SIDEBAR_FOOTER_HEIGHT = 72;
    private static final long BACKGROUND_TRANSITION_NANOS = 400000000L;
    private final Screen parent;
    private final List<ConfigScreenModel.Folder> folders = ConfigScreenModel.createFolders();
    private final List<ConfigScreenModel.Category> categories = this.folders.stream().flatMap(folder -> folder.categories().stream()).toList();
    private final List<ControlRow> controls = new ArrayList<ControlRow>();
    private final Map<String, Float> tabHover = new HashMap<String, Float>();
    private final Set<String> expandedFolders = new HashSet<String>();
    private final GlassPanelRenderer glassRenderer = new GlassPanelRenderer();
    private final LuxiumMenuBackgroundRenderer menuBackgroundRenderer = new LuxiumMenuBackgroundRenderer();
    private boolean loadedOpaqueIceEnabled;
    private boolean loadedWaterEnabled;
    private boolean loadedReceiverShadersEnabled;
    private boolean loadedLocalReceiverEnabled;
    private boolean loadedNeoGpuVanillaReceiverEnabled;
    private boolean loadedBlockLightTestEnabled;
    private boolean loadedNeoGpuVanillaCutoutEnabled;
    private boolean loadedNeoSkyCelestiaEnabled;
    private boolean loadedSkyEntityShadowsEnabled;
    private ConfigScreenModel.Category category;
    private ConfigWidgets.ActionButton resetPageButton;
    private ConfigWidgets.ActionButton resetAllButton;
    private ConfigWidgets.ActionButton youTubeButton;
    private ConfigOption<?> hoveredOption;
    private ConfigScreenModel.Folder hoveredFolder;
    private ConfigScreenModel.Category hoveredCategory;
    private ConfigOption<?> previewOption;
    private boolean previewAfter;
    private long observedPreviewRevision;
    private double scroll;
    private double targetScroll;
    private double sidebarScroll;
    private double sidebarTargetScroll;
    private boolean blurEnabled;
    private float blurSelection;
    private boolean dimmingEnabled;
    private float dimmingSelection;
    private long lastBackgroundTransitionNanos;
    private float previewAnimation;
    private float tabAnimation;
    private float activeTabY;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int previewWidth;
    private boolean compactLayout;

    public LuxiumConfigScreen(Screen parent) {
        super((Component)Component.m_237115_((String)"title.luxium.config"));
        this.loadedOpaqueIceEnabled = (Boolean)Config.CLIENT.opaqueIceEnabled.get();
        this.loadedWaterEnabled = (Boolean)Config.CLIENT.waterEnabled.get();
        this.loadedReceiverShadersEnabled = LuxiumGpuShaderFeatures.receiverShadersEnabled();
        this.loadedLocalReceiverEnabled = LuxiumGpuShaderFeatures.localReceiverEnabled();
        this.loadedNeoGpuVanillaReceiverEnabled = LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled();
        this.loadedBlockLightTestEnabled = Config.isFeatureEnabled(Config.CLIENT.blockLightTestEnabled);
        this.loadedNeoGpuVanillaCutoutEnabled = (Boolean)Config.CLIENT.neoGpuVanillaCutoutEnabled.get();
        this.loadedNeoSkyCelestiaEnabled = LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled();
        this.loadedSkyEntityShadowsEnabled = (Boolean)Config.CLIENT.skyEntityShadowsEnabled.get();
        this.blurEnabled = true;
        this.blurSelection = 1.0f;
        this.dimmingEnabled = true;
        this.dimmingSelection = 1.0f;
        this.lastBackgroundTransitionNanos = System.nanoTime();
        this.previewAnimation = 1.0f;
        this.tabAnimation = 1.0f;
        this.activeTabY = Float.NaN;
        this.parent = parent;
        this.category = this.categories.get(0);
    }

    protected void m_7856_() {
        this.calculateLayout();
        this.buildControls();
    }

    private void calculateLayout() {
        this.compactLayout = this.f_96543_ < 520 || this.f_96544_ < 380;
        int horizontalMargin = this.compactLayout ? 8 : 24;
        int verticalMargin = this.compactLayout ? 40 : 72;
        this.panelWidth = Math.min(1400, Math.max(1, this.f_96543_ - horizontalMargin));
        this.panelHeight = Math.min(820, Math.max(1, this.f_96544_ - verticalMargin));
        this.panelX = (this.f_96543_ - this.panelWidth) / 2;
        this.panelY = Math.max(34, (this.f_96544_ - this.panelHeight) / 2 + (this.compactLayout ? 8 : 12));
        this.sidebarWidth = this.compactLayout ? Mth.m_14045_((int)(this.panelWidth * 30 / 100), (int)86, (int)126) : Mth.m_14045_((int)(this.panelWidth * 19 / 100), (int)138, (int)280);
        this.previewWidth = this.compactLayout ? 0 : Mth.m_14045_((int)(this.panelWidth * 27 / 100), (int)190, (int)380);
    }

    private void buildControls() {
        this.m_169413_();
        this.controls.clear();
        int controlWidth = this.compactLayout ? Mth.m_14045_((int)((this.panelWidth - this.sidebarWidth) * 44 / 100), (int)76, (int)116) : Math.max(110, (this.panelWidth - this.sidebarWidth - this.previewWidth - 80) / 2);
        int y = this.contentTop();
        for (ConfigScreenModel.Row row : this.category.rows()) {
            if (row.isHeader()) {
                this.controls.add(new ControlRow(row, null, y));
                y += 38;
                continue;
            }
            ConfigOption<?> option = row.option();
            AbstractWidget widget = this.createWidget(option, controlWidth);
            this.m_7787_((GuiEventListener)widget);
            this.controls.add(new ControlRow(row, widget, y));
            y += 32;
        }
        this.resetPageButton = (ConfigWidgets.ActionButton)this.m_7787_((GuiEventListener)new ConfigWidgets.ActionButton(0, 0, 104, (Component)Component.m_237113_((String)"RESET PAGE"), this::resetPage));
        this.resetAllButton = (ConfigWidgets.ActionButton)this.m_7787_((GuiEventListener)new ConfigWidgets.ActionButton(0, 0, 94, (Component)Component.m_237113_((String)"RESET ALL"), this::resetAll));
        this.youTubeButton = (ConfigWidgets.ActionButton)this.m_7787_((GuiEventListener)new ConfigWidgets.ActionButton(0, 0, 96, (Component)Component.m_237113_((String)"YouTube"), this::openYouTube, -5438680, -1));
        this.scroll = 0.0;
        this.targetScroll = 0.0;
        this.tabAnimation = 0.0f;
    }

    private AbstractWidget createWidget(ConfigOption<?> option, int width) {
        return switch (option.type()) {
            default -> throw new IncompatibleClassChangeError();
            case ConfigOption.Type.BOOLEAN -> new ConfigWidgets.Toggle(0, 0, Math.min(width, 116), (ConfigOption)option);
            case ConfigOption.Type.INTEGER -> new ConfigWidgets.Slider(0, 0, width, (ConfigOption)option);
            case ConfigOption.Type.DOUBLE -> new ConfigWidgets.Slider(0, 0, width, (ConfigOption)option);
            case ConfigOption.Type.ENUM -> new ConfigWidgets.EnumCycle(0, 0, Math.min(width, 150), (ConfigOption)option);
            case ConfigOption.Type.COLOR -> ConfigWidgets.colorEditor(0, 0, Math.min(width, 112), (ConfigOption)option);
        };
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.scroll += (this.targetScroll - this.scroll) * 0.28;
        this.sidebarScroll += (this.sidebarTargetScroll - this.sidebarScroll) * 0.28;
        this.tabAnimation += (1.0f - this.tabAnimation) * 0.22f;
        long now = System.nanoTime();
        float transitionStep = Math.min(1.0f, (float)(now - this.lastBackgroundTransitionNanos) / 4.0E8f);
        this.lastBackgroundTransitionNanos = now;
        this.blurSelection = LuxiumConfigScreen.moveTowards(this.blurSelection, this.blurEnabled ? 1.0f : 0.0f, transitionStep);
        this.dimmingSelection = LuxiumConfigScreen.moveTowards(this.dimmingSelection, this.dimmingEnabled ? 1.0f : 0.0f, transitionStep);
        if (this.f_96541_.f_91073_ == null) {
            this.menuBackgroundRenderer.render(graphics, this.f_96543_, this.f_96544_, mouseX, mouseY, partialTick);
        }
        if (!this.glassRenderer.render(this.panelX, this.panelY, this.panelWidth, this.panelHeight, this.f_96543_, this.f_96544_, this.blurSelection, this.dimmingSelection) && this.dimmingSelection > 0.0f) {
            graphics.m_280509_(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, this.dimmedColor(-1509225201));
        }
        LuxiumConfigScreen.drawBorder(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight, 0x33FFFFFF);
        this.renderTitle(graphics, mouseX, mouseY, partialTick);
        this.renderSidebar(graphics, mouseX, mouseY);
        this.renderContent(graphics, mouseX, mouseY, partialTick);
        this.renderPreview(graphics, mouseX, mouseY);
        this.renderCompactPreviewTooltip(graphics, mouseX, mouseY);
    }

    private void renderTitle(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int youtubeWidth = this.compactLayout ? 72 : 96;
        this.youTubeButton.m_93674_(youtubeWidth);
        this.youTubeButton.m_252865_(this.panelX);
        this.youTubeButton.m_253211_(this.panelY - 29);
        this.youTubeButton.f_93624_ = true;
        this.youTubeButton.m_88315_(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.m_280509_(this.panelX, this.panelY, this.panelX + this.sidebarWidth, this.panelY + this.panelHeight, this.dimmedColor(0x4D000000));
        graphics.m_280509_(this.panelX + this.sidebarWidth - 1, this.panelY, this.panelX + this.sidebarWidth + 1, this.panelY + this.panelHeight, 0x33FFFFFF);
        int top = this.panelY + 8;
        int bottom = this.panelY + this.panelHeight - 72;
        int tabHeight = this.sidebarTabHeight();
        int y = top - (int)this.sidebarScroll;
        this.hoveredFolder = null;
        this.hoveredCategory = null;
        graphics.m_280588_(this.panelX, top, this.panelX + this.sidebarWidth, bottom);
        for (SidebarEntry entry : this.sidebarEntries()) {
            float hover;
            boolean hovered;
            if (entry.isFolder()) {
                ConfigScreenModel.Folder folder = entry.folder();
                boolean expanded = this.expandedFolders.contains(folder.id());
                boolean bl = hovered = mouseY >= top && mouseY < bottom && LuxiumConfigScreen.inside(mouseX, mouseY, this.panelX, y, this.sidebarWidth, tabHeight);
                if (hovered) {
                    this.hoveredFolder = folder;
                }
                hover = this.tabHover.getOrDefault(folder.id(), Float.valueOf(0.0f)).floatValue();
                hover += ((hovered ? 1.0f : 0.0f) - hover) * 0.22f;
                this.tabHover.put(folder.id(), Float.valueOf(hover));
                if (hover > 0.01f) {
                    graphics.m_280509_(this.panelX, y, this.panelX + this.sidebarWidth, y + tabHeight, Math.round(20.0f * hover) << 24 | 0xFFFFFF);
                }
                graphics.m_280056_(this.f_96547_, expanded ? "\u25bc" : "\u25b6", this.panelX + 10, y + (tabHeight - 8) / 2, -11157, true);
                graphics.m_280614_(this.f_96547_, this.fittedUpper(folder.title(), this.sidebarWidth - 36), this.panelX + 24, y + (tabHeight - 8) / 2, LuxiumConfigScreen.blend(-11157, -461080, hover), true);
                y += tabHeight;
                continue;
            }
            ConfigScreenModel.Category item = entry.category();
            boolean active = item == this.category;
            boolean bl = hovered = mouseY >= top && mouseY < bottom && LuxiumConfigScreen.inside(mouseX, mouseY, this.panelX, y, this.sidebarWidth, tabHeight);
            if (hovered && !item.description().getString().isEmpty()) {
                this.hoveredCategory = item;
            }
            hover = this.tabHover.getOrDefault(item.id(), Float.valueOf(0.0f)).floatValue();
            hover += ((hovered ? 1.0f : 0.0f) - hover) * 0.22f;
            this.tabHover.put(item.id(), Float.valueOf(hover));
            if (active || hover > 0.01f) {
                int alpha = active ? 20 : Math.round(20.0f * hover);
                graphics.m_280509_(this.panelX, y, this.panelX + this.sidebarWidth, y + tabHeight, alpha << 24 | 0xFFFFFF);
            }
            if (active) {
                if (Float.isNaN(this.activeTabY)) {
                    this.activeTabY = y;
                }
                this.activeTabY += ((float)y - this.activeTabY) * 0.26f;
            }
            int indent = entry.indented() ? 12 : 0;
            Component title = this.fittedUpper(item.title(), this.sidebarWidth - 24 - indent);
            graphics.m_280614_(this.f_96547_, title, this.panelX + 10 + indent + Math.round(3.0f * hover), y + (tabHeight - 8) / 2, active ? -461080 : LuxiumConfigScreen.blend(-6249040, -461080, hover), true);
            y += tabHeight;
        }
        if (!Float.isNaN(this.activeTabY)) {
            graphics.m_280509_(this.panelX, Math.round(this.activeTabY), this.panelX + 4, Math.round(this.activeTabY) + tabHeight, -11157);
        }
        graphics.m_280618_();
        int contentHeight = this.sidebarEntries().size() * tabHeight;
        int viewport = bottom - top;
        if (contentHeight > viewport) {
            int thumbHeight = Math.max(20, viewport * viewport / contentHeight);
            int thumbY = top + (int)((double)(viewport - thumbHeight) * this.sidebarScroll / (double)(contentHeight - viewport));
            graphics.m_280509_(this.panelX + this.sidebarWidth - 6, top, this.panelX + this.sidebarWidth - 3, bottom, 0x33000000);
            graphics.m_280509_(this.panelX + this.sidebarWidth - 6, thumbY, this.panelX + this.sidebarWidth - 3, thumbY + thumbHeight, 0x55FFFFFF);
        }
        int footerY = this.panelY + this.panelHeight - 72;
        graphics.m_280509_(this.panelX, footerY, this.panelX + this.sidebarWidth, this.panelY + this.panelHeight, this.dimmedColor(Integer.MIN_VALUE));
        graphics.m_280509_(this.panelX, footerY, this.panelX + this.sidebarWidth, footerY + 1, 0x33FFFFFF);
        if (this.sidebarWidth >= 118) {
            graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"screen.luxium.config.bg_blur"), this.panelX + 8, footerY + 13, -6249040, true);
            graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"screen.luxium.config.bg_dimming"), this.panelX + 8, footerY + 43, -6249040, true);
        }
        int toggleWidth = this.compactLayout ? 58 : 64;
        int toggleX = this.panelX + this.sidebarWidth - toggleWidth - 8;
        LuxiumConfigScreen.drawToggle(graphics, toggleX, footerY + 7, toggleWidth, 20, this.blurEnabled ? 1.0f : 0.0f);
        LuxiumConfigScreen.drawToggle(graphics, toggleX, footerY + 37, toggleWidth, 20, this.dimmingEnabled ? 1.0f : 0.0f);
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int viewport;
        int contentHeight;
        int left = this.panelX + this.sidebarWidth + 1;
        int right = this.panelX + this.panelWidth - this.previewWidth;
        int top = this.contentTop();
        int bottom = this.panelY + this.panelHeight - 48;
        this.hoveredOption = null;
        int labelX = left + 24;
        int controlRight = right - 24;
        for (ControlRow control : this.controls) {
            if (control.widget == null) continue;
            control.widget.f_93624_ = false;
        }
        graphics.m_280588_(left, top, right, bottom);
        for (ControlRow control : this.controls) {
            AbstractWidget abstractWidget;
            boolean visible;
            int animatedOffset = Math.round((1.0f - this.tabAnimation) * 10.0f);
            int y = control.baseY - (int)this.scroll + animatedOffset;
            if (control.row.isHeader()) {
                if (y + 38 <= top || y >= bottom) continue;
                graphics.m_280614_(this.f_96547_, LuxiumConfigScreen.upper(control.row.header()), labelX, y + 12, -11157, true);
                graphics.m_280509_(labelX, y + 27, controlRight, y + 29, -11157);
                continue;
            }
            control.widget.f_93624_ = visible = y + 32 > top && y < bottom;
            if (!visible) continue;
            ConfigOption<?> option = control.row.option();
            control.widget.f_93623_ = option.isEnabled();
            boolean hovered = LuxiumConfigScreen.inside(mouseX, mouseY, left, y, right - left, 32);
            if (hovered) {
                graphics.m_280509_(left, y, right, y + 32, 0x14FFFFFF);
                graphics.m_280509_(left, y, left + 2, y + 32, -11157);
                this.hoveredOption = option;
            }
            graphics.m_280509_(left + 10, y + 32 - 1, right - 10, y + 32, 0xDFFFFFF);
            control.widget.m_252865_(controlRight - control.widget.m_5711_());
            control.widget.m_253211_(y + 6);
            int labelWidth = Math.max(20, control.widget.m_252754_() - labelX - 6);
            graphics.m_280614_(this.f_96547_, this.fittedUpper(option.label(), labelWidth), labelX, y + 12, option.isEnabled() ? -461080 : -6249040, true);
            control.widget.m_88315_(graphics, mouseX, mouseY, partialTick);
            if (option.type() != ConfigOption.Type.COLOR || !((abstractWidget = control.widget) instanceof EditBox)) continue;
            EditBox box = (EditBox)abstractWidget;
            try {
                String hex = box.m_94155_().replace("#", "");
                int color = hex.length() == 6 ? Integer.parseInt(hex, 16) : 0;
                graphics.m_280509_(box.m_252754_() - 28, y + 7, box.m_252754_() - 6, y + 25, 0xFF000000 | color);
                LuxiumConfigScreen.drawBorder(graphics, box.m_252754_() - 28, y + 7, 22, 18, 0x33FFFFFF);
            }
            catch (NumberFormatException numberFormatException) {}
        }
        graphics.m_280618_();
        if (this.hoveredOption != null && this.hoveredOption != this.previewOption) {
            this.previewOption = this.hoveredOption;
            this.syncPreviewWithConfig();
            this.previewAnimation = 0.0f;
        }
        if ((contentHeight = this.controls.isEmpty() ? 0 : this.controls.get((int)(this.controls.size() - 1)).baseY + 32 - top) > (viewport = bottom - top)) {
            int trackY = top;
            int trackHeight = viewport;
            int thumbHeight = Math.max(24, viewport * viewport / contentHeight);
            int thumbY = trackY + (int)((double)(trackHeight - thumbHeight) * this.scroll / (double)(contentHeight - viewport));
            graphics.m_280509_(right - 5, trackY, right - 2, trackY + trackHeight, 0x33000000);
            graphics.m_280509_(right - 5, thumbY, right - 2, thumbY + thumbHeight, 0x55FFFFFF);
        }
        int actionsY = this.panelY + this.panelHeight - 34;
        graphics.m_280509_(left, actionsY - 8, right, this.panelY + this.panelHeight, this.dimmedColor(Integer.MIN_VALUE));
        graphics.m_280509_(left, actionsY - 8, right, actionsY - 7, 0x33FFFFFF);
        this.resetAllButton.m_252865_(right - this.resetAllButton.m_5711_());
        this.resetAllButton.m_253211_(actionsY);
        this.resetPageButton.m_252865_(this.resetAllButton.m_252754_() - this.resetPageButton.m_5711_() - 8);
        this.resetPageButton.m_253211_(actionsY);
        this.resetPageButton.f_93624_ = true;
        this.resetAllButton.f_93624_ = true;
        this.resetPageButton.m_88315_(graphics, mouseX, mouseY, partialTick);
        this.resetAllButton.m_88315_(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.previewWidth <= 0) {
            return;
        }
        int x = this.panelX + this.panelWidth - this.previewWidth;
        graphics.m_280509_(x, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, this.dimmedColor(0x66000000));
        graphics.m_280509_(x, this.panelY, x + 2, this.panelY + this.panelHeight, 0x33FFFFFF);
        int left = x + Math.max(14, this.previewWidth / 17);
        int usable = this.previewWidth - Math.max(28, this.previewWidth / 8);
        if (this.hoveredFolder != null) {
            graphics.m_280614_(this.f_96547_, LuxiumConfigScreen.upper(this.hoveredFolder.title()), left, this.panelY + 28, -11157, true);
            graphics.m_280509_(left, this.panelY + 46, left + usable, this.panelY + 48, 0x1AFFFFFF);
            int textY = this.panelY + 62;
            for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)this.hoveredFolder.description(), usable)) {
                graphics.m_280649_(this.f_96547_, line, left, textY, -6249040, true);
                textY += 12;
            }
            return;
        }
        if (this.hoveredCategory != null) {
            graphics.m_280614_(this.f_96547_, LuxiumConfigScreen.upper(this.hoveredCategory.title()), left, this.panelY + 28, -11157, true);
            graphics.m_280509_(left, this.panelY + 46, left + usable, this.panelY + 48, 0x1AFFFFFF);
            int textY = this.panelY + 62;
            for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)this.hoveredCategory.description(), usable)) {
                graphics.m_280649_(this.f_96547_, line, left, textY, -6249040, true);
                textY += 12;
            }
            return;
        }
        if (this.previewOption == null) {
            graphics.m_280653_(this.f_96547_, (Component)Component.m_237115_((String)"screen.luxium.config.hover_hint"), x + this.previewWidth / 2, this.panelY + this.panelHeight / 2, -2136955472);
            return;
        }
        this.syncPreviewIfConfigChanged();
        this.previewAnimation += (1.0f - this.previewAnimation) * 0.24f;
        int offsetY = Math.round((1.0f - this.previewAnimation) * 10.0f);
        int alpha = Math.round(this.previewAnimation * 255.0f);
        graphics.m_280614_(this.f_96547_, LuxiumConfigScreen.upper(this.previewOption.label()), left, this.panelY + 28 + offsetY, alpha << 24 | 0xFFD46B, true);
        graphics.m_280509_(left, this.panelY + 46, left + usable, this.panelY + 48, 0x1AFFFFFF);
        int textY = this.panelY + 62 + offsetY;
        if (ConfigPreviewImages.hasPreview(this.previewOption)) {
            int imageHeight = Math.min(200, usable * 9 / 16);
            ResourceLocation image = ConfigPreviewImages.image(this.previewOption, this.previewAfter);
            graphics.m_280398_(image, left, textY, 0, 0.0f, 0.0f, usable, imageHeight, usable, imageHeight);
            LuxiumConfigScreen.drawBorder(graphics, left, textY, usable, imageHeight, -6249040);
            int buttonWidth = Math.min(112, usable);
            int buttonX = left + (usable - buttonWidth) / 2;
            int buttonY = textY + imageHeight + 5;
            this.drawPreviewModeButton(graphics, buttonX, buttonY, buttonWidth, mouseX, mouseY);
            textY += imageHeight + 28;
        }
        for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)LuxiumConfigScreen.upper(this.previewOption.description()), usable)) {
            graphics.m_280649_(this.f_96547_, line, left, textY, alpha << 24 | 0xA0A5B0, true);
            textY += 12;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderCompactPreviewTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.previewWidth > 0 || this.hoveredOption == null) {
            return;
        }
        int popupWidth = Math.max(80, Math.min(320, this.panelWidth - 16));
        int padding = 10;
        int textWidth = popupWidth - padding * 2;
        List lines = this.f_96547_.m_92923_((FormattedText)this.hoveredOption.description(), textWidth);
        boolean hasImage = ConfigPreviewImages.hasPreview(this.hoveredOption);
        int imageHeight = hasImage ? Math.min(150, textWidth * 9 / 16) : 0;
        int popupHeight = padding + 9 + 8 + imageHeight + (imageHeight > 0 ? 28 : 0) + lines.size() * 11 + padding;
        int popupX = mouseX + 14;
        if (popupX + popupWidth > this.f_96543_ - 4) {
            popupX = mouseX - popupWidth - 14;
        }
        popupX = Mth.m_14045_((int)popupX, (int)4, (int)Math.max(4, this.f_96543_ - popupWidth - 4));
        int popupY = Mth.m_14045_((int)(mouseY + 12), (int)4, (int)Math.max(4, this.f_96544_ - popupHeight - 4));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 400.0f);
        try {
            graphics.m_280509_(popupX, popupY, popupX + popupWidth, popupY + popupHeight, -16052977);
            LuxiumConfigScreen.drawBorder(graphics, popupX, popupY, popupWidth, popupHeight, -11157);
            int contentX = popupX + padding;
            int contentY = popupY + padding;
            graphics.m_280614_(this.f_96547_, this.fittedUpper(this.hoveredOption.label(), textWidth), contentX, contentY, -11157, true);
            contentY += 17;
            if (imageHeight > 0) {
                this.syncPreviewIfConfigChanged();
                graphics.m_280398_(ConfigPreviewImages.image(this.hoveredOption, this.previewAfter), contentX, contentY, 0, 0.0f, 0.0f, textWidth, imageHeight, textWidth, imageHeight);
                LuxiumConfigScreen.drawBorder(graphics, contentX, contentY, textWidth, imageHeight, -6249040);
                contentY += imageHeight + 28;
            }
            for (FormattedCharSequence line : (List<FormattedCharSequence>)lines) {
                graphics.m_280649_(this.f_96547_, line, contentX, contentY, -6249040, false);
                contentY += 11;
            }
        }
        finally {
            graphics.m_280168_().m_85849_();
        }
    }

    private int contentTop() {
        return this.panelY + 18;
    }

    private static float moveTowards(float current, float target, float amount) {
        return current < target ? Math.min(target, current + amount) : Math.max(target, current - amount);
    }

    private int dimmedColor(int color) {
        int alpha = Math.round((float)(color >>> 24 & 0xFF) * this.dimmingSelection);
        return alpha << 24 | color & 0xFFFFFF;
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        int footerY;
        int toggleWidth;
        int toggleX;
        if (button != 0) {
            return super.m_6375_(mouseX, mouseY, button);
        }
        if (this.previewWidth > 0 && this.previewOption != null && ConfigPreviewImages.hasPreview(this.previewOption)) {
            int buttonY;
            PreviewImageBounds bounds = this.previewImageBounds();
            int buttonWidth = Math.min(112, bounds.width());
            int buttonX = bounds.x() + (bounds.width() - buttonWidth) / 2;
            if (LuxiumConfigScreen.inside(mouseX, mouseY, buttonX, buttonY = bounds.y() + bounds.height() + 5, buttonWidth, 18)) {
                this.previewAfter = !this.previewAfter;
                return true;
            }
        }
        if (LuxiumConfigScreen.inside(mouseX, mouseY, toggleX = this.panelX + this.sidebarWidth - (toggleWidth = this.compactLayout ? 58 : 64) - 8, (footerY = this.panelY + this.panelHeight - 72) + 7, toggleWidth, 20)) {
            this.blurEnabled = mouseX >= (double)toggleX + (double)toggleWidth / 2.0;
            return true;
        }
        if (LuxiumConfigScreen.inside(mouseX, mouseY, toggleX, footerY + 37, toggleWidth, 20)) {
            this.dimmingEnabled = mouseX >= (double)toggleX + (double)toggleWidth / 2.0;
            return true;
        }
        int tabHeight = this.sidebarTabHeight();
        int sidebarTop = this.panelY + 8;
        int sidebarBottom = this.panelY + this.panelHeight - 72;
        int y = sidebarTop - (int)this.sidebarScroll;
        for (SidebarEntry entry : this.sidebarEntries()) {
            if (mouseY >= (double)sidebarTop && mouseY < (double)sidebarBottom && LuxiumConfigScreen.inside(mouseX, mouseY, this.panelX, y, this.sidebarWidth, tabHeight)) {
                ConfigScreenModel.Category item;
                if (entry.isFolder()) {
                    String id = entry.folder().id();
                    if (!this.expandedFolders.add(id)) {
                        this.expandedFolders.remove(id);
                    }
                    return true;
                }
                this.category = item = entry.category();
                this.buildControls();
                this.previewOption = null;
                return true;
            }
            y += tabHeight;
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        int sidebarTop = this.panelY + 8;
        int sidebarBottom = this.panelY + this.panelHeight - 72;
        if (LuxiumConfigScreen.inside(mouseX, mouseY, this.panelX, sidebarTop, this.sidebarWidth, sidebarBottom - sidebarTop)) {
            int contentHeight = this.sidebarEntries().size() * this.sidebarTabHeight();
            int viewport = sidebarBottom - sidebarTop;
            this.sidebarTargetScroll = Mth.m_14008_((double)(this.sidebarTargetScroll - delta * 28.0), (double)0.0, (double)Math.max(0, contentHeight - viewport));
            return true;
        }
        int left = this.panelX + this.sidebarWidth;
        int right = this.panelX + this.panelWidth - this.previewWidth;
        if (LuxiumConfigScreen.inside(mouseX, mouseY, left, this.panelY, right - left, this.panelHeight)) {
            int contentHeight = this.controls.isEmpty() ? 0 : this.controls.get((int)(this.controls.size() - 1)).baseY + 32 - this.contentTop();
            int viewport = this.panelHeight - 66;
            this.targetScroll = Mth.m_14008_((double)(this.targetScroll - delta * 42.0), (double)0.0, (double)Math.max(0, contentHeight - viewport));
            return true;
        }
        return super.m_6050_(mouseX, mouseY, delta);
    }

    public void m_86600_() {
        if (this.f_96541_.f_91073_ == null) {
            this.menuBackgroundRenderer.tick();
        }
        for (ControlRow row : this.controls) {
            AbstractWidget abstractWidget = row.widget;
            if (!(abstractWidget instanceof EditBox)) continue;
            EditBox box = (EditBox)abstractWidget;
            box.m_94120_();
        }
    }

    public void m_7379_() {
        this.rebuildRendererIfChanged();
        this.f_96541_.m_91152_(this.parent);
    }

    public void m_7861_() {
        this.rebuildRendererIfChanged();
        this.glassRenderer.close();
    }

    private void rebuildRendererIfChanged() {
        boolean terrainPermutationChanged;
        LuxiumGpuShaderFeatures.refreshNow();
        boolean geometryChanged = this.loadedOpaqueIceEnabled != (Boolean)Config.CLIENT.opaqueIceEnabled.get();
        boolean receiverShadersEnabled = LuxiumGpuShaderFeatures.receiverShadersEnabled();
        boolean waterStateChanged = this.loadedWaterEnabled != (Boolean)Config.CLIENT.waterEnabled.get();
        boolean receiverStateChanged = this.loadedReceiverShadersEnabled != receiverShadersEnabled;
        boolean localReceiverStateChanged = this.loadedLocalReceiverEnabled != LuxiumGpuShaderFeatures.localReceiverEnabled();
        boolean neoGpuVanillaReceiverStateChanged = this.loadedNeoGpuVanillaReceiverEnabled != LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled();
        boolean blockLightTestChanged = this.loadedBlockLightTestEnabled != Config.isFeatureEnabled(Config.CLIENT.blockLightTestEnabled);
        boolean neoGpuVanillaCutoutChanged = this.loadedNeoGpuVanillaCutoutEnabled != (Boolean)Config.CLIENT.neoGpuVanillaCutoutEnabled.get();
        boolean neoSkyCelestiaStateChanged = this.loadedNeoSkyCelestiaEnabled != LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled();
        boolean skyEntityShadowPermutationChanged = this.loadedSkyEntityShadowsEnabled != (Boolean)Config.CLIENT.skyEntityShadowsEnabled.get();
        boolean shaderSetChanged = blockLightTestChanged || receiverStateChanged || localReceiverStateChanged || neoGpuVanillaReceiverStateChanged || neoSkyCelestiaStateChanged || waterStateChanged;
        boolean bl = terrainPermutationChanged = shaderSetChanged || LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled() && neoGpuVanillaCutoutChanged || (receiverShadersEnabled || LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled()) && skyEntityShadowPermutationChanged;
        if (geometryChanged || terrainPermutationChanged) {
            if (shaderSetChanged) {
                BlockLightTest.clearProgramCache();
            }
            if (this.f_96541_.f_91073_ != null && (geometryChanged || terrainPermutationChanged)) {
                this.f_96541_.f_91060_.m_109818_();
            }
            if (shaderSetChanged) {
                ((GameRendererAccessor)this.f_96541_.f_91063_).luxium$reloadShaders((ResourceProvider)this.f_96541_.m_91098_());
            }
            this.loadedOpaqueIceEnabled = (Boolean)Config.CLIENT.opaqueIceEnabled.get();
            this.loadedWaterEnabled = (Boolean)Config.CLIENT.waterEnabled.get();
            this.loadedReceiverShadersEnabled = receiverShadersEnabled;
            this.loadedLocalReceiverEnabled = LuxiumGpuShaderFeatures.localReceiverEnabled();
            this.loadedNeoGpuVanillaReceiverEnabled = LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled();
            this.loadedBlockLightTestEnabled = Config.isFeatureEnabled(Config.CLIENT.blockLightTestEnabled);
            this.loadedNeoGpuVanillaCutoutEnabled = (Boolean)Config.CLIENT.neoGpuVanillaCutoutEnabled.get();
            this.loadedNeoSkyCelestiaEnabled = LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled();
        }
        this.loadedSkyEntityShadowsEnabled = (Boolean)Config.CLIENT.skyEntityShadowsEnabled.get();
    }

    public boolean m_7043_() {
        return true;
    }

    private void resetPage() {
        ConfigScreenModel.reset(this.category);
        this.previewOption = null;
        this.buildControls();
    }

    private void resetAll() {
        ConfigScreenModel.resetAll(this.categories);
        this.previewOption = null;
        this.buildControls();
    }

    private void openYouTube() {
        String url = "https://www.youtube.com/@Vinlanx";
        this.f_96541_.m_91152_((Screen)new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.m_137581_().m_137646_("https://www.youtube.com/@Vinlanx");
            }
            this.f_96541_.m_91152_((Screen)this);
        }, "https://www.youtube.com/@Vinlanx", false));
    }

    private static void drawToggle(GuiGraphics graphics, int x, int y, int width, int height, float selection) {
        int half = width / 2;
        graphics.m_280509_(x, y, x + width, y + height, -1728053248);
        int selectionX = x + Math.round((float)half * selection);
        graphics.m_280509_(selectionX, y, selectionX + half, y + height, LuxiumConfigScreen.blend(-637906350, -650067881, selection));
        graphics.m_280137_(Minecraft.m_91087_().f_91062_, "OFF", x + half / 2, y + 6, selection > 0.5f ? -6249040 : -461080);
        graphics.m_280137_(Minecraft.m_91087_().f_91062_, "ON", x + half + half / 2, y + 6, selection > 0.5f ? -461080 : -6249040);
        LuxiumConfigScreen.drawBorder(graphics, x, y, width, height, 0x33FFFFFF);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.m_280509_(x, y, x + width, y + 1, color);
        graphics.m_280509_(x, y + height - 1, x + width, y + height, color);
        graphics.m_280509_(x, y, x + 1, y + height, color);
        graphics.m_280509_(x + width - 1, y, x + width, y + height, color);
    }

    private void drawPreviewModeButton(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = LuxiumConfigScreen.inside(mouseX, mouseY, x, y, width, 18);
        graphics.m_280509_(x, y, x + width, y + 18, hovered ? 0x26FFFFFF : -1728053248);
        LuxiumConfigScreen.drawBorder(graphics, x, y, width, 18, hovered ? -11157 : 0x33FFFFFF);
        graphics.m_280137_(this.f_96547_, this.previewAfter ? "AFTER" : "BEFORE", x + width / 2, y + 5, hovered ? -11157 : -461080);
    }

    private PreviewImageBounds previewImageBounds() {
        int x = this.panelX + this.panelWidth - this.previewWidth;
        int left = x + Math.max(14, this.previewWidth / 17);
        int usable = this.previewWidth - Math.max(28, this.previewWidth / 8);
        int imageHeight = Math.min(200, usable * 9 / 16);
        int imageY = this.panelY + 62 + Math.round((1.0f - this.previewAnimation) * 10.0f);
        return new PreviewImageBounds(left, imageY, usable, imageHeight);
    }

    private void syncPreviewWithConfig() {
        Boolean value;
        this.observedPreviewRevision = this.previewOption.revision();
        Object obj = this.previewOption.get();
        this.previewAfter = obj instanceof Boolean && (value = (Boolean)obj) != false;
    }

    private void syncPreviewIfConfigChanged() {
        if (!ConfigPreviewImages.hasPreview(this.previewOption)) {
            return;
        }
        if (this.previewOption.revision() != this.observedPreviewRevision) {
            Boolean value;
            this.observedPreviewRevision = this.previewOption.revision();
            Object obj = this.previewOption.get();
            this.previewAfter = obj instanceof Boolean && (value = (Boolean)obj) != false;
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= (double)x && mouseX < (double)(x + width) && mouseY >= (double)y && mouseY < (double)(y + height);
    }

    private static Component upper(Component component) {
        return Component.m_237113_((String)component.getString().toUpperCase(Locale.ROOT));
    }

    private Component fittedUpper(Component component, int maxWidth) {
        String text = component.getString().toUpperCase(Locale.ROOT);
        if (this.f_96547_.m_92895_(text) <= maxWidth) {
            return Component.m_237113_((String)text);
        }
        return Component.m_237113_((String)(this.f_96547_.m_92834_(text, Math.max(0, maxWidth - this.f_96547_.m_92895_("\u2026"))) + "\u2026"));
    }

    private int sidebarTabHeight() {
        return this.compactLayout ? 24 : 28;
    }

    private List<SidebarEntry> sidebarEntries() {
        ArrayList<SidebarEntry> entries = new ArrayList<SidebarEntry>();
        for (ConfigScreenModel.Folder folder : this.folders) {
            if ("main".equals(folder.id())) {
                for (ConfigScreenModel.Category item : folder.categories()) {
                    entries.add(SidebarEntry.category(item, false));
                }
                continue;
            }
            entries.add(SidebarEntry.folder(folder));
            if (!this.expandedFolders.contains(folder.id())) continue;
            for (ConfigScreenModel.Category item : folder.categories()) {
                entries.add(SidebarEntry.category(item, true));
            }
        }
        return entries;
    }

    private static int blend(int from, int to, float amount) {
        amount = Mth.m_14036_((float)amount, (float)0.0f, (float)1.0f);
        int a = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 24 & 0xFF), (float)(to >>> 24 & 0xFF)));
        int r = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 16 & 0xFF), (float)(to >>> 16 & 0xFF)));
        int g = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 8 & 0xFF), (float)(to >>> 8 & 0xFF)));
        int b = Math.round(Mth.m_14179_((float)amount, (float)(from & 0xFF), (float)(to & 0xFF)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private record ControlRow(ConfigScreenModel.Row row, AbstractWidget widget, int baseY) {
    }

    private record SidebarEntry(ConfigScreenModel.Folder folder, ConfigScreenModel.Category category, boolean indented) {
        static SidebarEntry folder(ConfigScreenModel.Folder folder) {
            return new SidebarEntry(folder, null, false);
        }

        static SidebarEntry category(ConfigScreenModel.Category category, boolean indented) {
            return new SidebarEntry(null, category, indented);
        }

        boolean isFolder() {
            return this.folder != null;
        }
    }

    private record PreviewImageBounds(int x, int y, int width, int height) {
    }
}
