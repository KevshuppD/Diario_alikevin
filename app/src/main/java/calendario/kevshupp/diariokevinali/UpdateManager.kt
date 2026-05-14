package calendario.kevshupp.diariokevinali

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class UpdateManager(private val context: Context) {
    private val TAG = "UpdateManager"
    private val downloadManager: DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var latestDownloadId: Long = -1
    private var hasShownDownloadCompleteDialog = false
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null

    interface UpdateCallback {
        fun onUpdateAvailable(url: String)
        fun onNoUpdate()
        fun onDownloadProgress(progress: Int)
        fun onDownloadComplete()
    }

    fun checkForUpdates(callback: UpdateCallback?) {
        val repoUrl = "https://api.github.com/repos/KevshuppD/Diario_alikevin/releases/latest"
        val request = Request.Builder()
            .url(repoUrl)
            .header("User-Agent", "DiarioKevinAli-App")
            .header("Cache-Control", "no-cache")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(@NonNull c: Call, @NonNull e: IOException) {
                Log.e(TAG, "Request failed", e)
                callback?.let {
                    Handler(Looper.getMainLooper()).post { it.onNoUpdate() }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(@NonNull c: Call, @NonNull r: Response) {
                if (r.isSuccessful && r.body != null) {
                    try {
                        val body = r.body!!.string()
                        val j = JSONObject(body)
                        val latestTag = j.getString("tag_name")
                        val currentVersion = BuildConfig.VERSION_NAME

                        Log.d(TAG, "Checking updates. Current: $currentVersion, Latest on GitHub: $latestTag")

                        if (isNewerVersion(currentVersion, latestTag)) {
                            var url: String? = null
                            val assets: JSONArray = j.getJSONArray("assets")
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    url = asset.getString("browser_download_url")
                                    break
                                }
                            }
                            if (url != null && callback != null) {
                                val finalUrl = url
                                Handler(Looper.getMainLooper()).post { callback.onUpdateAvailable(finalUrl) }
                            } else {
                                callback?.let {
                                    Handler(Looper.getMainLooper()).post { it.onNoUpdate() }
                                }
                            }
                        } else {
                            callback?.let {
                                Handler(Looper.getMainLooper()).post { it.onNoUpdate() }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        callback?.let {
                            Handler(Looper.getMainLooper()).post { it.onNoUpdate() }
                        }
                    }
                } else {
                    Log.e(TAG, "Response not successful: ${r.code}")
                    callback?.let {
                        Handler(Looper.getMainLooper()).post { it.onNoUpdate() }
                    }
                }
            }
        })
    }

    private fun isNewerVersion(current: String?, latest: String?): Boolean {
        if (current == null || latest == null) return false
        try {
            val cleanCurrent = current.lowercase().replace("v", "").split("-")[0]
            val cleanLatest = latest.lowercase().replace("v", "").split("-")[0]

            if (cleanCurrent == cleanLatest) return false

            val currParts = cleanCurrent.split(".")
            val lateParts = cleanLatest.split(".")
            val length = maxOf(currParts.size, lateParts.size)

            for (i in 0 until length) {
                val curr = if (i < currParts.size) currParts[i].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 else 0
                val late = if (i < lateParts.size) lateParts[i].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 else 0
                if (late > curr) return true
                if (curr > late) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Version comparison error", e)
            return current != latest
        }
        return false
    }

    fun downloadUpdate(url: String, callback: UpdateCallback?) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Descargando actualización")
        request.setDescription("Versión " + url.substring(url.lastIndexOf("/") + 1))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "DiarioKevinali_update.apk")
        request.setMimeType("application/vnd.android.package-archive")

        latestDownloadId = downloadManager.enqueue(request)
        hasShownDownloadCompleteDialog = false

        updateProgressRunnable?.let { updateHandler.removeCallbacks(it) }

        updateProgressRunnable = object : Runnable {
            override fun run() {
                val q = DownloadManager.Query()
                q.setFilterById(latestDownloadId)
                val cursor: Cursor? = downloadManager.query(q)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val downloadedIdx = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)

                        if (downloadedIdx != -1 && totalIdx != -1) {
                            val downloaded = it.getInt(downloadedIdx)
                            val total = it.getInt(totalIdx)
                            if (total > 0) {
                                val progress = ((downloaded * 100L) / total).toInt()
                                callback?.onDownloadProgress(progress)
                            }
                        }

                        if (statusIdx != -1) {
                            val status = it.getInt(statusIdx)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                callback?.onDownloadComplete()
                                updateProgressRunnable = null
                                return
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                updateProgressRunnable = null
                                return
                            }
                        }
                    }
                }
                updateHandler.postDelayed(this, 500)
            }
        }
        updateProgressRunnable?.let { updateHandler.post(it) }
    }

    fun installApk() {
        if (latestDownloadId == -1L) return
        val uri = downloadManager.getUriForDownloadedFile(latestDownloadId)
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }
    }
}