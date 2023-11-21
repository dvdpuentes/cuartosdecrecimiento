plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.arduinoconnector"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.arduinoconnector"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}


dependencies {

    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.20")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.6.0")
    implementation("androidx.navigation:navigation-ui:2.6.0")
    implementation ("com.github.felHR85:UsbSerial:6.1.0")
    //implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    //implementation ("com.cardiomood.android:android-widgets:0.1.1")
    implementation ("com.github.anastr:speedviewlib:1.6.1")
    implementation ("androidx.core:core:1.9.0")  // Reemplaza 1.x.x con la versión actual
    implementation ("org.nanohttpd:nanohttpd:2.3.1")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("org.bouncycastle:bcpkix-jdk15on:1.68")



    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}