package calendario.kevshupp.diariokevinali

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PetCareWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("PetCareWorker", "Ejecutando chequeo de la mascota en segundo plano.")
        
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val coupleId = prefs.getString("coupleId", null) ?: return Result.success()
        
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("pets").document(coupleId).get().await()
            if (!snapshot.exists()) return Result.success()
            
            val p = snapshot.toObject(Pet::class.java) ?: return Result.success()
            
            // Calcular decaimiento real-time para notificar con datos exactos
            val now = System.currentTimeMillis()
            val decayDiff = now - (if (p.lastDecayUpdate != 0L) p.lastDecayUpdate else p.lastInteraction)
            val hoursToDecay = decayDiff / (1000 * 60 * 60)
            
            var currentHunger = p.hunger
            var currentCleanliness = p.cleanliness
            var currentSleepPercent = p.sleepPercent
            
            if (hoursToDecay >= 1) {
                currentHunger = Math.min(100, p.hunger + (hoursToDecay * 4).toInt())
                currentCleanliness = Math.max(0, p.cleanliness - (hoursToDecay * 3).toInt())
                if (p.isSleeping) {
                    currentSleepPercent = Math.min(100, p.sleepPercent + (hoursToDecay * 15).toInt())
                } else {
                    currentSleepPercent = Math.max(0, p.sleepPercent - (hoursToDecay * 5).toInt())
                }
            }
            
            // Enviar notificaciones si es necesario y no se ha notificado recientemente (evitar spam)
            val sharedPrefs = context.getSharedPreferences("pet_notif_prefs", Context.MODE_PRIVATE)
            val enabled = sharedPrefs.getBoolean("notifications_enabled", true)
            if (!enabled) {
                Log.d("PetCareWorker", "Notificaciones de la mascota desactivadas por el usuario.")
                return Result.success()
            }
            
            val lastHungerNotif = sharedPrefs.getLong("last_hunger_notif", 0)
            val lastCleanNotif = sharedPrefs.getLong("last_clean_notif", 0)
            val lastSleepNotif = sharedPrefs.getLong("last_sleep_notif", 0)
            
            val cooldown = 4 * 60 * 60 * 1000 // 4 horas de enfriamiento entre alertas del mismo tipo
            
            // 1. Alerta de Hambre
            if (currentHunger >= 70 && (now - lastHungerNotif > cooldown)) {
                sendLocalNotification("🍖 ¡Thor tiene hambre!", "Su nivel de hambre es de $currentHunger%. ¡Dale algo de comer en el Diario! 🐟", 2001)
                sharedPrefs.edit().putLong("last_hunger_notif", now).apply()
            }
            
            // 2. Alerta de Suciedad
            if (currentCleanliness <= 30 && (now - lastCleanNotif > cooldown)) {
                sendLocalNotification("🧼 ¡Thor necesita un baño!", "Su nivel de limpieza es de $currentCleanliness%. ¡Dale un buen baño en el Diario! 🚿", 2002)
                sharedPrefs.edit().putLong("last_clean_notif", now).apply()
            }
            
            // 3. Alerta de Sueño
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val isLateNight = hour >= 22 || hour < 6
            val needsSleep = currentSleepPercent <= 20 || isLateNight
            if (!p.isSleeping && needsSleep && (now - lastSleepNotif > cooldown)) {
                sendLocalNotification("💤 ¡Thor tiene sueño!", "El nivel de energía de Thor es de $currentSleepPercent%. ¡Ponlo a dormir en el Diario para que descanse! 🌙", 2003)
                sharedPrefs.edit().putLong("last_sleep_notif", now).apply()
            }
            
        } catch (e: Exception) {
            Log.e("PetCareWorker", "Error al verificar la mascota: ${e.message}", e)
        }
        
        return Result.success()
    }

    private fun sendLocalNotification(title: String, body: String, notifId: Int) {
        // Asegurarse de que el canal de notificaciones esté creado (por si acaso)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Diario"
            val description = "Notificaciones de mensajes y momentos compartidos"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("diario_channel", name, importance)
            channel.description = description
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "diario_channel")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces) // icono del sistema por defecto
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            // Verificar permiso POST_NOTIFICATIONS si es Android 13+
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notifId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("PetCareWorker", "Permiso de notificación no disponible", e)
        }
    }
}
