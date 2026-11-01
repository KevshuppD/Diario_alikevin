package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Compañeros de la mascota: dibujados sobre la misma escena de 80×52 celdas, a la escala de Thor.
const val COMPANION_MAX_EQUIPPED = 2
private const val COMPANION_SCALE = 0.7f
private const val FRAME_MS = 125L
private const val ROAM_PERIOD = 140
private const val ROAM_FRAMES = 44
private const val SNAIL = "snail"

private fun DrawScope.px(ox: Float, oy: Float, c: Float, x: Int, y: Int, w: Int, h: Int, argb: Long) {
    drawRect(Color(argb), Offset(ox + x * c, oy + y * c), Size(w * c, h * c))
}

/** Dibuja un compañero en la celda (0,0) de la grilla [c]. */
internal fun DrawScope.drawCompanion(id: String, f: Int, ox: Float, oy: Float, c: Float) {
    fun r(x: Int, y: Int, w: Int, h: Int, argb: Long) = px(ox, oy, c, x, y, w, h, argb)
    when (id) {
        "wisp" -> {
            val l = 0xFF7FE7E0; val d = 0xFF237F97; val k = 0xFF111111
            r(3, 0, 2, 2, d); r(2, 2, 4, 1, d); r(0, 3, 8, 5, d); r(1, 8, 6, 1, d); r(1, 3, 6, 5, l); r(3, 1, 2, 2, l)
            if (f % 24 > 1) { r(2, 5, 1, 2, k); r(5, 5, 1, 2, k) } else { r(2, 6, 1, 1, k); r(5, 6, 1, 1, k) }
            val w = f % 4 < 2
            r(if (w) 1 else 2, 9, 2, 1, l); r(if (w) 5 else 4, 9, 2, 1, l)
        }
        "lantern" -> {
            val b = 0xFF5A3B2A
            r(3, 0, 2, 2, b); r(1, 2, 6, 1, b); r(0, 3, 8, 7, 0xFFC8201F)
            r(2, 4, 4, 5, if (f % 5 < 2) 0xFFFFB84D else 0xFFFFD36B); r(3, 5, 2, 3, if (f % 7 < 2) 0xFFFFD36B else 0xFFFFF5C2)
            r(1, 10, 6, 1, b); r(3, 11, 2, 2, 0xFFF7C948)
        }
        "firefly" -> {
            val on = f % 8 < 5; val wing = 1 + f % 2
            if (on) { r(1, 2, 4, 2, 0x59D6FF78); r(2, 1, 2, 4, 0x59D6FF78) }
            r(2, 2, 2, 2, if (on) 0xFFE5FF70 else 0xFF74803A); r(2, 1, 1, 1, 0xFF222222)
            r(1, 2 - wing, 1, 1, 0xFFD8F0FF); r(4, 2 - wing, 1, 1, 0xFFD8F0FF)
        }
        "star" -> {
            val l = 0xFFFFE066; val o = 0xFFC98A12
            r(4, 0, 1, 2, o); r(3, 2, 3, 1, o); r(0, 3, 9, 2, o); r(1, 5, 7, 1, o); r(2, 6, 5, 1, o); r(1, 7, 3, 1, o); r(5, 7, 3, 1, o)
            r(4, 2, 1, 2, l); r(1, 3, 7, 2, l); r(2, 5, 5, 1, l); r(3, 6, 3, 1, l)
            if (f % 10 < 3) { r(9, -1, 1, 3, 0xFFFFFFFF); r(8, 0, 3, 1, 0xFFFFFFFF) }
        }
        "clover" -> {
            val l = 0xFF3FBF5F; val d = 0xFF1F7A3C; val s = sin(f / 5.0).roundToInt()
            for (b in 0..1) for (a in 0..1) { r(a * 5 + s, b * 5, 4, 4, d); r(a * 5 + s + 1, b * 5 + 1, 2, 2, l) }
            r(4 + s, 4, 1, 1, 0xFF9BF0A8); r(4, 9, 1, 3, d)
        }
        "coin" -> {
            val w = when (f % 6) { 0 -> 8; 1, 5 -> 6; 2, 4 -> 3; else -> 1 }
            val l = 4 - w / 2; val o = 0xFFA86C0B
            r(l, 1, w, 7, o)
            if (w > 2) { r(l + 1, 0, w - 2, 9, o); r(l + 1, 1, w - 2, 7, 0xFFF7C948) }
            if (w > 4) r(3, 3, 2, 3, o)
        }
        "skull" -> {
            val w = 0xFFE8E4F0; val d = 0xFF3B2A55; val o = 0xFFA855F7; val j = if (f % 10 < 5) 0 else 1
            r(1, 0, 7, 1, d); r(0, 1, 9, 5, d); r(1, 1, 7, 5, w); r(2, 3, 2, 2, o); r(5, 3, 2, 2, o); r(4, 5, 1, 1, d)
            r(2, 6 + j, 5, 2, d); r(3, 6 + j, 1, 1, w); r(5, 6 + j, 1, 1, w)
        }
        "flame" -> {
            val b = when (f % 4) { 1 -> 1; 3 -> -1; else -> 0 }
            r(3 + b, 0, 2, 2, 0xFFC084FC); r(2, 2, 5, 2, 0xFFA855F7); r(1, 4, 7, 3, 0xFF7C3AED); r(2, 7, 5, 2, 0xFF5B21B6)
            r(3, 4, 3, 3, 0xFFE9D5FF); r(4 - b, 3, 1, 1, 0xFFE9D5FF)
        }
        SNAIL -> {
            val ring = f % 32 < 10; val d = if (ring) (if (f % 2 == 0) -1 else 1) else 0
            val b = 0xFFD9A441; val e = 0xFF8A6420; val s = 0xFF7A3F1D; val k = 0xFF222222
            r(1 + d, 7, 12, 2, b); r(1 + d, 9, 12, 1, e); r(10 + d, 2, 3, 6, b); r(10 + d, 1, 1, 2, e); r(12 + d, 1, 1, 2, e)
            r(2 + d, 1, 8, 6, s); r(3 + d, 0, 6, 1, s); r(4 + d, 2, 4, 3, 0xFFC7762E); r(5 + d, 3, 2, 1, s)
            r(2 + d, -2, 8, 1, k); r(2 + d, -1, 2, 2, k); r(8 + d, -1, 2, 2, k)
            if (ring) { r(14 + d, -1, 1, 1, 0xFFFFFFFF); r(16 + d, -3, 1, 2, 0xFFFFFFFF); r(d, -4, 1, 2, 0xFFFFFFFF) }
        }
        "butterfly" -> {
            val a = if (f % 4 < 2) 3 else 2; val k = 0xFF111111
            r(4, 1, 1, 6, k); r(4 - a, 0, a, 3, 0xFFFB923C); r(5, 0, a, 3, 0xFFFB923C)
            r(5 - a, 4, a - 1, 2, 0xFFF472B6); r(5, 4, a - 1, 2, 0xFFF472B6); r(3, -1, 1, 1, k); r(5, -1, 1, 1, k)
        }
        "duck" -> {
            val l = 0xFFFDE047; val o = 0xFFCA8A04; val t = if (f % 8 < 4) 0 else 1
            r(4, 0, 5, 1, o); r(3, 1, 7, 4, o); r(4, 1, 5, 3, l); r(9, 2 + t, 3, 2, 0xFFF97316); r(7, 2, 1, 1, 0xFF111111)
            r(0, 4, 11, 5, o); r(1, 5, 9, 3, l); r(3, 6, 3, 1, o)
        }
        "cloud" -> {
            val l = 0xFFF1F5F9; val o = 0xFF94A3B8
            r(3, 0, 7, 2, o); r(1, 2, 11, 4, o); r(4, 1, 5, 2, l); r(2, 3, 9, 2, l); r(4, 3, 1, 1, 0xFF111111); r(8, 3, 1, 1, 0xFF111111)
            for (i in 0..2) r(3 + i * 3, 7 + (f + i * 2) % 4, 1, 2, 0xFF60A5FA)
        }
        "robot" -> {
            val m = 0xFF94A3B8; val d = 0xFF475569; val up = f % 4 < 2
            r(4, 0, 1, 2, d); r(3, -1, 3, 1, if (f % 6 < 3) 0xFFEF4444 else 0xFF7F1D1D); r(1, 2, 7, 5, d); r(2, 3, 5, 3, m)
            r(3, 4, 1, 1, 0xFF22D3EE); r(5, 4, 1, 1, 0xFF22D3EE); r(2, 7, 5, 3, d); r(3, 8, 3, 1, 0xFFFACC15)
            r(0, if (up) 7 else 8, 1, 2, d); r(8, if (up) 8 else 7, 1, 2, d)
        }
        "dragon" -> {
            val l = 0xFF34D399; val o = 0xFF047857; val u = f % 4 < 2
            r(2, 4, 8, 5, o); r(3, 5, 6, 3, l); r(8, 1, 6, 5, o); r(9, 2, 4, 3, l); r(12, 3, 1, 1, 0xFF111111)
            r(13, 5, 3, 1, if (f % 12 < 3) 0xFFFB923C else o); r(9, 0, 1, 1, 0xFFFDE68A); r(12, 0, 1, 1, 0xFFFDE68A)
            r(0, 7, 3, 1, o); r(-1, 6, 1, 1, o)
            r(3, if (u) 0 else 2, 4, if (u) 4 else 3, 0xFF059669); r(4, if (u) 1 else 3, 2, 2, l)
        }
    }
}

