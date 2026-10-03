package com.karthik.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.karthik.data.SaveManager

enum class GameTheme {
    VIBRANT_CITY, CYBERPUNK_NIGHT, DESERT_OUTRUN, MIDNIGHT_FOREST
}

enum class GameState {
    IDLE, PLAYING, PAUSED, GAME_OVER, POLICE_BUSTED
}

enum class PowerUpType {
    MAGNET, SHIELD, NITRO
}

data class Obstacle(
    val id: Long,
    val x: Float,
    var y: Float,
    val width: Float = 120f,
    val height: Float = 40f,
    var nearMissChecked: Boolean = false,
    val isRoadblock: Boolean = false
)

data class Pedestrian(
    val id: Long,
    val isLeft: Boolean,
    var x: Float,
    var y: Float,
    val speed: Float,
    val direction: Float,
    val shirtColor: Color,
    val pantsColor: Color,
    var isHit: Boolean = false,
    var nearMissChecked: Boolean = false
)

data class Coin(
    val id: Long,
    var x: Float,
    var y: Float
)

data class CollectedCoin(
    val startX: Float,
    val startY: Float,
    var progress: Float = 0f,
    val id: Long
)

data class PowerUp(
    val id: Long,
    val type: PowerUpType,
    val x: Float,
    val y: Float
)

data class GameParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var alpha: Float = 1f,
    val size: Float = 10f,
    var life: Float = 1f
)

class GameEngine(private val saveManager: SaveManager? = null, context: android.content.Context? = null) {
    var gameState by mutableStateOf(GameState.IDLE)
    var score by mutableStateOf(0f)
    var highScore by mutableStateOf(0f)

    // Ghost Run
    val ghost: GhostRecorder? = context?.let { GhostRecorder(it) }
    var ghostX by mutableStateOf(0f)
    var coinsCollected by mutableStateOf(0)
    
    // Player State
    var playerX by mutableStateOf(0f)
    var playerY by mutableStateOf(0f)
    var currentLaneIndex by mutableStateOf(0) // -1, 0, 1
    var playerColor by mutableStateOf(Color(0xFF00E5FF))
    var selectedModel by mutableStateOf("SPEED_RACER")
    
    var obstacles = mutableStateListOf<Obstacle>()
    var coins = mutableStateListOf<Coin>()
    var animatingCoins = mutableStateListOf<CollectedCoin>()
    var powerUps = mutableStateListOf<PowerUp>()
    var particles = mutableStateListOf<GameParticle>()
    var pedestrians = mutableStateListOf<Pedestrian>()
    
    var distanceTravelled by mutableStateOf(0f)
    var lastCheckpointDistance by mutableStateOf(0f)
    var hasCheckpoint by mutableStateOf(false)
    var isSlowMotion by mutableStateOf(false)
    private var slowMoTimer = 0f
    private var currentSpeedMultiplier = 1.0f
    var invincibilityTimer by mutableStateOf(0f)
    var selectedTheme by mutableStateOf(GameTheme.VIBRANT_CITY)

    // Power-up States
    var magnetTimer by mutableStateOf(0f)
    var hasShield by mutableStateOf(false)
    var nitroTimer by mutableStateOf(0f)
    var nitroCharges by mutableStateOf(3)

    // Police Chase & Wanted System
    var wantedLevel by mutableStateOf(0) // 0 to 3 Stars
    var policeChaseActive by mutableStateOf(false)
    var policeChaseTimer by mutableStateOf(0f)
    var policeCarYOffset by mutableStateOf(-350f)
    var policeCatchCount by mutableStateOf(0)
    var currentFineAmount by mutableStateOf(50)
    var policeEscapeTrigger by mutableStateOf(false)
    var policeBustedTrigger by mutableStateOf(false)
    var policeCloseCallText by mutableStateOf("")
    var policeCloseCallTrigger by mutableStateOf(false)
    private var sirenTimer = 0f

    // Screen Shake & Near-Miss
    var screenShakeTimer by mutableStateOf(0f)
    var screenShakeIntensity by mutableStateOf(0f)
    var nearMissTrigger by mutableStateOf(false)
    var nearMissCount by mutableStateOf(0)
    var lastNearMissText by mutableStateOf("")

