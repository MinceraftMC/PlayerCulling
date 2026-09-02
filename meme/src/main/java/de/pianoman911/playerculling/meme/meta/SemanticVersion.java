package de.pianoman911.playerculling.meme.meta;

import org.jspecify.annotations.NullMarked;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public record SemanticVersion(int major, int minor, int patch, String metadata) {

    public static final SemanticVersion MIN_VERSION = new SemanticVersion(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, "");
    public static final SemanticVersion ZERO_VERSION = new SemanticVersion(0, 0, 0, "");
    public static final SemanticVersion MAX_VERSION = new SemanticVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "");

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$");

    public SemanticVersion(int major, int minor, int patch, String metadata) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.metadata = metadata;
    }

    public static SemanticVersion of(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Wrong configVersion format. Use semantic versioning!");
        }
        int major, minor, patch = 0;
        try {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));
            if (matcher.group(3) != null) {
                patch = Integer.parseInt(matcher.group(3));
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Wrong configVersion format. Use semantic versioning!", exception);
        }
        String metadata = "";
        if (version.contains("+")) {
            metadata = version.substring(version.indexOf("+") + 1);
        } else if (version.contains("-")) {
            metadata = version.substring(version.indexOf("-") + 1);
        }
        return new SemanticVersion(major, minor, patch, metadata);
    }

    public boolean isNewerThan(SemanticVersion other) {
        if (this.major != other.major) {
            return this.major > other.major;
        }
        if (this.minor != other.minor) {
            return this.minor > other.minor;
        }
        return this.patch > other.patch;
    }

    public boolean isOlderThan(SemanticVersion other) {
        if (this.major != other.major) {
            return this.major < other.major;
        }
        if (this.minor != other.minor) {
            return this.minor < other.minor;
        }
        return this.patch < other.patch;
    }


    public boolean isEqualTo(SemanticVersion other) {
        return this.major == other.major && this.minor == other.minor && this.patch == other.patch;
    }

    public boolean isEqualOrNewerThan(SemanticVersion other) {
        return isEqualTo(other) || isNewerThan(other);
    }

    public boolean isEqualOrOlderThan(SemanticVersion other) {
        return isEqualTo(other) || isOlderThan(other);
    }

    public String asShortPrettyString(boolean includeMetadata) {
        if (includeMetadata && !this.metadata.isEmpty()) {
            return String.format("%d.%d.%d+%s", this.major, this.minor, this.patch, this.metadata);
        }
        return String.format("%d.%d.%d", this.major, this.minor, this.patch);
    }

    public String asVeryShortPrettyString(boolean includeMetadata) {
        String version = String.format("%d.%d", this.major, this.minor);
        if (this.patch != 0) {
            version += "." + this.patch;
        }
        if (includeMetadata && !this.metadata.isEmpty()) {
            version += "+" + this.metadata;
        }
        return version;
    }

    @Override
    public String toString() {
        return "SematicVersion{" +
                "major=" + this.major +
                ", minor=" + this.minor +
                ", patch=" + this.patch +
                ", metadata='" + this.metadata + '\'' +
                '}';
    }
}