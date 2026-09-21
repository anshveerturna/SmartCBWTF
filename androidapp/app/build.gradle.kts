import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
}

fun normalizedApiBaseUrl(defaultValue: String, requireHttps: Boolean): String {
    val configured = (project.findProperty("API_BASE_URL") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: defaultValue
    val normalized = if (configured.endsWith("/")) configured else "$configured/"
    val uri = URI(normalized)

    require(!uri.host.isNullOrBlank()) {
        "API_BASE_URL must be an absolute URL, for example https://api.smartcbwtf.com/api/"
    }
    require(normalized.endsWith("/api/")) {
        "API_BASE_URL must include the /api/ path and trailing slash, got: $normalized"
    }
    require(uri.scheme == "https" || (!requireHttps && uri.scheme == "http")) {
        "Release API_BASE_URL must use HTTPS, got: $normalized"
    }

    return normalized
}

android {
    namespace = "com.smartcbwtf.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartcbwtf.mobile"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = normalizedApiBaseUrl("https://api.smartcbwtf.com/api/", requireHttps = true)
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        debug {
            // For physical device, use production API. For emulator, pass -PAPI_BASE_URL=http://10.0.2.2:8080/api/
            val apiBaseUrl = normalizedApiBaseUrl("http://10.0.2.2:8080/api/", requireHttps = false)
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val apiBaseUrl = normalizedApiBaseUrl("https://api.smartcbwtf.com/api/", requireHttps = true)
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    val navVersion = "2.7.7"
    val roomVersion = "2.6.1"
    val hiltVersion = "2.51.1"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // CameraX
    val cameraXVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.4.0")

    // Coil for image loading
    implementation("io.coil-kt:coil:2.6.0")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
