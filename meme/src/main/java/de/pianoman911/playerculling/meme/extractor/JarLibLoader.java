package de.pianoman911.playerculling.meme.extractor;

import de.pianoman911.playerculling.meme.MemeLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarLibLoader extends ClassLoader {

    private static final Logger LOGGER = MemeLogger.getLogger("JarLibLoader");
    private final Path jarPath;
    private final Path libPath;

    private final List<JarFile> jarFiles = new ArrayList<>();

    private final Map<String, JarFile> classToJarMap = new ConcurrentHashMap<>();

    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    public JarLibLoader(Path jarPath, Path libPath) {
        this.jarPath = jarPath;
        this.libPath = libPath;
        this.buildChildren();
    }

    private void buildChildren() {
        LOGGER.info("Building child jars for {} and libPath {}", this.jarPath, this.libPath);
        Set<Path> paths = new HashSet<>();
        paths.add(this.jarPath);

        if (Files.exists(this.libPath)) {
            try (Stream<Path> walk = Files.walk(this.libPath)) {
                walk.filter(p -> p.toString().endsWith(".jar")).forEach(paths::add);
            } catch (IOException e) {
                throw new RuntimeException("Failed to scan lib path", e);
            }
        }

        for (Path path : paths) {
            try {
                JarFile jarFile = new JarFile(path.toFile());
                this.jarFiles.add(jarFile);

                // Index vorab aufbauen (ermöglicht blitzschnelles Finden)
                var entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        this.classToJarMap.put(entry.getName(), jarFile);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to open jar file: {}", path, e);
            }
        }

        LOGGER.info("Finished build child jars, loaded {} jars", this.jarFiles.size());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = this.loadedClasses.get(name);
        if (clazz != null) {
            return clazz;
        }

        String path = name.replace('.', '/') + ".class";
        JarFile jarFile = this.classToJarMap.get(path);

        if (jarFile != null) {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry != null) {
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    byte[] classBytes = inputStream.readAllBytes();
                    clazz = defineClass(name, classBytes, 0, classBytes.length);
                    this.loadedClasses.put(name, clazz);
                    return clazz;
                } catch (IOException e) {
                    throw new ClassNotFoundException("Failed to read class bytes for " + name, e);
                }
            }
        }

        throw new ClassNotFoundException("Could not load class " + name + " in " + this.jarPath + " libPath: " + this.libPath);
    }
}
