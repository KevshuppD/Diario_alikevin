package calendario.kevshupp.diariokevinali

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** Repeticiones de citas: las usan la vista del calendario y los recordatorios. */

/** Indica si la cita ocurre en el día de `day`. Un día 31 mensual cae el último día de los meses más cortos. */
fun CalendarEvent.occursOn(day: Calendar): Boolean {
    val origin = Calendar.getInstance().apply { timeInMillis = date }
    val sameDay = origin.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
        origin.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return true
    if (origin.after(day)) return false
    val clampedDay = minOf(origin.get(Calendar.DAY_OF_MONTH), day.getActualMaximum(Calendar.DAY_OF_MONTH))
    return when (recurrence) {
        "DAILY" -> true
        "WEEKLY" -> origin.get(Calendar.DAY_OF_WEEK) == day.get(Calendar.DAY_OF_WEEK)
        "MONTHLY" -> day.get(Calendar.DAY_OF_MONTH) == clampedDay
        "YEARLY" -> origin.get(Calendar.MONTH) == day.get(Calendar.MONTH) && day.get(Calendar.DAY_OF_MONTH) == clampedDay
        else -> false
    }
}

/** Primera vez que ocurre la cita después de `afterMillis`, o null si ya pasó y no se repite. */
fun CalendarEvent.nextOccurrenceAfter(afterMillis: Long): Long? {
    if (date > afterMillis) return date
    val unit = when (recurrence) {
        "DAILY" -> Calendar.DAY_OF_YEAR
        "WEEKLY" -> Calendar.WEEK_OF_YEAR
        "MONTHLY" -> Calendar.MONTH
        "YEARLY" -> Calendar.YEAR
        else -> return null
    }
    val candidate = Calendar.getInstance()
    var steps = 1
    while (true) {
        // Siempre desde la fecha original, para que febrero no deje un día 31 fijo en 28
        candidate.timeInMillis = date
        candidate.add(unit, steps)
        if (candidate.timeInMillis > afterMillis) return candidate.timeInMillis
        steps++
    }
}

object CalendarReminders {
    const val EXTRA_EVENT_ID = "reminder_event_id"
    const val EXTRA_EVENT_TITLE = "reminder_event_title"
    const val EXTRA_EVENT_DESCRIPTION = "reminder_event_description"
    const val EXTRA_EVENT_DATE = "reminder_event_date"
    const val EXTRA_EVENT_RECURRENCE = "reminder_event_recurrence"

    private fun pendingIntent(context: Context, eventId: String, intent: Intent, flags: Int) =
        PendingIntent.getBroadcast(context, eventId.hashCode(), intent, flags or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(context: Context, event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val leadTime = prefs.getLong("appointmentLeadTime", 60L) * 60 * 1000
        val occurrence = event.nextOccurrenceAfter(System.currentTimeMillis() + leadTime)
        if (occurrence == null) {
            // Al editarla pudo dejar de repetirse o quedar en el pasado: no debe sonar la alarma anterior
            cancel(context, event.eventId)
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Cita programada")
            putExtra("content", "${event.title} - ${event.description}")
            putExtra(EXTRA_EVENT_ID, event.eventId)
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_DESCRIPTION, event.description)
            putExtra(EXTRA_EVENT_DATE, event.date)
            putExtra(EXTRA_EVENT_RECURRENCE, event.recurrence)
        }
        val pending = pendingIntent(context, event.eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmTime = occurrence - leadTime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pending)
        }
    }

    fun cancel(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pending = pendingIntent(context, eventId, intent, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }
}
