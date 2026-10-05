package com.karthik.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A lightweight snapshot that records the ghost car state at a given timestamp.
 * Stores timestamp, distanceTravelled, and laneIndex.
 */
data class GhostSnapshot(
    val timestamp: Float,          // Elapsed play time in seconds when snapshot was taken
    val distanceTravelled: Float,  // Player's distance travelled at timestamp
    val laneIndex: Int             // -1, 0, 1 (same range as player)
)

/**
 * Manages recording and replaying a ghost run.
 *
 * Recording: call [record] every game-loop tick while PLAYING.
 *            call [commitIfBest] at game-over; it persists the snapshots if the score beat the best.
 *
 * Replaying: call [updateGhostX] with deltaTime and current distanceTravelled.
 */
class GhostRecorder(private val context: Context) {

    companion object {
        private const val PREFS = "ghost_run_prefs"
        private const val KEY_COUNT = "snap_count"
        private const val KEY_TIME_PREFIX = "snap_time_"
        private const val KEY_DIST_PREFIX = "snap_dist_"
        private const val KEY_LANE_PREFIX = "snap_lane_"
        private const val SAMPLE_INTERVAL = 0.05f   // Record snapshot every 0.05 seconds (20 fps)
    }

    // ---- current-run recording ----
    private val currentSnapshots = mutableListOf<GhostSnapshot>()
    private var elapsedTime = 0f
    private var lastSampledTime = -SAMPLE_INTERVAL

    // ---- ghost-replay state (loaded from prefs, replayed each run) ----
    private var ghostSnapshots: List<GhostSnapshot> = emptyList()
    private var ghostTime = 0f

    var hasGhost by mutableStateOf(false)
        private set

    var isGhostActive by mutableStateOf(false)
        private set

    // Interpolated ghost X (world units). Read by GameEngine / Canvas.
    var ghostX by mutableStateOf(0f)
        private set

    // How far ahead/behind the ghost is (positive = ghost is ahead, negative = player is ahead)
    var ghostDelta by mutableStateOf(0f)

    val ghostMaxDistance: Float
        get() = ghostSnapshots.lastOrNull()?.distanceTravelled ?: 0f

    init {
        loadGhostFromPrefs()
    }

    // ------------------------------------------------------------------
    //  Recording
    // ------------------------------------------------------------------

    fun startRecording() {
        currentSnapshots.clear()
        elapsedTime = 0f
        lastSampledTime = -SAMPLE_INTERVAL
        ghostTime = 0f
        isGhostActive = hasGhost && ghostSnapshots.isNotEmpty()
    }

    fun record(deltaTime: Float, distanceTravelled: Float, laneIndex: Int) {
        elapsedTime += deltaTime
        if (elapsedTime - lastSampledTime >= SAMPLE_INTERVAL) {
            currentSnapshots.add(GhostSnapshot(elapsedTime, distanceTravelled, laneIndex))
            lastSampledTime = elapsedTime
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
            isGhostActive = true
        }
        currentSnapshots.clear()
        elapsedTime = 0f
        lastSampledTime = -SAMPLE_INTERVAL
        ghostTime = 0f
    }

    // ------------------------------------------------------------------
    //  Replaying
    // ------------------------------------------------------------------

    /**
     * Updates interpolated ghost X (world units) and ghostDelta based on [deltaTime] and [currentDistance].
     * Must be called every frame during PLAYING.
     */
    fun updateGhostX(deltaTime: Float, currentDistance: Float, horizontalOffset: Float): Float {
        if (!hasGhost || ghostSnapshots.isEmpty()) {
            isGhostActive = false
            return 0f
        }

        ghostTime += deltaTime

        val snaps = ghostSnapshots
        val firstSnap = snaps.first()
        val lastSnap = snaps.last()

        // 1. Ghost time has reached/exceeded end of recorded run
        if (ghostTime >= lastSnap.timestamp) {
            ghostX = lastSnap.laneIndex * horizontalOffset
            val ghostDist = lastSnap.distanceTravelled
            ghostDelta = ghostDist - currentDistance

            // Deactivate only once player has also reached/passed the ghost's max distance
            if (currentDistance >= ghostDist) {
                isGhostActive = false
            } else {
                isGhostActive = true
            }
            return ghostX
        }

        isGhostActive = true

        // 2. Before ghost's first timestamp
        if (ghostTime <= firstSnap.timestamp) {
            ghostX = firstSnap.laneIndex * horizontalOffset
            ghostDelta = firstSnap.distanceTravelled - currentDistance
            return ghostX
        }

        // 3. Binary search for bracket around ghostTime
        var lo = 0
        var hi = snaps.size - 1
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (snaps[mid].timestamp <= ghostTime) lo = mid else hi = mid
        }

        val a = snaps[lo]
        val b = snaps[hi]
        val timeDiff = (b.timestamp - a.timestamp).coerceAtLeast(0.0001f)
        val t = ((ghostTime - a.timestamp) / timeDiff).coerceIn(0f, 1f)

        val laneInterp = a.laneIndex + t * (b.laneIndex - a.laneIndex)
        val ghostDist = a.distanceTravelled + t * (b.distanceTravelled - a.distanceTravelled)

        ghostX = laneInterp * horizontalOffset
        ghostDelta = ghostDist - currentDistance

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
            editor.putFloat("$KEY_TIME_PREFIX$i", snap.timestamp)
            editor.putFloat("$KEY_DIST_PREFIX$i", snap.distanceTravelled)
            editor.putInt("$KEY_LANE_PREFIX$i", snap.laneIndex)
        }
        editor.apply()
    }

    private fun loadGhostFromPrefs() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_COUNT, 0)
        if (count == 0) {
            hasGhost = false
            isGhostActive = false
            return
        }

        // Handle legacy prefs missing timestamp data
        if (!prefs.contains("${KEY_TIME_PREFIX}0")) {
            prefs.edit().clear().apply()
            hasGhost = false
            isGhostActive = false
            return
        }

        val snaps = ArrayList<GhostSnapshot>(count)
        for (i in 0 until count) {
            val time = prefs.getFloat("$KEY_TIME_PREFIX$i", 0f)
            val dist = prefs.getFloat("$KEY_DIST_PREFIX$i", 0f)
            val lane = prefs.getInt("$KEY_LANE_PREFIX$i", 0)
            snaps.add(GhostSnapshot(time, dist, lane))
        }
        ghostSnapshots = snaps
        hasGhost = true
        isGhostActive = true
    }
}
