package calendario.kevshupp.diariokevinali;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.yalantis.ucrop.UCrop;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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
    private View downloadProgressContainer;
    private ProgressBar downloadProgressBar;

    private String currentCoupleId = "vínculo_único_123", currentUserId, currentUserName, currentUserImageUri;
    private int currentCropType = -1;

    private UpdateManager updateManager;
    private MessageEditor messageEditor;
    private AlbumManager albumManager;

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
        
        updateManager = new UpdateManager(this);
        messageEditor = new MessageEditor(this, currentCoupleId, currentUserId, currentUserName, currentUserImageUri);
        albumManager = new AlbumManager(this, currentCoupleId, currentUserId, currentUserName, currentUserImageUri);

        initViews();
        setupRecyclerView();
        listenMessagesFromFirestore();
        listenUserInfo();
        checkAndRequestPermissions();
        setupFirebaseMessaging();

        btnSend.setOnClickListener(v -> sendMessage());
        btnExpand.setOnClickListener(v -> messageEditor.showEditDialog(null, new MessageEditor.EditorCallback() {
            @Override public void onSave(Message m) { db.collection("messages").document(m.getMessageId()).set(m).addOnSuccessListener(aVoid -> sendNotificationV1("Te han enviado una carta 💌")); }
            @Override public void onPickImage(int code) { pickImage(PICK_IMAGE_CARTA); }
        }));
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnProfile.setOnClickListener(v -> showProfileDialog());
        btnFilterDate.setOnClickListener(v -> showDatePicker());
        btnCalendar.setOnClickListener(v -> showCalendarDialog());
        btnAlbum.setOnClickListener(v -> albumManager.showAlbumOptions(new AlbumManager.AlbumCallback() {
            @Override public void onPickImage() { pickImage(PICK_IMAGE_ALBUM); }
            @Override public void onMomentSaved() { sendNotificationV1("Se ha añadido un nuevo momento al álbum 📸"); }
        }));
        
        applyTheme(prefs.getString("theme", "Pixel Claro"));
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override public void onUpdateAvailable(String url) { showUpdateDialog(url); }
            @Override public void onNoUpdate() {}
            @Override public void onDownloadProgress(int p) { runOnUiThread(() -> downloadProgressBar.setProgress(p)); }
            @Override public void onDownloadComplete() { runOnUiThread(() -> { downloadProgressContainer.setVisibility(View.GONE); updateManager.installApk(); }); }
        });
    }

    private void showUpdateDialog(String url) {
        new AlertDialog.Builder(this)
                .setTitle("Actualización disponible")
                .setMessage("Una nueva versión está disponible en GitHub. ¿Deseas descargarla?")
                .setPositiveButton("Descargar", (d, w) -> {
                    downloadProgressContainer.setVisibility(View.VISIBLE);
                    updateManager.downloadUpdate(url, new UpdateManager.UpdateCallback() {
                        @Override public void onUpdateAvailable(String u) {}
                        @Override public void onNoUpdate() {}
                        @Override public void onDownloadProgress(int p) { runOnUiThread(() -> downloadProgressBar.setProgress(p)); }
                        @Override public void onDownloadComplete() { runOnUiThread(() -> { downloadProgressContainer.setVisibility(View.GONE); updateManager.installApk(); }); }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
        downloadProgressContainer = findViewById(R.id.downloadProgressContainer);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
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
                    if (m.getContent() == null || !m.getContent().startsWith("[ALBUM]")) {
                        messages.add(m);
                    }
                }
                adapter.notifyDataSetChanged();
                updateWidget();
            }
        });
    }

    private void updateWidget() {
        Intent wIntent = new Intent(this, LastMessageWidget.class);
        wIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] wIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
        wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
        sendBroadcast(wIntent);
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

    private void setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().subscribeToTopic("diario_" + currentCoupleId);
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private void sendMessage() {
        String txt = etMessage.getText().toString().trim(); if (txt.isEmpty()) return;
        Message msg = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, txt, new ArrayList<>(), System.currentTimeMillis(), false);
        db.collection("messages").document(msg.getMessageId()).set(msg).addOnSuccessListener(aVoid -> sendNotificationV1(txt));
        etMessage.setText("");
    }

    private void sendNotificationV1(String messageText) {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("service-account.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(is).createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                
                String projectId = "diario-pareja-a2d35"; 
                String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

                JSONObject jsonBody = new JSONObject();
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                JSONObject data = new JSONObject();

                notification.put("title", "Nuevo mensaje de " + currentUserName);
                notification.put("body", messageText);
                data.put("authorId", currentUserId);

                message.put("topic", "diario_" + currentCoupleId);
                message.put("notification", notification);
                message.put("data", data);
                jsonBody.put("message", message);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                new OkHttpClient().newCall(new Request.Builder().url(url).post(body).addHeader("Authorization", "Bearer " + token).build()).execute();
            } catch (Exception e) { Log.e("FCM_V1", "Error: " + e.getMessage()); }
        }).start();
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
        TextView tvDays = view.findViewById(R.id.tvDaysWithEvents);
        if (calendarListener != null) calendarListener.remove();
        calendarListener = db.collection("calendar").whereEqualTo("partnerId", currentCoupleId).addSnapshotListener((snaps, e) -> {
            if (snaps != null) { 
                all.clear(); 
                Set<String> uniqueDays = new HashSet<>();
                for (QueryDocumentSnapshot d : snaps) {
                    CalendarEvent ev = d.toObject(CalendarEvent.class);
                    all.add(ev);
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(ev.getDate());
                    uniqueDays.add(c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1));
                }
                if (tvDays != null) {
                    if (all.isEmpty()) tvDays.setText("No hay planes agendados");
                    else tvDays.setText("Planes en: " + String.join(", ", uniqueDays));
                }
                updateDayList(all, day, sTs[0], adp);
                updateWidget();
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
        for (CalendarEvent e : all) {
            long ets = e.getDate(); String rec = e.getRecurrence() != null ? e.getRecurrence() : "NONE";
            if (isSameDay(ets, ts) || (rec.equals("DAILY") && ets <= ts) || (rec.equals("WEEKLY") && ets <= ts && isSameDayOfWeek(ets, ts)) || (rec.equals("MONTHLY") && ets <= ts && isSameDayOfMonth(ets, ts)) || (rec.equals("YEARLY") && ets <= ts && isSameDayAndMonth(ets, ts))) day.add(e);
        }
        day.sort((e1, e2) -> Long.compare(e1.getDate(), e2.getDate())); adp.notifyDataSetChanged();
    }

    private boolean isSameDay(long t1, long t2) { return normalizeDate(t1) == normalizeDate(t2); }
    private boolean isSameDayOfWeek(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_WEEK) == c2.get(Calendar.DAY_OF_WEEK); }
    private boolean isSameDayOfMonth(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH); }
    private boolean isSameDayAndMonth(long t1, long t2) { Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1); Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2); return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH); }
    private long normalizeDate(long ts) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }

    private void showAddEventDialog(long ts, @Nullable CalendarEvent edit) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_calendar_event, null);
        b.setView(v);
        EditText et = v.findViewById(R.id.etEventTitle), etDesc = v.findViewById(R.id.etEventDescription);
        Button btnTime = v.findViewById(R.id.btnPickTime);
        Spinner spinner = v.findViewById(R.id.spinnerRecurrence);
        String[] values = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        final Calendar time = Calendar.getInstance();
        if (edit != null) { et.setText(edit.getTitle()); if (etDesc != null) etDesc.setText(edit.getDescription()); time.setTimeInMillis(edit.getDate()); for (int i = 0; i < values.length; i++) if (values[i].equals(edit.getRecurrence())) spinner.setSelection(i); } else { time.setTimeInMillis(ts); time.set(Calendar.HOUR_OF_DAY, 12); time.set(Calendar.MINUTE, 0); }
        btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
        btnTime.setOnClickListener(v1 -> new android.app.TimePickerDialog(this, (v2, h, m) -> { time.set(Calendar.HOUR_OF_DAY, h); time.set(Calendar.MINUTE, m); btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show());
        final AlertDialog d = b.create();
        v.findViewById(R.id.btnSaveEvent).setOnClickListener(v1 -> { String title = et.getText().toString().trim(); if (title.isEmpty()) return; String id = edit != null ? edit.getEventId() : UUID.randomUUID().toString(); CalendarEvent ev = new CalendarEvent(id, title, etDesc != null ? etDesc.getText().toString().trim() : "", time.getTimeInMillis(), currentUserId, currentCoupleId); ev.setRecurrence(values[spinner.getSelectedItemPosition()]); db.collection("calendar").document(id).set(ev); d.dismiss(); });
        v.findViewById(R.id.btnCancelEvent).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    private String calcRelationshipTime(int y, int m, int d) {
        Calendar start = Calendar.getInstance(); start.set(y, m - 1, d); Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - start.get(Calendar.YEAR), months = now.get(Calendar.MONTH) - start.get(Calendar.MONTH), days = now.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);
        if (days < 0) { months--; days += now.getActualMaximum(Calendar.DAY_OF_MONTH); }
        if (months < 0) { years--; months += 12; }
        return String.format(Locale.getDefault(), "Juntos: %d año(s), %d mes(es), %d día(s)", years, months, days);
    }

    private void showProfileDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null);
        b.setView(v);
        ImageView ivProfile = v.findViewById(R.id.ivProfileImage);
        TextView tvName = v.findViewById(R.id.tvCurrentUserName), tvTime = v.findViewById(R.id.tvTogetherTime);
        tvName.setText("Usuario: " + currentUserName); tvTime.setText(calcRelationshipTime(2022, 1, 19));
        if (currentUserImageUri != null) Glide.with(this).load(currentUserImageUri).circleCrop().into(ivProfile);
        ivProfile.setOnClickListener(v1 -> pickImage(PICK_IMAGE_PROFILE));
        v.findViewById(R.id.btnSaveProfile).setOnClickListener(v1 -> { SharedPreferences.Editor e = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit(); e.putString("userImage", currentUserImageUri).apply(); db.collection("users").document(currentUserId).update("profileImageUrl", currentUserImageUri).addOnSuccessListener(aVoid -> Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()); });
        v.findViewById(R.id.btnLogoutProfile).setOnClickListener(v1 -> logout());
        b.create().show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        b.setView(v);
        final AlertDialog dialog = b.create();
        ((TextView)v.findViewById(R.id.tvAppVersion)).setText("Versión actual: " + BuildConfig.VERSION_NAME);
        RadioGroup rg = v.findViewById(R.id.rgThemes);
        String currentTheme = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).getString("theme", "Pixel Claro");
        rg.check("Pixel Claro".equals(currentTheme) ? R.id.rbPixelClaro : R.id.rbPixelOscuro);
        rg.setOnCheckedChangeListener((g, id) -> { String t = id == R.id.rbPixelClaro ? "Pixel Claro" : "Pixel Oscuro"; applyTheme(t); getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().putString("theme", t).apply(); });
        v.findViewById(R.id.btnCheckUpdates).setOnClickListener(v1 -> updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override public void onUpdateAvailable(String url) { showUpdateDialog(url); }
            @Override public void onNoUpdate() { Toast.makeText(MainActivity.this, "La app está actualizada", Toast.LENGTH_SHORT).show(); }
            @Override public void onDownloadProgress(int p) { runOnUiThread(() -> downloadProgressBar.setProgress(p)); }
            @Override public void onDownloadComplete() { runOnUiThread(() -> { downloadProgressContainer.setVisibility(View.GONE); updateManager.installApk(); }); }
        }));
        v.findViewById(R.id.btnLogout).setOnClickListener(v1 -> { dialog.dismiss(); logout(); });
        v.findViewById(R.id.btnDismissSettings).setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void applyTheme(String theme) {
        adapter.setTheme(theme);
        int bg = theme.equals("Pixel Oscuro") ? Color.BLACK : Color.parseColor("#F5F5F5");
        int tb = theme.equals("Pixel Oscuro") ? Color.parseColor("#1F1F1F") : Color.parseColor("#5D2E7A");
        mainLayout.setBackgroundColor(bg); toolbar.setBackgroundColor(tb); inputContainer.setBackgroundColor(theme.equals("Pixel Oscuro") ? Color.parseColor("#2A1E2E") : Color.parseColor("#91465F"));
        Window w = getWindow(); w.setStatusBarColor(tb); w.setNavigationBarColor(theme.equals("Pixel Oscuro") ? Color.parseColor("#3A1F40") : Color.parseColor("#5D2E7A"));
        int c = Color.WHITE; btnSettings.setColorFilter(c); btnProfile.setColorFilter(c); btnAlbum.setColorFilter(c); btnCalendar.setColorFilter(c); btnFilterDate.setColorFilter(c); btnExpand.setColorFilter(c); btnSend.setColorFilter(c);
        etMessage.setBackgroundColor(theme.equals("Pixel Oscuro") ? Color.parseColor("#3A2E4A") : Color.parseColor("#F3E5AB")); etMessage.setTextColor(theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511"));
    }

    private void pickImage(int code) {
        currentCropType = code; Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*");
        if (code == PICK_IMAGE_ALBUM) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "Selecciona imágenes"), code);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null) {
            if (req == PICK_IMAGE_PROFILE) { Uri uri = data.getData(); if (uri != null) startCrop(uri); }
            else if (req == PICK_IMAGE_ALBUM) { if (data.getClipData() != null) { for (int i = 0; i < data.getClipData().getItemCount(); i++) upload(data.getClipData().getItemAt(i).getUri(), PICK_IMAGE_ALBUM); } else upload(data.getData(), PICK_IMAGE_ALBUM); }
            else if (req == PICK_IMAGE_CARTA) upload(data.getData(), PICK_IMAGE_CARTA);
            else if (req == UCrop.REQUEST_CROP) { Uri r = UCrop.getOutput(data); if (r != null && currentCropType == PICK_IMAGE_PROFILE) upload(r, PICK_IMAGE_PROFILE); }
        }
    }

    private void upload(Uri uri, int code) {
        if (uri == null) return;
        Toast.makeText(this, "Subiendo...", Toast.LENGTH_SHORT).show();
        messageEditor.uploadImage(uri, new UploadCallback() {
            @Override public void onStart(String id) {} @Override public void onProgress(String id, long b, long t) {}
            @Override public void onSuccess(String id, Map res) {
                String url = (String) res.get("secure_url");
                runOnUiThread(() -> {
                    if (code == PICK_IMAGE_PROFILE) { currentUserImageUri = url; db.collection("users").document(currentUserId).update("profileImageUrl", url); }
                    else if (code == PICK_IMAGE_CARTA) messageEditor.setImageUrl(url);
                    else if (code == PICK_IMAGE_ALBUM) albumManager.addImageUrl(url);
                });
            }
            @Override public void onError(String id, ErrorInfo e) {} @Override public void onReschedule(String id, ErrorInfo e) {}
        });
    }

    private void startCrop(Uri uri) {
        UCrop.Options opt = new UCrop.Options(); opt.setCompressionFormat(Bitmap.CompressFormat.JPEG); opt.setFreeStyleCropEnabled(true);
        UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg"))).withOptions(opt).start(this);
    }

    private void logout() { getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().clear().apply(); updateWidget(); startActivity(new Intent(this, LoginActivity.class)); finish(); }

    @Override public void onMessageClick(View v, Message msg) { if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) albumManager.showAlbumDetail(msg); else messageEditor.showMessageDetail(msg); }
    @Override public void onMessageLongClick(View v, Message msg) { if (msg.getAuthorId().equals(currentUserId)) { PopupMenu p = new PopupMenu(this, v); p.getMenu().add("Editar"); p.getMenu().add("Borrar"); p.setOnMenuItemClickListener(item -> { if (item.getTitle().equals("Editar")) { if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) albumManager.showEditAlbumDialog(msg); else messageEditor.showEditDialog(msg, new MessageEditor.EditorCallback() { @Override public void onSave(Message m) { db.collection("messages").document(m.getMessageId()).set(m); } @Override public void onPickImage(int c) { pickImage(PICK_IMAGE_CARTA); } }); } else if (item.getTitle().equals("Borrar")) db.collection("messages").document(msg.getMessageId()).delete(); return true; }); p.show(); } }
    @Override public void onDeleteClick(Message m) { db.collection("messages").document(m.getMessageId()).delete(); }
}
