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
    }

    fun cancelSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        Log.d(TAG, "Sincronización periódica cancelada.")
    }

    fun stopAllRunningSyncs(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_ONETIME_WORK_NAME)
        
        // Cancelar el periódico actual, pero reagendarlo de inmediato para no perder la programación futura
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        
        val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getLong("syncIntervalMinutes", 0L)
        val wifiOnly = prefs.getBoolean("syncWifiOnly", true)
        val chargingOnly = prefs.getBoolean("syncChargingOnly", false)
        if (intervalMinutes > 0) {
            scheduleSync(context, intervalMinutes, wifiOnly, chargingOnly)
        }
        Log.d(TAG, "Sincronizaciones en ejecución detenidas (y reagendadas si existía programación).")
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
