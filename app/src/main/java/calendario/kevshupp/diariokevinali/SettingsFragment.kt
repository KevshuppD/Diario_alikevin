package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.SettingsScreen

class SettingsFragment : Fragment() {
    private var theme: String = "Pixel Claro"

    companion object {
        @JvmStatic
        fun newInstance(userId: String, partnerId: String, theme: String): SettingsFragment {
            val f = SettingsFragment()
            val a = Bundle()
            a.putString("theme", theme)
            f.arguments = a
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme = arguments?.getString("theme") ?: "Pixel Claro"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val act = activity as? MainActivity
                val prefs = act?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                
                // Usar remember para que Compose mantenga el estado correctamente
                var currentTheme by remember { mutableStateOf(theme) }
                var useCustomBg by remember {
                    mutableStateOf(prefs?.getBoolean("useCustomBg", false) ?: false)
                }
                var currentCacheLimit by remember { 
                    mutableStateOf(prefs?.getLong("cacheSizeLimit", 100L) ?: 100L) 
                }
                var currentUpdateInterval by remember {
                    mutableStateOf(prefs?.getLong("updateInterval", 720L) ?: 720L)
                }
                var currentAppointmentLeadTime by remember {
                    mutableStateOf(prefs?.getLong("appointmentLeadTime", 60L) ?: 60L)
                }

                // Estados de sincronización con Google Drive
                var googleAccountEmail by remember {
                    mutableStateOf(prefs?.getString("syncGoogleAccountEmail", null))
                }
                var selectedFolderUri by remember {
                    mutableStateOf(prefs?.getString("syncLocalFolderUri", null))
                }
                var syncIntervalMinutes by remember {
                    mutableStateOf(prefs?.getLong("syncIntervalMinutes", 0L) ?: 0L)
                }
                var wifiOnly by remember {
                    mutableStateOf(prefs?.getBoolean("syncWifiOnly", true) ?: true)
                }
                var chargingOnly by remember {
                    mutableStateOf(prefs?.getBoolean("syncChargingOnly", false) ?: false)
                }
                var isSyncing by remember { mutableStateOf(false) }
                var syncProgress by remember { mutableStateOf(-1) }
                var syncStatus by remember { mutableStateOf("") }
                var syncState by remember {
                    mutableStateOf(prefs?.getString("syncState", "NO_SINCRONIZADO") ?: "NO_SINCRONIZADO")
                }
                var syncMaxRetries by remember {
                    mutableStateOf(prefs?.getInt("syncMaxRetries", 3) ?: 3)
                }
                var syncLastError by remember {
                    mutableStateOf(prefs?.getString("syncLastError", null))
                }
                var syncParallelLines by remember {
                    mutableStateOf(prefs?.getInt("syncParallelLines", 3) ?: 3)
                }
                var localFilesCount by remember { mutableStateOf(0) }
                var cloudFilesCount by remember { mutableStateOf(0) }
                var activeSyncSlots by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

                val coupleId = prefs?.getString("coupleId", null)

                // Escuchar cambios de SharedPreferences para actualizar Compose en tiempo real
                DisposableEffect(prefs) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                        when (key) {
                            "syncGoogleAccountEmail" -> googleAccountEmail = p.getString(key, null)
                            "syncLocalFolderUri" -> selectedFolderUri = p.getString(key, null)
                            "syncIntervalMinutes" -> syncIntervalMinutes = p.getLong(key, 0L)
                            "syncWifiOnly" -> wifiOnly = p.getBoolean(key, true)
                            "syncChargingOnly" -> chargingOnly = p.getBoolean(key, false)
                            "syncState" -> syncState = p.getString(key, "NO_SINCRONIZADO") ?: "NO_SINCRONIZADO"
                            "syncMaxRetries" -> syncMaxRetries = p.getInt(key, 3)
                            "syncLastError" -> syncLastError = p.getString(key, null)
                            "syncParallelLines" -> syncParallelLines = p.getInt(key, 3)
                        }
                    }
                    prefs?.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs?.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                // Escuchar en tiempo real contador de Firestore de la nube
                DisposableEffect(coupleId) {
                    if (coupleId.isNullOrEmpty()) {
                        cloudFilesCount = 0
                        onDispose {}
                    } else {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val listener = db.collection("pets").document(coupleId).collection("drive_sync_metadata")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.e("SettingsFragment", "Error escuchando metadatos de Firestore: ${error.message}")
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    val count = snapshot.documents.count { doc ->
                                        doc.getBoolean("eliminado") != true
                                    }
                                    cloudFilesCount = count
                                }
                            }
                        onDispose {
                            listener.remove()
                        }
                    }
                }

                // Calcular conteo local SAF reactivamente
                LaunchedEffect(selectedFolderUri, isSyncing) {
                    if (!selectedFolderUri.isNullOrEmpty()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val context = requireContext()
                                val uri = Uri.parse(selectedFolderUri)
                                val documentId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
                                val projection = arrayOf(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                                
                                var count = 0
                                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                                    val mimeTypeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                                    while (cursor.moveToNext()) {
                                        val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) ?: "" else ""
                                        if (mimeType.startsWith("image/")) {
                                            count++
                                        }
                                    }
                                }
                                localFilesCount = count
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error contando archivos locales via cursor: ${e.message}")
                                localFilesCount = 0
                            }
                        }
                    } else {
                        localFilesCount = 0
                    }
                }

                // Observar el estado de WorkManager para saber si se está sincronizando actualmente
                val workInfos = act?.let {
                    androidx.work.WorkManager.getInstance(it)
                        .getWorkInfosForUniqueWorkLiveData(SyncScheduler.UNIQUE_ONETIME_WORK_NAME)
                }
                val periodicWorkInfos = act?.let {
                    androidx.work.WorkManager.getInstance(it)
                        .getWorkInfosForUniqueWorkLiveData("SyncDrivePeriodicWork")
                }

                LaunchedEffect(workInfos, periodicWorkInfos, syncParallelLines) {
                    workInfos?.observe(viewLifecycleOwner) { infos ->
                        val runningInfo = infos?.find { it.state == androidx.work.WorkInfo.State.RUNNING }
                        if (runningInfo != null) {
                            isSyncing = true
                            syncProgress = runningInfo.progress.getInt("progress", -1)
                            syncStatus = runningInfo.progress.getString("status") ?: "Sincronizando..."
                            
                            val tempSlots = mutableListOf<Pair<String, Int>>()
                            for (i in 0 until syncParallelLines) {
                                val name = runningInfo.progress.getString("slot_${i}_name") ?: ""
                                val prog = runningInfo.progress.getInt("slot_${i}_progress", -1)
                                if (name.isNotEmpty()) {
                                    tempSlots.add(Pair(name, prog))
                                }
                            }
                            activeSyncSlots = tempSlots
                        } else {
                            val periodicRunningInfo = periodicWorkInfos?.value?.find { it.state == androidx.work.WorkInfo.State.RUNNING }
                            if (periodicRunningInfo != null) {
                                isSyncing = true
                                syncProgress = periodicRunningInfo.progress.getInt("progress", -1)
                                syncStatus = periodicRunningInfo.progress.getString("status") ?: "Sincronizando..."
                                
                                val tempSlots = mutableListOf<Pair<String, Int>>()
                                for (i in 0 until syncParallelLines) {
                                    val name = periodicRunningInfo.progress.getString("slot_${i}_name") ?: ""
                                    val prog = periodicRunningInfo.progress.getInt("slot_${i}_progress", -1)
                                    if (name.isNotEmpty()) {
                                        tempSlots.add(Pair(name, prog))
                                    }
                                }
                                activeSyncSlots = tempSlots
                            } else {
                                isSyncing = false
                                syncProgress = -1
                                syncStatus = ""
                                activeSyncSlots = emptyList()
                                if (prefs?.getString("syncState", "") == "SINCRONIZANDO") {
                                    prefs.edit().putString("syncState", "NO_SINCRONIZADO").apply()
                                }
                            }
                        }
                    }
                    periodicWorkInfos?.observe(viewLifecycleOwner) { infos ->
                        val runningInfo = infos?.find { it.state == androidx.work.WorkInfo.State.RUNNING }
                        if (runningInfo != null) {
                            isSyncing = true
                            syncProgress = runningInfo.progress.getInt("progress", -1)
                            syncStatus = runningInfo.progress.getString("status") ?: "Sincronizando..."
                            
                            val tempSlots = mutableListOf<Pair<String, Int>>()
                            for (i in 0 until syncParallelLines) {
                                val name = runningInfo.progress.getString("slot_${i}_name") ?: ""
                                val prog = runningInfo.progress.getInt("slot_${i}_progress", -1)
                                if (name.isNotEmpty()) {
                                    tempSlots.add(Pair(name, prog))
                                }
                            }
                            activeSyncSlots = tempSlots
                        } else {
                            val oneTimeRunningInfo = workInfos?.value?.find { it.state == androidx.work.WorkInfo.State.RUNNING }
                            if (oneTimeRunningInfo != null) {
                                isSyncing = true
                                syncProgress = oneTimeRunningInfo.progress.getInt("progress", -1)
                                syncStatus = oneTimeRunningInfo.progress.getString("status") ?: "Sincronizando..."
                                
                                val tempSlots = mutableListOf<Pair<String, Int>>()
                                for (i in 0 until syncParallelLines) {
                                    val name = oneTimeRunningInfo.progress.getString("slot_${i}_name") ?: ""
                                    val prog = oneTimeRunningInfo.progress.getInt("slot_${i}_progress", -1)
                                    if (name.isNotEmpty()) {
                                        tempSlots.add(Pair(name, prog))
                                    }
                                }
                                activeSyncSlots = tempSlots
                            } else {
                                isSyncing = false
                                syncProgress = -1
                                syncStatus = ""
                                activeSyncSlots = emptyList()
                                if (prefs?.getString("syncState", "") == "SINCRONIZANDO") {
                                    prefs.edit().putString("syncState", "NO_SINCRONIZADO").apply()
                                }
                            }
                        }
                    }
                }

                androidx.compose.material3.MaterialTheme {
                    SettingsScreen(
                        currentTheme = currentTheme,
                        useCustomBg = useCustomBg,
                        onBgPreferenceChange = { newVal ->
                            useCustomBg = newVal
                            prefs?.edit()?.putBoolean("useCustomBg", newVal)?.apply()
                            val lightCol = prefs?.getString("lightColor", "#D1C4E9")
                            val darkCol = prefs?.getString("darkColor", "#4A148C")
                            act?.applyTheme(currentTheme, lightCol, darkCol)
                        },
                        versionName = BuildConfig.VERSION_NAME,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            theme = newTheme // Actualizar la propiedad del fragmento también
                            prefs?.edit()?.putString("theme", newTheme)?.apply()
                            val lightCol = prefs?.getString("lightColor", "#D1C4E9")
                            val darkCol = prefs?.getString("darkColor", "#4A148C")
                            act?.applyTheme(newTheme, lightCol, darkCol)
                        },
                        onCheckUpdates = {
                            act?.getUpdateManager()?.checkForUpdates(object : UpdateManager.UpdateCallback {
                                override fun onUpdateAvailable(url: String) { act.showUpdateDialog(url) }
                                override fun onNoUpdate() {
                                    act?.runOnUiThread {
                                        android.widget.Toast.makeText(requireContext(), "No hay actualizaciones disponibles", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onDownloadProgress(progress: Int) {}
                                override fun onDownloadComplete() {}
                            })
                        },
                        onLogout = { act?.logout() },
                        onBack = { act?.onBackPressedDispatcher?.onBackPressed() },
                        onColorSelect = { colorHex ->
                            val isDark = currentTheme == "Pixel Oscuro"
                            if (isDark) {
                                prefs?.edit()?.putString("darkColor", colorHex)?.apply()
                                act?.applyTheme(currentTheme, null, colorHex)
                            } else {
                                prefs?.edit()?.putString("lightColor", colorHex)?.apply()
                                act?.applyTheme(currentTheme, colorHex, null)
                            }
                        },
                        currentCacheLimit = currentCacheLimit,
                        onCacheLimitChange = { newLimit ->
                            currentCacheLimit = newLimit
                            prefs?.edit()?.putLong("cacheSizeLimit", newLimit)?.apply()
                        },
                        onTestNotification = {
                            act?.testLocalNotification()
                        },
                        updateInterval = currentUpdateInterval,
                        onUpdateIntervalChange = { newInterval ->
                            currentUpdateInterval = newInterval
                            prefs?.edit()?.putLong("updateInterval", newInterval)?.apply()
                            DiarioApp.rescheduleUpdateCheck(requireContext(), newInterval)
                        },
                        appointmentLeadTime = currentAppointmentLeadTime,
                        onAppointmentLeadTimeChange = { newLeadTime ->
                            currentAppointmentLeadTime = newLeadTime
                            prefs?.edit()?.putLong("appointmentLeadTime", newLeadTime)?.apply()
                            act?.rescheduleAllCalendarReminders()
                        },
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncIntervalMinutes = syncIntervalMinutes,
                        wifiOnly = wifiOnly,
                        chargingOnly = chargingOnly,
                        syncState = syncState,
                        syncMaxRetries = syncMaxRetries,
                        syncLastError = syncLastError,
                        onMaxRetriesChange = { newRetries ->
                            prefs?.edit()?.putInt("syncMaxRetries", newRetries)?.apply()
                            syncMaxRetries = newRetries
                        },
                        onClearLastError = {
                            prefs?.edit()?.remove("syncLastError")?.apply()
                            syncLastError = null
                        },
                        onLinkGoogleDrive = {
                            act?.linkGoogleDriveAccount()
                        },
                        onUnlinkGoogleDrive = {
                            prefs?.edit()?.apply {
                                remove("syncGoogleAccountEmail")
                                remove("syncLocalFolderUri")
                                remove("syncIntervalMinutes")
                                remove("syncWifiOnly")
                                remove("syncChargingOnly")
                                remove("syncState")
                                apply()
                            }
                            googleAccountEmail = null
                            selectedFolderUri = null
                            SyncScheduler.cancelSync(requireContext())
                            android.widget.Toast.makeText(requireContext(), "Cuenta de Google desvinculada", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onSelectLocalFolder = {
                            act?.selectLocalFolder()
                        },
                        onIntervalChange = { newMins ->
                            prefs?.edit()?.putLong("syncIntervalMinutes", newMins)?.apply()
                            syncIntervalMinutes = newMins
                            SyncScheduler.scheduleSync(requireContext(), newMins, wifiOnly, chargingOnly)
                        },
                        onWifiOnlyChange = { newWifi ->
                            prefs?.edit()?.putBoolean("syncWifiOnly", newWifi)?.apply()
                            wifiOnly = newWifi
                            SyncScheduler.scheduleSync(requireContext(), syncIntervalMinutes, newWifi, chargingOnly)
                        },
                        onChargingOnlyChange = { newCharge ->
                            prefs?.edit()?.putBoolean("syncChargingOnly", newCharge)?.apply()
                            chargingOnly = newCharge
                            SyncScheduler.scheduleSync(requireContext(), syncIntervalMinutes, wifiOnly, newCharge)
                        },
                        onSyncNow = {
                            if (selectedFolderUri.isNullOrEmpty()) {
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Por favor, selecciona primero la carpeta local para sincronizar.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                act?.selectLocalFolder()
                            } else {
                                SyncScheduler.runNow(requireContext())
                                android.widget.Toast.makeText(requireContext(), "Sincronización iniciada...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStopSync = {
                            SyncScheduler.stopAllRunningSyncs(requireContext())
                            android.widget.Toast.makeText(requireContext(), "Sincronización detenida.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        isSyncing = isSyncing,
                        syncProgress = syncProgress,
                        syncStatus = syncStatus,
                        localFilesCount = localFilesCount,
                        cloudFilesCount = cloudFilesCount,
                        syncParallelLines = syncParallelLines,
                        activeSyncSlots = activeSyncSlots,
                        onParallelLinesChange = { newLines ->
                            prefs?.edit()?.putInt("syncParallelLines", newLines)?.apply()
                            syncParallelLines = newLines
                        }
                    )
                }
            }
        }
    }
}
