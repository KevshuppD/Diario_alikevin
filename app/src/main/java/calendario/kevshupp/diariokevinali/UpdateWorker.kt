package calendario.kevshupp.diariokevinali

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val updateManager = UpdateManager(applicationContext)
        
        val updateUrl = suspendCancellableCoroutine<String?> { continuation ->
            updateManager.checkForUpdates(object : UpdateManager.UpdateCallback {
                override fun onUpdateAvailable(url: String) {
                    if (continuation.isActive) continuation.resume(url)
                }

                override fun onNoUpdate() {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onDownloadProgress(progress: Int) {}
                override fun onDownloadComplete() {}
            })
        }

        if (updateUrl != null) {
            showNotification(updateUrl)
        }

        return Result.success()
    }

    private fun showNotification(url: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Actualizaciones", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("update_url", url)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_settings_pixel)
            .setContentTitle("Nueva actualización disponible")
            .setContentText("Hay una nueva versión del Diario de Kevin y Ali. ¡Toca para ver!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }
}
