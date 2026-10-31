package calendario.kevshupp.diariokevinali.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// =====================================================================================
// Fondos procedurales de Cuky (escena de 80×52 celdas), port 1:1 del prototipo
// "Cuky en Movimiento". Día / noche según si Cuky duerme. Sin asignaciones por cuadro.
// =====================================================================================

const val CUKY_SCENE_W = 80
const val CUKY_SCENE_H = 52

/** Fondos de Cuky dibujados en código (el resto de ids usa los PNG existentes). */
val CUKY_PROCEDURAL_BACKGROUNDS = setOf("coop", "farm", "meadow", "rain", "sunset", "beach", "jungle", "default", "space")

private const val W = CUKY_SCENE_W
private const val H = CUKY_SCENE_H

private val SKY_FARM_DAY = longArrayOf(0xFF1E9BE8, 0xFF45B5F0, 0xFF7DD0F4, 0xFFB8E8EC, 0xFFFFE8B0)
private val SKY_FARM_NIGHT = longArrayOf(0xFF0A0E36, 0xFF131852, 0xFF20256E, 0xFF392C80, 0xFF5C3A88)
private val SKY_MEADOW_DAY = longArrayOf(0xFF7B6CFF, 0xFFA98BFF, 0xFFE0A2F5, 0xFFFFBFD9, 0xFFFFE3C9)
private val SKY_MEADOW_NIGHT = longArrayOf(0xFF120C44, 0xFF1E1460, 0xFF32207A, 0xFF4C2A86, 0xFF6C3A8C)
private val SKY_RAIN_DAY = longArrayOf(0xFF4B5BB8, 0xFF5B78C8, 0xFF58A3CF, 0xFF6FCAC8, 0xFFA8E4C4)
private val SKY_RAIN_NIGHT = longArrayOf(0xFF0C1038, 0xFF121A4C, 0xFF16305E, 0xFF184868, 0xFF1E5C6A)
private val SKY_SUNSET_DAY = longArrayOf(0xFF4C2A9E, 0xFF8A2FA8, 0xFFD43A96, 0xFFF2556E, 0xFFFF7F4A, 0xFFFFA93D, 0xFFFFD15C)
private val SKY_SUNSET_NIGHT = longArrayOf(0xFF070B2C, 0xFF0E1240, 0xFF161852, 0xFF201E66, 0xFF2F2474, 0xFF46287E, 0xFF5E2E84)
private val RAINBOW = longArrayOf(0xFFFF5A5A, 0xFFFFA63D, 0xFFFFE14D, 0xFF4BD66A, 0xFF4D8CFF)
private val HUES = longArrayOf(0xFFFF4D8D, 0xFFFFD43B, 0xFFFFFFFF, 0xFF4D7CFF, 0xFFFF8A3D, 0xFFC85CFF)
private val BUNT = longArrayOf(0xFFFF4D8D, 0xFFFFD43B, 0xFF38BDF8, 0xFFFFF3D6)
private val EGGS = longArrayOf(0xFFFFB3D1, 0xFF9FE3FF, 0xFFFFF3D6)
private val COOP_PLANK_D = longArrayOf(0xFFD0713A, 0xFFBF6230, 0xFFDB8146)
private val COOP_PLANK_N = longArrayOf(0xFF332650, 0xFF2B2046, 0xFF3A2C5A)
private val HAY_BALES_NEW = intArrayOf(0, 65)
private val FIELD_D = longArrayOf(0xFFFFD12E, 0xFFA8E03E, 0xFFFF9A2E, 0xFF3EBB4A)
private val FIELD_N = longArrayOf(0xFF57562E, 0xFF2C5A3C, 0xFF634A2E, 0xFF1E4A38)
private val PUMPKINS = intArrayOf(6, 37, 24, 38, 69, 37)
private val BUTTERFLIES = longArrayOf(0xFFFF8A3D, 0xFF4D7CFF, 0xFFFFD43B)
private val RAIN_CLOUDS = intArrayOf(0, 3, 0, 22, 0, 0, 40, 7, 1, 64, 2, 1)
private val PUDDLES_NEW = intArrayOf(1, 44, 17, 56, 41, 20, 34, 48, 14)
private val MUSHROOMS_NEW = intArrayOf(24, 41, 29, 43)
private val FROG_JUMP = intArrayOf(0, 0, 0, -1, -2, -1, 0, 0)
private val RAIN_FLOWERS = longArrayOf(0xFF7C5CFF, 0xFF38BDF8, 0xFFFF4D8D, 0xFFFFD43B)
private val SUNSET_CLOUDS = intArrayOf(0, 6, 30, 13, 56, 3)

private val SKY_BEACH_DAY = longArrayOf(0xFF7CC8F0, 0xFF9FD8F5, 0xFFC8EAF8)
private val SKY_BEACH_NIGHT = longArrayOf(0xFF0E1530, 0xFF152044, 0xFF1E2C5A)
private val SKY_JUNGLE_DAY = longArrayOf(0xFF3E7A4A, 0xFF5E9A5A, 0xFF8FC07A)
private val SKY_JUNGLE_NIGHT = longArrayOf(0xFF0E1F1A, 0xFF142A22, 0xFF1C3A2C)
private val SHELLS = intArrayOf(6, 46, 30, 49, 70, 44)
private val VINES_X = intArrayOf(12, 24, 33, 48)
private val JUNGLE_LEAVES = intArrayOf(0, 44, 20, 46, 44, 45)
private val CONSOLE_LIGHTS = longArrayOf(0xFF22C55E, 0xFFEF4444, 0xFFF59E0B, 0xFF60A5FA, 0xFF22C55E)
private val CONSOLES_X = intArrayOf(0, 62)
private val HAY_BALES_X = intArrayOf(1, 64)
private val SUNFLOWERS_X = intArrayOf(8, 71)
private val LAMP_SWING = intArrayOf(0, 1, 1, 0, -1, -1)
private val BUTTERFLY_BOB = intArrayOf(0, 1, 2, 1)
private val APPLES = intArrayOf(8, 7, 16, 10, 12, 5)
private val FLOWERS_XY = intArrayOf(4, 40, 18, 44, 30, 39, 44, 46, 57, 41, 69, 45, 75, 38, 10, 48, 38, 49, 64, 49)
private val FLOWERS_COL = longArrayOf(0xFFEC4899, 0xFFF2B33D, 0xFFFFFFFF, 0xFF2563EB, 0xFFEC4899, 0xFFF2B33D, 0xFFFFFFFF, 0xFF2563EB, 0xFFEC4899, 0xFFFFFFFF)
private val PUDDLES = intArrayOf(2, 44, 16, 58, 42, 18, 36, 49, 12)
private val MUSHROOMS = intArrayOf(22, 40, 27, 42)
private val BIRDS = intArrayOf(0, 0, 6, 3, 11, 1)
private val BLADES_A = intArrayOf(0, -6, 0, 6, -6, 0, 6, 0)
private val BLADES_B = intArrayOf(-4, -4, 4, 4, -4, 4, 4, -4)

private fun wrap(v: Int, m: Int): Int = ((v % m) + m) % m
private fun rnd(v: Double): Int = Math.round(v).toInt()

/** Pincel de la escena: traduce celdas a píxeles y elige la paleta día / noche. */
private class ScenePainter {
    var ds: DrawScope? = null
    var x0 = 0f
    var y0 = 0f
    var c = 1f
    var night = false

