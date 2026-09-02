package de.pianoman911.playerculling.meme.extractor;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarLoader {

    public static ClassLoader createClassLoader(Path jarPath) {
        return new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                try {
                    return super.findClass(name);
                } catch (ClassNotFoundException exception) {
                    // Try to load the class from the jar file
                    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                        JarEntry entry = jarFile.getJarEntry(name.replace('.', '/') + ".class");
                        if (entry != null) {
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                byte[] classBytes = inputStream.readAllBytes();
                                return defineClass(name, classBytes, 0, classBytes.length);
                            }
                        }
                    } catch (Exception e) {
                        throw new ClassNotFoundException("Could not load class " + name + " from jar " + jarPath, e);
                    }
                    throw new ClassNotFoundException("Could not load class " + name + " from jar " + jarPath);
                }
            }
        };
    }

    public static void jarWalker(Path jarPath, String innerPath, @Nullable Predicate<Path> filter, Consumer<Path> consumer) {
        innerPath = innerPath.replace('.', '/');
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            String finalInnerPath = innerPath;
            jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(finalInnerPath))
                    .map(Paths::get)
                    .filter(path -> filter == null || filter.test(path))
                    .forEach(consumer);
        } catch (IOException e) {
            throw new RuntimeException("Could not walk jar file " + jarPath, e);
        }
    }
}
