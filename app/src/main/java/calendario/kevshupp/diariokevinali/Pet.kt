package calendario.kevshupp.diariokevinali

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Stable

@IgnoreExtraProperties
@Stable
data class Pet(
    var petType: String = PET_THOR,
    var happiness: Int = 100,
    var level: Int = 1,
    var name: String = "Thor",
    var lastInteraction: Long = System.currentTimeMillis(),
    var status: String = "FELIZ",
    var lovePoints: Int = 0,
    var experience: Int = 0,
    var streakDays: Int = 0,
    var lastInteractionDate: String? = null, // Formato yyyy-MM-dd
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
    var cukyEquippedAccessory: String? = null,
    var cukyUnlockedAccessories: List<String> = mutableListOf(),
    var cukyEquippedBackground: String = "coop",
    var cukyUnlockedBackgrounds: List<String> = listOf("coop"),
    var dailyTapCount: Int = 0,
    var lastTapDate: String? = null,
    var flappyHighScoreKevin: Int = 0,
    var flappyHighScoreAli: Int = 0,
    var snakeHighScoreKevin: Int = 0,
    var snakeHighScoreAli: Int = 0
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

    fun getActiveEquippedAccessory(): String? = if (isCuky()) cukyEquippedAccessory else equippedAccessory

    fun getActiveUnlockedAccessories(): List<String> = if (isCuky()) cukyUnlockedAccessories else unlockedAccessories

    fun getActiveEquippedBackground(): String = if (isCuky()) cukyEquippedBackground.ifBlank { "coop" } else equippedBackground.ifBlank { "default" }

    fun getActiveUnlockedBackgrounds(): List<String> = if (isCuky()) {
        if (cukyUnlockedBackgrounds.isEmpty()) listOf("coop") else cukyUnlockedBackgrounds
    } else {
        if (unlockedBackgrounds.isEmpty()) listOf("default") else unlockedBackgrounds
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
