plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.lostnet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lostnet"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Pruebas unitarias en la JVM: los métodos de Android devuelven valores por defecto
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // El pipeline publica el reporte de lint como artefacto sin detener el build
    lint {
        abortOnError = false
        htmlReport = true
        xmlReport = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    // Servidor HTTP falso para las pruebas de integración de la API (misma versión de OkHttp que usa Retrofit 2.9.0)
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.9")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.firebase:firebase-messaging")

        implementation ("androidx.appcompat:appcompat:1.6.1")
        implementation ("com.google.android.material:material:1.9.0")
        implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

        // Google Services & Maps
        implementation ("com.google.android.gms:play-services-maps:18.1.0")
        implementation ("com.google.android.gms:play-services-location:21.0.1")
        implementation ("com.google.android.gms:play-services-auth:20.7.0")

        // Retrofit & Gson
        implementation ("com.squareup.retrofit2:retrofit:2.9.0")
        implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

        // Glide (Opcional, para cargar imágenes de reportes)
        implementation ("com.github.bumptech.glide:glide:4.15.1")
        // Librería para cargar imágenes (Glide)
        implementation("com.github.bumptech.glide:glide:4.16.0")
}