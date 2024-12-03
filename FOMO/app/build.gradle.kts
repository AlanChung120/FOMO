plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  kotlin("plugin.serialization") version "2.0.0"

  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
  namespace = "com.example.fomo"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.example.fomo"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "0.30"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions { jvmTarget = "1.8" }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

secrets {
  // properties file containing secrets
  propertiesFileName = "secrets.properties"
  // properties file containing default secret values (when actual secret doesn't exist)
  defaultPropertiesFileName = "local.defaults.properties"
  // Configure which keys should be ignored by the plugin by providing regular expressions.
  // "sdk.dir" is ignored by default.
  ignoreList.add("keyToIgnore") // Ignore the key "keyToIgnore"
  ignoreList.add("sdk.*")       // Ignore all keys matching the regexp "sdk.*"
}

dependencies {

  implementation(libs.core.ktx)
  implementation(libs.androidx.ui.test.junit4.android)
  testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  // JUnit for basic testing
  testImplementation ("junit:junit:4.13.2")

// Kotlin Test Helpers
  testImplementation ("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")

// Mocking Frameworks
  testImplementation ("org.mockito:mockito-core:5.5.0")
  testImplementation ("org.mockito.kotlin:mockito-kotlin:5.0.0")
  testImplementation ("io.mockk:mockk:1.13.7")


  //database
  implementation(platform("io.github.jan-tennert.supabase:bom:3.0.1"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:auth-kt") // For Authentication
  implementation("io.github.jan-tennert.supabase:storage-kt:3.0.2")
  implementation("io.ktor:ktor-client-android:3.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
  implementation ("io.coil-kt:coil-compose:2.5.0")

  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.voyager.navigator)

  // Google Maps
  implementation(libs.places)
  implementation(libs.play.services.maps)
  implementation(libs.google.maps.compose)
  implementation(libs.google.maps.compose.utils)
  implementation(libs.google.maps.compose.widgets)
  implementation(libs.android.maps.utils)
  implementation(libs.firebase.firestore.ktx)

  // KTOR (API)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio) // or ktor-client-okhttp for Android
  implementation(libs.ktor.client.serialization)
  implementation(libs.ktor.serialization.kotlinx.json)

  // Icons
  implementation(libs.material.icons.core)
  implementation(libs.material.icons.extended)

  // General
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.play.services.location)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
  implementation(libs.voyager.navigator)

}
