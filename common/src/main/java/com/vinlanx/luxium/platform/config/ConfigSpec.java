package com.vinlanx.luxium.platform.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;

public final class ConfigSpec {
    private final List<ConfigSpec.ConfigValue<?>> values;
    private final Map<List<String>, String> sectionComments;
    private Runnable save;

    private ConfigSpec(List<ConfigSpec.ConfigValue<?>> values, Map<List<String>, String> sectionComments) {
        this.values = List.copyOf(values);
        this.sectionComments = Map.copyOf(sectionComments);
    }

    public List<ConfigSpec.ConfigValue<?>> values() { return this.values;
    } public Map<List<String>, String> sectionComments() { return this.sectionComments;
    } public void bindSave(Runnable save) { this.save = Objects.requireNonNull(save); }
    public void save() { Objects.requireNonNull(this.save, "Config is not registered").run(); }










    public abstract static class ConfigValue<T> { private final List<String> path; private final String comment; private final T defaultValue; private T value; private Supplier<T> getter; private Consumer<T> setter; private Runnable save; private ConfigValue(List<String> path, String comment, T defaultValue) {
 this.path = List.copyOf(path);
this.comment = comment;
 this.defaultValue = defaultValue;
 this.value = defaultValue;
 } public List<String> path() {

 return this.path; } public String comment() {
 return this.comment; } public T getDefault() {
 return this.defaultValue; } public T get() {
 return this.getter == null ? this.value : this.getter.get(); } public abstract T decode(Object var1); public void set(T newValue) { T



 valid = this.decode(newValue);
if (valid == null) { throw new IllegalArgumentException("Invalid config value: " + this.path); } else {
 if (this.setter == null) { this.value = valid; } else {
 this.setter.accept(valid); } }
 } public void bind(Supplier<T> getter, Consumer<T> setter, Runnable save) {


 if (this.getter != null) { throw new IllegalStateException("Config value is already bound: " + this.path); } else {
 this.getter = Objects.requireNonNull(getter);
this.setter = Objects.requireNonNull(setter);
this.save = Objects.requireNonNull(save); }
} public void save() {

 Objects.requireNonNull(this.save, "Config is not registered").run(); } }


    public static final class BooleanValue extends ConfigSpec.ConfigValue<Boolean> {
        private BooleanValue(List<String> path, String comment, boolean value) { super(path, comment, value); }

        public Boolean decode(Object value) { if (value instanceof Boolean bool) { return bool;
            } if (value instanceof String text) {
                    if (text.equalsIgnoreCase("true")) { return true;
                    } if (text.equalsIgnoreCase("false")) { return false; } }

 return null; } }



 public static final class IntValue extends ConfigSpec.ConfigValue<Integer> { private final int min; private final int max; private IntValue(List<String> path, String comment, int value, int min, int max) {



 super(path, comment, value);
 this.min = min;
 this.max = max;
 } public int min() {
 return this.min; } public int max() {
 return this.max; } public Integer decode(Object value) {

 if (!(value instanceof Number number)) { return null; } long
 integer = number.longValue();
return integer >= (long)this.min && integer <= (long)this.max && number.doubleValue() == (double)integer ? (int)integer : null;
            
        }
    }
    public static final class DoubleValue extends ConfigSpec.ConfigValue<Double> { private final double min; private final double max; private DoubleValue(List<String> path, String comment, double value, double min, double max) {



 super(path, comment, value);
 this.min = min;
 this.max = max;
 } public double min() {
 return this.min; } public double max() {
 return this.max; } public Double decode(Object value) {

 if (!(value instanceof Number number)) { return null; } double
 result = number.doubleValue();
return Double.isFinite(result) && result >= this.min && result <= this.max ? result : null;  } }



 public static final class EnumValue<E extends Enum<E>> extends ConfigSpec.ConfigValue<E> { private final List<E> choices; private EnumValue(List<String> path, String comment, E value, List<E> choices) {


 super(path, comment, value);
 this.choices = List.copyOf(choices);
} public List<E> choices() {
 return this.choices; } public E decode(Object value) { for (E choice :

this.choices) {
if (choice == value || value instanceof String text && choice.name().equalsIgnoreCase(text)) { return choice; }
 }
 return null; } } public static final



    class Builder {
        private final List<ConfigSpec.ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        private final Map<List<String>, String> sectionComments = new LinkedHashMap<>();
        private String comment = "";

        public ConfigSpec.Builder comment(String...lines) { this.comment = String.join("\n", lines); return this;
        }
        public ConfigSpec.Builder push(String name) { this.path.add(name);
            if (!this.comment.isEmpty()) { this.sectionComments.put(List.copyOf(this.path), this.comment);
            } this.comment = "";
            return this;
        }

        public ConfigSpec.Builder pop() { if (this.path.isEmpty()) { throw new IllegalStateException("Empty config section stack");
            } else { this.path.remove(this.path.size() - 1);
                return this;
            }
        }
        private List<String> key(String name) { List<String> key = new ArrayList<>(this.path);
            key.add(name);
            if (this.values.stream().anyMatch(value -> value.path().equals(key))) { throw new IllegalArgumentException("Duplicate config path: " + key);
            } else { return key;
            }
        }
        private <T extends ConfigSpec.ConfigValue<?>> T add(T value) { this.values.add(value);
            this.comment = "";
            return value;
        }
        public ConfigSpec.BooleanValue define(String name, boolean value) { return this.add(new ConfigSpec.BooleanValue(this.key(name), this.comment, value));
        }
        public ConfigSpec.IntValue defineInRange(String name, int value, int min, int max) { if (!(value >= min && value <= max)) { throw new IllegalArgumentException(name);
            } return this.add(new ConfigSpec.IntValue(this.key(name), this.comment, value, min, max));

        }
        public ConfigSpec.DoubleValue defineInRange(String name, double value, double min, double max) { if (!(Double.isFinite(value) && !(value < min) && !(value > max))) { throw new IllegalArgumentException(name); }
return this.add(new ConfigSpec.DoubleValue(this.key(name), this.comment, value, min, max)); } public <E extends Enum<E>> ConfigSpec.EnumValue<E> defineEnum(String name, E value, List<E> choices) {


 if (!choices.contains(value)) { throw new IllegalArgumentException(name);
            } else { return this.add(new ConfigSpec.EnumValue<>(this.key(name), this.comment, value, choices));
            }
        }
        public <T> Pair<T, ConfigSpec> configure(Function<ConfigSpec.Builder, T> factory) { T config = factory.apply(this);
            if (!this.path.isEmpty()) { throw new IllegalStateException("Unclosed config sections: " + this.path);
            } else { return Pair.of(config, new ConfigSpec(this.values, this.sectionComments)); } } }
    }
