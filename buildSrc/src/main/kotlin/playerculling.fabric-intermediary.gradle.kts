// Created by booky10 in BetterView (6:21 PM 04.04.2026)

import de.pianoman911.playerculling.gradle.PlayerCullingVersionExt

plugins {
    id("playerculling.version-ext")
    net.fabricmc.`fabric-loom-remap`
    id("playerculling.fabric-common")
}

val playercullingExt = project.extensions.getByType<PlayerCullingVersionExt>()
dependencies {
    playercullingExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${playercullingExt.versionName.get()}")

        // fabric api
        val depVersion = playercullingExt.dependencyVersion().get()
        val fabricApiVersion = libs.versions.hackGetVersion("fabricapi.$depVersion")
        modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

        // permissions api
        val permissionsVersion = libs.versions.hackGetVersion("fabric.permissions.$depVersion")
        "me.lucko:fabric-permissions-api:$permissionsVersion".apply {
            modImplementation(this)
            include(this)
        }

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        modImplementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    // skip depending on parchment, just way too much
    // of a headache using this plugin-based setup
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
}
