package calendario.kevshupp.diariokevinali;

import android.app.Application;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.signed.Signature;
import com.cloudinary.android.signed.SignatureProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DiarioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
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
