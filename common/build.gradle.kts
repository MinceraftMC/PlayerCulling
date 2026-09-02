plugins {
    com.gradleup.shadow
}

dependencies {
    api(libs.jspecify)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
}
