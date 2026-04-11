import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.minecraft.v12111)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(variantOf(libs.parchment.v12111) { artifactType("zip") })
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabricapi.v12111)

    modImplementation(libs.adventure.platform.fabric.v12111)

    include(libs.fabric.permissions.v12111)
    modImplementation(libs.fabric.permissions.v12111)

    api(projects.core)

    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)
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
