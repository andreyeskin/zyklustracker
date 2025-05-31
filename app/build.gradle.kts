plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        exclude("com.intellij", "annotations")
    }
}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.compiler)
    implementation(libs.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.kizitonwose:CalendarView:1.0.4")
    implementation("com.google.android.material:material:1.10.0")
}
