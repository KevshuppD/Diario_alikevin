package calendario.kevshupp.diariokevinali

import androidx.compose.runtime.Stable
import com.google.firebase.firestore.IgnoreExtraProperties

@Stable
@IgnoreExtraProperties
data class Recipe(
    var recipeId: String? = null,
    var coupleId: String? = null,
    var title: String? = null,
    var ingredients: String? = null,
    var steps: String? = null,
    var imageUrl: String? = null,
    var authorId: String? = null,
    var authorName: String? = null,
    var timestamp: Long = 0,
    // Variaciones dentro de la misma receta (ej. "Limonada con la mitad de azúcar y alulosa")
    var variants: List<RecipeVariant> = emptyList()
)

@Stable
@IgnoreExtraProperties
data class RecipeVariant(
    var title: String? = null,
    var ingredients: String? = null,
    var steps: String? = null,
    var authorName: String? = null
)
