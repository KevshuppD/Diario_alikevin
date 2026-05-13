package calendario.kevshupp.diariokevinali;

import java.util.ArrayList;
import java.util.List;

public class Message {
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_ALBUM = "ALBUM";

    private String messageId;
    private String partnerId;
    private String authorId;
    private String authorName;
    private String authorImageUrl;
    private String content;
    private String title;
    private List<String> imageUrls;
    private long timestamp;
    private boolean liked;
    private String type = TYPE_MESSAGE;

    public Message() {
        this.imageUrls = new ArrayList<>();
        this.liked = false;
    }

    public Message(String messageId, String partnerId, String authorId, String authorName, String authorImageUrl, String content, List<String> imageUrls, long timestamp, boolean liked) {
        this.messageId = messageId;
        this.partnerId = partnerId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorImageUrl = authorImageUrl;
        this.content = content;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
        this.liked = liked;
        this.type = (imageUrls != null && imageUrls.size() > 1) ? TYPE_ALBUM : TYPE_MESSAGE;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorImageUrl() { return authorImageUrl; }
    public void setAuthorImageUrl(String authorImageUrl) { this.authorImageUrl = authorImageUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getImageUrl() {
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
    }

    public void setImageUrl(String imageUrl) {
        if (this.imageUrls == null) this.imageUrls = new ArrayList<>();
        else this.imageUrls.clear();
        if (imageUrl != null) this.imageUrls.add(imageUrl);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
