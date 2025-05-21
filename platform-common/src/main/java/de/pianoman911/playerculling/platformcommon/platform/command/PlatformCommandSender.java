package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface PlatformCommandSender {

    IPlatform getPlatform();

    void sendMessage(Component message);

    boolean hasPermission(String permission, TriState defaultValue);

    default boolean hasPermission(String permission, @Nullable Boolean defaultValue) {
        return this.hasPermission(permission, TriState.byBoolean(defaultValue));
    }

    default boolean hasPermission(String permission) {
        return this.hasPermission(permission, TriState.NOT_SET);
    }
}
