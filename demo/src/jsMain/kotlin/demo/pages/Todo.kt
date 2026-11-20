package demo.pages

import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState

/** One entry of the todos list. Observable, so `done` drives its own row only. */
@OopReactState
class Todo(
    val id: Int,
    val title: String,
    var done: Boolean = false,
)
