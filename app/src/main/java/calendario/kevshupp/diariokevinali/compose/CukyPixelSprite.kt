package calendario.kevshupp.diariokevinali.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import calendario.kevshupp.diariokevinali.Pet
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// =====================================================================================
// Sprite procedural de Cuky (pixel-art por capas, 8 FPS a pasos de píxel entero).
// Port 1:1 del prototipo "Cuky en Movimiento". Todas las matrices y paletas son
// constantes de nivel superior: cero asignaciones dentro del ciclo de dibujo.
// =====================================================================================

// ---- Capas (20 columnas). o contorno, C crema, s mota, b café, l pecho, v pluma, d sombra, p rubor, f patas
private val CUKY_CREST = arrayOf("......oooooooo......", ".....oCsCCsCCso.....", "....oCCsCsCCsCCo....", "....oCsCCCsCsCCo....", ".....osCsCCsCso.....", "......oooooooo......")
private val CUKY_CREST_SMALL = arrayOf("....................", "....................", "........oo..........", ".......oCso.........", "......oCsCCo........", "......oooooo........")
private val CUKY_HEAD = arrayOf(".....obbbbbbbbbo....", "....obbbdbbbbdbbo...", "....obbbbbbbbbbbo...", "....obbbbbbbbbbbo...", "....opbbbbbbbbpbo...")
private val CUKY_TORSO = arrayOf("...obbbbbbbbbbbbbo..", "..obbbllllllbbbbbbo.", "..obblllvlllllbbbbo.", "..obbllllllvlbbbbbo.", "..obbblvllllbbbbbdo.", "...obbbbbbbbbbbbdo..", "....odbbbdbbbbddo...", ".....oooooooooo.....")
private val CUKY_WING = arrayOf("..............ooo...", ".............odddo..", ".............obdbdo.", ".............obdbdo.", "..............oddo..", "...............oo...")
private val CUKY_WING_UP = arrayOf("...............oo...", "..............oddo..", ".............obdbdo.", ".............obddo..", "..............ooo...", "....................")
private val CUKY_FEET = arrayOf(".......f....f.......", ".....fff...fff......")
private val CUKY_TAIL = arrayOf("..oo................", ".odbo...............", ".obdbo..............", "..obbo..............")
private val CUKY_NEST = arrayOf("..hhHhhHhhHhhHhhHh..", ".hHhhHhhHhhHhhHhhHh.", "..hhhhhhhhhhhhhhhh..")
private val CUKY_CROWN = arrayOf("......Y..Y..Y.......", "......YYYYYYY.......", "......ggggkgg.......")

// ---- Partículas (pares x,y)
private val FX_Z = intArrayOf(0, 0, 1, 0, 2, 0, 3, 0, 2, 1, 1, 2, 0, 3, 1, 3, 2, 3, 3, 3)
private val FX_HEART = intArrayOf(0, 0, 2, 0, 0, 1, 1, 1, 2, 1, 1, 2)
private val FX_STAR = intArrayOf(1, 0, 0, 1, 1, 1, 2, 1, 1, 2)
private val FLY_PATH = intArrayOf(16, 2, 18, 3, 19, 5, 18, 7, 16, 6, 15, 4)
private val PECK_SEQ = intArrayOf(0, 1, 3, 4, 2, 0, 0, 1, 0, 0)
private val HOP_DY = intArrayOf(0, -2, -4, -5, -4, -2, 0, 0, 0, 0, 0, 0)
private val FOAM = intArrayOf(6, 0, 9, -1, 12, 0, 4, 2, 14, 2, 8, -2, 11, -1, 5, 0, 13, 1, 7, -1, 10, -2, 15, 3)

// ---- Suciedad: col, fila, color, nivel mínimo, grupo (0 cresta, 1 cabeza, 2 cuerpo, 3 patas)
private const val G_CREST = 0
private const val G_HEAD = 1
private const val G_BODY = 2
private const val G_FEET = 3
private val DIRT_PTS = intArrayOf(
    5, 12, 'u'.code, 1, G_BODY, 13, 14, 'u'.code, 1, G_BODY, 9, 6, 'u'.code, 1, G_HEAD, 16, 12, 'u'.code, 1, G_BODY,
    6, 13, 'm'.code, 2, G_BODY, 7, 13, 'm'.code, 2, G_BODY, 12, 15, 'm'.code, 2, G_BODY, 3, 2, 'G'.code, 2, G_CREST, 12, 3, 'G'.code, 2, G_CREST, 7, 1, 'G'.code, 2, G_CREST,
    11, 15, 'm'.code, 3, G_BODY, 12, 14, 'm'.code, 3, G_BODY, 4, 11, 'm'.code, 3, G_BODY, 15, 12, 'm'.code, 3, G_BODY, 14, 6, 'm'.code, 3, G_HEAD,
    5, 19, 'm'.code, 3, G_FEET, 6, 19, 'm'.code, 3, G_FEET, 12, 19, 'm'.code, 3, G_FEET, 13, 19, 'm'.code, 3, G_FEET,
    8, 14, 'm'.code, 4, G_BODY, 9, 14, 'm'.code, 4, G_BODY, 8, 15, 'm'.code, 4, G_BODY, 3, 14, 'm'.code, 4, G_BODY, 4, 14, 'm'.code, 4, G_BODY, 16, 13, 'm'.code, 4, G_BODY,
    6, 8, 'm'.code, 4, G_HEAD, 8, 2, 'G'.code, 4, G_CREST, 9, 3, 'G'.code, 4, G_CREST, 10, 1, 'G'.code, 4, G_CREST, 5, 3, 'G'.code, 4, G_CREST,
    7, 18, 'm'.code, 4, G_FEET, 12, 18, 'm'.code, 4, G_FEET
)

