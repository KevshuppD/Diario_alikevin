package calendario.kevshupp.diariokevinali.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import calendario.kevshupp.diariokevinali.MedicationItem

data class MedicalData(
    val bloodType: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val emergencyContact: String = "",
    val insuranceNotes: String = "",
    val lastUpdated: Long = 0L,
    val updatedBy: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bloodType" to bloodType,
            "allergies" to allergies,
            "conditions" to conditions,
            "medications" to medications,
            "emergencyContact" to emergencyContact,
            "insuranceNotes" to insuranceNotes,
            "lastUpdated" to lastUpdated,
            "updatedBy" to updatedBy
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): MedicalData {
            if (map == null) return MedicalData()
            return MedicalData(
                bloodType = map["bloodType"] as? String ?: "",
                allergies = map["allergies"] as? String ?: "",
                conditions = map["conditions"] as? String ?: "",
                medications = map["medications"] as? String ?: "",
                emergencyContact = map["emergencyContact"] as? String ?: "",
                insuranceNotes = map["insuranceNotes"] as? String ?: "",
                lastUpdated = (map["lastUpdated"] as? Long) ?: 0L,
                updatedBy = map["updatedBy"] as? String ?: ""
            )
        }
    }
}

@Composable
fun MedicalEmergencyCard(
    theme: String,
    onOpenDialog: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable { onOpenDialog() }
    ) {
        // Sombra 3D
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(borderColor)
        )
        // Contenido principal de la tarjeta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(boxBackground)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🏥",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "FICHA MÉDICA DE EMERGENCIA",
                        fontFamily = Vt323,
                        fontSize = 17.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Datos de salud, alergias y contactos 📋",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
            }

            Text(
                text = "▶",
                fontFamily = Vt323,
                fontSize = 22.sp,
                color = borderColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalEmergencyDialog(
    currentUserId: String,
    currentUserName: String,
    coupleId: String?,
    partnerName: String,
    theme: String,
    onDismiss: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current

    val backgroundColor = when {
        isDark -> Color(0xFF1E1E1E)
        isMono -> Color.White
        else -> Color(0xFFF5E6BE)
    }

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

    val boxBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    val accentColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.Black
        else -> Color(0xFFD32F2F)
    }

    // Estado de pestañas: 0 = Mi Ficha, 1 = Ficha de Pareja
    var selectedTab by remember { mutableStateOf(0) }

    // Datos Médicos
    var myMedicalData by remember { mutableStateOf(MedicalData()) }
    var partnerMedicalData by remember { mutableStateOf<MedicalData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Campos editables para Mi Ficha
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var insuranceNotes by remember { mutableStateOf("") }

    // Opciones del selector de tipo de sangre
    val bloodTypesList = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Desconocido")
    var showBloodDropdown by remember { mutableStateOf(false) }

    // Medicamentos activos del módulo Misc → Medicamentos (cargados en tiempo real)
    var activeMeds by remember { mutableStateOf(emptyList<MedicationItem>()) }

    // Escuchar Firestore en tiempo real
    DisposableEffect(coupleId, currentUserId) {
        if (coupleId.isNullOrEmpty()) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("medical_records").document(coupleId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            isLoading = false
            if (snapshot != null && snapshot.exists()) {
                val dataMap = snapshot.data
                if (dataMap != null) {
                    // Cargar datos propios
                    @Suppress("UNCHECKED_CAST")
                    val myMap = dataMap[currentUserId] as? Map<String, Any>
                    if (myMap != null) {
                        val parsedMy = MedicalData.fromMap(myMap)
                        myMedicalData = parsedMy
                        bloodType = parsedMy.bloodType
                        allergies = parsedMy.allergies
                        conditions = parsedMy.conditions
                        medications = parsedMy.medications
                        emergencyContact = parsedMy.emergencyContact
                        insuranceNotes = parsedMy.insuranceNotes
                    }

                    // Cargar datos de la pareja (cualquier otra clave en el mapa)
                    dataMap.keys.forEach { key ->
                        if (key != currentUserId) {
                            @Suppress("UNCHECKED_CAST")
                            val partnerMap = dataMap[key] as? Map<String, Any>
                            if (partnerMap != null) {
                                partnerMedicalData = MedicalData.fromMap(partnerMap)
                            }
                        }
                    }
                }
            }
        }

        onDispose {
            listener.remove()
        }
    }

    // Cargar medicamentos activos desde la colección medications
    DisposableEffect(coupleId) {
        if (coupleId.isNullOrEmpty()) return@DisposableEffect onDispose { }
        val db = FirebaseFirestore.getInstance()
        val medsListener = db.collection("medications").document(coupleId)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    val list = snap.get("meds") as? List<*>
                    activeMeds = list?.mapNotNull { itemMap ->
                        val map = itemMap as? Map<*, *> ?: return@mapNotNull null
                        MedicationItem(
                            id = map["id"]?.toString() ?: "",
                            name = map["name"]?.toString() ?: "",
                            createdBy = map["createdBy"]?.toString() ?: "",
                            durationDays = (map["durationDays"] as? Number)?.toInt(),
                            selectedTimes = (map["selectedTimes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            enableReminder = map["enableReminder"] as? Boolean ?: false,
                            enableAlarm = map["enableAlarm"] as? Boolean ?: false,
                            startDate = (map["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            alarmSoundUri = map["alarmSoundUri"]?.toString(),
                            alarmSoundName = map["alarmSoundName"]?.toString()
                        )
                    } ?: emptyList()
                } else {
                    activeMeds = emptyList()
                }
            }
        onDispose { medsListener.remove() }
    }

    var isEditingMyData by remember { mutableStateOf(false) }

    fun saveMedicalData() {
        if (coupleId.isNullOrEmpty()) {
            Toast.makeText(context, "No hay pareja vinculada para guardar datos", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val updated = MedicalData(
            bloodType = bloodType,
            allergies = allergies,
            conditions = conditions,
            medications = medications,
            emergencyContact = emergencyContact,
            insuranceNotes = insuranceNotes,
            lastUpdated = System.currentTimeMillis(),
            updatedBy = currentUserName
        )

        val db = FirebaseFirestore.getInstance()
        val payload = mapOf(currentUserId to updated.toMap())

        db.collection("medical_records").document(coupleId)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                isSaving = false
                isEditingMyData = false
                Toast.makeText(context, "¡Ficha médica guardada con éxito! 🏥", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .border(3.dp, borderColor),
            color = backgroundColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Cabecera Retro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏥",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "FICHA MÉDICA",
                            fontFamily = Vt323,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontFamily = Vt323,
                            fontSize = 24.sp,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de pestañas Retro (Mi Ficha / Ficha de Pareja)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, borderColor)
                        .background(boxBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selectedTab == 0) accentColor else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🧑‍⚕️ Mi Ficha",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else textColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selectedTab == 1) accentColor else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💖 Pareja ($partnerName)",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == 0) {
                            val myMeds = activeMeds.filter { med ->
                                med.createdBy.isBlank() ||
                                med.createdBy.equals(currentUserName, ignoreCase = true) ||
                                med.createdBy.equals(currentUserId, ignoreCase = true)
                            }

                            if (isEditingMyData) {
                                // Modo Edición: Formulario de datos médicos
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Editar Mi Ficha Médica ✏️",
                                            fontFamily = Vt323,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Button(
                                            onClick = { isEditingMyData = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                            shape = RectangleShape
                                        ) {
                                            Text("✕ CANCELAR", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 1. Tipo de Sangre (Dropdown Selector + Editable)
                                    Text(
                                        text = "🩸 Grupo Sanguíneo:",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = bloodType,
                                            onValueChange = { bloodType = it },
                                            placeholder = { Text("Ej. O+, A-, B+...", fontFamily = Vt323, fontSize = 16.sp) },
                                            textStyle = TextStyle(fontFamily = Vt323, fontSize = 20.sp, color = textColor),
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { showBloodDropdown = true }) {
                                                    Text("▼", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = boxBackground,
                                                unfocusedContainerColor = boxBackground,
                                                focusedBorderColor = accentColor,
                                                unfocusedBorderColor = borderColor,
                                                cursorColor = textColor
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = showBloodDropdown,
                                            onDismissRequest = { showBloodDropdown = false },
                                            modifier = Modifier.background(boxBackground)
                                        ) {
                                            bloodTypesList.forEach { type ->
                                                DropdownMenuItem(
                                                    text = { Text(type, fontFamily = Vt323, fontSize = 18.sp, color = textColor) },
                                                    onClick = {
                                                        bloodType = type
                                                        showBloodDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 2. Alergias
                                    Text(
                                        text = "⚠️ Alergias (Medicamentos, alimentos, etc.):",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = allergies,
                                        onValueChange = { allergies = it },
                                        placeholder = { Text("Ej. Penicilina, Mariscos, Látex, Ninguna...", fontFamily = Vt323, fontSize = 16.sp) },
                                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = boxBackground,
                                            unfocusedContainerColor = boxBackground,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = borderColor,
                                            cursorColor = textColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3. Condiciones Médicas / Enfermedades
                                    Text(
                                        text = "🏥 Condiciones Médicas / Diagnósticos:",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = conditions,
                                        onValueChange = { conditions = it },
                                        placeholder = { Text("Ej. Asma, Migrañas, Diabetes, Hipertensión...", fontFamily = Vt323, fontSize = 16.sp) },
                                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = boxBackground,
                                            unfocusedContainerColor = boxBackground,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = borderColor,
                                            cursorColor = textColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4. Medicación Diaria
                                    Text(
                                        text = "💊 Medicamentos Actuales / Diarios:",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = medications,
                                        onValueChange = { medications = it },
                                        placeholder = { Text("Ej. Inhalador Salbutamol en caso de crisis, Paracetamol...", fontFamily = Vt323, fontSize = 16.sp) },
                                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = boxBackground,
                                            unfocusedContainerColor = boxBackground,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = borderColor,
                                            cursorColor = textColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5. Contacto de Emergencia
                                    Text(
                                        text = "📞 Contacto de Emergencia (Nombre y Teléfono):",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = emergencyContact,
                                        onValueChange = { emergencyContact = it },
                                        placeholder = { Text("Ej. Mamá: +56912345678, Papá: +56987654321", fontFamily = Vt323, fontSize = 16.sp) },
                                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = boxBackground,
                                            unfocusedContainerColor = boxBackground,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = borderColor,
                                            cursorColor = textColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 6. Seguro Médico / Notas Adicionales
                                    Text(
                                        text = "📑 Seguro Médico / Hospital / Notas:",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = insuranceNotes,
                                        onValueChange = { insuranceNotes = it },
                                        placeholder = { Text("Ej. Fonasa / Isapre / N° Póliza 12345 / Clínica Preferida...", fontFamily = Vt323, fontSize = 16.sp) },
                                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = boxBackground,
                                            unfocusedContainerColor = boxBackground,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = borderColor,
                                            cursorColor = textColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Botón 3D Guardar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clickable(enabled = !isSaving) { saveMedicalData() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .offset(y = 4.dp)
                                                .background(borderColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .border(2.dp, borderColor)
                                                .background(accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSaving) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                            } else {
                                                Text(
                                                    text = "💾 GUARDAR MI FICHA MÉDICA",
                                                    fontFamily = Vt323,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            } else {
                                // Modo Lectura: Tarjetas visuales de Mi Ficha
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Mi Ficha de Emergencia 🧑‍⚕️",
                                            fontFamily = Vt323,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Button(
                                            onClick = { isEditingMyData = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = RectangleShape
                                        ) {
                                            Text("✏️ EDITAR", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val hasMyData = bloodType.isNotEmpty() || allergies.isNotEmpty() || conditions.isNotEmpty() || emergencyContact.isNotEmpty() || insuranceNotes.isNotEmpty()

                                    if (!hasMyData && myMeds.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(2.dp, borderColor)
                                                .background(boxBackground)
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🏥", fontSize = 48.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Aún no has registrado tus datos médicos de emergencia.",
                                                    fontFamily = Vt323,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { isEditingMyData = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                    shape = RectangleShape
                                                ) {
                                                    Text("✏️ COMPLETAR MI FICHA MÉDICA", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    } else {
                                        // 🩸 Grupo Sanguíneo
                                        MedicalFieldDisplayCard(
                                            icon = "🩸",
                                            title = "GRUPO SANGUÍNEO",
                                            content = bloodType.ifEmpty { "No especificado" },
                                            highlight = true,
                                            theme = theme
                                        )

                                        // ⚠️ Alergias
                                        MedicalFieldDisplayCard(
                                            icon = "⚠️",
                                            title = "ALERGIAS DECLARADAS",
                                            content = allergies.ifEmpty { "Sin alergias declaradas" },
                                            highlight = allergies.isNotEmpty() && !allergies.equals("ninguna", ignoreCase = true),
                                            theme = theme
                                        )

                                        // 🏥 Condiciones Médicas
                                        MedicalFieldDisplayCard(
                                            icon = "🏥",
                                            title = "CONDICIONES MÉDICAS / DIAGNÓSTICOS",
                                            content = conditions.ifEmpty { "Sin condiciones declaradas" },
                                            highlight = false,
                                            theme = theme
                                        )

                                        // 💊 Medicación Habitual
                                        MedicalFieldDisplayCard(
                                            icon = "💊",
                                            title = "MEDICACIÓN HABITUAL (DECLARADA)",
                                            content = medications.ifEmpty { "Sin medicamentos declarados" },
                                            highlight = false,
                                            theme = theme
                                        )

                                        // 📋 Medicamentos Activos en la App (Mis Remedios)
                                        if (myMeds.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val medBorder = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                            val medBg = if (isDark) Color(0xFF1E3A2F) else Color(0xFFDCFCE7)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(2.dp, medBorder)
                                                    .background(medBg)
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("💊", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                                        Text(
                                                            text = "MEDICAMENTOS ACTIVOS EN LA APP (MIS REMEDIOS)",
                                                            fontFamily = Vt323,
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = medBorder
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    myMeds.forEach { med ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 3.dp)
                                                                .border(1.dp, medBorder.copy(alpha = 0.5f))
                                                                .background(boxBackground)
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "• ${med.name}",
                                                                    fontFamily = Vt323,
                                                                    fontSize = 18.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = textColor
                                                                )
                                                                if (med.selectedTimes.isNotEmpty()) {
                                                                    Text(
                                                                        text = "Horarios: ${med.selectedTimes.joinToString(" · ")}",
                                                                        fontFamily = Vt323,
                                                                        fontSize = 15.sp,
                                                                        color = textColor.copy(alpha = 0.75f)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 📞 Contacto de Emergencia con Botón Directo de Llamada
                                        if (emergencyContact.isNotEmpty()) {
                                            val contactPhone = extractPhoneNumber(emergencyContact)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .border(2.dp, accentColor)
                                                    .background(boxBackground)
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("📞", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                                        Text(
                                                            text = "CONTACTO DE EMERGENCIA",
                                                            fontFamily = Vt323,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = accentColor
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = emergencyContact,
                                                        fontFamily = Vt323,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )

                                                    if (contactPhone.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(
                                                            onClick = {
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                            shape = RectangleShape,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("📞 LLAMAR DIRECTO", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 📑 Seguro Médico / Notas
                                        if (insuranceNotes.isNotEmpty()) {
                                            MedicalFieldDisplayCard(
                                                icon = "📑",
                                                title = "SEGURO MÉDICO / NOTAS",
                                                content = insuranceNotes,
                                                highlight = false,
                                                theme = theme
                                            )
                                        }

                                        if (myMedicalData.lastUpdated > 0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                                            val dateStr = sdf.format(Date(myMedicalData.lastUpdated))
                                            Text(
                                                text = "Última actualización: $dateStr",
                                                fontFamily = Vt323,
                                                fontSize = 14.sp,
                                                color = textColor.copy(alpha = 0.6f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = { isEditingMyData = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = RectangleShape,
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("✏️ EDITAR MI FICHA MÉDICA", fontFamily = Vt323, fontSize = 20.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Pestaña: Ficha de Pareja (Visualización)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val pData = partnerMedicalData
                                val partnerMeds = activeMeds.filter { med ->
                                    val creator = med.createdBy.trim()
                                    !creator.equals(currentUserName, ignoreCase = true) &&
                                    !creator.equals(currentUserId, ignoreCase = true)
                                }

                                if (pData == null || (pData.bloodType.isEmpty() && pData.allergies.isEmpty() && pData.emergencyContact.isEmpty())) {
                                    if (partnerMeds.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(2.dp, borderColor)
                                                .background(boxBackground)
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🏥", fontSize = 48.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "$partnerName aún no ha registrado sus datos médicos de emergencia.",
                                                    fontFamily = Vt323,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Pídele que ingrese a Perfil > Ficha Médica para completar su información.",
                                                    fontFamily = Vt323,
                                                    fontSize = 16.sp,
                                                    color = textColor.copy(alpha = 0.7f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Encabezado de Pareja
                                    Text(
                                        text = "Ficha de Emergencia de $partnerName 💖",
                                        fontFamily = Vt323,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // 🩸 Grupo Sanguíneo
                                    MedicalFieldDisplayCard(
                                        icon = "🩸",
                                        title = "GRUPO SANGUÍNEO",
                                        content = pData.bloodType.ifEmpty { "No especificado" },
                                        highlight = true,
                                        theme = theme
                                    )

                                    // ⚠️ Alergias
                                    MedicalFieldDisplayCard(
                                        icon = "⚠️",
                                        title = "ALERGIAS DECLARADAS",
                                        content = pData.allergies.ifEmpty { "Sin alergias declaradas" },
                                        highlight = pData.allergies.isNotEmpty() && !pData.allergies.equals("ninguna", ignoreCase = true),
                                        theme = theme
                                    )

                                    // 🏥 Condiciones Médicas
                                    MedicalFieldDisplayCard(
                                        icon = "🏥",
                                        title = "CONDICIONES MÉDICAS / DIAGNÓSTICOS",
                                        content = pData.conditions.ifEmpty { "Sin condiciones declaradas" },
                                        highlight = false,
                                        theme = theme
                                    )

                                    // 💊 Medicación Habitual (Texto manual)
                                    MedicalFieldDisplayCard(
                                        icon = "💊",
                                        title = "MEDICACIÓN HABITUAL (DECLARADA)",
                                        content = pData.medications.ifEmpty { "Sin medicamentos declarados" },
                                        highlight = false,
                                        theme = theme
                                    )
                                }

                                // 💊 Medicamentos Activos en la App (Sincronizados en tiempo real del módulo Medicamentos)
                                if (partnerMeds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val medBorder = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                    val medBg = if (isDark) Color(0xFF1E2A3A) else Color(0xFFDBEAFE)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(2.dp, medBorder)
                                            .background(medBg)
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("💊", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                                Text(
                                                    text = "MEDICAMENTOS ACTIVOS EN LA APP ($partnerName)",
                                                    fontFamily = Vt323,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = medBorder
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            partnerMeds.forEach { med ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .border(1.dp, medBorder.copy(alpha = 0.5f))
                                                        .background(boxBackground)
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "• ${med.name}",
                                                            fontFamily = Vt323,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor
                                                        )
                                                        if (med.selectedTimes.isNotEmpty()) {
                                                            Text(
                                                                text = "Horarios: ${med.selectedTimes.joinToString(" · ")}",
                                                                fontFamily = Vt323,
                                                                fontSize = 15.sp,
                                                                color = textColor.copy(alpha = 0.75f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (pData != null && (pData.bloodType.isNotEmpty() || pData.allergies.isNotEmpty() || pData.emergencyContact.isNotEmpty())) {
                                    // 📞 Contacto de Emergencia con Botón Directo de Llamada
                                    val contactPhone = extractPhoneNumber(pData.emergencyContact)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .border(2.dp, accentColor)
                                            .background(boxBackground)
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📞", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                                Text(
                                                    text = "CONTACTO DE EMERGENCIA DE $partnerName",
                                                    fontFamily = Vt323,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = pData.emergencyContact.ifEmpty { "No especificado" },
                                                fontFamily = Vt323,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )

                                            if (contactPhone.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    // Botón Llamar Directamente
                                                    Button(
                                                        onClick = {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                        shape = RectangleShape,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("📞 LLAMAR", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                                                    }

                                                    // Botón Copiar
                                                    OutlinedButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            val clip = ClipData.newPlainText("Contacto de Emergencia", pData.emergencyContact)
                                                            clipboard.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Contacto copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RectangleShape,
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                                                    ) {
                                                        Text("📋 COPIAR", fontFamily = Vt323, fontSize = 18.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 📑 Seguro Médico / Notas
                                    MedicalFieldDisplayCard(
                                        icon = "📑",
                                        title = "SEGURO MÉDICO Y NOTAS",
                                        content = pData.insuranceNotes.ifEmpty { "Sin notas adicionales" },
                                        highlight = false,
                                        theme = theme
                                    )

                                    if (pData.lastUpdated > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                                        val dateStr = sdf.format(Date(pData.lastUpdated))
                                        Text(
                                            text = "Última actualización de $partnerName: $dateStr",
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // Botonera de Acceso Rápido a Emergencias Generales (Pie del diálogo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, borderColor)
                        .background(boxBackground)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚨 MARCADOR:",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    EmergencyQuickCallChip(label = "Ambulancia (131)", number = "131", context = context, textColor = textColor)
                    EmergencyQuickCallChip(label = "Policía (133)", number = "133", context = context, textColor = textColor)
                    EmergencyQuickCallChip(label = "General (911)", number = "911", context = context, textColor = textColor)
                }
            }
        }
    }
}

@Composable
fun MedicalFieldDisplayCard(
    icon: String,
    title: String,
    content: String,
    highlight: Boolean,
    theme: String
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val borderColor = when {
        highlight -> Color(0xFFD32F2F)
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val boxBackground = when {
        highlight && isDark -> Color(0xFF3D1B24)
        highlight -> Color(0xFFFFEBEE)
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(2.dp, borderColor)
            .background(boxBackground)
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                Text(
                    text = title,
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) Color(0xFFD32F2F) else textColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontFamily = Vt323,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun EmergencyQuickCallChip(
    label: String,
    number: String,
    context: Context,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Red)
            .background(Color(0xFFFFEBEE))
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = Vt323,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F)
        )
    }
}

fun extractPhoneNumber(contactText: String): String {
    if (contactText.isBlank()) return ""
    // Extraer dígitos y signo +
    val regex = Regex("""\+?[0-9\s\-()]{7,}""")
    val match = regex.find(contactText)
    return match?.value?.replace(Regex("""[\s\-()]"""), "") ?: ""
}
