import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import dev.detekt.gradle.extensions.DetektExtension
import groovy.json.JsonSlurper
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    // Consumers that annotate state classes apply this plugin and depend on `ktjs-oop-react-ksp`;
    // `demo` in this repository is the reference consumer.
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka)
}

// A working tree is never a release. Every module derives the version of its *next* release, which
// Maven Central does not have, so the suffix marks it as unreleased: consumers can tell an artifact
// built here from one that was published, and a local publish can never shadow a real version.
// The release workflow passes `-Pktjs.release=true`, which drops the suffix for both the build and
// `printPublishedVersions`. Nothing else reads the flag.
val versionSuffix =
    if (providers.gradleProperty("ktjs.release").map(String::toBoolean).getOrElse(false)) "" else "-SNAPSHOT"
extra["versionSuffix"] = versionSuffix

// Every published library carries this prefix, which is also how publishing, Dokka and the version
// report find them. Keep it in sync with the module directories and settings.gradle.kts. The prefix
// deliberately has no trailing dash: it is what the processor module is named after too.
val publishedPrefix = "ktjs-oop-react"

// Libraries that wrap no npm package have no upstream version to follow, so they carry a
// hand-written semantic version in their own build script instead of `{npm version}-kt.{revision}`.
// This repository re-implements `oop-react` for Kotlin/JS rather than wrapping an npm package, so
// both of its artifacts belong here.
val semanticModules = setOf("ktjs-oop-react", "ktjs-oop-react-ksp")

fun isPublished(module: String): Boolean = module.startsWith(publishedPrefix)

// Dokka aggregates every published module into one API reference. `demo` and `docs` are not part of
// the published API surface, so they are excluded by the prefix filter.
dependencies {
    subprojects
        .filter { isPublished(it.name) && it.name != "${publishedPrefix}-ksp" }
        .forEach { dokka(project(it.path)) }
}

