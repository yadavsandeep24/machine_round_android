// :core: shared infrastructure every feature module depends on. No feature code lives here.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.starter.core"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    // `api`: types that appear in :core's public signatures (Retrofit/OkHttp/Json, Compose, ViewModel, coroutines).
    api(libs.coroutines.android)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.retrofit)
    api(libs.retrofit.serialization)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    api(libs.timber)
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
}
