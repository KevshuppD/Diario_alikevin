package calendario.kevshupp.diariokevinali

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

data class RadarLocationData(
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speedKmh: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val activity: String = "STILL", // STILL, WALKING, IN_VEHICLE, UNKNOWN
    val currentZone: String = "",
    val address: String = "",
    val timestamp: Long = 0L,
    val isSharing: Boolean = true,
    val sosActive: Boolean = false,
    val sosTimestamp: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "profileImageUrl" to profileImageUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracy" to accuracy,
            "speedKmh" to speedKmh,
            "batteryLevel" to batteryLevel,
            "isCharging" to isCharging,
            "activity" to activity,
            "currentZone" to currentZone,
            "address" to address,
            "timestamp" to timestamp,
            "isSharing" to isSharing,
            "sosActive" to sosActive,
            "sosTimestamp" to sosTimestamp
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot?): RadarLocationData {
            if (doc == null || !doc.exists()) return RadarLocationData()
            val data = doc.data ?: return RadarLocationData()
            return RadarLocationData(
                userId = data["userId"] as? String ?: "",
                userName = data["userName"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                accuracy = (data["accuracy"] as? Number)?.toFloat() ?: 0f,
                speedKmh = (data["speedKmh"] as? Number)?.toFloat() ?: 0f,
                batteryLevel = (data["batteryLevel"] as? Number)?.toInt() ?: 100,
                isCharging = data["isCharging"] as? Boolean ?: false,
                activity = data["activity"] as? String ?: "STILL",
                currentZone = data["currentZone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                isSharing = data["isSharing"] as? Boolean ?: true,
                sosActive = data["sosActive"] as? Boolean ?: false,
                sosTimestamp = (data["sosTimestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class RadarPlaceZone(
    val id: String = "",
    val name: String = "",
    val icon: String = "🏠",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 150f,
    val addedBy: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "icon" to icon,
            "latitude" to latitude,
            "longitude" to longitude,
            "radiusMeters" to radiusMeters,
            "addedBy" to addedBy
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): RadarPlaceZone {
            if (map == null) return RadarPlaceZone()
            return RadarPlaceZone(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                icon = map["icon"] as? String ?: "🏠",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                radiusMeters = (map["radiusMeters"] as? Number)?.toFloat() ?: 150f,
                addedBy = map["addedBy"] as? String ?: ""
            )
        }
    }
}

data class RadarHistoryPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val placeName: String = "",
    val timestamp: Long = 0L,
    val speedKmh: Float = 0f
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "placeName" to placeName,
            "timestamp" to timestamp,
            "speedKmh" to speedKmh
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): RadarHistoryPoint {
            if (map == null) return RadarHistoryPoint()
            return RadarHistoryPoint(
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                placeName = map["placeName"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
                speedKmh = (map["speedKmh"] as? Number)?.toFloat() ?: 0f
            )
        }
    }
}

object ThorRadarManager {
    private const val TAG = "ThorRadarManager"
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val db = FirebaseFirestore.getInstance()

    private var lastUploadedLat: Double = 0.0
    private var lastUploadedLng: Double = 0.0
    private var lastUploadedTime: Long = 0L
    private var cachedZones: List<RadarPlaceZone> = emptyList()

    fun isAli(userId: String?, userName: String?): Boolean {
        val uid = (userId ?: "").lowercase()
        val uname = (userName ?: "").lowercase()
        return uid.contains("ali") || uname.contains("ali")
    }

    fun normalizeCoupleId(coupleId: String?): String {
        val clean = (coupleId ?: "").trim()
        if (clean.isEmpty() || clean == "vinculo_unico_123" || clean == "vínculo_único_123") {
            return "vínculo_único_123"
        }
        return clean
    }

    fun getMyDocName(userId: String?, userName: String?): String {
        return if (isAli(userId, userName)) "ali" else "kevin"
    }

    fun getPartnerDocName(userId: String?, userName: String?): String {
        return if (isAli(userId, userName)) "kevin" else "ali"
    }

    fun getMyDisplayName(userId: String?, userName: String?): String {
        return if (isAli(userId, userName)) "Ali" else "Kevin"
    }

    fun getPartnerDisplayName(userId: String?, userName: String?): String {
        return if (isAli(userId, userName)) "Kevin" else "Ali"
    }

    fun init(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
    }

    fun setCachedZones(zones: List<RadarPlaceZone>) {
        cachedZones = zones
    }

    fun getLastKnownLocationFallback(context: Context): Location? {
        val locMan = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER,
            android.location.LocationManager.PASSIVE_PROVIDER
        )
        val locations = mutableListOf<Location>()
        for (p in providers) {
            try {
                val l = locMan.getLastKnownLocation(p)
                if (l != null) locations.add(l)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return locations.maxByOrNull { it.time }
    }

    fun getBatteryStatus(context: Context): Pair<Int, Boolean> {
        var batteryPct = -1
        var isCharging = false
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPct = ((level / scale.toFloat()) * 100).toInt()
                }
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL ||
                             plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                             plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                             plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                             plugged > 0
            }

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (bm != null) {
                if (batteryPct !in 0..100) {
                    val cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    if (cap in 0..100) {
                        batteryPct = cap
                    }
                }
                if (!isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                        isCharging = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo batería: ${e.message}")
        }
        if (batteryPct !in 0..100) batteryPct = 100
        return Pair(batteryPct, isCharging)
    }

    @SuppressLint("MissingPermission")
    fun publishHeartbeat(context: Context, loc: Location? = null) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val rawUserId = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        val rawUserName = prefs.getString("userName", null)
        val coupleId = normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123"))
        val isSharing = prefs.getBoolean("radar_is_sharing", true)

        val docName = getMyDocName(rawUserId, rawUserName)
        val displayName = getMyDisplayName(rawUserId, rawUserName)
        val finalUserId = if (docName == "ali") "user_ali_02" else "user_kevin_01"

        val batteryInfo = getBatteryStatus(appContext)
        val now = System.currentTimeMillis()

        val activeLoc = loc ?: getLastKnownLocationFallback(appContext)
        val lat = activeLoc?.latitude ?: lastUploadedLat
        val lon = activeLoc?.longitude ?: lastUploadedLng
        val accuracy = activeLoc?.accuracy ?: 0f
        val speedKmh = if (activeLoc != null && activeLoc.hasSpeed()) activeLoc.speed * 3.6f else 0f

        val activity = when {
            speedKmh > 20f -> "IN_VEHICLE"
            speedKmh > 2.5f -> "WALKING"
            else -> "STILL"
        }

        val matchingZone = if (lat != 0.0) findMatchingZone(lat, lon, cachedZones) else null
        val zoneName = matchingZone?.name ?: ""

        var address = ""
        if (lat != 0.0) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    val geocoder = Geocoder(appContext, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val thoroughfare = addr.thoroughfare ?: ""
                        val locality = addr.locality ?: addr.subAdminArea ?: ""
                        address = if (thoroughfare.isNotEmpty()) "$thoroughfare, $locality" else locality
                    }
                }
            } catch (e: Exception) {
                // Ignore geocoding failure
            }
        }

        val profileImageUrl = prefs.getString("userImage", "") ?: ""

        val locationData = RadarLocationData(
            userId = finalUserId,
            userName = displayName,
            profileImageUrl = profileImageUrl,
            latitude = lat,
            longitude = lon,
            accuracy = accuracy,
            speedKmh = speedKmh,
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            activity = activity,
            currentZone = zoneName,
            address = address,
            timestamp = now,
            isSharing = isSharing
        )

        db.collection("locations").document(coupleId)
            .collection("users").document(docName)
            .set(locationData.toMap(), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Heartbeat y estado emitido para $docName: bat=${batteryInfo.first}%, lat=$lat, lon=$lon")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error emitiendo heartbeat", e)
            }

        if (lat != 0.0 && lon != 0.0) {
            val distMoved = calculateDistance(lastUploadedLat, lastUploadedLng, lat, lon)
            if (distMoved > 50f || (now - lastUploadedTime) > 300_000L) {
                lastUploadedLat = lat
                lastUploadedLng = lon
                lastUploadedTime = now

                val historyPoint = RadarHistoryPoint(
                    latitude = lat,
                    longitude = lon,
                    placeName = zoneName.ifEmpty { address },
                    timestamp = now,
                    speedKmh = speedKmh
                )

                db.collection("locations").document(coupleId)
                    .collection("history_${docName}").document(now.toString())
                    .set(historyPoint.toMap())
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun forceLocationUpdate(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val appContext = context.applicationContext
        init(appContext)

        // 1. Emitir estado y batería de inmediato
        publishHeartbeat(appContext, getLastKnownLocationFallback(appContext))

        if (!PermissionHelper.hasLocationPermission(appContext)) {
            onComplete?.invoke(true)
            return
        }

        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) {
                    handleNewLocation(appContext, loc)
                    onComplete?.invoke(true)
                }
            }
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener { loc ->
                    if (loc != null) {
                        handleNewLocation(appContext, loc)
                        onComplete?.invoke(true)
                    } else {
                        fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            ?.addOnSuccessListener { balancedLoc ->
                                if (balancedLoc != null) {
                                    handleNewLocation(appContext, balancedLoc)
                                    onComplete?.invoke(true)
                                } else {
                                    publishHeartbeat(appContext)
                                    onComplete?.invoke(true)
                                }
                            }
                    }
                }
                ?.addOnFailureListener {
                    publishHeartbeat(appContext)
                    onComplete?.invoke(true)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en forceLocationUpdate", e)
            publishHeartbeat(appContext)
            onComplete?.invoke(true)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLiveTracking(context: Context, intervalMillis: Long = 15000L) {
        val appContext = context.applicationContext
        init(appContext)

        // Emitir heartbeat inmediato con batería y estado
        publishHeartbeat(appContext, getLastKnownLocationFallback(appContext))

        if (!PermissionHelper.hasLocationPermission(appContext)) {
            Log.w(TAG, "No hay permisos de ubicación para iniciar tracking continuo")
            return
        }

        stopLiveTracking()

        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) {
                    handleNewLocation(appContext, loc)
                }
            }
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener { loc ->
                    if (loc != null) {
                        handleNewLocation(appContext, loc)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo ubicación inicial rápida", e)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                handleNewLocation(appContext, loc)
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Tracking de ubicación iniciado con intervalo $intervalMillis ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando requestLocationUpdates", e)
        }
    }

    fun stopLiveTracking() {
        locationCallback?.let {
            try {
                fusedLocationClient?.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo location updates", e)
            }
            locationCallback = null
        }
    }

    fun handleNewLocation(context: Context, loc: Location) {
        publishHeartbeat(context, loc)
    }

    fun findMatchingZone(lat: Double, lon: Double, zones: List<RadarPlaceZone>): RadarPlaceZone? {
        for (z in zones) {
            val dist = calculateDistance(lat, lon, z.latitude, z.longitude)
            if (dist <= z.radiusMeters) {
                return z
            }
        }
        return null
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        if (lat1 == 0.0 && lon1 == 0.0) return 0f
        if (lat2 == 0.0 && lon2 == 0.0) return 0f
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLonRad = Math.toRadians(lon2 - lon1)

        val y = sin(dLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)

        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        if (bearing < 0) {
            bearing += 360f
        }
        return bearing
    }

    fun getBearingDirectionName(bearing: Float): String {
        return when (bearing) {
            in 22.5..67.5 -> "Noreste (NE)"
            in 67.5..112.5 -> "Este (E)"
            in 112.5..157.5 -> "Sureste (SE)"
            in 157.5..202.5 -> "Sur (S)"
            in 202.5..247.5 -> "Suroeste (SO)"
            in 247.5..292.5 -> "Oeste (O)"
            in 292.5..337.5 -> "Noroeste (NO)"
            else -> "Norte (N)"
        }
    }

    fun triggerSos(context: Context, coupleId: String, userId: String, userName: String) {
        val safeCoupleId = normalizeCoupleId(coupleId)
        val docName = getMyDocName(userId, userName)
        val displayName = getMyDisplayName(userId, userName)
        val myUserId = if (docName == "ali") "user_ali_02" else "user_kevin_01"
        val now = System.currentTimeMillis()
        val updateMap = mapOf(
            "sosActive" to true,
            "sosTimestamp" to now
        )

        db.collection("locations").document(safeCoupleId)
            .collection("users").document(docName)
            .set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Alerta SOS guardada exitosamente en Firestore para $docName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error guardando alerta SOS en Firestore", e)
            }

        // Enviar notificación FCM de emergencia
        sendEmergencyNotification(context, safeCoupleId, myUserId, displayName)
    }

    fun cancelSos(coupleId: String, userId: String) {
        val safeCoupleId = normalizeCoupleId(coupleId)
        val docName = getMyDocName(userId, null)
        val updateMap = mapOf(
            "sosActive" to false,
            "sosTimestamp" to 0L
        )

        db.collection("locations").document(safeCoupleId)
            .collection("users").document(docName)
            .set(updateMap, SetOptions.merge())
    }

    private fun sendEmergencyNotification(context: Context, coupleId: String, senderId: String, senderName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val creds = MainActivity.getGoogleCredentials(context)
                val token = creds.accessToken.tokenValue
                val projectId = "diario-pareja-a2d35"
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val topicName = "diario_" + coupleId.lowercase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace("ñ", "n").replace(" ", "_")

                val jsonBody = JSONObject().apply {
                    val message = JSONObject().apply {
                        put("topic", topicName)
                        val notification = JSONObject().apply {
                            put("title", "🚨 ¡ALERTA SOS DE $senderName!")
                            put("body", "¡$senderName ha activado la alerta de emergencia en Thor Radar! Toca para ver su ubicación en vivo.")
                        }
                        put("notification", notification)
                        val data = JSONObject().apply {
                            put("authorId", senderId)
                            put("click_type", "sos")
                            put("type", "sos")
                            put("title", "🚨 ¡ALERTA SOS DE $senderName!")
                            put("body", "¡$senderName ha activado la alerta de emergencia en Thor Radar! Toca para ver su ubicación en vivo.")
                        }
                        put("data", data)
                        val android = JSONObject().apply {
                            put("priority", "HIGH")
                            val androidNotif = JSONObject().apply {
                                put("channel_id", "diario_channel")
                                put("sound", "default")
                                put("default_vibrate_timings", true)
                            }
                            put("notification", androidNotif)
                        }
                        put("android", android)
                    }
                    put("message", message)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonBody.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                Log.d(TAG, "SOS FCM response code: ${response.code}")
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando notificación SOS FCM", e)
            }
        }
    }
}
