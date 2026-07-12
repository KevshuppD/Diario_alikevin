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
import calendario.kevshupp.diariokevinali.DuplicateGroup
import calendario.kevshupp.diariokevinali.LocalPhoto
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

    DisposableEffect(currentUserId) {
        val prefs = context.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
        anniversaryDate = prefs.getLong("anniversaryDate", 1643328000000L)

        // Escuchar cambios en tiempo real de forma segura
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    val firestoreDate = snapshot.getLong("anniversaryDate")
                    if (firestoreDate != null && firestoreDate > 0) {
                        anniversaryDate = firestoreDate
                        prefs.edit().putLong("anniversaryDate", firestoreDate).apply()
                    }
                }
            }
        onDispose {
            listener.remove()
        }
    }

    DisposableEffect(coupleId) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (!coupleId.isNullOrEmpty()) {
            isSearchingPartner = true
            val db = FirebaseFirestore.getInstance()
            listener = db.collection("users")
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
        onDispose {
            listener?.remove()
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

        // Nombre de Usuario Retro Campo con botón de guardado integrado
        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Tu nombre de usuario", fontFamily = Vt323, fontSize = 16.sp) },
            textStyle = TextStyle(fontFamily = Vt323, fontSize = 24.sp, color = textColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            trailingIcon = {
                IconButton(
                    onClick = { onSaveProfile(nameText, currentUserImageUri, anniversaryDate) }
                ) {
                    Text(
                        text = "💾",
                        fontSize = 24.sp
                    )
                }
            },
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
    onAppointmentLeadTimeChange: (Long) -> Unit,
    googleAccountEmail: String?,
    selectedFolderUri: String?,
    syncIntervalMinutes: Long,
    wifiOnly: Boolean,
    chargingOnly: Boolean,
    syncState: String,
    syncMaxRetries: Int,
    syncLastError: String?,
    onMaxRetriesChange: (Int) -> Unit,
    onClearLastError: () -> Unit,
    onLinkGoogleDrive: () -> Unit,
    onUnlinkGoogleDrive: () -> Unit,
    onSelectLocalFolder: () -> Unit,
    onIntervalChange: (Long) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onChargingOnlyChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onStopSync: () -> Unit,
    isSyncing: Boolean,
    syncProgress: Int = -1,
    syncStatus: String = "",
    localFilesCount: Int = 0,
    cloudFilesCount: Int = 0,
    syncParallelLines: Int = 3,
    activeSyncSlots: List<Pair<String, Int>> = emptyList(),
    onParallelLinesChange: (Int) -> Unit = {},
    isScanningDuplicates: Boolean = false,
    duplicateGroups: List<DuplicateGroup> = emptyList(),
    scanCompleted: Boolean = false,
    scannedCount: Int = 0,
    totalToScan: Int = 0,
    deletedPhotosCount: Int = 0,
    spaceFreedBytes: Long = 0L,
    isDeleting: Boolean = false,
    onScanDuplicates: () -> Unit = {},
    onDeleteDuplicates: (List<LocalPhoto>) -> Unit = {},
    onResetDuplicateState: () -> Unit = {},
    syncDirection: String = "BIDIRECTIONAL",
    onDirectionChange: (String) -> Unit = {},
    onResetDrive: () -> Unit = {},
    onIncorrectPassword: () -> Unit = {},
    onTestFirestore: ( (String) -> Unit ) -> Unit = {},
    onTestGoogleDrive: ( (String) -> Unit ) -> Unit = {},
    onRenamePhotosByDate: () -> Unit = {}
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val isMono = currentTheme == "Pixel Monocromático"
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

    val btnBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    var appSettingsSubView by remember { mutableStateOf("menu") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        if (appSettingsSubView == "menu") {
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GeneralSettingsMenuButton("🎨 Diseño y Tema", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "theme"
                }
                GeneralSettingsMenuButton("🔔 Alertas y Tiempos", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "alerts"
                }
                GeneralSettingsMenuButton("☁️ Sincronización (Google Drive)", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "sync"
                }
                GeneralSettingsMenuButton("💾 Almacenamiento", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "cache"
                }
                GeneralSettingsMenuButton("⚙️ Sistema", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "system"
                }
                GeneralSettingsMenuButton("🛠️ Avanzado (Diagnóstico)", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "advanced"
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Versión actual: $versionName",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        } else {
            val subTitleText = when (appSettingsSubView) {
                "theme" -> "Diseño y Tema"
                "alerts" -> "Alertas y Tiempos"
                "sync" -> "Sincronización"
                "cache" -> "Almacenamiento"
                "duplicates" -> "Duplicados"
                "advanced" -> "Avanzado"
                else -> "Sistema"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { appSettingsSubView = "menu" }) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_back_pixel),
                        contentDescription = "Volver",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = subTitleText,
                    fontFamily = Vt323,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) 
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (appSettingsSubView) {
                "theme" -> {
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
                        val colors = if (isDark) {
                            listOf("#4A148C", "#0D47A1", "#1B5E20", "#C2185B", "#E65100", "#006064", "#3E2723")
                        } else {
                            listOf("#D1C4E9", "#B3E5FC", "#C8E6C9", "#F8BBD0", "#FFE0B2", "#B2EBF2", "#D7CCC8")
                        }
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                                    .border(2.dp, borderColor, CircleShape)
                                    .clickable {
                                        if (!isMono) {
                                            onColorSelect(colorHex)
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBgPreferenceChange(!useCustomBg) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, borderColor)
                                .background(if (useCustomBg) Color(0xFF81C784) else Color(0x22000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (useCustomBg) {
                                Text(
                                    text = "✓",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Aplicar color también al fondo",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor
                        )
                    }
                }
                "alerts" -> {
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
                }
                "sync" -> {
                    SettingsSyncCompose(
                        currentTheme = currentTheme,
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncIntervalMinutes = syncIntervalMinutes,
                        wifiOnly = wifiOnly,
                        chargingOnly = chargingOnly,
                        syncState = syncState,
                        syncMaxRetries = syncMaxRetries,
                        syncLastError = syncLastError,
                        onMaxRetriesChange = onMaxRetriesChange,
                        onClearLastError = onClearLastError,
                        onLinkGoogleDrive = onLinkGoogleDrive,
                        onUnlinkGoogleDrive = onUnlinkGoogleDrive,
                        onSelectLocalFolder = onSelectLocalFolder,
                        onIntervalChange = onIntervalChange,
                        onWifiOnlyChange = onWifiOnlyChange,
                        onChargingOnlyChange = onChargingOnlyChange,
                        onSyncNow = onSyncNow,
                        onStopSync = onStopSync,
                        isSyncing = isSyncing,
                        syncProgress = syncProgress,
                        syncStatus = syncStatus,
                        localFilesCount = localFilesCount,
                        cloudFilesCount = cloudFilesCount,
                        syncParallelLines = syncParallelLines,
                        activeSyncSlots = activeSyncSlots,
                        onParallelLinesChange = onParallelLinesChange,
                        syncDirection = syncDirection,
                        onDirectionChange = onDirectionChange,
                        onResetDrive = onResetDrive,
                        onIncorrectPassword = onIncorrectPassword
                    )
                }
                "cache" -> {
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

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "LIMPIEZA DE DUPLICADOS", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Busca fotos repetidas en tu carpeta compartida y elimina copias innecesarias para ahorrar almacenamiento local.",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { 
                                appSettingsSubView = "duplicates"
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF00796B) else Color(0xFFE65100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍 BUSCAR FOTOS REPETIDAS", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "ORGANIZACIÓN POR FECHA", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Renombra automáticamente todas las fotos de tu carpeta compartida usando su fecha de creación (ej. IMG_20260712_120000.png). La sincronización inteligente detectará el cambio de nombre sin subir copias duplicadas.",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { 
                                onRenamePhotosByDate()
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF00796B) else Color(0xFFE65100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅 RENOMBRAR FOTOS POR FECHA", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "duplicates" -> {
                    DuplicateRemoverScreen(
                        currentTheme = currentTheme,
                        selectedFolderUri = selectedFolderUri,
                        isScanning = isScanningDuplicates,
                        duplicateGroups = duplicateGroups,
                        scanCompleted = scanCompleted,
                        scannedCount = scannedCount,
                        totalToScan = totalToScan,
                        deletedCount = deletedPhotosCount,
                        freedSpaceBytes = spaceFreedBytes,
                        isDeleting = isDeleting,
                        onScan = onScanDuplicates,
                        onDelete = onDeleteDuplicates,
                        onReset = onResetDuplicateState,
                        textColor = textColor,
                        borderColor = borderColor,
                        isDark = isDark
                    )
                }
                "system" -> {
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

                    Spacer(modifier = Modifier.height(16.dp))

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
                }
                "advanced" -> {
                    AdvancedSettingsCompose(
                        currentTheme = currentTheme,
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncState = syncState,
                        onTestFirestore = onTestFirestore,
                        onTestGoogleDrive = onTestGoogleDrive
                    )
                }
            }
        }
        // Diálogo choice eliminado
    }
}

@Composable
fun GeneralSettingsMenuButton(
    text: String,
    btnBackground: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = 6.dp)
                .background(borderColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(2.dp, borderColor)
                .background(btnBackground)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontFamily = Vt323, fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Bold)
            Text("▶", fontFamily = Vt323, fontSize = 16.sp, color = borderColor)
        }
    }
}

@Composable
fun DuplicateRemoverScreen(
    currentTheme: String,
    selectedFolderUri: String?,
    isScanning: Boolean,
    duplicateGroups: List<DuplicateGroup>,
    scanCompleted: Boolean,
    scannedCount: Int,
    totalToScan: Int,
    deletedCount: Int,
    freedSpaceBytes: Long,
    isDeleting: Boolean,
    onScan: () -> Unit,
    onDelete: (List<LocalPhoto>) -> Unit,
    onReset: () -> Unit,
    textColor: Color,
    borderColor: Color,
    isDark: Boolean
) {
    val Vt323 = FontFamily(Font(R.font.vt323))
    
    val cardBgColor = when {
        isDark -> Color(0xFF1E1E1E)
        currentTheme == "Pixel Monocromático" -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    if (selectedFolderUri.isNullOrEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(cardBgColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ CONFIGURACIÓN INCOMPLETA",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Por favor, selecciona primero tu carpeta local de fotos en la pestaña de Sincronización.",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor)
            .background(cardBgColor)
            .padding(16.dp)
    ) {
        if (!isScanning && !scanCompleted && deletedCount == 0 && !isDeleting) {
            Text(
                text = "ESCANEAR EN BUSCA DE REPETIDOS",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Se analizarán todas las imágenes en la carpeta del álbum para buscar archivos idénticos mediante su firma de contenido MD5.",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onScan() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .offset(y = 6.dp)
                        .background(borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(2.dp, borderColor)
                        .background(if (isDark) Color(0xFF00796B) else Color(0xFF00897B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔍 INICIAR ESCANEO",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (isScanning) {
            Text(
                text = "ESCANEANDO CARPETA DE FOTOS...",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progressText = if (totalToScan > 0) "Procesando $scannedCount de $totalToScan archivos..." else "Buscando fotos locales..."
            Text(
                text = progressText,
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            val progressVal = if (totalToScan > 0) scannedCount.toFloat() / totalToScan else 0f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .border(2.dp, borderColor),
                color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C),
                trackColor = Color.Transparent
            )
        } else if (isDeleting) {
            Text(
                text = "ELIMINANDO IMÁGENES DUPLICADAS...",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progressText = if (scannedCount > 0) "Eliminando $scannedCount de $totalToScan..." else "Borrando archivos..."
            Text(
                text = progressText,
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            val progressVal = if (totalToScan > 0) scannedCount.toFloat() / totalToScan else 0f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .border(2.dp, borderColor),
                color = Color(0xFFD32F2F),
                trackColor = Color.Transparent
            )
        } else if (deletedCount > 0) {
            Text(
                text = "✨ LIMPIEZA COMPLETADA",
                fontFamily = Vt323,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val sizeMb = freedSpaceBytes / (1024.0 * 1024.0)
            val sizeText = if (sizeMb >= 0.1) String.format("%.1f MB", sizeMb) else String.format("%.1f KB", freedSpaceBytes / 1024.0)
            Text(
                text = "Se han eliminado con éxito $deletedCount imágenes duplicadas, liberando $sizeText de espacio local.",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onReset() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .offset(y = 6.dp)
                        .background(borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(2.dp, borderColor)
                        .background(if (isDark) Color(0xFF00796B) else Color(0xFF00897B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ENTENDIDO",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (scanCompleted) {
            if (duplicateGroups.isEmpty()) {
                Text(
                    text = "🎉 ¡TODO LIMPIO!",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No se encontraron imágenes duplicadas en la carpeta seleccionada.",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { onReset() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(2.dp, borderColor)
                            .background(if (isDark) Color(0xFF00796B) else Color(0xFF00897B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VOLVER",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val totalDuplicates = duplicateGroups.sumOf { it.duplicates.size }
                val totalSize = duplicateGroups.sumOf { g -> g.duplicates.sumOf { it.size } }
                val sizeMb = totalSize / (1024.0 * 1024.0)
                val sizeText = if (sizeMb >= 0.1) String.format("%.1f MB", sizeMb) else String.format("%.1f KB", totalSize / 1024.0)

                Text(
                    text = "DUPLICADOS DETECTADOS",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Se encontraron $totalDuplicates imágenes repetidas (Ahorro estimado: $sizeText).",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { 
                            val allDuplicates = duplicateGroups.flatMap { it.duplicates }
                            onDelete(allDuplicates)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(2.dp, borderColor)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗑️ ELIMINAR TODOS LOS DUPLICADOS",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Lista de duplicados encontrados:",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    duplicateGroups.forEach { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor.copy(alpha = 0.4f))
                                .background(textColor.copy(alpha = 0.03f))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .border(1.dp, borderColor)
                                ) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(group.original.uri)
                                            .size(120)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = group.original.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Original: ${group.original.name}",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val origMb = group.original.size / (1024.0 * 1024.0)
                                    val origSizeText = if (origMb >= 0.1) String.format("%.1f MB", origMb) else "${group.original.size / 1024} KB"
                                    Text(
                                        text = "Tamaño: $origSizeText",
                                        fontFamily = Vt323,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            group.duplicates.forEach { dup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "↳ Repetida: ${dup.name}",
                                        fontFamily = Vt323,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDelete(listOf(dup)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🗑️", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedSettingsCompose(
    currentTheme: String,
    googleAccountEmail: String?,
    selectedFolderUri: String?,
    syncState: String,
    onTestFirestore: ( (String) -> Unit ) -> Unit,
    onTestGoogleDrive: ( (String) -> Unit ) -> Unit
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val Vt323Sync = Vt323

    var firestoreTestResult by remember { mutableStateOf<String?>(null) }
    var driveTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingFirestore by remember { mutableStateOf(false) }
    var isTestingDrive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "ESTADO DE CONEXIONES",
            fontFamily = Vt323Sync,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Google Drive Card
        ConnectionCard(
            title = "Nube - Google Drive",
            status = if (googleAccountEmail != null) "VINCULADO" else "NO VINCULADO",
            statusColor = if (googleAccountEmail != null) Color(0xFF2E7D32) else Color(0xFFC62828),
            details = listOf(
                "Cuenta: ${googleAccountEmail ?: "Ninguna"}",
                "Carpeta Local: ${if (!selectedFolderUri.isNullOrEmpty()) "Seleccionada" else "No seleccionada"}"
            ),
            isDark = isDark,
            textColor = textColor,
            borderColor = borderColor,
            testButtonText = "PROBAR GOOGLE DRIVE ☁️",
            isTesting = isTestingDrive,
            testResult = driveTestResult,
            onTest = {
                isTestingDrive = true
                driveTestResult = "Probando conexión..."
                onTestGoogleDrive { result ->
                    driveTestResult = result
                    isTestingDrive = false
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Firebase Firestore Card
        ConnectionCard(
            title = "Base de Datos - Firestore",
            status = "CONECTADO",
            statusColor = Color(0xFF2E7D32),
            details = listOf(
                "Estado: Activo",
                "Sincronización: $syncState"
            ),
            isDark = isDark,
            textColor = textColor,
            borderColor = borderColor,
            testButtonText = "PROBAR FIRESTORE 🔥",
            isTesting = isTestingFirestore,
            testResult = firestoreTestResult,
            onTest = {
                isTestingFirestore = true
                firestoreTestResult = "Probando base de datos..."
                onTestFirestore { result ->
                    firestoreTestResult = result
                    isTestingFirestore = false
                }
            }
        )
    }
}

@Composable
fun ConnectionCard(
    title: String,
    status: String,
    statusColor: Color,
    details: List<String>,
    isDark: Boolean,
    textColor: Color,
    borderColor: Color,
    testButtonText: String,
    isTesting: Boolean,
    testResult: String?,
    onTest: () -> Unit
) {
    val Vt323Sync = Vt323
    val cardBg = if (isDark) Color(0xFF1E1E3F) else Color(0xFFFFFDF6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = Vt323Sync,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = status,
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .border(1.dp, statusColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        details.forEach { detail ->
            Text(
                text = "• $detail",
                fontFamily = Vt323Sync,
                fontSize = 15.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }

        if (testResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor.copy(alpha = 0.5f))
                    .background(if (isDark) Color(0xFF0D0D2B) else Color(0xFFF5E6BE))
                    .padding(8.dp)
            ) {
                Text(
                    text = testResult,
                    fontFamily = Vt323Sync,
                    fontSize = 14.sp,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clickable(enabled = !isTesting) { onTest() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .offset(y = 4.dp)
                    .background(borderColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .border(2.dp, borderColor)
                    .background(if (isTesting) Color.Gray else if (isDark) Color(0xFF00796B) else Color(0xFF00897B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTesting) "PROBANDO..." else testButtonText,
                    fontFamily = Vt323Sync,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

