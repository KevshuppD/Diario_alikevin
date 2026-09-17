package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdvancedSettingsCompose(
    currentTheme: String,
    googleAccountEmail: String?,
    selectedFolderUri: String?,
    syncState: String,
    coupleId: String?,
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
                "Carpeta Local: ${if (!selectedFolderUri.isNullOrEmpty()) "Seleccionada" else "No seleccionada"}",
                "Sincronización: $syncState"
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
                "Persistencia Local: Habilitada"
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
