package io.github.shijianjs.ktjs.oopreact.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Collects `oop-react` annotations into one generated file:
 *
 * ```kotlin
 * fun initializeOopReact() {
 *     oopReactRegister(
 *         oopReactMeta(Cart::class.js) {
 *             state()
 *             render("view")
 *         },
 *     )
 * }
 * ```
 *
 * The library needs this table because Kotlin/JS has no runtime annotation reflection: annotations
 * are a compile-time concept, and the runtime discovers members by name. Producing the table at build
 * time is what lets the library stay framework-free at runtime — one generated function, called once,
 * and no reflection anywhere.
 *
 * The generated package is fixed, so the application imports one known name. Applying this processor
 * in more than one module of the same application produces two functions with the same name and the
 * link step fails — put the annotated classes in the module that calls `initializeOopReact()`, which
 * is where application state lives anyway.
 */
class OopReactSymbolProcessor(
    environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger
    private var generated: Boolean = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val classes = LinkedHashMap<String, ClassModel>()
        val files = mutableSetOf<KSFile>()

        fun modelOf(declaration: KSClassDeclaration): ClassModel? {
            val qualifiedName = declaration.qualifiedName?.asString()
            if (qualifiedName == null) {
                logger.error("oop-react annotations need a class with a qualified name", declaration)
                return null
            }
            if (declaration.isPrivate()) {
                logger.error(
                    "@OopReactState cannot be applied to a private class: the generated metadata refers to it " +
                        "by name. Use `internal`.",
                    declaration,
                )
                return null
            }
            declaration.containingFile?.let(files::add)
            return classes.getOrPut(qualifiedName) {
                ClassModel(qualifiedName, declaration.simpleName.asString(), declaration)
            }
        }

        resolver.getSymbolsWithAnnotation(STATE_ANNOTATION).forEach { symbol ->
            val declaration = symbol as? KSClassDeclaration ?: return@forEach
            modelOf(declaration)?.state = true
        }

        resolver.getSymbolsWithAnnotation(RENDER_ANNOTATION).forEach { symbol ->
            val function = symbol as? KSFunctionDeclaration ?: return@forEach
            val owner = function.parentDeclaration as? KSClassDeclaration
            if (owner == null) {
                logger.error("@OopReactRender must be applied to a member function of a class", function)
                return@forEach
            }

            val model = modelOf(owner) ?: return@forEach
            val jsName = function.jsNameOrNull()
            if (jsName == null) {
                logger.error(missingJsName(function.simpleName.asString(), "method"), function)
                return@forEach
            }

            val annotation = function.findAnnotation(RENDER_ANNOTATION)!!
            model.renders += RenderModel(
                jsName = jsName,
                name = annotation.stringArgument("name").ifEmpty { null },
                key = annotation.stringArgument("key").ifEmpty { null },
            )
        }

        resolver.getSymbolsWithAnnotation(IGNORE_ANNOTATION).forEach { symbol ->
            val property = symbol as? KSPropertyDeclaration ?: return@forEach
            val owner = property.parentDeclaration as? KSClassDeclaration
            if (owner == null) {
                logger.error("@OopReactIgnore must be applied to a property of a class", property)
                return@forEach
            }

            val model = modelOf(owner) ?: return@forEach
            val jsName = property.jsNameOrNull()
            if (jsName == null) {
                logger.error(missingJsName(property.simpleName.asString(), "property"), property)
                return@forEach
            }
            model.ignores += jsName
        }

        resolver.getSymbolsWithAnnotation(ACTION_ANNOTATION).forEach { symbol ->
            val function = symbol as? KSFunctionDeclaration ?: return@forEach
            val owner = function.parentDeclaration as? KSClassDeclaration
            if (owner == null) {
                logger.error("@OopReactAction must be applied to a member function of a class", function)
                return@forEach
            }

            val model = modelOf(owner) ?: return@forEach
            val jsName = function.jsNameOrNull()
            if (jsName == null) {
                logger.error(missingJsName(function.simpleName.asString(), "method"), function)
                return@forEach
            }
            model.actions += jsName
        }

        if (classes.isEmpty()) return emptyList()

        writeInitFile(classes.values.toList(), files.toList())
        generated = true
        return emptyList()
    }

    private fun writeInitFile(classes: List<ClassModel>, files: List<KSFile>) {
        val text = buildString {
            appendLine("// Generated by ktjs-oop-react-ksp. Do not edit.")
            appendLine("package $GENERATED_PACKAGE")
            appendLine()
            appendLine("import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactMeta")
            appendLine("import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactRegister")
            appendLine()
            appendLine("/**")
            appendLine(" * Registers every class annotated with `@OopReactState`, `@OopReactRender`, `@OopReactIgnore` or")
            appendLine(" * `@OopReactAction` in this module.")
            appendLine(" *")
            appendLine(" * Call it once, before the first render:")
            appendLine(" *")
            appendLine(" * ```kotlin")
            appendLine(" * fun main() {")
            appendLine(" *     initializeOopReact()")
            appendLine(" *     …")
            appendLine(" * }")
            appendLine(" * ```")
            appendLine(" *")
            appendLine(" * Without it the annotated classes behave like plain ones: nothing is patched, nothing becomes")
            appendLine(" * observable.")
            appendLine(" */")
            appendLine("@Suppress(\"unused\")")
            appendLine("fun $INIT_FUNCTION() {")
            appendLine("    oopReactRegister(")
            for (model in classes) {
                appendLine("        oopReactMeta(${model.qualifiedName}::class.js, name = \"${model.className}\") {")
                if (model.state) appendLine("            state()")
                for (render in model.renders) {
                    val arguments = buildList {
                        add("\"${render.jsName}\"")
                        if (render.name != null) add("name = \"${render.name}\"")
                        if (render.key != null) add("key = \"${render.key}\"")
                    }
                    appendLine("            render(${arguments.joinToString(", ")})")
                }
                for (ignore in model.ignores) {
                    appendLine("            ignore(\"$ignore\")")
                }
                for (action in model.actions) {
                    appendLine("            action(\"$action\")")
                }
                appendLine("        },")
            }
            appendLine("    )")
            appendLine("}")
        }

        codeGenerator
            .createNewFile(
                dependencies = Dependencies(aggregating = true, sources = files.toTypedArray()),
                packageName = GENERATED_PACKAGE,
                fileName = "OopReactMetas",
            )
            .use { it.write(text.toByteArray()) }
    }

    private fun KSClassDeclaration.isPrivate(): Boolean =
        Modifier.PRIVATE in modifiers

    /**
     * The JavaScript name of a member, or `null` when it has none.
     *
     * Kotlin/JS compiles every class member to `name_<hash>_k$`, and the hash is not derivable from
     * source. `@JsName` is the only way to pin the name, so a member the runtime has to find by name
     * carries one — see the error message for why this is not optional.
     */
    private fun KSFunctionDeclaration.jsNameOrNull(): String? =
        findAnnotation(JS_NAME_ANNOTATION)?.stringArgument("name")

    private fun KSPropertyDeclaration.jsNameOrNull(): String? =
        findAnnotation(JS_NAME_ANNOTATION)?.stringArgument("name")

    private fun KSAnnotated.findAnnotation(qualifiedName: String): KSAnnotation? =
        annotations.firstOrNull { it.shortName.asString() == qualifiedName.substringAfterLast('.') && it.matches(qualifiedName) }

    private fun KSAnnotation.matches(qualifiedName: String): Boolean {
        val resolved = annotationType.resolve().declaration.qualifiedName?.asString()
        // A klib the processor cannot resolve still has a short name; the callers only ask for our own
        // annotations, whose short names are unique in practice.
        return resolved == null || resolved == qualifiedName
    }

    private fun KSAnnotation.stringArgument(name: String): String =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? String ?: ""

    private fun missingJsName(member: String, kind: String): String =
        "@OopReactRender/@OopReactIgnore/@OopReactAction need an explicit JavaScript name on $kind '$member'.\n" +
            "Kotlin/JS compiles a member to '$member<hash>_k\$', and the hash is not predictable from source, so " +
            "the runtime cannot find it. Add:\n\n" +
            "    @JsName(\"$member\")\n\n" +
            "to the declaration, before the oop-react annotation. The name has to match the Kotlin one."

    private class ClassModel(
        val qualifiedName: String,
        val className: String,
        val declaration: KSClassDeclaration,
    ) {
        var state: Boolean = false
        val renders: MutableList<RenderModel> = mutableListOf()
        val ignores: MutableSet<String> = mutableSetOf()
        val actions: MutableSet<String> = mutableSetOf()
    }

    private class RenderModel(
        val jsName: String,
        val name: String?,
        val key: String?,
    )
}

private const val GENERATED_PACKAGE = "io.github.shijianjs.ktjs.oopreact.generated"
private const val INIT_FUNCTION = "initializeOopReact"

private const val STATE_ANNOTATION = "io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState"
private const val IGNORE_ANNOTATION = "io.github.shijianjs.ktjs.oopreact.reactivity.OopReactIgnore"
private const val ACTION_ANNOTATION = "io.github.shijianjs.ktjs.oopreact.reactivity.OopReactAction"
private const val RENDER_ANNOTATION = "io.github.shijianjs.ktjs.oopreact.render.OopReactRender"
private const val JS_NAME_ANNOTATION = "kotlin.js.JsName"

class OopReactSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        OopReactSymbolProcessor(environment)
}
