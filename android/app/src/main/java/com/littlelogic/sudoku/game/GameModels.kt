package com.littlelogic.sudoku.game

import java.util.Locale
import kotlin.math.roundToInt

/** In-app language switch, independent from the system locale. */
enum class Language(val id: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromId(id: String?): Language = entries.firstOrNull { it.id == id } ?: RU

        fun fromSystem(): Language =
            if (Locale.getDefault().language.equals("ru", ignoreCase = true)) RU else EN
    }
}

/** Target number of given cells, exactly as in the web version. */
enum class Difficulty(val id: String, val givens: Int) {
    EASY("easy", 42),
    MEDIUM("medium", 35),
    HARD("hard", 29);

    companion object {
        fun fromId(id: String?): Difficulty? = entries.firstOrNull { it.id == id }
    }
}

enum class GameStatus { PLAYING, WON, LOST }

enum class DialogType { WON, LOST, CONFIRM_NEW_GAME }

/** Text keys for short hints shown in a snackbar. */
enum class MessageKey { NOTES_ON, NOTES_OFF, SELECT_CELL, FIXED_CELL, NO_HINTS, NO_MOVES }

/**
 * Immutable snapshot of a game. All cells are stored as flat 81-element lists:
 * digits 0..9 for [givens], [values], [solution] and a 9-bit mask for [notes].
 */
data class GameState(
    val level: Int = 1,
    val difficulty: Difficulty = Difficulty.EASY,
    val solution: List<Int> = emptyList(),
    val givens: List<Int> = emptyList(),
    val values: List<Int> = emptyList(),
    val notes: List<Int> = List(81) { 0 },
    val selected: Int = 40,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val seconds: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val noteMode: Boolean = false,
    val canUndo: Boolean = false,
    val highlightedHint: Int? = null,
    val dialog: DialogType? = null,
    val awaitingLevelTransitionAd: Boolean = false,
    val language: Language = Language.RU,
)

val GameState.isBoardReady: Boolean
    get() = solution.size == 81 && givens.size == 81 && values.size == 81 && notes.size == 81

fun formatTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(Locale.ROOT, safe / 60, safe % 60)
}

fun GameState.progressPercent(): Int {
    if (!isBoardReady) return 0
    val filled = values.indices.count { values[it] == solution[it] }
    return (filled.toFloat() / 81f * 100f).roundToInt()
}

/** How many copies of [number] are still missing from the board. */
fun GameState.remainingCount(number: Int): Int {
    if (!isBoardReady) return 9
    val placed = values.indices.count { values[it] == number && solution[it] == number }
    return (9 - placed).coerceAtLeast(0)
}

data class GameResult(
    val state: GameState,
    val message: MessageKey? = null,
    val levelCompleted: Boolean = false,
)
