package calendario.kevshupp.diariokevinali.compose

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import calendario.kevshupp.diariokevinali.R
import coil.imageLoader
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val IMG_W = 1080
private const val IMG_H = 1350

private val CELEBRATION_DESIGNS = listOf(
    "fuegos" to "Fuegos",
    "trofeo" to "Trofeo",
    "arcade" to "Arcade"
)

private val CONFETTI_COLORS = intArrayOf(
    0xFFEC4899.toInt(), 0xFF2563EB.toInt(), 0xFFFFFFFF.toInt(), 0xFF10B981.toInt(), 0xFF7C3AED.toInt()
)
private val SPARK_COLORS = intArrayOf(
    0xFFFBBF24.toInt(), 0xFFEC4899.toInt(), 0xFF60A5FA.toInt(), 0xFF34D399.toInt(), 0xFFFFFFFF.toInt(), 0xFFF97316.toInt()
)
private val LIGHT_COLORS = intArrayOf(
    0xFFEF4444.toInt(), 0xFFFBBF24.toInt(), 0xFF22C55E.toInt(), 0xFF3B82F6.toInt(), 0xFFEC4899.toInt()
)

private val TROPHY_PIXELS = arrayOf(
    "...KKKKKKKKKKK...",
    "...KGGGGGGGGGK...",
    "KKKKGLLGGGGGGKKKK",
    "KGGKGLLGGGGGGKGGK",
    "KGGKGLLGGGGGGKGGK",
    "KGGKGLLGGGGGGKGGK",
    "KGGKGLLGGGGGGKGGK",
    "KKKKGLLGGGGGGKKKK",
    ".KKKGGGGGGGGGKKK.",
    "...KKGGGGGGGKK...",
    "....KKKGGGKKK....",
    "......KGGGK......",
    "......KGGGK......",
    "......KGGGK......",
    ".....KKGGGKK.....",
    "....KGGGGGGGK....",
    "...KKKKKKKKKKK...",
    "...KRRRRRRRRRK...",
    "...KRRRRRRRRRK...",
    "...KKKKKKKKKKK..."
)

/** Datos de la imagen: todos los números salen de la temporada real, no están fijos. */
private data class CelebrationData(
    val spirits: List<Bitmap?>,
    val familyCount: Int,
    val season: Int,
    val date: String
)

/** Reparte [total] en filas o anillos conservando la proporción de [weights]. */
private fun distribute(total: Int, weights: IntArray): IntArray {
    val sum = weights.sum()
    val result = IntArray(weights.size) { weights[it] * total / sum }
    result[result.lastIndex] += total - result.sum()
    return result
}

private fun textPaint(typeface: android.graphics.Typeface?, size: Float, color: Int, align: Paint.Align = Paint.Align.CENTER) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
        textAlign = align
    }

private fun Canvas.shadowText(text: String, x: Float, y: Float, paint: Paint, shadow: Int, offset: Float) {
    val original = paint.color
    paint.color = shadow
    drawText(text, x + offset, y + offset, paint)
    paint.color = original
    drawText(text, x, y, paint)
}

private fun Canvas.drawSpirit(bitmap: Bitmap?, left: Float, top: Float, size: Float, paint: Paint) {
    if (bitmap == null) return
    drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), RectF(left, top, left + size, top + size), paint)
}

private fun Canvas.confetti(random: Random, count: Int, maxY: Int, colors: IntArray, paint: Paint) {
    repeat(count) { i ->
        val x = random.nextInt(IMG_W - 20).toFloat()
        val y = random.nextInt(maxY).toFloat()
        val w = 8f + random.nextInt(2) * 8f
        val h = 8f + random.nextInt(3) * 8f
        paint.color = colors[i % colors.size]
        drawRect(x, y, x + w, y + h, paint)
    }
}

