/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractSliderButton
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.narration.NarrationElementOutput
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 */
package com.vinlanx.luxium.client.ConfigScreen;

import com.vinlanx.luxium.client.ConfigScreen.ConfigOption;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class ConfigWidgets {
    static final int TEXT = -461080;
    static final int MUTED = -6249040;
    static final int ACCENT = -11157;
    static final int ON = -650067881;
    static final int OFF = -637906350;
    static final int BORDER = 0x3FFFFFFF;

    private ConfigWidgets() {
    }

    static EditBox colorEditor(int x, int y, int width, ConfigOption<Integer> option) {
        EditBox box = new EditBox(Minecraft.m_91087_().f_91062_, x, y, width, 20, option.label());
        box.m_94199_(7);
        box.m_94144_(String.format(Locale.ROOT, "#%06X", option.get() & 0xFFFFFF));
        box.m_94153_(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        box.m_94151_(text -> {
            String hex;
            String string = hex = text.startsWith("#") ? text.substring(1) : text;
            if (hex.length() == 6) {
                option.set(Integer.parseInt(hex, 16));
            }
        });
        return box;
    }

    private static float animate(float current, float target, float speed) {
        float next = current + (target - current) * speed;
        return Math.abs(next - target) < 0.001f ? target : next;
    }

    private static int blend(int from, int to, float amount) {
        amount = Mth.m_14036_((float)amount, (float)0.0f, (float)1.0f);
        int a = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 24 & 0xFF), (float)(to >>> 24 & 0xFF)));
        int r = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 16 & 0xFF), (float)(to >>> 16 & 0xFF)));
        int g = Math.round(Mth.m_14179_((float)amount, (float)(from >>> 8 & 0xFF), (float)(to >>> 8 & 0xFF)));
        int b = Math.round(Mth.m_14179_((float)amount, (float)(from & 0xFF), (float)(to & 0xFF)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    static final class EnumCycle<E extends Enum<E>>
    extends AbstractWidget {
        private final ConfigOption<E> option;
        private E value;
        private float hover;
        private float pulse;

        EnumCycle(int x, int y, int width, ConfigOption<E> option) {
            super(x, y, width, 20, option.label());
            this.option = option;
            this.value = (Enum)option.get();
        }

        public void m_5716_(double mouseX, double mouseY) {
            int index = this.option.choices().indexOf(this.value);
            this.value = (Enum)this.option.choices().get((index + 1) % this.option.choices().size());
            this.option.set(this.value);
            this.pulse = 1.0f;
        }

        protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.hover = ConfigWidgets.animate(this.hover, this.m_274382_() ? 1.0f : 0.0f, 0.2f);
            this.pulse = ConfigWidgets.animate(this.pulse, 0.0f, 0.12f);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), -1291845632);
            int border = ConfigWidgets.blend(0x3FFFFFFF, -11157, Math.max(this.hover, this.pulse));
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + 1, border);
            graphics.m_280509_(this.m_252754_(), this.m_252907_() + this.m_93694_() - 1, this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), border);
            if (this.pulse > 0.01f) {
                graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), (int)(this.pulse * 34.0f) << 24 | 0xFFD46B);
            }
            graphics.m_280137_(Minecraft.m_91087_().f_91062_, this.option.format(this.value).getString().toUpperCase(Locale.ROOT), this.m_252754_() + this.m_5711_() / 2, this.m_252907_() + 6, -461080);
        }

        protected void m_168797_(NarrationElementOutput output) {
            this.m_168802_(output);
        }
    }

    static final class ActionButton
    extends AbstractWidget {
        private final Runnable action;
        private final Integer fillColor;
        private final Integer textColor;
        private float hover;

        ActionButton(int x, int y, int width, Component label, Runnable action) {
            this(x, y, width, label, action, null, null);
        }

        ActionButton(int x, int y, int width, Component label, Runnable action, Integer fillColor, Integer textColor) {
            super(x, y, width, 20, label);
            this.action = action;
            this.fillColor = fillColor;
            this.textColor = textColor;
        }

        public void m_5716_(double mouseX, double mouseY) {
            this.action.run();
        }

        protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.hover = ConfigWidgets.animate(this.hover, this.m_274382_() ? 1.0f : 0.0f, 0.22f);
            if (this.fillColor != null) {
                int fill = ConfigWidgets.blend(this.fillColor, ActionButton.blendLighter(this.fillColor, this.hover * 0.25f), this.hover);
                graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), fill);
                graphics.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.m_5711_() / 2, this.m_252907_() + 6, this.textColor != null ? this.textColor : -461080);
                return;
            }
            int border = ConfigWidgets.blend(0x3FFFFFFF, -11157, this.hover);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), -1728053248);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + 1, border);
            graphics.m_280509_(this.m_252754_(), this.m_252907_() + this.m_93694_() - 1, this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), border);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 1, this.m_252907_() + this.m_93694_(), border);
            graphics.m_280509_(this.m_252754_() + this.m_5711_() - 1, this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), border);
            graphics.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.m_5711_() / 2, this.m_252907_() + 6, ConfigWidgets.blend(-6249040, -461080, this.hover));
        }

        protected void m_168797_(NarrationElementOutput output) {
            this.m_168802_(output);
        }

        private static int blendLighter(int color, float amount) {
            int a = color >>> 24 & 0xFF;
            int r = Math.min(255, Math.round((float)(color >>> 16 & 0xFF) * (1.0f + amount)));
            int g = Math.min(255, Math.round((float)(color >>> 8 & 0xFF) * (1.0f + amount)));
            int b = Math.min(255, Math.round((float)(color & 0xFF) * (1.0f + amount)));
            return a << 24 | r << 16 | g << 8 | b;
        }
    }

    static final class Slider<T extends Number>
    extends AbstractSliderButton {
        private final ConfigOption<T> option;
        private final boolean integral;
        private float hover;

        Slider(int x, int y, int width, ConfigOption<T> option) {
            super(x, y, width, 20, (Component)Component.m_237119_(), Slider.normalize(option, ((Number)option.get()).doubleValue()));
            this.option = option;
            this.integral = option.type() == ConfigOption.Type.INTEGER;
            this.m_5695_();
        }

        private static double normalize(ConfigOption<?> option, double current) {
            if ("neo_gpu_vanilla_capture_resolution".equals(option.id())) {
                double power = Math.log(Math.max(1.0, current)) / Math.log(2.0);
                return Mth.m_14008_((double)(power / 9.0), (double)0.0, (double)1.0);
            }
            double normalized = Mth.m_14008_((double)((current - option.min()) / (option.max() - option.min())), (double)0.0, (double)1.0);
            return option.isSliderReversed() ? 1.0 - normalized : normalized;
        }

        private double configNormalized(double sliderValue) {
            return this.option.isSliderReversed() ? 1.0 - sliderValue : sliderValue;
        }

        protected void m_5695_() {
            double normalized = this.configNormalized(this.f_93577_);
            double raw = this.option.min() + normalized * (this.option.max() - this.option.min());
            double snapped = "neo_gpu_vanilla_capture_resolution".equals(this.option.id()) ? (double)(1 << Mth.m_14045_((int)((int)Math.round(normalized * 9.0)), (int)0, (int)9)) : this.option.min() + (double)Math.round((raw - this.option.min()) / this.option.step()) * this.option.step();
            if (this.integral) {
                int integerValue = (int)Math.round(snapped);
                if ("sky_entity_shadow_update_fps".equals(this.option.id()) && integerValue == 61) {
                    this.m_93666_((Component)Component.m_237113_((String)"MAX"));
                } else {
                    this.m_93666_((Component)Component.m_237113_((String)(integerValue + this.option.suffix())));
                }
            } else {
                int decimals = this.option.step() >= 1.0 ? 0 : (this.option.step() >= 0.1 ? 1 : 2);
                this.m_93666_((Component)Component.m_237113_((String)String.format(Locale.ROOT, "%." + decimals + "f%s", snapped, this.option.suffix())));
            }
        }

        protected void m_5697_() {
            this.applyNormalizedValue(this.f_93577_);
        }

        public void m_5716_(double mouseX, double mouseY) {
            this.setValueFromCursor(mouseX);
        }

        protected void m_7212_(double mouseX, double mouseY, double dragX, double dragY) {
            this.setValueFromCursor(mouseX);
        }

        private void setValueFromCursor(double mouseX) {
            if (!this.option.isEnabled()) {
                return;
            }
            int trackEnd = this.m_252754_() + this.m_5711_() - 52;
            this.f_93577_ = Mth.m_14008_((double)((mouseX - (double)this.m_252754_()) / (double)(trackEnd - this.m_252754_())), (double)0.0, (double)1.0);
            this.applyNormalizedValue(this.f_93577_);
            this.m_5695_();
        }

        private void applyNormalizedValue(double normalizedValue) {
            double snapped;
            double configValue = this.configNormalized(normalizedValue);
            double raw = this.option.min() + configValue * (this.option.max() - this.option.min());
            if ("neo_gpu_vanilla_capture_resolution".equals(this.option.id())) {
                snapped = 1 << Mth.m_14045_((int)((int)Math.round(configValue * 9.0)), (int)0, (int)9);
                this.f_93577_ = Slider.normalize(this.option, snapped);
            } else {
                snapped = Mth.m_14008_((double)(this.option.min() + (double)Math.round((raw - this.option.min()) / this.option.step()) * this.option.step()), (double)this.option.min(), (double)this.option.max());
            }
            if (this.integral) {
                this.setInteger((int)Math.round(snapped));
            } else {
                this.setDecimal(snapped);
            }
        }

        private void setInteger(int value) {
            this.option.set(value);
        }

        private void setDecimal(double value) {
            this.option.set(value);
        }

        public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.f_93623_ = this.option.isEnabled();
            this.hover = ConfigWidgets.animate(this.hover, this.f_93623_ && this.m_274382_() ? 1.0f : 0.0f, 0.2f);
            int centerY = this.m_252907_() + this.m_93694_() / 2;
            int trackEnd = this.m_252754_() + this.m_5711_() - 52;
            int activeColor = this.f_93623_ ? -11157 : -6249040;
            graphics.m_280509_(this.m_252754_(), centerY - 2, trackEnd, centerY + 2, -1728053248);
            graphics.m_280509_(this.m_252754_(), centerY - 2, this.m_252754_() + (int)((double)(trackEnd - this.m_252754_()) * this.f_93577_), centerY + 2, activeColor);
            int handleX = this.m_252754_() + (int)((double)(trackEnd - this.m_252754_()) * this.f_93577_);
            int expansion = Math.round(this.hover * 2.0f);
            graphics.m_280509_(handleX - 3 - expansion, this.m_252907_() + 2 - expansion, handleX + 3 + expansion, this.m_252907_() + this.m_93694_() - 2 + expansion, this.f_93623_ ? ConfigWidgets.blend(-461080, -11157, this.hover) : -6249040);
            graphics.m_280614_(Minecraft.m_91087_().f_91062_, this.m_6035_(), trackEnd + 6, this.m_252907_() + 6, activeColor, true);
        }
    }

    static final class Toggle
    extends AbstractWidget {
        private final ConfigOption<Boolean> option;
        private boolean value;
        private float selection;
        private float hover;

        Toggle(int x, int y, int width, ConfigOption<Boolean> option) {
            super(x, y, width, 20, option.label());
            this.option = option;
            this.value = option.get();
            this.selection = this.value ? 1.0f : 0.0f;
        }

        public void m_5716_(double mouseX, double mouseY) {
            if (!this.option.isEnabled()) {
                return;
            }
            this.value = mouseX >= (double)this.m_252754_() + (double)this.m_5711_() / 2.0;
            this.option.set(this.value);
        }

        protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.f_93623_ = this.option.isEnabled();
            this.value = this.option.get();
            int half = this.m_5711_() / 2;
            this.selection = ConfigWidgets.animate(this.selection, this.value ? 1.0f : 0.0f, 0.24f);
            this.hover = ConfigWidgets.animate(this.hover, this.m_274382_() ? 1.0f : 0.0f, 0.22f);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), -1728053248);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + 1, 0x3FFFFFFF);
            graphics.m_280509_(this.m_252754_(), this.m_252907_() + this.m_93694_() - 1, this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), 0x3FFFFFFF);
            graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 1, this.m_252907_() + this.m_93694_(), 0x3FFFFFFF);
            graphics.m_280509_(this.m_252754_() + this.m_5711_() - 1, this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), 0x3FFFFFFF);
            int selectionX = this.m_252754_() + Math.round((float)half * this.selection);
            graphics.m_280509_(selectionX, this.m_252907_(), selectionX + half, this.m_252907_() + this.m_93694_(), ConfigWidgets.blend(-637906350, -650067881, this.selection));
            if (this.hover > 0.01f) {
                graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), (int)(this.hover * 20.0f) << 24 | 0xFFFFFF);
            }
            graphics.m_280137_(Minecraft.m_91087_().f_91062_, "OFF", this.m_252754_() + half / 2, this.m_252907_() + 6, this.value ? -6249040 : -461080);
            graphics.m_280137_(Minecraft.m_91087_().f_91062_, "ON", this.m_252754_() + half + half / 2, this.m_252907_() + 6, this.value ? -461080 : -6249040);
        }

        protected void m_168797_(NarrationElementOutput output) {
            this.m_168802_(output);
        }
    }
}

