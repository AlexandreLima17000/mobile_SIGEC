import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()){
        load(envFile.inputStream())
    }
}


android {
    namespace = "com.example.mobilesigec"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mobilesigec"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DB_URL", "\"${envProperties.getProperty("DB_URL") ?: ""}\"")
        buildConfigField("String", "DB_USER", "\"${envProperties.getProperty("DB_USER") ?: ""}\"")
        buildConfigField("String", "DB_PASSWORD", "\"${envProperties.getProperty("DB_PASSWORD") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("mysql:mysql-connector-java:5.1.49")
}

