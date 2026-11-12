package com.littlelogic.sudoku.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val game = SudokuGame()

    private val _state = MutableStateFlow(GameState(language = Language.fromSystem()))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<MessageKey>(extraBufferCapacity = 8)
    val messages: SharedFlow<MessageKey> = _messages.asSharedFlow()

    init {
        val restored = repository.restore()
        if (restored != null) {
            _state.value = restored
        } else {
            // Generating a puzzle checks uniqueness many times; keep cold start smooth.
            val initial = _state.value
            viewModelScope.launch {
                val fresh = withContext(Dispatchers.Default) { game.newGame(initial) }
                _state.value = fresh
                repository.save(fresh)
            }
        }
    }

    fun selectCell(index: Int) {
        _state.value = game.select(_state.value, index)
    }

    fun enterNumber(number: Int) = applyResult(game.enter(_state.value, number))

    fun erase() = applyResult(game.erase(_state.value))

    fun undo() = applyResult(game.undo(_state.value))

    fun hint() = applyResult(game.hint(_state.value))

    fun toggleNoteMode() = applyResult(game.toggleNoteMode(_state.value))

    fun newPuzzle(advance: Boolean = false, difficulty: Difficulty? = null) {
        val current = _state.value
        _state.value = game.newGame(current, advance, difficulty ?: current.difficulty)
        repository.save(_state.value)
    }

    fun requestNewGame() {
        val current = _state.value
        if (current.status == GameStatus.PLAYING && current.dialog == null) {
            _state.value = current.copy(dialog = DialogType.CONFIRM_NEW_GAME)
        } else {
            newPuzzle()
        }
    }

    /** Called when the interstitial has finished (or was unavailable). */
    fun showResultDialog() {
        val current = _state.value
        val dialog = when (current.status) {
            GameStatus.WON -> DialogType.WON
            GameStatus.LOST -> DialogType.LOST
            GameStatus.PLAYING -> null
        }
        if (dialog != null && current.dialog == null) {
            _state.value = current.copy(dialog = dialog, awaitingLevelTransitionAd = false)
        }
    }

    fun markInterstitialHandled() {
        if (_state.value.awaitingLevelTransitionAd) {
            _state.value = _state.value.copy(awaitingLevelTransitionAd = false)
        }
    }

    fun dismissDialog() {
        if (_state.value.dialog != null) _state.value = _state.value.copy(dialog = null)
    }

    fun setLanguage(language: Language) {
        if (_state.value.language == language) return
        _state.value = _state.value.copy(language = language)
        repository.save(_state.value)
    }

    fun tick() {
        val current = _state.value
        if (current.status != GameStatus.PLAYING || current.dialog != null) return
        _state.value = current.copy(seconds = (current.seconds + 1).coerceAtMost(359_999))
        if (_state.value.seconds % 5 == 0) repository.save(_state.value)
    }

    override fun onCleared() {
        repository.save(_state.value)
        super.onCleared()
    }

    private fun applyResult(result: GameResult) {
        val previous = _state.value
        _state.value = result.state.copy(
            awaitingLevelTransitionAd = result.levelCompleted && previous.status == GameStatus.PLAYING
        )
        result.message?.let { _messages.tryEmit(it) }
        repository.save(_state.value)

        val hinted = result.state.highlightedHint
        if (hinted != null) {
            viewModelScope.launch {
                delay(700L)
                if (_state.value.highlightedHint == hinted) {
                    _state.value = game.clearHighlight(_state.value)
                }
            }
        }
    }
}
