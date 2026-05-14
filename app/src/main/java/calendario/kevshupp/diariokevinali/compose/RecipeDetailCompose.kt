package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage

@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val secondaryTextColor = if (isDark) Color.LightGray else Color(0xFF8B4513)
    val accentColor = Color(0xFFFF80AB) // Rosa pixelado
    val sectionTitleColor = if (isDark) Color(0xFFFF80AB) else Color(0xFF5D2E7A)
    val boxBackground = if (isDark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF).copy(alpha = 0.5f)
    val borderColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4A2511)

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
                    .padding(16.dp)
            ) {
                // Título
                Text(
                    text = recipe.title ?: "Sin título",
                    fontFamily = Vt323,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 34.sp
                )

                Text(
                    text = "Por: ${recipe.authorName ?: "Anónimo"}",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Imagen si existe
                if (!recipe.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(2.dp, borderColor)
                            .background(boxBackground)
                            .padding(4.dp)
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
                SectionHeader("Ingredientes", sectionTitleColor)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(2.dp, borderColor)
                        .background(boxBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = recipe.ingredients ?: "No especificados",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = textColor,
                        lineHeight = 24.sp
                    )
                }

                // Pasos
                SectionHeader("Pasos de Preparación", sectionTitleColor)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(2.dp, borderColor)
                        .background(boxBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = recipe.steps ?: "No especificados",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = textColor,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF00796B) else Color(0xFF00897B),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Editar", fontFamily = Vt323, fontSize = 22.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFF5D2E7A),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cerrar", fontFamily = Vt323, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = Vt323,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        textAlign = TextAlign.Center
    )
}
