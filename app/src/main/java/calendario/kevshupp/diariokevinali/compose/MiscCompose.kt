package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun MiscScreen(
    theme: String,
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val backgroundColor = getAppBackgroundColor(theme)
    val textColor = if (isDark) Color.White else if (isMono) Color.Black else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else if (isMono) Color.Black else Color(0xFF4A2511)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else if (isMono) Color.White else Color(0xFFFFFBEA)
    
    // View state: "grid" or "checklist"
    var currentView by remember { mutableStateOf("grid") }

    if (currentView == "grid") {
        MiscGridView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = onBack,
            onSelectSpirits = { currentView = "checklist" }
        )
    } else {
        SpiritsChecklistView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    }
}

@Composable
fun MiscGridView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit,
    onSelectSpirits: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current

    // Dynamically retrieve the first spirit icon for the button logo
    val spiritIconId = remember(context) {
        val id = context.resources.getIdentifier("ic_spirit_01", "drawable", context.packageName)
        if (id != 0) id else android.R.drawable.ic_menu_gallery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar/Title Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "MISCELÁNEO",
                fontFamily = Vt323,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // 2x2 Grid Layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First item: active Fortnite Spirits Checklist button
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectSpirits() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = spiritIconId),
                            contentDescription = "Espíritus Fortnite",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Espíritus",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Other 3 slots: placeholders for future activities (locked)
            items(3) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(
                            width = 2.dp,
                            color = borderColor.copy(alpha = 0.4f)
                        )
                        .background(cardBg.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bloqueado",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpiritsChecklistView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    
    // User Session Configuration
    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    val currentUserId = remember(prefs) { prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01" }
    val coupleId = remember(prefs) { prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123" }
    
    // Deterministic identification of roles
    val isKevin = currentUserId == "user_kevin_01"
    
    // Firebase references
    val db = FirebaseFirestore.getInstance()
    var kevinList by remember { mutableStateOf(emptyList<String>()) }
    var aliList by remember { mutableStateOf(emptyList<String>()) }

    // Read real-time values from Firestore
    DisposableEffect(coupleId) {
        val listener = db.collection("fortnite_spirits").document(coupleId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val kList = snapshot.get("kevin_list") as? List<*>
                    val aList = snapshot.get("ali_list") as? List<*>
                    kevinList = kList?.filterIsInstance<String>() ?: emptyList()
                    aliList = aList?.filterIsInstance<String>() ?: emptyList()
                } else {
                    kevinList = emptyList()
                    aliList = emptyList()
                }
            }
        onDispose {
            listener.remove()
        }
    }

    // Toggle check state function
    val onToggleCheck: (String, Boolean, String) -> Unit = { spiritId, currentlyChecked, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_list") kevinList else aliList
        val newList = if (currentlyChecked) {
            currentList.filter { it != spiritId }
        } else {
            currentList + spiritId
        }
        db.collection("fortnite_spirits").document(coupleId)
            .set(mapOf(targetUserKey to newList), SetOptions.merge())
    }

    var showAllList by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }
    var filterMode by remember { mutableStateOf("todos") }
    var showFiltersMenu by remember { mutableStateOf(false) }
    
    val matchesFilter: (String) -> Boolean = { spiritId ->
        when (filterMode) {
            "todos" -> true
            "no_obtenidos" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                !ownedByMe
            }
            "obtenidos_otro_no_yo" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                val ownedByOther = if (isKevin) aliList.contains(spiritId) else kevinList.contains(spiritId)
                ownedByOther && !ownedByMe
            }
            "obtenidos_yo_no_otro" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                val ownedByOther = if (isKevin) aliList.contains(spiritId) else kevinList.contains(spiritId)
                ownedByMe && !ownedByOther
            }
            else -> true
        }
    }

    val categories = remember {
        listOf(
            SpiritCategory("Espíritu de Agua", (1..4).map { String.format("%02d", it) } + listOf("66", "67")),
            SpiritCategory("Espíritu Dormilón", (5..8).map { String.format("%02d", it) } + listOf("68", "69")),
            SpiritCategory("Espíritu de Tierra", (9..12).map { String.format("%02d", it) } + listOf("70", "71")),
            SpiritCategory("Espíritu del Cacahuete", listOf("13")),
            SpiritCategory("Espíritu Demoníaco", (14..17).map { String.format("%02d", it) } + listOf("72", "73")),
            SpiritCategory("Espíritu de Fuego", (18..21).map { String.format("%02d", it) } + listOf("74", "75")),
            SpiritCategory("Espíritu Punk", (22..25).map { String.format("%02d", it) } + listOf("76", "77")),
            SpiritCategory("Espíritu Pato", (26..29).map { String.format("%02d", it) } + listOf("78", "79")),
            SpiritCategory("Espíritu Rey", (30..33).map { String.format("%02d", it) } + listOf("80", "81")),
            SpiritCategory("Espíritu Fantasma", (34..37).map { String.format("%02d", it) } + listOf("82", "83")),
            SpiritCategory("Espíritu del Punto Cero", (38..41).map { String.format("%02d", it) } + listOf("84", "85")),
            SpiritCategory("Espíritu de Aura", (42..45).map { String.format("%02d", it) } + listOf("86", "87")),
            SpiritCategory("Espíritu de la Fundación", (46..49).map { String.format("%02d", it) } + listOf("88", "89")),
            SpiritCategory("Espíritu de la Parca", (50..53).map { String.format("%02d", it) } + listOf("90", "91")),
            SpiritCategory("Espíritu Futbolero", (54..57).map { String.format("%02d", it) } + listOf("92", "93")),
            SpiritCategory("Espíritu Jefe", (58..61).map { String.format("%02d", it) } + listOf("94", "95")),
            SpiritCategory("Espíritu Pescado", (62..65).map { String.format("%02d", it) } + listOf("96", "97"))
        )
    }

    val spiritsList = remember { (1..97).map { String.format("%02d", it) } }
    val spiritNames = remember {
        listOf(
            // Fila 1
            "Espíritu de Agua", "Espíritu de Agua Dorado", "Espíritu de Agua Gomita", "Espíritu de Agua Galaxia",
            "Espíritu Dormilón", "Espíritu Dormilón Dorado", "Espíritu Dormilón Gomita", "Espíritu Dormilón Galaxia",
            // Fila 2
            "Espíritu de Tierra", "Espíritu de Tierra Dorado", "Espíritu de Tierra Gomita", "Espíritu de Tierra Galaxia",
            "TheBurntPeanut (Espíritu del Cacahuete)",
            "Espíritu Demoníaco", "Espíritu Demoníaco Dorado", "Espíritu Demoníaco Gomita", "Espíritu Demoníaco Galaxia",
            // Fila 3
            "Espíritu de Fuego", "Espíritu de Fuego Dorado", "Espíritu de Fuego Gomita", "Espíritu de Fuego Galaxia",
            "Espíritu Punk", "Espíritu Punk Dorado", "Espíritu Punk Gomita", "Espíritu Punk Galaxia",
            // Fila 4
            "Espíritu Pato", "Espíritu Pato Dorado", "Espíritu Pato Gomita", "Espíritu Pato Galaxia",
            "Espíritu Rey", "Espíritu Rey Dorado", "Espíritu Rey Gomita", "Espíritu Rey Galaxia",
            // Fila 5
            "Espíritu Fantasma", "Espíritu Fantasma Dorado", "Espíritu Fantasma Gomita", "Espíritu Fantasma Galaxia",
            "Espíritu del Punto Cero", "Espíritu del Punto Cero Dorado", "Espíritu del Punto Cero Gomita", "Espíritu del Punto Cero Galaxia",
            // Fila 6 (Nuevos)
            "Espíritu de Aura", "Espíritu de Aura Dorado", "Espíritu de Aura Galaxia", "Espíritu de Aura Gomita",
            "Espíritu de la Fundación", "Espíritu de la Fundación Dorado", "Espíritu de la Fundación Galaxia", "Espíritu de la Fundación Gomita",
            // Fila 7 (Nuevos)
            "Espíritu de la Parca", "Espíritu de la Parca Dorado", "Espíritu de la Parca Galaxia", "Espíritu de la Parca Gomita",
            "Espíritu Futbolero", "Espíritu Futbolero Dorado", "Espíritu Futbolero Galaxia", "Espíritu Futbolero Gomita",
            // Fila 8 (Nuevos)
            "Espíritu Jefe", "Espíritu Jefe Dorado", "Espíritu Jefe Galaxia", "Espíritu Jefe Gomita",
            "Espíritu Pescado", "Espíritu Pescado Dorado", "Espíritu Pescado Galaxia", "Espíritu Pescado Gomita",
            // Nuevas Variantes (Gema y Holofoil)
            "Espíritu de Agua Gema", "Espíritu de Agua Holofoil",
            "Espíritu Dormilón Gema", "Espíritu Dormilón Holofoil",
            "Espíritu de Tierra Gema", "Espíritu de Tierra Holofoil",
            "Espíritu Demoníaco Gema", "Espíritu Demoníaco Holofoil",
            "Espíritu de Fuego Gema", "Espíritu de Fuego Holofoil",
            "Espíritu Punk Gema", "Espíritu Punk Holofoil",
            "Espíritu Pato Gema", "Espíritu Pato Holofoil",
            "Espíritu Rey Gema", "Espíritu Rey Holofoil",
            "Espíritu Fantasma Gema", "Espíritu Fantasma Holofoil",
            "Espíritu del Punto Cero Gema", "Espíritu del Punto Cero Holofoil",
            "Espíritu de Aura Gema", "Espíritu de Aura Holofoil",
            "Espíritu de la Fundación Gema", "Espíritu de la Fundación Holofoil",
            "Espíritu de la Parca Gema", "Espíritu de la Parca Holofoil",
            "Espíritu Futbolero Gema", "Espíritu Futbolero Holofoil",
            "Espíritu Jefe Gema", "Espíritu Jefe Holofoil",
            "Espíritu Pescado Gema", "Espíritu Pescado Holofoil"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "COLECCIÓN ESPÍRITUS",
                fontFamily = Vt323,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (showAllList) "[GRUPOS]" else "[LISTA]",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { showAllList = !showAllList }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                Text(
                    text = "⋮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor,
                    modifier = Modifier
                        .clickable { showFiltersMenu = true }
                        .padding(8.dp)
                )
                DropdownMenu(
                    expanded = showFiltersMenu,
                    onDismissRequest = { showFiltersMenu = false },
                    modifier = Modifier.background(cardBg).border(2.dp, borderColor)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "todos") "✓ TODOS" else "TODOS",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "todos"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "no_obtenidos") "✓ FALTANTES MÍOS" else "FALTANTES MÍOS",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "no_obtenidos"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "obtenidos_otro_no_yo") "✓ DEL OTRO QUE NO TENGO" else "DEL OTRO QUE NO TENGO",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "obtenidos_otro_no_yo"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "obtenidos_yo_no_otro") "✓ MÍOS QUE EL OTRO NO TIENE" else "MÍOS QUE EL OTRO NO TIENE",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "obtenidos_yo_no_otro"
                            showFiltersMenu = false
                        }
                    )
                }
            }
        }

        // Gamified Scoreboard Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(cardBg)
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Kevin",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${kevinList.size} / ${spiritsList.size}",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFBD93F9) else Color(0xFF4A2511)
                        )
                    }
                    
                    Text(
                        text = "VS",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ali",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${aliList.size} / ${spiritsList.size}",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFFF79C6) else Color(0xFF4A2511)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cute Leaderboard message
                val leaderMsg = when {
                    kevinList.size > aliList.size -> "¡Kevin va a la cabeza!"
                    aliList.size > kevinList.size -> "¡Ali va a la cabeza!"
                    kevinList.size > 0 -> "¡Están empatados!"
                    else -> "¡Empiecen su colección!"
                }

                Text(
                    text = leaderMsg,
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checklist Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            val totalFiltered = spiritsList.count { matchesFilter(it) }
            if (totalFiltered == 0) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay espíritus que coincidan con el filtro.",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (showAllList) {
                val filteredSpirits = spiritsList.filter { matchesFilter(it) }
                items(filteredSpirits, key = { "spirit_$it" }) { spiritId ->
                    val nameIndex = spiritId.toInt() - 1
                    val hasKevin = kevinList.contains(spiritId)
                    val hasAli = aliList.contains(spiritId)
                    val spiritResId = remember(spiritId, context) {
                        val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                        if (id != 0) id else android.R.drawable.ic_menu_gallery
                    }
                    SpiritRow(
                        spiritId = spiritId,
                        spiritResId = spiritResId,
                        spiritName = spiritNames.getOrElse(nameIndex) { "Espíritu #$spiritId" },
                        hasKevin = hasKevin,
                        hasAli = hasAli,
                        isKevin = isKevin,
                        isDark = isDark,
                        borderColor = borderColor,
                        textColor = textColor,
                        cardBg = cardBg,
                        onToggleCheck = onToggleCheck
                    )
                }
            } else {
                categories.forEach { category ->
                    val filteredCategorySpiritIds = category.spiritIds.filter { matchesFilter(it) }
                    if (filteredCategorySpiritIds.isNotEmpty()) {
                        val isExpanded = expandedCategories.contains(category.name)
                        val categoryKevinCount = category.spiritIds.count { kevinList.contains(it) }
                        val categoryAliCount = category.spiritIds.count { aliList.contains(it) }
                        
                        item(key = category.name) {
                            CategoryHeader(
                                category = category,
                                isExpanded = isExpanded,
                                borderColor = borderColor,
                                cardBg = cardBg,
                                textColor = textColor,
                                kevinCount = categoryKevinCount,
                                aliCount = categoryAliCount,
                                onClick = {
                                    expandedCategories = if (isExpanded) {
                                        expandedCategories - category.name
                                    } else {
                                        expandedCategories + category.name
                                    }
                                }
                            )
                        }
                        
                        if (isExpanded) {
                            items(filteredCategorySpiritIds, key = { "spirit_$it" }) { spiritId ->
                                val index = spiritId.toInt() - 1
                                val hasKevin = kevinList.contains(spiritId)
                                val hasAli = aliList.contains(spiritId)
                                val spiritResId = remember(spiritId, context) {
                                    val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                    if (id != 0) id else android.R.drawable.ic_menu_gallery
                                }
                                SpiritRow(
                                    spiritId = spiritId,
                                    spiritResId = spiritResId,
                                    spiritName = spiritNames.getOrElse(index) { "Espíritu #$spiritId" },
                                    hasKevin = hasKevin,
                                    hasAli = hasAli,
                                    isKevin = isKevin,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    cardBg = cardBg,
                                    onToggleCheck = onToggleCheck
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroCheckbox(
    checked: Boolean,
    enabled: Boolean,
    borderColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val boxBg = if (checked) Color(0xFF81C784) else Color(0x22000000)
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.5f))
            .background(boxBg)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(
                text = "X",
                fontFamily = Vt323,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

data class SpiritCategory(
    val name: String,
    val spiritIds: List<String>
)

@Composable
fun CategoryHeader(
    category: SpiritCategory,
    isExpanded: Boolean,
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    kevinCount: Int,
    aliCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isExpanded) "v" else ">",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = category.name,
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "K: $kevinCount/${category.spiritIds.size} | A: $aliCount/${category.spiritIds.size}",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SpiritRow(
    spiritId: String,
    spiritResId: Int,
    spiritName: String,
    hasKevin: Boolean,
    hasAli: Boolean,
    isKevin: Boolean,
    isDark: Boolean,
    borderColor: Color,
    textColor: Color,
    cardBg: Color,
    onToggleCheck: (String, Boolean, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(1.dp, borderColor.copy(alpha = 0.5f))
                .background(if (isDark) Color(0xFF121212) else Color(0xFFFFFDF5)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = spiritResId),
                contentDescription = "Espíritu $spiritId",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = spiritName,
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = "K",
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.7f)
            )
            RetroCheckbox(
                checked = hasKevin,
                enabled = isKevin,
                borderColor = borderColor,
                onCheckedChange = { onToggleCheck(spiritId, hasKevin, "kevin_list") }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = "A",
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.7f)
            )
            RetroCheckbox(
                checked = hasAli,
                enabled = !isKevin,
                borderColor = borderColor,
                onCheckedChange = { onToggleCheck(spiritId, hasAli, "ali_list") }
            )
        }
    }
}