    // Callbacks for audio & haptics
    var onPlaySfx: ((String) -> Unit)? = null
    var onVibrate: ((String) -> Unit)? = null

    private var nextSpawnY = 1000f
    private var nextPedestrianSpawnY = 600f
    private var nextObjectId = 0L

    private var moveSpeed = 500f
    val horizontalOffset = 140f // Dist between center of lanes
    var coinRotationFrame by mutableStateOf(0f)
    
    // Milestone Trigger
    var lastMilestoneReached by mutableStateOf(0)
    var milestoneTrigger by mutableStateOf(false)

    init {
        highScore = saveManager?.highScore ?: 0f
        val hex = saveManager?.getSelectedColor() ?: "#00E5FF"
        playerColor = Color(android.graphics.Color.parseColor(hex))
        
        val themeStr = saveManager?.getSelectedTheme() ?: "VIBRANT_CITY"
        selectedTheme = try { GameTheme.valueOf(themeStr) } catch(e: Exception) { GameTheme.VIBRANT_CITY }
        selectedModel = saveManager?.getSelectedModel() ?: "SPEED_RACER"
    }

    fun update(deltaTime: Float) {
        if (gameState != GameState.PLAYING) return

        // Ghost Run: record current lane and update ghost X
        ghost?.record(distanceTravelled, currentLaneIndex)
        ghostX = ghost?.updateGhostX(distanceTravelled, horizontalOffset) ?: 0f

        // Power-Up Countdown Timers
        if (magnetTimer > 0) magnetTimer = (magnetTimer - deltaTime).coerceAtLeast(0f)
        if (nitroTimer > 0) nitroTimer = (nitroTimer - deltaTime).coerceAtLeast(0f)

        // Screen Shake decay
        if (screenShakeTimer > 0) {
            screenShakeTimer = (screenShakeTimer - deltaTime).coerceAtLeast(0f)
        }

        // Update Particles
        val partIterator = particles.iterator()
        while (partIterator.hasNext()) {
            val p = partIterator.next()
            p.x += p.vx * deltaTime
            p.y += p.vy * deltaTime
            p.life -= deltaTime * 2f
            p.alpha = p.life.coerceIn(0f, 1f)
            if (p.life <= 0f) partIterator.remove()
        }

        // Speed Multiplier calculation
        val targetMultiplier = when {
            nitroTimer > 0 -> 1.75f
            isSlowMotion -> 0.5f
            else -> 1.0f
        }

        currentSpeedMultiplier = lerp(currentSpeedMultiplier, targetMultiplier, 0.08f)
        if (isSlowMotion) {
            slowMoTimer -= deltaTime
            if (slowMoTimer <= 0) isSlowMotion = false
        }
        
        val currentSpeed = moveSpeed * currentSpeedMultiplier
        playerY += currentSpeed * deltaTime
        distanceTravelled = playerY
        score = distanceTravelled / 10
        
        // Milestone Detection (Every 1000m)
        val currentMilestone = (score.toInt() / 1000) * 1000
        if (currentMilestone > 0 && currentMilestone > lastMilestoneReached) {
            lastMilestoneReached = currentMilestone
            milestoneTrigger = true
            onPlaySfx?.invoke("powerup")
            onVibrate?.invoke("double")
        }

        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime
        }

        // Target X interpolation based on current lane
        val targetX = currentLaneIndex * horizontalOffset
        playerX = lerp(playerX, targetX, 0.22f)
        
        coinRotationFrame = (coinRotationFrame + 6f) % 360f
        
        // Difficulty scaling logic
        val progressScale = (distanceTravelled / 40000f).coerceIn(0f, 3f) 
        val targetSpeed = 500f + (progressScale * 250f)
        moveSpeed = lerp(moveSpeed, targetSpeed, 0.01f)
        
        val targetSpacing = (800f - (progressScale * 60f)).coerceAtLeast(500f)
        
        // Object spawning
        if (playerY + 2000f > nextSpawnY) {
            spawnObjects(targetSpacing)
        }

        // Pedestrian spawning
        if (playerY + 1800f > nextPedestrianSpawnY) {
            spawnPedestrians()
        }

