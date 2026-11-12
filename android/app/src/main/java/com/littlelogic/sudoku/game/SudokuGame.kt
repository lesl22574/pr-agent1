package com.littlelogic.sudoku.game

/** Everything the player (or the clock) can do. */
sealed interface Action {

    data class Select(val index: Int) : Action

    data class Enter(val number: Int) : Action

    data object Erase : Action

    data object Undo : Action

    data object Hint : Action

    data object ToggleNotes : Action

    /** Header "new game" button: asks for confirmation while a puzzle is running. */
    data object NewGameRequest : Action

    /** "Start a new game" in the confirmation dialog. */
    data object ConfirmNewGame : Action

    /** Closes the current dialog without touching the puzzle. */
    data object DismissDialog : Action

    /** "Next level" after a win. */
    data object NextLevel : Action

    /** "One more puzzle" / "Try again" — keeps the level. */
    data object NewPuzzle : Action

    data class SetDifficulty(val difficulty: Difficulty) : Action

    /** One second of play. */
    data object Tick : Action

    data object ClearHintPulse : Action

    /** Hides the current toast once it has been visible long enough. */
    data object DismissMessage : Action
}

/**
 * Pure game rules: takes the current [GameState] plus an [Action] and returns the
 * next state. Puzzle generation is injected so tests can use fixed boards.
 */
object SudokuGame {

    fun newGame(
        level: Int = 1,
        difficulty: Difficulty = Difficulty.EASY,
        puzzleSource: (Difficulty) -> Puzzle = { SudokuEngine.generate(it) },
    ): GameState {
        val safeLevel = level.coerceIn(1, GameState.MAX_LEVEL)
        val puzzle = puzzleSource(difficulty)
        return GameState(
            level = safeLevel,
            difficulty = difficulty,
            solution = puzzle.solution,
            givens = puzzle.givens,
            values = puzzle.givens,
            notes = List(SudokuEngine.CELLS) { 0 },
            selected = firstEmptyCell(puzzle.givens),
            status = GameStatus.PLAYING,
            dialog = DialogKind.NONE,
        )
    }

    fun reduce(
        state: GameState,
        action: Action,
        puzzleSource: (Difficulty) -> Puzzle = { SudokuEngine.generate(it) },
    ): GameState = when (action) {
        is Action.Select -> if (!state.canInteract) state else state.copy(selected = action.index.coerceIn(0, 80))

        is Action.Enter -> enter(state, action.number)

        Action.Erase -> erase(state)
        Action.Undo -> undo(state)
        Action.Hint -> hint(state)

        Action.ToggleNotes -> if (!state.canInteract) {
            state
        } else {
            state.copy(noteMode = !state.noteMode)
                .withMessage(if (state.noteMode) MessageKey.NOTES_OFF else MessageKey.NOTES_ON)
        }

        Action.NewGameRequest -> when (state.status) {
            GameStatus.PLAYING -> state.copy(dialog = DialogKind.CONFIRM_NEW_GAME)
            else -> newGame(state.level, state.difficulty, puzzleSource)
        }

        Action.ConfirmNewGame -> newGame(state.level, state.difficulty, puzzleSource)

        Action.DismissDialog -> state.copy(dialog = DialogKind.NONE)

        Action.NextLevel -> newGame(
            level = (state.level + 1).coerceAtMost(GameState.MAX_LEVEL),
            difficulty = state.difficulty,
            puzzleSource = puzzleSource,
        )

        Action.NewPuzzle -> newGame(state.level, state.difficulty, puzzleSource)

        is Action.SetDifficulty ->
            if (action.difficulty == state.difficulty) state
            else newGame(state.level, action.difficulty, puzzleSource)

        Action.Tick -> if (state.canInteract && state.seconds < GameState.MAX_SECONDS) {
            state.copy(seconds = state.seconds + 1)
        } else {
            state
        }

        Action.ClearHintPulse -> if (state.hintPulse == -1) state else state.copy(hintPulse = -1)

        Action.DismissMessage -> if (state.message == null) state else state.copy(message = null)
    }

