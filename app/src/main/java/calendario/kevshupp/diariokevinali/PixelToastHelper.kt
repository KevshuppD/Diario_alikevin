package calendario.kevshupp.diariokevinali

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat

object PixelToastHelper {

    fun showStyledPixelToast(activity: Activity, message: String, currentTheme: String) {
        activity.runOnUiThread {
            try {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                val builder = AlertDialog.Builder(activity)
                val density = activity.resources.displayMetrics.density
                val paddingHorizontal = (24 * density).toInt()
                val paddingVertical = (16 * density).toInt()

                val layout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                }

                val textView = TextView(activity).apply {
                    text = message
                    textSize = 18f
                    gravity = Gravity.CENTER
                    try {
                        typeface = ResourcesCompat.getFont(activity, R.font.vt323)
                    } catch (e: Exception) {
                        // Ignorar
                    }
                }

                layout.addView(textView)

                val isDark = currentTheme == "Pixel Oscuro"
                if (isDark) {
                    layout.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
                    textView.setTextColor(Color.WHITE)
                } else {
                    layout.setBackgroundResource(R.drawable.bg_parchment_pixel)
                    textView.setTextColor(Color.parseColor("#4A2511"))
                }

                builder.setView(layout)
                val dialog = builder.create()

                dialog.window?.let { window ->
                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

                    val wlp = window.attributes
                    wlp.gravity = Gravity.BOTTOM
                    wlp.y = (100 * density).toInt()
                    window.attributes = wlp
                }

                dialog.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing) {
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        // Ignorar
                    }
                }, 2500)
            } catch (e: Exception) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
