package calendario.kevshupp.diariokevinali.compose

import android.text.Spannable
import android.text.Spanned
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/** Colores de la hoja de las cartas: los comparten el feed, la lectura y el editor. */
class LetterPaper(
    val paper: Color,
    val ink: Color,
    val subtle: Color,
    val author: Color,
    val rule: Color,
    val margin: Color,
    val frame: Color,
    val onFrame: Color,
    val adjustColors: Boolean
)

private val LIGHT_PAPER = LetterPaper(
    paper = Color(0xFFF3E5AB),
    ink = Color(0xFF4A2511),
    subtle = Color(0xFF8B4513),
    author = Color(0xFFA61E4D),
    rule = Color(0x224A2511),
    margin = Color(0x55EC4899),
    frame = Color(0xFF4A2511),
    onFrame = Color(0xFFF3E5AB),
    adjustColors = false
)

private val MONO_PAPER = LetterPaper(
    paper = Color.White,
    ink = Color.Black,
    subtle = Color.DarkGray,
    author = Color.Black,
    rule = Color.Transparent,
    margin = Color.Transparent,
    frame = Color.Black,
    onFrame = Color.White,
    adjustColors = false
)

@Composable
fun rememberLetterPaper(theme: String): LetterPaper {
    val appBackground = getAppBackgroundColor(theme)
    return remember(theme, appBackground) {
        when (theme) {
            // Papel de noche: sigue el color de fondo elegido, un poco más claro que él
            "Pixel Oscuro" -> LetterPaper(
                paper = lerp(appBackground, Color(0xFF4A3D73), 0.42f),
                ink = Color(0xFFEDE6D6),
                subtle = Color(0xFFB9A9D6),
                author = Color(0xFFFF8FB5),
                rule = Color(0x12EDE6D6),
                margin = Color(0x66FF7FA8),
                frame = Color(0xFF91465F),
                onFrame = Color.White,
                adjustColors = true
            )
            "Pixel Monocromático" -> MONO_PAPER
            else -> LIGHT_PAPER
        }
    }
}

private const val MIN_LETTER_CONTRAST = 4.5

/** Aclara un color de texto hasta que se lea sobre `background`, conservando su tono. */
fun readableColorOn(color: Int, background: Int): Int {
    val opaqueBackground = ColorUtils.setAlphaComponent(background, 255)
    var result = ColorUtils.setAlphaComponent(color, 255)
    var steps = 0
    while (ColorUtils.calculateContrast(result, opaqueBackground) < MIN_LETTER_CONTRAST && steps < 30) {
        result = ColorUtils.blendARGB(result, android.graphics.Color.WHITE, 0.08f)
        steps++
    }
    return result
}

/** Devuelve el texto con sus colores legibles sobre el papel. La carta guardada no cambia. */
fun adjustLetterColors(text: Spanned, background: Color): CharSequence {
    val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
    if (spans.isEmpty()) return text
    val backgroundArgb = background.toArgb()
    val result = text as? Spannable ?: SpannableString(text)
    spans.forEach { span ->
        val adjusted = readableColorOn(span.foregroundColor, backgroundArgb)
        if (adjusted != span.foregroundColor) {
            val start = result.getSpanStart(span)
            val end = result.getSpanEnd(span)
            val flags = result.getSpanFlags(span)
            result.removeSpan(span)
            result.setSpan(ForegroundColorSpan(adjusted), start, end, flags)
        }
    }
    return result
}
