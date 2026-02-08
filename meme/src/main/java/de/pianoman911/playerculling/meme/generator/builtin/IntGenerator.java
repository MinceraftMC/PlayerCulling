package de.pianoman911.playerculling.meme.generator.builtin;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;
import de.pianoman911.playerculling.meme.generator.GeneratorRegistry;
import io.leangen.geantyref.TypeToken;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IntGenerator extends AbstractGenerator<IntGenerator, Integer, IntGenerator.Serializer> {

    private final IntSet fuzzValues;

    public IntGenerator(IntSet fuzzValues) {
        this.fuzzValues = fuzzValues;
    }

    @Override
    protected <O> void fuzz0(ReflectionUtil.FieldAccessor<Integer> accessor, O obj, Consumer<O> fuzzed) {
        int original = accessor.get(obj);
        for (int fuzzValue : this.fuzzValues) {
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

    public IntSet getFuzzValues() {
        return this.fuzzValues;
    }

    public static class Serializer extends CommonSerializer<IntGenerator> {

        private static final Serializer INSTANCE = GeneratorRegistry.registerSerializer(new Serializer());

        protected Serializer() {
            super("int");
        }

        @Override
        public IntGenerator deserialize(Type type, ConfigurationNode node) throws SerializationException {
            IntSet fuzzValues = new IntOpenHashSet();
            if (!node.virtual()) {
                fuzzValues.addAll(Objects.requireNonNull(node.getList(Integer.class)));
            }
            return new IntGenerator(fuzzValues);
        }

        @Override
        public void serialize(Type type, @Nullable IntGenerator obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }
            node.setList(new TypeToken<Integer>() {}, List.copyOf(obj.getFuzzValues()));
        }
    }
}
