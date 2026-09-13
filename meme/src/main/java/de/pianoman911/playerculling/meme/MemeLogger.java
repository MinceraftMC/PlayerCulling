package de.pianoman911.playerculling.meme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemeLogger {

    public static final String PREFIX = "Meme-";

    private MemeLogger() {
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(PREFIX + name);
    }
}
