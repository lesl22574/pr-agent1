package com.littlelogic.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {

    @Test
    fun `solved grid follows sudoku rules`() {
        repeat(25) { seed ->
            val grid = solvedGrid(Random(seed))
            assertEquals(81, grid.size)
            val digits = (1..9).toSet()

            repeat(9) { row ->
                assertEquals(digits, (0..8).map { column -> grid[row * 9 + column] }.toSet())
            }
            repeat(9) { column ->
                assertEquals(digits, (0..8).map { row -> grid[row * 9 + column] }.toSet())
            }
            repeat(9) { box ->
                val cells = mutableListOf<Int>()
                repeat(9) { offset ->
                    val row = (box / 3) * 3 + offset / 3
                    val column = (box % 3) * 3 + offset % 3
                    cells.add(grid[row * 9 + column])
                }
                assertEquals(digits, cells.toSet())
            }
        }
    }

    @Test
    fun `every generated puzzle has one solution`() {
        for (difficulty in Difficulty.entries) {
            repeat(3) { seed ->
                val puzzle = createPuzzle(difficulty, Random(seed * 31 + difficulty.ordinal))
                assertEquals(81, puzzle.solution.size)
                assertEquals(81, puzzle.puzzle.size)

                val givenCount = puzzle.puzzle.count { it != 0 }
                assertTrue(
                    "$difficulty produced $givenCount given cells",
                    givenCount >= difficulty.givens,
                )

                puzzle.puzzle.indices.forEach { index ->
                    val given = puzzle.puzzle[index]
                    if (given != 0) assertEquals(puzzle.solution[index], given)
                }

                val grid = puzzle.puzzle.toIntArray()
                assertEquals(1, countSolutions(grid))
                // The solver must leave the caller's array untouched.
                assertTrue(grid.contentEquals(puzzle.puzzle.toIntArray()))
            }
        }
    }

    @Test
    fun `peers share a row column or box`() {
        assertTrue(arePeers(0, 8))
        assertTrue(arePeers(0, 72))
        assertTrue(arePeers(0, 10))
        assertTrue(arePeers(40, 40))
        assertFalse(arePeers(0, 12))
        assertFalse(arePeers(0, 30))
    }

    @Test
    fun `time is formatted as mm ss`() {
        assertEquals("00:00", formatTime(0))
        assertEquals("01:05", formatTime(65))
        assertEquals("100:00", formatTime(6000))
        assertEquals("00:00", formatTime(-10))
    }
}
