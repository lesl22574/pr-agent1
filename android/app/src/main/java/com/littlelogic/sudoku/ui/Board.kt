package com.littlelogic.sudoku.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlelogic.sudoku.game.Action
import com.littlelogic.sudoku.game.GameState
import com.littlelogic.sudoku.game.SudokuEngine
import com.littlelogic.sudoku.ui.theme.SudokuColors

private const val SIDE = 9

/**
 * The 9x9 board. Cells are real composables (crisp text, accessible labels),
 * grid lines are drawn on top with a [Canvas] so thin and heavy separators stay
 * exactly one pixel-sharp at any size.
 */
@Composable
fun SudokuBoard(
    state: GameState,
    strings: AppStrings,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(7.dp)
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .shadow(10.dp, shape)
            .clip(shape)
            .background(SudokuColors.White)
            .border(2.dp, SudokuColors.GridOuter, shape),
    ) {
        val inner = (maxWidth - 4.dp).coerceAtLeast(1.dp)
        val cell = inner / SIDE
        val numberSize = (cell.value * 0.58f).coerceIn(12f, 27f).sp
        val noteSize = (cell.value * 0.20f).coerceIn(6f, 10f).sp

        Box(
            Modifier
                .fillMaxSize()
                .padding(2.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until SIDE) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        for (col in 0 until SIDE) {
                            val index = row * SIDE + col
                            BoardCell(
                                state = state,
                                index = index,
                                numberSize = numberSize,
                                noteSize = noteSize,
                                description = strings.cellDescription(
                                    row + 1,
                                    col + 1,
                                    describeCell(state, strings, index),
                                ),
                                onAction = onAction,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
            BoardGrid(Modifier.matchParentSize())
        }
    }
}

private fun describeCell(state: GameState, strings: AppStrings, index: Int): String {
    val value = state.values.getOrElse(index) { 0 }
    if (value != 0) return value.toString()
    val notes = state.notes.getOrElse(index) { 0 }
    if (notes == 0) return strings.emptyCell
    return (1..9)
        .filter { notes and (1 shl (it - 1)) != 0 }
        .joinToString(separator = " ")
}

@Composable
private fun BoardCell(
    state: GameState,
    index: Int,
    numberSize: TextUnit,
    noteSize: TextUnit,
    description: String,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val value = state.values.getOrElse(index) { 0 }
    val notes = state.notes.getOrElse(index) { 0 }
    val given = state.givens.getOrElse(index) { 0 } != 0
    val selected = state.selected == index
    val chosen = state.values.getOrElse(state.selected) { 0 }
    val wrong = state.isWrong(index)
    val peer = !selected &&
        state.selected in 0 until SIDE * SIDE &&
        SudokuEngine.peers(index, state.selected)

    val target = when {
        state.hintPulse == index -> SudokuColors.HintFlash
        selected -> SudokuColors.SelectedCell
        wrong -> SudokuColors.WrongCell
        chosen != 0 && value == chosen -> SudokuColors.SameCell
        peer -> SudokuColors.PeerCell
        else -> SudokuColors.White
    }
    val background by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 160),
        label = "cellBackground",
    )

    val ink = when {
        selected && (value != 0 || notes != 0) -> SudokuColors.SelectedInk
        wrong -> SudokuColors.WrongInk
        chosen != 0 && value == chosen -> SudokuColors.TealDark
        given -> SudokuColors.GivenInk
        else -> SudokuColors.ValueInk
    }

    Box(
        modifier
            .background(background)
            .semantics { contentDescription = description }
            .clickable(enabled = state.canInteract) { onAction(Action.Select(index)) },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(2.dp, SudokuColors.SelectedRing),
            )
        }
        if (value != 0) {
            Text(
                text = value.toString(),
                color = ink,
                fontSize = numberSize,
                lineHeight = numberSize,
                fontWeight = if (given) FontWeight.ExtraBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        } else if (notes != 0) {
            NotesGrid(notes = notes, fontSize = noteSize, highlight = selected)
        }
    }
}

@Composable
private fun NotesGrid(notes: Int, fontSize: TextUnit, highlight: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(2.dp),
    ) {
        for (row in 0..2) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                for (col in 0..2) {
                    val number = row * 3 + col + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (notes and (1 shl (number - 1)) != 0) {
                            Text(
                                text = number.toString(),
                                color = if (highlight) SudokuColors.NotesInkActive else SudokuColors.NotesInk,
                                fontSize = fontSize,
                                lineHeight = fontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardGrid(modifier: Modifier) {
    Canvas(modifier) {
        val step = size.width / SIDE
        val thin = 1.dp.toPx()
        val heavy = 2.dp.toPx()
        for (line in 1 until SIDE) {
            val isHeavy = line % 3 == 0
            val color = if (isHeavy) SudokuColors.GridHeavy else SudokuColors.GridThin
            val width = if (isHeavy) heavy else thin
            drawLine(
                color = color,
                start = Offset(line * step, 0f),
                end = Offset(line * step, size.height),
                strokeWidth = width,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = color,
                start = Offset(0f, line * step),
                end = Offset(size.width, line * step),
                strokeWidth = width,
                cap = StrokeCap.Square,
            )
        }
    }
}
