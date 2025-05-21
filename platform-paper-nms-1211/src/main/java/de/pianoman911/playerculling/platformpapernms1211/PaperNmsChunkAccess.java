package de.pianoman911.playerculling.platformpapernms1211;

import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class PaperNmsChunkAccess implements PlatformChunkAccess {

    private final PaperPlatform platform;
    private final ChunkAccess chunk;

    public PaperNmsChunkAccess(PaperPlatform platform, ChunkAccess chunk) {
        this.platform = platform;
        this.chunk = chunk;
    }

    @Override
    public int getBlockId(int x, int y, int z) {
        BlockState blockState = this.chunk.getBlockState(new BlockPos(x, y, z));
        return Block.BLOCK_STATE_REGISTRY.getId(blockState);
    }

    @Override
    public boolean isOpaque(int x, int y, int z, int voxelIndex) {
        return this.platform.getOcclusionMappings().hasVoxel(this.getBlockId(x, y, z), voxelIndex);
    }
}
