package calendario.kevshupp.diariokevinali

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val TAG = "SyncScheduler"
    private const val UNIQUE_PERIODIC_WORK_NAME = "SyncDrivePeriodicWork"
    const val UNIQUE_ONETIME_WORK_NAME = "SyncDriveOneTimeWork"

    fun scheduleSync(
        context: Context,
        intervalMinutes: Long,
        wifiOnly: Boolean,
        chargingOnly: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)

        if (intervalMinutes <= 0) {
            // Sincronización manual, cancelamos cualquier tarea periódica existente
            cancelSync(context)
            Log.d(TAG, "Sincronización configurada como Manual. Tareas periódicas canceladas.")
            SyncLogger.log(context, "Sincronización periódica desactivada (Modo Manual).")
            return
        }

        // Definir restricciones de red y energía
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(chargingOnly)
            .build()

        // Creamos la petición de trabajo periódico (mínimo 15 minutos en WorkManager)
        val finalInterval = Math.max(15L, intervalMinutes)
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncDriveWorker>(
            finalInterval, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Encolamos de forma única para reemplazar el anterior si cambia la configuración
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
        Log.d(TAG, "Programada sincronización periódica cada $finalInterval minutos. Restricciones - Wi-Fi: $wifiOnly, Carga: $chargingOnly")
        SyncLogger.log(context, "Frecuencia actualizada: Sincronización programada cada $finalInterval min (Wi-Fi: $wifiOnly, Carga: $chargingOnly).")
    }

    fun cancelSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        Log.d(TAG, "Sincronización periódica cancelada.")
    }

    fun stopAllRunningSyncs(context: Context) {
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("syncCancelledByUser", true)
            .putLong("syncIntervalMinutes", 0L) // Pasar frecuencia a Manual (0L) para que no vuelva a ejecutarse
            .apply()

        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_ONETIME_WORK_NAME)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)

        Log.d(TAG, "Sincronización detenida por el usuario. Frecuencia cambiada a Manual.")
        SyncLogger.log(context, "Sincronización detenida por el usuario (Frecuencia cambiada a Manual).", "WARN")
    }

    fun runNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // Ejecución inmediata, hereda la conexión a internet pero sin restricciones pesadas
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncDriveWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_ONETIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
        Log.d(TAG, "Disparada sincronización inmediata en primer plano/segundo plano.")
    }
}
