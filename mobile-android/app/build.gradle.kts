import java.net.URI
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.okul360.hdtcca"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    manifestPlaceholders["cleartextAllowed"] = "false"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val apiEndpoint = providers.gradleProperty("API_BASE_URL").orElse("http://10.0.2.2:8000/api/").get()
    require(!apiEndpoint.contains('"') && !apiEndpoint.contains('\\'))
    buildConfigField("String", "API_BASE_URL", "\"$apiEndpoint\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { manifestPlaceholders["cleartextAllowed"] = "true" }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        // Conscrypt's desktop native-library lookup must not use Turkish casing.
        it.systemProperty("user.language", "en")
        it.systemProperty("user.country", "US")
        it.systemProperty(
          "robolectric.dependency.repo.url",
          "https://maven-central.storage-download.googleapis.com/maven2/"
        )
      }
    }
  }
  if (providers.gradleProperty("nativeBackendAcceptance").orNull == "true") {
    sourceSets.named("test") {
      kotlin.directories += "src/backendAcceptance/java"
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.messaging)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Firebase Auth and Google Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}


// Release builds must never silently ship the emulator URL or debug signing material.
val validateReleaseInputs = tasks.register("validateReleaseInputs") {
  doLast {
    val endpoint = providers.gradleProperty("API_BASE_URL").orNull
      ?: throw GradleException("Release requires an explicit HTTPS API_BASE_URL ending with /api/.")
    val uri = runCatching { URI(endpoint) }.getOrNull()
      ?: throw GradleException("Release API URL is invalid.")
    val host = uri.host.orEmpty().lowercase()
    if (uri.scheme != "https" || host.isBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null ||
      !uri.path.endsWith("/api/") || host in setOf("localhost", "10.0.2.2", "127.0.0.1", "::1") ||
      host.endsWith(".invalid") || host.endsWith(".example") || host == "example.com") {
      throw GradleException("Release requires the real HTTPS API endpoint; local/test URLs are not allowed.")
    }
    val path = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
      ?: throw GradleException("Release requires KEYSTORE_PATH, STORE_PASSWORD and KEY_PASSWORD from the build environment.")
    val keyFile = file(path)
    if (!keyFile.isFile || keyFile.name.contains("debug", ignoreCase = true) ||
      System.getenv("STORE_PASSWORD").isNullOrBlank() || System.getenv("KEY_PASSWORD").isNullOrBlank()) {
      throw GradleException("Release signing material is absent or points to a debug key.")
    }
  }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseInputs) }
