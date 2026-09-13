package de.pianoman911.playerculling.meme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@NullMarked
public class MemeDownloader {

    private static final Logger LOGGER = MemeLogger.getLogger("Downloader");
    private static final String PISTON_META = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final MemeInstance meme;

    private @MonotonicNonNull Path path;
    private @MonotonicNonNull Path libPath;
    private @MonotonicNonNull JsonObject meta;

    public MemeDownloader(MemeInstance meme) {
        this.meme = meme;
    }

    public void beginDownloads() {
        LOGGER.info("Starting downloads for version {}", this.meme.getVersion().asVeryShortPrettyString(true));
        this.fetchMeta();
        this.downloadClientJar();
        this.downloadLibraries();
        LOGGER.info("Finished downloads for version {}", this.meme.getVersion().asVeryShortPrettyString(true));
    }

    private void downloadClientJar() {
        String version = this.meme.getVersion().asVeryShortPrettyString(true);
        LOGGER.info("Downloading client jar for version {}", version);
        try {
            Path targetPath = this.meme.getDataPath().resolve("client_jars").resolve(version + ".jar");
            if (Files.exists(targetPath)) {
                this.path = targetPath;
                LOGGER.info("Client jar for version {} already exists at {}", version, this.path);
                return;
            }
            Files.createDirectories(targetPath.getParent());
            JsonObject downloads = this.meta.getAsJsonObject("downloads");
            if (!downloads.has("client")) {
                throw new RuntimeException("Version data does not contain 'client' download");
            }
            String clientUrl = downloads.getAsJsonObject("client").get("url").getAsString();
            URI clientUri = new URI(clientUrl);

            HttpResponse<Path> response = this.httpClient.send(HttpRequest.newBuilder().GET().uri(clientUri).build(),
                    HttpResponse.BodyHandlers.ofFile(targetPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to download client jar: " + response.statusCode() + " " + response.body());
            }
            this.path = response.body();
            LOGGER.info("Downloaded client jar for version {} to {}", version, this.path);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to download client jar", exception);
        }
    }

    private void downloadLibraries() {
        String version = this.meme.getVersion().asVeryShortPrettyString(true);
        this.libPath = this.meme.getDataPath().resolve("client_jars").resolve(version);

        LOGGER.info("Downloading libraries for version {}", version);

        for (JsonElement entry : this.meta.getAsJsonArray("libraries")) {
            JsonObject libraryMeta = entry.getAsJsonObject();
            if (libraryMeta.has("name")) {
                this.downloadLib(libraryMeta);
            }
        }
    }

    private void downloadLib(JsonObject libMeta) {
        try {
            String name = libMeta.get("name").getAsString();
            if (!libMeta.has("downloads")) {
                throw new IllegalStateException("Library " + name + " has no downloads defined!");
            }
            JsonObject downloads = libMeta.getAsJsonObject("downloads");
            if (!downloads.has("artifact")) {
                throw new IllegalStateException("Library " + name + " has no artifacts defined!");
            }
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (!artifact.has("url") || !artifact.has("path")) {
                throw new IllegalStateException("Library " + name + " has no url or path defined!");
            }
            URI downloadUri = new URI(artifact.get("url").getAsString());

            String path = artifact.get("path").getAsString();

            Path targetPath = this.libPath.resolve(path);
            if (Files.exists(targetPath)) {
                LOGGER.debug("Library {} already exists at {}", name, targetPath);
                return;
            }
            Files.createDirectories(targetPath.getParent());

            this.httpClient.send(HttpRequest.newBuilder().GET().uri(downloadUri).build(),
                    HttpResponse.BodyHandlers.ofFile(targetPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE));

            LOGGER.debug("Downloaded library {} to {}", name, targetPath);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void fetchMeta() {
        LOGGER.info("Fetching version meta for version {}", this.meme.getVersion().asVeryShortPrettyString(true));
        try {
            String version = this.meme.getVersion().asVeryShortPrettyString(true);
            HttpResponse<String> response = this.httpClient.send(HttpRequest.newBuilder().GET().uri(new URI(PISTON_META)).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch version manifest: " + response.statusCode() + " " + response.body());
            }
            JsonObject meta = MemeInstance.GSON.fromJson(response.body(), JsonObject.class);
            if (!meta.has("versions")) {
                throw new RuntimeException("Version manifest does not contain 'versions' field");
            }

            for (JsonElement element : meta.getAsJsonArray("versions")) {
                JsonObject versionObject = element.getAsJsonObject();
                if (versionObject.has("id") && versionObject.get("id").getAsString().equals(version)) {
                    String url = versionObject.get("url").getAsString();
                    HttpResponse<String> versionResponse = this.httpClient.send(HttpRequest.newBuilder().GET().uri(new URI(url)).build(), HttpResponse.BodyHandlers.ofString());
                    if (versionResponse.statusCode() != 200) {
                        throw new RuntimeException("Failed to fetch version data: " + versionResponse.statusCode() + " " + versionResponse.body());
                    }
                    this.meta = MemeInstance.GSON.fromJson(versionResponse.body(), JsonObject.class);
                    LOGGER.info("Fetched version meta for version {}", version);
                    return;
                }
            }
            throw new RuntimeException("Failed to fetch version data: version " + version + " not found!");
        } catch (Exception exception) {
            throw new RuntimeException("Failed to fetch version meta", exception);
        }
    }

    public boolean isDownloaded() {
        return this.path != null && Files.exists(this.path);
    }

    @Nullable
    public Path getDownloadedPath() {
        return this.path;
    }

    @Nullable
    public Path getLibPath() {
        return this.libPath;
    }
}
