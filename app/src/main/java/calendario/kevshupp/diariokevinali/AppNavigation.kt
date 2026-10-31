package calendario.kevshupp.diariokevinali

interface AppNavigation {
    fun pickImage(requestCode: Int)
    fun logout()
    fun saveCalendarEvent(edit: CalendarEvent?, title: String, description: String, date: Long, recurrence: String)
    fun getCurrentTheme(): String
    fun applyTheme(theme: String)
    fun showUpdateDialog(url: String)
    fun showUpdateDialog(info: AppUpdateInfo) {
        showUpdateDialog(info.downloadUrl)
    }
}
