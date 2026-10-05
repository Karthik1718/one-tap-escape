package com.karthik.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sin

data class TutorialSlide(
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val emoji: String
)

private val tutorialSlides = listOf(
    TutorialSlide(
        title = "SWIPE & SWERVE",
        subtitle = "Lane Controls",
        description = "Tap the LEFT side of the screen to move left.\nTap the RIGHT side to move right.\n\nYour car drives forward automatically — just dodge!",
        accentColor = Color(0xFF00E5FF),
        emoji = "🕹️"
    ),
    TutorialSlide(
        title = "DODGE & NEAR MISS",
        subtitle = "Obstacles & Bonus Score",
        description = "Avoid cones, barriers, and roadblocks.\n\n⚡ Drive CLOSE to obstacles without hitting them for a NEAR MISS bonus — the closer, the higher the reward!",
        accentColor = Color(0xFFFFD600),
        emoji = "⚡"
    ),
    TutorialSlide(
        title = "POWER-UPS",
        subtitle = "Collect & Dominate",
        description = "🧲 MAGNET — Pulls all nearby coins to your car.\n\n🛡️ SHIELD — Absorbs 1 crash. Survive a hit!\n\n🚀 NITRO — Smash through obstacles, outrun police, and gain invincibility!",
        accentColor = Color(0xFF7C4DFF),
        emoji = "🚀"
    ),
    TutorialSlide(
        title = "POLICE CHASE",
        subtitle = "Pedestrians & Wanted Stars",
        description = "🚶 Pedestrians walk on sidewalks. Drive close for SIDEWALK CLOSE CALL bonus (+15 💰).\n\n🚨 Hit a pedestrian → Wanted Level rises (1–3 ⭐) and Police Chase begins!\n\n⚡ Tap NITRO to escape. If caught, pay a fine or watch an ad!",
        accentColor = Color(0xFFFF1744),
        emoji = "🚨"
    ),
    TutorialSlide(
        title = "GHOST & MILESTONES",
        subtitle = "Beat Your Best",
        description = "👻 A translucent GHOST CAR replays your personal best run. Race to beat yourself!\n\n🏁 Every 1,000m triggers a MILESTONE checkpoint — revive from here if you crash.\n\n💰 Collect coins to unlock new cars, themes & colors in the Garage!",
        accentColor = Color(0xFF00C853),
        emoji = "👻"
    )
)

