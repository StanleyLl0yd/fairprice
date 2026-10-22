import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.sl.fairprice"
 compileSdk {
  version = release(37) { minorApiLevel = 0 }
 }
 defaultConfig {
  applicationId = "com.sl.fairprice"
  minSdk = 26
  targetSdk = 36
  versionCode = 1
  versionName = "0.0.1-ocr-probe"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  ndk { abiFilters += listOf("arm64-v8a") }
 }
 buildFeatures { compose = true }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
 val bom = platform("androidx.compose:compose-bom:2026.09.00")
 implementation(bom)
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
