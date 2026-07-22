package de.pianoman911.playerculling.core.provider;

import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkOcclusionDataProviderTest {

    @Test
    @SuppressWarnings("DataFlowIssue")
    void switchingWorldsInvalidatesChunkWithMatchingCoordinates() {
        TestWorld opaqueWorld = new TestWorld(true);
        TestWorld emptyWorld = new TestWorld(false);
        ChunkOcclusionDataProvider provider = new ChunkOcclusionDataProvider(null);

        provider.world(opaqueWorld);
        assertTrue(provider.isOpaqueFullCube(0, 0, 0));

        provider.world(emptyWorld);
        assertFalse(provider.isOpaqueFullCube(0, 0, 0));
    }

    private static IPlatform testPlatform() {
        return (IPlatform) Proxy.newProxyInstance(
                IPlatform.class.getClassLoader(),
                new Class<?>[]{IPlatform.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getCurrentTick")) {
                        return 0L;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static final class TestWorld extends PlatformWorld {

        private final PlatformChunkAccess chunk;

        private TestWorld(boolean opaque) {
            super(testPlatform());
            this.chunk = new PlatformChunkAccess() {
                @Override
                public int getBlockId(int x, int y, int z) {
                    return 0;
                }

                @Override
                public boolean isOpaque(int x, int y, int z, int voxelIndex) {
                    return opaque;
                }
            };
        }

        @Override
        public PlatformChunkAccess getChunkAccess(int x, int z) {
            return x == 0 && z == 0 ? this.chunk : null;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public Key getKey() {
            return Key.key("playerculling", "test");
        }

        @Override
        public int getMinY() {
            return 0;
        }

        @Override
        public int getMaxY() {
            return 1;
        }

        @Override
        public int getTrackingDistance() {
            return 0;
        }

        @Override
        public int getTrackingDistance(PlatformPlayer player) {
            return 0;
        }

        @Override
        protected List<PlatformPlayer> getPlayers0() {
            return List.of();
        }

        @Override
        public Vec3d rayTraceBlocks(Vec3d start, Vec3d dir, double maxDistance) {
            return null;
        }

        @Override
        public void spawnColoredParticle(double x, double y, double z, Color color, float size) {
        }

        @Override
        public String getBlockStateStringOfBlock(Vec3i blockPos) {
            return "air";
        }
    }
}
