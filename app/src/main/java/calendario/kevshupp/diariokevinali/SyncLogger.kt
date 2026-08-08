package calendario.kevshupp.diariokevinali

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncLogger {
    private const val TAG = "SyncLogger"
    private const val PREF_NAME = "DiarioSyncLogs"
    private const val KEY_LOGS = "logs_list"
    private const val MAX_LOGS = 150

    val logsList = mutableStateListOf<String>()

    @Synchronized
    fun log(context: Context?, message: String, level: String = "INFO") {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedLog = "[$timestamp] [$level] $message"
        
        Log.d(TAG, formattedLog)

        try {
            logsList.add(formattedLog)
            if (logsList.size > MAX_LOGS) {
                logsList.removeAt(0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error actualizando logsList en memoria", e)
        }

        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val rawString = prefs.getString(KEY_LOGS, "") ?: ""
                val currentLogs = if (rawString.isNotEmpty()) rawString.split("\n").toMutableList() else mutableListOf()
                currentLogs.add(formattedLog)
                if (currentLogs.size > MAX_LOGS) {
                    currentLogs.subList(0, currentLogs.size - MAX_LOGS).clear()
                }
                prefs.edit().putString(KEY_LOGS, currentLogs.joinToString("\n")).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Error persistiendo log en SharedPreferences", e)
            }
        }
    }

    @Synchronized
    fun logError(context: Context?, message: String, throwable: Throwable? = null) {
        val detail = throwable?.let { ": ${it.message ?: it.toString()}" } ?: ""
        log(context, "$message$detail", "ERROR")
    }

    @Synchronized
    fun loadLogs(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val rawString = prefs.getString(KEY_LOGS, "") ?: ""
            val logs = if (rawString.isNotEmpty()) rawString.split("\n") else emptyList()
            logsList.clear()
            logsList.addAll(logs)
            logs
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun clearLogs(context: Context) {
        try {
            logsList.clear()
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_LOGS).apply()
            log(context, "Historial de registros limpiado.", "SYSTEM")
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar registros", e)
        }
    }

    fun getFormattedLogs(context: Context): String {
        val logs = if (logsList.isNotEmpty()) logsList.toList() else loadLogs(context)
        return if (logs.isEmpty()) {
            "No hay registros de sincronización aún."
        } else {
            logs.joinToString("\n")
        }
    }

    fun copyToClipboard(context: Context) {
        val content = getFormattedLogs(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Diario Sync Logs", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📋 Registros copiados al portapapeles", Toast.LENGTH_SHORT).show()
    }
}
