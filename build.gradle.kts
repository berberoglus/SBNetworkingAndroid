plugins {
    // AGP 9.0+ has built-in Kotlin support — the standalone org.jetbrains.kotlin.android
    // plugin is no longer applied (it errors when applied alongside AGP 9). Kotlin compilation
    // is provided by AGP; the Kotlin version is driven by the serialization plugin on the classpath.
    // The library is DI-framework-agnostic: it applies neither Hilt nor KSP. Hilt wiring is an
    // optional consumer-side snippet documented in README.md ("Optional Hilt wiring").
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
