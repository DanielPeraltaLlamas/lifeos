import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
}

// build/agent/lib con el jar del agente y sus dependencias. Algunas se llaman igual
// aunque son de grupos distintos (androidx y org.jetbrains.androidx), así que se
// copian con el grupo delante. scripts/install-agent.sh lo instala.
val buildAgent by tasks.registering {
    dependsOn(tasks.jar)
    val runtime = configurations.runtimeClasspath.get()
    val artifacts = runtime.incoming.artifacts.resolvedArtifacts
    val agentJar = tasks.jar.flatMap { it.archiveFile }
    inputs.files(runtime)
    val out = layout.buildDirectory.dir("agent").get().asFile
    outputs.dir(out)
    doLast {
        out.deleteRecursively()
        val lib = out.resolve("lib").apply { mkdirs() }
        agentJar.get().asFile.copyTo(lib.resolve("linuxAgent.jar"))
        artifacts.get().forEach { a ->
            val group = (a.id.componentIdentifier as? ModuleComponentIdentifier)?.group ?: "local"
            a.file.copyTo(lib.resolve("${group}__${a.file.name}"), overwrite = true)
        }
    }
}