@Composable
fun TutorialOverlay(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val slide = tutorialSlides[currentPage]
    val totalPages = tutorialSlides.size

    // Animate a float for canvas illustrations
    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1B2A).copy(alpha = 0.97f),
                        Color(0xFF1B2838).copy(alpha = 0.98f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOW TO PLAY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                TextButton(onClick = onSkip) {
                    Text(
                        "SKIP →",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Canvas Illustration
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "slide_transition"
            ) { page ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        tutorialSlides[page].accentColor.copy(alpha = 0.3f)
                    )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> drawControlsIllustration(animProgress, bounce)
                            1 -> drawObstaclesIllustration(animProgress, bounce)
                            2 -> drawPowerUpsIllustration(animProgress, bounce)
                            3 -> drawPoliceIllustration(animProgress, bounce)
                            4 -> drawGhostIllustration(animProgress, bounce)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emoji + Title
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (fadeIn(tween(300))) togetherWith (fadeOut(tween(200)))
                },
                label = "title_transition"
            ) { page ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tutorialSlides[page].emoji,
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tutorialSlides[page].title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tutorialSlides[page].subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tutorialSlides[page].accentColor,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (fadeIn(tween(400, delayMillis = 100))) togetherWith (fadeOut(tween(200)))
                },
                label = "desc_transition"
            ) { page ->
                Text(
                    text = tutorialSlides[page].description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 120.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalPages) {
                    val isActive = i == currentPage
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 28.dp else 8.dp, 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (isActive) slide.accentColor else Color.White.copy(alpha = 0.25f)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous Button
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier
                            .weight(0.35f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("← BACK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Next / Start Driving Button
                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(0.65f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPage == totalPages - 1)
                            Color(0xFF00E5FF) else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (currentPage == totalPages - 1) "START DRIVING! 🏁" else "NEXT →",
                        color = Color(0xFF0D1B2A),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════
// Canvas Illustration Functions for Each Slide
// ═══════════════════════════════════════════════

private fun DrawScope.drawControlsIllustration(progress: Float, bounce: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Road background
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(cx - 90f, 20f),
        size = Size(180f, size.height - 40f),
        cornerRadius = CornerRadius(8f)
    )

    // Lane lines
    val laneLineColor = Color(0xFFFFD600).copy(alpha = 0.6f)
    for (i in 0..5) {
        val yOff = (i * 60f + progress * 60f) % 360f
        drawRoundRect(
            color = laneLineColor,
            topLeft = Offset(cx - 3f, 20f + yOff),
            size = Size(6f, 30f),
            cornerRadius = CornerRadius(3f)
        )
    }

    // Player car (moves left/right based on animation)
    val laneX = cx + sin(progress * 2 * Math.PI).toFloat() * 55f
    drawPlayerCar(laneX, cy + 20f, Color(0xFF00E5FF))

    // Left tap zone indicator
    val leftAlpha = if (sin(progress * 2 * Math.PI) < 0) 0.35f else 0.1f
    drawRoundRect(
        color = Color(0xFF00E5FF).copy(alpha = leftAlpha),
        topLeft = Offset(20f, cy - 50f),
        size = Size(cx - 120f, 100f),
        cornerRadius = CornerRadius(12f)
    )
    // "TAP" label left
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = leftAlpha + 0.1f),
        radius = 18f,
        center = Offset(cx / 2 - 30f, cy)
    )

    // Right tap zone indicator
    val rightAlpha = if (sin(progress * 2 * Math.PI) > 0) 0.35f else 0.1f
    drawRoundRect(
        color = Color(0xFF00E5FF).copy(alpha = rightAlpha),
        topLeft = Offset(cx + 100f, cy - 50f),
        size = Size(cx - 120f, 100f),
        cornerRadius = CornerRadius(12f)
    )
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = rightAlpha + 0.1f),
        radius = 18f,
        center = Offset(cx + cx / 2 + 30f, cy)
    )

    // Arrows
    val arrowColor = Color.White.copy(alpha = 0.7f)
    // Left arrow
    val laX = cx / 2 - 30f
    drawLine(arrowColor, Offset(laX + 8f, cy - 8f), Offset(laX - 4f, cy), strokeWidth = 3f)
    drawLine(arrowColor, Offset(laX - 4f, cy), Offset(laX + 8f, cy + 8f), strokeWidth = 3f)
    // Right arrow
    val raX = cx + cx / 2 + 30f
    drawLine(arrowColor, Offset(raX - 8f, cy - 8f), Offset(raX + 4f, cy), strokeWidth = 3f)
    drawLine(arrowColor, Offset(raX + 4f, cy), Offset(raX - 8f, cy + 8f), strokeWidth = 3f)
}

private fun DrawScope.drawObstaclesIllustration(progress: Float, bounce: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Road
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(cx - 100f, 20f),
        size = Size(200f, size.height - 40f),
        cornerRadius = CornerRadius(8f)
    )

    // Scrolling obstacles
    val obstacleY1 = 40f + (progress * 300f) % (size.height - 60f)
    drawRoundRect(
        color = Color(0xFFFF5722),
        topLeft = Offset(cx + 30f, obstacleY1),
        size = Size(50f, 22f),
        cornerRadius = CornerRadius(4f)
    )
    // Cone triangle on top
    val conePath = Path().apply {
        moveTo(cx + 55f, obstacleY1)
        lineTo(cx + 45f, obstacleY1 - 20f)
        lineTo(cx + 65f, obstacleY1 - 20f)
        close()
    }
    drawPath(conePath, Color(0xFFFF9100))

    val obstacleY2 = 40f + ((progress * 300f) + 160f) % (size.height - 60f)
    drawRoundRect(
        color = Color(0xFFFF5722),
        topLeft = Offset(cx - 75f, obstacleY2),
        size = Size(50f, 22f),
        cornerRadius = CornerRadius(4f)
    )

    // Player car swerving close
    val carX = cx - 10f + sin(progress * 4 * Math.PI).toFloat() * 35f
    drawPlayerCar(carX, cy + 30f, Color(0xFF00E5FF))

    // Near-miss spark effect
    val sparkAlpha = (sin(progress * 6 * Math.PI).toFloat() * 0.5f + 0.5f).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFD600).copy(alpha = sparkAlpha * 0.6f), Color.Transparent)
        ),
        radius = 35f,
        center = Offset(carX, cy + 10f)
    )

    // "NEAR MISS" indicator flash
    if (bounce > 0.5f) {
        drawRoundRect(
            color = Color(0xFFFFD600).copy(alpha = bounce * 0.7f),
            topLeft = Offset(cx - 60f, 30f),
            size = Size(120f, 28f),
            cornerRadius = CornerRadius(14f)
        )
    }
}

