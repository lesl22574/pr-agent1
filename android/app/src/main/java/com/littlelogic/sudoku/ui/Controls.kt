package com.littlelogic.sudoku.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlelogic.sudoku.R
import com.littlelogic.sudoku.game.Action
import com.littlelogic.sudoku.game.Difficulty
import com.littlelogic.sudoku.game.GameFormat
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.ui.theme.SudokuColors

/** Small rotated 3x3 mark, the app's logo. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = -8f }
            .clip(RoundedCornerShape(12.dp))
            .background(SudokuColors.Teal),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(7.dp),
        ) {
            val gap = 2.5.dp.toPx()
            val cell = (this.size.minDimension - 2 * gap) / 3f
            val faded = setOf(1, 3, 8)
            for (row in 0..2) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    drawRoundRect(
                        color = SudokuColors.White.copy(alpha = if (index in faded) 0.34f else 0.96f),
                        topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.22f),
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(
    strings: AppStrings,
    language: String,
    onLanguage: (String) -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Text(
            text = strings.brandFirst,
            color = SudokuColors.Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = strings.brandSecond,
            color = SudokuColors.Teal,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.weight(1f))
        if (showLabel) {
            Text(
                text = strings.headerLabel.uppercase(),
                color = SudokuColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.width(14.dp))
        }
        LanguageSwitch(language = language, onLanguage = onLanguage, strings = strings)
    }
}

@Composable
private fun LanguageSwitch(
    language: String,
    onLanguage: (String) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(SudokuColors.LanguageSwitchBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageButton(
            text = "RU",
            description = strings.languageRu,
            active = language == "ru",
            onClick = { onLanguage("ru") },
        )
        LanguageButton(
            text = "EN",
            description = strings.languageEn,
            active = language == "en",
            onClick = { onLanguage("en") },
        )
    }
}

@Composable
private fun LanguageButton(
    text: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .width(34.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) SudokuColors.White else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) SudokuColors.Teal else SudokuColors.LanguageSwitchInk,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
fun RoundIconButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(SudokuColors.RoundButtonBackground)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = SudokuColors.Teal,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun MobileStatsRow(state: GameState, strings: AppStrings, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MobileStat(strings.time, GameFormat.time(state.seconds))
        MobileStat(strings.mistakes, strings.mistakesValue(state.mistakes))
        MobileStat(strings.progress, strings.progressValue(state.progressPercent()))
    }
}

@Composable
private fun MobileStat(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text = label, color = Color(0xFF81968D), fontSize = 10.sp)
        Text(text = value, color = SudokuColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SudokuColors.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = SudokuColors.StatLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = SudokuColors.StatIcon,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                color = SudokuColors.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }
    }
}

@Composable
fun ProgressCard(
    title: String,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "progress",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SudokuColors.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                color = SudokuColors.StatLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = "$percent%",
                color = SudokuColors.Teal,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(9.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SudokuColors.Track),
        ) {
            Box(
                Modifier
                    .width(maxWidth * fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(SudokuColors.ProgressStart, SudokuColors.Teal),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = SudokuColors.Muted,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
        modifier = modifier,
    )
}

@Composable
fun DifficultySwitch(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(SudokuColors.SegmentBackground)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Difficulty.entries.forEach { difficulty ->
            val active = difficulty == state.difficulty
            Box(
                Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) SudokuColors.White else Color.Transparent)
                    .clickable { onAction(Action.SetDifficulty(difficulty)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.difficulty(difficulty),
                    color = if (active) SudokuColors.Teal else SudokuColors.SegmentInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun Keypad(state: GameState, onAction: (Action) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (number in 1..9) {
            val remaining = state.remainingFor(number)
            val active = state.selected in 0 until 81 && state.values.getOrElse(state.selected) { 0 } == number
            KeypadKey(
                number = number,
                remaining = remaining,
                active = active,
                enabled = state.canInteract && remaining > 0,
                onClick = { onAction(Action.Enter(number)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeypadKey(
    number: Int,
    remaining: Int,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) SudokuColors.Teal else SudokuColors.KeypadBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled || active) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number.toString(),
                color = if (active) SudokuColors.White else SudokuColors.KeypadInk,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = remaining.toString(),
                color = if (active) Color(0xFFCCE4DC) else SudokuColors.KeypadHint,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun ToolButton(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    trailing: String? = null,
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 15.dp)
    val background = if (active) SudokuColors.ToolActiveBackground else SudokuColors.ToolBackground
    val borderColor = if (active) SudokuColors.ToolActiveBorder else SudokuColors.ToolBorder
    val ink = if (active) SudokuColors.ToolActiveInk else SudokuColors.ToolInk

    Box(
        modifier
            .height(if (compact) 40.dp else 76.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        if (compact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    color = ink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = label,
                        color = ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (trailing != null) {
                        Text(
                            text = trailing,
                            color = SudokuColors.Muted,
                            fontSize = 9.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Four helper buttons in one row, the layout used on phones. */
