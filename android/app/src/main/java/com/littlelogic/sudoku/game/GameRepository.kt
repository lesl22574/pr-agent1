package com.littlelogic.sudoku.game

import android.content.Context

/**
 * SharedPreferences persistence. Saved data is validated on read so a damaged
 * or outdated record simply starts a fresh puzzle.
 */
class GameRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(state: GameState) {
        if (!state.isBoardReady) return
        prefs.edit().apply {
            putInt(KEY_LEVEL, state.level)
            putString(KEY_DIFFICULTY, state.difficulty.id)
            putString(KEY_SOLUTION, encodeDigits(state.solution))
            putString(KEY_GIVENS, encodeDigits(state.givens))
            putString(KEY_VALUES, encodeDigits(state.values))
            putString(KEY_NOTES, state.notes.joinToString(","))
            putInt(KEY_MISTAKES, state.mistakes)
            putInt(KEY_HINTS, state.hintsUsed)
            putInt(KEY_SECONDS, state.seconds)
            putString(KEY_STATUS, state.status.name)
            putString(KEY_LANGUAGE, state.language.id)
        }.apply()
    }

    fun restore(): GameState? {
        val level = prefs.getInt(KEY_LEVEL, 0)
        if (level !in 1..999_999) return null

        val difficulty = Difficulty.fromId(prefs.getString(KEY_DIFFICULTY, null)) ?: return null
        val solution = decodeDigits(prefs.getString(KEY_SOLUTION, null), 1, 9) ?: return null
        val givens = decodeDigits(prefs.getString(KEY_GIVENS, null), 0, 9) ?: return null
        val values = decodeDigits(prefs.getString(KEY_VALUES, null), 0, 9) ?: return null
        val notes = decodeNotes(prefs.getString(KEY_NOTES, null)) ?: return null

        for (index in 0 until 81) {
            if (givens[index] != 0 && givens[index] != solution[index]) return null
            if (givens[index] != 0 && values[index] != givens[index]) return null
        }

        val status = when (prefs.getString(KEY_STATUS, null)) {
            GameStatus.WON.name -> GameStatus.WON
            GameStatus.LOST.name -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
        val selected = values.indices.firstOrNull { givens[it] == 0 && values[it] == 0 } ?: 40

        return GameState(
            level = level,
            difficulty = difficulty,
            solution = solution,
            givens = givens,
            values = values,
            notes = notes,
            selected = selected,
            mistakes = prefs.getInt(KEY_MISTAKES, 0).coerceIn(0, 3),
            hintsUsed = prefs.getInt(KEY_HINTS, 0).coerceIn(0, 3),
            seconds = prefs.getInt(KEY_SECONDS, 0).coerceIn(0, 359_999),
            status = status,
            noteMode = false,
            canUndo = false,
            highlightedHint = null,
            dialog = when (status) {
                GameStatus.WON -> DialogType.WON
                GameStatus.LOST -> DialogType.LOST
                GameStatus.PLAYING -> null
            },
            awaitingLevelTransitionAd = false,
            language = Language.fromId(prefs.getString(KEY_LANGUAGE, null)),
        )
    }

    private fun encodeDigits(values: List<Int>): String = buildString(81) {
        values.forEach { append(it) }
    }

    private fun decodeDigits(raw: String?, min: Int, max: Int): List<Int>? {
        if (raw == null || raw.length != 81) return null
        val result = ArrayList<Int>(81)
        for (char in raw) {
            val digit = char.digitToIntOrNull() ?: return null
            if (digit !in min..max) return null
            result.add(digit)
        }
        return result
    }

    private fun decodeNotes(raw: String?): List<Int>? {
        val parts = raw?.split(',') ?: return null
        if (parts.size != 81) return null
        val result = ArrayList<Int>(81)
        for (part in parts) {
            val mask = part.toIntOrNull() ?: return null
            if (mask !in 0..511) return null
            result.add(mask)
        }
        return result
    }

    private companion object {
        const val PREFS_NAME = "littlelogic_sudoku_v1"
        const val KEY_LEVEL = "level"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SOLUTION = "solution"
        const val KEY_GIVENS = "givens"
        const val KEY_VALUES = "values"
        const val KEY_NOTES = "notes"
        const val KEY_MISTAKES = "mistakes"
        const val KEY_HINTS = "hintsUsed"
        const val KEY_SECONDS = "seconds"
        const val KEY_STATUS = "status"
        const val KEY_LANGUAGE = "language"
    }
}
