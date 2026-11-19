import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "dev.lifeos.desktop.MainKt"
        // Algunos componentes (SQLite) usan módulos de Java que no entran por defecto.
        jvmArgs("-Xmx384m")
        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "lifeos"
            packageVersion = "1.0.0"
            modules("java.instrument", "java.management", "java.sql", "jdk.unsupported")
            linux {
                iconFile.set(project.file("icons/lifeos.png"))
            }
        }
    }
}
