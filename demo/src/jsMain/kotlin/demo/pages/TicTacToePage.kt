package demo.pages

import demo.app.Section
import demo.app.Settings
import demo.i18n.Msg
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Stack
import mui.material.StackDirection
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.ChildrenBuilder
import react.Key
import react.ReactNode
import web.cssom.ClassName

private const val BOARD_SIZE = 9

/**
 * The official React tutorial, plus its assignments: move history, computed winner, two nested loops.
 *
 * The board reads `history[step]`; the status reads `step` and the winner; the move list reads the
 * whole history. Three islands, three subscriptions — and jumping through the history re-runs the
 * board and the list, not the status' own buttons.
 */
@OopReactState
class TicTacToePage(
    private val settings: Settings,
) : Page {
    override val path: String = "tictactoe"

    override val title: Msg = Msg.navTictactoe

    var history: List<Array<String?>> = listOf(emptyBoard())

    var step: Int = 0

    val squares: Array<String?>
        get() = history[step]

    val xIsNext: Boolean
        get() = step % 2 == 0

    val winner: String?
        get() = calculateWinner(squares)

    val isDraw: Boolean
        get() = winner == null && squares.all { it != null }

    override fun content(): ReactNode = jsx {
        Section(settings.t(Msg.tictactoeTitle), settings.t(Msg.tictactoeHint)) {
            +board()
            +status()
            +moves()
        }
    }

    fun play(index: Int) {
        val current = squares
        if (current[index] != null || calculateWinner(current) != null) return

        val next = current.copyOf()
        next[index] = if (xIsNext) "X" else "O"

        history = history.take(step + 1) + listOf(next)
        step += 1
    }

    fun jumpTo(target: Int) {
        step = target
    }

    fun restart() {
        history = listOf(emptyBoard())
        step = 0
    }

    @OopReactRender
    @JsName("board")
    fun board(): ReactNode {
        val squares = this.squares
        return jsx {
            Stack {
                className = ClassName("app-board")
                for (row in 0 until 3) {
                    Stack {
                        direction = responsive(StackDirection.row)
                        for (column in 0 until 3) {
                            val index = row * 3 + column
                            square(index, squares[index])
                        }
                    }
                }
            }
        }
    }

    @OopReactRender
    @JsName("status")
    fun status(): ReactNode {
        val winner = this.winner
        val xIsNext = this.xIsNext
        val isDraw = this.isDraw
        return jsx {
            Stack {
                direction = responsive(StackDirection.row)
                className = ClassName("app-row")
                Typography {
                    variant = TypographyVariant.h6
                    +when {
                        winner != null -> settings.t(Msg.winner, "player" to winner)
                        isDraw -> settings.t(Msg.draw)
                        else -> settings.t(Msg.nextPlayer, "player" to if (xIsNext) "X" else "O")
                    }
                }
                Button {
                    variant = ButtonVariant.outlined
                    onClick = { restart() }
                    +settings.t(Msg.restart)
                }
            }
        }
    }

    @OopReactRender
    @JsName("moves")
    fun moves(): ReactNode {
        val settings = this.settings
        val history = this.history
        val step = this.step
        return jsx {
            Stack {
                for (index in history.indices) {
                    val label =
                        if (index == 0) settings.t(Msg.jumpStart) else settings.t(Msg.jumpTo, "step" to index)
                    Button {
                        key = Key(index)
                        variant = if (index == step) ButtonVariant.contained else ButtonVariant.text
                        onClick = { jumpTo(index) }
                        +label
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.square(
        index: Int,
        value: String?,
    ) {
        Button {
            key = Key(index)
            variant = ButtonVariant.outlined
            className = ClassName("app-square")
            onClick = { play(index) }
            +(value ?: "")
        }
    }
}

private fun emptyBoard(): Array<String?> = arrayOfNulls(BOARD_SIZE)

/** 8 lines, checked in order; returns the winner's mark or `null`. */
private fun calculateWinner(squares: Array<String?>): String? {
    val lines =
        arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6),
        )

    for (line in lines) {
        val first = squares[line[0]]
        if (first != null && first == squares[line[1]] && first == squares[line[2]]) return first
    }
    return null
}
