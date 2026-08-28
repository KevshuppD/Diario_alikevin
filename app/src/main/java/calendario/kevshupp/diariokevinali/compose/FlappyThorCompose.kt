package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// MOTOR DE SONIDO RETRO 8-BIT (Delegado a RetroGameAudioEngine)
// =========================================================================

object FlappyAudioEngine {
    fun init(context: Context) = RetroGameAudioEngine.init(context)
    fun startBgm(enabled: Boolean) = RetroGameAudioEngine.startBgm("FLAPPY", enabled)
    fun stopBgm() = RetroGameAudioEngine.stopBgm()
    fun playJump(enabled: Boolean) = RetroGameAudioEngine.playJump(enabled)
    fun playPoint(enabled: Boolean) = RetroGameAudioEngine.playPoint(enabled)
    fun playHeart(enabled: Boolean) = RetroGameAudioEngine.playHeart(enabled)
    fun playDie(enabled: Boolean) = RetroGameAudioEngine.playDie(enabled)
}



// =========================================================================
// MODELOS DEL JUEGO
// =========================================================================

private data class Pipe(
    var x: Float,
    val topHeight: Float,
    val gap: Float,
    var passed: Boolean = false,
    var hasHeart: Boolean = false,
    var heartCollected: Boolean = false
)

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1f,
    val color: Color
)

// =========================================================================
// COMPOSABLE PRINCIPAL: FLAPPY THOR
// =========================================================================

