package calendario.kevshupp.diariokevinali.compose

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import calendario.kevshupp.diariokevinali.MedicationAlarmScheduler
import calendario.kevshupp.diariokevinali.MedicationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MedsDashboardView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    val coupleId = remember(prefs) { prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123" }
    val currentUserName = remember(prefs) { prefs.getString("userName", "Kevin") ?: "Kevin" }

    val db = FirebaseFirestore.getInstance()
    var medsList by remember { mutableStateOf(emptyList<MedicationItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<MedicationItem?>(null) }

    val accentColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.Black
        else -> Color(0xFFD32F2F)
    }

    val boxBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    // Escuchar Firestore en tiempo real para medicamentos
    DisposableEffect(coupleId) {
        val listener = db.collection("medications").document(coupleId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val list = snapshot.get("meds") as? List<*>
                    medsList = list?.mapNotNull { itemMap ->
                        val map = itemMap as? Map<*, *> ?: return@mapNotNull null
                        MedicationItem(
                            id = map["id"]?.toString() ?: "",
                            name = map["name"]?.toString() ?: "",
                            createdBy = map["createdBy"]?.toString() ?: "",
                            durationDays = (map["durationDays"] as? Number)?.toInt(),
                            intervalHours = (map["intervalHours"] as? Number)?.toInt() ?: 8,
                            selectedTimes = (map["selectedTimes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            enableReminder = map["enableReminder"] as? Boolean ?: false,
                            enableAlarm = map["enableAlarm"] as? Boolean ?: false,
                            startDate = (map["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } ?: emptyList()
                } else {
                    medsList = emptyList()
                }
            }
        onDispose {
            listener.remove()
        }
    }

    // Guardar medicamento en Firestore y agendar alarmas locales
    val saveMedication: (MedicationItem) -> Unit = { med ->
        val updatedList = if (medsList.any { it.id == med.id }) {
            medsList.map { if (it.id == med.id) med else it }
        } else {
            medsList + med
        }

        // Cancelar alarmas anteriores del mismo id para evitar duplicados
        val oldMed = medsList.find { it.id == med.id }
        if (oldMed != null) {
            MedicationAlarmScheduler.cancelAllAlarms(context, oldMed)
        }

        // Guardar en Firestore
        db.collection("medications").document(coupleId)
            .set(mapOf("meds" to updatedList.map { it }), SetOptions.merge())
            .addOnSuccessListener {
                // Programar alarmas locales si están habilitadas
                MedicationAlarmScheduler.scheduleAllAlarms(context, med)
                Toast.makeText(context, "Medicamento guardado con éxito 💊", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Eliminar medicamento de Firestore y cancelar alarmas
    val deleteMedication: (MedicationItem) -> Unit = { med ->
        val updatedList = medsList.filter { it.id != med.id }

        db.collection("medications").document(coupleId)
            .set(mapOf("meds" to updatedList.map { it }), SetOptions.merge())
            .addOnSuccessListener {
                // Cancelar alarmas locales
                MedicationAlarmScheduler.cancelAllAlarms(context, med)
                Toast.makeText(context, "Medicamento eliminado 🗑️", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera Retro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                fontFamily = Vt323,
                fontSize = 28.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "MEDICAMENTOS",
                fontFamily = Vt323,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                if (medsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(3.dp, borderColor)
                            .background(boxBackground)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💊", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No hay medicamentos registrados.",
                                fontFamily = Vt323,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Registra los medicamentos para recibir recordatorios y alarmas compartidas.",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(medsList) { med ->
                            MedicationCard(
                                med = med,
                                theme = theme,
                                textColor = textColor,
                                borderColor = borderColor,
                                cardBg = boxBackground,
                                accentColor = accentColor,
                                onEdit = {
                                    editingMedication = med
                                    showAddDialog = true
                                },
                                onDelete = { deleteMedication(med) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Agregar 3D
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        editingMedication = null
                        showAddDialog = true
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .offset(y = 6.dp)
                        .background(borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(3.dp, borderColor)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "➕ REGISTRAR MEDICAMENTO",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditMedicationDialog(
            medication = editingMedication,
            currentUserName = currentUserName,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            boxBackground = boxBackground,
            accentColor = accentColor,
            onDismiss = { showAddDialog = false },
            onSave = { med ->
                saveMedication(med)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MedicationCard(
    med: MedicationItem,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        // Sombra 3D
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(borderColor)
        )
        // Cuerpo de la tarjeta
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = med.name,
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    Text(
                        text = "✏️",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable { onEdit() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "🗑️",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Frecuencia e Intervalo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⏱️ Cada ${med.intervalHours} hrs",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor.copy(alpha = 0.9f)
                )
                
                val durationText = if (med.durationDays == null || med.durationDays <= 0) {
                    "♾️ Indefinido"
                } else {
                    "📅 Por ${med.durationDays} días"
                }
                Text(
                    text = durationText,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horas seleccionadas
            Text(
                text = "🔔 Horarios de toma:",
                fontFamily = Vt323,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                if (med.selectedTimes.isEmpty()) {
                    Text(
                        text = "Ningún horario seleccionado",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                } else {
                    med.selectedTimes.forEach { time ->
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .border(1.dp, borderColor)
                                .background(textColor.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = time,
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recordatorios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (med.enableReminder) "⏰ Recordatorio" else "❌ Sin recordatorio",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = if (med.enableReminder) textColor else textColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (med.enableAlarm) "🔔 Alarma" else "❌ Sin alarma",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = if (med.enableAlarm) textColor else textColor.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "Por: ${med.createdBy}",
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationDialog(
    medication: MedicationItem?,
    currentUserName: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    boxBackground: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (MedicationItem) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(medication?.name ?: "") }
    var isIndefinite by remember { mutableStateOf(medication?.durationDays == null || medication.durationDays <= 0) }
    var durationDaysStr by remember { mutableStateOf(medication?.durationDays?.toString() ?: "7") }
    var intervalHoursStr by remember { mutableStateOf(medication?.intervalHours?.toString() ?: "8") }
    
    var selectedTimes by remember { mutableStateOf(medication?.selectedTimes ?: emptyList()) }
    var enableReminder by remember { mutableStateOf(medication?.enableReminder ?: true) }
    var enableAlarm by remember { mutableStateOf(medication?.enableAlarm ?: false) }

    val dialogBg = if (theme == "Pixel Oscuro") Color(0xFF1E1E1E) else Color(0xFFF5E6BE)

    // Time picker dialog launcher helper
    val showTimePicker = {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                if (!selectedTimes.contains(timeStr)) {
                    selectedTimes = (selectedTimes + timeStr).sorted()
                }
            },
            hour,
            minute,
            true
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(3.dp, borderColor),
            color = dialogBg,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Cabecera Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (medication == null) "💊 NUEVO MEDICAMENTO" else "📝 EDITAR MEDICAMENTO",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Text("✕", fontFamily = Vt323, fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Nombre del Medicamento
                    Text(
                        text = "💊 Nombre del Medicamento:",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Ej. Paracetamol, Ibuprofeno...", fontFamily = Vt323, fontSize = 16.sp) },
                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = boxBackground,
                            unfocusedContainerColor = boxBackground,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderColor,
                            cursorColor = textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Frecuencia en Horas
                    Text(
                        text = "⏱️ Frecuencia (Cada cuántas horas):",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = intervalHoursStr,
                        onValueChange = { intervalHoursStr = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("Ej. 8, 12, 24", fontFamily = Vt323, fontSize = 16.sp) },
                        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = boxBackground,
                            unfocusedContainerColor = boxBackground,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderColor,
                            cursorColor = textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Duración del tratamiento
                    Text(
                        text = "📅 Duración del Tratamiento:",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isIndefinite,
                            onCheckedChange = { isIndefinite = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Text(
                            text = "Tratamiento indefinido / permanente",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = textColor
                        )
                    }

                    if (!isIndefinite) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = durationDaysStr,
                            onValueChange = { durationDaysStr = it.filter { char -> char.isDigit() } },
                            placeholder = { Text("Ej. 5, 7, 10 (días)", fontFamily = Vt323, fontSize = 16.sp) },
                            textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = boxBackground,
                                unfocusedContainerColor = boxBackground,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = borderColor,
                                cursorColor = textColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Configurar Horas de Toma del Día
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏰ Horarios de Toma (${selectedTimes.size}):",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Button(
                            onClick = { showTimePicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RectangleShape,
                            modifier = Modifier.border(1.dp, borderColor)
                        ) {
                            Text("➕ HORA", fontFamily = Vt323, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lista de Horas agregadas
                    if (selectedTimes.isEmpty()) {
                        Text(
                            text = "Ninguna hora agregada aún. Haz clic en + HORA.",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            selectedTimes.forEach { time ->
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp, end = 6.dp)
                                        .border(1.dp, borderColor)
                                        .background(textColor.copy(alpha = 0.05f))
                                        .clickable {
                                            selectedTimes = selectedTimes.filter { it != time }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = time,
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✕",
                                            fontFamily = Vt323,
                                            fontSize = 12.sp,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notificaciones y Alarma
                    Text(
                        text = "🔔 Avisos y Recordatorios:",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableReminder,
                            onCheckedChange = { enableReminder = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Text(
                            text = "Activar notificación (Recordatorio) ⏰",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = textColor
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableAlarm,
                            onCheckedChange = { enableAlarm = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Text(
                            text = "Activar alarma (Sonido/Vibración fuerte) 🔔",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón Guardar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            if (name.trim().isEmpty()) {
                                Toast.makeText(context, "Por favor ingresa el nombre", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            if (selectedTimes.isEmpty()) {
                                Toast.makeText(context, "Por favor selecciona al menos una hora", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            val medItem = MedicationItem(
                                id = medication?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                createdBy = medication?.createdBy ?: currentUserName,
                                durationDays = if (isIndefinite) null else durationDaysStr.toIntOrNull() ?: 7,
                                intervalHours = intervalHoursStr.toIntOrNull() ?: 8,
                                selectedTimes = selectedTimes,
                                enableReminder = enableReminder,
                                enableAlarm = enableAlarm,
                                startDate = medication?.startDate ?: System.currentTimeMillis()
                            )
                            onSave(medItem)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .border(3.dp, borderColor)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💾 GUARDAR",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// FlowRow implementation for older Compose versions if FlowRow is not standard
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        val lines = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentLineWidth = 0

        placeables.forEach { placeable ->
            if (currentLineWidth + placeable.width > layoutWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                currentLineWidth = 0
            }
            currentLine.add(placeable)
            currentLineWidth += placeable.width + 12 // margin
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        val height = lines.sumOf { line -> line.maxOf { it.height } + 12 }
        
        layout(layoutWidth, height) {
            var y = 0
            lines.forEach { line ->
                var x = 0
                val lineHeight = line.maxOf { it.height }
                line.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + 12
                }
                y += lineHeight + 12
            }
        }
    }
}
