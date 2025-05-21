package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionChunkCache;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.util.StringUtil;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingChunkSizesCommand {

    private PlayerCullingChunkSizesCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode() {
        return literal("chunksizes")
                .requires(ctx -> ctx.getExecutor() != null && ctx.getSender().hasPermission("playerculling.command.chunksizes"))
                .executes(ctx -> execute(
                        ctx.getSource().getSender(),
                        Objects.requireNonNull(ctx.getSource().getExecutor())
                ));
    }

    private static int execute(PlatformCommandSender sender, PlatformEntity executor) {
        OcclusionWorldCache world = executor.getWorld().getOcclusionWorldCache();
        sender.sendMessage(text("All Chunk bytes: ", NamedTextColor.GREEN)
                .append(text(StringUtil.toNumInUnits(world.bytes()))));
        List<OcclusionChunkCache> chunks = new ArrayList<>();
        for (OcclusionChunkCache chunk : world.getChunkCache()) {
            chunks.add(chunk);
        }
        chunks.sort((o1, o2) -> Long.compare(o2.byteSize(), o1.byteSize()));
        for (OcclusionChunkCache chunk : chunks) {
            sender.sendMessage(
                    text("Chunk ", NamedTextColor.GREEN)
                            .append(text(chunk.getX(), NamedTextColor.WHITE))
                            .append(text(", ", NamedTextColor.GREEN))
                            .append(text(chunk.getZ(), NamedTextColor.WHITE))
                            .append(text(": ", NamedTextColor.GREEN))
                            .append(text(StringUtil.toNumInUnits(chunk.byteSize()), NamedTextColor.WHITE))
                            .append(text(" bytes", NamedTextColor.GREEN))
                            .hoverEvent(HoverEvent.showText(text("Click to Teleport")))
                            .clickEvent(ClickEvent.callback(audience -> {
                                if (sender instanceof PlatformEntity entity) {
                                    entity.teleport(entity.getWorld(),
                                            chunk.getX() * 16 + 8,
                                            entity.getPosition().getY(),
                                            chunk.getZ() * 16 + 8
                                    );
                                }
                            }))
            );

        }
        return Command.SINGLE_SUCCESS;
    }
}
