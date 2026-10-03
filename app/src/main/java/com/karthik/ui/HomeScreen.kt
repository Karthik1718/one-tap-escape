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
fun HomeScreen(onPlayClick: () -> Unit, onCustomizeClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val saveManager = remember { com.karthik.data.SaveManager(context) }
    val highScore = saveManager.highScore
    val coins = saveManager.totalCoins
    val selectedModel = saveManager.getSelectedModel()
    val isSignedIn = PlayGamesManager.isSignedIn

    val modelName = when(selectedModel) {
        "CYBER_TRUCK" -> "🚚 Cyber Truck"
        "POLICE_CRUISER" -> "🚓 Police Cruiser"
        "SUPER_BOLT" -> "⚡ Super Bolt"
        else -> "🏎️ Speed Racer"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
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
            
            Button(
                onClick = {
                    AudioManager.playSfx(context, "button")
                    onPlayClick()
                },
                modifier = Modifier.width(230.dp).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("START RUN", fontSize = 22.sp, color = Color(0xFF1A237E), fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    AudioManager.playSfx(context, "button")
                    onCustomizeClick()
                },
                modifier = Modifier.width(230.dp).height(58.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("GARAGE & SHOP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💰 $coins coins",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Friends Leaderboard Button
            OutlinedButton(
                onClick = {
                    AudioManager.playSfx(context, "button")
                    if (activity != null) {
                        PlayGamesManager.showLeaderboard(activity)
                    }
                },
                modifier = Modifier.width(230.dp).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isSignedIn) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.55f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isSignedIn) Color(0xFF00E5FF).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🏆 ", fontSize = 16.sp)
                    Text(
                        text = if (isSignedIn) "LEADERBOARD" else "LEADERBOARD (SIGN IN)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var isMuted by remember { mutableStateOf(AudioManager.isMuted()) }
            IconButton(onClick = { isMuted = AudioManager.toggleMute() }) {
                Text(if (isMuted) "🔈" else "🔊", fontSize = 32.sp, color = Color.White)
            }
        }
    }
}
