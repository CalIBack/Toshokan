package com.example.animeapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.data.Genre
import com.example.animeapp.data.Translations
import com.example.animeapp.ui.AnimeRowAdapter
import com.example.animeapp.ui.FavoritesViewModel
import com.example.animeapp.ui.UiState
import com.example.animeapp.ui.openAnimeDetail
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.animeapp.data.Anime
import com.example.animeapp.data.AnimeRepository
import kotlinx.coroutines.launch

// Pantalla Favoritos (SQLite, funciona offline)
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {
    private val viewModel: FavoritesViewModel by viewModels()

    private var shownGenres: List<Genre> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shownGenres = emptyList()

        val input: EditText = view.findViewById(R.id.editFavorites)
        val chips: ChipGroup = view.findViewById(R.id.chipGroupFavorites)
        val summary: TextView = view.findViewById(R.id.textFavoritesSummary)
        val recycler: RecyclerView = view.findViewById(R.id.recyclerFavorites)
        val empty: TextView = view.findViewById(R.id.textEmptyFavorites)

        val adapter = AnimeRowAdapter({ openAnimeDetail(it) }, { toggleFavorite(it) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        chips.setOnCheckedStateChangeListener { group, checkedIds ->
            val genreId = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it).tag as Int }
            viewModel.setGenre(genreId)
        }
        input.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    AnimeRepository.favoriteIds.collect { ids ->
                        adapter.favoriteIds = ids
                        viewModel.refresh()
                    }
                }
                viewModel.state.collect { state ->
                    if (state !is UiState.Success) return@collect
                    val data = state.data

                    if (data.genres != shownGenres) {
                        shownGenres = data.genres
                        buildChips(chips, data.genres)
                    }

                    adapter.submitList(data.items)
                    summary.text = data.summary
                    empty.isVisible = data.items.isEmpty()
                    empty.text = if (data.total == 0) {
                        "Todavía no guardaste favoritos.\nAbrí un anime y tocá ♥ Guardar en favoritos."
                    } else {
                        "Ningún favorito coincide con la búsqueda."
                    }
                }
            }
        }
    }

    // Recarga al volver del detalle
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun buildChips(group: ChipGroup, genres: List<Genre>) {
        group.removeAllViews()
        genres.forEach { genre ->
            val chip = layoutInflater.inflate(R.layout.item_chip_filter, group, false) as Chip
            chip.id = View.generateViewId()
            chip.text = Translations.genre(genre.name)
            chip.tag = genre.id
            chip.isChecked = genre.id == viewModel.genreId
            group.addView(chip)
        }
        group.isVisible = genres.isNotEmpty()
    }

    // Corazón
    private fun toggleFavorite(anime: Anime) {
        viewLifecycleOwner.lifecycleScope.launch { AnimeRepository.toggleFavorite(anime) }
    }
}
