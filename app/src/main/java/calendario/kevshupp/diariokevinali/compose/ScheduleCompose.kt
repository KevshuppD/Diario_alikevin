package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class ClassSubject(
    val id: String = "",
    val name: String = "",
    val teacher: String = "",
    val room: String = "",
    val dayOfWeek: Int = 1, // 1: Lunes, 2: Martes, 3: Miércoles, 4: Jueves, 5: Viernes, 6: Sábado
    val startHour: Int = 8,
    val startMinute: Int = 30,
    val endHour: Int = 9,
    val endMinute: Int = 15,
    val owner: String = "both", // "kevin", "ali", "both"
    val colorHex: String = "#FF6B6B"
)

// Extension de Modifier para dibujar bordes continuos de clases que abarcan múltiples celdas
fun Modifier.continuousBlockBorder(
    width: Dp,
    color: Color,
    isTop: Boolean,
    isBottom: Boolean
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val widthPx = size.width
    val heightPx = size.height

    // Borde izquierdo
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, heightPx),
        strokeWidth = strokeWidth
    )
    // Borde derecho
    drawLine(
        color = color,
        start = Offset(widthPx, 0f),
        end = Offset(widthPx, heightPx),
        strokeWidth = strokeWidth
    )
    // Borde superior (solo si es el inicio de la clase)
    if (isTop) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(widthPx, 0f),
            strokeWidth = strokeWidth
        )
    }
    // Borde inferior (solo si es el final de la clase)
    if (isBottom) {
        drawLine(
            color = color,
            start = Offset(0f, heightPx),
            end = Offset(widthPx, heightPx),
            strokeWidth = strokeWidth
        )
    }
}

// Definición de bloques lectivos estándar
data class TimeSlot(
    val label: String,
    val startMinutes: Int,
    val endMinutes: Int
)

// Calcula slots continuos de 1 hora dinámicamente adaptados al rango real de clases
fun computeHourlyTimeSlots(subjects: List<ClassSubject>): List<TimeSlot> {
    val minClassHour = subjects.minOfOrNull { it.startHour } ?: 8
    val maxClassHour = subjects.maxOfOrNull { if (it.endMinute > 0) it.endHour + 1 else it.endHour } ?: 18

    val minHour = minOf(8, minClassHour).coerceAtLeast(6)
    val maxHour = maxOf(18, maxClassHour + 1).coerceAtMost(23)

    return (minHour until maxHour).map { hour ->
        val startStr = String.format("%02d:00", hour)
        val endStr = String.format("%02d:00", hour + 1)
        TimeSlot(startStr, hour * 60, (hour + 1) * 60)
    }
}

val DAYS_OF_WEEK = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
val SUBJECT_COLORS = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
    "#98DED9", "#C7CEEA", "#FFDAC1", "#E2F0CB",
    "#B5EAD7", "#FFB7B2", "#E0BBE4", "#957FEF"
)

// Helper para calcular color de texto con contraste perfecto (Oscuro o Claro) según luminancia
fun getContrastingTextColor(backgroundColor: Color): Color {
    val r = backgroundColor.red
    val g = backgroundColor.green
    val b = backgroundColor.blue
    // Luminancia estándar sRGB
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return if (luminance > 0.58) Color(0xFF1E1E1E) else Color.White
}

