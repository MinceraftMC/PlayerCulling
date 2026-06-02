import java.util.Locale
import java.util.Locale.getDefault

plugins {
    base
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

// Integriere das Kompilieren in den Standard-Build-Prozess des Moduls
tasks.assemble {
    dependsOn(compileNativesWithMake)
}