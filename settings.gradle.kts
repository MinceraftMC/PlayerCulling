enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "PlayerCulling"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("core")
include("api")
include("platform-common")
include("platform-paper")
include("plugin-paper")
include("mod-fabric")

listOf("1.21.1", "1.21.4", "1.21.6", "1.21.11", "26.1")
    .map { it.replace(".", "") }
    .forEach { include("platform-paper-nms-$it") }

listOf("1.21.4", "1.21.6", "26.1")
    .map { it.replace(".", "") }
    .forEach { include("platform-folia-nms-$it") }

listOf("1.21.4", "1.21.7", "1.21.9", "1.21.11", "26.1.2")
    .map { it.replace(".", "") }
    .forEach { include("platform-fabric-$it") }