@Composable
fun ScheduleDashboardView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    val coupleId = remember(prefs) { prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123" }
    val rawUserName = remember(prefs) { prefs.getString("userName", "Kevin") ?: "Kevin" }
    val currentUserOwner = remember(rawUserName) {
        if (rawUserName.equals("ali", ignoreCase = true)) "ali" else "kevin"
    }
    val db = FirebaseFirestore.getInstance()

    var subjects by remember { mutableStateOf<List<ClassSubject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Filtro de dueño: "both" (Ambos), "kevin" (Kevin), "ali" (Ali)
    var filterOwner by remember { mutableStateOf("both") }

    // Modo de vista: "grid" (Grilla Semanal) o "agenda" (Por Día)
    var viewMode by remember { mutableStateOf(if (isLandscape) "grid" else "agenda") }

    // Día seleccionado para vista de agenda (1: Lun, 2: Mar, 3: Mié, 4: Jue, 5: Vie)
    val currentDayOfWeek = remember {
        val cal = java.util.Calendar.getInstance()
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            else -> 1
        }
    }
    var selectedDayTab by remember { mutableStateOf(currentDayOfWeek) }

    // Diálogo de agregar/editar
    var showClassDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<ClassSubject?>(null) }

    // Suscripción Firestore en tiempo real
    DisposableEffect(coupleId) {
        val listener = db.collection("schedules")
            .document(coupleId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val rawList = snapshot.get("classes") as? List<Map<String, Any>> ?: emptyList()
                    val parsed = rawList.mapNotNull { map ->
                        try {
                            ClassSubject(
                                id = map["id"] as? String ?: System.currentTimeMillis().toString(),
                                name = map["name"] as? String ?: "",
                                teacher = map["teacher"] as? String ?: "",
                                room = map["room"] as? String ?: "",
                                dayOfWeek = (map["dayOfWeek"] as? Number)?.toInt() ?: 1,
                                startHour = (map["startHour"] as? Number)?.toInt() ?: 8,
                                startMinute = (map["startMinute"] as? Number)?.toInt() ?: 30,
                                endHour = (map["endHour"] as? Number)?.toInt() ?: 9,
                                endMinute = (map["endMinute"] as? Number)?.toInt() ?: 15,
                                owner = map["owner"] as? String ?: "both",
                                colorHex = map["colorHex"] as? String ?: "#FF6B6B"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    subjects = parsed
                } else {
                    subjects = emptyList()
                }
                isLoading = false
            }
        onDispose { listener.remove() }
    }

    val saveSubjectsToFirestore: (List<ClassSubject>) -> Unit = { list ->
        val serialized = list.map { s ->
            mapOf(
                "id" to s.id,
                "name" to s.name,
                "teacher" to s.teacher,
                "room" to s.room,
                "dayOfWeek" to s.dayOfWeek,
                "startHour" to s.startHour,
                "startMinute" to s.startMinute,
                "endHour" to s.endHour,
                "endMinute" to s.endMinute,
                "owner" to s.owner,
                "colorHex" to s.colorHex
            )
        }
        db.collection("schedules").document(coupleId)
            .set(mapOf("classes" to serialized), SetOptions.merge())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (isLandscape) 4.dp else 8.dp)
    ) {
        // ENCABEZADO SUPERIOR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 6.dp else 10.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "<",
                        fontFamily = Vt323,
                        fontSize = if (isLandscape) 22.sp else 26.sp,
                        color = textColor,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLandscape) "HORARIO 📚" else "HORARIO DE CLASES 📚",
                        fontFamily = Vt323,
                        fontSize = if (isLandscape) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1
                    )
                }

                Text(
                    text = "+ AÑADIR",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .border(1.5.dp, borderColor)
                        .background(cardBg)
                        .clickable {
                            editingSubject = null
                            showClassDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // FILTROS Y SELECTOR DE MODO (En fila dedicada)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtro de dueño (Ambos, Kevin, Ali)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("both" to "👥 Ambos", "kevin" to "👦 Kevin", "ali" to "👧 Ali").forEach { (key, label) ->
                        val isSelected = filterOwner == key
                        Text(
                            text = label,
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else textColor,
                            maxLines = 1,
                            modifier = Modifier
                                .border(1.dp, borderColor)
                                .background(if (isSelected) borderColor else cardBg)
                                .clickable { filterOwner = key }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Selector de modo de vista (Día vs Semana)
                Row(
                    modifier = Modifier
                        .border(1.dp, borderColor)
                        .background(cardBg)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (viewMode == "agenda") borderColor else Color.Transparent)
                            .clickable { viewMode = "agenda" }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📋 Día",
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            color = if (viewMode == "agenda") Color.White else textColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(if (viewMode == "grid") borderColor else Color.Transparent)
                            .clickable { viewMode = "grid" }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅 Semana",
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            color = if (viewMode == "grid") Color.White else textColor
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando horario...", fontFamily = Vt323, fontSize = 22.sp, color = textColor)
            }
        } else {
            // Filtrar asignaturas según dueño seleccionado
            val filteredSubjects = remember(subjects, filterOwner) {
                if (filterOwner == "both") subjects
                else subjects.filter { it.owner == filterOwner || it.owner == "both" }
            }

            if (viewMode == "agenda") {
                // VISTA AGENDA POR DÍA (Ultra clara, sin textos cortados, hermosa en vertical)
                ScheduleAgendaView(
                    days = DAYS_OF_WEEK,
                    selectedDay = selectedDayTab,
                    onSelectDay = { selectedDayTab = it },
                    subjects = filteredSubjects,
                    currentUserOwner = currentUserOwner,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    textColor = textColor,
                    onSubjectClick = { subject ->
                        editingSubject = subject
                        showClassDialog = true
                    },
                    onAddForDay = { dayNum ->
                        editingSubject = ClassSubject(
                            dayOfWeek = dayNum,
                            startHour = 8,
                            startMinute = 30,
                            endHour = 10,
                            endMinute = 0,
                            owner = currentUserOwner
                        )
                        showClassDialog = true
                    }
                )
            } else {
                val timeSlots = remember(filteredSubjects) {
                    computeHourlyTimeSlots(filteredSubjects)
                }

                // Grilla Semanal Mejorada con columnas dedicadas
                ScheduleGrid(
                    timeSlots = timeSlots,
                    days = DAYS_OF_WEEK,
                    subjects = filteredSubjects,
                    filterOwner = filterOwner,
                    currentUserOwner = currentUserOwner,
                    borderColor = borderColor,
                    cardBg = cardBg,
                    textColor = textColor,
                    onSubjectClick = { subject ->
                        editingSubject = subject
                        showClassDialog = true
                    }
                )
            }
        }
    }

    // Diálogo de Edición/Creación de Asignatura
    if (showClassDialog) {
        ClassEditDialog(
            subject = editingSubject,
            currentUserOwner = currentUserOwner,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onDismiss = { showClassDialog = false },
            onSave = { savedSubject ->
                val targetId = savedSubject.id.ifBlank { System.currentTimeMillis().toString() }
                val finalSubject = savedSubject.copy(id = targetId)
                val exists = subjects.any { it.id == targetId }
                val newList = if (exists) {
                    subjects.map { if (it.id == targetId) finalSubject else it }
                } else {
                    subjects + finalSubject
                }
                saveSubjectsToFirestore(newList)
                showClassDialog = false
            },
            onDelete = { subjectToDelete ->
                if (subjectToDelete.id.isNotBlank()) {
                    val newList = subjects.filter { it.id != subjectToDelete.id }
                    saveSubjectsToFirestore(newList)
                }
                showClassDialog = false
            }
        )
    }
}

