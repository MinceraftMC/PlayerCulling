package de.pianoman911.playerculling.meme.extractor;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.MemeInstance;
import de.pianoman911.playerculling.meme.mappings.MappingsEntry;

import java.nio.file.Path;

public class ModelExtractor {

    private final MemeInstance instance;

    public ModelExtractor(MemeInstance instance) {
        this.instance = instance;
    }

    public void extractModels() {
        Path path = this.instance.getMemeDownloader().getDownloadedPath();
        ClassLoader classLoader = JarLoader.createClassLoader(path);

        for (MappingsEntry entry : this.instance.getMappings().getMappings().values()) {
            try {
                Class<?> modelClazz = classLoader.loadClass(entry.getClassPath());
                ReflectionUtil.getMethod(modelClazz,Me)
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
