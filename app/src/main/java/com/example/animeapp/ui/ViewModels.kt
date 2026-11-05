package com.example.animeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.data.Anime
import com.example.animeapp.data.AnimeRepository
import com.example.animeapp.data.Genre
import com.example.animeapp.data.HomeData
import com.example.animeapp.data.FuzzySearch
import com.example.animeapp.data.Ranking
import com.example.animeapp.data.Translations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Los ViewModel guardan el estado de cada pantalla.
// Sobreviven a la rotación del celular y al cambio de pestaña,
// así los datos no se vuelven a pedir a la API cada vez.

// ---------- Inicio ----------

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state

    private var loadedFavoritesVersion = -1

    init {
        load()
    }

    fun load() {
        loadedFavoritesVersion = AnimeRepository.favoritesVersion
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(AnimeRepository.loadHome())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(AnimeRepository.describeError(e))
            }
        }
    }

    // Si se agregaron o quitaron favoritos, se recalculan los carruseles por género
    fun refreshIfFavoritesChanged() {
        if (AnimeRepository.favoritesVersion != loadedFavoritesVersion) load()
    }
}

// ---------- Top ratings (scroll infinito) ----------

data class TopData(val ranking: List<Anime>, val hasMore: Boolean, val loadingMore: Boolean)

class TopViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<TopData>>(UiState.Loading)
    val state: StateFlow<UiState<TopData>> = _state

    // Mensaje de un solo uso (por ejemplo, si falla cargar la página siguiente)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val loaded = mutableListOf<Anime>()
    private var lastPage = 0
    private var hasMore = true
    private var loading = false

    init {
        loadNextPage()
    }

    fun retry() {
        if (loaded.isEmpty()) loadNextPage()
    }

    fun loadNextPage() {
        if (loading || !hasMore) return
        loading = true
        if (loaded.isEmpty()) {
            _state.value = UiState.Loading
        } else {
            _state.value = UiState.Success(TopData(Ranking.sort(loaded), hasMore, loadingMore = true))
        }

        viewModelScope.launch {
            try {
                val response = AnimeRepository.getTopPage(lastPage + 1)
                loaded += response.data.orEmpty()
                lastPage++
                hasMore = response.pagination?.hasNextPage == true && lastPage < MAX_PAGES
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (loaded.isEmpty()) {
                    _state.value = UiState.Error(AnimeRepository.describeError(e))
                    return@launch
                }
                _message.value = AnimeRepository.describeError(e)
            } finally {
                loading = false
            }
            _state.value = UiState.Success(TopData(Ranking.sort(loaded), hasMore, loadingMore = false))
        }
    }

    fun messageShown() {
        _message.value = null
    }

    companion object {
        private const val MAX_PAGES = 8 // 8 páginas x 25 = top 200
    }
}

// ---------- Buscar ----------

data class SearchData(val summary: String, val results: List<Anime>, val emptyMessage: String)

class SearchViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<SearchData>>(UiState.Loading)
    val state: StateFlow<UiState<SearchData>> = _state

    var query = ""
        private set
    var genreId: Int? = null
        private set

    private var job: Job? = null

    init {
        run(debounce = false)
    }

    fun setQuery(text: String) {
        val clean = text.trim()
        if (clean == query) return
        query = clean
        run(debounce = true)
    }

    fun setGenre(id: Int?) {
        if (id == genreId) return
        genreId = id
        run(debounce = false)
    }

    fun retry() = run(debounce = false)

    // Debounce: se espera a que el usuario deje de escribir antes de llamar a la API.
    // Cancelar el Job también cancela la petición anterior.
    private fun run(debounce: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(450)
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(search())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(AnimeRepository.describeError(e))
            }
        }
    }

    private suspend fun search(): SearchData {
        val genre = Translations.browseGenres.firstOrNull { it.id == genreId }
        val genreName = genre?.let { Translations.genre(it.name) }

        return when {
            // Sin texto ni género: los más populares
            query.isEmpty() && genre == null -> SearchData(
                "Populares en MyAnimeList",
                Ranking.sort(AnimeRepository.getTopPage(1).data.orEmpty()),
                "No hay animes para mostrar."
            )
            // Solo género: los mejores de ese género
            query.isEmpty() -> SearchData(
                "Mejores de $genreName",
                AnimeRepository.getBestByGenre(genre!!.id, 25),
                "No hay animes de $genreName."
            )
            // Texto (y opcionalmente género)
            else -> {
                val results = AnimeRepository.search(query)
                    .filter { genre == null || it.hasGenre(genre.id) }
                val filterText = if (genreName != null) " en $genreName" else ""
                SearchData(
                    "${results.size} resultados para \"$query\"$filterText",
                    results,
                    "Sin resultados para \"$query\"$filterText."
                )
            }
        }
    }
}

// ---------- Favoritos ----------

data class FavoritesData(
    val items: List<Anime>,
    val genres: List<Genre>,   // géneros presentes en los favoritos (para los chips)
    val total: Int,
    val summary: String
)

class FavoritesViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<FavoritesData>>(UiState.Loading)
    val state: StateFlow<UiState<FavoritesData>> = _state

    var query = ""
        private set
    var genreId: Int? = null
        private set

    fun setQuery(text: String) {
        val clean = text.trim()
        if (clean == query) return
        query = clean
        refresh()
    }

    fun setGenre(id: Int?) {
        if (id == genreId) return
        genreId = id
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val all = AnimeRepository.getFavorites()
            // Si el género elegido ya no está en ningún favorito, se quita el filtro
            if (genreId != null && all.none { it.hasGenre(genreId!!) }) genreId = null

            // Filtro por género con SQL (LIKE) y búsqueda difusa en Kotlin
            val byGenre = if (genreId != null) AnimeRepository.getFavorites(genreId) else all
            val items = if (query.isEmpty()) byGenre else FuzzySearch.filter(query, byGenre) { it.allTitles() }

            val genres = AnimeRepository.topGenres(all, max = Int.MAX_VALUE)
            val summary = when {
                all.isEmpty() -> ""
                items.size == all.size -> "${all.size} guardados"
                else -> "${items.size} de ${all.size} guardados"
            }
            _state.value = UiState.Success(FavoritesData(items, genres, all.size, summary))
        }
    }
}
