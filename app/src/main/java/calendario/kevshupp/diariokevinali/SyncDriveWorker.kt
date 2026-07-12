package calendario.kevshupp.diariokevinali

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream
import java.security.MessageDigest
import com.google.api.client.http.InputStreamContent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger


class SyncDriveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override val coroutineContext: kotlinx.coroutines.CoroutineDispatcher
        get() = kotlinx.coroutines.Dispatchers.IO

    companion object {
        private const val TAG = "SyncDriveWorker"
        private const val FOLDER_NAME = "DiarioAliKevin_Album"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Iniciando proceso de sincronización con Google Drive...")

        val prefs = applicationContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val localFolderUriStr = prefs.getString("syncLocalFolderUri", null)
        val coupleId = prefs.getString("coupleId", null)
        val userId = prefs.getString("userId", null)
        val syncDirection = prefs.getString("syncDirection", "BIDIRECTIONAL") ?: "BIDIRECTIONAL"

        if (localFolderUriStr.isNullOrEmpty() || coupleId.isNullOrEmpty() || userId.isNullOrEmpty()) {
            Log.e(TAG, "Configuración incompleta: localFolderUri: $localFolderUriStr, coupleId: $coupleId, userId: $userId")
            SyncScheduler.cancelSync(applicationContext)
            return Result.failure()
        }

        // Limpiar registro local si cambió la carpeta de sincronización seleccionada
        val syncRegistryPrefs = applicationContext.getSharedPreferences("DiarioSyncedFiles", Context.MODE_PRIVATE)
        val lastSyncedFolderUri = prefs.getString("lastSyncedFolderUri", "")
        if (lastSyncedFolderUri != localFolderUriStr) {
            syncRegistryPrefs.edit().clear().apply()
            prefs.edit().putString("lastSyncedFolderUri", localFolderUriStr).apply()
            Log.d(TAG, "Carpeta local cambiada de '$lastSyncedFolderUri' a '$localFolderUriStr'. Registro de sincronización limpiado.")
        }

        // 1. Obtener la cuenta de Google vinculada
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
            Log.e(TAG, "No hay cuenta de Google vinculada para la sincronización.")
            SyncScheduler.cancelSync(applicationContext)
            return Result.failure()
        }

        val maxRetries = prefs.getInt("syncMaxRetries", 3)

        // Mostrar notificación e iniciar progreso sólo si está configurado correctamente
        showSyncNotification(-1, "Iniciando sincronización...", false)
        setProgress(workDataOf("progress" to -1, "status" to "Iniciando sincronización..."))
        prefs.edit()
            .putString("syncState", "SINCRONIZANDO")
            .remove("syncLastError")
            .apply()

        try {
            try {
                setForeground(getForegroundInfo())
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo promover el trabajador a primer plano (Foreground): ${e.message}")
            }

            // 2. Construir el cliente de Google Drive
            showSyncNotification(-1, "Conectando con Google Drive...", false)
            setProgress(workDataOf("progress" to -1, "status" to "Conectando con Google Drive..."))
            
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE)
            ).setSelectedAccount(account.account)

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Diario Kevin Ali")
                .build()

            // 3. Buscar o crear la carpeta compartida en Drive
            var folderId = prefs.getString("syncDriveFolderId", null)
            if (folderId.isNullOrEmpty()) {
                showSyncNotification(-1, "Verificando carpeta en Drive...", false)
                setProgress(workDataOf("progress" to -1, "status" to "Verificando carpeta en Drive..."))
                folderId = runWithRetry(maxRetries) {
                    getOrCreateDriveFolder(driveService) ?: throw java.io.IOException("No se pudo obtener o crear la carpeta de Google Drive.")
                }
                prefs.edit().putString("syncDriveFolderId", folderId).apply()
            }

            // 4. Cargar la carpeta local SAF y verificar si persisten los permisos
            val localFolderUri = Uri.parse(localFolderUriStr)
            val hasPermission = applicationContext.contentResolver.persistedUriPermissions.any { 
                it.uri == localFolderUri && it.isReadPermission 
            }
            val localFolder = DocumentFile.fromTreeUri(applicationContext, localFolderUri)
            if (!hasPermission || localFolder == null || !localFolder.exists() || !localFolder.isDirectory) {
                Log.e(TAG, "La carpeta local seleccionada no existe o perdió los permisos (común al reinstalar): $localFolderUri")
                
                // Limpiar la configuración dañada para evitar bucles de error estresantes
                prefs.edit()
                    .remove("syncLocalFolderUri")
                    .remove("syncIntervalMinutes")
                    .apply()
                SyncScheduler.cancelSync(applicationContext)
                
                showSyncNotification(100, "Permisos de carpeta revocados. Reconfígurala en Ajustes.", true, false)
                setProgress(workDataOf("progress" to -1, "status" to "Permisos de carpeta revocados"))
                return Result.failure()
            }

            // 5. Cargar metadatos desde Firestore y listar locales EN PARALELO (Omitiendo listado de Drive por velocidad)
            showSyncNotification(-1, "Consultando archivos locales y en la nube...", false)
            setProgress(workDataOf("progress" to -1, "status" to "Consultando archivos locales y en la nube..."))
            val db = FirebaseFirestore.getInstance()

            val (metadataSnapshot, localFiles) = coroutineScope {
                val firestoreDeferred = async {
                    runWithRetry(maxRetries) {
                        Tasks.await(
                            db.collection("pets").document(coupleId).collection("drive_sync_metadata").get()
                        )
                    }
                }
                val localFilesDeferred = async {
                    listFilesFromTreeUri(applicationContext, localFolderUri)
                }
                Pair(firestoreDeferred.await(), localFilesDeferred.await())
            }

            val dbMetadataList = metadataSnapshot.documents.mapNotNull { doc ->
                val meta = doc.toObject(SyncMetadata::class.java)
                if (meta != null) doc.id to meta else null
            }.toMap()

            val updatedDbMetadataMap = dbMetadataList.toMutableMap()
            val localFilesMap = localFiles.associateBy { it.name }

            // 5a. Poblar/actualizar el registro local de sincronización con archivos que ya existen localmente y en la nube
            val registryEditor = syncRegistryPrefs.edit()
            var registryChanged = false
            for (fileName in localFilesMap.keys) {
                val dbMeta = dbMetadataList[fileName]
                if (dbMeta != null && !dbMeta.eliminado) {
                    if (!syncRegistryPrefs.contains(fileName)) {
                        registryEditor.putBoolean(fileName, true)
                        registryChanged = true
                    }
                }
            }
            if (registryChanged) {
                registryEditor.apply()
                Log.d(TAG, "Registro local de sincronización actualizado con archivos existentes.")
            }

            // 5b. Detectar borrados locales y renombrados, y replicarlos en la nube (Drive + Firestore)
            // Sólo consideramos borrado local si no existe localmente Y ya había sido sincronizado previamente en este dispositivo
            // Omitimos si la dirección es SOLO_BAJADA
            val deletedLocally = mutableMapOf<String, SyncMetadata>()
            if (syncDirection != "DOWNLOAD_ONLY") {
                // Identificamos archivos locales "nuevos" (que no coinciden por nombre con la BD)
                val newLocalFiles = localFiles.filter { !updatedDbMetadataMap.containsKey(it.name) }
                val newLocalFilesWithMd5 = coroutineScope {
                    newLocalFiles.map { photo ->
                        async {
                            photo to (calculateMd5(photo.uri) ?: "")
                        }
                    }.awaitAll().filter { it.second.isNotEmpty() }
                }

                val renames = mutableListOf<Pair<SyncMetadata, LocalFileMeta>>()

                for ((fileName, meta) in dbMetadataList) {
                    if (meta.eliminado) continue
                    if (!localFilesMap.containsKey(fileName) && syncRegistryPrefs.getBoolean(fileName, false)) {
                        // Buscar coincidencia de MD5 con algún archivo local nuevo
                        val renameMatch = newLocalFilesWithMd5.find { it.second == meta.md5Checksum }
                        if (renameMatch != null) {
                            renames.add(meta to renameMatch.first)
                        } else {
                            deletedLocally[fileName] = meta
                        }
                    }
                }

                // Procesar los archivos renombrados
                if (renames.isNotEmpty()) {
                    Log.d(TAG, "Detectados ${renames.size} archivos renombrados localmente. Sincronizando nombres en Drive y Firestore...")
                    coroutineScope {
                        val renameJobs = renames.map { (meta, localFile) ->
                            async {
                                runWithRetry(maxRetries) {
                                    try {
                                        // 1. Renombrar en Google Drive
                                        val fileMetadata = com.google.api.services.drive.model.File().apply {
                                            name = localFile.name
                                        }
                                        driveService.files().update(meta.idDrive, fileMetadata).execute()
                                        Log.d(TAG, "Renombrado en Google Drive: ${meta.nombreArchivo} -> ${localFile.name}")
                                        
                                        // 2. Crear nuevo metadato en Firestore
                                        val newMeta = meta.copy(
                                            idLocal = localFile.name,
                                            nombreArchivo = localFile.name,
                                            uriLocal = localFile.uri.toString(),
                                            fechaModificacion = localFile.lastModified
                                        )
                                        Tasks.await(
                                            db.collection("pets").document(coupleId)
                                                .collection("drive_sync_metadata").document(localFile.name)
                                                .set(newMeta)
                                        )
                                        
                                        // 3. Eliminar metadato antiguo en Firestore
                                        Tasks.await(
                                            db.collection("pets").document(coupleId)
                                                .collection("drive_sync_metadata").document(meta.nombreArchivo)
                                                .delete()
                                        )
                                        
                                        // 4. Actualizar registro local de sincronización
                                        syncRegistryPrefs.edit()
                                            .remove(meta.nombreArchivo)
                                            .putBoolean(localFile.name, true)
                                            .apply()
                                        
                                        // 5. Actualizar mapa de metadatos en memoria
                                        synchronized(updatedDbMetadataMap) {
                                            updatedDbMetadataMap.remove(meta.nombreArchivo)
                                            updatedDbMetadataMap[localFile.name] = newMeta
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error al procesar renombrado de ${meta.nombreArchivo} a ${localFile.name}: ${e.message}", e)
                                    }
                                }
                            }
                        }
                        renameJobs.awaitAll()
                    }
                }
            }

            if (deletedLocally.isNotEmpty()) {
                Log.d(TAG, "Detectados ${deletedLocally.size} archivos borrados localmente. Procesando eliminación...")
                coroutineScope {
                    val jobs = deletedLocally.map { (fileName, meta) ->
                        async {
                            runWithRetry(maxRetries) {
                                try {
                                    driveService.files().delete(meta.idDrive).execute()
                                    Log.d(TAG, "Borrado de Google Drive: $fileName")
                                } catch (e: Exception) {
                                    Log.w(TAG, "No se pudo borrar de Drive: $fileName", e)
                                }
                                val updatedMeta = meta.copy(eliminado = true)
                                Tasks.await(
                                    db.collection("pets").document(coupleId)
                                        .collection("drive_sync_metadata").document(fileName)
                                        .set(updatedMeta)
                                )
                                syncRegistryPrefs.edit().remove(fileName).apply()
                                synchronized(updatedDbMetadataMap) {
                                    updatedDbMetadataMap[fileName] = updatedMeta
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }
            }

            // 5c. Detectar borrados remotos y replicarlos localmente (Borrar del dispositivo)
            // Omitimos si la dirección es SOLO_SUBIDA
            val localFilesToDelete = if (syncDirection == "UPLOAD_ONLY") {
                emptyList<LocalFileMeta>()
            } else {
                localFiles.filter { localFile ->
                    val meta = updatedDbMetadataMap[localFile.name]
                    meta != null && meta.eliminado
                }
            }
            if (localFilesToDelete.isNotEmpty()) {
                Log.d(TAG, "Detectados ${localFilesToDelete.size} archivos eliminados remotamente. Borrando localmente...")
                localFilesToDelete.forEach { localFile ->
                    try {
                        val singleFile = DocumentFile.fromSingleUri(applicationContext, localFile.uri)
                        singleFile?.delete()
                        syncRegistryPrefs.edit().remove(localFile.name).apply()
                        Log.d(TAG, "Borrado local: ${localFile.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al borrar localmente ${localFile.name}: ${e.message}")
                    }
                }
            }

            // Re-filtrar los archivos locales después del borrado remoto (en memoria, sin llamar al sistema de archivos)
            val finalLocalFiles = localFiles.filter { localFile ->
                val isDeleted = localFilesToDelete.any { it.name == localFile.name }
                !isDeleted && (updatedDbMetadataMap[localFile.name]?.eliminado != true)
            }
            val finalLocalFilesMap = finalLocalFiles.associateBy { it.name }

            // Recalcular archivos totales (excluyendo borrados)
            val activeCloudFilesCount = updatedDbMetadataMap.values.count { !it.eliminado }
            val totalFiles = finalLocalFiles.size + activeCloudFilesCount
            val processedCount = AtomicInteger(0)

            // Configurar hilos paralelos y slots
            val parallelLines = prefs.getInt("syncParallelLines", 3)
            val slots = Array(parallelLines) { "" }
            val slotProgress = IntArray(parallelLines) { 0 }

            suspend fun updateSlot(slotIndex: Int, fileName: String, progress: Int, isGeneralOnly: Boolean = false) {
                synchronized(slots) {
                    if (!isGeneralOnly && slotIndex in 0 until parallelLines) {
                        slots[slotIndex] = fileName
                        slotProgress[slotIndex] = progress
                    }
                }
                
                val currentProcessed = processedCount.get()
                val pct = if (totalFiles > 0) (currentProcessed * 100) / totalFiles else 0
                val dataBuilder = androidx.work.Data.Builder()
                    .putInt("progress", pct)
                
                var generalStatus = ""
                synchronized(slots) {
                    val activeSlots = mutableListOf<String>()
                    for (i in 0 until parallelLines) {
                        dataBuilder.putString("slot_${i}_name", slots[i])
                        dataBuilder.putInt("slot_${i}_progress", slotProgress[i])
                        if (slots[i].isNotEmpty()) {
                            activeSlots.add(slots[i])
                        }
                    }
                    generalStatus = if (activeSlots.isNotEmpty()) {
                        if (activeSlots.size == 1) "Sincronizando: ${activeSlots[0]}" else "Sincronizando ${activeSlots.size} archivos..."
                    } else {
                        "Sincronizando..."
                    }
                }
                dataBuilder.putString("status", generalStatus)
                
                try {
                    setProgress(dataBuilder.build())
                } catch (e: Exception) {
                    Log.w(TAG, "Error al actualizar progreso: ${e.message}")
                }
            }

            // 6. Determinar qué archivos locales necesitan subirse (en paralelo)
            // Omitimos si la dirección es SOLO_BAJADA
            val filesToUpload = if (syncDirection == "DOWNLOAD_ONLY") {
                emptyList()
            } else {
                coroutineScope {
                    finalLocalFiles.map { localFile ->
                        async {
                            val fileName = localFile.name
                            val dbMeta = updatedDbMetadataMap[fileName]
                            val localLastModified = localFile.lastModified
                            
                            var localMd5 = ""
                            var needUpload = false
                            if (dbMeta != null && dbMeta.eliminado) {
                                needUpload = false
                            } else if (dbMeta == null) {
                                needUpload = true
                            } else {
                                if (localLastModified > dbMeta.fechaModificacion) {
                                    localMd5 = if (dbMeta.fechaModificacion == localLastModified) {
                                        dbMeta.md5Checksum
                                    } else {
                                        calculateMd5(localFile.uri) ?: ""
                                    }
                                    if (dbMeta.md5Checksum != localMd5) {
                                        needUpload = true
                                    }
                                }
                            }

                            if (needUpload && localMd5.isEmpty()) {
                                localMd5 = calculateMd5(localFile.uri) ?: ""
                            }

                            if (needUpload) {
                                Triple(localFile, localMd5, dbMeta)
                            } else {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            // 7. Procesar archivos locales en paralelo (según líneas configuradas)
            coroutineScope {
                val uploadSemaphore = Semaphore(parallelLines)
                val uploadJobs = filesToUpload.map { (localFile, localMd5, dbMeta) ->
                    async {
                        uploadSemaphore.withPermit {
                            val fileName = localFile.name
                            Log.d(TAG, "Subiendo en paralelo: $fileName")
                            
                            val slotIndex = synchronized(slots) {
                                val idx = slots.indexOf("")
                                if (idx != -1) {
                                    slots[idx] = fileName
                                    slotProgress[idx] = 0
                                    idx
                                } else {
                                    0
                                }
                            }
                            updateSlot(slotIndex, fileName, 10)
                            
                            val mime = localFile.mimeType
                            val driveFileId = runWithRetry(maxRetries) {
                                uploadToDrive(driveService, folderId, localFile.uri, fileName, mime) ?: throw java.io.IOException("No se pudo subir la foto a Drive")
                            }
                            updateSlot(slotIndex, fileName, 50)
                            
                            val newMeta = SyncMetadata(
                                idLocal = fileName,
                                idDrive = driveFileId,
                                nombreArchivo = fileName,
                                uriLocal = localFile.uri.toString(),
                                md5Checksum = localMd5,
                                fechaModificacion = localFile.lastModified,
                                sincronizadoPor = userId,
                                eliminado = false
                            )
                            runWithRetry(maxRetries) {
                                Tasks.await(
                                    db.collection("pets").document(coupleId)
                                        .collection("drive_sync_metadata").document(fileName)
                                        .set(newMeta)
                                )
                            }
                            syncRegistryPrefs.edit().putBoolean(fileName, true).apply()
                            updateSlot(slotIndex, fileName, 100)
                            
                            synchronized(slots) {
                                slots[slotIndex] = ""
                                slotProgress[slotIndex] = 0
                            }
                            
                            processedCount.incrementAndGet()
                            updateSlot(0, "", -1, isGeneralOnly = true)
                        }
                    }
                }
                
                // Los archivos locales que no requirieron subirse se consideran procesados inmediatamente
                val localNotUploadedCount = finalLocalFiles.size - filesToUpload.size
                processedCount.addAndGet(localNotUploadedCount)
                updateSlot(0, "", -1, isGeneralOnly = true)
                
                uploadJobs.awaitAll()
            }

            // 8. Determinar qué archivos de Drive necesitan descargarse recorriendo la lista de Firestore
            // Omitimos si la dirección es SOLO_SUBIDA
            val filesToDownload = mutableListOf<Triple<String, String, SyncMetadata>>()
            if (syncDirection != "UPLOAD_ONLY") {
                for (dbMeta in updatedDbMetadataMap.values) {
                    if (dbMeta.eliminado) continue
                    val fileName = dbMeta.nombreArchivo
                    val existsLocally = finalLocalFilesMap.containsKey(fileName)

                    var needDownload = false
                    if (!existsLocally) {
                        needDownload = true
                    } else {
                        val localFile = finalLocalFilesMap[fileName]!!
                        val localLastModified = localFile.lastModified
                        // Si la fecha en la nube es posterior a la local, calculamos MD5 para verificar si cambió
                        if (dbMeta.fechaModificacion > localLastModified) {
                            val localMd5 = calculateMd5(localFile.uri) ?: ""
                            if (dbMeta.md5Checksum != localMd5) {
                                needDownload = true
                            }
                        }
                    }

                    if (needDownload) {
                        filesToDownload.add(Triple(dbMeta.idDrive, fileName, dbMeta))
                    }
                }
            }

            // Descargar archivos en paralelo (según líneas configuradas)
            coroutineScope {
                val downloadSemaphore = Semaphore(parallelLines)
                val downloadJobs = filesToDownload.map { (driveFileId, fileName, dbMeta) ->
                    async {
                        downloadSemaphore.withPermit {
                            Log.d(TAG, "Descargando en paralelo: $fileName")
                            
                            val slotIndex = synchronized(slots) {
                                val idx = slots.indexOf("")
                                if (idx != -1) {
                                    slots[idx] = fileName
                                    slotProgress[idx] = 0
                                    idx
                                } else {
                                    0
                                }
                            }
                            updateSlot(slotIndex, fileName, 10)
                            
                            val ext = fileName.substringAfterLast('.', "").lowercase()
                            val mimeType = when (ext) {
                                "png" -> "image/png"
                                "gif" -> "image/gif"
                                "webp" -> "image/webp"
                                else -> "image/jpeg"
                            }

                            val downloadedUri = runWithRetry(maxRetries) {
                                downloadFromDrive(driveService, driveFileId, localFolder, fileName, mimeType) ?: throw java.io.IOException("No se pudo descargar la foto de Drive")
                            }
                            updateSlot(slotIndex, fileName, 50)
                            
                            val downloadedFile = localFolder.findFile(fileName)
                            val finalMeta = SyncMetadata(
                                idLocal = fileName,
                                idDrive = driveFileId,
                                nombreArchivo = fileName,
                                uriLocal = downloadedUri.toString(),
                                md5Checksum = dbMeta.md5Checksum,
                                fechaModificacion = downloadedFile?.lastModified() ?: dbMeta.fechaModificacion,
                                sincronizadoPor = userId,
                                eliminado = false
                            )
                            runWithRetry(maxRetries) {
                                Tasks.await(
                                     db.collection("pets").document(coupleId)
                                         .collection("drive_sync_metadata").document(fileName)
                                         .set(finalMeta)
                                 )
                             }
                             syncRegistryPrefs.edit().putBoolean(fileName, true).apply()
                             updateSlot(slotIndex, fileName, 100)
                             
                             synchronized(slots) {
                                 slots[slotIndex] = ""
                                 slotProgress[slotIndex] = 0
                             }
                             
                             processedCount.incrementAndGet()
                             updateSlot(0, "", -1, isGeneralOnly = true)
                         }
                     }
                 }
                
                 // Los archivos en la nube que no requirieron descargarse se consideran procesados inmediatamente
                 val activeCloudFilesCount = updatedDbMetadataMap.values.count { !it.eliminado }
                 val cloudNotDownloadedCount = activeCloudFilesCount - filesToDownload.size
                 processedCount.addAndGet(cloudNotDownloadedCount)
                 updateSlot(0, "", -1, isGeneralOnly = true)
                 
                 downloadJobs.awaitAll()
             }

            Log.d(TAG, "Sincronización con Google Drive completada exitosamente.")
            prefs.edit()
                .putString("syncState", "SINCRONIZADO")
                .remove("syncLastError")
                .apply()
            showSyncNotification(100, "Sincronización completada exitosamente", true, true)
            setProgress(workDataOf("progress" to 100, "status" to "Completada"))
            return Result.success()

        } catch (e: kotlinx.coroutines.CancellationException) {
            val prefs = applicationContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("syncState", "NO_SINCRONIZADO").apply()
            val cancelledByUser = prefs.getBoolean("syncCancelledByUser", false)
            Log.i(TAG, "Sincronización cancelada. ¿Por el usuario?: $cancelledByUser")
            
            if (cancelledByUser) {
                prefs.edit().putBoolean("syncCancelledByUser", false).apply()
                showSyncNotification(100, "Sincronización detenida", true, false)
            } else {
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)
            }
            throw e
        } catch (e: java.lang.Exception) {
            val errorMsg = e.message ?: e.toString()
            Log.e(TAG, "Error en la sincronización con Google Drive: $errorMsg", e)
            
            val prefs = applicationContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            if (errorMsg.contains("404") || errorMsg.contains("File not found") || errorMsg.contains("notFound")) {
                prefs.edit().remove("syncDriveFolderId").apply()
            }
            prefs.edit()
                .putString("syncState", "NO_SINCRONIZADO")
                .putString("syncLastError", errorMsg)
                .apply()

            showSyncNotification(100, errorMsg, true, false)
            setProgress(workDataOf("progress" to -1, "status" to "Error: $errorMsg"))
            return Result.failure()
        }
    }

    private fun showSyncNotification(progress: Int, status: String, isFinished: Boolean, isSuccess: Boolean = true) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sincronización de fotos", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 1001

        if (isFinished) {
            val isCancelled = status == "Sincronización detenida"
            val title = when {
                isCancelled -> "Sincronización detenida"
                isSuccess -> "Sincronización completada"
                else -> "Sincronización fallida ⚠️"
            }
            val text = when {
                isCancelled -> "El proceso de sincronización fue cancelado por el usuario."
                isSuccess -> "Las fotos están sincronizadas con Google Drive."
                else -> {
                    val preview = if (status.length > 50) status.substring(0, 47) + "..." else status
                    "Toca para ver: $preview"
                }
            }
            val icon = R.drawable.ic_settings_pixel
            
            val intent = android.content.Intent(applicationContext, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!isSuccess && !isCancelled) {
                    putExtra("sync_error_msg", status)
                }
            }
            
            val requestCode = if (isSuccess) 2001 else 2002
            val pendingIntent = android.app.PendingIntent.getActivity(
                applicationContext,
                requestCode,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
            
            notificationManager.notify(notificationId, builder.build())
        } else {
            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_settings_pixel)
                .setContentTitle("Sincronizando fotos...")
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

            if (progress in 0..100) {
                builder.setProgress(100, progress, false)
            } else {
                builder.setProgress(0, 0, true) // Indeterminate
            }

            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun getOrCreateDriveFolder(driveService: Drive): String? {
        // Buscar si ya existe la carpeta
        val listQuery = driveService.files().list()
            .setQ("name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setFields("files(id)")
            .execute()
        
        val files = listQuery.files
        if (files != null && files.isNotEmpty()) {
            return files[0].id
        }

        // Crear la carpeta
        val folderMetadata = File().apply {
            name = FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        return folder.id
    }

    private fun uploadToDrive(driveService: Drive, folderId: String, fileUri: Uri, fileName: String, mimeType: String): String? {
        val fileMetadata = File().apply {
            name = fileName
            parents = listOf(folderId)
        }

        val inputStream = applicationContext.contentResolver.openInputStream(fileUri) ?: return null
        return inputStream.use { stream ->
            val mediaContent = InputStreamContent(mimeType, stream)

            val createRequest = driveService.files().create(fileMetadata, mediaContent)
            // Habilitar subida directa (media upload) para mejorar drásticamente la velocidad en fotos
            createRequest.mediaHttpUploader.isDirectUploadEnabled = true

            val driveFile = createRequest
                .setFields("id")
                .execute()

            driveFile.id
        }
    }

    private fun downloadFromDrive(
        driveService: Drive,
        fileId: String,
        localFolder: DocumentFile,
        fileName: String,
        mimeType: String
    ): Uri? {
        // Si el archivo ya existe localmente, lo borramos para evitar duplicados/conflictos al crear
        val existing = localFolder.findFile(fileName)
        existing?.delete()

        val newFile = localFolder.createFile(mimeType, fileName) ?: return null
        val outputStream = applicationContext.contentResolver.openOutputStream(newFile.uri) ?: return null

        outputStream.use { out ->
            driveService.files().get(fileId).executeMediaAndDownloadTo(out)
        }

        return newFile.uri
    }

    private fun calculateMd5(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(32768)
                var read: Int
                while (inputStream.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            } ?: return null
            val md5sum = digest.digest()
            val bigInt = java.math.BigInteger(1, md5sum)
            bigInt.toString(16).padStart(32, '0')
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular MD5 para $uri: ${e.message}")
            null
        }
    }

    private suspend fun <T> runWithRetry(maxRetries: Int, block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                attempt++
                if (attempt > maxRetries) {
                    throw e
                }
                Log.w(TAG, "Operación falló (intento $attempt/$maxRetries). Reintentando en ${attempt * 1000}ms...", e)
                kotlinx.coroutines.delay(1000L * attempt)
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(-1, "Iniciando sincronización...")
    }

    private fun createForegroundInfo(progress: Int, status: String): ForegroundInfo {
        val channelId = "sync_channel"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Sincronización de fotos", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(applicationContext, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            2001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_settings_pixel)
            .setContentTitle("Sincronizando fotos...")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, builder.build())
        }
    }

    data class LocalFileMeta(
        val name: String,
        val uri: Uri,
        val lastModified: Long,
        val size: Long,
        val mimeType: String
    )

    private fun listFilesFromTreeUri(context: Context, treeUri: Uri): List<LocalFileMeta> {
        val fileList = mutableListOf<LocalFileMeta>()
        val contentResolver = context.contentResolver
        return try {
            val documentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                android.provider.DocumentsContract.Document.COLUMN_SIZE,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val docIdIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val lastModifiedIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_SIZE)
                val mimeTypeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                
                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    val docId = if (docIdIndex != -1) cursor.getString(docIdIndex) else null
                    val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) ?: "" else ""
                    
                    if (!name.isNullOrEmpty() && !docId.isNullOrEmpty() && mimeType.startsWith("image/")) {
                        val lastModified = if (lastModifiedIndex != -1) cursor.getLong(lastModifiedIndex) else 0L
                        val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        val fileUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        fileList.add(LocalFileMeta(name, fileUri, lastModified, size, mimeType))
                    }
                }
            }
            fileList
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files from tree URI using cursor: ${e.message}", e)
            emptyList()
        }
    }
}

// Modelo de datos para Firestore
data class SyncMetadata(
    var idLocal: String = "",
    var idDrive: String = "",
    var nombreArchivo: String = "",
    var uriLocal: String = "",
    var md5Checksum: String = "",
    var fechaModificacion: Long = 0L,
    var sincronizadoPor: String = "",
    var eliminado: Boolean = false
)
