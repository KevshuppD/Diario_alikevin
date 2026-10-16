package calendario.kevshupp.diariokevinali.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.MainActivity
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.PetVoucher
import calendario.kevshupp.diariokevinali.TogetherDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// value = "valor" en ❤️ del vale: solo define qué tan seguido sale en la ruleta (no se pueden comprar)
private data class VoucherOption(val emoji: String, val title: String, val value: Int, val spicy: Boolean = false)

// Premios de la ruleta (constante a nivel superior)
private val VOUCHER_CATALOG = listOf(
    VoucherOption("🤗", "Abrazo de 1 minuto", 15),
    VoucherOption("💋", "10 besitos seguidos", 20),
    VoucherOption("🎮", "Partida al juego que yo elija", 25),
    VoucherOption("🎬", "Yo elijo la película", 30),
    VoucherOption("😴", "Siesta sin interrupciones", 30),
    VoucherOption("📺", "Yo elijo el próximo anime", 40),
    VoucherOption("🍽️", "Me libro de lavar la loza", 40),
    VoucherOption("💆", "Masaje de 15 minutos", 60),
    VoucherOption("🧹", "Haces una tarea de la casa por mí", 60),
    VoucherOption("🍕", "Tú invitas el antojo", 70),
    VoucherOption("🥞", "Desayuno en la cama", 80),
    VoucherOption("🌹", "Cita sorpresa organizada por ti", 150)
)

// "Nada": trozo sin premio (se cobra el giro igual). Con value = 12 sale ~20 % de las veces.
private val ROULETTE_NOTHING = VoucherOption("😢", "Nada", 12)

// "Otra vez": la ruleta vuelve a girar sola y gratis; solo se cobra y guarda el resultado final (0 escrituras extra)
private val ROULETTE_AGAIN = VoucherOption("🔁", "Otra vez", 25)
private const val ROULETTE_MAX_REPEATS = 5

// Trozos de la ruleta normal en orden de dibujo: "Nada" y "Otra vez" van separados entre los premios
private val ROULETTE_SEGMENTS: List<VoucherOption> =
    VOUCHER_CATALOG.take(3) + ROULETTE_AGAIN + VOUCHER_CATALOG.subList(3, 6) + ROULETTE_NOTHING + VOUCHER_CATALOG.drop(6)

// Premios de la ruleta Juntos.
// PROVISIONALES: el usuario va a definir las recompensas reales; reemplazar esta lista cuando las entregue.
private val TOGETHER_CATALOG = listOf(
    VoucherOption("📸", "Foto juntos en un lugar bonito", 20),
    VoucherOption("💃", "Bailar una canción juntos", 25),
    VoucherOption("🌅", "Paseo de 30 minutos", 35),
    VoucherOption("🍦", "Helado juntos", 40),
    VoucherOption("🍿", "Maratón de película o anime", 50),
    VoucherOption("🎳", "Salida a elegir", 90),
    VoucherOption("🍝", "Cena especial", 120),
    VoucherOption("💝", "Regalo sorpresa", 200),
    // Picantes 🌶️ (solo para la ruleta Juntos)
    VoucherOption("💋", "Beso apasionado de 1 minuto", 25, spicy = true),
    VoucherOption("😘", "Besos donde yo elija", 45, spicy = true),
    VoucherOption("💆", "Masaje con aceite sin apuro", 60, spicy = true),
    VoucherOption("🛁", "Ducha o baño juntos", 70, spicy = true),
    VoucherOption("👗", "Tú eliges lo que me pongo esta noche", 80, spicy = true),
    VoucherOption("🎲", "Esta noche mando yo", 100, spicy = true),
    VoucherOption("🌶️", "Cumplo un deseo picante tuyo", 130, spicy = true),
    VoucherOption("🔥", "Noche especial sin límite de tiempo", 180, spicy = true)
)
// Sin "Nada": la ruleta juntos siempre premia (salvo "Otra vez", que vuelve a girar)
private val TOGETHER_SEGMENTS: List<VoucherOption> = run {
    val normal = TOGETHER_CATALOG.filter { !it.spicy }
    val spicy = TOGETHER_CATALOG.filter { it.spicy }
    val mixed = ArrayList<VoucherOption>()
    for (i in 0 until maxOf(normal.size, spicy.size)) {
        normal.getOrNull(i)?.let { mixed.add(it) }
        spicy.getOrNull(i)?.let { mixed.add(it) }
        if (i == 3) mixed.add(ROULETTE_AGAIN)
    }
    mixed
}

