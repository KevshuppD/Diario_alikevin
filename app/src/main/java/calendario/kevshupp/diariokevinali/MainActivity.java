package calendario.kevshupp.diariokevinali;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
    private static final int PICK_IMAGE_RECIPE = 5;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ConstraintLayout mainLayout;
    private MaterialToolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageView ivToolbarHeart;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages;
    private EditText etMessage;
    private ImageButton btnSend, btnExpand, btnMenuMore, btnRecipes, btnCalendar, btnAlbum, btnProfile, btnHome, btnSettings;
    private View inputContainer, inputArea, bottomActionsBar;
    private View previewContainer;
    private FrameLayout fragmentContainer;
    private ImageView ivPreview;
    private ImageButton btnRemovePreview;
    private View navBarPadding;
    private LinearLayout downloadProgressContainer;
    private ProgressBar downloadProgressBar;
    private String selectedImageUrl = null;

    private String currentTheme = "Pixel Claro";
    private String currentCoupleId = "vínculo_único_123", currentUserId, currentUserName, currentUserImageUri;
    private int currentCropType = -1;

    private UpdateManager updateManager;
    private MessageEditor messageEditor;
    private AlbumManager albumManager;
    private RecipeManager recipeManager;

    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener, calendarListener;
    private Calendar selectedFilterDate = null;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        enableImmersiveMode();

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
        recipeManager = new RecipeManager(this, currentCoupleId, currentUserId, currentUserName, () -> pickImage(PICK_IMAGE_RECIPE));
        recipeManager.setTheme(currentTheme);

        initViews();
        setupDynamicMargins();
        setupOfflineStatusListener();
        
        btnMenuMore.setOnClickListener(this::showOverflowMenu);

        setupRecyclerView();
        listenMessagesFromFirestore();
        listenUserInfo();
        checkAndRequestPermissions();
        setupFirebaseMessaging();

        btnSend.setOnClickListener(v -> sendMessage());
        btnRemovePreview.setOnClickListener(v -> {
            selectedImageUrl = null;
            previewContainer.setVisibility(View.GONE);
        });

        // El botón del lápiz ahora abre el editor de cartas completo
        btnExpand.setOnClickListener(v -> messageEditor.showEditDialog(null, new MessageEditor.EditorCallback() {
            @Override
            public void onSave(Message m) {
                db.collection("messages").document(m.getMessageId()).set(m)
                    .addOnSuccessListener(aVoid -> {
                        sendNotificationV1(m.getTitle() != null ? m.getTitle() : "Nueva carta enviada", m.getImageUrl());
                        Toast.makeText(MainActivity.this, "Carta enviada ❤️", Toast.LENGTH_SHORT).show();
                    });
            }

            @Override
            public void onPickImage(int c) {
                pickImage(c);
            }
        }));

        btnRecipes.setOnClickListener(v -> showFragment(RecipeFragment.newInstance(currentCoupleId, currentUserId, currentUserName, currentTheme)));
        
        btnCalendar.setOnClickListener(v -> showFragment(CalendarFragment.newInstance(currentCoupleId, currentUserId, currentTheme)));
        btnAlbum.setOnClickListener(v -> showFragment(AlbumFragment.newInstance(currentCoupleId, currentUserId, currentUserName, currentUserImageUri, currentTheme)));
        btnProfile.setOnClickListener(v -> showFragment(ProfileFragment.newInstance(currentUserId, currentUserName, currentUserImageUri, currentTheme)));
        btnHome.setOnClickListener(v -> {
            fragmentContainer.setVisibility(View.GONE);
            rvMessages.setVisibility(View.VISIBLE);
            inputArea.setVisibility(View.VISIBLE);
            btnMenuMore.setVisibility(View.VISIBLE); // Mostrar filtro en cartas
        });
        btnSettings.setOnClickListener(v -> showFragment(SettingsFragment.newInstance(currentTheme)));

        applyTheme(prefs.getString("theme", "Pixel Claro"));
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override public void onUpdateAvailable(String url) { showUpdateDialog(url); }
            @Override public void onNoUpdate() {}
            @Override public void onDownloadProgress(int p) { runOnUiThread(() -> downloadProgressBar.setProgress(p)); }
            @Override public void onDownloadComplete() { runOnUiThread(() -> { downloadProgressContainer.setVisibility(View.GONE); updateManager.installApk(); }); }
        });
    }

    public void showUpdateDialog(String url) {
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
        btnMenuMore = findViewById(R.id.btnMenuMore);
        btnRecipes = findViewById(R.id.btnRecipes);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnAlbum = findViewById(R.id.btnAlbum);
        btnProfile = findViewById(R.id.btnProfile);
        btnHome = findViewById(R.id.btnHome);
        btnSettings = findViewById(R.id.btnSettings);
        inputArea = findViewById(R.id.inputArea);
        inputContainer = findViewById(R.id.inputContainer);
        bottomActionsBar = findViewById(R.id.bottomActionsBar);
        previewContainer = findViewById(R.id.previewContainer);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        ivPreview = findViewById(R.id.ivPreview);
        btnRemovePreview = findViewById(R.id.btnRemovePreview);
        navBarPadding = findViewById(R.id.navBarPadding);
        downloadProgressContainer = findViewById(R.id.downloadProgressContainer);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
    }

    private void showOverflowMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Filtrar por fecha");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals("Filtrar por fecha")) {
                showDatePicker();
            }
            return true;
        });
        popup.show();
    }

    private void showFragment(androidx.fragment.app.Fragment fragment) {
        btnMenuMore.setVisibility(View.GONE); // Ocultar filtro fuera de cartas
        rvMessages.setVisibility(View.GONE);
        inputArea.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            rvMessages.setVisibility(View.VISIBLE);
            inputArea.setVisibility(View.VISIBLE);
            btnMenuMore.setVisibility(View.VISIBLE); // Volver a mostrar filtro en cartas
            fragmentContainer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    private void setupDynamicMargins() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            
            // Ajustar el padding superior para no tapar la toolbar con la barra de estado
            mainLayout.setPadding(0, statusBarHeight, 0, 0);
            
            navBarPadding.getLayoutParams().height = navBarHeight;
            navBarPadding.requestLayout();
            return insets;
        });
    }

    private void setupOfflineStatusListener() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            updateConnectionUi(false);
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> updateConnectionUi(true));
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> updateConnectionUi(isNetworkAvailable()));
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                runOnUiThread(() -> updateConnectionUi(hasInternetCapability(networkCapabilities)));
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);

        updateConnectionUi(isNetworkAvailable());
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        return hasInternetCapability(caps);
    }

    private boolean hasInternetCapability(NetworkCapabilities caps) {
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void updateConnectionUi(boolean connected) {
        if (connected) {
            ivToolbarHeart.setColorFilter(null);
            tvToolbarTitle.setText("Nuestro Diario ");
        } else {
            ivToolbarHeart.setColorFilter(Color.GRAY);
            tvToolbarTitle.setText("Diario (Sin conexión) ");
        }
    }

    private void enableImmersiveMode() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller == null) return;
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
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
                List<Message> newMessages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Message m = doc.toObject(Message.class);
                    if (m.getContent() == null || !m.getContent().startsWith("[ALBUM]")) {
                        newMessages.add(m);
                    }
                }
                adapter.setMessageList(newMessages);
                // Desplazar al inicio automáticamente al recibir un nuevo mensaje
                if (!newMessages.isEmpty()) {
                    rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(0), 100);
                }
                updateWidget();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
        if (calendarListener != null) {
            calendarListener.remove();
            calendarListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firestoreListener == null) {
            listenMessagesFromFirestore();
        }
        enableImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    @Override
    protected void onDestroy() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        super.onDestroy();
    }

    private void updateWidget() {
        // Actualizar widget pequeño
        Intent wIntent = new Intent(this, LastMessageWidget.class);
        wIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] wIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageWidget.class));
        wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
        sendBroadcast(wIntent);

        // Actualizar widget grande
        Intent wLargeIntent = new Intent(this, LastMessageLargeWidget.class);
        wLargeIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] wLargeIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LastMessageLargeWidget.class));
        wLargeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wLargeIds);
        sendBroadcast(wLargeIntent);
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
        String txt = etMessage.getText().toString().trim(); 
        if (txt.isEmpty() && selectedImageUrl == null) return;
        
        List<String> imgs = new ArrayList<>();
        if (selectedImageUrl != null) imgs.add(selectedImageUrl);
        
        Message msg = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, txt, imgs, System.currentTimeMillis(), false);
        
        // Vibrar al enviar
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(70);
        }

        db.collection("messages").document(msg.getMessageId()).set(msg).addOnSuccessListener(aVoid -> {
            sendNotificationV1(txt, selectedImageUrl);
            selectedImageUrl = null;
            previewContainer.setVisibility(View.GONE);
        });
        etMessage.setText("");
    }

    public void sendNotificationV1(String messageText, String imageUrl) {
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
                notification.put("body", messageText != null && !messageText.isEmpty() ? messageText : "Te han enviado una foto 📸");
                data.put("authorId", currentUserId);
                if (imageUrl != null) data.put("imageUrl", imageUrl);

                message.put("topic", "diario_" + currentCoupleId);
                message.put("notification", notification);
                message.put("data", data);
                jsonBody.put("message", message);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                new OkHttpClient().newCall(new Request.Builder().url(url).post(body).addHeader("Authorization", "Bearer " + token).build()).execute();
            } catch (Exception e) { Log.e("FCM_V1", "Error: " + e.getMessage()); }
        }).start();
    }
    // Simplificado para no romper compatibilidad con llamadas existentes
    public void sendNotificationV1(String messageText) { sendNotificationV1(messageText, null); }

    private void showDatePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date_filter, null);
        builder.setView(view);

        if (currentTheme.equals("Pixel Oscuro")) {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) view.findViewById(R.id.tvFilterTitle)).setTextColor(Color.WHITE);
            ((Button) view.findViewById(R.id.btnClearFilter)).setTextColor(Color.WHITE);
            ((Button) view.findViewById(R.id.btnCancelFilter)).setTextColor(Color.WHITE);
        }

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





    public long normalizeDate(long ts) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }

    public void showAddEventDialog(long ts, @Nullable CalendarEvent edit) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_calendar_event, null);
        b.setView(v);

        Spinner spinner = v.findViewById(R.id.spinnerRecurrence);
        String[] displayValues = {"No repetir", "Diario", "Semanal", "Mensual", "Anual"};
        String[] values = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, displayValues) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTypeface(androidx.core.content.res.ResourcesCompat.getFont(MainActivity.this, R.font.vt323));
                    if ("Pixel Oscuro".equals(currentTheme)) ((TextView) view).setTextColor(Color.WHITE);
                    else ((TextView) view).setTextColor(Color.parseColor("#4A2511"));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTypeface(androidx.core.content.res.ResourcesCompat.getFont(MainActivity.this, R.font.vt323));
                    if ("Pixel Oscuro".equals(currentTheme)) {
                        ((TextView) view).setTextColor(Color.WHITE);
                        view.setBackgroundColor(Color.parseColor("#1A1A2E"));
                    } else {
                        ((TextView) view).setTextColor(Color.parseColor("#4A2511"));
                        view.setBackgroundColor(Color.parseColor("#F3E5AB"));
                    }
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (currentTheme.equals("Pixel Oscuro")) {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
            ((TextView) v.findViewById(R.id.tvAddEventTitle)).setTextColor(Color.WHITE);
            EditText et1 = v.findViewById(R.id.etEventTitle); et1.setTextColor(Color.WHITE); et1.setHintTextColor(Color.LTGRAY); et1.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            EditText et2 = v.findViewById(R.id.etEventDescription); if (et2 != null) { et2.setTextColor(Color.WHITE); et2.setHintTextColor(Color.LTGRAY); et2.setBackgroundResource(R.drawable.bg_message_pixel_dark); }
            ((TextView) ((android.view.ViewGroup)v.findViewById(R.id.llTime)).getChildAt(0)).setTextColor(Color.WHITE);
            ((TextView) ((android.view.ViewGroup)v.findViewById(R.id.llRecurrence)).getChildAt(0)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnPickTime)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnSaveEvent)).setTextColor(Color.WHITE);
            ((Button) v.findViewById(R.id.btnCancelEvent)).setTextColor(Color.WHITE);
            spinner.setBackgroundResource(R.drawable.bg_message_pixel_dark);
            spinner.setPopupBackgroundResource(R.drawable.bg_parchment_pixel_dark);
        }

        EditText et = v.findViewById(R.id.etEventTitle), etDesc = v.findViewById(R.id.etEventDescription);
        Button btnTime = v.findViewById(R.id.btnPickTime);
        final Calendar time = Calendar.getInstance();
        if (edit != null) { 
            et.setText(edit.getTitle()); 
            if (etDesc != null) etDesc.setText(edit.getDescription()); 
            time.setTimeInMillis(edit.getDate()); 
            for (int i = 0; i < values.length; i++) if (values[i].equals(edit.getRecurrence())) spinner.setSelection(i); 
        } else { 
            time.setTimeInMillis(ts); 
            time.set(Calendar.HOUR_OF_DAY, 12); 
            time.set(Calendar.MINUTE, 0); 
        }
        btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
        btnTime.setOnClickListener(v1 -> new android.app.TimePickerDialog(this, (v2, h, m) -> { time.set(Calendar.HOUR_OF_DAY, h); time.set(Calendar.MINUTE, m); btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show());
        final AlertDialog d = b.create();
        v.findViewById(R.id.btnSaveEvent).setOnClickListener(v1 -> { 
            String title = et.getText().toString().trim(); 
            if (title.isEmpty()) return; 
            String id = edit != null ? edit.getEventId() : UUID.randomUUID().toString(); 
            CalendarEvent ev = new CalendarEvent(id, title, etDesc != null ? etDesc.getText().toString().trim() : "", time.getTimeInMillis(), currentUserId, currentCoupleId); 
            ev.setAuthorName(currentUserName);
            int pos = spinner.getSelectedItemPosition();
            ev.setRecurrence(pos >= 0 ? values[pos] : "NONE"); 
            db.collection("calendar").document(id).set(ev); 
            d.dismiss(); 
        });
        v.findViewById(R.id.btnCancelEvent).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }







    public void applyTheme(String theme) {
        applyTheme(theme, null);
    }

    public void applyTheme(String theme, String lightColor) {
        this.currentTheme = theme;
        adapter.setTheme(theme);
        albumManager.setTheme(theme);
        recipeManager.setTheme(theme);
        messageEditor.setTheme(theme);
        int bg, tb, inputBg, etBg, etText, etHint;
        if (theme.equals("Pixel Oscuro")) {
            bg = Color.parseColor("#0D0D2B"); // Azul medianoche profundo
            tb = Color.parseColor("#0A0A1A"); // Negro-morado muy profundo
            inputBg = Color.parseColor("#0A0A1A");
            etBg = Color.parseColor("#1A1A2E");
            etText = Color.WHITE;
            etHint = Color.parseColor("#AAAAAA");
        } else {
            bg = Color.parseColor("#F5F5F5");
            if (lightColor == null) {
                lightColor = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).getString("lightColor", "#4A148C");
            }
            tb = Color.parseColor(lightColor); 
            inputBg = Color.parseColor("#6A1B9A"); 
            
            // Lógica refinada para el fondo del área de entrada basado en el color de barra
            switch (lightColor.toUpperCase()) {
                case "#0D47A1": inputBg = Color.parseColor("#1976D2"); break; // Azul
                case "#1B5E20": inputBg = Color.parseColor("#388E3C"); break; // Verde
                case "#C2185B": inputBg = Color.parseColor("#D81B60"); break; // Rosa
                case "#E65100": inputBg = Color.parseColor("#FB8C00"); break; // Naranja
                case "#006064": inputBg = Color.parseColor("#00838F"); break; // Cyan/Teal
                case "#3E2723": inputBg = Color.parseColor("#5D4037"); break; // Marrón
                default: inputBg = Color.parseColor("#6A1B9A"); break; // Morado original
            }
            
            etBg = Color.parseColor("#FFFFFF"); 
            etText = Color.parseColor("#212121");
            etHint = Color.parseColor("#757575");
        }
        mainLayout.setBackgroundColor(bg); 
        toolbar.setBackgroundColor(tb); 
        bottomActionsBar.setBackgroundColor(tb);
        inputContainer.setBackgroundColor(inputBg);
        previewContainer.setBackgroundColor(inputBg);
        navBarPadding.setBackgroundColor(inputBg);
        
        Window w = getWindow(); w.setStatusBarColor(tb); w.setNavigationBarColor(inputBg);
        int c = Color.WHITE;
        btnMenuMore.setColorFilter(c);
        btnRecipes.setColorFilter(c);
        btnCalendar.setColorFilter(c);
        btnAlbum.setColorFilter(c);
        btnProfile.setColorFilter(c);
        btnHome.setColorFilter(c);
        btnSettings.setColorFilter(c);

        btnExpand.setColorFilter(theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511")); 
        btnSend.setColorFilter(theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511"));

        etMessage.setBackgroundColor(etBg); 
        etMessage.setTextColor(etText);
        etMessage.setHintTextColor(etHint);
    }

    public void pickImage(int code) {
        currentCropType = code; Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*");
        if (code == PICK_IMAGE_ALBUM) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "Selecciona imágenes"), code);
    }

    public String getCurrentTheme() { return currentTheme; }
    public UpdateManager getUpdateManager() { return updateManager; }
    public ProgressBar getDownloadProgressBar() { return downloadProgressBar; }
    public View getDownloadProgressContainer() { return downloadProgressContainer; }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null) {
            if (req == PICK_IMAGE_PROFILE) { Uri uri = data.getData(); if (uri != null) startCrop(uri); }
            else if (req == PICK_IMAGE_ALBUM) { if (data.getClipData() != null) { for (int i = 0; i < data.getClipData().getItemCount(); i++) upload(data.getClipData().getItemAt(i).getUri(), PICK_IMAGE_ALBUM); } else upload(data.getData(), PICK_IMAGE_ALBUM); }
            else if (req == PICK_IMAGE_CARTA) {
                Uri uri = data.getData();
                if (uri != null) {
                    upload(uri, PICK_IMAGE_CARTA);
                }
            }
            else if (req == PICK_IMAGE_RECIPE) {
                Uri uri = data.getData();
                if (uri != null) {
                    currentCropType = PICK_IMAGE_RECIPE;
                    startCrop(uri);
                }
            }
            else if (req == UCrop.REQUEST_CROP) {
                Uri r = UCrop.getOutput(data);
                if (r != null) {
                    if (currentCropType == PICK_IMAGE_PROFILE) upload(r, PICK_IMAGE_PROFILE);
                    else if (currentCropType == PICK_IMAGE_RECIPE) upload(r, PICK_IMAGE_RECIPE);
                }
            }
        }
    }

    private int pendingUploads = 0;
    private int completedUploads = 0;

    private void upload(Uri uri, int code) {
        if (uri == null) return;
        pendingUploads++;
        updateUploadProgress();
        messageEditor.uploadImage(uri, new UploadCallback() {
            @Override public void onStart(String id) {} 
            @Override public void onProgress(String id, long b, long t) {}
            @Override public void onSuccess(String id, Map res) {
                completedUploads++;
                updateUploadProgress();
                String url = (String) res.get("secure_url");
                runOnUiThread(() -> {
                    if (code == PICK_IMAGE_PROFILE) { 
                        currentUserImageUri = url; 
                        db.collection("users").document(currentUserId).update("profileImageUrl", url);
                        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (f instanceof ProfileFragment) ((ProfileFragment) f).setProfileImage(url);
                    }
                    else if (code == PICK_IMAGE_CARTA) {
                        messageEditor.setImageUrl(url);
                        selectedImageUrl = url;
                    }
                    else if (code == PICK_IMAGE_ALBUM) {
                        albumManager.addImageUrl(url);
                        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (f instanceof AlbumFragment) ((AlbumFragment) f).addImageUrl(url);
                    }
                    else if (code == PICK_IMAGE_RECIPE) {
                        recipeManager.setImageUrl(url);
                        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (f instanceof RecipeFragment) ((RecipeFragment) f).setImageUrl(url);
                    }
                    
                    if (completedUploads == pendingUploads) {
                        Toast.makeText(MainActivity.this, "¡Todas las imágenes subidas!", Toast.LENGTH_SHORT).show();
                        pendingUploads = 0;
                        completedUploads = 0;
                        updateUploadProgress();
                    }
                });
            }
            @Override public void onError(String id, ErrorInfo e) {
                completedUploads++;
                updateUploadProgress();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show());
            } 
            @Override public void onReschedule(String id, ErrorInfo e) {}
        });
    }

    private void updateUploadProgress() {
        runOnUiThread(() -> {
            if (pendingUploads > 0 && completedUploads < pendingUploads) {
                downloadProgressContainer.setVisibility(View.VISIBLE);
                ((TextView) downloadProgressContainer.getChildAt(0)).setText("Subiendo imágenes (" + completedUploads + "/" + pendingUploads + ")...");
                downloadProgressBar.setIndeterminate(false);
                downloadProgressBar.setMax(pendingUploads);
                downloadProgressBar.setProgress(completedUploads);
            } else {
                downloadProgressContainer.setVisibility(View.GONE);
            }
        });
    }

    private void startCrop(Uri uri) {
        UCrop.Options opt = new UCrop.Options(); opt.setCompressionFormat(Bitmap.CompressFormat.JPEG); opt.setFreeStyleCropEnabled(true);
        UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg"))).withOptions(opt).start(this);
    }

    public void logout() { getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().clear().apply(); updateWidget(); startActivity(new Intent(this, LoginActivity.class)); finish(); }

    @Override public void onMessageClick(View v, Message msg) { if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) albumManager.showAlbumDetail(msg); else messageEditor.showMessageDetail(msg); }
    @Override public void onMessageLongClick(View v, Message msg) { if (msg.getAuthorId().equals(currentUserId)) { PopupMenu p = new PopupMenu(this, v); p.getMenu().add("Editar"); p.getMenu().add("Borrar"); p.setOnMenuItemClickListener(item -> { if (item.getTitle().equals("Editar")) { if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) albumManager.showEditAlbumDialog(msg); else messageEditor.showEditDialog(msg, new MessageEditor.EditorCallback() { @Override public void onSave(Message m) { db.collection("messages").document(m.getMessageId()).set(m); } @Override public void onPickImage(int c) { pickImage(PICK_IMAGE_CARTA); } }); } else if (item.getTitle().equals("Borrar")) db.collection("messages").document(msg.getMessageId()).delete(); return true; }); p.show(); } }
    @Override public void onDeleteClick(Message m) { db.collection("messages").document(m.getMessageId()).delete(); }
}
