package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.content.Context
import android.app.Dialog
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import calendario.kevshupp.diariokevinali.compose.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {
    private var theme: String = "Pixel Claro"

    companion object {
        @JvmStatic
        fun newInstance(userId: String, partnerId: String, theme: String): SettingsFragment {
            val f = SettingsFragment()
            val a = Bundle()
            a.putString("userId", userId)
            a.putString("partnerId", partnerId)
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
        val userId = arguments?.getString("userId") ?: "user_kevin_01"
        return ComposeView(requireContext()).apply {
            setContent {
                val act = activity as? MainActivity
                val prefs = act?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)

                val updateFirestoreSetting: (String, Any) -> Unit = { key, value ->
                    if (userId.isNotEmpty()) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        db.collection("users").document(userId).update(key, value)
                            .addOnFailureListener {
                                db.collection("users").document(userId).set(mapOf(key to value), com.google.firebase.firestore.SetOptions.merge())
                            }
                    }
                }
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
                var syncDirection by remember {
                    mutableStateOf(prefs?.getString("syncDirection", "BIDIRECTIONAL") ?: "BIDIRECTIONAL")
                }
                var localFilesCount by remember { mutableStateOf(0) }
                var cloudFilesCount by remember { mutableStateOf(0) }
                var activeSyncSlots by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

                val duplicateManager = remember { DuplicateManager(requireContext()) }
                val coroutineScope = rememberCoroutineScope()
                
                var isScanningDuplicates by remember { mutableStateOf(false) }
                var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
                var scanCompleted by remember { mutableStateOf(false) }
                var scannedCount by remember { mutableStateOf(0) }
                var totalToScan by remember { mutableStateOf(0) }
                var deletedPhotosCount by remember { mutableStateOf(0) }
                var spaceFreedBytes by remember { mutableStateOf(0L) }
                var isDeleting by remember { mutableStateOf(false) }

                val onScanDuplicates: () -> Unit = {
                    if (!selectedFolderUri.isNullOrEmpty()) {
                        isScanningDuplicates = true
                        scanCompleted = false
                        deletedPhotosCount = 0
                        spaceFreedBytes = 0L
                        scannedCount = 0
                        totalToScan = 0
                        coroutineScope.launch {
                            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    duplicateManager.findDuplicates(selectedFolderUri!!) { scanned, total ->
                                        isScanningDuplicates = true
                                        scannedCount = scanned
                                        totalToScan = total
                                    }
                                } catch (e: Exception) {
                                    Log.e("SettingsFragment", "Error scanning duplicates: ${e.message}", e)
                                    emptyList()
                                }
                            }
                            duplicateGroups = result
                            isScanningDuplicates = false
                            scanCompleted = true
                        }
                    }
                }

                val onDeleteDuplicates: (List<LocalPhoto>) -> Unit = { listToDelete ->
                    if (listToDelete.isNotEmpty()) {
                        isDeleting = true
                        scannedCount = 0
                        totalToScan = listToDelete.size
                        coroutineScope.launch {
                            val (freedCount, freedSpace) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                var space = 0L
                                val count = duplicateManager.deleteDuplicateFiles(listToDelete) { progress ->
                                    scannedCount = progress
                                }
                                listToDelete.forEach {
                                    space += it.size
                                }
                                Pair(count, space)
                            }
                            deletedPhotosCount = freedCount
                            spaceFreedBytes = freedSpace
                            isDeleting = false
                            scanCompleted = false
                            localFilesCount = maxOf(0, localFilesCount - freedCount)
                        }
                    }
                }

                val onResetDuplicateState: () -> Unit = {
                    isScanningDuplicates = false
                    duplicateGroups = emptyList()
                    scanCompleted = false
                    scannedCount = 0
                    totalToScan = 0
                    deletedPhotosCount = 0
                    spaceFreedBytes = 0L
                    isDeleting = false
                }

                val coupleId = prefs?.getString("coupleId", null)

                // Escuchar cambios de SharedPreferences para actualizar Compose en tiempo real
                DisposableEffect(prefs) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                        when (key) {
                            "theme" -> currentTheme = p.getString(key, "Pixel Claro") ?: "Pixel Claro"
                            "useCustomBg" -> useCustomBg = p.getBoolean(key, false)
                            "cacheSizeLimit" -> currentCacheLimit = p.getLong(key, 100L)
                            "updateInterval" -> currentUpdateInterval = p.getLong(key, 720L)
                            "appointmentLeadTime" -> currentAppointmentLeadTime = p.getLong(key, 60L)
                            "syncGoogleAccountEmail" -> googleAccountEmail = p.getString(key, null)
                            "syncLocalFolderUri" -> selectedFolderUri = p.getString(key, null)
                            "syncIntervalMinutes" -> syncIntervalMinutes = p.getLong(key, 0L)
                            "syncWifiOnly" -> wifiOnly = p.getBoolean(key, true)
                            "syncChargingOnly" -> chargingOnly = p.getBoolean(key, false)
                            "syncState" -> syncState = p.getString(key, "NO_SINCRONIZADO") ?: "NO_SINCRONIZADO"
                            "syncMaxRetries" -> syncMaxRetries = p.getInt(key, 3)
                            "syncLastError" -> syncLastError = p.getString(key, null)
                            "syncParallelLines" -> syncParallelLines = p.getInt(key, 3)
                            "syncDirection" -> syncDirection = p.getString(key, "BIDIRECTIONAL") ?: "BIDIRECTIONAL"
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
                            updateFirestoreSetting("useCustomBg", newVal)
                        },
                        versionName = BuildConfig.VERSION_NAME,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            theme = newTheme // Actualizar la propiedad del fragmento también
                            prefs?.edit()?.putString("theme", newTheme)?.apply()
                            val lightCol = prefs?.getString("lightColor", "#D1C4E9")
                            val darkCol = prefs?.getString("darkColor", "#4A148C")
                            act?.applyTheme(newTheme, lightCol, darkCol)
                            updateFirestoreSetting("theme", newTheme)
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
                                updateFirestoreSetting("darkColor", colorHex)
                            } else {
                                prefs?.edit()?.putString("lightColor", colorHex)?.apply()
                                act?.applyTheme(currentTheme, colorHex, null)
                                updateFirestoreSetting("lightColor", colorHex)
                            }
                        },
                        currentCacheLimit = currentCacheLimit,
                        onCacheLimitChange = { newLimit ->
                            currentCacheLimit = newLimit
                            prefs?.edit()?.putLong("cacheSizeLimit", newLimit)?.apply()
                            updateFirestoreSetting("cacheSizeLimit", newLimit)
                        },
                        onTestNotification = {
                            act?.testLocalNotification()
                        },
                        updateInterval = currentUpdateInterval,
                        onUpdateIntervalChange = { newInterval ->
                            currentUpdateInterval = newInterval
                            prefs?.edit()?.putLong("updateInterval", newInterval)?.apply()
                            DiarioApp.rescheduleUpdateCheck(requireContext(), newInterval, androidx.work.ExistingPeriodicWorkPolicy.REPLACE)
                            updateFirestoreSetting("updateInterval", newInterval)
                        },
                        appointmentLeadTime = currentAppointmentLeadTime,
                        onAppointmentLeadTimeChange = { newLeadTime ->
                            currentAppointmentLeadTime = newLeadTime
                            prefs?.edit()?.putLong("appointmentLeadTime", newLeadTime)?.apply()
                            act?.rescheduleAllCalendarReminders()
                            updateFirestoreSetting("appointmentLeadTime", newLeadTime)
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
                        },
                        syncDirection = syncDirection,
                        onDirectionChange = { newDir ->
                            prefs?.edit()?.putString("syncDirection", newDir)?.apply()
                            syncDirection = newDir
                        },
                        onResetDrive = {
                            val context = requireContext()
                            Toast.makeText(context, "Vaciando Google Drive...", Toast.LENGTH_SHORT).show()
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val actPrefs = context.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                                    val account = GoogleSignIn.getLastSignedInAccount(context)
                                    if (account == null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: No hay cuenta vinculada", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }
                                    val credential = GoogleAccountCredential.usingOAuth2(
                                        context,
                                        listOf(DriveScopes.DRIVE_FILE)
                                    ).setSelectedAccount(account.account)

                                    val driveService = Drive.Builder(
                                        NetHttpTransport(),
                                        GsonFactory.getDefaultInstance(),
                                        credential
                                    )
                                        .setApplicationName("Diario Kevin Ali")
                                        .build()

                                    var folderId = actPrefs.getString("syncDriveFolderId", null)
                                    if (folderId.isNullOrEmpty()) {
                                        val listQuery = driveService.files().list()
                                            .setQ("name = 'DiarioAliKevin_Album' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                                            .setFields("files(id)")
                                            .execute()
                                        val files = listQuery.files
                                        if (files != null && files.isNotEmpty()) {
                                            folderId = files[0].id
                                        }
                                    }

                                    if (!folderId.isNullOrEmpty()) {
                                        driveService.files().delete(folderId).execute()
                                        Log.d("ResetDrive", "Carpeta de Google Drive eliminada con éxito.")
                                    }

                                    val db = FirebaseFirestore.getInstance()
                                    val coupleId = actPrefs.getString("coupleId", null)
                                    if (!coupleId.isNullOrEmpty()) {
                                        val metadataRef = db.collection("pets").document(coupleId).collection("drive_sync_metadata")
                                        val snapshot = Tasks.await(metadataRef.get())
                                        val batch = db.batch()
                                        for (doc in snapshot.documents) {
                                            batch.delete(doc.reference)
                                        }
                                        Tasks.await(batch.commit())
                                        Log.d("ResetDrive", "Metadatos en Firestore eliminados con éxito.")
                                    }

                                    val syncRegistryPrefs = context.getSharedPreferences("DiarioSyncedFiles", android.content.Context.MODE_PRIVATE)
                                    syncRegistryPrefs.edit().clear().apply()

                                    actPrefs.edit()
                                        .remove("syncDriveFolderId")
                                        .putString("syncState", "NO_SINCRONIZADO")
                                        .apply()

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "¡Google Drive y metadatos vaciados!", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ResetDrive", "Error al vaciar nube: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onIncorrectPassword = {
                            Toast.makeText(requireContext(), "Contraseña incorrecta ❌", Toast.LENGTH_SHORT).show()
                        },
                        isScanningDuplicates = isScanningDuplicates,
                        duplicateGroups = duplicateGroups,
                        scanCompleted = scanCompleted,
                        scannedCount = scannedCount,
                        totalToScan = totalToScan,
                        deletedPhotosCount = deletedPhotosCount,
                        spaceFreedBytes = spaceFreedBytes,
                        isDeleting = isDeleting,
                        onScanDuplicates = onScanDuplicates,
                        onDeleteDuplicates = onDeleteDuplicates,
                        onResetDuplicateState = onResetDuplicateState,
                        onTestFirestore = { callback ->
                            val db = FirebaseFirestore.getInstance()
                            val coupleId = prefs?.getString("coupleId", null)
                            if (coupleId.isNullOrEmpty()) {
                                callback("Error: coupleId no configurado")
                            } else {
                                val testDocRef = db.collection("pets").document(coupleId).collection("connection_test").document("test")
                                val testData = mapOf("timestamp" to System.currentTimeMillis())
                                testDocRef.set(testData)
                                    .addOnSuccessListener {
                                        testDocRef.get()
                                            .addOnSuccessListener { doc ->
                                                if (doc.exists()) {
                                                    callback("✓ Firestore: Escritura/Lectura exitosa (${doc.getLong("timestamp")})")
                                                } else {
                                                    callback("Error: Documento de prueba no encontrado")
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                callback("Error de lectura: ${e.message}")
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        callback("Error de escritura: ${e.message}")
                                    }
                            }
                        },
                        onTestGoogleDrive = { callback ->
                            val context = requireContext()
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val account = GoogleSignIn.getLastSignedInAccount(context)
                                    if (account == null) {
                                        withContext(Dispatchers.Main) {
                                            callback("Error: Cuenta de Google no vinculada")
                                        }
                                        return@launch
                                    }
                                    val credential = GoogleAccountCredential.usingOAuth2(
                                        context,
                                        listOf(DriveScopes.DRIVE_FILE)
                                    ).setSelectedAccount(account.account)

                                    val driveService = Drive.Builder(
                                        NetHttpTransport(),
                                        GsonFactory.getDefaultInstance(),
                                        credential
                                    )
                                        .setApplicationName("Diario Kevin Ali")
                                        .build()

                                    val files = driveService.files().list()
                                        .setPageSize(1)
                                        .setFields("files(id, name)")
                                        .execute()
                                        .files

                                    withContext(Dispatchers.Main) {
                                        if (files != null) {
                                            callback("✓ Google Drive: Acceso verificado con éxito")
                                        } else {
                                            callback("✓ Google Drive: Conectado, pero no se encontraron archivos")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("TestDrive", "Drive test failed: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        callback("Error: ${e.message}")
                                    }
                                }
                            }
                        },
                        onRenamePhotosByDate = {
                            selectedFolderUri?.let { uriStr ->
                                renamePhotosByDate(uriStr)
                            } ?: run {
                                Toast.makeText(requireContext(), "Por favor, selecciona primero una carpeta de sincronización", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun showPixelProgressDialog(context: Context, initialText: String): Pair<Dialog, (String) -> Unit> {
        val d = Dialog(context, android.R.style.Theme_Panel)
        
        val isDark = theme == "Pixel Oscuro"
        val isMono = theme == "Pixel Monocromático"
        
        val barBgColor = when {
            isDark -> Color.parseColor("#E60D0D2B")
            isMono -> Color.parseColor("#E6FFFFFF")
            else -> Color.parseColor("#E6FFFDF9")
        }
        val textColor = when {
            isDark -> Color.WHITE
            isMono -> Color.BLACK
            else -> Color.parseColor("#4A2511")
        }
        
        val vt323 = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, R.font.vt323)
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
            
            if (isDark) {
                setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            } else if (isMono) {
                setBackgroundColor(Color.WHITE)
            } else {
                setBackgroundResource(R.drawable.bg_parchment_pixel)
            }
        }
        
        val titleTv = TextView(context).apply {
            text = "Renombrando Fotos"
            typeface = vt323
            textSize = 26f
            setTextColor(textColor)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleTv)
        
        val progressTv = TextView(context).apply {
            text = initialText
            typeface = vt323
            textSize = 20f
            setTextColor(textColor)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(progressTv)
        
        val spinner = ProgressBar(context).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(textColor)
        }
        layout.addView(spinner)
        
        d.setContentView(layout)
        d.setCancelable(false)
        
        val updateFunc = { newText: String ->
            (context as? android.app.Activity)?.runOnUiThread {
                progressTv.text = newText
            } ?: Unit
        }
        
        return Pair(d, updateFunc)
    }

    private fun renamePhotosByDate(treeUriStr: String) {
        val treeUri = Uri.parse(treeUriStr)
        val context = requireContext()
        val contentResolver = context.contentResolver
        
        // Show our beautiful pixel custom progress dialog
        val (progressDialog, updateProgress) = showPixelProgressDialog(context, "Analizando carpeta...")
        progressDialog.show()
        
        lifecycleScope.launch(Dispatchers.IO) {
            var renamedCount = 0
            try {
                val documentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                
                val projection = arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                
                val filesToProcess = mutableListOf<Triple<Uri, String, Long>>()
                val existingNames = mutableSetOf<String>()
                
                contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val docIdIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val lastModifiedIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val mimeTypeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                    
                    while (cursor.moveToNext()) {
                        val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                        val docId = if (docIdIndex != -1) cursor.getString(docIdIndex) else null
                        val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) ?: "" else ""
                        
                        if (!name.isNullOrEmpty() && !docId.isNullOrEmpty()) {
                            existingNames.add(name)
                            if (mimeType.startsWith("image/")) {
                                val lastModified = if (lastModifiedIndex != -1) cursor.getLong(lastModifiedIndex) else 0L
                                val fileUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                filesToProcess.add(Triple(fileUri, name, lastModified))
                            }
                        }
                    }
                }
                
                val totalFiles = filesToProcess.size
                withContext(Dispatchers.Main) {
                    updateProgress("Procesando: 0 / $totalFiles fotos")
                }
                
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val assignedNames = mutableSetOf<String>()
                var currentIndex = 0
                
                for ((fileUri, name, lastModified) in filesToProcess) {
                    currentIndex++
                    withContext(Dispatchers.Main) {
                        updateProgress("Procesando: $currentIndex / $totalFiles fotos")
                    }
                    
                    if (lastModified == 0L) continue
                    val dateStr = sdf.format(java.util.Date(lastModified))
                    val extension = name.substringAfterLast(".", "")
                    val baseName = "IMG_$dateStr"
                    var targetName = if (extension.isNotEmpty()) "$baseName.$extension" else baseName
                    
                    // If the file is already named correctly, keep it
                    if (name == targetName) {
                        assignedNames.add(name)
                        continue
                    }
                    
                    // Resolve collisions
                    var suffix = 1
                    while (existingNames.contains(targetName) || assignedNames.contains(targetName)) {
                        targetName = if (extension.isNotEmpty()) {
                            "${baseName}_$suffix.$extension"
                        } else {
                            "${baseName}_$suffix"
                        }
                        suffix++
                    }
                    
                    // Perform rename
                    try {
                        android.provider.DocumentsContract.renameDocument(contentResolver, fileUri, targetName)
                        existingNames.remove(name)
                        existingNames.add(targetName)
                        assignedNames.add(targetName)
                        renamedCount++
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Error al renombrar $name a $targetName: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error en renamePhotosByDate: ${e.message}")
            }
            
            // Invalidate the local photos cache since the filenames have changed!
            val act = activity as? MainActivity
            act?.getAlbumManager()?.invalidateLocalPhotosCache()
            
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(
                    context,
                    if (renamedCount > 0) "Se renombraron $renamedCount fotos con éxito." else "No fue necesario renombrar ninguna foto.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
