package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.Pet

@Composable
fun PetCaregiverRankingContent(
    pet: Pet,
    isDark: Boolean,
    borderColor: Color,
    contentColor: Color,
    accentColor: Color,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var rankingFilter by remember { mutableStateOf("global") } // "global", "thor", "cuky"

    val kevinPts = when (rankingFilter) {
        "thor" -> pet.getThorCareKevin()
        "cuky" -> pet.getCukyCareKevin()
        else -> pet.getTotalCareKevin()
    }
    val aliPts = when (rankingFilter) {
        "thor" -> pet.getThorCareAli()
        "cuky" -> pet.getCukyCareAli()
        else -> pet.getTotalCareAli()
    }
    val totalPts = (kevinPts + aliPts).coerceAtLeast(1)
    val kevinWins = kevinPts > aliPts
    val aliWins = aliPts > kevinPts
    val isTie = kevinPts == aliPts

    val kevinLevel = pet.getCaregiverLevel(kevinPts)
    val aliLevel = pet.getCaregiverLevel(aliPts)
    val kevinTitle = pet.getCaregiverTitle(kevinPts, false)
    val aliTitle = pet.getCaregiverTitle(aliPts, true)
    val kevinProgress = pet.getCaregiverProgress(kevinPts)
    val aliProgress = pet.getCaregiverProgress(aliPts)
    val kevinTarget = pet.getCaregiverNextLevelTarget(kevinPts)
    val aliTarget = pet.getCaregiverNextLevelTarget(aliPts)

    val kevinPct = ((kevinPts.toFloat() / totalPts) * 100).toInt()
    val aliPct = 100 - kevinPct

    val feedKevin = pet.getFeedCountKevin(rankingFilter)
    val feedAli = pet.getFeedCountAli(rankingFilter)

    val bathKevin = pet.getBathCountKevin(rankingFilter)
    val bathAli = pet.getBathCountAli(rankingFilter)

    val playKevin = pet.getPlayCountKevin(rankingFilter)
    val playAli = pet.getPlayCountAli(rankingFilter)

    val tapKevin = pet.getTapCountKevin(rankingFilter)
    val tapAli = pet.getTapCountAli(rankingFilter)

    val minigameKevin = pet.getMinigameCountKevin(rankingFilter)
    val minigameAli = pet.getMinigameCountAli(rankingFilter)

    var showGuide by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector de Filtro de Cuidado: 🌟 Global | 🐱 Thor | 🐔 Cuky
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                Triple("global", "🌟 GLOBAL", Color(0xFFD97706)),
                Triple("thor", "🐱 THOR", Color(0xFF2563EB)),
                Triple("cuky", "🐔 CUKY", Color(0xFFD97706))
            )

            filters.forEach { (key, label, activeCol) ->
                val isSelected = rankingFilter == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) activeCol else if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8D7C0),
                            shape = RectangleShape
                        )
                        .border(2.dp, if (isSelected) Color.White else borderColor)
                        .clickable { rankingFilter = key }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = if (isSelected) Color.White else contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Título y Estado Actual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8D7C0), shape = RectangleShape)
                .border(2.dp, borderColor)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (rankingFilter) {
                        "thor" -> "🐱 CUIDADOS DE THOR 🐱"
                        "cuky" -> "🐔 CUIDADOS DE CUKY 🐔"
                        else -> "👑 RANKING DE CUIDADORES 👑"
                    },
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isTie -> "🤝 ¡Empate perfecto en cuidados!"
                        kevinWins -> "🥇 Kevin lidera el cuidado con ${kevinPts - aliPts} pts de ventaja"
                        else -> "🥇 Ali lidera el cuidado con ${aliPts - kevinPts} pts de ventaja"
                    },
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    color = contentColor
                )
            }
        }

        // Barra de Batalla Visual (Kevin vs Ali)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF222222) else Color(0xFFF9F5EC), shape = RectangleShape)
                .border(2.dp, borderColor)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kevin
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (kevinWins) Text("👑 ", fontSize = 16.sp)
                    Text("Kevin ($kevinPts pts)", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }
                // Ali
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ali ($aliPts pts)", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                    if (aliWins) Text(" 👑", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barra segmentada bicolor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .border(2.dp, borderColor)
            ) {
                if (kevinPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(kevinPct.toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (kevinPct >= 20) {
                            Text("$kevinPct%", fontFamily = Vt323, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (aliPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(aliPct.toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFFEC4899)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (aliPct >= 20) {
                            Text("$aliPct%", fontFamily = Vt323, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Tarjetas de Nivel y Título de Cada Cuidador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tarjeta Kevin
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF), shape = RectangleShape)
                    .border(2.dp, if (kevinWins) Color(0xFF3B82F6) else borderColor)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👦 KEVIN", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                Text("Nv. $kevinLevel", fontFamily = Vt323, fontSize = 24.sp, color = contentColor, fontWeight = FontWeight.Bold)
                Text(kevinTitle, fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFF3B82F6), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { kevinProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .border(1.dp, borderColor),
                    color = Color(0xFF3B82F6),
                    trackColor = Color.Transparent
                )
                Text("${kevinPts}/$kevinTarget pts", fontFamily = Vt323, fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
            }

            // Tarjeta Ali
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isDark) Color(0xFF3B1E2E) else Color(0xFFFDF2F8), shape = RectangleShape)
                    .border(2.dp, if (aliWins) Color(0xFFEC4899) else borderColor)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👧 ALI", fontFamily = Vt323, fontSize = 18.sp, color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                Text("Nv. $aliLevel", fontFamily = Vt323, fontSize = 24.sp, color = contentColor, fontWeight = FontWeight.Bold)
                Text(aliTitle, fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFEC4899), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { aliProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .border(1.dp, borderColor),
                    color = Color(0xFFEC4899),
                    trackColor = Color.Transparent
                )
                Text("${aliPts}/$aliTarget pts", fontFamily = Vt323, fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
            }
        }

        // Desglose de Actividades de Cuidado
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF222222) else Color(0xFFF9F5EC), shape = RectangleShape)
                .border(2.dp, borderColor)
                .padding(12.dp)
        ) {
            Text(
                text = "📊 DESGLOSE DE CUIDADOS",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RankingCategoryRow(
                title = if (rankingFilter == "cuky") "🌾 Semillas dadas" else "🐟 Comidas dadas",
                kevinValue = feedKevin,
                aliValue = feedAli,
                unit = "veces",
                isDark = isDark,
                contentColor = contentColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 6.dp))

            RankingCategoryRow(
                title = "🛁 Baños realizados",
                kevinValue = bathKevin,
                aliValue = bathAli,
                unit = "veces",
                isDark = isDark,
                contentColor = contentColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 6.dp))

            RankingCategoryRow(
                title = "⚾ Juegos de pelota",
                kevinValue = playKevin,
                aliValue = playAli,
                unit = "veces",
                isDark = isDark,
                contentColor = contentColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 6.dp))

            RankingCategoryRow(
                title = "💖 Caricias y mimos",
                kevinValue = tapKevin,
                aliValue = tapAli,
                unit = "veces",
                isDark = isDark,
                contentColor = contentColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 6.dp))

            RankingCategoryRow(
                title = "🕹️ Minijuegos jugados",
                kevinValue = minigameKevin,
                aliValue = minigameAli,
                unit = "veces",
                isDark = isDark,
                contentColor = contentColor
            )
        }

        // Guía desplegable de Puntos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFEDE0CC), shape = RectangleShape)
                .border(1.dp, borderColor)
                .clickable { showGuide = !showGuide }
                .padding(10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️ ¿Cómo ganar Puntos de Cuidador?", fontFamily = Vt323, fontSize = 16.sp, color = contentColor, fontWeight = FontWeight.Bold)
                    Text(if (showGuide) "▲" else "▼", fontFamily = Vt323, fontSize = 16.sp, color = contentColor)
                }

                if (showGuide) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 🐟 Dar comida o premios: +10 pts de cuidador\n" +
                                "• 🛁 Bañar a la mascota: +10 pts de cuidador\n" +
                                "• ⚾ Lanzar pelota: +15 pts de cuidador\n" +
                                "• 💖 Acariciar en la habitación: +1 pt por mimo (máx 30/día)\n" +
                                "• 🕹️ Jugar minijuegos: +10 pts de cuidador\n" +
                                "• 💌 Enviar cartas en el diario: +5 pts de cuidador\n\n" +
                                "⭐ Rangos de Cuidador:\n" +
                                "🌱 Nv 1: Novato/a (0 - 49 pts)\n" +
                                "🌸 Nv 2: Dedicado/a (50 - 149 pts)\n" +
                                "🐾 Nv 3: Amigo/a de Oro (150 - 299 pts)\n" +
                                "💖 Nv 4: Experto/a (300 - 599 pts)\n" +
                                "⭐ Nv 5: Guardián/a Estelar (600 - 999 pts)\n" +
                                "👑 Nv 6: Leyenda Absoluta (1000+ pts)",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun RankingCategoryRow(
    title: String,
    kevinValue: Int,
    aliValue: Int,
    unit: String,
    isDark: Boolean,
    contentColor: Color
) {
    val kevinLeads = kevinValue > aliValue
    val aliLeads = aliValue > kevinValue

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = Vt323,
            fontSize = 15.sp,
            color = contentColor,
            modifier = Modifier.weight(1.3f)
        )

        Row(
            modifier = Modifier.weight(1.7f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (kevinLeads) "👑 " else ""}Kevin: $kevinValue",
                fontFamily = Vt323,
                fontSize = 14.sp,
                fontWeight = if (kevinLeads) FontWeight.Bold else FontWeight.Normal,
                color = if (kevinLeads) Color(0xFF3B82F6) else if (isDark) Color.LightGray else Color.DarkGray
            )
            Text(
                text = "  |  ",
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "${if (aliLeads) "👑 " else ""}Ali: $aliValue",
                fontFamily = Vt323,
                fontSize = 14.sp,
                fontWeight = if (aliLeads) FontWeight.Bold else FontWeight.Normal,
                color = if (aliLeads) Color(0xFFEC4899) else if (isDark) Color.LightGray else Color.DarkGray
            )
        }
    }
}
