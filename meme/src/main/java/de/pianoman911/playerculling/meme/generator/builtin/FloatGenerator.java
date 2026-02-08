package de.pianoman911.playerculling.meme.generator.builtin;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;
import de.pianoman911.playerculling.meme.generator.GeneratorRegistry;
import io.leangen.geantyref.TypeToken;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class FloatGenerator extends AbstractGenerator<FloatGenerator, Float, FloatGenerator.Serializer> {

    private final FloatSet fuzzValues;

    public FloatGenerator(FloatSet fuzzValues) {
        this.fuzzValues = fuzzValues;
    }

    @Override
    protected <O> void fuzz0(ReflectionUtil.FieldAccessor<Float> accessor, O obj, Consumer<O> fuzzed) {
        float original = accessor.get(obj);
        for (float fuzzValue : this.fuzzValues) {
            if (fuzzValue != original) {
                accessor.set(obj, fuzzValue);
                fuzzed.accept(obj);
            }
        }
    }

    @Override
    public Serializer getSerializer() {
        return Serializer.INSTANCE;
    }

    public FloatSet getFuzzValues() {
        return this.fuzzValues;
    }

    public static class Serializer extends CommonSerializer<FloatGenerator> {

        private static final Serializer INSTANCE = GeneratorRegistry.registerSerializer(new Serializer());

        protected Serializer() {
            super("float");
        }

        @Override
        public FloatGenerator deserialize(Type type, ConfigurationNode node) throws SerializationException {
            FloatSet fuzzValues = new FloatOpenHashSet();
            if (!node.virtual()) {
                fuzzValues.addAll(Objects.requireNonNull(node.getList(new TypeToken<Float>() {})));
            }
            return new FloatGenerator(fuzzValues);
        }

        @Override
        public void serialize(Type type, @Nullable FloatGenerator obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }
            node.setList(new TypeToken<Float>() {}, List.copyOf(obj.getFuzzValues()));
        }
    }
}