private val ROULETTE_COLORS = listOf(
    Color(0xFFEC4899), Color(0xFFD97706), Color(0xFF2563EB), Color(0xFF16A34A),
    Color(0xFF9333EA), Color(0xFFF59E0B), Color(0xFF0EA5E9), Color(0xFFE11D48)
)
private val TOGETHER_COLORS = listOf(
    Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFBE185D), Color(0xFFFB7185),
    Color(0xFFDB2777), Color(0xFFF9A8D4)
)

/**
 * Geometría precalculada de una ruleta (se crea una vez a nivel superior).
 * Cada trozo ocupa un ángulo proporcional a 1/valor: los más valiosos salen menos y el dibujo muestra la probabilidad real.
 */
private class RouletteWheel(val segments: List<VoucherOption>, val colors: List<Color>) {
    val weights = FloatArray(segments.size) { 1f / segments[it].value }
    private val totalWeight = weights.sum()
    val sweeps = FloatArray(segments.size) { weights[it] / totalWeight * 360f }
    // Ángulo de inicio de cada trozo, medido desde arriba en sentido horario
    val starts = FloatArray(segments.size).also { arr ->
        var acc = 0f
        for (i in arr.indices) { arr[i] = acc; acc += sweeps[i] }
    }
    val oddsText: String = segments.indices.joinToString("\n") { i ->
        "${segments[i].emoji} ${segments[i].title}: ${"%.1f".format(weights[i] / totalWeight * 100f)} %"
    }

    // Trozo bajo el puntero para una rotación dada (sin asignaciones: se llama en cada frame del giro)
    fun segmentAt(rotation: Float): Int {
        val angle = ((360f - rotation % 360f) % 360f + 360f) % 360f
        for (i in starts.indices.reversed()) {
            if (angle >= starts[i]) return i
        }
        return 0
    }

    fun pick(): Int {
        var r = kotlin.random.Random.nextFloat() * totalWeight
        for (i in weights.indices) {
            r -= weights[i]
            if (r <= 0f) return i
        }
        return weights.lastIndex
    }
}

private val NORMAL_WHEEL = RouletteWheel(ROULETTE_SEGMENTS, ROULETTE_COLORS)
private val TOGETHER_WHEEL = RouletteWheel(TOGETHER_SEGMENTS, TOGETHER_COLORS)

