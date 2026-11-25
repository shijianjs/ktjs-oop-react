pluginManagement {
    repositories {
        // The docs site is built with the ktjs-docusaurus Gradle plugin. Released versions come from
        // Central; `mavenLocal` is what serves a SNAPSHOT one, which is how the site is developed
        // against an unreleased wrapper. An empty local repository falls through to the next
        // repository, so the order costs nothing in CI.
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }

    // The plugin's version is a catalog entry (`libs.plugins.ktjs.docusaurus` in
    // `gradle/libs.versions.toml`); `docs` applies it with `alias(...)`, so no resolution strategy
    // is needed here.
}

dependencyResolutionManagement {
    repositories {
        // Same reasoning as above: the wrapper klib of a SNAPSHOT version comes from `mavenLocal`.
        mavenLocal()
        // Maven Central is the source of truth for kotlin-wrappers: the Aliyun mirror lags behind
        // and sometimes serves metadata without the corresponding klibs. Gradle does not fall back
        // to the next repository once metadata is found, so pin these modules to Central even when
        // a local mirror is active.
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("org.jetbrains.kotlin-wrappers")
                includeModule("org.jetbrains.kotlin", "kotlin-js-plain-objects")
                // the stdlib half of the same trap: its JS klib only exists on Central
                includeGroup("org.jetbrains.kotlin")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "ktjs-oop-react"

// The published library: the annotations, the runtime that patches prototypes and renders islands,
// and the MobX binding. No npm package is wrapped — the upstream here is `oop-react`, a TypeScript
// library this repository re-implements for Kotlin/JS — so the module carries a hand-written
// semantic version (`semanticModules` in the root build script).
include(":ktjs-oop-react")

// The KSP processor consumers apply to their own module. It turns the annotations (`@OopReactState`,
// `@OopReactRender`, `@OopReactIgnore`, `@OopReactAction`) into the metadata table the runtime reads,
// because Kotlin/JS has no runtime annotation reflection.
include(":ktjs-oop-react-ksp")

// The demo page, published under /demo/ of the Pages site. Not a published module.
include(":demo")

// The documentation site: Kotlin configuration plus Markdown, assembled by the ktjs-docusaurus
// Gradle plugin into a static site that `pagesSite` publishes at the Pages root.
include(":docs")
