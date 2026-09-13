package de.pianoman911.playerculling.meme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pianoman911.playerculling.common.config.GsonConfigHolder;
import de.pianoman911.playerculling.meme.extractor.JarLibLoader;
import de.pianoman911.playerculling.meme.extractor.ModelExtractor;
import de.pianoman911.playerculling.meme.mappings.EntityMappings;
import de.pianoman911.playerculling.meme.meta.MemeDescriptor;
import de.pianoman911.playerculling.meme.meta.SemanticVersion;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Stream;

@NullMarked
public class MemeInstance {

    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String DESCRIPTOR_PATH = "meme/descriptors";
    private static final Logger LOGGER = Logger.getLogger("MemeInstance");

    private final Path dataPath;
    private final MemeDownloader memeDownloader = new MemeDownloader(this);
    private final EntityMappings mappings = new EntityMappings(this);
    private final ModelExtractor extractor = new ModelExtractor(this);
    private final SemanticVersion version;
    private @MonotonicNonNull JarLibLoader clientJarLoader;
    private @MonotonicNonNull MemeDescriptor descriptor;

    public MemeInstance(Path dataPath, SemanticVersion version) {
        this.dataPath = dataPath;
        this.version = version;
    }

    public Path getDataPath() {
        return this.dataPath;
    }

    public MemeDownloader getMemeDownloader() {
        return this.memeDownloader;
    }

    public SemanticVersion getVersion() {
        return this.version;
    }

    public MemeDescriptor getDescriptor() {
        if (this.descriptor == null) {
            throw new IllegalStateException("Descriptor has not been determined yet");
        }
        return this.descriptor;
    }

    public EntityMappings getMappings() {
        return this.mappings;
    }

    public EntityMappings getEntityMappings() {
        return this.mappings;
    }

    public JarLibLoader getClientJarLoader() {
        return this.clientJarLoader;
    }

    public void startExtraction() {
        LOGGER.info("Starting extraction for version " + this.version.asVeryShortPrettyString(true));
        this.determineDescriptor();
        this.memeDownloader.beginDownloads();
        if (!this.memeDownloader.isDownloaded()) {
            throw new RuntimeException("Failed to download client jar for version " + this.version.asVeryShortPrettyString(true));
        }

        this.clientJarLoader = new JarLibLoader(this.memeDownloader.getDownloadedPath(), this.memeDownloader.getLibPath());

        this.mappings.build();

        this.extractor.extractModels();
    }

    private void determineDescriptor() {
        URL folderUrl = this.getClass().getResource("/" + DESCRIPTOR_PATH);
        if (folderUrl == null) {
            throw new RuntimeException("Could not find descriptor folder in resources");
        }

        URI uri;
        try {
            uri = folderUrl.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        FileSystem fileSystem = null;
        Path targetPath;
        try {
            if (uri.getScheme().equals("jar")) {
                try {
                    fileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
                String pathStr = uri.toString();
                int separatorIdx = pathStr.indexOf("!/");
                String subPath = pathStr.substring(separatorIdx + 1);
                targetPath = fileSystem.getPath(subPath);
            } else {
                targetPath = Paths.get(uri);
            }

            try (Stream<Path> stream = Files.walk(targetPath, 1)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> {
                            MemeDescriptor descriptor = new GsonConfigHolder<>(MemeDescriptor.class, MemeDescriptor::new,
                                    serializers -> {
                                        serializers.register(SemanticVersion.class, SemanticVersion.Serializer.INSTANCE);
                                    }, path).reloadConfig();
                            if (descriptor.isCompatibleWith(this.version)) {
                                this.descriptor = descriptor;
                                LOGGER.info("Found compatible descriptor for version " + this.version.asVeryShortPrettyString(true) + ": " + path);
                            }
                        });
                if (this.descriptor == null) {
                    throw new RuntimeException("No compatible descriptor found for configVersion " + this.version.asVeryShortPrettyString(true));
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (fileSystem != null && fileSystem.isOpen() && uri.getScheme().equals("jar")) {
                try {
                    fileSystem.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
