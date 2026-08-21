package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.MainActivity
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PetMenuDialog(
    pet: Pet,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onBuyBackground: (String, Int) -> Unit,
    onEquipBackground: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit,
    onBathPet: () -> Unit,
    onPlayBallPet: (Int, Int) -> Unit,
    onPlayMinigame: (String, Int, Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var shopCategory by remember { mutableStateOf("accessories") } // "accessories" o "backgrounds"
    var previewAccessory by remember { mutableStateOf(pet.equippedAccessory) }
    var previewBackground by remember { mutableStateOf(pet.equippedBackground) }

    LaunchedEffect(pet.equippedAccessory, pet.equippedBackground) {
        previewAccessory = pet.equippedAccessory
        previewBackground = pet.equippedBackground
    }

    var newName by remember { mutableStateOf(pet.name) }
    var showGameSelector by remember { mutableStateOf(false) }
    var showMemoryGame by remember { mutableStateOf(false) }
    var showSnakeGame by remember { mutableStateOf(false) }
    var showFlappyGame by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()) }
    val playedBallToday = pet.lastBallDate == today
    val playedMemoryToday = pet.lastMemoryDate == today
    val playedSnakeToday = pet.lastSnakeDate == today
    val playedFlappyToday = pet.lastFlappyDate == today
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var accumulatedTaps by remember { mutableStateOf(0) }
    var tapDebounceJob by remember { mutableStateOf<Job?>(null) }
    
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val accentColor = Color(0xFFFF4081)

    // Estados de animación interactiva de la habitación
    var isPlayingBall by remember { mutableStateOf(false) }
    var isWashing by remember { mutableStateOf(false) }
    var foodAnimationType by remember { mutableStateOf<String?>(null) }
    var showLoveHeart by remember { mutableStateOf(false) }
    val ballY = remember { Animatable(0f) }
    val ballX = remember { Animatable(80f) }
    val ballRotation = remember { Animatable(0f) }
    val bubbleOffsetY = remember { Animatable(200f) }
    val heartY = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val foodY = remember { Animatable(0f) }
    val foodAlpha = remember { Animatable(0f) }
    val catTranslationX = remember { Animatable(0f) }
    val catTranslationY = remember { Animatable(0f) }

    // Infinite transitions for Dialog mascot animation
    val dialogInfiniteTransition = rememberInfiniteTransition(label = "petDialogTransition")
    val dBobbingOffset by dialogInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dBobbing"
    )
    val dBreathingScale by dialogInfiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dBreathing"
    )
    val dWiggleRotation by dialogInfiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dWiggle"
    )

    var dIsClicked by remember { mutableStateOf(false) }
    val dClickScale by animateFloatAsState(
        targetValue = if (dIsClicked) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { dIsClicked = false }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Encabezado principal estilo Retro con botón Cerrar [X]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👾 MASCOTA VIRTUAL: ${pet.name.uppercase()} 👾",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )

                    // Botón pixel-art de cerrado rápido [X]
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFE53935), shape = RectangleShape)
                            .border(2.dp, borderColor)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Header con 4 pestañas: INFO, ESTILO, COMIDA, AJUSTES
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabItem("INFO", selectedTab == 0, isDark, borderColor) { selectedTab = 0 }
                    TabItem("ESTILO 👑", selectedTab == 1, isDark, borderColor) { selectedTab = 1 }
                    TabItem("COMIDA 🐟", selectedTab == 3, isDark, borderColor) { selectedTab = 3 }
                    TabItem("⚙️", selectedTab == 4, isDark, borderColor) { selectedTab = 4 }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Pestaña de Información
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🏡 LA HABITACIÓN DE THOR (INTERACTIVA 2D TAMAGOTCHI)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(340.dp)
                                .border(3.dp, borderColor)
                                .background(
                                    if (pet.isSleeping) Color(0xFF0F0F3D)
                                    else Color(0xFF8D6E63)
                                )
                                .clickable {
                                    if (pet.isSleeping) {
                                        android.widget.Toast.makeText(context, "💤 ¡Thor está durmiendo profundamente!", android.widget.Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    
                                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                                    val currentTapsToday = if (pet.lastTapDate == todayStr) pet.dailyTapCount else 0
                                    val limit = 30
                                    
                                    if (currentTapsToday + accumulatedTaps >= limit) {
                                        android.widget.Toast.makeText(context, "¡Thor ya recibió suficiente cariño por hoy! 💖 (Límite: $limit/día)", android.widget.Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    scope.launch {
                                        showLoveHeart = true
                                        heartY.snapTo(20f)
                                        heartAlpha.snapTo(1f)
                                        dIsClicked = true
                                        
                                        launch {
                                            heartY.animateTo(-90f, animationSpec = tween(1200, easing = EaseOutQuad))
                                        }
                                        launch {
                                            heartAlpha.animateTo(0f, animationSpec = tween(1200, easing = EaseOutQuad))
                                        }
                                    }

                                    // Incrementar contador local acumulado
                                    accumulatedTaps++

                                    // Programar la subida con debounce
                                    tapDebounceJob?.cancel()
                                    tapDebounceJob = scope.launch {
                                        delay(1500)
                                        val totalPoints = accumulatedTaps
                                        accumulatedTaps = 0
                                        if (totalPoints > 0) {
                                            onRewardPet(totalPoints, 0)
                                        }
                                    }
                                }
                        ) {
                            // Fondo Pixel-Art Dinámico (Día / Noche)
                            val roomBgRes = when (pet.equippedBackground) {
                                "jungle" -> if (pet.isSleeping) R.drawable.bg_thor_jungle_night else R.drawable.bg_thor_jungle_day
                                "space" -> if (pet.isSleeping) R.drawable.bg_thor_space_night else R.drawable.bg_thor_space_day
                                "beach" -> if (pet.isSleeping) R.drawable.bg_thor_beach_night else R.drawable.bg_thor_beach_day
                                else -> if (pet.isSleeping) R.drawable.bg_thor_room_night else R.drawable.bg_thor_room_day
                            }
                            Image(
                                painter = painterResource(id = roomBgRes),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Imagen de la mascota con estados de imagen pixel-art reales
                            val dThorImageRes = when {
                                isWashing -> R.drawable.ic_thor_bath
                                isPlayingBall -> R.drawable.ic_thor_play
                                pet.isSleeping -> R.drawable.ic_thor_sleep
                                else -> when (pet.equippedAccessory) {
                                    Pet.ACC_COLLAR -> R.drawable.ic_thor_collar
                                    Pet.ACC_MUSTACHE -> R.drawable.ic_thor_mustache
                                    Pet.ACC_BALLOON -> R.drawable.ic_thor_balloon
                                    Pet.ACC_BOW -> R.drawable.ic_thor_bow
                                    Pet.ACC_HAT -> R.drawable.ic_thor_hat
                                    Pet.ACC_BANDANA -> R.drawable.ic_thor_bandana
                                    Pet.ACC_GLASSES -> R.drawable.ic_thor_glasses
                                    Pet.ACC_CROWN -> R.drawable.ic_thor_crown
                                    Pet.ACC_BANANA -> R.drawable.ic_thor_banana
                                    Pet.ACC_SOCKS -> R.drawable.ic_thor_socks
                                    else -> R.drawable.ic_thor_base_trans
                                }
                            }

                            // Render de la mascota escalada
                            val thorSize = when {
                                pet.isSleeping -> 195.dp
                                isWashing -> 190.dp
                                isPlayingBall -> 175.dp
                                else -> 165.dp
                            }
                            val thorOffsetY = when {
                                pet.isSleeping -> (-15).dp
                                isWashing -> (-8).dp
                                isPlayingBall -> (-15).dp
                                else -> (-22).dp
                            }
                            val thorOffsetX = when {
                                pet.isSleeping -> (-8).dp
                                else -> 0.dp
                            }

                            Box(
                                modifier = Modifier
                                    .size(thorSize)
                                    .align(Alignment.BottomCenter)
                                    .offset(x = thorOffsetX, y = thorOffsetY)
                            ) {
                                Image(
                                    painter = painterResource(id = dThorImageRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = if (pet.isSleeping || isWashing) dBreathingScale else dBreathingScale * dClickScale,
                                            scaleY = if (pet.isSleeping || isWashing) dBreathingScale else dBreathingScale * dClickScale,
                                            translationX = catTranslationX.value,
                                            translationY = if (pet.isSleeping || isWashing) catTranslationY.value else dBobbingOffset + catTranslationY.value,
                                            rotationZ = if (pet.isSleeping || isWashing) 0f else dWiggleRotation
                                        )
                                )

                                if (pet.isSleeping) {
                                    val zzzInfinite = rememberInfiniteTransition(label = "zzz")
                                    val zzzOffset by zzzInfinite.animateFloat(
                                        initialValue = 0f,
                                        targetValue = -30f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = EaseOutQuad),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "zzz"
                                    )
                                    val zzzAlpha by zzzInfinite.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = EaseOutQuad),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "zzzAlpha"
                                    )
                                    Text(
                                        text = "Zzz...",
                                        fontFamily = Vt323,
                                        fontSize = 20.sp,
                                        color = Color.White.copy(alpha = zzzAlpha),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(y = zzzOffset.dp, x = 10.dp)
                                    )
                                }
                            }

                            // Capa de "Luz Apagada" cuando duerme
                            if (pet.isSleeping) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x9A0A0E29))
                                 )
                            }

                            // Corazón flotante
                            if (showLoveHeart) {
                                Text(
                                    text = "❤️ +1",
                                    fontFamily = Vt323,
                                    fontSize = 28.sp,
                                    color = Color(0xFFFF4081),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = heartY.value.dp)
                                        .graphicsLayer(alpha = heartAlpha.value)
                                )
                            }

                            // Pelota rebotando
                            if (isPlayingBall) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_toy_ball),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(35.dp)
                                        .offset(x = ballX.value.dp, y = ballY.value.dp)
                                        .graphicsLayer(rotationZ = ballRotation.value)
                                )
                            }

                            // Burbujas de Baño
                            if (isWashing) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val bubbles = listOf("🫧", "🫧", "🫧", "🫧")
                                    bubbles.forEachIndexed { idx, bubble ->
                                        val offsetFactor = idx * 45
                                        Text(
                                            text = bubble,
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .offset(x = (20 + offsetFactor).dp, y = (bubbleOffsetY.value - (idx * 20)).dp)
                                        )
                                    }
                                }
                            }

                            // Comida cayendo
                            if (foodAnimationType != null) {
                                val foodEmoji = when (foodAnimationType) {
                                    "cookie" -> "🐟"
                                    "milk" -> "🥛"
                                    "catnip" -> "🌿"
                                    "feast" -> "🍣"
                                    else -> "🍖"
                                }
                                Text(
                                    text = foodEmoji,
                                    fontSize = 32.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = foodY.value.dp)
                                        .graphicsLayer(alpha = foodAlpha.value)
                                )
                            }
                        }
                        
                        // Panel de Mini Actividades
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Lanzar Pelota
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .graphicsLayer(alpha = if (pet.isSleeping) 0.5f else 1.0f)
                                    .clickable {
                                        if (pet.isSleeping) {
                                            android.widget.Toast.makeText(context, "💤 ¡Thor está durmiendo profundamente!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (playedBallToday) {
                                            android.widget.Toast.makeText(context, "¡Ya jugaste con la pelota hoy! ⚾", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isPlayingBall = true
                                                ballX.snapTo(-60f)
                                                ballY.snapTo(-80f)
                                                ballRotation.snapTo(0f)
                                                catTranslationX.snapTo(0f)
                                                catTranslationY.snapTo(0f)
                                                
                                                launch {
                                                    delay(200)
                                                    catTranslationX.animateTo(40f, animationSpec = tween(300, easing = EaseOutQuad))
                                                    catTranslationY.animateTo(-65f, animationSpec = tween(200, easing = EaseOutQuad))
                                                    catTranslationY.animateTo(0f, animationSpec = tween(200, easing = EaseInQuad))
                                                    delay(300)
                                                    catTranslationX.animateTo(0f, animationSpec = tween(400, easing = EaseInOutQuad))
                                                }
                                                
                                                launch { ballRotation.animateTo(360f, animationSpec = tween(500, easing = LinearEasing)) }
                                                launch { ballX.animateTo(30f, animationSpec = tween(500, easing = EaseOutQuad)) }
                                                ballY.animateTo(190f, animationSpec = tween(500, easing = EaseInQuad))
                                                
                                                launch { ballRotation.animateTo(720f, animationSpec = tween(400, easing = LinearEasing)) }
                                                launch { ballX.animateTo(90f, animationSpec = tween(400, easing = EaseOutQuad)) }
                                                ballY.animateTo(110f, animationSpec = tween(200, easing = EaseOutQuad))
                                                ballY.animateTo(190f, animationSpec = tween(200, easing = EaseInQuad))
                                                
                                                launch { ballRotation.animateTo(1080f, animationSpec = tween(600, easing = LinearEasing)) }
                                                launch { ballX.animateTo(240f, animationSpec = tween(600, easing = EaseOutQuad)) }
                                                ballY.animateTo(190f, animationSpec = tween(600, easing = LinearEasing))
                                                
                                                isPlayingBall = false
                                                onPlayBallPet(10, 20)
                                            }
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(38.dp).offset(y = 4.dp).background(borderColor))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .border(2.dp, borderColor)
                                        .background(if (pet.isSleeping || playedBallToday) Color.Gray else Color(0xFFE2725B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (playedBallToday) "⚾ PELOTA (1/1)" else "⚾ PELOTA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                                }
                            }

                            // Botón Bañar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .graphicsLayer(alpha = if (pet.isSleeping) 0.5f else 1.0f)
                                    .clickable {
                                        if (pet.isSleeping) {
                                            android.widget.Toast.makeText(context, "💤 ¡Thor está durmiendo!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (pet.cleanliness >= 80) {
                                            android.widget.Toast.makeText(context, "¡Thor todavía está limpio! 🫧 (Limpieza: ${pet.cleanliness}%)", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isWashing = true
                                                bubbleOffsetY.snapTo(120f)
                                                onBathPet()
                                                dIsClicked = true
                                                bubbleOffsetY.animateTo(-60f, animationSpec = tween(1800, easing = LinearEasing))
                                                isWashing = false
                                            }
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(38.dp).offset(y = 4.dp).background(borderColor))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .border(2.dp, borderColor)
                                        .background(if (pet.isSleeping || pet.cleanliness >= 80) Color.Gray else Color(0xFF0EA5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧼 BAÑAR", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Panel de estadísticas compacto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFDF0))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CompactStat("Nivel", pet.level.toString(), contentColor)
                                    CompactStat("Racha 🔥", "${pet.streakDays}d", accentColor)
                                    CompactStat("Amor ❤️", (pet.lovePoints + accumulatedTaps).toString(), Color(0xFFFF4081))
                                    CompactStat("EXP ⭐", "${pet.experience}/100", Color(0xFF2196F3))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CompactStat("Felicidad 😊", "${pet.happiness}%", Color(0xFF4CAF50))
                                    CompactStat("Hambre 🍖", "${pet.hunger}%", Color(0xFFFF9800))
                                    CompactStat("Limpieza 🧼", "${pet.cleanliness}%", Color(0xFF0EA5E9))
                                    CompactStat("Sueño 💤", "${pet.sleepPercent}%", Color(0xFF9C27B0))
                                }
                            }
                        }
                        
                        if (pet.isSleeping) {
                            val remainingMinutes = (100 - pet.sleepPercent) * 4
                            val hours = remainingMinutes / 60
                            val minutes = remainingMinutes % 60
                            val sleepTimeStr = if (pet.sleepPercent >= 100) {
                                "¡Totalmente descansado!"
                            } else {
                                "Tiempo para despertar: ${hours}h ${minutes}m"
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sleepTimeStr,
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = Color(0xFF9C27B0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val timeUntilDecayDialog = rememberTimeUntilDecay(pet.lastInteraction, pet.happiness)
                        Text(
                            text = timeUntilDecayDialog,
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = if (pet.happiness <= 0) Color(0xFFFF4081) else contentColor.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { 
                                if (pet.isSleeping) {
                                    android.widget.Toast.makeText(context, "¡Thor está durmiendo profundamente! 💤 Despiértalo primero para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                } else if (pet.status == Pet.STATUS_HUNGRY) {
                                    android.widget.Toast.makeText(context, "¡Thor tiene demasiada hambre! 🍖 Aliméntalo en la pestaña COMIDA para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    showGameSelector = true 
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pet.isSleeping || pet.status == Pet.STATUS_HUNGRY) Color.Gray else Color(0xFF4CAF50)
                            ),
                            shape = RectangleShape
                        ) {
                            Text("🎮 JUGAR MINIJUEGOS", fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sleepButtonColor = if (pet.isSleeping) Color(0xFFFFA000) else Color(0xFF673AB7)
                        val sleepButtonText = if (pet.isSleeping) "☀️ Despertar" else "🌙 Poner a dormir"
                        Button(
                            onClick = { onToggleSleep() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = sleepButtonColor),
                            shape = RectangleShape
                        ) {
                            Text(sleepButtonText, fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else if (selectedTab == 1) {
                    // Pestaña de Tienda / Accesorios y Fondos (ESTILO)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Puntos disponibles: ${pet.lovePoints + accumulatedTaps} ❤️",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Vista de Previsualización
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(bottom = 12.dp)
                                .border(2.dp, borderColor)
                        ) {
                            val previewBgRes = when (previewBackground) {
                                "jungle" -> if (pet.isSleeping) R.drawable.bg_thor_jungle_night else R.drawable.bg_thor_jungle_day
                                "space" -> if (pet.isSleeping) R.drawable.bg_thor_space_night else R.drawable.bg_thor_space_day
                                "beach" -> if (pet.isSleeping) R.drawable.bg_thor_beach_night else R.drawable.bg_thor_beach_day
                                else -> if (pet.isSleeping) R.drawable.bg_thor_room_night else R.drawable.bg_thor_room_day
                            }
                            Image(
                                painter = painterResource(id = previewBgRes),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            val previewMascotRes = when {
                                pet.isSleeping -> R.drawable.ic_thor_sleep
                                else -> when (previewAccessory) {
                                    Pet.ACC_COLLAR -> R.drawable.ic_thor_collar
                                    Pet.ACC_MUSTACHE -> R.drawable.ic_thor_mustache
                                    Pet.ACC_BALLOON -> R.drawable.ic_thor_balloon
                                    Pet.ACC_BOW -> R.drawable.ic_thor_bow
                                    Pet.ACC_HAT -> R.drawable.ic_thor_hat
                                    Pet.ACC_BANDANA -> R.drawable.ic_thor_bandana
                                    Pet.ACC_GLASSES -> R.drawable.ic_thor_glasses
                                    Pet.ACC_CROWN -> R.drawable.ic_thor_crown
                                    Pet.ACC_BANANA -> R.drawable.ic_thor_banana
                                    Pet.ACC_SOCKS -> R.drawable.ic_thor_socks
                                    else -> R.drawable.ic_thor_base_trans
                                }
                            }
                            Image(
                                painter = painterResource(id = previewMascotRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )

                            if (pet.isSleeping) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x9A0A0E29))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(accentColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PREVISUALIZACIÓN",
                                    fontFamily = Vt323,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Selector de Categoría (Accesorios / Fondos)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.dp, borderColor),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (shopCategory == "accessories") accentColor else Color.Transparent)
                                    .clickable { shopCategory = "accessories" }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "ACCESORIOS 👑",
                                    fontFamily = Vt323,
                                    fontSize = 15.sp,
                                    color = if (shopCategory == "accessories") Color.White else contentColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(borderColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (shopCategory == "backgrounds") accentColor else Color.Transparent)
                                    .clickable { shopCategory = "backgrounds" }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "FONDOS 🖼️",
                                    fontFamily = Vt323,
                                    fontSize = 15.sp,
                                    color = if (shopCategory == "backgrounds") Color.White else contentColor
                                )
                            }
                        }

                        if (shopCategory == "accessories") {
                            val items = listOf(
                                Triple(Pet.ACC_COLLAR, "Collar Cascabel 🔔", 10),
                                Triple(Pet.ACC_SOCKS, "Calcetas y Botitas 🧦🥾", 15),
                                Triple(Pet.ACC_MUSTACHE, "Bigote Retro 🥸", 30),
                                Triple(Pet.ACC_BALLOON, "Globo Corazón 🎈", 60),
                                Triple(Pet.ACC_BOW, "Lazo Rosa 🎀", 80),
                                Triple(Pet.ACC_HAT, "Gorrito Pixel 🎩", 100),
                                Triple(Pet.ACC_BANDANA, "Pañuelo Pirata 🏴‍☠️", 120),
                                Triple(Pet.ACC_GLASSES, "Lentes Cool 🕶️", 150),
                                Triple(Pet.ACC_BANANA, "Plátano Nano 🍌", 200),
                                Triple(Pet.ACC_CROWN, "Corona Real 👑", 500)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(items) { (id, name, cost) ->
                                    val isUnlocked = pet.unlockedAccessories.contains(id)
                                    val isEquipped = pet.equippedAccessory == id
                                    val isPreviewed = previewAccessory == id

                                    AccessoryRow(
                                        name = name,
                                        cost = cost,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isEquipped,
                                        isPreviewed = isPreviewed,
                                        isDark = isDark,
                                        borderColor = borderColor,
                                        onPreview = {
                                            previewAccessory = if (previewAccessory == id) Pet.ACC_NONE else id
                                        },
                                        onAction = {
                                            if (isUnlocked) onEquipAccessory(if (isEquipped) Pet.ACC_NONE else id)
                                            else onBuyAccessory(id, cost)
                                        }
                                    )
                                }
                            }
                        } else {
                            val backgrounds = listOf(
                                Triple("default", "Habitación Clásica 🏠", 0),
                                Triple("jungle", "Selva Tropical 🌴", 50),
                                Triple("space", "Nave Espacial 🚀", 100),
                                Triple("beach", "Playa Paradise 🏖️", 150)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(backgrounds) { (id, name, cost) ->
                                    val isUnlocked = pet.unlockedBackgrounds.contains(id)
                                    val isEquipped = pet.equippedBackground == id
                                    val isPreviewed = previewBackground == id

                                    AccessoryRow(
                                        name = name,
                                        cost = cost,
                                        isUnlocked = isUnlocked || cost == 0,
                                        isEquipped = isEquipped,
                                        isPreviewed = isPreviewed,
                                        isDark = isDark,
                                        borderColor = borderColor,
                                        onPreview = {
                                            previewBackground = id
                                        },
                                        onAction = {
                                            if (isUnlocked || cost == 0) onEquipBackground(id)
                                            else onBuyBackground(id, cost)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 3) {
                    // Pestaña de Tienda de Alimentos / Comida
                    val foods = listOf(
                        Triple("cookie", "Galleta Pescado 🐟", Pair(5, 15)),
                        Triple("milk", "Leche Tibia 🥛", Pair(10, 30)),
                        Triple("catnip", "Catnip Relajante 🌿", Pair(15, 50)),
                        Triple("feast", "Banquete Gourmet 🍣", Pair(25, 80))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Puntos disponibles: ${pet.lovePoints} ❤️",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            "Alimenta a ${pet.name} para subir su felicidad e hidratación:",
                            fontFamily = Vt323,
                            color = contentColor,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(foods) { (id, name, pair) ->
                                val (cost, gain) = pair
                                FoodRow(
                                    name = name,
                                    cost = cost,
                                    gain = gain,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    onFeed = {
                                        if (pet.isSleeping) {
                                            android.widget.Toast.makeText(context, "¡Thor está durmiendo! 💤 No puede comer ahora.", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            scope.launch {
                                                foodAnimationType = id
                                                foodY.snapTo(-30f)
                                                foodAlpha.snapTo(1f)
                                                selectedTab = 0
                                                
                                                foodY.animateTo(190f, animationSpec = tween(900, easing = EaseInQuad))
                                                dIsClicked = true
                                                foodAlpha.animateTo(0f, animationSpec = tween(300))
                                                foodAnimationType = null
                                                
                                                onFeedPet(id, cost, gain)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 2) {
                    // Pestaña de Guía explicativa
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { selectedTab = 4 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                            shape = RectangleShape
                        ) {
                            Text("← Volver a Ajustes", fontFamily = Vt323, color = Color.White, fontSize = 16.sp)
                        }

                        Text(
                            "¿Cómo cuidar a ${pet.name}? 🐾",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Card Hambre y Nutrición
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🍖 HAMBRE Y NUTRICIÓN", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• El nivel de hambre aumenta gradualmente un 4% por hora sin interactuar.\n• Si el nivel de hambre sube por encima del 70%, ¡Thor tendrá demasiada hambre y entrará en estado hambriento! 😢\n• Bloqueo de Minijuegos: No podrás jugar con él si tiene hambre.\n• Compra galletas de pescado 🐟, leche 🥛 o banquetes 🍣 en la pestaña COMIDA para alimentarlo y subir su felicidad.", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Sueño y Descanso
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("💤 SUEÑO Y DESCANSO", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Usa el botón [Poner a dormir 🌙] para apagar la luz de su cuarto y dejarlo descansar.\n• Mientras duerme, Thor soñará plácidamente y no podrá realizar actividades.\n• Restricciones: No puedes alimentarlo ni jugar minijuegos con él mientras esté dormido.\n• ¡Asegúrate de despertarlo [☀️ Despertar] cuando estés listo para interactuar con él!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Felicidad
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("😊 FELICIDAD", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Cada carta enviada le da +10% de felicidad.\n• Si no envías nada en 24h, su felicidad cae un 20% al día.\n• ¡Visita la sección COMIDA 🐟 para restaurar felicidad al instante!\n• Si su felicidad baja de 40%, se pondrá triste 😢.", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Puntos de Amor
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("❤️ PUNTOS DE AMOR", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Cartas: Ganas +5 puntos por cada mensaje enviado.\n• Racha 🔥: ¡Interconecta todos los días! Cada día de racha sumado otorga un bonus de Racha * 2 puntos.\n• Nivel ✨: Cada carta te da +10 EXP. ¡Al llegar a 100 EXP subes de nivel y recibes +50 puntos de amor!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Tienda
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("👑 ACCESORIOS Y TIENDA", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Canjea tus puntos en la ROPA por accesorios.\n• Los accesorios equipados se superpondrán a tu mascota en tiempo real. ¡Haz que Thor luzca fabuloso!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else if (selectedTab == 4) {
                    val sharedPrefsNotif = remember { context.getSharedPreferences("pet_notif_prefs", Context.MODE_PRIVATE) }
                    var notificationsEnabled by remember { mutableStateOf(sharedPrefsNotif.getBoolean("notifications_enabled", true)) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚙️ AJUSTES DE ${pet.name.uppercase()} ⚙️",
                            fontFamily = Vt323,
                            color = contentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. NOMBRE
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Nombre de tu compañero:",
                                fontFamily = Vt323,
                                color = contentColor,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 18.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = contentColor,
                                        unfocusedTextColor = contentColor,
                                        focusedBorderColor = borderColor,
                                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
                                    )
                                )
                                Button(
                                    onClick = { 
                                        onUpdateName(newName)
                                        (context as? MainActivity)?.showStyledPixelToast("¡Nombre guardado! ❤️")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                                    shape = RectangleShape,
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Guardar", fontFamily = Vt323, color = Color.White, fontSize = 16.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

                        // 2. ALERTAS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notificaciones de Cuidado", fontFamily = Vt323, fontSize = 18.sp, color = contentColor)
                                Text(
                                    "Alertas de hambre, sueño y baño",
                                    fontFamily = Vt323,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.LightGray else Color.DarkGray
                                )
                            }
                            
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    notificationsEnabled = isChecked
                                    sharedPrefsNotif.edit().putBoolean("notifications_enabled", isChecked).apply()
                                    val statusText = if (isChecked) "activadas" else "desactivadas"
                                    (context as? MainActivity)?.showStyledPixelToast("Notificaciones $statusText 🔔")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                        }

                        HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

                        // 3. SINCRONIZACIÓN
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Base de Datos: Firebase Cloud Firestore\nEstado: Conectado a la Nube ☁\nCompañero: ${pet.name}",
                                fontFamily = Vt323,
                                color = contentColor,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    (context as? MainActivity)?.showStyledPixelToast("Sincronizando... ☁️")
                                    scope.launch {
                                        delay(800)
                                        (context as? MainActivity)?.showStyledPixelToast("¡Sincronización forzada con éxito! ☁️✨")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                                shape = RectangleShape
                            ) {
                                Text("Forzar Sincronización ☁️", fontFamily = Vt323, color = Color.White, fontSize = 16.sp)
                            }
                        }

                        HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

                        // 4. GUÍA
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Guía de Cuidado de Mascota:",
                                fontFamily = Vt323,
                                color = contentColor,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { selectedTab = 2 },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                                shape = RectangleShape
                            ) {
                                Text("📖 Ver Guía de Cuidado", fontFamily = Vt323, color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    if (showGameSelector) {
        MinigamesSelectorDialog(
            isDark = isDark,
            playedMemoryToday = playedMemoryToday,
            playedSnakeToday = playedSnakeToday,
            playedFlappyToday = playedFlappyToday,
            onDismiss = { showGameSelector = false },
            onPlayMemory = {
                showGameSelector = false
                showMemoryGame = true
            },
            onPlaySnake = {
                showGameSelector = false
                showSnakeGame = true
            },
            onPlayFlappy = {
                showGameSelector = false
                showFlappyGame = true
            }
        )
    }

    if (showMemoryGame) {
        MemoryGameDialog(
            isDark = isDark,
            onDismiss = { showMemoryGame = false },
            onReward = { pts, exp -> onPlayMinigame("memory", pts, exp) }
        )
    }

    if (showSnakeGame) {
        SnakeGameDialog(
            isDark = isDark,
            onDismiss = { showSnakeGame = false },
            onReward = { pts, exp -> onPlayMinigame("snake", pts, exp) }
        )
    }

    if (showFlappyGame) {
        FlappyThorGameDialog(
            pet = pet,
            isDark = isDark,
            onDismiss = { showFlappyGame = false },
            onReward = { pts, exp -> onPlayMinigame("flappy", pts, exp) }
        )
    }
}

@Composable
fun RowScope.TabItem(text: String, isSelected: Boolean, isDark: Boolean, borderColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .background(if (isSelected) borderColor else Color.Transparent)
            .clickable { onClick() }
            .border(1.dp, borderColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = if (isSelected) Color.White else if (isDark) Color.LightGray else Color.DarkGray
        )
    }
}

@Composable
fun InfoStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Vt323, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontFamily = Vt323, fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CompactStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Vt323, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(1.dp))
        Text(value, fontFamily = Vt323, fontSize = 16.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccessoryRow(
    name: String,
    cost: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isPreviewed: Boolean,
    isDark: Boolean,
    borderColor: Color,
    onPreview: () -> Unit,
    onAction: () -> Unit
) {
    val rowBorderColor = if (isPreviewed) borderColor else borderColor.copy(alpha = 0.3f)
    val rowBg = if (isPreviewed) {
        if (isDark) Color(0xFF2C2C35) else Color(0xFFFFF9E6)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isPreviewed) 2.dp else 1.dp, rowBorderColor)
            .background(rowBg)
            .clickable { onPreview() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
                if (isPreviewed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("👁️", fontSize = 14.sp)
                }
            }
            if (!isUnlocked) {
                Text("${cost} ❤️", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
            } else if (isEquipped) {
                Text("Equipado actualmente", fontFamily = Vt323, fontSize = 13.sp, color = Color(0xFF4CAF50))
            } else {
                Text("Desbloqueado", fontFamily = Vt323, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
            }
        }

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isEquipped -> Color(0xFF4CAF50)
                    isUnlocked -> borderColor
                    else -> Color(0xFF8B4513)
                }
            ),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = when {
                    isEquipped -> "EQUIPADO"
                    isUnlocked -> "EQUIPAR"
                    else -> "COMPRAR"
                },
                fontFamily = Vt323,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FoodRow(
    name: String,
    cost: Int,
    gain: Int,
    isDark: Boolean,
    borderColor: Color,
    onFeed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
            Text("${cost} ❤️ (Felicidad +${gain}%)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
        }

        Button(
            onClick = onFeed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "ALIMENTAR",
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun MinigamesSelectorDialog(
    isDark: Boolean,
    playedMemoryToday: Boolean,
    playedSnakeToday: Boolean,
    playedFlappyToday: Boolean,
    onDismiss: () -> Unit,
    onPlayMemory: () -> Unit,
    onPlaySnake: () -> Unit,
    onPlayFlappy: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮 SELECCIONAR JUEGO 🎮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Botón Flappy Thor
                Button(
                    onClick = onPlayFlappy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_thor_balloon),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🐱 FLAPPY THOR 🪽", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedFlappyToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ (Modo Libre)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedFlappyToday) "¡Sigue jugando por diversión! (Recompensa de hoy reclamada)"
                                else "¡Vuela y atrapa corazones! ✨ Recompensa diaria disponible",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Botón Retro Memory
                Button(
                    onClick = onPlayMemory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_heart_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧠 RETRO MEMORY", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedMemoryToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ (Modo Libre)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedMemoryToday) "¡Sigue jugando por diversión! (Recompensa de hoy reclamada)"
                                else "¡Encuentra ropa pixel-art de Thor! ✨ Recompensa diaria disponible",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Botón Retro Snake
                Button(
                    onClick = onPlaySnake,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_recipe_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🐍 LA SERPIENTE", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedSnakeToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ (Modo Libre)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedSnakeToday) "¡Sigue jugando por diversión! (Recompensa de hoy reclamada)"
                                else "¡Come manzanas y bate récords! ✨ Recompensa diaria disponible",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar ❌", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PixelArrowButton(
    arrow: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    borderColor: Color
) {
    val buttonBg = if (isDark) Color(0xFF3E3E3E) else Color(0xFFFFFDD0)
    val shadowColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD7CCC8)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationOffset = if (isPressed) 3.dp else 0.dp
    val shadowOffset = if (isPressed) 0.dp else 3.dp

    Box(
        modifier = modifier
            .size(54.dp, 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (shadowOffset > 0.dp) {
                    drawRect(
                        color = shadowColor,
                        topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                }
            }
            .offset(x = translationOffset, y = translationOffset)
            .background(buttonBg)
            .border(2.dp, borderColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = arrow,
            fontFamily = Vt323,
            fontSize = 24.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsMenuButton(
    text: String,
    isDark: Boolean,
    borderColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E1E2E) else Color(0xFFF3EFE0)),
        border = BorderStroke(1.dp, borderColor),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color(0xFF4A2511))
            Text("▶", fontFamily = Vt323, fontSize = 14.sp, color = borderColor)
        }
    }
}
