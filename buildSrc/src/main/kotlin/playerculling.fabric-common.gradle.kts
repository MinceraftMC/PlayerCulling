// Created by booky10 in BetterView (6:21 PM 04.04.2026)

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    `java-library`
}

dependencies {
    // common project setup
    api(project(":common")) {
        exclude(group = "net.kyori")
    }
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<AbstractRemapJarTask> {
    archiveBaseName = "${rootProject.name}-${project.name}".lowercase()
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath = file("src/main/resources/${rootProject.name.lowercase()}.accesswidener")
    mixin.defaultRefmapName = "${rootProject.name}-${project.name}-refmap.json".lowercase()
}
