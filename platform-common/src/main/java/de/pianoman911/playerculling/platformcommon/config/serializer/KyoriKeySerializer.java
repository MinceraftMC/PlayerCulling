package de.pianoman911.playerculling.platformcommon.config.serializer;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

@NullMarked
public final class KyoriKeySerializer implements TypeSerializer<Key> {

    public static final KyoriKeySerializer INSTANCE = new KyoriKeySerializer();

    private KyoriKeySerializer() {
    }

    @Override
    @Nullable
    public Key deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()) {
            return null;
        }
        return Key.key(Objects.requireNonNull(node.getString()));
    }

    @Override
    public void serialize(Type type, @Nullable Key obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.set(obj.asString());
    }
}
