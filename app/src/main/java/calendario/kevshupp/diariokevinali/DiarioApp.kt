package calendario.kevshupp.diariokevinali

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cloudinary.android.MediaManager
import com.cloudinary.android.signed.Signature
import com.cloudinary.android.signed.SignatureProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HashMap
import java.util.TreeMap
import java.util.concurrent.TimeUnit

class DiarioApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Habilitar persistencia offline de Firestore con un límite de caché de 100 MB
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024 * 1024) // 100 MB máximo
                    .build()
            )
            .build()
        db.firestoreSettings = settings

        val config: MutableMap<String, Any> = HashMap()
        config["cloud_name"] = "dhaqjw7se"

        MediaManager.init(this, object : SignatureProvider {
            override fun provideSignature(options: Map<*, *>): Signature {
                val apiSecret = "mU2Dk2JSYPVpjkuYJebvOaiGLyc"
                val apiKey = "199351452699291"

                // Crear una copia editable de los parámetros
                val params = HashMap<String, Any>()
                for ((key, value) in options) {
                    if (key is String && value != null) {
                        params[key] = value
                    }
                }

                // Cloudinary requiere un timestamp. Si no viene, lo generamos.
                if (params["timestamp"] == null) {
                    params["timestamp"] = System.currentTimeMillis() / 1000
                }

                // Ordenar parámetros alfabéticamente para la firma
                val sorted = TreeMap<String, Any>(params)
                val sb = StringBuilder()
                for ((key, value) in sorted) {
                    if (sb.isNotEmpty()) sb.append("&")
                    sb.append(key).append("=").append(value)
                }

                // Añadir el API Secret al final de la cadena
                sb.append(apiSecret)

                val signature = sha1(sb.toString())

                // Obtener el timestamp de forma segura para el objeto Signature
                var timestamp: Long = 0
                val tsValue = params["timestamp"]
                if (tsValue is Number) {
                    timestamp = tsValue.toLong()
                } else if (tsValue != null) {
                    try {
                        timestamp = tsValue.toString().toLong()
                    } catch (e: NumberFormatException) {
                        timestamp = System.currentTimeMillis() / 1000
                    }
                }

                return Signature(signature, apiKey, timestamp)
            }

            override fun getName(): String {
                return "DiarioAppSignatureProvider"
            }
        }, config)

        val interval = getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            .getLong("updateInterval", 720L) // 12h por defecto
        rescheduleUpdateCheck(this, interval, ExistingPeriodicWorkPolicy.KEEP)
        createNotificationChannel()
        schedulePetCareCheck(this)
    }

    private fun schedulePetCareCheck(context: Context) {
        val petCareRequest = PeriodicWorkRequest.Builder(
            PetCareWorker::class.java, 2, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "PetCareCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            petCareRequest
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Diario"
            val descriptionText = "Notificaciones de mensajes y momentos compartidos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("diario_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Usar hasta el 25% de la memoria disponible
                    .build()
            }
            .diskCache {
                val cacheSizeMB = getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
                    .getLong("cacheSizeLimit", 100L)
                DiskCache.Builder()
                    .directory(cacheDir.toPath().resolve("image_cache").toFile())
                    .maxSizeBytes(cacheSizeMB * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun sha1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val result = md.digest(input.toByteArray())
            val sb = StringBuilder()
            for (b in result) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.e("DiarioApp", "Error al generar SHA-1", e)
            ""
        }
    }

    companion object {
        private var okHttpClient: OkHttpClient? = null

        @JvmStatic
        @Synchronized
        fun getOkHttpClient(): OkHttpClient {
            if (okHttpClient == null) {
                okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            return okHttpClient!!
        }

        @JvmStatic
        fun rescheduleUpdateCheck(context: Context, intervalMinutes: Long) {
            rescheduleUpdateCheck(context, intervalMinutes, ExistingPeriodicWorkPolicy.KEEP)
        }

        @JvmStatic
        fun rescheduleUpdateCheck(
            context: Context,
            intervalMinutes: Long,
            policy: ExistingPeriodicWorkPolicy
        ) {
            val updateRequest = PeriodicWorkRequest.Builder(
                UpdateWorker::class.java, intervalMinutes, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpdateCheck",
                policy,
                updateRequest
            )
        }
    }
}
