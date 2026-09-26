package calendario.kevshupp.diariokevinali

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object SosAlarmHelper {
    private const val TAG = "SosAlarmHelper"
    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var isPlaying = false

    @Synchronized
    fun playSosAlarm(context: Context) {
        if (isPlaying) return
        isPlaying = true
        val appContext = context.applicationContext
        try {
            // 1. Vibración continua de alarma SOS
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // Patrón SOS en código morse / alarma de alta intensidad (... --- ...)
            val pattern = longArrayOf(0, 700, 200, 700, 200, 700, 400, 350, 150, 350, 150, 350, 400, 700, 200, 700, 200, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repetir continuamente
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

            // 2. Reproducción de sonido de alarma SOS a través del canal ALARM
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            stopMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(appContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "🚨 [SOS] Alarma sonora y vibración SOS iniciadas con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando sonido de alarma SOS", e)
        }
    }

    @Synchronized
    fun stopSosAlarm(context: Context? = null) {
        isPlaying = false
        stopMediaPlayer()
        try {
            val ctx = context?.applicationContext
            if (ctx != null) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            }
            Log.d(TAG, "🛑 [SOS] Alarma sonora y vibración SOS detenidas")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelando vibrador SOS: ${e.message}")
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando MediaPlayer de SOS: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}
