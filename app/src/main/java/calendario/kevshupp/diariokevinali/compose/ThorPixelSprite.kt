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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import calendario.kevshupp.diariokevinali.Pet
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

// =====================================================================================
// Sprite procedural de Thor (angora turco blanco, ojo ámbar y ojo azul), port del prototipo
// "Thor en Movimiento". Echado por defecto; de pie (versión 1 "Alto y gordito") para caminar,
// jugar y bañarse. Matrices a nivel superior: cero asignaciones en el ciclo de dibujo.
// =====================================================================================

private class ThorPart(val x: Int, val y: Int, val rows: Array<String>)

private val TH_TAIL = ThorPart(-3, 12, arrayOf(
    ".....OOO.........",
    "....OWWWO........",
    "...OWWWWO........",
    "...OWWWWO........",
    "..OWWWWWL........",
    "..OWWWWWL........",
    "..OWWWWWWL.......",
    ".OLWWWWWWL.......",
    "OWWWWWWWWWLLL....",
    "OWWWWWWWWWWWWL...",
    ".OWWWWsWWsWWWWL..",
    ".OWWWsWWWWWWWWSL.",
    "..OWWWWWWWWWSSWWL",
    "..OWWSWSWSWWTSSTL",
    "..OSSWSSSTSTSSSSL",
    "...OSTSSSTSSTSSL.",
    "...OSTTTTSSTSTL..",
    "....OOOSSSTSSL...",
    ".......OOOLLL...."
))
private val TH_BODY = ThorPart(1, 14, arrayOf(
    "..........OO.OL..........",
    ".........OWWOWWLLLL......",
    ".....OOOOWWWWWWWWWWL.....",
    "....LWWWWWWWWWWWWWWWL....",
    "...LWWWWWWWWWWWWWWWWWLL..",
    "..LWWWWWWWWWWWWWWWWWWWWL.",
    ".LWWWWWWWWWWWWWWWWWWWWWL.",
    ".LWWWWWWWWWWWWWWWWWWWWsWL",
    ".LWWWWWWWWWWWWWWWWWWWWWWL",
    "LWWWWWWWWWsWWssssWWWWWWWL",
    "LWWWWWWWWWWWWWWWsWSWWSSSL",
    "LWWsWWWWWWWSSSSWSSWSSWSL.",
    ".LWWWSSSSWWSWWSSSSWWSSL..",
    ".LSSWSWWSSSSSSSSTTTTSSL..",
    "..LSSWWSSTTTSSSSSSSSSSL..",
    "...LSSTSSSTTSSSSTTTTSL...",
    "....OOSSTSTTTSSTSTLLL....",
    "......OOOOOOOOOOOO......."
))
private val TH_RUFF = ThorPart(13, 10, arrayOf(
    "...........LL............",
    ".........LLWWLLLLL.......",
    "........LWWWWWWWWWLL.....",
    ".....LLLWWWWWWWWWWWWL....",
    "....LWWWWWWWWWWWWWWWWO...",
    "....LWWWWWWWWWWWWWWWWO...",
    "...LWWWWWWWWWWWWWWWWWWO..",
    "..LWWWWWWWWWWWWWWWWWWWWO.",
    ".LWWWWWWWWWWWWWWWWWWWWWO.",
    "LWWWWWWWWWWWWWWWWWsWWWsO.",
    "LWWWWWWWWWWWWWWWsWWWWWWO.",
    ".LWWWWWWWWWWWWWWWWWWWWsSO",
    ".LWWsWWWWsWWWWWWWWWSWSSSO",
    ".LWWsWWWWWssWWSSSWWSSWSSO",
    ".LWWssWWWWWSWWSWSSSWSWWO.",
    ".LWWWSSSWWSSWSSWSSSWSSTO.",
    "..LWWWSWSWWWSSSSSTSTSTSO.",
    "...LSSSSWWSTTSSSSSSTTLL..",
    "....LSSTTTTSSTTTTSTSSL...",
    ".....LLSSSTSTTTTSSTSL....",
    "......LSSSSSSSTSLLLL.....",
    ".......LLSTSOLOO.........",
    ".........OOO............."
))
private val TH_PAW_A = ThorPart(15, 25, arrayOf(
    "...LL........",
    "..LWWLLL.....",
    ".LWWWWWWLLLL.",
    "LWWWWWWWWWWWL",
    "LWWWsSSSSWWSL",
    "LWSSSSSSSTTSL",
    ".OOOTTSSTLOTO",
    "....OOOOO..O."
))
private val TH_PAW_B = ThorPart(25, 26, arrayOf(
    "..LLLLLLLL....",
    ".LWWWWWWWWLLO.",
    "LWWWWWWsWWWWWO",
    "LWWWWWWSSWWSWO",
    ".LWSWWSSTSSTSO",
    "..OOOOTSSTOOTO",
    "......OOOO..O."
))
private val TH_HEAD = ThorPart(14, 0, arrayOf(
    "...O...............O..",
    "..OWO.............OWO.",
    "..OWWO...........OWWO.",
    "..OWHWO.O.....O.OWHWO.",
    "..OHpWWOSOOOOOSOWWpHO.",
    "..OWpPWLWsWWWsLLWPpWO.",
    "..OWpPPWWWWsWWWWPPpWO.",
    "..OWpPPWWWWWWWWWPPpWO.",
    "...OLWWWWWWWWWWWWWLO..",
    "...OWOOOOWWWWWOOOOWO..",
    "...OWAKKAWWWWWBKKBWO..",
    "..OWWAKaAWWWWWBKbBWWO.",
    ".OWWWWaAWWWWWWWbBWWWWO",
    "OWWWWTWWWWNNNWWWWTWWWO",
    "OssSSSWWWWWNWWWWWSSSO.",
    ".LLsssWWWSWMWSWWWSssO.",
    "...LLsWWWWSWSWWWWsssL.",
    ".....LLWWWWWWWWWWLLL..",
    ".......LLWWWWLLLL.....",
    ".........LLLL........."
))
private val TH_ST_TAIL = ThorPart(-3, -3, arrayOf(
    "..........OOOOOO.",
    ".......OOOWWWWWWO",
    "......OWWWWWWWWWO",
    ".....OWWWWWWWWWO.",
    "....OWWWWWWWWWWWO",
    "...OWWWWWWWWWWWWO",
    "...OWWWWWWWWWWWWO",
    "..OWWWWWWWWWWWWWO",
    "..OWWWWWWWWWWWLO.",
    ".OWWWWWWWWWWWWO..",
    ".OWWWWWWWWWWWWO..",
    "OWWWWWWWWWWWWO...",
    "OWWWWWWWWWWsWO...",
    "OWWWWWWWWWWWWO...",
    ".OWWWWWWWWWLO....",
    ".OWWWsWWWWWO.....",
    ".OWWWWWWWSWWO....",
    "..OsWWWWSSSWO....",
    "..OWWSSSSSSWSO...",
    "..OWSWWSSWWSTL...",
    "..OSSSSWWTSTSSL..",
    "...OWWWTSSSSTSSL.",
    "....OLSSTTSTTSTL.",
    ".....OSSSSSSSSTSL",
    ".....OTSSTSTSSTTL",
    "......LLSSSSSSTSL",
    "........LSSSTTSL.",
    ".........LLSTSL..",
    "..........LTSL...",
    "...........LL...."
))
private val TH_ST_BODY = ThorPart(2, 14, arrayOf(
    ".........OOOOOOOOOO...........",
    "......LLOWWWWWWWWWWLLLL.......",
    "....LLWWWWWWWWWWWWWWWWWLLL....",
    "...LWWWWWWWWWWWWWWWWWWWWWWL...",
    "..LWWWWWWWWWWWWWWWWWWWWWWWWL..",
    ".LWWWWWWWWWWWWWWWWWWWWWWWWWWL.",
    ".LWWWWWWWWWWWWWWWWWWWWWWWWWWWL",
    ".LWWWWWWWWWWWWWWWWWWWWWWWWWsWL",
    "OWWWWWWWWWWWWWWWWWWWWWWsWWWWsL",
    "OWWWWsWWWWWWWWWsWWWWWSSSWWWSL.",
    "OWWWWWWsWsWSWWSWSWSSSSWWSSSSL.",
    "OsWSWWSSSWWSWWSWSSWWWSSTTTTL..",
    ".OWSSSSSSSWSWSSTTTSSSSSSSTL...",
    "..OWTTTTSTSSSTTSSTTTSTSSTSL...",
    "...OOTTTSSTSOTSTLTSSSSTTLL....",
    ".....OOSTOOO.OOOOTTOOOTSL.....",
    ".......OO........OO...OO......"
))
private val TH_ST_CHEST = ThorPart(17, 9, arrayOf(
    ".............OO.........",
    "..........OOOWWOO.......",
    "......OOOOWWWWWWWOO.....",
    ".....OWWWWWWWWWWWWWO....",
    "....OLWWWWWWWWWWWWWWO...",
    "..OOWWWWWWWWWWWWWWWWLO..",
    ".LWWWWWWWWWWWWWWWWWWWWO.",
    ".LWWWWWWWWWWWWWWWWWWWWWO",
    "LWWWWWWWWWWWWWWWWWWWWWWO",
    "LWWWWWWWWWWWWWWWWWWWWWsO",
    "LWWWWWWWWWWWWWWWWWWsWWWO",
    "LWWWWWWWWWWWWsWsWWWWWWWO",
    "LWWWWWWWWWWWWWWWWWWWWWWO",
    "LWWWsWWWWWWsWWWWWWSSSWO.",
    ".LWWWWsWWsWsWWSSSWSWSWO.",
    "..LWWWWWWWSWSSWSSWSWSSO.",
    "..LsWSSWWSWSSWSSSWSWSSO.",
    "...LWWWSSWSSWWWTTSSTSO..",
    "...LSWSSWWWTSSSSTSSTO...",
    "....LWTSTSSTSSTSSSSO....",
    ".....OOLSTSTTTSSSSO.....",
    "........OTTTSTTSOO......",
    ".........OOOOOOO........"
))
private val TH_KIT_TAIL = ThorPart(0, 12, arrayOf(
    "..OOO......",
    ".OWWWO.....",
    "OWWWWLLL...",
    "OWWWWWWWL..",
    "OWWWWWWWWL.",
    "OWWWWWWWWSL",
    "OWWWWWSWSWL",
    "OWWWSWSSSTL",
    ".OTSSTTSTL.",
    "..OSSSSSL..",
    "...OOOLL..."
))
private val TH_KIT_BODY = ThorPart(2, 12, arrayOf(
    "......OLLLLLL......",
    "....OOWWWWWWWLL....",
    "...LWWWWWWWWWWWLL..",
    "..LWWWWWWWWWWWWWWL.",
    ".LWWWWWWWWWWWWWWWL.",
    "LWWWWWWWWWWWWWWWWWL",
    "LWWWWWsWWWsWWsWWWWL",
    "LWWWWWWsWWWWWSWWWWL",
    ".LWWsWSSWSWWSWSSWL.",
    ".LWWSWWWWSSSSSTSTL.",
    "..OOTSTSSTTTTSSLL..",
    "....OOOSSTSLLLL....",
    ".......OOOO........"
))
private val TH_KIT_RUFF = ThorPart(10, 9, arrayOf(
    "....LLLLLL.....",
    "...LWWWWWWL....",
    "..LLWWWWWWWL...",
    ".LWWWWWWWWWWL..",
    ".LWWWWWWWWWWWO.",
    "LWWWWWWWWWWWWWO",
    "LWWWWWWWWWsWWsO",
    "LWWWWsWWWWWssWO",
    "LWWWWWWWWWSSWSO",
    "LWWWWWSSWSSWWWO",
    "LWSWWWSWSWSSSTO",
    ".LLSSWSTTTSTSL.",
    "..LSTTTTTSTSL..",
    "...LLSSTSTSL...",
    ".....LLLOLL...."
))
private val TH_KIT_PAW_A = ThorPart(10, 19, arrayOf(
    "...LL....",
    ".LLWWLLL.",
    "LWWWWWWWL",
    "LWWWWWWsL",
    "LSSSSSSTO",
    ".OOSSOOO.",
    "...OO...."
))
private val TH_KIT_PAW_B = ThorPart(18, 20, arrayOf(
    ".LLLLLLO.",
    "LWWWWWWWO",
    "LWWWWWWWO",
    "OSWSWWSTO",
    ".OOSSTOO.",
    "...OOO..."
))
private val TH_KIT_HEAD = ThorPart(9, 0, arrayOf(
    ".O...........O..",
    "OWO.........OWO.",
    "OWWO.......OWWO.",
    "OWPWOOOOOOOWPWO.",
    "OWPPWWWWWWWPPWO.",
    "OWPPPWWWWWPPPWO.",
    ".OWWWWWWWWWWWO..",
    "OOOOOWWWWWOOOOO.",
    "OAKKAWWWWWBKKBO.",
    "OAKaAWWWWWBKbBWO",
    "OWaAWWWWWWWbBWWO",
    "OWWWWWNNNWWWWWWO",
    ".LWWWWWMWWWWWLO.",
    ".LWWWWWWWLLLL...",
    "..LLLLLLL......."
))

