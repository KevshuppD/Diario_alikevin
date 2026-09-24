package calendario.kevshupp.diariokevinali

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

data class RadarSearchResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

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
    val sosTimestamp: Long = 0L,
    val isOnline: Boolean = true
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
            "sosTimestamp" to sosTimestamp,
            "isOnline" to isOnline
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot?): RadarLocationData {
            if (doc == null || !doc.exists()) return RadarLocationData()
            val data = doc.data ?: return RadarLocationData()
            return fromMap(data)
        }

        fun fromDataSnapshot(snapshot: DataSnapshot?): RadarLocationData {
            if (snapshot == null || !snapshot.exists()) return RadarLocationData()
            val data = snapshot.value as? Map<*, *> ?: return RadarLocationData()
            return fromMap(data)
        }

        fun fromMap(data: Map<*, *>?): RadarLocationData {
            if (data == null) return RadarLocationData()
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
                isSharing = data["isSharing"] as? Boolean ?: (data["isSharingLocation"] as? Boolean ?: true),
                sosActive = data["sosActive"] as? Boolean ?: false,
                sosTimestamp = (data["sosTimestamp"] as? Number)?.toLong() ?: 0L,
                isOnline = data["isOnline"] as? Boolean ?: true
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
    val addedBy: String = "",
    val wifiSsid: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "icon" to icon,
            "latitude" to latitude,
            "longitude" to longitude,
            "radiusMeters" to radiusMeters,
            "addedBy" to addedBy,
            "wifiSsid" to wifiSsid
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
                addedBy = map["addedBy"] as? String ?: "",
                wifiSsid = map["wifiSsid"] as? String ?: ""
            )
        }

        fun fromSnapshot(doc: DocumentSnapshot): RadarPlaceZone {
            val data = doc.data ?: return RadarPlaceZone(id = doc.id)
            return fromMap(data.plus("id" to doc.id))
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
    const val RTDB_URL = "https://diario-ali-kevin-default-rtdb.firebaseio.com"

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val db = FirebaseFirestore.getInstance()
    private var presenceSetup = false

    fun getDatabase(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    private var lastUploadedLat: Double = 0.0
    private var lastUploadedLng: Double = 0.0
    private var lastUploadedAccuracy: Float = 0f
    private var lastUploadedTime: Long = 0L
    private var lastUploadedBatteryPct: Int = -1
    private var lastUploadedChargingState: Boolean? = null
    private var lastHistoryLat: Double = 0.0
    private var lastHistoryLng: Double = 0.0
    private var lastHistoryPointTime: Long = 0L
    private var lastSpeedCalcLat: Double = 0.0
    private var lastSpeedCalcLng: Double = 0.0
    private var lastSpeedCalcTime: Long = 0L
    private var smoothedSpeedKmh: Float = 0f
    private var lastGeocodedLat: Double = 0.0
    private var lastGeocodedLng: Double = 0.0
    private var lastGeocodedTime: Long = 0L
    private var cachedAddress: String = ""
    private var cachedZones: List<RadarPlaceZone> = emptyList()
    private var zonesListener: ListenerRegistration? = null
    private var hasLoadedRemoteZones = false
    private var lastUploadedZone: String = ""
    var isForegroundTracking: Boolean = false

    fun getConnectedWifiSsid(context: Context): String? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val wifiInfo = caps.transportInfo as? WifiInfo
                        val ssid = wifiInfo?.ssid?.trim('"', ' ')
                        if (!ssid.isNullOrEmpty() && ssid != "<unknown ssid>" && ssid != "0x") {
                            return ssid
                        }
                    }
                }
            }

            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            val ssid = info?.ssid?.trim('"', ' ')
            if (!ssid.isNullOrEmpty() && ssid != "<unknown ssid>" && ssid != "0x") {
                return ssid
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo SSID Wi-Fi: ${e.message}")
        }
        return null
    }

    fun loadCachedZonesFromPrefs(context: Context): List<RadarPlaceZone> {
        try {
            val prefs = context.applicationContext.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("saved_zones_json", null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RadarPlaceZone>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val zone = RadarPlaceZone(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    icon = obj.optString("icon", "🏠"),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    radiusMeters = obj.optDouble("radiusMeters", 150.0).toFloat(),
                    addedBy = obj.optString("addedBy"),
                    wifiSsid = obj.optString("wifiSsid", "")
                )
                if (zone.id.isNotEmpty() && zone.latitude != 0.0) {
                    list.add(zone)
                }
            }
            return list
        } catch (e: Exception) {
            Log.w(TAG, "Error cargando zonas guardadas: ${e.message}")
            return emptyList()
        }
    }

    fun saveCachedZonesToPrefs(context: Context, zones: List<RadarPlaceZone>) {
        try {
            val jsonArray = JSONArray()
            for (z in zones) {
                val obj = JSONObject().apply {
                    put("id", z.id)
                    put("name", z.name)
                    put("icon", z.icon)
                    put("latitude", z.latitude)
                    put("longitude", z.longitude)
                    put("radiusMeters", z.radiusMeters.toDouble())
                    put("addedBy", z.addedBy)
                    put("wifiSsid", z.wifiSsid)
                }
                jsonArray.put(obj)
            }
            val prefs = context.applicationContext.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("saved_zones_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando zonas en prefs: ${e.message}")
        }
    }

    fun saveCachedLocation(context: Context, docName: String, data: RadarLocationData) {
        val prev = loadCachedLocation(context, docName)
        val latToSave = if (data.latitude != 0.0) data.latitude else (prev?.latitude ?: 0.0)
        val lonToSave = if (data.longitude != 0.0) data.longitude else (prev?.longitude ?: 0.0)
        val accToSave = if (data.accuracy > 0f) data.accuracy else (prev?.accuracy ?: 0f)
        val addrToSave = if (data.address.isNotBlank()) data.address else (prev?.address ?: "")
        val zoneToSave = if (data.currentZone.isNotBlank()) data.currentZone else (prev?.currentZone ?: "")

        if (latToSave == 0.0 && lonToSave == 0.0 && data.timestamp == 0L) return
        try {
            val prefs = context.applicationContext.getSharedPreferences("ThorRadarLocationPrefs", Context.MODE_PRIVATE)
            val obj = JSONObject().apply {
                put("userId", data.userId)
                put("userName", data.userName)
                put("profileImageUrl", data.profileImageUrl)
                put("latitude", latToSave)
                put("longitude", lonToSave)
                put("accuracy", accToSave.toDouble())
                put("speedKmh", data.speedKmh.toDouble())
                put("batteryLevel", data.batteryLevel)
                put("isCharging", data.isCharging)
                put("activity", data.activity)
                put("currentZone", zoneToSave)
                put("address", addrToSave)
                put("timestamp", data.timestamp)
                put("isSharing", data.isSharing)
                put("sosActive", data.sosActive)
                put("sosTimestamp", data.sosTimestamp)
            }
            prefs.edit().putString("loc_$docName", obj.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando ubicación en cache ($docName): ${e.message}")
        }
    }

    fun loadCachedLocation(context: Context, docName: String): RadarLocationData? {
        try {
            val prefs = context.applicationContext.getSharedPreferences("ThorRadarLocationPrefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("loc_$docName", null) ?: return null
            val obj = JSONObject(jsonStr)
            return RadarLocationData(
                userId = obj.optString("userId", ""),
                userName = obj.optString("userName", ""),
                profileImageUrl = obj.optString("profileImageUrl", ""),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                accuracy = obj.optDouble("accuracy", 0.0).toFloat(),
                speedKmh = obj.optDouble("speedKmh", 0.0).toFloat(),
                batteryLevel = obj.optInt("batteryLevel", 100),
                isCharging = obj.optBoolean("isCharging", false),
                activity = obj.optString("activity", "STILL"),
                currentZone = obj.optString("currentZone", ""),
                address = obj.optString("address", ""),
                timestamp = obj.optLong("timestamp", 0L),
                isSharing = obj.optBoolean("isSharing", true),
                sosActive = obj.optBoolean("sosActive", false),
                sosTimestamp = obj.optLong("sosTimestamp", 0L)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error cargando ubicación de cache ($docName): ${e.message}")
            return null
        }
    }

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

    private var appContextRef: Context? = null

    fun init(context: Context) {
        appContextRef = context.applicationContext
        if (cachedZones.isEmpty()) {
            val localZones = loadCachedZonesFromPrefs(context.applicationContext)
            if (localZones.isNotEmpty()) {
                cachedZones = localZones
                Log.d(TAG, "Zonas cargadas inmediatamente desde caché local: ${localZones.size}")
            }
        }
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
        val prefs = context.applicationContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val coupleId = normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123"))
        startListeningToZones(coupleId, context.applicationContext)
        setupPresence(context.applicationContext)
    }

    fun setupPresence(context: Context) {
        if (presenceSetup) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val rawUserId = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        val rawUserName = prefs.getString("userName", null)
        val coupleId = normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123"))
        val docName = getMyDocName(rawUserId, rawUserName)

        try {
            val rtdb = getDatabase()
            val connectedRef = rtdb.reference.child(".info/connected")
            val userRef = rtdb.reference.child("locations").child(coupleId).child("users").child(docName)

            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userRef.child("isOnline").onDisconnect().setValue(false)
                        userRef.child("isOnline").setValue(true)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Presence listener cancelado: ${error.message}")
                }
            })
            presenceSetup = true
        } catch (e: Exception) {
            Log.w(TAG, "Error configurando presencia RTDB: ${e.message}")
        }
    }

    fun startListeningToZones(coupleId: String, context: Context? = null) {
        val safeCoupleId = normalizeCoupleId(coupleId)
        if (context != null) {
            appContextRef = context.applicationContext
        }
        if (cachedZones.isEmpty() && context != null) {
            val localZones = loadCachedZonesFromPrefs(context.applicationContext)
            if (localZones.isNotEmpty()) {
                cachedZones = localZones
            }
        }
        if (zonesListener != null) return
        try {
            zonesListener = db.collection("locations").document(safeCoupleId)
                .collection("zones")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val list = snapshot.documents.mapNotNull { RadarPlaceZone.fromSnapshot(it) }
                        hasLoadedRemoteZones = true
                        cachedZones = list
                        val ctx = appContextRef ?: context
                        if (ctx != null) {
                            saveCachedZonesToPrefs(ctx, list)
                        }
                        Log.d(TAG, "Zonas seguras sincronizadas en memoria: ${list.size}")

                        if (ctx != null) {
                            val prefs = ctx.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
                            val lastZoneId = prefs.getString("last_active_zone_id", "") ?: ""
                            if (lastZoneId.isNotEmpty()) {
                                val activeZone = list.firstOrNull { it.id == lastZoneId }
                                if (activeZone != null) {
                                    prefs.edit()
                                        .putString("last_active_zone_name", activeZone.name)
                                        .putString("last_active_zone_icon", activeZone.icon)
                                        .apply()
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando listener de zonas", e)
        }
    }

    fun updateZoneInCache(context: Context, zone: RadarPlaceZone) {
        appContextRef = context.applicationContext
        val current = cachedZones.toMutableList()
        val index = current.indexOfFirst { it.id == zone.id }
        if (index >= 0) {
            current[index] = zone
        } else {
            current.add(zone)
        }
        cachedZones = current
        saveCachedZonesToPrefs(context, current)

        val prefs = context.applicationContext.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
        val lastZoneId = prefs.getString("last_active_zone_id", "") ?: ""
        if (lastZoneId == zone.id) {
            prefs.edit()
                .putString("last_active_zone_name", zone.name)
                .putString("last_active_zone_icon", zone.icon)
                .apply()
        }
    }

    fun removeZoneFromCache(context: Context, zoneId: String) {
        appContextRef = context.applicationContext
        cachedZones = cachedZones.filterNot { it.id == zoneId }
        saveCachedZonesToPrefs(context, cachedZones)

        val prefs = context.applicationContext.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
        val lastZoneId = prefs.getString("last_active_zone_id", "") ?: ""
        if (lastZoneId == zoneId) {
            prefs.edit()
                .putString("last_active_zone_id", "")
                .putString("last_active_zone_name", "")
                .putString("last_active_zone_icon", "")
                .remove("pending_exit_zone_id")
                .remove("pending_exit_count")
                .apply()
        }
    }

    fun setCachedZones(zones: List<RadarPlaceZone>) {
        cachedZones = zones
        appContextRef?.let { ctx ->
            saveCachedZonesToPrefs(ctx, zones)
        }
    }

    fun loadCachedLocationAsLocation(context: Context, docName: String): Location? {
        val cached = loadCachedLocation(context, docName) ?: return null
        if (cached.latitude == 0.0 && cached.longitude == 0.0) return null
        return Location("cache").apply {
            latitude = cached.latitude
            longitude = cached.longitude
            accuracy = if (cached.accuracy > 0f) cached.accuracy else 40f
            time = if (cached.timestamp > 0L) cached.timestamp else System.currentTimeMillis()
        }
    }

    fun getLastKnownLocationFallback(context: Context): Location? {
        val locMan = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
        val now = System.currentTimeMillis()
        val providers = listOf(
            "fused",
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER,
            android.location.LocationManager.PASSIVE_PROVIDER
        )
        val candidateLocations = mutableListOf<Location>()
        for (p in providers) {
            try {
                val l = locMan.getLastKnownLocation(p)
                if (l != null && l.latitude != 0.0 && l.longitude != 0.0) {
                    candidateLocations.add(l)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (candidateLocations.isEmpty()) {
            return null
        }

        // Priorizar lecturas relativamente recientes (< 45 min) y con buena precisión (<= 120m)
        val recentAndAccurate = candidateLocations.filter { (now - it.time) < 45 * 60 * 1000L && it.accuracy <= 120f }
        if (recentAndAccurate.isNotEmpty()) {
            return recentAndAccurate.minByOrNull { it.accuracy } ?: recentAndAccurate.maxByOrNull { it.time }
        }

        // Si no hay recientes, devolver el mejor candidato conocido disponible
        return candidateLocations.minByOrNull { it.accuracy } ?: candidateLocations.maxByOrNull { it.time }
    }

    fun publishBatteryUpdate(context: Context, batteryLevel: Int, isCharging: Boolean) {
        if (batteryLevel !in 0..100) return
        if (batteryLevel == lastUploadedBatteryPct && isCharging == lastUploadedChargingState) {
            return
        }
        lastUploadedBatteryPct = batteryLevel
        lastUploadedChargingState = isCharging

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val rawUserId = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        val rawUserName = prefs.getString("userName", null)
        val coupleId = normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123"))
        val docName = getMyDocName(rawUserId, rawUserName)

        val rtdb = getDatabase()
        val userRef = rtdb.reference.child("locations").child(coupleId).child("users").child(docName)

        val updates = hashMapOf<String, Any>(
            "batteryLevel" to batteryLevel,
            "isCharging" to isCharging,
            "timestamp" to System.currentTimeMillis()
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "⚡ [BATTERY RTDB] Actualizado estado en tiempo real para $docName: $batteryLevel%, isCharging=$isCharging")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error actualizando batería en RTDB ($docName)", e)
            }
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
    fun publishHeartbeat(context: Context, loc: Location? = null, force: Boolean = false) {
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

        val fallbackLoc = getLastKnownLocationFallback(appContext)
        val activeLoc = loc ?: fallbackLoc
        val cachedLoc = loadCachedLocation(appContext, docName)

        // Si la lectura activa tiene precisión muy pobre (> 100m) y ya tenemos una posición previa válida, mantener la posición previa
        val (lat, lon, accuracy) = if (activeLoc != null && activeLoc.latitude != 0.0 && activeLoc.longitude != 0.0) {
            if (activeLoc.accuracy > 100f && lastUploadedLat != 0.0 && (now - lastUploadedTime) < 180_000L) {
                Triple(lastUploadedLat, lastUploadedLng, lastUploadedAccuracy)
            } else {
                Triple(activeLoc.latitude, activeLoc.longitude, activeLoc.accuracy)
            }
        } else if (lastUploadedLat != 0.0 && lastUploadedLng != 0.0) {
            Triple(lastUploadedLat, lastUploadedLng, lastUploadedAccuracy)
        } else if (cachedLoc != null && cachedLoc.latitude != 0.0 && cachedLoc.longitude != 0.0) {
            Triple(cachedLoc.latitude, cachedLoc.longitude, if (cachedLoc.accuracy > 0f) cachedLoc.accuracy else 40f)
        } else {
            Triple(0.0, 0.0, 0f)
        }

        var calculatedSpeedKmh = 0f
        if (activeLoc != null && activeLoc.hasSpeed() && activeLoc.speed > 0.35f) {
            calculatedSpeedKmh = activeLoc.speed * 3.6f
        } else if (lastSpeedCalcTime > 0L && lastSpeedCalcLat != 0.0 && lat != 0.0) {
            val timeDiffSec = (now - lastSpeedCalcTime) / 1000f
            if (timeDiffSec in 1.2f..90.0f) {
                val distMeters = calculateDistance(lastSpeedCalcLat, lastSpeedCalcLng, lat, lon)
                val minMoveThreshold = maxOf(3.5f, accuracy * 0.35f)
                if (distMeters > minMoveThreshold) {
                    val rawSpeed = (distMeters / timeDiffSec) * 3.6f
                    if (rawSpeed in 0.5f..220f) {
                        calculatedSpeedKmh = rawSpeed
                    }
                }
            }
        }

        smoothedSpeedKmh = if (smoothedSpeedKmh == 0f || calculatedSpeedKmh == 0f) {
            calculatedSpeedKmh
        } else {
            (smoothedSpeedKmh * 0.35f + calculatedSpeedKmh * 0.65f)
        }

        val speedKmh = if (smoothedSpeedKmh < 1.8f) 0f else smoothedSpeedKmh

        if (lat != 0.0 && lon != 0.0) {
            lastSpeedCalcLat = lat
            lastSpeedCalcLng = lon
            lastSpeedCalcTime = now
        }

        val activity = when {
            speedKmh >= 20f -> "IN_VEHICLE"
            speedKmh >= 7.5f -> "RUNNING"
            speedKmh >= 2.0f -> "WALKING"
            else -> "STILL"
        }

        startListeningToZones(coupleId, appContext)

        val zones = if (cachedZones.isNotEmpty()) cachedZones else loadCachedZonesFromPrefs(appContext)
        if (cachedZones.isEmpty() && zones.isNotEmpty()) {
            cachedZones = zones
        }

        val currentWifi = getConnectedWifiSsid(appContext)
        val matchingZone = findMatchingZone(lat, lon, zones, currentWifi)
        val zoneName = matchingZone?.name ?: ""

        val zoneTransitionOccurred = if (isSharing && (lat != 0.0 || !currentWifi.isNullOrBlank())) {
            checkAndNotifyZoneTransitions(appContext, coupleId, finalUserId, displayName, lat, lon, accuracy, speedKmh, activity)
        } else {
            false
        }

        var address = cachedAddress
        if (lat != 0.0 && lon != 0.0) {
            val distFromLastGeocode = if (lastGeocodedLat != 0.0 && lastGeocodedLng != 0.0) {
                calculateDistance(lat, lon, lastGeocodedLat, lastGeocodedLng)
            } else {
                Float.MAX_VALUE
            }
            val timeSinceLastGeocode = System.currentTimeMillis() - lastGeocodedTime
            val needsGeocoding = cachedAddress.isEmpty() || distFromLastGeocode >= 40.0f || timeSinceLastGeocode >= 5 * 60 * 1000L

            if (needsGeocoding) {
                try {
                    val geocoder = Geocoder(appContext, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val addr = addresses[0]
                                val thoroughfare = addr.thoroughfare ?: ""
                                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                                val resAddr = if (thoroughfare.isNotEmpty() && locality.isNotEmpty()) "$thoroughfare, $locality" else thoroughfare.ifEmpty { locality }
                                if (resAddr.isNotEmpty()) {
                                    cachedAddress = resAddr
                                    lastGeocodedLat = lat
                                    lastGeocodedLng = lon
                                    lastGeocodedTime = System.currentTimeMillis()
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val thoroughfare = addr.thoroughfare ?: ""
                            val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                            val resAddr = if (thoroughfare.isNotEmpty() && locality.isNotEmpty()) "$thoroughfare, $locality" else thoroughfare.ifEmpty { locality }
                            if (resAddr.isNotEmpty()) {
                                cachedAddress = resAddr
                                address = resAddr
                                lastGeocodedLat = lat
                                lastGeocodedLng = lon
                                lastGeocodedTime = System.currentTimeMillis()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore geocoding failure
                }
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

        saveCachedLocation(context, docName, locationData)

        // SMART THROTTLING:
        // 1. Si force == true: acción explícita (usuario abrió radar, pulsó Actualizar o respondió a Magic Packet) -> subir inmediatamente.
        // 2. Si chargingChanged == true: cambió el estado de enchufe/cargador -> subir inmediatamente.
        // 3. Si zoneTransitionOccurred == true o zoneChanged == true: cambió la zona segura (entró o salió) -> subir inmediatamente para avisar al otro en tiempo real.
        // 4. Si isForegroundTracking == true (usuario con la pantalla de Thor Radar abierta):
        //    -> Subir en tiempo real si se desplazó >= 10 metros, si pasaron >= 10 segundos, o si la batería cambió.
        // 5. Si en background / pasivo (pantalla apagada o en segundo plano):
        //    -> En movimiento (caminando, auto, bici): subir si hubo desplazamiento real significativo (>= 40 metros y >= 60 segundos).
        //    -> En reposo / quieto: emitir latido cada 6 minutos (360 segundos) para mantener telemetría y batería fresca.
        //    -> O si el nivel de batería cambió (>= 3%) tras >= 60 segundos.
        val distMovedSinceUpload = if (lastUploadedLat != 0.0 && lat != 0.0) calculateDistance(lastUploadedLat, lastUploadedLng, lat, lon) else Float.MAX_VALUE
        val timeSinceUpload = now - lastUploadedTime
        val batteryPctChanged = Math.abs(batteryInfo.first - lastUploadedBatteryPct) >= 3
        val chargingChanged = (batteryInfo.second != lastUploadedChargingState)
        val zoneChanged = (zoneName != lastUploadedZone)

        val shouldUpload = when {
            force -> true
            chargingChanged -> true
            zoneTransitionOccurred -> true
            zoneChanged -> true
            isForegroundTracking -> (distMovedSinceUpload >= 10f || timeSinceUpload >= 10_000L || batteryPctChanged)
            else -> (
                (distMovedSinceUpload >= 40f && timeSinceUpload >= 60_000L) ||
                timeSinceUpload >= 360_000L || // 6 min en reposo
                (timeSinceUpload >= 60_000L && batteryPctChanged)
            )
        }

        if (!shouldUpload) {
            Log.d(TAG, "Heartbeat omitido por throttling (isFg=$isForegroundTracking, dist=${distMovedSinceUpload.toInt()}m, dt=${timeSinceUpload / 1000}s)")
            return
        }

        lastUploadedLat = if (lat != 0.0) lat else lastUploadedLat
        lastUploadedLng = if (lon != 0.0) lon else lastUploadedLng
        lastUploadedAccuracy = accuracy
        lastUploadedTime = now
        lastUploadedBatteryPct = batteryInfo.first
        lastUploadedChargingState = batteryInfo.second
        lastUploadedZone = zoneName

        val rtdb = getDatabase()
        val userRef = rtdb.reference.child("locations").child(coupleId).child("users").child(docName)

        val rtdbMap = locationData.toMap()

        userRef.setValue(rtdbMap)
            .addOnSuccessListener {
                Log.d(TAG, "Heartbeat RTDB emitido para $docName: bat=${batteryInfo.first}%, lat=$lat, lon=$lon")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error emitiendo heartbeat RTDB ($docName)", e)
            }

        if (isSharing && lat != 0.0 && lon != 0.0) {
            val distMovedSinceHistory = if (lastHistoryLat != 0.0 && lastHistoryLng != 0.0) {
                calculateDistance(lastHistoryLat, lastHistoryLng, lat, lon)
            } else {
                Float.MAX_VALUE
            }
            val timeSinceHistory = now - lastHistoryPointTime

            // Registrar historial SOLO si hubo desplazamiento real significativo (> 300m) y pasaron al menos 10 minutos
            if (distMovedSinceHistory >= 300f && timeSinceHistory >= 600_000L) {
                lastHistoryLat = lat
                lastHistoryLng = lon
                lastHistoryPointTime = now

                val historyPoint = RadarHistoryPoint(
                    latitude = lat,
                    longitude = lon,
                    placeName = zoneName.ifEmpty { address },
                    timestamp = now,
                    speedKmh = speedKmh
                )

                val histCol = db.collection("locations").document(coupleId)
                    .collection("history_${docName}")

                histCol.document(now.toString())
                    .set(historyPoint.toMap())
                    .addOnSuccessListener {
                        // Limpieza automática: Mantener un tope estricto de los 30 puntos más recientes
                        histCol.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                            .limit(10)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.size() > 0) {
                                    histCol.get().addOnSuccessListener { allDocs ->
                                        if (allDocs.size() > 30) {
                                            val toDelete = allDocs.documents.sortedBy { (it.get("timestamp") as? Number)?.toLong() ?: 0L }.take(allDocs.size() - 30)
                                            val batch = db.batch()
                                            for (d in toDelete) {
                                                batch.delete(d.reference)
                                            }
                                            batch.commit()
                                        }
                                    }
                                }
                            }
                    }
            }
        }
    }

    private var nativeLocationListener: android.location.LocationListener? = null

    @SuppressLint("MissingPermission")
    fun forceLocationUpdate(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val isSharing = prefs.getBoolean("radar_is_sharing", true)
        init(appContext)

        if (!isSharing) {
            publishHeartbeat(appContext, null, force = true)
            onComplete?.invoke(true)
            return
        }

        if (!PermissionHelper.hasLocationPermission(appContext)) {
            publishHeartbeat(appContext, getLastKnownLocationFallback(appContext), force = true)
            onComplete?.invoke(true)
            return
        }

        val fallback = getLastKnownLocationFallback(appContext)
        // Publicar inmediatamente la última ubicación conocida para respuesta instantánea
        if (fallback != null) {
            publishHeartbeat(appContext, fallback, force = true)
        }

        try {
            val cts = CancellationTokenSource()
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                ?.addOnSuccessListener { loc ->
                    val finalLoc = loc ?: fallback
                    publishHeartbeat(appContext, finalLoc, force = true)
                    onComplete?.invoke(true)
                }
                ?.addOnFailureListener {
                    if (fallback != null) {
                        publishHeartbeat(appContext, fallback, force = true)
                    }
                    onComplete?.invoke(true)
                }

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    cts.cancel()
                } catch (e: Exception) {}
            }, 5000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error en forceLocationUpdate", e)
            val fb = getLastKnownLocationFallback(appContext)
            publishHeartbeat(appContext, fb, force = true)
            onComplete?.invoke(true)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLiveTracking(context: Context, intervalMillis: Long = 10000L, isForeground: Boolean = true) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val isSharing = prefs.getBoolean("radar_is_sharing", true)
        isForegroundTracking = isForeground
        if (!isSharing) {
            Log.d(TAG, "startLiveTracking cancelado: radar_is_sharing está desactivado")
            stopLiveTracking()
            return
        }
        init(appContext)

        // Emitir heartbeat inmediato con batería y estado
        publishHeartbeat(appContext, getLastKnownLocationFallback(appContext), force = isForeground)

        if (!PermissionHelper.hasLocationPermission(appContext)) {
            Log.w(TAG, "No hay permisos de ubicación para iniciar tracking continuo")
            return
        }

        stopLiveTracking()
        isForegroundTracking = isForeground

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
            .setMinUpdateIntervalMillis(maxOf(3000L, intervalMillis / 2))
            .setMinUpdateDistanceMeters(if (isForeground) 5f else 15f)
            .setMaxUpdateDelayMillis(intervalMillis * 2)
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
            Log.d(TAG, "Tracking de ubicación Fused iniciado con intervalo $intervalMillis ms (isFg=$isForeground)")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando requestLocationUpdates", e)
        }

        try {
            val locMan = appContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (locMan != null) {
                nativeLocationListener = android.location.LocationListener { loc ->
                    handleNewLocation(appContext, loc)
                }
                if (locMan.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    locMan.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        intervalMillis,
                        if (isForeground) 5f else 15f,
                        nativeLocationListener!!,
                        Looper.getMainLooper()
                    )
                }
                if (locMan.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    locMan.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        intervalMillis,
                        if (isForeground) 10f else 25f,
                        nativeLocationListener!!,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error iniciando LocationManager nativo", e)
        }
    }

    fun stopLiveTracking() {
        isForegroundTracking = false
        locationCallback?.let {
            try {
                fusedLocationClient?.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo location updates", e)
            }
            locationCallback = null
        }

        nativeLocationListener?.let { listener ->
            try {
                val locMan = appContextRef?.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                locMan?.removeUpdates(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo LocationManager nativo", e)
            }
            nativeLocationListener = null
        }
    }

    fun handleNewLocation(context: Context, loc: Location) {
        publishHeartbeat(context, loc)
    }

    fun findMatchingZone(
        lat: Double,
        lon: Double,
        zones: List<RadarPlaceZone>,
        connectedWifiSsid: String? = null
    ): RadarPlaceZone? {
        if (zones.isEmpty()) return null

        // 1. Prioridad 1: Coincidencia por Wi-Fi SSID conectado (100% certeza de ubicación en interiores)
        if (!connectedWifiSsid.isNullOrBlank()) {
            val wifiNormalized = connectedWifiSsid.trim().lowercase()
            val wifiMatch = zones.firstOrNull {
                it.wifiSsid.isNotBlank() && it.wifiSsid.trim().lowercase() == wifiNormalized
            }
            if (wifiMatch != null) {
                return wifiMatch
            }
        }

        // 2. Coincidencia geográfica por radio GPS
        if (lat == 0.0 && lon == 0.0) return null
        return zones
            .map { it to calculateDistance(lat, lon, it.latitude, it.longitude) }
            .filter { (zone, dist) -> dist <= zone.radiusMeters }
            .minByOrNull { (_, dist) -> dist }
            ?.first
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
        val updateMap = mapOf<String, Any>(
            "sosActive" to true,
            "sosTimestamp" to now
        )

        try {
            val rtdb = getDatabase()
            rtdb.reference.child("locations").child(safeCoupleId)
                .child("users").child(docName)
                .updateChildren(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Alerta SOS guardada exitosamente en RTDB para $docName")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error guardando alerta SOS en RTDB", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error accediendo a RTDB para SOS", e)
        }

        // Enviar notificación FCM de emergencia
        sendEmergencyNotification(context, safeCoupleId, myUserId, displayName)
    }

    fun cancelSos(coupleId: String, userId: String) {
        val safeCoupleId = normalizeCoupleId(coupleId)
        val docName = getMyDocName(userId, null)
        val updateMap = mapOf<String, Any>(
            "sosActive" to false,
            "sosTimestamp" to 0L
        )

        try {
            val rtdb = getDatabase()
            rtdb.reference.child("locations").child(safeCoupleId)
                .child("users").child(docName)
                .updateChildren(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Alerta SOS cancelada en RTDB para $docName")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelando SOS en RTDB", e)
        }
    }

    private fun sendEmergencyNotification(context: Context, coupleId: String, senderId: String, senderName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val creds = MainActivity.getGoogleCredentials(context)
                val token = creds.accessToken.tokenValue
                val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "diario-ali-kevin"
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val topicName = "diario_" + coupleId.lowercase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace("ñ", "n").replace(" ", "_")

                val jsonBody = JSONObject().apply {
                    val message = JSONObject().apply {
                        put("topic", topicName)
                        val data = JSONObject().apply {
                            put("authorId", senderId)
                            put("authorName", senderName)
                            put("click_type", "sos")
                            put("type", "sos")
                            put("title", "🚨 ¡ALERTA SOS DE $senderName!")
                            put("body", "¡$senderName ha activado la alerta de emergencia en Thor Radar! Toca para ver su ubicación en vivo.")
                        }
                        put("data", data)
                        val android = JSONObject().apply {
                            put("priority", "HIGH")
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

    private fun checkAndNotifyZoneTransitions(
        context: Context,
        coupleId: String,
        senderId: String,
        senderName: String,
        lat: Double,
        lon: Double,
        accuracy: Float,
        speedKmh: Float = 0f,
        activity: String = "STILL"
    ): Boolean {
        val zones = if (cachedZones.isNotEmpty()) cachedZones else loadCachedZonesFromPrefs(context)
        if (zones.isEmpty()) {
            return false
        }
        if (cachedZones.isEmpty()) {
            cachedZones = zones
        }

        val currentWifi = getConnectedWifiSsid(context)

        // Si no tenemos Wi-Fi y las coordenadas no son válidas, no podemos calcular
        if (lat == 0.0 && lon == 0.0 && currentWifi.isNullOrBlank()) return false

        val prefs = context.getSharedPreferences("ThorRadarZonePrefs", Context.MODE_PRIVATE)
        val lastZoneId = prefs.getString("last_active_zone_id", "") ?: ""
        val lastZoneName = prefs.getString("last_active_zone_name", "") ?: ""
        val lastZoneIcon = prefs.getString("last_active_zone_icon", "📍") ?: "📍"
        val lastEventType = prefs.getString("last_event_type", "") ?: ""
        val lastEventTime = prefs.getLong("last_event_time", 0L)
        val pendingExitZoneId = prefs.getString("pending_exit_zone_id", "") ?: ""
        val pendingExitCount = prefs.getInt("pending_exit_count", 0)
        val firstExitTime = prefs.getLong("first_exit_time", 0L)
        val wasOutside = prefs.getBoolean("was_outside_zone", false)
        val now = System.currentTimeMillis()

        var currentMatching = findMatchingZone(lat, lon, zones, currentWifi)

        // Anclaje por Wi-Fi de la zona previa: Si estamos conectados a la red Wi-Fi de la zona activa,
        // garantizamos 100% que seguimos en ella sin importar cuánto fluctúe el GPS en interiores
        if (lastZoneId.isNotEmpty()) {
            val prevZone = zones.firstOrNull { it.id == lastZoneId }
            if (prevZone != null && !currentWifi.isNullOrBlank() && prevZone.wifiSsid.isNotBlank() &&
                prevZone.wifiSsid.trim().equals(currentWifi.trim(), ignoreCase = true)
            ) {
                currentMatching = prevZone
            }
        }

        if (currentMatching != null) {
            // Usuario está dentro de la zona (por GPS o Wi-Fi)
            val isDifferentZone = (currentMatching.id != lastZoneId)
            val isReturningAfterOutside = (currentMatching.id == lastZoneId && (lastEventType == "EXIT" || wasOutside || (now - lastEventTime >= 3_600_000L)))
            val isDuplicateRecentEnter = (lastZoneId == currentMatching.id && lastEventType == "ENTER" && (now - lastEventTime) < 300_000L)
            val isWifiConfirmed = (!currentWifi.isNullOrBlank() && currentMatching.wifiSsid.isNotBlank() &&
                    currentMatching.wifiSsid.trim().equals(currentWifi.trim(), ignoreCase = true))

            // Anti-flapping: Si acaba de salir hace menos de 5 min y la re-entrada NO es por Wi-Fi,
            // exigir que la precisión sea buena (< 35m) y que esté bien centrado dentro de la zona
            val isFlappingEnter = (lastEventType == "EXIT" && (now - lastEventTime) < 300_000L && !isWifiConfirmed)
            if (isFlappingEnter && lat != 0.0 && lon != 0.0) {
                val dist = calculateDistance(lat, lon, currentMatching.latitude, currentMatching.longitude)
                if (accuracy > 35f || dist > currentMatching.radiusMeters * 0.75f) {
                    Log.d(TAG, "checkAndNotifyZoneTransitions: Re-entrada pospuesta por anti-flapping (acc=${accuracy}m, dist=${dist.toInt()}m)")
                    return false
                }
            }

            if ((isDifferentZone || isReturningAfterOutside) && !isDuplicateRecentEnter) {
                // Entrada confirmada
                prefs.edit()
                    .putString("last_active_zone_id", currentMatching.id)
                    .putString("last_active_zone_name", currentMatching.name)
                    .putString("last_active_zone_icon", currentMatching.icon)
                    .putString("last_event_type", "ENTER")
                    .putLong("last_event_time", now)
                    .putBoolean("was_outside_zone", false)
                    .remove("pending_exit_zone_id")
                    .remove("pending_exit_count")
                    .remove("first_exit_time")
                    .apply()

                val title = "${currentMatching.icon} ¡$senderName llegó a ${currentMatching.name}!"
                val body = "$senderName ha llegado a ${currentMatching.name} (${currentMatching.icon})."
                sendZonePushNotification(context, coupleId, senderId, senderName, title, body)
                Log.d(TAG, "Notificación de llegada emitida con éxito: ${currentMatching.name} (Wi-Fi: $isWifiConfirmed)")
                return true
            } else {
                // Sigue en la misma zona
                val editor = prefs.edit()
                if (pendingExitCount > 0 || pendingExitZoneId.isNotEmpty() || firstExitTime > 0L) {
                    editor.remove("pending_exit_zone_id")
                        .remove("pending_exit_count")
                        .remove("first_exit_time")
                }
                editor.putBoolean("was_outside_zone", false)
                if (lastEventType != "ENTER") {
                    editor.putString("last_event_type", "ENTER")
                }
                if (lastZoneName != currentMatching.name || lastZoneIcon != currentMatching.icon) {
                    editor.putString("last_active_zone_name", currentMatching.name)
                        .putString("last_active_zone_icon", currentMatching.icon)
                }
                editor.apply()
                return false
            }
        } else {
            // Usuario no coincide directamente con ninguna zona
            if (lastZoneId.isNotEmpty()) {
                val prevZone = zones.firstOrNull { it.id == lastZoneId }
                if (prevZone != null) {
                    // 1. REGLA 1: Si sigue conectado al Wi-Fi de la zona, JAMÁS salir
                    if (!currentWifi.isNullOrBlank() && prevZone.wifiSsid.isNotBlank() &&
                        prevZone.wifiSsid.trim().equals(currentWifi.trim(), ignoreCase = true)
                    ) {
                        Log.d(TAG, "checkAndNotifyZoneTransitions: Salida bloqueada por anclaje Wi-Fi activo ($currentWifi)")
                        if (pendingExitCount > 0 || firstExitTime > 0L) {
                            prefs.edit().remove("pending_exit_zone_id").remove("pending_exit_count").remove("first_exit_time").apply()
                        }
                        return false
                    }

                    // 2. REGLA 2: Si el usuario está en REPOSO (STILL / < 2.0 km/h), descarta salidas por jitter
                    val isStationary = activity == "STILL" || speedKmh < 2.0f
                    if (isStationary && lat != 0.0 && lon != 0.0) {
                        val dist = calculateDistance(lat, lon, prevZone.latitude, prevZone.longitude)
                        // A menos que esté a más de 300m y con precisión excelente (<20m), no salir en reposo
                        if (dist < prevZone.radiusMeters + 300f || accuracy > 20f) {
                            Log.d(TAG, "checkAndNotifyZoneTransitions: Salida bloqueada por reposo (STILL / speed=${speedKmh} km/h, dist=${dist.toInt()}m)")
                            if (pendingExitCount > 0 || firstExitTime > 0L) {
                                prefs.edit().remove("pending_exit_zone_id").remove("pending_exit_count").remove("first_exit_time").apply()
                            }
                            return false
                        }
                    }

                    // 3. REGLA 3: Precisión deficiente (interiores / antenas de celda) no puede confirmar salida
                    if (accuracy > 45f) {
                        Log.d(TAG, "checkAndNotifyZoneTransitions: Salida omitida por baja precisión GPS (${accuracy}m > 45m)")
                        return false
                    }

                    if (lat == 0.0 && lon == 0.0) return false
                    val dist = calculateDistance(lat, lon, prevZone.latitude, prevZone.longitude)
                    // Margen de histéresis: 45 metros más el error de precisión
                    val exitThreshold = prevZone.radiusMeters + maxOf(45f, accuracy * 0.5f)

                    if (dist > exitThreshold) {
                        val currentFirstExitTime = if (pendingExitZoneId == lastZoneId && firstExitTime > 0L) firstExitTime else now
                        val currentCount = if (pendingExitZoneId == lastZoneId) pendingExitCount + 1 else 1

                        prefs.edit()
                            .putString("pending_exit_zone_id", lastZoneId)
                            .putInt("pending_exit_count", currentCount)
                            .putLong("first_exit_time", currentFirstExitTime)
                            .apply()

                        val timeOutsideMs = now - currentFirstExitTime
                        val isDrivingAway = (speedKmh >= 15.0f && dist > (prevZone.radiusMeters + 80f))
                        val isWalkingAway = (currentCount >= 3 && timeOutsideMs >= 60_000L && dist > (prevZone.radiusMeters + 50f) && speedKmh >= 2.5f)
                        val isSustainedTimeOutside = (currentCount >= 4 && timeOutsideMs >= 90_000L && dist > (prevZone.radiusMeters + 70f))

                        if (!isDrivingAway && !isWalkingAway && !isSustainedTimeOutside) {
                            Log.d(TAG, "Salida preliminar para ${prevZone.name} (dist=${dist.toInt()}m, time=${timeOutsideMs/1000}s, count=$currentCount). Esperando confirmación...")
                            return false
                        }

                        // Salida confirmada
                        val isRecentExit = (lastEventType == "EXIT" && (now - lastEventTime) < 180_000L)
                        if (!isRecentExit) {
                            prefs.edit()
                                .putString("last_active_zone_id", "")
                                .putString("last_active_zone_name", "")
                                .putString("last_active_zone_icon", "")
                                .putString("last_event_type", "EXIT")
                                .putLong("last_event_time", now)
                                .putBoolean("was_outside_zone", true)
                                .remove("pending_exit_zone_id")
                                .remove("pending_exit_count")
                                .remove("first_exit_time")
                                .apply()

                            val exitZoneName = prevZone.name.takeIf { it.isNotBlank() } ?: lastZoneName
                            val exitZoneIcon = prevZone.icon.takeIf { it.isNotBlank() } ?: lastZoneIcon

                            val title = "🚗 ¡$senderName salió de $exitZoneName!"
                            val body = "$senderName ha salido de $exitZoneName ($exitZoneIcon)."
                            sendZonePushNotification(context, coupleId, senderId, senderName, title, body)
                            Log.d(TAG, "Notificación de salida confirmada emitida: $exitZoneName (dist=${dist.toInt()}m, speed=${speedKmh}km/h)")
                            return true
                        }
                    } else {
                        // Está dentro del margen de histéresis: resetear contador
                        if (pendingExitCount > 0 || firstExitTime > 0L) {
                            prefs.edit()
                                .remove("pending_exit_zone_id")
                                .remove("pending_exit_count")
                                .remove("first_exit_time")
                                .apply()
                        }
                    }
                } else if (hasLoadedRemoteZones) {
                    // La zona fue eliminada remotamente del mapa
                    prefs.edit()
                        .putString("last_active_zone_id", "")
                        .putString("last_active_zone_name", "")
                        .putString("last_active_zone_icon", "")
                        .putString("last_event_type", "EXIT")
                        .putLong("last_event_time", now)
                        .putBoolean("was_outside_zone", true)
                        .remove("pending_exit_zone_id")
                        .remove("pending_exit_count")
                        .remove("first_exit_time")
                        .apply()
                }
            } else {
                if (!wasOutside) {
                    prefs.edit().putBoolean("was_outside_zone", true).apply()
                }
            }
            return false
        }
    }

    private fun sendZonePushNotification(
        context: Context,
        coupleId: String,
        senderId: String,
        senderName: String,
        title: String,
        body: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val creds = MainActivity.getGoogleCredentials(context)
                val token = creds.accessToken.tokenValue
                val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "diario-ali-kevin"
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val topicName = "diario_" + coupleId.lowercase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace("ñ", "n").replace(" ", "_")

                val jsonBody = JSONObject().apply {
                    val message = JSONObject().apply {
                        put("topic", topicName)
                        val data = JSONObject().apply {
                            put("authorId", senderId)
                            put("authorName", senderName)
                            put("click_type", "radar")
                            put("type", "radar")
                            put("title", title)
                            put("body", body)
                        }
                        put("data", data)
                        val android = JSONObject().apply {
                            put("priority", "HIGH")
                        }
                        put("android", android)
                    }
                    put("message", message)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val reqBody = jsonBody.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                Log.d(TAG, "Zone FCM push status code: ${response.code}")
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Zone FCM push falló: ${response.code} - $errorBody")
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando notificación push de zona", e)
            }
        }
    }

    fun sendLocationRequestPing(
        context: Context,
        coupleId: String,
        senderId: String,
        senderName: String,
        partnerName: String,
        isSilent: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val safeCoupleId = normalizeCoupleId(coupleId)
        val isSenderAli = isAli(senderId, senderName)
        val targetDoc = if (isSenderAli) "kevin" else "ali"
        val now = System.currentTimeMillis()

        // 1. Dual Ping: Registro en Realtime Database para respuesta instantánea (<50ms) si la app de la pareja está abierta
        try {
            val pingMap = mapOf(
                "requestedAt" to now,
                "requestedBy" to senderName,
                "senderId" to senderId,
                "targetDoc" to targetDoc,
                "silent" to isSilent,
                "is_silent" to isSilent
            )
            val rtdb = getDatabase()
            rtdb.reference.child("locations").child(safeCoupleId)
                .child("pings").child(targetDoc)
                .setValue(pingMap)
                .addOnSuccessListener {
                    Log.d(TAG, "⚡ [DUAL PING] RTDB ping registrado para $targetDoc (isSilent=$isSilent)")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error registrando ping en RTDB: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error registrando ping RTDB", e)
        }

        // 2. Dual Ping: Magic Packet Wake-on-LAN vía FCM push prioritario (despierta el celular si la app está en segundo plano o cerrada)
        CoroutineScope(Dispatchers.IO).launch {
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "diario-ali-kevin"
            val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
            val topicName = "diario_" + safeCoupleId.lowercase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("ñ", "n").replace(" ", "_")

            val jsonBody = JSONObject().apply {
                val message = JSONObject().apply {
                    put("topic", topicName)
                    val data = JSONObject().apply {
                        put("authorId", senderId)
                        put("authorName", senderName)
                        put("targetDoc", targetDoc)
                        put("click_type", "radar_ping")
                        put("type", "radar_ping")
                        put("magic_packet", "WOL_LOCATION_WAKEUP")
                        put("timestamp", now.toString())
                        put("silent", if (isSilent) "true" else "false")
                        put("is_silent", if (isSilent) "true" else "false")
                        if (!isSilent) {
                            put("title", "📍 Actualización de Thor Radar")
                            put("body", "¡$senderName ha solicitado tu ubicación en vivo!")
                        }
                    }
                    put("data", data)
                    val android = JSONObject().apply {
                        put("priority", "HIGH")
                        put("ttl", "120s")
                    }
                    put("android", android)
                }
                put("message", message)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // Hasta 2 intentos: si el 1º devuelve 401 (token expirado), invalidar caché y reintentar con token fresco
            var lastSuccess = false
            for (attempt in 0..1) {
                try {
                    val creds = MainActivity.getGoogleCredentials(context)
                    val token = creds.accessToken?.tokenValue
                    if (token.isNullOrBlank()) {
                        Log.w(TAG, "⚡ [MAGIC PACKET] Token OAuth2 vacío en intento ${attempt + 1}, abortando FCM")
                        break
                    }
                    val reqBody = jsonBody.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url(url)
                        .post(reqBody)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                    val code = response.code
                    Log.d(TAG, "⚡ [MAGIC PACKET] Push FCM intento ${attempt + 1} → HTTP $code")
                    lastSuccess = response.isSuccessful
                    response.close()
                    if (code == 401 && attempt == 0) {
                        // Token rechazado: forzar invalidación del caché para que el próximo intento obtenga uno nuevo
                        MainActivity.invalidateGoogleCredentials()
                        Log.w(TAG, "⚡ [MAGIC PACKET] Token 401 — invalidando caché y reintentando con token fresco...")
                        continue
                    }
                    break // Éxito (2xx) o error no recuperable (4xx/5xx distinto de 401)
                } catch (e: Exception) {
                    Log.e(TAG, "⚡ [MAGIC PACKET] Error enviando ping FCM intento ${attempt + 1}", e)
                    lastSuccess = false
                    if (attempt < 1) {
                        try { kotlinx.coroutines.delay(500L) } catch (_: Exception) {}
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(lastSuccess)
            }
        }
    }

    /**
     * Manejador del Magic Packet tipo Wake-on-LAN al recibir un radar_ping de FCM o Firestore.
     * Adquiere un WakeLock, obtiene la ubicación fresca suspendiendo hasta el fix o timeout, y responde a Firestore en 1 sola escritura garantizada.
     */
    fun handleMagicLocationPing(context: Context) {
        val appContext = context.applicationContext
        Log.d(TAG, "⚡ [MAGIC PACKET] Activando Wake-on-LAN Location Wakeup...")

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Diario:MagicLocationWakeLock")
        try {
            wakeLock?.acquire(45_000L)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adquirir WakeLock: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                init(appContext)
                val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
                val isSharing = prefs.getBoolean("radar_is_sharing", true)

                if (isSharing && PermissionHelper.hasLocationPermission(appContext)) {
                    // Adquirir Fix GPS fresco de alta precisión suspendiendo hasta obtener resultado o agotar timeout
                    val freshLoc = requestHighAccuracyFixSuspend(appContext, timeoutMs = 15_000L)
                    val finalLoc = freshLoc ?: getLastKnownLocationFallback(appContext)
                    publishHeartbeat(appContext, finalLoc, force = true)
                    Log.d(TAG, "⚡ [MAGIC PACKET] Ubicación respondida a Firestore con éxito (lat=${finalLoc?.latitude}, lon=${finalLoc?.longitude})")
                } else {
                    publishHeartbeat(appContext, null, force = true)
                }
                // Breve pausa para asegurar el flush del paquete TCP de Firestore antes de dormir el CPU
                delay(2000L)
            } catch (e: Exception) {
                Log.e(TAG, "⚡ [MAGIC PACKET] Error procesando wakeup", e)
            } finally {
                try {
                    if (wakeLock != null && wakeLock.isHeld) {
                        wakeLock.release()
                        Log.d(TAG, "⚡ [MAGIC PACKET] WakeLock liberado con éxito")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error liberando WakeLock: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestHighAccuracyFixSuspend(
        context: Context,
        timeoutMs: Long = 15_000L
    ): Location? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val appContext = context.applicationContext
        if (!PermissionHelper.hasLocationPermission(appContext)) {
            if (continuation.isActive) continuation.resumeWith(Result.success(null))
            return@suspendCancellableCoroutine
        }

        init(appContext)
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        var fusedCallback: LocationCallback? = null
        var nativeListener: android.location.LocationListener? = null
        var bestCandidate: Location? = null

        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val rawUserId = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        val rawUserName = prefs.getString("userName", null)
        val docName = getMyDocName(rawUserId, rawUserName)

        val mainHandler = Handler(Looper.getMainLooper())
        var timeoutRunnable: Runnable? = null
        var earlyCheckRunnable: Runnable? = null

        fun cleanup() {
            try {
                fusedCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            } catch (e: Exception) {}
            try {
                nativeListener?.let { locationManager?.removeUpdates(it) }
            } catch (e: Exception) {}
            timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            earlyCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        }

        fun finish(loc: Location?) {
            if (completed.compareAndSet(false, true)) {
                cleanup()
                val chosenLoc = loc ?: bestCandidate ?: getLastKnownLocationFallback(appContext) ?: loadCachedLocationAsLocation(appContext, docName)
                if (continuation.isActive) {
                    continuation.resumeWith(Result.success(chosenLoc))
                }
            }
        }

        timeoutRunnable = Runnable {
            Log.d(TAG, "requestHighAccuracyFix: timeout de ${timeoutMs}ms alcanzado. Usando mejor candidato disponible.")
            finish(bestCandidate ?: getLastKnownLocationFallback(appContext) ?: loadCachedLocationAsLocation(appContext, docName))
        }
        mainHandler.postDelayed(timeoutRunnable!!, timeoutMs)

        continuation.invokeOnCancellation {
            cleanup()
        }

        fun updateCandidate(newLoc: Location?) {
            if (newLoc == null || (newLoc.latitude == 0.0 && newLoc.longitude == 0.0)) return
            val now = System.currentTimeMillis()
            val isFresh = (now - newLoc.time) < 180_000L
            val currentBest = bestCandidate
            if (currentBest == null || (newLoc.hasAccuracy() && newLoc.accuracy < currentBest.accuracy) || (isFresh && newLoc.time > currentBest.time && newLoc.accuracy <= (currentBest.accuracy + 20f))) {
                bestCandidate = newLoc
            }
            if (newLoc.hasAccuracy() && newLoc.accuracy <= 75f && isFresh) {
                finish(newLoc)
            }
        }

        // 0. Revisar lastLocation de FusedProvider de inmediato
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) {
                    updateCandidate(loc)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fusedLocationClient.lastLocation error: ${e.message}")
        }

        // 1. FusedLocation getCurrentLocation (High Accuracy y Balanced)
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                ?.addOnSuccessListener { loc ->
                    if (loc != null) {
                        updateCandidate(loc)
                    }
                }
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                ?.addOnSuccessListener { loc ->
                    if (loc != null) {
                        updateCandidate(loc)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLocation error: ${e.message}")
        }

        // 2. FusedLocation requestLocationUpdates (rápido de 3 muestras)
        try {
            val locRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
                .setMinUpdateIntervalMillis(1000L)
                .setMaxUpdates(3)
                .build()

            fusedCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation
                    if (loc != null) {
                        updateCandidate(loc)
                    }
                }
            }
            fusedLocationClient?.requestLocationUpdates(locRequest, fusedCallback!!, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.w(TAG, "requestLocationUpdates error: ${e.message}")
        }

        // 3. LocationManager Nativo (GPS y Red)
        try {
            nativeListener = object : android.location.LocationListener {
                override fun onLocationChanged(loc: Location) {
                    updateCandidate(loc)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    nativeListener!!,
                    Looper.getMainLooper()
                )
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    nativeListener!!,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "LocationManager nativo error: ${e.message}")
        }

        // Si tras 4 segundos ya tenemos algún candidato válido (ej: de red o lastLocation), terminar temprano
        earlyCheckRunnable = Runnable {
            if (!completed.get() && bestCandidate != null && (bestCandidate!!.accuracy <= 120f || bestCandidate!!.latitude != 0.0)) {
                Log.d(TAG, "requestHighAccuracyFix: candidato aceptable obtenido a los 4s, finalizando temprano.")
                finish(bestCandidate)
            }
        }
        mainHandler.postDelayed(earlyCheckRunnable!!, 4000L)
    }

    suspend fun searchPlaces(context: Context, query: String): List<RadarSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val results = mutableListOf<RadarSearchResult>()
        val seenCoords = mutableSetOf<String>()

        fun addResult(title: String, subtitle: String, lat: Double, lon: Double) {
            val key = "${String.format(Locale.US, "%.4f", lat)}_${String.format(Locale.US, "%.4f", lon)}"
            if (!seenCoords.contains(key)) {
                seenCoords.add(key)
                results.add(RadarSearchResult(title, subtitle, lat, lon))
            }
        }

        // 1. Intentar Geocoder nativo de Android con Locale de Chile
        try {
            val chileLocale = Locale("es", "CL")
            val geocoder = Geocoder(context, chileLocale)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(cleanQuery, 6)
            if (!addresses.isNullOrEmpty()) {
                for (addr in addresses) {
                    val street = listOfNotNull(addr.thoroughfare, addr.subThoroughfare).filter { it.isNotBlank() }.joinToString(" ")
                    val placeName = addr.featureName?.takeIf { it.isNotBlank() && it != addr.subThoroughfare && it != addr.thoroughfare }

                    val title = when {
                        placeName != null && street.isNotEmpty() -> "$placeName ($street)"
                        placeName != null -> placeName
                        street.isNotEmpty() -> street
                        !addr.locality.isNullOrBlank() -> addr.locality
                        else -> cleanQuery
                    }

                    val comuna = listOfNotNull(addr.subLocality, addr.locality, addr.subAdminArea).firstOrNull { it.isNotBlank() && it != title }
                    val region = addr.adminArea?.takeIf { it.isNotBlank() && it != comuna }
                    val country = addr.countryName?.takeIf { it.isNotBlank() } ?: "Chile"

                    val subtitleParts = listOfNotNull(comuna, region, country).distinct()
                    val subtitle = if (subtitleParts.isNotEmpty()) {
                        subtitleParts.joinToString(", ")
                    } else {
                        "(${String.format(Locale.US, "%.4f", addr.latitude)}, ${String.format(Locale.US, "%.4f", addr.longitude)})"
                    }

                    addResult(title, subtitle, addr.latitude, addr.longitude)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder search error: ${e.message}")
        }

        // 2. OpenStreetMap Nominatim con prioridad Chile (countrycodes=cl)
        if (results.size < 6) {
            try {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&countrycodes=cl&limit=8"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "DiarioAliKevin/1.0 (contact@diarioapp.local)")
                    .build()
                val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val lat = obj.getDouble("lat")
                            val lon = obj.getDouble("lon")
                            val name = obj.optString("name")
                            val addrDetails = obj.optJSONObject("address")

                            var road = ""
                            var houseNum = ""
                            var comuna = ""
                            var region = ""
                            var country = "Chile"

                            if (addrDetails != null) {
                                road = addrDetails.optString("road")
                                houseNum = addrDetails.optString("house_number")
                                comuna = addrDetails.optString("municipality").ifEmpty {
                                    addrDetails.optString("city_district").ifEmpty {
                                        addrDetails.optString("suburb").ifEmpty {
                                            addrDetails.optString("city").ifEmpty {
                                                addrDetails.optString("town").ifEmpty {
                                                    addrDetails.optString("county")
                                                }
                                            }
                                        }
                                    }
                                }
                                region = addrDetails.optString("state").ifEmpty {
                                    addrDetails.optString("region")
                                }
                                country = addrDetails.optString("country", "Chile")
                            }

                            val streetPart = listOf(road, houseNum).filter { it.isNotBlank() }.joinToString(" ")
                            val title = when {
                                name.isNotBlank() && streetPart.isNotBlank() && name != road -> "$name ($streetPart)"
                                name.isNotBlank() -> name
                                streetPart.isNotBlank() -> streetPart
                                else -> obj.getString("display_name").split(",").firstOrNull()?.trim() ?: cleanQuery
                            }

                            val subtitleParts = listOf(comuna, region, country).filter { it.isNotBlank() && it != title }.distinct()
                            val subtitle = if (subtitleParts.isNotEmpty()) {
                                subtitleParts.joinToString(", ")
                            } else {
                                val disp = obj.optString("display_name")
                                if (disp.contains(",")) disp.substringAfter(",").trim() else disp
                            }

                            addResult(title, subtitle, lat, lon)
                        }
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.w(TAG, "Nominatim search error: ${e.message}")
            }
        }

        // 3. Fallback a Nominatim global si aún no hay resultados
        if (results.isEmpty()) {
            try {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=6"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "DiarioAliKevin/1.0 (contact@diarioapp.local)")
                    .build()
                val response = DiarioApp.getOkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val lat = obj.getDouble("lat")
                            val lon = obj.getDouble("lon")
                            val disp = obj.getString("display_name")
                            val name = obj.optString("name").ifEmpty {
                                disp.split(",").firstOrNull()?.trim() ?: cleanQuery
                            }
                            val subtitle = if (disp.contains(",")) disp.substringAfter(",").trim() else disp
                            addResult(name, subtitle, lat, lon)
                        }
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.w(TAG, "Global nominatim error: ${e.message}")
            }
        }

        return@withContext results
    }

    suspend fun getReverseAddress(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        if (lat == 0.0 && lon == 0.0) return@withContext ""
        try {
            val geocoder = Geocoder(context, Locale("es", "CL"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val thoroughfare = addr.thoroughfare ?: ""
                val subThoroughfare = addr.subThoroughfare ?: ""
                val street = if (thoroughfare.isNotEmpty()) {
                    if (subThoroughfare.isNotEmpty()) "$thoroughfare $subThoroughfare" else thoroughfare
                } else ""
                val comuna = listOfNotNull(addr.subLocality, addr.locality, addr.subAdminArea).firstOrNull { it.isNotBlank() } ?: ""
                val region = addr.adminArea?.takeIf { it.isNotBlank() && it != comuna } ?: ""

                val parts = listOf(street, comuna, region).filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    return@withContext parts.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            // Ignorar y retornar coordenadas
        }
        return@withContext "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)}"
    }
}
