plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.berberoglus.sbnetworking"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        // AGP 9 removed targetSdk from the library DSL (it is meaningless for a library);
        // compileSdk 36 is the effective target.
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
        }
    }

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    // Kotlin 2.x already emits real JVM default methods (the old -Xjvm-default=all is deprecated
    // and unnecessary), so interface defaults like AuthTokenProvider.refresh() work out of the box.
    jvmToolchain(21)
}

dependencies {
    api(platform(libs.okhttp.bom))
    api(libs.retrofit.core)
    api(libs.retrofit.serialization)
    api(libs.okhttp.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
