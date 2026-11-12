package com.littlelogic.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette of the original web design. */
object SudokuColors {
    val Paper = Color(0xFFF7F5EE)
    val Ink = Color(0xFF233A43)
    val Muted = Color(0xFF84939A)
    val Teal = Color(0xFF277D74)
    val TealDark = Color(0xFF17685F)
    val TealSoft = Color(0xFFE4F1EB)
    val Coral = Color(0xFFED9273)
    val Line = Color(0xFFE2E9E5)
    val White = Color(0xFFFFFFFF)

    // Board
    val GridOuter = Color(0xFF637B79)
    val GridHeavy = Color(0xFF77918D)
    val GridThin = Color(0xFFDCE7E3)
    val GivenInk = Color(0xFF2D4650)
    val ValueInk = Color(0xFF387A71)
    val NotesInk = Color(0xFF73938B)
    val NotesInkActive = Color(0xFF216F62)
    val PeerCell = Color(0xFFF2F7F3)
    val SameCell = Color(0xFFE2F0E8)
    val SelectedCell = Color(0xFFCCE7DC)
    val SelectedInk = Color(0xFF16594F)
    val SelectedRing = Color(0xFF4A9886)
    val WrongCell = Color(0xFFFFF0EA)
    val WrongInk = Color(0xFFD56F56)
    val HintFlash = Color(0xFFF5DFAB)

    // Controls
    val KeypadBackground = Color(0xFFF5F8F4)
    val KeypadInk = Color(0xFF327D70)
    val KeypadHint = Color(0xFF9CAB9E)
    val SegmentBackground = Color(0xFFF5F7F3)
    val SegmentInk = Color(0xFF929F9D)
    val RoundButtonBackground = Color(0xFFF3F6F1)

    val ToolBackground = Color(0xFFFFFFFF)
    val ToolBorder = Color(0xFFF0F2ED)
    val ToolInk = Color(0xFF53736F)
    val ToolActiveBackground = Color(0xFFE6F3E9)
    val ToolActiveBorder = Color(0xFF9BCBB8)
    val ToolActiveInk = Color(0xFF166D5E)

    val StatLabel = Color(0xFF9AA7A2)
    val StatIcon = Color(0xFF91AEA3)
    val Track = Color(0xFFEAF1E9)
    val ProgressStart = Color(0xFF64B49B)

    val TipBackground = Color(0xFFEAF1E9)
    val TipInk = Color(0xFF66837A)
    val TipTitle = Color(0xFF307D70)

    val LanguageSwitchBackground = Color(0xFFEBECE6)
    val LanguageSwitchInk = Color(0xFF87928E)

    val Scrim = Color(0x8C19342F)
    val ToastBackground = Color(0xFF263F43)
    val ModalArtBackground = Color(0xFFE5F3E9)
    val ModalArtInk = Color(0xFF2C8D78)
    val ModalKicker = Color(0xFF51A18B)

    val CardShadow = Color(0x18274A46)
    val CardBorder = Color(0xFFFFFFFF)
    val FooterInk = Color(0xFFACB6AC)
    val BackdropOrb = Color(0xFFE8EEE3)
    val BackdropRing = Color(0xFFF3E9DC)
    val BackdropSpot = Color(0xFFF4E7D8)
}

/** Serif display face for headings, system sans for the rest. */
object SudokuType {
    val Display = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-1).sp,
        color = SudokuColors.Ink,
    )

    val Title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.6).sp,
        color = SudokuColors.Ink,
    )

    val Kicker = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
        color = SudokuColors.Muted,
    )
}

private val SudokuColorScheme = lightColorScheme(
    primary = SudokuColors.Teal,
    onPrimary = SudokuColors.White,
    primaryContainer = SudokuColors.TealSoft,
    onPrimaryContainer = SudokuColors.TealDark,
    secondary = SudokuColors.Coral,
    onSecondary = SudokuColors.White,
    background = SudokuColors.Paper,
    onBackground = SudokuColors.Ink,
    surface = SudokuColors.White,
    onSurface = SudokuColors.Ink,
    surfaceVariant = SudokuColors.TealSoft,
    onSurfaceVariant = SudokuColors.Muted,
    outline = SudokuColors.Line,
    error = SudokuColors.WrongInk,
)

private val SudokuTypography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = SudokuColors.Ink),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, color = SudokuColors.Ink),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, color = SudokuColors.Muted),
    titleLarge = SudokuType.Title,
    headlineLarge = SudokuType.Display,
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SudokuColorScheme,
        typography = SudokuTypography,
        content = content,
    )
}
