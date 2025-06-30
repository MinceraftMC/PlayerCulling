plugins {
    alias(libs.plugins.paperweight.userdev)
}

dependencies {
    implementation(project(":platform-paper"))
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
}