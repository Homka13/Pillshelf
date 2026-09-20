plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.pillshelf"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.pillshelf.shlfkb"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("releaseConfig") {
            // Читаємо з env-змінних, які виставляє GitHub Actions.
            // Якщо змінні відсутні — Gradle кине виключення одразу при конфігурації,
            // не даючи зібрати реліз без правильного keystore.
            val storeFilePath = requireNotNull(System.getenv("RELEASE_STORE_FILE")) {
                "RELEASE_STORE_FILE env var is missing. Release builds require a keystore."
            }
            storeFile = file(storeFilePath)
            storePassword = requireNotNull(System.getenv("RELEASE_STORE_PASSWORD")) {
                "RELEASE_STORE_PASSWORD env var is missing."
            }
            keyAlias = requireNotNull(System.getenv("RELEASE_KEY_ALIAS")) {
                "RELEASE_KEY_ALIAS env var is missing."
            }
            keyPassword = requireNotNull(System.getenv("RELEASE_KEY_PASSWORD")) {
                "RELEASE_KEY_PASSWORD env var is missing."
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release завжди підписується releaseConfig.
            // Ніякого фоллбеку на debug — щоб не зламати оновлення на пристроях.
            signingConfig = signingConfigs.getByName("releaseConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.ui.tooling)
}
