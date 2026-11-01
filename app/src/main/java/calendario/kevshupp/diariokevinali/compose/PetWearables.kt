package calendario.kevshupp.diariokevinali.compose

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Accesorios de premio y de compra. [r] dibuja en la grilla de cada mascota; Cuky la escala a 0,7 para igualar a Thor.
val SPIRIT_ACCESSORIES = setOf("aura", "fox_mask", "spirit_crown")
const val ACC_CAPE = "wizard_cape"

/** Va delante de la mascota. Las coordenadas parten de la cabeza: [cx] centro y [top] borde superior. */
internal inline fun drawWearable(id: String?, f: Int, thor: Boolean, cx: Int, top: Int, r: (Int, Int, Int, Int, Long) -> Unit) {
    when (id) {
        "spirit_crown" -> {
            val y = top + if (thor) 0 else 1
            r(cx - 6, y + 3, 13, 3, 0xFFD99A16); r(cx - 6, y + 3, 13, 1, 0xFFF7C948)
            r(cx - 6, y + 1, 1, 2, 0xFFF7C948); r(cx - 2, y + 1, 1, 2, 0xFFF7C948); r(cx + 2, y + 1, 1, 2, 0xFFF7C948); r(cx + 6, y + 1, 1, 2, 0xFFF7C948)
            r(cx, y, 1, 3, 0xFFF7C948); r(cx - 4, y + 4, 1, 1, 0xFF7FE7E0); r(cx, y + 4, 1, 1, 0xFF7FE7E0); r(cx + 4, y + 4, 1, 1, 0xFF7FE7E0)
        }
        "fox_mask" -> {
            val x = cx + if (thor) 6 else 3
            val y = top + if (thor) 3 else 8
            val o = 0xFF3A2315; val w = 0xFFFFF8EC; val red = 0xFFC62828; val a = 0xFFE8681C
            r(x, y, 2, 3, o); r(x + 7, y, 2, 3, o); r(x + 1, y + 1, 1, 2, a); r(x + 7, y + 1, 1, 2, a)
            r(x, y + 3, 9, 4, o); r(x + 1, y + 7, 7, 1, o); r(x + 3, y + 8, 3, 1, o); r(x + 1, y + 3, 7, 4, w); r(x + 2, y + 7, 5, 1, w)
            r(x + 2, y + 4, 2, 1, 0xFF111111); r(x + 5, y + 4, 2, 1, 0xFF111111); r(x + 4, y + 6, 1, 2, red); r(x + 4, y + 3, 1, 1, red)
        }
        "scarf" -> {
            val x = cx - if (thor) 10 else 8
            val y = top + if (thor) 17 else 20
            val w = if (thor) 20 else 17
            r(x, y, w, 3, 0xFFDC2626)
            var i = 0
            while (i < w) { r(x + i, y, 1, 3, 0xFFFFFFFF); i += 4 }
            r(x + w - 5, y + 3, 4, 6, 0xFFDC2626); r(x + w - 5, y + 6, 4, 1, 0xFFFFFFFF)
        }
        "headphones" -> {
            val y = top + if (thor) 4 else 11
            val h = if (thor) 11 else 10
            val d = if (thor) 8 else 5
            r(cx - h + 1, y, 2 * h - 1, 1, 0xFF1F2937); r(cx - h, y + 1, 1, d, 0xFF1F2937); r(cx + h, y + 1, 1, d, 0xFF1F2937)
            r(cx - h - 2, y + d, 4, 5, 0xFFEC4899); r(cx + h - 1, y + d, 4, 5, 0xFFEC4899)
            r(cx - h - 1, y + d + 1, 1, 3, 0xFFFFFFFF); r(cx + h + 1, y + d + 1, 1, 3, 0xFFFFFFFF)
        }
        "chef_hat" -> {
            val y = top + if (thor) 1 else 6
            val o = 0xFF9CA3AF; val w = 0xFFFFFFFF
            r(cx - 6, y - 5, 13, 6, o); r(cx - 5, y - 4, 11, 4, w); r(cx - 4, y - 6, 3, 1, o); r(cx + 2, y - 6, 3, 1, o)
            r(cx - 5, y + 1, 11, 3, o); r(cx - 4, y + 1, 9, 2, w)
        }
        "viking" -> {
            val y = top + if (thor) 2 else 6
            val m = 0xFF9CA3AF; val d = 0xFF4B5563; val b = 0xFFF5E6C4
            r(cx - 6, y, 13, 4, d); r(cx - 5, y + 1, 11, 3, m); r(cx - 3, y - 2, 7, 2, d); r(cx - 2, y - 1, 5, 1, m); r(cx - 6, y + 4, 13, 1, 0xFFB45309)
            r(cx - 8, y, 2, 3, b); r(cx - 9, y - 2, 2, 2, b); r(cx - 9, y - 4, 1, 2, b); r(cx + 7, y, 2, 3, b); r(cx + 9, y - 2, 2, 2, b); r(cx + 10, y - 4, 1, 2, b)
        }
    }
}

