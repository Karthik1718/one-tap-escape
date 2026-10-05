package com.karthik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.karthik.audio.AudioManager
import com.karthik.data.FirebaseLeaderboardManager
import com.karthik.data.LeaderboardEntry
import com.karthik.data.SaveManager

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context) }
    val currentUserId = saveManager.getUserId()

    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var lastDocSnapshot by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Auto-prompt profile setup if username is empty on screen open
    LaunchedEffect(Unit) {
        if (saveManager.username.isBlank()) {
            showProfileDialog = true
        }
    }

    fun loadPage(reset: Boolean = false) {
        if (isLoading) return
        isLoading = true

        val currentLastDoc = if (reset) null else lastDocSnapshot
        val currentOffset = if (reset) 1 else entries.size + 1

        FirebaseLeaderboardManager.fetchLeaderboardPage(
            pageSize = 10L,
            lastDocumentSnapshot = currentLastDoc,
            startingRankOffset = currentOffset
        ) { newEntries, nextLastDoc, moreAvailable ->
            isLoading = false
            lastDocSnapshot = nextLastDoc
            hasMore = moreAvailable
            entries = if (reset) newEntries else (entries + newEntries)
        }
    }

    // Initial fetch
    LaunchedEffect(Unit) {
        loadPage(reset = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Row with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        AudioManager.playSfx(context, "button")
                        onBack()
                    }
                ) {
                    Text("⬅️", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "GLOBAL LEADERBOARD",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // User Profile Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatar = saveManager.avatarId.ifBlank { "🏎️" }
                    val name = saveManager.username.ifBlank { "Set Username" }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = avatar, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Best: ${saveManager.highScore.toInt()}m",
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            AudioManager.playSfx(context, "button")
                            showProfileDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Text(if (saveManager.username.isBlank()) "Setup ✏️" else "Edit ✏️", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Leaderboard List
            if (entries.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else if (entries.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scores recorded yet.\nBe the first on the leaderboard!",
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(entries) { index, item ->
                        LeaderboardItemRow(
                            entry = item,
                            isCurrentUser = item.userId == currentUserId
                        )
                    }

                    // Load More Button / Indicator at bottom
                    item {
                        if (hasMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00E5FF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            AudioManager.playSfx(context, "button")
                                            loadPage(reset = false)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                    ) {
                                        Text("LOAD MORE (+10)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "End of Leaderboard",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Profile Modal
        if (showProfileDialog) {
            ProfileDialog(
                onDismiss = { showProfileDialog = false },
                onProfileSaved = {
                    showProfileDialog = false
                    // Reload leaderboard to show updated profile
                    loadPage(reset = true)
                }
            )
        }
    }
}

@Composable
fun LeaderboardItemRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val rankBadge = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }

    val cardBorder = when {
        isCurrentUser -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
        entry.rank == 1 -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD600))
        entry.rank == 2 -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0))
        entry.rank == 3 -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCD7F32))
        else -> null
    }

    val containerColor = if (isCurrentUser) {
        Color(0xFF00E5FF).copy(alpha = 0.15f)
    } else {
        Color(0xFF1E293B).copy(alpha = 0.85f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number / Badge
            Box(
                modifier = Modifier.width(42.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = rankBadge,
                    fontSize = if (entry.rank <= 3) 22.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (entry.rank <= 3) Color.White else Color.White.copy(alpha = 0.7f)
                )
            }

            // Avatar
            Text(
                text = entry.avatarId.ifBlank { "🏎️" },
                fontSize = 26.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Username
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.username,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF00E5FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Score
            Text(
                text = "${entry.highScore}m",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF)
            )
        }
    }
}
