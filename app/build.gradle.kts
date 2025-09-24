plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.thutonexofinal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.thutonexofinal"
        minSdk = 23
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.google.android.material:material:1.9.0")

    implementation ("androidx.core:core-ktx:1.17.0")
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("com.google.android.material:material:1.12.0")

    /*    // Firebase Authentication
        implementation ("com.google.firebase:firebase-auth-ktx:22.3.0")

        // Google Sign-In
        implementation ("com.google.firebase:firebase-auth:23.0.0")*/
    // Firebase Firestore
    /*    implementation ("com.google.firebase:firebase-firestore-ktx:24.6.0")*/
    // Google Sign-In
    implementation ("com.google.android.gms:play-services-auth:21.2.0")

    // Circle
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    //
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.2.2"))

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")
}