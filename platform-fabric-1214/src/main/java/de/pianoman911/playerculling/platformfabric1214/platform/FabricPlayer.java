package de.pianoman911.playerculling.platformfabric1214.platform;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FabricPlayer extends FabricLivingEntity<ServerPlayer> implements PlatformPlayer {

    public FabricPlayer(FabricPlatform platform, ServerPlayer player) {
        super(platform, player);
    }

    public void replacePlayer(ServerPlayer player) {
        this.entity = player;
    }

    @Override
    public void sendMessage(Component message) {
        this.getDelegate().sendMessage(message);
    }

    @Override
    public boolean shouldPreventCulling() {
        return !this.getDelegate().isAlive();
    }

    @Override
    public boolean isSpectator() {
        return this.getDelegate().isSpectator();
    }

    @Override
    public int getTrackingDistance() {
        return this.getDelegate().requestedViewDistance();
    }

    @Override
    public Vec3d getDirection() {
        Vec3 dir = this.getDelegate().getForward();
        return new Vec3d(dir.x(), dir.y(), dir.z());
    }

    @Override
    public Vec3d getEyePosition() {
        Vec3 loc = this.getDelegate().getEyePosition();
        return new Vec3d(loc.x(), loc.y(), loc.z());
    }

    @Override
    public double getYaw() {
        return this.getDelegate().getYRot();
    }

    @Override
    public double getPitch() {
        return this.getDelegate().getXRot();
    }

    @Override
    public long getBlindnessTicks() {
        MobEffectInstance effect = this.getDelegate().getEffect(MobEffects.BLINDNESS);
        if (effect != null) {
            return effect.getDuration();
        }
        return -1;
    }

    @Override
    public boolean isGlowing() {
        return this.getDelegate().hasEffect(MobEffects.GLOWING);
    }

    @Override
    public long getDarknessTicks() {
        MobEffectInstance effect = this.getDelegate().getEffect(MobEffects.DARKNESS);
        if (effect != null) {
            return effect.getDuration();
        }
        return -1;
    }

    @Override
    public boolean canSeeNameTag(PlatformPlayer targetPlayer) {
        PlayerTeam targetTeam = this.getDelegate().level().getScoreboard().getPlayersTeam(targetPlayer.getName());
        if (targetTeam == null) {
            return true; // Always visible
        }
        return switch (targetTeam.getNameTagVisibility()) {
            case ALWAYS -> true;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> targetTeam.getPlayers().contains(this.getName());
            case HIDE_FOR_OWN_TEAM -> !targetTeam.getPlayers().contains(this.getName());
        };
    }

    @Override
    public boolean isSneaking() {
        return this.getDelegate().getLastClientInput().shift();
    }

    @Override
    public boolean isOnline() {
        return !this.getDelegate().hasDisconnected();
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
        // not supported in Fabric
    }

    public ServerPlayer getFabricPlayer() {
        return this.getDelegate();
    }
}
