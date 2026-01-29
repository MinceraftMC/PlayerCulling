package de.pianoman911.playerculling.meme.codec;

import de.pianoman911.playerculling.meme.codec.model.MemeModel;
import de.pianoman911.playerculling.meme.codec.model.MemeModelCube;
import de.pianoman911.playerculling.meme.codec.model.MemeModelMeta;
import de.pianoman911.playerculling.meme.codec.model.MemeModelPart;
import de.pianoman911.playerculling.meme.codec.serializer.MemeModelCubeSerializer;
import de.pianoman911.playerculling.meme.codec.serializer.MemeModelMetaSerializer;
import de.pianoman911.playerculling.meme.codec.serializer.MemeModelPartSerializer;
import de.pianoman911.playerculling.meme.codec.serializer.MemeModelSerializer;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;

public abstract class AbstractMemeIo<B extends AbstractConfigurationLoader.Builder<B, ?>> {

    private final B builder;

    protected AbstractMemeIo(B loaderBuilder) {
        this.builder = loaderBuilder.defaultOptions(AbstractMemeIo::registerSerializers);
    }

    private static ConfigurationOptions registerSerializers(ConfigurationOptions options) {
        return options.serializers(collection -> {
            collection.register(MemeModel.class, MemeModelSerializer.INSTANCE);
            collection.register(MemeModelMeta.class, MemeModelMetaSerializer.INSTANCE);
            collection.register(MemeModelCube.class, MemeModelCubeSerializer.INSTANCE);
            collection.register(MemeModelPart.class, MemeModelPartSerializer.INSTANCE);
        });
    }

    public void writeMemeModel(MemeModel model, Path path) throws IOException {
        AbstractConfigurationLoader<?> loader = this.builder.path(path).build();
        loader.save(loader.createNode().set(model));
    }

    public MemeModel readMemeModel(Path path) throws IOException {
        AbstractConfigurationLoader<?> loader = this.builder.path(path).build();
        return loader.load().get(MemeModel.class);
    }
}
