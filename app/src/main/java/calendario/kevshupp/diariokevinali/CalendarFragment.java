package calendario.kevshupp.diariokevinali;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private String coupleId, userId, theme;
    private FirebaseFirestore db;
    private ListenerRegistration calendarListener;
    private CalendarAdapter adapter;
    private List<CalendarEvent> allEvents = new ArrayList<>();
    private List<CalendarEvent> dayEvents = new ArrayList<>();
    private long selectedTimestamp;

    public static CalendarFragment newInstance(String coupleId, String userId, String theme) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString("coupleId", coupleId);
        args.putString("userId", userId);
        args.putString("theme", theme);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            coupleId = getArguments().getString("coupleId");
            userId = getArguments().getString("userId");
            theme = getArguments().getString("theme");
        }
        db = FirebaseFirestore.getInstance();
        selectedTimestamp = normalizeDate(System.currentTimeMillis());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        boolean isDark = "Pixel Oscuro".equals(theme);
        // Usamos un inflater con el tema adecuado para que el CalendarView tome los colores
        LayoutInflater themedInflater = isDark ? 
                inflater.cloneInContext(new ContextThemeWrapper(requireContext(), R.style.DarkCalendarTheme)) : 
                inflater;
                
        View view = themedInflater.inflate(R.layout.dialog_calendar, container, false);

        CalendarView cv = view.findViewById(R.id.calendarView);
        TextView tvDays = view.findViewById(R.id.tvDaysWithEvents);
        
        if (isDark) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) view.findViewById(R.id.tvCalendarTitle)).setTextColor(Color.WHITE);
            tvDays.setTextColor(Color.LTGRAY);
            ((TextView) view.findViewById(R.id.tvSelectedDate)).setTextColor(Color.WHITE);
            Button btnAdd = view.findViewById(R.id.btnAddEvent);
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        } else {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel);
        }

        view.findViewById(R.id.btnAddEvent).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showAddEventDialog(selectedTimestamp, null);
            }
        });

        RecyclerView rv = view.findViewById(R.id.rvEvents);
        adapter = new CalendarAdapter(dayEvents, userId, new CalendarAdapter.OnEventActionListener() {
            @Override public void onDeleteEvent(CalendarEvent e) { db.collection("calendar").document(e.getEventId()).delete(); }
            @Override public void onEditEvent(CalendarEvent e) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showAddEventDialog(normalizeDate(e.getDate()), e);
                }
            }
        });
        adapter.setTheme(theme);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        cv.setOnDateChangeListener((v, y, m, d) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(y, m, d);
            selectedTimestamp = normalizeDate(cal.getTimeInMillis());
            updateDayList();
        });

        listenCalendar(tvDays);

        return view;
    }

    private void listenCalendar(TextView tvDays) {
        if (calendarListener != null) calendarListener.remove();
        calendarListener = db.collection("calendar").whereEqualTo("partnerId", coupleId)
                .addSnapshotListener((snaps, e) -> {
                    if (snaps != null) {
                        allEvents.clear();
                        Set<String> uniqueDays = new HashSet<>();
                        for (QueryDocumentSnapshot d : snaps) {
                            CalendarEvent ev = d.toObject(CalendarEvent.class);
                            allEvents.add(ev);
                            Calendar c = Calendar.getInstance();
                            c.setTimeInMillis(ev.getDate());
                            uniqueDays.add(c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1));
                        }
                        if (tvDays != null) {
                            if (allEvents.isEmpty()) tvDays.setText("No hay planes agendados");
                            else tvDays.setText("Planes en: " + String.join(", ", uniqueDays));
                        }
                        updateDayList();
                    }
                });
    }

    private void updateDayList() {
        dayEvents.clear();
        for (CalendarEvent e : allEvents) {
            long ets = e.getDate();
            String rec = e.getRecurrence() != null ? e.getRecurrence() : "NONE";
            if (isSameDay(ets, selectedTimestamp) || 
                (rec.equals("DAILY") && ets <= selectedTimestamp) || 
                (rec.equals("WEEKLY") && ets <= selectedTimestamp && isSameDayOfWeek(ets, selectedTimestamp)) || 
                (rec.equals("MONTHLY") && ets <= selectedTimestamp && isSameDayOfMonth(ets, selectedTimestamp)) || 
                (rec.equals("YEARLY") && ets <= selectedTimestamp && isSameDayAndMonth(ets, selectedTimestamp))) {
                dayEvents.add(e);
            }
        }
        dayEvents.sort((e1, e2) -> Long.compare(e1.getDate(), e2.getDate()));
        adapter.notifyDataSetChanged();
    }

    private boolean isSameDay(long t1, long t2) { return normalizeDate(t1) == normalizeDate(t2); }
    private boolean isSameDayOfWeek(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_WEEK) == c2.get(Calendar.DAY_OF_WEEK); }
    private boolean isSameDayOfMonth(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH); }
    private boolean isSameDayAndMonth(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH); }
    private long normalizeDate(long ts) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }

    @Override
    public void onDestroy() {
        if (calendarListener != null) calendarListener.remove();
        super.onDestroy();
    }
}
