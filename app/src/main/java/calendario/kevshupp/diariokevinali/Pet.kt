package calendario.kevshupp.diariokevinali

data class Pet(
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
    var isSleeping: Boolean = false,
    var hunger: Int = 0,
    var cleanliness: Int = 100,
    var lastBallDate: String? = null,
    var lastSnakeDate: String? = null,
    var lastMemoryDate: String? = null,
    var lastBathDate: String? = null
) {
    companion object {
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
