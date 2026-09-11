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

/**
 * Calcula un GeoPoint desplazado una distancia exacta en metros hacia un rumbo (bearing) en grados.
 */
fun calculateOffsetGeoPoint(lat: Double, lon: Double, distanceMeters: Double, bearingDegrees: Double = 90.0): GeoPoint {
    val rEarth = 6378137.0
    val latRad = Math.toRadians(lat)
    val lonRad = Math.toRadians(lon)
    val bearingRad = Math.toRadians(bearingDegrees)
    val distRatio = distanceMeters / rEarth

    val newLatRad = Math.asin(Math.sin(latRad) * Math.cos(distRatio) + Math.cos(latRad) * Math.sin(distRatio) * Math.cos(bearingRad))
    val newLonRad = lonRad + Math.atan2(
        Math.sin(bearingRad) * Math.sin(distRatio) * Math.cos(latRad),
        Math.cos(distRatio) - Math.sin(latRad) * Math.sin(newLatRad)
    )

    return GeoPoint(Math.toDegrees(newLatRad), Math.toDegrees(newLonRad))
}

/**
 * Crea un icono táctil retro para el manejador de cambio de tamaño en el borde del círculo.
 */
fun createResizeHandleBitmap(colorArgb: Int): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Sombra suave exterior
    paint.color = android.graphics.Color.argb(90, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f + 2f, size / 2f - 4f, paint)

    // Fondo blanco nítido
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)

    // Borde exterior con color temático
    paint.color = colorArgb
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4.5f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)

    // Flechas indicadoras de tamaño
    paint.style = Paint.Style.FILL
    paint.textSize = 28f
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    canvas.drawText("↔", size / 2f, size / 2f + 9f, paint)

    return bitmap
}

/**
 * Overlay de renderizado en tiempo real para dibujar el círculo de cobertura
 * alrededor del centro del mapa de forma suave y precisa sin interferir con los toques.
 */
class CenterZoneOverlay(
    private val getRadiusMeters: () -> Float
) : Overlay() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.argb(55, 233, 30, 99)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = android.graphics.Color.argb(230, 233, 30, 99)
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = android.graphics.Color.argb(120, 233, 30, 99)
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mapView.projection ?: return
        val center = (mapView.mapCenter as? GeoPoint) ?: return
        val centerScreenX = mapView.width / 2f
        val centerScreenY = mapView.height / 2f
        val eastGeo = calculateOffsetGeoPoint(center.latitude, center.longitude, getRadiusMeters().toDouble(), 90.0)
        val eastScreen = proj.toPixels(eastGeo, Point())
        val centerGeoScreen = proj.toPixels(center, Point())
        val radiusPx = Math.hypot((eastScreen.x - centerGeoScreen.x).toDouble(), (eastScreen.y - centerGeoScreen.y).toDouble()).toFloat()

        if (radiusPx > 2f) {
            canvas.drawCircle(centerScreenX, centerScreenY, radiusPx, fillPaint)
            canvas.drawCircle(centerScreenX, centerScreenY, radiusPx, strokePaint)
            canvas.drawCircle(centerScreenX, centerScreenY, radiusPx * 0.5f, dashPaint)
        }
    }
}

/**
 * Animación cinemática suave para volar hacia un objetivo en el mapa.
 */
