package com.littlelogic.sudoku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.littlelogic.sudoku.data.GameStore
import com.littlelogic.sudoku.game.Action
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.game.SudokuGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Single source of truth for the screen. Player input is queued and processed one
 * action at a time on a background dispatcher, so puzzle generation never blocks
 * the UI and the order of taps is preserved.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val store = GameStore(application)
    private val actions = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(store.load() ?: SudokuGame.newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _language = MutableStateFlow(store.loadLanguage() ?: systemLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    @Volatile
    private var foreground = true
    private var timerActive = false
    private var timerJob: Job? = null
    private var hintPulseJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val action = actions.receiveCatching().getOrNull() ?: break
                apply(action)
            }
        }
        syncTimer()
    }

    fun dispatch(action: Action) {
        actions.trySend(action)
    }

    fun setLanguage(language: String) {
        if (_language.value == language) return
        _language.value = language
        store.saveLanguage(language)
    }

    /** Keeps the clock and the saved game in sync with the app lifecycle. */
    @Synchronized
    fun onForegroundChanged(isForeground: Boolean) {
        foreground = isForeground
        if (!isForeground) persist()
        syncTimer()
    }

    override fun onCleared() {
        persist()
        timerJob?.cancel()
        hintPulseJob?.cancel()
        actions.close()
        super.onCleared()
    }

    private fun apply(action: Action) {
        val previous = _state.value
        val next = SudokuGame.reduce(previous, action)
        if (next != previous) {
            _state.value = next
            if (action != Action.Tick || !next.canInteract || next.seconds % 5 == 0) {
                persist(next)
            }
            if (action == Action.Hint && next.hintPulse != -1) scheduleHintPulseClear()
        }
        syncTimer()
    }

    private fun scheduleHintPulseClear() {
        hintPulseJob?.cancel()
        hintPulseJob = viewModelScope.launch {
            delay(HINT_PULSE_MS)
            actions.trySend(Action.ClearHintPulse)
        }
    }

    @Synchronized
    private fun syncTimer() {
        val shouldRun = foreground && _state.value.canInteract
        if (shouldRun == timerActive) return
        timerActive = shouldRun
        timerJob?.cancel()
        timerJob = if (shouldRun) {
            viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    if (!foreground) break
                    actions.trySend(Action.Tick)
                }
            }
        } else {
            null
        }
    }

    private fun persist(state: GameState = _state.value) {
        runCatching { store.save(state) }
    }

    private fun systemLanguage(): String =
        if (Locale.getDefault().language.lowercase(Locale.ROOT).startsWith("ru")) "ru" else "en"

    private companion object {
        const val HINT_PULSE_MS = 700L
    }
}
