import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release signing credentials: keystore.properties (git-ignored, local dev -
// see .gitignore) takes priority; RELEASE_STORE_FILE/RELEASE_STORE_PASSWORD/
// RELEASE_KEY_ALIAS/RELEASE_KEY_PASSWORD env vars are the fallback, so CI can
// supply secrets without any checked-in file. If neither source is complete,
// the release build type is left unsigned - assembleRelease still succeeds
// (useful for a shrink/lint-only build) but packageRelease/the installer
// will refuse an unsigned APK, which is a clear failure rather than a silent
// one.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun releaseSigningProperty(propertiesKey: String, envVar: String): String? =
    keystoreProperties.getProperty(propertiesKey) ?: System.getenv(envVar)

val releaseStoreFile = releaseSigningProperty("storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningProperty("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningProperty("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningProperty("keyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.daycarelog.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.daycarelog.app"
        minSdk = 24
        targetSdk = 34
        // Bump both for every release: versionCode must strictly increase
        // (Android uses it to decide whether an install is an update over
        // the last one), versionName is the human-readable string shown to
        // users/in Settings.
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    // Kotlin 1.9.22 predates the Kotlin 2.0 Compose compiler Gradle plugin, so the
    // compiler extension is pinned here directly instead (1.5.8-1.5.10 all pair with
    // Kotlin 1.9.22 per the official compose-kotlin compatibility map).
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    // Retrofit 2.9.0 pulls in an older transitive Gson that predates the static
    // JsonParser.parseString(...) API used in AuthViewModel - pin a modern one directly.
    implementation("com.google.code.gson:gson:2.10.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
