package com.littlelogic.sudoku

import android.app.Application
import com.littlelogic.sudoku.ads.AdsSdk

class LittleLogicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdsSdk.initialize(this)
    }
}
