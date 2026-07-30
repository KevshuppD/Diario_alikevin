package calendario.kevshupp.diariokevinali

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.abs

object MedicationAlarmScheduler {
    fun scheduleAllAlarms(context: Context, med: MedicationItem) {
        if (!med.enableReminder && !med.enableAlarm) {
            cancelAllAlarms(context, med)
            return
        }

        med.selectedTimes.forEach { timeStr ->
            scheduleAlarmForTime(context, med, timeStr)
        }
    }

    fun cancelAllAlarms(context: Context, med: MedicationItem) {
        med.selectedTimes.forEach { timeStr ->
            cancelAlarmForTime(context, med.id, timeStr)
        }
    }

    private fun scheduleAlarmForTime(context: Context, med: MedicationItem, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val timeParts = timeStr.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If in the past today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check if duration expired
        val durationDays = med.durationDays ?: -1
        val durationMs = if (durationDays > 0) durationDays * 24L * 60 * 60 * 1000 else null
        if (durationMs != null && calendar.timeInMillis > med.startDate + durationMs) {
            // Already expired, do not schedule
            return
        }

        val intent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("medicationId", med.id)
            putExtra("name", med.name)
            putExtra("targetTime", timeStr)
            putExtra("enableAlarm", med.enableAlarm)
            putExtra("durationDays", durationDays)
            putExtra("startDate", med.startDate)
        }

        val requestCode = abs(med.id.hashCode() + timeStr.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
            Log.d("MedAlarmScheduler", "Alarm scheduled for ${med.name} at $timeStr")
        } catch (e: SecurityException) {
            Log.e("MedAlarmScheduler", "No permission to schedule exact alarm", e)
        }
    }

    private fun cancelAlarmForTime(context: Context, medicationId: String, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationReceiver::class.java)
        val requestCode = abs(medicationId.hashCode() + timeStr.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("MedAlarmScheduler", "Alarm cancelled for $medicationId at $timeStr")
        }
    }

    fun rescheduleAllAlarmsFromFirestore(context: Context, coupleId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("medications").document(coupleId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val list = snapshot.get("meds") as? List<*>
                    val medsList = list?.mapNotNull { itemMap ->
                        val map = itemMap as? Map<*, *> ?: return@mapNotNull null
                        MedicationItem(
                            id = map["id"]?.toString() ?: "",
                            name = map["name"]?.toString() ?: "",
                            createdBy = map["createdBy"]?.toString() ?: "",
                            durationDays = (map["durationDays"] as? Number)?.toInt(),
                            intervalHours = (map["intervalHours"] as? Number)?.toInt() ?: 8,
                            selectedTimes = (map["selectedTimes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            enableReminder = map["enableReminder"] as? Boolean ?: false,
                            enableAlarm = map["enableAlarm"] as? Boolean ?: false,
                            startDate = (map["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } ?: emptyList()

                    medsList.forEach { med ->
                        scheduleAllAlarms(context, med)
                    }
                    Log.d("MedAlarmScheduler", "Rescheduled ${medsList.size} medications from Firestore")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MedAlarmScheduler", "Failed to load medications for reschedule", e)
            }
    }
}
