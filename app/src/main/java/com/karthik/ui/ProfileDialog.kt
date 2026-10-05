package com.karthik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karthik.audio.AudioManager
import com.karthik.data.FirebaseLeaderboardManager
import com.karthik.data.SaveManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context) }
    val currentUserId = saveManager.getUserId()

    var usernameInput by remember { mutableStateOf(saveManager.username) }
    var selectedAvatar by remember {
        mutableStateOf(
            if (saveManager.avatarId.isNotBlank()) saveManager.avatarId else saveManager.getRandomAvatar()
        )
    }

    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Live validation debounce
    LaunchedEffect(usernameInput) {
        val trimmed = usernameInput.trim()
        if (trimmed.isBlank()) {
            isAvailable = null
            errorMessage = "Username cannot be blank"
            return@LaunchedEffect
        }
        if (trimmed.length < 3) {
            isAvailable = null
            errorMessage = "Username must be at least 3 characters"
            return@LaunchedEffect
        }
        if (trimmed.length > 15) {
            isAvailable = null
            errorMessage = "Username cannot exceed 15 characters"
            return@LaunchedEffect
        }

        // If username is unchanged from current user's saved username, it's valid
        if (trimmed.equals(saveManager.username, ignoreCase = true)) {
            isAvailable = true
            errorMessage = ""
            return@LaunchedEffect
        }

        isChecking = true
        errorMessage = ""
        delay(400) // Debounce 400ms

        FirebaseLeaderboardManager.checkUsernameAvailability(trimmed, currentUserId) { available ->
            isChecking = false
            isAvailable = available
            if (!available) {
                errorMessage = "Username is already taken!"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121829),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (saveManager.username.isBlank()) "CREATE PILOT PROFILE" else "EDIT PROFILE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Display
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(3.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedAvatar, fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar Selector List + Randomize
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CHOOSE AVATAR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            selectedAvatar = saveManager.getRandomAvatar()
                            AudioManager.playSfx(context, "button")
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("🎲 Random", fontSize = 12.sp, color = Color(0xFF00E5FF))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SaveManager.AVAILABLE_AVATARS) { avatar ->
                        val isSelected = avatar == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF1E2A38))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedAvatar = avatar
                                    AudioManager.playSfx(context, "button")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = avatar, fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Username TextField
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.replace(" ", "_") },
                    label = { Text("Username", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when {
                        isChecking -> {
                            Text(
                                text = "⏳ Checking availability...",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD600)
                            )
                        }
                        isAvailable == true -> {
                            Text(
                                text = "✓ Username available!",
                                fontSize = 12.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        errorMessage.isNotBlank() -> {
                            Text(
                                text = "✗ $errorMessage",
                                fontSize = 12.sp,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (saveManager.username.isNotBlank()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("CANCEL")
                        }
                    }

                    Button(
                        onClick = {
                            val cleanName = usernameInput.trim()
                            if (cleanName.isNotBlank() && isAvailable == true && !isSaving) {
                                isSaving = true
                                saveManager.saveUserProfile(cleanName, selectedAvatar)
                                
                                // Sync to Firebase
                                FirebaseLeaderboardManager.saveUserProfileAndScore(
                                    userId = currentUserId,
                                    username = cleanName,
                                    avatarId = selectedAvatar,
                                    score = saveManager.highScore.toLong()
                                ) {
                                    isSaving = false
                                    onProfileSaved()
                                }
                            }
                        },
                        enabled = (isAvailable == true && usernameInput.trim().isNotBlank() && !isSaving),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "SAVE",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
