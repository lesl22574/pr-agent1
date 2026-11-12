package com.littlelogic.sudoku.ads

import android.app.Activity
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader

private const val TAG = "YandexAds"

/**
 * Owns the interstitial ad for the whole activity:
 *  - one loader instance, reused for every preload (SDK recommendation);
 *  - shown exactly once at the pause between a solved level and the next one;
 *  - never shown during gameplay, on a new game, or after a loss;
 *  - a missing or failed ad simply continues the game.
 */
class AdsController(private val activity: Activity) {
    private var loader: InterstitialAdLoader? = InterstitialAdLoader(activity)
    private var interstitialAd: InterstitialAd? = null
    private var isShowing = false

    fun preload() {
        if (isShowing || interstitialAd != null || loader == null) return
        loader?.loadAd(
            AdRequest.Builder(AdsConfig.INTERSTITIAL_AD_UNIT_ID).build(),
            object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "interstitial loaded")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    // Deliberate pause before the next attempt: no retry from this callback.
                    Log.d(TAG, "interstitial unavailable: ${error.code} ${error.description}")
                }
            },
        )
    }

    /** Shows the ad at a level transition and always calls [onFinished] once. */
    fun showBetweenLevels(onFinished: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onFinished()
            preload()
            return
        }

        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            isShowing = false
            ad.setAdEventListener(null)
            interstitialAd = null
            onFinished()
            preload()
        }

        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                isShowing = true
            }

            override fun onAdDismissed() {
                finish()
            }

            override fun onAdFailedToShow(error: AdError) {
                Log.d(TAG, "interstitial not shown: ${error.description}")
                finish()
            }

            override fun onAdClicked() = Unit

            override fun onImpression(data: ImpressionData?) = Unit
        })

        runCatching { ad.show(activity) }
            .onFailure { throwable ->
                Log.d(TAG, "interstitial show skipped", throwable)
                finish()
            }
    }

    fun release() {
        loader?.cancelLoading()
        loader = null
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
    }
}
