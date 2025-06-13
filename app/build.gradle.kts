// In /app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ultralytics.yoloapp" // Make sure this matches your project's package name
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ultralytics.yoloapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // ===================================================================
    //  SECTION 1: ADD THIS BLOCK FOR VIEW BINDING
    //  This allows you to safely access views like `yolo_view` from your code.
    // ===================================================================
    buildFeatures {
        viewBinding = true
    }

    // ===================================================================
    //  SECTION 2: ADD THIS BLOCK TO HANDLE NATIVE LIBRARIES (.so files)
    //  This tells Gradle how to package the pre-compiled native libraries.
    // ===================================================================
    packagingOptions {
        // This is important. Some libraries (like TFLite and Sentry) might both
        // include a 'libc++_shared.so'. This tells Gradle to just pick the first
        // one it finds and not to fail the build due to a duplicate file.
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst("lib/arm64-v8a/libc++_shared.so")
    }
}

// ===================================================================
//  SECTION 3: ADD THIS ENTIRE DEPENDENCIES BLOCK
//  This is the most critical missing piece. It declares all the libraries
//  our reverse-engineered Kotlin code needs to compile and run.
// ===================================================================
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CameraX for camera preview and analysis
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // TensorFlow Lite for model inference
    // We use the 'support' library for image processing and metadata
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4") // For GPU acceleration

    // For parsing model metadata (if using YAML)
    implementation("org.yaml:snakeyaml:2.0")

    // For ListenableFuture used by CameraX
    implementation("com.google.guava:guava:32.1.2-android")
}