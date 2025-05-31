import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled
plugins {
    alias(libs.plugins.android.application)
    // Убираем Kotlin плагины, так как проект на Java
    // alias(libs.plugins.kotlin.android)
    // id("kotlin-kapt")
}

android {
    namespace = "at.fhj.andrey.zyklustracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.fhj.andrey.zyklustracker"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    // ViewBinding включаем
    buildFeatures {
        dataBinding = false
        viewBinding = true
    }
}

// Конфигурация для решения конфликтов зависимостей
configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        exclude("com.intellij", "annotations")
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Убираем Kotlin core, так как проект на Java
    // implementation(libs.core.ktx)

    // Java 8+ API desugaring для поддержки LocalDate на старых Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.material:material:1.10.0")

    // Calendar View
    implementation("com.github.kizitonwose:CalendarView:1.0.4")

    // Room Database (ИСПРАВЛЕНО: используем annotationProcessor для Java)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Gson для JSON конвертеров
    implementation("com.google.code.gson:gson:2.10.1")

    // MPAndroidChart для графиков
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Lifecycle компоненты для лучшей работы с Room
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
}