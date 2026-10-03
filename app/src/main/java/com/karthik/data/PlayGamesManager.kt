package com.karthik.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

/**
 * Singleton that wraps Google Play Games Services v2.
 *
 * Key behaviour
 * -------------
 * - Call [init] once from MainActivity.onCreate (before setContent).
 *   The v2 SDK signs in automatically in the background; no UI needed.
 * - Call [submitScore] after each run with the score in metres.
 * - Call [showLeaderboard] from any Activity to open the native leaderboard UI.
 *
 * Leaderboard IDs are read from BuildConfig so you can supply them in local.properties:
 *   games.leaderboard_all_time  = CgkI...xxxxxxxx
 *   games.leaderboard_weekly    = CgkI...yyyyyyyy
 *
 * If the IDs are not configured (dev/test build), submitting and showing are no-ops.
 */
object PlayGamesManager {

    // -----------------------------------------------------------------------
    // Public state (observable by Compose)
    // -----------------------------------------------------------------------

    /** true once the GamesSignInClient has confirmed the player is signed in */
    var isSignedIn by mutableStateOf(false)
        private set

    // -----------------------------------------------------------------------
    // Configuration  – set these to your real leaderboard IDs from Google Play Console
    // -----------------------------------------------------------------------

    /**
     * Replace with your actual leaderboard ID from the Play Console.
     * Format: "CgkI..." (22 chars)
     * You can define it in local.properties and load it in build.gradle if you prefer.
     */
    var leaderboardId: String = ""   // set via [configure] or hardcode here

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Call once from [Activity.onCreate] BEFORE [Activity.setContent].
     * The v2 SDK uses automatic sign-in (no prompt needed on most devices).
     */
    fun init(context: Context) {
        PlayGamesSdk.initialize(context)
    }

    /**
     * Optionally call after [init] to override the leaderboard IDs at runtime.
     * (Useful if you load them from BuildConfig.)
     */
    fun configure(leaderboardId: String) {
        this.leaderboardId = leaderboardId
    }

    /**
     * Checks the current sign-in status and updates [isSignedIn].
     * Call from the Activity that will show the leaderboard button.
     */
    fun refreshSignInState(activity: Activity) {
        PlayGames.getGamesSignInClient(activity)
            .isAuthenticated
            .addOnCompleteListener { task ->
                isSignedIn = task.isSuccessful && task.result?.isAuthenticated == true
            }
    }

    /**
     * Sign the player in manually (only needed if the auto sign-in was dismissed or failed).
     */
    fun signIn(activity: Activity, onResult: (Boolean) -> Unit = {}) {
        PlayGames.getGamesSignInClient(activity)
            .signIn()
            .addOnCompleteListener { task ->
                isSignedIn = task.isSuccessful
                onResult(isSignedIn)
            }
    }

    // -----------------------------------------------------------------------
    // Leaderboard
    // -----------------------------------------------------------------------

    /**
     * Submit [scoreMetres] to the all-time leaderboard.
     *
     * If already signed in, submits immediately.
     * If not yet signed in (e.g. background auth still pending), triggers sign-in
     * first and then submits — so no score is silently dropped.
     */
    fun submitScore(activity: Activity, scoreMetres: Long) {
        if (leaderboardId.isBlank()) return
        if (isSignedIn) {
            // Fast path: already authenticated
            PlayGames.getLeaderboardsClient(activity)
                .submitScore(leaderboardId, scoreMetres)
        } else {
            // Slow path: sign-in first, then submit
            PlayGames.getGamesSignInClient(activity)
                .signIn()
                .addOnCompleteListener { task ->
                    isSignedIn = task.isSuccessful
                    if (isSignedIn) {
                        PlayGames.getLeaderboardsClient(activity)
                            .submitScore(leaderboardId, scoreMetres)
                    }
                }
        }
    }

    /**
     * Launch the native Play Games leaderboard UI.
     * The Play Games SDK v2 handles silent sign-in automatically — we always
     * call [launchLeaderboardIntent] directly. Explicit sign-in is only needed
     * as a fallback when the intent request itself fails (e.g. player dismissed
     * the one-time account chooser before).
     */
    fun showLeaderboard(activity: Activity) {
        if (leaderboardId.isBlank()) return
        launchLeaderboardIntent(activity)
    }

    private fun launchLeaderboardIntent(activity: Activity) {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent ->
                isSignedIn = true          // confirmed authenticated
                activity.startActivityForResult(intent, RC_LEADERBOARD)
            }
            .addOnFailureListener {
                // Intent failed – player is not signed in; trigger explicit sign-in
                signIn(activity) { success ->
                    if (success) {
                        PlayGames.getLeaderboardsClient(activity)
                            .getLeaderboardIntent(leaderboardId)
                            .addOnSuccessListener { intent ->
                                activity.startActivityForResult(intent, RC_LEADERBOARD)
                            }
                    }
                }
            }
    }

    private const val RC_LEADERBOARD = 9004
}