/** Aura celeste que pulsa detrás de la mascota. */
internal inline fun drawAura(f: Int, cx: Int, cy: Int, rx0: Int, ry0: Int, r: (Int, Int, Int, Int, Long) -> Unit) {
    val pulse = sin(f / 4.0).roundToInt()
    val rx = rx0 + pulse; val ry = ry0 + pulse
    for (n in 0 until 57) {
        if ((n + f) % 5 == 0) continue
        val a = n * 2.0 * PI / 57
        val x = (cx + rx * cos(a)).toInt(); val y = (cy + ry * sin(a)).toInt()
        r(x, y, 2, 2, 0xFF237F97); r(x, y, 1, 1, 0xFF7FE7E0)
    }
    for (i in 0..4) {
        if ((f + i * 4) % 10 < 3) continue
        val a = i * 2.0 * PI / 5 - 1.2
        val x = (cx + rx * cos(a)).toInt(); val y = (cy + ry * sin(a)).toInt()
        r(x, y, 1, 3, 0xFFFFFFFF); r(x - 1, y + 1, 3, 1, 0xFFFFFFFF)
    }
}

/** Capa de lado para Thor, sobre el lomo. */
internal inline fun drawCapeSide(f: Int, dx: Int, dy: Int, r: (Int, Int, Int, Int, Long) -> Unit) {
    val wave = if (f % 8 < 4) 0 else 1
    for (i in 0 until 16) {
        val right = 21 - i / 5
        val left = right - 4 - (i * 9) / 10
        r(dx + left, dy + 29 + i, right - left, 1, 0xFF6D28D9)
        r(dx + left, dy + 29 + i, 1, 1, 0xFF3B1578); r(dx + right - 1, dy + 29 + i, 1, 1, 0xFF8B5CF6)
    }
    var x = 2
    while (x < 20) { r(dx + x, dy + 45, 2, 1 + (x + wave) % 2, 0xFF3B1578); x += 3 }
    r(dx + 17, dy + 28, 5, 2, 0xFFFACC15)
    r(dx + 12, dy + 35, 1, 1, 0xFFFDE047); r(dx + 9, dy + 40, 1, 1, 0xFFFDE047); r(dx + 15, dy + 41, 1, 1, 0xFFFDE047)
}

/** Capa que asoma detrás de Cuky. */
internal inline fun drawCapeBack(f: Int, dx: Int, dy: Int, r: (Int, Int, Int, Int, Long) -> Unit) {
    val wave = if (f % 8 < 4) 0 else 1
    for (i in 0 until 12) {
        val half = 7 + i / 2
        r(dx + 13 - half, dy + 13 + i, half * 2 + 1, 1, 0xFF6D28D9)
        r(dx + 13 - half, dy + 13 + i, 1, 1, 0xFF3B1578); r(dx + 13 + half, dy + 13 + i, 1, 1, 0xFF3B1578)
    }
    var x = 1
    while (x < 26) { r(dx + x, dy + 25, 2, 1 + (x + wave) % 2, 0xFF3B1578); x += 3 }
}
