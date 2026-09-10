package calendario.kevshupp.diariokevinali

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("BootReceiver", "Evento de inicio recibido: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            val isSharing = prefs.getBoolean("radar_is_sharing", true)
            if (isSharing && PermissionHelper.hasLocationPermission(context)) {
                Log.d("BootReceiver", "Iniciando ThorRadarService tras inicio/actualización...")
                ThorRadarService.startService(context)
            }
        }
    }
}
