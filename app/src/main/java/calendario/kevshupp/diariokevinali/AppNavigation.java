package calendario.kevshupp.diariokevinali;

public interface AppNavigation {
    void pickImage(int requestCode);
    void logout();
    void showAddEventDialog(String date, CalendarEvent event);
    String getCurrentTheme();
    void applyTheme(String theme);
    void showUpdateDialog(String url);
}