private val TH_PAL = IntArray(128).also { p ->
    p['O'.code] = 0xFF7C829E.toInt()
    p['W'.code] = 0xFFFFFEFA.toInt()
    p['s'.code] = 0xFFEEF0F6.toInt()
    p['S'.code] = 0xFFD5D9E8.toInt()
    p['T'.code] = 0xFFB4BAD0.toInt()
    p['P'.code] = 0xFFF7B6C2.toInt()
    p['p'.code] = 0xFFE88FA3.toInt()
    p['A'.code] = 0xFFD9822B.toInt()
    p['a'.code] = 0xFFF0B868.toInt()
    p['B'.code] = 0xFF4F93C4.toInt()
    p['b'.code] = 0xFFA5D4EC.toInt()
    p['K'.code] = 0xFF2A2A3A.toInt()
    p['N'.code] = 0xFFEE8C98.toInt()
    p['M'.code] = 0xFFA9778A.toInt()
    p['H'.code] = 0xFFFFFFFF.toInt()
    p['L'.code] = 0xFFB2B8CE.toInt()
}
/** Al mirar a la izquierda el sprite se voltea: se cruzan los colores para que cada ojo siga en su lado. */
private val TH_PAL_FLIP = TH_PAL.copyOf().also { p ->
    p['A'.code] = TH_PAL['B'.code]; p['a'.code] = TH_PAL['b'.code]
    p['B'.code] = TH_PAL['A'.code]; p['b'.code] = TH_PAL['a'.code]
}
private const val TH_W = 52
private const val TH_OX = 5
private const val TH_OY = 13
private const val TH_GROUND = TH_OY + 33
private const val TH_UP = 5
private const val TH_SCENE_SCALE = 0.7f
private const val TH_SCENE_SHIFT_X = 7
private const val TH_SCENE_FEET_Y = 49
private const val TH_LEG_W = 5
private const val TH_FRAME_MS = 125L

