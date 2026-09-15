package calendario.kevshupp.diariokevinali

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Stable

@IgnoreExtraProperties
@Stable
data class Pet(
    var petType: String = PET_THOR,
    // --- THOR STATS ---
    var happiness: Int = 100,
    var level: Int = 1,
    var name: String = "Thor",
    var thorName: String = "Thor",
    var lastInteraction: Long = System.currentTimeMillis(),
    var status: String = "FELIZ",
    var lovePoints: Int = 0,
    var experience: Int = 0,
    var streakDays: Int = 0,
    var lastInteractionDate: String? = null,
    var equippedAccessory: String? = null,
    var unlockedAccessories: List<String> = mutableListOf(),
    var hunger: Int = 0,
    var cleanliness: Int = 100,
    var lastBallDate: String? = null,
    var lastSnakeDate: String? = null,
    var lastMemoryDate: String? = null,
    var lastFlappyDate: String? = null,
    var lastBathDate: String? = null,
    var lastDecayUpdate: Long = System.currentTimeMillis(),
    var dndTriggeredByUserId: String? = null,
    var sleepPercent: Int = 100,
    var unlockedBackgrounds: List<String> = listOf("default"),
    var equippedBackground: String = "default",
    var dailyTapCount: Int = 0,
    var lastTapDate: String? = null,

    // --- CUKY STATS (Completamente Separadas e Independientes) ---
    var cukyName: String = "Cuky",
    var cukyHappiness: Int = 100,
    var cukyLevel: Int = 1,
    var cukyExperience: Int = 0,
    var cukyHunger: Int = 0,
    var cukyCleanliness: Int = 100,
    var cukySleepPercent: Int = 100,
    var cukyStatus: String = "FELIZ",
    var cukyIsSleeping: Boolean = false,
    var cukyStreakDays: Int = 0,
    var cukyLastInteraction: Long = System.currentTimeMillis(),
    var cukyLastInteractionDate: String? = null,
    var cukyLastDecayUpdate: Long = System.currentTimeMillis(),
    var cukyLastBallDate: String? = null,
    var cukyLastBathDate: String? = null,
    var cukyDailyTapCount: Int = 0,
    var cukyLastTapDate: String? = null,
    var cukyEquippedAccessory: String? = null,
    var cukyUnlockedAccessories: List<String> = mutableListOf(),
    var cukyEquippedBackground: String = "coop",
    var cukyUnlockedBackgrounds: List<String> = listOf("coop"),

    // --- MINIJUEGOS Y RÉCORDS ---
    var flappyHighScoreKevin: Int = 0,
    var flappyHighScoreAli: Int = 0,
    var snakeHighScoreKevin: Int = 0,
    var snakeHighScoreAli: Int = 0,

    // --- RANKING CUIDADOS THOR ---
    var carePointsKevinThor: Int = 0,
    var carePointsAliThor: Int = 0,
    var feedCountKevinThor: Int = 0,
    var feedCountAliThor: Int = 0,
    var bathCountKevinThor: Int = 0,
    var bathCountAliThor: Int = 0,
    var playCountKevinThor: Int = 0,
    var playCountAliThor: Int = 0,
    var tapCountKevinThor: Int = 0,
    var tapCountAliThor: Int = 0,
    var minigameCountKevinThor: Int = 0,
    var minigameCountAliThor: Int = 0,

    // --- RANKING CUIDADOS CUKY ---
    var carePointsKevinCuky: Int = 0,
    var carePointsAliCuky: Int = 0,
    var feedCountKevinCuky: Int = 0,
    var feedCountAliCuky: Int = 0,
    var bathCountKevinCuky: Int = 0,
    var bathCountAliCuky: Int = 0,
    var playCountKevinCuky: Int = 0,
    var playCountAliCuky: Int = 0,
    var tapCountKevinCuky: Int = 0,
    var tapCountAliCuky: Int = 0,
    var minigameCountKevinCuky: Int = 0,
    var minigameCountAliCuky: Int = 0,

    // --- TOTALES / COMPATIBILIDAD ---
    var carePointsKevin: Int = 0,
    var carePointsAli: Int = 0,
    var feedCountKevin: Int = 0,
    var feedCountAli: Int = 0,
    var bathCountKevin: Int = 0,
    var bathCountAli: Int = 0,
    var playCountKevin: Int = 0,
    var playCountAli: Int = 0,
    var tapCountKevin: Int = 0,
    var tapCountAli: Int = 0,
    var minigameCountKevin: Int = 0,
    var minigameCountAli: Int = 0
) {
    @get:Exclude
    var isSleepingSetFromNewField: Boolean = false

    @get:PropertyName("isSleeping")
    @set:PropertyName("isSleeping")
    var isSleeping: Boolean = false
        set(value) {
            field = value
            isSleepingSetFromNewField = true
        }

    @get:PropertyName("sleeping")
    @set:PropertyName("sleeping")
    var sleepingFallback: Boolean
        get() = isSleeping
        set(value) {
            if (!isSleepingSetFromNewField) {
                isSleeping = value
            }
        }

    fun isCuky(): Boolean = petType.equals(PET_CUKY, ignoreCase = true) || name.equals("Cuky", ignoreCase = true)

    fun getActiveName(): String = if (isCuky()) cukyName.ifBlank { "Cuky" } else (if (name.isNotBlank() && name != "Thor" && name != "Cuky") name else thorName.ifBlank { "Thor" })
    fun getActiveLevel(): Int = if (isCuky()) cukyLevel else level
    fun getActiveExperience(): Int = if (isCuky()) cukyExperience else experience
    fun getActiveHappiness(): Int = if (isCuky()) cukyHappiness else happiness
    fun getActiveHunger(): Int = if (isCuky()) cukyHunger else hunger
    fun getActiveCleanliness(): Int = if (isCuky()) cukyCleanliness else cleanliness
    fun getActiveSleepPercent(): Int = if (isCuky()) cukySleepPercent else sleepPercent
    fun getActiveIsSleeping(): Boolean = if (isCuky()) cukyIsSleeping else isSleeping
    fun getActiveStatus(): String = if (isCuky()) cukyStatus else status
    fun getActiveStreak(): Int = if (isCuky()) cukyStreakDays else streakDays
    fun getActiveLastInteraction(): Long = if (isCuky()) cukyLastInteraction else lastInteraction
    fun getActiveLastInteractionDate(): String? = if (isCuky()) cukyLastInteractionDate else lastInteractionDate
    fun getActiveLastDecayUpdate(): Long = if (isCuky()) cukyLastDecayUpdate else lastDecayUpdate
    fun getActiveLastBallDate(): String? = if (isCuky()) cukyLastBallDate else lastBallDate
    fun getActiveDailyTapCount(): Int = if (isCuky()) cukyDailyTapCount else dailyTapCount
    fun getActiveLastTapDate(): String? = if (isCuky()) cukyLastTapDate else lastTapDate

    fun getActiveEquippedAccessory(): String? = if (isCuky()) cukyEquippedAccessory else equippedAccessory

    fun getActiveUnlockedAccessories(): List<String> = if (isCuky()) cukyUnlockedAccessories else unlockedAccessories

    fun getActiveEquippedBackground(): String = if (isCuky()) cukyEquippedBackground.ifBlank { "coop" } else equippedBackground.ifBlank { "default" }

    fun getActiveUnlockedBackgrounds(): List<String> = if (isCuky()) {
        if (cukyUnlockedBackgrounds.isEmpty()) listOf("coop") else cukyUnlockedBackgrounds
    } else {
        if (unlockedBackgrounds.isEmpty()) listOf("default") else unlockedBackgrounds
    }

    // --- MÉTODOS DE CÁLCULO DE CUIDADOS POR MASCOTA Y GLOBAL ---
    fun getTotalCareKevin(): Int = carePointsKevinThor + carePointsKevinCuky + carePointsKevin
    fun getThorCareKevin(): Int = carePointsKevinThor + (if (carePointsKevinThor == 0 && carePointsKevinCuky == 0) carePointsKevin else 0)
    fun getCukyCareKevin(): Int = carePointsKevinCuky

    fun getTotalCareAli(): Int = carePointsAliThor + carePointsAliCuky + carePointsAli
    fun getThorCareAli(): Int = carePointsAliThor + (if (carePointsAliThor == 0 && carePointsAliCuky == 0) carePointsAli else 0)
    fun getCukyCareAli(): Int = carePointsAliCuky

    fun getCaregiverLevel(points: Int): Int {
        return when {
            points >= 1000 -> 6
            points >= 600 -> 5
            points >= 300 -> 4
            points >= 150 -> 3
            points >= 50 -> 2
            else -> 1
        }
    }

    fun getCaregiverTitle(points: Int, isAli: Boolean): String {
        val level = getCaregiverLevel(points)
        return when (level) {
            6 -> if (isAli) "👑 Maestra Legendaria" else "👑 Maestro Legendario"
            5 -> if (isAli) "⭐ Guardiana Estelar" else "⭐ Guardián Estelar"
            4 -> if (isAli) "💖 Cuidadora Experta" else "💖 Cuidador Experto"
            3 -> if (isAli) "🐾 Amiga de Oro" else "🐾 Amigo de Oro"
            2 -> if (isAli) "🌸 Cuidadora Dedicada" else "🌿 Cuidador Dedicado"
            else -> if (isAli) "🌱 Cuidadora Novata" else "🌱 Cuidador Novato"
        }
    }

    fun getCaregiverProgress(points: Int): Float {
        val thresholds = listOf(0, 50, 150, 300, 600, 1000)
        val level = getCaregiverLevel(points)
        if (level >= 6) return 1f
        val currentFloor = thresholds[level - 1]
        val nextCeil = thresholds[level]
        return ((points - currentFloor).toFloat() / (nextCeil - currentFloor).toFloat()).coerceIn(0f, 1f)
    }

    fun getCaregiverNextLevelTarget(points: Int): Int {
        val thresholds = listOf(50, 150, 300, 600, 1000, 1000)
        val level = getCaregiverLevel(points)
        if (level >= 6) return 1000
        return thresholds[level - 1]
    }

    companion object {
        const val PET_THOR = "thor"
        const val PET_CUKY = "cuky"

        const val STATUS_HAPPY = "FELIZ"
        const val STATUS_SAD = "TRISTE"
        const val STATUS_HUNGRY = "HAMBRIENTO"
        const val STATUS_SLEEPING = "DURMIENDO"
        const val STATUS_EVOLVING = "EVOLUCIONANDO"
        
        // IDs de accesorios disponibles
        const val ACC_NONE = "none"
        const val ACC_HAT = "hat"
        const val ACC_BOW = "bow"
        const val ACC_GLASSES = "glasses"
        const val ACC_CROWN = "crown"
        const val ACC_COLLAR = "collar"
        const val ACC_MUSTACHE = "mustache"
        const val ACC_BALLOON = "balloon"
        const val ACC_BANDANA = "bandana"
        const val ACC_BANANA = "banana"
        const val ACC_SOCKS = "socks"
    }
}
