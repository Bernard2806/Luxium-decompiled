/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$EnumValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.vinlanx.luxium.client.ConfigScreen;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ConfigOption<T> {
    private final String id;
    private final Component label;
    private final Component description;
    private final Type type;
    private final ForgeConfigSpec.ConfigValue<T> value;
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;
    private final List<T> choices;
    private final Function<T, Component> formatter;
    private final Consumer<T> changed;
    private final BooleanSupplier enabled;
    private final boolean reverseSlider;
    private long revision;

    private ConfigOption(String id, Component label, Component description, Type type, ForgeConfigSpec.ConfigValue<T> value, double min, double max, double step, String suffix, List<T> choices, Function<T, Component> formatter, Consumer<T> changed, BooleanSupplier enabled, boolean reverseSlider) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.choices = choices;
        this.formatter = formatter;
        this.changed = changed;
        this.enabled = enabled;
        this.reverseSlider = reverseSlider;
    }

    public static ConfigOption<Boolean> bool(String id, String labelKey, String tooltipKey, ForgeConfigSpec.BooleanValue value) {
        return new ConfigOption<Boolean>(id, (Component)Component.m_237115_((String)labelKey), (Component)Component.m_237115_((String)tooltipKey), Type.BOOLEAN, (ForgeConfigSpec.ConfigValue<Boolean>)value, 0.0, 1.0, 1.0, "", List.of(Boolean.valueOf(false), Boolean.valueOf(true)), state -> Component.m_237113_((String)(state != false ? "ON" : "OFF")), ignored -> {}, () -> true, false);
    }

    public static ConfigOption<Integer> integer(String id, String labelKey, String tooltipKey, ForgeConfigSpec.IntValue value, int min, int max, int step, String suffix) {
        return new ConfigOption<Integer>(id, (Component)Component.m_237115_((String)labelKey), (Component)Component.m_237115_((String)tooltipKey), Type.INTEGER, (ForgeConfigSpec.ConfigValue<Integer>)value, min, max, step, suffix, List.of(), number -> Component.m_237113_((String)(number + suffix)), ignored -> {}, () -> true, false);
    }

    public static ConfigOption<Double> decimal(String id, String labelKey, String tooltipKey, ForgeConfigSpec.DoubleValue value, double min, double max, double step, String suffix) {
        return new ConfigOption<Double>(id, (Component)Component.m_237115_((String)labelKey), (Component)Component.m_237115_((String)tooltipKey), Type.DOUBLE, (ForgeConfigSpec.ConfigValue<Double>)value, min, max, step, suffix, List.of(), number -> Component.m_237113_((String)(ConfigOption.formatDecimal(number, step) + suffix)), ignored -> {}, () -> true, false);
    }

    public static <E extends Enum<E>> ConfigOption<E> enumeration(String id, String labelKey, String tooltipKey, ForgeConfigSpec.EnumValue<E> value, List<E> choices, Function<E, Component> formatter, Consumer<E> changed) {
        return new ConfigOption<E>(id, (Component)Component.m_237115_((String)labelKey), (Component)Component.m_237115_((String)tooltipKey), Type.ENUM, value, 0.0, 0.0, 0.0, "", choices, formatter, changed, () -> true, false);
    }

    public static ConfigOption<Integer> color(String id, String labelKey, String tooltipKey, ForgeConfigSpec.IntValue value) {
        return new ConfigOption<Integer>(id, (Component)Component.m_237115_((String)labelKey), (Component)Component.m_237115_((String)tooltipKey), Type.COLOR, (ForgeConfigSpec.ConfigValue<Integer>)value, 0.0, 1.6777215E7, 1.0, "", List.of(), color -> Component.m_237113_((String)String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)), ignored -> {}, () -> true, false);
    }

    public ConfigOption<T> onChanged(Consumer<T> callback) {
        return new ConfigOption<T>(this.id, this.label, this.description, this.type, this.value, this.min, this.max, this.step, this.suffix, this.choices, this.formatter, callback, this.enabled, this.reverseSlider);
    }

    public ConfigOption<T> enabledWhen(BooleanSupplier condition) {
        return new ConfigOption<T>(this.id, this.label, this.description, this.type, this.value, this.min, this.max, this.step, this.suffix, this.choices, this.formatter, this.changed, condition, this.reverseSlider);
    }

    public ConfigOption<T> reversedSlider() {
        return new ConfigOption<T>(this.id, this.label, this.description, this.type, this.value, this.min, this.max, this.step, this.suffix, this.choices, this.formatter, this.changed, this.enabled, true);
    }

    public T get() {
        return (T)this.value.get();
    }

    public void set(T newValue) {
        this.value.set(newValue);
        this.value.save();
        this.changed.accept(newValue);
        ++this.revision;
    }

    public void reset() {
        this.set(this.value.getDefault());
    }

    public String id() {
        return this.id;
    }

    public Component label() {
        return this.label;
    }

    public Component description() {
        return this.description;
    }

    public Type type() {
        return this.type;
    }

    public double min() {
        return this.min;
    }

    public double max() {
        return this.max;
    }

    public double step() {
        return this.step;
    }

    public String suffix() {
        return this.suffix;
    }

    public List<T> choices() {
        return this.choices;
    }

    public Component format(T current) {
        return this.formatter.apply(current);
    }

    public boolean isEnabled() {
        return this.enabled.getAsBoolean();
    }

    public boolean isSliderReversed() {
        return this.reverseSlider;
    }

    public long revision() {
        return this.revision;
    }

    private static String formatDecimal(double value, double step) {
        if (step >= 1.0) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (step >= 0.1) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static enum Type {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        ENUM,
        COLOR;

    }
}

