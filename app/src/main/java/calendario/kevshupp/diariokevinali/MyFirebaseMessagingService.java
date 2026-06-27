package calendario.kevshupp.diariokevinali;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "Mensaje recibido de: " + remoteMessage.getFrom());
        
        String title = "Nuevo mensaje";
        String body = "";

        // FCM v1 puede enviar los datos en el objeto 'notification' o 'data'
        if (remoteMessage.getNotification() != null) {
            String nTitle = remoteMessage.getNotification().getTitle();
            String nBody = remoteMessage.getNotification().getBody();
            if (nTitle != null) title = nTitle;
            if (nBody != null) body = nBody;
        } else if (remoteMessage.getData().size() > 0) {
            String dTitle = remoteMessage.getData().get("title");
            String dBody = remoteMessage.getData().get("body");
            if (dTitle != null) title = dTitle;
            if (dBody != null) body = dBody;
        }

        String imageUrl = remoteMessage.getData().get("imageUrl");
        String authorId = remoteMessage.getData().get("authorId");
        
        Log.d("FCM", "Datos recibidos - Title: " + title + ", Body: " + body + ", AuthorId: " + authorId);

        // Evitar mostrar mi propia notificación
        String myId = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).getString("userId", "");
        if (authorId != null && authorId.equals(myId)) {
            Log.d("FCM", "Ignorando notificación propia");
            return;
        }

        String clickType = remoteMessage.getData().get("click_type");
        sendNotification(title, body != null ? body : "", imageUrl, clickType);
    }

    private void sendNotification(String title, String messageBody, String imageUrl, String clickType) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (clickType != null) {
            intent.putExtra("click_type", clickType);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "diario_channel";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            new Thread(() -> {
                Bitmap bitmap = null;
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    try (InputStream input = connection.getInputStream()) {
                        bitmap = BitmapFactory.decodeStream(input);
                    }
                } catch (Exception e) {
                    Log.e("FCM", "Error downloading notification image", e);
                }

                if (bitmap != null) {
                    notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .setSummaryText(messageBody));
                }
                notifyNow(channelId, notificationBuilder);
            }).start();
        } else {
            notifyNow(channelId, notificationBuilder);
        }
    }

    private void notifyNow(String channelId, NotificationCompat.Builder notificationBuilder) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        NotificationChannel channel = new NotificationChannel(channelId, "Diario", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
