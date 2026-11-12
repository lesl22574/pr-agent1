package com.littlelogic.sudoku.ads

import android.content.Context
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manual SDK initialization. Automatic initialization is disabled in the
 * manifest, so the game knows exactly when it is safe to request ads.
 */
object AdsSdk {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initialize(context: Context) {
        // Privacy settings (user consent, COPPA, location) belong here, before init.
        YandexAds.initialize(context.applicationContext) {
            _ready.value = true
        }
    }
}
