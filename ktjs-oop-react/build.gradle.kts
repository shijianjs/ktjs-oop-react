import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

// No npm package is wrapped here: this module re-implements the TypeScript `oop-react` library, so
// it carries a semantic version of its own (see `semanticModules` in the root build script).
version = "0.1.0-SNAPSHOT"
description = "Object-oriented UI for Kotlin/JS React: state and behaviour live in a plain class, " +
    "and only methods annotated with @OopReactRender take part in re-rendering."

@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>

kotlin {
    js {
        useEsModules()

        compilerOptions {
            // ES2015 is the first target whose output can express a real class, which both React
            // class components (the error boundary) and prototype patching need.
            // Kotlin/JS accepts es5/es2015 only (es2020 fails).
            target = "es2015"
        }

        // Islands are rendered into a real DOM, so the tests run in Chromium (Playwright) instead of
        // Node. A Node-only module would use `nodejs()` and `jsNodeTest`.
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }

        binaries.library()
    }

    compilerOptions {
        // Kotlin/JS dead-code-eliminates a library's JS output down to the `@JsExport` roots, and a
        // library module has none: the whole module would compile to an empty file, so any consumer
        // that imports it at runtime gets `undefined`. Keep the public API; a consumer's bundler
        // still shakes whatever it does not use. See the DCE note in AGENTS.md.
        freeCompilerArgs.add("-Xir-dce=false")
    }

    sourceSets {
        jsMain.dependencies {
            // The React binding is the official one. `kotlin-react` already declares npm `react`, and
            // `kotlin-react-dom` declares `react-dom`, so the versions travel with the klibs.
            api(libs.kotlin.react)
            api(libs.kotlin.react.dom)

            // The public surface uses kotlin-browser types (`web.*`) wherever DOM types appear.
            api(libs.kotlin.browser)

            // Reactivity is MobX itself — no abstraction layer on top. The React binding is
            // `mobx-react-lite`'s `observer`, the same component the TypeScript original uses.
            implementation(npm("mobx", npmVersions.getValue("mobx")))
            implementation(npm("mobx-react-lite", npmVersions.getValue("mobx-react-lite")))
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
            // Testing Library drives the tests through the DOM the way a user would; it is the
            // Kotlin/JS port of the tool the TypeScript original tests with.
            implementation(libs.kotlin.testing.library.react)
            implementation(libs.kotlin.testing.library.dom)
        }
    }
}
