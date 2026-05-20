package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.Message
import coil.compose.AsyncImage
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@Composable
fun MessageEditorDialog(
    initialMessage: Message?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, imageUrl: String?) -> Unit,
    onPickImage: () -> Unit,
    currentSelectedImageUrl: String? = null
) {
    var title by remember { mutableStateOf(initialMessage?.title ?: "") }
    var contentValue by remember { 
        val initialText = initialMessage?.content ?: ""
        mutableStateOf(TextFieldValue(initialText))
    }
    var imageUrl by remember { mutableStateOf(initialMessage?.imageUrl ?: currentSelectedImageUrl) }

    // Función para aplicar formato al texto seleccionado
    fun applyFormat(tagStart: String, tagEnd: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.substring(0, selection.start) + tagStart + selectedText + tagEnd + text.substring(selection.end)
        
        val newCursorPos = if (selection.collapsed) selection.start + tagStart.length else selection.start + tagStart.length + selectedText.length + tagEnd.length
        
        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }

    // Actualizar imageUrl si cambia externamente
    LaunchedEffect(currentSelectedImageUrl) {
        if (currentSelectedImageUrl != null) {
            imageUrl = currentSelectedImageUrl
        }
    }

    val backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val inputBackground = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFFFFFF).copy(alpha = 0.6f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .border(3.dp, borderColor),
            color = backgroundColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (initialMessage == null) "Escribir Carta" else "Editar Carta",
                    fontFamily = Vt323,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // Campo de Título
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Título...", fontFamily = Vt323, fontSize = 22.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = borderColor,
                            unfocusedIndicatorColor = borderColor.copy(alpha = 0.5f),
                            cursorColor = borderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de Mensaje
                    OutlinedTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        placeholder = { Text("Querida Ali...", fontFamily = Vt323, fontSize = 20.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 250.dp),
                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 20.sp, color = textColor),
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBackground,
                            unfocusedContainerColor = inputBackground,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor.copy(alpha = 0.7f),
                            cursorColor = borderColor
                        )
                    )

                    // Previsualización de Imagen
                    if (!imageUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(2.dp, borderColor)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUrl = null },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White)
                            }
                        }
                    }
                }

                // Barra de herramientas (Ubicación estratégica abajo para evitar menú flotante)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatButton("B", isDark) { applyFormat("<b>", "</b>") }
                    FormatButton("I", isDark) { applyFormat("<i>", "</i>") }
                    FormatButton("Color", isDark) { applyFormat("<font color='#FF4081'>", "</font>") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickImage,
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D2E7A))
                    ) {
                        Text("Imagen", fontFamily = Vt323, fontSize = 18.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
                    ) {
                        Text("Cancelar", fontFamily = Vt323, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSave(title, contentValue.text, imageUrl) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D2E7A)),
                    enabled = contentValue.text.isNotBlank()
                ) {
                    Text(if (initialMessage == null) "Enviar Carta" else "Guardar Cambios", fontFamily = Vt323, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun FormatButton(text: String, isDark: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFF5D2E7A),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(text = text, fontFamily = Vt323, fontSize = 16.sp, fontWeight = if (text == "B") FontWeight.Bold else FontWeight.Normal)
    }
}
