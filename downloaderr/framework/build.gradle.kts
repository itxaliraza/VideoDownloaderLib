plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

}

android {
    namespace = "com.example.framework"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures{
        buildConfig=true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.support)
    implementation(libs.compose.glide)
    implementation(libs.sdp.compose)

    //Kotlin ktor
    implementation(libs.bundles.ktor.app)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    implementation(libs.test.prsr)
//    implementation(libs.bundles.ads)


    debugImplementation(libs.leakcanary.android)

    //Text and Size-ing Libs
    implementation(libs.ssp.android)
    implementation(libs.sdp.android)
}