plugins {
    alias(libs.plugins.runtask.paper)
    alias(libs.plugins.gradle.shadow)
}

runPaper.folia.registerTask()

dependencies {
    listOf("1.21.1", "1.21.4")
        .map { "paper-nms-${it.replace(".", "")}" }
        .forEach { implementation(project(":platform-$it")) }
    listOf("1.21.4")
        .map { "folia-nms-${it.replace(".", "")}" }
        .forEach { implementation(project(":platform-$it")) }
}

tasks {
    runServer {
        runDirectory = project.layout.projectDirectory.dir("run")
        minecraftVersion("1.21.5")
    }

    shadowJar {
        mergeServiceFiles()

        archiveBaseName = rootProject.name
        archiveClassifier = "paper"
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")

        relocate("org.bstats", "de.pianoman911.playerculling.bstats")
    }

    assemble {
        dependsOn(shadowJar)
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
}
