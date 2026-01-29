package de.pianoman911.playerculling.meme.codec.serializer;

import de.pianoman911.playerculling.meme.codec.model.MemeModelCube;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@NullMarked
public class MemeModelCubeSerializer implements TypeSerializer<MemeModelCube> {

    public static final MemeModelCubeSerializer INSTANCE = new MemeModelCubeSerializer();

    @Override
    @Nullable
    public MemeModelCube deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()){
            return null;
        }
        float minX = node.node("minX").getFloat();
        float minY = node.node("minY").getFloat();
        float minZ = node.node("minZ").getFloat();
        float maxX = node.node("maxX").getFloat();
        float maxY = node.node("maxY").getFloat();
        float maxZ = node.node("maxZ").getFloat();
        return new MemeModelCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void serialize(Type type, @Nullable MemeModelCube obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        node.node("minX").set(obj.minX());
        node.node("minY").set(obj.minY());
        node.node("minZ").set(obj.minZ());
        node.node("maxX").set(obj.maxX());
        node.node("maxY").set(obj.maxY());
        node.node("maxZ").set(obj.maxZ());
    }
}
