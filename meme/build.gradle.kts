plugins {
    com.gradleup.shadow
}

dependencies {
    api(libs.google.gson)
    api(libs.jspecify)
    api(libs.checkerframework)
    api(projects.common)

    api(libs.slf4j)
    api(libs.bundles.log4j)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
}
