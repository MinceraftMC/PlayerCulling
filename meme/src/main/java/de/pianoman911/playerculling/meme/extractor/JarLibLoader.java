package de.pianoman911.playerculling.meme.extractor;

import de.pianoman911.playerculling.meme.MemeLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarLibLoader extends ClassLoader {

    private static final Logger LOGGER = MemeLogger.getLogger("JarLibLoader");
    private final Path jarPath;
    private final Path libPath;
    private final Set<Path> jars = new HashSet<>();
    private final WeakHashMap<String, Class<?>> loadedClasses = new WeakHashMap<>();

    public JarLibLoader(Path jarPath, Path libPath) {
        this.jarPath = jarPath;
        this.libPath = libPath;

        this.buildChildren();
    }

    private void buildChildren() {
        LOGGER.info("Building child jars for {} and libPath {}", this.jarPath, this.libPath);
        this.buildChild(this.libPath);
        LOGGER.info("Finished build child jars, found {} jars", this.jars.size());
    }

    private void buildChild(Path path) {
        try (Stream<Path> files = Files.list(path)) {
            files.forEach(p -> {
                if (Files.isDirectory(p)) {
                    this.buildChild(p);
                }
                if (p.toString().endsWith(".jar")) {
                    this.jars.add(p);
                }
            });
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        this.jars.add(this.jarPath);
    }

    @Override
    protected Class<?> findClass(String name) {
        return this.loadedClasses.computeIfAbsent(name, n -> {
            try {
                return this.findClass0(n);
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    protected Class<?> findClass0(String name) throws ClassNotFoundException{
        String path = name.replace('.', '/') + ".class";
        for (Path jar : this.jars) {
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                JarEntry entry = jarFile.getJarEntry(path);
                if (entry != null) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        byte[] classBytes = inputStream.readAllBytes();
                        return defineClass(name, classBytes, 0, classBytes.length);
                    }
                }
            } catch (Exception ignored1) {
            }
        }
        throw new ClassNotFoundException("Could not load class " + name + " in " + this.jarPath + " libPath: " + this.libPath);
    }
}
