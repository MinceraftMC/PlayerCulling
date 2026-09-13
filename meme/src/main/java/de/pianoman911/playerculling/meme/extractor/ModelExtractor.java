package de.pianoman911.playerculling.meme.extractor;

import de.pianoman911.playerculling.common.ReflectionUtil;
import de.pianoman911.playerculling.meme.MemeInstance;
import de.pianoman911.playerculling.meme.MemeLogger;
import de.pianoman911.playerculling.meme.data.Cube;
import de.pianoman911.playerculling.meme.data.CubeDeformation;
import de.pianoman911.playerculling.meme.data.Model;
import de.pianoman911.playerculling.meme.data.PartPose;
import de.pianoman911.playerculling.meme.mappings.MappingsEntry;
import de.pianoman911.playerculling.meme.wrapped.WrappedCubeDefinition;
import de.pianoman911.playerculling.meme.wrapped.WrappedCubeDeformation;
import de.pianoman911.playerculling.meme.wrapped.WrappedLayerDefinition;
import de.pianoman911.playerculling.meme.wrapped.WrappedPartDefinition;
import de.pianoman911.playerculling.meme.wrapped.WrappedPartPose;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModelExtractor {

    private static final Logger LOGGER = MemeLogger.getLogger("ModelExtractor");
    private final MemeInstance instance;
    private final Set<Model> extractedModels = ConcurrentHashMap.newKeySet();

    public ModelExtractor(MemeInstance instance) {
        this.instance = instance;
    }

    public void extractModels() {
        LOGGER.info("Extracting models for version {}", this.instance.getVersion());
        long started = System.currentTimeMillis();
        for (MappingsEntry entry : this.instance.getMappings().getMappings().values()) {
            try {
                Class<?> modelClazz = this.instance.getClientJarLoader().loadClass(entry.getClassName());

                MethodType methodType = MethodType.fromMethodDescriptorString(this.instance.getDescriptor().mappings.defaultLayerCreator,
                        this.instance.getClientJarLoader());

                MethodHandle creator;
                try {
                    creator = ReflectionUtil.getMethod(modelClazz, methodType, 0);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                WrappedLayerDefinition.Instance layerDefinition = new WrappedLayerDefinition.Instance(creator.invoke());
                Model model = this.extractModel(layerDefinition);
                this.extractedModels.add(model);
            } catch (Throwable exception) {
                throw new RuntimeException(exception);
            }
        }

        double processTime = (System.currentTimeMillis() - started) / 1000d;
        processTime = Math.round(processTime * 1000d) / 1000d;
        LOGGER.info("Finished extracting models, found {} models in {}s", this.extractedModels.size(), processTime);
    }

    private Model extractModel(WrappedLayerDefinition.Instance layerDefinition) {
        return this.extractModel(layerDefinition.getMesh().getRoot());
    }

    private Model extractModel(WrappedPartDefinition.Instance part) {
        List<WrappedCubeDefinition.Instance> wrappedCubes = part.getCubes();
        List<Cube> cubes = new ArrayList<>(wrappedCubes.size());
        for (WrappedCubeDefinition.Instance wrappedCube : wrappedCubes) {
            WrappedCubeDeformation.Instance wrappedCubeGrow = wrappedCube.getGrow();
            cubes.add(new Cube(
                    wrappedCube.getComment(),
                    wrappedCube.getOrigin(),
                    wrappedCube.getDimensions(),
                    new CubeDeformation(wrappedCubeGrow.getGrowX(), wrappedCubeGrow.getGrowY(), wrappedCubeGrow.getGrowZ()),
                    wrappedCube.isMirror()
            ));
        }
        WrappedPartPose.Instance wrappedPartPose = part.getPartPose();
        PartPose partPose = new PartPose(
                wrappedPartPose.getX(),
                wrappedPartPose.getY(),
                wrappedPartPose.getZ(),
                wrappedPartPose.getXRot(),
                wrappedPartPose.getYRot(),
                wrappedPartPose.getZRot(),
                wrappedPartPose.getXScale(),
                wrappedPartPose.getYScale(),
                wrappedPartPose.getZScale()
        );

        Map<String, Model> children = new HashMap<>();
        for (Map.Entry<String, WrappedPartDefinition.Instance> entry : part.getChildren().entrySet()) {
            children.put(entry.getKey(), this.extractModel(entry.getValue()));
        }
        return new Model(cubes, partPose, children);
    }
}
