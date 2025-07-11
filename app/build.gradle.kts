import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.starsentinel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.starsentinel"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}


dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.navigation.runtime.android)
    implementation(libs.material3.android)
    implementation(libs.navigation.compose)
    implementation(libs.runtime.livedata)
    implementation(libs.litert)
    implementation(libs.media3.common.ktx)
    implementation(libs.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.gson)
    implementation (libs.play.services.maps)
    implementation (libs.play.services.location)
    implementation (libs.maps.compose)
    implementation(libs.lottie.compose)
    implementation (libs.okhttp)
    implementation (libs.json)
    implementation(libs.health.services.client)


}