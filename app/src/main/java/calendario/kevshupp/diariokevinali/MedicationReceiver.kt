package calendario.kevshupp.diariokevinali

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.math.abs

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medicationId") ?: return
        val name = intent.getStringExtra("name") ?: "Medicamento"
        val targetTime = intent.getStringExtra("targetTime") ?: ""
        val enableAlarm = intent.getBooleanExtra("enableAlarm", false)
        val durationDays = intent.getIntExtra("durationDays", -1)
        val startDate = intent.getLongExtra("startDate", 0L)

        // Show medication notification
        showMedicationNotification(context, name, targetTime, enableAlarm)

        // Reschedule for the next day
        val nextCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val timeParts = targetTime.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Add 1 day for tomorrow
            add(Calendar.DAY_OF_YEAR, 1)
        }

        // Verify duration limit
        val durationMs = if (durationDays > 0) durationDays * 24L * 60 * 60 * 1000 else null
        if (durationMs != null && nextCalendar.timeInMillis > startDate + durationMs) {
            // Already expired, do not reschedule
            return
        }

        // Reschedule alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextIntent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("medicationId", medicationId)
            putExtra("name", name)
            putExtra("targetTime", targetTime)
            putExtra("enableAlarm", enableAlarm)
            putExtra("durationDays", durationDays)
            putExtra("startDate", startDate)
        }
        val requestCode = abs(medicationId.hashCode() + targetTime.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCalendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextCalendar.timeInMillis, pendingIntent)
        }
    }

    private fun showMedicationNotification(context: Context, name: String, targetTime: String, isAlarm: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isAlarm) "medication_alarms" else "medication_reminders"
        val channelName = if (isAlarm) "Alarmas de Medicamentos 🔔" else "Recordatorios de Medicamentos ⏰"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (isAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notificaciones para la toma de tus medicamentos"
                if (isAlarm) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Hora de tu medicamento! 💊")
            .setContentText("Toma: $name ($targetTime)")
            .setAutoCancel(true)
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)

        notificationManager.notify(abs(name.hashCode() + targetTime.hashCode()), builder.build())
    }
}
