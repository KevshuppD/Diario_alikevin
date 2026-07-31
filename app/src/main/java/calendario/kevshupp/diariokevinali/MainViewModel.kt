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

                val editor = prefs.edit()
                var changed = false

                if (themeVal != null && themeVal != prefs.getString("theme", "")) {
                    editor.putString("theme", themeVal)
                    _themeState.value = themeVal
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
                }
            } else {
                val initialPet = Pet()
                db.collection("pets").document(currentCoupleId).set(initialPet)
            }
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

    private fun calculateDecay(p: Pet, now: Long): DecayedStats {
        val lastDecay = if (p.lastDecayUpdate != 0L) p.lastDecayUpdate else now
        var decayDiff = now - lastDecay
        if (decayDiff < 0) decayDiff = 0
        val hoursToDecay = decayDiff / (1000 * 60 * 60)

        var decayedHunger = p.hunger
        var decayedCleanliness = p.cleanliness
        var decayedSleepPercent = p.sleepPercent
        var nextDecayUpdate = lastDecay

        if (hoursToDecay >= 1) {
            decayedHunger = Math.min(100, p.hunger + (hoursToDecay * 4).toInt())
            decayedCleanliness = Math.max(0, p.cleanliness - (hoursToDecay * 3).toInt())

            val calendar = java.util.Calendar.getInstance()
            for (h in 1..hoursToDecay) {
                calendar.timeInMillis = lastDecay + h * 3600000L
                val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                // Si la hora es de noche (00:00 - 08:00), recupera energía a 15%/h
                if (hourOfDay in 0..7) {
                    decayedSleepPercent = Math.min(100, decayedSleepPercent + 15)
                } else {
                    // Si el pet fue puesto a dormir manualmente (p.isSleeping), también recupera energía
                    if (p.isSleeping) {
                        decayedSleepPercent = Math.min(100, decayedSleepPercent + 15)
                    } else {
                        decayedSleepPercent = Math.max(0, decayedSleepPercent - 5)
                    }
                }
            }
            nextDecayUpdate += hoursToDecay * 3600000L
        }

        // Determinar si en el momento actual 'now' el pet debe estar durmiendo
        val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = calendarNow.get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7

        var isNowSleeping = isNightTime || p.isSleeping
        var finalSleepPercent = decayedSleepPercent

        // Despertar automáticamente si es de día, estaba durmiendo y la energía llegó al 100%
        if (!isNightTime && p.isSleeping && decayedSleepPercent >= 100) {
            isNowSleeping = false
            finalSleepPercent = 100
        }

        return DecayedStats(decayedHunger, decayedCleanliness, finalSleepPercent, nextDecayUpdate, isNowSleeping)
    }

    private fun checkPetDecay(p: Pet) {
        val now = System.currentTimeMillis()

        val stats = calculateDecay(p, now)
        val newHunger = stats.hunger
        val newCleanliness = stats.cleanliness
        val newSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        val happinessDiff = now - p.lastInteraction
        val daysToDecayHappiness = happinessDiff / (1000L * 60L * 60L * 24L)

        var newHappiness = p.happiness
        var lastInteractionCompensated = p.lastInteraction

        if (daysToDecayHappiness >= 1) {
            val decay = (daysToDecayHappiness * 20).toInt()
            newHappiness = Math.max(0, p.happiness - decay)
            lastInteractionCompensated += daysToDecayHappiness * 24L * 60L * 60L * 1000L
        }

        var newStatus = p.status
        if (isNowSleeping) {
            newStatus = Pet.STATUS_SLEEPING
        } else if (newHunger >= 70) {
            newStatus = Pet.STATUS_HUNGRY
        } else {
            newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
        }

        val hasChanged = newHappiness != p.happiness ||
                newHunger != p.hunger ||
                newCleanliness != p.cleanliness ||
                newSleepPercent != p.sleepPercent ||
                newStatus != p.status ||
                nextDecayUpdate != p.lastDecayUpdate ||
                isNowSleeping != p.isSleeping

        if (hasChanged) {
            db.collection("pets").document(currentCoupleId)
                .update(
                    "happiness", newHappiness,
                    "hunger", newHunger,
                    "cleanliness", newCleanliness,
                    "sleepPercent", newSleepPercent,
                    "status", newStatus,
                    "lastInteraction", lastInteractionCompensated,
                    "lastDecayUpdate", nextDecayUpdate,
                    "isSleeping", isNowSleeping
                )
        }
    }

    private fun savePetDataToWidgetPrefs(p: Pet) {
        val wPrefs = app.getSharedPreferences("thor_widget_prefs", Context.MODE_PRIVATE)
        wPrefs.edit()
            .putString("pet_name", p.name)
            .putInt("pet_level", p.level)
            .putInt("pet_happiness", p.happiness)
            .putString("pet_status", p.status)
            .putString("pet_accessory", p.equippedAccessory ?: "none")
            .putBoolean("pet_sleeping", p.isSleeping)
            .putInt("pet_hunger", p.hunger)
            .putInt("pet_cleanliness", p.cleanliness)
            .apply()
        ThorWidgetProvider.triggerUpdate(app)
    }

    fun updatePetOnInteraction() {
        val p = _petState.value ?: return
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))

        val stats = calculateDecay(p, now)
        val currentCleanliness = stats.cleanliness
        val currentSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate

        val newHappiness = Math.min(100, p.happiness + 10)
        var newLovePoints = p.lovePoints + 5
        var newExp = p.experience + 10
        var newLevel = p.level
        var newStreak = p.streakDays

        if (p.lastInteractionDate == null || p.lastInteractionDate != today) {
            if (p.lastInteractionDate != null) {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yesterdayStr = dayFormat.format(yesterday.time)

                if (p.lastInteractionDate == yesterdayStr) {
                    newStreak++
                    newLovePoints += (newStreak * 2)
                } else {
                    newStreak = 1
                }
            } else {
                newStreak = 1
            }
        }

        var leveledUp = false
        if (newExp >= 100) {
            newLevel++
            newExp -= 100;
            newLovePoints += 50
            leveledUp = true
        }

        db.collection("pets").document(currentCoupleId)
            .update(
                "happiness", newHappiness,
                "lovePoints", newLovePoints,
                "experience", newExp,
                "level", newLevel,
                "streakDays", newStreak,
                "lastInteractionDate", today,
                "lastInteraction", now,
                "lastDecayUpdate", nextDecayUpdate,
                "hunger", 0,
                "cleanliness", currentCleanliness,
                "sleepPercent", currentSleepPercent,
                "status", if (stats.isSleeping) Pet.STATUS_SLEEPING else Pet.STATUS_HAPPY,
                "isSleeping", stats.isSleeping
            )
            .addOnSuccessListener {
                if (leveledUp) {
                    levelUpEvent.value = Pair(p.name ?: "Thor", newLevel)
                }
            }
    }

    fun feedPet(foodId: String, cost: Int, happinessGain: Int) {
        val p = _petState.value ?: return
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.isSleeping || isNightTime) {
            toastMessage.value = "💤 ¡Thor está durmiendo!"
            return
        }
        if (p.lovePoints >= cost) {
            val now = System.currentTimeMillis()
            val stats = calculateDecay(p, now)
            val decayedCleanliness = stats.cleanliness
            val decayedSleepPercent = stats.sleepPercent
            val nextDecayUpdate = stats.nextDecayUpdate
            val isNowSleeping = stats.isSleeping

            val currentHappiness = p.happiness
            val newHappiness = Math.min(100, currentHappiness + happinessGain)
            var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.status
            if (!isNowSleeping) {
                if (newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
                    newStatus = Pet.STATUS_HAPPY
                }
            }

            db.collection("pets").document(currentCoupleId)
                .update(
                    "lovePoints", p.lovePoints - cost,
                    "happiness", newHappiness,
                    "status", newStatus,
                    "hunger", 0,
                    "cleanliness", decayedCleanliness,
                    "sleepPercent", decayedSleepPercent,
                    "lastInteraction", now,
                    "lastDecayUpdate", nextDecayUpdate,
                    "isSleeping", isNowSleeping
                )
                .addOnSuccessListener {
                    toastMessage.value = "¡Le has dado de comer a Thor! 💖 +$happinessGain% Felicidad"
                }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun rewardPet(points: Int, exp: Int) {
        val p = _petState.value ?: return
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.isSleeping || isNightTime) {
            toastMessage.value = "💤 ¡Thor está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        val currentDailyTaps = if (today == p.lastTapDate) p.dailyTapCount else 0

        val maxDailyTaps = 30
        val allowedTaps = Math.max(0, maxDailyTaps - currentDailyTaps)

        if (allowedTaps <= 0) {
            toastMessage.value = "¡Thor ya recibió suficiente cariño por hoy! 💖 (Límite: $maxDailyTaps/día)"
            return
        }

        val actualTapsAdded = Math.min(points, allowedTaps)

        val stats = calculateDecay(p, now)
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        var newExp = p.experience + exp
        var newLevel = p.level
        var newLovePoints = p.lovePoints + actualTapsAdded
        val newHappiness = Math.min(100, p.happiness + actualTapsAdded)

        var newStatus = p.status
        if (isNowSleeping) {
            newStatus = Pet.STATUS_SLEEPING
        } else if (stats.hunger >= 70) {
            newStatus = Pet.STATUS_HUNGRY
        } else {
            newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
        }

        var leveledUp = false
        if (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }

        val totalTapsToday = currentDailyTaps + actualTapsAdded
        val finalLevel = newLevel
        val showLevelUpToast = leveledUp

        db.collection("pets").document(currentCoupleId)
            .update(
                "lovePoints", newLovePoints,
                "experience", newExp,
                "level", newLevel,
                "happiness", newHappiness,
                "status", newStatus,
                "hunger", 0,
                "cleanliness", decayedCleanliness,
                "sleepPercent", decayedSleepPercent,
                "lastInteraction", now,
                "lastDecayUpdate", nextDecayUpdate,
                "isSleeping", isNowSleeping,
                "dailyTapCount", totalTapsToday,
                "lastTapDate", today
            )
            .addOnSuccessListener {
                if (totalTapsToday >= maxDailyTaps) {
                    toastMessage.value = "¡Thor se siente amado! ❤️ +$actualTapsAdded Amor (¡Límite alcanzado! 🎉)"
                } else {
                    toastMessage.value = "¡Thor se siente amado! ❤️ +$actualTapsAdded Amor ($totalTapsToday/$maxDailyTaps)"
                }
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.name ?: "Thor", finalLevel)
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
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (isNightTime) {
            toastMessage.value = "💤 Thor tiene que dormir durante la noche (00:00 - 08:00)."
            return
        }

        val dndActive = isDoNotDisturbActive()
        val targetSleepState = !p.isSleeping

        if (!targetSleepState && dndActive) {
            toastMessage.value = "No puedes despertar a Thor mientras el modo No Molestar esté activo en tu celular. 📵"
            return
        }

        val now = System.currentTimeMillis()
        val stats = calculateDecay(p, now)
        val decayedHunger = stats.hunger
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate

        var newHappiness = p.happiness
        var newStatus = p.status

        if (targetSleepState) {
            if (decayedSleepPercent >= 100) {
                toastMessage.value = "¡Thor ya está completamente descansado! ☀️ No necesita dormir."
                return
            }
            newStatus = Pet.STATUS_SLEEPING
            db.collection("pets").document(currentCoupleId)
                .update(
                    "isSleeping", true,
                    "status", newStatus,
                    "hunger", decayedHunger,
                    "cleanliness", decayedCleanliness,
                    "sleepPercent", decayedSleepPercent,
                    "lastInteraction", now,
                    "lastDecayUpdate", nextDecayUpdate,
                    "dndTriggeredByUserId", null
                )
                .addOnSuccessListener {
                    toastMessage.value = "¡Thor se ha ido a dormir! 🌙 Shhh..."
                }
        } else {
            newHappiness = Math.min(100, newHappiness + 20)
            newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
            db.collection("pets").document(currentCoupleId)
                .update(
                    "isSleeping", false,
                    "status", newStatus,
                    "happiness", newHappiness,
                    "hunger", decayedHunger,
                    "cleanliness", decayedCleanliness,
                    "sleepPercent", decayedSleepPercent,
                    "lastInteraction", now,
                    "lastDecayUpdate", nextDecayUpdate,
                    "dndTriggeredByUserId", null
                )
                .addOnSuccessListener {
                    toastMessage.value = "¡Thor ha despertado muy alegre! ☀️ +20% Felicidad"
                }
        }
    }

    fun syncDndStateWithPet() {
        val p = _petState.value ?: return
        val dndActive = isDoNotDisturbActive()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7

        val shouldSleep = dndActive || isNightTime

        if (shouldSleep) {
            if (!p.isSleeping) {
                db.collection("pets").document(currentCoupleId)
                    .update(
                        "isSleeping", true,
                        "status", Pet.STATUS_SLEEPING,
                        "dndTriggeredByUserId", if (dndActive) currentUserId else null
                    )
                    .addOnSuccessListener {
                        if (dndActive) {
                            toastMessage.value = "Thor se durmió porque activaste No Molestar 🌙"
                        }
                    }
            }
        } else {
            if (p.isSleeping && (p.dndTriggeredByUserId == null || currentUserId == p.dndTriggeredByUserId)) {
                val newHappiness = Math.min(100, p.happiness + 20)
                val newStatus = if (newHappiness > 40) Pet.STATUS_HAPPY else Pet.STATUS_SAD
                db.collection("pets").document(currentCoupleId)
                    .update(
                        "isSleeping", false,
                        "status", newStatus,
                        "happiness", newHappiness,
                        "lastInteraction", System.currentTimeMillis(),
                        "dndTriggeredByUserId", null
                    )
                    .addOnSuccessListener {
                        toastMessage.value = "¡Thor despertó! ☀️"
                    }
            }
        }
    }

    fun bathPet() {
        val p = _petState.value ?: return
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.isSleeping || isNightTime) {
            toastMessage.value = "💤 ¡Thor está durmiendo!"
            return
        }
        if (p.cleanliness >= 80) {
            toastMessage.value = "¡${p.name} todavía está limpio! 🫧 (Limpieza: ${p.cleanliness}%)"
            return
        }

        val now = System.currentTimeMillis()
        val stats = calculateDecay(p, now)
        val decayedHunger = stats.hunger
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        val newHappiness = Math.min(100, p.happiness + 10)
        var newExp = p.experience + 3
        var newLevel = p.level
        var newLovePoints = p.lovePoints
        var leveledUp = false
        if (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.status
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel
        val today = dayFormat.format(Date(now))

        db.collection("pets").document(currentCoupleId)
            .update(
                "cleanliness", 100,
                "happiness", newHappiness,
                "experience", newExp,
                "level", newLevel,
                "lovePoints", newLovePoints,
                "status", newStatus,
                "lastBathDate", today,
                "hunger", decayedHunger,
                "sleepPercent", decayedSleepPercent,
                "lastInteraction", now,
                "lastDecayUpdate", nextDecayUpdate,
                "isSleeping", isNowSleeping
            )
            .addOnSuccessListener {
                toastMessage.value = "¡Thor ha quedado súper limpio! 🫧🚿"
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.name ?: "Thor", finalLevel)
                }
            }
    }

    fun playBallPet(points: Int, happinessGain: Int) {
        val p = _petState.value ?: return
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.isSleeping || isNightTime) {
            toastMessage.value = "💤 ¡Thor está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        if (today == p.lastBallDate) {
            toastMessage.value = "¡Ya jugaste con la pelota hoy! ⚾"
            return
        }

        val stats = calculateDecay(p, now)
        val decayedHunger = stats.hunger
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        val newHappiness = Math.min(100, p.happiness + happinessGain)
        var newLovePoints = p.lovePoints + points
        var newExp = p.experience + 10
        var newLevel = p.level
        var leveledUp = false
        if (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.status
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel

        db.collection("pets").document(currentCoupleId)
            .update(
                "happiness", newHappiness,
                "lovePoints", newLovePoints,
                "experience", newExp,
                "level", newLevel,
                "status", newStatus,
                "lastBallDate", today,
                "hunger", decayedHunger,
                "cleanliness", decayedCleanliness,
                "sleepPercent", decayedSleepPercent,
                "lastInteraction", now,
                "lastDecayUpdate", nextDecayUpdate,
                "isSleeping", isNowSleeping
            )
            .addOnSuccessListener {
                toastMessage.value = "¡Jugaste a la pelota con Thor! ⚾"
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.name ?: "Thor", finalLevel)
                }
            }
    }

    fun playMinigame(gameType: String, points: Int, exp: Int) {
        val p = _petState.value ?: return
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 0..7
        if (p.isSleeping || isNightTime) {
            toastMessage.value = "💤 ¡Thor está durmiendo!"
            return
        }
        val now = System.currentTimeMillis()
        val today = dayFormat.format(Date(now))
        val updateDateField = if ("memory" == gameType) "lastMemoryDate" else "lastSnakeDate"

        if ("memory" == gameType && today == p.lastMemoryDate) {
            toastMessage.value = "¡Ya jugaste a Retro Memory hoy! 🧠"
            return
        }
        if ("snake" == gameType && today == p.lastSnakeDate) {
            toastMessage.value = "¡Ya jugaste a La Serpiente hoy! 🐍"
            return
        }

        val stats = calculateDecay(p, now)
        val decayedCleanliness = stats.cleanliness
        val decayedSleepPercent = stats.sleepPercent
        val nextDecayUpdate = stats.nextDecayUpdate
        val isNowSleeping = stats.isSleeping

        var newExp = p.experience + exp
        var newLevel = p.level
        var newLovePoints = p.lovePoints + points
        val newHappiness = Math.min(100, p.happiness + 15)
        var newStatus = if (isNowSleeping) Pet.STATUS_SLEEPING else p.status
        if (!isNowSleeping && newHappiness > 40 && Pet.STATUS_SAD == newStatus) {
            newStatus = Pet.STATUS_HAPPY
        }
        var leveledUp = false
        if (newExp >= 100) {
            newLevel++
            newExp -= 100
            newLovePoints += 50
            leveledUp = true
        }
        val showLevelUpToast = leveledUp
        val finalLevel = newLevel

        db.collection("pets").document(currentCoupleId)
            .update(
                "lovePoints", newLovePoints,
                "experience", newExp,
                "level", newLevel,
                "happiness", newHappiness,
                "status", newStatus,
                "hunger", 0,
                "cleanliness", decayedCleanliness,
                "sleepPercent", decayedSleepPercent,
                updateDateField, today,
                "lastInteraction", now,
                "lastDecayUpdate", nextDecayUpdate,
                "isSleeping", isNowSleeping
            )
            .addOnSuccessListener {
                toastMessage.value = "¡Premio reclamado! +$points ❤️ y +$exp EXP    "
                if (showLevelUpToast) {
                    levelUpEvent.value = Pair(p.name ?: "Thor", finalLevel)
                }
            }
            .addOnFailureListener { err ->
                Log.e("MainViewModel", "Error al actualizar recompensa: ${err.message}")
            }
    }

    fun updatePetName(newName: String) {
        db.collection("pets").document(currentCoupleId).update("name", newName)
    }

    fun buyAccessory(accessoryId: String, cost: Int) {
        val p = _petState.value ?: return
        if (p.lovePoints >= cost) {
            val unlocked = ArrayList(p.unlockedAccessories)
            if (!unlocked.contains(accessoryId)) {
                unlocked.add(accessoryId)
                db.collection("pets").document(currentCoupleId)
                    .update(
                        "lovePoints", p.lovePoints - cost,
                        "unlockedAccessories", unlocked,
                        "equippedAccessory", accessoryId
                    )
                    .addOnSuccessListener {
                        toastMessage.value = "¡Accesorio comprado y equipado! ✨"
                    }
            }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun equipAccessory(accessoryId: String) {
        db.collection("pets").document(currentCoupleId).update("equippedAccessory", accessoryId)
    }

    fun buyBackground(backgroundId: String, cost: Int) {
        val p = _petState.value ?: return
        if (p.lovePoints >= cost) {
            val unlocked = ArrayList(p.unlockedBackgrounds)
            if (!unlocked.contains(backgroundId)) {
                unlocked.add(backgroundId)
            }
            db.collection("pets").document(currentCoupleId)
                .update(
                    "lovePoints", p.lovePoints - cost,
                    "unlockedBackgrounds", unlocked,
                    "equippedBackground", backgroundId
                )
                .addOnSuccessListener {
                    toastMessage.value = "¡Fondo comprado y equipado! 🖼️✨"
                }
        } else {
            toastMessage.value = "No tienes suficientes puntos de amor ❤️"
        }
    }

    fun equipBackground(backgroundId: String) {
        val p = _petState.value ?: return
        db.collection("pets").document(currentCoupleId)
            .update("equippedBackground", backgroundId)
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

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
        petListener?.remove()
        userListener?.remove()
        calendarListener?.remove()
    }
}
