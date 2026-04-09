package calendario.kevshupp.diariokevinali;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.widget.RemoteViews;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LastMessageWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false, true);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        updateAppWidget(context, appWidgetManager, appWidgetId, false, true);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean forceLarge, boolean forceSmall) {
        SharedPreferences prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE);
        String coupleId = prefs.getString("coupleId", null);
        String myId = prefs.getString("userId", null);

        boolean isLarge;
        if (forceLarge) {
            isLarge = true;
        } else if (forceSmall) {
            isLarge = false;
        } else {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            isLarge = minHeight >= 180;
        }
        final boolean isLargeFinal = isLarge;
        int layoutId = isLargeFinal ? R.layout.widget_last_message_large : R.layout.widget_last_message;

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        Intent baseIntent = new Intent(context, MainActivity.class);
        baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent basePending = PendingIntent.getActivity(context, appWidgetId, baseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, basePending);

        if (coupleId != null && myId != null) {
            FirebaseFirestore.getInstance().collection("messages")
                .whereEqualTo("partnerId", coupleId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String lastContent = "No hay mensajes nuevos";
                    String senderName = "Pareja";
                    String messageId = null;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String authorId = doc.getString("authorId");
                        String content = doc.getString("content");
                        if (authorId != null && !authorId.equals(myId) && content != null && !content.startsWith("[ALBUM]")) {
                            lastContent = Html.fromHtml(content).toString();
                            senderName = doc.getString("authorName");
                            messageId = doc.getId();
                            break;
                        }
                    }

                    views.setTextViewText(R.id.tvWidgetTitle, "Mensaje de " + senderName);
                    views.setTextViewText(R.id.tvWidgetMessage, lastContent);

                    if (messageId != null) {
                        Intent msgIntent = new Intent(context, MainActivity.class);
                        msgIntent.putExtra("openMessageId", messageId);
                        msgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent msgPending = PendingIntent.getActivity(context, appWidgetId, msgIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(R.id.widgetRoot, msgPending);
                    }

                    if (isLargeFinal) {
                        FirebaseFirestore.getInstance().collection("calendar")
                            .whereEqualTo("partnerId", coupleId)
                            .get()
                            .addOnSuccessListener(eventSnap -> {
                                long now = System.currentTimeMillis();
                                DocumentSnapshot closest = null;
                                Long closestTs = null;
                                if (eventSnap != null) {
                                    for (DocumentSnapshot doc : eventSnap.getDocuments()) {
                                        Long ts = doc.getLong("date");
                                        if (ts == null || ts < now) continue;
                                        if (closestTs == null || ts < closestTs) {
                                            closest = doc;
                                            closestTs = ts;
                                        }
                                    }
                                }

                                if (closest != null && closestTs != null) {
                                    String title = closest.getString("title");
                                    SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM HH:mm", new Locale("es", "ES"));
                                    String dateText = sdf.format(new Date(closestTs));
                                    views.setTextViewText(R.id.tvWidgetEventTitle, title != null ? title : "Cita próxima");
                                    views.setTextViewText(R.id.tvWidgetEventDate, dateText);
                                } else {
                                    views.setTextViewText(R.id.tvWidgetEventTitle, "Sin eventos próximos");
                                    views.setTextViewText(R.id.tvWidgetEventDate, "");
                                }
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            })
                            .addOnFailureListener(e -> {
                                views.setTextViewText(R.id.tvWidgetEventTitle, "Sin eventos próximos");
                                views.setTextViewText(R.id.tvWidgetEventDate, "");
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            });
                    } else {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                })
                .addOnFailureListener(e -> {
                    views.setTextViewText(R.id.tvWidgetMessage, "Error cargando mensajes");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
        } else {
            views.setTextViewText(R.id.tvWidgetMessage, "Inicia sesión primero");
            if (isLargeFinal) {
                views.setTextViewText(R.id.tvWidgetEventTitle, "Inicia sesión primero");
                views.setTextViewText(R.id.tvWidgetEventDate, "");
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Si hay una actualización manual o push, podemos forzar el update
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, LastMessageWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}