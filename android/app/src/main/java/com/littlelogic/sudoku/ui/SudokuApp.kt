package com.littlelogic.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.littlelogic.sudoku.ads.AdsController
import com.littlelogic.sudoku.ads.YandexStickyBanner
import com.littlelogic.sudoku.game.DialogType
import com.littlelogic.sudoku.game.GameStatus
import com.littlelogic.sudoku.game.SudokuViewModel
import com.littlelogic.sudoku.game.isBoardReady
import kotlinx.coroutines.delay

@Composable
fun SudokuApp(
    viewModel: SudokuViewModel,
    adsController: AdsController,
    adsReady: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.isBoardReady) return

    val strings = stringsFor(state.language)
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Short hints from the game rules.
    LaunchedEffect(strings) {
        viewModel.messages.collect { key ->
            snackbarHostState.showSnackbar(messageText(key, strings))
        }
    }

    // Interstitial appears once, at the pause between the solved level and
    // the result window. The flag lives in the ViewModel, so a rotation while
    // the ad is open keeps the flow intact.
    LaunchedEffect(state.awaitingLevelTransitionAd) {
        if (state.awaitingLevelTransitionAd) {
            adsController.showBetweenLevels(onFinished = { viewModel.showResultDialog() })
            viewModel.markInterstitialHandled()
        }
    }

    // Timer runs only while the board is playable and the app is in the foreground.
    LaunchedEffect(state.status, state.dialog, resumed) {
        while (state.status == GameStatus.PLAYING && state.dialog == null && resumed) {
            delay(1000L)
            viewModel.tick()
        }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (adsReady) {
                Surface(color = Color.White) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        YandexStickyBanner()
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(
                strings = strings,
                language = state.language,
                onLanguageChange = viewModel::setLanguage,
                onNewGame = viewModel::requestNewGame,
            )
            LevelHeader(state = state, strings = strings)
            DifficultySelector(
                state = state,
                strings = strings,
                onSelect = { difficulty ->
                    if (difficulty != state.difficulty) viewModel.newPuzzle(difficulty = difficulty)
                },
            )
            SudokuBoard(
                state = state,
                strings = strings,
                onSelect = viewModel::selectCell,
            )
            NumberKeypad(
                state = state,
                strings = strings,
                onNumber = viewModel::enterNumber,
            )
            StatsRow(state = state, strings = strings)
            ToolsRow(
                state = state,
                strings = strings,
                onNotes = viewModel::toggleNoteMode,
                onHint = viewModel::hint,
                onErase = viewModel::erase,
                onUndo = viewModel::undo,
            )
            TipCard(strings = strings)
            Footer(strings = strings)
        }
    }

    when (state.dialog) {
        DialogType.WON -> ResultDialog(
            won = true,
            state = state,
            strings = strings,
            onPrimary = { viewModel.newPuzzle(advance = true) },
            onSecondary = { viewModel.newPuzzle() },
        )
        DialogType.LOST -> ResultDialog(
            won = false,
            state = state,
            strings = strings,
            onPrimary = { viewModel.newPuzzle() },
            onSecondary = { viewModel.newPuzzle() },
        )
        DialogType.CONFIRM_NEW_GAME -> ConfirmDialog(
            strings = strings,
            onConfirm = { viewModel.newPuzzle() },
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}
