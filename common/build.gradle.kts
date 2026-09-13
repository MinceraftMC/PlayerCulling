plugins {
    com.gradleup.shadow
}

dependencies {
    api(libs.bundles.configurate)
    api(libs.jspecify)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
}
