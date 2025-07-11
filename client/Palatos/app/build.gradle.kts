plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Use the Google Services plugin here
}

android {
    namespace = "com.example.PalaTos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.PalaTos"
        minSdk = 28 // Matches the minimum required by car-ui-lib
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true // Enables View Binding
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
        sourceCompatibility = JavaVersion.VERSION_17 // Matches Java 17 compatibility
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17" // Matches Java 17
    }
}

dependencies {
    // Core Android Libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Firebase Libraries
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Image Slideshow
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")

    // Android Car UI Library (requires minSdk 28)
    implementation("com.android.car.ui:car-ui-lib:2.6.0")

    // CardView for UI Components
    implementation("androidx.cardview:cardview:1.0.0")

    // RecyclerView for displaying lists
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

