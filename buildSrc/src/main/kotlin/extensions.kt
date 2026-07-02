// Created by booky10 in BetterView (3:35 PM 05.04.2026)

import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory
import org.gradle.api.provider.Provider

public fun AbstractExternalDependencyFactory.VersionFactory.hackGetVersion(name: String): String {
    // why the fuck is this not public?
    val mthd = AbstractExternalDependencyFactory.VersionFactory::class.java.getDeclaredMethod("getVersion", String::class.java)
    mthd.setAccessible(true)
    return (mthd.invoke(this, name) as Provider<*>).get() as String
}
