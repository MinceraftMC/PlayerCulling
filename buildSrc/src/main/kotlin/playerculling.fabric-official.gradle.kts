// Created by booky10 in BetterView (6:21 PM 04.04.2026)

import de.pianoman911.playerculling.gradle.PlayerCullingVersionExt

plugins {
    id("playerculling.version-ext")
    net.fabricmc.`fabric-loom`
    id("playerculling.fabric-common")
}

val playercullingExt = project.extensions.getByType<PlayerCullingVersionExt>()

dependencies {
    playercullingExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${playercullingExt.versionName.get()}")

        // fabric api
        val depVersion = playercullingExt.dependencyVersion().get()
        val fabricApiVersion = libs.versions.hackGetVersion("fabricapi.$depVersion")
        implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

        // permissions api
        val permissionsVersion = libs.versions.hackGetVersion("fabric.permissions.$depVersion")
        "me.lucko:fabric-permissions-api:$permissionsVersion".apply {
            implementation(this)
            include(this)
        }

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        implementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    implementation(libs.fabric.loader)
}