private fun drawFuegos(canvas: Canvas, data: CelebrationData, typeface: android.graphics.Typeface?) {
    canvas.drawColor(0xFF0B1238.toInt())
    val fill = Paint()
    val spiritPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val random = Random(7)
    val cx = IMG_W / 2f
    val cy = IMG_H / 2f
    repeat(150) { i ->
        val t = random.nextDouble() * Math.PI * 2
        val r = 250 + random.nextDouble() * 400
        val x = (cx + cos(t) * r * 0.86).toFloat()
        val y = (cy + sin(t) * r * 1.14).toFloat()
        val s = 6f + random.nextInt(3) * 4f
        fill.color = SPARK_COLORS[i % SPARK_COLORS.size]
        canvas.drawRect(x, y, x + s, y + s, fill)
    }
    val total = data.spirits.size
    val scale = min(1.2f, sqrt(162f / max(total, 1)))
    val radii = floatArrayOf(290f, 360f, 430f, 500f, 565f)
    val sizes = floatArrayOf(76f, 68f, 62f, 56f, 50f)
    val counts = distribute(total, intArrayOf(22, 28, 34, 38, 40))
    var n = 0
    counts.forEachIndexed { ring, count ->
        val s = sizes[ring] * scale
        for (j in 0 until count) {
            val t = (j + ring * 0.5) / count * Math.PI * 2
            val x = (cx + cos(t) * radii[ring] * 0.86 - s / 2).toFloat()
            val y = (cy + sin(t) * radii[ring] * 1.14 - s / 2).toFloat()
            canvas.drawSpirit(data.spirits[n++], x, y, s, spiritPaint)
        }
    }
    canvas.drawText("KEVIN & ALI", cx, 535f, textPaint(typeface, 54f, 0xFFFDE68A.toInt()))
    val title = textPaint(typeface, 104f, 0xFFFBBF24.toInt())
    canvas.shadowText("¡LO", cx, 625f, title, 0xFFBE185D.toInt(), 5f)
    canvas.shadowText("LOGRAMOS!", cx, 715f, title, 0xFFBE185D.toInt(), 5f)
    canvas.drawText("TEMPORADA ${data.season} COMPLETA", cx, 775f, textPaint(typeface, 44f, 0xFFFFFFFF.toInt()))
    val stat = textPaint(typeface, 40f, 0xFFF9A8D4.toInt())
    canvas.drawText("$total espíritus", cx, 825f, stat)
    canvas.drawText("$total maestrías", cx, 865f, stat)
    canvas.drawText(data.date, cx, 905f, textPaint(typeface, 32f, 0xFF93C5FD.toInt()))
}

private fun drawNamePlate(canvas: Canvas, left: Float, color: Int, name: String, total: Int, typeface: android.graphics.Typeface?) {
    val fill = Paint()
    fill.color = 0xFF1A1A1A.toInt()
    canvas.drawRect(left + 8, 408f, left + 308, 538f, fill)
    canvas.drawRect(left, 400f, left + 300, 530f, fill)
    fill.color = color
    canvas.drawRect(left + 5, 405f, left + 295, 525f, fill)
    canvas.drawText(name, left + 150, 462f, textPaint(typeface, 60f, 0xFFFFFFFF.toInt()))
    canvas.drawText("$total · $total coronas", left + 150, 505f, textPaint(typeface, 34f, 0xFFFFFFFF.toInt()))
}

