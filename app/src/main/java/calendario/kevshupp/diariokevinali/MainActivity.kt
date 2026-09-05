package calendario.kevshupp.diariokevinali

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.yalantis.ucrop.UCrop
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.HashSet
import java.util.Locale
import java.util.UUID
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.lifecycle.ViewModelProvider
import calendario.kevshupp.diariokevinali.compose.setFeedContent
import calendario.kevshupp.diariokevinali.compose.setOverlayContent

class MainActivity : AppCompatActivity(), AppNavigation {

    companion object {
        private const val PICK_IMAGE_PROFILE = 1
        private const val PICK_IMAGE_CARTA = 3
        private const val PICK_IMAGE_ALBUM = 4
        private const val PICK_IMAGE_RECIPE = 5
        private const val PERMISSION_REQUEST_CODE = 100

        @Volatile
        private var cachedGoogleCredentials: GoogleCredentials? = null

        @Synchronized
        fun getGoogleCredentials(context: Context): GoogleCredentials {
            var creds = cachedGoogleCredentials
            if (creds == null) {
                context.assets.open("service-account.json").use { `is` ->
                    creds = GoogleCredentials.fromStream(`is`)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"))
                    cachedGoogleCredentials = creds
                }
            }
            creds!!.refreshIfExpired()
            return creds!!
        }
    }

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var ivToolbarHeart: ImageView
    private lateinit var composeFeed: ComposeView
    
    private val messagesState: MutableState<List<Message>> = mutableStateOf(emptyList())
    private val themeState: MutableState<String> = mutableStateOf("Pixel Claro")
    private val showEditorState: MutableState<Boolean> = mutableStateOf(false)
    private val showPetDialogState: MutableState<Boolean> = mutableStateOf(false)
    private val editingMessageState: MutableState<Message?> = mutableStateOf(null)
    private val currentSelectedImageUrlState: MutableState<String?> = mutableStateOf(null)
    private val isUploadingState: MutableState<Boolean> = mutableStateOf(false)
    private val overlayMessageState: MutableState<String> = mutableStateOf("Cargando...")
    private val petState: MutableState<Pet> = mutableStateOf(Pet())

    private var messages: List<Message> = ArrayList()
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnExpand: ImageButton
    private lateinit var btnMenuMore: ImageButton
    private lateinit var btnRecipes: ImageButton
    private lateinit var btnCalendar: ImageButton
    private lateinit var btnAlbum: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnMisc: ImageButton
    private lateinit var inputContainer: View
    private lateinit var inputArea: View
    private lateinit var bottomActionsBar: View
    private lateinit var previewContainer: View
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var ivPreview: ImageView
    private lateinit var btnRemovePreview: ImageButton
    private lateinit var navBarPadding: View
    private lateinit var downloadProgressContainer: LinearLayout
    private lateinit var downloadProgressBar: ProgressBar
    private lateinit var toolbarBorder: View
    private lateinit var bottomActionsBarBorder: View
    private lateinit var tvTabHome: TextView
    private lateinit var tvTabCalendar: TextView
    private lateinit var tvTabAlbum: TextView
    private lateinit var tvTabRecipes: TextView
    private lateinit var tvTabProfile: TextView
    private lateinit var tvTabMisc: TextView
    private lateinit var tvTabSettings: TextView

    private var selectedImageUrl: String? = null
    private var currentTheme = "Pixel Claro"
    private var activeTabId = R.id.btnHome
    private var currentCoupleId = "vínculo_único_123"
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var currentUserImageUri: String? = null
    private var currentCropType = -1
    private val fcmExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    lateinit var viewModel: MainViewModel
    private lateinit var updateManager: UpdateManager
    private lateinit var messageEditor: MessageEditor
    lateinit var recipeManager: RecipeManager
    private lateinit var albumManager: AlbumManager

    private var fragment: androidx.fragment.app.Fragment? = null
    private lateinit var db: FirebaseFirestore
    private var firestoreListener: ListenerRegistration? = null
    private var calendarListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var petListener: ListenerRegistration? = null
    private var selectedFilterDate: Calendar? = null

    private val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var networkStatusTracker: NetworkStatusTracker? = null

