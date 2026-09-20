package calendario.kevshupp.diariokevinali.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import calendario.kevshupp.diariokevinali.RadarLocationData
import calendario.kevshupp.diariokevinali.RadarPlaceZone
import kotlinx.coroutines.delay
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import kotlin.math.roundToInt

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
    val timeDiffMs = if (partnerData.timestamp > 0L) System.currentTimeMillis() - partnerData.timestamp else Long.MAX_VALUE
    val isSharing = partnerData.isSharing
    val isOnline = hasValidData && isSharing && timeDiffMs < 2 * 60 * 1000L // En línea si transmitió hace menos de 2 min y radar encendido
    val isStale = !isSharing || !hasValidData || timeDiffMs >= 15 * 60 * 1000L

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

    val statusText = when {
        !isSharing -> "Apagado"
        !hasValidData -> "Sin señal"
        isOnline -> "En línea"
        timeDiffMs < 15 * 60 * 1000L -> timeAgo
        else -> "Inactivo ($timeAgo)"
    }
    val statusColor = when {
        !isSharing -> if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
        !hasValidData -> Color.Gray
        isOnline -> Color(0xFF4CAF50)
        timeDiffMs < 15 * 60 * 1000L -> if (isDark) Color(0xFFFFB74D) else Color(0xFFF57C00)
        else -> if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
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
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontFamily = Vt323,
                            fontSize = 13.sp,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                                .background(if (isDark) Color(0xFF332005) else Color(0xFFFFF3E0))
                                .clickable { onPingPartner() }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("🔄 ACTUALIZAR", fontFamily = Vt323, fontSize = 11.sp, color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
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
                                Text("🔄 ACTUALIZAR AHORA", fontFamily = Vt323, fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Text(
                            text = when {
                                !partnerData.isSharing -> "$partnerName tiene Thor Radar apagado en este momento."
                                partnerData.batteryLevel in 1..15 -> "Con poca batería su teléfono pudo haber activado el modo de ahorro de energía y suspendido el GPS en segundo plano."
                                timeDiffMs >= 30 * 60 * 1000L -> "Hace $timeAgo no se reciben datos. Es muy probable que Android haya congelado la app por 'Optimización de batería' o que falte el permiso 'Permitir todo el tiempo'."
                                else -> "El dispositivo de $partnerName no ha emitido señal reciente ($timeAgo). Pulsa 'Actualizar Ahora' para forzar una sincronización remota."
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

                    // Marcadores de Zonas Seguras con radio visual nítido y de alto contraste
                    zones.forEach { zone ->
                        if (zone.latitude != 0.0 && zone.longitude != 0.0) {
                            val circle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(GeoPoint(zone.latitude, zone.longitude), zone.radiusMeters.toDouble())
                                val zoneColor = if (isDark) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#AB47BC")
                                fillPaint.color = android.graphics.Color.argb(
                                    if (isDark) 55 else 45,
                                    android.graphics.Color.red(zoneColor),
                                    android.graphics.Color.green(zoneColor),
                                    android.graphics.Color.blue(zoneColor)
                                )
                                outlinePaint.color = android.graphics.Color.argb(
                                    if (isDark) 245 else 200,
                                    android.graphics.Color.red(zoneColor),
                                    android.graphics.Color.green(zoneColor),
                                    android.graphics.Color.blue(zoneColor)
                                )
                                outlinePaint.strokeWidth = if (isDark) 4f else 3f
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
