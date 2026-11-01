package calendario.kevshupp.diariokevinali

import com.google.firebase.firestore.DocumentSnapshot

/** Premios de mascota ganados con los espíritus de la Temporada 3. Lo gana quien primero llega a la meta por su cuenta. */
enum class RewardKind { SPIRITS, MASTERY, NORMAL, LUCKY, CURSED, SNAIL }

data class SpiritReward(val id: String, val name: String, val kind: RewardKind, val goal: Int)

data class SpiritCounts(val kevin: Int, val ali: Int)

object SpiritRewards {
    const val COLLECTION = "fortnite_spirits_s3"
    private val LUCKY = setOf("02", "05", "08", "11", "14", "17", "20", "24", "27", "30", "33")
    private val CURSED = setOf("03", "06", "09", "12", "15", "18", "21", "25", "28", "31", "34")
    private const val SNAIL_ID = "35"

    const val LOVE_POINTS_ID = "love_points"
    const val LOVE_POINTS_AMOUNT = 50L

    val ALL = listOf(
        SpiritReward(LOVE_POINTS_ID, "Puntos de Amor", RewardKind.SPIRITS, 5),
        SpiritReward("aura", "Aura espiritual", RewardKind.MASTERY, 5),
        SpiritReward("bg_forest", "Bosque de los Espíritus", RewardKind.SPIRITS, 10),
        SpiritReward("lantern", "Farol espiritual", RewardKind.MASTERY, 10),
        SpiritReward("bg_lake", "Lago de los Faroles", RewardKind.SPIRITS, 15),
        SpiritReward("fox_mask", "Máscara de zorro", RewardKind.MASTERY, 15),
        SpiritReward("bg_shrine", "Santuario", RewardKind.SPIRITS, 25),
        SpiritReward("wisp", "Espíritu compañero", RewardKind.MASTERY, 25),
        SpiritReward("spirit_crown", "Corona de espíritus", RewardKind.SPIRITS, 35),
        SpiritReward("bg_realm", "Reino Espiritual", RewardKind.MASTERY, 35),
        SpiritReward("firefly", "Luciérnaga", RewardKind.NORMAL, 5),
        SpiritReward("star", "Estrella Guía", RewardKind.NORMAL, 10),
        SpiritReward("clover", "Trébol de la Suerte", RewardKind.LUCKY, 5),
        SpiritReward("coin", "Moneda de la Fortuna", RewardKind.LUCKY, 10),
        SpiritReward("skull", "Calaverita Maldita", RewardKind.CURSED, 5),
        SpiritReward("flame", "Llama Maldita", RewardKind.CURSED, 10),
        SpiritReward("snail", "Den Den Mushi", RewardKind.SNAIL, 1)
    )

    private fun count(kind: RewardKind, list: List<String>, mastery: List<String>): Int = when (kind) {
        RewardKind.SPIRITS -> list.size
        RewardKind.MASTERY -> mastery.size
        RewardKind.LUCKY -> list.count { it in LUCKY }
        RewardKind.CURSED -> list.count { it in CURSED }
        RewardKind.NORMAL -> list.count { it !in LUCKY && it !in CURSED }
        RewardKind.SNAIL -> if (SNAIL_ID in list) 1 else 0
    }

    private fun strings(doc: DocumentSnapshot, field: String) =
        (doc.get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    /** Solo lee el documento; nunca modifica las listas ni las maestrías. */
    fun progress(doc: DocumentSnapshot): Map<String, SpiritCounts> {
        val kevin = strings(doc, "kevin_list"); val ali = strings(doc, "ali_list")
        val kevinM = strings(doc, "kevin_mastery"); val aliM = strings(doc, "ali_mastery")
        return ALL.associate { it.id to SpiritCounts(count(it.kind, kevin, kevinM), count(it.kind, ali, aliM)) }
    }

    /** Premios recién alcanzados y quién los ganó; si ambos llegaron, quien lleva más. */
    fun newWinners(progress: Map<String, SpiritCounts>, won: Map<String, String>): Map<String, String> =
        ALL.filter { it.id !in won }.mapNotNull { reward ->
            val p = progress[reward.id] ?: return@mapNotNull null
            when {
                maxOf(p.kevin, p.ali) < reward.goal -> null
                p.kevin >= p.ali -> reward.id to "Kevin"
                else -> reward.id to "Ali"
            }
        }.toMap()

    fun lockText(reward: SpiritReward, p: SpiritCounts?): String {
        if (reward.kind == RewardKind.SNAIL) return "Falta Transponder Snail"
        val kevin = p?.kevin ?: 0; val ali = p?.ali ?: 0
        val leader = if (ali > kevin) "Ali" else "Kevin"
        val unit = when (reward.kind) {
            RewardKind.MASTERY -> "maestrías"
            RewardKind.LUCKY -> "afortunados"
            RewardKind.CURSED -> "malditos"
            RewardKind.NORMAL -> "normales"
            else -> "espíritus"
        }
        return "$leader ${maxOf(kevin, ali)}/${reward.goal} $unit"
    }
}
