package com.vinlanx.luxium.fabric;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.vinlanx.luxium.platform.config.ConfigSpec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

final class FabricConfig implements AutoCloseable {
    private final Path path;
    private final CommentedFileConfig file;
    private final ConfigSpec spec;
    private final List<FabricConfig.Entry<?>> entries = new ArrayList<>();
    private volatile boolean closed;

    FabricConfig(Path path, ConfigSpec spec) {
        this.path = path;
        this.spec = spec;
        this.file = CommentedFileConfig.builder(path).sync().preserveInsertionOrder().build();
        this.file.load();
        for (ConfigSpec.ConfigValue<?> value : spec.values()) { this.bind(value); }
        spec.bindSave(this.file::save);
        this.applyComments();
        this.file.save();

        try { FileWatcher.defaultInstance().addWatch(path, () -> Minecraft.getInstance().execute(this::reload));
        } catch (IOException exception) {
            this.file.close();
            throw new IllegalStateException("Cannot watch Luxium config", exception);
        }
    }

    private <T> void bind(ConfigSpec.ConfigValue<T> value) {
        FabricConfig.Entry<T> entry = new FabricConfig.Entry<>(value);
        entry.read();
        this.entries.add(entry);
        value.bind(() -> entry.current, next -> {
            entry.current = next;
            this.file.set(value.path(), encode(next));
        }, this.file::save);
    }

    private static Object encode(Object value) { return value instanceof Enum<?> enumeration ? enumeration.name() : value;
    }

    private void applyComments() { this.spec.sectionComments().forEach(this.file::setComment);
        for (ConfigSpec.ConfigValue<?> value : this.spec.values()) {
            String comment = value.comment();
            if (value instanceof ConfigSpec.IntValue integer) {
                comment = comment + "\nRange: " + integer.min() + " ~ " + integer.max();
            } else if (value instanceof ConfigSpec.DoubleValue decimal) {
                comment = comment + "\nRange: " + decimal.min() + " ~ " + decimal.max();
            } else if (value instanceof ConfigSpec.EnumValue<?> enumeration) {
                comment = comment + "\nAllowed Values: " + String.join(", ", enumeration.choices().stream().map(Enum::name).toList());
            }
            if (!comment.isBlank()) { this.file.setComment(value.path(), comment.strip());
            } }
    }

    private void reload() {
        if (!(!this.closed)) { return;
 } this.file.load();
            boolean corrected = false;
            for (FabricConfig.Entry<?> entry : this.entries) { corrected |= entry.read(); }
            this.applyComments();
if (corrected) { this.file.save(); }
} @Override public void close() {


 this.closed = true;
        FileWatcher.defaultInstance().removeWatch(this.path);
        this.file.close();
    }

    private final class Entry<T> {
        private final ConfigSpec.ConfigValue<T> value;
        private volatile T current;
        private Entry(ConfigSpec.ConfigValue<T> value) { this.value = value; }

        private boolean read() { Object raw = FabricConfig.this.file.get(this.value.path());
            T parsed = this.value.decode(raw);
            this.current = parsed == null ? this.value.getDefault() : parsed;
            Object encoded = FabricConfig.encode(this.current);
            boolean corrected = parsed == null;
            if (corrected) { FabricConfig.this.file.set(this.value.path(), encoded);
            } return corrected;
        }
    }
    }
