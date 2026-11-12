package com.littlelogic.sudoku

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littlelogic.sudoku.ui.GameViewModel
import com.littlelogic.sudoku.ui.SudokuApp

/**
 * The whole game lives in a single activity: the board is drawn with Compose and
 * all rules sit in the pure Kotlin [com.littlelogic.sudoku.game] package.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val viewModel: GameViewModel = viewModel()
            SudokuApp(viewModel)
        }
    }
}
