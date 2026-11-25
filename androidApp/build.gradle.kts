import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// The API key is configuration, not code: `-Pflix.apiKey=…`, `flix.apiKey=…` in local.properties, or the
// FLIX_API_KEY environment variable (CI). The challenge's public key is the fallback so a fresh clone just builds.
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun config(
    name: String,
    env: String,
    default: String,
): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: providers.environmentVariable(env).orNull
        ?: default

val apiBaseUrl = config("flix.apiBaseUrl", "FLIX_API_BASE_URL", "https://global.api.flixbus.com/")
val apiKey = config("flix.apiKey", "FLIX_API_KEY", "e8d85577-2fb3-4d44-8091-ffb3da46e9ce")
val stationId = config("flix.stationId", "FLIX_STATION_ID", "dcbaaee6-9603-11e6-9066-549f350fcb0c")

android {
    namespace = "com.flix.timetable.android"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.flix.timetable"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "STATION_ID", "\"$stationId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No release keystore in a coding challenge: sign with the debug key so the release build is installable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}
