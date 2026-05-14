package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.Recipe
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
    onSaveProfile: (String, String?) -> Unit,
    onLogout: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val secondaryTextColor = if (isDark) Color.LightGray else Color(0xFF8B4513)
    val pinkColor = Color(0xFFFF80AB)
    val borderColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4A2511)

    var partnerName by remember { mutableStateOf("Buscando...") }
    var partnerImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coupleId) {
        if (coupleId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documents?.forEach { doc ->
                        if (doc.id != currentUserId) {
                            partnerName = doc.getString("userName") ?: "Pareja"
                            partnerImageUrl = doc.getString("profileImageUrl")
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mi Perfil Pixel",
            fontFamily = Vt323,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Imagen de Perfil Principal
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(3.dp, borderColor)
                .background(if (isDark) Color(0xFF1A1A2E) else Color(0xFFE7D4B5))
                .clickable { onPickImage() }
                .padding(6.dp)
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
                    Text("📷", fontSize = 40.sp)
                }
            }
        }

        var nameText by remember { mutableStateOf(currentUserName) }

        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Nombre", fontFamily = Vt323) },
            textStyle = LocalTextStyle.current.copy(fontFamily = Vt323, fontSize = 24.sp, color = textColor),
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = pinkColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = pinkColor,
                unfocusedLabelColor = secondaryTextColor
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tiempo Juntos estilo "Cuadro"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(if (isDark) Color(0xFF1A1A2E) else Color(0xFFF5E6BE))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Juntos: 4 año(s), 3 mes(es), 25 día(s)", 
                fontFamily = Vt323,
                fontSize = 22.sp,
                color = pinkColor,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Toca la imagen para cambiarla",
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = if (isDark) Color.LightGray else Color(0xFF8B4513),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Imagen de la Pareja
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(3.dp, borderColor)
                .background(if (isDark) Color(0xFF1A1A2E) else Color(0xFFE7D4B5))
                .padding(4.dp)
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
                    Text("?", fontSize = 30.sp)
                }
            }
        }

        Text(
            text = if (partnerImageUrl != null) partnerName else "Cargando pareja...",
            fontFamily = Vt323,
            fontSize = 24.sp,
            color = pinkColor,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSaveProfile(nameText, currentUserImageUri) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFF673AB7))
        ) {
            Text("Guardar Cambios", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
        ) {
            Text("Cerrar Sesión", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
        }
    }
}

@Composable
fun SettingsScreen(
    currentTheme: String,
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
    onUpdateIntervalChange: (Long) -> Unit
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4A2511)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
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
            Spacer(modifier = Modifier.width(48.dp)) // Espaciador para centrar el título
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
                        .clickable { onColorSelect(colorHex) }
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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onTestNotification,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1976D2) else Color(0xFF2196F3))
        ) {
            Text("PROBAR NOTIFICACIÓN", fontFamily = Vt323, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCheckUpdates,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFF673AB7))
        ) {
            Text("BUSCAR ACTUALIZACIONES", fontFamily = Vt323, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
        ) {
            Text("CERRAR SESIÓN", fontFamily = Vt323, fontSize = 22.sp)
        }


        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Versión actual: $versionName",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}
