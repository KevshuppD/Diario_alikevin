package calendario.kevshupp.diariokevinali.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun ProfileScreen(
    currentUserName: String,
    currentUserImageUri: String?,
    theme: String,
    coupleId: String?,
    currentUserId: String,
    onPickImage: () -> Unit,
    onSaveProfile: (String, String?, Long?) -> Unit,
    onLogout: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val backgroundColor = getAppBackgroundColor(theme)

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

    val pinkColor = Color(0xFFFF80AB)
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val boxBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    var partnerName by remember { mutableStateOf("Buscando...") }
    var partnerImageUrl by remember { mutableStateOf<String?>(null) }
    var isSearchingPartner by remember { mutableStateOf(true) }

    // Fecha de Aniversario Dinámica
    var anniversaryDate by remember { mutableStateOf(1643328000000L) } // Default 28 Ene 2022
    var showAnniversaryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        val prefs = context.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
        anniversaryDate = prefs.getLong("anniversaryDate", 1643328000000L)

        // Escuchar cambios en tiempo real
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    val firestoreDate = snapshot.getLong("anniversaryDate")
                    if (firestoreDate != null && firestoreDate > 0) {
                        anniversaryDate = firestoreDate
                        prefs.edit().putLong("anniversaryDate", firestoreDate).apply()
                    }
                }
            }
    }

    LaunchedEffect(coupleId) {
        if (!coupleId.isNullOrEmpty()) {
            isSearchingPartner = true
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener { snapshot, error ->
                    isSearchingPartner = false
                    if (error != null) {
                        partnerName = "Sin conexión"
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null || snapshot.isEmpty) {
                        partnerName = "Pareja no conectada"
                        return@addSnapshotListener
                    }

                    var found = false
                    snapshot.documents.forEach { doc ->
                        val docUserId = doc.getString("userId") ?: doc.id
                        if (docUserId != currentUserId) {
                            partnerName = doc.getString("userName") ?: "Pareja"
                            partnerImageUrl = doc.getString("profileImageUrl")
                            found = true
                        }
                    }
                    if (!found) {
                        partnerName = "Pareja no conectada"
                    }
                }
        } else {
            isSearchingPartner = false
            partnerName = "Sin vínculo"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título Retro
        Text(
            text = "Nuestro Perfil Pixel",
            fontFamily = Vt323,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 🌟 Marco de Perfil Doble Romántico (Kevin + Ali con corazón latiendo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tu Foto (Clickable para cambiar)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .border(3.dp, borderColor)
                        .background(boxBackground)
                        .clickable { onPickImage() }
                        .padding(5.dp)
                ) {
                    if (currentUserImageUri != null) {
                        AsyncImage(
                            model = currentUserImageUri,
                            contentDescription = "Tu perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                            Text("📷", fontSize = 32.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tú ✏️",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Corazón animado con latido
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Text(
                    text = "❤️",
                    fontSize = 36.sp,
                    modifier = Modifier.graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vínculo",
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    color = secondaryTextColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // Foto de tu Pareja
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .border(3.dp, borderColor)
                        .background(boxBackground)
                        .padding(5.dp)
                ) {
                    if (partnerImageUrl != null) {
                        AsyncImage(
                            model = partnerImageUrl,
                            contentDescription = "Pareja",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                            if (isSearchingPartner) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = pinkColor)
                            } else {
                                Text("?", fontSize = 32.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = partnerName,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🗓️ Historial de Relación editable en 3D
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            // Sombra
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(borderColor)
            )
            // Tarjeta principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, borderColor)
                    .background(boxBackground)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HISTORIA DE AMOR 📜",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = calculateTimeTogether(anniversaryDate),
                        fontFamily = Vt323,
                        fontSize = 22.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }
                
                // Botón editar fecha
                IconButton(
                    onClick = { showAnniversaryDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("✏️", fontSize = 22.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var nameText by remember { mutableStateOf(currentUserName) }

        // Nombre de Usuario Retro Campo
        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Tu nombre de usuario", fontFamily = Vt323, fontSize = 16.sp) },
            textStyle = TextStyle(fontFamily = Vt323, fontSize = 24.sp, color = textColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = boxBackground,
                unfocusedContainerColor = boxBackground,
                focusedBorderColor = pinkColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = pinkColor,
                unfocusedLabelColor = secondaryTextColor,
                cursorColor = textColor
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón 3D Guardar Cambios
        val saveBtnBg = if (isDark) Color(0xFF00796B) else Color(0xFFE2725B)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onSaveProfile(nameText, currentUserImageUri, anniversaryDate) }
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
                    .background(saveBtnBg),
                contentAlignment = Alignment.Center
            ) {
                Text("💾 GUARDAR CAMBIOS", fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Botón 3D Cerrar Sesión
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onLogout() }
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
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚪 CERRAR SESIÓN", fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // Dialogo del Editor de Aniversario Retro Píxel
    if (showAnniversaryDialog) {
        AnniversaryEditDialog(
            currentAnniversary = anniversaryDate,
            onDismiss = { showAnniversaryDialog = false },
            onSave = { newDate ->
                anniversaryDate = newDate
                showAnniversaryDialog = false
                onSaveProfile(currentUserName, currentUserImageUri, newDate)
            },
            isDark = isDark
        )
    }
}

@Composable
fun AnniversaryEditDialog(
    currentAnniversary: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val boxBackground = if (isDark) Color(0xFF282828) else Color(0xFFFFFBEA)

    val calendar = remember { Calendar.getInstance().apply { timeInMillis = currentAnniversary } }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) } 
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(3.dp, borderColor),
            color = backgroundColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuestra Fecha Especial 🗓️",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetroNumberPicker("Día", day, 1, 31, { day = it }, textColor, borderColor, boxBackground)
                    RetroNumberPicker("Mes", month, 1, 12, { month = it }, textColor, borderColor, boxBackground)
                    RetroNumberPicker("Año", year, 2000, 2026, { year = it }, textColor, borderColor, boxBackground)
                }

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancelar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .border(2.dp, borderColor)
                                .background(Color(0xFFD32F2F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CANCELAR", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                        }
                    }

                    // Guardar
                    val saveBtnBg = if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month - 1)
                                    val maxDays = getActualMaximum(Calendar.DAY_OF_MONTH)
                                    val finalDay = if (day > maxDays) maxDays else day
                                    set(Calendar.DAY_OF_MONTH, finalDay)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onSave(selectedCal.timeInMillis)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .border(2.dp, borderColor)
                                .background(saveBtnBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GUARDAR", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroNumberPicker(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    textColor: Color,
    borderColor: Color,
    boxBackground: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontFamily = Vt323, fontSize = 16.sp, color = textColor)
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, borderColor)
                .background(boxBackground)
                .clickable { if (value < max) onValueChange(value + 1) else onValueChange(min) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(36.dp)
                .border(2.dp, borderColor)
                .background(boxBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value.toString(), fontFamily = Vt323, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, borderColor)
                .background(boxBackground)
                .clickable { if (value > min) onValueChange(value - 1) else onValueChange(max) },
            contentAlignment = Alignment.Center
        ) {
            Text("-", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

fun calculateTimeTogether(anniversaryMillis: Long): String {
    val anniversary = Calendar.getInstance().apply { timeInMillis = anniversaryMillis }
    val today = Calendar.getInstance()
    
    var years = today.get(Calendar.YEAR) - anniversary.get(Calendar.YEAR)
    var months = today.get(Calendar.MONTH) - anniversary.get(Calendar.MONTH)
    var days = today.get(Calendar.DAY_OF_MONTH) - anniversary.get(Calendar.DAY_OF_MONTH)
    
    if (days < 0) {
        months -= 1
        val prevMonth = (today.get(Calendar.MONTH) - 1 + 12) % 12
        val temp = Calendar.getInstance().apply {
            set(Calendar.MONTH, prevMonth)
            set(Calendar.YEAR, today.get(Calendar.YEAR))
        }
        days += temp.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    if (months < 0) {
        years -= 1
        months += 12
    }
    
    val parts = mutableListOf<String>()
    if (years > 0) parts.add("$years ${if (years == 1) "año" else "años"}")
    if (months > 0) parts.add("$months ${if (months == 1) "mes" else "meses"}")
    if (days > 0) parts.add("$days ${if (days == 1) "día" else "días"}")
    
    return if (parts.isEmpty()) "¡Hoy es nuestro primer día! ❤️" else "Llevamos " + parts.joinToString(", ") + " juntos"
}

@Composable
fun SettingsScreen(
    currentTheme: String,
    useCustomBg: Boolean,
    onBgPreferenceChange: (Boolean) -> Unit,
    versionName: String,
    onThemeChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onColorSelect: (String) -> Unit,
    currentCacheLimit: Long,
    onCacheLimitChange: (Long) -> Unit,
    onTestNotification: () -> Unit,
    updateInterval: Long,
    onUpdateIntervalChange: (Long) -> Unit,
    appointmentLeadTime: Long,
    onAppointmentLeadTimeChange: (Long) -> Unit
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val isMono = currentTheme == "Pixel Monocromático"
    var showBgChoiceDialog by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf("") }
    
    val backgroundColor = getAppBackgroundColor(currentTheme)
    
    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_back_pixel),
                    contentDescription = "Volver",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Configuración",
                fontFamily = Vt323,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp)) 
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "TEMA VISUAL", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = currentTheme == "Pixel Claro", onClick = { onThemeChange("Pixel Claro") })
            Text("Pixel Claro", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onThemeChange("Pixel Claro") })
            Spacer(modifier = Modifier.width(20.dp))
            RadioButton(selected = currentTheme == "Pixel Oscuro", onClick = { onThemeChange("Pixel Oscuro") })
            Text("Pixel Oscuro", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onThemeChange("Pixel Oscuro") })
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isDark) "COLOR DE BARRAS (OSCURO)" else "COLOR DE BARRAS (CLARO)",
            fontFamily = Vt323, fontSize = 18.sp, color = textColor
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val colors = listOf("#7C3AED", "#0EA5E9", "#10B981", "#EC4899", "#F97316", "#06B6D4", "#92400E")
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                        .border(2.dp, borderColor, CircleShape)
                        .clickable {
                            if (!isMono) {
                                selectedColorHex = colorHex
                                showBgChoiceDialog = true
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "LÍMITE DE CACHÉ", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = currentCacheLimit == 100L, onClick = { onCacheLimitChange(100L) })
            Text("100MB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(100L) })
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(selected = currentCacheLimit == 500L, onClick = { onCacheLimitChange(500L) })
            Text("500MB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(500L) })
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(selected = currentCacheLimit == 1024L, onClick = { onCacheLimitChange(1024L) })
            Text("1GB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(1024L) })
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "FRECUENCIA DE ACTUALIZACIÓN", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
        Text(text = "(Mínimo 15 min por sistema Android)", fontFamily = Vt323, fontSize = 14.sp, color = textColor.copy(alpha = 0.6f))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = updateInterval == 15L, onClick = { onUpdateIntervalChange(15L) })
                Text("15m", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = updateInterval == 60L, onClick = { onUpdateIntervalChange(60L) })
                Text("1h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = updateInterval == 360L, onClick = { onUpdateIntervalChange(360L) })
                Text("6h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = updateInterval == 720L, onClick = { onUpdateIntervalChange(720L) })
                Text("12h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "AVISO DE CITAS (CALENDARIO)", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = appointmentLeadTime == 15L, onClick = { onAppointmentLeadTimeChange(15L) })
                Text("15m", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = appointmentLeadTime == 60L, onClick = { onAppointmentLeadTimeChange(60L) })
                Text("1h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = appointmentLeadTime == 180L, onClick = { onAppointmentLeadTimeChange(180L) })
                Text("3h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RadioButton(selected = appointmentLeadTime == 1440L, onClick = { onAppointmentLeadTimeChange(1440L) })
                Text("1d", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón 3D Buscar Actualizaciones
        val updatesBtnBg = if (isDark) Color(0xFF00796B) else Color(0xFF673AB7)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onCheckUpdates() }
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
                    .background(updatesBtnBg),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄 BUSCAR ACTUALIZACIONES", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón 3D Cerrar Sesión
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onLogout() }
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
                    .background(Color(0xFF795548)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚪 CERRAR SESIÓN", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Versión actual: $versionName",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = textColor.copy(alpha = 0.8f)
        )

        // Diálogo pixel-art para escoger si aplicar el color de barras también al fondo
        if (showBgChoiceDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showBgChoiceDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(3.dp, borderColor),
                    color = backgroundColor,
                    shape = RectangleShape
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Personalizar Fondo 🎨",
                            fontFamily = Vt323,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¿Quieres aplicar este color también al fondo de la aplicación?",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Opción 1: Solo Barras
                            val btnBg1 = if (isDark) Color(0xFF2E2D2D) else Color(0xFFE5D5B5)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        onBgPreferenceChange(false)
                                        onColorSelect(selectedColorHex)
                                        showBgChoiceDialog = false
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(42.dp).offset(y = 6.dp).background(borderColor))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .border(2.dp, borderColor)
                                        .background(btnBg1),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Solo en barras", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                                }
                            }

                            // Opción 2: Barras y Fondo
                            val btnBg2 = Color(android.graphics.Color.parseColor(selectedColorHex))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        onBgPreferenceChange(true)
                                        onColorSelect(selectedColorHex)
                                        showBgChoiceDialog = false
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(42.dp).offset(y = 6.dp).background(borderColor))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .border(2.dp, borderColor)
                                        .background(btnBg2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("En barras y fondo", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                                }
                            }

                            // Cancelar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { showBgChoiceDialog = false }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(42.dp).offset(y = 6.dp).background(borderColor))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .border(2.dp, borderColor)
                                        .background(Color(0xFFD32F2F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cancelar", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
