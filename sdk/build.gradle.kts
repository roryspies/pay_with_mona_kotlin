import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

android {
    namespace = "ng.mona.paywithmona"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "SUB_DOMAIN", "\"development\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "SUB_DOMAIN", "\"staging\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "SUB_DOMAIN", "\"production\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
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

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    publishing {
        singleVariant("devRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("stagingRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("productionRelease") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarjdklibs)

    implementation(libs.androidx.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.palette)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material3)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Biometric
    implementation(libs.androidx.biometric)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.coil.svg)

    // Custom Tabs
    implementation(libs.androidx.browser)

    // DataStore
    implementation(libs.datastore)

    // Kotlin
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.kotlin.serialization)

    // Ktor
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.core)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.serialization)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)

    // Timber
    implementation(libs.timber)
}

publishing {
    publications {
        create<MavenPublication>("devRelease") {
            groupId = "ng.mona"
            artifactId = "paywithmona-dev"
            version = "1.0.0"

            afterEvaluate {
                from(components["devRelease"])
            }
        }
        create<MavenPublication>("stagingRelease") {
            groupId = "ng.mona"
            artifactId = "paywithmona-staging"
            version = "1.0.0"

            afterEvaluate {
                from(components["stagingRelease"])
            }
        }
        create<MavenPublication>("productionRelease") {
            groupId = "ng.mona"
            artifactId = "paywithmona"
            version = "1.0.0"

            afterEvaluate {
                from(components["productionRelease"])
            }
        }
    }
}
