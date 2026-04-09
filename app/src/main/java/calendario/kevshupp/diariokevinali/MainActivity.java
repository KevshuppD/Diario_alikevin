package calendario.kevshupp.diariokevinali;

import android.app.DownloadManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {

    private static final int PICK_IMAGE_PROFILE = 1;
    private static final int PICK_IMAGE_CARTA = 3;

    private ConstraintLayout mainLayout;
    private MaterialToolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageView ivToolbarHeart;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages;
    private EditText etMessage;
    private ImageButton btnSend, btnExpand;
    private ImageButton btnProfile;
    private ImageButton btnSettings;
    private ImageButton btnFilterDate;
    private ImageButton btnCalendar;
    private View inputContainer;

    private String currentUserImageUri = null;
    private final String currentCoupleId = "vínculo_único_123";
    private String currentUserId;
    private String currentUserName;

    private ImageView ivDialogProfile;
    private ImageView ivSelectedCartaImage;
    private View imageCartaContainer;
    private String currentSelectedCartaImageUrl = null;

    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;
    private ListenerRegistration calendarListener;
    private Calendar selectedFilterDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "user_kevin_01");
        currentUserName = prefs.getString("userName", "Kevin");
        currentUserImageUri = prefs.getString("userImage", null);

        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        listenMessagesFromFirestore();
        listenUserInfo();

        btnSend.setOnClickListener(v -> sendMessage());
        btnExpand.setOnClickListener(v -> showEditDialog(null));
        btnSettings.setOnClickListener(this::showThemeMenu);
        btnProfile.setOnClickListener(v -> showProfileDialog());
        btnFilterDate.setOnClickListener(v -> showDatePicker());
        btnFilterDate.setOnLongClickListener(v -> {
            selectedFilterDate = null;
            listenMessagesFromFirestore();
            Toast.makeText(this, "Mostrando todas las cartas", Toast.LENGTH_SHORT).show();
            return true;
        });
        btnCalendar.setOnClickListener(v -> showCalendarDialog());
        
        applyTheme("Pixel Claro");
        checkUpdatesFromGitHub();
    }

    private void checkUpdatesFromGitHub() {
        // Replace with your actual GitHub username and repository name
        String url = "https://api.github.com/repos/KevShupp/Diario-KevinAli/releases/latest";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body);
                        String latestVersion = json.getString("tag_name").replace("v", "");
                        
                        JSONArray assets = json.getJSONArray("assets");
                        if (assets.length() > 0) {
                            String downloadUrl = assets.getJSONObject(0).getString("browser_download_url");

                            String currentVersion = BuildConfig.VERSION_NAME;
                            if (isNewerVersion(currentVersion, latestVersion)) {
                                runOnUiThread(() -> showUpdateDialog(downloadUrl));
                            }
                        }
                    } catch (Exception e) {
                        Log.e("UPDATE", "Error parsing GitHub JSON", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("UPDATE", "GitHub API call failed");
            }
        });
    }

    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] curParts = current.split("\\.");
            String[] latParts = latest.split("\\.");
            int length = Math.max(curParts.length, latParts.length);
            for (int i = 0; i < length; i++) {
                int cur = i < curParts.length ? Integer.parseInt(curParts[i]) : 0;
                int lat = i < latParts.length ? Integer.parseInt(latParts[i]) : 0;
                if (lat > cur) return true;
                if (cur > lat) return false;
            }
            return false;
        } catch (Exception e) {
            return !current.equals(latest);
        }
    }

    private void showUpdateDialog(String downloadUrl) {
        new AlertDialog.Builder(this)
                .setTitle("¡Nueva versión disponible!")
                .setMessage("He subido mejoras al diario. ¿Quieres actualizar?")
                .setPositiveButton("Actualizar", (d, w) -> startDownload(downloadUrl))
                .setNegativeButton("Luego", null)
                .show();
    }

    private void startDownload(String url) {
        Toast.makeText(this, "Descargando actualización...", Toast.LENGTH_LONG).show();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Actualizando Diario");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "diario_update.apk");

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    installApk();
                    unregisterReceiver(this);
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        }
    }

    private void installApk() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "diario_update.apk");
        if (file.exists()) {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void listenUserInfo() {
        db.collection("users").document(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        currentUserImageUri = value.getString("profileImageUrl");
                        SharedPreferences.Editor editor = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit();
                        editor.putString("userImage", currentUserImageUri);
                        editor.apply();
                        
                        if (ivDialogProfile != null) {
                            Glide.with(MainActivity.this)
                                    .load(currentUserImageUri)
                                    .placeholder(R.drawable.ic_profile_pixel)
                                    .into(ivDialogProfile);
                        }
                    }
                });
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);
        toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        ivToolbarHeart = findViewById(R.id.ivToolbarHeart);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnExpand = findViewById(R.id.btnExpand);
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnFilterDate = findViewById(R.id.btnFilterDate);
        btnCalendar = findViewById(R.id.btnCalendar);
        inputContainer = findViewById(R.id.inputContainer);
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages, currentUserId, this);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);
    }

    private void listenMessagesFromFirestore() {
        if (firestoreListener != null) firestoreListener.remove();

        Query query = db.collection("messages")
                .whereEqualTo("partnerId", currentCoupleId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (selectedFilterDate != null) {
            Calendar startOfDay = (Calendar) selectedFilterDate.clone();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            Calendar endOfDay = (Calendar) selectedFilterDate.clone();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 999);

            query = query.whereGreaterThanOrEqualTo("timestamp", startOfDay.getTimeInMillis())
                         .whereLessThanOrEqualTo("timestamp", endOfDay.getTimeInMillis());
        }

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FIRESTORE_ERROR", "Error al cargar: " + error.getMessage());
                return;
            }
            if (value != null) {
                List<Message> loadedMessages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Message msg = doc.toObject(Message.class);
                    loadedMessages.add(msg);
                }
                messages.clear();
                messages.addAll(loadedMessages);
                adapter.notifyDataSetChanged();
                if (!messages.isEmpty()) rvMessages.scrollToPosition(0);
            }
        });
    }

    private void showDatePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date_filter, null);
        builder.setView(view);

        RecyclerView rvAvailableDates = view.findViewById(R.id.rvAvailableDates);
        Button btnClear = view.findViewById(R.id.btnClearFilter);
        Button btnClose = view.findViewById(R.id.btnCancelFilter);

        List<Long> availableTimestamps = new ArrayList<>();
        DateFilterAdapter filterAdapter = new DateFilterAdapter(availableTimestamps, timestamp -> {
            selectedFilterDate = Calendar.getInstance();
            selectedFilterDate.setTimeInMillis(timestamp);
            listenMessagesFromFirestore();
            Toast.makeText(this, "Filtrando por fecha", Toast.LENGTH_SHORT).show();
        });

        rvAvailableDates.setLayoutManager(new LinearLayoutManager(this));
        rvAvailableDates.setAdapter(filterAdapter);

        AlertDialog dialog = builder.create();

        db.collection("messages")
                .whereEqualTo("partnerId", currentCoupleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<Long> seen = new HashSet<>();
                    availableTimestamps.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long ts = doc.getLong("timestamp");
                        if (ts != null) {
                            long normalized = normalizeDate(ts);
                            if (!seen.contains(normalized)) {
                                availableTimestamps.add(normalized);
                                seen.add(normalized);
                            }
                        }
                    }
                    availableTimestamps.sort((a, b) -> b.compareTo(a));
                    filterAdapter.notifyDataSetChanged();
                });

        btnClear.setOnClickListener(v -> {
            selectedFilterDate = null;
            listenMessagesFromFirestore();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCalendarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calendar, null);
        builder.setView(view);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        RecyclerView rvEvents = view.findViewById(R.id.rvEvents);
        TextView tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        TextView tvDaysWithEvents = view.findViewById(R.id.tvDaysWithEvents);
        Button btnAddEvent = view.findViewById(R.id.btnAddEvent);
        Button btnClose = view.findViewById(R.id.btnCalendarClose);

        List<CalendarEvent> allEvents = new ArrayList<>();
        List<CalendarEvent> dayEvents = new ArrayList<>();
        CalendarAdapter calendarAdapter = new CalendarAdapter(dayEvents, currentUserId, new CalendarAdapter.OnEventActionListener() {
            @Override
            public void onDeleteEvent(CalendarEvent event) {
                db.collection("calendar").document(event.getEventId()).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Cita eliminada", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onEditEvent(CalendarEvent event) {
                showAddEventDialog(normalizeDate(event.getDate()), event);
            }
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(calendarAdapter);

        final long[] currentSelectedTimestamp = {normalizeDate(System.currentTimeMillis())};

        if (calendarListener != null) calendarListener.remove();
        calendarListener = db.collection("calendar")
                .whereEqualTo("partnerId", currentCoupleId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    allEvents.clear();
                    Set<String> datesWithEvents = new HashSet<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                    
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            CalendarEvent event = doc.toObject(CalendarEvent.class);
                            allEvents.add(event);
                            datesWithEvents.add(sdf.format(new Date(event.getDate())));
                        }
                    }
                    
                    if (datesWithEvents.isEmpty()) {
                        tvDaysWithEvents.setText("No hay planes agendados");
                    } else {
                        tvDaysWithEvents.setText("Planes en: " + TextUtils.join(", ", datesWithEvents));
                    }

                    updateDayList(allEvents, dayEvents, currentSelectedTimestamp[0], calendarAdapter);
                });

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            currentSelectedTimestamp[0] = normalizeDate(cal.getTimeInMillis());
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvSelectedDate.setText("Eventos para " + sdf.format(new Date(currentSelectedTimestamp[0])) + ":");
            updateDayList(allEvents, dayEvents, currentSelectedTimestamp[0], calendarAdapter);
        });

        btnAddEvent.setOnClickListener(v -> {
            showAddEventDialog(currentSelectedTimestamp[0], null);
        });

        AlertDialog dialog = builder.create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            if (calendarListener != null) calendarListener.remove();
        });
        dialog.show();
    }

    private void updateDayList(List<CalendarEvent> source, List<CalendarEvent> target, long selectedDate, CalendarAdapter adapter) {
        target.clear();
        Calendar selCal = Calendar.getInstance();
        selCal.setTimeInMillis(selectedDate);
        
        for (CalendarEvent e : source) {
            Calendar eventCal = Calendar.getInstance();
            eventCal.setTimeInMillis(e.getDate());
            long eventDateNormalized = normalizeDate(e.getDate());
            
            boolean isSameDay = eventDateNormalized == selectedDate;
            boolean isWeekly = "WEEKLY".equals(e.getRecurrence()) && 
                               eventCal.get(Calendar.DAY_OF_WEEK) == selCal.get(Calendar.DAY_OF_WEEK) &&
                               eventDateNormalized <= selectedDate;
            boolean isYearly = "YEARLY".equals(e.getRecurrence()) && 
                               eventCal.get(Calendar.DAY_OF_MONTH) == selCal.get(Calendar.DAY_OF_MONTH) && 
                               eventCal.get(Calendar.MONTH) == selCal.get(Calendar.MONTH) &&
                               eventDateNormalized <= selectedDate;

            if (isSameDay || isWeekly || isYearly) {
                target.add(e);
            }
        }
        target.sort((e1, e2) -> {
            Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(e1.getDate());
            Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(e2.getDate());
            int time1 = c1.get(Calendar.HOUR_OF_DAY) * 60 + c1.get(Calendar.MINUTE);
            int time2 = c2.get(Calendar.HOUR_OF_DAY) * 60 + c2.get(Calendar.MINUTE);
            return Integer.compare(time1, time2);
        });
        adapter.notifyDataSetChanged();
    }

    private long normalizeDate(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void showAddEventDialog(long normalizedDayTimestamp, @Nullable CalendarEvent existingEvent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_calendar_event, null);
        builder.setView(view);
        
        EditText etTitle = view.findViewById(R.id.etEventTitle);
        EditText etDesc = view.findViewById(R.id.etEventDescription);
        Button btnPickTime = view.findViewById(R.id.btnPickTime);
        Spinner spinner = view.findViewById(R.id.spinnerRecurrence);

        String[] options = {"Sin repetir", "Semanal", "Anual"};
        ArrayAdapter<String> recurAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_pixel, options);
        recurAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(recurAdapter);

        final Calendar timeCal = Calendar.getInstance();
        if (existingEvent != null) {
            etTitle.setText(existingEvent.getTitle());
            etDesc.setText(existingEvent.getDescription());
            timeCal.setTimeInMillis(existingEvent.getDate());
            btnPickTime.setText(String.format(Locale.getDefault(), "%02d:%02d", timeCal.get(Calendar.HOUR_OF_DAY), timeCal.get(Calendar.MINUTE)));
            if ("WEEKLY".equals(existingEvent.getRecurrence())) spinner.setSelection(1);
            else if ("YEARLY".equals(existingEvent.getRecurrence())) spinner.setSelection(2);
        } else {
            timeCal.setTimeInMillis(normalizedDayTimestamp);
            timeCal.set(Calendar.HOUR_OF_DAY, 12);
            timeCal.set(Calendar.MINUTE, 0);
        }

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                timeCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                timeCal.set(Calendar.MINUTE, minute);
                btnPickTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, timeCal.get(Calendar.HOUR_OF_DAY), timeCal.get(Calendar.MINUTE), true).show();
        });

        builder.setPositiveButton(existingEvent == null ? "Agendar" : "Guardar", (d, which) -> {
            String title = etTitle.getText().toString().trim();
            if (TextUtils.isEmpty(title)) return;

            CalendarEvent event = existingEvent != null ? existingEvent : new CalendarEvent();
            if (existingEvent == null) event.setEventId(UUID.randomUUID().toString());
            event.setTitle(title);
            event.setDescription(etDesc.getText().toString().trim());
            event.setDate(timeCal.getTimeInMillis());
            event.setAuthorId(currentUserId);
            event.setAuthorName(currentUserName);
            event.setPartnerId(currentCoupleId);
            
            String recur = "NONE";
            if (spinner.getSelectedItemPosition() == 1) recur = "WEEKLY";
            else if (spinner.getSelectedItemPosition() == 2) recur = "YEARLY";
            event.setRecurrence(recur);

            db.collection("calendar").document(event.getEventId()).set(event)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, existingEvent == null ? "Cita agendada!" : "Cita actualizada!", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        Message newMessage = new Message();
        newMessage.setMessageId(UUID.randomUUID().toString());
        newMessage.setPartnerId(currentCoupleId);
        newMessage.setAuthorId(currentUserId);
        newMessage.setAuthorName(currentUserName);
        newMessage.setAuthorImageUrl(currentUserImageUri);
        newMessage.setContent(text);
        newMessage.setTimestamp(System.currentTimeMillis());
        
        saveMessageToFirestore(newMessage);
        etMessage.setText("");
    }

    private void saveMessageToFirestore(Message msg) {
        db.collection("messages").document(msg.getMessageId()).set(msg)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Carta enviada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show());
    }

    private void deleteMessage(Message msg) {
        if (msg.getImageUrl() != null) {
            deleteImageFromCloudinary(msg.getImageUrl());
        }
        db.collection("messages").document(msg.getMessageId()).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Carta eliminada", Toast.LENGTH_SHORT).show());
    }

    private void updateMessageInFirestore(Message msg) {
        db.collection("messages").document(msg.getMessageId()).set(msg);
    }

    private void showThemeMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Pixel Claro");
        popup.getMenu().add("Pixel Oscuro");
        popup.getMenu().add("Pixel Monocromático");
        popup.getMenu().add("Cerrar Sesión");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle() != null && item.getTitle().equals("Cerrar Sesión")) {
                logout();
            } else if (item.getTitle() != null) {
                applyTheme(item.getTitle().toString());
            }
            return true;
        });
        popup.show();
    }

    private void logout() {
        SharedPreferences.Editor editor = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void applyTheme(String themeName) {
        adapter.setTheme(themeName);
        switch (themeName) {
            case "Pixel Oscuro":
                mainLayout.setBackgroundColor(Color.BLACK);
                setupToolbar("#1F1F1F", "#00FF41", Color.WHITE);
                setupInput("#333333", "#00FF41", "#1A1A1A", Color.WHITE);
                break;
            case "Pixel Monocromático":
                // Black and White
                mainLayout.setBackgroundColor(Color.WHITE);
                setupToolbar("#000000", "#333333", Color.WHITE);
                setupInput("#FFFFFF", "#000000", "#FFFFFF", Color.BLACK);
                break;
            case "Pixel Claro":
            default:
                mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
                setupToolbar("#5D2E7A", "#2D1444", Color.WHITE);
                setupInput("#91465F", "#4D1A30", "#F3E5AB", Color.parseColor("#4A2511"));
                break;
        }
    }

    private void setupToolbar(String bgColor, String borderColor, int textColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setStroke(6, Color.parseColor(borderColor));
        toolbar.setBackground(gd);
        if (tvToolbarTitle != null) tvToolbarTitle.setTextColor(textColor);
        if (ivToolbarHeart != null) ivToolbarHeart.setColorFilter(textColor);
        btnSettings.setColorFilter(textColor);
        btnProfile.setColorFilter(textColor);
        btnCalendar.setColorFilter(textColor);
        btnFilterDate.setColorFilter(textColor);
    }

    private void setupInput(String containerColor, String borderColor, String editBgColor, int editTextColor) {
        GradientDrawable containerGd = new GradientDrawable();
        containerGd.setColor(Color.parseColor(containerColor));
        containerGd.setStroke(6, Color.parseColor(borderColor));
        inputContainer.setBackground(containerGd);

        GradientDrawable editGd = new GradientDrawable();
        editGd.setColor(Color.parseColor(editBgColor));
        editGd.setStroke(4, Color.parseColor(borderColor));
        etMessage.setBackground(editGd);
        etMessage.setTextColor(editTextColor);
        etMessage.setHintTextColor(editTextColor);

        btnSend.setColorFilter(editTextColor);
        btnExpand.setColorFilter(editTextColor);
    }

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null);
        builder.setView(view);

        ivDialogProfile = view.findViewById(R.id.ivProfileImage);
        TextView tvName = view.findViewById(R.id.tvCurrentUserName);
        TextView tvTogether = view.findViewById(R.id.tvTogetherTime);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);
        Button btnLogout = view.findViewById(R.id.btnLogoutProfile);

        String nameText = "Usuario: " + currentUserName;
        tvName.setText(nameText);

        // Counter: 19 de enero del 2022
        Calendar start = Calendar.getInstance();
        start.set(2022, Calendar.JANUARY, 19, 0, 0, 0);
        Calendar now = Calendar.getInstance();
        
        long diff = now.getTimeInMillis() - start.getTimeInMillis();
        long totalDays = TimeUnit.MILLISECONDS.toDays(diff);
        
        long years = totalDays / 365;
        long remainingAfterYears = totalDays % 365;
        long months = remainingAfterYears / 30;
        long finalDays = remainingAfterYears % 30;

        String togetherText = "Juntos por: " + years + " años, " + months + " meses y " + finalDays + " días";
        tvTogether.setText(togetherText);

        if (currentUserImageUri != null) {
            Glide.with(this).load(currentUserImageUri).into(ivDialogProfile);
        }

        ivDialogProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_PROFILE);
        });

        AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> dialog.dismiss());
        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            logout();
        });

        dialog.show();
    }

    private void startCrop(Uri sourceUri) {
        String destinationFileName = "cropped_image_" + System.currentTimeMillis() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        
        int purple = Color.parseColor("#5D2E7A");
        int darkPurple = Color.parseColor("#2D1444");
        
        options.setToolbarColor(purple);
        options.setStatusBarColor(darkPurple);
        options.setActiveControlsWidgetColor(purple);
        options.setToolbarWidgetColor(Color.WHITE);
        options.setToolbarTitle("Recortar Foto");
        
        options.setHideBottomControls(true);
        options.setFreeStyleCropEnabled(false);
        options.setShowCropGrid(true);
        options.setCircleDimmedLayer(true);
        
        UCrop.of(sourceUri, Uri.fromFile(new File(getCacheDir(), destinationFileName)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_PROFILE) {
                Uri imageUri = data.getData();
                if (imageUri != null) startCrop(imageUri);
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    Log.d("UCROP", "Recorte exitoso: " + resultUri.toString());
                    uploadToCloudinary(resultUri, true);
                }
            } else if (requestCode == PICK_IMAGE_CARTA) {
                Uri imageUri = data.getData();
                if (imageUri != null) uploadToCloudinary(imageUri, false);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e("UCROP_ERROR", "Error en recorte: " + cropError.getMessage());
                Toast.makeText(this, "Error al recortar imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadToCloudinary(Uri fileUri, boolean isProfile) {
        MediaManager.get().upload(fileUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                if (isProfile) {
                    updateProfileImage(imageUrl);
                } else {
                    currentSelectedCartaImageUrl = imageUrl;
                    if (ivSelectedCartaImage != null) {
                        imageCartaContainer.setVisibility(View.VISIBLE);
                        Glide.with(MainActivity.this).load(imageUrl).into(ivSelectedCartaImage);
                    }
                }
            }
            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(MainActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void updateProfileImage(String imageUrl) {
        if (currentUserImageUri != null && !currentUserImageUri.equals(imageUrl)) {
            deleteImageFromCloudinary(currentUserImageUri);
        }

        currentUserImageUri = imageUrl;
        Map<String, Object> data = new HashMap<>();
        data.put("profileImageUrl", imageUrl);
        
        db.collection("users").document(currentUserId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Foto de perfil guardada!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al guardar foto: " + e.getMessage());
                    Toast.makeText(this, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show();
                });
    }

    private String getPublicIdFromUrl(String url) {
        if (url == null || !url.contains("/") || !url.contains("cloudinary")) return null;
        try {
            String[] parts = url.split("/");
            String lastPart = parts[parts.length - 1];
            int dotIndex = lastPart.lastIndexOf('.');
            if (dotIndex > 0) {
                return lastPart.substring(0, dotIndex);
            }
            return lastPart;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteImageFromCloudinary(String url) {
        final String publicId = getPublicIdFromUrl(url);
        if (publicId == null) return;

        new Thread(() -> {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dhaqjw7se");
                config.put("api_key", "199351452699291");
                config.put("api_secret", "mU2Dk2JSYPVpjkuYJebvOaiGLyc");
                com.cloudinary.Cloudinary cloudinary = new com.cloudinary.Cloudinary(config);
                cloudinary.uploader().destroy(publicId, new HashMap());
                Log.d("CLOUDINARY", "Imagen eliminada: " + publicId);
            } catch (Exception e) {
                Log.e("CLOUDINARY", "Error eliminando de Cloudinary: " + e.getMessage());
            }
        }).start();
    }

    private void showViewMessageDialog(Message message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view_message, null);
        builder.setView(view);

        TextView tvAuthor = view.findViewById(R.id.tvViewAuthor);
        TextView tvTimestamp = view.findViewById(R.id.tvViewTimestamp);
        TextView tvContent = view.findViewById(R.id.tvViewContent);
        ImageView ivImage = view.findViewById(R.id.ivViewImage);
        Button btnClose = view.findViewById(R.id.btnClose);

        if (tvAuthor != null) tvAuthor.setText("De: " + message.getAuthorName());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        if (tvTimestamp != null) tvTimestamp.setText(sdf.format(new Date(message.getTimestamp())));
        
        if (tvContent != null) {
            if (message.getContent() != null) {
                tvContent.setText(Html.fromHtml(message.getContent(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                tvContent.setText("");
            }
        }

        if (ivImage != null) {
            String imageUrl = message.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ivImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(imageUrl).into(ivImage);
            } else {
                ivImage.setVisibility(View.GONE);
            }
        }

        AlertDialog dialog = builder.create();
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void applySpan(EditText editText, Object span) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start != end) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(editText.getText());
            ssb.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editText.setText(ssb);
            editText.setSelection(start, end);
        }
    }

    private void showEditDialog(@Nullable Message existingMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null);
        builder.setView(view);

        EditText etContent = view.findViewById(R.id.etDialogMessage);
        ivSelectedCartaImage = view.findViewById(R.id.ivSelectedImage);
        imageCartaContainer = view.findViewById(R.id.imageContainer);
        Button btnAddImage = view.findViewById(R.id.btnAddImage);
        ImageButton btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        
        Button btnBold = view.findViewById(R.id.btnBold);
        Button btnItalic = view.findViewById(R.id.btnItalic);
        Button btnColor = view.findViewById(R.id.btnColor);

        if (existingMessage != null) {
            if (existingMessage.getContent() != null) {
                etContent.setText(Html.fromHtml(existingMessage.getContent(), Html.FROM_HTML_MODE_COMPACT));
            }
            currentSelectedCartaImageUrl = existingMessage.getImageUrl();
            if (currentSelectedCartaImageUrl != null) {
                imageCartaContainer.setVisibility(View.VISIBLE);
                Glide.with(this).load(currentSelectedCartaImageUrl).into(ivSelectedCartaImage);
            }
        } else {
            currentSelectedCartaImageUrl = null;
        }

        btnBold.setOnClickListener(v -> applySpan(etContent, new StyleSpan(Typeface.BOLD)));
        btnItalic.setOnClickListener(v -> applySpan(etContent, new StyleSpan(Typeface.ITALIC)));
        btnColor.setOnClickListener(v -> {
            PopupMenu colorPopup = new PopupMenu(this, v);
            colorPopup.getMenu().add("Rojo").setOnMenuItemClickListener(i -> { applySpan(etContent, new ForegroundColorSpan(Color.RED)); return true; });
            colorPopup.getMenu().add("Azul").setOnMenuItemClickListener(i -> { applySpan(etContent, new ForegroundColorSpan(Color.BLUE)); return true; });
            colorPopup.getMenu().add("Verde").setOnMenuItemClickListener(i -> { applySpan(etContent, new ForegroundColorSpan(Color.GREEN)); return true; });
            colorPopup.getMenu().add("Rosa").setOnMenuItemClickListener(i -> { applySpan(etContent, new ForegroundColorSpan(Color.parseColor("#FF4081"))); return true; });
            colorPopup.getMenu().add("Café").setOnMenuItemClickListener(i -> { applySpan(etContent, new ForegroundColorSpan(Color.parseColor("#4A2511"))); return true; });
            colorPopup.show();
        });

        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_CARTA);
        });

        btnRemoveImage.setOnClickListener(v -> {
            currentSelectedCartaImageUrl = null;
            imageCartaContainer.setVisibility(View.GONE);
        });

        AlertDialog dialog = builder.create();
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        btnSave.setOnClickListener(v -> {
            String htmlContent = Html.toHtml(etContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            if (TextUtils.isEmpty(htmlContent)) return;

            if (existingMessage == null) {
                Message newMessage = new Message();
                newMessage.setMessageId(UUID.randomUUID().toString());
                newMessage.setPartnerId(currentCoupleId);
                newMessage.setAuthorId(currentUserId);
                newMessage.setAuthorName(currentUserName);
                newMessage.setAuthorImageUrl(currentUserImageUri);
                newMessage.setContent(htmlContent);
                newMessage.setImageUrl(currentSelectedCartaImageUrl);
                newMessage.setTimestamp(System.currentTimeMillis());
                saveMessageToFirestore(newMessage);
            } else {
                if (existingMessage.getImageUrl() != null && !existingMessage.getImageUrl().equals(currentSelectedCartaImageUrl)) {
                    deleteImageFromCloudinary(existingMessage.getImageUrl());
                }
                existingMessage.setContent(htmlContent);
                existingMessage.setImageUrl(currentSelectedCartaImageUrl);
                updateMessageInFirestore(existingMessage);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onMessageClick(View view, Message message) {
        showViewMessageDialog(message);
    }

    @Override
    public void onMessageLongClick(View view, Message message) {
        if (message.getAuthorId() != null && message.getAuthorId().equals(currentUserId)) {
            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenu().add("Editar");
            popup.getMenu().add("Eliminar");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle() != null && item.getTitle().equals("Editar")) {
                    showEditDialog(message);
                } else {
                    deleteMessage(message);
                }
                return true;
            });
            popup.show();
        }
    }

    @Override
    public void onDeleteClick(Message message) {
        deleteMessage(message);
    }
}