/** Capa animada sobre la escena. Los que vuelan cruzan la pantalla de vez en cuando; el caracol no se mueve del suelo. */
@Composable
fun CompanionsLayer(ids: List<String>, modifier: Modifier = Modifier) {
    if (ids.isEmpty()) return
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_MS)
            tick.intValue++
        }
    }
    Canvas(modifier = modifier) {
        val f = tick.intValue
        val cell = min(size.width / CUKY_SCENE_W, size.height / CUKY_SCENE_H)
        val x0 = (size.width - CUKY_SCENE_W * cell) / 2f
        val y0 = (size.height - CUKY_SCENE_H * cell) / 2f
        val c = cell * COMPANION_SCALE
        for (i in ids.indices) {
            val id = ids[i]
            val first = i == 0
            if (id == SNAIL) {
                drawCompanion(id, f, x0 + (if (first) 56 else 12) * cell, y0 + 41 * cell, c)
                continue
            }
            val speed = if (id == "firefly") 3 else 1
            val p = (f + i * 70 + id.length * 9) % ROAM_PERIOD
            val roam = p < ROAM_FRAMES
            val wave = if (roam) sin(PI * p / ROAM_FRAMES).toFloat() else 0f
            val lift = if (roam) sin(2 * PI * p / ROAM_FRAMES).toFloat() else 0f
            val x = (if (first) 58f else 12f) + 2f * speed * sin(f * speed / 9.0).toFloat() + (if (first) -36f else 36f) * wave
            val y = (if (first) 24f else 20f) + 2f * sin(f * speed / 4.0).toFloat() - 7f * lift
            drawCompanion(id, f, x0 + x.roundToInt() * cell, y0 + y.roundToInt() * cell, c)
        }
    }
}

