
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import de.pianoman911.playerculling.gradle.PlayerCullingVersionExt

// Created by booky10 in BetterView (10:09 PM 05.04.2026)

plugins {
    io.papermc.paperweight.userdev
    id("playerculling.version-ext")
}

val playercullingExt = project.extensions.getByType<PlayerCullingVersionExt>()

dependencies {
    implementation(project(":paper-common"))
    paperweight.paperDevBundle(
        playercullingExt.dependencyVersion()
            .map { libs.versions.hackGetVersion("paper.$it") })
}

configure<PaperweightUserExtension> {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

// hack to allow depending on this platform in java 21 projects
configurations.runtimeElements.configure {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}
