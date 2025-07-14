import com.google.gson.JsonObject
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.ZipUtils

plugins {
    alias(libs.plugins.fabric.loom)
}

// this subproject supports multiple versions, while our dependencies
// don't; therefore, we need to tell fabric to only load our dependencies
// on specific versions (see preparation tasks below)
val allVersions = listOf("1.21.4", "1.21.5")
    .associateWith { it.replace(".", "") }
allVersions.values.forEach {
    configurations.create("include$it") { isTransitive = false }
}

dependencies {
    minecraft(libs.minecraft.v1214)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(variantOf(libs.parchment.v1214) { artifactType("zip") })
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabricapi.v1214)

    modImplementation(libs.adventure.platform.fabric.v1214)
    "include1214"(libs.adventure.platform.fabric.v1214)
    "include1215"(libs.adventure.platform.fabric.v1215)

    include(libs.fabric.permissions.v1214)
    modImplementation(libs.fabric.permissions.v1214)

    api(projects.core)

    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)
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

        "Environment" to project.gradle.startParameter.taskNames,
    )
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

// forces dependencies into a file
fun forceDependencies(jarFile: File, dependencies: Map<String, String>) {
    println("Forcing $dependencies into $jarFile")
    ZipUtils.transformJson(JsonObject::class.java, jarFile.toPath(), "fabric.mod.json", {
        var depends = it.getAsJsonObject("depends")
        if (depends == null) {
            depends = JsonObject().apply { it.add("depends", this) }
        }
        // add all parameters to "depends" block
        dependencies.forEach { id, version -> depends.addProperty(id, version) }
        return@transformJson it
    })
}

// prepare jars so fabric only loads them on specific minecraft versions
allVersions.forEach { version, miniVersion ->
    tasks.register<Copy>("prepRemapJar$miniVersion") {
        from(configurations.named("include$miniVersion").map { it.resolve() })
        into(project.layout.buildDirectory.dir("prepRemap/$miniVersion"))

        doLast {
            outputs.files.asFileTree.forEach {
                forceDependencies(it, mapOf("minecraft" to version))
            }
        }
    }
}

// include version specific jars
tasks.named<RemapJarTask>("remapJar") {
    allVersions.values.forEach { miniVersion ->
        nestedJars.from(
            tasks.named("prepRemapJar$miniVersion")
                .map { it.outputs.files.asFileTree })
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
