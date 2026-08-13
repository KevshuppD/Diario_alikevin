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

// Calcula slots continuos de 1 hora dinámicamente asegurando margen superior e inferior
fun computeHourlyTimeSlots(subjects: List<ClassSubject>): List<TimeSlot> {
    val minClassHour = subjects.minOfOrNull { it.startHour } ?: 8
    val maxClassHour = subjects.maxOfOrNull { if (it.endMinute > 0) it.endHour + 1 else it.endHour } ?: 20

    val minHour = minOf(7, minClassHour - 1).coerceAtLeast(6)
    val maxHour = maxOf(21, maxClassHour + 1).coerceAtMost(23)

    return (minHour until maxHour).map { hour ->
        val startStr = String.format("%02d:00", hour)
        val endStr = String.format("%02d:00", hour + 1)
        TimeSlot("$startStr - $endStr", hour * 60, (hour + 1) * 60)
    }
}

val DAYS_OF_WEEK = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
val SUBJECT_COLORS = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
    "#98DED9", "#C7CEEA", "#FFDAC1", "#E2F0CB",
    "#B5EAD7", "#FFB7B2", "#E0BBE4", "#957FEF"
)

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
            .padding(if (isLandscape) 6.dp else 12.dp)
    ) {
        if (isLandscape) {
            // Encabezado horizontal compacto para Landscape (Evita cortes en bordes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<",
                        fontFamily = Vt323,
                        fontSize = 22.sp,
                        color = textColor,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HORARIO 📚",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Filtro de dueño
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("both" to "👥 Ambos", "kevin" to "👦 Kevin", "ali" to "👧 Ali").forEach { (key, label) ->
                        val isSelected = filterOwner == key
                        Text(
                            text = label,
                            fontFamily = Vt323,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else textColor,
                            maxLines = 1,
                            modifier = Modifier
                                .border(1.dp, borderColor)
                                .background(if (isSelected) borderColor else cardBg)
                                .clickable { filterOwner = key }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
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
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            // Encabezado vertical estándar para Portrait
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<",
                        fontFamily = Vt323,
                        fontSize = 26.sp,
                        color = textColor,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HORARIO DE CLASES 📚",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Text(
                    text = "+ AÑADIR",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .background(cardBg)
                        .clickable {
                            editingSubject = null
                            showClassDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Filtro de Dueño
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("both" to "👥 Ambos", "kevin" to "👦 Kevin", "ali" to "👧 Ali").forEach { (key, label) ->
                    val isSelected = filterOwner == key
                    Text(
                        text = label,
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = if (isSelected) Color.White else textColor,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .border(1.dp, borderColor)
                            .background(if (isSelected) borderColor else cardBg)
                            .clickable { filterOwner = key }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
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

            val timeSlots = remember(filteredSubjects) {
                computeHourlyTimeSlots(filteredSubjects)
            }

            // Grilla Interactiva del Horario
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
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val hourColumnWidth = 80.dp
    val dayColumnWidth = 145.dp
    val slotHeightDp = if (isLandscape) 55.dp else 68.dp
    val minTimeMinutes = timeSlots.firstOrNull()?.startMinutes ?: (7 * 60)
    val totalGridHeightDp = slotHeightDp * timeSlots.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor)
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
                    .height(36.dp)
                    .border(1.dp, borderColor),
                contentAlignment = Alignment.Center
            ) {
                Text("Hora", fontFamily = Vt323, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            }

            days.forEach { dayName ->
                Box(
                    modifier = Modifier
                        .width(dayColumnWidth)
                        .height(36.dp)
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(dayName, fontFamily = Vt323, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
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
            // Columna de Horas (Fija a la izquierda)
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
                            .border(1.dp, borderColor)
                            .background(borderColor.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.label,
                            fontFamily = Vt323,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 6 Columnas de Días (Lunes a Sábado) con superposición unificada de clases
            days.forEachIndexed { index, _ ->
                val dayOfWeekNum = index + 1 // 1..6
                val daySubjects = remember(subjects, dayOfWeekNum) {
                    subjects.filter { it.dayOfWeek == dayOfWeekNum }
                }

                Box(
                    modifier = Modifier
                        .width(dayColumnWidth)
                        .height(totalGridHeightDp)
                        .border(1.dp, borderColor.copy(alpha = 0.4f))
                ) {
                    // 1. Rejilla de fondo (Líneas horizontales por cada slot)
                    Column(modifier = Modifier.fillMaxSize()) {
                        timeSlots.forEach { slot ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(slotHeightDp)
                                    .border(0.5.dp, borderColor.copy(alpha = 0.25f))
                                    .clickable {
                                        // Click en celda vacía para añadir clase
                                        onSubjectClick(
                                            ClassSubject(
                                                dayOfWeek = dayOfWeekNum,
                                                startHour = slot.startMinutes / 60,
                                                startMinute = slot.startMinutes % 60,
                                                endHour = slot.endMinutes / 60,
                                                endMinute = slot.endMinutes % 60,
                                                owner = currentUserOwner
                                            )
                                        )
                                    }
                            )
                        }
                    }

                    // 2. Tarjetas de Clases Unificadas y Continuas en Capa de Superposición
                    daySubjects.forEach { sub ->
                        val subStartMinutes = sub.startHour * 60 + sub.startMinute
                        val subEndMinutes = sub.endHour * 60 + sub.endMinute

                        val startDiff = maxOf(0, subStartMinutes - minTimeMinutes)
                        val endDiff = maxOf(startDiff + 15, subEndMinutes - minTimeMinutes)
                        val durationMinutes = endDiff - startDiff

                        val topDp = slotHeightDp * (startDiff.toFloat() / 60f)
                        val heightDp = slotHeightDp * (durationMinutes.toFloat() / 60f)

                        // Detectar si se solapa con otra materia el mismo día
                        val overlappingSubs = daySubjects.filter { other ->
                            other.id != sub.id && isTimeOverlapping(
                                subStartMinutes, subEndMinutes,
                                other.startHour * 60 + other.startMinute, other.endHour * 60 + other.endMinute
                            )
                        }

                        val cardWidth = if (overlappingSubs.isNotEmpty()) {
                            (dayColumnWidth - 6.dp) / (overlappingSubs.size + 1)
                        } else {
                            dayColumnWidth - 4.dp
                        }

                        val xOffset = if (overlappingSubs.isNotEmpty()) {
                            val sortedGroup = (overlappingSubs + sub).sortedBy { it.id }
                            val subIdx = sortedGroup.indexOf(sub)
                            2.dp + (subIdx * cardWidth.value).dp
                        } else {
                            2.dp
                        }

                        val subjColor = try {
                            Color(android.graphics.Color.parseColor(sub.colorHex))
                        } catch (e: Exception) {
                            Color(0xFFFF6B6B)
                        }

                        val timeFormat = String.format(
                            "%02d:%02d-%02d:%02d",
                            sub.startHour,
                            sub.startMinute,
                            sub.endHour,
                            sub.endMinute
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = xOffset, y = topDp)
                                .width(cardWidth)
                                .height(heightDp)
                                .background(subjColor.copy(alpha = 0.95f))
                                .border(1.5.dp, Color.Black)
                                .clickable { onSubjectClick(sub) }
                                .padding(horizontal = 3.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (filterOwner == "both" && sub.owner != "both") {
                                    val ownerBadge = if (sub.owner == "kevin") "👦 Kevin" else "👧 Ali"
                                    Text(
                                        text = ownerBadge,
                                        fontFamily = Vt323,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = sub.name,
                                    fontFamily = Vt323,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    text = "⏰ $timeFormat",
                                    fontFamily = Vt323,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                if (sub.room.isNotBlank() && heightDp.value >= 38f) {
                                    Text(
                                        text = "📍 ${sub.room}",
                                        fontFamily = Vt323,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