@Composable
private fun rememberFrameTick(): androidx.compose.runtime.MutableIntState {
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_MS)
            tick.intValue++
        }
    }
    return tick
}

/** Miniatura animada de un compañero, a la misma escala que la mascota de la tienda. */
@Composable
fun CompanionThumb(id: String, modifier: Modifier = Modifier, slot: Int = 0) {
    val tick = rememberFrameTick()
    Canvas(modifier = modifier) {
        val f = tick.intValue
        val c = size.minDimension / 60f
        if (id == SNAIL) {
            drawCompanion(id, f, if (slot == 0) size.width - 18 * c else 2 * c, size.height - 12 * c, c)
        } else {
            val speed = if (id == "firefly") 3 else 1
            val baseX = if (slot == 0) size.width - 16 * c else 4 * c
            val x = baseX + (3f * speed * sin(f * speed / 9.0).toFloat()).roundToInt() * c
            val y = 8 * c + (3f * sin(f * speed / 4.0).toFloat()).roundToInt() * c
            drawCompanion(id, f, x, y, c)
        }
    }
}

/** Miniatura animada de un fondo, con encuadre centrado. */
@Composable
fun BackgroundThumb(id: String, isCuky: Boolean, modifier: Modifier = Modifier, night: Boolean = false) {
    val tick = rememberFrameTick()
    // La escena es más ancha que la miniatura: sin recorte se sale del recuadro
    Canvas(modifier = modifier.clipToBounds()) {
        val f = tick.intValue
        val cell = size.height / CUKY_SCENE_H
        val x0 = (size.width - CUKY_SCENE_W * cell) / 2f
        if (isCuky) drawCukyBackground(id, f, night, x0, 0f, cell) else drawThorBackground(id, f, night, x0, 0f, cell)
    }
}

