package de.pianoman911.playerculling.platformcommon.cache;

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
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OcclusionChunkCacheTest {

    @Test
    void emptyChunkProducesEmptyComputedCache() throws InterruptedException {
        TestWorld world = new TestWorld(-2, 3);
        OcclusionChunkCache cache = world.getOcclusionWorldCache().chunk(0, 0);

        await(cache::isFullyComputed);

        assertEquals(0, cache.getHeight());
        assertEquals(-2, cache.getMinY());
        assertEquals(-3, cache.getMaxY());
        assertEquals(0, cache.getOcclusionData().length);
        assertFalse(cache.isVoxelOccluded(0, 0, 0));
    }

    @Test
    void blockBelowMinifiedRangeRecomputesWithoutArrayCopyFailure() throws InterruptedException {
        TestWorld world = new TestWorld(-2, 3);
        world.chunk.setOpaque(0, 1, 0, 0, true);
        OcclusionChunkCache cache = world.getOcclusionWorldCache().chunk(0, 0);
        await(() -> cache.isFullyComputed() && cache.getMinY() == 1 && cache.getHeight() == 1);

        world.chunk.setOpaque(0, 0, 0, 0, true);
        assertDoesNotThrow(() -> cache.recalculateBlock(0, 0, 0));
        await(() -> cache.isFullyComputed() && cache.getMinY() == 0 && cache.getHeight() == 2);

        assertTrue(cache.isVoxelOccluded(0, 0, 0));
        assertTrue(cache.isVoxelOccluded(0, 2, 0));
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                fail("Timed out waiting for occlusion cache computation");
            }
            Thread.sleep(10L);
        }
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

        private final TestChunk chunk = new TestChunk();
        private final int minY;
        private final int maxY;

        private TestWorld(int minY, int maxY) {
            super(testPlatform());
            this.minY = minY;
            this.maxY = maxY;
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
            return this.minY;
        }

        @Override
        public int getMaxY() {
            return this.maxY;
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

    private static final class TestChunk implements PlatformChunkAccess {

        private final Set<String> opaque = ConcurrentHashMap.newKeySet();

        private void setOpaque(int x, int y, int z, int voxelIndex, boolean value) {
            String key = x + ":" + y + ":" + z + ":" + voxelIndex;
            if (value) {
                this.opaque.add(key);
            } else {
                this.opaque.remove(key);
            }
        }

        @Override
        public int getBlockId(int x, int y, int z) {
            return 0;
        }

        @Override
        public boolean isOpaque(int x, int y, int z, int voxelIndex) {
            return this.opaque.contains(x + ":" + y + ":" + z + ":" + voxelIndex);
        }
    }
}
