import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

plugins {
    net.fabricmc.`fabric-loom`
}

val testTaskVersion = "26.1.2"
val testTaskVersionFiltered = testTaskVersion.replace(".", "")

loom.noIntermediateMappings()

val includeAll: Configuration by configurations.creating

dependencies {
    // dummy fabric env setup (use non-obfuscated version for less prep time)
    minecraft("com.mojang:minecraft:26.1.2")

    // include common project once
    include(projects.core)
    include(projects.platformCommon)
    include(projects.api)

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

    // fabric api
    val fabricApiVersion = libs.versions.hackGetVersion("fabricapi.v$testTaskVersionFiltered")
    productionRuntimeMods("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // adventure component library
    val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.v$testTaskVersionFiltered")
    productionRuntimeMods("net.kyori:adventure-platform-fabric:$adventureVersion")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named<Jar>("jar") {
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
}

tasks.register<CustomServerProductionRunTask>("prodServer") {
    minecraftVersion = testTaskVersion
    loaderVersion = libs.versions.fabric.loader.get()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
