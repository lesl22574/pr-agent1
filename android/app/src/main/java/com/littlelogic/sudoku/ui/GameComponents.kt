package com.littlelogic.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.littlelogic.sudoku.game.Difficulty
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.game.GameStatus
import com.littlelogic.sudoku.game.Language
import com.littlelogic.sudoku.game.arePeers
import com.littlelogic.sudoku.game.formatTime
import com.littlelogic.sudoku.game.progressPercent
import com.littlelogic.sudoku.game.remainingCount

@Composable
fun TopBar(
    strings: Strings,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Text(
            text = strings.appName,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
            color = Ink,
        )
        Spacer(Modifier.weight(1f))
        LanguageSwitch(language = language, onLanguageChange = onLanguageChange)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3F6F1))
                .clickable(onClick = onNewGame)
                .semantics { contentDescription = strings.newGame },
            contentAlignment = Alignment.Center,
        ) {
            Text("↻", fontSize = 21.sp, color = Teal)
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .rotate(-8f)
            .clip(RoundedCornerShape(11.dp))
            .background(Teal)
            .padding(6.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { column ->
                        val dimmed = row * 3 + column in setOf(1, 3, 8)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = if (dimmed) 0.34f else 0.96f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitch(language: Language, onLanguageChange: (Language) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFEBECE6))
            .padding(4.dp),
    ) {
        Language.entries.forEach { item ->
            val active = item == language
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onLanguageChange(item) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (item == Language.RU) "RU" else "EN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (active) Teal else Color(0xFF87928E),
                )
            }
        }
    }
}

@Composable
fun LevelHeader(state: GameState, strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = strings.yourJourney,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp,
                color = Muted,
            )
            Text(
                text = "${strings.level} ${state.level.toString().padStart(2, '0')}",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink,
            )
        }
        Text(
            text = strings.hero,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = Muted,
            textAlign = TextAlign.End,
            maxLines = 2,
        )
    }
}

@Composable
fun DifficultySelector(state: GameState, strings: Strings, onSelect: (Difficulty) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFF5F7F3))
            .padding(5.dp)
            .semantics { contentDescription = strings.difficulty },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Difficulty.entries.forEach { difficulty ->
            val active = difficulty == state.difficulty
            val label = when (difficulty) {
                Difficulty.EASY -> strings.easy
                Difficulty.MEDIUM -> strings.medium
                Difficulty.HARD -> strings.hard
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(difficulty) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Teal else Color(0xFF929F9D),
                )
            }
        }
    }
}

