plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

// The release workflow exports the computed semantic version so the APK
// reports the same version as its GitHub release.
val semanticVersion: String =
    providers.environmentVariable("HECEXP_VERSION").orElse("0.0.1").get()

/**
 * Turns the semantic version into the numeric version code Android uses to
 * decide whether an update is an upgrade.
 *
 * The code is derived from the version instead of being set by hand, so a
 * release always carries a code that is higher than the one before it.
 * Without that, the first update after the initial release would be
 * refused by the system.
 *
 * The scheme is major * 10_000 + minor * 100 + patch, with a multiplier of
 * 100 for each part, so a part can grow by 99 before it reaches the next
 * one. The result stays inside the 2.1 billion limit Android imposes.
 */
fun versionCodeOf(version: String): Int {
    val parts = version
        .substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }

    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "dev.grubfx.healthconnect"
    compileSdk = 37

    defaultConfig {
        applicationId = "$namespace.export"
        minSdk = 33
        targetSdk = 37
        versionCode = versionCodeOf(semanticVersion)
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

/**
 * The sources of the Health Connect SDK.
 *
 * The SDK does not ship a schema file, but its sources declare every record
 * type with its aggregate metrics, the name of the field they aggregate and
 * the unit they are converted to. The generator reads those declarations, so
 * the metric registry of the app is always in step with the SDK.
 */
val healthConnectSources: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    healthConnectSources(libs.androidx.connect.client) {
        artifact {
            name = "connect-client"
            type = "java-source"
            classifier = "sources"
            extension = "jar"
        }
    }
}

/**
 * Writes the metric registry of the app from the Health Connect SDK.
 *
 * Run it with:
 *
 *     ./gradlew generateMetrics
 *
 * The registry is checked in, so the build does not depend on the task.
 * Run it again after the version of the SDK in the version catalog changes.
 */
val generateMetrics by tasks.registering {
    val outputFile = layout.projectDirectory
        .file("src/main/java/dev/grubfx/healthconnect/HealthMetrics.kt")
    val sources = healthConnectSources

    inputs.files(sources)
    outputs.file(outputFile)

    doLast {
        val sourcesJar = sources.files.single { it.name.endsWith("-sources.jar") }
        val registry = dev.grubfx.healthconnect.generator.HealthMetricsGenerator
            .generate(sourcesJar)
        outputFile.asFile.writeText(registry)

        logger.lifecycle(
            "Wrote ${outputFile.asFile.name} from ${sourcesJar.name}.",
        )
    }
}
