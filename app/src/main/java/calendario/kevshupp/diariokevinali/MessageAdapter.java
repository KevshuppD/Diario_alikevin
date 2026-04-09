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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

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
        View contentLayout;
        TextView tvAuthor, tvContent, tvTimestamp;
        ImageView ivMessageImage, ivAvatar;
        ImageButton btnDelete;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardMessage);
            contentLayout = itemView.findViewById(R.id.messageContentLayout);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Message message, String theme, String currentUserId, OnMessageClickListener listener) {
            tvAuthor.setText("De: " + message.getAuthorName());
            
            if (message.getContent() != null) {
                tvContent.setText(Html.fromHtml(message.getContent(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                tvContent.setText("");
            }
            
            tvTimestamp.setText(dateFormat.format(new Date(message.getTimestamp())));

            applyPixelTheme(theme);

            ivAvatar.setVisibility(View.VISIBLE);
            if (message.getAuthorImageUrl() != null && !message.getAuthorImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(message.getAuthorImageUrl())
                        .placeholder(R.drawable.ic_profile_pixel)
                        .error(R.drawable.ic_profile_pixel)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_pixel);
            }

            if (message.getImageUrls() != null && !message.getImageUrls().isEmpty()) {
                ivMessageImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrls().get(0))
                        .into(ivMessageImage);
            } else {
                ivMessageImage.setVisibility(View.GONE);
            }

            if (message.getAuthorId() != null && message.getAuthorId().equals(currentUserId)) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(message);
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMessageClick(v, message);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onMessageLongClick(v, message);
                return true;
            });
        }

        private void applyPixelTheme(String theme) {
            Typeface pixelFont;
            try {
                pixelFont = ResourcesCompat.getFont(itemView.getContext(), R.font.vt323);
            } catch (Exception e) {
                pixelFont = Typeface.MONOSPACE;
            }
            
            tvContent.setTypeface(pixelFont);
            tvAuthor.setTypeface(pixelFont, Typeface.BOLD);
            tvTimestamp.setTypeface(pixelFont);
            
            tvContent.setTextSize(22);
            tvAuthor.setTextSize(20);

            cardView.setCardElevation(0f);
            cardView.setRadius(0f);
            cardView.setStrokeWidth(0);
            cardView.setCardBackgroundColor(Color.TRANSPARENT);

            switch (theme) {
                case "Pixel Oscuro":
                    setupStyles("#1A1A1A", "#91465F", "#FF4081", "#FFFFFF", "#AAAAAA");
                    break;
                case "Pixel Monocromático":
                    // Black and White
                    setupStyles("#FFFFFF", "#000000", "#000000", "#000000", "#333333");
                    break;
                case "Pixel Claro":
                default:
                    setupStyles("#F3E5AB", "#4A2511", "#1A5D1A", "#4A2511", "#8B4513");
                    break;
            }
        }

        private void setupStyles(String bgColor, String borderColor, String authorColor, String contentColor, String timeColor) {
            Drawable pixelBg = createPixelDrawable(Color.parseColor(bgColor), Color.parseColor(borderColor));
            cardView.setBackground(pixelBg);
            
            tvAuthor.setTextColor(Color.parseColor(authorColor));
            tvContent.setTextColor(Color.parseColor(contentColor));
            tvTimestamp.setTextColor(Color.parseColor(timeColor));
            
            ivAvatar.setBackground(createPixelDrawable(Color.parseColor(bgColor), Color.parseColor(borderColor)));
            ivAvatar.setPadding(6, 6, 6, 6);
        }

        private Drawable createPixelDrawable(int bgColor, int borderColor) {
            GradientDrawable border = new GradientDrawable();
            border.setColor(borderColor);
            
            GradientDrawable main = new GradientDrawable();
            main.setColor(bgColor);
            
            GradientDrawable notch = new GradientDrawable();
            notch.setColor(borderColor);

            Drawable[] layers = new Drawable[6];
            layers[0] = border;
            layers[1] = main;
            layers[2] = notch; layers[3] = notch; layers[4] = notch; layers[5] = notch;

            LayerDrawable layerDrawable = new LayerDrawable(layers);
            
            layerDrawable.setLayerInset(1, 6, 6, 6, 6);
            
            layerDrawable.setLayerSize(2, 12, 12);
            layerDrawable.setLayerGravity(2, Gravity.TOP | Gravity.START);
            
            layerDrawable.setLayerSize(3, 12, 12);
            layerDrawable.setLayerGravity(3, Gravity.TOP | Gravity.END);
            
            layerDrawable.setLayerSize(4, 12, 12);
            layerDrawable.setLayerGravity(4, Gravity.BOTTOM | Gravity.START);
            
            layerDrawable.setLayerSize(5, 12, 12);
            layerDrawable.setLayerGravity(5, Gravity.BOTTOM | Gravity.END);

            return layerDrawable;
        }
    }
}
