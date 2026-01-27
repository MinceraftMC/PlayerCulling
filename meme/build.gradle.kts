import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.minecraft.v1219)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(variantOf(libs.parchment.v1219) { artifactType("zip") })
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabricapi.v1219)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to rootProject.name,
            "Implementation-Vendor" to "pianoman911",
            "Implementation-Contributors" to "booky10",
            "Implementation-Version" to project.version,
            "License" to "AGPL-3.0",

            "Build-Date" to rootProject.ext["compileDate"].toString(),
            "Build-Timestamp" to rootProject.ext["compileTime"].toString(),

            "Git-Commit" to rootProject.ext["gitHash"].toString(),
            "Git-Branch" to rootProject.ext["gitBranch"].toString(),
            "Git-Tag" to rootProject.ext["gitTag"].toString(),

            "Environment" to project.gradle.startParameter.taskNames,
        )
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

loom {
    clientOnlyMinecraftJar()

    accessWidenerPath = sourceSets.main.get().resources
        .find { it.name == "playerculling.accesswidener" }
    mixin.defaultRefmapName = "${rootProject.name}-${project.name}-refmap.json".lowercase()
}
