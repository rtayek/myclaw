plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ray.tvchat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ray.tvchat"
        minSdk = 26          // covers virtually all Google TV devices
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Compose for TV — components with proper 10-foot focus behavior
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    // WebSocket client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Tiny JSON (org.json ships with Android, used directly)
}
