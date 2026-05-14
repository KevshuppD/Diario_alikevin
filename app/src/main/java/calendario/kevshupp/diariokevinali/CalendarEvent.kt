package calendario.kevshupp.diariokevinali

/**
 * Modelo para eventos del calendario con soporte para recurrencia.
 */
data class CalendarEvent(
    var eventId: String = "",
    var title: String = "",
    var description: String = "",
    var date: Long = 0,
    var authorId: String = "",
    var authorName: String = "",
    var partnerId: String = "",
    var recurrence: String = "NONE" // "NONE", "WEEKLY", "YEARLY"
) {
    // Constructor secundario para compatibilidad con el código Java existente (6 parámetros)
    constructor(
        eventId: String,
        title: String,
        description: String,
        date: Long,
        authorId: String,
        partnerId: String
    ) : this(eventId, title, description, date, authorId, "", partnerId, "NONE")
}
