plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jfmart.JFMART"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jfmart.JFMART"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Backend address. 10.0.2.2 is the PC from inside the Android emulator; for a real phone put
        // jfmartApiUrl=http://<PC-LAN-IP>:8080/ in gradle.properties (or ~/.gradle/gradle.properties).
        val apiUrl = providers.gradleProperty("jfmartApiUrl").orElse("http://10.0.2.2:8080/").get()
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // QR codes: zxing-core draws the courier's code, zxing-embedded is the client's camera scanner
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}