package calendario.kevshupp.diariokevinali.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// El editor guarda HTML, pero al escribir se ve el resultado: las etiquetas se ocultan y se aplican como estilo.

private val HTML_TAG = Regex("<[^<>]+>")
private val TAG_NAME = Regex("^</?\\s*([a-zA-Z0-9]+)")
private val FONT_COLOR = Regex("color\\s*=\\s*['\"]?(#[0-9a-fA-F]{6})")
private val FONT_SIZE = Regex("size\\s*=\\s*['\"]?(\\d)")
private val ALIGN = Regex("align\\s*=\\s*['\"]?(left|center|right)")

private fun fontSizeFor(size: Int) = when {
    size <= 2 -> 14.sp
    size <= 4 -> 18.sp
    size <= 6 -> 26.sp
    else -> 32.sp
}

private class OpenTag(val name: String, val start: Int, val style: SpanStyle?, val paragraph: ParagraphStyle?)

/** Oculta las etiquetas HTML y muestra su efecto; el texto real del campo no cambia. */
class HtmlVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val visible = StringBuilder(source.length)
        val toVisible = IntArray(source.length + 1)
        val toSource = ArrayList<Int>(source.length + 1)
        val spans = ArrayList<AnnotatedString.Range<SpanStyle>>()
        val paragraphs = ArrayList<AnnotatedString.Range<ParagraphStyle>>()
        val open = ArrayList<OpenTag>()
        var paragraphOpen = false

        fun copyVisible(from: Int, to: Int) {
            for (i in from until to) {
                toVisible[i] = visible.length
                toSource.add(i)
                visible.append(source[i])
            }
        }

        var index = 0
        for (match in HTML_TAG.findAll(source)) {
            copyVisible(index, match.range.first)
            val tag = match.value
            val name = TAG_NAME.find(tag)?.groupValues?.get(1)?.lowercase() ?: ""
            val replacement = when {
                name == "br" -> "\n"
                name == "img" -> "🖼"
                tag.startsWith("</") && (name == "p" || name == "div") -> "\n"
                else -> ""
            }
            for (i in match.range) toVisible[i] = visible.length
            if (replacement.isNotEmpty()) {
                replacement.forEach { c ->
                    toSource.add(match.range.first)
                    visible.append(c)
                }
            }
            if (tag.startsWith("</")) {
                val at = open.indexOfLast { it.name == name }
                if (at >= 0) {
                    val closed = open.removeAt(at)
                    if (closed.style != null) spans.add(AnnotatedString.Range(closed.style, closed.start, visible.length))
                    if (closed.paragraph != null) {
                        paragraphs.add(AnnotatedString.Range(closed.paragraph, closed.start, visible.length))
                        paragraphOpen = false
                    }
                }
            } else if (replacement.isEmpty() || name == "p") {
                var paragraph: ParagraphStyle? = null
                val style = when (name) {
                    "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "i", "em", "blockquote" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                    "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    "font" -> SpanStyle(
                        color = FONT_COLOR.find(tag)?.let { Color(android.graphics.Color.parseColor(it.groupValues[1])) } ?: Color.Unspecified,
                        fontSize = FONT_SIZE.find(tag)?.let { fontSizeFor(it.groupValues[1].toInt()) } ?: androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                    else -> null
                }
                if (name == "p" && !paragraphOpen) {
                    paragraph = when (ALIGN.find(tag)?.groupValues?.get(1)) {
                        "center" -> ParagraphStyle(textAlign = TextAlign.Center)
                        "right" -> ParagraphStyle(textAlign = TextAlign.Right)
                        "left" -> ParagraphStyle(textAlign = TextAlign.Left)
                        else -> null
                    }
                    if (paragraph != null) paragraphOpen = true
                }
                if (name.isNotEmpty()) open.add(OpenTag(name, visible.length, style, paragraph))
            }
            index = match.range.last + 1
        }
        copyVisible(index, source.length)
        toVisible[source.length] = visible.length
        toSource.add(source.length)

        // Lo que sigue sin cerrar llega hasta el final del texto
        open.forEach { tag ->
            if (tag.style != null) spans.add(AnnotatedString.Range(tag.style, tag.start, visible.length))
            if (tag.paragraph != null) paragraphs.add(AnnotatedString.Range(tag.paragraph, tag.start, visible.length))
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toVisible[offset.coerceIn(0, source.length)]
            override fun transformedToOriginal(offset: Int): Int {
                val at = offset.coerceIn(0, toSource.size - 1)
                // Queda justo después del carácter anterior: lo escrito al final conserva su formato
                return if (at == 0) toSource[0] else toSource[at - 1] + 1
            }
        }
        return TransformedText(AnnotatedString(visible.toString(), spans, paragraphs), mapping)
    }
}

/**
 * Como las etiquetas no se ven, un borrado no debe romperlas: si la tecla de borrar cae dentro
 * de una etiqueta, se borra el carácter visible anterior y la etiqueta queda intacta.
 */
fun protectHtmlTags(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val removed = old.text.length - new.text.length
    if (removed != 1) return new
    val cut = new.selection.start
    if (cut !in 0 until old.text.length) return new
    if (old.text.removeRange(cut, cut + 1) != new.text) return new
    val tags = HTML_TAG.findAll(old.text).toList()
    var tag = tags.firstOrNull { cut in it.range } ?: return new
    while (true) {
        val previous = tags.firstOrNull { it.range.last + 1 == tag.range.first } ?: break
        tag = previous
    }
    val target = tag.range.first - 1
    if (target < 0) return old
    val text = old.text.removeRange(target, target + 1)
    return TextFieldValue(text, androidx.compose.ui.text.TextRange(target))
}
