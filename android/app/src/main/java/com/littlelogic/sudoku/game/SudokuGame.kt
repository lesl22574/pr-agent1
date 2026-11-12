package com.littlelogic.sudoku.game

import kotlin.random.Random

private const val MAX_HISTORY = 100
private const val MAX_MISTAKES = 3
private const val MAX_HINTS = 3
private const val MAX_LEVEL = 999_999

private data class Move(val values: List<Int>, val notes: List<Int>)

/**
 * Pure Kotlin rules of the game: no Android or UI dependencies here, so the
 * behaviour can be covered by JVM unit tests.
 */
class SudokuGame(private val random: Random = Random.Default) {
    private val history = ArrayDeque<Move>()

    fun newGame(
        current: GameState,
        advance: Boolean = false,
        difficulty: Difficulty = current.difficulty,
    ): GameState {
        history.clear()
        val generated = createPuzzle(difficulty, random)
        return current.copy(
            level = if (advance) (current.level + 1).coerceAtMost(MAX_LEVEL) else current.level,
            difficulty = difficulty,
            solution = generated.solution,
            givens = generated.puzzle,
            values = generated.puzzle,
            notes = List(81) { 0 },
            selected = generated.puzzle.indexOfFirst { it == 0 }.takeIf { it >= 0 } ?: 40,
            mistakes = 0,
            hintsUsed = 0,
            seconds = 0,
            status = GameStatus.PLAYING,
            noteMode = false,
            canUndo = false,
            highlightedHint = null,
            dialog = null,
            awaitingLevelTransitionAd = false,
        )
    }

    fun select(state: GameState, index: Int): GameState {
        if (!isEditable(state) || index !in 0 until 81) return state
        return state.copy(selected = index)
    }

    fun enter(state: GameState, number: Int): GameResult {
        if (!isEditable(state) || number !in 1..9) return GameResult(state)
        val index = state.selected
        if (index !in 0 until 81) return GameResult(state, MessageKey.SELECT_CELL)
        if (state.givens[index] != 0) return GameResult(state, MessageKey.FIXED_CELL)

        val values = state.values.toMutableList()
        val notes = state.notes.toMutableList()
        var mistakes = state.mistakes

        if (state.noteMode) {
            if (values[index] != 0) return GameResult(state)
            pushHistory(state)
            notes[index] = notes[index] xor (1 shl (number - 1))
        } else {
            if (values[index] == number) return GameResult(state)
            pushHistory(state)
            values[index] = number
            notes[index] = 0
            if (number == state.solution[index]) {
                removePeerNotes(notes, index, number)
            } else {
                mistakes++
            }
        }

        val next = state.copy(
            values = values,
            notes = notes,
            mistakes = mistakes,
            canUndo = history.isNotEmpty(),
        )
        return when {
            mistakes >= MAX_MISTAKES -> GameResult(next.copy(status = GameStatus.LOST))
            values.indices.all { values[it] == state.solution[it] } ->
                GameResult(next.copy(status = GameStatus.WON), levelCompleted = true)
            else -> GameResult(next)
        }
    }

    fun erase(state: GameState): GameResult {
        if (!isEditable(state)) return GameResult(state)
        val index = state.selected
        if (index !in 0 until 81) return GameResult(state, MessageKey.SELECT_CELL)
        if (state.givens[index] != 0) return GameResult(state, MessageKey.FIXED_CELL)
        if (state.values[index] == 0 && state.notes[index] == 0) return GameResult(state)

        pushHistory(state)
        val values = state.values.toMutableList()
        val notes = state.notes.toMutableList()
        values[index] = 0
        notes[index] = 0
        return GameResult(state.copy(values = values, notes = notes, canUndo = history.isNotEmpty()))
    }

    fun undo(state: GameState): GameResult {
        if (!isEditable(state)) return GameResult(state)
        val previous = history.removeLastOrNull() ?: return GameResult(state, MessageKey.NO_MOVES)
        return GameResult(
            state.copy(
                values = previous.values,
                notes = previous.notes,
                canUndo = history.isNotEmpty(),
            )
        )
    }

    fun hint(state: GameState): GameResult {
        if (!isEditable(state)) return GameResult(state)
        if (state.hintsUsed >= MAX_HINTS) return GameResult(state, MessageKey.NO_HINTS)

        var index = state.selected
        if (index !in 0 until 81 ||
            state.givens[index] != 0 ||
            state.values[index] == state.solution[index]
        ) {
            index = state.values.indices.firstOrNull { state.values[it] != state.solution[it] } ?: -1
        }
        if (index < 0) return GameResult(state)

        pushHistory(state)
        val values = state.values.toMutableList()
        val notes = state.notes.toMutableList()
        val digit = state.solution[index]
        values[index] = digit
        notes[index] = 0
        removePeerNotes(notes, index, digit)

        val next = state.copy(
            selected = index,
            values = values,
            notes = notes,
            hintsUsed = state.hintsUsed + 1,
            highlightedHint = index,
            canUndo = history.isNotEmpty(),
        )
        return if (values.indices.all { values[it] == state.solution[it] }) {
            GameResult(next.copy(status = GameStatus.WON), levelCompleted = true)
        } else {
            GameResult(next)
        }
    }

    fun toggleNoteMode(state: GameState): GameResult {
        if (!isEditable(state)) return GameResult(state)
        val next = state.copy(noteMode = !state.noteMode)
        return GameResult(
            next,
            message = if (next.noteMode) MessageKey.NOTES_ON else MessageKey.NOTES_OFF,
        )
    }

    fun clearHighlight(state: GameState): GameState = state.copy(highlightedHint = null)

    private fun isEditable(state: GameState): Boolean =
        state.isBoardReady && state.status == GameStatus.PLAYING && state.dialog == null

    private fun pushHistory(state: GameState) {
        history.addLast(Move(state.values, state.notes))
        if (history.size > MAX_HISTORY) history.removeFirst()
    }

    private fun removePeerNotes(notes: MutableList<Int>, index: Int, digit: Int) {
        val clearMask = (1 shl (digit - 1)).inv()
        for (i in 0 until 81) {
            if (arePeers(index, i)) notes[i] = notes[i] and clearMask
        }
    }
}
