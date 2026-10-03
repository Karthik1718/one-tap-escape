package com.karthik.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.karthik.BuildConfig

object AdManager {
    private const val TAG = "AdManager"
    
    // Ad Unit ID fetched from local.properties via build.gradle
    private val REWARDED_AD_UNIT_ID = BuildConfig.ADMOB_REWARDED_ID

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    fun loadRewardedAd(activity: Activity) {
        if (rewardedAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                rewardedAd = null
                isAdLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
                isAdLoading = false
            }
        })
    }

    fun showRewardedAd(activity: Activity, onDismissed: (() -> Unit)? = null, onRewardEarned: () -> Unit) {
        var wasRewardEarned = false

        rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
                rewardedAd = null
                if (wasRewardEarned) {
                    onRewardEarned()
                }
                onDismissed?.invoke()
                loadRewardedAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Ad failed to show: ${error.message}")
                rewardedAd = null
                onDismissed?.invoke()
                loadRewardedAd(activity)
            }
        }

        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount}")
                wasRewardEarned = true
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            loadRewardedAd(activity)
            onDismissed?.invoke()
            android.widget.Toast.makeText(activity, "Ad not ready, try again in a moment!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun isAdAvailable(): Boolean = rewardedAd != null
}