    private val pickImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data
            if (currentCropType == PICK_IMAGE_PROFILE) {
                val uri = data?.data
                if (uri != null) startCrop(uri)
            } else if (currentCropType == PICK_IMAGE_ALBUM) {
                if (data?.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        upload(data.clipData!!.getItemAt(i).uri, PICK_IMAGE_ALBUM)
                    }
                } else if (data?.data != null) {
                    upload(data.data!!, PICK_IMAGE_ALBUM)
                }
            } else if (currentCropType == PICK_IMAGE_CARTA) {
                val uri = data?.data
                if (uri != null) upload(uri, PICK_IMAGE_CARTA)
            } else if (currentCropType == PICK_IMAGE_RECIPE) {
                val uri = data?.data
                if (uri != null) startCrop(uri)
            }
        }
    }

    private val cropImageLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val r = UCrop.getOutput(result.data!!)
            if (r != null) {
                if (currentCropType == PICK_IMAGE_PROFILE) upload(r, PICK_IMAGE_PROFILE)
                else if (currentCropType == PICK_IMAGE_RECIPE) upload(r, PICK_IMAGE_RECIPE)
            }
        }
    }

    private val googleSignInLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        Log.d("MainActivity", "Resultado de Google Sign-In recibido. ResultCode: ${result.resultCode}")
        if (result.data != null) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null && result.resultCode == RESULT_OK) {
                    val email = account.email
                    val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
                    prefs.edit().putString("syncGoogleAccountEmail", email).apply()
                    Toast.makeText(this, "Vinculado con $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Vinculación no completada (ResultCode: ${result.resultCode})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e("MainActivity", "Error al iniciar sesión de Google. Status Code: ${e.statusCode}, msg: ${e.message}")
                Toast.makeText(this, "Error de vinculación (Código: ${e.statusCode}). Revisa tu configuración SHA-1 en Google Cloud/Firebase.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Vinculación cancelada. ResultCode: ${result.resultCode}", Toast.LENGTH_LONG).show()
        }
    }

    private val localFolderLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    val takeFlags = result.data!!.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    
                    val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
                    prefs.edit().putString("syncLocalFolderUri", uri.toString()).apply()
                    Toast.makeText(this, "Carpeta vinculada correctamente", Toast.LENGTH_SHORT).show()
                } catch (e: SecurityException) {
                    Log.e("MainActivity", "Error al persistir permisos: ${e.message}")
                    Toast.makeText(this, "Error al guardar carpeta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val dndReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED == intent.action) {
                viewModel.syncDndStateWithPet()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableImmersiveMode()

        val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userId", "user_kevin_01")
        currentUserName = prefs.getString("userName", "Kevin")
        currentUserImageUri = prefs.getString("userImage", null)
        currentCoupleId = prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123"
        if (currentCoupleId == "vinculo_unico_123") {
            currentCoupleId = "vínculo_único_123"
            prefs.edit().putString("coupleId", currentCoupleId).apply()
        }

        if (!prefs.contains("coupleId")) {
            prefs.edit().putString("coupleId", currentCoupleId).apply()
        }

        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupViewModelObservers()
        db = FirebaseFirestore.getInstance()
        ensureUserInFirestore()
        
        updateManager = UpdateManager(this)
        messageEditor = MessageEditor(this, currentCoupleId, currentUserId ?: "", currentUserName ?: "", currentUserImageUri)
        albumManager = AlbumManager(this, currentCoupleId, currentUserId ?: "", currentUserName ?: "", currentUserImageUri)
        recipeManager = RecipeManager(this, currentCoupleId, currentUserId ?: "", currentUserName ?: "") { pickImage(PICK_IMAGE_RECIPE) }
        recipeManager.setTheme(currentTheme)

        initViews()
        applyRefreshRate(prefs.getInt("refreshRate", 90))
        applyTheme(prefs.getString("theme", "Pixel Claro") ?: "Pixel Claro")
        updateTabSelection(R.id.btnHome)

        btnRecipes.setOnClickListener {
            updateTabSelection(R.id.btnRecipes)
            showFragment(RecipeFragment.newInstance(currentCoupleId, currentTheme))
        }
        btnCalendar.setOnClickListener {
            updateTabSelection(R.id.btnCalendar)
            showFragment(CalendarFragment.newInstance(currentCoupleId, currentUserId ?: "", currentTheme))
        }
        btnProfile.setOnClickListener {
            updateTabSelection(R.id.btnProfile)
            showFragment(ProfileFragment.newInstance(currentUserId ?: "", currentCoupleId, currentTheme))
        }
        btnSettings.setOnClickListener {
            updateTabSelection(R.id.btnSettings)
            showFragment(SettingsFragment.newInstance(currentUserId ?: "", currentCoupleId, currentTheme))
        }
        btnMisc.setOnClickListener {
            updateTabSelection(R.id.btnMisc)
            showFragment(MiscFragment.newInstance(currentTheme))
        }
        setupDynamicMargins()
        setupOfflineStatusListener()
        setupSosEmergencyListener()
        
        btnMenuMore.setOnClickListener { showOverflowMenu(it) }

        setupRecyclerView()
        PermissionHelper.checkAndRequestPermissions(this)
        setupFirebaseMessaging()

        if (prefs.getBoolean("radar_is_sharing", true) && PermissionHelper.hasLocationPermission(this)) {
            ThorRadarService.startService(this)
            ThorRadarManager.publishHeartbeat(this)
        }

        btnSend.setOnClickListener { sendMessage() }
        btnRemovePreview.setOnClickListener {
            selectedImageUrl = null
            previewContainer.visibility = View.GONE
        }

        handleUpdateIntent(intent)

        btnExpand.setOnClickListener {
            editingMessageState.value = null
            currentSelectedImageUrlState.value = null
            showEditorState.value = true
        }

        btnAlbum.setOnClickListener {
            updateTabSelection(R.id.btnAlbum)
            showFragment(AlbumFragment.newInstance(currentCoupleId, currentUserId ?: "", currentUserName ?: "", currentUserImageUri ?: "", currentTheme))
        }
        btnHome.setOnClickListener {
            updateTabSelection(R.id.btnHome)
            fragmentContainer.visibility = View.GONE
            composeFeed.visibility = View.VISIBLE
            inputArea.visibility = View.VISIBLE
            btnMenuMore.visibility = View.VISIBLE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    composeFeed.visibility = View.VISIBLE
                    inputArea.visibility = View.VISIBLE
                    btnMenuMore.visibility = View.VISIBLE
                    fragmentContainer.visibility = View.GONE
                    updateTabSelection(R.id.btnHome)
                } else {
                    isEnabled = false
                    this@MainActivity.onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        
        updateManager.checkForUpdates(object : UpdateManager.UpdateCallback {
            override fun onUpdateAvailable(url: String) { showUpdateDialog(url) }
            override fun onNoUpdate() {}
            override fun onDownloadProgress(progress: Int) { runOnUiThread { downloadProgressBar.progress = progress } }
            override fun onDownloadComplete() { runOnUiThread { downloadProgressContainer.visibility = View.GONE; updateManager.installApk() } }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.startAllListeners()
        setupOverlays()
        PermissionHelper.checkNotificationAndAlarmPermissions(this)
        try {
            registerReceiver(dndReceiver, IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED))
            viewModel.syncDndStateWithPet()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error registering dndReceiver: " + e.message)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopAllListeners()
        try {
            unregisterReceiver(dndReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun showUpdateDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Actualización disponible")
            .setMessage("Una nueva versión está disponible en GitHub. ¿Deseas descargarla?")
            .setPositiveButton("Descargar") { _, _ ->
                overlayMessageState.value = "Descargando actualización..."
                isUploadingState.value = true
                updateManager.downloadUpdate(url, object : UpdateManager.UpdateCallback {
                    override fun onUpdateAvailable(url: String) {}
                    override fun onNoUpdate() {}
                    override fun onDownloadProgress(progress: Int) { 
                        runOnUiThread { overlayMessageState.value = "Descargando actualización: $progress%" } 
                    }
                    override fun onDownloadComplete() { runOnUiThread { isUploadingState.value = false; updateManager.installApk() } }
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.mainLayout)
        toolbar = findViewById(R.id.toolbar)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        ivToolbarHeart = findViewById(R.id.ivToolbarHeart)
        composeFeed = findViewById(R.id.composeFeed)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnExpand = findViewById(R.id.btnExpand)
        btnMenuMore = findViewById(R.id.btnMenuMore)
        btnRecipes = findViewById(R.id.btnRecipes)
        btnCalendar = findViewById(R.id.btnCalendar)
        btnAlbum = findViewById(R.id.btnAlbum)
        btnProfile = findViewById(R.id.btnProfile)
        btnHome = findViewById(R.id.btnHome)
        btnSettings = findViewById(R.id.btnSettings)
        btnMisc = findViewById(R.id.btnMisc)
        inputArea = findViewById(R.id.inputArea)
        inputContainer = findViewById(R.id.inputContainer)
        bottomActionsBar = findViewById(R.id.bottomActionsBar)
        previewContainer = findViewById(R.id.previewContainer)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        ivPreview = findViewById(R.id.ivPreview)
        btnRemovePreview = findViewById(R.id.btnRemovePreview)
        navBarPadding = findViewById(R.id.navBarPadding)
        downloadProgressContainer = findViewById(R.id.downloadProgressContainer)
        downloadProgressBar = findViewById(R.id.downloadProgressBar)
        toolbarBorder = findViewById(R.id.toolbarBorder)
        bottomActionsBarBorder = findViewById(R.id.bottomActionsBarBorder)
        tvTabHome = findViewById(R.id.tvTabHome)
        tvTabCalendar = findViewById(R.id.tvTabCalendar)
        tvTabAlbum = findViewById(R.id.tvTabAlbum)
        tvTabRecipes = findViewById(R.id.tvTabRecipes)
        tvTabProfile = findViewById(R.id.tvTabProfile)
        tvTabMisc = findViewById(R.id.tvTabMisc)
        tvTabSettings = findViewById(R.id.tvTabSettings)
    }

    private fun showOverflowMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menu.add("Filtrar por fecha")
        popup.setOnMenuItemClickListener { item ->
            if (item.title.toString() == "Filtrar por fecha") {
                showDatePicker()
            }
            true
        }
        popup.show()
    }

    private fun showFragment(f: androidx.fragment.app.Fragment) {
        btnMenuMore.visibility = View.GONE
        composeFeed.visibility = View.GONE
        inputArea.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .addToBackStack(null)
            .commit()
    }

    private fun setupDynamicMargins() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            mainLayout.setPadding(0, statusBarHeight, 0, 0)
            navBarPadding.layoutParams.height = navBarHeight
            navBarPadding.requestLayout()
            insets
        }
    }

    private fun setupOfflineStatusListener() {
        networkStatusTracker = NetworkStatusTracker(this) { isOnline ->
            runOnUiThread { updateConnectionUi(isOnline) }
        }
        networkStatusTracker?.startListening()
    }

    private fun updateConnectionUi(connected: Boolean) {
        if (connected) {
            ivToolbarHeart.clearColorFilter()
            tvToolbarTitle.text = "Nuestro Diario "
        } else {
            ivToolbarHeart.setColorFilter(Color.GRAY)
            tvToolbarTitle.text = "Diario (Sin conexión) "
        }
    }

    private fun enableImmersiveMode() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setupViewModelObservers() {
        viewModel.petState.observe(this) { pet ->
            if (pet != null) {
                petState.value = pet
            }
        }
        viewModel.messagesState.observe(this) { list ->
            if (list != null) {
                android.util.Log.d("DIARIO_DEBUG", "MainActivity LiveData messagesState observer: recibido ${list.size} mensajes")
                messagesState.value = list
            }
        }
        viewModel.themeState.observe(this) { theme ->
            if (theme != null) {
                themeState.value = theme
                applyTheme(theme)
            }
        }
        viewModel.refreshRateState.observe(this) { hz ->
            if (hz != null) {
                applyRefreshRate(hz)
            }
        }
        viewModel.toastMessage.observe(this) { message ->
            if (message != null) {
                showStyledPixelToast(message)
                viewModel.toastMessage.value = null
            }
        }
        viewModel.levelUpEvent.observe(this) { pair ->
            if (pair != null) {
                showStyledPixelToast("¡${pair.first} ha subido al nivel ${pair.second}! 🎉")
                sendNotificationV1("¡${pair.first} subió al Nivel ${pair.second}! ⭐", "¡$currentUserName y tú alcanzaron un nuevo nivel con ${pair.first}!", null, "mascota")
                viewModel.levelUpEvent.value = null
            }
        }
    }

    private fun setupRecyclerView() {
        messages = ArrayList()
        setFeedContent(
            composeFeed,
            messagesState,
            petState,
            currentUserId ?: "",
            themeState,
            editingMessageState,
            showEditorState,
            showPetDialogState,
            currentSelectedImageUrlState,
            { msg: Message -> onMessageClick(null, msg) },
            { msg: Message ->
                if (msg.content != null && msg.content!!.startsWith("[ALBUM]")) {
                    albumManager.showEditAlbumDialog(msg)
                } else {
                    editingMessageState.value = msg
                    currentSelectedImageUrlState.value = msg.imageUrl
                    showEditorState.value = true
                }
            },
            { msg: Message -> viewModel.deleteMessage(msg) },
            { msg: Message ->
                Log.d("DIARIO_DEBUG", "MainActivity onLikeClick recibido para msgId: ${msg.messageId}, liked actual: ${msg.isLiked}")
                viewModel.toggleLikeMessage(msg)
                if (msg.isLiked) {
                    var letterTitle = msg.title
                    if (letterTitle == null || letterTitle.trim().isEmpty()) {
                        letterTitle = "una carta"
                    }
                    val notifTitle = "¡A $currentUserName le encantó! ❤️"
                    val notifBody = "Le dio me gusta a tu carta: \"$letterTitle\""
                    sendNotificationV1(notifTitle, notifBody, msg.imageUrl, "like")
                }
            },
            { title: String, content: String, imageUrl: String? ->
                var m = editingMessageState.value
                val isEdit = m != null
                if (m == null) {
                    m = Message()
                    m.messageId = db.collection("messages").document().id
                    m.authorId = currentUserId
                    m.authorName = currentUserName
                    m.authorImageUrl = currentUserImageUri
                    m.timestamp = System.currentTimeMillis()
                    m.partnerId = currentCoupleId
                }
                m.title = title
                m.content = content
                m.imageUrl = imageUrl
                if (imageUrl != null) {
                    val urls: MutableList<String> = ArrayList()
                    urls.add(imageUrl)
                    m.imageUrls = urls
                }

                viewModel.saveMessageToFirestore(m, isEdit)

                if (!isEdit) {
                    val notifTitle = "Nuevo mensaje de $currentUserName 💌"
                    val notifBody = if (!title.isNullOrEmpty()) "«$title»: $content" else content
                    sendNotificationV1(notifTitle, notifBody, imageUrl, "carta")
                    Toast.makeText(this@MainActivity, "Carta enviada ❤️", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Carta actualizada ✨", Toast.LENGTH_SHORT).show()
                }
            },
            { newName: String -> viewModel.updatePetName(newName) },
            { accessoryId: String, cost: Int -> viewModel.buyAccessory(accessoryId, cost) },
            { accessoryId: String -> viewModel.equipAccessory(accessoryId) },
            { backgroundId: String, cost: Int -> viewModel.buyBackground(backgroundId, cost) },
            { backgroundId: String -> viewModel.equipBackground(backgroundId) },
            { foodId: String, cost: Int, happinessGain: Int -> viewModel.feedPet(foodId, cost, happinessGain) },
            { points: Int, exp: Int -> viewModel.rewardPet(points, exp) },
            { viewModel.togglePetSleep() },
            { pickImage(PICK_IMAGE_CARTA) },
            { viewModel.bathPet() },
            { points: Int, happinessGain: Int -> viewModel.playBallPet(points, happinessGain) },
            { gameType: String, points: Int, exp: Int, score: Int -> viewModel.playMinigame(gameType, points, exp, score) }
        )
    }

    private fun listenMessagesFromFirestore() {
        firestoreListener?.remove()
        var query: Query = db.collection("messages").whereEqualTo("partnerId", currentCoupleId).orderBy("timestamp", Query.Direction.DESCENDING)
        if (selectedFilterDate == null) {
            query = query.limit(100)
        } else {
            val s = selectedFilterDate!!.clone() as Calendar
            s.set(Calendar.HOUR_OF_DAY, 0)
            s.set(Calendar.MINUTE, 0)
            s.set(Calendar.SECOND, 0)
            val e = selectedFilterDate!!.clone() as Calendar
            e.set(Calendar.HOUR_OF_DAY, 23)
            e.set(Calendar.MINUTE, 59)
            e.set(Calendar.SECOND, 59)
            query = query.whereGreaterThanOrEqualTo("timestamp", s.timeInMillis).whereLessThanOrEqualTo("timestamp", e.timeInMillis)
        }
        firestoreListener = query.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e("Firestore", "Error en el listener de mensajes", error)
                return@addSnapshotListener
            }
            if (value != null) {
                val newMessages: MutableList<Message> = ArrayList()
                for (doc in value) {
                    val m = doc.toObject(Message::class.java)
                    m.messageId = doc.id
                    android.util.Log.d("DIARIO_DEBUG", "SnapshotListener: cargado msgId: ${m.messageId}, liked: ${m.liked}, isLiked: ${m.isLiked}")
                    if (m.content == null || !m.content!!.startsWith("[ALBUM]")) {
                        newMessages.add(m)
                    }
                }
                messagesState.value = newMessages
                updateWidget()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopActiveListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startActiveListeners()
        enableImmersiveMode()

        val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("radar_is_sharing", true) && PermissionHelper.hasLocationPermission(this)) {
            ThorRadarService.startService(this)
            ThorRadarManager.publishHeartbeat(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private var sosListener: ListenerRegistration? = null
    private var lastSosTimestamp: Long = 0L

    private fun setupSosEmergencyListener() {
        val partnerDocName = if (ThorRadarManager.isAli(currentUserId, currentUserName)) "kevin" else "ali"
        val partnerDisplayName = if (ThorRadarManager.isAli(currentUserId, currentUserName)) "Kevin" else "Ali"
        val safeCoupleId = ThorRadarManager.normalizeCoupleId(currentCoupleId)

        sosListener?.remove()
        sosListener = db.collection("locations").document(safeCoupleId)
            .collection("users").document(partnerDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val isSos = snapshot.getBoolean("sosActive") ?: false
                    val sosTs = snapshot.getLong("sosTimestamp") ?: 0L
                    if (isSos && sosTs > lastSosTimestamp && (System.currentTimeMillis() - sosTs) < 300_000L) {
                        lastSosTimestamp = sosTs
                        runOnUiThread {
                            val vibrator = getSystemService(Vibrator::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(1200L)
                            }

                            AlertDialog.Builder(this)
                                .setTitle("🚨 ¡ALERTA SOS DE $partnerDisplayName!")
                                .setMessage("¡$partnerDisplayName ha activado la alarma de emergencia en Thor Radar!\n¿Deseas abrir el mapa para ver su ubicación en vivo?")
                                .setCancelable(false)
                                .setPositiveButton("🗺️ VER EN MAPA") { _, _ ->
                                    updateTabSelection(R.id.btnMisc)
                                    showFragment(MiscFragment.newInstance(currentTheme, "radar"))
                                }
                                .setNegativeButton("CERRAR", null)
                                .show()
                        }
                    }
                }
            }
    }

    override fun onDestroy() {
        sosListener?.remove()
        networkStatusTracker?.stopListening()
        try {
            fcmExecutor.shutdown()
        } catch (e: Exception) {
            Log.w("MainActivity", "Error cerrando fcmExecutor: ${e.message}")
        }
        super.onDestroy()
    }

    private fun updateWidget() {
        val wIntent = Intent(this, LastMessageWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val wIds = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(application, LastMessageWidget::class.java))
        wIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds)
        sendBroadcast(wIntent)

        val wLargeIntent = Intent(this, LastMessageLargeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val wLargeIds = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(application, LastMessageLargeWidget::class.java))
        wLargeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wLargeIds)
        sendBroadcast(wLargeIntent)
    }

    private fun ensureUserInFirestore() {
        val userId = currentUserId ?: return
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
                val editor = prefs.edit()
                
                val userUpdates = java.util.HashMap<String, Any?>()
                userUpdates["userId"] = currentUserId
                userUpdates["coupleId"] = currentCoupleId
                if (currentUserName != null && currentUserName!!.isNotEmpty()) {
                    userUpdates["userName"] = currentUserName
                }
                if (currentUserImageUri != null && currentUserImageUri!!.isNotEmpty()) {
                    userUpdates["profileImageUrl"] = currentUserImageUri
                }

                if (documentSnapshot.exists()) {
                    var prefsChanged = false
                    
                    val themeVal = documentSnapshot.getString("theme")
                    if (themeVal != null) {
                        editor.putString("theme", themeVal)
                        prefsChanged = true
                    } else {
                        userUpdates["theme"] = prefs.getString("theme", "Pixel Claro")
                    }
                    
                    val useCustomBgVal = documentSnapshot.getBoolean("useCustomBg")
                    if (useCustomBgVal != null) {
                        editor.putBoolean("useCustomBg", useCustomBgVal)
                        prefsChanged = true
                    } else {
                        userUpdates["useCustomBg"] = prefs.getBoolean("useCustomBg", false)
                    }
                    
                    val lightColorVal = documentSnapshot.getString("lightColor")
                    if (lightColorVal != null) {
                        editor.putString("lightColor", lightColorVal)
                        prefsChanged = true
                    } else {
                        userUpdates["lightColor"] = prefs.getString("lightColor", "#D1C4E9")
                    }
                    
                    val darkColorVal = documentSnapshot.getString("darkColor")
                    if (darkColorVal != null) {
                        editor.putString("darkColor", darkColorVal)
                        prefsChanged = true
                    } else {
                        userUpdates["darkColor"] = prefs.getString("darkColor", "#4A148C")
                    }
                    
                    val cacheLimitVal = documentSnapshot.getLong("cacheSizeLimit")
                    if (cacheLimitVal != null) {
                        editor.putLong("cacheSizeLimit", cacheLimitVal)
                        prefsChanged = true
                    } else {
                        userUpdates["cacheSizeLimit"] = prefs.getLong("cacheSizeLimit", 100L)
                    }
                    
                    val intervalVal = documentSnapshot.getLong("updateInterval")
                    if (intervalVal != null) {
                        editor.putLong("updateInterval", intervalVal)
                        prefsChanged = true
                    } else {
                        userUpdates["updateInterval"] = prefs.getLong("updateInterval", 720L)
                    }
                    
                    val leadTimeVal = documentSnapshot.getLong("appointmentLeadTime")
                    if (leadTimeVal != null) {
                        editor.putLong("appointmentLeadTime", leadTimeVal)
                        prefsChanged = true
                    } else {
                        userUpdates["appointmentLeadTime"] = prefs.getLong("appointmentLeadTime", 60L)
                    }

                    val refreshRateVal = documentSnapshot.getLong("refreshRate")?.toInt()
                    if (refreshRateVal != null) {
                        editor.putInt("refreshRate", refreshRateVal)
                        prefsChanged = true
                    } else {
                        userUpdates["refreshRate"] = prefs.getInt("refreshRate", 90)
                    }
                    
                    if (prefsChanged) {
                        editor.apply()
                        runOnUiThread {
                            val finalTheme = prefs.getString("theme", "Pixel Claro")
                            val lc = prefs.getString("lightColor", "#D1C4E9")
                            val dc = prefs.getString("darkColor", "#4A148C")
                            applyTheme(finalTheme ?: "Pixel Claro", lc, dc)
                            val finalHz = prefs.getInt("refreshRate", 90)
                            applyRefreshRate(finalHz)
                        }
                    }
                } else {
                    userUpdates["theme"] = prefs.getString("theme", "Pixel Claro")
                    userUpdates["useCustomBg"] = prefs.getBoolean("useCustomBg", false)
                    userUpdates["lightColor"] = prefs.getString("lightColor", "#D1C4E9")
                    userUpdates["darkColor"] = prefs.getString("darkColor", "#4A148C")
                    userUpdates["cacheSizeLimit"] = prefs.getLong("cacheSizeLimit", 100L)
                    userUpdates["updateInterval"] = prefs.getLong("updateInterval", 720L)
                    userUpdates["appointmentLeadTime"] = prefs.getLong("appointmentLeadTime", 60L)
                    userUpdates["refreshRate"] = prefs.getInt("refreshRate", 90)
                }

                db.collection("users").document(userId)
                    .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { Log.d("MainActivity", "Usuario y ajustes sincronizados con Firestore") }
                    .addOnFailureListener { e -> Log.e("MainActivity", "Error sincronizando usuario", e) }
            }
            .addOnFailureListener { e -> Log.e("MainActivity", "Error al obtener documento de usuario de Firestore", e) }
    }

    fun updateAllAuthorMessagesWithProfileImage(newUrl: String?) {
        val userId = currentUserId ?: return
        if (newUrl == null || newUrl.trim().isEmpty()) return
        db.collection("messages")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                val batch = db.batch()
                for (doc in queryDocumentSnapshots.documents) {
                    batch.update(doc.reference, "authorImageUrl", newUrl)
                }
                batch.commit()
                    .addOnSuccessListener { Log.d("MainActivity", "Cartas anteriores actualizadas con la nueva foto de perfil") }
                    .addOnFailureListener { e -> Log.e("MainActivity", "Error al actualizar cartas anteriores", e) }
            }
            .addOnFailureListener { e -> Log.e("MainActivity", "Error obteniendo cartas del autor para actualizar foto de perfil", e) }
    }

    private fun setupFirebaseMessaging() {
        val topicName = "diario_" + currentCoupleId.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n").replace(" ", "_")
        FirebaseMessaging.getInstance().subscribeToTopic(topicName)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscrito con éxito al tema: $topicName")
                } else {
                    Log.e("FCM", "Error al suscribirse al tema fcm", task.exception)
                }
            }
    }

    private fun savePetDataToWidgetPrefs(p: Pet) {
        val prefs = getSharedPreferences("thor_widget_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("pet_name", p.name)
            .putInt("pet_level", p.level)
            .putInt("pet_happiness", p.happiness)
            .putString("pet_status", p.status)
            .putString("pet_accessory", if (p.equippedAccessory != null) p.equippedAccessory else "none")
            .putBoolean("pet_sleeping", p.isSleeping)
            .putInt("pet_hunger", p.hunger)
            .putInt("pet_cleanliness", p.cleanliness)
            .apply()
        ThorWidgetProvider.triggerUpdate(this)
    }



    private fun handleUpdateIntent(intent: Intent?) {
        if (intent != null) {
            if (intent.hasExtra("update_url")) {
                val url = intent.getStringExtra("update_url")
                if (url != null) {
                    showUpdateDialog(url)
                }
            }
            if (intent.hasExtra("sync_error_msg")) {
                val errorMsg = intent.getStringExtra("sync_error_msg")
                if (errorMsg != null && errorMsg.isNotEmpty()) {
                    showSyncErrorDialog(errorMsg)
                    intent.removeExtra("sync_error_msg")
                }
            }
            if (intent.hasExtra("click_type")) {
                val clickType = intent.getStringExtra("click_type")
                if (clickType != null) {
                    navigateToClickType(clickType)
                    intent.removeExtra("click_type")
                }
            }
        }
    }

    fun navigateToClickType(clickType: String?) {
        if (clickType == null) return
        runOnUiThread {
            when (clickType.lowercase(Locale.ROOT)) {
                "mascota", "pet", "thor" -> {
                    updateTabSelection(R.id.btnHome)
                    fragmentContainer.visibility = View.GONE
                    composeFeed.visibility = View.VISIBLE
                    inputArea.visibility = View.VISIBLE
                    btnMenuMore.visibility = View.VISIBLE
                    showPetDialogState.value = true
                }
                "carta", "like", "mensaje", "feed" -> {
                    updateTabSelection(R.id.btnHome)
                    fragmentContainer.visibility = View.GONE
                    composeFeed.visibility = View.VISIBLE
                    inputArea.visibility = View.VISIBLE
                    btnMenuMore.visibility = View.VISIBLE
                }
                "receta", "recipe" -> {
                    updateTabSelection(R.id.btnRecipes)
                    showFragment(RecipeFragment.newInstance(currentCoupleId, currentTheme))
                }
                "cita", "calendar", "calendario" -> {
                    updateTabSelection(R.id.btnCalendar)
                    showFragment(CalendarFragment.newInstance(currentCoupleId, currentUserId ?: "", currentTheme))
                }
                "album", "foto", "recuerdo" -> {
                    updateTabSelection(R.id.btnAlbum)
                    showFragment(AlbumFragment.newInstance(currentCoupleId, currentUserId ?: "", currentUserName ?: "", currentUserImageUri ?: "", currentTheme))
                }
                "medicamento", "meds", "remedio", "remedios" -> {
                    updateTabSelection(R.id.btnMisc)
                    showFragment(MiscFragment.newInstance(currentTheme, "meds"))
                }
                "horario", "schedule", "clases" -> {
                    updateTabSelection(R.id.btnMisc)
                    showFragment(MiscFragment.newInstance(currentTheme, "schedule"))
                }
                "anime", "animes" -> {
                    updateTabSelection(R.id.btnMisc)
                    showFragment(MiscFragment.newInstance(currentTheme, "anime"))
                }
                "espiritus", "spirits", "checklist" -> {
                    updateTabSelection(R.id.btnMisc)
                    showFragment(MiscFragment.newInstance(currentTheme, "checklist"))
                }
                "sos", "radar", "thor_radar", "location" -> {
                    updateTabSelection(R.id.btnMisc)
                    showFragment(MiscFragment.newInstance(currentTheme, "radar"))
                }
            }
        }
    }

    fun showSyncErrorDialog(errorMsg: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Error de Sincronización")
            .setMessage("Ocurrió un error al sincronizar las fotos con Google Drive:\n\n$errorMsg")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun sendMessage() {
        val txt = etMessage.text.toString().trim()
        if (txt.isEmpty() && selectedImageUrl == null) return
        
        val imgs: MutableList<String> = ArrayList()
        if (selectedImageUrl != null) imgs.add(selectedImageUrl!!)
        
        val msg = Message(UUID.randomUUID().toString(), currentCoupleId, currentUserId, currentUserName, currentUserImageUri, txt, imgs, System.currentTimeMillis(), false)
        
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))

        saveMessageToFirestore(msg)
        etMessage.setText("")
        selectedImageUrl = null
        previewContainer.visibility = View.GONE
    }

    fun sendNotificationV1(title: String?, messageText: String?, imageUrl: String?) {
        sendNotificationV1(title, messageText, imageUrl, null)
    }

    fun sendNotificationV1(title: String?, messageText: String?, imageUrl: String?, type: String?) {
        fcmExecutor.execute {
            try {
                val credentials = getGoogleCredentials(this@MainActivity)
                val token = credentials.accessToken.tokenValue
                
                val projectId = "diario-pareja-a2d35"
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val jsonBody = JSONObject()
                val message = JSONObject()
                val notification = JSONObject()
                val data = JSONObject()

                notification.put("title", title ?: "Nuevo mensaje de $currentUserName")
                notification.put("body", if (messageText != null && messageText.isNotEmpty()) messageText else "Te han enviado una foto 📸")
                
                data.put("authorId", currentUserId)
                if (imageUrl != null) data.put("imageUrl", imageUrl)
                if (type != null) data.put("click_type", type)

                val topicName = "diario_" + currentCoupleId.lowercase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace("ñ", "n").replace(" ", "_")
                
                val android = JSONObject()
                val androidNotification = JSONObject()
                androidNotification.put("channel_id", "diario_channel")
                android.put("notification", androidNotification)

                message.put("topic", topicName)
                message.put("notification", notification)
                message.put("data", data)
                message.put("android", android)
                jsonBody.put("message", message)

                val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                var responseBody = ""
                if (response.body != null) {
                    responseBody = response.body!!.string()
                }
                val code = response.code
                val finalResp = responseBody
                if (response.isSuccessful) {
                    Log.d("FCM_V1", "Notificación enviada con éxito")
                } else {
                    Log.e("FCM_V1", "Error al enviar notificación: $code - $finalResp")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "⚠️ FCM Error: $code - $finalResp", Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            } catch (e: IOException) {
                Log.e("FCM_V1", "ERROR: No se encontró o no se pudo leer service-account.json en assets. Las notificaciones no se enviarán.")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "🚨 ERROR: No se pudo leer service-account.json en assets.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("FCM_V1", "Error en envío de notificación FCM: " + e.message, e)
            }
        }
    }

    fun sendNotificationV1(messageText: String?, imageUrl: String?) {
        sendNotificationV1("Nuevo mensaje de $currentUserName", messageText, imageUrl)
    }

    fun sendNotificationV1(messageText: String?) {
        sendNotificationV1(messageText, null)
    }

    fun testLocalNotification() {
        Toast.makeText(this, "Enviando notificación de prueba...", Toast.LENGTH_SHORT).show()
        val builder = NotificationCompat.Builder(this, "diario_channel")
            .setSmallIcon(R.drawable.ic_heart_pixel)
            .setContentTitle("Prueba de Diario Pixel 🔔")
            .setContentText("¡Funciona! Esta es una notificación de prueba local.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(999, builder.build())
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_date_filter, null)
        builder.setView(view)

        if (currentTheme == "Pixel Oscuro") {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            view.findViewById<TextView>(R.id.tvFilterTitle).setTextColor(Color.WHITE)
            view.findViewById<Button>(R.id.btnClearFilter).setTextColor(Color.WHITE)
            view.findViewById<Button>(R.id.btnCancelFilter).setTextColor(Color.WHITE)
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvAvailableDates)
        val tsList: MutableList<Long> = ArrayList()
        val adp = DateFilterAdapter(tsList, object : DateFilterAdapter.OnDateSelectedListener {
            override fun onDateSelected(timestamp: Long) {
                selectedFilterDate = Calendar.getInstance().apply { timeInMillis = timestamp }
                listenMessagesFromFirestore()
            }
        })
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adp
        
        db.collection("messages").whereEqualTo("partnerId", currentCoupleId).get().addOnSuccessListener { shots ->
            val seen = HashSet<Long>()
            tsList.clear()
            for (d in shots) {
                val ts = d.getLong("timestamp")
                if (ts != null) {
                    val n = normalizeDate(ts)
                    if (seen.add(n)) tsList.add(n)
                }
            }
            tsList.sortWith { t1, t2 -> t2.compareTo(t1) }
            adp.notifyDataSetChanged()
        }
        val dialog = builder.create()
        view.findViewById<View>(R.id.btnClearFilter).setOnClickListener {
            selectedFilterDate = null
            listenMessagesFromFirestore()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnCancelFilter).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun normalizeDate(ts: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun rescheduleAllCalendarReminders() {
        viewModel.rescheduleAllCalendarReminders()
    }

    override fun showAddEventDialog(date: String, event: CalendarEvent?) {
        try {
            val ts = date.toLong()
            showAddEventDialog(ts, event)
        } catch (e: NumberFormatException) {
            Log.e("MainActivity", "Error parsing date: $date")
        }
    }

    fun showAddEventDialog(ts: Long, edit: CalendarEvent?) {
        val b = AlertDialog.Builder(this)
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_add_calendar_event, null)
        b.setView(v)

        val spinner = v.findViewById<Spinner>(R.id.spinnerRecurrence)
        val displayValues = arrayOf("No repetir", "Diario", "Semanal", "Mensual", "Anual")
        val values = arrayOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY")
        
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, displayValues) {
            @NonNull
            override fun getView(position: Int, @Nullable convertView: View?, @NonNull parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (view is TextView) {
                    view.typeface = androidx.core.content.res.ResourcesCompat.getFont(this@MainActivity, R.font.vt323)
                    if ("Pixel Oscuro" == currentTheme) view.setTextColor(Color.WHITE)
                    else view.setTextColor(Color.parseColor("#4A2511"))
                }
                return view
            }

            override fun getDropDownView(position: Int, @Nullable convertView: View?, @NonNull parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                if (view is TextView) {
                    view.typeface = androidx.core.content.res.ResourcesCompat.getFont(this@MainActivity, R.font.vt323)
                    if ("Pixel Oscuro" == currentTheme) {
                        view.setTextColor(Color.WHITE)
                        view.setBackgroundColor(Color.parseColor("#1A1A2E"))
                    } else {
                        view.setTextColor(Color.parseColor("#4A2511"))
                        view.setBackgroundColor(Color.parseColor("#F3E5AB"))
                    }
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        if (currentTheme == "Pixel Oscuro") {
            v.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            v.findViewById<TextView>(R.id.tvAddEventTitle).setTextColor(Color.WHITE)
            val et1 = v.findViewById<EditText>(R.id.etEventTitle)
            et1.setTextColor(Color.WHITE)
            et1.setHintTextColor(Color.LTGRAY)
            et1.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            
            val et2 = v.findViewById<EditText>(R.id.etEventDescription)
            if (et2 != null) {
                et2.setTextColor(Color.WHITE)
                et2.setHintTextColor(Color.LTGRAY)
                et2.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            }
            ((v.findViewById<View>(R.id.llTime) as android.view.ViewGroup).getChildAt(0) as TextView).setTextColor(Color.WHITE)
            ((v.findViewById<View>(R.id.llRecurrence) as android.view.ViewGroup).getChildAt(0) as TextView).setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnPickTime).setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnSaveEvent).setTextColor(Color.WHITE)
            v.findViewById<Button>(R.id.btnCancelEvent).setTextColor(Color.WHITE)
            spinner.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            spinner.setPopupBackgroundResource(R.drawable.bg_parchment_pixel_dark)
        }

        val et = v.findViewById<EditText>(R.id.etEventTitle)
        val etDesc = v.findViewById<EditText>(R.id.etEventDescription)
        val btnTime = v.findViewById<Button>(R.id.btnPickTime)
        val time = Calendar.getInstance()
        if (edit != null) { 
            et.setText(edit.title) 
            etDesc?.setText(edit.description) 
            time.timeInMillis = edit.date 
            for (i in values.indices) {
                if (values[i] == edit.recurrence) spinner.setSelection(i)
            }
        } else { 
            time.timeInMillis = ts 
            time.set(Calendar.HOUR_OF_DAY, 12) 
            time.set(Calendar.MINUTE, 0) 
        }
        btnTime.text = String.format(Locale.getDefault(), "%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE))
        btnTime.setOnClickListener {
            android.app.TimePickerDialog(this, { _, h, m ->
                time.set(Calendar.HOUR_OF_DAY, h)
                time.set(Calendar.MINUTE, m)
                btnTime.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show()
        }
        val d = b.create()
        v.findViewById<View>(R.id.btnSaveEvent).setOnClickListener { 
            val title = et.text.toString().trim() 
            if (title.isEmpty()) return@setOnClickListener 
            val isNew = (edit == null)
            val id = edit?.eventId ?: UUID.randomUUID().toString() 
            val ev = CalendarEvent(id, title, etDesc?.text?.toString()?.trim() ?: "", time.timeInMillis, currentUserId ?: "", currentCoupleId) 
            ev.authorName = currentUserName ?: ""
            val pos = spinner.selectedItemPosition
            ev.recurrence = if (pos >= 0) values[pos] else "NONE" 
            db.collection("calendar").document(id).set(ev).addOnSuccessListener {
                viewModel.scheduleCalendarReminder(ev)
                val notifTitle = if (isNew) "¡Nueva cita creada! 📅" else "¡Cita modificada! 📅"
                sendNotificationV1(notifTitle, "$currentUserName ${if (isNew) "agregó la cita: \"" else "modificó la cita: \""}ev.title\"", null, "cita")
            }
            d.dismiss() 
        }
        v.findViewById<View>(R.id.btnCancelEvent).setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun isColorLight(color: Int): Boolean {
        val luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)) / 255.0
        return luminance > 0.5
    }

    fun updateTabSelection(activeTabId: Int) {
        this.activeTabId = activeTabId
        val isDark = "Pixel Oscuro" == currentTheme
        val activeColor: Int
        val inactiveColor: Int

        if (isDark) {
            activeColor = Color.parseColor("#FFEB3B")
            inactiveColor = Color.argb(160, 255, 255, 255)
        } else {
            activeColor = Color.parseColor("#4A2511")
            inactiveColor = Color.argb(140, 74, 37, 17)
        }

        btnHome.setColorFilter(if (activeTabId == R.id.btnHome) activeColor else inactiveColor)
        tvTabHome.setTextColor(if (activeTabId == R.id.btnHome) activeColor else inactiveColor)

        btnCalendar.setColorFilter(if (activeTabId == R.id.btnCalendar) activeColor else inactiveColor)
        tvTabCalendar.setTextColor(if (activeTabId == R.id.btnCalendar) activeColor else inactiveColor)

        btnAlbum.setColorFilter(if (activeTabId == R.id.btnAlbum) activeColor else inactiveColor)
        tvTabAlbum.setTextColor(if (activeTabId == R.id.btnAlbum) activeColor else inactiveColor)

        btnRecipes.setColorFilter(if (activeTabId == R.id.btnRecipes) activeColor else inactiveColor)
        tvTabRecipes.setTextColor(if (activeTabId == R.id.btnRecipes) activeColor else inactiveColor)

        btnProfile.setColorFilter(if (activeTabId == R.id.btnProfile) activeColor else inactiveColor)
        tvTabProfile.setTextColor(if (activeTabId == R.id.btnProfile) activeColor else inactiveColor)

        btnMisc.setColorFilter(if (activeTabId == R.id.btnMisc) activeColor else inactiveColor)
        tvTabMisc.setTextColor(if (activeTabId == R.id.btnMisc) activeColor else inactiveColor)

        btnSettings.setColorFilter(if (activeTabId == R.id.btnSettings) activeColor else inactiveColor)
        tvTabSettings.setTextColor(if (activeTabId == R.id.btnSettings) activeColor else inactiveColor)
    }

    fun applyRefreshRate(hz: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }

                if (display != null) {
                    val modes = display.supportedModes
                    val currentMode = display.mode
                    var bestMode: Display.Mode? = null
                    var minDiff = Float.MAX_VALUE

                    // 1. Intentar coincidir con la resolución actual buscando la tasa de refresco más cercana
                    for (mode in modes) {
                        if (mode.physicalWidth == currentMode.physicalWidth && mode.physicalHeight == currentMode.physicalHeight) {
                            val diff = Math.abs(mode.refreshRate - hz.toFloat())
                            if (diff < minDiff) {
                                minDiff = diff
                                bestMode = mode
                            }
                        }
                    }

                    // 2. Si no coincide la resolución exacta, buscar el modo más cercano globalmente
                    if (bestMode == null || minDiff > 3.0f) {
                        for (mode in modes) {
                            val diff = Math.abs(mode.refreshRate - hz.toFloat())
                            if (diff < minDiff) {
                                minDiff = diff
                                bestMode = mode
                            }
                        }
                    }

                    val layoutParams = window.attributes
                    if (bestMode != null && minDiff <= 3.0f) {
                        layoutParams.preferredDisplayModeId = bestMode.modeId
                    }
                    @Suppress("DEPRECATION")
                    layoutParams.preferredRefreshRate = hz.toFloat()
                    window.attributes = layoutParams
                }
            }
            Log.d("MainActivity", "Tasa de refresco configurada a: ${hz}Hz")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al aplicar tasa de refresco a $hz Hz", e)
        }
    }

    override fun applyTheme(theme: String) {
        applyTheme(theme, null, null)
    }

    fun applyTheme(theme: String, lightColor: String?) {
        applyTheme(theme, lightColor, null)
    }

    fun applyTheme(theme: String, lightColor: String?, darkColor: String?) {
        this.currentTheme = theme
        themeState.value = theme
        albumManager.setTheme(theme)
        recipeManager.setTheme(theme)
        messageEditor.setTheme(theme)
        var bg: Int
        var tb: Int
        var inputBg: Int
        var etBg: Int
        var etText: Int
        var etHint: Int
        val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
        val useCustomBg = prefs.getBoolean("useCustomBg", false)
        if (theme == "Pixel Oscuro") {
            val finalDarkColor = darkColor ?: prefs.getString("darkColor", "#4A148C") ?: "#4A148C"
            tb = Color.parseColor(finalDarkColor)
            bg = if (useCustomBg) {
                getDarkColorVariant(tb)
            } else {
                Color.parseColor("#0D0D2B")
            }

            inputBg = when (finalDarkColor.uppercase()) {
                "#0D47A1" -> Color.parseColor("#1976D2")
                "#1B5E20" -> Color.parseColor("#388E3C")
                "#C2185B" -> Color.parseColor("#D81B60")
                "#E65100" -> Color.parseColor("#FB8C00")
                "#006064" -> Color.parseColor("#00838F")
                "#3E2723" -> Color.parseColor("#5D4037")
                else -> Color.parseColor("#6B21A8")
            }

            etBg = Color.parseColor("#1A1A2E")
            etText = Color.WHITE
            etHint = Color.parseColor("#AAAAAA")
        } else {
            val actualLightColor = lightColor ?: prefs.getString("lightColor", "#D1C4E9") ?: "#D1C4E9"
            tb = Color.parseColor(actualLightColor) 
            bg = if (useCustomBg) {
                getPastelColor(tb)
            } else {
                Color.parseColor("#F5E6BE")
            }
            
            inputBg = when (actualLightColor.uppercase()) {
                "#B3E5FC" -> Color.parseColor("#81D4FA")
                "#C8E6C9" -> Color.parseColor("#A5D6A7")
                "#F8BBD0", "#C2185B" -> Color.parseColor("#F48FB1")
                "#FFE0B2" -> Color.parseColor("#FFCC80")
                "#B2EBF2" -> Color.parseColor("#80DEEA")
                "#D7CCC8" -> Color.parseColor("#BCAAA4")
                else -> Color.parseColor("#B39DDB")
            }
            
            etBg = Color.parseColor("#FFFFFF") 
            etText = Color.parseColor("#212121")
            etHint = Color.parseColor("#757575")
        }
        mainLayout.setBackgroundColor(bg) 
        toolbar.setBackgroundColor(tb) 
        bottomActionsBar.setBackgroundColor(tb)
        
        val containerBg = getDrawable(R.drawable.bg_input_container_pixel)
        if (containerBg is android.graphics.drawable.LayerDrawable) {
            val inner = containerBg.findDrawableByLayerId(R.id.inner_solid) as? android.graphics.drawable.GradientDrawable
            inner?.setColor(inputBg)
        }
        inputContainer.background = containerBg
        
        previewContainer.setBackgroundColor(inputBg)
        navBarPadding.setBackgroundColor(tb)
        
        val w = window
        @Suppress("DEPRECATION")
        w.statusBarColor = tb
        @Suppress("DEPRECATION")
        w.navigationBarColor = tb
        
        val controller = WindowCompat.getInsetsController(w, w.decorView)
        val isLight = isColorLight(tb)
        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
        
        val borderColorVal = if (theme == "Pixel Oscuro") Color.parseColor("#91465F") else Color.parseColor("#4A2511")
        toolbarBorder.setBackgroundColor(borderColorVal)
        bottomActionsBarBorder.setBackgroundColor(borderColorVal)

        val toolbarContentColor = if (theme == "Pixel Oscuro") Color.WHITE else Color.parseColor("#4A2511")
        tvToolbarTitle.setTextColor(toolbarContentColor)
        ivToolbarHeart.setColorFilter(toolbarContentColor)
        btnMenuMore.setColorFilter(toolbarContentColor)
        
        updateTabSelection(activeTabId)

        if (theme == "Pixel Oscuro") {
            btnExpand.setBackgroundResource(R.drawable.bg_btn_pixel_small_dark)
            btnSend.setBackgroundResource(R.drawable.bg_btn_pixel_small_dark)
            btnExpand.setColorFilter(Color.WHITE)
            btnSend.setColorFilter(Color.WHITE)
            etMessage.setBackgroundResource(R.drawable.bg_message_pixel_dark)
        } else {
            btnExpand.setBackgroundResource(R.drawable.bg_btn_pixel_small)
            btnSend.setBackgroundResource(R.drawable.bg_btn_pixel_small)
            btnExpand.setColorFilter(Color.parseColor("#4A2511"))
            btnSend.setColorFilter(Color.parseColor("#4A2511"))
            etMessage.setBackgroundResource(R.drawable.bg_message_pixel)
        }

        etMessage.setTextColor(etText)
        etMessage.setHintTextColor(etHint)
    }

    override fun pickImage(requestCode: Int) {
        currentCropType = requestCode
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            if (requestCode == PICK_IMAGE_ALBUM) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImageLauncher.launch(Intent.createChooser(i, "Selecciona imágenes"))
    }

    override fun getCurrentTheme(): String { return currentTheme }

    fun showStyledPixelToast(message: String) {
        PixelToastHelper.showStyledPixelToast(this, message, currentTheme)
    }
    
    fun getUpdateManager(): UpdateManager = updateManager
    fun getDownloadProgressBar(): ProgressBar = downloadProgressBar
    fun getDownloadProgressContainer(): View = downloadProgressContainer

    fun openAddRecipeDialog() {
        recipeManager.showAddRecipeDialog(null)
    }

    fun openRecipeDetailDialog(recipe: Recipe) {
        recipeManager.showRecipeDetailDialog(recipe)
    }

    private var pendingUploads = 0
    private var completedUploads = 0

    private fun upload(uri: Uri?, code: Int) {
        if (uri == null) return
        pendingUploads++
        updateUploadProgress()
        if (code == PICK_IMAGE_ALBUM) {
            albumManager.onAlbumUploadStarted()
        }
        fcmExecutor.execute {
            val optimizedUri = calendario.kevshupp.diariokevinali.compose.compressImageForUpload(this@MainActivity, uri)
            messageEditor.uploadImage(optimizedUri, object : UploadCallback {
                override fun onStart(id: String) {} 
                override fun onProgress(id: String, b: Long, t: Long) {}
                override fun onSuccess(id: String, res: Map<*, *>) {
                    completedUploads++
                    updateUploadProgress()
                    val url = res["secure_url"] as? String ?: ""
                    runOnUiThread {
                        if (code == PICK_IMAGE_PROFILE) { 
                            currentUserImageUri = url 
                            db.collection("users").document(currentUserId ?: "").update("profileImageUrl", url)
                            updateAllAuthorMessagesWithProfileImage(url)
                            val f = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            if (f is ProfileFragment) f.setProfileImage(url)
                        }
                        else if (code == PICK_IMAGE_CARTA) {
                            messageEditor.setImageUrl(url)
                            currentSelectedImageUrlState.value = url
                            selectedImageUrl = url
                        }
                        else if (code == PICK_IMAGE_ALBUM) {
                            albumManager.addImageUrl(url)
                            albumManager.onAlbumUploadFinished()
                        }
                        else if (code == PICK_IMAGE_RECIPE) {
                            recipeManager.setImageUrl(url)
                            val f = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            if (f is RecipeFragment) f.setImageUrl(url)
                        }
                        
                        if (completedUploads == pendingUploads) {
                            Toast.makeText(this@MainActivity, "¡Todas las imágenes subidas!", Toast.LENGTH_SHORT).show()
                            pendingUploads = 0
                            completedUploads = 0
                            updateUploadProgress()
                        }
                    }
                }
                override fun onError(id: String, e: ErrorInfo) {
                    completedUploads++
                    updateUploadProgress()
                    if (code == PICK_IMAGE_ALBUM) {
                        albumManager.onAlbumUploadFinished()
                    }
                    runOnUiThread { Toast.makeText(this@MainActivity, "Error al subir imagen", Toast.LENGTH_SHORT).show() }
                } 
                override fun onReschedule(id: String, e: ErrorInfo) {}
            })
        }
    }

    private fun updateUploadProgress() {
        runOnUiThread {
            val uploading = pendingUploads > 0 && completedUploads < pendingUploads
            isUploadingState.value = uploading
            if (uploading) {
                overlayMessageState.value = "Subiendo imágenes ($completedUploads/$pendingUploads)..."
            }
        }
    }

    private fun startCrop(uri: Uri) {
        val opt = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setFreeStyleCropEnabled(false)
            setHideBottomControls(true)
        }
        val cropIntent = UCrop.of(uri, Uri.fromFile(File(cacheDir, "crop_" + System.currentTimeMillis() + ".jpg")))
            .withAspectRatio(1f, 1f)
            .withOptions(opt)
            .getIntent(this)
        cropImageLauncher.launch(cropIntent)
    }

    override fun logout() { 
        getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit().clear().apply()
        updateWidget()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    fun getAlbumManager(): AlbumManager = albumManager

    fun onMessageClick(v: View?, msg: Message) { 
        if (msg.content != null && msg.content!!.startsWith("[ALBUM]")) {
            albumManager.showAlbumDetail(msg)
        } else {
            messageEditor.showMessageDetail(msg)
        }
    }
    
    fun onMessageLongClick(v: View?, msg: Message) {
        val authorId = msg.authorId ?: ""
        if (authorId == currentUserId) {
            val p = PopupMenu(this, v ?: composeFeed)
            p.menu.add("Editar")
            p.menu.add("Borrar")
            p.setOnMenuItemClickListener { item ->
                if (item.title == "Editar") {
                    if (msg.content != null && msg.content!!.startsWith("[ALBUM]")) {
                        albumManager.showEditAlbumDialog(msg)
                    } else {
                        editingMessageState.value = msg
                        currentSelectedImageUrlState.value = msg.imageUrl
                        showEditorState.value = true
                    }
                } else if (item.title == "Borrar") {
                    db.collection("messages").document(msg.messageId ?: "").delete()
                }
                true
            }
            p.show()
        }
    }
    
    fun onDeleteClick(m: Message) { 
        db.collection("messages").document(m.messageId ?: "").delete() 
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }
    
    private fun saveMessageToFirestore(m: Message) {
        var docId = m.messageId
        if (docId == null) {
            docId = UUID.randomUUID().toString()
            m.messageId = docId
        }
        if (m.partnerId == null) m.partnerId = currentCoupleId
        
        viewModel.saveMessageToFirestore(m, false)
        
        Log.d("Firestore", "Mensaje guardado con éxito: " + m.messageId)
        Toast.makeText(this@MainActivity, "¡Carta enviada!", Toast.LENGTH_SHORT).show()
        sendNotificationV1("¡Carta nueva! 💌", m.content, m.imageUrl, "carta")
    }

    private fun setupOverlays() {
        val overlayCompose = findViewById<ComposeView>(R.id.overlayCompose)
        if (overlayCompose != null) {
            setOverlayContent(overlayCompose, isUploadingState, overlayMessageState)
        }
    }



    private fun getPastelColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = 0.12f
        hsv[2] = 0.98f
        return Color.HSVToColor(hsv)
    }

    private fun getDarkColorVariant(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = 0.6f
        hsv[2] = 0.10f
        return Color.HSVToColor(hsv)
    }

    fun linkGoogleDriveAccount() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
        
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    fun selectLocalFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        localFolderLauncher.launch(intent)
    }
}