// ---- Accesorios: filas + fila superior + grupo
private class CukyAcc(val group: Int, val top: Int, val rows: Array<String>)
private val CUKY_ACCESSORIES: Map<String, CukyAcc> = mapOf(
    Pet.ACC_HAT to CukyAcc(G_CREST, -3, arrayOf(".......nnnnnn.......", "......nnnnnnnn......", "......NNNNNNNN......", "...nnnnnnnnnnnnnn...", "....AAAAAAAAAAAA....")),
    Pet.ACC_CROWN to CukyAcc(G_CREST, -3, CUKY_CROWN),
    Pet.ACC_GLASSES to CukyAcc(G_HEAD, 7, arrayOf("....twtttttwtt......", "....tttt..tttt......")),
    Pet.ACC_MUSTACHE to CukyAcc(G_HEAD, 9, arrayOf(".....N.......N......", "......NNNNNNN.......")),
    Pet.ACC_BOW to CukyAcc(G_BODY, 10, arrayOf(".......k...k........", ".......kkKkk........", ".......k...k........")),
    Pet.ACC_BALLOON to CukyAcc(G_BODY, -5, arrayOf(".................kk.", "................kWkk", "................kkkk", "................kkkk", ".................kk.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.", "..................o.")),
    Pet.ACC_BANANA to CukyAcc(G_BODY, 12, arrayOf("..................A.", ".................aa.", "................aaa.", "...............aa...", "...............A....")),
    Pet.ACC_BANDANA to CukyAcc(G_BODY, 10, arrayOf("....rrWrrrrrWrr.....", ".....rrrrWrrrrr.....", ".......rrrrr........", "........rWr.........", ".........r..........")),
    Pet.ACC_COLLAR to CukyAcc(G_BODY, 10, arrayOf("....kkkkkkkkkkk.....", ".........Y..........", ".........Y..........")),
    Pet.ACC_SOCKS to CukyAcc(G_FEET, 17, arrayOf(".......r....r.......", ".......W....W.......", ".....WWW...WWW......"))
)

// ---- Paletas (índice = código del carácter)
private fun cukyPalette(base: IntArray?, vararg colors: Pair<Char, Long>): IntArray {
    val pal = base?.copyOf() ?: IntArray(128)
    for ((ch, argb) in colors) pal[ch.code] = argb.toInt()
    return pal
}
private val PAL_GALLINITA = cukyPalette(
    null,
    'o' to 0xFF2B1A12, 'C' to 0xFFF3E6C8, 's' to 0xFF3A2418, 'b' to 0xFFA8532B, 'l' to 0xFFD9844E, 'v' to 0xFFC2703F,
    'd' to 0xFF7A3A1E, 'p' to 0xFFE8806E, 'f' to 0xFFE9A23B, 'y' to 0xFFF2B33D, 'e' to 0xFF1B0F0A, 'w' to 0xFFFFFFFF,
    'h' to 0xFFC99A4B, 'H' to 0xFF8A6326, 'm' to 0xFF5B3A1E, 'z' to 0xFF2563EB, 'k' to 0xFFEC4899, 'K' to 0xFFBE185D,
    'g' to 0xFFF59E0B, 'q' to 0xFF60A5FA, 'x' to 0xFF2B1A12, 'E' to 0xFFF6F0E2, 'B' to 0xFF7A7A7A, 'r' to 0xFFDC2626,
    'W' to 0xFFFFFFFF, 'n' to 0xFFE2B868, 'N' to 0xFF7A3A1E, 'Y' to 0xFFFACC15, 't' to 0xFF1F2937, 'a' to 0xFFFDE047,
    'A' to 0xFFA16207, 'G' to 0xFF8C8270, 'u' to 0xFFB08A64
)
private val PAL_POLLITO = cukyPalette(
    PAL_GALLINITA,
    'b' to 0xFFF2C14E, 'l' to 0xFFFAE29A, 'v' to 0xFFEFC66A, 'd' to 0xFFC9932E, 'C' to 0xFFFAE29A, 's' to 0xFFE0A93A, 'p' to 0xFFF2A07E
)
private val PAL_REINA = cukyPalette(
    PAL_GALLINITA,
    'b' to 0xFF8F3F1C, 'l' to 0xFFD4733A, 'v' to 0xFFB45A2C, 'd' to 0xFF5E2A12, 'C' to 0xFFFFF3D6, 's' to 0xFF2A170D, 'p' to 0xFFF08A78
)
private const val SHADOW_ARGB = 0x402B1A12
private const val STINK_ARGB = 0xFF84CC16.toInt()

