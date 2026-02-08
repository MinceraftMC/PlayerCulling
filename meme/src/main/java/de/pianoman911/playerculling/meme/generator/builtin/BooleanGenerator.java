package de.pianoman911.playerculling.meme.generator.builtin;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;
import de.pianoman911.playerculling.meme.generator.GeneratorRegistry;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class BooleanGenerator extends AbstractGenerator<BooleanGenerator, Boolean, BooleanGenerator.Serializer> {

    public static final BooleanGenerator INSTANCE = new BooleanGenerator();

    @Override
    public <O> void fuzz0(ReflectionUtil.FieldAccessor<Boolean> accessor, O obj, Consumer<O> fuzzed) {
        accessor.set(obj, !accessor.get(obj));
        fuzzed.accept(obj);
    }

    @Override
    public Serializer getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer extends CommonSerializer<BooleanGenerator> {

        public static final Serializer INSTANCE = GeneratorRegistry.registerSerializer(new Serializer());

        protected Serializer() {
            super("boolean");
        }

        @Override
        public BooleanGenerator deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (node.virtual()) {
                return null;
            }
            return BooleanGenerator.INSTANCE;
        }

        @Override
        public void serialize(Type type, @Nullable BooleanGenerator obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }
            // No state to serialize
        }
    }
}
