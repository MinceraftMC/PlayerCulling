package de.pianoman911.playerculling.meme.codec.serializer;

import de.pianoman911.playerculling.meme.codec.model.MemeModel;
import de.pianoman911.playerculling.meme.codec.model.MemeModelMeta;
import de.pianoman911.playerculling.meme.codec.model.MemeModelPart;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public class MemeModelSerializer implements TypeSerializer<MemeModel> {

    public static final MemeModelSerializer INSTANCE = new MemeModelSerializer();

    @Override
    @Nullable
    public MemeModel deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()){
            return null;
        }
        MemeModelMeta meta = Objects.requireNonNull(node.node("meta").get(MemeModelMeta.class));
        Map<Object, ? extends ConfigurationNode> partsNodes = node.node("parts").childrenMap();
        Map<String, MemeModelPart> parts = new java.util.HashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> partEntry : partsNodes.entrySet()) {
            String partName = partEntry.getKey().toString();
            MemeModelPart part = partEntry.getValue().get(MemeModelPart.class);
            if (part != null) {
                parts.put(partName, part);
            }
        }
        return new MemeModel(meta, parts);
    }

    @Override
    public void serialize(Type type, @Nullable MemeModel obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.node("meta").set(obj.meta());
        ConfigurationNode partsNode = node.node("parts");
        for (Map.Entry<String, MemeModelPart> partEntry : obj.parts().entrySet()) {
            partsNode.node(partEntry.getKey()).set(partEntry.getValue());
        }
    }
}
