package com.littlelogic.sudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFF7F5EE)
val Ink = Color(0xFF233A43)
val Muted = Color(0xFF84939A)
val Teal = Color(0xFF277D74)
val TealDark = Color(0xFF17685F)
val TealSoft = Color(0xFFE4F1EB)
val Coral = Color(0xFFED9273)
val Line = Color(0xFFE2E9E5)
val GridThin = Color(0xFFDCE7E3)
val GridThick = Color(0xFF77918D)
val CellPeer = Color(0xFFF2F7F3)
val CellSame = Color(0xFFE2F0E8)
val CellSelected = Color(0xFFCCE7DC)
val CellWrong = Color(0xFFFFF0EA)
val CellHint = Color(0xFFF5DFAB)
val KeyBackground = Color(0xFFF5F8F4)
val GivenInk = Color(0xFF2D4650)
val ValueInk = Color(0xFF387A71)
val WrongInk = Color(0xFFD56F56)
val NoteInk = Color(0xFF73938B)
val ProgressTrack = Color(0xFFEAF1E9)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Coral,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = KeyBackground,
    onSurfaceVariant = Muted,
    outline = Line,
)

@Composable
fun LittleLogicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
