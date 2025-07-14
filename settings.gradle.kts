enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "PlayerCulling"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

include("core")
include("api")
include("platform-common")
include("platform-paper")
include("plugin-paper")
include("mod-fabric")

listOf("1.21.1", "1.21.4", "1.21.6")
    .map { it.replace(".", "") }
    .forEach { include("platform-paper-nms-$it") }

listOf("1.21.4", "1.21.6")
    .map { it.replace(".", "") }
    .forEach { include("platform-folia-nms-$it") }

listOf("1.21.4", "1.21.5")
    .map { it.replace(".", "") }
    .forEach { include("platform-fabric-$it") }

