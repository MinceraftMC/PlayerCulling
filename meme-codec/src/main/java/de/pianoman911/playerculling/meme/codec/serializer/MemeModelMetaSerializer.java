package de.pianoman911.playerculling.meme.codec.serializer;

import de.pianoman911.playerculling.meme.codec.model.MemeModelMeta;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@NullMarked
public class MemeModelMetaSerializer implements TypeSerializer<MemeModelMeta> {

    public static final MemeModelMetaSerializer INSTANCE = new MemeModelMetaSerializer();

    @Override
    @Nullable
    public MemeModelMeta deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()) {
            return null;
        }
        String name = node.node("name").getString();
        String entityType = node.node("entityType").getString();
        int version = node.node("version").getInt();
        return new MemeModelMeta(name, entityType, version);
    }

    @Override
    public void serialize(Type type, @Nullable MemeModelMeta obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.node("name").set(obj.name());
        node.node("entityType").set(obj.entityType());
        node.node("version").set(obj.version());
    }
}
