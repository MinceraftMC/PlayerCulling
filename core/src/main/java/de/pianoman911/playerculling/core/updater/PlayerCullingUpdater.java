package de.pianoman911.playerculling.core.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@NullMarked
public final class PlayerCullingUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCullingUpdater");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final Component PREFIX = Component.text()
            .append(Component.text('[', NamedTextColor.GRAY))
            .append(Component.text("PlayerCulling", NamedTextColor.GOLD))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .build();
    private static final String GITHUB_REPO = "MinceraftMC/PlayerCulling";

    private final CullShip ship;
    private final Set<UUID> notifiedPlayers = new HashSet<>();
    private final Manifest manifest = loadManifest();
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean isDev = !"[build]".equals(this.manifest.getMainAttributes().getValue("Environment"));

    private UpdateState state = UpdateState.NOT_CHECKED;
    @Nullable
    private String downloadUrl = null;
    @Nullable
    private String updateInfo = null;

    public PlayerCullingUpdater(CullShip ship) {
        this.ship = ship;
    }

    public void enable() {
        if (this.isDev) {
            LOGGER.warn("Running in development environment! Updates will not be checked.");
            return;
        }
        this.ship.getConfig().addReloadHookAndRun(new Consumer<>() {

            private int taskId = -1;

            @Override
            public void accept(PlayerCullingConfig config) {
                ship.getPlatform().cancelTask(this.taskId);
                if (config.updater.enabled) {
                    this.taskId = ship.getPlatform().runTaskRepeatingAsync(PlayerCullingUpdater.this::checkForUpdate, 0L,
                            config.updater.getIntervalMs()
                    );
                }
            }
        });
    }

    private void checkForUpdate() {
        boolean notifyPlayers = true;

        try {
            LOGGER.info("Checking for updates...");
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + GITHUB_REPO + "/releases")).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warn("Failed to check for updates: Received {}", response.statusCode());
                this.state = UpdateState.UPDATE_REQUEST_FAILED;
                return;
            }

            JsonArray versions = GSON.fromJson(response.body(), JsonArray.class);
            if (versions.isEmpty()) {
                LOGGER.warn("Failed to check for updates: No versions found");
                this.state = UpdateState.UPDATE_REQUEST_FAILED;
                return;
            }

            JsonObject latestRelease = versions.get(0).getAsJsonObject();
            String latestTag = latestRelease.get("tag_name").getAsString();
            String currentTag = this.manifest.getMainAttributes().getValue("Git-Tag");

            if (currentTag.equals(latestTag)) {
                LOGGER.info("PlayerCulling version {} is up-to-date :)", currentTag);
                this.state = UpdateState.UP_TO_DATE;
                notifyPlayers = false;
            } else {
                String updateInfo = currentTag + " -> " + latestTag;
                if (Objects.equals(this.updateInfo, updateInfo)) {
                    notifyPlayers = false;
                }
                this.updateInfo = updateInfo;

                LOGGER.info("Update found: {}", updateInfo);
                this.downloadUrl = latestRelease.get("html_url").getAsString();
                this.state = UpdateState.AVAILABLE;
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Please report the following error to the developer:", throwable);
            this.state = UpdateState.UPDATE_REQUEST_FAILED;
        } finally {
            if (notifyPlayers) {
                this.notifiedPlayers.clear();
                this.notifyPlayers();
            }
        }
    }

    private void notifyPlayer(PlatformPlayer player) {
        if (!this.notifiedPlayers.add(player.getUniqueId())) {
            return;
        }

        Component message = switch (this.state) {
            case UPDATE_REQUEST_FAILED -> Component.text(
                    "Failed to check for updates, please check the log for more info", NamedTextColor.RED);
            case AVAILABLE -> {
                if (this.updateInfo == null || this.downloadUrl == null) {
                    yield Component.text(
                            "Update available, but no information available", NamedTextColor.RED);
                }
                yield Component.text()
                        .content("Update available ").color(NamedTextColor.YELLOW)
                        .append(Component.text('(', NamedTextColor.GRAY))
                        .append(Component.text(this.updateInfo, NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                                .hoverEvent(Component.text(this.downloadUrl, NamedTextColor.GRAY))
                                .clickEvent(ClickEvent.openUrl(this.downloadUrl)))
                        .append(Component.text(')', NamedTextColor.GRAY))
                        .build();
            }
            default -> null;
        };

        if (message != null) {
            player.sendMessage(PREFIX.append(message));
        }
    }

    private void notifyPlayers() {
        if (!this.ship.getConfig().getDelegate().updater.notifyAdmins) {
            return;
        }

        for (PlatformPlayer player : this.ship.getPlatform().getPlayers()) {
            if (player.hasPermission("playerculling.update-notify")) {
                this.notifyPlayer(player);
            }
        }
    }

    public void onJoin(PlatformPlayer player) {
        if (!this.ship.getConfig().getDelegate().updater.notifyAdmins) {
            return;
        }

        if (player.hasPermission("playerculling.update-notify")) {
            this.notifyPlayer(player);
        }
    }

    private Manifest loadManifest() {
        try (InputStream resource = PlayerCullingUpdater.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(resource);
            Attributes mainAttributes = manifest.getMainAttributes();
            LOGGER.info("Loaded manifest: {} {}, {}/{}({}) - Environment: {} - Licensed under {} on {}",
                    mainAttributes.getValue("Implementation-Title"),
                    mainAttributes.getValue("Implementation-Version"),
                    mainAttributes.getValue("Git-Commit"),
                    mainAttributes.getValue("Git-Branch"),
                    mainAttributes.getValue("Git-Tag"),
                    mainAttributes.getValue("Environment"),
                    mainAttributes.getValue("License"),
                    mainAttributes.getValue("Build-Date")
            );

            return manifest;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load manifest", exception);
        }
    }

    private enum UpdateState {

        NOT_CHECKED,
        UPDATE_REQUEST_FAILED,
        UP_TO_DATE,
        AVAILABLE,
    }
}
