package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import calendario.kevshupp.diariokevinali.R
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

import calendario.kevshupp.diariokevinali.Pet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SnakeParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1f,
    val color: Color
)

@Composable
fun SnakeGameDialog(
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
    val isDailyPending = pet.lastSnakeDate != today

    val prefs = remember(context) { context.getSharedPreferences("snake_game_prefs", Context.MODE_PRIVATE) }
    val isCurrentUserKevin = remember(context) {
        val mainPrefs = context.getSharedPreferences("diario_prefs", Context.MODE_PRIVATE)
        val uid = mainPrefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        uid.contains("kevin", ignoreCase = true)
    }
    val cloudHighScore = if (isCurrentUserKevin) pet.snakeHighScoreKevin else pet.snakeHighScoreAli
    var highScore by remember { mutableStateOf(maxOf(prefs.getInt("high_score", 0), cloudHighScore)) }
    var viewMode by remember { mutableStateOf(prefs.getString("view_mode", "FULLSCREEN") ?: "FULLSCREEN") }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }

    val isFull = viewMode == "FULLSCREEN"
    val gridCols = if (isFull) 14 else 12
    val gridRows = if (isFull) 16 else 12

    // Estados de Juego
    var snake by remember(viewMode) { mutableStateOf(listOf(6 to 6, 6 to 7)) }
    var direction by remember(viewMode) { mutableStateOf(0 to -1) } // Dirección inicial: ARRIBA
    var nextDirection by remember(viewMode) { mutableStateOf(0 to -1) }
    var food by remember(viewMode) { mutableStateOf(3 to 3) }
    var score by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameTicks by remember { mutableStateOf(0L) }

    val particles = remember { mutableStateListOf<SnakeParticle>() }
    val appleBitmap = ImageBitmap.imageResource(id = R.drawable.ic_game_apple)

    DisposableEffect(Unit) {
        onDispose {
            RetroGameAudioEngine.stopBgm()
        }
    }

    LaunchedEffect(isPlaying, isGameOver, soundEnabled) {
        if (isPlaying && !isGameOver && soundEnabled) {
            RetroGameAudioEngine.startBgm("SNAKE", true)
        } else {
            RetroGameAudioEngine.stopBgm()
        }
    }

    fun toggleSound() {
        val newVal = !soundEnabled
        soundEnabled = newVal
        prefs.edit().putBoolean("sound_enabled", newVal).apply()
        if (!newVal) {
            RetroGameAudioEngine.stopBgm()
        }
    }

    fun switchMode(newMode: String) {
        viewMode = newMode
        prefs.edit().putString("view_mode", newMode).apply()
        snake = listOf(6 to 6, 6 to 7)
        direction = 0 to -1
        nextDirection = 0 to -1
        food = 3 to 3
        particles.clear()
    }

    fun spawnFood() {
        var newFood: Pair<Int, Int>
        do {
            newFood = (0 until gridCols).random() to (0 until gridRows).random()
        } while (snake.contains(newFood))
        food = newFood
    }

    fun triggerGameOverRewards(finalScore: Int) {
        if (finalScore > 0) {
            val pts = finalScore * 2
            val xp = finalScore * 5
            if (isDailyPending && !hasClaimedDailyRewardThisSession) {
                onReward(pts, xp, finalScore)
                hasClaimedDailyRewardThisSession = true
            } else {
                onReward(0, 0, finalScore)
            }
        }
    }

    fun resetGame() {
        snake = listOf(6 to 6, 6 to 7)
        direction = 0 to -1
        nextDirection = 0 to -1
        food = 3 to 3
        score = 0
        isGameOver = false
        isPlaying = true
        particles.clear()
    }

    fun tryChangeDirection(dx: Int, dy: Int) {
        if (isGameOver || !isPlaying) return
        // Validar que el giro sea perpendicular para evitar 180° instantáneos
        if (direction.first != 0 && dy != 0) {
            nextDirection = 0 to dy
        } else if (direction.second != 0 && dx != 0) {
            nextDirection = dx to 0
        }
    }

    // Bucle del juego Snake
    LaunchedEffect(isPlaying, isGameOver, viewMode) {
        while (isPlaying && !isGameOver) {
            val speed = (180 - (score * 4)).coerceAtLeast(75)
            delay(speed.toLong())
            gameTicks++

            direction = nextDirection
            val head = snake.first()
            val newHead = (head.first + direction.first) to (head.second + direction.second)

            // Colisión con bordes
            if (newHead.first < 0 || newHead.first >= gridCols || newHead.second < 0 || newHead.second >= gridRows) {
                isGameOver = true
                RetroGameAudioEngine.playDie(soundEnabled)
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("high_score", highScore).apply()
                }
                triggerGameOverRewards(score)
                break
            }

            // Colisión consigo misma
            if (snake.contains(newHead)) {
                isGameOver = true
                RetroGameAudioEngine.playDie(soundEnabled)
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("high_score", highScore).apply()
                }
                triggerGameOverRewards(score)
                break
            }

            val newSnake = mutableListOf(newHead)
            if (newHead == food) {
                score++
                RetroGameAudioEngine.playPoint(soundEnabled)
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("high_score", highScore).apply()
                }
                newSnake.addAll(snake)
                spawnFood()
            } else {
                newSnake.addAll(snake.dropLast(1))
            }
            snake = newSnake
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
            // MODO PANTALLA COMPLETA RETRO PIXEL-ART (CUADROS 100% CUADRADOS)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF101216) else Color(0xFF1E232A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HUD Superior Retro
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Salir
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2B1D24), shape = RoundedCornerShape(4.dp))
                                .border(1.5.dp, Color(0xFFE06C75), shape = RoundedCornerShape(4.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("❌ Salir", fontFamily = Vt323, fontSize = 16.sp, color = Color(0xFFE06C75), fontWeight = FontWeight.Bold)
                        }

                        // Score & Récord
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SNAKE: $score",
                                fontFamily = Vt323,
                                fontSize = 30.sp,
                                color = Color(0xFF98C379),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RÉCORD: $highScore",
                                fontFamily = Vt323,
                                fontSize = 15.sp,
                                color = Color(0xFFABB2BF)
                            )
                        }

                        // Controles de Modo / Sonido / Pausa
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Sonido
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF21252B), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color(0xFF61AFEF), shape = RoundedCornerShape(4.dp))
                                    .clickable { toggleSound() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(if (soundEnabled) "🔊" else "🔇", fontSize = 14.sp, color = Color.White)
                            }

                            // Pocket
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF21252B), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color(0xFFE5C07B), shape = RoundedCornerShape(4.dp))
                                    .clickable { switchMode("POCKET") }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("📱 Pocket", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold)
                            }

                            // Pausa
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF21252B), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color(0xFFABB2BF), shape = RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (!isGameOver) isPlaying = !isPlaying
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(if (isPlaying) "⏸️" else "▶️", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }

                    // Contenedor del Tablero con CÁLCULO DE CASILLAS PERFECTAMENTE CUADRADAS
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(Unit) {
                                var accumulatedX = 0f
                                var accumulatedY = 0f
                                val threshold = 16.dp.toPx()

                                detectDragGestures(
                                    onDragStart = {
                                        accumulatedX = 0f
                                        accumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedX += dragAmount.x
                                        accumulatedY += dragAmount.y

                                        if (abs(accumulatedX) >= threshold || abs(accumulatedY) >= threshold) {
                                            if (abs(accumulatedX) > abs(accumulatedY)) {
                                                if (accumulatedX > 0) tryChangeDirection(1, 0) // Derecha
                                                else tryChangeDirection(-1, 0) // Izquierda
                                            } else {
                                                if (accumulatedY > 0) tryChangeDirection(0, 1) // Abajo
                                                else tryChangeDirection(0, -1) // Arriba
                                            }
                                            // Reseteo para encadenar giros fluidos con un solo trazo continuo
                                            accumulatedX = 0f
                                            accumulatedY = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        accumulatedX = 0f
                                        accumulatedY = 0f
                                    },
                                    onDragCancel = {
                                        accumulatedX = 0f
                                        accumulatedY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val maxCellW = maxWidth / gridCols
                        val maxCellH = maxHeight / gridRows
                        val cellSize = minOf(maxCellW, maxCellH)
                        val boardWidth = cellSize * gridCols
                        val boardHeight = cellSize * gridRows

                        Box(
                            modifier = Modifier
                                .size(boardWidth, boardHeight)
                                .background(Color(0xFF181A1F))
                                .border(3.dp, Color(0xFF98C379), shape = RectangleShape)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cW = size.width / gridCols
                                val cH = size.height / gridRows
                                val cSize = minOf(cW, cH)

                                // 1. Suelo Pixel-Art en Tablero de Ajedrez Retro Sutil
                                for (col in 0 until gridCols) {
                                    for (row in 0 until gridRows) {
                                        val isEven = (col + row) % 2 == 0
                                        val tileBg = if (isEven) Color(0xFF1E222B) else Color(0xFF16191F)
                                        drawRect(
                                            color = tileBg,
                                            topLeft = Offset(col * cSize, row * cSize),
                                            size = Size(cSize, cSize)
                                        )
                                    }
                                }

                                // 2. Manzana Pixel-Art con Brillo y Tallo
                                val appleX = food.first * cSize
                                val appleY = food.second * cSize
                                drawPixelApple(appleX, appleY, cSize, gameTicks)

                                // 3. Serpiente Pixel-Art Procedimental (Cuerpo y Cabeza con Relieve 8-Bit)
                                snake.forEachIndexed { idx, body ->
                                    val isHead = idx == 0
                                    val isTail = idx == snake.size - 1
                                    val segX = body.first * cSize
                                    val segY = body.second * cSize

                                    if (isHead) {
                                        drawPixelSnakeHead(segX, segY, cSize, direction)
                                    } else {
                                        drawPixelSnakeBody(segX, segY, cSize, isTail)
                                    }
                                }
                            }

                            // Pantalla Game Over
                            if (isGameOver) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF000000).copy(alpha = 0.88f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "¡FIN DE LA PARTIDA!",
                                        fontFamily = Vt323,
                                        fontSize = 30.sp,
                                        color = Color(0xFFE06C75),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val pts = score * 2
                                    val xp = score * 5
                                    Text(
                                        text = "Puntaje: $score pts",
                                        fontFamily = Vt323,
                                        fontSize = 22.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (isDailyPending && hasClaimedDailyRewardThisSession) {
                                        Text(
                                            text = "🎉 ¡Recompensa Diaria Reclamada! +$pts ❤️ +$xp EXP\n⭐ ¡Modo Libre Activado!",
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = Color(0xFF98C379),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "⭐ Modo Libre Activo (Partida de Récord)",
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = Color(0xFFE5C07B),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Ranking Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .border(1.dp, Color(0xFF61AFEF).copy(alpha = 0.6f))
                                            .background(Color(0xFF21252B))
                                            .padding(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text("🏆 RÉCORDS DE LA SERPIENTE", fontFamily = Vt323, fontSize = 16.sp, color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                Text("👑 Kevin: ${pet.snakeHighScoreKevin.coerceAtLeast(if (isCurrentUserKevin) highScore else 0)}", fontFamily = Vt323, fontSize = 15.sp, color = Color.White)
                                                Text("👑 Ali: ${pet.snakeHighScoreAli.coerceAtLeast(if (!isCurrentUserKevin) highScore else 0)}", fontFamily = Vt323, fontSize = 15.sp, color = Color.White)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { resetGame() },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF98C379)),
                                        shape = RectangleShape
                                    ) {
                                        Text("🔄 Jugar de Nuevo", fontFamily = Vt323, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (score > 0) triggerGameOverRewards(score)
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF61AFEF)),
                                        shape = RectangleShape
                                    ) {
                                        Text("🏆 Salir", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Pantalla Pausa
                            if (!isPlaying && !isGameOver) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.75f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⏸️ PAUSA", fontFamily = Vt323, fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // D-PAD Ergonómico Retro (Para control táctil cómodo)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Arriba
                            Box(
                                modifier = Modifier
                                    .size(76.dp, 44.dp)
                                    .background(Color(0xFF282C34), shape = RoundedCornerShape(6.dp))
                                    .border(2.dp, Color(0xFF98C379), shape = RoundedCornerShape(6.dp))
                                    .clickable { tryChangeDirection(0, -1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▲", color = Color(0xFF98C379), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Izquierda
                                Box(
                                    modifier = Modifier
                                        .size(76.dp, 44.dp)
                                        .background(Color(0xFF282C34), shape = RoundedCornerShape(6.dp))
                                        .border(2.dp, Color(0xFF98C379), shape = RoundedCornerShape(6.dp))
                                        .clickable { tryChangeDirection(-1, 0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("◀", color = Color(0xFF98C379), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }

                                // Abajo
                                Box(
                                    modifier = Modifier
                                        .size(76.dp, 44.dp)
                                        .background(Color(0xFF282C34), shape = RoundedCornerShape(6.dp))
                                        .border(2.dp, Color(0xFF98C379), shape = RoundedCornerShape(6.dp))
                                        .clickable { tryChangeDirection(0, 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▼", color = Color(0xFF98C379), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }

                                // Derecha
                                Box(
                                    modifier = Modifier
                                        .size(76.dp, 44.dp)
                                        .background(Color(0xFF282C34), shape = RoundedCornerShape(6.dp))
                                        .border(2.dp, Color(0xFF98C379), shape = RoundedCornerShape(6.dp))
                                        .clickable { tryChangeDirection(1, 0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▶", color = Color(0xFF98C379), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .background(Color(0xFF3F403F), shape = RoundedCornerShape(12.dp))
                            .border(3.dp, Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isPlaying && !isGameOver) Color(0xFFFF1744) else Color(0xFF3E1E1E), shape = CircleShape)
                                        .border(1.dp, Color.Black, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("POWER", fontFamily = Vt323, color = Color(0xFFC5C6C0), fontSize = 9.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF8E8F88), shape = RoundedCornerShape(4.dp))
                                        .clickable { toggleSound() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(if (soundEnabled) "🔊" else "🔇", fontSize = 9.sp, color = Color.White)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF8E8F88), shape = RoundedCornerShape(4.dp))
                                        .clickable { switchMode("FULLSCREEN") }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("📺 FULLSCREEN", fontFamily = Vt323, color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Pantalla LCD cuadrada
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color(0xFF9BBC0F))
                                .border(2.dp, Color(0xFF0F380F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .background(Color(0xFF0F380F))
                                    .padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("THOR SNAKE", fontFamily = Vt323, fontSize = 12.sp, color = Color(0xFF9BBC0F), fontWeight = FontWeight.Bold)
                                Text("SCORE:${score.toString().padStart(3, '0')}", fontFamily = Vt323, fontSize = 12.sp, color = Color(0xFF9BBC0F), fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGameOver) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize().background(Color(0xFF0F380F).copy(alpha = 0.95f))
                                    ) {
                                        Text("GAME OVER!", fontFamily = Vt323, fontSize = 28.sp, color = Color(0xFF9BBC0F), fontWeight = FontWeight.Bold)
                                        val pts = score * 2
                                        val xp = score * 5
                                        Text("+$pts LOVE  +$xp XP", fontFamily = Vt323, fontSize = 20.sp, color = Color(0xFF9BBC0F))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("PULSA [A] GUARDAR Y SALIR", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFF9BBC0F).copy(alpha = 0.8f))
                                    }
                                } else {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cellW = size.width / gridCols
                                        val cellH = size.height / gridRows
                                        val cSize = minOf(cellW, cellH)

                                        val dotColor = Color(0xFF8BAC0F).copy(alpha = 0.3f)
                                        for (i in 0 until gridCols) {
                                            for (j in 0 until gridRows) {
                                                drawRect(
                                                    color = dotColor,
                                                    topLeft = Offset(i * cSize + 1.dp.toPx(), j * cSize + 1.dp.toPx()),
                                                    size = Size(2.dp.toPx(), 2.dp.toPx())
                                                )
                                            }
                                        }

                                        drawImage(
                                            image = appleBitmap,
                                            dstOffset = IntOffset((food.first * cSize + 1.dp.toPx()).toInt(), (food.second * cSize + 1.dp.toPx()).toInt()),
                                            dstSize = IntSize((cSize - 2.dp.toPx()).toInt(), (cSize - 2.dp.toPx()).toInt()),
                                            colorFilter = ColorFilter.tint(Color(0xFF0F380F))
                                        )

                                        snake.forEachIndexed { idx, body ->
                                            val isHead = idx == 0
                                            val segX = body.first * cSize
                                            val segY = body.second * cSize

                                            if (isHead) {
                                                drawRect(
                                                    color = Color(0xFF0F380F),
                                                    topLeft = Offset(segX + 1.dp.toPx(), segY + 1.dp.toPx()),
                                                    size = Size(cSize - 2.dp.toPx(), cSize - 2.dp.toPx())
                                                )
                                                val eyeSize = 3.dp.toPx()
                                                val eyeOffset = 4.dp.toPx()
                                                val (eye1, eye2) = when (direction) {
                                                    0 to -1 -> Offset(segX + eyeOffset, segY + eyeOffset) to Offset(segX + cSize - eyeOffset - eyeSize, segY + eyeOffset)
                                                    0 to 1 -> Offset(segX + eyeOffset, segY + cSize - eyeOffset - eyeSize) to Offset(segX + cSize - eyeOffset - eyeSize, segY + cSize - eyeOffset - eyeSize)
                                                    -1 to 0 -> Offset(segX + eyeOffset, segY + eyeOffset) to Offset(segX + eyeOffset, segY + cSize - eyeOffset - eyeSize)
                                                    else -> Offset(segX + cSize - eyeOffset - eyeSize, segY + eyeOffset) to Offset(segX + cSize - eyeOffset - eyeSize, segY + cSize - eyeOffset - eyeSize)
                                                }
                                                drawRect(color = Color(0xFF9BBC0F), topLeft = eye1, size = Size(eyeSize, eyeSize))
                                                drawRect(color = Color(0xFF9BBC0F), topLeft = eye2, size = Size(eyeSize, eyeSize))
                                            } else {
                                                drawRoundRect(
                                                    color = Color(0xFF306230),
                                                    topLeft = Offset(segX + 2.dp.toPx(), segY + 2.dp.toPx()),
                                                    size = Size(cSize - 4.dp.toPx(), cSize - 4.dp.toPx()),
                                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Marca y SELECT / START
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("THOR POCKET™", fontFamily = Vt323, color = Color(0xFF2C2D2F), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // SELECT
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

                            // START
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp, 10.dp)
                                        .graphicsLayer(rotationZ = -25f)
                                        .background(Color(0xFF6B6A68), shape = RoundedCornerShape(3.dp))
                                        .border(1.dp, Color.Black, shape = RoundedCornerShape(3.dp))
                                        .clickable {
                                            if (score > 0) triggerGameOverRewards(score)
                                            onDismiss()
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("START", fontFamily = Vt323, fontSize = 9.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // D-PAD & A/B Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // D-PAD
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 140.dp)
                                    .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(140.dp, 48.dp)
                                    .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                            )
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFF252424)))

                            Box(
                                modifier = Modifier.size(70.dp, 60.dp).align(Alignment.TopCenter).clickable { tryChangeDirection(0, -1) },
                                contentAlignment = Alignment.Center
                            ) { Text("▲", color = Color(0xFF8E8F88), fontSize = 18.sp) }

                            Box(
                                modifier = Modifier.size(70.dp, 60.dp).align(Alignment.BottomCenter).clickable { tryChangeDirection(0, 1) },
                                contentAlignment = Alignment.Center
                            ) { Text("▼", color = Color(0xFF8E8F88), fontSize = 18.sp) }

                            Box(
                                modifier = Modifier.size(60.dp, 70.dp).align(Alignment.CenterStart).clickable { tryChangeDirection(-1, 0) },
                                contentAlignment = Alignment.Center
                            ) { Text("◀", color = Color(0xFF8E8F88), fontSize = 18.sp) }

                            Box(
                                modifier = Modifier.size(60.dp, 70.dp).align(Alignment.CenterEnd).clickable { tryChangeDirection(1, 0) },
                                contentAlignment = Alignment.Center
                            ) { Text("▶", color = Color(0xFF8E8F88), fontSize = 18.sp) }
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
                                        .clickable { isPlaying = !isPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("B", color = Color.White, fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isPlaying) "PAUSA" else "PLAY", fontFamily = Vt323, fontSize = 10.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }

                            Column(modifier = Modifier.padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFF8B1E3F), shape = CircleShape)
                                        .border(3.dp, Color.Black, shape = CircleShape)
                                        .clickable {
                                            if (isGameOver) {
                                                if (score > 0) triggerGameOverRewards(score)
                                                onDismiss()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A", color = Color.White, fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("START", fontFamily = Vt323, fontSize = 10.sp, color = Color(0xFF3F403F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// FUNCIONES DE DIBUJO PIXEL-ART 8-BIT PARA LA SERPIENTE
// =========================================================================

private fun DrawScope.drawPixelApple(x: Float, y: Float, size: Float, ticks: Long) {
    val p = size / 10f
    val bob = sin(ticks * 0.15f) * 1.5.dp.toPx()
    val ay = y + bob

    // Tallo marrón
    drawRect(color = Color(0xFF8D6E63), topLeft = Offset(x + 5 * p, ay + 1 * p), size = Size(p, 2 * p))
    // Hoja verde pixelada
    drawRect(color = Color(0xFF4CAF50), topLeft = Offset(x + 6 * p, ay + 1 * p), size = Size(2 * p, p))

    // Manzana roja principal
    drawRect(color = Color(0xFFE53935), topLeft = Offset(x + 2 * p, ay + 3 * p), size = Size(6 * p, 6 * p))
    drawRect(color = Color(0xFFD32F2F), topLeft = Offset(x + 1 * p, ay + 4 * p), size = Size(8 * p, 4 * p))

    // Brillo blanco pixel-art en esquina superior izquierda
    drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(x + 3 * p, ay + 4 * p), size = Size(p, p))

    // Borde sombreado oscuro 8-bit
    drawRect(color = Color(0xFF8E0000), topLeft = Offset(x + 2 * p, ay + 8 * p), size = Size(6 * p, p))
    drawRect(color = Color(0xFF8E0000), topLeft = Offset(x + 7 * p, ay + 4 * p), size = Size(p, 4 * p))
}

private fun DrawScope.drawPixelSnakeHead(x: Float, y: Float, size: Float, direction: Pair<Int, Int>) {
    val p = size / 10f

    // Base de la cabeza (Verde brillante)
    drawRect(color = Color(0xFF4ADE80), topLeft = Offset(x + p, y + p), size = Size(8 * p, 8 * p))
    // Sombra interior
    drawRect(color = Color(0xFF16A34A), topLeft = Offset(x + 2 * p, y + 2 * p), size = Size(6 * p, 6 * p))
    // Highlight superior
    drawRect(color = Color(0xFF86EFAC), topLeft = Offset(x + 2 * p, y + 2 * p), size = Size(4 * p, 2 * p))
    // Borde oscuro pixelado
    drawRect(color = Color(0xFF064E3B), topLeft = Offset(x, y), size = Size(size, size), style = Stroke(1.5.dp.toPx()))

    // Ojitos pixel-art grandes y expresivos
    val eyeSize = 2.5f * p
    val pupilSize = 1.2f * p

    val (eye1, eye2) = when (direction) {
        0 to -1 -> Offset(x + 2 * p, y + 2 * p) to Offset(x + 6 * p, y + 2 * p)
        0 to 1 -> Offset(x + 2 * p, y + 6 * p) to Offset(x + 6 * p, y + 6 * p)
        -1 to 0 -> Offset(x + 2 * p, y + 2 * p) to Offset(x + 2 * p, y + 6 * p)
        else -> Offset(x + 6 * p, y + 2 * p) to Offset(x + 6 * p, y + 6 * p)
    }

    // Fondo blanco del ojo
    drawRect(color = Color.White, topLeft = eye1, size = Size(eyeSize, eyeSize))
    drawRect(color = Color.White, topLeft = eye2, size = Size(eyeSize, eyeSize))

    // Pupila negra que mira al frente
    drawRect(color = Color.Black, topLeft = eye1 + Offset(0.6f * p, 0.6f * p), size = Size(pupilSize, pupilSize))
    drawRect(color = Color.Black, topLeft = eye2 + Offset(0.6f * p, 0.6f * p), size = Size(pupilSize, pupilSize))
}

private fun DrawScope.drawPixelSnakeBody(x: Float, y: Float, size: Float, isTail: Boolean) {
    val p = size / 10f

    val bodyColor = Color(0xFF22C55E)
    val highlightColor = Color(0xFF86EFAC)
    val shadowColor = Color(0xFF15803D)
    val borderColor = Color(0xFF064E3B)

    val inset = if (isTail) 2f * p else p
    val segSize = size - inset * 2f

    // Relleno principal
    drawRect(color = bodyColor, topLeft = Offset(x + inset, y + inset), size = Size(segSize, segSize))
    // Escama central / Brillo
    drawRect(color = highlightColor, topLeft = Offset(x + inset + p, y + inset + p), size = Size(2 * p, 2 * p))
    // Sombra inferior
    drawRect(color = shadowColor, topLeft = Offset(x + inset + 2 * p, y + inset + 4 * p), size = Size(3 * p, 2 * p))
    // Borde exterior
    drawRect(color = borderColor, topLeft = Offset(x + inset, y + inset), size = Size(segSize, segSize), style = Stroke(1.5.dp.toPx()))
}
