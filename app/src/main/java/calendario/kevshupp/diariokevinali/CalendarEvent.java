package calendario.kevshupp.diariokevinali;

public class CalendarEvent {
    private String eventId;
    private String title;
    private String description;
    private long date; 
    private String authorId;
    private String authorName;
    private String partnerId;
    private String recurrence; // "NONE", "WEEKLY", "YEARLY"

    public CalendarEvent() {
        this.recurrence = "NONE";
    }

    public CalendarEvent(String eventId, String title, String description, long date, String authorId, String partnerId) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.authorId = authorId;
        this.partnerId = partnerId;
        this.recurrence = "NONE";
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }
}
