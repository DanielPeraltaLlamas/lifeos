plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }

    android {
        namespace = "dev.lifeos.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            api(libs.compose.material.icons)
            implementation(libs.compose.components.resources)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

sqldelight {
    databases {
        create("LifeOsDatabase") {
            packageName.set("dev.lifeos.db")
        }
    }
}

compose.resources {
    packageOfResClass = "dev.lifeos.resources"
}
