package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.Recipe
import calendario.kevshupp.diariokevinali.RecipeVariant
import coil.compose.AsyncImage

@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    currentUserId: String?,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    currentUserName: String? = null,
    onSaveVariants: (List<RecipeVariant>) -> Unit = {}
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val secondaryTextColor = if (isDark) Color(0xFFFF4081) else Color(0xFF8B4513)
    val sectionTitleColor = if (isDark) Color(0xFFFF4081) else Color(0xFFE2725B)
    val boxBackground = if (isDark) Color(0xFF282828) else Color(0xFFFFFBEA)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)

    val isAuthor = currentUserId == recipe.authorId

    // 0 = receta principal; 1..n = variaciones
    var selectedTab by remember(recipe.recipeId) { mutableIntStateOf(0) }
    if (selectedTab > recipe.variants.size) selectedTab = 0
    val variant = if (selectedTab > 0) recipe.variants[selectedTab - 1] else null
    // null = cerrado; -1 = nueva variación; i = editar variación i
    var editingVariant by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteVariant by remember { mutableStateOf(false) }

    RecipeVariantsDialogs(
        recipe = recipe,
        editingVariant = editingVariant,
        confirmDelete = confirmDeleteVariant,
        selectedTab = selectedTab,
        isDark = isDark,
        currentUserName = currentUserName,
        onSaveVariants = onSaveVariants,
        onCloseEditor = { editingVariant = null },
        onCloseDelete = { confirmDeleteVariant = false },
        onSelectTab = { selectedTab = it }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(3.dp, borderColor),
            color = backgroundColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Fila Superior con Botón Cerrar Rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receta de Amor 📖",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✕",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Título de la Receta
                Text(
                    text = recipe.title ?: "Sin título",
                    fontFamily = Vt323,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 34.sp
                )

                Text(
                    text = "Creada por: ${recipe.authorName ?: "Anónimo"}",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Pestañas: principal + variaciones
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = if (selected) Color.White else textColor,
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .background(if (selected) sectionTitleColor else boxBackground)
                                .clickable { selectedTab = i }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "➕ Variación",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = sectionTitleColor,
                        modifier = Modifier
                            .border(2.dp, sectionTitleColor)
                            .clickable { editingVariant = -1 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (variant != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = variant.title?.takeIf { it.isNotBlank() } ?: "Variación $selectedTab",
                        fontFamily = Vt323,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = sectionTitleColor,
                        lineHeight = 28.sp
                    )
                    if (!variant.authorName.isNullOrBlank()) {
                        Text(
                            text = "Variación de: ${variant.authorName}",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = secondaryTextColor
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("✏️ Editar variación", fontFamily = Vt323, fontSize = 18.sp, color = secondaryTextColor,
                            modifier = Modifier.clickable { editingVariant = selectedTab - 1 })
                        Text("🗑️ Quitar", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFFD32F2F),
                            modifier = Modifier.clickable { confirmDeleteVariant = true })
                    }
                }

                // Imagen de la comida
                if (!recipe.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(3.dp, borderColor)
                            .background(boxBackground)
                            .padding(3.dp)
                    ) {
                        AsyncImage(
                            model = recipe.imageUrl.optimizeCloudinary(1000),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Ingredientes
                SectionHeader("✨ Ingredientes ✨", sectionTitleColor)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Sombra 3D
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(borderColor)
                    )
                    // Caja de Texto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor)
                            .background(boxBackground)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = (if (variant != null) variant.ingredients else recipe.ingredients)?.takeIf { it.isNotBlank() } ?: "No especificados",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            color = textColor,
                            lineHeight = 24.sp
                        )
                    }
                }

                // Pasos de Preparación
                SectionHeader("👨‍🍳 Pasos de Preparación 👨‍🍳", sectionTitleColor)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Sombra 3D
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(borderColor)
                    )
                    // Caja de Texto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor)
                            .background(boxBackground)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = (if (variant != null) variant.steps else recipe.steps)?.takeIf { it.isNotBlank() } ?: "No especificados",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            color = textColor,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Fila de Botones 3D de Acción
                if (isAuthor) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón Editar 3D
                        val editBtnBg = if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clickable { onEdit() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .offset(y = 6.dp)
                                    .background(borderColor)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(2.dp, borderColor)
                                    .background(editBtnBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("EDITAR", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Botón Eliminar 3D
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clickable { onDelete() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .offset(y = 6.dp)
                                    .background(borderColor)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(2.dp, borderColor)
                                    .background(Color(0xFFD32F2F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ELIMINAR", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Botón Cerrar 3D (siempre visible)
                val closeBtnBg = if (isDark) Color(0xFF4A148C) else Color(0xFF5D2E7A)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable { onDismiss() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(3.dp, borderColor)
                            .background(closeBtnBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CERRAR DETALLE", fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeVariantsDialogs(
    recipe: Recipe,
    editingVariant: Int?,
    confirmDelete: Boolean,
    selectedTab: Int,
    isDark: Boolean,
    currentUserName: String?,
    onSaveVariants: (List<RecipeVariant>) -> Unit,
    onCloseEditor: () -> Unit,
    onCloseDelete: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val accent = if (isDark) Color(0xFFFF4081) else Color(0xFFE2725B)

    if (editingVariant != null) {
        val existing = recipe.variants.getOrNull(editingVariant)
        var title by remember(editingVariant) {
            mutableStateOf(existing?.title ?: "${recipe.title ?: ""} ")
        }
        // Una variación nueva parte con los datos de la principal para solo cambiar lo distinto
        var ingredients by remember(editingVariant) { mutableStateOf(existing?.ingredients ?: recipe.ingredients ?: "") }
        var steps by remember(editingVariant) { mutableStateOf(existing?.steps ?: recipe.steps ?: "") }

        Dialog(onDismissRequest = onCloseEditor, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .border(3.dp, borderColor),
                color = backgroundColor,
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = if (existing == null) "🔀 Nueva variación" else "🔀 Editar variación",
                        fontFamily = Vt323, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = accent, unfocusedBorderColor = borderColor,
                            focusedLabelColor = accent, unfocusedLabelColor = textColor
                        )
                        OutlinedTextField(
                            value = title, onValueChange = { title = it },
                            label = { Text("Nombre (ej: con la mitad de azúcar)", fontFamily = Vt323) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 20.sp),
                            shape = RectangleShape, colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = ingredients, onValueChange = { ingredients = it },
                            label = { Text("✨ Ingredientes", fontFamily = Vt323) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 20.sp),
                            shape = RectangleShape, colors = fieldColors, minLines = 4,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = steps, onValueChange = { steps = it },
                            label = { Text("👨‍🍳 Pasos de preparación", fontFamily = Vt323) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 20.sp),
                            shape = RectangleShape, colors = fieldColors, minLines = 4,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCloseEditor, shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                            modifier = Modifier.weight(1f)
                        ) { Text("CANCELAR", fontFamily = Vt323, fontSize = 20.sp, color = Color.White) }
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
                                onCloseEditor()
                            },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                            modifier = Modifier.weight(1f)
                        ) { Text("GUARDAR", fontFamily = Vt323, fontSize = 20.sp, color = Color.White) }
                    }
                }
            }
        }
    }

    if (confirmDelete && selectedTab > 0) {
        val name = recipe.variants.getOrNull(selectedTab - 1)?.title ?: "esta variación"
        AlertDialog(
            onDismissRequest = onCloseDelete,
            shape = RectangleShape,
            title = { Text("¿Quitar variación?", fontFamily = Vt323, fontSize = 24.sp) },
            text = { Text("Se quitará \"$name\". La receta principal no cambia.", fontFamily = Vt323, fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onSaveVariants(recipe.variants.filterIndexed { i, _ -> i != selectedTab - 1 })
                    onSelectTab(0)
                    onCloseDelete()
                }) { Text("QUITAR", fontFamily = Vt323, fontSize = 20.sp, color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = onCloseDelete) { Text("CANCELAR", fontFamily = Vt323, fontSize = 20.sp) }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = Vt323,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 4.dp),
        textAlign = TextAlign.Center
    )
}
