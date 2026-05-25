import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    id("playerculling.fabric-official")
}

dependencies {
    minecraft(libs.minecraft.v261)

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabricapi.v261)

    modImplementation(libs.adventure.platform.fabric.v261)

    include(libs.fabric.permissions.v261)
    modImplementation(libs.fabric.permissions.v261)

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
