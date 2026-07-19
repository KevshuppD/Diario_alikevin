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

        // FCM v1 puede enviar los datos en el objeto 'notification' o 'data'
        val notification = remoteMessage.notification
        if (notification != null) {
            notification.title?.let { title = it }
            notification.body?.let { body = it }
        } else if (remoteMessage.data.isNotEmpty()) {
            remoteMessage.data["title"]?.let { title = it }
            remoteMessage.data["body"]?.let { body = it }
        }

        val imageUrl = remoteMessage.data["imageUrl"]
        val authorId = remoteMessage.data["authorId"]

        Log.d("FCM", "Datos recibidos - Title: $title, Body: $body, AuthorId: $authorId")

        // Evitar mostrar mi propia notificación
        val myId = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).getString("userId", "")
        if (authorId != null && authorId == myId) {
            Log.d("FCM", "Ignorando notificación propia")
            return
        }

        val clickType = remoteMessage.data["click_type"]
        sendNotification(title, body, imageUrl, clickType)
    }

    private fun sendNotification(title: String, messageBody: String, imageUrl: String?, clickType: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (clickType != null) {
                putExtra("click_type", clickType)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
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
