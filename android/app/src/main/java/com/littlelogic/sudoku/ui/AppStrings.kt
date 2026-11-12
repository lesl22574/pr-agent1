package com.littlelogic.sudoku.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.littlelogic.sudoku.R
import com.littlelogic.sudoku.game.Difficulty
import com.littlelogic.sudoku.game.MessageKey
import java.util.Locale

/**
 * Thin wrapper around [Resources] so the whole UI can be rendered in the language
 * picked in the top bar without recreating the activity.
 */
class AppStrings(private val resources: Resources) {

    val brandFirst: String = resources.getString(R.string.brand_first)
    val brandSecond: String = resources.getString(R.string.brand_second)
    val headerLabel: String = resources.getString(R.string.header_label)

    val heroTitlePrefix: String = resources.getString(R.string.hero_title_prefix)
    val heroTitleEm: String = resources.getString(R.string.hero_title_em)
    val heroTitleSuffix: String = resources.getString(R.string.hero_title_suffix)
    val heroCopy: String = resources.getString(R.string.hero_copy)
    val introFooter: String = resources.getString(R.string.intro_footer)

    val yourJourney: String = resources.getString(R.string.your_journey)
    val difficultyLabel: String = resources.getString(R.string.difficulty)
    val newGame: String = resources.getString(R.string.new_game)
    val boardLabel: String = resources.getString(R.string.board_label)
    val emptyCell: String = resources.getString(R.string.empty_cell)

    val atGlance: String = resources.getString(R.string.at_glance)
    val time: String = resources.getString(R.string.time)
    val mistakes: String = resources.getString(R.string.mistakes)
    val progress: String = resources.getString(R.string.progress)

    val littleHelpers: String = resources.getString(R.string.little_helpers)
    val notes: String = resources.getString(R.string.notes)
    val hint: String = resources.getString(R.string.hint)
    val erase: String = resources.getString(R.string.erase)
    val undo: String = resources.getString(R.string.undo)
    val on: String = resources.getString(R.string.on)
    val off: String = resources.getString(R.string.off)

    val tipTitle: String = resources.getString(R.string.tip_title)
    val tipBody: String = resources.getString(R.string.tip_body)
    val footer: String = resources.getString(R.string.footer)

    val winKicker: String = resources.getString(R.string.win_kicker)
    val winTitle: String = resources.getString(R.string.win_title)
    val winText: String = resources.getString(R.string.win_text)
    val nextLevel: String = resources.getString(R.string.next_level)
    val playAnother: String = resources.getString(R.string.play_another)
    val loseKicker: String = resources.getString(R.string.lose_kicker)
    val loseTitle: String = resources.getString(R.string.lose_title)
    val loseText: String = resources.getString(R.string.lose_text)
    val tryAgain: String = resources.getString(R.string.try_again)
    val newPuzzle: String = resources.getString(R.string.new_puzzle)
    val confirmKicker: String = resources.getString(R.string.confirm_kicker)
    val confirmTitle: String = resources.getString(R.string.confirm_title)
    val confirmText: String = resources.getString(R.string.confirm_text)
    val startNew: String = resources.getString(R.string.start_new)
    val keepPlaying: String = resources.getString(R.string.keep_playing)

    val languageSwitch: String = resources.getString(R.string.language_switch)
    val languageRu: String = resources.getString(R.string.language_ru)
    val languageEn: String = resources.getString(R.string.language_en)

    fun levelTitle(level: String): String = resources.getString(R.string.level_title, level)

    fun mistakesValue(value: Int): String = resources.getString(R.string.mistakes_value, value)

    fun hintsValue(value: Int): String = resources.getString(R.string.hints_value, value)

    fun progressValue(value: Int): String = resources.getString(R.string.progress_value, value)

    fun cellDescription(row: Int, column: Int, content: String): String =
        resources.getString(R.string.cell_description, row, column, content)

    fun difficulty(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EASY -> resources.getString(R.string.easy)
        Difficulty.MEDIUM -> resources.getString(R.string.medium)
        Difficulty.HARD -> resources.getString(R.string.hard)
    }

    fun message(key: MessageKey): String = when (key) {
        MessageKey.SELECT_CELL -> resources.getString(R.string.select_cell)
        MessageKey.FIXED_CELL -> resources.getString(R.string.fixed_cell)
        MessageKey.NO_HINTS -> resources.getString(R.string.no_hints)
        MessageKey.NO_MOVES -> resources.getString(R.string.no_moves)
        MessageKey.NOTES_ON -> resources.getString(R.string.note_on_toast)
        MessageKey.NOTES_OFF -> resources.getString(R.string.note_off_toast)
    }
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
    error("AppStrings is not provided. Wrap the UI into LocalizedApp().")
}

/**
 * Provides strings for [language] ("ru" / "en") by creating a configuration
 * context with that locale, so switching the language is instant.
 */
@Composable
fun LocalizedApp(language: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val strings = remember(context, language) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        val localized = context.createConfigurationContext(configuration)
        AppStrings(localized.resources)
    }
    CompositionLocalProvider(LocalAppStrings provides strings, content = content)
}
