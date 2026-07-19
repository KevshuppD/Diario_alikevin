package calendario.kevshupp.diariokevinali

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.RemoteViews
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LastMessageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, forceLarge = false, forceSmall = true)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    companion object {
        @JvmStatic
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            updateAppWidget(context, appWidgetManager, appWidgetId, forceLarge = false, forceSmall = true)
        }

        @JvmStatic
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, forceLarge: Boolean, forceSmall: Boolean) {
            val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            val coupleId = prefs.getString("coupleId", null)
            val myId = prefs.getString("userId", null)

            val isLarge = when {
                forceLarge -> true
                forceSmall -> false
                else -> {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    minHeight >= 180
                }
            }
            val layoutId = if (isLarge) R.layout.widget_last_message_large else R.layout.widget_last_message

            val views = RemoteViews(context.packageName, layoutId)

            val baseIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val basePending = PendingIntent.getActivity(
                context,
                appWidgetId,
                baseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, basePending)

            // Update immediately with loading state
            appWidgetManager.updateAppWidget(appWidgetId, views)

            if (coupleId != null && myId != null) {
                FirebaseFirestore.getInstance().collection("messages")
                    .whereEqualTo("partnerId", coupleId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        var lastContent = "No hay mensajes nuevos"
                        var senderName = "Pareja"
                        var messageId: String? = null
                        var timestamp: Long? = null

                        for (doc in querySnapshot) {
                            val authorId = doc.getString("authorId")
                            val content = doc.getString("content")
                            if (authorId != null && authorId != myId && content != null && !content.startsWith("[ALBUM]")) {
                                lastContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()
                                senderName = doc.getString("authorName") ?: "Pareja"
                                messageId = doc.id
                                timestamp = doc.getLong("timestamp")
                                break
                            }
                        }

                        var titleText = "Mensaje de $senderName"
                        if (timestamp != null) {
                            val dayStr = when {
                                isSameDay(timestamp, System.currentTimeMillis()) -> "Hoy"
                                isYesterday(timestamp) -> "Ayer"
                                else -> {
                                    val sdf = SimpleDateFormat("EEEE d", Locale("es", "ES"))
                                    val formatted = sdf.format(Date(timestamp))
                                    if (formatted.isNotEmpty()) {
                                        formatted.substring(0, 1).uppercase() + formatted.substring(1)
                                    } else {
                                        formatted
                                    }
                                }
                            }
                            titleText += " ($dayStr)"
                        }
                        views.setTextViewText(R.id.tvWidgetTitle, titleText)
                        views.setTextViewText(R.id.tvWidgetMessage, lastContent)

                        if (messageId != null) {
                            val msgIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra("openMessageId", messageId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            val msgPending = PendingIntent.getActivity(
                                context,
                                appWidgetId,
                                msgIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widgetRoot, msgPending)
                        }

                        if (isLarge) {
                            // Actualizar primero con el mensaje, mientras cargan los eventos
                            appWidgetManager.updateAppWidget(appWidgetId, views)

                            FirebaseFirestore.getInstance().collection("calendar")
                                .whereEqualTo("partnerId", coupleId)
                                .get()
                                .addOnSuccessListener { eventSnap ->
                                    val now = System.currentTimeMillis()
                                    var closest: com.google.firebase.firestore.DocumentSnapshot? = null
                                    var closestTs: Long? = null
                                    if (eventSnap != null) {
                                        for (doc in eventSnap.documents) {
                                            val ts = doc.getLong("date")
                                            if (ts == null || ts < now) continue
                                            if (closestTs == null || ts < closestTs) {
                                                closest = doc
                                                closestTs = ts
                                            }
                                        }
                                    }

                                    if (closest != null && closestTs != null) {
                                        val title = closest.getString("title")
                                        val sdf = SimpleDateFormat("EEE d MMM HH:mm", Locale("es", "ES"))
                                        val dateText = sdf.format(Date(closestTs))
                                        views.setTextViewText(R.id.tvWidgetEventTitle, title ?: "Cita próxima")
                                        views.setTextViewText(R.id.tvWidgetEventDate, dateText)
                                    } else {
                                        views.setTextViewText(R.id.tvWidgetEventTitle, "Sin eventos próximos")
                                        views.setTextViewText(R.id.tvWidgetEventDate, "")
                                    }
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                                .addOnFailureListener {
                                    views.setTextViewText(R.id.tvWidgetEventTitle, "Sin eventos próximos")
                                    views.setTextViewText(R.id.tvWidgetEventDate, "")
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                        } else {
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                    .addOnFailureListener {
                        views.setTextViewText(R.id.tvWidgetMessage, "Error cargando mensajes")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
            } else {
                views.setTextViewText(R.id.tvWidgetMessage, "Inicia sesión primero")
                if (isLarge) {
                    views.setTextViewText(R.id.tvWidgetEventTitle, "Inicia sesión primero")
                    views.setTextViewText(R.id.tvWidgetEventDate, "")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun isSameDay(t1: Long, t2: Long): Boolean {
            val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
            val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(timestamp: Long): Boolean {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            return isSameDay(timestamp, cal.timeInMillis)
        }
    }
}
