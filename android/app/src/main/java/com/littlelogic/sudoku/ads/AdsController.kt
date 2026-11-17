package com.littlelogic.sudoku.ads

import android.app.Activity
import android.util.Log
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener

private const val TAG = "YandexAds"

/**
 * Owns the interstitial ad of the screen (Yandex Mobile Ads SDK 5.x API):
 *  - preloads one ad after SDK initialization;
 *  - shows it exactly once at the pause between a solved level and the next one;
 *  - never shows it during gameplay, on a new game, or after a loss;
 *  - continues the game when an ad is unavailable.
 */
class AdsController(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private var isReady = false
    private var isShowing = false
    private var released = false
    private var pendingFinish: (() -> Unit)? = null

    fun preload() {
        if (released || isReady || isShowing || interstitialAd != null) return
        val ad = InterstitialAd(activity)
        ad.setAdUnitId(AdsConfig.INTERSTITIAL_AD_UNIT_ID)
        ad.setInterstitialAdEventListener(object : InterstitialAdEventListener {
            override fun onAdLoaded() {
                if (interstitialAd === ad) {
                    isReady = true
                    Log.d(TAG, "interstitial loaded")
                }
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.d(TAG, "interstitial unavailable: ${error.description}")
                // Deliberate pause before the next attempt: no retry from this callback.
                if (interstitialAd === ad) {
                    interstitialAd = null
                    isReady = false
                    ad.destroy()
                }
            }

            override fun onAdShown() {
                isShowing = true
            }

            override fun onAdDismissed() {
                finishShowing()
            }

            override fun onAdClicked() = Unit

            override fun onImpression(data: ImpressionData?) = Unit

            override fun onLeftApplication() = Unit

            override fun onReturnedToApplication() = Unit
        })
        interstitialAd = ad
        ad.loadAd(AdRequest.Builder().build())
    }

    /** Shows the ad at a level transition and always calls [onFinished] once. */
    fun showBetweenLevels(onFinished: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || !isReady) {
            onFinished()
            preload()
            return
        }
        pendingFinish = onFinished
        runCatching { ad.show() }
            .onFailure { throwable ->
                Log.d(TAG, "interstitial show skipped", throwable)
                finishShowing()
            }
    }

    private fun finishShowing() {
        val callback = pendingFinish
        pendingFinish = null
        isShowing = false
        isReady = false
        val ad = interstitialAd
        interstitialAd = null
        runCatching { ad?.destroy() }
        callback?.invoke()
        preload()
    }

    fun release() {
        released = true
        pendingFinish = null
        isShowing = false
        isReady = false
        runCatching { interstitialAd?.destroy() }
        interstitialAd = null
    }
}