        // Checkpoint logic
        if (distanceTravelled - lastCheckpointDistance > 2500f) {
            lastCheckpointDistance = distanceTravelled
            hasCheckpoint = true
        }

        // Update Pedestrians & Check Collisions / Close Calls
        val pedIterator = pedestrians.iterator()
        while (pedIterator.hasNext()) {
            val ped = pedIterator.next()
            ped.y += ped.speed * ped.direction * deltaTime

            // Pedestrian Collision with Car
            val dx = kotlin.math.abs(playerX - ped.x)
            val dy = kotlin.math.abs(playerY - ped.y)

            if (!ped.isHit && dx < 60f && dy < 75f) {
                ped.isHit = true
                spawnExplosion(ped.x, ped.y, Color(0xFFFF1744), 22)
                triggerShake(0.4f, 20f)
                onPlaySfx?.invoke("crash")
                onVibrate?.invoke("heavy")

                // Trigger Police Chase & Wanted Level!
                wantedLevel = (wantedLevel + 1).coerceAtMost(3)
                policeChaseActive = true
                policeChaseTimer = 6.0f
                policeCarYOffset = -380f
                onPlaySfx?.invoke("siren")
            }

            // Pedestrian Close Call (Near Miss on Sidewalk)
            if (!ped.isHit && !ped.nearMissChecked && dy < 80f && dx in 60f..115f) {
                ped.nearMissChecked = true
                policeCloseCallText = "SIDEWALK CLOSE CALL! +15 💰"
                policeCloseCallTrigger = true
                saveManager?.addCoins(15)
                score += 30f
                onPlaySfx?.invoke("near_miss")
                onVibrate?.invoke("light")
            }

            if (ped.y < playerY - 1200f || ped.y > playerY + 2200f) {
                pedIterator.remove()
            }
        }

        // Update Police Chase Mechanics
        if (policeChaseActive) {
            sirenTimer -= deltaTime
            if (sirenTimer <= 0f) {
                sirenTimer = 1.2f
                onPlaySfx?.invoke("siren")
            }

            if (nitroTimer > 0) {
                // Outrunning police with Nitro!
                policeCarYOffset = lerp(policeCarYOffset, -750f, 0.08f)
                policeChaseTimer -= deltaTime * 1.8f
                if (policeChaseTimer <= 0f || policeCarYOffset < -650f) {
                    policeChaseActive = false
                    wantedLevel = 0
                    policeEscapeTrigger = true
                    score += 250f
                    onPlaySfx?.invoke("powerup")
                    onVibrate?.invoke("double")
                }
            } else {
                // Police gaining ground!
                policeCarYOffset = lerp(policeCarYOffset, -90f, 0.035f)
                policeChaseTimer -= deltaTime
                if (policeChaseTimer <= 0f || policeCarYOffset >= -100f) {
                    // POLICE BUSTED!
                    policeCatchCount++
                    val multiplier = (1 shl (policeCatchCount - 1).coerceAtMost(4))
                    currentFineAmount = 50 * multiplier
                    policeChaseActive = false
                    gameState = GameState.POLICE_BUSTED
                    policeBustedTrigger = true
                    onPlaySfx?.invoke("crash")
                    onVibrate?.invoke("heavy")
                }
            }
        }

