package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import calendario.kevshupp.diariokevinali.Recipe
import calendario.kevshupp.diariokevinali.RecipeVariant
import coil.compose.AsyncImage

// Cuaderno de cocina: la receta es una hoja de pergamino, igual que las cartas del feed
private val PAPER = Color(0xFFF3E5AB)
private val INK = Color(0xFF4A2511)
private val INK_SOFT = Color(0xFF8B4513)
private val ACCENT = Color(0xFFA61E4D)
private val ACTIVE_TAB = Color(0xFFD97706)
private val DANGER = Color(0xFFC62828)
private const val CHECKS_PREFS = "RecipeChecks"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    currentUserId: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    currentUserName: String? = null,
    onSaveVariants: (List<RecipeVariant>) -> Unit = {}
) {
    val isAuthor = currentUserId == recipe.authorId

    // 0 = receta principal; 1..n = variaciones
    var selectedTab by remember(recipe.recipeId) { mutableIntStateOf(0) }
    if (selectedTab > recipe.variants.size) selectedTab = 0
    val variant = if (selectedTab > 0) recipe.variants[selectedTab - 1] else null
    // null = cerrado; -1 = nueva variación; i = editar variación i
    var editingVariant by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteVariant by remember { mutableStateOf(false) }
    var confirmDeleteRecipe by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (editingVariant != null) editingVariant = null else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize().border(3.dp, INK),
            color = PAPER,
            shape = RectangleShape
        ) {
            val editing = editingVariant
            if (editing != null) {
                VariantForm(
                    recipe = recipe,
                    editingVariant = editing,
                    currentUserName = currentUserName,
                    onSaveVariants = onSaveVariants,
                    onClose = { editingVariant = null },
                    onSelectTab = { selectedTab = it }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "←",
                            fontFamily = Vt323,
                            fontSize = 32.sp,
                            color = INK,
                            modifier = Modifier.clickable { onDismiss() }.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Receta de Amor 📖", fontFamily = Vt323, fontSize = 18.sp, color = ACCENT, fontWeight = FontWeight.Bold)
                            Text(recipe.title ?: "Sin título", fontFamily = Vt323, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = INK, lineHeight = 32.sp)
                            Text("Creada por: ${recipe.authorName ?: "Anónimo"}", fontFamily = Vt323, fontSize = 18.sp, color = INK_SOFT)
                        }
                    }

                    // Pestañas: principal + variaciones; bajan de línea para que ➕ siempre se vea
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf("⭐ Principal") + recipe.variants.mapIndexed { i, v ->
                            "🔀 " + (v.title?.takeIf { it.isNotBlank() } ?: "Variación ${i + 1}")
                        }
                        tabs.forEachIndexed { i, label ->
                            val selected = i == selectedTab
                            Text(
                                text = label,
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else INK,
                                modifier = Modifier
                                    .border(2.dp, INK)
                                    .background(if (selected) ACTIVE_TAB else Color(0xFFFFFBEA))
                                    .clickable { selectedTab = i }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Text(
                            text = "➕ Variación",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ACCENT,
                            modifier = Modifier
                                .border(2.dp, ACCENT)
                                .clickable { editingVariant = -1 }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (variant != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = variant.title?.takeIf { it.isNotBlank() } ?: "Variación $selectedTab",
                            fontFamily = Vt323,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ACCENT,
                            lineHeight = 26.sp
                        )
                        if (!variant.authorName.isNullOrBlank()) {
                            Text("Variación de: ${variant.authorName}", fontFamily = Vt323, fontSize = 18.sp, color = INK_SOFT)
                        }
                        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("✏️ Editar variación", fontFamily = Vt323, fontSize = 18.sp, color = INK,
                                modifier = Modifier.clickable { editingVariant = selectedTab - 1 })
                            Text("🗑️ Quitar", fontFamily = Vt323, fontSize = 18.sp, color = DANGER,
                                modifier = Modifier.clickable { confirmDeleteVariant = true })
                        }
                    }

                    if (!recipe.imageUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        AsyncImage(
                            model = recipe.imageUrl.optimizeCloudinary(1000),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(220.dp).border(3.dp, INK),
                            contentScale = ContentScale.Crop
                        )
                    }

                    RecipeSectionTitle("✨ INGREDIENTES")
                    IngredientChecklist(
                        recipeKey = "${recipe.recipeId}_$selectedTab",
                        ingredients = if (variant != null) variant.ingredients else recipe.ingredients
                    )

                    RecipeSectionTitle("👨‍🍳 PASOS DE PREPARACIÓN")
                    val steps = (if (variant != null) variant.steps else recipe.steps)?.takeIf { it.isNotBlank() }
                    if (steps != null) {
                        Text(steps, fontFamily = Vt323, fontSize = 20.sp, color = INK, lineHeight = 24.sp)
                    } else {
                        Text("Aún no hay pasos.", fontFamily = Vt323, fontSize = 20.sp, color = INK_SOFT)
                        if (variant != null || isAuthor) {
                            Text(
                                text = "Agregar pasos",
                                fontFamily = Vt323,
                                fontSize = 20.sp,
                                color = INK,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable { if (variant != null) editingVariant = selectedTab - 1 else onEdit() }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }

                    if (isAuthor) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RecipeAction("✏️ EDITAR", INK, Modifier.weight(1f)) { onEdit() }
                            RecipeAction("🗑️ ELIMINAR", DANGER, Modifier.weight(1f)) { confirmDeleteRecipe = true }
                        }
                    }
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }

        if (confirmDeleteVariant && selectedTab > 0) {
            val name = recipe.variants.getOrNull(selectedTab - 1)?.title ?: "esta variación"
            RecipeConfirmDialog(
                title = "¿Quitar variación?",
                message = "Se quitará \"$name\". La receta principal no cambia.",
                confirmLabel = "QUITAR",
                onConfirm = {
                    onSaveVariants(recipe.variants.filterIndexed { i, _ -> i != selectedTab - 1 })
                    selectedTab = 0
                    confirmDeleteVariant = false
                },
                onDismiss = { confirmDeleteVariant = false }
            )
        }
        if (confirmDeleteRecipe) {
            RecipeConfirmDialog(
                title = "¿Eliminar receta?",
                message = "Se eliminará \"${recipe.title ?: "esta receta"}\" con todas sus variaciones. No se puede deshacer.",
                confirmLabel = "ELIMINAR",
                onConfirm = {
                    confirmDeleteRecipe = false
                    onDelete()
                },
                onDismiss = { confirmDeleteRecipe = false }
            )
        }
    }
}

