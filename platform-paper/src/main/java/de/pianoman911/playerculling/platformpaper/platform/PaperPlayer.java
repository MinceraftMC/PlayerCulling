package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PaperPlayer extends PaperLivingEntity<Player> implements PlatformPlayer {

    private final PaperPlatform platform;

    public PaperPlayer(PaperPlatform platform, Player player) {
        super(platform, player);
        this.platform = platform;
    }

    @Override
    public boolean shouldPreventCulling() {
        return this.getDelegate().isDead();
    }

    @Override
    public boolean isSpectator() {
        // use nms for checking spectator mode, faster than using bukkit gamemode comparison
        return this.platform.getNmsAdapter().isSpectator(this.sender);
    }

    @Override
    public int getTrackingDistance() {
        return this.getDelegate().getClientViewDistance() << 4; // Chunks distance to blocks distance
    }

    @Override
    public Vec3d getDirection() {
        Vector dir = this.getDelegate().getLocation().getDirection();
        return new Vec3d(dir.getX(), dir.getY(), dir.getZ());
    }

    @Override
    public Vec3d getEyePosition() {
        Location loc = this.getDelegate().getEyeLocation();
        return new Vec3d(loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public double getYaw() {
        return this.getDelegate().getLocation().getYaw();
    }

    @Override
    public double getPitch() {
        return this.getDelegate().getLocation().getPitch();
    }

    @Override
    public long getBlindnessTicks() {
        PotionEffect blindnessEffect = this.getDelegate().getPotionEffect(PotionEffectType.BLINDNESS);
        if (blindnessEffect != null) {
            return blindnessEffect.getDuration();
        }
        return -1;
    }

    @Override
    public boolean isGlowing() {
        return this.getDelegate().isGlowing();
    }

    @Override
    public long getDarknessTicks() {
        PotionEffect darknessEffect = this.getDelegate().getPotionEffect(PotionEffectType.DARKNESS);
        if (darknessEffect != null) {
            return darknessEffect.getDuration();
        }
        return -1;
    }

    @Override
    public boolean canSeeNameTag(PlatformPlayer targetPlayer) {
        Player target = ((PaperPlayer) targetPlayer).getDelegate();
        return this.platform.getNmsAdapter().canSeeNametag(this.sender, target);
    }

    @Override
    public boolean isSneaking() {
        return this.getDelegate().isSneaking();
    }

    @Override
    public boolean isOnline() {
        return this.getDelegate().isConnected();
    }

    @Override
    public void showBossBar(BossBar bossBar) {
        this.getDelegate().showBossBar(bossBar);
    }

    @Override
    public void hideBossBar(BossBar bossBar) {
        this.getDelegate().hideBossBar(bossBar);
    }

    @Override
    public void addDirectPairing(PlatformPlayer... players) {
        this.platform.getNmsAdapter().addPairing(this, players);
    }
}
