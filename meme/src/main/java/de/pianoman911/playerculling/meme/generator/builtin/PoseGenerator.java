package de.pianoman911.playerculling.meme.generator.builtin;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;
import de.pianoman911.playerculling.meme.generator.GeneratorRegistry;
import net.minecraft.world.entity.Pose;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class PoseGenerator extends AbstractGenerator<PoseGenerator, Pose, PoseGenerator.Serializer> {

    public static final PoseGenerator INSTANCE = new PoseGenerator();

    @Override
    protected <O> void fuzz0(ReflectionUtil.FieldAccessor<Pose> accessor, O obj, Consumer<O> fuzzed) {
        for (Pose pose : Pose.values()) {
            if (pose != accessor.get(obj)) {
                accessor.set(obj, pose);
                fuzzed.accept(obj);
            }
        }
    }

    @Override
    public Serializer getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer extends CommonSerializer<PoseGenerator> {

        private static final Serializer INSTANCE = GeneratorRegistry.registerSerializer(new Serializer());

        protected Serializer() {
            super("pose");
        }

        @Override
        public PoseGenerator deserialize(Type type, org.spongepowered.configurate.ConfigurationNode node) {
            return PoseGenerator.INSTANCE;
        }

        @Override
        public void serialize(Type type, PoseGenerator obj, org.spongepowered.configurate.ConfigurationNode node) {
            // No state to serialize
        }
    }
}