        // Update obstacles and check collisions & near-misses
        val obsIterator = obstacles.iterator()
        while (obsIterator.hasNext()) {
            val obs = obsIterator.next()
            
            // Nitro Smash through obstacles
            if (nitroTimer > 0 && checkCollision(obs)) {
                spawnExplosion(obs.x, obs.y, Color(0xFFFF9100), 18)
                triggerShake(0.2f, 10f)
                onPlaySfx?.invoke("crash")
                onVibrate?.invoke("medium")
                score += 50f
                obsIterator.remove()
                continue
            }

            // Normal Collision
            if (checkCollision(obs)) {
                if (hasShield) {
                    // Shield protects!
                    hasShield = false
                    invincibilityTimer = 2.5f
                    spawnExplosion(playerX, playerY, Color(0xFF00E5FF), 20)
                    triggerShake(0.3f, 15f)
                    onPlaySfx?.invoke("crash")
                    onVibrate?.invoke("heavy")
                    obsIterator.remove()
                    continue
                } else {
                    // Game Over
                    spawnExplosion(playerX, playerY, Color(0xFFFF1744), 35)
                    triggerShake(0.5f, 25f)
                    onPlaySfx?.invoke("crash")
                    onVibrate?.invoke("heavy")
                    val prevBest = highScore
                    gameState = GameState.GAME_OVER
                    if (score > highScore) {
                        highScore = score
                        saveManager?.saveHighScore(highScore)
                    }
                    ghost?.commitIfBest(score, prevBest)
                    break
                }
            }

            // Near-Miss Detection
            if (!obs.nearMissChecked && obs.y < playerY && (playerY - obs.y) < 100f) {
                obs.nearMissChecked = true
                val lateralDist = kotlin.math.abs(playerX - obs.x)
                if (lateralDist in 80f..170f) {
                    // Close shave near miss!
                    nearMissCount++
                    val bonus = 25f * (1 + (nearMissCount / 5))
                    score += bonus
                    lastNearMissText = "⚡ NEAR MISS! +${bonus.toInt()}"
                    nearMissTrigger = true
                    onPlaySfx?.invoke("near_miss")
                    onVibrate?.invoke("light")
                }
            }

            if (obs.y < playerY - 400f) {
                obsIterator.remove()
            }
        }

        // Power-Up Collection
        val pUpIterator = powerUps.iterator()
        while (pUpIterator.hasNext()) {
            val pUp = pUpIterator.next()
            if (checkPowerUpCollection(pUp)) {
                when (pUp.type) {
                    PowerUpType.MAGNET -> magnetTimer = 10f
                    PowerUpType.SHIELD -> hasShield = true
                    PowerUpType.NITRO -> {
                        nitroTimer = 5f
                        invincibilityTimer = 5f
                    }
                }
                spawnExplosion(pUp.x, pUp.y, Color(0xFF00E5FF), 12)
                triggerShake(0.15f, 8f)
                onPlaySfx?.invoke("powerup")
                onVibrate?.invoke("double")
                pUpIterator.remove()
            } else if (pUp.y < playerY - 400f) {
                pUpIterator.remove()
            }
        }

        // Coin collection & Magnet attraction
        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()

