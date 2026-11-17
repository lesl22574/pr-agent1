package com.littlelogic.sudoku.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.AdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData

private const val TAG = "YandexAds"

/**
 * Banner pinned to the bottom of the screen in the Scaffold bottom bar.
 * SDK 5.x uses a fixed 320x50 dp banner; it is centred and never overlaps the board.
 */
@Composable
fun YandexStickyBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val banner = remember {
        BannerAdView(context).apply {
            setAdUnitId(AdsConfig.BANNER_AD_UNIT_ID)
            setAdSize(AdSize.BANNER_320x50)
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    Log.d(TAG, "banner loaded")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.d(TAG, "banner unavailable: ${error.description}")
                }

                override fun onImpression(data: ImpressionData?) = Unit

                override fun onAdClicked() = Unit

                override fun onLeftApplication() = Unit

                override fun onReturnedToApplication() = Unit
            })
        }
    }

    LaunchedEffect(banner) {
        banner.loadAd(AdRequest.Builder().build())
    }

    Box(
        modifier = modifier.fillMaxWidth().height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { banner },
            modifier = Modifier.width(320.dp).height(50.dp),
        )
    }
}
