plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.feemag.shop"
 compileSdk = 37
 defaultConfig {
  applicationId = "com.feemag.shop"
  minSdk = 36
  targetSdk = 37
  versionCode = 1
  versionName = "1.0"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 buildFeatures { compose = true }
}
ksp { arg("appfunctions:aggregateAppFunctions", "true") }
dependencies {
 implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10")
 ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation(platform("androidx.compose:compose-bom:2026.06.01"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
 testImplementation("junit:junit:4.13.2")
}
