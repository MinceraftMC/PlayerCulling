package de.pianoman911.playerculling.meme.mappings;

public class MappingsEntry {

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

    @Override
    public String toString() {
        return "MappingsEntry{" +
                "type='" + type + '\'' +
                ", classPath='" + classPath + '\'' +
                '}';
    }
}