@Composable
fun PetVouchersContent(
    pet: Pet,
    isDark: Boolean,
    borderColor: Color,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current.findMainActivity()
    val myUserId = activity?.viewModel?.currentUserId ?: ""
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val cardBg = if (isDark) Color(0xFF222222) else Color(0xFFF9F5EC)

    val isKevin = activity?.viewModel?.isCurrentUserKevin() ?: true
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val spinsToday = pet.getRouletteSpinsToday(isKevin, today)

    // Estado de "¿están juntos?" compartido entre la ruleta Juntos y los vales Juntos pendientes
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var togetherChecking by remember { mutableStateOf(false) }
    var togetherUnlocked by remember { mutableStateOf(activity?.viewModel?.isTogetherStillValid() == true) }
    var togetherMessage by remember { mutableStateOf<String?>(null) }
    val runTogetherCheck: () -> Unit = {
        if (!togetherChecking) {
            togetherChecking = true
            togetherMessage = "📡 Buscando a los dos en el radar... (hasta 30 s)"
            scope.launch {
                val result = TogetherDetector.check(context)
                togetherChecking = false
                togetherMessage = when (result) {
                    is TogetherDetector.Result.Together -> {
                        activity?.viewModel?.markTogetherVerified()
                        togetherUnlocked = true
                        null
                    }
                    is TogetherDetector.Result.Apart -> "😢 Están a ${formatDistance(result.distanceM)}. ¡Tienen que estar juntos!"
                    TogetherDetector.Result.NoPermission -> "📍 Activa el permiso de ubicación para usar esta ruleta."
                    TogetherDetector.Result.NoMyLocation -> "📍 No se pudo obtener tu ubicación. Revisa que el GPS esté encendido."
                    TogetherDetector.Result.PartnerNoResponse -> "📡 Tu pareja no respondió. Revisa que tenga el radar y la ubicación activados."
                }
                togetherMessage?.let { activity?.showStyledPixelToast(it) }
            }
        }
    }

    val pending = pet.vouchers.filter { it.status == PetVoucher.STATUS_PENDING }.sortedByDescending { it.createdAt }
    val done = pet.vouchers.filter { it.status == PetVoucher.STATUS_DONE }.sortedByDescending { it.doneAt }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🎟️ VALES DE PAREJA 🎟️",
            fontFamily = Vt323,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Puntos compartidos: ${pet.lovePoints} ❤️\nGira la ruleta: el vale que salga te lo debe cumplir tu pareja.",
            fontFamily = Vt323,
            fontSize = 16.sp,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // --- RULETA NORMAL ---
        val normalLeft = (Pet.ROULETTE_SPINS_PER_DAY - spinsToday).coerceAtLeast(0)
        val isExtraSpin = normalLeft == 0
        val spinCost = Pet.rouletteSpinCost(spinsToday)
        val noSpinsLeft = spinsToday >= Pet.ROULETTE_MAX_SPINS_PER_DAY
        VoucherRoulette(
            wheel = NORMAL_WHEEL,
            title = "🎡 RULETA DE VALES",
            canSpin = !noSpinsLeft && pet.lovePoints >= spinCost,
            idleText = when {
                noSpinsLeft -> "Ya usaste todos tus giros de hoy (incluido el extra). ¡Vuelve mañana!"
                pet.lovePoints < spinCost -> "Necesitan $spinCost ❤️ para girar"
                isExtraSpin -> "Ya usaste tus ${Pet.ROULETTE_SPINS_PER_DAY} giros normales. Queda 1 giro extra por $spinCost ❤️"
                else -> "Gira por $spinCost ❤️ y gana un vale al azar"
            },
            buttonText = if (isExtraSpin) "⭐ GIRO EXTRA ($spinCost ❤️)" else "🎡 GIRAR ($spinCost ❤️) • $normalLeft/${Pet.ROULETTE_SPINS_PER_DAY} hoy",
            buttonColor = if (isExtraSpin) Color(0xFFD97706) else accentColor,
            cardBg = cardBg,
            borderColor = borderColor,
            contentColor = contentColor,
            accentColor = accentColor,
            onResult = { option ->
                activity?.saveRouletteSpin(if (option === ROULETTE_NOTHING) null else option.title, option.emoji)
            }
        )

        // --- RULETA JUNTOS ---
        TogetherRouletteSection(
            pet = pet,
            isKevin = isKevin,
            today = today,
            unlocked = togetherUnlocked,
            checking = togetherChecking,
            message = togetherMessage,
            onCheck = runTogetherCheck,
            cardBg = cardBg,
            borderColor = borderColor,
            contentColor = contentColor,
            accentColor = accentColor
        )

        // --- PENDIENTES ---
        if (pending.isNotEmpty()) {
            VoucherSectionTitle("⏳ PENDIENTES (${pending.size})", accentColor)
            pending.forEach { v ->
                val iBought = v.buyerId == myUserId
                val lockedUntilTogether = v.together && !togetherUnlocked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RectangleShape)
                        .border(2.dp, if (iBought) borderColor else Color(0xFFEC4899))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(v.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.title, fontFamily = Vt323, fontSize = 18.sp, color = contentColor, fontWeight = FontWeight.Bold)
                        if (v.together) {
                            Text(
                                text = if (v.spicy) "💞🌶️ Vale juntos picante: solo se cumple estando juntos" else "💞 Vale juntos: solo se cumple estando juntos",
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = Color(0xFFEC4899)
                            )
                        }
                        Text(
                            text = if (iBought) "Tu pareja te lo debe • ${dateFormat.format(Date(v.createdAt))}"
                            else "¡Se lo debes a ${v.buyerName}! • ${dateFormat.format(Date(v.createdAt))}",
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    lockedUntilTogether && togetherChecking -> Color.Gray
                                    lockedUntilTogether -> Color(0xFFEC4899)
                                    else -> Color(0xFF16A34A)
                                }
                            )
                            .border(2.dp, Color.Black)
                            .clickable(enabled = !(lockedUntilTogether && togetherChecking)) {
                                if (lockedUntilTogether) runTogetherCheck() else activity?.completeVoucher(v.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                lockedUntilTogether && togetherChecking -> "..."
                                lockedUntilTogether -> "📍 ¿JUNTOS?"
                                else -> "✓ CUMPLIDO"
                            },
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (pending.isEmpty() && done.isEmpty()) {
            Text(
                "Aún no tienen vales. ¡Giren la ruleta! 🎡",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- HISTORIAL ---
        if (done.isNotEmpty()) {
            VoucherSectionTitle("💖 CUMPLIDOS", accentColor)
            done.forEach { v ->
                Text(
                    text = "${v.emoji} ${v.title} — de ${v.buyerName} • ${dateFormat.format(Date(v.doneAt))}",
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun VoucherSectionTitle(text: String, color: Color) {
    Text(text, fontFamily = Vt323, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun TogetherRouletteSection(
    pet: Pet,
    isKevin: Boolean,
    today: String,
    unlocked: Boolean,
    checking: Boolean,
    message: String?,
    onCheck: () -> Unit,
    cardBg: Color,
    borderColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    val activity = LocalContext.current.findMainActivity()
    val pink = Color(0xFFEC4899)

    if (!unlocked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg, RectangleShape)
                .border(2.dp, pink)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoucherSectionTitle("💞 RULETA JUNTOS", pink)
            Text(
                "Una ruleta especial con premios distintos, solo cuando el radar confirma que están en el mismo lugar.",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            message?.let {
                Text(it, fontFamily = Vt323, fontSize = 16.sp, color = contentColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (checking) Color.Gray else pink)
                    .border(2.dp, Color.Black)
                    .clickable(enabled = !checking) { onCheck() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (checking) "Comprobando..." else "📍 ¿ESTAMOS JUNTOS?", fontFamily = Vt323, fontSize = 19.sp, color = Color.White)
            }
        }
        return
    }

    val togetherSpins = pet.getTogetherSpinsToday(isKevin, today)
    val noneLeft = togetherSpins >= Pet.TOGETHER_SPINS_PER_DAY
    val cost = Pet.TOGETHER_SPIN_COST
    VoucherRoulette(
        wheel = TOGETHER_WHEEL,
        title = "💞 RULETA JUNTOS",
        canSpin = !noneLeft && pet.lovePoints >= cost,
        idleText = when {
            noneLeft -> "Ya usaste tus ${Pet.TOGETHER_SPINS_PER_DAY} giros juntos de hoy. ¡Mañana hay más!"
            pet.lovePoints < cost -> "Necesitan $cost ❤️ para girar"
            else -> "¡Están juntos! 💞 Gira por $cost ❤️ (aparte de la ruleta normal)"
        },
        buttonText = "💞 GIRAR JUNTOS ($cost ❤️) • ${(Pet.TOGETHER_SPINS_PER_DAY - togetherSpins).coerceAtLeast(0)}/${Pet.TOGETHER_SPINS_PER_DAY} hoy",
        buttonColor = pink,
        cardBg = cardBg,
        borderColor = pink,
        contentColor = contentColor,
        accentColor = pink,
        onResult = { option -> activity?.saveTogetherSpin(option.title, option.emoji, option.spicy) }
    )
}

private fun formatDistance(meters: Int): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000f) else "$meters m"

@Composable
private fun VoucherRoulette(
    wheel: RouletteWheel,
    title: String,
    canSpin: Boolean,
    idleText: String,
    buttonText: String,
    buttonColor: Color,
    cardBg: Color,
    borderColor: Color,
    contentColor: Color,
    accentColor: Color,
    onResult: (VoucherOption) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("roulette_sound", true)) }
    // init es idempotente y vuelve a cargar si un minijuego liberó el motor
    LaunchedEffect(Unit) { RetroGameAudioEngine.init(context.applicationContext) }
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VoucherOption?>(null) }
    val density = LocalDensity.current
    val emojiPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = with(density) { 20.sp.toPx() }
        }
    }
    val smallEmojiPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
        }
    }
    var showOdds by remember { mutableStateOf(false) }
    val enabled = canSpin && !spinning

    // Tick cada vez que una línea entre trozos pasa por el puntero (máx. 1 cada 35 ms para que no se sature)
    LaunchedEffect(soundOn) {
        var lastSegment = wheel.segmentAt(rotation.value)
        var lastTickAt = 0L
        snapshotFlow { rotation.value }.collect { r ->
            val seg = wheel.segmentAt(r)
            if (seg != lastSegment) {
                lastSegment = seg
                val now = System.currentTimeMillis()
                if (rotation.isRunning && now - lastTickAt >= 35) {
                    lastTickAt = now
                    RetroGameAudioEngine.playRouletteTick(soundOn)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RectangleShape)
            .border(2.dp, borderColor)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { VoucherSectionTitle(title, accentColor) }
            Text(
                text = if (soundOn) "🔊" else "🔇",
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable {
                        soundOn = !soundOn
                        prefs.edit().putBoolean("roulette_sound", soundOn).apply()
                    }
                    .padding(4.dp)
            )
        }

        Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.TopCenter) {
            Canvas(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(210.dp)
                    // Rotación en la fase de dibujo: sin recomposiciones por frame
                    .graphicsLayer { rotationZ = rotation.value }
            ) {
                val radius = size.minDimension / 2f
                for (i in wheel.segments.indices) {
                    drawArc(
                        color = wheel.colors[i % wheel.colors.size],
                        startAngle = wheel.starts[i] - 90f,
                        sweepAngle = wheel.sweeps[i],
                        useCenter = true
                    )
                    drawArc(
                        color = Color.Black,
                        startAngle = wheel.starts[i] - 90f,
                        sweepAngle = wheel.sweeps[i],
                        useCenter = true,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Emoji grande en trozos anchos, chico cerca del borde en los medianos, nada en los angostos
                    val sweep = wheel.sweeps[i]
                    if (sweep >= 8f) {
                        val paint = if (sweep >= 14f) emojiPaint else smallEmojiPaint
                        val dist = if (sweep >= 14f) 0.66f else 0.82f
                        val mid = Math.toRadians((wheel.starts[i] + sweep / 2f - 90f).toDouble())
                        val x = center.x + (radius * dist * cos(mid)).toFloat()
                        val y = center.y + (radius * dist * sin(mid)).toFloat() + paint.textSize / 3f
                        drawContext.canvas.nativeCanvas.drawText(wheel.segments[i].emoji, x, y, paint)
                    }
                }
                drawCircle(Color.Black, radius = radius, style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color.White, radius = radius * 0.12f)
                drawCircle(Color.Black, radius = radius * 0.12f, style = Stroke(width = 2.dp.toPx()))
            }
            // Puntero fijo arriba
            Text("▼", fontSize = 26.sp, color = Color.Black, fontFamily = Vt323)
        }

        Text(
            text = when {
                spinning && result == null -> "Girando..."
                result === ROULETTE_AGAIN -> "🔁 ¡Otra vez! Gira gratis..."
                result === ROULETTE_NOTHING -> "😢 Esta vez no salió nada..."
                result != null -> "🎉 ¡Salió: ${result!!.emoji} ${result!!.title}!"
                else -> idleText
            },
            fontFamily = Vt323,
            fontSize = 17.sp,
            color = contentColor,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) buttonColor else Color.Gray)
                .border(2.dp, Color.Black)
                .clickable(enabled = enabled) {
                    spinning = true
                    result = null
                    scope.launch {
                        var option: VoucherOption
                        var repeats = 0
                        while (true) {
                            var index = wheel.pick()
                            // Tope de repeticiones seguidas por si la suerte insiste
                            while (repeats >= ROULETTE_MAX_REPEATS && wheel.segments[index] === ROULETTE_AGAIN) index = wheel.pick()
                            option = wheel.segments[index]
                            // Dejar el trozo ganador (con algo de azar dentro del trozo) bajo el puntero
                            val inside = wheel.sweeps[index] * (0.2f + 0.6f * kotlin.random.Random.nextFloat())
                            val targetAngle = wheel.starts[index] + inside
                            val current = rotation.value % 360f
                            val delta = ((360f - targetAngle) - current + 720f) % 360f
                            val turns = if (repeats == 0) 5 else 3
                            rotation.animateTo(
                                targetValue = rotation.value + 360f * turns + delta,
                                animationSpec = tween(durationMillis = if (repeats == 0) 3800 else 3000, easing = FastOutSlowInEasing)
                            )
                            if (option !== ROULETTE_AGAIN) break
                            repeats++
                            result = ROULETTE_AGAIN
                            RetroGameAudioEngine.playRouletteAgain(soundOn)
                            delay(1100)
                            result = null
                        }
                        result = option
                        spinning = false
                        if (option === ROULETTE_NOTHING) RetroGameAudioEngine.playRouletteLose(soundOn)
                        else RetroGameAudioEngine.playRouletteWin(soundOn)
                        onResult(option)
                    }
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonText, fontFamily = Vt323, fontSize = 19.sp, color = Color.White)
        }

        Text(
            text = if (showOdds) "▲ Ocultar probabilidades" else "▼ Ver probabilidades",
            fontFamily = Vt323,
            fontSize = 15.sp,
            color = accentColor,
            modifier = Modifier.clickable { showOdds = !showOdds }
        )
        if (showOdds) {
            Text(
                text = wheel.oddsText,
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
