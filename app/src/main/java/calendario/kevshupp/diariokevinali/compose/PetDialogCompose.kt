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
import androidx.compose.ui.text.style.TextAlign
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
    onSwitchPet: (String) -> Unit = {},
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onBuyBackground: (String, Int) -> Unit,
    onEquipBackground: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit,
    onBathPet: () -> Unit,
    onPlayBallPet: (Int, Int) -> Unit,
    onPlayMinigame: (String, Int, Int, Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var shopCategory by remember { mutableStateOf("accessories") } // "accessories" o "backgrounds"
    var previewAccessory by remember { mutableStateOf(pet.getActiveEquippedAccessory()) }
    var previewBackground by remember { mutableStateOf(pet.getActiveEquippedBackground()) }

    LaunchedEffect(pet.getActiveEquippedAccessory(), pet.getActiveEquippedBackground(), pet.petType) {
        previewAccessory = pet.getActiveEquippedAccessory()
        previewBackground = pet.getActiveEquippedBackground()
    }

    var newName by remember { mutableStateOf(pet.getActiveName()) }
    LaunchedEffect(pet.getActiveName()) {
        newName = pet.getActiveName()
    }
    var showGameSelector by remember { mutableStateOf(false) }
    var showMemoryGame by remember { mutableStateOf(false) }
    var showSnakeGame by remember { mutableStateOf(false) }
    var showFlappyGame by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()) }
    val playedBallToday = pet.getActiveLastBallDate() == today
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

    // Animaciones procedurales de paseo y picoteo para Cuky
    val cukyWalkX = remember { Animatable(0f) }
    val cukyWalkY = remember { Animatable(0f) }
    var cukyFacingDirection by remember { mutableStateOf(1f) } // 1f: derecha, -1f: izquierda
    val cukyWaddleRotation = remember { Animatable(0f) }
    val cukyStepBounce = remember { Animatable(0f) }
    val cukyPeckRotation = remember { Animatable(0f) }
    val cukyPeckOffsetY = remember { Animatable(0f) }
    var showPeckSeed by remember { mutableStateOf(false) }
    val seedAlpha = remember { Animatable(0f) }

    LaunchedEffect(selectedTab, pet.getActiveIsSleeping(), isWashing, isPlayingBall, pet.isCuky()) {
        val isCuky = pet.isCuky()
        if (!isCuky || pet.getActiveIsSleeping() || isWashing || isPlayingBall || selectedTab != 0) {
            cukyWalkX.snapTo(0f)
            cukyWalkY.snapTo(0f)
            cukyWaddleRotation.snapTo(0f)
            cukyStepBounce.snapTo(0f)
            cukyPeckRotation.snapTo(0f)
            cukyPeckOffsetY.snapTo(0f)
            showPeckSeed = false
            return@LaunchedEffect
        }

        // Bucle de comportamiento autónomo de Cuky
        while (true) {
            // 1. Descanso / Observar entorno (1.2s - 2.5s)
            delay((1200..2500).random().toLong())

            // 2. Elegir nuevo destino X cubriendo ambos extremos (-120dp a +120dp)
            // Alternar para asegurar que viaje a los extremos izquierdo y derecho
            val currentX = cukyWalkX.value
            val targetX = if (currentX > 40f) {
                // Si está a la derecha, viajar hacia el extremo izquierdo o centro-izquierda
                (-120..-20).random().toFloat()
            } else if (currentX < -40f) {
                // Si está a la izquierda, viajar hacia el extremo derecho o centro-derecha
                (20..120).random().toFloat()
            } else {
                // Si está en el centro, elegir cualquier extremo
                if (Math.random() < 0.5) (-120..-60).random().toFloat() else (60..120).random().toFloat()
            }

            val startX = cukyWalkX.value
            val deltaX = targetX - startX

            if (Math.abs(deltaX) > 10f) {
                // Orientar hacia el destino
                cukyFacingDirection = if (deltaX > 0) 1f else -1f

                // Caminar dando pasitos de gallina a paso continuo
                val steps = (Math.abs(deltaX) / 6.5f).toInt().coerceIn(4, 28)
                for (s in 1..steps) {
                    val progress = s.toFloat() / steps
                    val nextX = startX + deltaX * progress

                    // Pasito en X
                    launch {
                        cukyWalkX.animateTo(nextX, tween(120, easing = LinearEasing))
                    }
                    // Bamboleo de cuerpo de gallina (Waddle)
                    val waddleTarget = if (s % 2 == 0) 7.5f else -7.5f
                    launch {
                        cukyWaddleRotation.animateTo(waddleTarget, tween(60, easing = EaseInOutQuad))
                        cukyWaddleRotation.animateTo(0f, tween(60, easing = EaseInOutQuad))
                    }
                    // Botecito de patitas (Hop)
                    launch {
                        cukyStepBounce.animateTo(-6f, tween(60, easing = EaseOutQuad))
                        cukyStepBounce.animateTo(0f, tween(60, easing = EaseInQuad))
                    }
                    delay(125L)
                }
                cukyWaddleRotation.snapTo(0f)
                cukyStepBounce.snapTo(0f)
            }

            // 3. Picotear el suelo (65% probabilidad)
            if (Math.random() < 0.65) {
                showPeckSeed = true
                launch {
                    seedAlpha.snapTo(1f)
                    delay(600)
                    seedAlpha.animateTo(0f, tween(300))
                }

                val pecks = (2..3).random()
                for (p in 1..pecks) {
                    // Inclinar cabeza y cuerpo hacia abajo para picotear
                    launch {
                        cukyPeckRotation.animateTo(-22f * cukyFacingDirection, tween(90, easing = EaseOutQuad))
                    }
                    launch {
                        cukyPeckOffsetY.animateTo(12f, tween(90, easing = EaseOutQuad))
                    }
                    delay(110L)
                    // Volver arriba
                    launch {
                        cukyPeckRotation.animateTo(0f, tween(90, easing = EaseInQuad))
                    }
                    launch {
                        cukyPeckOffsetY.animateTo(0f, tween(90, easing = EaseInQuad))
                    }
                    delay(110L)
                }
                showPeckSeed = false
            }
        }
    }

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
                        text = "👾 MASCOTA VIRTUAL: ${pet.getActiveName().uppercase()} 👾",
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

                // Selector de Mascota Activa: 🐱 Thor vs 🐔 Cuky
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isThor = !pet.isCuky()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isThor) accentColor else if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8D7C0),
                                shape = RectangleShape
                            )
                            .border(2.dp, if (isThor) Color.White else borderColor)
                            .clickable { onSwitchPet(Pet.PET_THOR) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐱 THOR",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = if (isThor) Color.White else contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (!isThor) Color(0xFFD97706) else if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8D7C0),
                                shape = RectangleShape
                            )
                            .border(2.dp, if (!isThor) Color.White else borderColor)
                            .clickable { onSwitchPet(Pet.PET_CUKY) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐔 CUKY",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = if (!isThor) Color.White else contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Header con 5 pestañas: INFO, RANKING 🏆, ESTILO 👑, COMIDA, AJUSTES
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabItem("INFO", selectedTab == 0, isDark, borderColor) { selectedTab = 0 }
                    TabItem("RANK 🏆", selectedTab == 2, isDark, borderColor) { selectedTab = 2 }
                    TabItem("ESTILO 👑", selectedTab == 1, isDark, borderColor) { selectedTab = 1 }
                    TabItem(if (pet.isCuky()) "COMIDA 🌽" else "COMIDA 🐟", selectedTab == 3, isDark, borderColor) { selectedTab = 3 }
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
                        // 🏡 LA HABITACIÓN DE LA MASCOTA (INTERACTIVA 2D TAMAGOTCHI)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(340.dp)
                                .border(3.dp, borderColor)
                                .background(
                                    if (pet.getActiveIsSleeping()) Color(0xFF0F0F3D)
                                    else Color(0xFF8D6E63)
                                )
                                .clickable {
                                    if (pet.getActiveIsSleeping()) {
                                        android.widget.Toast.makeText(context, "💤 ¡${pet.getActiveName()} está durmiendo profundamente!", android.widget.Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    
                                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                                    val currentTapsToday = if (pet.getActiveLastTapDate() == todayStr) pet.getActiveDailyTapCount() else 0
                                    val limit = 30
                                    
                                    if (currentTapsToday + accumulatedTaps >= limit) {
                                        android.widget.Toast.makeText(context, "¡${pet.getActiveName()} ya recibió suficiente cariño por hoy! 💖 (Límite: $limit/día)", android.widget.Toast.LENGTH_SHORT).show()
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
                            val currentBg = pet.getActiveEquippedBackground()
                            val isSleeping = pet.getActiveIsSleeping()
                            val roomBgRes = when (currentBg) {
                                "coop" -> if (isSleeping) R.drawable.bg_cuky_coop_night else R.drawable.bg_cuky_coop_day
                                "farm" -> if (isSleeping) R.drawable.bg_cuky_farm_night else R.drawable.bg_cuky_farm_day
                                "jungle" -> if (isSleeping) R.drawable.bg_thor_jungle_night else R.drawable.bg_thor_jungle_day
                                "space" -> if (isSleeping) R.drawable.bg_thor_space_night else R.drawable.bg_thor_space_day
                                "beach" -> if (isSleeping) R.drawable.bg_thor_beach_night else R.drawable.bg_thor_beach_day
                                else -> if (isSleeping) R.drawable.bg_thor_room_night else R.drawable.bg_thor_room_day
                            }
                            Image(
                                painter = painterResource(id = roomBgRes),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Imagen de la mascota con estados de imagen pixel-art reales (Thor / Cuky)
                            val dThorImageRes = getPetDrawableRes(
                                pet = pet,
                                isWashing = isWashing,
                                isPlayingBall = isPlayingBall
                            )

                            // Render de la mascota escalada
                            val isCuky = pet.isCuky()
                            val thorSize = when {
                                pet.getActiveIsSleeping() -> if (isCuky) 230.dp else 195.dp
                                isWashing -> if (isCuky) 225.dp else 190.dp
                                isPlayingBall -> if (isCuky) 215.dp else 175.dp
                                else -> if (isCuky) 205.dp else 165.dp
                            }
                            val thorOffsetY = when {
                                pet.getActiveIsSleeping() -> (-15).dp
                                isWashing -> (-8).dp
                                isPlayingBall -> (-12).dp
                                else -> if (isCuky) (-12).dp else (-22).dp
                            }
                            val thorOffsetX = when {
                                pet.getActiveIsSleeping() -> (-8).dp
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
                                        .graphicsLayer {
                                            val activeSleeping = pet.getActiveIsSleeping()
                                            scaleX = if (isCuky && !activeSleeping && !isWashing && !isPlayingBall) {
                                                cukyFacingDirection * (if (dIsClicked) dBreathingScale * dClickScale else dBreathingScale)
                                            } else {
                                                if (activeSleeping || isWashing) dBreathingScale else dBreathingScale * dClickScale
                                            }
                                            scaleY = if (activeSleeping || isWashing) dBreathingScale else dBreathingScale * dClickScale
                                            translationX = if (isCuky && !activeSleeping && !isWashing && !isPlayingBall) {
                                                cukyWalkX.value
                                            } else {
                                                catTranslationX.value
                                            }
                                            translationY = if (isCuky && !activeSleeping && !isWashing && !isPlayingBall) {
                                                cukyWalkY.value + cukyStepBounce.value + cukyPeckOffsetY.value
                                            } else if (activeSleeping || isWashing) {
                                                catTranslationY.value
                                            } else {
                                                dBobbingOffset + catTranslationY.value
                                            }
                                            rotationZ = if (isCuky && !activeSleeping && !isWashing && !isPlayingBall) {
                                                cukyWaddleRotation.value + cukyPeckRotation.value
                                            } else if (activeSleeping || isWashing) {
                                                0f
                                            } else {
                                                dWiggleRotation
                                            }
                                        }
                                )

                                // Semillitas que aparecen en el suelo cuando Cuky picotea
                                if (showPeckSeed && isCuky && !pet.getActiveIsSleeping()) {
                                    Text(
                                        text = "🌾",
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(
                                                x = (cukyWalkX.value + (18f * cukyFacingDirection)).dp,
                                                y = (cukyWalkY.value + 16f).dp
                                            )
                                            .graphicsLayer(alpha = seedAlpha.value)
                                    )
                                }

                                if (pet.getActiveIsSleeping()) {
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
                            if (pet.getActiveIsSleeping()) {
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
                                    "seeds" -> "🌾"
                                    "corn" -> "🌽"
                                    "melon" -> "🍉"
                                    "worm" -> "🪱"
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
                                    .graphicsLayer(alpha = if (pet.getActiveIsSleeping()) 0.5f else 1.0f)
                                    .clickable {
                                        if (pet.getActiveIsSleeping()) {
                                            android.widget.Toast.makeText(context, "💤 ¡${pet.getActiveName()} está durmiendo profundamente!", android.widget.Toast.LENGTH_SHORT).show()
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
                                        .background(if (pet.getActiveIsSleeping() || playedBallToday) Color.Gray else Color(0xFFE2725B)),
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
                                    .graphicsLayer(alpha = if (pet.getActiveIsSleeping()) 0.5f else 1.0f)
                                    .clickable {
                                        if (pet.getActiveIsSleeping()) {
                                            android.widget.Toast.makeText(context, "💤 ¡${pet.getActiveName()} está durmiendo!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (pet.getActiveCleanliness() >= 80) {
                                            android.widget.Toast.makeText(context, "¡${pet.getActiveName()} todavía está limpio! 🫧 (Limpieza: ${pet.getActiveCleanliness()}%)", android.widget.Toast.LENGTH_SHORT).show()
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
                                        .background(if (pet.getActiveIsSleeping() || pet.getActiveCleanliness() >= 80) Color.Gray else Color(0xFF0EA5E9)),
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
                                    CompactStat("Nivel", pet.getActiveLevel().toString(), contentColor)
                                    CompactStat("Racha 🔥", "${pet.getActiveStreak()}d", accentColor)
                                    CompactStat("Amor ❤️", (pet.lovePoints + accumulatedTaps).toString(), Color(0xFFFF4081))
                                    CompactStat("EXP ⭐", "${pet.getActiveExperience()}/100", Color(0xFF2196F3))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CompactStat("Felicidad 😊", "${pet.getActiveHappiness()}%", Color(0xFF4CAF50))
                                    CompactStat("Hambre 🍖", "${pet.getActiveHunger()}%", Color(0xFFFF9800))
                                    CompactStat("Limpieza 🧼", "${pet.getActiveCleanliness()}%", Color(0xFF0EA5E9))
                                    CompactStat("Sueño 💤", "${pet.getActiveSleepPercent()}%", Color(0xFF9C27B0))
                                }
                            }
                        }
                        
                        if (pet.getActiveIsSleeping()) {
                            val remainingMinutes = (100 - pet.getActiveSleepPercent()) * 4
                            val hours = remainingMinutes / 60
                            val minutes = remainingMinutes % 60
                            val sleepTimeStr = if (pet.getActiveSleepPercent() >= 100) {
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
                        
                        val timeUntilDecayDialog = rememberTimeUntilDecay(pet.getActiveLastInteraction(), pet.getActiveHappiness())
                        Text(
                            text = timeUntilDecayDialog,
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = if (pet.getActiveHappiness() <= 0) Color(0xFFFF4081) else contentColor.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { 
                                if (pet.getActiveIsSleeping()) {
                                    android.widget.Toast.makeText(context, "¡${pet.getActiveName()} está durmiendo profundamente! 💤 Despiértalo primero para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                } else if (pet.getActiveStatus() == Pet.STATUS_HUNGRY) {
                                    android.widget.Toast.makeText(context, "¡${pet.getActiveName()} tiene demasiada hambre! 🍖 Aliméntalo en la pestaña COMIDA para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    showGameSelector = true 
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pet.getActiveIsSleeping() || pet.getActiveStatus() == Pet.STATUS_HUNGRY) Color.Gray else Color(0xFF4CAF50)
                            ),
                            shape = RectangleShape
                        ) {
                            Text("🎮 JUGAR MINIJUEGOS", fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sleepButtonColor = if (pet.getActiveIsSleeping()) Color(0xFFFFA000) else Color(0xFF673AB7)
                        val sleepButtonText = if (pet.getActiveIsSleeping()) "☀️ Despertar" else "🌙 Poner a dormir"
                        Button(
                            onClick = { onToggleSleep() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = sleepButtonColor),
                            shape = RectangleShape
                        ) {
                            Text(sleepButtonText, fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(64.dp))
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
                            val isSleeping = pet.getActiveIsSleeping()
                            val previewBgRes = when (previewBackground) {
                                "coop" -> if (isSleeping) R.drawable.bg_cuky_coop_night else R.drawable.bg_cuky_coop_day
                                "farm" -> if (isSleeping) R.drawable.bg_cuky_farm_night else R.drawable.bg_cuky_farm_day
                                "jungle" -> if (isSleeping) R.drawable.bg_thor_jungle_night else R.drawable.bg_thor_jungle_day
                                "space" -> if (isSleeping) R.drawable.bg_thor_space_night else R.drawable.bg_thor_space_day
                                "beach" -> if (isSleeping) R.drawable.bg_thor_beach_night else R.drawable.bg_thor_beach_day
                                else -> if (isSleeping) R.drawable.bg_thor_room_night else R.drawable.bg_thor_room_day
                            }
                            Image(
                                painter = painterResource(id = previewBgRes),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            val previewMascotRes = getPetDrawableRes(
                                pet = pet,
                                accessory = previewAccessory,
                                isSleeping = isSleeping
                            )
                            val previewMascotSize = if (pet.isCuky()) 110.dp else 90.dp
                            Image(
                                painter = painterResource(id = previewMascotRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(previewMascotSize)
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                            )

                            if (isSleeping) {
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
                            val activeUnlockedAccs = pet.getActiveUnlockedAccessories()
                            val activeEquippedAcc = pet.getActiveEquippedAccessory()

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(
                                    items = items,
                                    key = { it.first }
                                ) { (id, name, cost) ->
                                    val isUnlocked = activeUnlockedAccs.contains(id)
                                    val isEquipped = activeEquippedAcc == id
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
                            val activeUnlockedBgs = pet.getActiveUnlockedBackgrounds()
                            val activeEquippedBg = pet.getActiveEquippedBackground()
                            val backgrounds = if (pet.isCuky()) {
                                listOf(
                                    Triple("coop", "Gallinero Acogedor 🛖", 0),
                                    Triple("farm", "Huerta de Cultivos 🌽🌻", 50),
                                    Triple("beach", "Playa Paradise 🏖️", 100),
                                    Triple("jungle", "Selva Tropical 🌴", 120),
                                    Triple("default", "Habitación Clásica 🏠", 150),
                                    Triple("space", "Nave Espacial 🚀", 200)
                                )
                            } else {
                                listOf(
                                    Triple("default", "Habitación Clásica 🏠", 0),
                                    Triple("jungle", "Selva Tropical 🌴", 50),
                                    Triple("coop", "Gallinero Acogedor 🛖", 75),
                                    Triple("farm", "Huerta de Cultivos 🌽🌻", 75),
                                    Triple("space", "Nave Espacial 🚀", 100),
                                    Triple("beach", "Playa Paradise 🏖️", 150)
                                )
                            }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(
                                    items = backgrounds,
                                    key = { it.first }
                                ) { (id, name, cost) ->
                                    val isUnlocked = activeUnlockedBgs.contains(id)
                                    val isEquipped = activeEquippedBg == id
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
                    val foods = if (pet.isCuky()) {
                        listOf(
                            Triple("seeds", "Semillas de Amor 🌾", Pair(5, 15)),
                            Triple("corn", "Maíz Dorado 🌽", Pair(10, 30)),
                            Triple("melon", "Sandía Fresca 🍉", Pair(15, 50)),
                            Triple("worm", "Banquete de Gusano 🪱", Pair(25, 80))
                        )
                    } else {
                        listOf(
                            Triple("cookie", "Galleta Pescado 🐟", Pair(5, 15)),
                            Triple("milk", "Leche Tibia 🥛", Pair(10, 30)),
                            Triple("catnip", "Catnip Relajante 🌿", Pair(15, 50)),
                            Triple("feast", "Banquete Gourmet 🍣", Pair(25, 80))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Puntos disponibles: ${pet.lovePoints} ❤️",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            "Alimenta a ${pet.getActiveName()} para subir su felicidad e hidratación:",
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
                            items(
                                items = foods,
                                key = { it.first }
                            ) { (id, name, pair) ->
                                val (cost, gain) = pair
                                FoodRow(
                                    name = name,
                                    cost = cost,
                                    gain = gain,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    onFeed = {
                                        if (pet.getActiveIsSleeping()) {
                                            android.widget.Toast.makeText(context, "¡${pet.getActiveName()} está durmiendo! 💤 No puede comer ahora.", android.widget.Toast.LENGTH_LONG).show()
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
                    // Pestaña de Ranking de Cuidadores
                    PetCaregiverRankingContent(
                        pet = pet,
                        isDark = isDark,
                        borderColor = borderColor,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onBackToHome = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
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
                            text = "⚙️ AJUSTES DE ${pet.getActiveName().uppercase()} ⚙️",
                            fontFamily = Vt323,
                            color = contentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. NOTIFICACIONES
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notificaciones de Cuidado",
                                    fontFamily = Vt323,
                                    color = contentColor,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Recordatorios cuando ${pet.getActiveName()} tenga hambre o sueño",
                                    fontFamily = Vt323,
                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                    fontSize = 14.sp
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

                        // 2. SINCRONIZACIÓN
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Base de Datos: Firebase Cloud Firestore\nEstado: Conectado a la Nube ☁\nCompañero Activo: ${pet.getActiveName()}",
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

                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    if (showGameSelector) {
        MinigamesSelectorDialog(
            pet = pet,
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
            onReward = { pts, exp -> onPlayMinigame("memory", pts, exp, 0) }
        )
    }

    if (showSnakeGame) {
        SnakeGameDialog(
            pet = pet,
            isDark = isDark,
            onDismiss = { showSnakeGame = false },
            onReward = { pts, exp, score -> onPlayMinigame("snake", pts, exp, score) }
        )
    }

    if (showFlappyGame) {
        FlappyThorGameDialog(
            pet = pet,
            isDark = isDark,
            onDismiss = { showFlappyGame = false },
            onReward = { pts, exp, score -> onPlayMinigame("flappy", pts, exp, score) }
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

fun getPetDrawableRes(
    pet: Pet,
    accessory: String? = pet.getActiveEquippedAccessory(),
    isSleeping: Boolean = pet.getActiveIsSleeping(),
    isWashing: Boolean = false,
    isPlayingBall: Boolean = false
): Int {
    val isCuky = pet.isCuky()
    return when {
        isWashing -> if (isCuky) R.drawable.ic_cuky_bath else R.drawable.ic_thor_bath
        isPlayingBall -> if (isCuky) R.drawable.ic_cuky_play else R.drawable.ic_thor_play
        isSleeping -> if (isCuky) R.drawable.ic_cuky_sleep else R.drawable.ic_thor_sleep
        else -> when (accessory) {
            Pet.ACC_COLLAR -> if (isCuky) R.drawable.ic_cuky_collar else R.drawable.ic_thor_collar
            Pet.ACC_MUSTACHE -> if (isCuky) R.drawable.ic_cuky_mustache else R.drawable.ic_thor_mustache
            Pet.ACC_BALLOON -> if (isCuky) R.drawable.ic_cuky_balloon else R.drawable.ic_thor_balloon
            Pet.ACC_BOW -> if (isCuky) R.drawable.ic_cuky_bow else R.drawable.ic_thor_bow
            Pet.ACC_HAT -> if (isCuky) R.drawable.ic_cuky_hat else R.drawable.ic_thor_hat
            Pet.ACC_BANDANA -> if (isCuky) R.drawable.ic_cuky_bandana else R.drawable.ic_thor_bandana
            Pet.ACC_GLASSES -> if (isCuky) R.drawable.ic_cuky_glasses else R.drawable.ic_thor_glasses
            Pet.ACC_CROWN -> if (isCuky) R.drawable.ic_cuky_crown else R.drawable.ic_thor_crown
            Pet.ACC_BANANA -> if (isCuky) R.drawable.ic_cuky_banana else R.drawable.ic_thor_banana
            Pet.ACC_SOCKS -> if (isCuky) R.drawable.ic_cuky_socks else R.drawable.ic_thor_socks
            else -> if (isCuky) R.drawable.ic_cuky_base_trans else R.drawable.ic_thor_base_trans
        }
    }
}

