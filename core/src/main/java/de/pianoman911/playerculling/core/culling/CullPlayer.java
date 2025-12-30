package de.pianoman911.playerculling.core.culling;


import de.pianoman911.playerculling.core.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.core.util.CameraMode;
import de.pianoman911.playerculling.core.util.ClientsideUtil;
import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformLivingEntity;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.AtomicFastStack;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CullPlayer {

    private static final double MAX_ANGLE = Math.toRadians(90D); // 90 degrees, impossible fov, but we can't detect the real fov
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");

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

    private final List<CullPackage> cullPackages = new ArrayList<>();

    private boolean cullingEnabled = true;
    private boolean spectating = false;
    private long lastDarkness = -1;
    private long lastWarn = 0;

    public CullPlayer(CullShip ship, PlatformPlayer player) {
        this.ship = ship;
        this.player = player;
        this.tracked = new AtomicFastStack<>(player.getWorld().getPlayerCount());

        this.cullPackages.add(new CullPackage(this));
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
            for (CullPackage cullPackage : this.cullPackages) {
                long processingTime = cullPackage.getAverageProcessingTime();
                if (processingTime > this.ship.getConfig().getDelegate().scheduler.getMaxCullTimeNs()) {
                    if (this.cullPackages.size() * 2 <= this.ship.getConfig().getDelegate().scheduler.maxThreads) {
                        this.cullPackages.add(new CullPackage(this));
                    } else if (System.currentTimeMillis() - this.lastWarn > 10_000L) {
                        LOGGER.warn("CullPlayer for player {} is taking too long to process ({} ns). Consider increasing the max threads setting.", this.player.getName(), processingTime);
                        this.lastWarn = System.currentTimeMillis();
                    }
                    break;
                }
                if (processingTime < this.ship.getConfig().getDelegate().scheduler.getMaxMergeNs()) {
                    if (this.cullPackages.size() > 1) {
                        this.cullPackages.remove(cullPackage);
                    }
                    break;
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
            return; // No need to cull if no other players are in the world
        }
        for (CullPackage cullPackage : this.cullPackages) {
            cullPackage.world(world);
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
        AABB trackedBox = target.getBoundingBox();

        // For 2x2x2 Shapes
        double aabbMinX = trackedBox.minX() * 2d;
        double aabbMinY = trackedBox.minY() * 2d;
        double aabbMinZ = trackedBox.minZ() * 2d;
        double aabbMaxX = trackedBox.maxX() * 2d;
        double aabbMaxY = trackedBox.maxY() * 2d;
        double aabbMaxZ = trackedBox.maxZ() * 2d;
        double aabbCenterX = aabbMinX + (aabbMaxX - aabbMinX) / 2;
        double aabbCenterY = aabbMinY + (aabbMaxY - aabbMinY) / 2;
        double aabbCenterZ = aabbMinZ + (aabbMaxZ - aabbMinZ) / 2;

        // Check if the player is in the view frustum, if, so we can cull it
        boolean mainInner = isInnerAngle(
                aabbCenterX, aabbCenterY, aabbCenterZ,
                this.viewerPosition.x, this.viewerPosition.y, this.viewerPosition.z,
                this.viewerDirection.x, this.viewerDirection.y, this.viewerDirection.z
        );

        // First person view
        boolean canSee = mainInner && cullingInstance.isAABBVisible(
                aabbMinX, aabbMinY, aabbMinZ, aabbMaxX, aabbMaxY, aabbMaxZ, this.viewerPosition);
        if (!canSee) {
            boolean secondaryInner = isInnerAngle(
                    aabbCenterX, aabbCenterY, aabbCenterZ,
                    this.viewerBack.x, this.viewerBack.y, this.viewerBack.z,
                    this.viewerDirection.x, this.viewerDirection.y, this.viewerDirection.z
            );

            // Third person view from back
            canSee = secondaryInner && cullingInstance.isAABBVisible(
                    aabbMinX, aabbMinY, aabbMinZ,
                    aabbMaxX, aabbMaxY, aabbMaxZ,
                    this.viewerBack
            );
            if (!canSee) {
                boolean tertiaryInner = isInnerAngle(
                        aabbCenterX, aabbCenterY, aabbCenterZ,
                        this.viewerFront.x, this.viewerFront.y, this.viewerFront.z,
                        this.inverseViewerDirection.x, this.inverseViewerDirection.y, this.inverseViewerDirection.z);

                // Third person view from front
                canSee = tertiaryInner && cullingInstance.isAABBVisible(
                        aabbMinX, aabbMinY, aabbMinZ,
                        aabbMaxX, aabbMaxY, aabbMaxZ,
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

    public List<CullPackage> getCullPackages() {
        synchronized (this.cullPackages) {
            return this.cullPackages;
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

    public void invalidateOther(UUID playerId) {
        // don't remove immediately, wait for async thread to finish processing
        // and then remove it to prevent the player from being added back again
        synchronized (this.toRemove) {
            this.toRemove.add(playerId);
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
