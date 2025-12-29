package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.core.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.core.util.CameraMode;
import de.pianoman911.playerculling.core.util.ClientsideUtil;
import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.command.SinglePlayerResolver;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.DebugUtil;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.Color;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.argument;
import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;


public final class PlayerCullingRayCastDebugCommand {

    private PlayerCullingRayCastDebugCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("raycastdebug")
                .requires(ctx -> ctx.getExecutor() instanceof PlatformPlayer && ctx.getSender().hasPermission("playerculling.command.raycastdebug"))
                .then(argument("target", ship.getPlatform().getArgumentProvider().player())
                        .executes(ctx -> execute(
                                ctx.getSource().getSender(),
                                (PlatformPlayer) ctx.getSource().getExecutor(),
                                ship,
                                false,
                                false,
                                ctx.getArgument("target", SinglePlayerResolver.class).resolve(ctx.getSource())))
                        .then(argument("showRay", BoolArgumentType.bool())
                                .executes(ctx -> execute(
                                        ctx.getSource().getSender(),
                                        (PlatformPlayer) ctx.getSource().getExecutor(),
                                        ship,
                                        ctx.getArgument("showRay", Boolean.class),
                                        false,
                                        ctx.getArgument("target", SinglePlayerResolver.class).resolve(ctx.getSource())))
                        )
                        .then(argument("blocks", BoolArgumentType.bool())
                                .executes(ctx -> execute(
                                        ctx.getSource().getSender(),
                                        (PlatformPlayer) ctx.getSource().getExecutor(),
                                        ship,
                                        false,
                                        ctx.getArgument("blocks", Boolean.class),
                                        ctx.getArgument("target", SinglePlayerResolver.class).resolve(ctx.getSource())))
                                .then(argument("showRay", BoolArgumentType.bool())
                                        .executes(ctx -> execute(
                                                ctx.getSource().getSender(),
                                                (PlatformPlayer) ctx.getSource().getExecutor(),
                                                ship,
                                                ctx.getArgument("showRay", Boolean.class),
                                                ctx.getArgument("blocks", Boolean.class),
                                                ctx.getArgument("target", SinglePlayerResolver.class).resolve(ctx.getSource())))
                                )
                        )
                );
    }

    public static int execute(PlatformCommandSender sender, PlatformPlayer executor, CullShip ship, boolean showRay, boolean blocks, PlatformPlayer target) {
        CullPlayer cullPlayer = ship.getPlayer(executor.getUniqueId());
        PlatformPlayer platformPlayer = cullPlayer.getPlatformPlayer();
        DataProvider provider = new DebuggingDataProviderChunk(cullPlayer, sender, showRay, blocks);
        OcclusionCullingInstance instance = new OcclusionCullingInstance(provider);

        if (!target.getWorld().equals(executor.getWorld())) {
            sender.sendMessage(text("Target is not in the same world", NamedTextColor.RED));
            return 0;
        }

        provider.world(executor.getWorld());

        AABB trackedBox = target.getBoundingBox();
        DebugUtil.drawBoundingBox(executor.getWorld(), trackedBox, Color.ORANGE);
        executor.getWorld().spawnColoredParticle(executor.getEyePosition(), Color.CYAN, 0.5f);

        Vec3d centerBox = trackedBox.getCenter().mul(2);
        Vec3d dir = executor.getDirection();

        Vec3d aabbMin = trackedBox.getMin().mul(2); // For 2x2x2 Shapes
        Vec3d aabbMax = trackedBox.getMax().mul(2);

        Vec3d eye = new Vec3d(executor.getEyePosition().getX(), executor.getEyePosition().getY(), executor.getEyePosition().getZ());

        Vec3d viewerPosition = new Vec3d();
        Vec3d viewerBack = new Vec3d();
        Vec3d viewerFront = new Vec3d();

        viewerPosition.set(eye.getX(), eye.getY(), eye.getZ());
        viewerPosition.mul(2);
        boolean visible = instance.isAABBVisible(aabbMin, aabbMax, viewerPosition);
        boolean angle = CullPlayer.isInnerAngle(centerBox, viewerPosition, dir);
        double a = Math.toDegrees(CullPlayer.angle(centerBox, viewerPosition, dir));
        sender.sendMessage(text("FirstPerson Visible: " + visible + " Angle(" + a + "): " + angle + " -> " + (visible && angle)));

        viewerBack.set(eye.getX(), eye.getY(), eye.getZ());
        ClientsideUtil.addPlayerViewOffset(viewerBack, platformPlayer, CameraMode.THIRD_PERSON_BACK);
        viewerBack.mul(2);
        visible = instance.isAABBVisible(aabbMin, aabbMax, viewerBack);
        angle = CullPlayer.isInnerAngle(centerBox, viewerBack, dir);
        a = Math.toDegrees(CullPlayer.angle(centerBox, viewerBack, dir));
        sender.sendMessage(text("ThirdPersonBack Visible: " + visible + " Angle(" + a + "): " + angle + " -> " + (visible && angle)));


        viewerFront.set(eye.getX(), eye.getY(), eye.getZ());
        ClientsideUtil.addPlayerViewOffset(viewerFront, platformPlayer, CameraMode.THIRD_PERSON_FRONT);
        viewerFront.mul(2);
        visible = instance.isAABBVisible(aabbMin, aabbMax, viewerFront);
        angle = CullPlayer.isInnerAngle(centerBox, viewerFront, dir.mul(-1));
        a = Math.toDegrees(CullPlayer.angle(centerBox, viewerFront, dir));
        sender.sendMessage(text("ThirdPersonFront Visible: " + visible + " Angle(" + a + "): " + angle + " -> " + (visible && angle)));

        return Command.SINGLE_SUCCESS;
    }

    public static class DebuggingDataProviderChunk implements DataProvider {

        private final DataProvider delegate;

        private final CullPlayer player;
        private final PlatformCommandSender sender;
        private final boolean blocks;
        private final boolean showRays;

        private DebuggingDataProviderChunk(CullPlayer player, PlatformCommandSender sender, boolean showRays, boolean blocks) {
            this.delegate = player.getProvider();
            this.player = player;
            this.sender = sender;
            this.showRays = showRays;
            this.blocks = blocks;
        }

        @Override
        public boolean isOpaqueFullCube(int x, int y, int z) {
            boolean occluded = this.delegate.isOpaqueFullCube(x, y, z);
            Vec3d pos = new Vec3d(x / 2.0, y / 2.0, z / 2.0).add(0.25, 0.25, 0.25);
            if (this.showRays) {
                this.player.getPlatformPlayer().getWorld().spawnColoredParticle(pos, occluded ? Color.RED : Color.GREEN, 0.5f);
            }
            if (this.blocks) {
                String state = this.player.getPlatformPlayer().getWorld().getBlockStateStringOfBlock(pos.toVec3iFloored());
                this.sender.sendMessage(text("Block: " + state + "at " + pos + " -> " + occluded));
            }
            return occluded;
        }

        @Override
        public void world(PlatformWorld world) {
            this.delegate.world(world);
        }

        @Override
        public int getPlayerViewDistance() {
            return this.delegate.getPlayerViewDistance();
        }
    }
}
