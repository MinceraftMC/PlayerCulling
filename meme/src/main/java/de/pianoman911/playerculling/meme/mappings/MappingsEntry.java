package de.pianoman911.playerculling.meme.mappings;

public class MappingsEntry {

    private static final int CLASS_NAME_CUT_LENGTH = ".class".length();

    private final String type;
    private final String classPath;

    public MappingsEntry(String type, String classPath) {
        this.type = type;
        this.classPath = classPath;
    }

    public String getType() {
        return this.type;
    }

    public String getClassPath() {
        return this.classPath;
    }

    public String getClassName() {
        return this.classPath.substring(0, this.classPath.length() - CLASS_NAME_CUT_LENGTH).replace('/', '.');
    }

    @Override
    public String toString() {
        return "MappingsEntry{" +
                "type='" + type + '\'' +
                ", classPath='" + classPath + '\'' +
                '}';
    }
}