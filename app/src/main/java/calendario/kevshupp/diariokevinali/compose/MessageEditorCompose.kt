package calendario.kevshupp.diariokevinali.compose

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.R
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
    var activeTab by remember { mutableStateOf(0) } // 0 = Editar, 1 = Vista Previa
    
    var showColors by remember { mutableStateOf(false) }
    var showSizes by remember { mutableStateOf(false) }

    // Función para aplicar formato al texto seleccionado o insertar en la posición actual
    fun applyFormat(tagStart: String, tagEnd: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val start = Math.max(0, Math.min(selection.start, text.length))
        val end = Math.max(0, Math.min(selection.end, text.length))
        
        val selectedText = text.substring(start, end)
        val newText = text.substring(0, start) + tagStart + selectedText + tagEnd + text.substring(end)
        
        val newCursorPos = if (start == end) start + tagStart.length else start + tagStart.length + selectedText.length + tagEnd.length
        
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

                // 🌟 Pestañas de Edición / Vista Previa
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) borderColor else inputBackground,
                            contentColor = if (activeTab == 0) backgroundColor else textColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
                    ) {
                        Text("Editar ✏️", fontFamily = Vt323, fontSize = 18.sp)
                    }

                    Button(
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) borderColor else inputBackground,
                            contentColor = if (activeTab == 1) backgroundColor else textColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
                    ) {
                        Text("Vista Previa 👁️", fontFamily = Vt323, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (activeTab == 0) {
                    // MODO EDICIÓN
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

                    // Barra de herramientas de formato
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FormatButton("B", isDark) { applyFormat("<b>", "</b>") }
                            FormatButton("I", isDark) { applyFormat("<i>", "</i>") }
                            FormatButton("U", isDark) { applyFormat("<u>", "</u>") }
                            FormatButton("S", isDark) { applyFormat("<s>", "</s>") }
                            
                            // Alignments
                            FormatButton("◀", isDark) { applyFormat("<p align=\"left\">", "</p>") }
                            FormatButton("◀▶", isDark) { applyFormat("<p align=\"center\">", "</p>") }
                            FormatButton("▶", isDark) { applyFormat("<p align=\"right\">", "</p>") }
                            
                            FormatButton("A±", isDark) { showSizes = !showSizes; showColors = false }
                            FormatButton("Color", isDark) { showColors = !showColors; showSizes = false }
                        }

                        // Selector horizontal de colores
                        if (showColors) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().background(inputBackground).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val colors = listOf(
                                    "#FF4081" to "Rosa",
                                    "#2196F3" to "Azul",
                                    "#4CAF50" to "Verde",
                                    "#FF9800" to "Naranja",
                                    "#F44336" to "Rojo",
                                    "#9C27B0" to "Morado",
                                    "#FFEB3B" to "Amarillo"
                                )
                                colors.forEach { (colorHex, _) ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                                            .border(2.dp, borderColor, CircleShape)
                                            .clickable {
                                                applyFormat("<font color='$colorHex'>", "</font>")
                                                showColors = false
                                            }
                                    )
                                }
                            }
                        }

                        // Selector horizontal de tamaños
                        if (showSizes) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().background(inputBackground).padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sizes = listOf(
                                    "2" to "Peq",
                                    "4" to "Med",
                                    "6" to "Gde",
                                    "7" to "Gig"
                                )
                                sizes.forEach { (sizeVal, label) ->
                                    Button(
                                        onClick = {
                                            applyFormat("<font size='$sizeVal'>", "</font>")
                                            showSizes = false
                                        },
                                        modifier = Modifier.height(30.dp),
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = borderColor, contentColor = backgroundColor),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(label, fontFamily = Vt323, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // MODO VISTA PREVIA INTERACTIVA
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                fontFamily = Vt323,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Contenedor de Vista Previa HTML Novedosa
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, borderColor)
                                .background(inputBackground)
                                .padding(12.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    TextView(ctx).apply {
                                        val typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                                        setTypeface(typeface)
                                        textSize = 20f
                                        setTextColor(textColor.toArgb())
                                    }
                                },
                                update = { textView ->
                                    textView.text = android.text.Html.fromHtml(
                                        contentValue.text,
                                        android.text.Html.FROM_HTML_MODE_LEGACY
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Imagen en vista previa
                        if (!imageUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .border(2.dp, borderColor),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
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
                        Text("Imagen 📸", fontFamily = Vt323, fontSize = 18.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
                    ) {
                        Text("Cancelar ❌", fontFamily = Vt323, fontSize = 18.sp)
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
                    Text(if (initialMessage == null) "Enviar Carta ✉" else "Guardar Cambios 💾", fontFamily = Vt323, fontSize = 20.sp)
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text = text, fontFamily = Vt323, fontSize = 16.sp, fontWeight = if (text == "B") FontWeight.Bold else FontWeight.Normal)
    }
}
