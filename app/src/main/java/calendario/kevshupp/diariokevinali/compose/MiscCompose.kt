package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import calendario.kevshupp.diariokevinali.DiarioApp
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
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
    
    // View state: "grid", "checklist", "anime"
    var currentView by remember { mutableStateOf("grid") }

    if (currentView == "grid") {
        MiscGridView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = onBack,
            onSelectSpirits = { currentView = "checklist" },
            onSelectAnime = { currentView = "anime" }
        )
    } else if (currentView == "checklist") {
        SpiritsChecklistView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else {
        AnimeDashboardView(
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
    onSelectSpirits: () -> Unit,
    onSelectAnime: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    var showWebDialog by remember { mutableStateOf(false) }

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

            // Second item: Anime section
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectAnime() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📺",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Anime",
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

            // Third item: Web manager section
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { showWebDialog = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🌐",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Web Gestión",
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

            // Fourth item: placeholder for future activities (locked)
            item {
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

    if (showWebDialog) {
        AlertDialog(
            onDismissRequest = { showWebDialog = false },
            containerColor = Color(0xFF161822),
            title = {
                Text(
                    text = "🌐 WEB DE GESTIÓN",
                    fontFamily = Vt323,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Puedes gestionar todos tus espíritus de forma visual y rápida (cambiar nombres, categorías y tipos arrastrando fotos) desde tu celular o computadora abriendo la web:\n\nhttps://kevshuppd.github.io/Diario_alikevin/",
                    fontFamily = Vt323,
                    fontSize = 19.sp,
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981))
                            .border(2.dp, Color(0xFF34D399))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kevshuppd.github.io/Diario_alikevin/"))
                                context.startActivity(intent)
                                showWebDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "ABRIR WEB",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF334155))
                            .border(2.dp, Color(0xFF64748B))
                            .clickable { showWebDialog = false }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "CERRAR",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        )
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
    
    val defaultCategories = remember {
        listOf(
            SpiritCategory("Espíritu de Batman", (98..104).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de Agua", (1..4).map { String.format("%02d", it) } + listOf("66", "67", "112")),
            SpiritCategory("Espíritu de Tierra", (9..12).map { String.format("%02d", it) } + listOf("70", "71", "114")),
            SpiritCategory("Espíritu de Fuego", (18..21).map { String.format("%02d", it) } + listOf("74", "75", "116")),
            SpiritCategory("Espíritu Pato", (26..29).map { String.format("%02d", it) } + listOf("78", "79", "118")),
            SpiritCategory("Espíritu Fantasma", (34..37).map { String.format("%02d", it) } + listOf("82", "83")),
            SpiritCategory("Espíritu Dormilón", (5..8).map { String.format("%02d", it) } + listOf("68", "69", "113")),
            SpiritCategory("Espíritu Demoníaco", (14..17).map { String.format("%02d", it) } + listOf("72", "73", "115")),
            SpiritCategory("Espíritu Punk", (22..25).map { String.format("%02d", it) } + listOf("76", "77", "117")),
            SpiritCategory("Espíritu Monarca", (30..33).map { String.format("%02d", it) } + listOf("80", "81")),
            SpiritCategory("Espíritu del Punto Cero", (38..41).map { String.format("%02d", it) } + listOf("84", "85")),
            SpiritCategory("Espíritu Pescado", (62..65).map { String.format("%02d", it) } + listOf("96", "97")),
            SpiritCategory("Espíritu Futbolero", (54..57).map { String.format("%02d", it) } + listOf("92", "93")),
            SpiritCategory("Espíritu de Aura", (42..45).map { String.format("%02d", it) } + listOf("86", "87")),
            SpiritCategory("Espíritu Jefe", (58..61).map { String.format("%02d", it) } + listOf("94", "95")),
            SpiritCategory("Espíritu de la Parca", (50..53).map { String.format("%02d", it) } + listOf("90", "91")),
            SpiritCategory("Espíritu de Viento", (105..111).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de la Fundación", (46..49).map { String.format("%02d", it) } + listOf("88", "89")),
            SpiritCategory("Espíritu Especial/Invitado", listOf("13") + (119..121).map { String.format("%02d", it) })
        )
    }
    val defaultSpiritsList = remember { (1..121).map { String.format("%02d", it) } }

    var categories by remember { mutableStateOf(defaultCategories) }
    var spiritsList by remember { mutableStateOf(defaultSpiritsList) }

    // Firebase references
    val db = FirebaseFirestore.getInstance()
    var kevinList by remember { mutableStateOf(emptyList<String>()) }
    var aliList by remember { mutableStateOf(emptyList<String>()) }
    var kevinMastery by remember { mutableStateOf(emptyList<String>()) }
    var aliMastery by remember { mutableStateOf(emptyList<String>()) }

    var customNames by remember { mutableStateOf(emptyMap<String, String>()) }
    var customCategories by remember { mutableStateOf(emptyMap<String, String>()) }
    var isEditMode by remember { mutableStateOf(false) }

    var editingSpiritId by remember { mutableStateOf<String?>(null) }
    var editingSpiritInitialName by remember { mutableStateOf("") }
    
    var editingCategoryOriginalName by remember { mutableStateOf<String?>(null) }
    var editingCategoryInitialName by remember { mutableStateOf("") }

    var deletingSpiritId by remember { mutableStateOf<String?>(null) }
    var deletingCategoryName by remember { mutableStateOf<String?>(null) }

    // Read real-time values from Firestore
    DisposableEffect(coupleId) {
        val listener = db.collection("fortnite_spirits").document(coupleId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val kList = snapshot.get("kevin_list") as? List<*>
                    val aList = snapshot.get("ali_list") as? List<*>
                    val kMastery = snapshot.get("kevin_mastery") as? List<*>
                    val aMastery = snapshot.get("ali_mastery") as? List<*>
                    kevinList = kList?.filterIsInstance<String>() ?: emptyList()
                    aliList = aList?.filterIsInstance<String>() ?: emptyList()
                    kevinMastery = kMastery?.filterIsInstance<String>() ?: emptyList()
                    aliMastery = aMastery?.filterIsInstance<String>() ?: emptyList()

                    val cNames = snapshot.get("custom_names") as? Map<*, *>
                    val cCategories = snapshot.get("custom_categories") as? Map<*, *>
                    customNames = cNames?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
                    customCategories = cCategories?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()

                    val dbSpiritsList = snapshot.get("spirits_list") as? List<*>
                    val parsedSpiritsList = dbSpiritsList?.filterIsInstance<String>() ?: emptyList()

                    val dbCategories = snapshot.get("categories") as? List<*>
                    val parsedCategories = dbCategories?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val name = map["name"] as? String ?: return@mapNotNull null
                        val spiritIds = (map["spiritIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        SpiritCategory(name, spiritIds)
                    } ?: emptyList()

                    val schemaVersion = (snapshot.get("schema_version") as? Number)?.toInt() ?: 1

                    val isFromCache = snapshot.metadata.isFromCache

                    if (parsedSpiritsList.isEmpty()) {
                        if (!isFromCache) {
                            // New document or empty list, write default values to Firestore!
                            val docRef = db.collection("fortnite_spirits").document(coupleId)
                            val updates = hashMapOf<String, Any>(
                                "categories" to defaultCategories.map { cat ->
                                    hashMapOf("name" to cat.name, "spiritIds" to cat.spiritIds)
                                },
                                "spirits_list" to defaultSpiritsList,
                                "schema_version" to 3
                            )
                            docRef.set(updates, SetOptions.merge())
                        }
                        categories = defaultCategories
                        spiritsList = defaultSpiritsList
                    } else if (schemaVersion < 3) {
                        if (!isFromCache) {
                            // Firestore document is outdated (schema_version < 3). Migrate it to 121!
                            val migrateSelections = { oldList: List<String> ->
                                oldList.map { id ->
                                    val num = id.toIntOrNull() ?: return@map id
                                    val newNum = when {
                                        num in 117..119 -> num + 2
                                        num in 110..116 -> num + 2
                                        num in 104..109 -> num + 1
                                        else -> num
                                    }
                                    String.format("%02d", newNum)
                                }.distinct()
                            }

                            val migratedKevin = if (schemaVersion == 1) migrateSelections(kevinList) else kevinList
                            val migratedAli = if (schemaVersion == 1) migrateSelections(aliList) else aliList
                            val migratedKevinMastery = if (schemaVersion == 1) migrateSelections(kevinMastery) else kevinMastery
                            val migratedAliMastery = if (schemaVersion == 1) migrateSelections(aliMastery) else aliMastery

                            val docRef = db.collection("fortnite_spirits").document(coupleId)
                            val updates = hashMapOf<String, Any>(
                                "categories" to defaultCategories.map { cat ->
                                    hashMapOf("name" to cat.name, "spiritIds" to cat.spiritIds)
                                },
                                "spirits_list" to defaultSpiritsList,
                                "kevin_list" to migratedKevin,
                                "ali_list" to migratedAli,
                                "kevin_mastery" to migratedKevinMastery,
                                "ali_mastery" to migratedAliMastery,
                                "schema_version" to 3
                            )
                            docRef.update(updates)
                            categories = defaultCategories
                            spiritsList = defaultSpiritsList
                        } else {
                            // Cache is outdated, display temporarily but wait for server to migrate/load
                            categories = parsedCategories.ifEmpty { defaultCategories }
                            spiritsList = parsedSpiritsList
                        }
                    } else {
                        // Firestore document is up to date, use its values
                        categories = parsedCategories.ifEmpty { defaultCategories }
                        spiritsList = parsedSpiritsList
                    }
                } else {
                    kevinList = emptyList()
                    aliList = emptyList()
                    kevinMastery = emptyList()
                    aliMastery = emptyList()
                    customNames = emptyMap()
                    customCategories = emptyMap()
                    categories = defaultCategories
                    spiritsList = defaultSpiritsList
                }
            }
        onDispose {
            listener.remove()
        }
    }

    val onSaveSpiritChanges: (String, String, String) -> Unit = { spiritId, newName, targetCategoryName ->
        val newCustomNames = if (newName.isBlank()) {
            customNames - spiritId
        } else {
            customNames + (spiritId to newName)
        }

        val updatedCategories = categories.map { category ->
            val updatedIds = if (category.name == targetCategoryName) {
                if (!category.spiritIds.contains(spiritId)) {
                    category.spiritIds + spiritId
                } else {
                    category.spiritIds
                }
            } else {
                category.spiritIds.filter { it != spiritId }
            }
            SpiritCategory(category.name, updatedIds)
        }

        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }

        val updates = mapOf(
            "custom_names" to newCustomNames,
            "categories" to categoriesMap,
            "schema_version" to 3
        )
        db.collection("fortnite_spirits").document(coupleId)
            .set(updates, SetOptions.merge())
    }

    val onSaveCategoryName: (String, String) -> Unit = { originalName, newName ->
        val newCustomCategories = if (newName.isBlank()) {
            customCategories - originalName
        } else {
            customCategories + (originalName to newName)
        }
        db.collection("fortnite_spirits").document(coupleId)
            .set(mapOf("custom_categories" to newCustomCategories, "schema_version" to 3), SetOptions.merge())
    }

    val onDeleteSpirit: (String) -> Unit = { spiritId ->
        val updatedSpiritsList = spiritsList.filter { it != spiritId }
        val updatedCategories = categories.map { category ->
            SpiritCategory(category.name, category.spiritIds.filter { it != spiritId })
        }
        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }
        val newCustomNames = customNames - spiritId
        val newKevinList = kevinList.filter { it != spiritId }
        val newAliList = aliList.filter { it != spiritId }
        val newKevinMastery = kevinMastery.filter { it != spiritId }
        val newAliMastery = aliMastery.filter { it != spiritId }

        val updates = mapOf(
            "spirits_list" to updatedSpiritsList,
            "categories" to categoriesMap,
            "custom_names" to newCustomNames,
            "kevin_list" to newKevinList,
            "ali_list" to newAliList,
            "kevin_mastery" to newKevinMastery,
            "ali_mastery" to newAliMastery,
            "schema_version" to 3
        )
        db.collection("fortnite_spirits").document(coupleId)
            .set(updates, SetOptions.merge())
    }

    val onDeleteCategory: (String) -> Unit = { categoryName ->
        val updatedCategories = categories.filter { it.name != categoryName }
        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }
        val newCustomCategories = customCategories - categoryName
        val updates = mapOf(
            "categories" to categoriesMap,
            "custom_categories" to newCustomCategories,
            "schema_version" to 3
        )
        db.collection("fortnite_spirits").document(coupleId)
            .set(updates, SetOptions.merge())
    }

    // Toggle check state function
    val onToggleCheck: (String, Boolean, String) -> Unit = { spiritId, currentlyChecked, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_list") kevinList else aliList
        val newList = if (currentlyChecked) {
            currentList.filter { it != spiritId }
        } else {
            currentList + spiritId
        }
        val updates = mutableMapOf<String, Any>(targetUserKey to newList)
        if (currentlyChecked) {
            val masteryKey = if (targetUserKey == "kevin_list") "kevin_mastery" else "ali_mastery"
            val currentMastery = if (targetUserKey == "kevin_list") kevinMastery else aliMastery
            val newMastery = currentMastery.filter { it != spiritId }
            updates[masteryKey] = newMastery
        }
        db.collection("fortnite_spirits").document(coupleId)
            .set(updates, SetOptions.merge())
    }

    // Toggle mastery state function
    val onToggleMastery: (String, Boolean, String) -> Unit = { spiritId, currentlyMastered, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_mastery") kevinMastery else aliMastery
        val newList = if (currentlyMastered) {
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
            "Espíritu Monarca", "Espíritu Monarca Dorado", "Espíritu Monarca Gomita", "Espíritu Monarca Galaxia",
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
            "Espíritu Monarca Gema", "Espíritu Monarca Holofoil",
            "Espíritu Fantasma Gema", "Espíritu Fantasma Holofoil",
            "Espíritu del Punto Cero Gema", "Espíritu del Punto Cero Holofoil",
            "Espíritu de Aura Gema", "Espíritu de Aura Holofoil",
            "Espíritu de la Fundación Gema", "Espíritu de la Fundación Holofoil",
            "Espíritu de la Parca Gema", "Espíritu de la Parca Holofoil",
            "Espíritu Futbolero Gema", "Espíritu Futbolero Holofoil",
            "Espíritu Jefe Gema", "Espíritu Jefe Holofoil",
            "Espíritu Pescado Gema", "Espíritu Pescado Holofoil",
            // Nuevos Espíritus (98..121)
            // Fila 9 (Batman)
            "Espíritu de Batman", "Espíritu de Batman Dorado", "Espíritu de Batman Gomita", "Espíritu de Batman Galaxia", "Espíritu de Batman Gema", "Espíritu de Batman Holofoil", "Espíritu de Batman Cubo",
            // Fila 10 (Viento)
            "Espíritu de Viento", "Espíritu de Viento Dorado", "Espíritu de Viento Gomita", "Espíritu de Viento Galaxia", "Espíritu de Viento Gema", "Espíritu de Viento Holofoil", "Espíritu de Viento Cubo",
            // Fila 11 (Cubo)
            "Espíritu de Agua Cubo", "Espíritu Dormilón Cubo", "Espíritu de Tierra Cubo", "Espíritu Demoníaco Cubo", "Espíritu de Fuego Cubo", "Espíritu Punk Cubo", "Espíritu Pato Cubo",
            // Fila 12 (Especiales / Invitados)
            "Espíritu Pollo", "Espíritu de Vini Jr.", "Espíritu de la Fundación Especial"
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
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isEditMode) "✓ MODO EDICIÓN" else "MODO EDICIÓN",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            isEditMode = !isEditMode
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👑 ${kevinMastery.size}",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFFF1FA8C) else Color(0xFFD4AF37)
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👑 ${aliMastery.size}",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFFF1FA8C) else Color(0xFFD4AF37)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cute Leaderboard message
                val leaderMsg = when {
                    kevinList.size > aliList.size -> "¡Kevin va a la cabeza!"
                    aliList.size > kevinList.size -> "¡Ali va a la cabeza!"
                    kevinList.size == aliList.size && kevinList.size > 0 -> {
                        when {
                            kevinMastery.size > aliMastery.size -> "¡Kevin lidera en maestría!"
                            aliMastery.size > kevinMastery.size -> "¡Ali lidera en maestría!"
                            else -> "¡Están empatados!"
                        }
                    }
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
                    val hasKevinMastery = kevinMastery.contains(spiritId)
                    val hasAliMastery = aliMastery.contains(spiritId)
                    val spiritResId = remember(spiritId, context) {
                        val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                        if (id != 0) id else android.R.drawable.ic_menu_gallery
                    }
                    val currentName = customNames[spiritId] ?: spiritNames.getOrElse(nameIndex) { "Espíritu #$spiritId" }
                    SpiritRow(
                        spiritId = spiritId,
                        spiritResId = spiritResId,
                        spiritName = currentName,
                        hasKevin = hasKevin,
                        hasAli = hasAli,
                        hasKevinMastery = hasKevinMastery,
                        hasAliMastery = hasAliMastery,
                        isKevin = isKevin,
                        isDark = isDark,
                        borderColor = borderColor,
                        textColor = textColor,
                        cardBg = cardBg,
                        isEditMode = isEditMode,
                        onEditName = {
                            editingSpiritId = spiritId
                            editingSpiritInitialName = currentName
                        },
                        onDeleteSpirit = {
                            deletingSpiritId = spiritId
                        },
                        onToggleCheck = onToggleCheck,
                        onToggleMastery = onToggleMastery
                    )
                }
            } else {
                categories.forEach { category ->
                    val filteredCategorySpiritIds = category.spiritIds.filter { matchesFilter(it) }
                    if (filteredCategorySpiritIds.isNotEmpty()) {
                        val isExpanded = expandedCategories.contains(category.name)
                        val categoryKevinCount = category.spiritIds.count { kevinList.contains(it) }
                        val categoryAliCount = category.spiritIds.count { aliList.contains(it) }
                        val categoryKevinMasteryCount = category.spiritIds.count { kevinMastery.contains(it) }
                        val categoryAliMasteryCount = category.spiritIds.count { aliMastery.contains(it) }
                        val displayCategoryName = customCategories[category.name] ?: category.name
                        
                        item(key = category.name) {
                            CategoryHeader(
                                category = category,
                                displayName = displayCategoryName,
                                isExpanded = isExpanded,
                                borderColor = borderColor,
                                cardBg = cardBg,
                                textColor = textColor,
                                kevinCount = categoryKevinCount,
                                aliCount = categoryAliCount,
                                kevinMasteryCount = categoryKevinMasteryCount,
                                aliMasteryCount = categoryAliMasteryCount,
                                isEditMode = isEditMode,
                                onEditCategoryName = {
                                    editingCategoryOriginalName = category.name
                                    editingCategoryInitialName = displayCategoryName
                                },
                                onDeleteCategory = {
                                    deletingCategoryName = category.name
                                },
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
                                val hasKevinMastery = kevinMastery.contains(spiritId)
                                val hasAliMastery = aliMastery.contains(spiritId)
                                val spiritResId = remember(spiritId, context) {
                                    val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                    if (id != 0) id else android.R.drawable.ic_menu_gallery
                                }
                                val currentName = customNames[spiritId] ?: spiritNames.getOrElse(index) { "Espíritu #$spiritId" }
                                SpiritRow(
                                    spiritId = spiritId,
                                    spiritResId = spiritResId,
                                    spiritName = currentName,
                                    hasKevin = hasKevin,
                                    hasAli = hasAli,
                                    hasKevinMastery = hasKevinMastery,
                                    hasAliMastery = hasAliMastery,
                                    isKevin = isKevin,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    cardBg = cardBg,
                                    isEditMode = isEditMode,
                                    onEditName = {
                                        editingSpiritId = spiritId
                                        editingSpiritInitialName = currentName
                                    },
                                    onDeleteSpirit = {
                                        deletingSpiritId = spiritId
                                    },
                                    onToggleCheck = onToggleCheck,
                                    onToggleMastery = onToggleMastery
                                )
                            }
                        }
                    }
                }

                // Render "Sin Categoría / Sueltos" Virtual Category at the bottom!
                val categorizedIds = categories.flatMap { it.spiritIds }.toSet()
                val uncategorizedIds = spiritsList.filter { !categorizedIds.contains(it) && matchesFilter(it) }
                if (uncategorizedIds.isNotEmpty()) {
                    val virtualCategoryName = "Sin Categoría / Sueltos"
                    val isExpanded = expandedCategories.contains(virtualCategoryName)
                    val categoryKevinCount = uncategorizedIds.count { kevinList.contains(it) }
                    val categoryAliCount = uncategorizedIds.count { aliList.contains(it) }
                    val categoryKevinMasteryCount = uncategorizedIds.count { kevinMastery.contains(it) }
                    val categoryAliMasteryCount = uncategorizedIds.count { aliMastery.contains(it) }
                    
                    item(key = virtualCategoryName) {
                        CategoryHeader(
                            category = SpiritCategory(virtualCategoryName, uncategorizedIds),
                            displayName = virtualCategoryName,
                            isExpanded = isExpanded,
                            borderColor = borderColor,
                            cardBg = cardBg,
                            textColor = textColor,
                            kevinCount = categoryKevinCount,
                            aliCount = categoryAliCount,
                            kevinMasteryCount = categoryKevinMasteryCount,
                            aliMasteryCount = categoryAliMasteryCount,
                            isEditMode = false, // Virtual category cannot be renamed or deleted
                            onClick = {
                                expandedCategories = if (isExpanded) {
                                    expandedCategories - virtualCategoryName
                                } else {
                                    expandedCategories + virtualCategoryName
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        items(uncategorizedIds, key = { "spirit_$it" }) { spiritId ->
                            val index = spiritId.toInt() - 1
                            val hasKevin = kevinList.contains(spiritId)
                            val hasAli = aliList.contains(spiritId)
                            val hasKevinMastery = kevinMastery.contains(spiritId)
                            val hasAliMastery = aliMastery.contains(spiritId)
                            val spiritResId = remember(spiritId, context) {
                                val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                if (id != 0) id else android.R.drawable.ic_menu_gallery
                            }
                            val currentName = customNames[spiritId] ?: spiritNames.getOrElse(index) { "Espíritu #$spiritId" }
                            SpiritRow(
                                spiritId = spiritId,
                                spiritResId = spiritResId,
                                spiritName = currentName,
                                hasKevin = hasKevin,
                                hasAli = hasAli,
                                hasKevinMastery = hasKevinMastery,
                                hasAliMastery = hasAliMastery,
                                isKevin = isKevin,
                                isDark = isDark,
                                borderColor = borderColor,
                                textColor = textColor,
                                cardBg = cardBg,
                                isEditMode = isEditMode,
                                onEditName = {
                                    editingSpiritId = spiritId
                                    editingSpiritInitialName = currentName
                                },
                                onDeleteSpirit = {
                                    deletingSpiritId = spiritId
                                },
                                onToggleCheck = onToggleCheck,
                                onToggleMastery = onToggleMastery
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingSpiritId != null) {
        val spiritId = editingSpiritId!!
        val currentCat = categories.find { it.spiritIds.contains(spiritId) }
        val currentCatName = currentCat?.name ?: ""
        val displayCategoryName = customCategories[currentCatName] ?: currentCatName
        
        var selectedCategoryName by remember(spiritId) { mutableStateOf(currentCatName) }
        var selectedType by remember(spiritId) {
            val name = editingSpiritInitialName
            val suffix = when {
                name == displayCategoryName -> "Normal"
                name.startsWith(displayCategoryName) -> name.substring(displayCategoryName.length).trim()
                else -> "Normal"
            }
            val types = listOf("Dorado", "Gomita", "Galaxia", "Gema", "Holofoil", "Cubo", "Extra", "Especial")
            val t = if (types.contains(suffix)) suffix else "Normal"
            mutableStateOf(t)
        }
        var tempName by remember(spiritId) { mutableStateOf(editingSpiritInitialName) }
        
        val updateGeneratedName = { categoryName: String, typeName: String ->
            val catDisplayName = customCategories[categoryName] ?: categoryName
            tempName = if (typeName == "Normal") {
                catDisplayName
            } else {
                "$catDisplayName $typeName"
            }
        }

        AlertDialog(
            onDismissRequest = { editingSpiritId = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "EDITAR ESPÍRITU",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "ID del Espíritu: $spiritId",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Nombre:",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor
                        ),
                        placeholder = {
                            Text(
                                "Escribe el nombre...",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category selector column
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "Categoría:",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, borderColor)
                                    .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFBEA))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    val isSelected = selectedCategoryName.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedCategoryName = ""
                                                // If category is removed, we don't automatically generate name
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "[Sin Categoría]" + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                                items(categories) { cat ->
                                    val displayCat = customCategories[cat.name] ?: cat.name
                                    val isSelected = selectedCategoryName == cat.name
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedCategoryName = cat.name 
                                                updateGeneratedName(cat.name, selectedType)
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = displayCat + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Type / Variant selector column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tipo:",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, borderColor)
                                    .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFBEA))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val types = listOf("Normal", "Dorado", "Gomita", "Galaxia", "Gema", "Holofoil", "Cubo", "Extra", "Especial")
                                items(types) { t ->
                                    val isSelected = selectedType == t
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedType = t 
                                                if (selectedCategoryName.isNotEmpty()) {
                                                    updateGeneratedName(selectedCategoryName, t)
                                                }
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = t + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.Red)
                            .clickable {
                                deletingSpiritId = spiritId
                                editingSpiritId = null
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "ELIMINAR",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .clickable { editingSpiritId = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "CANCELAR",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .clickable {
                                    onSaveSpiritChanges(spiritId, tempName, selectedCategoryName)
                                    editingSpiritId = null
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "GUARDAR",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        )
    }

    if (editingCategoryOriginalName != null) {
        var tempCategoryName by remember(editingCategoryOriginalName) { mutableStateOf(editingCategoryInitialName) }
        AlertDialog(
            onDismissRequest = { editingCategoryOriginalName = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "EDITAR CATEGORÍA",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Categoría Original: $editingCategoryOriginalName",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempCategoryName,
                        onValueChange = { tempCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor
                        ),
                        placeholder = {
                            Text(
                                "Escribe el nombre de la categoría...",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            editingCategoryOriginalName?.let { orig ->
                                onSaveCategoryName(orig, tempCategoryName)
                            }
                            editingCategoryOriginalName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "GUARDAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { editingCategoryOriginalName = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
    }



    if (deletingSpiritId != null) {
        val spiritId = deletingSpiritId!!
        AlertDialog(
            onDismissRequest = { deletingSpiritId = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "ELIMINAR ESPÍRITU",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar este espíritu de la lista permanente? Esta acción no se puede deshacer.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            onDeleteSpirit(spiritId)
                            deletingSpiritId = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ELIMINAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { deletingSpiritId = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
    }

    if (deletingCategoryName != null) {
        val categoryName = deletingCategoryName!!
        val displayName = customCategories[categoryName] ?: categoryName
        AlertDialog(
            onDismissRequest = { deletingCategoryName = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "ELIMINAR CATEGORÍA",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar la categoría \"$displayName\"? Los espíritus que pertenezcan a ella se mantendrán en la lista general pero quedarán sin categoría.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            onDeleteCategory(categoryName)
                            deletingCategoryName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ELIMINAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { deletingCategoryName = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
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
    displayName: String,
    isExpanded: Boolean,
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    kevinCount: Int,
    aliCount: Int,
    kevinMasteryCount: Int,
    aliMasteryCount: Int,
    isEditMode: Boolean = false,
    onEditCategoryName: () -> Unit = {},
    onDeleteCategory: () -> Unit = {},
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = if (isEditMode) Modifier.clickable { onEditCategoryName() } else Modifier
                )
                if (isEditMode) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onEditCategoryName() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✏️",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onDeleteCategory() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "❌",
                            fontSize = 16.sp
                        )
                    }
                }
            }
            if (!isEditMode) {
                Text(
                    text = "K: $kevinCount ($kevinMasteryCount👑) | A: $aliCount ($aliMasteryCount👑)",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
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
    hasKevinMastery: Boolean,
    hasAliMastery: Boolean,
    isKevin: Boolean,
    isDark: Boolean,
    borderColor: Color,
    textColor: Color,
    cardBg: Color,
    isEditMode: Boolean = false,
    onEditName: () -> Unit = {},
    onDeleteSpirit: () -> Unit = {},
    onToggleCheck: (String, Boolean, String) -> Unit,
    onToggleMastery: (String, Boolean, String) -> Unit
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

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spiritName,
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = if (isEditMode) Modifier.clickable { onEditName() } else Modifier
            )
            if (isEditMode) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clickable { onEditName() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "✏️",
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (!isEditMode) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RetroCheckbox(
                        checked = hasKevin,
                        enabled = isKevin,
                        borderColor = borderColor,
                        onCheckedChange = { onToggleCheck(spiritId, hasKevin, "kevin_list") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val crownAlpha = if (hasKevinMastery) 1f else 0.25f
                    Text(
                        text = "👑",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .alpha(crownAlpha)
                            .clickable(enabled = isKevin && hasKevin) {
                                onToggleMastery(spiritId, hasKevinMastery, "kevin_mastery")
                            }
                    )
                }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RetroCheckbox(
                        checked = hasAli,
                        enabled = !isKevin,
                        borderColor = borderColor,
                        onCheckedChange = { onToggleCheck(spiritId, hasAli, "ali_list") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val crownAlpha = if (hasAliMastery) 1f else 0.25f
                    Text(
                        text = "👑",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .alpha(crownAlpha)
                            .clickable(enabled = !isKevin && hasAli) {
                                onToggleMastery(spiritId, hasAliMastery, "ali_mastery")
                            }
                    )
                }
            }
        } else {
            // Show Delete button (Cross ❌) on the right edge in Edit Mode!
            Box(
                modifier = Modifier
                    .clickable { onDeleteSpirit() }
                    .padding(8.dp)
            ) {
                Text(
                    text = "❌",
                    fontSize = 20.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun AnimeDashboardView(
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
    
    val db = FirebaseFirestore.getInstance()
    var animeList by remember { mutableStateOf(emptyList<AnimeItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    DisposableEffect(coupleId) {
        val listener = db.collection("couple_anime").document(coupleId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val list = snapshot.get("animes") as? List<*>
                    animeList = list?.mapNotNull { itemMap ->
                        val map = itemMap as? Map<*, *> ?: return@mapNotNull null
                        AnimeItem(
                            id = map["id"]?.toString() ?: "",
                            title = map["title"]?.toString() ?: "",
                            imageUrl = map["imageUrl"]?.toString() ?: "",
                            currentEpisode = (map["currentEpisode"] as? Number)?.toInt() ?: 1,
                            totalEpisodes = (map["totalEpisodes"] as? Number)?.toInt(),
                            airingDay = map["airingDay"]?.toString(),
                            nextEpisodeNum = (map["nextEpisodeNum"] as? Number)?.toInt(),
                            airingAt = (map["airingAt"] as? Number)?.toLong()
                        )
                    } ?: emptyList()
                } else {
                    animeList = emptyList()
                }
            }
        onDispose {
            listener.remove()
        }
    }
    
    val updateAnimeEpisode: (String, Int, List<AnimeItem>) -> Unit = { animeId, delta, currentList ->
        val updatedList = currentList.map { anime ->
            if (anime.id == animeId) {
                val newEpisode = (anime.currentEpisode + delta).coerceAtLeast(1)
                anime.copy(currentEpisode = newEpisode)
            } else {
                anime
            }
        }
        val listToSave = updatedList.map { anime ->
            mapOf(
                "id" to anime.id,
                "title" to anime.title,
                "imageUrl" to anime.imageUrl,
                "currentEpisode" to anime.currentEpisode,
                "totalEpisodes" to anime.totalEpisodes,
                "airingDay" to anime.airingDay,
                "nextEpisodeNum" to anime.nextEpisodeNum,
                "airingAt" to anime.airingAt
            )
        }
        db.collection("couple_anime").document(coupleId)
            .set(mapOf("animes" to listToSave), SetOptions.merge())
    }
    
    val deleteAnime: (String, List<AnimeItem>) -> Unit = { animeId, currentList ->
        val updatedList = currentList.filter { it.id != animeId }
        val listToSave = updatedList.map { anime ->
            mapOf(
                "id" to anime.id,
                "title" to anime.title,
                "imageUrl" to anime.imageUrl,
                "currentEpisode" to anime.currentEpisode,
                "totalEpisodes" to anime.totalEpisodes,
                "airingDay" to anime.airingDay,
                "nextEpisodeNum" to anime.nextEpisodeNum,
                "airingAt" to anime.airingAt
            )
        }
        db.collection("couple_anime").document(coupleId)
            .set(mapOf("animes" to listToSave), SetOptions.merge())
    }
    
    val addAnime: (AnimeItem, List<AnimeItem>) -> Unit = { newAnime, currentList ->
        if (!currentList.any { it.id == newAnime.id }) {
            val updatedList = currentList + newAnime
            val listToSave = updatedList.map { anime ->
                mapOf(
                    "id" to anime.id,
                    "title" to anime.title,
                    "imageUrl" to anime.imageUrl,
                    "currentEpisode" to anime.currentEpisode,
                    "totalEpisodes" to anime.totalEpisodes,
                    "airingDay" to anime.airingDay,
                    "nextEpisodeNum" to anime.nextEpisodeNum,
                    "airingAt" to anime.airingAt
                )
            }
            db.collection("couple_anime").document(coupleId)
                .set(mapOf("animes" to listToSave), SetOptions.merge())
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                text = "ANIME COMPARTIDO",
                fontFamily = Vt323,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            
            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .background(if (isDark) Color(0xFF91465F) else Color(0xFF4A2511))
                        .clickable { showSearchDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ AÑADIR",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectedTabBg = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
            val selectedTabTextColor = Color.White
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, borderColor)
                    .background(if (selectedTab == 0) selectedTabBg else cardBg)
                    .clickable { selectedTab = 0 }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIENDO",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 0) selectedTabTextColor else textColor
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, borderColor)
                    .background(if (selectedTab == 1) selectedTabBg else cardBg)
                    .clickable { selectedTab = 1 }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CALENDARIO",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 1) selectedTabTextColor else textColor
                )
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cargando animes...",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = textColor
                )
            }
        } else if (selectedTab == 0) {
            if (animeList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no han añadido animes.\n¡Pulsa + AÑADIR para buscar uno!",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(animeList, key = { it.id }) { anime ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, borderColor)
                                .background(cardBg)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = anime.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(130.dp)
                                    .border(1.dp, borderColor.copy(alpha = 0.5f)),
                                contentScale = ContentScale.Crop
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.title,
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Capítulo: ",
                                        fontFamily = Vt323,
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, borderColor)
                                            .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDF5))
                                            .clickable { updateAnimeEpisode(anime.id, -1, animeList) }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "-",
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    
                                    Text(
                                        text = " ${anime.currentEpisode} ",
                                        fontFamily = Vt323,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, borderColor)
                                            .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDF5))
                                            .clickable { updateAnimeEpisode(anime.id, 1, animeList) }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+",
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    
                                    if (anime.totalEpisodes != null) {
                                        Text(
                                            text = " / ${anime.totalEpisodes}",
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                if (anime.airingDay != null) {
                                    val isAiringNext = anime.nextEpisodeNum != null
                                    val airingText = if (isAiringNext) {
                                        "Próximo: Cap ${anime.nextEpisodeNum} (${anime.airingDay})"
                                    } else {
                                        "Emisión: ${anime.airingDay}"
                                    }
                                    Text(
                                        text = airingText,
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Finalizado",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        color = textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.dp, Color.Red.copy(alpha = 0.7f))
                                    .background(cardBg)
                                    .clickable { deleteAnime(anime.id, animeList) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "X",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(daysOfWeek) { day ->
                    val animesForDay = animeList.filter { it.airingDay == day }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor)
                            .background(cardBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "📅 $day",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFBD93F9) else Color(0xFF4A2511)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        if (animesForDay.isEmpty()) {
                            Text(
                                text = "Ninguno para este día.",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        } else {
                            animesForDay.forEach { anime ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = anime.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(85.dp)
                                            .border(1.dp, borderColor),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = anime.title,
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val nextText = if (anime.nextEpisodeNum != null) {
                                            "Capítulo ${anime.nextEpisodeNum}"
                                        } else {
                                            "Nuevo capítulo"
                                        }
                                        Text(
                                            text = "$nextText (Vamos en el cap ${anime.currentEpisode})",
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                val finishedAnimes = animeList.filter { it.airingDay == null }
                if (finishedAnimes.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, borderColor)
                                .background(cardBg)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🎬 Finalizados / Sin emisión activa",
                                fontFamily = Vt323,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            finishedAnimes.forEach { anime ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = anime.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(85.dp)
                                            .border(1.dp, borderColor),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = anime.title,
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Finalizado (Vamos en el cap ${anime.currentEpisode})",
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.7f)
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
    
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Text(
                    text = "BUSCAR EN ANILIST",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Column {
                    var searchQuery by remember { mutableStateOf("") }
                    var searchResults by remember { mutableStateOf(emptyList<AnimeItem>()) }
                    var isSearching by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            ),
                            placeholder = {
                                Text(
                                    "Ej. One Piece...",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    color = textColor.copy(alpha = 0.5f)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF91465F) else Color(0xFF4A2511))
                                .clickable {
                                    if (searchQuery.isNotBlank() && !isSearching) {
                                        isSearching = true
                                        coroutineScope.launch {
                                            searchResults = searchAnimeOnAniList(searchQuery)
                                            isSearching = false
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "BUSCAR",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isSearching) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Buscando...",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin resultados.",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { anime ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, borderColor.copy(alpha = 0.3f))
                                        .background(cardBg)
                                        .clickable {
                                            addAnime(anime, animeList)
                                            showSearchDialog = false
                                        }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = anime.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(75.dp)
                                            .border(1.dp, borderColor),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = anime.title,
                                            fontFamily = Vt323,
                                            fontSize = 18.sp,
                                            color = textColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val statusText = if (anime.airingDay != null) {
                                            "Emisión: ${anime.airingDay}"
                                        } else {
                                            "Finalizado"
                                        }
                                        Text(
                                            text = statusText,
                                            fontFamily = Vt323,
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { showSearchDialog = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CERRAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            containerColor = cardBg,
            textContentColor = textColor
        )
    }
}

suspend fun searchAnimeOnAniList(queryText: String): List<AnimeItem> = withContext(Dispatchers.IO) {
    if (queryText.isBlank()) return@withContext emptyList()
    
    val client = DiarioApp.getOkHttpClient()
    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    
    val graphqlQuery = """
        query (${'$'}search: String) {
          Page(perPage: 10) {
            media(search: ${'$'}search, type: ANIME) {
              id
              title {
                romaji
                english
              }
              coverImage {
                large
              }
              nextAiringEpisode {
                airingAt
                episode
              }
              episodes
            }
          }
        }
    """.trimIndent()
    
    val jsonObject = JSONObject().apply {
        put("query", graphqlQuery)
        put("variables", JSONObject().apply { put("search", queryText) })
    }
    
    val body = jsonObject.toString().toRequestBody(mediaType)
    val request = Request.Builder()
        .url("https://graphql.anilist.co")
        .post(body)
        .build()
        
    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) return@withContext emptyList()
            
            val responseString = response.body!!.string()
            val responseJson = JSONObject(responseString)
            val dataJson = responseJson.optJSONObject("data") ?: return@withContext emptyList()
            val pageJson = dataJson.optJSONObject("Page") ?: return@withContext emptyList()
            val mediaArray = pageJson.optJSONArray("media") ?: return@withContext emptyList()
            
            val results = mutableListOf<AnimeItem>()
            for (i in 0 until mediaArray.length()) {
                val media = mediaArray.optJSONObject(i) ?: continue
                val id = media.optInt("id").toString()
                
                val titleObj = media.optJSONObject("title")
                val romajiTitle = titleObj?.optString("romaji")
                val englishTitle = titleObj?.optString("english")
                val title = if (!englishTitle.isNullOrBlank() && englishTitle != "null") englishTitle else romajiTitle ?: "Anime #${'$'}id"
                
                val coverImageObj = media.optJSONObject("coverImage")
                val imageUrl = coverImageObj?.optString("large") ?: ""
                
                val episodesVal = media.optInt("episodes", -1)
                val totalEpisodes = if (episodesVal > 0) episodesVal else null
                
                val nextAiring = media.optJSONObject("nextAiringEpisode")
                val airingAtVal = nextAiring?.optLong("airingAt", -1L)
                val airingAt = if (airingAtVal != null && airingAtVal > 0) airingAtVal else null
                val nextEpisodeNumVal = nextAiring?.optInt("episode", -1)
                val nextEpisodeNum = if (nextEpisodeNumVal != null && nextEpisodeNumVal > 0) nextEpisodeNumVal else null
                
                val airingDay = getAiringDayInSpanish(airingAt)
                
                results.add(
                    AnimeItem(
                        id = id,
                        title = title,
                        imageUrl = imageUrl,
                        currentEpisode = 1,
                        totalEpisodes = totalEpisodes,
                        airingDay = airingDay,
                        nextEpisodeNum = nextEpisodeNum,
                        airingAt = airingAt
                    )
                )
            }
            results
        }
    } catch (e: Exception) {
        Log.e("AnimeSearch", "Error searching anime", e)
        emptyList()
    }
}

fun getAiringDayInSpanish(airingAt: Long?): String? {
    if (airingAt == null) return null
    val date = java.util.Date(airingAt * 1000)
    val calendar = java.util.Calendar.getInstance()
    calendar.time = date
    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    return when (dayOfWeek) {
        java.util.Calendar.SUNDAY -> "Domingo"
        java.util.Calendar.MONDAY -> "Lunes"
        java.util.Calendar.TUESDAY -> "Martes"
        java.util.Calendar.WEDNESDAY -> "Miércoles"
        java.util.Calendar.THURSDAY -> "Jueves"
        java.util.Calendar.FRIDAY -> "Viernes"
        java.util.Calendar.SATURDAY -> "Sábado"
        else -> null
    }
}

data class AnimeItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val currentEpisode: Int = 1,
    val totalEpisodes: Int? = null,
    val airingDay: String? = null,
    val nextEpisodeNum: Int? = null,
    val airingAt: Long? = null
)
