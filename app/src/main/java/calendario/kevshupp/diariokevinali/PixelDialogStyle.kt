package calendario.kevshupp.diariokevinali

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/** Aplica el estilo pixel-art a los AlertDialog nativos. Llamar después de show(). */
object PixelDialogStyle {
    private class Palette(val background: Int, val border: Int, val text: Int)

    private fun paletteFor(theme: String) = when (theme) {
        "Pixel Oscuro" -> Palette(Color.parseColor("#1A1A1A"), Color.parseColor("#D97706"), Color.WHITE)
        "Pixel Monocromático" -> Palette(Color.BLACK, Color.WHITE, Color.WHITE)
        else -> Palette(Color.parseColor("#F3E5AB"), Color.parseColor("#4A2511"), Color.parseColor("#4A2511"))
    }

    private fun box(context: Context, fill: Int, border: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 0f
        setColor(fill)
        setStroke((3 * context.resources.displayMetrics.density).toInt(), border)
    }

    private fun apply(context: Context, window: Window?, buttons: List<Button?>, theme: String) {
        val colors = paletteFor(theme)
        val font = try { ResourcesCompat.getFont(context, R.font.vt323) } catch (e: Exception) { null }
        window?.setBackgroundDrawable(box(context, colors.background, colors.border))
        val texts = listOf(
            window?.findViewById<TextView>(androidx.appcompat.R.id.alertTitle) to 26f,
            window?.findViewById<TextView>(android.R.id.title) to 26f,
            window?.findViewById<TextView>(android.R.id.message) to 20f
        )
        texts.forEach { (view, size) ->
            view?.typeface = font ?: view?.typeface
            view?.textSize = size
            view?.setTextColor(colors.text)
        }
        buttons.filterNotNull().forEach { button ->
            button.typeface = font ?: button.typeface
            button.textSize = 18f
            button.isAllCaps = false
            button.setTextColor(colors.text)
            button.background = box(context, colors.background, colors.border)
        }
    }

    fun androidx.appcompat.app.AlertDialog.pixelStyle(theme: String) = apply {
        apply(context, window, listOf(getButton(android.content.DialogInterface.BUTTON_POSITIVE), getButton(android.content.DialogInterface.BUTTON_NEGATIVE), getButton(android.content.DialogInterface.BUTTON_NEUTRAL)), theme)
    }

    fun android.app.AlertDialog.pixelStyle(theme: String) = apply {
        apply(context, window, listOf(getButton(android.content.DialogInterface.BUTTON_POSITIVE), getButton(android.content.DialogInterface.BUTTON_NEGATIVE), getButton(android.content.DialogInterface.BUTTON_NEUTRAL)), theme)
    }
}
