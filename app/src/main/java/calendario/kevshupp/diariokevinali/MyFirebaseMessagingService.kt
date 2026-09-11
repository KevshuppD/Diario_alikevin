package calendario.kevshupp.diariokevinali

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

        // Evitar mostrar mi propia notificación (comparar con userId y userName)
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

        val rawType = remoteMessage.data["click_type"]
            ?: remoteMessage.data["type"]
            ?: remoteMessage.data["destination"]
            ?: remoteMessage.data["screen"]
            ?: remoteMessage.data["tab"]
            ?: remoteMessage.data["action"]

        val clickType = if (!rawType.isNullOrBlank()) {
            rawType
        } else {
            val combined = "$title $body".lowercase(java.util.Locale.ROOT)
            when {
                combined.contains("receta") -> "receta"
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

        if (rawType == "radar_ping" || clickType == "radar_ping") {
            Log.d("FCM", "Petición radar_ping recibida. Despertando servicio y actualizando ubicación...")
            try {
                if (prefs.getBoolean("radar_is_sharing", true) && PermissionHelper.hasLocationPermission(this)) {
                    ThorRadarService.startService(this)
                    ThorRadarManager.forceLocationUpdate(this)
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error en wakeup de radar tras radar_ping", e)
            }
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((key, value) in dataMap) {
                putExtra(key, value)
            }
            if (clickType != null) {
                putExtra("click_type", clickType)
            }
        }
        val requestCode = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "diario_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

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
                notifyNow(channelId, notificationBuilder)
            }
        } else {
            notifyNow(channelId, notificationBuilder)
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

    private fun notifyNow(channelId: String, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Diario", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
