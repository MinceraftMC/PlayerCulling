import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    com.gradleup.shadow
}

dependencies {
    implementation(projects.meme)

    api(libs.disruptor)
    runtimeOnly(libs.jline.terminal.jansi)
    api(libs.terminalconsoleappender)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        transform(Log4j2PluginsCacheFileTransformer::class.java)
        filesMatching("META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
