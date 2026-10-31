package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.CalendarEvent
import calendario.kevshupp.diariokevinali.R
import calendario.kevshupp.diariokevinali.nextOccurrenceAfter
import calendario.kevshupp.diariokevinali.occursOn
import java.text.SimpleDateFormat
import java.util.*

val CalendarVt323 = FontFamily(Font(R.font.vt323_regular))

private val WEEK_LETTERS = listOf("L", "M", "M", "J", "V", "S", "D")
private val KEVIN_LIGHT = Color(0xFF2563EB)
private val KEVIN_DARK = Color(0xFF7FB0FF)
private val ALI_LIGHT = Color(0xFFD61F7A)
private val ALI_DARK = Color(0xFFFF7FB8)

private class CalendarColors(
    val text: Color,
    val subtitle: Color,
    val card: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color,
    val kevin: Color,
    val ali: Color
)

@Composable
private fun rememberCalendarColors(theme: String): CalendarColors = remember(theme) {
    when (theme) {
        "Pixel Oscuro" -> CalendarColors(Color.White, Color(0xFFFF7FA8), Color(0xFF1A1A2E), Color(0xFF91465F), Color(0xFFFF4081), Color(0xFF14061A), KEVIN_DARK, ALI_DARK)
        "Pixel Monocromático" -> CalendarColors(Color.Black, Color.DarkGray, Color.White, Color.Black, Color.Black, Color.White, Color.Black, Color.DarkGray)
        else -> CalendarColors(Color(0xFF4A2511), Color(0xFF8B4513), Color(0xFFFFFDF9), Color(0xFF4A2511), Color(0xFF8B4513), Color.White, KEVIN_LIGHT, ALI_LIGHT)
    }
}