    fun r(col: Long, x: Int, y: Int, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val l = Math.round(x0 + x * c).toFloat(); val rr = Math.round(x0 + (x + w) * c).toFloat()
        val t = Math.round(y0 + y * c).toFloat(); val b = Math.round(y0 + (y + h) * c).toFloat()
        ds!!.drawRect(Color(col), Offset(l, t), Size(rr - l, b - t))
    }
    fun n(day: Long, nite: Long): Long = if (night) nite else day
    fun dith(col: Long, x: Int, y: Int, w: Int, h: Int, ph: Int = 0) {
        for (j in 0 until h) {
            var i = (j + ph) % 2
            while (i < w) { r(col, x + i, y + j, 1, 1); i += 2 }
        }
    }
    fun grad(bands: LongArray, ya: Int, yb: Int) {
        val nb = bands.size
        val h = (yb - ya).toDouble() / nb
        for (i in 0 until nb) {
            val y = ya + rnd(i * h)
            r(bands[i], 0, y, W, ceil(h).toInt() + 1)
            if (i > 0) dith(bands[i - 1], 0, y, W, 1)
        }
    }
    fun stars(f: Int, maxY: Int) {
        if (!night) return
        for (i in 0 until 18) {
            val x = (i * 37 + 11) % W
            val y = (i * 23 + 5) % maxY
            if ((f + i * 3) % 11 < 9) r(if (i % 5 != 0) 0xFFFFF3C4 else 0xFFBFDBFE, x, y, 1, 1)
        }
    }
    fun cloud(x: Int, y: Int, col: Long, sh: Long) {
        r(col, x + 2, y, 8, 1); r(col, x, y + 1, 13, 2); r(col, x + 4, y - 1, 4, 1); r(sh, x + 1, y + 3, 11, 1)
    }
    fun moonOrSun(x: Int, y: Int) {
        if (night) {
            r(0xFFF3E6C8, x, y, 6, 6); r(0xFFD9CCAE, x + 1, y + 3, 2, 1); r(0xFFD9CCAE, x + 4, y + 1, 1, 1)
        } else {
            r(0xFFFFE08A, x - 1, y + 1, 8, 4); r(0xFFFFE08A, x + 1, y - 1, 4, 8); r(0xFFFFF3C4, x, y, 6, 6); r(0xFFFFE08A, x + 3, y + 3, 3, 3)
        }
    }
    fun grassTufts(y: Int, col: Long, f: Int, step: Int) {
        var x = 1
        while (x < W) {
            val b = if (((f shr 2) + x) % 6 < 3) 0 else 1
            r(col, x, y - 1, 1, 2); r(col, x + 1 + b, y - 2, 1, 2)
            x += step
        }
    }

    // ---------------------------------------------------------------- Escenas
    fun hill(col: Long, hi: Long, y: Int, amp: Int, per: Int, ph: Double, yb: Int) {
        for (x in 0 until W) { val t = y + rnd(amp * sin(x.toDouble() / per + ph)); r(col, x, t, 1, yb - t); r(hi, x, t, 1, 1) }
    }
    fun blob(col: Long, cx: Int, cy: Int, rad: Int) {
        for (d in -rad..rad) { val w = rnd(sqrt((rad * rad - d * d).toDouble())); r(col, cx - w, cy + d, 2 * w + 1, 1) }
    }
    fun flower(x: Int, y: Int, col: Long, mid: Long = 0xFFFFD43B) {
        r(n(0xFF2F9E44, 0xFF1C4A3A), x + 1, y + 2, 1, 2); r(col, x, y + 1, 3, 1); r(col, x + 1, y, 1, 3); r(mid, x + 1, y + 1, 1, 1)
    }
    fun fireflies(f: Int, count: Int, y0: Int) {
        if (!night) return
        for (i in 0 until count) { val t = (f + i * 5) % 14; if (t < 9) r(0xFFFFF27A, 5 + i * 71 % 74 + t % 4, y0 + ((t shr 1) + i) % 7, 1, 1) }
    }
    fun butterfly(x: Int, y: Int, col: Long, f: Int) {
        val o = (f shr 1) % 2; r(col, x, y - o, 1, 1 + o); r(col, x + 2, y - o, 1, 1 + o); r(0xFF2B1A12, x + 1, y, 1, 2)
    }

    fun coop(f: Int) {
        for (i in 0 until 14) {
            val k = i % 3
            r(if (night) COOP_PLANK_N[k] else COOP_PLANK_D[k], i * 6, 0, 6, 44); r(n(0xFF9E4E25, 0xFF221838), i * 6, 0, 1, 44); r(n(0xFFA85628, 0xFF251A3C), i * 6 + 3, 9 + (i * 7) % 20, 1, 2)
        }
        r(n(0xFF1FA39B, 0xFF17506A), 0, 33, W, 11); r(n(0xFF68D8CC, 0xFF2A7890), 0, 32, W, 1)
        var x = 2
        while (x < W) { r(n(0xFF158A84, 0xFF123F56), x, 33, 1, 11); x += 6 }
        r(n(0xFF70391C, 0xFF17102C), 0, 0, W, 4); r(n(0xFF944F28, 0xFF241A40), 0, 3, W, 1)
        for (i in 0 until 13) {
            val bx = 2 + i * 6; val by = 5 + rnd(3 * sin(i / 12.0 * Math.PI)); val col = BUNT[i % 4]; val s = if (((f shr 2) + i) % 4 == 0) 1 else 0
            val c2 = if (night) (col and 0xFFFFFF) or 0x99000000 else col
            r(n(0xFFFFF3D6, 0xFF6A6080), bx, by - 1, 6, 1); r(c2, bx, by, 5, 1); r(c2, bx + 1, by + 1, 3, 1); r(c2, bx + 2, by + 2 + s, 1, 1)
        }
        r(n(0xFFFFF3D6, 0xFF6D6488), 53, 10, 22, 18); r(n(0xFF62C8F5, 0xFF141C58), 55, 12, 18, 14)
        if (night) { r(0xFFFFF3C4, 67, 14, 4, 4); r(0xFFFFFFFF, 58, 14, 1, 1); r(0xFFFFFFFF, 62, 18, 1, 1) } else { blob(0xFFFFE066, 69, 15, 2); r(0xFFFFFFFF, 57, 15, 5, 1) }
        r(n(0xFF62C744, 0xFF1D4A40), 55, 21, 18, 5); r(n(0xFF3FA53A, 0xFF163A36), 60, 23, 13, 3); r(n(0xFFFFF3D6, 0xFF6D6488), 63, 12, 2, 14); r(n(0xFFFFF3D6, 0xFF6D6488), 55, 18, 18, 1)
        r(n(0xFF8A4A24, 0xFF2A1E3C), 52, 28, 24, 3)
        for (i in 0 until 6) flower(53 + i * 4, 25, n(HUES[i], 0xFF8A6AAA))
        if (!night) for (j in 0 until 19) r(0x33FFE88C, 53 - j, 28 + j, 20, 1)
        r(n(0xFF70391C, 0xFF1E1534), 3, 23, 27, 2); r(n(0xFF944F28, 0xFF2C2048), 3, 23, 27, 1)
        for (k in 0..2) {
            val nx = 5 + k * 9; val col = n(EGGS[k], (EGGS[k] and 0xFFFFFF) or 0xAA000000)
            r(col, nx + 1, 17, 3, 3); r(col, nx + 2, 16, 1, 1); r(n(0xFFF2B33D, 0xFF6E5634), nx - 1, 19, 7, 4); dith(n(0xFFB8741F, 0xFF584428), nx - 1, 20, 7, 3)
        }
        val sw = LAMP_SWING[(f shr 1) % 6]
        r(0xFF2B1A12, 40 + sw, 4, 1, 6); r(0xFFC2410C, 37 + sw, 10, 7, 2); r(0xFFFFE066, 39 + sw, 12, 3, 2)
        blob(n(0x1AFFE066, 0x38FFB83C), 40 + sw, 13, if (night) 15 else 7)
        for (bx in HAY_BALES_NEW) {
            r(n(0xFFF2B33D, 0xFF7E6230), bx, 35, 15, 9); r(n(0xFFFFD76A, 0xFF94743A), bx, 35, 15, 1)
            for (j in 0 until 3) r(n(0xFFC98A23, 0xFF604A28), bx, 37 + j * 3, 15, 1)
            r(n(0xFFB91C1C, 0xFF5A2436), bx + 4, 35, 1, 9); r(n(0xFFB91C1C, 0xFF5A2436), bx + 10, 35, 1, 9)
        }
        r(n(0xFFF0B83A, 0xFF6E5530), 0, 44, W, 8); dith(n(0xFFFFD76A, 0xFF826538), 0, 44, W, 2)
        for (i in 0 until 20) r(if (night) 0xFF54401F else if (i % 2 == 1) 0xFFC98A23 else 0xFFFFE48F, (i * 13 + 4) % W, 46 + (i % 3) * 2, 3, 1)
        if (!night) for (i in 0 until 7) { val t = (f + i * 9) % 30; r(0xFFFFF9D0, 50 - i * 4 - (t shr 2), 28 + t % 16, 1, 1) }
    }

