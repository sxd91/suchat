import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0 起内置 Kotlin 支持，不再应用 org.jetbrains.kotlin.android。
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun gitShortHash(): String = providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
}.standardOutput.asText.get().trim()

val versionProps = Properties().apply {
    val f = file("../version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.sxd91.suchat"
    // compileSdk 37 / minSdk 33：受 miuix-blur 0.9.4-rc01 的 manifest 约束
    // （其 minSdkVersion 为 33，且需要 compileSdk 37 + Compose 1.12 + AGP 9.1+）。
    compileSdk = 37

    signingConfigs {
        val jks = file("../keystore.jks")
        if (jks.exists()) {
            register("release") {
                storeFile = jks
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.sxd91.suchat"
        minSdk = 33
        targetSdk = 37
        versionCode = (project.findProperty("versionCode") as String?
            ?: versionProps.getProperty("versionCode", "1")).toInt()
        versionName = project.findProperty("versionName") as String?
            ?: versionProps.getProperty("versionName", "0.1.0")

        vectorDrawables { useSupportLibrary = true }
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        buildConfigField(
            "String",
            "GIT_HASH",
            "\"${runCatching { gitShortHash() }.getOrDefault("unknown")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: getByName("debug").signingConfig
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

dependencies {
    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // --- miuix（LiquidGlass 悬浮底栏 / shader / 主题与组件 / 图标） ---
    // 这是 Android 体验契约的核心：底栏必须走 WeKit 液态玻璃模型
    // （pill 几何 + backdrop 采样 + 弹性拖拽指示器 + 按压物理）。
    implementation(libs.miuix.blur)
    implementation(libs.miuix.shader)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)

    // --- material-kolor（莫奈取色） ---
    implementation(libs.materialkolor)

    // --- kotlinx-serialization ---
    implementation(libs.kotlinx.serialization.json)

    // --- 网络与本地存储（既有数据层保留） ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
}