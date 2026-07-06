plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jv.attentionpanner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jv.attentionpanner"
        minSdk = 33
        targetSdk = 37
        versionCode = 2
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // REMOVED: ndk { abiFilters... } 
        // We removed this because it restricts the build to only one CPU. 
        // The 'splits' block below handles multiple CPUs now.
    }

    // --- NEW: GENERATE MULTIPLE APKS ---
    splits {
        abi {
            isEnable = true
            reset()
            // Creates separate APKs for these architectures
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            // Generates a "Universal" (fat) APK that works on all devices as well
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFileEnv = System.getenv("KEYSTORE_PATH") ?: "${project.projectDir}/keystore.jks"
            storeFile = file(keystoreFileEnv)
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "Alpha789@"
            keyAlias = System.getenv("KEY_ALIAS") ?: "water"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "Alpha789@"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        isCheckReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

tasks.withType<com.android.build.gradle.tasks.R8Task> {
    enableR8FullMode = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Coil for Images/Video frames
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-video:3.3.0")

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}