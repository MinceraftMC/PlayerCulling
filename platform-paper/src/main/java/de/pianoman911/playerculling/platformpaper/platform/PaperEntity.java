package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.TickRefreshSupplier;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class PaperEntity<T extends Entity> extends PaperCommandSender<T> implements PlatformEntity {

    protected final TickRefreshSupplier<Vec3d> position;
    protected final TickRefreshSupplier<AABB> aabb;

    public PaperEntity(PaperPlatform platform, T sender) {
        super(platform, sender);
        this.position = new TickRefreshSupplier<>(platform, () ->
                new Vec3d(sender.getX(), sender.getY(), sender.getZ()));
        this.aabb = new TickRefreshSupplier<>(platform, () -> {
            BoundingBox boundingBox = sender.getBoundingBox();
            return new AABB(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                    boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        });
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
    public AABB getBoundingBox() {
        return this.aabb.get();
    }

    @Override
    public void teleport(PlatformWorld world, double x, double y, double z) {
        this.getDelegate().teleportAsync(new Location(((PaperWorld) world).getWorld(), x, y, z));
    }
}
