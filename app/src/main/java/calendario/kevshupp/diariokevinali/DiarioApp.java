package calendario.kevshupp.diariokevinali;

import android.app.Application;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.signed.Signature;
import com.cloudinary.android.signed.SignatureProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import coil.ImageLoader;
import coil.ImageLoaderFactory;
import coil.disk.DiskCache;
import coil.memory.MemoryCache;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class DiarioApp extends Application implements ImageLoaderFactory {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Habilitar persistencia offline de Firestore con un caché mayor (50MB)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dhaqjw7se");
        
        MediaManager.init(this, new SignatureProvider() {
            @Override
            public Signature provideSignature(Map options) {
                String apiSecret = "mU2Dk2JSYPVpjkuYJebvOaiGLyc";
                String apiKey = "199351452699291";
                
                // Crear una copia editable de los parámetros
                Map<String, Object> params = new HashMap<>(options);
                
                // Cloudinary requiere un timestamp. Si no viene, lo generamos.
                if (params.get("timestamp") == null) {
                    params.put("timestamp", System.currentTimeMillis() / 1000);
                }

                // Ordenar parámetros alfabéticamente para la firma
                TreeMap<String, Object> sorted = new TreeMap<>(params);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                }
                
                // Añadir el API Secret al final de la cadena
                sb.append(apiSecret);
                
                String signature = sha1(sb.toString());
                
                // Obtener el timestamp de forma segura para el objeto Signature
                long timestamp = 0;
                Object tsValue = params.get("timestamp");
                if (tsValue instanceof Number) {
                    timestamp = ((Number) tsValue).longValue();
                } else if (tsValue != null) {
                    try {
                        timestamp = Long.parseLong(tsValue.toString());
                    } catch (NumberFormatException e) {
                        timestamp = System.currentTimeMillis() / 1000;
                    }
                }

                return new Signature(signature, apiKey, timestamp);
            }

            @Override
            public String getName() {
                return "DiarioAppSignatureProvider";
            }
        }, config);
        
        long interval = getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
                .getLong("updateInterval", 720L); // 12h por defecto
        rescheduleUpdateCheck(this, interval, ExistingPeriodicWorkPolicy.KEEP);
        createNotificationChannel();
        schedulePetCareCheck(this);
    }

    private void schedulePetCareCheck(Context context) {
        androidx.work.PeriodicWorkRequest petCareRequest = new androidx.work.PeriodicWorkRequest.Builder(
                PetCareWorker.class, 2, java.util.concurrent.TimeUnit.HOURS)
                .build();
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PetCareCheck",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                petCareRequest
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Diario";
            String description = "Notificaciones de mensajes y momentos compartidos";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("diario_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    public static void rescheduleUpdateCheck(Context context, long intervalMinutes) {
        rescheduleUpdateCheck(context, intervalMinutes, ExistingPeriodicWorkPolicy.KEEP);
    }

    public static void rescheduleUpdateCheck(Context context, long intervalMinutes, ExistingPeriodicWorkPolicy policy) {
        PeriodicWorkRequest updateRequest = new PeriodicWorkRequest.Builder(UpdateWorker.class, intervalMinutes, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpdateCheck",
                policy,
                updateRequest
        );
    }

    @NonNull
    @Override
    public ImageLoader newImageLoader() {
        return new ImageLoader.Builder(this)
                .memoryCache(() -> new MemoryCache.Builder(this)
                        .maxSizePercent(0.25) // Usar hasta el 25% de la memoria disponible
                        .build())
                .diskCache(() -> {
                    long cacheSizeMB = getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
                            .getLong("cacheSizeLimit", 100L);
                    return new DiskCache.Builder()
                            .directory(getCacheDir().toPath().resolve("image_cache").toFile())
                            .maxSizeBytes(cacheSizeMB * 1024 * 1024)
                            .build();
                })
                .crossfade(true)
                .build();
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] result = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("DiarioApp", "Error al generar SHA-1", e);
            return "";
        }
    }
}