private fun authorColor(name: String, colors: CalendarColors): Color = when {
    name.contains("ali", ignoreCase = true) -> colors.ali
    name.contains("kevin", ignoreCase = true) -> colors.kevin
    else -> colors.subtitle
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun startOfDay(millis: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun dayTitle(millis: Long): String {
    val day = startOfDay(millis)
    val today = startOfDay(System.currentTimeMillis())
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val locale = Locale("es", "CL")
    val date = SimpleDateFormat("d 'de' MMMM", locale).format(day.time)
    return when {
        isSameDay(day, today) -> "Hoy, $date"
        isSameDay(day, tomorrow) -> "Mañana, $date"
        else -> SimpleDateFormat("EEEE", locale).format(day.time).replaceFirstChar { it.uppercase(locale) } + " $date"
    }
}

private fun nextEventLine(events: List<CalendarEvent>): String? {
    val now = System.currentTimeMillis()
    val next = events.mapNotNull { event -> event.nextOccurrenceAfter(now)?.let { event to it } }
        .minByOrNull { it.second } ?: return null
    val days = ((startOfDay(next.second).timeInMillis - startOfDay(now).timeInMillis) / 86_400_000.0).let { Math.round(it).toInt() }
    val whenText = when (days) {
        0 -> "hoy"
        1 -> "mañana"
        else -> "en $days días"
    }
    return "Próxima: ${next.first.title}, $whenText"
}

@Composable
fun CalendarScreen(
    theme: String,
    events: List<CalendarEvent>,
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    onSaveEvent: (edit: CalendarEvent?, title: String, description: String, date: Long, recurrence: String) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
) {
    val backgroundColor = getAppBackgroundColor(theme)
    val colors = rememberCalendarColors(theme)

    var editorOpen by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    val openEditor = { event: CalendarEvent? ->
        editingEvent = event
        editorOpen = true
    }

    if (editorOpen) {
        EventEditorSheet(
            event = editingEvent,
            selectedDay = selectedTimestamp,
            colors = colors,
            onDismiss = { editorOpen = false },
            onSave = { title, description, date, recurrence ->
                onSaveEvent(editingEvent, title, description, date, recurrence)
                editorOpen = false
            }
        )
    }

    // El mes visible es independiente del día elegido: cambiar de mes no mueve la selección
    var visibleMonth by remember { mutableStateOf(startOfDay(selectedTimestamp).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis) }

    val dayEvents = remember(events, selectedTimestamp) {
        val day = startOfDay(selectedTimestamp)
        val time = Calendar.getInstance()
        events.filter { it.occursOn(day) }.sortedBy {
            time.timeInMillis = it.date
            time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        }
    }
    val nextLine = remember(events) { nextEventLine(events) }
    val today = startOfDay(System.currentTimeMillis())
    val showToday = !isSameDay(startOfDay(selectedTimestamp), today) ||
        startOfDay(visibleMonth).get(Calendar.MONTH) != today.get(Calendar.MONTH) ||
        startOfDay(visibleMonth).get(Calendar.YEAR) != today.get(Calendar.YEAR)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 70.dp)
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Calendario",
                        fontFamily = CalendarVt323,
                        fontSize = 30.sp,
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (showToday) {
                        Text(
                            text = "Hoy",
                            fontFamily = CalendarVt323,
                            fontSize = 18.sp,
                            color = colors.text,
                            modifier = Modifier
                                .border(2.dp, colors.border)
                                .background(colors.card)
                                .clickable {
                                    visibleMonth = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                                    onDateSelected(today.timeInMillis)
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                if (nextLine != null) {
                    Text(text = nextLine, fontFamily = CalendarVt323, fontSize = 17.sp, color = colors.subtitle)
                }
            }
            item(key = "grid") {
                Surface(
                    color = colors.card,
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, colors.border)
                ) {
                    CalendarGrid(
                        visibleMonth = visibleMonth,
                        selectedTime = selectedTimestamp,
                        events = events,
                        colors = colors,
                        onMonthChange = { visibleMonth = it },
                        onDateSelected = onDateSelected
                    )
                }
            }
            item(key = "day_title") {
                Text(
                    text = dayTitle(selectedTimestamp),
                    fontFamily = CalendarVt323,
                    fontSize = 22.sp,
                    color = colors.subtitle
                )
            }
            if (dayEvents.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nada agendado este día",
                        fontFamily = CalendarVt323,
                        fontSize = 18.sp,
                        color = colors.subtitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(dayEvents, key = { it.eventId }) { event ->
                    EventItem(event, colors, { openEditor(it) }, onDeleteEvent)
                }
            }
        }

        Button(
            onClick = { openEditor(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                text = "+ Agendar Cita",
                fontFamily = CalendarVt323,
                fontSize = 21.sp,
                color = colors.onAccent,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    visibleMonth: Long,
    selectedTime: Long,
    events: List<CalendarEvent>,
    colors: CalendarColors,
    onMonthChange: (Long) -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val monthCal = remember(visibleMonth) { Calendar.getInstance().apply { timeInMillis = visibleMonth } }
    val monthTitle = remember(visibleMonth) {
        SimpleDateFormat("MMMM yyyy", Locale("es", "CL")).format(monthCal.time).uppercase(Locale("es", "CL"))
    }
    val daysInMonth = remember(visibleMonth) { monthCal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val startOffset = remember(visibleMonth) { (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 }
    // Autores con citas por día del mes, para dibujar las marcas sin recalcular en cada recomposición
    val marks = remember(visibleMonth, events, colors) {
        val day = Calendar.getInstance().apply { timeInMillis = visibleMonth }
        (1..daysInMonth).associateWith { number ->
            day.set(Calendar.DAY_OF_MONTH, number)
            events.filter { it.occursOn(day) }.map { authorColor(it.authorName, colors) }.distinct().take(3)
        }
    }
    val selected = remember(selectedTime) { Calendar.getInstance().apply { timeInMillis = selectedTime } }
    val today = remember { Calendar.getInstance() }
    val changeMonth = { delta: Int ->
        onMonthChange(Calendar.getInstance().apply {
            timeInMillis = visibleMonth
            add(Calendar.MONTH, delta)
        }.timeInMillis)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("<", fontFamily = CalendarVt323, fontSize = 28.sp, color = colors.text,
                modifier = Modifier.clickable { changeMonth(-1) }.padding(horizontal = 14.dp, vertical = 4.dp))
            Text(monthTitle, fontFamily = CalendarVt323, fontSize = 21.sp, color = colors.text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(">", fontFamily = CalendarVt323, fontSize = 28.sp, color = colors.text,
                modifier = Modifier.clickable { changeMonth(1) }.padding(horizontal = 14.dp, vertical = 4.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_LETTERS.forEach { day ->
                Text(day, modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                    fontFamily = CalendarVt323, fontSize = 15.sp, color = colors.subtitle, textAlign = TextAlign.Center)
            }
        }

        val rows = (daysInMonth + startOffset + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - startOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val sameMonth = selected.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) && selected.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
                        val isSelected = sameMonth && selected.get(Calendar.DAY_OF_MONTH) == dayNumber
                        val isToday = today.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) == dayNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.15f)
                                .border(if (isToday) 2.dp else 1.dp, if (isToday) colors.subtitle else colors.border.copy(alpha = 0.3f))
                                .background(if (isSelected) colors.accent else Color.Transparent)
                                .clickable {
                                    onDateSelected(Calendar.getInstance().apply {
                                        timeInMillis = visibleMonth
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                    }.timeInMillis)
                                },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                fontFamily = CalendarVt323,
                                fontSize = 18.sp,
                                color = if (isSelected) colors.onAccent else colors.text
                            )
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                marks[dayNumber].orEmpty().forEach { mark ->
                                    Box(Modifier.size(6.dp).background(if (isSelected) colors.onAccent else mark))
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1.15f))
                    }
                }
            }
        }
    }
}

