import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    alias(libs.plugins.fabric.loom)
}

repositories {
    // adventure-platform-mod hasn't been released yet, use snapshot version
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    minecraft(libs.minecraft.v1217)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(variantOf(libs.parchment.v1217) { artifactType("zip") })
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabricapi.v1217)

    modImplementation(libs.adventure.platform.fabric.v1217)
    include(libs.adventure.platform.fabric.v1217)

    include(libs.fabric.permissions.v1217)
    modImplementation(libs.fabric.permissions.v1217)

    api(projects.core)

    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Implementation-Title" to rootProject.name,
        "Implementation-Vendor" to "pianoman911",
        "Implementation-Contributors" to "booky10",
        "Implementation-Version" to project.version,
        "License" to "AGPL-3.0",

        "Build-Date" to rootProject.ext["compileDate"],
        "Build-Timestamp" to rootProject.ext["compileTime"].toString(),

        "Git-Commit" to rootProject.ext["gitHash"],
        "Git-Branch" to rootProject.ext["gitBranch"],
        "Git-Tag" to rootProject.ext["gitTag"],

        "Environment" to project.gradle.startParameter.taskNames,
    )
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

loom {
    serverOnlyMinecraftJar()

    accessWidenerPath = sourceSets.main.get().resources
        .find { it.name == "playerculling.accesswidener" }
    mixin.defaultRefmapName = "${rootProject.name}-${project.name}-refmap.json".lowercase()
}
