package calendario.kevshupp.diariokevinali.compose

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Google Maps Estándar (El mapa clásico y limpio de Google)
private val GOOGLE_MAPS_TILES = object : OnlineTileSourceBase(
    "Google-Maps-Road",
    0,
    20,
    256,
    "",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=m&hl=es",
        "https://mt1.google.com/vt/lyrs=m&hl=es",
        "https://mt2.google.com/vt/lyrs=m&hl=es",
        "https://mt3.google.com/vt/lyrs=m&hl=es"
    ),
    "© Google Maps"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl&x=$x&y=$y&z=$zoom"
    }
}

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

    // Estados
    var selectedTab by remember { mutableStateOf(0) } // 0: Mapa, 1: Brújula, 2: Zonas, 3: Ajustes
    var myLocationData by remember { mutableStateOf(RadarLocationData(userId = currentUserId, userName = myDisplayName)) }
    var partnerLocationData by remember { mutableStateOf(RadarLocationData(userName = partnerName)) }
    var placeZones by remember { mutableStateOf<List<RadarPlaceZone>>(emptyList()) }
    var isSharingLocation by remember { mutableStateOf(prefs.getBoolean("radar_is_sharing", true)) }
    var isBatterySaver by remember { mutableStateOf(prefs.getBoolean("radar_battery_saver", false)) }

    val locationManager = remember(context) { context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager }
    var isGpsEnabled by remember {
        mutableStateOf(
            locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        )
    }

    var showAddZoneDialog by remember { mutableStateOf(false) }
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
        // Emitir latido inmediato con batería y ubicación
        ThorRadarManager.publishHeartbeat(context)
        ThorRadarManager.forceLocationUpdate(context)
    }

    // Escuchar datos de Firestore en tiempo real
    DisposableEffect(coupleId, myDocName, partnerDocName) {
        val db = FirebaseFirestore.getInstance()
        val locRef = db.collection("locations").document(coupleId)

        // Escuchar mi ubicación
        val myListener: ListenerRegistration = locRef.collection("users").document(myDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    myLocationData = RadarLocationData.fromDocument(snapshot)
                }
            }

        // Escuchar ubicación de la pareja
        val partnerListener: ListenerRegistration = locRef.collection("users").document(partnerDocName)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    partnerLocationData = RadarLocationData.fromDocument(snapshot)
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
                }
            }

        onDispose {
            myListener.remove()
            partnerListener.remove()
            zonesListener.remove()
        }
    }

    // Actualización periódica en primer plano
    LaunchedEffect(isSharingLocation) {
        if (isSharingLocation && PermissionHelper.hasLocationPermission(context)) {
            ThorRadarManager.forceLocationUpdate(context)
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
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
                    .background(if (isSharingLocation) Color(0xFF2E7D32) else Color(0xFF757575))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isSharingLocation) "🔴 EN VIVO" else "💤 PAUSA",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

    val activity = context as? android.app.Activity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasBgPermission by remember { mutableStateOf(PermissionHelper.hasBackgroundLocationPermission(context)) }
    var isBannerDismissed by remember { mutableStateOf(prefs.getBoolean("radar_bg_banner_dismissed", false)) }

    // Re-evaluar permisos y estado de GPS al volver de Ajustes o poner la app en primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasBgPermission = PermissionHelper.hasBackgroundLocationPermission(context)
                val locMan = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                isGpsEnabled = locMan?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                               locMan?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                if (hasBgPermission) {
                    ThorRadarManager.forceLocationUpdate(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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

        // Tarjeta Resumen de la Pareja (Live Partner Card)
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
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

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
                    onOpenAddZone = { showAddZoneDialog = true },
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
                    onAddZoneClick = { showAddZoneDialog = true }
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
                            ThorRadarService.startService(context)
                        } else {
                            ThorRadarService.stopService(context)
                        }
                    },
                    onToggleBatterySaver = { enabled ->
                        isBatterySaver = enabled
                        prefs.edit().putBoolean("radar_battery_saver", enabled).apply()
                        if (isSharingLocation) {
                            ThorRadarService.startService(context)
                        }
                    }
                )
            }
        }
    }

    // Diálogo para Añadir Zona Segura
    if (showAddZoneDialog) {
        AddZoneDialog(
            coupleId = coupleId,
            userId = currentUserId,
            currentLat = myLocationData.latitude,
            currentLng = myLocationData.longitude,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            accentColor = accentColor,
            onDismiss = { showAddZoneDialog = false }
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
}

@Composable
fun PartnerLiveCard(
    partnerName: String,
    partnerData: RadarLocationData,
    partnerImageUrl: String = "",
    distanceMeters: Float,
    directionName: String,
    isTogether: Boolean,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onNavigate: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val hasValidData = partnerData.timestamp > 0L && partnerData.latitude != 0.0
    val isOnline = hasValidData && (System.currentTimeMillis() - partnerData.timestamp) < 600_000L // Activo en los últimos 10 min

    val distanceText = when {
        !hasValidData -> "Esperando señal GPS de $partnerName..."
        isTogether -> "¡Juntos en el mismo lugar! ✨"
        distanceMeters >= 1000f -> "${String.format(Locale.US, "%.1f", distanceMeters / 1000f)} km ($directionName)"
        distanceMeters > 0f -> "${distanceMeters.roundToInt()} m ($directionName)"
        else -> "Calculando distancia..."
    }

    val activityIcon = when {
        !hasValidData -> "📡 Desconectado"
        partnerData.activity == "IN_VEHICLE" -> "🚗 Auto (${partnerData.speedKmh.roundToInt()} km/h)"
        partnerData.activity == "WALKING" -> "🚶 Caminando"
        else -> "🏠 En reposo"
    }

    val timeAgo = remember(partnerData.timestamp) {
        if (partnerData.timestamp == 0L) "Sin conexión aún"
        else {
            val diffSec = (System.currentTimeMillis() - partnerData.timestamp) / 1000
            when {
                diffSec < 60 -> "Hace un momento"
                diffSec < 3600 -> "Hace ${diffSec / 60}m"
                diffSec < 86400 -> "Hace ${diffSec / 3600}h"
                else -> "Hace ${diffSec / 86400}d"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor)
            .background(cardBg)
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Pixel / Foto con Badge de estado
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, if (partnerName == "Ali") Color(0xFFFF80AB) else Color(0xFF64B5F6))
                        .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFE8E8E8))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    if (partnerImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = partnerImageUrl,
                            contentDescription = partnerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (partnerName == "Ali") "👧" else "👦",
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = partnerName.uppercase(),
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "Activo" else timeAgo,
                            fontFamily = Vt323,
                            fontSize = 13.sp,
                            color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }

                    // Lugar / Dirección
                    val placeDisplay = when {
                        !hasValidData -> "Esperando señal de $partnerName..."
                        partnerData.currentZone.isNotEmpty() -> "En: ${partnerData.currentZone}"
                        partnerData.address.isNotEmpty() -> partnerData.address
                        else -> "Ubicación en GPS"
                    }

                    Text(
                        text = "📍 $placeDisplay",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Batería
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (partnerData.isCharging) "⚡" else "🔋",
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (partnerData.timestamp == 0L) "--%" else "${partnerData.batteryLevel}%",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                partnerData.timestamp == 0L -> textColor.copy(alpha = 0.5f)
                                partnerData.isCharging -> Color(0xFF4CAF50)
                                partnerData.batteryLevel <= 20 -> Color.Red
                                else -> textColor
                            }
                        )
                    }
                    Text(
                        text = if (partnerData.isCharging) "⚡ Cargando" else activityIcon,
                        fontFamily = Vt323,
                        fontSize = 12.sp,
                        fontWeight = if (partnerData.isCharging) FontWeight.Bold else FontWeight.Normal,
                        color = if (partnerData.isCharging) Color(0xFF4CAF50) else textColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fila de Distancia y Botón Cómo Llegar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF2A1B24) else Color(0xFFFFEEF2))
                    .border(1.dp, borderColor.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "❤️", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distanceText,
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFF80AB) else Color(0xFFC2185B)
                    )
                }

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = "🗺️ CÓMO LLEGAR",
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RadarMapView(
    myLocation: RadarLocationData,
    partnerLocation: RadarLocationData,
    myAvatarBitmap: Bitmap? = null,
    partnerAvatarBitmap: Bitmap? = null,
    zones: List<RadarPlaceZone>,
    theme: String,
    partnerName: String,
    userName: String,
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    accentColor: Color,
    onOpenAddZone: () -> Unit,
    onTriggerSos: () -> Unit
) {
    val context = LocalContext.current
    val isDark = theme == "Pixel Oscuro"
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var hasAutoCentered by remember { mutableStateOf(false) }

    fun centerBothLocations(animate: Boolean = true) {
        val map = mapViewInstance ?: return
        val hasMy = myLocation.latitude != 0.0 && myLocation.longitude != 0.0
        val hasPartner = partnerLocation.latitude != 0.0 && partnerLocation.longitude != 0.0

        if (hasMy && hasPartner) {
            val minLat = minOf(myLocation.latitude, partnerLocation.latitude)
            val maxLat = maxOf(myLocation.latitude, partnerLocation.latitude)
            val minLon = minOf(myLocation.longitude, partnerLocation.longitude)
            val maxLon = maxOf(myLocation.longitude, partnerLocation.longitude)

            val latDiff = maxLat - minLat
            val lonDiff = maxLon - minLon

            val latPad = maxOf(latDiff * 0.35, 0.005)
            val lonPad = maxOf(lonDiff * 0.35, 0.005)

            val box = BoundingBox(
                maxLat + latPad,
                maxLon + lonPad,
                minLat - latPad,
                minLon - lonPad
            )
            map.zoomToBoundingBox(box, animate, 90)
        } else if (hasPartner) {
            if (animate) map.controller.animateTo(GeoPoint(partnerLocation.latitude, partnerLocation.longitude))
            else map.controller.setCenter(GeoPoint(partnerLocation.latitude, partnerLocation.longitude))
            map.controller.setZoom(16.0)
        } else if (hasMy) {
            if (animate) map.controller.animateTo(GeoPoint(myLocation.latitude, myLocation.longitude))
            else map.controller.setCenter(GeoPoint(myLocation.latitude, myLocation.longitude))
            map.controller.setZoom(16.0)
        }
    }

    // Auto-centrar en ambas ubicaciones la primera vez que se cargan
    LaunchedEffect(myLocation.latitude, partnerLocation.latitude, mapViewInstance) {
        if (!hasAutoCentered && mapViewInstance != null && (myLocation.latitude != 0.0 || partnerLocation.latitude != 0.0)) {
            centerBothLocations(false)
            hasAutoCentered = true
        }
    }

    // Ciclo de vida del MapView
    DisposableEffect(Unit) {
        onDispose {
            mapViewInstance?.onPause()
            mapViewInstance?.onDetach()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, borderColor)
            .background(cardBg)
            .clipToBounds()
    ) {
        // Barra Superior del Marco de Mapa
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFEADBBE))
                .border(1.dp, borderColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🛰️ RADAR SATELITAL", fontFamily = Vt323, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
            }

            Text(
                text = if (myLocation.accuracy > 0) "PRECISIÓN: ±${myLocation.accuracy.roundToInt()}m" else "GPS ACTIVO",
                fontFamily = Vt323,
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }

        // Contenedor del Mapa estrictamente contenido con clip
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(GOOGLE_MAPS_TILES)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        if (isDark) {
                            val matrix = ColorMatrix(floatArrayOf(
                                -0.85f, 0f, 0f, 0f, 240f,
                                0f, -0.85f, 0f, 0f, 240f,
                                0f, 0f, -0.75f, 0f, 255f,
                                0f, 0f, 0f, 1f, 0f
                            ))
                            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
                        } else {
                            overlayManager.tilesOverlay.setColorFilter(null)
                        }

                        val startPoint = if (partnerLocation.latitude != 0.0) {
                            GeoPoint(partnerLocation.latitude, partnerLocation.longitude)
                        } else if (myLocation.latitude != 0.0) {
                            GeoPoint(myLocation.latitude, myLocation.longitude)
                        } else {
                            GeoPoint(-33.4489, -70.6693) // Santiago fallback
                        }
                        controller.setCenter(startPoint)
                        mapViewInstance = this
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // Dibujar Zonas Seguras
                    zones.forEach { zone ->
                        if (zone.latitude != 0.0) {
                            val circlePoints = Polygon.pointsAsCircle(
                                GeoPoint(zone.latitude, zone.longitude),
                                zone.radiusMeters.toDouble()
                            )
                            val polygon = Polygon(mapView).apply {
                                points = circlePoints
                                fillPaint.color = android.graphics.Color.argb(40, 233, 30, 99)
                                outlinePaint.color = android.graphics.Color.argb(160, 233, 30, 99)
                                outlinePaint.strokeWidth = 3f
                                title = "${zone.icon} ${zone.name}"
                            }
                            mapView.overlays.add(polygon)

                            // Marcador de la Zona
                            val zoneMarker = Marker(mapView).apply {
                                position = GeoPoint(zone.latitude, zone.longitude)
                                title = "${zone.icon} ${zone.name}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = BitmapDrawable(context.resources, createTextMarkerBitmap(zone.icon, 32))
                            }
                            mapView.overlays.add(zoneMarker)
                        }
                    }

                    // Marcador de Mi Ubicación (Kevin o Ali) con foto de perfil
                    if (myLocation.latitude != 0.0) {
                        val myMarker = Marker(mapView).apply {
                            position = GeoPoint(myLocation.latitude, myLocation.longitude)
                            title = "Tú ($userName)"
                            snippet = "Batería: ${myLocation.batteryLevel}%${if (myLocation.isCharging) " ⚡ (Cargando)" else ""}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            val avatarChar = if (userName.contains("Ali", ignoreCase = true)) "👧" else "👦"
                            icon = BitmapDrawable(
                                context.resources,
                                createAvatarMarkerBitmap(
                                    avatarBitmap = myAvatarBitmap,
                                    avatarEmoji = avatarChar,
                                    name = "Tú",
                                    colorArgb = android.graphics.Color.parseColor("#1976D2")
                                )
                            )
                        }
                        mapView.overlays.add(myMarker)
                    }

                    // Marcador de la Pareja con foto de perfil
                    if (partnerLocation.latitude != 0.0) {
                        val partnerMarker = Marker(mapView).apply {
                            position = GeoPoint(partnerLocation.latitude, partnerLocation.longitude)
                            title = partnerName
                            snippet = "Batería: ${partnerLocation.batteryLevel}%${if (partnerLocation.isCharging) " ⚡ (Cargando)" else ""} | ${partnerLocation.activity}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            val partnerChar = if (partnerName.contains("Ali", ignoreCase = true)) "👧" else "👦"
                            val ringColor = if (partnerLocation.sosActive) android.graphics.Color.RED else android.graphics.Color.parseColor("#E91E63")
                            icon = BitmapDrawable(
                                context.resources,
                                createAvatarMarkerBitmap(
                                    avatarBitmap = partnerAvatarBitmap,
                                    avatarEmoji = partnerChar,
                                    name = partnerName,
                                    colorArgb = ringColor
                                )
                            )
                        }
                        mapView.overlays.add(partnerMarker)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            )

            // Controles Flotantes del Mapa (En esquina superior derecha)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In / Out
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniControlButton(icon = "➕", theme = theme) {
                        mapViewInstance?.controller?.zoomIn()
                    }
                    MiniControlButton(icon = "➖", theme = theme) {
                        mapViewInstance?.controller?.zoomOut()
                    }
                }

                // Botón principal: Centrar Ambas Ubicaciones
                FloatingMapButton(icon = "👥", label = "Ambos", theme = theme) {
                    centerBothLocations(true)
                }

                // Centrar en Pareja
                FloatingMapButton(
                    icon = if (partnerName.contains("Ali", ignoreCase = true)) "👧" else "👦",
                    label = partnerName,
                    theme = theme,
                    imageUrl = partnerLocation.profileImageUrl
                ) {
                    if (partnerLocation.latitude != 0.0) {
                        mapViewInstance?.controller?.animateTo(GeoPoint(partnerLocation.latitude, partnerLocation.longitude))
                        mapViewInstance?.controller?.setZoom(16.5)
                    } else {
                        Toast.makeText(context, "Ubicación de $partnerName no disponible", Toast.LENGTH_SHORT).show()
                    }
                }

                // Centrar en Mí
                FloatingMapButton(
                    icon = if (userName.contains("Ali", ignoreCase = true)) "👧" else "👦",
                    label = "Yo",
                    theme = theme,
                    imageUrl = myLocation.profileImageUrl
                ) {
                    if (myLocation.latitude != 0.0) {
                        mapViewInstance?.controller?.animateTo(GeoPoint(myLocation.latitude, myLocation.longitude))
                        mapViewInstance?.controller?.setZoom(16.5)
                    }
                }
            }
        }

        // Barra inferior de Acciones Rápidas (Dentro del marco retro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF242424) else Color(0xFFF5E6BE))
                .border(1.dp, borderColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenAddZone,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(text = "➕ CREAR ZONA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
            }

            Button(
                onClick = onTriggerSos,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(text = "🚨 SOS ALERTA", fontFamily = Vt323, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun MiniControlButton(
    icon: String,
    theme: String = "",
    onClick: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val btnBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFBEA)
    val btnBorder = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val btnText = if (isDark) Color.White else Color(0xFF4A2511)

    Box(
        modifier = Modifier
            .size(30.dp)
            .border(2.dp, btnBorder)
            .background(btnBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 14.sp, color = btnText)
    }
}

@Composable
fun FloatingMapButton(
    icon: String,
    label: String,
    theme: String = "",
    imageUrl: String = "",
    onClick: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val btnBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFBEA)
    val btnBorder = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val btnText = if (isDark) Color.White else Color(0xFF4A2511)

    Box(
        modifier = Modifier
            .border(2.dp, btnBorder)
            .background(btnBg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.dp, btnBorder)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text(text = icon, fontSize = 16.sp)
            }
            Text(text = label, fontFamily = Vt323, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = btnText)
        }
    }
}

