dependencies {
    compileOnlyApi(libs.configurate.yaml)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.brigadier)
    compileOnly(libs.netty.all)
    api(projects.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.adventure.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j)
}
