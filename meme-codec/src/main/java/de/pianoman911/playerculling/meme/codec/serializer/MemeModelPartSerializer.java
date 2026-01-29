package de.pianoman911.playerculling.meme.codec.serializer;

import de.pianoman911.playerculling.meme.codec.model.MemeModelCube;
import de.pianoman911.playerculling.meme.codec.model.MemeModelPart;
import io.leangen.geantyref.TypeToken;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public class MemeModelPartSerializer implements TypeSerializer<MemeModelPart> {

    public static final MemeModelPartSerializer INSTANCE = new MemeModelPartSerializer();

    @Override
    @Nullable
    public MemeModelPart deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()){
            return null;
        }
        float x = node.node("x").getFloat();
        float y = node.node("y").getFloat();
        float z = node.node("z").getFloat();
        float rotationX = node.node("rotationX").getFloat();
        float rotationY = node.node("rotationY").getFloat();
        float rotationZ = node.node("rotationZ").getFloat();
        List<MemeModelCube> cubes = node.node("cubes").getList(new TypeToken<MemeModelCube>() {}, ArrayList::new);
        Map<String, MemeModelPart> children = new HashMap<>();
        Map<Object, ? extends ConfigurationNode> childrenNodes = node.node("children").childrenMap();
        for (Map.Entry<Object, ? extends ConfigurationNode> childEntry : childrenNodes.entrySet()) {
            String childName = childEntry.getKey().toString();
            MemeModelPart childPart = childEntry.getValue().get(MemeModelPart.class);
            if (childPart != null) {
                children.put(childName, childPart);
            }
        }
        return new MemeModelPart(x, y, z, rotationX, rotationY, rotationZ, cubes, children);
    }

    @Override
    public void serialize(Type type, @Nullable MemeModelPart obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        node.node("x").set(obj.x());
        node.node("y").set(obj.y());
        node.node("z").set(obj.z());
        node.node("rotationX").set(obj.rotationX());
        node.node("rotationY").set(obj.rotationY());
        node.node("rotationZ").set(obj.rotationZ());
        node.node("cubes").setList(new TypeToken<MemeModelCube>() {}, obj.cubes());

        ConfigurationNode childrenNode = node.node("children");
        for (Map.Entry<String, MemeModelPart> children : obj.children().entrySet()) {
            childrenNode.node(children.getKey()).set(MemeModelPart.class, children.getValue());
        }
    }
}
