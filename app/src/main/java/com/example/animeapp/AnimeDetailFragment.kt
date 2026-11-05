package com.example.animeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.example.animeapp.data.Anime
import com.example.animeapp.data.AnimeRepository
import com.example.animeapp.data.Episode
import com.example.animeapp.data.Translations
import com.example.animeapp.ui.CoverLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat

// Fragment "puente": Navigation sigue funcionando con Fragments,
// pero toda esta pantalla está hecha en Jetpack Compose.
class AnimeDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val anime = BundleCompat.getSerializable(requireArguments(), ARG_ANIME, Anime::class.java)
        (activity as? AppCompatActivity)?.supportActionBar?.title = anime?.title ?: "Detalle"

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = AppColors.scheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
                        if (anime != null) {
                            AnimeDetailScreen(anime)
                        } else {
                            Text("No se pudo cargar el anime", modifier = Modifier.padding(24.dp))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_ANIME = "anime"
    }
}

// Mismos colores que el tema XML de la app
private object AppColors {
    val Background = Color(0xFF0E0E13)
    val Surface = Color(0xFF1A1A23)
    val SurfaceHigh = Color(0xFF252532)
    val Outline = Color(0xFF33333F)
    val Accent = Color(0xFFFF9F43)
    val Heart = Color(0xFFFF5A6E)
    val Gold = Color(0xFFFFD166)
    val TextPrimary = Color(0xFFF2F2F7)
    val TextSecondary = Color(0xFFA0A0B2)

    val scheme = darkColorScheme(
        primary = Accent,
        onPrimary = Color.White,
        secondary = Gold,
        background = Background,
        surface = Background,
        surfaceVariant = Surface,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        outline = Outline
    )
}

// ---------- Pantalla ----------

@Composable
fun AnimeDetailScreen(anime: Anime) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeroHeader(anime)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            StatsRow(anime)
            Spacer(Modifier.height(16.dp))
            FavoriteButton(anime)
            Spacer(Modifier.height(24.dp))

            SectionTitle("Géneros")
            GenresRow(anime)
            Spacer(Modifier.height(24.dp))

            SectionTitle("Información")
            InfoGrid(anime)
            Spacer(Modifier.height(24.dp))

            SectionTitle("Sinopsis")
            Synopsis(anime.synopsis)
            Spacer(Modifier.height(24.dp))

            EpisodesSection(anime)
            Spacer(Modifier.height(32.dp))
        }
    }
}

