package com.littlelogic.sudoku

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.key
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader

@Composable
internal fun YandexBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val adWidthDp = maxWidth.value.roundToInt()
        if (adWidthDp > 0) {
            key(adWidthDp) {
                AndroidView(
                    factory = { viewContext ->
                        BannerAdView(viewContext).apply {
                            setAdSize(BannerAdSize.sticky(viewContext, adWidthDp))
                            setBannerAdEventListener(object : BannerAdEventListener {
                                override fun onAdLoaded() = Unit
                                override fun onAdFailedToLoad(adRequestError: AdRequestError) = Unit
                                override fun onAdClicked() = Unit
                                override fun onLeftApplication() = Unit
                                override fun onReturnedToApplication() = Unit
                                override fun onImpression(impressionData: ImpressionData?) = Unit
                            })
                            loadAd(AdRequest.Builder(BuildConfig.YANDEX_BANNER_AD_UNIT_ID).build())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    onRelease = { banner -> banner.destroy() },
                )
            }
        }
    }
}

/** Loads one interstitial ahead of time and shows it only on the level-advance action. */
internal class YandexInterstitialController(private val activity: Activity) {
    private var loader: InterstitialAdLoader? = InterstitialAdLoader(activity)
    private var loadedAd: InterstitialAd? = null
    private var isLoading = false
    private var isDisposed = false

    fun preload() {
        if (isDisposed || isLoading || loadedAd != null) return
        val currentLoader = loader ?: return
        isLoading = true
        currentLoader.loadAd(
            AdRequest.Builder(BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID).build(),
            object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    if (isDisposed) {
                        ad.setAdEventListener(null)
                    } else {
                        loadedAd = ad
                    }
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    isLoading = false
                }
            },
        )
    }

    fun showBetweenLevels(onContinue: () -> Unit) {
        val ad = loadedAd
        if (ad == null || isDisposed) {
            onContinue()
            preload()
            return
        }
        loadedAd = null
        var continued = false
        fun finishTransition() {
            if (continued) return
            continued = true
            ad.setAdEventListener(null)
            if (!isDisposed) preload()
            onContinue()
        }

        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() = Unit
            override fun onAdFailedToShow(adError: AdError) = finishTransition()
            override fun onAdDismissed() = finishTransition()
            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: ImpressionData?) = Unit
        })
        try {
            ad.show(activity)
        } catch (_: RuntimeException) {
            finishTransition()
        }
    }

    fun destroy() {
        isDisposed = true
        loader = null
        loadedAd?.setAdEventListener(null)
        loadedAd = null
    }
}
