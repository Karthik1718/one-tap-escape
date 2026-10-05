package com.karthik.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val avatarId: String = "🏎️",
    val highScore: Long = 0L,
    val rank: Int = 0,
    val updatedAt: Long = 0L
)

object FirebaseLeaderboardManager {
    private const val TAG = "FirebaseLeaderboard"
    private const val COLLECTION_LEADERBOARD = "leaderboard"

    // Safe check if Firebase is initialized
    private val firestore: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase not initialized: ${e.message}")
                null
            }
        }

    fun isFirebaseAvailable(): Boolean = firestore != null

    /**
     * Checks if a username is already taken by another user (case-insensitive).
     */
    fun checkUsernameAvailability(
        username: String,
        currentUserId: String,
        onResult: (isAvailable: Boolean) -> Unit
    ) {
        val db = firestore
        val cleanName = username.trim()
        if (cleanName.isBlank()) {
            onResult(false)
            return
        }

        if (db == null) {
            // Firebase unavailable fallback: assume available
            onResult(true)
            return
        }

        db.collection(COLLECTION_LEADERBOARD)
            .whereEqualTo("usernameLower", cleanName.lowercase())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val isTakenByOther = querySnapshot.documents.any { doc ->
                    val id = doc.getString("userId") ?: doc.id
                    id != currentUserId
                }
                onResult(!isTakenByOther)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking username: ${e.message}")
                // On failure/offline, allow user to proceed
                onResult(true)
            }
    }

    /**
     * Save user profile and current high score to Firestore.
     */
    fun saveUserProfileAndScore(
        userId: String,
        username: String,
        avatarId: String,
        score: Long,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (userId.isBlank() || username.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val db = firestore
        if (db == null) {
            Log.w(TAG, "Firebase unavailable, score saved locally only.")
            onComplete?.invoke(false)
            return
        }

        val userDoc = mapOf(
            "userId" to userId,
            "username" to username,
            "usernameLower" to username.trim().lowercase(),
            "avatarId" to avatarId.ifBlank { "🏎️" },
            "highScore" to score,
            "updatedAt" to System.currentTimeMillis()
        )

        // Store with document ID equal to userId so each user has exactly 1 entry
        db.collection(COLLECTION_LEADERBOARD)
            .document(userId)
            .get()
            .addOnSuccessListener { existingDoc ->
                val existingScore = existingDoc.getLong("highScore") ?: 0L
                val maxScore = maxOf(score, existingScore)

                val updatedPayload = userDoc.toMutableMap()
                updatedPayload["highScore"] = maxScore

                db.collection(COLLECTION_LEADERBOARD)
                    .document(userId)
                    .set(updatedPayload)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced score to Firebase: $maxScore")
                        onComplete?.invoke(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to sync score to Firebase: ${e.message}")
                        onComplete?.invoke(false)
                    }
            }
            .addOnFailureListener {
                // If doc fetch fails, write directly
                db.collection(COLLECTION_LEADERBOARD)
                    .document(userId)
                    .set(userDoc)
                    .addOnSuccessListener { onComplete?.invoke(true) }
                    .addOnFailureListener { onComplete?.invoke(false) }
            }
    }

    /**
     * Fetch paginated leaderboard entries sorted by high score descending.
     */
    fun fetchLeaderboardPage(
        pageSize: Long = 10L,
        lastDocumentSnapshot: DocumentSnapshot? = null,
        startingRankOffset: Int = 1,
        onResult: (entries: List<LeaderboardEntry>, nextLastDoc: DocumentSnapshot?, hasMore: Boolean) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            // Mock fallback data if Firebase is not connected yet
            val mockList = listOf(
                LeaderboardEntry("mock_1", "SpeedRacer", "🏎️", 2500L, 1),
                LeaderboardEntry("mock_2", "CyberCop", "🚓", 1850L, 2),
                LeaderboardEntry("mock_3", "NeonVolt", "⚡", 1420L, 3),
                LeaderboardEntry("mock_4", "RoboDriver", "🤖", 980L, 4)
            )
            onResult(mockList, null, false)
            return
        }

        var query = db.collection(COLLECTION_LEADERBOARD)
            .orderBy("highScore", Query.Direction.DESCENDING)
            .limit(pageSize)

        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                val entries = documents.mapIndexed { index, doc ->
                    LeaderboardEntry(
                        userId = doc.getString("userId") ?: doc.id,
                        username = doc.getString("username") ?: "Anonymous",
                        avatarId = doc.getString("avatarId") ?: "🏎️",
                        highScore = doc.getLong("highScore") ?: 0L,
                        rank = startingRankOffset + index,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                }
                val newLastDoc = if (documents.isNotEmpty()) documents.last() else null
                val hasMore = documents.size.toLong() == pageSize
                onResult(entries, newLastDoc, hasMore)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch leaderboard: ${e.message}")
                onResult(emptyList(), null, false)
            }
    }
}
