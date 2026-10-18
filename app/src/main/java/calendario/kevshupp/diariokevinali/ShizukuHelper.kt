package calendario.kevshupp.diariokevinali

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

/**
 * Quita el modo de batería "Restringido" usando Shizuku (sin root), para que
 * ThorRadarService pueda pasar a primer plano en segundo plano.
 */
object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 4242

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder() && !Shizuku.isPreV11()
    } catch (e: Exception) {
        false
    }

    private fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    /** Pide permiso a Shizuku si hace falta y ejecuta el arreglo. onResult corre en el hilo principal. */
    fun allowBackground(context: Context, onResult: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        if (hasPermission()) {
            runFix(appContext) { ok -> main.post { onResult(ok) } }
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    runFix(appContext) { ok -> main.post { onResult(ok) } }
                } else {
                    main.post { onResult(false) }
                }
            }
        }
        try {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            Shizuku.removeRequestPermissionResultListener(listener)
            Log.w(TAG, "No se pudo pedir permiso a Shizuku: ${e.message}")
            onResult(false)
        }
    }

    private fun runFix(context: Context, onDone: (Boolean) -> Unit) {
        thread(name = "shizuku-fix") {
            val pkg = context.packageName
            runCommand("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
            runCommand("cmd appops set $pkg RUN_IN_BACKGROUND allow")
            runCommand("dumpsys deviceidle whitelist +$pkg")
            onDone(!PermissionHelper.isBackgroundRestricted(context))
        }
    }

    private fun runCommand(command: String): Boolean = try {
        // Shizuku.newProcess es privado desde la API 13; es la forma documentada de ejecutar un comando shell
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        val exit = process.waitFor()
        Log.d(TAG, "'$command' → exit $exit")
        exit == 0
    } catch (e: Exception) {
        Log.w(TAG, "Error ejecutando '$command' con Shizuku: ${e.message}")
        false
    }
}