private fun DrawScope.drawPowerUpsIllustration(progress: Float, bounce: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Three power-up icons floating
    val spacing = 120f

    // Magnet (left)
    val magnetY = cy - 30f + sin((progress * 2 * Math.PI).toDouble()).toFloat() * 12f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF4081).copy(alpha = 0.4f), Color.Transparent)
        ),
        radius = 45f,
        center = Offset(cx - spacing, magnetY)
    )
    drawCircle(
        color = Color(0xFFFF4081),
        radius = 28f,
        center = Offset(cx - spacing, magnetY)
    )
    // U-shape magnet symbol
    drawArc(
        color = Color.White,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - spacing - 14f, magnetY - 14f),
        size = Size(28f, 28f),
        style = Stroke(width = 4f)
    )
    // Label
    drawRoundRect(
        color = Color(0xFFFF4081).copy(alpha = 0.3f),
        topLeft = Offset(cx - spacing - 35f, magnetY + 38f),
        size = Size(70f, 20f),
        cornerRadius = CornerRadius(10f)
    )

    // Shield (center)
    val shieldY = cy - 30f + sin((progress * 2 * Math.PI + 2).toDouble()).toFloat() * 12f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent)
        ),
        radius = 45f,
        center = Offset(cx, shieldY)
    )
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 28f,
        center = Offset(cx, shieldY)
    )
    // Shield shape
    val shieldPath = Path().apply {
        moveTo(cx, shieldY - 16f)
        lineTo(cx + 14f, shieldY - 6f)
        lineTo(cx + 10f, shieldY + 12f)
        lineTo(cx, shieldY + 18f)
        lineTo(cx - 10f, shieldY + 12f)
        lineTo(cx - 14f, shieldY - 6f)
        close()
    }
    drawPath(shieldPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = 3f))

    drawRoundRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.3f),
        topLeft = Offset(cx - 35f, shieldY + 38f),
        size = Size(70f, 20f),
        cornerRadius = CornerRadius(10f)
    )

    // Nitro (right)
    val nitroY = cy - 30f + sin((progress * 2 * Math.PI + 4).toDouble()).toFloat() * 12f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF9100).copy(alpha = 0.4f), Color.Transparent)
        ),
        radius = 45f,
        center = Offset(cx + spacing, nitroY)
    )
    drawCircle(
        color = Color(0xFFFF9100),
        radius = 28f,
        center = Offset(cx + spacing, nitroY)
    )
    // Lightning bolt
    val boltPath = Path().apply {
        moveTo(cx + spacing - 4f, nitroY - 16f)
        lineTo(cx + spacing + 8f, nitroY - 16f)
        lineTo(cx + spacing + 2f, nitroY - 2f)
        lineTo(cx + spacing + 10f, nitroY - 2f)
        lineTo(cx + spacing - 6f, nitroY + 18f)
        lineTo(cx + spacing, nitroY + 4f)
        lineTo(cx + spacing - 8f, nitroY + 4f)
        close()
    }
    drawPath(boltPath, Color.White.copy(alpha = 0.9f))

    drawRoundRect(
        color = Color(0xFFFF9100).copy(alpha = 0.3f),
        topLeft = Offset(cx + spacing - 35f, nitroY + 38f),
        size = Size(70f, 20f),
        cornerRadius = CornerRadius(10f)
    )

    // Connecting dotted line
    for (i in 0..6) {
        val dotX = cx - spacing + 30f + i * 35f
        if (dotX < cx + spacing - 25f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = 2f,
                center = Offset(dotX, cy - 30f)
            )
        }
    }
}

