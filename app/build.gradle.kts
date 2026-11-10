import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val weatherApiKey = providers.gradleProperty("OPENWEATHER_API_KEY").orNull
    ?: localProperties.getProperty("OPENWEATHER_API_KEY")
    ?: throw GradleException("Set OPENWEATHER_API_KEY in local.properties before building the app.")
require(weatherApiKey.isNotBlank() && weatherApiKey.all { it.isLetterOrDigit() }) {
    "OPENWEATHER_API_KEY must be a non-empty alphanumeric key."
}

android {
    namespace = "com.adamekka.tamz"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.adamekka.tamz"
        minSdk = 37
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$weatherApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