private const val OX = 3
private const val OY = 4

// ---- Estados de ojos y efectos
private const val EYES_OPEN = 0
private const val EYES_CLOSED = 1
private const val EYES_HAPPY = 2
private const val EYES_STRAIN = 3
private const val EYES_TIRED = 4

private const val FX_NONE = 0
private const val FX_PECK = 1
private const val FX_FLAP = 2
private const val FX_HOP = 3
private const val FX_SLEEP = 4
private const val FX_SHAKE = 5
private const val FX_BATH = 6
private const val FX_SPARKLE = 7
private const val FX_HUNGRY = 8

private const val ANIM_IDLE = 0
private const val ANIM_WALK = 1
private const val ANIM_PECK = 2
private const val ANIM_FLAP = 3
private const val ANIM_HOP = 4
private const val ANIM_SLEEP = 5
private const val ANIM_SHAKE = 6
private const val ANIM_BATH = 7
private const val ANIM_HUNGRY = 8

/** Acciones que la UI puede disparar sobre Cuky. */
object CukyAction {
    const val NONE = 0
    const val PECK = 1
    const val FLAP = 2
    const val HOP = 3
    const val BATH = 4
    const val BALL = 5

    /** Duración de cada acción en cuadros (8 FPS). */
    fun frames(action: Int): Int = when (action) {
        PECK -> 20
        FLAP -> 12
        HOP -> 24
        BATH -> 40
        BALL -> BALL_FRAMES
        else -> 0
    }

    /** Duración en milisegundos, para sincronizar la UI con la animación. */
    fun millis(action: Int): Long = frames(action) * FRAME_MS
}

private const val FRAME_MS = 125L

// ---- Pelota: cae rebotando, Cuky camina hacia ella, la picotea y la pelota se aleja
private const val BALL_FRAMES = 24
private const val BALL_GONE_FRAME = 22
private const val BALL_REACH = 6
private const val BALL_MAX_SPRITE_X = 51
private val BALL_X = intArrayOf(46, 44, 42, 40, 38, 36, 35, 34, 33, 32, 31, 31, 31, 34, 37, 40, 43, 45, 46, 46, 46, 46)
private val BALL_HEIGHT = intArrayOf(20, 12, 4, 0, 6, 8, 4, 0, 2, 0, 0, 0, 0, 3, 5, 3, 0, 1, 0, 0, 0, 0)

// ---- Paseo: acción, dirección, duración
private val ROUTE_ACT = intArrayOf(ANIM_WALK, ANIM_PECK, ANIM_WALK, ANIM_IDLE, ANIM_WALK, ANIM_FLAP, ANIM_WALK, ANIM_PECK, ANIM_IDLE)
private val ROUTE_DIR = intArrayOf(1, 0, 1, 0, -1, 0, -1, 0, 0)
private val ROUTE_LEN = intArrayOf(26, 20, 14, 10, 20, 12, 20, 10, 8)
private val ROUTE_TOTAL = ROUTE_LEN.sum()
private const val STROLL_START_X = 12
private const val SLEEP_X = 27

/** Niveles de suciedad según limpieza: 0 limpia, 1 polvito, 2 manchada, 3 embarrada, 4 muy sucia. */
fun cukyDirtLevel(cleanliness: Int): Int = when {
    cleanliness >= 80 -> 0
    cleanliness >= 60 -> 1
    cleanliness >= 40 -> 2
    cleanliness >= 20 -> 3
    else -> 4
}

private class CukyPose {
    var dx = 0; var dy = 0; var head = 0; var headDx = 0; var crest = 0; var crestDx = 0; var body = 0
    var eyes = EYES_OPEN; var wingUp = false; var sit = false; var nest = false; var lookDown = false
    var footL = 0; var footR = 0; var dirt = 0; var fx = FX_NONE; var fxF = 0; var seeds = 0
    var x = 0; var face = 1

    fun reset() {
        dx = 0; dy = 0; head = 0; headDx = 0; crest = 0; crestDx = 0; body = 0
        eyes = EYES_OPEN; wingUp = false; sit = false; nest = false; lookDown = false
        footL = 0; footR = 0; dirt = 0; fx = FX_NONE; fxF = 0; seeds = 0
    }
}

private class CukyClock {
    var frame = 0
    var strollT = 0
    var action = CukyAction.NONE
    var actionStart = 0
    var actionLen = 0
    var lastLevel = -1
    var lastX = STROLL_START_X
    var lastFace = 1
    var bathDirt = 0

    fun actionFrame(): Int = if (action != CukyAction.NONE && frame - actionStart < actionLen) frame - actionStart else -1
}

