import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


android {
    namespace = "at.fhj.andrey.zyklustracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "at.fhj.andrey.zyklustracker"
        minSdk = 28  // Health Connect требует минимум Android 9 (API 28)
        targetSdk = 36
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



        buildFeatures {
            dataBinding = false
            viewBinding = true
        }
    }

    buildFeatures {
        dataBinding = false
        viewBinding = true
    }
}
// <-- Добавляем блок kotlin { jvmToolchain }
kotlin {
    jvmToolchain(17)  // указываем Java 17 для Kotlin
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17" // JVM-таргет для компилятора Kotlin
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        exclude("com.intellij", "annotations")
    }
}

dependencies {
    // ===== CORE ANDROID DEPENDENCIES =====
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)

    // Java 8+ API desugaring для LocalDate
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ===== HEALTH CONNECT - СТАБИЛЬНАЯ ВЕРСИЯ 2025 =====
    implementation("androidx.health.connect:connect-client:1.1.0-rc01")

    // Lifecycle для Health Connect
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")

    // Activity Result API для permissions
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.fragment:fragment:1.6.2")

    // Coroutines для Java-Health Connect bridge
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ===== TESTING =====
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ===== UI COMPONENTS =====
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.kizitonwose:CalendarView:1.0.4")

    // ===== ROOM DATABASE =====
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ===== JSON UND CHARTS =====
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ===== CONCURRENT UTILITIES =====
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("com.google.guava:guava:31.1-android")
}