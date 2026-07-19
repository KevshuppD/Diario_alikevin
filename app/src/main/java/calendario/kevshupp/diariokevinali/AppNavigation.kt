package calendario.kevshupp.diariokevinali

interface AppNavigation {
    fun pickImage(requestCode: Int)
    fun logout()
    fun showAddEventDialog(date: String, event: CalendarEvent?)
    fun getCurrentTheme(): String
    fun applyTheme(theme: String)
    fun showUpdateDialog(url: String)
}