private class CukyTransform {
    var ox = 0f; var oy = 0f; var cell = 1f; var flip = false; var g = 0
}

// ---------------------------------------------------------------- Animaciones
private fun poseIdle(f: Int, s: CukyPose) {
    val p = f % 8
    s.head = if (p < 4) 0 else 1
    s.crest = if ((f + 7) % 8 < 4) 0 else 1
    if (f % 24 == 23) s.eyes = EYES_CLOSED
}

private fun poseWalk(k: Int, s: CukyPose) {
    val p = k % 4
    s.footL = if (p < 2) -1 else 0
    s.footR = if (p < 2) 0 else -1
    s.head = p % 2
    s.crest = (p + 1) % 2
}

private fun posePeck(f: Int, s: CukyPose) {
    val p = f % 10
    s.head = PECK_SEQ[p]
    s.crest = if (p == 3) 1 else if (p == 4) 2 else 0
    s.body = if (p == 3) 1 else 0
    s.fx = FX_PECK
    s.seeds = 4 - min(4, (f % 40) / 10)
}

private fun poseFlap(f: Int, s: CukyPose) {
    val p = f % 12
    s.eyes = EYES_HAPPY
    s.wingUp = p < 6 && p % 2 == 0
    s.head = if (p < 6) p % 2 else 0
    s.fx = FX_FLAP; s.fxF = f
}

private fun poseHop(f: Int, s: CukyPose) {
    val p = f % 12
    s.dy = HOP_DY[p]
    s.head = if (p == 0 || p == 6) 2 else if (p == 7) 1 else 0
    s.crest = if (p == 6) 1 else if (p in 1..2) -1 else 0
    s.wingUp = p in 1..5
    s.eyes = if (p in 1..7) EYES_HAPPY else EYES_OPEN
    s.fx = FX_HOP; s.fxF = p
}

private fun poseBall(f: Int, s: CukyPose) {
    when {
        f < 4 -> { s.head = 1; s.lookDown = true }
        f < 10 -> { poseWalk(f, s); s.dx = f - 3 }
        f < 13 -> {
            s.head = PECK_SEQ[f - 9]
            s.crest = if (f == 12) 1 else 0
            s.body = if (f == 12) 1 else 0
            s.dx = BALL_REACH
        }
        f < 20 -> { poseHop(f - 13, s); s.dx = BALL_REACH }
        else -> { poseWalk(f, s); s.dx = BALL_REACH - (f - 19) * 2 + 1 }
    }
}

private fun DrawScope.drawBall(t: CukyTransform, f: Int) {
    if (f >= BALL_GONE_FRAME) return
    val x = BALL_X[f]
    val y = OY + 18 + CUKY_FEET.size - 5 - BALL_HEIGHT[f]
    val edge = 0xFF7F1D1D.toInt()
    crect(t, x + 1, y, 3, 1, edge); crect(t, x, y + 1, 5, 3, edge); crect(t, x + 1, y + 4, 3, 1, edge)
    crect(t, x + 1, y + 1, 3, 3, 0xFFDC2626.toInt())
    // El brillo cambia de lugar para que se note que gira
    cpx(t, x + 1 + f % 3, y + 1 + (f + 1) % 2, 0xFFFCA5A5.toInt())
}

private fun poseSleep(f: Int, s: CukyPose) {
    val q = (f shr 1) % 8
    s.head = 1 + (if (q < 4) 0 else 1)
    s.crest = if (((f shr 1) + 7) % 8 < 4) 1 else 2
    s.eyes = EYES_CLOSED; s.nest = true; s.sit = true
    s.fx = FX_SLEEP; s.fxF = f
}

private fun poseShake(f: Int, s: CukyPose) {
    val p = f % 12
    if (p < 6) {
        s.dx = if (p % 2 == 1) -1 else 1
        s.crestDx = if (p % 2 == 1) 1 else -1
        s.eyes = EYES_CLOSED
    } else {
        poseIdle(f, s); s.eyes = EYES_HAPPY
    }
    s.fx = FX_SHAKE; s.fxF = p
}

private fun poseBath(p: Int, startDirt: Int, s: CukyPose) {
    if (p >= 28) {
        poseShake(p - 28, s)
        s.dirt = 0
        if (p >= 34) { s.fx = FX_SPARKLE; s.fxF = p - 34 }
        return
    }
    s.sit = true; s.body = 1
    s.head = 1 + (p shr 1) % 2; s.crest = 0
    s.dy = if (p % 4 < 2) 0 else 1
    s.eyes = if (p % 8 < 5) EYES_HAPPY else EYES_CLOSED
    s.dirt = max(0, startDirt - p / 6)
    s.wingUp = p % 6 < 2
    s.fx = FX_BATH; s.fxF = p
}

private fun poseHungry(f: Int, s: CukyPose) {
    s.head = 1; s.crest = 2; s.lookDown = true
    s.headDx = if (f % 16 < 8) 0 else -1
    s.fx = FX_HUNGRY; s.fxF = f
}

