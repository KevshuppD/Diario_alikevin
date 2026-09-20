package calendario.kevshupp.diariokevinali.compose

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt



@Composable
fun ThorRadarScreen(
    theme: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val backgroundColor = getAppBackgroundColor(theme)
    val textColor = if (isDark) Color.White else if (isMono) Color.Black else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else if (isMono) Color.Black else Color(0xFF4A2511)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else if (isMono) Color.White else Color(0xFFFFFBEA)
    val accentColor = if (isDark) Color(0xFFFF80AB) else if (isMono) Color.Black else Color(0xFFE91E63)

    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    var currentUserId by remember { mutableStateOf(prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01") }
    var currentUserName by remember { mutableStateOf(prefs.getString("userName", null)) }
    val coupleId = remember(prefs) { ThorRadarManager.normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123")) }

    val isUserAli = remember(currentUserId, currentUserName) { ThorRadarManager.isAli(currentUserId, currentUserName) }
    val partnerDocName = remember(isUserAli) { if (isUserAli) "kevin" else "ali" }
    val myDocName = remember(isUserAli) { if (isUserAli) "ali" else "kevin" }
    val partnerName = remember(isUserAli) { if (isUserAli) "Kevin" else "Ali" }
    val myDisplayName = remember(isUserAli) { if (isUserAli) "Ali" else "Kevin" }

    // Estados cargados desde caché inmediato para evitar cualquier parpadeo inicial
    var selectedTab by remember { mutableStateOf(0) } // 0: Mapa, 1: Brújula, 2: Zonas, 3: Ajustes
    var myLocationData by remember {
        mutableStateOf(
            ThorRadarManager.loadCachedLocation(context, myDocName)
                ?: RadarLocationData(userId = currentUserId, userName = myDisplayName)
        )
    }
    var partnerLocationData by remember {
        mutableStateOf(
            ThorRadarManager.loadCachedLocation(context, partnerDocName)
                ?: RadarLocationData(userName = partnerName)
        )
    }
    var placeZones by remember {
        mutableStateOf<List<RadarPlaceZone>>(
            ThorRadarManager.loadCachedZonesFromPrefs(context)
        )
    }
    var isSharingLocation by remember { mutableStateOf(prefs.getBoolean("radar_is_sharing", true)) }
    var isBatterySaver by remember { mutableStateOf(prefs.getBoolean("radar_battery_saver", false)) }

    val locationManager = remember(context) { context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager }
    var isGpsEnabled by remember {
        mutableStateOf(
            locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        )
    }
    val activity = context as? android.app.Activity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasBgPermission by remember { mutableStateOf(PermissionHelper.hasBackgroundLocationPermission(context)) }
    var isIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isBannerDismissed by remember { mutableStateOf(prefs.getBoolean("radar_bg_banner_dismissed", false)) }
    var showSetupWizardDialog by remember { mutableStateOf(false) }

    // Re-evaluar permisos y estado de GPS al volver de Ajustes o poner la app en primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasBgPermission = PermissionHelper.hasBackgroundLocationPermission(context)
                isIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                val locMan = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                isGpsEnabled = locMan?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                               locMan?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                if (hasBgPermission && isSharingLocation) {
                    ThorRadarManager.forceLocationUpdate(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showAddEditZoneDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<RadarPlaceZone?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }
    var sosCountdown by remember { mutableStateOf(3) }
    var isSosCountingDown by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Fotos de perfil y carga asíncrona de Bitmaps para los pines del mapa
    var partnerProfileImageUrlFromUsers by remember { mutableStateOf("") }
    var myAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var partnerAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val myImageUrl = remember(myLocationData.profileImageUrl, prefs) {
        val fromLoc = myLocationData.profileImageUrl
        if (fromLoc.isNotBlank()) fromLoc else (prefs.getString("userImage", "") ?: "")
    }

    val partnerImageUrl = remember(partnerLocationData.profileImageUrl, partnerProfileImageUrlFromUsers) {
        val fromLoc = partnerLocationData.profileImageUrl
        if (fromLoc.isNotBlank()) fromLoc else partnerProfileImageUrlFromUsers
    }

    // Escuchar colección users para rescatar la foto de la pareja si aún no viene en su locationData
    DisposableEffect(coupleId, partnerDocName) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("users").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                for (doc in snapshot.documents) {
                    val uid = doc.getString("userId") ?: doc.id
                    val uname = doc.getString("userName") ?: ""
                    val isPartner = if (isUserAli) {
                        uname.contains("Kevin", ignoreCase = true) || uid.contains("kevin", ignoreCase = true)
                    } else {
                        uname.contains("Ali", ignoreCase = true) || uid.contains("ali", ignoreCase = true)
                    }
                    if (isPartner) {
                        val img = doc.getString("profileImageUrl") ?: ""
                        if (img.isNotBlank()) {
                            partnerProfileImageUrlFromUsers = img
                        }
                    }
                }
            }
        }
        onDispose { listener.remove() }
    }

    // Cargar Bitmap de mi avatar para el marcador del mapa
    LaunchedEffect(myImageUrl) {
        if (myImageUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(myImageUrl)
                        .allowHardware(false)
                        .size(160, 160)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    if (result is BitmapDrawable) {
                        myAvatarBitmap = result.bitmap
                    }
                } catch (e: Exception) {
                    // Fallback a emoji si falla
                }
            }
        }
    }

    // Cargar Bitmap del avatar de mi pareja para el marcador del mapa
    LaunchedEffect(partnerImageUrl) {
        if (partnerImageUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(partnerImageUrl)
                        .allowHardware(false)
                        .size(160, 160)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    if (result is BitmapDrawable) {
                        partnerAvatarBitmap = result.bitmap
                    }
                } catch (e: Exception) {
                    // Fallback a emoji si falla
                }
            }
        }
    }

    // Configurar Osmdroid y servicio de fondo
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        // Iniciar servicio en background si está habilitado el compartir
        if (isSharingLocation && PermissionHelper.hasLocationPermission(context)) {
            ThorRadarService.startService(context)
        }
        // Emitir ubicación fresca una sola vez
        ThorRadarManager.forceLocationUpdate(context)

        // Enfoque On-Demand: Al abrir la pantalla de Radar, solicitar ubicación fresca a la pareja vía Magic Packet WOL
        ThorRadarManager.sendLocationRequestPing(
            context = context,
            coupleId = coupleId,
            senderId = currentUserId,
            senderName = myDisplayName,
            partnerName = partnerName
        )
    }

    // Escuchar datos de Firestore en tiempo real
    DisposableEffect(coupleId, myDocName, partnerDocName) {
        val db = FirebaseFirestore.getInstance()
        val locRef = db.collection("locations").document(coupleId)

        // Escuchar mi ubicación
        val myListener: ListenerRegistration = locRef.collection("users").document(myDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val data = RadarLocationData.fromDocument(snapshot)
                    myLocationData = data
                    ThorRadarManager.saveCachedLocation(context, myDocName, data)
                }
            }

        // Escuchar ubicación de la pareja
        val partnerListener: ListenerRegistration = locRef.collection("users").document(partnerDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val data = RadarLocationData.fromDocument(snapshot)
                    partnerLocationData = data
                    ThorRadarManager.saveCachedLocation(context, partnerDocName, data)
                }
            }

        // Escuchar Zonas Seguras
        val zonesListener: ListenerRegistration = locRef.collection("zones")
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val list = snapshots.documents.mapNotNull { doc ->
                        RadarPlaceZone.fromMap(doc.data?.plus("id" to doc.id))
                    }
                    placeZones = list
                    ThorRadarManager.setCachedZones(list)
                    ThorRadarManager.saveCachedZonesToPrefs(context, list)
                }
            }

        // Escuchar solicitudes de ping remoto en tiempo real vía Firestore
        val pingListener: ListenerRegistration = locRef.collection("pings").document(myDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val reqTime = snapshot.getLong("requestedAt") ?: 0L
                    if (reqTime > 0L && (System.currentTimeMillis() - reqTime) < 60_000L) {
                        Log.d("ThorRadarCompose", "⚡ Solicitud de ping recibida vía Firestore. Actualizando ubicación...")
                        ThorRadarManager.handleMagicLocationPing(context)
                    }
                }
            }

        onDispose {
            myListener.remove()
            partnerListener.remove()
            zonesListener.remove()
            pingListener.remove()
        }
    }

    // Tracking de ubicación mientras la pantalla está activa
    LaunchedEffect(isSharingLocation) {
        if (isSharingLocation && PermissionHelper.hasLocationPermission(context)) {
            ThorRadarManager.startLiveTracking(context, 8_000L, isForeground = true)
        } else {
            ThorRadarManager.stopLiveTracking()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
            val isSharing = prefs.getBoolean("radar_is_sharing", true)
            if (isSharing && PermissionHelper.hasLocationPermission(context)) {
                val isBatterySaver = prefs.getBoolean("radar_battery_saver", false)
                val interval = if (isBatterySaver) 60_000L else 30_000L
                ThorRadarManager.startLiveTracking(context, interval, isForeground = false)
            } else {
                ThorRadarManager.stopLiveTracking()
            }
        }
    }

    // Cálculo de Distancia y Rumbo
    val distanceMeters = remember(myLocationData, partnerLocationData) {
        if (myLocationData.latitude != 0.0 && partnerLocationData.latitude != 0.0) {
            ThorRadarManager.calculateDistance(
                myLocationData.latitude, myLocationData.longitude,
                partnerLocationData.latitude, partnerLocationData.longitude
            )
        } else {
            0f
        }
    }

    val bearingDegrees = remember(myLocationData, partnerLocationData) {
        if (myLocationData.latitude != 0.0 && partnerLocationData.latitude != 0.0) {
            ThorRadarManager.calculateBearing(
                myLocationData.latitude, myLocationData.longitude,
                partnerLocationData.latitude, partnerLocationData.longitude
            )
        } else {
            0f
        }
    }

    val directionName = remember(bearingDegrees) {
        ThorRadarManager.getBearingDirectionName(bearingDegrees)
    }

    val isTogether = distanceMeters in 0.1f..60f

    // Vibración y alerta si la pareja tiene SOS activo
    LaunchedEffect(partnerLocationData.sosActive) {
        if (partnerLocationData.sosActive) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(1000L)
            }
        }
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Cabecera Toolbar Retro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< ATRÁS",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🧭 THOR RADAR",
                fontFamily = Vt323,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            // Badge de Estado de Rastreo
            Box(
                modifier = Modifier
                    .border(2.dp, borderColor)
                    .background(if (isSharingLocation) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                    .clickable { selectedTab = 3 }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isSharingLocation) "🔴 EN VIVO" else "🛑 APAGADO",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }


    // Banner de Alerta SOS si la pareja activó emergencia
    if (partnerLocationData.sosActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(3.dp, Color.Red)
                .background(Color(0xFFFFEBEE))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🚨", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡ALERTA SOS DE $partnerName!",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = "Ha activado el botón de emergencia. Revisa su ubicación.",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = Color(0xFFB71C1C)
                    )
                }
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("VER", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }

    // Banner de GPS Apagado si el sensor de ubicación está desactivado en el teléfono
    if (!isGpsEnabled) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(2.dp, Color.Red)
                .background(if (isDark) Color(0xFF330A0A) else Color(0xFFFFEBEE))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPS / UBICACIÓN DESACTIVADA",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = "El sensor de ubicación de tu teléfono está apagado. Actívalo para que tu pareja pueda verte.",
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("ACTIVAR GPS", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }

    // Banner de Permiso en Segundo Plano si no está activado "Todo el tiempo" y no fue cerrado manualmente
    if (!hasBgPermission && !isBannerDismissed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(2.dp, Color(0xFFFF9800))
                .background(if (isDark) Color(0xFF332005) else Color(0xFFFFF3E0))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🛰️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RASTREO CON APP CERRADA (TODO EL TIEMPO)",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                    )
                    Text(
                        text = "Para que tu pareja vea tu ubicación aun con la app cerrada, activa 'Permitir todo el tiempo'.",
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        if (activity != null) {
                            PermissionHelper.requestBackgroundLocationPermission(activity)
                        } else {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("ACTIVAR", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✕",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable {
                            isBannerDismissed = true
                            prefs.edit().putBoolean("radar_bg_banner_dismissed", true).apply()
                        }
                        .padding(4.dp)
                )
            }
        }
    }

    // Banner cuando Thor Radar está Apagado (Solo visible fuera de la pestaña Ajustes)
    if (!isSharingLocation && selectedTab != 3) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(2.dp, Color(0xFFD32F2F))
                .background(if (isDark) Color(0xFF330A0A) else Color(0xFFFFEBEE))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🛑", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "THOR RADAR APAGADO (0% BATERÍA)",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = "El servicio de rastreo GPS está detenido para ahorrar batería.",
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        isSharingLocation = true
                        prefs.edit().putBoolean("radar_is_sharing", true).apply()
                        if (PermissionHelper.hasLocationPermission(context)) {
                            ThorRadarService.startService(context)
                            ThorRadarManager.forceLocationUpdate(context)
                            Toast.makeText(context, "⚡ Thor Radar encendido. Rastreo activado.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠️ Se requieren permisos de ubicación.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("ENCENDER", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }

    // Tarjeta Resumen de la Pareja (Live Partner Card) - Oculta en pestaña Ajustes para dar máximo espacio
    if (selectedTab != 3) {
        PartnerLiveCard(
            partnerName = partnerName,
            partnerData = partnerLocationData,
            partnerImageUrl = partnerImageUrl,
            distanceMeters = distanceMeters,
            directionName = directionName,
            isTogether = isTogether,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            accentColor = accentColor,
            onNavigate = {
                if (partnerLocationData.latitude != 0.0) {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${partnerLocationData.latitude},${partnerLocationData.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val geoUri = Uri.parse("geo:${partnerLocationData.latitude},${partnerLocationData.longitude}?q=${partnerLocationData.latitude},${partnerLocationData.longitude}($partnerName)")
                        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                    }
                } else {
                    Toast.makeText(context, "Ubicación de $partnerName aún no disponible", Toast.LENGTH_SHORT).show()
                }
            },
            onPingPartner = {
                Toast.makeText(context, "⚡ Sincronizando ubicación de $partnerName...", Toast.LENGTH_SHORT).show()
                ThorRadarManager.sendLocationRequestPing(
                    context = context,
                    coupleId = coupleId,
                    senderId = currentUserId,
                    senderName = myDisplayName,
                    partnerName = partnerName
                ) { fcmSuccess ->
                    // El Firestore ping ya fue enviado independientemente del FCM.
                    // Mostrar confirmación siempre — si el FCM falló, la señal llegó igual
                    // vía Firestore a la app de la pareja si estaba abierta.
                    Toast.makeText(
                        context,
                        if (fcmSuccess) "📡 Señal enviada: actualizando radar de $partnerName..."
                        else "📡 Señal Firestore enviada a $partnerName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

        // Pestañas / Selector de Módulos Retro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("🗺️ MAPA", "🧭 BRÚJULA", "🏠 ZONAS", "⚙️ AJUSTES")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, if (isSelected) accentColor else borderColor)
                        .background(if (isSelected) (if (isDark) Color(0xFF381E2F) else Color(0xFFFFD1DC)) else cardBg)
                        .clickable { selectedTab = index }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) (if (isDark) accentColor else Color(0xFF880E4F)) else textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Contenido según Pestaña (Enmarcado estricto)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> RadarMapView(
                    myLocation = myLocationData,
                    partnerLocation = partnerLocationData,
                    myAvatarBitmap = myAvatarBitmap,
                    partnerAvatarBitmap = partnerAvatarBitmap,
                    zones = placeZones,
                    theme = theme,
                    partnerName = partnerName,
                    userName = myDisplayName,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    textColor = textColor,
                    accentColor = accentColor,
                    onOpenAddZone = {
                        editingZone = null
                        showAddEditZoneDialog = true
                    },
                    onEditZone = { zone ->
                        editingZone = zone
                        showAddEditZoneDialog = true
                    },
                    onTriggerSos = {
                        isSosCountingDown = true
                        sosCountdown = 3
                        showSosDialog = true
                    }
                )
                1 -> RadarCompassView(
                    bearingDegrees = bearingDegrees,
                    distanceMeters = distanceMeters,
                    directionName = directionName,
                    partnerName = partnerName,
                    isTogether = isTogether,
                    theme = theme,
                    textColor = textColor,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    accentColor = accentColor
                )
                2 -> RadarZonesView(
                    zones = placeZones,
                    coupleId = coupleId,
                    theme = theme,
                    textColor = textColor,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    accentColor = accentColor,
                    onAddZoneClick = {
                        editingZone = null
                        showAddEditZoneDialog = true
                    },
                    onEditZoneClick = { zone ->
                        editingZone = zone
                        showAddEditZoneDialog = true
                    }
                )
                3 -> RadarSettingsView(
                    isSharing = isSharingLocation,
                    isBatterySaver = isBatterySaver,
                    myLocation = myLocationData,
                    currentUserId = currentUserId,
                    myDisplayName = myDisplayName,
                    partnerName = partnerName,
                    coupleId = coupleId,
                    theme = theme,
                    textColor = textColor,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    accentColor = accentColor,
                    onToggleSharing = { enabled ->
                        isSharingLocation = enabled
                        prefs.edit().putBoolean("radar_is_sharing", enabled).apply()
                        if (enabled) {
                            if (PermissionHelper.hasLocationPermission(context)) {
                                ThorRadarService.startService(context)
                                ThorRadarManager.forceLocationUpdate(context)
                                Toast.makeText(context, "⚡ Thor Radar encendido. Rastreo activado.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "⚠️ Se requieren permisos de ubicación para activar el radar", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            ThorRadarService.stopService(context)
                            ThorRadarManager.stopLiveTracking()
                            ThorRadarManager.publishHeartbeat(context)
                            Toast.makeText(context, "🛑 Thor Radar apagado. Servicio detenido para ahorrar batería.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleBatterySaver = { enabled ->
                        isBatterySaver = enabled
                        prefs.edit().putBoolean("radar_battery_saver", enabled).apply()
                        if (isSharingLocation) {
                            ThorRadarService.startService(context)
                        }
                    },
                    isGpsEnabled = isGpsEnabled,
                    hasBgLoc = hasBgPermission,
                    isIgnoringBattery = isIgnoringBattery,
                    onOpenSetupWizard = { showSetupWizardDialog = true }
                )
            }
        }
    }

    // Diálogo para Añadir / Editar Zona Segura
    if (showAddEditZoneDialog) {
        AddEditZoneDialog(
            coupleId = coupleId,
            userId = currentUserId,
            currentLat = myLocationData.latitude,
            currentLng = myLocationData.longitude,
            existingZone = editingZone,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            accentColor = accentColor,
            onDismiss = {
                showAddEditZoneDialog = false
                editingZone = null
            }
        )
    }

    // Diálogo SOS con cuenta atrás
    if (showSosDialog) {
        SosCountdownDialog(
            mySosActive = myLocationData.sosActive,
            partnerName = partnerName,
            onTriggerSos = {
                ThorRadarManager.triggerSos(context, coupleId, currentUserId, myDisplayName)
                showSosDialog = false
                Toast.makeText(context, "🚨 ALERTA SOS ENVIADA A $partnerName", Toast.LENGTH_LONG).show()
            },
            onCancel = {
                showSosDialog = false
            },
            onDeactivateSos = {
                ThorRadarManager.cancelSos(coupleId, currentUserId)
                showSosDialog = false
                Toast.makeText(context, "Alerta SOS desactivada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Diálogo Asistente de Configuración Requerida
    if (showSetupWizardDialog) {
        RadarSetupWizardDialog(
            isSharing = isSharingLocation,
            isGpsEnabled = isGpsEnabled,
            hasBgPermission = hasBgPermission,
            isIgnoringBattery = isIgnoringBattery,
            onToggleSharing = { enabled ->
                isSharingLocation = enabled
                prefs.edit().putBoolean("radar_is_sharing", enabled).apply()
                if (enabled) {
                    if (PermissionHelper.hasLocationPermission(context)) {
                        ThorRadarService.startService(context)
                        ThorRadarManager.forceLocationUpdate(context)
                    }
                } else {
                    ThorRadarService.stopService(context)
                    ThorRadarManager.stopLiveTracking()
                    ThorRadarManager.publishHeartbeat(context)
                }
            },
            onEnableGps = {
                try {
                    context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    PermissionHelper.openAppSettings(context)
                }
            },
            onRequestBgPermission = {
                if (activity != null) {
                    PermissionHelper.requestBackgroundLocationPermission(activity)
                } else {
                    PermissionHelper.openAppSettings(context)
                }
            },
            onRequestIgnoreBattery = {
                PermissionHelper.requestIgnoreBatteryOptimizations(context)
                isIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
            },
            onDismiss = {
                showSetupWizardDialog = false
                prefs.edit().putBoolean("radar_setup_wizard_dismissed", true).apply()
            },
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            accentColor = accentColor
        )
    }
}

