plugins {
    // AGP 9.0 起内置 Kotlin 支持：org.jetbrains.kotlin.android 不再应用
    // （应用会直接报 "no longer required for Kotlin support since AGP 9.0"）。
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}