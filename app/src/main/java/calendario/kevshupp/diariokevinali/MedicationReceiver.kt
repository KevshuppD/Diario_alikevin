package calendario.kevshupp.diariokevinali

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
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
        val alarmSoundUri = intent.getStringExtra("alarmSoundUri")
        val createdBy = intent.getStringExtra("createdBy") ?: ""

        // Determinar si este dispositivo es el dueño del medicamento
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val currentUserName = prefs.getString("userName", "") ?: ""
        val currentUserId = prefs.getString("userId", "") ?: ""
        // El dueño es quien creó el medicamento (comparamos por nombre de usuario guardado en createdBy)
        val isOwner = createdBy.isBlank() ||
            createdBy.equals(currentUserName, ignoreCase = true) ||
            createdBy.equals(currentUserId, ignoreCase = true)

        // Mostrar notificación: con sonido/alarma si es el dueño, silenciosa si es la pareja
        showMedicationNotification(
            context, medicationId, name, targetTime,
            enableAlarm = enableAlarm && isOwner,   // alarma fuerte solo al dueño
            alarmSoundUri = if (isOwner) alarmSoundUri else null,
            isOwner = isOwner
        )

        // Reagendar para el día siguiente
        val nextCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val timeParts = targetTime.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        // Verificar límite de duración
        val durationMs = if (durationDays > 0) durationDays * 24L * 60 * 60 * 1000 else null
        if (durationMs != null && nextCalendar.timeInMillis > startDate + durationMs) {
            return
        }

        // Reagendar alarma
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextIntent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("medicationId", medicationId)
            putExtra("name", name)
            putExtra("targetTime", targetTime)
            putExtra("enableAlarm", enableAlarm)
            putExtra("durationDays", durationDays)
            putExtra("startDate", startDate)
            putExtra("alarmSoundUri", alarmSoundUri)
            putExtra("createdBy", createdBy)
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

    private fun showMedicationNotification(
        context: Context,
        medicationId: String,
        name: String,
        targetTime: String,
        enableAlarm: Boolean,
        alarmSoundUri: String?,
        isOwner: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal silencioso para la pareja (solo aviso visual)
        // Canal con sonido/vibración para el dueño
        val channelId: String
        val channelName: String
        val importance: Int

        if (!isOwner) {
            // Pareja: notificación silenciosa informativa
            channelId = "med_partner_silent"
            channelName = "Recordatorios de pareja 👫"
            importance = NotificationManager.IMPORTANCE_LOW
        } else if (enableAlarm) {
            // Dueño con alarma activada: sonido fuerte
            channelId = if (!alarmSoundUri.isNullOrEmpty()) {
                "med_alarm_${medicationId}_${abs(alarmSoundUri.hashCode())}"
            } else {
                "medication_alarms"
            }
            channelName = if (!alarmSoundUri.isNullOrEmpty()) "Alarma de $name 🔔" else "Alarmas de Medicamentos 🔔"
            importance = NotificationManager.IMPORTANCE_HIGH
        } else {
            // Dueño sin alarma: recordatorio normal
            channelId = "medication_reminders"
            channelName = "Recordatorios de Medicamentos ⏰"
            importance = NotificationManager.IMPORTANCE_DEFAULT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notificaciones para la toma de: $name"
                when {
                    !isOwner -> {
                        // Sin sonido ni vibración para la pareja
                        setSound(null, null)
                        enableVibration(false)
                    }
                    enableAlarm -> {
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                        val uri = if (!alarmSoundUri.isNullOrEmpty()) {
                            Uri.parse(alarmSoundUri)
                        } else {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        }
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                        setSound(uri, audioAttributes)
                    }
                    // reminder sin alarma: usa el sonido por defecto del canal
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

        // Título diferente para la pareja
        val title = if (isOwner) "¡Hora de tu medicamento! 💊" else "Recordatorio de medicamento 👫"
        val body = if (isOwner) "Toma: $name ($targetTime)" else "Tu pareja debe tomar: $name ($targetTime)"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(
                when {
                    !isOwner -> NotificationCompat.PRIORITY_LOW
                    enableAlarm -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setCategory(
                when {
                    !isOwner -> NotificationCompat.CATEGORY_STATUS
                    enableAlarm -> NotificationCompat.CATEGORY_ALARM
                    else -> NotificationCompat.CATEGORY_REMINDER
                }
            )

        if (isOwner && enableAlarm) {
            val uri = if (!alarmSoundUri.isNullOrEmpty()) {
                Uri.parse(alarmSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            builder.setSound(uri)
        } else if (isOwner) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        } else {
            // Pareja: sin sonido
            builder.setDefaults(0)
            builder.setSound(null)
        }

        notificationManager.notify(abs(name.hashCode() + targetTime.hashCode()), builder.build())
    }
}