            // Magnet pulling logic
            if (magnetTimer > 0) {
                val dx = playerX - coin.x
                val dy = playerY - coin.y
                val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < 500f) {
                    coin.x = lerp(coin.x, playerX, 0.18f)
                    coin.y = lerp(coin.y, playerY, 0.18f)
                }
            }

            if (checkCoinCollection(coin)) {
                animatingCoins.add(CollectedCoin(coin.x, coin.y, 0f, coin.id))
                coinsCollected++
                saveManager?.addCoins(1)
                onPlaySfx?.invoke("coin")
                onVibrate?.invoke("light")
                coinIterator.remove()
            } else if (coin.y < playerY - 1000f) {
                coinIterator.remove()
            }
        }

        // Update animating coins
        val animIterator = animatingCoins.iterator()
        while (animIterator.hasNext()) {
            val anim = animIterator.next()
            anim.progress += deltaTime * 2f
            if (anim.progress >= 1.0f) {
                animIterator.remove()
            }
        }
    }

    private fun spawnPedestrians() {
        val leftX = -280f + (Math.random().toFloat() * 10f - 5f)
        val rightX = 280f + (Math.random().toFloat() * 10f - 5f)
        val shirtColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA))
        val pantsColors = listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF1565C0))

        pedestrians.add(
            Pedestrian(
                id = nextObjectId++,
                isLeft = true,
                x = leftX,
                y = nextPedestrianSpawnY,
                speed = 30f + Math.random().toFloat() * 30f,
                direction = if (Math.random() < 0.5) 1f else -1f,
                shirtColor = shirtColors.random(),
                pantsColor = pantsColors.random()
            )
        )

        pedestrians.add(
            Pedestrian(
                id = nextObjectId++,
                isLeft = false,
                x = rightX,
                y = nextPedestrianSpawnY + 220f,
                speed = 30f + Math.random().toFloat() * 30f,
                direction = if (Math.random() < 0.5) 1f else -1f,
                shirtColor = shirtColors.random(),
                pantsColor = pantsColors.random()
            )
        )

        nextPedestrianSpawnY += 450f + Math.random().toFloat() * 200f
    }

    private fun spawnObjects(currentSpacing: Float) {
        val laneIndices = listOf(-1, 0, 1)
        val obstacleLane = laneIndices.random()
        
        // Spawn standard cone/barrier obstacle or Police Roadblock at higher wanted level
        val isRoadblock = wantedLevel >= 2 && Math.random() < 0.4
        obstacles.add(
            Obstacle(
                id = nextObjectId++,
                x = obstacleLane * horizontalOffset,
                y = nextSpawnY,
                width = if (isRoadblock) 150f else 120f,
                isRoadblock = isRoadblock
            )
        )
        
        // Spawn coins
        if (Math.random() < 0.65) {
            val freeLanes = laneIndices.filter { it != obstacleLane }
            val coinLane = freeLanes.random()
            coins.add(Coin(nextObjectId++, coinLane * horizontalOffset, nextSpawnY + (currentSpacing / 2)))
        }

        // Rare Power-Up Spawn (18% chance)
        if (Math.random() < 0.18) {
            val freeLanes = laneIndices.filter { it != obstacleLane }
            val pLane = freeLanes.random()
            val type = PowerUpType.values().random()
            powerUps.add(PowerUp(nextObjectId++, type, pLane * horizontalOffset, nextSpawnY + (currentSpacing * 0.75f)))
        }

        nextSpawnY += currentSpacing
    }

    fun triggerNitroBoost() {
        if (gameState == GameState.PLAYING && (nitroCharges > 0 || nitroTimer > 0)) {
            if (nitroTimer <= 0) nitroCharges = (nitroCharges - 1).coerceAtLeast(0)
            nitroTimer = 4.0f
            invincibilityTimer = 4.0f
            onPlaySfx?.invoke("powerup")
            onVibrate?.invoke("double")
        }
    }

    fun payFine(): Boolean {
        if (saveManager?.totalCoins?.let { it >= currentFineAmount } == true) {
            saveManager.addCoins(-currentFineAmount)
            resumeFromBusted()
            return true
        }
        return false
    }

    fun waiveFineWithAd() {
        resumeFromBusted()
    }

    private fun resumeFromBusted() {
        policeChaseActive = false
        wantedLevel = 0
        invincibilityTimer = 3.5f
        hasShield = true
        gameState = GameState.PLAYING
        onPlaySfx?.invoke("powerup")
    }

    fun giveUpPolice() {
        gameState = GameState.GAME_OVER
        if (score > highScore) {
            highScore = score
            saveManager?.saveHighScore(highScore)
        }
    }

    private fun checkCollision(obs: Obstacle): Boolean {
        if (invincibilityTimer > 0) return false
        val playerRadius = 45f
        val rectLeft = obs.x - obs.width / 2
        val rectRight = obs.x + obs.width / 2
        val rectTop = obs.y + obs.height / 2
        val rectBottom = obs.y - obs.height / 2

        return (playerX + playerRadius > rectLeft && playerX - playerRadius < rectRight &&
                playerY + playerRadius > rectBottom && playerY - playerRadius < rectTop)
    }

    private fun checkCoinCollection(coin: Coin): Boolean {
        val playerRadius = 45f
        val distSq = (playerX - coin.x) * (playerX - coin.x) + (playerY - coin.y) * (playerY - coin.y)
        return distSq < (playerRadius + 35f) * (playerRadius + 35f)
    }

    private fun checkPowerUpCollection(pUp: PowerUp): Boolean {
        val playerRadius = 50f
        val distSq = (playerX - pUp.x) * (playerX - pUp.x) + (playerY - pUp.y) * (playerY - pUp.y)
        return distSq < (playerRadius + 40f) * (playerRadius + 40f)
    }

    private fun spawnExplosion(x: Float, y: Float, color: Color, count: Int) {
        val random = java.util.Random()
        for (i in 0 until count) {
            val angle = random.nextDouble() * 2.0 * Math.PI
            val speed = 150f + random.nextFloat() * 400f
            val vx = (kotlin.math.cos(angle) * speed).toFloat()
            val vy = (kotlin.math.sin(angle) * speed).toFloat()
            val pColor = if (random.nextBoolean()) color else Color(0xFFFFD600)
            particles.add(GameParticle(x, y, vx, vy, pColor, 1f, 8f + random.nextFloat() * 10f, 1f))
        }
    }

    fun triggerShake(duration: Float, intensity: Float) {
        screenShakeTimer = duration
        screenShakeIntensity = intensity
    }

    fun onTouch(isLeft: Boolean) {
        if (gameState == GameState.IDLE) {
            gameState = GameState.PLAYING
            onPlaySfx?.invoke("button")
            return
        }
        
        if (gameState == GameState.PLAYING) {
            val prevLane = currentLaneIndex
            if (isLeft) {
                if (currentLaneIndex > -2) currentLaneIndex--
            } else {
                if (currentLaneIndex < 2) currentLaneIndex++
            }
            if (currentLaneIndex != prevLane) {
                onPlaySfx?.invoke("swerve")
                onVibrate?.invoke("light")
            }
        }
    }

    fun pause() {
        if (gameState == GameState.PLAYING) gameState = GameState.PAUSED
    }

    fun resume() {
        if (gameState == GameState.PAUSED) gameState = GameState.PLAYING
    }

    fun revive() {
        playerY = lastCheckpointDistance
        nextSpawnY = playerY + 1000f
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        animatingCoins.clear()
        pedestrians.clear()
        policeChaseActive = false
        wantedLevel = 0
        invincibilityTimer = 3f
        hasShield = true // Bonus safety shield on revive
        gameState = GameState.PLAYING
        onPlaySfx?.invoke("powerup")
    }

    fun reviveWithCoins(): Boolean {
        if (saveManager?.totalCoins?.let { it >= 100 } == true) {
            saveManager.addCoins(-100)
            revive()
            return true
        }
        return false
    }

    fun boostStart() {
        playerY += 10000f
        nextSpawnY = playerY + 1000f
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        animatingCoins.clear()
        pedestrians.clear()
        invincibilityTimer = 4f
        nitroTimer = 4f
        nitroCharges = 3
        gameState = GameState.PLAYING
        onPlaySfx?.invoke("powerup")
    }

    fun activateSlowMo(duration: Float) {
        isSlowMotion = true
        slowMoTimer = duration
        invincibilityTimer = 2f
        if (gameState == GameState.PAUSED) gameState = GameState.PLAYING
    }
    
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }

    fun reset(autoStart: Boolean = false) {
        score = 0f
        coinsCollected = 0
        distanceTravelled = 0f
        lastCheckpointDistance = 0f
        hasCheckpoint = false
        playerX = 0f
        playerY = 0f
        currentLaneIndex = 0
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        particles.clear()
        animatingCoins.clear()
        pedestrians.clear()
        wantedLevel = 0
        policeChaseActive = false
        policeChaseTimer = 0f
        policeCarYOffset = -350f
        policeCatchCount = 0
        currentFineAmount = 50
        policeEscapeTrigger = false
        policeBustedTrigger = false
        policeCloseCallTrigger = false
        nitroCharges = 3
        nextSpawnY = 1000f
        nextPedestrianSpawnY = 600f
        moveSpeed = 500f
        currentSpeedMultiplier = 1.0f
        invincibilityTimer = 0f
        isSlowMotion = false
        slowMoTimer = 0f
        magnetTimer = 0f
        hasShield = false
        nitroTimer = 0f
        screenShakeTimer = 0f
        screenShakeIntensity = 0f
        nearMissCount = 0
        nearMissTrigger = false
        lastMilestoneReached = 0
        milestoneTrigger = false
        val hex = saveManager?.getSelectedColor() ?: "#00E5FF"
        playerColor = Color(android.graphics.Color.parseColor(hex))
        val themeStr = saveManager?.getSelectedTheme() ?: "VIBRANT_CITY"
        selectedTheme = try { GameTheme.valueOf(themeStr) } catch(e: Exception) { GameTheme.VIBRANT_CITY }
        selectedModel = saveManager?.getSelectedModel() ?: "SPEED_RACER"
        // Start ghost recording fresh for the new run
        ghost?.startRecording()
        gameState = if (autoStart) GameState.PLAYING else GameState.IDLE
    }
}
