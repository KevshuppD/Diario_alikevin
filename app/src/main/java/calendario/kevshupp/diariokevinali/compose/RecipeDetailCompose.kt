package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    currentUserId: String?,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val secondaryTextColor = if (isDark) Color(0xFFFF4081) else Color(0xFF8B4513)
    val sectionTitleColor = if (isDark) Color(0xFFFF4081) else Color(0xFFE2725B)
    val boxBackground = if (isDark) Color(0xFF282828) else Color(0xFFFFFBEA)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)

    val isAuthor = currentUserId == recipe.authorId

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
                            text = recipe.ingredients ?: "No especificados",
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
                            text = recipe.steps ?: "No especificados",
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
