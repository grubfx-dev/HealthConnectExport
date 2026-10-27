plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

// The release workflow exports the computed semantic version so the APK
// reports the same version as its GitHub release.
val semanticVersion: String =
    providers.environmentVariable("HECEXP_VERSION").orElse("0.0.1").get()

android {
    namespace = "dev.grubfx.healthconnect"
    compileSdk = 37

    defaultConfig {
        applicationId = "$namespace.export"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = semanticVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Credentials come from environment variables so they never land in
            // the repository. Locally they can be set in ~/.gradle/gradle.properties
            // or exported in the shell. In CI they are injected as secrets.
            val storePath = providers.environmentVariable("HECEXP_KEYSTORE_PATH").orNull
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = providers.environmentVariable("HECEXP_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("HECEXP_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("HECEXP_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only sign when a keystore is actually available, so unsigned
            // local builds still work.
            signingConfig = if (providers.environmentVariable("HECEXP_KEYSTORE_PATH").isPresent) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.connect.client)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}