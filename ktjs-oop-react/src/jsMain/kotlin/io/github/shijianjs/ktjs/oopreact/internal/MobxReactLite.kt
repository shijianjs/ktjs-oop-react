@file:JsModule("mobx-react-lite")

package io.github.shijianjs.ktjs.oopreact.internal

import react.ComponentType
import react.FC
import react.Props

/**
 * `observer` from `mobx-react-lite`, the official React binding.
 *
 * The subscription, batching and re-throwing behaviour of a tracked render is MobX's, not ours: it
 * subscribes the component to exactly the fields the render read, re-throws what the render throws
 * (so the error boundary above an island can catch it), and keeps working when React changes its
 * scheduling. Hand-rolling a `Reaction` loop here would have to re-implement all three.
 */
internal external fun <P : Props> observer(component: ComponentType<P>): FC<P>
