import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

plugins {
    alias(libs.plugins.fabric.loom)
}

val testTaskVersion = "1.21.5"
val testTaskVersionFiltered = testTaskVersion.replace(".", "")

loom.noIntermediateMappings()

val includeAll: Configuration by configurations.creating

dependencies {
    // dummy fabric env setup
    minecraft("com.mojang:minecraft:$testTaskVersion")
    mappings(loom.officialMojangMappings())
    // required for production run tasks, otherwise we
    // will just get cryptic error messages
    modImplementation(libs.fabric.loader)

    // include common project once
    include(projects.core)
    include(projects.platformCommon)
    include(projects.api)
    include(libs.fabric.permissions)

    // include common dependencies
    sequenceOf(libs.configurate.yaml).forEach {
        includeAll(it) {
            exclude("net.kyori", "option") // included in adventure platforms
            exclude("com.google.errorprone", "error_prone_annotations") // useless
            exclude("org.jspecify") // useless
        }
    }

    // include all fabric versions
    rootProject.subprojects
        .filter { it.name.matches("^platform-fabric-\\d+$".toRegex()) }
        .forEach { include(it) }

    // version-specific runtime mods
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named<RemapJarTask>("remapJar") {
    // include common dependencies transitively
    fun doInclude(dep: ResolvedDependency) {
        configurations.named("include").get().withDependencies {
            this.add(dependencyFactory.create(dep.moduleGroup, dep.moduleName, dep.moduleVersion))
        }
        dep.children.forEach { doInclude(it) }
    }
    includeAll.resolvedConfiguration.firstLevelModuleDependencies.forEach { doInclude(it) }
    // final fabric jar, place it in root build dir
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
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

        "paperweight-mappings-namespace" to "mojang",
        "Environment" to project.gradle.startParameter.taskNames
    )
}

tasks.withType<AbstractRemapJarTask> {
    archiveBaseName = rootProject.name
    archiveClassifier = "fabric"
}

loom {
    mixin.defaultRefmapName = "${rootProject.name}-${project.name}-refmap.json".lowercase()
}

// fabric's default task doesn't allow us to specify that we want to have standard input
@UntrackedTask(because = "Always rerun this task.")
abstract class CustomServerProductionRunTask : ServerProductionRunTask {

    @Inject
    constructor() : super()

    override fun configureProgramArgs(exec: ExecSpec?) {
        super.configureProgramArgs(exec)
        exec!!.standardInput = System.`in`
    }
}

tasks.register<CustomServerProductionRunTask>("prodServer") {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}