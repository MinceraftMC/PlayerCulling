import java.util.Locale.getDefault

plugins {
    alias(libs.plugins.gradle.shadow)
}

dependencies {
    api(projects.platformCommon)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// hack to allow depending on this platform in java 21 projects
configurations.runtimeElements.configure {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

val os = System.getProperty("os.name").lowercase(getDefault())
val targetOsDir = if (os.contains("win")) "windows_x64" else "linux_x64"

val compileNativesWithMake by tasks.registering(Exec::class) {
    group = "build"
    description = "Compile natives"

    workingDir = projectDir

    inputs.dir(projectDir.resolve("src/main/cpp/src"))

    doFirst {
        logger.lifecycle("Executing: ${commandLine.joinToString(" ")} inside $workingDir")
    }

    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine("mingw32-make", "-B")
    } else {
        commandLine("make", "-B")
    }
}

val cleanNativesWithMake by tasks.registering(Exec::class) {
    group = "build"
    description = "Clean natives"

    workingDir = projectDir

    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine("mingw32-make", "clean")
    } else {
        commandLine("make", "clean")
    }
}

tasks.processResources {
    dependsOn(compileNativesWithMake)
    from(layout.buildDirectory.dir("libs/natives/$targetOsDir")) {
        into("libs")
        include("playerculling_avx2.so")
    }
}

tasks.clean {
    dependsOn(cleanNativesWithMake)
}