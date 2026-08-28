package calendario.kevshupp.diariokevinali

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import coil.load
import com.google.firebase.firestore.FirebaseFirestore

class RecipeManager(
    private val context: Context,
    private val coupleId: String,
    private val userId: String,
    private val userName: String,
    private val onPickImage: Runnable
) {
    private val db = FirebaseFirestore.getInstance()
    private var currentTheme: String = ""
    private var pendingImageUrl: String? = null
    private var previewContainer: FrameLayout? = null
    private var previewImage: ImageView? = null
    private var removeImageButton: ImageButton? = null

    fun setTheme(theme: String) {
        this.currentTheme = theme
    }

    fun setImageUrl(url: String) {
        pendingImageUrl = url
        previewContainer?.visibility = View.VISIBLE
        previewImage?.let { imageView ->
            imageView.load(url) { crossfade(true) }
        }
        removeImageButton?.visibility = View.VISIBLE
    }

    fun addRecipe(recipe: Recipe) {
        db.collection("recipes").add(recipe)
            .addOnSuccessListener {
                Toast.makeText(context, "Receta guardada", Toast.LENGTH_SHORT).show()
                (context as? MainActivity)?.sendNotificationV1("¡Nueva receta! 🍳", "$userName agregó la receta: \"${recipe.title}\"", null, "receta")
            }
    }

    fun showAddRecipeDialog(existing: Recipe? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_recipe, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()

        val titleView = view.findViewById<TextView>(R.id.tvRecipeEditorTitle)
        val etTitle = view.findViewById<EditText>(R.id.etRecipeTitle)
        val etIngredients = view.findViewById<EditText>(R.id.etRecipeIngredients)
        val etSteps = view.findViewById<EditText>(R.id.etRecipeSteps)
        val btnAddImage = view.findViewById<Button>(R.id.btnRecipeAddImage)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelRecipe)
        val btnSave = view.findViewById<Button>(R.id.btnSaveRecipe)
        previewContainer = view.findViewById(R.id.recipePreviewContainer)
        previewImage = view.findViewById(R.id.ivRecipePreview)
        removeImageButton = view.findViewById(R.id.btnRemoveRecipeImage)

        if (currentTheme == "Pixel Oscuro") {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            titleView.setTextColor(Color.WHITE)
            etTitle.setTextColor(Color.WHITE)
            etTitle.setHintTextColor(Color.LTGRAY)
            etTitle.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            etIngredients.setTextColor(Color.WHITE)
            etIngredients.setHintTextColor(Color.LTGRAY)
            etIngredients.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            etSteps.setTextColor(Color.WHITE)
            etSteps.setHintTextColor(Color.LTGRAY)
            etSteps.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            btnAddImage.setTextColor(Color.WHITE)
            btnCancel.setTextColor(Color.WHITE)
            btnSave.setTextColor(Color.WHITE)
        }

        if (existing != null && existing.authorId != userId) {
            Toast.makeText(context, "No puedes editar recetas de otros", Toast.LENGTH_SHORT).show()
            return
        }

        if (existing != null) {
            titleView.text = "Editar receta"
            etTitle.setText(existing.title ?: "")
            etIngredients.setText(existing.ingredients ?: "")
            etSteps.setText(existing.steps ?: "")
            pendingImageUrl = existing.imageUrl
            if (!pendingImageUrl.isNullOrEmpty()) {
                previewContainer?.visibility = View.VISIBLE
                previewImage?.let { imageView ->
                    imageView.load(pendingImageUrl) { crossfade(true) }
                }
            }
        } else {
            titleView.text = "Nueva receta"
            pendingImageUrl = null
            previewContainer?.visibility = View.GONE
        }

        btnAddImage.setOnClickListener { onPickImage.run() }
        removeImageButton?.setOnClickListener {
            pendingImageUrl = null
            previewContainer?.visibility = View.GONE
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) return@setOnClickListener

            val recipe = Recipe(
                recipeId = existing?.recipeId,
                coupleId = coupleId,
                title = title,
                ingredients = etIngredients.text.toString().trim(),
                steps = etSteps.text.toString().trim(),
                imageUrl = pendingImageUrl,
                authorId = existing?.authorId ?: userId,
                authorName = existing?.authorName ?: userName,
                timestamp = existing?.timestamp ?: System.currentTimeMillis()
            )

            if (recipe.recipeId != null) {
                db.collection("recipes").document(recipe.recipeId!!).set(recipe)
                    .addOnSuccessListener { 
                        Toast.makeText(context, "Receta actualizada", Toast.LENGTH_SHORT).show()
                        (context as? MainActivity)?.sendNotificationV1("¡Receta modificada! 🍳", "$userName actualizó la receta: \"${recipe.title}\"", null, "receta")
                        dialog.dismiss() 
                    }
            } else {
                addRecipe(recipe)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun showRecipeDetailDialog(recipe: Recipe) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_detail, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()

        val title = view.findViewById<TextView>(R.id.tvRecipeDetailTitle)
        val author = view.findViewById<TextView>(R.id.tvRecipeDetailAuthor)
        val ingredients = view.findViewById<TextView>(R.id.tvRecipeDetailIngredients)
        val steps = view.findViewById<TextView>(R.id.tvRecipeDetailSteps)
        val imageContainer = view.findViewById<View>(R.id.recipeImageContainer)
        val imageView = view.findViewById<ImageView>(R.id.ivRecipeDetailImage)
        val btnClose = view.findViewById<Button>(R.id.btnCloseRecipeDetail)

        title.text = recipe.title ?: ""
        author.text = "Por: ${recipe.authorName ?: "Anónimo"}"
        ingredients.text = recipe.ingredients ?: ""
        steps.text = recipe.steps ?: ""

        if (!recipe.imageUrl.isNullOrEmpty()) {
            imageContainer.visibility = View.VISIBLE
            imageView.load(recipe.imageUrl) { crossfade(true) }
        } else {
            imageContainer.visibility = View.GONE
        }

        if (currentTheme == "Pixel Oscuro") {
            view.setBackgroundResource(R.drawable.bg_parchment_pixel_dark)
            title.setTextColor(Color.WHITE)
            author.setTextColor(Color.LTGRAY)
            ingredients.setTextColor(Color.WHITE)
            ingredients.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            steps.setTextColor(Color.WHITE)
            steps.setBackgroundResource(R.drawable.bg_message_pixel_dark)
            
            // Ajustar colores de títulos de sección que eran difíciles de leer
            view.findViewById<TextView>(R.id.tvIngredientsTitle).setTextColor(Color.parseColor("#FF80AB")) // Rosa claro pixel
            view.findViewById<TextView>(R.id.tvStepsTitle).setTextColor(Color.parseColor("#FF80AB"))
            
            btnClose.setTextColor(Color.WHITE)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
