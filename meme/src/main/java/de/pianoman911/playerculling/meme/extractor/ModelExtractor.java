package de.pianoman911.playerculling.meme.extractor;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.MemeInstance;
import de.pianoman911.playerculling.meme.MemeLogger;
import de.pianoman911.playerculling.meme.mappings.MappingsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class ModelExtractor {

    private static final Logger LOGGER = MemeLogger.getLogger("ModelExtractor");
    private final MemeInstance instance;

    public ModelExtractor(MemeInstance instance) {
        this.instance = instance;
    }

    public void extractModels() {
        LOGGER.info("Extracting models for version {}", this.instance.getVersion());
        for (MappingsEntry entry : this.instance.getMappings().getMappings().values()) {
            try {
                System.out.println("A");
                Class<?> modelClazz = this.instance.getClientJarLoader().loadClass(entry.getClassName());
                System.out.println("B");
                MethodType methodType = MethodType.fromMethodDescriptorString(this.instance.getDescriptor().mappings.defaultLayerCreator,
                        this.instance.getClientJarLoader());
                System.out.println("C");
                MethodHandle creator = ReflectionUtil.getMethod(modelClazz, methodType, 0);
                System.out.println("D");
                Object invoke = creator.invoke();
                System.out.println("Invoked");
                System.out.println(invoke);

            } catch (Throwable exception) {
                throw new RuntimeException(exception);
            }
        }
        System.out.println("Jay");
    }
}
