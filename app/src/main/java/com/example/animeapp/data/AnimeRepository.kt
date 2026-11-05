package com.example.animeapp.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

// Repositorio: API, caché y favoritos en un solo lugar
object AnimeRepository {
    private lateinit var db: FavoritesDbHelper

    // Abre la base y carga los ids de favoritos
    fun init(context: Context) {
        if (::db.isInitialized) return
        db = FavoritesDbHelper(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            _favoriteIds.value = db.getFavoriteIds()
        }
    }

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds

    private val api: JikanApi get() = RetrofitClient.api

    // Límite de Jikan: 400 ms entre llamadas
    private val throttleMutex = Mutex()
    private var lastCallTime = 0L
    private const val MIN_INTERVAL_MS = 400L

    private suspend fun waitTurn() {
        throttleMutex.withLock {
            val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastCallTime)
            if (wait > 0) delay(wait)
            lastCallTime = System.currentTimeMillis()
        }
    }

    // Si responde 429 espera y reintenta una vez
    private suspend fun <T> callApi(block: suspend () -> T): T {
        waitTurn()
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() != 429) throw e
            delay(1500)
            waitTurn()
            block()
        }
    }

    // Caché en memoria
    private val topPages = mutableMapOf<Int, AnimeResponse>()
    private val genreCache = mutableMapOf<Int, List<Anime>>()
    private val episodesCache = mutableMapOf<Pair<Int, Int>, EpisodesResponse>()

    private val knownAnimes = linkedMapOf<Int, Anime>()

    private fun remember(animes: List<Anime>?) {
        animes.orEmpty().forEach { knownAnimes[it.id] = it }
    }

    private var todayPick: Anime? = null
    private var todayPickReason = ""

    var favoritesVersion = 0
        private set

    // Top por página
    suspend fun getTopPage(page: Int): AnimeResponse {
        topPages[page]?.let { return it }
        val response = callApi { api.getTopAnime(page) }
        topPages[page] = response
        remember(response.data)
        return response
    }

    suspend fun getTop10(): List<Anime> = Ranking.sort(getTopPage(1).data.orEmpty()).take(10)

    // Mejores de un género
    suspend fun getBestByGenre(genreId: Int, limit: Int = 12): List<Anime> {
        genreCache[genreId]?.takeIf { it.size >= limit }?.let { return it.take(limit) }
        val response = callApi { api.getAnimeByGenre(genreId, limit) }
        val list = Ranking.sort(response.data.orEmpty()).take(limit)
        genreCache[genreId] = list
        remember(list)
        return list
    }

    // Buscador: Jikan + búsqueda difusa local
    suspend fun search(query: String): List<Anime> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val remote = try {
            callApi { api.searchAnime(q, 25) }.data.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (knownAnimes.isEmpty() && getFavorites().isEmpty()) throw e
            emptyList()
        }
        remember(remote)

        val candidates = (remote + knownAnimes.values + getFavorites()).distinctBy { it.id }
        val fuzzyMatches = FuzzySearch.filter(q, candidates) { it.allTitles() }
        val matchedIds = fuzzyMatches.map { it.id }.toSet()
        return fuzzyMatches + remote.filter { it.id !in matchedIds }
    }

    // Episodios con puntaje
    suspend fun getEpisodes(animeId: Int, page: Int): EpisodesResponse {
        episodesCache[animeId to page]?.let { return it }
        val response = callApi { api.getEpisodes(animeId, page) }
        episodesCache[animeId to page] = response
        return response
    }

    // Favoritos (SQLite en hilo IO)
    suspend fun getFavorites(genreId: Int? = null): List<Anime> =
        withContext(Dispatchers.IO) { db.getFavorites(genreId) }

    suspend fun isFavorite(animeId: Int): Boolean =
        withContext(Dispatchers.IO) { db.isFavorite(animeId) }

    suspend fun addFavorite(anime: Anime) {
        withContext(Dispatchers.IO) { db.addFavorite(anime) }
        _favoriteIds.value = _favoriteIds.value + anime.id
        favoritesVersion++
    }

    suspend fun removeFavorite(animeId: Int) {
        withContext(Dispatchers.IO) { db.removeFavorite(animeId) }
        _favoriteIds.value = _favoriteIds.value - animeId
        favoritesVersion++
    }

    // Corazón: agrega o quita
    suspend fun toggleFavorite(anime: Anime) {
        if (anime.id in _favoriteIds.value) removeFavorite(anime.id) else addFavorite(anime)
    }

    // Géneros más repetidos en favoritos
    fun topGenres(favorites: List<Anime>, max: Int = 3): List<Genre> =
        favorites.flatMap { it.genres.orEmpty() }
            .groupBy { it.id }
            .map { (_, list) -> list.first() to list.size }
            .sortedWith(compareByDescending<Pair<Genre, Int>> { it.second }.thenBy { it.first.name })
            .take(max)
            .map { it.first }

    // Controlador de carruseles
    suspend fun loadHome(): HomeData {
        val top10 = getTop10()
        val favorites = getFavorites()
        val favoriteIds = favorites.map { it.id }.toSet()

        // Llama a favoritos para obtener géneros, y ordena según ranking
        val sections = topGenres(favorites).mapNotNull { genre ->
            try {
                GenreSection(genre, getBestByGenre(genre.id, 12))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }.filter { it.animes.isNotEmpty() }

        if (todayPick == null) {
            chooseTodayPick(sections, favoriteIds)
        }
        return HomeData(todayPick, todayPickReason, top10, sections)
    }

    // Recomendación del día: de tus géneros o del top
    private fun chooseTodayPick(sections: List<GenreSection>, favoriteIds: Set<Int>) {
        val fromGenres = sections.flatMap { section ->
            section.animes
                .filter { it.id !in favoriteIds && (it.score ?: 0.0) >= 7.5 }
                .map { it to section.genre }
        }
        if (fromGenres.isNotEmpty()) {
            val (anime, genre) = fromGenres.random()
            todayPick = anime
            todayPickReason = "Porque te gusta ${Translations.genre(genre.name)}"
            return
        }

        val topCandidates = topPages[1]?.data.orEmpty()
            .filter { it.id !in favoriteIds && (it.score ?: 0.0) >= 8.0 }
        val any = topCandidates.ifEmpty { topPages[1]?.data.orEmpty() }
        if (any.isNotEmpty()) {
            todayPick = any.random()
            todayPickReason = "Entre los mejor puntuados de MyAnimeList"
        }
    }

    // Mensajes de error para el usuario
    fun describeError(e: Throwable): String = when (e) {
        is HttpException -> if (e.code() == 429) {
            "Jikan recibió demasiadas peticiones. Esperá unos segundos y reintentá."
        } else {
            "El servidor de Jikan respondió con un error (${e.code()})."
        }
        is IOException -> "No se pudo conectar con Jikan. Revisá tu conexión o probá más tarde."
        else -> "Ocurrió un error inesperado."
    }
}