private fun drawTrofeo(canvas: Canvas, data: CelebrationData, typeface: android.graphics.Typeface?) {
    canvas.drawColor(0xFFFBBF24.toInt())
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val spiritPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val cx = IMG_W / 2f
    val cy = IMG_H * 0.45f
    fill.color = 0xFFF97316.toInt()
    val ray = 1800f
    for (i in 0 until 15) {
        val a0 = Math.toRadians(i * 24.0 + 12.0)
        val a1 = Math.toRadians(i * 24.0 + 24.0)
        val path = Path()
        path.moveTo(cx, cy)
        path.lineTo((cx + cos(a0) * ray).toFloat(), (cy + sin(a0) * ray).toFloat())
        path.lineTo((cx + cos(a1) * ray).toFloat(), (cy + sin(a1) * ray).toFloat())
        path.close()
        canvas.drawPath(path, fill)
    }
    val rectPaint = Paint()
    canvas.confetti(Random(11), 110, 720, CONFETTI_COLORS, rectPaint)

    val title = textPaint(typeface, 170f, 0xFFFFFFFF.toInt())
    canvas.shadowText("¡TEMPORADA", cx, 140f, title, 0xFFBE185D.toInt(), 7f)
    canvas.shadowText("COMPLETA!", cx, 265f, title, 0xFFBE185D.toInt(), 7f)

    val pixel = 20f
    val trophyLeft = cx - 8.5f * pixel
    TROPHY_PIXELS.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            val color = when (ch) {
                'K' -> 0xFF1A1A1A.toInt()
                'G' -> 0xFFFBBF24.toInt()
                'L' -> 0xFFFDE68A.toInt()
                'R' -> 0xFFBE185D.toInt()
                else -> 0
            }
            if (color != 0) {
                rectPaint.color = color
                canvas.drawRect(trophyLeft + x * pixel, 275f + y * pixel, trophyLeft + (x + 1) * pixel, 275f + (y + 1) * pixel, rectPaint)
            }
        }
    }
    val total = data.spirits.size
    drawNamePlate(canvas, 40f, 0xFF1D4ED8.toInt(), "KEVIN", total, typeface)
    drawNamePlate(canvas, IMG_W - 340f, 0xFFBE185D.toInt(), "ALI", total, typeface)

    // Montaña de espíritus: cada fila es más ancha y queda delante de la anterior
    val counts = distribute(total, intArrayOf(10, 12, 14, 16, 18, 20, 22, 24, 26))
    val scale = min(1.2f, sqrt(162f / max(total, 1)))
    var n = 0
    counts.forEachIndexed { row, count ->
        if (count > 0) {
            val z = (90f + row * 4) * scale
            val gap = 1040f / count
            val y = 660f + row * 70
            for (j in 0 until count) {
                canvas.drawSpirit(data.spirits[n++], cx + (j - (count - 1) / 2f) * gap - z / 2, y, z, spiritPaint)
            }
        }
    }
}

