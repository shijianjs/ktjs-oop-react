import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
    // The demo is the reference consumer: it applies the processor to its own sources, which is
    // where the annotated state classes live.
    id("com.google.devtools.ksp")
}

@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>

// The brand image lives with the site's static files and is copied in here, so the demo and the
// documentation cannot drift apart. `srcDir(task)` only contributes the directory, not the
// dependency, hence the explicit wiring below.
val brandAssets = tasks.register<Copy>("brandAssets") {
    from(rootProject.file("docs/src/jsMain/resources/static/img")) {
        // Keep the same `img/` layout the site uses, so both pages reference `img/<logo>.png`.
        into("img")
    }
    into(layout.buildDirectory.dir("generated/brand"))
}

kotlin {
    js {
        useEsModules()

        // The demo is a standalone page, so the JS target needs an executable webpack bundle.
        binaries.executable()
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }
    }

    // The JVM target carries only the Playwright end-to-end suite in `src/jvmTest`; the demo itself
    // stays a JS page.
    jvm()

    sourceSets {
        jsMain {
            resources.srcDir(brandAssets)
        }
        jsMain.dependencies {
            implementation(project(":ktjs-oop-react"))

            // The UI kit the demo is written against: the official wrapper for MUI. Emotion is its
            // styling engine — MUI declares it as a peer, so the npm versions come from package.json.
            implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-material:2026.11.2-9.5.0")
            implementation(npm("@emotion/react", npmVersions.getValue("@emotion/react")))
            implementation(npm("@emotion/styled", npmVersions.getValue("@emotion/styled")))
        }

        jvmTest.dependencies {
            implementation("com.microsoft.playwright:playwright:1.61.0")
            implementation("cn.hutool:hutool-all:5.8.48")
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
            implementation("org.junit-pioneer:junit-pioneer:2.3.0")
            implementation(kotlin("test-junit5"))
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
        }
    }
}

dependencies {
    // One line, and every `@OopReactState` / `@OopReactRender` class in this module gets patched at
    // startup. See `Main.kt` for the `initializeOopReact()` call it generates.
    add("kspJs", project(":ktjs-oop-react-ksp"))
}

// `srcDir(task)` contributes the directory but not the task dependency; resource processing is what
// reads that directory, so the edge belongs there.
tasks.named("jsProcessResources") {
    dependsOn(brandAssets)
}

// ---------------------------------------------------------------------------------------------
// End-to-end suite
//
// The suite lives in `src/jvmTest`, so the demo stays one KMP module instead of carrying a second
// Gradle project. It uses Playwright's official JUnit 5 integration plus Hutool's SimpleServer for
// the static bundle, and reuses the Chromium that `kotlinInstallPlaywrightBrowsers` installs for
// the Kotlin/JS browser tests (hence the pinned Playwright version in the dependencies).
// `jvmTest` itself is disabled to keep `build`/`check` browser-free; this dedicated task runs the
// same test compilation on demand.
// ---------------------------------------------------------------------------------------------
val jvmE2eCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Runs the Kotlin/JVM Playwright suite against the bundled demo page."
    useJUnitPlatform()
    dependsOn("jsBrowserDistribution")
    dependsOn(":ktjs-oop-react:kotlinInstallPlaywrightBrowsers")

    testClassesDirs = jvmE2eCompilation.output.classesDirs
    classpath = files(jvmE2eCompilation.output.allOutputs, jvmE2eCompilation.runtimeDependencyFiles)
    systemProperty(
        "demo.dist",
        layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile.absolutePath,
    )
    systemProperty("e2e.output", layout.buildDirectory.dir("e2e-results").get().asFile.absolutePath)
    systemProperty("junit.jupiter.execution.timeout.default", "30 s")

    outputs.upToDateWhen { false }
}

tasks.named<Test>("jvmTest") { enabled = false }