    private fun firstEmptyCell(givens: List<Int>): Int =
        givens.indexOfFirst { it == 0 }.takeIf { it >= 0 } ?: 40

    private fun enter(state: GameState, number: Int): GameState {
        if (!state.canInteract) return state
        if (number !in 1..9) return state
        val index = state.selected
        if (state.givens[index] != 0) return state.withMessage(MessageKey.FIXED_CELL)

        if (state.noteMode) {
            if (state.values[index] != 0) return state
            val notes = state.notes.toMutableList()
            notes[index] = notes[index] xor (1 shl (number - 1))
            return state.withSnapshot().copy(notes = notes)
        }

        if (state.values[index] == number) return state

        var next = state.withSnapshot()
        val values = next.values.toMutableList()
        values[index] = number
        var notes = next.notes.toMutableList()
        notes[index] = 0

        if (number == next.solution[index]) {
            notes = withoutPeerNotes(notes, index, number)
        } else {
            next = next.copy(mistakes = next.mistakes + 1)
        }

        next = next.copy(values = values, notes = notes)
        return next.afterMove()
    }

    private fun erase(state: GameState): GameState {
        if (!state.canInteract) return state
        val index = state.selected
        if (state.givens[index] != 0) return state.withMessage(MessageKey.FIXED_CELL)
        if (state.values[index] == 0 && state.notes[index] == 0) return state

        val next = state.withSnapshot()
        val values = next.values.toMutableList()
        values[index] = 0
        val notes = next.notes.toMutableList()
        notes[index] = 0
        return next.copy(values = values, notes = notes)
    }

    private fun undo(state: GameState): GameState {
        if (!state.canInteract) return state
        val previous = state.history.lastOrNull() ?: return state.withMessage(MessageKey.NO_MOVES)
        return state.copy(
            values = previous.values,
            notes = previous.notes,
            history = state.history.dropLast(1),
        )
    }

    private fun hint(state: GameState): GameState {
        if (!state.canInteract) return state
        if (state.hintsUsed >= GameState.MAX_HINTS) return state.withMessage(MessageKey.NO_HINTS)

        var index = state.selected
        if (state.givens[index] != 0 || state.values[index] == state.solution[index]) {
            index = state.values.indices.firstOrNull { state.values[it] != state.solution[it] } ?: return state
        }

        var next = state.withSnapshot()
        val values = next.values.toMutableList()
        values[index] = next.solution[index]
        var notes = next.notes.toMutableList()
        notes[index] = 0
        notes = withoutPeerNotes(notes, index, next.solution[index])

        next = next.copy(
            selected = index,
            values = values,
            notes = notes,
            hintsUsed = next.hintsUsed + 1,
            hintPulse = index,
        )
        return next.afterMove()
    }

    private fun GameState.afterMove(): GameState = when {
        mistakes >= GameState.MAX_MISTAKES -> copy(status = GameStatus.LOST, dialog = DialogKind.LOST, hintPulse = -1)
        isComplete() -> copy(status = GameStatus.WON, dialog = DialogKind.WON, hintPulse = -1)
        else -> this
    }

    private fun GameState.withSnapshot(): GameState =
        copy(history = (history + UndoSnapshot(values, notes)).takeLast(GameState.MAX_HISTORY))

    private fun GameState.withMessage(key: MessageKey): GameState =
        copy(message = GameMessage(seq = (message?.seq ?: 0L) + 1L, key = key))

    private fun withoutPeerNotes(notes: MutableList<Int>, index: Int, number: Int): MutableList<Int> {
        val bit = 1 shl (number - 1)
        for (other in 0 until SudokuEngine.CELLS) {
            if (SudokuEngine.peers(index, other)) notes[other] = notes[other] and bit.inv()
        }
        return notes
    }
}
