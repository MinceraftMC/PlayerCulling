package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.PlayerCullingConstants;
import de.pianoman911.playerculling.platformcommon.util.TickRefreshSupplier;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class PaperEntity<T extends Entity> extends PaperCommandSender<T> implements PlatformEntity {

    protected final TickRefreshSupplier<Vec3d> position;
    protected final TickRefreshSupplier<AABB> aabb;

    public PaperEntity(PaperPlatform platform, T sender) {
        super(platform, sender);
        this.position = new TickRefreshSupplier<>(platform, pos -> {
            platform.getNmsAdapter().getPosition(this.getDelegate(), pos); // Copy direct without allocation
            return pos;
        }, new Vec3d(0, 0, 0));
        this.aabb = new TickRefreshSupplier<>(platform, box -> {
            platform.getNmsAdapter().getBoundingBox(this.getDelegate(), box); // Copy direct without allocation
            return box.scale(PlayerCullingConstants.VOXEL_SCALE);
        }, new AABB(0, 0, 0, 0, 0, 0));
    }

    @Override
    public UUID getUniqueId() {
        return this.getDelegate().getUniqueId();
    }

    @Override
    public String getName() {
        return this.getDelegate().getName();
    }

    @Override
    public PlatformWorld getWorld() {
        return this.platform.provideWorld(this.getDelegate().getWorld());
    }

    @Override
    public Vec3d getPosition() {
        return this.position.get();
    }

    @Override
    public AABB getScaledBoundingBox() {
        return this.aabb.get();
    }

    @Override
    public void teleport(PlatformWorld world, double x, double y, double z) {
        this.getDelegate().teleportAsync(new Location(((PaperWorld) world).getWorld(), x, y, z));
    }
}
