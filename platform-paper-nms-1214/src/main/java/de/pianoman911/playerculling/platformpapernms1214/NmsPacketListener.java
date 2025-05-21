package de.pianoman911.playerculling.platformpapernms1214;

import com.destroystokyo.paper.util.SneakyThrow;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerPlayer;

import java.lang.invoke.MethodHandle;

@ChannelHandler.Sharable
public class NmsPacketListener extends ChannelOutboundHandlerAdapter {

    private static final MethodHandle GET_CAMERA_ID = ReflectionUtil.getGetter(ClientboundSetCameraPacket.class, int.class, 0);

    private final CullShip ship;

    public NmsPacketListener(CullShip ship) {
        this.ship = ship;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void handleCamera(ChannelHandlerContext ctx, ClientboundSetCameraPacket packet) {
        ServerPlayer nmsPlayer = ((Connection) ctx.pipeline().get("packet_handler")).getPlayer();
        CullPlayer player = this.ship.getPlayer(nmsPlayer.getUUID());
        if (player == null) {
            return; // cull player is null, ignore
        }
        int cameraId;
        try {
            cameraId = (int) GET_CAMERA_ID.invoke(packet);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            throw new AssertionError();
        }
        player.setSpectating(cameraId != nmsPlayer.getId());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSetCameraPacket packet) {
            this.handleCamera(ctx, packet);
        }
        super.write(ctx, msg, promise);
    }
}
