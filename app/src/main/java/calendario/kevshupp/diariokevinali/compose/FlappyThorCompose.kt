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

// =========================================================================
// MOTOR DE SONIDO RETRO 8-BIT (Chiptune Procedimental / SoundPool)
// =========================================================================

object FlappyAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var soundPool: SoundPool? = null
    private var rawJumpId: Int = 0
    private var rawPointId: Int = 0
    private var rawHeartId: Int = 0
    private var rawDieId: Int = 0
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val sp = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            // Intentar cargar archivos de res/raw si el usuario los agrega
            val res = context.resources
            val pkg = context.packageName

            val jId = res.getIdentifier("flappy_jump", "raw", pkg)
            if (jId != 0) rawJumpId = sp.load(context, jId, 1)

            val pId = res.getIdentifier("flappy_point", "raw", pkg)
            if (pId != 0) rawPointId = sp.load(context, pId, 1)

            val hId = res.getIdentifier("flappy_heart", "raw", pkg)
            if (hId != 0) rawHeartId = sp.load(context, hId, 1)

            val dId = res.getIdentifier("flappy_die", "raw", pkg)
            if (dId != 0) rawDieId = sp.load(context, dId, 1)

            soundPool = sp
        } catch (_: Exception) {}
    }

    fun playJump(enabled: Boolean) {
        if (!enabled) return
        if (rawJumpId != 0) {
            soundPool?.play(rawJumpId, 0.9f, 0.9f, 1, 0, 1.0f)
            return
        }
        // Sintetizador Chiptune Flap / Jump (Pitch bend rápido hacia arriba: 380Hz -> 720Hz)
        scope.launch {
            playToneSweep(startFreq = 380.0, endFreq = 720.0, durationMs = 70, waveType = "SQUARE")
        }
    }

    fun playPoint(enabled: Boolean) {
        if (!enabled) return
        if (rawPointId != 0) {
            soundPool?.play(rawPointId, 0.9f, 0.9f, 1, 0, 1.0f)
            return
        }
        // Sintetizador Chiptune Coin/Point (Doble pitido agudo: 880Hz -> 1320Hz)
        scope.launch {
            playTone(988.0, 50, "SQUARE")
            delay(20)
            playTone(1318.0, 80, "SQUARE")
        }
    }

    fun playHeart(enabled: Boolean) {
        if (!enabled) return
        if (rawHeartId != 0) {
            soundPool?.play(rawHeartId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }
        // Arpegio Mágico de Corazón (C5 - E5 - G5 - C6)
        scope.launch {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in notes) {
                playTone(freq, 40, "TRIANGLE")
                delay(25)
            }
        }
    }

    fun playDie(enabled: Boolean) {
        if (!enabled) return
        if (rawDieId != 0) {
            soundPool?.play(rawDieId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }
        // Sonido 8-bit Hit & Drop (480Hz bajando a 120Hz con ruido)
        scope.launch {
            playToneSweep(startFreq = 480.0, endFreq = 120.0, durationMs = 180, waveType = "NOISE")
        }
    }

    private fun playTone(freq: Double, durationMs: Int, waveType: String) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val sampleVal: Double = when (waveType) {
                    "SQUARE" -> if (sin(2 * PI * freq * t) >= 0) 0.4 else -0.4
                    "TRIANGLE" -> (2.0 / PI) * Math.asin(sin(2 * PI * freq * t)) * 0.5
                    else -> sin(2 * PI * freq * t) * 0.4
                }
                // Decaimiento suave para evitar clics
                val envelope = (1.0 - (i.toDouble() / numSamples))
                buffer[i] = (sampleVal * envelope * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            track.setNotificationMarkerPosition(numSamples)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }
            })
        } catch (_: Exception) {}
    }

    private fun playToneSweep(startFreq: Double, endFreq: Double, durationMs: Int, waveType: String) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2 * PI * currentFreq / sampleRate

                val sampleVal: Double = if (waveType == "NOISE") {
                    val rnd = (Random.nextDouble() * 2.0 - 1.0) * 0.3
                    val sine = sin(phase) * 0.3
                    rnd + sine
                } else {
                    if (sin(phase) >= 0) 0.45 else -0.45
                }
                val envelope = (1.0 - progress)
                buffer[i] = (sampleVal * envelope * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            track.setNotificationMarkerPosition(numSamples)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }
            })
        } catch (_: Exception) {}
    }
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
    onReward: (points: Int, exp: Int) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FlappyAudioEngine.init(context)
    }

    val prefs = remember(context) { context.getSharedPreferences("flappy_thor_prefs", Context.MODE_PRIVATE) }
    var highScore by remember { mutableStateOf(prefs.getInt("high_score", 0)) }
    
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

    fun toggleSound() {
        val newVal = !soundEnabled
        soundEnabled = newVal
        prefs.edit().putBoolean("sound_enabled", newVal).apply()
    }

    fun switchMode(newMode: String) {
        viewMode = newMode
        prefs.edit().putString("view_mode", newMode).apply()
        // Limpiar tuberías al cambiar de modo para reiniciar con la calibración correcta
        pipes.clear()
        thorY = 0.42f
        thorVelocity = 0f
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

    // Configuración balanceada según el modo seleccionado
    val isFull = viewMode == "FULLSCREEN"
    val jumpForce = if (isFull) -0.0084f else -0.0072f
    val gravity = if (isFull) 0.00045f else 0.00034f
    val maxFallVelocity = if (isFull) 0.0088f else 0.0075f
    val pipeGap = if (isFull) 0.25f else 0.36f // Gap balanceado en pantalla completa (25% vs 36% en pantalla pequeña)
    val baseSpeed = if (isFull) 0.0038f else 0.0030f
    val spawnInterval = if (isFull) 120L else 145L

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

    // Bucle de física a 60 FPS
    LaunchedEffect(gameState, isPaused, viewMode) {
        while (gameState == "PLAYING" && !isPaused) {
            delay(16L)
            gameTicks++

            // 1. Gravedad y posición
            thorVelocity = (thorVelocity + gravity).coerceAtMost(maxFallVelocity)
            thorY += thorVelocity

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
                break
            }

            // 2. Generación progresiva de tuberías
            if (pipes.isEmpty() || gameTicks % spawnInterval == 0L) {
                val maxTop = if (isFull) 0.46f else 0.38f
                val minTop = if (isFull) 0.12f else 0.12f
                val topH = Random.nextFloat() * (maxTop - minTop) + minTop
                val spawnHeart = Random.nextFloat() < 0.45f
                pipes.add(
                    Pipe(
                        x = 1.15f,
                        topHeight = topH,
                        gap = pipeGap,
                        hasHeart = spawnHeart
                    )
                )
            }

            // 3. Velocidad con aceleración progresiva por puntaje
            val currentSpeed = baseSpeed + (score * 0.00004f).coerceAtMost(0.0018f)
            val thorRadius = if (isFull) 0.034f else 0.032f

            val iterator = pipes.iterator()
            while (iterator.hasNext()) {
                val pipe = iterator.next()
                pipe.x -= currentSpeed

                // Superar tubería -> Punto
                if (!pipe.passed && pipe.x + 0.12f < thorX) {
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
                    val heartX = pipe.x + 0.07f
                    val heartY = pipe.topHeight + (pipe.gap / 2f)
                    val dx = heartX - thorX
                    val dy = heartY - thorY
                    if (dx * dx + dy * dy < 0.0038f) {
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
                val pipeRight = pipe.x + 0.14f
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
                        break
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
                p.x += p.vx
                p.y += p.vy
                p.alpha -= 0.035f
                if (p.alpha <= 0f) pIterator.remove()
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
                        jump()
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

                    // 2. Colinas de fondo
                    val hillY = h * 0.73f
                    drawCircle(
                        color = if (isDark) Color(0xFF243B55).copy(alpha = 0.5f) else Color(0xFF81C784).copy(alpha = 0.65f),
                        radius = w * 0.6f,
                        center = Offset(w * 0.3f, hillY + w * 0.4f)
                    )
                    drawCircle(
                        color = if (isDark) Color(0xFF141E30).copy(alpha = 0.5f) else Color(0xFFA5D6A7).copy(alpha = 0.65f),
                        radius = w * 0.7f,
                        center = Offset(w * 0.85f, hillY + w * 0.45f)
                    )

                    // 3. Tuberías
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

                    // 6. Thor Volador
                    val thorDrawX = thorX * w
                    val thorDrawY = thorY * h
                    val thorSizePx = 52.dp.toPx()
                    val angle = (thorVelocity * 4200f).coerceIn(-24f, 40f)

                    rotate(degrees = angle, pivot = Offset(thorDrawX, thorDrawY)) {
                        drawImage(
                            image = thorBitmap,
                            dstOffset = IntOffset(
                                (thorDrawX - thorSizePx / 2f).toInt(),
                                (thorDrawY - thorSizePx / 2f).toInt()
                            ),
                            dstSize = IntSize(thorSizePx.toInt(), thorSizePx.toInt())
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Puntaje: $score  |  Corazones: $heartsCollected",
                                    fontFamily = Vt323,
                                    fontSize = 20.sp,
                                    color = if (isDark) Color.White else Color(0xFF4A2511),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val earnedLp = (score * 2).coerceAtLeast(0)
                                val earnedXp = (score * 5).coerceAtLeast(0)
                                Text(
                                    text = "Recompensa: +$earnedLp ❤️ +$earnedXp EXP",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(18.dp))

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
                                        if (score > 0) onReward(earnedLp, earnedXp)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RectangleShape
                                ) {
                                    Text("🏆 Guardar y Salir", fontFamily = Vt323, fontSize = 20.sp, color = Color.White)
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
                                val thorSizePx = 38.dp.toPx()
                                val angle = (thorVelocity * 3000f).coerceIn(-24f, 40f)

                                rotate(degrees = angle, pivot = Offset(thorDrawX, thorDrawY)) {
                                    drawImage(
                                        image = thorBitmap,
                                        dstOffset = IntOffset(
                                            (thorDrawX - thorSizePx / 2f).toInt(),
                                            (thorDrawY - thorSizePx / 2f).toInt()
                                        ),
                                        dstSize = IntSize(thorSizePx.toInt(), thorSizePx.toInt())
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
                                            val earnedLp = (score * 2).coerceAtLeast(0)
                                            val earnedXp = (score * 5).coerceAtLeast(0)
                                            if (score > 0) onReward(earnedLp, earnedXp)
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
