package com.littlelogic.sudoku.data

import android.content.Context
import com.littlelogic.sudoku.game.GameCodec
import com.littlelogic.sudoku.game.GameState

/**
 * Persistence of the running puzzle and the chosen language.
 * The whole game fits into one short string produced by [GameCodec], so plain
 * SharedPreferences is all we need — no database, no extra dependency.
 */
class GameStore(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): GameState? = runCatching {
        GameCodec.decode(preferences.getString(KEY_GAME, null))
    }.getOrNull()

    fun save(state: GameState) {
        if (state.solution.size != 81) return
        runCatching {
            preferences.edit().putString(KEY_GAME, GameCodec.encode(state)).apply()
        }
    }

    fun clear() {
        runCatching { preferences.edit().remove(KEY_GAME).apply() }
    }

    fun loadLanguage(): String? = preferences.getString(KEY_LANGUAGE, null)

    fun saveLanguage(language: String) {
        runCatching { preferences.edit().putString(KEY_LANGUAGE, language).apply() }
    }

    private companion object {
        const val FILE_NAME = "littlelogic-sudoku"
        const val KEY_GAME = "game"
        const val KEY_LANGUAGE = "language"
    }
}
