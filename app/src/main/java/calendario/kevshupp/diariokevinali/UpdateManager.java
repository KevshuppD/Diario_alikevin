package calendario.kevshupp.diariokevinali;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private final Context context;
    private final DownloadManager downloadManager;
    private long latestDownloadId = -1;
    private boolean hasShownDownloadCompleteDialog = false;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    public interface UpdateCallback {
        void onUpdateAvailable(String url);
        void onNoUpdate();
        void onDownloadProgress(int progress);
        void onDownloadComplete();
    }

    public UpdateManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public void checkForUpdates(UpdateCallback callback) {
        String repoUrl = "https://api.github.com/repos/KevshuppD/Diario_alikevin/releases/latest";
        new OkHttpClient().newCall(new Request.Builder().url(repoUrl).build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                Log.e(TAG, "Request failed", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback::onNoUpdate);
                }
            }
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                if (r.isSuccessful() && r.body() != null) {
                    try {
                        String body = r.body().string();
                        JSONObject j = new JSONObject(body);
                        String latestTag = j.getString("tag_name");
                        String currentVersion = BuildConfig.VERSION_NAME;

                        if (isNewerVersion(currentVersion, latestTag.replace("v", ""))) {
                            String url = null;
                            org.json.JSONArray assets = j.getJSONArray("assets");
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                if (asset.getString("name").endsWith(".apk")) {
                                    url = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                            if (url != null && callback != null) {
                                String finalUrl = url;
                                new Handler(Looper.getMainLooper()).post(() -> callback.onUpdateAvailable(finalUrl));
                            } else if (callback != null) {
                                new Handler(Looper.getMainLooper()).post(callback::onNoUpdate);
                            }
                        } else if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(callback::onNoUpdate);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(callback::onNoUpdate);
                        }
                    }
                } else if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback::onNoUpdate);
                }
            }
        });
    }

    private boolean isNewerVersion(String current, String latest) {
        try {
            current = current.replace("v", "");
            latest = latest.replace("v", "");
            String[] currParts = current.split("\\.");
            String[] lateParts = latest.split("\\.");
            int length = Math.max(currParts.length, lateParts.length);
            for (int i = 0; i < length; i++) {
                int curr = i < currParts.length ? Integer.parseInt(currParts[i].replaceAll("[^0-9]", "")) : 0;
                int late = i < lateParts.length ? Integer.parseInt(lateParts[i].replaceAll("[^0-9]", "")) : 0;
                if (late > curr) return true;
                if (curr > late) return false;
            }
        } catch (Exception e) {
            return !current.equals(latest);
        }
        return false;
    }

    public void downloadUpdate(String url, UpdateCallback callback) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Descargando actualización");
        request.setDescription("Versión " + url.substring(url.lastIndexOf("/") + 1));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "DiarioKevinali_update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        latestDownloadId = downloadManager.enqueue(request);
        hasShownDownloadCompleteDialog = false;

        if (updateProgressRunnable != null) updateHandler.removeCallbacks(updateProgressRunnable);

        updateProgressRunnable = new Runnable() {
            @Override public void run() {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(latestDownloadId);
                try (Cursor cursor = downloadManager.query(q)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (total > 0) {
                            int progress = (int) ((downloaded * 100L) / total);
                            if (callback != null) callback.onDownloadProgress(progress);
                        }
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            if (callback != null) callback.onDownloadComplete();
                            updateProgressRunnable = null;
                            return;
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            updateProgressRunnable = null;
                            return;
                        }
                    }
                }
                updateHandler.postDelayed(this, 500);
            }
        };
        updateHandler.post(updateProgressRunnable);
    }

    public void installApk() {
        if (latestDownloadId == -1) return;
        Uri uri = downloadManager.getUriForDownloadedFile(latestDownloadId);
        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        }
    }
}
