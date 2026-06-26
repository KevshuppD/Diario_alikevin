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

        if (localFolderUriStr.isNullOrEmpty() || coupleId.isNullOrEmpty() || userId.isNullOrEmpty()) {
            Log.e(TAG, "Configuración incompleta: localFolderUri: $localFolderUriStr, coupleId: $coupleId, userId: $userId")
            SyncScheduler.cancelSync(applicationContext)
            return Result.failure()
        }

        // 1. Obtener la cuenta de Google vinculada
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
            Log.e(TAG, "No hay cuenta de Google vinculada para la sincronización.")
            SyncScheduler.cancelSync(applicationContext)
            return Result.failure()
        }

        // Mostrar notificación e iniciar progreso sólo si está configurado correctamente
        showSyncNotification(-1, "Iniciando sincronización...", false)
        setProgress(workDataOf("progress" to -1, "status" to "Iniciando sincronización..."))

        try {
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
            showSyncNotification(-1, "Verificando carpeta en Drive...", false)
            setProgress(workDataOf("progress" to -1, "status" to "Verificando carpeta en Drive..."))
            val folderId = getOrCreateDriveFolder(driveService)
            if (folderId.isNullOrEmpty()) {
                Log.e(TAG, "No se pudo obtener o crear la carpeta de Google Drive.")
                showSyncNotification(100, "Error en carpeta de Drive", true, false)
                return Result.failure()
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

            // 5. Cargar metadatos desde Firestore
            showSyncNotification(-1, "Cargando metadatos de sincronización...", false)
            setProgress(workDataOf("progress" to -1, "status" to "Cargando metadatos de sincronización..."))
            val db = FirebaseFirestore.getInstance()
            val metadataSnapshot = Tasks.await(
                db.collection("pets").document(coupleId).collection("drive_sync_metadata").get()
            )
            
            val dbMetadataList = metadataSnapshot.documents.mapNotNull { doc ->
                val meta = doc.toObject(SyncMetadata::class.java)
                if (meta != null) doc.id to meta else null
            }.toMap()

            // 6. Obtener archivos de Google Drive
            showSyncNotification(-1, "Consultando archivos en la nube...", false)
            setProgress(workDataOf("progress" to -1, "status" to "Consultando archivos en la nube..."))
            val driveFilesList = driveService.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setFields("files(id, name, mimeType, md5Checksum, modifiedTime)")
                .execute()
            val driveFiles = driveFilesList.files ?: emptyList()
            val driveFilesMap = driveFiles.associateBy { it.name }

            val localFiles = localFolder.listFiles().filter { localFile ->
                !localFile.isDirectory && localFile.name != null && (localFile.type ?: "").startsWith("image/")
            }
            
            val localFilesNames = localFiles.mapNotNull { it.name }.toSet()
            val totalFiles = localFiles.size + driveFiles.filter { df -> (df.mimeType ?: "").startsWith("image/") }.size
            val processedCount = AtomicInteger(0)

            // 1. Determinar qué archivos locales necesitan subirse
            val filesToUpload = mutableListOf<Triple<DocumentFile, String, SyncMetadata?>>()
            for (localFile in localFiles) {
                val fileName = localFile.name!!
                val dbMeta = dbMetadataList[fileName]
                val driveFile = driveFilesMap[fileName]
                val localLastModified = localFile.lastModified()
                
                val localMd5 = if (dbMeta != null && dbMeta.fechaModificacion == localLastModified) {
                    dbMeta.md5Checksum
                } else {
                    calculateMd5(localFile.uri) ?: ""
                }

                var needUpload = false
                if (driveFile == null) {
                    needUpload = true
                } else if (dbMeta == null || dbMeta.md5Checksum != localMd5) {
                    if (driveFile.md5Checksum != localMd5) {
                        val driveModifiedTime = driveFile.modifiedTime?.value ?: 0L
                        if (localLastModified > driveModifiedTime) {
                            needUpload = true
                        }
                    }
                }

                if (needUpload) {
                    filesToUpload.add(Triple(localFile, localMd5, dbMeta))
                }
            }

            // 7. Procesar archivos locales en paralelo (máximo 3 concurrentes)
            coroutineScope {
                val uploadSemaphore = Semaphore(3)
                val uploadJobs = filesToUpload.map { (localFile, localMd5, dbMeta) ->
                    async {
                        uploadSemaphore.withPermit {
                            val fileName = localFile.name!!
                            Log.d(TAG, "Subiendo en paralelo: $fileName")
                            
                            val mime = localFile.type ?: "image/jpeg"
                            val driveFileId = uploadToDrive(driveService, folderId, localFile, mime)
                            if (driveFileId != null) {
                                val newMeta = SyncMetadata(
                                    idLocal = fileName,
                                    idDrive = driveFileId,
                                    nombreArchivo = fileName,
                                    uriLocal = localFile.uri.toString(),
                                    md5Checksum = localMd5,
                                    fechaModificacion = localFile.lastModified(),
                                    sincronizadoPor = userId
                                )
                                Tasks.await(
                                    db.collection("pets").document(coupleId)
                                        .collection("drive_sync_metadata").document(fileName)
                                        .set(newMeta)
                                )
                            }
                            
                            val currentProcessed = processedCount.incrementAndGet()
                            val pct = if (totalFiles > 0) (currentProcessed * 100) / totalFiles else 0
                            showSyncNotification(pct, "Subiendo: $fileName", false)
                            setProgress(workDataOf("progress" to pct, "status" to "Subiendo: $fileName"))
                        }
                    }
                }
                
                // Los archivos locales que no requirieron subirse se consideran procesados inmediatamente
                val localNotUploadedCount = localFiles.size - filesToUpload.size
                processedCount.addAndGet(localNotUploadedCount)
                
                uploadJobs.awaitAll()
            }

            // 8. Procesar archivos de Drive -> Descargar nuevos localmente
            val updatedMetadataSnapshot = Tasks.await(
                db.collection("pets").document(coupleId).collection("drive_sync_metadata").get()
            )
            val updatedDbMetadataList = updatedMetadataSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SyncMetadata::class.java)
            }
            val updatedDbMetadataMap = updatedDbMetadataList.associateBy { it.nombreArchivo }

            // Determinar qué archivos de Drive necesitan descargarse
            val filesToDownload = mutableListOf<Triple<com.google.api.services.drive.model.File, String, SyncMetadata?>>()
            for (df in driveFiles) {
                val fileName = df.name ?: continue
                val isImage = df.mimeType?.startsWith("image/") == true
                if (!isImage) continue

                val existsLocally = localFilesNames.contains(fileName)
                val dbMeta = updatedDbMetadataMap[fileName]

                var needDownload = false
                if (!existsLocally) {
                    needDownload = true
                } else if (dbMeta == null || dbMeta.md5Checksum != df.md5Checksum) {
                    val localFile = localFiles.find { it.name == fileName }
                    val localLastModified = localFile?.lastModified() ?: 0L
                    val driveModifiedTime = df.modifiedTime?.value ?: 0L
                    if (driveModifiedTime > localLastModified) {
                        needDownload = true
                    }
                }

                if (needDownload) {
                    filesToDownload.add(Triple(df, fileName, dbMeta))
                }
            }

            // Descargar archivos en paralelo (máximo 3 concurrentes)
            coroutineScope {
                val downloadSemaphore = Semaphore(3)
                val downloadJobs = filesToDownload.map { (df, fileName, dbMeta) ->
                    async {
                        downloadSemaphore.withPermit {
                            Log.d(TAG, "Descargando en paralelo: $fileName")
                            
                            val downloadedUri = downloadFromDrive(driveService, df.id, localFolder, fileName, df.mimeType)
                            if (downloadedUri != null) {
                                val downloadedFile = localFolder.findFile(fileName)
                                val newMd5 = df.md5Checksum ?: calculateMd5(downloadedUri) ?: ""
                                val finalMeta = SyncMetadata(
                                    idLocal = fileName,
                                    idDrive = df.id,
                                    nombreArchivo = fileName,
                                    uriLocal = downloadedUri.toString(),
                                    md5Checksum = newMd5,
                                    fechaModificacion = downloadedFile?.lastModified() ?: df.modifiedTime?.value ?: System.currentTimeMillis(),
                                    sincronizadoPor = userId
                                )
                                Tasks.await(
                                    db.collection("pets").document(coupleId)
                                        .collection("drive_sync_metadata").document(fileName)
                                        .set(finalMeta)
                                )
                            }
                            
                            val currentProcessed = processedCount.incrementAndGet()
                            val pct = if (totalFiles > 0) (currentProcessed * 100) / totalFiles else 0
                            showSyncNotification(pct, "Descargando: $fileName", false)
                            setProgress(workDataOf("progress" to pct, "status" to "Descargando: $fileName"))
                        }
                    }
                }
                
                // Los archivos en la nube que no requirieron descargarse se consideran procesados inmediatamente
                val cloudFilesCount = driveFiles.filter { df -> (df.mimeType ?: "").startsWith("image/") }.size
                val cloudNotDownloadedCount = cloudFilesCount - filesToDownload.size
                processedCount.addAndGet(cloudNotDownloadedCount)
                
                downloadJobs.awaitAll()
            }

            Log.d(TAG, "Sincronización con Google Drive completada exitosamente.")
            showSyncNotification(100, "Sincronización completada exitosamente", true, true)
            setProgress(workDataOf("progress" to 100, "status" to "Completada"))
            return Result.success()

        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Sincronización cancelada por el usuario o sistema.")
            showSyncNotification(100, "Sincronización detenida", true, false)
            throw e
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error en la sincronización con Google Drive: ${e.message}", e)
            showSyncNotification(100, "Error en la sincronización", true, false)
            setProgress(workDataOf("progress" to -1, "status" to "Error: ${e.message}"))
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
            val title = if (isSuccess) "Sincronización completada" else "Sincronización fallida"
            val text = if (isSuccess) "Las fotos están sincronizadas con Google Drive." else "Ocurrió un error al sincronizar."
            val icon = R.drawable.ic_settings_pixel
            
            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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

    private fun uploadToDrive(driveService: Drive, folderId: String, file: DocumentFile, mimeType: String): String? {
        val fileMetadata = File().apply {
            name = file.name
            parents = listOf(folderId)
        }

        val inputStream = applicationContext.contentResolver.openInputStream(file.uri) ?: return null
        val mediaContent = InputStreamContent(mimeType, inputStream)

        val createRequest = driveService.files().create(fileMetadata, mediaContent)
        // Habilitar subida directa (media upload) para mejorar drásticamente la velocidad en fotos
        createRequest.mediaHttpUploader.isDirectUploadEnabled = true

        val driveFile = createRequest
            .setFields("id")
            .execute()

        return driveFile.id
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
            val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum = digest.digest()
            val bigInt = java.math.BigInteger(1, md5sum)
            var output = bigInt.toString(16)
            // Rellenar con ceros a la izquierda
            while (output.length < 32) {
                output = "0$output"
            }
            output
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular MD5 para $uri: ${e.message}")
            null
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
    var sincronizadoPor: String = ""
)
