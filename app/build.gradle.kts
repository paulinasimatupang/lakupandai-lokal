plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "id.co.bankntbsyariah.lakupandai"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.co.bankntbsyariah.lakupandai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
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
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Glide (only keep the latest version)
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.google.firebase.messaging.ktx)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")

    // Retrofit for network requests
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.4.1")
    implementation("androidx.room:room-ktx:2.4.1")

    // Firebase dependencies (only keep one Crashlytics and Analytics)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Biometric Authentication
    implementation ("androidx.biometric:biometric:1.2.0-alpha04")

    // Signature pad library
    implementation ("com.github.gcacace:signature-pad:1.3.1")

    // Gson (keep one version)
    implementation("com.google.code.gson:gson:2.8.9")

    // Lottie animation
    implementation("com.airbnb.android:lottie:5.2.0")

    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui:1.5.1") // Ensure you have the correct Compose UI version
    implementation("androidx.compose.ui:ui-tooling:1.5.1") // Add ui-tooling dependency
    implementation("androidx.compose.material3:material3:1.1.0") // Add Material3 dependency
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0") // Optional for window size classes
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.1") // Preview support
    implementation("com.gu.android:toolargetool:0.3.0")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