dokka {
    dokkaPublications.html {
        moduleName.set("ktjs-oop-react")
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

// GitHub-backed namespace; verify it once in the Central Portal. All modules share it.
// Setting the root `group` alone does not propagate to subprojects.
val publishedGroup = "io.github.shijianjs"

// npm package versions live in package.json so that Dependabot can update them.
// Subprojects read this map to declare npm(...) dependencies.
@Suppress("UNCHECKED_CAST")
val npmVersions: Map<String, String> = run {
    val manifest = JsonSlurper().parse(file("package.json")) as Map<*, *>
    @Suppress("UNCHECKED_CAST")
    val dependencies = manifest["dependencies"] as Map<String, Any?>
    dependencies.mapValues { it.value.toString() }
}
extra["npmVersions"] = npmVersions

// Every module here carries a semantic version, so the revision table only has to exist for the
// root script to read; a future npm-backed module would add its entry here.
@Suppress("UNCHECKED_CAST")
val wrapperRevisions: Map<String, Int> = run {
    val properties = java.util.Properties()
    val file = file("wrapper-revisions.properties")
    if (file.exists()) {
        file.inputStream().use { properties.load(it) }
    }
    properties.stringPropertyNames().associate { name ->
        name to properties.getProperty(name).trim().toInt()
    }
}
extra["wrapperRevisions"] = wrapperRevisions

// Keep a single webpack instance across the npm workspace: npm otherwise installs a second copy
// for source-map-loader's peer dependency, and the test bundle fails with
// "The 'compilation' argument must be an instance of Compilation".
val webpackVersion = libs.versions.webpack.get()

plugins.withType<NodeJsRootPlugin> {
    extensions.getByType(NpmExtension::class.java).override("webpack", webpackVersion)
}

// The public API must use kotlin-browser's web.* types. Kotlin/JS's own org.w3c.dom.* and
// kotlinx.browser.* compile and run against the same objects, but the compiler treats them as
// unrelated types, which breaks callers that use kotlin-browser. detekt.yml forbids the imports.
subprojects {
    apply(plugin = "dev.detekt")

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        // The plain detekt task is wired into check; point it at the multiplatform source tree.
        source.setFrom(files("src"))
    }

    // Only published modules are documented; `demo` is the GitHub Pages site and the KSP processor
    // is a build-time tool with no API of its own.
    if (isPublished(name) && name != "${publishedPrefix}-ksp") {
        apply(plugin = "org.jetbrains.dokka")
    }
}

// Publishing is configured once for every module so that coordinates, POM metadata, signing and
// the Maven Central target cannot drift apart.
//
// klibs.io discovers libraries automatically: it reads the POM (url / scm.url must point at the
// GitHub repository) and requires the kotlin-tooling-metadata.json produced by the Kotlin
// Multiplatform plugin. Keep both intact when changing this block.
subprojects {
    if (!isPublished(name)) return@subprojects

    if (name !in semanticModules) {
        check(wrapperRevisions.containsKey(name)) {
            "Missing '$name' in wrapper-revisions.properties"
        }
    }

    val module = this

    group = publishedGroup

    apply(plugin = "com.vanniktech.maven.publish")

    // The subproject's Kotlin Multiplatform plugin is applied after this root script runs.
    // Choosing the javadoc jar earlier would hit the "no compatible plugin" branch and the
    // plugin's afterEvaluate default (Dokka HTML) would win.
    listOf("org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.jvm").forEach { pluginId ->
        plugins.withId(pluginId) {
            extensions.configure<MavenPublishBaseExtension> {
                // Maven Central requires a javadoc jar, but Dokka's HTML output duplicates its static
                // assets into every module's jar (~1.1 MB each). The API reference is deployed to the
                // Pages site, so publish the required empty jar instead of the HTML copy.
                configureBasedOnAppliedPlugins(javadocJar = JavadocJar.Empty())
            }
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral(automaticRelease = true)
        signAllPublications()

        // Gradle writes .md5/.sha1 next to every .asc signature. Maven Central does not need
        // them and they count toward the monthly file quota (KTC-5453, gradle/gradle#20232).
        excludeSignatureChecksums()

        pom {
            name.set(module.name)
            description.set(
                module.provider {
                    module.description ?: "Object-oriented UI for Kotlin/JS React."
                }
            )
            url.set("https://github.com/shijianjs/ktjs-oop-react")
            inceptionYear.set("2026")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/shijianjs/ktjs-oop-react/blob/master/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("shijianjs")
                    name.set("shijianjs")
                    url.set("https://github.com/shijianjs")
                }
            }

            scm {
                url.set("https://github.com/shijianjs/ktjs-oop-react")
                connection.set("scm:git:git://github.com/shijianjs/ktjs-oop-react.git")
                developerConnection.set("scm:git:ssh://git@github.com/shijianjs/ktjs-oop-react.git")
            }
        }
    }

    // A plain file repository produces the Maven repository layout — POMs, artifacts, sources,
    // javadoc and the `.asc` signatures — without contacting Central. The release workflow packages
    // one archive per coordinate from it and registers them with the hub.
    // Task name: publishAllPublicationsToHubStagingRepository
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "hubStaging"
                url = module.layout.buildDirectory.dir("hub-staging").get().asFile.toURI()
            }
        }
    }
}

// Prints "<module>=<version>" for every publishable module. Use it to see which versions a release
// would upload before running publishToMavenCentral, so version derivation stays in the module
// scripts rather than being duplicated in shell.
tasks.register("printPublishedVersions") {
    val publishedModules = subprojects.filter { isPublished(it.name) }
    doLast {
        publishedModules
            .associate { it.name to it.version.toString() }
            .toSortedMap()
            .forEach { (name, version) -> println("$name=$version") }
    }
}

// Assembles the GitHub Pages site: the documentation at the root, the demo under /demo/ and the API
// reference under /api/. The docs site's `baseUrl` and the two links it builds from it (in Config.kt)
// have to mirror this layout.
tasks.register<Sync>("pagesSite") {
    dependsOn(":demo:jsBrowserDistribution", "dokkaGeneratePublicationHtml")
    into(layout.buildDirectory.dir("pages"))
    from(project(":demo").layout.buildDirectory.dir("dist/js/productionExecutable")) {
        into("demo")
    }
    from(layout.buildDirectory.dir("dokka/html")) {
        into("api")
    }
}
