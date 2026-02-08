package de.pianoman911.playerculling.meme.generator.config;

import com.google.common.collect.Table;
import de.pianoman911.playerculling.meme.generator.AbstractGenerator;
import io.leangen.geantyref.TypeToken;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class GeneratorConfigSerializer implements TypeSerializer<GeneratorConfig> {

    @Override
    public GeneratorConfig deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.virtual()) {
            return null;
        }
        GeneratorConfig config = new GeneratorConfig();
        ConfigurationNode fieldBlacklistNode = node.node("fieldBlacklist");
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : fieldBlacklistNode.childrenMap().entrySet()) {
            String className = entry.getKey().toString();
            try {
                Class<?> clazz = Class.forName(className);
                String reason = entry.getValue().getString("");
                config.getFieldBlacklist().put(clazz, reason);
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException("Class not found: " + className, exception);
            }
        }

        ConfigurationNode generatorsNode = node.node("generators");
        for (Map.Entry<Object, ? extends ConfigurationNode> classes : generatorsNode.childrenMap().entrySet()) {
            String className = classes.getKey().toString();
            try {
                Class<?> clazz = Class.forName(className);
                ConfigurationNode classNode = classes.getValue();
                for (Map.Entry<Object, ? extends ConfigurationNode> fields : classNode.childrenMap().entrySet()) {
                    String fieldName = fields.getKey().toString();
                    AbstractGenerator<?, ?, ?> generator = fields.getValue().get(new TypeToken<AbstractGenerator<?, ?, ?>>() {});
                    config.getGenerators().put(clazz, fieldName, generator);
                }
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException("Class not found: " + className, exception);
            }
        }
        return config;
    }

    @Override
    public void serialize(Type type, @Nullable GeneratorConfig obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        Map<Class<?>, String> fieldBlacklist = obj.getFieldBlacklist();
        ConfigurationNode fieldBlacklistNode = node.node("fieldBlacklist");
        for (Map.Entry<Class<?>, String> entry : fieldBlacklist.entrySet()) {
            fieldBlacklistNode.node(entry.getKey().getName()).set(entry.getValue());
        }

        Table<Class<?>, String, AbstractGenerator<?, ?, ?>> generators = obj.getGenerators();
        ConfigurationNode generatorsNode = node.node("generators");
        for (Map.Entry<Class<?>, Map<String, AbstractGenerator<?, ?, ?>>> classes : generators.rowMap().entrySet()) {
            ConfigurationNode classNode = generatorsNode.node(classes.getKey().getName());
            for (Map.Entry<String, AbstractGenerator<?, ?, ?>> fields : classes.getValue().entrySet()) {
                classNode.node(fields.getKey()).set(new TypeToken<AbstractGenerator<?, ?, ?>>() {}, fields.getValue());
            }
        }
    }
}
