plugins {
    // AGP 9.0+ has built-in Kotlin support — the standalone org.jetbrains.kotlin.android
    // plugin is no longer applied (it errors when applied alongside AGP 9). Kotlin compilation
    // is provided by AGP; the Kotlin version is driven by the serialization/ksp plugins on the
    // classpath.
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
