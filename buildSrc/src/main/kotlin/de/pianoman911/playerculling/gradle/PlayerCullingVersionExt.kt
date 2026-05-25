package de.pianoman911.playerculling.gradle;
// Created by booky10 in BetterView (7:43 PM 04.04.2026)

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.property
import org.jetbrains.annotations.ApiStatus

abstract class BetterViewVersionExt(
    objects: ObjectFactory,
) {

    @ApiStatus.Internal
    val afterEvaluate: MutableList<Runnable> = ArrayList()

    val versionName: Property<String> = objects.property<String>()

    val languageVersion: Property<JavaLanguageVersion> = objects.property<JavaLanguageVersion>()
        .convention(JavaLanguageVersion.of(21))

    @ApiStatus.Internal
    fun dependencyVersion(): Provider<String> {
        return versionName.map { "v${it.replace(".", "")}" }
    }
}