private fun poseAnim(anim: Int, f: Int, s: CukyPose) {
    when (anim) {
        ANIM_WALK -> poseWalk(f, s)
        ANIM_PECK -> posePeck(f, s)
        ANIM_FLAP -> poseFlap(f, s)
        ANIM_HOP -> poseHop(f, s)
        ANIM_SLEEP -> poseSleep(f, s)
        ANIM_SHAKE -> poseShake(f, s)
        ANIM_HUNGRY -> poseHungry(f, s)
        else -> poseIdle(f, s)
    }
}

/** Paseo por el escenario: deja en [s] la pose, la posición x y hacia dónde mira. */
private fun poseStroll(f: Int, hungry: Boolean, s: CukyPose) {
    var t = f % ROUTE_TOTAL
    var x = 0
    var face = 1
    for (i in ROUTE_ACT.indices) {
        val len = ROUTE_LEN[i]
        val dir = ROUTE_DIR[i]
        if (t < len) {
            if (ROUTE_ACT[i] == ANIM_WALK) {
                x += dir * t; face = dir
                poseWalk(t, s)
            } else {
                val anim = if (ROUTE_ACT[i] == ANIM_IDLE && hungry) ANIM_HUNGRY else ROUTE_ACT[i]
                poseAnim(anim, t, s)
            }
            s.x = STROLL_START_X + x; s.face = face
            return
        }
        if (ROUTE_ACT[i] == ANIM_WALK) { x += dir * len; face = dir }
        t -= len
    }
    poseIdle(f, s)
    s.x = STROLL_START_X + x; s.face = face
}

// ---------------------------------------------------------------- Dibujo
private fun DrawScope.cpx(t: CukyTransform, gx: Int, gy: Int, argb: Int) = crect(t, gx, gy, 1, 1, argb)

/** Rectángulo en celdas con bordes redondeados a píxel físico: sin costuras aunque la celda sea fraccional. */
private fun DrawScope.crect(t: CukyTransform, gx: Int, gy: Int, w: Int, h: Int, argb: Int) {
    val cx = if (t.flip) 26 - gx - w else gx
    val l = Math.round(t.ox + cx * t.cell).toFloat()
    val r = Math.round(t.ox + (cx + w) * t.cell).toFloat()
    val tp = Math.round(t.oy + gy * t.cell).toFloat()
    val b = Math.round(t.oy + (gy + h) * t.cell).toFloat()
    drawRect(Color(argb), Offset(l, tp), Size(r - l, b - tp))
}

private fun DrawScope.clayer(t: CukyTransform, rows: Array<String>, dx: Int, dy: Int, pal: IntArray, top: Int) {
    for (r in rows.indices) {
        val row = rows[r]
        for (i in row.indices) {
            val ch = row[i]
            if (ch != '.') cpx(t, OX + i + dx, OY + top + r + dy, pal[ch.code])
        }
    }
}

private fun DrawScope.cpts(t: CukyTransform, list: IntArray, x: Int, y: Int, argb: Int) {
    var i = 0
    while (i < list.size) { cpx(t, x + list[i], y + list[i + 1], argb); i += 2 }
}

private fun DrawScope.drawFeet(t: CukyTransform, s: CukyPose, pal: IntArray) {
    for (r in CUKY_FEET.indices) {
        val row = CUKY_FEET[r]
        for (i in row.indices) if (row[i] != '.') cpx(t, OX + i + s.dx, OY + 18 + r + s.dy + (if (i < 10) s.footL else s.footR), pal['f'.code])
    }
}

private fun DrawScope.drawEyes(t: CukyTransform, s: CukyPose, pal: IntArray) {
    val y = OY + 7 + s.head + s.dy + (if (s.lookDown) 1 else 0)
    for (k in 0..1) {
        val x = OX + (if (k == 0) 5 else 11) + s.headDx + s.dx
        val o = pal['o'.code]; val e = pal['e'.code]
        when (s.eyes) {
            EYES_CLOSED -> { cpx(t, x, y + 1, o); cpx(t, x + 1, y + 1, o) }
            EYES_HAPPY -> { cpx(t, x - 1, y + 1, o); cpx(t, x, y, o); cpx(t, x + 1, y, o); cpx(t, x + 2, y + 1, o) }
            EYES_STRAIN -> { cpx(t, x, y, o); cpx(t, x + 1, y + 1, o) }
            EYES_TIRED -> { cpx(t, x, y, o); cpx(t, x + 1, y, o); cpx(t, x, y + 1, e); cpx(t, x + 1, y + 1, e) }
            else -> {
                cpx(t, x, y, e); cpx(t, x + 1, y, e); cpx(t, x, y + 1, e); cpx(t, x + 1, y + 1, e)
                cpx(t, x + (if (s.lookDown) 1 else 0), y, pal['w'.code])
            }
        }
    }
}