@Composable
fun ToolsRow(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ToolButton(
            iconRes = R.drawable.ic_notes,
            label = strings.notes,
            onClick = { onAction(Action.ToggleNotes) },
            modifier = Modifier.weight(1f),
            active = state.noteMode,
            enabled = state.canInteract,
            compact = true,
        )
        ToolButton(
            iconRes = R.drawable.ic_hint,
            label = strings.hint,
            onClick = { onAction(Action.Hint) },
            modifier = Modifier.weight(1f),
            enabled = state.canInteract && state.hintsLeft > 0,
            compact = true,
        )
        ToolButton(
            iconRes = R.drawable.ic_erase,
            label = strings.erase,
            onClick = { onAction(Action.Erase) },
            modifier = Modifier.weight(1f),
            enabled = state.canInteract,
            compact = true,
        )
        ToolButton(
            iconRes = R.drawable.ic_undo,
            label = strings.undo,
            onClick = { onAction(Action.Undo) },
            modifier = Modifier.weight(1f),
            enabled = state.canInteract && state.canUndo,
            compact = true,
        )
    }
}

/** Two-by-two helper grid used on wide screens. */
@Composable
fun ToolsGrid(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(
                iconRes = R.drawable.ic_notes,
                label = strings.notes,
                onClick = { onAction(Action.ToggleNotes) },
                modifier = Modifier.weight(1f),
                active = state.noteMode,
                enabled = state.canInteract,
                trailing = if (state.noteMode) strings.on else strings.off,
            )
            ToolButton(
                iconRes = R.drawable.ic_hint,
                label = strings.hint,
                onClick = { onAction(Action.Hint) },
                modifier = Modifier.weight(1f),
                enabled = state.canInteract && state.hintsLeft > 0,
                trailing = strings.hintsValue(state.hintsLeft),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(
                iconRes = R.drawable.ic_erase,
                label = strings.erase,
                onClick = { onAction(Action.Erase) },
                modifier = Modifier.weight(1f),
                enabled = state.canInteract,
            )
            ToolButton(
                iconRes = R.drawable.ic_undo,
                label = strings.undo,
                onClick = { onAction(Action.Undo) },
                modifier = Modifier.weight(1f),
                enabled = state.canInteract && state.canUndo,
            )
        }
    }
}

@Composable
fun TipCard(strings: AppStrings, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SudokuColors.TipBackground)
            .padding(15.dp),
    ) {
        Text(
            text = strings.tipTitle,
            color = SudokuColors.TipTitle,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.tipBody,
            color = SudokuColors.TipInk,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )
    }
}

/** Vertical helper strip used on short landscape screens. */
@Composable
fun ToolsColumn(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.width(52.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ToolIconButton(
            iconRes = R.drawable.ic_notes,
            description = strings.notes,
            active = state.noteMode,
            enabled = state.canInteract,
            onClick = { onAction(Action.ToggleNotes) },
        )
        ToolIconButton(
            iconRes = R.drawable.ic_hint,
            description = strings.hint,
            enabled = state.canInteract && state.hintsLeft > 0,
            onClick = { onAction(Action.Hint) },
        )
        ToolIconButton(
            iconRes = R.drawable.ic_erase,
            description = strings.erase,
            enabled = state.canInteract,
            onClick = { onAction(Action.Erase) },
        )
        ToolIconButton(
            iconRes = R.drawable.ic_undo,
            description = strings.undo,
            enabled = state.canInteract && state.canUndo,
            onClick = { onAction(Action.Undo) },
        )
    }
}

@Composable
private fun ToolIconButton(
    @DrawableRes iconRes: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(52.dp)
            .clip(shape)
            .background(if (active) SudokuColors.ToolActiveBackground else SudokuColors.ToolBackground)
            .border(1.dp, if (active) SudokuColors.ToolActiveBorder else SudokuColors.ToolBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (active) SudokuColors.ToolActiveInk else SudokuColors.ToolInk,
            modifier = Modifier.size(20.dp),
        )
    }
}
