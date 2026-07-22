dependencies {
    api(projects.platformCommon)
    compileOnly(libs.google.gson)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.adventure.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j)
}
