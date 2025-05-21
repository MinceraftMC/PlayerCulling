package de.pianoman911.playerculling.platformcommon.platform.entity;

import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import net.kyori.adventure.bossbar.BossBar;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PlatformPlayer extends PlatformLivingEntity {

    /**
     * Can block culling for this player. For example, if the player is dead
     */
    boolean shouldPreventCulling();

    boolean isSpectator();

    int getTrackingDistance();

    Vec3d getDirection();

    Vec3d getEyePosition();

    double getYaw();

    double getPitch();

    /**
     * @return the blindness ticks, or -1 if the player is not blind
     */
    long getBlindnessTicks();

    boolean isGlowing();

    /**
     * @return the darkness ticks, or -1 if the player is not dark
     */
    long getDarknessTicks();

    boolean canSeeNameTag(PlatformPlayer targetPlayer);

    boolean isSneaking();

    boolean isOnline();

    void showBossBar(BossBar bossBar);

    void hideBossBar(BossBar bossBar);

    void addDirectPairing(PlatformPlayer... players);
}
