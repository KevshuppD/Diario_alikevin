package calendario.kevshupp.diariokevinali

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.security.MessageDigest

class DuplicateManager(private val context: Context) {

    private val tag = "DuplicateManager"

    private fun calculateMd5(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val buffer = ByteArray(32768)
            var read: Int
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum = digest.digest()
            val bigInt = java.math.BigInteger(1, md5sum)
            var output = bigInt.toString(16)
            while (output.length < 32) {
                output = "0$output"
            }
            output
        } catch (e: Exception) {
            Log.e(tag, "Error calculating MD5 for URI $uri: ${e.message}", e)
            null
        }
    }

    /**
     * Scans the given SAF folder tree URI for duplicate images.
     * Invokes [onProgress] on each file scanned.
     */
    fun findDuplicates(
        localFolderUriStr: String,
        onProgress: (scanned: Int, total: Int) -> Unit
    ): List<DuplicateGroup> {
        val treeUri = Uri.parse(localFolderUriStr)
        val files = listPhotos(treeUri)
        val total = files.size
        if (total == 0) return emptyList()

        val md5Map = mutableMapOf<String, MutableList<LocalPhoto>>()

        files.forEachIndexed { index, photo ->
            onProgress(index + 1, total)
            val md5 = calculateMd5(Uri.parse(photo.uri))
            if (md5 != null) {
                val list = md5Map.getOrPut(md5) { mutableListOf() }
                list.add(photo)
            }
        }

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        md5Map.forEach { (md5, groupFiles) ->
            if (groupFiles.size > 1) {
                // Keep the oldest file (first created/modified) as the original, mark the rest as duplicates
                groupFiles.sortBy { it.lastModified }
                val original = groupFiles.first()
                val duplicates = groupFiles.subList(1, groupFiles.size)
                duplicateGroups.add(DuplicateGroup(md5, original, duplicates.toList()))
            }
        }
        return duplicateGroups
    }

    /**
     * Safely deletes the given duplicate files from SAF.
     * Returns the number of successfully deleted files.
     */
    fun deleteDuplicateFiles(duplicates: List<LocalPhoto>, onDeleted: (Int) -> Unit = {}): Int {
        var count = 0
        duplicates.forEachIndexed { index, photo ->
            try {
                val fileUri = Uri.parse(photo.uri)
                val docFile = DocumentFile.fromSingleUri(context, fileUri)
                if (docFile != null && docFile.exists()) {
                    if (docFile.delete()) {
                        count++
                    } else {
                        Log.w(tag, "Failed to delete file via DocumentFile: ${photo.name}")
                    }
                } else {
                    // Try DocumentsContract direct deletion as fallback
                    if (android.provider.DocumentsContract.deleteDocument(context.contentResolver, fileUri)) {
                        count++
                    } else {
                        Log.w(tag, "Failed to delete file via DocumentsContract fallback: ${photo.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception deleting duplicate file ${photo.name}: ${e.message}", e)
            }
            onDeleted(index + 1)
        }
        return count
    }

    private fun listPhotos(treeUri: Uri): List<LocalPhoto> {
        val fileList = mutableListOf<LocalPhoto>()
        val contentResolver = context.contentResolver
        try {
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
                        fileList.add(LocalPhoto(fileUri.toString(), name, lastModified, size))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error listing local files for duplicate search: ${e.message}", e)
        }
        return fileList
    }
}

data class DuplicateGroup(
    val md5: String,
    val original: LocalPhoto,
    val duplicates: List<LocalPhoto>
)
