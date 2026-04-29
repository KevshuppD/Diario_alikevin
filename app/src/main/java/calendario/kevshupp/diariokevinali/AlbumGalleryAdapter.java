package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AlbumGalleryAdapter extends RecyclerView.Adapter<AlbumGalleryAdapter.GalleryViewHolder> {

    private List<Message> moments;
    private OnMomentClickListener listener;
    private String theme = "Pixel Claro";

    public interface OnMomentClickListener {
        void onMomentClick(Message m);
        void onMomentLongClick(View v, Message m);
    }

    public AlbumGalleryAdapter(List<Message> moments, OnMomentClickListener listener) {
        this.moments = moments;
        this.listener = listener;
    }

    public void setTheme(String theme) {
        this.theme = theme;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_gallery, parent, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Message m = moments.get(position);
        
        List<String> urls = m.getImageUrls();
        String displayUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : null;

        Glide.with(holder.itemView.getContext())
                .load(displayUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_album_pixel)
                .into(holder.ivImage);

        if ("Pixel Oscuro".equals(theme)) {
            holder.card.setCardBackgroundColor(Color.parseColor("#1A1A2E"));
            holder.card.setStrokeColor(Color.parseColor("#30304A"));
        } else {
            holder.card.setCardBackgroundColor(Color.WHITE);
            holder.card.setStrokeColor(Color.parseColor("#DDDDDD"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMomentClick(m);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onMomentLongClick(v, m);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return moments.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        MaterialCardView card;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivGalleryImage);
            card = itemView.findViewById(R.id.cardGallery);
        }
    }
}
