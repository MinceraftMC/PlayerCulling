package de.pianoman911.playerculling.meme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@NullMarked
public class MemeDownloader {

    private static final String PISTON_META = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final MemeInstance meme;

    private @Nullable Path path;

    public MemeDownloader(MemeInstance meme) {
        this.meme = meme;
    }

    public void downloadClientJar() {
        String version = this.meme.getVersion().asVeryShortPrettyString(true);
        URI clientUri = extractClientURI(version);
        try {
            Path targetPath = this.meme.getDataPath().resolve("client_jars").resolve(version + ".jar");
            if (Files.exists(targetPath)) {
                this.path = targetPath;
                return;
            }
            Files.createDirectories(targetPath.getParent());
            HttpResponse<Path> response = this.httpClient.send(HttpRequest.newBuilder().GET().uri(clientUri).build(),
                    HttpResponse.BodyHandlers.ofFile(targetPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to download client jar: " + response.statusCode() + " " + response.body());
            }
            this.path = response.body();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private URI extractClientURI(String version) {
        try {
            HttpResponse<String> response = this.httpClient.send(HttpRequest.newBuilder().GET().uri(new URI(PISTON_META)).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch version manifest: " + response.statusCode() + " " + response.body());
            }
            JsonObject meta = MemeInstance.GSON.fromJson(response.body(), JsonObject.class);
            if (!meta.has("versions")) {
                throw new RuntimeException("Version manifest does not contain 'versions' field");
            }
            boolean found = false;
            for (JsonElement element : meta.getAsJsonArray("versions")) {
                JsonObject versionObject = element.getAsJsonObject();
                if (versionObject.has("id") && versionObject.get("id").getAsString().equals(version)) {
                    found = true;
                    String url = versionObject.get("url").getAsString();
                    HttpResponse<String> versionResponse = this.httpClient.send(HttpRequest.newBuilder().GET().uri(new URI(url)).build(), HttpResponse.BodyHandlers.ofString());
                    if (versionResponse.statusCode() != 200) {
                        throw new RuntimeException("Failed to fetch version data: " + versionResponse.statusCode() + " " + versionResponse.body());
                    }
                    JsonObject versionData = MemeInstance.GSON.fromJson(versionResponse.body(), JsonObject.class);
                    if (!versionData.has("downloads")) {
                        throw new RuntimeException("Version data does not contain 'downloads' field");
                    }
                    JsonObject downloads = versionData.getAsJsonObject("downloads");
                    if (!downloads.has("client")) {
                        throw new RuntimeException("Version data does not contain 'client' download");
                    }
                    String clientUrl = downloads.getAsJsonObject("client").get("url").getAsString();
                    return new URI(clientUrl);
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        throw new RuntimeException("Version not found: " + version);
    }

    public boolean isDownloaded() {
        return this.path != null && Files.exists(this.path);
    }

    @Nullable
    public Path getDownloadedPath() {
        return this.path;
    }
}
