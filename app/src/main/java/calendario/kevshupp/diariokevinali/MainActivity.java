package calendario.kevshupp.diariokevinali;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.NotificationManager;
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
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import java.io.IOException;
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
import okhttp3.Response;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.SnapshotStateKt;
import calendario.kevshupp.diariokevinali.compose.MessageFeedComposeKt;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity implements AppNavigation {

    private static final int PICK_IMAGE_PROFILE = 1;
    private static final int PICK_IMAGE_CARTA = 3;
    private static final int PICK_IMAGE_ALBUM = 4;
    private static final int PICK_IMAGE_RECIPE = 5;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ConstraintLayout mainLayout;
    private MaterialToolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageView ivToolbarHeart;
    private ComposeView composeFeed;
    private MutableState<List<Message>> messagesState = SnapshotStateKt.mutableStateOf(new ArrayList<>(), SnapshotStateKt.neverEqualPolicy());
    private MutableState<String> themeState = SnapshotStateKt.mutableStateOf("Pixel Claro", SnapshotStateKt.neverEqualPolicy());
    private MutableState<Boolean> showEditorState = SnapshotStateKt.mutableStateOf(false, SnapshotStateKt.neverEqualPolicy());
    private MutableState<Message> editingMessageState = SnapshotStateKt.mutableStateOf(null, SnapshotStateKt.neverEqualPolicy());
    private MutableState<String> currentSelectedImageUrlState = SnapshotStateKt.mutableStateOf(null, SnapshotStateKt.neverEqualPolicy());
    private List<Message> messages;
    private EditText etMessage;
    private ImageButton btnSend, btnExpand, btnMenuMore, btnRecipes, btnCalendar, btnAlbum, btnProfile, btnHome, btnSettings, btnMisc;
    private View inputContainer, inputArea, bottomActionsBar;
    private View previewContainer;
    private FrameLayout fragmentContainer;
    private ImageView ivPreview;
    private ImageButton btnRemovePreview;
    private View navBarPadding;
    private LinearLayout downloadProgressContainer;
    private ProgressBar downloadProgressBar;
    private View toolbarBorder, bottomActionsBarBorder;
    private TextView tvTabHome, tvTabCalendar, tvTabAlbum, tvTabRecipes, tvTabProfile, tvTabMisc, tvTabSettings;
    private String selectedImageUrl = null;

    private String currentTheme = "Pixel Claro";
    private int activeTabId = R.id.btnHome;
    private String currentCoupleId = "vínculo_único_123", currentUserId, currentUserName, currentUserImageUri;
    private int currentCropType = -1;
    private final java.util.concurrent.ExecutorService fcmExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    public MainViewModel viewModel;
    private UpdateManager updateManager;
    private MessageEditor messageEditor;
    public RecipeManager recipeManager;
    private AlbumManager albumManager;
    private MutableState<Boolean> isUploadingState = SnapshotStateKt.mutableStateOf(false, SnapshotStateKt.neverEqualPolicy());
    private MutableState<String> overlayMessageState = SnapshotStateKt.mutableStateOf("Cargando...", SnapshotStateKt.neverEqualPolicy());

    // Launchers modernos para resultados de actividades
    private final androidx.activity.result.ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (currentCropType == PICK_IMAGE_PROFILE) {
                        Uri uri = data.getData();
                        if (uri != null) startCrop(uri);
                    } else if (currentCropType == PICK_IMAGE_ALBUM) {
                        if (data.getClipData() != null) {
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                upload(data.getClipData().getItemAt(i).getUri(), PICK_IMAGE_ALBUM);
                            }
                        } else if (data.getData() != null) {
                            upload(data.getData(), PICK_IMAGE_ALBUM);
                        }
                    } else if (currentCropType == PICK_IMAGE_CARTA) {
                        Uri uri = data.getData();
                        if (uri != null) upload(uri, PICK_IMAGE_CARTA);
                    } else if (currentCropType == PICK_IMAGE_RECIPE) {
                        Uri uri = data.getData();
                        if (uri != null) startCrop(uri);
                    }
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri r = UCrop.getOutput(result.getData());
                    if (r != null) {
                        if (currentCropType == PICK_IMAGE_PROFILE) upload(r, PICK_IMAGE_PROFILE);
                        else if (currentCropType == PICK_IMAGE_RECIPE) upload(r, PICK_IMAGE_RECIPE);
                    }
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("MainActivity", "Resultado de Google Sign-In recibido. ResultCode: " + result.getResultCode());
                if (result.getData() != null) {
                    com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> task = 
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        com.google.android.gms.auth.api.signin.GoogleSignInAccount account = task.getResult(com.google.android.gms.common.api.ApiException.class);
                        if (account != null && result.getResultCode() == RESULT_OK) {
                            String email = account.getEmail();
                            SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
                            prefs.edit().putString("syncGoogleAccountEmail", email).apply();
                            Toast.makeText(this, "Vinculado con " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Vinculación no completada (ResultCode: " + result.getResultCode() + ")", Toast.LENGTH_SHORT).show();
                        }
                    } catch (com.google.android.gms.common.api.ApiException e) {
                        Log.e("MainActivity", "Error al iniciar sesión de Google. Status Code: " + e.getStatusCode() + ", msg: " + e.getMessage());
                        Toast.makeText(this, "Error de vinculación (Código: " + e.getStatusCode() + "). Revisa tu configuración SHA-1 en Google Cloud/Firebase.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Vinculación cancelada. ResultCode: " + result.getResultCode(), Toast.LENGTH_LONG).show();
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<Intent> localFolderLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            int takeFlags = result.getData().getFlags() 
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            
                            SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
                            prefs.edit().putString("syncLocalFolderUri", uri.toString()).apply();
                            Toast.makeText(this, "Carpeta vinculada correctamente", Toast.LENGTH_SHORT).show();
                        } catch (SecurityException e) {
                            Log.e("MainActivity", "Error al persistir permisos: " + e.getMessage());
                            Toast.makeText(this, "Error al guardar carpeta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private androidx.fragment.app.Fragment fragment;
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener, calendarListener, userListener, petListener;
    private Calendar selectedFilterDate = null;

    private final java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final MutableState<Pet> petState = androidx.compose.runtime.SnapshotStateKt.mutableStateOf(new Pet(), androidx.compose.runtime.SnapshotStateKt.neverEqualPolicy());

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final BroadcastReceiver dndReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(intent.getAction())) {
                if (viewModel != null) {
                    viewModel.syncDndStateWithPet();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        enableImmersiveMode();

        SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "user_kevin_01");
        currentUserName = prefs.getString("userName", "Kevin");
        currentUserImageUri = prefs.getString("userImage", null);
        currentCoupleId = prefs.getString("coupleId", "vínculo_único_123");
        if (currentCoupleId.equals("vinculo_unico_123")) {
            currentCoupleId = "vínculo_único_123";
            prefs.edit().putString("coupleId", currentCoupleId).apply();
        }

        if (!prefs.contains("coupleId")) {
            prefs.edit().putString("coupleId", currentCoupleId).apply();
        }

        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupViewModelObservers();
        db = FirebaseFirestore.getInstance();
        ensureUserInFirestore();
        
        updateManager = new UpdateManager(this);
        messageEditor = new MessageEditor(this, currentCoupleId, currentUserId, currentUserName, currentUserImageUri);
        albumManager = new AlbumManager(this, currentCoupleId, currentUserId, currentUserName, currentUserImageUri);
        recipeManager = new RecipeManager(this, currentCoupleId, currentUserId, currentUserName, () -> pickImage(PICK_IMAGE_RECIPE));
        recipeManager.setTheme(currentTheme);

        initViews();
        applyTheme(prefs.getString("theme", "Pixel Claro"));
        updateTabSelection(R.id.btnHome);

        btnRecipes.setOnClickListener(v -> {
            updateTabSelection(R.id.btnRecipes);
            showFragment(RecipeFragment.newInstance(currentCoupleId, currentTheme));
        });
        btnCalendar.setOnClickListener(v -> {
            updateTabSelection(R.id.btnCalendar);
            showFragment(CalendarFragment.newInstance(currentCoupleId, currentUserId, currentTheme));
        });
        btnProfile.setOnClickListener(v -> {
            updateTabSelection(R.id.btnProfile);
            showFragment(ProfileFragment.newInstance(currentUserId, currentCoupleId, currentTheme));
        });
        btnSettings.setOnClickListener(v -> {
            updateTabSelection(R.id.btnSettings);
            showFragment(SettingsFragment.newInstance(currentUserId, currentCoupleId, currentTheme));
        });
        btnMisc.setOnClickListener(v -> {
            updateTabSelection(R.id.btnMisc);
            showFragment(MiscFragment.newInstance(currentTheme));
        });
        setupDynamicMargins();
        setupOfflineStatusListener();
        
        btnMenuMore.setOnClickListener(this::showOverflowMenu);

        setupRecyclerView();
        checkAndRequestPermissions();
        setupFirebaseMessaging();
        btnSend.setOnClickListener(v -> sendMessage());
        btnRemovePreview.setOnClickListener(v -> {
            selectedImageUrl = null;
            previewContainer.setVisibility(View.GONE);
        });

        handleUpdateIntent(getIntent());

        // El botón del lápiz ahora abre el editor de cartas completo en Compose
        btnExpand.setOnClickListener(v -> {
            editingMessageState.setValue(null);
            currentSelectedImageUrlState.setValue(null);
            showEditorState.setValue(true);
        });

        
        btnAlbum.setOnClickListener(v -> {
            updateTabSelection(R.id.btnAlbum);
            showFragment(AlbumFragment.newInstance(currentCoupleId, currentUserId, currentUserName, currentUserImageUri, currentTheme));
        });
        btnHome.setOnClickListener(v -> {
            updateTabSelection(R.id.btnHome);
            fragmentContainer.setVisibility(View.GONE);
            composeFeed.setVisibility(View.VISIBLE);
            inputArea.setVisibility(View.VISIBLE);
            btnMenuMore.setVisibility(View.VISIBLE); // Mostrar filtro en cartas
        });

        // Manejo moderno del botón Atrás
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    composeFeed.setVisibility(View.VISIBLE);
                    inputArea.setVisibility(View.VISIBLE);
                    btnMenuMore.setVisibility(View.VISIBLE);
                    fragmentContainer.setVisibility(View.GONE);
                    updateTabSelection(R.id.btnHome);
                } else {
                    setEnabled(false);
                    MainActivity.this.getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override public void onUpdateAvailable(String url) { showUpdateDialog(url); }
            @Override public void onNoUpdate() {}
            @Override public void onDownloadProgress(int p) { runOnUiThread(() -> downloadProgressBar.setProgress(p)); }
            @Override public void onDownloadComplete() { runOnUiThread(() -> { downloadProgressContainer.setVisibility(View.GONE); updateManager.installApk(); }); }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.startAllListeners();
        setupOverlays();
        checkNotificationPermission();
        try {
            registerReceiver(dndReceiver, new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED));
            viewModel.syncDndStateWithPet();
        } catch (Exception e) {
            Log.e("MainActivity", "Error registering dndReceiver: " + e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopAllListeners();
        try {
            unregisterReceiver(dndReceiver);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void showUpdateDialog(String url) {
        new AlertDialog.Builder(this)
                .setTitle("Actualización disponible")
                .setMessage("Una nueva versión está disponible en GitHub. ¿Deseas descargarla?")
                .setPositiveButton("Descargar", (d, w) -> {
                    overlayMessageState.setValue("Descargando actualización...");
                    isUploadingState.setValue(true);
                    updateManager.downloadUpdate(url, new UpdateManager.UpdateCallback() {
                        @Override public void onUpdateAvailable(String u) {}
                        @Override public void onNoUpdate() {}
                        @Override public void onDownloadProgress(int p) { 
                            runOnUiThread(() -> overlayMessageState.setValue("Descargando actualización: " + p + "%")); 
                        }
                        @Override public void onDownloadComplete() { runOnUiThread(() -> { isUploadingState.setValue(false); updateManager.installApk(); }); }
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
        composeFeed = findViewById(R.id.composeFeed);
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
        btnMisc = findViewById(R.id.btnMisc);
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
        toolbarBorder = findViewById(R.id.toolbarBorder);
        bottomActionsBarBorder = findViewById(R.id.bottomActionsBarBorder);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabCalendar = findViewById(R.id.tvTabCalendar);
        tvTabAlbum = findViewById(R.id.tvTabAlbum);
        tvTabRecipes = findViewById(R.id.tvTabRecipes);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvTabMisc = findViewById(R.id.tvTabMisc);
        tvTabSettings = findViewById(R.id.tvTabSettings);
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
        composeFeed.setVisibility(View.GONE);
        inputArea.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
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
        // Mostrar barras de sistema en lugar de ocultarlas para evitar solapamiento en llamadas o notificaciones
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    private void setupViewModelObservers() {
        viewModel.getPetState().observe(this, pet -> {
            if (pet != null) {
                petState.setValue(pet);
            }
        });
        viewModel.getMessagesState().observe(this, list -> {
            if (list != null) {
                messagesState.setValue(list);
            }
        });
        viewModel.getThemeState().observe(this, theme -> {
            if (theme != null) {
                themeState.setValue(theme);
                applyTheme(theme);
            }
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                showStyledPixelToast(message);
                viewModel.getToastMessage().setValue(null);
            }
        });
        viewModel.getLevelUpEvent().observe(this, pair -> {
            if (pair != null) {
                showStyledPixelToast("¡" + pair.getFirst() + " ha subido al nivel " + pair.getSecond() + "! 🎉");
                viewModel.getLevelUpEvent().setValue(null);
            }
        });
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        MessageFeedComposeKt.setFeedContent(
            composeFeed,
            messagesState,
            petState,
            currentUserId,
            themeState,
            editingMessageState,
            showEditorState,
            currentSelectedImageUrlState,
            msg -> { onMessageClick(null, msg); return kotlin.Unit.INSTANCE; },
            msg -> {
                // Ahora el menú se maneja en Compose, este callback se llama al pulsar "Editar"
                if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) {
                    albumManager.showEditAlbumDialog(msg);
                } else {
                    editingMessageState.setValue(msg);
                    currentSelectedImageUrlState.setValue(msg.getImageUrl());
                    showEditorState.setValue(true);
                }
                return kotlin.Unit.INSTANCE;
            },
            msg -> { viewModel.deleteMessage(msg); return kotlin.Unit.INSTANCE; },
            msg -> {
                viewModel.toggleLikeMessage(msg);
                if (msg.isLiked()) {
                    String letterTitle = msg.getTitle();
                    if (letterTitle == null || letterTitle.trim().isEmpty()) {
                        letterTitle = "una carta";
                    }
                    String notifTitle = "¡A " + currentUserName + " le encantó! ❤️";
                    String notifBody = "Le dio me gusta a tu carta: \"" + letterTitle + "\"";
                    sendNotificationV1(notifTitle, notifBody, msg.getImageUrl(), "like");
                }
                return kotlin.Unit.INSTANCE;
            },
            (title, content, imageUrl) -> {
                Message m = editingMessageState.getValue();
                boolean isEdit = m != null;
                if (m == null) {
                    m = new Message();
                    m.setMessageId(db.collection("messages").document().getId());
                    m.setAuthorId(currentUserId);
                    m.setAuthorName(currentUserName);
                    m.setAuthorImageUrl(currentUserImageUri);
                    m.setTimestamp(System.currentTimeMillis());
                    m.setPartnerId(currentCoupleId);
                }
                m.setTitle(title);
                m.setContent(content);
                m.setImageUrl(imageUrl);
                if (imageUrl != null) {
                    List<String> urls = new ArrayList<>();
                    urls.add(imageUrl);
                    m.setImageUrls(urls);
                }

                viewModel.saveMessageToFirestore(m, isEdit);

                if (!isEdit) {
                    String notifTitle = "Nuevo mensaje de " + currentUserName + " 💌";
                    String notifBody = (title != null && !title.isEmpty()) ? "«" + title + "»: " + content : content;
                    sendNotificationV1(notifTitle, notifBody, imageUrl, "carta");
                    Toast.makeText(MainActivity.this, "Carta enviada ❤️", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Carta actualizada ✨", Toast.LENGTH_SHORT).show();
                }
                return kotlin.Unit.INSTANCE;
            },
            newName -> { viewModel.updatePetName(newName); return kotlin.Unit.INSTANCE; },
            (accessoryId, cost) -> { viewModel.buyAccessory(accessoryId, cost); return kotlin.Unit.INSTANCE; },
            accessoryId -> { viewModel.equipAccessory(accessoryId); return kotlin.Unit.INSTANCE; },
            (backgroundId, cost) -> { viewModel.buyBackground(backgroundId, cost); return kotlin.Unit.INSTANCE; },
            backgroundId -> { viewModel.equipBackground(backgroundId); return kotlin.Unit.INSTANCE; },
            (foodId, cost, happinessGain) -> { viewModel.feedPet(foodId, cost, happinessGain); return kotlin.Unit.INSTANCE; },
            (points, exp) -> { viewModel.rewardPet(points, exp); return kotlin.Unit.INSTANCE; },
            () -> { viewModel.togglePetSleep(); return kotlin.Unit.INSTANCE; },
            () -> { pickImage(PICK_IMAGE_CARTA); return kotlin.Unit.INSTANCE; },
            () -> { viewModel.bathPet(); return kotlin.Unit.INSTANCE; },
            (points, happinessGain) -> { viewModel.playBallPet(points, happinessGain); return kotlin.Unit.INSTANCE; },
            (gameType, points, exp) -> { viewModel.playMinigame(gameType, points, exp); return kotlin.Unit.INSTANCE; }
        );
    }

    private void listenMessagesFromFirestore() {
        if (firestoreListener != null) firestoreListener.remove();
        Query query = db.collection("messages").whereEqualTo("partnerId", currentCoupleId).orderBy("timestamp", Query.Direction.DESCENDING);
        if (selectedFilterDate == null) {
            query = query.limit(100);
        }
        if (selectedFilterDate != null) {
            Calendar s = (Calendar) selectedFilterDate.clone();
            s.set(Calendar.HOUR_OF_DAY, 0); s.set(Calendar.MINUTE, 0); s.set(Calendar.SECOND, 0);
            Calendar e = (Calendar) selectedFilterDate.clone();
            e.set(Calendar.HOUR_OF_DAY, 23); e.set(Calendar.MINUTE, 59); e.set(Calendar.SECOND, 59);
            query = query.whereGreaterThanOrEqualTo("timestamp", s.getTimeInMillis()).whereLessThanOrEqualTo("timestamp", e.getTimeInMillis());
        }
        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                android.util.Log.e("Firestore", "Error en el listener de mensajes", error);
                return;
            }
            if (value != null) {
                List<Message> newMessages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Message m = doc.toObject(Message.class);
                    m.setMessageId(doc.getId());
                    if (m.getContent() == null || !m.getContent().startsWith("[ALBUM]")) {
                        newMessages.add(m);
                    }
                }
                messagesState.setValue(newMessages);
                updateWidget();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.stopActiveListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.startActiveListeners();
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
        if (fcmExecutor != null) {
            fcmExecutor.shutdown();
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



    private void ensureUserInFirestore() {
        if (currentUserId == null) return;
        
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(documentSnapshot -> {
                SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                
                java.util.Map<String, Object> userUpdates = new java.util.HashMap<>();
                userUpdates.put("userId", currentUserId);
                userUpdates.put("coupleId", currentCoupleId);
                if (currentUserName != null && !currentUserName.isEmpty()) {
                    userUpdates.put("userName", currentUserName);
                }
                if (currentUserImageUri != null && !currentUserImageUri.isEmpty()) {
                    userUpdates.put("profileImageUrl", currentUserImageUri);
                }

                if (documentSnapshot.exists()) {
                    boolean prefsChanged = false;
                    
                    String themeVal = documentSnapshot.getString("theme");
                    if (themeVal != null) {
                        editor.putString("theme", themeVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("theme", prefs.getString("theme", "Pixel Claro"));
                    }
                    
                    Boolean useCustomBgVal = documentSnapshot.getBoolean("useCustomBg");
                    if (useCustomBgVal != null) {
                        editor.putBoolean("useCustomBg", useCustomBgVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("useCustomBg", prefs.getBoolean("useCustomBg", false));
                    }
                    
                    String lightColorVal = documentSnapshot.getString("lightColor");
                    if (lightColorVal != null) {
                        editor.putString("lightColor", lightColorVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("lightColor", prefs.getString("lightColor", "#D1C4E9"));
                    }
                    
                    String darkColorVal = documentSnapshot.getString("darkColor");
                    if (darkColorVal != null) {
                        editor.putString("darkColor", darkColorVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("darkColor", prefs.getString("darkColor", "#4A148C"));
                    }
                    
                    Long cacheLimitVal = documentSnapshot.getLong("cacheSizeLimit");
                    if (cacheLimitVal != null) {
                        editor.putLong("cacheSizeLimit", cacheLimitVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("cacheSizeLimit", prefs.getLong("cacheSizeLimit", 100L));
                    }
                    
                    Long intervalVal = documentSnapshot.getLong("updateInterval");
                    if (intervalVal != null) {
                        editor.putLong("updateInterval", intervalVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("updateInterval", prefs.getLong("updateInterval", 720L));
                    }
                    
                    Long leadTimeVal = documentSnapshot.getLong("appointmentLeadTime");
                    if (leadTimeVal != null) {
                        editor.putLong("appointmentLeadTime", leadTimeVal);
                        prefsChanged = true;
                    } else {
                        userUpdates.put("appointmentLeadTime", prefs.getLong("appointmentLeadTime", 60L));
                    }
                    
                    if (prefsChanged) {
                        editor.apply();
                        runOnUiThread(() -> {
                            String finalTheme = prefs.getString("theme", "Pixel Claro");
                            String lc = prefs.getString("lightColor", "#D1C4E9");
                            String dc = prefs.getString("darkColor", "#4A148C");
                            applyTheme(finalTheme, lc, dc);
                        });
                    }
                } else {
                    userUpdates.put("theme", prefs.getString("theme", "Pixel Claro"));
                    userUpdates.put("useCustomBg", prefs.getBoolean("useCustomBg", false));
                    userUpdates.put("lightColor", prefs.getString("lightColor", "#D1C4E9"));
                    userUpdates.put("darkColor", prefs.getString("darkColor", "#4A148C"));
                    userUpdates.put("cacheSizeLimit", prefs.getLong("cacheSizeLimit", 100L));
                    userUpdates.put("updateInterval", prefs.getLong("updateInterval", 720L));
                    userUpdates.put("appointmentLeadTime", prefs.getLong("appointmentLeadTime", 60L));
                }

                db.collection("users").document(currentUserId)
                    .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Usuario y ajustes sincronizados con Firestore"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Error sincronizando usuario", e));
            })
            .addOnFailureListener(e -> Log.e("MainActivity", "Error al obtener documento de usuario de Firestore", e));
    }

    public void updateAllAuthorMessagesWithProfileImage(String newUrl) {
        if (currentUserId == null || newUrl == null || newUrl.trim().isEmpty()) return;
        db.collection("messages")
            .whereEqualTo("authorId", currentUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    batch.update(doc.getReference(), "authorImageUrl", newUrl);
                }
                batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Cartas anteriores actualizadas con la nueva foto de perfil"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Error al actualizar cartas anteriores", e));
            })
            .addOnFailureListener(e -> Log.e("MainActivity", "Error obteniendo cartas del autor para actualizar foto de perfil", e));
    }

    private void setupFirebaseMessaging() {
        String topicName = "diario_" + currentCoupleId.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("ñ", "n").replace(" ", "_");
        FirebaseMessaging.getInstance().subscribeToTopic(topicName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Suscrito con éxito al tema: " + topicName);
                    } else {
                        Log.e("FCM", "Error al suscribirse al tema fcm", task.getException());
                    }
                });
    }

    private void savePetDataToWidgetPrefs(Pet p) {
        android.content.SharedPreferences prefs = getSharedPreferences("thor_widget_prefs", MODE_PRIVATE);
        prefs.edit()
            .putString("pet_name", p.getName())
            .putInt("pet_level", p.getLevel())
            .putInt("pet_happiness", p.getHappiness())
            .putString("pet_status", p.getStatus())
            .putString("pet_accessory", p.getEquippedAccessory() != null ? p.getEquippedAccessory() : "none")
            .putBoolean("pet_sleeping", p.isSleeping())
            .putInt("pet_hunger", p.getHunger())
            .putInt("pet_cleanliness", p.getCleanliness())
            .apply();
        ThorWidgetProvider.triggerUpdate(this);
    }


    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 100);
        
        // Pedir permiso de segundo plano
        requestIgnoreBatteryOptimizations();
    }
    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error requesting battery optimization ignore", e);
                }
            }
        }
    }

    private void handleUpdateIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("update_url")) {
                String url = intent.getStringExtra("update_url");
                if (url != null) {
                    showUpdateDialog(url);
                }
            }
            if (intent.hasExtra("sync_error_msg")) {
                String errorMsg = intent.getStringExtra("sync_error_msg");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    showSyncErrorDialog(errorMsg);
                    intent.removeExtra("sync_error_msg");
                }
            }
            if (intent.hasExtra("click_type")) {
                String clickType = intent.getStringExtra("click_type");
                if (clickType != null) {
                    navigateToClickType(clickType);
                    intent.removeExtra("click_type");
                }
            }
        }
    }

    public void navigateToClickType(String clickType) {
        if (clickType == null) return;
        runOnUiThread(() -> {
            switch (clickType) {
                case "carta":
                case "like":
                    updateTabSelection(R.id.btnHome);
                    fragmentContainer.setVisibility(View.GONE);
                    composeFeed.setVisibility(View.VISIBLE);
                    inputArea.setVisibility(View.VISIBLE);
                    btnMenuMore.setVisibility(View.VISIBLE);
                    break;
                case "receta":
                    updateTabSelection(R.id.btnRecipes);
                    showFragment(RecipeFragment.newInstance(currentCoupleId, currentTheme));
                    break;
                case "cita":
                    updateTabSelection(R.id.btnCalendar);
                    showFragment(CalendarFragment.newInstance(currentCoupleId, currentUserId, currentTheme));
                    break;
                case "album":
                    updateTabSelection(R.id.btnAlbum);
                    showFragment(AlbumFragment.newInstance(currentCoupleId, currentUserId, currentUserName, currentUserImageUri, currentTheme));
                    break;
            }
        });
    }

    public void showSyncErrorDialog(String errorMsg) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Error de Sincronización")
                .setMessage("Ocurrió un error al sincronizar las fotos con Google Drive:\n\n" + errorMsg)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void sendMessage() {
        String txt = etMessage.getText().toString().trim(); 
        if (txt.isEmpty() && selectedImageUrl == null) return;
        
        List<String> imgs = new ArrayList<>();
        if (selectedImageUrl != null) imgs.add(selectedImageUrl);
        
        Message msg = new Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, txt, imgs, System.currentTimeMillis(), false);
        
        // Vibrar al enviar (uso moderno de VibrationEffect)
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        saveMessageToFirestore(msg);
        etMessage.setText("");
        selectedImageUrl = null;
        previewContainer.setVisibility(View.GONE);
    }

    public void sendNotificationV1(String title, String messageText, String imageUrl) {
        sendNotificationV1(title, messageText, imageUrl, null);
    }

    public void sendNotificationV1(String title, String messageText, String imageUrl, String type) {
        fcmExecutor.execute(() -> {
            try {
                try (InputStream is = getAssets().open("service-account.json")) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                            .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
                    credentials.refreshIfExpired();
                    String token = credentials.getAccessToken().getTokenValue();
                    
                    String projectId = "diario-pareja-a2d35"; 
                    String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

                    JSONObject jsonBody = new JSONObject();
                    JSONObject message = new JSONObject();
                    JSONObject notification = new JSONObject();
                    JSONObject data = new JSONObject();

                    notification.put("title", title != null ? title : "Nuevo mensaje de " + currentUserName);
                    notification.put("body", messageText != null && !messageText.isEmpty() ? messageText : "Te han enviado una foto 📸");
                    
                    data.put("authorId", currentUserId);
                    if (imageUrl != null) data.put("imageUrl", imageUrl);
                    if (type != null) data.put("click_type", type);

                    // Topic name sin acentos para evitar errores en FCM
                    String topicName = "diario_" + currentCoupleId.toLowerCase()
                            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                            .replace("ñ", "n").replace(" ", "_");
                    
                    JSONObject android = new JSONObject();
                    JSONObject androidNotification = new JSONObject();
                    androidNotification.put("channel_id", "diario_channel");
                    android.put("notification", androidNotification);

                    message.put("topic", topicName);
                    message.put("notification", notification);
                    message.put("data", data);
                    message.put("android", android);
                    jsonBody.put("message", message);

                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader("Authorization", "Bearer " + token)
                            .build();

                    Response response = DiarioApp.getOkHttpClient().newCall(request).execute();
                    String responseBody = "";
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                    final int code = response.code();
                    final String finalResp = responseBody;
                    if (response.isSuccessful()) {
                        Log.d("FCM_V1", "Notificación enviada con éxito");
                    } else {
                        Log.e("FCM_V1", "Error al enviar notificación: " + code + " - " + finalResp);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "⚠️ FCM Error: " + code + " - " + finalResp, Toast.LENGTH_LONG).show();
                        });
                    }
                    response.close();
                }
            } catch (IOException e) {
                Log.e("FCM_V1", "ERROR: No se encontró o no se pudo leer service-account.json en assets. Las notificaciones no se enviarán.");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "🚨 ERROR: No se pudo leer service-account.json en assets.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e("FCM_V1", "Error en envío de notificación FCM: " + e.getMessage(), e);
            }
        });
    }

    public void sendNotificationV1(String messageText, String imageUrl) {
        sendNotificationV1("Nuevo mensaje de " + currentUserName, messageText, imageUrl);
    }

    // Simplificado para no romper compatibilidad con llamadas existentes
    public void sendNotificationV1(String messageText) { sendNotificationV1(messageText, null); }

    public void testLocalNotification() {
        Toast.makeText(this, "Enviando notificación de prueba...", Toast.LENGTH_SHORT).show();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "diario_channel")
                .setSmallIcon(R.drawable.ic_heart_pixel)
                .setContentTitle("Prueba de Diario Pixel 🔔")
                .setContentText("¡Funciona! Esta es una notificación de prueba local.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(999, builder.build());
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show();
        }
    }

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

    public void rescheduleAllCalendarReminders() {
        viewModel.rescheduleAllCalendarReminders();
    }

    @Override
    public void showAddEventDialog(String date, CalendarEvent event) {
        try {
            long ts = Long.parseLong(date);
            showAddEventDialog(ts, event);
        } catch (NumberFormatException e) {
            android.util.Log.e("MainActivity", "Error parsing date: " + date);
        }
    }

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
            boolean isNew = (edit == null);
            String id = edit != null ? edit.getEventId() : UUID.randomUUID().toString(); 
            CalendarEvent ev = new CalendarEvent(id, title, etDesc != null ? etDesc.getText().toString().trim() : "", time.getTimeInMillis(), currentUserId, currentCoupleId); 
            ev.setAuthorName(currentUserName);
            int pos = spinner.getSelectedItemPosition();
            ev.setRecurrence(pos >= 0 ? values[pos] : "NONE"); 
            db.collection("calendar").document(id).set(ev).addOnSuccessListener(aVoid -> {
                viewModel.scheduleCalendarReminder(ev);
                String notifTitle = isNew ? "¡Nueva cita creada! 📅" : "¡Cita modificada! 📅";
                sendNotificationV1(notifTitle, currentUserName + (isNew ? " agregó la cita: \"" : " modificó la cita: \"") + ev.getTitle() + "\"", null, "cita");
            });
            d.dismiss(); 
        });
        v.findViewById(R.id.btnCancelEvent).setOnClickListener(v1 -> d.dismiss());
        d.show();
    }

    private boolean isColorLight(int color) {
        double luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)) / 255.0;
        return luminance > 0.5;
    }

    public void updateTabSelection(int activeTabId) {
        this.activeTabId = activeTabId;
        boolean isDark = "Pixel Oscuro".equals(currentTheme);
        int activeColor;
        int inactiveColor;

        if (isDark) {
            activeColor = Color.parseColor("#FFEB3B");
            inactiveColor = Color.argb(160, 255, 255, 255);
        } else {
            activeColor = Color.parseColor("#4A2511");
            inactiveColor = Color.argb(140, 74, 37, 17);
        }

        // Home Tab
        btnHome.setColorFilter(activeTabId == R.id.btnHome ? activeColor : inactiveColor);
        tvTabHome.setTextColor(activeTabId == R.id.btnHome ? activeColor : inactiveColor);

        // Calendar Tab
        btnCalendar.setColorFilter(activeTabId == R.id.btnCalendar ? activeColor : inactiveColor);
        tvTabCalendar.setTextColor(activeTabId == R.id.btnCalendar ? activeColor : inactiveColor);

        // Album Tab
        btnAlbum.setColorFilter(activeTabId == R.id.btnAlbum ? activeColor : inactiveColor);
        tvTabAlbum.setTextColor(activeTabId == R.id.btnAlbum ? activeColor : inactiveColor);

        // Recipes Tab
        btnRecipes.setColorFilter(activeTabId == R.id.btnRecipes ? activeColor : inactiveColor);
        tvTabRecipes.setTextColor(activeTabId == R.id.btnRecipes ? activeColor : inactiveColor);

        // Profile Tab
        btnProfile.setColorFilter(activeTabId == R.id.btnProfile ? activeColor : inactiveColor);
        tvTabProfile.setTextColor(activeTabId == R.id.btnProfile ? activeColor : inactiveColor);

        // Misc Tab
        btnMisc.setColorFilter(activeTabId == R.id.btnMisc ? activeColor : inactiveColor);
        tvTabMisc.setTextColor(activeTabId == R.id.btnMisc ? activeColor : inactiveColor);

        // Settings Tab
        btnSettings.setColorFilter(activeTabId == R.id.btnSettings ? activeColor : inactiveColor);
        tvTabSettings.setTextColor(activeTabId == R.id.btnSettings ? activeColor : inactiveColor);
    }

    public void applyTheme(String theme) {
        applyTheme(theme, null, null);
    }

    public void applyTheme(String theme, String lightColor) {
        applyTheme(theme, lightColor, null);
    }

    public void applyTheme(String theme, String lightColor, String darkColor) {
        this.currentTheme = theme;
        themeState.setValue(theme);
        albumManager.setTheme(theme);
        recipeManager.setTheme(theme);
        messageEditor.setTheme(theme);
        int bg, tb, inputBg, etBg, etText, etHint;
        SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
        boolean useCustomBg = prefs.getBoolean("useCustomBg", false);
        if (theme.equals("Pixel Oscuro")) {
            // Usar el color pasado directamente, o cargar de SharedPreferences si no se proporciona
            String finalDarkColor = darkColor;
            if (finalDarkColor == null) {
                finalDarkColor = prefs.getString("darkColor", "#4A148C");
            }
            tb = Color.parseColor(finalDarkColor);
            if (useCustomBg) {
                bg = getDarkColorVariant(tb);
            } else {
                bg = Color.parseColor("#0D0D2B"); // Azul medianoche profundo por defecto
            }

            // Lógica refinada para el fondo del área de entrada basado en el color de barra en modo oscuro
            switch (finalDarkColor.toUpperCase()) {
                case "#0D47A1": inputBg = Color.parseColor("#1976D2"); break; // Azul oscuro
                case "#1B5E20": inputBg = Color.parseColor("#388E3C"); break; // Verde oscuro
                case "#C2185B": inputBg = Color.parseColor("#D81B60"); break; // Rosa oscuro
                case "#E65100": inputBg = Color.parseColor("#FB8C00"); break; // Naranja oscuro
                case "#006064": inputBg = Color.parseColor("#00838F"); break; // Cyan oscuro
                case "#3E2723": inputBg = Color.parseColor("#5D4037"); break; // Marrón oscuro
                default: inputBg = Color.parseColor("#6B21A8"); break; // Púrpura oscuro
            }

            etBg = Color.parseColor("#1A1A2E");
            etText = Color.WHITE;
            etHint = Color.parseColor("#AAAAAA");
        } else {
            String actualLightColor = lightColor;
            if (actualLightColor == null) {
                actualLightColor = prefs.getString("lightColor", "#D1C4E9");
            }
            tb = Color.parseColor(actualLightColor); 
            if (useCustomBg) {
                bg = getPastelColor(tb);
            } else {
                bg = Color.parseColor("#F5E6BE"); // Crema suave / Stardew Valley
            }
            inputBg = Color.parseColor("#B39DDB"); 
            
            // Lógica refinada para el fondo del área de entrada basado en el color de barra
            switch (actualLightColor.toUpperCase()) {
                case "#B3E5FC": inputBg = Color.parseColor("#81D4FA"); break; // Azul pastel
                case "#C8E6C9": inputBg = Color.parseColor("#A5D6A7"); break; // Verde pastel
                case "#F8BBD0": inputBg = Color.parseColor("#F48FB1"); break; // Rosa pastel
                case "#FFE0B2": inputBg = Color.parseColor("#FFCC80"); break; // Naranja pastel
                case "#B2EBF2": inputBg = Color.parseColor("#80DEEA"); break; // Cyan pastel
                case "#D7CCC8": inputBg = Color.parseColor("#BCAAA4"); break; // Marrón pastel
                default: inputBg = Color.parseColor("#B39DDB"); break; // Púrpura pastel
            }
            
            etBg = Color.parseColor("#FFFFFF"); 
            etText = Color.parseColor("#212121");
            etHint = Color.parseColor("#757575");
        }
        mainLayout.setBackgroundColor(bg); 
        toolbar.setBackgroundColor(tb); 
        bottomActionsBar.setBackgroundColor(tb);
        
        // Cargar el contenedor pixelado 3D y colorear su interior dinámicamente según el tema
        android.graphics.drawable.Drawable containerBg = getDrawable(R.drawable.bg_input_container_pixel);
        if (containerBg instanceof android.graphics.drawable.LayerDrawable) {
            android.graphics.drawable.LayerDrawable ld = (android.graphics.drawable.LayerDrawable) containerBg;
            android.graphics.drawable.GradientDrawable inner = (android.graphics.drawable.GradientDrawable) ld.findDrawableByLayerId(R.id.inner_solid);
            if (inner != null) {
                inner.setColor(inputBg);
            }
        }
        inputContainer.setBackground(containerBg);
        
        previewContainer.setBackgroundColor(inputBg);
        navBarPadding.setBackgroundColor(tb); // Que coincida con la barra inferior
        
        Window w = getWindow();
        w.setStatusBarColor(tb);
        w.setNavigationBarColor(tb); // Que coincida con la barra inferior
        
        // Ajustar íconos de la barra de estado y de navegación dinámicamente según la claridad del color
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(w, w.getDecorView());
        if (controller != null) {
            boolean isLight = isColorLight(tb);
            controller.setAppearanceLightStatusBars(isLight);
            controller.setAppearanceLightNavigationBars(isLight);
        }
        
        // Colorear los bordes pixelados superior e inferior
        int borderColorVal = theme.equals("Pixel Oscuro") ? Color.parseColor("#91465F") : Color.parseColor("#4A2511");
        if (toolbarBorder != null) toolbarBorder.setBackgroundColor(borderColorVal);
        if (bottomActionsBarBorder != null) bottomActionsBarBorder.setBackgroundColor(borderColorVal);

        int toolbarContentColor = theme.equals("Pixel Oscuro") ? Color.WHITE : Color.parseColor("#4A2511");
        tvToolbarTitle.setTextColor(toolbarContentColor);
        ivToolbarHeart.setColorFilter(toolbarContentColor);
        btnMenuMore.setColorFilter(toolbarContentColor);
        
        // Llamar a updateTabSelection para aplicar los filtros correctos de pestaña activa/inactiva
        updateTabSelection(activeTabId);

        // Aplicar fondos de botones 3D retro con cambio de estado táctil y sus respectivos filtros de color
        if (theme.equals("Pixel Oscuro")) {
            btnExpand.setBackgroundResource(R.drawable.bg_btn_pixel_small_dark);
            btnSend.setBackgroundResource(R.drawable.bg_btn_pixel_small_dark);
            btnExpand.setColorFilter(Color.WHITE);
            btnSend.setColorFilter(Color.WHITE);
            etMessage.setBackgroundResource(R.drawable.bg_message_pixel_dark);
        } else {
            btnExpand.setBackgroundResource(R.drawable.bg_btn_pixel_small);
            btnSend.setBackgroundResource(R.drawable.bg_btn_pixel_small);
            btnExpand.setColorFilter(Color.parseColor("#4A2511"));
            btnSend.setColorFilter(Color.parseColor("#4A2511"));
            etMessage.setBackgroundResource(R.drawable.bg_message_pixel);
        }

        etMessage.setTextColor(etText);
        etMessage.setHintTextColor(etHint);
    }

    public void pickImage(int code) {
        currentCropType = code;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        if (code == PICK_IMAGE_ALBUM) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(Intent.createChooser(i, "Selecciona imágenes"));
    }

    public String getCurrentTheme() { return currentTheme; }

    public void showStyledPixelToast(String message) {
        runOnUiThread(() -> {
            try {
                if (isFinishing() || isDestroyed()) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                
                float density = getResources().getDisplayMetrics().density;
                int paddingHorizontal = (int) (24 * density);
                int paddingVertical = (int) (16 * density);
                layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
                
                TextView textView = new TextView(MainActivity.this);
                textView.setText(message);
                textView.setTextSize(18);
                textView.setGravity(android.view.Gravity.CENTER);
                
                try {
                    textView.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(MainActivity.this, R.font.vt323));
                } catch (Exception e) {
                    // Ignorar
                }
                
                layout.addView(textView);
                
                boolean isDark = "Pixel Oscuro".equals(currentTheme);
                if (isDark) {
                    layout.setBackgroundResource(R.drawable.bg_parchment_pixel_dark);
                    textView.setTextColor(Color.WHITE);
                } else {
                    layout.setBackgroundResource(R.drawable.bg_parchment_pixel);
                    textView.setTextColor(Color.parseColor("#4A2511")); // chocolate
                }
                
                builder.setView(layout);
                AlertDialog dialog = builder.create();
                
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    
                    android.view.WindowManager.LayoutParams wlp = dialog.getWindow().getAttributes();
                    wlp.gravity = android.view.Gravity.BOTTOM;
                    wlp.y = (int) (100 * density);
                    dialog.getWindow().setAttributes(wlp);
                }
                
                dialog.show();
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (!isFinishing() && !isDestroyed() && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    } catch (Exception e) {
                        // Ignorar
                    }
                }, 2500);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public UpdateManager getUpdateManager() { return updateManager; }
    public ProgressBar getDownloadProgressBar() { return downloadProgressBar; }
    public View getDownloadProgressContainer() { return downloadProgressContainer; }

    public void openAddRecipeDialog() {
        if (recipeManager != null) recipeManager.showAddRecipeDialog(null);
    }

    public void openRecipeDetailDialog(Recipe recipe) {
        if (recipeManager != null && recipe != null) recipeManager.showRecipeDetailDialog(recipe);
    }


    private int pendingUploads = 0;
    private int completedUploads = 0;

    private void upload(Uri uri, int code) {
        if (uri == null) return;
        pendingUploads++;
        updateUploadProgress();
        if (code == PICK_IMAGE_ALBUM) {
            albumManager.onAlbumUploadStarted();
        }
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
                        updateAllAuthorMessagesWithProfileImage(url);
                        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (f instanceof ProfileFragment) ((ProfileFragment) f).setProfileImage(url);
                    }
                    else if (code == PICK_IMAGE_CARTA) {
                        messageEditor.setImageUrl(url);
                        currentSelectedImageUrlState.setValue(url);
                        selectedImageUrl = url;
                    }
                    else if (code == PICK_IMAGE_ALBUM) {
                        albumManager.addImageUrl(url);
                        albumManager.onAlbumUploadFinished();
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
                if (code == PICK_IMAGE_ALBUM) {
                    albumManager.onAlbumUploadFinished();
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show());
            } 
            @Override public void onReschedule(String id, ErrorInfo e) {}
        });
    }

    private void updateUploadProgress() {
        runOnUiThread(() -> {
            boolean uploading = pendingUploads > 0 && completedUploads < pendingUploads;
            isUploadingState.setValue(uploading);
            if (uploading) {
                overlayMessageState.setValue("Subiendo imágenes (" + completedUploads + "/" + pendingUploads + ")...");
            }
        });
    }

    private void startCrop(Uri uri) {
        UCrop.Options opt = new UCrop.Options();
        opt.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        opt.setFreeStyleCropEnabled(false);
        opt.setHideBottomControls(true);
        Intent cropIntent = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg")))
                .withAspectRatio(1, 1)
                .withOptions(opt)
                .getIntent(this);
        cropImageLauncher.launch(cropIntent);
    }

    public void logout() { getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().clear().apply(); updateWidget(); startActivity(new Intent(this, LoginActivity.class)); finish(); }
    public AlbumManager getAlbumManager() { return albumManager; }

    public void onMessageClick(View v, Message msg) { if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) albumManager.showAlbumDetail(msg); else messageEditor.showMessageDetail(msg); }
    public void onMessageLongClick(View v, Message msg) {
        if (msg.getAuthorId().equals(currentUserId)) {
            PopupMenu p = new PopupMenu(this, v != null ? v : composeFeed);
            p.getMenu().add("Editar");
            p.getMenu().add("Borrar");
            p.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Editar")) {
                    if (msg.getContent() != null && msg.getContent().startsWith("[ALBUM]")) {
                        albumManager.showEditAlbumDialog(msg);
                    } else {
                        editingMessageState.setValue(msg);
                        currentSelectedImageUrlState.setValue(msg.getImageUrl());
                        showEditorState.setValue(true);
                    }
                } else if (item.getTitle().equals("Borrar")) {
                    db.collection("messages").document(msg.getMessageId()).delete();
                }
                return true;
            });
            p.show();
        }
    }
    public void onDeleteClick(Message m) { db.collection("messages").document(m.getMessageId()).delete(); }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleUpdateIntent(intent);
    }
    private void saveMessageToFirestore(Message m) {
        String docId = m.getMessageId();
        if (docId == null) {
            docId = UUID.randomUUID().toString();
            m.setMessageId(docId);
        }
        if (m.getPartnerId() == null) m.setPartnerId(currentCoupleId);
        
        viewModel.saveMessageToFirestore(m, false);
        
        android.util.Log.d("Firestore", "Mensaje guardado con éxito: " + m.getMessageId());
        Toast.makeText(MainActivity.this, "¡Carta enviada!", Toast.LENGTH_SHORT).show();
        sendNotificationV1("¡Carta nueva! 💌", m.getContent(), m.getImageUrl(), "carta");
    }

    private void setupOverlays() {
        ComposeView overlayCompose = findViewById(R.id.overlayCompose);
        if (overlayCompose != null) {
            calendario.kevshupp.diariokevinali.compose.SharedComposeKt.setOverlayContent(overlayCompose, isUploadingState, overlayMessageState);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Por favor, permite alarmas exactas para los recordatorios", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int getPastelColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.12f; // Low saturation for pastel
        hsv[2] = 0.98f; // High brightness
        return Color.HSVToColor(hsv);
    }

    private int getDarkColorVariant(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.6f;  // Moderate saturation
        hsv[2] = 0.10f; // Very low brightness (almost black/midnight)
        return Color.HSVToColor(hsv);
    }

    public void linkGoogleDriveAccount() {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = 
            new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                .build();
        com.google.android.gms.auth.api.signin.GoogleSignInClient client = 
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);
        
        // Desconectar sesión previa para forzar selector de cuenta si es necesario
        client.signOut().addOnCompleteListener(task -> {
            googleSignInLauncher.launch(client.getSignInIntent());
        });
    }

    public void selectLocalFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        localFolderLauncher.launch(intent);
    }
}
