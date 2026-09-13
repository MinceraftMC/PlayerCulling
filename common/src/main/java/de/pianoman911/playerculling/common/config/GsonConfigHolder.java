package de.pianoman911.playerculling.common.config;

import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class GsonConfigHolder<T> extends ConfigHolder<T, GsonConfigurationLoader> {

    public GsonConfigHolder(Class<T> clazz, Supplier<T> def, Path path) {
        this(clazz, def, __ -> {
        }, path);
    }

    public GsonConfigHolder(Class<T> clazz, Supplier<T> def,
                            Consumer<TypeSerializerCollection.Builder> serializerConsumer, Path path) {
        this(clazz, __ -> def.get(), serializerConsumer, path);
    }

    public GsonConfigHolder(Class<T> clazz, Function<ConfigHolder<T, GsonConfigurationLoader>, T> def,
                            Consumer<TypeSerializerCollection.Builder> serializerConsumer, Path path) {
        super(clazz, def, path, () -> GsonConfigurationLoader.builder()
                .defaultOptions(options -> options.serializers(serializerConsumer)));
    }
}