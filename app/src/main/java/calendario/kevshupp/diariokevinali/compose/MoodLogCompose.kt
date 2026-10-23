package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MoodEntry(
    val id: String = "",
    val who: String = "",
    val mood: String = "",
    val moodLabel: String = "",
    val intensity: Int = 3,
    val note: String = "",
    val timestamp: Long = 0L,
    val author: String = "",
    val isGeneral: Boolean = false
)

// Catálogo fijo de ánimos (emoji, etiqueta) a nivel superior para no reasignarlo en cada recomposición
private val MOOD_OPTIONS = listOf(
    "😊" to "Feliz",
    "🥰" to "Cariñosa",
    "😌" to "Tranquila",
    "😴" to "Cansada",
    "😢" to "Triste",
    "😰" to "Ansiosa",
    "😡" to "Enojada",
    "🤒" to "Enferma",
    "😐" to "Neutral",
    "🤩" to "Emocionada"
)

private const val MOOD_NOTE_MAX = 2000

private val MOOD_PINK = Color(0xFFEC4899)
private val MOOD_BLUE = Color(0xFF2563EB)
private val MOOD_AMBER = Color(0xFFD97706)

// Los registros generales no pertenecen a un día; igual guardan timestamp para mantener el orden
private const val GENERAL_DAY_KEY = "general"

private fun dayKeyOf(cal: Calendar): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

