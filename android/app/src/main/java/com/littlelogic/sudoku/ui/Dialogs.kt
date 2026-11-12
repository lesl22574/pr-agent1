package com.littlelogic.sudoku.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlelogic.sudoku.game.Action
import com.littlelogic.sudoku.game.DialogKind
import com.littlelogic.sudoku.game.GameFormat
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.ui.theme.SudokuColors
import com.littlelogic.sudoku.ui.theme.SudokuType
import kotlin.random.Random

/**
 * Result and confirmation overlays, styled after the original modal:
 * round art badge, small kicker, serif title, summary row and two actions.
 */
@Composable
fun GameDialog(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
) {
    val kind = state.dialog
    if (kind == DialogKind.NONE) return

    val won = kind == DialogKind.WON
    val lost = kind == DialogKind.LOST
    val confirm = kind == DialogKind.CONFIRM_NEW_GAME

    val kicker = when {
        won -> strings.winKicker
        lost -> strings.loseKicker
        else -> strings.confirmKicker
    }
    val title = when {
        won -> strings.winTitle
        lost -> strings.loseTitle
        else -> strings.confirmTitle
    }
    val text = when {
        won -> strings.winText
        lost -> strings.loseText
        else -> strings.confirmText
    }
    val primary = when {
        won -> strings.nextLevel
        lost -> strings.tryAgain
        else -> strings.startNew
    }
    val secondary = when {
        won -> strings.playAnother
        lost -> strings.newPuzzle
        else -> strings.keepPlaying
    }
    val symbol = when {
        won -> "✳"
        lost -> "↺"
        else -> "✦"
    }

    Dialog(
        onDismissRequest = { if (confirm) onAction(Action.DismissDialog) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SudokuColors.Scrim),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .shadow(30.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(SudokuColors.White),
            ) {
                if (won) {
                    Confetti(Modifier.matchParentSize())
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 26.dp, end = 26.dp, top = 30.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SudokuColors.ModalArtBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = symbol,
                            color = SudokuColors.ModalArtInk,
                            fontFamily = FontFamily.Serif,
                            fontSize = 42.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = kicker,
                        color = SudokuColors.ModalKicker,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.9.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = SudokuType.Display.copy(fontSize = 28.sp, lineHeight = 32.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = text,
                        color = SudokuColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )

                    if (!confirm) {
                        Spacer(Modifier.height(18.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SudokuColors.Line),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            SummaryValue(
                                value = GameFormat.time(state.seconds),
                                label = strings.time,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryValue(
                                value = state.mistakes.toString(),
                                label = strings.mistakes,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SudokuColors.Line),
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        label = primary,
                        onClick = {
                            when {
                                won -> onAction(Action.NextLevel)
                                lost -> onAction(Action.NewPuzzle)
                                else -> onAction(Action.ConfirmNewGame)
                            }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = secondary,
                        color = SudokuColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                when {
                                    confirm -> onAction(Action.DismissDialog)
                                    else -> onAction(Action.NewPuzzle)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = SudokuColors.TealDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Text(
            text = label.uppercase(),
            color = SudokuColors.StatLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
        )
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SudokuColors.Teal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

private data class ConfettiPiece(
    val x: Float,
    val delay: Int,
    val duration: Int,
    val rotation: Float,
    val color: Color,
)

/** Light falling confetti, drawn only for the win dialog. */
@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val palette = listOf(
        Color(0xFFE99775),
        Color(0xFF67B49C),
        Color(0xFFEAC688),
        Color(0xFFA9C4DD),
    )
    val pieces = remember {
        val random = Random(7)
        List(22) { index ->
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextInt(0, 2200),
                duration = 2600 + random.nextInt(0, 1900),
                rotation = 320f + random.nextFloat() * 360f,
                color = palette[index % palette.size],
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp))) {
        pieces.forEach { piece ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = piece.duration,
                        delayMillis = piece.delay,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "confettiPiece",
            )
            Box(
                Modifier
                    .offset(
                        x = (maxWidth + 16.dp) * piece.x - 8.dp,
                        y = (maxHeight + 24.dp) * progress - 24.dp,
                    )
                    .size(width = 7.dp, height = 11.dp)
                    .graphicsLayer { rotationZ = progress * piece.rotation }
                    .background(piece.color, RoundedCornerShape(2.dp)),
            )
        }
    }
}
