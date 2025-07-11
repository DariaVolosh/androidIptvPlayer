plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
    id("de.mannodermaus.android-junit5")
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(rootProject.extra["myValue1"] as String)
            keyAlias = "key0"
            storePassword = rootProject.extra["myValue"] as String
            keyPassword = rootProject.extra["myValue"] as String
        }
    }

    sourceSets {
        named("main") {
            java.srcDirs("../ijkplayer-java/src/main/java")
            jniLibs.srcDirs("../ijkplayer-java/jniLibs")
        }
    }
    namespace = "com.example.iptvplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.iptvplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        //isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":ijkplayer-java"))
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.ui.test.junit4.android)
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)
    kapt("com.google.dagger:hilt-compiler:2.55")
    implementation("com.google.dagger:hilt-android:2.55")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.13.1")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    //implementation(libs.coil.compose)
    //implementation("io.coil-kt.coil3:coil-network:3.0.0-alpha02")
    //implementation("io.ktor:ktor-client-android:3.1.1")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.github.instacart:truetime-android:3.5")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.activity:activity-ktx:1.3.1")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.tracing:tracing-perfetto:1.0.0") // Or the latest version
    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation("androidx.tv:tv-material:1.1.0-alpha01")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("androidx.datastore:datastore-preferences-core:1.1.6")
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0-alpha01") // Or the latest stable version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0-alpha01") // Also often needed for viewModels()
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") // Use the latest version
}