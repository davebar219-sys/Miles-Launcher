plugins {
    id("com.android.application")
}

android {
    namespace = "com.davebar219.mileslauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.davebar219.mileslauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
}