@Composable
fun FlappyThorGameDialog(
    pet: Pet,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onReward: (points: Int, exp: Int, score: Int) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FlappyAudioEngine.init(context)
    }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var hasClaimedDailyRewardThisSession by remember { mutableStateOf(false) }
    val isDailyPending = pet.lastFlappyDate != today

    val prefs = remember(context) { context.getSharedPreferences("flappy_thor_prefs", Context.MODE_PRIVATE) }
    val isCurrentUserKevin = remember(context) {
        val mainPrefs = context.getSharedPreferences("diario_prefs", Context.MODE_PRIVATE)
        val uid = mainPrefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        uid.contains("kevin", ignoreCase = true)
    }
    val cloudHighScore = if (isCurrentUserKevin) pet.flappyHighScoreKevin else pet.flappyHighScoreAli
    var highScore by remember { mutableStateOf(maxOf(prefs.getInt("high_score", 0), cloudHighScore)) }
    
    // Modo de visualización: "FULLSCREEN" o "POCKET"
    var viewMode by remember { mutableStateOf(prefs.getString("view_mode", "FULLSCREEN") ?: "FULLSCREEN") }
    // Sonido activado/desactivado
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }

    // Estados de juego
    var gameState by remember { mutableStateOf("READY") } // "READY", "PLAYING", "GAMEOVER"
    var score by remember { mutableStateOf(0) }
    var heartsCollected by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    // Física
    val thorX = 0.25f
    var thorY by remember { mutableStateOf(0.42f) }
    var thorVelocity by remember { mutableStateOf(0f) }
    var gameTicks by remember { mutableStateOf(0L) }

    // Elementos dinámicos
    val pipes = remember { mutableStateListOf<Pipe>() }
    val particles = remember { mutableStateListOf<Particle>() }

    // Sprite de Thor según accesorio
    val thorResId = remember(pet.equippedAccessory) {
        when (pet.equippedAccessory) {
            "collar" -> R.drawable.ic_thor_collar
            "mustache" -> R.drawable.ic_thor_mustache
            "balloon" -> R.drawable.ic_thor_balloon
            "bow" -> R.drawable.ic_thor_bow
            "hat" -> R.drawable.ic_thor_hat
            "bandana" -> R.drawable.ic_thor_bandana
            "glasses" -> R.drawable.ic_thor_glasses
            "crown" -> R.drawable.ic_thor_crown
            "banana" -> R.drawable.ic_thor_banana
            "socks" -> R.drawable.ic_thor_socks
            else -> R.drawable.ic_thor_base_trans
        }
    }
    val thorBitmap = ImageBitmap.imageResource(id = thorResId)

    // Control del BGM en bucle sincronizado con estado de juego, pausa y sonido
    DisposableEffect(Unit) {
        onDispose {
            FlappyAudioEngine.stopBgm()
        }
    }

    LaunchedEffect(gameState, isPaused, soundEnabled) {
        if (gameState == "PLAYING" && !isPaused && soundEnabled) {
            FlappyAudioEngine.startBgm(true)
        } else {
            FlappyAudioEngine.stopBgm()
        }
    }

    fun toggleSound() {
        val newVal = !soundEnabled
        soundEnabled = newVal
        prefs.edit().putBoolean("sound_enabled", newVal).apply()
        if (!newVal) {
            FlappyAudioEngine.stopBgm()
        }
    }

    fun switchMode(newMode: String) {
        viewMode = newMode
        prefs.edit().putString("view_mode", newMode).apply()
        // Limpiar tuberías al cambiar de modo para reiniciar con la calibración correcta
        pipes.clear()
        thorY = 0.42f
        thorVelocity = 0f
    }

    fun triggerGameOverRewards(finalScore: Int, hearts: Int) {
        val earnedLp = (finalScore * 2 + hearts * 2).coerceAtLeast(if (finalScore > 0) 5 else 0)
        val earnedXp = (finalScore * 5 + hearts * 3).coerceAtLeast(if (finalScore > 0) 5 else 0)
        if (finalScore > 0) {
            if (isDailyPending && !hasClaimedDailyRewardThisSession) {
                onReward(earnedLp, earnedXp, finalScore)
                hasClaimedDailyRewardThisSession = true
            } else {
                onReward(0, 0, finalScore)
            }
        }
    }

    fun resetGame() {
        thorY = 0.42f
        thorVelocity = 0f
        pipes.clear()
        particles.clear()
        score = 0
        heartsCollected = 0
        isPaused = false
        gameTicks = 0L
        gameState = "READY"
    }

    // Configuración balanceada y accesible
    val isFull = viewMode == "FULLSCREEN"
    val jumpForce = if (isFull) -0.0078f else -0.0068f
    val gravity = if (isFull) 0.00038f else 0.00030f
    val maxFallVelocity = if (isFull) 0.0080f else 0.0070f
    val baseSpeed = if (isFull) 0.0032f else 0.0028f
    val spawnInterval = if (isFull) 140L else 160L

    fun jump() {
        if (gameState == "READY") {
            gameState = "PLAYING"
            thorVelocity = jumpForce
            FlappyAudioEngine.playJump(soundEnabled)
        } else if (gameState == "PLAYING" && !isPaused) {
            thorVelocity = jumpForce
            FlappyAudioEngine.playJump(soundEnabled)
        } else if (gameState == "GAMEOVER") {
            resetGame()
            gameState = "PLAYING"
            thorVelocity = jumpForce
            FlappyAudioEngine.playJump(soundEnabled)
        }
    }

    // Bucle de física fluido adaptativo sincronizado con la tasa de refresco (60 / 90 / 120 FPS)
    LaunchedEffect(gameState, isPaused, viewMode) {
        var lastFrameTimeNanos = 0L
        var accumulatedTimeMs = 0.0
        var lastSpawnTick = 0L
        while (gameState == "PLAYING" && !isPaused) {
            withFrameNanos { nowNanos ->
                if (lastFrameTimeNanos == 0L) {
                    lastFrameTimeNanos = nowNanos
                    return@withFrameNanos
                }
                val deltaMs = (nowNanos - lastFrameTimeNanos) / 1_000_000.0
                lastFrameTimeNanos = nowNanos

                // Factor delta normalizado respecto a 60 FPS (16.666 ms)
                val dtFactor = (deltaMs / 16.666).coerceIn(0.2, 3.0).toFloat()
                accumulatedTimeMs += deltaMs
                gameTicks = (accumulatedTimeMs / 16.666).toLong()

                // 1. Gravedad y posición
                thorVelocity = (thorVelocity + gravity * dtFactor).coerceAtMost(maxFallVelocity)
                thorY += thorVelocity * dtFactor

                // Límites
                if (thorY < 0.03f) {
                    thorY = 0.03f
                    thorVelocity = 0f
                }
                val floorLimit = if (isFull) 0.81f else 0.82f
                if (thorY > floorLimit) {
                    thorY = floorLimit
                    gameState = "GAMEOVER"
                    FlappyAudioEngine.playDie(soundEnabled)
                    if (score > highScore) {
                        highScore = score
                        prefs.edit().putInt("high_score", highScore).apply()
                    }
                    triggerGameOverRewards(score, heartsCollected)
                    return@withFrameNanos
                }

                // 2. Generación progresiva de tuberías con aperturas amplias y cómodas
                if (pipes.isEmpty() || gameTicks - lastSpawnTick >= spawnInterval) {
                    lastSpawnTick = gameTicks
                    val currentPipeGap = if (isFull) 0.32f else 0.38f // Apertura generosa para pasar con facilidad
                    val minTop = if (isFull) 0.12f else 0.14f
                    val maxTop = if (isFull) 0.40f else 0.36f
                    val topH = Random.nextFloat() * (maxTop - minTop) + minTop
                    val spawnHeart = Random.nextFloat() < 0.45f

                    pipes.add(
                        Pipe(
                            x = 1.15f,
                            topHeight = topH,
                            gap = currentPipeGap,
                            hasHeart = spawnHeart
                        )
                    )
                }

                // 3. Velocidad suave y hitbox justa
                val currentSpeed = (baseSpeed + (score * 0.00003f).coerceAtMost(0.0012f)) * dtFactor
                val thorRadius = if (isFull) 0.024f else 0.022f // Hitbox más permisiva

                val iterator = pipes.iterator()
                while (iterator.hasNext()) {
                    val pipe = iterator.next()
                    pipe.x -= currentSpeed

                    // Superar tubería -> Punto
                    if (!pipe.passed && pipe.x + 0.10f < thorX) {
                        pipe.passed = true
                        score++
                        FlappyAudioEngine.playPoint(soundEnabled)
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("high_score", highScore).apply()
                        }
                    }

                    // Recolectar corazón
                    if (pipe.hasHeart && !pipe.heartCollected) {
                        val heartX = pipe.x + 0.06f
                        val heartY = pipe.topHeight + (pipe.gap / 2f)
                        val dx = heartX - thorX
                        val dy = heartY - thorY
                        if (dx * dx + dy * dy < 0.0045f) {
                            pipe.heartCollected = true
                            heartsCollected++
                            score += 2
                            FlappyAudioEngine.playHeart(soundEnabled)
                            repeat(6) {
                                particles.add(
                                    Particle(
                                        x = heartX,
                                        y = heartY,
                                        vx = (Random.nextFloat() - 0.5f) * 0.012f,
                                        vy = (Random.nextFloat() - 0.5f) * 0.012f,
                                        color = Color(0xFFFF4081)
                                    )
                                )
                            }
                        }
                    }

                    // Colisión con tuberías
                    val pipeLeft = pipe.x
                    val pipeRight = pipe.x + 0.12f
                    if (thorX + thorRadius > pipeLeft && thorX - thorRadius < pipeRight) {
                        val topPipeBottom = pipe.topHeight
                        val bottomPipeTop = pipe.topHeight + pipe.gap

                        if (thorY - thorRadius < topPipeBottom || thorY + thorRadius > bottomPipeTop) {
                            gameState = "GAMEOVER"
                            FlappyAudioEngine.playDie(soundEnabled)
                            if (score > highScore) {
                                highScore = score
                                prefs.edit().putInt("high_score", highScore).apply()
                            }
                            triggerGameOverRewards(score, heartsCollected)
                            return@withFrameNanos
                        }
                    }

                    if (pipe.x < -0.25f) {
                        iterator.remove()
                    }
                }

                // 4. Actualizar partículas
                val pIterator = particles.iterator()
                while (pIterator.hasNext()) {
                    val p = pIterator.next()
                    p.x += p.vx * dtFactor
                    p.y += p.vy * dtFactor
                    p.alpha -= 0.035f * dtFactor
                    if (p.alpha <= 0f) pIterator.remove()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        if (viewMode == "FULLSCREEN") {
            // =========================================================================
            // MODO PANTALLA COMPLETA
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF1A1B26) else Color(0xFF68BBE3))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (gameState != "GAMEOVER") {
                            jump()
                        }
                    }
            ) {
                // Lienzo Pantalla Completa
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Nubes retro
                    val cloudOffset = (gameTicks * 0.5f) % (w + 160f)
                    drawPixelCloud(w - cloudOffset, h * 0.07f, isDark)
                    val cloudOffset2 = ((gameTicks * 0.35f) + w * 0.5f) % (w + 180f)
                    drawPixelCloud(w - cloudOffset2, h * 0.15f, isDark)

                    // 2. Tuberías
                    pipes.forEach { pipe ->
                        val pX = pipe.x * w
                        val pW = 0.15f * w
                        val topH = pipe.topHeight * h
                        val bottomY = (pipe.topHeight + pipe.gap) * h
                        val bottomH = h * 0.84f - bottomY

                        drawFullRetroPipe(pX, 0f, pW, topH, isTop = true, isDark = isDark)
                        if (bottomH > 0f) {
                            drawFullRetroPipe(pX, bottomY, pW, bottomH, isTop = false, isDark = isDark)
                        }

                        if (pipe.hasHeart && !pipe.heartCollected) {
                            val heartX = (pipe.x + 0.075f) * w
                            val heartBob = sin(gameTicks * 0.12f) * 8f
                            val heartY = (pipe.topHeight + pipe.gap / 2f) * h + heartBob
                            drawFullPixelHeart(heartX, heartY)
                        }
                    }

                    // 4. Suelo
                    val groundY = h * 0.84f
                    val groundColor = if (isDark) Color(0xFF2E7D32) else Color(0xFF43A047)
                    val dirtColor = if (isDark) Color(0xFF3E2723) else Color(0xFFD7CCC8)

                    drawRect(color = groundColor, topLeft = Offset(0f, groundY), size = Size(w, 14.dp.toPx()))
                    drawRect(color = Color.Black, topLeft = Offset(0f, groundY), size = Size(w, 2.5.dp.toPx()))
                    drawRect(color = dirtColor, topLeft = Offset(0f, groundY + 14.dp.toPx()), size = Size(w, h - groundY - 14.dp.toPx()))

                    val groundOffset = (gameTicks * 3.2f) % 18.dp.toPx()
                    var gx = -groundOffset
                    while (gx < w) {
                        drawLine(
                            color = if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                            start = Offset(gx, groundY + 2.5.dp.toPx()),
                            end = Offset(gx + 8.dp.toPx(), groundY + 14.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        gx += 18.dp.toPx()
                    }

                    // 5. Partículas
                    particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = 3.5.dp.toPx(),
                            center = Offset(p.x * w, p.y * h)
                        )
                    }

                    // 6. Thor Pájaro Blanco Sprite Dedicado Flappy
                    val thorDrawX = thorX * w
                    val thorDrawY = thorY * h
                    val thorSizePx = 54.dp.toPx()
                    val angle = (thorVelocity * 4200f).coerceIn(-26f, 45f)

                    rotate(degrees = angle, pivot = Offset(thorDrawX, thorDrawY)) {
                        drawWhiteThorBirdSprite(
                            cx = thorDrawX,
                            cy = thorDrawY,
                            sizePx = thorSizePx,
                            velocity = thorVelocity,
                            ticks = gameTicks,
                            accessory = pet.equippedAccessory ?: ""
                        )
                    }
                }

                // HUD Superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Salir
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, Color.White, shape = RoundedCornerShape(6.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("❌ Salir", fontFamily = Vt323, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Puntos
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            fontFamily = Vt323,
                            fontSize = 38.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "❤️ $heartsCollected  |  RÉCORD: $highScore",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Botones Pocket / Sonido / Pausa
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Sonido
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.White, shape = RoundedCornerShape(6.dp))
                                .clickable { toggleSound() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(if (soundEnabled) "🔊" else "🔇", fontSize = 14.sp, color = Color.White)
                        }

                        // Cambiar a Pocket
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.White, shape = RoundedCornerShape(6.dp))
                                .clickable { switchMode("POCKET") }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📱 Pocket", fontFamily = Vt323, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Pausa
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.White, shape = RoundedCornerShape(6.dp))
                                .clickable {
                                    if (gameState == "PLAYING") isPaused = !isPaused
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(if (isPaused) "▶️" else "⏸️", fontSize = 14.sp, color = Color.White)
                        }
                    }
                }

                // Mensaje Ready
                if (gameState == "READY") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(12.dp))
                                .border(2.dp, Color.White, shape = RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🐱 FLAPPY THOR 🪽",
                                    fontFamily = Vt323,
                                    fontSize = 32.sp,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "¡TOCA LA PANTALLA PARA VOLAR!",
                                    fontFamily = Vt323,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Esquiva tubos y atrapa corazones flotantes.",
                                    fontFamily = Vt323,
                                    fontSize = 16.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Mensaje Game Over
                if (gameState == "GAMEOVER") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .border(3.dp, Color(0xFFFF4081), shape = RoundedCornerShape(12.dp)),
                            color = if (isDark) Color(0xFF1E1E2E) else Color(0xFFFFFBEA),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "¡FIN DE LA PARTIDA!",
                                    fontFamily = Vt323,
                                    fontSize = 28.sp,
                                    color = Color(0xFFFF4081),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Puntaje: $score pts  |  ❤️ Atrapados: $heartsCollected",
                                    fontFamily = Vt323,
                                    fontSize = 20.sp,
                                    color = if (isDark) Color.White else Color(0xFF4A2511),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val earnedLp = (score * 2 + heartsCollected * 2).coerceAtLeast(if (score > 0) 5 else 0)
                                val earnedXp = (score * 5 + heartsCollected * 3).coerceAtLeast(if (score > 0) 5 else 0)

                                if (isDailyPending && hasClaimedDailyRewardThisSession) {
                                    Text(
                                        text = "🎉 ¡Recompensa Diaria Obtenida! +$earnedLp ❤️ +$earnedXp EXP\n⭐ ¡Modo Libre Activado!",
                                        fontFamily = Vt323,
                                        fontSize = 16.sp,
                                        color = Color(0xFF4CAF50),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "⭐ Modo Libre Activo (Partida de Récord)",
                                        fontFamily = Vt323,
                                        fontSize = 16.sp,
                                        color = Color(0xFFFF9800),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // Ranking Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFFF4081).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF161622) else Color(0xFFFFF4D6), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text("🏆 RÉCORDS DE FLAPPY THOR", fontFamily = Vt323, fontSize = 16.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Text("👑 Kevin: ${pet.flappyHighScoreKevin.coerceAtLeast(if (isCurrentUserKevin) highScore else 0)} pts", fontFamily = Vt323, fontSize = 15.sp, color = if (isDark) Color.White else Color.Black)
                                            Text("👑 Ali: ${pet.flappyHighScoreAli.coerceAtLeast(if (!isCurrentUserKevin) highScore else 0)} pts", fontFamily = Vt323, fontSize = 15.sp, color = if (isDark) Color.White else Color.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { jump() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                    shape = RectangleShape
                                ) {
                                    Text("🔄 Jugar de Nuevo", fontFamily = Vt323, fontSize = 20.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (score > 0) triggerGameOverRewards(score, heartsCollected)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RectangleShape
                                ) {
                                    Text("🏆 Salir", fontFamily = Vt323, fontSize = 20.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Pausa
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏸️ PAUSA",
                            fontFamily = Vt323,
                            fontSize = 36.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // MODO CONSOLA POCKET (THOR POCKET™)
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFC5C6C0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Marco gris de la consola
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .background(Color(0xFF3F403F), shape = RoundedCornerShape(12.dp))
                            .border(3.dp, Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Barra superior
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF8B1E3F), shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BATTERY",
                                    fontFamily = Vt323,
                                    color = Color(0xFF8E8F88),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Toggle sonido
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF8E8F88), shape = RoundedCornerShape(4.dp))
                                        .clickable { toggleSound() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(if (soundEnabled) "🔊" else "🔇", fontSize = 10.sp, color = Color.White)
                                }

                                // Cambiar a Pantalla Completa
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF8E8F88), shape = RoundedCornerShape(4.dp))
                                        .clickable { switchMode("FULLSCREEN") }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "📺 FULLSCREEN",
                                        fontFamily = Vt323,
                                        color = Color(0xFFFFD54F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pantalla LCD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .background(Color(0xFF9BBC0F), shape = RoundedCornerShape(4.dp))
                                .border(2.dp, Color(0xFF0F380F), shape = RoundedCornerShape(4.dp))
                                .clipToBounds()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    jump()
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                val cloudOffset = (gameTicks * 0.4f) % (w + 120f)
                                drawPixelCloud(w - cloudOffset, h * 0.12f, false)

                                pipes.forEach { pipe ->
                                    val pX = pipe.x * w
                                    val pW = 0.15f * w
                                    val topH = pipe.topHeight * h
                                    val bottomY = (pipe.topHeight + pipe.gap) * h
                                    val bottomH = h * 0.88f - bottomY

                                    drawRetroPipe(pX, 0f, pW, topH, isTop = true)
                                    if (bottomH > 0f) {
                                        drawRetroPipe(pX, bottomY, pW, bottomH, isTop = false)
                                    }

                                    if (pipe.hasHeart && !pipe.heartCollected) {
                                        val heartX = (pipe.x + 0.075f) * w
                                        val heartBob = sin(gameTicks * 0.12f) * 6f
                                        val heartY = (pipe.topHeight + pipe.gap / 2f) * h + heartBob
                                        drawFullPixelHeart(heartX, heartY)
                                    }
                                }

                                val groundY = h * 0.88f
                                drawRect(color = Color(0xFF8BAC0F), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
                                drawRect(color = Color(0xFF0F380F), topLeft = Offset(0f, groundY), size = Size(w, 3.dp.toPx()))

                                val thorDrawX = thorX * w
                                val thorDrawY = thorY * h
                                val thorSizePx = 40.dp.toPx()
                                val angle = (thorVelocity * 3000f).coerceIn(-24f, 40f)

                                rotate(degrees = angle, pivot = Offset(thorDrawX, thorDrawY)) {
                                    drawWhiteThorBirdSprite(
                                        cx = thorDrawX,
                                        cy = thorDrawY,
                                        sizePx = thorSizePx,
                                        velocity = thorVelocity,
                                        ticks = gameTicks,
                                        accessory = pet.equippedAccessory ?: "",
                                        isPocket = true
                                    )
                                }
                            }

                            // HUD LCD
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PUNTOS: $score ❤️$heartsCollected",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F380F),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RÉCORD: $highScore",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F380F),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (gameState == "READY") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF9BBC0F).copy(alpha = 0.8f)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("🐱 FLAPPY THOR 🪽", fontFamily = Vt323, fontSize = 28.sp, color = Color(0xFF0F380F), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("¡TOCA PARA VOLAR!", fontFamily = Vt323, fontSize = 20.sp, color = Color(0xFF306230), fontWeight = FontWeight.Bold)
                                }
                            } else if (gameState == "GAMEOVER") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF9BBC0F).copy(alpha = 0.88f))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("GAME OVER", fontFamily = Vt323, fontSize = 30.sp, color = Color(0xFF0F380F), fontWeight = FontWeight.Bold)
                                    Text("Puntos: $score  |  Corazones: $heartsCollected", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFF306230), fontWeight = FontWeight.Bold)
                                    val earnedLp = (score * 2).coerceAtLeast(0)
                                    val earnedXp = (score * 5).coerceAtLeast(0)
                                    Text("Recompensa: +$earnedLp ❤️ +$earnedXp EXP", fontFamily = Vt323, fontSize = 17.sp, color = Color(0xFF0F380F), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Presiona 'A' para Reintentar", fontFamily = Vt323, fontSize = 16.sp, color = Color(0xFF306230))
                                    Text("Presiona 'START' para Guardar", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFF0F380F))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controles de Consola
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("THOR POCKET™", fontFamily = Vt323, color = Color(0xFF2C2D2F), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // SELECT (SALIR)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp, 10.dp)
                                        .graphicsLayer(rotationZ = -25f)
                                        .background(Color(0xFF6B6A68), shape = RoundedCornerShape(3.dp))
                                        .border(1.dp, Color.Black, shape = RoundedCornerShape(3.dp))
                                        .clickable { onDismiss() }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("SELECT", fontFamily = Vt323, fontSize = 9.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }

                            // START (GUARDAR)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp, 10.dp)
                                        .graphicsLayer(rotationZ = -25f)
                                        .background(Color(0xFF6B6A68), shape = RoundedCornerShape(3.dp))
                                        .border(1.dp, Color.Black, shape = RoundedCornerShape(3.dp))
                                        .clickable {
                                            if (score > 0) triggerGameOverRewards(score, heartsCollected)
                                            onDismiss()
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("START", fontFamily = Vt323, fontSize = 9.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // D-PAD & Botones B/A
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // D-PAD
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clickable { jump() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 130.dp)
                                    .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(130.dp, 44.dp)
                                    .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                            )
                            Box(modifier = Modifier.size(44.dp).background(Color(0xFF252424)))
                        }

                        // Botones B y A
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFF8B1E3F), shape = CircleShape)
                                        .border(3.dp, Color.Black, shape = CircleShape)
                                        .clickable { if (gameState == "PLAYING") isPaused = !isPaused },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("B", color = Color.White, fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isPaused) "PLAY" else "PAUSA", fontFamily = Vt323, fontSize = 10.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }

                            Column(modifier = Modifier.padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFF8B1E3F), shape = CircleShape)
                                        .border(3.dp, Color.Black, shape = CircleShape)
                                        .clickable { jump() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A", color = Color.White, fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("FLAP", fontFamily = Vt323, fontSize = 10.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DIBUJO EN CANVAS RETRO
// ==========================================

private fun DrawScope.drawFullRetroPipe(x: Float, y: Float, width: Float, height: Float, isTop: Boolean, isDark: Boolean) {
    if (height <= 0f) return
    val lipHeight = 20.dp.toPx().coerceAtMost(height)
    val lipExtra = 6.dp.toPx()

    val baseColor = if (isDark) Color(0xFF2E7D32) else Color(0xFF43A047)
    val lightColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF81C784)
    val darkColor = if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32)

    val bodyY = if (isTop) y else y + lipHeight
    val bodyH = height - lipHeight
    if (bodyH > 0f) {
        drawRect(color = baseColor, topLeft = Offset(x, bodyY), size = Size(width, bodyH))
        drawRect(color = lightColor, topLeft = Offset(x + 5.dp.toPx(), bodyY), size = Size(5.dp.toPx(), bodyH))
        drawRect(color = darkColor, topLeft = Offset(x + width - 8.dp.toPx(), bodyY), size = Size(8.dp.toPx(), bodyH))
        drawRect(color = Color.Black, topLeft = Offset(x, bodyY), size = Size(width, bodyH), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
    }

    val lipY = if (isTop) y + height - lipHeight else y
    val lipX = x - lipExtra / 2f
    val lipW = width + lipExtra

    drawRect(color = baseColor, topLeft = Offset(lipX, lipY), size = Size(lipW, lipHeight))
    drawRect(color = lightColor, topLeft = Offset(lipX + 5.dp.toPx(), lipY), size = Size(5.dp.toPx(), lipHeight))
    drawRect(color = darkColor, topLeft = Offset(lipX + lipW - 8.dp.toPx(), lipY), size = Size(8.dp.toPx(), lipHeight))
    drawRect(color = Color.Black, topLeft = Offset(lipX, lipY), size = Size(lipW, lipHeight), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
}

private fun DrawScope.drawRetroPipe(x: Float, y: Float, width: Float, height: Float, isTop: Boolean) {
    if (height <= 0f) return
    val lipHeight = 16.dp.toPx().coerceAtMost(height)
    val lipExtra = 4.dp.toPx()

    val bodyY = if (isTop) y else y + lipHeight
    val bodyH = height - lipHeight
    if (bodyH > 0f) {
        drawRect(color = Color(0xFF306230), topLeft = Offset(x, bodyY), size = Size(width, bodyH))
        drawRect(color = Color(0xFF8BAC0F), topLeft = Offset(x + 4.dp.toPx(), bodyY), size = Size(4.dp.toPx(), bodyH))
        drawRect(color = Color(0xFF0F380F), topLeft = Offset(x + width - 6.dp.toPx(), bodyY), size = Size(6.dp.toPx(), bodyH))
        drawRect(color = Color(0xFF0F380F), topLeft = Offset(x, bodyY), size = Size(width, bodyH), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    }

    val lipY = if (isTop) y + height - lipHeight else y
    val lipX = x - lipExtra / 2f
    val lipW = width + lipExtra

    drawRect(color = Color(0xFF306230), topLeft = Offset(lipX, lipY), size = Size(lipW, lipHeight))
    drawRect(color = Color(0xFF8BAC0F), topLeft = Offset(lipX + 4.dp.toPx(), lipY), size = Size(4.dp.toPx(), lipHeight))
    drawRect(color = Color(0xFF0F380F), topLeft = Offset(lipX + lipW - 6.dp.toPx(), lipY), size = Size(6.dp.toPx(), lipHeight))
    drawRect(color = Color(0xFF0F380F), topLeft = Offset(lipX, lipY), size = Size(lipW, lipHeight), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
}

private fun DrawScope.drawFullPixelHeart(cx: Float, cy: Float) {
    val pixelSize = 3.dp.toPx()
    val heartMatrix = arrayOf(
        intArrayOf(0, 1, 1, 0, 1, 1, 0),
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(0, 1, 1, 1, 1, 1, 0),
        intArrayOf(0, 0, 1, 1, 1, 0, 0),
        intArrayOf(0, 0, 0, 1, 0, 0, 0)
    )

    val startX = cx - (3.5f * pixelSize)
    val startY = cy - (3f * pixelSize)

    heartMatrix.forEachIndexed { r, row ->
        row.forEachIndexed { c, cell ->
            if (cell == 1) {
                drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(startX + c * pixelSize, startY + r * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}

private fun DrawScope.drawPixelCloud(x: Float, y: Float, isDark: Boolean) {
    val cloudW = 60.dp.toPx()
    val cloudH = 22.dp.toPx()
    val color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.65f)

    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(cloudW, cloudH),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(x + 12.dp.toPx(), y - 8.dp.toPx()),
        size = Size(36.dp.toPx(), 18.dp.toPx()),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
}

/**
 * Dibuja el sprite pixel-art dedicado y limpio de Thor Blanco (Perrito/Pájaro volador).
 * Diseño limpio sin bordes extraños, con pelaje blanco puro, orejitas esponjosas, ojitos negros tiernos y alitas batientes.
 */
private fun DrawScope.drawWhiteThorBirdSprite(
    cx: Float,
    cy: Float,
    sizePx: Float,
    velocity: Float,
    ticks: Long,
    accessory: String,
    isPocket: Boolean = false
) {
    val pixel = sizePx / 16f
    val startX = cx - (sizePx / 2f)
    val startY = cy - (sizePx / 2f)

    // Paleta limpia
    val white = if (isPocket) Color(0xFFE0F8D0) else Color(0xFFFFFFFF)
    val softGray = if (isPocket) Color(0xFF8BAC0F) else Color(0xFFE2E8F0)
    val darkOutline = if (isPocket) Color(0xFF0F380F) else Color(0xFF2D3748)
    val eyeColor = if (isPocket) Color(0xFF0F380F) else Color(0xFF1A202C)
    val blush = if (isPocket) Color(0xFF8BAC0F) else Color(0xFFFFB2D6)
    val noseColor = if (isPocket) Color(0xFF0F380F) else Color(0xFF1A202C)
    val wingFeather = if (isPocket) Color(0xFF8BAC0F) else Color(0xFFEDF2F7)

    // Aleteo animado suave
    val flapFrame = ((ticks / 4) % 3).toInt()

    // Matriz de Thor Blanco 16x14 píxeles (cabeza y cuerpo adorable)
    // 0: Vacío, 1: Contorno, 2: Blanco, 3: Sombra suave, 4: Ojos, 5: Brillo, 6: Nariz/Hocico, 7: Rubor
    val thorMatrix = arrayOf(
        // Orejitas
        intArrayOf(0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0),
        intArrayOf(1, 2, 2, 1, 0, 0, 0, 0, 0, 1, 2, 2, 1, 0),
        intArrayOf(1, 2, 3, 1, 1, 1, 1, 1, 1, 1, 3, 2, 1, 0),
        // Cabeza
        intArrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0),
        intArrayOf(1, 2, 4, 5, 2, 2, 2, 2, 4, 5, 2, 2, 1, 0),
        intArrayOf(1, 2, 4, 4, 2, 2, 2, 2, 4, 4, 2, 2, 1, 0),
        intArrayOf(1, 7, 2, 2, 2, 6, 6, 2, 2, 2, 7, 2, 1, 0),
        // Cuerpo esponjoso
        intArrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0),
        intArrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0),
        intArrayOf(1, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 1, 0),
        intArrayOf(0, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 0, 0),
        intArrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0)
    )

    thorMatrix.forEachIndexed { r, row ->
        row.forEachIndexed { c, cell ->
            val col = when (cell) {
                1 -> darkOutline
                2 -> white
                3 -> softGray
                4 -> eyeColor
                5 -> Color.White
                6 -> noseColor
                7 -> blush
                else -> null
            }
            if (col != null) {
                drawRect(
                    color = col,
                    topLeft = Offset(startX + c * pixel, startY + r * pixel),
                    size = Size(pixel, pixel)
                )
            }
        }
    }

    // Alita blanca pixelada que bate
    val wingDY = when (flapFrame) {
        0 -> -2 * pixel
        1 -> 0f
        else -> 2 * pixel
    }

    // Ala batiente pixel art
    drawRect(color = darkOutline, topLeft = Offset(startX + 1 * pixel, startY + 6 * pixel + wingDY), size = Size(5 * pixel, 4 * pixel))
    drawRect(color = wingFeather, topLeft = Offset(startX + 2 * pixel, startY + 7 * pixel + wingDY), size = Size(3 * pixel, 2 * pixel))
    drawRect(color = white, topLeft = Offset(startX + 2 * pixel, startY + 7 * pixel + wingDY), size = Size(2 * pixel, 1 * pixel))

    // Accesorios
    when (accessory) {
        "crown" -> {
            val cx = startX + 4 * pixel
            val cy = startY - 2 * pixel
            drawRect(color = Color(0xFFFFD700), topLeft = Offset(cx, cy), size = Size(5 * pixel, 2 * pixel))
            drawRect(color = Color(0xFFFF1744), topLeft = Offset(cx + 2 * pixel, cy - 1 * pixel), size = Size(1 * pixel, 1 * pixel))
        }
        "bow" -> {
            drawRect(color = Color(0xFFFF4081), topLeft = Offset(startX + 2 * pixel, startY + 1 * pixel), size = Size(3 * pixel, 2 * pixel))
        }
        "glasses" -> {
            drawRect(color = darkOutline, topLeft = Offset(startX + 2 * pixel, startY + 4 * pixel), size = Size(9 * pixel, 2 * pixel), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f * pixel))
        }
        "bandana" -> {
            drawRect(color = Color(0xFFE53935), topLeft = Offset(startX + 2 * pixel, startY + 7 * pixel), size = Size(9 * pixel, 2 * pixel))
        }
    }
}

