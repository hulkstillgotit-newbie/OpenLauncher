import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    id("kotlin-kapt")
}

android {
    namespace = "com.android.launcher3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.launcher3"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../proguard.flags")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "../src",
                "../src_no_quickstep",
                "../src_build_config",
                "../src_plugins",
                // AOSP iconloaderlib (provides BaseIconFactory, BitmapInfo, SafeCloseable, etc.)
                "../iconloaderlib/src",
                "../iconloaderlib/src_full_lib",
                // AOSP animationlib (provides Interpolators)
                "../animationlib/src",
                // AOSP msdllib (provides MSDLPlayer, MSDLToken, etc.)
                "../msdllib/src",
                // Testing shared (provides TestProtocol)
                "../tests/multivalentTests/shared",
                // Remaining AOSP stubs (Flags, window flags, etc.)
                "../stubs"
            )
            res.srcDirs("../res", "../iconloaderlib/res", "../animationlib/res")
            manifest.srcFile("../AndroidManifest.xml")
            proto {
                srcDir("../protos")
                srcDir("../protos_overrides")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Allow access to internal/hidden android APIs via stub sources
    lint {
        abortOnError = false
        baseline = file("../lint-baseline.xml")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.fragment:fragment:1.8.4")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.slice:slice-view:1.1.0-alpha02")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.window:window-core:1.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-animation:1.0.0")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Compose
    implementation("androidx.compose.runtime:runtime:1.7.4")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.ui:ui:1.7.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.4")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // Dagger 2
    implementation("com.google.dagger:dagger:2.52")
    kapt("com.google.dagger:dagger-compiler:2.52")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Protobuf Lite
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")

    // Lottie (used by quickstep resources lib, harmless to include)
    implementation("com.airbnb.android:lottie:6.5.2")
}
