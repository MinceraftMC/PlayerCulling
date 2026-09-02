plugins {
    com.gradleup.shadow
}

dependencies {
    api(libs.google.gson)
    api(libs.jspecify)
    api(libs.checkerframework)
    api(projects.common)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
}
