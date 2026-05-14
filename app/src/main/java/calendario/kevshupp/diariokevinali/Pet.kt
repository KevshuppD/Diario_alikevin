package calendario.kevshupp.diariokevinali

data class Pet(
    var happiness: Int = 100,
    var level: Int = 1,
    var name: String = "Thor",
    var lastInteraction: Long = System.currentTimeMillis(),
    var status: String = "FELIZ"
) {
    companion object {
        const val STATUS_HAPPY = "FELIZ"
        const val STATUS_SAD = "TRISTE"
        const val STATUS_HUNGRY = "HAMBRIENTO"
    }
}
