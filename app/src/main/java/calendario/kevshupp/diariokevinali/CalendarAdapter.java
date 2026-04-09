package calendario.kevshupp.diariokevinali;

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

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvAuthor;
        ImageButton btnDelete;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDescription = itemView.findViewById(R.id.tvEventDescription);
            tvAuthor = itemView.findViewById(R.id.tvEventAuthor);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