private fun DrawScope.drawBeak(t: CukyTransform, s: CukyPose, pal: IntArray) {
    val x = OX + 8 + s.headDx + s.dx
    val y = OY + 8 + s.head + s.dy
    cpx(t, x, y, pal['y'.code]); cpx(t, x + 1, y, pal['y'.code]); cpx(t, x, y + 1, pal['y'.code]); cpx(t, x + 1, y + 1, pal['A'.code])
}

private fun DrawScope.drawDirt(t: CukyTransform, s: CukyPose, pal: IntArray) {
    var i = 0
    while (i < DIRT_PTS.size) {
        val col = DIRT_PTS[i]; val row = DIRT_PTS[i + 1]; val ch = DIRT_PTS[i + 2]; val lvl = DIRT_PTS[i + 3]; val grp = DIRT_PTS[i + 4]
        i += 5
        if (lvl > s.dirt) continue
        var dx = s.dx; var dy = s.dy
        when (grp) {
            G_CREST -> { dx += s.headDx + s.crestDx; dy += s.head + s.crest }
            G_HEAD -> { dx += s.headDx; dy += s.head }
            G_BODY -> dy += s.body
            else -> dy += if (col < 10) s.footL else s.footR
        }
        cpx(t, OX + col + dx, OY + row + dy, pal[ch])
    }
    if (s.dirt >= 3) {
        val a = (t.g % 6) * 2
        cpx(t, FLY_PATH[a], FLY_PATH[a + 1], pal['x'.code])
    }
    if (s.dirt >= 4) {
        val b = ((t.g + 3) % 6) * 2
        cpx(t, FLY_PATH[b] - 13, FLY_PATH[b + 1] + 1, pal['x'.code])
        val w = (t.g shr 1) % 2
        for (sx in intArrayOfStink) {
            cpx(t, OX + sx + w + s.dx, OY - 2 + s.dy, STINK_ARGB)
            cpx(t, OX + sx + 1 - w + s.dx, OY - 3 + s.dy, STINK_ARGB)
            cpx(t, OX + sx + w + s.dx, OY - 4 + s.dy, STINK_ARGB)
        }
    }
}
private val intArrayOfStink = intArrayOf(6, 13)

private fun DrawScope.drawAccessory(t: CukyTransform, s: CukyPose, acc: CukyAcc, small: Boolean, pal: IntArray) {
    var dx = s.dx; var dy = s.dy
    when (acc.group) {
        G_CREST -> { dx += s.headDx + s.crestDx; dy += s.head + s.crest + (if (small) 2 else 0) }
        G_HEAD -> { dx += s.headDx; dy += s.head }
        G_BODY -> dy += s.body
    }
    for (r in acc.rows.indices) {
        val row = acc.rows[r]
        val gy = acc.top + r
        for (i in row.indices) {
            val ch = row[i]
            if (ch == '.') continue
            val footLift = if (acc.group == G_FEET && gy >= 18) (if (i < 10) s.footL else s.footR) else 0
            cpx(t, OX + i + dx, OY + gy + dy + footLift, pal[ch.code])
        }
    }
}

private fun DrawScope.drawEffects(t: CukyTransform, s: CukyPose, pal: IntArray) {
    val p = s.fxF
    when (s.fx) {
        FX_PECK -> for (i in 0 until s.seeds) cpx(t, OX + 7 + i * 2, OY + 19, pal['g'.code])
        FX_FLAP -> { val h = p % 16; if (h < 10) cpts(t, FX_HEART, OX + 16, OY + 4 - h / 2, pal['k'.code]) }
        FX_HOP -> if (p in 2..7) { cpts(t, FX_STAR, 1, 4 + p % 2, pal['g'.code]); cpts(t, FX_STAR, 22, 2 + (p + 1) % 2, pal['g'.code]) }
        FX_SLEEP -> cpts(t, FX_Z, OX + 16, OY + 3 - (p shr 1) % 10, pal['z'.code])
        FX_SHAKE -> if (p < 7) {
            val q = pal['q'.code]
            cpx(t, OX + 1 - p, OY + 8 + p % 3, q); cpx(t, OX + 18 + p, OY + 7 + (p + 1) % 3, q)
            cpx(t, OX + 3 - p, OY + 2, q); cpx(t, OX + 16 + p, OY + 1, q)
        }
        FX_SPARKLE -> { cpts(t, FX_STAR, 1 + p % 2, 6, pal['g'.code]); cpts(t, FX_STAR, 21, 3 + p % 2, pal['g'.code]); cpts(t, FX_STAR, 19, 15 - p % 2, pal['E'.code]) }
        FX_HUNGRY -> {
            crect(t, 0, OY + 18, 6, 1, pal['B'.code]); crect(t, 1, OY + 19, 4, 1, pal['B'.code])
            if (p % 8 < 5) { val k = pal['k'.code]; cpx(t, OX + 17, OY + 1, k); cpx(t, OX + 17, OY + 2, k); cpx(t, OX + 17, OY + 4, k) }
        }
        FX_BATH -> {
            val foam = min(FOAM.size / 2, p shr 1)
            for (i in 0 until foam) cpx(t, OX + FOAM[i * 2] + s.headDx, OY + FOAM[i * 2 + 1] + s.dy + s.head + s.crest, pal['E'.code])
            crect(t, OX - 1, OY + 14, 22, 1, pal['H'.code]); crect(t, OX, OY + 14, 20, 1, pal['q'.code])
            var i = p % 2
            while (i < 20) { cpx(t, OX + i, OY + 13, pal['E'.code]); i += 2 }
            crect(t, OX, OY + 15, 20, 6, pal['h'.code]); crect(t, OX, OY + 17, 20, 1, pal['H'.code])
            crect(t, OX + 3, OY + 15, 1, 6, pal['H'.code]); crect(t, OX + 16, OY + 15, 1, 6, pal['H'.code])
            crect(t, OX, OY + 21, 20, 1, pal['o'.code])
            for (b in 0..2) {
                val tt = (p + b * 4) % 12
                if (tt < 9) { val bx = OX + 3 + b * 6 + tt % 2; val by = OY + 12 - tt; cpx(t, bx, by, pal['q'.code]); cpx(t, bx + 1, by, pal['E'.code]) }
            }
            if (p % 6 < 2) {
                val q = pal['q'.code]
                cpx(t, OX - 2, OY + 11, q); cpx(t, OX + 21, OY + 10, q); cpx(t, OX - 1, OY + 9, q); cpx(t, OX + 22, OY + 12, q)
            }
        }
    }
}

