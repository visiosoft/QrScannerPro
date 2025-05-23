package mpo.qrcodescanner.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdsManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var hasShownInitialAd = false

    init {
        MobileAds.initialize(context)
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712", // Test ad unit ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInitialInterstitialAd(activity: android.app.Activity) {
        if (!hasShownInitialAd) {
            interstitialAd?.show(activity)
            hasShownInitialAd = true
            loadInterstitialAd() // Load the next ad
        }
    }
} 