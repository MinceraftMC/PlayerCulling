package de.pianoman911.playerculling.platformfabric1214.platform;

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minecraft.commands.CommandSource;

public class FabricCommandSender<T extends CommandSource> implements PlatformCommandSender {

    protected final FabricPlatform platform;
    protected T sender;

    public FabricCommandSender(FabricPlatform platform, T sender) {
        this.platform = platform;
        this.sender = sender;
    }

    protected T getDelegate() {
        return this.sender;
    }

    @Override
    public IPlatform getPlatform() {
        return this.platform;
    }

    @Override
    public void sendMessage(Component message) {

    }

    @Override
    public boolean hasPermission(String permission, TriState state) {
        return false;
    }
}