private fun recurrenceLabel(recurrence: String): String? = when (recurrence) {
    "DAILY" -> "Diario"
    "WEEKLY" -> "Semanal"
    "MONTHLY" -> "Mensual"
    "YEARLY" -> "Anual"
    else -> null
}

@Composable
private fun EventTag(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = CalendarVt323,
        fontSize = 15.sp,
        color = color,
        modifier = Modifier.border(1.dp, color).padding(horizontal = 5.dp)
    )
}

@Composable
private fun EventItem(
    event: CalendarEvent,
    colors: CalendarColors,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val hour = remember(event.date) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.date)) }
    val repeat = recurrenceLabel(event.recurrence)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, colors.border)
            .background(colors.card)
            .clickable { onEdit(event) }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(hour, fontFamily = CalendarVt323, fontSize = 21.sp, color = colors.subtitle, modifier = Modifier.width(54.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title.ifEmpty { "Sin título" },
                    fontFamily = CalendarVt323,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                if (event.description.isNotEmpty()) {
                    Text(event.description, fontFamily = CalendarVt323, fontSize = 17.sp, color = colors.text.copy(alpha = 0.8f))
                }
                if (event.authorName.isNotEmpty() || repeat != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (event.authorName.isNotEmpty()) EventTag(event.authorName, authorColor(event.authorName, colors))
                        if (repeat != null) EventTag("↻ $repeat", colors.subtitle)
                    }
                }
            }
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(32.dp)) {
                Text("🗑", fontSize = 18.sp, color = colors.text)
            }
        }
        if (confirmDelete) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (repeat != null) "¿Borrar la cita y sus repeticiones?" else "¿Borrar esta cita?",
                    fontFamily = CalendarVt323,
                    fontSize = 17.sp,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                Text("No", fontFamily = CalendarVt323, fontSize = 18.sp, color = colors.text,
                    modifier = Modifier.border(2.dp, colors.border).clickable { confirmDelete = false }.padding(horizontal = 12.dp, vertical = 4.dp))
                Text("Borrar", fontFamily = CalendarVt323, fontSize = 18.sp, color = colors.onAccent,
                    modifier = Modifier.background(colors.accent).border(2.dp, colors.border)
                        .clickable { confirmDelete = false; onDelete(event) }
                        .padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }
}

