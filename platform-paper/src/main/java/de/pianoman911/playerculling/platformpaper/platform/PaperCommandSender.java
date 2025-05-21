package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.command.CommandSender;

public class PaperCommandSender<T extends CommandSender> implements PlatformCommandSender {

    protected final PaperPlatform platform;
    protected final T sender;

    public PaperCommandSender(PaperPlatform platform, T sender) {
        this.platform = platform;
        this.sender = sender;
    }

    public final T getDelegate() {
        return this.sender;
    }

    @Override
    public IPlatform getPlatform() {
        return this.platform;
    }

    @Override
    public void sendMessage(Component message) {
        this.sender.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission, TriState defaultValue) {
        if (!defaultValue.equals(TriState.NOT_SET)) {
            if (this.sender.isPermissionSet(permission)){
                return Boolean.TRUE.equals(defaultValue.toBoolean());
            }
        }
        return this.sender.hasPermission(permission);
    }
}
