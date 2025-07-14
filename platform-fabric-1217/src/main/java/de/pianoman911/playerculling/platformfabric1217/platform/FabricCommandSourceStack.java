package de.pianoman911.playerculling.platformfabric1217.platform;

import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class FabricCommandSourceStack implements PlatformCommandSourceStack {

    private final FabricPlatform platform;
    private final CommandSourceStack sourceStack;

    public FabricCommandSourceStack(FabricPlatform platform, CommandSourceStack sourceStack) {
        this.platform = platform;
        this.sourceStack = sourceStack;
    }

    @Override
    public @NotNull PlatformCommandSender getSender() {
        return this.platform.provideCommandSender(this.sourceStack.source);
    }

    @Override
    public PlatformEntity getExecutor() {
        Entity entity = this.sourceStack.getEntity();
        return entity == null ? null : this.platform.provideEntity(entity);
    }

    public CommandSourceStack getFabricSourceStack() {
        return this.sourceStack;
    }
}
