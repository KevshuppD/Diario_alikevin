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
                val activityRef = activity as? MainActivity
                val prefs = activityRef?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                
                var recipes by remember { mutableStateOf(listOf<Recipe>()) }
                var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
                var currentId by remember { 
                    mutableStateOf(coupleId ?: prefs?.getString("coupleId", "vínculo_único_123")) 
                }
                
                val isDark = (theme ?: prefs?.getString("theme", "Pixel Claro")) == "Pixel Oscuro"

                DisposableEffect(currentId) {
                    val finalId = currentId
                    recipeListener?.remove()
                    
                    if (!finalId.isNullOrEmpty()) {
                        android.util.Log.d("RecipeFragment", "Escuchando recetas para ID: $finalId")
                        recipeListener = db.collection("recipes")
                            .whereEqualTo("coupleId", finalId)
                            .addSnapshotListener { value, error ->
                                if (error != null) {
                                    android.util.Log.e("RecipeFragment", "Error cargando recetas: ${error.message}")
                                    return@addSnapshotListener
                                }
                                if (value != null) {
                                    val items = value.mapNotNull { doc ->
                                        doc.toObject(Recipe::class.java).apply { recipeId = doc.id }
                                    }.sortedByDescending { it.timestamp }
                                    android.util.Log.d("RecipeFragment", "Recetas cargadas: ${items.size}")
                                    recipes = items
                                }
                            }
                    } else {
                        android.util.Log.w("RecipeFragment", "coupleId es nulo o vacío, no se puede cargar el recetario")
                    }
                    
                    onDispose {
                        recipeListener?.remove()
                        recipeListener = null
                    }
                }

                // Mostrar el detalle si hay una receta seleccionada
                selectedRecipe?.let { recipe ->
                    calendario.kevshupp.diariokevinali.compose.RecipeDetailDialog(
                        recipe = recipe,
                        isDark = isDark,
                        onDismiss = { selectedRecipe = null }
                    )
                }

                RecipeListScreen(
                    recipes = recipes,
                    isDarkTheme = isDark,
                    onRecipeClick = { recipe -> selectedRecipe = recipe },
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