/** Una línea por ingrediente, para marcar al cocinar. Las marcas se guardan solo en este teléfono. */
@Composable
private fun IngredientChecklist(recipeKey: String, ingredients: String?) {
    val lines = remember(ingredients) { ingredients.orEmpty().lines().filter { it.isNotBlank() } }
    if (lines.isEmpty()) {
        Text("No especificados", fontFamily = Vt323, fontSize = 20.sp, color = INK_SOFT)
        return
    }
    val prefs = LocalContext.current.getSharedPreferences(CHECKS_PREFS, Context.MODE_PRIVATE)
    var checked by remember(recipeKey, ingredients) {
        mutableStateOf(prefs.getStringSet(recipeKey, emptySet()).orEmpty().filter { it in lines }.toSet())
    }
    fun save(new: Set<String>) {
        checked = new
        prefs.edit().putStringSet(recipeKey, new).apply()
    }
    lines.forEach { line ->
        val isChecked = line in checked
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { save(if (isChecked) checked - line else checked + line) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { save(if (it) checked + line else checked - line) },
                colors = CheckboxDefaults.colors(checkedColor = ACTIVE_TAB, uncheckedColor = INK)
            )
            Text(
                text = line,
                fontFamily = Vt323,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                color = if (isChecked) INK_SOFT else INK,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
        }
    }
    if (checked.isNotEmpty()) {
        Text(
            text = "Desmarcar todo",
            fontFamily = Vt323,
            fontSize = 17.sp,
            color = INK,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { save(emptySet()) }.padding(vertical = 6.dp)
        )
    }
}

/** Formulario de variación dentro de la misma hoja, sin abrir otra ventana. */
@Composable
private fun VariantForm(
    recipe: Recipe,
    editingVariant: Int,
    currentUserName: String?,
    onSaveVariants: (List<RecipeVariant>) -> Unit,
    onClose: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    val existing = recipe.variants.getOrNull(editingVariant)
    var title by remember(editingVariant) { mutableStateOf(existing?.title ?: "${recipe.title ?: ""} ") }
    // Una variación nueva parte con los datos de la principal para solo cambiar lo distinto
    var ingredients by remember(editingVariant) { mutableStateOf(existing?.ingredients ?: recipe.ingredients ?: "") }
    var steps by remember(editingVariant) { mutableStateOf(existing?.steps ?: recipe.steps ?: "") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = INK, unfocusedTextColor = INK,
        focusedBorderColor = ACCENT, unfocusedBorderColor = INK,
        focusedLabelColor = ACCENT, unfocusedLabelColor = INK,
        cursorColor = INK
    )
    val fieldStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 20.sp)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontFamily = Vt323, fontSize = 32.sp, color = INK, modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
            Text(
                text = if (existing == null) "🔀 Nueva variación" else "🔀 Editar variación",
                fontFamily = Vt323, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ACCENT
            )
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Nombre (ej: con la mitad de azúcar)", fontFamily = Vt323) },
                textStyle = fieldStyle, shape = RectangleShape, colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = ingredients, onValueChange = { ingredients = it },
                label = { Text("✨ Ingredientes", fontFamily = Vt323) },
                textStyle = fieldStyle, shape = RectangleShape, colors = fieldColors, minLines = 4,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = steps, onValueChange = { steps = it },
                label = { Text("👨‍🍳 Pasos de preparación", fontFamily = Vt323) },
                textStyle = fieldStyle, shape = RectangleShape, colors = fieldColors, minLines = 6,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecipeAction("CANCELAR", INK, Modifier.weight(1f)) { onClose() }
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val edited = RecipeVariant(
                        title = title.trim(),
                        ingredients = ingredients.trim(),
                        steps = steps.trim(),
                        authorName = existing?.authorName ?: currentUserName
                    )
                    val list = recipe.variants.toMutableList()
                    if (existing == null) list.add(edited) else list[editingVariant] = edited
                    onSaveVariants(list)
                    onSelectTab(if (existing == null) list.size else editingVariant + 1)
                    onClose()
                },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("GUARDAR", fontFamily = Vt323, fontSize = 20.sp, color = Color.White) }
        }
    }
}

@Composable
private fun RecipeConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text(title, fontFamily = Vt323, fontSize = 24.sp) },
        text = { Text(message, fontFamily = Vt323, fontSize = 20.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, fontFamily = Vt323, fontSize = 20.sp, color = DANGER) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", fontFamily = Vt323, fontSize = 20.sp) }
        }
    )
}

@Composable
private fun RecipeAction(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(48.dp).border(2.dp, color).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun RecipeSectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = Vt323,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = ACCENT,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
    )
}
