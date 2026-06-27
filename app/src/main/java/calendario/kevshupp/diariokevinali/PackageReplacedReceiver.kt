package calendario.kevshupp.diariokevinali

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("PackageReplacedReceiver", "App actualizada/reinstalada. Deteniendo sincronizaciones manuales huérfanas.")
            SyncScheduler.stopAllRunningSyncs(context)
            
            val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("syncState", "NO_SINCRONIZADO").apply()
        }
    }
}
