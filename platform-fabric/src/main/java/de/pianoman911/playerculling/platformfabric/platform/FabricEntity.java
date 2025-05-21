package de.pianoman911.playerculling.platformfabric.platform;

import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.TickRefreshSupplier;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

// Can't build on top CommandSource
@NullMarked
public class FabricEntity<T extends Entity> implements PlatformEntity {

    protected final FabricPlatform platform;
    protected final TickRefreshSupplier<Vec3d> position;
    protected final TickRefreshSupplier<AABB> aabb;
    protected T entity;

    public FabricEntity(FabricPlatform platform, T sender) {
        this.platform = platform;
        this.entity = sender;
        this.position = new TickRefreshSupplier<>(platform, () ->
                new Vec3d(sender.getX(), sender.getY(), sender.getZ()));
        this.aabb = new TickRefreshSupplier<>(platform, () -> {
            net.minecraft.world.phys.AABB boundingBox = sender.getBoundingBox();
            return new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        });
    }

    public final T getDelegate() {
        return this.entity;
    }

    @Override
    public IPlatform getPlatform() {
        return this.platform;
    }

    @Override
    public void sendMessage(Component message) {
        // Don't support
    }

    @Override
    public boolean hasPermission(String permission, TriState defaultValue) {
        if (defaultValue == TriState.NOT_SET) {
            return Permissions.check(this.entity, permission, 2);
        } else {
            return Permissions.check(this.entity, permission, Boolean.TRUE.equals(defaultValue.toBoolean()));
        }
    }

    @Override
    public UUID getUniqueId() {
        return this.getDelegate().getUUID();
    }

    @Override
    public String getName() {
        return this.getDelegate().getScoreboardName();
    }

    @Override
    public PlatformWorld getWorld() {
        return this.platform.provideWorld(this.getDelegate().level());
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
        this.getDelegate().teleportTo(((FabricWorld) world).getWorld(), x, y, z, Set.of(),
                this.getDelegate().getYRot(), this.getDelegate().getXRot(), true);
    }
}
