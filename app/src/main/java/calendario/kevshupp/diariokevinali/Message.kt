package calendario.kevshupp.diariokevinali

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Stable

/**
 * Modelo de mensaje/publicación para el feed y álbumes.
 */
@IgnoreExtraProperties
@Stable
data class Message @JvmOverloads constructor(
    var messageId: String? = null,
    var partnerId: String? = null,
    var authorId: String? = null,
    var authorName: String? = null,
    var authorImageUrl: String? = null,
    var content: String? = null,
    var title: String? = null,
    var imageUrls: MutableList<String>? = mutableListOf(),
    var timestamp: Long = 0,
    var liked: Boolean = false,
    var type: String? = TYPE_MESSAGE
) {
    constructor(
        messageId: String?,
        partnerId: String?,
        authorId: String?,
        authorName: String?,
        authorImageUrl: String?,
        content: String?,
        imageUrls: MutableList<String>?,
        timestamp: Long,
        liked: Boolean
    ) : this(
        messageId, partnerId, authorId, authorName, authorImageUrl,
        content, null, imageUrls ?: mutableListOf(), timestamp, liked,
        if ((imageUrls?.size ?: 0) > 1) TYPE_ALBUM else TYPE_MESSAGE
    )

    companion object {
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_ALBUM = "ALBUM"
    }

    // Propiedad calculada para compatibilidad con código existente que espera un solo String
    @get:PropertyName("imageUrl")
    @set:PropertyName("imageUrl")
    var imageUrl: String?
        get() = imageUrls?.firstOrNull()
        set(value) {
            if (imageUrls == null) imageUrls = mutableListOf()
            if (value != null && imageUrls?.contains(value) != true) {
                if (imageUrls!!.isEmpty()) {
                    imageUrls?.add(value)
                }
            }
        }

    @get:JvmName("getIsLikedProp")
    @set:JvmName("setIsLikedProp")
    @get:PropertyName("isLiked")
    @set:PropertyName("isLiked")
    var isLiked: Boolean
        get() = liked
        set(value) {
            liked = value
        }

    // Inicialización para ajustar el tipo basado en las imágenes (copiado de la lógica Java)
    init {
        if ((imageUrls?.size ?: 0) > 1) {
            type = TYPE_ALBUM
        }
    }
}
