package calendario.kevshupp.diariokevinali

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.d("InstallResultReceiver", "PackageInstaller status: $status, msg: $message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Si el sistema solicita confirmación del usuario (fallback interactivo)
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("InstallResultReceiver", "¡Instalación silenciosa completada con éxito!")
            }
            else -> {
                Log.e("InstallResultReceiver", "Error en instalación silenciosa ($status): $message")
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_COMPLETE = "calendario.kevshupp.diariokevinali.ACTION_INSTALL_COMPLETE"
    }
}
