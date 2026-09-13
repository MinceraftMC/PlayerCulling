package de.pianoman911.playerculling.common.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public class ConfigHolder<T, L extends AbstractConfigurationLoader<?>> {

    private final Class<T> clazz;
    private final Function<ConfigHolder<T, L>, T> def;
    private final Path path;
    private final Supplier<L> loader;
    private final Set<Consumer<T>> reloadHooks = new CopyOnWriteArraySet<>();

    private volatile @Nullable T config;

    public <B extends AbstractConfigurationLoader.Builder<B, L>> ConfigHolder(
            Class<T> clazz, Function<ConfigHolder<T, L>, T> def, Path path, Supplier<B> loaderBuilder
    ) {
        this.clazz = clazz;
        this.path = path;
        this.def = def;
        this.loader = () -> loaderBuilder.get().path(path).build();
    }

    public void addReloadHook(Consumer<T> consumer) {
        this.reloadHooks.add(consumer);
    }

    public void addReloadHookAndRun(Consumer<T> consumer) {
        this.addReloadHook(consumer);

        T config = this.config;
        if (config != null) {
            consumer.accept(config);
        }
    }

    public T reloadConfig() {
        T config;
        try {
            synchronized (this) {
                if (Files.exists(this.path)) {
                    config = this.loader.get().load().get(this.clazz);
                    if (config == null) {
                        throw new IllegalStateException("Can't load config " + this.clazz);
                    }
                } else {
                    // create default
                    config = this.def.apply(this);
                }
                this.config = config;
                this.saveConfig0();
            }
            // trigger reload hooks
            for (Consumer<T> runnable : this.reloadHooks) {
                runnable.accept(config);
            }
            return config;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config", exception);
        }
    }

    public void saveConfig() {
        synchronized (this) {
            this.saveConfig0();
        }
    }

    private void saveConfig0() {
        try {
            Files.createDirectories(this.path.getParent());
            L loader = this.loader.get();
            loader.save(loader.createNode().set(this.clazz, this.config));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public T getDelegate() {
        T config = this.config;
        if (config == null) {
            throw new IllegalStateException("Config currently isn't loaded");
        }
        return config;
    }
}