fun smoothFlyTo(map: MapView, target: GeoPoint, targetZoom: Double = 16.5) {
    try {
        map.controller.animateTo(target, targetZoom, 1100L)
    } catch (e: Exception) {
        map.controller.animateTo(target)
        map.controller.setZoom(targetZoom)
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
    val activity = context as? android.app.Activity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasBgPermission by remember { mutableStateOf(PermissionHelper.hasBackgroundLocationPermission(context)) }
    var isIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isBannerDismissed by remember { mutableStateOf(prefs.getBoolean("radar_bg_banner_dismissed", false)) }
    var showSetupWizardDialog by remember {
        mutableStateOf(
            !prefs.getBoolean("radar_setup_wizard_dismissed", false) &&
            (!isSharingLocation || !isGpsEnabled || !hasBgPermission || !isIgnoringBattery)
        )
    }

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

    // Actualización de alta frecuencia en tiempo real mientras se visualiza la pantalla (4s)
    LaunchedEffect(isSharingLocation) {
        if (isSharingLocation && PermissionHelper.hasLocationPermission(context)) {
            ThorRadarManager.startLiveTracking(context, 4000L)
            while (isActive) {
                ThorRadarManager.forceLocationUpdate(context)
                delay(4000L)
            }
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
                val interval = if (isBatterySaver) 30_000L else 10_000L
                ThorRadarManager.startLiveTracking(context, interval)
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
                ThorRadarManager.sendLocationRequestPing(
                    context = context,
                    coupleId = coupleId,
                    senderId = currentUserId,
                    senderName = myDisplayName,
                    partnerName = partnerName
                )
                Toast.makeText(context, "🔔 Solicitud de ubicación enviada a $partnerName", Toast.LENGTH_SHORT).show()
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
            },
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            accentColor = accentColor
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
    onNavigate: () -> Unit,
    onPingPartner: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val hasValidData = partnerData.timestamp > 0L && partnerData.latitude != 0.0
    val isOnline = hasValidData && (System.currentTimeMillis() - partnerData.timestamp) < 600_000L // Activo en los últimos 10 min
    val timeDiffMs = if (partnerData.timestamp > 0L) System.currentTimeMillis() - partnerData.timestamp else Long.MAX_VALUE
    val isStale = !hasValidData || timeDiffMs >= 8 * 60 * 1000L // Más de 8 min

    val distanceText = when {
        !hasValidData -> "Esperando señal GPS de $partnerName..."
        isTogether -> "¡Juntos en el mismo lugar! ✨"
        distanceMeters >= 1000f -> "${String.format(Locale.US, "%.1f", distanceMeters / 1000f)} km ($directionName)"
        distanceMeters > 0f -> "${distanceMeters.roundToInt()} m ($directionName)"
        else -> "Calculando distancia..."
    }

    val activityText = when {
        !hasValidData -> "📡 Desconectado"
        partnerData.activity == "IN_VEHICLE" -> "🚗 En auto (${partnerData.speedKmh.roundToInt()} km/h)"
        partnerData.activity == "RUNNING" -> "🚴 En movimiento (${partnerData.speedKmh.roundToInt()} km/h)"
        partnerData.activity == "WALKING" -> "🚶 Caminando (${partnerData.speedKmh.roundToInt()} km/h)"
        else -> "🛋️ En reposo"
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

                // Batería & Actividad (Auto/Caminando/Reposo)
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
                        text = activityText,
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (partnerData.activity) {
                            "IN_VEHICLE" -> Color(0xFF2196F3)
                            "RUNNING" -> Color(0xFFFF9800)
                            "WALKING" -> Color(0xFF4CAF50)
                            else -> textColor.copy(alpha = 0.75f)
                        }
                    )
                }
            }

            // Diagnóstico y Aviso Inteligente si la Pareja no está enviando ubicación reciente
            if (isStale) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                        .background(if (isDark) Color(0xFF332005) else Color(0xFFFFF3E0))
                        .padding(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    !partnerData.isSharing -> "🛑 RADAR APAGADO EN SU CELULAR"
                                    partnerData.batteryLevel in 1..15 -> "🪫 BATERÍA MUY BAJA (${partnerData.batteryLevel}%)"
                                    timeDiffMs >= 30 * 60 * 1000L -> "⚠️ SEÑAL SUSPENDIDA / INACTIVA"
                                    else -> "📡 ESPERANDO SEÑAL GPS"
                                },
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = onPingPartner,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFE65100) else Color(0xFFFF9800)),
                                shape = RoundedCornerShape(0.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("🔔 PEDIR UBICACIÓN", fontFamily = Vt323, fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Text(
                            text = when {
                                !partnerData.isSharing -> "$partnerName tiene Thor Radar apagado en este momento."
                                partnerData.batteryLevel in 1..15 -> "Con poca batería su teléfono pudo haber activado el modo de ahorro de energía y suspendido el GPS en segundo plano."
                                timeDiffMs >= 30 * 60 * 1000L -> "Hace $timeAgo no se reciben datos. Es muy probable que Android haya congelado la app por 'Optimización de batería' o que falte el permiso 'Permitir todo el tiempo'."
                                else -> "El dispositivo de $partnerName no ha emitido señal reciente ($timeAgo). Pulsa 'Pedir Ubicación' para solicitar actualización."
                            },
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
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
    onEditZone: (RadarPlaceZone) -> Unit,
    onTriggerSos: () -> Unit
) {
    val context = LocalContext.current
    val isDark = theme == "Pixel Oscuro"
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var hasAutoCentered by remember { mutableStateOf(false) }
    var selectedMapZone by remember { mutableStateOf<RadarPlaceZone?>(null) }

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
            smoothFlyTo(map, GeoPoint(partnerLocation.latitude, partnerLocation.longitude), 16.5)
        } else if (hasMy) {
            smoothFlyTo(map, GeoPoint(myLocation.latitude, myLocation.longitude), 16.5)
        }
    }

    // Auto-centrar en ambas ubicaciones la primera vez que se cargan
    LaunchedEffect(myLocation.latitude, partnerLocation.latitude) {
        if (!hasAutoCentered && (myLocation.latitude != 0.0 || partnerLocation.latitude != 0.0)) {
            mapViewInstance?.let {
                centerBothLocations(animate = false)
                hasAutoCentered = true
            }
        }
    }

    val avatarChar = if (userName.contains("Ali", ignoreCase = true)) "👧" else "👦"
    val myBadge = when (myLocation.activity) {
        "IN_VEHICLE" -> "🚗"
        "RUNNING" -> "🚴"
        "WALKING" -> "🚶"
        else -> null
    }
    val myMarkerBitmap = remember(myAvatarBitmap, myBadge, avatarChar) {
        createAvatarMarkerBitmap(
            avatarBitmap = myAvatarBitmap,
            avatarEmoji = avatarChar,
            name = "Tú",
            colorArgb = android.graphics.Color.parseColor("#1976D2"),
            activityBadgeEmoji = myBadge
        )
    }

    val partnerChar = if (partnerName.contains("Ali", ignoreCase = true)) "👧" else "👦"
    val ringColor = if (partnerLocation.sosActive) android.graphics.Color.RED else android.graphics.Color.parseColor("#E91E63")
    val partnerBadge = when (partnerLocation.activity) {
        "IN_VEHICLE" -> "🚗"
        "RUNNING" -> "🚴"
        "WALKING" -> "🚶"
        else -> null
    }
    val partnerMarkerBitmap = remember(partnerAvatarBitmap, partnerBadge, ringColor, partnerChar) {
        createAvatarMarkerBitmap(
            avatarBitmap = partnerAvatarBitmap,
            avatarEmoji = partnerChar,
            name = partnerName,
            colorArgb = ringColor,
            activityBadgeEmoji = partnerBadge
        )
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

            val speedText = if (myLocation.speedKmh >= 2.0f) " • 🚀 ${myLocation.speedKmh.roundToInt()} km/h" else ""
            Text(
                text = (if (myLocation.accuracy > 0) "PRECISIÓN: ±${myLocation.accuracy.roundToInt()}m" else "GPS ACTIVO") + speedText,
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

                    // Marcadores de Zonas Seguras con radio visual
                    zones.forEach { zone ->
                        if (zone.latitude != 0.0 && zone.longitude != 0.0) {
                            val circle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(GeoPoint(zone.latitude, zone.longitude), zone.radiusMeters.toDouble())
                                val zoneColor = if (isDark) android.graphics.Color.parseColor("#4A148C") else android.graphics.Color.parseColor("#CE93D8")
                                fillPaint.color = android.graphics.Color.argb(45, android.graphics.Color.red(zoneColor), android.graphics.Color.green(zoneColor), android.graphics.Color.blue(zoneColor))
                                outlinePaint.color = android.graphics.Color.argb(180, android.graphics.Color.red(zoneColor), android.graphics.Color.green(zoneColor), android.graphics.Color.blue(zoneColor))
                                outlinePaint.strokeWidth = 2.5f
                            }
                            mapView.overlays.add(circle)

                            val zoneMarker = Marker(mapView).apply {
                                position = GeoPoint(zone.latitude, zone.longitude)
                                title = "${zone.icon} ${zone.name}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = BitmapDrawable(context.resources, createTextMarkerBitmap(zone.icon, 28))
                                infoWindow = null
                                setInfoWindow(null)
                                setOnMarkerClickListener { _, _ ->
                                    selectedMapZone = zone
                                    true
                                }
                            }
                            mapView.overlays.add(zoneMarker)
                        }
                    }

                    // Marcador de Mi Ubicación (Kevin o Ali) con foto de perfil
                    if (myLocation.latitude != 0.0) {
                        val myMarker = Marker(mapView).apply {
                            position = GeoPoint(myLocation.latitude, myLocation.longitude)
                            title = "Tú ($userName)"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = BitmapDrawable(context.resources, myMarkerBitmap)
                            infoWindow = null
                            setInfoWindow(null)
                            setOnMarkerClickListener { _, _ -> true }
                        }
                        mapView.overlays.add(myMarker)
                    }

                    // Marcador de la Pareja con foto de perfil
                    if (partnerLocation.latitude != 0.0) {
                        val partnerMarker = Marker(mapView).apply {
                            position = GeoPoint(partnerLocation.latitude, partnerLocation.longitude)
                            title = partnerName
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = BitmapDrawable(context.resources, partnerMarkerBitmap)
                            infoWindow = null
                            setInfoWindow(null)
                            setOnMarkerClickListener { _, _ -> true }
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
                        mapViewInstance?.let { map ->
                            smoothFlyTo(map, GeoPoint(partnerLocation.latitude, partnerLocation.longitude), 16.5)
                        }
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
                        mapViewInstance?.let { map ->
                            smoothFlyTo(map, GeoPoint(myLocation.latitude, myLocation.longitude), 16.5)
                        }
                    } else {
                        Toast.makeText(context, "Tu ubicación GPS no está disponible", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Tarjeta Flotante cuando se toca una Zona Segura en el mapa
            if (selectedMapZone != null) {
                val zone = selectedMapZone!!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(2.dp, accentColor),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = zone.icon, fontSize = 28.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zone.name,
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Radio de detección: ${zone.radiusMeters.roundToInt()}m",
                                    fontFamily = Vt323,
                                    fontSize = 13.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = {
                                    val z = zone
                                    selectedMapZone = null
                                    onEditZone(z)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(0.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✏️ EDITAR", fontFamily = Vt323, fontSize = 14.sp, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.dp, borderColor)
                                    .clickable { selectedMapZone = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✖", fontSize = 11.sp, color = textColor)
                            }
                        }
                    }
                }
            }
        }

        // Barra Inferior de Acciones Rápidas
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
    onAddZoneClick: () -> Unit,
    onEditZoneClick: (RadarPlaceZone) -> Unit
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
                items(
                    items = zones,
                    key = { it.id }
                ) { zone ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor)
                            .background(if (theme == "Pixel Oscuro") Color(0xFF282828) else Color(0xFFFFF7DB))
                            .clickable { onEditZoneClick(zone) }
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Botón Editar Zona
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, borderColor)
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .clickable { onEditZoneClick(zone) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✏️ EDITAR", fontFamily = Vt323, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                }

                                // Botón Borrar Zona
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, borderColor)
                                        .background(Color.Red.copy(alpha = 0.1f))
                                        .clickable {
                                            db.collection("locations").document(coupleId)
                                                .collection("zones").document(zone.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    ThorRadarManager.removeZoneFromCache(context, zone.id)
                                                    Toast.makeText(context, "Zona '${zone.name}' eliminada", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🗑️", fontSize = 13.sp)
                                }
                            }
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
    onToggleBatterySaver: (Boolean) -> Unit,
    isGpsEnabled: Boolean,
    hasBgLoc: Boolean,
    isIgnoringBattery: Boolean,
    onOpenSetupWizard: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = theme == "Pixel Oscuro"
    val activity = context as? android.app.Activity
    var localIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(10.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Tarjeta Maestra de Control de Encendido / Apagado
        RadarMasterPowerCard(
            isSharing = isSharing,
            onToggleSharing = onToggleSharing,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor
        )

        // 2. Toggle Modo Ahorro de Batería
        SettingToggleCard(
            title = "MODO AHORRO DE BATERÍA",
            description = "Actualiza cada 60s en vez de 15s para reducir el consumo en viajes largos.",
            checked = isBatterySaver,
            onCheckedChange = onToggleBatterySaver,
            textColor = textColor,
            borderColor = borderColor,
            theme = theme
        )

        // 3. Panel Consolidado de Requisitos y Permisos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF252028) else Color(0xFFF3E5F5))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🩺", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REQUISITOS EN TU CELULAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onOpenSetupWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("ASISTENTE 🛠️", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                    }
                }

                // GPS
                RadarRequirementRow(
                    icon = if (isGpsEnabled) "✅" else "⚠️",
                    title = "Sensor GPS",
                    subtitle = if (isGpsEnabled) "Ubicación del sistema encendida" else "Ubicación desactivada en Android",
                    isOk = isGpsEnabled,
                    buttonText = if (isGpsEnabled) null else "ACTIVAR",
                    onButtonClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    textColor = textColor,
                    isDark = isDark
                )

                // Segundo Plano
                RadarRequirementRow(
                    icon = if (hasBgLoc) "✅" else "⚠️",
                    title = "Segundo plano (Todo el tiempo)",
                    subtitle = if (hasBgLoc) "Permiso concedido para app cerrada" else "Se requiere 'Permitir todo el tiempo'",
                    isOk = hasBgLoc,
                    buttonText = if (hasBgLoc) null else "ACTIVAR",
                    onButtonClick = {
                        if (activity != null) {
                            PermissionHelper.requestBackgroundLocationPermission(activity)
                        } else {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    textColor = textColor,
                    isDark = isDark
                )

                // Batería
                RadarRequirementRow(
                    icon = if (localIgnoringBattery) "✅" else "⚠️",
                    title = "Batería sin restricciones",
                    subtitle = if (localIgnoringBattery) "Optimización desactivada" else "Android podría pausar el radar",
                    isOk = localIgnoringBattery,
                    buttonText = if (localIgnoringBattery) null else "QUITAR LÍMITE",
                    onButtonClick = {
                        PermissionHelper.requestIgnoreBatteryOptimizations(context)
                        localIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                    },
                    textColor = textColor,
                    isDark = isDark
                )
            }
        }

        // 4. Tarjeta de Diagnóstico / Telemetría GPS e Identidad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF222222) else Color(0xFFFFF7DB))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📡 DISPOSITIVO Y TELEMETRÍA GPS",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Usuario: $myDisplayName ($currentUserId) | Pareja: $partnerName",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = "Lat: ${String.format(Locale.US, "%.5f", myLocation.latitude)} | Lng: ${String.format(Locale.US, "%.5f", myLocation.longitude)} | ±${myLocation.accuracy.roundToInt()}m | 🔋 ${myLocation.batteryLevel}%",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }

        // 5. Botón Forzar Actualización Manual
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
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text(text = "🔄 ACTUALIZAR MI UBICACIÓN AHORA", fontFamily = Vt323, fontSize = 17.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun RadarRequirementRow(
    icon: String,
    title: String,
    subtitle: String,
    isOk: Boolean,
    buttonText: String?,
    onButtonClick: () -> Unit,
    textColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isOk) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFFF9800))
            .background(if (isDark) (if (isOk) Color(0xFF1E281E) else Color(0xFF332005)) else (if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Vt323,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
        if (buttonText != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(buttonText, fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
            }
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
fun RadarMasterPowerCard(
    isSharing: Boolean,
    onToggleSharing: (Boolean) -> Unit,
    theme: String,
    textColor: Color,
    borderColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val cardBorder = if (isSharing) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val cardBg = if (isSharing) {
        if (isDark) Color(0xFF1B3B1D) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF3B1B1B) else Color(0xFFFFEBEE)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, cardBorder)
            .background(cardBg)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSharing) "🟢" else "🛑",
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSharing) "RADAR ENCENDIDO (TRANSMITIENDO)" else "RADAR APAGADO (AHORRO 100%)",
                        fontFamily = Vt323,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSharing) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFE57373) else Color(0xFFD32F2F))
                    )
                    Text(
                        text = if (isSharing) {
                            "Rastreo en segundo plano activo. Tu pareja puede ver tu ubicación."
                        } else {
                            "Servicio en segundo plano y GPS 100% detenidos. Cero consumo de batería."
                        },
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            }

            // Botón Maestro de Encendido / Apagado
            Button(
                onClick = { onToggleSharing(!isSharing) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSharing) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (isSharing) "🛑 APAGAR THOR RADAR" else "⚡ ENCENDER THOR RADAR",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = if (isSharing) {
                    "💡 Apagar el radar detiene por completo el servicio en segundo plano (Foreground Service) y el GPS para ahorrar batería."
                } else {
                    "💡 Enciende el radar cuando salgas o quieras que tu pareja vea tu ubicación y zonas seguras en tiempo real."
                },
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun RadarSetupWizardDialog(
    isSharing: Boolean,
    isGpsEnabled: Boolean,
    hasBgPermission: Boolean,
    isIgnoringBattery: Boolean,
    onToggleSharing: (Boolean) -> Unit,
    onEnableGps: () -> Unit,
    onRequestBgPermission: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onDismiss: () -> Unit,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val allReady = isSharing && isGpsEnabled && hasBgPermission && isIgnoringBattery

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🛠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIGURACIÓN DEL RADAR",
                        fontFamily = Vt323,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Text(
                    text = if (allReady) {
                        "✨ ¡Excelente! Todos los requisitos están activos para que tu pareja vea tu ubicación en tiempo real sin interrupciones."
                    } else {
                        "Para que el radar funcione en segundo plano y Android no congele la ubicación con la app cerrada, activa estos 4 elementos:"
                    },
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.85f)
                )

                Divider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

                // Item 1: Thor Radar Encendido
                SetupCheckItem(
                    title = "1. THOR RADAR ENCENDIDO",
                    subtitle = if (isSharing) "Radar activo en la app" else "El radar está apagado",
                    isReady = isSharing,
                    buttonText = "ENCENDER",
                    onAction = { onToggleSharing(true) },
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 2: Sensor GPS del Teléfono
                SetupCheckItem(
                    title = "2. SENSOR GPS DEL TELÉFONO",
                    subtitle = if (isGpsEnabled) "Ubicación del sistema encendida" else "GPS apagado en Android",
                    isReady = isGpsEnabled,
                    buttonText = "ACTIVAR GPS",
                    onAction = onEnableGps,
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 3: Permiso Todo el Tiempo
                SetupCheckItem(
                    title = "3. PERMISO 'TODO EL TIEMPO'",
                    subtitle = if (hasBgPermission) "Rastreo con pantalla bloqueada activo" else "Solo 'Mientras la app está en uso'",
                    isReady = hasBgPermission,
                    buttonText = "ACTIVAR",
                    onAction = onRequestBgPermission,
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 4: Batería Sin Restricciones
                SetupCheckItem(
                    title = "4. BATERÍA: SIN RESTRICCIONES",
                    subtitle = if (isIgnoringBattery) "Android no congelará el proceso" else "Optimización activa (puede suspender el GPS)",
                    isReady = isIgnoringBattery,
                    buttonText = "QUITAR LÍMITE",
                    onAction = onRequestIgnoreBattery,
                    textColor = textColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (allReady) Color(0xFF2E7D32) else accentColor),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = if (allReady) "✅ TODO LISTO • CONTINUAR AL MAPA" else "ENTENDIDO • IR AL MAPA",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SetupCheckItem(
    title: String,
    subtitle: String,
    isReady: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    textColor: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isReady) Color(0xFF4CAF50) else Color(0xFFFF9800))
            .background(
                if (isReady) {
                    if (isDark) Color(0xFF1B3B1D) else Color(0xFFE8F5E9)
                } else {
                    if (isDark) Color(0xFF332005) else Color(0xFFFFF3E0)
                }
            )
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isReady) "✅" else "⚠️",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReady) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                )
                Text(
                    text = subtitle,
                    fontFamily = Vt323,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            if (!isReady) {
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(buttonText, fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AddEditZoneDialog(
    coupleId: String,
    userId: String,
    currentLat: Double,
    currentLng: Double,
    existingZone: RadarPlaceZone? = null,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onZoneSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = theme == "Pixel Oscuro"
    val isEditing = existingZone != null

    var name by remember { mutableStateOf(existingZone?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existingZone?.icon ?: "🏠") }
    var radiusMeters by remember { mutableStateOf(existingZone?.radiusMeters ?: 150f) }

    var selectedLat by remember {
        mutableStateOf(
            if (existingZone != null && existingZone.latitude != 0.0) existingZone.latitude
            else if (currentLat != 0.0) currentLat
            else -33.4489
        )
    }
    var selectedLng by remember {
        mutableStateOf(
            if (existingZone != null && existingZone.longitude != 0.0) existingZone.longitude
            else if (currentLng != 0.0) currentLng
            else -70.6693
        )
    }
    var addressText by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<RadarSearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val emojis = listOf("🏠", "🎓", "💼", "🏋️", "☕", "🍔", "🛒", "❤️", "🌲", "🏥", "🎮", "🚗", "✈️", "🏖️", "🐾")

    // Geocodificación inversa automática al cambiar coordenadas
    LaunchedEffect(selectedLat, selectedLng) {
        val addr = ThorRadarManager.getReverseAddress(context, selectedLat, selectedLng)
        if (addr.isNotEmpty()) {
            addressText = addr
        }
    }

    // Búsqueda en vivo tipo Google Maps con debounce
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(350)
            isSearching = true
            val results = ThorRadarManager.searchPlaces(context, q)
            searchResults = results
            showSearchResults = results.isNotEmpty()
            isSearching = false
        } else {
            searchResults = emptyList()
            showSearchResults = false
            isSearching = false
        }
    }

    fun performSearch() {
        val q = searchQuery.trim()
        if (q.length < 2) {
            Toast.makeText(context, "Escribe al menos 2 caracteres para buscar", Toast.LENGTH_SHORT).show()
            return
        }
        isSearching = true
        showSearchResults = true
        coroutineScope.launch {
            val results = ThorRadarManager.searchPlaces(context, q)
            searchResults = results
            isSearching = false
            if (results.isEmpty()) {
                Toast.makeText(context, "No se encontraron resultados para '$q'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabecera fija
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "✏️ EDITAR ZONA SEGURA" else "➕ NUEVA ZONA SEGURA",
                        fontFamily = Vt323,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .border(1.5.dp, borderColor)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("❌", fontSize = 11.sp)
                    }
                }

                // Contenido Scrolleable en el centro
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Buscador compacto con búsqueda en vivo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                            },
                            placeholder = {
                                Text("Buscar calle, comuna o lugar...", fontFamily = Vt323, fontSize = 14.sp, color = textColor.copy(alpha = 0.5f))
                            },
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                            textStyle = TextStyle(fontFamily = Vt323, fontSize = 15.sp, color = textColor),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "✖",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { searchQuery = ""; showSearchResults = false }.padding(4.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = borderColor,
                                cursorColor = accentColor
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )

                        Button(
                            onClick = { performSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(0.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(48.dp),
                            enabled = !isSearching
                        ) {
                            Text(if (isSearching) "..." else "BUSCAR", fontFamily = Vt323, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    // Resultados búsqueda si hay
                    if (showSearchResults && searchResults.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(2.dp, accentColor),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sugerencias (${searchResults.size}):",
                                        fontFamily = Vt323,
                                        fontSize = 13.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "CERRAR ✖",
                                        fontFamily = Vt323,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { showSearchResults = false }
                                    )
                                }
                                searchResults.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLat = item.latitude
                                                selectedLng = item.longitude
                                                addressText = if (item.subtitle.isNotBlank()) "${item.title}, ${item.subtitle}" else item.title
                                                if (name.isBlank()) {
                                                    name = item.title
                                                }
                                                showSearchResults = false
                                                searchQuery = ""
                                                val target = GeoPoint(item.latitude, item.longitude)
                                                mapViewRef?.controller?.animateTo(target)
                                                mapViewRef?.controller?.setZoom(17.0)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📍", fontSize = 16.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontFamily = Vt323,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.subtitle.isNotBlank()) {
                                                Text(
                                                    text = item.subtitle,
                                                    fontFamily = Vt323,
                                                    fontSize = 12.sp,
                                                    color = textColor.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text("ELEGIR ➔", fontFamily = Vt323, fontSize = 12.sp, color = accentColor)
                                    }
                                    HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 0.8.dp)
                                }
                            }
                        }
                    }

                    // 2. Mapa ampliado y cómodo (270.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                            .border(2.dp, borderColor)
                            .clipToBounds()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(GOOGLE_MAPS_TILES)
                                    setMultiTouchControls(true)
                                    controller.setZoom(16.5)
                                    controller.setCenter(GeoPoint(selectedLat, selectedLng))

                                    if (isDark) {
                                        val matrix = ColorMatrix(floatArrayOf(
                                            -0.85f, 0f, 0f, 0f, 240f,
                                            0f, -0.85f, 0f, 0f, 240f,
                                            0f, 0f, -0.75f, 0f, 255f,
                                            0f, 0f, 0f, 1f, 0f
                                        ))
                                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
                                    }

                                    overlays.add(CenterZoneOverlay { radiusMeters })

                                    addMapListener(object : org.osmdroid.events.MapListener {
                                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                            val c = mapCenter as? GeoPoint
                                            if (c != null) {
                                                selectedLat = c.latitude
                                                selectedLng = c.longitude
                                            }
                                            return false
                                        }

                                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                                            val c = mapCenter as? GeoPoint
                                            if (c != null) {
                                                selectedLat = c.latitude
                                                selectedLng = c.longitude
                                            }
                                            return false
                                        }
                                    })

                                    setOnTouchListener { v, event ->
                                        when (event.actionMasked) {
                                            MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                                val c = (v as? MapView)?.mapCenter as? GeoPoint
                                                if (c != null) {
                                                    selectedLat = c.latitude
                                                    selectedLng = c.longitude
                                                }
                                            }
                                        }
                                        false
                                    }

                                    mapViewRef = this
                                }
                            },
                            update = { mapView ->
                                mapViewRef = mapView
                                @Suppress("UNUSED_VARIABLE")
                                val currentRadius = radiusMeters
                                mapView.postInvalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Retículo Central Fijo
                        Box(
                            modifier = Modifier.align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(56.dp)) {
                                val midX = size.width / 2f
                                val midY = size.height / 2f
                                val strokeW = 2.dp.toPx()
                                val c = Color(0xFFE91E63)
                                drawLine(c.copy(alpha = 0.7f), Offset(0f, midY), Offset(midX - 16.dp.toPx(), midY), strokeWidth = strokeW)
                                drawLine(c.copy(alpha = 0.7f), Offset(midX + 16.dp.toPx(), midY), Offset(size.width, midY), strokeWidth = strokeW)
                                drawLine(c.copy(alpha = 0.7f), Offset(midX, 0f), Offset(midX, midY - 16.dp.toPx()), strokeWidth = strokeW)
                                drawLine(c.copy(alpha = 0.7f), Offset(midX, midY + 16.dp.toPx()), Offset(midX, size.height), strokeWidth = strokeW)
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(cardBg, CircleShape)
                                    .border(2.dp, Color(0xFFE91E63), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = selectedEmoji, fontSize = 18.sp)
                            }
                        }

                        // Banner informativo superior
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                                .background(Color(0xDD000000), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📍 Mueve el mapa para ubicar el centro",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        // Controles Zoom
                        Column(
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(cardBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                    .border(1.2.dp, borderColor, RoundedCornerShape(4.dp))
                                    .clickable { mapViewRef?.controller?.zoomIn() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("➕", fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(cardBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                    .border(1.2.dp, borderColor, RoundedCornerShape(4.dp))
                                    .clickable { mapViewRef?.controller?.zoomOut() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("➖", fontSize = 12.sp)
                            }
                        }

                        // Botón GPS
                        if (currentLat != 0.0 && currentLng != 0.0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .background(cardBg.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                                    .border(1.2.dp, accentColor, RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedLat = currentLat
                                        selectedLng = currentLng
                                        mapViewRef?.controller?.animateTo(GeoPoint(currentLat, currentLng))
                                        mapViewRef?.controller?.setZoom(16.5)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📍 Mi GPS", fontFamily = Vt323, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Dirección compacta
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(textColor.copy(alpha = 0.05f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        if (addressText.isNotEmpty()) {
                            Text(
                                text = "📍 $addressText",
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Coords: (${String.format(Locale.US, "%.5f", selectedLat)}, ${String.format(Locale.US, "%.5f", selectedLng)})",
                            fontFamily = Vt323,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }

                    // 3. Input de Nombre de la Zona
                    Text(text = "Nombre de la Zona:", fontFamily = Vt323, fontSize = 14.sp, color = textColor)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text(
                                text = "Ej. Casa, Universidad, Trabajo...",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = TextStyle(
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderColor,
                            cursorColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    // 4. Selector de Icono / Emoji
                    Text(text = "Icono de la Zona:", fontFamily = Vt323, fontSize = 14.sp, color = textColor)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = emojis,
                            key = { it }
                        ) { emoji ->
                            val isSel = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.dp, if (isSel) accentColor else borderColor)
                                    .background(if (isSel) accentColor.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    // 5. Selector de Radio de Cobertura
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Radio de Cobertura:",
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "${radiusMeters.roundToInt()} metros",
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Slider(
                        value = radiusMeters,
                        onValueChange = {
                            radiusMeters = it
                            mapViewRef?.postInvalidate()
                        },
                        valueRange = 30f..800f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = borderColor.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(50f to "50m", 100f to "100m", 200f to "200m", 350f to "350m", 500f to "500m").forEach { (r, label) ->
                            val isSel = (radiusMeters.roundToInt() == r.roundToInt())
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.2.dp, if (isSel) accentColor else borderColor)
                                    .background(if (isSel) accentColor.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable {
                                        radiusMeters = r
                                        mapViewRef?.postInvalidate()
                                    }
                                    .padding(vertical = 3.dp),
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
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = borderColor.copy(alpha = 0.35f), thickness = 1.dp)

                // 3. Botones de acción fijos en la parte inferior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", fontFamily = Vt323, fontSize = 15.sp, color = textColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Ingresa un nombre para el lugar", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val zoneId = existingZone?.id?.ifEmpty { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()
                            val placeZone = RadarPlaceZone(
                                id = zoneId,
                                name = name.trim(),
                                icon = selectedEmoji,
                                latitude = selectedLat,
                                longitude = selectedLng,
                                radiusMeters = radiusMeters,
                                addedBy = existingZone?.addedBy?.ifEmpty { userId } ?: userId
                            )
                            FirebaseFirestore.getInstance()
                                .collection("locations").document(coupleId)
                                .collection("zones").document(zoneId)
                                .set(placeZone.toMap())
                                .addOnSuccessListener {
                                    ThorRadarManager.updateZoneInCache(context, placeZone)
                                    val msg = if (isEditing) "Zona '${name.trim()}' actualizada con éxito" else "Zona '${name.trim()}' guardada con éxito"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onZoneSaved?.invoke()
                                    onDismiss()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error guardando zona: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val btnText = if (isEditing) "💾 GUARDAR CAMBIOS" else "💾 GUARDAR ZONA"
                        Text(btnText, fontFamily = Vt323, fontSize = 15.sp, color = Color.White)
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
    colorArgb: Int,
    activityBadgeEmoji: String? = null
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

    // Badge flotante de actividad (🚗 En auto / 🚴 En movimiento / 🚶 Caminando)
    if (!activityBadgeEmoji.isNullOrBlank()) {
        val badgeX = 96f
        val badgeY = 24f
        val badgeRadius = 18f

        // Borde exterior del badge
        paint.shader = null
        paint.color = colorArgb
        canvas.drawCircle(badgeX, badgeY, badgeRadius + 2f, paint)

        // Fondo del badge
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(badgeX, badgeY, badgeRadius, paint)

        // Emoji dentro del badge
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        val badgeBaseline = badgeY - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(activityBadgeEmoji, badgeX, badgeBaseline, paint)
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
