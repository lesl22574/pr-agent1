package com.littlelogic.sudoku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.littlelogic.sudoku.R
import com.littlelogic.sudoku.game.Action
import com.littlelogic.sudoku.game.GameFormat
import com.littlelogic.sudoku.game.GameMessage
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.ui.theme.SudokuColors
import com.littlelogic.sudoku.ui.theme.SudokuTheme
import com.littlelogic.sudoku.ui.theme.SudokuType
import kotlinx.coroutines.delay

@Composable
fun SudokuApp(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onForegroundChanged(true)
                Lifecycle.Event.ON_STOP -> viewModel.onForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SudokuTheme {
        LocalizedApp(language = language) {
            GameScreen(
                state = state,
                language = language,
                strings = LocalAppStrings.current,
                onLanguage = viewModel::setLanguage,
                onAction = viewModel::dispatch,
            )
        }
    }
}

@Composable
private fun GameScreen(
    state: GameState,
    language: String,
    strings: AppStrings,
    onLanguage: (String) -> Unit,
    onAction: (Action) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SudokuColors.Paper),
    ) {
        BackdropArt()
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            val wide = maxWidth >= 760.dp && maxHeight >= 520.dp
            val shortLandscape = !wide && maxWidth > maxHeight && maxHeight < 480.dp
            when {
                wide -> WideLayout(state, strings, language, onLanguage, onAction)
                shortLandscape -> LandscapeLayout(state, strings, language, onLanguage, onAction)
                else -> CompactLayout(state, strings, language, onLanguage, onAction)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ToastHost(state.message, strings, onAction)
        }
        GameDialog(state = state, strings = strings, onAction = onAction)
    }
}

@Composable
private fun CompactLayout(
    state: GameState,
    strings: AppStrings,
    language: String,
    onLanguage: (String) -> Unit,
    onAction: (Action) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            strings = strings,
            language = language,
            onLanguage = onLanguage,
            showLabel = false,
        )
        Spacer(Modifier.height(4.dp))
        MobileStatsRow(state = state, strings = strings)
        Spacer(Modifier.height(7.dp))
        GameCard(
            state = state,
            strings = strings,
            onAction = onAction,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        ToolsRow(state = state, strings = strings, onAction = onAction)
    }
}

/** Phone held sideways: the board stays big and the helpers move to a side strip. */
@Composable
private fun LandscapeLayout(
    state: GameState,
    strings: AppStrings,
    language: String,
    onLanguage: (String) -> Unit,
    onAction: (Action) -> Unit,
) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            TopBar(
                strings = strings,
                language = language,
                onLanguage = onLanguage,
                showLabel = false,
            )
            Spacer(Modifier.height(3.dp))
            MobileStatsRow(state = state, strings = strings)
            Spacer(Modifier.height(5.dp))
            GameCard(
                state = state,
                strings = strings,
                onAction = onAction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        ToolsColumn(
            state = state,
            strings = strings,
            onAction = onAction,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun WideLayout(
    state: GameState,
    strings: AppStrings,
    language: String,
    onLanguage: (String) -> Unit,
    onAction: (Action) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            strings = strings,
            language = language,
            onLanguage = onLanguage,
            showLabel = true,
        )
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            IntroPanel(
                strings = strings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            GameCard(
                state = state,
                strings = strings,
                onAction = onAction,
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
            )
            SidePanel(
                state = state,
                strings = strings,
                onAction = onAction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun GameCard(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .shadow(18.dp, shape)
            .clip(shape)
            .background(SudokuColors.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = strings.yourJourney, style = SudokuType.Kicker)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = strings.levelTitle(GameFormat.level(state.level)),
                    style = SudokuType.Title,
                )
            }
            RoundIconButton(
                iconRes = R.drawable.ic_new_game,
                description = strings.newGame,
                onClick = { onAction(Action.NewGameRequest) },
            )
        }
        Spacer(Modifier.height(8.dp))
        DifficultySwitch(state = state, strings = strings, onAction = onAction)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val side = minOf(maxWidth, maxHeight)
                SudokuBoard(
                    state = state,
                    strings = strings,
                    onAction = onAction,
                    modifier = Modifier
                        .size(side)
                        .align(Alignment.Center),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Keypad(state = state, onAction = onAction)
    }
}

@Composable
private fun IntroPanel(strings: AppStrings, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .size(width = 21.dp, height = 2.dp)
                    .background(SudokuColors.Coral, RoundedCornerShape(5.dp)),
            )
            Text(
                text = strings.headerLabel.uppercase(),
                color = SudokuColors.Teal,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = buildAnnotatedString {
                append(strings.heroTitlePrefix)
                withStyle(SpanStyle(color = SudokuColors.Teal, fontStyle = FontStyle.Italic)) {
                    append(strings.heroTitleEm)
                }
                append(strings.heroTitleSuffix)
            },
            style = SudokuType.Display.copy(fontSize = 40.sp, lineHeight = 44.sp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.heroCopy,
            color = SudokuColors.Muted,
            fontSize = 12.sp,
            lineHeight = 21.sp,
            modifier = Modifier.widthIn(max = 250.dp),
        )
        Spacer(Modifier.height(28.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text(
                text = "✳",
                color = SudokuColors.Coral,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
            )
            Text(
                text = strings.introFooter,
                color = SudokuColors.Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.widthIn(max = 130.dp),
            )
        }
    }
}

@Composable
private fun SidePanel(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(strings.atGlance)
            Text(
                text = "✳",
                color = SudokuColors.Coral,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
            )
        }
        StatCard(
            title = strings.time,
            value = GameFormat.time(state.seconds),
            iconRes = R.drawable.ic_clock,
        )
        StatCard(
            title = strings.mistakes,
            value = strings.mistakesValue(state.mistakes),
            iconRes = R.drawable.ic_mistakes,
        )
        ProgressCard(title = strings.progress, percent = state.progressPercent())
        SectionLabel(
            text = strings.littleHelpers,
            modifier = Modifier.padding(top = 8.dp),
        )
        ToolsGrid(state = state, strings = strings, onAction = onAction)
        TipCard(strings = strings, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun BackdropArt() {
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        drawCircle(
            color = SudokuColors.BackdropOrb,
            radius = width * 0.42f,
            center = Offset(-width * 0.10f, height * 0.42f),
        )
        drawCircle(
            color = SudokuColors.BackdropRing,
            radius = width * 0.50f,
            center = Offset(width * 1.05f, -height * 0.04f),
            style = Stroke(width = width * 0.16f),
        )
        drawCircle(
            color = SudokuColors.BackdropSpot,
            radius = width * 0.22f,
            center = Offset(width * 0.93f, height * 1.02f),
        )
    }
}

/** Small dark pill at the bottom of the screen, mirrors the web toast. */
@Composable
private fun ToastHost(
    message: GameMessage?,
    strings: AppStrings,
    onAction: (Action) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val current = message
    LaunchedEffect(current?.seq) {
        if (current != null) {
            text = strings.message(current.key)
            delay(2100)
            onAction(Action.DismissMessage)
        }
    }

    AnimatedVisibility(
        visible = current != null,
        enter = fadeIn(tween(180)) + slideInVertically { it / 3 },
        exit = fadeOut(tween(180)) + slideOutVertically { it / 3 },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SudokuColors.ToastBackground)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            )
        }
    }
}
