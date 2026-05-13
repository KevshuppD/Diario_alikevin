package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateFilterAdapter extends RecyclerView.Adapter<DateFilterAdapter.DateViewHolder> {

    private List<Long> dateList;
    private OnDateSelectedListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM", new Locale("es", "ES"));
    private SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy", Locale.getDefault());

    public interface OnDateSelectedListener {
        void onDateSelected(long timestamp);
    }

    public DateFilterAdapter(List<Long> dateList, OnDateSelectedListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_filter, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        long timestamp = dateList.get(position);
        Date date = new Date(timestamp);
        
        String formattedDate = sdf.format(date);
        // Capitalize first letter
        formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
        
        holder.tvDate.setText(formattedDate);
        holder.tvYear.setText(yearSdf.format(date));
        
        holder.itemView.setOnClickListener(v -> listener.onDateSelected(timestamp));
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvYear;
        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvFilterDateText);
            tvYear = itemView.findViewById(R.id.tvFilterYearText);
            
            Typeface pixelFont;
            try {
                pixelFont = ResourcesCompat.getFont(itemView.getContext(), R.font.vt323);
                tvDate.setTypeface(pixelFont);
                tvYear.setTypeface(pixelFont);
            } catch (Exception ignored) {}
        }
    }
}
