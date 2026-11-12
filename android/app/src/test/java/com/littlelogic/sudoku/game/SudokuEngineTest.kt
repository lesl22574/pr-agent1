package com.littlelogic.sudoku.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuEngineTest {

    private val knownPuzzle = (
        "530070000" +
            "600195000" +
            "098000060" +
            "800060003" +
            "400803001" +
            "700020006" +
            "060000280" +
            "000419005" +
            "000080079"
        ).map { if (it == '0') 0 else it - '0' }

    private val knownSolution = (
        "534678912" +
            "672195348" +
            "198342567" +
            "859761423" +
            "426853791" +
            "713924856" +
            "961537284" +
            "287419635" +
            "345286179"
        ).map { it - '0' }

    @Test
    fun generatedPuzzlesHaveAUniqueSolution() {
        for (difficulty in Difficulty.entries) {
            repeat(5) { round ->
                val puzzle = SudokuEngine.generate(difficulty, Random(round))
                assertTrue(
                    "${difficulty.name} solution must be valid",
                    SudokuEngine.isSolved(puzzle.solution),
                )
                assertTrue(
                    "${difficulty.name} givens must match the solution",
                    puzzle.givens.indices.all { puzzle.givens[it] == 0 || puzzle.givens[it] == puzzle.solution[it] },
                )
                assertEquals(
                    "${difficulty.name} must keep exactly one solution",
                    1,
                    SudokuEngine.countSolutions(puzzle.givens, limit = 2),
                )
                assertTrue(
                    "${difficulty.name} keeps at most ${difficulty.givens} givens",
                    puzzle.givens.count { it != 0 } <= difficulty.givens,
                )
            }
        }
    }

    @Test
    fun harderLevelsHaveFewerGivens() {
        val easy = SudokuEngine.generate(Difficulty.EASY, Random(1)).givens.count { it != 0 }
        val medium = SudokuEngine.generate(Difficulty.MEDIUM, Random(1)).givens.count { it != 0 }
        val hard = SudokuEngine.generate(Difficulty.HARD, Random(1)).givens.count { it != 0 }
        assertTrue("easy should be the fullest", easy >= medium)
        assertTrue("hard should be the emptiest", medium >= hard)
    }

    @Test
    fun peersAreDetected() {
        assertTrue(SudokuEngine.peers(0, 8))
        assertTrue(SudokuEngine.peers(0, 72))
        assertTrue(SudokuEngine.peers(0, 20))
        assertFalse(SudokuEngine.peers(0, 40))
    }

    @Test
    fun completedBoardIsRecognised() {
        assertTrue(SudokuEngine.isSolved(knownSolution))
        assertFalse(SudokuEngine.isSolved(knownPuzzle))
        assertTrue(SudokuEngine.hasNoConflicts(knownPuzzle))
        assertEquals(1, SudokuEngine.countSolutions(knownPuzzle, limit = 2))
    }

    @Test
    fun conflictsAreRejected() {
        val broken = knownSolution.toMutableList()
        broken[0] = broken[1]
        assertFalse(SudokuEngine.hasNoConflicts(broken))
        assertEquals(0, SudokuEngine.countSolutions(broken, limit = 2))
    }
}
