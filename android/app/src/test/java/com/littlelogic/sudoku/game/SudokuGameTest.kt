package com.littlelogic.sudoku.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameTest {

    private val source: (Difficulty) -> Puzzle = { difficulty ->
        SudokuEngine.generate(difficulty, Random(42))
    }

    private fun newGame(level: Int = 1, difficulty: Difficulty = Difficulty.EASY) =
        SudokuGame.newGame(level = level, difficulty = difficulty, puzzleSource = source)

    private fun firstEmpty(state: GameState, skip: Int = -1) =
        state.givens.indices.firstOrNull { state.givens[it] == 0 && it != skip && state.values[it] == 0 }

    @Test
    fun newGameStartsFromThePuzzle() {
        val state = newGame()
        assertEquals(81, state.values.size)
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(DialogKind.NONE, state.dialog)
        assertTrue(state.notes.all { it == 0 })
        assertEquals(0, state.seconds)
        assertTrue(state.givens[state.selected] == 0)
        assertFalse(state.canUndo)
    }

    @Test
    fun givenCellsCannotBeChanged() {
        var state = newGame()
        val given = state.givens.indices.first { state.givens[it] != 0 }
        state = SudokuGame.reduce(state, Action.Select(given), source)
        state = SudokuGame.reduce(state, Action.Enter(5), source)
        assertEquals(state.givens[given], state.values[given])
        assertEquals(MessageKey.FIXED_CELL, state.message?.key)
    }

    @Test
    fun wrongEntryCountsAMistakeAndCanBeUndone() {
        var state = newGame()
        val index = firstEmpty(state)!!
        val wrong = (1..9).first { it != state.solution[index] }
        state = SudokuGame.reduce(state, Action.Select(index), source)
        state = SudokuGame.reduce(state, Action.Enter(wrong), source)
        assertEquals(1, state.mistakes)
        assertTrue(state.isWrong(index))
        assertTrue(state.canUndo)

        state = SudokuGame.reduce(state, Action.Undo, source)
        assertEquals(0, state.values[index])
        assertFalse(state.canUndo)
        assertEquals(1, state.mistakes)
    }

    @Test
    fun eraseClearsValuesAndNotes() {
        var state = newGame()
        val index = firstEmpty(state)!!
        state = SudokuGame.reduce(state, Action.Select(index), source)
        state = SudokuGame.reduce(state, Action.ToggleNotes, source)
        state = SudokuGame.reduce(state, Action.Enter(3), source)
        assertTrue(state.notes[index] != 0)
        state = SudokuGame.reduce(state, Action.Erase, source)
        assertEquals(0, state.values[index])
        assertEquals(0, state.notes[index])
    }

    @Test
    fun notesModeWritesPencilMarksOnly() {
        var state = newGame()
        val index = firstEmpty(state)!!
        state = SudokuGame.reduce(state, Action.Select(index), source)
        assertEquals(MessageKey.NOTES_ON, SudokuGame.reduce(state, Action.ToggleNotes, source).message?.key)
        state = SudokuGame.reduce(state, Action.ToggleNotes, source)
        assertTrue(state.noteMode)

        state = SudokuGame.reduce(state, Action.Enter(4), source)
        state = SudokuGame.reduce(state, Action.Enter(7), source)
        assertEquals(0, state.values[index])
        assertTrue(state.notes[index] and (1 shl 3) != 0)
        assertTrue(state.notes[index] and (1 shl 6) != 0)

        state = SudokuGame.reduce(state, Action.Enter(4), source)
        assertEquals(0, state.notes[index] and (1 shl 3))
    }

    @Test
    fun hintFillsTheSelectedCellAndIsLimited() {
        var state = newGame()
        val index = firstEmpty(state)!!
        state = SudokuGame.reduce(state, Action.Select(index), source)
        state = SudokuGame.reduce(state, Action.Hint, source)
        assertEquals(state.solution[index], state.values[index])
        assertEquals(1, state.hintsUsed)
        assertEquals(index, state.hintPulse)

        state = SudokuGame.reduce(state, Action.ClearHintPulse, source)
        assertEquals(-1, state.hintPulse)

        state = SudokuGame.reduce(state, Action.Hint, source)
        state = SudokuGame.reduce(state, Action.Hint, source)
        assertEquals(3, state.hintsUsed)
        state = SudokuGame.reduce(state, Action.Hint, source)
        assertEquals(3, state.hintsUsed)
        assertEquals(MessageKey.NO_HINTS, state.message?.key)
    }

    @Test
    fun threeMistakesEndTheGame() {
        var state = newGame()
        repeat(3) {
            val index = firstEmpty(state)!!
            val wrong = (1..9).first { it != state.solution[index] }
            state = SudokuGame.reduce(state, Action.Select(index), source)
            state = SudokuGame.reduce(state, Action.Enter(wrong), source)
            state = SudokuGame.reduce(state, Action.Erase, source)
        }
        assertEquals(GameStatus.LOST, state.status)
        assertEquals(DialogKind.LOST, state.dialog)
        assertFalse(state.canInteract)

        val retry = SudokuGame.reduce(state, Action.NewPuzzle, source)
        assertEquals(GameStatus.PLAYING, retry.status)
        assertEquals(0, retry.mistakes)
        assertEquals(0, retry.seconds)
        assertEquals(state.level, retry.level)
    }

    @Test
    fun fillingTheBoardWinsAndNextLevelAdvances() {
        var state = newGame(level = 4)
        for (index in 0 until 81) {
            if (state.givens[index] == 0) {
                state = SudokuGame.reduce(state, Action.Select(index), source)
                state = SudokuGame.reduce(state, Action.Enter(state.solution[index]), source)
            }
        }
        assertEquals(GameStatus.WON, state.status)
        assertEquals(DialogKind.WON, state.dialog)
        assertEquals(100, state.progressPercent())

        assertEquals(5, SudokuGame.reduce(state, Action.NextLevel, source).level)
        assertEquals(4, SudokuGame.reduce(state, Action.NewPuzzle, source).level)
    }

    @Test
    fun clockTicksOnlyWhilePlayable() {
        var state = newGame()
        repeat(3) { state = SudokuGame.reduce(state, Action.Tick, source) }
        assertEquals(3, state.seconds)

        state = SudokuGame.reduce(state, Action.NewGameRequest, source)
        assertEquals(DialogKind.CONFIRM_NEW_GAME, state.dialog)
        val paused = SudokuGame.reduce(state, Action.Tick, source)
        assertEquals(3, paused.seconds)

        val dismissed = SudokuGame.reduce(state, Action.DismissDialog, source)
        assertEquals(DialogKind.NONE, dismissed.dialog)
        assertEquals(3, dismissed.seconds)
    }

    @Test
    fun changingDifficultyStartsAFreshPuzzleOnTheSameLevel() {
        var state = newGame(level = 7)
        state = SudokuGame.reduce(state, Action.ToggleNotes, source)
        state = SudokuGame.reduce(state, Action.SetDifficulty(Difficulty.HARD), source)
        assertEquals(Difficulty.HARD, state.difficulty)
        assertEquals(7, state.level)
        assertEquals(0, state.seconds)
        assertFalse(state.noteMode)
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test
    fun selectingIgnoresInputWhileADialogIsOpen() {
        var state = newGame()
        state = SudokuGame.reduce(state, Action.NewGameRequest, source)
        state = SudokuGame.reduce(state, Action.Select(3), source)
        assertTrue(state.selected != 3)
    }

    @Test
    fun messagesIncrementTheirSequence() {
        var state = newGame()
        val index = state.givens.indices.first { state.givens[it] != 0 }
        state = SudokuGame.reduce(state, Action.Select(index), source)
        state = SudokuGame.reduce(state, Action.Enter(1), source)
        val first = state.message
        assertNotNull(first)
        state = SudokuGame.reduce(state, Action.Enter(1), source)
        assertTrue((state.message?.seq ?: 0) > (first?.seq ?: 0))
    }
}
