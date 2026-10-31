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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val activityRef = activity as? MainActivity
                val prefs = activityRef?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                
                var recipes by remember { mutableStateOf(listOf<Recipe>()) }
                var selectedRecipeId by remember { mutableStateOf<String?>(null) }
                // Se busca en la lista viva para que las variaciones guardadas aparezcan al instante
                val selectedRecipe = recipes.firstOrNull { it.recipeId == selectedRecipeId }
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
                                        // Una receta mal formada no debe tumbar el recetario completo
                                        try {
                                            doc.toObject(Recipe::class.java).apply { recipeId = doc.id }
                                        } catch (e: Exception) {
                                            android.util.Log.e("RecipeFragment", "Receta ${doc.id} no se pudo leer: ${e.message}")
                                            null
                                        }
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
                        currentUserId = currentUserId,
                        onEdit = {
                            selectedRecipeId = null
                            activityRef?.recipeManager?.showAddRecipeDialog(recipe)
                        },
                        onDelete = {
                            val id = recipe.recipeId
                            if (id != null) {
                                db.collection("recipes").document(id).delete()
                                    .addOnSuccessListener {
                                        android.widget.Toast.makeText(context, "Receta eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                        selectedRecipeId = null
                                    }
                                    .addOnFailureListener {
                                        android.widget.Toast.makeText(context, "Error al eliminar", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        onDismiss = { selectedRecipeId = null },
                        currentUserName = prefs?.getString("userName", null),
                        onSaveVariants = { variants ->
                            val id = recipe.recipeId ?: return@RecipeDetailDialog
                            db.collection("recipes").document(id)
                                .update("variants", variants)
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "Variación guardada 🔀", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    android.widget.Toast.makeText(context, "Error al guardar la variación", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }

                RecipeListScreen(
                    recipes = recipes,
                    theme = theme,
                    onRecipeClick = { recipe -> selectedRecipeId = recipe.recipeId },
                    onAddRecipeClick = { activityRef?.openAddRecipeDialog() }
                )
            }
        }
    }
}
