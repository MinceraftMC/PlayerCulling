package de.pianoman911.playerculling.platformpaper.util;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

// adventure created this epic api for loading java services...
// and then they messed it up! why do I have to use system properties?
// this should be configurable using method parameters and not using system properties!
public final class ServicesUtil {

    private ServicesUtil() {
    }

    public static <T> T loadService(Class<? extends T> clazz) {
        ServiceLoader<? extends T> loader = ServiceLoader.load(clazz, clazz.getClassLoader());
        for (Iterator<? extends T> it = loader.iterator(); it.hasNext(); ) {
            try {
                return it.next();
            } catch (ServiceConfigurationError ignored) {
                // try the next one
            }
        }
        throw new IllegalStateException("Failed to load service " + clazz.getName() + ", no valid implementation found");
    }
}
