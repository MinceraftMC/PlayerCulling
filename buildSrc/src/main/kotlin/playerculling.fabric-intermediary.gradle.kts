// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    id("playerculling.version-ext")
    net.fabricmc.`fabric-loom-remap`
    id("playerculling.fabric-common")
}

val playercullingExt = project.extensions.getByType<PlayerCullingVersionExt>()
dependencies {
    playercullingExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${playercullingExt.versionName.get()}")

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        modImplementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    // skip depending on parchment, just way too much
    // of a headache using this plugin-based setup
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
}