@Composable
fun SudokuBoard(
    state: GameState,
    strings: Strings,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = maxWidth / 9f
        val digitSize = cellWidth.value * 0.58f
        val noteSize = cellWidth.value * 0.25f
        val chosen = state.selected.takeIf { it in 0 until 81 }?.let { state.values[it] } ?: 0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(2.dp, GridThick, RoundedCornerShape(8.dp))
                .semantics { contentDescription = strings.board },
        ) {
            Column(Modifier.fillMaxSize()) {
                repeat(9) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(9) { column ->
                            val index = row * 9 + column
                            val value = state.values[index]
                            val given = state.givens[index] != 0
                            val wrong = !given && value != 0 && value != state.solution[index]
                            val background = when {
                                index == state.highlightedHint -> CellHint
                                index == state.selected -> CellSelected
                                wrong -> CellWrong
                                chosen != 0 && value == chosen && index != state.selected -> CellSame
                                arePeers(index, state.selected) -> CellPeer
                                else -> Color.White
                            }
                            val description = buildString {
                                append(strings.cell(row + 1, column + 1))
                                append(": ")
                                append(if (value == 0) strings.empty else value.toString())
                                if (given) append(", ").append(strings.given)
                                if (wrong) append(", ").append(strings.incorrect)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(background)
                                    .gridLines(row, column)
                                    .then(
                                        if (index == state.selected) {
                                            Modifier.border(2.dp, if (wrong) Coral else Teal)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelect(index) }
                                    .semantics { contentDescription = description },
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    value != 0 -> Text(
                                        text = value.toString(),
                                        fontSize = digitSize.sp,
                                        lineHeight = digitSize.sp,
                                        fontWeight = if (given) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = when {
                                            given -> GivenInk
                                            wrong -> WrongInk
                                            else -> ValueInk
                                        },
                                    )
                                    state.notes[index] != 0 -> NotesGrid(
                                        mask = state.notes[index],
                                        color = if (index == state.selected) TealDark else NoteInk,
                                        noteSize = noteSize,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.gridLines(row: Int, column: Int): Modifier = drawBehind {
    val thin = 1.dp.toPx()
    val thick = 2.dp.toPx()
    if (column < 8) {
        val strong = column == 2 || column == 5
        val width = if (strong) thick else thin
        drawLine(
            color = if (strong) GridThick else GridThin,
            start = Offset(size.width - width / 2f, 0f),
            end = Offset(size.width - width / 2f, size.height),
            strokeWidth = width,
        )
    }
    if (row < 8) {
        val strong = row == 2 || row == 5
        val width = if (strong) thick else thin
        drawLine(
            color = if (strong) GridThick else GridThin,
            start = Offset(0f, size.height - width / 2f),
            end = Offset(size.width, size.height - width / 2f),
            strokeWidth = width,
        )
    }
}

@Composable
private fun NotesGrid(mask: Int, color: Color, noteSize: Float) {
    Column(Modifier.fillMaxSize().padding(2.dp)) {
        repeat(3) { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                repeat(3) { column ->
                    val digit = row * 3 + column + 1
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (mask and (1 shl (digit - 1)) != 0) {
                            Text(
                                text = digit.toString(),
                                fontSize = noteSize.sp,
                                lineHeight = noteSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberKeypad(
    state: GameState,
    strings: Strings,
    onNumber: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = strings.numbers },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (number in 1..9) {
            val remaining = state.remainingCount(number)
            val enabled = state.status == GameStatus.PLAYING && remaining > 0
            val active = state.selected in state.values.indices && state.values[state.selected] == number
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            !enabled -> KeyBackground.copy(alpha = 0.4f)
                            active -> Teal
                            else -> KeyBackground
                        }
                    )
                    .clickable(enabled = enabled) { onNumber(number) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = number.toString(),
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        color = if (active) Color.White else ValueInk,
                    )
                    Text(
                        text = remaining.toString(),
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color(0xFFCCE4DC) else Color(0xFF9CAB9E),
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(state: GameState, strings: Strings) {
    val progress = state.progressPercent()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(label = strings.time, value = formatTime(state.seconds))
            StatItem(label = strings.mistakes, value = "${state.mistakes} / 3")
            StatItem(label = strings.progress, value = "$progress%")
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ProgressTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress / 100f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF64B49B), Teal))),
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9AA7A2))
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
fun ToolsRow(
    state: GameState,
    strings: Strings,
    onNotes: () -> Unit,
    onHint: () -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
) {
    val editable = state.status == GameStatus.PLAYING && state.dialog == null
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = strings.littleHelpers },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ToolButton(
            glyph = "✎",
            label = strings.notes,
            detail = if (state.noteMode) strings.on else strings.off,
            active = state.noteMode,
            enabled = editable,
            onClick = onNotes,
        )
        ToolButton(
            glyph = "✦",
            label = strings.hint,
            detail = "${3 - state.hintsUsed} / 3",
            active = false,
            enabled = editable && state.hintsUsed < 3,
            onClick = onHint,
        )
        ToolButton(
            glyph = "⌫",
            label = strings.erase,
            detail = "⌫",
            active = false,
            enabled = editable,
            onClick = onErase,
        )
        ToolButton(
            glyph = "↶",
            label = strings.undo,
            detail = "↶",
            active = false,
            enabled = editable && state.canUndo,
            onClick = onUndo,
        )
    }
}

@Composable
private fun RowScope.ToolButton(
    glyph: String,
    label: String,
    detail: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled -> Color(0xFFB9C4BE)
        active -> TealDark
        else -> Color(0xFF53736F)
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) Color(0xFFE6F3E9) else Color.White)
            .border(
                width = 1.dp,
                color = if (active) Color(0xFF9BCBB8) else Color(0xFFF0F2ED),
                shape = RoundedCornerShape(13.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = glyph, fontSize = 19.sp, color = contentColor)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = detail, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
fun TipCard(strings: Strings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TealSoft)
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = strings.tipTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp,
                color = Teal,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.tipBody,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFF66837A),
            )
        }
    }
}

@Composable
fun Footer(strings: Strings) {
    Text(
        text = strings.footer,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 10.sp,
        color = Color(0xFFACB6AC),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ResultDialog(
    won: Boolean,
    state: GameState,
    strings: Strings,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Dialog(onDismissRequest = onPrimary) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = if (won) "✳" else "↺", fontSize = 34.sp, color = Coral)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (won) strings.winKicker else strings.loseKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Teal,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (won) strings.winTitle else strings.loseTitle,
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (won) strings.winText else strings.loseText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF90A09A),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DialogStat(value = formatTime(state.seconds), label = strings.time)
                    DialogStat(value = state.mistakes.toString(), label = strings.mistakes)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                ) {
                    Text(
                        text = if (won) strings.nextLevel else strings.tryAgain,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                TextButton(onClick = onSecondary) {
                    Text(
                        text = if (won) strings.playAgain else strings.newPuzzle,
                        color = Color(0xFF92A19D),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TealDark)
        Text(text = label, fontSize = 10.sp, color = Color(0xFF99A9A2))
    }
}

@Composable
fun ConfirmDialog(strings: Strings, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "✦", fontSize = 34.sp, color = Coral)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.confirmKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Teal,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.confirmTitle,
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = strings.confirmText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF90A09A),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                ) {
                    Text(text = strings.startNew, fontWeight = FontWeight.Bold, color = Color.White)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = strings.keepPlaying, color = Color(0xFF92A19D), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
