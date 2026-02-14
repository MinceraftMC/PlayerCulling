import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.stream.Stream

plugins {
    alias(libs.plugins.paperweight.userdev) apply false
    alias(libs.plugins.gradle.shadow) apply false
    alias(libs.plugins.fabric.loom) apply false
}

ext["gitHash"] = git("rev-parse --short HEAD").get()
ext["gitBranch"] = git("rev-parse --abbrev-ref HEAD").get()
ext["gitTag"] = git("describe --tags --abbrev=0").get()

fun git(git: String): Provider<String> {
    return providers.exec {
        commandLine(Stream.concat(Stream.of("git"), git.split(" ").stream()).toList())
    }.standardOutput.asText.map { it.trim() }
}

ext["compileTime"] = ZonedDateTime.now(ZoneOffset.UTC)
ext["compileDate"] = DateTimeFormatter.ISO_DATE_TIME.format(ext["compileTime"] as Temporal) as String

allprojects {
    group = "de.pianoman911"
    version = "2.1.4-SNAPSHOT"
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://libraries.minecraft.net/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    tasks.withType<Jar> {
        archiveBaseName = "${rootProject.name.lowercase()}-${project.name}"

        manifest.attributes(
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

    configure<JavaPluginExtension> {
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.ADOPTIUM
        }
    }

    configure<PublishingExtension> {
        repositories.maven("https://repo.minceraft.dev/releases/") {
            name = "minceraft"
            authentication { create<BasicAuthentication>("basic") }
            credentials(PasswordCredentials::class)
        }
        publications.create<MavenPublication>("maven") {
            artifactId = "${rootProject.name}-${project.name}".lowercase()
            from(components["java"])
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