@Composable
fun ScheduleAgendaView(
    days: List<String>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    subjects: List<ClassSubject>,
    currentUserOwner: String,
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    onSubjectClick: (ClassSubject) -> Unit,
    onAddForDay: (Int) -> Unit
) {
    val daySubjects = remember(subjects, selectedDay) {
        subjects.filter { it.dayOfWeek == selectedDay }
            .sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor)
            .background(cardBg)
    ) {
        // Pestañas de días (Lunes a Viernes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(borderColor.copy(alpha = 0.15f))
        ) {
            days.forEachIndexed { index, dayName ->
                val dayNum = index + 1
                val isSelected = selectedDay == dayNum
                val countForDay = subjects.count { it.dayOfWeek == dayNum }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectDay(dayNum) }
                        .background(if (isSelected) borderColor.copy(alpha = 0.35f) else Color.Transparent)
                        .border(0.5.dp, borderColor.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName.take(3).uppercase(),
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) borderColor else textColor
                        )
                        if (countForDay > 0) {
                            Text(
                                text = "($countForDay)",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Lista de clases del día seleccionado
        if (daySubjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "☕ ¡Día libre!",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "No hay clases programadas para este día.",
                        fontFamily = Vt323,
                        fontSize = 17.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAddForDay(selectedDay) },
                        colors = ButtonDefaults.buttonColors(containerColor = borderColor)
                    ) {
                        Text("+ Añadir clase aquí", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daySubjects, key = { it.id }) { sub ->
                    val subjColor = try {
                        Color(android.graphics.Color.parseColor(sub.colorHex))
                    } catch (e: Exception) {
                        Color(0xFFFF6B6B)
                    }
                    val contentColor = getContrastingTextColor(subjColor)

                    val startTime = String.format("%02d:%02d", sub.startHour, sub.startMinute)
                    val endTime = String.format("%02d:%02d", sub.endHour, sub.endMinute)

                    val durationMinutes = (sub.endHour * 60 + sub.endMinute) - (sub.startHour * 60 + sub.startMinute)
                    val durationStr = if (durationMinutes >= 60) {
                        val hrs = durationMinutes / 60
                        val mins = durationMinutes % 60
                        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
                    } else {
                        "${durationMinutes}m"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubjectClick(sub) }
                            .border(1.5.dp, Color.Black),
                        colors = CardDefaults.cardColors(containerColor = subjColor),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Horario y duración
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⏰ $startTime - $endTime",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "($durationStr)",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        color = contentColor.copy(alpha = 0.85f)
                                    )
                                }

                                // Dueño
                                val ownerLabel = when (sub.owner) {
                                    "kevin" -> "👦 Kevin"
                                    "ali" -> "👧 Ali"
                                    else -> "👥 Ambos"
                                }
                                Box(
                                    modifier = Modifier
                                        .background(contentColor.copy(alpha = 0.15f))
                                        .border(1.dp, contentColor.copy(alpha = 0.4f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ownerLabel,
                                        fontFamily = Vt323,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Nombre de la asignatura
                            Text(
                                text = sub.name,
                                fontFamily = Vt323,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )

                            // Detalles (Sala y Profesor)
                            if (sub.room.isNotBlank() || sub.teacher.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (sub.room.isNotBlank()) {
                                        Text(
                                            text = "📍 Sala: ${sub.room}",
                                            fontFamily = Vt323,
                                            fontSize = 15.sp,
                                            color = contentColor.copy(alpha = 0.9f)
                                        )
                                    }
                                    if (sub.teacher.isNotBlank()) {
                                        Text(
                                            text = "👨‍🏫 ${sub.teacher}",
                                            fontFamily = Vt323,
                                            fontSize = 15.sp,
                                            color = contentColor.copy(alpha = 0.9f)
                                        )
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
fun ScheduleGrid(
    timeSlots: List<TimeSlot>,
    days: List<String>,
    subjects: List<ClassSubject>,
    filterOwner: String,
    currentUserOwner: String = "kevin",
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    onSubjectClick: (ClassSubject) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val hourColumnWidth = 46.dp
    val slotHeightDp = if (isLandscape) 42.dp else 48.dp
    val minTimeMinutes = timeSlots.firstOrNull()?.startMinutes ?: (8 * 60)
    val totalGridHeightDp = slotHeightDp * timeSlots.size

    val showSplitByOwner = filterOwner == "both"

    // Calcular anchos para aprovechar toda la pantalla horizontal
    val availableWidth = (screenWidthDp - hourColumnWidth).coerceAtLeast(260.dp)
    val subColWidth = if (showSplitByOwner) {
        maxOf(74.dp, availableWidth / 4) // En vertical caben ~2 días completos (4 subcolumnas) en pantalla
    } else {
        maxOf(85.dp, availableWidth / 3.2f) // En modo 1 persona caben ~3 días completos en pantalla
    }
    val singleDayWidth = if (showSplitByOwner) subColWidth * 2 else subColWidth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.5.dp, borderColor)
            .background(cardBg)
    ) {
        // Cabecera de días (fija horizontalmente con scroll sincronizado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(borderColor.copy(alpha = 0.15f))
                .horizontalScroll(horizontalScrollState)
        ) {
            // Esquina superior izquierda (Hora)
            Box(
                modifier = Modifier
                    .width(hourColumnWidth)
                    .height(38.dp)
                    .border(0.5.dp, borderColor),
                contentAlignment = Alignment.Center
            ) {
                Text("Hora", fontFamily = Vt323, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
            }

            days.forEach { dayName ->
                if (showSplitByOwner) {
                    Column(
                        modifier = Modifier
                            .width(subColWidth * 2)
                            .border(0.5.dp, borderColor)
                    ) {
                        // Título del Día
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(19.dp)
                                .background(borderColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dayName, fontFamily = Vt323, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        // Subcabeceras Kevin / Ali
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(subColWidth)
                                    .height(19.dp)
                                    .border(0.5.dp, borderColor.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👦 Kevin", fontFamily = Vt323, fontSize = 12.sp, color = textColor)
                            }
                            Box(
                                modifier = Modifier
                                    .width(subColWidth)
                                    .height(19.dp)
                                    .border(0.5.dp, borderColor.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👧 Ali", fontFamily = Vt323, fontSize = 12.sp, color = textColor)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(singleDayWidth)
                            .height(38.dp)
                            .border(0.5.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(dayName, fontFamily = Vt323, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }
        }

        // Cuerpo del horario con scroll vertical y horizontal emparejado
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        ) {
            // Columna de Horas (Limpia a la izquierda)
            Column(
                modifier = Modifier
                    .width(hourColumnWidth)
                    .height(totalGridHeightDp)
            ) {
                timeSlots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .width(hourColumnWidth)
                            .height(slotHeightDp)
                            .border(0.5.dp, borderColor.copy(alpha = 0.25f))
                            .background(borderColor.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = slot.label,
                            fontFamily = Vt323,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }

            // Columnas de Días (Lunes a Viernes)
            days.forEachIndexed { index, _ ->
                val dayOfWeekNum = index + 1 // 1..5
                val daySubjects = remember(subjects, dayOfWeekNum) {
                    subjects.filter { it.dayOfWeek == dayOfWeekNum }
                }

                if (showSplitByOwner) {
                    val kevinSubs = remember(daySubjects) { daySubjects.filter { it.owner == "kevin" || it.owner == "both" } }
                    val aliSubs = remember(daySubjects) { daySubjects.filter { it.owner == "ali" || it.owner == "both" } }

                    Row(
                        modifier = Modifier
                            .width(subColWidth * 2)
                            .height(totalGridHeightDp)
                            .border(0.5.dp, borderColor.copy(alpha = 0.35f))
                    ) {
                        DayOwnerColumn(
                            width = subColWidth,
                            totalHeight = totalGridHeightDp,
                            slotHeightDp = slotHeightDp,
                            timeSlots = timeSlots,
                            minTimeMinutes = minTimeMinutes,
                            subjects = kevinSubs,
                            dayOfWeekNum = dayOfWeekNum,
                            ownerKey = "kevin",
                            borderColor = borderColor,
                            onSubjectClick = onSubjectClick
                        )
                        DayOwnerColumn(
                            width = subColWidth,
                            totalHeight = totalGridHeightDp,
                            slotHeightDp = slotHeightDp,
                            timeSlots = timeSlots,
                            minTimeMinutes = minTimeMinutes,
                            subjects = aliSubs,
                            dayOfWeekNum = dayOfWeekNum,
                            ownerKey = "ali",
                            borderColor = borderColor,
                            onSubjectClick = onSubjectClick
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(singleDayWidth)
                            .height(totalGridHeightDp)
                            .border(0.5.dp, borderColor.copy(alpha = 0.35f))
                    ) {
                        DayOwnerColumn(
                            width = singleDayWidth,
                            totalHeight = totalGridHeightDp,
                            slotHeightDp = slotHeightDp,
                            timeSlots = timeSlots,
                            minTimeMinutes = minTimeMinutes,
                            subjects = daySubjects,
                            dayOfWeekNum = dayOfWeekNum,
                            ownerKey = filterOwner,
                            borderColor = borderColor,
                            onSubjectClick = onSubjectClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOwnerColumn(
    width: Dp,
    totalHeight: Dp,
    slotHeightDp: Dp,
    timeSlots: List<TimeSlot>,
    minTimeMinutes: Int,
    subjects: List<ClassSubject>,
    dayOfWeekNum: Int,
    ownerKey: String,
    borderColor: Color,
    onSubjectClick: (ClassSubject) -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(totalHeight)
            .border(0.5.dp, borderColor.copy(alpha = 0.2f))
    ) {
        // 1. Rejilla de fondo
        Column(modifier = Modifier.fillMaxSize()) {
            timeSlots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeightDp)
                        .border(0.5.dp, borderColor.copy(alpha = 0.12f))
                        .clickable {
                            onSubjectClick(
                                ClassSubject(
                                    dayOfWeek = dayOfWeekNum,
                                    startHour = slot.startMinutes / 60,
                                    startMinute = slot.startMinutes % 60,
                                    endHour = slot.endMinutes / 60,
                                    endMinute = slot.endMinutes % 60,
                                    owner = ownerKey
                                )
                            )
                        }
                )
            }
        }

        // 2. Tarjetas de Clases
        subjects.forEach { sub ->
            val subStartMinutes = sub.startHour * 60 + sub.startMinute
            val subEndMinutes = sub.endHour * 60 + sub.endMinute

            val startDiff = maxOf(0, subStartMinutes - minTimeMinutes)
            val endDiff = maxOf(startDiff + 15, subEndMinutes - minTimeMinutes)
            val durationMinutes = endDiff - startDiff

            val topDp = slotHeightDp * (startDiff.toFloat() / 60f)
            val heightDp = maxOf(24.dp, slotHeightDp * (durationMinutes.toFloat() / 60f))

            val subjColor = try {
                Color(android.graphics.Color.parseColor(sub.colorHex))
            } catch (e: Exception) {
                Color(0xFFFF6B6B)
            }
            val contentColor = getContrastingTextColor(subjColor)

            val timeFormat = String.format(
                "%02d:%02d-%02d:%02d",
                sub.startHour,
                sub.startMinute,
                sub.endHour,
                sub.endMinute
            )

            Card(
                modifier = Modifier
                    .offset(x = 1.dp, y = topDp)
                    .width(width - 2.dp)
                    .height(heightDp)
                    .clickable { onSubjectClick(sub) }
                    .border(1.dp, Color.Black),
                colors = CardDefaults.cardColors(containerColor = subjColor),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = sub.name,
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = if (heightDp >= 36.dp) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                    if (heightDp >= 32.dp) {
                        Text(
                            text = timeFormat,
                            fontFamily = Vt323,
                            fontSize = 10.sp,
                            color = contentColor.copy(alpha = 0.9f),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (sub.room.isNotBlank() && heightDp >= 44.dp) {
                        Text(
                            text = "📍${sub.room}",
                            fontFamily = Vt323,
                            fontSize = 10.sp,
                            color = contentColor.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun isTimeOverlapping(startA: Int, endA: Int, startB: Int, endB: Int): Boolean {
    return maxOf(startA, startB) < minOf(endA, endB)
}

@Composable
fun ClassEditDialog(
    subject: ClassSubject?,
    currentUserOwner: String = "kevin",
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onDismiss: () -> Unit,
    onSave: (ClassSubject) -> Unit,
    onDelete: (ClassSubject) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var teacher by remember { mutableStateOf(subject?.teacher ?: "") }
    var room by remember { mutableStateOf(subject?.room ?: "") }
    var dayOfWeek by remember { mutableStateOf(subject?.dayOfWeek ?: 1) }
    var startHourText by remember { mutableStateOf((subject?.startHour ?: 8).toString()) }
    var startMinuteText by remember { mutableStateOf(String.format("%02d", subject?.startMinute ?: 30)) }
    var endHourText by remember { mutableStateOf((subject?.endHour ?: 9).toString()) }
    var endMinuteText by remember { mutableStateOf(String.format("%02d", subject?.endMinute ?: 15)) }
    var owner by remember { mutableStateOf(subject?.owner ?: currentUserOwner) }
    var colorHex by remember { mutableStateOf(subject?.colorHex ?: SUBJECT_COLORS.first()) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedContainerColor = cardBg,
        unfocusedContainerColor = cardBg,
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor.copy(alpha = 0.6f),
        focusedLabelColor = textColor,
        unfocusedLabelColor = textColor.copy(alpha = 0.7f),
        cursorColor = textColor
    )
    val textFieldStyle = TextStyle(
        fontFamily = Vt323,
        fontSize = 18.sp,
        color = textColor
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Text(
                text = if (subject?.id.isNull_or_empty()) "AÑADIR CLASE 📖" else "EDITAR CLASE ✏️",
                fontFamily = Vt323,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de Asignatura", fontFamily = Vt323, color = textColor) },
                    textStyle = textFieldStyle,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Sala / Aula", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Profesor/a", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Día de la Semana:", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    DAYS_OF_WEEK.forEachIndexed { idx, day ->
                        val dayNum = idx + 1
                        val selected = dayOfWeek == dayNum
                        Text(
                            text = day.take(3),
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (selected) Color.White else textColor,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .border(1.dp, borderColor)
                                .background(if (selected) borderColor else cardBg)
                                .clickable { dayOfWeek = dayNum }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text("Horario (8:30 a 20:25):", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = startHourText,
                        onValueChange = { startHourText = it },
                        label = { Text("H.Inicio", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontFamily = Vt323, fontSize = 20.sp, color = textColor)
                    OutlinedTextField(
                        value = startMinuteText,
                        onValueChange = { startMinuteText = it },
                        label = { Text("Min", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text("-", fontFamily = Vt323, fontSize = 20.sp, color = textColor)
                    OutlinedTextField(
                        value = endHourText,
                        onValueChange = { endHourText = it },
                        label = { Text("H.Fin", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontFamily = Vt323, fontSize = 20.sp, color = textColor)
                    OutlinedTextField(
                        value = endMinuteText,
                        onValueChange = { endMinuteText = it },
                        label = { Text("Min", fontFamily = Vt323, color = textColor) },
                        textStyle = textFieldStyle,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("¿De quién es esta clase?:", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                val availableOwners = remember(currentUserOwner, subject) {
                    val list = mutableListOf<Pair<String, String>>()
                    if (currentUserOwner == "ali") {
                        list.add("ali" to "Ali")
                        list.add("both" to "Ambos")
                    } else {
                        list.add("kevin" to "Kevin")
                        list.add("both" to "Ambos")
                    }
                    if (subject != null && subject.owner.isNotBlank() && list.none { it.first == subject.owner }) {
                        val otherLabel = if (subject.owner == "ali") "Ali" else "Kevin"
                        list.add(0, subject.owner to otherLabel)
                    }
                    list
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableOwners.forEach { (key, label) ->
                        val selected = owner == key
                        Text(
                            text = label,
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (selected) Color.White else textColor,
                            modifier = Modifier
                                .border(1.dp, borderColor)
                                .background(if (selected) borderColor else cardBg)
                                .clickable { owner = key }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text("Color de Asignatura:", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SUBJECT_COLORS.forEach { hex ->
                        val parsedCol = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Red }
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(parsedCol)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.Black else Color.Gray)
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val sH = startHourText.toIntOrNull() ?: 8
                        val sM = startMinuteText.toIntOrNull() ?: 30
                        val eH = endHourText.toIntOrNull() ?: 9
                        val eM = endMinuteText.toIntOrNull() ?: 15
                        onSave(
                            ClassSubject(
                                id = subject?.id ?: "",
                                name = name,
                                teacher = teacher,
                                room = room,
                                dayOfWeek = dayOfWeek,
                                startHour = sH,
                                startMinute = sM,
                                endHour = eH,
                                endMinute = eM,
                                owner = owner,
                                colorHex = colorHex
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = borderColor,
                    contentColor = Color.White
                )
            ) {
                Text("Guardar", fontFamily = Vt323, fontSize = 18.sp)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subject != null && subject.id.isNotBlank()) {
                    TextButton(onClick = { onDelete(subject) }) {
                        Text("Eliminar", fontFamily = Vt323, fontSize = 18.sp, color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                }
            }
        }
    )
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
