// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    id("playerculling.version-ext")
    net.fabricmc.`fabric-loom`
    id("playerculling.fabric-common")
}

val playercullingExt = project.extensions.getByType<PlayerCullingVersionExt>()

dependencies {
    playercullingExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${playercullingExt.versionName.get()}")

        // depend on moonrise for chunk loading stuff
        val depVersion = playercullingExt.dependencyVersion().get()
        val moonriseVersion = libs.versions.hackGetVersion("moonrise.$depVersion")
        api("maven.modrinth:moonrise-opt:$moonriseVersion")

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        implementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    implementation(libs.fabric.loader)
}