@Composable
fun RadarCompassView(
    bearingDegrees: Float,
    distanceMeters: Float,
    directionName: String,
    isTogether: Boolean,
    partnerName: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val animatedRotation by animateFloatAsState(
        targetValue = bearingDegrees,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "compassRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, borderColor)
            .background(cardBg)
            .padding(16.dp)
            .clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧭 BRÚJULA DE AMOR",
            fontFamily = Vt323,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = if (isTogether) "¡Están juntos! ❤️" else "Apuntando hacia $partnerName",
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dial de Brújula Pixel Art
        Box(
            modifier = Modifier
                .size(230.dp)
                .border(4.dp, borderColor)
                .background(if (isDark) Color(0xFF141414) else Color(0xFFFFF9E6)),
            contentAlignment = Alignment.Center
        ) {
            // Puntos Cardinales
            Text("N", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            Text("S", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            Text("E", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
            Text("O", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))

            // Círculo central con marcas
            Canvas(modifier = Modifier.size(190.dp)) {
                drawCircle(
                    color = borderColor.copy(alpha = 0.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Aguja Giratoria hacia la Pareja
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedRotation),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Flecha Norte / Pareja (Rosa Neón con Corazón)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(text = "❤️", fontSize = 22.sp)
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(46.dp)
                                .background(accentColor)
                        )
                    }

                    // Centro de la brújula
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(2.dp, borderColor)
                            .background(Color.White)
                    )

                    // Cola de la flecha
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(42.dp)
                                .background(textColor.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lecturas de Distancia y Ángulo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF2C1E26) else Color(0xFFFFEEF5))
                .padding(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RUMBO: ${bearingDegrees.roundToInt()}° $directionName",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                val distStr = if (distanceMeters >= 1000f) "${String.format(Locale.US, "%.2f", distanceMeters / 1000f)} Kilómetros" else "${distanceMeters.roundToInt()} Metros"
                Text(
                    text = "DISTANCIA: $distStr",
                    fontFamily = Vt323,
                    fontSize = 17.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RadarZonesView(
    zones: List<RadarPlaceZone>,
    coupleId: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onAddZoneClick: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, borderColor)
            .background(cardBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏠 ZONAS SEGURAS (${zones.size})",
                fontFamily = Vt323,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Button(
                onClick = onAddZoneClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("+ AGREGAR", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (zones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no hay zonas registradas.\nAgrega 'Casa Kevin', 'Casa Ali' o 'Universidad' para recibir alertas automáticas cuando tu pareja llegue o salga.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(zones) { zone ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor)
                            .background(if (theme == "Pixel Oscuro") Color(0xFF282828) else Color(0xFFFFF7DB))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = zone.icon, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zone.name,
                                    fontFamily = Vt323,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Radio de detección: ${zone.radiusMeters.roundToInt()}m",
                                    fontFamily = Vt323,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }

                            // Botón Borrar Zona
                            Text(
                                text = "🗑️",
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clickable {
                                        db.collection("locations").document(coupleId)
                                            .collection("zones").document(zone.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Zona '${zone.name}' eliminada", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarSettingsView(
    isSharing: Boolean,
    isBatterySaver: Boolean,
    myLocation: RadarLocationData,
    currentUserId: String,
    myDisplayName: String,
    partnerName: String,
    coupleId: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onToggleSharing: (Boolean) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, borderColor)
            .background(cardBg)
            .padding(14.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "⚙️ AJUSTES DE THOR RADAR",
            fontFamily = Vt323,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // Tarjeta de Identidad y Dispositivo Actual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (theme == "Pixel Oscuro") Color(0xFF2C2230) else Color(0xFFFCE4EC))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "👤 IDENTIDAD EN ESTE DISPOSITIVO",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estás identificado como: $myDisplayName ($currentUserId)",
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = "Pareja vinculada: $partnerName | Vínculo: $coupleId",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }

        // Toggle Compartir Ubicación
        SettingToggleCard(
            title = "COMPARTIR UBICACIÓN EN VIVO",
            description = "Permite que tu pareja vea tu ubicación en tiempo real y estado de batería.",
            checked = isSharing,
            onCheckedChange = onToggleSharing,
            textColor = textColor,
            borderColor = borderColor,
            theme = theme
        )

        // Toggle Ahorro de Batería
        SettingToggleCard(
            title = "MODO AHORRO DE BATERÍA",
            description = "Actualiza cada 60s en vez de 15s para reducir el consumo en viajes largos.",
            checked = isBatterySaver,
            onCheckedChange = onToggleBatterySaver,
            textColor = textColor,
            borderColor = borderColor,
            theme = theme
        )

        // Tarjeta de Permisos Segundo Plano (Todo el tiempo)
        val hasBgLoc = PermissionHelper.hasBackgroundLocationPermission(context)
        val activity = context as? android.app.Activity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, if (hasBgLoc) borderColor else Color(0xFFFF9800))
                .background(if (theme == "Pixel Oscuro") (if (hasBgLoc) Color(0xFF282828) else Color(0xFF332005)) else (if (hasBgLoc) Color(0xFFFFF7DB) else Color(0xFFFFF3E0)))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PERMISO 'TODO EL TIEMPO'",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBgLoc) textColor else (if (theme == "Pixel Oscuro") Color(0xFFFFB74D) else Color(0xFFE65100))
                    )
                    Text(
                        text = if (hasBgLoc) "✅ Activo: La app puede rastrear en segundo plano" else "⚠️ Inactivo: Se requiere 'Permitir todo el tiempo' para funcionar con la app cerrada.",
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (activity != null) {
                            PermissionHelper.requestBackgroundLocationPermission(activity)
                        } else {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasBgLoc) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(if (hasBgLoc) "VER" else "ACTIVAR", fontFamily = Vt323, fontSize = 14.sp, color = Color.White)
                }
            }
        }

        // Tarjeta de Diagnóstico GPS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (theme == "Pixel Oscuro") Color(0xFF222222) else Color(0xFFF3E5F5))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "📡 ESTADO DE TU GPS",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${String.format(Locale.US, "%.5f", myLocation.latitude)} | Lng: ${String.format(Locale.US, "%.5f", myLocation.longitude)}",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = "Precisión: ±${myLocation.accuracy.roundToInt()}m | Batería: ${myLocation.batteryLevel}%",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor
                )
            }
        }

        // Botón Forzar Actualización Manual
        Button(
            onClick = {
                ThorRadarManager.forceLocationUpdate(context) { success ->
                    if (success) {
                        Toast.makeText(context, "Ubicación actualizada con éxito 📍", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Verifica que el GPS y los permisos estén activados", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "🔄 ACTUALIZAR MI UBICACIÓN AHORA", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    borderColor: Color,
    theme: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(if (theme == "Pixel Oscuro") Color(0xFF282828) else Color(0xFFFFF7DB))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF80AB),
                    checkedTrackColor = Color(0xFF91465F)
                )
            )
        }
    }
}

@Composable
fun AddZoneDialog(
    coupleId: String,
    userId: String,
    currentLat: Double,
    currentLng: Double,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = theme == "Pixel Oscuro"
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🏠") }
    var radiusMeters by remember { mutableStateOf(150f) }

    val emojis = listOf("🏠", "🎓", "💼", "🏋️", "☕", "🌲", "❤️", "🍔")
    val previewCenter = remember(currentLat, currentLng) {
        if (currentLat != 0.0) GeoPoint(currentLat, currentLng)
        else GeoPoint(-33.4489, -70.6693)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "➕ NUEVA ZONA SEGURA",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                // Input de nombre con colores visibles tanto en modo claro como oscuro
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = "Nombre del lugar (ej. Casa Kevin)",
                            fontFamily = Vt323,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = textColor.copy(alpha = 0.7f),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        cursorColor = accentColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Icono
                Text(text = "Icono de la Zona:", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.forEach { emoji ->
                        val isSel = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(2.dp, if (isSel) accentColor else borderColor)
                                .background(if (isSel) accentColor.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                // Mini-Mapa interactivo con el círculo de radio en tiempo real
                Text(
                    text = "Vista Previa de la Zona (Radio: ${radiusMeters.roundToInt()}m):",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .border(2.dp, borderColor)
                        .clipToBounds()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(GOOGLE_MAPS_TILES)
                                setMultiTouchControls(true)
                                controller.setZoom(16.5)
                                controller.setCenter(previewCenter)

                                if (isDark) {
                                    val matrix = ColorMatrix(floatArrayOf(
                                        -0.85f, 0f, 0f, 0f, 240f,
                                        0f, -0.85f, 0f, 0f, 240f,
                                        0f, 0f, -0.75f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    ))
                                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
                                }
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.clear()

                            // Dibujar círculo del radio dinámico
                            val circlePoints = Polygon.pointsAsCircle(previewCenter, radiusMeters.toDouble())
                            val circlePolygon = Polygon(mapView).apply {
                                points = circlePoints
                                fillPaint.color = android.graphics.Color.argb(55, 233, 30, 99)
                                outlinePaint.color = android.graphics.Color.argb(220, 233, 30, 99)
                                outlinePaint.strokeWidth = 3.5f
                            }
                            mapView.overlays.add(circlePolygon)

                            // Marcador central con el icono seleccionado
                            val centerMarker = Marker(mapView).apply {
                                position = previewCenter
                                title = selectedEmoji
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = BitmapDrawable(context.resources, createTextMarkerBitmap(selectedEmoji, 28))
                            }
                            mapView.overlays.add(centerMarker)

                            mapView.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Slider para ajustar el radio de detección suavemente
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 30f..800f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = borderColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Botones rápidos de preset de radio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(50f to "50m", 100f to "100m", 200f to "200m", 350f to "350m", 500f to "500m").forEach { (r, label) ->
                        val isSel = (radiusMeters.roundToInt() == r.roundToInt())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, if (isSel) accentColor else borderColor)
                                .background(if (isSel) accentColor.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { radiusMeters = r }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = "📍 Ubicación fijada: (${String.format(Locale.US, "%.4f", currentLat)}, ${String.format(Locale.US, "%.4f", currentLng)})",
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Ingresa un nombre para el lugar", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val zoneId = UUID.randomUUID().toString()
                            val newZone = RadarPlaceZone(
                                id = zoneId,
                                name = name.trim(),
                                icon = selectedEmoji,
                                latitude = currentLat,
                                longitude = currentLng,
                                radiusMeters = radiusMeters,
                                addedBy = userId
                            )
                            FirebaseFirestore.getInstance()
                                .collection("locations").document(coupleId)
                                .collection("zones").document(zoneId)
                                .set(newZone.toMap())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Zona '${name.trim()}' guardada con éxito", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text("GUARDAR ZONA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SosCountdownDialog(
    mySosActive: Boolean,
    partnerName: String,
    onTriggerSos: () -> Unit,
    onCancel: () -> Unit,
    onDeactivateSos: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(3) }

    LaunchedEffect(mySosActive) {
        if (!mySosActive) {
            secondsLeft = 3
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            if (secondsLeft == 0) {
                onTriggerSos()
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(4.dp, Color.Red)
                .background(Color(0xFFFFEBEE))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🚨", fontSize = 48.sp)
                Text(
                    text = "ALERTA DE EMERGENCIA SOS",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                if (mySosActive) {
                    Text(
                        text = "¡Tu alerta SOS está ACTIVA!\n$partnerName ha recibido la alarma y tu ubicación en vivo.",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onDeactivateSos,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text("✅ DESACTIVAR SOS (ESTOY BIEN)", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                    }
                } else {
                    Text(
                        text = "Enviando alerta push de máxima prioridad a $partnerName en:",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "$secondsLeft",
                        fontFamily = Vt323,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("❌ CANCELAR", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                        }
                        Button(
                            onClick = onTriggerSos,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚨 ENVIAR YA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Helpers para crear marcadores Bitmaps para Osmdroid
private fun createAvatarMarkerBitmap(
    avatarBitmap: Bitmap? = null,
    avatarEmoji: String,
    name: String,
    colorArgb: Int
): Bitmap {
    val width = 120
    val height = 140
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    // Sombra en la base
    paint.color = android.graphics.Color.argb(80, 0, 0, 0)
    canvas.drawCircle(60f, 130f, 18f, paint)

    // Pin exterior con color distintivo (Azul / Rosa / Rojo)
    paint.color = colorArgb
    canvas.drawCircle(60f, 60f, 50f, paint)

    // Triángulo inferior del pin
    val path = android.graphics.Path().apply {
        moveTo(25f, 80f)
        lineTo(60f, 125f)
        lineTo(95f, 80f)
        close()
    }
    canvas.drawPath(path, paint)

    // Círculo interior blanco de fondo
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(60f, 60f, 40f, paint)

    if (avatarBitmap != null) {
        try {
            val targetSize = 76
            val scaled = Bitmap.createScaledBitmap(avatarBitmap, targetSize, targetSize, true)
            val shader = android.graphics.BitmapShader(
                scaled,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
            val matrix = android.graphics.Matrix().apply {
                setTranslate(60f - targetSize / 2f, 60f - targetSize / 2f)
            }
            shader.setLocalMatrix(matrix)

            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                isFilterBitmap = true
            }
            canvas.drawCircle(60f, 60f, 38f, imagePaint)
        } catch (e: Exception) {
            paint.textSize = 42f
            paint.textAlign = Paint.Align.CENTER
            val baseline = 60f - ((paint.descent() + paint.ascent()) / 2)
            canvas.drawText(avatarEmoji, 60f, baseline, paint)
        }
    } else {
        // Fallback a Emoji central si no hay foto
        paint.textSize = 42f
        paint.textAlign = Paint.Align.CENTER
        val baseline = 60f - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(avatarEmoji, 60f, baseline, paint)
    }

    return bitmap
}

private fun createTextMarkerBitmap(text: String, sizeDp: Int): Bitmap {
    val size = (sizeDp * 2).coerceAtLeast(40)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size * 0.7f
        textAlign = Paint.Align.CENTER
    }
    val baseline = (size / 2f) - ((paint.descent() + paint.ascent()) / 2)
    canvas.drawText(text, size / 2f, baseline, paint)

    return bitmap
}
