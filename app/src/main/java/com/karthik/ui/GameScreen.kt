package com.karthik.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthik.ads.AdManager
import com.karthik.audio.AudioManager
import com.karthik.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    onBackToMenu: () -> Unit,
    onOpenLeaderboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val saveManager = remember { com.karthik.data.SaveManager(context) }
    val engine = remember { GameEngine(saveManager, context) }
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Connect audio & haptics
    LaunchedEffect(Unit) {
        engine.onPlaySfx = { type -> AudioManager.playSfx(context, type) }
        engine.onVibrate = { type -> AudioManager.vibrate(context, type) }
    }

    // Milestone local state
    var showMilestone by remember { mutableStateOf(false) }
    var milestoneText by remember { mutableStateOf("") }

    // Near-Miss local state
    var showNearMiss by remember { mutableStateOf(false) }
    var nearMissText by remember { mutableStateOf("") }

    // Close-Call local state
    var showCloseCall by remember { mutableStateOf(false) }
    var closeCallText by remember { mutableStateOf("") }

    LaunchedEffect(engine.policeCloseCallTrigger) {
        if (engine.policeCloseCallTrigger) {
            closeCallText = engine.policeCloseCallText
            showCloseCall = true
            delay(900)
            showCloseCall = false
            engine.policeCloseCallTrigger = false
        }
    }

    LaunchedEffect(engine.milestoneTrigger) {
        if (engine.milestoneTrigger) {
            milestoneText = "${engine.lastMilestoneReached}m"
            showMilestone = true
            delay(1500)
            showMilestone = false
            engine.milestoneTrigger = false
        }
    }

    LaunchedEffect(engine.nearMissTrigger) {
        if (engine.nearMissTrigger) {
            nearMissText = engine.lastNearMissText
            showNearMiss = true
            delay(900)
            showNearMiss = false
            engine.nearMissTrigger = false
        }
    }

    // Tutorial state — show on first launch only
    var showTutorial by remember { mutableStateOf(!saveManager.isTutorialCompleted()) }
    
    LaunchedEffect(Unit) {
        engine.reset(autoStart = false)
    }
    
    // 60 FPS Game Loop
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while(true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastTime) / 1000f
            lastTime = currentTime
            
            if (engine.gameState == GameState.PLAYING) {
                engine.update(deltaTime)
            }
            delay(16)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (showTutorial) return@detectTapGestures
                        if (engine.gameState == GameState.PLAYING) {
                            engine.onTouch(isLeft = offset.x < size.width / 2)
                        } else if (engine.gameState == GameState.IDLE) {
                            engine.onTouch(true)
                        }
                    }
                }
        ) {
            // Police Emergency Flashing Border Overlay during Police Chase
            if (engine.policeChaseActive) {
                val pFlash = (System.currentTimeMillis() / 120) % 2 == 0L
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    (if (pFlash) Color.Red else Color.Blue).copy(alpha = 0.28f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    (if (!pFlash) Color.Red else Color.Blue).copy(alpha = 0.28f)
                                )
                            )
                        )
                )
            }

            // High-Quality Dynamic Game Rendering with Screen Shake
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shakeX = if (engine.screenShakeTimer > 0) ((Math.random().toFloat() * 2f - 1f) * engine.screenShakeIntensity) else 0f
                val shakeY = if (engine.screenShakeTimer > 0) ((Math.random().toFloat() * 2f - 1f) * engine.screenShakeIntensity) else 0f

                translate(left = shakeX, top = shakeY) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val roadWidth = 420f
                    val sceneryWidth = (size.width - roadWidth) / 2

                    // 1. Scenery Background (Realistic 3D Atmosphere)
                    when (engine.selectedTheme) {
                        GameTheme.VIBRANT_CITY -> {
                            // City park/sidewalks with skyline
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF388E3C))),
                                topLeft = Offset(0f, 0f), size = Size(sceneryWidth, size.height)
                            )
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF388E3C), Color(0xFF2E7D32))),
                                topLeft = Offset(size.width - sceneryWidth, 0f), size = Size(sceneryWidth, size.height)
                            )
                            // City Building Silhouettes with glowing windows
                            val bSpacing = 280f
                            for (i in -1..((size.height / bSpacing).toInt() + 1)) {
                                val yB = (i * bSpacing) + (engine.playerY * 0.4f % bSpacing)
                                // Left Skyline
                                drawRect(color = Color(0xFF263238), topLeft = Offset(10f, yB), size = Size(sceneryWidth * 0.65f, 220f))
                                // Building windows
                                for (wR in 0..4) {
                                    for (wC in 0..2) {
                                        val winOn = ((i + wR + wC) % 3 != 0)
                                        drawRect(
                                            color = if (winOn) Color(0xFFFFD54F).copy(alpha = 0.75f) else Color(0xFF1E282D),
                                            topLeft = Offset(20f + (wC * 22f), yB + 20f + (wR * 38f)),
                                            size = Size(12f, 18f)
                                        )
                                    }
                                }
                                // Right Skyline
                                val rX = size.width - sceneryWidth + 20f
                                drawRect(color = Color(0xFF1E282D), topLeft = Offset(rX, yB - 60f), size = Size(sceneryWidth * 0.7f, 240f))
                                for (wR in 0..4) {
                                    for (wC in 0..2) {
                                        val winOn = ((i * 2 + wR + wC) % 4 != 0)
                                        drawRect(
                                            color = if (winOn) Color(0xFF81D4FA).copy(alpha = 0.75f) else Color(0xFF151D21),
                                            topLeft = Offset(rX + 16f + (wC * 22f), yB - 40f + (wR * 40f)),
                                            size = Size(12f, 20f)
                                        )
                                    }
                                }
                            }
                        }
                        GameTheme.CYBERPUNK_NIGHT -> {
                            // Dark synthwave grid landscape
                            drawRect(color = Color(0xFF0A0A14), topLeft = Offset(0f, 0f), size = Size(sceneryWidth, size.height))
                            drawRect(color = Color(0xFF0A0A14), topLeft = Offset(size.width - sceneryWidth, 0f), size = Size(sceneryWidth, size.height))
                            // Neon Grid lines
                            val gridSpacing = 80f
                            for (i in -1..((size.height / gridSpacing).toInt() + 1)) {
                                val yG = (i * gridSpacing) + (engine.playerY * 0.8f % gridSpacing)
                                drawLine(color = Color(0xFFFF007F).copy(alpha = 0.35f), start = Offset(0f, yG), end = Offset(sceneryWidth, yG), strokeWidth = 1.5f)
                                drawLine(color = Color(0xFFFF007F).copy(alpha = 0.35f), start = Offset(size.width - sceneryWidth, yG), end = Offset(size.width, yG), strokeWidth = 1.5f)
                            }
                            // Neon Roadside Pillars with glowing tips
                            val pSpacing = 240f
                            for (i in -1..((size.height / pSpacing).toInt() + 1)) {
                                val yP = (i * pSpacing) + (engine.playerY % pSpacing)
                                drawRect(color = Color(0xFF1A1A2E), topLeft = Offset(sceneryWidth - 30f, yP), size = Size(14f, 50f))
                                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF00E5FF), Color.Transparent)), radius = 24f, center = Offset(sceneryWidth - 23f, yP))
                                drawCircle(color = Color(0xFF00E5FF), radius = 6f, center = Offset(sceneryWidth - 23f, yP))
                                
                                drawRect(color = Color(0xFF1A1A2E), topLeft = Offset(size.width - sceneryWidth + 16f, yP), size = Size(14f, 50f))
                                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF007F), Color.Transparent)), radius = 24f, center = Offset(size.width - sceneryWidth + 23f, yP))
                                drawCircle(color = Color(0xFFFF007F), radius = 6f, center = Offset(size.width - sceneryWidth + 23f, yP))
                            }
                        }
                        GameTheme.DESERT_OUTRUN -> {
                            // Warm desert sunset sand
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFD87D39), Color(0xFFE59855))),
                                topLeft = Offset(0f, 0f), size = Size(sceneryWidth, size.height)
                            )
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFE59855), Color(0xFFD87D39))),
                                topLeft = Offset(size.width - sceneryWidth, 0f), size = Size(sceneryWidth, size.height)
                            )
                            // Cacti with drop shadows
                            val cSpacing = 320f
                            for (i in -1..((size.height / cSpacing).toInt() + 1)) {
                                val yC = (i * cSpacing) + (engine.playerY % cSpacing)
                                val cX = sceneryWidth * 0.45f
                                // Shadow
                                drawOval(color = Color(0xFF8D4F20).copy(alpha = 0.5f), topLeft = Offset(cX - 25f, yC + 50f), size = Size(50f, 15f))
                                // Main stem & arms
                                drawRoundRect(color = Color(0xFF2E6930), topLeft = Offset(cX - 8f, yC - 40f), size = Size(16f, 90f), cornerRadius = CornerRadius(8f, 8f))
                                drawRoundRect(color = Color(0xFF2E6930), topLeft = Offset(cX - 30f, yC - 10f), size = Size(14f, 40f), cornerRadius = CornerRadius(6f, 6f))
                                drawRect(color = Color(0xFF2E6930), topLeft = Offset(cX - 24f, yC + 15f), size = Size(20f, 12f))
                                drawRoundRect(color = Color(0xFF2E6930), topLeft = Offset(cX + 16f, yC - 25f), size = Size(14f, 45f), cornerRadius = CornerRadius(6f, 6f))
                                drawRect(color = Color(0xFF2E6930), topLeft = Offset(cX + 4f, yC + 5f), size = Size(18f, 12f))
                            }
                        }
                        GameTheme.MIDNIGHT_FOREST -> {
                            drawRect(color = Color(0xFF071418), topLeft = Offset(0f, 0f), size = Size(sceneryWidth, size.height))
                            drawRect(color = Color(0xFF071418), topLeft = Offset(size.width - sceneryWidth, 0f), size = Size(sceneryWidth, size.height))
                            val fSpacing = 260f
                            for (i in -1..((size.height / fSpacing).toInt() + 1)) {
                                val yF = (i * fSpacing) + (engine.playerY % fSpacing)
                                val fX = sceneryWidth * 0.45f
                                for (tier in 0..2) {
                                    val tPath = Path().apply {
                                        moveTo(fX, yF - 60f + (tier * 25f))
                                        lineTo(fX - 45f + (tier * 8f), yF + 10f + (tier * 25f))
                                        lineTo(fX + 45f - (tier * 8f), yF + 10f + (tier * 25f))
                                        close()
                                    }
                                    drawPath(tPath, color = Color(0xFF00382B))
                                }
                            }
                        }
                    }

                    // 2. Highway Asphalt Surface with Real Gradient & Tire Skids
                    val roadLeft = sceneryWidth
                    val roadRight = size.width - sceneryWidth
                    val footpathWidth = 140f

                    // A. Left & Right Textured Concrete Footpaths (Sidewalks)
                    val sidewalkColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF1F1B24) else Color(0xFF90A4AE)
                    val tileGridColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF78909C)

                    // Left Sidewalk
                    drawRect(color = sidewalkColor, topLeft = Offset(roadLeft - footpathWidth, 0f), size = Size(footpathWidth, size.height))
                    // Right Sidewalk
                    drawRect(color = sidewalkColor, topLeft = Offset(roadRight, 0f), size = Size(footpathWidth, size.height))

                    // Sidewalk Paving Grid Lines & Curb Edges
                    val tileHeight = 70f
                    for (i in -1..((size.height / tileHeight).toInt() + 1)) {
                        val yTile = (i * tileHeight) + (engine.playerY % tileHeight)
                        // Horizontal tile lines
                        drawLine(color = tileGridColor, start = Offset(roadLeft - footpathWidth, yTile), end = Offset(roadLeft, yTile), strokeWidth = 2f)
                        drawLine(color = tileGridColor, start = Offset(roadRight, yTile), end = Offset(roadRight + footpathWidth, yTile), strokeWidth = 2f)
                    }

                    // Dark Curb Borders
                    drawRect(color = Color(0xFF37474F), topLeft = Offset(roadLeft - 4f, 0f), size = Size(4f, size.height))
                    drawRect(color = Color(0xFF37474F), topLeft = Offset(roadRight, 0f), size = Size(4f, size.height))

                    // Street Light Poles along Footpaths
                    val lampSpacing = 350f
                    for (i in -1..((size.height / lampSpacing).toInt() + 1)) {
                        val yLamp = (i * lampSpacing) + (engine.playerY % lampSpacing)
                        // Left Lamp Post
                        drawCircle(color = Color(0xFF455A64), radius = 6f, center = Offset(roadLeft - footpathWidth + 15f, yLamp))
                        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFF9C4).copy(alpha = 0.5f), Color.Transparent)), radius = 24f, center = Offset(roadLeft - footpathWidth + 15f, yLamp))
                        // Right Lamp Post
                        drawCircle(color = Color(0xFF455A64), radius = 6f, center = Offset(roadRight + footpathWidth - 15f, yLamp))
                        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFF9C4).copy(alpha = 0.5f), Color.Transparent)), radius = 24f, center = Offset(roadRight + footpathWidth - 15f, yLamp))
                    }

                    // Main Road Surface
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to Color(0xFF1A2226),
                            0.2f to Color(0xFF263238),
                            0.5f to Color(0xFF2E3D44),
                            0.8f to Color(0xFF263238),
                            1.0f to Color(0xFF1A2226),
                            startX = roadLeft,
                            endX = roadRight
                        ),
                        topLeft = Offset(roadLeft, 0f),
                        size = Size(roadWidth, size.height)
                    )

                    // Burnt Rubber Tire Skid Marks on Asphalt
                    val skidSpacing = 600f
                    for (i in -1..((size.height / skidSpacing).toInt() + 1)) {
                        val ySkid = (i * skidSpacing) + (engine.playerY % skidSpacing)
                        val sOffset = if (i % 2 == 0) -engine.horizontalOffset else engine.horizontalOffset
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.35f),
                            topLeft = Offset(centerX + sOffset - 35f, ySkid),
                            size = Size(10f, 140f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.35f),
                            topLeft = Offset(centerX + sOffset + 25f, ySkid + 10f),
                            size = Size(10f, 130f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    // 3D Beveled Road Curbs (Rumble Strips)
                    val curbWidth = 18f
                    val stripeHeight = 55f
                    val (c1, c2) = when(engine.selectedTheme) {
                        GameTheme.CYBERPUNK_NIGHT -> Color(0xFF00E5FF) to Color(0xFFFF007F)
                        GameTheme.DESERT_OUTRUN -> Color(0xFFB22222) to Color(0xFFFFF8E1)
                        else -> Color.White to Color(0xFFD32F2F)
                    }

                    for (i in -1..((size.height / (stripeHeight * 2)).toInt() + 1)) {
                        val yOffset = (i * stripeHeight * 2) + (engine.playerY % (stripeHeight * 2))
                        // Left Curb
                        drawRect(color = c1, topLeft = Offset(roadLeft - curbWidth, yOffset), size = Size(curbWidth, stripeHeight))
                        drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(roadLeft - curbWidth, yOffset), size = Size(3f, stripeHeight))
                        drawRect(color = c2, topLeft = Offset(roadLeft - curbWidth, yOffset + stripeHeight), size = Size(curbWidth, stripeHeight))
                        drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(roadLeft - curbWidth, yOffset + stripeHeight), size = Size(3f, stripeHeight))

                        // Right Curb
                        drawRect(color = c1, topLeft = Offset(roadRight, yOffset), size = Size(curbWidth, stripeHeight))
                        drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(roadRight + curbWidth - 3f, yOffset), size = Size(3f, stripeHeight))
                        drawRect(color = c2, topLeft = Offset(roadRight, yOffset + stripeHeight), size = Size(curbWidth, stripeHeight))
                        drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(roadRight + curbWidth - 3f, yOffset + stripeHeight), size = Size(3f, stripeHeight))
                    }

                    // Lane Markings
                    val laneWidth = engine.horizontalOffset
                    val dashHeight = 44f
                    val dashGap = 65f
                    val laneColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF00E5FF).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.65f)
                    
                    for (i in -1..((size.height / (dashHeight + dashGap)).toInt() + 1)) {
                        val yOffset = (i * (dashHeight + dashGap)) + (engine.playerY % (dashHeight + dashGap))
                        // Left lane divider
                        drawRoundRect(color = Color.Black.copy(alpha = 0.25f), topLeft = Offset(centerX - laneWidth * 0.5f - 2f, yOffset + 2f), size = Size(5f, dashHeight), cornerRadius = CornerRadius(2f, 2f))
                        drawRoundRect(color = laneColor, topLeft = Offset(centerX - laneWidth * 0.5f - 2f, yOffset), size = Size(5f, dashHeight), cornerRadius = CornerRadius(2f, 2f))
                        // Right lane divider
                        drawRoundRect(color = Color.Black.copy(alpha = 0.25f), topLeft = Offset(centerX + laneWidth * 0.5f - 3f, yOffset + 2f), size = Size(5f, dashHeight), cornerRadius = CornerRadius(2f, 2f))
                        drawRoundRect(color = laneColor, topLeft = Offset(centerX + laneWidth * 0.5f - 3f, yOffset), size = Size(5f, dashHeight), cornerRadius = CornerRadius(2f, 2f))
                    }
                    
                    // Double Center Strip
                    drawRect(color = Color(0xFFFFD600).copy(alpha = 0.85f), topLeft = Offset(centerX - 4, 0f), size = Size(3f, size.height))
                    drawRect(color = Color(0xFFFFD600).copy(alpha = 0.85f), topLeft = Offset(centerX + 2, 0f), size = Size(3f, size.height))

                    // B. Draw Pedestrians Walking on Footpaths
                    engine.pedestrians.forEach { ped ->
                        val relY = centerY - (ped.y - engine.playerY)
                        if (relY > -80 && relY < size.height + 80) {
                            val pedCanvasX = centerX + ped.x
                            val legSwing = (kotlin.math.sin((engine.playerY + ped.y) / 12.0).toFloat() * 12f)
                            
                            // Pedestrian Shadow
                            drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(pedCanvasX - 14f, relY + 12f), size = Size(28f, 10f))
                            
                            if (ped.isHit) {
                                // Knocked over pose
                                drawOval(color = ped.shirtColor, topLeft = Offset(pedCanvasX - 18f, relY - 10f), size = Size(36f, 20f))
                                drawCircle(color = Color(0xFFFFD180), radius = 10f, center = Offset(pedCanvasX + 16f, relY))
                            } else {
                                // Top-down walking human figure (Shoulders, Head, Arms, Legs)
                                // Legs
                                drawRoundRect(color = ped.pantsColor, topLeft = Offset(pedCanvasX - 12f, relY - 8f + legSwing), size = Size(8f, 22f), cornerRadius = CornerRadius(3f, 3f))
                                drawRoundRect(color = ped.pantsColor, topLeft = Offset(pedCanvasX + 4f, relY - 8f - legSwing), size = Size(8f, 22f), cornerRadius = CornerRadius(3f, 3f))
                                // Torso / Shirt
                                drawRoundRect(color = ped.shirtColor, topLeft = Offset(pedCanvasX - 16f, relY - 14f), size = Size(32f, 24f), cornerRadius = CornerRadius(6f, 6f))
                                // Head (Skin tone)
                                drawCircle(color = Color(0xFFFFD180), radius = 10f, center = Offset(pedCanvasX, relY - 2f))
                                // Hair / Cap
                                drawCircle(color = Color(0xFF3E2723), radius = 8f, center = Offset(pedCanvasX, relY - 4f))
                            }
                        }
                    }

                    // 3. Draw Obstacles with 3D Depth
                    engine.obstacles.forEach { obs ->
                        val relativeY = centerY - (obs.y - engine.playerY)
                        if (relativeY > -120 && relativeY < size.height + 120) {
                            val isCone = obs.id % 2 == 0L
                            val obsColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFFFF007F) else Color(0xFFFF5722)
                            
                            // Drop Shadow
                            drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(centerX + obs.x - obs.width/2 - 5f, relativeY + 15f), size = Size(obs.width + 10f, 25f))

                            if (obs.isRoadblock) {
                                // Police Barricade / Roadblock Obstacle
                                val rW = obs.width
                                drawRect(color = Color(0xFF263238), topLeft = Offset(centerX + obs.x - rW/2 + 10f, relativeY - 25f), size = Size(14f, 50f))
                                drawRect(color = Color(0xFF263238), topLeft = Offset(centerX + obs.x + rW/2 - 24f, relativeY - 25f), size = Size(14f, 50f))
                                drawRoundRect(color = Color(0xFFD32F2F), topLeft = Offset(centerX + obs.x - rW/2, relativeY - 16f), size = Size(rW, 32f), cornerRadius = CornerRadius(4f, 4f))
                                // White stripes
                                for (st in 0..3) {
                                    drawRect(color = Color.White, topLeft = Offset(centerX + obs.x - rW/2 + 10f + (st * 36f), relativeY - 16f), size = Size(16f, 32f))
                                }
                                // Strobe siren light on top
                                val rFlash = (System.currentTimeMillis() / 100) % 2 == 0L
                                drawCircle(color = if (rFlash) Color.Red else Color.Blue, radius = 10f, center = Offset(centerX + obs.x, relativeY - 22f))
                            } else if (isCone) {
                                val cBase = 52f
                                val cHeight = 65f
                                drawRoundRect(color = obsColor, topLeft = Offset(centerX + obs.x - cBase/2, relativeY + cHeight/2 - 12f), size = Size(cBase, 16f), cornerRadius = CornerRadius(4f, 4f))
                                val conePath = Path().apply {
                                    moveTo(centerX + obs.x, relativeY - cHeight/2)
                                    lineTo(centerX + obs.x - cBase/2 + 8f, relativeY + cHeight/2 - 12f)
                                    lineTo(centerX + obs.x + cBase/2 - 8f, relativeY + cHeight/2 - 12f)
                                    close()
                                }
                                drawPath(conePath, color = obsColor)
                                drawRect(color = Color.White, topLeft = Offset(centerX + obs.x - 12f, relativeY - 8f), size = Size(24f, 14f))
                            } else {
                                val bWidth = obs.width
                                val bHeight = obs.height
                                val plankColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF00E5FF) else Color(0xFFFFD600)
                                drawRect(color = Color(0xFF263238), topLeft = Offset(centerX + obs.x - bWidth/2 + 10f, relativeY - bHeight/2), size = Size(10f, bHeight + 10f))
                                drawRect(color = Color(0xFF263238), topLeft = Offset(centerX + obs.x + bWidth/2 - 20f, relativeY - bHeight/2), size = Size(10f, bHeight + 10f))
                                drawRoundRect(color = plankColor, topLeft = Offset(centerX + obs.x - bWidth/2, relativeY - 14f), size = Size(bWidth, 28f), cornerRadius = CornerRadius(4f, 4f))
                                for (s in 0..4) {
                                    drawRect(color = Color(0xFF1E1E1E), topLeft = Offset(centerX + obs.x - bWidth/2 + (s * 25f), relativeY - 14f), size = Size(12f, 28f))
                                }
                            }
                        }
                    }

                    // 4. Draw Coins with 3D Coin Ridge
                    val rot = kotlin.math.cos(Math.toRadians(engine.coinRotationFrame.toDouble())).toFloat()
                    engine.coins.forEach { coin ->
                        val relativeY = centerY - (coin.y - engine.playerY)
                        if (relativeY > -100 && relativeY < size.height + 100) {
                            val cColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF00E5FF) else Color(0xFFFFD600)
                            val cWidth = 42f * Math.abs(rot)
                            // Shadow
                            drawOval(color = Color.Black.copy(alpha = 0.35f), topLeft = Offset(centerX + coin.x - cWidth/2, relativeY + 16f), size = Size(cWidth, 12f))
                            // Gold Coin Body
                            drawOval(color = cColor, topLeft = Offset(centerX + coin.x - cWidth/2, relativeY - 21f), size = Size(cWidth, 42f))
                            drawOval(color = Color.White.copy(alpha = 0.5f), topLeft = Offset(centerX + coin.x - (cWidth * 0.7f) / 2, relativeY - 16f), size = Size(cWidth * 0.7f, 30f))
                        }
                    }

                    // 5. Draw Power-Ups on Track (Realistic Vector Icons)
                    engine.powerUps.forEach { pUp ->
                        val relativeY = centerY - (pUp.y - engine.playerY)
                        if (relativeY > -100 && relativeY < size.height + 100) {
                            val pulse = 1f + (kotlin.math.sin(System.currentTimeMillis() / 150.0).toFloat() * 0.12f)
                            val pX = centerX + pUp.x
                            val pY = relativeY

                            when (pUp.type) {
                                PowerUpType.MAGNET -> {
                                    // Cyan Pulsing Outer Energy Field
                                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color.Transparent)), radius = 54f * pulse, center = Offset(pX, pY))
                                    // Drop Shadow
                                    drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(pX - 25f, pY + 22f), size = Size(50f, 14f))

                                    // U-shaped Red Magnet Body
                                    val mPath = Path().apply {
                                        moveTo(pX - 22f, pY + 16f)
                                        lineTo(pX - 22f, pY - 8f)
                                        cubicTo(pX - 22f, pY - 28f, pX + 22f, pY - 28f, pX + 22f, pY - 8f)
                                        lineTo(pX + 22f, pY + 16f)
                                        lineTo(pX + 12f, pY + 16f)
                                        lineTo(pX + 12f, pY - 6f)
                                        cubicTo(pX + 12f, pY - 18f, pX - 12f, pY - 18f, pX - 12f, pY - 6f)
                                        lineTo(pX - 12f, pY + 16f)
                                        close()
                                    }
                                    drawPath(mPath, color = Color(0xFFD32F2F))

                                    // Silver Metallic Tips
                                    drawRoundRect(color = Color(0xFFECEFF1), topLeft = Offset(pX - 23f, pY + 6f), size = Size(12f, 12f), cornerRadius = CornerRadius(2f, 2f))
                                    drawRoundRect(color = Color(0xFFECEFF1), topLeft = Offset(pX + 11f, pY + 6f), size = Size(12f, 12f), cornerRadius = CornerRadius(2f, 2f))

                                    // Cyan Magnetic Field Arcs
                                    drawArc(color = Color(0xFF00E5FF), startAngle = 30f, sweepAngle = 120f, useCenter = false, topLeft = Offset(pX - 30f, pY + 14f), size = Size(60f, 20f), style = Stroke(width = 3f))
                                }
                                PowerUpType.SHIELD -> {
                                    // Green Pulsing Outer Energy Field
                                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF76FF03).copy(alpha = 0.6f), Color.Transparent)), radius = 54f * pulse, center = Offset(pX, pY))
                                    // Drop Shadow
                                    drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(pX - 25f, pY + 22f), size = Size(50f, 14f))

                                    // Metallic Knight Shield Shape
                                    val sPath = Path().apply {
                                        moveTo(pX, pY - 26f)
                                        lineTo(pX + 24f, pY - 18f)
                                        lineTo(pX + 22f, pY + 6f)
                                        quadraticTo(pX + 16f, pY + 24f, pX, pY + 28f)
                                        quadraticTo(pX - 16f, pY + 24f, pX - 22f, pY + 6f)
                                        lineTo(pX - 24f, pY - 18f)
                                        close()
                                    }
                                    // Gold Rim
                                    drawPath(sPath, color = Color(0xFFFFD54F))
                                    // Royal Shield Body
                                    val innerShield = Path().apply {
                                        moveTo(pX, pY - 22f)
                                        lineTo(pX + 20f, pY - 15f)
                                        lineTo(pX + 18f, pY + 4f)
                                        quadraticTo(pX + 13f, pY + 20f, pX, pY + 24f)
                                        quadraticTo(pX - 13f, pY + 20f, pX - 18f, pY + 4f)
                                        lineTo(pX - 20f, pY - 15f)
                                        close()
                                    }
                                    drawPath(innerShield, color = Color(0xFF2E7D32))

                                    // Bright Emblem (Shield Cross)
                                    drawRect(color = Color(0xFF76FF03), topLeft = Offset(pX - 4f, pY - 12f), size = Size(8f, 26f))
                                    drawRect(color = Color(0xFF76FF03), topLeft = Offset(pX - 12f, pY - 4f), size = Size(24f, 8f))
                                }
                                PowerUpType.NITRO -> {
                                    // Fiery Orange Outer Glow
                                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF5722).copy(alpha = 0.65f), Color.Transparent)), radius = 54f * pulse, center = Offset(pX, pY))
                                    // Drop Shadow
                                    drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(pX - 22f, pY + 22f), size = Size(44f, 14f))

                                    // NOS Cylinder Canister
                                    drawRoundRect(color = Color(0xFF1E88E5), topLeft = Offset(pX - 16f, pY - 18f), size = Size(32f, 38f), cornerRadius = CornerRadius(6f, 6f))
                                    // Dome Cap & Nozzle Valve
                                    drawCircle(color = Color(0xFFECEFF1), radius = 10f, center = Offset(pX, pY - 18f))
                                    drawRect(color = Color(0xFF37474F), topLeft = Offset(pX - 4f, pY - 26f), size = Size(8f, 8f))
                                    drawCircle(color = Color(0xFFFF1744), radius = 4f, center = Offset(pX, pY - 26f))

                                    // Lightning Bolt Symbol on Bottle
                                    val boltPath = Path().apply {
                                        moveTo(pX + 2f, pY - 12f)
                                        lineTo(pX - 10f, pY + 1f)
                                        lineTo(pX - 1f, pY + 1f)
                                        lineTo(pX - 4f, pY + 14f)
                                        lineTo(pX + 8f, pY + 1f)
                                        lineTo(pX, pY + 1f)
                                        close()
                                    }
                                    drawPath(boltPath, color = Color(0xFFFFD600))
                                }
                            }
                        }
                    }

                    // 6. Draw Animating Fly Coins
                    engine.animatingCoins.forEach { anim ->
                        val startCanvasY = centerY - (anim.startY - engine.playerY)
                        val startCanvasX = centerX + anim.startX
                        val targetX = 50.dp.toPx()
                        val targetY = 100.dp.toPx()
                        val t = anim.progress
                        val easedT = t * t
                        val currentX = startCanvasX + (targetX - startCanvasX) * easedT
                        val currentY = startCanvasY + (targetY - startCanvasY) * easedT
                        val cColor = if (engine.selectedTheme == GameTheme.CYBERPUNK_NIGHT) Color(0xFF00E5FF) else Color(0xFFFFD600)
                        val rScale = kotlin.math.cos(Math.toRadians(t.toDouble() * 720.0)).toFloat()
                        val animWidth = 40f * Math.abs(rScale)
                        drawOval(color = cColor, topLeft = Offset(currentX - animWidth / 2, currentY - 20f), size = Size(animWidth, 40f), alpha = 1f - (t * 0.3f))
                    }

                    // 7. Explosions & Particles
                    engine.particles.forEach { p ->
                        val pCanvasY = centerY - (p.y - engine.playerY)
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size, center = Offset(centerX + p.x, pCanvasY))
                    }

                    // 8. GHOST BEST-RUN CAR (drawn relative to player car)
                    if (engine.ghost?.hasGhost == true && engine.ghost?.isGhostActive == true && engine.gameState == GameState.PLAYING) {
                        val gx = centerX + engine.ghostX
                        val gy = centerY - (engine.ghost?.ghostDelta ?: 0f)
                        val gW = 96f
                        val gH = 176f
                        val ghostAlpha = 0.38f
                        val ghostColor = Color(0xFF00E5FF)

                        // Soft pulsing aura around ghost
                        val ghostPulse = 1f + (kotlin.math.sin(System.currentTimeMillis() / 180.0).toFloat() * 0.07f)
                        drawCircle(
                            brush = Brush.radialGradient(listOf(ghostColor.copy(alpha = 0.22f), Color.Transparent)),
                            radius = 90f * ghostPulse, center = Offset(gx, gy)
                        )

                        // Ghost car silhouette (simplified body)
                        drawRoundRect(
                            color = ghostColor.copy(alpha = ghostAlpha),
                            topLeft = Offset(gx - gW/2, gy - gH/2),
                            size = Size(gW, gH),
                            cornerRadius = CornerRadius(16f, 16f)
                        )
                        // Ghost cockpit window
                        drawRoundRect(
                            color = Color.White.copy(alpha = ghostAlpha * 0.6f),
                            topLeft = Offset(gx - gW/2 + 15f, gy - gH/4),
                            size = Size(gW - 30f, 62f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        // Ghost taillights
                        drawRoundRect(color = ghostColor.copy(alpha = 0.7f), topLeft = Offset(gx - gW/2 + 14f, gy + gH/2 - 8f), size = Size(16f, 6f), cornerRadius = CornerRadius(2f, 2f))
                        drawRoundRect(color = ghostColor.copy(alpha = 0.7f), topLeft = Offset(gx + gW/2 - 30f, gy + gH/2 - 8f), size = Size(16f, 6f), cornerRadius = CornerRadius(2f, 2f))
                        // Cyan top marker stripe above the ghost car (replaces text label)
                        drawRoundRect(
                            color = ghostColor.copy(alpha = 0.75f),
                            topLeft = Offset(gx - 30f, gy - gH/2 - 18f),
                            size = Size(60f, 10f),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                    }

                    // 8b. REALISTIC 3D VEHICLE RENDERING
                    val px = centerX + engine.playerX
                    val py = centerY
                    val carW = 96f
                    val carH = 176f
                    val alpha = if (engine.invincibilityTimer > 0) {
                        if ((System.currentTimeMillis() / 90) % 2 == 0L) 0.35f else 0.85f
                    } else 1.0f

                    // Calculate steering angle for front wheels
                    val steerAngle = ((engine.currentLaneIndex * engine.horizontalOffset - engine.playerX) / engine.horizontalOffset).coerceIn(-1f, 1f) * 16f

                    // A. Forward Headlight Beams (Illuminating the Highway)
                    val beamPathL = Path().apply {
                        moveTo(px - 32f, py - carH/2 + 10f)
                        lineTo(px - 14f, py - carH/2 + 10f)
                        lineTo(px + 45f, py - carH/2 - 280f)
                        lineTo(px - 110f, py - carH/2 - 280f)
                        close()
                    }
                    val beamPathR = Path().apply {
                        moveTo(px + 14f, py - carH/2 + 10f)
                        lineTo(px + 32f, py - carH/2 + 10f)
                        lineTo(px + 110f, py - carH/2 - 280f)
                        lineTo(px - 45f, py - carH/2 - 280f)
                        close()
                    }
                    val beamBrush = Brush.verticalGradient(
                        0.0f to Color(0xFFFFF9C4).copy(alpha = 0.35f * alpha),
                        1.0f to Color.Transparent,
                        startY = py - carH/2 + 10f,
                        endY = py - carH/2 - 280f
                    )
                    drawPath(beamPathL, brush = beamBrush)
                    drawPath(beamPathR, brush = beamBrush)

                    // B. Soft Underbody Ground Shadow
                    drawOval(
                        color = Color.Black.copy(alpha = 0.45f * alpha),
                        topLeft = Offset(px - carW/2 - 8f, py - carH/2 + 8f),
                        size = Size(carW + 16f, carH + 16f)
                    )

                    // C. 4 Rubber Wheels with Rims & Steering
                    val wheelW = 16f
                    val wheelH = 36f
                    val wheelColor = Color(0xFF1E1E1E)
                    val rimColor = Color(0xFFB0BEC5)

                    // Front Left Wheel (Steered)
                    rotate(degrees = steerAngle, pivot = Offset(px - carW/2 + 6f, py - carH/2 + 36f)) {
                        drawRoundRect(color = wheelColor, topLeft = Offset(px - carW/2 - 4f, py - carH/2 + 20f), size = Size(wheelW, wheelH), cornerRadius = CornerRadius(4f, 4f))
                        drawRoundRect(color = rimColor, topLeft = Offset(px - carW/2 - 1f, py - carH/2 + 26f), size = Size(10f, 24f), cornerRadius = CornerRadius(2f, 2f))
                    }
                    // Front Right Wheel (Steered)
                    rotate(degrees = steerAngle, pivot = Offset(px + carW/2 - 6f, py - carH/2 + 36f)) {
                        drawRoundRect(color = wheelColor, topLeft = Offset(px + carW/2 - 12f, py - carH/2 + 20f), size = Size(wheelW, wheelH), cornerRadius = CornerRadius(4f, 4f))
                        drawRoundRect(color = rimColor, topLeft = Offset(px + carW/2 - 9f, py - carH/2 + 26f), size = Size(10f, 24f), cornerRadius = CornerRadius(2f, 2f))
                    }
                    // Rear Left Wheel
                    drawRoundRect(color = wheelColor, topLeft = Offset(px - carW/2 - 4f, py + carH/2 - 56f), size = Size(wheelW, wheelH), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(color = rimColor, topLeft = Offset(px - carW/2 - 1f, py + carH/2 - 50f), size = Size(10f, 24f), cornerRadius = CornerRadius(2f, 2f))
                    // Rear Right Wheel
                    drawRoundRect(color = wheelColor, topLeft = Offset(px + carW/2 - 12f, py + carH/2 - 56f), size = Size(wheelW, wheelH), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(color = rimColor, topLeft = Offset(px + carW/2 - 9f, py + carH/2 - 50f), size = Size(10f, 24f), cornerRadius = CornerRadius(2f, 2f))

                    // D. Nitro Exhaust Flames
                    if (engine.nitroTimer > 0) {
                        val flameLen = 45f + (Math.random().toFloat() * 30f)
                        drawOval(brush = Brush.verticalGradient(listOf(Color(0xFFFF9100), Color.Transparent)), topLeft = Offset(px - 28f, py + carH/2 - 6f), size = Size(18f, flameLen))
                        drawOval(brush = Brush.verticalGradient(listOf(Color(0xFFFF9100), Color.Transparent)), topLeft = Offset(px + 10f, py + carH/2 - 6f), size = Size(18f, flameLen))
                    }

                    // E. Vehicle Body by Model
                    when (engine.selectedModel) {
                        "CYBER_TRUCK" -> {
                            // Heavy angular stainless steel body
                            val truckPath = Path().apply {
                                moveTo(px - carW/2 + 10f, py - carH/2)
                                lineTo(px + carW/2 - 10f, py - carH/2)
                                lineTo(px + carW/2, py - carH/2 + 40f)
                                lineTo(px + carW/2 - 4f, py + carH/2 - 15f)
                                lineTo(px - carW/2 + 4f, py + carH/2 - 15f)
                                lineTo(px - carW/2, py - carH/2 + 40f)
                                close()
                            }
                            drawPath(truckPath, color = Color(0xFF455A64).copy(alpha = alpha))
                            // Brushed steel metallic sheen
                            drawRoundRect(color = Color(0xFF607D8B).copy(alpha = 0.65f * alpha), topLeft = Offset(px - carW/2 + 14f, py - carH/2 + 15f), size = Size(carW - 28f, carH - 45f), cornerRadius = CornerRadius(4f, 4f))
                            // Full-width front neon LED laser blade
                            drawRoundRect(color = Color(0xFF00E5FF), topLeft = Offset(px - carW/2 + 8f, py - carH/2 + 4f), size = Size(carW - 16f, 8f), cornerRadius = CornerRadius(2f, 2f))
                            // Angular black tinted roof & glass
                            drawRoundRect(color = Color(0xFF102027).copy(alpha = alpha), topLeft = Offset(px - carW/2 + 16f, py - carH/4), size = Size(carW - 32f, 75f), cornerRadius = CornerRadius(2f, 2f))
                            // Rear Neon Red Lightbar
                            drawRect(color = Color(0xFFFF1744), topLeft = Offset(px - carW/2 + 6f, py + carH/2 - 18f), size = Size(carW - 12f, 6f))
                        }
                        "POLICE_CRUISER" -> {
                            // Highway Patrol Cruiser
                            drawRoundRect(color = Color(0xFF1B1B1B).copy(alpha = alpha), topLeft = Offset(px - carW/2, py - carH/2), size = Size(carW, carH), cornerRadius = CornerRadius(16f, 16f))
                            // White Door & Roof Panels
                            drawRect(color = Color.White.copy(alpha = alpha), topLeft = Offset(px - carW/2 + 6f, py - carH/4 - 10f), size = Size(carW - 12f, 90f))
                            // Front Push Bumper / Bullbar
                            drawRect(color = Color(0xFF37474F), topLeft = Offset(px - carW/2 + 18f, py - carH/2 - 6f), size = Size(carW - 36f, 8f))
                            drawRect(color = Color(0xFF263238), topLeft = Offset(px - 14f, py - carH/2 - 8f), size = Size(6f, 10f))
                            drawRect(color = Color(0xFF263238), topLeft = Offset(px + 8f, py - carH/2 - 8f), size = Size(6f, 10f))
                            // Windshield & Rear glass
                            drawRoundRect(color = Color(0xFF0D47A1).copy(alpha = 0.85f * alpha), topLeft = Offset(px - carW/2 + 14f, py - carH/4 - 4f), size = Size(carW - 28f, 38f), cornerRadius = CornerRadius(4f, 4f))
                            // Alternating Red & Blue Strobe Siren Bar
                            val flash = (System.currentTimeMillis() / 120) % 2 == 0L
                            drawRoundRect(color = if (flash) Color(0xFFFF1744) else Color(0xFF2979FF), topLeft = Offset(px - 26f, py - 4f), size = Size(24f, 12f), cornerRadius = CornerRadius(3f, 3f))
                            drawRoundRect(color = if (!flash) Color(0xFFFF1744) else Color(0xFF2979FF), topLeft = Offset(px + 2f, py - 4f), size = Size(24f, 12f), cornerRadius = CornerRadius(3f, 3f))
                            // Light reflections on car roof
                            drawCircle(color = (if (flash) Color.Red else Color.Blue).copy(alpha = 0.25f), radius = 35f, center = Offset(px, py + 2f))
                        }
                        "SUPER_BOLT" -> {
                            // Exotic GT Supercar
                            drawRoundRect(color = engine.playerColor.copy(alpha = alpha), topLeft = Offset(px - carW/2, py - carH/2), size = Size(carW, carH), cornerRadius = CornerRadius(20f, 20f))
                            // Aerodynamic front chin splitter
                            drawRoundRect(color = Color(0xFF1E1E1E), topLeft = Offset(px - carW/2 - 2f, py - carH/2), size = Size(carW + 4f, 12f), cornerRadius = CornerRadius(6f, 6f))
                            // Racing twin stripe
                            drawRect(color = Color.White.copy(alpha = 0.85f * alpha), topLeft = Offset(px - 7f, py - carH/2), size = Size(14f, carH))
                            // Hood Louvers (Heat extractors)
                            drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(px - 24f, py - carH/2 + 30f), size = Size(12f, 16f))
                            drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(px + 12f, py - carH/2 + 30f), size = Size(12f, 16f))
                            // Curved Cockpit & Windshield
                            drawRoundRect(color = Color(0xFF1A1A1A).copy(alpha = alpha), topLeft = Offset(px - carW/2 + 14f, py - carH/4), size = Size(carW - 28f, 68f), cornerRadius = CornerRadius(8f, 8f))
                            // Diagonal Glass Glare
                            drawLine(color = Color.White.copy(alpha = 0.4f * alpha), start = Offset(px - 20f, py - carH/4 + 6f), end = Offset(px + 8f, py - carH/4 + 32f), strokeWidth = 3f)
                            // GT3 Carbon Rear Wing & Endplates
                            drawRect(color = Color(0xFF1E1E1E), topLeft = Offset(px - carW/2 - 6f, py + carH/2 - 14f), size = Size(carW + 12f, 12f))
                            drawRect(color = engine.playerColor, topLeft = Offset(px - carW/2 - 8f, py + carH/2 - 20f), size = Size(5f, 20f))
                            drawRect(color = engine.playerColor, topLeft = Offset(px + carW/2 + 3f, py + carH/2 - 20f), size = Size(5f, 20f))
                        }
                        else -> {
                            // SPEED_RACER: Sleek Sports Car
                            drawRoundRect(color = engine.playerColor.copy(alpha = alpha), topLeft = Offset(px - carW/2, py - carH/2), size = Size(carW, carH), cornerRadius = CornerRadius(16f, 16f))
                            // Front Air Intake Grille
                            drawRoundRect(color = Color(0xFF1A1A1A), topLeft = Offset(px - carW/2 + 18f, py - carH/2 + 2f), size = Size(carW - 36f, 10f), cornerRadius = CornerRadius(4f, 4f))
                            // Aerodynamic Cockpit Glass
                            drawRoundRect(color = Color(0xFF212121).copy(alpha = alpha), topLeft = Offset(px - carW/2 + 15f, py - carH/4), size = Size(carW - 30f, 62f), cornerRadius = CornerRadius(6f, 6f))
                            // Glass Glare
                            drawLine(color = Color.White.copy(alpha = 0.35f * alpha), start = Offset(px - 16f, py - carH/4 + 8f), end = Offset(px + 10f, py - carH/4 + 32f), strokeWidth = 2.5f)
                            // Rear Ducktail Spoiler
                            drawRoundRect(color = Color(0xFF1A1A1A), topLeft = Offset(px - carW/2 + 8f, py + carH/2 - 12f), size = Size(carW - 16f, 8f), cornerRadius = CornerRadius(3f, 3f))
                        }
                    }

                    // F. Aerodynamic Side Mirrors
                    val mirrorColor = Color(0xFF263238).copy(alpha = alpha)
                    drawRoundRect(color = mirrorColor, topLeft = Offset(px - carW/2 - 10f, py - carH/4 + 10f), size = Size(10f, 14f), cornerRadius = CornerRadius(3f, 3f))
                    drawRoundRect(color = mirrorColor, topLeft = Offset(px + carW/2, py - carH/4 + 10f), size = Size(10f, 14f), cornerRadius = CornerRadius(3f, 3f))

                    // G. Glowing Taillights
                    drawRoundRect(color = Color(0xFFFF1744).copy(alpha = 0.9f * alpha), topLeft = Offset(px - carW/2 + 14f, py + carH/2 - 8f), size = Size(16f, 6f), cornerRadius = CornerRadius(2f, 2f))
                    drawRoundRect(color = Color(0xFFFF1744).copy(alpha = 0.9f * alpha), topLeft = Offset(px + carW/2 - 30f, py + carH/2 - 8f), size = Size(16f, 6f), cornerRadius = CornerRadius(2f, 2f))
                    // Dual Chrome Exhaust Pipes
                    drawCircle(color = Color(0xFFCFD8DC), radius = 5f, center = Offset(px - 22f, py + carH/2))
                    drawCircle(color = Color(0xFF212121), radius = 3f, center = Offset(px - 22f, py + carH/2))
                    drawCircle(color = Color(0xFFCFD8DC), radius = 5f, center = Offset(px + 22f, py + carH/2))
                    drawCircle(color = Color(0xFF212121), radius = 3f, center = Offset(px + 22f, py + carH/2))

                    // H. Active Auras (Shield, Magnet)
                    if (engine.hasShield) {
                        val sPulse = 1f + (kotlin.math.sin(System.currentTimeMillis() / 110.0).toFloat() * 0.05f)
                        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF76FF03).copy(alpha = 0.15f), Color(0xFF76FF03).copy(alpha = 0.45f))), radius = 114f * sPulse, center = Offset(px, py))
                        drawCircle(color = Color(0xFF76FF03), radius = 114f * sPulse, center = Offset(px, py), style = Stroke(width = 3.dp.toPx()))
                    }

                    if (engine.magnetTimer > 0) {
                        val mPulse = (System.currentTimeMillis() % 1000) / 1000f
                        drawCircle(color = Color(0xFF00E5FF).copy(alpha = (1f - mPulse) * 0.6f), radius = 60f + (mPulse * 160f), center = Offset(px, py), style = Stroke(width = 2.dp.toPx()))
                    }

                    // I. POLICE CRUISER PURSUIT (Behind or Pulling Beside Player)
                    if (engine.policeChaseActive) {
                        val polY = py - engine.policeCarYOffset
                        val pCarW = 96f
                        val pCarH = 176f
                        val pStrobeFlash = (System.currentTimeMillis() / 90) % 2 == 0L

                        // Helper function to draw a Police Cruiser vehicle
                        val drawPoliceCar = { cX: Float, cY: Float ->
                            // Shadow
                            drawOval(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(cX - pCarW/2 - 6f, cY - pCarH/2 + 6f), size = Size(pCarW + 12f, pCarH + 12f))
                            // Main Black Body
                            drawRoundRect(color = Color(0xFF151515), topLeft = Offset(cX - pCarW/2, cY - pCarH/2), size = Size(pCarW, pCarH), cornerRadius = CornerRadius(16f, 16f))
                            // White Doors & Roof
                            drawRect(color = Color.White, topLeft = Offset(cX - pCarW/2 + 6f, cY - pCarH/4 - 10f), size = Size(pCarW - 12f, 90f))
                            // Bullbar Push Bumper
                            drawRect(color = Color(0xFF37474F), topLeft = Offset(cX - pCarW/2 + 18f, cY - pCarH/2 - 6f), size = Size(pCarW - 36f, 8f))
                            // Windshield & Rear Window
                            drawRoundRect(color = Color(0xFF0D47A1).copy(alpha = 0.85f), topLeft = Offset(cX - pCarW/2 + 14f, cY - pCarH/4 - 4f), size = Size(pCarW - 28f, 38f), cornerRadius = CornerRadius(4f, 4f))
                            // Flashing Emergency Strobe Lights
                            drawRoundRect(color = if (pStrobeFlash) Color(0xFFFF1744) else Color(0xFF2979FF), topLeft = Offset(cX - 26f, cY - 4f), size = Size(24f, 12f), cornerRadius = CornerRadius(3f, 3f))
                            drawRoundRect(color = if (!pStrobeFlash) Color(0xFFFF1744) else Color(0xFF2979FF), topLeft = Offset(cX + 2f, cY - 4f), size = Size(24f, 12f), cornerRadius = CornerRadius(3f, 3f))
                            // Headlight Beams
                            val pBeamL = Path().apply {
                                moveTo(cX - 32f, cY - pCarH/2 + 10f)
                                lineTo(cX - 14f, cY - pCarH/2 + 10f)
                                lineTo(cX - 60f, cY - pCarH/2 - 180f)
                                lineTo(cX - 110f, cY - pCarH/2 - 180f)
                                close()
                            }
                            val pBeamR = Path().apply {
                                moveTo(cX + 14f, cY - pCarH/2 + 10f)
                                lineTo(cX + 32f, cY - pCarH/2 + 10f)
                                lineTo(cX + 110f, cY - pCarH/2 - 180f)
                                lineTo(cX + 60f, cY - pCarH/2 - 180f)
                                close()
                            }
                            drawPath(pBeamL, color = Color(0xFFFFF9C4).copy(alpha = 0.25f))
                            drawPath(pBeamR, color = Color(0xFFFFF9C4).copy(alpha = 0.25f))
                        }

                        // 1 Star: 1 Police Car behind
                        drawPoliceCar(px, polY)

                        // 2 Stars: 2 Police Cars
                        if (engine.wantedLevel >= 2) {
                            drawPoliceCar(px - 110f, polY + 60f)
                        }
                        // 3 Stars: 3 Police Cars
                        if (engine.wantedLevel >= 3) {
                            drawPoliceCar(px + 110f, polY + 60f)
                        }
                    }

                    // J. POLICE HELICOPTER SPOTLIGHT (Wanted Level 3)
                    if (engine.wantedLevel == 3) {
                        val spotPulse = 1f + (kotlin.math.sin(System.currentTimeMillis() / 90.0).toFloat() * 0.08f)
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFFFFF59D).copy(alpha = 0.55f), Color.Transparent)),
                            radius = 160f * spotPulse,
                            center = Offset(px, py - 30f)
                        )
                        // Helicopter Shadow Silhouette Overlay
                        drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = 220f, center = Offset(px + 50f, py + 120f), style = Stroke(width = 4.dp.toPx()))
                        val bladeAngle = (System.currentTimeMillis() % 360).toFloat()
                        rotate(degrees = bladeAngle * 5f, pivot = Offset(px + 50f, py + 120f)) {
                            drawLine(color = Color.Black.copy(alpha = 0.35f), start = Offset(px - 100f, py + 120f), end = Offset(px + 200f, py + 120f), strokeWidth = 8f)
                        }
                    }
                }
            }


            // --- HUD & UI Overlays ---
            // 1. Top HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Distance Pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🏁", fontSize = 16.sp)
                            Text(
                                text = "${engine.score.toInt()}m",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                        }
                    }

                    // Active Power-Up, Ghost & Wanted Badges — arranged in two lines to avoid overlap
                    // Line 1: Wanted + Shield + Magnet + Nitro
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (engine.wantedLevel > 0) {
                            Surface(color = Color(0xFFFF1744), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "🚨 WANTED " + "★".repeat(engine.wantedLevel),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (engine.hasShield) {
                            Surface(color = Color(0xFF76FF03).copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp)) {
                                Text("🛡️ SHIELD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                        if (engine.magnetTimer > 0) {
                            Surface(color = Color(0xFF00E5FF).copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp)) {
                                Text("🧲 ${engine.magnetTimer.toInt()}s", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                        if (engine.nitroTimer > 0) {
                            Surface(color = Color(0xFFFF6D00).copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp)) {
                                Text("⚡ NITRO ${engine.nitroTimer.toInt()}s", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }
                    // Line 2: Best Score / Ghost Run Tracker (BEHIND until best score beaten, then AHEAD + NEW RECORD)
                    val targetBestDistance = kotlin.math.max(engine.highScore * 10f, engine.ghost?.ghostMaxDistance ?: 0f)
                    if (targetBestDistance > 0f && engine.gameState == GameState.PLAYING) {
                        val diffMeters = (engine.distanceTravelled - targetBestDistance) / 10f
                        val isAhead = diffMeters >= 0f
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isAhead) Color(0xFFFFD700).copy(alpha = 0.22f) else Color(0xFF00E5FF).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAhead) Color(0xFFFFD700).copy(alpha = 0.8f) else Color(0xFF00E5FF).copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = if (isAhead) "👑 NEW RECORD" else "👻 GHOST BEST",
                                    color = if (isAhead) Color(0xFFFFD700) else Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = if (isAhead) Color(0xFF00C853).copy(alpha = 0.85f)
                                        else Color(0xFFFF6D00).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isAhead) "AHEAD +${diffMeters.toInt()}m"
                                           else "BEHIND -${(-diffMeters).toInt()}m",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Coins & High Score Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    text = "${com.karthik.data.SaveManager.totalCoins}",
                                    color = Color(0xFFFFD600),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🏆", fontSize = 14.sp)
                                Text(
                                    text = "${com.karthik.data.SaveManager.highScore.toInt()}m",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Pause Button
                IconButton(
                    onClick = { 
                        AudioManager.playSfx(context, "button")
                        if (engine.gameState == GameState.PLAYING) engine.pause() else engine.resume() 
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = if (engine.gameState == GameState.PAUSED) 1f else 0.25f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = if (engine.gameState == GameState.PAUSED) "▶️" else "⏸️", fontSize = 20.sp)
                        }
                    }
                }
            }

            // 2. Police Chase Warning Bar
            if (engine.policeChaseActive) {
                Surface(
                    color = Color(0xFFFF1744).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🚨", fontSize = 18.sp)
                        Text(
                            text = "POLICE CHASE! HIT NITRO TO ESCAPE (${engine.policeChaseTimer.toInt()}s)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 3. Sidewalk Close Call Burst Notification
            AnimatedVisibility(
                visible = showCloseCall,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 1.3f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 160.dp)
            ) {
                Text(
                    text = closeCallText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.graphicsLayer { shadowElevation = 15.dp.toPx() }
                )
            }

            // 4. Near-Miss Floating Burst
            AnimatedVisibility(
                visible = showNearMiss,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 1.3f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp)
            ) {
                Text(
                    text = nearMissText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD600),
                    modifier = Modifier.graphicsLayer { shadowElevation = 15.dp.toPx() }
                )
            }

            // 5. Milestone Notification
            AnimatedVisibility(
                visible = showMilestone,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 1.2f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = milestoneText,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer { shadowElevation = 20.dp.toPx() }
                    )
                    Text(
                        text = "MILESTONE REACHED!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Cyan
                    )
                }
            }

            // 6. Dedicated Nitro Boost HUD Button (Bottom Right)
            if (engine.gameState == GameState.PLAYING) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 36.dp, end = 20.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Button(
                        onClick = { engine.triggerNitroBoost() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (engine.nitroTimer > 0) Color(0xFFFF9100)
                                else if (engine.nitroCharges > 0) Color(0xFFFF5722)
                                else Color(0xFF555555)
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", fontSize = 20.sp)
                            Text(
                                text = if (engine.nitroTimer > 0) "ACTIVE"
                                    else "x${engine.nitroCharges}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 7. Slow Motion Button
            if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (engine.isSlowMotion) {
                            Text(
                                "SLOW MOTION ACTIVE", 
                                color = Color.Cyan, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        
                        Button(
                            onClick = { 
                                if (activity != null) {
                                    engine.pause()
                                    AdManager.showRewardedAd(
                                        activity = activity,
                                        onDismissed = { engine.resume() }
                                    ) { 
                                        engine.activateSlowMo(10f) 
                                    } 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
                        ) {
                            Text("SLOW MOTION (AD)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 8. First-Time Tutorial Overlay
            if (showTutorial) {
                TutorialOverlay(
                    onComplete = {
                        saveManager.setTutorialCompleted(true)
                        showTutorial = false
                    },
                    onSkip = {
                        saveManager.setTutorialCompleted(true)
                        showTutorial = false
                    }
                )
            }

            // 9. Idle Start Screen
            if (engine.gameState == GameState.IDLE && !showTutorial) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TAP TO START", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (activity != null) {
                            Button(onClick = { 
                                AdManager.showRewardedAd(activity, onDismissed = null) { engine.boostStart() } 
                            }) {
                                Text("BOOST START (AD)")
                            }
                        }
                    }
                }
            }

            // 9. Police Busted Fine Dialog Overlay
            if (engine.gameState == GameState.POLICE_BUSTED) {
                PoliceBustedOverlay(engine, activity, snackbarHostState, scope)
            }

            // 10. Game Over Overlay
            if (engine.gameState == GameState.GAME_OVER) {
                GameOverOverlay(engine, activity, onBackToMenu, onOpenLeaderboard, snackbarHostState, scope)
            }
        }
    }
}

@Composable
fun PoliceBustedOverlay(
    engine: GameEngine,
    activity: Activity?,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .background(Brush.verticalGradient(listOf(Color(0xFFB71C1C).copy(alpha = 0.94f), Color(0xFF880E4F).copy(alpha = 0.96f)))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF1744))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "BUSTED BY POLICE 🚔",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1744)
                )

                Text(
                    text = "You hit a pedestrian on the footpath! (Offense #${engine.policeCatchCount})",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECKLESS DRIVING FINE", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${engine.currentFineAmount} 💰", color = Color(0xFFFFD600), fontSize = 44.sp, fontWeight = FontWeight.Black)
                }

                Divider(color = Color.White.copy(alpha = 0.15f))

                // Pay Fine Button
                Button(
                    onClick = {
                        if (!engine.payFine()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Not enough coins to pay fine! 🔒")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                ) {
                    Text("PAY FINE (${engine.currentFineAmount} 💰)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                // Watch Ad to Waive Fine Button
                if (activity != null) {
                    Button(
                        onClick = {
                            AdManager.showRewardedAd(activity, onDismissed = null) {
                                engine.waiveFineWithAd()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Text("WATCH AD (WAIVE FINE 🎬)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Give Up Button
                OutlinedButton(
                    onClick = {
                        engine.giveUpPolice()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Text("SURRENDER (GAME OVER)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    engine: GameEngine, 
    activity: Activity?, 
    onBackToMenu: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val isNewHighScore = engine.score >= com.karthik.data.SaveManager.highScore

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E).copy(alpha = 0.92f), Color(0xFF0D47A1).copy(alpha = 0.96f)))), 
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isNewHighScore) "NEW BEST! 🏆" else "GAME OVER 💥", 
                    fontSize = 30.sp, 
                    fontWeight = FontWeight.Black, 
                    color = if (isNewHighScore) Color(0xFFFFD600) else Color.White
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${engine.score.toInt()}m", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
                }

                // Row: Share + Leaderboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Score Button
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🏁 I survived ${engine.score.toInt()}m in One Tap Escape: Car Dodge! Can you beat my high score? Download now! 👉 https://play.google.com/store/apps/details?id=com.karthik&pcampaignid=web_share"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Your Score"))
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    ) {
                        Text("📢 SHARE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    // Leaderboard Button
                    Button(
                        onClick = {
                            onOpenLeaderboard()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    ) {
                        Text("🏆 RANKS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackToMenu, 
                        modifier = Modifier.weight(0.35f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("🏠", fontSize = 22.sp)
                    }
                    
                    Button(
                        onClick = { engine.reset(autoStart = true) }, 
                        modifier = Modifier.weight(0.65f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("RETRY", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
                
                if (activity != null) {
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    
                    Button(
                        onClick = { 
                            AdManager.showRewardedAd(activity, onDismissed = null) { engine.revive() } 
                        }, 
                        modifier = Modifier.fillMaxWidth().height(50.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Text("REVIVE (AD)", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            if (!engine.reviveWithCoins()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Not enough coins! 🔒")
                                }
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("REVIVE (100 💰)", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
