plugins {
    alias(libs.plugins.runtask.paper)
    alias(libs.plugins.gradle.shadow)
}

runPaper.folia.registerTask()

dependencies {
    listOf("1.21.1", "1.21.4", "1.21.6", "1.21.11")
        .map { "paper-nms-${it.replace(".", "")}" }
        .forEach { implementation(project(":platform-$it")) }
    listOf("1.21.4", "1.21.6")
        .map { "folia-nms-${it.replace(".", "")}" }
        .forEach { implementation(project(":platform-$it")) }
}

tasks {
    runServer {
        runDirectory = project.layout.projectDirectory.dir("run")
        minecraftVersion("1.21.11")
    }

    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveBaseName = rootProject.name
        archiveClassifier = "paper"
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")

        relocate("org.bstats", "de.pianoman911.playerculling.bstats")
    }

    jar {
        manifest.attributes(
            "paperweight-mappings-namespace" to "mojang",
        )
    }

    assemble {
        dependsOn(shadowJar)
    }
}
