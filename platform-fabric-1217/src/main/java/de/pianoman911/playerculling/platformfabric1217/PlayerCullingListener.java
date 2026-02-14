package de.pianoman911.playerculling.platformfabric1217;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.pianoman911.playerculling.core.commands.PlayerCullingCommand;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformfabric1217.common.IServerLevel;
import de.pianoman911.playerculling.platformfabric1217.platform.FabricCommandSourceStack;
import de.pianoman911.playerculling.platformfabric1217.platform.FabricWorld;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class PlayerCullingListener implements
        ServerPlayConnectionEvents.Join,
        ServerPlayConnectionEvents.Disconnect,
        ServerPlayerEvents.AfterRespawn,
        ServerTickEvents.EndWorldTick,
        ServerLifecycleEvents.ServerStopped,
        CommandRegistrationCallback {

    private final PlayerCullingMod plugin;

    public PlayerCullingListener(PlayerCullingMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register(this);
        ServerPlayConnectionEvents.DISCONNECT.register(this);
        ServerTickEvents.END_WORLD_TICK.register(this);
        ServerLifecycleEvents.SERVER_STOPPED.register(this);
        CommandRegistrationCallback.EVENT.register(this);
    }

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl serverGamePacketListener, PacketSender packetSender, MinecraftServer minecraftServer) {
        PlatformPlayer platformPlayer = this.plugin.getPlatform().providePlayer(serverGamePacketListener.player);
        this.plugin.getCullShip().addPlayer(new CullPlayer(this.plugin.getCullShip(), platformPlayer));
    }

    @Override
    public void onPlayDisconnect(ServerGamePacketListenerImpl serverGamePacketListener, MinecraftServer minecraftServer) {
        this.plugin.getPlatform().invalidatePlayer(serverGamePacketListener.player);
    }

    @Override
    public void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        CullPlayer player = this.plugin.getCullShip().getPlayer(newPlayer.getUUID());
        if (player == null) {
            return; // cull player is null, ignore
        }
        player.setSpectating(false);
    }

    @Override
    public void onEndTick(ServerLevel serverLevel) {
        if (serverLevel.dimension() == ServerLevel.OVERWORLD) {
            // only tick platform once
            this.plugin.getPlatform().tick();
        }
        // tick wrapped world (if it exists)
        FabricWorld world = ((IServerLevel) serverLevel).getCullWorld();
        if (world != null) {
            world.tick();
        }
    }

    @Override
    public void onServerStopped(MinecraftServer server) {
        PlayerCullingMod.getInstance().getPlatform().shutdownScheduler();
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        LiteralCommandNode<CommandSourceStack> node = PlayerCullingCommand.createConverted(this.plugin.getCullShip(),
                platform -> ((FabricCommandSourceStack) platform).getFabricSourceStack(),
                fabric -> this.plugin.getPlatform().provideCommandSourceStack(fabric));
        dispatcher.getRoot().addChild(node);
    }
}
