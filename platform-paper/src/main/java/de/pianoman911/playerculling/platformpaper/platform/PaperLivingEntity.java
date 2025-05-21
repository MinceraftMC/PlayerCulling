package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformLivingEntity;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Set;

public class PaperLivingEntity<T extends LivingEntity> extends PaperEntity<T> implements PlatformLivingEntity {

    public PaperLivingEntity(PaperPlatform platform, T sender) {
        super(platform, sender);
    }

    @Override
    public Vec3i getTargetBlock(int maxDistance) {
        Block targetBlock = this.getDelegate().getTargetBlock(Set.of(), maxDistance);
        return new Vec3i(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
    }
}
