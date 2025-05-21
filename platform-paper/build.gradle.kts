import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.gradle.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    api(libs.bstats)
    api(projects.core)
}

bukkit {
    main = "$group.playerculling.platformpaper.PlayerCullingPlugin"
    apiVersion = "1.21"
    authors = listOf("pianoman911")
    contributors = listOf("booky10")
    foliaSupported = true

    name = rootProject.name
    description = "${rootProject.ext["gitHash"]}/${rootProject.ext["gitBranch"]} " +
            "(${rootProject.ext["gitTag"]}), ${rootProject.ext["compileDate"]}"

    permissions {
        register("playerculling.update-notify") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("playerculling.bypass") {
            default = BukkitPluginDescription.Permission.Default.FALSE
        }
    }
}
