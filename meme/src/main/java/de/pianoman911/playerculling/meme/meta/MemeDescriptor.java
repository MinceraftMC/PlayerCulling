package de.pianoman911.playerculling.meme.meta;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.pianoman911.playerculling.meme.MemeInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public record MemeDescriptor(int configVersion, SemanticVersion minVersion, SemanticVersion maxVersion, Mappings mappings) {

    public static MemeDescriptor fromJson(Path path) {
        JsonObject json;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            json = MemeInstance.GSON.fromJson(reader, JsonObject.class);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        int configVersion = json.get("config-version").getAsInt();
        SemanticVersion minVersion = readOrDefault(json, j -> SemanticVersion.of(j.get("min-version").getAsString()), SemanticVersion.MIN_VERSION);
        SemanticVersion maxVersion = readOrDefault(json, j -> SemanticVersion.of(j.get("max-version").getAsString()), SemanticVersion.MAX_VERSION);
        Mappings mappings = Mappings.fromJson(json.getAsJsonObject("mappings"));

        return new MemeDescriptor(configVersion, minVersion, maxVersion, mappings);
    }

    public boolean isCompatibleWith(SemanticVersion version) {
        return !version.isOlderThan(this.minVersion) && !version.isNewerThan(this.maxVersion);
    }

    private static <T> T readOrDefault(JsonObject json, Function<JsonObject, T> function, T defaultValue) {
        try {
            return function.apply(json);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    public record Mappings(String relativeClassPath) {

        public static Mappings fromJson(JsonObject json) {
                String relativeClassPath = json.get("model-classpath").getAsString();
                return new Mappings(relativeClassPath);
            }
        }
}