    fun farm(f: Int) {
        grad(if (night) SKY_FARM_NIGHT else SKY_FARM_DAY, 0, 28); stars(f, 20)
        if (!night) blob(0x4DFFE278, 64, 8, 8)
        moonOrSun(61, 5)
        val cx = wrap(f shr 2, W + 30) - 15
        cloud(cx, 5, n(0xFFFFFFFF, 0xFF433F88), n(0xFFFFD8C2, 0xFF332F70)); cloud(wrap(cx + 45, W + 30) - 15, 11, n(0xFFFFFFFF, 0xFF433F88), n(0xFFFFD8C2, 0xFF332F70))
        hill(n(0xFF6E8FE0, 0xFF27296E), n(0xFFA5BCF2, 0xFF3A3C8C), 19, 3, 7, 1.0, 30); hill(n(0xFF5CC23E, 0xFF1A4A42), n(0xFF9BE65C, 0xFF256056), 24, 2, 11, 0.0, 34)
        for (y in 27 until 33) for (x in 0 until W) { val k = (((x + (y and 1) * 2) shr 4) + (y shr 2)) % 4; r(if (night) FIELD_N[k] else FIELD_D[k], x, y, 1, 1) }
        r(n(0xFFE03131, 0xFF5A2036), 50, 20, 10, 7); r(n(0xFFFFFFFF, 0xFF8A84A4), 53, 23, 4, 4); r(n(0xFF8F1D1D, 0xFF3A1428), 49, 18, 12, 2); r(n(0xFF8F1D1D, 0xFF3A1428), 51, 17, 8, 1)
        var x = 2
        while (x < W) {
            if (x !in 44..64) {
                val b = if (((f shr 1) + x) % 8 < 4) 0 else 1
                r(n(0xFF2B8A3E, 0xFF1E5240), x + b, 16, 2, 22); r(n(0xFF51CF66, 0xFF2A6A50), x + b - 3, 22, 3, 1); r(n(0xFF51CF66, 0xFF2A6A50), x + b + 2, 26, 3, 1); r(n(0xFF51CF66, 0xFF2A6A50), x + b - 3, 30, 3, 1)
                r(n(0xFFFFD43B, 0xFF8A7A3A), x + b + 2, 19, 2, 4); r(n(0xFFF08C00, 0xFF6E5A2C), x + b + 3, 20, 1, 1); r(n(0xFFE9F58A, 0xFF5C7040), x + b, 14, 2, 2)
            }
            x += 7
        }
        val sc = (f shr 2) % 2
        r(n(0xFF8A4B24, 0xFF3A2A30), 54, 25, 1, 14); r(n(0xFF2563EB, 0xFF25346E), 52, 28, 5, 6); r(n(0xFFE03131, 0xFF5A2036), 48 + sc, 28, 16 - sc, 2); blob(n(0xFFFFE3A8, 0xFF7E7470), 54, 24, 2); r(n(0xFFF59F00, 0xFF6E5230), 51, 21, 7, 2); r(n(0xFFF59F00, 0xFF6E5230), 53, 19, 3, 2)
        r(n(0xFFFFFFFF, 0xFF7A7490), 0, 35, W, 1)
        x = 3
        while (x < W) { r(n(0xFFFFFFFF, 0xFF7A7490), x, 33, 2, 7); x += 8 }
        r(n(0xFF92501F, 0xFF3E2A2C), 0, 39, W, 13)
        var y = 41
        while (y < H) { r(n(0xFF6C3814, 0xFF2C1E24), 0, y, W, 1); dith(n(0xFFB46A30, 0xFF4C3434), 0, y + 1, W, 1, y); y += 3 }
        var i = 0
        while (i < PUMPKINS.size) {
            val px = PUMPKINS[i]; val py = PUMPKINS[i + 1]
            r(n(0xFFFF7A1A, 0xFF7A4A2A), px, py, 6, 3); r(n(0xFFE8590C, 0xFF5E3A24), px + 2, py, 1, 3); r(n(0xFFFFA94D, 0xFF8A5C34), px + 1, py, 1, 1); r(n(0xFF2F9E44, 0xFF1E5240), px + 2, py - 1, 2, 1)
            i += 2
        }
        x = 12
        while (x < W) { r(n(0xFF51CF66, 0xFF2A6A50), x, 39, 1, 2); r(n(0xFF8CE99A, 0xFF347A5C), x + 1, 38, 1, 2); x += 9 }
        fireflies(f, 7, 18)
        if (!night) butterfly(wrap(f, W), 18, 0xFFFF4D8D, f)
    }

    fun meadow(f: Int) {
        grad(if (night) SKY_MEADOW_NIGHT else SKY_MEADOW_DAY, 0, 26); stars(f, 20); moonOrSun(60, 5)
        cloud(wrap(f shr 3, W + 30) - 15, 8, n(0xFFFFFFFF, 0xFF56488E), n(0xFFFFC6DC, 0xFF42367A))
        hill(n(0xFF55C9A5, 0xFF1E4C62), n(0xFF8CEAC6, 0xFF2B6878), 21, 3, 10, 1.0, 32); hill(n(0xFF7ED957, 0xFF1E5A4A), n(0xFFB4F27A, 0xFF2E7460), 27, 2, 7, 0.0, 38)
        r(n(0xFF7A3E1E, 0xFF35243C), 11, 17, 4, 20); r(n(0xFFA85F30, 0xFF49344E), 11, 17, 1, 20); r(n(0xFF7A3E1E, 0xFF35243C), 14, 22, 6, 2)
        blob(n(0xFFF06BA8, 0xFF6A3880), 7, 10, 7); blob(n(0xFFF06BA8, 0xFF6A3880), 21, 10, 7); blob(n(0xFFFF8FC4, 0xFF7E4896), 14, 7, 8); blob(n(0xFFFFB8DA, 0xFF9A5EB0), 11, 5, 3); blob(n(0xFFFFB8DA, 0xFF9A5EB0), 20, 7, 2); dith(n(0xFFD94F92, 0xFF5A2E72), 3, 13, 23, 3)
        for (i in 0 until 9) { val t = (f + i * 11) % 44; r(n(0xFFFFB8DA, 0xFF9A5EB0), 2 + i * 7 + rnd(3 * sin(t / 5.0)), 8 + t, 1, 1) }
        r(n(0xFF48B83C, 0xFF17503E), 0, 36, W, 16); dith(n(0xFF6EDB4C, 0xFF20664C), 0, 36, W, 16)
        for (y in 33 until H) {
            val cx = 62 + rnd(5 * sin(y / 4.0)); val w = 2 + ((y - 33) shr 2)
            r(n(0xFF2EA8F5, 0xFF264A9A), cx - w, y, 2 * w + 1, 1)
            if ((y + (f shr 1)) % 4 == 0) r(n(0xFFD6F5FF, 0xFF6A8ACC), cx - w + 1 + y % 3, y, 2, 1)
        }
        for (i in 0 until 30) {
            val fx = (i * 29 + 7) % 77; val fy = 37 + (i * 17) % 13
            if (kotlin.math.abs(fx - 62) < 9 && fy > 38) continue
            flower(fx, fy, n(HUES[i % 6], 0xFF6A5A9A), n(if (i % 6 == 1) 0xFFFF8A3D else 0xFFFFD43B, 0xFF8A7A5A))
        }
        var x = 0
        while (x < W) { val b = if (((f shr 1) + x) % 4 < 2) 0 else 1; r(n(0xFF2F9E44, 0xFF16463A), x + b, 34, 1, 3); x += 3 }
        fireflies(f, 8, 28)
        if (!night) for (k in 0..2) { val t = (f + k * 17) % 60; butterfly(wrap(t * 2 + k * 25, W), 16 + k * 5 + BUTTERFLY_BOB[(f shr 1) % 4], BUTTERFLIES[k], f) }
    }

