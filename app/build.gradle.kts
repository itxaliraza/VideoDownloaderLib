plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"

}

android {
    namespace = "adm.downloader"
    compileSdk = 36

    defaultConfig {
        applicationId = "adm.downloader"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
     implementation(libs.androidx.material3)
    implementation(project(":downloaderr:sdk"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
     debugImplementation(libs.androidx.ui.tooling)

    implementation(project(":adm_downloader"))
    implementation(project(":downloaderr:main"))
    implementation(project(":downloaderr:framework"))
    implementation(project(":downloaderr:domain"))
    implementation(project(":downloaderr:entities"))
    implementation(project(":downloaderr:data"))
    implementation(project(":downloaderr:persistence"))
    implementation(libs.kotlinx.serialization.json)


    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.sdp.compose)
    implementation(libs.compose.glide)

}