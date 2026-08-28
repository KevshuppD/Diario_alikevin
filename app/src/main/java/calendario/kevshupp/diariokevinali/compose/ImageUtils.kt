package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Utilidad para optimizar URLs de Cloudinary añadiendo parámetros de transformación.
 * Esto reduce el ancho de banda y mejora la velocidad de carga al solicitar imágenes
 * con el tamaño y calidad adecuados.
 */
fun String?.optimizeCloudinary(width: Int = 800): String? {
    if (this == null) return null
    if (this.contains("cloudinary.com") && this.contains("/upload/")) {
        // Evitar duplicar transformaciones si ya existen
        if (this.contains("/w_") || this.contains("/q_auto")) return this
        return this.replace("/upload/", "/upload/w_$width,c_fill,q_auto,f_auto/")
    }
    return this
}

/**
 * Calcula de forma óptima el valor inSampleSize para no cargar Bitmaps gigabytes en memoria RAM.
 */
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Decodifica una imagen desde una Uri local aplicando downsampling seguro para prevenir OutOfMemoryError.
 */
fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int = 1200, reqHeight: Int = 1200): Bitmap? {
    return try {
        // Primero leer solo las dimensiones de la imagen
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calcular el factor de escala inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        // Cargar el bitmap escalado optimizado
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Comprime y reescala una imagen antes de subirla a Cloudinary.
 * Genera un archivo temporal en cacheDir y retorna su Uri (o la original si falla).
 */
fun compressImageForUpload(
    context: Context,
    uri: Uri,
    maxWidth: Int = 1920,
    maxHeight: Int = 1920,
    quality: Int = 85
): Uri {
    return try {
        val bitmap = decodeSampledBitmapFromUri(context, uri, maxWidth, maxHeight) ?: return uri
        val tempFile = java.io.File.createTempFile("upload_opt_", ".jpg", context.cacheDir)
        java.io.FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        Uri.fromFile(tempFile)
    } catch (e: Exception) {
        uri
    }
}