    fun rain(f: Int) {
        grad(if (night) SKY_RAIN_NIGHT else SKY_RAIN_DAY, 0, 30)
        if (!night) for (i in RAINBOW.indices) for (x in 0 until 44) {
            val y = rnd(31 - sqrt(max(0.0, 484.0 - (x - 22) * (x - 22))) * 0.9) + i
            r(RAINBOW[i], 34 + x, y, 1, 1)
        }
        var i = 0
        while (i < RAIN_CLOUDS.size) {
            val cx = wrap((f shr 3) + RAIN_CLOUDS[i], W + 30) - 15
            if (RAIN_CLOUDS[i + 2] == 1) cloud(cx, RAIN_CLOUDS[i + 1], n(0xFF7A84C8, 0xFF2B2F6A), n(0xFF454C94, 0xFF181C48)) else cloud(cx, RAIN_CLOUDS[i + 1], n(0xFF5C64AE, 0xFF20245A), n(0xFF454C94, 0xFF181C48))
            i += 3
        }
        hill(n(0xFF3DAF8C, 0xFF143E56), n(0xFF74D8AC, 0xFF1C5468), 25, 2, 10, 0.0, 34); hill(n(0xFF46C95A, 0xFF154A40), n(0xFF8CEB72, 0xFF1F6050), 30, 2, 8, 2.0, 40)
        r(n(0xFFFFF0CE, 0xFF5A5078), 8, 23, 13, 10); r(n(0xFFE8551E, 0xFF6E2A3A), 6, 21, 17, 2); r(n(0xFFE8551E, 0xFF6E2A3A), 8, 19, 13, 2); r(n(0xFFFF8A4D, 0xFF88384A), 10, 18, 9, 1); r(n(0xFF2563EB, 0xFF25346E), 10, 27, 4, 6); r(n(0xFFFFE066, 0xFFFFD43B), 16, 25, 3, 3)
        if (night) blob(0x2EFFD43B, 17, 27, 7)
        r(n(0xFF2FB344, 0xFF154A40), 0, 38, W, 14); dith(n(0xFF51D85C, 0xFF1E5C42), 0, 38, W, 14, 1)
        i = 0
        while (i < PUDDLES_NEW.size) {
            val px = PUDDLES_NEW[i]; val py = PUDDLES_NEW[i + 1]; val pw = PUDDLES_NEW[i + 2]
            r(n(0xFF3DB8F5, 0xFF254C96), px + 1, py, pw - 2, 4); r(n(0xFF3DB8F5, 0xFF254C96), px, py + 1, pw, 2); r(n(0xFFD5F5FF, 0xFF6A9ACC), px + 2, py, pw - 6, 1); r(n(0xFFC7A6FF, 0xFF4E3C96), px + 3, py + 2, pw - 8, 1)
            val t = (f + px) % 10
            if (t < 6) { r(0xFFEFFAFF, px + (pw shr 1) - t, py + 1, 1, 1); r(0xFFEFFAFF, px + (pw shr 1) + t, py + 1, 1, 1) }
            i += 3
        }
        i = 0
        while (i < MUSHROOMS_NEW.size) {
            val mx = MUSHROOMS_NEW[i]; val my = MUSHROOMS_NEW[i + 1]
            r(0xFFFFF3D6, mx + 1, my, 2, 3); r(0xFFF03E3E, mx - 1, my - 3, 6, 2); r(0xFFF03E3E, mx, my - 4, 4, 1); r(0xFFFFFFFF, mx, my - 3, 1, 1); r(0xFFFFFFFF, mx + 3, my - 4, 1, 1)
            i += 2
        }
        val boot = n(0xFFFFD12E, 0xFF7E6A30)
        r(boot, 72, 36, 3, 5); r(boot, 72, 40, 5, 2); r(boot, 66, 37, 3, 5); r(boot, 66, 41, 5, 2)
        r(n(0xFF5C3A20, 0xFF2E2234), 49, 27, 1, 14); r(n(0xFFFF4D8D, 0xFF823A70), 42, 25, 15, 2); r(n(0xFFFF4D8D, 0xFF823A70), 44, 24, 11, 1); r(n(0xFFFFD43B, 0xFF8A7A40), 47, 25, 5, 2)
        val j = FROG_JUMP[(f shr 1) % 8]
        r(n(0xFF1FC86A, 0xFF1C7050), 38, 44 + j, 5, 3); r(0xFFFFFFFF, 38, 43 + j, 1, 1); r(0xFFFFFFFF, 42, 43 + j, 1, 1); r(0xFF2B1A12, 38, 44 + j, 1, 1); r(0xFF2B1A12, 42, 44 + j, 1, 1)
        for (k in 0 until 8) flower((k * 31 + 3) % 76, 39 + (k * 13) % 11, n(RAIN_FLOWERS[k % 4], 0xFF5E5A9A))
        val drop = n(0xE6EBF8FF, 0xD97DAAE1)
        for (k in 0 until 40) {
            val sp = if (k % 2 == 1) 4 else 3
            val x = wrap(k * 13 + 3 - f * (sp shr 1), W); val y = wrap(f * sp + k * 7, H)
            r(drop, x, y, 1, 2); if (sp == 4) r(drop, x - 1, y + 2, 1, 1)
        }
    }

    fun sunset(f: Int) {
        grad(if (night) SKY_SUNSET_NIGHT else SKY_SUNSET_DAY, 0, 34); stars(f, 28)
        if (night) {
            r(0xFFFFF3C4, 60, 4, 7, 7); r(0xFFE4D2A0, 62, 6, 2, 2)
            for (x in 0 until W) r(if (x % 2 == 1) 0x5940F0B4 else 0x59BE5AFF, x, 10 + rnd(4 * sin(x / 9.0 + f / 10.0)), 1, 6)
        } else {
            blob(0x40FFE278, 58, 27, 17); blob(0x66FFE278, 58, 27, 12); blob(0xFFFFE066, 58, 27, 9); blob(0xFFFFF6C2, 58, 26, 6)
            for (i in 0 until 8) { val a = i * Math.PI / 4 + (f shr 3) * 0.2; r(0xFFFFE98A, 58 + rnd(14 * kotlin.math.cos(a)), 27 + rnd(14 * sin(a)), 1, 1) }
        }
        var i = 0
        while (i < SUNSET_CLOUDS.size) { cloud(wrap((f shr 3) + SUNSET_CLOUDS[i], W + 30) - 15, SUNSET_CLOUDS[i + 1], n(0xFFFFB4C8, 0xFF3C3480), n(0xFFC2388E, 0xFF2A2460)); i += 2 }
        if (!night) {
            val bx = wrap(f shr 1, W + 20) - 10
            i = 0
            while (i < BIRDS.size) { val ox = BIRDS[i]; val oy = BIRDS[i + 1]; val w = (f shr 1) % 2; r(0xFF3A1248, bx + ox, oy + 8 + w, 1, 1); r(0xFF3A1248, bx + ox + 1, oy + 9 - w, 1, 1); r(0xFF3A1248, bx + ox + 2, oy + 8 + w, 1, 1); i += 2 }
        }
        hill(n(0xFF9A3F9E, 0xFF2A2268), n(0xFFD466A8, 0xFF40348A), 25, 3, 6, 1.0, 36); hill(n(0xFF6A2A8E, 0xFF1C1850), n(0xFFA84098, 0xFF2C2670), 29, 2, 9, 0.0, 38)
        r(n(0xFFB9C4D4, 0xFF3C4266), 27, 15, 7, 21); r(n(0xFFE9EEF5, 0xFF555C86), 28, 15, 2, 21); r(n(0xFFE03131, 0xFF6A2238), 27, 12, 7, 3); r(n(0xFFFF6B6B, 0xFF883048), 28, 11, 5, 1)
        r(n(0xFFD42A2A, 0xFF5A1E30), 4, 19, 22, 17); r(n(0xFF7A1616, 0xFF2B1020), 2, 16, 26, 3); r(n(0xFF7A1616, 0xFF2B1020), 5, 13, 20, 3); r(n(0xFF7A1616, 0xFF2B1020), 9, 11, 12, 2); r(n(0xFFFF9A4D, 0xFF6A3050), 25, 19, 1, 17)
        var x = 6
        while (x < 25) { r(n(0xFFB01E1E, 0xFF481828), x, 20, 1, 16); x += 4 }
        r(0xFFFFF3D6, 10, 25, 10, 11); r(n(0xFFD42A2A, 0xFF5A1E30), 11, 26, 8, 9)
        for (k in 0 until 9) { r(0xFFFFF3D6, 11 + k, 26 + k, 1, 1); r(0xFFFFF3D6, 19 - k, 26 + k, 1, 1) }
        r(0xFFFFF3D6, 12, 19, 6, 4); r(if (night) 0xFFFFD43B else 0xFF5E1A17, 13, 20, 4, 2)
        r(n(0xFFFFF0CE, 0xFF55507A), 70, 17, 2, 19)
        val a0 = (f shr 1) * Math.PI / 8
        for (b in 0 until 4) { val a = a0 + b * Math.PI / 2; for (k in 1..7) r(n(if (b % 2 == 1) 0xFFFFFFFF else 0xFFFF4D8D, 0xFF8A84B0), 71 + rnd(k * kotlin.math.cos(a)), 16 + rnd(k * sin(a)), 1, 1) }
        r(0xFF2B1A12, 70, 15, 2, 2)
        r(n(0xFFE8A21E, 0xFF3A2A52), 0, 35, W, 9)
        x = 0
        while (x < W) { val b = if (((f shr 1) + x) % 6 < 3) 0 else 1; r(n(0xFFFFD84D, 0xFF54406A), x + b, 33 + x % 3, 1, 4); r(n(0xFFB9700F, 0xFF2E2044), x, 39 + x % 4, 1, 3); x += 2 }
        x = 0
        while (x < W) { r(n(0xFF4A1E5E, 0xFF1A1234), x + 3, 36, 2, 9); x += 11 }
        r(n(0xFF4A1E5E, 0xFF1A1234), 0, 39, W, 1)
        r(n(0xFFC76B2E, 0xFF2E2240), 0, 44, W, 8); dith(n(0xFFE8914A, 0xFF3C2C50), 0, 44, W, 2)
        for (k in 0 until 10) r(n(0xFF944518, 0xFF231A34), (k * 17 + 5) % W, 47 + (k % 3) * 2, 4, 1)
        fireflies(f, 6, 30)
    }
}

