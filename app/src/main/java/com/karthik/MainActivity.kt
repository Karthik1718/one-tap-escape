package com.karthik

import android.os.Bundle
import com.karthik.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.karthik.data.PlayGamesManager
import com.karthik.ui.MainNavigation
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable Full Screen Edge-to-Edge & Hide System Bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // Initialize AdMob and pre-load first ad
        MobileAds.initialize(this) {
            com.karthik.ads.AdManager.loadRewardedAd(this)
        }

        // Initialize audio engine
        com.karthik.audio.AudioManager.init(this)

        // Initialize Google Play Games Services (v2 auto sign-in)
        PlayGamesManager.init(this)
        // Configure your leaderboard ID here (from Play Console → Leaderboards)
        PlayGamesManager.configure(leaderboardId = BuildConfig.GAMES_LEADERBOARD_ID)
        PlayGamesManager.refreshSignInState(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh sign-in state when returning from the leaderboard/account UI
        PlayGamesManager.refreshSignInState(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Refresh sign-in state after returning from the Play Games leaderboard UI
        PlayGamesManager.refreshSignInState(this)
    }

    override fun onStart() {
        super.onStart()
        com.karthik.audio.AudioManager.startMusic()
    }

    override fun onStop() {
        super.onStop()
        com.karthik.audio.AudioManager.stopMusic()
    }
}
