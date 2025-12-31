package de.pianoman911.playerculling.core.culling;


import de.pianoman911.playerculling.core.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.core.util.CameraMode;
import de.pianoman911.playerculling.core.util.ClientsideUtil;
import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.PlayerCullingConstants;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformLivingEntity;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.atomics.AtomicCooldownRunnable;
import de.pianoman911.playerculling.platformcommon.util.atomics.AtomicFastStack;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CullPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");
    private static final double MAX_ANGLE = Math.toRadians(90D); // 90 degrees, impossible fov, but we can't detect the real fov

    // Minecraft related magic values
    private static final double BLINDNESS_DISTANCE_SQUARED = 6 * 6;
    private static final double DARKNESS_DISTANCE_SQUARED = 16 * 16;
    private static final long DARKNESS_MIN_MS = 2000;
    private static final long BLINDNESS_FADE_OUT_TICKS = 25;
    private static final long DARKNESS_FADE_OUT_TICKS = 30;
    final AtomicFastStack<PlatformEntity> tracked;
    private final CullShip ship;
    private final PlatformPlayer player;
    private final Vec3d viewerPosition = new Vec3d(0, 0, 0);
    private final Vec3d viewerDirection = new Vec3d(0, 0, 0);
    private final Vec3d inverseViewerDirection = new Vec3d(0, 0, 0);
    private final Vec3d viewerBack = new Vec3d(0, 0, 0);
    private final Vec3d viewerFront = new Vec3d(0, 0, 0);

    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    private final Set<UUID> toRemove = new HashSet<>(); // diff queue for hidden players

    private final Set<CullWorker> cullWorkers = ConcurrentHashMap.newKeySet();

    private final AtomicCooldownRunnable warnCooldown = new AtomicCooldownRunnable(PlayerCullingConstants.PANIC_LOG_INTERVAL);

    private boolean cullingEnabled = true;
    private boolean spectating = false;
    private long lastDarkness = -1;

    public CullPlayer(CullShip ship, PlatformPlayer player) {
        this.ship = ship;
        this.player = player;
        this.tracked = new AtomicFastStack<>(player.getWorld().getEntityCount());

        this.cullWorkers.add(new CullWorker(this));
    }

    /**
     * @param dir normalized direction vector
     */
    public static boolean isInnerAngle(Vec3d targetPos, Vec3d selfPos, Vec3d dir) {
        return isInnerAngle(
                targetPos.x, targetPos.y, targetPos.z,
                selfPos.x, selfPos.y, selfPos.z,
                dir.x, dir.y, dir.z
        );
    }

    /**
     * @param dirX normalized direction vector coord x
     * @param dirY normalized direction vector coord y
     * @param dirZ normalized direction vector coord z
     */
    public static boolean isInnerAngle(
            double targetX, double targetY, double targetZ,
            double selfX, double selfY, double selfZ,
            double dirX, double dirY, double dirZ
    ) {
        return angle(
                targetX, targetY, targetZ,
                selfX, selfY, selfZ,
                dirX, dirY, dirZ
        ) <= MAX_ANGLE;
    }

    /**
     * @param dir normalized direction vector
     */
    public static double angle(Vec3d targetPos, Vec3d selfPos, Vec3d dir) {
        return angle(
                targetPos.x, targetPos.y, targetPos.z,
                selfPos.x, selfPos.y, selfPos.z,
                dir.x, dir.y, dir.z
        );
    }

    /**
     * @param dirX normalized direction vector coord x
     * @param dirY normalized direction vector coord y
     * @param dirZ normalized direction vector coord z
     */
    public static double angle(
            double targetX, double targetY, double targetZ,
            double selfX, double selfY, double selfZ,
            double dirX, double dirY, double dirZ
    ) {
        double targetVecX = targetX - selfX;
        double targetVecY = targetY - selfY;
        double targetVecZ = targetZ - selfZ;
        double targetVecDotDir = targetVecX * dirX + targetVecY * dirY + targetVecZ * dirZ;
        double targetVecLengthSqrt = targetVecX * targetVecX + targetVecY * targetVecY + targetVecZ * targetVecZ;
        // skip multiplying length of targetVec by length of direction as we assume direction is normalized
        return Math.acos(targetVecDotDir / Math.sqrt(targetVecLengthSqrt));
    }

    public void prepareCull() {
        synchronized (this) {
            this.prepareCull0();
        }
    }

    public void finishCull() {
        synchronized (this.toRemove) {
            this.hidden.removeAll(this.toRemove);
            this.toRemove.clear();
        }
        synchronized (this) {
            for (CullWorker cullWorker : this.cullWorkers) {
                if (!cullWorker.hasAccurateTimings()) {
                    continue; // skip if we don't have enough data yet
                }
                long processingTime = cullWorker.getAverageProcessingTime();
                if (processingTime > this.ship.getConfig().getDelegate().scheduler.getForkThresholdNs()) {
                    if (this.cullWorkers.size() <= this.ship.getConfig().getDelegate().scheduler.maxThreads * 2) {
                        this.cullWorkers.add(new CullWorker(this));
                    }
                    this.warnCooldown.run(() -> LOGGER.warn("CullPlayer for player {} is taking too long to process ({} ns). " +
                            "Consider increasing the max threads setting.", this.player.getName(), processingTime));
                    cullWorker.resetTimings();
                    break; // only fork one worker per finishCull call
                } else if (processingTime < this.ship.getConfig().getDelegate().scheduler.getDestroyThresholdNs()) {
                    if (this.cullWorkers.size() > 1) {
                        this.cullWorkers.remove(cullWorker);
                    }
                }
            }
        }
    }

    private void prepareCull0() {
        if (!this.cullingEnabled
                || this.player.shouldPreventCulling()
                || this.player.isSpectator()
                || this.spectating
                || this.player.hasPermission("playerculling.bypass", false)
        ) {
            this.hidden.clear();
            return;
        }

        PlatformWorld world = this.player.getWorld();
        List<PlatformEntity> entitiesInWorld = world.getEntities();
        if (entitiesInWorld.size() <= 1) {
            return; // No need to cull if no other entities are in the world
        }
        for (CullWorker cullWorker : this.cullWorkers) {
            cullWorker.world(world);
        }
        Vec3d eye = this.player.getEyePosition();

        boolean blindness;
        boolean darkness;

        if (this.player.getBlindnessTicks() != -1) {
            blindness = this.player.getBlindnessTicks() > BLINDNESS_FADE_OUT_TICKS;
        } else {
            blindness = false;
        }

        if ((this.player.getDarknessTicks() != -1)) {
            if (this.lastDarkness == -1) {
                this.lastDarkness = System.currentTimeMillis();
            }
            darkness = (System.currentTimeMillis() - this.lastDarkness > DARKNESS_MIN_MS) && this.player.getDarknessTicks() > DARKNESS_FADE_OUT_TICKS;
        } else {
            this.lastDarkness = -1;
            darkness = false;
        }

        int trackingDist = this.player.getWorld().getTrackingDistance(this.player);
        if (trackingDist <= 0) { // No view distance set
            this.hidden.clear();
            return;
        }
        this.tracked.grow(entitiesInWorld.size()); // Ensure tracked stack capacity
        if (this.tracked.fastClear() >= 0) {
            LOGGER.warn("CullPlayer tracked stack for player {} was not empty during prepareCull!", this.player.getName());
        }

        double trackingDistSq = trackingDist * trackingDist;

        for (PlatformEntity worldEntity : entitiesInWorld) {
            if (worldEntity == this.player) {
                continue;
            }
            boolean nameTag = this.player.canSeeNameTag(worldEntity);

            double distSq = eye.distanceSquared(worldEntity.getPosition());

            if (worldEntity instanceof PlatformLivingEntity livingEntity) {
                if (livingEntity.isGlowing() || (livingEntity instanceof PlatformPlayer targetPlayer
                        && !targetPlayer.isSneaking() && nameTag)) {
                    this.unHideWithDirectPairing(worldEntity);
                }
            }
            if (nameTag) {
                this.unHideWithDirectPairing(worldEntity);
            } else if (
                    distSq < trackingDistSq && // In regular tracking distance
                            (!blindness || distSq < BLINDNESS_DISTANCE_SQUARED) && // In blindness distance
                            (!darkness || distSq < DARKNESS_DISTANCE_SQUARED) // In darkness distance
            ) { // cull
                this.tracked.push(worldEntity);
            } else { // not visible
                this.hidden.add(worldEntity.getUniqueId());
            }
        }
        if (this.tracked.isEmpty()) { // No players to cull
            return;
        }

        // Refresh positions
        this.viewerPosition.set(eye.getX(), eye.getY(), eye.getZ()).mul(2);
        this.viewerDirection.set(this.player.getDirection());
        this.inverseViewerDirection.copyFrom(this.viewerDirection).inverse();

        this.viewerBack.set(eye.getX(), eye.getY(), eye.getZ());
        ClientsideUtil.addPlayerViewOffset(this.viewerBack, this.player, CameraMode.THIRD_PERSON_BACK);
        this.viewerBack.mul(2);

        this.viewerFront.set(eye.getX(), eye.getY(), eye.getZ());
        ClientsideUtil.addPlayerViewOffset(this.viewerFront, this.player, CameraMode.THIRD_PERSON_FRONT);
        this.viewerFront.mul(2);
    }

    void cull(PlatformEntity target, OcclusionCullingInstance cullingInstance) {
        AABB trackedBox = target.getScaledBoundingBox();

        // Check if the player is in the view frustum, if, so we can cull it
        boolean mainInner = isInnerAngle(
                trackedBox.getCenterX(), trackedBox.getCenterY(), trackedBox.getCenterZ(),
                this.viewerPosition.x, this.viewerPosition.y, this.viewerPosition.z,
                this.viewerDirection.x, this.viewerDirection.y, this.viewerDirection.z
        );

        // First person view
        boolean canSee = mainInner && cullingInstance.isAABBVisible(
                trackedBox.getMinX(), trackedBox.getMinY(), trackedBox.getMinZ(),
                trackedBox.getMaxX(), trackedBox.getMaxY(), trackedBox.getMaxZ(),
                this.viewerPosition
        );
        if (!canSee) {
            boolean secondaryInner = isInnerAngle(
                    trackedBox.getCenterX(), trackedBox.getCenterY(), trackedBox.getCenterZ(),
                    this.viewerBack.x, this.viewerBack.y, this.viewerBack.z,
                    this.viewerDirection.x, this.viewerDirection.y, this.viewerDirection.z
            );

            // Third person view from back
            canSee = secondaryInner && cullingInstance.isAABBVisible(
                    trackedBox.getMinX(), trackedBox.getMinY(), trackedBox.getMinZ(),
                    trackedBox.getMaxX(), trackedBox.getMaxY(), trackedBox.getMaxZ(),
                    this.viewerBack
            );
            if (!canSee) {
                boolean tertiaryInner = isInnerAngle(
                        trackedBox.getCenterX(), trackedBox.getCenterY(), trackedBox.getCenterZ(),
                        this.viewerFront.x, this.viewerFront.y, this.viewerFront.z,
                        this.inverseViewerDirection.x, this.inverseViewerDirection.y, this.inverseViewerDirection.z);

                // Third person view from front
                canSee = tertiaryInner && cullingInstance.isAABBVisible(
                        trackedBox.getMinX(), trackedBox.getMinY(), trackedBox.getMinZ(),
                        trackedBox.getMaxX(), trackedBox.getMaxY(), trackedBox.getMaxZ(),
                        this.viewerFront
                );
            }
        }

        if (canSee) {
            this.unHideWithDirectPairing(target);
        } else {
            this.hidden.add(target.getUniqueId());
        }
    }

    private void unHideWithDirectPairing(PlatformEntity target) {
        if (this.hidden.remove(target.getUniqueId())) {
            this.player.addDirectPairing(target);
        }
    }

    public PlatformPlayer getPlatformPlayer() {
        return this.player;
    }

    public boolean isCullingEnabled() {
        return this.cullingEnabled;
    }

    public void setCullingEnabled(boolean cullingEnabled) {
        this.cullingEnabled = cullingEnabled;
        this.resetHidden();
    }

    public Collection<CullWorker> getCullWorker() {
        synchronized (this.cullWorkers) {
            return this.cullWorkers;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // this is a getter
    public boolean isHidden(UUID playerId) {
        return this.hidden.contains(playerId);
    }

    public int getHiddenCount() {
        return this.hidden.size();
    }

    @Unmodifiable
    public Set<UUID> getHidden() {
        return Set.copyOf(this.hidden);
    }

    public void resetHidden() {
        this.hidden.clear();
    }

    public void invalidateOther(UUID entityId) {
        // don't remove immediately, wait for async thread to finish processing
        // and then remove it to prevent the player from being added back again
        synchronized (this.toRemove) {
            this.toRemove.add(entityId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CullPlayer other)) {
            return false;
        }
        return Objects.equals(this.player, other.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player);
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }
}
