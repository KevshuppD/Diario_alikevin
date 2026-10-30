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

private val SKY_FARM_DAY = longArrayOf(0xFF9FD3EC, 0xFFB9E0F0, 0xFFD3ECF5)
private val SKY_FARM_NIGHT = longArrayOf(0xFF121633, 0xFF1A1F42, 0xFF262B55)
private val SKY_MEADOW_DAY = longArrayOf(0xFFF7C6D6, 0xFFF9D7E2, 0xFFFCE7EF)
private val SKY_MEADOW_NIGHT = longArrayOf(0xFF1C1636, 0xFF251D45, 0xFF30265A)
private val SKY_RAIN_DAY = longArrayOf(0xFF8EA3B5, 0xFF9FB3C4, 0xFFB3C4D2)
private val SKY_RAIN_NIGHT = longArrayOf(0xFF10161F, 0xFF151D29, 0xFF1B2433)
private val SKY_SUNSET_DAY = longArrayOf(0xFFF59E0B, 0xFFF2795B, 0xFFEC6A8A, 0xFFC2508F)
private val SKY_SUNSET_NIGHT = longArrayOf(0xFF0F1229, 0xFF161A38, 0xFF1E2247, 0xFF2A2C58)
private val RAINBOW = longArrayOf(0xFFEF4444, 0xFFF59E0B, 0xFFFACC15, 0xFF22C55E, 0xFF3B82F6)

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
    fun coop(f: Int) {
        r(n(0xFF8A5A36, 0xFF3B2A22), 0, 0, W, 44)
        var x = 0
        while (x < W) {
            r(n(0xFF744A2C, 0xFF2F211A), x, 6, 1, 38)
            for (k in 0 until 4) r(n(0xFF7D5030, 0xFF33241C), x + 3, 10 + k * 9 + (x % 5), 2, 1)
            x += 7
        }
        r(n(0xFF5E3A22, 0xFF24180F), 0, 3, W, 3); r(n(0xFF4A2D1A, 0xFF1C120B), 0, 6, W, 1)
        r(n(0xFF5E3A22, 0xFF24180F), 18, 0, 3, 6); r(n(0xFF5E3A22, 0xFF24180F), 58, 0, 3, 6)
        r(0xFF4A2D1A, 54, 11, 18, 14); r(n(0xFFBFE3F2, 0xFF1E2440), 55, 12, 16, 12)
        if (!night) { r(0xFFFFFFFF, 57, 14, 4, 1); r(0xFFE6F4FA, 60, 17, 6, 1) } else { r(0xFFF3E6C8, 66, 14, 3, 3); r(0xFFFFF3C4, 58, 15, 1, 1) }
        r(0xFF4A2D1A, 62, 12, 1, 12); r(0xFF4A2D1A, 55, 17, 16, 1)
        if (!night) for (j in 0 until 20) r(0x29FFECAA, 54 - j, 25 + j, 18, 1)
        r(0xFF4A2D1A, 4, 24, 22, 1); r(0xFF5E3A22, 5, 25, 1, 4); r(0xFF5E3A22, 24, 25, 1, 4)
        r(0xFF8A6326, 9, 21, 9, 3); dith(0xFFC99A4B, 9, 21, 9, 3); r(0xFFF6F0E2, 11, 20, 2, 2); r(0xFFF6F0E2, 14, 20, 2, 2)
        val sw = LAMP_SWING[(f shr 1) % 6]
        r(0xFF2B1A12, 38 + sw, 6, 1, 4); r(0xFF2B1A12, 36 + sw, 10, 5, 1); r(n(0xFFF2B33D, 0xFFF59E0B), 37 + sw, 11, 3, 2)
        if (night) { r(0x29F59E0B, 24 + sw, 13, 28, 35); r(0x1FF59E0B, 30 + sw, 13, 16, 35) }
        for (bx in HAY_BALES_X) {
            val bw = 15
            r(n(0xFFC99A4B, 0xFF6E5634), bx, 35, bw, 9)
            for (j in 0 until 3) r(n(0xFFA87D35, 0xFF584428), bx, 37 + j * 3, bw, 1)
            r(n(0xFF7A3A1E, 0xFF3B2A22), bx + 4, 35, 1, 9); r(n(0xFF7A3A1E, 0xFF3B2A22), bx + bw - 5, 35, 1, 9)
        }
        r(n(0xFFC99A4B, 0xFF5E4630), 0, 44, W, 8); dith(n(0xFFE2B868, 0xFF6E5634), 0, 44, W, 2)
        for (i in 0 until 14) r(n(0xFFA87D35, 0xFF4A3826), (i * 13 + 4) % W, 46 + (i % 3) * 2, 3, 1)
        if (!night) for (i in 0 until 5) { val t = (f + i * 9) % 30; r(0xFFFFF3C4, 48 - i * 3 - (t shr 2), 26 + t % 16, 1, 1) }
    }

    fun farm(f: Int) {
        grad(if (night) SKY_FARM_NIGHT else SKY_FARM_DAY, 0, 26); stars(f, 22); moonOrSun(64, 4)
        val cx = wrap(f shr 2, W + 30) - 15
        cloud(cx, 6, n(0xFFFFFFFF, 0xFF3A4170), n(0xFFDCEBF3, 0xFF2C3260))
        cloud(wrap(cx + 45, W + 30) - 15, 11, n(0xFFFFFFFF, 0xFF3A4170), n(0xFFDCEBF3, 0xFF2C3260))
        r(n(0xFF8FBF6A, 0xFF24402A), 0, 22, W, 8)
        for (x in 0 until W) r(n(0xFF8FBF6A, 0xFF24402A), x, 20 + rnd(2 * sin(x / 9.0)), 1, 3)
        r(n(0xFF6FA64A, 0xFF1D3622), 0, 27, W, 6)
        var x = 2
        while (x < W) {
            val b = if (((f shr 1) + x) % 8 < 4) 0 else 1
            r(n(0xFF4E7F32, 0xFF2C4A2A), x + b, 16, 1, 22)
            r(n(0xFF6FA64A, 0xFF3A5E36), x + b - 2, 22, 2, 1); r(n(0xFF6FA64A, 0xFF3A5E36), x + b + 1, 26, 2, 1); r(n(0xFF6FA64A, 0xFF3A5E36), x + b - 2, 30, 2, 1)
            r(n(0xFFF2C14E, 0xFF8A7A3A), x + b + 1, 19, 1, 3); r(n(0xFFD9A62E, 0xFF6E6030), x + b + 1, 22, 1, 1); r(n(0xFFC2D96A, 0xFF4A5E30), x + b, 15, 1, 1)
            x += 6
        }
        for (g in SUNFLOWERS_X) {
            val b = (f shr 2) % 2
            r(0xFF3E6B2E, g, 20, 1, 16); r(0xFF3E6B2E, g - 2, 27, 2, 1)
            r(n(0xFFF2B33D, 0xFFA37A2A), g - 2 + b, 15, 5, 5); r(n(0xFFF2B33D, 0xFFA37A2A), g - 1 + b, 14, 3, 7)
            r(0xFF5B3A1E, g - 1 + b, 16, 3, 3); dith(0xFF7A4A26, g - 1 + b, 16, 3, 3)
        }
        r(n(0xFF7A4F2C, 0xFF3B2A1E), 0, 40, W, 12)
        var y = 42
        while (y < H) { r(n(0xFF64401F, 0xFF2E2016), 0, y, W, 1); y += 3 }
        dith(n(0xFF8A5E36, 0xFF45321F), 0, 41, W, 1)
        r(n(0xFFA0703F, 0xFF4A3624), 0, 33, W, 1); r(n(0xFFA0703F, 0xFF4A3624), 0, 37, W, 1)
        x = 3
        while (x < W) { r(n(0xFF7A4F2C, 0xFF3B2A1E), x, 31, 2, 10); r(n(0xFFB07F4A, 0xFF55402C), x, 31, 1, 1); x += 10 }
        grassTufts(41, n(0xFF5E8F3E, 0xFF2C4A2A), f, 7)
        if (night) for (i in 0 until 5) { val t = (f + i * 5) % 14; if (t < 9) r(0xFFF4F07A, 6 + i * 15 + (t % 4), 18 + ((t shr 1) % 5), 1, 1) }
    }

    fun meadow(f: Int) {
        grad(if (night) SKY_MEADOW_NIGHT else SKY_MEADOW_DAY, 0, 24); stars(f, 20); moonOrSun(58, 5)
        cloud(wrap(f shr 3, W + 30) - 15, 8, n(0xFFFFFFFF, 0xFF41376A), n(0xFFF3DCE6, 0xFF332A58))
        for (x in 0 until W) r(n(0xFFB5D98A, 0xFF2E3F3A), x, 20 + rnd(3 * sin(x / 11.0 + 1)), 1, 10)
        for (x in 0 until W) r(n(0xFF9BCB6B, 0xFF2C4430), x, 26 + rnd(2 * sin(x / 7.0)), 1, 8)
        r(n(0xFF7A4A26, 0xFF3A2618), 11, 14, 3, 22); r(n(0xFF5E3A22, 0xFF2B1C12), 12, 20, 1, 12)
        r(n(0xFF5E8F3E, 0xFF24402A), 2, 4, 22, 12); r(n(0xFF5E8F3E, 0xFF24402A), 5, 2, 16, 16)
        dith(n(0xFF7DB35A, 0xFF2E5034), 4, 3, 18, 8); r(n(0xFF4E7F32, 0xFF1D3622), 3, 13, 20, 3)
        var i = 0
        while (i < APPLES.size) { r(n(0xFFEF4444, 0xFF7A2A2A), APPLES[i], APPLES[i + 1], 2, 2); i += 2 }
        r(n(0xFF7DB35A, 0xFF26402C), 0, 34, W, 18); dith(n(0xFF8FC468, 0xFF2C4A32), 0, 34, W, 2)
        var x = 0
        while (x < W) { val b = if (((f shr 1) + x) % 4 < 2) 0 else 1; r(n(0xFF5E8F3E, 0xFF24402A), x + b, 32, 1, 3); x += 3 }
        for (k in FLOWERS_COL.indices) {
            val fx = FLOWERS_XY[k * 2]; val fy = FLOWERS_XY[k * 2 + 1]
            val cc = if (night) 0xFF6B5A7A else FLOWERS_COL[k]
            r(0xFF3E6B2E, fx + 1, fy + 2, 1, 2); r(cc, fx, fy + 1, 3, 1); r(cc, fx + 1, fy, 1, 3); r(0xFFF2B33D, fx + 1, fy + 1, 1, 1)
        }
        if (!night) {
            for (k in 0..1) {
                val t = (f + k * 20) % 40
                val bx = wrap(t * 2 + k * 30, W)
                val by = 10 + k * 6 + BUTTERFLY_BOB[(f shr 1) % 4]
                val col = if (k == 1) 0xFF2563EB else 0xFFF59E0B
                val op = (f shr 1) % 2
                r(col, bx, by - op, 1, 1 + op); r(col, bx + 2, by - op, 1, 1 + op); r(0xFF2B1A12, bx + 1, by, 1, 2)
            }
            val bx = wrap(70 - f, W); val by = 30 + ((f shr 1) % 3)
            r(0xFFF2B33D, bx, by, 2, 1); r(0xFF2B1A12, bx + 1, by, 1, 1); r(0xFFE0F2FE, bx, by - 1, 1, 1)
        } else {
            for (k in 0 until 4) { val t = (f + k * 7) % 16; if (t < 10) r(0xFFF4F07A, 8 + k * 18 + (t % 3), 30 + ((t shr 1) % 4), 1, 1) }
        }
    }

    fun rain(f: Int) {
        grad(if (night) SKY_RAIN_NIGHT else SKY_RAIN_DAY, 0, 30)
        cloud(wrap(f shr 3, W + 30) - 15, 3, n(0xFF6E8396, 0xFF26303F), n(0xFF5E7286, 0xFF1E2733))
        cloud(wrap((f shr 3) + 40, W + 30) - 15, 8, n(0xFF7E93A6, 0xFF2A3442), n(0xFF6E8396, 0xFF222B38))
        cloud(wrap((f shr 3) + 22, W + 30) - 15, 1, n(0xFF6E8396, 0xFF26303F), n(0xFF5E7286, 0xFF1E2733))
        if (f % 64 > 40 && !night) {
            for (i in RAINBOW.indices) for (x in 0 until 34) {
                val y = rnd(28 - sqrt(max(0.0, 289.0 - (x - 17) * (x - 17))) * 0.9) + i
                r(RAINBOW[i], 46 + x, y, 1, 1)
            }
        }
        for (x in 0 until W) r(n(0xFF7E9E6A, 0xFF1F2E25), x, 26 + rnd(2 * sin(x / 10.0)), 1, 6)
        r(n(0xFF6E8F57, 0xFF25331F), 0, 32, W, 20); dith(n(0xFF5E7E4A, 0xFF1F2A1A), 0, 40, W, 12, 1)
        var i = 0
        while (i < PUDDLES.size) {
            val px = PUDDLES[i]; val py = PUDDLES[i + 1]; val pw = PUDDLES[i + 2]
            r(n(0xFF8FB6D9, 0xFF2F4560), px, py, pw, 3); r(n(0xFFB9D4EC, 0xFF3C5878), px + 2, py, pw - 6, 1)
            val t = (f + px) % 10
            if (t < 6) { r(n(0xFFE6F0FA, 0xFF5C7EA4), px + pw / 2 - t, py + 1, 1, 1); r(n(0xFFE6F0FA, 0xFF5C7EA4), px + pw / 2 + t, py + 1, 1, 1) }
            i += 3
        }
        i = 0
        while (i < MUSHROOMS.size) {
            val mx = MUSHROOMS[i]; val my = MUSHROOMS[i + 1]
            r(0xFFF6F0E2, mx + 1, my, 2, 3); r(0xFFDC2626, mx - 1, my - 3, 6, 2); r(0xFFDC2626, mx, my - 4, 4, 1)
            r(0xFFFFFFFF, mx, my - 3, 1, 1); r(0xFFFFFFFF, mx + 3, my - 4, 1, 1)
            i += 2
        }
        val sx = wrap(76 - (f shr 2), W)
        r(0xFFC2703F, sx, 46, 3, 2); r(0xFFA8532B, sx + 1, 46, 1, 1); r(0xFFB3C4D2, sx - 2, 47, 2, 1); r(0xFF2B1A12, sx - 2, 45, 1, 2)
        val drop = n(0xD9E6F0FA, 0xD96C8EB5)
        for (k in 0 until 34) {
            val x = wrap(k * 13 + 3 - f * 2, W); val y = wrap(f * 4 + k * 7, H)
            r(drop, x, y, 1, 2); r(drop, x - 1, y + 2, 1, 1)
        }
    }

    fun sunset(f: Int) {
        grad(if (night) SKY_SUNSET_NIGHT else SKY_SUNSET_DAY, 0, 34); stars(f, 26)
        if (night) { r(0xFFF3E6C8, 60, 4, 7, 7); r(0xFFD9CCAE, 62, 6, 2, 2) }
        else { r(0xFFFFE08A, 54, 18, 12, 10); r(0xFFFFE08A, 56, 16, 8, 14); r(0xFFFFF3C4, 57, 18, 6, 6); dith(0xFFFFE08A, 50, 26, 20, 2) }
        if (!night) {
            val bx = wrap(f shr 1, W + 20) - 10
            var i = 0
            while (i < BIRDS.size) {
                val ox = BIRDS[i]; val oy = BIRDS[i + 1]; val w = (f shr 1) % 2
                r(0xFF2B1A12, bx + ox, oy + 8 + w, 1, 1); r(0xFF2B1A12, bx + ox + 1, oy + 9 - w, 1, 1); r(0xFF2B1A12, bx + ox + 2, oy + 8 + w, 1, 1)
                i += 2
            }
        }
        for (x in 0 until W) r(n(0xFFB0502E, 0xFF1E1A30), x, 30 + rnd(2 * sin(x / 8.0)), 1, 6)
        r(n(0xFF9CA3AF, 0xFF3A3F52), 27, 14, 7, 22); r(n(0xFFC4C9D0, 0xFF4A5066), 28, 14, 2, 22)
        r(n(0xFF6B7280, 0xFF2A2E3E), 27, 11, 7, 3); r(n(0xFF6B7280, 0xFF2A2E3E), 28, 10, 5, 1)
        r(n(0xFFA8322B, 0xFF4A1E1E), 4, 18, 22, 18); r(n(0xFF7A2420, 0xFF2B1414), 2, 15, 26, 3)
        r(n(0xFF7A2420, 0xFF2B1414), 5, 12, 20, 3); r(n(0xFF7A2420, 0xFF2B1414), 9, 10, 12, 2)
        var y = 13
        while (y < 18) { r(n(0xFF5E1A17, 0xFF200E0E), 3, y, 24, 1); y += 2 }
        var x = 6
        while (x < 25) { r(n(0xFF8E2A25, 0xFF3A1818), x, 19, 1, 17); x += 4 }
        r(0xFFF3E6C8, 10, 25, 10, 11); r(n(0xFFA8322B, 0xFF4A1E1E), 11, 26, 8, 9)
        for (k in 0 until 9) { r(0xFFF3E6C8, 11 + k, 26 + k, 1, 1); r(0xFFF3E6C8, 19 - k, 26 + k, 1, 1) }
        r(0xFFF3E6C8, 12, 19, 6, 4); r(if (night) 0xFFF2B33D else 0xFF5E1A17, 13, 20, 4, 2)
        r(n(0xFFE6D3B3, 0xFF4A4458), 70, 16, 2, 20)
        val blades = if ((f shr 1) % 2 == 0) BLADES_A else BLADES_B
        var b = 0
        while (b < blades.size) {
            val vx = blades[b]; val vy = blades[b + 1]
            for (k in 1..6) { val t = k / 6.0; r(n(0xFFF6F0E2, 0xFF6B6480), 71 + rnd(vx * t), 15 + rnd(vy * t), 1, 1) }
            b += 2
        }
        r(0xFF2B1A12, 70, 14, 2, 2)
        r(n(0xFF8A5A36, 0xFF2A2030), 0, 36, W, 16); dith(n(0xFF9E6B42, 0xFF332838), 0, 36, W, 2)
        for (i in 0 until 10) r(n(0xFF6E4426, 0xFF221A28), (i * 17 + 5) % W, 40 + (i % 4) * 3, 4, 1)
        r(n(0xFF6E4426, 0xFF2B1A12), 0, 38, W, 1); r(n(0xFF6E4426, 0xFF2B1A12), 0, 41, W, 1)
        x = 36
        while (x < W) { r(n(0xFF6E4426, 0xFF2B1A12), x, 36, 2, 8); x += 7 }
        grassTufts(46, n(0xFF7A6A2E, 0xFF3A3428), f, 9)
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
