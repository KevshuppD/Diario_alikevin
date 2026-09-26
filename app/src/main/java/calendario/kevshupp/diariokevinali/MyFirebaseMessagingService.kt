package calendario.kevshupp.diariokevinali

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        var title = "Nuevo mensaje"
        var body = ""

        // FCM v1 puede enviar los datos en el objeto 'data' o 'notification'
        if (remoteMessage.data.isNotEmpty()) {
            remoteMessage.data["title"]?.let { title = it }
            remoteMessage.data["body"]?.let { body = it }
        } else if (remoteMessage.notification != null) {
            remoteMessage.notification?.title?.let { title = it }
            remoteMessage.notification?.body?.let { body = it }
        }

        val imageUrl = remoteMessage.data["imageUrl"]
        val authorId = remoteMessage.data["authorId"]
        val authorName = remoteMessage.data["authorName"]

        Log.d("FCM", "Datos recibidos - Title: $title, Body: $body, AuthorId: $authorId, AuthorName: $authorName")

        val rawType = remoteMessage.data["click_type"]
            ?: remoteMessage.data["type"]
            ?: remoteMessage.data["destination"]
            ?: remoteMessage.data["screen"]
            ?: remoteMessage.data["tab"]
            ?: remoteMessage.data["action"]
            ?: ""

        val isMagicPacket = rawType == "radar_ping" ||
                remoteMessage.data["magic_packet"] == "WOL_LOCATION_WAKEUP"

        if (isMagicPacket) {
            val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
            val myId = prefs.getString("userId", "")?.trim()?.lowercase() ?: ""
            val myName = prefs.getString("userName", "")?.trim()?.lowercase() ?: ""
            val myDoc = ThorRadarManager.getMyDocName(myId, myName)
            val targetDoc = remoteMessage.data["targetDoc"]?.trim()?.lowercase() ?: ""

            // Ignorar si el ping era para el otro usuario o si fui yo quien lo emitió
            val authIdNorm = authorId?.trim()?.lowercase() ?: ""
            if (authIdNorm.isNotEmpty() && (authIdNorm == myId || (myName.isNotEmpty() && authIdNorm == myName))) {
                Log.d("FCM", "⚡ [MAGIC PACKET] Ignorando ping emitido por mí mismo")
                return
            }
            if (targetDoc.isNotEmpty() && targetDoc != myDoc) {
                Log.d("FCM", "⚡ [MAGIC PACKET] Ignorando ping porque targetDoc ($targetDoc) != myDoc ($myDoc)")
                return
            }

            val isSilent = remoteMessage.data["silent"] == "true" ||
                    remoteMessage.data["is_silent"] == "true" ||
                    remoteMessage.data["silent_ping"] == "true"

            Log.d("FCM", "⚡ [MAGIC PACKET] Petición radar_ping recibida para $myDoc (isSilent=$isSilent). Ejecutando handleMagicLocationPing...")
            try {
                ThorRadarManager.handleMagicLocationPing(this)
            } catch (e: Exception) {
                Log.e("FCM", "Error en handleMagicLocationPing tras radar_ping", e)
            }

            if (!isSilent) {
                val displayTitle = title.ifBlank { "📍 Thor Radar" }
                val sender = authorName ?: "Tu pareja"
                val displayBody = body.ifBlank { "¡$sender ha solicitado tu ubicación en vivo!" }
                sendNotification(displayTitle, displayBody, null, "radar", remoteMessage.data)
            }
            return
        }

        // Evitar mostrar mi propia notificación visual (comparar con userId y userName)
        val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
        val myId = prefs.getString("userId", "")?.trim()?.lowercase() ?: ""
        val myName = prefs.getString("userName", "")?.trim()?.lowercase() ?: ""

        val authIdNorm = authorId?.trim()?.lowercase() ?: ""
        val authNameNorm = authorName?.trim()?.lowercase() ?: ""

        if (authIdNorm.isNotEmpty() && (authIdNorm == myId || (myName.isNotEmpty() && authIdNorm == myName))) {
            Log.d("FCM", "Ignorando notificación propia por authorId: $authIdNorm")
            return
        }
        if (authNameNorm.isNotEmpty() && myName.isNotEmpty() && authNameNorm == myName) {
            Log.d("FCM", "Ignorando notificación propia por authorName: $authNameNorm")
            return
        }

        val clickType = if (rawType.isNotBlank()) {
            rawType
        } else {
            val combined = "$title $body".lowercase(java.util.Locale.ROOT)
            when {
                combined.contains("receta") -> "receta"
                combined.contains("actualización") || combined.contains("actualizacion") || combined.contains("versión") || combined.contains("version") || combined.contains("update") -> "update"
                combined.contains("cita") || combined.contains("evento") || combined.contains("calendario") -> "cita"
                combined.contains("medicamento") || combined.contains("pastilla") || combined.contains("remedio") -> "medicamento"
                combined.contains("thor") || combined.contains("mascota") -> "mascota"
                combined.contains("radar") || combined.contains("ubicación") || combined.contains("ubicacion") || combined.contains("sos") -> "radar"
                combined.contains("álbum") || combined.contains("album") || combined.contains("recuerdo") || combined.contains("foto") -> "album"
                combined.contains("horario") || combined.contains("clase") -> "horario"
                combined.contains("anime") -> "anime"
                combined.contains("espíritu") || combined.contains("espiritu") || combined.contains("checklist") -> "espiritus"
                else -> "carta"
            }
        }

        if (clickType == "update") {
            val updateVer = remoteMessage.data["version"]
            if (!updateVer.isNullOrBlank()) {
                val updateManager = UpdateManager(this)
                if (!updateManager.isNewerVersion(BuildConfig.VERSION_NAME, updateVer)) {
                    Log.d("FCM", "Ignorando notificación de actualización: la versión remota ($updateVer) ya está instalada o es inferior a la actual (${BuildConfig.VERSION_NAME})")
                    return
                }
            }
        }

        if (clickType == "sos") {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                val wl = pm?.newWakeLock(
                    android.os.PowerManager.FULL_WAKE_LOCK or
                            android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            android.os.PowerManager.ON_AFTER_RELEASE,
                    "Diario:SosEmergencyWakeLock"
                )
                wl?.acquire(10_000L)
            } catch (e: Exception) {
                Log.w("FCM", "No se pudo adquirir WakeLock para SOS: ${e.message}")
            }
            SosAlarmHelper.playSosAlarm(this)
        }

        sendNotification(title, body, imageUrl, clickType, remoteMessage.data)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        imageUrl: String?,
        clickType: String?,
        dataMap: Map<String, String> = emptyMap()
    ) {
        val isSos = clickType == "sos"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((key, value) in dataMap) {
                putExtra(key, value)
            }
            if (clickType != null) {
                putExtra("click_type", clickType)
            }
        }
        val requestCode = if (isSos) 911 else (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isSos) "diario_sos_emergency_v2" else "diario_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(if (isSos) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (isSos) {
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_ALARM)
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
            val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            notificationBuilder.setSound(alarmUri)
            notificationBuilder.setVibrate(longArrayOf(0, 800, 200, 800, 200, 800, 400, 350, 150, 350))
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        if (!imageUrl.isNullOrBlank()) {
            serviceScope.launch {
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .setSummaryText(messageBody)
                    )
                }
                notifyNow(channelId, notificationBuilder, isSos)
            }
        } else {
            notifyNow(channelId, notificationBuilder, isSos)
        }
    }

    private fun downloadImage(imageUrl: String): Bitmap? {
        var bitmap: Bitmap? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(imageUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            connection.inputStream.use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error downloading notification image", e)
        } finally {
            connection?.disconnect()
        }
        return bitmap
    }

    private fun notifyNow(channelId: String, notificationBuilder: NotificationCompat.Builder, isSos: Boolean = false) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isSos) {
                val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val sosChannel = NotificationChannel(channelId, "Alerta de Emergencia SOS", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 800, 200, 800, 200, 800, 400, 350, 150, 350)
                    setSound(alarmUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    description = "Alarmas de emergencia SOS en tiempo real de tu pareja"
                }
                notificationManager.createNotificationChannel(sosChannel)
            } else {
                val channel = NotificationChannel(channelId, "Diario", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableLights(true)
                    enableVibration(true)
                    description = "Notificaciones de Diario Ali & Kevin"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notifId = if (isSos) 911 else System.currentTimeMillis().toInt()
        notificationManager.notify(notifId, notificationBuilder.build())
    }
}