private const val E_OPEN = 0
private const val E_CLOSED = 1
private const val E_HAPPY = 2
private const val E_GRUMPY = 3
private const val E_DOWN = 4

private const val V_NONE = 0
private const val V_WALK = 1
private const val V_PLAY = 2
private const val V_BATH = 3
private const val V_SHAKE = 4

private const val FX_NONE = 0
private const val FX_PURR = 1
private const val FX_SLEEP = 2
private const val FX_HUNGRY = 3

private const val C_O = 0xFF7C829E.toInt()
private const val C_W = 0xFFFFFEFA.toInt()
private const val C_S = 0xFFD5D9E8.toInt()
private const val C_T = 0xFFB4BAD0.toInt()
private const val C_K = 0xFF2A2A3A.toInt()
private const val C_SHADOW = 0x33323240
private const val C_WATER = 0xFF60A5FA.toInt()
private const val C_FOAM = 0xFFFFFFFF.toInt()
private const val C_FOAM_S = 0xFFD8ECFA.toInt()

// col, fila, tipo (0 polvo, 1 barro), nivel, cabeza (1) o cuerpo (0)
private val TH_DIRT = intArrayOf(
    6,20,0,1,0, 16,18,0,1,0, 29,22,0,1,0, 20,4,0,1,1, 32,14,0,1,1,
    9,23,1,2,0, 10,23,1,2,0, 24,24,1,2,0, 25,24,1,2,0, 18,8,0,2,1, 31,8,0,2,1,
    3,24,1,3,0, 4,24,1,3,0, 22,29,1,3,0, 23,29,1,3,0, 33,29,1,3,0, 34,29,1,3,0, 19,14,1,3,1, 20,14,1,3,1,
    13,27,1,4,0, 14,27,1,4,0, 15,27,1,4,0, 30,19,1,4,0, 31,19,1,4,0, 26,6,1,4,1, 27,6,1,4,1, 31,12,1,4,1
)
private val TH_FLY_A = intArrayOf(38, 9, 39, 8, 41, 9, 41, 11, 39, 12, 37, 11)
private val TH_FLY_B = intArrayOf(10, 11, 11, 10, 13, 11, 13, 13, 11, 14, 9, 13)
private val TH_HEART = intArrayOf(0, 0, 2, 0, 0, 1, 1, 1, 2, 1, 1, 2)
private val TH_LEG_X = intArrayOf(23, 31, 5, 13)

/** Acciones que la UI puede disparar sobre Thor. */
object ThorAction {
    const val NONE = 0
    const val PURR = 1
    const val PLAY = 2
    const val BATH = 3
    fun frames(action: Int): Int = when (action) {
        PURR -> 14
        PLAY -> 24
        BATH -> 38
        else -> 0
    }
    fun millis(action: Int): Long = frames(action) * TH_FRAME_MS
}

// Paseo: 0 reposo, 1 caminar, 2 lamerse, 3 ronronear
private val TH_R_ACT = intArrayOf(1, 2, 1, 0, 1, 3, 1, 0)
private val TH_R_DIR = intArrayOf(1, 0, 1, 0, -1, 0, -1, 0)
private val TH_R_LEN = intArrayOf(24, 16, 12, 10, 18, 14, 18, 8)
private val TH_R_TOTAL = TH_R_LEN.sum()
private const val TH_START_X = 6
private const val TH_CENTER_X = 17

