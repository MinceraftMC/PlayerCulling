import de.pianoman911.playerculling.gradle.PlayerCullingVersionExt

plugins {
    id("playerculling.fabric-official")
}

configure<PlayerCullingVersionExt> {
    versionName = "26.1"
    languageVersion = JavaLanguageVersion.of(25)
}
