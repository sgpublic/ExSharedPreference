plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")

    kotlin("plugin.lombok") version "1.7.10"
    id("io.freefair.lombok") version "5.3.0"
}

android {
    namespace = "io.github.sgpublic.exsp.demo"
    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.sgpublic.exsp.demo"
        minSdk = 16
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        all {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kapt {
    keepJavacAnnotationProcessors = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")

    implementation(project(":runtime"))
    kapt(project(":compiler"))
    val lombokVer = "1.18.24"
    compileOnly("org.projectlombok:lombok:$lombokVer")
    annotationProcessor("org.projectlombok:lombok:$lombokVer")
}