private class ThorPose {
    var v = V_NONE; var vf = 0; var dy = 0; var head = 0; var eyes = E_OPEN; var wag = false
    var groom = false; var tongue = false; var fx = FX_NONE; var fxF = 0; var dirt = 0
    var x = 0; var face = 1
    fun reset() { v = V_NONE; vf = 0; dy = 0; head = 0; eyes = E_OPEN; wag = false; groom = false; tongue = false; fx = FX_NONE; fxF = 0; dirt = 0 }
}
private class ThorClock {
    var frame = 0; var strollT = 0; var action = ThorAction.NONE; var start = 0; var len = 0
    var lastLevel = -1; var lastX = TH_START_X; var lastFace = 1; var bathDirt = 0
    fun af(): Int = if (action != ThorAction.NONE && frame - start < len) frame - start else -1
}
private class ThorT { var ox = 0f; var oy = 0f; var cell = 1f; var flip = false; var g = 0; var pal = TH_PAL
    val xo = IntArray(4); val li = IntArray(4) }

private fun poseIdle(f: Int, s: ThorPose) { s.head = if (f % 8 < 4) 0 else 1; if (f % 28 == 27) s.eyes = E_CLOSED }
private fun poseGroom(f: Int, s: ThorPose) { val p = f % 16; s.head = if (p < 12) 1 else 0; s.groom = p < 12; s.eyes = if (p < 12) E_CLOSED else E_OPEN; s.tongue = p < 12 && p % 4 < 2 }
private fun posePurr(f: Int, s: ThorPose) { s.eyes = E_HAPPY; s.wag = true; s.head = if (f % 4 < 2) 0 else 1; s.fx = FX_PURR; s.fxF = f }
private fun poseSleep(f: Int, s: ThorPose) { s.head = 7 + (if ((f shr 1) % 8 < 4) 0 else 1); s.eyes = E_CLOSED; s.fx = FX_SLEEP; s.fxF = f }
private fun poseHungry(f: Int, s: ThorPose) { s.head = 1; s.eyes = E_DOWN; s.fx = FX_HUNGRY; s.fxF = f }
private fun poseBath(f: Int, s: ThorPose) { if (f < 26) { s.v = V_BATH; s.vf = f } else { s.v = V_SHAKE; s.vf = f - 26 } }
private fun poseStroll(f: Int, hungry: Boolean, s: ThorPose) {
    var t = f % TH_R_TOTAL; var x = 0; var face = 1
    for (i in TH_R_ACT.indices) {
        val len = TH_R_LEN[i]; val dir = TH_R_DIR[i]
        if (t < len) {
            x += dir * (t shr 1); if (dir != 0) face = dir
            when (TH_R_ACT[i]) {
                1 -> { s.v = V_WALK; s.vf = t }
                2 -> poseGroom(t, s)
                3 -> posePurr(t, s)
                else -> if (hungry) poseHungry(t, s) else poseIdle(t, s)
            }
            s.x = TH_START_X + x; s.face = face; return
        }
        x += dir * (len shr 1); if (dir != 0) face = dir
        t -= len
    }
    s.x = TH_START_X + x; s.face = face
}

