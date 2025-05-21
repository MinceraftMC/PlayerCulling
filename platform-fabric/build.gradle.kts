plugins {
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(variantOf(libs.parchment) { artifactType("zip") })
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    sequenceOf(
        libs.adventure.platform.fabric,
        libs.fabric.permissions
    ).forEach {
        modImplementation(it)
        include(it)
    }

    include(api(projects.core)!!)
    sequenceOf(
        libs.configurate.core,
        libs.configurate.yaml
    ).forEach {
        implementation(it)
        include(it)
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    jar {
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

            "paperweight-mappings-namespace" to "mojang",
            "Environment" to project.gradle.startParameter.taskNames
        )
    }

    remapJar {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        archiveBaseName = rootProject.name
        archiveClassifier = "fabric"
    }
}

loom {
    accessWidenerPath.set(
        sourceSets.main.get().resources
            .find { it.name == "playerculling.accesswidener" })
}