/** Dibuja a Cuky completa en la grilla de 26×26 celdas definida por [t]. */
private fun DrawScope.drawCuky(t: CukyTransform, s: CukyPose, level: Int, accessory: String?) {
    val small = level < 5
    val reina = level >= 15
    val pal = if (small) PAL_POLLITO else if (reina) PAL_REINA else PAL_GALLINITA
    val sw = if (s.dy < -2) 4 else 8
    crect(t, OX + 10 - sw / 2, OY + 20, sw, 1, SHADOW_ARGB)
    if (reina) clayer(t, CUKY_TAIL, s.dx, s.dy + s.body, pal, 10)
    if (!s.sit) drawFeet(t, s, pal)
    clayer(t, CUKY_TORSO, s.dx, s.dy + s.body, pal, 10)
    clayer(t, CUKY_HEAD, s.dx + s.headDx, s.dy + s.head, pal, 5)
    drawEyes(t, s, pal)
    drawBeak(t, s, pal)
    clayer(t, if (small) CUKY_CREST_SMALL else CUKY_CREST, s.dx + s.headDx + s.crestDx, s.dy + s.head + s.crest, pal, 0)
    if (s.dirt > 0) drawDirt(t, s, pal)
    clayer(t, if (s.wingUp) CUKY_WING_UP else CUKY_WING, s.dx, s.dy + s.body, pal, if (s.wingUp) 8 else 10)
    if (s.dirt >= 3 && !s.wingUp) cpx(t, OX + 15 + s.dx, OY + 12 + s.dy + s.body, pal['m'.code])
    if (reina) clayer(t, CUKY_CROWN, s.dx + s.headDx, s.dy + s.head + s.crest, pal, -3)
    val acc = CUKY_ACCESSORIES[accessory]
    if (acc != null && !(reina && accessory == Pet.ACC_CROWN)) drawAccessory(t, s, acc, small, pal)
    if (s.nest) clayer(t, CUKY_NEST, 0, 0, pal, 16)
    drawEffects(t, s, pal)
}

// ---------------------------------------------------------------- Composable
/**
 * Cuky dibujada en pixel-art procedural.
 * - [strollScene] = true: escenario de 80 celdas de ancho donde Cuky pasea, picotea y aletea.
 * - [strollScene] = false: Cuky en reposo centrada (tarjeta del feed, previsualización de la tienda).
 * [action] + [actionKey]: cada vez que cambia la llave se reproduce la acción una vez.
 */