private val RECURRENCE_OPTIONS = listOf(
    "NONE" to "No repetir",
    "DAILY" to "Diario",
    "WEEKLY" to "Semanal",
    "MONTHLY" to "Mensual",
    "YEARLY" to "Anual"
)

/** Hoja inferior para crear o editar una cita. Una cita repetida conserva su fecha original. */
@Composable
private fun EventEditorSheet(
    event: CalendarEvent?,
    selectedDay: Long,
    colors: CalendarColors,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, date: Long, recurrence: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var recurrence by remember { mutableStateOf(event?.recurrence ?: "NONE") }
    var showTitleError by remember { mutableStateOf(false) }
    var dateTime by remember {
        mutableStateOf(event?.date ?: startOfDay(selectedDay).apply { set(Calendar.HOUR_OF_DAY, 12) }.timeInMillis)
    }
    val hourText = remember(dateTime) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dateTime)) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        focusedBorderColor = colors.border,
        unfocusedBorderColor = colors.border,
        focusedLabelColor = colors.subtitle,
        unfocusedLabelColor = colors.subtitle,
        cursorColor = colors.text
    )
    val fieldStyle = androidx.compose.ui.text.TextStyle(fontFamily = CalendarVt323, fontSize = 20.sp, color = colors.text)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .statusBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card)
                    .border(3.dp, colors.border)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (event == null) "Nueva cita" else "Editar cita", fontFamily = CalendarVt323, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text(dayTitle(dateTime), fontFamily = CalendarVt323, fontSize = 20.sp, color = colors.subtitle)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showTitleError = false },
                    label = { Text("Título de la cita", fontFamily = CalendarVt323, fontSize = 16.sp) },
                    singleLine = true,
                    isError = showTitleError,
                    textStyle = fieldStyle,
                    shape = RectangleShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTitleError) {
                    Text("Escribe un título para guardar la cita", fontFamily = CalendarVt323, fontSize = 16.sp, color = colors.subtitle)
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)", fontFamily = CalendarVt323, fontSize = 16.sp) },
                    textStyle = fieldStyle,
                    shape = RectangleShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hora", fontFamily = CalendarVt323, fontSize = 20.sp, color = colors.subtitle)
                    Text(
                        text = hourText,
                        fontFamily = CalendarVt323,
                        fontSize = 22.sp,
                        color = colors.text,
                        modifier = Modifier
                            .border(2.dp, colors.border)
                            .clickable {
                                val time = Calendar.getInstance().apply { timeInMillis = dateTime }
                                android.app.TimePickerDialog(context, { _, hour, minute ->
                                    time.set(Calendar.HOUR_OF_DAY, hour)
                                    time.set(Calendar.MINUTE, minute)
                                    dateTime = time.timeInMillis
                                }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
                Text("Repetir", fontFamily = CalendarVt323, fontSize = 20.sp, color = colors.subtitle)
                RECURRENCE_OPTIONS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (value, label) ->
                            SheetButton(label, recurrence == value, colors, Modifier.weight(1f)) { recurrence = value }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    SheetButton("Cancelar", false, colors, Modifier.weight(1f), onDismiss)
                    SheetButton("Guardar", true, colors, Modifier.weight(1f)) {
                        val cleanTitle = title.trim()
                        if (cleanTitle.isEmpty()) showTitleError = true
                        else onSave(cleanTitle, description.trim(), dateTime, recurrence)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetButton(label: String, selected: Boolean, colors: CalendarColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = CalendarVt323,
        fontSize = 19.sp,
        textAlign = TextAlign.Center,
        color = if (selected) colors.onAccent else colors.text,
        modifier = modifier
            .border(2.dp, colors.border)
            .background(if (selected) colors.accent else colors.card)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    )
}