@Composable
fun MoodLogDashboardView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    val coupleId = remember(prefs) { prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123" }
    val myName = remember(prefs) { prefs.getString("userName", "Kevin")?.trim() ?: "Kevin" }
    // Por defecto se registra el ánimo de la pareja
    val partnerName = if (myName.equals("Ali", ignoreCase = true)) "Kevin" else "Ali"

    val db = remember { FirebaseFirestore.getInstance() }
    val entriesRef = remember(coupleId) { db.collection("mood_logs").document(coupleId).collection("entries") }

    var selectedDay by remember { mutableStateOf(Calendar.getInstance()) }
    var showGeneral by remember { mutableStateOf(false) }
    val dayKey = if (showGeneral) GENERAL_DAY_KEY else dayKeyOf(selectedDay)
    var filter by remember { mutableStateOf("Ambos") }
    var entries by remember { mutableStateOf(emptyList<MoodEntry>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<MoodEntry?>(null) }

    // Solo se escucha el día visible: lecturas acotadas al día seleccionado
    DisposableEffect(entriesRef, dayKey) {
        isLoading = true
        val listener = entriesRef.whereEqualTo("dayKey", dayKey)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null || snapshot == null) return@addSnapshotListener
                entries = snapshot.documents.map { doc ->
                    MoodEntry(
                        id = doc.id,
                        who = doc.getString("who") ?: "",
                        mood = doc.getString("mood") ?: "",
                        moodLabel = doc.getString("moodLabel") ?: "",
                        intensity = doc.getLong("intensity")?.toInt() ?: 3,
                        note = doc.getString("note") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        author = doc.getString("author") ?: "",
                        isGeneral = doc.getBoolean("isGeneral") ?: false
                    )
                }.sortedBy { it.timestamp }
            }
        onDispose { listener.remove() }
    }

    val visibleEntries = remember(entries, filter) {
        if (filter == "Ambos") entries else entries.filter { it.who.equals(filter, ignoreCase = true) }
    }
    val groupedByHour = remember(visibleEntries) {
        visibleEntries.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
        }.toSortedMap()
    }

    val isToday = dayKeyOf(selectedDay) == dayKeyOf(Calendar.getInstance())
    val dayLabel = remember(selectedDay) {
        SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es", "ES")).format(selectedDay.time)
            .replaceFirstChar { it.uppercase() }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "<",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📔 BITÁCORA DE ÁNIMO",
                fontFamily = Vt323,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pestañas: registros por día o generales sin fecha
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "📅 POR DÍA", true to "📝 GENERAL").forEach { (general, label) ->
                val selected = showGeneral == general
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, if (selected) MOOD_AMBER else borderColor)
                        .background(if (selected) MOOD_AMBER else cardBg)
                        .clickable { showGeneral = general }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontFamily = Vt323, fontSize = 18.sp, color = if (selected) Color.White else textColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de día
        if (!showGeneral) Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(cardBg)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◀",
                fontFamily = Vt323,
                fontSize = 26.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { selectedDay = (selectedDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) } }
                    .padding(horizontal = 12.dp)
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dayLabel,
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                if (!isToday) {
                    Text(
                        text = "[ IR A HOY ]",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = MOOD_AMBER,
                        modifier = Modifier.clickable { selectedDay = Calendar.getInstance() }
                    )
                }
            }
            Text(
                text = "▶",
                fontFamily = Vt323,
                fontSize = 26.sp,
                color = if (isToday) textColor.copy(alpha = 0.3f) else textColor,
                modifier = Modifier
                    .clickable(enabled = !isToday) {
                        selectedDay = (selectedDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                    }
                    .padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtro por persona
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Ali", "Kevin", "Ambos").forEach { option ->
                val selected = filter == option
                val accent = when (option) {
                    "Ali" -> MOOD_PINK
                    "Kevin" -> MOOD_BLUE
                    else -> MOOD_AMBER
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, if (selected) accent else borderColor)
                        .background(if (selected) accent else cardBg)
                        .clickable { filter = option }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.uppercase(),
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = if (selected) Color.White else textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Línea de tiempo del día separada por hora
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                Text("Cargando...", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.align(Alignment.Center))
            } else if (visibleEntries.isEmpty()) {
                Text(
                    text = if (showGeneral) "Sin registros generales.\nToca ➕ y marca 'Sin fecha'." else "Sin registros este día.\nToca ➕ para anotar un ánimo.",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (showGeneral) {
                        // Más recientes primero; sin fecha ni hora visibles
                        items(visibleEntries.reversed(), key = { it.id }) { entry ->
                            MoodEntryCard(
                                entry = entry,
                                time = null,
                                canDelete = entry.author.equals(myName, ignoreCase = true),
                                textColor = textColor,
                                borderColor = borderColor,
                                cardBg = cardBg,
                                onDelete = { entryToDelete = entry }
                            )
                        }
                    } else groupedByHour.forEach { (hour, hourEntries) ->
                        item(key = "h_$hour") {
                            Text(
                                text = "── %02d:00 ──".format(hour),
                                fontFamily = Vt323,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MOOD_AMBER,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(hourEntries, key = { it.id }) { entry ->
                            MoodEntryCard(
                                entry = entry,
                                time = timeFormat.format(entry.timestamp),
                                canDelete = entry.author.equals(myName, ignoreCase = true),
                                textColor = textColor,
                                borderColor = borderColor,
                                cardBg = cardBg,
                                onDelete = { entryToDelete = entry }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(MOOD_PINK)
                .clickable { showAddDialog = true }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("➕ REGISTRAR ÁNIMO", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
        }
    }

    if (showAddDialog) {
        AddMoodDialog(
            defaultWho = partnerName,
            defaultGeneral = showGeneral,
            baseDay = selectedDay,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onDismiss = { showAddDialog = false },
            onSave = { who, mood, label, intensity, note, timestamp, general ->
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                entriesRef.add(
                    mapOf(
                        "who" to who,
                        "mood" to mood,
                        "moodLabel" to label,
                        "intensity" to intensity,
                        "note" to note,
                        "timestamp" to timestamp,
                        "dayKey" to if (general) GENERAL_DAY_KEY else dayKeyOf(cal),
                        "isGeneral" to general,
                        "author" to myName
                    )
                )
                showAddDialog = false
            }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("¿Borrar registro?", fontFamily = Vt323, fontSize = 22.sp) },
            text = { Text(if (entry.isGeneral) "${entry.mood} ${entry.moodLabel}" else "${entry.mood} ${entry.moodLabel} · ${timeFormat.format(entry.timestamp)}", fontFamily = Vt323, fontSize = 18.sp) },
            confirmButton = {
                Text(
                    "BORRAR",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = MOOD_PINK,
                    modifier = Modifier
                        .clickable {
                            entriesRef.document(entry.id).delete()
                            entryToDelete = null
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    "CANCELAR",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { entryToDelete = null }.padding(8.dp)
                )
            }
        )
    }
}

@Composable
private fun MoodEntryCard(
    entry: MoodEntry,
    time: String?,
    canDelete: Boolean,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onDelete: () -> Unit
) {
    val accent = if (entry.who.equals("Ali", ignoreCase = true)) MOOD_PINK else MOOD_BLUE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = entry.mood, fontSize = 34.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (time != null) {
                    Text(time, fontFamily = Vt323, fontSize = 18.sp, color = textColor.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(entry.who.uppercase(), fontFamily = Vt323, fontSize = 18.sp, color = accent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(entry.moodLabel, fontFamily = Vt323, fontSize = 18.sp, color = textColor)
            }
            Text("■".repeat(entry.intensity) + "□".repeat(5 - entry.intensity), fontFamily = Vt323, fontSize = 16.sp, color = accent)
            if (entry.note.isNotBlank()) {
                Text(entry.note, fontFamily = Vt323, fontSize = 18.sp, color = textColor)
            }
            if (entry.author.isNotBlank()) {
                Text("anotó: ${entry.author}", fontFamily = Vt323, fontSize = 14.sp, color = textColor.copy(alpha = 0.5f))
            }
        }
        if (canDelete) {
            Text(
                "✕",
                fontFamily = Vt323,
                fontSize = 22.sp,
                color = textColor,
                modifier = Modifier.clickable { onDelete() }.padding(6.dp)
            )
        }
    }
}

@Composable
private fun AddMoodDialog(
    defaultWho: String,
    defaultGeneral: Boolean,
    baseDay: Calendar,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onDismiss: () -> Unit,
    onSave: (who: String, mood: String, label: String, intensity: Int, note: String, timestamp: Long, general: Boolean) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    var who by remember { mutableStateOf(defaultWho) }
    var selectedMood by remember { mutableStateOf<Pair<String, String>?>(null) }
    var intensity by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(now.get(Calendar.MINUTE)) }
    var isGeneral by remember { mutableStateOf(defaultGeneral) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = { Text("NUEVO ÁNIMO", fontFamily = Vt323, fontSize = 24.sp, color = textColor) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("¿De quién?", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ali", "Kevin").forEach { option ->
                        val accent = if (option == "Ali") MOOD_PINK else MOOD_BLUE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(2.dp, if (who == option) accent else borderColor)
                                .background(if (who == option) accent else Color.Transparent)
                                .clickable { who = option }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(option.uppercase(), fontFamily = Vt323, fontSize = 18.sp, color = if (who == option) Color.White else textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Ánimo", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                MOOD_OPTIONS.chunked(5).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { option ->
                            val selected = selectedMood == option
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(2.dp, if (selected) MOOD_AMBER else Color.Transparent)
                                    .clickable { selectedMood = option }
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(option.first, fontSize = 24.sp)
                                Text(option.second, fontFamily = Vt323, fontSize = 12.sp, color = textColor, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Intensidad", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(2.dp, borderColor)
                                .background(if (level <= intensity) MOOD_AMBER else Color.Transparent)
                                .clickable { intensity = level },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$level", fontFamily = Vt323, fontSize = 18.sp, color = if (level <= intensity) Color.White else textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.clickable { isGeneral = !isGeneral }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(2.dp, borderColor)
                            .background(if (isGeneral) MOOD_AMBER else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGeneral) Text("✓", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sin fecha (general)", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                }
                if (!isGeneral) {
                    Text("Hora", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeStepper(value = hour, range = 0..23, textColor = textColor, borderColor = borderColor) { hour = it }
                        Text(" : ", fontFamily = Vt323, fontSize = 24.sp, color = textColor)
                        TimeStepper(value = minute, range = 0..59, textColor = textColor, borderColor = borderColor) { minute = it }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(MOOD_NOTE_MAX) },
                    label = { Text("Nota (opcional)", fontFamily = Vt323) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = Vt323, fontSize = 20.sp, color = textColor),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = textColor,
                        focusedBorderColor = MOOD_AMBER,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MOOD_AMBER,
                        unfocusedLabelColor = textColor.copy(alpha = 0.7f)
                    ),
                    minLines = 6,
                    maxLines = 14
                )
                Text(
                    "${note.length}/$MOOD_NOTE_MAX",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            val mood = selectedMood
            Text(
                "GUARDAR",
                fontFamily = Vt323,
                fontSize = 20.sp,
                color = if (mood != null) MOOD_PINK else textColor.copy(alpha = 0.3f),
                modifier = Modifier
                    .clickable(enabled = mood != null) {
                        // Los generales usan la hora real de creación solo para ordenar
                        val ts = if (isGeneral) System.currentTimeMillis() else (baseDay.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        onSave(who, mood!!.first, mood.second, intensity, note.trim(), ts, isGeneral)
                    }
                    .padding(8.dp)
            )
        },
        dismissButton = {
            Text(
                "CANCELAR",
                fontFamily = Vt323,
                fontSize = 20.sp,
                color = textColor,
                modifier = Modifier.clickable { onDismiss() }.padding(8.dp)
            )
        }
    )
}

@Composable
private fun TimeStepper(
    value: Int,
    range: IntRange,
    textColor: Color,
    borderColor: Color,
    onChange: (Int) -> Unit
) {
    val span = range.last - range.first + 1
    Row(
        modifier = Modifier.border(2.dp, borderColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "−",
            fontFamily = Vt323,
            fontSize = 22.sp,
            color = textColor,
            modifier = Modifier.clickable { onChange((value - 1 - range.first + span) % span + range.first) }.padding(horizontal = 10.dp)
        )
        Text("%02d".format(value), fontFamily = Vt323, fontSize = 22.sp, color = textColor)
        Text(
            "+",
            fontFamily = Vt323,
            fontSize = 22.sp,
            color = textColor,
            modifier = Modifier.clickable { onChange((value + 1 - range.first) % span + range.first) }.padding(horizontal = 10.dp)
        )
    }
}
