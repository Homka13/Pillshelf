plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// true лише коли передані УСІ чотири env-змінні для підпису релізу.
val hasReleaseSigning = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD",
).all { !System.getenv(it).isNullOrBlank() }

android {
    namespace = "com.example.pillshelf"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yukhymshulha.pillshelf"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("releaseConfig") {
            // Значення беруться з env-змінних, які виставляє GitHub Actions.
            // Читаємо "м'яко": requireNotNull тут виконувався б на етапі конфігурації
            // і ламав БУДЬ-ЯКУ Gradle-команду (wrapper, assembleDebug, sync в IDE)
            // на машині без release-секретів. Жорстка перевірка — нижче, у tasks.
            if (hasReleaseSigning) {
                storeFile = file(System.getenv("RELEASE_STORE_FILE")!!)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Підпис не задаємо: AGP сам підписує debug згенерованим
            // ~/.android/debug.keystore. Раніше тут був debugConfig, що
            // вказував на ${rootDir}/debug.keystore — файл, якого немає в
            // репозиторії (він під *.keystore у .gitignore), тож
            // assembleDebug падав на будь-якому чистому клоні.
            // Фіксований debug-сертифікат тут ні до чого не прив'язаний:
            // ні Maps, ні Firebase у проєкті немає.
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Ніякого фоллбеку на debug-ключ — щоб не зламати оновлення на пристроях.
            // Без секретів signingConfig лишається null → APK буде UNSIGNED,
            // і крок apksigner verify у CI зупинить реліз.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("releaseConfig")
            } else {
                null
            }
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

ksp {
    // Схема Room експортується в git (app/schemas/) — це основа для
    // чесних міграцій: будь-яка зміна версії БД має супроводжуватися
    // новим JSON-файлом схеми в коміті.
    arg("room.schemaLocation", "$projectDir/schemas")
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
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}

// Жорсткий запобіжник: збирати release без ключа не можна.
// Перевірка на taskGraph.whenReady спрацьовує ДО старту виконання задач,
// тому падає одразу, а не після 10 хвилин компіляції. І не заважає ні
// `wrapper`, ні debug-збірці, ні синхронізації проєкту в Android Studio.
gradle.taskGraph.whenReady {
    val needsSigning = allTasks.any {
        it.name == "assembleRelease" || it.name == "bundleRelease"
    }
    if (needsSigning) {
        check(hasReleaseSigning) {
            "Release build requires signing env vars: RELEASE_STORE_FILE, " +
                "RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD."
        }
    }
}
