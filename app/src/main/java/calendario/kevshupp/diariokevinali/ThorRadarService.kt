package calendario.kevshupp.diariokevinali

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ThorRadarService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "radar_channel"
        const val NOTIFICATION_ID = 2024
        private const val TAG = "ThorRadarService"

        fun startService(context: Context) {
            val intent = Intent(context, ThorRadarService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ThorRadarService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ThorRadarManager.init(this)
        createNotificationChannel()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Diario:ThorRadarWakeLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar WakeLock: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val isSharing = prefs.getBoolean("radar_is_sharing", true)
        if (!isSharing) {
            Log.d(TAG, "ThorRadarService iniciado con radar_is_sharing=false. Deteniendo servicio...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (PermissionHelper.hasLocationPermission(this)) {
            val isBatterySaver = prefs.getBoolean("radar_battery_saver", false)
            val interval = if (isBatterySaver) 60_000L else 30_000L
            ThorRadarManager.startLiveTracking(this, interval, isForeground = false)
        }
        Log.d(TAG, "ThorRadarService activo en segundo plano listo para recibir Magic Packets y movimiento pasivo.")

        return START_STICKY
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(timeoutMs)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        serviceJob.cancelChildren()
        releaseWakeLock()
        super.onDestroy()
        ThorRadarManager.stopLiveTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Thor Radar"
            val descriptionText = "Rastreo de ubicación en tiempo real para parejas"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("click_type", "radar")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1005,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_heart_pixel)
            .setContentTitle("🧭 Thor Radar Activo")
            .setContentText("Compartiendo ubicación y estado con tu pareja")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
