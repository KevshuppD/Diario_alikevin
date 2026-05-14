package calendario.kevshupp.diariokevinali

/**
 * Modelo de usuario para Firestore.
 * Se utilizan valores por defecto para permitir que Firestore cree instancias vacías.
 */
data class User(
    var userId: String = "",
    var userName: String = "",
    var profileImageUrl: String = ""
)
