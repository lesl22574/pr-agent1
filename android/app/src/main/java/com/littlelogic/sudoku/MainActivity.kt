package com.littlelogic.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littlelogic.sudoku.ads.AdsController
import com.littlelogic.sudoku.ads.AdsSdk
import com.littlelogic.sudoku.game.SudokuViewModel
import com.littlelogic.sudoku.ui.LittleLogicTheme
import com.littlelogic.sudoku.ui.SudokuApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LittleLogicTheme {
                val viewModel: SudokuViewModel = viewModel()
                val adsController = remember { AdsController(this@MainActivity) }
                val adsReady by AdsSdk.ready.collectAsState()

                LaunchedEffect(adsReady) {
                    if (adsReady) adsController.preload()
                }

                DisposableEffect(adsController) {
                    onDispose { adsController.release() }
                }

                SudokuApp(
                    viewModel = viewModel,
                    adsController = adsController,
                    adsReady = adsReady,
                )
            }
        }
    }
}