// ---- Fondos ganados con espíritus (escena 80×52, animados)
val SPIRIT_BACKGROUNDS = setOf("bg_forest", "bg_lake", "bg_shrine", "bg_realm")
private val FOREST_SKY = longArrayOf(0xFF0D1B3A, 0xFF10264A, 0xFF153359, 0xFF1B4066)
private val SHRINE_SKY = longArrayOf(0xFF8B3F85, 0xFFC9547E, 0xFFF4716C, 0xFFFF9160, 0xFFFFB35C)
private val REALM_SKY = longArrayOf(0xFF120A33, 0xFF1D0F4D, 0xFF2B1466, 0xFF3C1A7D, 0xFF4E2391)
private val LAKE_SKY = longArrayOf(0xFF101A45, 0xFF17245C, 0xFF233176)
private val WISPS = intArrayOf(10, 24, 24, 16, 56, 20, 70, 28, 40, 10)
private val PINES_BACK = intArrayOf(6, 18, 31, 46, 58, 73)
private val PINES_FRONT = intArrayOf(0, 12, 25, 67, 78)

internal fun DrawScope.drawSpiritBackground(id: String, f: Int, x0: Float, y0: Float, c: Float) {
    fun r(x: Int, y: Int, w: Int, h: Int, argb: Long) = px(x0, y0, c, x, y, w, h, argb)
    fun pine(x: Int, base: Int, h: Int, col: Long) {
        for (i in 0 until h) { val w = 1 + (i shr 1) * 2; r(x - (w shr 1), base - h + i, w, 1, col) }
        r(x, base, 1, 3, 0xFF2A1C16)
    }
    when (id) {
        "bg_forest" -> {
            for (i in FOREST_SKY.indices) r(0, i * 10, 80, 10, FOREST_SKY[i])
            r(0, 40, 80, 12, 0xFF1F4A3F); r(0, 46, 80, 6, 0xFF173A32)
            val moon = 0xFFF6F2C8
            r(62, 5, 8, 8, moon); r(64, 4, 4, 10, moon); r(61, 7, 10, 4, moon)
            for (i in PINES_BACK.indices) pine(PINES_BACK[i], 40, 16 + i % 3 * 4, 0xFF123A3A)
            for (i in PINES_FRONT.indices) pine(PINES_FRONT[i], 42, 22 + i % 2 * 5, 0xFF0C2A2C)
            for (i in 0 until WISPS.size / 2) {
                if ((f + i * 5) % 23 == 0) continue
                val x = WISPS[i * 2] + (2 * sin(f / 9.0 + i)).roundToInt()
                val y = WISPS[i * 2 + 1] + (3 * sin(f / 6.0 + i * 2)).roundToInt()
                r(x, y, 2, 2, 0xFF7FE7E0); r(x - 1, y, 1, 1, 0xFF2F8F94); r(x + 2, y + 1, 1, 1, 0xFF2F8F94)
            }
        }
        "bg_shrine" -> {
            for (i in SHRINE_SKY.indices) r(0, i * 8, 80, 8, SHRINE_SKY[i])
            r(32, 28, 16, 8, 0xFFFFE39A); r(35, 25, 10, 14, 0xFFFFE39A)
            r(0, 38, 80, 14, 0xFF5A3B4D); r(28, 38, 24, 14, 0xFF9A8C8C)
            r(28, 42, 24, 1, 0xFF766A6C); r(28, 46, 24, 1, 0xFF766A6C); r(28, 50, 24, 1, 0xFF766A6C)
            r(16, 10, 48, 3, 0xFFC8201F); r(14, 8, 52, 2, 0xFF1E1214); r(20, 17, 40, 2, 0xFFC8201F)
            r(22, 10, 4, 29, 0xFFA81B1B); r(54, 10, 4, 29, 0xFFA81B1B); r(37, 13, 6, 4, 0xFF1E1214)
            for (x in intArrayOfLanterns) {
                r(x + 1, 38, 2, 6, 0xFF8A8080); r(x, 32, 4, 6, if ((f + x) % 7 < 2) 0xFFFFB84D else 0xFFFFD36B)
                r(x - 1, 30, 6, 2, 0xFF3A2A2A); r(x - 1, 38, 6, 1, 0xFF3A2A2A)
            }
            for (i in 0 until 9) {
                val px = ((i * 17 + f / 2 + (2 * sin(f / 5.0 + i)).roundToInt()) % 80 + 80) % 80
                r(px, (i * 11 + f) % 46, 1, 1, if (i % 2 == 0) 0xFFFF94B8 else 0xFFFFC2D6)
            }
        }
        "bg_realm" -> {
            for (i in REALM_SKY.indices) r(0, i * 9, 80, 9, REALM_SKY[i])
            for (x in 0 until 80) {
                val y = 12 + (5 * sin((x + f) / 9.0)).roundToInt()
                r(x, y, 1, 3, 0xFF3FD8B8); r(x, y + 3, 1, 2, 0xFF2E8FB8); r(x, y - 1, 1, 1, 0xFF8EF5D4)
            }
            for (i in 0 until 26) if ((f + i * 3) % 17 > 1) r((i * 37 + 11) % 80, (i * 23 + 5) % 34, 1, 1, if (i % 3 == 0) 0xFFFFD9FB else 0xFFFFFFFF)
            for (i in 0..1) {
                val x = if (i == 0) 4 else 60; val w = if (i == 0) 17 else 16
                val y = (if (i == 0) 27 else 23) + sin(f / 10.0 + i * 2).roundToInt()
                r(x, y, w, 2, 0xFF79E29B); r(x, y + 2, w, 2, 0xFF6F4D9E); r(x + 3, y + 4, w - 6, 2, 0xFF4F3478); r(x + 6, y + 6, w - 12, 1, 0xFF4F3478)
            }
            r(0, 42, 80, 10, 0xFF2F1D5E); r(0, 42, 80, 2, 0xFF68D8C2)
            for (i in 0..2) {
                val x = 14 + i * 26; val y = 36 + (2 * sin(f / 7.0 + i * 2)).roundToInt()
                r(x, y, 2, 2, 0xFFFFD86B); r(x - 1, y, 1, 1, 0xFFF98AD7)
            }
        }
        "bg_lake" -> {
            for (i in LAKE_SKY.indices) r(0, i * 10, 80, 10, LAKE_SKY[i])
            r(0, 30, 80, 8, 0xFF1A2342); r(0, 38, 80, 14, 0xFF14315F)
            for (i in 0 until 12) {
                val x = (i * 29 + 4 + sin(f / 12.0 + i).roundToInt() + 76) % 76
                val y = (((4 + (i * 17) % 25) - f / 3) % 30 + 30) % 30
                r(x + 1, y, 3, 1, 0xFFC8201F); r(x, y + 1, 5, 3, 0xFFFFB84D); r(x + 1, y + 2, 3, 1, 0xFFFFF0A8); r(x + 1, y + 4, 3, 1, 0xFFC8201F)
            }
            for (x in intArrayOfReflections) {
                val d = sin(f / 6.0 + x).roundToInt()
                r(x + d, 40, 5, 1, 0xFFFFB84D); r(x + 1 - d, 43, 3, 1, 0xFFC98A3D)
            }
            r(0, 46, 80, 6, 0xFF3A2D42); r(0, 46, 80, 1, 0xFF5C4963)
        }
    }
}
private val intArrayOfLanterns = intArrayOf(6, 70)
private val intArrayOfReflections = intArrayOf(8, 22, 60, 72)

/** Avatar fijo del widget: fondo, mascota y compañeros en un solo cuadro. */
fun renderWidgetAvatar(
    sizePx: Int, pet: android.graphics.Bitmap, isCuky: Boolean, background: String?, companions: List<String>, night: Boolean
): android.graphics.Bitmap {
    val back = renderPixelPetBitmap(sizePx) {
        if (background != null) {
            val cell = size.height / CUKY_SCENE_H
            val x0 = (size.width - CUKY_SCENE_W * cell) / 2f
            if (isCuky) drawCukyBackground(background, 0, night, x0, 0f, cell) else drawThorBackground(background, 0, night, x0, 0f, cell)
        }
    }
    val front = renderPixelPetBitmap(sizePx) {
        val c = size.minDimension / 60f
        companions.forEachIndexed { i, id ->
            val x = if (i == 0) size.width - 18 * c else 3 * c
            val y = if (id == SNAIL) size.height - 12 * c else 8 * c
            drawCompanion(id, 0, x, y, c)
        }
    }
    val out = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(out).apply {
        drawBitmap(back, 0f, 0f, null)
        drawBitmap(pet, 0f, 0f, null)
        drawBitmap(front, 0f, 0f, null)
    }
    return out
}
