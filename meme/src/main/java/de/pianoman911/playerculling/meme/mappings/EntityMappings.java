package de.pianoman911.playerculling.meme.mappings;

import de.pianoman911.playerculling.meme.MemeInstance;
import de.pianoman911.playerculling.meme.extractor.JarLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EntityMappings {

    private final Map<String, MappingsEntry> mappings = new HashMap<>();

    private final MemeInstance instance;
    private final PathParser parser;

    public EntityMappings(MemeInstance instance) {
        this.instance = instance;
        this.parser = new PathParser(instance);
    }

    public void build() {
        String relativeClassPath = this.instance.getDescriptor().mappings().relativeClassPath();
        JarLoader.jarWalker(this.instance.getMemeDownloader().getDownloadedPath(), relativeClassPath,
                path -> {
                    String pathString = path.toString();
                    return pathString.contains("Model") && // Extract only Models
                            !pathString.contains("$"); // Ignore nested classes
                }, this::extractFromPath
        );
    }

    private void extractFromPath(Path path) {
        MappingsEntry entry = this.parser.parse(path);
        this.mappings.put(entry.getType(), entry);
    }

    public Map<String, MappingsEntry> getMappings() {
        return this.mappings;
    }
}
