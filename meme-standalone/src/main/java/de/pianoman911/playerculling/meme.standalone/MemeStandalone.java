package de.pianoman911.playerculling.meme.standalone;

import de.pianoman911.playerculling.meme.MemeInstance;
import de.pianoman911.playerculling.meme.meta.SemanticVersion;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;

import java.nio.file.Path;

public class MemeStandalone {

    static {
        System.setProperty("java.util.logging.manager", org.apache.logging.log4j.jul.LogManager.class.getName());
        System.setProperty("java.awt.headless", "true");

        System.setOut(IoBuilder.forLogger("STDOUT").setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger("STDERR").setLevel(Level.ERROR).buildPrintStream());

        // some more classes which frequently cause errors on shutdown
        try {
            Class.forName("org.jline.reader.Macro").getDeclaredFields();
            Class.forName("org.jline.reader.impl.LineReaderImpl$3").getDeclaredFields();
            Class.forName("org.jline.reader.Parser$ParseContext").getDeclaredFields();
            Class.forName("org.jline.reader.impl.DefaultParser$BracketChecker").getDeclaredFields();
            Class.forName("org.jline.reader.impl.DefaultParser$ArgumentList").getDeclaredFields();
            Class.forName("org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory$StatusMessage").getDeclaredFields();
            Class.forName("org.apache.logging.log4j.core.util.Throwables").getDeclaredFields();
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(exception);
        }
    }

    public static void main(String[] args) {
        MemeInstance instance = new MemeInstance(Path.of("./meme_data"), SemanticVersion.of("26.2.0"));
        instance.startExtraction();
    }
}