private fun ScenePainter.beach(f: Int) {
    grad(if (night) SKY_BEACH_NIGHT else SKY_BEACH_DAY, 0, 24); stars(f, 20); moonOrSun(60, 4)
    cloud(wrap(f shr 3, W + 30) - 15, 6, n(0xFFFFFFFF, 0xFF2E3A66), n(0xFFDCEBF3, 0xFF243055))
    r(n(0xFF5E8F6A, 0xFF1A2A3A), 8, 21, 14, 3); r(n(0xFF4E7F5A, 0xFF16243A), 11, 19, 8, 2); r(n(0xFF3E6B2E, 0xFF152030), 14, 16, 1, 3)
    r(n(0xFF2E9BD6, 0xFF10244A), 0, 24, W, 12); r(n(0xFF5FB8E6, 0xFF183466), 0, 24, W, 2)
    for (k in 0 until 12) { val x = wrap(k * 11 + (f shr 1) * (if (k % 2 == 0) 1 else -1), W); r(n(0xFFE6F6FF, 0xFF3C5A8A), x, 27 + (k % 4) * 2, 3, 1) }
    if (night) for (j in 0 until 5) r(0xFF8A8FB0, 60 + (f + j) % 2, 26 + j * 2, 4, 1) else dith(0xFFFFF3C4, 58, 25, 8, 1)
    r(n(0xFFF2D59A, 0xFF4A4030), 0, 36, W, 16); dith(n(0xFFE6C482, 0xFF3E3528), 0, 40, W, 12, 1)
    for (x in 0 until W) r(n(0xFFFFFFFF, 0xFF6A80A8), x, 35 + rnd(sin(x / 6.0 + f / 4.0)), 1, 1)
    var i = 0
    while (i < SHELLS.size) { r(n(0xFFF6C1D0, 0xFF6B5A6A), SHELLS[i], SHELLS[i + 1], 2, 1); r(n(0xFFE89AB0, 0xFF5A4A5A), SHELLS[i], SHELLS[i + 1] + 1, 2, 1); i += 2 }
    // Sombrilla y reposera
    r(n(0xFFE6D3B3, 0xFF5A5060), 20, 26, 1, 16); r(n(0xFFEF4444, 0xFF6A2A2A), 14, 26, 13, 2); r(n(0xFFFFFFFF, 0xFF7A7A8A), 18, 26, 3, 2); r(n(0xFFEF4444, 0xFF6A2A2A), 16, 25, 9, 1)
    r(n(0xFF2563EB, 0xFF2A3A6A), 3, 36, 2, 5); r(n(0xFF2563EB, 0xFF2A3A6A), 4, 40, 10, 2); r(n(0xFFFFFFFF, 0xFF6A7090), 6, 40, 2, 2); r(n(0xFFFFFFFF, 0xFF6A7090), 10, 40, 2, 2)
    r(0xFF8A5A36, 4, 42, 1, 3); r(0xFF8A5A36, 13, 42, 1, 3)
    // Palmera
    for (k in 0 until 14) r(n(0xFF8A5A36, 0xFF3A2A22), 64 + k / 3, 22 + k, 2, 1)
    val b = (f shr 2) % 2
    val leaf = n(0xFF3E8F3E, 0xFF1D3A26); val dark = n(0xFF2E6B2E, 0xFF152A1E)
    r(leaf, 56 + b, 20, 8, 1); r(leaf, 54 + b, 21, 3, 1); r(leaf, 65 - b, 19, 9, 1); r(leaf, 72 - b, 20, 4, 1); r(dark, 60, 18, 8, 1)
    r(dark, 58 + b, 22, 2, 2); r(dark, 70 - b, 21, 2, 2); r(0xFF5B3A1E, 63, 22, 2, 2); r(0xFF5B3A1E, 66, 22, 2, 2)
    // Cangrejo
    val cx = wrap(f shr 1, W + 10) - 5; val leg = f % 2
    r(n(0xFFEF4444, 0xFF8A3030), cx, 47, 4, 2); r(0xFF2B1A12, cx, 46, 1, 1); r(0xFF2B1A12, cx + 3, 46, 1, 1)
    r(n(0xFFDC2626, 0xFF7A2A2A), cx - 1, 48 + leg, 1, 1); r(n(0xFFDC2626, 0xFF7A2A2A), cx + 4, 49 - leg, 1, 1)
    if (!night) { val gx = wrap(f shr 1, W + 20) - 10; val w = (f shr 1) % 2; r(0xFFFFFFFF, gx, 9 + w, 1, 1); r(0xFFFFFFFF, gx + 1, 10 - w, 1, 1); r(0xFFFFFFFF, gx + 2, 9 + w, 1, 1) }
}

private fun ScenePainter.jungle(f: Int) {
    grad(if (night) SKY_JUNGLE_NIGHT else SKY_JUNGLE_DAY, 0, 30)
    var x = 0
    while (x < W) { r(n(0xFF2E6B3A, 0xFF0E2018), x, 6 + (x % 5), 7, 30); dith(n(0xFF3E7F46, 0xFF132A1E), x, 6 + (x % 5), 7, 4); x += 9 }
    r(n(0xFF1F5A2E, 0xFF0A1810), 0, 0, W, 6); dith(n(0xFF2E7A3E, 0xFF12261A), 0, 6, W, 3)
    // Cascada y poza
    r(n(0xFF6E6E6E, 0xFF2A2A30), 60, 8, 14, 28); r(n(0xFF8FD3F0, 0xFF2A4A6A), 63, 8, 8, 28)
    for (k in 0 until 6) r(n(0xFFFFFFFF, 0xFF6A8AAA), 64 + (k % 3) * 2, 8 + wrap(f * 2 + k * 5, 26), 1, 3)
    r(n(0xFF4FA8D8, 0xFF1E3A5A), 54, 36, 26, 5); dith(n(0xFFFFFFFF, 0xFF6A8AAA), 60, 36, 12, 1, f % 2)
    // Troncos y lianas
    r(n(0xFF6B4A2A, 0xFF2A1E14), 4, 4, 5, 36); r(n(0xFF5A3A20, 0xFF22180F), 5, 4, 1, 36); r(n(0xFF6B4A2A, 0xFF2A1E14), 40, 0, 4, 38)
    for (v in VINES_X) {
        val len = 10 + v % 7; val b = if (((f shr 2) + v) % 4 < 2) 0 else 1
        for (k in 0 until len) { val sx = v + (if (k > len / 2) b else 0); r(n(0xFF3E8F3E, 0xFF1D3A26), sx, k, 1, 1); if (k % 3 == 2) r(n(0xFF5EAF4E, 0xFF24462E), sx + 1, k, 1, 1) }
    }
    // Loro en su rama
    r(n(0xFF6B4A2A, 0xFF2A1E14), 26, 17, 14, 1)
    r(n(0xFFDC2626, 0xFF5A1A1A), 31, 13, 3, 4); r(n(0xFF2563EB, 0xFF1E2A5A), 31, 16, 3, 1); r(0xFFF2B33D, 34, 14, 1, 1)
    r(if (night) 0xFF2B1A12 else 0xFFFFFFFF, 32, 14, 1, 1); r(n(0xFFF59E0B, 0xFF5A3A1A), 32, 18, 1, 2)
    r(n(0xFF4A6A2A, 0xFF1A2A14), 0, 40, W, 12); dith(n(0xFF5E8A3A, 0xFF22381A), 0, 40, W, 2)
    r(n(0xFFEC4899, 0xFF6A2A4A), 16, 38, 2, 2); r(n(0xFFF59E0B, 0xFF6A4A2A), 50, 39, 2, 2)
    var i = 0
    while (i < JUNGLE_LEAVES.size) {
        val lx = JUNGLE_LEAVES[i]; val ly = JUNGLE_LEAVES[i + 1]
        r(n(0xFF2E8F3E, 0xFF15361E), lx, ly, 8, 2); r(n(0xFF2E8F3E, 0xFF15361E), lx + 2, ly - 1, 5, 1); r(n(0xFF1F6A2E, 0xFF0E2614), lx + 1, ly + 2, 6, 1)
        i += 2
    }
    if (night) for (k in 0 until 7) { val t = (f + k * 5) % 14; if (t < 9) r(0xFFF4F07A, 4 + k * 11 + (t % 4), 20 + ((t shr 1) % 6), 1, 1) }
}

