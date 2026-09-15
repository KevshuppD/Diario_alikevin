package calendario.kevshupp.diariokevinali

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ThorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        @JvmStatic
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("thor_widget_prefs", Context.MODE_PRIVATE)
            val name = prefs.getString("pet_name", "Thor") ?: "Thor"
            val petType = prefs.getString("pet_type", "thor") ?: "thor"
            val isCuky = petType.equals("cuky", ignoreCase = true) || name.equals("Cuky", ignoreCase = true)
            val level = prefs.getInt("pet_level", 1)
            val happiness = prefs.getInt("pet_happiness", 100)
            val status = prefs.getString("pet_status", "FELIZ") ?: "FELIZ"
            val accessory = prefs.getString("pet_accessory", "none") ?: "none"
            val isSleeping = prefs.getBoolean("pet_sleeping", false)

            val thorImageRes = when {
                isSleeping -> if (isCuky) R.drawable.ic_cuky_sleep else R.drawable.ic_thor_sleep
                else -> when (accessory) {
                    "collar" -> if (isCuky) R.drawable.ic_cuky_collar else R.drawable.ic_thor_collar
                    "mustache" -> if (isCuky) R.drawable.ic_cuky_mustache else R.drawable.ic_thor_mustache
                    "balloon" -> if (isCuky) R.drawable.ic_cuky_balloon else R.drawable.ic_thor_balloon
                    "bow" -> if (isCuky) R.drawable.ic_cuky_bow else R.drawable.ic_thor_bow
                    "hat" -> if (isCuky) R.drawable.ic_cuky_hat else R.drawable.ic_thor_hat
                    "bandana" -> if (isCuky) R.drawable.ic_cuky_bandana else R.drawable.ic_thor_bandana
                    "glasses" -> if (isCuky) R.drawable.ic_cuky_glasses else R.drawable.ic_thor_glasses
                    "crown" -> if (isCuky) R.drawable.ic_cuky_crown else R.drawable.ic_thor_crown
                    "banana" -> if (isCuky) R.drawable.ic_cuky_banana else R.drawable.ic_thor_banana
                    "socks" -> if (isCuky) R.drawable.ic_cuky_socks else R.drawable.ic_thor_socks
                    else -> if (isCuky) R.drawable.ic_cuky_base_trans else R.drawable.ic_thor_base_trans
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_thor).apply {
                setImageViewResource(R.id.widget_pet_avatar, thorImageRes)
                setTextViewText(R.id.widget_pet_name, name)
                setTextViewText(R.id.widget_pet_level, "LVL $level")

                val displayStatus = if (isSleeping) "DURMIENDO 💤" else status
                setTextViewText(R.id.widget_pet_status, "Estado: $displayStatus")

                val smile = if (isSleeping) "💤" else if (happiness > 40) "😊" else "😢"
                setTextViewText(R.id.widget_pet_happiness, "Felicidad: $happiness% $smile")
            }

            // Al tocar el widget, abre la MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        @JvmStatic
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ThorWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, ThorWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