private fun DrawScope.tr(t: ThorT, gx: Int, gy: Int, w: Int, h: Int, argb: Int) {
    val cx = if (t.flip) TH_W - gx - w else gx
    val l = Math.round(t.ox + cx * t.cell).toFloat(); val r = Math.round(t.ox + (cx + w) * t.cell).toFloat()
    val tp = Math.round(t.oy + gy * t.cell).toFloat(); val b = Math.round(t.oy + (gy + h) * t.cell).toFloat()
    drawRect(Color(argb), Offset(l, tp), Size(r - l, b - tp))
}
private fun DrawScope.tpart(t: ThorT, p: ThorPart, dx: Int, dy: Int) {
    for (r in p.rows.indices) { val row = p.rows[r]; for (i in row.indices) { val k = row[i]; if (k != '.') tr(t, p.x + i + dx, p.y + r + dy, 1, 1, t.pal[k.code]) } }
}
private fun DrawScope.ttail(t: ThorT, p: ThorPart, dx: Int, dy: Int, f: Int, reach: Int, div: Int) {
    for (r in p.rows.indices) {
        val sw = Math.round(sin(f / 3.0 + r / 4.0) * max(0, reach - r) / div).toInt()
        val row = p.rows[r]
        for (i in row.indices) { val k = row[i]; if (k != '.') tr(t, p.x + i + dx + sw, p.y + r + dy, 1, 1, t.pal[k.code]) }
    }
}
private fun DrawScope.teyes(t: ThorT, ex0: Int, ex1: Int, y: Int, eyes: Int) {
    if (eyes == E_OPEN) return
    for (k in 0..1) {
        val x = if (k == 0) ex0 else ex1
        val col = t.pal[(if (k == 0) 'A' else 'B').code]
        tr(t, x, y, 4, 4, C_W)
        when (eyes) {
            E_CLOSED -> tr(t, x, y + 2, 4, 1, C_O)
            E_HAPPY -> { tr(t, x, y + 2, 1, 1, C_O); tr(t, x + 1, y + 1, 2, 1, C_O); tr(t, x + 3, y + 2, 1, 1, C_O) }
            E_GRUMPY -> { tr(t, x, y + 1, 4, 1, C_O); tr(t, x, y + 2, 4, 2, col); tr(t, x + 1, y + 2, 2, 2, C_K) }
            E_DOWN -> { tr(t, x, y, 4, 1, C_O); tr(t, x, y + 2, 4, 2, col); tr(t, x + 1, y + 3, 2, 1, C_K) }
        }
    }
}
private fun DrawScope.tdirt(t: ThorT, s: ThorPose, hx: Int, hy: Int, bx: Int, by: Int, stand: Boolean) {
    var i = 0
    while (i < TH_DIRT.size) {
        val c = TH_DIRT[i]; val r = TH_DIRT[i + 1]; val mud = TH_DIRT[i + 2] == 1; val lvl = TH_DIRT[i + 3]; val head = TH_DIRT[i + 4] == 1
        i += 5
        if (lvl > s.dirt) continue
        if (stand && !head && (r >= 26 || r <= 18)) continue
        tr(t, (if (head) hx else bx) + c, (if (head) hy else by) + r, 1, 1, if (mud) 0xFF8A6240.toInt() else 0xFFA5A6B0.toInt())
    }
    if (s.dirt >= 3) { val a = (t.g % 6) * 2; tr(t, TH_FLY_A[a], TH_FLY_A[a + 1], 1, 1, C_K) }
    if (s.dirt >= 4) { val a = (t.g % 6) * 2; tr(t, TH_FLY_B[a], TH_FLY_B[a + 1], 1, 1, C_K) }
}
private fun DrawScope.tcape(t: ThorT, bx: Int, by: Int) {
    val sw = (t.g shr 1) % 2
    for (x in 3 until 20) {
        val top = by + 16 + (if (x < 7) (7 - x) shr 1 else 0)
        val bot = by + 24 + (if ((x + sw * 2) % 4 < 2) 1 else 0) - (if (x < 6) 6 - x else 0)
        tr(t, bx + x, top, 1, bot - top + 1, 0xFF7F1D1D.toInt())
        tr(t, bx + x, top + 1, 1, bot - top - 1, if ((x + sw) % 5 == 0) 0xFFB91C1C.toInt() else 0xFFDC2626.toInt())
        if (x in 7..16) tr(t, bx + x, top + 1, 1, 1, 0xFFF87171.toInt())
    }
    tr(t, bx + 18, by + 16, 3, 3, 0xFFFACC15.toInt())
}
private fun DrawScope.twear(t: ThorT, a: String?, hx: Int, hy: Int, bx: Int, by: Int, stand: Boolean) {
    val c = hx + 25
    when (a) {
        Pet.ACC_HAT -> { tr(t, c - 5, hy - 1, 10, 5, 0xFF7A3A1E.toInt()); tr(t, c - 5, hy + 2, 10, 1, 0xFFEC4899.toInt()); tr(t, c - 8, hy + 4, 16, 1, 0xFF5E2C14.toInt()) }
        Pet.ACC_CROWN -> { val g = 0xFFFACC15.toInt(); tr(t, c - 4, hy + 3, 9, 2, g); tr(t, c - 4, hy + 1, 1, 2, g); tr(t, c, hy + 1, 1, 2, g); tr(t, c + 4, hy + 1, 1, 2, g); tr(t, c, hy + 3, 1, 1, 0xFFEC4899.toInt()) }
        Pet.ACC_GLASSES -> { val b = 0xFF111827.toInt(); tr(t, c - 8, hy + 9, 17, 1, b); tr(t, c - 7, hy + 10, 5, 3, b); tr(t, c + 3, hy + 10, 5, 3, b); tr(t, c - 6, hy + 10, 1, 1, C_FOAM); tr(t, c + 4, hy + 10, 1, 1, C_FOAM) }
        Pet.ACC_MUSTACHE -> { val b = 0xFF5B3A1E.toInt(); tr(t, c - 5, hy + 15, 4, 1, b); tr(t, c + 2, hy + 15, 4, 1, b); tr(t, c - 1, hy + 14, 3, 1, b) }
        Pet.ACC_BOW -> { val k = 0xFFEC4899.toInt(); tr(t, c + 3, hy + 1, 2, 3, k); tr(t, c + 6, hy + 1, 2, 3, k); tr(t, c + 5, hy + 2, 1, 1, 0xFFBE185D.toInt()) }
        Pet.ACC_BALLOON -> { tr(t, bx + 6, by + 3, 5, 5, 0xFFEC4899.toInt()); tr(t, bx + 7, by + 4, 1, 1, C_FOAM); tr(t, bx + 8, by + 8, 1, 10, C_O) }
        Pet.ACC_BANANA -> { val y = 0xFFFDE047.toInt(); tr(t, bx + 37, by + 26, 5, 2, y); tr(t, bx + 40, by + 24, 2, 2, y); tr(t, bx + 37, by + 26, 1, 1, 0xFFA16207.toInt()) }
        Pet.ACC_BANDANA -> { val x = c + 5; val y = hy + 17; val q = 0xFF2BB6AE.toInt(); val o = 0xFFF59E0B.toInt(); tr(t, x, y, 6, 3, q); tr(t, x + 1, y + 3, 4, 3, q); tr(t, x + 2, y + 6, 2, 2, q); tr(t, x + 1, y + 1, 2, 1, o); tr(t, x + 3, y + 4, 1, 2, o); tr(t, x + 4, y, 1, 1, 0xFFFFD76A.toInt()) }
        Pet.ACC_COLLAR -> { tr(t, c - 7, hy + 18, 15, 1, 0xFFEC4899.toInt()); tr(t, c - 1, hy + 19, 3, 2, 0xFFFACC15.toInt()) }
        Pet.ACC_SOCKS -> if (!stand) { val r = 0xFFDC2626.toInt(); tr(t, bx + 20, by + 27, 7, 2, r); tr(t, bx + 22, by + 27, 1, 2, C_FOAM); tr(t, bx + 31, by + 27, 7, 2, r); tr(t, bx + 33, by + 27, 1, 2, C_FOAM) }
    }
}
private fun DrawScope.tball(t: ThorT, f: Int) {
    val p = f % 16; val b = if (p > 6) min(10, (p - 6) * 2) else 0; val x = 38 + b; val g = TH_GROUND
    val d = 0xFF7F1D1D.toInt(); tr(t, x, g - 5, 3, 1, d); tr(t, x - 1, g - 4, 5, 3, d); tr(t, x, g - 1, 3, 1, d); tr(t, x, g - 4, 3, 3, 0xFFDC2626.toInt()); tr(t, x, g - 4, 1, 1, 0xFFFCA5A5.toInt())
}
private fun DrawScope.ttub(t: ThorT, f: Int) {
    val ty = TH_GROUND - 8
    tr(t, TH_OX - 3, ty, 47, 9, C_O); tr(t, TH_OX - 2, ty + 1, 45, 7, 0xFF7C4A24.toInt()); tr(t, TH_OX - 2, ty + 1, 45, 1, 0xFFA9713B.toInt())
    for (i in 0 until 15) { tr(t, TH_OX - 2 + i * 3, ty - 2 - i % 2, 3, 3, C_FOAM); tr(t, TH_OX - 2 + i * 3, ty, 3, 1, C_FOAM_S) }
    for (i in 0 until 4) { val q = (f + i * 5) % 14; if (q < 10) { tr(t, TH_OX + 2 + i * 10, ty - 4 - q, 2, 2, 0xFFBAE6FD.toInt()); tr(t, TH_OX + 2 + i * 10, ty - 4 - q, 1, 1, C_FOAM) } }
}
/** Pata de la versión "Alto y gordito": la parte alta queda fija y el pie avanza; las del fondo van en sombra. */
private fun DrawScope.tleg(t: ThorT, x: Int, top: Int, bot: Int, sw: Int, lift: Int, far: Boolean) {
    val n = bot - top; val fill = if (far) C_S else C_W; val sh = if (far) C_T else C_S
    for (r in 0 until n) {
        val off = Math.round(sw.toFloat() * r / (n - 1)); val paw = r >= n - 3; val ww = if (paw) 6 else TH_LEG_W
        val lx = x + off; val y = top + r - lift
        tr(t, lx - 1, y, ww + 2, 1, C_O); tr(t, lx, y, ww, 1, fill); tr(t, lx + ww - 1, y, 1, 1, sh)
    }
    val ly = bot - lift
    tr(t, x + sw - 1, ly, 8, 1, C_O); tr(t, x + sw + 2, ly - 1, 1, 1, C_T); tr(t, x + sw + 4, ly - 1, 1, 1, C_T)
}
private fun DrawScope.drawStanding(t: ThorT, s: ThorPose, accessory: String?, cape: Boolean) {
    val f = s.vf; val mode = s.v; val ph = f * PI / 4
    val x0 = TH_OX + (if (mode == V_SHAKE && f % 12 < 6) (if (f % 2 == 1) 1 else -1) else 0)
    var dy = 0; var hd = 0; var eyes = E_OPEN; var wag = false; var raised = false
    for (i in 0..3) { t.xo[i] = 0; t.li[i] = 0 }
    when (mode) {
        V_WALK -> {
            val a = Math.round(sin(ph) * 2).toInt(); val la = Math.round(max(0.0, cos(ph)) * 2).toInt(); val lb = Math.round(max(0.0, -cos(ph)) * 2).toInt()
            t.xo[0] = a; t.xo[3] = a; t.xo[1] = -a; t.xo[2] = -a; t.li[0] = la; t.li[3] = la; t.li[1] = lb; t.li[2] = lb
            dy = -Math.round(abs(sin(ph))).toInt(); hd = Math.round(abs(cos(ph))).toInt()
        }
        V_PLAY -> { val p = f % 16; hd = if (p < 4) 1 else 0; raised = p in 4..8; if (raised) { t.li[0] = 7; t.xo[0] = 4 }; eyes = if (raised) E_HAPPY else E_OPEN; wag = p < 5 }
        V_BATH -> { hd = if (f % 4 < 2) 0 else 1; eyes = if (f % 10 < 8) E_GRUMPY else E_CLOSED }
        V_SHAKE -> eyes = if (f % 12 < 6) E_CLOSED else E_HAPPY
    }
    val by = TH_OY + dy - TH_UP; val top = by + 22; val hy = by + hd - 4; val g = TH_GROUND
    tr(t, TH_OX, g, 38, 1, C_SHADOW)
    ttail(t, TH_ST_TAIL, x0, by, if (mode == V_NONE) t.g else f, 20, if (wag) 7 else 12)
    tleg(t, x0 + TH_LEG_X[3], top, g, t.xo[3], t.li[3], true)
    tleg(t, x0 + TH_LEG_X[1], top, g, t.xo[1], t.li[1], true)
    tpart(t, TH_ST_BODY, x0, by)
    if (cape) tcape(t, x0, by)
    tleg(t, x0 + TH_LEG_X[2], top + 1, g, t.xo[2], t.li[2], false)
    if (!raised) tleg(t, x0 + TH_LEG_X[0], top + 1, g, t.xo[0], t.li[0], false)
    tpart(t, TH_ST_CHEST, x0, by)
    if (raised) tleg(t, x0 + 25, top + 1, g - 2, t.xo[0], t.li[0], false)
    tpart(t, TH_HEAD, x0 + 5, hy)
    teyes(t, x0 + 24, x0 + 33, hy + 9, eyes)
    tdirt(t, s, x0 + 5, hy, x0, by, true)
    twear(t, accessory, x0 + 5, hy, x0, by - 3, true)
    if (accessory == Pet.ACC_SOCKS && mode != V_BATH) for (k in 0..3) {
        if (k == 0 && raised) continue
        val sx = x0 + TH_LEG_X[k] + t.xo[k]; val sy = g - 5 - t.li[k]
        tr(t, sx, sy, 6, 2, 0xFFDC2626.toInt()); tr(t, sx, sy + 2, 6, 1, C_FOAM)
    }
    when (mode) {
        V_PLAY -> tball(t, f)
        V_BATH -> { ttub(t, f); for (i in 0 until 5) tr(t, x0 + 25 + i * 2, hy + 3 + i % 2, 3, 2, C_FOAM) }
        V_SHAKE -> if (f % 12 < 6) { val q = f % 6; tr(t, TH_OX - 2 - q, TH_OY + 18, 1, 1, C_WATER); tr(t, TH_OX + 42 + q, TH_OY + 14, 1, 1, C_WATER); tr(t, TH_OX + 6 - q, TH_OY + 6, 1, 1, C_WATER); tr(t, TH_OX + 38 + q, TH_OY + 2, 1, 1, C_WATER) }
    }
}
private fun DrawScope.drawLying(t: ThorT, s: ThorPose, accessory: String?, cape: Boolean) {
    val bx = TH_OX; val by = TH_OY; val hy = by + s.head
    tr(t, TH_OX, TH_OY + 33, 34, 1, C_SHADOW)
    ttail(t, TH_TAIL, bx, by, t.g, 12, if (s.wag) 5 else 9)
    tpart(t, TH_BODY, bx, by)
    if (cape) tcape(t, bx, by)
    tpart(t, TH_RUFF, bx, by)
    tpart(t, TH_PAW_A, bx, by)
    if (!s.groom) tpart(t, TH_PAW_B, bx, by)
    tpart(t, TH_HEAD, bx, hy)
    teyes(t, bx + 19, bx + 28, hy + 9, s.eyes)
    if (s.groom) tpart(t, TH_PAW_B, bx - 7, by - 10)
    if (s.tongue) tr(t, bx + 25, hy + 16, 2, 1, 0xFFFF7FA8.toInt())
    tdirt(t, s, bx, hy, bx, by, false)
    twear(t, accessory, bx, hy, bx, by, false)
    val p = s.fxF
    when (s.fx) {
        FX_PURR -> { val h = p % 14; if (h < 10) { var i = 0; while (i < TH_HEART.size) { tr(t, TH_OX + 37 + TH_HEART[i], TH_OY + 8 - (h shr 1) + TH_HEART[i + 1], 1, 1, 0xFFEC4899.toInt()); i += 2 } } }
        FX_SLEEP -> {
            tr(t, TH_OX - 1, TH_OY + 27, 42, 6, 0xFFB8894F.toInt())
            var x = 0; while (x < 42) { tr(t, TH_OX - 1 + x, TH_OY + 27, 1, 6, 0xFF8A6233.toInt()); x += 3 }
            tr(t, TH_OX - 1, TH_OY + 27, 42, 1, 0xFFD9B070.toInt())
            val z = (p shr 1) % 10; val b = C_WATER
            tr(t, TH_OX + 36, TH_OY + 9 - z, 3, 1, b); tr(t, TH_OX + 37, TH_OY + 10 - z, 1, 1, b); tr(t, TH_OX + 36, TH_OY + 11 - z, 3, 1, b)
        }
        FX_HUNGRY -> {
            tr(t, TH_OX + 36, TH_OY + 30, 7, 1, 0xFF7A7F99.toInt()); tr(t, TH_OX + 37, TH_OY + 31, 5, 1, 0xFF7A7F99.toInt())
            if (p % 8 < 5) { tr(t, TH_OX + 38, TH_OY + 2, 1, 3, 0xFFEC4899.toInt()); tr(t, TH_OX + 38, TH_OY + 6, 1, 1, 0xFFEC4899.toInt()) }
        }
    }
}
/** Etapa Gatito (nivel 1–4): dibujo propio, con pasitos en vez de patas largas. */
private fun DrawScope.drawKitten(t: ThorT, s: ThorPose, accessory: String?) {
    val f = if (s.v != V_NONE) s.vf else t.g
    val walking = s.v == V_WALK; val x = TH_OX + 2; val y = TH_OY + 9 + (if (walking) -(f % 2) else 0)
    tr(t, 8, TH_GROUND, 26, 1, C_SHADOW)
    ttail(t, TH_KIT_TAIL, x, y, f, 6, if (s.wag) 3 else 6)
    tpart(t, TH_KIT_BODY, x, y); tpart(t, TH_KIT_RUFF, x, y)
    val a = f % 4 < 2
    tpart(t, TH_KIT_PAW_A, x + (if (walking) (if (a) 2 else -1) else 0), TH_OY + 9)
    tpart(t, TH_KIT_PAW_B, x + (if (walking) (if (a) -1 else 2) else 0), TH_OY + 9)
    var eyes = s.eyes; var hd = s.head.coerceAtMost(2)
    when (s.v) {
        V_PLAY -> { val p = f % 16; eyes = if (p in 4..8) E_HAPPY else E_OPEN; hd = if (p < 4) 1 else 0 }
        V_BATH -> eyes = if (f % 10 < 8) E_GRUMPY else E_CLOSED
        V_SHAKE -> eyes = if (f % 12 < 6) E_CLOSED else E_HAPPY
    }
    val hy = y + hd
    tpart(t, TH_KIT_HEAD, x, hy)
    if (eyes != E_OPEN) for (e in 0..1) {
        val ex = x + (if (e == 0) 10 else 19); val ey = hy + 7
        val col = t.pal[(if (e == 0) 'A' else 'B').code]
        tr(t, ex, ey, 4, 4, C_W)
        when (eyes) {
            E_CLOSED -> tr(t, ex, ey + 2, 4, 1, C_O)
            E_HAPPY -> { tr(t, ex, ey + 2, 1, 1, C_O); tr(t, ex + 1, ey + 1, 2, 1, C_O); tr(t, ex + 3, ey + 2, 1, 1, C_O) }
            else -> { tr(t, ex, ey + 1, 4, 1, C_O); tr(t, ex, ey + 2, 4, 2, col); tr(t, ex + 1, ey + 2, 2, 2, C_K) }
        }
    }
    var i = 0
    while (i < TH_DIRT.size) {
        val c = TH_DIRT[i]; val r = TH_DIRT[i + 1]; val mud = TH_DIRT[i + 2] == 1; val lvl = TH_DIRT[i + 3]; val head = TH_DIRT[i + 4] == 1
        i += 5
        if (lvl > s.dirt) continue
        val px = if (head) x + 9 + (c - 14) * 2 / 3 else x + 2 + c * 2 / 3
        val py = if (head) hy + r * 2 / 3 else TH_OY + 9 + r * 2 / 3
        tr(t, px, py, 1, 1, if (mud) 0xFF8A6240.toInt() else 0xFFA5A6B0.toInt())
    }
    twear(t, accessory, x - 9, hy - 1, x - 9, TH_OY + 2, false)
    if (s.fx == FX_SLEEP) {
        tr(t, TH_OX, TH_OY + 28, 34, 5, 0xFFB8894F.toInt()); tr(t, TH_OX, TH_OY + 28, 34, 1, 0xFFD9B070.toInt())
        val z = (s.fxF shr 1) % 10; tr(t, TH_OX + 28, TH_OY + 12 - z, 3, 1, C_WATER); tr(t, TH_OX + 29, TH_OY + 13 - z, 1, 1, C_WATER); tr(t, TH_OX + 28, TH_OY + 14 - z, 3, 1, C_WATER)
    }
    if (s.v == V_PLAY) tball(t, f)
    if (s.v == V_BATH) ttub(t, f)
}