private fun ScenePainter.room(f: Int) {
    r(n(0xFFF3D9B1, 0xFF2E2A3A), 0, 0, W, 38)
    var x = 0
    while (x < W) { r(n(0xFFEBCB9B, 0xFF282436), x, 0, 2, 38); x += 6 }
    r(n(0xFF8A5A36, 0xFF2A1E18), 0, 37, W, 2)
    r(n(0xFFB07A4A, 0xFF3A2A1E), 0, 39, W, 13)
    var y = 41
    while (y < H) { r(n(0xFF9A6A3E, 0xFF30221A), 0, y, W, 1); y += 4 }
    for (i in 0 until 8) r(n(0xFF9A6A3E, 0xFF30221A), (i * 11 + (i % 2) * 5) % W, 39 + (i % 3) * 4, 1, 4)
    r(n(0xFFEC4899, 0xFF4A2238), 22, 44, 36, 6); r(n(0xFFF9A8D4, 0xFF5A2A48), 24, 45, 32, 4); dith(n(0xFFEC4899, 0xFF4A2238), 26, 46, 28, 2)
    // Ventana con cortinas
    r(n(0xFF8A5A36, 0xFF2A1E18), 50, 6, 22, 18); r(n(0xFF9FD3EC, 0xFF121633), 52, 8, 18, 14)
    if (night) { r(0xFFF3E6C8, 64, 10, 3, 3); r(0xFFFFF3C4, 55, 10, 1, 1); r(0xFFFFF3C4, 58, 18, 1, 1) } else { r(0xFFFFFFFF, 55, 11, 6, 1); r(0xFFFFFFFF, 54, 12, 8, 1) }
    r(0xFF8A5A36, 60, 8, 1, 14); r(0xFF8A5A36, 52, 14, 18, 1)
    val cb = (f shr 2) % 2
    r(n(0xFF6B4A2A, 0xFF2A1E14), 46, 4, 28, 1); r(n(0xFFDC2626, 0xFF4A1A1A), 48, 5, 4 + cb, 21); r(n(0xFFDC2626, 0xFF4A1A1A), 70 - cb, 5, 4 + cb, 21)
    // Cuadro con corazón
    r(n(0xFFD97706, 0xFF5A3A10), 8, 8, 12, 9); r(n(0xFF9FD3EC, 0xFF2A3A5A), 9, 9, 10, 7)
    r(0xFFEC4899, 12, 11, 2, 1); r(0xFFEC4899, 15, 11, 2, 1); r(0xFFEC4899, 12, 12, 5, 1); r(0xFFEC4899, 13, 13, 3, 1); r(0xFFEC4899, 14, 14, 1, 1)
    // Repisa con planta y libros
    r(n(0xFF8A5A36, 0xFF2A1E18), 4, 22, 18, 1); r(n(0xFFC2703F, 0xFF4A2A1A), 6, 19, 4, 3); r(n(0xFF3E8F3E, 0xFF1D3A26), 5, 16, 6, 3)
    r(n(0xFF2563EB, 0xFF1E2A5A), 13, 17, 2, 5); r(n(0xFFF59E0B, 0xFF5A3A10), 15, 18, 2, 4); r(n(0xFF16A34A, 0xFF123A1E), 17, 17, 2, 5)
    // Reloj de péndulo
    r(n(0xFF6B4A2A, 0xFF2A1E14), 30, 6, 8, 16); r(n(0xFFF6F0E2, 0xFF6A6A7A), 31, 7, 6, 6); r(0xFF2B1A12, 34, 8, 1, 3); r(0xFF2B1A12, 34, 10, 2, 1)
    val sw = LAMP_SWING[(f shr 1) % 6]
    r(0xFF2B1A12, 34, 14, 1, 4); r(0xFFF2B33D, 33 + sw, 18, 3, 2)
    // Lámpara de pie
    r(0xFF2B1A12, 75, 24, 1, 15); r(n(0xFFF2B33D, 0xFFF59E0B), 72, 20, 7, 4)
    if (night) { r(0x29F59E0B, 60, 24, 20, 28); r(0x1FF59E0B, 66, 24, 14, 28) }
}

private fun ScenePainter.space(f: Int) {
    r(n(0xFF4A5568, 0xFF1E2230), 0, 0, W, 40)
    var x = 0
    while (x < W) { r(n(0xFF3F4A5C, 0xFF181C28), x, 0, 1, 40); x += 10 }
    var y = 0
    while (y < 40) { r(n(0xFF3F4A5C, 0xFF181C28), 0, y, W, 1); y += 10 }
    r(n(0xFFE0F2FE, 0xFFEF4444), 0, 1, W, 1)
    // Ventanal circular con estrellas y planeta
    for (dy in -13..13) { val w = rnd(sqrt(169.0 - dy * dy)); r(n(0xFF9CA3AF, 0xFF4A5060), 40 - w - 1, 18 + dy, 2 * w + 3, 1) }
    for (dy in -12..12) { val w = rnd(sqrt(144.0 - dy * dy)); r(0xFF0A0E24, 40 - w, 18 + dy, 2 * w + 1, 1) }
    for (i in 0 until 20) {
        val sx = 28 + wrap(i * 29 - f * (1 + i % 3), 25); val sy = 6 + (i * 13) % 25
        if ((sx - 40) * (sx - 40) + (sy - 18) * (sy - 18) < 140) r(if (i % 4 == 0) 0xFFBFDBFE else 0xFFFFFFFF, sx, sy, 1, 1)
    }
    r(0xFFF59E0B, 44, 12, 5, 5); dith(0xFFD97706, 44, 12, 5, 5); r(0xFFF3E6C8, 42, 14, 9, 1)
    // Consolas con luces
    for (cx in CONSOLES_X) {
        r(n(0xFF2D3748, 0xFF141820), cx, 30, 18, 10)
        for (k in 0 until 5) { val on = ((f shr 1) + k + cx) % 3 != 0; r(if (on) CONSOLE_LIGHTS[k] else 0xFF1A202C, cx + 2 + k * 3, 32, 2, 1) }
        r(n(0xFF0F766E, 0xFF0A3A36), cx + 3, 34, 12, 4)
        for (k in 0 until 12) r(0xFF5EEAD4, cx + 3 + k, 35 + rnd(sin((k + f) / 2.0)), 1, 1)
    }
    r(n(0xFF6B7280, 0xFF2A2E3E), 0, 40, W, 12); dith(n(0xFF9CA3AF, 0xFF3A3F52), 0, 40, W, 1)
    x = 0
    while (x < W) { r(n(0xFF4B5563, 0xFF1E2230), x, 41, 1, 11); x += 4 }
    if (night && f % 16 < 8) r(0x30EF4444, 0, 0, W, 40)
}

private val scenePainter = ScenePainter()

/** Dibuja el fondo [id] en la escena de 80×52 celdas con origen ([x0], [y0]) y celdas de [cell] px. */
internal fun DrawScope.drawCukyBackground(id: String, frame: Int, night: Boolean, x0: Float, y0: Float, cell: Float) {
    val p = scenePainter
    p.ds = this; p.x0 = x0; p.y0 = y0; p.c = cell; p.night = night
    clipRect(x0, y0, x0 + W * cell, y0 + H * cell) {
        when (id) {
            "farm" -> p.farm(frame)
            "meadow" -> p.meadow(frame)
            "rain" -> p.rain(frame)
            "sunset" -> p.sunset(frame)
            "beach" -> p.beach(frame)
            "jungle" -> p.jungle(frame)
            "default" -> p.room(frame)
            "space" -> p.space(frame)
            else -> p.coop(frame)
        }
    }
    p.ds = null
}

// =====================================================================================
// Fondos procedurales de Thor, port del prototipo "Thor en Movimiento" (misma escena 80×52).
// Gallinero y Huerta reutilizan las escenas nuevas de Cuky.
// =====================================================================================

/** Fondos de Thor dibujados en código. */
val THOR_PROCEDURAL_BACKGROUNDS = setOf("default", "beach", "jungle", "space", "coop", "farm")

