package de.pianoman911.playerculling.platformcommon.config;

import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@NullMarked
public class YamlConfigHolder<T> {

    private static final YamlConfigurationLoader.Builder CONFIG_BUILDER = YamlConfigurationLoader.builder()
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2);

    private final Class<T> clazz;
    private final Path path;
    private final YamlConfigurationLoader loader;
    private final Set<Consumer<T>> reloadHooks = new HashSet<>();
    private T config;

    public YamlConfigHolder(Class<T> clazz, Path path) {
        this.clazz = clazz;
        this.path = path;
        this.loader = CONFIG_BUILDER.path(path).build();

        this.config = this.reloadConfig();
    }

    public void addReloadHook(Consumer<T> consumer) {
        synchronized (this) {
            this.reloadHooks.add(consumer);
        }
    }

    public void addReloadHookAndRun(Consumer<T> consumer) {
        this.addReloadHook(consumer);
        consumer.accept(this.config);
    }

    public T reloadConfig() {
        synchronized (this) {
            try {
                Files.createDirectories(path.getParent());
                this.config = this.clazz.cast(this.loader.load().get(this.clazz));
                this.saveConfig();
                for (Consumer<T> runnable : this.reloadHooks) {
                    runnable.accept(this.config);
                }
                return this.config;
            } catch (IOException exception) {
                throw new RuntimeException("Failed to load config", exception);
            }
        }
    }

    public void saveConfig() {
        synchronized (this) {
            try {
                this.loader.save(this.loader.createNode().set(this.clazz, this.config));
            } catch (ConfigurateException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    public T getDelegate() {
        synchronized (this) {
            return this.config;
        }
    }
}
