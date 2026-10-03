package com.karthik.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthik.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Icon scale & bounce
    val iconScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SplashIconScale"
    )

    // Title scale & alpha
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300),
        label = "SplashTitleAlpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 600),
        label = "SplashSubtitleAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2600)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Neon Glowing App Icon Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(iconScale)
                    .size(150.dp)
            ) {
                // Background Radial Neon Glow
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    modifier = Modifier.size(148.dp)
                ) {}

                // Icon Card
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(2.5.dp, Color(0xFF00E5FF).copy(alpha = 0.85f)),
                    shadowElevation = 16.dp,
                    color = Color.Black,
                    modifier = Modifier.size(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "One Tap Escape Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(30.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Bold Metallic Game Title
            Text(
                text = "ONE TAP ESCAPE",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Subtitle Badge
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                modifier = Modifier.alpha(subtitleAlpha)
            ) {
                Text(
                    text = "⚡ ARCADE CAR DODGE & DRIFT",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 4. Sleek Loading Progress Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(subtitleAlpha)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "STARTING ENGINES...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
