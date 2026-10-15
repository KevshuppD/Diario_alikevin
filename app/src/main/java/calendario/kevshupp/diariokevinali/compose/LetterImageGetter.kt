package calendario.kevshupp.diariokevinali.compose

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Fotos dentro del texto de una carta (<img src="...#w=50"> escrito desde el editor web).
 *
 * Html.fromHtml pide cada imagen de forma síncrona: se devuelve un contenedor vacío con el tamaño
 * final y Coil la descarga en segundo plano; al llegar, se vuelve a asignar el texto para que el
 * TextView recalcule el alto de la línea.
 *
 * El fragmento #w= indica el ancho en % del texto (33, 50 o 100). Sin él, se usa el ancho completo.
 */
class LetterImageGetter(private val textView: TextView) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable {
        val holder = UrlDrawable()
        if (source.isNullOrBlank() || !source.startsWith("https://")) return holder

        val widthPercent = Regex("#w=(\\d{1,3})").find(source)?.groupValues?.get(1)?.toIntOrNull()
            ?.coerceIn(10, 100) ?: 100
        val cleanUrl = source.substringBefore('#')
        val availableWidth = (textView.width - textView.paddingLeft - textView.paddingRight)
            .takeIf { it > 0 } ?: textView.resources.displayMetrics.widthPixels
        val targetWidth = availableWidth * widthPercent / 100
        // Alto provisional 3:4 para que el texto no salte demasiado mientras carga
        holder.setBounds(0, 0, targetWidth, targetWidth * 3 / 4)

        val request = ImageRequest.Builder(textView.context)
            .data(cleanUrl.optimizeCloudinary(1000))
            .target { result ->
                val ratio = if (result.intrinsicWidth > 0) result.intrinsicHeight.toFloat() / result.intrinsicWidth else 0.75f
                val height = (targetWidth * ratio).toInt().coerceAtLeast(1)
                result.setBounds(0, 0, targetWidth, height)
                holder.inner = result
                holder.setBounds(0, 0, targetWidth, height)
                // Reasignar el mismo texto obliga al TextView a medir de nuevo con el alto real
                textView.text = textView.text
            }
            .build()
        textView.context.imageLoader.enqueue(request)
        return holder
    }

    private class UrlDrawable : Drawable() {
        var inner: Drawable? = null

        override fun draw(canvas: Canvas) {
            inner?.draw(canvas)
        }

        override fun setAlpha(alpha: Int) {
            inner?.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            inner?.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    companion object {
        private val IMG_TAG = Regex("<img[^>]*>", RegexOption.IGNORE_CASE)
        private val BREAK_BEFORE = Regex("(<br\\s*/?>|<p[^>]*>)\\s*$", RegexOption.IGNORE_CASE)
        private val BREAK_AFTER = Regex("^\\s*(<br\\s*/?>|</p>)", RegexOption.IGNORE_CASE)

        /**
         * Deja cada foto en su propia línea: las cartas guardadas por la web antes de la corrección tenían
         * las fotos alineadas a la izquierda pegadas al texto y Html.fromHtml las mostraba en medio del párrafo.
         */
        fun isolateImages(html: String): String {
            if (!html.contains("<img", ignoreCase = true)) return html
            return IMG_TAG.replace(html) { m ->
                val before = html.substring(0, m.range.first)
                val after = html.substring(m.range.last + 1)
                val needsBefore = before.isNotBlank() && !BREAK_BEFORE.containsMatchIn(before)
                val needsAfter = after.isNotBlank() && !BREAK_AFTER.containsMatchIn(after)
                (if (needsBefore) "<br>" else "") + m.value + (if (needsAfter) "<br>" else "")
            }
        }

        private val IMG_SRC = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        /** URLs (sin #w=) de las fotos insertadas en el texto, para no repetirlas debajo de la carta. */
        fun inlineImageUrls(content: String?): Set<String> {
            if (content.isNullOrEmpty() || !content.contains("<img", ignoreCase = true)) return emptySet()
            return IMG_SRC.findAll(content).map { it.groupValues[1].substringBefore('#') }.toSet()
        }
    }
}
