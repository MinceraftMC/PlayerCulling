package de.pianoman911.playerculling.meme.meta;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashSet;
import java.util.Set;

@ConfigSerializable
public class MemeDescriptor {

    public int configVersion = 1;
    public SemanticVersion minVersion = SemanticVersion.MIN_VERSION;
    public SemanticVersion maxVersion = SemanticVersion.MAX_VERSION;
    public Mappings mappings;
    public Set<String> libraries = new HashSet<>();

    public boolean isCompatibleWith(SemanticVersion version) {
        return !version.isOlderThan(this.minVersion) && !version.isNewerThan(this.maxVersion);
    }

    @ConfigSerializable
    public static class Mappings {

        public String relativeClassPath;
        public String defaultLayerCreator;
    }
}
