package com.littlelogic.sudoku.game

/**
 * Compact text format used to persist the current puzzle between launches
 * (the Android layer only stores this single string).
 *
 * Layout: `version|level|difficulty|solution|givens|values|notes|mistakes|hints|seconds|status`
 * with the 81 board cells joined by commas. [decode] is deliberately forgiving:
 * anything that does not look like a valid saved game returns null, and the app
 * simply starts a new puzzle.
 */
object GameCodec {

    private const val VERSION = 1
    private const val SEPARATOR = '|'
    private const val CELL_SEPARATOR = ','

    fun encode(state: GameState): String {
        require(state.solution.size == SudokuEngine.CELLS) { "cannot save an empty state" }
        return listOf(
            VERSION.toString(),
            state.level.toString(),
            state.difficulty.storageKey,
            joinCells(state.solution),
            joinCells(state.givens),
            joinCells(state.values),
            joinCells(state.notes),
            state.mistakes.toString(),
            state.hintsUsed.toString(),
            state.seconds.toString(),
            state.status.name,
        ).joinToString(SEPARATOR.toString())
    }

    fun decode(raw: String?): GameState? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(SEPARATOR)
        if (parts.size != 11) return null
        if (parts[0].toIntOrNull() != VERSION) return null

        val level = parts[1].toIntOrNull() ?: return null
        if (level !in 1..GameState.MAX_LEVEL) return null

        val difficulty = Difficulty.fromStorageKey(parts[2]) ?: return null
        val solution = parseCells(parts[3], 1..9) ?: return null
        val givens = parseCells(parts[4], 0..9) ?: return null
        val values = parseCells(parts[5], 0..9) ?: return null
        val notes = parseCells(parts[6], 0..511) ?: return null

        val mistakes = parts[7].toIntOrNull() ?: return null
        val hintsUsed = parts[8].toIntOrNull() ?: return null
        val seconds = parts[9].toIntOrNull() ?: return null
        val status = runCatching { GameStatus.valueOf(parts[10]) }.getOrNull() ?: return null

        // A given must always match the solution, otherwise the saved game is broken.
        if (solution.indices.any { givens[it] != 0 && givens[it] != solution[it] }) return null

        val selected = values.indices.firstOrNull { givens[it] == 0 && values[it] == 0 } ?: 40

        return GameState(
            level = level,
            difficulty = difficulty,
            solution = solution,
            givens = givens,
            values = values,
            notes = notes,
            selected = selected,
            mistakes = mistakes.coerceIn(0, GameState.MAX_MISTAKES),
            hintsUsed = hintsUsed.coerceIn(0, GameState.MAX_HINTS),
            seconds = seconds.coerceIn(0, GameState.MAX_SECONDS),
            noteMode = false,
            status = status,
            dialog = when (status) {
                GameStatus.WON -> DialogKind.WON
                GameStatus.LOST -> DialogKind.LOST
                GameStatus.PLAYING -> DialogKind.NONE
            },
        )
    }

    private fun joinCells(cells: List<Int>): String = cells.joinToString(CELL_SEPARATOR.toString())

    private fun parseCells(text: String, range: IntRange): List<Int>? {
        val parts = text.split(CELL_SEPARATOR)
        if (parts.size != SudokuEngine.CELLS) return null
        val cells = ArrayList<Int>(SudokuEngine.CELLS)
        for (part in parts) {
            val value = part.toIntOrNull() ?: return null
            if (value !in range) return null
            cells.add(value)
        }
        return cells
    }
}

/** Formatting helpers shared by the UI and the tests. */
object GameFormat {

    fun time(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "%02d:%02d".format(safe / 60, safe % 60)
    }

    fun level(level: Int): String = "%02d".format(level.coerceIn(1, GameState.MAX_LEVEL))
}
