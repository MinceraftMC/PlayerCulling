plugins {
    alias(libs.plugins.paperweight.userdev)
}

dependencies {
    implementation(project(":platform-paper"))
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
}