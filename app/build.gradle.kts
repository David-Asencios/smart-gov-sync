import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

val mapProperties = Properties().apply {
    listOf("local.defaults.properties", "secrets.properties").forEach { fileName ->
        rootProject.file(fileName).takeIf { it.isFile }?.inputStream()?.use { stream ->
            load(stream)
        }
    }
}
val mapsApiKey = providers.gradleProperty("MAPS_API_KEY").orNull
    ?: System.getenv("MAPS_API_KEY")
    ?: mapProperties.getProperty("MAPS_API_KEY")
    ?: "DEFAULT_API_KEY"

android {
    namespace = "com.example.tarea16"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tarea16"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Permite sincronizar/compilar el proyecto aunque la clave local aun no
        // exista, y usa la clave real cuando se configura localmente o en CI.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.maps)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
