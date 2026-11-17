package com.littlelogic.sudoku.ads

import android.content.Context
import com.yandex.mobile.ads.common.MobileAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manual initialization used by Yandex Mobile Ads SDK 5.x. */
object AdsSdk {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initialize(context: Context) {
        // Privacy settings (user consent, COPPA, location) belong here, before init.
        MobileAds.initialize(context.applicationContext) {
            _ready.value = true
        }
    }
}