// Portada grande con degradé y títulos encima
@Composable
private fun HeroHeader(anime: Anime) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
    ) {
        // Picasso no tiene versión para Compose: se usa un ImageView clásico dentro de AndroidView
        AndroidView(
            factory = { context ->
                ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            },
            update = { imageView -> CoverLoader.load(imageView, anime.hdImageUrl(), anime.title) },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to AppColors.Background.copy(alpha = 0.25f),
                        1f to AppColors.Background
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = anime.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                color = Color.White
            )
            val otherTitles = listOfNotNull(
                anime.titleEnglish?.takeIf { it != anime.title },
                anime.titleJapanese
            )
            if (otherTitles.isNotEmpty()) {
                Text(
                    text = otherTitles.joinToString("  ·  "),
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = Translations.metaLine(anime),
                color = AppColors.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// Puntuación, ranking, popularidad y miembros
@Composable
private fun StatsRow(anime: Anime) {
    val numbers = NumberFormat.getIntegerInstance()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(
            label = anime.scoredBy?.let { "${numbers.format(it)} votos" } ?: "Puntuación",
            value = anime.score?.let { "★ %.2f".format(it) } ?: "★ —",
            valueColor = AppColors.Gold,
            modifier = Modifier.weight(1f)
        )
        StatBox("Ranking", anime.rank?.let { "#$it" } ?: "—", AppColors.TextPrimary, Modifier.weight(1f))
        StatBox("Popularidad", anime.popularity?.let { "#$it" } ?: "—", AppColors.TextPrimary, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Botón de favoritos (SQLite). El estado de Compose redibuja el botón al cambiar.
@Composable
private fun FavoriteButton(anime: Anime) {
    val scope = rememberCoroutineScope()
    var isFavorite by remember { mutableStateOf<Boolean?>(null) } // null = todavía no se sabe

    LaunchedEffect(anime.id) {
        isFavorite = AnimeRepository.isFavorite(anime.id)
    }

    val onClick: () -> Unit = {
        scope.launch {
            if (isFavorite == true) AnimeRepository.removeFavorite(anime.id)
            else AnimeRepository.addFavorite(anime)
            isFavorite = isFavorite != true
        }
    }

    if (isFavorite == true) {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("♥ En favoritos · tocar para quitar", color = AppColors.Heart)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = isFavorite != null,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("♥  Guardar en favoritos", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresRow(anime: Anime) {
    val genres = anime.genres.orEmpty()
    if (genres.isEmpty()) {
        Text("Sin géneros registrados", color = AppColors.TextSecondary)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            Text(
                text = Translations.genre(genre.name),
                color = AppColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(AppColors.Accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .border(1.dp, AppColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

// Datos técnicos en dos columnas
@Composable
private fun InfoGrid(anime: Anime) {
    val season = listOfNotNull(Translations.season(anime.season), anime.year?.toString())
        .joinToString(" ")
        .ifEmpty { null }
    val rows = listOf(
        "Tipo" to Translations.type(anime.type),
        "Episodios" to anime.episodes?.toString(),
        "Estado" to Translations.status(anime.status),
        "Temporada" to season,
        "Duración" to anime.duration,
        "Estudio" to anime.studios.orEmpty().joinToString(", ") { it.name }.ifEmpty { null },
        "Clasificación" to anime.rating
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = AppColors.TextSecondary, fontSize = 12.sp)
                        Text(value ?: "—", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// Sinopsis con "Ver más / Ver menos"
@Composable
private fun Synopsis(text: String?) {
    var expanded by remember { mutableStateOf(false) }
    val synopsis = text?.takeIf { it.isNotBlank() }
    if (synopsis == null) {
        Text("Sin sinopsis disponible", color = AppColors.TextSecondary)
        return
    }
    Text(
        text = synopsis,
        color = AppColors.TextPrimary,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 5,
        overflow = TextOverflow.Ellipsis
    )
    if (synopsis.length > 250) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Ver menos" else "Ver más", color = AppColors.Accent)
        }
    }
}

// ---------- Episodios (/anime/{id}/episodes) ----------

@Composable
private fun EpisodesSection(anime: Anime) {
    val scope = rememberCoroutineScope()
    val episodes = remember { mutableStateListOf<Episode>() }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadNext() {
        if (loading || !hasMore) return
        loading = true
        error = null
        scope.launch {
            try {
                val response = AnimeRepository.getEpisodes(anime.id, page + 1)
                episodes.addAll(response.data.orEmpty())
                page++
                hasMore = response.pagination?.hasNextPage == true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = AnimeRepository.describeError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(anime.id) { loadNext() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Episodios")
        if (episodes.isNotEmpty()) {
            val average = episodes.mapNotNull { it.score }.average()
            if (!average.isNaN()) {
                Text(
                    text = "  promedio ★ %.2f / 5".format(average),
                    color = AppColors.Gold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }

    episodes.forEach { episode -> EpisodeRow(episode) }

    when {
        loading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.Accent, modifier = Modifier.size(28.dp))
        }
        error != null -> Column {
            Text(error ?: "", color = AppColors.TextSecondary, fontSize = 14.sp)
            TextButton(onClick = { loadNext() }) { Text("Reintentar", color = AppColors.Accent) }
        }
        episodes.isEmpty() -> Text(
            text = if (anime.type == "Movie") "Es una película: no tiene lista de episodios."
            else "MyAnimeList no tiene episodios cargados para este anime.",
            color = AppColors.TextSecondary,
            fontSize = 14.sp
        )
        hasMore -> OutlinedButton(onClick = { loadNext() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cargar más episodios", color = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(AppColors.Surface, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("${episode.number}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = episode.title ?: "Episodio ${episode.number}",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val tags = listOfNotNull(
                formatDate(episode.aired),
                if (episode.filler) "Relleno" else null,
                if (episode.recap) "Resumen" else null
            )
            if (tags.isNotEmpty()) {
                Text(tags.joinToString(" · "), color = AppColors.TextSecondary, fontSize = 12.sp)
            }
        }
        Text(
            text = episode.score?.let { "★ %.2f".format(it) } ?: "★ —",
            color = AppColors.Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(64.dp)
        )
    }
}

// "2009-04-05T00:00:00+00:00" -> "05/04/2009"
private fun formatDate(iso: String?): String? {
    val parts = iso?.take(10)?.split("-") ?: return null
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else null
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
