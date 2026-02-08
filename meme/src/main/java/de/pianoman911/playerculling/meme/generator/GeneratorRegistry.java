package de.pianoman911.playerculling.meme.generator;

import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {

    private static final Map<String, AbstractGenerator.CommonSerializer<?>> SERIALIZERS = new HashMap<>();

    public static <S extends AbstractGenerator.CommonSerializer<?>> S registerSerializer(S serializer) {
        SERIALIZERS.put(serializer.getTypeName(), serializer);
        return serializer;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <G extends AbstractGenerator<G,?, AbstractGenerator.CommonSerializer<G>>> G deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()) {
            return null;
        }
        String nodeType = (String) node.key();
        AbstractGenerator.CommonSerializer<?> serializer = SERIALIZERS.get(nodeType);
        if (serializer == null) {
            throw new IllegalStateException("No generator serializer registered for type: " + nodeType);
        }
        return (G) serializer.deserialize(type, node);
    }
}
