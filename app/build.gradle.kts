import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.google.devtools.ksp)
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 35
    namespace = "code.name.monkey.retromusic"

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        vectorDrawables {
            useSupportLibrary = true
        }

        applicationId = namespace
        versionCode = 10660
        versionName = "6.6.0"

        buildConfigField("String", "GOOGLE_PLAY_LICENSING_KEY", "\"${getProperty(getProperties("../public.properties"), "GOOGLE_PLAY_LICENSE_KEY")}\"")
        val localProperties = getProperties("local.properties")
        val defaultRefreshToken = "eyJraWQiOiJoUzFKYTdVMCIsImFsZyI6IkVTNTEyIn0.eyJ0eXBlIjoibzJfcmVmcmVzaCIsInVpZCI6MjA0MTg4NTU1LCJzY29wZSI6IndfdXNyIHJfdXNyIHdfc3ViIiwiY2lkIjoxMzMxOSwic1ZlciI6MSwiZ1ZlciI6MCwiaXNzIjoiaHR0cHM6Ly9hdXRoLnRpZGFsLmNvbS92MSJ9.ALlkbro7NIpyKNrtjCrh2_lqrxJIMUURSzLCi3KlqY7MTwAV9VO7-O4qbzog8AekvHKFf4l0HWgqD8OJk-YKlS_yAeBdhtxuY8bv_SdAcYdptgXOwYecdgGqIlPdTEobsgbyQ-105AN5Tu24MP8DG7qGgd24kzEmN2fQ5Jfs6A5w8LgH"
        val defaultClientId = "fX2JxdmntZWK0ixT"
        val defaultClientSecret = "1Nn9AfDAjxrgJFJbKNWLeAyKGVGmINuXPPLHVXAvxAg="
        buildConfigField("String", "TIDAL_REFRESH_TOKEN", buildConfigString(getProperty(localProperties, "TIDAL_REFRESH_TOKEN", defaultRefreshToken)))
        buildConfigField("String", "TIDAL_CLIENT_ID", buildConfigString(getProperty(localProperties, "TIDAL_CLIENT_ID", defaultClientId)))
        buildConfigField("String", "TIDAL_CLIENT_SECRET", buildConfigString(getProperty(localProperties, "TIDAL_CLIENT_SECRET", defaultClientSecret)))
        
        // Supabase configuration
        val defaultSupabaseUrl = "https://example.supabase.co"
        val defaultSupabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_key"
        buildConfigField("String", "SUPABASE_URL", buildConfigString(getProperty(localProperties, "SUPABASE_URL", defaultSupabaseUrl)))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(getProperty(localProperties, "SUPABASE_ANON_KEY", defaultSupabaseKey)))
    }
    val signingProperties = getProperties("retro.properties")
    val theSigningConfig = if (signingProperties != null) {
        signingConfigs.create("release") {
            storeFile = file(getProperty(signingProperties, "storeFile"))
            keyAlias = getProperty(signingProperties, "keyAlias")
            storePassword = getProperty(signingProperties, "storePassword")
            keyPassword = getProperty(signingProperties, "keyPassword")
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = theSigningConfig
        }
        getByName("debug") {
            signingConfig = theSigningConfig
            applicationIdSuffix = ".debug"
            versionNameSuffix = " DEBUG"
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("normal") {
            dimension = "version"
        }
        create("fdroid") {
            dimension = "version"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Make sure this matches Kotlin version
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/java.properties"
            )
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        warning.addAll(listOf("ImpliedQuantity", "Instantiatable", "MissingQuantity", "MissingTranslation", "StringFormatInvalid"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    configurations.configureEach {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:1.3.9")
    }
}


dependencies {
    implementation(project(":appthemehelper"))
    implementation(libs.gridLayout)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.androidx.mediarouter)
    //Cast Dependencies
    "normalImplementation"(libs.google.play.services.cast.framework)
    //WebServer by NanoHttpd
    "normalImplementation"(libs.nanohttpd)

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common.java8)

    implementation(libs.androidx.core.splashscreen)

    "normalImplementation"(libs.google.feature.delivery)
    "normalImplementation"(libs.google.play.review)
    "normalImplementation"(libs.google.play.billing)


            implementation(libs.android.material)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.afollestad.material.dialogs.core)
    implementation(libs.afollestad.material.dialogs.input)
    implementation(libs.afollestad.material.dialogs.color)
    implementation(libs.afollestad.material.cab)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)

    implementation(libs.advrecyclerview)

    implementation(libs.fadingedgelayout)

    implementation(libs.keyboardvisibilityevent)
    implementation(libs.jetradarmobile.android.snowfall)

    implementation(libs.chrisbanes.insetter)


    implementation(libs.org.eclipse.egit.github.core)
    implementation(libs.jaudiotagger)
    implementation(libs.slidableactivity)
    implementation(libs.material.intro)
    implementation(libs.fastscroll.library)
    implementation(libs.customactivityoncrash)
    implementation(libs.tankery.circularSeekBar)

    implementation(libs.androidx.exoplayer)

    // Web Scraping
    implementation("org.jsoup:jsoup:1.17.2")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    
    // Compose Interop
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")

    // Coil for Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Palette
    implementation("androidx.palette:palette-ktx:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

fun getProperties(fileName: String): Properties? {
    val properties = Properties()
    val file = rootProject.file(fileName)
    if (file.exists()) {
        file.inputStream().use { properties.load(it) }
    } else {
        return null
    }
    return properties
}

fun getProperty(properties: Properties?, name: String, defaultValue: String = "$name missing"): String =
    properties?.getProperty(name) ?: defaultValue

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
