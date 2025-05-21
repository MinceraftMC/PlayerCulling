package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionChunkCache;
import de.pianoman911.playerculling.platformcommon.platform.command.BlockPosResolver;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformLivingEntity;
import de.pianoman911.playerculling.platformcommon.util.DebugUtil;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.argument;
import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingBlockDebugCommand {

    private PlayerCullingBlockDebugCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("blockdebug")
                .requires(ctx -> ctx.getExecutor() instanceof PlatformLivingEntity && ctx.getSender().hasPermission("playerculling.command.blockdebug"))
                .executes(ctx -> execute(
                        ctx.getSource().getSender(),
                        (PlatformLivingEntity) ctx.getSource().getExecutor(),
                        false,
                        ((PlatformLivingEntity) ctx.getSource().getExecutor()).getTargetBlock(100)
                ))
                .then(argument("block", ship.getPlatform().getArgumentProvider().blockPos())
                        .executes(ctx -> execute(
                                        ctx.getSource().getSender(),
                                        (PlatformLivingEntity) ctx.getSource().getExecutor(),
                                        false,
                                        ctx.getArgument("block", BlockPosResolver.class).resolve(ctx.getSource())
                                )
                        )
                )
                .then(argument("raw", BoolArgumentType.bool())
                        .executes(ctx -> execute(
                                        ctx.getSource().getSender(),
                                        (PlatformLivingEntity) ctx.getSource().getExecutor(),
                                        BoolArgumentType.getBool(ctx, "raw"),
                                        ((PlatformLivingEntity) ctx.getSource().getExecutor()).getTargetBlock(100)
                                )
                        )
                        .then(argument("block", ship.getPlatform().getArgumentProvider().blockPos())
                                .executes(ctx -> execute(
                                        ctx.getSource().getSender(),
                                        (PlatformLivingEntity) ctx.getSource().getExecutor(),
                                        BoolArgumentType.getBool(ctx, "raw"),
                                        ctx.getArgument("block", BlockPosResolver.class).resolve(ctx.getSource())
                                ))
                        )
                );
    }

    private static int execute(PlatformCommandSender sender, PlatformLivingEntity executor, boolean raw, Vec3i block) {
        OcclusionChunkCache chunk = executor.getWorld().getOcclusionWorldCache().chunk(
                block.getX() >> 4,
                block.getZ() >> 4
        );

        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();

        sender.sendMessage(text("---{ Block Debug }---"));
        sender.sendMessage(text(String.format("Block: %f %f %f", x, y, z)));

        if (raw) {
            sender.sendMessage(text("---Raw---"));
            boolean[] shapes = chunk.isOpaqueFullBlock((int) x, (int) y, (int) z);
            for (int i = 0; i < 8; i++) {
                executor.sendMessage(text(String.format("%d: %s", i, shapes[i])));
            }
        } else {
            sender.sendMessage(text("---Occlusion Cache---"));
            for (int i = 0; i < 8; i++) {
                double offX = (i & 1) * 0.5;
                double offY = ((i >> 1) & 1) * 0.5;
                double offZ = ((i >> 2) & 1) * 0.5;

                executor.sendMessage(text(String.format("%d: %s X: %f;%f;%f", i, chunk.isOccluded(x + offX, y + offY, z + offZ), offX, offY, offZ)));
            }
        }
        executor.sendMessage(text("---------------------"));

        DebugUtil.drawBoundingBox(executor.getWorld(), new AABB(x, y, z, x + 1, y + 1, z + 1));

        return Command.SINGLE_SUCCESS;
    }
}
