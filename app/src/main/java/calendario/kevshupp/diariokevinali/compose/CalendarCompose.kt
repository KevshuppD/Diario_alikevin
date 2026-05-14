package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.CalendarEvent
import calendario.kevshupp.diariokevinali.R
import java.text.SimpleDateFormat
import java.util.*

val CalendarVt323 = FontFamily(Font(R.font.vt323_regular))

@Composable
fun CalendarScreen(
    isDark: Boolean,
    events: List<CalendarEvent>,
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF0D1E14) else Color(0xFFFBF2E3)
    val textColor = if (isDark) Color(0xFFEEF3EA) else Color(0xFF111111)
    val cardColor = if (isDark) Color(0xFF1E3B26) else Color(0xFFF4E9D2)
    val borderColor = if (isDark) Color(0xFFEEF3EA) else Color(0xFF111111)
    val buttonColor = if (isDark) Color(0xFF5C20FF) else Color(0xFF4D00E5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "CALENDARIO",
            fontFamily = CalendarVt323,
            fontSize = 32.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = cardColor,
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxWidth()
                .border(4.dp, borderColor, RectangleShape)
                .padding(8.dp)
        ) {
            CalendarGrid(selectedTimestamp, onDateSelected, isDark, borderColor)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Eventos para hoy:",
            fontFamily = CalendarVt323,
            fontSize = 22.sp,
            color = textColor,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val dayEvents = events.filter {
                val cal1 = Calendar.getInstance().apply { timeInMillis = it.date }
                val cal2 = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
            }
            if (dayEvents.isEmpty()) {
                item {
                    Text(
                        text = "No hay eventos para hoy",
                        fontFamily = CalendarVt323,
                        fontSize = 18.sp,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(dayEvents) { event ->
                    EventItem(event, isDark, borderColor, onEditEvent, onDeleteEvent)
                }
            }
        }

        Button(
            onClick = { onAddEvent() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                text = "Agendar Cita",
                fontFamily = CalendarVt323,
                fontSize = 20.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CalendarGrid(selectedTime: Long, onDateSelected: (Long) -> Unit, isDark: Boolean, borderColor: Color) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedTime }
    val currentMonthYear = SimpleDateFormat("MMMM 'de' yyyy", Locale.getDefault()).format(cal.time).uppercase(Locale.getDefault())
    val textColor = if (isDark) Color(0xFFEEF3EA) else Color(0xFF111111)
    val cellColor = if (isDark) Color(0xFF26412B) else Color(0xFFF9F0DD)
    val cellSelectedColor = if (isDark) Color(0xFF5C20FF) else Color(0xFF4D00E5)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonthOffset = Calendar.getInstance().apply {
        timeInMillis = selectedTime
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) // 1=Dom, 2=Lun...

    // Ajustar a Lunes inicio (ISO)
    val startOffset = (firstDayOfMonthOffset + 5) % 7 

    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera mes
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("<", fontFamily = CalendarVt323, fontSize = 28.sp, color = textColor,
                modifier = Modifier.clickable { 
                    val newCal = Calendar.getInstance().apply { 
                        timeInMillis = selectedTime
                        add(Calendar.MONTH, -1)
                    }
                    onDateSelected(newCal.timeInMillis)
                }.padding(8.dp))

            Text(currentMonthYear, fontFamily = CalendarVt323, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))

            Text(">", fontFamily = CalendarVt323, fontSize = 28.sp, color = textColor,
                modifier = Modifier.clickable { 
                    val newCal = Calendar.getInstance().apply { 
                        timeInMillis = selectedTime
                        add(Calendar.MONTH, 1)
                    }
                    onDateSelected(newCal.timeInMillis)
                }.padding(8.dp))
        }

        // Días de la semana
        Row(modifier = Modifier.fillMaxWidth().border(1.dp, borderColor)) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                Text(day, modifier = Modifier.weight(1f).padding(vertical = 4.dp).border(0.5.dp, borderColor.copy(alpha = 0.2f)), 
                    fontFamily = CalendarVt323, fontSize = 14.sp, color = textColor, textAlign = TextAlign.Center)
            }
        }

        // Grid de números
        val totalCells = ((daysInMonth + startOffset + 6) / 7) * 7
        for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIdx = row * 7 + col
                    val dayNumber = dayIdx - startOffset + 1
                    
                    if (dayNumber in 1..daysInMonth) {
                        val isSelected = (dayNumber == cal.get(Calendar.DAY_OF_MONTH))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(1.dp, borderColor)
                                .background(if (isSelected) cellSelectedColor else Color.Transparent)
                                .clickable {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedTime
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                    }
                                    onDateSelected(newCal.timeInMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                fontFamily = CalendarVt323,
                                fontSize = 16.sp,
                                color = if (isSelected) Color.White else textColor
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).border(0.5.dp, borderColor.copy(alpha = 0.1f)))
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(
    event: CalendarEvent,
    isDark: Boolean,
    borderColor: Color,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF162916) else Color(0xFFFFF5DF)
    val textColor = if (isDark) Color(0xFFEEF3EA) else Color(0xFF111111)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(backgroundColor)
            .clickable { onEdit(event) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = event.title ?: "Sin título",
                fontFamily = CalendarVt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onDelete(event) }, modifier = Modifier.size(24.dp)) {
                Text("🗑", fontSize = 18.sp, color = textColor)
            }
        }
        if (!event.description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.description!!,
                fontFamily = CalendarVt323,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
