package demo.app

/**
 * How many times a render method has run, per name.
 *
 * Deliberately **not** an observable object: counting renders is a side effect, and a render method
 * must not write state — writing an observable field while React renders would invalidate the very
 * subscription that is running. A plain object is invisible to MobX and therefore safe.
 */
object RenderStats {
    private val counts: MutableMap<String, Int> = mutableMapOf()

    /** Reads "how many times `name` rendered before this one", i.e. 1 on the first render. */
    fun bump(name: String): Int {
        val next = (counts[name] ?: 0) + 1
        counts[name] = next
        return next
    }

    fun count(name: String): Int = counts[name] ?: 0
}
