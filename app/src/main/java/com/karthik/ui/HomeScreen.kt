package com.karthik.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthik.audio.AudioManager
import com.karthik.data.PlayGamesManager

@Composable
fun HomeScreen(
    onPlayClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val context = LocalContext.current
    val saveManager = remember { com.karthik.data.SaveManager(context) }
    val highScore = saveManager.highScore
    val coins = saveManager.totalCoins
    val selectedModel = saveManager.getSelectedModel()
    var showTutorial by remember { mutableStateOf(false) }

    val modelName = when(selectedModel) {
        "CYBER_TRUCK" -> "🚚 Cyber Truck"
        "POLICE_CRUISER" -> "🚓 Police Cruiser"
        "SUPER_BOLT" -> "⚡ Super Bolt"
        else -> "🏎️ Speed Racer"
    }

    // Mute state hoisted so it's accessible in the overlay
    var isMuted by remember { mutableStateOf(AudioManager.isMuted()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))
    ) {
        // Volume toggle – top-end corner
        IconButton(
            onClick = { isMuted = AudioManager.toggleMute() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Text(if (isMuted) "🔇" else "🔊", fontSize = 28.sp)
        }

        // Main content – centred
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ONE TAP ESCAPE",
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BEST SCORE: ${highScore.toInt()}m",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = modelName,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Primary CTA
            Button(
                onClick = {
                    AudioManager.playSfx(context, "button")
                    onPlayClick()
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("START RUN", fontSize = 22.sp, color = Color(0xFF1A237E), fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-column row: GARAGE & SHOP | HOW TO PLAY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AudioManager.playSfx(context, "button")
                        onCustomizeClick()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("GARAGE & SHOP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        AudioManager.playSfx(context, "button")
                        showTutorial = true
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("HOW TO PLAY 📖", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard – full width
            OutlinedButton(
                onClick = {
                    AudioManager.playSfx(context, "button")
                    onLeaderboardClick()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🏆 ", fontSize = 16.sp)
                    Text(
                        text = "LEADERBOARD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Tutorial Overlay (replayable from Home)
        if (showTutorial) {
            TutorialOverlay(
                onComplete = { showTutorial = false },
                onSkip = { showTutorial = false }
            )
        }
    }
}
