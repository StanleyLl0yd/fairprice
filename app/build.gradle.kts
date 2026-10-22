import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
}

// Signing is opt-in. Debug and unsigned analysis CI never access release secrets.
// The signed job fails closed if any of the four credentials or the keystore is absent.
val signedPreviewMode = providers.gradleProperty("fairprice.signedPreview").orNull == "true"
val signingValue: (String) -> String = { name ->
 providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
  ?: error("Required Android signing input is missing: $name")
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
  versionCode = if (signedPreviewMode) 4 else 1
  versionName = if (signedPreviewMode) "0.1.0-beta.1" else "0.0.1-ocr-probe"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  val instrumentationAbi = providers.gradleProperty("fairprice.ocrTestAbi").orNull
  require(instrumentationAbi == null || instrumentationAbi == "x86_64") {
    "Only x86_64 is permitted as the test-only override"
  }
  ndk { abiFilters += listOf(instrumentationAbi ?: "arm64-v8a") }
 }
 signingConfigs {
  if (signedPreviewMode) {
   create("fairpriceUpload") {
    val path = signingValue("FP_SIGNING_STORE_FILE")
    check(file(path).isFile) { "Android signing keystore file is not available" }
    storeFile = file(path)
    storePassword = signingValue("FP_SIGNING_STORE_PASSWORD")
    keyAlias = signingValue("FP_SIGNING_KEY_ALIAS")
    keyPassword = signingValue("FP_SIGNING_KEY_PASSWORD")
   }
  }
 }
 buildTypes {
  getByName("release") {
   if (signedPreviewMode) {
    signingConfig = signingConfigs.getByName("fairpriceUpload")
   }
   isMinifyEnabled = false
  }
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
 // Stable CameraX line; actual camera capture uses no external storage or network.
 implementation("androidx.camera:camera-core:1.6.2")
 implementation("androidx.camera:camera-camera2:1.6.2")
 implementation("androidx.camera:camera-lifecycle:1.6.2")
 implementation("androidx.camera:camera-view:1.6.2")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation(bom)
 debugImplementation(bom)
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
