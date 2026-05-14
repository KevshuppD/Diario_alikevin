package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.R
import calendario.kevshupp.diariokevinali.Recipe
import coil.compose.AsyncImage

val Vt323 = FontFamily(Font(R.font.vt323))

@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    isDarkTheme: Boolean,
    onRecipeClick: (Recipe) -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val titleColor = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF4A2511)
    val subtitleColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF8B4513)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Recetario",
            fontFamily = Vt323,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Text(
            text = "Nuestras recetas compartidas",
            fontFamily = Vt323,
            fontSize = 20.sp,
            color = subtitleColor,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recipes) { recipe ->
                RecipeItem(recipe, isDarkTheme, onRecipeClick)
            }
        }

        Button(
            onClick = onAddRecipeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B4513),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Agregar Receta",
                fontFamily = Vt323,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun RecipeItem(recipe: Recipe, isDarkTheme: Boolean, onClick: (Recipe) -> Unit) {
    val cardBg = if (isDarkTheme) Color(0xFF3D3D3D) else Color(0xFFF5E6BE)
    val textColor = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF4A2511)
    val secondaryTextColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF8B4513)
    val borderColor = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF4A2511)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 3.dp, color = borderColor)
            .background(cardBg)
            .clickable { onClick(recipe) }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = recipe.title ?: "Sin título",
                fontFamily = Vt323,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 26.sp
            )
            
            Text(
                text = "Por: ${recipe.authorName ?: "Anónimo"}",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = secondaryTextColor,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (!recipe.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(top = 8.dp)
                        .border(1.dp, borderColor),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = recipe.ingredients ?: "",
                fontFamily = Vt323,
                fontSize = 19.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
                lineHeight = 20.sp
            )
        }
    }
}