private fun DrawScope.drawPoliceIllustration(progress: Float, bounce: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Road
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(cx - 90f, 20f),
        size = Size(180f, size.height - 40f),
        cornerRadius = CornerRadius(8f)
    )

    // Player car ahead
    drawPlayerCar(cx, cy - 20f, Color(0xFF00E5FF))

    // Police car chasing behind
    val policeY = cy + 80f + bounce * 15f
    drawPlayerCar(cx - 10f, policeY, Color(0xFF1565C0))

    // Police siren lights flashing
    val flash = (progress * 8f).toInt() % 2 == 0
    drawCircle(
        color = if (flash) Color.Red.copy(alpha = 0.9f) else Color.Red.copy(alpha = 0.2f),
        radius = 8f,
        center = Offset(cx - 25f, policeY - 22f)
    )
    drawCircle(
        color = if (!flash) Color.Blue.copy(alpha = 0.9f) else Color.Blue.copy(alpha = 0.2f),
        radius = 8f,
        center = Offset(cx + 5f, policeY - 22f)
    )

    // Red/blue ambient glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                (if (flash) Color.Red else Color.Blue).copy(alpha = 0.15f),
                Color.Transparent
            )
        ),
        radius = 120f,
        center = Offset(cx, policeY)
    )

    // Wanted stars at top
    val starCount = ((progress * 3f).toInt() % 3) + 1
    for (i in 0 until 3) {
        drawCircle(
            color = if (i < starCount) Color(0xFFFFD600) else Color.White.copy(alpha = 0.15f),
            radius = 10f,
            center = Offset(cx - 30f + i * 30f, 40f)
        )
    }

    // Pedestrian on sidewalk (left side)
    val pedX = cx - 120f
    val pedY = cy + sin(progress * 4 * Math.PI).toFloat() * 30f
    // Head
    drawCircle(color = Color(0xFFFFCDD2), radius = 7f, center = Offset(pedX, pedY - 18f))
    // Body
    drawRoundRect(
        color = Color(0xFFE53935),
        topLeft = Offset(pedX - 6f, pedY - 10f),
        size = Size(12f, 18f),
        cornerRadius = CornerRadius(3f)
    )
    // Legs
    drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(pedX - 5f, pedY + 8f),
        size = Size(10f, 12f),
        cornerRadius = CornerRadius(2f)
    )
}

private fun DrawScope.drawGhostIllustration(progress: Float, bounce: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Road
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(cx - 90f, 20f),
        size = Size(180f, size.height - 40f),
        cornerRadius = CornerRadius(8f)
    )

    // Ghost car (translucent, slightly ahead/behind based on animation)
    val ghostOffset = sin(progress * 2 * Math.PI).toFloat() * 40f
    drawPlayerCar(cx + 15f, cy - 30f + ghostOffset, Color.White.copy(alpha = 0.25f))
    // Ghost trail
    for (i in 1..3) {
        drawCircle(
            color = Color.White.copy(alpha = 0.08f / i),
            radius = 20f - i * 4f,
            center = Offset(cx + 15f, cy - 30f + ghostOffset + i * 25f)
        )
    }

    // Player car
    drawPlayerCar(cx - 15f, cy + 20f, Color(0xFF00E5FF))

    // Milestone markers on road
    val milestoneY1 = (progress * 200f) % (size.height)
    drawRoundRect(
        color = Color(0xFF00C853).copy(alpha = 0.5f),
        topLeft = Offset(cx - 85f, milestoneY1),
        size = Size(170f, 4f),
        cornerRadius = CornerRadius(2f)
    )

    // Coins floating
    for (i in 0..2) {
        val coinY = 50f + i * 65f + (progress * 100f) % 200f
        val coinAlpha = if (coinY < size.height - 30f) 0.8f else 0f
        drawCircle(
            color = Color(0xFFFFD600).copy(alpha = coinAlpha),
            radius = 10f,
            center = Offset(cx + 40f - i * 20f, coinY)
        )
        drawCircle(
            color = Color(0xFFFFF176).copy(alpha = coinAlpha * 0.6f),
            radius = 5f,
            center = Offset(cx + 40f - i * 20f, coinY)
        )
    }

    // Checkpoint flag at top
    val flagX = cx + 70f
    drawLine(Color.White.copy(alpha = 0.5f), Offset(flagX, 30f), Offset(flagX, 70f), strokeWidth = 2f)
    drawRect(
        color = Color(0xFF00C853).copy(alpha = 0.7f + bounce * 0.3f),
        topLeft = Offset(flagX + 2f, 30f),
        size = Size(22f, 14f)
    )
}

// Small helper to draw a simplified car shape
private fun DrawScope.drawPlayerCar(x: Float, y: Float, color: Color) {
    // Car body
    drawRoundRect(
        color = color,
        topLeft = Offset(x - 16f, y - 24f),
        size = Size(32f, 48f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Windshield
    drawRoundRect(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(x - 10f, y - 14f),
        size = Size(20f, 12f),
        cornerRadius = CornerRadius(4f)
    )
    // Headlights
    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 3f, center = Offset(x - 10f, y - 22f))
    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 3f, center = Offset(x + 10f, y - 22f))
}
