package calendario.kevshupp.diariokevinali.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import android.widget.Toast
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.RadarPlaceZone
import calendario.kevshupp.diariokevinali.RadarSearchResult
import calendario.kevshupp.diariokevinali.ThorRadarManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.util.*
import kotlin.math.roundToInt

// Google Maps Estándar (El mapa clásico y limpio de Google)
val GOOGLE_MAPS_TILES = object : OnlineTileSourceBase(
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
    private val isDark: Boolean = false,
    private val getRadiusMeters: () -> Float
) : Overlay() {
    private val themeColor = if (isDark) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#E91E63")
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.argb(
            if (isDark) 55 else 55,
            android.graphics.Color.red(themeColor),
            android.graphics.Color.green(themeColor),
            android.graphics.Color.blue(themeColor)
        )
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (isDark) 4.5f else 4f
        color = android.graphics.Color.argb(
            if (isDark) 245 else 230,
            android.graphics.Color.red(themeColor),
            android.graphics.Color.green(themeColor),
            android.graphics.Color.blue(themeColor)
        )
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = android.graphics.Color.argb(
            if (isDark) 160 else 120,
            android.graphics.Color.red(themeColor),
            android.graphics.Color.green(themeColor),
            android.graphics.Color.blue(themeColor)
        )
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

    DisposableEffect(Unit) {
        onDispose {
            try {
                mapViewRef?.onPause()
                mapViewRef?.onDetach()
            } catch (_: Exception) {}
        }
    }

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

                                    overlays.add(CenterZoneOverlay(isDark = isDark) { radiusMeters })

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

                                    try {
                                        onResume()
                                    } catch (_: Exception) {}
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
