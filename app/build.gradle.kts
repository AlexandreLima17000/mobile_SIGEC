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

        val dbUrl = envProperties.getProperty("DB_URL") ?: "jdbc:mysql://10.0.2.2:3306/sigec?useSSL=false&allowPublicKeyRetrieval=true"
        val dbUser = envProperties.getProperty("DB_USER") ?: "root"
        val dbPassword = envProperties.getProperty("DB_PASSWORD") ?: envProperties.getProperty("DB_PASS") ?: ""

        buildConfigField("String", "DB_URL", "\"$dbUrl\"")
        buildConfigField("String", "DB_USER", "\"$dbUser\"")
        buildConfigField("String", "DB_PASSWORD", "\"$dbPassword\"")
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE"
        }
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
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    implementation("mysql:mysql-connector-java:5.1.49")
}

