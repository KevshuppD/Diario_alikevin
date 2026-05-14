package calendario.kevshupp.diariokevinali.compose

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