@Composable
fun CukyPixelStage(
    pet: Pet,
    modifier: Modifier = Modifier,
    accessory: String? = pet.getActiveEquippedAccessory(),
    strollScene: Boolean = true,
    action: Int = CukyAction.NONE,
    actionKey: Int = 0,
    bathStartCleanliness: Int = 100,
    background: String? = null,
    sceneCells: Int = 80,
    bottomCells: Int = 3
) {
    val sleeping = pet.getActiveIsSleeping()
    val level = pet.getActiveLevel()
    val cleanliness = pet.getActiveCleanliness()
    val hungry = pet.getActiveHunger() >= 70

    val clock = remember { CukyClock() }
    val pose = remember { CukyPose() }
    val transform = remember { CukyTransform() }
    val tick = remember { mutableIntStateOf(0) }
    val sleepingNow by rememberUpdatedState(sleeping)

    LaunchedEffect(actionKey) {
        if (actionKey != 0 && action != CukyAction.NONE) {
            clock.action = action
            clock.actionStart = clock.frame
            clock.actionLen = CukyAction.frames(action)
            clock.bathDirt = cukyDirtLevel(bathStartCleanliness)
        }
    }
    LaunchedEffect(level) {
        if (clock.lastLevel in 1 until level) {
            clock.action = CukyAction.HOP
            clock.actionStart = clock.frame
            clock.actionLen = CukyAction.frames(CukyAction.HOP)
        }
        clock.lastLevel = level
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_MS)
            clock.frame++
            if (clock.actionFrame() < 0 && !sleepingNow) clock.strollT++
            tick.intValue = clock.frame
        }
    }

    Canvas(modifier = modifier) {
        tick.intValue // lectura en fase de dibujo: solo redibuja, no recompone
        val s = pose
        s.reset()
        val af = clock.actionFrame()
        var x = clock.lastX
        var face = clock.lastFace
        when {
            sleeping -> { poseSleep(clock.frame, s); x = SLEEP_X; face = 1 }
            af >= 0 -> when (clock.action) {
                CukyAction.BATH -> poseBath(af, clock.bathDirt, s)
                CukyAction.PECK -> posePeck(af, s)
                CukyAction.FLAP -> poseFlap(af, s)
                CukyAction.BALL -> poseBall(af, s)
                else -> poseHop(af, s)
            }
            strollScene -> { poseStroll(clock.strollT, hungry, s); x = s.x; face = s.face }
            else -> if (hungry) poseHungry(clock.frame, s) else poseIdle(clock.frame, s)
        }
        if (!sleeping && af >= 0 && clock.action == CukyAction.BATH) {
            // la suciedad la maneja la animación de baño
        } else {
            s.dirt = cukyDirtLevel(cleanliness)
            if (s.dirt == 4 && s.eyes == EYES_OPEN) s.eyes = EYES_TIRED
        }
        clock.lastX = x; clock.lastFace = face

        val t = transform
        t.g = clock.frame
        if (strollScene && background != null) {
            // Escena completa 80×52: fondo procedural + Cuky en la misma grilla
            t.cell = min(size.width / CUKY_SCENE_W, size.height / CUKY_SCENE_H)
            val sx0 = (size.width - CUKY_SCENE_W * t.cell) / 2f
            val sy0 = (size.height - CUKY_SCENE_H * t.cell) / 2f
            drawCukyBackground(background, clock.frame, sleeping, sx0, sy0, t.cell)
            t.ox = sx0 + (if (sleeping) SLEEP_X else x) * t.cell
            t.oy = sy0 + (CUKY_SCENE_H - 27) * t.cell
            t.flip = face < 0 && !sleeping && af < 0
        } else if (strollScene) {
            t.cell = floor(min(size.width / sceneCells, size.height / 30f))
            val sceneX0 = floor((size.width - sceneCells * t.cell) / 2f)
            t.ox = sceneX0 + (if (sleeping) (sceneCells - 26) / 2 else x) * t.cell
            t.oy = size.height - (26 + bottomCells) * t.cell
            t.flip = face < 0 && !sleeping && af < 0
        } else {
            t.cell = floor(min(size.width, size.height) / 26f)
            t.ox = floor((size.width - 26 * t.cell) / 2f)
            t.oy = floor((size.height - 26 * t.cell) / 2f)
            t.flip = false
        }
        val playingBall = !sleeping && af >= 0 && clock.action == CukyAction.BALL
        // Juega hacia el lado con más espacio, para que la pelota no salga de la escena
        if (playingBall && strollScene) t.flip = x + BALL_MAX_SPRITE_X > sceneCells
        drawCuky(t, s, level, accessory)
        if (playingBall) drawBall(t, af)
    }
}

/** Dibuja un sprite procedural en un Bitmap, para superficies sin Compose como el widget. */
internal fun renderPixelPetBitmap(sizePx: Int, draw: DrawScope.() -> Unit): Bitmap {
    val image = ImageBitmap(sizePx, sizePx)
    val size = Size(sizePx.toFloat(), sizePx.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(image), size, draw)
    return image.asAndroidBitmap()
}

/** Cuadro fijo de Cuky para el widget: mismo encuadre que la tarjeta del feed. */
fun renderCukyBitmap(sizePx: Int, level: Int, accessory: String?, sleeping: Boolean, hunger: Int, cleanliness: Int): Bitmap =
    renderPixelPetBitmap(sizePx) {
        val s = CukyPose()
        when {
            sleeping -> poseSleep(0, s)
            hunger >= 70 -> poseHungry(0, s)
            else -> poseIdle(0, s)
        }
        s.dirt = cukyDirtLevel(cleanliness)
        if (s.dirt == 4 && s.eyes == EYES_OPEN) s.eyes = EYES_TIRED
        val t = CukyTransform()
        t.cell = floor(min(size.width, size.height) / 26f)
        t.ox = floor((size.width - 26 * t.cell) / 2f)
        t.oy = floor((size.height - 26 * t.cell) / 2f)
        drawCuky(t, s, level, accessory)
    }
