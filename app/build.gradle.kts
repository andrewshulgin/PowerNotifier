plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.andrewshulgin.powernotifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.andrewshulgin.powernotifier"
        minSdk = 14
        targetSdk = 34
        versionCode = 202406050
        versionName = "0.0.2"
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
}

dependencies {
    implementation(libs.constraint.layout)
    implementation(libs.appcompat.v7)
}
