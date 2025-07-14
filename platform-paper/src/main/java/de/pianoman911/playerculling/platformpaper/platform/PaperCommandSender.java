package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.lang.ref.WeakReference;

@NullMarked
public class PaperCommandSender<T extends CommandSender> implements PlatformCommandSender {

    protected final PaperPlatform platform;
    protected final WeakReference<T> sender;

    public PaperCommandSender(PaperPlatform platform, T sender) {
        this.platform = platform;
        this.sender = new WeakReference<>(sender);
    }

    public final T getDelegate() {
        T delegate = this.sender.get();
        if (delegate != null) {
            return delegate;
        }
        throw new IllegalStateException("Delegate of " + this + " has been garbage collected");
    }

    @Override
    public IPlatform getPlatform() {
        return this.platform;
    }

    @Override
    public void sendMessage(Component message) {
        this.getDelegate().sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission, TriState defaultValue) {
        T delegate = this.getDelegate();
        if (defaultValue != TriState.NOT_SET && !delegate.isPermissionSet(permission)) {
            return defaultValue == TriState.TRUE;
        }
        return delegate.hasPermission(permission);
    }
}
