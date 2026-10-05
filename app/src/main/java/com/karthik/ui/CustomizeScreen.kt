package com.karthik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthik.audio.AudioManager
import com.karthik.data.SaveManager
import kotlinx.coroutines.launch

data class CarModelItem(
    val id: String,
    val name: String,
    val price: Int,
    val icon: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context) }
    val coins = saveManager.totalCoins
    
    val purchasedColors = remember { mutableStateListOf<String>().apply { addAll(saveManager.getPurchasedColors()) } }
    var selectedColor by remember { mutableStateOf(saveManager.getSelectedColor()) }
    
    val purchasedThemes = remember { mutableStateListOf<String>().apply { addAll(saveManager.getPurchasedThemes()) } }
    var selectedTheme by remember { mutableStateOf(saveManager.getSelectedTheme()) }

    val purchasedModels = remember { mutableStateListOf<String>().apply { addAll(saveManager.getPurchasedModels()) } }
    var selectedModel by remember { mutableStateOf(saveManager.getSelectedModel()) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingPurchase by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingThemePurchase by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingModelPurchase by remember { mutableStateOf<CarModelItem?>(null) }
    
    val availableColors = listOf(
        "#00E5FF" to 0,      // Cyan (Default)
        "#FF4081" to 200,    // Pink
        "#7C4DFF" to 200,    // Purple
        "#FFEA00" to 500,    // Yellow
        "#00C853" to 500,    // Green
        "#FF3D00" to 1000,   // Red
        "#FFFFFF" to 2000    // White (Premium)
    )

    val availableModels = listOf(
        CarModelItem("SPEED_RACER", "Speed Racer", 0, "🏎️", "Streamlined aerodynamic racer with twin exhausts"),
        CarModelItem("CYBER_TRUCK", "Cyber Truck", 800, "🚚", "Armor plated with neon laser lightbars"),
        CarModelItem("MUSCLE_BEAST", "Muscle Beast", 1500, "🔥", "Raw V8 muscle car with racing stripe & fat exhausts"),
        CarModelItem("SUPER_BOLT", "Super Bolt", 3000, "⚡", "High-downforce supercar with carbon rear wing")
    )

    val availableThemes = listOf(
        "VIBRANT_CITY" to "Vibrant City",
        "CYBERPUNK_NIGHT" to "Cyberpunk Night",
        "DESERT_OUTRUN" to "Desert Outrun",
        "MIDNIGHT_FOREST" to "Midnight Forest"
    )
    val themePrices = mapOf(
        "VIBRANT_CITY" to 0,
        "CYBERPUNK_NIGHT" to 1000,
        "DESERT_OUTRUN" to 2500,
        "MIDNIGHT_FOREST" to 5000
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("GARAGE & SHOP", color = Color.White, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = {
                        AudioManager.playSfx(context, "button")
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to menu",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Text("💰 $coins", color = Color(0xFFFFD600), modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                
                // 1. VEHICLE MODELS SECTION
                Text("VEHICLE MODELS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    availableModels.forEach { item ->
                        val isPurchased = purchasedModels.contains(item.id)
                        val isSelected = selectedModel == item.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AudioManager.playSfx(context, "button")
                                    if (isPurchased) {
                                        saveManager.saveSelectedModel(item.id)
                                        selectedModel = item.id
                                    } else if (coins >= item.price) {
                                        pendingModelPurchase = item
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Not enough coins for this vehicle! 🔒")
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else null,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(item.icon, fontSize = 26.sp)
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                                            if (!isPurchased) {
                                                Text("🔒", fontSize = 14.sp)
                                            }
                                        }
                                        Text(item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                        if (!isPurchased) {
                                            Text("Price: ${item.price} 💰", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Text("EQUIPPED", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                } else if (isPurchased) {
                                    Text("SELECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Button(
                                        onClick = {
                                            AudioManager.playSfx(context, "button")
                                            if (coins >= item.price) {
                                                pendingModelPurchase = item
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Not enough coins! 🔒")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("🔒 UNLOCK", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 2. PLAYER COLORS SECTION
                Text("PAINT COLORS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(14.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(85.dp),
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(availableColors) { (hex, price) ->
                        val isPurchased = purchasedColors.contains(hex)
                        val isSelected = selectedColor == hex
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        AudioManager.playSfx(context, "button")
                                        if (isPurchased) {
                                            saveManager.saveSelectedColor(hex)
                                            selectedColor = hex
                                        } else if (coins >= price) {
                                            pendingPurchase = hex to price
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Not enough coins! 🔒")
                                            }
                                        }
                                    },
                                shape = CircleShape,
                                color = Color(android.graphics.Color.parseColor(hex)),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(4.dp, Color.White) else null
                            ) {
                                if (!isPurchased) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🔒$price", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                } else if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✔️", fontSize = 22.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isSelected) "Active" else if (isPurchased) "Select" else "🔒 Locked", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))

                // 3. ENVIRONMENT THEMES SECTION
                Text("ENVIRONMENT THEMES", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    availableThemes.forEach { (id, name) ->
                        val price = themePrices[id] ?: 0
                        val isPurchased = purchasedThemes.contains(id)
                        val isSelected = selectedTheme == id

                        val onThemeClick = {
                            AudioManager.playSfx(context, "button")
                            if (isPurchased) {
                                saveManager.saveSelectedTheme(id)
                                selectedTheme = id
                            } else if (coins >= price) {
                                pendingThemePurchase = id to price
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Not enough coins for this theme! 🔒")
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeClick() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.35f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        if (!isPurchased) {
                                            Text("🔒", fontSize = 13.sp)
                                        }
                                    }
                                    Text(if (isPurchased) "Unlocked" else "Price: $price 💰", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                }
                                if (isSelected) {
                                    Text("ACTIVE", color = Color(0xFF76FF03), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                } else if (isPurchased) {
                                    Text("SELECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Button(
                                        onClick = { onThemeClick() }, 
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("🔒 UNLOCK", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation Dialogs
    pendingModelPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingModelPurchase = null },
            title = { Text("Unlock ${item.name} 🔒") },
            text = { Text("Do you want to unlock the ${item.name} for ${item.price} coins?") },
            confirmButton = {
                Button(onClick = {
                    AudioManager.playSfx(context, "powerup")
                    saveManager.addCoins(-item.price)
                    saveManager.savePurchasedModel(item.id)
                    saveManager.saveSelectedModel(item.id)
                    purchasedModels.add(item.id)
                    selectedModel = item.id
                    pendingModelPurchase = null
                }) { Text("UNLOCK 🔓") }
            },
            dismissButton = {
                TextButton(onClick = { pendingModelPurchase = null }) { Text("CANCEL") }
            }
        )
    }

    pendingPurchase?.let { (hex, price) ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text("Confirm Paint Color 🔒") },
            text = { Text("Do you want to unlock this color for $price coins?") },
            confirmButton = {
                Button(onClick = {
                    AudioManager.playSfx(context, "powerup")
                    saveManager.addCoins(-price)
                    saveManager.savePurchasedColor(hex)
                    saveManager.saveSelectedColor(hex)
                    purchasedColors.add(hex)
                    selectedColor = hex
                    pendingPurchase = null
                }) { Text("UNLOCK 🔓") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) { Text("CANCEL") }
            }
        )
    }

    pendingThemePurchase?.let { (id, price) ->
        val name = availableThemes.find { it.first == id }?.second ?: id
        AlertDialog(
            onDismissRequest = { pendingThemePurchase = null },
            title = { Text("Unlock Theme 🔒") },
            text = { Text("Do you want to unlock the $name theme for $price coins?") },
            confirmButton = {
                Button(onClick = {
                    AudioManager.playSfx(context, "powerup")
                    saveManager.addCoins(-price)
                    saveManager.savePurchasedTheme(id)
                    saveManager.saveSelectedTheme(id)
                    purchasedThemes.add(id)
                    selectedTheme = id
                    pendingThemePurchase = null
                }) { Text("UNLOCK 🔓") }
            },
            dismissButton = {
                TextButton(onClick = { pendingThemePurchase = null }) { Text("CANCEL") }
            }
        )
    }
}
