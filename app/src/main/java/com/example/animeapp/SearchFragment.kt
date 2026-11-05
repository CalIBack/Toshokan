package com.example.animeapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
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
import com.example.animeapp.data.Translations
import com.example.animeapp.ui.AnimeRowAdapter
import com.example.animeapp.ui.SearchViewModel
import com.example.animeapp.ui.UiState
import com.example.animeapp.ui.openAnimeDetail
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.animeapp.data.Anime
import com.example.animeapp.data.AnimeRepository
import kotlinx.coroutines.launch

// Buscar: texto (búsqueda difusa) + filtro por género
class SearchFragment : Fragment(R.layout.fragment_search) {

    private val viewModel: SearchViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input: EditText = view.findViewById(R.id.editSearch)
        val chips: ChipGroup = view.findViewById(R.id.chipGroupSearch)
        val summary: TextView = view.findViewById(R.id.textSearchSummary)
        val recycler: RecyclerView = view.findViewById(R.id.recyclerSearch)
        val progress: ProgressBar = view.findViewById(R.id.progressSearch)
        val empty: TextView = view.findViewById(R.id.textEmptySearch)
        val error: View = view.findViewById(R.id.errorSearch)
        error.findViewById<View>(R.id.buttonRetry).setOnClickListener { viewModel.retry() }

        val adapter = AnimeRowAdapter({ openAnimeDetail(it) }, { toggleFavorite(it) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Chips de géneros (uno solo a la vez; tocar el elegido lo desmarca)
        Translations.browseGenres.forEach { genre ->
            val chip = layoutInflater.inflate(R.layout.item_chip_filter, chips, false) as Chip
            chip.id = View.generateViewId()
            chip.text = Translations.genre(genre.name)
            chip.tag = genre.id
            chip.isChecked = genre.id == viewModel.genreId
            chips.addView(chip)
        }
        chips.setOnCheckedStateChangeListener { group, checkedIds ->
            val genreId = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it).tag as Int }
            viewModel.setGenre(genreId)
        }

        input.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    AnimeRepository.favoriteIds.collect { ids -> adapter.favoriteIds = ids }
                }
                viewModel.state.collect { state ->
                    progress.isVisible = state is UiState.Loading
                    error.isVisible = state is UiState.Error
                    recycler.isVisible = state is UiState.Success
                    summary.isVisible = state is UiState.Success
                    empty.isVisible = state is UiState.Success && state.data.results.isEmpty()
                    when (state) {
                        is UiState.Success -> {
                            summary.text = state.data.summary
                            empty.text = state.data.emptyMessage
                            adapter.submitList(state.data.results) {
                                recycler.scrollToPosition(0)
                            }
                        }
                        is UiState.Error ->
                            error.findViewById<TextView>(R.id.textErrorMessage).text = state.message
                        UiState.Loading -> Unit
                    }
                }
            }
        }
    }

    // Corazón de las tarjetas: guarda o quita sin abrir el detalle
    private fun toggleFavorite(anime: Anime) {
        viewLifecycleOwner.lifecycleScope.launch { AnimeRepository.toggleFavorite(anime) }
    }
}