private fun drawArcade(canvas: Canvas, data: CelebrationData, typeface: android.graphics.Typeface?) {
    canvas.drawColor(0xFF120024.toInt())
    val fill = Paint()
    val spiritPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val total = data.spirits.size
    // Marco de 2 casillas de grosor: se achica la casilla hasta que quepan todos los espíritus
    var columns = 12
    var rows = (IMG_H.toFloat() * columns / IMG_W).roundToInt()
    while (columns * rows - (columns - 4) * (rows - 4) < total) {
        columns++
        rows = (IMG_H.toFloat() * columns / IMG_W).roundToInt()
    }
    val cellW = IMG_W.toFloat() / columns
    val cellH = IMG_H.toFloat() / rows
    var n = 0
    var slot = 0
    for (r in 0 until rows) for (c in 0 until columns) {
        if (r < 2 || r >= rows - 2 || c < 2 || c >= columns - 2) {
            val x = c * cellW
            val y = r * cellH
            fill.color = LIGHT_COLORS[slot++ % LIGHT_COLORS.size]
            canvas.drawRect(x, y, x + cellW, y + cellH, fill)
            if (n < total) canvas.drawSpirit(data.spirits[n++], x, y, min(cellW, cellH), spiritPaint)
        }
    }
    val cx = IMG_W / 2f
    val title = textPaint(typeface, 150f, 0xFFFBBF24.toInt())
    canvas.drawText("TEMPORADA", cx + 12, 302f, title.apply { color = 0xFF2563EB.toInt() })
    canvas.drawText("SUPERADA", cx + 12, 432f, title)
    title.color = 0xFFFBBF24.toInt()
    canvas.shadowText("TEMPORADA", cx, 290f, title, 0xFFEC4899.toInt(), 6f)
    canvas.shadowText("SUPERADA", cx, 420f, title, 0xFFEC4899.toInt(), 6f)
    canvas.drawText("¡100% DE LA COLECCIÓN!", cx, 510f, textPaint(typeface, 66f, 0xFF34D399.toInt()))

    val left = 170f
    val right = IMG_W - 170f
    val rowsText = listOf(
        Triple("KEVIN ESPÍRITUS", "$total/$total", 0xFF60A5FA.toInt()),
        Triple("KEVIN MAESTRÍAS", "$total/$total", 0xFF60A5FA.toInt()),
        Triple("ALI ESPÍRITUS", "$total/$total", 0xFFF472B6.toInt()),
        Triple("ALI MAESTRÍAS", "$total/$total", 0xFFF472B6.toInt())
    )
    val valuePaint = textPaint(typeface, 56f, 0xFFFFFFFF.toInt(), Paint.Align.RIGHT)
    var y = 590f
    rowsText.forEach { (label, value, color) ->
        canvas.drawText(label, left, y, textPaint(typeface, 56f, color, Paint.Align.LEFT))
        canvas.drawText(value, right, y, valuePaint)
        y += 62f
    }
    fill.color = 0xFFFBBF24.toInt()
    var dash = left
    while (dash < right) {
        canvas.drawRect(dash, y - 40f, min(dash + 18f, right), y - 34f, fill)
        dash += 30f
    }
    y += 16f
    canvas.drawText("FAMILIAS COMPLETAS", left, y, textPaint(typeface, 56f, 0xFFFBBF24.toInt(), Paint.Align.LEFT))
    canvas.drawText("${data.familyCount}/${data.familyCount}", right, y, textPaint(typeface, 56f, 0xFFFBBF24.toInt(), Paint.Align.RIGHT))

    fill.color = 0xFFEC4899.toInt()
    canvas.drawRect(298f, 908f, 798f, 1088f, fill)
    fill.color = 0xFFFBBF24.toInt()
    canvas.drawRect(290f, 900f, 790f, 1080f, fill)
    canvas.drawText("KEVIN & ALI", 540f, 975f, textPaint(typeface, 80f, 0xFF120024.toInt()))
    canvas.drawText("¡PERFECT!", 540f, 1055f, textPaint(typeface, 80f, 0xFF120024.toInt()))
    canvas.drawText("TEMPORADA ${data.season} · ${data.date}", cx, 1140f, textPaint(typeface, 42f, 0xFFD8B4FE.toInt()))
}

