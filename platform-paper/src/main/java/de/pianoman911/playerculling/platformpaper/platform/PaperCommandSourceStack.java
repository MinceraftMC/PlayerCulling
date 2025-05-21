package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PaperCommandSourceStack implements PlatformCommandSourceStack {

    private final PaperPlatform platform;
    private final CommandSourceStack sourceStack;

    public PaperCommandSourceStack(PaperPlatform platform, CommandSourceStack sourceStack) {
        this.platform = platform;
        this.sourceStack = sourceStack;
    }

    @Override
    public @NotNull PlatformCommandSender getSender() {
        return this.platform.provideCommandSender(this.sourceStack.getSender());
    }

    @Override
    public PlatformEntity getExecutor() {
        Entity executor = this.sourceStack.getExecutor();
        return executor == null ? null : this.platform.provideCommandSender(executor);
    }

    public CommandSourceStack getPaperSourceStack() {
        return sourceStack;
    }
}
