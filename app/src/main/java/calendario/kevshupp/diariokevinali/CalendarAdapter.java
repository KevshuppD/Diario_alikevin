package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarEvent> eventList;
    private String currentUserId;
    private OnEventActionListener listener;
    private String theme = "Pixel Claro";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnEventActionListener {
        void onDeleteEvent(CalendarEvent event);
        void onEditEvent(CalendarEvent event);
    }

    public CalendarAdapter(List<CalendarEvent> eventList, String currentUserId, OnEventActionListener listener) {
        this.eventList = eventList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_event, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarEvent event = eventList.get(position);
        
        boolean isDark = "Pixel Oscuro".equals(theme);
        if (isDark) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1A1A2E"));
            holder.cardView.setStrokeColor(Color.parseColor("#30304A"));
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.tvDescription.setTextColor(Color.LTGRAY);
            holder.tvAuthor.setTextColor(Color.parseColor("#A084CA"));
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.cardView.setStrokeColor(Color.parseColor("#DDDDDD"));
            holder.tvTitle.setTextColor(Color.parseColor("#5D2E7A"));
            holder.tvDescription.setTextColor(Color.parseColor("#333333"));
            holder.tvAuthor.setTextColor(Color.parseColor("#91465F"));
        }

        String timeStr = timeFormat.format(new Date(event.getDate()));
        String recurrenceStr = "";
        if ("WEEKLY".equals(event.getRecurrence())) recurrenceStr = " (Semanal)";
        else if ("YEARLY".equals(event.getRecurrence())) recurrenceStr = " (Anual)";

        holder.tvTitle.setText(event.getTitle() + " - " + timeStr + recurrenceStr);
        holder.tvDescription.setText(event.getDescription());
        holder.tvAuthor.setText("Agendado por: " + event.getAuthorName());

        // Solo permitir borrar/editar si es el autor
        if (event.getAuthorId() != null && event.getAuthorId().equals(currentUserId)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteEvent(event);
            });
            // Al pulsar en cualquier parte de la tarjeta se edita
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEditEvent(event);
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void setTheme(String theme) {
        this.theme = theme;
        notifyDataSetChanged();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvAuthor;
        ImageButton btnDelete;
        com.google.android.material.card.MaterialCardView cardView;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.cardEvent);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDescription = itemView.findViewById(R.id.tvEventDescription);
            tvAuthor = itemView.findViewById(R.id.tvEventAuthor);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
