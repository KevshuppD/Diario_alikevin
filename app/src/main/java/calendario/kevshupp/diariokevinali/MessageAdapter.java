package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private String currentTheme = "Pixel Claro";
    private String currentUserId;
    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(View view, Message message);
        void onMessageLongClick(View view, Message message);
        void onDeleteClick(Message message);
    }

    public MessageAdapter(List<Message> messageList, String currentUserId, OnMessageClickListener listener) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message, currentTheme, currentUserId, listener);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setTheme(String theme) {
        this.currentTheme = theme;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvAuthor, tvContent, tvTimestamp;
        ImageView ivMessageImage, ivAvatar;
        RecyclerView rvAlbumPhotos;
        ImageButton btnDelete, btnLike;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardMessage);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            rvAlbumPhotos = itemView.findViewById(R.id.rvAlbumPhotos);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnLike = itemView.findViewById(R.id.btnLike);
        }

        public void bind(Message message, String theme, String currentUserId, OnMessageClickListener listener) {
            tvAuthor.setText("De: " + message.getAuthorName());
            
            boolean isAlbum = message.getContent() != null && message.getContent().startsWith("[ALBUM]");
            String displayContent = message.getContent();
            if (isAlbum) {
                displayContent = displayContent.replace("[ALBUM] ", "📸 <b>Momento:</b> ");
            }
            
            tvContent.setText(Html.fromHtml(displayContent, Html.FROM_HTML_MODE_COMPACT));
            tvTimestamp.setText(dateFormat.format(new Date(message.getTimestamp())));

            applyPixelTheme(theme);

            if (message.getAuthorImageUrl() != null && !message.getAuthorImageUrl().isEmpty()) {
                Glide.with(itemView.getContext()).load(message.getAuthorImageUrl())
                        .placeholder(R.drawable.ic_profile_pixel).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_pixel);
            }

            if (isAlbum) {
                ivMessageImage.setVisibility(View.GONE);
                rvAlbumPhotos.setVisibility(View.VISIBLE);
                rvAlbumPhotos.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvAlbumPhotos.setAdapter(new AlbumPhotosAdapter(message.getImageUrls()));
            } else {
                rvAlbumPhotos.setVisibility(View.GONE);
                if (message.getImageUrl() != null) {
                    ivMessageImage.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext()).load(message.getImageUrl()).into(ivMessageImage);
                } else {
                    ivMessageImage.setVisibility(View.GONE);
                }
            }

            btnLike.setColorFilter(message.isLiked() ? Color.parseColor("#FF4081") : Color.parseColor("#CCCCCC"));
            if (!message.getAuthorId().equals(currentUserId)) {
                btnLike.setEnabled(true);
                btnLike.setOnClickListener(v -> {
                    message.setLiked(!message.isLiked());
                    FirebaseFirestore.getInstance().collection("messages")
                            .document(message.getMessageId())
                            .update("liked", message.isLiked());
                });
            } else {
                btnLike.setEnabled(false);
            }

            if (message.getAuthorId() != null && message.getAuthorId().equals(currentUserId)) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDeleteClick(message); });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> { if (listener != null) listener.onMessageClick(v, message); });
            itemView.setOnLongClickListener(v -> { if (listener != null) listener.onMessageLongClick(v, message); return true; });
        }

        private void applyPixelTheme(String theme) {
            Typeface pixelFont;
            try { pixelFont = ResourcesCompat.getFont(itemView.getContext(), R.font.vt323); } 
            catch (Exception e) { pixelFont = Typeface.MONOSPACE; }
            tvContent.setTypeface(pixelFont);
            tvAuthor.setTypeface(pixelFont, Typeface.BOLD);
            tvTimestamp.setTypeface(pixelFont);
            switch (theme) {
                case "Pixel Oscuro": setupStyles("#1A1A1A", "#91465F", "#FF4081", "#FFFFFF", "#AAAAAA"); break;
                case "Pixel Monocromático": setupStyles("#FFFFFF", "#000000", "#000000", "#000000", "#333333"); break;
                default: setupStyles("#F3E5AB", "#4A2511", "#1A5D1A", "#4A2511", "#8B4513"); break;
            }
        }

        private void setupStyles(String bgColor, String borderColor, String authorColor, String contentColor, String timeColor) {
            cardView.setBackground(createPixelDrawable(Color.parseColor(bgColor), Color.parseColor(borderColor)));
            tvAuthor.setTextColor(Color.parseColor(authorColor));
            tvContent.setTextColor(Color.parseColor(contentColor));
            tvTimestamp.setTextColor(Color.parseColor(timeColor));
            ivAvatar.setBackground(createPixelDrawable(Color.parseColor(bgColor), Color.parseColor(borderColor)));
            ivAvatar.setPadding(6, 6, 6, 6);
        }

        private Drawable createPixelDrawable(int bgColor, int borderColor) {
            GradientDrawable border = new GradientDrawable(); border.setColor(borderColor);
            GradientDrawable main = new GradientDrawable(); main.setColor(bgColor);
            GradientDrawable notch = new GradientDrawable(); notch.setColor(borderColor);
            Drawable[] layers = {border, main, notch, notch, notch, notch};
            LayerDrawable ld = new LayerDrawable(layers);
            ld.setLayerInset(1, 6, 6, 6, 6);
            ld.setLayerSize(2, 12, 12); ld.setLayerGravity(2, Gravity.TOP | Gravity.START);
            ld.setLayerSize(3, 12, 12); ld.setLayerGravity(3, Gravity.TOP | Gravity.END);
            ld.setLayerSize(4, 12, 12); ld.setLayerGravity(4, Gravity.BOTTOM | Gravity.START);
            ld.setLayerSize(5, 12, 12); ld.setLayerGravity(5, Gravity.BOTTOM | Gravity.END);
            return ld;
        }
    }

    static class AlbumPhotosAdapter extends RecyclerView.Adapter<AlbumPhotosAdapter.PhotoViewHolder> {
        private List<String> photos;
        public AlbumPhotosAdapter(List<String> photos) { this.photos = photos; }
        @NonNull @Override public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(300, 300));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(4, 0, 4, 0);
            return new PhotoViewHolder(iv);
        }
        @Override public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext()).load(photos.get(position)).into((ImageView)holder.itemView);
        }
        @Override public int getItemCount() { return photos != null ? photos.size() : 0; }
        static class PhotoViewHolder extends RecyclerView.ViewHolder { public PhotoViewHolder(@NonNull View itemView) { super(itemView); } }
    }
}
