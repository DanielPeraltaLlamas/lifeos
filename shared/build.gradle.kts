import java.util.Properties

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
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
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

// Configuración de Supabase: se lee de local.properties (fuera de git) y se
// genera un objeto Kotlin. Sin esas claves la app funciona igual, sin sincronizar.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val configDir = layout.buildDirectory.dir("generated/lifeosConfig/kotlin")
val generateSupabaseConfig by tasks.registering {
    val url = localProps.getProperty("supabase.url", "").trim()
    val key = localProps.getProperty("supabase.key", "").trim()
    inputs.property("url", url)
    inputs.property("key", key)
    val outDir = configDir.get().asFile
    outputs.dir(outDir)
    doLast {
        val file = outDir.resolve("dev/lifeos/sync/SupabaseConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            "package dev.lifeos.sync\n\n" +
                "/** Generado desde local.properties al compilar. No editar. */\n" +
                "internal object SupabaseConfig {\n" +
                "    const val URL = \"$url\"\n" +
                "    const val KEY = \"$key\"\n" +
                "}\n",
        )
    }
}
kotlin.sourceSets.commonMain { kotlin.srcDir(generateSupabaseConfig) }

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
