package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.DiarioApp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
                            airingAt = (map["airingAt"] as? Number)?.toLong(),
                            isWatched = map["isWatched"] as? Boolean ?: false
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
                "airingAt" to anime.airingAt,
                "isWatched" to anime.isWatched
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
                "airingAt" to anime.airingAt,
                "isWatched" to anime.isWatched
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
                    "airingAt" to anime.airingAt,
                    "isWatched" to anime.isWatched
                )
            }
            db.collection("couple_anime").document(coupleId)
                .set(mapOf("animes" to listToSave), SetOptions.merge())
        }
    }

    val toggleAnimeWatched: (String, List<AnimeItem>) -> Unit = { animeId, currentList ->
        val updatedList = currentList.map { anime ->
            if (anime.id == animeId) {
                anime.copy(isWatched = !anime.isWatched)
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
                "airingAt" to anime.airingAt,
                "isWatched" to anime.isWatched
            )
        }
        db.collection("couple_anime").document(coupleId)
            .set(mapOf("animes" to listToSave), SetOptions.merge())
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val selectedTabBg = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
            val selectedTabTextColor = Color.White
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, borderColor)
                    .background(if (selectedTab == 0) selectedTabBg else cardBg)
                    .clickable { selectedTab = 0 }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIENDO",
                    fontFamily = Vt323,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 0) selectedTabTextColor else textColor
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .border(2.dp, borderColor)
                    .background(if (selectedTab == 1) selectedTabBg else cardBg)
                    .clickable { selectedTab = 1 }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CALENDARIO",
                    fontFamily = Vt323,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 1) selectedTabTextColor else textColor
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .border(2.dp, borderColor)
                    .background(if (selectedTab == 2) selectedTabBg else cardBg)
                    .clickable { selectedTab = 2 }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VISTOS",
                    fontFamily = Vt323,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 2) selectedTabTextColor else textColor
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
            val activeAnimeList = animeList.filter { !it.isWatched }
            if (activeAnimeList.isEmpty()) {
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
                    items(activeAnimeList, key = { it.id }) { anime ->
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
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(2.dp, Color(0xFF10B981))
                                        .background(cardBg)
                                        .clickable { toggleAnimeWatched(anime.id, animeList) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
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
            }
        } else if (selectedTab == 1) {
            val activeAnimeList = animeList.filter { !it.isWatched }
            val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(daysOfWeek) { day ->
                    val animesForDay = activeAnimeList.filter { it.airingDay == day }
                    
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
                
                val finishedAnimes = activeAnimeList.filter { it.airingDay == null }
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
        } else {
            val watchedAnimeList = animeList.filter { it.isWatched }
            if (watchedAnimeList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay animes marcados como vistos.",
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
                    items(watchedAnimeList, key = { it.id }) { anime ->
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
                                
                                Text(
                                    text = "Completado en Cap: ${anime.currentEpisode}" + (if (anime.totalEpisodes != null) " / ${anime.totalEpisodes}" else ""),
                                    fontFamily = Vt323,
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(2.dp, Color(0xFF3B82F6))
                                        .background(cardBg)
                                        .clickable { toggleAnimeWatched(anime.id, animeList) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "↩",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
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
    val airingAt: Long? = null,
    val isWatched: Boolean = false
)
