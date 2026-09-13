package de.pianoman911.playerculling.meme.extractor;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarLoader extends ClassLoader {

    protected final Path jarPath;

    public JarLoader(Path jarPath) {
        this.jarPath = jarPath;
    }

    public static void jarWalker(Path jarPath, String innerPath, @Nullable Predicate<Path> filter, Consumer<Path> consumer) {
        innerPath = innerPath.replace('.', '/');
        System.out.println("innerPath: " + innerPath + " -> Path: " + jarPath);
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

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException exception) {
            // Try to load the class from the jar file

            throw new ClassNotFoundException("Could not load class " + name + " from jar " + jarPath);
        }
    }
}
