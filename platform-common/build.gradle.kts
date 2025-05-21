dependencies {
    compileOnlyApi(libs.configurate.yaml)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.brigadier)
    compileOnly(libs.netty.all)
    api(projects.api)
}
