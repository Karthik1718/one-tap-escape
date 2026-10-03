package com.karthik.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A lightweight snapshot that records the ghost car state at a given distance.
 * We only need distance + laneIndex; the engine lerps the X from there.
 */
data class GhostSnapshot(
    val distanceTravelled: Float,
    val laneIndex: Int          // -1, 0, 1  (same range as player)
)

/**
 * Manages recording and replaying a ghost run.
 *
 * Recording: call [record] every game-loop tick while PLAYING.
 *            call [commitIfBest] at game-over; it persists the snapshots if the score beat the best.
 *
 * Replaying: call [ghostXAt] with the current distanceTravelled to get the ghost's
 *            target X offset (in game-world units, same as engine.playerX).
 *            Returns null if no ghost data is available yet.
 */
class GhostRecorder(private val context: Context) {

    companion object {
        private const val PREFS = "ghost_run_prefs"
        private const val KEY_COUNT = "snap_count"
        private const val KEY_DIST_PREFIX = "snap_dist_"
        private const val KEY_LANE_PREFIX = "snap_lane_"
        private const val SAMPLE_INTERVAL = 0.1f   // Record one snapshot every ~0.1 game-world units of distance
    }

    // ---- current-run recording ----
    private val currentSnapshots = mutableListOf<GhostSnapshot>()
    private var lastSampledDistance = -SAMPLE_INTERVAL

    // ---- ghost-replay state (loaded from prefs, replayed each run) ----
    private var ghostSnapshots: List<GhostSnapshot> = emptyList()
    var hasGhost by mutableStateOf(false)
        private set

    // Interpolated ghost X (world units). Read by GameEngine / Canvas.
    var ghostX by mutableStateOf(0f)
        private set

    // How far ahead/behind the ghost is, for UI label
    var ghostDelta by mutableStateOf(0f)   // positive = ghost is ahead

    init {
        loadGhostFromPrefs()
    }

    // ------------------------------------------------------------------
    //  Recording
    // ------------------------------------------------------------------

    fun startRecording() {
        currentSnapshots.clear()
        lastSampledDistance = -SAMPLE_INTERVAL
    }

    fun record(distanceTravelled: Float, laneIndex: Int) {
        if (distanceTravelled - lastSampledDistance >= SAMPLE_INTERVAL) {
            currentSnapshots.add(GhostSnapshot(distanceTravelled, laneIndex))
            lastSampledDistance = distanceTravelled
        }
    }

    /**
     * At game-over: if [finalScore] > [previousBest], persist this run as the new ghost.
     */
    fun commitIfBest(finalScore: Float, previousBest: Float) {
        if (finalScore > previousBest && currentSnapshots.isNotEmpty()) {
            saveGhostToPrefs(currentSnapshots)
            ghostSnapshots = currentSnapshots.toList()
            hasGhost = true
        }
        currentSnapshots.clear()
        lastSampledDistance = -SAMPLE_INTERVAL
    }

    // ------------------------------------------------------------------
    //  Replaying
    // ------------------------------------------------------------------

    /**
     * Returns the interpolated ghost X (world units) at [currentDistance].
     * Must be called every frame during PLAYING.
     */
    fun updateGhostX(currentDistance: Float, horizontalOffset: Float): Float {
        if (!hasGhost || ghostSnapshots.isEmpty()) return 0f

        // Binary-search for the right bracket
        val snaps = ghostSnapshots
        if (currentDistance <= snaps.first().distanceTravelled) {
            ghostX = snaps.first().laneIndex * horizontalOffset
            ghostDelta = snaps.first().distanceTravelled - currentDistance
            return ghostX
        }
        if (currentDistance >= snaps.last().distanceTravelled) {
            ghostX = snaps.last().laneIndex * horizontalOffset
            ghostDelta = snaps.last().distanceTravelled - currentDistance  // will be ≤ 0
            return ghostX
        }

        var lo = 0
        var hi = snaps.size - 1
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (snaps[mid].distanceTravelled <= currentDistance) lo = mid else hi = mid
        }

        val a = snaps[lo]
        val b = snaps[hi]
        val t = ((currentDistance - a.distanceTravelled) / (b.distanceTravelled - a.distanceTravelled)).coerceIn(0f, 1f)
        val laneInterp = a.laneIndex + t * (b.laneIndex - a.laneIndex)
        ghostX = laneInterp * horizontalOffset

        // delta: ghost best-run distance vs current distance
        // The ghost is "at" a.distanceTravelled world position; player is at currentDistance.
        // positive delta means ghost is ahead (ghost was further along at this time).
        ghostDelta = a.distanceTravelled - currentDistance

        return ghostX
    }

    // ------------------------------------------------------------------
    //  Persistence
    // ------------------------------------------------------------------

    private fun saveGhostToPrefs(snaps: List<GhostSnapshot>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(KEY_COUNT, snaps.size)
        snaps.forEachIndexed { i, snap ->
            editor.putFloat("$KEY_DIST_PREFIX$i", snap.distanceTravelled)
            editor.putInt("$KEY_LANE_PREFIX$i", snap.laneIndex)
        }
        editor.apply()
    }

    private fun loadGhostFromPrefs() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_COUNT, 0)
        if (count == 0) { hasGhost = false; return }

        val snaps = ArrayList<GhostSnapshot>(count)
        for (i in 0 until count) {
            val dist = prefs.getFloat("$KEY_DIST_PREFIX$i", 0f)
            val lane = prefs.getInt("$KEY_LANE_PREFIX$i", 0)
            snaps.add(GhostSnapshot(dist, lane))
        }
        ghostSnapshots = snaps
        hasGhost = true
    }
}
