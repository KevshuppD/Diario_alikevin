package calendario.kevshupp.diariokevinali;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

public class ThorWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("thor_widget_prefs", Context.MODE_PRIVATE);
        String name = prefs.getString("pet_name", "Thor");
        int level = prefs.getInt("pet_level", 1);
        int happiness = prefs.getInt("pet_happiness", 100);
        String status = prefs.getString("pet_status", "FELIZ");
        String accessory = prefs.getString("pet_accessory", "none");
        boolean isSleeping = prefs.getBoolean("pet_sleeping", false);

        int thorImageRes;
        if ("collar".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_collar;
        } else if ("mustache".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_mustache;
        } else if ("balloon".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_balloon;
        } else if ("bow".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_bow;
        } else if ("hat".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_hat;
        } else if ("bandana".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_bandana;
        } else if ("glasses".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_glasses;
        } else if ("crown".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_crown;
        } else if ("banana".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_banana;
        } else if ("socks".equals(accessory)) {
            thorImageRes = R.drawable.ic_thor_socks;
        } else {
            thorImageRes = R.drawable.ic_thor_base_trans;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_thor);
        views.setImageViewResource(R.id.widget_pet_avatar, thorImageRes);
        views.setTextViewText(R.id.widget_pet_name, name);
        views.setTextViewText(R.id.widget_pet_level, "LVL " + level);
        
        String displayStatus = isSleeping ? "DURMIENDO 💤" : status;
        views.setTextViewText(R.id.widget_pet_status, "Estado: " + displayStatus);
        
        String smile = isSleeping ? "💤" : (happiness > 40 ? "😊" : "😢");
        views.setTextViewText(R.id.widget_pet_happiness, "Felicidad: " + happiness + "% " + smile);

        // Al tocar el widget, abre la MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void triggerUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, ThorWidgetProvider.class));
        if (ids.length > 0) {
            Intent intent = new Intent(context, ThorWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        }
    }
}
