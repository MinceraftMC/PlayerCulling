// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    `java-library`
}

val playercullingExt = project.extensions.create<PlayerCullingVersionExt>("playerculling")

// we need to load stuff before fabric does, so this plugin
// separates the extension properties from the actual config
project.afterEvaluate {
    playercullingExt.afterEvaluate.forEach { it.run() }
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = playercullingExt.languageVersion
    }
}
