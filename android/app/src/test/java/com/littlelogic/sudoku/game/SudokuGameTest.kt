package com.littlelogic.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGameTest {

    private fun freshState(): GameState {
        val puzzle = createPuzzle(Difficulty.EASY, Random(7))
        return GameState(
            solution = puzzle.solution,
            givens = puzzle.puzzle,
            values = puzzle.puzzle,
        )
    }

    @Test
    fun `last correct digit completes the level`() {
        val game = SudokuGame(Random(1))
        val base = freshState()
        val index = base.givens.indexOfFirst { it == 0 }
        val values = base.solution.toMutableList()
        values[index] = 0
        val almostDone = base.copy(values = values, selected = index)

        val result = game.enter(almostDone, base.solution[index])

        assertEquals(GameStatus.WON, result.state.status)
        assertTrue(result.levelCompleted)
    }

    @Test
    fun `three wrong digits finish the level`() {
        val game = SudokuGame(Random(2))
        val base = freshState()
        val index = base.givens.indexOfFirst { it == 0 }
        val wrongDigits = (1..9).filter { it != base.solution[index] }.take(3)

        var state = base.copy(selected = index)
        wrongDigits.forEach { digit -> state = game.enter(state, digit).state }

        assertEquals(3, state.mistakes)
        assertEquals(GameStatus.LOST, state.status)
    }

    @Test
    fun `undo restores the previous board`() {
        val game = SudokuGame(Random(3))
        val base = freshState()
        val index = base.givens.indexOfFirst { it == 0 }
        val selected = base.copy(selected = index)

        val after = game.enter(selected, base.solution[index]).state
        assertTrue(after.canUndo)
        val undone = game.undo(after).state

        assertEquals(0, undone.values[index])
        assertEquals(base.values, undone.values)
    }

    @Test
    fun `hints are limited to three`() {
        val game = SudokuGame(Random(4))
        var state = freshState()
        repeat(3) { state = game.hint(state).state }

        assertEquals(3, state.hintsUsed)
        assertEquals(MessageKey.NO_HINTS, game.hint(state).message)
    }

    @Test
    fun `notes toggle the digit bit`() {
        val game = SudokuGame(Random(5))
        val base = freshState()
        val index = base.givens.indexOfFirst { it == 0 }
        val toggled = game.toggleNoteMode(base)

        assertEquals(MessageKey.NOTES_ON, toggled.message)
        val withNote = game.enter(toggled.state.copy(selected = index), 5).state
        assertEquals(1 shl 4, withNote.notes[index])
        val withoutNote = game.enter(withNote, 5).state
        assertEquals(0, withoutNote.notes[index])
    }

    @Test
    fun `new game advances the level and resets progress`() {
        val game = SudokuGame(Random(6))
        val state = freshState().copy(level = 4, seconds = 120, mistakes = 2)

        val next = game.newGame(state, advance = true, difficulty = Difficulty.HARD)

        assertEquals(5, next.level)
        assertEquals(Difficulty.HARD, next.difficulty)
        assertEquals(0, next.seconds)
        assertEquals(0, next.mistakes)
        assertEquals(GameStatus.PLAYING, next.status)
        assertTrue(next.values.indices.any { next.values[it] == 0 })
    }
}