private val TH_SKY_BEACH_DAY = longArrayOf(0xFF1E9BE8, 0xFF4FBDF0, 0xFF88D8F2, 0xFFC5F0EE)
private val TH_SKY_BEACH_NIGHT = longArrayOf(0xFF0A0E36, 0xFF131852, 0xFF20256E, 0xFF3A2F80)
private val TH_SKY_JUNGLE_DAY = longArrayOf(0xFFC7F27A, 0xFF7FDB5C, 0xFF34B56A, 0xFF128A62)
private val TH_SKY_JUNGLE_NIGHT = longArrayOf(0xFF0B2A3A, 0xFF0E3440, 0xFF114044, 0xFF0E4A44)
private val TH_SHELVES_Y = intArrayOf(12, 21, 30)
private val TH_VINES_X = intArrayOf(15, 24, 33, 50)
private val TH_JUNGLE_FLOWERS = intArrayOf(16, 38, 22, 41, 46, 39, 3, 43, 52, 45, 70, 44, 32, 48)
private val TH_JUNGLE_FLOWER_COL = longArrayOf(0xFFEC4899, 0xFFF59E0B, 0xFFA855F7, 0xFFEC4899, 0xFFF97316, 0xFFFACC15, 0xFF38BDF8)
private val TH_JUNGLE_BUSHES = intArrayOf(0, 45, 23, 47, 44, 46)
private val TH_CONSOLE_LIGHTS = longArrayOf(0xFF22C55E, 0xFFEF4444, 0xFFF59E0B, 0xFF60A5FA, 0xFFEC4899)

private fun ScenePainter.thorRoom(f: Int) {
    r(n(0xFFB65A30, 0xFF3A2748), 0, 0, W, 40)
    var y = 2; var row = 0
    while (y < 40) {
        r(n(0xFF8A3F20, 0xFF291A38), 0, y + 3, W, 1)
        var x = (row % 2) * 4
        while (x < W) { r(n(0xFF8A3F20, 0xFF291A38), x, y, 1, 3); x += 8 }
        x = (row * 5) % 13
        while (x < W) { r(n(0xFFCE7040, 0xFF452F58), x + 1, y, 6, 3); x += 19 }
        y += 4; row++
    }
    // Ventana con árbol o luna
    r(n(0xFFFFF0CE, 0xFF6A6088), 29, 5, 22, 21); r(n(0xFF4FC3F7, 0xFF141C58), 31, 7, 18, 17)
    if (night) { r(0xFFFFF3C4, 42, 9, 4, 4); r(0xFFFFFFFF, 34, 10, 1, 1); r(0xFFFFFFFF, 37, 15, 1, 1) }
    else { r(0xFFFFFFFF, 33, 10, 6, 1); r(0xFFFFFFFF, 35, 9, 3, 1); blob(0xFF4CAF50, 44, 19, 4); r(0xFF6B4423, 44, 21, 1, 3) }
    r(n(0xFFFFF0CE, 0xFF6A6088), 39, 7, 2, 17); r(n(0xFFFFF0CE, 0xFF6A6088), 31, 14, 18, 1); r(n(0xFF8A4A24, 0xFF30213E), 27, 26, 26, 2)
    // Planta colgante
    val sb = (f shr 2) % 2
    r(0xFF2F9E44, 55, 8, 1, 4); blob(n(0xFF40C057, 0xFF1E5A4A), 55 + sb, 13, 3); r(n(0xFFD9622B, 0xFF6A3050), 53, 16, 5, 3)
    // Repisas con planta, libros y ovillo
    for (sy in TH_SHELVES_Y) r(n(0xFF7A3E1E, 0xFF261A34), 3, sy, 20, 2)
    r(n(0xFFE8722C, 0xFF6A3A34), 5, 8, 5, 4); blob(n(0xFF40C057, 0xFF1E5A4A), 7, 6, 2)
    r(0xFF2563EB, 13, 8, 2, 4); r(0xFFF59E0B, 15, 9, 2, 3); r(0xFF16A34A, 17, 8, 2, 4); r(0xFF2563EB, 6, 17, 1, 4); r(0xFFF03E3E, 8, 18, 2, 3)
    blob(n(0xFF3B82F6, 0xFF27407A), 16, 27, 2)
    // Patito sobre repisa
    r(0xFFFFD12E, 62, 17, 5, 4); r(0xFFFFD12E, 65, 15, 3, 3); r(0xFFF97316, 68, 16, 2, 1); r(n(0xFF7A3E1E, 0xFF261A34), 60, 21, 14, 2)
    // Linterna
    r(n(0xFF22242E, 0xFF141420), 70, 27, 4, 6); r(n(0xFFFFB020, 0xFFFFD76A), 71, 29, 2, 3)
    if (night) blob(0x24FFB020, 71, 30, 20)
    // Piso y alfombra
    r(n(0xFFC47A42, 0xFF3E2A30), 0, 40, W, 12)
    y = 43
    while (y < H) { r(n(0xFFA25E2C, 0xFF2E1E26), 0, y, W, 1); y += 4 }
    for (i in 0 until 10) r(n(0xFFA25E2C, 0xFF2E1E26), (i * 17 + 4) % W, 40 + (i % 3) * 4, 1, 3)
    r(n(0xFF1FA39B, 0xFF17506A), 18, 45, 44, 6); r(n(0xFFF2B33D, 0xFF6E5634), 20, 46, 40, 4); r(n(0xFF1FA39B, 0xFF17506A), 22, 47, 36, 2)
    // Rascador
    r(n(0xFF8A6233, 0xFF4A3A30), 66, 38, 10, 2); r(n(0xFFD9C08A, 0xFF6A5A48), 69, 27, 4, 12)
    y = 28
    while (y < 38) { r(n(0xFFA8894F, 0xFF4A3E34), 69, y, 4, 1); y += 2 }
}

private fun ScenePainter.thorBeach(f: Int) {
    grad(if (night) TH_SKY_BEACH_NIGHT else TH_SKY_BEACH_DAY, 0, 24); stars(f, 18)
    if (night) r(0xFFFFF3C4, 58, 4, 6, 6) else { blob(0x4DFFE278, 60, 7, 7); blob(0xFFFFE066, 60, 7, 4) }
    // Isla lejana
    r(n(0xFF2FA36B, 0xFF1A3A5A), 6, 21, 16, 3); r(n(0xFF46C47F, 0xFF22486A), 10, 19, 8, 2)
    // Mar con olas y reflejo de la luna
    r(n(0xFF06B6D4, 0xFF123C7A), 0, 24, W, 8); r(n(0xFF0891B2, 0xFF0E2F66), 0, 32, W, 6); r(n(0xFF5EEAD4, 0xFF2A5A9A), 0, 24, W, 1)
    for (k in 0 until 12) r(n(0xFFE6FFFB, 0xFF5A82BA), wrap(k * 11 + (f shr 1) * (if (k % 2 == 1) 1 else -1), W), 27 + (k % 4) * 3, 3, 1)
    if (night) for (j in 0 until 5) r(0xFFBFC9E8, 59 + (f + j) % 2, 26 + j * 2, 5, 1)
    // Arena y espuma
    r(n(0xFFFFD98A, 0xFF5A4E58), 0, 38, W, 14); dith(n(0xFFF4BE62, 0xFF4A3F4C), 0, 41, W, H - 41, 41)
    for (x in 0 until W) r(n(0xFFFFFFFF, 0xFF8AA0C8), x, 37 + rnd(sin(x / 6.0 + f / 4.0)), 1, 1)
    // Palmera
    for (k in 0 until 14) r(n(0xFF7A4A26, 0xFF3A2A30), 63 + (k shr 2), 24 + k, 3, 1)
    val b = (f shr 2) % 2; val leaf = n(0xFF22B573, 0xFF1D5A4A)
    r(leaf, 54 + b, 20, 10, 2); r(leaf, 66 - b, 20, 10, 2); r(leaf, 58, 18, 10, 2); r(leaf, 52 + b, 22, 4, 2); r(leaf, 74 - b, 22, 4, 2)
    r(0xFF7A4A26, 62, 22, 2, 2); r(0xFF7A4A26, 66, 22, 2, 2)
    // Sombrilla
    r(n(0xFFFFFFFF, 0xFF8A84A0), 18, 26, 1, 17); r(n(0xFFF03E3E, 0xFF7A2A40), 11, 26, 15, 2); r(n(0xFFFFFFFF, 0xFF8A84A0), 15, 26, 3, 2)
    r(n(0xFFFFD12E, 0xFF8A7A40), 21, 26, 3, 2); r(n(0xFFF03E3E, 0xFF7A2A40), 13, 25, 11, 1)
    // Castillo de arena y cangrejo
    r(n(0xFFE8B458, 0xFF5A4A44), 36, 42, 8, 5); r(n(0xFFE8B458, 0xFF5A4A44), 38, 39, 4, 3); r(0xFFEC4899, 39, 36, 1, 3)
    val cx = wrap(f shr 1, W + 10) - 5; val l = f % 2
    r(n(0xFFF03E3E, 0xFF8A3040), cx, 48, 4, 2); r(0xFF22243A, cx, 47, 1, 1); r(0xFF22243A, cx + 3, 47, 1, 1)
    r(n(0xFFC92A2A, 0xFF6A2030), cx - 1, 49 + l, 1, 1); r(n(0xFFC92A2A, 0xFF6A2030), cx + 4, 50 - l, 1, 1)
}

