package com.littlelogic.sudoku.game

import kotlin.math.roundToInt

/** Lifecycle of a single puzzle. */
enum class GameStatus { PLAYING, WON, LOST }

/** Which overlay is currently visible. */
enum class DialogKind { NONE, WON, LOST, CONFIRM_NEW_GAME }

/** Short messages shown as a toast. Kept UI-agnostic; the UI maps them to strings. */
enum class MessageKey {
    SELECT_CELL,
    FIXED_CELL,
    NO_HINTS,
    NO_MOVES,
    NOTES_ON,
    NOTES_OFF,
}

data class GameMessage(val seq: Long, val key: MessageKey)

/** Values + notes of one previous move, used by undo. */
data class UndoSnapshot(val values: List<Int>, val notes: List<Int>)

/**
 * Complete immutable state of the game. Every change goes through
 * [SudokuGame.reduce], which makes the game easy to test and keeps Compose
 * recomposition predictable.
 */
data class GameState(
    val level: Int = 1,
    val difficulty: Difficulty = Difficulty.EASY,
    val solution: List<Int> = emptyList(),
    val givens: List<Int> = emptyList(),
    val values: List<Int> = emptyList(),
    val notes: List<Int> = emptyList(),
    val selected: Int = 40,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val seconds: Int = 0,
    val noteMode: Boolean = false,
    val status: GameStatus = GameStatus.PLAYING,
    val dialog: DialogKind = DialogKind.NONE,
    val history: List<UndoSnapshot> = emptyList(),
    val hintPulse: Int = -1,
    val message: GameMessage? = null,
) {

    val isPlaying: Boolean get() = status == GameStatus.PLAYING

    /** The board accepts input only while the puzzle is playable and no dialog is open. */
    val canInteract: Boolean get() = isPlaying && dialog == DialogKind.NONE

    val canUndo: Boolean get() = history.isNotEmpty()

    val hintsLeft: Int get() = (MAX_HINTS - hintsUsed).coerceAtLeast(0)

    /** True when the cell holds a number that is not the solution. */
    fun isWrong(index: Int): Boolean =
        givens[index] == 0 && values[index] != 0 && values[index] != solution[index]

    /** Percentage of correctly filled cells, givens included. */
    fun progressPercent(): Int {
        if (solution.isEmpty()) return 0
        val filled = values.indices.count { values[it] == solution[it] }
        return (filled * 100.0 / SudokuEngine.CELLS).roundToInt()
    }

    /** How many of [number] are still missing on the board. */
    fun remainingFor(number: Int): Int {
        val placed = values.indices.count { values[it] == number && solution[it] == number }
        return (9 - placed).coerceAtLeast(0)
    }

    fun isComplete(): Boolean =
        values.size == SudokuEngine.CELLS && values.indices.all { values[it] == solution[it] }

    companion object {
        const val MAX_MISTAKES = 3
        const val MAX_HINTS = 3
        const val MAX_HISTORY = 100
        const val MAX_LEVEL = 999_999
        const val MAX_SECONDS = 359_999

        /** Empty state used before the first puzzle is generated. */
        val EMPTY = GameState()
    }
}
