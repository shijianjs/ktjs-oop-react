plugins {
    kotlin("jvm")
}

// Ships alongside the library it generates code for: a consumer's build uses the same version of
// both, so the metadata contract cannot drift.
version = "0.1.0-SNAPSHOT"
description = "KSP processor that turns oop-react annotations into the metadata table the runtime reads."

kotlin {
    compilerOptions {
        // The processor is a build-time tool; it is never on an application's runtime classpath.
        freeCompilerArgs.add("-Xjdk-release=21")
    }

    jvmToolchain(21)
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.14")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
