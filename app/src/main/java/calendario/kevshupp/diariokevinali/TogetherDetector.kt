package calendario.kevshupp.diariokevinali

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Comprueba si Kevin y Ali están juntos usando el Thor Radar (Realtime Database: 0 escrituras en Firestore).
 * 1. Publica una ubicación fresca propia.
 * 2. Envía un ping silencioso (Magic Packet FCM + RTDB) para que la pareja publique la suya.
 * 3. Espera la ubicación fresca de la pareja y compara la distancia.
 */
object TogetherDetector {
    private const val TAG = "TogetherDetector"

    // Distancia máxima para considerarse juntos (se suma la imprecisión del GPS, con tope)
    const val TOGETHER_RADIUS_M = 100f
    private const val MAX_ACCURACY_SLACK_M = 100f

    // Una ubicación de la pareja publicada hasta 1 min antes de la consulta todavía vale
    private const val PARTNER_FRESH_BEFORE_MS = 60_000L
    private const val PARTNER_TIMEOUT_MS = 30_000L
    private const val MY_FRESH_MS = 3 * 60_000L

    sealed class Result {
        data class Together(val distanceM: Int) : Result()
        data class Apart(val distanceM: Int) : Result()
        object NoPermission : Result()
        object NoMyLocation : Result()
        object PartnerNoResponse : Result()
    }

    suspend fun check(context: Context): Result {
        val appContext = context.applicationContext
        if (!PermissionHelper.hasLocationPermission(appContext)) return Result.NoPermission

        val prefs = appContext.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
        val userName = prefs.getString("userName", null)
        val coupleId = ThorRadarManager.normalizeCoupleId(prefs.getString("coupleId", "vínculo_único_123"))
        val myDoc = ThorRadarManager.getMyDocName(userId, userName)
        val partnerDoc = ThorRadarManager.getPartnerDocName(userId, userName)
        val start = System.currentTimeMillis()

        // 1. Mi ubicación fresca
        suspendCancellableCoroutine<Unit> { cont ->
            ThorRadarManager.forceLocationUpdate(appContext) { if (cont.isActive) cont.resume(Unit) }
        }

        // 2. Pedir la ubicación de la pareja (silencioso)
        ThorRadarManager.sendLocationRequestPing(
            context = appContext,
            coupleId = coupleId,
            senderId = userId,
            senderName = ThorRadarManager.getMyDisplayName(userId, userName),
            partnerName = ThorRadarManager.getPartnerDisplayName(userId, userName),
            isSilent = true
        )

        val usersRef = ThorRadarManager.getDatabase().reference.child("locations").child(coupleId).child("users")

        // 3. Esperar una ubicación reciente de la pareja
        val partner = withTimeoutOrNull(PARTNER_TIMEOUT_MS) {
            suspendCancellableCoroutine<RadarLocationData> { cont ->
                val ref = usersRef.child(partnerDoc)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val data = RadarLocationData.fromDataSnapshot(snapshot)
                        if (data.isSharing && data.latitude != 0.0 && data.timestamp >= start - PARTNER_FRESH_BEFORE_MS) {
                            ref.removeEventListener(this)
                            if (cont.isActive) cont.resume(data)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Error escuchando pareja: ${error.message}")
                    }
                }
                ref.addValueEventListener(listener)
                cont.invokeOnCancellation { ref.removeEventListener(listener) }
            }
        } ?: return Result.PartnerNoResponse

        // Mi ubicación publicada (la que acaba de subir forceLocationUpdate)
        val mine = suspendCancellableCoroutine<RadarLocationData?> { cont ->
            usersRef.child(myDoc).get()
                .addOnSuccessListener { if (cont.isActive) cont.resume(RadarLocationData.fromDataSnapshot(it)) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
        if (mine == null || mine.latitude == 0.0 || mine.timestamp < start - MY_FRESH_MS) return Result.NoMyLocation

        val distance = ThorRadarManager.calculateDistance(mine.latitude, mine.longitude, partner.latitude, partner.longitude)
        val threshold = TOGETHER_RADIUS_M + minOf(mine.accuracy + partner.accuracy, MAX_ACCURACY_SLACK_M)
        Log.d(TAG, "Distancia=${distance.toInt()} m, umbral=${threshold.toInt()} m")
        return if (distance <= threshold) Result.Together(distance.toInt()) else Result.Apart(distance.toInt())
    }
}
