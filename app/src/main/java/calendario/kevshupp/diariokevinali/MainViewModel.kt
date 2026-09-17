package calendario.kevshupp.diariokevinali

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val app = getApplication<Application>()
    private val prefs = app.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)

    // Datos de sesión leídos de SharedPreferences
    val currentCoupleId: String = prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123"
    val currentUserId: String = prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01"
    val currentUserName: String = prefs.getString("userName", "Kevin") ?: "Kevin"
    var currentUserImageUri: String? = prefs.getString("userImage", null)
        private set

    // SimpleDateFormat para uso interno
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Estados observables desde Compose/Java
    private val _messagesState = MutableLiveData<List<Message>>(emptyList())
    val messagesState: LiveData<List<Message>> = _messagesState

    private val _petState = MutableLiveData<Pet>()
    val petState: LiveData<Pet> = _petState

    private val _themeState = MutableLiveData(prefs.getString("theme", "Pixel Claro") ?: "Pixel Claro")
    val themeState: LiveData<String> = _themeState

    private val _refreshRateState = MutableLiveData(prefs.getInt("refreshRate", 90))
    val refreshRateState: LiveData<Int> = _refreshRateState

    val showEditorState = MutableLiveData(false)
    val editingMessageState = MutableLiveData<Message?>()
    val currentSelectedImageUrlState = MutableLiveData<String?>()
    val isUploadingState = MutableLiveData(false)
    val overlayMessageState = MutableLiveData("Cargando...")

    // Eventos de un solo uso (SingleLiveEvent alternativo)
    val toastMessage = MutableLiveData<String?>()
    val levelUpEvent = MutableLiveData<Pair<String, Int>?>()

    // Registros de listeners de Firestore
    private var firestoreListener: ListenerRegistration? = null
    private var petListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var calendarListener: ListenerRegistration? = null

    // Filtro de fecha para mensajes
    private var selectedFilterDate: Calendar? = null

    init {
        // Controlled via Activity lifecycle methods
    }

    fun startAllListeners() {
        listenUserInfo()
        listenPet()
        listenMessagesFromFirestore()
        listenCalendar()
        MedicationAlarmScheduler.rescheduleAllAlarmsFromFirestore(app, currentCoupleId)
    }

    fun stopAllListeners() {
        userListener?.remove()
        userListener = null
        petListener?.remove()
        petListener = null
        firestoreListener?.remove()
        firestoreListener = null
        calendarListener?.remove()
        calendarListener = null
    }

    fun startActiveListeners() {
        listenMessagesFromFirestore()
        listenCalendar()
    }

    fun stopActiveListeners() {
        firestoreListener?.remove()
        firestoreListener = null
        calendarListener?.remove()
        calendarListener = null
    }

    fun setSelectedFilterDate(date: Calendar?) {
        selectedFilterDate = date
        listenMessagesFromFirestore()
    }

    fun getSelectedFilterDate(): Calendar? = selectedFilterDate

    // --- LISTENERS DE FIRESTORE ---

    private fun listenUserInfo() {
        userListener?.remove()
        userListener = db.collection("users").document(currentUserId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MainViewModel", "Error en listener de usuario", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val url = snapshot.getString("profileImageUrl")
                if (url != null && url != currentUserImageUri) {
                    currentUserImageUri = url
                    prefs.edit().putString("userImage", url).apply()
                }

                val themeVal = snapshot.getString("theme")
                val useCustomBgVal = snapshot.getBoolean("useCustomBg")
                val lightColorVal = snapshot.getString("lightColor")
                val darkColorVal = snapshot.getString("darkColor")
                val cacheSizeLimitVal = snapshot.getLong("cacheSizeLimit")
                val updateIntervalVal = snapshot.getLong("updateInterval")
                val appointmentLeadTimeVal = snapshot.getLong("appointmentLeadTime")
                val refreshRateVal = snapshot.getLong("refreshRate")?.toInt()

                val editor = prefs.edit()
                var changed = false

                if (themeVal != null && themeVal != prefs.getString("theme", "")) {
                    editor.putString("theme", themeVal)
                    _themeState.value = themeVal
                    changed = true
                }
                if (refreshRateVal != null && refreshRateVal != prefs.getInt("refreshRate", 90)) {
                    editor.putInt("refreshRate", refreshRateVal)
                    _refreshRateState.value = refreshRateVal
                    changed = true
                }
                if (useCustomBgVal != null && useCustomBgVal != prefs.getBoolean("useCustomBg", false)) {
                    editor.putBoolean("useCustomBg", useCustomBgVal)
                    changed = true
                }
                if (lightColorVal != null && lightColorVal != prefs.getString("lightColor", "")) {
                    editor.putString("lightColor", lightColorVal)
                    changed = true
                }
                if (darkColorVal != null && darkColorVal != prefs.getString("darkColor", "")) {
                    editor.putString("darkColor", darkColorVal)
                    changed = true
                }
                if (cacheSizeLimitVal != null && cacheSizeLimitVal != prefs.getLong("cacheSizeLimit", 100L)) {
                    editor.putLong("cacheSizeLimit", cacheSizeLimitVal)
                    changed = true
                }
                if (updateIntervalVal != null && updateIntervalVal != prefs.getLong("updateInterval", 720L)) {
                    editor.putLong("updateInterval", updateIntervalVal)
                    changed = true
                }
                if (appointmentLeadTimeVal != null && appointmentLeadTimeVal != prefs.getLong("appointmentLeadTime", 60L)) {
                    editor.putLong("appointmentLeadTime", appointmentLeadTimeVal)
                    changed = true
                }

                if (changed) {
                    editor.apply()
                }
            }
        }
    }

    private fun listenPet() {
        petListener?.remove()
        petListener = db.collection("pets").document(currentCoupleId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MainViewModel", "Error en listener de mascota", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val p = snapshot.toObject(Pet::class.java)
                if (p != null) {
                    _petState.value = p
                    checkPetDecay(p)
                    savePetDataToWidgetPrefs(p)
                    checkAndMigratePetRanking(p)
                }
            } else {
                val initialPet = Pet()
                db.collection("pets").document(currentCoupleId).set(initialPet)
            }
        }
    }

    private var hasMigratedRankingThisSession = false
    private fun checkAndMigratePetRanking(p: Pet) {
        if (hasMigratedRankingThisSession) return

        val updates = mutableMapOf<String, Any>()

        // Kevin: Si existían puntos o contadores globales legacy que no estaban en Thor ni Cuky
        val kevinThorDiff = p.carePointsKevin - (p.carePointsKevinThor + p.carePointsKevinCuky)
        if (kevinThorDiff > 0) updates["carePointsKevinThor"] = p.carePointsKevinThor + kevinThorDiff

        val kevinFeedDiff = p.feedCountKevin - (p.feedCountKevinThor + p.feedCountKevinCuky)
        if (kevinFeedDiff > 0) updates["feedCountKevinThor"] = p.feedCountKevinThor + kevinFeedDiff

        val kevinBathDiff = p.bathCountKevin - (p.bathCountKevinThor + p.bathCountKevinCuky)
        if (kevinBathDiff > 0) updates["bathCountKevinThor"] = p.bathCountKevinThor + kevinBathDiff

        val kevinPlayDiff = p.playCountKevin - (p.playCountKevinThor + p.playCountKevinCuky)
        if (kevinPlayDiff > 0) updates["playCountKevinThor"] = p.playCountKevinThor + kevinPlayDiff

        val kevinTapDiff = p.tapCountKevin - (p.tapCountKevinThor + p.tapCountKevinCuky)
        if (kevinTapDiff > 0) updates["tapCountKevinThor"] = p.tapCountKevinThor + kevinTapDiff

        val kevinMiniDiff = p.minigameCountKevin - (p.minigameCountKevinThor + p.minigameCountKevinCuky)
        if (kevinMiniDiff > 0) updates["minigameCountKevinThor"] = p.minigameCountKevinThor + kevinMiniDiff

        // Ali: Si existían puntos o contadores globales legacy que no estaban en Thor ni Cuky
        val aliThorDiff = p.carePointsAli - (p.carePointsAliThor + p.carePointsAliCuky)
        if (aliThorDiff > 0) updates["carePointsAliThor"] = p.carePointsAliThor + aliThorDiff

        val aliFeedDiff = p.feedCountAli - (p.feedCountAliThor + p.feedCountAliCuky)
        if (aliFeedDiff > 0) updates["feedCountAliThor"] = p.feedCountAliThor + aliFeedDiff

        val aliBathDiff = p.bathCountAli - (p.bathCountAliThor + p.bathCountAliCuky)
        if (aliBathDiff > 0) updates["bathCountAliThor"] = p.bathCountAliThor + aliBathDiff

        val aliPlayDiff = p.playCountAli - (p.playCountAliThor + p.playCountAliCuky)
        if (aliPlayDiff > 0) updates["playCountAliThor"] = p.playCountAliThor + aliPlayDiff

        val aliTapDiff = p.tapCountAli - (p.tapCountAliThor + p.tapCountAliCuky)
        if (aliTapDiff > 0) updates["tapCountAliThor"] = p.tapCountAliThor + aliTapDiff

        val aliMiniDiff = p.minigameCountAli - (p.minigameCountAliThor + p.minigameCountAliCuky)
        if (aliMiniDiff > 0) updates["minigameCountAliThor"] = p.minigameCountAliThor + aliMiniDiff

        if (updates.isNotEmpty()) {
            hasMigratedRankingThisSession = true
            db.collection("pets").document(currentCoupleId).update(updates)
                .addOnSuccessListener {
                    Log.d("MainViewModel", "Ranking de cuidadores normalizado y sincronizado en BD Firestore exitosamente")
                }
        } else {
            hasMigratedRankingThisSession = true
        }
    }

    private fun listenMessagesFromFirestore() {
        firestoreListener?.remove()
        var query: Query = db.collection("messages")
            .whereEqualTo("partnerId", currentCoupleId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val filterDate = selectedFilterDate
        if (filterDate == null) {
            query = query.limit(100)
        } else {
            val s = filterDate.clone() as Calendar
            s.set(Calendar.HOUR_OF_DAY, 0)
            s.set(Calendar.MINUTE, 0)
            s.set(Calendar.SECOND, 0)

            val end = filterDate.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)

            query = query.whereGreaterThanOrEqualTo("timestamp", s.timeInMillis)
                .whereLessThanOrEqualTo("timestamp", end.timeInMillis)
        }

        firestoreListener = query.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e("MainViewModel", "Error en el listener de mensajes", error)
                return@addSnapshotListener
            }
            if (value != null) {
                val newMessages = ArrayList<Message>()
                for (doc in value) {
                    val m = doc.toObject(Message::class.java)
                    m.messageId = doc.id
                    android.util.Log.d("DIARIO_DEBUG", "MainViewModel listener: cargado msgId: ${m.messageId}, liked: ${m.liked}, isLiked: ${m.isLiked}")
                    val content = m.content
                    if (content == null || !content.startsWith("[ALBUM]")) {
                        newMessages.add(m)
                    }
                }
                _messagesState.value = newMessages
                updateWidget()
            }
        }
    }

    private fun listenCalendar() {
        calendarListener?.remove()
        calendarListener = db.collection("calendar")
            .whereEqualTo("partnerId", currentCoupleId)
            .addSnapshotListener { snaps, e ->
                if (e != null) {
                    Log.e("MainViewModel", "Error en listener del calendario", e)
                    return@addSnapshotListener
                }
                if (snaps != null) {
                    for (doc in snaps) {
                        val ev = doc.toObject(CalendarEvent::class.java)
                        if (ev.eventId.isEmpty()) ev.eventId = doc.id
                        scheduleCalendarReminder(ev)
                    }
                }
            }
    }

    // --- LÓGICA DE MASCOTA (THOR) ---

    class DecayedStats(
        val hunger: Int,
        val cleanliness: Int,
        val sleepPercent: Int,
        val nextDecayUpdate: Long,
        val isSleeping: Boolean
    )

    private fun calculateDecay(p: Pet, now: Long, forCuky: Boolean = p.isCuky()): DecayedStats {
        val lastDecay = if (forCuky) (if (p.cukyLastDecayUpdate != 0L) p.cukyLastDecayUpdate else now) else (if (p.lastDecayUpdate != 0L) p.lastDecayUpdate else now)
        var decayDiff = now - lastDecay
        if (decayDiff < 0) decayDiff = 0
        val hoursToDecay = decayDiff / (1000 * 60 * 60)

        val baseHunger = if (forCuky) p.cukyHunger else p.hunger
        val baseCleanliness = if (forCuky) p.cukyCleanliness else p.cleanliness
        val baseSleepPercent = if (forCuky) p.cukySleepPercent else p.sleepPercent
        val baseIsSleeping = if (forCuky) p.cukyIsSleeping else p.isSleeping

        var decayedHunger = baseHunger
        var decayedCleanliness = baseCleanliness
        var decayedSleepPercent = baseSleepPercent
        var nextDecayUpdate = lastDecay

        if (hoursToDecay >= 1) {
            decayedHunger = Math.min(100, baseHunger + (hoursToDecay * 4).toInt())
            decayedCleanliness = Math.max(0, baseCleanliness - (hoursToDecay * 3).toInt())

            val calendar = java.util.Calendar.getInstance()
            for (h in 1..hoursToDecay) {
                calendar.timeInMillis = lastDecay + h * 3600000L
                val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                if (hourOfDay in 0..7) {
                    decayedSleepPercent = Math.min(100, decayedSleepPercent + 15)
                } else {
                    if (baseIsSleeping) {
                        decayedSleepPercent = Math.min(100, decayedSleepPercent + 15)
                    } else {
                        decayedSleepPercent = Math.max(0, decayedSleepPercent - 5)
                    }
                }
            }
            nextDecayUpdate += hoursToDecay * 3600000L
        }

        val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = calendarNow.get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7

        var isNowSleeping = isNightTime || baseIsSleeping
        var finalSleepPercent = decayedSleepPercent

        if (!isNightTime && baseIsSleeping && decayedSleepPercent >= 100) {
            isNowSleeping = false
            finalSleepPercent = 100
        }

        return DecayedStats(decayedHunger, decayedCleanliness, finalSleepPercent, nextDecayUpdate, isNowSleeping)
    }

    private fun checkPetDecay(p: Pet) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>()

        // Thor decay
        val thorStats = calculateDecay(p, now, forCuky = false)
        val thorHappinessDiff = now - p.lastInteraction
        val thorDaysToDecay = thorHappinessDiff / (1000L * 60L * 60L * 24L)
        var newThorHappiness = p.happiness
        var thorInteractionCompensated = p.lastInteraction

        if (thorDaysToDecay >= 1) {
            val decay = (thorDaysToDecay * 20).toInt()
            newThorHappiness = Math.max(0, p.happiness - decay)
            thorInteractionCompensated += thorDaysToDecay * 24L * 60L * 60L * 1000L
        }
        var newThorStatus = p.status
        if (thorStats.isSleeping) {
            newThorStatus = Pet.STATUS_SLEEPING
        } else if (thorStats.hunger >= 70) {
            newThorStatus = Pet.STATUS_HUNGRY
        } else {
            newThorStatus = if (newThorHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
        }

        val today = dayFormat.format(Date(now))
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dayFormat.format(yesterday.time)

        val isThorStreakAlive = p.lastInteractionDate != null && 
                (p.lastInteractionDate == today || p.lastInteractionDate == yesterdayStr)
        var newThorStreak = p.streakDays
        if (!isThorStreakAlive && p.streakDays > 0) newThorStreak = 0

        if (newThorHappiness != p.happiness || thorStats.hunger != p.hunger || thorStats.cleanliness != p.cleanliness ||
            thorStats.sleepPercent != p.sleepPercent || newThorStatus != p.status || thorStats.nextDecayUpdate != p.lastDecayUpdate ||
            thorStats.isSleeping != p.isSleeping || newThorStreak != p.streakDays) {
            updates["happiness"] = newThorHappiness
            updates["hunger"] = thorStats.hunger
            updates["cleanliness"] = thorStats.cleanliness
            updates["sleepPercent"] = thorStats.sleepPercent
            updates["status"] = newThorStatus
            updates["lastInteraction"] = thorInteractionCompensated
            updates["lastDecayUpdate"] = thorStats.nextDecayUpdate
            updates["isSleeping"] = thorStats.isSleeping
            updates["streakDays"] = newThorStreak
        }

        // Cuky decay
        val cukyStats = calculateDecay(p, now, forCuky = true)
        val cukyHappinessDiff = now - p.cukyLastInteraction
        val cukyDaysToDecay = cukyHappinessDiff / (1000L * 60L * 60L * 24L)
        var newCukyHappiness = p.cukyHappiness
        var cukyInteractionCompensated = p.cukyLastInteraction

        if (cukyDaysToDecay >= 1) {
            val decay = (cukyDaysToDecay * 20).toInt()
            newCukyHappiness = Math.max(0, p.cukyHappiness - decay)
            cukyInteractionCompensated += cukyDaysToDecay * 24L * 60L * 60L * 1000L
        }
        var newCukyStatus = p.cukyStatus
        if (cukyStats.isSleeping) {
            newCukyStatus = Pet.STATUS_SLEEPING
        } else if (cukyStats.hunger >= 70) {
            newCukyStatus = Pet.STATUS_HUNGRY
        } else {
            newCukyStatus = if (newCukyHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
        }

        val isCukyStreakAlive = p.cukyLastInteractionDate != null && 
                (p.cukyLastInteractionDate == today || p.cukyLastInteractionDate == yesterdayStr)
        var newCukyStreak = p.cukyStreakDays
        if (!isCukyStreakAlive && p.cukyStreakDays > 0) newCukyStreak = 0

        if (newCukyHappiness != p.cukyHappiness || cukyStats.hunger != p.cukyHunger || cukyStats.cleanliness != p.cukyCleanliness ||
            cukyStats.sleepPercent != p.cukySleepPercent || newCukyStatus != p.cukyStatus || cukyStats.nextDecayUpdate != p.cukyLastDecayUpdate ||
            cukyStats.isSleeping != p.cukyIsSleeping || newCukyStreak != p.cukyStreakDays) {
            updates["cukyHappiness"] = newCukyHappiness
            updates["cukyHunger"] = cukyStats.hunger
            updates["cukyCleanliness"] = cukyStats.cleanliness
            updates["cukySleepPercent"] = cukyStats.sleepPercent
            updates["cukyStatus"] = newCukyStatus
            updates["cukyLastInteraction"] = cukyInteractionCompensated
            updates["cukyLastDecayUpdate"] = cukyStats.nextDecayUpdate
            updates["cukyIsSleeping"] = cukyStats.isSleeping
            updates["cukyStreakDays"] = newCukyStreak
        }

        if (updates.isNotEmpty()) {
            db.collection("pets").document(currentCoupleId).update(updates)
        }
    }

    private fun savePetDataToWidgetPrefs(p: Pet) {
        val wPrefs = app.getSharedPreferences("thor_widget_prefs", Context.MODE_PRIVATE)
        wPrefs.edit()
            .putString("pet_name", p.name)
            .putString("pet_type", p.petType)
            .putInt("pet_level", p.level)
            .putInt("pet_happiness", p.happiness)
            .putString("pet_status", p.status)
            .putString("pet_accessory", p.getActiveEquippedAccessory() ?: "none")
            .putBoolean("pet_sleeping", p.isSleeping)
            .putInt("pet_hunger", p.hunger)
            .putInt("pet_cleanliness", p.cleanliness)
            .apply()
        ThorWidgetProvider.triggerUpdate(app)
    }

    fun isCurrentUserKevin(): Boolean {
        val uid = currentUserId.lowercase()
        val uname = currentUserName.lowercase()
        return uid.contains("kevin") || uname.contains("kevin")
    }

    fun updatePetOnInteraction() {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dayFormat.format(yesterday.time)

        val stats = calculateDecay(p, now, isCuky)
        val currentCleanliness = stats.cleanliness
        val currentSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate

        val currentHappiness = p.getActiveHappiness()
        val currentExp = p.getActiveExperience()
        val currentLevel = p.getActiveLevel()
        val currentStreak = p.getActiveStreak()
        val currentLastInteractionDate = p.getActiveLastInteractionDate()

        val newHappiness = Math.min(100, currentHappiness + 10)
        var newLovePoints = p.lovePoints + 5
        var newExp = currentExp + 5
        var newLevel = currentLevel
        var newStreak = currentStreak

        if (currentLastInteractionDate == today) {
            newStreak = Math.max(1, currentStreak)
        } else if (currentLastInteractionDate == yesterdayStr) {
            newStreak = Math.max(0, currentStreak) + 1
            newLovePoints += (newStreak * 2)
        } else {
            newStreak = 1
        }

        var leveledUp = false
        while (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }

        val isKevin = isCurrentUserKevin()
        val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
        val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"

        val updates = mutableMapOf<String, Any>(
            "lovePoints" to newLovePoints,
            carePtsPetKey to FieldValue.increment(5),
            carePtsTotalKey to FieldValue.increment(5)
        )

        if (isCuky) {
            updates["cukyHappiness"] = newHappiness
            updates["cukyExperience"] = newExp
            updates["cukyLevel"] = newLevel
            updates["cukyStreakDays"] = newStreak
            updates["cukyLastInteractionDate"] = today
            updates["cukyLastInteraction"] = now
            updates["cukyLastDecayUpdate"] = nextDecayUpdate
            updates["cukyHunger"] = 0
            updates["cukyCleanliness"] = currentCleanliness
            updates["cukySleepPercent"] = currentSleepPercent
            updates["cukyStatus"] = if (stats.isSleeping) Pet.STATUS_SLEEPING else Pet.STATUS_HAPPY
            updates["cukyIsSleeping"] = stats.isSleeping
        } else {
            updates["happiness"] = newHappiness
            updates["experience"] = newExp
            updates["level"] = newLevel
            updates["streakDays"] = newStreak
            updates["lastInteractionDate"] = today
            updates["lastInteraction"] = now
            updates["lastDecayUpdate"] = nextDecayUpdate
            updates["hunger"] = 0
            updates["cleanliness"] = currentCleanliness
            updates["sleepPercent"] = currentSleepPercent
            updates["status"] = if (stats.isSleeping) Pet.STATUS_SLEEPING else Pet.STATUS_HAPPY
            updates["isSleeping"] = stats.isSleeping
        }

        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                if (leveledUp) {
                    levelUpEvent.value = Pair(p.getActiveName(), newLevel)
                }
            }
    }

    fun feedPet(foodId: String, cost: Int, happinessGain: Int) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.getActiveIsSleeping() || isNightTime) {
            toastMessage.value = "💤 ¡${p.getActiveName()} está durmiendo!"
            return
        }
        if (p.lovePoints >= cost) {
            val now = System.currentTimeMillis()
            val stats = calculateDecay(p, now, isCuky)
            val decayedCleanliness = stats.cleanliness
            val decayedSleepPercent = stats.sleepPercent
            val nextDecayUpdate = stats.nextDecayUpdate
            val isNowSleeping = stats.isSleeping

            val currentHappiness = p.getActiveHappiness()
            val newHappiness = Math.min(100, currentHappiness + happinessGain)
            var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.getActiveStatus()
            if (!isNowSleeping) {
                if (newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
                    newStatus = Pet.STATUS_HAPPY
                }
            }

            val isKevin = isCurrentUserKevin()
            val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
            val feedCountPetKey = if (isKevin) (if (isCuky) "feedCountKevinCuky" else "feedCountKevinThor") else (if (isCuky) "feedCountAliCuky" else "feedCountAliThor")
            val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"
            val feedCountTotalKey = if (isKevin) "feedCountKevin" else "feedCountAli"

            val updates = mutableMapOf<String, Any>(
                "lovePoints" to (p.lovePoints - cost),
                carePtsPetKey to FieldValue.increment(10),
                feedCountPetKey to FieldValue.increment(1),
                carePtsTotalKey to FieldValue.increment(10),
                feedCountTotalKey to FieldValue.increment(1)
            )

            if (isCuky) {
                updates["cukyHappiness"] = newHappiness
                updates["cukyStatus"] = newStatus
                updates["cukyHunger"] = 0
                updates["cukyCleanliness"] = decayedCleanliness
                updates["cukySleepPercent"] = decayedSleepPercent
                updates["cukyLastInteraction"] = now
                updates["cukyLastDecayUpdate"] = nextDecayUpdate
                updates["cukyIsSleeping"] = isNowSleeping
            } else {
                updates["happiness"] = newHappiness
                updates["status"] = newStatus
                updates["hunger"] = 0
                updates["cleanliness"] = decayedCleanliness
                updates["sleepPercent"] = decayedSleepPercent
                updates["lastInteraction"] = now
                updates["lastDecayUpdate"] = nextDecayUpdate
                updates["isSleeping"] = isNowSleeping
            }

            db.collection("pets").document(currentCoupleId)
                .update(updates)
                .addOnSuccessListener {
                    toastMessage.value = "¡Le has dado de comer a ${p.getActiveName()}! 💖 +$happinessGain% Felicidad"
                }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun rewardPet(points: Int, exp: Int) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.getActiveIsSleeping() || isNightTime) {
            toastMessage.value = "💤 ¡${p.getActiveName()} está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        val currentDailyTaps = if (today == p.getActiveLastTapDate()) p.getActiveDailyTapCount() else 0

        val maxDailyTaps = 30
        val allowedTaps = Math.max(0, maxDailyTaps - currentDailyTaps)

        if (allowedTaps <= 0) {
            toastMessage.value = "¡${p.getActiveName()} ya recibió suficiente cariño por hoy! 💖 (Límite: $maxDailyTaps/día)"
            return
        }

        val actualTapsAdded = Math.min(points, allowedTaps)

        val stats = calculateDecay(p, now, isCuky)
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        var newExp = p.getActiveExperience() + exp
        var newLevel = p.getActiveLevel()
        var newLovePoints = p.lovePoints + actualTapsAdded
        val newHappiness = Math.min(100, p.getActiveHappiness() + actualTapsAdded)

        var newStatus = p.getActiveStatus()
        if (isNowSleeping) {
            newStatus = Pet.STATUS_SLEEPING
        } else if (stats.hunger >= 70) {
            newStatus = Pet.STATUS_HUNGRY
        } else {
            newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
        }

        var leveledUp = false
        while (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }

        val totalTapsToday = currentDailyTaps + actualTapsAdded
        val finalLevel = newLevel
        val showLevelUpToast = leveledUp

        val isKevin = isCurrentUserKevin()
        val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
        val tapCountPetKey = if (isKevin) (if (isCuky) "tapCountKevinCuky" else "tapCountKevinThor") else (if (isCuky) "tapCountAliCuky" else "tapCountAliThor")
        val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"
        val tapCountTotalKey = if (isKevin) "tapCountKevin" else "tapCountAli"

        val updates = mutableMapOf<String, Any>(
            "lovePoints" to newLovePoints,
            carePtsPetKey to FieldValue.increment(actualTapsAdded.toLong()),
            tapCountPetKey to FieldValue.increment(actualTapsAdded.toLong()),
            carePtsTotalKey to FieldValue.increment(actualTapsAdded.toLong()),
            tapCountTotalKey to FieldValue.increment(actualTapsAdded.toLong())
        )

        if (isCuky) {
            updates["cukyExperience"] = newExp
            updates["cukyLevel"] = newLevel
            updates["cukyHappiness"] = newHappiness
            updates["cukyStatus"] = newStatus
            updates["cukyHunger"] = 0
            updates["cukyCleanliness"] = decayedCleanliness
            updates["cukySleepPercent"] = decayedSleepPercent
            updates["cukyLastInteraction"] = now
            updates["cukyLastDecayUpdate"] = nextDecayUpdate
            updates["cukyIsSleeping"] = isNowSleeping
            updates["cukyDailyTapCount"] = totalTapsToday
            updates["cukyLastTapDate"] = today
        } else {
            updates["experience"] = newExp
            updates["level"] = newLevel
            updates["happiness"] = newHappiness
            updates["status"] = newStatus
            updates["hunger"] = 0
            updates["cleanliness"] = decayedCleanliness
            updates["sleepPercent"] = decayedSleepPercent
            updates["lastInteraction"] = now
            updates["lastDecayUpdate"] = nextDecayUpdate
            updates["isSleeping"] = isNowSleeping
            updates["dailyTapCount"] = totalTapsToday
            updates["lastTapDate"] = today
        }

        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                if (totalTapsToday >= maxDailyTaps) {
                    toastMessage.value = "¡${p.getActiveName()} se siente amado/a! ❤️ +$actualTapsAdded Amor (¡Límite alcanzado! 🎉)"
                } else {
                    toastMessage.value = "¡${p.getActiveName()} se siente amado/a! ❤️ +$actualTapsAdded Amor ($totalTapsToday/$maxDailyTaps)"
                }
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.getActiveName(), finalLevel)
                }
            }
            .addOnFailureListener { err ->
                Log.e("MainViewModel", "Error al actualizar recompensa: ${err.message}")
            }
    }

    fun isDoNotDisturbActive(): Boolean {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (nm != null) {
            val filter = nm.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
    }

    fun togglePetSleep() {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (isNightTime) {
            toastMessage.value = "💤 ${p.getActiveName()} tiene que dormir durante la noche (00:00 - 08:00)."
            return
        }

        val dndActive = isDoNotDisturbActive()
        val targetSleepState = !p.getActiveIsSleeping()

        if (!targetSleepState && dndActive) {
            toastMessage.value = "No puedes despertar a ${p.getActiveName()} mientras el modo No Molestar esté activo en tu celular. 📵"
            return
        }

        val now = System.currentTimeMillis()
        val stats = calculateDecay(p, now, isCuky)
        val decayedHunger = stats.hunger
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate

        var newHappiness = p.getActiveHappiness()
        var newStatus = p.getActiveStatus()

        val updates = mutableMapOf<String, Any?>(
            "dndTriggeredByUserId" to null
        )

        if (targetSleepState) {
            if (decayedSleepPercent >= 100) {
                toastMessage.value = "¡${p.getActiveName()} ya está completamente descansado/a! ☀️ No necesita dormir."
                return
            }
            newStatus = Pet.STATUS_SLEEPING
            if (isCuky) {
                updates["cukyIsSleeping"] = true
                updates["cukyStatus"] = newStatus
                updates["cukyHunger"] = decayedHunger
                updates["cukyCleanliness"] = decayedCleanliness
                updates["cukySleepPercent"] = decayedSleepPercent
                updates["cukyLastInteraction"] = now
                updates["cukyLastDecayUpdate"] = nextDecayUpdate
            } else {
                updates["isSleeping"] = true
                updates["status"] = newStatus
                updates["hunger"] = decayedHunger
                updates["cleanliness"] = decayedCleanliness
                updates["sleepPercent"] = decayedSleepPercent
                updates["lastInteraction"] = now
                updates["lastDecayUpdate"] = nextDecayUpdate
            }

            db.collection("pets").document(currentCoupleId)
                .update(updates)
                .addOnSuccessListener {
                    toastMessage.value = "¡${p.getActiveName()} se ha ido a dormir! 🌙 Shhh..."
                }
        } else {
            newHappiness = Math.min(100, newHappiness + 20)
            newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
            if (isCuky) {
                updates["cukyIsSleeping"] = false
                updates["cukyStatus"] = newStatus
                updates["cukyHappiness"] = newHappiness
                updates["cukyHunger"] = decayedHunger
                updates["cukyCleanliness"] = decayedCleanliness
                updates["cukySleepPercent"] = decayedSleepPercent
                updates["cukyLastInteraction"] = now
                updates["cukyLastDecayUpdate"] = nextDecayUpdate
            } else {
                updates["isSleeping"] = false
                updates["status"] = newStatus
                updates["happiness"] = newHappiness
                updates["hunger"] = decayedHunger
                updates["cleanliness"] = decayedCleanliness
                updates["sleepPercent"] = decayedSleepPercent
                updates["lastInteraction"] = now
                updates["lastDecayUpdate"] = nextDecayUpdate
            }

            db.collection("pets").document(currentCoupleId)
                .update(updates)
                .addOnSuccessListener {
                    toastMessage.value = "¡${p.getActiveName()} ha despertado muy alegre! ☀️ +20% Felicidad"
                }
        }
    }

    fun syncDndStateWithPet() {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val dndActive = isDoNotDisturbActive()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7

        val shouldSleep = dndActive || isNightTime

        if (shouldSleep) {
            if (!p.getActiveIsSleeping()) {
                val updates = mutableMapOf<String, Any?>(
                    "dndTriggeredByUserId" to if (dndActive) currentUserId else null
                )
                if (isCuky) {
                    updates["cukyIsSleeping"] = true
                    updates["cukyStatus"] = Pet.STATUS_SLEEPING
                } else {
                    updates["isSleeping"] = true
                    updates["status"] = Pet.STATUS_SLEEPING
                }
                db.collection("pets").document(currentCoupleId)
                    .update(updates)
                    .addOnSuccessListener {
                        if (dndActive) {
                            toastMessage.value = "${p.getActiveName()} se durmió porque activaste No Molestar 🌙"
                        }
                    }
            }
        } else {
            if (p.getActiveIsSleeping() && (p.dndTriggeredByUserId == null || currentUserId == p.dndTriggeredByUserId)) {
                val newHappiness = Math.min(100, p.getActiveHappiness() + 20)
                val newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
                val updates = mutableMapOf<String, Any?>(
                    "dndTriggeredByUserId" to null
                )
                if (isCuky) {
                    updates["cukyIsSleeping"] = false
                    updates["cukyStatus"] = newStatus
                    updates["cukyHappiness"] = newHappiness
                    updates["cukyLastInteraction"] = System.currentTimeMillis()
                } else {
                    updates["isSleeping"] = false
                    updates["status"] = newStatus
                    updates["happiness"] = newHappiness
                    updates["lastInteraction"] = System.currentTimeMillis()
                }
                db.collection("pets").document(currentCoupleId)
                    .update(updates)
                    .addOnSuccessListener {
                        toastMessage.value = "¡${p.getActiveName()} despertó! ☀️"
                    }
            }
        }
    }

    fun bathPet() {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.getActiveIsSleeping() || isNightTime) {
            toastMessage.value = "💤 ¡${p.getActiveName()} está durmiendo!"
            return
        }
        val currentCleanliness = p.getActiveCleanliness()
        if (currentCleanliness >= 80) {
            toastMessage.value = "¡${p.getActiveName()} todavía está limpio/a! 🫧 (Limpieza: $currentCleanliness%)"
            return
        }

        val now = System.currentTimeMillis()
        val stats = calculateDecay(p, now, isCuky)
        val decayedHunger = stats.hunger
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        val newHappiness = Math.min(100, p.getActiveHappiness() + 10)
        var newExp = p.getActiveExperience() + 3
        var newLevel = p.getActiveLevel()
        var newLovePoints = p.lovePoints
        var leveledUp = false
        while (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.getActiveStatus()
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel
        val today = dayFormat.format(Date(now))

        val isKevin = isCurrentUserKevin()
        val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
        val bathCountPetKey = if (isKevin) (if (isCuky) "bathCountKevinCuky" else "bathCountKevinThor") else (if (isCuky) "bathCountAliCuky" else "bathCountAliThor")
        val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"
        val bathCountTotalKey = if (isKevin) "bathCountKevin" else "bathCountAli"

        val updates = mutableMapOf<String, Any>(
            "lovePoints" to newLovePoints,
            carePtsPetKey to FieldValue.increment(15),
            bathCountPetKey to FieldValue.increment(1),
            carePtsTotalKey to FieldValue.increment(15),
            bathCountTotalKey to FieldValue.increment(1)
        )

        if (isCuky) {
            updates["cukyCleanliness"] = 100
            updates["cukyHappiness"] = newHappiness
            updates["cukyExperience"] = newExp
            updates["cukyLevel"] = newLevel
            updates["cukyStatus"] = newStatus
            updates["cukyLastBathDate"] = today
            updates["cukyHunger"] = decayedHunger
            updates["cukySleepPercent"] = decayedSleepPercent
            updates["cukyLastInteraction"] = now
            updates["cukyLastDecayUpdate"] = nextDecayUpdate
            updates["cukyIsSleeping"] = isNowSleeping
        } else {
            updates["cleanliness"] = 100
            updates["happiness"] = newHappiness
            updates["experience"] = newExp
            updates["level"] = newLevel
            updates["status"] = newStatus
            updates["lastBathDate"] = today
            updates["hunger"] = decayedHunger
            updates["sleepPercent"] = decayedSleepPercent
            updates["lastInteraction"] = now
            updates["lastDecayUpdate"] = nextDecayUpdate
            updates["isSleeping"] = isNowSleeping
        }

        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                toastMessage.value = "¡${p.getActiveName()} ha quedado súper limpio/a! 🫧🚿"
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.getActiveName(), finalLevel)
                }
            }
    }

    fun playBallPet(points: Int, happinessGain: Int) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.getActiveIsSleeping() || isNightTime) {
            toastMessage.value = "💤 ¡${p.getActiveName()} está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        if (today == p.getActiveLastBallDate()) {
            toastMessage.value = "¡Ya jugaste con la pelota hoy! ⚾"
            return
        }

        val stats = calculateDecay(p, now, isCuky)
        val decayedHunger = stats.hunger
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        val newHappiness = Math.min(100, p.getActiveHappiness() + happinessGain)
        var newLovePoints = p.lovePoints + points
        var newExp = p.getActiveExperience() + 5
        var newLevel = p.getActiveLevel()
        var leveledUp = false
        while (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.getActiveStatus()
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel

        val isKevin = isCurrentUserKevin()
        val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
        val playCountPetKey = if (isKevin) (if (isCuky) "playCountKevinCuky" else "playCountKevinThor") else (if (isCuky) "playCountAliCuky" else "playCountAliThor")
        val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"
        val playCountTotalKey = if (isKevin) "playCountKevin" else "playCountAli"

        val updates = mutableMapOf<String, Any>(
            "lovePoints" to newLovePoints,
            carePtsPetKey to FieldValue.increment(15),
            playCountPetKey to FieldValue.increment(1),
            carePtsTotalKey to FieldValue.increment(15),
            playCountTotalKey to FieldValue.increment(1)
        )

        if (isCuky) {
            updates["cukyHappiness"] = newHappiness
            updates["cukyExperience"] = newExp
            updates["cukyLevel"] = newLevel
            updates["cukyStatus"] = newStatus
            updates["cukyLastBallDate"] = today
            updates["cukyHunger"] = decayedHunger
            updates["cukyCleanliness"] = decayedCleanliness
            updates["cukySleepPercent"] = decayedSleepPercent
            updates["cukyLastInteraction"] = now
            updates["cukyLastDecayUpdate"] = nextDecayUpdate
            updates["cukyIsSleeping"] = isNowSleeping
        } else {
            updates["happiness"] = newHappiness
            updates["experience"] = newExp
            updates["level"] = newLevel
            updates["status"] = newStatus
            updates["lastBallDate"] = today
            updates["hunger"] = decayedHunger
            updates["cleanliness"] = decayedCleanliness
            updates["sleepPercent"] = decayedSleepPercent
            updates["lastInteraction"] = now
            updates["lastDecayUpdate"] = nextDecayUpdate
            updates["isSleeping"] = isNowSleeping
        }

        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                toastMessage.value = "¡Jugaste a la pelota con ${p.getActiveName()}! ⚾"
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.getActiveName(), finalLevel)
                }
            }
    }

    fun playMinigame(gameType: String, points: Int, exp: Int, score: Int = 0) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.getActiveIsSleeping() || isNightTime) {
            toastMessage.value = "💤 ¡${p.getActiveName()} está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        val updateDateField = when (gameType) {
            "memory" -> "lastMemoryDate"
            "flappy" -> "lastFlappyDate"
            else -> "lastSnakeDate"
        }

        val alreadyPlayedToday = when (gameType) {
            "memory" -> today == p.lastMemoryDate
            "flappy" -> today == p.lastFlappyDate
            else -> today == p.lastSnakeDate
        }

        val stats = calculateDecay(p, now, isCuky)
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        // Solo otorgar Puntos de Amor y EXP en la primera partida del día
        val effectivePoints = if (alreadyPlayedToday) 0 else points
        val effectiveExp = if (alreadyPlayedToday) 0 else exp

        var newExp = p.getActiveExperience() + effectiveExp
        var newLevel = p.getActiveLevel()
        var newLovePoints = p.lovePoints + effectivePoints
        val newHappiness = Math.min(100, p.getActiveHappiness() + if (alreadyPlayedToday) 5 else 15)
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.getActiveStatus()
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        var leveledUp = false
        while (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel

        val isKevin = isCurrentUserKevin()
        val carePtsPetKey = if (isKevin) (if (isCuky) "carePointsKevinCuky" else "carePointsKevinThor") else (if (isCuky) "carePointsAliCuky" else "carePointsAliThor")
        val miniCountPetKey = if (isKevin) (if (isCuky) "minigameCountKevinCuky" else "minigameCountKevinThor") else (if (isCuky) "minigameCountAliCuky" else "minigameCountAliThor")
        val carePtsTotalKey = if (isKevin) "carePointsKevin" else "carePointsAli"
        val miniCountTotalKey = if (isKevin) "minigameCountKevin" else "minigameCountAli"

        val updates = mutableMapOf<String, Any>(
            "lovePoints" to newLovePoints,
            updateDateField to today,
            carePtsPetKey to FieldValue.increment(10),
            miniCountPetKey to FieldValue.increment(1),
            carePtsTotalKey to FieldValue.increment(10),
            miniCountTotalKey to FieldValue.increment(1)
        )

        if (isCuky) {
            updates["cukyExperience"] = newExp
            updates["cukyLevel"] = newLevel
            updates["cukyHappiness"] = newHappiness
            updates["cukyStatus"] = newStatus
            updates["cukyHunger"] = 0
            updates["cukyCleanliness"] = decayedCleanliness
            updates["cukySleepPercent"] = decayedSleepPercent
            updates["cukyLastInteraction"] = now
            updates["cukyLastDecayUpdate"] = nextDecayUpdate
            updates["cukyIsSleeping"] = isNowSleeping
        } else {
            updates["experience"] = newExp
            updates["level"] = newLevel
            updates["happiness"] = newHappiness
            updates["status"] = newStatus
            updates["hunger"] = 0
            updates["cleanliness"] = decayedCleanliness
            updates["sleepPercent"] = decayedSleepPercent
            updates["lastInteraction"] = now
            updates["lastDecayUpdate"] = nextDecayUpdate
            updates["isSleeping"] = isNowSleeping
        }

        var newHighScoreBeaten = false
        if (score > 0) {
            if (gameType == "flappy") {
                if (isKevin && score > p.flappyHighScoreKevin) {
                    updates["flappyHighScoreKevin"] = score
                    newHighScoreBeaten = true
                } else if (!isKevin && score > p.flappyHighScoreAli) {
                    updates["flappyHighScoreAli"] = score
                    newHighScoreBeaten = true
                }
            } else if (gameType == "snake") {
                if (isKevin && score > p.snakeHighScoreKevin) {
                    updates["snakeHighScoreKevin"] = score
                    newHighScoreBeaten = true
                } else if (!isKevin && score > p.snakeHighScoreAli) {
                    updates["snakeHighScoreAli"] = score
                    newHighScoreBeaten = true
                }
            }
        }

        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                if (alreadyPlayedToday) {
                    if (newHighScoreBeaten) {
                        toastMessage.value = "🎉 ¡NUEVO RÉCORD: $score PTS! 🏆"
                    } else {
                        toastMessage.value = "¡Bien jugado! 🎮 (Modo libre activo)"
                    }
                } else {
                    if (newHighScoreBeaten) {
                        toastMessage.value = "🎉 ¡NUEVO RÉCORD ($score pts) y premio diario! +$points ❤️ +$exp EXP"
                    } else {
                        toastMessage.value = "¡Premio diario reclamado! +$points ❤️ y +$exp EXP 🎉"
                    }
                }
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.getActiveName(), finalLevel)
                }
            }
            .addOnFailureListener { err ->
                Log.e("MainViewModel", "Error al actualizar recompensa: ${err.message}")
            }
    }

    fun updatePetName(newName: String) {
        val p = _petState.value ?: return
        val field = if (p.isCuky()) "cukyName" else "name"
        val updates = mutableMapOf<String, Any>(field to newName)
        if (!p.isCuky()) {
            updates["thorName"] = newName
        }
        db.collection("pets").document(currentCoupleId).update(updates)
    }

    fun switchPet(newPetType: String) {
        val p = _petState.value ?: return
        val currentActiveName = if (newPetType == Pet.PET_CUKY) {
            if (p.cukyName.isNotBlank()) p.cukyName else "Cuky"
        } else {
            if (p.thorName.isNotBlank()) p.thorName else if (p.name.isNotBlank() && p.name != "Cuky") p.name else "Thor"
        }
        db.collection("pets").document(currentCoupleId)
            .update(
                "petType", newPetType,
                "name", currentActiveName
            )
            .addOnSuccessListener {
                toastMessage.value = if (newPetType == Pet.PET_CUKY) "¡Cuky la gallina ahora te acompaña! 🐔🤎" else "¡Thor el gatito ahora te acompaña! 🐱🤍"
            }
    }

    fun buyAccessory(accessoryId: String, cost: Int) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentUnlocked = if (isCuky) p.cukyUnlockedAccessories else p.unlockedAccessories
        if (p.lovePoints >= cost) {
            val unlocked = ArrayList(currentUnlocked)
            if (!unlocked.contains(accessoryId)) {
                unlocked.add(accessoryId)
                val updates = mutableMapOf<String, Any>(
                    "lovePoints" to (p.lovePoints - cost)
                )
                if (isCuky) {
                    updates["cukyUnlockedAccessories"] = unlocked
                    updates["cukyEquippedAccessory"] = accessoryId
                } else {
                    updates["unlockedAccessories"] = unlocked
                    updates["equippedAccessory"] = accessoryId
                }
                db.collection("pets").document(currentCoupleId)
                    .update(updates)
                    .addOnSuccessListener {
                        toastMessage.value = "¡Accesorio comprado y equipado! ✨"
                    }
            }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun equipAccessory(accessoryId: String) {
        val p = _petState.value ?: return
        val field = if (p.isCuky()) "cukyEquippedAccessory" else "equippedAccessory"
        db.collection("pets").document(currentCoupleId).update(field, accessoryId)
    }

    fun buyBackground(backgroundId: String, cost: Int) {
        val p = _petState.value ?: return
        val isCuky = p.isCuky()
        val currentUnlocked = if (isCuky) p.cukyUnlockedBackgrounds else p.unlockedBackgrounds
        if (p.lovePoints >= cost) {
            val unlocked = ArrayList(currentUnlocked)
            if (!unlocked.contains(backgroundId)) {
                unlocked.add(backgroundId)
            }
            val updates = mutableMapOf<String, Any>(
                "lovePoints" to (p.lovePoints - cost)
            )
            if (isCuky) {
                updates["cukyUnlockedBackgrounds"] = unlocked
                updates["cukyEquippedBackground"] = backgroundId
            } else {
                updates["unlockedBackgrounds"] = unlocked
                updates["equippedBackground"] = backgroundId
            }
            db.collection("pets").document(currentCoupleId)
                .update(updates)
                .addOnSuccessListener {
                    toastMessage.value = "¡Fondo comprado y equipado! 🖼️✨"
                }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun equipBackground(backgroundId: String) {
        val p = _petState.value ?: return
        val field = if (p.isCuky()) "cukyEquippedBackground" else "equippedBackground"
        db.collection("pets").document(currentCoupleId)
            .update(field, backgroundId)
            .addOnSuccessListener {
                toastMessage.value = "¡Fondo equipado! 🖼️"
            }
    }

    // --- MÉTODOS DE MENSAJES Y NOTIFICACIONES ---

    fun saveMessageToFirestore(msg: Message, isEdit: Boolean) {
        db.collection("messages").document(msg.messageId ?: "").set(msg)
            .addOnSuccessListener {
                updatePetOnInteraction()
            }
    }

    fun deleteMessage(msg: Message) {
        db.collection("messages").document(msg.messageId ?: "").delete()
    }

    fun toggleLikeMessage(msg: Message) {
        android.util.Log.d("DIARIO_DEBUG", "toggleLikeMessage llamado para msgId: ${msg.messageId}, liked actual: ${msg.liked}")
        val newLiked = !msg.liked
        msg.liked = newLiked
        val updates = mapOf(
            "liked" to newLiked,
            "isLiked" to newLiked
        )
        db.collection("messages").document(msg.messageId ?: "").update(updates)
            .addOnSuccessListener {
                android.util.Log.d("DIARIO_DEBUG", "toggleLikeMessage Firestore EXITO, nuevo liked: $newLiked")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DIARIO_DEBUG", "toggleLikeMessage Firestore ERROR", e)
            }
        
        // Crear una nueva lista con una copia de la carta modificada para que Compose note el cambio de referencia y recomponga al instante
        val updatedList = (_messagesState.value ?: emptyList()).map {
            if (it.messageId == msg.messageId) it.copy(liked = newLiked) else it
        }
        _messagesState.value = ArrayList(updatedList)
    }

    // --- ENLACE CON ACTIVIDAD PARA COMPATIBILIDAD ---

    private fun updateWidget() {
        val wIntent = Intent(app, LastMessageWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val wIds = AppWidgetManager.getInstance(app)
                .getAppWidgetIds(ComponentName(app, LastMessageWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds)
        }
        app.sendBroadcast(wIntent)

        val wLargeIntent = Intent(app, LastMessageLargeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val wLargeIds = AppWidgetManager.getInstance(app)
                .getAppWidgetIds(ComponentName(app, LastMessageLargeWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wLargeIds)
        }
        app.sendBroadcast(wLargeIntent)
    }

    fun scheduleCalendarReminder(event: CalendarEvent) {
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(app, NotificationReceiver::class.java).apply {
            putExtra("title", "Cita programada")
            putExtra("content", "${event.title} - ${event.description}")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            event.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appointmentLeadTime = prefs.getLong("appointmentLeadTime", 60L) * 60 * 1000
        val alarmTime = event.date - appointmentLeadTime

        if (alarmTime > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            }
        }
    }

    fun rescheduleAllCalendarReminders() {
        db.collection("calendar").whereEqualTo("partnerId", currentCoupleId)
            .get().addOnSuccessListener { snaps ->
                if (snaps != null) {
                    for (doc in snaps) {
                        val ev = doc.toObject(CalendarEvent::class.java)
                        if (ev.eventId.isEmpty()) ev.eventId = doc.id
                        scheduleCalendarReminder(ev)
                    }
                }
            }
    }

    fun adminResetRanking() {
        if (currentCoupleId.isEmpty()) return
        val updates = mapOf<String, Any>(
            "carePointsKevin" to 0,
            "carePointsAli" to 0,
            "carePointsKevinThor" to 0,
            "carePointsAliThor" to 0,
            "carePointsKevinCuky" to 0,
            "carePointsAliCuky" to 0,
            "foodCountKevin" to 0,
            "foodCountAli" to 0,
            "bathCountKevin" to 0,
            "bathCountAli" to 0,
            "ballCountKevin" to 0,
            "ballCountAli" to 0,
            "minigameCountKevin" to 0,
            "minigameCountAli" to 0,
            "foodCountKevinThor" to 0,
            "foodCountAliThor" to 0,
            "bathCountKevinThor" to 0,
            "bathCountAliThor" to 0,
            "ballCountKevinThor" to 0,
            "ballCountAliThor" to 0,
            "minigameCountKevinThor" to 0,
            "minigameCountAliThor" to 0,
            "foodCountKevinCuky" to 0,
            "foodCountAliCuky" to 0,
            "bathCountKevinCuky" to 0,
            "bathCountAliCuky" to 0,
            "ballCountKevinCuky" to 0,
            "ballCountAliCuky" to 0,
            "minigameCountKevinCuky" to 0,
            "minigameCountAliCuky" to 0
        )
        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                toastMessage.value = "👑 ¡Ranking de cuidadores reiniciado a 0!"
            }
            .addOnFailureListener {
                toastMessage.value = "❌ Error al reiniciar ranking: ${it.message}"
            }
    }

    fun adminResetMinigames() {
        if (currentCoupleId.isEmpty()) return
        val updates = mapOf<String, Any?>(
            "flappyHighScoreKevin" to 0,
            "flappyHighScoreAli" to 0,
            "snakeHighScoreKevin" to 0,
            "snakeHighScoreAli" to 0,
            "lastFlappyDate" to "",
            "lastSnakeDate" to "",
            "lastMemoryDate" to ""
        )
        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                val app = getApplication<Application>()
                app.getSharedPreferences("flappy_thor_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                app.getSharedPreferences("snake_game_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                toastMessage.value = "🎮 ¡Récords y partidas de minijuegos reiniciados!"
            }
            .addOnFailureListener {
                toastMessage.value = "❌ Error al reiniciar minijuegos: ${it.message}"
            }
    }

    fun adminResetPets() {
        if (currentCoupleId.isEmpty()) return
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "level" to 1,
            "experience" to 0,
            "happiness" to 100,
            "hunger" to 0,
            "cleanliness" to 100,
            "sleepPercent" to 0,
            "isSleeping" to false,
            "status" to Pet.STATUS_HAPPY,
            "lastInteraction" to now,
            "lastDecayUpdate" to now,
            "streakDays" to 1,
            "cukyLevel" to 1,
            "cukyExperience" to 0,
            "cukyHappiness" to 100,
            "cukyHunger" to 0,
            "cukyCleanliness" to 100,
            "cukySleepPercent" to 0,
            "cukyIsSleeping" to false,
            "cukyStatus" to Pet.STATUS_HAPPY,
            "cukyLastInteraction" to now,
            "cukyLastDecayUpdate" to now,
            "cukyStreakDays" to 1
        )
        db.collection("pets").document(currentCoupleId)
            .update(updates)
            .addOnSuccessListener {
                toastMessage.value = "🐾 ¡Mascotas reiniciadas (Nivel 1, 100% vitalidad)!"
            }
            .addOnFailureListener {
                toastMessage.value = "❌ Error al reiniciar mascotas: ${it.message}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
        petListener?.remove()
        userListener?.remove()
        calendarListener?.remove()
    }
}
