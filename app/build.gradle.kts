plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Add the Google services Gradle plugin
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.octopus.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.octopus.launcher"
        minSdk = 23  // Firebase requires minSdk 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Disable lint for release builds to avoid file locking issues on Windows
            isDebuggable = false
            // Enable performance optimizations
            isShrinkResources = true
        }
        debug {
            // Increase memory for debug builds
            isDebuggable = true
        }
    }
    
    // Disable lint checks for release builds
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    
    // Configure packaging options for better performance
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // Enable compiler optimizations for maximum performance
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            // Enable aggressive optimizations for release builds
            "-opt-in=kotlin.contracts.ExperimentalContracts"
        )
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    // Material Icons Extended for more icon options
    // Note: This should be managed by the Compose BOM, but if it doesn't work, 
    // we may need to use custom vector drawables instead
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    
    // Coil for smooth image loading and rendering
    implementation(libs.coil.compose)
    
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    
    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")
    
    // Firebase Crashlytics (optional, for crash reporting)
    // implementation("com.google.firebase:firebase-crashlytics")
    
    // Firebase Remote Config (optional, for dynamic configuration)
    // implementation("com.google.firebase:firebase-config")
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}