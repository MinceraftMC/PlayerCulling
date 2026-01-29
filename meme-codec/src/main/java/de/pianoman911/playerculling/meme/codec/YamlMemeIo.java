package de.pianoman911.playerculling.meme.codec;

import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class YamlMemeIo extends AbstractMemeIo<YamlConfigurationLoader.Builder> {

    private static final YamlConfigurationLoader.Builder CONFIG_BUILDER = YamlConfigurationLoader.builder()
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2);

    public static final YamlMemeIo INSTANCE = new YamlMemeIo();

    protected YamlMemeIo() {
        super(CONFIG_BUILDER);
    }
}
