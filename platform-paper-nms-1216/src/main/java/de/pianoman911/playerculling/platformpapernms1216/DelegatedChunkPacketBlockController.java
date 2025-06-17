package de.pianoman911.playerculling.platformpapernms1216;

import com.destroystokyo.paper.util.SneakyThrow;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.antixray.ChunkPacketInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.lang.invoke.MethodHandle;

// Used for listening to all block changes
public class DelegatedChunkPacketBlockController extends ChunkPacketBlockController {

    private static final MethodHandle GET_CHUNK_PACKET_BLOCK_CONTROLLER = ReflectionUtil.getGetter(Level.class, ChunkPacketBlockController.class, 0);
    private static final MethodHandle SET_CHUNK_PACKET_BLOCK_CONTROLLER = ReflectionUtil.getSetter(Level.class, ChunkPacketBlockController.class, 0);

    private final ChunkPacketBlockController delegate;
    private final BlockChangeListener listener;

    public DelegatedChunkPacketBlockController(ChunkPacketBlockController delegate, BlockChangeListener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void inject(ServerLevel level, BlockChangeListener listener) {
        try {
            ChunkPacketBlockController controller = (ChunkPacketBlockController) GET_CHUNK_PACKET_BLOCK_CONTROLLER.invoke(level);
            SET_CHUNK_PACKET_BLOCK_CONTROLLER.invoke(level, new DelegatedChunkPacketBlockController(controller, listener));
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    @Override
    public final BlockState[] getPresetBlockStates(Level level, ChunkPos chunkPos, int chunkSectionY) {
        return this.delegate.getPresetBlockStates(level, chunkPos, chunkSectionY);
    }

    @Override
    public final boolean shouldModify(ServerPlayer player, LevelChunk chunk) {
        return this.delegate.shouldModify(player, chunk);
    }

    @Override
    public final ChunkPacketInfo<BlockState> getChunkPacketInfo(ClientboundLevelChunkWithLightPacket chunkPacket, LevelChunk chunk) {
        return this.delegate.getChunkPacketInfo(chunkPacket, chunk);
    }

    @Override
    public final void modifyBlocks(ClientboundLevelChunkWithLightPacket chunkPacket, ChunkPacketInfo<BlockState> chunkPacketInfo) {
        this.delegate.modifyBlocks(chunkPacket, chunkPacketInfo);
    }

    @Override
    public final void onBlockChange(Level level, BlockPos blockPos, BlockState newBlockState, BlockState oldBlockState, int flags, int maxUpdateDepth) {
        this.delegate.onBlockChange(level, blockPos, newBlockState, oldBlockState, flags, maxUpdateDepth);
        this.listener.handle(level, blockPos, oldBlockState, newBlockState);
    }

    @Override
    public final void onPlayerLeftClickBlock(ServerPlayerGameMode serverPlayerGameMode, BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence) {
        this.delegate.onPlayerLeftClickBlock(serverPlayerGameMode, blockPos, action, direction, worldHeight, sequence);
    }

    @FunctionalInterface
    public interface BlockChangeListener {

        void handle(Level level, BlockPos pos, BlockState oldState, BlockState newState);
    }
}
