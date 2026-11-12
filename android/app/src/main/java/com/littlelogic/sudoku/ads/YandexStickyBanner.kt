package com.littlelogic.sudoku.ads

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlin.math.roundToInt

private const val TAG = "YandexAds"

/**
 * Adaptive sticky banner pinned to the bottom of the screen. It takes the full
 * width of its container and never overlaps the board. SDK calls run on the
 * main thread; [DisposableEffect] releases the view with the composition.
 */
@Composable
fun YandexStickyBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val adWidthDp = maxWidth.value.roundToInt()
        val adSize = remember(context, adWidthDp) { BannerAdSize.sticky(context, adWidthDp) }
        val heightDp = with(LocalDensity.current) {
            adSize.getHeightInPixels(context).toFloat().toDp()
        }

        val banner = remember(adSize) {
            BannerAdView(context).apply {
                setAdSize(adSize)
                setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        Log.d(TAG, "banner loaded")
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.d(TAG, "banner unavailable: ${error.code} ${error.description}")
                    }

                    override fun onAdClicked() = Unit

                    override fun onImpression(data: ImpressionData?) = Unit
                })
            }
        }

        LaunchedEffect(banner) {
            banner.loadAd(AdRequest.Builder(AdsConfig.BANNER_AD_UNIT_ID).build())
        }

        DisposableEffect(banner) {
            onDispose { banner.destroy() }
        }

        AndroidView(
            factory = { banner },
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp),
        )
    }
}
