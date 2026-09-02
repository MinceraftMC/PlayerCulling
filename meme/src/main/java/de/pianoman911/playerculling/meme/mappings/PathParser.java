package de.pianoman911.playerculling.meme.mappings;

import de.pianoman911.playerculling.meme.MemeInstance;

import java.nio.file.Path;

public class PathParser {

    private static final String ADULT = "Adult";
    private static final String BABY = "Baby";

    private final MemeInstance instance;

    public PathParser(MemeInstance instance) {
        this.instance = instance;
    }

    public MappingsEntry parse(Path path) {
        String pathString = path.toString();
        String className = pathString.substring(pathString.lastIndexOf("/") + 1);
        String type = className.substring(0, getIndexOfUpper(className, 0, true));

        if (ageCheck(type)) {
            type = className.substring(0, getIndexOfUpper(className, 2,false));
        }

        return new MappingsEntry(type, pathString);
    }

    public boolean ageCheck(String type) {
        return ADULT.equalsIgnoreCase(type) || BABY.equalsIgnoreCase(type);
    }

    private int getIndexOfUpper(String text, int offset, boolean reverse) {
        int found = 0;
        char[] chars = text.toCharArray();
        if (reverse) {
            for (int i = chars.length - 1; i > 0; i--) {
                if (Character.isUpperCase(chars[i]) && found++ >= offset) {
                    return i;
                }
            }
            return 0;
        } else {
            for (int i = 0; i < chars.length; i++) {
                if (Character.isUpperCase(chars[i]) && found++ >= offset) {
                    return i;
                }
            }
            return text.length() - 1;
        }
    }
}
