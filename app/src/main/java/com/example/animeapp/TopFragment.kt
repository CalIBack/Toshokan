package com.example.animeapp

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.ui.TopAdapter
import com.example.animeapp.ui.TopViewModel
import com.example.animeapp.ui.UiState
import com.example.animeapp.ui.openAnimeDetail
import com.example.animeapp.data.Anime
import com.example.animeapp.data.AnimeRepository
import kotlinx.coroutines.launch

// Pantalla Top: podio y ranking
class TopFragment : Fragment(R.layout.fragment_top) {
    private val viewModel: TopViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerTop)
        val progress: ProgressBar = view.findViewById(R.id.progressTop)
        val progressMore: ProgressBar = view.findViewById(R.id.progressTopMore)
        val error: View = view.findViewById(R.id.errorTop)
        error.findViewById<View>(R.id.buttonRetry).setOnClickListener { viewModel.retry() }

        val adapter = TopAdapter({ openAnimeDetail(it) }, { toggleFavorite(it) })
        val layoutManager = LinearLayoutManager(requireContext())
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        // Scroll infinito
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 5) {
                    viewModel.loadNextPage()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    AnimeRepository.favoriteIds.collect { ids -> adapter.favoriteIds = ids }
                }
                launch {
                    viewModel.state.collect { state ->
                        progress.isVisible = state is UiState.Loading
                        error.isVisible = state is UiState.Error
                        recycler.isVisible = state is UiState.Success
                        when (state) {
                            is UiState.Success -> {
                                adapter.submit(state.data.ranking)
                                progressMore.isVisible = state.data.loadingMore
                            }
                            is UiState.Error ->
                                error.findViewById<TextView>(R.id.textErrorMessage).text = state.message
                            UiState.Loading -> progressMore.isVisible = false
                        }
                    }
                }
                launch {
                    viewModel.message.collect { message ->
                        if (message != null) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            viewModel.messageShown()
                        }
                    }
                }
            }
        }
    }

    // Corazón
    private fun toggleFavorite(anime: Anime) {
        viewLifecycleOwner.lifecycleScope.launch { AnimeRepository.toggleFavorite(anime) }
    }
}
