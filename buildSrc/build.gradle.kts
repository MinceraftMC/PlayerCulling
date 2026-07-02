plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.plugin.paperweight)
    implementation(libs.plugin.shadow)
    implementation(libs.plugin.loom)
    implementation(libs.plugin.pluginyml)
    implementation(libs.plugin.runtask)
}
