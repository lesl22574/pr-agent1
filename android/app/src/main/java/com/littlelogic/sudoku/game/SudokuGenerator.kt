package com.littlelogic.sudoku.game

import kotlin.random.Random

/** Bits 1..9 are set; bit 0 is unused so a digit can be used directly as a shift. */
private const val FULL_MASK = 0x3FE

data class Puzzle(val solution: List<Int>, val puzzle: List<Int>)

private fun boxOf(row: Int, column: Int): Int = (row / 3) * 3 + column / 3

fun arePeers(first: Int, second: Int): Boolean {
    val r1 = first / 9
    val c1 = first % 9
    val r2 = second / 9
    val c2 = second % 9
    return r1 == r2 || c1 == c2 || boxOf(r1, c1) == boxOf(r2, c2)
}

/**
 * Builds a valid solved board by shuffling bands, stacks, rows within bands,
 * columns within stacks and the digits themselves (same approach as the web game).
 */
fun solvedGrid(random: Random = Random.Default): List<Int> {
    val groups = (0..2).shuffled(random)
    val stacks = (0..2).shuffled(random)
    val rows = groups.flatMap { group -> (0..2).shuffled(random).map { x -> group * 3 + x } }
    val columns = stacks.flatMap { group -> (0..2).shuffled(random).map { x -> group * 3 + x } }
    val digits = (1..9).shuffled(random)
    return rows.flatMap { row -> columns.map { column -> digits[(row * 3 + row / 3 + column) % 9] } }
}

/**
 * Counts solutions of [grid] up to [limit], using bit masks for every row,
 * column and 3x3 box. The grid is restored before the function returns.
 */
fun countSolutions(grid: IntArray, limit: Int = 2): Int {
    val rows = IntArray(9)
    val columns = IntArray(9)
    val boxes = IntArray(9)

    for (index in 0 until 81) {
        val digit = grid[index]
        if (digit == 0) continue
        val bit = 1 shl digit
        val row = index / 9
        val column = index % 9
        val box = boxOf(row, column)
        if (((rows[row] or columns[column] or boxes[box]) and bit) != 0) return 0
        rows[row] = rows[row] or bit
        columns[column] = columns[column] or bit
        boxes[box] = boxes[box] or bit
    }

    var count = 0

    fun search() {
        var best = -1
        var bestMask = 0
        var bestLength = 10

        for (index in 0 until 81) {
            if (grid[index] != 0) continue
            val row = index / 9
            val column = index % 9
            val box = boxOf(row, column)
            val mask = FULL_MASK and (rows[row] or columns[column] or boxes[box]).inv()
            val candidates = mask.countOneBits()
            if (candidates == 0) return
            if (candidates < bestLength) {
                best = index
                bestMask = mask
                bestLength = candidates
                if (candidates == 1) break
            }
        }

        if (best == -1) {
            count++
            return
        }

        val row = best / 9
        val column = best % 9
        val box = boxOf(row, column)
        var mask = bestMask
        while (mask != 0 && count < limit) {
            val bit = mask and -mask
            val digit = bit.countTrailingZeroBits()
            grid[best] = digit
            rows[row] = rows[row] or bit
            columns[column] = columns[column] or bit
            boxes[box] = boxes[box] or bit

            search()

            grid[best] = 0
            rows[row] = rows[row] and bit.inv()
            columns[column] = columns[column] and bit.inv()
            boxes[box] = boxes[box] and bit.inv()
            mask = mask and (mask - 1)
        }
    }

    search()
    return count
}

/** Removes digits while the puzzle keeps exactly one solution. */
fun createPuzzle(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
    val solution = solvedGrid(random).toIntArray()
    val puzzle = solution.copyOf()
    var filled = 81

    for (index in (0 until 81).shuffled(random)) {
        if (filled <= difficulty.givens) break
        val previous = puzzle[index]
        puzzle[index] = 0
        if (countSolutions(puzzle) == 1) {
            filled--
        } else {
            puzzle[index] = previous
        }
    }
    return Puzzle(solution = solution.toList(), puzzle = puzzle.toList())
}
