import java.util.Locale.getDefault

plugins {
    base
    java
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

val compileNativesWithMake by tasks.registering(Exec::class) {
    group = "build"
    description = "Compile natives"

    workingDir = projectDir

    val os = System.getProperty("os.name").lowercase(getDefault())
    val targetDir = if (os.contains("win")) "windows_x64" else "linux_x64"

    outputs.dir(layout.buildDirectory.dir("resources/main/natives/$targetDir"))

    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine("mingw32-make")
    } else {
        commandLine("make")
    }
}

tasks.processResources {
    from("build/resources/main/natives/linux_x64"){
        include("playerculling_avx2.so")
        into("libs")
    }
}

tasks.assemble {
    dependsOn(compileNativesWithMake)
}