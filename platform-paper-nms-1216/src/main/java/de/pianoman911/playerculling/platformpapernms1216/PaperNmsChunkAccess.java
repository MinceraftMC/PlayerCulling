package de.pianoman911.playerculling.platformpapernms1216;

import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlatform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class PaperNmsChunkAccess implements PlatformChunkAccess {

    private static final int VOID_AIR_STATE_ID = Block.BLOCK_STATE_REGISTRY.getId(Blocks.VOID_AIR.defaultBlockState());

    private final PaperPlatform platform;
    private final ChunkAccess chunk;

    public PaperNmsChunkAccess(PaperPlatform platform, ChunkAccess chunk) {
        this.platform = platform;
        this.chunk = chunk;
    }

    @Override
    public int getBlockId(int x, int y, int z) {
        LevelChunkSection section = this.chunk.getSection(this.chunk.getSectionIndex(y));
        if (section != null && !section.hasOnlyAir()) {
            BlockState state = section.getBlockState(x & 0xF, y & 0xF, z & 0xF);
            return Block.BLOCK_STATE_REGISTRY.getId(state);
        }
        return VOID_AIR_STATE_ID;
    }

    @Override
    public boolean isOpaque(int x, int y, int z, int voxelIndex) {
        return this.platform.getOcclusionMappings().hasVoxel(this.getBlockId(x, y, z), voxelIndex);
    }
}