private fun renderCelebration(context: Context, design: String, data: CelebrationData): Bitmap {
    val typeface = ResourcesCompat.getFont(context, R.font.vt323)
    val bitmap = Bitmap.createBitmap(IMG_W, IMG_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    when (design) {
        "trofeo" -> drawTrofeo(canvas, data, typeface)
        "arcade" -> drawArcade(canvas, data, typeface)
        else -> drawFuegos(canvas, data, typeface)
    }
    return bitmap
}

private suspend fun loadSpiritBitmaps(context: Context, urls: List<String>): List<Bitmap?> =
    withContext(Dispatchers.IO) {
        val limit = Semaphore(8)
        urls.map { url ->
            async {
                limit.withPermit {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(192)
                        .allowHardware(false) // Canvas por software no puede leer bitmaps de hardware
                        .build()
                    (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                }
            }
        }.awaitAll()
    }

private fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DiarioAliKevin")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    val stream = uri?.let { context.contentResolver.openOutputStream(it) }
    if (stream == null) {
        false
    } else {
        stream.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
} catch (e: Exception) {
    e.printStackTrace()
    false
}

private fun shareImage(context: Context, bitmap: Bitmap, name: String) {
    val dir = File(context.cacheDir, "celebracion").apply { mkdirs() }
    val file = File(dir, name)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir celebración"))
}

/**
 * Botón de la celebración de temporada. Solo se habilita cuando Kevin y Ali
 * tienen todos los espíritus de la temporada con maestría.
 */
@Composable
fun SeasonCelebrationButton(
    season: Int,
    orderedSpiritIds: List<String>,
    kevinMastery: List<String>,
    aliMastery: List<String>,
    familyCount: Int,
    imageUrlFor: (String) -> String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color
) {
    val missing = remember(orderedSpiritIds, kevinMastery, aliMastery) {
        val kevin = kevinMastery.toSet()
        val ali = aliMastery.toSet()
        orderedSpiritIds.count { it !in kevin } + orderedSpiritIds.count { it !in ali }
    }
    val unlocked = orderedSpiritIds.isNotEmpty() && missing == 0
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(2.dp, borderColor)
            .background(if (unlocked) Color(0xFFD97706) else cardBg)
            .clickable(enabled = unlocked) { showDialog = true }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (unlocked) "🏆 CREAR CELEBRACIÓN DE TEMPORADA" else "🔒 CELEBRACIÓN · faltan $missing maestrías entre los dos",
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = if (unlocked) Color(0xFF1A1A1A) else textColor.copy(alpha = 0.7f)
        )
    }

    if (showDialog) {
        SeasonCelebrationDialog(
            season = season,
            urls = remember(orderedSpiritIds) { orderedSpiritIds.map(imageUrlFor) },
            familyCount = familyCount,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun SeasonCelebrationDialog(
    season: Int,
    urls: List<String>,
    familyCount: Int,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var design by remember { mutableStateOf("trofeo") }
    var data by remember { mutableStateOf<CelebrationData?>(null) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    val failed = data?.spirits?.count { it == null } ?: 0

    LaunchedEffect(urls) {
        val spirits = loadSpiritBitmaps(context, urls)
        val date = SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date())
        data = CelebrationData(spirits, familyCount, season, date)
    }
    LaunchedEffect(data, design) {
        val current = data ?: return@LaunchedEffect
        image = null
        image = withContext(Dispatchers.Default) { renderCelebration(context, design, current) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                // Margen extra: los botones no deben quedar bajo la barra de navegación aunque el diálogo no reciba sus insets
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CELEBRACIÓN T$season", fontFamily = Vt323, fontSize = 28.sp, color = textColor, modifier = Modifier.weight(1f))
                Text(
                    "✕",
                    fontFamily = Vt323,
                    fontSize = 28.sp,
                    color = textColor,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelSegmented(
                    options = CELEBRATION_DESIGNS,
                    selected = design,
                    onSelect = { design = it },
                    textColor = textColor,
                    borderColor = borderColor,
                    background = cardBg
                )
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(IMG_W.toFloat() / IMG_H).border(2.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    val current = image
                    if (current == null) {
                        Text("Preparando imagen...", fontFamily = Vt323, fontSize = 22.sp, color = textColor)
                    } else {
                        Image(
                            bitmap = current.asImageBitmap(),
                            contentDescription = "Imagen de celebración de temporada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (failed > 0) {
                    Text(
                        "No cargaron $failed espíritus. Revisa tu conexión y vuelve a abrir antes de guardar.",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.height(70.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ready = image != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(2.dp, borderColor)
                        .clickable(enabled = ready) {
                            val current = image ?: return@clickable
                            scope.launch {
                                val name = "celebracion_t${season}_$design.png"
                                val saved = withContext(Dispatchers.IO) { saveToGallery(context, current, name) }
                                Toast.makeText(
                                    context,
                                    if (saved) "Guardada en Imágenes/DiarioAliKevin" else "No se pudo guardar. Prueba con Compartir.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("GUARDAR", fontFamily = Vt323, fontSize = 22.sp, color = textColor)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(2.dp, borderColor)
                        .background(Color(0xFFD97706))
                        .clickable(enabled = ready) {
                            val current = image ?: return@clickable
                            scope.launch {
                                val name = "celebracion_t${season}_$design.png"
                                try {
                                    withContext(Dispatchers.IO) { shareImage(context, current, name) }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "No se pudo compartir la imagen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("COMPARTIR", fontFamily = Vt323, fontSize = 22.sp, color = Color(0xFF1A1A1A))
                }
            }
        }
    }
}
