import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The release key's location and passwords, kept out of git (see design/README.md).
// Without this file, as on CI, release builds come out unsigned.
val keystoreFile: File = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use(::load) }

android {
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric needs the app's resources (strings, the Figtree font) to draw screens.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // DemoFrames renders the README animations only when given a folder:
            // ./gradlew :app:testDebugUnitTest --tests '*DemoFrames*' -Pdawnly.frames=<folder>
            providers.gradleProperty("dawnly.frames").orNull?.let {
                test.systemProperty("dawnly.frames", it)
                test.maxParallelForks = 2 // the light and dark clips render side by side
            }
            test.maxHeapSize = "2g"
            // Robolectric reaches into this JDK module; newer JDKs need it opened explicitly.
            test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }
    namespace = "com.dawnly.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aryanlanke.dawnly"
        minSdk = 26
        targetSdk = 37
        versionCode = 26
        versionName = "1.7.8"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Android's org.json is a stub on the PC; this is the same API, for the reminder-store tests.
    testImplementation("org.json:json:20240303")
    // Draws real screens on the PC, frame by frame, for the README animations (DemoFrames).
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
