package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.RecipeListScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RecipeFragment : Fragment() {
    private var coupleId: String? = null
    private var theme: String = "Pixel Claro"
    private var currentUserId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private var recipeListener: ListenerRegistration? = null

    companion object {
        @JvmStatic
        fun newInstance(coupleId: String, theme: String): RecipeFragment {
            val f = RecipeFragment()
            val a = Bundle()
            a.putString("coupleId", coupleId)
            a.putString("theme", theme)
            f.arguments = a
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coupleId = arguments?.getString("coupleId")
        theme = arguments?.getString("theme") ?: "Pixel Claro"
        val prefs = activity?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
        currentUserId = prefs?.getString("userId", null)
    }

    fun setImageUrl(url: String) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var recipes by remember { mutableStateOf(listOf<Recipe>()) }
                val isDark = theme == "Pixel Oscuro"
                val activityRef = activity as? MainActivity

                LaunchedEffect(Unit) {
                    if (coupleId.isNullOrEmpty()) {
                        val prefs = activityRef?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                        coupleId = prefs?.getString("coupleId", coupleId)
                        currentUserId = prefs?.getString("userId", currentUserId)
                        theme = prefs?.getString("theme", theme) ?: theme
                    }
                }

                DisposableEffect(coupleId) {
                    recipeListener?.remove()
                    recipeListener = db.collection("recipes")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { value, _ ->
                            if (value != null) {
                                val wantedId = coupleId
                                val wantedIdNormalized = normalizeCoupleId(wantedId)
                                val ownerId = currentUserId
                                val filtered = value.mapNotNull { doc ->
                                    val recipe = doc.toObject(Recipe::class.java).apply { recipeId = doc.id }
                                    val docCoupleId = doc.getString("coupleId") ?: recipe.coupleId
                                    val docPartnerId = doc.getString("partnerId")
                                    val docAltId = doc.getString("couple_id")
                                    val docAuthorId = recipe.authorId ?: doc.getString("authorId")

                                    val matchesCouple = listOf(docCoupleId, docPartnerId, docAltId)
                                        .filterNotNull()
                                        .any { candidate ->
                                            candidate == wantedId || normalizeCoupleId(candidate) == wantedIdNormalized
                                        }

                                    val matchesOwner = ownerId != null && docAuthorId == ownerId
                                    if (matchesCouple || (docCoupleId == null && docPartnerId == null && docAltId == null && matchesOwner)) {
                                        recipe
                                    } else {
                                        null
                                    }
                                }
                                recipes = filtered
                            }
                        }
                    onDispose {
                        recipeListener?.remove()
                        recipeListener = null
                    }
                }

                RecipeListScreen(
                    recipes = recipes,
                    isDarkTheme = isDark,
                    onRecipeClick = { recipe -> activityRef?.openRecipeDetailDialog(recipe) },
                    onAddRecipeClick = { activityRef?.openAddRecipeDialog() }
                )
            }
        }
    }

    private fun normalizeCoupleId(value: String?): String? {
        return value
            ?.lowercase()
            ?.replace("á", "a")
            ?.replace("é", "e")
            ?.replace("í", "i")
            ?.replace("ó", "o")
            ?.replace("ú", "u")
            ?.replace("ñ", "n")
    }
}
