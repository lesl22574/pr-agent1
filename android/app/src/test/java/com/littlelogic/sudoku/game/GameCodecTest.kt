package com.littlelogic.sudoku.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameCodecTest {

    private fun playedGame(): GameState {
        var state = SudokuGame.newGame(
            level = 3,
            difficulty = Difficulty.MEDIUM,
            puzzleSource = { SudokuEngine.generate(it, Random(11)) },
        )
        val index = state.givens.indices.first { state.givens[it] == 0 }
        state = SudokuGame.reduce(state, Action.Select(index))
        state = SudokuGame.reduce(state, Action.Enter(state.solution[index]))
        state = SudokuGame.reduce(state, Action.Tick)
        return state.copy(seconds = 125, hintsUsed = 2)
    }

    @Test
    fun savedGameSurvivesRoundTrip() {
        val state = playedGame()
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(restored)
        restored!!
        assertEquals(state.level, restored.level)
        assertEquals(state.difficulty, restored.difficulty)
        assertEquals(state.solution, restored.solution)
        assertEquals(state.givens, restored.givens)
        assertEquals(state.values, restored.values)
        assertEquals(state.notes, restored.notes)
        assertEquals(state.mistakes, restored.mistakes)
        assertEquals(state.hintsUsed, restored.hintsUsed)
        assertEquals(state.seconds, restored.seconds)
        assertEquals(state.status, restored.status)
        assertEquals(GameStatus.PLAYING, restored.status)
        assertEquals(DialogKind.NONE, restored.dialog)
    }

    @Test
    fun finishedGameKeepsItsDialog() {
        var state = SudokuGame.newGame(
            level = 2,
            difficulty = Difficulty.EASY,
            puzzleSource = { SudokuEngine.generate(it, Random(5)) },
        )
        for (index in 0 until 81) {
            if (state.givens[index] == 0) {
                state = SudokuGame.reduce(state, Action.Select(index))
                state = SudokuGame.reduce(state, Action.Enter(state.solution[index]))
            }
        }
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertEquals(GameStatus.WON, restored?.status)
        assertEquals(DialogKind.WON, restored?.dialog)
    }

    @Test
    fun brokenPayloadsAreRejected() {
        assertNull(GameCodec.decode(null))
        assertNull(GameCodec.decode(""))
        assertNull(GameCodec.decode("not a saved game"))

        val encoded = GameCodec.encode(playedGame())
        val parts = encoded.split("|").toMutableList()

        val brokenGivens = parts[4].split(",").toMutableList()
        val firstGiven = brokenGivens.indices.first { brokenGivens[it] != "0" }
        brokenGivens[firstGiven] = if (brokenGivens[firstGiven] == "9") "8" else "9"
        parts[4] = brokenGivens.joinToString(",")
        assertNull("givens must agree with the solution", GameCodec.decode(parts.joinToString("|")))

        val brokenSolution = encoded.split("|").toMutableList()
        brokenSolution[3] = List(81) { "0" }.joinToString(",")
        assertNull("zeros are not a valid solution", GameCodec.decode(brokenSolution.joinToString("|")))

        val brokenNotes = encoded.split("|").toMutableList()
        brokenNotes[6] = List(81) { "999" }.joinToString(",")
        assertNull("notes are bit masks 0..511", GameCodec.decode(brokenNotes.joinToString("|")))
    }

    @Test
    fun timeIsFormatted() {
        assertEquals("00:00", GameFormat.time(0))
        assertEquals("01:05", GameFormat.time(65))
        assertEquals("60:00", GameFormat.time(3600))
        assertEquals("07", GameFormat.level(7))
    }
}
