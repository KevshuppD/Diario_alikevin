package calendario.kevshupp.diariokevinali;

import android.Manifest;
import android.app.DownloadManager;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.database.Cursor;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.os.Handler;
import android.os.Looper;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.core.content.res.ResourcesCompat;
import com.yalantis.ucrop.UCrop;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {

    private static final int PICK_IMAGE_PROFILE = 1;
    private static final int PICK_IMAGE_CARTA = 3;
    private static final int PICK_IMAGE_ALBUM = 4;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ConstraintLayout mainLayout;
    private MaterialToolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageView ivToolbarHeart;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages;
    private EditText etMessage;
    private ImageButton btnSend, btnExpand, btnProfile, btnSettings, btnFilterDate, btnCalendar, btnAlbum;
    private View inputContainer;

    private String currentUserImageUri = null;
    private final String currentCoupleId = "vínculo_único_123";
    private String currentUserId, currentUserName;

    private ImageView ivDialogProfile, ivSelectedCartaImage;
    private View imageCartaContainer;
    private String currentSelectedCartaImageUrl = null;
    private final List<String> currentAlbumImages = new ArrayList<>();
    private RecyclerView rvAlbumPreview;
    private int currentCropType = -1;
    private static final int REQUEST_INSTALL_PACKAGES = 200;
    private long latestDownloadId = -1;
    private boolean hasShownDownloadCompleteDialog = false;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver;
    private LinearLayout downloadProgressContainer;
    private ProgressBar downloadProgressBar;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener, calendarListener;
    private Calendar selectedFilterDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "user_kevin_01");
        currentUserName = prefs.getString("userName", "Kevin");
        currentUserImageUri = prefs.getString("userImage", null);

        if (!prefs.contains("coupleId")) {
            prefs.edit().putString("coupleId", currentCoupleId).apply();
        }

        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        initUpdateReceiver();

        initViews();
        downloadProgressContainer = findViewById(R.id.downloadProgressContainer);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        setupRecyclerView();
        listenMessagesFromFirestore();
        listenUserInfo();

        btnSend.setOnClickListener(v -> sendMessage());
        btnExpand.setOnClickListener(v -> showEditDialog(null));
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnProfile.setOnClickListener(v -> showProfileDialog());
        btnFilterDate.setOnClickListener(v -> showDatePicker());
        btnCalendar.setOnClickListener(v -> showCalendarDialog());
        btnAlbum.setOnClickListener(v -> showAlbumOptionsDialog());
        
        applyTheme("Pixel Claro");
        checkUpdatesFromGitHub();
        checkAndRequestPermissions();
        setupFirebaseMessaging();
        handleWidgetIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(
                downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            );
        }
        checkDownloadedUpdateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent == null) return;
        String messageId = intent.getStringExtra("openMessageId");
        if (messageId == null || messageId.isEmpty()) return;
        db.collection("messages").document(messageId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Message m = doc.toObject(Message.class);
            if (m == null) return;
            if (m.getContent() != null && m.getContent().startsWith("[ALBUM]")) {
                showAlbumDetail(m);
            } else {
                showMessageDetail(m);
            }
        });
    }

    private void setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().subscribeToTopic("diario_" + currentCoupleId)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FCM", "Suscrito al tema de la pareja");
                }
            });
    }

    private void initUpdateReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != latestDownloadId) return;
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Uri uri = downloadManager.getUriForDownloadedFile(id);
                            if (uri != null) {
                                runOnUiThread(() -> showInstallNotification(uri));
                            }
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error descargando actualización", Toast.LENGTH_LONG).show());
                        }
                    }
                }
            }
        };
    }

    private void showInstallNotification(Uri apkUri) {
        new AlertDialog.Builder(this)
                .setTitle("Descarga completada")
                .setMessage("La actualización se descargó correctamente. ¿Deseas instalarla ahora?")
                .setPositiveButton("Instalar", (dialog, which) -> installDownloadedApk(apkUri))
                .setNegativeButton("Más tarde", (dialog, which) -> Toast.makeText(this, "Puedes instalar la actualización desde el archivo descargado más tarde.", Toast.LENGTH_SHORT).show())
                .setCancelable(true)
                .show();
    }

    private void installDownloadedApk(Uri apkUri) {
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(installIntent);
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Algunos permisos son necesarios para el funcionamiento del Diario", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
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
        btnAlbum = findViewById(R.id.btnAlbum);
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
        Query query = db.collection("messages").whereEqualTo("partnerId", currentCoupleId).orderBy("timestamp", Query.Direction.DESCENDING);
        if (selectedFilterDate != null) {
            Calendar s = (Calendar) selectedFilterDate.clone();
            s.set(Calendar.HOUR_OF_DAY, 0); s.set(Calendar.MINUTE, 0); s.set(Calendar.SECOND, 0);
            Calendar e = (Calendar) selectedFilterDate.clone();
            e.set(Calendar.HOUR_OF_DAY, 23); e.set(Calendar.MINUTE, 59); e.set(Calendar.SECOND, 59);
            query = query.whereGreaterThanOrEqualTo("timestamp", s.getTimeInMillis()).whereLessThanOrEqualTo("timestamp", e.getTimeInMillis());
        }
        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (value != null) {
                messages.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Message m = doc.toObject(Message.class);
                    // FILTRO: Solo agregar al feed principal si NO es un momento del álbum
                    if (m.getContent() == null || !m.getContent().startsWith("[ALBUM]")) {
                        messages.add(m);
                    }
                }
                adapter.notifyDataSetChanged();

                // Actualizar widget cuando hay nuevos mensajes
                Intent wIntent = new Intent(this, LastMessageWidget.class);
                wIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] wIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
                wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
                sendBroadcast(wIntent);
            }
        });
    }

    private void listenUserInfo() {
        db.collection("users").document(currentUserId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                String url = snapshot.getString("profileImageUrl");
                if (url != null && !url.equals(currentUserImageUri)) {
                    currentUserImageUri = url;
                    getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().putString("userImage", url).apply();
                }
            }
        });
    }

    private void showDatePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date_filter, null);
        builder.setView(view);
        RecyclerView rv = view.findViewById(R.id.rvAvailableDates);
        List<Long> tsList = new ArrayList<>();
        DateFilterAdapter adp = new DateFilterAdapter(tsList, ts -> {
            selectedFilterDate = Calendar.getInstance(); selectedFilterDate.setTimeInMillis(ts);
            listenMessagesFromFirestore();
        });
        rv.setLayoutManager(new LinearLayoutManager(this)); rv.setAdapter(adp);
        db.collection("messages").whereEqualTo("partnerId", currentCoupleId).get().addOnSuccessListener(shots -> {
            Set<Long> seen = new HashSet<>(); tsList.clear();
            for (QueryDocumentSnapshot d : shots) {
                Long ts = d.getLong("timestamp");
                if (ts != null) { long n = normalizeDate(ts); if (seen.add(n)) tsList.add(n); }
            }
            tsList.sort((t1, t2) -> t2.compareTo(t1)); adp.notifyDataSetChanged();
        });
        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnClearFilter).setOnClickListener(v -> { selectedFilterDate = null; listenMessagesFromFirestore(); dialog.dismiss(); });
        view.findViewById(R.id.btnCancelFilter).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCalendarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calendar, null);
        builder.setView(view);
        CalendarView cv = view.findViewById(R.id.calendarView);
        RecyclerView rv = view.findViewById(R.id.rvEvents);
        List<CalendarEvent> all = new ArrayList<>(), day = new ArrayList<>();
        CalendarAdapter adp = new CalendarAdapter(day, currentUserId, new CalendarAdapter.OnEventActionListener() {
            @Override public void onDeleteEvent(CalendarEvent e) { db.collection("calendar").document(e.getEventId()).delete(); }
            @Override public void onEditEvent(CalendarEvent e) { showAddEventDialog(normalizeDate(e.getDate()), e); }
        });
        rv.setLayoutManager(new LinearLayoutManager(this)); rv.setAdapter(adp);
        final long[] sTs = {normalizeDate(System.currentTimeMillis())};
        if (calendarListener != null) calendarListener.remove();
        TextView tvDaysWithEvents = view.findViewById(R.id.tvDaysWithEvents);
        calendarListener = db.collection("calendar").whereEqualTo("partnerId", currentCoupleId).addSnapshotListener((snaps, e) -> {
            if (snaps != null) { 
                all.clear(); 
                int totalEvents = snaps.size();
                if (totalEvents == 0) {
                    tvDaysWithEvents.setText("No hay planes agendados aún.");
                } else {
                    tvDaysWithEvents.setText("Tienes " + totalEvents + " plan(es) en total.");
                }
                
                for (QueryDocumentSnapshot d : snaps) {
                    CalendarEvent ev = d.toObject(CalendarEvent.class);
                    all.add(ev);
                }
                updateDayList(all, day, sTs[0], adp);

                Intent wIntent = new Intent(this, LastMessageWidget.class);
                wIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] wIds = AppWidgetManager.getInstance(getApplication())
                    .getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
                wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
                sendBroadcast(wIntent);
            } else if (e != null) {
                Log.e("Firestore", "Error escuchando calendario", e);
                tvDaysWithEvents.setText("Error al cargar planes.");
            }
        });
        cv.setOnDateChangeListener((v, y, m, d) -> {
            Calendar cal = Calendar.getInstance(); cal.set(y, m, d); sTs[0] = normalizeDate(cal.getTimeInMillis());
            updateDayList(all, day, sTs[0], adp);
        });
        final AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnAddEvent).setOnClickListener(v -> showAddEventDialog(sTs[0], null));
        view.findViewById(R.id.btnCalendarClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateDayList(List<CalendarEvent> all, List<CalendarEvent> day, long ts, CalendarAdapter adp) {
        day.clear(); 
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(ts);
        
        for (CalendarEvent e : all) {
            long eventTs = e.getDate();
            String recurrence = e.getRecurrence() != null ? e.getRecurrence() : "NONE";
            
            if (isSameDay(eventTs, ts)) {
                day.add(e);
            } else if (recurrence.equals("DAILY")) {
                if (eventTs <= ts) day.add(e);
            } else if (recurrence.equals("WEEKLY")) {
                if (eventTs <= ts && isSameDayOfWeek(eventTs, ts)) day.add(e);
            } else if (recurrence.equals("MONTHLY")) {
                if (eventTs <= ts && isSameDayOfMonth(eventTs, ts)) day.add(e);
            } else if (recurrence.equals("YEARLY")) {
                if (eventTs <= ts && isSameDayAndMonth(eventTs, ts)) day.add(e);
            }
        }
        day.sort((e1, e2) -> Long.compare(e1.getDate(), e2.getDate())); 
        adp.notifyDataSetChanged();
    }

    private boolean isSameDay(long ts1, long ts2) {
        return normalizeDate(ts1) == normalizeDate(ts2);
    }

    private boolean isSameDayOfWeek(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(ts1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(ts2);
        return c1.get(Calendar.DAY_OF_WEEK) == c2.get(Calendar.DAY_OF_WEEK);
    }

    private boolean isSameDayOfMonth(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(ts1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(ts2);
        return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isSameDayAndMonth(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(ts1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(ts2);
        return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH) &&
               c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    private long normalizeDate(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void showAddEventDialog(long ts, @Nullable CalendarEvent edit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_calendar_event, null);
        builder.setView(v);
        EditText et = v.findViewById(R.id.etEventTitle);
        EditText etDesc = v.findViewById(R.id.etEventDescription);
        Button btn = v.findViewById(R.id.btnPickTime);
        Spinner spinner = v.findViewById(R.id.spinnerRecurrence);

        String[] options = {"No repetir", "Diario", "Semanal", "Mensual", "Anual"};
        String[] values = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.parseColor("#4A2511"));
                    ((TextView) view).setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.vt323));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.parseColor("#4A2511"));
                    ((TextView) view).setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.vt323));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        final Calendar time = Calendar.getInstance();
        if (edit != null) { 
            et.setText(edit.getTitle());
            if (etDesc != null) etDesc.setText(edit.getDescription());
            time.setTimeInMillis(edit.getDate()); 
            btn.setText(String.format(Locale.getDefault(), "%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
            
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(edit.getRecurrence())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
        else { 
            time.setTimeInMillis(ts); 
            time.set(Calendar.HOUR_OF_DAY, 12); 
            time.set(Calendar.MINUTE, 0); 
            btn.setText("Seleccionar hora (12:00)");
        }

        btn.setOnClickListener(v1 -> new TimePickerDialog(this, (v2, h, m) -> {
            time.set(Calendar.HOUR_OF_DAY, h); time.set(Calendar.MINUTE, m);
            btn.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
        }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show());

        // Se crea el diálogo antes de asignar listeners a los botones internos
        final AlertDialog dialog = builder.create();

        v.findViewById(R.id.btnCancelEvent).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSaveEvent).setOnClickListener(v1 -> {
            String title = et.getText().toString().trim(); if (title.isEmpty()) return;
            String desc = etDesc != null ? etDesc.getText().toString().trim() : "";
            String id = edit != null ? edit.getEventId() : UUID.randomUUID().toString();
            String recurrence = values[spinner.getSelectedItemPosition()];
            
            CalendarEvent ev = new CalendarEvent(id, title, desc, time.getTimeInMillis(), currentUserId, currentCoupleId);
            ev.setRecurrence(recurrence);
            
            db.collection("calendar").document(id).set(ev);
            Toast.makeText(this, "Evento guardado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void sendMessage() {
        String txt = etMessage.getText().toString().trim(); if (txt.isEmpty()) return;
        Message msg = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, txt, new ArrayList<>(), System.currentTimeMillis(), false);
        db.collection("messages").document(msg.getMessageId()).set(msg)
            .addOnSuccessListener(aVoid -> {
                sendNotificationV1(txt);
            });
        etMessage.setText("");
    }

    private String getAccessToken() {
        try {
            InputStream is = getAssets().open("service-account.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e("FCM_V1", "Error obteniendo token: " + e.getMessage());
            return null;
        }
    }

    private void sendNotificationV1(String messageText) {
        new Thread(() -> {
            String token = getAccessToken();
            if (token == null) return;

            try {
                String projectId = "diario-pareja-a2d35"; 
                String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

                OkHttpClient client = new OkHttpClient();

                JSONObject jsonBody = new JSONObject();
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                JSONObject data = new JSONObject();

                notification.put("title", "Nuevo mensaje de " + currentUserName);
                notification.put("body", messageText);

                data.put("authorId", currentUserId);
                data.put("title", "Nuevo mensaje de " + currentUserName);
                data.put("body", messageText);

                message.put("topic", "diario_" + currentCoupleId);
                message.put("notification", notification);
                message.put("data", data);

                jsonBody.put("message", message);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                client.newCall(request).execute();
                Log.d("FCM_V1", "Notificación v1 enviada!");

            } catch (Exception e) {
                Log.e("FCM_V1", "Error en envío: " + e.getMessage());
            }
        }).start();
    }

    private void deleteMessage(Message msg) { db.collection("messages").document(msg.getMessageId()).delete(); }

    private void showSettingsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        b.setView(v);

        final AlertDialog dialog = b.create();

        TextView tvVersion = v.findViewById(R.id.tvAppVersion);
        tvVersion.setText("Versión actual: " + BuildConfig.VERSION_NAME);

        RadioGroup rgThemes = v.findViewById(R.id.rgThemes);
        String currentTheme = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).getString("theme", "Pixel Claro");
        if ("Pixel Claro".equals(currentTheme)) {
            rgThemes.check(R.id.rbPixelClaro);
        } else {
            rgThemes.check(R.id.rbPixelOscuro);
        }

        rgThemes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPixelClaro) {
                applyTheme("Pixel Claro");
            } else if (checkedId == R.id.rbPixelOscuro) {
                applyTheme("Pixel Oscuro");
            }
        });

        v.findViewById(R.id.btnCheckUpdates).setOnClickListener(v1 -> {
            Toast.makeText(this, "Comprobando actualizaciones...", Toast.LENGTH_SHORT).show();
            checkUpdatesFromGitHubManual();
        });
        
        v.findViewById(R.id.btnLogout).setOnClickListener(v1 -> {
            dialog.dismiss();
            logout();
        });

        v.findViewById(R.id.btnDismissSettings).setOnClickListener(v1 -> dialog.dismiss());

        dialog.show();
    }

    private void checkUpdatesFromGitHubManual() {
        String repoUrl = "https://api.github.com/repos/KevshuppD/Diario_alikevin/releases/latest";
        new OkHttpClient().newCall(new Request.Builder().url(repoUrl).build()).enqueue(new Callback() {
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                if (r.isSuccessful() && r.body() != null) {
                    try {
                        String body = r.body().string();
                        JSONObject j = new JSONObject(body);
                        String latestTag = j.getString("tag_name");
                        String currentVersion = BuildConfig.VERSION_NAME;

                        if (isNewerVersion(currentVersion, latestTag.replace("v", ""))) {
                            String url = null;
                            org.json.JSONArray assets = j.getJSONArray("assets");
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                if (asset.getString("name").endsWith(".apk")) {
                                    url = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                            if (url != null) {
                                String finalUrl = url;
                                runOnUiThread(() -> showUpdateDialog(finalUrl));
                            }
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "La aplicación está actualizada", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al comprobar actualizaciones", Toast.LENGTH_SHORT).show());
                    }
                }
            }
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Fallo en la conexión", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showThemeMenu(View view) {
        PopupMenu p = new PopupMenu(this, view);
        p.getMenu().add("Pixel Claro"); p.getMenu().add("Pixel Oscuro"); p.getMenu().add("Cerrar Sesión");
        p.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Cerrar Sesión")) logout();
            else applyTheme(item.getTitle().toString());
            return true;
        });
        p.show();
    }

    private void applyTheme(String theme) {
        adapter.setTheme(theme);

        int bg = theme.equals("Pixel Oscuro") ? Color.BLACK : Color.parseColor("#F5F5F5");
        int tb = theme.equals("Pixel Oscuro") ? Color.parseColor("#1F1F1F") : Color.parseColor("#5D2E7A");
        int inputBg = theme.equals("Pixel Oscuro") ? Color.parseColor("#2A1E2E") : Color.parseColor("#91465F");
        int inputFieldBg = theme.equals("Pixel Oscuro") ? Color.parseColor("#3A2E4A") : Color.parseColor("#F3E5AB");
        int inputTextColor = theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511");

        mainLayout.setBackgroundColor(bg);
        toolbar.setBackgroundColor(tb);
        inputContainer.setBackgroundColor(inputBg);

        // Ajuste barra de estado y navegación según el tema (morado oscuro a la app)
        Window window = getWindow();
        window.setStatusBarColor(tb);
        int navColor = theme.equals("Pixel Oscuro") ? Color.parseColor("#3A1F40") : Color.parseColor("#5D2E7A");
        window.setNavigationBarColor(navColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (theme.equals("Pixel Oscuro")) {
                // barras oscuras -> iconos blancos (modo oscuro)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                // barras moradas claras -> iconos claros para mejor contraste
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        // Forzar color de iconos de navbar de la app (botones de barra inferior e iconos de acción)
        btnSettings.setColorFilter(Color.WHITE);
        btnProfile.setColorFilter(Color.WHITE);
        btnAlbum.setColorFilter(Color.WHITE);
        btnCalendar.setColorFilter(Color.WHITE);
        btnFilterDate.setColorFilter(Color.WHITE);
        btnExpand.setColorFilter(Color.WHITE);
        btnSend.setColorFilter(Color.WHITE);

        // Ajustes de entrada para color de texto/hint
        etMessage.setBackgroundColor(inputFieldBg);
        etMessage.setTextColor(inputTextColor);
        etMessage.setHintTextColor(theme.equals("Pixel Oscuro") ? Color.LTGRAY : Color.parseColor("#8B4513"));

        // Botones de toolbar y send
        btnExpand.setColorFilter(theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511"));
        btnSend.setColorFilter(theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511"));
    }

    private void showProfileDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null);

        b.setView(v);
        ivDialogProfile = v.findViewById(R.id.ivProfileImage);
        EditText etProfileName = v.findViewById(R.id.etProfileName);
        TextView tvCurrentUserName = v.findViewById(R.id.tvCurrentUserName);
        Button btnSaveProfile = v.findViewById(R.id.btnSaveProfile);
        Button btnLogoutProfile = v.findViewById(R.id.btnLogoutProfile);

        tvCurrentUserName.setVisibility(View.VISIBLE);
        tvCurrentUserName.setText("Usuario: " + currentUserName);

        etProfileName.setText("");
        etProfileName.setHint("Ej. Kevin");
        etProfileName.setVisibility(View.GONE); // no mostrar campo de edición para evitar duplicar el nombre

        TextView tvTogetherTime = v.findViewById(R.id.tvTogetherTime);
        tvTogetherTime.setText(calcRelationshipTime(2022, 1, 19));

        if (currentUserImageUri != null) Glide.with(this).load(currentUserImageUri).circleCrop().into(ivDialogProfile);
        ivDialogProfile.setOnClickListener(v1 -> pickImage(PICK_IMAGE_PROFILE));

        btnSaveProfile.setOnClickListener(v1 -> {
            String newName = etProfileName.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = currentUserName; // no cambió nombre, usar actual
            }
            currentUserName = newName;
            tvCurrentUserName.setText("Usuario: " + newName);
            SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
            prefs.edit().putString("userName", newName).putString("userImage", currentUserImageUri).apply();
            db.collection("users").document(currentUserId)
                    .update("userName", newName, "profileImageUrl", currentUserImageUri)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show());
        });

        btnLogoutProfile.setOnClickListener(v1 -> logout());

        AlertDialog dialog = b.create();
        dialog.show();
    }

    private void pickImage(int code) {
        currentCropType = code;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        if (code == PICK_IMAGE_ALBUM) {
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(Intent.createChooser(i, "Selecciona imágenes del Diario"), code);
    }

    private void startCrop(Uri uri) {
        String name = "crop_" + System.currentTimeMillis() + ".jpg";
        UCrop.Options opt = new UCrop.Options();
        opt.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        opt.setFreeStyleCropEnabled(true);
        opt.setToolbarColor(Color.parseColor("#5D2E7A"));
        opt.setStatusBarColor(Color.parseColor("#2D1444"));
        opt.setToolbarWidgetColor(Color.WHITE);
        UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), name)))
                .withOptions(opt)
                .start(this);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null) {
            if (req == PICK_IMAGE_PROFILE || req == PICK_IMAGE_CARTA) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) startCrop(selectedImage);
            } else if (req == PICK_IMAGE_ALBUM) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        if (imageUri != null) uploadToCloudinary(imageUri, PICK_IMAGE_ALBUM);
                    }
                } else {
                    Uri imageUri = data.getData();
                    if (imageUri != null) uploadToCloudinary(imageUri, PICK_IMAGE_ALBUM);
                }
            } else if (req == UCrop.REQUEST_CROP) {
                Uri r = UCrop.getOutput(data);
                if (r != null) uploadToCloudinary(r, currentCropType);
            }
        }
    }

    private void uploadToCloudinary(Uri uri, int code) {
        // Mostrar un pequeño brindis para avisar que inició la subida
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        
        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override public void onStart(String id) {}
            @Override public void onProgress(String id, long b, long t) {}
            @Override public void onSuccess(String id, Map res) {
                String url = (String) res.get("secure_url");
                Log.d("DIARIO_DEBUG", "Cloudinary Success: " + url + " Code: " + code);
                runOnUiThread(() -> {
                    if (code == PICK_IMAGE_PROFILE) { 
                        currentUserImageUri = url; 
                        db.collection("users").document(currentUserId).update("profileImageUrl", url); 
                        if (ivDialogProfile != null) Glide.with(MainActivity.this).load(url).circleCrop().into(ivDialogProfile); 
                    }
                    else if (code == PICK_IMAGE_CARTA) { 
                        currentSelectedCartaImageUrl = url; 
                        if (ivSelectedCartaImage != null && imageCartaContainer != null) { 
                            imageCartaContainer.setVisibility(View.VISIBLE); 
                            Glide.with(MainActivity.this).load(url).into(ivSelectedCartaImage); 
                        } 
                    }
                    else if (code == PICK_IMAGE_ALBUM) { 
                        currentAlbumImages.add(url); 
                        updateAlbumPreview(); 
                    }
                });
            }
            @Override public void onError(String id, ErrorInfo e) { 
                Log.e("DIARIO_DEBUG", "Cloudinary Error: " + e.getDescription());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error subida: " + e.getDescription(), Toast.LENGTH_LONG).show()); 
            }
            @Override public void onReschedule(String id, ErrorInfo e) {}
        }).dispatch();
    }

    private void applySpan(EditText et, Class<?> spanClass, Object newSpan) {
        int s = et.getSelectionStart(), e = et.getSelectionEnd();
        if (s == e) { Toast.makeText(this, "Selecciona texto primero", Toast.LENGTH_SHORT).show(); return; }
        int start = Math.min(s, e);
        int end = Math.max(s, e);
        Spannable ssb = et.getText();
        
        Object[] existingSpans = ssb.getSpans(start, end, spanClass);
        boolean found = false;
        
        for (Object existingSpan : existingSpans) {
            if (existingSpan instanceof StyleSpan && newSpan instanceof StyleSpan) {
                if (((StyleSpan) existingSpan).getStyle() == ((StyleSpan) newSpan).getStyle()) {
                    ssb.removeSpan(existingSpan);
                    found = true;
                }
            } else {
                ssb.removeSpan(existingSpan);
                found = true;
            }
        }
        
        if (!found) {
            ssb.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        et.setText(ssb);
        et.setSelection(end);
    }

    private void showEditDialog(@Nullable Message edit) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null);
        b.setView(v); EditText et = v.findViewById(R.id.etDialogMessage);
        ivSelectedCartaImage = v.findViewById(R.id.ivSelectedImage); imageCartaContainer = v.findViewById(R.id.imageContainer);
        if (edit != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                et.setText(Html.fromHtml(edit.getContent(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                et.setText(Html.fromHtml(edit.getContent()));
            }
            currentSelectedCartaImageUrl = edit.getImageUrl();
            if (currentSelectedCartaImageUrl != null) {
                imageCartaContainer.setVisibility(View.VISIBLE);
                Glide.with(this).load(currentSelectedCartaImageUrl).into(ivSelectedCartaImage);
            }
        } else {
            currentSelectedCartaImageUrl = null;
            imageCartaContainer.setVisibility(View.GONE);
        }
        v.findViewById(R.id.btnBold).setOnClickListener(v1 -> applySpan(et, StyleSpan.class, new StyleSpan(Typeface.BOLD)));
        v.findViewById(R.id.btnItalic).setOnClickListener(v1 -> applySpan(et, StyleSpan.class, new StyleSpan(Typeface.ITALIC)));
        v.findViewById(R.id.btnColor).setOnClickListener(v1 -> { int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA}; String[] names = {"Rojo", "Azul", "Verde", "Rosa"}; new AlertDialog.Builder(this).setItems(names, (d, w) -> applySpan(et, ForegroundColorSpan.class, new ForegroundColorSpan(colors[w]))).show(); });
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> pickImage(PICK_IMAGE_CARTA));
        v.findViewById(R.id.btnRemoveImage).setOnClickListener(v1 -> { currentSelectedCartaImageUrl = null; imageCartaContainer.setVisibility(View.GONE); });
        
        final AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String html;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                html = Html.toHtml(et.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            } else {
                html = Html.toHtml(et.getText());
            }
            if (edit == null) { Message m = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, html, new ArrayList<>(), System.currentTimeMillis(), false); m.setImageUrl(currentSelectedCartaImageUrl); db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> sendNotificationV1("Te han enviado una carta 💌")); }
            else { edit.setContent(html); edit.setImageUrl(currentSelectedCartaImageUrl); db.collection("messages").document(edit.getMessageId()).set(edit); }
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showAlbumOptionsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_album_options, null);
        b.setView(v);
        
        final AlertDialog dialog = b.create();
        
        v.findViewById(R.id.btnOptionAdd).setOnClickListener(v1 -> {
            dialog.dismiss();
            showAlbumDialog();
        });
        
        v.findViewById(R.id.btnOptionView).setOnClickListener(v1 -> {
            dialog.dismiss();
            showSharedAlbumDialog();
        });
        
        v.findViewById(R.id.btnOptionCancel).setOnClickListener(v1 -> dialog.dismiss());
        
        dialog.show();
    }

    private void showSharedAlbumDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_shared_album, null);
        b.setView(v);
        
        RecyclerView rv = v.findViewById(R.id.rvAlbumPhotos);
        rv.setLayoutManager(new LinearLayoutManager(this)); // Cambiado a vertical para el nuevo feed

        final AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCloseAlbum).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnViewAll).setVisibility(View.GONE); // Ya no es necesario si mostramos todo el feed

        // Para el álbum (mensajes filtrados)
        db.collection("messages").whereEqualTo("partnerId", currentCoupleId).orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error cargando álbum", Toast.LENGTH_SHORT).show();
                return;
            }
            // Actualizar widget después de cualquier cambio en mensajes
            Intent wIntent = new Intent(this, LastMessageWidget.class);
            wIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] wIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
            wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
            sendBroadcast(wIntent);

            if (querySnapshot != null) {
                List<Message> albumMessages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String content = doc.getString("content");
                    if (content != null && content.startsWith("[ALBUM]")) {
                        albumMessages.add(doc.toObject(Message.class));
                    }
                }
                rv.setAdapter(new AlbumFeedAdapter(albumMessages));
            }
        });

        dialog.show();
    }

    private void showEditAlbumDialog(Message m) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null);
        b.setView(v); 
        ((TextView)v.findViewById(R.id.tvDialogTitle)).setText("Editar Recuerdo");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        
        EditText et = v.findViewById(R.id.etDialogMessage);
        String currentDesc = m.getContent().replace("[ALBUM] ", "");
        et.setText(currentDesc);
        et.setHint("Descripción del momento");

        imageCartaContainer = v.findViewById(R.id.imageContainer);
        rvAlbumPreview = v.findViewById(R.id.rvAlbumPreview);
        rvAlbumPreview.setVisibility(View.VISIBLE);
        imageCartaContainer.setVisibility(View.GONE);

        currentAlbumImages.clear();
        if (m.getImageUrls() != null) currentAlbumImages.addAll(m.getImageUrls());
        
        rvAlbumPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        updateAlbumPreview();

        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> pickImage(PICK_IMAGE_ALBUM));
        
        // Botón para limpiar fotos (ahora es "Limpiar todas")
        v.findViewById(R.id.btnRemoveImage).setOnClickListener(v1 -> {
            new AlertDialog.Builder(this, R.style.PixelAlertDialog)
                .setTitle("Limpiar fotos")
                .setMessage("¿Quieres quitar todas las fotos de este recuerdo?")
                .setPositiveButton("Sí", (d, w) -> {
                    currentAlbumImages.clear();
                    updateAlbumPreview();
                })
                .setNegativeButton("No", null)
                .show();
        });

        final AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String c = et.getText().toString().trim();
            m.setContent("[ALBUM] " + c);
            m.setImageUrls(new ArrayList<>(currentAlbumImages));
            db.collection("messages").document(m.getMessageId()).set(m)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Recuerdo actualizado", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });
        dialog.show();
    }

    private class AlbumFeedAdapter extends RecyclerView.Adapter<AlbumFeedAdapter.ViewHolder> {
        private List<Message> items;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public AlbumFeedAdapter(List<Message> items) { this.items = items; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment_feed, parent, false);
            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message m = items.get(position);
            holder.tvUser.setText("Por: " + m.getAuthorName());
            holder.tvDate.setText(sdf.format(new Date(m.getTimestamp())));
            String desc = m.getContent().replace("[ALBUM] ", "");
            holder.tvDesc.setText(desc.isEmpty() ? "Momento compartido" : desc);
            
            holder.rvPhotos.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            holder.rvPhotos.setAdapter(new MessageAdapter.AlbumPhotosAdapter(m.getImageUrls()));

            // Al hacer clic normal, abrimos el modo edición/visualización
            holder.itemView.setOnClickListener(v -> {
                if (m.getAuthorId().equals(currentUserId)) {
                    showEditAlbumDialog(m);
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Solo el autor puede editar este recuerdo", Toast.LENGTH_SHORT).show();
                }
            });

            // Acción de Borrar (Menú contextual idéntico a las cartas)
            holder.itemView.setOnLongClickListener(v -> {
                if (m.getAuthorId().equals(currentUserId)) {
                    PopupMenu popup = new PopupMenu(holder.itemView.getContext(), v);
                    popup.getMenu().add("Editar Recuerdo");
                    popup.getMenu().add("Borrar Recuerdo");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Editar Recuerdo")) {
                            showEditAlbumDialog(m);
                        } else if (item.getTitle().equals("Borrar Recuerdo")) {
                            new AlertDialog.Builder(holder.itemView.getContext(), R.style.PixelAlertDialog)
                                .setTitle("Borrar")
                                .setMessage("¿Seguro que deseas eliminar este recuerdo?")
                                .setPositiveButton("Sí", (d, w) -> {
                                    db.collection("messages").document(m.getMessageId()).delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(holder.itemView.getContext(), "Recuerdo eliminado", Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("No", null)
                                .show();
                        }
                        return true;
                    });
                    popup.show();
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Solo puedes gestionar tus propios momentos", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvDate, tvDesc;
            RecyclerView rvPhotos;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUser = itemView.findViewById(R.id.tvMomentUser);
                tvDate = itemView.findViewById(R.id.tvMomentDate);
                tvDesc = itemView.findViewById(R.id.tvMomentDescription);
                rvPhotos = itemView.findViewById(R.id.rvMomentPhotos);
            }
        }
    }

    private void showAlbumDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null);
        b.setView(v); ((TextView)v.findViewById(R.id.tvDialogTitle)).setText("Nuestro Álbum");
        EditText et = v.findViewById(R.id.etDialogMessage); et.setHint("¿Qué hacíamos hoy?");
        v.findViewById(R.id.formatToolbar).setVisibility(View.GONE);
        ivSelectedCartaImage = v.findViewById(R.id.ivSelectedImage); imageCartaContainer = v.findViewById(R.id.imageContainer);
        rvAlbumPreview = v.findViewById(R.id.rvAlbumPreview); rvAlbumPreview.setVisibility(View.VISIBLE);
        currentAlbumImages.clear(); rvAlbumPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        v.findViewById(R.id.btnAddImage).setOnClickListener(v1 -> pickImage(PICK_IMAGE_ALBUM));

        v.findViewById(R.id.btnRemoveImage).setOnClickListener(v1 -> {
            if (!currentAlbumImages.isEmpty()) {
                currentAlbumImages.remove(currentAlbumImages.size() - 1);
                updateAlbumPreview();
            }
        });

        final AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCancel).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnSave).setOnClickListener(v1 -> {
            String c = et.getText().toString().trim(); 
            if (c.isEmpty() && currentAlbumImages.isEmpty()) {
                Toast.makeText(this, "Agrega texto o al menos una foto", Toast.LENGTH_SHORT).show();
                return;
            }
            // Guardar con [ALBUM] al inicio para que showSharedAlbumDialog lo reconozca
            Message m = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, "[ALBUM] " + c, new ArrayList<>(currentAlbumImages), System.currentTimeMillis(), false);
            db.collection("messages").document(m.getMessageId()).set(m)
                .addOnSuccessListener(aVoid -> {
                    sendNotificationV1("Se ha añadido un nuevo momento al álbum 📸");
                    Toast.makeText(this, "Momento guardado en el álbum", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar el momento", Toast.LENGTH_SHORT).show());
        });
        dialog.show();
    }

    private void updateAlbumPreview() {
        if (rvAlbumPreview != null) {
            rvAlbumPreview.setAdapter(new AlbumPreviewAdapter(currentAlbumImages));
        }
    }

    private class AlbumPreviewAdapter extends RecyclerView.Adapter<AlbumPreviewAdapter.PreviewViewHolder> {
        private List<String> photos;
        public AlbumPreviewAdapter(List<String> photos) { this.photos = photos; }
        @NonNull @Override public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_preview, parent, false);
            return new PreviewViewHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
            String url = photos.get(position);
            ImageView iv = holder.itemView.findViewById(R.id.ivPreviewPhoto);
            Glide.with(holder.itemView.getContext()).load(url).into(iv);
            
            TextView tv = holder.itemView.findViewById(R.id.tvRemovePhoto);
            tv.setOnClickListener(v -> {
                photos.remove(position);
                notifyDataSetChanged();
            });
        }
        @Override public int getItemCount() { return photos.size(); }
        class PreviewViewHolder extends RecyclerView.ViewHolder {
            public PreviewViewHolder(@NonNull View itemView) { super(itemView); }
        }
    }

    @Override public void onMessageClick(View v, Message msg) {
        if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) {
            showAlbumDetail(msg);
        } else {
            showMessageDetail(msg);
        }
    }
    @Override public void onMessageLongClick(View v, Message msg) { 
        if (msg.getAuthorId().equals(currentUserId)) {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Editar");
            popup.getMenu().add("Borrar");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Editar")) showEditDialog(msg);
                else if (item.getTitle().equals("Borrar")) new AlertDialog.Builder(this).setTitle("Borrar").setPositiveButton("Sí", (d, w) -> deleteMessage(msg)).show();
                return true;
            });
            popup.show();
        }
    }
    @Override public void onDeleteClick(Message m) { deleteMessage(m); }

    private void showAlbumDetail(Message msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_date_filter, null);
        b.setView(v);
        ((TextView)v.findViewById(R.id.tvFilterTitle)).setText("Momento Especial");
        String date = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES")).format(new Date(msg.getTimestamp()));
        ((TextView)v.findViewById(R.id.tvFilterSubtitle)).setText(date + "\n" + msg.getContent().replace("[ALBUM] ", ""));
        RecyclerView rv = v.findViewById(R.id.rvAvailableDates);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(new MessageAdapter.AlbumPhotosAdapter(msg.getImageUrls()));

        final AlertDialog dialog = b.create();
        v.findViewById(R.id.btnCancelFilter).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.btnClearFilter).setVisibility(View.GONE);
        dialog.show();
    }

    private void showMessageDetail(Message msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_message_detail, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvMessageDetailTitle);
        TextView tvContent = view.findViewById(R.id.tvMessageDetailContent);
        ImageView ivImage = view.findViewById(R.id.ivMessageDetailImage);
        Button btnClose = view.findViewById(R.id.btnMessageDetailClose);

        tvTitle.setText("Carta de " + msg.getAuthorName());
        tvContent.setText(Html.fromHtml(msg.getContent(), Html.FROM_HTML_MODE_COMPACT));

        if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
            ivImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(msg.getImageUrl()).into(ivImage);
        } else {
            ivImage.setVisibility(View.GONE);
        }

        final AlertDialog dialog = builder.create();
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String calcRelationshipTime(int yearStart, int monthStart, int dayStart) {
        Calendar start = Calendar.getInstance();
        start.set(yearStart, monthStart - 1, dayStart, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();

        int years = now.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        int months = now.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        int days = now.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            months -= 1;
            Calendar prevMonth = (Calendar) now.clone();
            prevMonth.add(Calendar.MONTH, -1);
            days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years -= 1;
            months += 12;
        }

        return String.format(Locale.getDefault(), "Juntos: %d año(s), %d mes(es), %d día(s)", years, months, days);
    }

    private void logout() { 
        getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().clear().apply(); 
        
        // Avisar al widget que se cerró sesión
        Intent intent = new Intent(this, LastMessageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        startActivity(new Intent(this, LoginActivity.class)); 
        finish(); 
    }

    private void checkUpdatesFromGitHub() {
        String repoUrl = "https://api.github.com/repos/KevshuppD/Diario_alikevin/releases/latest";
        Log.d("GITHUB_UPDATE", "Checking for updates at: " + repoUrl);
        new OkHttpClient().newCall(new Request.Builder().url(repoUrl).build()).enqueue(new Callback() {
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                if (r.isSuccessful() && r.body() != null) {
                    try {
                        String body = r.body().string();
                        Log.d("GITHUB_UPDATE", "Response success: " + body);
                        JSONObject j = new JSONObject(body);
                        String latestTag = j.getString("tag_name");
                        String currentVersion = BuildConfig.VERSION_NAME;

                        Log.d("GITHUB_UPDATE", "Comparing: Current[" + currentVersion + "] with Latest[" + latestTag + "]");

                        if (isNewerVersion(currentVersion, latestTag.replace("v", ""))) {
                            String url = null;
                            org.json.JSONArray assets = j.getJSONArray("assets");
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                if (asset.getString("name").endsWith(".apk")) {
                                    url = asset.getString("browser_download_url");
                                    break;
                                }
                            }

                            if (url != null) {
                                String finalUrl = url;
                                runOnUiThread(() -> {
                                    Log.d("GITHUB_UPDATE", "Showing update dialog for: " + finalUrl);
                                    showUpdateDialog(finalUrl);
                                });
                            }
                        } else {
                            Log.d("GITHUB_UPDATE", "App is up to date");
                        }
                    } catch (Exception e) {
                        Log.e("GITHUB_UPDATE", "Error parsing response", e);
                    }
                } else {
                    Log.e("GITHUB_UPDATE", "Response not successful: " + r.code() + " " + r.message());
                }
            }
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                Log.e("GITHUB_UPDATE", "Request failed", e);
            }
        });
    }

    private boolean isNewerVersion(String current, String latest) {
        try {
            // Eliminar cualquier prefijo 'v' si llegó a pasar por error
            current = current.replace("v", "");
            latest = latest.replace("v", "");
            
            String[] currParts = current.split("\\.");
            String[] lateParts = latest.split("\\.");
            int length = Math.max(currParts.length, lateParts.length);
            for (int i = 0; i < length; i++) {
                int curr = i < currParts.length ? Integer.parseInt(currParts[i].replaceAll("[^0-9]", "")) : 0;
                int late = i < lateParts.length ? Integer.parseInt(lateParts[i].replaceAll("[^0-9]", "")) : 0;
                if (late > curr) return true;
                if (curr > late) return false;
            }
        } catch (Exception e) {
            Log.e("GITHUB_UPDATE", "Error comparing versions", e);
            return !current.equals(latest);
        }
        return false;
    }

    private void showUpdateDialog(String url) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Actualización disponible")
                    .setMessage("Una nueva versión está disponible en GitHub. ¿Deseas descargarla?")
                    .setPositiveButton("Descargar", (d, w) -> {
                        downloadProgressContainer.setVisibility(View.VISIBLE);
                        downloadGitHubRelease(url);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void downloadGitHubRelease(String url) {
        if (downloadManager == null) {
            Toast.makeText(this, "Error al iniciar descarga", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Descargando actualización");
        request.setDescription("Descargando APK desde GitHub Releases");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);
        request.setMimeType("application/vnd.android.package-archive");
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "DiarioKevinali_update.apk");

        latestDownloadId = downloadManager.enqueue(request);
        hasShownDownloadCompleteDialog = false;
        downloadProgressBar.setProgress(0);

        if (updateProgressRunnable != null) {
            updateHandler.removeCallbacks(updateProgressRunnable);
        }

        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(latestDownloadId);
                try (Cursor cursor = downloadManager.query(q)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        
                        if (bytesTotal > 0) {
                            int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            downloadProgressBar.setProgress(progress);
                        }

                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloadProgressContainer.setVisibility(View.GONE);
                            updateProgressRunnable = null;
                            
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                checkDownloadedUpdateStatus();
                            } else {
                                Toast.makeText(MainActivity.this, "La descarga falló", Toast.LENGTH_SHORT).show();
                            }
                            return; // Detener actualizaciones
                        }
                    }
                }
                updateHandler.postDelayed(this, 500);
            }
        };
        updateHandler.post(updateProgressRunnable);
    }

    private void checkDownloadedUpdateStatus() {
        if (latestDownloadId == -1 || hasShownDownloadCompleteDialog || downloadManager == null) return;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(latestDownloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Uri uri = downloadManager.getUriForDownloadedFile(latestDownloadId);
                    if (uri != null) {
                        showInstallNotification(uri);
                        hasShownDownloadCompleteDialog = true;
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {
                    Toast.makeText(this, "Error descargando actualización", Toast.LENGTH_LONG).show();
                    hasShownDownloadCompleteDialog = true;
                }
            }
        }
    }
}
