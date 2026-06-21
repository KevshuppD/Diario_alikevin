package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.R
import calendario.kevshupp.diariokevinali.Recipe
import coil.compose.AsyncImage

val Vt323 = FontFamily(Font(R.font.vt323))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    theme: String,
    onRecipeClick: (Recipe) -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val backgroundColor = getAppBackgroundColor(theme)

    val titleColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val subtitleColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.DarkGray
        else -> Color(0xFF8B4513)
    }

    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val cardBg = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredRecipes = remember(recipes, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            recipes
        } else {
            recipes.filter { recipe ->
                (recipe.title?.contains(searchQuery, ignoreCase = true) == true) ||
                (recipe.ingredients?.contains(searchQuery, ignoreCase = true) == true) ||
                (recipe.authorName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera Retro Premium
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_recipe_pixel),
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Recetario Pixel",
                fontFamily = Vt323,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }

        Text(
            text = "🍳 Nuestras recetas secretas compartidas",
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = subtitleColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Barra de búsqueda con estilo Pixel 3D
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Sombra
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(borderColor)
            )
            // Entrada de texto
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Buscar por título o ingrediente...",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = subtitleColor.copy(alpha = 0.5f)
                    )
                },
                textStyle = TextStyle(
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = titleColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                    cursorColor = titleColor
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = borderColor,
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(8.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, borderColor)
            )
        }

        // Listado de Recetas
        if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (recipes.isEmpty()) {
                        "¡Vaya, el caldero está vacío! 🍲\nAgreguen vuestra primera receta juntos en el botón de abajo."
                    } else {
                        "No encontramos recetas que coincidan con la búsqueda 🔍"
                    },
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = TextStyle(lineHeight = 24.sp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = filteredRecipes,
                    key = { it.recipeId ?: "recipe_${it.timestamp}" }
                ) { recipe ->
                    RecipeItem(recipe, theme, onRecipeClick)
                }
            }
        }

        // Botón 3D Premium "Agregar Receta"
        val buttonBg = when {
            isDark -> Color(0xFF00796B)
            isMono -> Color.White
            else -> Color(0xFFE2725B)
        }
        val buttonTextColor = when {
            isDark -> Color.White
            isMono -> Color.Black
            else -> Color.White
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onAddRecipeClick() }
        ) {
            // Sombra del botón
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .offset(y = 6.dp)
                    .background(borderColor)
            )
            // Botón interactivo superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(3.dp, borderColor)
                    .background(buttonBg)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍳 NUEVA RECETA COMPARTE",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonTextColor
                )
            }
        }
    }
}

@Composable
fun RecipeItem(recipe: Recipe, theme: String, onClick: (Recipe) -> Unit) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val cardBg = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }
    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    val secondaryTextColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.DarkGray
        else -> Color(0xFF8B4513)
    }
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Efecto 3D de sombra pixelada
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(borderColor)
        )
        // Tarjeta Principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 3.dp, color = borderColor)
                .background(cardBg)
                .clickable { onClick(recipe) }
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.title ?: "Sin título",
                        fontFamily = Vt323,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 26.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "🍲",
                        fontSize = 24.sp
                    )
                }

                Text(
                    text = "Preparado por: ${recipe.authorName ?: "Anónimo"}",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (!recipe.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .border(2.dp, borderColor)
                            .padding(2.dp)
                    ) {
                        AsyncImage(
                            model = recipe.imageUrl.optimizeCloudinary(600),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vista previa elegante de los ingredientes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(borderColor.copy(alpha = 0.08f))
                        .border(1.dp, borderColor.copy(alpha = 0.25f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = recipe.ingredients ?: "Ingredientes no especificados",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
