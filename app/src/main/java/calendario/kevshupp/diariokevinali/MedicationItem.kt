package calendario.kevshupp.diariokevinali

data class MedicationItem(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val durationDays: Int? = null, // null/0/-1 means indefinite/permanent
    val intervalHours: Int = 8,
    val selectedTimes: List<String> = emptyList(), // e.g., ["09:00", "21:00"]
    val enableReminder: Boolean = false,
    val enableAlarm: Boolean = false,
    val startDate: Long = System.currentTimeMillis()
)
