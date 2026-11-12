package com.littlelogic.sudoku.game

import kotlin.random.Random

/**
 * Difficulty levels. [givens] is the number of prefilled cells that is left in a
 * freshly generated puzzle (the web version uses the same targets: 42 / 35 / 29).
 */
enum class Difficulty(val givens: Int, val storageKey: String) {
    EASY(42, "easy"),
    MEDIUM(35, "medium"),
    HARD(29, "hard");

    companion object {
        fun fromStorageKey(key: String?): Difficulty? = entries.firstOrNull { it.storageKey == key }
    }
}

/** A generated puzzle. [givens] uses 0 for an empty cell. */
data class Puzzle(val solution: List<Int>, val givens: List<Int>) {
    init {
        require(solution.size == SudokuEngine.CELLS) { "solution must contain ${SudokuEngine.CELLS} cells" }
        require(givens.size == SudokuEngine.CELLS) { "givens must contain ${SudokuEngine.CELLS} cells" }
    }
}

/**
 * Pure Kotlin sudoku engine: generates puzzles with a unique solution and answers
 * the questions the game needs (peer cells, solution counting, finished board).
 *
 * No Android dependencies, so it can be unit tested on the JVM.
 */
object SudokuEngine {

    const val CELLS = 81
    const val EMPTY = 0

    private const val FULL_MASK = 0x3FE // bits 1..9 set

    /** Bit mask of the numbers used by row [row], column [col] and box [box]. */
    fun boxOf(row: Int, col: Int): Int = (row / 3) * 3 + col / 3

    /** True when two cells share a row, a column or a 3x3 box. */
    fun peers(first: Int, second: Int): Boolean {
        val firstRow = first / 9
        val firstCol = first % 9
        val secondRow = second / 9
        val secondCol = second % 9
        return firstRow == secondRow ||
            firstCol == secondCol ||
            boxOf(firstRow, firstCol) == boxOf(secondRow, secondCol)
    }

    /** True when no row, column or box contains the same number twice. */
    fun hasNoConflicts(cells: List<Int>): Boolean {
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        for (index in 0 until CELLS) {
            val value = cells[index]
            if (value == EMPTY) continue
            if (value !in 1..9) return false
            val row = index / 9
            val col = index % 9
            val box = boxOf(row, col)
            val bit = 1 shl value
            if ((rows[row] or cols[col] or boxes[box]) and bit != 0) return false
            rows[row] = rows[row] or bit
            cols[col] = cols[col] or bit
            boxes[box] = boxes[box] or bit
        }
        return true
    }

    /** True when every cell is filled and the board is a valid solution. */
    fun isSolved(cells: List<Int>): Boolean =
        cells.size == CELLS && cells.none { it == EMPTY } && hasNoConflicts(cells)

    /**
     * Counts solutions of [cells] (0 = empty) but stops as soon as [limit] is
     * reached, which is all we need to prove that a puzzle is unique.
     */
    fun countSolutions(cells: List<Int>, limit: Int = 2): Int =
        countSolutions(IntArray(CELLS) { if (it < cells.size) cells[it] else EMPTY }, limit)

    private fun countSolutions(cells: IntArray, limit: Int): Int {
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)

        for (index in 0 until CELLS) {
            val value = cells[index]
            if (value == EMPTY) continue
            if (value !in 1..9) return 0
            val row = index / 9
            val col = index % 9
            val box = boxOf(row, col)
            val bit = 1 shl value
            if ((rows[row] or cols[col] or boxes[box]) and bit != 0) return 0
            rows[row] = rows[row] or bit
            cols[col] = cols[col] or bit
            boxes[box] = boxes[box] or bit
        }

        var found = 0

        fun search() {
            // Pick the empty cell with the fewest candidates (classic MRV rule).
            var bestIndex = -1
            var bestMask = 0
            var bestCount = 10

            for (index in 0 until CELLS) {
                if (cells[index] != EMPTY) continue
                val row = index / 9
                val col = index % 9
                val used = rows[row] or cols[col] or boxes[boxOf(row, col)]
                val mask = FULL_MASK and used.inv()
                val candidates = Integer.bitCount(mask)
                if (candidates == 0) return
                if (candidates < bestCount) {
                    bestIndex = index
                    bestMask = mask
                    bestCount = candidates
                    if (candidates == 1) break
                }
            }

            if (bestIndex == -1) {
                found++
                return
            }

            val row = bestIndex / 9
            val col = bestIndex % 9
            val box = boxOf(row, col)
            var mask = bestMask

            while (mask != 0 && found < limit) {
                val bit = mask and (-mask)
                cells[bestIndex] = Integer.numberOfTrailingZeros(bit)
                rows[row] = rows[row] or bit
                cols[col] = cols[col] or bit
                boxes[box] = boxes[box] or bit

                search()

                cells[bestIndex] = EMPTY
                rows[row] = rows[row] xor bit
                cols[col] = cols[col] xor bit
                boxes[box] = boxes[box] xor bit
                mask = mask xor bit
            }
        }

        search()
        return found
    }

    /**
     * Builds a random completed grid, then removes numbers one by one while the
     * puzzle keeps exactly one solution. Returns givens + full solution.
     */
    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        val solution = solvedGrid(random)
        val cells = solution.copyOf()
        var filled = CELLS

        for (index in (0 until CELLS).shuffled(random)) {
            if (filled <= difficulty.givens) break
            if (cells[index] == EMPTY) continue
            val removed = cells[index]
            cells[index] = EMPTY
            if (countSolutions(cells, limit = 2) == 1) {
                filled--
            } else {
                cells[index] = removed
            }
        }

        return Puzzle(solution = solution.toList(), givens = cells.toList())
    }

    /**
     * A valid completed grid built from the classic pattern
     * `(row * 3 + row / 3 + col) % 9`, with rows, columns and digits shuffled.
     */
    private fun solvedGrid(random: Random): IntArray {
        val rowGroups = (0..2).shuffled(random)
        val colGroups = (0..2).shuffled(random)
        val rows = rowGroups.flatMap { group -> (0..2).shuffled(random).map { group * 3 + it } }
        val cols = colGroups.flatMap { group -> (0..2).shuffled(random).map { group * 3 + it } }
        val digits = (1..9).shuffled(random)

        val grid = IntArray(CELLS)
        for (row in 0..8) {
            for (col in 0..8) {
                grid[row * 9 + col] = digits[(row * 3 + row / 3 + col) % 9]
            }
        }
        val result = IntArray(CELLS)
        for (row in 0..8) {
            for (col in 0..8) {
                result[row * 9 + col] = grid[rows[row] * 9 + cols[col]]
            }
        }
        return result
    }
}
