package de.pianoman911.playerculling.natives;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NativeLibLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling-NativeLibLoader");
    private static final LibType SYSTEM = LibType.fromSystem();
    private static final String LOCAL_JAR_LIB_FOLDER = "libs";

    public static void loadLib(String name) {
        name += SYSTEM.ext;

        Path libPath;
        Path localPath = Path.of(name);
        if (Files.exists(localPath)) {
            libPath = localPath.toAbsolutePath();
        } else {
            try (InputStream is = NativeLibLoader.class.getResourceAsStream("/" + LOCAL_JAR_LIB_FOLDER + "/" + name)) {
                if (is == null) {
                    throw new IOException("Library " + name + " not found in JAR");
                }

                Path tempDir = Files.createTempDirectory("PlayerCullingNatives");
                libPath = tempDir.resolve(name);
                Files.copy(is, libPath, StandardCopyOption.REPLACE_EXISTING);
                libPath.toFile().deleteOnExit();
            } catch (IOException exception) {
                throw new RuntimeException("Failed to extract native library " + name, exception);
            }
        }
        LOGGER.info("Try loading native library {}...", name);
        System.load(libPath.toString());
        LOGGER.info("Successfully loaded native library {}!", name);
    }

    enum LibType {
        SO(".dll"),
        DLL(".so");

        private final String ext;

        LibType(String ext) {
            this.ext = ext;
        }

        public static LibType fromSystem() {
            return System.getProperty("os.name").toLowerCase().contains("win") ? SO : DLL;
        }
    }
}
