package de.pianoman911.playerculling.meme.generator;

import de.pianoman911.playerculling.common.ReflectionUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public abstract class AbstractGenerator<F extends AbstractGenerator<F, T, S>, T, S extends AbstractGenerator.CommonSerializer<F>> {

    private static final Logger LOGGER = LoggerFactory.getLogger("MemeFuzzer");

    public final <O> void fuzz(ReflectionUtil.FieldAccessor<T> accessor, O obj, Consumer<O> fuzzed) {
        try {
            fuzzed.accept(obj); // Initial fuzzer call with default value
            fuzz0(accessor, obj, fuzzed);
        } catch (Throwable throwable) {
            LOGGER.error("Error while fuzzing field {} of type {}", accessor.field().getName(), accessor.field().getType().getName(), throwable);
        }
    }

    protected abstract <O> void fuzz0(ReflectionUtil.FieldAccessor<T> accessor, O obj, Consumer<O> fuzzed);

    public abstract S getSerializer();

    public static abstract class CommonSerializer<F> implements TypeSerializer<F> {

        private final String typeName;

        protected CommonSerializer(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return this.typeName;
        }
    }

    public static class Serializer<G extends AbstractGenerator<G, ?, CommonSerializer<G>>> implements TypeSerializer<G> {

        public static final Serializer<?> INSTANCE = new Serializer<>();

        @Override
        public G deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (node.virtual()) {
                return null;
            }
            return GeneratorRegistry.deserialize(type, node);
        }

        @Override
        public void serialize(Type type, @Nullable G obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }
            CommonSerializer<G> serializer = obj.getSerializer();
            ConfigurationNode data = node.node(serializer.getTypeName());
            serializer.serialize(type, obj, data);
        }
    }
}
