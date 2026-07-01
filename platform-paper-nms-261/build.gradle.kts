plugins {
    io.papermc.paperweight.userdev
}

dependencies {
    implementation(project(":platform-paper"))
    paperweight.paperDevBundle("26.1.2.build.+")
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// hack to allow depending on this platform in java 17 projects
configurations.runtimeElements.configure {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}