private fun ScenePainter.thorJungle(f: Int) {
    grad(if (night) TH_SKY_JUNGLE_NIGHT else TH_SKY_JUNGLE_DAY, 0, 40)
    var x = 0
    while (x < W) { blob(n(0xFF169A5E, 0xFF0C3440), x + 4, 14 + x % 5, 7); x += 9 }
    // Cascada con poza
    r(n(0xFF7E8AA8, 0xFF2A3448), 58, 8, 18, 30); r(n(0xFFA4B0C8, 0xFF3A4660), 58, 8, 3, 30); r(n(0xFF38BDF8, 0xFF23528A), 63, 8, 9, 30)
    for (k in 0 until 7) r(n(0xFFFFFFFF, 0xFF7AA0D0), 64 + (k % 3) * 3, 8 + wrap(f * 2 + k * 5, 28), 1, 3)
    r(n(0xFF06B6D4, 0xFF17406F), 50, 37, 30, 5)
    for (i in 0 until 8) r(n(0xFFFFFFFF, 0xFF7AA0D0), 60 + i * 2, 37 + (f + i) % 2, 1, 1)
    // Troncos y copa
    r(n(0xFF7A4A26, 0xFF2E2230), 5, 0, 6, 41); r(n(0xFFA06A3A, 0xFF40303E), 6, 0, 1, 41); r(n(0xFF7A4A26, 0xFF2E2230), 40, 0, 5, 39); r(n(0xFF0E6E48, 0xFF082830), 0, 0, W, 5)
    x = 0
    while (x < W) { r(n(0xFF22B573, 0xFF124A44), x, 4 + x % 3, 4, 2); x += 5 }
    // Lianas
    for (v in TH_VINES_X) {
        val len = 12 + v % 7; val b = if (((f shr 2) + v) % 4 < 2) 0 else 1
        for (k in 0 until len) {
            val sx = v + (if (k * 2 > len) b else 0)
            r(n(0xFF2F9E44, 0xFF1D5A4A), sx, k, 1, 1)
            if (k % 3 == 2) r(n(0xFF8CE99A, 0xFF2C7A5C), sx + 1, k, 1, 1)
        }
    }
    // Loro
    val peck = (f shr 3) % 6 == 0
    r(n(0xFF7A4A26, 0xFF2E2230), 24, 19, 16, 1); r(n(0xFFF03E3E, 0xFF7A2A40), 30, 14, 4, 5); r(n(0xFF2563EB, 0xFF25346E), 29, 16, 2, 3); r(n(0xFFFACC15, 0xFF7E6A30), 32, 18, 2, 2)
    r(0xFFFFD12E, 34, 15 + (if (peck) 1 else 0), 1, 1); r(0xFF22243A, 32, 15, 1, 1)
    // Suelo, flores y arbustos
    r(n(0xFF3E9B3A, 0xFF174A36), 0, 41, W, 11)
    x = 0
    while (x < W) { r(n(0xFF6BCB4F, 0xFF1F6050), x, 41 + (if (x % 4 == 0) 0 else 1), 1, 1); x += 2 }
    for (i in TH_JUNGLE_FLOWER_COL.indices) {
        val fx = TH_JUNGLE_FLOWERS[i * 2]; val fy = TH_JUNGLE_FLOWERS[i * 2 + 1]; val col = n(TH_JUNGLE_FLOWER_COL[i], 0xFF6A5A9A)
        r(col, fx, fy + 1, 3, 1); r(col, fx + 1, fy, 1, 3); r(0xFFFFE066, fx + 1, fy + 1, 1, 1)
    }
    var i = 0
    while (i < TH_JUNGLE_BUSHES.size) {
        val bx = TH_JUNGLE_BUSHES[i]; val by = TH_JUNGLE_BUSHES[i + 1]
        r(n(0xFF128A3E, 0xFF0E3A30), bx, by, 10, 2); r(n(0xFF128A3E, 0xFF0E3A30), bx + 2, by - 1, 6, 1)
        i += 2
    }
    if (night) for (k in 0 until 8) { val t = (f + k * 5) % 14; if (t < 9) r(0xFFFFF27A, 4 + k * 10 + t % 4, 18 + (t shr 1) % 8, 1, 1) }
}

private fun ScenePainter.thorSpace(f: Int) {
    r(n(0xFF3B4C7A, 0xFF191C3C), 0, 0, W, 40)
    var x = 0
    while (x < W) { r(n(0xFF2C3A62, 0xFF12142E), x, 0, 1, 40); x += 10 }
    var y = 0
    while (y < 40) { r(n(0xFF2C3A62, 0xFF12142E), 0, y, W, 1); y += 10 }
    x = 0
    while (x < W) { r(if ((x shr 2) % 2 == 1) n(0xFF38BDF8, 0xFFEF4444) else n(0xFFA855F7, 0xFF7F1D1D), x, 1, 3, 1); x += 4 }
    // Ventanal con nebulosas, estrellas y planeta
    blob(n(0xFFA8B4D0, 0xFF55608A), 40, 19, 14); blob(0xFF080B24, 40, 19, 12); blob(0xFF2A1466, 35, 14, 5); blob(0xFF14366A, 46, 24, 5)
    for (i in 0 until 24) {
        val sx = 28 + wrap(i * 29 - f * (1 + i % 3), 25); val sy = 7 + (i * 13) % 25
        if ((sx - 40) * (sx - 40) + (sy - 19) * (sy - 19) < 135) r(if (i % 4 != 0) 0xFFFFFFFF else 0xFFBFDBFE, sx, sy, 1, 1)
    }
    blob(0xFFF97316, 45, 14, 4); r(0xFFFDBA74, 43, 12, 3, 1); r(0xFFC2410C, 44, 16, 5, 1); r(0xFFFDE68A, 40, 14, 11, 1)
    // Consolas
    for (cx in CONSOLES_X) {
        r(n(0xFF1E2748, 0xFF0E1128), cx, 28, 18, 12); r(n(0xFF5A6A9A, 0xFF2A3050), cx, 28, 18, 1)
        for (k in 0 until 5) r(if (((f shr 1) + k + cx) % 3 != 0) TH_CONSOLE_LIGHTS[k] else 0xFF2A3050, cx + 2 + k * 3, 30, 2, 1)
        r(0xFF083B3E, cx + 3, 33, 12, 5)
        for (k in 0 until 12) r(0xFF5EEAD4, cx + 3 + k, 35 + rnd(sin((k + f) / 2.0)), 1, 1)
    }
    // Piso y alarma
    r(n(0xFF58658C, 0xFF2A2E50), 0, 40, W, 12); dith(n(0xFF7D8BB4, 0xFF3A4068), 0, 40, W, 1)
    x = 0
    while (x < W) { r(n(0xFF3E4A70, 0xFF1E2240), x, 41, 1, 11); x += 4 }
    r(n(0xFFFACC15, 0xFF7E6A30), 22, 49, 36, 1)
    if (night && f % 16 < 8) r(0x29EF4444, 0, 0, W, 40)
}

/** Dibuja el fondo [id] de Thor en la escena de 80×52 celdas con origen ([x0], [y0]) y celdas de [cell] px. */
internal fun DrawScope.drawThorBackground(id: String, frame: Int, night: Boolean, x0: Float, y0: Float, cell: Float) {
    val p = scenePainter
    p.ds = this; p.x0 = x0; p.y0 = y0; p.c = cell; p.night = night
    clipRect(x0, y0, x0 + W * cell, y0 + H * cell) {
        when (id) {
            "beach" -> p.thorBeach(frame)
            "jungle" -> p.thorJungle(frame)
            "space" -> p.thorSpace(frame)
            "coop" -> p.coop(frame)
            "farm" -> p.farm(frame)
            else -> p.thorRoom(frame)
        }
    }
    p.ds = null
}