/**
 * Thor dibujado en pixel-art procedural, sobre el fondo que tenga detrás.
 * - [strollScene] = true: escena de 80×52 celdas donde pasea, se lame y ronronea.
 * - [strollScene] = false: Thor echado y centrado (tarjeta del feed, previsualización de la tienda).
 * [action] + [actionKey]: cada vez que cambia la llave se reproduce la acción una vez.
 */
@Composable
fun ThorPixelStage(
    pet: Pet,
    modifier: Modifier = Modifier,
    accessory: String? = pet.getActiveEquippedAccessory(),
    strollScene: Boolean = true,
    action: Int = ThorAction.NONE,
    actionKey: Int = 0,
    bathStartCleanliness: Int = 100,
    background: String? = null
) {
    val sleeping = pet.getActiveIsSleeping()
    val level = pet.getActiveLevel()
    val cleanliness = pet.getActiveCleanliness()
    val hungry = pet.getActiveHunger() >= 70

    val clock = remember { ThorClock() }
    val pose = remember { ThorPose() }
    val transform = remember { ThorT() }
    val tick = remember { mutableIntStateOf(0) }
    val sleepingNow by rememberUpdatedState(sleeping)

    LaunchedEffect(actionKey) {
        if (actionKey != 0 && action != ThorAction.NONE) {
            clock.action = action; clock.start = clock.frame; clock.len = ThorAction.frames(action)
            clock.bathDirt = cukyDirtLevel(bathStartCleanliness)
        }
    }
    LaunchedEffect(level) {
        if (clock.lastLevel in 1 until level) { clock.action = ThorAction.PURR; clock.start = clock.frame; clock.len = ThorAction.frames(ThorAction.PURR) }
        clock.lastLevel = level
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TH_FRAME_MS)
            clock.frame++
            if (clock.af() < 0 && !sleepingNow) clock.strollT++
            tick.intValue = clock.frame
        }
    }

    Canvas(modifier = modifier) {
        tick.intValue // lectura en fase de dibujo: solo redibuja, no recompone
        val s = pose
        s.reset()
        val af = clock.af()
        var x = clock.lastX; var face = clock.lastFace
        when {
            sleeping -> { poseSleep(clock.frame, s); x = TH_CENTER_X; face = 1 }
            af >= 0 -> when (clock.action) {
                ThorAction.BATH -> poseBath(af, s)
                ThorAction.PLAY -> { s.v = V_PLAY; s.vf = af }
                else -> posePurr(af, s)
            }
            strollScene -> { poseStroll(clock.strollT, hungry, s); x = s.x; face = s.face }
            else -> if (hungry) poseHungry(clock.frame, s) else poseIdle(clock.frame, s)
        }
        s.dirt = if (af >= 0 && clock.action == ThorAction.BATH) (if (af < 26) max(0, clock.bathDirt - af / 6) else 0) else cukyDirtLevel(cleanliness)
        clock.lastX = x; clock.lastFace = face

        val t = transform
        t.g = clock.frame
        if (strollScene) {
            t.cell = min(size.width / CUKY_SCENE_W, size.height / CUKY_SCENE_H)
            val sx0 = (size.width - CUKY_SCENE_W * t.cell) / 2f
            t.ox = sx0 + x * t.cell
            if (background != null) {
                // Escena completa 80×52: fondo procedural + Thor en la misma grilla
                val sy0 = (size.height - CUKY_SCENE_H * t.cell) / 2f
                drawThorBackground(background, clock.frame, sleeping, sx0, sy0, t.cell)
                // Thor más chico que la grilla del fondo, con los pies sobre el suelo de la escena
                val sceneCell = t.cell
                t.cell = sceneCell * TH_SCENE_SCALE
                t.ox = sx0 + (x + TH_SCENE_SHIFT_X) * sceneCell
                t.oy = sy0 + TH_SCENE_FEET_Y * sceneCell - (TH_GROUND + 1) * t.cell
            } else {
                t.oy = size.height - (TH_GROUND + 1) * t.cell
            }
            t.flip = face < 0 && !sleeping && af < 0
        } else {
            t.cell = floor(min(size.width / TH_W, size.height / (TH_GROUND + 1 - 5)))
            t.ox = floor((size.width - TH_W * t.cell) / 2f)
            t.oy = size.height - (TH_GROUND + 1) * t.cell
            t.flip = false
        }
        t.pal = if (t.flip) TH_PAL_FLIP else TH_PAL
        val cape = level >= 15
        when {
            level < 5 -> drawKitten(t, s, accessory)
            s.v != V_NONE -> drawStanding(t, s, accessory, cape)
            else -> drawLying(t, s, accessory, cape)
        }
    }
}

/** Cuadro fijo de Thor para el widget: mismo encuadre que la tarjeta del feed. */
fun renderThorBitmap(sizePx: Int, level: Int, accessory: String?, sleeping: Boolean, hunger: Int, cleanliness: Int): Bitmap =
    renderPixelPetBitmap(sizePx) {
        val s = ThorPose()
        when {
            sleeping -> poseSleep(0, s)
            hunger >= 70 -> poseHungry(0, s)
            else -> poseIdle(0, s)
        }
        s.dirt = cukyDirtLevel(cleanliness)
        val t = ThorT()
        t.cell = floor(min(size.width / TH_W, size.height / (TH_GROUND + 1 - 5)))
        t.ox = floor((size.width - TH_W * t.cell) / 2f)
        t.oy = size.height - (TH_GROUND + 1) * t.cell
        when {
            level < 5 -> drawKitten(t, s, accessory)
            else -> drawLying(t, s, accessory, level >= 15)
        }
    }
