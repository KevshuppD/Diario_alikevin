package calendario.kevshupp.diariokevinali

import androidx.compose.runtime.Stable

/**
 * Modelo de usuario para Firestore.
 * Se utilizan valores por defecto para permitir que Firestore cree instancias vacías.
 */
@Stable
data class User(
    var userId: String = "",
    var userName: String = "",
    var profileImageUrl: String = ""
)
